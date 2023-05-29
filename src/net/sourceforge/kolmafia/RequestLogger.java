package net.sourceforge.kolmafia;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.*;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.ElVibratoManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.NullStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;

public class RequestLogger extends NullStream {
  public static final RequestLogger INSTANCE = new RequestLogger();

  private static PrintStream outputStream = KoLmafiaTUI.outputStream;
  private static PrintStream mirrorStream = NullStream.INSTANCE;

  private static PrintStream sessionStream = NullStream.INSTANCE;
  private static PrintStream debugStream = NullStream.INSTANCE;
  private static PrintStream traceStream = NullStream.INSTANCE;

  private static String lastURLString = "";
  public static String previousUpdateString = "";
  private static boolean wasLastRequestSimple = false;

  private RequestLogger() {}

  public static String getLastURLString() {
    return lastURLString;
  }

  @Override
  public void println() {
    RequestLogger.printLine();
  }

  @Override
  public void println(final String line) {
    RequestLogger.printLine(line);
  }

  public static void printList(final List<?> printing, final PrintStream ostream) {
    if (printing == null || ostream == null) {
      return;
    }

    if (printing != KoLConstants.availableSkills) {
      ostream.println(
          printing.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .collect(Collectors.joining(KoLConstants.LINE_BREAK)));
      return;
    }

    StringBuffer buffer = new StringBuffer();
    SkillDatabase.generateSkillList(buffer, false);

    if (ostream != INSTANCE) {
      ostream.println(buffer);
      return;
    }

    printLine(buffer.toString(), false);

    buffer.setLength(0);
    SkillDatabase.generateSkillList(buffer, true);
    KoLConstants.commandBuffer.append(buffer.toString());
  }

  public static void printList(final List<?> printing) {
    RequestLogger.printList(printing, INSTANCE);
  }

  public static void printLine() {
    RequestLogger.printLine(MafiaState.CONTINUE, "", true);
  }

  public static void printLine(final String message) {
    RequestLogger.printLine(MafiaState.CONTINUE, message, true);
  }

  public static void printLine(final String message, final boolean addToBuffer) {
    RequestLogger.printLine(MafiaState.CONTINUE, message, addToBuffer);
  }

  public static void printLine(final MafiaState state, final String message) {
    RequestLogger.printLine(state, message, true);
  }

  public static void printLine(final MafiaState state, String message, boolean addToBuffer) {
    if (message == null) {
      return;
    }

    message = message.trim();

    if (message.length() == 0 && RequestLogger.previousUpdateString.length() == 0) {
      return;
    }

    RequestLogger.previousUpdateString = message;

    RequestLogger.outputStream.println(message);
    RequestLogger.debugStream.println(message);

    if (StaticEntity.backtraceTrigger != null && message.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by message");
    }

    if (!addToBuffer) {
      RequestLogger.mirrorStream.println(message);
      return;
    }

    StringBuffer colorBuffer = new StringBuffer();

    if (message.isEmpty()) {
      colorBuffer.append("<br>");
    } else {
      boolean addedColor = false;

      // Temporary workaround for Java bug
      if (message.startsWith("/")) {
        colorBuffer.append("<span>");
      }

      if (state == MafiaState.ERROR || state == MafiaState.ABORT) {
        addedColor = true;
        colorBuffer.append("<font color=red>");
      } else if (message.startsWith("> QUEUED")) {
        addedColor = true;
        colorBuffer.append(" <font color=olive><b>");
      } else if (message.startsWith("> ")) {
        addedColor = true;
        colorBuffer.append(" <font color=olive>");
      }

      colorBuffer.append(StringUtilities.globalStringReplace(message, "\n", "<br>"));

      if (message.startsWith("> QUEUED")) {
        colorBuffer.append("</b>");
      }

      if (addedColor) {
        colorBuffer.append("</font><br>");
      } else {
        colorBuffer.append("<br>");
      }

      if (!message.contains("<") && message.contains(KoLConstants.LINE_BREAK)) {
        colorBuffer.append("</pre>");
      }

      // Temporary workaround for Java bug
      if (message.startsWith("/")) {
        colorBuffer.append("</span>");
      }

      StringUtilities.globalStringDelete(colorBuffer, "<html>");
      StringUtilities.globalStringDelete(colorBuffer, "</html>");
    }

    colorBuffer.append(KoLConstants.LINE_BREAK);
    KoLConstants.commandBuffer.append(colorBuffer.toString());
    RequestLogger.mirrorStream.println(colorBuffer);
    RelayServer.addStatusMessage(colorBuffer.toString());
  }

