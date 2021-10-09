package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class CoinMasterPurchaseRequest extends PurchaseRequest {
  private final CoinmasterData data;
  private final AdventureResult cost;
  private final CoinMasterRequest request;

  /**
   * Constructs a new <code>CoinMasterPurchaseRequest</code> which retrieves things from Coin
   * Masters.
   */
  public CoinMasterPurchaseRequest(
      final CoinmasterData data, final AdventureResult item, final AdventureResult price) {
    super(""); // We do not run this request itself

    this.shopName = data.getMaster();
    this.isMallStore = false;
    this.item = item.getInstance(1);
    this.price = price.getCount();
    this.quantity = item.getCount();

    this.limit = this.quantity;
    this.canPurchase = true;

    this.timestamp = 0L;

    this.data = data;
    this.cost = price;
    this.request = data.getRequest(true, new AdventureResult[] {this.item});
  }

  public CoinmasterData getData() {
    return this.data;
  }

  @Override
  public String getPriceString() {
    int price =
        this.cost.isMeat() ? NPCPurchaseRequest.currentDiscountedPrice(this.price) : this.price;

    return KoLConstants.COMMA_FORMAT.format(price) + " " + this.cost.getPluralName(price);
  }

  @Override
  public AdventureResult getCost() {
    return this.cost;
  }

  @Override
  public String getCurrency(final long count) {
    return this.cost.getPluralName(this.price);
  }

  public int getTokenItemId() {
    return this.cost.getItemId();
  }

  @Override
  public int affordableCount() {
    int tokens = this.data.affordableTokens(this.cost);
    int price = this.price;
    return price == 0 ? 0 : tokens / price;
  }

  public boolean availableItem() {
    return this.data.isAccessible()
        && this.data.availableItem(this.item.getItemId())
        && this.data.canBuyItem(this.item.getItemId());
  }

  @Override
  public boolean canPurchase() {
    return this.canPurchase && this.availableItem() && this.affordableCount() > 0;
  }

  @Override
  public void setCanPurchase(final boolean canPurchase) {
    super.setCanPurchase(canPurchase && this.availableItem() && this.affordableCount() > 0);
  }

  @Override
  public void setCanPurchase() {
    super.setCanPurchase(this.availableItem() & this.affordableCount() > 0);
  }

  @Override
  public String color() {
    return this.canPurchase && this.affordableCount() > 0 ? null : "gray";
  }

  @Override
  public boolean isAccessible() {
    return this.data.isAccessible();
  }

  @Override
  public void run() {
    if (this.request == null) {
      return;
    }

    if (this.limit < 1) {
      return;
    }

    // Make sure we have enough tokens to buy what we want.
    if (this.data.availableTokens(this.cost) < this.limit * this.price) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't afford that.");
      return;
    }

    // Make sure the Coin Master is accessible
    String message = this.data.accessible();
    if (message != null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, message);
      return;
    }

    // Now that we're ready, make the purchase!

    KoLmafia.updateDisplay(
        "Purchasing "
            + this.item.getName()
            + " ("
            + KoLConstants.COMMA_FORMAT.format(this.limit)
            + " @ "
            + this.getPriceString()
            + ")...");

    this.initialCount = this.item.getCount(KoLConstants.inventory);
    this.request.setQuantity(this.limit);

    RequestThread.postRequest(this.request);
  }
}
