package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;


public class MonorailCommand
	extends AbstractCommand
{
	public MonorailCommand()
	{
		this.usage = " - get Favored by Lyle";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( Preferences.getBoolean( "_lyleFavored" ) )
		{
			KoLmafia.updateDisplay( "You have already had a Lyle buff today" );
			return;
		}
		RequestThread.postRequest( new GenericRequest( "place.php?whichplace=monorail&action=monorail_lyle" ) );
	}
}
