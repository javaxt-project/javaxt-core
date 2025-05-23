package javaxt.utils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//******************************************************************************
//**  ThreadPool
//******************************************************************************
/**
 *   Used to spawn threads and execute tasks. Instances of this class should
 *   override the process() method which is called by individual threads.
 *   Objects (e.g. data) are added to a queue via the add() method. Individual
 *   threads wait for data to be added to the queue. As new data is added,
 *   it is assigned to a thread to process. The thread, in turn, calls the
 *   process() method. The ThreadPool will run indefinitely, unless the done()
 *   method is called. The join() method can be used to join the ThreadPool
 *   to the caller's thread. Example:
 *
    <pre>

      //Instantiate thread pool
        int numThreads = 2;
        ThreadPool pool = new ThreadPool(numThreads){
            public void process(Object obj){
                //Do something!
            }
        };

      //Start the thread
        pool.start();


      //Add tiles to the pool
        for (int i : new int[]{1,2,3,4,5,6,7,8,9,10}){
            pool.add(i);
        }


      //Notify the pool that we have finished added records
        pool.done();


      //Wait for threads to finish
        pool.join();

    </pre>
 *
 ******************************************************************************/

public class ThreadPool {

    private int numThreads;
    private Integer maxPoolSize;
    private final ArrayList<Thread> threads;
    private final ArrayList<Thread> activeThreads;
    private final List pool;
    private class Return{}
    private ConcurrentHashMap<Long, HashMap<String, Object>> params;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ThreadPool(int numThreads, Integer maxPoolSize){
        this.numThreads = numThreads;
        threads = new ArrayList<>();
        activeThreads = new ArrayList<>();
        pool = new LinkedList();
        if (maxPoolSize!=null){
            if (maxPoolSize<1) maxPoolSize = null;
        }
        this.maxPoolSize = maxPoolSize;
        params = new ConcurrentHashMap<>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ThreadPool(int numThreads){
        this(numThreads, null);
    }


  //**************************************************************************
  //** start
  //**************************************************************************
  /** Used to initialize threads in the pool
   */
    public ThreadPool start(){

        threads.clear();

        for (int i=0; i<numThreads; i++){
            Thread t = new Thread(){
                public void run(){
                    while (true) {

                        Object obj;
                        synchronized (pool) {
                            while (pool.isEmpty()) {
                                try {
                                    pool.wait();
                                }
                                catch (InterruptedException e) {
                                    return;
                                }
                            }
                            obj = pool.remove(0);
                            pool.notifyAll();
                        }

                        if ((obj instanceof Return)){


                          //Remove thread from the activeThreads array
                            synchronized(activeThreads){
                                for (int i=0; i<activeThreads.size(); i++){
                                    if (activeThreads.get(i).getId()==this.getId()){
                                        activeThreads.remove(i);
                                        break;
                                    }
                                }


                              //Add the object back to the pool for other threads to process
                                if (!activeThreads.isEmpty()) add(obj);
                                activeThreads.notify();
                            }


                          //Call the exit() callback and return
                            exit();
                            return;

                        }
                        else{

                            try{

                              //Call the process() callback
                                process(obj);

                            }
                            catch(Exception e){

                              //Remove thread from the activeThreads array
                                synchronized(activeThreads){
                                    for (int i=0; i<activeThreads.size(); i++){
                                        if (activeThreads.get(i).getId()==this.getId()){
                                            activeThreads.remove(i);
                                            break;
                                        }
                                    }
                                    activeThreads.notify();
                                }

                              //Throw exception
                                throw e;
                            }

                        }
                    }
                }
            };

            synchronized(activeThreads){
                activeThreads.add(t);
                activeThreads.notify();
            }

            t.start();
            threads.add(t);
        }
        return this;
    }


  //**************************************************************************
  //** process
  //**************************************************************************
  /** Called whenever a thread gets an object to process
   */
    public void process(Object obj){}


  //**************************************************************************
  //** exit
  //**************************************************************************
  /** Called when a thread is being disposed
   */
    public void exit(){}


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns a variable for an individual thread
   */
    public Object get(String key){
        return get(key, null);
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns a variable associated with an individual thread
   *  @param key The name of the variable
   *  @param setter Optional. Used to generate a value if the value has not
   *  been set. The following example shows how to call the get method with
   *  a setter as a lamba expression to return a new database connection:
   <pre>
        Connection conn = (Connection) get("conn", () -> {
            return database.getConnection();
        });
   </pre>
   */
    public Object get(String key, Setter setter) {
        long id = getThreadID();
        Object val = null;
        synchronized(params){
            HashMap<String, Object> map = params.get(id);

            if (map==null && setter!=null){
                map = new HashMap<>();
                params.put(id, map);
            }

            if (map!=null){
                val = map.get(key);
                if (val==null && setter!=null){
                    try{
                        val = setter.getValue();
                        if (val==null) throw new Exception("Setter cannot return a null value");
                        map.put(key, val);
                    }
                    catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
            params.notifyAll();
        }
        return val;
    }


  //**************************************************************************
  //** set
  //**************************************************************************
  /** Used to set a variable for an individual thread
   */
    public void set(String key, Object value){
        long id = getThreadID();
        synchronized(params){
            HashMap<String, Object> map = params.get(id);
            if (map==null){
                map = new HashMap<>();
                params.put(id, map);
            }
            map.put(key, value);
            params.notifyAll();
        }
    }


  //**************************************************************************
  //** getThreadID
  //**************************************************************************
    private long getThreadID(){
        long id = Thread.currentThread().getId();
        for (Thread t : threads){
            if (id==t.getId()){
                return id;
            }
        }


        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        for (Thread t : threads){
            if (t.getThreadGroup().parentOf(threadGroup)){
                return t.getId();
            }
        }
        //ThreadGroup parentGroup = threadGroup.getParent();

        throw new RuntimeException("Thread not found");
    }


  //**************************************************************************
  //** Setter Interface
  //**************************************************************************
    public static interface Setter {
        public Object getValue() throws Exception;
    }


  //**************************************************************************
  //** add
  //**************************************************************************
  /** Used to add an object to the pool to process
   *  @return Integer representing the position in the queue
   */
    public int add(Object object){
        int idx;
        synchronized (pool) {

            if (maxPoolSize!=null){
                while (pool.size()>maxPoolSize){
                    try{
                        pool.wait();
                    }
                    catch(java.lang.InterruptedException e){
                        break;
                    }
                }
            }

            idx = pool.size();
            pool.add(object);
            pool.notify();
        }
        return idx;
    }


  //**************************************************************************
  //** getActiveThreadCount
  //**************************************************************************
  /** Returns the number of active threads in the pool
   */
    public int getActiveThreadCount(){
        int count;
        synchronized(activeThreads){
            count = activeThreads.size();
        }
        return count;
    }


  //**************************************************************************
  //** getQueue
  //**************************************************************************
  /** Returns a handle to the job queue. Use with care. Be sure to add a
   *  synchronized block when processing or iterating through items in the
   *  queue.
   */
    public List getQueue(){
        return pool;
    }


  //**************************************************************************
  //** done
  //**************************************************************************
  /** Used to notify the threads that we are done adding objects to the pool
   *  and to exit when ready
   */
    public void done(){
        add(new Return());
    }


  //**************************************************************************
  //** join
  //**************************************************************************
  /** Used to wait for threads to complete
   */
    public void join() throws InterruptedException {
        for (Thread thread : threads){
            thread.join();
        }
    }
}