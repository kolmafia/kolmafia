package net.sourceforge.kolmafia.textui.command;

import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SnowsuitCommand extends AbstractCommand implements ModeCommand {
  public static final Map<Integer, String> MODES =
      Map.ofEntries(
          Map.entry(1, "eyebrows"),
          Map.entry(2, "smirk"),
          Map.entry(3, "nose"),
          Map.entry(4, "goatee"),
          Map.entry(5, "hat"));

  public SnowsuitCommand() {
    this.usage = "[?] <decoration> - decorate Snowsuit (and equip it if unequipped)";
  }

  private int findDecision(final String parameters) {
    return MODES.entrySet().stream()
        .filter(e -> e.getValue().equalsIgnoreCase(parameters))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(0);
  }

  @Override
  public boolean validate(final String command, final String parameters) {
    return findDecision(parameters) != 0;
  }

  public String normalize(String parameters) {
    return parameters;
  }

  @Override
  public HashSet<String> getModes() {
    return new HashSet<>(MODES.values());
  }

  private static String getModeFromDecision(int decision) {
    return MODES.getOrDefault(decision, null);
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

    int choice = findDecision(parameters);

    if (choice == 0) {
      KoLmafia.updateDisplay(
          "Decoration "
              + parameters
              + " not recognised. Valid values are eyebrows, goatee, hat, nose and smirk");
      return;
    }

    if (EquipmentManager.getEquipment(Slot.FAMILIAR).getItemId() != ItemPool.SNOW_SUIT) {
      AdventureResult snowsuit = ItemPool.get(ItemPool.SNOW_SUIT);
      RequestThread.postRequest(new EquipmentRequest(snowsuit, Slot.FAMILIAR));
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
