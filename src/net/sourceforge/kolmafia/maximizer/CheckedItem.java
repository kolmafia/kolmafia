package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.MrStoreRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;

public class CheckedItem extends AdventureResult {
  public CheckedItem(int itemId, EquipScope equipScope, long maxPrice, PriceLevel priceLevel) {
    super(itemId, 1, false);

    this.inventory = InventoryManager.getCount(itemId);

    this.initial = InventoryManager.getAccessibleCount(itemId);

    // special case used to get a CheckItem that .equals( EquipmentRequest.UNEQUIP ).
    if (itemId == -1) {
      this.name = "(none)";
      this.inventory = Integer.MAX_VALUE;
      this.initial = Integer.MAX_VALUE;
    }

    String itemName = this.getName();
    this.foldable = 0;

    if (itemId > 0 && Preferences.getBoolean("maximizerFoldables")) {
      FoldGroup group = ItemDatabase.getFoldGroup(itemName);
      if (group != null) {
        for (int i = 0; i < group.names.size(); ++i) {
          String form = group.names.get(i);
          if (!form.equals(itemName)) {
            int foldItemId = ItemDatabase.getItemId(form);
            int count = InventoryManager.getAccessibleCount(foldItemId);
            this.foldable += count;
            if (count > 0) {
              this.foldItemId = foldItemId;
            }
          }
        }
        // Cannot have more than one item from January's Garbage Tote, no matter how many you have
        //
        // The actual tote (or replica tote), must be accessible, since we "fold"
        // by using that item and selecting a choice.
        //
        // Fold groups are stored in lower case
        if (group.names.get(0).equals("january's garbage tote")) {
          if (this.foldable + this.initial > 1) {
            this.foldable = 1 - this.initial;
          }
          if (this.foldable > 0) {
            int tote =
                KoLCharacter.inLegacyOfLoathing()
                        && InventoryManager.hasItem(ItemPool.REPLICA_GARBAGE_TOTE)
                    ? ItemPool.REPLICA_GARBAGE_TOTE
                    : ItemPool.GARBAGE_TOTE;
            if (InventoryManager.getAccessibleCount(tote) == 0) {
              this.foldable = 0;
            }
          }
        }
      }
    }

    boolean skillCreateCheck =
        Preferences.getBoolean("maximizerCreateOnHand")
            && equipScope == EquipScope.SPECULATE_INVENTORY
            && !ItemDatabase.isEquipment(itemId);
    if (this.initial >= 3 || (equipScope.checkInventoryOnly() && !skillCreateCheck)) {
      return;
    }

    Concoction c = ConcoctionPool.get(itemId);
    if (c == null) return;

    this.creatable = c.creatable;

    if (c.getAdventuresNeeded(1) > 0 && Preferences.getBoolean("maximizerNoAdventures")) {
      this.creatable = 0;
    } else if (c.price > 0) {
      long theoreticBuyable = maxPrice / c.price;
      int limit = CheckedItem.limitBuyable(itemId);
      if (limit < theoreticBuyable) {
        this.npcBuyable = limit;
      } else {
        // SAFETY: limitBuyable caps at Integer.MAX_VALUE
        this.npcBuyable = (int) theoreticBuyable;
      }
    }

    if (this.getCount() >= 3 || equipScope != EquipScope.SPECULATE_ANY) {
      return;
    }

    if (!ItemDatabase.isAllowed(this)) {
      // Unallowed items can't be bought or pulled, though the original code
      // just reset everything to zero

      this.initial = 0;
      this.creatable = 0;
      this.npcBuyable = 0;
    } else if (InventoryManager.canUseMall(itemId)) {
      // consider Mall buying, but only if none are otherwise available
      if (this.getCount() == 0) {
        // We include things with historical price up to twice as high as limit, as current price
        // may be lower
        long price = Math.min(maxPrice, KoLCharacter.getAvailableMeat());
        if (priceLevel == PriceLevel.DONT_CHECK || MallPriceDatabase.getPrice(itemId) < price * 2) {
          this.mallBuyable = 1;
          this.buyableFlag = true;
        }
      }
    } else if (!KoLCharacter.isHardcore()
        && (!KoLCharacter.inLegacyOfLoathing() || InventoryManager.pullableInLoL(itemId))
        && (!KoLCharacter.inSeaPath() || InventoryManager.pullableInSeaPath(itemId))) {
      // consider pulling
      this.pullable = this.getCount(KoLConstants.storage);

      this.pullBuyable = 0;
      if (InventoryManager.canUseMallToStorage(itemId)) {
        // consider Mall buying, but only if none are otherwise available
        if (this.getCount() == 0) {
          // We include things with historical price up to twice as high as limit, as current price
          // may be lower
          long price = Math.min(maxPrice, KoLCharacter.getStorageMeat());
          if (priceLevel == PriceLevel.DONT_CHECK
              || MallPriceDatabase.getPrice(itemId) < price * 2) {
            this.pullBuyable = 1;
            this.buyableFlag = true;
          }
        }
      }

      this.pullfoldable = 0;
      if (itemId > 0 && Preferences.getBoolean("maximizerFoldables")) {
        FoldGroup group = ItemDatabase.getFoldGroup(itemName);
        if (group != null) {
          for (int i = 0; i < group.names.size(); ++i) {
            String form = group.names.get(i);
            if (!form.equals(itemName)) {
              int foldItemId = ItemDatabase.getItemId(form);
              AdventureResult foldItem = ItemPool.get(foldItemId);
              int count = foldItem.getCount(KoLConstants.storage);
              this.pullfoldable += count;
              if (count > 0) {
                this.foldItemId = foldItemId;
              }
            }
          }
          // Cannot have more than one item from January's Garbage Tote, no matter how many you
          // have
          if (group.names.get(0).equals("january's garbage tote")) {
            if (this.pullfoldable > 1) {
              this.pullfoldable = 1;
            }
          }
        }
      }
    }

    // We never want to suggest turning Mr. Accessories into other items
    if (c.getIngredients().length > 0
        && ((MrStoreRequest.MR_A).equals(c.getIngredients()[0])
            || (MrStoreRequest.UNCLE_B).equals(c.getIngredients()[0]))) {
      this.creatable = 0;
    }
  }

