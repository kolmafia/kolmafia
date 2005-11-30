/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.DefaultListCellRenderer;

// events
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JList;

// utilities
import java.util.Comparator;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to examine item descriptions
 */

public class ExamineItemsFrame extends KoLFrame
{
	private LockableListModel allItems = null;

	public ExamineItemsFrame( KoLmafia client )
	{
		super( client, "Examine Items" );

		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add( new ExamineItemsPanel(), BorderLayout.CENTER );
	}

	public boolean isEnabled()
	{	return true;
	}

	private class ExamineItemsPanel extends ItemManagePanel
	{
		public ExamineItemsPanel()
		{
			super( "All KoL Items", "Sort by name", "Sort by item #", allKoLItems() );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			java.util.Collections.sort( allItems );
			elementList.setCellRenderer( new ItemNumberCellRenderer() );
		}

		public void actionCancelled()
		{
			// Sort elements by item number
			java.util.Collections.sort( allItems, new ItemNumberComparator() );
			elementList.setCellRenderer( new ItemNumberCellRenderer() );
		}
	}

	private static class ItemNumberCellRenderer extends DefaultListCellRenderer
	{
		public ItemNumberCellRenderer()
		{	setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof AdventureResult) )
				return defaultComponent;

			AdventureResult ar = (AdventureResult) value;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );
			stringForm.append( ar.getItemID() );
			stringForm.append( ")" );

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	private LockableListModel allKoLItems()
	{
		if ( allItems == null )
		{
			allItems = new LockableListModel();;
			for ( int i = 1; i < TradeableItemDatabase.ITEM_COUNT; ++i )
			{
				String name = TradeableItemDatabase.getItemName( i );
				if ( name != null)
					allItems.add( new AdventureResult( name, 0, false ) );
			}
		}

		return allItems;
	}

	private class ItemNumberComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if ( !(o1 instanceof AdventureResult ) )
				throw new ClassCastException();

			if ( !(o2 instanceof AdventureResult ) )
				throw new ClassCastException();

			return ((AdventureResult)o1).getItemID() - ((AdventureResult)o2).getItemID();
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( ExamineItemsFrame.class, parameters )).run();
	}
}
