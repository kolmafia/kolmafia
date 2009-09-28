/**
 * 
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.SellStuffRequest;

public class AutoSellCommand
	extends AbstractCommand
{
	public AutoSellCommand()
	{
		this.usage = " <item> [, <item>]... - autosell items.";
	}

	public void run( final String cmd, final String parameters )
	{
		Object[] items = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );
		if ( items.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new SellStuffRequest( items, SellStuffRequest.AUTOSELL ) );
	}
}