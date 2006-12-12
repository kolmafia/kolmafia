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

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;

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

	protected JPanel eastPanel;
	protected JPanel filterPanel;
	protected LockableListModel elementModel;
	protected ShowDescriptionList elementList;
	protected int baseFilter = ConsumeItemRequest.CONSUME_MULTIPLE;

	protected JButton [] buttons;
	protected JCheckBox [] filters;
	protected JRadioButton [] movers;
	protected FilterItemComboBox wordfilter;


	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
	{
		super( title, confirmedText, cancelledText, new ShowDescriptionList( elements ), false );

		elementList = (ShowDescriptionList) scrollComponent;
		elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		elementList.setVisibleRowCount( 8 );

		elementModel = elements;

		if ( elementModel == inventory )
		{
			elementModel = inventory.getMirrorImage();
			elementList.setModel( elementModel );
		}

		wordfilter = new FilterItemComboBox();
		centerPanel.add( wordfilter, BorderLayout.NORTH );
	}

	public ItemManagePanel( LockableListModel elementModel )
	{
		super( "", null, null, new ShowDescriptionList( elementModel ), false );

		this.elementModel = elementModel;
		this.elementList = (ShowDescriptionList) scrollComponent;

		wordfilter = new FilterItemComboBox();
		centerPanel.add( wordfilter, BorderLayout.NORTH );

		wordfilter.filterItems();
	}

	public void actionConfirmed()
	{
	}

	public void actionCancelled()
	{
	}

	public void setButtons( ActionListener [] buttonListeners )
	{	setButtons( true, buttonListeners );
	}

	public void setButtons( boolean addFilterCheckboxes, ActionListener [] buttonListeners )
	{
		// Handle buttons along the right hand side, if there are
		// supposed to be buttons.

		eastPanel = new JPanel( new BorderLayout() );

		if ( buttonListeners != null )
		{
			JPanel eastGridPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
			buttons = new JButton[ buttonListeners.length ];

			for ( int i = 0; i < buttonListeners.length; ++i )
			{
				if ( buttonListeners[i] instanceof JButton )
				{
					buttons[i] = (JButton) buttonListeners[i];
				}
				else
				{
					buttons[i] = new JButton( buttonListeners[i].toString() );
					buttons[i].addActionListener( buttonListeners[i] );
				}

				eastGridPanel.add( buttons[i] );
			}

			eastPanel.add( eastGridPanel, BorderLayout.NORTH );
		}

		// Handle filters along the top always, whenever buttons
		// are added.

		filterPanel = new JPanel();
		JPanel northPanel = new JPanel( new BorderLayout() );

		if ( !addFilterCheckboxes )
		{
			filters = null;
		}
		else if ( buttonListeners == null )
		{
			filters = new JCheckBox[3];
			filters[0] = new JCheckBox( "Show food", KoLCharacter.canEat() );
			filters[1] = new JCheckBox( "Show booze", KoLCharacter.canDrink() );
			filters[2] = new JCheckBox( "Show others", true );
		}
		else if ( elementModel == ConcoctionsDatabase.getConcoctions() )
		{
			filters = new JCheckBox[4];
			filters[0] = new JCheckBox( "Show food", KoLCharacter.canEat() );
			filters[1] = new JCheckBox( "Show booze", KoLCharacter.canDrink() );
			filters[2] = new JCheckBox( "Show junk", true );
			filters[3] = new JCheckBox( "Show others", true );
		}
		else
		{
			filters = new JCheckBox[5];

			filters[0] = new JCheckBox( "Show food", KoLCharacter.canEat() );
			filters[1] = new JCheckBox( "Show booze", KoLCharacter.canDrink() );
			filters[2] = new JCheckBox( "Show equipment", true );
			filters[3] = new JCheckBox( "Show junk", true );
			filters[4] = new JCheckBox( "Show others", true );
		}

		if ( filters != null )
		{
			for ( int i = 0; i < filters.length; ++i )
			{
				filterPanel.add( filters[i] );
				filters[i].addActionListener( new UpdateFilterListener() );
			}

		}

		northPanel.add( filterPanel, BorderLayout.NORTH );

		// If there are buttons, they likely need movers.  Therefore, add
		// some movers to everything.

		if ( buttonListeners != null )
		{
			JPanel moverPanel = new JPanel();

			movers = new JRadioButton[4];
			movers[0] = new JRadioButton( "Move all" );
			movers[1] = new JRadioButton( "Move all but one" );
			movers[2] = new JRadioButton( "Move multiple", true );
			movers[3] = new JRadioButton( "Move exactly one" );

			ButtonGroup moverGroup = new ButtonGroup();
			for ( int i = 0; i < 4; ++i )
			{
				moverGroup.add( movers[i] );
				moverPanel.add( movers[i] );
			}

			northPanel.add( moverPanel, BorderLayout.SOUTH );
		}

		actualPanel.add( northPanel, BorderLayout.NORTH );

		if ( buttonListeners != null )
			actualPanel.add( eastPanel, BorderLayout.EAST );

		wordfilter.filterItems();
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( elementList == null || buttons == null )
			return;

		if ( buttons.length > 0 && buttons[ buttons.length - 1 ] == null )
			return;

		elementList.setEnabled( isEnabled );
		for ( int i = 0; i < buttons.length; ++i )
			buttons[i].setEnabled( isEnabled );
	}

	protected AdventureResult [] getDesiredItems( String message )
	{
		return getDesiredItems( message, movers == null ? TAKE_MULTIPLE :
			movers[0].isSelected() ? TAKE_ALL : movers[1].isSelected() ? TAKE_ALL_BUT_ONE :
			movers[2].isSelected() ? TAKE_MULTIPLE : TAKE_ONE );
	}

	protected AdventureResult [] getDesiredItems( String message, int quantityType )
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
					quantity = KoLFrame.getQuantity( message + " " + currentItem.getName() + "...", currentItem.getCount() );
					break;
				default:
					quantity = 1;
					break;
			}

			quantity = Math.min( quantity, currentItem.getCount() );

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity <= 0 )
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

	protected class UpdateFilterListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	wordfilter.filterItems();
		}
	}

	protected abstract class TransferListener implements ActionListener
	{
		protected String description;
		protected boolean retrieveFromClosetFirst;

		public TransferListener( String description, boolean retrieveFromClosetFirst )
		{
			this.description = description;
			this.retrieveFromClosetFirst = retrieveFromClosetFirst;
		}

		public AdventureResult [] initialSetup()
		{
			AdventureResult [] items = getDesiredItems( description );
			if (items == null )
				return null;

			if ( retrieveFromClosetFirst )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.CLOSET_TO_INVENTORY, items ) );

			return items;
		}
	}

	public void refreshFilter()
	{	elementModel.applyListFilter( wordfilter.filter );
	}

	/**
	 * Special instance of a JComboBox which overrides the default
	 * key events of a JComboBox to allow you to catch key events.
	 */

	protected class FilterItemComboBox extends MutableComboBox
	{
		protected boolean food, booze, equip, junk, other;

		public FilterItemComboBox()
		{
			super( new LockableListModel(), true );
			filter = new ConsumptionBasedFilter();
		}

		public void setSelectedItem( Object anObject )
		{
			super.setSelectedItem( anObject );
			filterItems();
		}

		protected void findMatch( int keyCode )
		{
			super.findMatch( keyCode );
			filterItems();
		}

		protected void filterItems()
		{
			if ( filters == null )
			{
				food = true;
				booze = true;
				equip = true;
				junk = true;
				other = true;
			}
			else
			{
				food = filters[0].isSelected();
				booze = filters[1].isSelected();

				if ( filters.length == 3 )
				{
					junk = true;
					other = filters[2].isSelected();
					equip = other;
				}
				else if ( filters.length == 4 )
				{
					junk = filters[2].isSelected();
					other = filters[3].isSelected();
					equip = other;
				}
				else
				{
					equip = filters[2].isSelected();
					junk = filters[3].isSelected();
					other = filters[4].isSelected();
				}
			}

			filter.shouldHideJunkItems = !junk;
			elementList.applyFilter( filter );
		}

		protected class ConsumptionBasedFilter extends WordBasedFilter
		{
			public ConsumptionBasedFilter()
			{	super( false );
			}

			public boolean isVisible( Object element )
			{
				if ( isNonResult( element ) )
					return super.isVisible( element );

				boolean isItem = element instanceof ItemCreationRequest;
				isItem |= element instanceof AdventureResult && ((AdventureResult)element).isItem();

				if ( !isItem )
					return super.isVisible( element );

				boolean isVisibleWithFilter = true;
				String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
				int itemId = TradeableItemDatabase.getItemId( name );

				switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
				{
				case ConsumeItemRequest.CONSUME_EAT:
					isVisibleWithFilter = food;
					break;

				case ConsumeItemRequest.CONSUME_DRINK:
					isVisibleWithFilter = booze;
					break;

				case ConsumeItemRequest.EQUIP_HAT:
				case ConsumeItemRequest.EQUIP_SHIRT:
				case ConsumeItemRequest.EQUIP_WEAPON:
				case ConsumeItemRequest.EQUIP_OFFHAND:
				case ConsumeItemRequest.EQUIP_PANTS:
				case ConsumeItemRequest.EQUIP_ACCESSORY:
				case ConsumeItemRequest.EQUIP_FAMILIAR:
					isVisibleWithFilter = equip;
					break;

				default:

					if ( element instanceof AdventureResult )
					{
						// Milk of magnesium is marked as food, as are
						// munchies pills; all others are marked as expected.

						isVisibleWithFilter = other;
						if ( name.equals( "milk of magnesium" ) || name.equals( "munchies pills" ) )
							isVisibleWithFilter |= food;
					}
					else
					{
						switch ( ConcoctionsDatabase.getMixingMethod( itemId ) )
						{
						case ItemCreationRequest.COOK:
						case ItemCreationRequest.COOK_REAGENT:
						case ItemCreationRequest.SUPER_REAGENT:
							isVisibleWithFilter = food || other;
							break;

						case ItemCreationRequest.COOK_PASTA:
						case ItemCreationRequest.WOK:
							isVisibleWithFilter = food;
							break;

						case ItemCreationRequest.MIX:
						case ItemCreationRequest.MIX_SPECIAL:
						case ItemCreationRequest.STILL_BOOZE:
						case ItemCreationRequest.MIX_SUPER:
							isVisibleWithFilter = booze;
							break;

						default:
							isVisibleWithFilter = other;
							break;
						}
					}
				}

				if ( !isVisibleWithFilter )
					return false;

				return super.isVisible( element );
			}
		}
	}
}
