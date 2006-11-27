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
	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;
	public static final int SOFA = 4;

	private int turnCount;
	private int equipmentId;

	/**
	 * Constructs a new <code>ClanGymRequest</code>.
	 *
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

		KoLmafia.updateDisplay( "Executing request..." );
		super.run();
	}

	protected void processResults()
	{	KoLmafia.updateDisplay( "Workout completed." );
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
