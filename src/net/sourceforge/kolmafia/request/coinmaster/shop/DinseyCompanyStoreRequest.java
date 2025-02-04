package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class DinseyCompanyStoreRequest extends CoinMasterRequest {
  public static final String master = "The Dinsey Company Store";
  public static final String SHOPID = "landfillstore";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FunFunds");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.FUNFUNDS, 1);

  public static final CoinmasterData DINSEY_COMPANY_STORE =
      new CoinmasterData(master, "DinseyStore", DinseyCompanyStoreRequest.class)
          .withToken("FunFunds&trade;")
          .withPluralToken("FunFunds&trade;")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(DinseyCompanyStoreRequest::accessible);

  public DinseyCompanyStoreRequest() {
    super(DINSEY_COMPANY_STORE);
  }

  public DinseyCompanyStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DINSEY_COMPANY_STORE, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_stenchAirportToday")
        && !Preferences.getBoolean("stenchAirportAlways")) {
      return "You don't have access to Dinseylandfill";
    }
    if (KoLCharacter.getLimitMode().limitZone("Dinseylandfill")) {
      return "You cannot currently access Dinseylandfill";
    }
    return null;
  }
}
