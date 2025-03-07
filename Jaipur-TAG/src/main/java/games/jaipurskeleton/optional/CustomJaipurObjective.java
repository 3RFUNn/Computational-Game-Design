package games.jaipurskeleton.optional;

import core.Game;
import core.interfaces.IGameHeuristic;
import games.jaipurskeleton.JaipurGameState;
import java.util.List;
import java.util.stream.Collectors;

public class CustomJaipurObjective implements IGameHeuristic {

    @Override
    public double evaluateGame(Game game) {
        // Retrieve the Jaipur game state
        JaipurGameState state = (JaipurGameState) game.getGameState();

        // Feature 1: Calculate absolute score difference between two players
        List<Integer> scores = state.getPlayerScores().stream()
                .map(scoreObj -> scoreObj.getValue())
                .collect(Collectors.toList());
        double scoreDiff = Math.abs(scores.get(0) - scores.get(1));

        // Feature 2: Total rounds played (using rounds won by both players)
        int roundsPlayed = state.getPlayerNRoundsWon().get(0).getValue() +
                state.getPlayerNRoundsWon().get(1).getValue();

        // Feature 3: Bonus if the first player wins (assumed ordinal position 1)
        boolean firstPlayerWon = state.getOrdinalPosition(state.getFirstPlayer()) == 1;
        double firstPlayerBonus = firstPlayerWon ? 10 : 0;

        // Combine features using a weighted sum formula
        double objective = scoreDiff + roundsPlayed - firstPlayerBonus;
        return objective;
    }
}