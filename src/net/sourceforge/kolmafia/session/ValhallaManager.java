package net.sourceforge.kolmafia.session;

import java.io.PrintStream;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class ValhallaManager

{
	private static final AdventureResult [] USABLE = new AdventureResult []
	{
		ItemPool.get( ItemPool.ELITE_SCROLL, 1 ),
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
	};

	public static void preAscension()
	{
		// Create a badass belt

		CreateItemRequest belt = CreateItemRequest.getInstance( ItemPool.BADASS_BELT );
		if ( belt != null && belt.getQuantityPossible() > 0 )
		{
			belt.setQuantityNeeded( belt.getQuantityPossible() );
			RequestThread.postRequest( belt );
		}

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
				RequestThread.postRequest( new UseItemRequest( item.getInstance( count ) ) );
			}
		}

		// Sell autosellable quest items

		ArrayList items = new ArrayList();
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

		// User-defined actions:
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "preAscensionScript" ) );

		// GenericRequest keys on the following preference to decide
		// whether to call ValhallaManager.onAscension()
		Preferences.setInteger( "lastBreakfast", 0 );
	}

	public static void onAscension()
	{
		KoLCharacter.reset();

		Preferences.increment( "knownAscensions", 1 );
		Preferences.setInteger( "lastBreakfast", -1 );

		KoLmafia.resetCounters();
		UntinkerRequest.reset();
		ValhallaManager.resetPerAscensionCounters();

		StaticEntity.getClient().resetSession();
	}

	public static void postAscension()
	{
		ItemDatabase.reset();

		RequestThread.openRequestSequence();
		StaticEntity.getClient().refreshSession();
		RequestThread.closeRequestSequence();

		EquipmentManager.updateEquipmentLists();
		ConcoctionDatabase.refreshConcoctions( true );

		// Reset certain settings that the player almost certainly will
		// use differently at the beginning of a run vs. at the end.

		MoodManager.setMood( "apathetic" );
		Preferences.setFloat( "hpAutoRecovery",	-0.05f );
		Preferences.setFloat( "mpAutoRecovery",	-0.05f );

		// Note the information in the session log
		// for recording purposes.

		ValhallaManager.logNewAscension();

		// The semirare counter is set in Valhalla.
		TurnCounter.startCounting( 70, "Semirare window begin loc=*", "lparen.gif" );
		TurnCounter.startCounting( 80, "Semirare window end loc=*", "rparen.gif" );

		// If you are in Beecore, watch out for wandering bees!
		if ( KoLCharacter.inBeecore() )
		{
			// Until the interval to the first bee is spaded, don't
			// bother setting a counter.
			//
			// TurnCounter.startCounting( 15, "Bee window begin loc=*", "lparen.gif" );
			// TurnCounter.startCounting( 20, "Bee window end loc=*", "rparen.gif" );
		}

		// User-defined actions:
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "postAscensionScript" ) );

		if ( Preferences.getBoolean( "autostartGalaktikQuest" ) )
		{
			RequestThread.postRequest( new GalaktikRequest( "startquest" ) );
		}

		// Pull a VIP key and report on whether a present is available
		ClanLoungeRequest.visitLounge();
	}

	private static final void logNewAscension()
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

		if ( KoLCharacter.inBeecore() )
		{
			sessionStream.print( "Bees Hate You " );
		}
		else if ( KoLCharacter.inFistcore() )
		{
			sessionStream.print( "Way of the Surprising Fist " );
		}
		else if ( KoLCharacter.isTrendy() )
		{
			sessionStream.print( "Trendy " );
		}
		else if ( KoLCharacter.canEat() && KoLCharacter.canDrink() )
		{
			sessionStream.print( "No-Path " );
		}
		else if ( KoLCharacter.canEat() )
		{
			sessionStream.print( "Teetotaler " );
		}
		else if ( KoLCharacter.canDrink() )
		{
			sessionStream.print( "Boozetafarian " );
		}
		else
		{
			sessionStream.print( "Oxygenarian " );
		}

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

	public static final void resetPerAscensionCounters()
	{
		Preferences.setFloat( "slimelingFullness", 0.0F );
		Preferences.setInteger( "slimelingStacksDropped", 0 );
		Preferences.setInteger( "slimelingStacksDue", 0 );
		Preferences.setInteger( "currentBountyItem", 0 );
		Preferences.setString( "currentHippyStore", "none" );
		Preferences.setString( "currentWheelPosition", "muscle" );
		Preferences.setString( "warProgress", "unstarted" );
		Preferences.setInteger( "fratboysDefeated", 0 );
		Preferences.setInteger( "hippiesDefeated", 0 );
		Preferences.setInteger( "guyMadeOfBeesCount", 0 );
		Preferences.setBoolean( "guyMadeOfBeesDefeated", false );
		Preferences.setInteger( "carboLoading", 0 );
		Preferences.setInteger( "sugarCounter4178", 0 );
		Preferences.setInteger( "sugarCounter4179", 0 );
		Preferences.setInteger( "sugarCounter4180", 0 );
		Preferences.setInteger( "sugarCounter4181", 0 );
		Preferences.setInteger( "sugarCounter4182", 0 );
		Preferences.setInteger( "sugarCounter4183", 0 );
		Preferences.setInteger( "sugarCounter4191", 0 );
		Preferences.setInteger( "meansuckerPrice", 400 );
		Preferences.setInteger( "mayflyExperience", 0 );
		Preferences.setString( "trapperOre", "chrome" );
		Preferences.setString( "louvreLayout", "" );
		Preferences.setInteger( "pendingMapReflections", 0 );
		Preferences.setString( "violetFogLayout", "" );
		Preferences.setString( "dolphinItem", "" );
		Preferences.setString( "spookyPuttyMonster", "" );
		Preferences.setString( "cameraMonster", "" );
		Preferences.setString( "photocopyMonster", "" );
		Preferences.setString( "telescope1", "" );
		Preferences.setString( "telescope2", "" );
		Preferences.setString( "telescope3", "" );
		Preferences.setString( "telescope4", "" );
		Preferences.setString( "telescope5", "" );
		Preferences.setString( "telescope6", "" );
		Preferences.setString( "telescope7", "" );
		Preferences.setInteger( "singleFamiliarRun", 0 );
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
		TurnCounter.clearCounters();
	}
}
