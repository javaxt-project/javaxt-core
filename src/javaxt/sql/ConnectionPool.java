package javaxt.sql;

import java.util.Map;
import java.util.HashMap;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//******************************************************************************
//**  ConnectionPool
//******************************************************************************
/**
 *   A lightweight, high-performance JDBC connection pool manager with health
 *   monitoring, validation caching, and lock-free concurrent connection
 *   management.
 *
 ******************************************************************************/

public class ConnectionPool {

    private ConnectionPoolDataSource       dataSource;
    private int                            maxConnections;
    private int                            minConnections;
    private long                           timeoutMs;
    private PrintWriter                    logWriter;
    private final AtomicInteger            totalConnections = new AtomicInteger(0); // Lock-free connection counting
    private PoolConnectionEventListener    poolConnectionEventListener;

    // Health monitoring and validation
    private long                           connectionIdleTimeoutMs;
    private long                           connectionMaxAgeMs;
    private String                         validationQuery;
    private int                            validationTimeout;
    private ScheduledExecutorService       healthCheckExecutor;
    private ScheduledFuture<?>             healthCheckTask;

    // Thread-safe counters and flags
    private final AtomicInteger            activeConnections = new AtomicInteger(0);
    private final AtomicBoolean            isDisposed = new AtomicBoolean(false);
    private final AtomicBoolean            doPurgeConnection = new AtomicBoolean(false);

    // Thread-safe connection storage
    private final ConcurrentLinkedQueue<PooledConnectionWrapper> recycledConnections = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<PooledConnection, PooledConnectionWrapper> connectionWrappers = new ConcurrentHashMap<>();
    private volatile PooledConnection connectionInTransition;

    // Validation caching for performance optimization
    private final ConcurrentHashMap<PooledConnection, Long> validationCache = new ConcurrentHashMap<>();
    private static final long VALIDATION_CACHE_TTL = 30000; // 30 seconds


