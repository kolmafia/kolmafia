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

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.SortedListModel;

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

		this.general = new AddRemovePanel();
		this.shelves = new MuseumShelfList();
		this.ordering = new OrderingPanel();

		this.addTab( "General", this.general );
		this.addTab( "Shelves", this.shelves );
		this.tabs.addTab( "Ordering", this.ordering );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
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
			this.setLayout( new GridLayout( 2, 1, 10, 10 ) );

			this.inventoryPanel = new OutsideDisplayPanel();
			this.displayPanel = new InsideDisplayPanel();

			this.add( this.inventoryPanel );
			this.add( this.displayPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( this.inventoryPanel == null || this.displayPanel == null )
				return;

			super.setEnabled( isEnabled );
			this.inventoryPanel.setEnabled( isEnabled );
			this.displayPanel.setEnabled( isEnabled );
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
				this.elementList = (ShowDescriptionList) this.scrollComponent;
			}

			private void move( boolean moveAll )
			{
				if ( !KoLCharacter.hasDisplayCase() )
				{
					JOptionPane.showMessageDialog( null, "Sorry, you don't have a display case." );
					return;
				}

				RequestThread.openRequestSequence();
				RequestThread.postRequest( new MuseumRequest( AddRemovePanel.this.getSelectedValues( this.elementList.getSelectedValues(), moveAll ), true ) );
				RequestThread.postRequest( new MuseumRequest() );
				RequestThread.closeRequestSequence();
			}

			public void actionConfirmed()
			{	this.move( true );
			}

			public void actionCancelled()
			{	this.move( false );
			}
		}

		private class InsideDisplayPanel extends LabeledScrollPanel
		{
			private ShowDescriptionList elementList;

			public InsideDisplayPanel()
			{
				super( "Display Case", "take all", "take some", new ShowDescriptionList( collection ) );
				this.elementList = (ShowDescriptionList) this.scrollComponent;
			}

			private void move( boolean moveAll )
			{
				RequestThread.openRequestSequence();
				RequestThread.postRequest( new MuseumRequest( AddRemovePanel.this.getSelectedValues( this.elementList.getSelectedValues(), moveAll ), false ) );
				RequestThread.postRequest( new MuseumRequest() );
				RequestThread.closeRequestSequence();
			}

			public void actionConfirmed()
			{	this.move( true );
			}

			public void actionCancelled()
			{	this.move( false );
			}
		}
	}

	public class MuseumShelfList extends PanelList
	{
		public MuseumShelfList()
		{	super( 1, 480, 200, MuseumManager.getShelves(), true );
		}

		public PanelListCell constructPanelListCell( Object value, int index )
		{
			MuseumShelfPanel toConstruct = new MuseumShelfPanel( index, (SortedListModel) value );
			return toConstruct;
		}

		public boolean isResizeableList()
		{	return true;
		}
	}

	public class MuseumShelfPanel extends LabeledScrollPanel implements PanelListCell
	{
		private int index;
		private ShowDescriptionList elementList;

		public MuseumShelfPanel( int index, SortedListModel value )
		{
			super( MuseumManager.getHeader( index ), "move", "remove", new ShowDescriptionList( value ), false );

			this.index = index;
			this.elementList = (ShowDescriptionList) this.scrollComponent;
		}

		public void actionConfirmed()
		{
			Object [] headerArray = MuseumManager.getHeaders().toArray();

			String selectedValue = (String) JOptionPane.showInputDialog(
				null, "Moving to this shelf...", "Shelfishness!", JOptionPane.INFORMATION_MESSAGE, null,
				headerArray, headerArray[0] );

			if ( selectedValue == null )
				return;

			for ( int i = 0; i < headerArray.length; ++i )
				if ( selectedValue.equals( headerArray[i] ) )
					MuseumManager.move( this.elementList.getSelectedValues(), this.index, i );
		}

		public void actionCancelled()
		{
			RequestThread.openRequestSequence();
			RequestThread.postRequest( new MuseumRequest( this.elementList.getSelectedValues(), false ) );
			RequestThread.postRequest( new MuseumRequest() );
			RequestThread.closeRequestSequence();
		}

		public void updateDisplay( PanelList list, Object value, int index )
		{
		}
	}

	private class OrderingPanel extends LabeledScrollPanel
	{
		private LockableListModel headers;
		private JList elementList;

		public OrderingPanel()
		{
			super( "Reorder Shelves", "move up", "apply", new JList( (LockableListModel) MuseumManager.getHeaders().clone() ) );

			this.elementList = (JList) this.scrollComponent;
			this.headers = (LockableListModel) this.elementList.getModel();
		}

		public void actionConfirmed()
		{
			int selectedIndex = this.elementList.getSelectedIndex();
			if ( selectedIndex < 1 )
				return;

			Object removed = this.headers.remove( selectedIndex );
			this.headers.add( selectedIndex - 1, removed );
			this.elementList.setSelectedIndex( selectedIndex - 1 );
		}

		public void actionCancelled()
		{
			String [] headerArray = new String[ this.headers.size() ];
			this.headers.toArray( headerArray );
			MuseumManager.reorder( headerArray );
		}
	}
}
