package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.MomRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MomCommand
	extends AbstractCommand
{
	public MomCommand()
	{
		this.usage = " hot | cold | stench | spooky | sleaze | critical | stats - get daily food.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = null;

		if ( split.length == 1 && !split[ 0 ].equals( "" ) )
		{
			command = split[ 0 ];
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Syntax: mom hot|cold|stench|spooky|sleaze|critical|stats" );
			return;
		}

		int action = 0;

		if ( Character.isDigit( command.charAt( 0 ) ) )
		{
			action = StringUtilities.parseInt( command );
		}
		else
		{
			for ( int i = 0; i < MomRequest.FOOD.length; ++i )
			{
				if ( command.equalsIgnoreCase( MomRequest.FOOD[ i ] ) )
				{
					action = i + 1;
					break;
				}
			}
		}

		if ( action < 1 || action > 7 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Syntax: mom hot|cold|stench|spooky|sleaze|critical|stats" );
			return;
		}

		RequestThread.postRequest( new MomRequest( action ) );
	}
}
