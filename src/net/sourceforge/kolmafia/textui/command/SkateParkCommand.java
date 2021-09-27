package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.SkateParkRequest;

public class SkateParkCommand
	extends AbstractCommand
{
	public SkateParkCommand()
	{
		this.usage = " lutz | comet | band shell | merry-go-round | eels - get daily Skate Park buff.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		int buff = SkateParkRequest.placeToBuff( parameters.trim() );
		if ( buff == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That's not a valid location in the Skate Park" );
			return;
		}

		RequestThread.postRequest( new SkateParkRequest( buff ) );
	}
}
