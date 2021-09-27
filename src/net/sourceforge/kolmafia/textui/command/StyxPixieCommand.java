package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.HeyDezeRequest;

public class StyxPixieCommand
	extends AbstractCommand
{
	public StyxPixieCommand()
	{
		this.usage = " muscle | mysticality | moxie - get daily Styx Pixie buff.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Styx unless you are in Bad Moon." );
			return;
		}

		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equalsIgnoreCase( "muscle" ) )
		{
			RequestThread.postRequest( new HeyDezeRequest( Stat.MUSCLE ) );
		}
		else if ( command.equalsIgnoreCase( "mysticality" ) )
		{
			RequestThread.postRequest( new HeyDezeRequest( Stat.MYSTICALITY ) );
		}
		else if ( command.equalsIgnoreCase( "moxie" ) )
		{
			RequestThread.postRequest( new HeyDezeRequest( Stat.MOXIE ) );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can only buff muscle, mysticality, or moxie." );
			return;
		}
	}
}
