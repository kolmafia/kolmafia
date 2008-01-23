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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.KoLPanel;
import net.sourceforge.kolmafia.MutableComboBox;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.VioletFogManager;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

/**
 * This panel allows the user to select which item they would like to do for each of the different choice
 * adventures.
 */

public class ChoiceOptionsPanel
	extends JPanel
{
	private final TreeMap choiceMap;
	private final TreeMap selectMap;
	private final CardLayout choiceCards;

	private final JComboBox[] optionSelects;

	private final JComboBox sewerSelect;
	private final JComboBox castleWheelSelect;
	private final JComboBox palindomePapayaSelect;
	private final JComboBox spookyForestSelect;
	private final JComboBox violetFogSelect;
	private final JComboBox maidenSelect;
	private final JComboBox louvreSelect;
	private final JComboBox manualLouvre;
	private final JComboBox billiardRoomSelect;
	private final JComboBox riseSelect, fallSelect;
	private final JComboBox oceanDestSelect, oceanActionSelect;

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

		String[] options;

		this.optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
		for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
		{
			this.optionSelects[ i ] = new JComboBox();
			options = AdventureDatabase.CHOICE_ADVS[ i ].getOptions();
			for ( int j = 0; j < options.length; ++j )
			{
				this.optionSelects[ i ].addItem( options[ j ] );
			}
		}

		this.sewerSelect = new JComboBox();
		options = AdventureDatabase.LUCKY_SEWER.getOptions();
		for ( int i = 0; i < options.length; ++i )
		{
			this.sewerSelect.addItem( options[ i ] );
		}

		this.castleWheelSelect = new JComboBox();
		this.castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
		this.castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
		this.castleWheelSelect.addItem( "Turn to muscle position" );
		this.castleWheelSelect.addItem( "Turn to mysticality position" );
		this.castleWheelSelect.addItem( "Turn to moxie position" );
		this.castleWheelSelect.addItem( "Turn clockwise" );
		this.castleWheelSelect.addItem( "Turn counterclockwise" );
		this.castleWheelSelect.addItem( "Ignore this adventure" );

		this.palindomePapayaSelect = new JComboBox();
		this.palindomePapayaSelect.addItem( "3 papayas" );
		this.palindomePapayaSelect.addItem( "Trade papayas for stats" );
		this.palindomePapayaSelect.addItem( "Fewer stats" );
		this.palindomePapayaSelect.addItem( "Stats until out of papayas then papayas" );
		this.palindomePapayaSelect.addItem( "Stats until out of papayas then fewer stats" );

		this.spookyForestSelect = new JComboBox();
		this.spookyForestSelect.addItem( "Loot Seal Clubber corpse" );
		this.spookyForestSelect.addItem( "Loot Turtle Tamer corpse" );
		this.spookyForestSelect.addItem( "Loot Pastamancer corpse" );
		this.spookyForestSelect.addItem( "Loot Sauceror corpse" );
		this.spookyForestSelect.addItem( "Loot Disco Bandit corpse" );
		this.spookyForestSelect.addItem( "Loot Accordion Thief corpse" );

		this.violetFogSelect = new JComboBox();
		for ( int i = 0; i < VioletFogManager.FogGoals.length; ++i )
		{
			this.violetFogSelect.addItem( VioletFogManager.FogGoals[ i ] );
		}

		this.louvreSelect = new JComboBox();
		this.louvreSelect.addItem( "Ignore this adventure" );
		for ( int i = 0; i < LouvreManager.LouvreGoals.length - 3; ++i )
		{
			this.louvreSelect.addItem( LouvreManager.LouvreGoals[ i ] );
		}
		for ( int i = LouvreManager.LouvreGoals.length - 3; i < LouvreManager.LouvreGoals.length; ++i )
		{
			this.louvreSelect.addItem( "Boost " + LouvreManager.LouvreGoals[ i ] );
		}

		this.louvreSelect.addItem( "Boost Prime Stat" );
		this.louvreSelect.addItem( "Boost Lowest Stat" );

		LockableListModel overrideList = new LockableListModel();

		this.manualLouvre = new MutableComboBox( overrideList, true );
		overrideList.add( "Use specified goal" );

		for ( int i = 1; i <= 3; ++i )
		{
			for ( int j = 1; j <= 3; ++j )
			{
				for ( int k = 1; k <= 3; ++k )
				{
					overrideList.add( this.getLouvreDirection( i ) + ", " + this.getLouvreDirection( j ) + ", " + this.getLouvreDirection( k ) );
				}
			}
		}

		String overrideSetting = Preferences.getString( "louvreOverride" );
		if ( !overrideSetting.equals( "" ) && !overrideList.contains( overrideSetting ) )
		{
			overrideList.add( 1, overrideSetting );
		}

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

		this.oceanDestSelect = new OceanDestinationComboBox();

		this.oceanActionSelect = new JComboBox();
		this.oceanActionSelect.addItem( "continue" );
		this.oceanActionSelect.addItem( "show" );
		this.oceanActionSelect.addItem( "stop" );
		this.oceanActionSelect.addItem( "save and continue" );
		this.oceanActionSelect.addItem( "save and show" );
		this.oceanActionSelect.addItem( "save and stop" );

		this.addChoiceSelect( "Plains", "Castle Wheel", this.castleWheelSelect );
		this.addChoiceSelect( "Plains", "Papaya War", this.palindomePapayaSelect );
		this.addChoiceSelect( "Woods", "Forest Corpses", this.spookyForestSelect );
		this.addChoiceSelect( "Item-Driven", "Violet Fog", this.violetFogSelect );
		this.addChoiceSelect( "Manor1", "Billiard Room", this.billiardRoomSelect );
		this.addChoiceSelect( "Manor1", "Rise of Spookyraven", this.riseSelect );
		this.addChoiceSelect( "Manor1", "Fall of Spookyraven", this.fallSelect );
		this.addChoiceSelect( "Manor1", "Louvre Goal", this.louvreSelect );
		this.addChoiceSelect( "Manor1", "Louvre Override", this.manualLouvre );
		this.addChoiceSelect( "Manor1", "The Maidens", this.maidenSelect );
		this.addChoiceSelect( "Island", "Ocean Destination", this.oceanDestSelect );
		this.addChoiceSelect( "Island", "Ocean Action", this.oceanActionSelect );

		this.addChoiceSelect(
			AdventureDatabase.LUCKY_SEWER.getZone(), AdventureDatabase.LUCKY_SEWER.getName(), this.sewerSelect );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			this.addChoiceSelect(
				AdventureDatabase.CHOICE_ADVS[ i ].getZone(), AdventureDatabase.CHOICE_ADVS[ i ].getName(),
				this.optionSelects[ i ] );
		}

		this.loadSettings();

		ArrayList optionsList;
		Object[] keys = this.choiceMap.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			optionsList = (ArrayList) this.choiceMap.get( keys[ i ] );
			this.add( new ChoicePanel( optionsList ), keys[ i ] );
		}
	}

	public UpdateChoicesListener getUpdateListener()
	{
		return new UpdateChoicesListener();
	}

	private String getLouvreDirection( final int i )
	{
		switch ( i )
		{
		case 1:
			return "up";
		case 2:
			return "down";
		default:
			return "side";
		}
	}

	private void addChoiceSelect( final String zone, final String name, final JComboBox option )
	{
		if ( !this.choiceMap.containsKey( zone ) )
		{
			this.choiceMap.put( zone, new ArrayList() );
		}

		ArrayList options = (ArrayList) this.choiceMap.get( zone );

		if ( !options.contains( name ) )
		{
			options.add( name );
			this.selectMap.put( name, new ArrayList() );
		}

		options = (ArrayList) this.selectMap.get( name );
		options.add( option );
	}

	private class ChoicePanel
		extends KoLPanel
	{
		public ChoicePanel( final ArrayList options )
		{
			super( new Dimension( 150, 20 ), new Dimension( 300, 20 ) );

			Object key;
			ArrayList value;

			ArrayList elementList = new ArrayList();

			for ( int i = 0; i < options.size(); ++i )
			{
				key = options.get( i );
				value = (ArrayList) ChoiceOptionsPanel.this.selectMap.get( key );

				if ( value.size() == 1 )
				{
					elementList.add( new VerifiableElement( key + ":  ", (JComboBox) value.get( 0 ) ) );
				}
				else
				{
					for ( int j = 0; j < value.size(); ++j )
					{
						elementList.add( new VerifiableElement(
							key + " " + ( j + 1 ) + ":  ", (JComboBox) value.get( j ) ) );
					}
				}
			}

			VerifiableElement[] elements = new VerifiableElement[ elementList.size() ];
			elementList.toArray( elements );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			ChoiceOptionsPanel.this.saveSettings();
		}

		public void actionCancelled()
		{
		}

		public void addStatusLabel()
		{
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	private class OceanDestinationComboBox
		extends JComboBox
	{
		public OceanDestinationComboBox()
		{
			super();
			createMenu();
			addActionListener( new OceanDestinationListener() );
		}

		public void createMenu()
		{
			String dest = Preferences.getString( "oceanDestination" );
			createMenu( dest );
		}

		public void createMenu( String dest )
		{
			removeAllItems();
			addItem( "ignore adventure" );
			addItem( "manual control" );
			addItem( "muscle" );
			addItem( "mysticality" );
			addItem( "moxie" );
			addItem( "random choice" );
			if ( dest.indexOf( "," ) != -1 )
			{
				addItem( "go to " + dest );
			}
			addItem( "choose destination..." );
			loadSettings( dest );
		}

		public void loadSettings()
		{
			String dest = Preferences.getString( "oceanDestination" );
			loadSettings( dest );
		}

		public void loadSettings( String dest )
		{
			if ( dest.equals( "ignore" ) )
			{
				setSelectedIndex( 0 );
			}
			else if ( dest.equals( "manual" ) )
			{
				setSelectedIndex( 1 );
			}
			else if ( dest.equals( "muscle" ) )
			{
				setSelectedIndex( 2 );
			}
			else if ( dest.equals( "mysticality" ) )
			{
				setSelectedIndex( 3 );
			}
			else if ( dest.equals( "moxie" ) )
			{
				setSelectedIndex( 4 );
			}
			else if ( dest.equals( "random" ) )
			{
				setSelectedIndex( 5 );
			}
			else if ( dest.indexOf( "," ) != -1 )
			{
				setSelectedIndex( 6 );
			}
			else
			{
				// Manual
				setSelectedIndex( 1 );
			}
		}

		public void saveSettings( String dest )
		{
			if ( dest.startsWith( "ignore" ) )
			{
				Preferences.setString( "choiceAdventure189", "2" );
				Preferences.setString( "oceanDestination", "ignore" );
				return;
			}

			Preferences.setString( "choiceAdventure189", "1" );
			String value = "";
			if ( dest.startsWith( "manual" ) )
			{
				value = "manual";
			}
			else if ( dest.startsWith( "muscle" ) )
			{
				value = "muscle";
			}
			else if ( dest.startsWith( "mysticality" ) )
			{
				value = "mysticality";
			}
			else if ( dest.startsWith( "moxie" ) )
			{
				value = "moxie";
			}
			else if ( dest.startsWith( "random" ) )
			{
				value = "random";
			}
			else if ( dest.startsWith( "go to " ) )
			{
				value = dest.substring( 6 );
			}
			else
			{
				// Shouldn't get here
				value = "manual";
			}

			Preferences.setString( "oceanDestination", value );
		}

		private class OceanDestinationListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				Object item = OceanDestinationComboBox.this.getSelectedItem();
				if ( item == null )
					return;

				String dest = (String) item;

				// See if choosing custom destination
				if ( !dest.startsWith( "choose" ) )
				{
					// Save chosen setting
					OceanDestinationComboBox.this.saveSettings( dest );
					return;
				}

				// Prompt for a new destination
				String coords = getCoordinates();
				if ( coords == null )
				{
					loadSettings();
					return;
				}

				// Save setting
				Preferences.setString( "oceanDestination", coords );

				// Rebuild combo box
				OceanDestinationComboBox.this.createMenu( coords );
			}

			private String getCoordinates()
			{
				String coords = KoLFrame.input( "Longitude, Latitude" );
				if ( coords == null )
					return null;

				int index = coords.indexOf( "," );
				if ( index == -1 )
				{
					return null;
				}

				int longitude = StaticEntity.parseInt( coords.substring( 0, index ) );
				if ( longitude < 1 || longitude > 242 )
				{
					return null;
				}

				int latitude = StaticEntity.parseInt( coords.substring( index + 1 ) );
				if ( latitude < 1 || latitude > 100 )
				{
					return null;
				}

				return String.valueOf( longitude ) + "," + String.valueOf( latitude );
			}
		}
	}

	private class UpdateChoicesListener
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			JList source = (JList) e.getSource();
			KoLAdventure location = (KoLAdventure) source.getSelectedValue();
			if ( location == null )
			{
				return;
			}

			ChoiceOptionsPanel.this.choiceCards.show(
				ChoiceOptionsPanel.this,
				ChoiceOptionsPanel.this.choiceMap.containsKey( location.getParentZone() ) ? location.getParentZone() : "" );
		}
	}

	public void saveSettings()
	{
		Object override = this.manualLouvre.getSelectedItem();
		int overrideIndex = this.manualLouvre.getSelectedIndex();
		Preferences.setString(
			"louvreOverride", overrideIndex == 0 || override == null ? "" : (String) override );

		Preferences.setString( "violetFogGoal", String.valueOf( this.violetFogSelect.getSelectedIndex() ) );
		Preferences.setString( "luckySewerAdventure", (String) this.sewerSelect.getSelectedItem() );
		Preferences.setString( "choiceAdventure89", String.valueOf( this.maidenSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure127", String.valueOf( this.palindomePapayaSelect.getSelectedIndex() + 1 ) );

		int louvreGoal = this.louvreSelect.getSelectedIndex();
		Preferences.setString(
			"choiceAdventure91", String.valueOf( overrideIndex > 0 || louvreGoal > 0 ? "1" : "2" ) );
		Preferences.setString( "louvreDesiredGoal", String.valueOf( louvreGoal ) );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			int index = this.optionSelects[ i ].getSelectedIndex();
			String choice = AdventureDatabase.CHOICE_ADVS[ i ].getSetting();
			Preferences.setString( choice, String.valueOf( index + 1 ) );
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
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 1: // Map quest position (choice adventure 11)
			// Muscle goes through mysticality
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 2: // Muscle position (choice adventure 9)
			Preferences.setString( "choiceAdventure9", "3" ); // Leave the muscle position alone
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 3: // Mysticality position (choice adventure 10)
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			Preferences.setString( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 4: // Moxie position (choice adventure 12)
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;

		case 5: // Turn the wheel clockwise
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 6: // Turn the wheel counterclockwise
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 7: // Ignore this adventure
			Preferences.setString( "choiceAdventure9", "3" ); // Leave the muscle position alone
			Preferences.setString( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;
		}

		switch ( this.spookyForestSelect.getSelectedIndex() )
		{
		case 0: // Seal clubber corpse
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "1" );
			break;

		case 1: // Turtle tamer corpse
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "2" );
			break;

		case 2: // Pastamancer corpse
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "1" );
			break;

		case 3: // Sauceror corpse
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "2" );
			break;

		case 4: // Disco bandit corpse
			Preferences.setString( "choiceAdventure26", "3" );
			Preferences.setString( "choiceAdventure29", "1" );
			break;

		case 5: // Accordion thief corpse
			Preferences.setString( "choiceAdventure26", "3" );
			Preferences.setString( "choiceAdventure29", "2" );
			break;
		}

		switch ( this.billiardRoomSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure77", "3" );
			Preferences.setString( "choiceAdventure78", "3" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 1: // Muscle
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "2" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 2: // Mysticality
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "1" );
			Preferences.setString( "choiceAdventure79", "2" );
			break;

		case 3: // Moxie
			Preferences.setString( "choiceAdventure77", "1" );
			Preferences.setString( "choiceAdventure78", "3" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 4: // Library Key
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "1" );
			Preferences.setString( "choiceAdventure79", "1" );
			break;
		}

		switch ( this.riseSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure80", "4" );
			break;

		case 1: // Mysticality
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "1" );
			break;

		case 2: // Moxie
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "2" );
			break;

		case 3: // Mysticality Class Skill
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "3" );
			break;

		case 4: // Second Floor
			Preferences.setString( "choiceAdventure80", "99" );
			break;
		}

		switch ( this.fallSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure81", "4" );
			break;

		case 1: // Muscle
			Preferences.setString( "choiceAdventure81", "3" );
			break;

		case 2: // Gallery Key
			Preferences.setString( "choiceAdventure81", "1" );
			Preferences.setString( "choiceAdventure87", "2" );
			break;

		case 3: // Second Floor
			Preferences.setString( "choiceAdventure81", "99" );
			break;
		}

		// OceanDestinationComboBox handles its own settings.

		switch ( this.oceanActionSelect.getSelectedIndex() )
		{
		case 0:
			Preferences.setString( "oceanAction", "continue" );
			break;
		case 1:
			Preferences.setString( "oceanAction", "show" );
			break;
		case 2:
			Preferences.setString( "oceanAction", "stop" );
			break;
		case 3:
			Preferences.setString( "oceanAction", "savecontinue" );
			break;
		case 4:
			Preferences.setString( "oceanAction", "saveshow" );
			break;
		case 5:
			Preferences.setString( "oceanAction", "savestop" );
			break;
		}
	}

	public void loadSettings()
	{
		int index = Preferences.getInteger( "violetFogGoal" );
		if ( index >= 0 )
		{
			this.violetFogSelect.setSelectedIndex( index );
		}

		String setting = Preferences.getString( "louvreOverride" );
		if ( setting.equals( "" ) )
		{
			this.manualLouvre.setSelectedIndex( 0 );
		}
		else
		{
			this.manualLouvre.setSelectedItem( setting );
		}

		index = Preferences.getInteger( "louvreDesiredGoal" );
		if ( index >= 0 )
		{
			this.louvreSelect.setSelectedIndex( index );
		}

		this.maidenSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure89" ) );
		this.palindomePapayaSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure127" ) - 1 );

		boolean foundItem = false;
		String sewerItem = Preferences.getString( "luckySewerAdventure" );

		String[] sewerOptions = AdventureDatabase.LUCKY_SEWER.getOptions();
		for ( int i = 0; i < sewerOptions.length; ++i )
		{
			if ( sewerOptions[ i ].equals( sewerItem ) )
			{
				foundItem = true;
				this.sewerSelect.setSelectedItem( sewerItem );
			}
		}

		if ( !foundItem )
		{
			Preferences.setString( "luckySewerAdventure", "stolen accordion" );
			this.sewerSelect.setSelectedItem( "stolen accordion" );
		}

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			index = Preferences.getInteger( AdventureDatabase.CHOICE_ADVS[ i ].getSetting() );
			if ( index > 0 )
			{
				this.optionSelects[ i ].setSelectedIndex( index - 1 );
			}
		}

		// Determine the desired wheel position by examining
		// which choice adventure has the "3" value.
		// If none are "3", may be clockwise or counterclockwise
		// If they are all "3", leave wheel alone

		int[] counts = { 0, 0, 0, 0 };
		int option3 = 11;

		for ( int i = 9; i < 13; ++i )
		{
			int choice = Preferences.getInteger( "choiceAdventure" + i );
			counts[ choice ]++ ;

			if ( choice == 3 )
			{
				option3 = i;
			}
		}

		index = 0;

		if ( counts[ 1 ] == 4 )
		{
			// All choices say turn clockwise
			index = 5;
		}
		else if ( counts[ 2 ] == 4 )
		{
			// All choices say turn counterclockwise
			index = 6;
		}
		else if ( counts[ 3 ] == 4 )
		{
			// All choices say leave alone
			index = 7;
		}
		else if ( counts[ 3 ] != 1 )
		{
			// Bogus. Assume map quest
			index = 0;
		}
		else if ( option3 == 9 )
		{
			// Muscle says leave alone
			index = 2;
		}
		else if ( option3 == 10 )
		{
			// Mysticality says leave alone
			index = 3;
		}
		else if ( option3 == 11 )
		{
			// Map Quest says leave alone. If we turn
			// clockwise twice, we are going through
			// mysticality. Otherwise, through moxie.
			index = counts[ 1 ] == 2 ? 1 : 0;
		}
		else if ( option3 == 12 )
		{
			// Moxie says leave alone
			index = 4;
		}

		if ( index >= 0 )
		{
			this.castleWheelSelect.setSelectedIndex( index );
		}

		// Now, determine what is located in choice adventure #26,
		// which shows you which slot (in general) to use.

		index = Preferences.getInteger( "choiceAdventure26" );
		index = index * 2 + Preferences.getInteger( "choiceAdventure" + ( 26 + index ) ) - 3;

		this.spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

		// Figure out what to do in the billiard room

		switch ( Preferences.getInteger( "choiceAdventure77" ) )
		{
		case 1:

			// Moxie
			index = 3;
			break;

		case 2:
			index = Preferences.getInteger( "choiceAdventure78" );

			switch ( index )
			{
			case 1:
				index = Preferences.getInteger( "choiceAdventure79" );
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
		{
			this.billiardRoomSelect.setSelectedIndex( index );
		}

		// Figure out what to do at the bookcases

		index = Preferences.getInteger( "choiceAdventure80" );
		if ( index == 4 )
		{
			this.riseSelect.setSelectedIndex( 0 );
		}
		else if ( index == 99 )
		{
			this.riseSelect.setSelectedIndex( 4 );
		}
		else
		{
			this.riseSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure88" ) );
		}

		index = Preferences.getInteger( "choiceAdventure81" );
		if ( index == 4 )
		{
			this.fallSelect.setSelectedIndex( 0 );
		}
		else if ( index == 3 )
		{
			this.fallSelect.setSelectedIndex( 1 );
		}
		else if ( index == 99 )
		{
			this.riseSelect.setSelectedIndex( 3 );
		}
		else
		{
			this.fallSelect.setSelectedIndex( 2 );
		}

		// OceanDestinationComboBox handles its own settings.

		String action = Preferences.getString( "oceanAction" );
		if ( action.equals( "continue" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 0 );
		}
		else if ( action.equals( "show" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 1 );
		}
		else if ( action.equals( "stop" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 2 );
		}
		else if ( action.equals( "savecontinue" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 3 );
		}
		else if ( action.equals( "saveshow" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 4 );
		}
		else if ( action.equals( "savestop" ) )
		{
			this.oceanActionSelect.setSelectedIndex( 5 );
		}
	}
}
