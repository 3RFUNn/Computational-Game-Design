package games.jaipurskeleton.actions;

import com.google.common.collect.ImmutableMap;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.components.JaipurCard;

import java.util.Objects;

public class TakeCards extends AbstractAction {

    public final ImmutableMap<JaipurCard.GoodType, Integer> howManyPerTypeTakeFromMarket;
    public final ImmutableMap<JaipurCard.GoodType, Integer> howManyPerTypeGiveFromHand;
    final int playerID;

    boolean triggerRoundEnd;

    public TakeCards(ImmutableMap<JaipurCard.GoodType, Integer> howManyPerTypeTakeFromMarket, ImmutableMap<JaipurCard.GoodType, Integer> howManyPerTypeGiveFromHand, int playerID) {
        this.howManyPerTypeTakeFromMarket = howManyPerTypeTakeFromMarket;
        this.howManyPerTypeGiveFromHand = howManyPerTypeGiveFromHand;
        this.playerID = playerID;
    }

    /**
     * <p>Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.</p>
     * <p>In extended sequences, this function makes a call to the
     * {@link AbstractGameState#setActionInProgress(IExtendedSequence)} method with the argument <code>`this`</code>
     * to indicate that this action has multiple steps and is now in progress. This call could be wrapped in an <code>`if`</code>
     * statement if sometimes the action simply executes an effect in one step, or all parameters have values associated.</p>
     *
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        JaipurGameState jgs = (JaipurGameState) gs;

        if (howManyPerTypeTakeFromMarket.size() == 1) {
            JaipurCard.GoodType goodType = howManyPerTypeTakeFromMarket.keySet().iterator().next();
            int howMany = howManyPerTypeTakeFromMarket.get(goodType);
            if (goodType == JaipurCard.GoodType.Camel) {
                // Option C: Take ALL the camels.

                // TODO 1: Increment player herds by the number of camels in the market
                jgs.getPlayerHerds().get(playerID).increment(howMany);
                // TODO 1: Remove all camels from the market
                jgs.getMarket().get(goodType).decrement(howMany);
                // TODO 1: Refill market with cards from the draw deck, to required market size
                for (int i = 0; i < howMany; i++) {
                    // TODO 1: If the draw deck becomes empty when trying to draw a new card, set the `triggerRoundEnd` flag to true
                    if (jgs.getDrawDeck().getSize() == 0) {
                        triggerRoundEnd = true;
                        return true;
                    }
                    JaipurCard card = jgs.getDrawDeck().draw();
                    jgs.getMarket().get(card.goodType).increment();
                }
                return true;

            } else if (howMany == 1) {
                // Option B: Take 1 single good

                // TODO 2: Increment the number of cards the player has of this type (`goodType`) by 1
                jgs.getPlayerHands().get(playerID).get(goodType).increment(howMany);
                // TODO 2: Reduce the number of cards in the market of this type by 1
                jgs.getMarket().get(goodType).decrement(howMany);
                // TODO 2: Draw a new card from the draw deck and increment the corresponding type in the market by 1
                // TODO 2: If the draw deck becomes empty when trying to draw a new card, set the `triggerRoundEnd` flag to true
                if (jgs.getDrawDeck().getSize() == 0) {
                    triggerRoundEnd = true;
                    return true;
                }
                JaipurCard card = jgs.getDrawDeck().draw();
                jgs.getMarket().get(card.goodType).increment();
                return true;
            }
        }

        // Option A: Take several (non-camel) goods, replace the same number with cards from hand or camels
        for (JaipurCard.GoodType gt : howManyPerTypeTakeFromMarket.keySet()) {
            int count = howManyPerTypeTakeFromMarket.get(gt);
            jgs.getPlayerHands().get(playerID).get(gt).increment(count);
            jgs.getMarket().get(gt).decrement(count);
        }
        for (JaipurCard.GoodType gt : howManyPerTypeGiveFromHand.keySet()) {
            int count = howManyPerTypeGiveFromHand.get(gt);
            if (gt == JaipurCard.GoodType.Camel) {
                jgs.getPlayerHerds().get(playerID).decrement(count);
            } else {
                jgs.getPlayerHands().get(playerID).get(gt).decrement(count);
            }
            jgs.getMarket().get(gt).increment(count);
        }

        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public TakeCards copy() {
        TakeCards copy = new TakeCards(howManyPerTypeTakeFromMarket, howManyPerTypeGiveFromHand, playerID);
        copy.triggerRoundEnd = triggerRoundEnd;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TakeCards takeCards)) return false;
        return playerID == takeCards.playerID
                && triggerRoundEnd == takeCards.triggerRoundEnd
                && Objects.equals(howManyPerTypeTakeFromMarket, takeCards.howManyPerTypeTakeFromMarket)
                && Objects.equals(howManyPerTypeGiveFromHand, takeCards.howManyPerTypeGiveFromHand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(howManyPerTypeTakeFromMarket, howManyPerTypeGiveFromHand, playerID, triggerRoundEnd);
    }

    @Override
    public String toString() {
        return "Take cards: " + howManyPerTypeTakeFromMarket.toString() + (howManyPerTypeGiveFromHand != null ? " (replenish with: " + howManyPerTypeGiveFromHand + ")" : "");
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public boolean isTriggerRoundEnd() {
        return triggerRoundEnd;
    }
}