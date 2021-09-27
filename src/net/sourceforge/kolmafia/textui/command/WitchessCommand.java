package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;


public class WitchessCommand
	extends AbstractCommand
{
	public WitchessCommand()
	{
		this.usage = " - Get the Witchess buff";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !StandardRequest.isAllowed( "Items", "Witchess Set" ) )
		{
			return;
		}
		if ( Preferences.getBoolean( "_witchessBuff" ) )
		{
			KoLmafia.updateDisplay( "You already got your Witchess buff today." );
			return;
		}
		if ( Preferences.getInteger( "puzzleChampBonus") != 20 )
		{
			KoLmafia.updateDisplay( "You cannot automatically get a Witchess buff until all puzzles are solved." );
			return;
		}
		RequestThread.postRequest( new WitchessRequest() );
	}
}
