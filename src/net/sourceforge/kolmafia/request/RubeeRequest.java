package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class RubeeRequest extends CoinMasterRequest {
  public static final String master = "FantasyRealm Rubee&trade; Store";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Rubees&trade;");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.RUBEE, 1);

  public static final CoinmasterData RUBEE =
      new CoinmasterData(master, "FantasyRealm Store", RubeeRequest.class)
          .withToken("Rubee&trade;")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "fantasyrealm");

  public RubeeRequest() {
    super(RUBEE);
  }

  public RubeeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(RUBEE, buying, attachments);
  }

  public RubeeRequest(final boolean buying, final AdventureResult attachment) {
    super(RUBEE, buying, attachment);
  }

  public RubeeRequest(final boolean buying, final int itemId, final int quantity) {
    super(RUBEE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=fantasyrealm")) {
      return;
    }

    CoinmasterData data = RUBEE;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=fantasyrealm")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(RUBEE, urlString, true);
  }

  public static String accessible() {
    return Preferences.getBoolean("_frToday") || Preferences.getBoolean("frAlways")
        ? null
        : "Need access to Fantasy Realm";
  }

  @Override
  public void equip() {
    if (!KoLCharacter.hasEquipped(ItemPool.FANTASY_REALM_GEM)) {
      EquipmentRequest request =
          new EquipmentRequest(ItemPool.get(ItemPool.FANTASY_REALM_GEM), Slot.ACCESSORY3);
      RequestThread.postRequest(request);
    }
  }
}
