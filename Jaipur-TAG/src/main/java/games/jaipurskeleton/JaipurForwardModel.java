package games.jaipurskeleton;

import com.google.common.collect.ImmutableMap;
import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.jaipurskeleton.actions.SellCards;
import games.jaipurskeleton.actions.TakeCards;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.components.JaipurToken;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.jaipurskeleton.components.JaipurCard.GoodType.Camel;
import static games.jaipurskeleton.components.JaipurCard.GoodType.Jade;

/**
 * Jaipur rules: <a href="https://www.fgbradleys.com/rules/rules2/Jaipur-rules.pdf">pdf here</a>
 */
public class JaipurForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        JaipurGameState gs = (JaipurGameState) firstState;
        JaipurParameters jp = (JaipurParameters) firstState.getGameParameters();

        // Initialize variables
        gs.market = new HashMap<>();
        for (JaipurCard.GoodType gt : JaipurCard.GoodType.values()) {
            // 5 cards in the market
            gs.market.put(gt, new Counter(0, 0, jp.getMarketSize(), "Market: " + gt));
        }

        gs.drawDeck = new Deck<>("Draw deck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.playerHands = new ArrayList<>();
        gs.playerHerds = new ArrayList<>();
        gs.nGoodTokensSold = new Counter(0, 0, JaipurCard.GoodType.values().length, "N Good Tokens Fully Sold");
        gs.goodTokens = new HashMap<>();
        gs.bonusTokens = new HashMap<>();

        // Initialize player scores, rounds won trackers, and other player-specific variables
        gs.playerScores = new ArrayList<>();
        gs.playerNRoundsWon = new ArrayList<>();
        gs.playerNGoodTokens = new ArrayList<>();
        gs.playerNBonusTokens = new ArrayList<>();
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerScores.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " score"));
            gs.playerNRoundsWon.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n rounds won"));
            gs.playerNGoodTokens.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n good tokens"));
            gs.playerNBonusTokens.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n bonus tokens"));

            // Create herds, maximum 11 camels in the game
            gs.playerHerds.add(new Counter(0, 0, 11, "Player " + i + " herd"));

            Map<JaipurCard.GoodType, Counter> playerHand = new HashMap<>();
            for (JaipurCard.GoodType gt : JaipurCard.GoodType.values()) {
                if (gt != Camel) {
                    // Hand limit of 7
                    playerHand.put(gt, new Counter(0, 0, jp.getHandLimit(), "Player " + i + " hand: " + gt));
                }
            }
            gs.playerHands.add(playerHand);
        }

        // Set up the first round
        setupRound(gs, jp);
    }

    private void setupRound(JaipurGameState gs, JaipurParameters jp) {
        Random r = new Random(jp.getRandomSeed());

        // Market initialisation
        // Place 3 camel cards in the market
        for (JaipurCard.GoodType gt : JaipurCard.GoodType.values()) {
            if (gt == Camel) {
                gs.market.get(gt).setValue(jp.getNCamelsInMarketAtStart());
            } else {
                gs.market.get(gt).setValue(0);
            }
        }

        // Create deck of cards
        gs.drawDeck.clear();
        for (JaipurCard.GoodType gt : JaipurCard.GoodType.values()) {
            if (gt == Jade && !jp.useJadeCards) {
                continue;
            }
            int count = jp.getDrawDeckCards().get(gt);
            if (gt == Camel) {
                // 11 Camel cards, - 3 already in the market
                count -= jp.getNCamelsInMarketAtStart();
            }
            JaipurCard card = new JaipurCard(gt);
            for (int i = 0; i < count; i++) {
                gs.drawDeck.add(card);
            }
        }
        gs.drawDeck.shuffle(r);

        // Deal N cards to each player
        for (int i = 0; i < gs.getNPlayers(); i++) {
            Map<JaipurCard.GoodType, Counter> playerHand = gs.playerHands.get(i);

            // First, reset
            gs.playerHerds.get(i).setValue(0);
            for (JaipurCard.GoodType gt : JaipurCard.GoodType.values()) {
                if (gt != Camel) {
                    playerHand.get(gt).setValue(0);
                }
            }

            // Deal cards
            for (int j = 0; j < jp.getNCardsInHandAtStart(); j++) {  // 5 cards in hand
                JaipurCard card = gs.drawDeck.draw();

                // If camel, it goes into the herd instead
                if (card.goodType == Camel) {
                    gs.playerHerds.get(i).increment();
                } else {
                    // Otherwise, into the player's hand
                    playerHand.get(card.goodType).increment();
                }
            }
        }

        // Take first 2 cards from the deck and place them face up in the market.
        for (int i = 0; i < jp.getMarketSize() - jp.getNCamelsInMarketAtStart(); i++) {
            JaipurCard card = gs.drawDeck.draw();
            gs.market.get(card.goodType).increment();
        }

        // Initialize tokens
        gs.nGoodTokensSold.setValue(0);
        gs.goodTokens.clear();
        gs.bonusTokens.clear();

        // Initialize the good tokens
        for (JaipurCard.GoodType type : jp.goodTokensProgression.keySet()) {
            if (type == Jade && !jp.useJadeCards) {
                continue;
            }
            Integer[] progression = jp.goodTokensProgression.get(type);
            Deck<JaipurToken> tokenDeck = new Deck<>(" Good tokens " + type, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
            for (int p : progression) {
                tokenDeck.add(new JaipurToken(type, p));
            }
            gs.goodTokens.put(type, tokenDeck);
        }

        // Initialize the bonus tokens
        for (int nSold : jp.bonusTokensAvailable.keySet()) {
            Integer[] values = jp.bonusTokensAvailable.get(nSold);
            Deck<JaipurToken> tokenDeck = new Deck<>("Bonus tokens " + nSold, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            for (int v : values) {
                tokenDeck.add(new JaipurToken(v));
            }
            // Shuffle
            tokenDeck.shuffle(r);
            gs.bonusTokens.put(nSold, tokenDeck);
        }

        // Reset player-specific variables that don't persist between rounds
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerScores.get(i).setValue(0);
            gs.playerNGoodTokens.get(i).setValue(0);
            gs.playerNBonusTokens.get(i).setValue(0);
        }

        // First player
        gs.setFirstPlayer(0);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = gameState.getCurrentPlayer();
        JaipurGameState jgs = (JaipurGameState) gameState;
        JaipurParameters jp = (JaipurParameters) gameState.getGameParameters();
        Map<JaipurCard.GoodType, Counter> playerHand = jgs.playerHands.get(currentPlayer);

        // Can sell cards from hand
        // TODO: Follow lab 1 instructions (Section 3.1) to fill in this method here.
        for (JaipurCard.GoodType gt : playerHand.keySet()) {
            if (playerHand.get(gt).getValue() >= jp.goodNCardsMinimumSell.get(gt)) {
                // Can sell this good type ! We can choose any number of cards to
                // sell of this type between minimum and how many we have
                for (int n = jp.goodNCardsMinimumSell.get(gt); n <= playerHand.get(gt).getValue(); n++) {
                    actions.add(new SellCards(gt, n));
                }
            }
        }

        // Can take cards from the market, respecting hand limit
        // Option C: Take all camels, they don't count towards hand limit
        // TODO 1: Check how many camel cards are in the market. If more than 0, construct one TakeCards action object and add it to the `actions` ArrayList. (The `howManyPerTypeGiveFromHand` argument should be null)
        int camelCount = jgs.market.get(Camel).getValue();
        if (camelCount > 0) {
            actions.add(new TakeCards(ImmutableMap.of(Camel, camelCount), null, currentPlayer));
        }

        int nCardsInHand = 0;
        for (JaipurCard.GoodType gt : playerHand.keySet()) {
            nCardsInHand += playerHand.get(gt).getValue();
        }

        // Check hand limit for taking non-camel cards in hand
        if (nCardsInHand < jp.getHandLimit()) {
            // Option B: Take a single (non-camel) card from the market
            // TODO 2: For each good type in the market, if there is at least 1 of that type (which is not a Camel), construct one TakeCards action object to take 1 of that type from the market, and add it to the `actions` ArrayList. (The `howManyPerTypeGiveFromHand` argument should be null)
            for (JaipurCard.GoodType gt : jgs.market.keySet()) {
                if (gt == Camel) {
                    continue;
                }
                if (jgs.market.get(gt).getValue() > 0) {
                    actions.add(new TakeCards(ImmutableMap.of(gt, 1), null, currentPlayer));
                }
            }
        }

        // Option A: Take several (non-camel) cards and replenish with cards of different types from hand (or with camels)
        // TODO (Advanced, bonus, optional): Calculate legal option A variations
        List<Integer> marketCards = new ArrayList<>();
        for (JaipurCard.GoodType gt : jgs.market.keySet()) {
            if (gt == Camel) {
                continue;
            }
            for (int i = 0; i < jgs.market.get(gt).getValue(); i++) {
                marketCards.add(gt.ordinal());
            }
        }
        List<Integer> myCards = new ArrayList<>();
        int handCount = 0;
        for (JaipurCard.GoodType gt : jgs.playerHands.get(currentPlayer).keySet()) {
            for (int i = 0; i < jgs.playerHands.get(currentPlayer).get(gt).getValue(); i++) {
                myCards.add(gt.ordinal());
                handCount++;
            }
        }
        for (int i = 0; i < jgs.playerHerds.get(currentPlayer).getValue() && i < jp.getHandLimit() - handCount; i++) {
            myCards.add(Camel.ordinal());
        }
        for (int i = 2; i <= marketCards.size(); i++) {
            List<int[]> takeChoices = Utils.generateCombinations(marketCards.stream().mapToInt(Integer::intValue).toArray(), i);
            takeChoices = removeDuplicates(takeChoices);
            for (int[] takeChoice : takeChoices) {
                Set<Integer> sTakeChoice = new HashSet<>();
                Map<JaipurCard.GoodType, Integer> howManyPerTypeTakeFromMarket = new HashMap<>();
                for (int j = 0; j < i; j++) {
                    sTakeChoice.add(takeChoice[j]);
                    howManyPerTypeTakeFromMarket.put(JaipurCard.GoodType.values()[takeChoice[j]],
                            howManyPerTypeTakeFromMarket.getOrDefault(JaipurCard.GoodType.values()[takeChoice[j]], 0) + 1);
                }
                List<Integer> myCardsAvailable = new ArrayList<>();
                for (Integer myCard : myCards) {
                    if (sTakeChoice.contains(myCard)) {
                        continue;
                    } else {
                        myCardsAvailable.add(myCard);
                    }
                }
                List<int[]> replaceChoices = Utils.generateCombinations(myCardsAvailable.stream().mapToInt(Integer::intValue).toArray(), i);
                replaceChoices = removeDuplicates(replaceChoices);
                for (int[] replaceChoice : replaceChoices) {
                    Map<JaipurCard.GoodType, Integer> howManyPerTypeGiveFromHand = new HashMap<>();
                    for (int j = 0; j < i; j++) {
                        howManyPerTypeGiveFromHand.put(JaipurCard.GoodType.values()[replaceChoice[j]],
                                howManyPerTypeGiveFromHand.getOrDefault(JaipurCard.GoodType.values()[replaceChoice[j]], 0) + 1);
                    }
                    actions.add(new TakeCards(ImmutableMap.copyOf(howManyPerTypeTakeFromMarket), ImmutableMap.copyOf(howManyPerTypeGiveFromHand), currentPlayer));
                }
            }
        }

        return actions;
    }

    List<int[]> removeDuplicates(List<int[]> choices) {
        Set<String> seen = new HashSet<>();
        return choices.stream()
                .filter(choice -> seen.add(Arrays.toString(choice)))
                .collect(Collectors.toList());
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        if (currentState.isActionInProgress()) return;

        // Check game end
        JaipurGameState jgs = (JaipurGameState) currentState;
        JaipurParameters jp = (JaipurParameters) currentState.getGameParameters();
        if (actionTaken instanceof TakeCards && ((TakeCards) actionTaken).isTriggerRoundEnd() || jgs.nGoodTokensSold.getValue() == jp.nGoodTokensEmptyRoundEnd) {
            // Round end!
            endRound(currentState);

            // Check most camels, add extra points
            int maxCamels = 0;
            HashSet<Integer> pIdMaxCamels = new HashSet<>();
            for (int i = 0; i < jgs.getNPlayers(); i++) {
                if (jgs.playerHerds.get(i).getValue() > maxCamels) {
                    maxCamels = jgs.playerHerds.get(i).getValue();
                    pIdMaxCamels.clear();
                    pIdMaxCamels.add(i);
                } else if (jgs.playerHerds.get(i).getValue() == maxCamels) {
                    pIdMaxCamels.add(i);
                }
            }
            if (pIdMaxCamels.size() == 1) {
                // Exactly 1 player has most camels, they get bonus. If tied, nobody gets bonus.
                int player = pIdMaxCamels.iterator().next();
                jgs.playerScores.get(player).increment(jp.nPointsMostCamels);
                if (jgs.getCoreGameParameters().recordEventHistory) {
                    jgs.recordHistory("Player " + player + " earns the " + jp.nPointsMostCamels + " Camel bonus points (" + maxCamels + " camels)");
                }
            }

            // Decide winner of round
            int roundsWon = 0;
            int winner = -1;
            StringBuilder scores = new StringBuilder();
            for (int p = 0; p < jgs.getNPlayers(); p++) {
                int o = jgs.getOrdinalPosition(p);
                scores.append(p).append(":").append(jgs.playerScores.get(p).getValue());
                if (o == 1) {
                    jgs.playerNRoundsWon.get(p).increment();
                    roundsWon = jgs.playerNRoundsWon.get(p).getValue();
                    winner = p;
                    scores.append(" (win)");
                }
                scores.append(", ");
            }
            scores.append(")");
            scores = new StringBuilder(scores.toString().replace(", )", ""));
            if (jgs.getCoreGameParameters().recordEventHistory) {
                jgs.recordHistory("Round scores: " + scores);
            }

            if (roundsWon == ((JaipurParameters) jgs.getGameParameters()).nRoundsWinForGameWin) {
                // Game over, this player won
                jgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                for (int i = 0; i < jgs.getNPlayers(); i++) {
                    if (i == winner) {
                        jgs.setPlayerResult(WIN_GAME, i);
                    } else {
                        jgs.setPlayerResult(LOSE_GAME, i);
                    }
                }
                return;
            }

            // Reset and set up for next round
            setupRound(jgs, jp);

        } else {
            // It's next player's turn
            endPlayerTurn(jgs);
        }
    }
}