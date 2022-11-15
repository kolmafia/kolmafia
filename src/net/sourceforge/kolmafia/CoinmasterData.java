package net.sourceforge.kolmafia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;

public class CoinmasterData implements Comparable<CoinmasterData> {

  public static final AdventureResult MEAT =
      new AdventureLongCountResult(AdventureResult.MEAT, 1) {
        @Override
        public String toString() {
          return this.getCount() + " Meat";
        }

        @Override
        public String getPluralName(int price) {
          return "Meat";
        }
      };

  // Mandatory fields
  private final String master;
  private final String nickname;
  private final Class<? extends CoinMasterRequest> requestClass;

  // Optional fields
  // The token(s) that you exchange for items.
  private String token = null;
  private String tokenTest = null;
  private boolean positiveTest = false;
  private Pattern tokenPattern = null;
  private AdventureResult item = null;
  private String property = null;

  // For Coinmasters that deal with "rows", a map from item id to row number
  private Map<Integer, Integer> itemRows = null;

  // The base URL used to buy things from this Coinmaster
  private String buyURL = null;
  private String buyAction = null;
  private List<AdventureResult> buyItems = null;
  private Map<Integer, Integer> buyPrices = null;

  // The base URL used to sell things to this Coinmaster
  private String sellURL = null;
  private String sellAction = null;
  private List<AdventureResult> sellItems = null;
  private Map<Integer, Integer> sellPrices = null;

  // Fields assumed to be common to buying & selling
  private String itemField = null;
  private Pattern itemPattern = null;
  private String countField = null;
  private Pattern countPattern = null;
  private String storageAction = null;
  private String tradeAllAction = null;

  private boolean canPurchase = true;

  // Derived fields
  public String pluralToken = null;
  private AdventureResult tokenItem = null;
  private Set<AdventureResult> currencies = null;

  // Base constructor for CoinmasterData with only the mandatory fields.
  // Optional fields can be added fluidly.
  // Derived fields are built lazily

  public CoinmasterData(
      String master, final String nickname, final Class<? extends CoinMasterRequest> requestClass) {
    this.master = master;
    this.nickname = nickname;
    this.requestClass = requestClass;
  }

  // Fluid field construction

  public CoinmasterData withToken(String token) {
    this.token = token;
    return this;
  }

  public CoinmasterData withPluralToken(String pluralToken) {
    this.pluralToken = pluralToken;
    return this;
  }

  public CoinmasterData withTokenTest(String tokenTest) {
    this.tokenTest = tokenTest;
    return this;
  }

  public CoinmasterData withPositiveTest(boolean positiveTest) {
    this.positiveTest = positiveTest;
    return this;
  }

  public CoinmasterData withTokenPattern(Pattern tokenPattern) {
    this.tokenPattern = tokenPattern;
    return this;
  }

  public CoinmasterData withItem(AdventureResult item) {
    this.item = item;
    return this;
  }

  public CoinmasterData withProperty(String property) {
    this.property = property;
    return this;
  }

  public CoinmasterData withItemRows(String master) {
    return withItemRows(CoinmastersDatabase.getRows(master));
  }

  public CoinmasterData withItemRows(Map<Integer, Integer> itemRows) {
    this.itemRows = itemRows;
    return this;
  }

  public CoinmasterData withBuyURL(String buyURL) {
    this.buyURL = buyURL;
    return this;
  }

  public CoinmasterData withBuyAction(String buyAction) {
    this.buyAction = buyAction;
    return this;
  }

  public CoinmasterData withBuyItems(String master) {
    return withBuyItems(CoinmastersDatabase.getBuyItems(master));
  }

  public CoinmasterData withBuyItems(List<AdventureResult> buyItems) {
    this.buyItems = buyItems;
    return this;
  }

  public CoinmasterData withBuyPrices() {
    return withBuyPrices(CoinmastersDatabase.getNewMap());
  }

  public CoinmasterData withBuyPrices(String master) {
    return withBuyPrices(CoinmastersDatabase.getBuyPrices(master));
  }

  public CoinmasterData withBuyPrices(Map<Integer, Integer> buyPrices) {
    this.buyPrices = buyPrices;
    return this;
  }

  public CoinmasterData withSellURL(String sellURL) {
    this.sellURL = sellURL;
    return this;
  }

  public CoinmasterData withSellAction(String sellAction) {
    this.sellAction = sellAction;
    return this;
  }

  public CoinmasterData withSellItems(List<AdventureResult> sellItems) {
    this.sellItems = sellItems;
    return this;
  }

