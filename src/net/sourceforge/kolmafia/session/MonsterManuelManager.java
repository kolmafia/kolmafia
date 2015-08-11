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

import net.sourceforge.kolmafia.request.MonsterManuelRequest;

public class MonsterManuelManager
{
	private static final Map<Integer, String> manuelEntries = new TreeMap<Integer, String>();
	private static final Map<Integer, Integer> manuelFactoidCounts = new TreeMap<Integer, Integer>();

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
		String name = MonsterManuelManager.extractMonsterName( entry );
		String image = MonsterManuelManager.extractMonsterImage( entry );

		// If we are looking at this entry for the first time, do some checks.
		if ( old == null )
		{
			MonsterData monster = MonsterDatabase.findMonsterById( id );
			if ( monster == null )
			{
				// We don't know a monster with this ID. Add to monster ID map.
				RequestLogger.printLine( "New monster #" + id + " found in Manuel with name '" + name + "' and image '" + image + "'" );
				monster = MonsterDatabase.registerMonster( name, id, image );
			}
			else if ( !monster.getManuelName().equals( name ) )
			{
				// We know this monster, but do not have the correct Manuel name
				RequestLogger.printLine( "Monster #" + id + " has name '" + monster.getManuelName() + "' but Manuel calls it '" + name + "'" );
				monster.setManuelName( name );
			}
		}
	}

	// <td rowspan=4 valign=top class=small><b><font size=+2>A.M.C. gremlin</font></b>
	private static final Pattern NAME_PATTERN = Pattern.compile( "<td rowspan=4 valign=top class=small><b><font size=\\+2>(.*?)</font></b>" );
	public static String extractMonsterName( final String text )
	{
		Matcher matcher = MonsterManuelManager.NAME_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ).trim() : "";
	}

	// <td rowspan=4 valign=top width=100><img src=http://images.kingdomofloathing.com/adventureimages/gremlinamc.gif width=100></td>
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<td rowspan=4 valign=top width=100>.*?adventureimages/(.*?\\.gif).*?</td>" );

	public static String extractMonsterImage( final String text )
	{
		Matcher matcher = MonsterManuelManager.IMAGE_PATTERN.matcher( text );
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

	public static int getFactoidsAvailable( final int id)
	{
		// If we don't know the ID, nothing to be done.
		if ( id <= 0 )
		{
			return 0;
		}

		// See if we have it cached
		Integer factoids = MonsterManuelManager.manuelFactoidCounts.get( id );
		if ( factoids == null )
		{
			// No. Attempt to look up the monster in your quest log
			MonsterManuelRequest request = new MonsterManuelRequest( id );
			RequestThread.postRequest( request );
			factoids = MonsterManuelManager.manuelFactoidCounts.get( id );
		}

		return factoids == null ? 0 : factoids.intValue();
	}
}
