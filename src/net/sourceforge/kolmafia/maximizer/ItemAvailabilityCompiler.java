package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

final class ItemAvailabilityCompiler {
  private final int itemId;
  private final EquipScope equipScope;
  private final long maxPrice;
  private final PriceLevel priceLevel;
  private final boolean ignoreStandardRestriction;
  private final int inventory;
  private int initial;
  private int creatable;
  private int npcBuyable;
  private int mallBuyable;
  private int foldable;
  private int pullable;
  private int pullFoldable;
  private int storageBuyable;
  private int foldItemId;

  private ItemAvailabilityCompiler(
      int itemId,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      boolean ignoreStandardRestriction) {
    this.itemId = itemId;
    this.equipScope = equipScope;
    this.maxPrice = maxPrice;
    this.priceLevel = priceLevel;
    this.ignoreStandardRestriction = ignoreStandardRestriction;
    this.inventory = InventoryManager.getCount(itemId);
    this.initial = InventoryManager.getAccessibleCount(itemId, true, ignoreStandardRestriction);
  }

  static ItemAvailability compile(
      int itemId,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      boolean ignoreStandardRestriction) {
    return new ItemAvailabilityCompiler(
            itemId, equipScope, maxPrice, priceLevel, ignoreStandardRestriction)
        .compile();
  }

  private ItemAvailability compile() {
    boolean codpieceGem = ItemSlotGroup.ETERNITY_CODPIECE.accepts(this.itemId);
    int maxUseful = this.maxUseful(codpieceGem);
    String itemName = ItemDatabase.getItemName(this.itemId);
    FoldGroup foldGroup =
        this.itemId > 0 && Preferences.getBoolean("maximizerFoldables")
            ? ItemDatabase.getFoldGroup(itemName)
            : null;
    this.compileAccessibleFolds(foldGroup, itemName);

    boolean skillCreateCheck =
        Preferences.getBoolean("maximizerCreateOnHand")
            && this.equipScope == EquipScope.SPECULATE_INVENTORY
            && !ItemDatabase.isEquipment(this.itemId);
    if (this.initial >= maxUseful || (this.equipScope.checkInventoryOnly() && !skillCreateCheck)) {
      return this.result();
    }

    Concoction concoction = ConcoctionPool.get(this.itemId);
    if (concoction == null) {
      return this.result();
    }

    this.compileCreation(concoction);
    if (this.total() >= maxUseful || this.equipScope != EquipScope.SPECULATE_ANY) {
      return this.result();
    }

    this.compileRemoteAcquisition(codpieceGem, maxUseful, foldGroup, itemName);
    this.protectMrStoreCurrency(concoction);
    return this.result();
  }

  private int maxUseful(boolean codpieceGem) {
    if (!codpieceGem) {
      return 3;
    }

    var modifiers = ModifierDatabase.getItemModifiers(this.itemId);
    int equipmentLimit =
        !ItemDatabase.isEquipment(this.itemId)
            ? 0
            : modifiers != null && modifiers.getBoolean(BooleanModifier.SINGLE) ? 1 : 3;
    return ItemSlotGroup.ETERNITY_CODPIECE.slots().size() + equipmentLimit;
  }

  private void compileAccessibleFolds(FoldGroup foldGroup, String itemName) {
    if (foldGroup == null) {
      return;
    }

    var folds = getFoldAvailability(foldGroup, itemName, false);
    this.foldable = folds.count();
    this.foldItemId = folds.sourceItemId();

    if (!foldGroup.names.getFirst().equals("january's garbage tote")) {
      return;
    }

    this.foldable = Math.min(this.foldable, 1 - this.initial);
    if (this.foldable > 0
        && InventoryManager.getAccessibleCount(InventoryManager.getGarbageTote()) == 0) {
      this.foldable = 0;
    }
  }

