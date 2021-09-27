package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanLoungeRequest;

public class PoolCommand
	extends AbstractCommand
{
	public PoolCommand()
	{
		this.usage = " type [,type [,type]] - play pool games in your clan's VIP lounge";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What stance do you wish to take?" );
			return;
		}

		String[] split = parameters.split( "," );
		if ( split.length < 1 || split.length > 3 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Specify from 1 to 3 pool games" );
			return;
		}

		int [] option = new int[ split.length ];
		for ( int i = 0; i < split.length; ++i )
		{
			String tag = split[i].trim();
			option[i] = ClanLoungeRequest.findPoolGame( tag );
			if ( option[i] == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what a '" + tag + "' pool game is." );
				return;
			}
		}

		for ( int i = 0; i < option.length; ++i )
		{
			RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.POOL_TABLE, option[i] ) );
		}
	}
}
