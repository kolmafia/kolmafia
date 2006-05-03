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
import java.awt.CardLayout;
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
	private MultiButtonPanel bruteforcer, inventory, closet, consume, create, special;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	StaticEntity.getClient()	The StaticEntity.getClient() to be notified in the event of error.
	 */

	public ItemManageFrame()
	{
		super( "Item Manager" );

		tabs = new JTabbedPane();
		consume = new ConsumePanel();
		bruteforcer = new InventPanel();
		create = new CreateItemPanel();
		inventory = new OutsideClosetPanel();
		closet = new InsideClosetPanel();
		special = null;

		tabs.addTab( "Consume", consume );
		
		if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
		{
			// If the person is in a mysticality sign, make sure
			// you retrieve information from the restaurant.
	
			if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
			{
				special = new SpecialPanel( StaticEntity.getClient().getRestaurantItems() );
				tabs.add( "Restaurant", special );
	
				if ( StaticEntity.getClient().getRestaurantItems().isEmpty() )
					(new RequestThread( new RestaurantRequest( StaticEntity.getClient() ) )).start();
			}
	
			// If the person is in a moxie sign and they have completed
			// the beach quest, then retrieve information from the
			// microbrewery.
	
			if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() && KoLCharacter.getInventory().contains( ConcoctionsDatabase.CAR ) )
			{
				special = new SpecialPanel( StaticEntity.getClient().getMicrobreweryItems() );
				tabs.add( "Microbrewery", special );
	
				if ( StaticEntity.getClient().getMicrobreweryItems().isEmpty() )
					(new RequestThread( new MicrobreweryRequest( StaticEntity.getClient() ) )).start();
			}
		}

