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

public class AscensionDataRequest extends KoLRequest
{
	private String playerName;
	private String playerID;
	private List ascensionData;

	public AscensionDataRequest( KoLmafia client, String playerName )
	{
		super( client, "ascensionhistory.php" );
		addFormField( "back", "self" );

		if ( client != null )
			addFormField( "who", client.getPlayerID( playerName ) );

		this.playerName = playerName;

		if ( client != null )
			this.playerID = client.getPlayerID( playerName );

		this.ascensionData = new ArrayList();
	}

	public void run()
	{
		super.run();
		responseText = responseText.replaceAll( "<a.*?</a>", "" );
		refreshFields();
	}

	/**
	 * Internal method used to refresh the fields of the profile
	 * request based on the response text.  This should be called
	 * after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		ascensionData.clear();
		Matcher fieldMatcher = Pattern.compile( "<tr.*?</tr>" ).matcher( responseText );

		while ( fieldMatcher.find() )
			ascensionData.add( new AscensionDataField( playerName, playerID, fieldMatcher.group() ) );
	}

	/**
	 * Static method used by the clan manager in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static AscensionDataRequest getInstance( String playerName, String responseText )
	{
		AscensionDataRequest instance = new AscensionDataRequest( null, playerName );

		instance.responseText = responseText;
		instance.refreshFields();

		return instance;
	}

	public String getPlayerName()
	{	return playerName;
	}

	public String getPlayerID()
	{	return playerID;
	}

	public void initialize()
	{
		if ( responseText == null )
			this.run();
	}

	public static class AscensionDataField
	{
		public AscensionDataField( String playerName, String playerID, String rowData )
		{
		}
	}
}
