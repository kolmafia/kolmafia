package net.sourceforge.kolmafia.request;

import java.util.Comparator;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;

public abstract class PurchaseRequest extends GenericRequest
    implements Comparable<PurchaseRequest> {
  public static final int MAX_QUANTITY = 16777215;

  public static final Comparator<PurchaseRequest> nameComparator = new NameComparator();
  public static final Comparator<PurchaseRequest> priceComparator = new PriceComparator();

  protected String hashField;

  protected String shopName;

  protected AdventureResult item;
  protected int quantity;
  protected long price;
  protected int limit;

  protected boolean canPurchase;
  protected long timestamp;

  protected int initialCount; // for detecting partial yields

  public PurchaseRequest(final String location) {
    super(location);
  }

  public boolean isMallStore() {
    return false;
  }

  @Override
  public String getHashField() {
    return this.hashField;
  }

  public AdventureResult getItem() {
    return this.item;
  }

  public int getItemId() {
    return this.item.getItemId();
  }

  public String getItemName() {
    return this.item.getName();
  }

  public String getShopName() {
    return this.shopName;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  /**
   * Retrieves the price of the item being purchased.
   *
   * @return The price of the item being purchased
   */
  public long getPrice() {
    return this.price;
  }

  public String getPriceString() {
    return KoLConstants.COMMA_FORMAT.format(this.getPrice());
  }

  public AdventureResult getCost() {
    return new AdventureLongCountResult(AdventureResult.MEAT, this.getPrice());
  }

  public String getCurrency(final long count) {
    return "Meat";
  }

  /**
   * Retrieves the quantity of the item available in the store.
   *
   * @return The quantity of the item in the store
   */
  public int getQuantity() {
    return this.quantity;
  }

  /**
   * Sets the quantity of the item available in the store.
   *
   * @param quantity The quantity of the item available in the store.
   */
  public void setQuantity(final int quantity) {
    this.quantity = quantity;
  }

  /**
   * Retrieves the quantity of the item being purchased.
   *
   * @return The quantity of the item being purchased
   */
  public int getLimit() {
    return this.limit;
  }

  /**
   * Sets the maximum number of items that can be purchased through this request.
   *
   * @param limit The maximum number of items to be purchased with this request
   */
  public void setLimit(final int limit) {
    this.limit = Math.min(this.quantity, limit);
  }

  /** Converts this request into a readable string. */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    buffer.append(this.item.getName());
    buffer.append(" (");

    if (this.quantity == PurchaseRequest.MAX_QUANTITY) {
      buffer.append("unlimited");
    } else if (this.quantity < 0) {
      buffer.append("unknown");
    } else {
      buffer.append(KoLConstants.COMMA_FORMAT.format(this.quantity));

      if (this.limit < this.quantity) {
        buffer.append(" limit ");
        buffer.append(KoLConstants.COMMA_FORMAT.format(this.limit));
      }
    }

    buffer.append(" @ ");
    buffer.append(this.getPriceString());
    buffer.append("): ");
    buffer.append(this.shopName);

    return buffer.toString();
  }

  public long getAvailableMeat() {
    return KoLCharacter.getAvailableMeat();
  }

  public void setCanPurchase(final boolean canPurchase) {
    this.canPurchase = canPurchase;
  }

  public void setCanPurchase() {
    this.setCanPurchase(this.getAvailableMeat() >= this.getPrice());
  }

  public boolean canPurchase() {
    return this.canPurchase && this.getAvailableMeat() >= this.getPrice();
  }

  public String color() {
    return this.canPurchase && this.getAvailableMeat() >= this.getPrice() ? null : "gray";
  }

  public boolean canPurchaseIgnoringMeat() {
    return this.canPurchase;
  }

  public int affordableCount() {
    // Eliminate silly purchases
    return (int) Math.min(Integer.MAX_VALUE, this.getAvailableMeat() / this.getPrice());
  }

  public boolean isAccessible() {
    return true;
  }

  public String accessible() {
    return null;
  }

  /**
   * Executes the purchase request. This calculates the number of items which will be purchased and
   * adds it to the list. Note that it marks whether or not it's already been run to avoid problems
   * with repeating the request.
   */
  @Override
  public void run() {
    if (this.limit < 1 || !this.canPurchase()) {
      return;
    }

    // If the item is not currently recognized, notify user. This
    // should never happen, since we automatically detect and
    // register new items in mall stores and in NPC stores.

    if (this.item.getItemId() == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Item not present in KoLmafia database.");
      return;
    }

    // Make sure we have enough Meat to buy what we want.

    if (this.getAvailableMeat() < (long) this.limit * this.getPrice()) {
      return;
    }

    // Make sure we are wearing the appropriate outfit, if necessary

    try (Checkpoint checkpoint = new Checkpoint()) {
      if (this.ensureProperAttire()) {
        this.makePurchase();
      }
    }
  }

  private void makePurchase() {
    String shopName = getFormField("whichstore");

    // If shop exists, and it's not an empty string
    if (shopName != null && !shopName.isEmpty()) {
      shopName = " from #" + shopName;
    } else {
      // Set to empty string just incase it is null
      shopName = "";
    }

    // Now that we're ready, make the purchase!

    KoLmafia.updateDisplay(
        "Purchasing "
            + this.item.getName()
            + " ("
            + KoLConstants.COMMA_FORMAT.format(this.limit)
            + " @ "
            + this.getPriceString()
            + (this.limit > 1
                ? " = " + KoLConstants.COMMA_FORMAT.format((long) this.limit * getPrice())
                : "")
            + ")"
            + shopName
            + "...");

    this.initialCount = this.getCurrentCount();

    super.run();
  }

  public int getCurrentCount() {
    return this.item.getCount(KoLConstants.inventory);
  }

  public static class PriceComparator implements Comparator<PurchaseRequest> {
    @Override
    public int compare(PurchaseRequest o1, PurchaseRequest o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      // Order first by price, low to high.
      long thisPrice = o1.getPrice();
      long thatPrice = o2.getPrice();
      if (thisPrice != thatPrice) {
        return Long.signum(thisPrice - thatPrice);
      }

      // limit is how many items you can actually buy
      // Order next by limit, high to low
      if (o1.limit != o2.limit) {
        return o2.limit - o1.limit;
      }

      // If limits are equal but quantity is not, one or the other
      // stores has an artificial limit. Reward those that don't do
      // that by sorting low to high on quantity.
      if (o1.quantity != o2.quantity) {
        return o1.quantity - o2.quantity;
      }

      // Return 0 as they are equal
      return 0;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof PriceComparator;
    }
  }

  public static class NameComparator extends PriceComparator
      implements Comparator<PurchaseRequest> {
    @Override
    public int compare(PurchaseRequest o1, PurchaseRequest o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      // Order first by item name
      int result = o1.item.getName().compareToIgnoreCase(o2.item.getName());

      // Names being equal, order using price comparator
      return (result != 0) ? result : super.compare(o1, o2);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof NameComparator;
    }
  }

  @Override
  public int compareTo(final PurchaseRequest pr) {
    if (pr == null) {
      return -1;
    }

    return PurchaseRequest.nameComparator.compare(this, pr);
  }

  public boolean ensureProperAttire() {
    return true;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof PurchaseRequest
        && this.shopName.equals(((PurchaseRequest) o).shopName)
        && this.item.getItemId() == ((PurchaseRequest) o).item.getItemId();
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += this.shopName != null ? this.shopName.hashCode() : 0;
    hash += this.item != null ? this.item.hashCode() : 0;
    return hash;
  }

  public static boolean registerRequest(final String urlString) {
    if (urlString.startsWith("mallstore.php")) {
      return MallPurchaseRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("town_giftshop.php")) {
      return NPCPurchaseRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("shop.php")) {
      return NPCPurchaseRequest.registerShopRequest(urlString, false);
    }

    return false;
  }
}
