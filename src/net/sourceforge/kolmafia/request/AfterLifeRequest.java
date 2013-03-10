/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AfterLifeRequest
	extends GenericRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "<span onclick='descitem\\(([\\d]+)\\)'>([^<]*)<.*?name=whichitem value=([\\d]+)>", Pattern.DOTALL );
	private static final Pattern KARMA_PATTERN = Pattern.compile( "You gain ([0123456789,]+) Karma", Pattern.DOTALL );

	private AfterLifeRequest()
	{
		super( "afterlife.php" );
	}

	@Override
	public void processResults()
	{
		AfterLifeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "afterlife.php" ) )
		{
			return false;
		}

		// If this is our first visit to the afterlife - we are outside
		// the pearly gates - refresh the charpane
		if ( urlString.equals( "afterlife.php" ) )
		{
			return true;
		}

		// Learn new astral items simply by visiting an astral vendor

		// afterlife.php?place=permery
		// afterlife.php?place=deli
		// afterlife.php?place=armory
		// afterlife.php?place=reincarnate

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			int itemId = StringUtilities.parseInt( matcher.group(3) );

			String data = ItemDatabase.getItemDataName( itemId );
			if ( data == null || !data.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}
		}

		String action = GenericRequest.getAction( urlString );

		// No need to refresh if simply visiting a vendor
		if ( action == null )
		{
			return false;
		}

		if ( action.equals( "pearlygates" ) )
		{
			int karma = Preferences.getInteger( "bankedKarma" );
			RequestLogger.updateSessionLog( "You have " + karma + " banked Karma." );
			// <td valign=center>You gain 311 Karma</td>
			matcher = KARMA_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				int delta = StringUtilities.parseInt( matcher.group( 1 ) );
				RequestLogger.updateSessionLog( "You gain " + delta + " Karma" );
				karma += delta;
			}
			RequestLogger.updateSessionLog( "Your new Karma balance is " + karma );
			Preferences.setInteger( "bankedKarma", karma ); 
			return true;
		}

		int delta = 0;
		if ( action.equals( "scperm" ) )
		{
			// afterlife.php?action=scperm&whichskill=6027
			// <td valign=center>You spend 100 Karma</td>
			delta = -100;
		}
		else if ( action.equals( "hcperm" ) )
		{
			// afterlife.php?action=hcperm&whichskill=6027
			// <td valign=center>You spend 200 Karma</td>
			delta = -200;
		}
		else if ( action.equals( "returnskill" ) )
		{
			// afterlife.php?action=returnskill&classid=6&skillid=27&hc=1
			// <td>Skill permanence returned.</td>
			delta = urlString.indexOf( "hc=1" ) == -1 ? 100 : 200;
		}
		else if ( action.equals( "buydeli" ) )
		{
			// afterlife.php?action=buydeli&whichitem=5045
			// <td valign=center>You spend 1 Karma</td>
			delta = -1;
		}
		else if ( action.equals( "delireturn" ) )
		{
			// afterlife.php?action=delireturn&whichitem=5045
			// <td valign=center>You gain 1 Karma</td>
			delta = 1;
		}
		else if ( action.equals( "buyarmory" ) )
		{
			// afterlife.php?action=buyarmory&whichitem=5041
			// <td valign=center>You spend 10 Karma</td>
			delta = -10;
		}
		else if ( action.equals( "armoryreturn" ) )
		{
			// afterlife.php?action=armoryreturn&whichitem=5041
			// <td valign=center>You gain 10 Karma</td>
			delta = 10;
		}

		if ( delta != 0 )
		{
			Preferences.increment( "bankedKarma", delta ); 
			String message = ( delta < 0 ) ?
				( "You spend " + (-delta) + " Karma" ) :
				( "You gain " + delta + " Karma" );
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}

	public static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=([^&]*)" );
	public static final Pattern CLASSID_PATTERN = Pattern.compile( "classid=([^&]*)" );
	public static final Pattern SKILLID_PATTERN = Pattern.compile( "skillid=([^&]*)" );
	public static final Pattern HC_PATTERN = Pattern.compile( "hc=([^&]*)" );
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=([^&]*)" );
	public static final Pattern SIGN_PATTERN = Pattern.compile( "whichsign=([^&]*)" );
	public static final Pattern GENDER_PATTERN = Pattern.compile( "gender=([^&]*)" );
	public static final Pattern CLASS_PATTERN = Pattern.compile( "whichclass=([^&]*)" );
	public static final Pattern PATH_PATTERN = Pattern.compile( "whichpath=([^&]*)" );
	public static final Pattern TYPE_PATTERN = Pattern.compile( "asctype=([^&]*)" );

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "afterlife.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		// Visiting the Permery
		// afterlife.php?place=permery
		// Visiting the Deli
		// afterlife.php?place=deli
		// Visiting the Armory
		// afterlife.php?place=armory
		// Visiting the Bureau of Reincarnation
		// afterlife.php?place=reincarnate

		// No need to refresh if simply visiting a vendor
		if ( action == null )
		{
			return true;
		}

		String message = null;
		int karma = Preferences.getInteger( "bankedKarma" ); 

		// Walking through the Pearly Gates
		// afterlife.php?action=pearlygates
		if ( action.equals( "pearlygates" ) )
		{
			message = "Welcome to Valhalla!";
		}

		// Perming a skill
		// afterlife.php?action=scperm&whichskill=6027
		// afterlife.php?action=hcperm&whichskill=6027
		else if ( action.equals( "scperm" ) || action.equals( "hcperm" ) )
		{
			Matcher m = SKILL_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}

			int skillId = StringUtilities.parseInt( m.group( 1 ) );
			boolean hc = action.startsWith( "hc" );
			String skill = SkillDatabase.getSkillName( skillId );

			String type = hc ? "Hard" : "Soft";
			String cost = hc ? "200" : "100";
			String name = ( skill != null ) ? skill : ( "Skill #" + skillId );
			message = type + "core perm " + name + " for " + cost + " Karma (initial balance = " + karma + ")";
		}

		// Returning a skill
		// afterlife.php?action=returnskill&classid=6&skillid=27&hc=1
		else if ( action.equals( "returnskill" ) )
		{
			Matcher m = CLASSID_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int classId = StringUtilities.parseInt( m.group( 1 ) );

			m = SKILLID_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int skillId = StringUtilities.parseInt( m.group( 1 ) );

			m = HC_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			boolean hc = m.group( 1 ).equals( "1" );

			int id = ( classId * 1000 ) + skillId;
			String skill = SkillDatabase.getSkillName( id );

			String type = hc ? "Hard" : "Soft";
			String cost = hc ? "200" : "100";
			String name = ( skill != null ) ? skill : ( "Skill #" + id );
			message = "Return " + type + "core Skill " + name + " for " + cost + " Karma (initial balance = " + karma + ")";
		}

		// Buying from the Deli
		// afterlife.php?action=buydeli&whichitem=5045
		// Buying an item
		// afterlife.php?action=buyarmory&whichitem=5041
		else if ( action.equals( "buydeli" ) || action.equals( "buyarmory" ) )
		{
			Matcher m = ITEMID_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int itemId = StringUtilities.parseInt( m.group( 1 ) );
			String itemName = ItemDatabase.getItemName( itemId );
			String cost = action.equals( "buydeli" ) ? "1" : "10";
			message = "Buy " + itemName + " for " + cost + " Karma (initial balance = " + karma + ")";
		}

		// Returning an item to the Deli
		// afterlife.php?action=delireturn&whichitem=5045
		// Returning an item
		// afterlife.php?action=armoryreturn&whichitem=5041
		else if ( action.equals( "delireturn" ) || action.equals( "armoryreturn" ) )
		{
			Matcher m = ITEMID_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int itemId = StringUtilities.parseInt( m.group( 1 ) );
			String itemName = ItemDatabase.getItemName( itemId );
			String cost = action.startsWith( "deli" ) ? "1" : "10";
			message = "Return " + itemName + " for " + cost + " Karma (initial balance = " + karma + ")";
		}

		// Ascending
		// afterlife.php?action=ascend&asctype=3&whichclass=4&gender=2&whichpath=4&whichsign=2
		// Confirming Ascension
		// afterlife.php?action=ascend&confirmascend=1&whichsign=2&gender=2&whichclass=4&whichpath=4&asctype=3
		else if ( action.equals( "ascend" ) )
		{
			if ( urlString.indexOf( "confirmascend=1" ) == -1 )
			{
				return true;
			}

			Matcher m = TYPE_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int type = StringUtilities.parseInt( m.group( 1 ) );

			StringBuilder builder = new StringBuilder();
			builder.append( "Ascend as a " );

			switch ( type )
			{
			case 1:
				builder.append( "Casual" );
				break;
			case 2:
				builder.append( "Normal" );
				break;
			case 3:
				builder.append( "Hardcore" );
				break;
			default:
				builder.append( "(Type " );
				builder.append( String.valueOf( type ) );
				builder.append( ")" );
				break;
			}

			m = GENDER_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int gender = StringUtilities.parseInt( m.group( 1 ) );

			builder.append( " " );

			switch ( gender )
			{
			case 1:
				builder.append( "Male" );
				break;
			case 2:
				builder.append( "Female" );
				break;
			default:
				builder.append( "(Gender " );
				builder.append( String.valueOf( gender ) );
				builder.append( ")" );
				break;
			}

			m = CLASS_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int pclass = StringUtilities.parseInt( m.group( 1 ) );

			builder.append( " " );

			switch ( pclass )
			{
			case 1:
				builder.append( "Seal Clubber" );
				break;
			case 2:
				builder.append( "Turtle Tamer" );
				break;
			case 3:
				builder.append( "Pastamancer" );
				break;
			case 4:
				builder.append( "Sauceror" );
				break;
			case 5:
				builder.append( "Disco Bandit" );
				break;
			case 6:
				builder.append( "Accordion Thief" );
				break;
			case 11:
				builder.append( "Avatar of Boris" );
				break;
			case 12:
				builder.append( "Zombie Master" );
				break;
			case 14:
				builder.append( "Avatar of Jarlsberg" );
				break;
			default:
				builder.append( "(Class " );
				builder.append( String.valueOf( pclass ) );
				builder.append( ")" );
				break;
			}

			m = SIGN_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}

			builder.append( " under the " );

			int sign = StringUtilities.parseInt( m.group( 1 ) );
			if ( sign >= 1 && sign <= KoLCharacter.ZODIACS.length )
			{
				builder.append( KoLCharacter.ZODIACS[ sign - 1 ] );
			}
			else if ( sign == 10 )
			{
				builder.append( "Bad Moon" );
			}
			else
			{
				builder.append( "(Sign " );
				builder.append( String.valueOf( sign ) );
				builder.append( ")" );
			}

			builder.append( " sign" );

			m = PATH_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return true;
			}
			int path = StringUtilities.parseInt( m.group( 1 ) );

			builder.append( " on " );

			switch ( path )
			{
			case 0:
				builder.append( "no" );
				break;
			case 1:
				builder.append( "a Boozetafarians" );
				break;
			case 2:
				builder.append( "a Teetotaler" );
				break;
			case 3:
				builder.append( "an Oxygenarian" );
				break;
			case 4:
				builder.append( "a Bees Hate You" );
				break;
			case 6:
				builder.append( "a Way of the Surprising Fist" );
				break;
			case 7:
				builder.append( "a Trendy" );
				break;
			case 8:
				builder.append( "an Avatar of Boris" );
				break;
			case 9:
				builder.append( "a Bugbear Invasion" );
				break;
			case 10:
				builder.append( "a Zombie Slayer" );
				break;
			case 11:
				builder.append( "a Class Act" );
				break;
			case 12:
				builder.append( "an Avatar of Jarlsberg" );
				break;
			default:
				builder.append( "(Path " );
				builder.append( String.valueOf( path ) );
				builder.append( ")" );
				break;
			}

			builder.append( " path," );

			builder.append( " banking " );
			builder.append( String.valueOf( karma ) );
			builder.append( " Karma." );

			message = builder.toString();
		}

		if ( message == null )
		{
			// Something New!
			return false;
		}

		KoLmafia.updateDisplay( "" );
		RequestLogger.updateSessionLog( "" );

		KoLmafia.updateDisplay( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
