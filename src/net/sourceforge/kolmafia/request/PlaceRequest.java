/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;

public class PlaceRequest
	extends GenericRequest
{
	public PlaceRequest()
	{
		super( "place.php" );
	}

	public PlaceRequest( final String place )
	{
		this();
		this.addFormField( "whichplace", place );
	}

	public PlaceRequest( final String place, final String action )
	{
		this( place );
		this.addFormField( "action", action );
	}

	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		PlaceRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			action = "";
		}

		if ( place.equals( "arcade" ) )
		{
			ArcadeRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "chateau" ) )
		{
			ChateauRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "desertbeach" ) )
		{
			if ( action.equals( "db_nukehouse" ) )
			{
				if ( responseText.contains( "anticheese" ) )
				{
					Preferences.setInteger( "lastAnticheeseDay", KoLCharacter.getCurrentDays() );
				}
			}
		}
		else if ( place.equals( "forestvillage" ) )
		{
			if ( action.equals( "fv_untinker" ) )
			{
				UntinkerRequest.parseResponse( urlString, responseText );
			}
		}
		else if ( place.equals( "junggate" ) )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}
		else if ( place.equals( "knoll_friendly" ) )
		{
			KnollRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "manor1" ) )
		{
			if ( action.equals( "manor1_ladys" ) )
			{
				if ( responseText.contains( "ghost of a necklace" ) )
				{
					ResultProcessor.removeItem( ItemPool.SPOOKYRAVEN_NECKLACE );
				}
			}
		}
		else if ( place.equals( "manor2" ) )
		{
			if ( action.equals( "manor2_ladys" ) )
			{
				// Lady Spookyraven's ghostly eyes light up at the sight of her dancing
				// finery. She grabs it from you and excitedly shouts "Meet me in the
				// ballroom in five minutes!" as she darts through the wall.

				if ( responseText.contains( "She grabs it from you" ) )
				{
					ResultProcessor.removeItem( ItemPool.POWDER_PUFF );
					ResultProcessor.removeItem( ItemPool.FINEST_GOWN );
					ResultProcessor.removeItem( ItemPool.DANCING_SHOES );
				}
			}
		}
		else if ( place.equals( "mountains" ) )
		{
			if ( responseText.contains( "chateau" ) )
			{
				Preferences.setBoolean( "chateauAvailable", true );
			}
		}
		else if ( place.equals( "nstower" ) )
		{
			SorceressLairManager.parseTowerResponse( action, responseText );
		}
		else if ( place.equals( "nstower_door" ) )
		{
			SorceressLairManager.parseTowerDoorResponse( action, responseText );;
		}
		else if ( place.equals( "orc_chasm" ) )
		{
			OrcChasmRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "rabbithole" ) )
		{
			RabbitHoleRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "spelunky" ) )
		{
			SpelunkyRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "town_wrong" ) )
		{
			if ( action.equals( "townwrong_artist_quest" ) )
			{
				ArtistRequest.parseResponse( urlString, responseText );
			}
		}
		else if ( place.equals( "twitch" ) )
		{
			if ( action.equals( "twitch_bank" ) && responseText.contains( "Thanks fer bringin' the money back" ) )
			{
				ResultProcessor.removeItem( ItemPool.BIG_BAG_OF_MONEY );
			}
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) )
		{
			return false;
		}

		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return true;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			action = "";
		}

		String message = null;
		boolean turns = false;
		boolean compact = false;

		if ( place.equals( "airport_sleaze" ) )
		{
			if ( action.equals( "airport1_npc1" ) )
			{
				message = "Talking to Buff Jimmy";
			}
			else if ( action.equals( "airport1_npc2" ) )
			{
				message = "Talking to Taco Dan";
			}
			else if ( action.equals( "airport1_npc3" ) )
			{
				message = "Talking to Broden";
			}
		}
		else if ( place.equals( "airport_spooky" ) )
		{
			if ( action.equals( "airport2_radio" ) )
			{
				message = "Using the radio on Conspiracy Island";
			}
		}
		else if ( place.equals( "airport_spooky_bunker" ) )
		{
			if ( action.equals( "si_shop1locked" ) ||
			     action.equals( "si_shop2locked" ) ||
			     action.equals( "si_shop3locked" ))
			{
				return true;
			}
			if ( action.equals( "si_shop1locked" ) )
			{
				message = "Manipulating the Control Panel in the Conspiracy Island bunker";
			}
		}
		else if ( place.equals( "canadia" ) )
		{
			if ( action.equals( "lc_mcd" ) )
			{
				message = "Visiting the Super-Secret Canadian Mind Control Device";
			}
			else if ( action.equals( "lc_marty" ) )
			{
				message = "Talking to Marty";
			}
		}
		else if ( place.equals( "crashsite" ) )
		{
			if ( action.equals( "crash_ship" ) )
			{
				message = "Visiting the Crashed Spaceship";
			}
		}
		else if ( place.equals( "desertbeach" ) )
		{
			if ( action.equals( "db_gnasir" ) )
			{
				message = "Talking to Gnasir";
			}
			else if ( action.equals( "db_nukehouse" ) )
			{
				message = "Visiting the Ruined House";
				compact = true;	// Part of Breakfast
			}
			else if ( action.equals( "db_pyramid1" ) )
			{
				// message = "Visiting the Small Pyramid";
			}
		}
		else if ( place.equals( "forestvillage" ) )
		{
			if ( action.equals( "fv_friar" ) )
			{
				// Don't log this
				return true;
			}
			if ( action.equals( "fv_mystic" ) )
			{
				message = "Talking to the Crackpot Mystic";
			}
		}
		else if ( place.equals( "highlands" ) )
		{
			if ( action.equals( "highlands_dude" ) )
			{
				message = "Talking to the Highland Lord";
			}
		}
		else if ( place.equals( "ioty2014_candy" ) )
		{
			if ( action.equals( "witch_house" ) )
			{
				message = "Visiting the Candy Witch's House";
			}
		}
		else if ( place.equals( "ioty2014_rumple" ) )
		{
			if ( action.equals( "workshop" ) )
			{
				message = "Visiting Rumplestiltskin's Workshop";
			}
		}
		else if ( place.equals( "manor1" ) )
		{
			if ( action.equals( "manor1lock_billiards" ) ||
			     action.equals( "manor1lock_library" ) ||
			     action.equals( "manor1lock_stairsup" ) )
			{
				return true;
			}
			if ( action.equals( "manor1_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "manor2" ) )
		{
			if ( action.equals( "manor2lock_ballroom" ) ||
			     action.equals( "manor2lock_bathroom" ) ||
			     action.equals( "manor2lock_bedroom" ) ||
			     action.equals( "manor2lock_gallery" ) ||
			     action.equals( "manor2lock_stairsup" ) )
			{
				return true;
			}
			if ( action.equals( "manor2_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "manor3" ) )
		{
			if ( action.equals( "manor3_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "manor4" ) )
		{
			if ( action.equals( "manor4_chamber" ) )
			{
				return true;
			}
			if ( action.equals( "manor4_chamberwall" ) ||
			     action.equals( "manor4_chamberwalllabel" ) )
			{
				message = "Inspecting Suspicious Masonry";
			}
		}
		else if ( place.equals( "mclargehuge" ) )
		{
			if ( action.equals( "trappercabin" ) )
			{
				message = "Visiting the Trapper";
			}
			else if ( action.equals( "cloudypeak" ) )
			{
				message = "Ascending the Mist-Shrouded Peak";
			}
		}
		else if ( place.equals( "mountains" ) )
		{
			if ( action.equals( "mts_melvin" ) )
			{
				message = "Talking to Melvign the Gnome";
			}
		}
		else if ( place.equals( "orc_chasm" ) )
		{
			if ( action.startsWith( "bridge" ) || action.equals( "label2" ) )
			{
				// Building the bridge. Do we need to log anything?
				return true;
			}
		}
		else if ( place.equals( "palindome" ) )
		{
			if ( action.equals( "pal_drlabel" ) || action.equals( "pal_droffice" ) )
			{
				message = "Visiting Dr. Awkward's office";
			}
			else if ( action.equals( "pal_mrlabel" ) || action.equals( "pal_mroffice" ) )
			{
				message = "Visiting Mr. Alarm's office";
			}
		}
		else if ( place.equals( "plains" ) )
		{
			if ( action.equals( "rift_scorch" ) || action.equals( "rift_light" ) )
			{
				return true;
			}
			if ( action.equals( "garbage_grounds" ) )
			{
				message = "Inspecting the Giant Pile of Coffee Grounds";
			}
			else if ( action.equals( "lutersgrave" ) )
			{
				if ( !InventoryManager.hasItem( ItemPool.CLANCY_LUTE ) )
				{
					message = "The Luter's Grave";
					turns = true;
				}
			}
		}
		else if ( place.equals( "pyramid" ) )
		{
			if ( action.equals( "pyramid_control" ) )
			{
				message = "Visiting the Pyramid Control Room";
			}
		}
		else if ( place.equals( "town" ) )
		{
			if ( action.equals( "town_oddjobs" ) )
			{
				message = "Visiting the Odd Jobs Board";
			}
		}
		else if ( place.equals( "town" ) )
		{
			if ( action.equals( "town_oddjobs" ) )
			{
				message = "Visiting the Odd Jobs Board";
			}
		}
		else if ( place.equals( "twitch" ) )
		{
			if ( action.equals( "twitch_votingbooth" ) )
			{
				message = "Visiting the Voting / Phone Booth";
			}
			else if ( action.equals( "twitch_dancave1" ) ||
				  action.equals( "twitch_dancave1" ) )
			{
				message = "Visiting Caveman Dan's Cave";
			}
			else if ( action.equals( "twitch_shoerepair" ) )
			{
				message = "Visiting the Shoe Repair Store";
			}
			else if ( action.equals( "twitch_colosseum" ) )
			{
				message = "Visiting the Chariot-Racing Colosseum";
			}
			else if ( action.equals( "twitch_survivors" ) )
			{
				message = "Visiting the Post-Apocalyptic Survivor Encampment";
			}
			else if ( action.equals( "twitch_bank" ) )
			{
				message = "Visiting the Third Four-Fifths Bank of the West";
			}
		}
		else if ( place.equals( "rabbithole" ) )
		{
			if ( action.equals( "rabbithole_teaparty" ) )
			{
				message = "Visiting the Mad Tea Party";
			}
		}
		else if ( place.equals( "sea_oldman" ) )
		{
			// place.php?whichplace=sea_oldman&action=oldman_oldman&preaction=pickreward&whichreward=6313[/code]
			if ( action.equals( "oldman_oldman" ) )
			{
				message = "Talking to the Old Man";
			}
		}
		else if ( place.equals( "woods" ) )
		{
			if ( action.equals( "woods_emptybm" ) )
			{
				// Visiting the Empty Black Market
				return true;
			}
			if ( action.equals( "woods_smokesignals" ) )
			{
				message = "Investigating the Smoke Signals";
			}
			if ( action.equals( "woods_hippy" ) )
			{
				message = "Talking to that Hippy";
			}
			if ( action.equals( "woods_dakota_anim" ) || action.equals( "woods_dakota" ) )
			{
				message = "Talking to Dakota Fanning";
			}
		}

		if ( message == null )
		{
			// Other classes have already been given the
			// opportunity to claim this request. Don't log the URL
			// of simple visits, but do log unclaimed actions.
			return action.equals( "" );
		}

		if ( turns )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] " + message;
		}

		if ( !compact )
		{
			RequestLogger.printLine();
		}
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
