package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanLoungeRequest;

public class CrimboTreeCommand
	extends AbstractCommand
{
	public CrimboTreeCommand()
	{
		this.usage = " [get] - check [or get present from] the Crimbo Tree in your clan's VIP lounge";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( !parameters.equals( "get" ) )
		{
			KoLmafia.updateDisplay( "Check back in " + Preferences.getInteger( "crimboTreeDays" ) + " days." );
			return;
		}
		else if ( parameters.equals( "get" ) && Preferences.getInteger( "crimboTreeDays" ) > 0 )
		{
			RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE ) );
			KoLmafia.updateDisplay( "There's nothing under the Crimbo Tree with your name on it right now. Check back in " + Preferences.getInteger( "crimboTreeDays" ) + " days." );
			return;
		}

		RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE ) );
	}
}
