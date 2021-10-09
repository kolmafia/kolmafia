package net.sourceforge.kolmafia.request;

import java.util.EnumSet;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class CraftRequest extends GenericRequest {
  private CraftingType mixingMethod;
  private final int quantity;
  private final AdventureResult item1;
  private final AdventureResult item2;
  private int remaining;
  private int created;

  public CraftRequest(final String mode, final int quantity, final int itemId1, final int itemId2) {
    super("craft.php");

    this.addFormField("action", "craft");
    this.setMixingMethod(mode);
    this.addFormField("a", String.valueOf(itemId1));
    this.addFormField("b", String.valueOf(itemId2));

    this.quantity = quantity;
    this.remaining = quantity;
    this.item1 = ItemPool.get(itemId1, quantity);
    this.item2 = ItemPool.get(itemId2, quantity);
  }

  private void setMixingMethod(final String mode) {
    if (mode.equals("combine")) {
      this.mixingMethod = CraftingType.COMBINE;
    } else if (mode.equals("cocktail")) {
      this.mixingMethod = CraftingType.MIX;
    } else if (mode.equals("cook")) {
      this.mixingMethod = CraftingType.COOK;
    } else if (mode.equals("smith")) {
      this.mixingMethod = CraftingType.SMITH;
    } else if (mode.equals("jewelry")) {
      this.mixingMethod = CraftingType.JEWELRY;
    } else {
      this.mixingMethod = CraftingType.NOCREATE;
      return;
    }

    this.addFormField("mode", mode);
  }

  public int created() {
    return this.quantity - this.remaining;
  }

  @Override
  public void run() {
    if (this.mixingMethod == CraftingType.NOCREATE
        || this.quantity <= 0
        || !KoLmafia.permitsContinue()) {
      return;
    }

    // Get all the ingredients up front

    if (!InventoryManager.retrieveItem(this.item1) || !InventoryManager.retrieveItem(this.item2)) {
      return;
    }

    this.remaining = this.quantity;

    while (this.remaining > 0 && KoLmafia.permitsContinue()) {
      if (!CreateItemRequest.autoRepairBoxServant(
          this.mixingMethod, EnumSet.noneOf(CraftingRequirements.class))) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Auto-repair was unsuccessful.");
        return;
      }

      this.addFormField("qty", String.valueOf(this.remaining));
      this.created = 0;

      super.run();

      if (this.responseCode == 302 && this.redirectLocation.startsWith("inventory")) {
        CreateItemRequest.REDIRECT_REQUEST.constructURLString(this.redirectLocation).run();
      }

      if (this.created == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        return;
      }

      this.remaining -= this.created;
    }
  }

  @Override
  public void processResults() {
    this.created = CreateItemRequest.parseCrafting(this.getURLString(), this.responseText);

    if (this.responseText.indexOf("Smoke") != -1) {
      KoLmafia.updateDisplay("Your box servant has escaped!");
    }
  }
}
