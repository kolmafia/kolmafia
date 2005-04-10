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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class MuseumRequest extends KoLRequest
{
	private int requestType;
	private String outfitName;

	public MuseumRequest( KoLmafia client )
	{
		super( client, "managecollection.php" );
	}

	public void run()
	{
		super.run();

		// If you changed your outfit, there will be a redirect
		// to the equipment page - therefore, do so.

		Matcher displayMatcher = Pattern.compile( "<b>Take:.*?</select>" ).matcher( replyContent );
		if ( displayMatcher.find() )
		{
			String content = displayMatcher.group();
			List resultList = client.getCharacterData().getCollection();
			resultList.clear();

			int lastFindIndex = 0;
			Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( content );
			while ( optionMatcher.find() )
			{
				try
				{
					lastFindIndex = optionMatcher.end();
					int itemID = df.parse( optionMatcher.group(1) ).intValue();

					AdventureResult result =
						TradeableItemDatabase.getItemName( itemID ) != null ?
							new AdventureResult( itemID, df.parse( optionMatcher.group(3) ).intValue() ) :
								new AdventureResult( optionMatcher.group(2).trim(), df.parse( optionMatcher.group(3) ).intValue() );

					AdventureResult.addResultToList( resultList, result );
				}
				catch ( Exception e )
				{
					// If an exception occurs during the parsing, just
					// continue after notifying the LogStream of the
					// error.  This could be handled better, but not now.

					logStream.println( e );
					e.printStackTrace( logStream );
				}
			}
		}
	}
}