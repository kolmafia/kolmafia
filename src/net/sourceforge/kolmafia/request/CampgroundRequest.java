package net.sourceforge.kolmafia.request;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest extends GenericRequest {
  private static final Pattern LIBRAM_PATTERN =
      Pattern.compile(
          "Summon (Candy Heart|Party Favor|Love Song|BRICKOs|Dice|Resolutions|Taffy) *.[(]([\\d,]+) MP[)]");
  private static final Pattern HOUSING_PATTERN =
      Pattern.compile("/rest([\\da-z])(tp)?(_free)?.gif");
  private static final Pattern CINCHO_LOOSEN_PATTERN =
      Pattern.compile("your cincho loosens (\\d+)%");
  private static final Pattern FURNISHING_PATTERN = Pattern.compile("<b>(?:an? )?(.*?)</b>");

  private static final Pattern JUNG_PATTERN = Pattern.compile("junggate_(\\d)");
  private static final Pattern DNA_PATTERN = Pattern.compile("sample of <b>(.*?)</b> DNA");
  private static final Pattern FUEL_PATTERN_1 = Pattern.compile("fuel gauge reads ([\\d,]+) litre");
  private static final Pattern FUEL_PATTERN_2 =
      Pattern.compile("<p>The fuel gauge currently reads: (.*?)</p>");
  private static final Pattern FUEL_PATTERN_3 = Pattern.compile("&qty=([\\d,]+)&iid=([\\d,]+)");
  private static final Pattern COLD_MEDICINE_CABINET_PATTERN =
      Pattern.compile(
          "You can visit the doctors again in (\\d+) turns?\\.<br>You have (\\d) consul");

  private static int currentDwellingLevel = 0;
  private static AdventureResult currentDwelling = null;
  private static AdventureResult currentBed = null;
  private static AdventureResult currentWorkshedItem = null;
  private static int asdonMartinFuel = 0;

  public static final AdventureResult BIG_ROCK = ItemPool.get(ItemPool.BIG_ROCK, 1);

  public static final AdventureResult BLACK_BLUE_LIGHT = ItemPool.get(ItemPool.BLACK_BLUE_LIGHT, 1);
  public static final AdventureResult LOUDMOUTH_LARRY = ItemPool.get(ItemPool.LOUDMOUTH_LARRY, 1);
  public static final AdventureResult PLASMA_BALL = ItemPool.get(ItemPool.PLASMA_BALL, 1);
  public static final AdventureResult LED_CLOCK = ItemPool.get(ItemPool.LED_CLOCK, 1);

  // The following are items that (can) have modifiers in modifiers.txt
  public static final Set<Integer> campgroundItems =
      Set.of(
          // Housing
          ItemPool.BIG_ROCK,
          ItemPool.NEWBIESPORT_TENT,
          ItemPool.BARSKIN_TENT,
          ItemPool.COTTAGE,
          ItemPool.HOUSE,
          ItemPool.SANDCASTLE,
          ItemPool.TWIG_HOUSE,
          ItemPool.HOBO_FORTRESS,
          ItemPool.GINGERBREAD_HOUSE,
          ItemPool.BRICKO_PYRAMID,
          ItemPool.GINORMOUS_PUMPKIN,
          ItemPool.GIANT_FARADAY_CAGE,
          ItemPool.SNOW_FORT,
          ItemPool.ELEVENT,
          ItemPool.RESIDENCE_CUBE,
          ItemPool.GIANT_PILGRIM_HAT,
          ItemPool.HOUSE_SIZED_MUSHROOM,

          // Bedding
          ItemPool.BEANBAG_CHAIR,
          ItemPool.COLD_BEDDING,
          ItemPool.GAUZE_HAMMOCK,
          ItemPool.HOT_BEDDING,
          ItemPool.LAZYBONES_RECLINER,
          ItemPool.SLEAZE_BEDDING,
          ItemPool.SPOOKY_BEDDING,
          ItemPool.STENCH_BEDDING,
          ItemPool.SLEEPING_STOCKING,
          ItemPool.SALTWATERBED,
          ItemPool.SPIRIT_BED,

          // Inside dwelling: maids
          ItemPool.MAID,
          ItemPool.CLOCKWORK_MAID,
          ItemPool.MEAT_BUTLER,

          // Inside dwelling: miscellaneous
          // (Certificate of Participation)
          // (Shiny Certificate of Participation)
          ItemPool.BONSAI_TREE,
          ItemPool.CUCKOO_CLOCK,
          ItemPool.FENG_SHUI,
          ItemPool.LED_CLOCK,
          ItemPool.LUCKY_CAT_STATUE,
          ItemPool.MEAT_GLOBE,
          ItemPool.PICTURE_OF_YOU,
          ItemPool.TIN_ROOF,
          ItemPool.CRIMBO_CANDLE,

          // Inside dwelling: "Tasteful" items
          ItemPool.BLACK_BLUE_LIGHT,
          ItemPool.LOUDMOUTH_LARRY,
          ItemPool.PLASMA_BALL,

          // Kitchen
          ItemPool.SHAKER,
          ItemPool.COCKTAIL_KIT,
          ItemPool.BARTENDER,
          ItemPool.CLOCKWORK_BARTENDER,
          ItemPool.OVEN,
          ItemPool.RANGE,
          ItemPool.CHEF,
          ItemPool.CLOCKWORK_CHEF,

          // Workshed
          ItemPool.CHEMISTRY_LAB,
          ItemPool.INDUCTION_OVEN,
          ItemPool.LP_ROM_BURNER,
          ItemPool.HIGH_EFFICIENCY_STILL,
          ItemPool.AUTO_ANVIL,
          ItemPool.JACKHAMMER_DRILL_PRESS,
          ItemPool.SNOW_MACHINE,
          ItemPool.SPINNING_WHEEL,
          ItemPool.DNA_LAB,
          ItemPool.MAYO_CLINIC,
          ItemPool.ASDON_MARTIN,
          ItemPool.DIABOLIC_PIZZA_CUBE,
          ItemPool.COLD_MEDICINE_CABINET,
          ItemPool.MODEL_TRAIN_SET,

          // Outside dwelling
          ItemPool.MEAT_GOLEM,
          ItemPool.PAGODA_PLANS,
          ItemPool.SCARECROW,
          ItemPool.TOILET_PAPER,
          ItemPool.HAUNTED_DOGHOUSE,
          ItemPool.WITCHESS_SET,
          ItemPool.SOURCE_TERMINAL,
          ItemPool.TRAPEZOID,
          ItemPool.GIANT_BLACK_MONOLITH,

          // Special item that aids resting
          ItemPool.COMFY_BLANKET);

  public static final int[] transientFurnishings = {
    // Bedding
    ItemPool.BEANBAG_CHAIR,
    ItemPool.COLD_BEDDING,
    ItemPool.GAUZE_HAMMOCK,
    ItemPool.HOT_BEDDING,
    ItemPool.LAZYBONES_RECLINER,
    ItemPool.SLEAZE_BEDDING,
    ItemPool.SPOOKY_BEDDING,
    ItemPool.STENCH_BEDDING,
    ItemPool.SLEEPING_STOCKING,
    ItemPool.SALTWATERBED,
    ItemPool.SPIRIT_BED,

    // Inside dwelling: miscellaneous
    ItemPool.BONSAI_TREE,
    ItemPool.CUCKOO_CLOCK,
    ItemPool.FENG_SHUI,
    ItemPool.LED_CLOCK,
    ItemPool.LUCKY_CAT_STATUE,
    ItemPool.MEAT_GLOBE,
    ItemPool.TIN_ROOF,
    ItemPool.CRIMBO_CANDLE,

    // Inside dwelling: "Tasteful" items
    ItemPool.BLACK_BLUE_LIGHT,
    ItemPool.LOUDMOUTH_LARRY,
    ItemPool.PLASMA_BALL,
  };

  public static class TallGrass extends AdventureResult {
    public TallGrass(int count) {
      super("packet of grass seeds", count);
    }

    @Override
    public String toString() {
      int count = this.getCount();
      return count == 1
          ? "tall grass"
          : count < 8 ? "tall grass (" + count + ")" : "very tall grass";
    }

    @Override
    public String getName() {
      int count = this.getCount();
      return count != 8 ? "tall grass" : "very tall grass";
    }

    @Override
    public int getPluralCount() {
      int count = this.getCount();
      return count != 8 ? count : 1;
    }

    @Override
    public String getPluralName() {
      return this.getPluralName(this.getCount());
    }

    @Override
    public String getPluralName(int count) {
      return count == 1
          ? "patch of tall grass"
          : count < 8 ? "patches of tall grass" : "patch of very tall grass";
    }
  }

  public static class Mushroom extends AdventureResult {
    public Mushroom(int days) {
      super("packet of mushroom spores", days);
    }

    @Override
    public int getPluralCount() {
      // We always have 1 mushroom to pick
      return 1;
    }

    @Override
    public String getPluralName() {
      return this.toString();
    }

    @Override
    public String toString() {
      return switch (count) {
        case 1 -> "free-range mushroom";
        case 2 -> "plump free-range mushroom";
        case 3 -> "bulky free-range mushroom";
        case 4 -> "giant free-range mushroom";
        case 5, 6, 7, 8, 9, 10 -> "immense free-range mushroom";
        default -> "colossal free-range mushroom";
      };
    }
  }

  // Crops
  public static final AdventureResult PUMPKIN = ItemPool.get(ItemPool.PUMPKIN, 1);
  public static final AdventureResult HUGE_PUMPKIN = ItemPool.get(ItemPool.HUGE_PUMPKIN, 1);
  public static final AdventureResult GINORMOUS_PUMPKIN =
      ItemPool.get(ItemPool.GINORMOUS_PUMPKIN, 1);
  public static final AdventureResult PEPPERMINT_SPROUT =
      ItemPool.get(ItemPool.PEPPERMINT_SPROUT, 1);
  public static final AdventureResult GIANT_CANDY_CANE = ItemPool.get(ItemPool.GIANT_CANDY_CANE, 1);
  public static final AdventureResult SKELETON = ItemPool.get(ItemPool.SKELETON, 1);
  public static final AdventureResult BARLEY = ItemPool.get(ItemPool.BARLEY, 1);
  public static final AdventureResult BEER_LABEL = ItemPool.get(ItemPool.FANCY_BEER_LABEL, 1);
  public static final AdventureResult TWO_BEER_LABEL = ItemPool.get(ItemPool.FANCY_BEER_LABEL, 2);
  public static final AdventureResult THREE_BEER_LABEL = ItemPool.get(ItemPool.FANCY_BEER_LABEL, 3);
  public static final AdventureResult ICE_HARVEST = ItemPool.get(ItemPool.ICE_HARVEST, 1);
  public static final AdventureResult FROST_FLOWER = ItemPool.get(ItemPool.FROST_FLOWER, 1);
  public static final AdventureResult CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 1);
  public static final AdventureResult THREE_CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 3);
  public static final AdventureResult FIVE_CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 5);
  public static final AdventureResult EIGHT_CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 8);
  public static final AdventureResult ELEVEN_CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 11);
  public static final AdventureResult FIFTEEN_CORNUCOPIA = ItemPool.get(ItemPool.CORNUCOPIA, 15);
  public static final AdventureResult MEGACOPIA = ItemPool.get(ItemPool.MEGACOPIA, 1);
  public static final AdventureResult NO_TALL_GRASS = new TallGrass(0);
  public static final AdventureResult TALL_GRASS = new TallGrass(1);
  public static final AdventureResult TWO_TALL_GRASS = new TallGrass(2);
  public static final AdventureResult THREE_TALL_GRASS = new TallGrass(3);
  public static final AdventureResult FOUR_TALL_GRASS = new TallGrass(4);
  public static final AdventureResult FIVE_TALL_GRASS = new TallGrass(5);
  public static final AdventureResult SIX_TALL_GRASS = new TallGrass(6);
  public static final AdventureResult SEVEN_TALL_GRASS = new TallGrass(7);
  public static final AdventureResult VERY_TALL_GRASS = new TallGrass(8);
  public static final AdventureResult FREE_RANGE_MUSHROOM = new Mushroom(1);
  public static final AdventureResult PLUMP_FREE_RANGE_MUSHROOM = new Mushroom(2);
  public static final AdventureResult BULKY_FREE_RANGE_MUSHROOM = new Mushroom(3);
  public static final AdventureResult GIANT_FREE_RANGE_MUSHROOM = new Mushroom(4);
  public static final AdventureResult IMMENSE_FREE_RANGE_MUSHROOM = new Mushroom(5);
  public static final AdventureResult COLOSSAL_FREE_RANGE_MUSHROOM = new Mushroom(11);
  public static final AdventureResult GROVELING_GRAVEL = ItemPool.get(ItemPool.GROVELING_GRAVEL, 1);
  public static final AdventureResult FRUITY_PEBBLE = ItemPool.get(ItemPool.FRUITY_PEBBLE, 1);
  public static final AdventureResult LODESTONE = ItemPool.get(ItemPool.LODESTONE, 1);
  public static final AdventureResult MILESTONE = ItemPool.get(ItemPool.MILESTONE, 1);
  public static final AdventureResult BOLDER_BOULDER = ItemPool.get(ItemPool.BOLDER_BOULDER, 1);
  public static final AdventureResult MOLEHILL_MOUNTAIN =
      ItemPool.get(ItemPool.MOLEHILL_MOUNTAIN, 1);
  public static final AdventureResult WHETSTONE = ItemPool.get(ItemPool.WHETSTONE, 1);
  public static final AdventureResult HARD_ROCK = ItemPool.get(ItemPool.HARD_ROCK, 1);
  public static final AdventureResult STRANGE_STALAGMITE =
      ItemPool.get(ItemPool.STRANGE_STALAGMITE, 1);

  public enum CropType {
    PUMPKIN(ItemPool.PUMPKIN_SEEDS),
    PEPPERMINT(ItemPool.PEPPERMINT_PACKET),
    SKELETON(ItemPool.DRAGON_TEETH),
    BEER(ItemPool.BEER_SEEDS),
    WINTER(ItemPool.WINTER_SEEDS),
    THANKSGARDEN(ItemPool.THANKSGARDEN_SEEDS),
    GRASS(ItemPool.TALL_GRASS_SEEDS),
    MUSHROOM(ItemPool.MUSHROOM_SPORES),
    ROCK(ItemPool.ROCK_SEEDS),
    ;

    private final AdventureResult seeds;

    CropType(int seeds) {
      this.seeds = ItemPool.get(seeds, 1);
    }

    public AdventureResult getSeeds() {
      return this.seeds;
    }

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }
  }

  public enum CropPlot {
    PLOT1("rgarden1"),
    PLOT2("rgarden2"),
    PLOT3("rgarden3"),
    ;

    // Class fields
    private final String action;

    // Derived fields
    private final String lowerCaseName;

    // Map field
    private static final Map<String, CropPlot> nameToPlotMap = new HashMap<>();

    private CropPlot(String action) {
      this.action = action;
      this.lowerCaseName = this.name().toLowerCase();
    }

    public String getAction() {
      return this.action;
    }

    public static CropPlot nameToPlot(final String name) {
      return nameToPlotMap.get(name.toLowerCase());
    }

    public void populateMaps() {
      nameToPlotMap.put(this.lowerCaseName, this);
    }

    @Override
    public String toString() {
      return this.lowerCaseName;
    }
  }

  static {
    for (var plot : EnumSet.allOf(CropPlot.class)) {
      plot.populateMaps();
    }
  }

  private static final HashMap<AdventureResult, CropType> CROPMAP = new HashMap<>();
  private static final HashMap<AdventureResult, CropPlot> CROPPLOT = new HashMap<>();

  static {
    CROPMAP.put(PUMPKIN, CropType.PUMPKIN);
    CROPMAP.put(HUGE_PUMPKIN, CropType.PUMPKIN);
    CROPMAP.put(GINORMOUS_PUMPKIN, CropType.PUMPKIN);
    CROPMAP.put(PEPPERMINT_SPROUT, CropType.PEPPERMINT);
    CROPMAP.put(GIANT_CANDY_CANE, CropType.PEPPERMINT);
    CROPMAP.put(SKELETON, CropType.SKELETON);
    CROPMAP.put(BARLEY, CropType.BEER);
    CROPMAP.put(BEER_LABEL, CropType.BEER);
    CROPMAP.put(ICE_HARVEST, CropType.WINTER);
    CROPMAP.put(FROST_FLOWER, CropType.WINTER);
    CROPMAP.put(CORNUCOPIA, CropType.THANKSGARDEN);
    CROPMAP.put(MEGACOPIA, CropType.THANKSGARDEN);
    CROPMAP.put(TALL_GRASS, CropType.GRASS);
    CROPMAP.put(FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(PLUMP_FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(BULKY_FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(GIANT_FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(IMMENSE_FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(COLOSSAL_FREE_RANGE_MUSHROOM, CropType.MUSHROOM);
    CROPMAP.put(GROVELING_GRAVEL, CropType.ROCK);
    CROPPLOT.put(GROVELING_GRAVEL, CropPlot.PLOT1);
    CROPMAP.put(FRUITY_PEBBLE, CropType.ROCK);
    CROPPLOT.put(FRUITY_PEBBLE, CropPlot.PLOT1);
    CROPMAP.put(LODESTONE, CropType.ROCK);
    CROPPLOT.put(LODESTONE, CropPlot.PLOT1);
    CROPMAP.put(MILESTONE, CropType.ROCK);
    CROPPLOT.put(MILESTONE, CropPlot.PLOT2);
    CROPMAP.put(BOLDER_BOULDER, CropType.ROCK);
    CROPPLOT.put(BOLDER_BOULDER, CropPlot.PLOT2);
    CROPMAP.put(MOLEHILL_MOUNTAIN, CropType.ROCK);
    CROPPLOT.put(MOLEHILL_MOUNTAIN, CropPlot.PLOT2);
    CROPMAP.put(WHETSTONE, CropType.ROCK);
    CROPPLOT.put(WHETSTONE, CropPlot.PLOT3);
    CROPMAP.put(HARD_ROCK, CropType.ROCK);
    CROPPLOT.put(HARD_ROCK, CropPlot.PLOT3);
    CROPMAP.put(STRANGE_STALAGMITE, CropType.ROCK);
    CROPPLOT.put(STRANGE_STALAGMITE, CropPlot.PLOT3);
  }

  public static CropPlot cropToPlot(AdventureResult crop) {
    return CROPPLOT.get(crop);
  }

  public static final List<Integer> workshedItems =
      Arrays.asList(
          ItemPool.JACKHAMMER_DRILL_PRESS,
          ItemPool.AUTO_ANVIL,
          ItemPool.INDUCTION_OVEN,
          ItemPool.CHEMISTRY_LAB,
          ItemPool.HIGH_EFFICIENCY_STILL,
          ItemPool.LP_ROM_BURNER,
          ItemPool.SNOW_MACHINE,
          ItemPool.SPINNING_WHEEL,
          ItemPool.DNA_LAB,
          ItemPool.MAYO_CLINIC,
          ItemPool.ASDON_MARTIN,
          ItemPool.DIABOLIC_PIZZA_CUBE,
          ItemPool.COLD_MEDICINE_CABINET,
          ItemPool.MODEL_TRAIN_SET);

  public static final AdventureResult[] CROPS = {
    CampgroundRequest.PUMPKIN,
    CampgroundRequest.HUGE_PUMPKIN,
    CampgroundRequest.GINORMOUS_PUMPKIN,
    CampgroundRequest.PEPPERMINT_SPROUT,
    CampgroundRequest.GIANT_CANDY_CANE,
    CampgroundRequest.SKELETON,
    CampgroundRequest.BARLEY,
    CampgroundRequest.BEER_LABEL,
    CampgroundRequest.TWO_BEER_LABEL,
    CampgroundRequest.THREE_BEER_LABEL,
    CampgroundRequest.ICE_HARVEST,
    CampgroundRequest.FROST_FLOWER,
    CampgroundRequest.CORNUCOPIA,
    CampgroundRequest.THREE_CORNUCOPIA,
    CampgroundRequest.FIVE_CORNUCOPIA,
    CampgroundRequest.EIGHT_CORNUCOPIA,
    CampgroundRequest.ELEVEN_CORNUCOPIA,
    CampgroundRequest.FIFTEEN_CORNUCOPIA,
    CampgroundRequest.MEGACOPIA,
    CampgroundRequest.TALL_GRASS,
    // CampgroundRequest.TWO_TALL_GRASS,
    // CampgroundRequest.THREE_TALL_GRASS,
    // CampgroundRequest.FOUR_TALL_GRASS,
    // CampgroundRequest.FIVE_TALL_GRASS,
    // CampgroundRequest.SIX_TALL_GRASS,
    // CampgroundRequest.SEVEN_TALL_GRASS,
    CampgroundRequest.VERY_TALL_GRASS,
    CampgroundRequest.FREE_RANGE_MUSHROOM,
    CampgroundRequest.PLUMP_FREE_RANGE_MUSHROOM,
    CampgroundRequest.BULKY_FREE_RANGE_MUSHROOM,
    CampgroundRequest.GIANT_FREE_RANGE_MUSHROOM,
    CampgroundRequest.IMMENSE_FREE_RANGE_MUSHROOM,
    CampgroundRequest.COLOSSAL_FREE_RANGE_MUSHROOM,
    CampgroundRequest.GROVELING_GRAVEL,
    CampgroundRequest.FRUITY_PEBBLE,
    CampgroundRequest.LODESTONE,
    CampgroundRequest.MILESTONE,
    CampgroundRequest.BOLDER_BOULDER,
    CampgroundRequest.MOLEHILL_MOUNTAIN,
    CampgroundRequest.WHETSTONE,
    CampgroundRequest.HARD_ROCK,
    CampgroundRequest.STRANGE_STALAGMITE,
  };

  public static void reset() {
    KoLConstants.campground.clear();
    CampgroundRequest.currentDwellingLevel = 0;
    CampgroundRequest.currentDwelling = null;
    CampgroundRequest.currentBed = null;
    CampgroundRequest.currentWorkshedItem = null;
  }

  private final String action;

  /** Constructs a new <code>CampgroundRequest</code> with the specified action in mind. */

  // campground.php?action=garden&pwd

  public CampgroundRequest(final String action) {
    super("campground.php");
    this.addFormField("action", action);
    this.action = action;
  }

  /** Constructs a new <code>CampgroundRequest</code>. */
  public CampgroundRequest() {
    // Not available in Ed and some Limitmodes
    this("inspectdwelling");
  }

  @Override
  public int getAdventuresUsed() {
    return getAdventuresUsed("rest".equals(this.action));
  }

  public static int getAdventuresUsed(String urlString) {
    return getAdventuresUsed("rest".equals(GenericRequest.getAction(urlString)));
  }

  private static int getAdventuresUsed(boolean rest) {
    return rest && KoLCharacter.freeRestsRemaining() == 0 ? 1 : 0;
  }

  public static void setCampgroundItem(final int itemId, int count) {
    CampgroundRequest.setCampgroundItem(ItemPool.get(itemId, count));
  }

  public static void setCampgroundItem(final AdventureResult item) {
    int i = KoLConstants.campground.indexOf(item);
    if (i != -1) {
      AdventureResult old = KoLConstants.campground.get(i);
      if (old.getCount() == item.getCount()) {
        return;
      }
      KoLConstants.campground.remove(i);
    }
    KoLConstants.campground.add(item);
  }

  public static void removeCampgroundItem(AdventureResult item) {
    int i = KoLConstants.campground.indexOf(item);
    if (i != -1) {
      KoLConstants.campground.remove(i);
    }
  }

  public static AdventureResult getCrop() {
    for (AdventureResult crop : CampgroundRequest.CROPS) {
      int index = KoLConstants.campground.indexOf(crop);
      if (index != -1) {
        return KoLConstants.campground.get(index);
      }
    }
    return null;
  }

  public static List<AdventureResult> getCrops() {
    var list = new ArrayList<AdventureResult>();
    for (AdventureResult crop : CampgroundRequest.CROPS) {
      int index = KoLConstants.campground.indexOf(crop);
      if (index != -1) {
        list.add(KoLConstants.campground.get(index));
      }
    }
    return list;
  }

  public static CropType getCropType() {
    return CampgroundRequest.getCropType(getCrop());
  }

  public static CropType getCropType(AdventureResult crop) {
    return crop == null ? null : CROPMAP.get(crop);
  }

  public static AdventureResult parseCrop(final String crop) {
    String name = crop;
    int count = 1;

    int paren = crop.indexOf(" (");
    if (paren != -1) {
      name = crop.substring(0, paren).trim();
      count = StringUtilities.parseInt(crop.substring(paren + 2, crop.length() - 1));
    }

    return switch (name) {
      case "tall grass" -> CampgroundRequest.TALL_GRASS.getInstance(count);
      case "very tall grass" -> CampgroundRequest.VERY_TALL_GRASS;
      case "free-range mushroom" -> CampgroundRequest.FREE_RANGE_MUSHROOM;
      case "plump free-range mushroom" -> CampgroundRequest.PLUMP_FREE_RANGE_MUSHROOM;
      case "bulky free-range mushroom" -> CampgroundRequest.BULKY_FREE_RANGE_MUSHROOM;
      case "giant free-range mushroom" -> CampgroundRequest.GIANT_FREE_RANGE_MUSHROOM;
      case "immense free-range mushroom" -> CampgroundRequest.IMMENSE_FREE_RANGE_MUSHROOM;
      case "colossal free-range mushroom" -> CampgroundRequest.COLOSSAL_FREE_RANGE_MUSHROOM;
      default -> new AdventureResult(name, count, false);
    };
  }

  public static boolean hasCropOrBetter(final String crop) {
    return CampgroundRequest.hasCropOrBetter(CampgroundRequest.getCrop(), crop);
  }

  public static boolean hasCropOrBetter(final AdventureResult current, final String cropName) {
    if (current == null || current.getCount() == 0 || cropName.equals("none")) {
      // Nothing in your garden or no garden or don't want to pick
      return false;
    }

    // We want whatever is there.  Since we made it this far,
    // we have something to pick.
    if (cropName.equals("any")) {
      return true;
    }

    int currentID = current.getItemId();
    int currentCount = current.getCount();

    AdventureResult desired = CampgroundRequest.parseCrop(cropName);
    int desiredID = desired.getItemId();
    int desiredCount = desired.getCount();

    // If the current crop type equals the desired crop and the
    // count is at least as great, peachy. Or is it pumpkiny?
    if (currentID == desiredID) {
      return currentCount >= desiredCount;
    }

    for (AdventureResult crop : CampgroundRequest.CROPS) {
      int cropID = crop.getItemId();

      // We found the current crop before we found the
      // desired crop. Not good enough.
      if (cropID == currentID) {
        return false;
      }

      // We found the desired crop before we found the
      // current crop - which is therefore better IFF its type is the same.
      if (cropID == desiredID) {
        return CROPMAP.get(crop) == CROPMAP.get(current);
      }
    }

    // Shouldn't get here - didn't find either the current or the desired crop
    return false;
  }

  public static void clearCrop() {
    for (AdventureResult crop : CampgroundRequest.CROPS) {
      KoLConstants.campground.remove(crop);
    }
    for (AdventureResult seed :
        Arrays.stream(CropType.values())
            .map(CropType::getSeeds)
            .collect(Collectors.toUnmodifiableSet())) {
      KoLConstants.campground.remove(seed);
    }
  }

  public static void harvestCrop() {
    List<AdventureResult> crops = CampgroundRequest.getCrops();
    if (crops.isEmpty()) {
      // No garden
      return;
    }

    CropType cropType = CampgroundRequest.getCropType(crops.get(0));

    // Dealing with mushroom gardens is an Adventure
    if (cropType == CropType.MUSHROOM) {
      harvestMushrooms(true);
      return;
    }

    // Other garden types have zero or more things to pick.
    // We learned the count by looking at the campground.
    int count = crops.stream().mapToInt(AdventureResult::getCount).sum();
    if (count == 0) {
      // Nothing to pick.
      return;
    }

    if (cropType == CropType.ROCK) {
      // rock garden is actually 3 gardens
      var itemIds =
          crops.stream().map(AdventureResult::getItemId).collect(Collectors.toUnmodifiableSet());
      if (itemIds.contains(ItemPool.GROVELING_GRAVEL)
          || itemIds.contains(ItemPool.FRUITY_PEBBLE)
          || itemIds.contains(ItemPool.LODESTONE)) {
        CampgroundRequest request = new CampgroundRequest("rgarden1");
        RequestThread.postRequest(request);
      }
      if (itemIds.contains(ItemPool.MILESTONE)
          || itemIds.contains(ItemPool.BOLDER_BOULDER)
          || itemIds.contains(ItemPool.MOLEHILL_MOUNTAIN)) {
        CampgroundRequest request = new CampgroundRequest("rgarden2");
        RequestThread.postRequest(request);
      }
      if (itemIds.contains(ItemPool.WHETSTONE)
          || itemIds.contains(ItemPool.HARD_ROCK)
          || itemIds.contains(ItemPool.STRANGE_STALAGMITE)) {
        CampgroundRequest request = new CampgroundRequest("rgarden3");
        RequestThread.postRequest(request);
      }
    } else {
      // Grass plots are special: each cluster of tall grass is picked
      // individually - except for Very Tall Grass (the 8th growth)
      if (cropType != CropType.GRASS || count == 8) {
        // Harvest the entire garden in one go
        count = 1;
      }

      // Pick your crop (in multiple requests, if Tall Grass)
      CampgroundRequest request = new CampgroundRequest("garden");
      while (count-- > 0) {
        RequestThread.postRequest(request);
      }
    }
  }

  public static void harvestCrop(CropPlot plot) {
    List<AdventureResult> crops = CampgroundRequest.getCrops();
    if (crops.isEmpty()) {
      // No garden
      return;
    }

    // The Rock Garden is actually 3 plots.
    // For now, CROPPLOT only has Rock Garden entries
    for (var crop : crops) {
      if (CROPPLOT.get(crop) == plot && crop.getCount() > 0) {
        CampgroundRequest request = new CampgroundRequest(plot.getAction());
        RequestThread.postRequest(request);
      }
    }
  }

  public static void fertilizeCrop() {
    // For now, only mushroom gardens need to be fertilized each day.
    CampgroundRequest.harvestMushrooms(false);
  }

  public static void harvestMushrooms(final boolean pick) {
    AdventureResult crop = CampgroundRequest.getCrop();
    if (crop == null || CampgroundRequest.getCropType(crop) != CropType.MUSHROOM) {
      // We don't have a mushroom garden
      return;
    }

    if (Preferences.getBoolean("_mushroomGardenVisited")) {
      // We've already fertilized or picked our mushroom garden today
      return;
    }

    // Fight through Piranha plants: adventure.php?snarfblat=543

    if (KoLCharacter.getAdventuresLeft() <= 0) {
      // You need an available turn (even though the fights are free)
      return;
    }

    if (KoLCharacter.isFallingDown()
        && !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.DRUNKULA_WINEGLASS))) {
      // You cannot be falling down drunk
      return;
    }

    // We expect redirection to either fight.php or choice.php
    // We want to handle it ourselves, so run it in a RelayRequest
    RelayRequest request = new RelayRequest(false);

    KoLAdventure.setNextAdventure("Your Mushroom Garden");

    while (KoLmafia.permitsContinue()) {
      if (RecoveryManager.isRecoveryPossible()) {
        RecoveryManager.runBetweenBattleChecks(true);
      }

      if (!KoLmafia.permitsContinue()) {
        return;
      }

      request.constructURLString("adventure.php?snarfblat=543");
      RequestThread.postRequest(request);

      // We expect redirection
      String redirect = request.redirectLocation;

      // If the adventure does not redirect, we've already
      // dealt with the mushroom garden today.
      if (redirect == null) {
        Preferences.setBoolean("_mushroomGardenVisited", true);
        return;
      }

      // If it redirects to a fight, we are still fighting
      // our way through piranha plants.
      if (redirect.startsWith("fight.php") || redirect.startsWith("fambattle.php")) {
        // Fight! Fight! Fight!
        FightRequest.INSTANCE.run(redirect);
        KoLmafia.executeAfterAdventureScript();
        if (!FightRequest.won) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You were defeated by a piranha plant. Run this fight manually.");
          return;
        }
        continue;
      }

      // If it redirects to something other than a choice,
      // something weird is happening. Bail.
      if (!redirect.startsWith("choice.php")) {
        return;
      }

      // Follow the redirection to choice.php
      request.constructURLString(redirect, false);
      RequestThread.postRequest(request);

      // Turtle Taming can preempt The Mushy Center
      if (ChoiceManager.lastChoice != 1410) {
        // Take the default (only) option.
        ChoiceManager.gotoGoal();
        continue;
      }

      // We have reached The Mushy Center.
      // Look at which options are available.

      Map<Integer, String> options = ChoiceUtilities.parseChoices(request.responseText);

      int fertilizeOption = 0; // Expect 1
      int pickOption = 0; // Expect 2

      for (Map.Entry<Integer, String> entry : options.entrySet()) {
        Integer key = entry.getKey();
        String value = entry.getValue();
        if (value.startsWith("Fertilize")) {
          fertilizeOption = key;
        } else if (value.startsWith("Pick")) {
          pickOption = key;
        }
      }

      // Decide which choice option to submit
      int option = pick ? pickOption : fertilizeOption;

      // If there is no option to fertilize, pick.
      if (fertilizeOption == 0) {
        option = pickOption;
      }

      ChoiceManager.processChoiceAdventure(option, "", false);
      break;
    }
  }

  public static void updateElVibratoPortal() {
    int charges = Preferences.getInteger("currentPortalEnergy");
    CampgroundRequest.removeCampgroundItem(ItemPool.get(ItemPool.TRAPEZOID));
    CampgroundRequest.setCampgroundItem(ItemPool.TRAPEZOID, charges);
  }

  public static void growTallGrass() {
    AdventureResult crop = CampgroundRequest.getCrop();
    if (crop == null || CampgroundRequest.getCropType(crop) != CropType.GRASS) {
      // We don't have a grass patch
      return;
    }

    int count = crop.getCount();
    if (count == 8) {
      // We already have very tall grass
      return;
    }

    // Remove existing grass from campground
    CampgroundRequest.removeCampgroundItem(crop);

    // Improve plot of grass and add it back to the campground
    CampgroundRequest.setCampgroundItem(crop.getInstance(count + 1));
  }

  public static void useSpinningWheel() {
    if (CampgroundRequest.currentWorkshedItem != null
        && CampgroundRequest.currentWorkshedItem.getItemId() == ItemPool.SPINNING_WHEEL) {
      RequestThread.postRequest(new CampgroundRequest("spinningwheel"));
    }
  }

  @Override
  public void run() {
    if (this.action.equals("rest")
        && KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP()
        && KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP()
        && !KoLConstants.activeEffects.contains(KoLAdventure.BEATEN_UP)) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "You don't need to rest.");
      return;
    }

    if (this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft()) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "You don't have any time left for that");
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    CampgroundRequest.parseResponse(this.getURLString(), this.responseText);
  }

  @Override
  protected boolean shouldFollowRedirect() {
    // Will redirect in Nuclear Autumn
    if (KoLCharacter.inNuclearAutumn()) {
      return true;
    }
    // Workshed may be redirected to Shop if Mayo Clinic installed
    return action != null && (action.equals("workshed") || action.equals("terminal"));
  }

  public static void handleCinchoRest(final String responseText) {
    var m = CINCHO_LOOSEN_PATTERN.matcher(responseText);
    if (m.find()) {
      Preferences.decrement("_cinchUsed", StringUtilities.parseInt(m.group(1)), 0);
      Preferences.increment("_cinchoRests");
    }
  }

  public static void parseResponse(final String urlString, final String responseText) {
    // Workshed may redirect to shop.php
    if (urlString.startsWith("shop.php")) {
      NPCPurchaseRequest.parseShopResponse(urlString, responseText);
      return;
    }

    if (!urlString.startsWith("campground.php")) {
      return;
    }

    var action = GenericRequest.getAction(urlString);
    var preaction = GenericRequest.getPreaction(urlString);

    if (action == null) {
      CampgroundRequest.parseCampground(urlString, responseText);
      action = "";
    }

    if (preaction == null) {
      preaction = "";
    }

    // A request can have both action=bookshelf and preaction=yyy.
    // Check for that.
    if (action.equals("bookshelf")) {
      if (preaction.equals("")) {
        // No preaction. Look at books.
        CampgroundRequest.parseBookTitles(responseText);
        return;
      }
    }

    if (action.equals("makepizza")) {
      PizzaCubeRequest.parseResponse(urlString, responseText);
      return;
    }

    if (action.endsWith("powerelvibratoportal")) {
      PortalRequest.parseResponse(urlString, responseText);
      return;
    }

    if (action.startsWith("telescope")) {
      TelescopeRequest.parseResponse(urlString, responseText);
      return;
    }

    // Using a book skill from the Mystic Bookshelf does this:
    //   campground.php?quantity=1&preaction=summonlovesongs&pwd
    //
    // Using a book skill from the skill menu redirects to the
    // above URL with an additional field:
    //   skilluse=1

    // Combining clip arts does this:
    //   campground.php?action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03&pwd

    if (preaction.startsWith("summon") || preaction.equals("combinecliparts")) {
      UseSkillRequest.parseResponse(urlString, responseText);
      return;
    }

    if (action.equals("rest")) {
      Preferences.increment("timesRested", 1);

      // Your black-and-blue light cycles wildly between
      // black and blue, then emits a shower of sparks as it
      // goes permanently black.
      if (responseText.contains("goes permanently black")) {
        CampgroundRequest.removeCampgroundItem(BLACK_BLUE_LIGHT);
      }

      // Your blue plasma ball crackles weakly, emits a whine
      // that sounds like "pika...pika...pika..." and goes
      // dark.
      if (responseText.contains("crackles weakly")) {
        CampgroundRequest.removeCampgroundItem(PLASMA_BALL);
      }

      // Your Loudmouth Larry Lamprey twitches and flops
      // wildly, singing "Daisy, Daisy, tell me your answer
      // true," in ever-slower, distorted loops. Looks like
      // it's ready to go to its eternal fishy reward.
      if (responseText.contains("eternal fishy reward")) {
        CampgroundRequest.removeCampgroundItem(LOUDMOUTH_LARRY);
      }

      // You dream that your teeth fall out, and you put them
      // in your pocket for safe keeping. Fortunately, when
      // you wake up, you appear to have grown a new set.
      if (responseText.contains("your teeth fall out")) {
        ResultProcessor.processItem(ItemPool.LOOSE_TEETH, 1);
      }

      // "Hey," he says, "youse got some teeth. T'anks. Here
      // youse goes."
      if (responseText.contains("youse got some teeth")) {
        ResultProcessor.processItem(ItemPool.LOOSE_TEETH, -1);
      }

      if (responseText.contains("lunge toward the clock")) {
        CampgroundRequest.removeCampgroundItem(LED_CLOCK);
        Preferences.setBoolean("_confusingLEDClockUsed", true);
      }

      if (responseText.contains("razor-sharp-claw-tipped arms")
          || responseText.contains("horrible mucous")
          || responseText.contains("Tentacles, tentacles everywhere")
          || responseText.contains("teeth near your neck")) {
        Preferences.decrement("_nightmareFuelCharges");
      }

      handleCinchoRest(responseText);

      var m = HOUSING_PATTERN.matcher(responseText);
      if (m.find()) {
        KoLCharacter.updateFreeRests(m.group(3) != null);
      }

      return;
    }

    if (action.equals("garden")
        || action.equals("rgarden1")
        || action.equals("rgarden2")
        || action.equals("rgarden3")) {
      CampgroundRequest.parseCampground(urlString, responseText);
      return;
    }

    if (action.equals("inspectdwelling")) {
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.inspectDwelling(responseText);
      return;
    }

    if (action.equals("inspectkitchen")) {
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseKitchen(responseText);
      return;
    }

    if (action.equals("workshed")) {
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseWorkshed(responseText);
      return;
    }

    if (action.equals("dnapotion")) {
      if (responseText.contains("little bottle of gene tonic")) {
        Preferences.increment("_dnaPotionsMade", 1);
      }
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseWorkshed(responseText);
      return;
    }

    if (action.equals("dnainject")) {
      if (responseText.contains("abominable genetic hybrid")) {
        Preferences.setBoolean("_dnaHybrid", true);
        Preferences.setString("_dnaSyringe", "");
      }
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseWorkshed(responseText);
      return;
    }

    if (action.equals("spinningwheel")) {
      // You work the spinning wheel and spin some air into Meat.
      // You can't spin any more air into Meat today.
      if (responseText.contains("air into Meat")) {
        Preferences.setBoolean("_spinningWheel", true);
      }
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseWorkshed(responseText);
      return;
    }

    if (action.equals("fuelconvertor")) {
      Matcher fuelMatcher = FUEL_PATTERN_2.matcher(responseText);
      if (fuelMatcher.find()) {
        asdonMartinFuel = StringUtilities.parseInt(fuelMatcher.group(1));
      }
      fuelMatcher = FUEL_PATTERN_3.matcher(urlString);
      if (fuelMatcher.find()) {
        int qty = StringUtilities.parseInt(fuelMatcher.group(1));
        int itemId = StringUtilities.parseInt(fuelMatcher.group(2));
        ResultProcessor.processResult(ItemPool.get(itemId, -qty));
      }
      CampgroundRequest.parseCampground(urlString, responseText);
      CampgroundRequest.parseWorkshed(responseText);
      return;
    }

    if (preaction.equals("drive")) {
      Matcher fuelMatcher = FUEL_PATTERN_1.matcher(responseText);
      if (fuelMatcher.find()) {
        asdonMartinFuel = StringUtilities.parseInt(fuelMatcher.group(1));
      }
      return;
    }

    if (action.equals("monolith")) {
      Preferences.setBoolean("_blackMonolithUsed", true);
      return;
    }
  }

  private static void parseCampground(final String urlString, final String responseText) {
    if (urlString.contains("ajax=1")) {
      return;
    }

    boolean haveTelescope = findImage(responseText, "telescope.gif", ItemPool.TELESCOPE);
    if (haveTelescope) {
      KoLCharacter.setTelescope(true);
      KoLCharacter.checkTelescope();
      int upgrades = Preferences.getInteger("telescopeUpgrades");
      CampgroundRequest.setCampgroundItem(ItemPool.TELESCOPE, upgrades);
    }

    KoLCharacter.setBookshelf(responseText.contains("action=bookshelf"));

    findImage(responseText, "pagoda.gif", ItemPool.PAGODA_PLANS);
    findImage(responseText, "scarecrow.gif", ItemPool.SCARECROW);
    findImage(responseText, "golem.gif", ItemPool.MEAT_GOLEM);
    findImage(responseText, "doghouse.gif", ItemPool.HAUNTED_DOGHOUSE);
    findImage(responseText, "chesstable.gif", ItemPool.WITCHESS_SET);
    findImage(responseText, "campterminal.gif", ItemPool.SOURCE_TERMINAL);
    findImage(responseText, "monolith.gif", ItemPool.GIANT_BLACK_MONOLITH);

    if (responseText.contains("portal1.gif")) {
      // Charged portal.

      // If we think it is uncharged, assume it is fully
      // charged. Otherwise, believe the property.
      int charges = Preferences.getInteger("currentPortalEnergy");
      if (charges == 0) {
        Preferences.setInteger("currentPortalEnergy", 20);
        charges = 20;
      }
      updateElVibratoPortal();
    }

    if (responseText.contains("portal2.gif")) {
      // Uncharged portal.
      Preferences.setInteger("currentPortalEnergy", 0);
      updateElVibratoPortal();
    }

    if (responseText.contains("campterminal.gif")
        && Preferences.getString("sourceTerminalEducateKnown").equals("")) {
      // There is a Terminal, but we don't know what upgrades it has, so find out
      RequestThread.postRequest(new TerminalRequest("status"));
      RequestThread.postRequest(new TerminalRequest("educate"));
      RequestThread.postRequest(new TerminalRequest("enhance"));
      RequestThread.postRequest(new TerminalRequest("enquiry"));
      RequestThread.postRequest(new TerminalRequest("extrude"));
    }

    findImage(responseText, "teatree", ItemPool.POTTED_TEA_TREE);
    if (responseText.contains("teatree_used.gif")) {
      Preferences.setBoolean("_pottedTeaTreeUsed", true);
    }

    CampgroundRequest.parseGarden(responseText);

    Matcher jungMatcher = JUNG_PATTERN.matcher(responseText);
    if (jungMatcher.find()) {
      int jungLink = StringUtilities.parseInt(jungMatcher.group(1));
      switch (jungLink) {
        case 1 -> CampgroundRequest.setCampgroundItem(ItemPool.SUSPICIOUS_JAR, 1);
        case 2 -> CampgroundRequest.setCampgroundItem(ItemPool.GOURD_JAR, 1);
        case 3 -> CampgroundRequest.setCampgroundItem(ItemPool.MYSTIC_JAR, 1);
        case 4 -> CampgroundRequest.setCampgroundItem(ItemPool.OLD_MAN_JAR, 1);
        case 5 -> CampgroundRequest.setCampgroundItem(ItemPool.ARTIST_JAR, 1);
        case 6 -> CampgroundRequest.setCampgroundItem(ItemPool.MEATSMITH_JAR, 1);
        case 7 -> CampgroundRequest.setCampgroundItem(ItemPool.JICK_JAR, 1);
      }
    } else {
      Preferences.setBoolean("_psychoJarUsed", false);
    }

    CampgroundRequest.parseDwelling(responseText);
  }

  private static boolean parseGarden(final String responseText) {
    clearCrop();

    return findImage(
            responseText, "pumpkinpatch_0.gif", ItemPool.PUMPKIN, 0, ItemPool.PUMPKIN_SEEDS, 0)
        || findImage(
            responseText, "pumpkinpatch_1.gif", ItemPool.PUMPKIN, 1, ItemPool.PUMPKIN_SEEDS, 1)
        || findImage(
            responseText, "pumpkinpatch_2.gif", ItemPool.PUMPKIN, 2, ItemPool.PUMPKIN_SEEDS, 2)
        || findImage(
            responseText, "pumpkinpatch_3.gif", ItemPool.PUMPKIN, 3, ItemPool.PUMPKIN_SEEDS, 3)
        || findImage(
            responseText, "pumpkinpatch_4.gif", ItemPool.PUMPKIN, 4, ItemPool.PUMPKIN_SEEDS, 4)
        || findImage(
            responseText,
            "pumpkinpatch_giant.gif",
            ItemPool.HUGE_PUMPKIN,
            1,
            ItemPool.PUMPKIN_SEEDS,
            5)
        || findImage(
            responseText,
            "pumpkinpatch_ginormous.gif",
            ItemPool.GINORMOUS_PUMPKIN,
            1,
            ItemPool.PUMPKIN_SEEDS,
            11)
        || findImage(
            responseText,
            "pepperpatch_0.gif",
            ItemPool.PEPPERMINT_SPROUT,
            0,
            ItemPool.PEPPERMINT_PACKET,
            0)
        || findImage(
            responseText,
            "pepperpatch_1.gif",
            ItemPool.PEPPERMINT_SPROUT,
            3,
            ItemPool.PEPPERMINT_PACKET,
            1)
        || findImage(
            responseText,
            "pepperpatch_2.gif",
            ItemPool.PEPPERMINT_SPROUT,
            6,
            ItemPool.PEPPERMINT_PACKET,
            2)
        || findImage(
            responseText,
            "pepperpatch_3.gif",
            ItemPool.PEPPERMINT_SPROUT,
            9,
            ItemPool.PEPPERMINT_PACKET,
            3)
        || findImage(
            responseText,
            "pepperpatch_4.gif",
            ItemPool.PEPPERMINT_SPROUT,
            12,
            ItemPool.PEPPERMINT_PACKET,
            4)
        || findImage(
            responseText,
            "pepperpatch_huge.gif",
            ItemPool.GIANT_CANDY_CANE,
            1,
            ItemPool.PEPPERMINT_PACKET,
            5)
        || findImage(
            responseText, "bonegarden0.gif", ItemPool.SKELETON, 0, ItemPool.DRAGON_TEETH, 0)
        || findImage(
            responseText, "bonegarden1.gif", ItemPool.SKELETON, 5, ItemPool.DRAGON_TEETH, 1)
        || findImage(
            responseText, "bonegarden2.gif", ItemPool.SKELETON, 10, ItemPool.DRAGON_TEETH, 2)
        || findImage(
            responseText, "bonegarden3.gif", ItemPool.SKELETON, 15, ItemPool.DRAGON_TEETH, 3)
        || findImage(
            responseText, "bonegarden4.gif", ItemPool.SKELETON, 20, ItemPool.DRAGON_TEETH, 4)
        || findImage(
            responseText, "bonegarden5.gif", ItemPool.SKELETON, 25, ItemPool.DRAGON_TEETH, 5)
        ||
        // This is day 6 for A Bone Garden.  It triggers a combat, so it should never be
        // automatically picked.
        // Setting a negative number of items will make it possible to tell that it isn't empty.
        findImage(
            responseText,
            "bonegarden_spoilzlul.gif",
            ItemPool.SKELETON,
            -1,
            ItemPool.DRAGON_TEETH,
            6)
        || findImage(responseText, "beergarden0.gif", ItemPool.BARLEY, 0, ItemPool.BEER_SEEDS, 0)
        || findImage(responseText, "beergarden1.gif", ItemPool.BARLEY, 3, ItemPool.BEER_SEEDS, 1)
        || findImage(responseText, "beergarden2.gif", ItemPool.BARLEY, 6, ItemPool.BEER_SEEDS, 2)
        || findImage(
            responseText, "beergarden3.gif", ItemPool.FANCY_BEER_LABEL, 1, ItemPool.BEER_SEEDS, 3)
        || findImage(
            responseText, "beergarden4.gif", ItemPool.FANCY_BEER_LABEL, 1, ItemPool.BEER_SEEDS, 4)
        || findImage(
            responseText, "beergarden5.gif", ItemPool.FANCY_BEER_LABEL, 2, ItemPool.BEER_SEEDS, 5)
        || findImage(
            responseText, "beergarden6.gif", ItemPool.FANCY_BEER_LABEL, 2, ItemPool.BEER_SEEDS, 6)
        || findImage(
            responseText, "beergarden7.gif", ItemPool.FANCY_BEER_LABEL, 3, ItemPool.BEER_SEEDS, 7)
        || findImage(
            responseText, "wintergarden0.gif", ItemPool.ICE_HARVEST, 0, ItemPool.WINTER_SEEDS, 0)
        || findImage(
            responseText, "wintergarden1.gif", ItemPool.ICE_HARVEST, 3, ItemPool.WINTER_SEEDS, 1)
        || findImage(
            responseText, "wintergarden2.gif", ItemPool.ICE_HARVEST, 6, ItemPool.WINTER_SEEDS, 2)
        || findImage(
            responseText, "wintergarden3.gif", ItemPool.FROST_FLOWER, 1, ItemPool.WINTER_SEEDS, 3)
        || findImage(
            responseText, "wintergarden4.gif", ItemPool.FROST_FLOWER, 1, ItemPool.WINTER_SEEDS, 4)
        || findImage(
            responseText, "wintergarden5.gif", ItemPool.FROST_FLOWER, 1, ItemPool.WINTER_SEEDS, 5)
        || findImage(
            responseText, "wintergarden6.gif", ItemPool.FROST_FLOWER, 1, ItemPool.WINTER_SEEDS, 6)
        || findImage(
            responseText, "wintergarden7.gif", ItemPool.FROST_FLOWER, 1, ItemPool.WINTER_SEEDS, 7)
        || findImage(
            responseText,
            "thanksgarden1.gif",
            ItemPool.CORNUCOPIA,
            0,
            ItemPool.THANKSGARDEN_SEEDS,
            0)
        || findImage(
            responseText,
            "thanksgarden2.gif",
            ItemPool.CORNUCOPIA,
            1,
            ItemPool.THANKSGARDEN_SEEDS,
            1)
        || findImage(
            responseText,
            "thanksgarden3.gif",
            ItemPool.CORNUCOPIA,
            3,
            ItemPool.THANKSGARDEN_SEEDS,
            2)
        || findImage(
            responseText,
            "thanksgarden4.gif",
            ItemPool.CORNUCOPIA,
            5,
            ItemPool.THANKSGARDEN_SEEDS,
            3)
        || findImage(
            responseText,
            "thanksgarden5.gif",
            ItemPool.CORNUCOPIA,
            8,
            ItemPool.THANKSGARDEN_SEEDS,
            4)
        || findImage(
            responseText,
            "thanksgarden6.gif",
            ItemPool.CORNUCOPIA,
            11,
            ItemPool.THANKSGARDEN_SEEDS,
            5)
        || findImage(
            responseText,
            "thanksgarden7.gif",
            ItemPool.CORNUCOPIA,
            15,
            ItemPool.THANKSGARDEN_SEEDS,
            6)
        || findImage(
            responseText,
            "thanksgardenmega.gif",
            ItemPool.MEGACOPIA,
            1,
            ItemPool.THANKSGARDEN_SEEDS,
            7)
        ||
        // Use special instances of the TallGrass extension of an AdventureResult
        findImage(responseText, "grassgarden0.gif", NO_TALL_GRASS)
        || findImage(responseText, "grassgarden1.gif", TALL_GRASS)
        || findImage(responseText, "grassgarden2.gif", TWO_TALL_GRASS)
        || findImage(responseText, "grassgarden3.gif", THREE_TALL_GRASS)
        || findImage(responseText, "grassgarden4.gif", FOUR_TALL_GRASS)
        || findImage(responseText, "grassgarden5.gif", FIVE_TALL_GRASS)
        || findImage(responseText, "grassgarden6.gif", SIX_TALL_GRASS)
        || findImage(responseText, "grassgarden7.gif", SEVEN_TALL_GRASS)
        || findImage(responseText, "grassgarden8.gif", VERY_TALL_GRASS)
        || findImage(
            responseText,
            "mushgarden.gif",
            new Mushroom(Preferences.getInteger("mushroomGardenCropLevel")))
        || findRockGarden(responseText);
  }

  private static boolean findRockGarden(final String responseText) {
    if (!responseText.contains("/rockgarden/")) {
      return false;
    }

    var hasSomething = false;

    if (findImage(responseText, "rockgarden/a1.gif", ItemPool.GROVELING_GRAVEL, 1)
        || findImage(responseText, "rockgarden/a2.gif", ItemPool.GROVELING_GRAVEL, 2)
        || findImage(responseText, "rockgarden/a3.gif", ItemPool.GROVELING_GRAVEL, 3)
        || findImage(responseText, "rockgarden/a4.gif", ItemPool.FRUITY_PEBBLE, 1)
        || findImage(responseText, "rockgarden/a5.gif", ItemPool.FRUITY_PEBBLE, 2)
        || findImage(responseText, "rockgarden/a6.gif", ItemPool.FRUITY_PEBBLE, 3)
        || findImage(responseText, "rockgarden/a7.gif", ItemPool.LODESTONE, 1)) {
      hasSomething = true;
    } else {
      findImage(responseText, "rockgarden/a0.gif", ItemPool.GROVELING_GRAVEL, 0);
    }

    if (findImage(responseText, "rockgarden/b1.gif", ItemPool.MILESTONE, 1)
        || findImage(responseText, "rockgarden/b2.gif", ItemPool.MILESTONE, 2)
        || findImage(responseText, "rockgarden/b3.gif", ItemPool.MILESTONE, 3)
        || findImage(responseText, "rockgarden/b4.gif", ItemPool.BOLDER_BOULDER, 1)
        || findImage(responseText, "rockgarden/b5.gif", ItemPool.BOLDER_BOULDER, 2)
        || findImage(responseText, "rockgarden/b6.gif", ItemPool.BOLDER_BOULDER, 3)
        || findImage(responseText, "rockgarden/b7.gif", ItemPool.MOLEHILL_MOUNTAIN, 1)) {
      hasSomething = true;
    } else {
      findImage(responseText, "rockgarden/b0.gif", ItemPool.MILESTONE, 0);
    }

    if (findImage(responseText, "rockgarden/c1.gif", ItemPool.WHETSTONE, 1)
        || findImage(responseText, "rockgarden/c2.gif", ItemPool.WHETSTONE, 2)
        || findImage(responseText, "rockgarden/c3.gif", ItemPool.WHETSTONE, 3)
        || findImage(responseText, "rockgarden/c4.gif", ItemPool.HARD_ROCK, 1)
        || findImage(responseText, "rockgarden/c5.gif", ItemPool.HARD_ROCK, 2)
        || findImage(responseText, "rockgarden/c6.gif", ItemPool.HARD_ROCK, 3)
        || findImage(responseText, "rockgarden/c7.gif", ItemPool.STRANGE_STALAGMITE, 1)) {
      hasSomething = true;
    } else {
      findImage(responseText, "rockgarden/c0.gif", ItemPool.WHETSTONE, 0);
    }

    if (hasSomething) {
      CampgroundRequest.setCampgroundItem(ItemPool.ROCK_SEEDS, 1);
    } else {
      CampgroundRequest.setCampgroundItem(ItemPool.ROCK_SEEDS, 0);
    }

    return true;
  }

  private static void parseDwelling(final String responseText) {
    Matcher m = HOUSING_PATTERN.matcher(responseText);
    if (!m.find()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to parse housing!");
      return;
    }

    int dwelling = 0;
    String dwell = m.group(1);
    if (StringUtilities.isNumeric(dwell)) {
      dwelling = Integer.parseInt(dwell);
    } else {
      char dw = dwell.charAt(0);
      dwelling = dw - 'a' + 10;
    }

    int itemId = -1;
    switch (dwelling) {
      case 0 -> {
        // placeholder for "the ground"
        CampgroundRequest.currentDwelling = BIG_ROCK;
        CampgroundRequest.currentDwellingLevel = 0;
      }
      case 1 -> itemId = ItemPool.NEWBIESPORT_TENT;
      case 2 -> itemId = ItemPool.BARSKIN_TENT;
      case 3 -> itemId = ItemPool.COTTAGE;
      case 4 -> itemId = ItemPool.HOUSE;
      case 5 -> itemId = ItemPool.SANDCASTLE;
      case 6 -> itemId = ItemPool.TWIG_HOUSE;
      case 7 -> itemId = ItemPool.HOBO_FORTRESS;
      case 8 -> itemId = ItemPool.GINGERBREAD_HOUSE;
      case 9 -> itemId = ItemPool.BRICKO_PYRAMID;
      case 10 -> itemId = ItemPool.GINORMOUS_PUMPKIN;
      case 11 -> itemId = ItemPool.GIANT_FARADAY_CAGE;
      case 12 -> itemId = ItemPool.SNOW_FORT;
      case 13 -> itemId = ItemPool.ELEVENT;
      case 14 -> itemId = ItemPool.RESIDENCE_CUBE;
      case 15 -> itemId = ItemPool.GIANT_PILGRIM_HAT;
      case 16 -> itemId = ItemPool.HOUSE_SIZED_MUSHROOM;
      default -> KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Unrecognized housing type (" + CampgroundRequest.currentDwellingLevel + ")!");
    }

    if (itemId != -1) {
      CampgroundRequest.setCurrentDwelling(itemId);
    }

    if (m.group(2) != null) {
      CampgroundRequest.setCampgroundItem(ItemPool.TOILET_PAPER, 1);
    }

    KoLCharacter.updateFreeRests(m.group(3) != null);
  }

  private static void inspectDwelling(final String responseText) {
    int startIndex = responseText.indexOf("Your dwelling has the following stuff");
    int endIndex = responseText.indexOf("<b>Your Campsite</b>", startIndex + 1);
    if (startIndex > 0 && endIndex > 0) {
      var relevantResponse = responseText.substring(startIndex, endIndex);

      // Three mutually exclusive dwelling servants
      if (findImage(relevantResponse, "maid.gif", ItemPool.MAID)
          || findImage(relevantResponse, "maid2.gif", ItemPool.CLOCKWORK_MAID)
          || findImage(relevantResponse, "butler.gif", ItemPool.MEAT_BUTLER)) {}

      Matcher m = FURNISHING_PATTERN.matcher(relevantResponse);
      while (m.find()) {
        String name = m.group(1);

        if (name.equals("Really Good Feng Shui")) {
          name = "Feng Shui for Big Dumb Idiots";
        }

        AdventureResult ar = ItemPool.get(name, 1);
        if (CampgroundRequest.isBedding(ar.getItemId())) {
          CampgroundRequest.setCurrentBed(ar);
        }

        CampgroundRequest.setCampgroundItem(ar);
      }
    }
  }

  private static void parseKitchen(final String responseText) {
    boolean hasOven = findImage(responseText, "ezcook.gif", ItemPool.OVEN);
    KoLCharacter.setOven(hasOven);

    boolean hasRange = findImage(responseText, "oven.gif", ItemPool.RANGE);
    KoLCharacter.setRange(hasRange);

    boolean hasChef =
        findImage(responseText, "chefinbox.gif", ItemPool.CHEF)
            || findImage(responseText, "cchefbox.gif", ItemPool.CLOCKWORK_CHEF);
    KoLCharacter.setChef(hasChef);

    boolean hasShaker = findImage(responseText, "shaker.gif", ItemPool.SHAKER);
    KoLCharacter.setShaker(hasShaker);

    boolean hasCocktailKit = findImage(responseText, "cocktailkit.gif", ItemPool.COCKTAIL_KIT);
    KoLCharacter.setCocktailKit(hasCocktailKit);

    boolean hasBartender =
        findImage(responseText, "bartinbox.gif", ItemPool.BARTENDER)
            || findImage(responseText, "cbartbox.gif", ItemPool.CLOCKWORK_BARTENDER);
    KoLCharacter.setBartender(hasBartender);

    boolean hasSushiMat = findImage(responseText, "sushimat.gif", ItemPool.SUSHI_ROLLING_MAT);
    KoLCharacter.setSushiMat(hasSushiMat);
  }

  private static void parseWorkshed(final String responseText) {
    // Do we need to remember these things in KoLCharacter?
    if (findImage(responseText, "wbchemset.gif", ItemPool.CHEMISTRY_LAB)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.CHEMISTRY_LAB);
    } else if (findImage(responseText, "wboven.gif", ItemPool.INDUCTION_OVEN)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.INDUCTION_OVEN);
    } else if (findImage(responseText, "wblprom.gif", ItemPool.LP_ROM_BURNER)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.LP_ROM_BURNER);
    } else if (findImage(responseText, "wbstill.gif", ItemPool.HIGH_EFFICIENCY_STILL)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.HIGH_EFFICIENCY_STILL);
    } else if (findImage(responseText, "wbanvil.gif", ItemPool.AUTO_ANVIL)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.AUTO_ANVIL);
    } else if (findImage(responseText, "wbdrillpress.gif", ItemPool.JACKHAMMER_DRILL_PRESS)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.JACKHAMMER_DRILL_PRESS);
    } else if (findImage(responseText, "snowmachine.gif", ItemPool.SNOW_MACHINE)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.SNOW_MACHINE);
    } else if (findImage(responseText, "spinningwheel.gif", ItemPool.SPINNING_WHEEL)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.SPINNING_WHEEL);
    } else if (findImage(responseText, "genelab.gif", ItemPool.DNA_LAB)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.DNA_LAB);
      Matcher dnaMatcher = DNA_PATTERN.matcher(responseText);
      if (dnaMatcher.find()) {
        Preferences.setString("dnaSyringe", dnaMatcher.group(1));
      } else if (responseText.contains("DNA extraction syringe is currently empty")) {
        Preferences.setString("dnaSyringe", "");
      }
      if (responseText.contains("lab needs to reorder the supplies")) {
        Preferences.setInteger("_dnaPotionsMade", 3);
      }
      if (responseText.contains("horrible abomination once today")) {
        Preferences.setBoolean("_dnaHybrid", true);
      }
    }
    // Mayo Clinic redirects to Store, so handle in NPCPurchaseRequest
    else if (findImage(responseText, "asdongarage.gif", ItemPool.ASDON_MARTIN)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.ASDON_MARTIN);
      Matcher fuelMatcher = FUEL_PATTERN_1.matcher(responseText);
      if (fuelMatcher.find()) {
        asdonMartinFuel = StringUtilities.parseInt(fuelMatcher.group(1));
      }
    } else if (findImage(responseText, "horadricoven.gif", ItemPool.DIABOLIC_PIZZA_CUBE)) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.DIABOLIC_PIZZA_CUBE);
    } else if (findImage(responseText, "cmcabinet.gif", ItemPool.COLD_MEDICINE_CABINET)
        || responseText.contains("Looks like the doctors are out for the day.")) {
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.COLD_MEDICINE_CABINET);
      // Cold Medicine Cabinet usually redirects to choice.php, so this is also handled in
      // ChoiceManager
      Matcher cabinetMatcher = COLD_MEDICINE_CABINET_PATTERN.matcher(responseText);
      if (cabinetMatcher.find()) {
        int turns = StringUtilities.parseInt(cabinetMatcher.group(1));
        int remaining = StringUtilities.parseInt(cabinetMatcher.group(2));
        Preferences.setInteger("_nextColdMedicineConsult", KoLCharacter.getTurnsPlayed() + turns);
        Preferences.setInteger("_coldMedicineConsults", 5 - remaining);
      } else {
        Preferences.setInteger("_coldMedicineConsults", 5);
      }
    }
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId) {
    return CampgroundRequest.findImage(responseText, filename, itemId, 1);
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId, int count) {
    if (!responseText.contains(filename)) {
      return false;
    }

    CampgroundRequest.setCampgroundItem(itemId, count);

    return true;
  }

  private static boolean findImage(
      final String responseText,
      final String filename,
      final int itemId1,
      int count1,
      final int itemId2,
      int count2) {
    if (!responseText.contains(filename)) {
      return false;
    }

    CampgroundRequest.setCampgroundItem(itemId1, count1);
    CampgroundRequest.setCampgroundItem(itemId2, count2);

    return true;
  }

  private static boolean findImage(
      final String responseText, final String filename, AdventureResult item) {
    if (!responseText.contains(filename)) {
      return false;
    }

    CampgroundRequest.setCampgroundItem(item);

    return true;
  }

  public static AdventureResult getCurrentDwelling() {
    return currentDwelling == null ? BIG_ROCK : currentDwelling;
  }

  public static int getCurrentDwellingLevel() {
    return currentDwellingLevel;
  }

  public static void setCurrentDwelling(int itemId) {
    CampgroundRequest.currentDwelling = ItemPool.get(itemId, 1);
    CampgroundRequest.currentDwellingLevel = CampgroundRequest.dwellingLevel(itemId);
  }

  public static void destroyFurnishings() {
    CampgroundRequest.setCurrentBed(null);
    for (int itemId : CampgroundRequest.transientFurnishings) {
      CampgroundRequest.removeCampgroundItem(ItemPool.get(itemId, 1));
    }
  }

  public static AdventureResult getCurrentBed() {
    return CampgroundRequest.currentBed;
  }

  public static void setCurrentBed(AdventureResult bed) {
    if (CampgroundRequest.currentBed != null) {
      CampgroundRequest.removeCampgroundItem(CampgroundRequest.currentBed);
    }
    CampgroundRequest.currentBed = bed;
  }

  public static AdventureResult getCurrentWorkshedItem() {
    return currentWorkshedItem;
  }

  public static void setCurrentWorkshedItem(int itemId) {
    AdventureResult workshedItem = ItemPool.get(itemId, 1);
    CampgroundRequest.setCurrentWorkshedItem(workshedItem);
  }

  public static void setCurrentWorkshedItem(AdventureResult workshedItem) {
    if (workshedItem.equals(CampgroundRequest.getCurrentWorkshedItem())) {
      return;
    }

    if (CampgroundRequest.getCurrentWorkshedItem() != null) {
      CampgroundRequest.removeCampgroundItem(CampgroundRequest.getCurrentWorkshedItem());
    }
    CampgroundRequest.currentWorkshedItem = workshedItem;
    CampgroundRequest.setCampgroundItem(workshedItem);
    if (workshedItem.getItemId() == ItemPool.MAYO_CLINIC) {
      ConcoctionDatabase.refreshConcoctions();
    }
  }

  /**
   * Remove the current workshed item from the campground.
   *
   * <p>Only for use in tests: the only way to remove your workshed is by ascending, and in that
   * case `reset` works better.
   */
  public static void resetCurrentWorkshedItem() {
    CampgroundRequest.currentWorkshedItem = null;
    CampgroundRequest.removeCampgroundItem(CampgroundRequest.getCurrentWorkshedItem());
  }

  public static int dwellingLevel(final int itemId) {
    return switch (itemId) {
      case ItemPool.NEWBIESPORT_TENT -> 1;
      case ItemPool.BARSKIN_TENT -> 2;
      case ItemPool.COTTAGE -> 3;
      case ItemPool.BRICKO_PYRAMID -> 4;
      case ItemPool.HOUSE -> 5;
      case ItemPool.SANDCASTLE -> 6;
      case ItemPool.GINORMOUS_PUMPKIN -> 7;
      case ItemPool.TWIG_HOUSE -> 8;
      case ItemPool.GINGERBREAD_HOUSE -> 9;
      case ItemPool.HOBO_FORTRESS -> 10;
      case ItemPool.GIANT_FARADAY_CAGE -> 11;
      case ItemPool.SNOW_FORT -> 12;
      case ItemPool.ELEVENT -> 13;
      case ItemPool.RESIDENCE_CUBE -> 14;
      case ItemPool.GIANT_PILGRIM_HAT -> 15;
      case ItemPool.HOUSE_SIZED_MUSHROOM -> 16;
      default -> 0;
    };
  }

  public static boolean isBedding(final int itemId) {
    return switch (itemId) {
      case ItemPool.BEANBAG_CHAIR,
          ItemPool.GAUZE_HAMMOCK,
          ItemPool.LAZYBONES_RECLINER,
          ItemPool.SLEEPING_STOCKING,
          ItemPool.HOT_BEDDING,
          ItemPool.COLD_BEDDING,
          ItemPool.STENCH_BEDDING,
          ItemPool.SPOOKY_BEDDING,
          ItemPool.SLEAZE_BEDDING,
          ItemPool.SALTWATERBED,
          ItemPool.SPIRIT_BED -> true;
      default -> false;
    };
  }

  public static boolean isWorkshedItem(final int itemId) {
    return CampgroundRequest.workshedItems.contains(itemId);
  }

  public static int getFuel() {
    return CampgroundRequest.asdonMartinFuel;
  }

  public static void setFuel(int fuel) {
    CampgroundRequest.asdonMartinFuel = fuel;
  }

  public static void useFuel(final int fuel) {
    CampgroundRequest.asdonMartinFuel -= fuel;
  }

  private record Book(String name, String skill) {}

  private static final Book[] BOOKS = {
    new Book("Tome of Snowcone Summoning", "Summon Snowcones"),
    new Book("Tome of Sticker Summoning", "Summon Stickers"),
    new Book("Tome of Sugar Shummoning", "Summon Sugar Sheets"),
    new Book("Tome of Clip Art", "Summon Clip Art"),
    new Book("Tome of Rad Libs", "Summon Rad Libs"),
    new Book("The Smith's Tome", "Summon Smithsness"),
    // The bookshelf currently says:
    // "McPhee's Grimoire of Hilarious Item Summoning"
    // gives access to "Summon Hilarious Items".
    //
    // The item is currently named:
    // "McPhee's Grimoire of Hilarious Object Summoning"
    // and gives access to "Summon Hilarious Objects".
    new Book("McPhee's Grimoire", "Summon Hilarious Objects"),
    new Book("Sp'n-Zor's Grimoire", "Summon Tasteful Items"),
    new Book("Sorcerers of the Shore Grimoire", "Summon Alice's Army Cards"),
    new Book("Thinknerd Grimoire", "Summon Geeky Gifts"),
    new Book("Libram of Candy Heart Summoning", "Summon Candy Heart"),
    new Book("Libram of Divine Favors", "Summon Party Favor"),
    new Book("Libram of Love Songs", "Summon Love Song"),
    new Book("Libram of BRICKOs", "Summon BRICKOs"),
    new Book("Gygaxian Libram", "Summon Dice"),
    new Book("Libram of Resolutions", "Summon Resolutions"),
    new Book("Libram of Pulled Taffy", "Summon Taffy"),
    new Book("Confiscator's Grimoire", "Summon Confiscated Things"),
  };

  private static void parseBookTitles(final String responseText) {
    if ((KoLCharacter.inBadMoon()
            || KoLCharacter.inAxecore()
            || KoLCharacter.inZombiecore()
            || KoLCharacter.isSneakyPete()
            || KoLCharacter.inNuclearAutumn()
            || KoLCharacter.inRobocore())
        && !KoLCharacter.kingLiberated()) {
      // You can't use Mr. Skills in Bad Moon
      // You can't use Mr. Skills as an Avatar of Boris or Sneaky Pete
      // You can't use Mr. Skills as a Zombie Master or in Nuclear Autumn
      return;
    }

    String libram = null;
    for (Book b : BOOKS) {
      String book = b.name;
      if (responseText.contains(book)) {
        String skill = b.skill;
        KoLCharacter.addAvailableSkill(skill, true);
        if (book.contains("Libram")) {
          libram = skill;
        }
      }
    }

    if (libram != null) {
      Matcher matcher = CampgroundRequest.LIBRAM_PATTERN.matcher(responseText);
      if (matcher.find()) {
        int cost = StringUtilities.parseInt(matcher.group(2));
        SkillDatabase.setLibramSkillCasts(cost);
      }
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("campground.php")) {
      return false;
    }

    String action = Objects.requireNonNullElse(GenericRequest.getAction(urlString), "");
    String preaction = Objects.requireNonNullElse(GenericRequest.getPreaction(urlString), "");
    if (action.isEmpty() && preaction.isEmpty()) {
      // Simple visit. Nothing to log.
      return true;
    }

    // Dispatch campground requests to other classes

    if (action.endsWith("elvibratoportal")) {
      return PortalRequest.registerRequest(urlString);
    }

    if (action.startsWith("telescope")) {
      return TelescopeRequest.registerRequest(urlString);
    }

    // campground.php?pwd&action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03
    // 01 = DONUT
    // 02 = BOMB
    // 03 = KITTEN
    // 04 = WINE
    // 05 = CHEESE
    // 06 = LIGHT BULB
    // 07 = SNOWFLAKE
    // 08 = SKULL
    // 09 = CLOCK
    // 10 = HAMMER

    if (preaction.startsWith("summon") || preaction.equals("combinecliparts")) {
      // Detect a redirection to campground.php from
      // skills.php. The first one was already logged.
      if (urlString.contains("skilluse=1")) {
        return true;
      }
      return UseSkillRequest.registerRequest(urlString);
    }

    // Dispatch campground requests from this class

    if (action.equals("inspectdwelling")
        || action.equals("inspectkitchen")
        || action.equals("workshed")) {
      // Nothing to log.
      return true;
    }

    String message = null;

    switch (action) {
      case "garden" -> message = "Harvesting your garden";
      case "spinningwheel" -> message = "Spinning Meat from air";
      case "dnapotion" -> message = "Making a Gene Tonic";
      case "dnainject" -> message = "Hybridizing yourself";
      case "rest" -> message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your dwelling";
      case "witchess" -> {
        KoLAdventure.lastVisitedLocation = null;
        KoLAdventure.lastLocationName = null;
        KoLAdventure.lastLocationURL = urlString;
        KoLAdventure.setLastAdventure("None");
        KoLAdventure.setNextAdventure("None");
        message = "[" + KoLAdventure.getAdventureCount() + "] Your Witchess Set";
      }
      case "fuelconvertor" -> {
        Matcher fuelMatcher = FUEL_PATTERN_3.matcher(urlString);
        if (fuelMatcher.find()) {
          int qty = StringUtilities.parseInt(fuelMatcher.group(1));
          int itemId = StringUtilities.parseInt(fuelMatcher.group(2));
          if (qty > 1) {
            message = "Converting " + qty + " " + ItemDatabase.getPluralName(itemId) + " into Fuel";
          } else {
            message = "Converting " + ItemDatabase.getItemName(itemId) + " into Fuel";
          }
        }
      }
      default -> {
        // Unknown action.
        return false;
      }
    }

    RequestLogger.printLine("");
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);
    return true;
  }
}
