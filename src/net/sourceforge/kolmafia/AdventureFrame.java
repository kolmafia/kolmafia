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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;

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
		adventureDetails.add( this.adventureSelect, BorderLayout.CENTER );

		this.requestMeter = new JProgressBar();
		this.requestMeter.setOpaque( true );
		this.requestMeter.setStringPainted( true );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( this.requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		this.framePanel.setLayout( new BorderLayout( 20, 20 ) );
		this.framePanel.add( adventureDetails, BorderLayout.NORTH );
		this.framePanel.add( this.getSouthernTabs(), BorderLayout.CENTER );

		updateSelectedAdventure( AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) ) );
		this.fillDefaultConditions();

		JComponentUtilities.setComponentSize( this.framePanel, 640, 480 );
		CharsheetFrame.removeExtraTabs();
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void setStatusMessage( String message )
	{
		if ( this.requestMeter == null || message.length() == 0 )
			return;

		this.requestMeter.setString( message );
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
		this.tabs.insertTab( "Overview", null, this.getAdventureSummary(), null, 0 );
		this.tabs.insertTab( "Choice Advs", null, new SimpleScrollPane( new ChoiceOptionsPanel() ), null, 1 );
		return this.tabs;
	}

	private JSplitPane getAdventureSummary()
	{
		this.sessionGrid = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
			this.getAdventureSummary( "defaultDropdown1", this.locationSelect ), this.getAdventureSummary( "defaultDropdown2", this.locationSelect ) );

		int location = StaticEntity.getIntegerProperty( "defaultDropdownSplit" );

		if ( location == 0 )
			this.sessionGrid.setDividerLocation( 0.5 );
		else
			this.sessionGrid.setDividerLocation( location );

		this.sessionGrid.setResizeWeight( 0.5 );
		return this.sessionGrid;
	}

	public void dispose()
	{
		StaticEntity.setProperty( "defaultDropdownSplit", String.valueOf( this.sessionGrid.getLastDividerLocation() ) );
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
			this.choiceCards = new CardLayout( 10, 10 );

			this.choiceMap = new TreeMap();
			this.selectMap = new TreeMap();

			this.setLayout( this.choiceCards );
			this.add( new JPanel(), "" );

			String [] options;

			this.optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				this.optionSelects[i] = new JComboBox();
				options = AdventureDatabase.CHOICE_ADVS[i].getOptions();
				for ( int j = 0; j < options.length; ++j )
					this.optionSelects[i].addItem( options[j] );
			}

			this.sewerSelect = new JComboBox();
			options = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < options.length; ++i )
				this.sewerSelect.addItem( options[i] );

			this.castleWheelSelect = new JComboBox();
			this.castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
			this.castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
			this.castleWheelSelect.addItem( "Turn to muscle position" );
			this.castleWheelSelect.addItem( "Turn to mysticality position" );
			this.castleWheelSelect.addItem( "Turn to moxie position" );
			this.castleWheelSelect.addItem( "Turn clockwise" );
			this.castleWheelSelect.addItem( "Turn counterclockwise" );
			this.castleWheelSelect.addItem( "Ignore this adventure" );

			this.spookyForestSelect = new JComboBox();
			this.spookyForestSelect.addItem( "Loot Seal Clubber corpse" );
			this.spookyForestSelect.addItem( "Loot Turtle Tamer corpse" );
			this.spookyForestSelect.addItem( "Loot Pastamancer corpse" );
			this.spookyForestSelect.addItem( "Loot Sauceror corpse" );
			this.spookyForestSelect.addItem( "Loot Disco Bandit corpse" );
			this.spookyForestSelect.addItem( "Loot Accordion Thief corpse" );

			this.violetFogSelect = new JComboBox();
			for ( int i = 0; i < VioletFog.FogGoals.length; ++i )
				this.violetFogSelect.addItem( VioletFog.FogGoals[i] );

			this.louvreSelect = new JComboBox();
			this.louvreSelect.addItem( "Ignore this adventure" );
			for ( int i = 0; i < Louvre.LouvreGoals.length - 3; ++i )
				this.louvreSelect.addItem( Louvre.LouvreGoals[i] );
			for ( int i = Louvre.LouvreGoals.length - 3; i < Louvre.LouvreGoals.length; ++i )
				this.louvreSelect.addItem( "Boost " + Louvre.LouvreGoals[i] );

			this.louvreSelect.addItem( "Boost Prime Stat" );
			this.louvreSelect.addItem( "Boost Lowest Stat" );

			this.maidenSelect = new JComboBox();
			this.maidenSelect.addItem( "Fight a random knight" );
			this.maidenSelect.addItem( "Only fight the wolf knight" );
			this.maidenSelect.addItem( "Only fight the snake knight" );
			this.maidenSelect.addItem( "Maidens, then fight a random knight" );
			this.maidenSelect.addItem( "Maidens, then fight the wolf knight" );
			this.maidenSelect.addItem( "Maidens, then fight the snake knight" );

			this.billiardRoomSelect = new JComboBox();
			this.billiardRoomSelect.addItem( "ignore this adventure" );
			this.billiardRoomSelect.addItem( "muscle substats" );
			this.billiardRoomSelect.addItem( "mysticality substats" );
			this.billiardRoomSelect.addItem( "moxie substats" );
			this.billiardRoomSelect.addItem( "Spookyraven Library Key" );

			this.riseSelect = new JComboBox();
			this.riseSelect.addItem( "ignore this adventure" );
			this.riseSelect.addItem( "boost mysticality substats" );
			this.riseSelect.addItem( "boost moxie substats" );
			this.riseSelect.addItem( "acquire mysticality skill" );
			this.riseSelect.addItem( "unlock second floor stairs" );

			this.fallSelect = new JComboBox();
			this.fallSelect.addItem( "ignore this adventure" );
			this.fallSelect.addItem( "boost muscle substats" );
			this.fallSelect.addItem( "reveal key in conservatory" );
			this.fallSelect.addItem( "unlock second floor stairs" );

			this.addChoiceSelect( "Plains", "Castle Wheel", this.castleWheelSelect );
			this.addChoiceSelect( "Woods", "Forest Corpses", this.spookyForestSelect );
			this.addChoiceSelect( "Unsorted", "Violet Fog", this.violetFogSelect );
			this.addChoiceSelect( "Manor", "Billiard Room", this.billiardRoomSelect );
			this.addChoiceSelect( "Manor", "Rise of Spookyraven", this.riseSelect );
			this.addChoiceSelect( "Manor", "Fall of Spookyraven", this.fallSelect );
			this.addChoiceSelect( "Manor", "The Louvre", this.louvreSelect );
			this.addChoiceSelect( "Manor", "The Maidens", this.maidenSelect );

			this.addChoiceSelect( AdventureDatabase.LUCKY_SEWER.getZone(), AdventureDatabase.LUCKY_SEWER.getName(), this.sewerSelect );

			for ( int i = 0; i < this.optionSelects.length; ++i )
				this.addChoiceSelect( AdventureDatabase.CHOICE_ADVS[i].getZone(), AdventureDatabase.CHOICE_ADVS[i].getName(), this.optionSelects[i] );

			ArrayList optionsList;
			Object [] keys = this.choiceMap.keySet().toArray();

			for ( int i = 0; i < keys.length; ++i )
			{
				optionsList = (ArrayList) this.choiceMap.get( keys[i] );
				this.add( new ChoicePanel( optionsList ), (String) keys[i] );
			}

			this.actionCancelled();
			AdventureFrame.this.locationSelect.addListSelectionListener( new UpdateChoicesListener() );
		}

		private void addChoiceSelect( String zone, String name, JComboBox option )
		{
			if ( !this.choiceMap.containsKey( zone ) )
				this.choiceMap.put( zone, new ArrayList() );

			ArrayList options = (ArrayList) this.choiceMap.get( zone );

			if ( !options.contains( name ) )
			{
				options.add( name );
				this.selectMap.put( name, new ArrayList() );
			}

			options = (ArrayList) this.selectMap.get( name );
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
					value = (ArrayList) ChoiceOptionsPanel.this.selectMap.get( key );

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

				this.setContent( elements );
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
				KoLAdventure location = (KoLAdventure) AdventureFrame.this.locationSelect.getSelectedValue();
				if ( location == null )
					return;

				ChoiceOptionsPanel.this.choiceCards.show( ChoiceOptionsPanel.this, ChoiceOptionsPanel.this.choiceMap.containsKey( location.getParentZone() ) ? location.getParentZone() : "" );
			}
		}

		public void actionConfirmed()
		{
			if ( this.isRefreshing )
				return;

			StaticEntity.setProperty( "violetFogGoal", String.valueOf( this.violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) this.sewerSelect.getSelectedItem() );
			StaticEntity.setProperty( "choiceAdventure89", String.valueOf( this.maidenSelect.getSelectedIndex() ) );

			int louvreGoal = this.louvreSelect.getSelectedIndex();
			StaticEntity.setProperty( "choiceAdventure91",  String.valueOf( louvreGoal > 0 ? "1" : "2" ) );
			StaticEntity.setProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );

			for ( int i = 0; i < this.optionSelects.length; ++i )
			{
				int index = this.optionSelects[i].getSelectedIndex();
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

			switch ( this.castleWheelSelect.getSelectedIndex() )
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

			switch ( this.spookyForestSelect.getSelectedIndex() )
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

			switch ( this.billiardRoomSelect.getSelectedIndex() )
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

			switch ( this.riseSelect.getSelectedIndex() )
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

			switch ( this.fallSelect.getSelectedIndex() )
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
			this.isRefreshing = true;

			int index = StaticEntity.getIntegerProperty( "violetFogGoal" );
			if ( index >= 0 )
				this.violetFogSelect.setSelectedIndex( index );

			index = StaticEntity.getIntegerProperty( "louvreDesiredGoal" );
			if ( index >= 0 )
				this.louvreSelect.setSelectedIndex( index );

			this.maidenSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure89" ) );

			boolean foundItem = false;
			String sewerItem = StaticEntity.getProperty( "luckySewerAdventure" );

			String [] sewerOptions = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < sewerOptions.length; ++i )
			{
				if ( sewerOptions[i].equals( sewerItem ) )
				{
					foundItem = true;
					this.sewerSelect.setSelectedItem( sewerItem );
				}
			}

			if ( !foundItem )
			{
				StaticEntity.setProperty( "luckySewerAdventure", "stolen accordion" );
				this.sewerSelect.setSelectedItem( "stolen accordion" );
			}

			for ( int i = 0; i < this.optionSelects.length; ++i )
			{
				index = StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i].getSetting() );
				if ( index > 0 )
					this.optionSelects[i].setSelectedIndex( index - 1 );
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
				this.castleWheelSelect.setSelectedIndex( index );

			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.

			index = StaticEntity.getIntegerProperty( "choiceAdventure26" );
			index = index * 2 + StaticEntity.getIntegerProperty( "choiceAdventure" + (26 + index) ) - 3;

			this.spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

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
				this.billiardRoomSelect.setSelectedIndex( index );

			// Figure out what to do at the bookcases

			index = StaticEntity.getIntegerProperty( "choiceAdventure80" );
			if ( index == 4 )
				this.riseSelect.setSelectedIndex(0);
			else if ( index == 99 )
				this.riseSelect.setSelectedIndex(4);
			else
				this.riseSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure88" ) );

			index = StaticEntity.getIntegerProperty( "choiceAdventure81" );
			if ( index == 4 )
				this.fallSelect.setSelectedIndex(0);
			else if ( index == 3 )
				this.fallSelect.setSelectedIndex(1);
			else if ( index == 99 )
				this.riseSelect.setSelectedIndex(3);
			else
				this.fallSelect.setSelectedIndex(2);

			this.isRefreshing = false;
		}
	}
}
