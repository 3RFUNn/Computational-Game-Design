package games.jaipurskeleton.stats;

import core.actions.AbstractAction;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.actions.TakeCards;
import games.jaipurskeleton.components.JaipurCard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JaipurMetrics implements IMetricsCollection {


    static public class RoundScoreDifference extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            JaipurGameState gs = (JaipurGameState) e.state;
            double scoreDiff = 0;
            for (int i = 0; i < gs.getNPlayers() - 1; i++) {
                scoreDiff += Math.abs(gs.getPlayerScores().get(i).getValue() - gs.getPlayerScores().get(i + 1).getValue());
            }
            scoreDiff /= (gs.getNPlayers() - 1);
            records.put("ScoreDiff", scoreDiff);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("ScoreDiff", Double.class);
            return columns;
        }
    }

    static public class PurchaseFromMarket extends AbstractMetric {
        JaipurCard.GoodType[] goodTypes;

        public PurchaseFromMarket() {
            super();
            goodTypes = JaipurCard.GoodType.values();
        }

        public PurchaseFromMarket(String[] args) {
            super(args);
            goodTypes = new JaipurCard.GoodType[args.length];
            for (int i = 0; i < args.length; i++) {
                goodTypes[i] = JaipurCard.GoodType.valueOf(args[i]);
            }
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractAction action = e.action;
            if (action instanceof TakeCards tc) {
                for (JaipurCard.GoodType type : goodTypes) {
                    if (tc.howManyPerTypeTakeFromMarket.containsKey(type))
                        records.put("Purchase - " + type.name(), tc.howManyPerTypeTakeFromMarket.get(type));
                }
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (JaipurCard.GoodType type : goodTypes) {
                columns.put("Purchase - " + type.name(), Integer.class);
            }
            return columns;
        }
    }

    static public class WinRoundAsFirstPlayer extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            JaipurGameState jgs = (JaipurGameState) e.state;
            records.put("WinAsFirstPlayer", jgs.getOrdinalPosition(e.state.getFirstPlayer()) == 1);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("WinAsFirstPlayer", Boolean.class);
            return columns;
        }
    }

    static public class ActionType extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractAction action = e.action;
            if (action instanceof TakeCards tc) {
                if (tc.howManyPerTypeGiveFromHand == null) {
                    if (!tc.howManyPerTypeTakeFromMarket.containsKey(JaipurCard.GoodType.Camel)) {
                        records.put("Take a card", 1);
                    } else {
                        records.put("Take the camels", 1);
                    }
                } else {
                    records.put("Take multiple", 1);
                }
            } else {
                records.put("Sell goods", 1);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Take a card", Integer.class);
            columns.put("Take the camels", Integer.class);
            columns.put("Take multiple", Integer.class);
            columns.put("Sell goods", Integer.class);
            return columns;
        }
    }

    public static class SecondPlayerWin extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            JaipurGameState gs = (JaipurGameState) e.state;
            double secondplayerwon = 0 ;
            secondplayerwon +=gs.getPlayerNRoundsWon().get(1).getValue();
            records.put("SecondPlayerWin", secondplayerwon);

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("SecondPlayerWin", Double.class);
            return columns;
        }
    }
}
