/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.awt.Component;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class ShowDescriptionList extends JList implements KoLConstants
{
	public int lastSelectIndex;
	public JPopupMenu contextMenu;
	public ListElementFilter filter;
	private LockableListModel listModel;

	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	public ShowDescriptionList( LockableListModel listModel )
	{
		contextMenu = new JPopupMenu();

		boolean isEncyclopedia = listModel.get(0) instanceof Entry;

		if ( listModel.size() == 0 || !isEncyclopedia )
			contextMenu.add( new DescriptionMenuItem() );

		contextMenu.add( new WikiLookupMenuItem() );

		if ( listModel == junkItemList )
			contextMenu.add( new RemoveFromJunkListMenuItem() );

		if ( listModel == mementoList )
			contextMenu.add( new RemoveFromMementoListMenuItem() );

		if ( listModel == tally )
			contextMenu.add( new ZeroTallyMenuItem() );

		if ( listModel == tally || listModel == inventory )
			contextMenu.add( new AutoSellMenuItem() );

		if ( listModel == tally || listModel == inventory || isEncyclopedia || listModel == ConcoctionsDatabase.getConcoctions() )
		{
			contextMenu.add( new AddToJunkListMenuItem() );
			contextMenu.add( new AddToMementoListMenuItem() );
		}

		addMouseListener( new PopupListener() );
		addMouseListener( new ShowDescriptionAdapter() );

		if ( listModel == junkItemList )
		{
			this.listModel = inventory.getMirrorImage();

			setModel( listModel );
			applyFilter( new JunkListFilter() );
		}
		else
		{
			this.listModel = listModel.getMirrorImage();
			setModel( listModel );
		}

		setVisibleRowCount( 4 );
		setCellRenderer( AdventureResult.getDefaultRenderer() );
	}

	public void applyFilter( ListElementFilter filter )
	{
		this.filter = filter;
		listModel.applyListFilter( filter );
	}

	/**
	 * Shows and hides the applicable context menu item.  Actually
	 * all it does is show it -- the VM will handle hiding it.
	 */

	public class PopupListener extends MouseAdapter
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

	private abstract class ContextMenuItem extends ThreadedMenuItem
	{
		public int index;
		public Object item;

		public ContextMenuItem( String title )
		{	super( title );
		}

		public void run()
		{
			this.index = lastSelectIndex == -1 ? getSelectedIndex() : lastSelectIndex;
			this.item = ShowDescriptionList.this.getModel().getElementAt( index );

			if ( item == null )
				return;

			ensureIndexIsVisible( index );
			this.executeAction();
		}

		public abstract void executeAction();
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected.
	 */

	private class DescriptionMenuItem extends ContextMenuItem
	{
		public DescriptionMenuItem()
		{	super( "Game description" );
		}

		public void executeAction()
		{	showDescription( item );
		}
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected, as it appears on the wiki.
	 */

	private class WikiLookupMenuItem extends ContextMenuItem
	{
		public WikiLookupMenuItem()
		{	super( "Wiki description" );
		}

		public void executeAction()
		{
			String name = null;
			if ( item instanceof AdventureResult )
				name = ((AdventureResult)item).getName();
			else if ( item instanceof ItemCreationRequest )
				name = ((ItemCreationRequest)item).getName();
			else if ( item instanceof String )
				name = (String) item;
			else if ( item instanceof Entry )
				name = (String) ((Entry)item).getValue();

			if ( name != null )
				StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
		}
	}

	private class AddToJunkListMenuItem extends ContextMenuItem
	{
		public AddToJunkListMenuItem()
		{	super( "Add to junk list" );
		}

		public void executeAction()
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
				else if ( items[i] instanceof Entry && TradeableItemDatabase.contains( (String) ((Entry)items[i]).getValue() ) )
					junkItemList.add( new AdventureResult( (String) ((Entry)items[i]).getValue(), 1, false ) );
			}

			StaticEntity.saveFlaggedItemList();
			listModel.applyListFilter( filter );
		}
	}

	private class AddToMementoListMenuItem extends ContextMenuItem
	{
		public AddToMementoListMenuItem()
		{	super( "Add to memento list" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[i] instanceof ItemCreationRequest )
					mementoList.add( ((ItemCreationRequest)items[i]).createdItem );
				else if ( items[i] instanceof AdventureResult && ((AdventureResult)items[i]).isItem() )
					mementoList.add( items[i] );
				else if ( items[i] instanceof String && TradeableItemDatabase.contains( (String) items[i] ) )
					mementoList.add( new AdventureResult( (String) items[i], 1, false ) );
				else if ( items[i] instanceof Entry && TradeableItemDatabase.contains( (String) ((Entry)items[i]).getValue() ) )
					mementoList.add( new AdventureResult( (String) ((Entry)items[i]).getValue(), 1, false ) );
			}

			StaticEntity.saveFlaggedItemList();
		}
	}

	private class ZeroTallyMenuItem extends ContextMenuItem
	{
		public ZeroTallyMenuItem()
		{	super( "Zero out entries" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( tally, ((AdventureResult)items[i]).getNegation() );
		}
	}

	private class AutoSellMenuItem extends ContextMenuItem
	{
		public AutoSellMenuItem()
		{	super( "Autosell selected" );
		}

		public void executeAction()
		{
			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null, "Are you sure you would like to sell the selected items?", "Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
				return;

			RequestThread.postRequest( new AutoSellRequest( getSelectedValues(), AutoSellRequest.AUTOSELL ) );
		}
	}

	private class RemoveFromJunkListMenuItem extends ContextMenuItem
	{
		public RemoveFromJunkListMenuItem()
		{	super( "This is not junk" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
				junkItemList.remove( items[i] );

			StaticEntity.saveFlaggedItemList();
		}
	}

	private class RemoveFromMementoListMenuItem extends ContextMenuItem
	{
		public RemoveFromMementoListMenuItem()
		{	super( "This is not sacred" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
				mementoList.remove( items[i] );

			StaticEntity.saveFlaggedItemList();
		}
	}

	private class JunkListFilter extends ListElementFilter implements ListDataListener
	{
		public JunkListFilter()
		{	junkItemList.addListDataListener( this );
		}

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

		public void intervalAdded( ListDataEvent e )
		{	listModel.applyListFilters();
		}

		public void intervalRemoved( ListDataEvent e )
		{	listModel.applyListFilters();
		}

		public void contentsChanged( ListDataEvent e )
		{	listModel.applyListFilters();
		}
	}
}
