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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class ValhallaManager
{
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
	};

	public static void preAscension()
	{
		// Untinker the Bitchin' meatcar

		if ( InventoryManager.hasItem( ItemPool.BITCHIN_MEATCAR ) )
		{
			RequestThread.postRequest( new UntinkerRequest( ItemPool.BITCHIN_MEATCAR ) );
		}

		// Create a badass belt

		CreateItemRequest belt = CreateItemRequest.getInstance( ItemPool.BADASS_BELT );
		if ( belt != null && belt.getQuantityPossible() > 0 )
		{
			belt.setQuantityNeeded( belt.getQuantityPossible() );
			RequestThread.postRequest( belt );
		}

		// Use any 31337 scrolls.

		AdventureResult scroll = ItemPool.get( ItemPool.ELITE_SCROLL, 1 );
		int count = scroll.getCount( KoLConstants.inventory );
		if ( count > 0 )
		{
			RequestThread.postRequest( new UseItemRequest( scroll.getInstance( count ) ) );
		}

		// Trade in gunpowder.

		if ( InventoryManager.hasItem( ItemPool.GUNPOWDER ) )
		{
			BreakfastManager.visitPyro();
		}

		// Sell autosellable quest items

		ArrayList items = new ArrayList();
		for ( int i = 0; i < ValhallaManager.AUTOSELLABLE.length; ++i )
		{
			AdventureResult item = ValhallaManager.AUTOSELLABLE[i];
			if ( KoLConstants.inventory.contains( item ) )
			{
				items.add( item );
			}
		}

		if ( items.size() > 0 )
		{
			SellStuffRequest request = new SellStuffRequest( items.toArray(), SellStuffRequest.AUTOSELL );
			RequestThread.postRequest( request );
		}

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
		RequestThread.openRequestSequence();

		StaticEntity.getClient().refreshSession();
		EquipmentManager.updateEquipmentLists();
		ConcoctionDatabase.refreshConcoctions();

		// Note the information in the session log
		// for recording purposes.

		MoodManager.setMood( "apathetic" );
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

		if ( KoLCharacter.canEat() && KoLCharacter.canDrink() )
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

		RequestThread.closeRequestSequence();
		
		// User-defined actions:
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "postAscensionScript" ) );
		
		if ( Preferences.getBoolean( "autostartGalaktikQuest" ) )
		{
			RequestThread.postRequest(
				new GenericRequest( "galaktik.php?action=startquest&pwd" ) );
		}
	}

	public static final void resetPerAscensionCounters()
	{
		Preferences.setInteger( "currentBountyItem", 0 );
		Preferences.setString( "currentHippyStore", "none" );
		Preferences.setString( "currentWheelPosition", "muscle" );
		Preferences.setString( "warProgress", "unstarted" );
		Preferences.setInteger( "fratboysDefeated", 0 );
		Preferences.setInteger( "hippiesDefeated", 0 );
		Preferences.setInteger( "guyMadeOfBeesCount", 0 );
		Preferences.setBoolean( "guyMadeOfBeesDefeated", false );
		Preferences.setString( "trapperOre", "chrome" );
		Preferences.setString( "louvreLayout", "" );
		Preferences.setString( "violetFogLayout", "" );
		Preferences.setString( "dolphinItem", "" );
		Preferences.setString( "spookyPuttyMonster", "" );
		Preferences.setString( "cameraMonster", "" );
		Preferences.setString( "telescope1", "" );
		Preferences.setString( "telescope2", "" );
		Preferences.setString( "telescope3", "" );
		Preferences.setString( "telescope4", "" );
		Preferences.setString( "telescope5", "" );
		Preferences.setString( "telescope6", "" );
		Preferences.setString( "telescope7", "" );
		TurnCounter.clearCounters();
	}

}
