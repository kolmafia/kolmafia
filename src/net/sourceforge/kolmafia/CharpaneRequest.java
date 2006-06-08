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
	private boolean isRunning = false;
	private static CharpaneRequest instance = null;

	private CharpaneRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charpane.php" );
	}

	public static CharpaneRequest getInstance()
	{
		if ( instance == null || instance.client != StaticEntity.getClient() )
			instance = new CharpaneRequest( StaticEntity.getClient() );

		return instance;
	}

	public void run()
	{
		if ( isRunning )
			return;

		if ( !(Thread.currentThread() instanceof CharpaneThread) )
		{
			(new CharpaneThread()).start();
			return;
		}

		isRunning = true;
		super.run();
		isRunning = false;
	}

	protected void processResults()
	{	processCharacterPane( this.responseText );
	}

	public static synchronized void processCharacterPane( String responseText )
	{
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

		KoLCharacter.updateStatus();
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
					modified[i] = Integer.parseInt( modifiedMatcher.group(1) );
				else
					modified[i] = Integer.parseInt( statMatcher.group( i + 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
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

			KoLCharacter.setHP( Integer.parseInt( currentHP ), Integer.parseInt( maximumHP ), Integer.parseInt( maximumHP ) );
			KoLCharacter.setMP( Integer.parseInt( currentMP ), Integer.parseInt( maximumMP ), Integer.parseInt( maximumMP ) );

			String availableMeat = miscMatcher.group(5).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			KoLCharacter.setAvailableMeat( Integer.parseInt( availableMeat ) );

			String adventuresLeft = miscMatcher.group(6).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = Integer.parseInt( adventuresLeft );
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );
		}
	}

	private static void handleMindControl( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setMindControlLevel( Integer.parseInt( matcher.group(2) ) );
		else
			KoLCharacter.setMindControlLevel( 0 );
	}

	private class CharpaneThread extends Thread
	{
		public void run()
		{	CharpaneRequest.this.run();
		}
	}
}
