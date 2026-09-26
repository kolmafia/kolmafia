package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class WitchessSolutionDatabase {
  private static final Map<Integer, WitchessSolution> witchessSolutions = new HashMap<>();

  private static final Map<Character, int[]> moveDict = new HashMap<>();

  private WitchessSolutionDatabase() {}

  static {
    moveDict.put('r', new int[] {0, 2});
    moveDict.put('l', new int[] {0, -2});
    moveDict.put('u', new int[] {-2, 0});
    moveDict.put('d', new int[] {2, 0});
    WitchessSolutionDatabase.reset();
  }

  public static void reset() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader(
            "witchess_solutions.txt", KoLConstants.WITCHESS_SOLUTIONS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 2) {
          continue;
        }

        int puzzleId = StringUtilities.parseInt(data[0]);
        var solution = parseSolution(puzzleId, data);

        WitchessSolutionDatabase.witchessSolutions.put(puzzleId, solution);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static class WitchessSolution {
    protected final int puzzleId;
    protected final Character[] moves;
    protected final String coords;

    public WitchessSolution(int puzzleId, Character[] moves, String coords) {
      this.puzzleId = puzzleId;
      this.moves = moves;
      this.coords = coords;
    }

    public int getId() {
      return this.puzzleId;
    }

    public String getCoords() {
      return this.coords;
    }
  }

  private static WitchessSolution parseSolution(Integer puzzleId, String[] data) {
    Character[] solution =
        Arrays.stream(data[1].trim().split(" ")).map(s -> s.charAt(0)).toArray(Character[]::new);

    var coords = solvePath(solution);
    return new WitchessSolution(puzzleId, solution, coords);
  }

  private static int[] calculateMidpoint(int[] start, int[] end) {
    int midX = (start[0] + end[0]) / 2;
    int midY = (start[1] + end[1]) / 2;
    return new int[] {midX, midY};
  }

  private static String solvePath(Character[] moves) {
    /*
    We could hard-code the size of each puzzle and therefore the starting coordinate,
    but to reduce the need for transcribed data, we can instead determine the size of each grid
    based on the moves made. Since you move from one extreme of the grid to the opposite,
    we can assume that the max grid size will always be the absolute value of the difference between
    each move in the cardinal directions (up/down and left/right)

    However, each move increments the grid position by 2, so we need to double the difference
    to get the real size and coordinates.

    In a Witchess puzzle, the grid is made up of 4 types of table cells:
      1. corner, the start and end points of each move the player makes during a game.
          Identified in the html as a <div> with class="corner"
          <div rel="0,0" class="corner "></div>

      2. hline, the horizontal grid lines that players traverse during gameplay.
          Identified as a <div> with class="hline"
          <div rel="0,1" class="hline"></div>

      3. vline, the vertical grid lines that players traverse during gameplay.
          Identified as a <div> with class="vline"
          <div rel="1,0" class="vline"></div>

      4. square, the white spaces between the grid lines in the game.
          These optionally contain witchess pieces, which define the acceptable movement in the game.
          <div rel="1,1" class="square"></div>

    A witchess puzzle is solved when the user submits a GET request to witchess.php with "sol", "ajax", and "number" parameters.
    During normal play of the game, KoL generates a list of the player's moves,
    tracking all "vline" and "hline" coordinates the player has traversed.
    The "sol" parameter is an ordered list of those coordinates, separated by a pipe | character.
    This function determines the list of coordinates that would be traversed in a successful game,
    orders it, and converts it to a |-separated list string to append to a GET request to solve the puzzle.
    The "ajax" parameter has always been observed to be "1" for these puzzles, and "number" is always the puzzle's ID.

    Assuming a starting position of (6,0)
    If the player moves right once, their new position is (6,2). We'd record the difference between those two positions, so (6,1)
    Right again would take the player to (6,4), and we'd log the next midpoint, (6,3)
    Up once would take the player to (4,4) and we'd log (5,4) as the next midpoint
    Our list of moves at this point looks like (6,1),(6,3),(5,4);
    If we were done here, we'd then sort this result to (5,4),(6,1),(6,3)
    And finally convert to a string separated by the | character: 5,4|6,1|6,3
    The result is URL Encoded when submitted, but the GenericRequest class handles that for us.
    */
    Map<Character, Integer> moveCounts = new HashMap<>();
    for (char move : moves) {
      moveCounts.merge(move, 1, Integer::sum);
    }

    /*
     * netVerticalDisplacement is used to find the maximum X value of the puzzle by determining the total vertical space moved
     * netHorizontalDisplacement is used to find the maximum Y value the same way.
     */
    int netVerticalDisplacement =
        Math.abs(moveCounts.getOrDefault('u', 0) - moveCounts.getOrDefault('d', 0));
    int netHorizontalDisplacement =
        Math.abs(moveCounts.getOrDefault('l', 0) - moveCounts.getOrDefault('r', 0));

    /*
     * Since corners in the game also have coordinates,
     * each move actually increases or decreases the player's x/y position by 2,
     * meaning the total size of the grid is twice the maximum number of moves in each direction.
     */
    int maxX = netVerticalDisplacement * 2;
    int maxY = netHorizontalDisplacement * 2;

    int x = maxX, y = 0;
    Set<String> path = new HashSet<>();

    /*
     * To determine the list of all "vline" and "hline" coordinates traversed,
     * we loop through the list of moves in a successful game, determine our new position,
     * and record the coordinate between our current and last position.
     */
    for (char move : moves) {
      int[] start = new int[] {x, y};
      int[] delta = moveDict.get(move);
      x = Math.max(0, Math.min(x + delta[0], maxX));
      y = Math.max(0, Math.min(y + delta[1], maxY));
      int[] end = new int[] {x, y};
      int[] midpoint = calculateMidpoint(start, end);
      path.add(midpoint[0] + "," + midpoint[1]);
    }

    /*
     * Every "sol" sent during manual gameplay was sorted first by the x coordinate, then y,
     * and concatenated into a |-separated string, so we should do the same.
     */
    List<String> sortedPath = new ArrayList<>(path);
    sortedPath.sort(
        Comparator.comparingInt((String s) -> Integer.parseInt(s.split(",")[0]))
            .thenComparing(s -> Integer.parseInt(s.split(",")[1])));

    return String.join("|", sortedPath);
  }

  public static WitchessSolution getWitchessSolution(final Integer puzzleId) {
    return WitchessSolutionDatabase.witchessSolutions.get(puzzleId);
  }
}
