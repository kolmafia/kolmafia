/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONException;
import org.json.JSONObject;

public class CharSheetRequest
	extends GenericRequest
{
	private static final Pattern BASE_PATTERN = Pattern.compile( " \\(base: ([\\d,]+)\\)" );
	private static final Pattern AVATAR_PATTERN =
		Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^>\'\"\\s]+)" );
	private static final Pattern EFFECT_TABLE_PATTERN =	
		Pattern.compile( "Effects:.*?<table>(.*?)</table>" );
	private static final Pattern EFFECT_PATTERN =	
		Pattern.compile( "<td>.*?onClick='eff.\"([^\"]*).*?</td><td.*?>(.*?) \\((.*?)\\) *<" );

	/**
	 * Constructs a new <code>CharSheetRequest</code>. The data in the KoLCharacter entity will be overridden over
	 * the course of this request.
	 */

	public CharSheetRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charsheet.php" );
	}

	/**
	 * Runs the request. Note that only the character's statistics are retrieved via this retrieval.
	 */

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public String getHashField()
	{
		return null;
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving character data..." );
		super.run();
	}

	public void processResults()
	{
		CharSheetRequest.parseStatus( this.responseText );
	}

	public static final void parseStatus( final String responseText )
	{
		// Set the character's avatar.
		Matcher avatarMatcher = CharSheetRequest.AVATAR_PATTERN.matcher( responseText );

		if ( avatarMatcher.find() )
		{
			FileUtilities.downloadImage( avatarMatcher.group( 1 ) );
			KoLCharacter.setAvatar( avatarMatcher.group( 1 ) );
		}

		// Strip all of the HTML from the server reply
		// and then figure out what to do from there.

		String token = "";
		StringTokenizer cleanContent =
			new StringTokenizer( responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" ), "\n" );

		while ( !token.startsWith( " (#" ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setUserId( StringUtilities.parseInt( token.substring( 3, token.length() - 1 ) ) );
		GenericRequest.skipTokens( cleanContent, 1 );

		String className = cleanContent.nextToken().trim();

		// Hit point parsing begins with the first index of
		// the words indicating that the upcoming token will
		// show the HP values (Current, Maximum).

		while ( !token.startsWith( "Current" ) )
		{
			token = cleanContent.nextToken();
		}

		int currentHP = GenericRequest.intToken( cleanContent );
		while ( !token.startsWith( "Maximum" ) )
		{
			token = cleanContent.nextToken();
		}

		int maximumHP = GenericRequest.intToken( cleanContent );
		token = cleanContent.nextToken();
		KoLCharacter.setHP( currentHP, maximumHP, CharSheetRequest.retrieveBase( token, maximumHP ) );

		// Mana point parsing is exactly the same as hit point
		// parsing - so this is just a copy-paste of the code.

		while ( !token.startsWith( "Current" ) )
		{
			token = cleanContent.nextToken();
		}

		int currentMP = GenericRequest.intToken( cleanContent );
		while ( !token.startsWith( "Maximum" ) )
		{
			token = cleanContent.nextToken();
		}

		int maximumMP = GenericRequest.intToken( cleanContent );
		token = cleanContent.nextToken();
		KoLCharacter.setMP( currentMP, maximumMP, CharSheetRequest.retrieveBase( token, maximumMP ) );

		// Players with a custom title will have their actual class shown in this area.

		while ( !token.startsWith( "Mus" ) )
		{
			if ( token.equals( "Class:" ) )
			{
				className = cleanContent.nextToken().trim();
				break;
			}
			token = cleanContent.nextToken();
		}

		// Next, you begin parsing the different stat points;
		// this involves hunting for the stat point's name,
		// skipping the appropriate number of tokens, and then
		// reading in the numbers.

		long[] mus = CharSheetRequest.findStatPoints( cleanContent, token, "Mus" );
		long[] mys = CharSheetRequest.findStatPoints( cleanContent, token, "Mys" );
		long[] mox = CharSheetRequest.findStatPoints( cleanContent, token, "Mox" );

		KoLCharacter.setStatPoints( (int)mus[ 0 ], mus[ 1 ], (int)mys[ 0 ], mys[ 1 ], (int)mox[ 0 ], mox[ 1 ] );

		// Drunkenness may or may not exist (in other words,
		// if the character is not drunk, nothing will show
		// up). Therefore, parse it if it exists; otherwise,
		// parse until the "Adventures remaining:" token.

		while ( !token.startsWith( "Temul" ) && !token.startsWith( "Inebr" ) && !token.startsWith( "Tipsi" ) && !token.startsWith( "Drunk" ) && !token.startsWith( "Adven" ) )
		{
			token = cleanContent.nextToken();
		}

		if ( !token.startsWith( "Adven" ) )
		{
			KoLCharacter.setInebriety( GenericRequest.intToken( cleanContent ) );
			while ( !token.startsWith( "Adven" ) )
			{
				token = cleanContent.nextToken();
			}
		}
		else
		{
			KoLCharacter.setInebriety( 0 );
		}

		// Now parse the number of adventures remaining,
		// the monetary value in the character's pocket,
		// and the number of turns accumulated.

		int oldAdventures = KoLCharacter.getAdventuresLeft();
		int newAdventures = GenericRequest.intToken( cleanContent );
		ResultProcessor.processAdventuresLeft( newAdventures - oldAdventures );

		while ( !token.startsWith( "Meat" ) )
		{
			token = cleanContent.nextToken();
		}
		KoLCharacter.setAvailableMeat( GenericRequest.intToken( cleanContent ) );

		// Determine the player's ascension count, if any.
		// This is seen by whether or not the word "Ascensions"
		// appears in their player profile.

		if ( responseText.indexOf( "Ascensions:" ) != -1 )
		{
			while ( !token.startsWith( "Ascensions" ) )
			{
				token = cleanContent.nextToken();
			}
			KoLCharacter.setAscensions( GenericRequest.intToken( cleanContent ) );
		}

		// There may also be a "turns this run" field which
		// allows you to have a Ronin countdown.
		boolean runStats = responseText.indexOf( "(this run)" ) != -1;

		while ( !token.startsWith( "Turns" ) ||
			( runStats && token.indexOf( "(this run)" ) == -1 ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setCurrentRun( GenericRequest.intToken( cleanContent ) );
		while ( !token.startsWith( "Days" ) ||
			( runStats && token.indexOf( "(this run)" ) == -1 ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setCurrentDays( GenericRequest.intToken( cleanContent ) );

		// Determine the player's zodiac sign, if any. We
		// could read the path in next, but it's easier to
		// read it from the full response text.

		if ( responseText.indexOf( "Sign:" ) != -1 )
		{
			while ( !cleanContent.nextToken().startsWith( "Sign:" ) )
			{
				;
			}
			KoLCharacter.setSign( cleanContent.nextToken() );
		}

		// Is this where Path: optionally appears for Bees Hate You?

		// Consumption restrictions have special messages.
		//
		// "You may not eat or drink anything."
		// "You may not eat any food or drink any non-alcoholic beverages."
		// "You may not consume any alcohol."

		KoLCharacter.setConsumptionRestriction(
			responseText.indexOf( "You may not eat or drink anything." ) != -1 ?
			AscensionSnapshot.OXYGENARIAN :
			responseText.indexOf( "You may not eat any food or drink any non-alcoholic beverages." ) != -1 ?
			AscensionSnapshot.BOOZETAFARIAN :
			responseText.indexOf( "You may not consume any alcohol." ) != -1 ?
			AscensionSnapshot.TEETOTALER :
			AscensionSnapshot.NOPATH );

		// You are in Hardcore mode, and may not receive items or buffs
		// from other players.

		boolean hardcore = responseText.indexOf( "You are in Hardcore mode" ) != -1;
		KoLCharacter.setHardcore( hardcore );

		// You may not receive items from other players until you have
		// played # more Adventures.

		KoLCharacter.setRonin( hardcore || responseText.indexOf( "You may not receive items from other players" ) != -1 );

		// Deduce interaction from above settings

		CharPaneRequest.setInteraction();

		// See if the player has a store
		KoLCharacter.setStore( responseText.indexOf( "Mall of Loathing" ) != -1 );

		// See if the player has a display case
		KoLCharacter.setDisplayCase( responseText.indexOf( "in the Museum" ) != -1 );

		// Determine the player's current PvP rank

		if ( responseText.indexOf( "PvP:" ) != -1 )
		{
			while ( !cleanContent.nextToken().startsWith( "Ranking" ) )
			{
				;
			}
			KoLCharacter.setPvpRank( GenericRequest.intToken( cleanContent ) );
		}

		// The Effects list appears after the store and display case
		// and before the Skills list. I have no idea where it appears
		// relative to PVP Ranking. Therefore, just pick up and parse
		// the table directly.

		Matcher effectTableMatcher = CharSheetRequest.EFFECT_TABLE_PATTERN.matcher( responseText );
		if ( effectTableMatcher.find() )
		{
			KoLConstants.recentEffects.clear();
			ArrayList visibleEffects = new ArrayList();

			Matcher effectMatcher = CharSheetRequest.EFFECT_PATTERN.matcher( effectTableMatcher.group( 1 ) );
			while ( effectMatcher.find() )
			{
				String descId = effectMatcher.group( 1 );
				String effectName = effectMatcher.group( 2 );
				String durationString = effectMatcher.group( 3 );
				int duration =
					durationString.equals( "&infin;" ) ?
					Integer.MAX_VALUE :
					StringUtilities.isNumeric( durationString ) ?
					StringUtilities.parseInt( durationString ) :
					0;
				AdventureResult effect = CharPaneRequest.extractEffect( descId, effectName, duration );
				if ( effect == null )
				{
					continue;
				}

				int activeCount = effect.getCount( KoLConstants.activeEffects );

				if ( effect.getCount() != activeCount )
				{
					ResultProcessor.processResult( effect.getInstance( effect.getCount() - activeCount ) );
				}

				visibleEffects.add( effect );
			}

			KoLmafia.applyEffects();
			KoLConstants.activeEffects.retainAll( visibleEffects );
			KoLCharacter.recalculateAdjustments();
		}

		while ( !token.startsWith( "Skill" ) )
		{
			token = cleanContent.nextToken();
		}

		// The first token says "(click the skill name for more
		// information)" which is not really a skill.

		GenericRequest.skipTokens( cleanContent, 1 );
		token = cleanContent.nextToken();

		List newSkillSet = new ArrayList();
		List permedSkillSet = new ArrayList();

		// Loop until we get to Current Familiar, since everything
		// before that contains the player's skills.

		while ( !token.startsWith( "Current" ) && !token.startsWith( "[" ) )
		{
			if ( SkillDatabase.contains( token ) )
			{
				String skillName = token;
				UseSkillRequest skill = UseSkillRequest.getInstance( skillName );
				int skillId = SkillDatabase.getSkillId( skillName );
				boolean shouldAddSkill = true;

				switch ( skillId )
				{
				case SkillDatabase.SNOWCONE:
				case SkillDatabase.STICKER:
				case SkillDatabase.SUGAR:
				case SkillDatabase.CLIP_ART:
				case SkillDatabase.HILARIOUS:
				case SkillDatabase.TASTEFUL:
				case SkillDatabase.CARDS:
				case SkillDatabase.CANDY_HEART:
				case SkillDatabase.PARTY_FAVOR:
				case SkillDatabase.LOVE_SONG:
				case SkillDatabase.BRICKOS:
				case SkillDatabase.DICE:
					shouldAddSkill = !KoLCharacter.inBadMoon() || KoLCharacter.kingLiberated();
					break;
				case SkillDatabase.OLFACTION:
					shouldAddSkill = !KoLCharacter.inBadMoon() || KoLCharacter.skillsRecalled();
					break;
				}

				if ( shouldAddSkill )
				{
					newSkillSet.add( skill );
				}

				if ( !cleanContent.hasMoreTokens() )
				{
					break;
				}

				token = cleanContent.nextToken();

				// (<b>HP</b>)
				if ( token.equals( "(" ) || token.equals( " (" ) )
				{
					GenericRequest.skipTokens( cleanContent, 2 );
					permedSkillSet.add( skill );
				}
				// (P)
				else if ( token.equals( "(P)" ) || token.equals( " (P)" ) )
				{
					permedSkillSet.add( skill );
				}
				else
				{
					continue;
				}
			}

			// No more tokens if no familiar equipped
			if ( !cleanContent.hasMoreTokens() )
			{
				break;
			}

			token = cleanContent.nextToken();
		}

		// Finally, set the class name that we figured out.
		// This must be done before setAvailableSkills, since
		// moxious maneuver is conditionally added based on your class.
		KoLCharacter.setClassName( className );

		KoLCharacter.setAvailableSkills( newSkillSet );
		KoLCharacter.setPermedSkills( permedSkillSet );
	}

	/**
	 * Helper method used to find the statistic points. This method was created because statistic-point finding is
	 * exactly the same for every statistic point.
	 *
	 * @param tokenizer The <code>StringTokenizer</code> containing the tokens to be parsed
	 * @param searchString The search string indicating the beginning of the statistic
	 * @return The 2-element array containing the parsed statistics
	 */

	private static final long[] findStatPoints( final StringTokenizer tokenizer, String token, final String searchString )
	{
		long[] stats = new long[ 2 ];

		while ( !token.startsWith( searchString ) )
		{
			token = tokenizer.nextToken();
		}

		stats[ 0 ] = GenericRequest.intToken( tokenizer );
		token = tokenizer.nextToken();
		int base = CharSheetRequest.retrieveBase( token, (int) stats[ 0 ] );

		while ( !token.startsWith( "(" ) )
		{
			token = tokenizer.nextToken();
		}

		stats[ 1 ] = KoLCharacter.calculateSubpoints( base, GenericRequest.intToken( tokenizer ) );
		return stats;
	}

	/**
	 * Utility method for retrieving the base value for a statistic, given the tokenizer, and assuming that the base
	 * might be located in the next token. If it isn't, the default value is returned instead. Note that this advances
	 * the <code>StringTokenizer</code> one token ahead of the base value for the statistic.
	 *
	 * @param st The <code>StringTokenizer</code> possibly containing the base value
	 * @param defaultBase The value to return, if no base value is found
	 * @return The parsed base value, or the default value if no base value is found
	 */

	private static final int retrieveBase( final String token, final int defaultBase )
	{
		Matcher baseMatcher = CharSheetRequest.BASE_PATTERN.matcher( token );
		return baseMatcher.find() ? StringUtilities.parseInt( baseMatcher.group( 1 ) ) : defaultBase;
	}

	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		int muscle = JSON.getInt( "muscle" );
		int rawmuscle = JSON.getInt( "rawmuscle" );

		int mysticality = JSON.getInt( "mysticality" );
		int rawmysticality = JSON.getInt( "rawmysticality" );

		int moxie = JSON.getInt( "moxie" );
		int rawmoxie = JSON.getInt( "rawmoxie" );

		KoLCharacter.setStatPoints( muscle, rawmuscle,
					    mysticality, rawmysticality,
					    moxie, rawmoxie );
	}
}
