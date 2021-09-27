package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCPurchaseRequest
	extends PurchaseRequest
{
	private static final AdventureResult TROUSERS = ItemPool.get( ItemPool.TRAVOLTAN_TROUSERS, 1 );
	private static final AdventureResult FLEDGES = ItemPool.get( ItemPool.PIRATE_FLEDGES, 1 );
	private final static AdventureResult SUPER_SKILL = EffectPool.get( EffectPool.SUPER_SKILL );
	private final static AdventureResult SUPER_STRUCTURE = EffectPool.get( EffectPool.SUPER_STRUCTURE );
	private final static AdventureResult SUPER_VISION = EffectPool.get( EffectPool.SUPER_VISION );
	private final static AdventureResult SUPER_SPEED = EffectPool.get( EffectPool.SUPER_SPEED );
	private final static AdventureResult SUPER_ACCURACY = EffectPool.get( EffectPool.SUPER_ACCURACY );

	public static final Pattern PIRATE_EPHEMERA_PATTERN =
		Pattern.compile( "pirate (?:brochure|pamphlet|tract)" );

	private static final Pattern NPCSHOPID_PATTERN = Pattern.compile( "whichshop=([^&]*)" );

	private final String npcStoreId;
	private final String quantityField;
	private final int row;

	/**
	 * Constructs a new <code>NPCPurchaseRequest</code> which retrieves things from NPC stores.
	 */

	public NPCPurchaseRequest( final String storeName, final String storeId, final int itemId, final int row, final int price )
	{
		this( storeName, storeId, itemId, row, price, PurchaseRequest.MAX_QUANTITY );
	}

	public NPCPurchaseRequest( final String storeName, final String storeId, final int itemId, final int row, final int price, final int quantity )
	{
		super( NPCPurchaseRequest.pickForm( storeId ) );

		this.isMallStore = false;
		this.item = ItemPool.get( itemId );

		this.shopName = storeName;
		this.npcStoreId = storeId;
		this.row = row;

		this.quantity = quantity;
		this.price = price;
		this.limit = this.quantity;
		this.canPurchase = true;

		this.timestamp = 0L;

		if ( this.row != 0 )
		{
			this.addFormField( "whichshop", storeId );
			this.addFormField( "action", "buyitem" );
			this.addFormField( "whichrow", String.valueOf( row ) );
			this.addFormField( "ajax", "1" );
			this.hashField = "pwd";
			this.quantityField = "quantity";
			return;
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );

		if ( storeId.equals( "town_giftshop.php" ) )
		{
			this.addFormField( "action", "buy" );
			this.hashField = "pwd";
			this.quantityField = "howmany";
		}
		else if ( storeId.equals( "fdkol" ) )
		{
			this.addFormField( "whichshop", storeId );
			this.addFormField( "action", "buyitem" );
			this.addFormField( "ajax", "1" );
			this.hashField = "pwd";
			this.quantityField = "quantity";
		}
		else
		{
			this.addFormField( "whichstore", storeId );
			this.addFormField( "buying", "1" );
			this.addFormField( "ajax", "1" );
			this.hashField = "phash";
			this.quantityField = "howmany";
		}
	}

	public static String pickForm( final String shopId )
	{
		if ( shopId.contains( "." ) )
		{
			return shopId;
		}
		return "shop.php";
	}

	public static String getShopId( final String urlString )
	{
		Matcher m = NPCPurchaseRequest.NPCSHOPID_PATTERN.matcher( urlString );
		return m.find() ? m.group( 1 ) : null;
	}

	public String getStoreId()
	{
		return this.npcStoreId;
	}

	@Override
	public int getQuantity()
	{
		switch ( this.getItemId() )
			{
			case ItemPool.FEDORA_MOUNTED_FOUNTAIN:
			case ItemPool.PORKPIE_MOUNTED_POPPER:
			case ItemPool.SOMBRERO_MOUNTED_SPARKLER:
				return Preferences.getBoolean( "_fireworksShopHatBought" ) ? 0 : 1;
			case ItemPool.CATHERINE_WHEEL:
			case ItemPool.ROCKET_BOOTS:
			case ItemPool.OVERSIZED_SPARKLER:
				return Preferences.getBoolean( "_fireworksShopEquipmentBought" ) ? 0 : 1;
			case ItemPool.BLART:
			case ItemPool.RAINPROOF_BARREL_CAULK:
			case ItemPool.PUMP_GREASE:
				return 1;
			}
		return super.getQuantity();
	}

	/**
	 * Retrieves the price of the item being purchased.
	 *
	 * @return The price of the item being purchased
	 */
	@Override
	public int getPrice()
	{
		long factor = 100;
		if ( this.shopName.equals( "Doc Galaktik's Medicine Show" ) && QuestDatabase.isQuestFinished( Quest.DOC ) )
		{
			// This is before the subtractions on purpose. It is possible that KoL
			// will change and this will need to be moved down.
			// The exact multiplier is 2/3 but with rounding this will give
			// the desired result.
			factor = 67;
		}
		if ( NPCPurchaseRequest.usingTrousers() ) factor -= 5;
		if ( KoLCharacter.hasSkill( "Five Finger Discount" ) ) factor -= 5;
		return (int) ( ( this.price * factor ) / 100 );
	}

	public static int currentDiscountedPrice( int price )
	{
		long factor = 100;
		if ( NPCPurchaseRequest.usingTrousers() ) factor -= 5;
		if ( KoLCharacter.hasSkill( "Five Finger Discount" ) ) factor -= 5;
		return (int)( ( price * factor ) / 100 );
	}

	private static boolean usingTrousers()
	{
		return EquipmentManager.getEquipment( EquipmentManager.PANTS ).equals( NPCPurchaseRequest.TROUSERS );
	}

	@Override
	public void run()
	{
		this.addFormField( this.quantityField, String.valueOf( this.limit ) );

		super.run();
	}

	@Override
	public boolean ensureProperAttire()
	{
		if ( this.npcStoreId.equals( "fdkol" ) )
		{
			// Travoltan trousers do not give a discount
			return true;
		}

		int neededOutfit = OutfitPool.NONE;

		switch ( this.npcStoreId )
		{
		case "bugbear":
			neededOutfit = OutfitPool.BUGBEAR_COSTUME;
			break;
		case "bartlebys":
			if ( !KoLCharacter.hasEquipped( NPCPurchaseRequest.FLEDGES ) )
			{
				neededOutfit = OutfitPool.SWASHBUCKLING_GETUP;
			}
			break;
		case "hippy":
			if ( this.shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				neededOutfit = OutfitPool.HIPPY_OUTFIT;
			}
			else if ( QuestLogRequest.isHippyStoreAvailable() )
			{
				neededOutfit = OutfitPool.NONE;
			}
			else if ( this.shopName.equals( "Hippy Store (Hippy)" ) )
			{
				neededOutfit = OutfitPool.WAR_HIPPY_OUTFIT;
			}
			else if ( this.shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				neededOutfit = OutfitPool.WAR_FRAT_OUTFIT;
			}
			break;
		}

		// Only switch outfits if the person is not currently wearing the outfit and if they
		// have that outfit.

		if ( neededOutfit != OutfitPool.NONE )
		{
			if ( EquipmentManager.isWearingOutfit( neededOutfit ) )
			{
				return true;
			}

			if ( !EquipmentManager.hasOutfit( neededOutfit ) )
			{
				return false;
			}
		}

		// If you have a buff from Greatest American Pants and have it set to keep the buffs,
		// disallow outfit changes.

		if ( Preferences.getBoolean( "gapProtection" ) )
		{
			if ( KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SKILL ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_STRUCTURE ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_VISION ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SPEED ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_ACCURACY ) )
			{
				if ( neededOutfit != OutfitPool.NONE )
				{
					KoLmafia.updateDisplay(
						MafiaState.ERROR,
						"You have a Greatest American Pants buff and buying the necessary " + getItemName() + " would cause you to lose it." );

					return false;
				}

				return true;
			}
		}

		// If the recovery manager is running, do not change equipment as this has the potential
		// for an infinite loop.

		if ( RecoveryManager.isRecoveryActive() && !MoodManager.isExecuting() )
		{
			if ( neededOutfit != OutfitPool.NONE )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR,
					"Aborting implicit outfit change due to potential infinite loop in auto-recovery. Please buy the necessary " + getItemName() + " manually." );

				return false;
			}

			return true;
		}

		// If there's an outfit that you need to use, change into it.

		if ( neededOutfit != OutfitPool.NONE )
		{
			( new EquipmentRequest( EquipmentDatabase.getOutfit( neededOutfit ) ) ).run();

			return true;
		}

		// Otherwise, maybe you can put on some Travoltan Trousers to decrease the cost of the
		// purchase, but only if auto-recovery isn't running.

		if ( !NPCPurchaseRequest.usingTrousers() && KoLConstants.inventory.contains( NPCPurchaseRequest.TROUSERS ) )
		{
			( new EquipmentRequest( NPCPurchaseRequest.TROUSERS, EquipmentManager.PANTS ) ).run();
		}

		return true;
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();

		if ( urlString.startsWith( "shop.php" ) )
		{
			NPCPurchaseRequest.parseShopResponse( urlString, this.responseText );
		}

		int quantityAcquired = this.item.getCount( KoLConstants.inventory ) - this.initialCount;

		if ( quantityAcquired > 0 )
		{
			// Normal NPC stores say "You spent xxx Meat" and we
			// have already parsed that.
			if ( !urlString.startsWith( "shop.php" ) )
			{
				ResultProcessor.processMeat( -1 * this.getPrice() * quantityAcquired );
				KoLCharacter.updateStatus();
			}

			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_giftshop.php" ) )
		{
			return false;
		}

		Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		String itemName = ItemDatabase.getItemName( itemId );
		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
		int priceVal = NPCStoreDatabase.price( itemId );

		Matcher m = NPCPurchaseRequest.NPCSHOPID_PATTERN.matcher(urlString);
		String shopId = m.find() ? NPCStoreDatabase.getStoreName( m.group(1) ) : null;
		String shopName = shopId != null ? shopId : "an NPC Store";

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + priceVal + " each from " + shopName );

		return true;
	}

	public static final int parseWhichRow( final String urlString )
	{
		String shopId = NPCPurchaseRequest.getShopId( urlString );
		return parseWhichRow( shopId, urlString );
	}

	public static final int parseWhichRow( final String shopId, final String urlString )
	{
		Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return -1;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		return NPCStoreDatabase.itemIdByRow( shopId, row );
	}

	public static final void parseShopRowResponse( final String urlString, final String responseText )
	{
		int itemId = parseWhichRow( urlString );

		if ( itemId == -1 )
		{
			return;
		}

		CreateItemRequest item = CreateItemRequest.getInstance( itemId, false );
		if ( item == null )
		{
			return; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = item.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 1 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		for ( AdventureResult ingredient : ingredients )
		{
			ResultProcessor.processResult(
					ingredient.getInstance( -1 * ingredient.getCount() * quantity ) );
		}
	}
	
	private static final Pattern ITEM_PATTERN = Pattern.compile( "<tr rel=\\\"(\\d+).*?descitem.(\\d+)\\)'><b>(.*?)(?:<font.*)?</b>.*?title=\\\"(.*?)\\\">.*?<b>(.*?)</b>.*?whichrow=(\\d+)", Pattern.DOTALL );
	private static final Pattern SHOP_NAME_PATTERN = Pattern.compile(  "bgcolor=blue><b>(.*?)</b>" , Pattern.DOTALL );
	private static final Pattern BLOOD_MAYO_PATTERN = Pattern.compile( "blood mayonnaise concentration: (\\d+) mayograms" );

	public static final void learnNPCStoreItem( final String shopName, final String shopId, final String name, final String cost, final String row )
	{
		String printMe;
		// Print what goes in npcstores.txt
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = shopName + "\t" + shopId + "\t" + name + "\t" + cost + "\tROW" + row;
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
	}

	public static final void learnCoinmasterItem( final String shopName, final String name, final String cost, final String row )
	{
		String printMe;
		// Print what goes in coinmasters.txt
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = shopName + "\tbuy\t" + cost + "\t" + name + "\tROW" + row;
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
	}

	public static final void parseShopResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) )
		{
			return;
		}

		String shopId = NPCPurchaseRequest.getShopId( urlString );
		if ( shopId == null )
		{
			return;
		}

		int boughtItemId = parseWhichRow( shopId, urlString );

		// Learn new items by simply visiting a store
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group(1) );
			String desc = matcher.group(2);
			String name = matcher.group(3);
			String data = ItemDatabase.getItemDataName( id );
			if ( data == null || !data.equals( name ) )
			{
				// Unknown item in shop
				String currency = matcher.group(4);
				String cost = matcher.group(5).replaceAll( ",", "" );
				String row = matcher.group(6);
				String shopName = "";
				Matcher nameMatcher = SHOP_NAME_PATTERN.matcher( responseText );
				if ( nameMatcher.find() )
				{
					shopName = nameMatcher.group(1);
				}
				if ( shopName.equals( "Results:" ) && nameMatcher.find() )
				{
					shopName = nameMatcher.group(1);
				}
				if ( currency.equals( "Meat" ) )
				{
					NPCPurchaseRequest.learnNPCStoreItem( shopName, shopId, name, cost, row );
				}
				else
				{
					NPCPurchaseRequest.learnCoinmasterItem( shopName, name, cost, row );
				}
				ItemDatabase.registerItem( id, name, desc );
			}
		}

		// Quest tracker update
		if ( shopId.equals( "junkmagazine" ) )
		{
			if ( !QuestDatabase.isQuestLaterThan( Quest.HIPPY, "step1" ) )
			{
				QuestDatabase.setQuestProgress( Quest.HIPPY, "step2" );
			}
		}

		// The following trade collections of ingredients for an item
		if ( shopId.equals( "airport" ) ||
		     shopId.equals( "beergarden" ) ||
		     shopId.equals( "crimbo16" ) ||
		     shopId.equals( "crimbo19toys" ) ||
		     shopId.equals( "grandma" ) ||
		     shopId.equals( "junkmagazine" ) ||
		     shopId.startsWith( "kolhs_" ) ||
		     shopId.equals( "mystic" ) ||
		     shopId.equals( "rumple" ) ||
		     shopId.equals( "snowgarden" ) ||
		     shopId.equals( "spant" ) ||
		     shopId.equals( "starchart" ) ||
		     shopId.equals( "xo" ) )
		{
			NPCPurchaseRequest.parseShopRowResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "sugarsheets" ) )
		{
			SugarSheetRequest.parseResponse( urlString, responseText );
			return;
		}

		// The following does too, but is limited and needs extra parsing
		if ( shopId.equals( "still" ) )
		{
			StillRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "5dprinter" ) )
		{
			FiveDPrinterRequest.parseResponse( urlString, responseText );
			return;
		}

		// The following does too, but wants a special message
		if ( shopId.equals( "jarl" ) )
		{
			JarlsbergRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "chateau" ) )
		{
			ChateauRequest.parseShopResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "mayoclinic" ) )
		{
			if ( !responseText.contains( "Mayo" ) )
			{
				// We don't have it installed, maybe got here through URL manipulation?
				return;
			}
			boolean refreshConcoctions = false;
			AdventureResult currentWorkshed = CampgroundRequest.getCurrentWorkshedItem();
			if ( currentWorkshed == null || currentWorkshed.getItemId() != ItemPool.MAYO_CLINIC )
			{
				refreshConcoctions = true;
			}
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.MAYO_CLINIC );
			if ( urlString.contains( "ajax=1" ) )
			{
				return;
			}
			Matcher mayoMatcher = BLOOD_MAYO_PATTERN.matcher( responseText );
			if ( mayoMatcher.find() )
			{
				Preferences.setString( "mayoLevel", mayoMatcher.group( 1 ) );
			}
			if ( !urlString.contains( "buyitem" ) )
			{
				if ( responseText.contains( "miracle whip" ) )
				{
					Preferences.setBoolean( "_mayoDeviceRented", false );
					Preferences.setBoolean( "itemBoughtPerAscension8266", false );
				}
				else if ( responseText.contains( "mayo lance" ) )
				{
					Preferences.setBoolean( "_mayoDeviceRented", false );
					Preferences.setBoolean( "itemBoughtPerAscension8266", true );
				}
				else
				{
					Preferences.setBoolean( "_mayoDeviceRented", true );
					Preferences.setBoolean( "itemBoughtPerAscension8266", true );
				}
			}
			Preferences.setBoolean( "_mayoTankSoaked", !responseText.contains( "Soak in the Mayo Tank" ) );
			if ( refreshConcoctions )
			{
				ConcoctionDatabase.refreshConcoctions();
			}
			return;
		}

		if ( shopId.equals( "hiddentavern" ) )
		{
			// If Hidden Tavern not already unlocked, new items available
			if ( Preferences.getInteger( "hiddenTavernUnlock" ) != KoLCharacter.getAscensions() )
			{
				// Unlock Hidden Tavern
				Preferences.setInteger( "hiddenTavernUnlock", KoLCharacter.getAscensions() );
				ConcoctionDatabase.setRefreshNeeded( true );
			}
			return;
		}

		// The following are coinmasters

		if ( shopId.equals( "arcade" ) )
		{
			TicketCounterRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "armory" ) )
		{
			ArmoryAndLeggeryRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "blackmarket" ) )
		{
			// If Black Market not already unlocked, unlock it
			if ( !QuestLogRequest.isBlackMarketAvailable() )
			{
				QuestDatabase.setQuestProgress( Quest.MACGUFFIN, "step1" );
				ConcoctionDatabase.setRefreshNeeded( true );
			}
			BlackMarketRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "damachine" ) )
		{
			VendingMachineRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "driparmory" ) )
		{
			DripArmoryRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "shore" ) )
		{
			ShoreGiftShopRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "dv" ) )
		{
			TerrifiedEagleInnRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "guzzlr" ) )
		{
			GuzzlrRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "trapper" ) )
		{
			TrapperRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "cindy" ) )
		{
			BoutiqueRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "fdkol" ) )
		{
			FDKOLRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "fwshop" ) )
		{
			if ( responseText.contains( "<b>Combat Explosives" ) )
			{
				Preferences.setBoolean( "_fireworksShop", true );
				Preferences.setBoolean( "_fireworksShopHatBought", !responseText.contains( "<b>Dangerous Hats" ) );
				Preferences.setBoolean( "_fireworksShopEquipmentBought", !responseText.contains( "<b>Explosive Equipment" ) );
			}
			return;
		}

		if ( shopId.equals( "elvishp1" ) ||
		     shopId.equals( "elvishp2" ) ||
		     shopId.equals( "elvishp3" ) )
		{
			SpaaaceRequest.parseResponse( urlString, responseText );
			return;
		}

		// Spring Break Beach shops

		if ( shopId.equals( "sbb_jimmy" ) )
		{
			BuffJimmyRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "sbb_taco" ) )
		{
			TacoDanRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "sbb_brogurt" ) )
		{
			BrogurtRequest.parseResponse( urlString, responseText );
			return;
		}

		// Conspiracy Island shops

		if ( shopId.equals( "si_shop1" ) )
		{
			SHAWARMARequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "si_shop2" ) )
		{
			CanteenRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "si_shop3" ) )
		{
			ArmoryRequest.parseResponse( urlString, responseText );
			return;
		}

		// That 70s Volcano shops

		if ( shopId.equals( "infernodisco" ) )
		{
			DiscoGiftCoRequest.parseResponse( urlString, responseText );
			return;
		}

		// Dinsey Landfill shops

		if ( shopId.equals( "landfillstore" ) )
		{
			DinseyCompanyStoreRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "toxic" ) )
		{
			ToxicChemistryRequest.parseResponse( urlString, responseText );
			return;
		}

		// The Glaciest shops

		if ( shopId.equals( "glaciest" ) )
		{
			WalMartRequest.parseResponse( urlString, responseText );
			return;
		}

		// Twitch Shops

		if ( shopId.equals( "caveshop" ) )
		{
			NeandermallRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "shoeshop" ) )
		{
			ShoeRepairRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "applestore" ) )
		{
			AppleStoreRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "nina" ) )
		{
			NinjaStoreRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "shakeshop" ) )
		{
			YeNeweSouvenirShoppeRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "conmerch" ) )
		{
			MerchTableRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "topiary" ) )
		{
			NuggletCraftingRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "fishbones" ) )
		{
			FishboneryRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.startsWith( "crimbo14" ) )
		{
			Crimbo14Request.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "crimbo17" ) )
		{
			Crimbo17Request.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.startsWith( "crimbo20booze" ) )
		{
			Crimbo20BoozeRequest.parseResponse( urlString, responseText );
		}

		if ( shopId.startsWith( "crimbo20candy" ) )
		{
			Crimbo20CandyRequest.parseResponse( urlString, responseText );
		}

		if ( shopId.startsWith( "crimbo20food" ) )
		{
			Crimbo20FoodRequest.parseResponse( urlString, responseText );
		}

		if ( shopId.equals( "edunder_shopshop" ) )
		{
			EdShopRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "batman_cave" ) )
		{
			BatFabricatorRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "batman_chemicorp" ) )
		{
			ChemiCorpRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "batman_orphanage" ) )
		{
			GotporkOrphanageRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "batman_pd" ) )
		{
			GotporkPDRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "ltt" ) )
		{
			LTTRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "bacon" ) )
		{
			MemeShopRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "pokefam" ) )
		{
			PokemporiumRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "detective" ) )
		{
			PrecinctRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "thankshop" ) )
		{
			ThankShopRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "spacegate" ) )
		{
			SpacegateFabricationRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "fantasyrealm" ) )
		{
			RubeeRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "glover" ) )
		{
			GMartRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "piraterealm" ) )
		{
			FunALogRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "lathe" ) )
		{
			SpinMasterLatheRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "campfire" ) )
		{
			YourCampfireRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "exploathing" ) )
		{
			CosmicRaysBazaarRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "mariogear" ) )
		{
			PlumberGearRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "marioitems" ) )
		{
			PlumberItemRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "wildfire" ) )
		{
			if ( responseText.contains( "You acquire an item" ) )
			{
				switch ( boughtItemId )
				{
				case ItemPool.BLART:
					Preferences.setBoolean( "itemBoughtPerAscension10790", true );
					break;
				case ItemPool.RAINPROOF_BARREL_CAULK:
					Preferences.setBoolean( "itemBoughtPerAscension10794", true );
					break;
				case ItemPool.PUMP_GREASE:
					Preferences.setBoolean( "itemBoughtPerAscension10795", true );
					break;
				}
			}

			if ( !urlString.contains( "ajax=1" ) )
			{
				// B. L. A. R. T.
				Preferences.setBoolean( "itemBoughtPerAscension10790", !responseText.contains( "<tr rel=\"10790\">" ) );
				// rainproof barrel caulk
				Preferences.setBoolean( "itemBoughtPerAscension10794", !responseText.contains( "<tr rel=\"10794\">" ) );
				// pump grease
				Preferences.setBoolean( "itemBoughtPerAscension10795", !responseText.contains( "<tr rel=\"10795\">" ) );
				return;
			}
		}

		// When we purchase items from NPC stores using ajax, the
		// response tells us nothing about the contents of the store.
		if ( urlString.contains( "ajax=1" ) )
		{
			return;
		}

		// These shops have variable offerings
		if ( shopId.equals( "bartlebys" ) )
		{
			Matcher m = PIRATE_EPHEMERA_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				Preferences.setInteger( "lastPirateEphemeraReset", KoLCharacter.getAscensions() );
				Preferences.setString( "lastPirateEphemera", m.group( 0 ) );
			}
			return;
		}

		if ( shopId.equals( "hippy" ) )
		{
			// Check to see if any of the items offered in the
			// hippy store are special.

			String side = "none";

			if ( responseText.contains( "peach" ) &&
			     responseText.contains( "pear" ) &&
			     responseText.contains( "plum" ) )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "hippy";
			}
			else if ( responseText.contains( "bowl of rye sprouts" ) &&
			          responseText.contains( "cob of corn" ) &&
			          responseText.contains( "juniper berries" ) )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "fratboy";
			}

			Preferences.setString( "currentHippyStore", side );
			Preferences.setString( "sidequestOrchardCompleted", side );

			if ( responseText.contains( "Oh, hey, boss!  Welcome back!" ) )
			{
				Preferences.setBoolean( "_hippyMeatCollected", true );
			}
			return;
		}
	}

	public static final boolean registerShopRowRequest( final String urlString )
	{
		int itemId = parseWhichRow( urlString );

		if ( itemId == -1 )
		{
			return true;
		}

		CreateItemRequest item = CreateItemRequest.getInstance( itemId, false );
		if ( item == null )
		{
			return true; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = item.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 1 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		if ( quantity > item.getQuantityPossible() )
		{
			return true; // attempt will fail
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append( "Trade " );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				buffer.append( ", " );
			}

			buffer.append( ingredients[ i ].getCount() * quantity );
			buffer.append( " " );
			buffer.append( ingredients[ i ].getName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( buffer.toString() );

		return true;
	}

	public static final boolean registerShopRequest( final String urlString, boolean meatOnly )
	{
		if ( !urlString.startsWith( "shop.php" ) )
		{
			return false;
		}

		String shopId = NPCPurchaseRequest.getShopId( urlString );
		if ( shopId == null )
		{
			return false;
		}

		String shopName = NPCStoreDatabase.getStoreName( shopId );

		int itemId = -1;

		Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( m.find() )
		{
			itemId = StringUtilities.parseInt( m.group( 1 ) );
		}
		else
		{
			itemId = parseWhichRow( shopId, urlString );
		}

		if ( itemId == -1 )
		{
			// Just visiting the shop
			return true;
		}

		String itemName = ItemDatabase.getItemName( itemId );
		int priceVal = NPCStoreDatabase.price( itemId );

		// A "shop" can have items for Meat and also for tokens.
		// If  there is no Meat price, let correct class claim it.
		if ( priceVal == 0 )
		{
			// If we've already checked tokens, this is an unknown item
			if ( meatOnly )
			{
				return false;
			}

			// The following trade collections of ingredients for an item
			if ( shopId.equals( "5dprinter" ) ||
			     shopId.equals( "airport" ) ||
			     shopId.equals( "beergarden" ) ||
			     shopId.equals( "crimbo16" ) ||
			     shopId.equals( "crimbo19toys" ) ||
			     shopId.equals( "grandma" ) ||
			     shopId.equals( "junkmagazine" ) ||
			     shopId.startsWith( "kolhs_" ) ||
			     shopId.equals( "mystic" ) ||
			     shopId.equals( "rumple" ) ||
			     shopId.equals( "snowgarden" ) ||
			     shopId.equals( "spant" ) ||
			     shopId.equals( "xo" ) )
			{
				return NPCPurchaseRequest.registerShopRowRequest( urlString );
			}

			// The following does too, but always makes a single item
			if ( shopId.equals( "starchart" ) )
			{
				return StarChartRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "sugarsheets" ) )
			{
				return SugarSheetRequest.registerRequest( urlString );
			}

			// The following does too, but may be limited
			if ( shopId.equals( "still" ) )
			{
				return StillRequest.registerRequest( urlString );
			}

			// The following does too, but wants a special message
			if ( shopId.equals( "jarl" ) )
			{
				return JarlsbergRequest.registerRequest( urlString );
			}

			// The following are coinmasters

			if ( shopId.equals( "armory" ) )
			{
				return ArmoryAndLeggeryRequest.registerRequest( urlString, true );
			}

			if ( shopId.equals( "blackmarket" ) )
			{
				return BlackMarketRequest.registerRequest( urlString, true );
			}

			if ( shopId.equals( "damachine" ) )
			{
				return VendingMachineRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "driparmory" ) )
			{
				return DripArmoryRequest.registerRequest( urlString, true );
			}

			if ( shopId.equals( "guzzlr" ) )
			{
				return GuzzlrRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "shore" ) )
			{
				return ShoreGiftShopRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "dv" ) )
			{
				return TerrifiedEagleInnRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "trapper" ) )
			{
				return TrapperRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "cindy" ) )
			{
				return BoutiqueRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "fdkol" ) )
			{
				return FDKOLRequest.registerRequest( urlString, true );
			}

			// Spring Break Beach shops

			if ( shopId.equals( "sbb_jimmy" ) )
			{
				return BuffJimmyRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "sbb_taco" ) )
			{
				return TacoDanRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "sbb_brogurt" ) )
			{
				return BrogurtRequest.registerRequest( urlString );
			}

			// Conspiracy Island shops

			if ( shopId.equals( "si_shop1" ) )
			{
				return SHAWARMARequest.registerRequest( urlString );
			}

			if ( shopId.equals( "si_shop2" ) )
			{
				return CanteenRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "si_shop3" ) )
			{
				return ArmoryRequest.registerRequest( urlString );
			}

			// That 70s Volcano shops

			if ( shopId.equals( "infernodisco" ) )
			{
				return DiscoGiftCoRequest.registerRequest( urlString );
			}

			// Dinsey Landfill shops

			if ( shopId.equals( "landfillstore" ) )
			{
				return DinseyCompanyStoreRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "toxic" ) )
			{
				return ToxicChemistryRequest.registerRequest( urlString );
			}

			// The Glaciest shops

			if ( shopId.equals( "glaciest" ) )
			{
				return WalMartRequest.registerRequest( urlString );
			}

			// Twitch Shops

			if ( shopId.equals( "caveshop" ) )
			{
				return NeandermallRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "shoeshop" ) )
			{
				return ShoeRepairRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "applestore" ) )
			{
				return AppleStoreRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "nina" ) )
			{
				return NinjaStoreRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "shakeshop" ) )
			{
				return YeNeweSouvenirShoppeRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "conmerch" ) )
			{
				return MerchTableRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "arcade" ) )
			{
				return TicketCounterRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "fishbones" ) )
			{
				return FishboneryRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "topiary" ) )
			{
				return NuggletCraftingRequest.registerRequest( urlString );
			}

			if ( shopId.startsWith( "crimbo14" ) )
			{
				return Crimbo14Request.registerRequest( urlString );
			}

			if ( shopId.equals( "crimbo17" ) )
			{
				return Crimbo17Request.registerRequest( urlString );
			}

			if ( shopId.equals( "crimbo20booze" ) )
			{
				return Crimbo20BoozeRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "crimbo20candy" ) )
			{
				return Crimbo20CandyRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "crimbo20food" ) )
			{
				return Crimbo20FoodRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "edunder_shopshop" ) )
			{
				return EdShopRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "batman_cave" ) )
			{
				return BatFabricatorRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "batman_chemicorp" ) )
			{
				return ChemiCorpRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "batman_orphanage" ) )
			{
				return GotporkOrphanageRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "batman_pd" ) )
			{
				return GotporkPDRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "ltt" ) )
			{
				return LTTRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "bacon" ) )
			{
				return MemeShopRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "pokefam" ) )
			{
				return PokemporiumRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "detective" ) )
			{
				return PrecinctRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "thankshop" ) )
			{
				return ThankShopRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "spacegate" ) )
			{
				return SpacegateFabricationRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "fantasyrealm" ) )
			{
				return RubeeRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "glover" ) )
			{
				return GMartRequest.registerRequest( urlString );
			}

			return false;
		}

		Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			return true;
		}

		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + priceVal + " each from " + shopName );

		return true;
	}
}
