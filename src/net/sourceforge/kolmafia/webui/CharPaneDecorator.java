/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;

import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CharPaneDecorator
{
	private static final Pattern EFFECT_PATTERN = Pattern.compile(
		"onClick='eff\\(.*?(\\d+)(?:</a>)?\\)" );
	private static final Pattern FONT_TAG_PATTERN = Pattern.compile(
		"^((?:<font .*?>)?)(.*?)((?:</font>)?)$", Pattern.DOTALL );

	private static final ArrayList recentLocations = new ArrayList();

	private static final String[][] BIRDFORM_STRINGS =
	{
		{
			"birdformStench",
			"<span title=\"stinkbug->Statue Treatment->buzzard->+meat\"><font color=green>",
			"/5</font></span>, "
		},
		{
			"birdformSleaze",
			"<span title=\"louse->The Bird->great tit->+ML\"><font color=purple>",
			"/5</font></span>, "
		},
		{
			"birdformCold",
			"<span title=\"ice ant->Antarctic Flap->penguin->regen\"><font color=blue>",
			"/5</font></span>, "
		},
		{
			"birdformHot",
			"<span title=\"fire ant->Rise from Ashes->phoenix->+stats\"><font color=red>",
			"/5</font></span>, "
		},
		{
			"birdformSpooky",
			"<span title=\"beetle->Feast on Carrion->raven->+items\"><font color=gray>",
			"/5</font></span>, "
		},
		{
			"birdformRoc",
			"<span title=\"Talon Slash/Wing Buffet->roc->adventures\">",
			"/15</span>, "
		},
	};

	public static final void decorate( final StringBuffer buffer )
	{
		// We are interested in the following sections of the CharPane:
		//
		// Status (HP/MP)
		// Last Adventure
		// Familiar or Minstrel
		// Effects
		// Intrinsics
		//
		// Two interface settings affect the appearance and/or order of
		// these sections:
		//
		// CharPaneRequest.compactCharacterPane
		// CharPaneRequest.familiarBelowEffects

		// Decorate the various sections
		CharPaneDecorator.decorateStatus( buffer );
		CharPaneDecorator.decorateLastAdventure( buffer );
		CharPaneDecorator.decorateFamiliar( buffer );
		CharPaneDecorator.decorateEffects( buffer );
		CharPaneDecorator.decorateIntrinsics( buffer );

		// Update the safety text every time we load the charpane
		StringUtilities.singleStringReplace( buffer, "<body", "<body onload=\"updateSafetyText();\"" );
		// Add a "refresh" link at the end
		StringUtilities.singleStringReplace( buffer, "</body>",
		"<center><font size=1>[<a href=\"charpane.php\">refresh</a>]</font></center></body>" );
	}

	public static final void decorateStatus( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayAddsRestoreLinks" ) )
		{
			return;
		}

		if ( buffer.indexOf( "Astral Spirit" ) != -1 )
		{	// No restoration needed in Valhalla!
			return;
		}

		// First, replace HP information with a restore HP link, if necessary

		float current = KoLCharacter.getCurrentHP();
		float maximum = KoLCharacter.getMaximumHP();
		float target = Preferences.getFloat( "hpAutoRecoveryTarget" );
		float threshold = maximum; // * target
		float dangerous = maximum * Preferences.getFloat( "hpAutoRecovery" );

		CharPaneDecorator.addRestoreLink( buffer, true, current, threshold, dangerous );

		// Next, replace MP information with a restore MP link, if necessary

		current = KoLCharacter.getCurrentMP();
		maximum = KoLCharacter.getMaximumMP();
		target = Preferences.getFloat( "mpAutoRecoveryTarget" );
		threshold = maximum; // * target
		dangerous = maximum * Preferences.getFloat( "mpAutoRecovery" );

		CharPaneDecorator.addRestoreLink( buffer, false, current, threshold, dangerous );
	}

	// Normal:
	//
	//     <td align=center><img src="http://images.kingdomofloathing.com/itemimages/hp.gif" class=hand onclick='doc("hp");' title="Hit Points" alt="Hit Points"><br><span class=black>219&nbsp;/&nbsp;238</span></td>
	//     <td align=center><img src="http://images.kingdomofloathing.com/itemimages/mp.gif" class=hand onclick='doc("mp");' title="Muscularity Points" alt="Muscularity Points"><br><span class=black>44&nbsp;/&nbsp;54</span></td>
	// 
	// Compact:
	//
	//   <tr><td align=right>HP:</td><td align=left><b><font color=black>792/792</font></b></td></tr>
	//   <tr><td align=right>MP:</td><td align=left><b>1398/1628</b></td></tr>

	private static final Pattern POINTS_PATTERN = Pattern.compile( "(<td.*?<br><span.*?>)([\\d,]+)(&nbsp;.*?</td>)" );
	private static final Pattern COMPACT_POINTS_PATTERN = Pattern.compile( "(<td.*?<b>)(?:<font.*?>)?(\\d+)(.*?(?:</font>)?</b></td>)" );

	private static final void addRestoreLink( final StringBuffer buffer, final boolean hp, final float current, final float threshold, final float dangerous )
	{
		// If we don't need restoration, do nothing
		if ( current >= threshold )
		{
			return;
		}

		// Locate current value
		String text = hp ?
			CharPaneDecorator.getHPDatum( buffer ) :
			CharPaneDecorator.getMPDatum( buffer );

		Matcher matcher = CharPaneRequest.compactCharacterPane ? 
			CharPaneDecorator.COMPACT_POINTS_PATTERN.matcher( text ) :
			CharPaneDecorator.POINTS_PATTERN.matcher( text );

		if ( matcher.find() )
		{
			// Craft a replacement for the current value
			StringBuffer rep = new StringBuffer();

			rep.append( matcher.group( 1 ) );
			rep.append( "<a style=\"color:" );
			rep.append( current <= dangerous ? "red" : "black" );
			rep.append( "\" title=\"Restore your " );
			rep.append( hp ? "HP" : "MP" );
			rep.append( "\" href=\"/KoLmafia/sideCommand?cmd=restore+" );
			rep.append( hp ? "hp" : "mp" );
			rep.append( "&pwd=" );
			rep.append( GenericRequest.passwordHash );
			rep.append( "\">" );
			rep.append( matcher.group( 2 ) );
			rep.append( "</a>" );
			rep.append( matcher.group( 3 ) );

			// Replace the original text with the replacement
			StringUtilities.singleStringReplace( buffer, text, rep.toString() );
		}
	}

	private static final String getHPDatum( final StringBuffer buffer )
	{
		int startIndex, endIndex;

		if ( CharPaneRequest.compactCharacterPane )
		{
			startIndex = buffer.indexOf( "<td align=right>HP:" );
			endIndex = buffer.indexOf( "</tr>", startIndex );
		}
		else
		{
			startIndex = buffer.indexOf( "<td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/hp.gif" );
			endIndex = buffer.indexOf( "</td>", startIndex ) + 5;
		}

		return startIndex < 0 ? "" : buffer.substring( startIndex, endIndex );
	}

	private static final String getMPDatum( final StringBuffer buffer )
	{
		int startIndex, endIndex;

		if ( CharPaneRequest.compactCharacterPane )
		{
			startIndex = buffer.indexOf( "<td align=right>MP:" );
			endIndex = buffer.indexOf( "</tr>", startIndex );
		}
		else
		{
			startIndex = buffer.indexOf( "<td align=center><img src=\"http://images.kingdomofloathing.com/itemimages/mp.gif" );
			endIndex = buffer.indexOf( "</td>", startIndex ) + 5;
		}

		return startIndex < 0 ? "" : buffer.substring( startIndex, endIndex );
	}

	private static final Pattern LASTADV_PATTERN = Pattern.compile(
		">Last Adventure.*?<font[^>]*>(.*?)<br></font>.*?</table>" );
	private static final Pattern COMPACT_LASTADV_PATTERN = Pattern.compile(
		"<td align=right>(<a onclick=[^<]+ title=\"Last Adventure: ([^\"]+)\" target=mainpane href=\"([^\"]+)\">.*?</a>:)</td>" );

	public static final void decorateLastAdventure( final StringBuffer buffer )
	{
		int nLinks = Preferences.getInteger( "recentLocations" );

		if ( nLinks <= 1 )
		{
			return;
		}

		Pattern pattern = CharPaneRequest.compactCharacterPane ? COMPACT_LASTADV_PATTERN : LASTADV_PATTERN;
		Matcher matcher = pattern.matcher( buffer );

		if ( !matcher.find() )
		{
			return;
		}

		// group(1) is the link itself, end() is the insertion point for the recent list

		String link;

		if ( CharPaneRequest.compactCharacterPane )
		{
			link = "<a onclick='if (top.mainpane.focus) top.mainpane.focus();' target=mainpane href=\"" + matcher.group( 3 ) + "\">" + matcher.group( 2 ) + "</a>";
		}
		else
		{
			link = matcher.group( 1 );
		}

		if ( CharPaneDecorator.recentLocations.size() == 0 )
		{
			CharPaneDecorator.recentLocations.add( link );
			return;
		}

		if ( !CharPaneDecorator.recentLocations.get( 0 ).equals( link ) )
		{
			CharPaneDecorator.recentLocations.remove( link );
			CharPaneDecorator.recentLocations.add( 0, link );

			while ( CharPaneDecorator.recentLocations.size() > nLinks )
			{
				CharPaneDecorator.recentLocations.remove( nLinks );
			}

		}

		if ( CharPaneDecorator.recentLocations.size() <= 1 )
		{
			return;
		}

		if ( CharPaneRequest.compactCharacterPane )
		{
			StringBuffer linkBuffer = new StringBuffer();

			linkBuffer.append( "<td>" );

			linkBuffer.append( "<span onmouseover=\"document.getElementById('lastadvmenu').style.display = 'inline';\" onmouseout=\"document.getElementById('lastadvmenu').style.display = 'none';\">" );

			linkBuffer.append( "<div style=\"text-align: right\">" );

			linkBuffer.append( matcher.group( 1 ) );

			linkBuffer.append( "</div>" );

			linkBuffer.append( "<span id=\"lastadvmenu\"" );
			linkBuffer.append( " style=\"position: absolute; padding: 5px 5px 5px 5px; background: #f5f5f5; display: none\">" );

			linkBuffer.append( "<font size=1>" );

			for ( int i = 0; i < CharPaneDecorator.recentLocations.size(); ++i )
			{
				if ( i > 0 )
				{
					linkBuffer.append( "<br/>" );
				}

				linkBuffer.append( "<nobr>" );
				linkBuffer.append( CharPaneDecorator.recentLocations.get( i ) );
				linkBuffer.append( "</nobr>" );
			}

			linkBuffer.append( "</font>" );

			linkBuffer.append( "</span>" );
			linkBuffer.append( "</span>" );
			linkBuffer.append( "</td>" );

			buffer.delete( matcher.start(), matcher.end() );
			buffer.insert( matcher.start(), linkBuffer.toString() );
		}
		else
		{
			StringBuffer linkBuffer = new StringBuffer();

			linkBuffer.append( "<font size=1>" );

			for ( int i = 1; i < CharPaneDecorator.recentLocations.size(); ++i )
			{
				if ( i > 1 )
				{
					linkBuffer.append( "<br/>" );
				}

				linkBuffer.append( "<nobr>" );
				linkBuffer.append( CharPaneDecorator.recentLocations.get( i ) );
				linkBuffer.append( "</nobr>" );
			}

			linkBuffer.append( "</font>" );
			buffer.insert( matcher.end(), linkBuffer.toString() );
		}
	}

	public static final void decorateFamiliar( final StringBuffer buffer )
	{
		StringBuffer annotations = CharPaneDecorator.getFamiliarAnnotation();
		if ( annotations == null )
		{
			return;
		}

		if ( CharPaneRequest.compactCharacterPane )
		{
			int pos = buffer.indexOf( "<a target=mainpane href=\"familiar.php\"" );
			if ( pos == -1 ) return;
			annotations.append( "<br>" );
			buffer.insert( pos, annotations );
		}
		else
		{
			int pos = buffer.indexOf( "<b>Familiar:</b>" );
			if ( pos == -1 ) return;
			annotations.insert( 0, "<br>(" );
			annotations.append( ")" );
			buffer.insert( pos + 16, annotations );
		}
	}

	public static final void decorateEffects( final StringBuffer buffer )
	{
		if ( Preferences.getBoolean( "relayAddsUpArrowLinks" ) )
		{
			CharPaneDecorator.addUpArrowLinks( buffer );
		}

		Iterator it = TurnCounter.iterator();
		if ( it.hasNext() )
		{
			CharPaneDecorator.addCounters( buffer, it );
		}

		StringUtilities.singleStringReplace( buffer, "<font size=2>Everything Looks Yellow","<font size=2 color=olive>Everything Looks Yellow" );
		StringUtilities.singleStringReplace( buffer, "<font size=2>Everything Looks Red","<font size=2 color=red>Everything Looks Red" );
		StringUtilities.singleStringReplace( buffer, "<font size=2>Everything Looks Blue","<font size=2 color=blue>Everything Looks Blue" );
	}

	public static final void decorateIntrinsics( final StringBuffer buffer )
	{
	}
	
	public static final StringBuffer getFamiliarAnnotation()
	{
		FamiliarData familiar = KoLCharacter.getEffectiveFamiliar();
		if ( familiar == null )
		{
			return null;
		}

		StringBuffer buffer = new StringBuffer();
		switch ( familiar.getId() )
		{
		case FamiliarPool.TRON:
			buffer.append( Preferences.getString( "_tokenDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.SANDWORM:
			buffer.append( Preferences.getString( "_aguaDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.LLAMA:
			buffer.append( Preferences.getString( "_gongDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.PIXIE:
			buffer.append( Preferences.getString( "_absintheDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.BADGER:
			buffer.append( Preferences.getString( "_astralDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.BANDER:
			buffer.append( Preferences.getString( "_banderRunaways" ) );
			buffer.append( "/" );
			buffer.append( String.valueOf( familiar.getModifiedWeight() / 5 ) );
			return buffer;

		case FamiliarPool.BOOTS:
			buffer.append( Preferences.getString( "_banderRunaways" ) );
			buffer.append( "/" );
			buffer.append( String.valueOf( familiar.getModifiedWeight() / 5 ) );
			buffer.append( " runs" );
			buffer.append( "<br>" );
			buffer.append( Preferences.getString( "_bootStomps" ) );
			buffer.append( "/7 " );
			buffer.append( " stomp" );
			if ( Preferences.getInteger( "_bootStomps" ) != 1  )
			{
				buffer.append( "s" );
			}
			if ( Preferences.getBoolean( "bootsCharged" ) )
			{
				buffer.append( "!" );
			}
			return buffer;

		case FamiliarPool.GIBBERER:
		case FamiliarPool.HARE:
			buffer.append( String.valueOf( Preferences.getInteger( "extraRolloverAdventures" ) - Preferences.getInteger( "_resolutionAdv" ) ) );
			buffer.append( " adv" );
			return buffer;

		case FamiliarPool.SLIMELING:
		{
			buffer.append( "~" );
			buffer.append( String.valueOf( Preferences.getFloat( "slimelingFullness" ) ) );
			buffer.append( " full" );

			int due = Preferences.getInteger( "slimelingStacksDue" );
			int got = Preferences.getInteger( "slimelingStacksDropped" );
			if ( due > got )
			{
				// N stacks drop in N * (N + 1)/2 combats according to
				// <http://ben.bloomroad.com/kol/tower_monsters.html>
				// free runaways do not count
				// each stack drops on the turn it's expected to with
				// no variance
				// int expectedTurns = ( got + 1 ) * ( got // + 2 ) / 2;
			
				buffer.append( "; " );
				buffer.append( String.valueOf( got ) );
				buffer.append( "/" );
				buffer.append( String.valueOf( due ) );
				buffer.append( " stacks" );
			}
			return buffer;
		}

		case FamiliarPool.HIPSTER:
			buffer.append( Preferences.getString( "_hipsterAdv" ) );
			buffer.append( "/7" );
			return buffer;

		case FamiliarPool.GRINDER:
			buffer.append( Preferences.getString( "_pieDrops" ) );
			buffer.append( " pie" );
			if ( Preferences.getInteger( "_pieDrops" ) != 1 )
			{
				buffer.append( "s" );
			}
			buffer.append( "<br>" );
			buffer.append( Preferences.getString( "_piePartsCount" ) );
			buffer.append( " part" );
			if ( Preferences.getInteger( "_piePartsCount" ) != 1 )
			{
				buffer.append( "s" );
			}
			return buffer;

		case FamiliarPool.ALIEN:
			buffer.append( Preferences.getString( "_transponderDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.GROOSE:
			buffer.append( Preferences.getString( "_grooseDrops" ) );
			buffer.append( "/5" );
			return buffer;

		case FamiliarPool.KLOOP:
			buffer.append( Preferences.getString( "_kloopDrops" ) );
			buffer.append( "/5" );
			return buffer;
		}

		return null;
	}

	public static final void addCounters( final StringBuffer buffer, Iterator it )
	{
		TurnCounter current = (TurnCounter) it.next();
		while ( current.getTurnsRemaining() < 0 )
		{	// Skip any expired informational counters that are
			// still pending delivery to a counterScript.
			if ( !it.hasNext() ) return;
			current = (TurnCounter) it.next();
		}
		String text = buffer.toString();
		buffer.setLength( 0 );
		int lastPos = 0;
		int insPos;
		boolean compact = CharPaneRequest.compactCharacterPane;
		Matcher m = CharPaneDecorator.EFFECT_PATTERN.matcher( text );
		while ( m.find() )
		{
			int duration = StringUtilities.parseInt( m.group( 1 ) );
			if ( duration >= current.getTurnsRemaining() )
			{
				insPos = text.lastIndexOf( "<tr>", m.start( 0 ) );
				buffer.append( text.substring( lastPos, insPos ) );
				lastPos = insPos;
				do
				{
					CharPaneDecorator.addOneCounter( buffer, current, compact );
					if ( !it.hasNext() )
					{
						buffer.append( text.substring( lastPos ) );
						return;
					}
					current = (TurnCounter) it.next();
				}
				while ( duration >= current.getTurnsRemaining() );
			}
		}

		// If we've gotten this far, there are counters that are higher
		// than any effect duration.  Insert them at the very end, but
		// before any intrinsics.

		insPos = text.lastIndexOf( "&infin;" );
		if ( insPos != -1 )
		{
			insPos = text.lastIndexOf( "</table>", insPos );
		}
		else
		{
			insPos = text.lastIndexOf( "</table>" );
		}
		if ( insPos == -1 )
		{	// something is very wrong
			insPos = text.length();
		}
		buffer.append( text.substring( lastPos, insPos ) );
		lastPos = insPos;
		while ( true )
		{
			CharPaneDecorator.addOneCounter( buffer, current, compact );
			if ( !it.hasNext() )
			{
				buffer.append( text.substring( lastPos ) );
				return;
			}
			current = (TurnCounter) it.next();
		}
	}

	private static final void addOneCounter( StringBuffer buffer, TurnCounter current, boolean compact )
	{
		String url = current.imageURL();

		if ( CharPaneRequest.compactCharacterPane )
		{
			Matcher m = CharPaneDecorator.FONT_TAG_PATTERN.matcher( current.getLabel() );
			m.find();	// this cannot fail, group 2 matches anything
			buffer.append( "<tr><td>" );
			if ( url != null )
			{
				buffer.append( "<a href=\"" + url + "\" target=\"mainpane\">" );
			}
			buffer.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/" );
			buffer.append( current.getImage() );
			buffer.append( "\" title=\"" );
			buffer.append( m.group( 2 ) );
			buffer.append( "\">" );
			if ( url != null )
			{
				buffer.append( "</a>" );
			}
			buffer.append ("</td><td>" );
			buffer.append( m.group( 1 ) );
			buffer.append ("(<a href=\"/KoLmafia/sideCommand?cmd=counters+deletehash+" );
			buffer.append( System.identityHashCode( current ) );
			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">" );
			buffer.append( current.getTurnsRemaining() );
			buffer.append( "</a>)" );
			buffer.append( m.group( 3 ) );
			buffer.append( "</td></tr>" );
		}
		else	// !compact
		{
			buffer.append( "<tr><td>" );
			if ( url != null )
			{
				buffer.append( "<a href=\"" + url + "\" target=\"mainpane\">" );
			}
			buffer.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/" );
			buffer.append( current.getImage() );
			buffer.append( "\">" );
			if ( url != null )
			{
				buffer.append( "</a>" );
			}
			buffer.append( "</td><td valign=center><font size=2>" );
			buffer.append( current.getLabel() );
			buffer.append( " (<a href=\"/KoLmafia/sideCommand?cmd=counters+deletehash+" );
			buffer.append( System.identityHashCode( current ) );
			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">" );
			buffer.append( current.getTurnsRemaining() );
			buffer.append( "</a>)</td></tr>" );
		}
	}

	public static final void addUpArrowLinks( final StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, add in a link to the sidepane which matches the
		// player's current situation.

		String fontColor = null;
		String moodText = null;

		if ( MoodManager.willExecute( 0 ) )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "mood " + Preferences.getString( "currentMood" );
		}
		else if ( ManaBurnManager.getNextBurnCast() != null )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "burn extra mp";
		}
		else if ( !MoodManager.getTriggers().isEmpty() )
		{
			fontColor = "gray";
			moodText = "mood " + Preferences.getString( "currentMood" );
		}
		else
		{
			for ( int i = 0; i < KoLConstants.activeEffects.size(); ++i )
			{
				AdventureResult currentEffect = (AdventureResult) KoLConstants.activeEffects.get( i );
				if ( currentEffect.getCount() != Integer.MAX_VALUE &&
				     !MoodManager.getDefaultAction( "lose_effect", currentEffect.getName() ).equals( "" ) )
				{
					fontColor = "black";
					moodText = "save as mood";
					break;
				}
			}
		}

		if ( moodText == null )
		{
			// In this case, do nothing, since there aren't any
			// effects that will get saved to a mood, and there's
			// nothing that can be maintained.
		}
		else if ( CharPaneRequest.compactCharacterPane )
		{
			int effectIndex = text.indexOf( "eff(", startingIndex );
			boolean shouldAddDivider = effectIndex == -1;

			if ( shouldAddDivider )
			{
				startingIndex = text.lastIndexOf( "</table>" ) + 8;
			}
			else
			{
				startingIndex = text.lastIndexOf( "<table", effectIndex );
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( shouldAddDivider )
			{
				buffer.append( "<hr width=50%>" );
			}

			buffer.append( "<font size=2 color=" );
			buffer.append( fontColor );

			buffer.append( ">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=" );

			if ( moodText.startsWith( "mood" ) )
			{
				buffer.append( "mood+execute" );
			}
			else
			{
				buffer.append( StringUtilities.getURLEncode( moodText ) );
			}

			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\" style=\"color:" );
			buffer.append( fontColor );
			buffer.append( "\">" );

			buffer.append( moodText );
			buffer.append( "</a>]</font><br><br>" );
		}
		else
		{
			int effectIndex = text.indexOf( "Effects:</font></b>", startingIndex );
			boolean shouldAddDivider = effectIndex == -1;
			boolean shouldAddTable = false;

			if ( shouldAddDivider )
			{
				startingIndex = text.lastIndexOf( "<table" );
				if ( startingIndex < text.lastIndexOf( "target=mainpane" ) )
				{
					startingIndex = text.lastIndexOf( "</center>" );
					shouldAddTable = true;
				}
			}
			else
			{
				startingIndex = text.indexOf( "<br>", effectIndex );
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( shouldAddDivider )
			{
				buffer.append( "<center><p><b><font size=2>Effects:</font></b>" );
			}

			buffer.append( "<br><font size=2 color=" );
			buffer.append( fontColor );

			buffer.append( ">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=" );

			if ( moodText.startsWith( "mood" ) )
			{
				buffer.append( "mood+execute" );
			}
			else
			{
				buffer.append( StringUtilities.getURLEncode( moodText ) );
			}

			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\" style=\"color:" );
			buffer.append( fontColor );
			buffer.append( "\">" );

			buffer.append( moodText );
			buffer.append( "</a>]</font>" );

			if ( shouldAddDivider )
			{
				buffer.append( "</p></center>" );
			}

			if ( shouldAddTable )
			{
				buffer.append( "<table></table>" );
			}
		}

		// Insert any effects which are in your maintenance list which
		// have already run out.

		List missingEffects = MoodManager.getMissingEffects();

		// If the player has at least one effect, then go ahead and add
		// all of their missing effects.

		startingIndex = text.indexOf( "<tr>", lastAppendIndex );
		if ( startingIndex != -1 && !missingEffects.isEmpty() && !KoLConstants.activeEffects.isEmpty() )
		{
			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			AdventureResult currentEffect;

			for ( int i = 0; i < missingEffects.size(); ++i )
			{
				currentEffect = (AdventureResult) missingEffects.get( i );

				String effectName = currentEffect.getName();
				int effectId = EffectDatabase.getEffectId( effectName );
				String escapedEffectName = StringUtilities.getEntityEncode( effectName );

				String descriptionId = EffectDatabase.getDescriptionId( effectId );

				buffer.append( "<tr>" );

				if ( !CharPaneRequest.compactCharacterPane || !Preferences.getBoolean( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><img src=\"" );
					buffer.append( EffectDatabase.getImage( effectId ) );
					buffer.append( "\" class=hand alt=\"" );
					buffer.append( escapedEffectName );
					buffer.append( "\" title=\"" );
					buffer.append( escapedEffectName );
					buffer.append( "\" onClick='eff(\"" + descriptionId + "\");'></td>" );
				}

				if ( !CharPaneRequest.compactCharacterPane || Preferences.getBoolean( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><font size=2>" );
					buffer.append( escapedEffectName );
				}
				else
				{
					buffer.append( "<td><font size=2>" );
				}

				buffer.append( " (0)</font>&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=" );

				buffer.append( StringUtilities.getURLEncode(
					MoodManager.getDefaultAction( "lose_effect", effectName ) ) );

				buffer.append( "&pwd=" );
				buffer.append( GenericRequest.passwordHash );
				buffer.append( "\" title=\"Increase rounds of " );
				buffer.append( escapedEffectName );
				buffer.append( "\"><img src=\"/images/redup.gif\" border=0></a></td></tr>" );
			}
		}

		// Finally, replace all of the shrug off links associated with
		// this response text.

		while ( startingIndex != -1 )
		{
			startingIndex = text.indexOf( "onClick='eff", lastAppendIndex + 1 );
			if ( startingIndex == -1 )
			{
				break;
			}

			startingIndex = text.lastIndexOf( "<", startingIndex );
			AdventureResult effect = CharPaneRequest.extractEffect( text, startingIndex );

			if ( effect == null )
			{
				int nextAppendIndex = text.indexOf( ">", startingIndex ) + 1;
				buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
				lastAppendIndex = nextAppendIndex;
				continue;
			}

			String effectName = effect.getName();
			int duration = effect.getCount();
			String escapedEffectName = StringUtilities.getEntityEncode( effectName );

			int nextAppendIndex = text.indexOf( "(", startingIndex ) + 1;
			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			if ( CharPaneRequest.compactCharacterPane )
			{
				if ( Preferences.getBoolean( "relayTextualizesEffects" ) )
				{
					nextAppendIndex = text.indexOf( "></td>", startingIndex );
					buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
					lastAppendIndex = nextAppendIndex + 6;

					int deleteIndex = buffer.lastIndexOf( "<img" );
					buffer.delete( deleteIndex, buffer.length() );

					buffer.append( "<td align=right><nobr><font size=2>" );
					buffer.append( escapedEffectName );
					buffer.append( "</font></nobr></td>" );
				}

				nextAppendIndex = text.indexOf( "<td>(", startingIndex ) + 5;
			}
			else
			{
				nextAppendIndex = text.indexOf( "(", text.indexOf( "<font size=2>", startingIndex ) ) + 1;
			}

			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			String upkeepAction = MoodManager.getDefaultAction( "lose_effect", effectName );

			if ( upkeepAction.startsWith( "adventure" ) || upkeepAction.endsWith( "mushroom" ) )
			{
				upkeepAction = "";
			}

			String imageAction = null;

			if ( upkeepAction.endsWith( "absinthe" ) )
			{
				imageAction = "Go to Worm Wood";
				upkeepAction = "wormwood.php";
			}

			boolean isShruggable = UneffectRequest.isShruggable( effectName );
			boolean isTimer = effectName.startsWith( "Timer " );
			boolean isIntrinsic = duration == Integer.MAX_VALUE;

			// Add a removal link to the duration for buffs which
			// can be removed.  This is either when the buff can be
			// shrugged or the buff has a default removal method.

			String removeAction = isIntrinsic? "" : MoodManager.getDefaultAction( "gain_effect", effectName );

			if ( effectName.equalsIgnoreCase( "On the Trail" ) )
			{
				buffer.append( StringUtilities.getEntityEncode(
					Preferences.getString( "olfactedMonster" ) ) );
				buffer.append( ", " );
			}
			else if ( effectName.equalsIgnoreCase( "Shape of...Mole!" ) )
			{
				int level = Preferences.getInteger( "moleTunnelLevel" );
				buffer.append( (level >= 0 ? "+" : "") + level );
				buffer.append( ", " );
			}
			else if ( effectName.equalsIgnoreCase( "Form of...Bird!" ) )
			{
				for ( int i = 0; i < BIRDFORM_STRINGS.length; ++i )
				{
					int count = Preferences.getInteger( BIRDFORM_STRINGS[ i ][ 0 ] );
					if ( count != 0 )
					{
						buffer.append( BIRDFORM_STRINGS[ i ][ 1 ] );
						buffer.append( count );
						buffer.append( BIRDFORM_STRINGS[ i ][ 2 ] );
					}
				}
			}

			if ( !removeAction.equals( "" ) )
			{
				buffer.append( "<a href=\"/KoLmafia/sideCommand?cmd=" );
				buffer.append( StringUtilities.getURLEncode( removeAction ) );
				buffer.append( "&pwd=" );
				buffer.append( GenericRequest.passwordHash );
				buffer.append( "\" title=\"" );

				if ( isTimer )
				{
					buffer.append( "Cancel " );
				}
				else if ( isShruggable )
				{
					buffer.append( "Shrug off the " );
				}
				else if ( removeAction.startsWith( "uneffect" ) )
				{
					buffer.append( "Use a remedy to remove the " );
				}
				else
				{
					buffer.append( Character.toUpperCase( removeAction.charAt( 0 ) ) );
					buffer.append( removeAction.substring( 1 ) );
					buffer.append( " to remove the " );
				}

				buffer.append( escapedEffectName );

				if ( !isTimer )
				{
					buffer.append( " effect" );
				}

				buffer.append( "\"" );

				if ( effectName.indexOf( "Poisoned" ) != -1 || effectName.equals( "Beaten Up" ) )
				{
					buffer.append( " style=\"color:red\"" );
				}

				buffer.append( ">" );
			}

			nextAppendIndex = text.indexOf( ")", lastAppendIndex ) + 1;
			buffer.append( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );
			lastAppendIndex = nextAppendIndex;

			if ( isShruggable || !removeAction.equals( "" ) )
			{
				buffer.append( "</a>" );
			}

			buffer.append( ")" );

			// Add the up-arrow icon for buffs which can be maintained, based
			// on information known to the mood maintenance module.

			if ( imageAction != null )
			{
				buffer.append( "&nbsp;<a href=\"" );
				buffer.append( upkeepAction );
				buffer.append( "\" target=\"mainpane\" title=\"" );
				buffer.append( imageAction );
				buffer.append( "\"><img src=\"/images/browser.gif\" width=14 height=14 border=0></a>" );
			}
			else if ( !upkeepAction.equals( "" ) && MoodManager.currentlyExecutable( effect, upkeepAction ))
			{
				buffer.append( "&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=" );
				buffer.append( StringUtilities.getURLEncode( upkeepAction ) );
				buffer.append( "&pwd=" );
				buffer.append( GenericRequest.passwordHash );
				buffer.append( "\" title=\"Increase rounds of " );
				buffer.append( escapedEffectName );
				buffer.append( "\"><img src=\"/images/" );

				if ( duration <= 5 )
				{
					buffer.append( "red" );
				}

				buffer.append( "up.gif\" border=0></a>" );
			}
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}

	public static final void updateFromPreferences()
	{
		CharPaneDecorator.recentLocations.clear();
	}
}
