package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;

public class JukeboxCommand
	extends AbstractCommand
{
	public JukeboxCommand()
	{
		this.usage = " song - listen to a song on your clan's jukebox";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Which song do you want to listen to?" );
			return;
		}

		int song = ClanRumpusRequest.findSong( parameters );
		if ( song == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of a song is that?" );
			return;
		}

		RequestThread.postRequest( new ClanRumpusRequest( RequestType.JUKEBOX, song ) );
	}
}
