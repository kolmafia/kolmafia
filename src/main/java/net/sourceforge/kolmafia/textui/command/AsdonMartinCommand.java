package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AsdonMartinCommand extends AbstractCommand {

  private enum DrivingStyle {
    OBNOXIOUSLY("Obnoxiously", 0, EffectPool.OBNOXIOUSLY),
    STEALTHILY("Stealthily", 1, EffectPool.STEALTHILY),
    WASTEFULLY("Wastefully", 2, EffectPool.WASTEFULLY),
    SAFELY("Safely", 3, EffectPool.SAFELY),
    RECKLESSLY("Recklessly", 4, EffectPool.RECKLESSLY),
    QUICKLY("Quickly", 5, EffectPool.QUICKLY),
    INTIMIDATINGLY("Intimidatingly", 6, EffectPool.INTIMIDATINGLY),
    OBSERVANTLY("Observantly", 7, EffectPool.OBSERVANTLY),
    WATERPROOFLY("Waterproofly", 8, EffectPool.WATERPROOFLY);

    private final String name;
    private final int driveId;
    private final AdventureResult effect;

    DrivingStyle(final String name, final int driveId, final int effectId) {
      this.name = name;
      this.driveId = driveId;
      this.effect = EffectPool.get(effectId);
    }

    static DrivingStyle find(final String name) {
      for (var style : values()) {
        if (style.name.equalsIgnoreCase(name)) {
          return style;
        }
      }
      return null;
    }

    static DrivingStyle current() {
      for (var style : values()) {
        if (KoLConstants.activeEffects.contains(style.effect)) {
          return style;
        }
      }
      return null;
    }
  }

  public AsdonMartinCommand() {
    this.usage =
        " drive style [times]|clear, fuel [#] item name  - Get drive buff or convert items to fuel";
  }

  private static void undrive(final DrivingStyle style) {
    RequestThread.postRequest(
        new GenericRequest("campground.php?pwd&preaction=undrive&stop=Stop+Driving+" + style.name));
  }

  private static void drive(final DrivingStyle style, final int times) {
    post("campground.php?pwd&preaction=drive&whichdrive=" + style.driveId, times);
  }

  private static void driveMore(final int times) {
    var style = DrivingStyle.current();
    post(
        "campground.php?pwd&preaction=drive&whichdrive="
            + style.driveId
            + "&more=Drive+More+"
            + style.name,
        times);
  }

  private static void post(final String url, final int times) {
    RequestThread.postRequest(new GenericRequest(times > 1 ? url + "&drivetimes=" + times : url));
  }

  @Override
  public void run(final String cmd, final String parameters) {
    var workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    if (workshedItem == null || (workshedItem.getItemId() != ItemPool.ASDON_MARTIN)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have an Asdon Martin");
      return;
    }

    String[] params = parameters.trim().split("\\s+");

    switch (params[0]) {
      case "drive" -> {
        if (params.length < 2) {
          printUsage();
        } else if (params[1].equalsIgnoreCase("clear")) {
          clearCommand();
        } else {
          driveCommand(params[1], params.length > 2 ? params[2] : null);
        }
      }
      case "fuel" -> {
        if (params.length < 2) {
          printUsage();
        } else {
          fuelCommand(parameters.trim().substring(5));
        }
      }
      default -> printUsage();
    }
  }

  private void printUsage() {
    RequestLogger.printLine("Usage: asdonmartin" + this.usage);
  }

  private static void clearCommand() {
    var currentStyle = DrivingStyle.current();
    if (currentStyle == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a driving style");
      return;
    }
    undrive(currentStyle);
  }

  private static void driveCommand(final String styleName, final String timesParam) {
    var style = DrivingStyle.find(styleName);
    if (style == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Driving style " + styleName + " not recognised");
      return;
    }

    int times = 1;
    if (timesParam != null) {
      times = StringUtilities.parseInt(timesParam);
      if (times < 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid number of times to drive");
        return;
      }
    }

    if (CampgroundRequest.getFuel() < 37 * times) {
      RequestLogger.printLine("You haven't got enough fuel");
      return;
    }

    switch (DrivingStyle.current()) {
      case null -> drive(style, times);
      case DrivingStyle current when current == style -> driveMore(times);
      case DrivingStyle current -> {
        undrive(current);
        drive(style, times);
      }
    }
  }

  private static void fuelCommand(final String param) {
    AdventureResult item = ItemFinder.getFirstMatchingItem(param, true, null, Match.ASDON);
    if (item == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, param + " cannot be used as fuel.");
      return;
    }
    if (!InventoryManager.checkpointedRetrieveItem(item)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have enough " + item.getDataName() + ".");
      return;
    }
    if (item.getCount() > 0) {
      CampgroundRequest request = new CampgroundRequest("fuelconvertor");
      request.addFormField("qty", String.valueOf(item.getCount()));
      request.addFormField("iid", String.valueOf(item.getItemId()));
      RequestThread.postRequest(request);
    }
  }
}
