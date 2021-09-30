package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;

public class SetPreferencesCommand extends AbstractCommand {
  public SetPreferencesCommand() {
    this.usage = " <preference> [ = <value> ] - show/change preference settings";
    this.flags = KoLmafiaCLI.FULL_LINE_CMD;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int splitIndex = parameters.indexOf("=");
    if (splitIndex == -1) {
      // Allow reading of system properties

      if (parameters.startsWith("System.")) {
        RequestLogger.printLine(System.getProperty(parameters.substring(7)));
      } else if (Preferences.isUserEditable(parameters)) {
        RequestLogger.printLine(Preferences.getString(parameters));
      }

      return;
    }

    String name = parameters.substring(0, splitIndex).trim();
    if (!Preferences.isUserEditable(name)) {
      return;
    }

    String value = parameters.substring(splitIndex + 1).trim();
    if (value.startsWith("\"")) {
      value = value.substring(1, value.endsWith("\"") ? value.length() - 1 : value.length());
    }

    while (value.endsWith(";")) {
      value = value.substring(0, value.length() - 1).trim();
    }

    SetPreferencesCommand.setProperty(name, value, true);
  }

  public static void setProperty(String name, String value, boolean print) {
    if (name.equals("battleAction")) {
      if (value.contains(";") || value.startsWith("consult")) {
        CombatActionManager.setDefaultAction(value);
        value = "custom combat script";
      } else {
        value = CombatActionManager.getLongCombatOptionName(value);
      }

      if (value == null) {
        return;
      }

      // Special handling of the battle action property,
      // such that auto-recovery gets reset as needed.
      LockableListFactory.setSelectedItem(KoLCharacter.getBattleSkillNames(), value);
    }

    if (name.equals("customCombatScript")) {
      ChangeCombatScriptCommand.update(value);
      return;
    }

    if (name.startsWith("combatHotkey")) {
      String desiredValue = CombatActionManager.getLongCombatOptionName(value);

      if (!desiredValue.startsWith("attack") || value.startsWith("attack")) {
        value = desiredValue;
      }
    }

    if (name.equals("_userMods")) {
      Modifiers.overrideModifier("Generated:_userMods", value);
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
    }

    if (Preferences.getString(name).equals(value)) {
      return;
    }

    // suppress CLI output iff it is a pref that starts with _ AND is not defined in defaults.txt
    if (print && !name.startsWith("_") || Preferences.containsDefault(name)) {
      RequestLogger.printLine(name + " => " + value);
    }

    Preferences.setString(name, value);

    if (name.equals("currentMood")) {
      MoodManager.setMood(value);
    }

    if (name.startsWith("combatHotkey")) {
      StationaryButtonDecorator.reloadCombatHotkeyMap();
    }
  }
}
