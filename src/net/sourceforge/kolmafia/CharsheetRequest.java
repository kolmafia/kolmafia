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
import java.util.StringTokenizer;

public class CharsheetRequest extends KoLRequest
{
	public CharsheetRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charsheet.php" );
	}

	public void run()
	{
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode == 302 )
			return;

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		StringTokenizer parsedContent = new StringTokenizer(
			replyContent.replaceAll( "<[^>]*>", "\n" ), "\n" );

		// The first token in the stream contains the character's
		// name, but so does the second - discard the first so
		// that the code can appear in blocks.

		skipTokens( parsedContent, 1 );

		// The next four tokens contain the character's name and
		// other things which identify the character.

		String name = parsedContent.nextToken();
		int userID = intToken( parsedContent, 3, 1 );
		int level = intToken( parsedContent, 6 );
		String classname = parsedContent.nextToken().trim();

		// The next block of tokens contains the character's
		// hit point and mana point information (with possible
		// information about whether or not it's the base.

		skipTokens( parsedContent, 2 );
		int currentHP = intToken( parsedContent );
		skipTokens( parsedContent, 1 );
		int maximumHP = intToken( parsedContent );
		int baseHP = retrieveBase( parsedContent, maximumHP );

		int currentMP = intToken( parsedContent );
		skipTokens( parsedContent, 1 );
		int maximumMP = intToken( parsedContent );
		int baseMP = retrieveBase( parsedContent, maximumMP );

		// Now, we get the character's parsedContentats and calculate
		// the total of all subpoints they've gained so far,
		// based on the base values.

		int adjMus = intToken( parsedContent );
		int subMus = calculateSubpoints( retrieveBase( parsedContent, adjMus ), intToken( parsedContent ) );

		skipTokens( parsedContent, 4 );
		int adjMys = intToken( parsedContent );
		int subMys = calculateSubpoints( retrieveBase( parsedContent, adjMys ), intToken( parsedContent ) );

		skipTokens( parsedContent, 4 );
		int adjMox = intToken( parsedContent );
		int subMox = calculateSubpoints( retrieveBase( parsedContent, adjMox ), intToken( parsedContent ) );

		// Now, retrieve how drunk the character is and how
		// many adventures they have remaining, remembering
		// to discard any notes in parenthesis.  Meat and
		// turns played so far are also found.

		skipTokens( parsedContent, 4 );
		int drunk = intToken( parsedContent );

		if ( !parsedContent.nextToken().startsWith( "Adv" ) )
			skipTokens( parsedContent, 1 );

		int adv = intToken( parsedContent );
		skipTokens( parsedContent, 1 );
		int meat = intToken( parsedContent );
		skipTokens( parsedContent, 1 );
		int turns = intToken( parsedContent );

		// The remaining information is not necessarily easy
		// to parse (since it may or may not exist).  Therefore,
		// it is actually more useful to use different requests
		// to retrieve the desired data.
	}

	private int calculateSubpoints( int baseValue, int sinceLastBase )
	{	return baseValue * baseValue + sinceLastBase - 1;
	}

	private int retrieveBase( StringTokenizer st, int defaultBase )
	{
		String s = st.nextToken();
		if ( s.startsWith( " (base: " ) )
		{
			st.nextToken();
			return Integer.parseInt( s.substring( 8, s.length() - 2 ) );
		}
		else
			return defaultBase;
	}

	private void skipTokens( StringTokenizer st, int tokenCount )
	{
		for ( int i = 0; i < tokenCount; ++i )
			st.nextToken();
	}

	private int intToken( StringTokenizer st )
	{	return Integer.parseInt( st.nextToken() );
	}

	private int intToken( StringTokenizer st, int fromStart )
	{
		String s = st.nextToken();
		return Integer.parseInt( s.substring( fromStart ) );
	}

	private int intToken( StringTokenizer st, int fromStart, int fromEnd )
	{
		String s = st.nextToken();
		return Integer.parseInt( s.substring( fromStart, s.length() - 1 - fromEnd ) );
	}
}