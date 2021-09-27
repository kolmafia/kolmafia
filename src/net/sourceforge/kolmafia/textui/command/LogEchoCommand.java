package net.sourceforge.kolmafia.textui.command;

import java.util.Date;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LogEchoCommand
	extends AbstractCommand
{
	public LogEchoCommand()
	{
		this.usage = " timestamp | <text> - include timestamp or text in the session log only.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( parameters.equalsIgnoreCase( "timestamp" ) )
		{
			parameters = HolidayDatabase.getCalendarDayAsString( new Date() );
		}

		parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.getSessionStream().println( " > " + parameters );
	}
}
