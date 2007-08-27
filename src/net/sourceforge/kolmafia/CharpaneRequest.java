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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharpaneRequest extends KoLRequest
{
	private static final Pattern COLOR_PATTERN = Pattern.compile( "(color|class)=\"?\'?([^\"\'>]*)" );
	private static final AdventureResult ABSINTHE = new AdventureResult( "Absinthe-Minded", 1, true );

	private static boolean canInteract = false;
	private static boolean isRunning = false;
	private static boolean isProcessing = false;
	private static String lastResponse = "";

	private static final CharpaneRequest instance = new CharpaneRequest();

	private CharpaneRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charpane.php" );
	}

	public static final CharpaneRequest getInstance()
	{	return instance;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public static final boolean canInteract()
	{	return canInteract;
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

	public static final String getLastResponse()
	{	return lastResponse;
	}

	public static final void processCharacterPane( String responseText )
	{
		if ( isProcessing )
			return;

		lastResponse = responseText;
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
			KoLCharacter.setDetunedRadioVolume( 0 );
			KoLCharacter.setAnnoyotronLevel( 0 );
		}

		refreshEffects( responseText );
		KoLCharacter.updateStatus();

		canInteract = !KoLCharacter.isHardcore() &&
			(KoLCharacter.getCurrentRun() >= 1000 || responseText.indexOf( "storage.php" ) == -1);

		isProcessing = false;
	}

	private static final void handleCompactMode( String responseText )
	{
		try
		{
			handleStatPoints( responseText, "Mus", "Mys", "Mox" );
			handleMiscPoints( responseText, "HP", "MP", "Meat", "Adv", "", "<b>", "</b>" );
			handleMindControl( responseText, "MC" );
			handleDetunedRadio( responseText, "Radio" );
			handleAnnoyotron( responseText, "AOT5K" );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void handleExpandedMode( String responseText )
	{
		try
		{
			handleStatPoints( responseText, "Muscle", "Mysticality", "Moxie" );
			handleMiscPoints( responseText, "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );
			handleMindControl( responseText, "Mind Control" );
			handleDetunedRadio( responseText, "Detuned Radio" );
			handleAnnoyotron( responseText, "Annoy-o-Tron 5k" );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void handleStatPoints( String responseText, String musString, String mysString, String moxString ) throws Exception
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

	private static final void handleMiscPoints( String responseText, String hpString, String mpString, String meatString, String advString, String spacer, String openTag, String closeTag ) throws Exception
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

			if ( oldAdventures != newAdventures )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, newAdventures - oldAdventures ) );
		}
	}

	private static final void handleMindControl( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setMindControlLevel( StaticEntity.parseInt( matcher.group(2) ) );
		else
			KoLCharacter.setMindControlLevel( 0 );
	}

	private static final void handleDetunedRadio( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setDetunedRadioVolume( StaticEntity.parseInt( matcher.group(2) ) );
		else
			KoLCharacter.setDetunedRadioVolume( 0 );
	}

	private static final void handleAnnoyotron( String responseText, String mcString ) throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
			KoLCharacter.setAnnoyotronLevel( StaticEntity.parseInt( matcher.group(2) ) );
		else
			KoLCharacter.setAnnoyotronLevel( 0 );
	}

	public static final AdventureResult extractEffect( String responseText, int searchIndex )
	{
		String effectName = null;
		int durationIndex = -1;

		if ( KoLRequest.isCompactMode )
		{
			int startIndex = responseText.indexOf( "alt=\"", searchIndex ) + 5;
			effectName = responseText.substring( startIndex, responseText.indexOf( "\"", startIndex ) );
			durationIndex = responseText.indexOf( "<td>(", startIndex ) + 5;
		}
		else
		{
			int startIndex = responseText.indexOf( "<font size=2>", searchIndex ) + 13;
			effectName = responseText.substring( startIndex, responseText.indexOf( "(", startIndex ) ).trim();
			durationIndex = responseText.indexOf( "(", startIndex ) + 1;
		}

		searchIndex = responseText.indexOf( "onClick='eff", searchIndex );
		searchIndex = responseText.indexOf( "(", searchIndex ) + 1;

		String descriptionId = responseText.substring( searchIndex + 1, responseText.indexOf( ")", searchIndex ) - 1 );
		int effectId = StatusEffectDatabase.getEffect( descriptionId );

		String duration = responseText.substring( durationIndex,
			responseText.indexOf( ")", durationIndex ) );

		if ( effectId == -1 )
		{
			effectId = StatusEffectDatabase.getEffectId( effectName );

			if ( effectId != -1 )
			{
				RequestLogger.printLine( "Status effect database updated." );
				StatusEffectDatabase.addDescriptionId( effectId, descriptionId );
			}
		}

		if ( duration.indexOf( "&" ) != -1 || duration.indexOf( "<" ) != -1 )
			return null;

		return new AdventureResult( effectName, StaticEntity.parseInt( duration ), true );
	}

	private static final void refreshEffects( String responseText )
	{
		int searchIndex = 0;
		int onClickIndex = 0;

		recentEffects.clear();
		ArrayList visibleEffects = new ArrayList();

		while ( onClickIndex != -1 )
		{
			onClickIndex = responseText.indexOf( "onClick='eff", onClickIndex + 1 );

			if ( onClickIndex == -1 )
				continue;

			searchIndex = responseText.lastIndexOf( "<", onClickIndex );

			AdventureResult effect = extractEffect( responseText, searchIndex );
			if ( effect == null )
				continue;

			int activeCount = effect.getCount( activeEffects );

			if ( effect.getCount() != activeCount )
				StaticEntity.getClient().processResult( effect.getInstance( effect.getCount() - activeCount ) );

			visibleEffects.add( effect );
		}

		KoLmafia.applyEffects();
		activeEffects.retainAll( visibleEffects );

		if ( StaticEntity.isCounting( "Wormwood" ) )
			return;

		int absintheCount = ABSINTHE.getCount( activeEffects );

		if ( absintheCount > 8 )
			StaticEntity.startCounting( absintheCount - 9, "Wormwood", "tinybottle.gif" );
		else if ( absintheCount > 4 )
			StaticEntity.startCounting( absintheCount - 5, "Wormwood", "tinybottle.gif" );
		else if ( absintheCount > 0 )
			StaticEntity.startCounting( absintheCount - 1, "Wormwood", "tinybottle.gif" );
	}

	public static final void decorate( StringBuffer buffer )
	{
		StaticEntity.singleStringReplace( buffer, "<body", "<body onLoad=\"updateSafetyText();\"" );

		if ( KoLSettings.getBooleanProperty( "relayAddsRestoreLinks" ) )
			addRestoreLinks( buffer );

		if ( KoLSettings.getBooleanProperty( "relayAddsUpArrowLinks" ) )
			addUpArrowLinks( buffer );
	}

	public static final void addRestoreLinks( StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String fontTag = "";

		int startingIndex = 0;
		int lastAppendIndex = 0;

		// First, locate your HP information inside of the response
		// text and replace it with a restore HP link.

		float threshold = KoLSettings.getFloatProperty( "hpAutoRecoveryTarget" ) * ((float) KoLCharacter.getMaximumHP());
		float dangerous = KoLSettings.getFloatProperty( "hpAutoRecovery" ) * ((float) KoLCharacter.getMaximumHP());

		if ( KoLCharacter.getCurrentHP() < threshold )
		{
			if ( KoLRequest.isCompactMode )
			{
				startingIndex = text.indexOf( "<td align=right>HP:", startingIndex );
				startingIndex = text.indexOf( "<b>", startingIndex ) + 3;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
					fontTag = "<font color=red>";
			}
			else
			{
				startingIndex = text.indexOf( "doc(\"hp\")", startingIndex );
				startingIndex = text.indexOf( "<br>", startingIndex ) + 4;

				fontTag = text.substring( startingIndex, text.indexOf( ">", startingIndex ) + 1 );
				if ( KoLCharacter.getCurrentHP() < dangerous )
					fontTag = "<span class=red>";
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( ">", startingIndex ) + 1;
			lastAppendIndex = startingIndex;

			startingIndex = text.indexOf( KoLRequest.isCompactMode ? "/" : "&", startingIndex );

			if ( !KoLRequest.isCompactMode )
				buffer.append( fontTag );

			buffer.append( "<a title=\"Restore your HP\" href=\"/KoLmafia/sideCommand?cmd=restore+hp\" style=\"color:" );

			Matcher colorMatcher = COLOR_PATTERN.matcher( fontTag );
			if ( colorMatcher.find() )
				buffer.append( colorMatcher.group(2) + "\">" );
			else
				buffer.append( "black\"><b>" );

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
			if ( !KoLRequest.isCompactMode )
				buffer.append( "</span>" );

			buffer.append( fontTag );
		}

		// Next, locate your MP information inside of the response
		// text and replace it with a restore MP link.

		threshold = KoLSettings.getFloatProperty( "mpAutoRecoveryTarget" ) * ((float) KoLCharacter.getMaximumMP());
		dangerous = KoLSettings.getFloatProperty( "mpAutoRecovery" ) * ((float) KoLCharacter.getMaximumMP());

		if ( KoLCharacter.getCurrentMP() < threshold )
		{
			if ( KoLRequest.isCompactMode )
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
			buffer.append( "\" title=\"Restore your MP\" href=\"/KoLmafia/sideCommand?cmd=restore+mp\">" );
			startingIndex = KoLRequest.isCompactMode ? text.indexOf( "/", startingIndex ) : text.indexOf( "&", startingIndex );
			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			buffer.append( "</a>" );
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}

	public static final void addUpArrowLinks( StringBuffer buffer )
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

		if ( MoodSettings.willExecute( 0 ) )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "mood " + KoLSettings.getUserProperty( "currentMood" );
		}
		else if ( MoodSettings.getNextBurnCast( false ) != null )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "burn extra mp";
		}
		else if ( !KoLSettings.getBooleanProperty( "relayAddsMoodRefreshLink" ) )
		{
			fontColor = "gray";
			moodText = "burn extra mp";
		}
		else if ( !MoodSettings.getTriggers().isEmpty() )
		{
			fontColor = "gray";
			moodText = "mood " + KoLSettings.getUserProperty( "currentMood" );
		}
		else
		{
			AdventureResult currentEffect;

			for ( int i = 0; i < activeEffects.size() && moodText == null; ++i )
			{
				currentEffect = (AdventureResult) activeEffects.get(i);
				if ( !MoodSettings.getDefaultAction( "lose_effect", currentEffect.getName() ).equals( "" ) )
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
		else if ( KoLRequest.isCompactMode )
		{
			int effectIndex = text.indexOf( "eff(", startingIndex );
			boolean shouldAddDivider = effectIndex == -1;

			if ( shouldAddDivider )
				startingIndex = text.lastIndexOf( "</table>" ) + 8;
			else
				startingIndex = text.lastIndexOf( "<table", effectIndex );

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( shouldAddDivider )
				buffer.append( "<hr width=50%>" );

			buffer.append( "<font size=2 color=" );
			buffer.append( fontColor );

			buffer.append( ">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=" );

			try
			{
				if ( moodText.startsWith( "mood" ) )
					buffer.append( "mood+execute" );
				else
					buffer.append( URLEncoder.encode( moodText, "UTF-8" ) );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

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
					startingIndex = text.lastIndexOf( "</center>" );
			}

			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			if ( effectIndex == -1 )
				buffer.append( "<center><p><b><font size=2>Effects:</font></b>" );

			buffer.append( "<br><font size=2 color=" );
			buffer.append( fontColor );

			buffer.append( ">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=" );

			try
			{
				if ( moodText.startsWith( "mood" ) )
					buffer.append( "mood+execute" );
				else
					buffer.append( URLEncoder.encode( moodText, "UTF-8" ) );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			buffer.append( "\" style=\"color:" );
			buffer.append( fontColor );
			buffer.append( "\">" );

			buffer.append( moodText );
			buffer.append( "</a>]</font>" );

			if ( effectIndex == -1 )
				buffer.append( "</p></center>" );
		}

		// Insert any effects which are in your maintenance list which
		// have already run out.

		ArrayList missingEffects = MoodSettings.getMissingEffects();

		// If the player has at least one effect, then go ahead and add
		// all of their missing effects.

		if ( !activeEffects.isEmpty() && !missingEffects.isEmpty() )
		{
			startingIndex = text.indexOf( "<tr>", lastAppendIndex );
			buffer.append( text.substring( lastAppendIndex, startingIndex ) );
			lastAppendIndex = startingIndex;

			AdventureResult currentEffect;

			for ( int i = 0; i < missingEffects.size(); ++i )
			{
				currentEffect = (AdventureResult) missingEffects.get(i);
				int effectId = StatusEffectDatabase.getEffectId( currentEffect.getName() );
				String descriptionId = StatusEffectDatabase.getDescriptionId( effectId );

				buffer.append( "<tr>" );

				if ( !KoLRequest.isCompactMode || !KoLSettings.getBooleanProperty( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><img src=\"" );
					buffer.append( StatusEffectDatabase.getImage( effectId ) );
					buffer.append( "\" class=hand alt=\"" );
					buffer.append( currentEffect.getName() );
					buffer.append( "\" title=\"" );
					buffer.append( currentEffect.getName() );
					buffer.append( "\" onClick='eff(\"" + descriptionId + "\");'></td>" );
				}

				if ( !KoLRequest.isCompactMode || KoLSettings.getBooleanProperty( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><font size=2>" );

					if ( !KoLRequest.isCompactMode )
						buffer.append( currentEffect.getName() );
					else
						buffer.append( "<nobr>" + StatusEffectDatabase.getEffectName( effectId ) + "</nobr>" );
				}
				else
					buffer.append( "<td><font size=2>" );

				buffer.append( " (0)</font>&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=" );

				try
				{
					buffer.append( URLEncoder.encode(
						MoodSettings.getDefaultAction( "lose_effect", currentEffect.getName() ), "UTF-8" ) );
				}
				catch ( Exception e )
				{
					// Hm, something bad happened.  Instead of giving a real link,
					// give a fake link instead.

					buffer.append( "win+game" );
				}

				buffer.append( "\" title=\"Increase rounds of " );
				buffer.append( currentEffect.getName() );
				buffer.append( "\"><img src=\"/images/redup.gif\" border=0></a></td></tr>" );
			}
		}

		// Finally, replace all of the shrug off links associated with
		// this response text.

		while ( startingIndex != -1 )
		{
			startingIndex = text.indexOf( "onClick='eff", lastAppendIndex + 1 );

			if ( startingIndex == -1 )
				continue;

			startingIndex = text.lastIndexOf( "<", startingIndex );
			AdventureResult effect = CharpaneRequest.extractEffect( text, startingIndex );

			if ( effect == null )
			{
				int nextAppendIndex = text.indexOf( ">", startingIndex ) + 1;
				buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
				lastAppendIndex = nextAppendIndex;
				continue;
			}

			String effectName = effect.getName();

			int nextAppendIndex = text.indexOf( "(", startingIndex ) + 1;
			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			if ( KoLRequest.isCompactMode )
			{
				if ( KoLSettings.getBooleanProperty( "relayTextualizesEffects" ) )
				{
					nextAppendIndex = text.indexOf( "></td>", startingIndex );
					buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
					lastAppendIndex = nextAppendIndex + 6;

					int deleteIndex = buffer.lastIndexOf( "<img" );
					buffer.delete( deleteIndex, buffer.length() );

					buffer.append( "<td align=right><nobr><font size=2>" );
					buffer.append( effectName );
					buffer.append( "</font></nobr></td>" );
				}

				nextAppendIndex = text.indexOf( "<td>(", startingIndex ) + 5;
			}
			else
				nextAppendIndex = text.indexOf( "(", text.indexOf( "<font size=2>", startingIndex ) ) + 1;

			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			String upkeepAction = MoodSettings.getDefaultAction( "lose_effect", effectName );

			if ( upkeepAction.endsWith( "snowcone" ) || upkeepAction.endsWith( "mushroom" ) || upkeepAction.endsWith( "cupcake" ) )
				upkeepAction = "";

			String imageAction = null;

			if ( upkeepAction.endsWith( "absinthe" ) )
			{
				imageAction = "Go to Worm Wood";
				upkeepAction = "wormwood.php";
			}

			String removeAction = MoodSettings.getDefaultAction( "gain_effect", effectName );

			String skillName = UneffectRequest.effectToSkill( effectName );
			int skillType = ClassSkillsDatabase.getSkillType( ClassSkillsDatabase.getSkillId( skillName ) );

			// Add a removal link to the duration for buffs which can
			// be removed.  This is either when the buff can be shrugged
			// or the buff has a default removal method.

			if ( skillType == ClassSkillsDatabase.BUFF || KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
				removeAction = "uneffect " + effectName;

			if ( !removeAction.equals( "" ) )
			{
				buffer.append( "<a href=\"/KoLmafia/sideCommand?cmd=" );

				try
				{
					buffer.append( URLEncoder.encode( removeAction, "UTF-8" ) );
				}
				catch ( Exception e )
				{
					// Hm, something bad happened.  Instead of giving a real link,
					// give a fake link instead.

					buffer.append( "win+game" );
				}

				buffer.append( "\" title=\"" );

				if ( skillType == ClassSkillsDatabase.BUFF )
					buffer.append( "Shrug off the " );
				else if ( removeAction.startsWith( "uneffect" ) )
					buffer.append( "Use a remedy to remove the " );
				else
					buffer.append( Character.toUpperCase( removeAction.charAt(0) ) + removeAction.substring(1) + " to remove the " );

				buffer.append( effectName );
				buffer.append( " effect\"" );

				if ( effectName.indexOf( "Poisoned" ) != -1 || effectName.equals( "Beaten Up" ) )
					buffer.append( " style=\"color:red\"" );

				buffer.append( ">" );
			}

			nextAppendIndex = text.indexOf( ")", lastAppendIndex ) + 1;
			int duration = StaticEntity.parseInt( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );

			buffer.append( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );
			lastAppendIndex = nextAppendIndex;

			if ( skillType == ClassSkillsDatabase.BUFF || !removeAction.equals( "" ) )
				buffer.append( "</a>" );

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

				try
				{
					buffer.append( URLEncoder.encode( upkeepAction, "UTF-8" ) );
				}
				catch ( Exception e )
				{
					// Hm, something bad happened.  Instead of giving a real link,
					// give a fake link instead.

					buffer.append( "win+game" );
				}

				buffer.append( "\" title=\"Increase rounds of " );
				buffer.append( effectName );
				buffer.append( "\"><img src=\"/images/" );

				if ( duration <= 5 )
					buffer.append( "red" );

				buffer.append( "up.gif\" border=0></a>" );
			}
		}

		buffer.append( text.substring( lastAppendIndex ) );
	}
}
