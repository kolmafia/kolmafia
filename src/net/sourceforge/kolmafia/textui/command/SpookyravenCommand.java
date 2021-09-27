package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.LightsOutManager;
import net.sourceforge.kolmafia.session.TurnCounter;

public class SpookyravenCommand
	extends AbstractCommand
{
	public SpookyravenCommand()
	{
		this.usage = " - display quest statuses; [on|off] - turn counter tracking on/off; "
		           + "[stephen|elizabeth] - mark that quest as completed";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			LightsOutManager.report();
		}

		if ( parameters.equals( "on" ) )
		{
			Preferences.setBoolean( "trackLightsOut", true );
			LightsOutManager.checkCounter();
			return;
		}

		if ( parameters.equals( "off" ) )
		{
			Preferences.setBoolean( "trackLightsOut", false );
			TurnCounter.stopCounting( "Spookyraven Lights Out" );
			return;
		}

		if ( parameters.equals( "elizabeth" ) )
		{
			Preferences.setString( "nextSpookyravenElizabethRoom", "none" );
			return;
		}

		if ( parameters.equals( "stephen" ) )
		{
			Preferences.setString( "nextSpookyravenStephenRoom", "none" );
			return;
		}
	}

}
