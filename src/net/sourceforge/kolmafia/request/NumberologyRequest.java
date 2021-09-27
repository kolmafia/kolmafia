package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.NumberologyManager;

public class NumberologyRequest
	extends GenericRequest
{
	public static final Pattern SEED_PATTERN = Pattern.compile( "num=([^&]*)" );

	private int seed = -1;

	public NumberologyRequest( int seed )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1103" );
		this.addFormField( "option", "1" );
		this.addFormField( "num", String.valueOf( seed ) );
		this.seed = Math.abs( seed );
	}

	public static final int getSeed( final String urlString )
	{
		return GenericRequest.getNumericField( urlString, NumberologyRequest.SEED_PATTERN );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you already used all casts of Calculate the Universe today, punt
		if ( Preferences.getInteger( "skillLevel144" ) <= Preferences.getInteger( "_universeCalculated" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already Calculated the Universe today." );
			return;
		}

		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have time to Calculate the Universe right now." );
			return;
		}

		// If the specified seed will get you "try again", punt
		int result = NumberologyManager.numberology( this.seed );
		String prize = NumberologyManager.numberologyPrize( result );
		if ( prize == NumberologyManager.TRY_AGAIN )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Seed " + this.seed + " will result in Try Again." );
			return;
		}

		GenericRequest skillRequest = new GenericRequest( "runskillz.php" );
		skillRequest.addFormField( "action", "Skillz" );
		skillRequest.addFormField( "whichskill", String.valueOf( SkillPool.CALCULATE_THE_UNIVERSE ) );
		skillRequest.addFormField( "ajax", "1" );

		// Run it via GET
		String URLString = skillRequest.getFullURLString();
		skillRequest.constructURLString( URLString, false );
		skillRequest.run();

		// See what KoL has to say about it.
		String responseText = skillRequest.responseText;

		if ( responseText.contains( "You don't have that skill" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't know how to Calculate the Universe" );
			return;
		}

		if ( responseText.contains( "You don't have enough" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need at least 1 MP to Calculate the Universe" );
			return;
		}

		if ( responseText.contains( "You can't use that skill again today" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already Calculated the Universe today" );
			Preferences.setInteger( "_universeCalculated", Preferences.getInteger( "skillLevel144" ) );
			return;
		}

		if ( !responseText.contains( "whichchoice" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't Calculate the Universe" );
			return;
		}

		// Doing the Maths
		super.run();
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );
		if ( choice != 1103 )
		{
			return false;
		}

		int seed = NumberologyRequest.getSeed( urlString );
		if ( seed == -1 )
		{
			return false;
		}

		String message = "[" + KoLAdventure.getAdventureCount() + "] numberology " + NumberologyManager.numberology( seed );
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