  public CoinmasterData withSellPrices(Map<Integer, Integer> sellPrices) {
    this.sellPrices = sellPrices;
    return this;
  }

  public CoinmasterData withItemField(String itemField) {
    this.itemField = itemField;
    return this;
  }

  public CoinmasterData withItemPattern(Pattern itemPattern) {
    this.itemPattern = itemPattern;
    return this;
  }

  public CoinmasterData withCountField(String countField) {
    this.countField = countField;
    return this;
  }

  public CoinmasterData withCountPattern(Pattern countPattern) {
    this.countPattern = countPattern;
    return this;
  }

  public CoinmasterData withStorageAction(String storageAction) {
    this.storageAction = storageAction;
    return this;
  }

  public CoinmasterData withTradeAllAction(String tradeAllAction) {
    this.tradeAllAction = tradeAllAction;
    return this;
  }

  public CoinmasterData withCanPurchase(boolean canPurchase) {
    this.canPurchase = canPurchase;
    return this;
  }

  // Convenience method for shop.php Coinmasters that use "rows"
  public CoinmasterData withRowShopFields(String master, String shopId) {
    return this.withItemRows(master)
        .withBuyURL("shop.php?whichshop=" + shopId)
        .withBuyAction("buyitem")
        .withBuyItems(master)
        .withBuyPrices(master)
        .withItemField("whichrow")
        .withItemPattern(GenericRequest.WHICHROW_PATTERN)
        .withCountField("quantity")
        .withCountPattern(GenericRequest.QUANTITY_PATTERN);
  }

  // Convenience method for Coinmasters that use "whichitem"/"quantity"
  public CoinmasterData withWhichItemFields(String master) {
    return this.withBuyItems(master)
        .withBuyPrices(master)
        .withItemField("whichitem")
        .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
        .withCountField("quantity")
        .withCountPattern(GenericRequest.QUANTITY_PATTERN);
  }

  // Ye Olde BOA Constructor for CoinmasterData
  // To be removed, once all instances are built fluidly

  public CoinmasterData(
      final String master,
      final String nickname,
      final Class<? extends CoinMasterRequest> requestClass,
      final String token,
      final String tokenTest,
      final boolean positiveTest,
      final Pattern tokenPattern,
      final AdventureResult item,
      final String property,
      final Map<Integer, Integer> itemRows,
      final String buyURL,
      final String buyAction,
      final List<AdventureResult> buyItems,
      final Map<Integer, Integer> buyPrices,
      final String sellURL,
      final String sellAction,
      final List<AdventureResult> sellItems,
      final Map<Integer, Integer> sellPrices,
      final String itemField,
      final Pattern itemPattern,
      final String countField,
      final Pattern countPattern,
      final String storageAction,
      final String tradeAllAction,
      final boolean canPurchase) {
    this(master, nickname, requestClass);
    this.token = token;
    this.tokenTest = tokenTest;
    this.positiveTest = positiveTest;
    this.tokenPattern = tokenPattern;
    this.item = item;
    this.property = property;
    this.itemRows = itemRows;
    this.buyURL = buyURL;
    this.buyAction = buyAction;
    this.buyItems = buyItems;
    this.buyPrices = buyPrices;
    this.sellURL = sellURL;
    this.sellAction = sellAction;
    this.sellItems = sellItems;
    this.sellPrices = sellPrices;
    this.itemField = itemField;
    this.itemPattern = itemPattern;
    this.countField = countField;
    this.countPattern = countPattern;
    this.storageAction = storageAction;
    this.tradeAllAction = tradeAllAction;
    this.canPurchase = canPurchase;
  }

  // Getters for mandatory fields

  public final String getMaster() {
    return this.master;
  }

  public final String getNickname() {
    return this.nickname;
  }

  public final Class<? extends CoinMasterRequest> getRequestClass() {
    return this.requestClass;
  }

  public final String getToken() {
    return this.token;
  }

  public final void setToken(final String token) {
    this.token = token;
    this.pluralToken = null;
  }

  // Getters for optional fields

  public final String getTokenTest() {
    return this.tokenTest;
  }

  public final boolean getPositiveTest() {
    return this.positiveTest;
  }

  public final Pattern getTokenPattern() {
    return this.tokenPattern;
  }

  public final AdventureResult getItem() {
    return this.item;
  }

  public final void setItem(final AdventureResult item) {
    this.item = item;
  }

  public final String getProperty() {
    return this.property;
  }

