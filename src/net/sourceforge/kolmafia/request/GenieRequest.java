package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class GenieRequest
	extends GenericRequest
{
	private static boolean usingPocketWish = false;
	private static final Pattern WISH_PATTERN = Pattern.compile( "You have (\\d) wish" );

	private static final int COMBAT = 0;
	private static final int EFFECT = 1;
	private static final int ITEM = 2;
	private static final int MEAT = 3;
	private static final int STATS = 4;
	private static final int TROPHY = 5;

	private final String wish;

	public GenieRequest( final String wish )
	{
		super( "choice.php" );

		this.addFormField( "whichchoice", "1267" );
		this.addFormField( "wish", wish );
		this.addFormField( "option", "1" );
		this.wish = wish.toLowerCase().trim();
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	public static int parseWish( final String wish )
	{
		switch ( wish )
		{
			case "you were free": return GenieRequest.COMBAT;
			case "for a blessed rustproof +2 gray dragon scale mail":
			case "for a pony":
			case "for more wishes": return GenieRequest.ITEM;
			case "i was rich":
			case "i were rich": return GenieRequest.MEAT;
			case "i was a little bit taller":
			case "i were a little bit taller":
			case "i was a baller":
			case "i were a baller": 
			case "i was a rabbit in a hat with a bat":
			case "i were a rabbit in a hat with a bat":
			case "i was big":
			case "i were big": return GenieRequest.STATS;
		}

		if ( wish.startsWith( "to fight" ) || wish.startsWith( "to be fighting a" ) )
		{
			return GenieRequest.COMBAT;
		}
		else if ( wish.startsWith( "to be" ) || wish.startsWith( "i was" ) )
		{
			return GenieRequest.EFFECT;
		}
		else if ( wish.startsWith("for trophy") )
		{
			return GenieRequest.TROPHY;
		}

		return -1;
	}

	public static String getDesiredEffectName( final String wish )
	{
		if ( !GenieRequest.wishedForEffect( wish ) )
		{
			return null;
		}

		String effectName = wish.substring( 6 );

		int[] ids = EffectDatabase.getEffectIds(effectName, false);

		if ( ids.length != 1 )
		{
			return null;
		}

		return EffectDatabase.getEffectName( ids[ 0 ] );
	}

	public static boolean wishedForEffect( final String wish )
	{
		return GenieRequest.parseWish( wish ) == GenieRequest.EFFECT;
	}

	public static boolean wishedForCombat( final String wish )
	{
		return GenieRequest.parseWish( wish ) == GenieRequest.COMBAT;
	}


	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		int itemId = -1;
		if ( InventoryManager.hasItem( ItemPool.GENIE_BOTTLE ) && Preferences.getInteger( "_genieWishesUsed" ) < 3 )
		{
			itemId = ItemPool.GENIE_BOTTLE;
		}
		else if ( InventoryManager.hasItem( ItemPool.POCKET_WISH ) )
		{
			itemId = ItemPool.POCKET_WISH;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You do not have a genie bottle or pocket wish to use." );
			return;
		}

		InventoryManager.retrieveItem( itemId, 1, true, false, false );

		GenericRequest useRequest = new GenericRequest( "inv_use.php" );
		useRequest.addFormField( "whichitem", String.valueOf( itemId ) );
		if ( this.getAdventuresUsed() > 0 )
		{
			// set location to "None" for the benefit of
			// betweenBattleScripts
			Preferences.setString( "nextAdventure", "None" );
			RecoveryManager.runBetweenBattleChecks( true );
		}

		useRequest.run();

		super.run();
	}

	@Override
	public int getAdventuresUsed()
	{
		return GenieRequest.wishedForCombat( this.wish ) ? 1 : 0;
	}

	// You are using a pocket wish!
	// You have 2 wishes left today.
	// You have 1 wish left today.

	public static void visitChoice( final String responseText )
	{
		Matcher matcher = GenieRequest.WISH_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int wishesLeft = Integer.parseInt( matcher.group( 1 ) );
			Preferences.setInteger( "_genieWishesUsed", 3 - wishesLeft );
			GenieRequest.usingPocketWish = false;
		}
		else if ( responseText.contains( "You are using a pocket wish!" ) )
		{
			GenieRequest.usingPocketWish = true;
		}
	}

	public static void postChoice( final String responseText, final String wish )
	{
		String desiredEffect = GenieRequest.getDesiredEffectName( wish );

		if ( responseText.contains( "You acquire" ) ||
		     responseText.contains( "You gain" ) ||
		     responseText.contains( ">Fight!<" ) )
		{
			// Successful wish
			if ( desiredEffect != null && EffectDatabase.hasAttribute( desiredEffect, "nohookah" ) )
			{
				String message = desiredEffect + " is wishable, but KoLmafia thought it was not";
				RequestLogger.printLine(message);
				RequestLogger.updateSessionLog(message);
			}

			if ( GenieRequest.usingPocketWish )
			{
				ResultProcessor.removeItem( ItemPool.POCKET_WISH );
			}
			else
			{
				Preferences.increment( "_genieWishesUsed" );
			}
		}
		else
		{
			// Unsuccessful wish
			if ( desiredEffect != null && !EffectDatabase.hasAttribute( desiredEffect, "nohookah" ) )
			{
				String message = desiredEffect + " is not wishable, but KoLmafia thought it was";
				RequestLogger.printLine(message);
				RequestLogger.updateSessionLog(message);
			}
		}

		if ( responseText.contains( ">Fight!<" ) )
		{
			Preferences.increment( "_genieFightsUsed" );

			KoLAdventure.lastVisitedLocation = null;
			KoLAdventure.lastLocationName = null;
			KoLAdventure.lastLocationURL = "choice.php";
			KoLAdventure.setLastAdventure( "None" );
			KoLAdventure.setNextAdventure( "None" );

			EncounterManager.ignoreSpecialMonsters();

			String message = "[" + KoLAdventure.getAdventureCount() + "] genie summoned monster";
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}
	}

}
