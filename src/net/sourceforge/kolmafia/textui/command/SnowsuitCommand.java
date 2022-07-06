package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SnowsuitCommand extends AbstractCommand implements ModeCommand {
  public static final Map<String, Integer> MODES =
      Map.ofEntries(
          Map.entry("eyebrows", 1),
          Map.entry("smirk", 2),
          Map.entry("nose", 3),
          Map.entry("goatee", 4),
          Map.entry("hat", 5));

  public SnowsuitCommand() {
    this.usage = "[?] <decoration> - decorate Snowsuit (and equip it if unequipped)";
  }

  private int getChoice(final String parameters) {
    return MODES.getOrDefault(parameters.toLowerCase(), 0);
  }

  @Override
  public boolean validate(final String command, final String parameters) {
    return getChoice(parameters) != 0;
  }

  @Override
  public String normalize(String parameters) {
    return parameters;
  }

  @Override
  public Set<String> getModes() {
    return MODES.keySet();
  }

  private static String getModeFromDecision(int decision) {
    return MODES.entrySet().stream()
        .filter(e -> e.getValue() == decision)
        .findAny()
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Set the snowsuit pref from a decision number
   *
   * @param decision Decision number (also acts as an id for the mode)
   */
  public static void setStateFromDecision(int decision) {
    String mode = getModeFromDecision(decision);
    if (mode != null) {
      Preferences.setString("snowsuit", mode);
    }
  }

  private static final Pattern CHARPANE_PATTERN = Pattern.compile("snowface([1-5]).gif");

  /**
   * Sets correct value based on information from KoL
   *
   * @param responseText Text from a CharPaneRequest
   */
  public static void check(final String responseText) {
    Matcher matcher = CHARPANE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int decision = StringUtilities.parseInt(matcher.group(1));
      setStateFromDecision(decision);
    }
  }

  @Override
  public void run(final String cmd, String parameters) {
    String currentDecoration = Preferences.getString("snowsuit");

    if (parameters.length() == 0) {
      KoLmafia.updateDisplay("Current decoration on Snowsuit is " + currentDecoration);
      return;
    }

    int choice = getChoice(parameters);

    if (choice == 0) {
      KoLmafia.updateDisplay(
          "Decoration "
              + parameters
              + " not recognised. Valid values are eyebrows, goatee, hat, nose and smirk");
      return;
    }

    if (EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).getItemId()
        != ItemPool.SNOW_SUIT) {
      AdventureResult snowsuit = ItemPool.get(ItemPool.SNOW_SUIT);
      RequestThread.postRequest(new EquipmentRequest(snowsuit, EquipmentManager.FAMILIAR));
    }

    if (parameters.equalsIgnoreCase(currentDecoration)) {
      KoLmafia.updateDisplay("Decoration " + parameters + " already equipped.");
      return;
    }

    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("inventory.php?action=decorate"));
    }
    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=640&option=" + choice));
    }
    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Snowsuit decorated with " + parameters + ".");
    }
  }
}
