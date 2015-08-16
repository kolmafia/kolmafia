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

package net.sourceforge.kolmafia.session;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.request.MonsterManuelRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterManuelManager
{
	private static final Map<Integer, String> manuelEntries = new TreeMap<Integer, String>();
	private static final Map<Integer, Integer> manuelFactoidCounts = new TreeMap<Integer, Integer>();

	public static void flushCache()
	{
		MonsterManuelManager.manuelEntries.clear();
		MonsterManuelManager.manuelFactoidCounts.clear();
	}

	public static void reset()
	{
		// Reset Your winged yeti and You the Adventurer
		MonsterManuelManager.reset( MonsterDatabase.findMonsterById( 1667 ) );
		MonsterManuelManager.reset( MonsterDatabase.findMonsterById( 1669 ) );

		Iterator<Entry<Integer,Integer>> entryIterator = MonsterManuelManager.manuelFactoidCounts.entrySet().iterator();

		// Remove all entries that have less than 3 factoids, since the
		// current player may know more.
		while ( entryIterator.hasNext() )
		{
			Entry<Integer,Integer> entry = entryIterator.next();
			Integer key = entry.getKey();
			Integer value = entry.getValue();
			if ( value.intValue() < 3 )
			{
				MonsterManuelManager.manuelEntries.remove( key );
				entryIterator.remove();
			}
		}
	}

	public static void reset( MonsterData monster )
	{
		// We just learned a factoid, so flush data for this monster
		if ( monster != null )
		{
			int id = monster.getId();
			if ( id > 0 )
			{
				MonsterManuelManager.manuelEntries.remove( id );
				MonsterManuelManager.manuelFactoidCounts.remove( id );
			}
		}
	}

	public static void registerMonster( final int id, final String text )
	{
		// See if this is a new entry

		String old = MonsterManuelManager.manuelEntries.get( id );
		if ( old != null && old.equals( text ) )
		{
			// We have seen this exact Manuel entry before.
			return;
		}

		// Either the entry is new or is different from what we have
		// saved; perhaps there are additional factoids.

		// Detach the entry from the page text and store in entry map
		String entry = new String( text );
		MonsterManuelManager.manuelEntries.put( id, entry );

		// Count the factoids and remember that, too.
		int factoids = MonsterManuelManager.countFactoids( entry );
		MonsterManuelManager.manuelFactoidCounts.put( id, factoids );

		// Extract some fields from the entry

		// If we are looking at this entry for the first time, do some checks.
		if ( old == null )
		{
			MonsterData monster = MonsterDatabase.findMonsterById( id );
			String name = MonsterManuelManager.extractMonsterName( entry );
			String image = MonsterManuelManager.extractMonsterImage( entry );
			String attackString = MonsterManuelManager.extractMonsterAttack( entry );
			String defenseString = MonsterManuelManager.extractMonsterDefense( entry );
			String hpString = MonsterManuelManager.extractMonsterHP( entry );
			String phylumString = MonsterManuelManager.extractMonsterPhylum( entry );
			Element element = MonsterManuelManager.extractMonsterElement( entry );
			String initiativeString = MonsterManuelManager.extractMonsterInitiative( entry );
			if ( monster == null )
			{
				// We don't know a monster with this ID. Add to monster ID map.
				String attributes = MonsterManuelManager.buildMonsterAttributes( attackString, defenseString, hpString, phylumString, element, initiativeString );

				RequestLogger.printLine( "New monster #" + id + " found in Manuel with name '" + name + "' image '" + image + "' attributes ='" + attributes + "'" );
				monster = MonsterDatabase.registerMonster( name, id, image, attributes );
				return;
			}

			// Check our data with what Manuel says
			if ( !monster.getManuelName().equals( name ) )
			{
				// We know this monster, but do not have the correct Manuel name
				RequestLogger.printLine( "Monster #" + id + " has name '" + monster.getManuelName() + "' but Manuel calls it '" + name + "'" );
				monster.setManuelName( name );
			}

			if ( !monster.hasImage( image ) )
			{
				RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has unrecognized image '" + image + "'" );
			}

			if ( attackString.equals( "?" ) )
			{
				// Scaling monster: either standard scaling or a formula
				if ( !( monster.scales() || monster.getBaseAttack() == -1 ) )
				{
					RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") scales, but KoLmafia doesn't");
				}
			}
			else
			{
				// Non-scaling monster
				int baseAttack = monster.getBaseAttack();
				int attack = StringUtilities.parseInt( attackString );
				if ( baseAttack != -1 && baseAttack != attack )
				{
					RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has attack " + attack + ", but KoLmafia says it is " + baseAttack );
				}
				int baseDefense = monster.getBaseDefense();
				int defense = StringUtilities.parseInt( defenseString );
				if ( baseDefense != -1 && baseDefense != defense )
				{
					RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has defense " + defense + ", but KoLmafia says it is " + baseDefense );
				}
				int baseHP = monster.getBaseHP();
				int hp = StringUtilities.parseInt( hpString );
				if ( baseHP != -1 && baseHP != hp )
				{
					RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has HP " + hp + ", but KoLmafia says it is " + baseHP );
				}
			}

			int baseInitiative = monster.getBaseInitiative();
			int initiative = MonsterManuelManager.parseInitiative( initiativeString );
			if ( baseInitiative != -1 && baseInitiative != initiative )
			{
				RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") '" + initiativeString +  "', but KoLmafia says it is " + baseInitiative );
			}

			Element attackElement = monster.getAttackElement();
			Element defenseElement = monster.getDefenseElement();
			if ( element != defenseElement )
			{
				RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has element " + element.toString() +  ", but KoLmafia says it has attack element " + attackElement.toString() + " and defense element " + defenseElement );
			}

			Phylum phylum = MonsterManuelManager.parsePhylum( phylumString );
			if ( phylum != monster.getPhylum() )
			{
				RequestLogger.printLine( "Manuel says that '" + name + "' (" + id + ") has phylum " + phylum + ", but KoLmafia says it is " + monster.getPhylum() );
			}
		}
	}

	public static int parseInitiative( final String initiativeString )
	{
		return  // Never wins initiative
			initiativeString.startsWith( "Never" ) ?
			-10000 :
			// Always wins initiative
			initiativeString.startsWith( "Always" ) ?
			10000 :
			// Initiative +100%
			StringUtilities.parseInt( initiativeString.substring( 12, initiativeString.length() - 1 ) );
	}
			       
	public static Phylum parsePhylum( final String phylum )
	{
		return MonsterDatabase.parsePhylum( phylum.toLowerCase() );
	}

	public static String buildMonsterAttributes( final String attack, final String defense, final String hp, final String phylum, final Element element, final String initiative )
	{
		StringBuilder buffer = new StringBuilder();
		if ( attack.equals( "?" ) )
		{
			// Attack/Defense/HP = ? means this is a scaling monster
			buffer.append( "Scale: ? Cap: ? Floor: ?" );
		}
		else
		{
			buffer.append( "Atk: " );
			buffer.append( StringUtilities.parseInt( attack ) );
			buffer.append( " Def: " );
			buffer.append( StringUtilities.parseInt( defense ) );
			buffer.append( " HP: " );
			buffer.append( StringUtilities.parseInt( hp ) );
		}

		buffer.append( " Init: " );
		buffer.append( String.valueOf( MonsterManuelManager.parseInitiative( initiative ) ) );

		if ( element != Element.NONE )
		{
			buffer.append( " E: " );
			buffer.append( element.toString() );
		}

		buffer.append( " P: " );
		buffer.append( MonsterManuelManager.parsePhylum( phylum ) );

		return buffer.toString();
	}

	// <td rowspan=4 valign=top class=small><b><font size=+2>A.M.C. gremlin</font></b>
	private static final Pattern NAME_PATTERN = Pattern.compile( "<td rowspan=4 valign=top class=small><b><font size=\\+2>(.*?)</font></b>" );
	public static String extractMonsterName( final String text )
	{
		Matcher matcher = MonsterManuelManager.NAME_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ).trim() : "";
	}

	// <td rowspan=4 valign=top width=100><img src=http://images.kingdomofloathing.com/adventureimages/gremlinamc.gif width=100></td>
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<td rowspan=4 valign=top width=100><img src=http://images.kingdomofloathing.com/(?:adventureimages/(?:\\.\\./)?)?(.*?\\.gif).*?</td>" );

	public static String extractMonsterImage( final String text )
	{
		Matcher matcher = MonsterManuelManager.IMAGE_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/nicesword.gif width=30 height=30 alt="Attack Power (approximate)" title="Attack Power (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>150</font></b></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/nicesword.gif width=30 height=30 alt="Attack Power (approximate)" title="Attack Power (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>?</font></b></td>
	private static final Pattern ATTACK_PATTERN = Pattern.compile( "Attack Power \\(approximate\\).*?<font size=\\+2>(.*?)</font>" );

	public static String extractMonsterAttack( final String text )
	{
		Matcher matcher = MonsterManuelManager.ATTACK_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/whiteshield.gif width=30 height=30 alt="Defense (approximate)" title="Defense (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>150</font></b></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/whiteshield.gif width=30 height=30 alt="Defense (approximate)" title="Defense (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>?</font></b></td>
	private static final Pattern DEFENSE_PATTERN = Pattern.compile( "Defense \\(approximate\\).*?<font size=\\+2>(.*?)</font>" );

	public static String extractMonsterDefense( final String text )
	{
		Matcher matcher = MonsterManuelManager.DEFENSE_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/hp.gif width=30 height=30 alt="Hit Points (approximate)" title="Hit Points (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>1000</font></b></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/hp.gif width=30 height=30 alt="Hit Points (approximate)" title="Hit Points (approximate)"></td><td width=50 valign=center align=left><b><font size=+2>?</font></b></td>
	private static final Pattern HP_PATTERN = Pattern.compile( "Hit Points \\(approximate\\).*?<font size=\\+2>(.*?)</font>" );

	public static String extractMonsterHP( final String text )
	{
		Matcher matcher = MonsterManuelManager.HP_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/beastflavor.gif alt="This monster is a Beast" title="This monster is a Beast" width=30 height=30></td>
	private static final Pattern PHYLUM_PATTERN = Pattern.compile( "This monster is (?:an? )?(.*?)\"" );

	public static String extractMonsterPhylum( final String text )
	{
		Matcher matcher = MonsterManuelManager.PHYLUM_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/circle.gif width=30 height=30 alt="This monster has no particular elemental alignment." title="This monster has no particular elemental alignment."></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/stench.gif width=30 height=30 alt="This monster is Stinky.  Stench is weak against Cold and Sleaze." title="This monster is Stinky.  Stench is weak against Cold and Sleaze."></td>
	private static final Pattern ELEMENT_PATTERN = Pattern.compile( "This monster is (Hot|Cold|Spooky|Stinky|Sleazy)" );

	public static Element extractMonsterElement( final String text )
	{
		Matcher matcher = MonsterManuelManager.ELEMENT_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return Element.NONE;
		}
		String element = matcher.group( 1 );
		return  element.equals( "Hot" ) ? Element.HOT :
			element.equals( "Cold" ) ? Element.COLD :
			element.equals( "Spooky" ) ? Element.SPOOKY :
			element.equals( "Stinky" ) ? Element.STENCH :
			element.equals( "Sleazy" ) ? Element.SLEAZE :
			Element.NONE;
	}

	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/snail.gif alt="Never wins initiative" title="Never wins initiative" width=30 height=30></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/lightningbolt.gif alt="Always wins initiative" title="Always wins initiative" width=30 height=30></td>
	// <td width=30><img src=http://images.kingdomofloathing.com/itemimages/watch.gif alt="Initiative +100%" title="Initiative +100%" width=30 height=30></td>
	private static final Pattern INITIATIVE_PATTERN = Pattern.compile( "\"(Never wins initiative|Always wins initiative|Initiative \\+.*?%)\"" );

	public static String extractMonsterInitiative( final String text )
	{
		Matcher matcher = MonsterManuelManager.INITIATIVE_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final Pattern FACTOIDS_PATTERN = Pattern.compile( "<ul>(.*?)</ul>", Pattern.DOTALL );
	private static final Pattern FACTOID_PATTERN = Pattern.compile( "<li>(.*?)(?=<li>|$)", Pattern.DOTALL );

	public static int countFactoids( final String text )
	{
		int count = 0;

		Matcher matcher = MonsterManuelManager.FACTOIDS_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			Matcher factoids = MonsterManuelManager.FACTOID_PATTERN.matcher( matcher.group( 1 )  );
			while ( factoids.find() )
			{
				count += 1;
			}
		}
		
		return count;
	}

	public static String getManuelText( final int id)
	{
		// If we don't know the ID, nothing to be done.
		if ( id <= 0 )
		{
			return "";
		}

		// See if we have it cached
		String text = MonsterManuelManager.manuelEntries.get( id );
		if ( text == null )
		{
			// No. Attempt to look up the monster in your quest log
			MonsterManuelRequest request = new MonsterManuelRequest( id );
			RequestThread.postRequest( request );
			text = MonsterManuelManager.manuelEntries.get( id );
		}

		return text == null ? "" : text;
	}

	public static int getFactoidsAvailable( final int id, final boolean cachedOnly )
	{
		// If we don't know the ID, nothing to be done.
		if ( id <= 0 )
		{
			return 0;
		}

		// See if we have it cached
		Integer factoids = MonsterManuelManager.manuelFactoidCounts.get( id );
		if ( factoids == null && !cachedOnly )
		{
			// No. Attempt to look up the monster in your quest log
			MonsterManuelRequest request = new MonsterManuelRequest( id );
			RequestThread.postRequest( request );
			factoids = MonsterManuelManager.manuelFactoidCounts.get( id );
		}

		return factoids == null ? 0 : factoids.intValue();
	}
}
