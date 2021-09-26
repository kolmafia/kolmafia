package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpacegateCommand
	extends AbstractCommand
{
	public SpacegateCommand()
	{
		this.usage = " vaccine [#], destination [#######|random] - Perform the specified action at the Spacegate Facility";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !Preferences.getBoolean( "spacegateAlways" ) && !Preferences.getBoolean( "_spacegateToday" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You are not cleared to access the Spacegate facility" );
			return;
		}

		String[] params = parameters.trim().split( "\\s+" );
		String command = params[0];

		if ( command.equals( "vaccine" ) )
		{
			if ( params.length < 2 )
			{
				RequestLogger.printLine( "Usage: spacegate " + this.usage );
				return;
			}
			int vaccine = StringUtilities.parseInt( params[1] );
			if ( vaccine < 1 || vaccine > 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Choose vaccine 1, 2, or 3" );
				return;
			}
			if ( Preferences.getBoolean( "_spacegateVaccine" ) )
			{
				RequestLogger.printLine( "You've already been vaccinated today" );
				return;
			}

			String setting = "spacegateVaccine" + params[1];

			// Visit the vaccinator to see if this vaccine is unlocked
			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=spacegate&action=sg_vaccinator" ) );

			if ( !Preferences.getBoolean( setting ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You have not unlocked that vaccine yet" );
				return;
			}

			// We know it, so, get it!
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1234&option=" + vaccine ) );
			return;
		}
		else if ( command.equals( "destination" ) )
		{
			if ( params.length < 2 )
			{
				RequestLogger.printLine( "Usage: spacegate " + this.usage );
				return;
			}
			if ( !Preferences.getString( "_spacegateCoordinates" ).equals( "" ) )
			{
				RequestLogger.printLine( "You've already chosen a destination today" );
				return;
			}

			String destination = params[1];
			if ( destination.equals( "random" ) )
			{
				RequestThread.postRequest( new GenericRequest( "place.php?whichplace=spacegate&action=sg_Terminal" ) );
				RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1235&option=3" ) );
				return;
			}

			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=spacegate&action=sg_Terminal" ) );
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1235&option=2&word=" + destination ) );
			return;
		}

		RequestLogger.printLine( "Usage: spacegate " + this.usage );
	}
}
