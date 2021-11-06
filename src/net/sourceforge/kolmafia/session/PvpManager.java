package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpManager {
  // The current mapping of stances
  public static final TreeMap<Integer, String> optionToStance = new TreeMap<Integer, String>();
  public static final TreeMap<String, Integer> stanceToOption = new TreeMap<String, Integer>();
  public static boolean stancesKnown = false;

  // Support for fuzzy mapping of stance names
  public static String[] canonicalStances = null;
  public static final TreeMap<String, Integer> canonicalStanceToOption =
      new TreeMap<String, Integer>();

  // <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1"
  // selected>Beary Famous</option><option value="2" >Barely Dressed</option><option value="3"
  // >Basket Reaver</option><option value="4" >Polar Envy</option><option value="5" >Maul
  // Power</option><option value="6" >Grave Robbery</option><option value="7" >Most Things
  // Eaten</option><option value="8" >Hibernation Ready</option><option value="9" >Visiting the
  // Cousins</option><option value="10" >Northern Digestion</option><option value="11" >Most
  // Murderous</option></select>

  private static final Pattern STANCE_DROPDOWN_PATTERN =
      Pattern.compile("<select name=\"stance\">.*?</select>", Pattern.DOTALL);
  private static final Pattern STANCE_OPTION_PATTERN =
      Pattern.compile("<option value=\"([\\d]*)\" (?:selected)?>(.*?)</option>");

  public static boolean noFight = false;

  public static final boolean checkStances() {
    if (!PvpManager.stancesKnown) {
      PeeVPeeRequest request = new PeeVPeeRequest("fight");
      RequestThread.postRequest(request);
    }
    return PvpManager.stancesKnown;
  }

  public static final void parseStances(final String responseText) {
    Matcher stanceMatcher = PvpManager.STANCE_DROPDOWN_PATTERN.matcher(responseText);
    if (!stanceMatcher.find()) {
      return;
    }

    String stances = stanceMatcher.group(0);

    PvpManager.optionToStance.clear();
    PvpManager.stanceToOption.clear();

    ArrayList<String> canonical = new ArrayList<String>();

    Matcher optionsMatcher = PvpManager.STANCE_OPTION_PATTERN.matcher(stances);
    while (optionsMatcher.find()) {
      int option = StringUtilities.parseInt(optionsMatcher.group(1));
      String stance = optionsMatcher.group(2);
      PvpManager.optionToStance.put(option, stance);
      PvpManager.stanceToOption.put(stance, option);
      String canonicalStance = StringUtilities.getCanonicalName(stance);
      PvpManager.canonicalStanceToOption.put(canonicalStance, option);
      canonical.add(canonicalStance);
    }

    Collections.sort(canonical);
    PvpManager.canonicalStances = new String[canonical.size()];
    PvpManager.canonicalStances = canonical.toArray(PvpManager.canonicalStances);

    PvpManager.stancesKnown = true;
  }

  public static final int findStance(final String stanceName) {
    List<String> matchingNames =
        StringUtilities.getMatchingNames(PvpManager.canonicalStances, stanceName);
    if (matchingNames.size() != 1) {
      return -1;
    }

    String name = matchingNames.get(0);
    Integer stance = PvpManager.canonicalStanceToOption.get(name);
    return stance == null ? -1 : stance.intValue();
  }

  public static final String findStance(final int stance) {
    return PvpManager.optionToStance.get(stance);
  }

  private static boolean checkHippyStone() {
    if (!KoLCharacter.getHippyStoneBroken()) {
      if (!InputFieldUtilities.confirm("Would you like to break your hippy stone?")) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "This feature is not available to hippies.");
        return false;
      }
      new GenericRequest("peevpee.php?action=smashstone&confirm=on").run();
      return KoLCharacter.getHippyStoneBroken();
    }
    return true;
  }

  public static void executePvpRequest(final int attacks, final String mission, final int stance) {
    if (!PvpManager.checkHippyStone()) {
      return;
    }

    PeeVPeeRequest request = new PeeVPeeRequest("", stance, mission);

    int availableFights = KoLCharacter.getAttacksLeft();
    int totalFights = (attacks > availableFights || attacks == 0) ? availableFights : attacks;
    int fightsCompleted = 0;

    while (fightsCompleted++ < totalFights) {
      // Execute the beforePVPScript to change equipment, get
      // buffs, whatever.
      KoLmafia.executeBeforePVPScript();

      // If the beforePVPScript aborts, stop before initiating a fight
      if (KoLmafia.refusesContinue()) {
        break;
      }

      KoLmafia.updateDisplay("Attack " + fightsCompleted + " of " + totalFights);
      RequestThread.postRequest(request);

      // If he wants to abort the command, honor it
      if (KoLmafia.refusesContinue()) {
        break;
      }

      // If no fight occurred, reduce fightsCompleted
      if (PvpManager.noFight) {
        fightsCompleted--;
        PvpManager.noFight = false;
      }

      KoLmafia.forceContinue();
    }

    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("You have " + KoLCharacter.getAttacksLeft() + " attacks remaining.");
    }
  }

  public static final void executePvpRequest(
      final ProfileRequest[] targets, final PeeVPeeRequest request, final int stance) {
    if (!PvpManager.checkHippyStone()) {
      return;
    }

    for (int i = 0;
        i < targets.length && KoLmafia.permitsContinue() && KoLCharacter.getAttacksLeft() > 0;
        ++i) {
      if (targets[i] == null) {
        continue;
      }

      if (Preferences.getString("currentPvpVictories").contains(targets[i].getPlayerName())) {
        continue;
      }

      if (targets[i].getPlayerName().toLowerCase().startsWith("devster")) {
        continue;
      }

      // Execute the beforePVPScript to change equipment, get buffs, whatever.
      KoLmafia.executeBeforePVPScript();

      // Choose current "best" stance
      // *** this is broken, as of Season 19
      request.addFormField("stance", String.valueOf(stance));

      KoLmafia.updateDisplay("Attacking " + targets[i].getPlayerName() + "...");
      request.setTarget(targets[i].getPlayerName());
      request.setTargetType("0");
      RequestThread.postRequest(request);

      if (request.responseText.contains("lost some dignity in the attempt")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You lost to " + targets[i].getPlayerName() + ".");
      }
    }
  }
}
