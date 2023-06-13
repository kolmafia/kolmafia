package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropPlot;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;

public class GardenCommand extends AbstractCommand {
  public GardenCommand() {
    this.usage = " [pick] - get status of garden, or harvest it.";
  }

  private boolean checkMushroomGarden(CropType cropType) {
    if (cropType != CropType.MUSHROOM) {
      KoLmafia.updateDisplay("You don't have a mushroom garden.");
      return false;
    }
    if (Preferences.getBoolean("_mushroomGardenVisited")) {
      KoLmafia.updateDisplay("You've already dealt with your mushroom garden today.");
      return false;
    }
    if (KoLCharacter.isFallingDown()
        && !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.DRUNKULA_WINEGLASS))) {
      KoLmafia.updateDisplay("You are too drunk to enter your mushroom garden.");
      return false;
    }
    if (KoLCharacter.getAdventuresLeft() <= 0) {
      KoLmafia.updateDisplay("You need an available turn to fight through piranha plants.");
      return false;
    }
    return true;
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (KoLCharacter.isEd()
        || KoLCharacter.inNuclearAutumn()
        || KoLCharacter.getLimitMode().limitCampground()) {
      KoLmafia.updateDisplay("You can't get to your campground to visit your garden.");
      return;
    }

    List<AdventureResult> crops = CampgroundRequest.getCrops();

    if (crops.isEmpty()) {
      KoLmafia.updateDisplay("You don't have a garden.");
      return;
    }

    String[] split = parameters.split(" +");
    String command = split[0];

    CropType cropType = CampgroundRequest.getCropType(crops.get(0));

    if (command.equals("")) {
      String gardenType = cropType.toString();
      StringBuilder display = new StringBuilder();
      display.append("Your ").append(gardenType).append(" garden has ");
      if (cropType == CropType.ROCK) {
        boolean first = true;
        for (var crop : crops) {
          if (crop.getCount() > 0) {
            if (!first) {
              display.append(", and ");
            }
            int count = crop.getPluralCount();
            String name = crop.getPluralName();
            display.append(count).append(" ").append(name);
            first = false;
          }
        }
        if (first) {
          display.append("nothing");
        }
      } else {
        var onlyCrop = crops.get(0);
        int count = onlyCrop.getPluralCount();
        String name = onlyCrop.getPluralName();
        display.append(count).append(" ").append(name);
      }
      display.append(" in it.");
      KoLmafia.updateDisplay(display.toString());
      return;
    }

    if (command.equals("fertilize")) {
      // Mushroom garden only
      if (checkMushroomGarden(cropType)) {
        CampgroundRequest.harvestMushrooms(false);
      }
      return;
    }

    if (command.equals("pick")) {
      // Mushroom garden only
      if (cropType == CropType.MUSHROOM) {
        if (checkMushroomGarden(cropType)) {
          CampgroundRequest.harvestMushrooms(true);
        }
        return;
      }

      // Rock garden only
      if (cropType == CropType.ROCK && split.length > 1) {
        Set<CropPlot> plots =
            Arrays.stream(split, 1, split.length)
                .map(CropPlot::nameToPlot)
                .filter(x -> x != null)
                .collect(Collectors.toSet());
        if (plots.size() > 0) {
          for (var crop : crops) {
            if (crop.getCount() == 0) {
              continue;
            }
            CropPlot plot = CampgroundRequest.cropToPlot(crop);
            if (plot == null) {
              // Not expected
              continue;
            }
            if (!plots.contains(plot)) {
              continue;
            }
            RequestLogger.printLine("Harvesting " + plot + ": " + crop);
            CampgroundRequest.harvestCrop(plot);
            plots.remove(plot);
          }
          for (var plot : plots) {
            RequestLogger.printLine("There is nothing to pick in " + plot + ".");
          }
        }

        return;
      }

      int count = crops.stream().mapToInt(AdventureResult::getCount).sum();
      if (count == 0) {
        KoLmafia.updateDisplay("There is nothing ready to pick in your garden.");
        return;
      }

      CampgroundRequest.harvestCrop();
      return;
    }
  }
}
