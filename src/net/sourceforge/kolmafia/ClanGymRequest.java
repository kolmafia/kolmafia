/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * exercising in the clan gym.
 */

public class ClanGymRequest extends KoLRequest
{
	private static final int BREAKFAST = -1;
	public static final int SEARCH = 0;

	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;
	public static final int SOFA = 4;

	public static int [][] MAXIMUM_USAGE = new int[10][];
	static
	{
		MAXIMUM_USAGE[0] = new int[0];
		MAXIMUM_USAGE[1] = new int [] { 0, 0, 0, 0, 1 };
		MAXIMUM_USAGE[2] = new int [] { 0, 0, 0, 0 };
		MAXIMUM_USAGE[3] = new int [] { 0, 3, 0, 3 };
		MAXIMUM_USAGE[4] = new int [] { 0, 0, 1, 0 };
		MAXIMUM_USAGE[5] = new int [] { 0, 0, 0, 0 };
		MAXIMUM_USAGE[6] = new int[0];
		MAXIMUM_USAGE[7] = new int[0];
		MAXIMUM_USAGE[8] = new int[0];
		MAXIMUM_USAGE[9] = new int [] { 0, 0, 3, 1 };
	}

	private int turnCount;
	private int equipmentId;

	/**
	 * Constructs a new <code>ClanGymRequest</code>.
	 * @param	equipmentId	The identifier for the equipment you're using
	 */

	public ClanGymRequest( int equipmentId )
	{
		super( "clan_rumpus.php" );
		this.equipmentId = equipmentId;
	}

	private static String chooseGym( int equipmentId )
	{
		switch ( equipmentId )
		{
		case MUSCLE:
			// If we are in a Muscle sign, Degrassi Knoll has a gym.
			if ( KoLCharacter.inMuscleSign() )
				return "knoll.php";

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		case MYSTICALITY:
			// If we are in a Mysticality sign, Canadia has a gym.
			if ( KoLCharacter.inMysticalitySign() )
				return "canadia.php";

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		case MOXIE:
			// If we are in a Moxie sign, the Gnomish Gnomads has a gym.
			if ( KoLCharacter.inMoxieSign() )
				return "gnomes.php";

			// Otherwise, use the one in our clan - if we're in one.
			return "clan_rumpus.php";

		default:
			return "clan_rumpus.php";
		}
	}

	private static String chooseAction( int equipmentId )
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
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public ClanGymRequest setTurnCount( int turnCount )
	{
		this.turnCount = turnCount;
		return this;
	}

	public void run()
	{
		constructURLString( chooseGym( equipmentId ) );

		switch ( equipmentId )
		{
		case SEARCH:
		case BREAKFAST:
			break;

		case SOFA:
			addFormField( "preaction", "nap" );
			break;

		default:
			String equipment = chooseAction( equipmentId );
			if ( equipment == null )
				return;

			if ( equipment.length() > 1 )
			{
				addFormField( "action", equipment );
			}
			else
			{
				addFormField( "preaction", "gym" );
				addFormField( "whichgym", equipment );
			}

			break;
		}

		if ( turnCount > 0 )
		{
			addFormField( "numturns", String.valueOf( turnCount ) );

			if ( KoLCharacter.getAdventuresLeft() < turnCount )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient adventures." );
				return;
			}
		}

		if ( equipmentId != SEARCH )
			KoLmafia.updateDisplay( "Executing request..." );

		super.run();
	}

	protected void processResults()
	{
		if ( equipmentId != SEARCH )
		{
			if ( equipmentId != BREAKFAST )
				KoLmafia.updateDisplay( "Workout completed." );

			return;
		}

		equipmentId = BREAKFAST;

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.

		if ( responseText.indexOf( "rump3_3.gif" ) != -1 )
		{
			clearDataFields();
			addFormField( "action", "click" );
			addFormField( "spot", "3" );
			addFormField( "furni", "3" );

			for ( int i = 0; i < 3; ++i )
			{
				KoLmafia.updateDisplay( "Attempting to win a prize (" + (i+1) + " of 3)..." );
				if ( responseText.indexOf( "wisp of smoke" ) == -1 && responseText.indexOf( "broken down" ) == -1 )
					super.run();
			}
		}

		if ( !KoLCharacter.canInteract() )
			return;

		for ( int i = 0; i < MAXIMUM_USAGE.length; ++i )
		{
			for ( int j = 0; j < MAXIMUM_USAGE[i].length; ++j )
			{
				if ( i == 3 && j == 3 )
					continue;

				// If the equipment is not present, then go ahead and
				// skip this check.

				if ( MAXIMUM_USAGE[i][j] == 0 || responseText.indexOf( "rump" + i + "_" + j + ".gif" ) == -1 )
					continue;

				clearDataFields();

				if ( i == 9 && j == 2 )
				{
					addFormField( "preaction", "buychips" );
					addFormField( "whichbag", String.valueOf( RNG.nextInt(3) + 1 ) );
				}
				else
				{
					addFormField( "action", "click" );
					addFormField( "spot", String.valueOf(i) );
					addFormField( "furni", String.valueOf(j) );
				}

				for ( int k = 0; k < MAXIMUM_USAGE[i][j]; ++k )
					super.run();
			}
		}
	}

	/**
	 * An alternative method to doing adventure calculation is determining
	 * how many adventures are used by the given request, and subtract
	 * them after the request is done.  This number defaults to <code>zero</code>;
	 * overriding classes should change this value to the appropriate
	 * amount.
	 *
	 * @return	The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{	return turnCount;
	}
}
