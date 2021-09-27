package net.sourceforge.kolmafia.swingui.menu;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.request.UntinkerRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class UntinkerMenuItem
	extends ThreadedMenuItem
{
	public UntinkerMenuItem()
	{
		super( "Untinker Item", new UntinkerListener() );
	}

	private static class UntinkerListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			SortedListModel untinkerItems = new SortedListModel();

			for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
			{
				AdventureResult currentItem = KoLConstants.inventory.get( i );
				int itemId = currentItem.getItemId();

				// Ignore silly fairy gravy + meat from yesterday recipe
				if ( itemId == ItemPool.MEAT_STACK )
				{
					continue;
				}

				// Otherwise, accept any COMBINE recipe
				CraftingType mixMethod = ConcoctionDatabase.getMixingMethod( currentItem );
				if ( mixMethod == CraftingType.COMBINE || mixMethod == CraftingType.JEWELRY )
				{
					untinkerItems.add( currentItem );
				}
			}

			if ( untinkerItems.isEmpty() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have any untinkerable items." );
				return;
			}

			AdventureResult selectedValue =
				(AdventureResult) InputFieldUtilities.input( "You can unscrew meat paste?", untinkerItems );
			if ( selectedValue == null )
			{
				return;
			}

			RequestThread.postRequest( new UntinkerRequest( selectedValue.getItemId() ) );
		}
	}
}
