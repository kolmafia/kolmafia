package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CouncilRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.utilities.AdventureResultArray;

import java.io.PrintStream;

public class ValhallaManager

{
	private static final AdventureResult [] USABLE = new AdventureResult []
	{
		ItemPool.get( ItemPool.GATES_SCROLL, 1 ),
		ItemPool.get( ItemPool.FISHERMANS_SACK, 1 ),
		ItemPool.get( ItemPool.BONERDAGON_CHEST, 1 ),
	};

	private static final AdventureResult [] AUTOSELLABLE = new AdventureResult []
	{
		ItemPool.get( ItemPool.SMALL_LAMINATED_CARD, 1 ),
		ItemPool.get( ItemPool.LITTLE_LAMINATED_CARD, 1 ),
		ItemPool.get( ItemPool.NOTBIG_LAMINATED_CARD, 1 ),
		ItemPool.get( ItemPool.UNLARGE_LAMINATED_CARD, 1 ),
		ItemPool.get( ItemPool.DWARVISH_DOCUMENT, 1 ),
		ItemPool.get( ItemPool.DWARVISH_PAPER, 1 ),
		ItemPool.get( ItemPool.DWARVISH_PARCHMENT, 1 ),
		ItemPool.get( ItemPool.CULTIST_ROBE, 1 ),
		ItemPool.get( ItemPool.CREASED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.CRINKLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.CRUMPLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.FOLDED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RAGGED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RIPPED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RUMPLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.TORN_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RAVE_VISOR, 1 ),
		ItemPool.get( ItemPool.BAGGY_RAVE_PANTS, 1 ),
		ItemPool.get( ItemPool.PACIFIER_NECKLACE, 1 ),
		ItemPool.get( ItemPool.GLOWSTICK_ON_A_STRING, 1 ),
		ItemPool.get( ItemPool.CANDY_NECKLACE, 1 ),
		ItemPool.get( ItemPool.TEDDYBEAR_BACKPACK, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_RED_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_YELLOW_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_BLUE_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_ORANGE_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_GREEN_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_VIOLET_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_VERMILION_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_AMBER_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_CHARTREUSE_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_TEAL_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_INDIGO_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_PURPLE_SLIME, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_BROWN_SLIME, 1 ),
		ItemPool.get( ItemPool.FISH_OIL_SMOKE_BOMB, 1 ),
		ItemPool.get( ItemPool.VIAL_OF_SQUID_INK, 1 ),
		ItemPool.get( ItemPool.POTION_OF_FISHY_SPEED, 1 ),
		ItemPool.get( ItemPool.AUTOPSY_TWEEZERS, 1 ),
		ItemPool.get( ItemPool.GNOMISH_EAR, 1 ),
		ItemPool.get( ItemPool.GNOMISH_LUNG, 1 ),
		ItemPool.get( ItemPool.GNOMISH_ELBOW, 1 ),
		ItemPool.get( ItemPool.GNOMISH_KNEE, 1 ),
		ItemPool.get( ItemPool.GNOMISH_FOOT, 1 ),
	};

	private static final AdventureResult[] FREEPULL = new AdventureResult[]
	{
		ClanLoungeRequest.VIP_KEY,
		ItemPool.get( ItemPool.CURSED_KEG, 1 ),
		ItemPool.get( ItemPool.CURSED_MICROWAVE, 1 ),
	};

