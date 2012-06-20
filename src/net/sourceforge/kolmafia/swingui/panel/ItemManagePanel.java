/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.button.RequestButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class ItemManagePanel
	extends ScrollablePanel
{
	public static final int USE_MULTIPLE = 0;

	public static final int TAKE_ALL = 1;
	public static final int TAKE_ALL_BUT_USABLE = 2;
	public static final int TAKE_MULTIPLE = 3;
	public static final int TAKE_ONE = 4;

	public JPanel northPanel;
	public LockableListModel elementModel;
	public ShowDescriptionList elementList;

	public JButton[] buttons;
	public JCheckBox[] filters;
	public JRadioButton[] movers;

	protected final AutoFilterTextField filterfield;
	private JPanel buttonPanel;

	public ItemManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel )
	{
		this(
			confirmedText,
			cancelledText,
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables() );
	}

	public ItemManagePanel( final String confirmedText, final String cancelledText,
		final LockableListModel elementModel, final boolean addFilterField, final boolean addRefreshButton )
	{
		super( "", confirmedText, cancelledText, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) this.scrollComponent;
		this.elementModel = (LockableListModel) this.elementList.getModel();

		this.elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.elementList.setVisibleRowCount( 8 );

		this.filterfield = this.getWordFilter();

		if ( addFilterField )
		{
			this.centerPanel.add( this.filterfield, BorderLayout.NORTH );
		}

		if ( addRefreshButton )
		{
			this.eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
		}

		this.northPanel = new JPanel( new BorderLayout() );
		this.actualPanel.add( this.northPanel, BorderLayout.NORTH );
	}

	protected AutoFilterTextField getWordFilter()
	{
		return new FilterItemField();
	}

	protected void listenToCheckBox( final JCheckBox box )
	{
		box.addActionListener( this.filterfield );
	}

	protected void listenToRadioButton( final JRadioButton button )
	{
		button.addActionListener( this.filterfield );
	}

	public ItemManagePanel( final LockableListModel elementModel )
	{
		this(
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables() );
	}

	public ItemManagePanel( final LockableListModel elementModel, final boolean addFilterField,
		final boolean addRefreshButton )
	{
		super( "", null, null, new ShowDescriptionList( elementModel ), false );

		this.elementList = (ShowDescriptionList) this.scrollComponent;
		this.elementModel = (LockableListModel) this.elementList.getModel();

		this.northPanel = new JPanel( new BorderLayout() );
		this.actualPanel.add( this.northPanel, BorderLayout.NORTH );

		this.filterfield = this.getWordFilter();

		if ( addFilterField )
		{
			this.centerPanel.add( this.filterfield, BorderLayout.NORTH );
		}

		if ( addRefreshButton )
		{
			this.eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
		}
	}

	@Override
	public void actionConfirmed()
	{
	}

	@Override
	public void actionCancelled()
	{
	}

	public void setFixedFilter( final boolean food, final boolean booze, final boolean equip, final boolean other,
		final boolean notrade )
	{
		if ( this.filterfield instanceof FilterItemField )
		{
			FilterItemField itemfilter = (FilterItemField) this.filterfield;

			itemfilter.food = food;
			itemfilter.booze = booze;
			itemfilter.equip = equip;
			itemfilter.other = other;
			itemfilter.notrade = notrade;
		}

		this.filterItems();
	}

	public void addFilters()
	{
		JPanel filterPanel = new JPanel();
		this.filters = new JCheckBox[ 5 ];

		this.filters[ 0 ] = new JCheckBox( "food", KoLCharacter.canEat() );
		this.filters[ 1 ] = new JCheckBox( "booze", KoLCharacter.canDrink() );
		this.filters[ 2 ] = new JCheckBox( "equip", true );
		this.filters[ 3 ] = new JCheckBox( "others", true );
		this.filters[ 4 ] = new JCheckBox( "no-trade", true );

		for ( int i = 0; i < 5; ++i )
		{
			filterPanel.add( this.filters[ i ] );
			this.listenToCheckBox( this.filters[ i ] );
		}

		this.northPanel.add( filterPanel, BorderLayout.NORTH );
		this.filterItems();
	}

	public void filterItems()
	{
		this.filterfield.update();
	}

	public void setButtons( final ActionListener[] buttonListeners )
	{
		this.setButtons( true, buttonListeners );
	}

	public void setButtons( boolean addFilters, final ActionListener[] buttonListeners )
	{
		// Handle buttons along the right hand side, if there are
		// supposed to be buttons.

		if ( buttonListeners != null )
		{
			this.buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
			this.buttons = new JButton[ buttonListeners.length ];

			for ( int i = 0; i < buttonListeners.length; ++i )
			{
				if ( buttonListeners[ i ] instanceof JButton )
				{
					this.buttons[ i ] = (JButton) buttonListeners[ i ];
				}
				else
				{
					this.buttons[ i ] = new JButton( buttonListeners[ i ].toString() );
					this.buttons[ i ].addActionListener( buttonListeners[ i ] );
				}

				this.buttonPanel.add( this.buttons[ i ] );
			}

			this.eastPanel.add( this.buttonPanel, BorderLayout.NORTH );
		}

		// Handle filters and movers along the top

		if ( addFilters )
		{
			this.addFilters();
			this.addMovers();
		}
		else
		{
			this.filters = null;
		}

		if ( buttonListeners != null )
		{
			this.actualPanel.add( this.eastPanel, BorderLayout.EAST );
		}
	}

	public void addButtons( final JButton[] buttons )
	{
		this.addButtons( buttons, true );
	}

	public void addButtons( final JButton[] buttons, final boolean save )
	{
		for ( int i = 0; i < buttons.length; ++i )
		{
			this.buttonPanel.add( buttons[i] );
		}

		if ( !save )
		{
			return;
		}

		JButton[] oldButtons = this.buttons;
		int oldSize = oldButtons.length;
		int newSize = oldSize + buttons.length;
		JButton[] newButtons = new JButton[ newSize ];

		// Copy in the old buttons
		for ( int i = 0; i < oldSize; ++i )
		{
			newButtons[ i ] = oldButtons[ i ];
		}

		// Copy in the new buttons
		for ( int i = oldSize; i < newSize; ++i )
		{
			JButton newButton = buttons[ i - oldSize ];
			newButtons[ i ] = newButton;
		}

		// Save the button list
		this.buttons = newButtons;
	}

	public void addMovers()
	{
		JPanel moverPanel = new JPanel();

		this.movers = new JRadioButton[ 4 ];
		this.movers[ 0 ] = new JRadioButton( "max possible" );
		this.movers[ 1 ] = new JRadioButton( "all but usable" );
		this.movers[ 2 ] = new JRadioButton( "multiple", true );
		this.movers[ 3 ] = new JRadioButton( "exactly one" );

		ButtonGroup moverGroup = new ButtonGroup();
		for ( int i = 0; i < 4; ++i )
		{
			moverGroup.add( this.movers[ i ] );
			moverPanel.add( this.movers[ i ] );
		}

		this.northPanel.add( moverPanel, BorderLayout.SOUTH );
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		if ( this.elementList == null || this.buttons == null )
		{
			super.setEnabled( isEnabled );
			return;
		}

		if ( this.buttons.length > 0 && this.buttons[ this.buttons.length - 1 ] == null )
		{
			super.setEnabled( isEnabled );
			return;
		}

		this.elementList.setEnabled( isEnabled );
		for ( int i = 0; i < this.buttons.length; ++i )
		{
			this.buttons[ i ].setEnabled( isEnabled );
		}
	}

	public Object[] getDesiredItems( final String message )
	{
		if ( this.movers == null || this.movers[ 2 ].isSelected() )
		{
			return this.getDesiredItems(
				message,
				message.equals( "Queue" ) || message.equals( "Consume" ) || message.equals( "Feed" ) ? ItemManagePanel.USE_MULTIPLE : ItemManagePanel.TAKE_MULTIPLE );
		}

		return this.getDesiredItems(
			message,
			this.movers[ 0 ].isSelected() ? ItemManagePanel.TAKE_ALL : this.movers[ 1 ].isSelected() ? ItemManagePanel.TAKE_ALL_BUT_USABLE : ItemManagePanel.TAKE_ONE );
	}

	public Object[] getDesiredItems( final String message, final int quantityType )
	{
		Object[] items = this.elementList.getSelectedValues();
		if ( items.length == 0 )
		{
			return null;
		}

		int neededSize = items.length;
		boolean isTally = this.elementList.getOriginalModel() == KoLConstants.tally;

		String itemName;
		int itemCount, quantity;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] == null )
			{
				--neededSize;
				continue;
			}

			if ( items[ i ] instanceof AdventureResult )
			{
				itemName = ( (AdventureResult) items[ i ] ).getName();
				itemCount =
					isTally ? ( (AdventureResult) items[ i ] ).getCount( KoLConstants.inventory ) : ( (AdventureResult) items[ i ] ).getCount();
			}
			else
			{
				itemName = ( (Concoction) items[ i ] ).getName();
				itemCount = ( (Concoction) items[ i ] ).getAvailable();
			}

			quantity =
				Math.min(
					this.getDesiredItemAmount( items[ i ], itemName, itemCount, message, quantityType ), itemCount );
			if ( quantity == Integer.MIN_VALUE )
			{
				return null;
			}

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity <= 0 )
			{
				items[ i ] = null;
				--neededSize;
			}
			else if ( items[ i ] instanceof AdventureResult )
			{
				items[ i ] = ( (AdventureResult) items[ i ] ).getInstance( quantity );
			}
			else
			{
				ConcoctionDatabase.push( (Concoction) items[ i ], quantity );
				items[ i ] = null;
			}
		}

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		if ( neededSize == 0 )
		{
			return null;
		}

		Object[] desiredItems = new Object[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[ i ] != null )
			{
				desiredItems[ neededSize++ ] = items[ i ];
			}
		}

		return desiredItems;
	}

	protected int getDesiredItemAmount( final Object item, final String itemName, final int itemCount,
		final String message, final int quantityType )
	{
		int quantity = 0;
		switch ( quantityType )
		{
		case TAKE_ALL:
			quantity = itemCount;
			break;

		case TAKE_ALL_BUT_USABLE:
			quantity = itemCount - this.getUsableItemAmount( item, itemName );
			break;

		case TAKE_MULTIPLE:
		{
			Integer value = InputFieldUtilities.getQuantity( message + " " + itemName + "...", itemCount );
			if ( value == null )
			{
				return Integer.MIN_VALUE;
			}
				
			quantity = value.intValue();

			break;
		}

		case USE_MULTIPLE:

			int standard = itemCount;

			if ( !message.equals( "Feed" ) )
			{
				if ( item instanceof Concoction )
				{
					int previous = 0, capacity = itemCount, unit = 0;

					if ( ( (Concoction) item ).getFullness() > 0 )
					{
						previous = KoLCharacter.getFullness() + ConcoctionDatabase.getQueuedFullness();
						capacity = KoLCharacter.getFullnessLimit();
						unit = ( (Concoction) item ).getFullness();
						standard = previous >= capacity ? itemCount : Math.min( ( capacity - previous ) / unit, itemCount );
					}
					else if ( ( (Concoction) item ).getInebriety() > 0 )
					{
						previous = KoLCharacter.getInebriety() + ConcoctionDatabase.getQueuedInebriety();
						capacity = KoLCharacter.getInebrietyLimit();
						unit = ( (Concoction) item ).getInebriety();
						standard = previous > capacity ? itemCount : Math.max( 1, Math.min( ( capacity - previous ) / unit, itemCount ) );
					}
					else if ( ( (Concoction) item ).getSpleenHit() > 0 )
					{
						previous = KoLCharacter.getSpleenUse() + ConcoctionDatabase.getQueuedSpleenHit();
						capacity = KoLCharacter.getSpleenLimit();
						unit = ( (Concoction) item ).getSpleenHit();
						standard = previous >= capacity ? itemCount : Math.min( ( capacity - previous ) / unit, itemCount );
					}
				}

				int maximum = UseItemRequest.maximumUses( itemName );

				standard = Math.min( standard, maximum );
			}

			quantity = standard;
			if ( standard >= 2 )
			{
				Integer value = InputFieldUtilities.getQuantity( message + " " + itemName + "...", itemCount, Math.min( standard, itemCount ) );
				if ( value == null )
				{
					return Integer.MIN_VALUE;
				}
				quantity = value.intValue();
			}

			break;

		default:
			quantity = 1;
			break;
		}

		return quantity;
	}

	protected int getUsableItemAmount( final Object item, final String itemName )
	{
		int id;
		if ( item instanceof Concoction )
		{
			id = ((Concoction) item).getItemId();
		}
		else
		{
			id = ((AdventureResult) item).getItemId();
		}
		switch ( ItemDatabase.getConsumptionType( id ) )
		{
		case KoLConstants.EQUIP_HAT:
			return Preferences.getInteger( "usableHats" );
		case KoLConstants.EQUIP_WEAPON:
			switch ( EquipmentDatabase.getHands( itemName ) )
			{
			case 3:
				return Preferences.getInteger( "usable3HWeapons" );
			case 2:
				return Preferences.getInteger( "usable2HWeapons" );
			default:
				return Preferences.getInteger( "usable1HWeapons" );
			}
		case KoLConstants.EQUIP_OFFHAND:
			return Preferences.getInteger( "usableOffhands" );
		case KoLConstants.EQUIP_SHIRT:
			return Preferences.getInteger( "usableShirts" );
		case KoLConstants.EQUIP_PANTS:
			return Preferences.getInteger( "usablePants" );
		case KoLConstants.EQUIP_ACCESSORY:
			Modifiers mods = Modifiers.getModifiers( itemName );
			if ( mods != null && mods.getBoolean( Modifiers.SINGLE ) )
			{
				return Preferences.getInteger( "usable1xAccs" );
			}
			else
			{
				return Preferences.getInteger( "usableAccessories" );
			}
		default:
			return Preferences.getInteger( "usableOther" );
		}
	}

	public abstract class TransferListener
		extends ThreadedListener
	{
		public String description;
		public boolean retrieveFromClosetFirst;

		public TransferListener( final String description, final boolean retrieveFromClosetFirst )
		{
			this.description = description;
			this.retrieveFromClosetFirst = retrieveFromClosetFirst;
		}

		public Object[] initialSetup()
		{
			return this.retrieveItems( ItemManagePanel.this.getDesiredItems( this.description ) );
		}

		public Object[] initialSetup( final int transferType )
		{
			return this.retrieveItems( ItemManagePanel.this.getDesiredItems( this.description, transferType ) );
		}

		private Object[] retrieveItems( final Object[] items )
		{
			if ( items == null )
			{
				return null;
			}

			if ( this.retrieveFromClosetFirst )
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, items ) );
			}

			return items;
		}
		
		@Override
		protected boolean retainFocus()
		{
			return true;
		}
	}

	public class ConsumeListener
		extends TransferListener
	{
		public ConsumeListener( final boolean retrieveFromClosetFirst )
		{
			super( "Consume", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				RequestThread.postRequest( UseItemRequest.getInstance( (AdventureResult) items[ i ] ) );
			}
		}

		@Override
		public String toString()
		{
			return "use item";
		}
	}

	public class EquipListener
		extends TransferListener
	{
		public EquipListener( final boolean retrieveFromClosetFirst )
		{
			super( "Equip", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				int usageType = ItemDatabase.getConsumptionType( item.getItemId() );

				switch ( usageType )
				{
				case KoLConstants.EQUIP_FAMILIAR:
				case KoLConstants.EQUIP_ACCESSORY:
				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_CONTAINER:
				case KoLConstants.EQUIP_SHIRT:
				case KoLConstants.EQUIP_WEAPON:
				case KoLConstants.EQUIP_OFFHAND:
					RequestThread.postRequest( new EquipmentRequest(
									   item, EquipmentManager.consumeFilterToEquipmentType( usageType ) ) );
				}
			}
		}

		@Override
		public String toString()
		{
			return "equip item";
		}
	}

	public class PutInClosetListener
		extends TransferListener
	{
		public PutInClosetListener( final boolean retrieveFromClosetFirst )
		{
			super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( !this.retrieveFromClosetFirst )
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );
			}
		}

		@Override
		public String toString()
		{
			return this.retrieveFromClosetFirst ? "put in bag" : "put in closet";
		}
	}

	public class AutoSellListener
		extends TransferListener
	{
		private final boolean autosell;

		public AutoSellListener( final boolean retrieveFromClosetFirst, final boolean autosell )
		{
			super( autosell ? "Autoselling" : "Mallselling", retrieveFromClosetFirst );
			this.autosell = autosell;
		}

		@Override
		protected void execute()
		{
			if ( !this.autosell && !KoLCharacter.hasStore() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a store in the mall." );
				return;
			}

			if ( this.autosell && !InputFieldUtilities.confirm( "Are you sure you would like to sell the selected items?" ) )
			{
				return;
			}

			if ( !this.autosell && !InputFieldUtilities.confirm( "Are you sure you would like to place the selected items in your store?" ) )
			{
				return;
			}

			Object[] items = this.initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( autosell )
			{
				RequestThread.postRequest( new AutoSellRequest( items ) );
			}
			else
			{
				RequestThread.postRequest( new AutoMallRequest( items ) );
			}
		}

		@Override
		public String toString()
		{
			return this.autosell ? "auto sell" : "place in mall";
		}
	}

	public class GiveToClanListener
		extends TransferListener
	{
		public GiveToClanListener( final boolean retrieveFromClosetFirst )
		{
			super( "Stashing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null )
			{
				return;
			}

			RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
		}

		@Override
		public String toString()
		{
			return "clan stash";
		}
	}

	public class PutOnDisplayListener
		extends TransferListener
	{
		public PutOnDisplayListener( final boolean retrieveFromClosetFirst )
		{
			super( "Showcasing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( !KoLCharacter.hasDisplayCase() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a display case in the Cannon Museum." );
				return;
			}

			RequestThread.postRequest( new DisplayCaseRequest( items, true ) );
		}

		@Override
		public String toString()
		{
			return "display case";
		}
	}

	public class PulverizeListener
		extends TransferListener
	{
		public PulverizeListener( final boolean retrieveFromClosetFirst )
		{
			super( "Smashing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[ i ] ) );
			}
		}

		@Override
		public String toString()
		{
			return "pulverize";
		}
	}

	/**
	 * Special instance of a JComboBox which overrides the default key events of a JComboBox to allow you to catch key
	 * events.
	 */

	public class FilterItemField
		extends AutoFilterTextField
	{
		public boolean food, booze, equip, restores, other, notrade;

		public FilterItemField()
		{
			super( ItemManagePanel.this.elementList );

			this.food = true;
			this.booze = true;
			this.equip = true;
			this.restores = true;
			this.other = true;
			this.notrade = true;
		}

		@Override
		public void update()
		{
			if ( ItemManagePanel.this.filters != null )
			{
				this.food = ItemManagePanel.this.filters[ 0 ].isSelected();
				this.booze = ItemManagePanel.this.filters[ 1 ].isSelected();
				this.equip = ItemManagePanel.this.filters[ 2 ].isSelected();

				this.other = ItemManagePanel.this.filters[ 3 ].isSelected();
				this.restores = this.other;
				this.notrade = ItemManagePanel.this.filters[ 4 ].isSelected();
			}

			super.update();
		}

		@Override
		public boolean isVisible( final Object element )
		{
			if ( element instanceof AdventureResult && ( (AdventureResult) element ).getCount() < 0 )
			{
				// return false;
			}

			String name = AutoFilterTextField.getResultName( element );
			boolean isVisibleWithFilter = true;

			switch ( ItemDatabase.getConsumptionType( name ) )
			{
			case KoLConstants.CONSUME_EAT:
				isVisibleWithFilter = FilterItemField.this.food;
				break;

			case KoLConstants.CONSUME_DRINK:
				isVisibleWithFilter = FilterItemField.this.booze;
				break;

			case KoLConstants.EQUIP_HAT:
			case KoLConstants.EQUIP_SHIRT:
			case KoLConstants.EQUIP_WEAPON:
			case KoLConstants.EQUIP_OFFHAND:
			case KoLConstants.EQUIP_PANTS:
			case KoLConstants.EQUIP_CONTAINER:
			case KoLConstants.EQUIP_ACCESSORY:
			case KoLConstants.EQUIP_FAMILIAR:
				isVisibleWithFilter = FilterItemField.this.equip;
				break;

			default:

				if ( element instanceof CreateItemRequest )
				{
					switch ( ConcoctionDatabase.getMixingMethod( name ) & KoLConstants.CT_MASK )
					{
					case KoLConstants.COOK:
					case KoLConstants.COOK_FANCY:
						isVisibleWithFilter = FilterItemField.this.food || FilterItemField.this.other;
						break;

					case KoLConstants.WOK:
					case KoLConstants.SUSHI:
						isVisibleWithFilter = FilterItemField.this.food;
						break;

					case KoLConstants.MIX:
					case KoLConstants.MIX_FANCY:
					case KoLConstants.STILL_BOOZE:
						isVisibleWithFilter = FilterItemField.this.booze;
						break;

					default:
						isVisibleWithFilter = FilterItemField.this.other;
						break;
					}
				}
				else
				{
					// Milk of magnesium is marked as food,
					// as are munchies pills; all others
					// are marked as expected.

					isVisibleWithFilter = FilterItemField.this.other;
					if ( name.equalsIgnoreCase( "milk of magnesium" ) ||
					     name.equalsIgnoreCase( "munchies pills" ) ||
					     name.equalsIgnoreCase( "distention pill" ))
					{
						isVisibleWithFilter |= FilterItemField.this.food;
					}
				}
			}

			if ( !isVisibleWithFilter )
			{
				return false;
			}

			int itemId = element instanceof AdventureResult ?
				( (AdventureResult) element ).getItemId() :
				ItemDatabase.getItemId( name, 1, false );

			if ( itemId < 1 )
			{
				return ItemManagePanel.this.filters == null && super.isVisible( element );
			}

			if ( !FilterItemField.this.notrade && !ItemDatabase.isTradeable( itemId ) )
			{
				return false;
			}

			return super.isVisible( element );
		}
	}

	protected class RefreshButton
		extends RequestButton
	{
		public RefreshButton()
		{
			super( "refresh", new EquipmentRequest( EquipmentRequest.REFRESH ) );
		}
	}
}
