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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JComboBox;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

import javax.swing.ListSelectionModel;
import com.informit.guides.JDnDList;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An internal class which creates a panel which manages items.
 * This is done because most of the item management displays
 * are replicated.  Note that a lot of this code was borrowed
 * directly from the ActionVerifyPanel class in the utilities
 * package for Spellcast.
 */

public class ItemManagePanel extends LabeledScrollPanel
{
	protected static final int TAKE_ALL = 1;
	protected static final int TAKE_ALL_BUT_ONE = 2;
	protected static final int TAKE_MULTIPLE = 3;
	protected static final int TAKE_ONE = 4;

	protected boolean showMovers;
	protected JPanel optionPanel;
	protected LockableListModel elementModel;
	protected ShowDescriptionList elementList;

	protected JButton [] buttons;
	protected JRadioButton [] movers;

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
	{	this( title, confirmedText, cancelledText, elements, true, false );
	}

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements, boolean isRootPane )
	{	this( title, confirmedText, cancelledText, elements, isRootPane, false );
	}

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements, boolean isRootPane, boolean allowDragAndDrop )
	{
		super( title, confirmedText, cancelledText,
			allowDragAndDrop ? (JList) new JDnDList( elements ) : (JList) new ShowDescriptionList( elements ), isRootPane );

		elementList = (ShowDescriptionList) scrollComponent;
		elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		elementList.setVisibleRowCount( 8 );
	}

	public ItemManagePanel( String title, LockableListModel elementModel, boolean showMovers )
	{
		super( "", null, null, new ShowDescriptionList( elementModel ), false );

		this.showMovers = showMovers;
		this.optionPanel = new JPanel();

		this.elementModel = elementModel;
		this.elementList = (ShowDescriptionList) scrollComponent;
	}

	public void actionConfirmed()
	{
	}

	public void actionCancelled()
	{
	}

	public void setButtons( String [] buttonLabels, ActionListener [] buttonListeners )
	{
		JPanel buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
		buttons = new JButton[ buttonLabels.length ];

		for ( int i = 0; i < buttonLabels.length; ++i )
		{
			buttons[i] = new JButton( buttonLabels[i] );
			buttons[i].addActionListener( buttonListeners[i] );
			buttonPanel.add( buttons[i] );
		}

		JPanel moverPanel = new JPanel();

		movers = new JRadioButton[4];
		movers[0] = new JRadioButton( "Move all", true );
		movers[1] = new JRadioButton( "Move all but one" );
		movers[2] = new JRadioButton( "Move multiple" );
		movers[3] = new JRadioButton( "Move exactly one" );

		ButtonGroup moverGroup = new ButtonGroup();
		for ( int i = 0; i < 4; ++i )
		{
			moverGroup.add( movers[i] );
			if ( showMovers )
				moverPanel.add( movers[i] );
		}

		JPanel northPanel = new JPanel( new BorderLayout() );
		northPanel.add( moverPanel, BorderLayout.CENTER );
		northPanel.add( optionPanel, BorderLayout.NORTH );

		actualPanel.add( northPanel, BorderLayout.NORTH );

		JPanel buttonContainer = new JPanel( new BorderLayout() );
		buttonContainer.add( buttonPanel, BorderLayout.NORTH );
		actualPanel.add( buttonContainer, BorderLayout.EAST );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( elementList == null || buttons == null || movers == null )
			return;

		if ( buttons.length > 0 && buttons[ buttons.length - 1 ] == null )
			return;

		if ( movers.length > 0 && movers[ movers.length - 1 ] == null )
			return;

		elementList.setEnabled( isEnabled );
		for ( int i = 0; i < buttons.length; ++i )
			buttons[i].setEnabled( isEnabled );

		for ( int i = 0; i < movers.length; ++i )
			movers[i].setEnabled( isEnabled );
	}

	protected AdventureResult [] getDesiredItems( String message )
	{
		return getDesiredItems( elementList, message,
			movers[0].isSelected() ? TAKE_ALL : movers[1].isSelected() ? TAKE_ALL_BUT_ONE :
			movers[2].isSelected() ? TAKE_MULTIPLE : TAKE_ONE );
	}

	protected void filterSelection( boolean eat, boolean drink, boolean other, boolean nosell, boolean notrade )
	{
		Object [] elements = elementList.getSelectedValues();
		for ( int i = 0; i < elements.length; ++i )
		{
			int actualIndex = ((LockableListModel)elementList.getModel()).indexOf( elements[i] );
			if ( !AdventureResult.isVisibleWithFilter( ((LockableListModel)elementList.getModel()).get( actualIndex ), eat, drink, other, nosell, notrade ) )
				elementList.removeSelectionInterval( actualIndex, actualIndex );
		}
	}

	protected AdventureResult [] getDesiredItems( JList elementList, String message, int quantityType )
	{
		Object [] items = elementList.getSelectedValues();
		if ( items.length == 0 )
			return null;

		int neededSize = items.length;
		AdventureResult currentItem;

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[i];

			int quantity = 0;
			switch ( quantityType )
			{
				case TAKE_ALL:
					quantity = currentItem.getCount();
					break;
				case TAKE_ALL_BUT_ONE:
					quantity = currentItem.getCount() - 1;
					break;
				case TAKE_MULTIPLE:
					quantity = getQuantity( message + " " + currentItem.getName() + "...", currentItem.getCount() );
					break;
				default:
					quantity = 1;
					break;
			}

			// If the user manually enters zero, return from
			// this, since they probably wanted to cancel.

			if ( quantity == 0 && quantityType == TAKE_MULTIPLE )
				return null;

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity == 0 )
			{
				items[i] = null;
				--neededSize;
			}
			else
			{
				items[i] = currentItem.getInstance( quantity );
			}
		}

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		AdventureResult [] desiredItems = new AdventureResult[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
			if ( items[i] != null )
				desiredItems[ neededSize++ ] = (AdventureResult) items[i];

		return desiredItems;
	}

	protected static class FilterCheckBox extends JCheckBox implements ActionListener
	{
		protected boolean isTradeable;
		protected JCheckBox [] filters;
		protected ShowDescriptionList elementList;

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, String label, boolean isSelected )
		{	this( filters, elementList, false, label, isSelected );
		}

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, boolean isTradeable, String label, boolean isSelected )
		{
			super( label, isSelected );
			addActionListener( this );

			this.isTradeable = isTradeable;
			this.filters = filters;
			this.elementList = elementList;
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( isTradeable )
			{
				if ( !filters[0].isSelected() && !filters[1].isSelected() && !filters[2].isSelected() )
				{
					filters[3].setEnabled( false );
					filters[4].setEnabled( false );
				}
				else
				{
					filters[3].setEnabled( true );
					filters[4].setEnabled( true );
				}


				elementList.setCellRenderer(
					AdventureResult.getAutoSellCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() ) );
			}
			else
			{
				elementList.setCellRenderer(
					AdventureResult.getConsumableCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
			}

			elementList.validate();
		}
	}

	protected static final int getQuantity( String title, int maximumValue, int defaultValue )
	{
		// Check parameters; avoid programmer error.
		if ( defaultValue > maximumValue )
			defaultValue = maximumValue;

		if ( maximumValue == 1 && maximumValue == defaultValue )
			return 1;

		String currentValue = JOptionPane.showInputDialog( title, COMMA_FORMAT.format( defaultValue ) );
		if ( currentValue == null )
			return 0;

		if ( currentValue.equals( "*" ) )
			return maximumValue;

		int desiredValue = StaticEntity.parseInt( currentValue );
		return desiredValue <= 0 ? maximumValue - desiredValue : Math.min( desiredValue, maximumValue );
	}

	protected static final int getQuantity( String title, int maximumValue )
	{	return getQuantity( title, maximumValue, maximumValue );
	}
}
