package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BigBrotherRequest
	extends CoinMasterRequest
{
	public static final String master = "Big Brother"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BigBrotherRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BigBrotherRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? sand dollar" );
	public static final AdventureResult SAND_DOLLAR = ItemPool.get( ItemPool.SAND_DOLLAR, 1 );
	public static final AdventureResult BLACK_GLASS = ItemPool.get( ItemPool.BLACK_GLASS, 1 );

	public static final CoinmasterData BIG_BROTHER =
		new CoinmasterData(
			BigBrotherRequest.master,
			"bigbrother",
			BigBrotherRequest.class,
			"sand dollar",
			"You haven't got any sand dollars",
			false,
			BigBrotherRequest.TOKEN_PATTERN,
			BigBrotherRequest.SAND_DOLLAR,
			null,
			null,
			"monkeycastle.php",
			"buyitem",
			BigBrotherRequest.buyItems,
			BigBrotherRequest.buyPrices,
			null,
			null,
			null,
			null,
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
				case ItemPool.MADNESS_REEF_MAP:
				case ItemPool.MARINARA_TRENCH_MAP:
				case ItemPool.ANEMONE_MINE_MAP:
				case ItemPool.DIVE_BAR_MAP:
				case ItemPool.SKATE_PARK_MAP:
					return !ItemDatabase.haveVirtualItem( itemId );
				case ItemPool.DAMP_OLD_BOOT:
					return !Preferences.getBoolean( "dampOldBootPurchased" );
				case ItemPool.BLACK_GLASS:
					return BigBrotherRequest.BLACK_GLASS.getCount( KoLConstants.inventory ) == 0;
				case ItemPool.FOLDER_19:
					return KoLCharacter.hasEquipped( EquipmentManager.FOLDER_HOLDER );
				}
				return super.canBuyItem( itemId );
			}
		};

	public static final AdventureResult AERATED_DIVING_HELMET = ItemPool.get( ItemPool.AERATED_DIVING_HELMET, 1 );
	public static final AdventureResult SCUBA_GEAR = ItemPool.get( ItemPool.SCUBA_GEAR, 1 );
	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult AMPHIBIOUS_TOPHAT = ItemPool.get( ItemPool.AMPHIBIOUS_TOPHAT, 1 );
	public static final AdventureResult BUBBLIN_STONE = ItemPool.get( ItemPool.BUBBLIN_STONE, 1 );
	public static final AdventureResult OLD_SCUBA_TANK = ItemPool.get( ItemPool.OLD_SCUBA_TANK, 1 );
	public static final AdventureResult SCHOLAR_MASK = ItemPool.get( ItemPool.SCHOLAR_MASK, 1 );
	public static final AdventureResult GLADIATOR_MASK = ItemPool.get( ItemPool.GLADIATOR_MASK, 1 );
	public static final AdventureResult CRAPPY_MASK = ItemPool.get( ItemPool.CRAPPY_MASK, 1 );

	private static AdventureResult self = null;
	private static AdventureResult familiar = null;
	private static boolean rescuedBigBrother = false;

	public BigBrotherRequest()
	{
		super( BigBrotherRequest.BIG_BROTHER );
	}

	public BigBrotherRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BigBrotherRequest.BIG_BROTHER, buying, attachments );
	}

	public BigBrotherRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BigBrotherRequest.BIG_BROTHER, buying, attachment );
	}

	public BigBrotherRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BigBrotherRequest.BIG_BROTHER, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BigBrotherRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( !location.contains( "who=2" ) || !responseText.contains( "sand dollar" ) )
			{
				return;
			}

			// We know for sure that we have rescued Big Brother
			// this ascension
			Preferences.setBoolean( "bigBrotherRescued", true );

			// Parse current coin balances
			CoinMasterRequest.parseBalance( data, responseText );

			// Look at his inventory
			Preferences.setBoolean( "dampOldBootPurchased", !responseText.contains( "damp old boot" ) );
			Preferences.setBoolean( "mapToMadnessReefPurchased", !responseText.contains( "map to Madness Reef" ) );
			Preferences.setBoolean( "mapToTheMarinaraTrenchPurchased", !responseText.contains( "map to the Marinara Trench" ) );
			Preferences.setBoolean( "mapToAnemoneMinePurchased", !responseText.contains( "map to Anemone Mine" ) );
			Preferences.setBoolean( "mapToTheDiveBarPurchased", !responseText.contains( "map to the Dive Bar" ) );
			Preferences.setBoolean( "mapToTheSkateParkPurchased", !responseText.contains( "map to the Skate Park" ) );

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );

		int itemId = CoinMasterRequest.extractItemId( data, location );
		switch ( itemId )
		{
		case ItemPool.MADNESS_REEF_MAP:
			if ( responseText.contains( "Big Brother shows you the map" ) )
			{
				Preferences.setBoolean( "mapToMadnessReefPurchased", true );
			}
			break;
		case ItemPool.MARINARA_TRENCH_MAP:
			if ( responseText.contains( "Big Brother shows you the map" ) )
			{
				Preferences.setBoolean( "mapToTheMarinaraTrenchPurchased", true );
			}
			break;
		case ItemPool.ANEMONE_MINE_MAP:
			if ( responseText.contains( "Big Brother shows you the map" ) )
			{
				Preferences.setBoolean( "mapToAnemoneMinePurchased", true );
			}
			break;
		case ItemPool.DIVE_BAR_MAP:
			if ( responseText.contains( "Big Brother shows you the map" ) )
			{
				Preferences.setBoolean( "mapToTheDiveBarPurchased", true );
			}
			break;
		case ItemPool.SKATE_PARK_MAP:
			if ( responseText.contains( "Big Brother shows you the map" ) )
			{
				Preferences.setBoolean( "mapToTheSkateParkPurchased", true );
			}
			break;
		}
	}

	private static void update()
	{
		// Definitive checks that we've rescued Big Brother:
		// - We saw it happen
		// - You have a bubblin' stone (a quest item)
		// - We have visited his store

		BigBrotherRequest.rescuedBigBrother = 
			Preferences.getBoolean( "bigBrotherRescued" ) ||
			InventoryManager.getAccessibleCount( BigBrotherRequest.BUBBLIN_STONE ) > 0;

		if ( InventoryManager.getAccessibleCount( BigBrotherRequest.AERATED_DIVING_HELMET ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.AERATED_DIVING_HELMET;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.SCHOLAR_MASK ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.SCHOLAR_MASK;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.GLADIATOR_MASK ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.GLADIATOR_MASK;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.CRAPPY_MASK ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.CRAPPY_MASK;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.SCUBA_GEAR ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.SCUBA_GEAR;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.OLD_SCUBA_TANK ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.OLD_SCUBA_TANK;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();

		// For the dancing frog, the amphibious tophat is the best familiar equipment
		if ( familiar.getId() == FamiliarPool.DANCING_FROG &&
		     InventoryManager.getAccessibleCount( BigBrotherRequest.AMPHIBIOUS_TOPHAT ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.AMPHIBIOUS_TOPHAT;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.DAS_BOOT ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.DAS_BOOT;
		}
		else if ( InventoryManager.getAccessibleCount( BigBrotherRequest.BATHYSPHERE ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.BATHYSPHERE;
		}
	}

	public static String accessible()
	{
		BigBrotherRequest.update();

		if ( !BigBrotherRequest.rescuedBigBrother )
		{
			return "You haven't rescued Big Brother yet.";
		}

		if ( BigBrotherRequest.self == null && !KoLCharacter.currentBooleanModifier( "Adventure Underwater" ) )
		{
			return "You don't have the right equipment to adventure underwater.";
		}

		if ( BigBrotherRequest.familiar == null && !KoLCharacter.currentBooleanModifier( "Underwater Familiar" ) )
		{
			return "Your familiar doesn't have the right equipment to adventure underwater.";
		}

		return null;
	}

	@Override
	public void equip()
	{
		BigBrotherRequest.update();
		if ( !KoLCharacter.currentBooleanModifier( "Adventure Underwater" ) )
		{
			EquipmentRequest request = new EquipmentRequest( BigBrotherRequest.self );
			RequestThread.postRequest( request );
		}

		if ( !KoLCharacter.currentBooleanModifier( "Underwater Familiar" ) )
		{
			EquipmentRequest request = new EquipmentRequest( familiar );
			RequestThread.postRequest( request );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim monkeycastle.php?action=buyitem or
		// monkeycastle.php?who=2
		if ( !urlString.startsWith( "monkeycastle.php" ) )
		{
			return false;
		}

		if ( !urlString.contains( "action=buyitem" ) && !urlString.contains( "who=2" ) )
		{
			return false;
		}

		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
