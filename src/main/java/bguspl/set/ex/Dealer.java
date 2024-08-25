package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;


    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;
    private long roundTime;
    //private long roundStartTime;
    private long roundSystemTime;
    public BlockingQueue<Player> playersQueue;
    public boolean isReshuffling;
    private boolean warn;
    public long second=1000;
    public long ten=10;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playersQueue=new LinkedBlockingQueue<Player>();
        warn=false;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        for (Player p : players)
        {
            Thread playerThread = new Thread(p);
            playerThread.start();
        }
        while (!shouldFinish()) {
            synchronized(table){
                isReshuffling=true;
                placeCardsOnTable();
                if(env.config.hints){
                    table.hints();
                }
                isReshuffling=false;
                updateTimerDisplay(true); //changed from false to true
            }
            timerLoop();//when a timer loop is ending we need to stats over with a new cards on the table 
            synchronized(table){
                isReshuffling=true;
                removeAllCardsFromTable();
            }
        }
        announceWinners();
        terminate();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }
    
  
    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        roundTime=env.config.turnTimeoutMillis;
        roundSystemTime=System.currentTimeMillis();
        reshuffleTime=roundSystemTime+env.config.turnTimeoutMillis+second;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();//checks if there are any sets that we need to deal with
            placeCardsOnTable();//if there were any sets that go down, put a new cards instead. 
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate=true;
        
        for (int i = players.length-1; i >= 0; i--) 
        {
            players[i].terminate();

        }
    }
    

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        while(!playersQueue.isEmpty()){
        Player playerNow=playersQueue.poll();
        if(playerNow == null) 
            return;
        int[][] cardsAndSlots= table.getPlayerCardsAndSlots(playerNow.id);
        for(int i = 0; i < cardsAndSlots[1].length; i++){
            if(cardsAndSlots[1][i] == -1){
                try{
                    playerNow.dealerReponse.put(2);
                } catch(Exception e){}
                return;
            }
        }
        boolean test = env.util.testSet(cardsAndSlots[0]);
        if (test){
            for (int i=0;i<cardsAndSlots[1].length;i++){
                table.removeCard(cardsAndSlots[1][i]);
            }
            try{
                playerNow.dealerReponse.put(0);//point
                roundTime=env.config.turnTimeoutMillis;
                roundSystemTime=System.currentTimeMillis();
                reshuffleTime=roundSystemTime+env.config.turnTimeoutMillis+second;
                updateTimerDisplay(true);
            } catch(Exception e){}
        }
        else{
            try{
                playerNow.dealerReponse.put(1);//penalty
            } catch(Exception e){}
        }
        }
    }
        
    



    private void randomFromDeck(List<Integer> slots){
        if(slots.size()>0){//the number of the free slots
            while( !slots.isEmpty() &&!deck.isEmpty()){
                Random r = new Random();
                int card=deck.remove(r.nextInt(deck.size()));
                int slot= slots.remove (r.nextInt(slots.size()));
                table.placeCard(card,slot);
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        if (!deck.isEmpty()){
            randomFromDeck(table.freeSlots());
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        long sleepTime = reshuffleTime - System.currentTimeMillis();
        if(sleepTime > env.config.turnTimeoutWarningMillis){
            sleepTime = sleepTime % second+ten;
            if(sleepTime != 0){
                try {
                    synchronized(this) {wait(sleepTime);}
                }
                catch(InterruptedException InterruptedException){}   
            }
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if(reset){
            env.ui.setCountdown(env.config.turnTimeoutMillis  , false);
            reshuffleTime = env.config.turnTimeoutMillis +second  + System.currentTimeMillis();
        }
        
        else {
            roundTime=reshuffleTime - System.currentTimeMillis();
            if (roundTime < 0 || roundTime<ten) {roundTime = 0;}
            roundSystemTime=System.currentTimeMillis();
            warn = (roundTime <= env.config.turnTimeoutWarningMillis);         
            env.ui.setCountdown(roundTime, warn);
        }
       
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for (int i=0; i< env.config.columns*env.config.rows;i++){
            int card = table.slotToCardInt(i);
            if (card!=-1){ 
                table.removeCard(i);
                deck.add(card);
            }
          
        }
        
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int[] winners=new int[env.config.players];
        int countWinners=0;
        int max=0;
        for(Player p:players){
            if(p.score()>max){
                max=p.score();
                countWinners=1;
                winners[0]=p.id;
            }
            else if (p.score()==max){
                winners[countWinners]=p.id;
                countWinners++;
                
            }
        }
        env.ui.announceWinner(Arrays.copyOfRange(winners,0,countWinners));
    }
        
    

    public void notifyDealer (Player player){
        synchronized(this){notifyAll();}

    }
}
    

