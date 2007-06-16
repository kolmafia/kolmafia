/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

public class CampgroundRequest extends KoLRequest
{
	private String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		super( "campground.php" );
		this.action = "";
	}

	/**
	 * Constructs a new <code>CampgroundRequest</code> with the
	 * specified action in mind.
	 */

	public CampgroundRequest( String action )
	{
		super( "campground.php" );
		this.addFormField( "action", action );
		this.action = action;
	}

	/**
	 * Runs the campground request, updating theas appropriate.
	 */

	public void run()
	{
		if ( this.action.equals( "relax" ) && KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() )
			return;

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( this.responseCode != 200 )
			return;

		// For now, just set whether or not you have a
		// bartender and chef.

	}

	public void processResults()
	{
		KoLCharacter.setChef( this.responseText.indexOf( "cook.php" ) != -1 );
		KoLCharacter.setBartender( this.responseText.indexOf( "cocktail.php" ) != -1 );
		KoLCharacter.setToaster( this.responseText.indexOf( "action=toast" ) != -1 );
		KoLCharacter.setArches( this.responseText.indexOf( "action=arches" ) != -1 );

		// Update adventure tally for resting and relaxing
		// at the campground.

		if ( this.action.equals( "rest" ) )
		{
			if ( this.responseText.indexOf( "You sleep" ) == -1 )
				KoLmafia.updateDisplay( ERROR_STATE, "Could not rest." );
		}
		else if ( this.action.equals( "relax" ) )
		{
			if ( this.responseText.indexOf( "You relax" ) == -1 )
				KoLmafia.updateDisplay( ERROR_STATE, "Could not relax." );
		}

		// Make sure that the character received something if
		// they were looking for toast

		if ( this.action.equals( "toast" ) && this.responseText.indexOf( "acquire" ) == -1 )
			KoLmafia.updateDisplay( PENDING_STATE, "No more toast left." );
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
	{	return this.responseCode != 200 || (!this.action.equals( "rest" ) && !this.action.equals( "relax" )) ? 0 : 1;
	}
}

