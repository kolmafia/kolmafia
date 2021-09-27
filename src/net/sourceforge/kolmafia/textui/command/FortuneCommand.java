package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanFortuneRequest;
import net.sourceforge.kolmafia.request.ClanFortuneRequest.Buff;

public class FortuneCommand
	extends AbstractCommand
{
	public FortuneCommand()
	{
		this.usage = " - buff mus|mys|mox|familiar|meat|item [word1 word2 word3] | <playername> [word1 word2 word3] - get a buff or an item, "
		           + "using preference-defined words if none are specified."
				   + "\nIf playername has spaces, cannot specify words, and does not support playernames with 3 spaces";
	}
	
	@Override
	public void run( final String cmd, String parameters )
	{
		// Check that your clan has a fortune teller
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to request from the clan fortune teller?" );
			return;
		}

		String[] params = parameters.split( "\\s" );

		if ( params[0].equals( "buff" ) || params[0].equals( "effect" ) )
		{
			if ( Preferences.getBoolean( "_clanFortuneBuffUsed" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You already received a buff from the clan fortune teller." );
				return;
			}
			String buffname = params[1];
			Buff buff;
			// get a buff
			if ( buffname.startsWith( "susie" ) || buffname.startsWith( "fam" ) )
			{
				buff = Buff.FAMILIAR;
			}
			else if ( buffname.startsWith( "hagnk" ) || buffname.startsWith( "item" ) )
			{
				buff = Buff.ITEM;
			}
			else if ( buffname.startsWith( "meat" ) )
			{
				buff = Buff.MEAT;
			}
			else if ( buffname.startsWith( "gunther" ) || buffname.startsWith( "mus" ) )
			{
				buff = Buff.MUSCLE;
			}
			else if ( buffname.startsWith( "gorgonzola" ) || buffname.startsWith( "mys" ) )
			{
				buff = Buff.MYSTICALITY;
			}
			else if ( buffname.startsWith( "shifty" ) || buffname.startsWith( "mox" ) )
			{
				buff = Buff.MOXIE;
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't a valid buff." );
				return;
			}
			
			if ( params.length == 2 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( buff ) );
			}
			else if ( params.length == 5 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( buff, params[2], params[3], params[4] ) );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You need to choose all 3 words or none of the words for your compatibility test." );
				return;
			}
		}
		else
		{
			if ( Preferences.getInteger( "_clanFortuneConsultUses" ) == 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You already consulted with a clanmate 3 times today." );
				return;
			}
			if ( params.length == 4 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( params[0], params[1], params[2], params[3] ) );
			}
			else
			{
				// If not 4 parameters, assume a name with spaces
				RequestThread.postRequest( new ClanFortuneRequest( parameters ) );
			}
		}
	}
}
