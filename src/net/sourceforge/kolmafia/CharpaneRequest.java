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
		this.responseText = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"><style type=\"text/css\">body, td {	font-size: 1em;}</style></head><body bgcolor=white text=black link=black alink=black vlink=black><center><table align=center><tr><td><a target=mainpane href=\"charsheet.php\"><img src=\"http://images.kingdomofloathing.com/otherimages/accordionthief_f.gif\" width=60 height=100 border=0></a></td><td valign=center><center><a target=mainpane href=\"charsheet.php\"><b>Playername</b></a><br>Level 10<br>Sub-Sub-Apprentice Accordion Thief</td></tr></table><table align=center><tr><td align=right>Muscle:</td><td align=left><b><font color=blue>150</font>&nbsp;(78)</b></td></tr><tr><td align=right>Mysticality:</td><td align=left><b><font color=blue>124</font>&nbsp;(79)</b></td></tr><tr><td align=right>Moxie:</td><td align=left><b><font color=blue>161</font>&nbsp;(100)</b></td></tr><tr><td align=right>Temulency:</td><td><b>19</b></td></tr></table><center>You'd better keep an eye on your drinking...</center><table cellpadding=3 align=center><tr><td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/hp.gif\" class=hand onclick='doc(\"hp\");'><br><span class=black>161&nbsp;/&nbsp;165</span></td><td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/mp.gif\" class=hand onclick='doc(\"mp\");'><br><span class=black>14&nbsp;/&nbsp;196</span></td></tr><tr><td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" class=hand onclick='doc(\"meat\");'><br><span class=black>503,480</span></td><td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/hourglass.gif\" class=hand onclick='doc(\"adventures\");'><br><span class=black>324</span></td></tr></table><br><font size=2><b><a class=nounder href=\"mclargehuge.php\" target=mainpane>Last Adventure:</a></b></font><br><center><table cellspacing=0 cellpadding=0><tr><td><font size=2><a target=mainpane href=\"adventure.php?snarfblat=64\">The Icy Peak</a><br></font></td></tr></table></center><center><p><b><font size=2>Effects:</font></b><br></center><center><table><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/happy.gif\" class=hand onClick='eff(\"26\");'></td><td valign=center><font size=2>Mariachi Mood (101)</font><br></td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/notes.gif\" class=hand onClick='eff(\"60\");'></td><td valign=center><font size=2>Aloysius' Antiphon of Aptitude (189)</font><br></td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/notes.gif\" class=hand onClick='eff(\"67\");'></td><td valign=center><font size=2>Fat Leon's Phat Loot Lyric (303)</font><br></td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/notes.gif\" class=hand onClick='eff(\"63\");'></td><td valign=center><font size=2>Polka of Plenty (303)</font><br></td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/happy.gif\" class=hand onClick='eff(\"50\");'></td><td valign=center><font size=2>Empathy (322)</font><br></td></tr><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/string.gif\" class=hand onClick='eff(\"16\");'></td><td valign=center><font size=2>Leash of Linguini (322)</font><br></td></tr></table><p><table width=90%><tr><td colspan=2 align=center><font size=2><b>Familiar:</b></font></td></tr><tr><td align=center valign=center><a target=mainpane href=\"familiar.php\"><img src=\"http://images.kingdomofloathing.com/itemimages/hat2.gif\" width=30 height=30 border=0></a></td><td valign=center align=left><a target=mainpane href=\"familiar.php\"><b><font size=2>Cid</a></b>, the  45-pound Hovering Sombrero</font></td></tr></table></center></center></body></html>";
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
			if ( isCompactMode )
				handleCompactMode( responseText );
			else
				handleExpandedMode( responseText );
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
		if ( responseText.indexOf( "<img src=\"http://images.kingdomofloathing.com/otherimages/inf_small.gif\">" ) == -1 )
		{
			handleStatPoints( responseText, "Mus", "Mys", "Mox" );
			handleMiscPoints( responseText, "HP", "MP", "Adv", "Meat", "", "<b>", "</b>" );
			handleMindControl( responseText, "MC" );
		}
	}

	private static void handleExpandedMode( String responseText ) throws Exception
	{
		if ( responseText.indexOf( "<img src=\"http://images.kingdomofloathing.com/otherimages/inf_small.gif\">" ) == -1 )
		{
			handleStatPoints( responseText, "Muscle", "Mysticality", "Moxie" );
			handleMiscPoints( responseText, "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );
			handleMindControl( responseText, "Mind Control" );
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
