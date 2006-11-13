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
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;

// containers
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;

// other imports
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of <code>KoLFrame</code> which handles all the item
 * management functionality of Kingdom of Loathing.  This ranges from
 * basic transfer to and from the closet to item creation, cooking,
 * item use, and equipment.
 */

public class ItemManageFrame extends KoLFrame
{
//	private ItemManagePanel bruteForcer;
	private ItemManagePanel insideBackpack, insideCloset, itemCreator, npcOfferings;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public ItemManageFrame()
	{
		super( "Item Manager" );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		insideBackpack = new ClosetManagePanel( inventory );
		insideCloset = new ClosetManagePanel( closet );
		itemCreator = new CreateItemPanel();
//		bruteForcer = new InventPanel();
		npcOfferings = null;

		tabs.addTab( "Inventory Items", insideBackpack );
		tabs.addTab( "Closeted Items", insideCloset );
		tabs.addTab( "Creatable Items", itemCreator );
//		tabs.addTab( "Recipe Finder", bruteForcer );

		// If the person is in a mysticality sign, make sure
		// you retrieve information from the restaurant.

		if ( KoLCharacter.inMysticalitySign() && !restaurantItems.isEmpty() )
		{
			npcOfferings = new SpecialPanel( restaurantItems );
			tabs.add( "Restaurant Menu", npcOfferings );
		}

		// If the person is in a moxie sign and they have completed
		// the beach quest, then retrieve information from the
		// microbrewery.

		if ( KoLCharacter.inMoxieSign() && !microbreweryItems.isEmpty() )
		{
			npcOfferings = new SpecialPanel( microbreweryItems );
			tabs.add( "Microrewery Menu", npcOfferings );
		}

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		addTab( "Scripted Actions", new CommonActionsPanel() );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class CommonActionsPanel extends JPanel
	{
		private JPanel container;
		private Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

		public CommonActionsPanel()
		{
			container = new JPanel();
			container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

			// End-user warning

			JLabel warnLabel = new JLabel(
				"<html>KoLmafia will not prompt you for confirmation when you click these buttons.  Read the descriptions before pressing.</html>" );

			warnLabel.setMaximumSize( MAX_WIDTH );
			warnLabel.setAlignmentX( LEFT_ALIGNMENT );
			container.add( warnLabel );

			addButtonAndLabel( new JunkItemsButton(),
				"This feature compares the list of items which you have flagged as \"junk\" against the items in your inventory, and if it finds any matches, autosells those junk items." );

			SimpleScrollPane scroller = new SimpleScrollPane( new ShowDescriptionList( junkItemList ) );
			scroller.setMaximumSize( MAX_WIDTH );
			scroller.setAlignmentX( LEFT_ALIGNMENT );
			container.add( scroller );

			addButtonAndLabel( new EndOfRunSaleButton(),
				"This feature takes all items which are currently in your inventory and either autosells them, if they're available in NPC stores, or dumps them into your store in the mall." );

			addButtonAndLabel( new MallRestockButton(),
				"This feature looks at all the items currently in your store, and if you have any matching items in your inventory, drops those items into your store at your current price." );

			addButtonAndLabel( new DisplayCaseButton(),
				"This feature scans your inventory and, if it finds any items which match what's in your display case, puts those items on display." );

			setLayout( new CardLayout( 10, 10 ) );
			add( container, "" );
		}

		private void addButtonAndLabel( ThreadedActionButton button, String label )
		{
			container.add( Box.createVerticalStrut( 15 ) );

			button.setAlignmentX( LEFT_ALIGNMENT );
			container.add( button );
			container.add( Box.createVerticalStrut( 5 ) );

			JLabel description = new JLabel( "<html>" + label + "</html>" );
			description.setMaximumSize( MAX_WIDTH );

			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			container.add( description );
			container.add( Box.createVerticalStrut( 5 ) );
		}

		private class DoNothingButton extends ThreadedActionButton
		{
			public DoNothingButton()
			{	super( "Warning!" );
			}

			public void executeTask()
			{
			}
		}

		private class JunkItemsButton extends ThreadedActionButton
		{
			public JunkItemsButton()
			{	super( "autosell junk items" );
			}

			public void executeTask()
			{
			}
		}

		private class EndOfRunSaleButton extends ThreadedActionButton
		{
			public EndOfRunSaleButton()
			{
				super( "end of run sale" );
				setEnabled( KoLCharacter.canInteract() );
			}

			public void executeTask()
			{	StaticEntity.getClient().makeEndOfRunSaleRequest();
			}
		}

		private class MallRestockButton extends ThreadedActionButton
		{
			public MallRestockButton()
			{
				super( "mall store restocker" );
				setEnabled( !KoLCharacter.isHardcore() );
			}

			public void executeTask()
			{
				(new StoreManageRequest()).run();

				StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
				StoreManager.getSoldItemList().toArray( sold );

				int itemCount;
				AdventureResult item;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < sold.length; ++i )
				{
					item = new AdventureResult( sold[i].getItemId(), 1 );
					itemCount = item.getCount( inventory );

					if ( itemCount > 0 )
						items.add( item.getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
					return;

				(new AutoSellRequest( items.toArray(), AutoSellRequest.AUTOMALL )).run();
			}
		}

		private class DisplayCaseButton extends ThreadedActionButton
		{
			public DisplayCaseButton()
			{
				super( "display case matcher" );
				setEnabled( !KoLCharacter.isHardcore() );
			}

			public void executeTask()
			{
				(new MuseumRequest()).run();

				AdventureResult [] display = new AdventureResult[ collection.size() ];
				collection.toArray( display );

				int itemCount;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < display.length; ++i )
				{
					itemCount = display[i].getCount( inventory );
					if ( itemCount > 0 )
						items.add( display[i].getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
					return;

				(new MuseumRequest( items.toArray(), true )).run();
			}
		}
	}

	private class SpecialPanel extends ItemManagePanel
	{
		private final int PURCHASE_ONE = 1;
		private final int PURCHASE_MULTIPLE = 2;

		public SpecialPanel( LockableListModel items )
		{
			super( items );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setButtons( new ActionListener [] { new BuyListener( PURCHASE_ONE ), new BuyListener( PURCHASE_MULTIPLE ) } );
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
					(KoLRequest) (new RestaurantRequest( item )) : (KoLRequest) (new MicrobreweryRequest( item ));

				(new RequestThread( request, consumptionCount )).start();
			}
		}
	}

	private class ClosetManagePanel extends ItemManagePanel
	{
		public ClosetManagePanel( LockableListModel elementModel )
		{
			super( elementModel );

			boolean isCloset = (elementModel == closet);

			setButtons( new ActionListener [] {

				new ConsumeListener(),
				new PutInClosetListener( isCloset ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOSELL ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOMALL ),
				new PulverizeListener( isCloset ),
				new PutOnDisplayListener( isCloset ),
				new GiveToClanListener( isCloset )

			} );

			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		private class ConsumeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Consume" );
				if ( items.length == 0 )
					return;

				Runnable [] requests = new Runnable[ items.length ];
				for ( int i = 0; i < items.length; ++i )
					requests[i] = new ConsumeItemRequest( (AdventureResult) items[i] );

				(new RequestThread( requests )).start();
			}

			public String toString()
			{	return "use item";
			}
		}

		protected class PutInClosetListener extends TransferListener
		{
			public PutInClosetListener( boolean retrieveFromClosetFirst )
			{	super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				if ( !retrieveFromClosetFirst )
					requests[0] = new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				initializeTransfer();
			}

			public String toString()
			{	return retrieveFromClosetFirst ? "inventory" : "closet";
			}
		}

		protected class AutoSellListener extends TransferListener
		{
			private int sellType;

			public AutoSellListener( boolean retrieveFromClosetFirst, int sellType )
			{
				super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst );
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

				requests[ requests.length - 1 ] = new AutoSellRequest( items, sellType );
				initializeTransfer();
			}

			public String toString()
			{	return sellType == AutoSellRequest.AUTOSELL ? "auto sell" : "place in mall";
			}
		}

		protected class GiveToClanListener extends TransferListener
		{
			public GiveToClanListener( boolean retrieveFromClosetFirst )
			{	super( "Stashing", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null )
					return;

				requests[ requests.length - 1 ] = new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH );
				initializeTransfer();
			}

			public String toString()
			{	return "clan stash";
			}
		}

