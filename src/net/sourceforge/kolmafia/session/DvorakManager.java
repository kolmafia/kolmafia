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
  private static String lastResponse = "";

  // For testing
  public static void reset() {
    currentRow = 0;
    lastResponse = "";
  }

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
    Matcher matcher = TILE_PATTERN.matcher(responseText);
    int count = 0;
    while (matcher.find()) {
      int row = count / 9;
      int column = count % 9;
      if (row > 7) {
        KoLmafia.updateDisplay("Too many rows!");
        return -1;
      }
      if (matcher.group(1).equals("cell")) {
        currentRow = row;
      }
      char tile = matcher.group(2).charAt(0);
      tiles[row][column] = tile;
      count++;
    }

    if (count != (7 * 9)) {
      KoLmafia.updateDisplay("Wrong number of cells!");
      return -1;
    }

    // If we have made it to a row, we must have successfully
    // jumped that far.

    currentSolution = solution.substring(0, 6 - currentRow);

    return currentRow;
  }

  public static void printTiles() {
    StringBuilder buffer = new StringBuilder();
    for (int row = 0; row < 7; ++row) {
      buffer.setLength(0);
      buffer.append("Row ");
      buffer.append((row + 1));
      buffer.append(": ");
      for (int col = 0; col < 9; ++col) {
        buffer.append(tiles[row][col]);
        buffer.append(" ");
      }
      if (row == currentRow) {
        buffer.append(" ---you are here");
      }
      RequestLogger.printLine(buffer.toString());
    }

    RequestLogger.printLine();
    RequestLogger.printLine("Current solution = \"" + currentSolution + "\"");
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
      currentRow = -1;
    } else {
      parseTiles(responseText);
    }
  }

  public static final void lastTile(final String responseText) {
    // Called when we arrive at choice 125
    //
    // You jump to the last letter, and put your pom-poms down with
    // a sigh of relief -- thank goodness that's
    // over. Worst. Spelling bee. Ever.

    if (responseText.contains("You jump to the last letter")) {

      String message = "What's that spell? " + currentSolution + "!";
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

    Matcher matcher = WHICHTILE_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      RequestLogger.registerLocation("The Hidden Temple");
      return true;
    }

    int col = StringUtilities.parseInt(matcher.group(1));

    // We saved the array and currentRow when we last visited the puzzle.
    if (currentRow < 0 || currentRow > 6 || col < 0 || col > 8) {
      // Shouldn't happen, but log the URL, at least
      return false;
    }

    int row = currentRow;
    char letter = tiles[row][col];

    currentSolution += letter;

    StringBuilder buffer = new StringBuilder();
    buffer.append("Give me ");
    buffer.append(AN_LETTERS.indexOf(letter) != -1 ? "an" : "a");
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

    StringBuffer span = new StringBuffer();
    span.append("<center><table cols=2><tr>");

    StringBuffer stepButton = new StringBuffer();
    String url = "/KoLmafia/waitSpecialCommand?cmd=dvorak+step&pwd=" + GenericRequest.passwordHash;
    stepButton.append("<td>");
    stepButton.append("<form name=stepform action='").append(url).append("' method=post>");
    stepButton
        .append("<input type=hidden name=pwd value='")
        .append(GenericRequest.passwordHash)
        .append("'>");
    stepButton.append("<input class=button type=submit value=\"Step\">").append("</form>");
    stepButton.append("</td>");
    span.append(stepButton);

    StringBuffer solveButton = new StringBuffer();
    url = "/KoLmafia/specialCommand?cmd=dvorak&pwd=" + GenericRequest.passwordHash;
    solveButton.append("<td>");
    solveButton.append("<form name=solveform action='").append(url).append("' method=post>");
    solveButton
        .append("<input type=hidden name=pwd value='")
        .append(GenericRequest.passwordHash)
        .append("'>");
    solveButton.append("<input class=button type=submit value=\"Solve!\">").append("</form>");
    solveButton.append("</td>");
    span.append(solveButton);

    span.append("</tr></table></center>");

    // Insert it into the page
    buffer.insert(index, span);
  }

  public static final void saveResponse(final String responseText) {
    lastResponse = responseText;
  }

  public static final void step() {
    String URL = nextStep();
    String responseText = "Oops";
    if (URL != null) {
      GenericRequest request = new GenericRequest(URL);
      request.run();
      StringBuffer buffer = new StringBuffer(request.responseText);
      RequestEditorKit.getFeatureRichHTML(request.getURLString(), buffer);
      responseText = buffer.toString();
    }
    RelayRequest.specialCommandResponse = responseText;
    RelayRequest.specialCommandIsAdventure = true;
  }

  private static final String nextStep() {
    if (lastResponse == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't appear to be at the tiles puzzle");
      return null;
    }

    // When we visited this url, we parsed the responseText and
    // saved the tiles and currentRow

    if (currentRow < 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "We can't tell what row you are on");
      return null;
    }

    int row = currentRow;
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
      return null;
    }

    return "tiles.php?action=jump&whichtile=" + found;
  }

  public static final void solve() {
    GenericRequest request = new GenericRequest("");
    for (int row = currentRow; row >= 0; --row) {
      String URL = nextStep();
      if (URL == null) {
        // Not expected
        RelayRequest.specialCommandResponse = "Oops!";
        RelayRequest.specialCommandIsAdventure = true;
        lastResponse = null;
        return;
      }
      request.constructURLString(URL);
      request.run();
    }

    String responseText = request.responseText;
    // This should always be true, but check anyway.
    if (responseText.contains("No Visible Means of Support")) {
      ChoiceManager.preChoice(request);
      ChoiceManager.visitChoice(request);
    }

    StringBuffer buffer = new StringBuffer(responseText);
    RequestEditorKit.getFeatureRichHTML(request.getURLString(), buffer);
    RelayRequest.specialCommandResponse = buffer.toString();
    RelayRequest.specialCommandIsAdventure = true;
    lastResponse = null;
  }
}
