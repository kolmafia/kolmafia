package net.sourceforge.kolmafia.request.concoction.shop;

import java.util.Arrays;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class TinkeringBenchRequest extends CreateItemRequest {

  // There are 10 items you can make at the Tinkering Bench.
  // You can only ever make one of each, even if you've upgraded.

  public enum TinkeredItem {
    NONE(0),
    BIPHASIC_OCULUS(ItemPool.BIPHASIC_MOLECULAR_OCULUS, ItemPool.TRIPHASIC_MOLECULAR_OCULUS),
    TRIPHASIC_OCULUS(ItemPool.TRIPHASIC_MOLECULAR_OCULUS),
    MAGNETRON_PISTOL(ItemPool.FOCUSED_MAGNETRON_PISTOL),
    MOTION_SENSOR(ItemPool.MOTION_SENSOR),
    BELT_POUCH(ItemPool.QUICK_RELEASE_BELT_POUCH, ItemPool.QUICK_RELEASE_FANNYPACK),
    FANNYPACK(ItemPool.QUICK_RELEASE_FANNYPACK, ItemPool.QUICK_RELEASE_UTILITY_BELT),
    UTILITY_BELT(ItemPool.QUICK_RELEASE_UTILITY_BELT),
    HIGH_TENSION(ItemPool.HIGH_TENSION_EXOSKELETON, ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON),
    ULTRA_HIGH_TENSION(
        ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON, ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON),
    IRRESPONSIBLE_TENSION(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON);

    AdventureResult item;
    AdventureResult nextItem;

    TinkeredItem(final int itemId, final int nextItemId) {
      this.item = ItemPool.get(itemId);
      this.nextItem = ItemPool.get(nextItemId);
    }

    TinkeredItem(final int itemId) {
      this(itemId, 0);
    }

    public boolean haveItem() {
      return InventoryManager.getAccessibleCount(this.item) > 0;
    }

    private boolean haveCreatedItem() {
      if (this.haveItem()) {
        return true;
      }
      if (this.nextItem.getItemId() == 0) {
        return false;
      }
      return find(this.nextItem).haveCreatedItem();
    }

    public boolean canMake() {
      return !this.haveCreatedItem();
    }

    public static TinkeredItem find(final int id) {
      return Arrays.stream(TinkeredItem.values())
          .filter(o -> o.item.getItemId() == id)
          .findAny()
          .orElse(NONE);
    }

    public static TinkeredItem find(AdventureResult ar) {
      return find(ar.getItemId());
    }

    public static TinkeredItem find(final Concoction conc) {
      return find(conc.getItemId());
    }
  }

  public static boolean canMake(final Concoction conc) {
    return TinkeredItem.find(conc).canMake();
  }

  public static boolean haveItem(final Concoction conc) {
    return TinkeredItem.find(conc).haveItem();
  }

  public TinkeringBenchRequest(final Concoction conc) {
    super("shop.php", conc);

    this.addFormField("whichshop", "wereprofessor_tinker");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
    // You can only ever create 1 of each item
    this.setQuantityNeeded(1);
    this.addFormField("quantity", "1");
  }

  @Override
  public void run() {
    // Attempt to retrieve the ingredients
    if (!this.makeIngredients()) {
      return;
    }

    // CreateItemRequest makes an outfit Checkpoint in its item creation
    // loop. If we were wearing an item which we are upgrading, we don't
    // want it to try to equip it, find it is gone, and try to make
    // another one. It won't be possible, but its wasted effort and
    // obfuscates the logging.
    for (AdventureResult item : this.concoction.getIngredients()) {
      SpecialOutfit.forgetEquipment(item);
    }

    KoLmafia.updateDisplay("Creating 1 " + this.getName() + "...");
    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "Creating at Tinkering Bench was unsuccessful.");
      return;
    }

    TinkeringBenchRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=wereprofessor_tinker")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=wereprofessor_tinker")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
