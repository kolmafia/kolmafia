package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.request.coinmaster.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.coinmaster.BURTRequest;
import net.sourceforge.kolmafia.request.coinmaster.BigBrotherRequest;
import net.sourceforge.kolmafia.request.coinmaster.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.coinmaster.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.Crimbo11Request;
import net.sourceforge.kolmafia.request.coinmaster.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.coinmaster.DimemasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.FreeSnackRequest;
import net.sourceforge.kolmafia.request.coinmaster.FudgeWandRequest;
import net.sourceforge.kolmafia.request.coinmaster.GameShoppeRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.request.coinmaster.MrStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.SkeletonOfCrimboPastRequest;
import net.sourceforge.kolmafia.request.coinmaster.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.TravelingTraderRequest;
// CHECKSTYLE.SUPPRESS: AvoidStarImport
import net.sourceforge.kolmafia.request.coinmaster.shop.*;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class CoinmasterRegistry {
  public static final CoinmasterData[] COINMASTERS =
      new CoinmasterData[] {
        AirportRequest.DATA,
        AlliedHqRequest.DATA,
        AltarOfBonesRequest.ALTAR_OF_BONES,
        AppleStoreRequest.APPLE_STORE,
        ArmoryRequest.ARMORY,
        ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY,
        AWOLQuartermasterRequest.AWOL,
        BatFabricatorRequest.BAT_FABRICATOR,
        BeerGardenRequest.DATA,
        BigBrotherRequest.BIG_BROTHER,
        BlackMarketRequest.BLACK_MARKET,
        BountyHunterHunterRequest.BHH,
        BoutiqueRequest.BOUTIQUE,
        BrogurtRequest.BROGURT,
        BuffJimmyRequest.BUFF_JIMMY,
        BURTRequest.BURT,
        CanteenRequest.CANTEEN,
        ChemiCorpRequest.CHEMICORP,
        CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR,
        CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP,
        Crimbo11Request.CRIMBO11,
        Crimbo14Request.CRIMBO14,
        Crimbo16Request.DATA,
        Crimbo17Request.CRIMBO17,
        Crimbo20BoozeRequest.CRIMBO20BOOZE,
        Crimbo20CandyRequest.CRIMBO20CANDY,
        Crimbo20FoodRequest.CRIMBO20FOOD,
        Crimbo23ElfArmoryRequest.DATA,
        Crimbo23ElfBarRequest.DATA,
        Crimbo23ElfCafeRequest.DATA,
        Crimbo23ElfFactoryRequest.DATA,
        Crimbo23PirateArmoryRequest.DATA,
        Crimbo23PirateBarRequest.DATA,
        Crimbo23PirateCafeRequest.DATA,
        Crimbo23PirateFactoryRequest.DATA,
        Crimbo24BarRequest.DATA,
        Crimbo24CafeRequest.DATA,
        Crimbo24FactoryRequest.DATA,
        CrimboCartelRequest.CRIMBO_CARTEL,
        DedigitizerRequest.DATA,
        DimemasterRequest.HIPPY,
        DinostaurRequest.DINOSTAUR,
        DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE,
        DiscoGiftCoRequest.DISCO_GIFTCO,
        DollHawkerRequest.DOLLHAWKER,
        DripArmoryRequest.DRIP_ARMORY,
        EdShopRequest.EDSHOP,
        FancyDanRequest.FANCY_DAN,
        FDKOLRequest.FDKOL,
        FishboneryRequest.FISHBONERY,
        FiveDPrinterRequest.DATA,
        FixodentRequest.DATA,
        FlowerTradeinRequest.DATA,
        FreeSnackRequest.FREESNACKS,
        FudgeWandRequest.FUDGEWAND,
        FunALogRequest.FUN_A_LOG,
        GameShoppeRequest.GAMESHOPPE,
        GMartRequest.GMART,
        GeneticFiddlingRequest.DATA,
        GotporkOrphanageRequest.GOTPORK_ORPHANAGE,
        GotporkPDRequest.GOTPORK_PD,
        GrandmaRequest.DATA,
        GuzzlrRequest.GUZZLR,
        HermitRequest.HERMIT,
        IsotopeSmitheryRequest.ISOTOPE_SMITHERY,
        JunkMagazineRequest.DATA,
        KiwiKwikiMartRequest.DATA,
        KOLHSArtRequest.DATA,
        KOLHSChemRequest.DATA,
        KOLHSShopRequest.DATA,
        KringleRequest.DATA,
        LTTRequest.LTT,
        LunarLunchRequest.LUNAR_LUNCH,
        MemeShopRequest.BACON_STORE,
        MerchTableRequest.MERCH_TABLE,
        MrStoreRequest.MR_STORE,
        MrStore2002Request.MR_STORE_2002,
        NeandermallRequest.NEANDERMALL,
        NinjaStoreRequest.NINJA_STORE,
        NuggletCraftingRequest.NUGGLETCRAFTING,
        PlumberGearRequest.PLUMBER_GEAR,
        PlumberItemRequest.PLUMBER_ITEMS,
        PokemporiumRequest.POKEMPORIUM,
        PrecinctRequest.PRECINCT,
        PrimordialSoupKitchenRequest.DATA,
        QuartersmasterRequest.FRATBOY,
        ReplicaMrStoreRequest.REPLICA_MR_STORE,
        RubeeRequest.RUBEE,
        RumpleRequest.DATA,
        SeptEmberCenserRequest.SEPTEMBER_CENSER,
        ShadowForgeRequest.DATA,
        SHAWARMARequest.SHAWARMA,
        ShoeRepairRequest.SHOE_REPAIR,
        ShoreGiftShopRequest.SHORE_GIFT_SHOP,
        SkeletonOfCrimboPastRequest.SKELETON_OF_CRIMBO_PAST,
        SliemceRequest.DATA,
        SpacegateFabricationRequest.SPACEGATE_STORE,
        SpantRequest.DATA,
        SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE,
        StarChartRequest.DATA,
        SugarSheetRequest.DATA,
        SwaggerShopRequest.SWAGGER_SHOP,
        TacoDanRequest.TACO_DAN,
        TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN,
        ThankShopRequest.CASHEW_STORE,
        TicketCounterRequest.TICKET_COUNTER,
        ToxicChemistryRequest.TOXIC_CHEMISTRY,
        TrapperRequest.TRAPPER,
        TravelingTraderRequest.TRAVELER,
        UsingYourShowerThoughtsRequest.DATA,
        VendingMachineRequest.VENDING_MACHINE,
        WalMartRequest.WALMART,
        WarbearBoxRequest.WARBEARBOX,
        WetCrapForSaleRequest.DATA,
        WinterGardenRequest.DATA,
        XOShopRequest.DATA,
        YeNeweSouvenirShoppeRequest.SHAKE_SHOP,
        YourCampfireRequest.YOUR_CAMPFIRE,
      };

  public static final String[] MASTERS = new String[COINMASTERS.length];
  public static final String[] NICKNAMES = new String[COINMASTERS.length];

  static {
    for (int i = 0; i < COINMASTERS.length; ++i) {
      CoinmasterData cm = COINMASTERS[i];
      MASTERS[i] = StringUtilities.getCanonicalName(cm.getMaster());
      NICKNAMES[i] = StringUtilities.getCanonicalName(cm.getNickname());
      cm.registerShop();
      cm.registerCurrencies();
      cm.registerShopRows();
      cm.registerPurchaseRequests();
      cm.registerPropertyToken();
    }
  }

  public static void reset() {
    // Nothing to do, but calling this will run the static
    // initialization the first time this class is accessed.
  }

  public static CoinmasterData findCoinmaster(final String nickname, final String master) {
    CoinmasterData result = findCoinmasterByNickname(nickname);
    if (result != null) {
      return result;
    }

    result = findCoinmaster(master);
    if (result != null) {
      return result;
    }

    return null;
  }

  public static CoinmasterData findCoinmaster(final String master) {
    List<String> matchingNames = StringUtilities.getMatchingNames(MASTERS, master);

    int size = matchingNames.size();

    if (size == 0) {
      return null;
    }

    String match =
        (size == 1) ? matchingNames.get(0) : StringUtilities.getCanonicalName(master).trim();

    for (int i = 0; i < MASTERS.length; ++i) {
      if (match.equals(MASTERS[i])) {
        return COINMASTERS[i];
      }
    }

    return null;
  }

  public static CoinmasterData findCoinmasterByNickname(final String nickname) {
    List<String> matchingNames = StringUtilities.getMatchingNames(NICKNAMES, nickname);

    if (matchingNames.size() != 1) {
      return null;
    }

    String name = matchingNames.get(0);

    return Arrays.stream(COINMASTERS)
        .filter(data -> name.equalsIgnoreCase(data.getNickname()))
        .findAny()
        .orElse(null);
  }

  public static CoinmasterData findBuyer(final int itemId) {
    if (itemId == -1) {
      return null;
    }

    return Arrays.stream(COINMASTERS)
        .filter(data -> data.canSellItem(itemId))
        .findAny()
        .orElse(null);
  }

  public static CoinmasterData findSeller(final int itemId) {
    return Arrays.stream(COINMASTERS)
        .filter(data -> data.canBuyItem(itemId))
        .findAny()
        .orElse(null);
  }
}
