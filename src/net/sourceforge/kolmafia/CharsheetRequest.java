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

/**
 * An extension of <code>KoLRequest</code> which retrieves the character's
 * information from the server.  Note that this request only retrieves the
 * character's statistics at the current time; skills and effects will be
 * retrieved at a later date.  Equipment retrieval takes place through a
 * different request.
 */

public class CharsheetRequest extends KoLRequest
{
	private KoLCharacter character;

	/**
	 * Constructs a new <code>CharsheetRequest</code>.  This also
	 * stores the reference to the character to be provided; the
	 * data found in that character will be overridden over the
	 * course of this request.
	 *
	 * @param	client	The client to be notified in case of errors
	 * @param	character	The character whose data will be overridden by the request
	 */

	public CharsheetRequest( KoLmafia client, KoLCharacter character )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charsheet.php" );
		this.character = character;
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

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		StringTokenizer parsedContent = new StringTokenizer(
			replyContent.replaceAll( "<[^>]*>", "\n" ), "\n" );

		logStream.println( "Parsing character data..." );

		try
		{
			// The first two tokens in the stream contains the
			// name, but the character's name was known at login.
			// Therefore, these tokens can be discarded.

			skipTokens( parsedContent, 2 );

			// The next three tokens contain the character's name and
			// other things which identify the character.

			character.setUserID( intToken( parsedContent, 3, 1 ) );
			character.setLevel( intToken( parsedContent, 6 ) );
			character.setClassName( parsedContent.nextToken().trim() );

			// The next block of tokens contains the character's
			// hit point and mana point information (with possible
			// information about whether or not it's the base.

			skipTokens( parsedContent, 2 );
			int currentHP = intToken( parsedContent );
			skipTokens( parsedContent, 1 );
			int maximumHP = intToken( parsedContent );
			int baseMaxHP = retrieveBase( parsedContent, maximumHP );

			character.setHP( currentHP, maximumHP, baseMaxHP );

			// Here it gets tricky - there's a chance that the user
			// has absolutely no MP, and KoL skips sending MP data
			// to save bandwidth.  So, to avoid being tricked, you
			// scan to see if the player had MP before arbitrarily
			// skipping tokens.

			int currentMP = 0;
			int maximumMP = 0;
			int baseMaxMP = 0;

			if ( replyContent.indexOf( "Current Mana Points:" ) != -1 )
			{
				currentMP = intToken( parsedContent );
				skipTokens( parsedContent, 1 );
				maximumMP = intToken( parsedContent );
				baseMaxMP = retrieveBase( parsedContent, maximumMP );
			}

			character.setMP( currentMP, maximumMP, baseMaxMP );

			// Now, we get the character's stats and calculate
			// the total of all subpoints they've gained so far,
			// based on the base values.

			int adjustedMuscle = intToken( parsedContent );
			int totalMuscle = calculateSubpoints( retrieveBase( parsedContent, adjustedMuscle ), intToken( parsedContent ) );

			skipTokens( parsedContent, 4 );
			int adjustedMysticality = intToken( parsedContent );
			int totalMysticality = calculateSubpoints( retrieveBase( parsedContent, adjustedMysticality ), intToken( parsedContent ) );

			skipTokens( parsedContent, 4 );
			int adjustedMoxie = intToken( parsedContent );
			int totalMoxie = calculateSubpoints( retrieveBase( parsedContent, adjustedMoxie ), intToken( parsedContent ) );

			character.setStats( adjustedMuscle, totalMuscle,
				adjustedMysticality, totalMysticality, adjustedMoxie, totalMoxie );

			// Now, retrieve how drunk the character is and how
			// many adventures they have remaining, remembering
			// to discard any notes in parenthesis.  Meat and
			// turns played so far are also found.

			character.setInebriety( 0 );
			skipTokens( parsedContent, 3 );
			if ( !parsedContent.nextToken().startsWith( "Adv" ) )
			{
				character.setInebriety( intToken( parsedContent ) );
				if ( !parsedContent.nextToken().startsWith( "Adv" ) )
					skipTokens( parsedContent, 1 );
			}

			character.setAdventuresLeft( intToken( parsedContent ) );
			skipTokens( parsedContent, 1 );
			character.setAvailableMeat( intToken( parsedContent ) );
			skipTokens( parsedContent, 1 );
			character.setTotalTurnsUsed( intToken( parsedContent ) );

			// The remaining information is not necessarily easy
			// to parse (since it may or may not exist).  Therefore,
			// it is actually more useful to use different requests
			// to retrieve the desired data.

			logStream.println( "Parsing complete." );
		}
		catch ( RuntimeException e )
		{
			logStream.println( e );
		}
	}

	/**
	 * Utility method for calculating how many subpoints have been accumulated
	 * thus far, given the current base point value of the statistic and how
	 * many have been accumulate since the last gain.
	 *
	 * @param	baseValue	The current base point value
	 * @param	sinceLastBase	Number of subpoints accumulate since the last base point gain
	 * @return	The total number of subpoints acquired since creation
	 */

	private int calculateSubpoints( int baseValue, int sinceLastBase )
	{	return baseValue * baseValue + sinceLastBase - 1;
	}

	/**
	 * Utility method for retrieving the base value for a statistic, given
	 * the tokenizer, and assuming that the base might be located in the
	 * next token.  If it isn't, the default value is returned instead.
	 * Note that this advances the <code>StringTokenizer</code> one token
	 * ahead of the base value for the statistic.
	 *
	 * @param	st	The <code>StringTokenizer</code> possibly containing the base value
	 * @param	defaultBase	The value to return, if no base value is found
	 * @return	The parsed base value, or the default value if no base value is found
	 */

	private int retrieveBase( StringTokenizer st, int defaultBase )
	{
		String s = st.nextToken();
		if ( s.startsWith( " (base: " ) )
		{
			st.nextToken();
			return Integer.parseInt( s.substring( 8, s.length() - 1 ) );
		}
		else
			return defaultBase;
	}
}