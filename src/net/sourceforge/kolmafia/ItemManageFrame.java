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
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import tab.CloseTabbedPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public class ItemManageFrame extends KoLFrame
{
	private static final Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public ItemManageFrame()
	{
		super( "Item Manager" );

		tabs = new CloseTabbedPane();
		tabs.setTabLayoutPolicy( CloseTabbedPane.SCROLL_TAB_LAYOUT );

		LabeledScrollPanel npcOfferings = null;

		tabs.addTab( "Usable", new ConsumeItemPanel() );
		tabs.addTab( "Recent", new InventoryManagePanel( tally ) );
		tabs.addTab( "Inventory", new InventoryManagePanel( inventory ) );
		tabs.addTab( "Closet", new InventoryManagePanel( closet ) );
		tabs.addTab( "Creations", new CreateItemPanel() );
//		tabs.addTab( "Finder", new InventPanel() );

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

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		addTab( "Filters", new FlaggedItemsPanel() );
		addTab( "Scripts", new CommonActionsPanel() );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class FlaggedItemsPanel extends JPanel
	{
		private JPanel container;

		public FlaggedItemsPanel()
		{
			container = new JPanel();
			container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

			// Memento list.

			JLabel description = new JLabel( "<html>The following items are flagged as \"mementos\".  KoLmafia will never autosell these items, place them in the mall, or pulverize them, even if they are flagged as junk.  Furthermore, any item which cannot be autosold in game will be avoided by the end of run sale script and need not be added here to take effect.  The only way to bypass this restriction is to use the relay browser, which does not use this list.</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			container.add( description );
			container.add( Box.createVerticalStrut( 10 ) );

			SimpleScrollPane scroller = new SimpleScrollPane( new ShowDescriptionList( mementoList ) );
			scroller.setMaximumSize( MAX_WIDTH );
			scroller.setAlignmentX( LEFT_ALIGNMENT );
			container.add( scroller );

			container.add( Box.createVerticalStrut( 30 ) );

			// Junk item list.

			description = new JLabel( "<html>The following items are the items in your inventory which are flagged as \"junk\".  On many areas of KoLmafia's interface, these items will be flagged with a gray color.  In addition, there is a junk item script available in the scripts tab of this item manager which sells all of these items at once.</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			container.add( description );
			container.add( Box.createVerticalStrut( 10 ) );

			scroller = new SimpleScrollPane( new ShowDescriptionList( junkItemList ) );
			scroller.setMaximumSize( MAX_WIDTH );
			scroller.setAlignmentX( LEFT_ALIGNMENT );
			container.add( scroller );

			setLayout( new CardLayout( 10, 10 ) );
			add( container, "" );
		}
	}

	private class JunkDetailsLabel extends JLabel implements ListDataListener
	{
		public void intervalRemoved( ListDataEvent e )
		{	updateText();
		}

		public void intervalAdded( ListDataEvent e )
		{	updateText();
		}

		public void contentsChanged( ListDataEvent e )
		{	updateText();
		}

		public void updateText()
		{
			int totalValue = 0;

			AdventureResult currentItem;
			Object [] items = junkItemList.toArray();

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];
				totalValue += currentItem.getCount( inventory ) * TradeableItemDatabase.getPriceById( currentItem.getItemId() );
			}

			setText( "<html>Gnollish toolboxes, briefcases, warm subject gift certificates, and Penultimate Fantasy chests, if flagged as junk, will be used. " +
				"If you have the Pulverize and a tenderizing hammer, then items will be pulverized if you have malus access or they are weapons, armor, or pants with power greater than or equal to 100. " +
				"All other items flagged as junk will be autosold.  The current autosell value of items to be handled in this script is " + COMMA_FORMAT.format( totalValue ) + " meat.</html>" );
		}
	}

	private class JunkOnlyFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			if ( element instanceof AdventureResult )
			{
				if ( junkItemList.contains( element ) )
					return true;
			}
			else if ( element instanceof ItemCreationRequest )
			{
				if ( junkItemList.contains( ((ItemCreationRequest) element).createdItem ) )
					return true;
			}

			return false;

		}
	}

	private class ExcludeMementoFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			AdventureResult data = null;

			if ( element instanceof AdventureResult )
				data = (AdventureResult) element;
			else if ( element instanceof ItemCreationRequest )
				data = ((ItemCreationRequest) element).createdItem;

			if ( data == null )
				return false;

			return !mementoList.contains( data ) && TradeableItemDatabase.getPriceById( data.getItemId() ) > 0;
		}
	}

	private class CommonActionsPanel extends JPanel
	{
		private JPanel container;
		private JunkDetailsLabel label;
		private Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

		public CommonActionsPanel()
		{
			container = new JPanel();
			container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

			addButtonLabelList( new JunkItemsButton(), "", new ShowDescriptionList( inventory, junkItemList, new JunkOnlyFilter() ) );
			label.updateText();

			inventory.addListDataListener( label );
			junkItemList.addListDataListener( label );

			container.add( new JSeparator() );
			container.add( Box.createVerticalStrut( 15 ) );

			addButtonLabelList( new EndOfRunSaleButton(),
				"All items flagged as junk will be \"junked\" (see above script for more information).  KoLmafia will then place all items which are not already in your store at 999,999,999 meat, except for items flagged as \"mementos\" (see Filters tab for more details). " + StoreManageFrame.UNDERCUT_MESSAGE,
				new ShowDescriptionList( inventory, mementoList, new ExcludeMementoFilter() ) );

			container.add( new JSeparator() );
			container.add( Box.createVerticalStrut( 15 ) );

			addButtonAndLabel( new MallRestockButton(),
				"This feature looks at all the items currently in your store, and if you have any matching items in your inventory that are also auto-sellable, drops those items into your store at your current price.  Note that if any items are already sold out, these items will not be re-added, even if you've run this script previously on this character, as KoLmafia does not currently remember past decisions related to store management." );

			container.add( new JSeparator() );
			container.add( Box.createVerticalStrut( 15 ) );

			addButtonAndLabel( new DisplayCaseButton(),
				"This feature scans your inventory and, if it finds any items which match what's in your display case, and if you have more than one of that item in your display case, puts those items on display.  If there are items which you would rather not have extras of on display, then before running this script, auto-sell these items, pulverize these items, place these items in your closet, or place these items in your clan's stash, and KoLmafia will not add those items to your display case.  Alternatively, you may run one of the other scripts listed above, which may remove the item from your inventory." );

			setLayout( new CardLayout( 10, 10 ) );
			add( container, "" );
		}

		private void addButtonAndLabel( ThreadedButton button, String label )
		{	addButtonLabelList( button, label, null );
		}

		private void addButtonLabelList( ThreadedButton button, String label, ShowDescriptionList list )
		{
			JPanel buttonPanel = new JPanel();
			buttonPanel.add( button );
			buttonPanel.setAlignmentX( LEFT_ALIGNMENT );
			buttonPanel.setMaximumSize( MAX_WIDTH );

			container.add( buttonPanel );
			container.add( Box.createVerticalStrut( 5 ) );

			if ( list != null )
			{
				SimpleScrollPane scroller = new SimpleScrollPane( list );
				scroller.setMaximumSize( MAX_WIDTH );
				scroller.setAlignmentX( LEFT_ALIGNMENT );

				container.add( scroller );
				container.add( Box.createVerticalStrut( 15 ) );
			}

			JLabel description = button instanceof JunkItemsButton ? new JunkDetailsLabel() : new JLabel( "<html>" + label + "</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			container.add( description );
			container.add( Box.createVerticalStrut( 10 ) );

			if ( button instanceof JunkItemsButton )
				this.label = (JunkDetailsLabel) description;

			container.add( Box.createVerticalStrut( 25 ) );
		}

		private class JunkItemsButton extends ThreadedButton
		{
			public JunkItemsButton()
			{	super( "junk item script" );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				StaticEntity.getClient().makeJunkRemovalRequest();
			}
		}

		private class EndOfRunSaleButton extends ThreadedButton
		{
			public EndOfRunSaleButton()
			{
				super( "end of run sale" );
				setEnabled( KoLCharacter.canInteract() );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				StaticEntity.getClient().makeEndOfRunSaleRequest();
			}
		}

		private class MallRestockButton extends ThreadedButton
		{
			public MallRestockButton()
			{
				super( "mall store restocker" );
				setEnabled( !KoLCharacter.isHardcore() );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				RequestThread.postRequest( new StoreManageRequest() );

				SoldItem [] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
				StoreManager.getSoldItemList().toArray( sold );

				int itemCount;
				AdventureResult item;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < sold.length; ++i )
				{
					item = new AdventureResult( sold[i].getItemId(), 1 );
					itemCount = item.getCount( inventory );

					if ( itemCount > 0 && TradeableItemDatabase.getPriceById( item.getItemId() ) > 0 )
						items.add( item.getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
				{
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}

				RequestThread.postRequest( new AutoSellRequest( items.toArray(), AutoSellRequest.AUTOMALL ) );
			}
		}

		private class DisplayCaseButton extends ThreadedButton
		{
			public DisplayCaseButton()
			{
				super( "display case matcher" );
				setEnabled( !KoLCharacter.isHardcore() );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				RequestThread.postRequest( new MuseumRequest() );

				AdventureResult [] display = new AdventureResult[ collection.size() ];
				collection.toArray( display );

				int itemCount;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < display.length; ++i )
				{
					itemCount = display[i].getCount( inventory );
					if ( itemCount > 1 )
						items.add( display[i].getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
				{
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}

				RequestThread.postRequest( new MuseumRequest( items.toArray(), true ) );
			}
		}
	}

	private class SpecialPanel extends LabeledScrollPanel
	{
		private final int PURCHASE_ONE = 1;
		private final int PURCHASE_MULTIPLE = 2;

		private JList elementList;

		public SpecialPanel( LockableListModel items )
		{
			super( "", "buy one", "buy multiple", new JList( items ) );

			this.elementList = (JList) scrollComponent;
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}

		public void actionConfirmed()
		{	handlePurchase( PURCHASE_ONE );
		}

		public void actionCancelled()
		{	handlePurchase( PURCHASE_MULTIPLE );
		}

		private void handlePurchase( int purchaseType )
		{
			String item = (String) elementList.getSelectedValue();
			if ( item == null )
				return;

			int consumptionCount = purchaseType == PURCHASE_MULTIPLE ? getQuantity( "Buying multiple " + item + "...", Integer.MAX_VALUE, 1 ) : 1;
			if ( consumptionCount == 0 )
				return;

			Runnable request = elementList.getModel() == restaurantItems ?
				(KoLRequest) (new RestaurantRequest( item )) : (KoLRequest) (new MicrobreweryRequest( item ));

			StaticEntity.getClient().makeRequest( request, consumptionCount );
		}
	}

	private class ConsumeItemPanel extends ItemManagePanel
	{
		public ConsumeItemPanel()
		{
			super( "Use Items", "use item", "check wiki", inventory );

			filterPanel = new JPanel();

			filters = new JCheckBox[4];

			filters[0] = new JCheckBox( "Show food", KoLCharacter.canEat() );
			filters[1] = new JCheckBox( "Show booze", KoLCharacter.canDrink() );
			filters[2] = new JCheckBox( "Show restoratives", true );
			filters[3] = new JCheckBox( "Show others", true );

			for ( int i = 0; i < filters.length; ++i )
			{
				filterPanel.add( filters[i] );
				filters[i].addActionListener( new UpdateFilterListener() );
			}

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

			JPanel northPanel = new JPanel( new BorderLayout() );
			northPanel.add( filterPanel, BorderLayout.NORTH );
			northPanel.add( moverPanel, BorderLayout.SOUTH );

			actualPanel.add( northPanel, BorderLayout.NORTH );

			wordfilter = new ConsumableFilterComboBox();
			centerPanel.add( wordfilter, BorderLayout.NORTH );

			wordfilter.filterItems();
		}

		public void actionConfirmed()
		{
			Object [] items = getDesiredItems( "Consume" );
			if ( items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
		}

		public void actionCancelled()
		{
			String name;
			Object [] values = elementList.getSelectedValues();

			for ( int i = 0; i < values.length; ++i )
			{
				name = ((AdventureResult)values[i]).getName();
				if ( name != null )
					StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
			}
		}

		private class ConsumableFilterComboBox extends FilterItemComboBox
		{
			private boolean food, booze, restores, other;

			public ConsumableFilterComboBox()
			{	filter = new ConsumableFilter();
			}

			public void filterItems()
			{
				food = filters[0].isSelected();
				booze = filters[1].isSelected();
				restores = filters[2].isSelected();
				other = filters[3].isSelected();

				elementList.applyFilter( filter );
			}

			private class ConsumableFilter extends WordBasedFilter
			{
				public boolean isVisible( Object element )
				{
					switch ( TradeableItemDatabase.getConsumptionType( ((AdventureResult)element).getItemId() ) )
					{
					case CONSUME_EAT:
						return food && super.isVisible( element );

					case CONSUME_DRINK:
						return booze && super.isVisible( element );

					case GROW_FAMILIAR:
					case CONSUME_ZAP:
						return other && super.isVisible( element );

					case CONSUME_RESTORE:
						return restores ? super.isVisible( element ) : false;

					case CONSUME_MULTIPLE:
					case CONSUME_USE:
						return HPRestoreItemList.contains( (AdventureResult) element ) ?
							restores && super.isVisible( element ) : other && super.isVisible( element );

					default:
						return false;
					}
				}
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

		private class SearchListener extends ThreadedListener
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
				AdventureDatabase.retrieveItem( new AdventureResult( MEAT_PASTE, 1 ) );
				RequestThread.postRequest( request );

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
						testCombination( currentTest );
					}
				}

				KoLmafia.updateDisplay( ERROR_STATE, "No new item combinations were found." );
			}

			private void combineThreeItems()
			{
				RequestThread.postRequest( request );

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

							testCombination( currentTest );
						}
					}
				}

				KoLmafia.updateDisplay( ERROR_STATE, "No new item combinations were found." );
			}

			private void testCombination( AdventureResult [] currentTest )
			{
				if ( !ConcoctionsDatabase.isKnownCombination( currentTest ) )
				{
					KoLmafia.updateDisplay( "Testing combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
					request.addFormField( "item1", String.valueOf( currentTest[0].getItemId() ) );
					request.addFormField( "item2", String.valueOf( currentTest[1].getItemId() ) );

					if ( currentTest.length == 3 )
						request.addFormField( "item3", String.valueOf( currentTest[2].getItemId() ) );

					RequestThread.postRequest( request );

					if ( request.responseText.indexOf( "You acquire" ) != -1 )
					{
						KoLmafia.updateDisplay( "Found new item combination: " + currentTest[0].getName() + " + " + currentTest[1].getName() + " + " + currentTest[2].getName() );
						return;
					}
				}
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
			setButtons( new ActionListener [] { new CreateListener(), new CreateAndUseListener() } );

			JCheckBox [] addedFilters = new JCheckBox[5];

			addedFilters[0] = new CreateSettingCheckbox( "Auto-repair", "autoRepairBoxes", "Auto-repair box servant on explosion" );
			addedFilters[1] = new CreateSettingCheckbox( "Use oven/kit", "createWithoutBoxServants", "Create without requiring a box servant" );
			addedFilters[2] = new CreateSettingCheckbox( "Allow closet", "showClosetIngredients", "List items creatable when adding the closet" );
			addedFilters[3] = new CreateSettingCheckbox( "Allow stash", "showStashIngredients", "List items creatable when adding the clan stash" );
			addedFilters[4] = new CreateSettingCheckbox( "Infinite NPC", "assumeInfiniteNPCItems", "Purchase items from NPC stores whenever needed" );

			JPanel addedPanel = new JPanel();

			for ( int i = 0; i < addedFilters.length; ++i )
				addedPanel.add( addedFilters[i] );

			ConcoctionsDatabase.getConcoctions().applyListFilters();
			northPanel.add( addedPanel, BorderLayout.SOUTH );
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
					RequestThread.postRequest( new ClanStashRequest() );
				}

				ConcoctionsDatabase.refreshConcoctions();
			}
		}

		private class CreateListener extends ThreadedListener
		{
			public void run()
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
				RequestThread.postRequest( selection );
			}

			public String toString()
			{	return "create item";
			}
		}

		private class CreateAndUseListener extends ThreadedListener
		{
			public void run()
			{
				Object selected = elementList.getSelectedValue();

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
				RequestThread.postRequest( selection );
				RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( selection.getItemId(), selection.getQuantityNeeded() ) ) );
				RequestThread.closeRequestSequence();
			}

			public String toString()
			{	return "create & use";
			}
		}
	}
}
