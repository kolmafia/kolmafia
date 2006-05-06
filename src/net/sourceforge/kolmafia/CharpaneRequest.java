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
		responseText = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"><style type=\"text/css\">body, td {	font-size: .8em;}</style></head><body bgcolor=white text=black link=black alink=black vlink=black><centeR><b><a target=mainpane href=\"charsheet.php\">Ivalice</a></b><br>Lvl. 6<hr width=50%><table cellpadding=1 cellspacing=0 align=center><tr><td align=right>Mus:</td><td align=left><b><font color=blue>23</font>&nbsp;(30)</b></td></tr><tr><td align=right>Mys:</td><td align=left><b><font color=blue>12</font>&nbsp;(18)</b></td></tr><tr><td align=right>Mox:</td><td align=left><b><font color=blue>18</font>&nbsp;(19)</b></td></tr></table><hr width=50%><table align=center cellpadding=1 cellspacing=1><tr><td align=right>HP:</td><td align=left><b><font color=black>39/39</font></b></td></tr><tr><td align=right>MP:</td><td align=left><b>1/18</b></td></tr><tr><td align=right>Meat:</td><td align=left><b>6,737</b></td></tr><tr><td align=right><a title=\"Last Adventure: Knob Goblin Laboratory\" target=mainpane href=\"adventure.php?snarfblat=50\">Adv</a>:</td><td align=left><b>184</b></td></tr><tr><td colspan=2 align=center><b>Hardcore</b></td></tr></table><hr width=50%><table cellpadding=1 cellspacing=0><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/beatenup.gif\" class=hand alt=\"Beaten Up\" title=\"Beaten Up\" onClick='eff(\"7\");'></td><td>(3)</td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/clock.gif\" class=hand alt=\"Ticking Clock\" title=\"Ticking Clock\" onClick='eff(\"73\");'></td><td>(195)</td></tr></table><hr width=50%><centeR><a target=mainpane href=\"familiar.php\"><img src=\"http://images.kingdomofloathing.com/itemimages/familiar15.gif\" width=30 height=30 border=0></a><br>11 lbs.</center></center></body></html>";
		processResults();
	}
	
	protected void processResults()
	{	processCharacterPane( this.responseText );
	}
	
	public static void processCharacterPane( String responseText )
	{
		// By refreshing the KoLCharacter pane, you can
		// determine whether or not you are in compact
		// mode - be sure to refresh this value.

		KoLRequest.isCompactMode = responseText.indexOf( "<br>Lvl. " ) != -1;

		// The easiest way to retrieve the KoLCharacter pane
		// data is to use regular expressions.  But, the
		// only data that requires synchronization is the
		// modified stat values, health and mana.

		try
		{
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
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
		}

		KoLCharacter.updateStatus();
	}

	private static void handleCompactMode( String responseText ) throws Exception
	{
		handleStatPoints( responseText, "Mus", "Mys", "Mox" );
		handleMiscPoints( responseText, "HP", "MP", "Meat", "Adv", "", "<b>", "</b>" );
		handleMindControl( responseText, "MC" );
	}

	private static void handleExpandedMode( String responseText ) throws Exception
	{
		handleStatPoints( responseText, "Muscle", "Mysticality", "Moxie" );
		handleMiscPoints( responseText, "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );
		handleMindControl( responseText, "Mind Control" );
	}

	private static void handleStatPoints( String responseText, String musString, String mysString, String moxString ) throws Exception
	{
		int [] modified = new int[3];

		Matcher statMatcher = Pattern.compile( musString + ".*?<b>(.*?)</b>.*?" + mysString + ".*?<b>(.*?)</b>.*?" + moxString + ".*?<b>(.*?)</b>" ).matcher( responseText );

		if ( statMatcher.find() )
		{
			for ( int i = 0; i < 3; ++i )
			{
				Matcher modifiedMatcher = Pattern.compile( "<font color=blue>(.*?)</font>&nbsp;\\((.*?)\\)" ).matcher(
					statMatcher.group( i + 1 ) );

				modified[i] = modifiedMatcher.find() ? df.parse( modifiedMatcher.group(1) ).intValue() :
					df.parse( statMatcher.group( i + 1 ) ).intValue();
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
			KoLCharacter.setHP( df.parse( miscMatcher.group(1).replaceAll( "<.*?>", "" ) ).intValue(), df.parse( miscMatcher.group(2).replaceAll( "<.*?>", "" ) ).intValue(), df.parse( miscMatcher.group(2).replaceAll( "<.*?>", "" ) ).intValue() );
			KoLCharacter.setMP( df.parse( miscMatcher.group(3) ).intValue(), df.parse( miscMatcher.group(4) ).intValue(), df.parse( miscMatcher.group(4) ).intValue() );

			KoLCharacter.setAvailableMeat( df.parse( miscMatcher.group(5) ).intValue() );

			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = df.parse( miscMatcher.group(6) ).intValue();

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );
		}
	}

	private static void handleMindControl( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(.*?)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setMindControlLevel( df.parse( matcher.group(2) ).intValue() );
		else
			KoLCharacter.setMindControlLevel( 0 );
	}
}
