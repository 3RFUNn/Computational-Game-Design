package games.jaipurskeleton.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.components.JaipurToken;

import java.util.Objects;

/**
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
 */

public class SellCards extends AbstractAction {
    final JaipurCard.GoodType goodType;
    final int howMany;

    public SellCards(JaipurCard.GoodType goodType, int howMany) {
        this.goodType = goodType;
        this.howMany = howMany;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        JaipurGameState state = (JaipurGameState) gs;
        int player = gs.getCurrentPlayer();

        removeCardsFromHand(state, player);
        collectGoodTokens(state, player);
        collectBonusTokens(state, player);

        return true;
    }

    private void removeCardsFromHand(JaipurGameState state, int player) {
        state.getPlayerHands().get(player).get(goodType).decrement(howMany);
    }

    private void collectGoodTokens(JaipurGameState state, int player) {
        Deck<JaipurToken> tokens = state.getGoodTokens().get(goodType);
        boolean hadTokens = tokens.getSize() > 0;

        while (tokens.getSize() > 0 && state.getPlayerNGoodTokens().get(player).getValue() < howMany) {
            JaipurToken token = tokens.draw();
            state.getPlayerScores().get(player).increment(token.tokenValue);
            state.getPlayerNGoodTokens().get(player).increment();
        }

        if (hadTokens && tokens.getSize() == 0) {
            state.getnGoodTokensSold().increment();
        }
    }

    private void collectBonusTokens(JaipurGameState state, int player) {
        if (!state.getBonusTokens().containsKey(howMany)) {
            return;
        }

        Deck<JaipurToken> bonusTokens = state.getBonusTokens().get(howMany);
        if (bonusTokens.getSize() > 0) {
            JaipurToken token = bonusTokens.draw();
            state.getPlayerScores().get(player).increment(token.tokenValue);
            state.getPlayerNBonusTokens().get(player).increment();
        }
    }

    @Override
    public SellCards copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SellCards)) return false;
        SellCards sellCards = (SellCards) o;
        return howMany == sellCards.howMany && goodType == sellCards.goodType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(goodType, howMany);
    }

    @Override
    public String toString() {
        return "Sell " + howMany + " " + goodType + " cards";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}