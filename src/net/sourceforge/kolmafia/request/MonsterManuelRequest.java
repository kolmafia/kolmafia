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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.session.MonsterManuelManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterManuelRequest
	extends GenericRequest
{
	public MonsterManuelRequest( final String page )
	{
		// questlog.php?which=6&vl=a
		super( "questlog.php" );
		this.addFormField( "which", "6" );
		if ( page != null )
		{
			this.addFormField( "vl", page );
		}
	}

	public MonsterManuelRequest( final int id )
	{
		this( MonsterManuelRequest.getManuelPage( id ) );
	}

	public static String getManuelPage( final int id )
	{
		MonsterData monster = MonsterDatabase.findMonsterById( id );
		if ( monster == null )
		{
			return null;
		}

		String name = monster.getManuelName();
		if ( name == null || name.length() < 1 )
		{
			return null;
		}

		char first = name.charAt( 0 );
		return Character.isLetter( first ) ? String.valueOf( Character.toLowerCase( first ) ) : "-";
	}

	private static final Pattern MONSTER_ENTRY_PATTERN = Pattern.compile( "<a name='mon(\\d+)'>.*?</table>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "questlog.php" ) || !urlString.contains( "which=6" ) )
		{
			return;
		}

		// Parse the page and register each Manuel entry
		Matcher matcher = MonsterManuelRequest.MONSTER_ENTRY_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group( 1 ) );
			MonsterManuelManager.registerMonster( id, matcher.group( 0 ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "questlog.php" ) || !urlString.contains( "which=6" ) )
		{
			return false;
		}

		// Claim but don't log Monster Manuel visits
		return true;
	}
}
