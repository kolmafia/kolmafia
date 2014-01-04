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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VolcanoIslandRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "(action|subaction)=([^&]*)" );

	// Actions
	private static final String NPC = "npc";

	// Subactions
	private static final String SLIME = "getslime";

	/**
	 * Constructs a new <code>VolcanoIslandRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private VolcanoIslandRequest()
	{
		this( NPC );
	}

	public VolcanoIslandRequest( final String action )
	{
		super( "volcanoisland.php" );
		this.addFormField( "action", action );
	}

	public VolcanoIslandRequest( final String action, final String subaction )
	{
		this( action );
		this.addFormField( "subaction", subaction );
	}

	public static void getSlime()
	{
		VolcanoIslandRequest request = new VolcanoIslandRequest( NPC, SLIME);
		RequestThread.postRequest( request );
	}

	public static String npcName()
	{
		String classType = KoLCharacter.getClassType();
		if ( classType.equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			return "a Palm Tree Shelter";
		}
		if ( classType.equals( KoLCharacter.TURTLE_TAMER ) )
		{
			return "a Guy in the Bushes";
		}
		if ( classType.equals( KoLCharacter.DISCO_BANDIT ) )
		{
			return "a Girl in a Black Dress";
		}
		if ( classType.equals( KoLCharacter.ACCORDION_THIEF ) )
		{
			return "the Fishing Village";
		}
		if ( classType.equals( KoLCharacter.PASTAMANCER ) )
		{
			return "a Protestor";
		}
		if ( classType.equals( KoLCharacter.SAUCEROR ) )
		{
			return "a Boat";
		}
		return null;
	}

	private static String visitNPC( final String urlString )
	{
		Matcher matcher = VolcanoIslandRequest.ACTION_PATTERN.matcher( urlString);
		String action = null;
		String subaction = null;
		while ( matcher.find() )
		{
			String tag = matcher.group(1);
			String value = matcher.group(2);
			if ( tag.equals( "action" ) )
			{
				action = value;
			}
			else 
			{
				subaction = value;
			}
		}

		if ( action == null || !action.equals( NPC ) )
		{
			return null;
		}

		if ( subaction == null )
		{
			String name = VolcanoIslandRequest.npcName();
			return "Visiting " + name + " on the Secret Tropical Island Volcano Lair";
		}

		if ( subaction.equals( SLIME ) && KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
		{
			return "[" + KoLAdventure.getAdventureCount() + "] Volcano Island (Drums of Slime)";
		}

		return null;
	}

	public static void getBreakfast()
	{
		// If you have defeated your Nemesis as an Accordion Thief, you
		// have The Trickster's Trikitixa in inventory and can visit
		// the Fishing Village once a day for a free fisherman's sack.
		if ( InventoryManager.hasItem( ItemPool.TRICKSTER_TRIKITIXA ) )
		{
			VolcanoIslandRequest request = new VolcanoIslandRequest();
			request.run();
		}
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "volcanoisland.php" ) ||
		     urlString.indexOf( "action=tniat" ) == -1 )
		{
			return;
		}

		// A Pastamancer wearing the spaghetti cult robes loses them
		// when first visiting the Temple
		//
		// "A fierce wind whips through the chamber, first blowing back
		// your hood and then ripping the robe from your shoulders."

		if ( KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER &&
		     responseText.indexOf( "ripping the robe from your shoulders" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.SPAGHETTI_CULT_ROBE );
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "volcanoisland.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "subaction=make" ) != -1 )
		{
			return PhineasRequest.registerRequest( urlString );
		}

		String message = VolcanoIslandRequest.visitNPC( urlString );
		if ( message == null )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