  public static PrintStream openStream(
      final String filename, final PrintStream originalStream, boolean hasLocation) {
    if (!hasLocation && KoLCharacter.getUserName().isEmpty()) {
      return NullStream.INSTANCE;
    }

    // Before doing anything, be sure to close the
    // original stream.

    if (!(originalStream instanceof NullStream)) {
      if (hasLocation) {
        return originalStream;
      }

      RequestLogger.closeStream(originalStream);
    }

    return LogStream.openStream(filename, false);
  }

  public static void closeStream(final PrintStream stream) {
    try {
      stream.close();
    } catch (Exception e) {
    }
  }

  public static void openCustom(PrintStream out) {
    RequestLogger.outputStream = out;
  }

  public static void closeCustom() {
    RequestLogger.closeStream(RequestLogger.outputStream);
    RequestLogger.outputStream = KoLmafiaTUI.outputStream;
  }

  public static void openMirror(final String location) {
    RequestLogger.mirrorStream =
        RequestLogger.openStream(location, RequestLogger.mirrorStream, true);
  }

  public static void closeMirror() {
    RequestLogger.closeStream(RequestLogger.mirrorStream);
    RequestLogger.mirrorStream = NullStream.INSTANCE;
  }

  public static void setSessionStream(PrintStream stream) {
    RequestLogger.sessionStream = stream;
  }

  public static PrintStream getSessionStream() {
    return RequestLogger.sessionStream;
  }

  public static void openSessionLog() {
    RequestLogger.sessionStream =
        RequestLogger.openStream(
            KoLConstants.SESSIONS_DIRECTORY
                + StringUtilities.globalStringReplace(KoLCharacter.getUserName(), " ", "_")
                + "_"
                + KoLConstants.DAILY_FORMAT.format(new Date())
                + ".txt",
            RequestLogger.sessionStream,
            false);
  }

  public static void closeSessionLog() {
    RequestLogger.closeStream(RequestLogger.sessionStream);
    RequestLogger.sessionStream = NullStream.INSTANCE;
  }

  public static void updateSessionLog() {
    RequestLogger.sessionStream.println();
  }

  public static void updateSessionLog(final String line) {
    if (StaticEntity.backtraceTrigger != null && line.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by session log message");
    }

    RequestLogger.sessionStream.println(line);
  }

  public static boolean isDebugging() {
    return RequestLogger.debugStream != NullStream.INSTANCE;
  }

  public static PrintStream getDebugStream() {
    return RequestLogger.debugStream;
  }

  public static void openDebugLog() {
    RequestLogger.debugStream =
        RequestLogger.openStream(
            "DEBUG_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt",
            RequestLogger.debugStream,
            true);
    NamedListenerRegistry.fireChange("(debug)");
  }

  public static void closeDebugLog() {
    RequestLogger.closeStream(RequestLogger.debugStream);
    RequestLogger.debugStream = NullStream.INSTANCE;
    NamedListenerRegistry.fireChange("(debug)");
  }

  public static void updateDebugLog() {
    RequestLogger.debugStream.println();
  }

  public static void updateDebugLog(final String line) {
    if (StaticEntity.backtraceTrigger != null && line.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by debug log message");
    }

    RequestLogger.debugStream.println(line);
  }

