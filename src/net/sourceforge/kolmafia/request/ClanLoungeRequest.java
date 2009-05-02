/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLoungeRequest
	extends GenericRequest
{
	private static final int SEARCH = 0;

	public static final int KLAW = 1;
	public static final int HOTTUB = 2;
	public static final int POOL_TABLE = 3;

	// Pool options
	public static final int AGGRESSIVE_STANCE = 1;
	public static final int STRATEGIC_STANCE = 2;
	public static final int STYLISH_STANCE = 3;

	private int action;
	private int option;

	/**
	 * Constructs a new <code>ClanLoungeRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private ClanLoungeRequest()
	{
		this( SEARCH );
	}

	public ClanLoungeRequest( final int action )
	{
		super( "clan_viplounge.php" );
		this.action = action;
	}

	public ClanLoungeRequest( final int action, final int option )
	{
		super( "clan_viplounge.php" );
		this.action = action;
		this.option = option;
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public static String equipmentName( final String urlString )
	{
		if ( urlString.indexOf( "klaw" ) != -1 )
		{
			return "Deluxe Mr. Klaw \"Skill\" Crane Game";
		}
		if ( urlString.indexOf( "hottub" ) != -1 )
		{
			return "Relaxing Hot Tub";
		}
		if ( urlString.indexOf( "pooltable" ) != -1 )
		{
			return "Pool Table";
		}
		return null;
	}

	private static String equipmentVisit( final String urlString )
	{
		String name = ClanLoungeRequest.equipmentName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in clan VIP lounge";
		}
		if ( urlString.indexOf( "poolgame" ) != -1 )
		{
			return "Playing a pool game in clan VIP lounge";
		}
		return null;
	}

	public void run()
	{
		switch ( this.action )
		{
		case ClanLoungeRequest.SEARCH:
			break;

		case ClanLoungeRequest.KLAW:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "klaw" );
			break;

		case ClanLoungeRequest.HOTTUB:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "hottub" );
			break;

		case ClanLoungeRequest.POOL_TABLE:
			this.constructURLString( "clan_viplounge.php" );
			if ( option != 0 )
			{
				this.addFormField( "action", "poolgame" );
				this.addFormField( "stance", String.valueOf( option ) );
			}
			else
			{
				this.addFormField( "action", "pooltable" );
			}
			break;

		default:
			break;
		}

		super.run();
	}

	public static void getBreakfast()
	{
		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.

		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.KLAW );
		RequestThread.postRequest( request );

		// You probably shouldn't play with this machine any more today
		// -- you wouldn't want to look greedy in front of the other
		// VIPs, would you?

		while ( request.responseText.indexOf( "want to look greedy" ) == -1 )
		{
			request.run();
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) )
		{
			return false;
		}

		String message = ClanLoungeRequest.equipmentVisit( urlString );

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
