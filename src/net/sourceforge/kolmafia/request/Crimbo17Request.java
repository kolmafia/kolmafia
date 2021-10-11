package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class Crimbo17Request extends CoinMasterRequest {
  public static final String master = "Cheer-o-Vend 3000";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(Crimbo17Request.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(Crimbo17Request.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(Crimbo17Request.master);
  private static final Pattern CHEER_PATTERN = Pattern.compile("([\\d,]+) crystalline cheer");
  public static final AdventureResult CHEER = ItemPool.get(ItemPool.CRYSTALLINE_CHEER, 1);

  public static final CoinmasterData CRIMBO17 =
      new CoinmasterData(
          Crimbo17Request.master,
          "crimbo17",
          Crimbo17Request.class,
          "crystalline cheer",
          "no crystalline cheer",
          false,
          Crimbo17Request.CHEER_PATTERN,
          Crimbo17Request.CHEER,
          null,
          Crimbo17Request.itemRows,
          "shop.php?whichshop=crimbo17",
          "buyitem",
          Crimbo17Request.buyItems,
          Crimbo17Request.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          String classType = KoLCharacter.getClassType();
          switch (itemId) {
            case ItemPool.MIME_SCIENCE_VOL_1:
              return classType.equals(KoLCharacter.SEAL_CLUBBER);
            case ItemPool.MIME_SCIENCE_VOL_2:
              return classType.equals(KoLCharacter.TURTLE_TAMER);
            case ItemPool.MIME_SCIENCE_VOL_3:
              return classType.equals(KoLCharacter.PASTAMANCER);
            case ItemPool.MIME_SCIENCE_VOL_4:
              return classType.equals(KoLCharacter.SAUCEROR);
            case ItemPool.MIME_SCIENCE_VOL_5:
              return classType.equals(KoLCharacter.DISCO_BANDIT);
            case ItemPool.MIME_SCIENCE_VOL_6:
              return classType.equals(KoLCharacter.ACCORDION_THIEF);
          }
          return super.canBuyItem(itemId);
        }
      };

  public Crimbo17Request() {
    super(Crimbo17Request.CRIMBO17);
  }

  public Crimbo17Request(final boolean buying, final AdventureResult[] attachments) {
    super(Crimbo17Request.CRIMBO17, buying, attachments);
  }

  public Crimbo17Request(final boolean buying, final AdventureResult attachment) {
    super(Crimbo17Request.CRIMBO17, buying, attachment);
  }

  public Crimbo17Request(final boolean buying, final int itemId, final int quantity) {
    super(Crimbo17Request.CRIMBO17, buying, itemId, quantity);
  }

  @Override
  public void run() {
    if (this.action != null) {
      this.addFormField("pwd");
    }

    super.run();
  }

  @Override
  public void processResults() {
    Crimbo17Request.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo17")) {
      return;
    }

    CoinmasterData data = Crimbo17Request.CRIMBO17;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return "Crimbo is gone";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo17")) {
      return false;
    }

    CoinmasterData data = Crimbo17Request.CRIMBO17;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
