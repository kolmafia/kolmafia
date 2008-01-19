package net.sourceforge.kolmafia.webui;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLSettings;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.MoodManager;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class CharPaneDecorator
{
	private static final Pattern COLOR_PATTERN = Pattern.compile( "(color|class)=\"?\'?([^\"\'>]*)" );

	public static final void decorate( final StringBuffer buffer )
	{
		StaticEntity.singleStringReplace( buffer, "<body", "<body onload=\"updateSafetyText();\"" );

		if ( KoLSettings.getBooleanProperty( "relayAddsRestoreLinks" ) )
		{
			CharPaneDecorator.addRestoreLinks( buffer );
		}

		if ( KoLSettings.getBooleanProperty( "relayAddsUpArrowLinks" ) )
		{
			CharPaneDecorator.addUpArrowLinks( buffer );
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

		float threshold = KoLSettings.getFloatProperty( "hpAutoRecoveryTarget" ) * (float) KoLCharacter.getMaximumHP();
		float dangerous = KoLSettings.getFloatProperty( "hpAutoRecovery" ) * (float) KoLCharacter.getMaximumHP();

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

		threshold = KoLSettings.getFloatProperty( "mpAutoRecoveryTarget" ) * (float) KoLCharacter.getMaximumMP();
		dangerous = KoLSettings.getFloatProperty( "mpAutoRecovery" ) * (float) KoLCharacter.getMaximumMP();

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
			moodText = "mood " + KoLSettings.getUserProperty( "currentMood" );
		}
		else if ( MoodManager.getNextBurnCast( false ) != null )
		{
			fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
			moodText = "burn extra mp";
		}
		else if ( !KoLSettings.getBooleanProperty( "relayAddsMoodRefreshLink" ) )
		{
			fontColor = "gray";
			moodText = "burn extra mp";
		}
		else if ( !MoodManager.getTriggers().isEmpty() )
		{
			fontColor = "gray";
			moodText = "mood " + KoLSettings.getUserProperty( "currentMood" );
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

			try
			{
				if ( moodText.startsWith( "mood" ) )
				{
					buffer.append( "mood+execute" );
				}
				else
				{
					buffer.append( URLEncoder.encode( moodText, "UTF-8" ) );
				}
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
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

			try
			{
				if ( moodText.startsWith( "mood" ) )
				{
					buffer.append( "mood+execute" );
				}
				else
				{
					buffer.append( URLEncoder.encode( moodText, "UTF-8" ) );
				}
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
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
				int effectId = EffectDatabase.getEffectId( currentEffect.getName() );
				String descriptionId = EffectDatabase.getDescriptionId( effectId );

				buffer.append( "<tr>" );

				if ( !GenericRequest.isCompactMode || !KoLSettings.getBooleanProperty( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><img src=\"" );
					buffer.append( EffectDatabase.getImage( effectId ) );
					buffer.append( "\" class=hand alt=\"" );
					buffer.append( currentEffect.getName() );
					buffer.append( "\" title=\"" );
					buffer.append( currentEffect.getName() );
					buffer.append( "\" onClick='eff(\"" + descriptionId + "\");'></td>" );
				}

				if ( !GenericRequest.isCompactMode || KoLSettings.getBooleanProperty( "relayTextualizesEffects" ) )
				{
					buffer.append( "<td><font size=2>" );
					buffer.append( currentEffect.getName() );
				}
				else
				{
					buffer.append( "<td><font size=2>" );
				}

				buffer.append( " (0)</font>&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=" );

				try
				{
					buffer.append( URLEncoder.encode( MoodManager.getDefaultAction(
						"lose_effect", currentEffect.getName() ), "UTF-8" ) );
				}
				catch ( Exception e )
				{
					// Hm, something bad happened.  Instead of giving a real link,
					// give a fake link instead.

					buffer.append( "win+game" );
				}

				buffer.append( "&pwd=" );
				buffer.append( GenericRequest.passwordHash );
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

			int nextAppendIndex = text.indexOf( "(", startingIndex ) + 1;
			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			if ( GenericRequest.isCompactMode )
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
			{
				nextAppendIndex = text.indexOf( "(", text.indexOf( "<font size=2>", startingIndex ) ) + 1;
			}

			buffer.append( text.substring( lastAppendIndex, nextAppendIndex ) );
			lastAppendIndex = nextAppendIndex;

			String upkeepAction = MoodManager.getDefaultAction( "lose_effect", effectName );

			if ( upkeepAction.startsWith( "adventure" ) || upkeepAction.endsWith( "snowcone" ) || upkeepAction.endsWith( "mushroom" ) || upkeepAction.endsWith( "cupcake" ) )
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

			// Add a removal link to the duration for buffs which can
			// be removed.  This is either when the buff can be shrugged
			// or the buff has a default removal method.

			if ( skillType == SkillDatabase.BUFF || KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
			{
				removeAction = "uneffect " + effectName;
			}

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
					buffer.append( Character.toUpperCase( removeAction.charAt( 0 ) ) + removeAction.substring( 1 ) + " to remove the " );
				}

				buffer.append( effectName );
				buffer.append( " effect\"" );

				if ( effectName.indexOf( "Poisoned" ) != -1 || effectName.equals( "Beaten Up" ) )
				{
					buffer.append( " style=\"color:red\"" );
				}

				buffer.append( ">" );
			}

			nextAppendIndex = text.indexOf( ")", lastAppendIndex ) + 1;
			int duration = StaticEntity.parseInt( text.substring( lastAppendIndex, nextAppendIndex - 1 ) );

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

				buffer.append( "&pwd=" );
				buffer.append( GenericRequest.passwordHash );
				buffer.append( "\" title=\"Increase rounds of " );
				buffer.append( effectName );
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
}
