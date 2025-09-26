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
import net.sourceforge.kolmafia.request.coinmaster.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.AlliedHqRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.AppleStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ArmoryAndLeggeryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BatFabricatorRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BlackMarketRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BoutiqueRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BrogurtRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BuffJimmyRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.CanteenRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ChemiCorpRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.CosmicRaysBazaarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo14Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo17Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20BoozeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20CandyRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20FoodRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfBarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfCafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfFactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateBarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateCafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateFactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24BarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24CafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24FactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DedigitizerRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DinostaurRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DinseyCompanyStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DiscoGiftCoRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DollHawkerRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DripArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.EdShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FDKOLRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FancyDanRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FishboneryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FixodentRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FlowerTradeinRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FunALogRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GeneticFiddlingRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GotporkOrphanageRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GotporkPDRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GuzzlrRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.KiwiKwikiMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.LTTRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.LunarLunchRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MemeShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MerchTableRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MrStore2002Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.NeandermallRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.NinjaStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.NuggletCraftingRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PlumberGearRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PlumberItemRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PokemporiumRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PrecinctRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PrimordialSoupKitchenRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ReplicaMrStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.RubeeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SHAWARMARequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SeptEmberCenserRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ShoeRepairRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ShoreGiftShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SpacegateFabricationRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SpinMasterLatheRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TacoDanRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TerrifiedEagleInnRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ThankShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TicketCounterRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ToxicChemistryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TrapperRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.UsingYourShowerThoughtsRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.VendingMachineRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.WalMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.WarbearBoxRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.WetCrapForSaleRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.YeNeweSouvenirShoppeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.YourCampfireRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class CoinmasterRegistry {
  public static final CoinmasterData[] COINMASTERS =
      new CoinmasterData[] {
        AlliedHqRequest.DATA,
        AltarOfBonesRequest.ALTAR_OF_BONES,
        AppleStoreRequest.APPLE_STORE,
        ArmoryRequest.ARMORY,
        ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY,
        AWOLQuartermasterRequest.AWOL,
        BatFabricatorRequest.BAT_FABRICATOR,
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
        GuzzlrRequest.GUZZLR,
        HermitRequest.HERMIT,
        IsotopeSmitheryRequest.ISOTOPE_SMITHERY,
        KiwiKwikiMartRequest.DATA,
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
        SeptEmberCenserRequest.SEPTEMBER_CENSER,
        SHAWARMARequest.SHAWARMA,
        ShoeRepairRequest.SHOE_REPAIR,
        ShoreGiftShopRequest.SHORE_GIFT_SHOP,
        SpacegateFabricationRequest.SPACEGATE_STORE,
        SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE,
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
