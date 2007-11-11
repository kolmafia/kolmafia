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
import java.awt.CardLayout;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class ItemManageFrame extends KoLFrame
{
	private static int pullsRemaining = 0;
	private static UnfocusedTabbedPane fullnessTabs;
	private static UnfocusedTabbedPane inebrietyTabs;

	private static final JLabel pullsRemainingLabel1 = new JLabel( " " );
	private static final JLabel pullsRemainingLabel2 = new JLabel( " " );

	private LockableListModel itemPanelNames = new LockableListModel();
	private JList itemPanelList = new JList( this.itemPanelNames );
	private CardLayout itemPanelCards = new CardLayout();
	private JPanel managePanel = new JPanel( this.itemPanelCards );

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public ItemManageFrame()
	{	this( true );
	}

	public ItemManageFrame( boolean useTabs )
	{
		super( "Item Manager" );

		this.addPanel( "Usable", new UsableItemPanel() );

		JPanel foodPanel = new JPanel( new BorderLayout() );

		if ( KoLSettings.getBooleanProperty( "addCreationQueue" ) )
			foodPanel.add( new ConsumePanel( true, false ), BorderLayout.NORTH );
		foodPanel.add( new QueuePanel( true, false ), BorderLayout.CENTER );

		this.addPanel( " - Food", foodPanel );

		JPanel boozePanel = new JPanel( new BorderLayout() );

		if ( KoLSettings.getBooleanProperty( "addCreationQueue" ) )
			boozePanel.add( new ConsumePanel( false, true ), BorderLayout.NORTH );
		boozePanel.add( new QueuePanel( false, true ), BorderLayout.CENTER );

		this.addPanel( " - Booze", boozePanel );
		this.addPanel( " - Restores", new RestorativeItemPanel() );

		this.addSeparator();

		this.addPanel( "General", new InventoryManagePanel( inventory, false ) );
		this.addPanel( " - Recent", new InventoryManagePanel( tally, false ) );
		this.addPanel( " - Closet", new InventoryManagePanel( closet, false ) );
		if ( !KoLCharacter.inBadMoon() )
			this.addPanel( " - Storage", new HagnkStoragePanel( false ) );

		this.addSeparator();

		this.addPanel( "Creatable", new CreateItemPanel( true, true, true, true ) );

		this.addPanel( " - Cookable", new CreateItemPanel( true, false, false, false ) );
		this.addPanel( " - Mixable", new CreateItemPanel( false, true, false, false ) );

		this.addSeparator();

		this.addPanel( "Equipment", new InventoryManagePanel( inventory, true ) );
		if ( !KoLCharacter.inBadMoon() )
			this.addPanel( " - Storage", new HagnkStoragePanel( true ) );
		this.addPanel( " - Create", new CreateItemPanel( false, false, true, false ) );

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		this.addSeparator();

		this.itemPanelNames.add( "Item Filters" );
		this.addPanel( " - Mementos", new MementoItemsPanel() );
		this.addPanel( " - Cleanup", new JunkItemsPanel() );
		this.addPanel( " - Keep One", new SingletonItemsPanel() );
		this.addPanel( " - Restock", new RestockPanel() );

		this.itemPanelList.addListSelectionListener( new CardSwitchListener() );
		this.itemPanelList.setPrototypeCellValue( "ABCDEFGHIJKLM" );
		this.itemPanelList.setCellRenderer( new OptionRenderer() );

		JPanel listHolder = new JPanel( new CardLayout( 10, 10 ) );
		listHolder.add( new SimpleScrollPane( this.itemPanelList ), "" );

		JPanel mainPanel = new JPanel( new BorderLayout() );

		mainPanel.add( listHolder, BorderLayout.WEST );
		mainPanel.add( this.managePanel, BorderLayout.CENTER );

		this.itemPanelList.setSelectedIndex( KoLSettings.getIntegerProperty( "itemManagerIndex" ) );
		this.framePanel.add( mainPanel, BorderLayout.CENTER );
	}

	public static final int getPullsRemaining()
	{	return pullsRemaining;
	}

	public static final void setPullsRemaining( int pullsRemaining )
	{
		ItemManageFrame.pullsRemaining = pullsRemaining;

		if ( KoLCharacter.isHardcore() )
		{
			pullsRemainingLabel1.setText( "In Hardcore" );
			pullsRemainingLabel2.setText( "In Hardcore" );
			return;
		}

		switch ( pullsRemaining )
		{
		case 0:
				pullsRemainingLabel1.setText( "No Pulls Left" );
				pullsRemainingLabel2.setText( "No Pulls Left" );
			break;
		case 1:
				pullsRemainingLabel1.setText( "1 Pull Left" );
				pullsRemainingLabel2.setText( "1 Pull Left" );
			break;
		default:
				pullsRemainingLabel1.setText( pullsRemaining + " Pulls Left" );
				pullsRemainingLabel2.setText( pullsRemaining + " Pulls Left" );
		}
	}

	private void addPanel( String name, JComponent panel )
	{
		this.itemPanelNames.add( name );
		this.managePanel.add( panel, String.valueOf( this.itemPanelNames.size() ) );
	}

	private void addSeparator()
	{
		JPanel separator = new JPanel();
		separator.setOpaque( false );
		separator.setLayout( new BoxLayout( separator, BoxLayout.Y_AXIS ) );

		separator.add( Box.createVerticalGlue() );
		separator.add( new JSeparator() );
		this.itemPanelNames.add( separator );
	}

	private class CardSwitchListener implements ListSelectionListener
	{
		public void valueChanged( ListSelectionEvent e )
		{
			int cardIndex = ItemManageFrame.this.itemPanelList.getSelectedIndex();

			if ( ItemManageFrame.this.itemPanelNames.get( cardIndex ) instanceof JComponent )
				return;

			KoLSettings.setUserProperty( "itemManagerIndex", String.valueOf( cardIndex ) );
			ItemManageFrame.this.itemPanelCards.show( ItemManageFrame.this.managePanel, String.valueOf( cardIndex + 1 ) );
		}
	}

	private class JunkItemsPanel extends OverlapPanel
	{
		public JunkItemsPanel()
		{	super( "cleanup", "help", junkList, true );
		}

		public void actionConfirmed()
		{	StaticEntity.getClient().makeJunkRemovalRequest();
		}

		public void actionCancelled()
		{	alert( "These items have been flagged as \"junk\" because at some point in the past, you've opted to autosell all of the item.  If you use the \"cleanup\" command, KoLmafia will dispose of these items either by pulverizing them (equipment) or autoselling them (non-equipment)." );
		}
	}

	private class SingletonItemsPanel extends OverlapPanel
	{
		public SingletonItemsPanel()
		{	super( "closet", "help", singletonList, true );
		}

		public void actionConfirmed()
		{
			AdventureResult current;
			AdventureResult [] items = new AdventureResult[ singletonList.size() ];
			for ( int i = 0; i < singletonList.size(); ++i )
			{
				current = (AdventureResult) singletonList.get(i);
				items[i] = current.getInstance( Math.min( current.getCount( inventory ),
					Math.max( 0, 1 - current.getCount( closet ) ) ) );
			}

			RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );

		}

		public void actionCancelled()
		{	alert( "These items are flagged as \"singletons\".  Using the \"closet\" button, KoLmafia will try to ensure that at least one of the item exists in your closet.\n\nIF THE PLAYER IS STILL IN HARDCORE OR RONIN, these items are treated as a special class of junk items where during the \"cleanup\" routine mentioned in the junk tab, KoLmafia will attempt to leave one of the item in the players inventory.\n\nPlease take note that once the player breaks Ronin, KoLmafia will treat these items as normal junk and ignore the general preservation rule." );
		}
	}
	private class MementoItemsPanel extends OverlapPanel
	{
		public MementoItemsPanel()
		{	super( "closet", "help", mementoList, true );
		}

		public void actionConfirmed()
		{
			AdventureResult current;
			AdventureResult [] items = new AdventureResult[ mementoList.size() ];
			for ( int i = 0; i < mementoList.size(); ++i )
			{
				current = (AdventureResult) mementoList.get(i);
				items[i] = current.getInstance( current.getCount( inventory ) );
			}

			RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
		}

		public void actionCancelled()
		{	alert( "These items are flagged as \"mementos\".  IF YOU SET A PREFERENCE, KoLmafia will never sell or pulverize these items." );
		}
	}

	private static final AdventureResult MAGNESIUM = new AdventureResult( "milk of magnesium", 1, false );

	private class ConsumePanel extends ItemManagePanel
	{
		private boolean food, booze;

		public ConsumePanel( boolean food, boolean booze )
		{
			super( "consume", "create", ConcoctionsDatabase.getUsables(), false, false );

			this.food = food;
			this.booze = booze;

			JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			this.elementList.setCellRenderer( AdventureResult.getCreationQueueRenderer() );
			this.elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

			this.elementList.setVisibleRowCount( 3 );
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			UnfocusedTabbedPane queueTabs = getTabbedPane();

			if ( this.food )
			{
				fullnessTabs = queueTabs;
				queueTabs.addTab( "0 Full Queued", centerPanel );
			}
			else
			{
				inebrietyTabs = queueTabs;
				queueTabs.addTab( "0 Drunk Queued", centerPanel );
			}

			queueTabs.addTab( "Ingredients Used", new SimpleScrollPane( ConcoctionsDatabase.getQueue(), 7 ) );
			actualPanel.add( queueTabs, BorderLayout.CENTER );

			this.eastPanel.add( new UndoQueueButton(), BorderLayout.SOUTH );

			this.setEnabled( true );
			this.filterItems();
		}

		public FilterTextField getWordFilter()
		{	return new ConsumableFilterField();
		}

		public void actionConfirmed()
		{
			ConcoctionsDatabase.handleQueue( true );

			if ( fullnessTabs != null )
				fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
			if ( inebrietyTabs != null )
				inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
		}

		public void actionCancelled()
		{
			ConcoctionsDatabase.handleQueue( false );

			if ( fullnessTabs != null )
				fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
			if ( inebrietyTabs != null )
				inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
		}

		private class UndoQueueButton extends ThreadedButton
		{
			public UndoQueueButton()
			{	super( "undo" );
			}

			public void run()
			{
				ConcoctionsDatabase.pop();
				ConcoctionsDatabase.refreshConcoctions();

				if ( fullnessTabs != null )
					fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
				if ( inebrietyTabs != null )
					inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
		}

		private class ConsumableFilterField extends FilterItemField
		{
			public boolean isVisible( Object element )
			{
				Concoction creation = (Concoction) element;

				if ( creation.getQueued() == 0 )
					return false;

				int fullness = TradeableItemDatabase.getFullness( creation.getName() );
				int inebriety = TradeableItemDatabase.getInebriety( creation.getName() );

				if ( fullness > 0 )
					return ConsumePanel.this.food && super.isVisible( element );
				else if ( inebriety > 0 )
					return ConsumePanel.this.booze && super.isVisible( element );
				else
					return false;
			}
		}
	}

	private class QueuePanel extends ItemManagePanel
	{
		private boolean food, booze;
		private JCheckBox [] filters;

		public QueuePanel( boolean food, boolean booze )
		{
			super( ConcoctionsDatabase.getUsables(), true, true );

			this.food = food;
			this.booze = booze;

			if ( KoLSettings.getBooleanProperty( "addCreationQueue" ) )
				this.setButtons( false, new ActionListener [] { new EnqueueListener(), new ExecuteListener(), new BuffUpListener() } );
			else
				this.setButtons( false, new ActionListener [] { new ExecuteListener(), new BuffUpListener() } );

			JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			this.elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

			this.elementList.setVisibleRowCount( 6 );
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.filters = new JCheckBox[ food || booze ? 5 : 4 ];

			this.filters[0] = new JCheckBox( "no create" );
			this.filters[1] = new JCheckBox( "+mus only" );
			this.filters[2] = new JCheckBox( "+mys only" );
			this.filters[3] = new JCheckBox( "+mox only" );

			for ( int i = 0; i < 4; ++i )
				this.listenToCheckBox( this.filters[i] );

			if ( food || booze )
				this.filters[4] = new ExperimentalCheckbox( food, booze );

			JPanel filterPanel = new JPanel();
			for ( int i = 0; i < this.filters.length; ++i )
				filterPanel.add( this.filters[i] );

			this.setEnabled( true );
			this.northPanel.add( filterPanel, BorderLayout.NORTH );

			this.filterItems();
		}

		public FilterTextField getWordFilter()
		{	return new ConsumableFilterField();
		}

		protected void listenToCheckBox( JCheckBox box )
		{
			super.listenToCheckBox( box );
			box.addActionListener( new ReSortListener() );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		private class ReSortListener extends ThreadedListener
		{
			public void run()
			{	ConcoctionsDatabase.getUsables().sort();
			}
		}

		private class EnqueueListener extends ThreadedListener
		{
			public void run()
			{
				getDesiredItems( "Queue" );
				ConcoctionsDatabase.refreshConcoctions();

				if ( fullnessTabs != null )
					fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
				if ( inebrietyTabs != null )
					inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
			}

			public String toString()
			{	return "enqueue";
			}
		}

		private class ExecuteListener extends ThreadedListener
		{
			public void run()
			{
				getDesiredItems( "Consume" );
				ConcoctionsDatabase.refreshConcoctions();
				ConcoctionsDatabase.handleQueue( true );

				if ( fullnessTabs != null )
					fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
				if ( inebrietyTabs != null )
					inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
			}

			public String toString()
			{	return "consume";
			}
		}

		private class BuffUpListener extends ThreadedListener
		{
			public void run()
			{
				if ( QueuePanel.this.food )
				{
					RequestThread.postRequest( new ConsumeItemRequest( MAGNESIUM ) );
				}
				else
				{
					if ( !activeEffects.contains( new AdventureResult( "Ode to Booze", 1, true ) ) )
						RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
				}
			}

			public String toString()
			{	return QueuePanel.this.food ? "use milk" : "cast ode";
			}
		}

		private class ConsumableFilterField extends FilterItemField
		{
			public boolean isVisible( Object element )
			{
				Concoction creation = (Concoction) element;

				int fullness = TradeableItemDatabase.getFullness( creation.getName() );
				int inebriety = TradeableItemDatabase.getInebriety( creation.getName() );

				if ( fullness > 0 )
				{
					if ( !QueuePanel.this.food )
						return false;
				}
				else if ( inebriety > 0 )
				{
					if ( !QueuePanel.this.booze )
						return false;
				}
				else
					return false;

				if ( creation.getTotal() == 0 )
					return false;

				if ( QueuePanel.this.filters[0].isSelected() )
				{
					AdventureResult item = creation.getItem();
					if ( item != null && item.getCount( inventory ) == 0 )
						return false;
				}

				if ( QueuePanel.this.filters[1].isSelected() )
				{
					String range = TradeableItemDatabase.getMuscleRange( creation.getName() );
					if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
						return false;
				}

				if ( QueuePanel.this.filters[2].isSelected() )
				{
					String range = TradeableItemDatabase.getMysticalityRange( creation.getName() );
					if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
						return false;
				}

				if ( QueuePanel.this.filters[3].isSelected() )
				{
					String range = TradeableItemDatabase.getMoxieRange( creation.getName() );
					if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
						return false;
				}

				return super.isVisible( element );
			}
		}
	}

	private class ExperimentalCheckbox extends JCheckBox implements ActionListener
	{
		public ExperimentalCheckbox( boolean food, boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : "per full" );

			this.setToolTipText( "Sort gains per adventure" );
			this.setSelected( KoLSettings.getBooleanProperty( "showGainsPerUnit" ) );

			this.addActionListener( this );
			KoLSettings.registerCheckbox( "showGainsPerUnit", this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( KoLSettings.getBooleanProperty( "showGainsPerUnit" ) == this.isSelected() )
				return;

			KoLSettings.setUserProperty( "showGainsPerUnit", String.valueOf( this.isSelected() ) );
			ConcoctionsDatabase.getUsables().sort();
		}
	}


	private class CreationSettingCheckBox extends JCheckBox implements ActionListener
	{
		private String property;

		public CreationSettingCheckBox( String label, String property, String tooltip )
		{
			super( label );

			this.setToolTipText( tooltip );
			this.setSelected( KoLSettings.getBooleanProperty( property ) );

			this.addActionListener( this );

			this.property = property;
			KoLSettings.registerCheckbox( property, this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( KoLSettings.getBooleanProperty( this.property ) == this.isSelected() )
				return;

			KoLSettings.setUserProperty( this.property, String.valueOf( this.isSelected() ) );
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	protected class UsableItemPanel extends InventoryManagePanel
	{
		public UsableItemPanel()
		{	super( inventory, false );
		}

		public FilterTextField getWordFilter()
		{	return new UsableItemFilterField();
		}

		public void actionConfirmed()
		{
			Object [] items = this.getDesiredItems( "Consume" );
			if ( items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
		}

		public void actionCancelled()
		{
			String name;
			Object [] values = this.elementList.getSelectedValues();

			for ( int i = 0; i < values.length; ++i )
			{
				name = ((AdventureResult)values[i]).getName();
				if ( name != null )
					StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
			}
		}

		private class UsableItemFilterField extends FilterItemField
		{
			public boolean isVisible( Object element )
			{
				AdventureResult item = (AdventureResult)element;
				int itemId = item.getItemId();

				if ( !UsableItemFilterField.this.notrade && !TradeableItemDatabase.isTradeable( itemId ) )
					 return false;

				boolean filter = false;

				switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
				{
				case CONSUME_EAT:
					filter = UsableItemFilterField.this.food;
					break;

				case CONSUME_DRINK:
					filter = UsableItemFilterField.this.booze;
					break;

				case CONSUME_USE:
				case MESSAGE_DISPLAY:
				case INFINITE_USES:
				case CONSUME_MULTIPLE:
				case GROW_FAMILIAR:
				case CONSUME_ZAP:
				case MP_RESTORE:
				case HP_RESTORE:
					filter = UsableItemFilterField.this.other;
					break;

				case EQUIP_FAMILIAR:
				case EQUIP_ACCESSORY:
				case EQUIP_HAT:
				case EQUIP_PANTS:
				case EQUIP_SHIRT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
					filter = UsableItemFilterField.this.equip;
					break;

				default:
					return false;
				}

				return filter && super.isVisible( element );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends InventoryManagePanel
	{
		public CreateItemPanel( boolean food, boolean booze, boolean equip, boolean other )
		{
			super( "create item", "create & use", ConcoctionsDatabase.getCreatables(), equip && !other );

			if ( this.isEquipmentOnly )
			{
				super.addFilters();
			}
			else
			{
				JPanel filterPanel = new JPanel();
				filterPanel.add( new CreationSettingCheckBox( "require in-a-boxes", "requireBoxServants", "Do not cook/mix without chef/bartender" ) );
				filterPanel.add( new CreationSettingCheckBox( "repair on explosion", "autoRepairBoxServants", "Automatically repair chefs and bartenders on explosion" ) );

				this.northPanel.add( filterPanel, BorderLayout.NORTH );
				this.setFixedFilter( food, booze, equip, other, true );
			}

			ConcoctionsDatabase.getCreatables().updateFilter( false );
		}

		public void addFilters()
		{
		}

		public void actionConfirmed()
		{
			Object selected = this.elementList.getSelectedValue();

			if ( selected == null )
				return;

			ItemCreationRequest selection = (ItemCreationRequest) selected;
			int quantityDesired = getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityPossible() );
			if ( quantityDesired < 1 )
				return;

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			selection.setQuantityNeeded( quantityDesired );

			RequestThread.openRequestSequence();

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			Object selected = this.elementList.getSelectedValue();

			if ( selected == null )
				return;

			ItemCreationRequest selection = (ItemCreationRequest) selected;

			int maximum = ConsumeItemRequest.maximumUses( selection.getItemId() );
			int quantityDesired = maximum < 2 ? maximum : getQuantity( "Creating multiple " + selection.getName() + "...",
				Math.min( maximum, selection.getQuantityPossible() ) );

			if ( quantityDesired < 1 )
				return;

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			selection.setQuantityNeeded( quantityDesired );

			RequestThread.openRequestSequence();

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( selection.getItemId(), quantityDesired ) ) );
			RequestThread.closeRequestSequence();
		}
	}

	private static class OptionRenderer extends DefaultListCellRenderer
	{
		public OptionRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			return value instanceof JComponent ? (Component) value :
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
		}
	}

	public class InventoryManagePanel extends ItemManagePanel
	{
		protected boolean isEquipmentOnly;
		private FilterRadioButton [] equipmentFilters;

		public InventoryManagePanel( LockableListModel elementModel, boolean isEquipmentOnly )
		{
			super( elementModel );
			this.isEquipmentOnly = isEquipmentOnly;

			boolean isCloset = (elementModel == closet);

			this.setButtons( true, new ActionListener [] {

				new ConsumeListener(),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOSELL ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOMALL ),
				new PulverizeListener( isCloset ),
				new PutInClosetListener( isCloset ),
				new PutOnDisplayListener( isCloset ),
				new GiveToClanListener( isCloset ),

			} );

			elementList.setCellRenderer( AdventureResult.getDefaultRenderer( this.isEquipmentOnly ) );

			if ( this.movers != null )
				this.movers[2].setSelected( true );
		}

		public InventoryManagePanel( String confirmText, String cancelText, LockableListModel model, boolean isEquipmentOnly )
		{
			super( confirmText, cancelText, model );
			this.isEquipmentOnly = isEquipmentOnly;

			this.addFilters();
			elementList.setCellRenderer( AdventureResult.getDefaultRenderer( this.isEquipmentOnly ) );
		}

		public void addFilters()
		{
			if ( !this.isEquipmentOnly )
			{
				super.addFilters();
				return;
			}

			this.equipmentFilters = new FilterRadioButton[7];
			this.equipmentFilters[0] = new FilterRadioButton( "weapons", true );
			this.equipmentFilters[1] = new FilterRadioButton( "offhand" );
			this.equipmentFilters[2] = new FilterRadioButton( "hats" );
			this.equipmentFilters[3] = new FilterRadioButton( "shirts" );
			this.equipmentFilters[4] = new FilterRadioButton( "pants" );
			this.equipmentFilters[5] = new FilterRadioButton( "accessories" );
			this.equipmentFilters[6] = new FilterRadioButton( "familiar" );

			ButtonGroup filterGroup = new ButtonGroup();
			JPanel filterPanel = new JPanel();

			for ( int i = 0; i < 7; ++i )
			{
				filterGroup.add( this.equipmentFilters[i] );
				filterPanel.add( this.equipmentFilters[i] );
			}

			this.northPanel.add( filterPanel, BorderLayout.NORTH );
			this.filterItems();
		}

		public FilterTextField getWordFilter()
		{	return new EquipmentFilterField();
		}

		private class FilterRadioButton extends JRadioButton
		{
			public FilterRadioButton( String label )
			{	this( label, false );
			}

			public FilterRadioButton( String label, boolean isSelected )
			{
				super( label, isSelected );
				InventoryManagePanel.this.listenToRadioButton( this );
			}
		}

		private class EquipmentFilterField extends FilterItemField
		{
			public boolean isVisible( Object element )
			{
				if ( InventoryManagePanel.this.equipmentFilters == null )
					return super.isVisible( element );

				if ( element instanceof AdventureResult && !((AdventureResult)element).isItem() )
					return false;

				boolean isVisibleWithFilter = true;

				if ( element == null )
					return false;

				int itemId = element instanceof AdventureResult ? ((AdventureResult)element).getItemId() :
					element instanceof ItemCreationRequest ? ((ItemCreationRequest)element).getItemId() : -1;

				if ( itemId == -1 )
					return false;

				switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
				{
				case EQUIP_WEAPON:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[0].isSelected();
					break;

				case EQUIP_OFFHAND:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[1].isSelected();
					break;

				case EQUIP_HAT:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[2].isSelected();
					break;

				case EQUIP_SHIRT:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[3].isSelected();
					break;

				case EQUIP_PANTS:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[4].isSelected();
					break;

				case EQUIP_ACCESSORY:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[5].isSelected();
					break;

				case EQUIP_FAMILIAR:
					isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[6].isSelected();
					break;

				default:
					return false;
				}

				return isVisibleWithFilter && super.isVisible( element );
			}
		}
	}

	private class HagnkStoragePanel extends InventoryManagePanel
	{
		private boolean isPullingForUse = false;

		public HagnkStoragePanel( boolean isEquipmentOnly )
		{
			super( "pull item", isEquipmentOnly ? "pull & use" : "closet item", storage, isEquipmentOnly );

			this.addFilters();
			this.addMovers();

			if ( isEquipmentOnly )
				this.eastPanel.add( pullsRemainingLabel1, BorderLayout.SOUTH );
			else
				this.eastPanel.add( pullsRemainingLabel2, BorderLayout.SOUTH );
		}

		public void addMovers()
		{
			if ( !this.isEquipmentOnly )
				super.addMovers();
		}

		protected int getDesiredItemAmount( Object item, String itemName, int itemCount, String message, int quantityType )
		{
			if ( !isPullingForUse && !this.isEquipmentOnly || quantityType != TAKE_MULTIPLE )
				return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );

			int consumptionType = TradeableItemDatabase.getConsumptionType( ((AdventureResult)item).getItemId() );
			switch ( consumptionType )
			{
				case EQUIP_HAT:
				case EQUIP_PANTS:
				case EQUIP_SHIRT:
				case EQUIP_WEAPON:
				case EQUIP_OFFHAND:
					return 1;

				default:
					return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );
			}
		}

		private Object [] pullItems()
		{
			this.isPullingForUse = true;
			Object [] items = this.getDesiredItems( "Pulling" );
			this.isPullingForUse = false;

			if ( items == null )
				return null;

			RequestThread.openRequestSequence();

			if ( items.length == storage.size() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
			else
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );

			RequestThread.closeRequestSequence();
			return items;
		}

		public void actionConfirmed()
		{	pullItems();
		}

		public void actionCancelled()
		{
			Object [] items = pullItems();
			if ( items == null )
				return;

			RequestThread.closeRequestSequence();

			if ( isEquipmentOnly )
			{
				for ( int i = 0; i < items.length; ++i )
					RequestThread.postRequest( new EquipmentRequest( (AdventureResult) items[i] ) );
			}
			else
			{
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
			}

			RequestThread.closeRequestSequence();
		}
	}

	private class RestockPanel extends OverlapPanel
	{
		public RestockPanel()
		{
			super( "automall", "host sale", profitableList, true );

			this.filters[4].setSelected( false );
			this.filters[4].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{
			if ( !confirm( "ALL OF THE ITEMS IN THIS LIST, not just the ones you've selected, will be placed into your store.  Are you sure you wish to continue?" ) )
				return;

			StaticEntity.getClient().makeAutoMallRequest();
		}

		public void actionCancelled()
		{
			int selected = JOptionPane.showConfirmDialog( ItemManageFrame.this,
				basicTextWrap( "KoLmafia will place all tradeable, autosellable items into your store at 999,999,999 meat. " + StoreManageFrame.UNDERCUT_MESSAGE ), "", JOptionPane.YES_NO_CANCEL_OPTION );

			if ( selected != JOptionPane.YES_OPTION && selected != JOptionPane.NO_OPTION )
				return;

			KoLmafia.updateDisplay( "Gathering data..." );
			StaticEntity.getClient().makeEndOfRunSaleRequest( selected == JOptionPane.YES_OPTION );
		}
	}
}
