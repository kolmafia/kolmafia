package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.IslandRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcertCommand
	extends AbstractCommand
{
	public ConcertCommand()
	{
		this.usage = " m[oon'd] | d[ilated pupils] | o[ptimist primal] | e[lvish] | wi[nklered] | wh[ite-boy angst]";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String arg = parameters.trim();

		IslandRequest request =
			Character.isDigit( arg.charAt( 0 ) ) ?
			IslandRequest.getConcertRequest( StringUtilities.parseInt( arg ) ) :
			IslandRequest.getConcertRequest( arg );

		if ( request == null )
		{
			String error = IslandRequest.concertError( arg );
			KoLmafia.updateDisplay( MafiaState.ERROR, error );
			return;
		}

		RequestThread.postRequest( request );
	}
}
