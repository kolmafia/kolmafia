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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLFrame.RequestButton;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class ItemManagePanel extends LabeledScrollPanel
{
	public static final int USE_MULTIPLE = 0;

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

	private FilterItemField filterfield;

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elementModel )
	{
		this( title, confirmedText, cancelledText, elementModel, true,
			elementModel == tally || elementModel == inventory || elementModel == closet ||
			elementModel == ConcoctionsDatabase.getCreatables() || elementModel == ConcoctionsDatabase.getUsables() );
	}

	public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elementModel, boolean addFilterField, boolean addRefreshButton )
	{
		super( title, confirmedText, cancelledText, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) this.scrollComponent;
		this.elementModel = (LockableListModel) this.elementList.getModel();

		this.elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.elementList.setVisibleRowCount( 8 );

		this.filterfield = this.getWordFilter();

		if ( addFilterField )
			this.centerPanel.add( this.filterfield, BorderLayout.NORTH );

		if ( addRefreshButton )
			this.eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );

		this.northPanel = new JPanel( new BorderLayout() );
		this.actualPanel.add( this.northPanel, BorderLayout.NORTH );
	}

	protected FilterItemField getWordFilter()
	{	return new FilterItemField();
	}

	protected void listenToCheckBox( JCheckBox box )
	{	box.addActionListener( this.filterfield );
	}

	protected void listenToRadioButton( JRadioButton button )
	{	button.addActionListener( this.filterfield );
	}

	public ItemManagePanel( LockableListModel elementModel )
	{
		this( elementModel, true,
			elementModel == tally || elementModel == inventory || elementModel == closet ||
			elementModel == ConcoctionsDatabase.getCreatables() || elementModel == ConcoctionsDatabase.getUsables() );
	}

	public ItemManagePanel( LockableListModel elementModel, boolean addFilterField, boolean addRefreshButton )
	{
		super( "", null, null, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) this.scrollComponent;
		this.elementModel = (LockableListModel) this.elementList.getModel();

		this.northPanel = new JPanel( new BorderLayout() );
		this.actualPanel.add( this.northPanel, BorderLayout.NORTH );

		this.filterfield = this.getWordFilter();

		if ( addFilterField )
			this.centerPanel.add( this.filterfield, BorderLayout.NORTH );

		if ( addRefreshButton )
			this.eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
	}

	public void actionConfirmed()
	{
	}

	public void actionCancelled()
	{
	}

	public void setFixedFilter( boolean food, boolean booze, boolean equip, boolean other, boolean notrade )
	{
		this.filterfield.food = food;
		this.filterfield.booze = booze;
		this.filterfield.equip = equip;
		this.filterfield.other = other;
		this.filterfield.notrade = notrade;
	}

	public void addFilters()
	{
		JPanel filterPanel = new JPanel();
		this.filters = new JCheckBox[5];

		this.filters[0] = new JCheckBox( "food", KoLCharacter.canEat() );
		this.filters[1] = new JCheckBox( "booze", KoLCharacter.canDrink() );
		this.filters[2] = new JCheckBox( "equip", true );
		this.filters[3] = new JCheckBox( "others", true );
		this.filters[4] = new JCheckBox( "no-trade", true );

		for ( int i = 0; i < 5; ++i )
		{
			filterPanel.add( this.filters[i] );
			this.listenToCheckBox( this.filters[i] );
		}

		this.northPanel.add( filterPanel, BorderLayout.NORTH );
	}

	public void filterItems()
	{	this.filterfield.filterItems();
	}

	public void setButtons( ActionListener [] buttonListeners )
	{	this.setButtons( true, buttonListeners );
	}

	public void setButtons( boolean addFilters, ActionListener [] buttonListeners )
	{
		// Handle buttons along the right hand side, if there are
		// supposed to be buttons.

		if ( buttonListeners != null )
		{
			JPanel eastGridPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
			this.buttons = new JButton[ buttonListeners.length ];

			for ( int i = 0; i < buttonListeners.length; ++i )
			{
				if ( buttonListeners[i] instanceof JButton )
				{
					this.buttons[i] = (JButton) buttonListeners[i];
				}
				else
				{
					this.buttons[i] = new JButton( buttonListeners[i].toString() );
					this.buttons[i].addActionListener( buttonListeners[i] );
				}

				eastGridPanel.add( this.buttons[i] );
			}

			this.eastPanel.add( eastGridPanel, BorderLayout.NORTH );
		}

		// Handle filters along the top always, whenever buttons
		// are added.

		if ( !addFilters )
			this.filters = null;
		else
			this.addFilters();

		// If there are buttons, they likely need movers.  Therefore, add
		// some movers to everything.

		if ( addFilters )
			addMovers();

		if ( buttonListeners != null )
			this.actualPanel.add( this.eastPanel, BorderLayout.EAST );
	}

	public void addMovers()
	{
		JPanel moverPanel = new JPanel();

		this.movers = new JRadioButton[4];
		this.movers[0] = new JRadioButton( "max possible" );
		this.movers[1] = new JRadioButton( "all but one" );
		this.movers[2] = new JRadioButton( "multiple", true );
		this.movers[3] = new JRadioButton( "exactly one" );

		ButtonGroup moverGroup = new ButtonGroup();
		for ( int i = 0; i < 4; ++i )
		{
			moverGroup.add( this.movers[i] );
			moverPanel.add( this.movers[i] );
		}

		this.northPanel.add( moverPanel, BorderLayout.SOUTH );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( this.elementList == null || this.buttons == null )
			return;

		if ( this.buttons.length > 0 && this.buttons[ this.buttons.length - 1 ] == null )
			return;

		this.elementList.setEnabled( isEnabled );
		for ( int i = 0; i < this.buttons.length; ++i )
			this.buttons[i].setEnabled( isEnabled );
	}

	public Object [] getDesiredItems( String message )
	{
		if ( this.movers == null || this.movers[2].isSelected() )
			return this.getDesiredItems( message, message.equals( "Queue" ) || message.equals( "Consume" ) ? USE_MULTIPLE : TAKE_MULTIPLE );

		return this.getDesiredItems( message, this.movers[0].isSelected() ? TAKE_ALL : this.movers[1].isSelected() ? TAKE_ALL_BUT_ONE : TAKE_ONE );
	}

	public Object [] getDesiredItems( String message, int quantityType )
	{
		Object [] items = this.elementList.getSelectedValues();
		if ( items.length == 0 )
			return null;

		int neededSize = items.length;

		String itemName;
		int itemCount;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] instanceof AdventureResult )
			{
				itemName = ((AdventureResult) items[i]).getName();
				itemCount = ((AdventureResult) items[i]).getCount();
			}
			else
			{
				itemName = ((Concoction) items[i]).getName();
				itemCount = ((Concoction) items[i]).getTotal();
			}

			int quantity = 0;
			switch ( quantityType )
			{
				case TAKE_ALL:
					quantity = itemCount;
					break;

				case TAKE_ALL_BUT_ONE:
					quantity = itemCount - 1;
					break;

				case TAKE_MULTIPLE:
					quantity = KoLFrame.getQuantity( message + " " + itemName + "...", itemCount );
					if ( itemCount > 0 && quantity == 0 )
						return new Object[0];
					break;

				case USE_MULTIPLE:

					int standard = itemCount;

					if ( items[i] instanceof Concoction )
					{
						int previous = 0, capacity = itemCount, unit = 1;

						if ( ((Concoction)items[i]).getFullness() > 0 )
						{
							previous = KoLCharacter.getFullness() + ConcoctionsDatabase.getQueuedFullness();
							capacity = KoLCharacter.getFullnessLimit();
							unit = ((Concoction)items[i]).getFullness();
						}
						else if ( ((Concoction)items[i]).getInebriety() > 0 )
						{
							previous = KoLCharacter.getInebriety() + ConcoctionsDatabase.getQueuedInebriety();
							capacity = KoLCharacter.getInebrietyLimit();
							unit = ((Concoction)items[i]).getInebriety();
						}

						standard = (capacity - previous) / unit;
						standard = standard == 0 || capacity < previous ? itemCount : Math.min( standard, itemCount );
					}
					else
					{
						standard = ConsumeItemRequest.maximumUses( TradeableItemDatabase.getItemId( itemName ), false );
					}

					quantity = standard < 2 ? standard : KoLFrame.getQuantity( message + " " + itemName + "...",
						itemCount, Math.min( standard, itemCount ) );

					if ( itemCount > 0 && quantity == 0 )
						return new Object[0];

					break;

				default:
					quantity = 1;
					break;
			}

			quantity = Math.min( quantity, itemCount );

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity <= 0 )
			{
				items[i] = null;
				--neededSize;
			}
			else if ( items[i] instanceof AdventureResult )
			{
				items[i] = ((AdventureResult)items[i]).getInstance( quantity );
			}
			else
			{
				ConcoctionsDatabase.push( (Concoction) items[i], quantity );
				items[i] = null;
			}
		}

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		Object [] desiredItems = new Object[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
			if ( items[i] != null )
				desiredItems[ neededSize++ ] = items[i];

		return desiredItems;
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

		public Object [] initialSetup()
		{
			Object [] items = ItemManagePanel.this.getDesiredItems( this.description );
			if (items == null )
				return null;

			if ( this.retrieveFromClosetFirst )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.CLOSET_TO_INVENTORY, items ) );

			return items;
		}
	}

	public class ConsumeListener extends ThreadedListener
	{
		public void run()
		{
			Object [] items = ItemManagePanel.this.getDesiredItems( "Consume" );
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
			Object [] items = this.initialSetup();
			if ( items == null )
				return;

			if ( !this.retrieveFromClosetFirst )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
		}

		public String toString()
		{	return this.retrieveFromClosetFirst ? "inventory" : "closet";
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
			if ( this.sellType == AutoSellRequest.AUTOMALL && !KoLCharacter.hasStore() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
				return;
			}

			if ( this.sellType == AutoSellRequest.AUTOSELL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
				"Are you sure you would like to sell the selected items?",
					"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
						return;

			if ( this.sellType == AutoSellRequest.AUTOMALL && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
				"Are you sure you would like to place the selected items in your store?",
					"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
						return;

			Object [] items = this.initialSetup();
			if ( items == null )
				return;

			RequestThread.postRequest( new AutoSellRequest( items, this.sellType ) );
		}

		public String toString()
		{	return this.sellType == AutoSellRequest.AUTOSELL ? "auto sell" : "place in mall";
		}
	}

	public class GiveToClanListener extends TransferListener
	{
		public GiveToClanListener( boolean retrieveFromClosetFirst )
		{	super( "Stashing", retrieveFromClosetFirst );
		}

		public void run()
		{
			Object [] items = this.initialSetup();
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
			Object [] items = this.initialSetup();
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
			Object [] items = this.initialSetup();
			if ( items == null || items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[i] ) );
		}

		public String toString()
		{	return "pulverize";
		}
	}

	/**
	 * Special instance of a JComboBox which overrides the default
	 * key events of a JComboBox to allow you to catch key events.
	 */

	public class FilterItemField extends JTextField implements ActionListener
	{
		public SimpleListFilter filter;
		public boolean food, booze, equip, restores, other, notrade;

		public FilterItemField()
		{
			this.filter = this.getFilter();
			this.addKeyListener( new FilterListener() );

			this.food = true; this.booze = true; this.equip = true;
			this.restores = true; this.other = true; this.notrade = true;
		}

		public SimpleListFilter getFilter()
		{	return new ConsumptionBasedFilter();
		}

		public void actionPerformed( ActionEvent e )
		{	this.filterItems();
		}

		public class FilterListener extends KeyAdapter
		{
			public void keyReleased( KeyEvent e )
			{	FilterItemField.this.filterItems();
			}
		}

		public void filterItems()
		{
			if ( ItemManagePanel.this.filters != null )
			{
				this.food = ItemManagePanel.this.filters[0].isSelected();
				this.booze = ItemManagePanel.this.filters[1].isSelected();
				this.equip = ItemManagePanel.this.filters[2].isSelected();

				this.other = ItemManagePanel.this.filters[3].isSelected();
				this.restores = this.other;
				this.notrade = ItemManagePanel.this.filters[4].isSelected();
			}

			this.filter.makeStrict();
			ItemManagePanel.this.elementList.applyFilter( this.filter );

			if ( ItemManagePanel.this.elementModel.getSize() == 0 )
			{
				this.filter.makeFuzzy();
				ItemManagePanel.this.elementList.applyFilter( this.filter );
			}
		}

		public class ConsumptionBasedFilter extends SimpleListFilter
		{
			public ConsumptionBasedFilter()
			{	super( FilterItemField.this );
			}

			public boolean isVisible( Object element )
			{
				if ( this.isNonResult( element ) )
					return ItemManagePanel.this.filters == null && super.isVisible( element );

				boolean isItem = element instanceof ItemCreationRequest;
				isItem |= element instanceof AdventureResult && ((AdventureResult)element).isItem();

				if ( !isItem )
					return super.isVisible( element );

				String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
				int itemId = TradeableItemDatabase.getItemId( name );

				if ( !FilterItemField.this.notrade && !TradeableItemDatabase.isTradeable( itemId ) )
					return false;

				boolean isVisibleWithFilter = true;

				switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
				{
				case CONSUME_EAT:
					isVisibleWithFilter = FilterItemField.this.food;
					break;

				case CONSUME_DRINK:
					isVisibleWithFilter = FilterItemField.this.booze;
					break;

				case EQUIP_HAT:
				case EQUIP_SHIRT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
				case EQUIP_PANTS:
				case EQUIP_ACCESSORY:
				case EQUIP_FAMILIAR:
					isVisibleWithFilter = FilterItemField.this.equip;
					break;

				default:

					if ( element instanceof AdventureResult )
					{
						// Milk of magnesium is marked as food, as are
						// munchies pills; all others are marked as expected.

						isVisibleWithFilter = FilterItemField.this.other;
						if ( name.equalsIgnoreCase( "milk of magnesium" ) || name.equalsIgnoreCase( "munchies pills" ) )
							isVisibleWithFilter |= FilterItemField.this.food;
					}
					else
					{
						switch ( ConcoctionsDatabase.getMixingMethod( itemId ) )
						{
						case COOK:
						case COOK_REAGENT:
						case SUPER_REAGENT:
							isVisibleWithFilter = FilterItemField.this.food || FilterItemField.this.other;
							break;

						case COOK_PASTA:
						case WOK:
							isVisibleWithFilter = FilterItemField.this.food;
							break;

						case MIX:
						case MIX_SPECIAL:
						case STILL_BOOZE:
						case MIX_SUPER:
							isVisibleWithFilter = FilterItemField.this.booze;
							break;

						default:
							isVisibleWithFilter = FilterItemField.this.other;
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

	private class RefreshButton extends RequestButton
	{
		public RefreshButton()
		{	super( "refresh", new EquipmentRequest( EquipmentRequest.CLOSET ) );
		}

		public void run()
		{
			super.run();
			ItemManagePanel.this.elementList.updateUI();
		}
	}
}
