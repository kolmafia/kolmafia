package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.StandardRewardDatabase;
import net.sourceforge.kolmafia.persistence.StandardRewardDatabase.StandardPulverized;
import net.sourceforge.kolmafia.persistence.StandardRewardDatabase.StandardReward;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.shop.ShopRowDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ArmoryAndLeggeryRequest extends CoinMasterShopRequest {
  public static final String master = "Armory & Leggery";
  public static final String SHOPID = "armory";

  public static final CoinmasterData ARMORY_AND_LEGGERY =
      new CoinmasterData(master, "armory", ArmoryAndLeggeryRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShopRows(ArmoryAndLeggeryRequest::visitShopRows);

  static {
    initializeCoinMasterInventory();
  }

  public static void initializeCoinMasterInventory() {
    CoinmasterData data = ARMORY_AND_LEGGERY;

    List<ShopRow> shopRows = new ArrayList<>();

    for (var entry : StandardRewardDatabase.allStandardRewards().entrySet()) {
      // The item we wish to buy
      int itemId = entry.getKey();
      var reward = entry.getValue();

      if (reward.row().equals("UNKNOWN")) {
        // We've not determined the row number for this reward yet
        continue;
      }

      // The pulverized item from the next year
      int currency = StandardRewardDatabase.findPulverization(reward.year() + 1, reward.type());
      if (currency == -1) {
        // You can't buy the current year's Standard rewards
        continue;
      }

      AdventureResult cost = ItemPool.get(currency, 1);
      int row = StringUtilities.parseInt(reward.row());
      AdventureResult item = ItemPool.get(itemId, PurchaseRequest.MAX_QUANTITY);

      ShopRow shopRow = new ShopRow(row, item.getInstance(1), cost);
      shopRows.add(shopRow);
      ShopRowDatabase.registerShopRow(shopRow, "armory");
    }

    data.setShopRows(shopRows);
  }

  // We can learn new rows from visiting this shop with new pulverized items.
  public static void visitShopRows(final List<ShopRow> shopRows) {
    CoinmasterData data = ARMORY_AND_LEGGERY;

    List<String> pulverizeLines = new ArrayList<>();
    List<String> rewardLines = new ArrayList<>();

    // ShopRequest.parseInventory will parse the rows and register new items.
    for (ShopRow shopRow : shopRows) {
      AdventureResult[] costs = shopRow.getCosts();

      if (costs == null || costs.length != 1) {
        continue;
      }

      AdventureResult currency = costs[0];
      // This shop is also an npcstore.
      if (currency.isMeat()) {
        continue;
      }

      // The currency is a pulverized standard reward.
      //
      // 11526	2025	norm	crepe paper pared cuttings
      // 11534	2025	hard	petrified wood waste parts

      // The item is a specific year's standard reward.
      //
      // 11520	2025	norm	SC	UNKNOWN	crepe paper phrygian cap
      // 11528	2025	hard	SC	UNKNOWN	petrified wood war pike

      // Our protocol when a new standard season starts:
      //
      // 1) Add the pulverized Normal and Hardcore rewards to standard-pulverized.txt
      // 2) Update the UNKNOWN rows from the previous year with that we learn here.
      // 3) Add 6 Normal and 6 Hardcore rewards to standard-rewards.txt
      //    with ROW = UNKNOWN
      //
      // If we do that, then parsing the inventory here will generate the new entries for
      // standard-pulverize.txt and also the ROW data for the previous year's items in
      // standard-rewards.txt. Adding the new UNKNOWN rows for the current year must be manual,
      // since we can't deduce whether an item is Hardcore or Normal, and which class earns it.

      AdventureResult item = shopRow.getItem();

      // We can learn new rows, but in order to register them, the current year's pulverized
      // standard reward must be known and registered. We can do that here, if the reward item is
      // known, step 3 of our protocol.

      var reward = StandardRewardDatabase.findStandardReward(item.getItemId());
      var pulverized = StandardRewardDatabase.findStandardPulverized(currency.getItemId());

      if (pulverized == null) {
        // id	year	type	name
        int itemId = currency.getItemId();
        int year = reward == null ? 0 : reward.year() + 1;
        boolean type = reward.type();
        String name = currency.getName();
        StandardPulverized toRegister = new StandardPulverized(itemId, year, type, name);
        StandardRewardDatabase.registerStandardPulverized(itemId, toRegister);

        String line = StandardRewardDatabase.toData(toRegister);
        pulverizeLines.add(line);
      }

      if (reward != null && reward.row().equals("UNKNOWN")) {
        // id	year	type	class	row	name
        int itemId = item.getItemId();
        int year = reward.year();
        boolean type = reward.type();
        var cl = reward.cl();
        int row = shopRow.getRow();
        String name = item.getName();

        StandardReward toRegister =
            new StandardReward(itemId, year, type, cl, String.valueOf(row), name);
        StandardRewardDatabase.registerStandardReward(itemId, toRegister);

        String line = StandardRewardDatabase.toData(toRegister);
        rewardLines.add(line);
      }
    }

    if (pulverizeLines.size() == 0 && rewardLines.size() == 0) {
      return;
    }

    String divider = "--------------------";

    if (pulverizeLines.size() > 0) {
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
      for (String line : pulverizeLines) {
        RequestLogger.printLine(line);
        RequestLogger.updateSessionLog(line);
      }
    }

    if (rewardLines.size() > 0) {
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
      for (String line : rewardLines) {
        RequestLogger.printLine(line);
        RequestLogger.updateSessionLog(line);
      }
    }

    RequestLogger.printLine(divider);
    RequestLogger.updateSessionLog(divider);
  }

  // *** Obsolete: used in "test standard-rewards", which should be
  // *** subsumed by our shop.php inventory parsing

  public static record CoinmasterReward(
      int itemId, String itemName, String currency, int price, int row) {}

  // <tr rel="7985"><td valign=center></td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/polyparachute.gif"
  // class=hand onClick='javascript:descitem(973760204)'></td><td valign=center><a
  // onClick='javascript:descitem(973760204)'><b>polyester
  // parachute</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/wickerbits.gif width=30
  // height=30 onClick='javascript:descitem(134381888)' alt="wickerbits"
  // title="wickerbits"></td><td><b>1</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=armory&action=buyitem&quantity=1&whichrow=804&pwd=' value='Buy'></td></tr>

  public static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)",
          Pattern.DOTALL);

  public static CoinmasterReward parseCoinmasterReward(Matcher matcher) {
    int itemId = StringUtilities.parseInt(matcher.group(1));
    String itemName = matcher.group(3).trim();
    String currency = matcher.group(4);
    int price = StringUtilities.parseInt(matcher.group(5));
    int row = StringUtilities.parseInt(matcher.group(6));

    // The currency must be an item
    if (currency.equals("Meat")) {
      return null;
    }

    return new CoinmasterReward(itemId, itemName, currency, price, row);
  }

  public static String toData(CoinmasterReward creward) {
    if (creward == null) {
      return null;
    }

    int currency = ItemDatabase.getItemId(creward.currency());
    if (currency == -1) {
      RequestLogger.printLine("currency '" + creward.currency() + "' is unknown.");
      return null;
    }

    StandardPulverized pulverized = StandardRewardDatabase.findStandardPulverized(currency);
    if (pulverized == null) {
      RequestLogger.printLine(
          "currency '" + creward.currency() + "' is not registered yet as a currency.");
      return null;
    }

    int itemId = creward.itemId();
    String itemName = creward.itemName();
    int year = pulverized.year() - 1;
    boolean type = pulverized.type();
    StandardReward current = StandardRewardDatabase.findStandardReward(itemId);
    AscensionClass cl = current == null ? null : current.cl();
    String row = "ROW" + creward.row();

    StandardReward sreward = new StandardReward(itemId, year, type, cl, row, itemName);
    return StandardRewardDatabase.toData(sreward);
  }
}
