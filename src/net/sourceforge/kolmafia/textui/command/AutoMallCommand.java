package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.AutoMallRequest;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;

public class AutoMallCommand
	extends AbstractCommand
{
	public AutoMallCommand()
	{
		this.usage = " - dump all profitable, non-memento items into the Mall.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		AutoMallCommand.automall();
	}

	public static void automall()
	{
		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		int itemCount;

		AdventureResult currentItem;
		AdventureResult[] itemsArray = new AdventureResult[ KoLConstants.profitableList.size() ];
		AdventureResult[] items = KoLConstants.profitableList.toArray( itemsArray );

		AdventureResultArray sellList = new AdventureResultArray();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = items[ i ];

			if ( KoLConstants.mementoList.contains( currentItem ) )
			{
				continue;
			}

			if ( currentItem.getItemId() == ItemPool.MEAT_PASTE || currentItem.getItemId() == ItemPool.MEAT_STACK || currentItem.getItemId() == ItemPool.DENSE_STACK )
			{
				continue;
			}

			itemCount = currentItem.getCount( KoLConstants.inventory );

			if ( itemCount > 0 )
			{
				sellList.add( currentItem.getInstance( itemCount ) );
			}
		}

		if ( !sellList.isEmpty() )
		{
			RequestThread.postRequest( new AutoMallRequest( sellList.toArray() ) );
		}
	}
}
