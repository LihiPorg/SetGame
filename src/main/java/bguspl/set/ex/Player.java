package bguspl.set.ex;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Random;
import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;
    protected final BlockingQueue<Integer> dealerReponse;
    private final BlockingQueue<Integer> keyPressQueue;
    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;
    /**
     * The current score of the player.
     */
    private int score;
    private Dealer dealer;
    private boolean panelty;
    private boolean point;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer=dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.keyPressQueue=new LinkedBlockingQueue<Integer>(env.config.featureSize);
        this.dealerReponse=new LinkedBlockingQueue<Integer>(env.config.featureSize);
        this.panelty=false;
        this.point=false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) {
            createArtificialIntelligence();
        }
        
        while (!terminate) {
            
            try{
                
                Integer slot=keyPressQueue.take();
                if(table.contains(id,(Integer)slot))
                {
                    
                    table.removeToken(id, slot);
                    
                }
                else{
                   
                        if(!table.isTokenFull(id)){
                            table.placeToken(id, slot);
                        
                            if (table.isTokenFull(id)){
                             
                                dealer.playersQueue.add(this);

                                dealer.notifyDealer(this);
                             
                                Integer answer = dealerReponse.take();
                                if(answer == 0){
                                    point();
                                } else if(answer == 1){
                                    penalty();
                                }
                   
                            }
                        }
                    
                }
            } catch(InterruptedException e){}
         
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            Random r = new Random();
            while (!terminate) {
                keyPressed(r.nextInt(env.config.tableSize));
                try {
                    synchronized (this) { wait(2); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate=true;
        playerThread.interrupt();
        try {
            playerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if(!panelty&&!point&&!dealer.isReshuffling){
            keyPressQueue.offer(slot);
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        point=true;
        env.ui.setScore(id, ++score);
        long seconds=env.config.pointFreezeMillis;
        while (seconds>0 )
        {

            
                env.ui.setFreeze(id, seconds);
                seconds=seconds-dealer.second;
                try {Thread.sleep(dealer.second);}
                catch (InterruptedException ignore) {}
            
        }
        point=false;
        env.ui.setFreeze(id,0);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        panelty=true;
        long seconds=env.config.penaltyFreezeMillis;
        while (seconds>0 )
        {
            env.ui.setFreeze(id, seconds);
            seconds=seconds-dealer.second;
            try {Thread.sleep(dealer.second);}
            catch (InterruptedException ignore) {}
        }
        panelty=false;
        env.ui.setFreeze(id,0);

    }

    public int score() {
        return score;
    }
}
