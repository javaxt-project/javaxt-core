package javaxt.sql;

import java.io.PrintWriter;
//import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

//******************************************************************************
//**  ConnectionPool
//******************************************************************************
/**
 *   A lightweight standalone JDBC connection pool manager.
 *
 ******************************************************************************/

public class ConnectionPool {

// This class is a modified version of the MiniConnectionPoolManager (version 2012-01-24)
// Copyright 2007-2012 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// MiniConnectionPoolManager Changes for javaxt:
// - Renamed MiniConnectionPoolManager class to ConnectionPool
// - Updated package name
// - Removed java.sql.Connection import and replaced "Connection" with "java.sql.Connection"
// - Added 2 new constructors that take a javaxt.sql.Database
// - Added getMaxConnections method
// - Renamed dispose method to close
// - Minor code formatting


    private ConnectionPoolDataSource       dataSource;
    private int                            maxConnections;
    private long                           timeoutMs;
    private PrintWriter                    logWriter;
    private Semaphore                      semaphore;
    private PoolConnectionEventListener    poolConnectionEventListener;

    // The following variables must only be accessed within synchronized blocks.
    // @GuardedBy("this") could by used in the future.
    private LinkedList<PooledConnection>   recycledConnections;          // list of inactive PooledConnections
    private int                            activeConnections;            // number of active (open) connections of this pool
    private boolean                        isDisposed;                   // true if this connection pool has been disposed
    private boolean                        doPurgeConnection;            // flag to purge the connection currently beeing closed instead of recycling it
    private PooledConnection               connectionInTransition;       // a PooledConnection which is currently within a PooledConnection.getConnection() call, or null
    private Database database;