  public Map<Integer, Integer> getRows() {
    return this.itemRows;
  }

  public final Integer getRow(int itemId) {
    if (this.itemRows == null) {
      return itemId;
    }
    Integer row = this.itemRows.get(itemId);
    return row;
  }

  // The base URL used to buy things from this Coinmaster
  public final String getBuyURL() {
    return this.buyURL;
  }

  public final String getBuyAction() {
    return this.buyAction;
  }

  public final List<AdventureResult> getBuyItems() {
    return this.buyItems;
  }

  public final Map<Integer, Integer> getBuyPrices() {
    return this.buyPrices;
  }

  // The base URL used to sell things to this Coinmaster
  public final String getSellURL() {
    return this.sellURL;
  }

  public final String getSellAction() {
    return this.sellAction;
  }

  public final List<AdventureResult> getSellItems() {
    return this.sellItems;
  }

  public final Map<Integer, Integer> getSellPrices() {
    return this.sellPrices;
  }

  // Fields assumed to be common to buying & selling

  public final String getItemField() {
    return this.itemField;
  }

  public final Pattern getItemPattern() {
    return this.itemPattern;
  }

  public final Matcher getItemMatcher(final String string) {
    return this.itemPattern == null ? null : this.itemPattern.matcher(string);
  }

  public final String getCountField() {
    return this.countField;
  }

  public final Pattern getCountPattern() {
    return this.countPattern;
  }

  public final Matcher getCountMatcher(final String string) {
    return this.countPattern == null ? null : this.countPattern.matcher(string);
  }

  public final String getStorageAction() {
    return this.storageAction;
  }

  public final String getTradeAllAction() {
    return this.tradeAllAction;
  }

  public final boolean getCanPurchase() {
    return this.canPurchase;
  }

  // Getters for derived fields

  public final String getPluralToken() {
    if (this.pluralToken == null) {
      this.pluralToken =
          this.item != null ? ItemDatabase.getPluralName(this.token) : this.token + "s";
    }
    return this.pluralToken;
  }

  private AdventureResult getTokenItem() {
    if (this.tokenItem == null) {
      this.tokenItem =
          new AdventureResult(this.token, -1, 1, false) {
            @Override
            public String getPluralName(final int count) {
              return count == 1
                  ? CoinmasterData.this.getToken()
                  : CoinmasterData.this.getPluralToken();
            }
          };
    }
    return this.tokenItem;
  }

  public final int availableTokens() {
    AdventureResult item = this.item;
    if (item != null) {
      return item.getItemId() == ItemPool.WORTHLESS_ITEM
          ? HermitRequest.getWorthlessItemCount()
          : item.getCount(KoLConstants.inventory);
    }
    String property = this.property;
    if (property != null) {
      return Preferences.getInteger(property);
    }
    return 0;
  }

  public final int availableTokens(final AdventureResult currency) {
    if (currency.isMeat()) {
      return Concoction.getAvailableMeat();
    }

    int itemId = currency.getItemId();

    if (itemId != -1) {
      return itemId == ItemPool.WORTHLESS_ITEM
          ? HermitRequest.getWorthlessItemCount()
          : currency.getCount(KoLConstants.inventory);
    }

    if (this.property != null) {
      return Preferences.getInteger(this.property);
    }

    return 0;
  }

  public final int availableStorageTokens() {
    return this.storageAction != null ? this.item.getCount(KoLConstants.storage) : 0;
  }

  public final int availableStorageTokens(final AdventureResult currency) {
    return this.storageAction != null && currency.getItemId() != -1
        ? currency.getCount(KoLConstants.storage)
        : 0;
  }

  public final int affordableTokens(final AdventureResult currency) {
    if (currency.isMeat()) {
      return Concoction.getAvailableMeat();
    }

    return currency.getItemId() == ItemPool.WORTHLESS_ITEM
        ? HermitRequest.getAcquirableWorthlessItemCount()
        : this.availableTokens(currency);
  }

  public boolean availableItem(final int itemId) {
    if (this.buyItems == null) {
      return false;
    }
    AdventureResult item = ItemPool.get(itemId, 1);
    return (this.buyItems.contains(item));
  }

  public boolean canBuyItem(final int itemId) {
    if (this.buyItems == null) {
      return false;
    }

    AdventureResult item = ItemPool.get(itemId, 1);
    return item.getCount(this.buyItems) > 0;
  }

  public int getBuyPrice(final int itemId) {
    if (this.buyPrices == null) {
      return 0;
    }

    Integer price = this.buyPrices.get(itemId);
    return price != null ? price : 0;
  }

