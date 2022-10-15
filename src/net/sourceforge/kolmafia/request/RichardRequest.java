package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RichardRequest extends GenericRequest {
  public static final int MYSTICALITY = 1;
  public static final int MOXIE = 2;
  public static final int MUSCLE = 3;

  private static final Pattern TURN_PATTERN = Pattern.compile("numturns=(\\d+)");

  private static int getTurns(String urlString) {
    Matcher turnMatcher = TURN_PATTERN.matcher(urlString);
    return turnMatcher.find() ? StringUtilities.parseInt(turnMatcher.group(1)) : 0;
  }

  private int turnCount = 1;

  /**
   * Constructs a new <code>RichardRequest</code>.
   *
   * @param equipmentId The identifier for the equipment you're using
   */
  public RichardRequest(final int equipmentId) {
    super("clan_hobopolis.php");
    this.addFormField("place", "3");
    this.addFormField("preaction", "spendturns");
    this.addFormField("whichservice", String.valueOf(equipmentId));
  }

  public RichardRequest setTurnCount(final int turnCount) {
    this.turnCount = turnCount;
    this.addFormField("numturns", String.valueOf(turnCount));
    return this;
  }

  @Override
  public void run() {
    if (KoLCharacter.getAdventuresLeft() < this.turnCount) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Insufficient adventures.");
      return;
    }

    KoLmafia.updateDisplay("Helping Richard...");

    super.run();
  }

  @Override
  public void processResults() {
    KoLmafia.updateDisplay("Workout completed.");
  }

  @Override
  public int getAdventuresUsed() {
    return this.turnCount;
  }

  public static int getAdventuresUsed(String urlString) {
    return getTurns(urlString);
  }

  public static boolean registerRequest(final String urlString) {
    String gymType = null;

    if (!urlString.startsWith("clan_hobopolis.php")
        || !urlString.contains("place=3")
        || !urlString.contains("preaction=spendturns")) {
      return false;
    }

    if (urlString.contains("whichservice=1")) {
      gymType = "Help Richard make bandages (Mysticality)";
    }
    if (urlString.contains("whichservice=2")) {
      gymType = "Help Richard make grenades (Moxie)";
    }
    if (urlString.contains("whichservice=3")) {
      gymType = "Help Richard make shakes (Muscle)";
    }

    if (gymType == null) {
      return false;
    }

    int turns = getTurns(urlString);

    RequestLogger.printLine(
        "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turns + " turns)");
    RequestLogger.updateSessionLog(
        "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turns + " turns)");
    return true;
  }
}
