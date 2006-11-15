/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A special class which displays an item's description after you float
 * click on the JList.
 */

public class ShowDescriptionList extends JList implements KoLConstants
{
	protected int lastSelectIndex;
	protected JPopupMenu contextMenu;
	protected LockableListModel.ListElementFilter filter;

	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	public ShowDescriptionList( LockableListModel model )
	{
		super( model );
		contextMenu = new JPopupMenu();

		boolean isEncyclopedia = model.get(0) instanceof Map.Entry;

		if ( model.size() == 0 || !isEncyclopedia )
			contextMenu.add( new DescriptionMenuItem() );

		contextMenu.add( new WikiLookupMenuItem() );

		if ( model == tally || model == inventory || isEncyclopedia || model == ConcoctionsDatabase.getConcoctions() )
			contextMenu.add( new AddToJunkListMenuItem() );

		if ( model == junkItemList )
			contextMenu.add( new RemoveFromJunkListMenuItem() );

		if ( model == tally )
			contextMenu.add( new ZeroTallyMenuItem() );

		addMouseListener( new PopupListener() );
		addMouseListener( new ShowDescriptionAdapter() );

		if ( model == junkItemList )
		{
			setModel( inventory.getMirrorImage() );
			applyFilter( new JunkListFilter() );
		}

		setVisibleRowCount( 4 );
		setCellRenderer( AdventureResult.getDefaultRenderer() );
	}

	public void applyFilter( LockableListModel.ListElementFilter filter )
	{
		this.filter = filter;
		((LockableListModel)getModel()).applyListFilter( filter );
	}

	/**
	 * Shows and hides the applicable context menu item.  Actually
	 * all it does is show it -- the VM will handle hiding it.
	 */

	protected class PopupListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{	maybeShowPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{	maybeShowPopup( e );
		}

		private void maybeShowPopup( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				int index = locationToIndex( e.getPoint() );
				lastSelectIndex = index;

				if ( !isSelectedIndex( index ) )
				{
					clearSelection();
					addSelectionInterval( index, index );
				}

				contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
			else
			{
				lastSelectIndex = -1;
			}
		}
    }

	private void showDescription( Object item )
	{
		if ( item instanceof AdventureResult )
		{
			if ( ((AdventureResult)item).isItem() )
				FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((AdventureResult)item).getItemId() ) );
			if ( ((AdventureResult)item).isStatusEffect() )
				FightFrame.showLocation( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getEffectId( ((AdventureResult)item).getName() ) );
		}
		else if ( item instanceof ItemCreationRequest )
		{
			FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((ItemCreationRequest)item).getItemId() ) );
		}
		else if ( item instanceof String )
		{
			Matcher playerMatcher = PLAYERID_MATCHER.matcher( (String) item );
			if ( playerMatcher.find() )
			{
				Object [] parameters = new Object [] { "#" + playerMatcher.group(1) };
				SwingUtilities.invokeLater( new CreateFrameRunnable( ProfileFrame.class, parameters ) );
			}
		}
	}

	/**
	 * Shows the description of the item which was recently
	 * double-clicked.
	 */

	private class ShowDescriptionAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			if ( e.getClickCount() == 2 )
			{
				int index = locationToIndex( e.getPoint() );
				Object item = getModel().getElementAt( index );

				if ( item == null )
					return;

				ensureIndexIsVisible( index );
				showDescription( item );
			}
		}
	}

	private abstract class ThreadedMenuItem extends JMenuItem implements ActionListener, Runnable
	{
		protected int index;
		protected Object item;

		public ThreadedMenuItem( String title )
		{
			super( title );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			this.index = lastSelectIndex == -1 ? getSelectedIndex() : lastSelectIndex;
			this.item = ShowDescriptionList.this.getModel().getElementAt( index );

			if ( item == null )
				return;

			ensureIndexIsVisible( index );
			this.run();
		}
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected.
	 */

	private class DescriptionMenuItem extends ThreadedMenuItem
	{
		public DescriptionMenuItem()
		{	super( "Game description" );
		}

		public void run()
		{	showDescription( item );
		}
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected, as it appears on the wiki.
	 */

	private class WikiLookupMenuItem extends ThreadedMenuItem
	{
		public WikiLookupMenuItem()
		{	super( "Wiki description" );
		}

		public void run()
		{
			String name = null;
			if ( item instanceof AdventureResult )
				name = ((AdventureResult)item).getName();
			else if ( item instanceof ItemCreationRequest )
				name = ((ItemCreationRequest)item).getName();
			else if ( item instanceof String )
				name = (String) item;
			else if ( item instanceof Map.Entry )
				name = (String) ((Map.Entry)item).getValue();

			if ( name != null )
				StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
		}
	}

	public void junkSelectedValues()
	{
		Object [] items = getSelectedValues();
		ShowDescriptionList.this.clearSelection();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] instanceof ItemCreationRequest )
				junkItemList.add( ((ItemCreationRequest)items[i]).createdItem );
			else if ( items[i] instanceof AdventureResult && ((AdventureResult)items[i]).isItem() )
				junkItemList.add( items[i] );
			else if ( items[i] instanceof String && TradeableItemDatabase.contains( (String) items[i] ) )
				junkItemList.add( new AdventureResult( (String) items[i], 1, false ) );
			else if ( items[i] instanceof Map.Entry && TradeableItemDatabase.contains( (String) ((Map.Entry)items[i]).getValue() ) )
				junkItemList.add( new AdventureResult( (String) ((Map.Entry)items[i]).getValue(), 1, false ) );
		}

		StaticEntity.saveJunkItemList();
		if ( filter != null )
			((LockableListModel)getModel()).applyListFilter( filter );
	}

	public void unjunkSelectedValues()
	{
		Object [] items = getSelectedValues();
		ShowDescriptionList.this.clearSelection();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] instanceof ItemCreationRequest )
				junkItemList.remove( ((ItemCreationRequest)items[i]).createdItem );
			else if ( items[i] instanceof AdventureResult )
				junkItemList.remove( items[i] );
			else if ( items[i] instanceof String )
				junkItemList.remove( new AdventureResult( (String) items[i], 1, false ) );
			else if ( items[i] instanceof Map.Entry )
				junkItemList.remove( new AdventureResult( (String) ((Map.Entry)items[i]).getValue(), 1, false ) );
		}

		StaticEntity.saveJunkItemList();
		((LockableListModel)getModel()).applyListFilter( filter );
	}

	private class AddToJunkListMenuItem extends ThreadedMenuItem
	{
		public AddToJunkListMenuItem()
		{	super( "Add to junk list" );
		}

		public void run()
		{	junkSelectedValues();
		}
	}

	private class ZeroTallyMenuItem extends ThreadedMenuItem
	{
		public ZeroTallyMenuItem()
		{	super( "Zero out entries" );
		}

		public void run()
		{
			Object [] items = getSelectedValues();
			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( tally, ((AdventureResult)items[i]).getNegation() );
		}
	}

	private class RemoveFromJunkListMenuItem extends ThreadedMenuItem
	{
		public RemoveFromJunkListMenuItem()
		{	super( "This is not junk" );
		}

		public void run()
		{	unjunkSelectedValues();
		}
	}



	private class JunkListFilter extends LockableListModel.ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			if ( element instanceof AdventureResult )
			{
				if ( junkItemList.contains( element ) )
					return true;
			}
			else if ( element instanceof ItemCreationRequest )
			{
				if ( junkItemList.contains( ((ItemCreationRequest) element).createdItem ) )
					return true;
			}

			return false;
		}
	}
}
