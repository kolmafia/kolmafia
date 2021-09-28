package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PoolskillCommand extends AbstractCommand {
  public PoolskillCommand() {
    this.usage = " - display estimated Pool skill.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int drunk = KoLCharacter.getInebriety();
    int drunkBonus = drunk - (drunk > 10 ? (drunk - 10) * 3 : 0);
    int equip = KoLCharacter.getPoolSkill();
    int semiRare = Preferences.getInteger("poolSharkCount");
    int semiRareBonus = 0;
    if (semiRare > 25) {
      semiRareBonus = 10;
    } else if (semiRare > 0) {
      semiRareBonus = (int) Math.floor(2 * Math.sqrt(semiRare));
    }
    int training = Preferences.getInteger("poolSkill");
    int poolSkill = equip + training + semiRareBonus + drunkBonus;

    RequestLogger.printLine("Pool Skill is estimated at : " + poolSkill + ".");
    RequestLogger.printLine(
        equip
            + " from equipment, "
            + drunkBonus
            + " from having "
            + drunk
            + " inebriety, "
            + training
            + " hustling training and "
            + semiRareBonus
            + " learning from "
            + semiRare
            + " sharks.");
  }
}
