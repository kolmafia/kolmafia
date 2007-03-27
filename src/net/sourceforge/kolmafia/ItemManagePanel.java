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
import java.awt.Component;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class ItemManagePanel extends LabeledScrollPanel
{
	public static final int CONSUME_MULTIPLE = 0;

	public static final int TAKE_ALL = 1;
	public static final int TAKE_ALL_BUT_ONE = 2;
	public static final int TAKE_MULTIPLE = 3;
	public static final int TAKE_ONE = 4;

	public JPanel northPanel;
	public LockableListModel elementModel;
	public ShowDescriptionList elementList;

	public JButton [] buttons;
	public JCheckBox [] filters;
	public JRadioButton [] movers;
	public FilterItemComboBox wordfilter;

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elementModel )
	{
		super( title, confirmedText, cancelledText, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) scrollComponent;
		this.elementModel = (LockableListModel) elementList.getModel();

		this.elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.elementList.setVisibleRowCount( 8 );

		wordfilter = new FilterItemComboBox();
		centerPanel.add( wordfilter, BorderLayout.NORTH );

		this.wordfilter.filterItems();
	}

	public ItemManagePanel( LockableListModel elementModel )
	{
		super( "", null, null, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) scrollComponent;
		this.elementModel = (LockableListModel) elementList.getModel();

		this.wordfilter = new FilterItemComboBox();
		centerPanel.add( wordfilter, BorderLayout.NORTH );

		this.wordfilter.filterItems();
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

		northPanel = new JPanel( new BorderLayout() );

		if ( !addFilterCheckboxes )
		{
			filters = null;
		}
		else
		{
			JPanel filterPanel = new JPanel();
			filters = new JCheckBox[5];

			if ( buttonListeners == null )
			{
				filters[0] = new JCheckBox( "food", KoLCharacter.canEat() );
				filters[1] = new JCheckBox( "booze", KoLCharacter.canDrink() );
				filters[2] = new JCheckBox( "equip", true );
				filters[3] = new JCheckBox( "others", true );
				filters[4] = new JCheckBox( "no-trade", true );
			}
			else
			{
				filters[0] = new JCheckBox( "Show food", KoLCharacter.canEat() );
				filters[1] = new JCheckBox( "Show booze", KoLCharacter.canDrink() );
				filters[2] = new JCheckBox( "Show equipment", true );
				filters[3] = new JCheckBox( "Show others", true );
				filters[4] = new JCheckBox( "Show no-trade", true );
			}

			for ( int i = 0; i < 5; ++i )
			{
				filterPanel.add( filters[i] );
				filters[i].addActionListener( new UpdateFilterListener() );
			}

			northPanel.add( filterPanel, BorderLayout.CENTER );
		}

		// If there are buttons, they likely need movers.  Therefore, add
		// some movers to everything.

		if ( buttonListeners != null && buttonListeners.length != 0 )
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

	public AdventureResult [] getDesiredItems( String message )
	{
		if ( movers == null )
			return getDesiredItems( message, message.equals( "Consume" ) ? CONSUME_MULTIPLE : TAKE_MULTIPLE );

		if ( movers[2].isSelected() )
			return getDesiredItems( message, message.equals( "Consume" ) ? CONSUME_MULTIPLE : TAKE_MULTIPLE );

		return getDesiredItems( message, movers[0].isSelected() ? TAKE_ALL : movers[1].isSelected() ? TAKE_ALL_BUT_ONE : TAKE_ONE );
	}

	public AdventureResult [] getDesiredItems( String message, int quantityType )
	{
		Object [] items = elementList.getSelectedValues();
		if ( items.length == 0 )
			return null;

		int neededSize = items.length;
		AdventureResult currentItem;

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = items[i] instanceof Concoction ? ((Concoction) items[i]).getItem() : (AdventureResult) items[i];

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
					if ( currentItem.getCount() > 0 && quantity == 0 )
						return new AdventureResult[0];
					break;

				case CONSUME_MULTIPLE:
					int maximum = ConsumeItemRequest.maximumUses( currentItem.getItemId() );
					quantity = maximum < 2 ? maximum : KoLFrame.getQuantity( message + " " + currentItem.getName() + "...",
						Math.min( maximum, currentItem.getCount() ) );

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

	public class UpdateFilterListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	wordfilter.filterItems();
		}
	}

	public abstract class TransferListener extends ThreadedListener
	{
		public String description;
		public boolean retrieveFromClosetFirst;

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

	public class ConsumeListener extends ThreadedListener
	{
		public void run()
		{
			Object [] items = getDesiredItems( "Consume" );
			if ( items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
			{
				int usageType = TradeableItemDatabase.getConsumptionType( ((AdventureResult)items[i]).getItemId() );

				switch ( usageType )
				{
				case NO_CONSUME:
					break;

				case EQUIP_FAMILIAR:
				case EQUIP_ACCESSORY:
				case EQUIP_HAT:
				case EQUIP_PANTS:
				case EQUIP_SHIRT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
					RequestThread.postRequest( new EquipmentRequest( (AdventureResult) items[i], KoLCharacter.consumeFilterToEquipmentType( usageType ) ) );
					break;

				default:
					RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
					break;
				}
			}
		}

		public String toString()
		{	return "use item";
		}
	}

	public class PutInClosetListener extends TransferListener
	{
		public PutInClosetListener( boolean retrieveFromClosetFirst )
		{	super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst );
		}

		public void run()
		{
			AdventureResult [] items = initialSetup();
			if ( items == null )
				return;

			if ( !retrieveFromClosetFirst )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
		}

		public String toString()
		{	return retrieveFromClosetFirst ? "inventory" : "closet";
		}
	}

	public class AutoSellListener extends TransferListener
	{
		private int sellType;

		public AutoSellListener( boolean retrieveFromClosetFirst, int sellType )
		{
			super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst );
			this.sellType = sellType;
		}

		public void run()
		{
			if ( sellType == AutoSellRequest.AUTOMALL && !KoLCharacter.hasStore() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
				return;
			}

			if ( sellType == AutoSellRequest.AUTOSELL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
				"Are you sure you would like to sell the selected items?",
					"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
						return;

			if ( sellType == AutoSellRequest.AUTOMALL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
				"Are you sure you would like to place the selected items in your store?",
					"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
						return;

			AdventureResult [] items = initialSetup();
			if ( items == null )
				return;

			RequestThread.postRequest( new AutoSellRequest( items, sellType ) );
		}

		public String toString()
		{	return sellType == AutoSellRequest.AUTOSELL ? "auto sell" : "place in mall";
		}
	}

	public class GiveToClanListener extends TransferListener
	{
		public GiveToClanListener( boolean retrieveFromClosetFirst )
		{	super( "Stashing", retrieveFromClosetFirst );
		}

		public void run()
		{
			AdventureResult [] items = initialSetup();
			if ( items == null )
				return;

			RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
		}

		public String toString()
		{	return "clan stash";
		}
	}

	public class PutOnDisplayListener extends TransferListener
	{
		public PutOnDisplayListener( boolean retrieveFromClosetFirst )
		{	super( "Showcasing", retrieveFromClosetFirst );
		}

		public void run()
		{
			Object [] items = initialSetup();
			if ( items == null )
				return;

			if ( !KoLCharacter.hasDisplayCase() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You don't own a display case in the Cannon Museum.");
				return;
			}

			RequestThread.postRequest( new MuseumRequest( items, true ) );
		}

		public String toString()
		{	return "display case";
		}
	}

	public class PulverizeListener extends TransferListener
	{
		public PulverizeListener( boolean retrieveFromClosetFirst )
		{	super( "Smashing", retrieveFromClosetFirst );
		}

		public void run()
		{
			AdventureResult [] items = initialSetup();
			if ( items == null || items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
			{
				boolean willSmash = TradeableItemDatabase.isTradeable( items[i].getItemId() ) ||
					JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
						items[i].getName() + " is untradeable.  Are you sure?", "Smash request nag screen!", JOptionPane.YES_NO_OPTION );

				if ( willSmash )
					RequestThread.postRequest( new PulverizeRequest( items[i] ) );
			}
		}

		public String toString()
		{	return "pulverize";
		}
	}

	/**
	 * Special instance of a JComboBox which overrides the default
	 * key events of a JComboBox to allow you to catch key events.
	 */

	public class FilterItemComboBox extends MutableComboBox
	{
		public boolean food, booze, equip, other, notrade;

		public FilterItemComboBox()
		{
			super( new LockableListModel(), true );
			filter = new ConsumptionBasedFilter();

			food = true; booze = true; equip = true;
			other = true; notrade = true;
		}

		public void setSelectedItem( Object anObject )
		{
			super.setSelectedItem( anObject );
			filterItems();
		}

		public void findMatch( int keyCode )
		{
			super.findMatch( keyCode );
			filterItems();
		}

		public void filterItems()
		{
			if ( filters != null )
			{
				food = filters[0].isSelected();
				booze = filters[1].isSelected();

				equip = filters[2].isSelected();
				other = filters[3].isSelected();
				notrade = filters[4].isSelected();
			}

			filter.makeStrict();
			elementList.applyFilter( filter );

			if ( elementModel.getSize() == 0 )
			{
				filter.makeFuzzy();
				elementList.applyFilter( filter );
			}
		}

		public class ConsumptionBasedFilter extends WordBasedFilter
		{
			public boolean isVisible( Object element )
			{
				if ( isNonResult( element ) )
					return filters == null && super.isVisible( element );

				boolean isItem = element instanceof ItemCreationRequest;
				isItem |= element instanceof AdventureResult && ((AdventureResult)element).isItem();

				if ( !isItem )
					return super.isVisible( element );

				String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
				int itemId = TradeableItemDatabase.getItemId( name );

				if ( !notrade && !TradeableItemDatabase.isTradeable( itemId ) )
					return false;

				boolean isVisibleWithFilter = true;

				switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
				{
				case CONSUME_EAT:
					isVisibleWithFilter = food;
					break;

				case CONSUME_DRINK:
					isVisibleWithFilter = booze;
					break;

				case EQUIP_HAT:
				case EQUIP_SHIRT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
				case EQUIP_PANTS:
				case EQUIP_ACCESSORY:
				case EQUIP_FAMILIAR:
					isVisibleWithFilter = equip;
					break;

				default:

					if ( element instanceof AdventureResult )
					{
						// Milk of magnesium is marked as food, as are
						// munchies pills; all others are marked as expected.

						isVisibleWithFilter = other;
						if ( name.equalsIgnoreCase( "milk of magnesium" ) || name.equalsIgnoreCase( "munchies pills" ) )
							isVisibleWithFilter |= food;
					}
					else
					{
						switch ( ConcoctionsDatabase.getMixingMethod( itemId ) )
						{
						case COOK:
						case COOK_REAGENT:
						case SUPER_REAGENT:
							isVisibleWithFilter = food || other;
							break;

						case COOK_PASTA:
						case WOK:
							isVisibleWithFilter = food;
							break;

						case MIX:
						case MIX_SPECIAL:
						case STILL_BOOZE:
						case MIX_SUPER:
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
