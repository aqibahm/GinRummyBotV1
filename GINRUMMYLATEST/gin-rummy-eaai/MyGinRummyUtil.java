import java.util.ArrayList;

public class MyGinRummyUtil {

    //Given a hand determines the amount of deadwood points it contains
    public static int getBestDeadwoodPoints(ArrayList<Card> myCards){
        if(myCards.size() != 10)
            throw new IllegalArgumentException();
        ArrayList<ArrayList<ArrayList<Card>>> bestMeldConfigs = GinRummyUtil.cardsToBestMeldSets(myCards);
        return bestMeldConfigs.isEmpty() ? GinRummyUtil.getDeadwoodPoints(myCards) : GinRummyUtil.getDeadwoodPoints(bestMeldConfigs.get(0), myCards);
    }

    //Best deadwood points possible after a discard
    public static int getBestDeadwoodPointsAfterDiscard(ArrayList<Card> myCards){
        if(myCards.size() < 11)
            throw new IllegalArgumentException();
        int minDeadwood = Integer.MAX_VALUE;
        for(Card card: myCards){
            //Clone the hand and remove the current card
            ArrayList<Card> remainingCards = (ArrayList<Card>) myCards.clone();
            remainingCards.remove(card);
            ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(remainingCards);
            int deadwoodPoints = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(remainingCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), remainingCards);
            if(deadwoodPoints < minDeadwood)
                minDeadwood = deadwoodPoints;
        }

