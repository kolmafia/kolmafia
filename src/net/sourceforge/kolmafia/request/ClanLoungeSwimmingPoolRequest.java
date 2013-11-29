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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class ClanLoungeSwimmingPoolRequest
	extends GenericRequest
{
	// Default to GET_OUT
	public static final int HANDSTAND = 1;
	public static final int GET_OUT = 2;
	public static final int SAY = 3;
	public static final int CLOSE_EYES= 4;
	public static final int TREASURE = 5;

	private int action;

	private static final Pattern SWIMMING_POOL_PATTERN = Pattern.compile( "var state =.*found a ([\\w-&; ]*)!\\\"" );

	/**
	 * Constructs a new <code>ClanLoungeRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	public ClanLoungeSwimmingPoolRequest()
	{
		this( GET_OUT );
	}

	public ClanLoungeSwimmingPoolRequest( final int action )
	{
		super( "choice.php" );
		this.action = action;
	}

	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	@Override
	public void run()
	{
		switch ( this.action )
		{
		case ClanLoungeSwimmingPoolRequest.HANDSTAND:
			RequestLogger.printLine( "In the pool, flipping over." );

			this.constructURLString( "choice.php" );
			this.addFormField( "whichchoice", "585" );
			this.addFormField( "option", "1" );
			this.addFormField( "action", "flip" );
			break;

		case ClanLoungeSwimmingPoolRequest.GET_OUT:
			RequestLogger.printLine( "Getting out of the pool." );

			this.constructURLString( "choice.php" );
			this.addFormField( "whichchoice", "585" );
			this.addFormField( "option", "1" );
			this.addFormField( "action", "leave" );
			break;

		case ClanLoungeSwimmingPoolRequest.SAY:
			RequestLogger.printLine( "In the pool, saying..." );

			this.constructURLString( "choice.php" );
			this.addFormField( "whichchoice", "585" );
			this.addFormField( "option", "1" );
			this.addFormField( "action", "say" );
			//this.addFormField( "say", "" );
			break;

		case ClanLoungeSwimmingPoolRequest.CLOSE_EYES:
			RequestLogger.printLine( "In the pool, closing your eyes." );

			this.constructURLString( "choice.php" );
			this.addFormField( "whichchoice", "585" );
			this.addFormField( "option", "1" );
			this.addFormField( "action", "blink" );
			break;

		case ClanLoungeSwimmingPoolRequest.TREASURE:
			RequestLogger.printLine( "In the pool, diving for treasure." );

			this.constructURLString( "choice.php" );
			this.addFormField( "whichchoice", "585" );
			this.addFormField( "option", "1" );
			this.addFormField( "action", "treasure" );
			break;

		default:
			break;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		ClanLoungeSwimmingPoolRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=585" ) || responseText == null )
		{
			return;
		}
		
		// Only match when diving for treasure, to avoid false positives when others are chatting
		if ( urlString.indexOf( "action=treasure" ) != -1 )
		{
			Matcher swimmingPoolMatcher = SWIMMING_POOL_PATTERN.matcher( responseText );
			if ( swimmingPoolMatcher.find() )
			{
				RequestLogger.printLine( "You found a " + swimmingPoolMatcher.group(1) + " in the VIP pool!" );

				AdventureResult result = ItemPool.get( swimmingPoolMatcher.group(1), 1 );
				ResultProcessor.processItem( result.getItemId(), 1);

				Preferences.setBoolean( "_olympicSwimmingPoolItemFound", true );
			}
		}
	}

	private static String actionVisit( final String urlString )
	{
		String actionDescription = null;
		
		if ( urlString.indexOf( "action=flip" ) != -1 )
		{
			actionDescription = "Doing handstand in";
		}
		if ( urlString.indexOf( "action=leave" ) != -1 )
		{
			actionDescription = "Getting out of";
		}
		if ( urlString.indexOf( "action=say" ) != -1 )
		{
			actionDescription = "Saying something in";
		}
		if ( urlString.indexOf( "action=blink" ) != -1 )
		{
			actionDescription = "Blinking in";
		}
		if ( urlString.indexOf( "action=treasure" ) != -1 )
		{
			actionDescription = "Diving for treasure in";
		}

		if ( actionDescription != null )
		{
			return actionDescription + " clan VIP swimming pool";
		}
		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) || urlString.indexOf( "whichchoice=585" ) == -1 )
		{
			return false;
		}

		String message = ClanLoungeSwimmingPoolRequest.actionVisit( urlString );

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		return false;
	}
}
