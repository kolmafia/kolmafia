/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CharPaneDecorator
{
	private static final Pattern COLOR_PATTERN = Pattern.compile( "(color|class)=\"?\'?([^\"\'>]*)" );
	private static final Pattern LASTADV_PATTERN = Pattern.compile(
		">Last Adventure.*?<font[^>]*>(.*?)<br></font>.*?</table>" );
	private static final Pattern EFFECT_PATTERN = Pattern.compile(
		"onClick='eff\\(.*?(\\d+)(?:</a>)?\\)" );
	
	private static final ArrayList recentLocations = new ArrayList();
	
	private static final String[][] BIRDFORM_STRINGS = {
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
		StringUtilities.singleStringReplace( buffer, "<body", "<body onload=\"updateSafetyText();\"" );

		StringUtilities.singleStringReplace( buffer, "</body>",
		"<center><font size=1>[<a href=\"charpane.php\">refresh</a>]</font></center></body>" );

		if ( Preferences.getBoolean( "relayAddsRestoreLinks" ) )
		{
			CharPaneDecorator.addRestoreLinks( buffer );
		}

		if ( Preferences.getBoolean( "relayAddsUpArrowLinks" ) )
		{
			CharPaneDecorator.addUpArrowLinks( buffer );
		}
		
		Iterator it = TurnCounter.iterator();
		if ( it.hasNext() )
		{
			CharPaneDecorator.addCounters( buffer, it );
		}
		
		if ( !GenericRequest.isCompactMode &&
			Preferences.getInteger( "recentLocations" ) >= 1 )
		{
			CharPaneDecorator.addRecentLocations( buffer );
		}
	}

	public static final void addCounters( final StringBuffer buffer, Iterator it )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );
		TurnCounter current = (TurnCounter) it.next();
		int lastPos = 0;
		int insPos;
		boolean compact = GenericRequest.isCompactMode;
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
		// If we've gotten this far, there are counters that are higher than
		// any effect duration.  Insert them at the very end, but before any intrinsics.
		insPos = text.lastIndexOf( "(&infin;)" );
		if ( insPos != -1 )
		{
			insPos = text.lastIndexOf( "</table>", insPos );
		}
		else
		{
			insPos = text.lastIndexOf( "</table>" );
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
		if ( compact )
		{
			buffer.append( "<tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/" );
			buffer.append( current.getImage() );
			buffer.append( "\" title=\"" );
			buffer.append( current.getLabel() );
			buffer.append( "\"></td><td>(<a href=\"/KoLmafia/sideCommand?cmd=counters+deletehash+" );
			buffer.append( System.identityHashCode( current ) );
			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">" );
			buffer.append( current.getTurnsRemaining() );
			buffer.append( "</a>)</td></tr>" );
		}
		else	// !compact
		{
			buffer.append( "<tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/" );
			buffer.append( current.getImage() );
			buffer.append( "\"></td><td valign=center><font size=2>" );
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

	public static final void addRestoreLinks( final StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, locate your HP information inside of the response
		// text and replace it with a restore HP link.

		float threshold = /* Preferences.getFloat( "hpAutoRecoveryTarget" ) * (float) */
			KoLCharacter.getMaximumHP();
		float dangerous = Preferences.getFloat( "hpAutoRecovery" ) * (float) KoLCharacter.getMaximumHP();

		if ( KoLCharacter.getCurrentHP() < threshold )
		{
			if ( GenericRequest.isCompactMode )
			{
				startingIndex = text.indexOf( "<td align=right>HP:", startingIndex );
				startingIndex = text.indexOf( "<b>", startingIndex ) + 3;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
				{
					fontTag = "<font color=red>";
				}
			}
			else
			{
				startingIndex = text.indexOf( "doc(\"hp\")", startingIndex );
				startingIndex = text.indexOf( "<br>", startingIndex ) + 4;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
				{
					fontTag = "<span class=red>";
				}
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( ">", startingIndex ) + 1;
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( GenericRequest.isCompactMode ? "/" : "&", startingIndex );

			if ( !GenericRequest.isCompactMode )
			{
				buffer.append( fontTag );
			}

			buffer.append( "<a title=\"Restore your HP\" href=\"/KoLmafia/sideCommand?cmd=restore+hp&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\" style=\"color:" );

			Matcher colorMatcher = CharPaneDecorator.COLOR_PATTERN.matcher( fontTag );
			if ( colorMatcher.find() )
			{
				buffer.append( colorMatcher.group( 2 ) + "\">" );
			}
			else
			{
				buffer.append( "black\"><b>" );
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
			if ( !GenericRequest.isCompactMode )
			{
				buffer.append( "</span>" );
			}

			buffer.append( fontTag );
		}

		// Next, locate your MP information inside of the response
		// text and replace it with a restore MP link.

		threshold = /*Preferences.getFloat( "mpAutoRecoveryTarget" ) * (float) */
			KoLCharacter.getMaximumMP();
		dangerous = Preferences.getFloat( "mpAutoRecovery" ) * (float) KoLCharacter.getMaximumMP();

		if ( KoLCharacter.getCurrentMP() < threshold )
		{
			if ( GenericRequest.isCompactMode )
			{
				startingIndex = text.indexOf( "<td align=right>MP:", startingIndex );
				startingIndex = text.indexOf( "<b>", startingIndex ) + 3;
			}
			else
			{

				startingIndex = text.indexOf( "doc(\"mp\")", startingIndex );
				startingIndex = text.indexOf( "<br>", startingIndex ) + 4;
				startingIndex = text.indexOf( ">", startingIndex ) + 1;
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "<a style=\"color:" );
			buffer.append( KoLCharacter.getCurrentMP() < dangerous ? "red" : "black" );
			buffer.append( "\" title=\"Restore your MP\" href=\"/KoLmafia/sideCommand?cmd=restore+mp&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">" );
			startingIndex =
				GenericRequest.isCompactMode ? text.indexOf( "/", startingIndex ) : text.indexOf( "&", startingIndex );
			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}

	public static final void addUpArrowLinks( final StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, add in a link to the sidepane which matches the player's
		// current situation.

		String fontColor = null;
		String moodText = null;

		if ( MoodManager.willExecute( 0 ) )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "mood " + Preferences.getString( "currentMood" );
		}
		else if ( MoodManager.getNextBurnCast() != null )
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
			AdventureResult currentEffect;

			for ( int i = 0; i < KoLConstants.activeEffects.size() && moodText == null; ++i )
			{
				currentEffect = (AdventureResult) KoLConstants.activeEffects.get( i );
				if ( !MoodManager.getDefaultAction( "lose_effect", currentEffect.getName() ).equals( "" ) )
				{
					fontColor = "black";
					moodText = "save as mood";
				}
			}
		}

		if ( moodText == null )
		{
			// In this case, do nothing, since there aren't any effects
			// that will get saved to a mood, and there's nothing that
			// can be maintained.
		}
		else if ( GenericRequest.isCompactMode )
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
			if ( effectIndex != -1 )
			{
				startingIndex = text.indexOf( "<br>", effectIndex );
			}
			else
			{
				startingIndex = text.lastIndexOf( "<table" );
				if ( startingIndex < text.lastIndexOf( "target=mainpane" ) )
				{
					startingIndex = text.lastIndexOf( "</center>" );
				}
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( effectIndex == -1 )
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

			if ( effectIndex == -1 )
			{
				buffer.append( "</p></center>" );
			}
		}

		// Insert any effects which are in your maintenance list which
		// have already run out.

		ArrayList missingEffects = MoodManager.getMissingEffects();

		// If the player has at least one effect, then go ahead and add
		// all of their missing effects.

		if ( !KoLConstants.activeEffects.isEmpty() && !missingEffects.isEmpty() )
		{
			startingIndex = text.indexOf( "<tr>", lastAppendIndex );
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

				if ( !GenericRequest.isCompactMode || !Preferences.getBoolean( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><img src=\"" );
					buffer.append( EffectDatabase.getImage( effectId ) );
					buffer.append( "\" class=hand alt=\"" );
					buffer.append( escapedEffectName );
					buffer.append( "\" title=\"" );
					buffer.append( escapedEffectName );
					buffer.append( "\" onClick='eff(\"" + descriptionId + "\");'></td>" );
				}

				if ( !GenericRequest.isCompactMode || Preferences.getBoolean( "relayTextualizesEffects" ) )
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
				continue;
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
			String escapedEffectName = StringUtilities.getEntityEncode( effectName );

			int nextAppendIndex = text.indexOf( "(", startingIndex ) + 1;
			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			if ( GenericRequest.isCompactMode )
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

			String removeAction = MoodManager.getDefaultAction( "gain_effect", effectName );

			String skillName = UneffectRequest.effectToSkill( effectName );
			int skillType = SkillDatabase.getSkillType( SkillDatabase.getSkillId( skillName ) );

			// Add a removal link to the duration for buffs which
			// can be removed.  This is either when the buff can be
			// shrugged or the buff has a default removal method.

			if ( skillType == SkillDatabase.BUFF || InventoryManager.hasItem( UneffectRequest.REMEDY ) )
			{
				removeAction = "uneffect " + effectName;
			}

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

				if ( skillType == SkillDatabase.BUFF )
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
				buffer.append( " effect\"" );

				if ( effectName.indexOf( "Poisoned" ) != -1 || effectName.equals( "Beaten Up" ) )
				{
					buffer.append( " style=\"color:red\"" );
				}

				buffer.append( ">" );
			}

			nextAppendIndex = text.indexOf( ")", lastAppendIndex ) + 1;
			int duration = StringUtilities.parseInt( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );

			buffer.append( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );
			lastAppendIndex = nextAppendIndex;

			if ( skillType == SkillDatabase.BUFF || !removeAction.equals( "" ) )
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
			else if ( !upkeepAction.equals( "" ) )
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

	private static final void addRecentLocations( final StringBuffer buffer )
	{
		Matcher m = LASTADV_PATTERN.matcher( buffer );
		if ( !m.find() )
		{
			return;
		}
		// group(1) is the link itself, end() is the insertion point for the recent list
		String link = m.group( 1 );
		int nLinks = Preferences.getInteger( "recentLocations" );
		if ( CharPaneDecorator.recentLocations.size() == 0 )
		{	// initialize
			CharPaneDecorator.recentLocations.add( link );
			return;
		}
		if ( !CharPaneDecorator.recentLocations.get( 0 ).equals( link ) )
		{
			CharPaneDecorator.recentLocations.remove( link );
			CharPaneDecorator.recentLocations.add( 0, link );
			if ( CharPaneDecorator.recentLocations.size() > nLinks + 1 )
			{
				CharPaneDecorator.recentLocations.subList( nLinks + 1,
					CharPaneDecorator.recentLocations.size() ).clear();
			}
		}
		if ( CharPaneDecorator.recentLocations.size() <= 1 )
		{
			return;
		}
		
		String text = buffer.substring( m.end() );
		buffer.setLength( m.end() );
		buffer.append( "<font size=1>" );
		for ( int i = 1; i < CharPaneDecorator.recentLocations.size(); ++i )
		{
			if ( i > 1 )
			{
				buffer.append( ", " );
			}
			buffer.append( CharPaneDecorator.recentLocations.get( i ) );		
		}
		buffer.append( "</font>" );
		buffer.append( text );
	}
	
	public static final void updateFromPreferences()
	{
		CharPaneDecorator.recentLocations.clear();
	}
}
