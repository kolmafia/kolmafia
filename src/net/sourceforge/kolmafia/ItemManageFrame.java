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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.StoreManager.SoldItem;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class ItemManageFrame extends KoLFrame
{
	private static int pullsRemaining = 0;
	private static JLabel pullsRemainingLabel1 = new JLabel( " " );
	private static JLabel pullsRemainingLabel2 = new JLabel( " " );

	private static final Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

	private LockableListModel itemPanelNames = new LockableListModel();
	private JList itemPanelList = new JList( itemPanelNames );
	private CardLayout itemPanelCards = new CardLayout();
	private JPanel managePanel = new JPanel( itemPanelCards );

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

		addPanel( "Usable", new UsableItemPanel( false ) );
		addPanel( " - Food", new ExperimentalPanel( true, false ) );
		addPanel( " - Booze", new ExperimentalPanel( false, true ) );

		addSeparator();

		addPanel( "Inventory", new InventoryManagePanel( inventory, true ) );
		addPanel( " - Recent", new InventoryManagePanel( tally, true ) );
		addPanel( " - Closet", new InventoryManagePanel( closet, true ) );

		addSeparator();

		addPanel( "Creatable", new CreateItemPanel( true, true, true, true ) );

		addPanel( " - Cookable", new CreateItemPanel( true, false, false, false ) );
		addPanel( " - Mixable", new CreateItemPanel( false, true, false, false ) );
		addPanel( " - Others", new CreateItemPanel( false, false, true, true ) );

		addSeparator();

		addPanel( "Storage", new HagnkCompletePanel() );
		addPanel( " - Equipment", new HagnkEquipmentPanel() );

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		itemPanelList.addListSelectionListener( new CardSwitchListener() );
		itemPanelList.setPrototypeCellValue( "ABCDEFGHIJKLM" );
		itemPanelList.setCellRenderer( new OptionRenderer() );

		JPanel listHolder = new JPanel( new CardLayout( 10, 10 ) );
		listHolder.add( new SimpleScrollPane( itemPanelList ), "" );

		JPanel mainPanel = new JPanel( new BorderLayout() );

		mainPanel.add( listHolder, BorderLayout.WEST );
		mainPanel.add( managePanel, BorderLayout.CENTER );

		tabs.addTab( "Handle Items", mainPanel );
		addTab( "Update Filters", new FlaggedItemsPanel() );
		addTab( "Scripted Actions", new CommonActionsPanel() );
//		addTab( "Recipes", new InventPanel() );

		JPanel tabHolder = new JPanel( new CardLayout( 10, 10 ) );
		tabHolder.add( tabs, "" );

		itemPanelList.setSelectedIndex(0);
		framePanel.add( tabHolder, BorderLayout.CENTER );
	}

	public static int getPullsRemaining()
	{	return pullsRemaining;
	}

	public static void setPullsRemaining( int pullsRemaining )
	{
		ItemManageFrame.pullsRemaining = pullsRemaining;

		if ( KoLCharacter.isHardcore() )
		{
			pullsRemainingLabel1.setText( "In Hardcore" );
			pullsRemainingLabel2.setText( "In Hardcore" );
			return;
		}
		else
		{
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
	}

	private void addPanel( String name, JComponent panel )
	{
		itemPanelNames.add( name );
		managePanel.add( panel, String.valueOf( itemPanelNames.size() ) );
	}

	private void addSeparator()
	{
		JPanel separator = new JPanel();
		separator.setOpaque( false );
		separator.setLayout( new BoxLayout( separator, BoxLayout.Y_AXIS ) );

		separator.add( Box.createVerticalGlue() );
		separator.add( new JSeparator() );
		itemPanelNames.add( separator );
	}

	private class CardSwitchListener implements ListSelectionListener
	{
		public void valueChanged( ListSelectionEvent e )
		{
			int cardIndex = itemPanelList.getSelectedIndex();

			if ( itemPanelNames.get( cardIndex ) instanceof JComponent )
				return;

			itemPanelCards.show( managePanel, String.valueOf( cardIndex + 1 ) );
		}
	}

	private class FlaggedItemsPanel extends JPanel
	{
		private JPanel container;

		public FlaggedItemsPanel()
		{
			container = new JPanel();
			container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

			// Memento list.

			JLabel description = new JLabel( "<html>The following items are flagged as \"mementos\".  IF YOU SET A PREFERENCE, KoLmafia will never autosell these items, place them in the mall, or pulverize them, even if they are flagged as junk.  Furthermore, any item which cannot be autosold in game will be avoided by the end of run sale script and need not be added here to take effect.  The only way to bypass this restriction is to use the relay browser, which does not use this list.</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			container.add( description );
			container.add( Box.createVerticalStrut( 10 ) );

			ItemManagePanel scroller = new ItemManagePanel( mementoList );
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

			scroller = new ItemManagePanel( junkItemList );
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

			setText( "<html>Gnollish toolboxes, briefcases, small and large boxes, 31337 scrolls, Warm Subject gift certificates, and Penultimate Fantasy chests, if flagged as junk, will be used. " +
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

	private static final AdventureResult MAGNESIUM = new AdventureResult( "milk of magnesium", 1, false );

	private class ExperimentalPanel extends ItemManagePanel
	{
		private boolean food, booze;
		private JCheckBox [] filters;

		public ExperimentalPanel( boolean food, boolean booze )
		{
			super( "Use Items", "use item", food && booze ? "win game" : food ? "drink milk" : "cast ode", ConcoctionsDatabase.getUsables() );

			JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.food = food;
			this.booze = booze;

			filters = new JCheckBox[ food || booze ? 5 : 4 ];

			filters[0] = new JCheckBox( "no create" );
			filters[1] = new JCheckBox( "+mus only" );
			filters[2] = new JCheckBox( "+mys only" );
			filters[3] = new JCheckBox( "+mox only" );

			for ( int i = 0; i < 4; ++i )
				listenToCheckBox( filters[i] );

			if ( food || booze )
				filters[4] = new ExperimentalCheckbox( food, booze );

			JPanel filterPanel = new JPanel();
			for ( int i = 0; i < filters.length; ++i )
				filterPanel.add( filters[i] );

			setEnabled( true );
			actualPanel.add( filterPanel, BorderLayout.NORTH );

			filterItems();
		}

		public FilterItemField getWordFilter()
		{	return new ConsumableFilterField();
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( food && booze )
			{
				cancelledButton.setEnabled( false );
			}
			else if ( food )
			{
				cancelledButton.setEnabled( KoLCharacter.hasItem( MAGNESIUM, true ) && !activeEffects.contains( TradeableItemDatabase.GOT_MILK ) );
			}
			else
			{
				cancelledButton.setEnabled( KoLCharacter.hasSkill( "The Ode to Booze" ) && !activeEffects.contains( TradeableItemDatabase.ODE ) &&
					KoLCharacter.getMaximumMP() >= ClassSkillsDatabase.getMPConsumptionById( 6014 ) );
			}

			super.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			Object [] items = getDesiredItems( "Consume" );

			if ( items.length != 1 )
				return;

			RequestThread.openRequestSequence();
			SpecialOutfit.createImplicitCheckpoint();

			if ( items[0] instanceof AdventureResult )
			{
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[0] ) );
			}
			else
			{
				String [] pieces = ((String)items[0]).split( " => " );
				int repeat = StaticEntity.parseInt( pieces[1] );

				KoLRequest request = food ? (KoLRequest) new RestaurantRequest( pieces[0] ) : new MicrobreweryRequest( pieces[0] );
				for ( int i = 0; i < repeat; ++i )
					RequestThread.postRequest( request );
			}

			SpecialOutfit.restoreImplicitCheckpoint();
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			if ( food )
			{
				RequestThread.postRequest( new ConsumeItemRequest( MAGNESIUM ) );
			}
			else
			{
				if ( !activeEffects.contains( new AdventureResult( "Ode to Booze", 1, true ) ) )
					RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
			}
		}

		private class ConsumableFilterField extends FilterItemField
		{
			public ConsumableFilterField()
			{	filter = new ConsumableFilter();
			}

			public void filterItems()
			{	elementList.applyFilter( filter );
			}

			private class ConsumableFilter extends SimpleListFilter
			{
				public ConsumableFilter()
				{	super( ConsumableFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					Concoction creation = (Concoction) element;
					AdventureResult item = creation.getItem();

					if ( creation.getTotal() == 0 )
						return false;

					if ( filters[0].isSelected() )
					{
						if ( item != null && item.getCount( inventory ) == 0 )
							return false;
					}

					if ( filters[1].isSelected() )
					{
						String range = TradeableItemDatabase.getMuscleRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					if ( filters[2].isSelected() )
					{
						String range = TradeableItemDatabase.getMysticalityRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					if ( filters[3].isSelected() )
					{
						String range = TradeableItemDatabase.getMoxieRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					int fullness = TradeableItemDatabase.getFullness( creation.getName() );
					int inebriety = TradeableItemDatabase.getInebriety( creation.getName() );

					if ( fullness > 0 )
						return ExperimentalPanel.this.food && super.isVisible( element );
					else if ( inebriety > 0 )
						return ExperimentalPanel.this.booze && super.isVisible( element );
					else
						return false;
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

			filterItems();
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

	private class ExperimentalCheckbox extends JCheckBox implements ActionListener
	{
		public ExperimentalCheckbox( boolean food, boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : "per full" );

			setToolTipText( "Sort gains per adventure" );
			setSelected( StaticEntity.getBooleanProperty( "showGainsPerUnit" ) );

			addActionListener( this );
			KoLSettings.registerCheckbox( "showGainsPerUnit", this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( StaticEntity.getBooleanProperty( "showGainsPerUnit" ) == isSelected() )
				return;

			StaticEntity.setProperty( "showGainsPerUnit", String.valueOf( isSelected() ) );
			ConcoctionsDatabase.getUsables().sort();
		}
	}


	private class CreationSettingCheckBox extends JCheckBox implements ActionListener
	{
		private String property;

		public CreationSettingCheckBox( String label, String property, String tooltip )
		{
			super( label );

			setToolTipText( tooltip );
			setSelected( StaticEntity.getBooleanProperty( property ) );

			addActionListener( this );

			this.property = property;
			KoLSettings.registerCheckbox( property, this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( StaticEntity.getBooleanProperty( property ) == isSelected() )
				return;

			StaticEntity.setProperty( property, String.valueOf( isSelected() ) );

			ConcoctionsDatabase.recognizeNextRefresh();
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends ItemManagePanel
	{
		public CreateItemPanel( boolean food, boolean booze, boolean equip, boolean other )
		{
			super( "", "create item", "create & use", ConcoctionsDatabase.getCreatables() );
			setFixedFilter( food, booze, equip, other, true );

			JPanel filterPanel = new JPanel();

			JCheckBox allowNoBox = new CreationSettingCheckBox( "Require in-a-boxes for creation", "requireBoxServants", "Require in-a-boxes, auto-repair on explosion" );
			filterPanel.add( allowNoBox );

			JCheckBox infiniteNPC = new CreationSettingCheckBox( "Add NPC items to calculations", "assumeInfiniteNPCItems", "Assume NPC items are available for item creation" );
			filterPanel.add( infiniteNPC );

			actualPanel.add( filterPanel, BorderLayout.NORTH );

			ConcoctionsDatabase.getCreatables().applyListFilters();
			filterItems();
		}

		public void actionConfirmed()
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

			RequestThread.openRequestSequence();

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
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

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( selection.getItemId(), selection.getQuantityNeeded() ) ) );

			RequestThread.closeRequestSequence();
		}
	}

	private static class OptionRenderer extends DefaultListCellRenderer
	{
		public OptionRenderer()
		{
			setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			return value instanceof JComponent ? (Component) value :
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
		}
	}

	private class HagnkEquipmentPanel extends HagnkStoragePanel
	{
		private FilterRadioButton [] equipmentFilters;

		public HagnkEquipmentPanel()
		{
			equipmentFilters = new FilterRadioButton[7];
			equipmentFilters[0] = new FilterRadioButton( "weapons", true );
			equipmentFilters[1] = new FilterRadioButton( "offhand" );
			equipmentFilters[2] = new FilterRadioButton( "hats" );
			equipmentFilters[3] = new FilterRadioButton( "shirts" );
			equipmentFilters[4] = new FilterRadioButton( "pants" );
			equipmentFilters[5] = new FilterRadioButton( "accessories" );
			equipmentFilters[6] = new FilterRadioButton( "familiar" );

			ButtonGroup filterGroup = new ButtonGroup();
			JPanel filterPanel = new JPanel();

			for ( int i = 0; i < 7; ++i )
			{
				filterGroup.add( equipmentFilters[i] );
				filterPanel.add( equipmentFilters[i] );
			}

			eastPanel.add( pullsRemainingLabel2, BorderLayout.SOUTH );
			actualPanel.add( filterPanel, BorderLayout.NORTH );

			elementList.setCellRenderer( AdventureResult.getEquipmentRenderer() );
			filterItems();
		}

		public FilterItemField getWordFilter()
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
				listenToRadioButton( this );
			}
		}

		private class EquipmentFilterField extends FilterItemField
		{
			public EquipmentFilterField()
			{	filter = new EquipmentFilter();
			}

			private class EquipmentFilter extends SimpleListFilter
			{
				public EquipmentFilter()
				{	super( EquipmentFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					boolean isVisibleWithFilter = true;

					switch ( TradeableItemDatabase.getConsumptionType( ((AdventureResult)element).getItemId() ) )
					{
					case EQUIP_WEAPON:
						isVisibleWithFilter = equipmentFilters[0].isSelected();
						break;

					case EQUIP_OFFHAND:
						isVisibleWithFilter = equipmentFilters[1].isSelected();
						break;

					case EQUIP_HAT:
						isVisibleWithFilter = equipmentFilters[2].isSelected();
						break;

					case EQUIP_SHIRT:
						isVisibleWithFilter = equipmentFilters[3].isSelected();
						break;

					case EQUIP_PANTS:
						isVisibleWithFilter = equipmentFilters[4].isSelected();
						break;

					case EQUIP_ACCESSORY:
						isVisibleWithFilter = equipmentFilters[5].isSelected();
						break;

					case EQUIP_FAMILIAR:
						isVisibleWithFilter = equipmentFilters[6].isSelected();
						break;

					default:
						return false;
					}

					if ( !isVisibleWithFilter )
						return false;

					return super.isVisible( element );
				}
			}
		}
	}

	private class HagnkCompletePanel extends HagnkStoragePanel
	{
		public HagnkCompletePanel()
		{
			northPanel = new JPanel( new BorderLayout() );
			setButtons( true, false, null );
			actualPanel.add( northPanel, BorderLayout.NORTH );

			eastPanel.add( pullsRemainingLabel1, BorderLayout.SOUTH );
		}
	}

	private abstract class HagnkStoragePanel extends ItemManagePanel
	{
		public HagnkStoragePanel()
		{	super( "", "pull item", "closet item", storage );
		}

		public void actionConfirmed()
		{
			Object [] items = getDesiredItems( "Pulling" );
			if ( items == null )
				return;

			if ( items.length == storage.size() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
			else
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
		}

		public void actionCancelled()
		{
			Object [] items = getDesiredItems( "Pulling" );
			if ( items == null )
				return;

			RequestThread.openRequestSequence();

			if ( items.length == storage.size() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
			else
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );

			RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );

			RequestThread.closeRequestSequence();
		}
	}
}