  private void compileCreation(Concoction concoction) {
    this.creatable = concoction.creatable;
    if (concoction.getAdventuresNeeded(1) > 0 && Preferences.getBoolean("maximizerNoAdventures")) {
      this.creatable = 0;
      return;
    }

    if (concoction.price <= 0) {
      return;
    }

    long theoreticallyBuyable = this.maxPrice / concoction.price;
    int limit = NPCStoreDatabase.getQuantity(this.itemId).orElse(Integer.MAX_VALUE);
    this.npcBuyable = (int) Math.min(theoreticallyBuyable, limit);
  }

  private void compileRemoteAcquisition(
      boolean codpieceGem, int maxUseful, FoldGroup foldGroup, String itemName) {
    if (!this.ignoreStandardRestriction && !ItemDatabase.isAllowed(this.itemId)) {
      this.initial = 0;
      this.creatable = 0;
      this.npcBuyable = 0;
      return;
    }

    if (InventoryManager.canUseMall(this.itemId)) {
      int needed = this.neededPurchaseQuantity(codpieceGem, maxUseful);
      if (needed > 0 && this.historicalPriceMayBeAffordable(KoLCharacter.getAvailableMeat())) {
        this.mallBuyable = needed;
      }
      return;
    }

    if (KoLCharacter.isHardcore() || !InventoryManager.pullableInCurrentPath(this.itemId)) {
      return;
    }

    this.pullable = ItemPool.get(this.itemId).getCount(KoLConstants.storage);
    if (InventoryManager.canUseMallToStorage(this.itemId)) {
      int needed = this.neededPurchaseQuantity(codpieceGem, maxUseful);
      if (needed > 0 && this.historicalPriceMayBeAffordable(KoLCharacter.getStorageMeat())) {
        this.storageBuyable = needed;
      }
    }

    if (foldGroup == null) {
      return;
    }

    var folds = getFoldAvailability(foldGroup, itemName, true);
    this.pullFoldable = folds.count();
    if (folds.sourceItemId() > 0) {
      this.foldItemId = folds.sourceItemId();
    }
    if (foldGroup.names.getFirst().equals("january's garbage tote")) {
      this.pullFoldable = Math.min(this.pullFoldable, 1);
    }
  }

  private int neededPurchaseQuantity(boolean codpieceGem, int maxUseful) {
    return codpieceGem ? maxUseful - this.total() : this.total() == 0 ? 1 : 0;
  }

  private boolean historicalPriceMayBeAffordable(long availableMeat) {
    return this.priceLevel == PriceLevel.DONT_CHECK
        || MallPriceDatabase.getPrice(this.itemId) < Math.min(this.maxPrice, availableMeat) * 2;
  }

  private void protectMrStoreCurrency(Concoction concoction) {
    var ingredients = concoction.getIngredients();
    if (ingredients.length == 0) {
      return;
    }

    int ingredientId = ingredients[0].getItemId();
    if (ingredientId == ItemPool.MR_ACCESSORY || ingredientId == ItemPool.UNCLE_BUCK) {
      this.creatable = 0;
    }
  }

  private int total() {
    return this.initial
        + this.creatable
        + this.npcBuyable
        + this.mallBuyable
        + this.foldable
        + this.pullable
        + this.pullFoldable
        + this.storageBuyable;
  }

  private ItemAvailability result() {
    return new ItemAvailability(
        this.inventory,
        this.initial,
        this.creatable,
        this.npcBuyable,
        this.mallBuyable,
        this.foldable,
        this.pullable,
        this.pullFoldable,
        this.storageBuyable,
        this.foldItemId);
  }

  private record FoldAvailability(int count, int sourceItemId) {}

  private static FoldAvailability getFoldAvailability(
      FoldGroup group, String itemName, boolean storageOnly) {
    int available = 0;
    int availableItemId = 0;
    for (String form : group.names) {
      if (form.equals(itemName)) {
        continue;
      }

      int foldItemId = ItemDatabase.getItemId(form);
      int count =
          storageOnly
              ? ItemPool.get(foldItemId).getCount(KoLConstants.storage)
              : InventoryManager.getAccessibleCount(foldItemId);
      available += count;
      if (count > 0) {
        availableItemId = foldItemId;
      }
    }
    return new FoldAvailability(available, availableItemId);
  }
}
