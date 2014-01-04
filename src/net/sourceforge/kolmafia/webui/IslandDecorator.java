/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.IslandRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.IslandManager.Quest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandDecorator
{
	private static final String progressLineStyle = "<td style=\"color: red;font-size: 80%\" align=center>";

	// KoLmafia images showing each quest area on bigisland.php

	// JHunz's replacement images for Big Island sidequest areas from his
	// BattlefieldCounter Greasemonkey script, used with his permission.
	//
	//	http://userscripts.org/scripts/show/11720

	private static final String IMAGE_ROOT = "http://images.kingdomofloathing.com/otherimages/bigisland/";
	private static final String LOCAL_ROOT = "/images/otherimages/bigisland/";

	private static final Object[][] IMAGES =
	{
		{
			Quest.JUNKYARD,
			IslandDecorator.IMAGE_ROOT + "2.gif",
			IslandDecorator.LOCAL_ROOT + "2F.gif",
			IslandDecorator.LOCAL_ROOT + "2H.gif",
		},
		{
			Quest.ORCHARD,
			IslandDecorator.IMAGE_ROOT + "3.gif",
			IslandDecorator.LOCAL_ROOT + "3F.gif",
			IslandDecorator.LOCAL_ROOT + "3H.gif",
		},
		{
			Quest.ARENA,
			IslandDecorator.IMAGE_ROOT + "6.gif",
			IslandDecorator.LOCAL_ROOT + "6F.gif",
			IslandDecorator.LOCAL_ROOT + "6H.gif",
		},
		{
			Quest.FARM,
			IslandDecorator.IMAGE_ROOT + "15.gif",
			IslandDecorator.LOCAL_ROOT + "15F.gif",
			IslandDecorator.LOCAL_ROOT + "15H.gif",
		},
		{
			Quest.LIGHTHOUSE,
			IslandDecorator.IMAGE_ROOT + "17.gif",
			IslandDecorator.LOCAL_ROOT + "17F.gif",
			IslandDecorator.LOCAL_ROOT + "17H.gif",
		},
		{
			Quest.NUNS,
			IslandDecorator.IMAGE_ROOT + "19.gif",
			IslandDecorator.LOCAL_ROOT + "19F.gif",
			IslandDecorator.LOCAL_ROOT + "19H.gif",
		},
	};

	private static final Object[] findImages( final Quest quest )
	{
		for ( int i = 0; i < IslandDecorator.IMAGES.length; ++i )
		{
			Object[] row = IslandDecorator.IMAGES[ i ];
			if ( (Quest)( row[ 0 ] ) == quest )
			{
				return row;
			}
		}
		return null;
	}

	private static final String originalImage( final Quest quest )
	{
		Object[] row = IslandDecorator.findImages( quest );
		return row == null ? "" : (String)( row[ 1 ] );
	}

	private static final String fratImage( final Quest quest )
	{
		Object[] row = IslandDecorator.findImages( quest );
		return row == null ? "" : (String)( row[ 2 ] );
	}

	private static final String hippyImage( final Quest quest )
	{
		Object[] row = IslandDecorator.findImages( quest );
		return row == null ? "" : (String)( row[ 3 ] );
	}

	private static final String sidequestImage( final String setting, final Quest quest )
	{
		String status = Preferences.getString( setting );
		return	status.equals( "fratboy" ) ?
			IslandDecorator.fratImage( quest) :
			status.equals( "hippy" ) ?
			IslandDecorator.hippyImage( quest) :
			null;
	}

	/*
	 * Methods to decorate the Fight page
	 */

	public static final void decorateThemtharFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		String message = "<p><center>" + IslandDecorator.meatMessage() + "<br>";
		buffer.insert( index, message );
	}

	// Meat drops from dirty thieving brigands
	private static final int BRIGAND_MIN = 800;
	private static final int BRIGAND_MAX = 1250;

	private static final String meatMessage()
	{
		StringBuffer message = new StringBuffer();

		int current = IslandManager.currentNunneryMeat();
		message.append( KoLConstants.COMMA_FORMAT.format( current ) );
		message.append( " meat recovered, " );

		double left = 100000 - current;

		message.append( KoLConstants.COMMA_FORMAT.format( left ) );
		message.append( " left (" );

		double mod = ( KoLCharacter.currentNumericModifier( Modifiers.MEATDROP ) + 100.0f ) / 100.0f;
		double min = BRIGAND_MIN * mod;
		double max = BRIGAND_MAX * mod;

		int minTurns = (int) Math.ceil( left / max );
		int maxTurns = (int) Math.ceil( left / min );

		message.append( String.valueOf( minTurns ) );

		if ( minTurns != maxTurns )
		{
			message.append( "-" );
			message.append( String.valueOf( maxTurns ) );
		}

		message.append( " turns)." );

		return message.toString();
	}

	private static final String[] GREMLIN_TOOLS =
	{
		"It whips out a hammer",
		"He whips out a crescent wrench",
		"It whips out a pair of pliers",
		"It whips out a screwdriver",
	};
	
	private static final int[][] TOOL_LOCATIONS = 
	{
		{
			AdventurePool.JUNKYARD_BARREL,
			ItemPool.MOLYBDENUM_HAMMER
		},

		{
			AdventurePool.JUNKYARD_TIRES,
			ItemPool.MOLYBDENUM_WRENCH
		},

		{
			AdventurePool.JUNKYARD_REFRIGERATOR,
			ItemPool.MOLYBDENUM_PLIERS
		},

		{
			AdventurePool.JUNKYARD_CAR,
			ItemPool.MOLYBDENUM_SCREWDRIVER
		}
	};

	public static final void decorateGremlinFight( final StringBuffer buffer )
	{
		// Color the tool in the monster spoiler text
		int loc = KoLAdventure.lastAdventureId();
		if ( IslandManager.missingGremlinTool() == null )
		{
			for ( int i = 0; i < IslandDecorator.TOOL_LOCATIONS.length; ++i )
			{
				if ( loc != IslandDecorator.TOOL_LOCATIONS[i][0] )
				{
					continue;
				}
				if ( KoLConstants.inventory.contains( ItemPool.get( IslandDecorator.TOOL_LOCATIONS[i][1], 1 ) ) )
				{
					break;
				}
				String zoneTool = ItemDatabase.getItemName( IslandDecorator.TOOL_LOCATIONS[i][1] );
				StringUtilities.singleStringReplace( buffer,
								     zoneTool,
								     "<font color=#DD00FF>" + zoneTool + "</font>" );
				break;
			}
		}

		for ( int i = 0; i < IslandDecorator.GREMLIN_TOOLS.length; ++i )
		{
			String tool = IslandDecorator.GREMLIN_TOOLS[ i ];
			if ( buffer.indexOf( tool ) != -1 )
			{
				// Make the message pink
				StringUtilities.singleStringReplace( buffer, tool, "<font color=#DD00FF>" + tool + "</font>" );
				String select1 = "<option picurl=magnet2 selected value=2497>";
				String select2 = "<option selected value=2497>";

				// If we already have the molybdenum magnet
				// selected (which should only be possible if
				// we are funkslinging), cool. Otherwise, get
				// rid of current selection(s) and select the
				// magnet on the first combat item dropdown.

				if ( buffer.indexOf( select1 ) == -1 && buffer.indexOf( select2 ) == -1 )
				{
					// Unselect battle actions in dropdowns on the fight page
					StringUtilities.globalStringReplace( buffer, " selected ", " " );

					// Select the molybdenum magnet
					// <option picurl=magnet2 selected value=2497>molybdenum magnet (1)</option>
					String search = "<option picurl=magnet2 value=2497>";
					StringUtilities.singleStringReplace( buffer, search, select1 );
				}
				break;
			}
		}
	}

	public static final void appendMissingGremlinTool( final StringBuffer buffer )
	{
		if ( IslandManager.missingGremlinTool() != null )
		{
			buffer.append( "<br />This gremlin does <b>NOT</b> have a " ).append( IslandManager.missingGremlinTool() );
		}
	}

	private static final String victoryMessageHTML( final int last, final int current )
	{
		String message = IslandManager.victoryMessage( last, current );
		return message == null ? "" : message + "<br>";
	}

	private static final String areaMessageHTML( final int last, final int current )
	{
		String message = IslandManager.areaMessage( last, current );
		return message == null ? "" : "<b>" + message + "</b><br>";
	}

	private static final String heroMessageHTML( final int last, final int current )
	{
		String message = IslandManager.heroMessage( last, current );
		return message == null ? "" : "<b>" + message + "</b><br>";
	}

	public static final void decorateBattlefieldFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		// Don't bother showing progress of the war if you've just won
		String monster = FightRequest.getLastMonsterName();
		if ( monster.equalsIgnoreCase( "The Big Wisniewski" ) || monster.equalsIgnoreCase( "The Man" ) )
		{
			return;
		}

		int last;
		int current;

		if ( IslandManager.fratboy() )
		{
			last = IslandManager.lastFratboysDefeated();
			current = IslandManager.fratboysDefeated();
		}
		else
		{
			last = IslandManager.lastHippiesDefeated();
			current = IslandManager.hippiesDefeated();
		}

		if ( last == current )
		{
			return;
		}

		String message =
			"<p><center>" +
			victoryMessageHTML( last, current ) +
			areaMessageHTML( last, current ) +
			heroMessageHTML( last, current ) +
			"</center>";

		buffer.insert( index, message );
	}

	/*
	 * Method to decorate the Big Island map
	 */

	// Decorate the HTML with custom goodies
	public static final void decorateBigIsland( final String url, final StringBuffer buffer )
	{
		// Quest-specific page decorations
		IslandDecorator.decorateJunkyard( buffer );
		IslandDecorator.decorateArena( url, buffer );
		IslandDecorator.decorateNunnery( url, buffer );

		// Find the table that contains the map.
		int tableIndex = buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Mysterious Island of Mystery</b></td>" );
		if ( tableIndex != -1 )
		{
			String fratboyMessage = IslandManager.sideSummary( "frat boys" );
			String hippyMessage = IslandManager.sideSummary( "hippies" );
			String row = 
				"<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle +
				fratboyMessage +
				"</td>" + IslandDecorator.progressLineStyle +
				hippyMessage +
				"</td>" + "</tr></table></td></tr>";

			buffer.insert( tableIndex, row );
		}

		// Now replace sidequest location images for completed quests
		IslandDecorator.sidequestImage( buffer, "sidequestArenaCompleted", Quest.ARENA );
		IslandDecorator.sidequestImage( buffer, "sidequestFarmCompleted", Quest.FARM );
		IslandDecorator.sidequestImage( buffer, "sidequestJunkyardCompleted", Quest.JUNKYARD );
		IslandDecorator.sidequestImage( buffer, "sidequestLighthouseCompleted", Quest.LIGHTHOUSE );
		IslandDecorator.sidequestImage( buffer, "sidequestNunsCompleted", Quest.NUNS );
		IslandDecorator.sidequestImage( buffer, "sidequestOrchardCompleted", Quest.ORCHARD );
	}

	private static final void sidequestImage( final StringBuffer buffer, final String setting, final Quest quest )
	{
		String image = IslandDecorator.sidequestImage( setting, quest );
		if ( image == null )
		{
			return;
		}

		String old = IslandDecorator.originalImage( quest);
		StringUtilities.singleStringReplace( buffer, old, image );
	}

	public static final void decorateJunkyard( final StringBuffer buffer )
	{
		if ( IslandManager.currentJunkyardLocation().equals( "" ) )
		{
			return;
		}

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Junkyard</b></td>" );
		if ( tableIndex == -1 )
		{
			return;
		}

		String message;

		if ( !InventoryManager.hasItem( ItemPool.MOLYBDENUM_MAGNET ) )
		{
			message = "Visit Yossarian in uniform to get a molybdenum magnet";
		}
		else if ( IslandManager.currentJunkyardTool().equals( "" ) )
		{
			message = "Visit Yossarian for your next assignment";
		}
		else if ( InventoryManager.hasItem( ItemPool.MOLYBDENUM_HAMMER ) &&
				  InventoryManager.hasItem( ItemPool.MOLYBDENUM_SCREWDRIVER ) &&
				  InventoryManager.hasItem( ItemPool.MOLYBDENUM_PLIERS ) &&
				  InventoryManager.hasItem( ItemPool.MOLYBDENUM_WRENCH ) )
		{
			message = "Visit Yossarian in uniform to receive your reward for finding all four molybdenum tools";
		}
		else
		{
			message = "Look for the " + IslandManager.currentJunkyardTool() + " " + IslandManager.currentJunkyardLocation();
		}

		String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + ".</td>" + "</tr></table></td></tr>";

		buffer.insert( tableIndex, row );
	}

	public static final void decorateArena( final String urlString, final StringBuffer buffer )
	{
		// If he's not visiting the arena, punt
		if ( !urlString.contains( "place=concert" ) )
		{
			return;
		}

		// If there's no concert available, see if quest is in progress
		if ( buffer.indexOf( "value=\"concert\"" ) == -1 )
		{
			if ( Preferences.getString( "warProgress" ).equals( "finished" ) ||
			     !Preferences.getString( "sidequestArenaCompleted" ).equals( "none" ) )
			{
				// War is over or quest is finished. Punt.
				return;
			}

			int tableIndex =
				buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Mysterious Island Arena</b></td>" );
			if ( tableIndex != -1 )
			{
				String message = RequestEditorKit.advertisingMessage();
				String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + "</td></tr></table></td></tr>";
				buffer.insert( tableIndex, row );
			}
			return;
		}

		String quest = Preferences.getString( "sidequestArenaCompleted" );
		String [][] array =
			quest.equals( "hippy" ) ?
			IslandRequest.HIPPY_CONCERTS :
			IslandRequest.FRATBOY_CONCERTS;

		String text = buffer.toString();
		buffer.setLength( 0 );

		int index1 = 0, index2;

		// Add first choice spoiler
		String choice = array[0][0] + ": " + array[0][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" ).append( choice ).append( ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add second choice spoiler
		choice = array[1][0] + ": " + array[1][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" ).append( choice ).append( ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add third choice spoiler
		choice = array[2][0] + ": " + array[2][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" ).append( choice ).append( ")</font><br/></form>" );
		index1 = index2 + 7;

		// Append remainder of buffer
		buffer.append( text.substring( index1 ) );
	}

	public static final void decorateNunnery( final String urlString, final StringBuffer buffer )
	{
		// If he's not visiting the nunnery, punt
		if ( !urlString.contains( "place=nunnery" ) )
		{
			return;
		}

		// See if quest is in progress
		if ( Preferences.getString( "warProgress" ).equals( "finished" ) ||
		     !Preferences.getString( "sidequestNunsCompleted" ).equals( "none" ) )
		{
			// Either the war or quest is over. Punt
			return;
		}

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Our Lady of Perpetual Indecision</b></td>" );
		if ( tableIndex == -1 )
		{
			return;
		}

		String message = IslandDecorator.meatMessage();
		String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + "</td></tr></table></td></tr>";
		buffer.insert( tableIndex, row );
	}

	public static final void decoratePostwarIsland( final String url, final StringBuffer buffer )
	{
		// Quest-specific page decorations
		IslandDecorator.decorateArena( url, buffer );

		// Replace sidequest location images for completed quests

		// The arena is available after the war only if the fans of the
		// concert you promoted won the war.
		String arena = IslandManager.questCompleter( "sidequestArenaCompleted" );
		String winner = IslandManager.warWinner();
		if ( arena.equals( winner ) )
		{
			IslandDecorator.sidequestImage( buffer, "sidequestArenaCompleted", Quest.ARENA );
		}

		// If you aided the nuns during the war, they will help you
		// after the war, regardless of who won.
		IslandDecorator.sidequestImage( buffer, "sidequestNunsCompleted", Quest.NUNS );
	}
}
