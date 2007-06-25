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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharpaneRequest extends KoLRequest
{
	private static boolean isRunning = false;
	private static boolean isProcessing = false;
	private static final CharpaneRequest instance = new CharpaneRequest();

	private CharpaneRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charpane.php" );
	}

	public static CharpaneRequest getInstance()
	{	return instance;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void run()
	{
		if ( isRunning )
			return;

		isRunning = true;
		super.run();
		isRunning = false;
	}

	public void processResults()
	{	processCharacterPane( this.responseText );
	}

	public static void processCharacterPane( String responseText )
	{
		if ( isProcessing )
			return;

		isProcessing = true;

		// By refreshing the KoLCharacter pane, you can
		// determine whether or not you are in compact
		// mode - be sure to refresh this value.

		KoLRequest.isCompactMode = responseText.indexOf( "<br>Lvl. " ) != -1;

		// The easiest way to retrieve the KoLCharacter pane
		// data is to use regular expressions.  But, the
		// only data that requires synchronization is the
		// modified stat values, health and mana.

		if ( responseText.indexOf( "<img src=\"http://images.kingdomofloathing.com/otherimages/inf_small.gif\">" ) == -1 )
		{
			if ( isCompactMode )
				handleCompactMode( responseText );
			else
				handleExpandedMode( responseText );
		}
		else
		{
			KoLCharacter.setStatPoints( 1, 0, 1, 0, 1, 0 );
			KoLCharacter.setHP( 1, 1, 1 );
			KoLCharacter.setMP( 1, 1, 1 );
			KoLCharacter.setAvailableMeat( 0 );
			KoLCharacter.setAdventuresLeft( 0 );
			KoLCharacter.setMindControlLevel( 0 );
		}

		refreshEffects( responseText );
		KoLCharacter.updateStatus();
		isProcessing = false;
	}

	private static void handleCompactMode( String responseText )
	{
		try
		{
			handleStatPoints( responseText, "Mus", "Mys", "Mox" );
			handleMiscPoints( responseText, "HP", "MP", "Meat", "Adv", "", "<b>", "</b>" );
			handleMindControl( responseText, "MC" );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Character pane error", new String [] {
				responseText } );
		}
	}

	private static void handleExpandedMode( String responseText )
	{
		try
		{
			handleStatPoints( responseText, "Muscle", "Mysticality", "Moxie" );
			handleMiscPoints( responseText, "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );
			handleMindControl( responseText, "Mind Control" );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Character pane error", new String [] {
				responseText } );
		}
	}

	private static void handleStatPoints( String responseText, String musString, String mysString, String moxString ) throws Exception
	{
		int [] modified = new int[3];

		Matcher statMatcher = Pattern.compile( musString + ".*?<b>(.*?)</b>.*?" + mysString + ".*?<b>(.*?)</b>.*?" + moxString + ".*?<b>(.*?)</b>" ).matcher( responseText );

		if ( statMatcher.find() )
		{
			for ( int i = 0; i < 3; ++i )
			{
				Matcher modifiedMatcher = Pattern.compile( "<font color=blue>(\\d+)</font>&nbsp;\\((\\d+)\\)" ).matcher(
					statMatcher.group( i + 1 ) );

				if ( modifiedMatcher.find() )
					modified[i] = StaticEntity.parseInt( modifiedMatcher.group(1) );
				else
					modified[i] = StaticEntity.parseInt( statMatcher.group( i + 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			}

			KoLCharacter.setStatPoints( modified[0], KoLCharacter.getTotalMuscle(), modified[1],
				KoLCharacter.getTotalMysticality(), modified[2], KoLCharacter.getTotalMoxie() );
		}
	}

	private static void handleMiscPoints( String responseText, String hpString, String mpString, String meatString, String advString, String spacer, String openTag, String closeTag ) throws Exception
	{
		// On the other hand, health and all that good stuff
		// is complicated, has nested images, and lots of other
		// weird stuff.  Handle it in a non-modular fashion.

		Matcher miscMatcher = Pattern.compile(
			hpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" +
			mpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" +
			meatString + ".*?" + openTag + "(.*?)" + closeTag + ".*?" + advString + ".*?" + openTag + "(.*?)" + closeTag ).matcher( responseText );

		if ( miscMatcher.find() )
		{
			String currentHP = miscMatcher.group(1).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			String maximumHP = miscMatcher.group(2).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

			String currentMP = miscMatcher.group(3).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			String maximumMP = miscMatcher.group(4).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

			KoLCharacter.setHP( StaticEntity.parseInt( currentHP ), StaticEntity.parseInt( maximumHP ), StaticEntity.parseInt( maximumHP ) );
			KoLCharacter.setMP( StaticEntity.parseInt( currentMP ), StaticEntity.parseInt( maximumMP ), StaticEntity.parseInt( maximumMP ) );

			String availableMeat = miscMatcher.group(5).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			KoLCharacter.setAvailableMeat( StaticEntity.parseInt( availableMeat ) );

			String adventuresLeft = miscMatcher.group(6).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = StaticEntity.parseInt( adventuresLeft );
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );
		}
	}

	private static void handleMindControl( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setMindControlLevel( StaticEntity.parseInt( matcher.group(2) ) );
		else
			KoLCharacter.setMindControlLevel( 0 );
	}

	private static void refreshEffects( String responseText )
	{
		int searchIndex = 0;
		int lastSearchIndex = 0;

		recentEffects.clear();
		ArrayList visibleEffects = new ArrayList();

		while ( searchIndex != -1 )
		{
			searchIndex = responseText.indexOf( "onClick='eff", lastSearchIndex + 1 );

			if ( searchIndex == -1 )
				continue;

			int nextSearchIndex = responseText.indexOf( "(", searchIndex ) + 1;
			lastSearchIndex = nextSearchIndex;

			String descriptionId = responseText.substring( nextSearchIndex,
				responseText.indexOf( ")", nextSearchIndex ) );

			int effectId = StatusEffectDatabase.getEffect( descriptionId );
			if ( effectId == -1 )
				continue;

			String effectName = StatusEffectDatabase.getEffectName( effectId );

			if ( KoLRequest.isCompactMode )
				lastSearchIndex = responseText.indexOf( "<td>(", lastSearchIndex ) + 5;
			else
				lastSearchIndex = responseText.indexOf( "(", responseText.indexOf( "<font size=2>", lastSearchIndex ) ) + 1;

			nextSearchIndex = responseText.indexOf( ")", lastSearchIndex );
			String duration = responseText.substring( lastSearchIndex, nextSearchIndex );

			if ( duration.indexOf( "&" ) == -1 && duration.indexOf( "<" ) == -1 )
			{
				int durationValue = StaticEntity.parseInt( duration );
				AdventureResult effect = new AdventureResult( effectName, durationValue, true );
				if ( effect.getCount( activeEffects ) != durationValue )
				{
					activeEffects.remove( effect );
					StaticEntity.getClient().processResult( effect );
				}

				visibleEffects.add( effect );
			}

			lastSearchIndex = nextSearchIndex;
		}

		KoLmafia.applyEffects();
		activeEffects.retainAll( visibleEffects );
	}
}
