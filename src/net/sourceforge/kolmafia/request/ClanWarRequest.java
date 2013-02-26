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

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

public class ClanWarRequest
	extends GenericRequest
	implements Comparable<ClanWarRequest>
{
	private static final Pattern CLANID_PATTERN =
		Pattern.compile( "name=whichclan value=(\\d+)></td><td><b>([^<]+)</td><td>([\\d]+)</td>" );
	private static final Pattern WAIT_PATTERN = Pattern.compile( "<br>Your clan can attack again in (.*?)<p>" );

	private static final SortedListModel enemyClans = new SortedListModel();
	private static String nextAttack = null;

	private final String name;
	private final boolean isPurchase;

	public ClanWarRequest()
	{
		super( "clan_attack.php" );
		this.name = null;
		this.isPurchase = false;
	}

	private ClanWarRequest( final String id, final String name )
	{
		super( "clan_attack.php" );
		this.addFormField( "whichclan", id );

		this.name = name;
		this.isPurchase = false;
	}

	public ClanWarRequest( final int goodies, final int oatmeal, final int recliners, final int grunts,
		final int flyers, final int archers )
	{
		super( "clan_war.php" );

		this.name = null;
		this.isPurchase = true;

		this.addFormField( "action", "Yep." );
		this.addFormField( "goodies", String.valueOf( goodies ) );
		this.addFormField( "oatmeal", String.valueOf( oatmeal ) );
		this.addFormField( "recliners", String.valueOf( recliners ) );
		this.addFormField( "grunts", String.valueOf( grunts ) );
		this.addFormField( "flyers", String.valueOf( flyers ) );
		this.addFormField( "archers", String.valueOf( archers ) );
	}

	@Override
	public void run()
	{
		if ( this.getPath().equals( "clan_attack.php" ) )
		{
			if ( this.name == null )
			{
				KoLmafia.updateDisplay( "Retrieving clan attack state..." );
			}
			else
			{
				KoLmafia.updateDisplay( "Attacking " + this.name + "..." );
			}
		}

		super.run();
	}

	public static final String getNextAttack()
	{
		return ClanWarRequest.nextAttack == null ? "You may attack right now." : ClanWarRequest.nextAttack;
	}

	public static final SortedListModel getEnemyClans()
	{
		return ClanWarRequest.enemyClans;
	}

	@Override
	public void processResults()
	{
		if ( this.isPurchase || this.name != null )
		{
			return;
		}

		ClanWarRequest.nextAttack = null;

		if ( this.getPath().equals( "clan_attack.php" ) )
		{
			parseTargets();
		}
		else
		{
			parseWaitTime();
		}
	}

	private void parseTargets()
	{
		ClanWarRequest.enemyClans.clear();

		int bagCount = 0;
		Matcher clanMatcher = ClanWarRequest.CLANID_PATTERN.matcher( this.responseText );

		while ( clanMatcher.find() )
		{
			bagCount = Integer.parseInt( clanMatcher.group( 3 ) );
			if ( bagCount == 1 )
			{
				ClanWarRequest.enemyClans.add( new ClanWarRequest(
					clanMatcher.group( 1 ), clanMatcher.group( 2 ) ) );
			}
		}

		if ( ClanWarRequest.enemyClans.isEmpty() )
		{
			this.constructURLString( "clan_war.php" ).run();
			return;
		}

		Preferences.setBoolean( "clanAttacksEnabled", true );

		if ( ClanWarRequest.enemyClans.getSize() > 0 )
		{
			ClanWarRequest.enemyClans.setSelectedIndex( KoLConstants.RNG.nextInt( ClanWarRequest.enemyClans.getSize() ) );
		}
	}

	private void parseWaitTime()
	{
		Matcher nextMatcher = ClanWarRequest.WAIT_PATTERN.matcher( this.responseText );
		if ( nextMatcher.find() )
		{
			ClanWarRequest.nextAttack = "You may attack again in " + nextMatcher.group( 1 );
			Preferences.setBoolean( "clanAttacksEnabled", true );
		}
		else
		{
			Preferences.setBoolean( "clanAttacksEnabled", false );
			ClanWarRequest.nextAttack = "You do not have the ability to attack.";
		}

		KoLmafia.updateDisplay( ClanWarRequest.nextAttack );
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public int compareTo( final ClanWarRequest car )
	{
		return this.name.compareToIgnoreCase( car.name );
	}
}
