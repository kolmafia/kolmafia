package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.request.BeerPongRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class BeerPongDecorator
{
	public static final void decorate( final StringBuffer buffer )
	{
		String insult = BeerPongRequest.findRicketsInsult( buffer.toString() );
		if ( insult == null )
		{
			return;
		}

		int index = BeerPongRequest.findPirateInsult( insult );
		if ( index == 0 )
		{
			return;
		}

		String retort = BeerPongRequest.pirateRetort( index );
		if ( retort == null )
		{
			return;
		}

		StringUtilities.singleStringReplace( buffer, ">" + retort, " selected>" + retort );
	}
}
