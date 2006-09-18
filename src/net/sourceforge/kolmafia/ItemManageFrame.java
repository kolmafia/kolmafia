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
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;

// containers
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

// other imports
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the item
 * management functionality of Kingdom of Loathing.  This ranges from
 * basic transfer to and from the closet to item creation, cooking,
 * item use, and equipment.
 */

public class ItemManageFrame extends KoLFrame
{
	private MultiButtonPanel bruteForcer, inventoryManager, closetManager, itemConsumer, itemCreator, npcOfferings;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public ItemManageFrame()
	{
		super( "Item Manager" );

		tabs = new JTabbedPane();
		itemConsumer = new ConsumePanel();
		bruteForcer = new InventPanel();
		itemCreator = new CreateItemPanel();
		inventoryManager = new OutsideClosetPanel();
		closetManager = new InsideClosetPanel();
		npcOfferings = null;

		tabs.addTab( "Consume", itemConsumer );

		if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
		{
			// If the person is in a mysticality sign, make sure
			// you retrieve information from the restaurant.

			if ( KoLCharacter.inMysticalitySign() && !restaurantItems.isEmpty() )
			{
				npcOfferings = new SpecialPanel( restaurantItems );
				tabs.add( "Restaurant", npcOfferings );
			}

			// If the person is in a moxie sign and they have completed
			// the beach quest, then retrieve information from the
			// microbrewery.

			if ( KoLCharacter.inMoxieSign() && !microbreweryItems.isEmpty() )
			{
				npcOfferings = new SpecialPanel( microbreweryItems );
				tabs.add( "Microbrewery", npcOfferings );
			}
		}

//		tabs.addTab( "Find Recipe", bruteForcer );
		tabs.addTab( "Create", itemCreator );
		tabs.addTab( "Inventory", inventoryManager );
		tabs.addTab( "Closet", closetManager );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class ConsumePanel extends MultiButtonPanel
	{
		private JCheckBox [] filters;

		public ConsumePanel()
		{
			super( "Usable Items", usables, false );

			setButtons( new String [] { "use one", "use multiple", "refresh" },
				new ActionListener [] { new ConsumeListener( false ), new ConsumeListener( true ),
				new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );

			filters = new JCheckBox[3];
			filters[0] = new FilterCheckBox( filters, elementList, "Show food", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, "Show drink", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, "Show others", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setCellRenderer(
				AdventureResult.getConsumableCellRenderer( KoLCharacter.canEat(), KoLCharacter.canDrink(), true ) );
		}

		protected AdventureResult [] getDesiredItems( String message )
		{
			filterSelection( filters[0].isSelected(),
				 filters[1].isSelected(), filters[2].isSelected(), true, true );
			return super.getDesiredItems( message );
		}

		private class ConsumeListener implements ActionListener
		{
			private boolean useMultiple;

			public ConsumeListener( boolean useMultiple )
			{	this.useMultiple = useMultiple;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Consume" );
				if ( items.length == 0 )
					return;

				int consumptionType, consumptionCount;
				AdventureResult currentItem;

				Runnable [] requests = new Runnable[ items.length ];

				for ( int i = 0; i < items.length; ++i )
				{
					currentItem = (AdventureResult) items[i];

					consumptionType = TradeableItemDatabase.getConsumptionType( currentItem.getName() );
					consumptionCount = useMultiple ? getQuantity( "Using multiple " + currentItem.getName() + "...", currentItem.getCount() ) : 1;

					if ( consumptionCount == 0 )
						return;

					requests[i] = new ConsumeItemRequest( StaticEntity.getClient(), currentItem.getInstance( consumptionCount ) );
				}

				(new RequestThread( requests )).start();
			}
		}
	}

	private class SpecialPanel extends MultiButtonPanel
	{
		private final int PURCHASE_ONE = 1;
		private final int PURCHASE_MULTIPLE = 2;

		public SpecialPanel( LockableListModel items )
		{
			super( "Sign-Specific Stuffs", items, false );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setButtons( new String [] { "buy one", "buy multiple" },
				new ActionListener [] {
					new BuyListener( PURCHASE_ONE ),
					new BuyListener( PURCHASE_MULTIPLE )
				} );
		}

		private class BuyListener implements ActionListener
		{
			private int purchaseType;

			public BuyListener( int purchaseType )
			{	this.purchaseType = purchaseType;
			}

			public void actionPerformed( ActionEvent e )
			{
				String item = (String) elementList.getSelectedValue();
				if ( item == null )
					return;

				int consumptionCount = purchaseType == PURCHASE_MULTIPLE ? getQuantity( "Buying multiple " + item + "...", Integer.MAX_VALUE, 1 ) : 1;
				if ( consumptionCount == 0 )
					return;

				Runnable request = elementList.getModel() == restaurantItems ?
					(KoLRequest) (new RestaurantRequest( StaticEntity.getClient(), item )) : (KoLRequest) (new MicrobreweryRequest( StaticEntity.getClient(), item ));

				(new RequestThread( request, consumptionCount )).start();
			}
		}
	}

	private class ClosetManagePanel extends MultiButtonPanel
	{
		private JCheckBox [] filters;

		public ClosetManagePanel( String title, LockableListModel elementModel )
		{
			super( title, elementModel, true );

			filters = new JCheckBox[5];
			filters[0] = new FilterCheckBox( filters, elementList, true, "Show food", true );
			filters[1] = new FilterCheckBox( filters, elementList, true, "Show drink", true );
			filters[2] = new FilterCheckBox( filters, elementList, true, "Show others", true );
			filters[3] = new FilterCheckBox( filters, elementList, true, "Show no-sell", true );
			filters[4] = new FilterCheckBox( filters, elementList, true, "Show no-trade", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setCellRenderer(
				AdventureResult.getAutoSellCellRenderer( true, true, true, true, true ) );
		}

		protected AdventureResult [] getDesiredItems( String message )
		{
			filterSelection( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() );
			return super.getDesiredItems( message );
		}

		protected abstract class TransferListener implements ActionListener
		{
			protected String description;
			protected boolean retrieveFromClosetFirst;

			protected Runnable [] requests;
			protected ShowDescriptionList elementList;

			public TransferListener( String description, boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{
				this.description = description;
				this.retrieveFromClosetFirst = retrieveFromClosetFirst;
				this.elementList = elementList;
			}

			public AdventureResult [] initialSetup()
			{
				AdventureResult [] items = getDesiredItems( description );
				if (items == null )
					return null;
				this.requests = new Runnable[ !retrieveFromClosetFirst || description.equals( "Bagging" ) ? 1 : 2 ];

				if ( retrieveFromClosetFirst )
					requests[0] = new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.CLOSET_TO_INVENTORY, items );

				return items;
			}

			public void initializeTransfer()
			{	(new RequestThread( requests )).start();
			}
		}

		protected class PutInClosetListener extends TransferListener
		{
			public PutInClosetListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{	super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst, elementList );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				if ( !retrieveFromClosetFirst )
					requests[0] = new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				initializeTransfer();
			}
		}

		protected class AutoSellListener extends TransferListener
		{
			private int sellType;

			public AutoSellListener( boolean retrieveFromClosetFirst, int sellType, ShowDescriptionList elementList )
			{
				super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst, elementList );
				this.sellType = sellType;
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( sellType == AutoSellRequest.AUTOMALL && !KoLCharacter.hasStore() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
					return;
				}

				if ( sellType == AutoSellRequest.AUTOSELL && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
					"Are you sure you would like to sell the selected items?",
						"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
							return;

				if ( sellType == AutoSellRequest.AUTOMALL && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
					"Are you sure you would like to place the selected items in your store?",
						"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
							return;

				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				requests[ requests.length - 1 ] = new AutoSellRequest( StaticEntity.getClient(), items, sellType );
				initializeTransfer();
			}
		}

		protected class GiveToClanListener extends TransferListener
		{
			public GiveToClanListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{	super( "Stashing", retrieveFromClosetFirst, elementList );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				requests[ requests.length - 1 ] = new ClanStashRequest( StaticEntity.getClient(), items, ClanStashRequest.ITEMS_TO_STASH );
				initializeTransfer();
			}
		}

		protected class PutOnDisplayListener extends TransferListener
		{
			public PutOnDisplayListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{	super( "Showcasing", retrieveFromClosetFirst, elementList );
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = initialSetup();
				if ( items == null )
					return;

				if ( !KoLCharacter.hasDisplayCase() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You don't own a display case in the Cannon Museum.");
					return;
				}

				requests[ requests.length - 1 ] = new MuseumRequest( StaticEntity.getClient(), items, true );
				initializeTransfer();
			}
		}

		protected class PulverizeListener extends TransferListener
		{
			public PulverizeListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{	super( "Smashing", retrieveFromClosetFirst, elementList );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null || items.length == 0 )
					return;

				requests = new Runnable[ items.length ];
				for ( int i = 0; i < items.length; ++i )
				{
					boolean willSmash = TradeableItemDatabase.isTradeable( items[i].getItemID() ) ||
						JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
							items[i].getName() + " is untradeable.  Are you sure?", "Smash request nag screen!", JOptionPane.YES_NO_OPTION );

					requests[i] = willSmash ? new PulverizeRequest( StaticEntity.getClient(), items[i] ) : null;
				}

				initializeTransfer();
			}
		}
	}

	private class OutsideClosetPanel extends ClosetManagePanel
	{
		public OutsideClosetPanel()
		{
			super( "Inside Inventory", inventory );
			setButtons( new String [] { "closet", "sell", "mall", "pulverize", "museum", "clan", "refresh" },
				new ActionListener [] {
					new PutInClosetListener( false, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOMALL, elementList ),
					new PulverizeListener( false, elementList ),
					new PutOnDisplayListener( false, elementList ),
					new GiveToClanListener( false, elementList ),
					new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );
		}
	}

	private class InsideClosetPanel extends ClosetManagePanel
	{
		public InsideClosetPanel()
		{
			super( "Inside Closet", closet );
			setButtons( new String [] { "backpack", "sell", "mall", "pulverize", "museum", "clan", "refresh" },
				new ActionListener [] {
					new PutInClosetListener( true, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOMALL, elementList ),
					new PulverizeListener( true, elementList ),
					new PutOnDisplayListener( true, elementList ),
					new GiveToClanListener( true, elementList ),
					new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );
		}
	}

	private class InventPanel extends MultiButtonPanel
	{
		public InventPanel()
		{
			super( "Invent an Item", inventory, false );
			elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer( true, true, true ) );

			setButtons( new String [] { "combine", "cook", "mix", "smith", "pliers", "tinker" },
				new ActionListener [] { new SearchListener( "combine.php" ), new SearchListener( "cook.php" ),
					new SearchListener( "cocktail.php" ), new SearchListener( "smith.php" ), new SearchListener( "jewelry.php" ),
					new SearchListener( "gnomes.php" ) } );
		}

		private final int NORMAL = 1;
		private final int GNOMES = 2;

		private class SearchListener implements ActionListener, Runnable
		{
			private int searchType;
			private KoLRequest request;

			public SearchListener( String location )
			{
				request = new KoLRequest( StaticEntity.getClient(), location, true );
				request.addFormField( "pwd" );

				if ( location.equals( "gnomes.php" ) )
				{
					searchType = GNOMES;
					request.addFormField( "place", "tinker" );
					request.addFormField( "action", "tinksomething" );
					request.addFormField( "qty", "1" );
				}
				else
				{
					searchType = NORMAL;
					request.addFormField( "action", "combine" );
					request.addFormField( "quantity", "1" );
				}
			}

			public void actionPerformed( ActionEvent e )
			{	(new RequestThread( this )).start();
			}

			public void run()
			{
				switch ( searchType )
				{
					case NORMAL:
						combineTwoItems();
						break;

					case GNOMES:
						combineThreeItems();
						break;
				}
			}

			private void combineTwoItems()
			{
				AdventureDatabase.retrieveItem( new AdventureResult( ItemCreationRequest.MEAT_PASTE, 1 ) );
				request.run();

				// In order to ensure that you do not test items which
				// are not available in the drop downs, go to the page
				// first and find out which ones are available.

				List availableItems = new ArrayList();
				Matcher selectMatcher = Pattern.compile( "<select.*?</select>" ).matcher( request.responseText );
				if ( !selectMatcher.find() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "Method not currently available." );
					return;
				}

				int itemID = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemID = StaticEntity.parseInt( optionMatcher.group(1) );
					if ( itemID >= 1 )  availableItems.add( new AdventureResult( itemID, 1 ) );
				}

				// Determine which items are available at the "core"
				// of the tests -- in other words, items which are
				// actually being tested against all other items.

				List coreItems = new ArrayList();
				coreItems.addAll( availableItems );

				Object [] selection = elementList.getSelectedValues();
				List selectedItems = new ArrayList();
				for ( int i = 0; i < selection.length; ++i )
					selectedItems.add( selection[i] );
				coreItems.retainAll( selectedItems );

				// Convert everything into arrays so that you can
				// iterate through them without problems.

				AdventureResult [] coreArray = new AdventureResult[ coreItems.size() ];
				coreItems.toArray( coreArray );

				AdventureResult [] availableArray = new AdventureResult[ availableItems.size() ];
				availableItems.toArray( availableArray );

				// Begin testing every single possible combination.

				AdventureResult [] currentTest = new AdventureResult[2];
				for ( int i = 0; i < coreArray.length && KoLmafia.permitsContinue(); ++i )
				{
					for ( int j = 0; j < availableArray.length && KoLmafia.permitsContinue(); ++j )
					{
						currentTest[0] = coreArray[i];
						currentTest[1] = availableArray[j];

						if ( !ConcoctionsDatabase.isKnownCombination( currentTest ) )
						{
							KoLmafia.updateDisplay( "Testing combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() );
							request.addFormField( "item1", String.valueOf( currentTest[0].getItemID() ) );
							request.addFormField( "item2", String.valueOf( currentTest[1].getItemID() ) );

							request.run();

							if ( request.responseText.indexOf( "You acquire" ) != -1 )
							{
								KoLmafia.updateDisplay( "Found new item combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() );
								return;
							}
						}
					}
				}

				KoLmafia.updateDisplay( ERROR_STATE, "No new item combinations were found." );
				return;
			}

			private void combineThreeItems()
			{
				request.run();

				// In order to ensure that you do not test items which
				// are not available in the drop downs, go to the page
				// first and find out which ones are available.

				List availableItems = new ArrayList();
				Matcher selectMatcher = Pattern.compile( "<select.*?</select>" ).matcher( request.responseText );
				if ( !selectMatcher.find() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "Method not currently available." );
					return;
				}

				int itemID = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemID = StaticEntity.parseInt( optionMatcher.group(1) );
					if ( itemID >= 1 )  availableItems.add( new AdventureResult( itemID, 1 ) );
				}

				// Determine which items are available at the "core"
				// of the tests -- in other words, items which are
				// actually being tested against all other items.

				List coreItems = new ArrayList();
				coreItems.addAll( availableItems );

				Object [] selection = elementList.getSelectedValues();
				List selectedItems = new ArrayList();
				for ( int i = 0; i < selection.length; ++i )
					selectedItems.add( selection[i] );
				coreItems.retainAll( selectedItems );

				// Convert everything into arrays so that you can
				// iterate through them without problems.

				AdventureResult [] coreArray = new AdventureResult[ coreItems.size() ];
				coreItems.toArray( coreArray );

				AdventureResult [] availableArray = new AdventureResult[ availableItems.size() ];
				availableItems.toArray( availableArray );

				// Begin testing every single possible combination.

				AdventureResult [] currentTest = new AdventureResult[3];
				for ( int i = 0; i < coreArray.length && KoLmafia.permitsContinue(); ++i )
				{
					for ( int j = 0; j < availableArray.length && KoLmafia.permitsContinue(); ++j )
					{
						for ( int k = j; k < availableArray.length && KoLmafia.permitsContinue(); ++k )
						{
							currentTest[0] = coreArray[i];
							currentTest[1] = availableArray[j];
							currentTest[2] = availableArray[k];

							if ( !ConcoctionsDatabase.isKnownCombination( currentTest ) )
							{
								KoLmafia.updateDisplay( "Testing combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
								request.addFormField( "item1", String.valueOf( currentTest[0].getItemID() ) );
								request.addFormField( "item2", String.valueOf( currentTest[1].getItemID() ) );
								request.addFormField( "item3", String.valueOf( currentTest[1].getItemID() ) );

								request.run();

								if ( request.responseText.indexOf( "You acquire" ) != -1 )
								{
									KoLmafia.updateDisplay( "Found new item combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
									return;
								}
							}
						}
					}
				}

				KoLmafia.updateDisplay( ERROR_STATE, "No new item combinations were found." );
				return;
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends MultiButtonPanel
	{
		public CreateItemPanel()
		{
			super( "Create an Item", ConcoctionsDatabase.getConcoctions(), false );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setButtons( new String [] { "create one", "create multiple", "refresh" },
				new ActionListener [] { new CreateListener( false ), new CreateListener( true ),
				new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );

			JCheckBox [] filters = new JCheckBox[6];

			filters[0] = new FilterCheckBox( filters, elementList, "Show cookables", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, "Show mixables", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, "Show others", true );

			filters[3] = new CreateSettingCheckbox( "Allow no-box", "createWithoutBoxServants", "Create without requiring a box servant" );
			filters[4] = new CreateSettingCheckbox( "Allow closet", "showClosetIngredients", "List items creatable when adding the closet" );
			filters[5] = new CreateSettingCheckbox( "Allow stash", "showStashIngredients", "List items creatable when adding the clan stash" );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setCellRenderer(
				AdventureResult.getConsumableCellRenderer( KoLCharacter.canEat(), KoLCharacter.canDrink(), true ) );
		}

		private class CreateSettingCheckbox extends JCheckBox implements ActionListener
		{
			private String setting;

			public CreateSettingCheckbox( String title, String setting, String tooltip )
			{
				super( title, StaticEntity.getBooleanProperty( setting ) );

				this.setting = setting;
				setToolTipText( tooltip );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				StaticEntity.setProperty( setting, String.valueOf( isSelected() ) );

				if ( setting.equals( "showStashIngredients" ) && KoLCharacter.hasClan() && isSelected() &&
					StaticEntity.getClient().shouldMakeConflictingRequest() && !ClanManager.isStashRetrieved() )
				{
					(new RequestThread( new ClanStashRequest( StaticEntity.getClient() ) )).start();
				}

				ConcoctionsDatabase.refreshConcoctions();
			}
		}

		private class CreateListener implements ActionListener
		{
			private boolean createMultiple;

			public CreateListener( boolean createMultiple )
			{	this.createMultiple = createMultiple;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object selected = elementList.getSelectedValue();

				if ( selected == null )
					return;

				ItemCreationRequest selection = (ItemCreationRequest) selected;
				int quantityDesired = createMultiple ? getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityNeeded() ) : 1;
				if ( quantityDesired < 1 )
					return;

				KoLmafia.updateDisplay( "Verifying ingredients..." );
				selection.setQuantityNeeded( quantityDesired );
				(new RequestThread( selection )).start();
			}
		}
	}
}
