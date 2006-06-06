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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A special class used to determine the current moon phase.
 * Theoretically, the calculations are simple enough to be
 * an internal class elsewhere, but because it makes everything
 * cleaner to do things this way, so it goes.
 */

public class MoonPhaseRequest extends KoLRequest
{
	/**
	 * The phases of the moons can be retrieved from the top menu,
	 * which varies based on whether or not the player is using
	 * compact mode.
	 */

	public MoonPhaseRequest( KoLmafia client )
	{	super( client, isCompactMode ? "compactmenu.php" : "topmenu.php" );
	}

	/**
	 * Runs the moon phase request, updating the client as appropriate.
	 */

	public void run()
	{
		KoLmafia.updateDisplay( "Synchronizing moon data..." );
		super.run();
	}

	protected void processResults()
	{
		// Get current phase of Ronald and Grimace

		Matcher moonMatcher = Pattern.compile( "moon(.)\\.gif.*moon(.)\\.gif" ).matcher( responseText );
		if ( moonMatcher.find() )
			MoonPhaseDatabase.setMoonPhases( Integer.parseInt( moonMatcher.group(1) ) - 1, Integer.parseInt( moonMatcher.group(2) ) - 1 );
	}
}
