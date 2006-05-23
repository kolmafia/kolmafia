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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

// other imports
import java.util.List;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the item
 * management functionality of Kingdom of Loathing.  This ranges from
 * basic transfer to and from the Display to item creation, cooking,
 * item use, and equipment.
 */

public class MuseumFrame extends KoLFrame
{
	private JComponent general, shelves, ordering;

	/**
	 * Constructs a new <code>MuseumFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	StaticEntity.getClient()	The StaticEntity.getClient() to be notified in the event of error.
	 */

	public MuseumFrame()
	{
		super( "Display Case" );

		(new RequestThread( new MuseumRequest( StaticEntity.getClient() ) )).start();

		general = new AddRemovePanel();
		shelves = new MuseumShelfList();
		ordering = new OrderingPanel();

		JScrollPane shelvesScroller = new JScrollPane( shelves, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JScrollPane orderingScroller = new JScrollPane( ordering, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		tabs = new JTabbedPane();
		tabs.addTab( "General", general );
		tabs.addTab( "Shelves", shelvesScroller );
		tabs.addTab( "Ordering", orderingScroller );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the display and taking items from
	 * the display.
	 */

	private class AddRemovePanel extends JPanel
	{
		private ItemManagePanel inventoryPanel, displayPanel;

		public AddRemovePanel()
		{
			setLayout( new GridLayout( 2, 1, 10, 10 ) );

			inventoryPanel = new OutsideDisplayPanel();
			displayPanel = new InsideDisplayPanel();

			add( inventoryPanel );
			add( displayPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			inventoryPanel.setEnabled( isEnabled );
			displayPanel.setEnabled( isEnabled );
		}

		private Object [] getSelectedValues( Object [] selection, boolean moveAll )
		{
			if ( !moveAll )
				for ( int i = 0; i < selection.length; ++i )
					selection[i] = ((AdventureResult)selection[i]).getInstance(
						getQuantity( "Moving " + ((AdventureResult)selection[i]).getName() + "...", ((AdventureResult)selection[i]).getCount(), 1 ) );

			return selection;
		}

		private class OutsideDisplayPanel extends ItemManagePanel
		{
			public OutsideDisplayPanel()
			{	super( "Inventory", "add all", "add some", KoLCharacter.getInventory() );
			}

			private void move( boolean moveAll )
			{
				if ( !KoLCharacter.hasDisplayCase() )
				{
					JOptionPane.showMessageDialog( null, "Sorry, you don't have a display case." );
					return;
				}

				Runnable [] parameters = new Runnable[2];
				parameters[0] = new MuseumRequest( StaticEntity.getClient(), getSelectedValues( elementList.getSelectedValues(), moveAll ), true );
				parameters[1] = new MuseumRequest( StaticEntity.getClient() );

				(new RequestThread( parameters )).start();
			}

			protected void actionConfirmed()
			{	move( true );
			}

			protected void actionCancelled()
			{	move( false );
			}
		}

		private class InsideDisplayPanel extends ItemManagePanel
		{
			public InsideDisplayPanel()
			{	super( "Display Case", "take all", "take some", KoLCharacter.getCollection() );
			}

			private void move( boolean moveAll )
			{
				Runnable [] parameters = new Runnable[2];
				parameters[0] = new MuseumRequest( StaticEntity.getClient(), getSelectedValues( elementList.getSelectedValues(), moveAll ), false );
				parameters[1] = new MuseumRequest( StaticEntity.getClient() );

				(new RequestThread( parameters )).start();
			}

			protected void actionConfirmed()
			{	move( true );
			}

			protected void actionCancelled()
			{	move( false );
			}
		}
	}

	public class MuseumShelfList extends PanelList
	{
		public MuseumShelfList()
		{	super( 2, 480, 200, MuseumManager.getShelves(), true );
		}

		protected PanelListCell constructPanelListCell( Object value, int index )
		{
			MuseumShelfPanel toConstruct = new MuseumShelfPanel( index, (SortedListModel) value );
			return toConstruct;
		}

		protected boolean isResizeableList()
		{	return true;
		}
	}

	public class MuseumShelfPanel extends ItemManagePanel implements PanelListCell, Runnable
	{
		private int index;

		public MuseumShelfPanel( int index, SortedListModel value )
		{
			super( MuseumManager.getHeader( index ), "move", "remove", value, false );
			this.index = index;
		}

		public void actionConfirmed()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{
			Object [] headerArray = MuseumManager.getHeaders().toArray();

			String selectedValue = (String) JOptionPane.showInputDialog(
				null, "Moving to this shelf...", "Shelfishness!", JOptionPane.INFORMATION_MESSAGE, null,
				headerArray, headerArray[0] );

			for ( int i = 0; i < headerArray.length; ++i )
				if ( selectedValue.equals( headerArray[i] ) )
					MuseumManager.move( elementList.getSelectedValues(), index, i );
		}

		public void actionCancelled()
		{
			Runnable [] parameters = new Runnable[2];
			parameters[0] = new MuseumRequest( StaticEntity.getClient(), elementList.getSelectedValues(), false );
			parameters[1] = new MuseumRequest( StaticEntity.getClient() );

			(new RequestThread( parameters )).start();
		}

		public void updateDisplay( PanelList list, Object value, int index )
		{
		}
	}

	private class OrderingPanel extends ItemManagePanel implements Runnable
	{
		private LockableListModel headers;

		public OrderingPanel()
		{
			super( "Reorder Shelves", "move up", "apply", (LockableListModel) MuseumManager.getHeaders().getMirrorImage() );
			headers = (LockableListModel) elementList.getModel();
		}

		public void actionConfirmed()
		{
			int selectedIndex = elementList.getSelectedIndex();
			if ( selectedIndex < 1 )
				return;

			Object removed = headers.remove( selectedIndex );
			headers.add( selectedIndex - 1, removed );
			elementList.setSelectedIndex( selectedIndex - 1 );
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{
			String [] headerArray = new String[ headers.size() ];
			headers.toArray( headerArray );
			MuseumManager.reorder( headerArray );
		}
	}
}