  @Override
  public final int getCount() {
    if (this.getItemId() == 0) {
      // We have all the no items you'd ever want!
      return Integer.MAX_VALUE;
    }
    if (this.singleFlag) {
      return Math.min(
          1,
          this.initial
              + this.creatable
              + this.npcBuyable
              + this.mallBuyable
              + this.foldable
              + this.pullable
              + this.pullfoldable
              + this.pullBuyable);
    }

    return this.initial
        + this.creatable
        + this.npcBuyable
        + this.mallBuyable
        + this.foldable
        + this.pullable
        + this.pullfoldable
        + this.pullBuyable;
  }

  public void validate(long maxPrice, PriceLevel priceLevel) throws MaximizerInterruptedException {
    if (!KoLmafia.permitsContinue()) {
      throw new MaximizerInterruptedException();
    }

    if (priceLevel == PriceLevel.DONT_CHECK) {
      return;
    }

    if (!this.buyableFlag) {
      return;
    }

    // Check mall price
    long price = MallPriceManager.getMallPrice(this.getItemId());

    // Check if too expensive for max price settings
    if (price <= 0 || price > maxPrice) {
      this.mallBuyable = 0;
      this.pullBuyable = 0;
    }

    // Check character has meat to buy with
    if (price > KoLCharacter.getAvailableMeat()) {
      this.mallBuyable = 0;
    }

    // Check character has storage meat to buy for pulling
    if (price > KoLCharacter.getStorageMeat()) {
      this.pullBuyable = 0;
    }
  }

  private static int limitBuyable(final int itemId) {
    var possibleQuantity = NPCStoreDatabase.getQuantity(itemId);
    return possibleQuantity.orElse(Integer.MAX_VALUE);
  }

  public static final int TOTAL_MASK = 0xFF;
  public static final int SUBTOTAL_MASK = 0x0F;
  public static final int INITIAL_SHIFT = 8;
  public static final int CREATABLE_SHIFT = 12;
  public static final int NPCBUYABLE_SHIFT = 16;
  public static final int FOLDABLE_SHIFT = 20;
  public static final int PULLABLE_SHIFT = 24;
  public static final int BUYABLE_FLAG = 1 << 28;
  public static final int AUTOMATIC_FLAG = 1 << 29;
  public static final int CONDITIONAL_FLAG = 1 << 30;

  public int inventory;
  public int initial;
  public int creatable;
  public int npcBuyable;
  public int mallBuyable;
  public int foldable;
  public int pullable;
  public int pullfoldable;
  public int pullBuyable;
  public int foldItemId;

  public boolean buyableFlag;
  public boolean automaticFlag;
  public boolean requiredFlag;
  public boolean conditionalFlag;
  public boolean singleFlag;
}
