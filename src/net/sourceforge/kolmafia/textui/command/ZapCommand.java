package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.ZapRequest;

public class ZapCommand
	extends AbstractCommand
{
	public ZapCommand()
	{
		this.usage = " <item> [, <item>]... - transform items with your wand.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		ZapCommand.zap( parameters );
	}

	public static void zap( final String parameters )
	{
		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Zap what?" );
			return;
		}

		AdventureResult[] items = ItemFinder.getMatchingItemList( parameters, KoLConstants.inventory );

		for ( AdventureResult item : items )
		{
			RequestThread.postRequest( new ZapRequest( item ) );
		}
	}
}
