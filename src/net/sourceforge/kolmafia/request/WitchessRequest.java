package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class WitchessRequest extends GenericRequest {
  private static final Pattern SOLVED_PATTERN = Pattern.compile("Solved Today");

  private static final Pattern PUZZLE_NUMBER_PATTERN = Pattern.compile("Witchess Puzzle #(\\d+)");

  private final String num;
  private int thisPuzzle;
  private boolean isSolved;
  private boolean errorOnNoRun;

  /**
   * Constructs a new <code>WitchessRequest</code> which acquires the Puzzle Champ buff if possible
   */
  public WitchessRequest() {
    super("choice.php");
    this.addFormField("whichchoice", "1183");
    this.addFormField("option", "2");

    this.num = null;
    this.thisPuzzle = -1;
    this.isSolved = false;
    this.errorOnNoRun = false;
  }

  /**
   * Constructs a new <code>WitchessRequest</code> which retrieves the daily puzzle as specified by
   * the given num
   *
   * @param num Which daily Witchess puzzle to retrieve
   */
  public WitchessRequest(String num) {
    super("witchess.php");
    this.addFormField("num", num);

    this.num = num;
    this.thisPuzzle = -1;
    this.isSolved = false;
    this.errorOnNoRun = false;
  }

  public WitchessRequest(String num, boolean errorOnNoRun) {
    super("witchess.php");
    this.addFormField("num", num);

    this.num = num;
    this.thisPuzzle = -1;
    this.isSolved = false;
    this.errorOnNoRun = errorOnNoRun;
  }

  public String getNum() {
    return this.num;
  }

  public int getThisPuzzle() {
    return this.thisPuzzle;
  }

  public boolean getIsSolved() {
    return this.isSolved;
  }

  @Override
  public void run() {
    var state = errorOnNoRun ? KoLConstants.MafiaState.ERROR : KoLConstants.MafiaState.CONTINUE;
    if (KoLCharacter.inLegacyOfLoathing()) {
      if (!Preferences.getBoolean("replicaWitchessSetAvailable")) {
        KoLmafia.updateDisplay(state, "You need to use a replica Witchess Set first.");
        return;
      }
    } else {
      if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Witchess Set")) {
        KoLmafia.updateDisplay(state, "Witchess is too old to use in your current path.");
        return;
      }
    }

    if (this.num == null) {
      runGetBuff();
    } else {
      runGetPuzzle();
    }
  }

  private void runGetBuff() {
    if (Preferences.getBoolean("_witchessBuff")) {
      KoLmafia.updateDisplay("You already got your Witchess buff today.");
      return;
    }
    if (Preferences.getInteger("puzzleChampBonus") != 20) {
      KoLmafia.updateDisplay(
          "You cannot automatically get a Witchess buff until all puzzles are solved.");
      return;
    }
    RequestThread.postRequest(new GenericRequest("campground.php?action=witchess"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1181&option=3"));
    super.run();
  }

  private void runGetPuzzle() {
    var state = errorOnNoRun ? KoLConstants.MafiaState.ERROR : KoLConstants.MafiaState.CONTINUE;

    if (Preferences.getInteger("puzzleChampBonus") == 20) {
      KoLmafia.updateDisplay(state, "You have already solved all of the Witchess puzzles.");
      return;
    }
    if (Preferences.getBoolean("_witchessBuff")) {
      KoLmafia.updateDisplay(state, "You already solved today's Witchess puzzles.");
      return;
    }
    super.run();
  }

  @Override
  public void processResults() {
    if (this.num == null) {
      WitchessRequest.parseResponse(this.getURLString(), this.responseText);
    } else {
      parsePuzzleResponse();
    }
  }

  public static final void parseResponse(String urlString, String responseText) {
    if (!urlString.startsWith("witchess.php")) {
      return;
    }

    if (responseText.contains("Puzzle Champ")) {
      Preferences.setBoolean("_witchessBuff", true);
    }
  }

  private void parsePuzzleResponse() {
    Matcher puzzleNumberMatcher = PUZZLE_NUMBER_PATTERN.matcher(this.responseText);
    if (puzzleNumberMatcher.find()) {
      this.thisPuzzle = StringUtilities.parseInt(puzzleNumberMatcher.group(1));
    }

    Matcher isSolvedMatcher = SOLVED_PATTERN.matcher(this.responseText);
    this.isSolved = isSolvedMatcher.find();
  }
}
