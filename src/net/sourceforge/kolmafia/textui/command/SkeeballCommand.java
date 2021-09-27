package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ArcadeRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SkeeballCommand
	extends AbstractCommand
{
	public SkeeballCommand()
	{
		this.usage = "[<count>] - squander Game Grid tokens at the broken Skeeball machine";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		int tokens = ArcadeRequest.TOKEN.getCount( KoLConstants.inventory );
		int count;

		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			count = 1;
		}
		else if ( parameters.equals( "*" ) )
		{
			count = tokens;
		}
		else if ( StringUtilities.isNumeric( parameters ) )
		{
			count = Math.min( StringUtilities.parseInt( parameters ), tokens );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "\"" + parameters + "\" doesn't look like a number." );
			return;
		}

		for ( int i = 0; i < count; ++i )
		{
			RequestThread.postRequest( new ArcadeRequest( "arcade_skeeball" ) );
		}
	}
}