        return minDeadwood;
    }

    //How many cards would make gin given a current hand
    public static int numCardsMakingGin(ArrayList<Card> myCards, ArrayList<Card> seen){
        int count = 0;
        for(Card card: myCards){
            if(!(seen.contains(card))){
                //add card to my hand
                myCards.add(card);
                int val = getBestDeadwoodPoints(myCards);
                if(val == 0)
                    count++;
                myCards.remove(card);
            }
        }

        return count;
    }

    //Add a card to hand using bitstring operations
    public static long bitstringAddCardsToHand(long hand, Card card){
        long val = hand | 1L<<card.getId();
        return val;
    }

    //Check if a card is in hand using bitstring operations
    public static boolean bitStringCheckCardInHand(long hand, Card card){
        return ((hand & 1L<<card.getId()) != 0);
    }

    //Remove a card from hand using bitstring operations
    public static long bitStringRemoveCard(long hand, Card card){
        long val = hand ^ 1L<<card.getId();
        return val;
    }

    //Remove card from hand but only if it is not part of the hand using bitstring operations
    public static long bitStringRemoveCardIfNotInHand(long hand, Card card){
        long val = hand & (hand ^1L<<card.getId());
        return val;
    }

    //Methods from assignment
    //Determine whether opponent can make meld with a particular card
    //Strategy adopted: Check if the number of times a given card that can help the opponent make a meld or run with
    //the card object passed to the function appears in seen, and then do the same for the opponent's hand.
    //Simple comparison yields who holds the cards likely to result in the opponent's hand
    //This method doesn't work the way it should. Come back to it in a short bit
    public static boolean opponentCannotMakeMeld(ArrayList<Card> seen, ArrayList<Card> opponentCards, Card card){
        int seenMeldCount = 0;
        int seenRunCount = 0;
        int opponentMeldCount = 0;
        int opponentRunCount = 0;
        boolean canMakeMeld = false;

        for(Card seenCard: seen){
            //Seen meld check
            if(seenCard.getRank() == card.getRank())
                seenMeldCount++;

            //Seen run check
            if(seenCard.getSuit() == card.getSuit()){
                if(Math.abs(seenCard.getRank() - card.getRank()) <= 2)
                    seenRunCount++;
            }
        }

        for(Card opponentCard: opponentCards){
            //Opponent meld check
            if(opponentCard.getRank() == card.getRank())
                opponentMeldCount++;

            //Opponent run check
            if(opponentCard.getSuit() == card.getSuit()){
                if(Math.abs(opponentCard.getRank() - card.getRank()) == 1)
                    opponentRunCount++;
            }
        }

        if(seenMeldCount > opponentMeldCount || seenRunCount > opponentRunCount) {
            canMakeMeld = false;
        }

        return canMakeMeld;
    }

    //Determine whether a given card would form a new meld in the hand.
    //If so, find the drop in deadwood
    public static int checkMeldAndDeadwoodDrop(ArrayList<Card> myCards, Card card){
        int originalMinDeadwood = Integer.MAX_VALUE;
        int newMinDeadwood = Integer.MAX_VALUE;


        //newCards is the hand that contains the new card
        ArrayList<Card> newCards = (ArrayList<Card>) myCards.clone();
        newCards.add(card);

        //If no melds are formed after the card is added, don't go any further
        ArrayList<ArrayList<ArrayList<Card>>> bestMeldConfigs = GinRummyUtil.cardsToBestMeldSets(newCards);
        if(bestMeldConfigs.isEmpty())
            return 0;

        //Find the minimum deadwood for the original card configuration
        for(Card originalCard: myCards){
            ArrayList<Card> placeholder = (ArrayList<Card>) myCards.clone();
            placeholder.remove(originalCard);
            ArrayList<ArrayList<ArrayList<Card>>> bestMelds = GinRummyUtil.cardsToBestMeldSets(placeholder);
            int originalDeadwood = bestMelds.isEmpty() ? GinRummyUtil.getDeadwoodPoints(placeholder) : GinRummyUtil.getDeadwoodPoints(bestMelds.get(0), placeholder);
            if(originalDeadwood < originalMinDeadwood)
                originalMinDeadwood = originalDeadwood;
        }

        //Find the minimum deadwood for new card configuration
        for(Card iterCard: newCards){
            ArrayList<Card> remainingCards = (ArrayList<Card>) newCards.clone();
            remainingCards.remove(iterCard);
            int newDeadwood = GinRummyUtil.getDeadwoodPoints(bestMeldConfigs.get(0), remainingCards);
            if(newDeadwood < newMinDeadwood)
                newMinDeadwood = newDeadwood;
        }

        return (originalMinDeadwood - newMinDeadwood);
    }

    //Based on the cards that you have seen, determine which cards in your hand cannot be made into any melds
    //even after drawing one card
    //Set of cards that we can draw = opponentCards + unseenCards
    //These cards would be the ones that don't have either
    //(1)another card of the same rank
    //(2)another card of the same suit and consecutive rank
    //Another set of cards that should be considered is set of cards that are unreachable in the current
    //and all future game states, since they are in the discard pile
    //These cards would be the set obtained by finding the cards that are seen but are not in either
    //(1) our hand
    //(2) the opponent's hand
    //A good direction to start in could be finding the set of single cards
    public static ArrayList<Card> cannotFormMeldOneDraw(ArrayList<Card> myHand){
        //As of now, the method doesn't take into account unreachable cards
        //Instead, it just considers singly present cards that don't form a two card meld/run with any other card in current hand
        //Start by finding the set of single cards
        ArrayList<Card> singles = new ArrayList<>();
        //ArrayList<Card> doubles = new ArrayList<>();
        //Store cards that do not have other cards repeating their ranks or cards of same suit with consecutive ranks
        for(Card card: myHand){
            int meldCount = 0;
            int runCount = 0;
            ArrayList<Card> remaining = (ArrayList<Card>) myHand.clone();
            remaining.remove(card);
            for(Card remCard: remaining){
                if(remCard.getRank() == card.getRank())
                    meldCount++;
                if(remCard.getSuit() == card.getSuit()){
                    if(Math.abs(remCard.getRank() - card.getRank()) <= 1)
                        runCount++;
                }

                if(meldCount == 0 && runCount == 0)
                    singles.add(card);
            }
        }

        //Skipping unreachable cards logic for now
        return singles;
    }

    //Determine the cards that do not form a meld even after one draw
    public static ArrayList<Card> cannotFormMeldTwoDraws(ArrayList<Card> myHand, ArrayList<Card> seenCards){
        ArrayList<Card> singleCards = cannotFormMeldOneDraw(myHand);
        ArrayList<Card> unseen = new ArrayList<>();
        for(int i = 0; i < 52; i++){
            //Set of all cards in a deck
            unseen.add(Card.getCard(i));
        }
        //Remove cards that are seen
        for(Card card: seenCards){
            unseen.remove(card);
        }
        //Removal of any hand cards that might have remained in unseen cards for safe code run
        for(Card card: myHand){
            unseen.remove(card);
        }

        //Note that seen cards contains the cards that the opponent discarded just before our turn
        //So manually adding these cards to seen is not required

        //Find cards in unseen that have same ranks as the current card value in singles

        for(int i = 0; i < singleCards.size(); i++){
            //checking for melds
            int meldCount = 0;
            int runCount = 0;

            for(Card unseenCard: unseen){
                //Check for meld formation
                if(unseenCard.getRank() == singleCards.get(i).getRank())
                    meldCount++;

                //Check for run formation
                if(unseenCard.getSuit() == singleCards.get(i).getSuit()){
                    //Checks for first adjacent card for the current single card that contributes to a run
                    if(Math.abs(unseenCard.getRank() - singleCards.get(i).getRank()) == 1){
                        runCount++;
                        //Checks for second adjacent card for the current single card that contributes to a run
                        for(Card secondUnseenCard: unseen){
                            if(Math.abs(secondUnseenCard.getRank() - singleCards.get(i).getRank()) == 2)
                                runCount++;
                        }
                    }
                }
            }

            if(runCount >= 2 || meldCount >= 2)
                singleCards.remove(singleCards.get(i));
        }
        return singleCards;
    }

    //Determine average deadwood after picking the next card, which could be either a card in opponent's hand or an unseen card
    public static int averageDeadwoodAfterDraw(ArrayList<Card> myCards, ArrayList<Card> opponentDiscards, ArrayList<Card> opponentCards){
        //A card in the opponent's hand that you're sure about, would be a card that you've already seen
        //I can draw any of the unseen cards
        // I can draw any of the cards that I know that the opponent has
        ArrayList<Card> allCards = new ArrayList<>();
        for(int i = 0; i < 52; i++){
            //To initialize allCards with all cards in a standard French deck of cards
            allCards.add(Card.getCard(i));
        }

        //Remove cards that are in my hand
        //Remove cards that were discarded by the opponent
        //The remaining cards would be either the unseen cards or cards in the opponent's hand that we don't know about
        for(Card card: myCards){
            //This removed cards in my hand from allCards
            allCards.remove(card);
        }

        for(Card card: opponentDiscards){
            allCards.remove(card);
        }

        //Just for safe code execution, we should also add the cards that we know for sure that the opponent has
        for(Card card: opponentCards){
            allCards.add(card);
        }

        //Now allCards contains (1) all unseen cards, (2) all the cards that the opponent might have
        //We can run a for loop, find the best deadwood points for drawing each of the cards from the allCards list
        //Then divide it by the size of the allCards list, finally return this number
        int deadwoodCount = 0;
        for(Card card: allCards){
            ArrayList<Card> remainingCards = (ArrayList<Card>) myCards.clone();
            remainingCards.add(card);
            deadwoodCount += getBestDeadwoodPoints(remainingCards);
        }

        return (deadwoodCount)/allCards.size();
    }

}
