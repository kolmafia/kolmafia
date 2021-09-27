package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.SorceressLairManager;

public class TelescopeRequest
	extends GenericRequest
{
	private static final Pattern WHERE_PATTERN = Pattern.compile( "campground.php.*action=telescope([^&]*)" );

	public static final int HIGH = 1;
	public static final int LOW = 2;

	private final int where;

	/**
	 * Constructs a new <code>TelescopeRequest</code>
	 */

	public TelescopeRequest( final int where )
	{
		super( "campground.php" );

		this.where = where;
		switch ( where )
		{
		case HIGH:
			this.addFormField( "action", "telescopehigh" );
			break;
		case LOW:
			this.addFormField( "action", "telescopelow" );
			break;
		}
	}

	@Override
	public void run()
	{
		if ( KoLCharacter.getTelescopeUpgrades() < 1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a telescope" );
			return;
		}

		if ( KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated() && KoLCharacter.getTelescopeUpgrades() > 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your telescope is unavailable in Bad Moon" );
			return;
		}

		if ( this.where != TelescopeRequest.HIGH && this.where != TelescopeRequest.LOW )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't look there." );
			return;
		}

		KoLmafia.updateDisplay( "Looking through your telescope..." );

		super.run();
	}

	@Override
	public void processResults()
	{
		TelescopeRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.where == TelescopeRequest.HIGH )
		{
			// "You've already peered into the Heavens
			// today. You're already feeling as inspired as you can
			// be for one day."
			if ( this.responseText.contains( "already peered" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You've already done that today." );
				return;
			}

			// Let regular effect parsing detect Starry-Eyed effect.
			super.processResults();
			return;
		}
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return;
		}

		if ( urlString.contains( "action=telescopehigh" ) )
		{
			Preferences.setBoolean( "telescopeLookedHigh", true );
			return;
		}

		if ( !urlString.contains( "action=telescopelow" ) )
		{
			return;
		}

		Preferences.setInteger( "lastTelescopeReset", KoLCharacter.getAscensions() );
		Preferences.setString( "telescope1", "" );
		Preferences.setString( "telescope2", "" );
		Preferences.setString( "telescope3", "" );
		Preferences.setString( "telescope4", "" );
		Preferences.setString( "telescope5", "" );
		Preferences.setString( "telescope6", "" );
		Preferences.setString( "telescope7", "" );
		Preferences.setString( "nsChallenge1", "none" );
		Preferences.setString( "nsChallenge2", "none" );
		Preferences.setString( "nsChallenge3", "none" );
		Preferences.setString( "nsChallenge4", "none" );
		Preferences.setString( "nsChallenge5", "none" );

		// In Bugbear Invasion, there is no point in looking low through your telescope
		if ( KoLCharacter.inBugcore() )
		{
			return;
		}

		int upgrades = 0;

		for ( int i = 0; i < TelescopeRequest.PATTERNS.length; ++i )
		{
			Pattern pattern = ( i == 0 && KoLCharacter.inBeecore() ) ?
				TelescopeRequest.BEECORE_PATTERN :
				TelescopeRequest.PATTERNS[ i ];

			Matcher matcher = pattern.matcher( responseText );
			if ( !matcher.find() )
			{
				break;
			}

			upgrades++;

			String test = matcher.group( 1 );
			String setting1 = "telescope" + upgrades;
			String setting2 = "nsChallenge" + upgrades;

			SorceressLairManager.parseChallenge( upgrades, test, setting1, setting2 );
		}

		int previousUpgrades = Preferences.getInteger( "telescopeUpgrades" );
		if ( upgrades == 5 && previousUpgrades > upgrades )
		{
			// There is no way to detect upgrades 6 and 7 here
			upgrades = previousUpgrades;
		}
		KoLCharacter.setTelescopeUpgrades( upgrades );
		Preferences.setInteger( "telescopeUpgrades", upgrades );
	}

	// You focus the telescope on the entrance of the cave, and see a mass
	// of bees surrounding it.
	private static final Pattern BEECORE_PATTERN =
		Pattern.compile( "see (.*?) surrounding it." );

	private static final Pattern[] PATTERNS =
	{
		// "You adjust the focus and see a second group of people <description>."
		Pattern.compile( "second group of people (.*?)\\." ),

		// "You scan to the right a bit and see a third group of <description>."
		Pattern.compile( "third group of (.*?)\\." ),

		// "You sweep the telescope up to reveal some <description>."
		Pattern.compile( "reveal some (.*?)\\." ),

		// "Beyond the maze's entrance you see <description>."
		Pattern.compile( "entrance you see (.*?)\\." ),

		// "You focus the telescope on the back side of the keep, somehow, and see a pipe <description>."
		Pattern.compile( "see a pipe (.*?)\\." ),
	};

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = TelescopeRequest.WHERE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "telescope look " + matcher.group( 1 ) );

		return true;
	}
}
