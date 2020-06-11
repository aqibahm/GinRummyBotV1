import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;


/**
 * Implements a random dummy Gin Rummy player that has the following trivial, poor play policy: 
 * Ignore opponent actions and cards no longer in play.
 * Draw face up card only if it becomes part of a meld.  Draw face down card otherwise.
 * Discard a highest ranking unmelded card without regard to breaking up pairs, etc.
 * Knock as early as possible.
 *
 * @author Todd W. Neller
 * @version 1.0

Copyright (C) 2020 Todd Neller

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Information about the GNU General Public License is available online at:
http://www.gnu.org/licenses/
To receive a copy of the GNU General Public License, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.

 */
public class Aja6082GinRummyPlayer implements GinRummyPlayer {

	static class state{
		//State data
		//The current state:
		static int turn;
		//No. of cards remaining in face-down deck:
		static int remainingDeck;
		//The top card of the discard pile:
		static Card faceUp;
		//List containing seen cards:
		static ArrayList<Card> seen = new ArrayList<>();
		//List of cards that were seen and the opponent drew:
		static ArrayList<Card> opponentCards = new ArrayList<>();
		//List of cards that the opponent discarded:
		static ArrayList<Card> opponentDiscards = new ArrayList<>();

		//State setters
		static void increaseTurn(){
			turn++;
		}

		static void setTurn(int val){
			turn = val;
		}

		static void setRemainingDeck(int val){
			remainingDeck = val;
		}

		static void decreaseRemainingDeck(){
			remainingDeck--;
		}

		static void increaseRemainingDeck(){
			remainingDeck++;
		}

		static void setFaceUp(Card card){
			faceUp = card;
		}

		static void addSeen(Card seenCard){
			seen.add(seenCard);
		}

		static void addOpponentCard(Card opponentCard){
			opponentCards.add(opponentCard);
		}

		static void removeOpponentCard(Card opponentCard){
			opponentCards.remove(opponentCard);
		}

		static void addOpponentDiscard(Card opponentCard){
			opponentDiscards.add(opponentCard);
		}

		//Method to reset all values at the end of a round
		static void reset(){
			setTurn(0);
			setRemainingDeck(52);
			seen.clear();
			opponentCards.clear();
			opponentDiscards.clear();
		}

		//State getters
		static int getTurn(){
			return turn;
		}

		static int getRemainingDeck(){
			return remainingDeck;
		}

		static Card getFaceUp(){
			return faceUp;
		}

		static ArrayList<Card> getSeen(){
			return seen;
		}

		static ArrayList<Card> getOpponentCards(){
			return opponentCards;
		}

		static ArrayList<Card> getOpponentDiscards(){
			return opponentDiscards;
		}
	}

