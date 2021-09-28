package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class DvorakManager {
  private static final char[][] tiles = new char[7][9];
  private static int currentRow = 0;

  private static final String solution = "BANANAS";
  private static String currentSolution = "";

  // <td class='cell greyed'><img
  // src="http://images.kingdomofloathing.com/itemimages/tilek.gif"
  // width=30 height=30 border=0 alt='Tile labeled "K"'></td>
  //
  // <td class='cell'><a class=nounder
  // href='tiles.php?action=jump&whichtile=8'><img
  // src="http://images.kingdomofloathing.com/itemimages/tilep.gif"
  // width=30 height=30 border=0 alt='Tile labeled "P"'></a></td>

  private static final Pattern TILE_PATTERN =
      Pattern.compile("<td class='(cell|cell greyed)'.*?'Tile labeled \"(.)\"'>(</a>)?</td>");

  private static int parseTiles(final String responseText) {
    Matcher matcher = DvorakManager.TILE_PATTERN.matcher(responseText);
    int count = 0;
    while (matcher.find()) {
      int row = count / 9;
      int column = count % 9;
      if (row > 7) {
        KoLmafia.updateDisplay("Too many rows!");
        return -1;
      }
      if (matcher.group(1).equals("cell")) {
        DvorakManager.currentRow = row;
      }
      char tile = matcher.group(2).charAt(0);
      DvorakManager.tiles[row][column] = tile;
      count++;
    }

    if (count != (7 * 9)) {
      KoLmafia.updateDisplay("Wrong number of cells!");
      return -1;
    }

    // If we have made it to a row, we must have successfully
    // jumped that far.

    DvorakManager.currentSolution =
        DvorakManager.solution.substring(0, 6 - DvorakManager.currentRow);

    return DvorakManager.currentRow;
  }

  public static void printTiles() {
    StringBuilder buffer = new StringBuilder();
    for (int row = 0; row < 7; ++row) {
      buffer.setLength(0);
      buffer.append("Row ");
      buffer.append((row + 1));
      buffer.append(": ");
      for (int col = 0; col < 9; ++col) {
        buffer.append(DvorakManager.tiles[row][col]);
        buffer.append(" ");
      }
      if (row == DvorakManager.currentRow) {
        buffer.append(" ---you are here");
      }
      RequestLogger.printLine(buffer.toString());
    }

    RequestLogger.printLine();
    RequestLogger.printLine("Current solution = \"" + DvorakManager.currentSolution + "\"");
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("tiles.php")) {
      return;
    }

    // We can get the puzzle from whatever row we are on. Unless we screwed up.
    //
    // As you step to that tile, something tells you that you've
    // made an incorrect choice. That something is a large stone
    // pillar pistoning down from the ceiling and mashing you to a
    // pulp. Squish!

    if (responseText.contains("Squish!")) {
      String message = "Oops.";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      DvorakManager.currentRow = -1;
    } else {
      DvorakManager.parseTiles(responseText);
    }
  }

  public static final void lastTile(final String responseText) {
    // Called when we arrive at choice 125
    //
    // You jump to the last letter, and put your pom-poms down with
    // a sigh of relief -- thank goodness that's
    // over. Worst. Spelling bee. Ever.

    if (responseText.contains("You jump to the last letter")) {

      String message = "What's that spell? " + DvorakManager.currentSolution + "!";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
  }

  private static final Pattern WHICHTILE_PATTERN = Pattern.compile("whichtile=(\\d)");
  private static final String AN_LETTERS = "AEFILMNORSX";

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("tiles.php")) {
      return false;
    }

    Matcher matcher = DvorakManager.WHICHTILE_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      RequestLogger.registerLocation("The Hidden Temple");
      return true;
    }

    int col = StringUtilities.parseInt(matcher.group(1));

    // We saved the array and currentRow when we last visited the puzzle.
    if (DvorakManager.currentRow < 0 || DvorakManager.currentRow > 6 || col < 0 || col > 8) {
      // Shouldn't happen, but log the URL, at least
      return false;
    }

    int row = DvorakManager.currentRow;
    char letter = DvorakManager.tiles[row][col];

    DvorakManager.currentSolution += letter;

    StringBuilder buffer = new StringBuilder();
    buffer.append("Give me ");
    buffer.append(DvorakManager.AN_LETTERS.indexOf(letter) != -1 ? "an" : "a");
    buffer.append(" ");
    buffer.append(letter);
    buffer.append("!");

    String message = buffer.toString();

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }

  public static final void decorate(final StringBuffer buffer) {
    String search = "</div></center></td></tr>";
    int index = buffer.indexOf(search);
    if (index == -1) {
      return;
    }
    index += 6;

    // Build a "Solve!" button
    StringBuffer button = new StringBuffer();

    button.append("<form name=solveform action='");
    button.append("/KoLmafia/specialCommand?cmd=dvorak&pwd=");
    button.append(GenericRequest.passwordHash);
    button.append("' method=post>");
    button.append("<input class=button type=submit value=\"Solve!\">");
    button.append("</form>");

    // Insert it into the page
    buffer.insert(index, button);
  }

  private static String lastResponse = "";

  public static final void saveResponse(final String responseText) {
    DvorakManager.lastResponse = responseText;
  }

  public static final void solve() {
    if (DvorakManager.lastResponse == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't appear to be at the tiles puzzle");
      return;
    }

    // When we visited this url, we parsed the responseText and
    // saved the tiles and currentRow

    if (DvorakManager.currentRow < 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "We can't tell what row you are on");
      return;
    }

    // Execute requests to hop from tile to tile to the end.

    GenericRequest request = new GenericRequest("");
    for (int row = DvorakManager.currentRow; row >= 0; --row) {
      char match = solution.charAt(6 - row);
      int found = -1;
      for (int col = 0; col < 9; ++col) {
        char tile = tiles[row][col];
        if (match == tile) {
          found = col;
          break;
        }
      }
      if (found == -1) {
        KoLmafia.updateDisplay("Could not find '" + match + "' in row " + (row + 1));
        return;
      }

      String url = "tiles.php?action=jump&whichtile=" + found;
      request.constructURLString(url);
      request.run();
    }

    StringBuffer buffer = new StringBuffer(request.responseText);
    RequestEditorKit.getFeatureRichHTML(request.getURLString(), buffer);
    RelayRequest.specialCommandResponse = buffer.toString();
    RelayRequest.specialCommandIsAdventure = true;
    DvorakManager.lastResponse = null;
  }
}
