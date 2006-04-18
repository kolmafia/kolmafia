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
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// containers
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;

// other imports
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Method;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.text.DecimalFormat;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame extends KoLFrame
{
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private WithdrawPanel withdrawal;
	private DonationPanel donation;
	private AttackPanel attacks;
	private WarfarePanel warfare;
	private SnapshotPanel snapshot;
	private AscensionPanel ascension;
	private MemberSearchPanel search;
	private ClanMemberPanelList results;

	public ClanManageFrame()
	{
		super( "Clan Management" );

		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();
		this.withdrawal = new WithdrawPanel();
		this.attacks = new AttackPanel();
		this.warfare = new WarfarePanel();
		this.snapshot = new SnapshotPanel();
		this.ascension = new AscensionPanel();
		this.search = new MemberSearchPanel();
		this.tabs = new JTabbedPane();

		JPanel stashPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		stashPanel.add( storing );
		stashPanel.add( withdrawal );

		tabs.addTab( "Stash Manager", stashPanel );

		JPanel snapPanel = new JPanel();
		snapPanel.setLayout( new BoxLayout( snapPanel, BoxLayout.Y_AXIS ) );
		snapPanel.add( snapshot );
		snapPanel.add( ascension );

		tabs.addTab( "Clan Snapshot", snapPanel );

		JPanel warfarePanel = new JPanel();
		warfarePanel.setLayout( new BoxLayout( warfarePanel, BoxLayout.Y_AXIS ) );
		warfarePanel.add( attacks );
		warfarePanel.add( warfare );

		tabs.addTab( "Clan Warfare", warfarePanel );

		JPanel purchasePanel = new JPanel();
		purchasePanel.setLayout( new BoxLayout( purchasePanel, BoxLayout.Y_AXIS ) );
		purchasePanel.add( donation );
		purchasePanel.add( clanBuff );

		tabs.addTab( "Clan Buffs", purchasePanel );

		results = new ClanMemberPanelList();
		JComponent [] header = new JComponent[4];
		header[0] = new JLabel( "Member Name", JLabel.LEFT );
		header[1] = new JLabel( "Clan Title", JLabel.LEFT );
		header[2] = new JLabel( "Karma", JLabel.LEFT );
		header[3] = new BootCheckBox();

		JComponentUtilities.setComponentSize( header[0], 160, 20 );
		JComponentUtilities.setComponentSize( header[1], 210, 20 );
		JComponentUtilities.setComponentSize( header[2], 80, 20 );
		JComponentUtilities.setComponentSize( header[3], 20, 20 );

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout( new BoxLayout( headerPanel, BoxLayout.X_AXIS ) );
		headerPanel.add( Box.createHorizontalStrut( 25 ) );

		for ( int i = 0; i < header.length; ++i )
		{
			headerPanel.add( Box.createHorizontalStrut( 10 ) );
			headerPanel.add( header[i] );
		}

		headerPanel.add( Box.createHorizontalStrut( 5 ) );

		JPanel resultsPanel = new JPanel( new BorderLayout() );
		resultsPanel.add( headerPanel, BorderLayout.NORTH );
		resultsPanel.add( new JScrollPane( results, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ), BorderLayout.CENTER );

		JPanel searchPanel = new JPanel( new BorderLayout() );
		searchPanel.add( search, BorderLayout.NORTH );
		searchPanel.add( resultsPanel, BorderLayout.CENTER );
		tabs.addTab( "Member Search", searchPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		(new RequestThread( new ClanStashRequest( StaticEntity.getClient() ) )).start();
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>ClanManageFrame</code>.
	 */

	private class ClanBuffPanel extends LabeledKoLPanel
	{
		private boolean isBuffing;
		private JComboBox buffField;
		private JTextField countField;

		public ClanBuffPanel()
		{
			super( "Buy Clan Buffs", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			this.isBuffing = false;

			buffField = new JComboBox( ClanBuffRequest.getRequestList( StaticEntity.getClient() ) );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( (Runnable) buffField.getSelectedItem(), getValue( countField ) )).start();
		}

		protected void actionCancelled()
		{
			if ( isBuffing )
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Purchase attempts cancelled." );
		}
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>ClanManageFrame</code>.
	 */

	private class AttackPanel extends LabeledKoLPanel
	{
		private JComboBox enemyList;

		public AttackPanel()
		{
			super( "Loot Another Clan", "attack", "refresh", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			enemyList = new JComboBox( ClanListRequest.getEnemyClans() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Victim: ", enemyList );
			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( (Runnable) enemyList.getSelectedItem() )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ClanListRequest( StaticEntity.getClient() ) )).start();
		}
	}

	private class WarfarePanel extends LabeledKoLPanel
	{
		private JTextField goodies;
		private JTextField oatmeal, recliners;
		private JTextField ground, airborne, archers;

		public WarfarePanel()
		{
			super( "Prepare for WAR!!!", "purchase", "calculate", new Dimension( 120, 20 ), new Dimension( 200, 20 ) );

			goodies = new JTextField();
			oatmeal = new JTextField();
			recliners = new JTextField();
			ground = new JTextField();
			airborne = new JTextField();
			archers = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[6];
			elements[0] = new VerifiableElement( "Goodies: ", goodies );
			elements[1] = new VerifiableElement( "Oatmeal: ", oatmeal );
			elements[2] = new VerifiableElement( "Recliners: ", recliners );
			elements[3] = new VerifiableElement( "Ground Troops: ", ground );
			elements[4] = new VerifiableElement( "Airborne Troops: ", airborne );
			elements[5] = new VerifiableElement( "La-Z-Archers: ", archers );

			setContent( elements );
		}

		public void actionConfirmed()
		{	(new RequestThread( new ClanMaterialsRequest() )).start();
		}

		public void actionCancelled()
		{
			int totalValue = getValue( goodies ) * 1000 + getValue( oatmeal ) * 3 + getValue( recliners ) * 1500 +
				getValue( ground ) * 300 + getValue( airborne ) * 500 + getValue( archers ) * 500;

			JOptionPane.showMessageDialog( null, "This purchase will cost " + totalValue + " meat" );
		}

		private class ClanMaterialsRequest extends KoLRequest
		{
			public ClanMaterialsRequest()
			{
				super( StaticEntity.getClient(), "clan_war.php" );
				addFormField( "action", "Yep." );
				addFormField( "goodies", String.valueOf( getValue( goodies ) ) );
				addFormField( "oatmeal", String.valueOf( getValue( oatmeal ) ) );
				addFormField( "recliners", String.valueOf( getValue( recliners ) ) );
				addFormField( "grunts", String.valueOf( getValue( ground ) ) );
				addFormField( "flyers", String.valueOf( getValue( airborne ) ) );
				addFormField( "archers", String.valueOf( getValue( archers ) ) );
			}

			public void run()
			{
				DEFAULT_SHELL.updateDisplay( "Purchasing clan materials..." );

				super.run();

				// Theoretically, there should be a test for error state,
				// but because I'm lazy, that's not happening.

				DEFAULT_SHELL.updateDisplay( "Purchase request processed." );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for donations to
	 * the clan coffer.
	 */

	private class DonationPanel extends LabeledKoLPanel
	{
		private JTextField amountField;

		public DonationPanel()
		{
			super( "Fund Your Clan", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient(), getValue( amountField ) ) )).start();
		}

		protected void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "The Hermit beat you to it.  ARGH!" );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class StoragePanel extends ItemManagePanel
	{
		public StoragePanel()
		{
			super( "Inside Inventory", "put in stash", "put in closet", KoLCharacter.getInventory() );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient(), elementList.getSelectedValues(), ClanStashRequest.ITEMS_TO_STASH ) )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.INVENTORY_TO_CLOSET, elementList.getSelectedValues() ) )).start();
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class WithdrawPanel extends ItemManagePanel
	{
		public WithdrawPanel()
		{
			super( "Inside Clan Stash", "put in bag", "refresh", ClanManager.getStash() );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient(), elementList.getSelectedValues(), ClanStashRequest.STASH_TO_ITEMS ) )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient() ) )).start();
		}
	}

	private class MemberSearchPanel extends KoLPanel
	{
		private JComboBox parameterSelect;
		private JComboBox matchSelect;
		private JTextField valueField;

		public MemberSearchPanel()
		{
			super( "search clan", "apply changes", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			parameterSelect = new JComboBox();
			for ( int i = 0; i < ClanSnapshotTable.FILTER_NAMES.length; ++i )
				parameterSelect.addItem( ClanSnapshotTable.FILTER_NAMES[i] );

			matchSelect = new JComboBox();
			matchSelect.addItem( "Less than..." );
			matchSelect.addItem( "Equal to..." );
			matchSelect.addItem( "Greater than..." );

			valueField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Parameter: ", parameterSelect );
			elements[1] = new VerifiableElement( "Constraint: ", matchSelect );
			elements[2] = new VerifiableElement( "Value:", valueField );

			setContent( elements, null, null, true, true );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new MemberSearcher() )).start();
		}

		private class MemberSearcher implements Runnable
		{
			public void run()
			{
				ClanManager.applyFilter( matchSelect.getSelectedIndex() - 1, parameterSelect.getSelectedIndex(), valueField.getText() );
				DEFAULT_SHELL.updateDisplay( "Search results retrieved." );
			}
		}

		protected void actionCancelled()
		{	(new RequestThread( new MemberChanger() )).start();
		}

		private class MemberChanger implements Runnable
		{
			public void run()
			{
				DEFAULT_SHELL.updateDisplay( "Determining changes..." );

				List titleChange = new ArrayList();
				List newTitles = new ArrayList();
				List boots = new ArrayList();

				Object currentComponent;
				ClanMemberPanel currentMember;
				Object desiredTitle;

				for ( int i = 0; i < results.getComponentCount(); ++i )
				{
					currentComponent = results.getComponent(i);
					if ( currentComponent instanceof ClanMemberPanel )
					{
						currentMember = (ClanMemberPanel) currentComponent;
						if ( currentMember.bootCheckBox.isSelected() )
							boots.add( currentMember.memberName.getText() );

						desiredTitle = currentMember.titleField.getText();
						if ( desiredTitle != null && !desiredTitle.equals( currentMember.initialTitle ) )
						{
							titleChange.add( currentMember.memberName.getText() );
							newTitles.add( (String) desiredTitle );
						}
					}
				}

				DEFAULT_SHELL.updateDisplay( "Applying changes..." );
				(new ClanMembersRequest( StaticEntity.getClient(), titleChange.toArray(), newTitles.toArray(), boots.toArray() )).run();
				DEFAULT_SHELL.updateDisplay( "Changes have been applied." );
			}
		}
	}

	private class BootCheckBox extends JCheckBox implements ActionListener
	{
		private boolean shouldSelect;

		public BootCheckBox()
		{
			addActionListener( this );
			setToolTipText( "Boot" );
			shouldSelect = true;
		}

		public void actionPerformed( ActionEvent e )
		{
			Object currentComponent;
			ClanMemberPanel currentMember;

			for ( int i = 0; i < results.getComponentCount(); ++i )
			{
				currentComponent = results.getComponent(i);
				if ( currentComponent instanceof ClanMemberPanel )
				{
					currentMember = (ClanMemberPanel) currentComponent;
					currentMember.bootCheckBox.setSelected( shouldSelect );
				}
			}

			shouldSelect = !shouldSelect;
		}
	}

	public class ClanMemberPanelList extends PanelList
	{
		public ClanMemberPanelList()
		{	super( 9, 540, 30, ClanSnapshotTable.getFilteredList() );
		}

		protected PanelListCell constructPanelListCell( Object value, int index )
		{
			ClanMemberPanel toConstruct = new ClanMemberPanel( (ProfileRequest) value );
			toConstruct.updateDisplay( this, value, index );
			return toConstruct;
		}
	}

	public class ClanMemberPanel extends JPanel implements PanelListCell
	{
		private JLabel memberName;
		private JTextField titleField;
		private JLabel clanKarma;
		private JCheckBox bootCheckBox;

		private String initialRank, initialTitle;
		private ProfileRequest profile;

		public ClanMemberPanel( ProfileRequest value )
		{
			this.profile = value;

			memberName = new JLabel( value.getPlayerName(), JLabel.LEFT );

			// In the event that they were just searching for fun purposes,
			// there will be no ranks.  So it still looks like something,
			// add the rank manually.

			initialRank = value.getRank();
			initialTitle = value.getTitle();
			titleField = new JTextField();
			bootCheckBox = new JCheckBox();
			clanKarma = new JLabel( df.format( value.getKarma() ), JLabel.LEFT );

			JButton profileButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			profileButton.addActionListener( new ShowProfileListener() );

			JComponentUtilities.setComponentSize( profileButton, 20, 20 );
			JComponentUtilities.setComponentSize( memberName, 160, 20 );
			JComponentUtilities.setComponentSize( titleField, 210, 20 );
			JComponentUtilities.setComponentSize( clanKarma, 80, 20 );
			JComponentUtilities.setComponentSize( bootCheckBox, 20, 20 );

			JPanel corePanel = new JPanel();
			corePanel.setLayout( new BoxLayout( corePanel, BoxLayout.X_AXIS ) );
			corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( profileButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( memberName ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( titleField ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( clanKarma ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( bootCheckBox ); corePanel.add( Box.createHorizontalStrut( 10 ) );

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( Box.createVerticalStrut( 3 ) );
			add( corePanel );
			add( Box.createVerticalStrut( 2 ) );
		}

		public void updateDisplay( PanelList list, Object value, int index )
		{
			profile = (ProfileRequest) value;
			memberName.setText( profile.getPlayerName() );
			titleField.setText( profile.getTitle() );
			clanKarma.setText( df.format( profile.getKarma() ) );
		}

		private class ShowProfileListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object [] parameters = new Object[2];
				parameters[0] = StaticEntity.getClient();
				parameters[1] = profile;

				(new CreateFrameRunnable( ProfileFrame.class, parameters )).run();
			}
		}
	}

	private class SnapshotPanel extends LabeledKoLPanel
	{
		private JCheckBox [] optionBoxes;
		private final String [][] options =
		{
			{ "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>", "Progression statistics (level, power, class)" },
			{ "<td>Title</td><td>Rank</td><td>Karma</td>", "Internal clan statistics (title, karma)" },
			{ "<td>Class</td><td>Path</td><td>Turns</td><td>Meat</td>", "Leaderboard (class, path, turns, wealth)" },
			{ "<td>PVP</td><td>Food</td><td>Drink</td>", "Miscellaneous statistics (pvp, food, booze)" },
			{ "<td>Created</td><td>Last Login</td>", "Creation and last login dates" },
		};

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "logshot", new Dimension( 300, 16 ), new Dimension( 20, 16 ) );

			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			String tableHeaderSetting = getProperty( "clanRosterHeader" );
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( tableHeaderSetting.indexOf( options[i][0] ) != -1 );
		}

		protected void actionConfirmed()
		{
			// Apply all the settings before generating the
			// needed clan ClanSnapshotTable.

			StringBuffer tableHeaderSetting = new StringBuffer();

			for ( int i = 0; i < options.length; ++i )
				if ( optionBoxes[i].isSelected() )
					tableHeaderSetting.append( options[i][0] );

			setProperty( "clanRosterHeader", tableHeaderSetting.toString() + "<td>Ascensions</td>" );

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot( 0, 0, 0, 0, false );
		}

		protected void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}

	private class AscensionPanel extends LabeledKoLPanel
	{
		private JTextField mostAscensionsBoardSizeField;
		private JTextField mainBoardSizeField;
		private JTextField classBoardSizeField;
		private JTextField maxAgeField;
		private JCheckBox playerMoreThanOnceOption;

		public AscensionPanel()
		{
			super( "Clan Leaderboards", "snapshot", new Dimension( 240, 20 ), new Dimension( 80, 20 ) );

			mostAscensionsBoardSizeField = new JTextField( "20" );
			mainBoardSizeField = new JTextField( "10" );
			classBoardSizeField = new JTextField( "5" );
			maxAgeField = new JTextField( "0" );
			playerMoreThanOnceOption = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Most Ascensions Board Size:  ", mostAscensionsBoardSizeField );
			elements[1] = new VerifiableElement( "Fastest Ascensions Board Size:  ", mainBoardSizeField );
			elements[2] = new VerifiableElement( "Class Breakdown Board Size:  ", classBoardSizeField );
			elements[3] = new VerifiableElement( "Maximum Ascension Age (in days):  ", maxAgeField );
			elements[4] = new VerifiableElement( "Allow Multiple Appearances:  ", playerMoreThanOnceOption );

			setContent( elements );
		}

		protected void actionConfirmed()
		{
			int mostAscensionsBoardSize = mostAscensionsBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : Integer.parseInt( mostAscensionsBoardSizeField.getText() );
			int mainBoardSize = mainBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : Integer.parseInt( mainBoardSizeField.getText() );
			int classBoardSize = classBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : Integer.parseInt( classBoardSizeField.getText() );
			int maxAge = maxAgeField.getText().equals( "" ) ? Integer.MAX_VALUE : Integer.parseInt( maxAgeField.getText() );
			boolean playerMoreThanOnce = playerMoreThanOnceOption.isSelected();

			String oldSetting = getProperty( "clanRosterHeader" );
			setProperty( "clanRosterHeader", "<td>Ascensions</td>" );

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot( mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce );
			setProperty( "clanRosterHeader", oldSetting );
		}

		protected void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}
}
