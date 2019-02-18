package entrants.pacman.alexookah;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.internal.Maze;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.alexookah).
 */



@SuppressWarnings("Duplicates")
public class MyPacMan_0 extends PacmanController {
    private MOVE myMove = MOVE.NEUTRAL;
    private static final int MIN_DISTANCE = 20;
    private static final int RESET_SEEN_AFTER = 200;
    private ArrayList<Integer> pillsEaten = new ArrayList<>();
    private HashMap<Constants.GHOST, GhostInfo> mGhostsInfo = new HashMap<>();
    private Maze mMaze;

    private class GhostInfo {
        public int lastSeenTimestamp;
        public int location;
        public int lastSeenLocation;
        public boolean isEdible;

        public GhostInfo(int lastSeenTimestamp, int location) {
            this.lastSeenTimestamp = lastSeenTimestamp;
            this.location = location;
            this.lastSeenLocation = location;
            this.isEdible = false;
        };
    }

    private void initGhostsInfo() {
        for(Constants.GHOST ghost : Constants.GHOST.values()) {
            mGhostsInfo.put(ghost, new GhostInfo(0, -1));
        }
    }

    public MOVE getMove(Game game, long timeDue) {
        //Place your game logic here to play the game as Ms Pac-Man
        int current = game.getPacmanCurrentNodeIndex();
        int currentTimestamp = game.getCurrentLevelTime();

        Maze thisMaze = game.getCurrentMaze();
        if (mMaze == null || !mMaze.name.equals(thisMaze.name)) {
            mMaze = thisMaze;
            initGhostsInfo();
            pillsEaten = new ArrayList<>();
        }


        if (game.wasPacManEaten()) {
            System.out.println("Game Over");
            initGhostsInfo();
        }



        if (game.wasPillEaten()) {
            pillsEaten.add(current);
        }


        String myLog = "Ghosts are at: ";
        for(Constants.GHOST ghost : Constants.GHOST.values()) {
            int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
            String location = String.valueOf(ghostLocation);
            location = location.equals("-1") ? "unknown" : location;
            myLog += ghost.name() + " -> " + location + ", ";
            if (ghostLocation != -1) {
                GhostInfo newInfo = new GhostInfo(currentTimestamp, ghostLocation);
                newInfo.isEdible = game.getGhostEdibleTime(ghost) != 0;
                mGhostsInfo.put(ghost, newInfo);
            }


            int timeSinceSeen = game.getCurrentLevelTime() - mGhostsInfo.get(ghost).lastSeenTimestamp;

            // forget last position after some period of time
            if (timeSinceSeen > RESET_SEEN_AFTER && ghostLocation != -1) {
                mGhostsInfo.put(ghost, new GhostInfo(0, -1));
                System.out.println("NEED TO RESET ITS BEEN A LONG ITME" + mGhostsInfo.get(ghost).lastSeenTimestamp + " : " + ghost.name());
            }
        }

        //myLog += game.getCurrentLevelTime() + " current: " + String.valueOf(current) + " last: " + String.valueOf(lastPosition);
        System.out.println(myLog);



        // Strategy 1: Adjusted for PO
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            // If can't see these will be -1 so all fine there
            System.out.println(game.getGhostEdibleTime(ghost) + " - - - " + game.getGhostLairTime(ghost) );

            int ghostLocation = mGhostsInfo.get(ghost).location;
            boolean isEdible = mGhostsInfo.get(ghost).isEdible;
            int timeSinceSeen = game.getCurrentLevelTime() - mGhostsInfo.get(ghost).lastSeenTimestamp;


            if (ghostLocation != -1 && !isEdible && timeSinceSeen < MIN_DISTANCE) {
                if (game.getShortestPathDistance(current, ghostLocation) < MIN_DISTANCE) {
                    System.out.println();
                    MOVE myNextMove = game.getNextMoveAwayFromTarget(current, ghostLocation, Constants.DM.PATH);
                    System.out.println("Evading Ghost because my location is:" + current + " the ghost is: " + ghostLocation + " and i am moving: " + myNextMove);

                    return myNextMove;
                }
            }

        }


        // Strategy 3: Go after the pills and power pills that we can see
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        ArrayList<Integer> targets = new ArrayList<Integer>();

        for (int i = 0; i < pills.length; i++) {
            // check which pills are available
            Boolean pillStillAvailable = !pillsEaten.contains(pills[i]); //game.isPillStillAvailable(i);

            if (pillStillAvailable != null) {
                if (pillStillAvailable) {
                    targets.add(pills[i]);
                }
            }
        }

        for (int i = 0; i < powerPills.length; i++) {            //check with power pills are available
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null) {
                if (pillStillAvailable) {
                    //targets.add(powerPills[i]);
                }
            }
        }

        if (!targets.isEmpty()) {
            int[] targetsArray = new int[targets.size()];        //convert from ArrayList to array

            for (int i = 0; i < targetsArray.length; i++) {
                targetsArray[i] = targets.get(i);
            }
            //return the next direction once the closest target has been identified
//            System.out.println("Hunting pill");
            //System.out.println("Eating...");
            MOVE nextMove = game.getNextMoveTowardsTarget(current, game.getClosestNodeIndexFromNodeIndex(current, targetsArray, Constants.DM.PATH), Constants.DM.PATH);
            System.out.println("Eating... --> " + nextMove);
            return nextMove;
        }


        return myMove;
    }



}