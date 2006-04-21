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
 * An extension of <code>KoLRequest</code> which retrieves the KoLCharacter's
 * information from the server.  Note that this request only retrieves the
 * KoLCharacter's statistics at the current time; skills and effects will be
 * retrieved at a later date.  Equipment retrieval takes place through a
 * different request.
 */

public class CharsheetRequest extends KoLRequest
{
	private static final Pattern BASE_PATTERN = Pattern.compile( " \\(base: ([\\d,]+)\\)" );

	/**
	 * Constructs a new <code>CharsheetRequest</code>.  The data
	 * in the KoLCharacter entity will be overridden over the
	 * course of this request.
	 *
	 * @param	client	The client to be notified in case of errors
	 */

	public CharsheetRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charsheet.php" );
	}

	/**
	 * Runs the request.  Note that only the KoLCharacter's statistics
	 * are retrieved via this retrieval.
	 */

	public void run()
	{
		DEFAULT_SHELL.updateDisplay( "Retrieving character data..." );
		super.run();
	}

	protected void processResults()
	{	parseStatus( responseText );
	}

	public static void parseStatus( String responseText )
	{
		// Set the character's avatar.
		Matcher avatarMatcher = Pattern.compile( "http://images.kingdomofloathing.com/([^>]*?)\\.gif" ).matcher( responseText );
		avatarMatcher.find();

		RequestEditorKit.downloadImage( avatarMatcher.group() );
		KoLCharacter.setAvatar( avatarMatcher.group(1) + ".gif" );

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		StringTokenizer parsedContent = new StringTokenizer( responseText, "<>" );

		try
		{
			// The first two tokens in the stream contains the
			// name, but the character's name was known at login.
			// Therefore, these tokens can be discarded.

			String token = parsedContent.nextToken();
			while ( !token.startsWith( " (" ) )
				token = parsedContent.nextToken();

			KoLCharacter.setUserID( Integer.parseInt( token.substring( 3, token.length() - 1 ) ) );
			skipTokens( parsedContent, 3 );
			KoLCharacter.setClassName( parsedContent.nextToken().trim() );

			// Hit point parsing begins with the first index of
			// the words indicating that the upcoming token will
			// show the HP values (Current, Maximum).

			while ( !token.startsWith( "Current" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int currentHP = intToken( parsedContent );

			while ( !token.startsWith( "Maximum" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int maximumHP = intToken( parsedContent );
			KoLCharacter.setHP( currentHP, maximumHP, retrieveBase( parsedContent.nextToken(), maximumHP ) );

			// Mana point parsing is exactly the same as hit point
			// parsing - so this is just a copy-paste of the code.

			while ( !token.startsWith( "Current" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int currentMP = intToken( parsedContent );

			while ( !token.startsWith( "Maximum" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int maximumMP = intToken( parsedContent );
			KoLCharacter.setMP( currentMP, maximumMP, retrieveBase( parsedContent.nextToken(), maximumMP ) );

			// Next, you begin parsing the different stat points;
			// this involves hunting for the stat point's name,
			// skipping the appropriate number of tokens, and then
			// reading in the numbers.

			int [] mus = findStatPoints( parsedContent, "Mus" );
			int [] mys = findStatPoints( parsedContent, "Mys" );
			int [] mox = findStatPoints( parsedContent, "Mox" );

			KoLCharacter.setStatPoints( mus[0], mus[1], mys[0], mys[1], mox[0], mox[1] );

			// Drunkenness may or may not exist (in other words,
			// if the KoLCharacter is not drunk, nothing will show
			// up).  Therefore, parse it if it exists; otherwise,
			// parse until the "Adventures remaining:" token.

			while ( !token.startsWith("Temul") && !token.startsWith("Inebr") && !token.startsWith("Tipsi") &&
				!token.startsWith("Drunk") && !token.startsWith("Adven") )
					token = parsedContent.nextToken();

			if ( !token.startsWith( "Adven" ) )
			{
				skipTokens( parsedContent, 3 );
				KoLCharacter.setInebriety( intToken( parsedContent ) );

				while ( !token.startsWith( "Adven" ) )
					token = parsedContent.nextToken();
			}
			else
				KoLCharacter.setInebriety( 0 );

			// Now parse the number of adventures remaining,
			// the monetary value in the KoLCharacter's pocket,
			// and the number of turns accumulated.

			skipTokens( parsedContent, 3 );

			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = intToken( parsedContent );

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );

			while ( !token.startsWith( "Meat" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			KoLCharacter.setAvailableMeat( intToken( parsedContent ) );

			// Determine the player's ascension count, if any.
			if ( responseText.indexOf( "Ascensions:" ) != -1 )
			{
				while ( !token.startsWith( "Ascensions" ) )
					token = parsedContent.nextToken();
				skipTokens( parsedContent, 4 );
				KoLCharacter.setAscensions( intToken( parsedContent ) );
			}

			if ( responseText.indexOf( "(this run)" ) != -1 )
			{
				while ( !token.startsWith( "Turns" ) || token.indexOf( "(this run)" ) == -1 )
					token = parsedContent.nextToken();

				skipTokens( parsedContent, 3 );
				KoLCharacter.setTotalTurnsUsed( intToken( parsedContent ) );
			}

			// Determine the player's zodiac sign, if any.

			if ( responseText.indexOf( "Sign:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Sign:" ) );
				skipTokens( parsedContent, 3 );
				KoLCharacter.setSign( parsedContent.nextToken() );
			}

			KoLCharacter.setHardcore( responseText.indexOf( "You are in Hardcore mode" ) != -1 );
			KoLCharacter.setInteraction( responseText.indexOf( "You may not receive items from other players" ) == -1 &&
				responseText.indexOf( "You are in Hardcore mode" ) == -1 );

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
				while ( !parsedContent.nextToken().startsWith( "Ranking" ) );
				skipTokens( parsedContent, 3 );
				KoLCharacter.setPvpRank( Integer.parseInt( parsedContent.nextToken() ) );
			}

			// Determine whether or not the player has any
			// active effects - if so, retrieve them.

			KoLCharacter.getEffects().clear();
			if ( responseText.indexOf( "Effects:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Eff" ) );
				skipTokens( parsedContent, 13 );
				token = parsedContent.nextToken();

				while ( !token.equals( "/table" ) )
				{
					StaticEntity.getClient().parseEffect( parsedContent.nextToken() );
					if ( parsedContent.nextToken().startsWith( "font" ) )

						// "shrug off" link
						skipTokens( parsedContent, 13 );
					else
						// no such link
						skipTokens( parsedContent, 6 );
					token = parsedContent.nextToken();
				}

				// Ensure that the effects are refreshed
				// against the current list.

				StaticEntity.getClient().applyRecentEffects();
			}

			if ( responseText.indexOf( "Skills:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Ski" ) );

				token = parsedContent.nextToken();
				List availableSkills = new ArrayList();
				while ( !token.equals( "/table" ) )
				{
					if ( token.startsWith( "a" ) )
					{
						String skillName = parsedContent.nextToken().trim();
						if ( ClassSkillsDatabase.contains( skillName ) )
							availableSkills.add( new UseSkillRequest( StaticEntity.getClient(), skillName, "", 1 ) );
					}
					token = parsedContent.nextToken();
				}

				KoLCharacter.setAvailableSkills( availableSkills );
			}

			// Current equipment is also listed on the KoLCharacter
			// sheet -- because we now have consumption types,
			// it is now possible to retrieve all the equipment.

			// We can't get familiar equipment from this page,
			// so don't reset it.

			String [] equipment = new String[8];
			for ( int i = 0; i < 8; ++i )
				equipment[i] = EquipmentRequest.UNEQUIP;
			int fakeHands = 0;

			Matcher equipmentMatcher = Pattern.compile( "<b>Equipment.*?<table>(.*?)</table>" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				String currentItem;
				Matcher itemMatcher = Pattern.compile( "<b>(.*?)</b>" ).matcher( equipmentMatcher.group(1) );
				boolean seenWeapon = false;

				while ( itemMatcher.find() )
				{
					currentItem = itemMatcher.group(1);

					switch ( TradeableItemDatabase.getConsumptionType( currentItem ) )
					{
						case ConsumeItemRequest.EQUIP_HAT:
							equipment[ KoLCharacter.HAT ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_WEAPON:
							equipment[ seenWeapon ? KoLCharacter.OFFHAND : KoLCharacter.WEAPON ] = currentItem;
							seenWeapon = true;
							break;

						case ConsumeItemRequest.EQUIP_OFFHAND:
							if ( currentItem.equals( "fake hand" ) )
								fakeHands++;
							else
								equipment[ KoLCharacter.OFFHAND ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_SHIRT:
							equipment[ KoLCharacter.SHIRT ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_PANTS:
							equipment[ KoLCharacter.PANTS ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_ACCESSORY:

							if ( equipment[ KoLCharacter.ACCESSORY1 ].equals( EquipmentRequest.UNEQUIP ) )
								equipment[ KoLCharacter.ACCESSORY1 ] = currentItem;
							else if ( equipment[ KoLCharacter.ACCESSORY2 ].equals( EquipmentRequest.UNEQUIP ) )
								equipment[ KoLCharacter.ACCESSORY2 ] = currentItem;
							else
								equipment[ KoLCharacter.ACCESSORY3 ] = currentItem;
					}
				}
			}

			KoLCharacter.setEquipment( equipment, null );
			KoLCharacter.setFakeHands( fakeHands );
		}
		catch ( RuntimeException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
		}

		KoLCharacter.updateStatus();
	}

	/**
	 * Helper method used to find the statistic points.  This method was
	 * created because statistic-point finding is exactly the same for
	 * every statistic point.
	 *
	 * @param	st	The <code>StringTokenizer</code> containing the tokens to be parsed
	 * @param	searchString	The search string indicating the beginning of the statistic
	 * @return	The 2-element array containing the parsed statistics
	 */

	private static int [] findStatPoints( StringTokenizer st, String searchString )
	{
		int [] stats = new int[2];
		String token = st.nextToken();

		while ( !token.startsWith( searchString ) )
			token = st.nextToken();
		skipTokens( st, 6 );
		stats[0] = intToken( st );
		int base = retrieveBase( st.nextToken(), stats[0] );
		while ( !token.equals( "b" ) )
			token = st.nextToken();
		stats[1] = KoLCharacter.calculateSubpoints( base, intToken( st ) );

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
		try
		{
			Matcher baseMatcher = BASE_PATTERN.matcher( token );
			return baseMatcher.find() ? df.parse( baseMatcher.group(1) ).intValue() : defaultBase;
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return defaultBase;
		}
	}
}