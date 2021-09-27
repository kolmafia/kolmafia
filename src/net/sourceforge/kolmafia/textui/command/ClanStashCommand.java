package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.ClanStashRequest;

import net.sourceforge.kolmafia.session.ClanManager;

public class ClanStashCommand
	extends AbstractCommand
{
	public ClanStashCommand()
	{
		this.usage = " [put] <item>... | take <item>... - exchange items with clan stash";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		List<AdventureResult> list = null;
		int direction = ClanStashRequest.ITEMS_TO_STASH;

		int space = parameters.indexOf( " " );
		if ( space != -1 )
		{
			String command = parameters.substring( 0, space );
			if ( command.equals( "take" ) )
			{
				direction = ClanStashRequest.STASH_TO_ITEMS;
				parameters = parameters.substring( 4 ).trim();
				list = ClanManager.getStash();
			}
			else if ( command.equals( "put" ) )
			{
				parameters = parameters.substring( 3 ).trim();
				list = KoLConstants.inventory;
			}
		}

		if ( list == null )
		{
			return;
		}

		AdventureResult[] itemList = ItemFinder.getMatchingItemList( parameters, list );
		if ( itemList.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new ClanStashRequest( itemList, direction  ) );
	}
}
