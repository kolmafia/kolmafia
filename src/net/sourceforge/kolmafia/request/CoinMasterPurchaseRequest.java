package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.ShopRow;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class CoinMasterPurchaseRequest extends PurchaseRequest {
  private final CoinmasterData data;
  private final ShopRow shopRow;
  private final AdventureResult cost;
  private final AdventureResult[] costs;
  private final CoinMasterRequest request;

  /**
   * Constructs a new <code>CoinMasterPurchaseRequest</code> which retrieves things from Coin
   * Masters.
   */
  public CoinMasterPurchaseRequest(
      final CoinmasterData data, final AdventureResult item, final AdventureResult price) {
    super(""); // We do not run this request itself

    this.shopName = data.getMaster();
    this.shopRow = null;

    this.item = item.getInstance(1);
    this.quantity = item.getCount();

    this.costs = new AdventureResult[] {price};
    this.cost = price;
    this.price = price.getCount();

    this.limit = this.quantity;
    this.canPurchase = true;

    this.timestamp = 0L;

    this.data = data;
    this.request = data.getRequest(true, new AdventureResult[] {this.item});
  }

  public CoinMasterPurchaseRequest(final CoinmasterData data, final ShopRow row) {
    super(""); // We do not run this request itself

    this.shopName = data.getMaster();
    this.shopRow = row;

    AdventureResult item = row.getItem();
    this.item = item.getInstance(1);
    this.quantity = CoinmastersDatabase.purchaseLimit(item.getItemId());

    // PurchaseRequest.orice is Meat. In this class, it is the quantity
    // of the "cost" - the traded token. In a ShopRow, there can be
    // multiple costs. Kludge: use the first cost.
    this.costs = row.getCosts();
    this.cost = costs[0];
    this.price = cost.getCount();

    this.limit = this.quantity;
    this.canPurchase = true;

    this.timestamp = 0L;

    this.data = data;

    this.request = data.getRequest(row, 1);
  }

  @Override
  public boolean isMallStore() {
    return false;
  }

  public CoinmasterData getData() {
    return this.data;
  }

  @Override
  public String getPriceString() {
    if (this.shopRow != null) {
      return this.shopRow.costString();
    }
    long price =
        this.cost.isMeat() ? NPCPurchaseRequest.currentDiscountedPrice(this.price) : this.price;

    return KoLConstants.COMMA_FORMAT.format(price) + " " + this.cost.getPluralName(this.price);
  }

  @Override
  public AdventureResult getCost() {
    return this.cost;
  }

  @Override
  public AdventureResult[] getCosts() {
    return this.costs;
  }

  @Override
  public String getCurrency(final long count) {
    if (this.shopRow != null) {
      return this.shopRow.costString();
    }
    return this.cost.getPluralName(this.price);
  }

  public int getTokenItemId() {
    return this.cost.getItemId();
  }

  @Override
  public int affordableCount() {
    if (this.shopRow != null) {
      return this.shopRow.getAffordableCount();
    }

    int tokens = this.data.affordableTokens(this.cost);
    long price = this.price;
    return price == 0 ? 0 : (int) (tokens / price);
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
    super.setCanPurchase(
        canPurchase && this.isAccessible() && this.availableItem() && this.affordableCount() > 0);
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
  public String accessible() {
    return this.data.accessible();
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
    if (this.affordableCount() < this.limit) {
      // if (this.data.availableTokens(this.cost) < this.limit * this.price) {
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
