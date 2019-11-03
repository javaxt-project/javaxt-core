package javaxt.utils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private ArrayList<Thread> threads;
    private List pool;
    private class Return{}


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ThreadPool(int numThreads, Integer maxPoolSize){
        this.numThreads = numThreads;
        threads = new ArrayList<Thread>();
        pool = new LinkedList();
        if (maxPoolSize!=null){
            if (maxPoolSize<1) maxPoolSize = null;
        }
        this.maxPoolSize = maxPoolSize;
    }

    public ThreadPool(int numThreads){
        this(numThreads, null);
    }


  //**************************************************************************
  //** start
  //**************************************************************************
  /** Used to initialize threads in the pool
   */
    public ThreadPool start(){
        for (int i=0; i<numThreads; i++){
            Thread t = new Thread(){
                public void run(){
                    while (true) {

                        Object obj = null;
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
                            add(obj);
                            return;
                        }
                        else{
                            process(obj);
                        }
                    }
                }
            };
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
  //** add
  //**************************************************************************
  /** Used to add an object to the pool to process
   */
    public void add(Object object){
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

            pool.add(object);
            pool.notify();
        }
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
    public void join() throws InterruptedException{
        while (true) {
            for (Thread thread : threads){
                thread.join();
            }
            break;
        }
    }
}