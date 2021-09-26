package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.BugbearManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BugbearsCommand
	extends AbstractCommand
{
	public BugbearsCommand()
	{
		this.usage = " - List progress of bugbear hunting.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		StringBuilder output = new StringBuilder();

		output.append( "<table border=2 cols=3>" );
		output.append( "<tr>" );
		output.append( "<th rowspan=2>Mothership Zone</th>" );
		output.append( "<th rowspan=2>Status</th>" );
		output.append( "<th rowspan=2>Bugbear</th>" );
		output.append( "<th>Location 1</th>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<th>Location 2</th>" );
		output.append( "</tr>" );

		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] data = BugbearManager.BUGBEAR_DATA[ i ];

			output.append( "<tr>" );
			output.append( "<td rowspan=2>" );
			output.append( BugbearManager.dataToShipZone( data ) );
			output.append( "</td>" );

			String setting = BugbearManager.dataToStatusSetting( data );
			String status = Preferences.getString( setting );
			String value =
				StringUtilities.isNumeric( status ) ?
				( status + "/" + BugbearManager.dataToLevel( data ) * 3 ) :
				status;

			output.append( "<td rowspan=2>" );
			output.append( value );
			output.append( "</td>" );

			output.append( "<td rowspan=2>" );
			output.append( BugbearManager.dataToBugbear( data ) );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( BugbearManager.dataToBugbearZone1( data ) );
			output.append( "</td>" );
			output.append( "</tr>" );

			output.append( "<tr>" );
			output.append( "<td>" );
			output.append( BugbearManager.dataToBugbearZone2( data ) );
			output.append( "</td>" );
			output.append( "</tr>" );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
