package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.IslandManager;

public class DimemasterRequest
	extends CoinMasterRequest
{
	public static final String master = "Dimemaster"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( DimemasterRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( DimemasterRequest.master );
	private static final LockableListModel<AdventureResult> sellItems = CoinmastersDatabase.getSellItems( DimemasterRequest.master );
	private static final Map<Integer, Integer> sellPrices = CoinmastersDatabase.getSellPrices( DimemasterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You've.*?got ([\\d,]+) dime" );
	public static final CoinmasterData HIPPY =
		new CoinmasterData(
			DimemasterRequest.master,
			"dimemaster",
			DimemasterRequest.class,
			"dime",
			"You don't have any dimes",
			false,
			DimemasterRequest.TOKEN_PATTERN,
			null,
			"availableDimes",
			null,
			"bigisland.php?place=camp&whichcamp=1",
			"getgear",
			DimemasterRequest.buyItems,
			DimemasterRequest.buyPrices,
			"bigisland.php?place=camp&whichcamp=1",
			"turnin",
			DimemasterRequest.sellItems,
			DimemasterRequest.sellPrices,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.PATCHOULI_OIL_BOMB:
				case ItemPool.EXPLODING_HACKY_SACK:
					return Preferences.getString( "sidequestLighthouseCompleted" ).equals( "hippy" );
				}
				return super.canBuyItem( itemId );
			}
		};

	static
	{
		ConcoctionPool.set( new Concoction( "dime", "availableDimes" ) );
	}

    public DimemasterRequest()
	{
		super( DimemasterRequest.HIPPY );
	}

	public DimemasterRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( DimemasterRequest.HIPPY, buying, attachments );
	}

	public DimemasterRequest( final boolean buying, final AdventureResult attachment )
	{
		super( DimemasterRequest.HIPPY, buying, attachment );
	}

	public DimemasterRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( DimemasterRequest.HIPPY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		CoinMasterRequest.parseResponse( DimemasterRequest.HIPPY, this.getURLString(), this.responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) || urlString.indexOf( "whichcamp=1" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = DimemasterRequest.HIPPY;
		IslandRequest.lastCampVisited = data;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		if ( !IslandManager.warProgress().equals( "started" ) )
		{
			return "You're not at war.";
		}

		if ( !EquipmentManager.hasOutfit( OutfitPool.WAR_HIPPY_OUTFIT ) )
		{
			return "You don't have the War Hippy Fatigues";
		}

		return null;
	}

	@Override
	public void equip()
	{
		if ( !EquipmentManager.isWearingOutfit( OutfitPool.WAR_HIPPY_OUTFIT ) )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( OutfitPool.WAR_HIPPY_OUTFIT );
			EquipmentRequest request = new EquipmentRequest( outfit );
			RequestThread.postRequest( request );
		}
	}
}
