package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TerminalRequest
	extends GenericRequest
{
	public TerminalRequest( final String input )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1191" );
		this.addFormField( "option", "1" );
		this.addFormField( "input", input );
	}

	@Override
	public void run()
	{
		if ( !KoLCharacter.inNuclearAutumn() && !KoLConstants.campground.contains( ItemPool.get( ItemPool.SOURCE_TERMINAL ) ) )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "You don't have a Source terminal." );
			return;
		}
		if ( KoLCharacter.inNuclearAutumn() && !KoLConstants.falloutShelter.contains( ItemPool.get( ItemPool.SOURCE_TERMINAL ) ) )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "You don't have a Source terminal." );
			return;
		}
		if ( KoLCharacter.inNuclearAutumn() )
		{
			RequestThread.postRequest( new FalloutShelterRequest( "vault_term" ) );
		}
		else
		{
			RequestThread.postRequest( new CampgroundRequest( "terminal" ) );
		}
		super.run();
	}

	@Override
	public void processResults()
	{
		KoLmafia.updateDisplay( "Source Terminal used." );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1191 )
		{
			return false;
		}
		
		String input = GenericRequest.extractField( urlString, "input" );
		if ( input == null )
		{
			return false;
		}
		input = StringUtilities.globalStringReplace( input.substring( 6 ), "+", " " );

		String message = "Source Terminal: " + input;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
		return true;
	}
}
