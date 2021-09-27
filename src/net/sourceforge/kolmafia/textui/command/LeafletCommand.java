package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.session.LeafletManager;

public class LeafletCommand
	extends AbstractCommand
{
	public LeafletCommand()
	{
		this.usage = "  [nomagic] | location | command  - complete leaflet quest [without using magic words].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "" ) || parameters.equals( "stats" ) )
		{
			LeafletManager.robStrangeLeaflet( true );
			return;
		}
		if ( parameters.equals( "nomagic" ) )
		{
			LeafletManager.robStrangeLeaflet( false );
			return;
		}
		if ( parameters.equals( "location" ) )
		{
			String location = LeafletManager.locationName();
			KoLmafia.updateDisplay( "Current leaflet location: " + location );
			return;
		}
	}
}
