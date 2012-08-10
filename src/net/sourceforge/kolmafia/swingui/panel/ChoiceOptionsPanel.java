/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.VioletFogManager;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.textui.command.GongCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * This panel allows the user to select which item they would like to do for each of the different choice
 * adventures.
 */

public class ChoiceOptionsPanel
	extends JTabbedPane
	implements PreferenceListener
{
	private final TreeMap choiceMap;
	private final HashMap selectMap;
	private final CardLayout choiceCards;
	private final JPanel choicePanel;

	private final JComboBox[] optionSelects;

	private final JComboBox castleWheelSelect;
	private final JComboBox palindomePapayaSelect;
	private final JComboBox spookyForestSelect;
	private final JComboBox violetFogSelect;
	private final JComboBox maidenSelect;
	private final JComboBox louvreSelect;
	private final JComboBox manualLouvre;
	private final JComboBox billiardRoomSelect;
	private final JComboBox riseSelect, fallSelect;
	private final OceanDestinationComboBox oceanDestSelect;
	private final JComboBox oceanActionSelect;
	private final JComboBox barrelSelect;
	private final JComboBox darkAtticSelect;
	private final JComboBox unlivingRoomSelect;
	private final JComboBox debasementSelect;
	private final JComboBox propDeportmentSelect;
	private final JComboBox reloadedSelect;
	private final JComboBox sororityGuideSelect;
	private final JComboBox gongSelect;
	private final JComboBox basementMallSelect;
	private final JComboBox breakableSelect;
	private final JComboBox addingSelect;

	/**
	 * Constructs a new <code>ChoiceOptionsPanel</code>.
	 */

	public ChoiceOptionsPanel()
	{
		super( JTabbedPane.LEFT );
		this.choiceCards = new CardLayout( 10, 10 );

		this.choiceMap = new TreeMap();
		this.selectMap = new HashMap();

		this.choicePanel = new JPanel( this.choiceCards );
		this.choicePanel.add( new JPanel(), "" );
		this.addTab( "Zone", new GenericScrollPane( this.choicePanel ) );
		this.setToolTipTextAt( 0, "Choices specific to the current adventure zone" );

		String[] options;

		this.optionSelects = new JComboBox[ ChoiceManager.CHOICE_ADVS.length ];
		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i )
		{
			this.optionSelects[ i ] = new JComboBox();
			this.optionSelects[ i ].addItem( "show in browser" );
			options = ChoiceManager.CHOICE_ADVS[ i ].getOptions();
			for ( int j = 0; j < options.length; ++j )
			{
				this.optionSelects[ i ].addItem( options[ j ] );
			}
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
		this.spookyForestSelect.addItem( "show in browser" );
		this.spookyForestSelect.addItem( "mosquito larva or spooky mushrooms" );
		this.spookyForestSelect.addItem( "Spooky-Gro fertilizer" );
		this.spookyForestSelect.addItem( "spooky sapling & sell bar skins" );
		this.spookyForestSelect.addItem( "Spooky Temple map then skip adventure" );
		this.spookyForestSelect.addItem( "meet vampire hunter" );
		this.spookyForestSelect.addItem( "meet vampire" );
		this.spookyForestSelect.addItem( "gain meat" );
		this.spookyForestSelect.addItem( "loot Seal Clubber corpse" );
		this.spookyForestSelect.addItem( "loot Turtle Tamer corpse" );
		this.spookyForestSelect.addItem( "loot Pastamancer corpse" );
		this.spookyForestSelect.addItem( "loot Sauceror corpse" );
		this.spookyForestSelect.addItem( "loot Disco Bandit corpse" );
		this.spookyForestSelect.addItem( "loot Accordion Thief corpse" );

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

		this.manualLouvre = new AutoFilterComboBox( overrideList, true );
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
		this.maidenSelect.addItem( "Ignore this adventure" );
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

		this.barrelSelect = new JComboBox();
		this.barrelSelect.addItem( "top rows (mixed drinks)" );
		this.barrelSelect.addItem( "middle rows (basic booze)" );
		this.barrelSelect.addItem( "top & middle rows" );
		this.barrelSelect.addItem( "bottom rows (schnapps, fine wine)" );
		this.barrelSelect.addItem( "top & bottom rows" );
		this.barrelSelect.addItem( "middle & bottom rows" );
		this.barrelSelect.addItem( "all available drinks" );

		this.darkAtticSelect = new JComboBox();
		this.darkAtticSelect.addItem( "show in browser" );
		this.darkAtticSelect.addItem( "staff guides" );
		this.darkAtticSelect.addItem( "ghost trap" );
		this.darkAtticSelect.addItem( "mass kill werewolves with silver shotgun shell" );
		this.darkAtticSelect.addItem( "raise area ML, then staff guides" );
		this.darkAtticSelect.addItem( "raise area ML, then ghost trap" );
		this.darkAtticSelect.addItem( "raise area ML, then mass kill werewolves" );
		this.darkAtticSelect.addItem( "raise area ML, then mass kill werewolves or ghost trap" );
		this.darkAtticSelect.addItem( "lower area ML, then staff guides" );
		this.darkAtticSelect.addItem( "lower area ML, then ghost trap" );
		this.darkAtticSelect.addItem( "lower area ML, then mass kill werewolves" );
		this.darkAtticSelect.addItem( "lower area ML, then mass kill werewolves or ghost trap" );

		this.unlivingRoomSelect = new JComboBox();
		this.unlivingRoomSelect.addItem( "show in browser" );
		this.unlivingRoomSelect.addItem( "mass kill zombies with chainsaw chain" );
		this.unlivingRoomSelect.addItem( "mass kill skeletons with funhouse mirror" );
		this.unlivingRoomSelect.addItem( "get costume item" );
		this.unlivingRoomSelect.addItem( "raise area ML, then mass kill zombies" );
		this.unlivingRoomSelect.addItem( "raise area ML, then mass kill skeletons" );
		this.unlivingRoomSelect.addItem( "raise area ML, then mass kill zombies/skeletons" );
		this.unlivingRoomSelect.addItem( "raise area ML, then get costume item" );
		this.unlivingRoomSelect.addItem( "lower area ML, then mass kill zombies" );
		this.unlivingRoomSelect.addItem( "lower area ML, then mass kill skeletons" );
		this.unlivingRoomSelect.addItem( "lower area ML, then get costume item" );
		this.unlivingRoomSelect.addItem( "lower area ML, then mass kill zombies/skeletons" );

		this.debasementSelect = new JComboBox();
		this.debasementSelect.addItem( "show in browser" );
		this.debasementSelect.addItem( "Prop Deportment" );
		this.debasementSelect.addItem( "mass kill vampires with plastic vampire fangs" );
		this.debasementSelect.addItem( "raise area ML, then Prop Deportment" );
		this.debasementSelect.addItem( "raise area ML, then mass kill vampires" );
		this.debasementSelect.addItem( "lower area ML, then Prop Deportment" );
		this.debasementSelect.addItem( "lower area ML, then mass kill vampires" );

		this.propDeportmentSelect = new JComboBox();
		this.propDeportmentSelect.addItem( "show in browser" );
		this.propDeportmentSelect.addItem( "chainsaw chain" );
		this.propDeportmentSelect.addItem( "silver item" );
		this.propDeportmentSelect.addItem( "funhouse mirror" );
		this.propDeportmentSelect.addItem( "chainsaw/mirror" );

		this.reloadedSelect = new JComboBox();
		this.reloadedSelect.addItem( "show in browser" );
		this.reloadedSelect.addItem( "melt Maxwell's Silver Hammer" );
		this.reloadedSelect.addItem( "melt silver tongue charrrm bracelet" );
		this.reloadedSelect.addItem( "melt silver cheese-slicer" );
		this.reloadedSelect.addItem( "melt silver shrimp fork" );
		this.reloadedSelect.addItem( "melt silver pat� knife" );
		this.reloadedSelect.addItem( "don't melt anything" );

		this.sororityGuideSelect = new JComboBox();
		this.sororityGuideSelect.addItem( "show in browser" );
		this.sororityGuideSelect.addItem( "attic" );
		this.sororityGuideSelect.addItem( "main floor" );
		this.sororityGuideSelect.addItem( "basement" );

		this.gongSelect = new JComboBox();
		for ( int i = 0; i < GongCommand.GONG_PATHS.length; ++i )
		{
			this.gongSelect.addItem( GongCommand.GONG_PATHS[ i ] );
		}

		this.basementMallSelect = new JComboBox();
		this.basementMallSelect.addItem( "do not show Mall prices" );
		this.basementMallSelect.addItem( "show Mall prices for items you don't have" );
		this.basementMallSelect.addItem( "show Mall prices for all items" );

		this.breakableSelect = new JComboBox();
		this.breakableSelect.addItem( "abort on breakage" );
		this.breakableSelect.addItem( "equip previous" );
		this.breakableSelect.addItem( "re-equip from inventory, or abort" );
		this.breakableSelect.addItem( "re-equip from inventory, or previous" );
		this.breakableSelect.addItem( "acquire & re-equip" );

		this.addingSelect = new JComboBox();
		this.addingSelect.addItem( "show in browser" );
		this.addingSelect.addItem( "create goal scrolls only" );
		this.addingSelect.addItem( "create goal & 668 scrolls" );
		this.addingSelect.addItem( "create goal, 31337, 668 scrolls" );

		this.addChoiceSelect( "Item-Driven", "Llama Gong", this.gongSelect );
		this.addChoiceSelect( "Item-Driven", "Breakable Equipment", this.breakableSelect );
		this.addChoiceSelect( "Plains", "Castle Wheel", this.castleWheelSelect );
		this.addChoiceSelect( "Plains", "Papaya War", this.palindomePapayaSelect );
		this.addChoiceSelect( "Plains", "Ferny's Basement", this.basementMallSelect );
		this.addChoiceSelect( "Woods", "Spooky Forest", this.spookyForestSelect );
		this.addChoiceSelect( "Item-Driven", "Violet Fog", this.violetFogSelect );
		this.addChoiceSelect( "Manor1", "Billiard Room", this.billiardRoomSelect );
		this.addChoiceSelect( "Manor1", "Rise of Spookyraven", this.riseSelect );
		this.addChoiceSelect( "Manor1", "Fall of Spookyraven", this.fallSelect );
		this.addChoiceSelect( "Manor1", "Louvre Goal", this.louvreSelect );
		this.addChoiceSelect( "Manor1", "Louvre Override", this.manualLouvre );
		this.addChoiceSelect( "Manor1", "The Maidens", this.maidenSelect );
		this.addChoiceSelect( "Island", "Ocean Destination", this.oceanDestSelect );
		this.addChoiceSelect( "Island", "Ocean Action", this.oceanActionSelect );
		this.addChoiceSelect( "Mountain", "Barrel full of Barrels", this.barrelSelect );
		this.addChoiceSelect( "Mountain", "Orc Chasm", this.addingSelect );
		this.addChoiceSelect( "Events", "Sorority House Attic", this.darkAtticSelect );
		this.addChoiceSelect( "Events", "Sorority House Unliving Room", this.unlivingRoomSelect );
		this.addChoiceSelect( "Events", "Sorority House Debasement", this.debasementSelect );
		this.addChoiceSelect( "Events", "Sorority House Prop Deportment", this.propDeportmentSelect );
		this.addChoiceSelect( "Events", "Sorority House Relocked and Reloaded", this.reloadedSelect );
		this.addChoiceSelect( "Item-Driven", "Sorority Staff Guide", this.sororityGuideSelect );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			this.addChoiceSelect(
				ChoiceManager.CHOICE_ADVS[ i ].getZone(), ChoiceManager.CHOICE_ADVS[ i ].getName(),
				this.optionSelects[ i ] );
		}

		this.addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 llama lama gong" ) );
		this.addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 tiny bottle of absinthe" ) );
		this.addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 haunted sorority house staff guide" ) );

		PreferenceListenerRegistry.registerListener( "choiceAdventure*", this );
		PreferenceListenerRegistry.registerListener( "violetFogGoal", this );
		PreferenceListenerRegistry.registerListener( "louvreOverride", this );
		PreferenceListenerRegistry.registerListener( "louvreDesiredGoal", this );
		PreferenceListenerRegistry.registerListener( "barrelGoal", this );
		PreferenceListenerRegistry.registerListener( "gongPath", this );
		PreferenceListenerRegistry.registerListener( "oceanAction", this );
		PreferenceListenerRegistry.registerListener( "oceanDestination", this );
		PreferenceListenerRegistry.registerListener( "basementMallPrices", this );
		PreferenceListenerRegistry.registerListener( "breakableHandling", this );
		PreferenceListenerRegistry.registerListener( "addingScrolls", this );

		this.loadSettings();

		ArrayList optionsList;
		Object[] keys = this.choiceMap.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			optionsList = (ArrayList) this.choiceMap.get( keys[ i ] );
			if ( keys[ i ].equals( "Item-Driven" ) )
			{
				this.addTab( "Item",
					new GenericScrollPane( new ChoicePanel( optionsList ) ) );
				this.setToolTipTextAt( 1, "Choices related to the use of an item" );
			}
			else
			{
				this.choicePanel.add( new ChoicePanel( optionsList ), keys[ i ] );
			}
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

	private void addChoiceSelect( final String zone, final String name, final JComponent option )
	{
		if ( zone == null )
		{
			return;
		}

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
		extends GenericPanel
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
					elementList.add( new VerifiableElement( key + ":  ", (JComponent) value.get( 0 ) ) );
				}
				else
				{
					for ( int j = 0; j < value.size(); ++j )
					{
						elementList.add( new VerifiableElement(
							key + " " + ( j + 1 ) + ":  ", (JComponent) value.get( j ) ) );
					}
				}
			}

			VerifiableElement[] elements = new VerifiableElement[ elementList.size() ];
			elementList.toArray( elements );

			this.setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			ChoiceOptionsPanel.this.saveSettings();
		}

		@Override
		public void actionCancelled()
		{
		}

		@Override
		public void addStatusLabel()
		{
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	private class OceanDestinationComboBox
		extends JComboBox
		implements ActionListener
	{
		public OceanDestinationComboBox()
		{
			super();
			this.createMenu( Preferences.getString( "oceanDestination" ) );
			this.addActionListener( this );
		}

		private void createMenu( String dest )
		{
			this.addItem( "ignore adventure" );
			this.addItem( "manual control" );
			this.addItem( "muscle" );
			this.addItem( "mysticality" );
			this.addItem( "moxie" );
			this.addItem( "El Vibrato power sphere" );
			this.addItem( "the plinth" );
			this.addItem( "random choice" );
			if ( dest.indexOf( "," ) != -1 )
			{
				this.addItem( "go to " + dest );
			}
			this.addItem( "choose destination..." );
		}

		public void loadSettings()
		{
			this.loadSettings( Preferences.getString( "oceanDestination" ) );
		}

		private void loadSettings( String dest )
		{
			// Default is "Manual"
			int index = 1;

			if ( dest.equals( "ignore" ) )
			{
				index = 0;
			}
			else if ( dest.equals( "manual" ) )
			{
				index = 1;
			}
			else if ( dest.equals( "muscle" ) )
			{
				index = 2;
			}
			else if ( dest.equals( "mysticality" ) )
			{
				index = 3;
			}
			else if ( dest.equals( "moxie" ) )
			{
				index = 4;
			}
			else if ( dest.equals( "sphere" ) )
			{
				index = 5;
			}
			else if ( dest.equals( "plinth" ) )
			{
				index = 6;
			}
			else if ( dest.equals( "random" ) )
			{
				index = 7;
			}
			else if ( dest.indexOf( "," ) != -1 )
			{
				index = 8;
			}

			this.setSelectedIndex( index );
		}

		public void saveSettings()
		{
			String dest = (String) this.getSelectedItem();
			if ( dest == null )
			{
				return;
			}

			if ( dest.startsWith( "ignore" ) )
			{
				Preferences.setString( "choiceAdventure189", "2" );
				Preferences.setString( "oceanDestination", "ignore" );
				return;
			}

			String value = "";
			if ( dest.startsWith( "muscle" ) )
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
			else if ( dest.startsWith( "El Vibrato power sphere" ) )
			{
				value = "sphere";
			}
			else if ( dest.startsWith( "the plinth" ) )
			{
				value = "plinth";
			}
			else if ( dest.startsWith( "random" ) )
			{
				value = "random";
			}
			else if ( dest.startsWith( "go to " ) )
			{
				value = dest.substring( 6 );
			}
			else if ( dest.startsWith( "choose " ) )
			{
				return;
			}
			else	// For anything else, assume Manual Control
			{
				// For manual control, do not take a choice first
				Preferences.setString( "choiceAdventure189", "0" );
				Preferences.setString( "oceanDestination", "manual" );
				return;
			}

			Preferences.setString( "choiceAdventure189", "1" );
			Preferences.setString( "oceanDestination", value );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			String dest = (String) this.getSelectedItem();
			if ( dest == null )
			{
				return;
			}

			// Are we choosing a custom destination?
			if ( !dest.startsWith( "choose" ) )
			{
				return;
			}

			// Prompt for a new destination
			String coords = getCoordinates();
			if ( coords == null )
			{
				// Restore previous selection
				this.loadSettings();
				return;
			}

			// Rebuild combo box
			this.removeAllItems();
			this.createMenu( coords );

			// Select the "go to" menu item
			this.setSelectedIndex( 8 );

			// Request that the settings be saved in a different thread.
			RequestThread.runInParallel( new SaveOceanDestinationSettingsRunnable( this ) );
		}

		private String getCoordinates()
		{
			String coords = InputFieldUtilities.input( "Longitude, Latitude" );
			if ( coords == null )
			{
				return null;
			}

			int index = coords.indexOf( "," );
			if ( index == -1 )
			{
				return null;
			}

			int longitude = StringUtilities.parseInt( coords.substring( 0, index ) );
			if ( longitude < 1 || longitude > 242 )
			{
				return null;
			}

			int latitude = StringUtilities.parseInt( coords.substring( index + 1 ) );
			if ( latitude < 1 || latitude > 100 )
			{
				return null;
			}

			return String.valueOf( longitude ) + "," + String.valueOf( latitude );
		}
	}

	private static class SaveOceanDestinationSettingsRunnable
		implements Runnable
	{
		private OceanDestinationComboBox dest;

		public SaveOceanDestinationSettingsRunnable( OceanDestinationComboBox dest )
		{
			this.dest = dest;
		}

		public void run()
		{
			this.dest.saveSettings();
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
			String zone = location.getParentZone();
			if ( zone.equals( "Item-Driven" ) )
			{
				ChoiceOptionsPanel.this.setSelectedIndex( 1 );
				ChoiceOptionsPanel.this.choiceCards.show(
					ChoiceOptionsPanel.this.choicePanel, "" );
			}
			else
			{
				ChoiceOptionsPanel.this.setSelectedIndex( 0 );
				ChoiceOptionsPanel.this.choiceCards.show(
					ChoiceOptionsPanel.this.choicePanel,
					ChoiceOptionsPanel.this.choiceMap.containsKey( zone ) ? zone : "" );
			}
			KoLCharacter.updateSelectedLocation( location );
		}
	}

	private boolean isAdjusting = false;

	public synchronized void update()
	{
		if ( !this.isAdjusting )
		{
			this.loadSettings();
		}
	}

	public synchronized void saveSettings()
	{
		if ( this.isAdjusting )
		{
			return;
		}
		this.isAdjusting = true;

		Object override = this.manualLouvre.getSelectedItem();
		int overrideIndex = this.manualLouvre.getSelectedIndex();
		Preferences.setString( "louvreOverride",
			overrideIndex == 0 || override == null ? "" : (String) override );

		Preferences.setInteger( "violetFogGoal", this.violetFogSelect.getSelectedIndex() );
		Preferences.setString( "choiceAdventure127",
			String.valueOf( this.palindomePapayaSelect.getSelectedIndex() + 1 ) );
		Preferences.setInteger( "barrelGoal", this.barrelSelect.getSelectedIndex() + 1 );
		Preferences.setString( "choiceAdventure549",
			String.valueOf( this.darkAtticSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure550",
			String.valueOf( this.unlivingRoomSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure551",
			String.valueOf( this.debasementSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure552",
			String.valueOf( this.propDeportmentSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure553",
			String.valueOf( this.reloadedSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure554",
			String.valueOf( this.sororityGuideSelect.getSelectedIndex() ) );
		Preferences.setInteger( "basementMallPrices", this.basementMallSelect.getSelectedIndex() );
		Preferences.setInteger( "breakableHandling", this.breakableSelect.getSelectedIndex() + 1 );
		Preferences.setInteger( "addingScrolls", this.addingSelect.getSelectedIndex() );
		Preferences.setInteger( "gongPath", this.gongSelect.getSelectedIndex() );
		GongCommand.setPath( this.gongSelect.getSelectedIndex() );

		int louvreGoal = this.louvreSelect.getSelectedIndex();
		Preferences.setString( "choiceAdventure91",
			String.valueOf( overrideIndex > 0 || louvreGoal > 0 ? "1" : "2" ) );
		Preferences.setInteger( "louvreDesiredGoal", louvreGoal );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			int index = this.optionSelects[ i ].getSelectedIndex();
			String choice = ChoiceManager.CHOICE_ADVS[ i ].getSetting();
			Preferences.setString( choice, String.valueOf( index ) );
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
		case 0:		// Manual Control
			Preferences.setString( "choiceAdventure502", "0" );
			break;
		case 1:		// Mosquito Larva or Spooky Mushrooms
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "1" );
			break;
		case 2:		// Spooky-Gro Fertilizer
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "2" );
			break;
		case 3:		// Spooky Sapling & Sell Bar Skins
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "3" );
			// If we have no Spooky Sapling
			// Preferences.setString( "choiceAdventure504", "3" );
			// If we have bear skins:
			// Preferences.setString( "choiceAdventure504", "2" );
			// Exit choice
			Preferences.setString( "choiceAdventure504", "4" );
			break;
		case 4:		// Spooky Temple Map then skip adventure
			// Without tree-holed coin
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "2" );
			// With tree-holed coin
			// Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "3" );
			Preferences.setString( "choiceAdventure507", "1" );
			break;
		case 5:		// Meet Vampire Hunter
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "2" );
			break;
		case 6:		// Meet Vampire
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "3" );
			break;
		case 7:		// Gain Meat
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "1" );
			break;
		case 8:	 // Seal clubber corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "1" );
			break;
		case 9:	// Loot Turtle Tamer corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "2" );
			break;
		case 10:	// Loot Pastamancer corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "1" );
			break;
		case 11:	// Loot Sauceror corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "2" );
			break;
		case 12:	// Loot Disco Bandit corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "3" );
			Preferences.setString( "choiceAdventure29", "1" );
			break;
		case 13:	// Loot Accordion Thief corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
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

		// necessary for backwards-compatibility
		switch ( this.maidenSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure89", "6" );
			break;

		case 1: // Fight a random knight
		case 2: // Only fight the wolf knight
		case 3: // Only fight the snake knight
		case 4: // Maidens, then fight a random knight
		case 5: // Maidens, then fight the wolf knight
		case 6: // Maidens, then fight the snake knight
			Preferences.setString( "choiceAdventure89",
				String.valueOf( this.maidenSelect.getSelectedIndex() - 1 ) );
			break;
		}

		// OceanDestinationComboBox handles its own settings.
		this.oceanDestSelect.saveSettings();

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

		this.isAdjusting = false;
	}

	public synchronized void loadSettings()
	{
		this.isAdjusting = true;
		ActionPanel.enableActions( false );	// prevents recursive actions from being triggered

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

		this.palindomePapayaSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "choiceAdventure127" ) - 1 ) );
		this.barrelSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "barrelGoal" ) - 1 ) );
		this.darkAtticSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure549" ) );
		this.unlivingRoomSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure550" ) );
		this.debasementSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure551" ) );
		this.propDeportmentSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure552" ) );
		this.reloadedSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure553" ) );
		this.sororityGuideSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure554" ) );
		this.basementMallSelect.setSelectedIndex( Preferences.getInteger( "basementMallPrices" ) );
		this.breakableSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "breakableHandling" ) - 1 ) );

		int adding = Preferences.getInteger( "addingScrolls" );
		if ( adding == -1 )
		{
			adding = Preferences.getBoolean( "createHackerSummons" ) ? 3 : 2;
			Preferences.setInteger( "addingScrolls", adding );
		}
		this.addingSelect.setSelectedIndex( adding );

		this.gongSelect.setSelectedIndex( Preferences.getInteger( "gongPath" ) );

		for ( int i = 0; i < this.optionSelects.length; ++i )
		{
			index = Preferences.getInteger( ChoiceManager.CHOICE_ADVS[ i ].getSetting() );
			if ( index >= 0 )
			{
				if ( index >= this.optionSelects[ i ].getItemCount() )
				{
					System.out.println( "Invalid setting " + index + " for "
						+ ChoiceManager.CHOICE_ADVS[ i ].getSetting() );
					index = 0;
				}
				this.optionSelects[ i ].setSelectedIndex( index );
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

		// Figure out what to do in the spooky forest
		switch ( Preferences.getInteger( "choiceAdventure502" ) )
		{
		default:
		case 0:
			// Manual Control
			index = 0;
			break;

		case 1:
			switch ( Preferences.getInteger( "choiceAdventure503" ) )
			{
			case 1:	// Get Meat
				index = 7;
				break;
			case 2:	// Meet Vampire Hunter
				index = 5;
				break;
			case 3:	// Spooky Sapling & Sell Bar Skins
				index = 3;
				break;
			}
		case 2:
			switch ( Preferences.getInteger( "choiceAdventure505" ) )
			{
			case 1:	// Mosquito Larva or Spooky Mushrooms
				index = 1;
				break;
			case 2:	// Tree-holed coin -> Spooky Temple Map
				index = 4;
				break;
			case 3:	// Meet Vampire
				index = 6;
				break;
			}
			break;
		case 3:
			switch ( Preferences.getInteger( "choiceAdventure506" ) )
			{
			case 1:	// Forest Corpses
				index = Preferences.getInteger( "choiceAdventure26" );
				index = index * 2 + Preferences.getInteger( "choiceAdventure" + ( 26 + index ) ) - 3;
				index += 8;
				break;
			case 2:	// Spooky-Gro Fertilizer
				index = 2;
				break;
			case 3:	// Spooky Temple Map
				index = 4;
				break;
			}
		}

		this.spookyForestSelect.setSelectedIndex( index < 0 || index > 13 ? 0 : index );

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

		// Figure out what to do at the maidens
		// necessary for backwards-compatibility

		index = Preferences.getInteger( "choiceAdventure89" );
		if ( index == 6 )
		{
			this.maidenSelect.setSelectedIndex( 0 );
		}
		else
		{
			this.maidenSelect.setSelectedIndex( index + 1 );
		}

		// OceanDestinationComboBox handles its own settings.
		this.oceanDestSelect.loadSettings();

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

		this.isAdjusting = false;
		ActionPanel.enableActions( true );
	}

	public static class CommandButton
		extends JButton
		implements ActionListener
	{
		public CommandButton( String cmd )
		{
			super( cmd );

			this.setHorizontalAlignment( SwingConstants.LEFT );

			this.setActionCommand( cmd );
			this.addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			CommandDisplayFrame.executeCommand( e.getActionCommand() );
		}
	}
}
