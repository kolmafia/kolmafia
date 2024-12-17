package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class TakerSpaceRequest extends CreateItemRequest {
  private record Ingredients(int spice, int rum, int anchor, int mast, int silk, int gold) {}

  public TakerSpaceRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1537");
    this.addFormField("option", "1");
    var ingredients = getItemIngredients(this.getItemId());
    this.addFormField("spice", String.valueOf(ingredients.spice));
    this.addFormField("rum", String.valueOf(ingredients.rum));
    this.addFormField("anchor", String.valueOf(ingredients.anchor));
    this.addFormField("mast", String.valueOf(ingredients.mast));
    this.addFormField("silk", String.valueOf(ingredients.silk));
    this.addFormField("gold", String.valueOf(ingredients.gold));
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    int count = this.getQuantityNeeded();
    if (count == 0) {
      return;
    }

    RequestThread.postRequest(new CampgroundRequest("workshed"));

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");

    super.run();
  }

  public static int canMake(final Concoction conc) {
    var ingredients = getItemIngredients(conc.getItemId());

    int canMake = Integer.MAX_VALUE;

    var spice = ingredients.spice;
    if (spice > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceSpice") / spice);
    }
    var rum = ingredients.rum;
    if (rum > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceRum") / rum);
    }
    var anchor = ingredients.anchor;
    if (anchor > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceAnchor") / anchor);
    }
    var mast = ingredients.mast;
    if (mast > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceMast") / mast);
    }
    var silk = ingredients.silk;
    if (silk > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceSilk") / silk);
    }
    var gold = ingredients.gold;
    if (gold > 0) {
      canMake = Math.min(canMake, Preferences.getInteger("takerSpaceGold") / gold);
    }

    return canMake;
  }

  private static Ingredients getItemIngredients(int itemId) {
    return switch (itemId) {
      case ItemPool.DEFT_PIRATE_HOOK -> new Ingredients(0, 0, 1, 1, 0, 1);
      case ItemPool.IRON_TRICORN_HAT -> new Ingredients(0, 0, 2, 1, 0, 0);
      case ItemPool.JOLLY_ROGER_FLAG -> new Ingredients(0, 1, 0, 1, 1, 0);
      case ItemPool.SLEEPING_PROFANE_PARROT -> new Ingredients(15, 3, 0, 0, 2, 1);
      case ItemPool.PIRRRATES_CURRRSE -> new Ingredients(2, 2, 0, 0, 0, 0);
      case ItemPool.TANKARD_OF_SPICED_RUM -> new Ingredients(1, 2, 0, 0, 0, 0);
      case ItemPool.TANKARD_OF_SPICED_GOLDSCHLEPPER -> new Ingredients(0, 2, 0, 0, 0, 1);
      case ItemPool.PACKAGED_LUXURY_GARMENT -> new Ingredients(0, 0, 0, 0, 3, 2);
      case ItemPool.HARPOON -> new Ingredients(0, 0, 0, 2, 0, 0);
      case ItemPool.CHILI_POWDER_CUTLASS -> new Ingredients(5, 0, 1, 0, 0, 0);
      case ItemPool.CURSED_AZTEC_TAMALE -> new Ingredients(2, 0, 0, 0, 0, 0);
      case ItemPool.JOLLY_ROGER_TATTOO_KIT -> new Ingredients(0, 6, 1, 1, 0, 6);
      case ItemPool.GOLDEN_PET_ROCK -> new Ingredients(0, 0, 0, 0, 0, 7);
      case ItemPool.GROGGLES -> new Ingredients(0, 6, 0, 0, 0, 0);
      case ItemPool.PIRATE_DINGHY -> new Ingredients(0, 0, 1, 1, 1, 0);
      case ItemPool.ANCHOR_BOMB -> new Ingredients(0, 1, 3, 1, 0, 1);
      case ItemPool.SILKY_PIRATE_DRAWERS -> new Ingredients(0, 0, 0, 0, 2, 0);
      case ItemPool.SPICES -> new Ingredients(1, 0, 0, 0, 0, 0);
      default -> new Ingredients(0, 0, 0, 0, 0, 0);
    };
  }
}
