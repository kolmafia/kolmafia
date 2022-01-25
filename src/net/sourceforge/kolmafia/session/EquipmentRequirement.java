package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentRequirement {
  private Stat stat = Stat.NONE;
  private int amount;

  public EquipmentRequirement(String requirement) {
    if (requirement.startsWith("Mus:")) {
      stat = Stat.MUSCLE;
      amount = StringUtilities.parseInt(requirement.substring(5));
    }

    if (requirement.startsWith("Mys:")) {
      stat = Stat.MYSTICALITY;
      amount = StringUtilities.parseInt(requirement.substring(5));
    }

    if (requirement.startsWith("Mox:")) {
      stat = Stat.MOXIE;
      amount = StringUtilities.parseInt(requirement.substring(5));
    }
  }

  public boolean isMuscle() {
    return stat == Stat.MUSCLE;
  }

  public boolean isMysticality() {
    return stat == Stat.MYSTICALITY;
  }

  public boolean isMoxie() {
    return stat == Stat.MOXIE;
  }

  public Stat getStat() {
    return stat;
  }

  public int getAmount() {
    return amount;
  }
}
