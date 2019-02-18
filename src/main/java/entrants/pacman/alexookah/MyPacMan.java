package entrants.pacman.alexookah;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.*;

import prediction.*;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.alexookah).
 */



@SuppressWarnings("Duplicates")
public class MyPacMan extends PacmanController {
    private MOVE myMove = MOVE.NEUTRAL;
    private static final int MIN_DISTANCE = 20;
    private static final int RESET_SEEN_AFTER = 2;
    private GhostPredictor mGhostPredictor;

    private ArrayList<Integer> pillsAvailable = new ArrayList<>();
    private ArrayList<Integer> powerPillsAvailable = new ArrayList<>();

    private HashMap<Constants.GHOST, GhostInfo> mGhostsInfo = new HashMap<>();
    private int currentLevel = -1;

    private class GhostInfo {
        public int lastSeenTimestamp;
        public int location;
        public int lastLocation;
        public boolean isEdible;
        public Constants.MOVE lastMove;
        public boolean isVisible;

        public GhostInfo(int lastSeenTimestamp, int location) {
            this.lastSeenTimestamp = lastSeenTimestamp;
            this.location = location;
            this.lastLocation = -1;
            this.isEdible = false;
            this.lastMove = MOVE.NEUTRAL;
            this.isVisible = false;
        };
    }

    private void initGhostsInfo() {
        mGhostPredictor = new GhostPredictor();

        for(Constants.GHOST ghost : Constants.GHOST.values()) {
            mGhostsInfo.put(ghost, new GhostInfo(0, -1));
        }

    }

