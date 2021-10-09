package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class QuantumTerrariumRequest extends GenericRequest {
  public static final QuantumTerrariumRequest INSTANCE = new QuantumTerrariumRequest();

  private static final Pattern CURRENT_FAM_PATTERN =
      Pattern.compile("Your Current Familiar.*? onClick='fam\\((\\d+)\\)'");
  private static final Pattern NEXT_FAM_PATTERN =
      Pattern.compile("Your Familiar in <b>(\\d+)</b> Adventures.*? onClick='fam\\((\\d+)\\)'");
  private static final Pattern REALIGNMENT_COOLDOWN_PATTERN =
      Pattern.compile("You will be able to align the quanta again in <b>(\\d+) adventures.</b>");

  public static final String FAMILIAR_COUNTER = "Quantum Familiar";
  public static final String COOLDOWN_COUNTER = "Q.F.I.D.M.A.";

  private static int lastChecked = 0;

  private int force;

  public QuantumTerrariumRequest() {
    super("qterrarium.php");

    this.force = 0;
  }

  public QuantumTerrariumRequest(final int force) {
    super("qterrarium.php");

    this.force = force;

    if (this.force > 0) {
      this.addFormField("action", "fam");
      this.addFormField("pwd");
      this.addFormField("fid", String.valueOf(this.force));
    }
  }

  public static void checkCounter(GenericRequest request) {
    // No result, no check
    if (!request.hasResult()) {
      return;
    }

    if (lastChecked >= KoLCharacter.getCurrentRun()) {
      return;
    }

    if (!KoLCharacter.inQuantum()) {
      return;
    }

    if (KoLCharacter.inFightOrChoice()) {
      return;
    }

    if ((TurnCounter.getCounters(FAMILIAR_COUNTER, 0, 1).contains(FAMILIAR_COUNTER))
        || !TurnCounter.isCounting(FAMILIAR_COUNTER)) {
      lastChecked = KoLCharacter.getCurrentRun();
      RequestThread.postRequest(INSTANCE);
    }
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (!KoLCharacter.inQuantum()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Can only make quantum terrarium requests in Quantum Terrarium.");
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    if (!KoLCharacter.inQuantum()) {
      return;
    }

    if (!QuantumTerrariumRequest.parseResponse(this.getURLString(), this.responseText)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Quantum Terrarium request unsuccessful.");
    }
  }

  public static boolean parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("qterrarium.php")) {
      return false;
    }

    Matcher matcher = CURRENT_FAM_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1));
      if (id != KoLCharacter.getFamiliar().getId()) {
        ApiRequest.updateStatus(true);
      }
    }

    int nextFamId = -1;

    matcher = NEXT_FAM_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int turns = StringUtilities.parseInt(matcher.group(1));
      nextFamId = StringUtilities.parseInt(matcher.group(2));

      Preferences.setString("nextQuantumFamiliar", FamiliarDatabase.getFamiliarName(nextFamId));
      Preferences.setInteger("nextQuantumFamiliarTurn", KoLCharacter.getTurnsPlayed() + turns);
      TurnCounter.stopCounting(FAMILIAR_COUNTER);
      TurnCounter.startCounting(
          turns, FAMILIAR_COUNTER + " loc=*", FamiliarDatabase.getFamiliarImageLocation(nextFamId));
    }

    matcher = REALIGNMENT_COOLDOWN_PATTERN.matcher(responseText);
    int turns = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
    Preferences.setInteger("_nextQuantumAlignment", KoLCharacter.getTurnsPlayed() + turns);
    TurnCounter.stopCounting(COOLDOWN_COUNTER);
    TurnCounter.startCounting(turns, COOLDOWN_COUNTER + " loc=*", "quantum.gif");

    if (responseText.contains("arranging the quanta to force your desired future")) {
      String message =
          "Forced next quantum familiar to be " + FamiliarDatabase.getFamiliarName(nextFamId);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    KoLCharacter.updateStatus();

    return true;
  }

  public static boolean registerRequest(final String urlString) {
    return urlString.startsWith("qterrarium.php");
  }
}