  public static void updateDebugLog(final Throwable t) {
    t.printStackTrace(RequestLogger.debugStream);
  }

  public static void updateDebugLog(final Object o) {
    RequestLogger.debugStream.println(o.toString());
  }

  public static boolean isTracing() {
    return RequestLogger.traceStream != NullStream.INSTANCE;
  }

  public static PrintStream getTraceStream() {
    return RequestLogger.traceStream;
  }

  public static void openTraceStream() {
    RequestLogger.traceStream =
        RequestLogger.openStream(
            "TRACE_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt",
            RequestLogger.traceStream,
            true);
  }

  public static void closeTraceStream() {
    RequestLogger.closeStream(RequestLogger.traceStream);
    RequestLogger.traceStream = NullStream.INSTANCE;
  }

  private static final StringBuilder traceBuffer = new StringBuilder();

  public static synchronized void trace(String message) {
    if (RequestLogger.isTracing()) {
      traceBuffer.setLength(0);
      traceBuffer.append((new Date()).getTime());
      traceBuffer.append(": ");
      traceBuffer.append(message);
      RequestLogger.traceStream.println(traceBuffer);
    }
  }

  public static void registerRequest(final GenericRequest request, final String urlString) {
    try {
      RequestLogger.doRegister(request, urlString);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public enum RequestList {
    ALTAR_OF_LITERACY_REQUEST(AltarOfLiteracyRequest.class),
    BOUNTY_HUNTER_HUNTER_REQUEST(BountyHunterHunterRequest.class),
    FUDGE_WAND_REQUEST(FudgeWandRequest.class),
    SUMMONING_CHAMBER_REQUEST(SummoningChamberRequest.class),
    HEY_DEZE_REQUEST(HeyDezeRequest.class),
    MR_STORE_REQUEST(MrStoreRequest.class),
    SPAAACE_REQUEST(SpaaaceRequest.class),
    VOLCANO_MAZE_REQUEST(VolcanoMazeRequest.class),
    BURNING_NEWSPAPER_REQUEST(BurningNewspaperRequest.class),
    METEOROID_REQUEST(MeteoroidRequest.class),
    GRUBBY_WOOL_REQUEST(GrubbyWoolRequest.class),
    CLAN_LOUNGE_SWIMMING_POOL_REQUEST(ClanLoungeSwimmingPoolRequest.class),
    CARGO_CULTIST_SHORTS_REQUEST(CargoCultistShortsRequest.class),
    DECK_OF_EVERY_CARD_REQUEST(DeckOfEveryCardRequest.class),
    SWEET_SYNTHESIS_REQUEST(SweetSynthesisRequest.class),
    FLORIST_REQUEST(FloristRequest.class),
    NUMBEROLOGY_REQUEST(NumberologyRequest.class),
    POTTED_TEA_TREE_REQUEST(PottedTeaTreeRequest.class),
    SAUSAGE_O_MATIC_REQUEST(SausageOMaticRequest.class),
    TERMINAL_REQUEST(TerminalRequest.class),
    WAX_GLOB_REQUEST(WaxGlobRequest.class),
    CAMPGROUND_REQUEST(CampgroundRequest.class),
    PIZZA_CUBE_REQUEST(PizzaCubeRequest.class),
    PORTAL_REQUEST(PortalRequest.class),
    TELESCOPE_REQUEST(TelescopeRequest.class),
    USE_SKILL_REQUEST(UseSkillRequest.class),
    EQUIPMENT_REQUEST(EquipmentRequest.class),
    MICRO_BREWERY_REQUEST(MicroBreweryRequest.class),
    CHEZ_SNOOTEE_REQUEST(ChezSnooteeRequest.class),
    CRIMBO_CAFE_REQUEST(CrimboCafeRequest.class),
    USE_ITEM_REQUEST(UseItemRequest.class),
    FALLOUT_SHELTER_REQUEST(FalloutShelterRequest.class),
    AFTER_LIFE_REQUEST(AfterLifeRequest.class),
    AIRPORT_REQUEST(AirportRequest.class),
    ALTAR_OF_BONES_REQUEST(AltarOfBonesRequest.class),
    ARMORY_REQUEST(ArmoryRequest.class),
    APPLE_STORE_REQUEST(AppleStoreRequest.class),
    ARCADE_REQUEST(ArcadeRequest.class),
    ARTIST_REQUEST(ArtistRequest.class),
    AUTO_MALL_REQUEST(AutoMallRequest.class),
    AUTO_SELL_REQUEST(AutoSellRequest.class),
    AWOL_QUARTERMASTER_REQUEST(AWOLQuartermasterRequest.class),
    BAT_FABRICATOR_REQUEST(BatFabricatorRequest.class),
    BEER_GARDEN_REQUEST(BeerGardenRequest.class),
    BEER_PONG_REQUEST(BeerPongRequest.class),
    BIG_BROTHER_REQUEST(BigBrotherRequest.class),
    BOUTIQUE_REQUEST(BoutiqueRequest.class),
    BROGURT_REQUEST(BrogurtRequest.class),
    BUFF_JIMMY_REQUEST(BuffJimmyRequest.class),
    BURT_REQUEST(BURTRequest.class),
    CAFE_REQUEST(CafeRequest.class),
    CAKE_ARENA_REQUEST(CakeArenaRequest.class),
    CAMP_AWAY_REQUEST(CampAwayRequest.class),
    CANTEEN_REQUEST(CanteenRequest.class),
    CHATEAU_REQUEST(ChateauRequest.class),
    CHEMI_CORP_REQUEST(ChemiCorpRequest.class),
    CLAN_LOUNGE_REQUEST(ClanLoungeRequest.class),
    CLAN_RUMPUS_REQUEST(ClanRumpusRequest.class),
    CLAN_STASH_REQUEST(ClanStashRequest.class),
    CLOSET_REQUEST(ClosetRequest.class),
    COSMIC_RAYS_BAZAAR_REQUEST(CosmicRaysBazaarRequest.class),
    CRIMBCO_GIFT_SHOP_REQUEST(CRIMBCOGiftShopRequest.class),
    CRIMBO07_REQUEST(Crimbo07Request.class),
    CRIMBO09_REQUEST(Crimbo09Request.class),
    CRIMBO10_REQUEST(Crimbo10Request.class),
    CRIMBO11_REQUEST(Crimbo11Request.class),
    CRIMBO12_REQUEST(Crimbo12Request.class),
    CRIMBO14_REQUEST(Crimbo14Request.class),
    CRIMBO16_REQUEST(Crimbo16Request.class),
    CRIMBO17_REQUEST(Crimbo17Request.class),
    CRIMBO20_BOOZE_REQUEST(Crimbo20BoozeRequest.class),
    CRIMBO20_CANDY_REQUEST(Crimbo20CandyRequest.class),
    CRIMBO20_FOOD_REQUEST(Crimbo20FoodRequest.class),
    CRIMBO21_TREE_REQUEST(Crimbo21TreeRequest.class),
    CRIMBO_CARTEL_REQUEST(CrimboCartelRequest.class),
    CURSE_REQUEST(CurseRequest.class),
    DIG_REQUEST(DigRequest.class),
    DIMEMASTER_REQUEST(DimemasterRequest.class),
    DINOSTAUR_REQUEST(DinostaurRequest.class),
    REPLICA_MR_STORE_REQUEST(ReplicaMrStoreRequest.class),
    DINSEY_COMPANY_STORE_REQUEST(DinseyCompanyStoreRequest.class),
    DISCO_GIFT_CO_REQUEST(DiscoGiftCoRequest.class),
    DISPLAY_CASE_REQUEST(DisplayCaseRequest.class),
    DOLL_HAWKER_REQUEST(DollHawkerRequest.class),
    DREADSY_LVANIA_REQUEST(DreadsylvaniaRequest.class),
    DWARF_CONTRAPTION_REQUEST(DwarfContraptionRequest.class),
    DWARF_FACTORY_REQUEST(DwarfFactoryRequest.class),
    ED_BASE_REQUEST(EdBaseRequest.class),
    ED_SHOP_REQUEST(EdShopRequest.class),
    FAMILIAR_REQUEST(FamiliarRequest.class),
    FAM_TEAM_REQUEST(FamTeamRequest.class),
    FANCY_DAN_REQUEST(FancyDanRequest.class),
    FANTASY_REALM_REQUEST(FantasyRealmRequest.class),
    FISHBONERY_REQUEST(FishboneryRequest.class),
    FIVE_D_PRINTER_REQUEST(FiveDPrinterRequest.class),
    FREE_SNACK_REQUEST(FreeSnackRequest.class),
    FRIAR_REQUEST(FriarRequest.class),
    FUN_A_LOG_REQUEST(FunALogRequest.class),
    GAME_SHOPPE_REQUEST(GameShoppeRequest.class),
    G_MART_REQUEST(GMartRequest.class),
    GOURD_REQUEST(GourdRequest.class),
    GOTPORK_ORPHANAGE_REQUEST(GotporkOrphanageRequest.class),
    GOTPORK_PD_REQUEST(GotporkPDRequest.class),
    GRANDMA_REQUEST(GrandmaRequest.class),
    GRANDPA_REQUEST(GrandpaRequest.class),
    GUILD_REQUEST(GuildRequest.class),
    HERMIT_REQUEST(HermitRequest.class),
    ISLAND_REQUEST(IslandRequest.class),
    ISOTOPE_SMITHERY_REQUEST(IsotopeSmitheryRequest.class),
    JARLSBERG_REQUEST(JarlsbergRequest.class),
    JUNK_MAGAZINE_REQUEST(JunkMagazineRequest.class),
    KNOLL_REQUEST(KnollRequest.class),
    KOLHS_REQUEST(KOLHSRequest.class),
    KRINGLE_REQUEST(KringleRequest.class),
    LEAFLET_REQUEST(LeafletRequest.class),
    LTT_REQUEST(LTTRequest.class),
    LUNAR_LUNCH_REQUEST(LunarLunchRequest.class),
    MALL_SEARCH_REQUEST(MallSearchRequest.class),
    MANAGE_STORE_REQUEST(ManageStoreRequest.class),
    MEME_SHOP_REQUEST(MemeShopRequest.class),
    MERCH_TABLE_REQUEST(MerchTableRequest.class),
    MIND_CONTROL_REQUEST(MindControlRequest.class),
    MOM_REQUEST(MomRequest.class),
    MONSTER_MANUEL_REQUEST(MonsterManuelRequest.class),
    MUSHROOM_REQUEST(MushroomRequest.class),
    NEANDERMALL_REQUEST(NeandermallRequest.class),
    NEMESIS_REQUEST(NemesisRequest.class),
    NINJA_STORE_REQUEST(NinjaStoreRequest.class),
    NUGGLET_CRAFTING_REQUEST(NuggletCraftingRequest.class),
    PANDAMONIUM_REQUEST(PandamoniumRequest.class),
    PEE_V_PEE_REQUEST(PeeVPeeRequest.class),
    PIXEL_REQUEST(PixelRequest.class),
    POKEMPORIUM_REQUEST(PokemporiumRequest.class),
    PRECINCT_REQUEST(PrecinctRequest.class),
    PROFILE_REQUEST(ProfileRequest.class),
    PULVERIZE_REQUEST(PulverizeRequest.class),
    QUARTERSMASTER_REQUEST(QuartersmasterRequest.class),
    RAFFLE_REQUEST(RaffleRequest.class),
    RICHARD_REQUEST(RichardRequest.class),
    RUBEE_REQUEST(RubeeRequest.class),
    RUMPLE_REQUEST(RumpleRequest.class),
    SCRAPHEAP_REQUEST(ScrapheapRequest.class),
    SEA_MERKIN_REQUEST(SeaMerkinRequest.class),
    SEND_GIFT_REQUEST(SendGiftRequest.class),
    SEND_MAIL_REQUEST(SendMailRequest.class),
    SHADOW_FORGE_REQUEST(ShadowForgeRequest.class),
    SHOE_REPAIR_REQUEST(ShoeRepairRequest.class),
    SHAWARMA_REQUEST(SHAWARMARequest.class),
    SHORE_GIFT_SHOP_REQUEST(ShoreGiftShopRequest.class),
    SHRINE_REQUEST(ShrineRequest.class),
    SKATE_PARK_REQUEST(SkateParkRequest.class),
    SLIEMCE_REQUEST(SliemceRequest.class),
    SPACEGATE_FABRICATION_REQUEST(SpacegateFabricationRequest.class),
    SPANT_REQUEST(SpantRequest.class),
    SPIN_MASTER_LATHE_REQUEST(SpinMasterLatheRequest.class),
    STAR_CHART_REQUEST(StarChartRequest.class),
    STANDARD_REQUEST(StandardRequest.class),
    STILL_REQUEST(StillRequest.class),
    STORAGE_REQUEST(StorageRequest.class),
    SUBURBAN_DIS_REQUEST(SuburbanDisRequest.class),
    SUGAR_SHEET_REQUEST(SugarSheetRequest.class),
    SWAGGER_SHOP_REQUEST(SwaggerShopRequest.class),
    TACO_DAN_REQUEST(TacoDanRequest.class),
    TAVERN_REQUEST(TavernRequest.class),
    TERRIFIED_EAGLE_INN_REQUEST(TerrifiedEagleInnRequest.class),
    THANK_SHOP_REQUEST(ThankShopRequest.class),
    TOXIC_CHEMISTRY_REQUEST(ToxicChemistryRequest.class),
    TRAPPER_REQUEST(TrapperRequest.class),
    TRAVELING_TRADER_REQUEST(TravelingTraderRequest.class),
    TROPHY_HUT_REQUEST(TrophyHutRequest.class),
    TUTORIAL_REQUEST(TutorialRequest.class),
    UNEFFECT_REQUEST(UneffectRequest.class),
    UNTINKER_REQUEST(UntinkerRequest.class),
    VENDING_MACHINE_REQUEST(VendingMachineRequest.class),
    VOLCANO_ISLAND_REQUEST(VolcanoIslandRequest.class),
    WAL_MART_REQUEST(WalMartRequest.class),
    WARBEAR_BOX_REQUEST(WarbearBoxRequest.class),
    WILDFIRE_CAMP_REQUEST(WildfireCampRequest.class),
    WINTER_GARDEN_REQUEST(WinterGardenRequest.class),
    XO_SHOP_REQUEST(XOShopRequest.class),
    YE_NEWE_SOUVENIR_SHOPPE_REQUEST(YeNeweSouvenirShoppeRequest.class),
    YOUR_CAMPFIRE_REQUEST(YourCampfireRequest.class),
    ZAP_REQUEST(ZapRequest.class);

    private final Class<? extends GenericRequest> thisRequest;

    RequestList(Class<? extends GenericRequest> thisRequest) {
      this.thisRequest = thisRequest;
    }

    public Class<? extends GenericRequest> getRequest() {
      return this.thisRequest;
    }
  }

  private static void doRegister(final GenericRequest request, final String urlString)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // If we are in a fight, don't even look at things which are
    // not fight.php, since they will immediately redirect to
    // continue the fight.
    if (FightRequest.currentRound != 0 && !urlString.startsWith("fight.php")) {
      return;
    }

    RequestLogger.lastURLString = urlString;

    if (urlString.startsWith("api")
        || urlString.startsWith("charpane")
        || urlString.startsWith("account")
        || urlString.startsWith("login")
        || urlString.startsWith("logout")) {
      return;
    }

    // We want to do special things when we visit locations within
    // the Sorceress' Lair. Those locations which are "adventures"
    // but are not claimed here will be picked up by KoLAdventure

    if (SorceressLairManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if (KoLAdventure.recordToSession(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Anything past this is not an "adventure" per se

    boolean isExternal =
        request.getClass() == GenericRequest.class
            || request instanceof RelayRequest
            || request instanceof PlaceRequest;

    if ((isExternal || request instanceof FightRequest)
        && FightRequest.registerRequest(isExternal, urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    for (var req : RequestList.values()) {
      if (isExternal || request.getClass() == req.getRequest()) {
        Method method = req.getRequest().getMethod("registerRequest", String.class);
        if ((boolean) method.invoke(null, urlString)) {
          RequestLogger.wasLastRequestSimple = false;
          return;
        }
      }
    }

    // Some general URLs which never need to be registered
    // because they don't do anything.

    if (urlString.startsWith("choice")) {
      ChoiceManager.registerRequest(urlString);
      return;
    }

    // We want to register a simple visit to tiles.php
    if (DvorakManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register a simple visit to ocean.php
    if (OceanManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register a simple visit to elvmachine.php
    if (ElVibratoManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Anything else that doesn't submit an actual form
    // should not be registered.

    if (!urlString.contains("?")) {
      return;
    }

    if (urlString.startsWith("campground")
        || urlString.startsWith("doc.php")
        || urlString.startsWith("inventory.php?ajax")
        || urlString.startsWith("inventory.php?which=")
        || urlString.startsWith("inventory.php?action=message")
        || urlString.startsWith("mining")) {
      return;
    }

    // Check UseItemRequest early, so that lastItemUsed gets
    // cleared when processing anything else.  Otherwise, any
    // non-item-use that redirects to inventory.php?action=message
    // (such as outfit changes) will cause the last item to be
    // processed again.

    // However, we have to check CreateItemRequest earlier, so that
    // it can handle single-/multi-use concoctions.

    if (CreateItemRequest.registerRequest(isExternal, urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Let the "placeholder" for place.php take every otherwise
    // unclaimed call to that URL.

    if (PlaceRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ArmoryAndLeggeryRequest)
        && ArmoryAndLeggeryRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TicketCounterRequest)
        && ArcadeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BlackMarketRequest)
        && BlackMarketRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FDKOLRequest)
        && FDKOLRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TicketCounterRequest)
        && TicketCounterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Do PurchaseRequest after all Coinmaster shops so they can
    // register simple visits, if they so choose.

    if ((isExternal || request instanceof PurchaseRequest)
        && PurchaseRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Let PlaceRequest step in and suppress logging for any
    // unclaimed simple visits to a place.php container

    if (PlaceRequest.unclaimedPlace(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Otherwise, print the raw URL so that it's at least mentioned
    // in the session log.

    if (!RequestLogger.wasLastRequestSimple) {
      RequestLogger.updateSessionLog();
    }

    RequestLogger.wasLastRequestSimple = true;
    RequestLogger.updateSessionLog(urlString);
  }

  public static void registerLocation(final String location) {
    String message = "[" + KoLAdventure.getAdventureCount() + "] " + location;

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);
  }

  public static void registerLastLocation() {
    String location = KoLAdventure.lastLocationName;

    if (location == null) {
      location =
          (GenericRequest.itemMonster != null) ? GenericRequest.itemMonster : "Unknown Location";
    }

    RequestLogger.registerLocation(location);
  }
}
