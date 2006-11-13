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
import java.awt.GridLayout;
import java.awt.BorderLayout;

// containers
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

// other imports
import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

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
	 */

	public MuseumFrame()
	{
		super( "Museum Display" );

		general = new AddRemovePanel();
		shelves = new MuseumShelfList();
		ordering = new OrderingPanel();

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		addTab( "General", general );
		addTab( "Shelves", shelves );
		tabs.addTab( "Ordering", ordering );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the display and taking items from
	 * the display.
	 */

	private class AddRemovePanel extends JPanel
	{
		private LabeledScrollPanel inventoryPanel, displayPanel;

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
			if ( inventoryPanel == null || displayPanel == null )
				return;

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

		private class OutsideDisplayPanel extends LabeledScrollPanel
		{
			private ShowDescriptionList elementList;

			public OutsideDisplayPanel()
			{
				super( "Inventory", "add all", "add some", new ShowDescriptionList( inventory ) );
				elementList = (ShowDescriptionList) scrollComponent;
			}

			private void move( boolean moveAll )
			{
				if ( !KoLCharacter.hasDisplayCase() )
				{
					JOptionPane.showMessageDialog( null, "Sorry, you don't have a display case." );
					return;
				}

				Runnable [] parameters = new Runnable[2];
				parameters[0] = new MuseumRequest( getSelectedValues( elementList.getSelectedValues(), moveAll ), true );
				parameters[1] = new MuseumRequest();

				(new RequestThread( parameters )).start();
			}

			public void actionConfirmed()
			{	move( true );
			}

			public void actionCancelled()
			{	move( false );
			}
		}

		private class InsideDisplayPanel extends LabeledScrollPanel
		{
			private ShowDescriptionList elementList;

			public InsideDisplayPanel()
			{
				super( "Display Case", "add all", "add some", new ShowDescriptionList( collection ) );
				elementList = (ShowDescriptionList) scrollComponent;
			}

			private void move( boolean moveAll )
			{
				Runnable [] parameters = new Runnable[2];
				parameters[0] = new MuseumRequest( getSelectedValues( elementList.getSelectedValues(), moveAll ), false );
				parameters[1] = new MuseumRequest();

				(new RequestThread( parameters )).start();
			}

			public void actionConfirmed()
			{	move( true );
			}

			public void actionCancelled()
			{	move( false );
			}
		}
	}

	public class MuseumShelfList extends PanelList
	{
		public MuseumShelfList()
		{	super( 1, 480, 200, MuseumManager.getShelves(), true );
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

	public class MuseumShelfPanel extends LabeledScrollPanel implements PanelListCell, Runnable
	{
		private int index;
		private ShowDescriptionList elementList;

		public MuseumShelfPanel( int index, SortedListModel value )
		{
			super( MuseumManager.getHeader( index ), "move", "remove", new ShowDescriptionList( value ), false );

			this.index = index;
			this.elementList = (ShowDescriptionList) scrollComponent;
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

			if ( selectedValue == null )
				return;

			for ( int i = 0; i < headerArray.length; ++i )
				if ( selectedValue.equals( headerArray[i] ) )
					MuseumManager.move( elementList.getSelectedValues(), index, i );
		}

		public void actionCancelled()
		{
			Runnable [] parameters = new Runnable[2];
			parameters[0] = new MuseumRequest( elementList.getSelectedValues(), false );
			parameters[1] = new MuseumRequest();

			(new RequestThread( parameters )).start();
		}

		public void updateDisplay( PanelList list, Object value, int index )
		{
		}
	}

	private class OrderingPanel extends LabeledScrollPanel implements Runnable
	{
		private LockableListModel headers;
		private JList elementList;

		public OrderingPanel()
		{
			super( "Reorder Shelves", "move up", "apply", new JList( (LockableListModel) MuseumManager.getHeaders().clone() ) );

			elementList = (JList) scrollComponent;
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
