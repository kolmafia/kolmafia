package net.sourceforge.kolmafia.request;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BurningLeavesRequest extends CreateItemRequest {
  private enum Outcome {
    NONE(0),
    LEAFLET(11),
    BOMB(37, ItemPool.AUTUMNIC_BOMB),
    TORCH(42, ItemPool.IMPROMPTU_TORCH),
    FIG_LEAF(43, ItemPool.FLAMING_FIG_LEAF),
    DRAPE(44, ItemPool.SMOLDERING_DRAPE),
    RESIN(50, ItemPool.DISTILLED_RESIN),
    AEGIS(66, ItemPool.AUTUMNAL_AEGIS),
    LASSO(69, ItemPool.LIT_LEAF_LASSO, "_leafLassosCrafted", 3),
    BED(75, ItemPool.FOREST_CANOPY_BED),
    BALM(99, ItemPool.AUTUMNIC_BALM),
    MONSTERA(111),
    DAY_SHORTENER(222, ItemPool.DAY_SHORTENER, "_leafDayShortenerCrafted", 1),
    LEAVIATHAN(666),
    COPING_JUICE(1111, ItemPool.COPING_JUICE),
    LEAFCUTTER(6666, ItemPool.SMOLDERING_LEAFCUTTER_ANT_EGG, "_leafcutterAntEggCrafted", 1),
    SUPER_HEATED(11111, ItemPool.SUPER_HEATED_LEAF, "_leafTattooCrafted", 1);

    public static Outcome findByLeaves(final int leaves) {
      return Arrays.stream(Outcome.values())
          .filter(o -> o.getLeaves() == leaves)
          .findAny()
          .orElse(NONE);
    }

    public static Outcome findByConcoction(final Concoction conc) {
      return Arrays.stream(Outcome.values())
          .filter(o -> o.getItemId() == conc.getItemId())
          .findAny()
          .orElse(NONE);
    }

    private final int leaves;
    private final int itemId;

    private final String dailyPref;
    private final int dailyMax;

    Outcome(final int leaves, final int itemId, final String dailyPref, final int dailyMax) {
      this.leaves = leaves;
      this.itemId = itemId;
      this.dailyPref = dailyPref;
      this.dailyMax = dailyMax;
    }

    Outcome(final int leaves, final int itemId) {
      this(leaves, itemId, null, -1);
    }

    Outcome(final int leaves) {
      this(leaves, -1);
    }

    public int getLeaves() {
      return leaves;
    }

    public int getItemId() {
      return itemId;
    }

    public int canMake() {
      var max = InventoryManager.getAccessibleCount(ItemPool.INFLAMMABLE_LEAF) / this.getLeaves();
      if (this.dailyMax < 0) return max;
      if (this.dailyMax == 1) return Preferences.getBoolean(this.dailyPref) ? 0 : Math.min(max, 1);
      return Math.min(max, this.dailyMax - Preferences.getInteger(this.dailyPref));
    }

    public void increment() {
      if (this == NONE) {
        Preferences.increment("_leavesBurned");
        return;
      }
      if (this.dailyMax < 0) return;
      if (this.dailyMax == 1) {
        Preferences.setBoolean(this.dailyPref, true);
        return;
      }
      Preferences.increment(this.dailyPref, 1, this.dailyMax, false);
    }

    public void setToMax() {
      if (this.itemId < 0) {
        Preferences.setInteger("_leafMonstersFought", 5);
      }
      if (this.dailyMax < 0) return;
      if (this.dailyMax == 1) {
        Preferences.setBoolean(this.dailyPref, true);
      } else {
        Preferences.setInteger(this.dailyPref, this.dailyMax);
      }
    }
  }

  public static int canMake(final Concoction conc) {
    var outcome = Outcome.findByConcoction(conc);
    return outcome.canMake();
  }

  public BurningLeavesRequest(final Concoction conc) {
    super("choice.php", conc);
    var outcome = Outcome.findByConcoction(conc);
    this.addFormField("whichchoice", "1510");
    this.addFormField("leaves", String.valueOf(outcome.getLeaves()));
    this.addFormField("option", "1");
  }

  private static GenericRequest VISIT_REQUEST =
      new GenericRequest("campground.php?preaction=leaves", false);

  public static void randomLeaves(final int leaves) {
    VISIT_REQUEST.run();
    var req = new GenericRequest("choice.php");
    req.addFormField("whichchoice", "1510");
    req.addFormField("leaves", String.valueOf(leaves));
    req.addFormField("option", "1");
    req.run();
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    int count = this.getQuantityNeeded();
    if (count == 0) {
      return;
    }

    // Attempt to retrieve the ingredients
    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + count + " " + this.getName() + "...");

    int creationYield = this.getYield();

    VISIT_REQUEST.run();

    while (count > 0 && KoLmafia.permitsContinue()) {
      this.setQuantityNeeded(Math.min(count, creationYield));
      super.run();
      count -= creationYield;
    }
  }

  private static final Pattern LEAVES_BURNED_PATTERN =
      Pattern.compile("You've stoked the fire with <b>(\\d)</b> random lea(?:f|ves) today\\.");

  public static void visitChoice(final String responseText) {
    Preferences.setBoolean("_leavesJumped", !responseText.contains("Jump in the Flames"));

    Matcher matcher = BurningLeavesRequest.LEAVES_BURNED_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int leavesBurned = Integer.parseInt(matcher.group(1));
      Preferences.setInteger("_leavesBurned", leavesBurned);
    }
  }

  public static void postChoice(final String responseText, final int leaves) {
    if (responseText.contains(
        "You jump in the blazing fire absorb some of the flames and jump out")) {
      Preferences.setBoolean("_leavesJumped", true);
      return;
    }

    if (responseText.contains("You can't thrown in none leaves.")
        || responseText.contains("You don't have that many leaves!")) {
      return;
    }

    var outcome = Outcome.findByLeaves(leaves);

    if (responseText.contains("You don't feel like burning that many leaves again today.")) {
      outcome.setToMax();
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR,
          "You don't feel like burning that many leaves again today.");
      return;
    }

    // Note that fights will never reach here as they have no postChoice.
    outcome.increment();

    ResultProcessor.processItem(ItemPool.INFLAMMABLE_LEAF, leaves * -1);
  }

  public static final Pattern URL_LEAVES_PATTERN = Pattern.compile("leaves=(\\d+)");

  public static int extractLeavesFromURL(final String urlString) {
    Matcher matcher = URL_LEAVES_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static void registerLeafFight(final String location) {
    Preferences.increment("_leafMonstersFought", 1, 5, false);
    var leaves = extractLeavesFromURL(location);
    ResultProcessor.processItem(ItemPool.INFLAMMABLE_LEAF, leaves * -1);
  }

  public static boolean registerRequest(final String urlString) {
    return urlString.startsWith("choice.php") && urlString.contains("whichchoice=1510");
  }
}
