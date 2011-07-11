/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.widget.DisabledItemsComboBox;

public class DailyDeedsPanel
	extends Box
	implements PreferenceListener
{
	public static final AdventureResult GREAT_PANTS = ItemPool.get( ItemPool.GREAT_PANTS, 1 );
	public static final AdventureResult INFERNAL_SEAL_CLAW = ItemPool.get( ItemPool.INFERNAL_SEAL_CLAW, 1 );
	public static final AdventureResult NAVEL_RING = ItemPool.get( ItemPool.NAVEL_RING, 1 );

	/*
	 * Built-in deeds. {Type, Name, ...otherArgs}
	 * Type: one of { BooleanPref, BooleanItem, Skill, Special }
	 *
	 * NOTE: when adding a new built-in deed, also add an appropriate entry for it in getVersion and increment dailyDeedsVersion
	 * in defaults.txt.
	 */
	public static final String[][] BUILTIN_DEEDS =
	{
		{
			"BooleanPref", "Breakfast", "breakfastCompleted", "breakfast"
		},
		{
			"BooleanPref", "Daily Dungeon", "dailyDungeonDone", "adv * Daily Dungeon"
		},
		{
			"Special", "Submit Spading Data",
		},
		{
			"Special", "Crimbo Tree",
		},
		{
			"Special", "Chips",
		},
		{
			"BooleanItem", "Library Card", "libraryCardUsed"
		},
		{
			"Special", "Telescope",
		},
		{
			"Special", "Ball Pit",
		},
		{
			"Special", "Styx Pixie",
		},
		{
			"Special", "VIP Pool",
		},
		{
			"Special", "April Shower",
		},
		{
			"BooleanItem", "Bag o' Tricks", "_bagOTricksUsed"
		},
		{
			"BooleanItem", "Legendary Beat", "_legendaryBeat"
		},
		{
			"BooleanItem", "Outrageous Sombrero", "outrageousSombreroUsed"
		},
		{
			"Special", "Friars",
		},
		{
			"Special", "Skate Park",
		},
		{
			"Special", "Concert",
		},
		{
			"Special", "Demon Summoning",
		},
		{
			"Skill", "Rage Gland", "rageGlandVented"
		},
		{
			"Special", "Free Rests",
		},
		{
			"Special", "Hot Tub",
		},
		{
			"Special", "Nuns",
		},
		{
			"BooleanItem", "Oscus' Soda", "oscusSodaUsed"
		},
		{
			"BooleanItem", "Express Card", "expressCardUsed"
		},
		{
			"Special", "Flush Mojo",
		},
		{
			"Special", "Pudding",
		},
		{
			"Special", "Melange",
		},
		{
			"Special", "Stills",
		},
		{
			"Special", "Tea Party",
		},
		{
			"Special", "Photocopy",
		},
		{
			"Special", "Putty",
		},
		{
			"Special", "Camera",
		},
		{
			"Special", "Bonus Adventures",
		},
		{
			"Special", "Familiar Drops",
		},
		{
			"Special", "Free Fights",
		},
		{
			"Special", "Free Runaways",
		}
	};

	private static final int getVersion( String deed )
	{
		// Add a method to return the proper version for the deed given.
		// i.e. if( deed.equals( "Breakfast" ) ) return 1;
		return 0;
	};

	public DailyDeedsPanel()
	{
		super( BoxLayout.Y_AXIS );

		int currentVersion = Preferences.getInteger( "dailyDeedsVersion" ) ;
		Preferences.resetToDefault( "dailyDeedsVersion" );
		int releaseVersion = Preferences.getInteger( "dailyDeedsVersion" );

		//Version handling: if our version is older than the one in defaults.txt,
		//add deeds with newer version numbers to the end of dailyDeedsOptions.

		if ( currentVersion < releaseVersion )
		{
			for ( int i = 0; i < BUILTIN_DEEDS.length; ++i )
			{
				if ( getVersion( BUILTIN_DEEDS[ i ][ 1 ] ) > currentVersion )
				{
					String oldString = Preferences.getString( "dailyDeedsOptions" );
					Preferences.setString( "dailyDeedsOptions", oldString + "," + BUILTIN_DEEDS[ i ][ 1 ] );
					RequestLogger.printLine( "New deed found.  Adding " + BUILTIN_DEEDS[ i ][ 1 ] + " to the end of your deeds panel." );
				}
			}
			RequestLogger.printLine( "Deeds updated.  Now version " + releaseVersion + "." );
		}

		this.populate();
		PreferenceListenerRegistry.registerListener( "dailyDeedsOptions", this );
	}

	private void populate()
	{
		// If we're not logged in, don't populate daily deeds.
		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
		{
			return;
		}

		String[][] fullDeedsList = DailyDeedsPanel.BUILTIN_DEEDS;
		String deedsString = Preferences.getString( "dailyDeedsOptions" );
		// REGEX: splits deedsString by commas that are NOT immediately followed by a pipe.
		// This is necessary to allow commas in custom text deeds.
		String[] pieces = deedsString.split( ",(?!\\|)" );

		// The i loop iterates over all of the elements in the dailyDeedsOptions preference.
		for ( int i = 0; i < pieces.length; ++i )
		{
			/*
			 * The j loop iterates down the full list of deeds. Once it finds the deed in question, it
			 * checks what kind of deed we're handling. Currently there is generalized handling for BooleanPref,
			 * BooleanItem, Multipref, Skill, and Text types; all the other built-ins are marked as Special and require
			 * their own function in dailyDeedsPanel to handle.
			 */
			for ( int j = 0; j < fullDeedsList.length; ++j )
			{
				/*
				 * Built-in handling
				 */
				if ( pieces[ i ].equals( fullDeedsList[ j ][ 1 ] ) )
				{
					/*
					 * Generalized handling
					 */
					if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "BooleanPref" ) )
					{
						ParseBooleanDeed( fullDeedsList[ j ] );
						break;
					}
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "BooleanItem" ) )
					{
						ParseBooleanItemDeed( fullDeedsList[ j ] );
						break;
					}
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Skill" ) )
					{
						ParseSkillDeed( fullDeedsList[ j ] );
						break;
					}

					/*
					 * Special Handling
					 */
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Special" ) )
					{
						ParseSpecialDeed( fullDeedsList[ j ] );
						break;
					}

					// we'll only get here if an unknown deed type was set in BUILTIN_DEEDS.
					// Shouldn't happen.

					RequestLogger.printLine( "Unknown deed type: " + fullDeedsList[ j ][ 0 ] );
					break;
				}
			}
			/*
			 * Custom handling
			 */
			if ( pieces[ i ].split( "\\|" )[ 0 ].equals( "$CUSTOM" )
					&& pieces[ i ].split( "\\|" ).length > 1 )
			{
				String cString = pieces[ i ].substring( 8 );//remove $CUSTOM|
				String[] customPieces = cString.split( "\\|" );

				if ( customPieces[ 0 ].equalsIgnoreCase( "BooleanPref" ) )
				{
					ParseBooleanDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "MultiPref" ) )
				{
					ParseMultiDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "BooleanItem" ) )
				{
					ParseBooleanItemDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Skill" ) )
				{
					ParseSkillDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Text" ) )
				{
					ParseTextDeed( customPieces );
				}
			}
		}
	}

	private void ParseMultiDeed( String[] deedsString )
	{
		if ( deedsString.length != 5 )
		{
			RequestLogger.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type MultiPref. (5)" );
			return;
		}

		/*
		 * MultiPref|displayText|preference|command|maxPref
		 */

		String pref = deedsString[ 2 ];

		if ( !KoLCharacter.baseUserName().equals( "GLOBAL" ) && Preferences.getString( pref ).equals( "" ) )
		{
			RequestLogger.printLine( "Daily Deeds error: couldn't resolve preference " + pref );
			return;
		}

		String displayText = deedsString[ 1 ];
		String command = deedsString[ 3 ];
		try
		{
			int maxPref = Integer.parseInt( deedsString[ 4 ] );

			this.add( new MultiPrefDaily( displayText, pref, command, maxPref ) );
		}
		catch ( NumberFormatException e )
		{
			RequestLogger.printLine( "Daily Deeds error: five-parameter 'Multi' deeds require an int for the fifth parameter." );
		}
	}

	private void ParseTextDeed( String[] deedsString )
	{
		// No error handling here, really.  0-length strings don't do anything;
		// blank strings end up working like a \n

		this.add( new TextDeed( deedsString ) );
	}

	private void ParseBooleanDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 4 )
		{
			RequestLogger.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Boolean. (3 or 4)" );
			return;
		}

		String pref = deedsString[ 2 ];

		if ( !KoLCharacter.baseUserName().equals( "GLOBAL" ) && Preferences.getString( pref ).equals( "" ) )
		{
			RequestLogger.printLine( "Daily Deeds error: couldn't resolve preference " + pref );
			return;
		}

		if ( deedsString.length == 3 )
		{
			/*
			 * BooleanPref|displayText|preference
			 * command is the same as displayText
			 */
			// Use the display text for the command if none was specified
			String command = deedsString[ 1 ];

			this.add( new BooleanDaily( pref, command ) );
		}
		else if ( deedsString.length == 4 )
		{
			/*
			 * BooleanPref|displayText|preference|command
			 */
			String displayText = deedsString[ 1 ];
			String command = deedsString[ 3 ];

			this.add( new BooleanDaily( displayText, pref, command ) );
		}
	}

	private void ParseBooleanItemDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 4 )
		{
			RequestLogger.printLine( "You did not pass the proper number of parameters for a deed of type BooleanItem. (3 or 4)" );
			return;
		}

		String pref = deedsString[ 2 ];

		if ( !KoLCharacter.baseUserName().equals( "GLOBAL" ) && Preferences.getString( pref ).equals( "" ) )
		{
			RequestLogger.printLine( "Daily Deeds error: couldn't resolve preference " + pref );
			return;
		}

		if ( deedsString.length == 3 )
		{
			/*
			 * BooleanItem|displayText|preference
			 * itemId is found from displayText
			 */
			int itemId = ItemDatabase.getItemId( deedsString[ 1 ] );
			String item = ItemDatabase.getItemName( itemId );

			if ( itemId == -1 )
 			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 1 ] );
				return;
 			}

			this.add( new BooleanItemDaily( pref, itemId, "use " + item ) );
 		}

		else if ( deedsString.length == 4 )
		{
			/*
			 * BooleanItem|displayText|preference|itemName
			 * itemId is found from itemName
			 */
			String displayText = deedsString[ 1 ];
			// Use the substring matching of getItemId because itemName may not
			// be the canonical name of the item
			int itemId = ItemDatabase.getItemId( deedsString[ 3 ] );
			String item = ItemDatabase.getItemName( itemId );

			if ( itemId == -1 )
			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 3 ] );
				return;
			}

			this.add( new BooleanItemDaily( displayText, pref, itemId, "use " + item ) );
		}
 	}

	private void ParseSkillDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 5 )
		{
			RequestLogger.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Skill. (3, 4, or 5)" );
			return;
		}

		String pref = deedsString[ 2 ];
		String resolvedPref = Preferences.getString( pref );

		if ( !KoLCharacter.baseUserName().equals( "GLOBAL" ) && resolvedPref.equals( "" ) )
		{
			RequestLogger.printLine( "Daily Deeds error: couldn't resolve preference " + pref );
			return;
		}

		if ( deedsString.length == 3 )
		{
			/*
			 * Skill|displayText|preference
			 * skillName is found from displayText
			 */
			String skillName = SkillDatabase.getSkillName( deedsString[ 1 ] );

			if ( skillName == null )
			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
						+ deedsString[ 1 ] );
				return;
			}

			if ( resolvedPref.equalsIgnoreCase( "True" ) || resolvedPref.equalsIgnoreCase( "False" ) )
			{
				this.add( new BooleanSkillDaily( pref, skillName, "cast " + skillName ) );
			}
			else
			{
				RequestLogger.printLine( "Daily Deeds error: three-parameter 'Skill' deeds can only handle boolean preferences." );
			}
		}
		else if ( deedsString.length == 4 )
		{
			/*
			 * Skill|displayText|preference|skillName
			 */
			String displayText = deedsString[ 1 ];
			String skillName = SkillDatabase.getSkillName( deedsString[ 3 ] );

			if ( skillName == null )
			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
						+ deedsString[ 3 ] );
				return;
			}
			if ( resolvedPref.equalsIgnoreCase( "True" ) || resolvedPref.equalsIgnoreCase( "False" ) )
			{
				this.add( new BooleanSkillDaily( displayText, pref, skillName, "cast " + skillName ) );
			}
			else
			{
				RequestLogger.printLine( "Daily Deeds error: four-parameter 'Skill' deeds can only handle boolean preferences." );
			}
		}
		else if ( deedsString.length == 5 )
		{
			/*
			 * Skill|displayText|preference|skillName|maxCasts
			 */
			String displayText = deedsString[ 1 ];
			String skillName = SkillDatabase.getSkillName( deedsString[ 3 ] );
			try
			{
				int maxCasts = Integer.parseInt( deedsString[ 4 ] );

				if ( skillName == null )
				{
					RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
							+ deedsString[ 3 ] );
					return;
				}
				this.add( new MultiSkillDaily( displayText, pref, skillName, "cast " + skillName,
						maxCasts ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger.printLine( "Daily Deeds error: five-parameter 'Skill' deeds require an int for the fifth parameter." );
			}
		}
	}
	private void ParseSpecialDeed( String[] deedsString )
	{
		if ( deedsString[ 1 ].equals( "Submit Spading Data" ) )
		{
			this.add( new SpadeDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Crimbo Tree" ) )
		{
			this.add( new CrimboTreeDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Chips" ) )
		{
			this.add( new ChipsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Telescope" ) )
		{
			this.add( new TelescopeDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Ball Pit" ) )
		{
			this.add( new PitDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Styx Pixie" ) )
		{
			this.add( new StyxDaily() );
		}
		else if ( deedsString[ 1 ].equals( "VIP Pool" ) )
		{
			this.add( new PoolDaily() );
		}
		else if ( deedsString[ 1 ].equals( "April Shower" ) )
		{
			this.add( new ShowerCombo() );
		}
		else if ( deedsString[ 1 ].equals( "Friars" ) )
		{
			this.add( new FriarsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Skate Park" ) )
		{
			this.add( new SkateDaily( "lutz", "ice", "_skateBuff1", "Fishy" ) );
			this.add( new SkateDaily( "comet", "roller", "_skateBuff2", "-30% to Sea penalties" ) );
			this.add( new SkateDaily( "band shell", "peace", "_skateBuff3", "+sand dollars" ) );
			this.add( new SkateDaily( "eels", "peace", "_skateBuff4", "+10 lbs. underwater" ) );
			this.add( new SkateDaily( "merry-go-round", "peace", "_skateBuff5", "+25% items underwater" ) );

		}
		else if ( deedsString[ 1 ].equals( "Concert" ) )
		{
			this.add( new ConcertDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Demon Summoning" ) )
		{
			this.add( new DemonCombo() );
		}
		else if ( deedsString[ 1 ].equals( "Free Rests" ) )
		{
			this.add( new RestsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Hot Tub" ) )
		{
			this.add( new HotTubDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Nuns" ) )
		{
			this.add( new NunsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Flush Mojo" ) )
		{
			this.add( new MojoDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Pudding" ) )
		{
			this.add( new PuddingDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Melange" ) )
		{
			this.add( new MelangeDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Stills" ) )
		{
			this.add( new StillsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Tea Party" ) )
		{
			this.add( new TeaPartyDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Photocopy" ) )
		{
			this.add( new PhotocopyDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Putty" ) )
		{
			this.add( new PuttyDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Camera" ) )
		{
			this.add( new CameraDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Bonus Adventures" ) )
		{
			this.add( new AdvsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Familiar Drops" ) )
		{
			this.add( new DropsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Free Fights" ) )
		{
			this.add( new FreeFightsDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Free Runaways" ) )
		{
			this.add( new RunawaysDaily() );
		}
		else
		// you added a special deed to BUILTIN_DEEDS but didn't add a method call.
		{
			RequestLogger.printLine( "Couldn't match a deed: " + deedsString[ 1 ]
					+ " does not have a built-in method." );
		}
	}

	public void update()
	{
		// Called whenever the dailyDeedsOptions preference is changed.
		this.removeAll();
		this.populate();
		this.revalidate();
		this.repaint();
	}

	public void add( Daily daily )
	{
		daily.add( Box.createHorizontalGlue() );
		daily.initialUpdate();
		super.add( daily );
	}

	public abstract static class Daily
		extends Box
		implements ActionListener, PreferenceListener
	{
		private ArrayList buttons;
		private JLabel label;

		public Daily()
		{
			super( BoxLayout.X_AXIS );
		}

		public void addListener( String preference )
		{
			PreferenceListenerRegistry.registerListener( preference, this );
		}

		public void addItem( int itemId )
		{
			InventoryManager.registerListener( itemId, this );
		}

		public JButton addButton( String command )
		{
			JButton button = new JButton( command );
			button.setActionCommand( command );
			button.addActionListener( this );
			button.setBackground( this.getBackground() );
			button.setDefaultCapable( false );
			button.putClientProperty( "JButton.buttonType", "segmented" );
			if ( this.buttons == null )
			{
				this.buttons = new ArrayList();
				button.putClientProperty( "JButton.segmentPosition", "only" );
			}
			else
			{
				button.putClientProperty( "JButton.segmentPosition", "last" );
				int last = this.buttons.size() - 1;
				((JButton) this.buttons.get( last )).putClientProperty(
					"JButton.segmentPosition", last == 0 ? "first" : "middle" );
			}
			this.buttons.add( button );
			this.add( button );
			return button;
		}

		public void addButton( String command, String tip )
		{
			this.addButton( command ).setToolTipText( tip );
		}

		public JButton addComboButton( String command, String displaytext )
		{
			JButton button = new JButton( command );
			button.setActionCommand( command );
			button.setText( displaytext );
			button.addActionListener( this );
			button.setBackground( this.getBackground() );
			button.setDefaultCapable( false );
			button.putClientProperty( "JButton.buttonType", "segmented" );
			if ( this.buttons == null )
			{
				this.buttons = new ArrayList();
				button.putClientProperty( "JButton.segmentPosition", "only" );
			}
			else
			{
				button.putClientProperty( "JButton.segmentPosition", "last" );
				int last = this.buttons.size() - 1;
				((JButton) this.buttons.get( last )).putClientProperty(
					"JButton.segmentPosition", last == 0 ? "first" : "middle" );
			}
			this.buttons.add( button );
			this.add( button );
			return button;
		}

		public DisabledItemsComboBox addComboBox( Object choice[], ArrayList tooltips, String lengthString )
		{
			DisabledItemsComboBox comboBox = new DisabledItemsComboBox();
			int ht = comboBox.getFontMetrics(comboBox.getFont()).getHeight() ;
			int len = comboBox.getFontMetrics(comboBox.getFont()).stringWidth( lengthString );

			// pseudo magic numbers here, but maximumsize will likely never
			// be looked at by the layout manager. If  maxsize is not set,
			// the layout manager isn't happy.
			// The combobox is ultimately sized by setPrototypeDisplayValue().

			comboBox.setMaximumSize( new Dimension( (int)Math.round( len + 100 ), (int)Math.round( ht * 1.5 ) ) );
			comboBox.setPrototypeDisplayValue( (Object)lengthString );

			for ( int i = 0; i < choice.length ; ++i )
			{
				comboBox.addItem( choice[i]);
			}

			comboBox.setTooltips( tooltips );
			this.add( comboBox );
			return comboBox;
		}

		public void setComboTarget( JButton b, String act )
		{
			b.setActionCommand(act);
		}

		public JButton buttonText( int idx, String command )
		{
			JButton button = (JButton) this.buttons.get( idx );
			button.setText( command );
			button.setActionCommand( command );
			return button;
		}

		public void buttonText( int idx, String command, String tip )
		{
			this.buttonText( idx, command ).setToolTipText( tip );
		}

		public void addLabel( String text )
		{
			this.label = new JLabel( text );
			this.add( this.label );
		}

		public void setText( String text )
		{
			this.label.setText( text );
		}

		public void setEnabled( boolean enabled )
		{
			Iterator i = this.buttons.iterator();
			while ( i.hasNext() )
			{
				((JButton) i.next()).setEnabled( enabled );
			}
		}

		public void setEnabled( int index, boolean enabled )
		{
			((JButton) this.buttons.get( index )).setEnabled( enabled );
		}

		public void setShown( boolean shown )
		{
			if ( shown != this.isVisible() )
			{
				this.setVisible( shown );
				this.revalidate();
			}
		}

		public void actionPerformed( ActionEvent e )
		{
			CommandDisplayFrame.executeCommand( e.getActionCommand() );
			// Try to avoid having a random button, possibly with a high associated
			// cost, set as the default button when this one is disabled.
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
		}

		public void initialUpdate()
		{
			this.update();
		}

		public void update()
		{
		}
	}

	public static class ShowerCombo
		extends Daily
	{
		// We don't really need the ability to disable items within
		// the shower combo box, but it's implemented here for consistency

		DisabledItemsComboBox box = new DisabledItemsComboBox();
		JButton btn;

		public ShowerCombo()
		{
			ArrayList ttips = new ArrayList();
			Object[] choices = {
				"April Shower",
				"Muscle",
				"Mysticality",
				"Moxie",
				"Ice",
				"MP"
			};
			Object[] tips = {
				"Take a shower",
				"+5% to all Muscle Gains, 50 turns",
				"+5% to all Mysticality Gains, 50 turns",
				"+5% to all Moxie Gains, 50 turns",
				"shards of double-ice",
				"mp or amazing idea"
			};

			ttips.addAll(Arrays.asList(tips));

			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_aprilShower" );
			this.addListener( "kingLiberated" );

			//the string is used to set the combobox width. pick the largest, add a space
			box = this.addComboBox( choices, ttips, "April Shower " );
			box.addActionListener(new ShowerComboListener() );
			this.add( Box.createRigidArea(new Dimension(5,1) ) );//small 5px spacer

			btn = this.addComboButton( "" , "Go!"); //initialize GO button to do nothing
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean as = Preferences.getBoolean( "_aprilShower" );
			this.setShown( ( !bm || kf ) && ( have || as ) );
			this.setEnabled( !as );
			box.setEnabled( !as );
		}

		//can probably generalize these combo listeners and put them somewhere else.
		//for now they're individual to each combo.
		private class ShowerComboListener
			implements ActionListener
			//the combo listeners exist solely to update the GO button with
			//the combo box target
		{
			public void actionPerformed( final ActionEvent e )
			{
				DisabledItemsComboBox cb = (DisabledItemsComboBox)e.getSource();
				if ( cb.getSelectedIndex() == 0)
				{
					setComboTarget( btn, "" );
				}
				else
				{
					String Choice = cb.getSelectedItem().toString();

					if ( Choice != null )
					{
						setComboTarget(btn, "shower " + Choice );
					}
				}
			}
		}
	}

	public static class DemonCombo
		extends Daily
	{
		DisabledItemsComboBox box = new DisabledItemsComboBox();
		JButton btn = null;

		public DemonCombo()
		{
			int len = KoLAdventure.DEMON_TYPES.length;
			ArrayList ttips = new ArrayList();
			Object[] choices = new String[ len + 1 ];
			choices[0] = "Summoning Chamber";
			Object[] tips = {
				"Summon a demon",
				"Yum!",
				"+100% meat, 30 turns",
				"+5-16 HP/MP, 30 turns",
				"+20 hot damage, +5 DR, 30 turns",
				"+30 stench damage, 30 turns",
				null,
				"Booze!",
				"why ARE you here?",
				"+80-100 hot damage, 30 turns",
				"stat boost, 30 turns"
			};

			for ( int i=1; i <= len ; ++i )
			{
				this.addListener( "demonName" + i );
				choices[i] =  (String)KoLAdventure.DEMON_TYPES[ i - 1 ][ 1 ] ;
			}

			ttips.addAll(Arrays.asList(tips));

			this.addListener( "(character)" );
			this.addListener( "demonSummoned" );

			this.addItem( ItemPool.EYE_OF_ED );
			this.addItem( ItemPool.HEADPIECE_OF_ED );
			this.addItem( ItemPool.STAFF_OF_ED );

			box = this.addComboBox( choices, ttips, "Summoning Chamber " );
			box.addActionListener(new DemonComboListener() );
			this.add( Box.createRigidArea(new Dimension(5,1) ) );

			// Initialize the GO button to do nothing.
			btn = this.addComboButton( "", "Go!");
		}

		public void update()
		{
			boolean summoned = Preferences.getBoolean( "demonSummoned" );
			boolean have = InventoryManager.getCount( ItemPool.EYE_OF_ED ) > 0
				|| InventoryManager.getCount( ItemPool.HEADPIECE_OF_ED ) > 0
				|| InventoryManager.getCount( ItemPool.STAFF_OF_ED ) > 0;
			this.setShown( have );
			this.setEnabled( !summoned );
			box.setEnabled( !summoned ); // this.setEnabled will not disable the combo box, for whatever reason

			// Disable individual choices if we don't have the demon names
			// Don't touch the first list element
			for ( int i = 1; i <= KoLAdventure.DEMON_TYPES.length ; ++i )
			{
				box.setDisabledIndex( i, Preferences.getString( "demonName" + i ).equals( "" ) );
			}
		}

		private class DemonComboListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				DisabledItemsComboBox cb = (DisabledItemsComboBox)e.getSource();
				if ( cb.getSelectedIndex() == 0 )
				{
					setComboTarget(btn, "");
				}
				else
				{
					String Choice = (String)cb.getSelectedItem().toString();

					if ( Choice != null )
					{
						setComboTarget(btn, "summon " + Choice);
					}
				}
			}
		}
	}

	public static class BooleanDaily
		extends Daily
	{
		String preference;
		private ArrayList buttons;

		/**
		 * @param preference
		 *	the preference to look at. The preference is used to set the availability of the
		 *	element.
		 * @param command
		 *	the command to execute. This will also be the displayed button text.
		 */

		public BooleanDaily( String preference, String command )
		{
			this.preference = preference;
			this.addListener( preference );
			this.addButton( command );
		}

		/**
		 * @param displayText
		 *	the text that will be displayed on the button
		 * @param preference
		 *	the preference to look at. The preference is used to set the availability of the
		 *	element.
		 * @param command
		 *	the command to execute.
		 */

		public BooleanDaily( String displayText, String preference, String command )
		{
			this.preference = preference;
			this.addListener( preference );
			this.addComboButton( command, displayText );
		}

		public void update()
		{
			this.setEnabled( !Preferences.getBoolean( this.preference ) );
		}
	}

	public static class BooleanItemDaily
		extends Daily
	{
		String preference;
		int itemId;

		/**
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param itemId
		 * 	the ID of the item. the item is used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public BooleanItemDaily( String preference, int itemId, String command )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.addItem( itemId );
			this.addListener( preference );
			this.addButton( command );
		}

		/**
		 * @param displayText
		 * 	the text that will be displayed on the button
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param itemId
		 * 	the ID of the item. the item is used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public BooleanItemDaily( String displayText, String preference, int itemId, String command )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.addItem( itemId );
			this.addListener( preference );
			this.addComboButton( command, displayText );
		}

		public void update()
		{
			boolean pref = Preferences.getBoolean( this.preference );
			this.setShown( pref || InventoryManager.getCount( this.itemId ) > 0 );
			this.setEnabled( !pref );
		}
	}

	public static class BooleanSkillDaily
		extends Daily
	{
		String preference;
		String skill;

		/**
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param skill
		 * 	the skill used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public BooleanSkillDaily( String preference, String skill, String command )
		{
			this.preference = preference;
			this.skill = skill;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addButton( command );
		}

		/**
		 * @param displayText
		 * 	the text that will be displayed on the button
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param skill
		 * 	the skill used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public BooleanSkillDaily( String displayText, String preference, String skill, String command )
		{
			this.preference = preference;
			this.skill = skill;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText );
		}

		public void update()
		{
			boolean pref = Preferences.getBoolean( this.preference );
			this.setShown( KoLCharacter.hasSkill( this.skill ) );
			this.setEnabled( !pref );
		}
	}

	public static class MultiPrefDaily
		extends Daily
	{
		String preference;
		int maxPref;

		/**
		 * @param preference
		 *                the preference to look at. The preference is used to set the availability of the
		 *                element.
		 * @param skill
		 *                the skill used to set the visibility of the element.
		 * @param command
		 *                the command to execute.
		 * @param maxPref
		 *                the number of skill uses before the button is disabled.
		 */

		public MultiPrefDaily( String displayText, String preference, String command, int maxPref )
		{
			this.preference = preference;
			this.maxPref = maxPref;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText );
			this.addLabel( "" );
		}

		public void update()
		{
			int uses = Preferences.getInteger( this.preference );
			this.setShown( true );
			this.setEnabled( uses < this.maxPref );
			this.setText( uses + "/" + this.maxPref );
		}
	}

	public static class MultiSkillDaily extends Daily
	{
		String preference;
		String skill;
		int maxCasts;

		/**
		 * @param preference
		 *                the preference to look at. The preference is used to set the availability of the
		 *                element.
		 * @param skill
		 *                the skill used to set the visibility of the element.
		 * @param command
		 *                the command to execute.
		 * @param maxCasts
		 *                the number of skill uses before the button is disabled.
		 */

		public MultiSkillDaily( String displayText, String preference, String skill, String command,
				int maxCasts )
		{
			this.preference = preference;
			this.skill = skill;
			this.maxCasts = maxCasts;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText );
			this.addLabel( "" );
		}

		public void update()
		{
			int casts = Preferences.getInteger( this.preference );
			this.setShown( KoLCharacter.hasSkill( this.skill ) );
			this.setEnabled( casts < this.maxCasts );
			this.setText( casts + "/" + this.maxCasts );
		}
	}
	
	public class TextDeed extends Daily
	{
		String[] deedsString;
 
		public TextDeed( String[] deedString )
		{
			for ( int i = 1; i < deedString.length; ++i )
			{
				if ( !KoLCharacter.baseUserName().equals( "GLOBAL" )
						&& !Preferences.getString( deedString[ i ] ).equals( "" ) )
				{
					this.addListener( deedString[ i ] );
				}
			}
			this.deedsString = deedString;
			this.addLabel( "" );
		}

		public void update()
		{
			String text = "";

			for ( int i = 1; i < deedsString.length; ++i )
			{
				if ( !KoLCharacter.baseUserName().equals( "GLOBAL" )
						&& !Preferences.getString( deedsString[ i ] ).equals( "" ) )
				{
					text = text + Preferences.getString( deedsString[ i ] );
				}
				else
				{
					text = text + deedsString[ i ];
				}
			}
			this.setText( text );
		}
	}

	public static class NunsDaily
		extends Daily
	{
		public NunsDaily()
		{
			this.addListener( "nunsVisits" );
			this.addListener( "sidequestNunsCompleted" );
			this.addButton( "Nuns" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nv = Preferences.getInteger( "nunsVisits" );
			boolean snc = Preferences.getString( "sidequestNunsCompleted" ).equals( "none" );
			this.setShown( !snc );
			this.setEnabled( nv < 3 && !snc );
			this.setText( nv + "/3" );
		}
	}

	public static class SkateDaily
		extends Daily
	{
		private String state, visited;

		public SkateDaily( String name, String state, String visited, String desc )
		{
			this.state = state;
			this.visited = visited;
			this.addListener( "skateParkStatus" );
			this.addListener( visited );
			this.addButton( "skate " + name );
			this.addLabel( desc );
		}

		public void update()
		{
			this.setShown( Preferences.getString( "skateParkStatus" ).equals( this.state ) );
			this.setEnabled( !Preferences.getBoolean( this.visited ) );
		}
	}

	public static class SpadeDaily
		extends Daily
	{
		public SpadeDaily()
		{
			this.addListener( "spadingData" );
			this.addButton( "spade" );
			this.addLabel( "" );
		}

		public void update()
		{
			int ns = Preferences.getString( "spadingData" ).split( "\\|" ).length / 3;
			this.setShown( ns > 0 );
			this.setText( ns == 1 ? "one item to submit" : (ns + " items to submit") );
		}
	}

	public static class TelescopeDaily
		extends Daily
	{
		public TelescopeDaily()
		{
			this.addListener( "telescopeLookedHigh" );
			this.addListener( "telescopeUpgrades" );
			this.addListener( "kingLiberated" );
			this.addButton( "telescope high" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			int nu = Preferences.getInteger( "telescopeUpgrades" );
			this.setShown( ( !bm || kf ) && ( nu > 0 ) );
			this.setEnabled( nu > 0 && !Preferences.getBoolean( "telescopeLookedHigh" ) );
			this.setText( nu == 0 ? "" : ("+" + nu*5 + "% all, 10 turns") );
		}
	}

	public static class ConcertDaily
		extends Daily
	{
		public ConcertDaily()
		{
			this.addListener( "concertVisited" );
			this.addListener( "sidequestArenaCompleted" );
			this.addButton( "concert ?" );
			this.addButton( "concert ?" );
			this.addButton( "concert ?" );
		}

		public void update()
		{
			boolean cv = Preferences.getBoolean( "concertVisited" );
			String side = Preferences.getString( "sidequestArenaCompleted" );
			if ( side.equals( "fratboy" ) )
			{
				this.setShown( true );
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Elvish", "+10% all stats, 20 turns" );
				this.buttonText( 1, "concert Winklered", "+40% meat, 20 turns" );
				this.buttonText( 2, "concert White-boy Angst", "+50% initiative, 20 turns" );
			}
			else if ( side.equals( "hippy" ) )
			{
				this.setShown( true );
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Moon'd", "+5 stats per fight, 20 turns" );
				this.buttonText( 1, "concert Dilated Pupils", "+20% items, 20 turns" );
				this.buttonText( 2, "concert Optimist Primal", "+5 lbs., 20 turns" );
			}
			else {
				this.setShown( false );
				this.setEnabled( false );
			}
		}
	}

	public static class RestsDaily
		extends Daily
	{
		public RestsDaily()
		{
			this.addListener( "timesRested" );
			this.addListener( "(skill)" );
			this.addButton( "rest" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nr = Preferences.getInteger( "timesRested" );
			int fr = 0;
			if ( KoLCharacter.hasSkill( "Disco Nap" ) ) ++fr;
			if ( KoLCharacter.hasSkill( "Disco Power Nap" ) ) fr += 2;
			if ( KoLCharacter.hasSkill( "Executive Narcolepsy" ) ) ++fr;
			this.setShown( fr > 0 );
			this.setEnabled( nr < fr );
			this.setText( nr + " (" + fr + " free)" );
		}
	}

	public static class FriarsDaily
		extends Daily
	{
		public FriarsDaily()
		{
			this.addListener( "friarsBlessingReceived" );
			this.addListener( "lastFriarCeremonyAscension" );
			this.addListener( "kingLiberated" );
			this.addListener( "(character)" );
			this.addButton( "friars food", "+30% food drops, 20 turns" );
			this.addButton( "friars familiar", "+2 familiar exp per fight, 20 turns" );
			this.addButton( "friars booze", "+30% booze drops, 20 turns" );
		}

		public void update()
		{
			boolean kf = KoLCharacter.kingLiberated();
			int lfc = Preferences.getInteger( "lastFriarCeremonyAscension" );
			int ka = Preferences.getInteger( "knownAscensions" );
			this.setShown( kf || lfc == ka );
			this.setEnabled( !Preferences.getBoolean( "friarsBlessingReceived" ) );
		}
	}

	public static class StyxDaily
		extends Daily
	{
		public StyxDaily()
		{
			this.addListener( "styxPixieVisited" );
			this.addButton( "styx muscle", "+25% musc, +10 weapon dmg, +5 DR, 10 turns" );
			this.addButton( "styx mysticality", "+25% myst, +15 spell dmg, 10-15 MP regen, 10 turns" );
			this.addButton( "styx moxie", "+25% mox, +40% meat, +20% item, 10 turns" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			this.setShown( bm );
			this.setEnabled( !Preferences.getBoolean( "styxPixieVisited" ) &&
				bm );
		}
	}

	public static class MojoDaily
		extends Daily
	{
		public MojoDaily()
		{
			this.addListener( "currentMojoFilters" );
			this.addListener( "kingLiberated" );
			this.addItem( ItemPool.MOJO_FILTER );
			this.addButton( "use mojo filter" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean have = InventoryManager.getCount( ItemPool.MOJO_FILTER ) > 0;
			int nf = Preferences.getInteger( "currentMojoFilters" );
			this.setShown( have || nf > 0 );
			this.setEnabled( have && nf < 3 );
			this.setText( nf + "/3" );
		}
	}

	public static class HotTubDaily
		extends Daily
	{
		public HotTubDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_hotTubSoaks" );
			this.addListener( "kingLiberated" );
			this.addButton( "hottub" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			int nf = Preferences.getInteger( "_hotTubSoaks" );
			this.setShown( ( !bm || kf ) && ( have || nf > 0 ) );
			this.setEnabled( nf < 5 );
			this.setText( nf + "/5" );
		}
	}

	public static class PoolDaily
		extends Daily
	{
		public PoolDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_poolGames" );
			this.addListener( "kingLiberated" );
			this.addButton( "pool 1", "weapon dmg +50%, +5 lbs, 10 turns" );
			this.addButton( "pool 2", "spell dmg +50%, 10 MP per Adv, 10 turns" );
			this.addButton( "pool 3", "init +50%, +10% item, 10 turns" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			int nf = Preferences.getInteger( "_poolGames" );
			this.setShown( ( !bm || kf ) && ( have || nf > 0 ) );
			this.setEnabled( nf < 3 );
			this.setText( nf + "/3" );
		}
	}

	public static class CrimboTreeDaily
		extends Daily
	{
		public CrimboTreeDaily()
		{
			this.addListener( "_crimboTree" );
			this.addListener( "crimboTreeDays" );
			this.addListener( "kingLiberated" );
			this.addButton( "crimbotree get" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean tree = Preferences.getBoolean( "_crimboTree" );
			int ctd = Preferences.getInteger( "crimboTreeDays" );
			this.setShown( ( !bm || kf ) && tree );
			this.setEnabled( ctd == 0 );
			this.setText( ctd + " days to go." );
		}
	}

	public static class MelangeDaily
		extends Daily
	{
		public MelangeDaily()
		{
			this.addItem( ItemPool.SPICE_MELANGE );
			this.addListener( "spiceMelangeUsed" );
			this.addLabel( "" );
		}

		public void update()
		{
			int have = InventoryManager.getCount( ItemPool.SPICE_MELANGE );
			if ( Preferences.getBoolean( "spiceMelangeUsed" ) )
			{
				this.setShown( true );
				this.setText( "SPICE MELANGE USED, have " + have );
			}
			else
			{
				this.setShown( have > 0 );
				this.setText( "spice melange not used, have " + have );
			}
		}
	}

	public static class StillsDaily
		extends Daily
	{
		public StillsDaily()
		{
			this.addListener( "(stills)" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			this.setShown( KoLCharacter.isMoxieClass() &&
				KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) );
			this.setText( (10 - KoLCharacter.getStillsAvailable()) +
				"/10 stills used" );
		}
	}

	public static class TeaPartyDaily
		extends Daily
	{
		public TeaPartyDaily()
		{
			this.addItem( ItemPool.DRINK_ME_POTION );
			this.addListener( "_madTeaParty" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			int have = InventoryManager.getCount( ItemPool.DRINK_ME_POTION );
			if ( Preferences.getBoolean( "_madTeaParty" ) )
			{
				this.setShown( !bm || kf );
				if ( have == 1 )
				{
					this.setText( "Mad Tea Party used, have " + have + " potion");
				}
				else
				{
					this.setText( "Mad Tea Party used, have " + have + " potions");
				}
			}
			else
			{
				this.setShown( have > 0 );
				if ( have == 1 )
				{
					this.setText( "Mad Tea Party not used, have " + have + " potion" );
				}
				else
				{
					this.setText( "Mad Tea Party not used, have " + have + " potions" );
				}
			}
		}
	}

	public static class FreeFightsDaily
		extends Daily
	{
		public FreeFightsDaily()
		{
			this.addListener( "_brickoFights" );
			this.addListener( "_hipsterAdv" );
			this.addListener( "_sealsSummoned" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bf = !KoLCharacter.isHardcore() ||
				(KoLCharacter.isHardcore() && KoLCharacter.hasSkill( "Summon BRICKOs" ));
			boolean hh = KoLCharacter.findFamiliar( FamiliarPool.HIPSTER ) != null ;
			boolean sc = KoLCharacter.getClassType().equals(KoLCharacter.SEAL_CLUBBER);

			this.setShown( bf || hh || sc );
			int maxSummons = 5;
			if ( KoLCharacter.hasEquipped( DailyDeedsPanel.INFERNAL_SEAL_CLAW ) ||
			     DailyDeedsPanel.INFERNAL_SEAL_CLAW.getCount( KoLConstants.inventory ) > 0 )
			{
				maxSummons = 10;
			}
			String text = "Fights: ";
			if( bf ) text = text + Preferences.getInteger( "_brickoFights" ) + "/10 BRICKO";
			if( bf && ( hh || sc ) ) text = text + ", ";
			if( hh ) text = text + Preferences.getInteger( "_hipsterAdv" ) + "/7 hipster";
			if( hh && sc ) text = text + ", ";
			if( sc ) text = text + Preferences.getInteger( "_sealsSummoned" ) + "/" + maxSummons + " seals summoned";
			this.setText( text );
		}
	}

	public static class RunawaysDaily
		extends Daily
	{
		public RunawaysDaily()
		{
//			this.addItem( ItemPool.NAVEL_RING );
			this.addListener( "_banderRunaways" );
			this.addListener( "_navelRunaways" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hb = KoLCharacter.findFamiliar( FamiliarPool.BANDER ) != null;
			boolean gp = InventoryManager.getCount( ItemPool.GREAT_PANTS ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.GREAT_PANTS );
			boolean nr = Preferences.getInteger( "_navelRunaways" ) > 0
				|| InventoryManager.getCount( ItemPool.NAVEL_RING ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.NAVEL_RING );
			this.setShown( hb || gp || nr );
			String text = "Runaways: ";
			if( hb ) text = text + Preferences.getInteger( "_banderRunaways" ) + " bandersnatch" ;
			if( hb && ( gp || nr ) ) text = text + ", ";
			if( nr && !gp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " navel ring";
			if( nr && gp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap+navel";
			if( gp && !nr ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap pants";
			this.setText( text );
		}
	}

	public static class DropsDaily
		extends Daily
	{
		public DropsDaily()
		{
			this.addListener( "_absintheDrops" );
			this.addListener( "_aguaDrops" );
			this.addListener( "_astralDrops" );
			this.addListener( "_gongDrops" );
			this.addListener( "_pieDrops" );
			this.addListener( "_piePartsCount" );
			this.addListener( "_tokenDrops" );
			this.addListener( "_transponderDrops" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hf1 = KoLCharacter.findFamiliar( FamiliarPool.PIXIE ) != null;
			boolean hf2 = KoLCharacter.findFamiliar( FamiliarPool.SANDWORM ) != null;
			boolean hf3 = KoLCharacter.findFamiliar( FamiliarPool.BADGER ) != null;
			boolean hf4 = KoLCharacter.findFamiliar( FamiliarPool.LLAMA ) != null;
			boolean hf5 = KoLCharacter.findFamiliar( FamiliarPool.GRINDER ) != null;
			boolean hf6 = KoLCharacter.findFamiliar( FamiliarPool.TRON ) != null;
			boolean hf7 = KoLCharacter.findFamiliar( FamiliarPool.ALIEN ) != null;
			this.setShown( hf1 || hf2 || hf3 || hf4 || hf5 || hf6 || hf7 );
			String text = "Drops: ";
			if( hf1 ) text = text + Preferences.getInteger( "_absintheDrops" ) + " absinthe";
			if( hf1 && ( hf2 || hf3 || hf4 || hf5 || hf6 || hf7 ) ) text = text + ", ";
			if( hf2 ) text = text + Preferences.getInteger( "_aguaDrops" ) + " agua";
			if( hf2 && ( hf3 || hf4 || hf5 || hf6 || hf7 ) ) text = text + ", ";
			if( hf3 ) text = text + Preferences.getInteger( "_astralDrops" ) + " astral";
			if( hf3 && ( hf4 || hf5 || hf6 || hf7 ) ) text = text + ", ";
			if( hf4 ) text = text + Preferences.getInteger( "_gongDrops" ) + " gong";
			if( hf4 && ( hf5  || hf6 || hf7 ) ) text = text + ", ";
			if( hf5 )
			{
				if( Preferences.getInteger( "_pieDrops" )==1 )
					text = text + Preferences.getInteger( "_pieDrops" ) + " pie (";
				else text = text + Preferences.getInteger( "_pieDrops" ) + " pies (";
				text = text + Preferences.getInteger( "_piePartsCount" ) +")";
			}
			if( hf5 && ( hf6 || hf7 ) ) text = text + ", ";
			if( hf6 ) text = text + Preferences.getInteger( "_tokenDrops" ) + " token";
			if( hf6 && hf7 ) text = text + ", ";
			if( hf7 ) text = text + Preferences.getInteger( "_transponderDrops" ) + " transponder";
			this.setText( text );
		}
	}

	public static class AdvsDaily
		extends Daily
	{
		public AdvsDaily()
		{
			// this.addItem( ItemPool.TIME_HELMET );
			// this.addItem( ItemPool.V_MASK );
			this.addListener( "_gibbererAdv" );
			this.addListener( "_hareAdv" );
			this.addListener( "_hipsterAdv" );
			this.addListener( "_riftletAdv" );
			this.addListener( "_timeHelmetAdv" );
			this.addListener( "_vmaskAdv" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hf1 = KoLCharacter.findFamiliar( FamiliarPool.GIBBERER ) != null;
			boolean hf2 = KoLCharacter.findFamiliar( FamiliarPool.HARE ) != null;
			boolean hf3 = KoLCharacter.findFamiliar( FamiliarPool.RIFTLET ) != null;
			boolean hf4 = InventoryManager.getCount( ItemPool.TIME_HELMET ) > 0
				|| Preferences.getInteger( "_timeHelmetAdv" ) > 0;
			boolean hf5 = InventoryManager.getCount( ItemPool.V_MASK ) > 0
				|| Preferences.getInteger( "_vmaskAdv" ) > 0;
			String text = "Advs: ";
			if( hf1 ) text = text + Preferences.getInteger( "_gibbererAdv" ) + " gibberer";
			if( hf1 && (hf2 || hf3 || hf4 || hf5) ) text = text + ", ";
			if( hf2 ) text = text + Preferences.getInteger( "_hareAdv" ) + " hare";
			if( hf2 && (hf3 || hf4 || hf5) ) text = text + ", ";
			if( hf3 ) text = text + Preferences.getInteger( "_riftletAdv" ) + " riftlet";
			if( hf3 && (hf4 || hf5) ) text = text + ", ";
			if( hf4 ) text = text + Preferences.getInteger( "_timeHelmetAdv" ) + " time helmet";
			if( hf4 && hf5 ) text = text + ", ";
			if( hf5 ) text = text + Preferences.getInteger( "_vmaskAdv" ) + " V mask";
			this.setShown( hf1 || hf2 || hf3 || hf4 || hf5 );
			this.setText( text );
		}
	}

	public static class PuttyDaily
		extends Daily
	{
		public PuttyDaily()
		{
			this.addListener( "spookyPuttyCopiesMade" );
			this.addListener( "spookyPuttyMonster" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean kf = KoLCharacter.kingLiberated();
			boolean hc = KoLCharacter.isHardcore();
			boolean have = InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MITRE ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_LEOTARD ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_BALL ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SHEET ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SNAKE ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MONSTER ) > 0
				|| Preferences.getInteger( "spookyPuttyCopiesMade" ) > 0;
			String text = Preferences.getInteger( "spookyPuttyCopiesMade" ) +
				"/5 putty uses";
			String monster = Preferences.getString( "spookyPuttyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setShown( ( kf || !hc ) && have );
			this.setText( text );
		}
	}

	public static class CameraDaily
		extends Daily
	{
		public CameraDaily()
		{
			this.addListener( "_cameraUsed" );
			this.addListener( "cameraMonster" );
			this.addLabel( "" );
		}

		public void update()
		{
			String text = Preferences.getBoolean( "_cameraUsed" ) ?
				"4-d camera used"
				: "4-d camera not used yet";
			String monster = Preferences.getString( "cameraMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
		}
	}

	public static class PhotocopyDaily
		extends Daily
	{
		public PhotocopyDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_photocopyUsed" );
			this.addListener( "photocopyMonster" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			String text = Preferences.getBoolean( "_photocopyUsed" ) ?
				"photocopied monster used"
				: "photocopied monster not used yet";
			String monster = Preferences.getString( "photocopyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
			this.setShown( (!bm || kf) && have );
		}
	}

	public static class PuddingDaily
		extends Daily
	{
		public PuddingDaily()
		{
			this.addListener( "blackPuddingsDefeated" );
			this.addButton( "eat black pudding" );
			this.addLabel( "" );
		}

		public void update()
		{
			int bpd = Preferences.getInteger( "blackPuddingsDefeated" );
			this.setText( bpd + " defeated!" );
			this.setShown( bpd < 240 && KoLCharacter.canEat() );
		}
	}

	public static class ChipsDaily
	extends Daily
	{
		public ChipsDaily()
		{
			this.addListener( "_chipBags" );
			this.addButton( "chips radium", "moxie +30 for 10" );
			this.addButton( "chips wintergreen", "muscle +30 for 10" );
			this.addButton( "chips ennui", "mysticality +30 for 10" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nf = Preferences.getInteger( "_chipBags" );
			this.setShown( KoLCharacter.hasClan() &&
				KoLCharacter.canInteract() );
			this.setEnabled( nf < 3 );
			this.setText( nf + "/3" );
		}
	}
	public static class PitDaily
	extends Daily
	{
		public PitDaily()
		{
			this.addListener( "_ballpit" );
			this.addListener( "kingLiberated" );
			this.addButton( "ballpit", "stat boost for 20" );
		}

		public void update()
		{
			boolean dun = Preferences.getBoolean( "_ballpit" );
			this.setShown( KoLCharacter.hasClan() &&
				KoLCharacter.canInteract());
			this.setEnabled( !dun );
		}
	}
}
