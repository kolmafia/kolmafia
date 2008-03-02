/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

public class ClanRumpusRequest
	extends GenericRequest
{
	private static final int BREAKFAST = -1;
	public static final int SEARCH = 0;

	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;
	public static final int SOFA = 4;

	private static final Pattern TURN_PATTERN = Pattern.compile( "numturns=(\\d+)" );

	public static final int[][] MAXIMUM_USAGE = new int[ 10 ][];
	static
	{
		ClanRumpusRequest.MAXIMUM_USAGE[ 0 ] = new int[ 0 ];
		ClanRumpusRequest.MAXIMUM_USAGE[ 1 ] = new int[] { 0, 0, 0, 0, 1 };
		ClanRumpusRequest.MAXIMUM_USAGE[ 2 ] = new int[] { 0, 0, 0, 0 };
		ClanRumpusRequest.MAXIMUM_USAGE[ 3 ] = new int[] { 0, 3, 0, 3 };
		ClanRumpusRequest.MAXIMUM_USAGE[ 4 ] = new int[] { 0, 1, 1, 0 };
		ClanRumpusRequest.MAXIMUM_USAGE[ 5 ] = new int[] { 0, 0, 0, 0 };
		ClanRumpusRequest.MAXIMUM_USAGE[ 6 ] = new int[ 0 ];
		ClanRumpusRequest.MAXIMUM_USAGE[ 7 ] = new int[ 0 ];
		ClanRumpusRequest.MAXIMUM_USAGE[ 8 ] = new int[ 0 ];
		ClanRumpusRequest.MAXIMUM_USAGE[ 9 ] = new int[] { 0, 0, 3, 1 };
	}

	private int turnCount;
	private int equipmentId;

	/**
	 * Constructs a new <code>ClanRumpusRequest</code>.
	 *
	 * @param equipmentId The identifier for the equipment you're using
	 */

	public ClanRumpusRequest( final int equipmentId )
	{
		super( "clan_rumpus.php" );
		this.equipmentId = equipmentId;
	}

	private static final String chooseGym( final int equipmentId )
	{
		switch ( equipmentId )
		{
		case MUSCLE:
			// If we are in a Muscle sign, Degrassi Knoll has a gym.
			if ( KoLCharacter.inMuscleSign() )
			{
				return "knoll.php";
			}

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		case MYSTICALITY:
			// If we are in a Mysticality sign, Canadia has a gym.
			if ( KoLCharacter.inMysticalitySign() )
			{
				return "canadia.php";
			}

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		case MOXIE:
			// If we are in a Moxie sign, the Gnomish Gnomads has a gym.
			if ( KoLCharacter.inMoxieSign() )
			{
				return "gnomes.php";
			}

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		default:
			return "clan_rumpus.php";
		}
	}

	private static final String chooseAction( final int equipmentId )
	{
		switch ( equipmentId )
		{
		case MUSCLE:
			// If we are in a Muscle sign, Degrassi Knoll has a gym.
			return KoLCharacter.inMuscleSign() ? "gym" : "3";

		case MYSTICALITY:
			// If we are in a Mysticality sign, Canadia has a gym.
			return KoLCharacter.inMysticalitySign() ? "institute" : "1";

		case MOXIE:
			// If we are in a Moxie sign, the Gnomish Gnomads has a gym.
			return KoLCharacter.inMoxieSign() ? "train" : "2";

		case SOFA:
			return "5";
		}

		return null;
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public ClanRumpusRequest setTurnCount( final int turnCount )
	{
		this.turnCount = turnCount;
		return this;
	}

	public void run()
	{
		this.constructURLString( ClanRumpusRequest.chooseGym( this.equipmentId ) );

		switch ( this.equipmentId )
		{
		case SEARCH:
		case BREAKFAST:
			break;

		case SOFA:
			this.addFormField( "preaction", "nap" );
			break;

		default:
			String equipment = ClanRumpusRequest.chooseAction( this.equipmentId );
			if ( equipment == null )
			{
				return;
			}

			if ( equipment.length() > 1 )
			{
				this.addFormField( "action", equipment );
			}
			else
			{
				this.addFormField( "preaction", "gym" );
				this.addFormField( "whichgym", equipment );
			}

			break;
		}

		if ( this.turnCount > 0 )
		{
			this.addFormField( "numturns", String.valueOf( this.turnCount ) );

			if ( KoLCharacter.getAdventuresLeft() < this.turnCount )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Insufficient adventures." );
				return;
			}
		}

		if ( this.equipmentId != ClanRumpusRequest.SEARCH )
		{
			KoLmafia.updateDisplay( "Executing request..." );
		}

		super.run();
	}

	public void processResults()
	{
		if ( this.equipmentId != ClanRumpusRequest.SEARCH )
		{
			if ( this.equipmentId != ClanRumpusRequest.BREAKFAST )
			{
				KoLmafia.updateDisplay( "Workout completed." );
			}

			return;
		}

		this.equipmentId = ClanRumpusRequest.BREAKFAST;

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.

		if ( this.responseText.indexOf( "rump3_3.gif" ) != -1 )
		{
			this.clearDataFields();
			this.addFormField( "action", "click" );
			this.addFormField( "spot", "3" );
			this.addFormField( "furni", "3" );

			for ( int i = 0; i < 3; ++i )
			{
				KoLmafia.updateDisplay( "Attempting to win a prize (" + ( i + 1 ) + " of 3)..." );
				if ( this.responseText.indexOf( "wisp of smoke" ) == -1 && this.responseText.indexOf( "broken down" ) == -1 )
				{
					super.run();
				}
			}
		}

		if ( !KoLCharacter.canInteract() )
		{
			return;
		}

		for ( int i = 0; i < ClanRumpusRequest.MAXIMUM_USAGE.length; ++i )
		{
			for ( int j = 0; j < ClanRumpusRequest.MAXIMUM_USAGE[ i ].length; ++j )
			{
				if ( i == 3 && j == 3 )
				{
					continue;
				}

				// If the equipment is not present, then go ahead and
				// skip this check.

				if ( ClanRumpusRequest.MAXIMUM_USAGE[ i ][ j ] == 0 || this.responseText.indexOf( "rump" + i + "_" + j + ".gif" ) == -1 )
				{
					continue;
				}

				this.clearDataFields();

				if ( i == 9 && j == 2 )
				{
					this.addFormField( "preaction", "buychips" );
					this.addFormField( "whichbag", String.valueOf( KoLConstants.RNG.nextInt( 3 ) + 1 ) );
				}
				else
				{
					this.addFormField( "action", "click" );
					this.addFormField( "spot", String.valueOf( i ) );
					this.addFormField( "furni", String.valueOf( j ) );
				}

				for ( int k = 0; k < ClanRumpusRequest.MAXIMUM_USAGE[ i ][ j ]; ++k )
				{
					super.run();
				}
			}
		}
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done. This number defaults to <code>zero</code>; overriding
	 * classes should change this value to the appropriate amount.
	 *
	 * @return The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		return this.turnCount;
	}

	public static boolean registerRequest( final String urlString )
	{
		String gymType = null;

		if ( urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=gym" ) != -1 )
		{
			gymType = "Pump Up Muscle";
		}

		if ( urlString.startsWith( "canadia.php" ) && urlString.indexOf( "action=institute" ) != -1 )
		{
			gymType = "Pump Up Mysticality";
		}

		if ( urlString.startsWith( "gnomes.php" ) && urlString.indexOf( "action=train" ) != -1 )
		{
			gymType = "Pump Up Moxie";
		}

		if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "action=3" ) != -1 )
		{
			gymType = "Pump Up Muscle";
		}

		if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "action=1" ) != -1 )
		{
			gymType = "Pump Up Mysticality";
		}

		if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "action=2" ) != -1 )
		{
			gymType = "Pump Up Moxie";
		}

		if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "action=5" ) != -1 )
		{
			gymType = "Rest in Clan Sofa";
		}

		if ( gymType == null )
		{
			return false;
		}

		Matcher turnMatcher = ClanRumpusRequest.TURN_PATTERN.matcher( urlString );
		if ( !turnMatcher.find() )
		{
			return false;
		}

		RequestLogger.printLine( "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turnMatcher.group( 1 ) + " turns)" );
		RequestLogger.updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turnMatcher.group( 1 ) + " turns)" );
		return true;
	}
}