    /**
    * Thrown in {@link #getConnection()} or {@link #getValidConnection()} when no free connection becomes
    * available within <code>timeout</code> seconds.
    */
    public static class TimeoutException extends RuntimeException {
       private static final long serialVersionUID = 1;
       public TimeoutException () {
          super("Timeout while waiting for a free database connection."); }
       public TimeoutException (String msg) {
          super(msg);
       }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Constructs a ConnectionPool with a timeout of 20 seconds.
   */
    public ConnectionPool(Database database, int maxConnections) throws SQLException {
        this(database.getConnectionPoolDataSource(), maxConnections);
        this.database = database;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Constructs a ConnectionPool.
   */
    public ConnectionPool(Database database, int maxConnections, int timeout) throws SQLException{
        this(database.getConnectionPoolDataSource(), maxConnections, timeout);
        this.database = database;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Constructs a ConnectionPool with a timeout of 20 seconds.
   */
    public ConnectionPool (ConnectionPoolDataSource dataSource, int maxConnections) {
       this(dataSource, maxConnections, null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Constructs a ConnectionPool object.
   *  @param dataSource JDBC ConnectionPoolDataSource for the connections.
   *  @param maxConnections The maximum number of connections.
   *  @param timeout The maximum time in seconds to wait for a free connection.
   *  Defaults to 20 seconds
   */
    public ConnectionPool (ConnectionPoolDataSource dataSource, int maxConnections, Integer timeout) {
        if (dataSource==null) throw new IllegalArgumentException("dataSource is required");
        if (maxConnections<1) throw new IllegalArgumentException("Invalid maxConnections");
        if (timeout==null || timeout<0) timeout = 20;

        this.dataSource = dataSource;
        this.maxConnections = maxConnections;
        this.timeoutMs = timeout * 1000L;
        try { logWriter = dataSource.getLogWriter(); }
        catch (SQLException e) {}

        semaphore = new Semaphore(maxConnections,true);
        recycledConnections = new LinkedList<PooledConnection>();
        poolConnectionEventListener = new PoolConnectionEventListener();
    }


    /**
    * Closes all unused pooled connections.
    */
    public synchronized void close() throws SQLException {
       if (isDisposed) {
          return; }
       isDisposed = true;
       SQLException e = null;
       while (!recycledConnections.isEmpty()) {
          PooledConnection pconn = recycledConnections.remove();
          try {
             pconn.close(); }
           catch (SQLException e2) {
              if (e == null) {
                 e = e2;
              }
           }
       }
       if (e != null) {
          throw e;
       }
    }

    /**
    * Retrieves a connection from the connection pool.
    *
    * <p>If <code>maxConnections</code> connections are already in use, the method
    * waits until a connection becomes available or <code>timeout</code> seconds elapsed.
    * When the application is finished using the connection, it must close it
    * in order to return it to the pool.
    *
    * @return
    *    a new <code>Connection</code> object.
    * @throws TimeoutException
    *    when no connection becomes available within <code>timeout</code> seconds.
    */
    public Connection getConnection() throws SQLException {
        int javaVersion = javaxt.utils.Java.getVersion();
        if (javaVersion<6){
            Connection c = new Connection();
            c.open(getConnection2(timeoutMs), database);
            return c;
        }
        else{
            return getValidConnection();
        }
    }

    private java.sql.Connection getConnection2 (long timeoutMs) throws SQLException {
       // This routine is unsynchronized, because semaphore.tryAcquire() may block.
       synchronized (this) {
          if (isDisposed) {
             throw new IllegalStateException("Connection pool has been disposed."); }}
       try {
          if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
             throw new TimeoutException(); }}
        catch (InterruptedException e) {
          throw new RuntimeException("Interrupted while waiting for a database connection.",e); }
       boolean ok = false;
       try {
          java.sql.Connection conn = getConnection3();
          ok = true;
          return conn; }
        finally {
          if (!ok) {
             semaphore.release();
          }
        }
    }

    private synchronized java.sql.Connection getConnection3() throws SQLException {
       if (isDisposed) {                                       // test again within synchronized lock
          throw new IllegalStateException("Connection pool has been disposed."); }
       PooledConnection pconn;
       if (!recycledConnections.isEmpty()) {
          pconn = recycledConnections.remove(); }
        else {
          pconn = dataSource.getPooledConnection();
          pconn.addConnectionEventListener(poolConnectionEventListener); }
       java.sql.Connection conn;
       try {
          // The JDBC driver may call ConnectionEventListener.connectionErrorOccurred()
          // from within PooledConnection.getConnection(). To detect this within
          // disposeConnection(), we temporarily set connectionInTransition.
          connectionInTransition = pconn;
          conn = pconn.getConnection();
       }
        finally {
          connectionInTransition = null;
        }
       activeConnections++;
       assertInnerState();
       return conn;
    }

    /**
    * Retrieves a connection from the connection pool and ensures that it is valid
    * by calling {@link Connection#isValid(int)}.
    *
    * <p>If a connection is not valid, the method tries to get another connection
    * until one is valid (or a timeout occurs).
    *
    * <p>Pooled connections may become invalid when e.g. the database server is
    * restarted.
    *
    * <p>This method is slower than {@link #getConnection()} because the JDBC
    * driver has to send an extra command to the database server to test the connection.
    *
    * <p>This method requires Java 1.6 or newer.
    *
    * @throws TimeoutException
    *    when no valid connection becomes available within <code>timeout</code> seconds.
    */
    public Connection getValidConnection() {
       long time = System.currentTimeMillis();
       long timeoutTime = time + timeoutMs;
       int triesWithoutDelay = getInactiveConnections() + 1;
       while (true) {
          java.sql.Connection conn = getValidConnection2(time, timeoutTime);
          if (conn != null) {
            Connection c = new Connection();
            c.open(conn, database);
            return c;
          }
          triesWithoutDelay--;
          if (triesWithoutDelay <= 0) {
             triesWithoutDelay = 0;
             try {
                Thread.sleep(250); }
              catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for a valid database connection.", e); }
          }
          time = System.currentTimeMillis();
          if (time >= timeoutTime) {
             throw new TimeoutException("Timeout while waiting for a valid database connection."); }
       }
    }


    private java.sql.Connection getValidConnection2 (long time, long timeoutTime) {
       long rtime = Math.max(1, timeoutTime - time);
       java.sql.Connection conn;
       try {
          conn = getConnection2(rtime); }
        catch (SQLException e) {
          return null; }
       rtime = timeoutTime - System.currentTimeMillis();
       int rtimeSecs = Math.max(1, (int)((rtime+999)/1000));
       try {
          if (conn.isValid(rtimeSecs)) {
             return conn; }}
        catch (SQLException e) {}
           // This Exception should never occur. If it nevertheless occurs, it's because of an error in the
           // JDBC driver which we ignore and assume that the connection is not valid.
       // When isValid() returns false, the JDBC driver should have already called connectionErrorOccurred()
       // and the PooledConnection has been removed from the pool, i.e. the PooledConnection will
       // not be added to recycledConnections when Connection.close() is called.
       // But to be sure that this works even with a faulty JDBC driver, we call purgeConnection().
       purgeConnection(conn);
       return null;
    }

    // Purges the PooledConnection associated with the passed Connection from the connection pool.
    private synchronized void purgeConnection (java.sql.Connection conn) {
       try {
          doPurgeConnection = true;
          // (A potential problem of this program logic is that setting the doPurgeConnection flag
          // has an effect only if the JDBC driver calls connectionClosed() synchronously within
          // Connection.close().)
          conn.close(); }
        catch (SQLException e) {}
          // ignore exception from close()
        finally {
          doPurgeConnection = false; }
    }

    private synchronized void recycleConnection (PooledConnection pconn) {
       if (isDisposed || doPurgeConnection) {
          disposeConnection(pconn);
          return; }
       if (activeConnections <= 0) {
          throw new AssertionError(); }
       activeConnections--;
       semaphore.release();
       recycledConnections.add(pconn);
       assertInnerState();
    }

    private synchronized void disposeConnection (PooledConnection pconn) {
       pconn.removeConnectionEventListener(poolConnectionEventListener);
       if (!recycledConnections.remove(pconn) && pconn != connectionInTransition) {
          // If the PooledConnection is not in the recycledConnections list
          // and is not currently within a PooledConnection.getConnection() call,
          // we assume that the connection was active.
          if (activeConnections <= 0) {
             throw new AssertionError(); }
          activeConnections--;
          semaphore.release(); }
       closeConnectionAndIgnoreException(pconn);
       assertInnerState();
    }

    private void closeConnectionAndIgnoreException (PooledConnection pconn) {
       try {
          pconn.close(); }
        catch (SQLException e) {
          log("Error while closing database connection: "+e.toString()); }
    }

    private void log (String msg) {
       String s = "ConnectionPool: "+msg;
       try {
          if (logWriter == null) {
             System.err.println(s); }
           else {
             logWriter.println(s); }}
        catch (Exception e) {}
    }

    private synchronized void assertInnerState() {
       if (activeConnections < 0) {
          throw new AssertionError(); }
       if (activeConnections + recycledConnections.size() > maxConnections) {
          throw new AssertionError(); }
       if (activeConnections + semaphore.availablePermits() > maxConnections) {
          throw new AssertionError(); }
    }

    private class PoolConnectionEventListener implements ConnectionEventListener {
       public void connectionClosed (ConnectionEvent event) {
          PooledConnection pconn = (PooledConnection)event.getSource();
          recycleConnection(pconn); }
       public void connectionErrorOccurred (ConnectionEvent event) {
          PooledConnection pconn = (PooledConnection)event.getSource();
          disposeConnection(pconn); }
    }

    /**
    * Returns the number of active (open) connections of this pool.
    *
    * <p>This is the number of <code>Connection</code> objects that have been
    * issued by {@link #getConnection()}, for which <code>Connection.close()</code>
    * has not yet been called.
    *
    * @return
    *    the number of active connections.
    **/
    public synchronized int getActiveConnections() {
       return activeConnections;
    }

    /**
    * Returns the number of inactive (unused) connections in this pool.
    *
    * <p>This is the number of internally kept recycled connections,
    * for which <code>Connection.close()</code> has been called and which
    * have not yet been reused.
    *
    * @return
    *    the number of inactive connections.
    **/
    public synchronized int getInactiveConnections() {
       return recycledConnections.size();
    }


    public int getMaxConnections(){
        return maxConnections;
    }

    public ConnectionPoolDataSource getConnectionPoolDataSource(){
        return dataSource;
    }

    public int getTimeout(){
        return Math.round(timeoutMs/1000);
    }
}