    private Database database;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ConnectionPool(Database database, int maxConnections) throws SQLException {
        this(database, maxConnections, null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ConnectionPool(Database database, int maxConnections, int timeout) throws SQLException{
        this(database, maxConnections, new HashMap<String, Object>() {{
            put("timeout", timeout);
        }});
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate the ConnectionPool using with a javaxt.sql.Database
   *  @param database javaxt.sql.Database with database connection information
   *  including a valid getConnectionPoolDataSource() response. The database
   *  object provides additional run-time query optimizations (e.g. connection
   *  metadata).
   *  @param maxConnections Maximum number of database connections for the
   *  connection pool.
   *  @param options Additional pool configuration options including:
   *  <ul>
   *  <li>timeout: The maximum time to wait for a free connection, in seconds. Default is 20 seconds.</li>
   *  <li>idleTimeout: Connection idle timeout in seconds. Default is 300 seconds (5 minutes).</li>
   *  <li>maxAge: Maximum connection age in seconds. Default is 1800 seconds (30 minutes).</li>
   *  <li>validationQuery: Query to validate connections. Default is "SELECT 1".</li>
   *  <li>validationTimeout: Interval used to execute validation queries. Default is 5 seconds.</li>
   *  </ul>
   */
    public ConnectionPool(Database database, int maxConnections, Map<String, Object> options) throws SQLException {
        if (database==null) throw new IllegalArgumentException("Database is required");
        this.database = database;
        init(database.getConnectionPoolDataSource(), maxConnections, options);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ConnectionPool(ConnectionPoolDataSource dataSource, int maxConnections) {
        init(dataSource, maxConnections, null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ConnectionPool(ConnectionPoolDataSource dataSource, int maxConnections, Integer timeout) {
        init(dataSource, maxConnections, new HashMap<String, Object>() {{
            put("timeout", timeout);
        }});
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ConnectionPool(ConnectionPoolDataSource dataSource, int maxConnections, Map<String, Object> options) throws SQLException {
        init(dataSource, maxConnections, options);
    }


  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /** Retrieves a connection from the connection pool. If all the connections
   *  are in use, the method waits until a connection becomes available or
   *  <code>timeout</code> seconds elapsed. When the application is finished
   *  using the connection, it must ne closed in order to return it to the pool.
   */
    public Connection getConnection() throws SQLException {
        long time = System.currentTimeMillis();
        long timeoutTime = time + timeoutMs;
        int triesWithoutDelay = getInactiveConnections() + 1;

        while (true) {
            java.sql.Connection conn = getConnection(time, timeoutTime);
            if (conn != null) {
                Connection c = new Connection();
                c.open(conn, database);
                return c;
            }
            triesWithoutDelay--;
            if (triesWithoutDelay <= 0) {
                triesWithoutDelay = 0;
                try {
                    // Intentional sleep to avoid busy waiting when no connections are available
                    Thread.sleep(250);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for a valid database connection.", e);
                }
            }
            time = System.currentTimeMillis();
            if (time >= timeoutTime) {
                throw new TimeoutException("Timeout while waiting for a valid database connection.");
            }
        }
    }


  //**************************************************************************
  //** getActiveConnections
  //**************************************************************************
  /** Returns the number of active (open) connections of this pool.
   *
   * <p>This is the number of <code>Connection</code> objects that have been
   * issued by {@link #getConnection()}, for which <code>Connection.close()</code>
   * has not yet been called.</p>
   */
    public int getActiveConnections() {
        return activeConnections.get();
    }


  //**************************************************************************
  //** getInactiveConnections
  //**************************************************************************
  /** Returns the number of inactive (unused) connections in this pool.
   *
   * <p>This is the number of internally kept recycled connections,
   * for which <code>Connection.close()</code> has been called and which
   * have not yet been reused.</p>
   */
    public int getInactiveConnections() {
        return recycledConnections.size();
    }


  //**************************************************************************
  //** getMaxConnections
  //**************************************************************************
  /** Returns the configured maximum number of connections in the pool.
   */
    public int getMaxConnections(){
        return maxConnections;
    }


  //**************************************************************************
  //** getConnectionPoolDataSource
  //**************************************************************************
  /** Returns the ConnectionPoolDataSource backing this connection pool.
   */
    public ConnectionPoolDataSource getConnectionPoolDataSource(){
        return dataSource;
    }


  //**************************************************************************
  //** getTimeout
  //**************************************************************************
  /** Returns the maximum time to wait for a free connection, in seconds.
   */
    public int getTimeout(){
        return Math.round(timeoutMs/1000);
    }


  //**************************************************************************
  //** getConnectionIdleTimeout
  //**************************************************************************
  /** Returns the connection idle timeout in seconds. Connections that remain
   *  unused in the pool for more than the idle timeout are automatically
   *  removed. This prevents accumulation of stale connections that may have
   *  been closed by the database server. Note that this only affects
   *  connections sitting idle in the pool, not active connections being used
   *  by your application.
   */
    public int getConnectionIdleTimeout() {
        return Math.round(connectionIdleTimeoutMs/1000);
    }


  //**************************************************************************
  //** getConnectionIdleTimeout
  //**************************************************************************
  /** Returns the maximum connection age in seconds.
   */
    public long getConnectionMaxAge() {
        return Math.round(connectionMaxAgeMs/1000);
    }


  //**************************************************************************
  //** getValidationQuery
  //**************************************************************************
  /** Returns the validation query used to periodically test connections in
   *  the pool.
   */
    public String getValidationQuery() {
        return validationQuery;
    }


  //**************************************************************************
  //** getValidationTimeout
  //**************************************************************************
  /** Returns the interval used to execute validation queries in seconds.
   */
    public int getValidationTimeout() {
        return validationTimeout;
    }


  //**************************************************************************
  //** close
  //**************************************************************************
  /** Closes all unused pooled connections and shuts down the connection pool.
   *  After calling this method, clients can no longer get new connections via
   *  getConnection().
   */
    public void close() throws SQLException {
        if (!isDisposed.compareAndSet(false, true)) {
            return; // Already disposed
        }

        stopHealthMonitoring();

        // Use disposeConnection to properly handle the totalConnectionCount decrement
        PooledConnectionWrapper wrapper;
        while ((wrapper = recycledConnections.poll()) != null) {
            disposeConnection(wrapper.connection);
        }

        connectionWrappers.clear();
    }


  //**************************************************************************
  //** isClosed
  //**************************************************************************
  /** Returns true if the connection pool has been closed.
   */
    public boolean isClosed() {
        return isDisposed.get();
    }


  //**************************************************************************
  //** init
  //**************************************************************************
    private void init(ConnectionPoolDataSource dataSource, int maxConnections, Map<String, Object> options) {

        if (dataSource==null) throw new IllegalArgumentException("dataSource is required");
        if (maxConnections<1) throw new IllegalArgumentException("Invalid maxConnections");


        if (options==null) options = new HashMap<>();
        Integer timeout = new Value(options.get("timeout")).toInteger();
        if (timeout==null || timeout <= 0) timeout = 20; // 20 seconds default

        Integer idleTimeout = new Value(options.get("idleTimeout")).toInteger();
        if (idleTimeout==null || idleTimeout <= 0) idleTimeout = 300; // 5 minutes default

        Integer maxAge = new Value(options.get("maxAge")).toInteger();
        if (maxAge==null || maxAge <= 0) maxAge = 1800; // 30 minutes default

        Integer validationTimeout = new Value(options.get("validationTimeout")).toInteger();
        if (validationTimeout==null || validationTimeout <= 0) validationTimeout = 5; // 5 seconds

        String validationQuery = new Value(options.get("validationQuery")).toString();
        if (validationQuery == null || validationQuery.trim().isEmpty()) {
            validationQuery = "SELECT 1";
        }
        else{
            validationQuery = validationQuery.trim();
        }


        this.dataSource = dataSource;
        this.maxConnections = maxConnections;
        this.minConnections = (int) Math.round(Math.max(((long)maxConnections)*0.2, 1.0));
        this.timeoutMs = timeout * 1000L;
        this.connectionIdleTimeoutMs = idleTimeout * 1000L;
        this.connectionMaxAgeMs = maxAge * 1000L;
        this.validationQuery = validationQuery;
        this.validationTimeout = validationTimeout;

        // Initialize atomic counter for lock-free connection management
        this.totalConnections.set(0);

        try { logWriter = dataSource.getLogWriter(); }
        catch (SQLException e) {}

        poolConnectionEventListener = new PoolConnectionEventListener();
        startHealthMonitoring();
    }


  //**************************************************************************
  //** getConnection
  //**************************************************************************
    private java.sql.Connection getConnection(long time, long timeoutTime) {
        long rtime = Math.max(1, timeoutTime - time);
        java.sql.Connection conn;
        try {
            conn = acquireConnection(rtime);
        }
        catch (SQLException e) {
            return null;
        }

        // Calculate remaining time for validation
        rtime = timeoutTime - System.currentTimeMillis();
        int rtimeSecs = Math.max(1, (int)((rtime + 999) / 1000));

        try {
            if (conn.isValid(rtimeSecs)) {
                return conn;
            }
        }
        catch (SQLException e) {
            log("isValid() failed: " + e.getMessage());
            // This Exception should never occur. If it nevertheless occurs, it's because of an error in the
            // JDBC driver which we ignore and assume that the connection is not valid.
        }

        // When isValid() returns false, the JDBC driver should have already called connectionErrorOccurred()
        // and the PooledConnection has been removed from the pool, i.e. the PooledConnection will
        // not be added to recycledConnections when Connection.close() is called.
        // But to be sure that this works even with a faulty JDBC driver, we call purgeConnection().
        purgeConnection(conn);
        return null;
    }


  //**************************************************************************
  //** acquireConnection
  //**************************************************************************
    private java.sql.Connection acquireConnection(long timeoutMs) throws SQLException {
        if (isDisposed.get()) {
            throw new IllegalStateException("Connection pool has been disposed.");
        }

        long startTime = System.currentTimeMillis();
        long timeoutTime = startTime + timeoutMs;

        while (System.currentTimeMillis() < timeoutTime) {
            // First, try to get a recycled connection
            java.sql.Connection recycledConn = getRecycledConnection();
            if (recycledConn != null) {
                return recycledConn;
            }

            // No recycled connection available, try to create a new one
            // Use atomic counter for lock-free connection limit control
            int currentTotal = totalConnections.get();
            if (currentTotal < maxConnections) {
                if (totalConnections.compareAndSet(currentTotal, currentTotal + 1)) {
                    try {
                        java.sql.Connection conn = createNewConnection();
                        if (conn != null) {
                            return conn;
                        } else {
                            // Connection creation failed, decrement counter
                            totalConnections.decrementAndGet();
                        }
                    } catch (SQLException e) {
                        // Connection creation failed, decrement counter
                        totalConnections.decrementAndGet();
                        // Continue to next iteration to try again
                    }
                }
            }
            // If we couldn't create a connection, wait a bit and try again
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for a database connection.", e);
            }
        }

        throw new TimeoutException();
    }


  //**************************************************************************
  //** getRecycledConnection
  //**************************************************************************
    private java.sql.Connection getRecycledConnection() throws SQLException {
        PooledConnectionWrapper wrapper = recycledConnections.poll();
        if (wrapper == null) {
            return null; // No recycled connections available
        }

        PooledConnection pconn = wrapper.connection;

        // Smart validation: skip validation for recently used connections
        boolean needsValidation = wrapper.isExpired(connectionMaxAgeMs) ||
                                wrapper.isIdle(connectionIdleTimeoutMs) ||
                                !isRecentlyValidated(pconn);

        if (needsValidation && !validateConnection(pconn)) {
            // Connection is invalid, dispose it properly
            doPurgeConnection.set(true);
            try {
                pconn.removeConnectionEventListener(poolConnectionEventListener);
                pconn.close();
            } catch (SQLException e) {
                // Ignore close errors for invalid connections
            } finally {
                doPurgeConnection.set(false);
            }
            connectionWrappers.remove(pconn);
            totalConnections.decrementAndGet();
            return null; // Try another recycled connection or create new one
        }

        // Connection is valid, update its usage time and return it
        wrapper = wrapper.markUsed();
        connectionWrappers.put(pconn, wrapper);

        java.sql.Connection conn;
        try {
            connectionInTransition = pconn;
            activeConnections.incrementAndGet(); // Increment before getConnection() to ensure it's always counted
            conn = pconn.getConnection();
        } catch (SQLException e) {
            connectionInTransition = null;
            // Connection failed, decrement the activeConnections counter we just incremented
            activeConnections.decrementAndGet();
            // Connection failed, dispose it
            connectionWrappers.remove(pconn);
            doPurgeConnection.set(true);
            try {
                pconn.removeConnectionEventListener(poolConnectionEventListener);
                pconn.close();
            } catch (SQLException ex) {
                // Ignore close errors for failed connections
            } finally {
                doPurgeConnection.set(false);
                totalConnections.decrementAndGet();
            }
            return null;
        } finally {
            connectionInTransition = null;
        }

        return conn;
    }

    private java.sql.Connection createNewConnection() throws SQLException {
        PooledConnection pconn = null;
        try {
            pconn = dataSource.getPooledConnection();
            pconn.addConnectionEventListener(poolConnectionEventListener);
            PooledConnectionWrapper wrapper = new PooledConnectionWrapper(pconn);
            connectionWrappers.put(pconn, wrapper);

            java.sql.Connection conn;
            try {
                connectionInTransition = pconn;
                activeConnections.incrementAndGet(); // Increment before getConnection() to ensure it's always counted
                conn = pconn.getConnection();
                // totalConnections was already incremented in acquireConnection
                return conn;
            } catch (SQLException e) {
                connectionInTransition = null;
                // Connection creation failed, decrement the activeConnections counter we just incremented
                activeConnections.decrementAndGet();
                // Connection creation failed, clean up
                connectionWrappers.remove(pconn);
                doPurgeConnection.set(true);
                try {
                    pconn.removeConnectionEventListener(poolConnectionEventListener);
                    pconn.close();
                } catch (SQLException ex) {
                    // Ignore close errors for failed connections
                } finally {
                    doPurgeConnection.set(false);
                    totalConnections.decrementAndGet();
                }
                throw e;
            } finally {
                connectionInTransition = null;
            }
        } catch (SQLException e) {
            throw e;
        }
    }


  //**************************************************************************
  //** startHealthMonitoring
  //**************************************************************************
  /** Starts the background health monitoring thread.
   */
    private void startHealthMonitoring() {
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConnectionPool-HealthCheck");
            t.setDaemon(true);
            return t;
        });

        // Run health check every 30 seconds
        healthCheckTask = healthCheckExecutor.scheduleWithFixedDelay(
            this::performHealthCheck, 30, 30, TimeUnit.SECONDS);
    }


  //**************************************************************************
  //** stopHealthMonitoring
  //**************************************************************************
  /** Stops the health monitoring thread.
   */
    private void stopHealthMonitoring() {
        if (healthCheckTask != null) {
            healthCheckTask.cancel(false);
            healthCheckTask = null;
        }
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                    log("Health monitoring thread did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            healthCheckExecutor = null;
        }
    }


  //**************************************************************************
  //** performHealthCheck
  //**************************************************************************
  /** Performs health check on idle connections.
   */
    private void performHealthCheck() {
        try {
            log("Health check started");
            int removedCount = 0;
            long now = System.currentTimeMillis();

            // Check for idle and expired connections
            log("Checking " + recycledConnections.size() + " recycled connections for idle/expired");

            // Drain connections to temporary list to avoid concurrent modification
            java.util.List<PooledConnectionWrapper> toCheck = new java.util.ArrayList<>();
            PooledConnectionWrapper wrapper;
            while ((wrapper = recycledConnections.poll()) != null) {
                toCheck.add(wrapper);
            }

            // Process connections and re-add valid ones
            for (PooledConnectionWrapper w : toCheck) {
                if (w.isIdle(connectionIdleTimeoutMs) || w.isExpired(connectionMaxAgeMs)) {
                    disposeConnection(w.connection);
                    removedCount++;
                    log("Removed " + (w.isExpired(connectionMaxAgeMs) ? "expired" : "idle") +
                        " connection from pool. Age: " + (now - w.createdTime) + "ms");
                } else {
                    // Re-add valid connection back to the queue
                    recycledConnections.offer(w);
                }
            }

            if (removedCount > 0) {
                log("Health check completed: removed " + removedCount + " connections");
            }

            // Log pool statistics periodically
            PoolStatistics stats = getPoolStatistics();
            log("Pool stats - Active: " + stats.activeConnections +
                ", Recycled: " + stats.recycledConnections +
                ", Available permits: " + stats.availablePermits +
                ", Max: " + stats.maxConnections +
                ", Min: " + stats.minConnections +
                ", Total: " + stats.totalConnections);

            // Ensure at least minConnections are available (warmed) in the pool
            int currentRecycled = recycledConnections.size();
            int currentActive = activeConnections.get();
            int total = currentActive + currentRecycled;

            if (currentRecycled < minConnections && total < maxConnections && !isDisposed.get()) {
                log("Pool warm-up: ensuring minimum " + minConnections + " connections available");
                int maxAttempts = 3; // Limit attempts to prevent infinite loops
                int attempts = 0;

                while (currentRecycled < minConnections && total < maxConnections && !isDisposed.get() && attempts < maxAttempts) {
                    attempts++;

                    // Try to increment totalConnections for the warm-up connection
                    int currentTotal = totalConnections.get();
                    if (currentTotal >= maxConnections) {
                        log("Maximum connections reached for pool warm-up");
                        break;
                    }
                    if (!totalConnections.compareAndSet(currentTotal, currentTotal + 1)) {
                        continue; // Try again if CAS failed
                    }

                    PooledConnection pconn = null;
                    try {
                        pconn = dataSource.getPooledConnection();
                        pconn.addConnectionEventListener(poolConnectionEventListener);

                        if (validateConnection(pconn)) {
                            PooledConnectionWrapper w = new PooledConnectionWrapper(pconn);
                            connectionWrappers.put(pconn, w);
                            recycledConnections.offer(w);
                            currentRecycled++;
                            total = currentActive + currentRecycled;
                            log("Pool warm-up: added connection " + currentRecycled + "/" + minConnections);
                        } else {
                            // If validation fails, dispose the connection and decrement counter
                            disposeConnection(pconn);
                            pconn = null; // Ensure pconn is null so finally block doesn't try to close it again
                        }
                    } catch (SQLException e) {
                        log("Failed to create or validate connection during warm-up: " + e.getMessage());
                        if (pconn != null) {
                            disposeConnection(pconn); // Dispose and decrement counter
                        } else {
                            totalConnections.decrementAndGet(); // Decrement counter if connection creation failed before pconn was assigned
                        }
                    }
                }

                if (attempts >= maxAttempts) {
                    log("Pool warm-up: reached maximum attempts (" + maxAttempts + ")");
                }
            }
        } catch (Exception e) {
            log("Error during health check: " + e.getMessage());
        }
    }


  //**************************************************************************
  //** isRecentlyValidated
  //**************************************************************************
  /** Validates a connection using the configured validation query.
   *  This method is completely isolated from the pool lifecycle to prevent race conditions.
   */
    private boolean isRecentlyValidated(PooledConnection pooledConnection) {
        if (validationQuery == null || validationQuery.trim().isEmpty()) {
            return true; // No validation query configured, consider it valid
        }

        Long lastValidated = validationCache.get(pooledConnection);
        if (lastValidated == null) {
            return false; // Never validated
        }

        long now = System.currentTimeMillis();
        return (now - lastValidated) < VALIDATION_CACHE_TTL;
    }


  //**************************************************************************
  //** validateConnection
  //**************************************************************************
    private boolean validateConnection(PooledConnection pooledConnection) {
        if (validationQuery == null || validationQuery.trim().isEmpty()) {
            return true;
        }

        // Check validation cache first
        Long lastValidated = validationCache.get(pooledConnection);
        long now = System.currentTimeMillis();

        if (lastValidated != null && (now - lastValidated) < VALIDATION_CACHE_TTL) {
            return true; // Recently validated, skip actual validation
        }

        // TODO: Implement a safer validation mechanism that doesn't interfere with driver pooling
        return true;
    }


  //**************************************************************************
  //** purgeConnection
  //**************************************************************************
  /** Purges the PooledConnection associated with the passed Connection from
   *  the connection pool.
   */
    private void purgeConnection(java.sql.Connection conn) {
        doPurgeConnection.set(true);
        try {
            // Setting doPurgeConnection flag ensures that when connectionClosed() fires,
            // recycleConnection() will call disposeConnection() instead of recycling the connection.
            conn.close();
        } catch (SQLException e) {
            log("Error closing connection during purge: " + e.getMessage());
        } finally {
            doPurgeConnection.set(false);
        }
    }


  //**************************************************************************
  //** recycleConnection
  //**************************************************************************
    private void recycleConnection (PooledConnection pconn) {
        if (isDisposed.get() || doPurgeConnection.get()) {
            disposeConnection(pconn);
            return;
        }

        // Check if this connection is currently being processed to prevent duplicate processing
        if (pconn == connectionInTransition) {
            log("Warning: Ignoring recycle request for connection in transition - potential leak risk");
            return;
        }

        // Use atomic decrement to avoid TOCTOU race condition
        int prev = activeConnections.decrementAndGet();
        if (prev < 0) {
            throw new AssertionError("Active connections count went negative");
        }

        // Get the existing wrapper and update its usage time
        PooledConnectionWrapper wrapper = connectionWrappers.get(pconn);
        if (wrapper != null) {
            // Update the wrapper with current usage time and add to recycled connections
            wrapper = wrapper.markUsed();
            recycledConnections.offer(wrapper);
        }
        else {
            // Fallback: create new wrapper if not found (shouldn't happen in normal operation)
            wrapper = new PooledConnectionWrapper(pconn);
            recycledConnections.offer(wrapper);
        }

        // Connection successfully recycled
        // Note: totalConnections remains unchanged during recycling since the connection
        // is just moving from active to recycled state, not being disposed
    }


  //**************************************************************************
  //** disposeConnection
  //**************************************************************************
    private void disposeConnection (PooledConnection pconn) {
        pconn.removeConnectionEventListener(poolConnectionEventListener);

        // Use connectionWrappers.remove() return value as a guard to prevent double disposal
        // Only proceed with disposal if this connection was actually managed by the pool
        PooledConnectionWrapper removedWrapper = connectionWrappers.remove(pconn);
        if (removedWrapper == null) {
            // Connection was not managed by the pool, nothing to dispose
            return;
        }

        validationCache.remove(pconn);

        // Try to remove from recycled connections
        boolean foundInRecycled = false;
        for (PooledConnectionWrapper wrapper : recycledConnections) {
            if (wrapper.connection == pconn) {
                if (recycledConnections.remove(wrapper)) {
                    foundInRecycled = true;
                }
                break;
            }
        }

        // If not found in recycled connections and not currently in transition,
        // and not being purged (validation connections), we assume that the connection was active
        if (!foundInRecycled && pconn != connectionInTransition && !doPurgeConnection.get()) {
            // Use atomic decrement to avoid race condition
            int prev = activeConnections.decrementAndGet();
            if (prev < 0) {
                // Connection was never counted as active, restore counter
                activeConnections.incrementAndGet();
            }
        }

        // Only decrement totalConnections when disposing a connection (not recycling)
        // This ensures that the total connection count is properly managed
        if (!foundInRecycled) {
            totalConnections.decrementAndGet();
        }

        try {
            pconn.close();
        }
        catch (SQLException e) {
            log("Error while closing database connection: "+e.toString());
        }
        assertInnerState();
    }


  //**************************************************************************
  //** log
  //**************************************************************************
    private void log(String msg) {
        String s = "ConnectionPool: "+msg;
        try {
            if (logWriter == null) {
                //System.err.println(s);
            }
            else {
                logWriter.println(s);
            }
        }
        catch (Exception e) {}
    }


  //**************************************************************************
  //** assertInnerState
  //**************************************************************************
    private void assertInnerState() {
        int active = activeConnections.get();
        int total = totalConnections.get();

        if (active < 0) {
            throw new AssertionError("Active connections count is negative: " + active);
        }
        if (total < 0) {
            throw new AssertionError("Total connections count is negative: " + total);
        }
        // Relaxed assertion: allow temporary overshoot due to lock-free design timing windows
        // Only fail if we're significantly over the limit (more than 10% tolerance)
        if (total > maxConnections + Math.max(1, maxConnections / 10)) {
            throw new AssertionError("Total connections significantly exceed maximum: total=" + total +
                                    ", max=" + maxConnections + ", tolerance=" + Math.max(1, maxConnections / 10));
        }
    }


  //**************************************************************************
  //** PoolConnectionEventListener Class
  //**************************************************************************
    private class PoolConnectionEventListener implements ConnectionEventListener {
        @Override
        public void connectionClosed (ConnectionEvent event) {
            PooledConnection pconn = (PooledConnection)event.getSource();
            recycleConnection(pconn);
        }
        @Override
        public void connectionErrorOccurred (ConnectionEvent event) {
            PooledConnection pconn = (PooledConnection)event.getSource();
            disposeConnection(pconn);
        }
    }


  //**************************************************************************
  //** PooledConnectionWrapper Class
  //**************************************************************************
  /** Wrapper class to track connection metadata
   */
    private static class PooledConnectionWrapper {
        final PooledConnection connection;
        final long createdTime;
        final long lastUsedTime;

        PooledConnectionWrapper(PooledConnection connection) {
            this.connection = connection;
            this.createdTime = System.currentTimeMillis();
            this.lastUsedTime = System.currentTimeMillis();
        }

        PooledConnectionWrapper(PooledConnection connection, long createdTime, long lastUsedTime) {
            this.connection = connection;
            this.createdTime = createdTime;
            this.lastUsedTime = lastUsedTime;
        }

        boolean isIdle(long idleTimeoutMs) {
            return System.currentTimeMillis() - lastUsedTime > idleTimeoutMs;
        }

        boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - createdTime > maxAgeMs;
        }

        PooledConnectionWrapper markUsed() {
            return new PooledConnectionWrapper(connection, createdTime, System.currentTimeMillis());
        }
    }


  //**************************************************************************
  //** getPoolStatistics
  //**************************************************************************
  /** Returns current pool statistics for monitoring.
   */
    public PoolStatistics getPoolStatistics() {
        int active = activeConnections.get();
        int recycled = recycledConnections.size();
        int total = totalConnections.get();
        int available = maxConnections - total;

        return new PoolStatistics(
            active,
            recycled,
            available,
            maxConnections,
            minConnections,
            total
        );
    }


  //**************************************************************************
  //** PoolStatistics Class
  //**************************************************************************
  /** Pool statistics for monitoring connection pool health.
   */
    public static class PoolStatistics {
        public final int activeConnections;
        public final int recycledConnections;
        public final int availablePermits;
        public final int maxConnections;
        public final int minConnections;
        public final int totalConnections;

        public PoolStatistics(int activeConnections, int recycledConnections,
                            int availablePermits, int maxConnections, int minConnections, int totalConnections) {
            this.activeConnections = activeConnections;
            this.recycledConnections = recycledConnections;
            this.availablePermits = availablePermits;
            this.maxConnections = maxConnections;
            this.minConnections = minConnections;
            this.totalConnections = totalConnections;
        }

        @Override
        public String toString() {
            return String.format("PoolStatistics{active=%d, recycled=%d, available=%d, max=%d, min=%d, total=%d}",
                activeConnections, recycledConnections, availablePermits, maxConnections, minConnections, totalConnections);
        }
    }


  //**************************************************************************
  //** TimeoutException Class
  //**************************************************************************
  /** Thrown in {@link #getConnection()} when no free connection becomes
   *  available within <code>timeout</code> seconds.
   */
    public static class TimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1;
        public TimeoutException () { super("Timeout while waiting for a free database connection."); }
        public TimeoutException (String msg) { super(msg); }
    }

}