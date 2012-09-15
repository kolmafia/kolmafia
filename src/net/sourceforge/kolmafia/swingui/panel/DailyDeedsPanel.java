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

import java.awt.Dimension;
import java.awt.KeyboardFocusManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.TrendyRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;

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
	 * Type: one of { Command, Item, Skill, Special }
	 *
	 * NOTE: when adding a new built-in deed, also add an appropriate entry for it in getVersion and increment dailyDeedsVersion
	 * in defaults.txt.
	 */
	public static final String[][] BUILTIN_DEEDS =
	{
		{
			"Command", "Breakfast", "breakfastCompleted", "breakfast", "1", "Perform typical daily tasks - use 1/day items, visit 1/day locations like various clan furniture, use item creation skills, etc. Configurable in preferences."
		},
		{
			"Command", "Daily Dungeon", "dailyDungeonDone", "adv * Daily Dungeon", "1", "Adventure in Daily Dungeon"
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
			"Item", "Library Card", "libraryCardUsed", "Library Card", "1", "40-50 stat gain of one of Mus/Myst/Mox, randomly chosen"
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
			"Item", "Bag o' Tricks", "_bagOTricksUsed", "Bag o' Tricks", "1", "5 random current effects extended by 3 turns"
		},
		{
			"Item", "Legendary Beat", "_legendaryBeat", "Legendary Beat", "1", "+50% items, 20 turns"
		},
		{
			"Item", "Outrageous Sombrero", "outrageousSombreroUsed", "Outrageous Sombrero", "1", "+3% items, 5 turns"
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
			"Skill", "Rage Gland", "rageGlandVented", "Rage Gland", "1", "-10% Mus/Myst/Mox, randomly chosen, and each turn of combat do level to 2*level damage, 5 turns"
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
			"Item", "Oscus' Soda", "oscusSodaUsed", "Oscus' Soda", "1", "200-300 MP"
		},
		{
			"Item", "Express Card", "expressCardUsed", "Express Card", "1", "extends duration of all current effects by 5 turns, restores all MP, cools zapped wands"
		},
		{
			"Special", "Flush Mojo",
		},
		{
			"Special", "Feast",
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
			"Special", "Romantic Arrow",
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
		},
		{
			"Special", "Hatter"
		},
		{
			"Special", "Banished Monsters"
		},
		{
			"Special", "Swimming Pool"
		}
	};

	private static final int getVersion( String deed )
	{
		// Add a method to return the proper version for the deed given.
		// i.e. if( deed.equals( "Breakfast" ) ) return 1;

		if ( deed.equals( "Swimming Pool" ) )
			return 5;
		else if ( deed.equals( "Banished Monsters" ) )
			return 4;
		else if ( deed.equals( "Hatter" ) )
			return 3;
		else if ( deed.equals( ( "Romantic Arrow" ) ) )
			return 2;
		else if ( deed.equals( ( "Feast" ) ) )
			return 1;
		else
			return 0;
	}

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

		RequestThread.executeMethodAfterInitialization( this, "populate" );
		PreferenceListenerRegistry.registerListener( "dailyDeedsOptions", this );
	}

	public void populate()
	{
		// If we're not logged in, don't populate daily deeds.
		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
		{
			return;
		}

		int sCount = 0;

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
					if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Command" ) )
					{
						parseCommandDeed( fullDeedsList[ j ] );
						break;
					}
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Item" ) )
					{
						parseItemDeed( fullDeedsList[ j ] );
						break;
					}
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Skill" ) )
					{
						parseSkillDeed( fullDeedsList[ j ] );
						break;
					}

					/*
					 * Special Handling
					 */
					else if ( fullDeedsList[ j ][ 0 ].equalsIgnoreCase( "Special" ) )
					{
						parseSpecialDeed( fullDeedsList[ j ] );
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

				if ( customPieces[ 0 ].equalsIgnoreCase( "Command" ) )
				{
					parseCommandDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Item" ) )
				{
					parseItemDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Skill" ) )
				{
					parseSkillDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Text" ) )
				{
					parseTextDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Combo" ) )
				{
					parseComboDeed( customPieces );
				}
				else if ( customPieces[ 0 ].equalsIgnoreCase( "Simple" ) )
				{
					parseSimpleDeed( customPieces, sCount );
					++sCount;
				}
			}
		}
	}

	private void parseComboDeed( String[] deedsString )
	{
		boolean isMulti = false;
		int maxUses = 1;
		if ( deedsString.length > 3 )
		{
			if ( deedsString[3].equalsIgnoreCase( "$ITEM" ) )
			{

			}
			else
			{
				try
				{
					maxUses = Integer.parseInt( deedsString[3] );
					isMulti = true;
				}
				catch( NumberFormatException e)
				{
					//not sure what you did.  Possibly used the wrong number of arguments, or specified a non-integer max
					return;
				}
			}
		}
		if ( deedsString.length > ( isMulti ? 4 : 3 ) && ( deedsString.length - ( isMulti ? 4 : 3 ) ) % 4 != 0 )
		{
			RequestLogger.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Combo." );
			return;
		}
		/*
		 * !isMulti First 3:
		 * Combo|displayText|preference
		 * this first pref is used to enable/disable the whole combodeed.
		 *
		 * isMulti First 4:
		 * Combo|displayText|preference|maxUses
		 */

		String displayText = deedsString[ 1 ];
		String pref = deedsString[ 2 ];

		// pack up the rest of the deed into an ArrayList.
		// .get( element ) gives a string array containing { "$ITEM", displayText, preference, command }

		ArrayList<String[]> packedDeed = new ArrayList<String[]>();
		for ( int i = ( isMulti ? 4 : 3 ); i < deedsString.length; i += 4 )
		{
			if ( !deedsString[ i ].equals( "$ITEM" ) )
			{
				RequestLogger.printLine( "Each combo item must start with $ITEM, you used "
					+ deedsString[ i ] );
				return;
			}
			packedDeed.add( new String[]
			{
				deedsString[ i ], deedsString[ i + 1 ], deedsString[ i + 2 ], deedsString[ i + 3 ]
			} );
		}

		if ( isMulti )
		{
			this.add( new ComboDaily( displayText, pref, packedDeed, maxUses ) );
		}
		else
		{
			this.add( new ComboDaily( displayText, pref, packedDeed ) );
		}
	}

	private void parseTextDeed( String[] deedsString )
	{
		// No error handling here, really.  0-length strings don't do anything;
		// blank strings end up working like a \n

		this.add( new TextDeed( deedsString ) );
	}

	private void parseCommandDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 6 )
		{
			RequestLogger
				.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Command. (3, 4, 5, or 6)" );
			return;
		}

		String pref = deedsString[ 2 ];

		if ( deedsString.length == 3 )
		{
			/*
			 * BooleanPref|displayText|preference
			 * command is the same as displayText
			 */
			// Use the display text for the command if none was specified
			String command = deedsString[ 1 ];

			this.add( new CommandDaily( pref, command ) );
		}
		else if ( deedsString.length == 4 )
		{
			/*
			 * BooleanPref|displayText|preference|command
			 */
			String displayText = deedsString[ 1 ];
			String command = deedsString[ 3 ];

			this.add( new CommandDaily( displayText, pref, command ) );
		}
		else if ( deedsString.length == 5 )
		{
			/*
			 * MultiPref|displayText|preference|command|maxPref
			 */

			String displayText = deedsString[ 1 ];
			String command = deedsString[ 3 ];
			try
			{
				int maxPref = Integer.parseInt( deedsString[ 4 ] );

				this.add( new CommandDaily( displayText, pref, command, maxPref ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Command deeds require an int for the fifth parameter." );
			}
		}
		else if ( deedsString.length == 6 )
		{
			/*
			 * MultiPref|displayText|preference|command|maxPref|toolTip
			 */

			String displayText = deedsString[ 1 ];
			String command = deedsString[ 3 ];
			String toolTip = deedsString[ 5 ];
			try
			{
				int maxPref = Integer.parseInt( deedsString[ 4 ] );

				this.add( new CommandDaily( displayText, pref, command, maxPref, toolTip ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Command deeds require an int for the fifth parameter." );
			}
		}
	}

	private void parseItemDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 6 )
		{
			RequestLogger
				.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Item. (3, 4, 5, or 6)" );
			return;
		}

		String pref = deedsString[ 2 ];

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
				RequestLogger
					.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 1 ] );
				return;
			}

			this.add( new ItemDaily( pref, itemId, "use " + item ) );
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
			String split = deedsString[ 3 ].split( ";" )[ 0 ];
			int itemId = ItemDatabase.getItemId( split );
			String item = ItemDatabase.getItemName( itemId );
			if ( deedsString[ 3 ].split( ";" ).length > 1 )
			{
				for ( int i = 1; i < deedsString[ 3 ].split( ";" ).length; ++i )
				{
					item += ";" + deedsString[ 3 ].split( ";" )[ i ];
				}
			}

			if ( itemId == -1 )
			{
				RequestLogger
					.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 3 ] );
				return;
			}

			this.add( new ItemDaily( displayText, pref, itemId, "use " + item ) );
		}
		else if ( deedsString.length == 5 )
		{
			/*
			 * BooleanItem|displayText|preference|itemName|maxUses
			 * itemId is found from itemName
			 */
			String displayText = deedsString[ 1 ];
			// Use the substring matching of getItemId because itemName may not
			// be the canonical name of the item
			String split = deedsString[ 3 ].split( ";" )[ 0 ];
			int itemId = ItemDatabase.getItemId( split );
			String item = ItemDatabase.getItemName( itemId );
			if ( deedsString[ 3 ].split( ";" ).length > 1 )
			{
				for ( int i = 1; i < deedsString[ 3 ].split( ";" ).length; ++i )
				{
					item += ";" + deedsString[ 3 ].split( ";" )[ i ];
				}
			}

			if ( itemId == -1 )
			{
				RequestLogger
					.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 3 ] );
				return;
			}
			try
			{
				int maxUses = Integer.parseInt( deedsString[ 4 ] );

				this.add( new ItemDaily( displayText, pref, itemId, "use " + item, maxUses ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Item deeds require an int for the fifth parameter." );
			}
		}
		else if ( deedsString.length == 6 )
		{
			/*
			 * BooleanItem|displayText|preference|itemName|maxUses|toolTip
			 * itemId is found from itemName
			 */
			String displayText = deedsString[ 1 ];
			String toolTip = deedsString[ 5 ];
			// Use the substring matching of getItemId because itemName may not
			// be the canonical name of the item
			String split = deedsString[ 3 ].split( ";" )[ 0 ];
			int itemId = ItemDatabase.getItemId( split );
			String item = ItemDatabase.getItemName( itemId );
			if ( deedsString[ 3 ].split( ";" ).length > 1 )
			{
				for ( int i = 1; i < deedsString[ 3 ].split( ";" ).length; ++i )
				{
					item += ";" + deedsString[ 3 ].split( ";" )[ i ];
				}
			}

			if ( itemId == -1 )
			{
				RequestLogger
					.printLine( "Daily Deeds error: unable to resolve item " + deedsString[ 3 ] );
				return;
			}
			try
			{
				int maxUses = Integer.parseInt( deedsString[ 4 ] );

				this.add( new ItemDaily( displayText, pref, itemId, "use " + item, maxUses, toolTip ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Item deeds require an int for the fifth parameter." );
			}
		}
	}

	private void parseSkillDeed( String[] deedsString )
	{
		if ( deedsString.length < 3 || deedsString.length > 6 )
		{
			RequestLogger
				.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Skill. (3, 4, 5, or 6)" );
			return;
		}

		String pref = deedsString[ 2 ];

		if ( deedsString.length == 3 )
		{
			/*
			 * Skill|displayText|preference
			 * skillName is found from displayText
			 */
			List<?> skillNames = SkillDatabase.getMatchingNames( deedsString[ 1 ] );

			if ( skillNames.size() != 1 )
			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
					+ deedsString[ 1 ] );
				return;
			}

			this.add( new SkillDaily( pref, (String) skillNames.get( 0 ), "cast " + skillNames.get( 0 ) ) );
		}
		else if ( deedsString.length == 4 )
		{
			/*
			 * Skill|displayText|preference|skillName
			 */
			String displayText = deedsString[ 1 ];
			List<?> skillNames = SkillDatabase.getMatchingNames( deedsString[ 3 ] );

			if ( skillNames.size() != 1 )
			{
				RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
					+ deedsString[ 3 ] );
				return;
			}
			this.add( new SkillDaily( displayText, pref, (String) skillNames.get( 0 ), "cast "
				+ skillNames.get( 0 ) ) );
		}
		else if ( deedsString.length == 5)
		{
			String displayText = deedsString[ 1 ];
			List<?> skillNames = SkillDatabase.getMatchingNames( deedsString[ 3 ] );

			try
			{
				int maxCasts = Integer.parseInt( deedsString[ 4 ] );

				if ( skillNames.size() != 1 )
				{
					RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
						+ deedsString[ 3 ] );
					return;
				}
				this.add( new SkillDaily( displayText, pref, (String) skillNames.get( 0 ), "cast "
					+ skillNames.get( 0 ), maxCasts ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Skill deeds require an int for the fifth parameter." );
			}
		}
		else if ( deedsString.length == 6 )
		{
			String displayText = deedsString[ 1 ];
			List<?> skillNames = SkillDatabase.getMatchingNames( deedsString[ 3 ] );
			String toolTip = deedsString[ 5 ];

			try
			{
				int maxCasts = Integer.parseInt( deedsString[ 4 ] );

				if ( skillNames.size() != 1 )
				{
					RequestLogger.printLine( "Daily Deeds error: unable to resolve skill "
						+ deedsString[ 3 ] );
					return;
				}
				this.add( new SkillDaily( displayText, pref, (String) skillNames.get( 0 ), "cast "
					+ skillNames.get( 0 ), maxCasts, toolTip ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Skill deeds require an int for the fifth parameter." );
			}
		}
	}
	private void parseSimpleDeed( String[] deedsString, int sCount )
	{
		if ( deedsString.length < 2 || deedsString.length > 4 )
		{
			RequestLogger
				.printLine( "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Simple. (2, 3, or 4)" );
			return;
		}

		if ( deedsString.length == 2 )
		{
			/*
			 * Simple|displayText
			 * command is the same as displayText
			 */
			// Use the display text for the command if none was specified
			String command = deedsString[ 1 ];

			this.add( new SimpleDaily( command, sCount ) );
		}
		else if ( deedsString.length == 3 )
		{
			/*
			 * Simple|displayText|command
			 */
			String displayText = deedsString[ 1 ];
			String command = deedsString[ 2 ];

			this.add( new SimpleDaily( displayText, command, sCount ) );
		}
		else if ( deedsString.length == 4 )
		{
			/*
			 * Simple|displayText|command|maxPref
			 */

			String displayText = deedsString[ 1 ];
			String command = deedsString[ 2 ];
			try
			{
				int maxPref = Integer.parseInt( deedsString[ 3 ] );

				this.add( new SimpleDaily( displayText, command, maxPref, sCount ) );
			}
			catch ( NumberFormatException e )
			{
				RequestLogger
					.printLine( "Daily Deeds error: Simple deeds require an int for the fourth parameter." );
			}
		}
	}

	private void parseSpecialDeed( String[] deedsString )
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
		else if ( deedsString[ 1 ].equals( "Feast" ) )
		{
			this.add( new FeastDaily() );
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
		else if ( deedsString[ 1 ].equals( "Romantic Arrow" ) )
		{
			this.add( new RomanticDaily() );
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
		else if ( deedsString[ 1 ].equals( "Hatter" ) )
		{
			this.add( new HatterDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Banished Monsters" ) )
		{
			this.add( new BanishedDaily() );
		}
		else if ( deedsString[ 1 ].equals( "Swimming Pool" ) )
		{
			this.add( new SwimmingPoolDaily() );
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
		RequestThread.runInParallel( new InitialUpdateRunnable( daily ) );
		super.add( daily );
	}

	private static class InitialUpdateRunnable
		implements Runnable
	{
		private Daily daily;

		public InitialUpdateRunnable( Daily daily )
		{
			this.daily = daily;
		}

		public void run()
		{
			daily.update();
		}
	}

	public abstract static class Daily
		extends Box
		implements ActionListener, PreferenceListener
	{
		private ArrayList<JButton> buttons;
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
				this.buttons = new ArrayList<JButton>();
				button.putClientProperty( "JButton.segmentPosition", "only" );
			}
			else
			{
				button.putClientProperty( "JButton.segmentPosition", "last" );
				int last = this.buttons.size() - 1;
				this.buttons.get( last ).putClientProperty(
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
				this.buttons = new ArrayList<JButton>();
				button.putClientProperty( "JButton.segmentPosition", "only" );
			}
			else
			{
				button.putClientProperty( "JButton.segmentPosition", "last" );
				int last = this.buttons.size() - 1;
				this.buttons.get( last ).putClientProperty(
					"JButton.segmentPosition", last == 0 ? "first" : "middle" );
			}
			this.buttons.add( button );
			this.add( button );
			return button;
		}

		public void addComboButton( String command, String displaytext, String tip )
		{
			this.addComboButton( command, displaytext ).setToolTipText( tip );
		}

		public DisabledItemsComboBox addComboBox( Object choice[], ArrayList<Object> tooltips, String lengthString )
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
			JButton button = this.buttons.get( idx );
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

		@Override
		public void setEnabled( boolean enabled )
		{
			if ( this.buttons != null )
			{
				Iterator<JButton> i = this.buttons.iterator();

				while ( i.hasNext() )
				{
					JButton button = i.next();

					button.setEnabled( enabled );
				}
			}
		}

		public void setEnabled( int index, boolean enabled )
		{
			this.buttons.get( index ).setEnabled( enabled );
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

		public abstract void update();
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
			ArrayList<Object> ttips = new ArrayList<Object>();
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

		@Override
		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean as = Preferences.getBoolean( "_aprilShower" );
			boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Clan Item", "April Shower" );
			this.setShown( ( !bm || kf ) && ( have || as ) && trendy );
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
			ArrayList<Object> ttips = new ArrayList<Object>();
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
				"Why ARE you here?",
				"+80-100 hot damage, 30 turns",
				"Stat boost, 30 turns"
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

		@Override
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

	public static class ComboDaily
		extends Daily
	{
		DisabledItemsComboBox box = new DisabledItemsComboBox();
		JButton btn = null;

		ArrayList<String[]> packedDeed;
		String preference;
		int maxPref = 1;

		public ComboDaily( String displayText, String pref, ArrayList<String[]> packedDeed )
		{
			this.packedDeed = packedDeed;
			this.preference = pref;

			int len = packedDeed.size();
			ArrayList<Object> ttips = new ArrayList<Object>();
			Object[] tips = new Object[ len + 1 ];
			Object[] choices = new String[ len + 1 ];
			choices[ 0 ] = displayText;
			tips[ 0 ] = "";
			String lengthString = "ABCDEFGH";

			for ( int i = 1; i <= len; ++i )
			{
				String[] item = packedDeed.get( i - 1 );

				tips[ i ] = item[ 3 ];
				this.addListener( item[ 2 ] );
				choices[ i ] = item[ 1 ];

				if ( item[ 1 ].length() > lengthString.length() )
				{
					lengthString = item[ 1 ];
				}
			}
			ttips.addAll( Arrays.asList( tips ) );

			this.addListener( pref );
			this.box = this.addComboBox( choices, ttips, lengthString + " " );
			this.box.addActionListener( new ComboListener() );
			this.add( Box.createRigidArea( new Dimension( 5, 1 ) ) );

			btn = this.addComboButton( "", "Go!" );
		}

		public ComboDaily( String displayText, String pref, ArrayList<String[]> packedDeed, int maxUses )
		{
			this( displayText, pref, packedDeed);
			this.maxPref = maxUses;
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			int prefToInt = 1;
			String pref = Preferences.getString( this.preference );
			if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
				|| pref.equalsIgnoreCase( "" ) )
			{
				prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
			}
			else
			{
				try
				{
					prefToInt = Integer.parseInt( pref );
				}
				catch ( NumberFormatException e )
				{
				}
			}
			this.setEnabled( prefToInt < this.maxPref );
			this.box.setEnabled( prefToInt < this.maxPref );
			if ( this.maxPref > 1 )
			{
				this.setText( prefToInt + "/" + this.maxPref );
			}
			this.setShown( true );

			for ( int i = 1; i <= packedDeed.size(); ++i )
			{
				prefToInt = 1;
				String[] item = packedDeed.get( i - 1 );
				pref = Preferences.getString( item[ 2 ] );
				if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
					|| pref.equalsIgnoreCase( "" ) )
				{
					prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
				}
				else
				{
					try
					{
						prefToInt = Integer.parseInt( pref );
					}
					catch ( NumberFormatException e )
					{
					}
				}
				this.box.setDisabledIndex( i, prefToInt > 0 );
			}
		}

		private class ComboListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				DisabledItemsComboBox cb = (DisabledItemsComboBox) e.getSource();
				int choice = cb.getSelectedIndex();
				if ( choice == 0 )
				{
					setComboTarget( btn, "" );
				}
				else
				{
					String[] item = packedDeed.get( choice - 1 );
					setComboTarget( btn, item[ 3 ] );
				}
			}
		}
	}

	public static class SimpleDaily
		extends Daily
	{
		String preference;
		int maxPref = 1;

		/**
		 * @param command
		 *                the command to execute. This will also be the displayed button text.
		 * @param sCount
		 */

		public SimpleDaily( String command, int sCount )
		{
			this.preference = "_simpleDeed" + sCount;
			this.addListener( preference );
			JButton button = this.addButton( command );
			button.addActionListener( new SimpleListener( this.preference ) );
		}

		/**
		 * @param displayText
		 *                the text that will be displayed on the button
		 * @param command
		 *                the command to execute.
		 * @param sCount
		 */

		public SimpleDaily( String displayText, String command, int sCount )
		{
			this.preference = "_simpleDeed" + sCount;
			this.addListener( preference );
			JButton button = this.addComboButton( command, displayText );
			button.addActionListener( new SimpleListener( this.preference ) );
		}

		/**
		 * @param displayText
		 *                the text that will be displayed on the button
		 * @param command
		 *                the command to execute.
		 * @param maxPref
		 *                the integer at which to disable the button.
		 * @param sCount
		 */
		public SimpleDaily( String displayText, String command, int maxPref, int sCount )
		{
			this.preference = "_simpleDeed" + sCount;
			this.maxPref = maxPref;
			this.addListener( preference );
			JButton button = this.addComboButton( command, displayText );
			button.addActionListener( new SimpleListener( this.preference ) );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			int prefToInt = 1;
			String pref = Preferences.getString( this.preference );
			if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
				|| pref.equalsIgnoreCase( "" ) )
			{
				prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
			}
			else
			{
				try
				{
					prefToInt = Integer.parseInt( pref );
				}
				catch ( NumberFormatException e )
				{
				}
			}
			this.setEnabled( prefToInt < this.maxPref );
			if ( this.maxPref > 1 )
			{
				this.setText( prefToInt + "/" + this.maxPref );
			}
		}

		private class SimpleListener
			implements ActionListener
		{
			String preference;

			public SimpleListener( String pref )
			{
				this.preference = pref;
			}

			public void actionPerformed( ActionEvent arg0 )
			{
				String pref = this.preference;
				int value = Preferences.getInteger( pref );
				Preferences.setInteger( pref, ++value );
			}
		}
	}

	public static class CommandDaily
		extends Daily
	{
		String preference;
		int maxPref = 1;

		/**
		 * @param preference
		 *	the preference to look at. The preference is used to set the availability of the
		 *	element.
		 * @param command
		 *	the command to execute. This will also be the displayed button text.
		 */

		public CommandDaily( String preference, String command )
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

		public CommandDaily( String displayText, String preference, String command )
		{
			this.preference = preference;
			this.addListener( preference );
			this.addComboButton( command, displayText );
		}

		/**
		 * @param displayText
		 *	the text that will be displayed on the button
		 * @param preference
		 *	the preference to look at. The preference is used to set the availability of the
		 *	element.
		 * @param command
		 *	the command to execute.
		 * @param maxPref
		 *	the integer at which to disable the button.
		 */
		public CommandDaily( String displayText, String preference, String command, int maxPref )
		{
			this.preference = preference;
			this.maxPref = maxPref;
			this.addListener( preference );
			this.addComboButton( command, displayText );
			this.addLabel( "" );
		}

		/**
		 * @param displayText
		 *	the text that will be displayed on the button
		 * @param preference
		 *	the preference to look at. The preference is used to set the availability of the
		 *	element.
		 * @param command
		 *	the command to execute.
		 * @param maxPref
		 *	the integer at which to disable the button.
		 * @param toolTip
		 * 	tooltip to display for button on mouseover, for extended information.
		 */
		public CommandDaily( String displayText, String preference, String command, int maxPref, String toolTip )
		{
			this.preference = preference;
			this.maxPref = maxPref;
			this.addListener( preference );
			this.addComboButton( command, displayText, toolTip );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			int prefToInt = 1;
			String pref = Preferences.getString( this.preference );
			if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
				|| pref.equalsIgnoreCase( "" ) )
			{
				prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
			}
			else
			{
				try
				{
					prefToInt = Integer.parseInt( pref );
				}
				catch ( NumberFormatException e )
				{
				}
			}
			this.setEnabled( prefToInt < this.maxPref );
			if ( this.maxPref > 1 )
			{
				this.setText( prefToInt + "/" + this.maxPref );
			}
		}
	}

	public static class ItemDaily
		extends Daily
	{
		String preference;
		int itemId;
		int maxUses = 1;

		/**
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param itemId
		 * 	the ID of the item. the item is used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public ItemDaily( String preference, int itemId, String command )
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
		public ItemDaily( String displayText, String preference, int itemId, String command )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.addItem( itemId );
			this.addListener( preference );
			this.addComboButton( command, displayText );
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
		 * @param maxUses
		 * 	maximum number of uses of the item per day.
		 */
		public ItemDaily( String displayText, String preference, int itemId, String command, int maxUses )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.maxUses = maxUses;
			this.addItem( itemId );
			this.addListener( preference );
			this.addComboButton( command, displayText );
			this.addLabel( "" );
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
		 * @param maxUses
		 * 	maximum number of uses of the item per day.
		 * @param toolTip
		 * 	tooltip to display for button on mouseover, for extended information.
		 */
		public ItemDaily( String displayText, String preference, int itemId, String command, int maxUses, String toolTip )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.maxUses = maxUses;
			this.addItem( itemId );
			this.addListener( preference );
			this.addComboButton( command, displayText, toolTip );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{

			int prefToInt = 1;
			String pref = Preferences.getString( this.preference );
			boolean haveItem = InventoryManager.getCount( this.itemId ) > 0;

			if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
				|| pref.equalsIgnoreCase( "" ) )
			{
				prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
			}
			else
			{
				try
				{
					prefToInt = Integer.parseInt( pref );
				}
				catch ( NumberFormatException e )
				{
				}
			}
			this.setShown( prefToInt > 0 || haveItem );

			this.setEnabled( haveItem && prefToInt < this.maxUses );
			if ( this.maxUses > 1 )
			{
				this.setText( prefToInt + "/" + this.maxUses );
			}
		}
	}

	public static class SkillDaily
		extends Daily
	{
		String preference;
		String skill;
		int maxCasts = 1;

		/**
		 * @param preference
		 * 	the preference to look at. The preference is used to set the availability of the
		 * 	element.
		 * @param skill
		 * 	the skill used to set the visibility of the element.
		 * @param command
		 * 	the command to execute.
		 */
		public SkillDaily( String preference, String skill, String command )
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
		public SkillDaily( String displayText, String preference, String skill, String command )
		{
			this.preference = preference;
			this.skill = skill;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText );
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
		 * @param maxCasts
		 *  	the number of skill uses before the button is disabled.
		 */
		public SkillDaily( String displayText, String preference, String skill, String command, int maxCasts )
		{
			this.preference = preference;
			this.skill = skill;
			this.maxCasts = maxCasts;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText );
			this.addLabel( "" );
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
		 * @param maxCasts
		 *  	the number of skill uses before the button is disabled.
		 * @param toolTip
		 * 	tooltip to display for button on mouseover, for extended information.
		 */
		public SkillDaily( String displayText, String preference, String skill, String command, int maxCasts, String toolTip )
		{
			this.preference = preference;
			this.skill = skill;
			this.maxCasts = maxCasts;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addComboButton( command, displayText, toolTip );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			int prefToInt = 1;
			String pref = Preferences.getString( this.preference );
			if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" )
				|| pref.equalsIgnoreCase( "" ) )
			{
				prefToInt = pref.equalsIgnoreCase( "true" ) ? 1 : 0;
			}
			else
			{
				try
				{
					prefToInt = Integer.parseInt( pref );
				}
				catch ( NumberFormatException e )
				{
				}
			}
			this.setShown( KoLCharacter.hasSkill( this.skill ) );
			this.setEnabled( prefToInt < this.maxCasts );
			if ( this.maxCasts > 1 )
			{
				this.setText( prefToInt + "/" + this.maxCasts );
			}
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

		@Override
		public void update()
		{
			String text = "";

			for ( int i = 1; i < deedsString.length; ++i )
			{
				if ( !KoLCharacter.baseUserName().equals( "GLOBAL" )
						&& !Preferences.getString( deedsString[ i ] ).equals( "" ) )
				{
					text += Preferences.getString( deedsString[ i ] );
				}
				else
				{
					text += deedsString[ i ];
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

		@Override
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

		@Override
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

		@Override
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

		@Override
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

		@Override
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
			this.addListener( "kingLiberated" );
			this.addButton( "rest" );
			this.addLabel( "" );
		}

		@Override
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

		@Override
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

		@Override
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

		@Override
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

		@Override
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

		@Override
		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Clan Item", "Pool Table" );
			int nf = Preferences.getInteger( "_poolGames" );
			this.setShown( ( !bm || kf ) && ( have || nf > 0 ) && trendy );
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

		@Override
		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean tree = Preferences.getBoolean( "_crimboTree" );
			boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Clan Item", "Crimbo Tree" );
			int ctd = Preferences.getInteger( "crimboTreeDays" );
			this.setShown( ( !bm || kf ) && tree && trendy );
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

		@Override
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
			this.addListener( "lastGuildStoreOpen" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			boolean unlocked = KoLCharacter.getGuildStoreOpen();
			this.setShown( KoLCharacter.isMoxieClass() &&
				KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) && unlocked );
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

		@Override
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

		@Override
		public void update()
		{
			boolean bf = !KoLCharacter.isHardcore() ||
				(KoLCharacter.isHardcore() && KoLCharacter.hasSkill( "Summon BRICKOs" ));
			FamiliarData hipster = KoLCharacter.findFamiliar( FamiliarPool.HIPSTER );
			boolean hh = hipster != null && hipster.canEquip() ;
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

		@Override
		public void update()
		{
			FamiliarData bander = KoLCharacter.findFamiliar( FamiliarPool.BANDER );
			boolean hba = bander != null && bander.canEquip() ;
			FamiliarData boots = KoLCharacter.findFamiliar( FamiliarPool.BOOTS );
			boolean hbo = boots != null && boots.canEquip() ;
			boolean run = Preferences.getInteger( "_navelRunaways" ) > 0;
			boolean gp = InventoryManager.getCount( ItemPool.GREAT_PANTS ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.GREAT_PANTS );
			boolean nr = InventoryManager.getCount( ItemPool.NAVEL_RING ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.NAVEL_RING );
			boolean pp = InventoryManager.getCount( ItemPool.PEPPERMINT_PARASOL ) > 0;
			this.setShown( hba || hbo || gp || nr || pp );
			String text = "Runaways: ";
			if( hba && !hbo ) text = text + Preferences.getInteger( "_banderRunaways" ) + " bandersnatch" ;
			if( hba && hbo ) text = text + Preferences.getInteger( "_banderRunaways" ) + " bandersnatch+boots" ;
			if( hbo && !hba ) text = text + Preferences.getInteger( "_banderRunaways" ) + " stomping boots" ;
			if( ( hba || hbo ) && ( run || gp || nr || pp ) ) text = text + ", ";
			if( run && !nr && !gp && !pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " navel ring";
			if( nr && !gp && !pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " navel ring";
			if( nr && !gp && pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " navel+parasol";
			if( nr && gp && !pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap+navel";
			if( nr && gp && pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap+navel+parasol";
			if( !nr && gp && !pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap pants";
			if( !nr && gp && pp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap+parasol";
			if( !nr && !gp && pp  ) text = text + Preferences.getInteger( "_navelRunaways" ) + " peppermint parasol";
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
			this.addListener( "_grooseDrops" );
			this.addListener( "_kloopDrops" );
			this.addListener( "_pieDrops" );
			this.addListener( "_piePartsCount" );
			this.addListener( "_tokenDrops" );
			this.addListener( "_transponderDrops" );
			this.addListener( "_bootStomps" );
			this.addListener( "bootsCharged" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			StringBuilder buffer = new StringBuilder();
			boolean shown = false;
			int count = 0;

			buffer.append( "<html>Drops: " );

			FamiliarData pixie = KoLCharacter.findFamiliar( FamiliarPool.PIXIE );
			int absintheDrops = Preferences.getInteger( "_absintheDrops" );
			if ( ( pixie != null && pixie.canEquip() ) || absintheDrops > 0  )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( absintheDrops );
				buffer.append( " absinthe" );
				shown = true;
				count++;
			}

			FamiliarData sandworm = KoLCharacter.findFamiliar( FamiliarPool.SANDWORM );
			int aguaDrops = Preferences.getInteger( "_aguaDrops" );
			if ( (sandworm != null && sandworm.canEquip() ) || aguaDrops > 0  )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( aguaDrops );
				buffer.append( " agua" );
				shown = true;
				count++;
			}

			FamiliarData badger = KoLCharacter.findFamiliar( FamiliarPool.BADGER );
			int badgerDrops = Preferences.getInteger( "_astralDrops" );
			if ( ( badger != null && badger.canEquip() ) || badgerDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( badgerDrops );
				buffer.append( " astral" );
				shown = true;
				count++;
			}

			FamiliarData llama = KoLCharacter.findFamiliar( FamiliarPool.LLAMA );
			int gongDrops = Preferences.getInteger( "_gongDrops" );
			if ( ( llama != null && llama.canEquip() ) || gongDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( gongDrops );
				buffer.append( " gong" );
				shown = true;
				count++;
			}

			FamiliarData grinder = KoLCharacter.findFamiliar( FamiliarPool.GRINDER );
			int pieDrops = Preferences.getInteger( "_pieDrops" );

			if ( ( grinder != null && grinder.canEquip() ) || pieDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( pieDrops );
				if ( pieDrops == 1 )
				{
					buffer.append( " pie (" );
				}
				else
				{
					buffer.append( " pies (" );
				}
				buffer.append( Preferences.getString( "_piePartsCount" ) );
				buffer.append( "/" );
				int drops = Preferences.getInteger( "_pieDrops" );
				int need;
				if ( drops < 1 )
				{
					need = 5;
				}
				else
				{
					drops -= 1;
					need = 5 + ( 10 + drops ) * ( drops + 1 ) / 2;
					need = Math.min( need, 50 );
					AdventureResult item = grinder.getItem();
					if( item != null && item.getItemId() == ItemPool.MICROWAVE_STOGIE )
					{
						need -= 5;
					}
				}
				buffer.append( String.valueOf( need ) );
				buffer.append( ")" );
				shown = true;
				count++;
			}

			FamiliarData tron = KoLCharacter.findFamiliar( FamiliarPool.TRON );
			int tokenDrops = Preferences.getInteger( "_tokenDrops" );
			if ( ( tron != null && tron.canEquip() ) || tokenDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( tokenDrops );
				buffer.append( " token" );
				shown = true;
				count++;
			}

			FamiliarData alien = KoLCharacter.findFamiliar( FamiliarPool.ALIEN );
			int alienDrops = Preferences.getInteger( "_transponderDrops" );
			if ( ( alien != null && alien.canEquip() ) || alienDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( alienDrops );
				buffer.append( " transponder" );
				shown = true;
				count++;
			}

			FamiliarData boots = KoLCharacter.findFamiliar( FamiliarPool.BOOTS );
			if ( boots != null && boots.canEquip() )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( Preferences.getString( "_bootStomps" ) );
				buffer.append( " stomp" );
				if ( Preferences.getInteger( "_bootStomps" ) != 1 ) buffer.append( "s" );
				if ( Preferences.getBoolean( "bootsCharged" ) ) buffer.append( " (C)" );
				shown = true;
				count++;
			}

			FamiliarData groose = KoLCharacter.findFamiliar( FamiliarPool.GROOSE );
			int grooseDrops = Preferences.getInteger( "_grooseDrops" );
			if ( ( groose != null && groose.canEquip() ) || grooseDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( grooseDrops );
				buffer.append( " grease" );
				shown = true;
				count++;
			}

			FamiliarData kloop = KoLCharacter.findFamiliar( FamiliarPool.KLOOP );
			int kloopDrops = Preferences.getInteger( "_kloopDrops" );
			if ( ( kloop != null && kloop.canEquip() ) || kloopDrops > 0 )
			{
				if ( count >= 5 )
				{
					buffer.append( "<br>Drops: " );
					count = 0;
				}
				else if ( shown )
				{
					buffer.append( ", " );
				}
				buffer.append( kloopDrops );
				buffer.append( " folio" );
				shown = true;
				count++;
			}

			buffer.append( "</html>" );

			this.setShown( shown );
			this.setText( buffer.toString() );
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
			this.addListener( "_riftletAdv" );
			this.addListener( "_timeHelmetAdv" );
			this.addListener( "_vmaskAdv" );
			this.addListener( "_gnomeAdv" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			FamiliarData gibberer = KoLCharacter.findFamiliar( FamiliarPool.GIBBERER );
			boolean hf1 = gibberer != null && gibberer.canEquip() ;
			FamiliarData hare = KoLCharacter.findFamiliar( FamiliarPool.HARE );
			boolean hf2 = hare != null && hare.canEquip() ;
			FamiliarData riftlet = KoLCharacter.findFamiliar( FamiliarPool.RIFTLET );
			boolean hf3 = riftlet != null && riftlet.canEquip() ;
			boolean hf4 = InventoryManager.getCount( ItemPool.TIME_HELMET ) > 0
				|| Preferences.getInteger( "_timeHelmetAdv" ) > 0;
			boolean hf5 = InventoryManager.getCount( ItemPool.V_MASK ) > 0
				|| Preferences.getInteger( "_vmaskAdv" ) > 0;
			FamiliarData gnome = KoLCharacter.findFamiliar( FamiliarPool.REAGNIMATED_GNOME );
			boolean hf6 = gnome != null && gnome.canEquip() ;
			String text = "Advs: ";
			if( hf1 ) text = text + Preferences.getInteger( "_gibbererAdv" ) + " gibberer";
			if( hf1 && ( hf2 || hf3 || hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf2 ) text = text + Preferences.getInteger( "_hareAdv" ) + " hare";
			if( hf2 && ( hf3 || hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf3 ) text = text + Preferences.getInteger( "_riftletAdv" ) + " riftlet";
			if( hf3 && ( hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf4 ) text = text + Preferences.getInteger( "_timeHelmetAdv" ) + " time helmet";
			if( hf4 && ( hf5 || hf6 ) ) text = text + ", ";
			if( hf5 ) text = text + Preferences.getInteger( "_vmaskAdv" ) + " V mask";
			if( hf5 && hf6 ) text = text + ", ";
			if( hf6 ) text = text + Preferences.getInteger( "_gnomeAdv" ) + " gnome";
			this.setShown( hf1 || hf2 || hf3 || hf4 || hf5 || hf6 );
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
			this.addListener( "_raindohCopiesMade" );
			this.addListener( "rainDohMonster" );
			this.addListener( "kingLiberated" );
			this.addItem( ItemPool.SPOOKY_PUTTY_SHEET );
			this.addItem( ItemPool.RAIN_DOH_BOX );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			boolean kf = KoLCharacter.kingLiberated();
			boolean hc = KoLCharacter.isHardcore();
			boolean hadPutty = Preferences.getInteger( "spookyPuttyCopiesMade" ) > 0;
			boolean hadRainDoh = Preferences.getInteger( "_raindohCopiesMade" ) > 0;
			boolean shown = false;
			boolean havePutty = InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MITRE ) > 0
				|| InventoryManager.getEquippedCount( ItemPool.get( ItemPool.SPOOKY_PUTTY_MITRE, 1 ) ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_LEOTARD ) > 0
				|| InventoryManager.getEquippedCount( ItemPool.get( ItemPool.SPOOKY_PUTTY_LEOTARD, 1 ) ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_BALL ) > 0
				|| InventoryManager.getEquippedCount( ItemPool.get( ItemPool.SPOOKY_PUTTY_BALL, 1 ) ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SHEET ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SNAKE ) > 0
				|| InventoryManager.getEquippedCount( ItemPool.get( ItemPool.SPOOKY_PUTTY_SNAKE, 1 ) ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MONSTER ) > 0;
			boolean haveRainDoh = InventoryManager.getCount( ItemPool.RAIN_DOH_BOX ) > 0
				|| InventoryManager.getCount( ItemPool.RAIN_DOH_MONSTER ) > 0;
			String text = "";

			if ( havePutty || hadPutty )
			{
				text += Preferences.getInteger( "spookyPuttyCopiesMade" ) + "/";
				text += Math.min( 5, 6 - Preferences.getInteger( "_raindohCopiesMade" ) ) + " ";
				text += "putty uses";
				String monster = Preferences.getString( "spookyPuttyMonster" );
				if ( !monster.equals( "" ) )
				{
					text += ", now " + monster;
				}
				shown = true;
			}
			if ( haveRainDoh || hadRainDoh )
			{
				if ( shown )
					text += "; ";
				text += Preferences.getInteger( "_raindohCopiesMade" ) + "/";
				text += Math.min( 5, 6 - Preferences.getInteger( "spookyPuttyCopiesMade" ) ) + " ";
				text += "rain-doh uses";
				String monster = Preferences.getString( "rainDohMonster" );
				if ( !monster.equals( "" ) )
				{
					text += ", now " + monster;
				}
			}
			this.setShown( ( kf || !hc ) && ( hadPutty || havePutty || haveRainDoh || hadRainDoh ) );
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

		@Override
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

	public static class RomanticDaily
		extends Daily
	{
		public RomanticDaily()
		{
			this.addListener( "_badlyRomanticArrows" );
			this.addListener( "_romanticFightsLeft" );
			this.addListener( "romanticTarget" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			FamiliarData angel = KoLCharacter.findFamiliar( FamiliarPool.OBTUSE_ANGEL );
			boolean oa = angel != null && angel.canEquip() ;
			String text = Preferences.getInteger( "_badlyRomanticArrows" ) > 0 ?
				"Romantic Arrow used" :
				"Romantic Arrow not used yet";
			String monster = Preferences.getString( "romanticTarget" );
			int left = Preferences.getInteger( "_romanticFightsLeft" );
			if ( !monster.equals( "" ) && left > 0 )
			{
				text = text + ", now " + monster + " (" + left + " left)";
			}
			this.setText( text );
			this.setShown( oa );
		}
	}

	public static class PhotocopyDaily
		extends Daily
	{
		public PhotocopyDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addItem( ItemPool.PHOTOCOPIER );
			this.addItem( ItemPool.PHOTOCOPIED_MONSTER );
			this.addListener( "_photocopyUsed" );
			this.addListener( "photocopyMonster" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Clan Item", "Fax Machine" );
			boolean photo = InventoryManager.getCount( ItemPool.PHOTOCOPIER ) > 0
				|| InventoryManager.getCount( ItemPool.PHOTOCOPIED_MONSTER ) > 0
				|| Preferences.getBoolean( "_photocopyUsed" );
			String text = Preferences.getBoolean( "_photocopyUsed" ) ?
				"photocopied monster used"
				: "photocopied monster not used yet";
			String monster = Preferences.getString( "photocopyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
			this.setShown( photo || (!bm || kf) && have && trendy );
		}
	}

	public static class FeastDaily
		extends Daily
	{
		public FeastDaily()
		{
			this.addItem( ItemPool.MOVEABLE_FEAST );
			this.addListener( "_feastUsed" );
			this.addListener( "_feastedFamiliars" );
			this.addButton( "use moveable feast" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			int fu = Preferences.getInteger( "_feastUsed" );
			String list = Preferences.getString( "_feastedFamiliars" );
			boolean have = InventoryManager.getCount( ItemPool.MOVEABLE_FEAST ) > 0;
			for ( int i = 0; !have && i < KoLCharacter.getFamiliarList().size(); ++i )
			{
				FamiliarData current = (FamiliarData) KoLCharacter.getFamiliarList().get( i );
				if ( current.getItem() != null && current.getItem().getItemId() == ItemPool.MOVEABLE_FEAST )
				{
					have = true;
				}
			}
			this.buttonText( 0, "use moveable feast", fu + "/5" );
			this.setText( list );
			this.setShown( have );
			this.setEnabled( ( fu < 5 ) );
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

		@Override
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

		@Override
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

		@Override
		public void update()
		{
			boolean dun = Preferences.getBoolean( "_ballpit" );
			this.setShown( KoLCharacter.hasClan() &&
				KoLCharacter.canInteract());
			this.setEnabled( !dun );
		}
	}

	public static class HatterDaily
		extends Daily
	{
		DisabledItemsComboBox box = new DisabledItemsComboBox();
		JButton btn;

		static ArrayList<String> effectHats = new ArrayList<String>();
		ArrayList<?> effects = new ArrayList<Object>();
		ArrayList<Object> modifiers = new ArrayList<Object>();

		HatterComboListener listener = new HatterComboListener();

		public HatterDaily()
		{
			this.modifiers.add( null );

			this.addListener( "_madTeaParty" );
			this.addListener( "(hats)" );
			this.addListener( "kingLiberated" );

			this.addItem( ItemPool.DRINK_ME_POTION );
			this.addItem( ItemPool.VIP_LOUNGE_KEY );

			box = this.addComboBox( this.effects.toArray(), this.modifiers, "Available Hatter Buffs: BLAH" );
			this.add( Box.createRigidArea( new Dimension( 5, 1 ) ) );

			// Initialize the GO button to do nothing.
			btn = this.addComboButton( "", "Go!" );
			update();
		}

		@Override
		public void update()
		{
			box.removeActionListener( listener );
			this.box.removeAllItems();
			box.addActionListener( listener );

			HatterDaily.effectHats = new ArrayList<String>();
			this.modifiers = new ArrayList<Object>();
			box.addItem( "Available Hatter Buffs: " );
			HatterDaily.effectHats.add( null );
			this.modifiers.add( null );

			//build hat options here
			List<AdventureResult> hats = EquipmentManager.getEquipmentLists()[ EquipmentManager.HAT ];
			FamiliarData current = (FamiliarData) KoLCharacter.getFamiliar();

			if ( current.getItem() != null && EquipmentDatabase.isHat( current.getItem() ) )
			{
				hats.add( current.getItem() );
			}

			Object[][] hat_data = RabbitHoleManager.HAT_DATA;

			//iterate across hatter buffs (i.e. hat character-lengths) first
			if ( hats.size() > 0 )
			{
				for ( int i = 0; i < hat_data.length; ++i )
				{
					// iterate down inventory second
					for ( int j = 0; j <= hats.size(); ++j )
					{
						AdventureResult ad = hats.get( j );

						if ( ad != null && !ad.getName().equals( "(none)" ) &&
								EquipmentManager.canEquip( ad ) )
						{
							if ( ( (Integer) hat_data[ i ][ 0 ] ).intValue() == RabbitHoleManager
								.hatLength( ad.getName() ) )
							{
								HatterDaily.effectHats.add( ad.getName() );
								box.addItem( hat_data[ i ][ 1 ], false );
								modifiers.add( hat_data[ i ][ 2 ] );
								break;
							}
						}
					}
				}
			}
			box.setTooltips( modifiers );

			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = ( InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0 )
				|| ( InventoryManager.getCount( ItemPool.DRINK_ME_POTION ) > 0 );
			boolean active = KoLConstants.activeEffects.contains( EffectPool.get( Effect.DOWN_THE_RABBIT_HOLE ) );

			this.setEnabled( !Preferences.getBoolean( "_madTeaParty" ) );
			box.setEnabled( !Preferences.getBoolean( "_madTeaParty" ) );

			this.setShown( !KoLCharacter.isTrendy() && ( have || active ) && ( !bm || kf ) );

			setComboTarget(btn, "");
		}

		public static String getEffectHat( int index )
		{
			return effectHats.get( index );
		}

		private class HatterComboListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				DisabledItemsComboBox cb = (DisabledItemsComboBox) e.getSource();

				if ( cb.getItemCount() == 0 )
				{
					return;
				}

				if ( cb.getSelectedIndex() == 0 )
				{
					setComboTarget( btn, "" );
				}
				else
				{
					String Choice = cb.getSelectedItem().toString();

					if ( Choice != null )
					{
						setComboTarget( btn, "hatter " + HatterDaily.getEffectHat( cb.getSelectedIndex() ) );
					}
				}
			}
		}


	}

	public static class BanishedDaily
	extends Daily
	{
		public BanishedDaily()
		{
			this.addListener( "banishingShoutMonsters" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			boolean ban = KoLCharacter.hasSkill( "Banishing Shout" );
			String text = "Banished monsters: " + Preferences.getString( "banishingShoutMonsters" ).replaceAll( "\\|", ", " );
			this.setText( text );
			this.setShown( ban );
		}
	}

	public static class SwimmingPoolDaily
		extends Daily
	{
		public SwimmingPoolDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_olympicSwimmingPool" );
			this.addListener( "kingLiberated" );
			this.addButton( "swim laps", "init +30%, +25 stench dmg, +20 ml, 50 turns" );
			this.addButton( "swim sprints", "-5% combat, 50 turns" );
			this.addLabel( "" );
		}

		@Override
		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean sp = Preferences.getBoolean( "_olympicSwimmingPool" );
			boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Clan Item", "Swimming Pool" );
			this.setShown( ( !bm || kf ) && ( have || sp ) && trendy );
			this.setEnabled( !sp );
		}
	}
}
