package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
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
    if (KoLCharacter.isFallingDown()) {
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

    CropType cropType = CampgroundRequest.getCropType(crops.get(0));

    if (parameters.equals("")) {
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

    if (parameters.equals("fertilize")) {
      // Mushroom garden only
      if (checkMushroomGarden(cropType)) {
        CampgroundRequest.harvestMushrooms(false);
      }
      return;
    }

    if (parameters.equals("pick")) {
      // Mushroom garden only
      if (cropType == CropType.MUSHROOM && checkMushroomGarden(cropType)) {
        CampgroundRequest.harvestMushrooms(true);
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
