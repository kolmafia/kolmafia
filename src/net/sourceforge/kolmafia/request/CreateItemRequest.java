package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CreateItemRequest extends GenericRequest implements Comparable<CreateItemRequest> {
  public static final GenericRequest REDIRECT_REQUEST =
      new GenericRequest("inventory.php?action=message");

  public static final Pattern ITEMID_PATTERN = Pattern.compile("item\\d?=(\\d+)");
  private static final Pattern QUANTITY_PATTERN = Pattern.compile("(quantity|qty)=(\\d+)");
  public static final Pattern TARGET_PATTERN = Pattern.compile("target=(\\d+)");
  public static final Pattern MODE_PATTERN = Pattern.compile("mode=([^&]+)");
  public static final Pattern CRAFT_PATTERN_1 = Pattern.compile("[\\&\\?](?:a|b)=(\\d+)");
  public static final Pattern CRAFT_PATTERN_2 = Pattern.compile("steps\\[\\]=(\\d+),(\\d+)");

  public static final Pattern CRAFT_COMMENT_PATTERN =
      Pattern.compile("<!-- ?cr:(\\d+)x(-?\\d+),(-?\\d+)=(\\d+) ?-->");
  // 1=quantity, 2,3=items used, 4=result (redundant)
  public static final Pattern DISCOVERY_PATTERN = Pattern.compile("descitem\\((\\d+)\\);");
  public static final Pattern JACKHAMMER_PATTERN =
      Pattern.compile("jackhammer lets you finish your smithing in record time");
  public static final Pattern AUTO_ANVIL_PATTERN =
      Pattern.compile("auto-anvil handles some of the smithing");
  public static final Pattern THORS_PLIERS_PATTERN =
      Pattern.compile("use Thor's Pliers to do the job super fast");
  public static final Pattern RAPID_PROTOTYPING_PATTERN =
      Pattern.compile(
          "That rapid prototyping programming you downloaded is really paying dividends");
  public static final Pattern CORNER_CUTTER_PATTERN =
      Pattern.compile("You really crafted that item the LyleCo way");
  public static final Pattern HOMEBODYL_PATTERN =
      Pattern.compile("You are so relaxed that your crafting takes hardly any time at all!");

  public static final AdventureResult TENDER_HAMMER = ItemPool.get(ItemPool.TENDER_HAMMER, 1);
  public static final AdventureResult GRIMACITE_HAMMER = ItemPool.get(ItemPool.GRIMACITE_HAMMER, 1);

  public Concoction concoction;
  public AdventureResult createdItem;

  private final String name;
  private final int itemId;
  private CraftingType mixingMethod;
  private final EnumSet<CraftingRequirements> requirements;

  protected int beforeQuantity;
  private int yield;

  protected int quantityNeeded, quantityPossible, quantityPullable;

  /**
   * Constructs a new <code>CreateItemRequest</code> with nothing known other than the form to use.
   * This is used by descendant classes to avoid weird type-casting problems, as it assumes that
   * there is no known way for the item to be created.
   *
   * @param formSource The form to be used for the item creation
   * @param conc The Concoction for the item to be handled
   */
  public CreateItemRequest(final String formSource, final Concoction conc) {
    super(formSource);

    this.concoction = conc;
    this.itemId = conc.getItemId();
    this.name = conc.getName();
    this.mixingMethod = CraftingType.SUBCLASS;
    this.requirements = conc.getRequirements();
    // is this right?
    this.calculateYield();
  }

  private CreateItemRequest(final Concoction conc) {
    this("", conc);

    this.mixingMethod = conc.getMixingMethod();
  }

  private void calculateYield() {
    this.yield = this.concoction.getYield();
    this.createdItem = this.concoction.getItem().getInstance(this.yield);
  }

  public int getYield() {
    return this.yield;
  }

  @Override
  public void reconstructFields() {
    String formSource = "craft.php";
    String action = "craft";
    String mode = null;

    switch (this.mixingMethod) {
      case COMBINE:
      case ACOMBINE:
      case JEWELRY:
        mode = "combine";
        break;

      case MIX:
      case MIX_FANCY:
        mode = "cocktail";
        break;

      case COOK:
      case COOK_FANCY:
        mode = "cook";
        break;

      case SMITH:
      case SSMITH:
        mode = "smith";
        break;

      case ROLLING_PIN:
        formSource = "inv_use.php";
        break;

      case MALUS:
        formSource = "guild.php";
        action = "malussmash";
        break;

      default:
        // For everything else, simply force the datastring to
        // be rebuilt before the request is submitted
        this.setDataChanged();
        return;
    }

    this.constructURLString(formSource);
    this.addFormField("action", action);

    if (mode != null) {
      this.addFormField("mode", mode);
      this.addFormField("ajax", "1");
    }
  }

  public static final CreateItemRequest getInstance(final int itemId) {
    return CreateItemRequest.getInstance(ConcoctionPool.get(itemId), true);
  }

  public static final CreateItemRequest getInstance(final int itemId, final boolean rNINP) {
    return CreateItemRequest.getInstance(ConcoctionPool.get(itemId), rNINP);
  }

  public static final CreateItemRequest getInstance(final AdventureResult item) {
    return CreateItemRequest.getInstance(ConcoctionPool.get(item), true);
  }

  public static final CreateItemRequest getInstance(
      final AdventureResult item, final boolean rNINP) {
    return CreateItemRequest.getInstance(ConcoctionPool.get(item), rNINP);
  }

  public static final CreateItemRequest getInstance(
      final Concoction conc, final boolean returnNullIfNotPermitted) {
    if (conc == null) {
      ConcoctionDatabase.excuse = null;
      return null;
    }

    CreateItemRequest instance = conc.getRequest();

    if (instance == null) {
      ConcoctionDatabase.excuse = null;
      return null;
    }

    if (instance instanceof CombineMeatRequest) {
      return instance;
    }

    // If the item creation process is not permitted, then return
    // null to indicate that it is not possible to create the item.

    if (returnNullIfNotPermitted) {
      if (Preferences.getBoolean("unknownRecipe" + conc.getItemId())) {
        ConcoctionDatabase.excuse =
            "That item requires a recipe.  If you've already learned it, visit the crafting discoveries page in the relay browser to let KoLmafia know about it.";
        return null;
      }

      Concoction concoction = instance.concoction;
      if (!ConcoctionDatabase.checkPermittedMethod(
          concoction)) { // checkPermittedMethod set the excuse
        return null;
      }
    }

    return instance;
  }

  // This API should only be called by Concoction.getRequest(), which
  // is responsible for caching the instances.
  public static final CreateItemRequest constructInstance(final Concoction conc) {
    if (conc == null) {
      return null;
    }

    int itemId = conc.getItemId();

    if (CombineMeatRequest.getCost(itemId) > 0) {
      return new CombineMeatRequest(conc);
    }

    // Return the appropriate subclass of item which will be
    // created.

    switch (conc.getMixingMethod()) {
      case NOCREATE:
        return null;

      case STILL:
        return new StillRequest(conc);

      case STARCHART:
        return new StarChartRequest(conc);

      case SUGAR_FOLDING:
        return new SugarSheetRequest(conc);

      case PIXEL:
        return new PixelRequest(conc);

      case GNOME_TINKER:
        return new GnomeTinkerRequest(conc);

      case STAFF:
        return new ChefStaffRequest(conc);

      case SUSHI:
        return new SushiRequest(conc);

      case SINGLE_USE:
        return new SingleUseRequest(conc);

      case MULTI_USE:
        return new MultiUseRequest(conc);

      case SEWER:
        return new SewerRequest(conc);

      case CRIMBO05:
        return new Crimbo05Request(conc);

      case CRIMBO06:
        return new Crimbo06Request(conc);

      case CRIMBO07:
        return new Crimbo07Request(conc);

      case CRIMBO12:
        return new Crimbo12Request(conc);

      case CRIMBO16:
        return new Crimbo16Request(conc);

      case PHINEAS:
        return new PhineasRequest(conc);

      case CLIPART:
        return new ClipArtRequest(conc);

      case JARLS:
        return new JarlsbergRequest(conc);

      case GRANDMA:
        return new GrandmaRequest(conc);

      case KRINGLE:
        return new KringleRequest(conc);

      case CHEMCLASS:
      case ARTCLASS:
      case SHOPCLASS:
        return new KOLHSRequest(conc);

      case BEER:
        return new BeerGardenRequest(conc);

      case JUNK:
        return new JunkMagazineRequest(conc);

      case WINTER:
        return new WinterGardenRequest(conc);

      case RUMPLE:
        return new RumpleRequest(conc);

      case FIVE_D:
        return new FiveDPrinterRequest(conc);

      case VYKEA:
        return new VYKEARequest(conc);

      case DUTYFREE:
        return new AirportRequest(conc);

      case FLOUNDRY:
        return new FloundryRequest(conc);

      case TERMINAL:
        return new TerminalExtrudeRequest(conc);

      case BARREL:
        return new BarrelShrineRequest(conc);

      case WAX:
        return new WaxGlobRequest(conc);

      case SPANT:
        return new SpantRequest(conc);

      case XO:
        return new XOShopRequest(conc);

      case SLIEMCE:
        return new SliemceRequest(conc);

      case NEWSPAPER:
        return new BurningNewspaperRequest(conc);

      case METEOROID:
        return new MeteoroidRequest(conc);

      case SAUSAGE_O_MATIC:
        return new SausageOMaticRequest(conc);

      case SPACEGATE:
        return new SpacegateEquipmentRequest(conc);

      case FANTASY_REALM:
        return new FantasyRealmRequest(conc);

      default:
        return new CreateItemRequest(conc);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof CreateItemRequest) {
      return this.compareTo((CreateItemRequest) o) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.name != null ? this.name.toLowerCase().hashCode() : 0;
  }

  @Override
  public int compareTo(final CreateItemRequest o) {
    return o == null ? -1 : this.getName().compareToIgnoreCase(o.getName());
  }

  /**
   * Runs the item creation request. Note that if another item needs to be created for the request
   * to succeed, this method will fail.
   */
  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (!KoLmafia.permitsContinue() || this.quantityNeeded <= 0) {
      return;
    }

    // Acquire all needed ingredients

    if (this.mixingMethod != CraftingType.SUBCLASS
        && this.mixingMethod != CraftingType.ROLLING_PIN
        && !this.makeIngredients()) {
      return;
    }

    // Save outfit in case we need to equip something - like a Grimacite hammer

    try (Checkpoint checkpoint = new Checkpoint()) {
      this.createItemLoop();
    }
  }

  private void createItemLoop() {
    while (this.quantityNeeded > 0 && KoLmafia.permitsContinue()) {
      if (!this.autoRepairBoxServant()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Auto-repair was unsuccessful.");
        break;
      }

      this.reconstructFields();

      this.beforeQuantity = this.createdItem.getCount(KoLConstants.inventory);

      switch (this.mixingMethod) {
        case SUBCLASS:
          super.run();
          if (this.responseCode == 302 && this.redirectLocation.startsWith("inventory")) {
            CreateItemRequest.REDIRECT_REQUEST.constructURLString(this.redirectLocation).run();
          }
          break;

        case ROLLING_PIN:
          this.makeDough();
          break;

        case COINMASTER:
          this.makeCoinmasterPurchase();
          break;

        default:
          this.combineItems();
          break;
      }

      // Certain creations are used immediately.

      if (this.noCreation()) {
        break;
      }

      // Figure out how many items were created

      int createdQuantity = this.createdItem.getCount(KoLConstants.inventory) - this.beforeQuantity;

      // If we created none, log error and stop iterating

      if (createdQuantity == 0) {
        // If the subclass didn't detect the failure, do so here.

        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        }

        break;
      }

      KoLmafia.updateDisplay(
          "Successfully created " + this.getName() + " (" + createdQuantity + ")");
      this.quantityNeeded -= createdQuantity;
    }
  }

  public boolean noCreation() {
    return false;
  }

  private static final AdventureResult[][] DOUGH_DATA = {
    // input, tool, output
    {
      ItemPool.get(ItemPool.DOUGH, 1),
      ItemPool.get(ItemPool.ROLLING_PIN, 1),
      ItemPool.get(ItemPool.FLAT_DOUGH, 1),
    },
    {
      ItemPool.get(ItemPool.FLAT_DOUGH, 1),
      ItemPool.get(ItemPool.UNROLLING_PIN, 1),
      ItemPool.get(ItemPool.DOUGH, 1),
    },
  };

  public void makeDough() {
    AdventureResult input = null;
    AdventureResult tool = null;
    AdventureResult output = null;

    // Find the array row and load the
    // correct tool/input/output data.

    for (AdventureResult[] row : CreateItemRequest.DOUGH_DATA) {
      output = row[2];
      if (this.itemId == output.getItemId()) {
        tool = row[1];
        input = row[0];
        break;
      }
    }

    if (tool == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Can't deduce correct tool to use.");
      return;
    }

    int needed = this.quantityNeeded;

    // See how many of the ingredient are already available
    int available = InventoryManager.getAccessibleCount(input);

    // See how many of those we should pull into inventory
    int retrieve = Math.min(available, needed);

    // See how many we need to purchase
    int purchase = needed - retrieve;

    // Pull accessible ingredients into inventory
    if (!InventoryManager.retrieveItem(input.getInstance(retrieve))) {
      // This should not happen, but cope.
      purchase = needed - input.getCount(KoLConstants.inventory);
    }

    if (purchase > 0) {
      // If we got here, we don't have all of the ingredient that we need
      // It's always cheaper to buy wads of dough, so just do that.
      // Using makePurchases directly because retrieveItem does not handle this recursion
      // gracefully.

      AdventureResult dough = ItemPool.get(ItemPool.DOUGH, purchase);
      List<PurchaseRequest> results = MallPriceManager.searchNPCs(dough);
      KoLmafia.makePurchases(results, results.toArray(new PurchaseRequest[0]), purchase, false, 50);

      // And if we are making wads of dough, that reduces how many we need

      if (output.getItemId() == ItemPool.DOUGH) {
        needed -= purchase;
      }
    }

    // Purchasing might have satisfied our needs

    if (needed == 0) {
      return;
    }

    // If we don't have the correct tool, and the person wishes to
    // create more than 10 dough, then notify the person that they
    // should purchase a tool before continuing.

    if (needed > 10 && !InventoryManager.retrieveItem(tool)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Please purchase a " + tool.getName() + " first.");
      return;
    }

    // If we have the correct tool, use it to create the dough.

    if (InventoryManager.hasItem(tool) && InventoryManager.retrieveItem(tool)) {
      KoLmafia.updateDisplay("Using " + tool.getName() + "...");
      UseItemRequest.getInstance(tool).run();
      return;
    }

    // Without the right tool, we must knead the dough by hand.

    String name = output.getName();
    UseItemRequest request = UseItemRequest.getInstance(input);

    for (int i = 1; KoLmafia.permitsContinue() && i <= needed; ++i) {
      KoLmafia.updateDisplay("Creating " + name + " (" + i + " of " + needed + ")...");
      request.run();
    }
  }

  public void makeCoinmasterPurchase() {
    PurchaseRequest request = this.concoction.getPurchaseRequest();
    if (request == null) {
      return;
    }
    request.setLimit(this.quantityNeeded);
    request.run();
  }

  /** Helper routine which actually does the item combination. */
  private void combineItems() {
    String path = this.getPath();
    String quantityField = "quantity";

    this.calculateYield();
    AdventureResult[] ingredients =
        ConcoctionDatabase.getIngredients(this.concoction.getIngredients());

    if (ingredients.length == 1) {
      if (this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Ran out of adventures.");
        return;
      }

      // If there is only one ingredient, then it probably
      // only needs a "whichitem" field added to the request.

      this.addFormField("whichitem", String.valueOf(ingredients[0].getItemId()));
    } else if (path.equals("craft.php")) {
      quantityField = "qty";

      this.addFormField("a", String.valueOf(ingredients[0].getItemId()));
      this.addFormField("b", String.valueOf(ingredients[1].getItemId()));
    } else {
      for (int i = 0; i < ingredients.length; ++i) {
        this.addFormField("item" + (i + 1), String.valueOf(ingredients[i].getItemId()));
      }
    }

    int quantity = (this.quantityNeeded + this.yield - 1) / this.yield;
    this.addFormField(quantityField, String.valueOf(quantity));

    KoLmafia.updateDisplay("Creating " + this.name + " (" + this.quantityNeeded + ")...");
    super.run();
  }

  @Override
  public void processResults() {
    if (CreateItemRequest.parseGuildCreation(this.getURLString(), this.responseText)) {
      return;
    }

    CreateItemRequest.parseCrafting(this.getURLString(), this.responseText);

    // Check to see if box-servant was overworked and exploded.

    if (this.responseText.contains("Smoke")) {
      KoLmafia.updateDisplay("Your box servant has escaped!");
    }
  }

  public static int parseCrafting(final String location, final String responseText) {
    if (!location.startsWith("craft.php")) {
      return 0;
    }

    if (location.contains("action=pulverize")) {
      return PulverizeRequest.parseResponse(location, responseText);
    }

    Matcher m = MODE_PATTERN.matcher(location);
    String mode = m.find() ? m.group(1) : "";
    if (mode.equals("discoveries")) {
      m = DISCOVERY_PATTERN.matcher(responseText);
      while (m.find()) {
        int id = ItemDatabase.getItemIdFromDescription(m.group(1));
        String pref = "unknownRecipe" + id;
        if (id > 0 && Preferences.getBoolean(pref)) {
          KoLmafia.updateDisplay("You know the recipe for " + ItemDatabase.getItemName(id));
          Preferences.setBoolean(pref, false);
          ConcoctionDatabase.setRefreshNeeded(true);
        }
      }

      return 0;
    }

    boolean paste =
        mode.equals("combine") && (!KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore());
    int created = 0;

    m = CRAFT_COMMENT_PATTERN.matcher(responseText);
    if (!m.find()) {
      return 0;
    }

    // Multi-step crafting makes it harder to tell how many
    // free crafts were used, so we look at the text following
    // each craft individually for the free crafting texts.
    List<Integer[]> craftComments = new ArrayList<Integer[]>();

    do {
      // item ids can be -1, if crafting uses a single item
      int qty = StringUtilities.parseInt(m.group(1));
      int item1 = StringUtilities.parseInt(m.group(2));
      int item2 = StringUtilities.parseInt(m.group(3));
      if (item1 > 0) {
        ResultProcessor.processItem(item1, -qty);
      }
      if (item2 > 0) {
        ResultProcessor.processItem(item2, -qty);
      }
      if (paste) {
        ResultProcessor.processItem(ItemPool.MEAT_PASTE, -qty);
      }
      if (item1 < 0) {
        RequestLogger.updateSessionLog("Crafting used " + qty + ItemDatabase.getItemName(item2));
      } else if (item2 < 0) {
        RequestLogger.updateSessionLog("Crafting used " + qty + ItemDatabase.getItemName(item1));
      } else {
        RequestLogger.updateSessionLog(
            "Crafting used "
                + qty
                + " each of "
                + ItemDatabase.getItemName(item1)
                + " and "
                + ItemDatabase.getItemName(item2));
      }
      String pref = "unknownRecipe" + m.group(4);
      if (Preferences.getBoolean(pref)) {
        KoLmafia.updateDisplay("(You apparently already knew this recipe.)");
        Preferences.setBoolean(pref, false);
        ConcoctionDatabase.setRefreshNeeded(true);
      }

      if (ItemDatabase.isFancyItem(item1) || ItemDatabase.isFancyItem(item2)) {
        if (mode.equals("cook") && KoLCharacter.hasChef()) {
          Preferences.increment("chefTurnsUsed", qty);
        } else if (mode.equals("cocktail") && KoLCharacter.hasBartender()) {
          Preferences.increment("bartenderTurnsUsed", qty);
        }
      }

      craftComments.add(new Integer[] {m.start(), qty});
    } while (m.find());

    // Parse for the end of the table we currently are in
    int craftStart = craftComments.get(0)[0];
    int craftEnd = m.regionEnd();
    Matcher tableStartMatcher =
        Pattern.compile("<table").matcher(responseText).region(craftStart, craftEnd);
    Matcher tableEndMatcher =
        Pattern.compile("</table>").matcher(responseText).region(craftStart, craftEnd);
    while (tableEndMatcher.find()) {
      // Make sure that wasn't just a table inside a table
      if (!tableStartMatcher.find() || tableStartMatcher.start() > tableEndMatcher.start()) {
        craftEnd = tableEndMatcher.start();
        break;
      }
    }
    craftComments.add(new Integer[] {craftEnd});

    if (responseText.contains("Smoke")) {
      String servant = "servant";
      if (mode.equals("cook")) {
        servant = "chef";
        KoLCharacter.setChef(false);
      } else if (mode.equals("cocktail")) {
        servant = "bartender";
        KoLCharacter.setBartender(false);
      }
      RequestLogger.updateSessionLog("Your " + servant + " blew up");
    }

    for (int i = 0; i + 1 < craftComments.size(); ++i) {
      String craftSection =
          responseText.substring(craftComments.get(i)[0], craftComments.get(i + 1)[0]);
      created = craftComments.get(i)[1];

      int turnsSaved = 0;
      Matcher freeTurn;

      // Remove from Jackhammer, then Warbear Anvil, then Thor's Pliers
      if (mode.equals("smith")) {
        freeTurn = JACKHAMMER_PATTERN.matcher(craftSection);
        if (freeTurn.find()) {
          int jackhammerTurnsSaved =
              Math.min(
                  3 - Preferences.getInteger("_legionJackhammerCrafting"), created - turnsSaved);
          Preferences.increment("_legionJackhammerCrafting", created, 3, false);
          turnsSaved += jackhammerTurnsSaved;
        }
        freeTurn = AUTO_ANVIL_PATTERN.matcher(craftSection);
        if (freeTurn.find()) {
          int autoAnvilTurnsSaved =
              Math.min(
                  5 - Preferences.getInteger("_warbearAutoAnvilCrafting"), created - turnsSaved);
          Preferences.increment("_warbearAutoAnvilCrafting", created - turnsSaved, 5, false);
          turnsSaved += autoAnvilTurnsSaved;
        }
        freeTurn = THORS_PLIERS_PATTERN.matcher(craftSection);
        if (freeTurn.find()) {
          int thorsPliersTurnsSaved =
              Math.min(10 - Preferences.getInteger("_thorsPliersCrafting"), created - turnsSaved);
          Preferences.increment("_thorsPliersCrafting", created - turnsSaved, 10, false);
          turnsSaved += thorsPliersTurnsSaved;
        }
      }

      // Remove from Homebodyl, Rapid Prototyping, then Corner Cutter
      freeTurn = HOMEBODYL_PATTERN.matcher(craftSection);
      if (freeTurn.find()) {
        int homebodylTurnsSaved =
            Math.min(Preferences.getInteger("homebodylCharges"), created - turnsSaved);
        Preferences.decrement("homebodylCharges", created - turnsSaved, 0);
        turnsSaved += homebodylTurnsSaved;
      }
      freeTurn = RAPID_PROTOTYPING_PATTERN.matcher(craftSection);
      if (freeTurn.find()) {
        int rapidPrototypingTurnsSaved =
            Math.min(5 - Preferences.getInteger("_rapidPrototypingUsed"), created - turnsSaved);
        Preferences.increment("_rapidPrototypingUsed", created - turnsSaved, 5, false);
        turnsSaved += rapidPrototypingTurnsSaved;
      }
      freeTurn = CORNER_CUTTER_PATTERN.matcher(craftSection);
      if (freeTurn.find()) {
        int expertCornerCutterTurnsSaved =
            Math.min(5 - Preferences.getInteger("_expertCornerCutterUsed"), created - turnsSaved);
        Preferences.increment("_expertCornerCutterUsed", created - turnsSaved, 5, false);
        turnsSaved += expertCornerCutterTurnsSaved;
      }
    }

    return created;
  }

  public static boolean parseGuildCreation(final String urlString, final String responseText) {
    if (!urlString.startsWith("guild.php")) {
      return false;
    }

    // If nothing was created, don't deal with ingredients

    if (!responseText.contains("You acquire")) {
      return true;
    }

    int multiplier = 1;

    // Using the Malus uses 5 ingredients at a time
    if (urlString.contains("action=malussmash")) {
      multiplier = 5;
    } else {
      return true;
    }

    AdventureResult[] ingredients = CreateItemRequest.findIngredients(urlString);
    int quantity = CreateItemRequest.getQuantity(urlString, ingredients, multiplier);

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult item = ingredients[i];
      ResultProcessor.processItem(item.getItemId(), -quantity);
    }

    return true;
  }

  private boolean autoRepairBoxServant() {
    return CreateItemRequest.autoRepairBoxServant(this.mixingMethod, this.requirements);
  }

  public static boolean autoRepairBoxServant(
      final CraftingType mixingMethod, final EnumSet<CraftingRequirements> requirements) {
    if (KoLmafia.refusesContinue()) {
      return false;
    }

    if (requirements.contains(CraftingRequirements.HAMMER)
        && !InventoryManager.retrieveItem(CreateItemRequest.TENDER_HAMMER)) {
      return false;
    }

    if (requirements.contains(CraftingRequirements.GRIMACITE)) {
      AdventureResult hammer = CreateItemRequest.GRIMACITE_HAMMER;
      int slot = EquipmentManager.WEAPON;

      if (!KoLCharacter.hasEquipped(hammer, slot)
          && EquipmentManager.canEquip(hammer)
          && InventoryManager.retrieveItem(hammer)) {
        (new EquipmentRequest(hammer, slot)).run();
      }

      return KoLCharacter.hasEquipped(hammer, slot);
    }

    // If we are not cooking or mixing, or if we already have the
    // appropriate servant installed, we don't need to repair

    switch (mixingMethod) {
      case COOK_FANCY:

        // We need range installed to cook fancy foods.
        if (!KoLCharacter.hasRange()) {
          // Acquire and use a range
          if (!InventoryManager.retrieveItem(ItemPool.RANGE)) {
            return false;
          }
          UseItemRequest.getInstance(ItemPool.get(ItemPool.RANGE, 1)).run();
        }

        // If we have a chef, fancy cooking is now free
        if (KoLCharacter.hasChef()) {
          return true;
        }
        break;

      case MIX_FANCY:

        // We need a cocktail kit installed to mix fancy
        // drinks.
        if (!KoLCharacter.hasCocktailKit()) {
          // Acquire and use cocktail kit
          if (!InventoryManager.retrieveItem(ItemPool.COCKTAIL_KIT)) {
            return false;
          }
          UseItemRequest.getInstance(ItemPool.get(ItemPool.COCKTAIL_KIT, 1)).run();
        }

        // If we have a bartender, fancy mixing is now free
        if (KoLCharacter.hasBartender() || KoLCharacter.hasSkill("Cocktail Magic")) {
          return true;
        }
        break;

      case SMITH:
        return (KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore())
            || InventoryManager.retrieveItem(CreateItemRequest.TENDER_HAMMER);

      case SSMITH:
        return InventoryManager.retrieveItem(CreateItemRequest.TENDER_HAMMER);

      default:
        return true;
    }

    // In G-Lover, no Box Servants
    if (KoLCharacter.inGLover()) {
      return true;
    }

    boolean autoRepairSuccessful = false;

    // If they want to auto-repair, make sure that the appropriate
    // item is available in their inventory

    switch (mixingMethod) {
      case COOK_FANCY:
        autoRepairSuccessful =
            CreateItemRequest.useBoxServant(ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF);
        break;

      case MIX_FANCY:
        autoRepairSuccessful =
            CreateItemRequest.useBoxServant(ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER);
        break;
    }

    return autoRepairSuccessful && KoLmafia.permitsContinue();
  }

  private static boolean useBoxServant(final int servant, final int clockworkServant) {
    // We have no box servant.

    if (!Preferences.getBoolean("autoRepairBoxServants")) {
      // We don't want to autorepair. It's OK if we don't
      // require one and have turns available to craft.
      return !Preferences.getBoolean("requireBoxServants")
          && (KoLCharacter.getAdventuresLeft() > 0
              || ConcoctionDatabase.getFreeCraftingTurns() > 0);
    }

    // We want to autorepair.

    // First, check to see if a box servant is available
    // for usage, either normally, or through some form
    // of creation.

    int usedServant;

    if (InventoryManager.hasItem(clockworkServant, false)) {
      usedServant = clockworkServant;
    } else if (InventoryManager.hasItem(servant, true)) {
      usedServant = servant;
    } else if (InventoryManager.canUseMall() || InventoryManager.canUseClanStash()) {
      usedServant = servant;
    } else {
      // We can't autorepair. It's still OK if we are willing
      // to cook without a box servant and have turns
      // available to craft.
      return !Preferences.getBoolean("requireBoxServants")
          && (KoLCharacter.getAdventuresLeft() > 0
              || ConcoctionDatabase.getFreeCraftingTurns() > 0);
    }

    // Once you hit this point, you're guaranteed to
    // have the servant in your inventory, so attempt
    // to repair the box servant.

    UseItemRequest.getInstance(ItemPool.get(usedServant, 1)).run();
    return servant == ItemPool.CHEF ? KoLCharacter.hasChef() : KoLCharacter.hasBartender();
  }

  public boolean makeIngredients() {
    KoLmafia.updateDisplay(
        "Verifying ingredients for " + this.name + " (" + this.quantityNeeded + ")...");

    this.calculateYield();
    int yield = this.yield;

    // If this is a combining request, you need meat paste as well.

    if ((this.mixingMethod == CraftingType.COMBINE
            || this.mixingMethod == CraftingType.ACOMBINE
            || this.mixingMethod == CraftingType.JEWELRY)
        && (!KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore())) {
      int pasteNeeded =
          this.concoction.getMeatPasteNeeded(this.quantityNeeded + this.concoction.initial);
      AdventureResult paste = ItemPool.get(ItemPool.MEAT_PASTE, pasteNeeded);

      if (!InventoryManager.retrieveItem(paste)) {
        return false;
      }
    }

    AdventureResult[] ingredients =
        ConcoctionDatabase.getIngredients(this.concoction.getIngredients()).clone();

    // Sort ingredients by their creatability, so that if the overall creation
    // is going to fail, it should do so immediately, without wasted effort.
    Arrays.sort(
        ingredients,
        new Comparator<AdventureResult>() {
          @Override
          public int compare(AdventureResult o1, AdventureResult o2) {
            Concoction left = ConcoctionPool.get(o1);
            if (left == null) return -1;
            Concoction right = ConcoctionPool.get(o2);
            if (right == null) return 1;
            return left.creatable < right.creatable ? -1 : left.creatable < right.creatable ? 1 : 0;
          }
        });

    // Retrieve all the ingredients
    AdventureResult[] retrievals = new AdventureResult[ingredients.length];

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];

      // First, calculate the multiplier that's needed
      // for this ingredient to avoid not making enough
      // intermediate ingredients and getting an error.

      int multiplier = 0;
      for (int j = 0; j < ingredients.length; ++j) {
        if (ingredient.getItemId() == ingredients[j].getItemId()) {
          multiplier += ingredients[j].getCount();
        }
      }

      // Then, make enough of the ingredient in order
      // to proceed with the concoction.

      int quantity = this.quantityNeeded * multiplier;

      if (yield > 1) {
        quantity = (quantity + yield - 1) / yield;
      }

      AdventureResult retrieval = ingredient.getInstance(quantity);
      if (!InventoryManager.retrieveItem(retrieval)) {
        return false;
      }

      retrievals[i] = retrieval;
    }

    // clockwork clockwise dome = clockwork widget + flange + spring
    // clockwork widget = flange + cog + sprocket
    //
    // Creating a clockwork counterclockwise dome retrieves a flange and a spring and recurses
    // Creating a clockwork widget sees that it has a flange, retrieves a cog and a socket,
    //   makes the widget and returns
    // The previously retrieved flange is no longer present for the clockwork counterclockwise dome
    //
    // The issue arises when an ingredient is needed both for this creation and another
    // ingredient  and we no longer have a necessary ingredient in inventory after, supposedly,
    // retrieving all the ingredients

    // Make another pass to get ingredients that were consumed
    // while creating other ingredients.

    for (int i = 0; i < ingredients.length; ++i) {
      if (!InventoryManager.retrieveItem(retrievals[i])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the item Id for the item created by this request.
   *
   * @return The item Id of the item being created
   */
  public int getItemId() {
    return this.itemId;
  }

  /**
   * Returns the name of the item created by this request.
   *
   * @return The name of the item being created
   */
  public String getName() {
    return this.name;
  }

  /** Returns the quantity of items to be created by this request if it were to run right now. */
  public int getQuantityNeeded() {
    return this.quantityNeeded;
  }

  /**
   * Sets the quantity of items to be created by this request. This method is used whenever the
   * original quantity intended by the request changes.
   */
  public void setQuantityNeeded(final int quantityNeeded) {
    this.quantityNeeded = quantityNeeded;
  }

  /** Returns the quantity of items that could be created with available ingredients. */
  public int getQuantityPossible() {
    return this.quantityPossible;
  }

  /** Sets the quantity of items that could be created. This is set by refreshConcoctions. */
  public void setQuantityPossible(final int quantityPossible) {
    this.quantityPossible = quantityPossible;
  }

  /** Returns the quantity of items that could be pulled with the current budget. */
  public int getQuantityPullable() {
    return this.quantityPullable;
  }

  /** Sets the quantity of items that could be pulled. This is set by refreshConcoctions. */
  public void setQuantityPullable(final int quantityPullable) {
    this.quantityPullable = quantityPullable;
  }

  /**
   * Returns the string form of this item creation request. This displays the item name, and the
   * amount that will be created by this request.
   *
   * @return The string form of this request
   */
  @Override
  public String toString() {
    return this.getName() + " (" + this.getQuantityPossible() + ")";
  }

  /**
   * An alternative method to doing adventure calculation is determining how many adventures are
   * used by the given request, and subtract them after the request is done.
   *
   * @return The number of adventures used by this request.
   */
  @Override
  public int getAdventuresUsed() {
    return CreateItemRequest.getAdventuresUsed(this);
  }

  public static int getAdventuresUsed(final GenericRequest request) {
    String urlString = request.getURLString();
    String path = request.getPath();

    CraftingType mixingMethod = CreateItemRequest.findMixingMethod(urlString);

    if (mixingMethod == CraftingType.NOCREATE) {
      return 0;
    }

    int multiplier = CreateItemRequest.getMultiplier(mixingMethod);
    if (multiplier == 0) {
      return 0;
    }

    AdventureResult[] ingredients = CreateItemRequest.findIngredients(urlString);
    int quantity = CreateItemRequest.getQuantity(urlString, ingredients, multiplier);
    return CreateItemRequest.getAdventuresUsed(mixingMethod, quantity);
  }

  private static CraftingType findMixingMethod(final String urlString) {
    if (urlString.startsWith("guild.php")) {
      return CraftingType.NOCREATE;
    }
    Concoction concoction = CreateItemRequest.findConcoction(urlString);
    return concoction == null ? CraftingType.NOCREATE : concoction.getMixingMethod();
  }

  private static Concoction findConcoction(final String urlString) {
    if (urlString.startsWith("craft.php")) {
      if (urlString.contains("target")) {
        Matcher targetMatcher = CreateItemRequest.TARGET_PATTERN.matcher(urlString);
        if (targetMatcher.find()) {
          return null;
        }
        return ConcoctionPool.get(StringUtilities.parseInt(targetMatcher.group(1)));
      }
      return ConcoctionPool.findConcoction(CreateItemRequest.findIngredients(urlString));
    }

    return null;
  }

  private static int getMultiplier(final CraftingType mixingMethod) {
    switch (mixingMethod) {
      case SMITH:
        return (KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore()) ? 0 : 1;

      case SSMITH:
        return 1;

      case COOK_FANCY:
        return KoLCharacter.hasChef() ? 0 : 1;

      case MIX_FANCY:
        return KoLCharacter.hasBartender() ? 0 : 1;

      default:
        return 0;
    }
  }

  private static int getAdventuresUsed(final CraftingType mixingMethod, final int quantityNeeded) {
    switch (mixingMethod) {
      case SMITH:
      case SSMITH:
        return Math.max(
            0,
            (quantityNeeded
                - ConcoctionDatabase.getFreeCraftingTurns()
                - ConcoctionDatabase.getFreeSmithingTurns()
                - ConcoctionDatabase.getFreeSmithJewelTurns()));

      case COOK_FANCY:
      case MIX_FANCY:
        return Math.max(0, (quantityNeeded - ConcoctionDatabase.getFreeCraftingTurns()));

      default:
        return 0;
    }
  }

  public static final boolean registerRequest(final boolean isExternal, final String urlString) {
    // Delegate to subclasses if appropriate

    if (urlString.startsWith("shop.php")) {
      // Let shop.php creation methods register themselves
      return false;
    }

    if (urlString.startsWith("volcanoisland.php")) {
      return PhineasRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("sushi.php")) {
      return SushiRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("crimbo07.php")) {
      return Crimbo07Request.registerRequest(urlString);
    }

    if (urlString.contains("action=makestaff")) {
      return ChefStaffRequest.registerRequest(urlString);
    }

    if (urlString.contains("action=makepaste") || urlString.contains("action=makestuff")) {
      return CombineMeatRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("multiuse.php")) {
      if (MultiUseRequest.registerRequest(urlString)) {
        return true;
      }

      if (SingleUseRequest.registerRequest(urlString)) {
        return true;
      }

      return false;
    }

    if (urlString.startsWith("gnomes.php")) {
      return GnomeTinkerRequest.registerRequest(urlString);
    }

    if (urlString.startsWith("inv_use.php")) {
      if (MultiUseRequest.registerRequest(urlString)) {
        return true;
      }

      if (SingleUseRequest.registerRequest(urlString)) {
        return true;
      }

      Matcher whichMatcher = CreateItemRequest.WHICHITEM_PATTERN.matcher(urlString);
      if (!whichMatcher.find()) {
        return false;
      }

      int tool = StringUtilities.parseInt(whichMatcher.group(1));

      int ingredient = -1;

      switch (tool) {
        case ItemPool.ROLLING_PIN:
          ingredient = ItemPool.DOUGH;
          break;
        case ItemPool.UNROLLING_PIN:
          ingredient = ItemPool.FLAT_DOUGH;
          break;
        default:
          return false;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Use " + ItemDatabase.getDisplayName(tool));

      // *** Should do this after we get response text back
      AdventureResult item = ItemPool.get(ingredient);
      int quantity = item.getCount(KoLConstants.inventory);
      ResultProcessor.processItem(ingredient, 0 - quantity);

      return true;
    }

    // Now that we know it's not a special subclass instance,
    // all we do is parse out the ingredients which were used
    // and then print the attempt to the screen.

    String command = null;
    int multiplier = 1;
    boolean usesTurns = false;

    if (urlString.startsWith("craft.php")) {
      if (urlString.contains("action=pulverize")) {
        return false;
      }

      if (!urlString.contains("action=craft")) {
        return true;
      }

      if (urlString.contains("mode=combine")) {
        command = "Combine";
      } else if (urlString.contains("mode=cocktail")) {
        command = "Mix";
        usesTurns = !KoLCharacter.hasBartender() && !KoLCharacter.hasSkill("Cocktail Magic");
      } else if (urlString.contains("mode=cook")) {
        command = "Cook";
        usesTurns = !KoLCharacter.hasChef();
      } else if (urlString.contains("mode=smith")) {
        command = "Smith";
        usesTurns = true;
      } else if (urlString.contains("mode=jewelry")) {
        command = "Ply";
        usesTurns = true;
      } else {
        // Take credit for all visits to crafting
        return true;
      }
    } else if (urlString.startsWith("guild.php")) {
      if (urlString.contains("action=malussmash")) {
        command = "Pulverize";
        multiplier = 5;
      } else {
        return false;
      }
    } else {
      return false;
    }

    String line = CreateItemRequest.getCreationCommand(command, urlString, multiplier, usesTurns);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(line);

    // *** We should deduct ingredients after we have verified that the creation worked.
    AdventureResult[] ingredients = CreateItemRequest.findIngredients(urlString);
    int quantity = CreateItemRequest.getQuantity(urlString, ingredients, multiplier);
    CreateItemRequest.useIngredients(urlString, ingredients, quantity);

    return true;
  }

  public static final String getCreationCommand(
      final String command, final String urlString, final int multiplier, final boolean usesTurns) {
    StringBuilder buffer = new StringBuilder();

    if (usesTurns) {
      buffer.append("[");
      buffer.append(KoLAdventure.getAdventureCount());
      buffer.append("] ");
    }

    buffer.append(command);

    AdventureResult[] ingredients = CreateItemRequest.findIngredients(urlString);
    int quantity = CreateItemRequest.getQuantity(urlString, ingredients, multiplier);

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult item = ingredients[i];
      if (item.getItemId() == 0) {
        continue;
      }

      buffer.append(' ');

      if (i > 0) {
        buffer.append("+ ");
      }

      buffer.append(quantity);
      buffer.append(' ');
      buffer.append(item.getName());
    }

    return buffer.toString();
  }

  public static final AdventureResult[] findIngredients(final String urlString) {
    if (urlString.startsWith("craft.php") && urlString.contains("target")) {
      // Crafting is going to make an item from ingredients.
      // Return the ingredients we think will be used.

      Matcher targetMatcher = CreateItemRequest.TARGET_PATTERN.matcher(urlString);
      if (!targetMatcher.find()) {
        return null;
      }

      int itemId = StringUtilities.parseInt(targetMatcher.group(1));
      return ConcoctionDatabase.getIngredients(itemId);
    }

    Matcher matcher =
        urlString.startsWith("craft.php")
            ? CreateItemRequest.CRAFT_PATTERN_1.matcher(urlString)
            : CreateItemRequest.ITEMID_PATTERN.matcher(urlString);

    List<AdventureResult> ingredients = new ArrayList<>();
    while (matcher.find()) {
      ingredients.add(CreateItemRequest.getIngredient(matcher.group(1)));
    }

    return ingredients.toArray(new AdventureResult[0]);
  }

  private static AdventureResult getIngredient(final String itemId) {
    return ItemPool.get(StringUtilities.parseInt(itemId), 1);
  }

  public static final int getQuantity(
      final String urlString, final AdventureResult[] ingredients, int multiplier) {
    if (!urlString.contains("max=on")
        && !urlString.contains("smashall=1")
        && !urlString.contains("makeall=on")) {
      Matcher matcher = CreateItemRequest.QUANTITY_PATTERN.matcher(urlString);
      return matcher.find() ? StringUtilities.parseInt(matcher.group(2)) * multiplier : multiplier;
    }

    int quantity = Integer.MAX_VALUE;

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult item = ingredients[i];
      quantity = Math.min(item.getCount(KoLConstants.inventory) / multiplier, quantity);
    }

    return quantity * multiplier;
  }

  private static void useIngredients(
      final String urlString, AdventureResult[] ingredients, int quantity) {
    // Let crafting tell us which ingredients it used and remove
    // them from inventory after the fact.
    if (urlString.startsWith("craft.php")) {
      return;
    }

    // Similarly,.we deal with ingredients from guild tools later
    if (urlString.startsWith("guild.php")) {
      return;
    }

    // If we have no ingredients, nothing to do
    if (ingredients == null) {
      return;
    }

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult item = ingredients[i];
      ResultProcessor.processItem(item.getItemId(), 0 - quantity);
    }

    if (urlString.contains("mode=combine")) {
      ResultProcessor.processItem(ItemPool.MEAT_PASTE, 0 - quantity);
    }
  }
}
