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

import java.util.List;
import java.util.ArrayList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;

/**
 * An extension of <code>KoLRequest</code> which retrieves the character's
 * information from the server.  Note that this request only retrieves the
 * character's statistics at the current time; skills and effects will be
 * retrieved at a later date.  Equipment retrieval takes place through a
 * different request.
 */

public class CharpaneRequest extends KoLRequest
{
	private KoLCharacter character;

	public CharpaneRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charpane.php" );
		this.character = client.getCharacterData();
	}

	/**
	 * Runs the request.  Note that only the character's statistics
	 * are retrieved via this retrieval.
	 */

	public void run()
	{
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// By refreshing the character pane, you can
		// determine whether or not you are in compact
		// mode - be sure to refresh this value.

		KoLRequest.isCompactMode = responseText.indexOf( "<br>Lvl. " ) != -1;

		// The easiest way to retrieve the character pane
		// data is to use regular expressions.  But, the
		// only data that requires synchronization is the
		// modified stat values, health and mana.

		try
		{
			if ( isCompactMode )
				handleCompactMode();
			else
				handleExpandedMode();
		}
		catch ( Exception e )
		{
			logStream.println( e );
			e.printStackTrace( logStream );
		}
	}

	private void handleCompactMode() throws Exception
	{
		handleStatPoints( "Mus", "Mys", "Mox" );
		handleMiscPoints( "HP", "MP", "Meat", "Adv", "" );
	}

	private void handleExpandedMode() throws Exception
	{
		handleStatPoints( "Muscle", "Mysticality", "Moxie" );
		handleMiscPoints( "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;" );
	}

	private void handleStatPoints( String musString, String mysString, String moxString ) throws Exception
	{
		int [] modified = new int[3];

		Matcher statMatcher = Pattern.compile( musString + ".*?<b>(.*?)</b>.*?" + mysString + ".*?<b>(.*?)</b>.*?" + moxString + ".*?<b>(.*?)</b>" ).matcher( responseText );

		if ( statMatcher.find() )
		{
			for ( int i = 0; i < 3; ++i )
			{
				Matcher modifiedMatcher = Pattern.compile( "<font color=blue>(.*?)</font>&nbsp;\\((.*?)\\)" ).matcher(
					statMatcher.group( i + 1 ) );

				modified[i] = modifiedMatcher.find() ? df.parse( modifiedMatcher.group(1) ).intValue() :
					df.parse( statMatcher.group( i + 1 ) ).intValue();
			}

			character.setStatPoints( modified[0], character.getTotalMuscle(), modified[1],
				character.getTotalMysticality(), modified[2], character.getTotalMoxie() );
		}
	}

	private void handleMiscPoints( String hpString, String mpString, String meatString, String advString, String spacerString ) throws Exception
	{
		Matcher miscMatcher = Pattern.compile( hpString + ".*?<font.*?>(.*?)" + spacerString + "/" + spacerString + "(.*?)</font>.*?" +
			mpString + ".*?<b>(<font.*?>)?(.*?)" + spacerString + "/" + spacerString + "(.*?)(</font>)?</b>.*?" +
			meatString + ".*?<b>(<font.*?>)?(.*?)(</font>)?</b>.*?" + advString + ".*?<b>(<font.*?>)?(.*?)(</font>)?</b>" ).matcher( responseText );

		if ( miscMatcher.find() )
		{
			character.setHP( df.parse( miscMatcher.group(1) ).intValue(), df.parse( miscMatcher.group(2) ).intValue(),
				character.getBaseMaxHP() );

			character.setMP( df.parse( miscMatcher.group(4) ).intValue(), df.parse( miscMatcher.group(5) ).intValue(),
				character.getBaseMaxMP() );

			character.setAvailableMeat( df.parse( miscMatcher.group(8) ).intValue() );
			character.setAdventuresLeft( df.parse( miscMatcher.group(11) ).intValue() );

		}
	}
}
