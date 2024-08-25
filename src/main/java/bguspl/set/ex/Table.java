package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    protected final Integer[][] tokens;//show the tokens for each player.

    protected final Object[] tokenLock;


    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot){

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.tokens=new Integer[env.config.players][3];
        this.tokenLock=new Object[env.config.tableSize];
        for (int i=0;i< tokenLock.length;i++){
            tokenLock[i]=new Object();
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        synchronized(tokenLock[slot]){
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        
            cardToSlot[card] = slot;
            slotToCard[slot] = card;
            env.ui.placeCard(card, slot);
        }
        
    
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        synchronized(tokenLock[slot]){
            try {
                Thread.sleep(env.config.tableDelayMillis);
            } catch (InterruptedException ignored) {}
            for (int player=0; player<tokens.length;player++){
                
                for (int i=0 ; i<tokens[player].length;i++){
                    
                    if(tokens[player][i]!=null &&tokens[player][i]==slot){
                        removeToken(player,slot);
                    }
                }  
            }
            cardToSlot[slotToCard[slot]]=null;
            slotToCard[slot]=null;
            env.ui.removeCard(slot);
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        boolean found=false;
        synchronized(tokenLock[slot]){
            if(slotToCard[slot] != null){
                for(int i=0;i<tokens[player].length&&!found;i++){
                        if(tokens[player][i]==null){
                        tokens[player][i]=slot; 
                        env.ui.placeToken(player, slot);
                        found=true;
                    }       
                }
            }
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        synchronized(tokenLock[slot]){
            if(slotToCard[slot] != null)
            for(int i=0;i<tokens[player].length;i++){
                if(tokens[player][i]!=null&&tokens[player][i]==slot){
                    tokens[player][i]=null;
                    env.ui.removeToken(player,slot);
                    return true;
                }
            }
        }
        return false;

    }

    public List<Integer> freeSlots (){
        List<Integer> freeSlotslList= new LinkedList<Integer>();
        for (int i=0 ; i<slotToCard.length ; i++){
            if (slotToCard[i]==null){
                freeSlotslList.add(i);
            }
        }
        return freeSlotslList;
    }  
    public int slotToCardInt(int slot){
        if (slotToCard[slot]==null)
            return -1; 
        return slotToCard[slot];
    }


    public int[][] getPlayerCardsAndSlots (int id){
        int [][] cardsAndSlots = new int[2][env.config.featureSize];
        for (int i=0;i<tokens[id].length;i++){
            if (tokens[id][i]==null){
                cardsAndSlots[1][i]=-1;
            }
            else{
                int slot = tokens [id][i];
                int card= slotToCardInt(slot);
                cardsAndSlots[0][i]=card;
                cardsAndSlots[1][i]=slot;
            }
        }
        return cardsAndSlots;
    }
    public boolean contains(int id,int slot){
        for (int i=0;i<tokens[id].length;i++){
            if(tokens[id][i]!=null && tokens[id][i]==(Integer)slot)
                return true;
        }
        return false;
    }

    public boolean isTokenFull(int id){
        int counter=0;
        for (int i=0;i<tokens[id].length;i++){
            if(tokens[id][i]!=null)
                counter++;
        }
        return counter==env.config.featureSize;
    }
}
