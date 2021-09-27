package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;

public class ChipsCommand
	extends AbstractCommand
{
	public ChipsCommand()
	{
		this.usage = " type [,type [,type]] - buy chips from your clan's snack machine";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of chips do you want?" );
			return;
		}

		String[] split = parameters.split( "," );
		if ( split.length < 1 || split.length > 3 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Specify from 1 to 3 kinds of chip" );
			return;
		}

		int [] option = new int[ split.length ];
		for ( int i = 0; i < split.length; ++i )
		{
			String flavor = split[i].trim();
			option[i] = ClanRumpusRequest.findChips( flavor );
			if ( option[i] == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't buy '" + flavor + "' chips" );
				return;
			}
		}

		for ( int i = 0; i < option.length; ++i )
		{
			RequestThread.postRequest( new ClanRumpusRequest( RequestType.CHIPS, option[i] ) );
		}
	}
}
