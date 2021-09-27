package net.sourceforge.kolmafia;

import java.util.List;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AppleStoreRequest;
import net.sourceforge.kolmafia.request.ArmoryRequest;
import net.sourceforge.kolmafia.request.ArmoryAndLeggeryRequest;
import net.sourceforge.kolmafia.request.BatFabricatorRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BlackMarketRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BoutiqueRequest;
import net.sourceforge.kolmafia.request.BrogurtRequest;
import net.sourceforge.kolmafia.request.BuffJimmyRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CanteenRequest;
import net.sourceforge.kolmafia.request.ChemiCorpRequest;
import net.sourceforge.kolmafia.request.CosmicRaysBazaarRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.Crimbo14Request;
import net.sourceforge.kolmafia.request.Crimbo17Request;
import net.sourceforge.kolmafia.request.Crimbo20BoozeRequest;
import net.sourceforge.kolmafia.request.Crimbo20CandyRequest;
import net.sourceforge.kolmafia.request.Crimbo20FoodRequest;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DinseyCompanyStoreRequest;
import net.sourceforge.kolmafia.request.DiscoGiftCoRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.DripArmoryRequest;
import net.sourceforge.kolmafia.request.EdShopRequest;
import net.sourceforge.kolmafia.request.FDKOLRequest;
import net.sourceforge.kolmafia.request.FishboneryRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.FunALogRequest;
import net.sourceforge.kolmafia.request.GMartRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GotporkOrphanageRequest;
import net.sourceforge.kolmafia.request.GotporkPDRequest;
import net.sourceforge.kolmafia.request.GuzzlrRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.LTTRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.MemeShopRequest;
import net.sourceforge.kolmafia.request.MerchTableRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.NeandermallRequest;
import net.sourceforge.kolmafia.request.NinjaStoreRequest;
import net.sourceforge.kolmafia.request.NuggletCraftingRequest;
import net.sourceforge.kolmafia.request.PlumberGearRequest;
import net.sourceforge.kolmafia.request.PlumberItemRequest;
import net.sourceforge.kolmafia.request.PokemporiumRequest;
import net.sourceforge.kolmafia.request.PrecinctRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.RubeeRequest;
import net.sourceforge.kolmafia.request.SHAWARMARequest;
import net.sourceforge.kolmafia.request.ShoeRepairRequest;
import net.sourceforge.kolmafia.request.ShoreGiftShopRequest;
import net.sourceforge.kolmafia.request.SpacegateFabricationRequest;
import net.sourceforge.kolmafia.request.SpinMasterLatheRequest;
import net.sourceforge.kolmafia.request.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.TacoDanRequest;
import net.sourceforge.kolmafia.request.TerrifiedEagleInnRequest;
import net.sourceforge.kolmafia.request.ThankShopRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.ToxicChemistryRequest;
import net.sourceforge.kolmafia.request.TrapperRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;
import net.sourceforge.kolmafia.request.WarbearBoxRequest;
import net.sourceforge.kolmafia.request.WalMartRequest;
import net.sourceforge.kolmafia.request.YeNeweSouvenirShoppeRequest;
import net.sourceforge.kolmafia.request.YourCampfireRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class CoinmasterRegistry
{
	public static final CoinmasterData [] COINMASTERS = new CoinmasterData[]
	{
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
		CrimboCartelRequest.CRIMBO_CARTEL,
		DimemasterRequest.HIPPY,
		DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE,
		DiscoGiftCoRequest.DISCO_GIFTCO,
		DollHawkerRequest.DOLLHAWKER,
		DripArmoryRequest.DRIP_ARMORY,
		EdShopRequest.EDSHOP,
		FDKOLRequest.FDKOL,
		FishboneryRequest.FISHBONERY,
		FreeSnackRequest.FREESNACKS,
		FudgeWandRequest.FUDGEWAND,
		FunALogRequest.FUN_A_LOG,
		GameShoppeRequest.GAMESHOPPE,
		GMartRequest.GMART,
		GotporkOrphanageRequest.GOTPORK_ORPHANAGE,
		GotporkPDRequest.GOTPORK_PD,
		GuzzlrRequest.GUZZLR,
		HermitRequest.HERMIT,
		IsotopeSmitheryRequest.ISOTOPE_SMITHERY,
		LTTRequest.LTT,
		LunarLunchRequest.LUNAR_LUNCH,
		MemeShopRequest.BACON_STORE,
		MerchTableRequest.MERCH_TABLE,
		MrStoreRequest.MR_STORE,
		NeandermallRequest.NEANDERMALL,
		NinjaStoreRequest.NINJA_STORE,
		NuggletCraftingRequest.NUGGLETCRAFTING,
		PlumberGearRequest.PLUMBER_GEAR,
		PlumberItemRequest.PLUMBER_ITEMS,
		PokemporiumRequest.POKEMPORIUM,
		PrecinctRequest.PRECINCT,
		QuartersmasterRequest.FRATBOY,
		RubeeRequest.RUBEE,
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
		VendingMachineRequest.VENDING_MACHINE,
		WalMartRequest.WALMART,
		WarbearBoxRequest.WARBEARBOX,
		YeNeweSouvenirShoppeRequest.SHAKE_SHOP,
		YourCampfireRequest.YOUR_CAMPFIRE,
	};

	public static final String [] MASTERS = new String[ COINMASTERS.length ];
	public static final String [] NICKNAMES = new String[ COINMASTERS.length ];
	static
	{
		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData cm = COINMASTERS[ i ];
			MASTERS[ i ] = StringUtilities.getCanonicalName( cm.getMaster() );
			NICKNAMES[ i ] = StringUtilities.getCanonicalName( cm.getNickname() );
			COINMASTERS[ i ].registerPurchaseRequests();
		}
	}

    public static void reset()
	{
		// Nothing to do, but calling this will run the static
		// initialization the first time this class is accessed.
	}

	public static CoinmasterData findCoinmaster( final String master )
	{
		List<String> matchingNames = StringUtilities.getMatchingNames( MASTERS, master );

		int size = matchingNames.size();

		if ( size == 0 )
		{
			return null;
		}

		String match = ( size == 1 ) ?
			matchingNames.get( 0 ) :
			StringUtilities.getCanonicalName( master ).trim();

		for ( int i = 0; i < MASTERS.length; ++i )
		{
			if ( match.equals( MASTERS[ i ] ) )
			{
				return COINMASTERS[ i ];
			}
		}

		return null;
	}

	public static CoinmasterData findCoinmasterByNickname( final String nickname )
	{
		List<String> matchingNames = StringUtilities.getMatchingNames( NICKNAMES, nickname );

		if ( matchingNames.size() != 1 )
		{
			return null;
		}

		String name = matchingNames.get( 0 );
		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData data = COINMASTERS[ i ];
			if ( name.equalsIgnoreCase( data.getNickname() ) )
			{
				return data;
			}
		}
		return null;
	}

	public static CoinmasterData findBuyer( final int itemId )
	{
		if ( itemId == -1 )
		{
			return null;
		}

		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData data = COINMASTERS[ i ];
			if ( data.canSellItem( itemId ) )
			{
				return data;
			}
		}

		return null;
	}

	public static CoinmasterData findSeller( final int itemId )
	{
		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData data = COINMASTERS[ i ];
			if ( data.canBuyItem( itemId ) )
			{
				return data;
			}
		}
		return null;
	}
}
