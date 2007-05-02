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
import java.awt.Font;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.BufferedReader;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import tab.CloseTabbedPane;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class AdventureFrame extends AdventureOptionsFrame
{
	private static AdventureFrame INSTANCE = null;

	private JProgressBar requestMeter = null;

	private JSplitPane sessionGrid;
	private AdventureSelectPanel adventureSelect;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		INSTANCE = this;
		this.adventureSelect = new AdventureSelectPanel( true );

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( adventureSelect, BorderLayout.CENTER );

		requestMeter = new JProgressBar();
		requestMeter.setOpaque( true );
		requestMeter.setStringPainted( true );
		requestMeter.setString( " " );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		framePanel.setLayout( new BorderLayout( 20, 20 ) );
		framePanel.add( adventureDetails, BorderLayout.NORTH );
		framePanel.add( getSouthernTabs(), BorderLayout.CENTER );

		updateSelectedAdventure( AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) ) );
		fillDefaultConditions();

		JComponentUtilities.setComponentSize( framePanel, 640, 480 );
		CharsheetFrame.removeExtraTabs();
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void setStatusMessage( String message )
	{
		if ( requestMeter == null || message.length() == 0 )
			return;

		requestMeter.setString( message );
	}

	public static void updateRequestMeter( int value, int maximum )
	{
		if ( INSTANCE == null || INSTANCE.requestMeter == null )
			return;

		INSTANCE.requestMeter.setMaximum( maximum );
		INSTANCE.requestMeter.setValue( value );
	}

	public static void updateSelectedAdventure( KoLAdventure location )
	{
		if ( INSTANCE == null || location == null || INSTANCE.zoneSelect == null || INSTANCE.locationSelect == null )
			return;

		if ( !conditions.isEmpty() )
			return;

		INSTANCE.zoneSelect.setSelectedItem( AdventureDatabase.ZONE_DESCRIPTIONS.get( location.getParentZone() ) );
		INSTANCE.locationSelect.setSelectedValue( location, true );
	}

	public boolean useSidePane()
	{	return true;
	}

	public JTabbedPane getSouthernTabs()
	{
		super.getSouthernTabs();
		tabs.insertTab( "Normal Options", null, getAdventureSummary(), null, 0 );

		// Components of auto-restoration

		JPanel restorePanel = new JPanel( new GridLayout( 1, 2, 10, 10 ) );

		tabs.insertTab( "Choice Adventures", null, new SimpleScrollPane( new ChoiceOptionsPanel() ), null, 1 );
		return tabs;
	}

	private JSplitPane getAdventureSummary()
	{
		this.sessionGrid = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
			getAdventureSummary( "defaultDropdown1", locationSelect ), getAdventureSummary( "defaultDropdown2", locationSelect ) );

		int location = StaticEntity.getIntegerProperty( "defaultDropdownSplit" );

		if ( location == 0 )
			sessionGrid.setDividerLocation( 0.5 );
		else
			sessionGrid.setDividerLocation( location );

		sessionGrid.setResizeWeight( 0.5 );
		return sessionGrid;
	}

	public void dispose()
	{
		StaticEntity.setProperty( "defaultDropdownSplit", String.valueOf( sessionGrid.getLastDividerLocation() ) );
		super.dispose();
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends JPanel
	{
		private boolean isRefreshing = true;
		private TreeMap choiceMap;
		private TreeMap selectMap;
		private CardLayout choiceCards;

		private JComboBox [] optionSelects;

		private JComboBox sewerSelect;
		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;
		private JComboBox violetFogSelect;
		private JComboBox maidenSelect;
		private JComboBox louvreSelect;
		private JComboBox billiardRoomSelect;
		private JComboBox riseSelect, fallSelect;

		/**
		 * Constructs a new <code>ChoiceOptionsPanel</code>.
		 */

		public ChoiceOptionsPanel()
		{
			choiceCards = new CardLayout( 10, 10 );

			choiceMap = new TreeMap();
			selectMap = new TreeMap();

			this.setLayout( choiceCards );
			add( new JPanel(), "" );

			String [] options;

			optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				optionSelects[i] = new JComboBox();
				options = AdventureDatabase.CHOICE_ADVS[i].getOptions();
				for ( int j = 0; j < options.length; ++j )
					optionSelects[i].addItem( options[j] );
			}

			sewerSelect = new JComboBox();
			options = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < options.length; ++i )
				sewerSelect.addItem( options[i] );

			castleWheelSelect = new JComboBox();
			castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
			castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
			castleWheelSelect.addItem( "Turn to muscle position" );
			castleWheelSelect.addItem( "Turn to mysticality position" );
			castleWheelSelect.addItem( "Turn to moxie position" );
			castleWheelSelect.addItem( "Turn clockwise" );
			castleWheelSelect.addItem( "Turn counterclockwise" );
			castleWheelSelect.addItem( "Ignore this adventure" );

			spookyForestSelect = new JComboBox();
			spookyForestSelect.addItem( "Loot Seal Clubber corpse" );
			spookyForestSelect.addItem( "Loot Turtle Tamer corpse" );
			spookyForestSelect.addItem( "Loot Pastamancer corpse" );
			spookyForestSelect.addItem( "Loot Sauceror corpse" );
			spookyForestSelect.addItem( "Loot Disco Bandit corpse" );
			spookyForestSelect.addItem( "Loot Accordion Thief corpse" );

			violetFogSelect = new JComboBox();
			for ( int i = 0; i < VioletFog.FogGoals.length; ++i )
				violetFogSelect.addItem( VioletFog.FogGoals[i] );

			louvreSelect = new JComboBox();
			louvreSelect.addItem( "Ignore this adventure" );
			for ( int i = 0; i < Louvre.LouvreGoals.length - 3; ++i )
				louvreSelect.addItem( Louvre.LouvreGoals[i] );
			for ( int i = Louvre.LouvreGoals.length - 3; i < Louvre.LouvreGoals.length; ++i )
				louvreSelect.addItem( "Boost " + Louvre.LouvreGoals[i] );

			louvreSelect.addItem( "Boost Prime Stat" );
			louvreSelect.addItem( "Boost Lowest Stat" );

			maidenSelect = new JComboBox();
			maidenSelect.addItem( "Fight a random knight" );
			maidenSelect.addItem( "Only fight the wolf knight" );
			maidenSelect.addItem( "Only fight the snake knight" );
			maidenSelect.addItem( "Maidens, then fight a random knight" );
			maidenSelect.addItem( "Maidens, then fight the wolf knight" );
			maidenSelect.addItem( "Maidens, then fight the snake knight" );

			billiardRoomSelect = new JComboBox();
			billiardRoomSelect.addItem( "ignore this adventure" );
			billiardRoomSelect.addItem( "muscle substats" );
			billiardRoomSelect.addItem( "mysticality substats" );
			billiardRoomSelect.addItem( "moxie substats" );
			billiardRoomSelect.addItem( "Spookyraven Library Key" );

			riseSelect = new JComboBox();
			riseSelect.addItem( "ignore this adventure" );
			riseSelect.addItem( "boost mysticality substats" );
			riseSelect.addItem( "boost moxie substats" );
			riseSelect.addItem( "acquire mysticality skill" );
			riseSelect.addItem( "unlock second floor stairs" );

			fallSelect = new JComboBox();
			fallSelect.addItem( "ignore this adventure" );
			fallSelect.addItem( "boost muscle substats" );
			fallSelect.addItem( "reveal key in conservatory" );
			fallSelect.addItem( "unlock second floor stairs" );

			addChoiceSelect( "Beanstalk", "Castle Wheel", castleWheelSelect );
			addChoiceSelect( "Woods", "Forest Corpses", spookyForestSelect );
			addChoiceSelect( "Unsorted", "Violet Fog", violetFogSelect );
			addChoiceSelect( "Manor", "Billiard Room", billiardRoomSelect );
			addChoiceSelect( "Manor", "Rise of Spookyraven", riseSelect );
			addChoiceSelect( "Manor", "Fall of Spookyraven", fallSelect );
			addChoiceSelect( "Manor", "The Louvre", louvreSelect );
			addChoiceSelect( "Manor", "The Maidens", maidenSelect );

			addChoiceSelect( AdventureDatabase.LUCKY_SEWER.getZone(), AdventureDatabase.LUCKY_SEWER.getName(), sewerSelect );

			for ( int i = 0; i < optionSelects.length; ++i )
				addChoiceSelect( AdventureDatabase.CHOICE_ADVS[i].getZone(), AdventureDatabase.CHOICE_ADVS[i].getName(), optionSelects[i] );

			ArrayList optionsList;
			Object [] keys = choiceMap.keySet().toArray();

			for ( int i = 0; i < keys.length; ++i )
			{
				optionsList = (ArrayList) choiceMap.get( keys[i] );
				add( new ChoicePanel( optionsList ), (String) keys[i] );
			}

			actionCancelled();
			locationSelect.addListSelectionListener( new UpdateChoicesListener() );
		}

		private void addChoiceSelect( String zone, String name, JComboBox option )
		{
			if ( !choiceMap.containsKey( zone ) )
				choiceMap.put( zone, new ArrayList() );

			ArrayList options = (ArrayList) choiceMap.get( zone );

			if ( !options.contains( name ) )
			{
				options.add( name );
				selectMap.put( name, new ArrayList() );
			}

			options = (ArrayList) selectMap.get( name );
			options.add( option );
		}

		private class ChoicePanel extends KoLPanel
		{
			public ChoicePanel( ArrayList options )
			{
				super( new Dimension( 150, 20 ), new Dimension( 300, 20 ) );

				Object key;
				ArrayList value;

				ArrayList elementList = new ArrayList();

				for ( int i = 0; i < options.size(); ++i )
				{
					key = options.get(i);
					value = (ArrayList) selectMap.get( key );

					if ( value.size() == 1 )
					{
						elementList.add( new VerifiableElement( key + ":  ", (JComboBox) value.get(0) ) );
					}
					else
					{
						for ( int j = 0; j < value.size(); ++j )
							elementList.add( new VerifiableElement( key + " " + (j+1) + ":  ", (JComboBox) value.get(j) ) );
					}
				}

				VerifiableElement [] elements = new VerifiableElement[ elementList.size() ];
				elementList.toArray( elements );

				setContent( elements );
			}

			public void actionConfirmed()
			{	ChoiceOptionsPanel.this.actionConfirmed();
			}

			public void actionCancelled()
			{
			}

			public void addStatusLabel()
			{
			}

			public void setEnabled( boolean isEnabled )
			{
			}
		}

		private class UpdateChoicesListener implements ListSelectionListener
		{
			public void valueChanged( ListSelectionEvent e )
			{
				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
				if ( location == null )
					return;

				choiceCards.show( ChoiceOptionsPanel.this, choiceMap.containsKey( location.getParentZone() ) ? location.getParentZone() : "" );
			}
		}

		public void actionConfirmed()
		{
			if ( isRefreshing )
				return;

			StaticEntity.setProperty( "violetFogGoal", String.valueOf( violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) sewerSelect.getSelectedItem() );
			StaticEntity.setProperty( "choiceAdventure89", String.valueOf( maidenSelect.getSelectedIndex() ) );

			int louvreGoal = louvreSelect.getSelectedIndex();
			StaticEntity.setProperty( "choiceAdventure91",  String.valueOf( louvreGoal > 0 ? "1" : "2" ) );
			StaticEntity.setProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );

			for ( int i = 0; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i].getSetting();
				StaticEntity.setProperty( choice, String.valueOf( index + 1 ) );
			}

			//              The Wheel:

			//              Muscle
			// Moxie          +         Mysticality
			//            Map Quest

			// Option 1: Turn the wheel clockwise
			// Option 2: Turn the wheel counterclockwise
			// Option 3: Leave the wheel alone

			switch ( castleWheelSelect.getSelectedIndex() )
			{
			case 0: // Map quest position (choice adventure 11)
									// Muscle goes through moxie
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 1: // Map quest position (choice adventure 11)
									// Muscle goes through mysticality
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 2: // Muscle position (choice adventure 9)
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 3: // Mysticality position (choice adventure 10)
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 4: // Moxie position (choice adventure 12)
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;

			case 5: // Turn the wheel clockwise
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 6: // Turn the wheel counterclockwise
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 7: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;
			}

			switch ( spookyForestSelect.getSelectedIndex() )
			{
			case 0: // Seal clubber corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "1" );
				break;

			case 1: // Turtle tamer corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "2" );
				break;

			case 2: // Pastamancer corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "1" );
				break;

			case 3: // Sauceror corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "2" );
				break;

			case 4: // Disco bandit corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "1" );
				break;

			case 5: // Accordion thief corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "2" );
				break;
			}

			switch ( billiardRoomSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure77", "3" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "2" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 2: // Mysticality
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "2" );
				break;

			case 3: // Moxie
				StaticEntity.setProperty( "choiceAdventure77", "1" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 4: // Library Key
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "1" );
				break;
			}

			switch ( riseSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure80", "4" );
				break;

			case 1: // Mysticality
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 2: // Moxie
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "2" );
				break;

			case 3: // Mysticality Class Skill
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "3" );
				break;

			case 4: // Second Floor
				StaticEntity.setProperty( "choiceAdventure80", "99" );
				break;
			}

			switch ( fallSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure81", "3" );
				break;

			case 2: // Gallery Key
				StaticEntity.setProperty( "choiceAdventure81", "1" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				break;

			case 3: // Second Floor
				StaticEntity.setProperty( "choiceAdventure81", "99" );
				break;
			}
		}

		public void actionCancelled()
		{
			isRefreshing = true;

			int index = StaticEntity.getIntegerProperty( "violetFogGoal" );
			if ( index >= 0 )
				violetFogSelect.setSelectedIndex( index );

			index = StaticEntity.getIntegerProperty( "louvreDesiredGoal" );
			if ( index >= 0 )
				louvreSelect.setSelectedIndex( index );

			maidenSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure89" ) );

			boolean foundItem = false;
			String sewerItem = StaticEntity.getProperty( "luckySewerAdventure" );

			String [] sewerOptions = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < sewerOptions.length; ++i )
			{
				if ( sewerOptions[i].equals( sewerItem ) )
				{
					foundItem = true;
					sewerSelect.setSelectedItem( sewerItem );
				}
			}

			if ( !foundItem )
			{
				StaticEntity.setProperty( "luckySewerAdventure", "stolen accordion" );
				sewerSelect.setSelectedItem( "stolen accordion" );
			}

			for ( int i = 0; i < optionSelects.length; ++i )
			{
				index = StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i].getSetting() );
				if ( index > 0 )
					optionSelects[i].setSelectedIndex( index - 1 );
			}

			// Determine the desired wheel position by examining
			// which choice adventure has the "3" value.
			// If none are "3", may be clockwise or counterclockwise
			// If they are all "3", leave wheel alone

			int [] counts = { 0, 0, 0, 0 };
			int option3 = 11;

			for ( int i = 9; i < 13; ++i )
			{
				int choice = StaticEntity.getIntegerProperty( "choiceAdventure" + i );
				counts[choice]++;

				if ( choice == 3 )
					option3 = i;
			}

			index = 0;

			if ( counts[1] == 4 )
			{
				// All choices say turn clockwise
				index = 5;
			}
			else if ( counts[2] == 4 )
			{
				// All choices say turn counterclockwise
				index = 6;
			}
			else if ( counts[3] == 4 )
			{
				// All choices say leave alone
				index = 7;
			}
			else if ( counts[3] != 1 )
			{
				// Bogus. Assume map quest
				index = 0;
			}
			else if ( option3 == 9)
			{
				// Muscle says leave alone
				index = 2;
			}
			else if ( option3 == 10)
			{
				// Mysticality says leave alone
				index = 3;
			}
			else if ( option3 == 11 )
			{
				// Map Quest says leave alone. If we turn
				// clockwise twice, we are going through
				// mysticality. Otherwise, through moxie.
				index = ( counts[1] == 2 ) ? 1 : 0;
			}
			else if ( option3 == 12 )
			{
				// Moxie says leave alone
				index = 4;
			}

			if ( index >= 0 )
				castleWheelSelect.setSelectedIndex( index );

			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.

			index = StaticEntity.getIntegerProperty( "choiceAdventure26" );
			index = index * 2 + StaticEntity.getIntegerProperty( "choiceAdventure" + (26 + index) ) - 3;

			spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

			// Figure out what to do in the billiard room

			switch ( StaticEntity.getIntegerProperty( "choiceAdventure77" ) )
			{
			case 1:

				// Moxie
				index = 3;
				break;

			case 2:
				index = StaticEntity.getIntegerProperty( "choiceAdventure78" );

				switch ( index )
				{
				case 1:
					index = StaticEntity.getIntegerProperty( "choiceAdventure79" );
					index = index == 1 ? 4 : index == 2 ? 2 : 0;
					break;
				case 2:
					// Muscle
					index = 1;
					break;
				case 3:
					// Ignore this adventure
					index = 0;
					break;
				}

				break;

			case 3:

				// Ignore this adventure
				index = 0;
				break;
			}

			if ( index >= 0 )
				billiardRoomSelect.setSelectedIndex( index );

			// Figure out what to do at the bookcases

			index = StaticEntity.getIntegerProperty( "choiceAdventure80" );
			if ( index == 4 )
				riseSelect.setSelectedIndex(0);
			else if ( index == 99 )
				riseSelect.setSelectedIndex(4);
			else
				riseSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure88" ) );

			index = StaticEntity.getIntegerProperty( "choiceAdventure81" );
			if ( index == 4 )
				fallSelect.setSelectedIndex(0);
			else if ( index == 3 )
				fallSelect.setSelectedIndex(1);
			else if ( index == 99 )
				riseSelect.setSelectedIndex(3);
			else
				fallSelect.setSelectedIndex(2);

			isRefreshing = false;
		}
	}
}
