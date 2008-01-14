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
import net.sourceforge.kolmafia.KoLSettings;
import net.sourceforge.kolmafia.MutableComboBox;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.VioletFogManager;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

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

		String overrideSetting = KoLSettings.getUserProperty( "louvreOverride" );
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
		this.addChoiceSelect( "Manor2", "Louvre Goal", this.louvreSelect );
		this.addChoiceSelect( "Manor2", "Louvre Override", this.manualLouvre );
		this.addChoiceSelect( "Manor2", "The Maidens", this.maidenSelect );
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
			String dest = KoLSettings.getUserProperty( "oceanDestination" );
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
			String dest = KoLSettings.getUserProperty( "oceanDestination" );
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
				KoLSettings.setUserProperty( "choiceAdventure189", "2" );
				KoLSettings.setUserProperty( "oceanDestination", "ignore" );
				return;
			}

			KoLSettings.setUserProperty( "choiceAdventure189", "1" );
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

			KoLSettings.setUserProperty( "oceanDestination", value );
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
				KoLSettings.setUserProperty( "oceanDestination", coords );

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
		KoLSettings.setUserProperty(
			"louvreOverride", overrideIndex == 0 || override == null ? "" : (String) override );

		KoLSettings.setUserProperty( "violetFogGoal", String.valueOf( this.violetFogSelect.getSelectedIndex() ) );
		KoLSettings.setUserProperty( "luckySewerAdventure", (String) this.sewerSelect.getSelectedItem() );
		KoLSettings.setUserProperty( "choiceAdventure89", String.valueOf( this.maidenSelect.getSelectedIndex() ) );
		KoLSettings.setUserProperty( "choiceAdventure127", String.valueOf( this.palindomePapayaSelect.getSelectedIndex() + 1 ) );

		int louvreGoal = this.louvreSelect.getSelectedIndex();
		KoLSettings.setUserProperty(
			"choiceAdventure91", String.valueOf( overrideIndex > 0 || louvreGoal > 0 ? "1" : "2" ) );
		KoLSettings.setUserProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			int index = this.optionSelects[ i ].getSelectedIndex();
			String choice = AdventureDatabase.CHOICE_ADVS[ i ].getSetting();
			KoLSettings.setUserProperty( choice, String.valueOf( index + 1 ) );
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
			KoLSettings.setUserProperty( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "3" ); // Leave the map quest position alone
			KoLSettings.setUserProperty( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 1: // Map quest position (choice adventure 11)
			// Muscle goes through mysticality
			KoLSettings.setUserProperty( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "3" ); // Leave the map quest position alone
			KoLSettings.setUserProperty( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 2: // Muscle position (choice adventure 9)
			KoLSettings.setUserProperty( "choiceAdventure9", "3" ); // Leave the muscle position alone
			KoLSettings.setUserProperty( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			KoLSettings.setUserProperty( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 3: // Mysticality position (choice adventure 10)
			KoLSettings.setUserProperty( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			KoLSettings.setUserProperty( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 4: // Moxie position (choice adventure 12)
			KoLSettings.setUserProperty( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			KoLSettings.setUserProperty( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;

		case 5: // Turn the wheel clockwise
			KoLSettings.setUserProperty( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			KoLSettings.setUserProperty( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 6: // Turn the wheel counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			KoLSettings.setUserProperty( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 7: // Ignore this adventure
			KoLSettings.setUserProperty( "choiceAdventure9", "3" ); // Leave the muscle position alone
			KoLSettings.setUserProperty( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			KoLSettings.setUserProperty( "choiceAdventure11", "3" ); // Leave the map quest position alone
			KoLSettings.setUserProperty( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;
		}

		switch ( this.spookyForestSelect.getSelectedIndex() )
		{
		case 0: // Seal clubber corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "1" );
			KoLSettings.setUserProperty( "choiceAdventure27", "1" );
			break;

		case 1: // Turtle tamer corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "1" );
			KoLSettings.setUserProperty( "choiceAdventure27", "2" );
			break;

		case 2: // Pastamancer corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "2" );
			KoLSettings.setUserProperty( "choiceAdventure28", "1" );
			break;

		case 3: // Sauceror corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "2" );
			KoLSettings.setUserProperty( "choiceAdventure28", "2" );
			break;

		case 4: // Disco bandit corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "3" );
			KoLSettings.setUserProperty( "choiceAdventure29", "1" );
			break;

		case 5: // Accordion thief corpse
			KoLSettings.setUserProperty( "choiceAdventure26", "3" );
			KoLSettings.setUserProperty( "choiceAdventure29", "2" );
			break;
		}

		switch ( this.billiardRoomSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			KoLSettings.setUserProperty( "choiceAdventure77", "3" );
			KoLSettings.setUserProperty( "choiceAdventure78", "3" );
			KoLSettings.setUserProperty( "choiceAdventure79", "3" );
			break;

		case 1: // Muscle
			KoLSettings.setUserProperty( "choiceAdventure77", "2" );
			KoLSettings.setUserProperty( "choiceAdventure78", "2" );
			KoLSettings.setUserProperty( "choiceAdventure79", "3" );
			break;

		case 2: // Mysticality
			KoLSettings.setUserProperty( "choiceAdventure77", "2" );
			KoLSettings.setUserProperty( "choiceAdventure78", "1" );
			KoLSettings.setUserProperty( "choiceAdventure79", "2" );
			break;

		case 3: // Moxie
			KoLSettings.setUserProperty( "choiceAdventure77", "1" );
			KoLSettings.setUserProperty( "choiceAdventure78", "3" );
			KoLSettings.setUserProperty( "choiceAdventure79", "3" );
			break;

		case 4: // Library Key
			KoLSettings.setUserProperty( "choiceAdventure77", "2" );
			KoLSettings.setUserProperty( "choiceAdventure78", "1" );
			KoLSettings.setUserProperty( "choiceAdventure79", "1" );
			break;
		}

		switch ( this.riseSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			KoLSettings.setUserProperty( "choiceAdventure80", "4" );
			break;

		case 1: // Mysticality
			KoLSettings.setUserProperty( "choiceAdventure80", "3" );
			KoLSettings.setUserProperty( "choiceAdventure88", "1" );
			break;

		case 2: // Moxie
			KoLSettings.setUserProperty( "choiceAdventure80", "3" );
			KoLSettings.setUserProperty( "choiceAdventure88", "2" );
			break;

		case 3: // Mysticality Class Skill
			KoLSettings.setUserProperty( "choiceAdventure80", "3" );
			KoLSettings.setUserProperty( "choiceAdventure88", "3" );
			break;

		case 4: // Second Floor
			KoLSettings.setUserProperty( "choiceAdventure80", "99" );
			break;
		}

		switch ( this.fallSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			KoLSettings.setUserProperty( "choiceAdventure81", "4" );
			break;

		case 1: // Muscle
			KoLSettings.setUserProperty( "choiceAdventure81", "3" );
			break;

		case 2: // Gallery Key
			KoLSettings.setUserProperty( "choiceAdventure81", "1" );
			KoLSettings.setUserProperty( "choiceAdventure87", "2" );
			break;

		case 3: // Second Floor
			KoLSettings.setUserProperty( "choiceAdventure81", "99" );
			break;
		}

		// OceanDestinationComboBox handles its own settings.

		switch ( this.oceanActionSelect.getSelectedIndex() )
		{
		case 0:
			KoLSettings.setUserProperty( "oceanAction", "continue" );
			break;
		case 1:
			KoLSettings.setUserProperty( "oceanAction", "show" );
			break;
		case 2:
			KoLSettings.setUserProperty( "oceanAction", "stop" );
			break;
		case 3:
			KoLSettings.setUserProperty( "oceanAction", "savecontinue" );
			break;
		case 4:
			KoLSettings.setUserProperty( "oceanAction", "saveshow" );
			break;
		case 5:
			KoLSettings.setUserProperty( "oceanAction", "savestop" );
			break;
		}
	}

	public void loadSettings()
	{
		int index = KoLSettings.getIntegerProperty( "violetFogGoal" );
		if ( index >= 0 )
		{
			this.violetFogSelect.setSelectedIndex( index );
		}

		String setting = KoLSettings.getUserProperty( "louvreOverride" );
		if ( setting.equals( "" ) )
		{
			this.manualLouvre.setSelectedIndex( 0 );
		}
		else
		{
			this.manualLouvre.setSelectedItem( setting );
		}

		index = KoLSettings.getIntegerProperty( "louvreDesiredGoal" );
		if ( index >= 0 )
		{
			this.louvreSelect.setSelectedIndex( index );
		}

		this.maidenSelect.setSelectedIndex( KoLSettings.getIntegerProperty( "choiceAdventure89" ) );
		this.palindomePapayaSelect.setSelectedIndex( KoLSettings.getIntegerProperty( "choiceAdventure127" ) - 1 );

		boolean foundItem = false;
		String sewerItem = KoLSettings.getUserProperty( "luckySewerAdventure" );

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
			KoLSettings.setUserProperty( "luckySewerAdventure", "stolen accordion" );
			this.sewerSelect.setSelectedItem( "stolen accordion" );
		}

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			index = KoLSettings.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[ i ].getSetting() );
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
			int choice = KoLSettings.getIntegerProperty( "choiceAdventure" + i );
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

		index = KoLSettings.getIntegerProperty( "choiceAdventure26" );
		index = index * 2 + KoLSettings.getIntegerProperty( "choiceAdventure" + ( 26 + index ) ) - 3;

		this.spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

		// Figure out what to do in the billiard room

		switch ( KoLSettings.getIntegerProperty( "choiceAdventure77" ) )
		{
		case 1:

			// Moxie
			index = 3;
			break;

		case 2:
			index = KoLSettings.getIntegerProperty( "choiceAdventure78" );

			switch ( index )
			{
			case 1:
				index = KoLSettings.getIntegerProperty( "choiceAdventure79" );
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

		index = KoLSettings.getIntegerProperty( "choiceAdventure80" );
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
			this.riseSelect.setSelectedIndex( KoLSettings.getIntegerProperty( "choiceAdventure88" ) );
		}

		index = KoLSettings.getIntegerProperty( "choiceAdventure81" );
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

		String action = KoLSettings.getUserProperty( "oceanAction" );
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