//		tabs.addTab( "Find Recipe", bruteforcer );
		tabs.addTab( "Create", create );
		tabs.addTab( "Inventory", inventory );
		tabs.addTab( "Closet", closet );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class ConsumePanel extends MultiButtonPanel
	{
		public ConsumePanel()
		{
			super( "Usable Items", KoLCharacter.getUsables(), false );

			setButtons( new String [] { "use one", "use multiple", "refresh" },
				new ActionListener [] { new ConsumeListener( false ), new ConsumeListener( true ),
				new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );

			JCheckBox [] filters = new JCheckBox[3];
			filters[0] = new FilterCheckBox( filters, elementList, KoLCharacter.getUsables(), "Show food", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, KoLCharacter.getUsables(), "Show drink", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, KoLCharacter.getUsables(), "Show others", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setModel( AdventureResult.getFilteredItemList(
				KoLCharacter.getUsables(), filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
		}

		private class ConsumeListener implements ActionListener
		{
			private boolean useMultiple;

			public ConsumeListener( boolean useMultiple )
			{	this.useMultiple = useMultiple;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = elementList.getSelectedValues();
				if ( items.length == 0 )
					return;

				int consumptionType, consumptionCount;
				AdventureResult currentItem;

				Runnable [] requests = new Runnable[ items.length ];
				int [] repeatCount = new int[ items.length ];

				for ( int i = 0; i < items.length; ++i )
				{
					currentItem = (AdventureResult) items[i];

					consumptionType = TradeableItemDatabase.getConsumptionType( currentItem.getName() );
					consumptionCount = useMultiple ? getQuantity( "Using multiple " + currentItem.getName() + "...", currentItem.getCount() ) : 1;

					if ( consumptionCount == 0 )
						return;

					if ( consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE || consumptionType == ConsumeItemRequest.CONSUME_RESTORE )
					{
						requests[i] = new ConsumeItemRequest( StaticEntity.getClient(), currentItem.getInstance( consumptionCount ) );
						repeatCount[i] = 1;
					}
					else
					{
						requests[i] = new ConsumeItemRequest( StaticEntity.getClient(), currentItem.getInstance( 1 ) );
						repeatCount[i] = consumptionCount;
					}
				}

				(new RequestThread( requests, repeatCount )).start();
			}
		}
	}

	private class SpecialPanel extends MultiButtonPanel
	{
		private final int PURCHASE_ONE = 1;
		private final int PURCHASE_MULTIPLE = 2;
		private final int PURCHASE_MAX = 3;

		public SpecialPanel( LockableListModel items )
		{
			super( "Sign-Specific Stuffs", items, false );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setButtons( new String [] { "buy one", "buy multiple", "buy maximum" },
				new ActionListener [] {
					new BuyListener( PURCHASE_ONE ),
					new BuyListener( PURCHASE_MULTIPLE ),
					new BuyListener( PURCHASE_MAX )
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

				int consumptionCount = purchaseType == PURCHASE_MULTIPLE ? getQuantity( "Buying multiple " + item + "...", Integer.MAX_VALUE, 1 ) :
					purchaseType == PURCHASE_ONE ? 1 : 30;

				if ( consumptionCount == 0 )
					return;

				Runnable request = elementList.getModel() == StaticEntity.getClient().getRestaurantItems() ?
					(KoLRequest) (new RestaurantRequest( StaticEntity.getClient(), item )) : (KoLRequest) (new MicrobreweryRequest( StaticEntity.getClient(), item ));

				(new RequestThread( request, consumptionCount )).start();
			}
		}
	}

	private class ClosetManagePanel extends MultiButtonPanel
	{
		private JCheckBox [] filters;

		public ClosetManagePanel( String title, SortedListModel elementModel )
		{
			super( title, elementModel, true );
			this.elementModel = elementModel;

			filters = new JCheckBox[5];
			filters[0] = new FilterCheckBox( filters, elementList, elementModel, true, "Show food", true );
			filters[1] = new FilterCheckBox( filters, elementList, elementModel, true, "Show drink", true );
			filters[2] = new FilterCheckBox( filters, elementList, elementModel, true, "Show others", true );
			filters[3] = new FilterCheckBox( filters, elementList, elementModel, true, "Show no-sell", true );
			filters[4] = new FilterCheckBox( filters, elementList, elementModel, true, "Show no-trade", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setModel( AdventureResult.getFilteredItemList(
				elementModel, filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() ) );
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

			public Object [] initialSetup()
			{
				Object [] items = getDesiredItems( description );
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
				Object [] items = initialSetup();
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
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
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

				Object [] items = initialSetup();
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
				Object [] items = initialSetup();
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
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You don't own a display case in the Cannon Museum.");
					return;
				}

				requests[ requests.length - 1 ] = new MuseumRequest( StaticEntity.getClient(), items, true );
				initializeTransfer();
			}
		}
	}

	private class OutsideClosetPanel extends ClosetManagePanel
	{
		public OutsideClosetPanel()
		{
			super( "Inside Inventory", KoLCharacter.getInventory() );
			setButtons( new String [] { "closet", "sell", "mall", "museum", "clan", "refresh" },
				new ActionListener [] {
					new PutInClosetListener( false, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOMALL, elementList ),
					new PutOnDisplayListener( false, elementList ),
					new GiveToClanListener( false, elementList ),
					new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );
		}
	}

	private class InsideClosetPanel extends ClosetManagePanel
	{
		public InsideClosetPanel()
		{
			super( "Inside Closet", KoLCharacter.getCloset() );
			setButtons( new String [] { "backpack", "sell", "mall", "museum", "clan", "refresh" },
				new ActionListener [] {
					new PutInClosetListener( true, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOMALL, elementList ),
					new PutOnDisplayListener( true, elementList ),
					new GiveToClanListener( true, elementList ),
					new RequestButton( "Refresh Items", new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET ) ) } );
		}
	}

	private class InventPanel extends MultiButtonPanel
	{
		public InventPanel()
		{
			super( "Invent an Item", KoLCharacter.getInventory(), false );

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
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Method not currently available." );
					return;
				}

				int itemID = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemID = Integer.parseInt( optionMatcher.group(1) );
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
				for ( int i = 0; i < coreArray.length && StaticEntity.getClient().permitsContinue(); ++i )
				{
					for ( int j = 0; j < availableArray.length && StaticEntity.getClient().permitsContinue(); ++j )
					{
						currentTest[0] = coreArray[i];
						currentTest[1] = availableArray[j];

						if ( !ConcoctionsDatabase.isKnownCombination( currentTest ) )
						{
							DEFAULT_SHELL.updateDisplay( "Testing combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() );
							request.addFormField( "item1", String.valueOf( currentTest[0].getItemID() ) );
							request.addFormField( "item2", String.valueOf( currentTest[1].getItemID() ) );

							request.run();

							if ( request.responseText.indexOf( "You acquire" ) != -1 )
							{
								DEFAULT_SHELL.updateDisplay( "Found new item combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() );
								return;
							}
						}
					}
				}

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "No new item combinations were found." );
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
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Method not currently available." );
					return;
				}

				int itemID = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemID = Integer.parseInt( optionMatcher.group(1) );
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
				for ( int i = 0; i < coreArray.length && StaticEntity.getClient().permitsContinue(); ++i )
				{
					for ( int j = 0; j < availableArray.length && StaticEntity.getClient().permitsContinue(); ++j )
					{
						for ( int k = j; k < availableArray.length && StaticEntity.getClient().permitsContinue(); ++k )
						{
							currentTest[0] = coreArray[i];
							currentTest[1] = availableArray[j];
							currentTest[2] = availableArray[k];

							if ( !ConcoctionsDatabase.isKnownCombination( currentTest ) )
							{
								DEFAULT_SHELL.updateDisplay( "Testing combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
								request.addFormField( "item1", String.valueOf( currentTest[0].getItemID() ) );
								request.addFormField( "item2", String.valueOf( currentTest[1].getItemID() ) );
								request.addFormField( "item3", String.valueOf( currentTest[1].getItemID() ) );

								request.run();

								if ( request.responseText.indexOf( "You acquire" ) != -1 )
								{
									DEFAULT_SHELL.updateDisplay( "Found new item combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
									return;
								}
							}
						}
					}
				}

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "No new item combinations were found." );
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

			filters[0] = new FilterCheckBox( filters, elementList, ConcoctionsDatabase.getConcoctions(), "Show food", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, ConcoctionsDatabase.getConcoctions(), "Show drink", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, ConcoctionsDatabase.getConcoctions(), "Show others", true );

			filters[3] = new CreateSettingCheckbox( "Allow closet", "showClosetDrivenCreations",
				"Get ingredients from closet if needed" );
			filters[4] = new CreateSettingCheckbox( "Allow no-box", "createWithoutBoxServants",
				"Create without requiring a box servant" );
			filters[5] = new CreateSettingCheckbox( "Auto-repair", "autoRepairBoxes",
				"Create and install new box servant after explosion" );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setModel( AdventureResult.getFilteredCreationList(
				filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
		}

		private class CreateSettingCheckbox extends JCheckBox implements ActionListener
		{
			private String setting;

			public CreateSettingCheckbox( String title, String setting, String tooltip )
			{
				super( title, getProperty( setting ).equals( "true" ) );

				this.setting = setting;
				setToolTipText( tooltip );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				setProperty( setting, String.valueOf( isSelected() ) );
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
				
				DEFAULT_SHELL.updateDisplay( "Verifying ingredients..." );
				selection.setQuantityNeeded( quantityDesired );
				(new RequestThread( selection )).start();
			}
		}
	}
}
