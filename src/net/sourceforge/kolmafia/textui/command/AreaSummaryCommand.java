package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

public class AreaSummaryCommand
	extends AbstractCommand
{
	public AreaSummaryCommand()
	{
		this.usage = " <location> - show summary data for the specified area.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLAdventure location = AdventureDatabase.getAdventure( parameters );
		if ( location == null )
		{
			return;
		}

		AreaCombatData data = AdventureDatabase.getAreaCombatData( location.toString() );
		if ( data == null )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html>" );
		data.getSummary( buffer, false );
		buffer.append( "</html>" );

		KoLmafiaCLI.showHTML( buffer.toString() );
	}
}
