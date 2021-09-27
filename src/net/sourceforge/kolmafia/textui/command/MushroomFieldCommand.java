package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.session.MushroomManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MushroomFieldCommand
	extends AbstractCommand
{
	{
		this.usage = " [ plant <square> <type> | pick <square> | harvest ] - view or use your mushroom plot";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "plant" ) )
		{
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Syntax: field plant square spore" );
				return;
			}

			String squareString = split[ 1 ];
			int square = StringUtilities.parseInt( squareString );

			// Skip past command and square
			parameters = parameters.substring( command.length() ).trim();
			parameters = parameters.substring( squareString.length() ).trim();

			if ( parameters.indexOf( "mushroom" ) == -1 )
			{
				parameters = parameters.trim() + " mushroom";
			}

			int spore = ItemFinder.getFirstMatchingItem( parameters ).getItemId();

			if ( spore == -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unknown spore: " + parameters );
				return;
			}

			MushroomManager.plantMushroom( square, spore );
		}
		else if ( command.equals( "pick" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Syntax: field pick square" );
				return;
			}

			String squareString = split[ 1 ];

			int square = StringUtilities.parseInt( squareString );
			MushroomManager.pickMushroom( square, true );
		}
		else if ( command.equals( "harvest" ) )
		{
			MushroomManager.harvestMushrooms();
		}

		String plot = MushroomManager.getMushroomManager( false );

		if ( KoLmafia.permitsContinue() )
		{
            String plotDetails = "Current:" +
                    KoLConstants.LINE_BREAK +
                    "<code>" +
                    plot +
                    "</code>" +
                    KoLConstants.LINE_BREAK +
                    "Forecast:" +
                    KoLConstants.LINE_BREAK +
                    "<code>" +
                    MushroomManager.getForecastedPlot( false ) +
                    "</code>" +
                    KoLConstants.LINE_BREAK;
            RequestLogger.printLine( plotDetails );
		}
	}
}
