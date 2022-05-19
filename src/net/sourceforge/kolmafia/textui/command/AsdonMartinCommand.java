package net.sourceforge.kolmafia.textui.command;

import java.util.List;
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

public class AsdonMartinCommand extends AbstractCommand {

  private static class DriveStyle {
    public String name;
    public int driveId;
    public AdventureResult effect;

    public DriveStyle(String name, int driveId, AdventureResult effect) {
      this.name = name;
      this.driveId = driveId;
      this.effect = effect;
    }
  }

  private static final DriveStyle[] DRIVESTYLE =
      new DriveStyle[] {
        new DriveStyle("Obnoxiously", 0, EffectPool.get(EffectPool.OBNOXIOUSLY)),
        new DriveStyle("Stealthily", 1, EffectPool.get(EffectPool.STEALTHILY)),
        new DriveStyle("Wastefully", 2, EffectPool.get(EffectPool.WASTEFULLY)),
        new DriveStyle("Safely", 3, EffectPool.get(EffectPool.SAFELY)),
        new DriveStyle("Recklessly", 4, EffectPool.get(EffectPool.RECKLESSLY)),
        new DriveStyle("Quickly", 5, EffectPool.get(EffectPool.QUICKLY)),
        new DriveStyle("Intimidatingly", 6, EffectPool.get(EffectPool.INTIMIDATINGLY)),
        new DriveStyle("Observantly", 7, EffectPool.get(EffectPool.OBSERVANTLY)),
        new DriveStyle("Waterproofly", 8, EffectPool.get(EffectPool.WATERPROOFLY)),
      };

  public AsdonMartinCommand() {
    this.usage =
        " drive style|clear, fuel [#] item name  - Get drive buff or convert items to fuel";
  }

  private static int findDriveStyle(final String name) {
    for (DriveStyle driveStyle : DRIVESTYLE) {
      if (name.equalsIgnoreCase(driveStyle.name)) {
        return driveStyle.driveId;
      }
    }
    return -1;
  }

  private static String driveStyleName(final int index) {
    if (index < 0 || index > 8) {
      return null;
    }
    return DRIVESTYLE[index].name;
  }

  private static int currentDriveStyle() {
    List<AdventureResult> active = KoLConstants.activeEffects;
    for (DriveStyle driveStyle : DRIVESTYLE) {
      if (active.contains(driveStyle.effect)) {
        return driveStyle.driveId;
      }
    }
    return -1;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    var workshedItem = CampgroundRequest.getCurrentWorkshedItem();
    if (workshedItem == null || (workshedItem.getItemId() != ItemPool.ASDON_MARTIN)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have an Asdon Martin");
      return;
    }

    String[] params = parameters.trim().split("\\s+");
    String command = params[0];

    if (command.equals("drive")) {
      if (params.length < 2) {
        RequestLogger.printLine("Usage: asdonmartin" + this.usage);
        return;
      }
      String driveStyle = params[1];
      if (driveStyle.equalsIgnoreCase("clear")) {
        int currentStyle = AsdonMartinCommand.currentDriveStyle();
        if (currentStyle == -1) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a driving style");
          return;
        }
        String request =
            "campground.php?pwd&preaction=undrive&stop=Stop+Driving+"
                + AsdonMartinCommand.driveStyleName(currentStyle);
        // Remove driving style
        RequestThread.postRequest(new GenericRequest(request));
        return;
      } else {
        int style = AsdonMartinCommand.findDriveStyle(driveStyle);
        if (style == -1) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "Driving style " + driveStyle + " not recognised");
          return;
        }

        if (CampgroundRequest.getFuel() < 37) {
          RequestLogger.printLine("You haven't got enough fuel");
          return;
        }

        int currentStyle = AsdonMartinCommand.currentDriveStyle();
        if (currentStyle == -1) {
          // Get buff, none to remove or extend
          RequestThread.postRequest(
              new GenericRequest("campground.php?preaction=drive&whichdrive=" + style));
          return;
        } else if (currentStyle == style) {
          // Extend buff
          String request =
              "campground.php?pwd&preaction=drive&whichdrive="
                  + style
                  + "&more=Drive+More+"
                  + AsdonMartinCommand.driveStyleName(style);
          RequestThread.postRequest(new GenericRequest(request));
          return;
        } else {
          // Remove buff
          String request =
              "campground.php?pwd&preaction=undrive&stop=Stop+Driving+"
                  + AsdonMartinCommand.driveStyleName(currentStyle);
          RequestThread.postRequest(new GenericRequest(request));
          // Get new buff
          RequestThread.postRequest(
              new GenericRequest("campground.php?preaction=drive&whichdrive=" + style));
          return;
        }
      }
    } else if (command.equals("fuel")) {
      String param = parameters.substring(5);
      AdventureResult item = ItemFinder.getFirstMatchingItem(param, true, null, Match.ASDON);
      if (item == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, param + " cannot be used as fuel.");
        return;
      }
      if (!InventoryManager.checkpointedRetrieveItem(item)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You don't have enough " + item.getDataName() + ".");
        return;
      }
      if (item.getCount() > 0) {
        CampgroundRequest request = new CampgroundRequest("fuelconvertor");
        request.addFormField("qty", String.valueOf(item.getCount()));
        request.addFormField("iid", String.valueOf(item.getItemId()));
        RequestThread.postRequest(request);
      }
      return;
    }

    RequestLogger.printLine("Usage: asdonmartin" + this.usage);
  }
}