    public MOVE getMove(Game game, long timeDue) {
        //Place your game logic here to play the game as Ms Pac-Man
        int current = game.getPacmanCurrentNodeIndex();
        int currentTimestamp = game.getCurrentLevelTime();


        // level change initialize stuff
        if (currentLevel != game.getCurrentLevel()) {
            initGhostsInfo();

            mGhostPredictor.clear(game.getCurrentMaze());

            //initialize pills available
            int[] pills = game.getPillIndices();
            pillsAvailable = new ArrayList<>();
            for (int i = 0; i < pills.length; i++) {
                pillsAvailable.add(pills[i]);
            }

            //initialize power pills available
            int[] powerPills = game.getPowerPillIndices();
            powerPillsAvailable = new ArrayList<>();
            for (int i = 0; i < powerPills.length; i++) {
                powerPillsAvailable.add(powerPills[i]);
            }

            currentLevel = game.getCurrentLevel();
        }

        if (game.wasPacManEaten()) {
            System.out.println("pacman got eaten...");
            initGhostsInfo();
        }

        if (game.wasPowerPillEaten()) {
            int currentIndexPowerPillEaten = powerPillsAvailable.indexOf(current);
            if (currentIndexPowerPillEaten != -1) {
                powerPillsAvailable.remove(currentIndexPowerPillEaten);
            } else {
                System.out.println("current doesnt match inside powerPillsAvailable...  map must have changed? or pwoer-pill has been already eaten?");
            }
        }

        if (game.wasPillEaten()) {
            int currentIndexPillEaten = pillsAvailable.indexOf(current);
            if (currentIndexPillEaten != -1) {
                pillsAvailable.remove(currentIndexPillEaten);
            } else {
                System.out.println("current doesnt match inside pillsAvailable...  map must have changed? or pill has been already eaten?");
            }
        }

        String myLog = "Ghosts are at: ";
        for(Constants.GHOST ghost : Constants.GHOST.values()) {
            int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
            String location = String.valueOf(ghostLocation);
            location = location.equals("-1") ? "unknown" : location;
            myLog += ghost.name() + " -> " + location + ", ";



            if (ghostLocation != -1) {

                //we see this ghost
                // lets update where we saw it
                mGhostsInfo.get(ghost).isVisible = true;
                mGhostsInfo.get(ghost).lastMove = game.getGhostLastMoveMade(ghost);

                mGhostsInfo.get(ghost).lastSeenTimestamp = currentTimestamp;

                mGhostsInfo.get(ghost).location = ghostLocation;
                mGhostsInfo.get(ghost).lastLocation = ghostLocation;

                mGhostsInfo.get(ghost).isEdible = game.getGhostEdibleTime(ghost) != 0;


            } else {
                mGhostsInfo.get(ghost).isVisible = false;


                int timeSinceSeen = game.getCurrentLevelTime() - mGhostsInfo.get(ghost).lastSeenTimestamp;

                // forget last position after some period of time
                if (timeSinceSeen > RESET_SEEN_AFTER && mGhostsInfo.get(ghost).lastLocation != -1) {

                    System.out.println("NEED TO RESET ITS BEEN A LONG TIME" + mGhostsInfo.get(ghost).lastSeenTimestamp + " : " + ghost.name());
                    mGhostsInfo.get(ghost).location = -1;
                }
            }
        }

        //myLog += game.getCurrentLevelTime() + " current: " + String.valueOf(current) + " last: " + String.valueOf(lastPosition);
        //System.out.println(myLog);


        // Strategy 1: Avoid Ghosts Around Me
        ArrayList<Constants.GHOST> ghostsAround = new ArrayList<>();
        ArrayList<Integer> distancesOfGhosts = new ArrayList<>();

        for (Constants.GHOST ghost : Constants.GHOST.values()) {

            //System.out.println(game.getGhostEdibleTime(ghost) + " - - - " + game.getGhostLairTime(ghost) );

            int ghostLocation = mGhostsInfo.get(ghost).location;
            boolean isEdible = mGhostsInfo.get(ghost).isEdible;

            boolean isVisible = mGhostsInfo.get(ghost).isVisible;
            int timeSinceSeen = game.getCurrentLevelTime() - mGhostsInfo.get(ghost).lastSeenTimestamp;

            int lastGhostLocation = mGhostsInfo.get(ghost).lastLocation;

            // If can't see these will be -1 so all fine there
            if (ghostLocation != -1 && !isEdible) {

                int distanceFromGhost = game.getShortestPathDistance(current, ghostLocation);

                if (distanceFromGhost < MIN_DISTANCE) {
                    ghostsAround.add(ghost);
                    distancesOfGhosts.add(distanceFromGhost);
                }
            } else if (lastGhostLocation != -1 && timeSinceSeen < 14 && game.getGhostCurrentNodeIndex(ghost) == -1 && !isVisible && !isEdible) {
                System.out.println("MAYBE ITS STILL SOMEWHERE AROUND THE CORNER GET AWAY..." );


                int distanceFromGhost = game.getShortestPathDistance(current, lastGhostLocation);

                ghostsAround.add(ghost);
                distancesOfGhosts.add(distanceFromGhost);
            }
        }

        if (!ghostsAround.isEmpty()) {

            if (ghostsAround.size() == 1) {

                int closestIndexGhost = findClosestGhost(distancesOfGhosts);

                Constants.GHOST closestGhost = ghostsAround.get(closestIndexGhost);

                int ghostLocation = mGhostsInfo.get(closestGhost).location;
                if (ghostLocation == -1) {
                    ghostLocation = mGhostsInfo.get(closestGhost).lastLocation;
                }

                System.out.println();
                MOVE myNextMove = game.getNextMoveAwayFromTarget(current, ghostLocation, Constants.DM.PATH);
                System.out.println("Evading Ghost: " + closestGhost.name() + " because my location is:" + current + " the ghost is: " + ghostLocation + " and i am moving: " + myNextMove);
                return myNextMove;

            } else {
                System.out.println("More than 1 ghost around to evade");


                ArrayList<Constants.MOVE> movesAvoidingGhosts = new ArrayList<>();
                for (Constants.GHOST ghost : ghostsAround) {

                    int ghostLocation = mGhostsInfo.get(ghost).location;
                    if (ghostLocation == -1) {
                        ghostLocation = mGhostsInfo.get(ghost).lastLocation;
                    }
                    MOVE myNextMove = game.getNextMoveAwayFromTarget(current, ghostLocation, Constants.DM.PATH);

                    movesAvoidingGhosts.add(myNextMove);
                }

                //Check if FLANKED

                Constants.MOVE[] possibleMoves = game.getPossibleMoves(current);

                for (Constants.MOVE possibleMove : possibleMoves) {
                    if (!movesAvoidingGhosts.contains(possibleMove)) {
                        //flanked
                        System.out.println("FLANKED -> MOVE " + possibleMove);
                        return movesAvoidingGhosts.get(0);
                    }
                }

                //Not flanked

                String myPossibleMoves = "";
                for (Constants.MOVE move : possibleMoves) {
                    myPossibleMoves += move + " ";
                }
                Constants.MOVE finalMove = movesAvoidingGhosts.get(0);
                System.out.println("POSSIBLE MOVES: " + myPossibleMoves + "decided to move at: " + finalMove);


                return finalMove;
            }
        }

        // Strategy 3: Go after the pills and power pills
        if (!pillsAvailable.isEmpty()) {

            int[] targetsArray = convertToArray(pillsAvailable);

            Constants.MOVE moveTowardsPill = game.getNextMoveTowardsTarget(current, game.getClosestNodeIndexFromNodeIndex(current, targetsArray, Constants.DM.PATH), Constants.DM.PATH);
            System.out.println("Eating...: " + moveTowardsPill + " position: " + current);

            return moveTowardsPill;
        }

        return myMove;
    }

    int[] convertToArray(ArrayList<Integer> fromArralyst) {

        int[] targetsArray = new int[fromArralyst.size()];        //convert from ArrayList to array

        for (int i = 0; i < targetsArray.length; i++) {
            targetsArray[i] = fromArralyst.get(i);
        }
        return targetsArray;
    }


    int findClosestGhost(ArrayList<Integer> arrayDistances) {

        int minDistanceIndex = 0;
        for (int i = 0; i < arrayDistances.size(); i++) {

            if (arrayDistances.get(i) < arrayDistances.get(minDistanceIndex)) {

                minDistanceIndex = i;
            }
        }
        return minDistanceIndex;
    }
}