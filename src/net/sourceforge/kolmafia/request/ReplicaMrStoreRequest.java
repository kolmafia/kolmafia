package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ReplicaMrStoreRequest extends CoinMasterRequest {
  public static final String master = "Replica Mr. Store";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) Replica Mr. Accessor");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.REPLICA_MR_ACCESSORY, 1);

  public static final CoinmasterData REPLICA_MR_STORE =
      new CoinmasterData(master, "Replica Mr. Store", ReplicaMrStoreRequest.class)
          .withToken("replica Mr. Accessory")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "mrreplica")
          .withCanBuyItem(ReplicaMrStoreRequest::canBuyItem)
          .withAvailableItem(ReplicaMrStoreRequest::availableItem);

  public ReplicaMrStoreRequest() {
    super(REPLICA_MR_STORE);
  }

  public ReplicaMrStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(REPLICA_MR_STORE, buying, attachments);
  }

  public ReplicaMrStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(REPLICA_MR_STORE, buying, attachment);
  }

  public ReplicaMrStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(REPLICA_MR_STORE, buying, itemId, quantity);
  }

  private static final Map<Integer, Integer> itemToYear =
      Map.ofEntries(
          Map.entry(ItemPool.REPLICA_DARK_JILL, 2004),
          Map.entry(ItemPool.REPLICA_HAND_TURKEY, 2004),
          Map.entry(ItemPool.REPLICA_CRIMBO_ELF, 2004),
          Map.entry(ItemPool.REPLICA_GRAVY_MAYPOLE, 2005),
          Map.entry(ItemPool.REPLICA_WAX_LIPS, 2005),
          Map.entry(ItemPool.REPLICA_BUGBEAR_SHAMAN, 2005),
          Map.entry(ItemPool.REPLICA_SNOWCONE_BOOK, 2006),
          Map.entry(ItemPool.REPLICA_JEWEL_EYED_WIZARD_HAT, 2006),
          Map.entry(ItemPool.REPLICA_PUMPKIN_BUCKET, 2006),
          Map.entry(ItemPool.REPLICA_BOTTLE_ROCKET, 2007),
          Map.entry(ItemPool.REPLICA_NAVEL_RING, 2007),
          Map.entry(ItemPool.REPLICA_V_MASK, 2007),
          Map.entry(ItemPool.REPLICA_FIREWORKS, 2008),
          Map.entry(ItemPool.REPLICA_COTTON_CANDY_COCOON, 2008),
          Map.entry(ItemPool.REPLICA_HAIKU_KATANA, 2008),
          Map.entry(ItemPool.REPLICA_BANDERSNATCH, 2009),
          Map.entry(ItemPool.REPLICA_ELVISH_SUNGLASSES, 2009),
          Map.entry(ItemPool.REPLICA_SQUAMOUS_POLYP, 2009),
          Map.entry(ItemPool.REPLICA_JUJU_MOJO_MASK, 2010),
          Map.entry(ItemPool.REPLICA_GREAT_PANTS, 2010),
          Map.entry(ItemPool.REPLICA_ORGAN_GRINDER, 2010),
          Map.entry(ItemPool.REPLICA_CUTE_ANGEL, 2011),
          Map.entry(ItemPool.REPLICA_PATRIOT_SHIELD, 2011),
          Map.entry(ItemPool.REPLICA_PLASTIC_VAMPIRE_FANGS, 2011),
          Map.entry(ItemPool.REPLICA_RESOLUTION_BOOK, 2012),
          Map.entry(ItemPool.REPLICA_CAMP_SCOUT_BACKPACK, 2012),
          Map.entry(ItemPool.REPLICA_DEACTIVATED_NANOBOTS, 2012),
          Map.entry(ItemPool.REPLICA_GREEN_THUMB, 2013),
          Map.entry(ItemPool.REPLICA_FOLDER_HOLDER, 2013),
          Map.entry(ItemPool.REPLICA_SMITH_BOOK, 2013),
          Map.entry(ItemPool.REPLICA_GENE_SPLICING_LAB, 2014),
          Map.entry(ItemPool.REPLICA_STILL_GRILL, 2014),
          Map.entry(ItemPool.REPLICA_CRIMBO_SAPLING, 2014),
          Map.entry(ItemPool.REPLICA_CHATEAU_ROOM_KEY, 2015),
          Map.entry(ItemPool.REPLICA_YELLOW_PUCK, 2015),
          Map.entry(ItemPool.REPLICA_DECK_OF_EVERY_CARD, 2015),
          Map.entry(ItemPool.REPLICA_WITCHESS_SET, 2016),
          Map.entry(ItemPool.REPLICA_INTERGNAT, 2016),
          Map.entry(ItemPool.REPLICA_SOURCE_TERMINAL, 2016),
          Map.entry(ItemPool.REPLICA_SPACE_PLANULA, 2017),
          Map.entry(ItemPool.REPLICA_ROBORTENDER, 2017),
          Map.entry(ItemPool.REPLICA_GENIE_BOTTLE, 2017),
          Map.entry(ItemPool.REPLICA_GARBAGE_TOTE, 2018),
          Map.entry(ItemPool.REPLICA_GOD_LOBSTER, 2018),
          Map.entry(ItemPool.REPLICA_NEVERENDING_PARTY_INVITE, 2018),
          Map.entry(ItemPool.REPLICA_SAUSAGE_O_MATIC, 2019),
          Map.entry(ItemPool.REPLICA_FOURTH_SABER, 2019),
          Map.entry(ItemPool.REPLICA_HEWN_MOON_RUNE_SPOON, 2019),
          Map.entry(ItemPool.REPLICA_POWERFUL_GLOVE, 2020),
          Map.entry(ItemPool.REPLICA_CAMELCALF, 2020),
          Map.entry(ItemPool.REPLICA_CARGO_CULTIST_SHORTS, 2020),
          Map.entry(ItemPool.REPLICA_MINIATURE_CRYSTAL_BALL, 2021),
          Map.entry(ItemPool.REPLICA_EMOTION_CHIP, 2021),
          Map.entry(ItemPool.REPLICA_INDUSTRIAL_FIRE_EXTINGUISHER, 2021),
          Map.entry(ItemPool.REPLICA_GREY_GOSLING, 2022),
          Map.entry(ItemPool.REPLICA_DESIGNER_SWEATPANTS, 2022),
          Map.entry(ItemPool.REPLICA_JURASSIC_PARKA, 2022),
          Map.entry(ItemPool.REPLICA_CINCHO_DE_MAYO, 2023),
          Map.entry(ItemPool.REPLICA_MR_STORE_2002_CATALOG, 2023),
          Map.entry(ItemPool.REPLICA_PATRIOTIC_EAGLE, 2023));

  private static final Set<Integer> freeYears = Set.of(2023);

  static Boolean canBuyItem(final Integer itemId) {
    return availableItem(itemId);
  }

  static Boolean availableItem(final Integer itemId) {
    Integer itemYear = itemToYear.get(itemId);
    if (itemYear == null) {
      return false;
    }
    int currentYear = Preferences.getInteger("currentReplicaStoreYear");
    if (itemYear == currentYear) {
      return true;
    }
    // This year's items disappear from the store after you purchase them
    if (freeYears.contains(itemYear)
        && InventoryManager.getCount(itemId) == 0
        && InventoryManager.getEquippedCount(itemId) == 0) {
      return true;
    }
    return false;
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  // <td colspan=14 align=center>&mdash; <b>2007</b> &mdash;</td>
  private static final Pattern YEAR_PATTERN = Pattern.compile("&mdash; <b>(\\d+)</b> &mdash;");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=mrreplica")) {
      return;
    }

    Matcher yearMatcher = YEAR_PATTERN.matcher(responseText);
    if (yearMatcher.find()) {
      int year = StringUtilities.parseInt(yearMatcher.group(1));
      Preferences.setInteger("currentReplicaStoreYear", year);
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(REPLICA_MR_STORE, urlString, responseText);
      // Purchasing certain items makes them unavailable
      NamedListenerRegistry.fireChange("(coinmaster)");
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(REPLICA_MR_STORE, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mrreplica")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(REPLICA_MR_STORE, urlString, true);
  }

  public static String accessible() {
    if (!KoLCharacter.inLegacyOfLoathing()) {
      return "Only Legacy Loathers can buy replica Mr. Items";
    }
    return null;
  }
}