	public static void preAscension()
	{
		// Trade in gunpowder.

		if ( InventoryManager.hasItem( ItemPool.GUNPOWDER ) )
		{
			BreakfastManager.visitPyro();
		}

		// Use any usable quest items
		for ( int i = 0; i < ValhallaManager.USABLE.length; ++i )
		{
			AdventureResult item = ValhallaManager.USABLE[i];
			int count = item.getCount( KoLConstants.inventory );
			if ( count > 0 )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( item.getInstance( count ) ) );
			}
		}

		// Sell autosellable quest items

		AdventureResultArray items = new AdventureResultArray();
		for ( int i = 0; i < ValhallaManager.AUTOSELLABLE.length; ++i )
		{
			AdventureResult item = ValhallaManager.AUTOSELLABLE[i];
			int count = item.getCount( KoLConstants.inventory );
			if ( count > 0 )
			{
				items.add( item.getInstance( count ) );
			}
		}

		if ( items.size() > 0 )
		{
			AutoSellRequest request = new AutoSellRequest( items.toArray() );
			RequestThread.postRequest( request );
		}

		// Harvest your garden
		CampgroundRequest.harvestCrop();

		// Harvest your mushroom plot
		if ( MushroomManager.ownsPlot() )
		{
			MushroomManager.harvestMushrooms();
		}

		// Repackage bear arms
		AdventureResult leftArm = ItemPool.get( ItemPool.LEFT_BEAR_ARM, 1 );
		AdventureResult rightArm = ItemPool.get( ItemPool.RIGHT_BEAR_ARM, 1 );
		AdventureResult armBox = ItemPool.get( ItemPool.BOX_OF_BEAR_ARM, 1 );
		if ( KoLConstants.inventory.contains( leftArm ) && KoLConstants.inventory.contains( rightArm )
			 && !KoLConstants.inventory.contains( armBox ) )
		{
			UseItemRequest arm = UseItemRequest.getInstance( leftArm );
			RequestThread.postRequest( arm );
		}

		// As the final action before we enter the gash, run a user supplied script
		// If script aborts, we will not jump.
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "preAscensionScript" ) );
	}

	public static void onAscension()
	{
		// Save and restore chat literacy, since you are allowed to chat while in Valhalla
		boolean checkedLiteracy = ChatManager.checkedChatLiteracy();
		boolean chatLiterate = ChatManager.getChatLiteracy();

		KoLCharacter.reset( false );

		if ( checkedLiteracy )
		{
			ChatManager.setChatLiteracy( chatLiterate );
		}

		try
		{
			PreferenceListenerRegistry.deferPreferenceListeners( true );
			Preferences.increment( "knownAscensions", 1 );
			Preferences.setInteger( "lastBreakfast", -1 );
			KoLCharacter.setCurrentRun( 0 );

			KoLmafia.resetCounters();
			ValhallaManager.resetPerAscensionCounters();

			UntinkerRequest.reset();
			KoLCharacter.setGuildStoreOpen( false );
		}
		finally
		{
			PreferenceListenerRegistry.deferPreferenceListeners( false );
		}

		KoLmafia.resetSession();
	}

	public static void postAscension()
	{
		ItemDatabase.reset();

		CharPaneRequest.setInValhalla( false );
		KoLmafia.refreshSession();

		EquipmentManager.updateEquipmentLists();
		ValhallaManager.resetMoonsignCafes();
		ConcoctionDatabase.refreshConcoctions();
		ConsumablesDatabase.setSmoresData();
		ConsumablesDatabase.setAffirmationCookieData();
		ConsumablesDatabase.setVariableConsumables();
		ConsumablesDatabase.calculateAdventureRanges();
		HermitRequest.initialize();

		// Reset certain settings that the player almost certainly will
		// use differently at the beginning of a run vs. at the end.

		MoodManager.setMood( "apathetic" );
		Preferences.setFloat( "hpAutoRecovery",	-0.05f );
		Preferences.setFloat( "mpAutoRecovery",	-0.05f );

		// Note the information in the session log
		// for recording purposes.

		ValhallaManager.logNewAscension();

		// The semirare counter is set in Valhalla.
		if ( KoLCharacter.inLAR() )
		{
			TurnCounter.startCounting( 75, "Fortune Cookie", "fortune.gif" );
		}
		else
		{
			TurnCounter.startCounting( 70, "Semirare window begin", "lparen.gif" );
			TurnCounter.startCounting( 80, "Semirare window end loc=*", "rparen.gif" );
		}

		// First Rain monster expected on turns 9-11
		if ( KoLCharacter.inRaincore() )
		{
			TurnCounter.startCounting( 8, "Rain Monster window begin loc=*", "lparen.gif" );
			TurnCounter.startCounting( 10, "Rain Monster window end loc=*", "rparen.gif" );
		}

		// First West of Loathing monster expected on turns 6-11
		else if ( KoLCharacter.isWestOfLoathing() )
		{
			TurnCounter.startCounting( 5, "WoL Monster window begin loc=*", "lparen.gif" );
			TurnCounter.startCounting( 10, "WoL Monster window end loc=*", "rparen.gif" );
		}
		// Starting Source Enlightenment depends on current Source Points
		else if ( KoLCharacter.inTheSource() )
		{
			Preferences.setInteger( "sourceEnlightenment", Math.min( Preferences.getInteger( "sourcePoints" ), 11 ) );
		}
		else if ( KoLCharacter.isKingdomOfExploathing() )
		{
			// In Kingdom of Exploathing, you need to visit the council before you can visit place.php
			RequestThread.postRequest( new CouncilRequest() );
			// This will get you the telegram from Lady Spookyraven
			RequestThread.postRequest( new PlaceRequest( "manor1" ) );
			// And potentially other things. Refresh inventory.
			ApiRequest.updateInventory();
		}

		// User-defined actions:
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "postAscensionScript" ) );

		ValhallaManager.pullFreeItems();

		if ( Preferences.getBoolean( "autoQuest" ) )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.SPOOKYRAVEN_TELEGRAM, 1 ) ) );
		}

		// Standard paths can change what is available, so check clan
		ClanLoungeRequest.visitLounge();
		ClanLoungeRequest.visitLoungeFloor2();

		// Check hotdog stand, speakeasy, and floundry, if present
		if ( ClanManager.getClanLounge().contains( ClanManager.HOT_DOG_STAND ) )
		{
			ClanLoungeRequest.visitLounge( ClanLoungeRequest.HOT_DOG_STAND );
		}
		if ( ClanManager.getClanLounge().contains( ClanManager.SPEAKEASY ) )
		{
			ClanLoungeRequest.visitLounge( ClanLoungeRequest.SPEAKEASY );
		}
		if ( ClanManager.getClanLounge().contains( ClanManager.FLOUNDRY ) )
		{
			ClanLoungeRequest.visitLounge( ClanLoungeRequest.FLOUNDRY );
		}

		//force rebuild of daily deeds panel
		PreferenceListenerRegistry.firePreferenceChanged( "dailyDeedsOptions" );
	}

	// Pull items that
	private static void pullFreeItems()
	{
		for ( int i = 0; i < ValhallaManager.FREEPULL.length; ++i )
		{
			AdventureResult item = ValhallaManager.FREEPULL[i];
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				continue;
			}

			if ( item.getCount( KoLConstants.freepulls ) > 0 )
			{
				RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, item ) );
			}
		}
	}

	private static void resetMoonsignCafes()
	{
		// Change available items if they've changed due to ascension.
		if ( KoLCharacter.inBadMoon() && KoLConstants.kitchenItems.isEmpty() )
		{
			HellKitchenRequest.getMenu();
		}
		else if ( !KoLCharacter.inBadMoon() && !KoLConstants.kitchenItems.isEmpty() )
		{
			HellKitchenRequest.reset();
		}
		if ( KoLCharacter.canEat() && KoLCharacter.canadiaAvailable() && KoLConstants.restaurantItems.isEmpty() )
		{
			ChezSnooteeRequest.getMenu();
		}
		else if ( ( !KoLCharacter.canEat() || !KoLCharacter.canadiaAvailable() ) && !KoLConstants.restaurantItems.isEmpty() )
		{
			ChezSnooteeRequest.reset();
		}
		if ( KoLCharacter.canDrink() && KoLCharacter.gnomadsAvailable() && KoLConstants.microbreweryItems.isEmpty() )
		{
			MicroBreweryRequest.getMenu();
		}
		else if ( ( !KoLCharacter.canDrink() || !KoLCharacter.gnomadsAvailable() ) && !KoLConstants.microbreweryItems.isEmpty() )
		{
			MicroBreweryRequest.reset();
		}
	}

	private static void logNewAscension()
	{
		PrintStream sessionStream = RequestLogger.getSessionStream();

		sessionStream.println();
		sessionStream.println();
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println( "	   Beginning New Ascension	     " );
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println();

		sessionStream.println( "Ascension #" + KoLCharacter.getAscensions() + ":" );

		if ( KoLCharacter.isHardcore() )
		{
			sessionStream.print( "Hardcore " );
		}
		else
		{
			sessionStream.print( "Softcore " );
		}

		Path path = KoLCharacter.getPath();
		String pathName = ( path == Path.NONE ) ? "No-Path " : path.getName() + " ";
		sessionStream.print( pathName );

		sessionStream.println( KoLCharacter.getClassType() );

		sessionStream.println( KoLCharacter.getSign() );
		sessionStream.println();
		sessionStream.println();

		RequestLogger.printList( KoLConstants.availableSkills, sessionStream );
		sessionStream.println();

		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		sessionStream.println();
		sessionStream.println();
	}

	public static void resetPerAscensionCounters()
	{
		Preferences.setString( "banishingShoutMonsters", "" );
		Preferences.setFloat( "slimelingFullness", 0.0F );
		Preferences.setInteger( "slimelingStacksDropped", 0 );
		Preferences.setInteger( "slimelingStacksDue", 0 );
		Preferences.setInteger( "camelSpit", 0 );
		Preferences.setInteger( "campAwayDecoration", 0 );
		Preferences.setBoolean( "chaosButterflyThrown", false );
		Preferences.setString( "commerceGhostItem", "" );
		Preferences.setString( "crimbotChassis", "" );
		Preferences.setString( "crimbotArm", "" );
		Preferences.setString( "crimbotPropulsion", "" );
		Preferences.setString( "currentEasyBountyItem", "" );
		Preferences.setString( "currentHardBountyItem", "" );
		Preferences.setString( "currentSpecialBountyItem", "" );
		Preferences.setInteger( "crimboTreeDays", 7 );
		Preferences.setString( "currentHippyStore", "none" );
		Preferences.setString( "dnaSyringe", "" );
		Preferences.setString( "edPiece", "" );
		Preferences.setInteger( "eldritchTentaclesFought", 0 );
		Preferences.setString( "lastCopyableMonster", "" );
		Preferences.setInteger( "guzzlrDeliveryProgress", 0 );
		Preferences.setBoolean( "itemBoughtPerAscension637", false );
		Preferences.setBoolean( "itemBoughtPerAscension8266", false );
		Preferences.setBoolean( "itemBoughtPerAscension10790", false );
		Preferences.setBoolean( "itemBoughtPerAscension10794", false );
		Preferences.setBoolean( "itemBoughtPerAscension10795", false );
		Preferences.setString( "mayoInMouth", "" );
		Preferences.setInteger( "mayoLevel", 0 );
		Preferences.setString( "mayoMinderSetting", "" );
		Preferences.setBoolean( "mappingMonsters", false );
		Preferences.setString( "retroCapeSuperhero", "vampire" );
		Preferences.setString( "retroCapeWashingInstructions", "hold" );
		Preferences.setString( "snowsuit", "" );
		Preferences.setInteger( "guyMadeOfBeesCount", 0 );
		Preferences.setBoolean( "guyMadeOfBeesDefeated", false );
		Preferences.setInteger( "frenchGuardTurtlesFreed", 0 );
		Preferences.setInteger( "guardTurtlesFreed", 0 );
		Preferences.setBoolean( "hermitHax0red", false );
		Preferences.setInteger( "carboLoading", 0 );
		Preferences.setBoolean( "popularTartUnlocked", false );
		Preferences.setInteger( "smoresEaten", 0 );
		Preferences.setInteger( "affirmationCookiesEaten", 0 );
		Preferences.setInteger( "scrapbookCharges", 0 );
		Preferences.setInteger( "snojoMoxieWins", 0 );
		Preferences.setInteger( "snojoMuscleWins", 0 );
		Preferences.setInteger( "snojoMysticalityWins", 0 );
		Preferences.setString( "snojoSetting", "NONE" );
		Preferences.setInteger( "sugarCounter4178", 0 );
		Preferences.setInteger( "sugarCounter4179", 0 );
		Preferences.setInteger( "sugarCounter4180", 0 );
		Preferences.setInteger( "sugarCounter4181", 0 );
		Preferences.setInteger( "sugarCounter4182", 0 );
		Preferences.setInteger( "sugarCounter4183", 0 );
		Preferences.setInteger( "sugarCounter4191", 0 );
		Preferences.setInteger( "cozyCounter6332", 0 );
		Preferences.setInteger( "cozyCounter6333", 0 );
		Preferences.setInteger( "cozyCounter6334", 0 );
		Preferences.setInteger( "lastAnticheeseDay", -5 );
		Preferences.setInteger( "nextParanormalActivity", 1 );
		Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Storage Room" );
		Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Bedroom" );
		Preferences.setString( "nextQuantumFamiliar", "none" );
		Preferences.setInteger( "nextQuantumFamiliarTurn", 0 );
		Preferences.setInteger( "meansuckerPrice", 400 );
		Preferences.setInteger( "mayflyExperience", 0 );
		Preferences.setString( "trapperOre", "" );
		Preferences.setInteger( "twinpeakProgress", 0 );
		Preferences.setInteger( "gnasirProgress", 0 );
		Preferences.setString( "louvreLayout", "" );
		Preferences.setInteger( "pendingMapReflections", 0 );
		Preferences.setString( "violetFogLayout", "" );
		Preferences.setString( "dolphinItem", "" );
		Preferences.setInteger( "parasolUsed", 0 );
		Preferences.setInteger( "blankOutUsed", 0 );
		Preferences.setString( "telescope1", "" );
		Preferences.setString( "telescope2", "" );
		Preferences.setString( "telescope3", "" );
		Preferences.setString( "telescope4", "" );
		Preferences.setString( "telescope5", "" );
		Preferences.setString( "telescope6", "" );
		Preferences.setString( "telescope7", "" );
		Preferences.setInteger( "currentHedgeMazeRoom", 0 );
		Preferences.setInteger( "singleFamiliarRun", 0 );
		Preferences.setString( "plantingDate", "");
		Preferences.setInteger( "plantingDay", -1);
		Preferences.setInteger( "pyramidPosition", 1 );
		Preferences.setBoolean( "pyramidBombUsed", false );
		Preferences.setInteger( "jungCharge", 0 );
		Preferences.setInteger( "cubelingProgress", 0 );
		Preferences.setInteger( "grimstoneCharge", 0 );
		Preferences.setInteger( "miniAdvClass", 0 );
		Preferences.setInteger( "turtleBlessingTurns", 0 );
		Preferences.setString( "testudinalTeachings", "" );
		Preferences.setString( "yearbookCameraTarget", "" );
		Preferences.setBoolean( "yearbookCameraPending", false );
		Preferences.setInteger( "yearbookCameraUpgrades", Preferences.getInteger( "yearbookCameraAscensions" ) );
		Preferences.setString( "pastaThrall1", "" );
		Preferences.setString( "pastaThrall2", "" );
		Preferences.setString( "pastaThrall3", "" );
		Preferences.setString( "pastaThrall4", "" );
		Preferences.setString( "pastaThrall5", "" );
		Preferences.setString( "pastaThrall6", "" );
		Preferences.setString( "pastaThrall7", "" );
		Preferences.setString( "pastaThrall8", "" );
		Preferences.setBoolean( "prayedForGlamour", false );
		Preferences.setBoolean( "prayedForProtection", false );
		Preferences.setBoolean( "prayedForVigor", false );
		Preferences.setInteger( "rockinRobinProgress", 25 );
		Preferences.setInteger( "optimisticCandleProgress", 25 );
		Preferences.setInteger( "garbageFireProgress", 25 );
		Preferences.setInteger( "xoSkeleltonXProgress", 8 );
		Preferences.setInteger( "xoSkeleltonOProgress", 3 );
		Preferences.setString( "demonName12", "" );
		Preferences.setString( "sourceTerminalEnquiry", "" );
		Preferences.setString( "sourceTerminalEducate1", "" );
		Preferences.setString( "sourceTerminalEducate2", "" );
		Preferences.setInteger( "cornucopiasOpened", 0 );
		Preferences.setInteger( "bagOTricksCharges", 0 );
		Preferences.setString( "boomBoxSong", "" );
		Preferences.setInteger( "catBurglarBankHeists", 0 );
		Preferences.setString( "latteModifier", "" );
		Preferences.resetToDefault( "latteUnlocks" );
		Preferences.setInteger( "highTopPumped", 0 );
		Preferences.setInteger( "smutOrcNoncombatProgress", 0 );
		Preferences.setInteger( "unicornHornInflation", 0 );
		Preferences.setInteger( "redSnapperProgress", 0 );
		Preferences.setInteger( "aminoAcidsUsed", 0 );
		Preferences.setString( "cyrusAdjectives", "" );
		Preferences.setString( "copperheadClubHazard", "none" );
		Preferences.setInteger( "shenInitiationDay", 0 );
		Preferences.setInteger( "dripAdventuresSinceAscension", 0 );
		Preferences.setInteger( "drippingHallAdventuresSinceAscension", 0 );
		Preferences.setInteger( "drippingTreesAdventuresSinceAscension", 0 );
		Preferences.setInteger( "drippyJuice", 0 );
		Preferences.setBoolean( "lockPicked", false );
		Preferences.setBoolean( "leafletCompleted", false );
		Preferences.setInteger( "shockingLickCharges", 0 );
		Preferences.setString( "backupCameraMode", "meat" );
		Preferences.setBoolean( "backupCameraReverserEnabled", false );
		Preferences.setBoolean( "fireExtinguisherBatHoleUsed", false );
		Preferences.setBoolean( "fireExtinguisherChasmUsed", false );
		Preferences.setBoolean( "fireExtinguisherCyrptUsed", false );
		Preferences.setBoolean( "fireExtinguisherDesertUsed", false );
		Preferences.setBoolean( "fireExtinguisherHaremUsed", false );
		MonorailManager.resetMuffinOrder();
		// Copied monsters
		Preferences.setString( "cameraMonster", "" );
		Preferences.setString( "crappyCameraMonster", "" );
		Preferences.setString( "crudeMonster", "" );
		Preferences.setString( "envyfishMonster", "" );
		Preferences.setString( "iceSculptureMonster", "" );
		Preferences.setString( "photocopyMonster", "" );
		Preferences.setString( "rainDohMonster", "" );
		Preferences.setString( "screencappedMonster", "" );
		Preferences.setString( "spookyPuttyMonster", "" );
		Preferences.setString( "waxMonster", "" );
		Preferences.setString( "nosyNoseMonster", "" );
		Preferences.setString( "superficiallyInterestedMonster", "" );
		// Way of the Surprising Fist
		Preferences.setInteger( "charitableDonations", 0 );
		Preferences.setInteger( "fistSkillsKnown", 0 );
		Preferences.setBoolean( "fistTeachingsHaikuDungeon", false);
		Preferences.setBoolean( "fistTeachingsPokerRoom", false);
		Preferences.setBoolean( "fistTeachingsBarroomBrawl", false);
		Preferences.setBoolean( "fistTeachingsConservatory", false);
		Preferences.setBoolean( "fistTeachingsBatHole", false);
		Preferences.setBoolean( "fistTeachingsFunHouse", false);
		Preferences.setBoolean( "fistTeachingsMenagerie", false);
		Preferences.setBoolean( "fistTeachingsSlums", false);
		Preferences.setBoolean( "fistTeachingsFratHouse", false);
		Preferences.setBoolean( "fistTeachingsRoad", false);
		Preferences.setBoolean( "fistTeachingsNinjaSnowmen", false);
		// The Sea
		Preferences.setBoolean( "bigBrotherRescued", false );
		Preferences.setBoolean( "dampOldBootPurchased", false );
		Preferences.setInteger( "dreadScroll1", 0 );
		Preferences.setInteger( "dreadScroll2", 0 );
		Preferences.setInteger( "dreadScroll3", 0 );
		Preferences.setInteger( "dreadScroll4", 0 );
		Preferences.setInteger( "dreadScroll5", 0 );
		Preferences.setInteger( "dreadScroll6", 0 );
		Preferences.setInteger( "dreadScroll7", 0 );
		Preferences.setInteger( "dreadScroll8", 0 );
		Preferences.setInteger( "encountersUntilDMTChoice", 5 );
		Preferences.setInteger( "encountersUntilNEPChoice", 7 );
		Preferences.setInteger( "gladiatorBallMovesKnown", 0 );
		Preferences.setInteger( "gladiatorBladeMovesKnown", 0 );
		Preferences.setInteger( "gladiatorNetMovesKnown", 0 );
		Preferences.setInteger( "lastColosseumRoundWon", 0 );
		Preferences.setInteger( "lastCouncilVisit", 0 );
		Preferences.setInteger( "merkinVocabularyMastery", 0 );
		Preferences.setString( "lassoTraining", "" );
		Preferences.setBoolean( "mapToMadnessReefPurchased", false );
		Preferences.setBoolean( "mapToTheMarinaraTrenchPurchased", false );
		Preferences.setBoolean( "mapToAnemoneMinePurchased", false );
		Preferences.setBoolean( "mapToTheDiveBarPurchased", false );
		Preferences.setBoolean( "mapToTheSkateParkPurchased", false );
		Preferences.setString( "merkinLockkeyMonster", "" );
		Preferences.setString( "merkinQuestPath", "none" );
		Preferences.setString( "seahorseName", "" );
		Preferences.setString( "workteaClue", "" );
		// Grimstone Mask trackers
		Preferences.setInteger( "rumpelstiltskinTurnsUsed", 0 );
		Preferences.setInteger( "rumpelstiltskinKidsRescued", 0 );
		Preferences.setInteger( "cinderellaMinutesToMidnight", 0 );
		Preferences.setInteger( "cinderellaScore", 0 );
		Preferences.setString( "grimstoneMaskPath", "" );
		// Gingerbread City
		Preferences.setInteger( "gingerMuscleChoice", 0 );
		Preferences.setBoolean( "gingerSubwayLineUnlocked", false );
		Preferences.setBoolean( "gingerNegativesDropped", false );
		Preferences.setBoolean( "gingerBlackmailAccomplished", false );
		Preferences.setInteger( "gingerLawChoice", 0 );
		Preferences.setInteger( "gingerDigCount", 0 );
		// Spacegate
		Preferences.setInteger( "procrastinatorLanguageFluency", 0 );
		Preferences.setInteger( "spaceBabyLanguageFluency", 0 );
		Preferences.setInteger( "spacePirateLanguageFluency", 0 );
		// Garbage item charge
		Preferences.setInteger( "garbageChampagneCharge", 0 );
		Preferences.setInteger( "garbageShirtCharge", 0 );
		Preferences.setInteger( "garbageTreeCharge", 0 );
		// Boxing Daycare
		Preferences.setInteger( "daycareEquipment", 0 );
		Preferences.setInteger( "daycareToddlers", 0 );
		Preferences.setInteger( "daycareInstructors", 0 );
		// Sausage Grinder
		Preferences.setInteger( "sausageGrinderUnits", 0 );
		// Hewn Moon Rune Spoon
		Preferences.setBoolean( "moonTuned", false );
		// Cargo Cultist Shorts
		Preferences.setString( "cargoPocketsEmptied", "" );
		Preferences.setString( "cargoPocketScraps", "" );
		Preferences.setString( "demonName13", "" );
		// Community Service
		Preferences.setString( "csServicesPerformed", "" );	
		// AWOL
		Preferences.setInteger( "awolMedicine", 0 );
		Preferences.setInteger( "awolVenom", 0 );
		Preferences.increment( "awolPointsBeanslinger", Preferences.getInteger( "awolDeferredPointsBeanslinger" ) );
		Preferences.increment( "awolPointsCowpuncher", Preferences.getInteger( "awolDeferredPointsCowpuncher" ) );
		Preferences.increment( "awolPointsSnakeoiler", Preferences.getInteger( "awolDeferredPointsSnakeoiler" ) );
		Preferences.setInteger( "awolDeferredPointsBeanslinger", 0 );
		Preferences.setInteger( "awolDeferredPointsCowpuncher", 0 );
		Preferences.setInteger( "awolDeferredPointsSnakeoiler", 0 );
		// The Source trackers
		Preferences.resetToDefault( "sourceAgentsDefeated" );
		Preferences.resetToDefault( "sourceInterval" );
		Preferences.resetToDefault( "sourceOracleTarget" );
		Preferences.resetToDefault( "sourceEnlightenment" );
		// Nuclear Autumn
		Preferences.resetToDefault( "falloutShelterChronoUsed" );
		Preferences.resetToDefault( "falloutShelterCoolingTankUsed" );
		// Gelatinous Noob
		Preferences.increment( "noobPoints", Preferences.getInteger( "noobDeferredPoints" ) );
		// License to Adventure
		Preferences.setBoolean( "bondAdv", false );
		Preferences.setBoolean( "bondWpn", false );
		Preferences.setBoolean( "bondInit", false );
		Preferences.setBoolean( "bondDA", false );
		Preferences.setBoolean( "bondHP", false );
		Preferences.setBoolean( "bondItem2", false );
		Preferences.setBoolean( "bondStat", false );
		Preferences.setBoolean( "bondDrunk1", false );
		Preferences.setBoolean( "bondBooze", false );
		Preferences.setBoolean( "bondSymbols", false );
		Preferences.setBoolean( "bondDrunk2", false );
		Preferences.setBoolean( "bondJetpack", false );
		Preferences.setBoolean( "bondStealth", false );
		Preferences.setBoolean( "bondMartiniTurn", false );
		Preferences.setBoolean( "bondMeat", false );
		Preferences.setBoolean( "bondItem1", false );
		Preferences.setBoolean( "bondMus1", false );
		Preferences.setBoolean( "bondMys1", false );
		Preferences.setBoolean( "bondMox1", false );
		Preferences.setBoolean( "bondBeach", false );
		Preferences.setBoolean( "bondBeat", false );
		Preferences.setBoolean( "bondMartiniDelivery", false );
		Preferences.setBoolean( "bondMus2", false );
		Preferences.setBoolean( "bondMys2", false );
		Preferences.setBoolean( "bondMox2", false );
		Preferences.setBoolean( "bondMartiniPlus", false );
		Preferences.setBoolean( "bondBridge", false );
		Preferences.setBoolean( "bondWar", false );
		Preferences.setBoolean( "bondMPregen", false );
		Preferences.setBoolean( "bondWeapon2", false );
		Preferences.setBoolean( "bondItem3", false );
		Preferences.setBoolean( "bondStealth2", false );
		Preferences.setBoolean( "bondSpleen", false );
		Preferences.setBoolean( "bondStat2", false );
		Preferences.setBoolean( "bondDesert", false );
		Preferences.setBoolean( "bondHoney", false );
		// Pocket Familiars
		Preferences.setString( "pokefamBoosts", "" );
		// Kingdom Of Exploathing
		Preferences.setBoolean( "spaceInvaderDefeated", false );
		// Dark Gyffte
		Preferences.setString( "ensorcelee", "" );
		Preferences.setInteger( "ensorceleeLevel", 0 );

		// Path of the Plumber
		Preferences.setInteger( "plumberBadgeCost", 50 );
		Preferences.setInteger( "plumberCostumeCost", 50 );
		Preferences.setString( "plumberCostumeWorn", "none" );
		// You, Robot
		Preferences.setInteger( "youRobotBody", 0 );
		Preferences.setInteger( "youRobotBottom", 0 );
		Preferences.setString( "youRobotCPUUpgrades", "" );
		Preferences.setInteger( "youRobotLeft", 0 );
		Preferences.setInteger( "youRobotRight", 0 );
		Preferences.setInteger( "youRobotTop", 0 );
		Preferences.setInteger( "statbotUses", 0 );
		// Wildfire
		Preferences.setBoolean( "wildfireBarrelCaulked", false );
		Preferences.setBoolean( "wildfireDusted", false );
		Preferences.setBoolean( "wildfireFracked", false );
		Preferences.setBoolean( "wildfirePumpGreased", false );
		Preferences.setBoolean( "wildfireSprinkled", false );
		// Campground resets
		Preferences.setBoolean( "hasBartender", false );
		Preferences.setBoolean( "hasChef", false );
		Preferences.setBoolean( "hasCocktailKit", false );
		Preferences.setBoolean( "hasOven", false );
		Preferences.setBoolean( "hasRange", false );
		Preferences.setBoolean( "hasShaker", false );
		Preferences.setBoolean( "hasSushiMat", false );
		Preferences.setInteger( "mushroomGardenCropLevel", 1 );

		QuestDatabase.resetQuests();
		IslandManager.resetIsland();
		BanishManager.resetAscension();
		BugbearManager.resetStatus();

		TurnCounter.clearCounters();
		AdventureQueueDatabase.resetQueue();
		AdventureSpentDatabase.resetTurns();
	}
}
