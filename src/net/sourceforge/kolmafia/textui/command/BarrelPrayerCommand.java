package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;

public class BarrelPrayerCommand extends AbstractCommand {
  public BarrelPrayerCommand() {
    this.usage = " protection | glamour | vigor | buff ";
  }

  public static final int PROTECTION = 1;
  public static final int GLAMOUR = 2;
  public static final int VIGOR = 3;
  public static final int BUFF = 4;

  public static final Object[][] PRAYER =
      new Object[][] {
        {"protection", "barrel lid", IntegerPool.get(PROTECTION)},
        {"glamour", "barrel hoop earring", IntegerPool.get(GLAMOUR)},
        {"vigor", "bankruptcy barrel", IntegerPool.get(VIGOR)},
        {"buff", "class buff", IntegerPool.get(BUFF)},
      };

  public static final int findPrayer(final String name) {
    for (int i = 0; i < PRAYER.length; ++i) {
      if (name.equalsIgnoreCase((String) PRAYER[i][0])
          || name.equalsIgnoreCase((String) PRAYER[i][1])) {
        Integer index = (Integer) PRAYER[i][2];
        return index.intValue();
      }
    }

    return 0;
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    if (parameters.equals("")) {
      RequestLogger.printLine("Usage: barrelprayer" + this.usage);
      RequestLogger.printLine("protection or barrel lid: get barrel lid (1/ascension)");
      RequestLogger.printLine(
          "glamour or barrel hoop earring: get barrel hoop earring (1/ascension)");
      RequestLogger.printLine("vigor or bankruptcy barrel : get bankruptcy barrel (1/ascension)");
      RequestLogger.printLine("buff: get class buff");
      return;
    }

    int option = BarrelPrayerCommand.findPrayer(parameters);
    if (option == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what '" + parameters + "' barrel prayer is.");
      return;
    }

    if (!Preferences.getBoolean("barrelShrineUnlocked")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Barrel Shrine not installed");
      return;
    }

    if (!StandardRequest.isAllowed("Items", "shrine to the Barrel god")) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Your path restricts you from approaching the Barrel Shrine");
      return;
    }

    if (Preferences.getBoolean("_barrelPrayer")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already prayed to the Barrel God today.");
      return;
    }

    if ((option == 1 && Preferences.getBoolean("prayedForProtection"))
        || (option == 2 && Preferences.getBoolean("prayedForGlamour"))
        || (option == 3 && Preferences.getBoolean("prayedForVigor"))) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already prayed for that item this ascension.");
      return;
    }

    GenericRequest request = new GenericRequest("da.php?barrelshrine=1");
    RequestThread.postRequest(request);
    request.constructURLString("choice.php?whichchoice=1100&option=" + option);
    RequestThread.postRequest(request);
    ConcoctionDatabase.refreshConcoctionsNow();
  }
}
