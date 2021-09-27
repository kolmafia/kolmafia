package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WildfireCampRequest
	extends PlaceRequest
{
	// 5: Inferno, 4: Raging, 3: Burning, 2: Smouldering, 1: Smoking, 0: Clear
	private static final Map<KoLAdventure, Integer> FIRE_LEVEL = new HashMap<>();

	public WildfireCampRequest()
	{
		super( "wildfire_camp" );
	}

	public WildfireCampRequest( final String action )
	{
		super( "wildfire_camp", action, true );
	}

	@Override
	public void processResults()
	{
		WildfireCampRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		if ( action == null )
		{
			return;
		}

		switch ( action )
		{
		case "wildfire_rainbarrel":
			parseRainbarrel( responseText );
			return;
		case "wildfire_oldpump":
			parsePump( responseText );
			return;
		}
	}

	private static void parseRainbarrel( final String responseText )
	{
		Preferences.setBoolean( "_wildfireBarrelHarvested", true );

		if ( responseText.contains( "You collect" ) )
		{
			Preferences.setBoolean( "wildfireBarrelCaulked", responseText.contains( "You collect 150 water" ) );
		}
	}

	private static void parsePump( final String responseText )
	{
		if ( responseText.contains( "You collect" ) )
		{
			Preferences.setBoolean( "wildfirePumpGreased", responseText.contains( "You collect 50 water" ) );
		}
	}

	private static final Pattern CAPTAIN_ZONE_COST = Pattern.compile("provide (\\d+) gallons of water" );
	private static final Pattern CAPTAIN_ZONE = Pattern.compile( "<option.*?value=\"(\\d+)\">.*? \\(.*?: (\\d)\\)</option>" );
	private static final Pattern CAPTAIN_REFILL = Pattern.compile( "It is only (\\d+)% full." );
	public static void parseCaptain( final String responseText )
	{
		Matcher zoneMatcher = CAPTAIN_ZONE.matcher( responseText );

		FIRE_LEVEL.clear();

		while ( zoneMatcher.find() )
		{
			int locationId = StringUtilities.parseInt( zoneMatcher.group( 1 ) );
			int level = StringUtilities.parseInt( zoneMatcher.group( 2 ) );

			KoLAdventure location = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + locationId );

			if ( location != null )
			{
				FIRE_LEVEL.put( location, level );
			}
		}

		Matcher refillMatcher = CAPTAIN_REFILL.matcher( responseText );

		if ( refillMatcher.find() )
		{
			int charge = StringUtilities.parseInt( refillMatcher.group( 1 ) );
			Preferences.setInteger( "_fireExtinguisherCharge", charge );
			Preferences.setBoolean( "_fireExtinguisherRefilled", false );
		}

		Matcher zoneCostMatcher = CAPTAIN_ZONE_COST.matcher( responseText );

		if ( zoneCostMatcher.find() )
		{
			int cost = StringUtilities.parseInt( zoneCostMatcher.group( 1 ) );
			Preferences.setInteger( "_captainHagnkUsed", cost / 10 );
		}
	}

	public static void refresh()
	{
		if ( KoLCharacter.inFirecore() )
		{
			RequestThread.postRequest( new WildfireCampRequest( "wildfire_captain" ) );
			RequestThread.postRequest( new GenericRequest( "choice.php?pwd&whichchoice=1451&option=2" ) );
		}
	}

	public static int getFireLevel( final KoLAdventure location )
	{
		return FIRE_LEVEL.getOrDefault( location, 5 );
	}

	public static void reduceFireLevel( final KoLAdventure location )
	{
		if ( location == null )
		{
			return;
		}

		FIRE_LEVEL.put( location, Math.max( 0, getFireLevel( location ) - 1 ) );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=wildfire_camp" ) )
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

		switch ( action )
		{
		case "wildfire_rainbarrel":
			message = "Harvesting the rain barrel";
			break;
		case "wildfire_oldpump":
			message = "[" + KoLAdventure.getAdventureCount() + "] Harvesting the water pump";
			break;
		case "wildfire_captain":
		case "wildfire_fracker":
		case "wildfire_cropster":
		case "wildfire_sprinklerjoe":
			break;
		default:
			return false;
		}

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}
}
