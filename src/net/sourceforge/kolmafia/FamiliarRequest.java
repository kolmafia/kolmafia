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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FamiliarRequest extends KoLRequest
{
	private FamiliarData changeTo;
	private boolean isChangingFamiliar;
	private KoLCharacter characterData;

	public FamiliarRequest( KoLmafia client )
	{
		super( client, "familiar.php" );
		this.isChangingFamiliar = false;
		this.characterData = client.getCharacterData();
	}

	public FamiliarRequest( KoLmafia client, FamiliarData changeTo )
	{
		super( client, "familiar.php" );
		addFormField( "action", "newfam" );
		this.characterData = client.getCharacterData();

		this.changeTo = changeTo;
		addFormField( "newfam", String.valueOf( changeTo.getID() ) );
		this.isChangingFamiliar = true;
	}

	public String getFamiliarChange()
	{	return changeTo == null ? null : changeTo.toString();
	}

	public void run()
	{
		updateDisplay( DISABLED_STATE, "Retrieving familiar data..." );
		super.run();

		// There's nothing to parse if an error was encountered,
		// which could be either an error state or a redirection

		if ( isErrorState || responseCode != 200 )
			return;

		// Determine which familiars are present.

		int whichIndex;
		for ( int i = 1; i < 50; ++i )
		{
			whichIndex = responseText.indexOf( "<input type=radio name=newfam value=" + i );
			if ( whichIndex != -1 )
				characterData.addFamiliar( new FamiliarData( i, responseText.substring( whichIndex, responseText.indexOf( "</tr>", whichIndex ) ) ) );
		}

		// If there was a change, then make sure that the character
		// has an updated familiar on their display.

		if ( isChangingFamiliar )
		{
			characterData.setFamiliarDescription( changeTo.getRace(), changeTo.getWeight() + characterData.getAdditionalWeight() );
			characterData.setFamiliarItem( changeTo.getItem() );
			updateDisplay( NOCHANGE, "Familiar changed." );
		}
		else
		{
			Matcher currentMatcher = Pattern.compile( "Current Familiar.*?</b><br>(\\d+) pound (.*?) \\(\\d+ kills\\)<table><tr><td valign=center>Equipment:.*?<td valign=center>(.*?)</td>" ).matcher( responseText );
			if ( currentMatcher.find() )
			{
				characterData.setFamiliarDescription( currentMatcher.group(2).trim(), Integer.parseInt( currentMatcher.group(1) ) );
				characterData.setFamiliarItem( currentMatcher.group(3) );
			}
			else
			{
				currentMatcher = Pattern.compile( "Current Familiar.*?</b><br>(\\d+) pound (.*?) \\(\\d+ kills\\)<p>" ).matcher( responseText );
				if ( currentMatcher.find() )
					characterData.setFamiliarDescription( currentMatcher.group(2), Integer.parseInt( currentMatcher.group(1) ) );
			}

			updateDisplay( NOCHANGE, "Familiar data retrieved." );
		}
	}
}