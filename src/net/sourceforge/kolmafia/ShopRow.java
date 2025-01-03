package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult.MeatResult;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopRow implements Comparable<ShopRow> {

  private int row;
  private AdventureResult item;
  private AdventureResult[] costs;

  public ShopRow(int row, AdventureResult item, AdventureResult... costs) {
    this.row = row;
    this.item = item;
    this.costs = costs;
  }

  public int getRow() {
    return this.row;
  }

  public AdventureResult getItem() {
    return this.item;
  }

  public AdventureResult[] getCosts() {
    return this.costs;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof ShopRow data && this.row == data.row;
  }

  @Override
  public int hashCode() {
    return this.row;
  }

  @Override
  public int compareTo(final ShopRow ir) {
    return this.row < ir.row ? -1 : this.row == ir.row ? 0 : 1;
  }

  public int getAffordableCount() {
    // Look at all costs and decide the max you can buy given available balances

    int max = Integer.MAX_VALUE;

    for (AdventureResult cost : this.getCosts()) {
      int available =
          cost.isMeat() ? Concoction.getAvailableMeat() : cost.getCount(KoLConstants.inventory);
      int price = cost.getCount();
      if (cost.isMeat()) {
        price = NPCPurchaseRequest.currentDiscountedPrice(price);
      }
      max = Math.min(max, available / price);
    }

    return max;
  }

  public String costString() {
    return this.costString(1);
  }

  public String costString(long count) {
    StringBuilder buf = new StringBuilder();
    String separator = "";

    buf.append("(");
    for (AdventureResult cost : costs) {
      long price = cost.getCount() * count;
      if (cost.isMeat()) {
        price = NPCPurchaseRequest.currentDiscountedPrice(price);
      }
      buf.append(separator);
      separator = "+";

      buf.append(price);
      buf.append(" ");
      buf.append(cost.getPluralName(price));
    }
    buf.append(")");
    return buf.toString();
  }

  // <b style="color: white">Crimbo Factory</b>
  private static final Pattern SHOP_PATTERN = Pattern.compile("<table.*?<b.*?>(.*?)</b>");

  public static String parseShopName(final String html) {
    Matcher m = SHOP_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  // name=whichshop value="grandma"
  private static final Pattern SHOP_ID_PATTERN = Pattern.compile("name=whichshop value=\"(.*?)\"");

  public static String parseShopId(final String html) {
    Matcher m = SHOP_ID_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  /* The Armory and Leggery: Meat
    <tr rel="634">
      <td valign=center></td>
      <td>
        <img src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/whip.gif" class="hand pop" rel="desc_item.php?whichitem=353896186" onClick='javascript:descitem(353896186)'></td>
      <td valign=center><a onClick='javascript:descitem(353896186)'><b>cool whip</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td>
      <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/meat.gif width=30 height=30 alt="Meat" title="Meat"></td>
      <td><b>28</b>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=armory&action=buyitem&quantity=1&whichrow=487&pwd=daa2136bd0601e7e4671281b3354bd48' value='Buy'></td>
    </tr>
  */

  /* The Armory and Leggery: item cost
   <tr rel="11025">
     <td valign=center></td>
     <td>
       <img src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/cerarobe.gif" class="hand pop" rel="desc_item.php?whichitem=935849680" onClick='javascript:descitem(935849680)'></td>
     <td valign=center><a onClick='javascript:descitem(935849680)'><b>ceramic cenobite's robe</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td>
     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/adobebits.gif width=30 height=30 onClick='javascript:descitem(469007963)' alt="adobe assortment" title="adobe assortment"></td>
     <td><b>1</b>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=armory&action=buyitem&quantity=1&whichrow=1453&pwd=daa2136bd0601e7e4671281b3354bd48' value='Buy'></td>
   </tr>
  */

  /* Grandma
   <tr rel="4283">
     <td valign=center><input type=radio name=whichrow value=125></td>
     <td>
       <img src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/tailpiece.gif" class="hand pop" rel="desc_item.php?whichitem=219864306" onClick='javascript:descitem(219864306)'></td>
     <td valign=center><a onClick='javascript:descitem(219864306)'><b>crappy Mer-kin tailpiece</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td>
     <td>
       <img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/seachaps.gif width=30 height=30 onClick='javascript:descitem(424923166)' alt="sea chaps" title="sea chaps"></td>
     <td><b>1</b>&nbsp;&nbsp;</td>
     <td>
       <img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/teflonfins.gif width=30 height=30 onClick='javascript:descitem(667590304)' alt="teflon swim fins" title="teflon swim fins"></td>
     <td><b>1</b>&nbsp;&nbsp;</td>
     <td>
       <img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/scale3.gif width=30 height=30 onClick='javascript:descitem(922469052)' alt="pristine fish scale" title="pristine fish scale"></td>
     <td><b>3</b>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td></td>
     <td>&nbsp;&nbsp;</td>
     <td valign=center><input class="button doit multibuy disabled" disabled type=button rel='shop.php?whichshop=grandma&action=buyitem&quantity=1&whichrow=125&pwd=daa2136bd0601e7e4671281b3354bd48' value='Buy'></td>
   </tr>
  */

  /* Crimbo23 Factory
    <tr rel="11771">
      <td valign=center><input type=radio name=whichrow value=1538></td>
      <td><img src="/images/itemimages/chiatiki.gif" class="hand pop"
      rel="desc_item.php?whichitem=232982896"
               onClick='javascript:descitem(232982896)'></td>
      <td valign=center><a onClick='javascript:descitem(232982896)'><b>lucky moai statuette</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td>
      <td>
        <img src=/images/itemimages/spirit_easter.gif width=30 height=30 onClick='javascript:descitem(756560474)' alt="Spirit of Easter" title="Spirit of Easter"></td>
      <td><b>50</b>&nbsp;&nbsp;</td>
      <td>
        <img src=/images/itemimages/spirit_stpatrick.gif width=30 height=30 onClick='javascript:descitem(655322653)' alt="Spirit of St. Patrick's Day" title="Spirit of St. Patrick's Day"></td>
      <td><b>50</b>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td></td>
      <td>&nbsp;&nbsp;</td>
      <td valign=center>
        <input class="button doit multibuy "  type=button rel='shop.php?whichshop=crimbo24_factory&action=buyitem&quantity=1&whichrow=1538&pwd=daa2136bd0601e7e4671281b3354bd48' value='Buy'></td>
    </tr>
  */

  /* Crimbo24 Pirate Armory: sell an item for multiple item
     <tr rel="11405">
       <td valign=center><input type=radio name=whichrow value=1420></td>
       <td><img src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/crimbucparts.gif" class="hand pop" rel="desc_item.php?whichitem=269803558" onClick='javascript:descitem(269803558)'></td>
       <td valign=center><a onClick='javascript:descitem(269803558)'><b>Crimbuccaneer flotsam</b>&nbsp;<b>(3)</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td>
       <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/crimbucgun.gif width=30 height=30 onClick='javascript:descitem(493752694)' alt="sawed-off blunderbuss" title="sawed-off blunderbuss"></td>
       <td><b>1</b>&nbsp;&nbsp;</td>
       <td></td>
       <td>&nbsp;&nbsp;</td>
       <td></td>
       <td>&nbsp;&nbsp;</td>
       <td></td>
       <td>&nbsp;&nbsp;</td>
       <td></td>
       <td>&nbsp;&nbsp;</td>
       <td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=crimbo23_pirate_armory&action=buyitem&quantity=1&whichrow=1420&pwd=5b123ae252a3648944198ceaf318bd50' value='Buy'></td>
     </tr>
  */

  // <tr rel="ITEMID">
  //   <td...optional radio button containing whichrow</td>
  //   <td.../itemimages/crimbucparts.gif...javascript:descitem(269803558)...</td>
  //   <td...javascript:descitem(269803558)...<b>Crimbuccaneer
  // flotsam</b>&nbsp;<b>(3)</b>&nbsp;&nbsp;&nbsp;&nbsp;...<td>
  //   <td>.../itemimages/crimbucgun.gif...javascript:descitem(493752694)...title="sawed-off
  // blunderbuss"...</td>
  //   <td><b>1</b>&nbsp;&nbsp;</td>
  //   ... up to four more costs or no cost:
  //   <td></td>
  //   <td>&nbsp;&nbsp;</td>
  //
  // <td...rel='shop.php?whichshop=crimbo23_pirate_armory&action=buyitem&quantity=1&whichrow=1420&pwd=xxx' value='Buy'></td>
  //  </tr>

  private static final Pattern ROW_PATTERN =
      Pattern.compile("<tr rel=\"(\\d+)\">(.*?)</tr>", Pattern.DOTALL);
  private static final Pattern TD_PATTERN = Pattern.compile("<td.*?>(.*?)</td>");
  private static final Pattern TD2_PATTERN =
      Pattern.compile("itemimages/(.*?)\\..*?descitem\\((\\d*)\\)");
  private static final Pattern TD3_PATTERN = Pattern.compile("<b>\\(?(.*?)\\)?</b>");
  private static final Pattern IEVEN_PATTERN = Pattern.compile("descitem\\((.*?)\\)");
  private static final Pattern IODD_PATTERN = Pattern.compile("<b>([\\d,]+)</b>");
  private static final Pattern IROW_PATTERN = Pattern.compile("whichrow=(\\d+)");

  public static List<ShopRow> parseShop(final String html, final boolean includeMeat) {
    List<ShopRow> result = new ArrayList<>();
    Matcher m = ROW_PATTERN.matcher(html);
    while (m.find()) {
      int itemId = Integer.valueOf(m.group(1));
      int tds = 0;

      int row = 0;
      AdventureResult item = null;
      String image = null;
      String descid = null;
      String name = null;
      int count = 1;
      List<AdventureResult> ingredients = new ArrayList<>();

      String idescid = "";
      boolean isMeat = false;
      boolean skip = false;

      Matcher td = TD_PATTERN.matcher(m.group(2));
      while (td.find()) {
        String text = td.group(1);
        switch (++tds) {
          case 1 -> {
            // Optional radio button.
          }
          case 2 -> {
            // Item image and descid
            Matcher m2 = TD2_PATTERN.matcher(text);
            if (m2.find()) {
              image = m2.group(1);
              descid = m2.group(2);
            }
          }
          case 3 -> {
            // descid and item name and optional count
            if (descid == null) {
              continue;
            }
            Matcher m3 = TD3_PATTERN.matcher(text);
            name = m3.find() ? m3.group(1) : "";
            count = m3.find() ? Integer.valueOf(m3.group(1)) : 1;

            // We have found the item. Do we know what it is?
            int iid = ItemDatabase.getItemIdFromDescription(descid);
            if (itemId != iid) {
              // No. Register it by looking at the item description
              ItemDatabase.registerItem(descid);
            }
            item = new AdventureResult(itemId, count, false);
          }
          case 4, 6, 8, 10, 12 -> {
            // ingredient image, descid, name
            if (text.contains("meat.gif")) {
              isMeat = true;
            } else {
              Matcher m4 = IEVEN_PATTERN.matcher(text);
              idescid = m4.find() ? m4.group(1) : null;
              isMeat = false;
            }
          }
          case 5, 7, 9, 11, 13 -> {
            if (!isMeat && idescid == null) {
              continue;
            }
            if (isMeat && !includeMeat) {
              skip = true;
              continue;
            }

            // ingredient count
            Matcher m5 = IODD_PATTERN.matcher(text);
            int icount = 1;
            if (m5.find()) {
              icount = StringUtilities.parseInt(m5.group(1));
            }
            // We have found an ingredient. Do we know what it is?
            if (isMeat) {
              AdventureResult cost = new MeatResult(icount);
              ingredients.add(cost);
            } else {
              int iid = ItemDatabase.getItemIdFromDescription(idescid);
              if (iid == -1) {
                // No. Register it by looking at the item description
                ItemDatabase.registerItem(idescid);
                iid = ItemDatabase.getItemIdFromDescription(idescid);
              }
              AdventureResult ingredient = new AdventureResult(iid, icount, false);
              ingredients.add(ingredient);
            }
          }
          case 14 -> {
            // rel string with whichrow
            Matcher m6 = IROW_PATTERN.matcher(text);
            if (m6.find()) {
              row = Integer.valueOf(m6.group(1));
            }
          }
          default -> {}
        }
      }
      if (tds != 14) {
        // Don't have enough fields?
        continue;
      }
      if (skip) {
        continue;
      }
      AdventureResult[] currencies = ingredients.toArray(new AdventureResult[0]);
      ShopRow irow = new ShopRow(row, item, currencies);
      result.add(irow);
    }
    return result;
  }

  public static Set<AdventureResult> deriveCurrencies(List<ShopRow> shopRows) {
    Set<AdventureResult> result = new HashSet<>();
    for (ShopRow shopRow : shopRows) {
      for (AdventureResult cost : shopRow.getCosts()) {
        AdventureResult currency = cost.getInstance(1);
        result.add(currency);
      }
    }
    return result;
  }

  // <b>You have:</b>
  // <table>
  //   <tr>
  //     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/spirit_easter.gif width=30
  // height=30 onClick='javascript:descitem(756560474)' alt="Spirit of Easter" title="Spirit of
  // Easter"></td>
  //     <td>1,527 Spirits of Easter</td></tr><tr>
  //     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/spirit_stpatrick.gif width=30
  // height=30 onClick='javascript:descitem(655322653)' alt="Spirit of St. Patrick's Day"
  // title="Spirit of St. Patrick's Day"></td>
  //     <td>1,413 Spirits of St. Patrick's Day</td></tr><tr>
  //     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/spirit_veterans.gif width=30
  // height=30 onClick='javascript:descitem(303899144)' alt="Spirit of Veteran's Day" title="Spirit
  // of Veteran's Day"></td>
  //     <td>no Spirits of Veteran's Day</td></tr><tr>
  //     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/spirit_thanks.gif width=30
  // height=30 onClick='javascript:descitem(166270423)' alt="Spirit of Thanksgiving" title="Spirit
  // of Thanksgiving"></td>
  //     <td>no Spirits of Thanksgiving</td></tr><tr>
  //     <td><img src=https://d2uyhvukfffg5a.cloudfront.net/itemimages/spirit_xmas.gif width=30
  // height=30 onClick='javascript:descitem(903725390)' alt="Spirit of Christmas" title="Spirit of
  // Christmas"></td>
  //     <td>no Spirits of Christmas</td>
  //   </tr>
  // </table>

  private static final Pattern CURRENCY_TABLE_PATTERN =
      Pattern.compile("You have:.*?<table>(.*?)</table>", Pattern.DOTALL);
  private static final Pattern CURRENCY_PATTERN = Pattern.compile("javascript:descitem\\((.*?)\\)");

  public static Set<AdventureResult> parseCurrencies(final String html) {
    Set<AdventureResult> result = new HashSet<>();
    Matcher tableMatcher = CURRENCY_TABLE_PATTERN.matcher(html);
    if (tableMatcher.find()) {
      Matcher currencyMatcher = CURRENCY_PATTERN.matcher(tableMatcher.group(1));
      while (currencyMatcher.find()) {
        int itemId = ItemDatabase.getItemIdFromDescription(currencyMatcher.group(1));
        if (itemId != -1) {
          AdventureResult currency = new AdventureResult(itemId, 1, false);
          result.add(currency);
        }
      }
    }
    return result;
  }

  // Conversion from ShopRow objects to and from data strings.
  // KoLmafia data file format is tab-separated fields
  //
  // Star Chart	ROW142	star shirt	star (15)	line (15)	star chart

  public String toData(final String shopName) {
    return toData(shopName, this.row, this.item, this.costs);
  }

  public static String toData(
      final String shopName,
      final int row,
      final AdventureResult item,
      final AdventureResult... costs) {
    StringBuilder buf = new StringBuilder();
    buf.append(shopName);
    buf.append("\t");
    buf.append("ROW");
    buf.append(row);
    buf.append("\t");
    buf.append(item);
    for (AdventureResult cost : costs) {
      buf.append("\t");
      buf.append(cost);
    }
    return buf.toString();
  }

  public static ShopRow fromData(final String[] data) {
    if (data.length < 4) {
      return null;
    }
    if (!data[1].startsWith("ROW")) {
      return null;
    }

    String master = data[0];
    int row = Integer.valueOf(data[1].substring(3));
    AdventureResult item = AdventureResult.parseItem(data[2], true);
    List<AdventureResult> costs = new ArrayList<>();
    for (int index = 3; index < data.length; ++index) {
      AdventureResult cost = AdventureResult.parseItem(data[index], true);
      costs.add(cost);
    }
    return new ShopRow(row, item, costs.toArray(new AdventureResult[0]));
  }
}
