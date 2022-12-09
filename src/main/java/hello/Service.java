package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * @author StarL
 */
public class Service {
    private static final Logger LOGGER
            = LoggerFactory.getLogger(Service.class);

    private static final String POSITION_FORMAT = "%d,%d";

    private Request.PlayerState myState;
    private Request.Arena arena;
    private Set<String> blocks;

    private static final Set<String> DANGER_ZONES = new HashSet<>();

    public Response strategy(Request request) {
        initialize(request);
        this.blocks = new HashSet<>();
        PriorityQueue<Request.PlayerState> players = new PriorityQueue<>(arena.state.size(), priority());
        updateData(players);
        LOGGER.info(String.format("nearest player: [%d,%d]", players.peek().x, players.peek().y));

        if (isInDangerZones()) return leaveDangerZones();
        if (myState.wasHit) {
            Request.PlayerState attacker = attackedBy(players);
            if (attacker == null) return Response.LEFT;
            LOGGER.info(String.format("attacker: [%d,%d]", attacker.x, attacker.y));
            if (!Direction.isFaceToFace(attacker.d, myState.d) && canMove()) {
                return Response.MOVE;
            }
            return Response.LEFT;
        }
        if (isEnemyInAttackRange(players)) return Response.ATTACK;
        return findNearestPlayer(players);
    }

    private Response leaveDangerZones() {
        if(!canMove()) return Response.LEFT;
        return Response.MOVE;
    }

    private void initialize(Request request) {
        this.myState = request.arena.state.get(request._links.self.href);
        this.arena = request.arena;
        if(DANGER_ZONES.size() == 0) setDangerZone(request.arena.dims);
    }

    private void setDangerZone(List<Integer> dims) {
        int x = dims.get(0);
        int y = dims.get(1);
        DANGER_ZONES.add(String.format(POSITION_FORMAT, 0, 0));
        DANGER_ZONES.add(String.format(POSITION_FORMAT, x - 1, y - 1));
        DANGER_ZONES.add(String.format(POSITION_FORMAT, x - 1, 0));
        DANGER_ZONES.add(String.format(POSITION_FORMAT, 0, y - 1));
    }

    private boolean isInDangerZones() {
        LOGGER.info(String.format("my location: [%d,%d]", myState.x, myState.y));
        LOGGER.info(String.format("danger zones: %s", DANGER_ZONES));
        return DANGER_ZONES.contains(String.format(POSITION_FORMAT, myState.x, myState.y));
    }

    private Response findNearestPlayer(PriorityQueue<Request.PlayerState> players) {
        Request.PlayerState player = players.poll();
        if (player == null) {
            if (!canMove()) {
                return Response.LEFT;
            }
            return Response.MOVE;
        }
        int x = player.x - myState.x;
        int y = player.y - myState.y;
        if (!canMove()) {
            return Response.LEFT;
        }
        if (distanceToPlayer(myState, true, player) < distanceToPlayer(myState, player)) return Response.MOVE;
        else return Response.LEFT;
    }

    private int distanceToPlayer(Request.PlayerState myState, Request.PlayerState player) {
        return distanceToPlayer(myState, false, player);
    }

    private int distanceToPlayer(Request.PlayerState myState, boolean move, Request.PlayerState player) {
        int moveX = move ? myState.d.getMove()[0] : 0;
        int moveY = move ? myState.d.getMove()[1] : 0;
        return Math.abs(player.x + moveX - myState.x) + Math.abs(player.y + moveY - myState.y);
    }

    private boolean canMove() {
        return canMove(myState.x, myState.y, myState.d);
    }

    private boolean canMove(int x, int y, Direction d) {
        x = x + d.getMove()[0];
        y = y + d.getMove()[1];
        List<Integer> dims = arena.dims;
        return x >= 0 && x < dims.get(0) && y >= 0 && y < dims.get(1) && !this.blocks.contains(String.format(POSITION_FORMAT, x, y));
    }

    private void updateData(PriorityQueue<Request.PlayerState> pq) {
        for (Request.PlayerState player : this.arena.state.values()) {
            player.d = Direction.findDirection(player.direction);
            blocks.add(String.format(POSITION_FORMAT, player.x, player.y));
            if (myState == player) continue;
            pq.offer(player);
        }
    }

    private Request.PlayerState attackedBy(PriorityQueue<Request.PlayerState> players) {
        while (!players.isEmpty()) {
            Request.PlayerState player = players.poll();
            if (isInAttackRange(player, myState)) return player;
        }
        return null;
    }

    private Comparator<Request.PlayerState> priority() {
        return ((o1, o2) -> {
            int o1Score = Math.abs(myState.x - o1.x) + Math.abs(myState.y - o1.y);
            int o2Score = Math.abs(myState.x - o2.x) + Math.abs(myState.y - o2.y);
            return o1Score - o2Score;
        });
    }

    private boolean isInAttackRange(Request.PlayerState attacker, Request.PlayerState attacked) {
        Direction attackerDirection = attacker.d;
        int xBound = Math.max(attacker.x + attackerDirection.getAttackRange()[0], 0);
        int yBound = Math.max(attacker.y + attackerDirection.getAttackRange()[1], 0);
        if (attacker.x - attacked.x == 0) {
            return Math.min(yBound, attacker.y) <= attacked.y && attacked.y <= Math.max(yBound, attacker.y);
        }else if (attacker.y - attacked.y == 0) {
            return Math.min(xBound, attacker.x) <= attacked.x && attacked.x <= Math.max(xBound, attacker.x);
        }
        return false;
    }

    private boolean isEnemyInAttackRange(PriorityQueue<Request.PlayerState> players) {
        for(Request.PlayerState player: players) {
            if (isInAttackRange(myState, player)) {
                LOGGER.info(String.format("my location: [%d,%d]", myState.x, myState.y));
                LOGGER.info(String.format("attaced location: [%d,%d]", player.x, player.y));
                return true;
            }
        }
        return false;
    }

    enum Response {
        MOVE("F"), RIGHT("R"), LEFT("L"), ATTACK("T");
        private final String command;

        Response(String command) {

            this.command = command;
        }

        public String getCommand() {
            return command;
        }

    }

    enum Direction {
        NORTH("N", new int[]{0, -3}, new int[]{0, -1}, 0),
        EAST("E", new int[]{3, 0}, new int[]{1,0},1),
        SOUTH("S", new int[]{0, 3}, new int[]{0,1}, 2),
        WEST("W", new int[]{-3, 0}, new int[]{-1,0},3);

        private final String symbol;
        private final int[] attackRange;
        private final int[] move;
        private final int val;

        Direction(String symbol, int[] attackRange, int[] move, int val) {
            this.symbol = symbol;
            this.attackRange = attackRange;
            this.move = move;
            this.val = val;
        }

        public static Direction findDirection(String val) {
            switch (val) {
                case "N":
                    return NORTH;
                case "W":
                    return WEST;
                case "E":
                    return EAST;
                case "S":
                default:
                    return SOUTH;
            }
        }

        public Direction nextDirection(Response response) {
            switch (response) {
                case LEFT:
                    return values()[(this.val + 3) % 4];
                case RIGHT:
                    return values()[(this.val + 1) % 4];
                default:
                    return this;
            }
        }

        public static boolean isFaceToFace(Direction d1, Direction d2) {
            return (d1.val + 2) % values().length == d2.val;
        }

        public String getSymbol() {
            return symbol;
        }

        public int[] getAttackRange() {
            return attackRange;
        }

        public int[] getMove() {
            return move;
        }
    }
}
