package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Arrays;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class TinkeringBenchRequest extends CoinMasterShopRequest {
  public static final String master = "Tinkering Bench";
  public static final String SHOPID = "wereprofessor_tinker";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, TinkeringBenchRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(TinkeringBenchRequest::canBuyItem)
          .withAccessible(TinkeringBenchRequest::accessible)
          .withPurchasedItem(TinkeringBenchRequest::purchasedItem)
          // previous version didn't use ajax so we won't here either
          .withAjax(false);

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
  }

  public static boolean canMake(final int itemId) {
    return TinkeredItem.find(itemId).canMake();
  }

  public static boolean haveItem(final int itemId) {
    return TinkeredItem.find(itemId).haveItem();
  }

  private static Boolean canBuyItem(final Integer itemId) {
    if (!TinkeringBenchRequest.canMake(itemId)) {
      return false;
    }
    return DATA.availableItem(itemId);
  }

  public static String accessible() {
    if (!KoLCharacter.isMildManneredProfessor()) {
      return "Only a mild-mannered professor can work at their Tinkering Bench.";
    }
    return null;
  }

  public static void purchasedItem(final AdventureResult item, final Boolean storage) {
    // CreateItemRequest makes an outfit Checkpoint in its item creation
    // loop. If we were wearing an item which we are upgrading, we don't
    // want it to try to equip it, find it is gone, and try to make
    // another one. It won't be possible, but its wasted effort and
    // obfuscates the logging.
    var shopRow = DATA.getShopRow(item.getItemId());
    if (shopRow == null) {
      var message = "Unrecognised shop row for Tinkering Bench creation of " + item.getName();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return;
    }
    var ingredients = shopRow.getCosts();
    for (AdventureResult ingredient : ingredients) {
      SpecialOutfit.forgetEquipment(ingredient);
    }
  }
}