  public AdventureResult itemBuyPrice(final int itemId) {
    int price = this.getBuyPrice(itemId);
    return this.item == null
        ? this.getTokenItem().getInstance(price)
        : this.item.getInstance(price);
  }

  public Set<AdventureResult> currencies() {
    if (this.currencies == null) {
      this.currencies = new TreeSet<>();
      for (AdventureResult item : this.buyItems) {
        this.currencies.add(this.itemBuyPrice(item.getItemId()));
      }
    }
    return this.currencies;
  }

  public final boolean canSellItem(final int itemId) {
    if (this.sellPrices != null) {
      return this.sellPrices.containsKey(itemId);
    }
    return false;
  }

  public final int getSellPrice(final int itemId) {
    if (this.sellPrices != null) {
      Integer price = this.sellPrices.get(itemId);
      return price != null ? price : 0;
    }
    return 0;
  }

  public AdventureResult itemSellPrice(final int itemId) {
    int price = this.getSellPrice(itemId);
    return this.item == null
        ? AdventureResult.tallyItem(this.token, price, false)
        : this.item.getInstance(price);
  }

  public void registerPurchaseRequests() {
    // If this Coin Master doesn't sell anything that goes into
    // your inventory, nothing to register
    if (!this.canPurchase) {
      return;
    }

    // Clear existing purchase requests
    CoinmastersDatabase.clearPurchaseRequests(this);

    // For each item you can buy from this Coin Master, create a purchase request
    for (AdventureResult item : this.buyItems) {
      AdventureResult price = this.itemBuyPrice(item.getItemId());
      CoinmastersDatabase.registerPurchaseRequest(this, item, price);
    }
  }

  @Override
  public String toString() {
    return this.master;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof CoinmasterData && this.master == ((CoinmasterData) o).master;
  }

  @Override
  public int hashCode() {
    return this.master != null ? this.master.hashCode() : 0;
  }

  @Override
  public int compareTo(final CoinmasterData cd) {
    return this.master.compareToIgnoreCase(cd.master);
  }

  public CoinMasterRequest getRequest() {
    Class<? extends CoinMasterRequest> requestClass = this.getRequestClass();
    Class<?>[] parameters = new Class<?>[0];

    try {
      Constructor<? extends CoinMasterRequest> constructor =
          requestClass.getConstructor(parameters);
      Object[] initargs = new Object[0];
      return constructor.newInstance(initargs);
    } catch (Exception e) {
      return null;
    }
  }

  public CoinMasterRequest getRequest(final boolean buying, final AdventureResult[] items) {
    Class<? extends CoinMasterRequest> requestClass = this.getRequestClass();
    Class<?>[] parameters = new Class<?>[2];
    parameters[0] = boolean.class;
    parameters[1] = AdventureResult[].class;

    try {
      Constructor<? extends CoinMasterRequest> constructor =
          requestClass.getConstructor(parameters);
      Object[] initargs = new Object[2];
      initargs[0] = buying;
      initargs[1] = items;
      return constructor.newInstance(initargs);
    } catch (Exception e) {
      return null;
    }
  }

  public boolean isAccessible() {
    return this.accessible() == null;
  }

  public String accessible() {
    // Returns an error reason or null

    Class<? extends CoinMasterRequest> requestClass = this.getRequestClass();
    Class<?>[] parameters = new Class<?>[0];

    try {
      Method method = requestClass.getMethod("accessible", parameters);
      Object[] args = new Object[0];
      return (String) method.invoke(null, args);
    } catch (Exception e) {
      return null;
    }
  }

  public String canSell() {
    // Returns an error reason or null

    Class<? extends CoinMasterRequest> requestClass = this.getRequestClass();
    Class<?>[] parameters = new Class<?>[0];

    try {
      Method method = requestClass.getMethod("canSell", parameters);
      Object[] args = new Object[0];
      return (String) method.invoke(null, args);
    } catch (Exception e) {
      return null;
    }
  }

  public String canBuy() {
    // Returns an error reason or null

    Class<? extends CoinMasterRequest> requestClass = this.getRequestClass();
    Class<?>[] parameters = new Class<?>[0];

    try {
      Method method = requestClass.getMethod("canBuy", parameters);
      Object[] args = new Object[0];
      return (String) method.invoke(null, args);
    } catch (Exception e) {
      return null;
    }
  }

  public void purchaseItem(AdventureResult item, boolean storage) {}
}
