package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.MallPriceManager;

public class CheckedItem extends AdventureResult {
  private ItemAvailability availability;
  private final boolean buyable;

  public CheckedItem(int itemId, EquipScope equipScope, long maxPrice, PriceLevel priceLevel) {
    this(itemId, equipScope, maxPrice, priceLevel, false);
  }

  public CheckedItem(
      int itemId,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      boolean ignoreStandardRestriction) {
    super(itemId, 1, false);

    if (itemId == -1) {
      this.name = "(none)";
      this.availability = ItemAvailability.unlimited();
      this.buyable = false;
      return;
    }

    this.availability =
        ItemAvailabilityCompiler.compile(
            itemId, equipScope, maxPrice, priceLevel, ignoreStandardRestriction);
    this.buyable = this.availability.buyable();
  }

  @Override
  public final int getCount() {
    if (this.getItemId() == 0) {
      // We have all the no items you'd ever want!
      return Integer.MAX_VALUE;
    }
    return this.singleFlag ? Math.min(1, this.availability.total()) : this.availability.total();
  }

  final int getAvailableCount() {
    return this.availability.total();
  }

  AcquisitionMethod acquisitionMethod(int used) {
    return this.availability.acquisitionMethod(used);
  }

  ItemAvailability availability() {
    return this.availability;
  }

  List<AcquisitionOption> acquisitionOptions() {
    return this.availability.options();
  }

  boolean isBuyable() {
    return this.buyable;
  }

  public void validate(long maxPrice, PriceLevel priceLevel) throws MaximizerInterruptedException {
    if (!KoLmafia.permitsContinue()) {
      throw new MaximizerInterruptedException();
    }
    if (priceLevel == PriceLevel.DONT_CHECK || !this.buyable) {
      return;
    }

    long price = MallPriceManager.getMallPrice(this.getItemId());
    this.availability =
        this.availability.withValidatedMallPrice(
            price, maxPrice, KoLCharacter.getAvailableMeat(), KoLCharacter.getStorageMeat());
  }

  public boolean automaticFlag;
  public boolean requiredFlag;
  public boolean conditionalFlag;
  public boolean singleFlag;
}
