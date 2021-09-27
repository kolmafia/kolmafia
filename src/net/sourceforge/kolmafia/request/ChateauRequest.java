package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.Limitmode;

public class ChateauRequest
	extends PlaceRequest
{
	private static final Pattern PAINTING_PATTERN = Pattern.compile( "Painting of a[n]? (.*?) \\(1\\)\" title" );

	private static final AdventureResult CHATEAU_MUSCLE = ItemPool.get( ItemPool.CHATEAU_MUSCLE, 1 );
	private static final AdventureResult CHATEAU_MYST = ItemPool.get( ItemPool.CHATEAU_MYST, 1 );
	private static final AdventureResult CHATEAU_MOXIE = ItemPool.get( ItemPool.CHATEAU_MOXIE, 1 );
	private static final AdventureResult CHATEAU_FAN = ItemPool.get( ItemPool.CHATEAU_FAN, 1 );
	private static final AdventureResult CHATEAU_CHANDELIER = ItemPool.get( ItemPool.CHATEAU_CHANDELIER, 1 );
	private static final AdventureResult CHATEAU_SKYLIGHT = ItemPool.get( ItemPool.CHATEAU_SKYLIGHT, 1 );
	private static final AdventureResult CHATEAU_BANK = ItemPool.get( ItemPool.CHATEAU_BANK, 1 );
	private static final AdventureResult CHATEAU_JUICE_BAR = ItemPool.get( ItemPool.CHATEAU_JUICE_BAR, 1 );
	private static final AdventureResult CHATEAU_PENS = ItemPool.get( ItemPool.CHATEAU_PENS, 1 );

	public static final AdventureResult CHATEAU_PAINTING = ItemPool.get( ItemPool.CHATEAU_WATERCOLOR, 1 );

	public static String ceiling = null;

	public ChateauRequest()
	{
		super( "chateau" );
	}

	public ChateauRequest( final String action )
	{
		super( "chateau", action );
	}

	public static void reset()
	{
		KoLConstants.chateau.clear();
		ChateauRequest.ceiling = null;
	}

	public static void refresh()
	{
		ChateauRequest.reset();
		if ( ChateauRequest.chateauAvailable() )
		{
			RequestThread.postRequest( new ChateauRequest() );
		}
	}

	@Override
	public void processResults()
	{
		ChateauRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		ChateauRequest.reset();

		Matcher paintingMatcher = ChateauRequest.PAINTING_PATTERN.matcher( responseText );
		if ( paintingMatcher.find() )
		{
			Preferences.setString( "chateauMonster", paintingMatcher.group(1) );
		}

		// nightstand
		if ( responseText.contains( "nightstand_mus.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MUSCLE );
		}
		else if ( responseText.contains( "nightstand_mag.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MYST );
		}
		else if ( responseText.contains( "nightstand_moxie.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MOXIE );
		}

		// ceiling
		if ( responseText.contains( "ceilingfan.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_FAN );
			ChateauRequest.ceiling = "ceiling fan";
		}
		else if ( responseText.contains( "chandelier.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_CHANDELIER );
			ChateauRequest.ceiling = "antler chandelier";
		}
		else if ( responseText.contains( "skylight.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "artificial skylight";
		}

		// desk
		if ( responseText.contains( "desk_bank.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_BANK );
		}
		else if ( responseText.contains( "desk_juice.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_JUICE_BAR );
		}
		else if ( responseText.contains( "desk_stat.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_PENS );
		}

		String action = GenericRequest.getAction( urlString );

		// Nothing more to do for a simple visit
		if ( action == null )
		{
			KoLCharacter.updateFreeRests( responseText.contains( "chateau_restlabelfree" ) );
			return;
		}

		// place.php?whichplace=chateau&action=chateau_restlabelfree
		// or action=cheateau_restlabel
		// or action=chateau_restbox
		if ( action.startsWith( "chateau_rest" ) ||
		     // It will be nice when KoL fixes this misspelling
		     action.startsWith( "cheateau_rest" ) )
		{
			Preferences.increment( "timesRested" );
			KoLCharacter.updateFreeRests( responseText.contains( "chateau_restlabelfree" ) );
			KoLCharacter.updateStatus();
		}
		else if ( action.startsWith( "chateau_desk" ) )
		{
			Preferences.setBoolean( "_chateauDeskHarvested", true );
		}
	}

	public static final void gainItem( final AdventureResult result )
	{
		switch ( result.getItemId () )
		{
		case ItemPool.CHATEAU_MUSCLE:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MYST );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MOXIE );
			break;
		case ItemPool.CHATEAU_MYST:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MYST );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MOXIE );
			break;
		case ItemPool.CHATEAU_MOXIE:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MOXIE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MYST );
			break;
		case ItemPool.CHATEAU_FAN:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_CHANDELIER );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "ceiling fan";
			break;
		case ItemPool.CHATEAU_CHANDELIER:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_CHANDELIER );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "antler chandelier";
			break;
		case ItemPool.CHATEAU_SKYLIGHT:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_SKYLIGHT );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_CHANDELIER );
			ChateauRequest.ceiling = "artificial skylight";
			break;
		case ItemPool.CHATEAU_BANK:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_BANK );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_JUICE_BAR );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_PENS );
			break;
		case ItemPool.CHATEAU_JUICE_BAR:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_JUICE_BAR );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_BANK );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_PENS );
			break;
		case ItemPool.CHATEAU_PENS:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_PENS );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_JUICE_BAR );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_BANK );
			break;
		}
	}

	public static final void parseShopResponse( final String urlString, final String responseText )
	{
		// Adjust for changes in rollover adventures/fights or free rests
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=chateau" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			// Nothing to log for simple visits
			return true;
		}

		String message = null;

		if ( action.startsWith( "chateau_desk" ) )
		{
			if ( Preferences.getBoolean( "_chateauDeskHarvested" ) )
			{
				// Claim this, but don't bother logging it
				return true;
			}
				
			if ( action.equals( "chateau_desk1" ) )
			{
				message = "Collecting Meat from Swiss piggy bank";
			}
			else if ( action.equals( "chateau_desk2" ) )
			{
				message = "Collecting potions from continental juice bar";
			}
			else if ( action.equals( "chateau_desk3" ) )
			{
				message = "Collecting pens from fancy stationery set";
			}
			else if ( action.equals( "chateau_desk" ) )
			{
				message = "Collecting swag from the item on your desk";
			}
		}
		if ( action.startsWith( "chateau_nightstand" ) ||
		     action.startsWith( "chateau_ceiling" ) )
		{
			// Clicking it gives you info, but does nothing.
			return true;
		}
		if ( action.startsWith( "chateau_painting" ) )
		{
			// Clicking painting redirects to a fight, unless
			// you've already fought today. Ignore that.
			return true;
		}
		else if ( action.startsWith( "chateau_rest" ) ||
			  // It will be nice when KoL fixes this misspelling
			  action.startsWith( "cheateau_rest" ))
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your bed in the Chateau";
		}

		if ( message == null )
		{
			// Log URL for anything else
			return false;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static boolean chateauAvailable()
	{	return Preferences.getBoolean( "chateauAvailable" ) &&
		       StandardRequest.isAllowed( "Items", "Chateau Mantegna room key" ) &&
		       !Limitmode.limitZone( "Mountain" ) &&
		       !KoLCharacter.inBadMoon() &&
		       !KoLCharacter.isKingdomOfExploathing();
	}

	public static boolean chateauRestUsable()
	{	return Preferences.getBoolean( "restUsingChateau" ) &&
		       ChateauRequest.chateauAvailable();
	}
}
