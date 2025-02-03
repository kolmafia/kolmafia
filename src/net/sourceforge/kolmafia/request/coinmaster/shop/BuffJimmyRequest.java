package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class BuffJimmyRequest extends CoinMasterRequest {
  public static final String master = "Buff Jimmy's Souvenir Shop";
  public static final String SHOPID = "sbb_jimmy";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData BUFF_JIMMY =
      new CoinmasterData(master, "BuffJimmy", BuffJimmyRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(BuffJimmyRequest::accessible);

  public BuffJimmyRequest() {
    super(BUFF_JIMMY);
  }

  public BuffJimmyRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BUFF_JIMMY, buying, attachments);
  }

  public BuffJimmyRequest(final boolean buying, final AdventureResult attachment) {
    super(BUFF_JIMMY, buying, attachment);
  }

  public BuffJimmyRequest(final boolean buying, final int itemId, final int quantity) {
    super(BUFF_JIMMY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_sleazeAirportToday")
        && !Preferences.getBoolean("sleazeAirportAlways")) {
      return "You don't have access to Spring Break Beach";
    }
    if (KoLCharacter.getLimitMode().limitZone("Spring Break Beach")) {
      return "You cannot currently access Spring Break Beach";
    }
    return null;
  }
}
