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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GuildRequest
	extends GenericRequest
{
	private static final Pattern STILLS_PATTERN = Pattern.compile( "with (\\d+) bright" );
	public static final Pattern SKILL_PATTERN = Pattern.compile( "skillid=(\\d*)" );

	public GuildRequest()
	{
		super( "guild.php" );
	}

	public GuildRequest( final String place)
	{
		this();
		this.addFormField( "place", place );
	}

	public static String whichGuild()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "The Brotherhood of the Smackdown";
		case KoLConstants.MYSTICALITY:
			return "The League of Chef-Magi";
		case KoLConstants.MOXIE:
			return "The Department of Shadowy Arts and Crafts";
		}

		return "None";
	}

	public static String getStoreName()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "The Smacketeria";
		case KoLConstants.MYSTICALITY:
			return "Gouda's Grimoire and Grocery";
		case KoLConstants.MOXIE:
			return "The Shadowy Store";
		}

		return "Nowhere";
	}

	public static String getImplementName()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "The Malus of Forethought";
		case KoLConstants.MYSTICALITY:
			return "The Wok of Ages";
		case KoLConstants.MOXIE:
			return "Nash Crosby's Still";
		}

		return "Nothing";
	}

	public static String getMasterName()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "Gunther, Lord of the Smackdown";
		case KoLConstants.MYSTICALITY:
			return "Gorgonzola, the Chief Chef";
		case KoLConstants.MOXIE:
			return "Shifty, the Thief Chief";
		}

		return "Nobody";
	}

	public static String getTrainerName()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "Torg, the Trainer";
		case KoLConstants.MYSTICALITY:
			return "Brie, the Trainer";
		case KoLConstants.MOXIE:
			return "Lefty, the Trainer";
		}

		return "Nobody";
	}

	public static String getPacoName()
	{
		switch ( KoLCharacter.mainStat() )
		{
		case KoLConstants.MUSCLE:
			return "Olaf the Janitor";
		case KoLConstants.MYSTICALITY:
			return "Blaine";
		case KoLConstants.MOXIE:
			return "Izzy the Lizard";
		}

		return "Nobody";
	}

	public static String getSCGName()
	{
		String name = KoLCharacter.getClassType();
		if ( name.equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			return "Grignr, the Seal Clubber";
		}
		if ( name.equals( KoLCharacter.TURTLE_TAMER ) )
		{
			return "Terry, the Turtle Tamer";
		}
		if ( name.equals( KoLCharacter.PASTAMANCER ) )
		{
			return "Asiago, the Pastamancer";
		}
		if ( name.equals( KoLCharacter.SAUCEROR ) )
		{
			return "Edam, the Sauceror";
		}
		if ( name.equals( KoLCharacter.DISCO_BANDIT ) )
		{
			return "Duncan Drisorderly, the Disco Bandit";
		}
		if ( name.equals( KoLCharacter.ACCORDION_THIEF ) )
		{
			return "Stradella, the Accordion Thief";
		}

		return "Nobody";
	}

	public static String getOCGName()
	{
		String name = KoLCharacter.getClassType();
		if ( name.equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			return "Terry, the Turtle Tamer";
		}
		if ( name.equals( KoLCharacter.TURTLE_TAMER ) )
		{
			return "Grignr, the Seal Clubber";
		}
		if ( name.equals( KoLCharacter.PASTAMANCER ) )
		{
			return "Edam, the Sauceror";
		}
		if ( name.equals( KoLCharacter.SAUCEROR ) )
		{
			return "Asiago, the Pastamancer";
		}
		if ( name.equals( KoLCharacter.DISCO_BANDIT ) )
		{
			return "Stradella, the Accordion Thief";
		}
		if ( name.equals( KoLCharacter.ACCORDION_THIEF ) )
		{
			return "Duncan Drisorderly, the Disco Bandit";
		}

		return "Nobody";
	}

	public static String getNPCName( final String place )
	{
		if ( place == null )
		{
			return null;
		}

		if ( place.equals( "paco" ) )
		{
			return GuildRequest.getPacoName();
		}

		if ( place.equals( "ocg" ) )
		{
			return GuildRequest.getOCGName();
		}

		if ( place.equals( "scg" ) )
		{
			return GuildRequest.getSCGName();
		}

		if ( place.equals( "trainer" ) )
		{
			return GuildRequest.getTrainerName();
		}

		if ( place.equals( "challenge" ) )
		{
			return GuildRequest.getMasterName();
		}

		return null;
	}

	@Override
	public void processResults()
	{
		GuildRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final int findSkill( final String urlString )
	{
		Matcher matcher = GuildRequest.SKILL_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return SkillDatabase.classSkillsBase() + StringUtilities.parseInt( matcher.group( 1 ) );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "guild.php" ) )
		{
			return;
		}
		
		KoLCharacter.setGuildStoreOpen( responseText.indexOf( "\"store.php" ) != -1 );

		Matcher matcher = GenericRequest.PLACE_PATTERN.matcher( urlString );
		String place = matcher.find() ? matcher.group( 1 ) : null;

		if ( place != null && place.equals( "still" ) )
		{
			matcher = GuildRequest.STILLS_PATTERN.matcher( responseText );
			int count =  matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
			KoLCharacter.setStillsAvailable( count );
			return;
		}

		if ( place != null && place.equals( "paco" ) )
		{
			// "paco" assigns the meat car, white citadel, and dwarven factory quests
			if ( InventoryManager.hasItem( ItemPool.CITADEL_SATCHEL ) )
			{
				ResultProcessor.processItem( ItemPool.CITADEL_SATCHEL, -1 );
				QuestDatabase.setQuestProgress( Quest.CITADEL, QuestDatabase.FINISHED );
			}

			if ( InventoryManager.hasItem( ItemPool.THICK_PADDED_ENVELOPE ) )
			{
				ResultProcessor.processItem( ItemPool.THICK_PADDED_ENVELOPE, -1 );
			}

			if ( responseText.contains( "White Citadel" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, QuestDatabase.STARTED );
			}

			return;
		}

		if ( place != null && place.equals( "ocg" ) )
		{
			// "ocg" (Other Class in Guild) assigns Fernswarthy
			// quest

			// <Muscle class> looks surprised as you hand over
			// Fernswarthy's key.

			// "So, have you returned with Fernswarthy's key?"
			// <Mysticality class> nods approvingly as you hand the
			// key to him.

			// <Moxie class> grins and takes Fernswarthy's key from
			// you.

			if ( responseText.indexOf( "hand over Fernswarthy's key" ) != -1 ||
			     responseText.indexOf( "returned with Fernswarthy's key" ) != -1 ||
			     responseText.indexOf( "takes Fernswarthy's key" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.FERNSWARTHYS_KEY, -1 );
			}

			return;
		}

		matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for other simple visits.

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "buyskill" ) )
		{
			if ( responseText.indexOf( "You learn a new skill" ) != -1 )
			{
				int skillId = GuildRequest.findSkill( urlString );
				int cost = SkillDatabase.getSkillPurchaseCost( skillId );
				if ( cost > 0 )
				{
					ResultProcessor.processMeat( -cost );
				}
				ConcoctionDatabase.refreshConcoctions( true );
			}
			return;
		}

		if ( action.equals( "makestaff" ) )
		{
			ChefStaffRequest.parseCreation( urlString, responseText );
			return;
		}

		if ( action.equals( "stillbooze" ) ||
		     action.equals( "stillfruit" ) ||
		     action.equals( "wokcook" ) ||
		     action.equals( "malussmash" ) )
		{
			CreateItemRequest.parseGuildCreation( urlString, responseText );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "guild.php" ) )
		{
			return false;
		}

		Matcher matcher = GenericRequest.PLACE_PATTERN.matcher( urlString );
		String place = matcher.find() ? matcher.group(1) : null;

		if ( place != null && place.equals( "still" ) )
		{
			return true;
		}

		String npc = getNPCName( place );

		if ( npc != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + npc );
			return true;
		}

		matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for other simple visits.

		if ( action == null )
		{
			return true;
		}

		if ( action.equals( "train" ) )
		{
			return true;
		}

		// Other requests handle other actions in the Guild

		// action = makestaff
		// action = wokcook
		// action = malussmash
		// action = stillfruit
		// action = stillbooze

		return false;
	}
}