		protected class PutOnDisplayListener extends TransferListener
		{
			public PutOnDisplayListener( boolean retrieveFromClosetFirst )
			{	super( "Showcasing", retrieveFromClosetFirst );
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

				requests[ requests.length - 1 ] = new MuseumRequest( items, true );
				initializeTransfer();
			}

			public String toString()
			{	return "display case";
			}
		}

		protected class PulverizeListener extends TransferListener
		{
			public PulverizeListener( boolean retrieveFromClosetFirst )
			{	super( "Smashing", retrieveFromClosetFirst );
			}

			public void actionPerformed( ActionEvent e )
			{
				AdventureResult [] items = initialSetup();
				if ( items == null || items.length == 0 )
					return;

				requests = new Runnable[ items.length ];
				for ( int i = 0; i < items.length; ++i )
				{
					boolean willSmash = TradeableItemDatabase.isTradeable( items[i].getItemId() ) ||
						JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
							items[i].getName() + " is untradeable.  Are you sure?", "Smash request nag screen!", JOptionPane.YES_NO_OPTION );

					requests[i] = willSmash ? new PulverizeRequest( items[i] ) : null;
				}

				initializeTransfer();
			}

			public String toString()
			{	return "pulverize";
			}
		}
	}

	private class InventPanel extends ItemManagePanel
	{
		public InventPanel()
		{
			super( inventory );

			setButtons( new ActionListener [] { new SearchListener( "combine.php" ), new SearchListener( "cook.php" ),
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
				request = new KoLRequest( location, true );
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

			public String toString()
			{	return request.formURLString;
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

				int itemId = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemId = StaticEntity.parseInt( optionMatcher.group(1) );
					if ( itemId >= 1 )  availableItems.add( new AdventureResult( itemId, 1 ) );
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
							request.addFormField( "item1", String.valueOf( currentTest[0].getItemId() ) );
							request.addFormField( "item2", String.valueOf( currentTest[1].getItemId() ) );

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

				int itemId = 0;
				Matcher optionMatcher = Pattern.compile( "<option value=\"?(\\d+)" ).matcher( selectMatcher.group() );
				while ( optionMatcher.find() )
				{
					itemId = StaticEntity.parseInt( optionMatcher.group(1) );
					if ( itemId >= 1 )  availableItems.add( new AdventureResult( itemId, 1 ) );
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
								request.addFormField( "item1", String.valueOf( currentTest[0].getItemId() ) );
								request.addFormField( "item2", String.valueOf( currentTest[1].getItemId() ) );
								request.addFormField( "item3", String.valueOf( currentTest[1].getItemId() ) );

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

	private class CreateItemPanel extends ItemManagePanel
	{
		public CreateItemPanel()
		{
			super( ConcoctionsDatabase.getConcoctions() );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setButtons( new ActionListener [] { new CreateListener(), new CreateAndUseListener() } );

			JCheckBox [] addedFilters = new JCheckBox[3];

			addedFilters[0] = new CreateSettingCheckbox( "Use oven/kit", "createWithoutBoxServants", "Create without requiring a box servant" );
			addedFilters[1] = new CreateSettingCheckbox( "Allow closet", "showClosetIngredients", "List items creatable when adding the closet" );
			addedFilters[2] = new CreateSettingCheckbox( "Allow stash", "showStashIngredients", "List items creatable when adding the clan stash" );

			for ( int i = 0; i < addedFilters.length; ++i )
				filterPanel.add( addedFilters[i] );
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
					KoLmafia.isAdventuring() && !ClanManager.isStashRetrieved() )
				{
					(new RequestThread( new ClanStashRequest() )).start();
				}

				ConcoctionsDatabase.refreshConcoctions();
			}
		}

		private class CreateListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object selected = elementList.getSelectedValue();

				if ( selected == null )
					return;

				ItemCreationRequest selection = (ItemCreationRequest) selected;
				int quantityDesired = getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityPossible() );
				if ( quantityDesired < 1 )
					return;

				KoLmafia.updateDisplay( "Verifying ingredients..." );
				selection.setQuantityNeeded( quantityDesired );
				(new RequestThread( selection )).start();
			}

			public String toString()
			{	return "create item";
			}
		}

		private class CreateAndUseListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object selected = elementList.getSelectedValue();

				if ( selected == null )
					return;

				Runnable [] requests = new Runnable[2];

				ItemCreationRequest selection = (ItemCreationRequest) selected;
				int quantityDesired = getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityPossible() );
				if ( quantityDesired < 1 )
					return;

				KoLmafia.updateDisplay( "Verifying ingredients..." );
				selection.setQuantityNeeded( quantityDesired );

				requests[0] = selection;
				requests[1] = new ConsumeItemRequest( new AdventureResult(
					selection.getItemId(), selection.getQuantityNeeded() ) );

				(new RequestThread( requests )).start();
			}

			public String toString()
			{	return "create & use";
			}
		}
	}
}