	protected int playerNum;
	@SuppressWarnings("unused")
	protected int startingPlayerNum;
	protected ArrayList<Card> cards = new ArrayList<Card>();
	protected Random random = new Random();
	protected boolean opponentKnocked = false;
	Card faceUpCard, drawnCard;
	ArrayList<Long> drawDiscardBitstrings = new ArrayList<Long>();

	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards){
		ArrayList<Card> sampleCards = new ArrayList<>();
		//Initialize state of the game with turn = 0
		state.setTurn(0);
		//Initialize deckRemaining with 32, since both players get 20 cards
		state.setRemainingDeck(32);
		for(Card card: cards){
			//Initialize seen cards with the bot's hand
			state.addSeen(card);
		}
		this.playerNum = playerNum;
		this.startingPlayerNum = startingPlayerNum;
		this.cards.clear();
		for (Card card : cards) {
			this.cards.add(card);

		}
		opponentKnocked = false;
		drawDiscardBitstrings.clear();
		System.out.println(GinRummyUtil.cardsToBestMeldSets(state.getSeen()));
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// Return true if card would be a part of a meld, false otherwise.
		if(state.getTurn() == 0){
			//Not sure about this
			state.setFaceUp(card);
			state.addSeen(card);
			state.decreaseRemainingDeck();
		}
		this.faceUpCard = card;
		//Check whether this card would give a significant drop in deadwood
		int deadwoodDrop = MyGinRummyUtil.checkMeldAndDeadwoodDrop(cards, card);

		if(deadwoodDrop > 0){
			return true;
		}
		return false;

//		@SuppressWarnings("unchecked")
//		ArrayList<Card> newCards = (ArrayList<Card>) cards.clone();
//		newCards.add(card);
//		for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(newCards))
//			if (meld.contains(card))
//				return true;
//		return false;
	}

	@Override
	public void reportDraw(int playerNum, Card drawnCard) {
		// Ignore other player draws.  Add to cards if playerNum is this player.
		if (playerNum == this.playerNum) {
			cards.add(drawnCard);
			this.drawnCard = drawnCard;
			state.addSeen(drawnCard);
		}
		else{
			if(drawnCard == null)
				state.decreaseRemainingDeck();
			else {
				state.addOpponentCard(drawnCard);
				state.addSeen(drawnCard);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Card getDiscard() {
		//This method essentially decides which card would be the best discard
		// Discard a random card (not just drawn face up) leaving minimal deadwood points.
		int minDeadwood = Integer.MAX_VALUE;
		ArrayList<Card> candidateCards = new ArrayList<Card>();
		for (Card card : cards) {
			// Cannot draw and discard face up card.
			if (card == drawnCard && drawnCard == faceUpCard)
				continue;
			// Disallow repeat of draw and discard.
			ArrayList<Card> drawDiscard = new ArrayList<Card>();
			drawDiscard.add(drawnCard);
			drawDiscard.add(card);
			if (drawDiscardBitstrings.contains(GinRummyUtil.cardsToBitstring(drawDiscard)))
				continue;

			ArrayList<Card> remainingCards = (ArrayList<Card>) cards.clone();
			remainingCards.remove(card);
			ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(remainingCards);
			int deadwood = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(remainingCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), remainingCards);
			if (deadwood <= minDeadwood) {
				if (deadwood < minDeadwood) {
					minDeadwood = deadwood;
					candidateCards.clear();
				}
				candidateCards.add(card);
			}
		}

		ArrayList<Card> visited = new ArrayList<Card>();
		ArrayList<Card> singles = new ArrayList<Card>();
		ArrayList<Card> cardsToDelete = new ArrayList<Card>();

		for(Card candidate: candidateCards){
			int count = 0;
			if(!visited.contains(candidate)){
				for(Card card: cards){
					if(candidate.getRank() == card.getRank()){
						count++;
					}
				}
				visited.add(candidate);

				if(count == 1){
					singles.add(candidate);
					cardsToDelete.add(candidate);
				}
			}
		}

		for(Card delete: cardsToDelete){
			candidateCards.remove(delete);
		}

		Card temp;

		if(singles.isEmpty()){
			temp = candidateCards.get(0);
			for(Card candidate: candidateCards){
				if(candidate.getRank() > temp.getRank())
					temp = candidate;
			}
		}

		else{
			temp = singles.get(0);
			for(Card single: singles){
				if(single.getRank() > temp.getRank())
					temp = single;
			}
		}


		Card discard = temp;
		// Prevent future repeat of draw, discard pair.
		ArrayList<Card> drawDiscard = new ArrayList<Card>();
		drawDiscard.add(drawnCard);
		drawDiscard.add(discard);
		drawDiscardBitstrings.add(GinRummyUtil.cardsToBitstring(drawDiscard));
		return discard;
	}

	@Override
	public void reportDiscard(int playerNum, Card discardedCard) {
		// Ignore other player discards.  Remove from cards if playerNum is this player.
		if (playerNum == this.playerNum) {
			cards.remove(discardedCard);
			//Increase turn after this bot discards
			state.increaseTurn();
			state.setFaceUp(discardedCard);
		}
		else{//if opponent discards:
			state.setFaceUp(discardedCard);
			state.removeOpponentCard(discardedCard);
			state.addOpponentDiscard(discardedCard);
			state.addSeen(discardedCard);
		}
	}

	@Override
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		// Check if deadwood of maximal meld is low enough to go out. 
		ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(cards);
		if (!opponentKnocked && (bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD))
			return null;
		return bestMeldSets.isEmpty() ? new ArrayList<ArrayList<Card>>() : bestMeldSets.get(random.nextInt(bestMeldSets.size()));
	}

	@Override
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		// Melds ignored by simple player, but could affect which melds to make for complex player.
		state.reset();
		if (playerNum != this.playerNum)
			opponentKnocked = true;


	}

	@Override
	public void reportScores(int[] scores) {
		// Ignored by simple player, but could affect strategy of more complex player.
	}

	@Override
	public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
		// Ignored by simple player, but could affect strategy of more complex player.

	}

	@Override
	public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
		// Ignored by simple player, but could affect strategy of more complex player.
		state.reset();
	}

}
