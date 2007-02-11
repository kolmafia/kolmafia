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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharsheetRequest extends KoLRequest
{
	private static final Pattern BASE_PATTERN = Pattern.compile( " \\(base: ([\\d,]+)\\)" );
	private static final Pattern AVATAR_PATTERN = Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^>\'\"\\s]+)" );

	/**
	 * Constructs a new <code>CharsheetRequest</code>.  The data
	 * in the KoLCharacter entity will be overridden over the
	 * course of this request.
	 */

	public CharsheetRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charsheet.php" );
	}

	/**
	 * Runs the request.  Note that only the KoLCharacter's statistics
	 * are retrieved via this retrieval.
	 */

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving character data..." );
		super.run();
	}

	public void processResults()
	{	parseStatus( responseText );
	}

	public static void parseStatus( String responseText )
	{
		// Set the character's avatar.
		Matcher avatarMatcher = AVATAR_PATTERN.matcher( responseText );

		if ( avatarMatcher.find() )
		{
			RequestEditorKit.downloadImage( avatarMatcher.group(1) );
			KoLCharacter.setAvatar( avatarMatcher.group(1) );
		}

		// Strip all of the HTML from the server reply
		// and then figure out what to do from there.

		String token = "";
		StringTokenizer cleanContent = new StringTokenizer( responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" ), "\n" );

		while ( !token.startsWith( " (" ) )
			token = cleanContent.nextToken();

		KoLCharacter.setUserId( StaticEntity.parseInt( token.substring( 3, token.length() - 1 ) ) );
		skipTokens( cleanContent, 1 );
		KoLCharacter.setClassName( cleanContent.nextToken().trim() );

		// Hit point parsing begins with the first index of
		// the words indicating that the upcoming token will
		// show the HP values (Current, Maximum).

		while ( !token.startsWith( "Current" ) )
			token = cleanContent.nextToken();

		int currentHP = intToken( cleanContent );
		while ( !token.startsWith( "Maximum" ) )
			token = cleanContent.nextToken();

		int maximumHP = intToken( cleanContent );
		token = cleanContent.nextToken();
		KoLCharacter.setHP( currentHP, maximumHP, retrieveBase( token, maximumHP ) );

		// Mana point parsing is exactly the same as hit point
		// parsing - so this is just a copy-paste of the code.

		while ( !token.startsWith( "Current" ) )
			token = cleanContent.nextToken();

		int currentMP = intToken( cleanContent );
		while ( !token.startsWith( "Maximum" ) )
			token = cleanContent.nextToken();

		int maximumMP = intToken( cleanContent );
		token = cleanContent.nextToken();
		KoLCharacter.setMP( currentMP, maximumMP, retrieveBase( token, maximumMP ) );

		// Next, you begin parsing the different stat points;
		// this involves hunting for the stat point's name,
		// skipping the appropriate number of tokens, and then
		// reading in the numbers.

		int [] mus = findStatPoints( cleanContent, token, "Mus" );
		int [] mys = findStatPoints( cleanContent, token, "Mys" );
		int [] mox = findStatPoints( cleanContent, token, "Mox" );

		KoLCharacter.setStatPoints( mus[0], mus[1], mys[0], mys[1], mox[0], mox[1] );

		// Drunkenness may or may not exist (in other words,
		// if the KoLCharacter is not drunk, nothing will show
		// up).  Therefore, parse it if it exists; otherwise,
		// parse until the "Adventures remaining:" token.


		while ( !token.startsWith( "Temul" ) && !token.startsWith( "Inebr" ) && !token.startsWith( "Tipsi" ) &&
			!token.startsWith( "Drunk" ) && !token.startsWith( "Adven" ) )
				token = cleanContent.nextToken();

		if ( !token.startsWith( "Adven" ) )
		{
			KoLCharacter.setInebriety( intToken( cleanContent ) );
			while ( !token.startsWith( "Adven" ) )
				token = cleanContent.nextToken();
		}
		else
			KoLCharacter.setInebriety( 0 );

		// Now parse the number of adventures remaining,
		// the monetary value in the KoLCharacter's pocket,
		// and the number of turns accumulated.

		int oldAdventures = KoLCharacter.getAdventuresLeft();
		int newAdventures = intToken( cleanContent );

		StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );

		while ( !token.startsWith( "Meat" ) )
			token = cleanContent.nextToken();
		KoLCharacter.setAvailableMeat( intToken( cleanContent ) );

		// Determine the player's ascension count, if any.
		// This is seen by whether or not the word "Ascensions"
		// appears in their player profile.

		if ( responseText.indexOf( "Ascensions:" ) != -1 )
		{
			while ( !token.startsWith( "Ascensions" ) )
				token = cleanContent.nextToken();
			KoLCharacter.setAscensions( intToken( cleanContent ) );
		}

		// There may also be a "turns this run" field which
		// allows you to have a Ronin countdown.

		if ( responseText.indexOf( "(this run)" ) != -1 )
		{
			while ( !token.startsWith( "Turns" ) || token.indexOf( "(this run)" ) == -1 )
				token = cleanContent.nextToken();

			KoLCharacter.setTotalTurnsUsed( intToken( cleanContent ) );
		}
		else
		{
			while ( !token.startsWith( "Turns" ) )
				token = cleanContent.nextToken();

			KoLCharacter.setTotalTurnsUsed( intToken( cleanContent ) );
		}

		// Determine the player's zodiac sign, if any.  We
		// could read the path in next, but it's easier to
		// read it from the full response text.

		if ( responseText.indexOf( "Sign:" ) != -1 )
		{
			while ( !cleanContent.nextToken().startsWith( "Sign:" ) );
			KoLCharacter.setSign( cleanContent.nextToken() );
		}

		KoLCharacter.setHardcore( responseText.indexOf( "You are in Hardcore mode" ) != -1 );

		// Determine the current consumption restrictions
		// the player possesses.

		KoLCharacter.setConsumptionRestriction(
			responseText.indexOf( "You may not eat or drink anything." ) != -1 ? AscensionSnapshotTable.OXYGENARIAN :
			responseText.indexOf( "You may not eat any food or drink any non-alcoholic beverages." ) != -1 ? AscensionSnapshotTable.BOOZETAFARIAN :
			responseText.indexOf( "You may not consume any alcohol." ) != -1 ? AscensionSnapshotTable.TEETOTALER : AscensionSnapshotTable.NOPATH );

		// See if the player has a store
		KoLCharacter.setStore( responseText.indexOf( "Mall of Loathing" ) != -1 );

		// See if the player has a display case
		KoLCharacter.setDisplayCase( responseText.indexOf( "Cannon Museum" ) != -1 );

		// Determine the player's current PvP rank

		if ( responseText.indexOf( "PvP:" ) != -1 )
		{
			while ( !cleanContent.nextToken().startsWith( "Ranking" ) );
			KoLCharacter.setPvpRank( intToken( cleanContent ) );
		}

		while ( !token.startsWith( "Skill" ) )
			token = cleanContent.nextToken();

		// The first token says "(click the skill name for more information)"
		// which is not really a skill.

		skipTokens( cleanContent, 1 );
		token = cleanContent.nextToken();

		List newSkillSet = new ArrayList();

		// Loop until we get to Current Familiar, since everything before that
		// contains the player's skills.

		while ( !token.startsWith( "Current" ) )
		{
			if ( token.startsWith( "(" ) || token.startsWith( " (" ) )
			{
				if ( token.length() <= 2 )
					skipTokens( cleanContent, 2 );
			}
			else if ( ClassSkillsDatabase.contains( token ) )
				newSkillSet.add( UseSkillRequest.getInstance( token ) );

			// No more tokens if no familiar equipped
			if ( !cleanContent.hasMoreTokens() )
				break;

			token = cleanContent.nextToken();
		}

		KoLCharacter.setAvailableSkills( newSkillSet );
		KoLCharacter.recalculateAdjustments();
	}

	/**
	 * Helper method used to find the statistic points.  This method was
	 * created because statistic-point finding is exactly the same for
	 * every statistic point.
	 *
	 * @param	tokenizer	The <code>StringTokenizer</code> containing the tokens to be parsed
	 * @param	searchString	The search string indicating the beginning of the statistic
	 * @return	The 2-element array containing the parsed statistics
	 */

	private static int [] findStatPoints( StringTokenizer tokenizer, String token, String searchString )
	{
		int [] stats = new int[2];

		while ( !token.startsWith( searchString ) )
			token = tokenizer.nextToken();

		stats[0] = intToken( tokenizer );
		token = tokenizer.nextToken();
		int base = retrieveBase( token, stats[0] );

		while ( !token.startsWith( "(" ) )
			token = tokenizer.nextToken();

		stats[1] = KoLCharacter.calculateSubpoints( base, intToken( tokenizer ) );
		return stats;
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

	private static int retrieveBase( String token, int defaultBase )
	{
		Matcher baseMatcher = BASE_PATTERN.matcher( token );
		return baseMatcher.find() ? StaticEntity.parseInt( baseMatcher.group(1) ) : defaultBase;
	}
}
