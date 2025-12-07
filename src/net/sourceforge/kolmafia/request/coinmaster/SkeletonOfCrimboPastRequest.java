package net.sourceforge.kolmafia.request.coinmaster;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.CoinMasterShopRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SkeletonOfCrimboPastRequest extends CoinMasterRequest {
  public static final String master = "Skeleton of Crimbo Past";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? knucklebones?\\.");
  public static final AdventureResult KNUCKLEBONE = ItemPool.get(ItemPool.KNUCKLEBONE, 1);
  private static final Pattern OPTION_PATTERN = Pattern.compile("option=(\\d+)");

  public static final CoinmasterData SKELETON_OF_CRIMBO_PAST =
      new CoinmasterData(master, "socp", SkeletonOfCrimboPastRequest.class)
          .withToken("knucklebone")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(KNUCKLEBONE)
          .withBuyURL("choice.php?whichchoice=1567")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withEquip(SkeletonOfCrimboPastRequest::equip)
          .withUnequip(SkeletonOfCrimboPastRequest::unequip)
          .withAccessible(SkeletonOfCrimboPastRequest::accessible);

  private record Option(String option, int id) {}

  private static final Option[] OPTIONS = {
    new Option("1", ItemPool.SMOKING_POPE),
    new Option("2", ItemPool.PRIZE_TURKEY),
    new Option("3", ItemPool.MEDICAL_GRUEL),
  };

  private static String idToOption(final int id) {
    if (id < ItemPool.SMOKING_POPE) return "4";
    return Arrays.stream(OPTIONS)
        .filter(o -> o.id == id)
        .map(o -> o.option)
        .findFirst()
        .orElse(null);
  }

  private static int optionToId(final String opt) {
    if (opt.equals("4")) return Preferences.getInteger("_crimboPastDailySpecialItem");
    return Arrays.stream(OPTIONS)
        .filter(o -> o.option.equals(opt))
        .map(o -> o.id)
        .findFirst()
        .orElse(-1);
  }

  public SkeletonOfCrimboPastRequest() {
    super(SKELETON_OF_CRIMBO_PAST);
  }

  public SkeletonOfCrimboPastRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SKELETON_OF_CRIMBO_PAST, buying, attachments);
  }

  @Override
  public void setItem(final AdventureResult item) {
    int itemId = item.getItemId();
    String option = idToOption(itemId);
    this.addFormField("option", option);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void visit(final String text) {
    var special = extractDailySpecial(text);

    Preferences.setBoolean("_crimboPastSmokingPope", !text.contains("Buy a Smoking Pope"));
    Preferences.setBoolean("_crimboPastPrizeTurkey", !text.contains("Buy a prize turkey"));
    Preferences.setBoolean("_crimboPastMedicalGruel", !text.contains("Buy medical gruel"));
    Preferences.setBoolean("_crimboPastDailySpecial", !text.contains("Daily Special"));

    if (special != null) {
      var item = special.getKey();
      var price = special.getValue();

      Preferences.setInteger("_crimboPastDailySpecialItem", item.getItemId());
      Preferences.setInteger("_crimboPastDailySpecialPrice", price);
      applySpecial();
    }
  }

  public static void checkSpecial() {
    // SKip if no skeleton
    if (SkeletonOfCrimboPastRequest.accessible() != null) return;
    // If we're able to just apply a daily special we're already aware of, great.
    if (applySpecial()) return;
    // Otherwise we need to visit to learn and apply the special.
    RequestThread.postRequest(getRequest());
  }

  private static boolean applySpecial() {
    var itemId = Preferences.getInteger("_crimboPastDailySpecialItem");
    var price = Preferences.getInteger("_crimboPastDailySpecialPrice");

    if (itemId < 0 || price <= 0) return false;

    var item = ItemPool.get(itemId);
    var items = SKELETON_OF_CRIMBO_PAST.getBuyItems();
    var prices = SKELETON_OF_CRIMBO_PAST.getBuyPrices();

    if (items.contains(item) && prices.getOrDefault(itemId, 0).equals(price)) return true;

    items.removeIf(i -> i.getItemId() < ItemPool.SMOKING_POPE);
    items.add(item);
    prices.entrySet().removeIf(e -> e.getKey() < ItemPool.SMOKING_POPE);
    prices.put(item.getItemId(), price);
    SKELETON_OF_CRIMBO_PAST.registerPurchaseRequests();

    return true;
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = SKELETON_OF_CRIMBO_PAST;

    if (location.contains("option=5")) {
      // We exited the choice adventure.
      return;
    }

    Matcher optionMatcher = OPTION_PATTERN.matcher(location);
    if (!optionMatcher.find()) {
      return;
    }

    String option = optionMatcher.group(1);
    int itemId = optionToId(option);

    if (itemId != -1) {
      CoinMasterRequest.completePurchase(data, itemId, 1, false);
    }

    CoinMasterRequest.parseBalance(data, responseText);
    ConcoctionDatabase.setRefreshNeeded(true);
  }

  public static final boolean registerRequest(final String urlString) {
    CoinmasterData data = SKELETON_OF_CRIMBO_PAST;

    // "Chat[ting]" to the Skeleton of Crimbo Past visits the coinmaster choice
    if (urlString.startsWith("main.php") && urlString.contains("talktosocp=1")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Visiting " + data.getMaster());
      return true;
    }

    // choice.php?pwd&whichchoice=1567
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1567")) {
      return false;
    }

    if (urlString.contains("option=5")) {
      // We exited the choice adventure.
      return true;
    }

    Matcher optionMatcher = OPTION_PATTERN.matcher(urlString);
    if (!optionMatcher.find()) {
      return true;
    }

    String option = optionMatcher.group(1);
    int itemId = optionToId(option);

    if (itemId != -1) {
      CoinMasterRequest.buyStuff(data, itemId, 1, false);
    }

    return true;
  }

  public static String accessible() {
    var ownsFamiliar = KoLCharacter.ownedFamiliar(FamiliarPool.SKELETON_OF_CRIMBO_PAST).isPresent();
    if (!ownsFamiliar) {
      return "You do not have a Skeleton of Crimbo Past";
    }
    // From initial testing it appears that the familiar needn't be active to visit the shop.
    // Once it is out of standard we will need to check if this still works.
    return null;
  }

  public static Boolean equip() {
    // Chat with your Skeleton of Crimbo Past. Does not need to be active familiar
    RequestThread.postRequest(new GenericRequest("main.php?talktosocp=1", false));
    return true;
  }

  public static Boolean unequip() {
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1567&option=5"));
    return true;
  }

  public static CoinMasterShopRequest getRequest() {
    return CoinMasterShopRequest.getRequest(SKELETON_OF_CRIMBO_PAST);
  }

  private static final Pattern DAILY_SPECIAL_PATTERN =
      Pattern.compile("Daily Special:.*?descitem\\((\\d+)\\).*?\\((\\d+) knucklebones\\)");

  public static Map.Entry<AdventureResult, Integer> extractDailySpecial(final String text) {
    Matcher matcher = DAILY_SPECIAL_PATTERN.matcher(text);

    if (matcher.find()) {
      var descid = matcher.group(1);
      var id = ItemDatabase.getItemIdFromDescription(descid);
      var item = ItemPool.get(id);
      var price = StringUtilities.parseInt(matcher.group(2));
      return Map.entry(ItemPool.get(item.getItemId()), price);
    }

    return null;
  }
}
