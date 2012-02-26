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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.IntegerCache;

public class ShrineRequest
	extends GenericRequest
{
	public static final int BORIS = 1;
	public static final int JARLSBERG = 2;
	public static final int PETE = 3;

	public static final Object[][] SHRINE_DATA =
	{
		{
			IntegerCache.valueOf( ShrineRequest.BORIS ),
			"boris",
			"Statue of Boris",
			"heroDonationBoris",
			ItemPool.get( ItemPool.BORIS_KEY, 1 ),
		},
		{
			IntegerCache.valueOf( ShrineRequest.JARLSBERG ),
			"jarlsberg",
			"Statue of Jarlsberg",
			"heroDonationJarlsberg",
			ItemPool.get( ItemPool.JARLSBERG_KEY, 1 ),
		},
		{
			IntegerCache.valueOf( ShrineRequest.PETE ),
			"sneakypete",
			"Statue of Sneaky Pete",
			"heroDonationSneakyPete",
			ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 ),
		},
	};

	private static int dataId( final Object[] data )
	{
		return ( data == null ) ? 0 : ((Integer) data[0]).intValue();
	}

	private static String dataAction( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[1]);
	}

	private static String dataPlace( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[2]);
	}

	private static String dataSetting( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[3]);
	}

	private static AdventureResult dataKey( final Object[] data )
	{
		return ( data == null ) ? null : ((AdventureResult) data[4]);
	}

	private static Object[] idToData( final int id )
	{
		for ( int i = 0; i < SHRINE_DATA.length; ++i )
		{
			Object [] data = SHRINE_DATA[i];
			if ( id == dataId( data ) )
			{
				return data;
			}
		}
		return null;
	}

	private static Object[] actionToData( final String action )
	{
		for ( int i = 0; i < SHRINE_DATA.length; ++i )
		{
			Object [] data = SHRINE_DATA[i];
			if ( action.equals( dataAction( data ) ) )
			{
				return data;
			}
		}
		return null;
	}

	private static final String actionToPlace( final String action )
	{
		return dataPlace( actionToData( action ) );
	}

	private final int amount;
	private boolean hasStatueKey;

	/**
	 * Constructs a new <code>ShrineRequest</code>.
	 *
	 * @param heroId The identifier for the hero to whom you are donating
	 * @param amount The amount you're donating to the given hero
	 */

	public ShrineRequest( final int heroId, final int amount )
	{
		super( "da.php" );

		Object [] data = idToData( heroId );
		AdventureResult key = null;

		if ( data != null )
		{
			this.addFormField( "action", dataAction( data ) );
			key = dataKey( data );
		}
		this.hasStatueKey = key != null && KoLConstants.inventory.contains( key );

		this.addFormField( "howmuch", String.valueOf( amount ) );
		this.amount = amount;
	}

	/**
	 * Runs the request. Note that this does not report an error if it
	 * fails; it merely parses the results to see if any gains were made.
	 */

	public void run()
	{
		if ( !this.hasStatueKey )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the appropriate key." );
			return;
		}
		super.run();
	}

	public void processResults()
	{
                String error = ShrineRequest.parseResponse( this.getURLString(), this.responseText );
                if ( error != null )
                {
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, error );
			return;
                }
		KoLmafia.updateDisplay( "Donation complete." );
	}

	public static final String parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "da.php" ) )
		{
			return null;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return null;
		}

		int qty = GenericRequest.getHowMuch( urlString );
		if ( qty < 0 )
		{
			return null;
		}

		Object [] data = actionToData( action );
		if ( data == null )
		{
			return null;
		}

		// If we get here, we tried donating

		String preference = dataSetting( data );

		if ( responseText.indexOf( "You gain" ) == -1 )
		{
			return responseText.indexOf( "That's not enough" ) == -1 ?
				"Donation limit exceeded." :
				"Donation must be larger.";
		}

		ResultProcessor.processMeat( 0 - qty );
		Preferences.increment( preference, qty );

		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "da.php" ) )
		{
			return false;
		}

		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		String place = ShrineRequest.actionToPlace( action );

		if ( place == null )
		{
			return false;
		}

		int qty = GenericRequest.getHowMuch( urlString );
		if ( qty < 0 )
		{
			return false;
		}

		String message = "Donating " + qty + " Meat to the " + place;

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
