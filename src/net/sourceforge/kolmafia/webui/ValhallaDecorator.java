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
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ValhallaDecorator
{
	public static final void decorateGashJump( final StringBuffer buffer )
	{
		buffer.delete( buffer.indexOf( "<p>Are you" ), buffer.indexOf( "<p><center>" ) );
		StringUtilities.singleStringReplace( buffer, "<p>Please", " Please" );

		StringBuffer predictions = new StringBuffer();

		predictions.append( "</center></td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );
		predictions.append( "<td><div style=\"padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>" );
		HolidayDatabase.addPredictionHTML( predictions, new Date(), HolidayDatabase.getPhaseStep(), false );
		predictions.append( "</font></div></td></tr><tr><td colspan=3><br>" );
		predictions.append( KoLConstants.LINE_BREAK );
		predictions.append( KoLConstants.LINE_BREAK );

		StringUtilities.singleStringReplace( buffer, "</center><p>", predictions.toString() );

		int startPoint = SkillDatabase.classSkillsBase();

		StringBuffer reminders = new StringBuffer();

		reminders.append( "<br><table>" );
		reminders.append( "<tr><td><input type=submit class=button value=\"Ascend\"><input type=hidden name=confirm value=on><input type=hidden name=confirm2 value=on></td></tr>" );
		reminders.append( "</table>" );

		reminders.append( "<br><table cellspacing=10 cellpadding=10><tr>" );

		ArrayList skillList = new ArrayList();
		ArrayList unpermedSkills = new ArrayList();
		for ( int i = 0; i < KoLConstants.availableSkills.size(); ++i )
		{
			UseSkillRequest skill = (UseSkillRequest) KoLConstants.availableSkills.get( i );
			skillList.add( String.valueOf( skill.getSkillId() ) );
			if ( !KoLConstants.permedSkills.contains( skill ) )
			{
				unpermedSkills.add( skill );
			}
		}

		reminders.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Haven't Yet Permed</th></tr><tr><td align=center><font size=\"-1\">" );
		ValhallaDecorator.listPermableSkills( reminders, unpermedSkills );
		reminders.append( "</font></td></tr></table></td>" );

		reminders.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Didn't Buy</th></tr><tr><td align=center><font size=\"-1\">" );
		ValhallaDecorator.listPermanentSkills( reminders, skillList, startPoint );
		reminders.append( "</font></td></tr></table></td>" );

		reminders.append( "<td bgcolor=\"#eeeeff\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Common Stuff You Didn't Do</th></tr><tr><td align=center><font size=\"-1\">" );
		ValhallaDecorator.listCommonTasks( reminders );
		reminders.append( "</font></td></tr></table></td>" );

                reminders.append( "</tr></table><br><br>" );

		StringUtilities.singleStringReplace(
			buffer,
			"<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)",
			reminders.toString() );

		return;
	}

	public static final void decorateAfterLife( final String location, final StringBuffer buffer )
	{
		if ( !location.startsWith( "afterlife.php" ) )
		{
			return;
		}
	}

	// The following is obsolete
	private static final void decorateOldAfterlife( final String location, final StringBuffer buffer )
	{
		// What we're going to do is kill the standard form and replace
		// it with one that requires a lot less scrolling while still
		// retaining all of the form fields.  But first, extract needed
		// information from it.

		ArrayList softSkills = new ArrayList();
		ArrayList hardSkills = new ArrayList();

		Matcher permedMatcher =
			Pattern.compile( "<b>Permanent Skills:</b>.*?</table>", Pattern.DOTALL ).matcher( buffer.toString() );
		if ( permedMatcher.find() )
		{
			Matcher skillMatcher =
				Pattern.compile( "desc_skill.php\\?whichskill=(\\d+)[^>]+>[^<+]+</a>(.*?)</td>" ).matcher(
					permedMatcher.group() );

			while ( skillMatcher.find() )
			{
				softSkills.add( skillMatcher.group( 1 ) );
				if ( skillMatcher.group( 2 ).length() > 0 )
				{
					hardSkills.add( skillMatcher.group( 1 ) );
				}
			}
		}

		ArrayList recentSkills = new ArrayList();
		Matcher recentMatcher =
			Pattern.compile( "<b>Current Skills:</b>.*?</table>", Pattern.DOTALL ).matcher( buffer.toString() );
		if ( recentMatcher.find() )
		{
			Matcher skillMatcher = Pattern.compile( "value=(\\d+)" ).matcher( recentMatcher.group() );
			while ( skillMatcher.find() )
			{
				recentSkills.add( skillMatcher.group( 1 ) );
			}
		}

		boolean badMoon = buffer.indexOf( "You have unlocked the Bad Moon sign for your next run." ) != -1;

		// Now we begin replacing the standard Valhalla form with one
		// that is much more compact.

		int endIndex = buffer.indexOf( "</form>" );
		String suffix = buffer.toString().substring( endIndex + 7 );
		buffer.delete( buffer.indexOf( "<form" ), buffer.length() );

		String skillListScript =
			"var a, b; if ( document.getElementById( 'skillsview' ).options[0].selected ) { a = 'soft'; b = 'hard'; } else { a = 'hard'; b = 'soft'; } document.getElementById( a + 'skills' ).style.display = 'inline'; document.getElementById( b + 'skills' ).style.display = 'none'; void(0);";

		// Add some holiday predictions to the page to make things more
		// useful, since people sometimes forget KoLmafia has a
		// calendar.

		buffer.append( "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<td><div style=\"background-color: #ffffcc; padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>" );
		HolidayDatabase.addPredictionHTML( buffer, new Date(), HolidayDatabase.getPhaseStep() );
		buffer.append( "</font></div></td></tr><tr><td colspan=3><br><br>" );
		buffer.append( KoLConstants.LINE_BREAK );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<form name=\"ascform\" action=valhalla.php method=post onSubmit=\"document.ascform.whichsign.value = document.ascform.whichsignhc.value; return true;\">" );
		buffer.append( "<input type=hidden name=action value=\"resurrect\"><input type=hidden name=pwd value=\"" );
		buffer.append( GenericRequest.passwordHash );
		buffer.append( "\"><center><table>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Lifestyle:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"asctype\"><option value=1>Casual</option><option value=2>Softcore</option><option value=3 selected>Hardcore</option></select>" );
		buffer.append( "</td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>New Class:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichclass\"><option value=0 selected></option>" );
		buffer.append( "<option value=1>Seal Clubber</option><option value=2>Turtle Tamer</option>" );
		buffer.append( "<option value=3>Pastamancer</option><option value=4>Sauceror</option>" );
		buffer.append( "<option value=5>Disco Bandit</option><option value=6>Accordion Thief</option>" );
		buffer.append( "</select></td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Gender:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"gender\"><option value=1" );
		if ( !KoLCharacter.getAvatar().endsWith( "_f.gif" ) )
		{
			buffer.append( " selected" );
		}
		buffer.append( ">Male</option><option value=2" );
		if ( KoLCharacter.getAvatar().endsWith( "_f.gif" ) )
		{
			buffer.append( " selected" );
		}
		buffer.append( ">Female</option></select>" );

		buffer.append( "</td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td colspan=2>&nbsp;</td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Moon Sign:</b>&nbsp;</td><td>" );
		buffer.append( "<input type=\"hidden\" name=\"whichsign\" value=\"\">" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichsignhc\"><option value=0 selected></option>" );
		buffer.append( "<option value=0>-- Muscle signs --</option>" );
		buffer.append( "<option value=1>The Mongoose (+musc stats)</option>" );
		buffer.append( "<option value=2>The Wallaby (+fam weight)</option>" );
		buffer.append( "<option value=3>The Vole (+criticals)</option>" );
		buffer.append( "<option value=0>-- Mysticality signs --</option>" );
		buffer.append( "<option value=4>The Platypus (+myst stats)</option>" );
		buffer.append( "<option value=5>The Opossum (+adv/food)</option>" );
		buffer.append( "<option value=6>The Marmot (+clovers)</option>" );
		buffer.append( "<option value=0>-- Moxie signs --</option>" );
		buffer.append( "<option value=7>The Wombat (+moxie stats)</option>" );
		buffer.append( "<option value=8>The Blender (+adv/booze)</option>" );
		buffer.append( "<option value=9>The Packrat (+drops)</option>" );
		if ( badMoon )
		{
			buffer.append( "<option value=0></option>" );
			buffer.append( "<option value=10>Bad Moon</option>" );
		}
		buffer.append( "</select></td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Restrictions:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichpath\"><option value=0 selected>No dietary restrictions</option><option value=1>Boozetafarian</option><option value=2>Teetotaler</option><option value=3>Oxygenarian</option></select>" );
		buffer.append( "</td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Skill to Keep:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=keepskill><option value=9999 selected></option><option value=\"-1\" selected>(no skill)</option>" );

		int skillId;
		for ( int i = 0; i < recentSkills.size(); ++i )
		{
			skillId = Integer.parseInt( (String) recentSkills.get( i ) );
			if ( skillId == 0 )
			{
				continue;
			}

			buffer.append( "<option value=" );
			buffer.append( skillId );
			buffer.append( ">" );
			buffer.append( SkillDatabase.getSkillName( skillId ) );

			if ( skillId % 1000 == 0 )
			{
				buffer.append( " (Trivial)" );
			}

			buffer.append( "</option>" );
		}

		buffer.append( "</select></td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td colspan=2>&nbsp;</td></tr><tr><td>&nbsp;</td><td>" );
		buffer.append( "<input class=button type=submit value=\"Resurrect\"><input type=hidden name=\"confirm\" value=on></td></tr></table></center></form>" );
		buffer.append( KoLConstants.LINE_BREAK );
		buffer.append( KoLConstants.LINE_BREAK );

		// Finished with adding all the data in a more compact form.
		// Now, we go ahead and add in all the missing data that
		// players might want to look at to see which class to go for
		// next.

		buffer.append( "<center><br><br><select id=\"skillsview\" onchange=\"" + skillListScript + "\"><option>Unpermed Softcore Skills</option><option selected>Unpermed Hardcore Skills</option></select>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<br><br><div id=\"softskills\" style=\"display:none\">" );
		buffer.append( KoLConstants.LINE_BREAK );
		ValhallaDecorator.createSkillTable( buffer, softSkills );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "</div><div id=\"hardskills\" style=\"display:inline\">" );
		buffer.append( KoLConstants.LINE_BREAK );
		ValhallaDecorator.createSkillTable( buffer, hardSkills );
		buffer.append( KoLConstants.LINE_BREAK );
		buffer.append( "</div></center>" );
		buffer.append( KoLConstants.LINE_BREAK );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( suffix );
	}

	private static final void createSkillTable( final StringBuffer buffer, final ArrayList skillList )
	{
		buffer.append( "<table width=\"80%\"><tr>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ffcccc\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Muscle Skills</th></tr><tr><td><font size=\"-1\">" );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 1000 );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 2000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccccff\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Mysticality Skills</th></tr><tr><td><font size=\"-1\">" );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 3000 );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 4000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "<td valign=\"top\" bgcolor=\"#ccffcc\"><table><tr><th style=\"text-decoration: underline; text-align: left;\">Moxie Skills</th></tr><tr><td><font size=\"-1\">" );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 5000 );
		ValhallaDecorator.listPermanentSkills( buffer, skillList, 6000 );
		buffer.append( "</font></td></tr></table></td>" );
		buffer.append( "</tr></table>" );
	}

	private static final void listPermableSkills( final StringBuffer buffer, final ArrayList unpermedSkills )
	{
		for ( int i = 0; i < unpermedSkills.size(); ++i )
		{
			UseSkillRequest skill = (UseSkillRequest)unpermedSkills.get( i );
			int skillId = skill.getSkillId();

			if ( !SkillDatabase.isPermable( skillId ) )
			{
				continue;
			}

			String skillName = skill.getSkillName();

			buffer.append( "<nobr>" );
			buffer.append( "<a onClick=\"skill('" );
			buffer.append( skillId );
			buffer.append( "');\">" );
			buffer.append( skillName );
			buffer.append( "</a>" );
			buffer.append( "</nobr><br>" );
		}
	}

	private static final void listPermanentSkills( final StringBuffer buffer, final ArrayList skillList, final int startingPoint )
	{
		for ( int i = 0; i < 100; ++i )
		{
			int skillId = startingPoint + i;

			// Special case: don't torment Seal Clubbers by listing
			// Lunge-Smack as a purchasable skill. If you already
			// have it, it IS permable
			if ( skillId == 1004 )
			{
				continue;
			}

			String skillName = SkillDatabase.getSkillName( skillId );
			if ( skillName == null )
			{
				continue;
			}

			if ( !SkillDatabase.isPermable( skillId ) )
			{
				continue;
			}

			buffer.append( "<nobr>" );
			boolean alreadyPermed = skillList.contains( String.valueOf( skillId ) );
			if ( alreadyPermed )
			{
				buffer.append( "<font color=darkgray><s>" );
			}

			buffer.append( "<a onClick=\"skill('" );
			buffer.append( skillId );
			buffer.append( "');\">" );
			buffer.append( skillName );
			buffer.append( "</a>" );

			if ( alreadyPermed )
			{
				buffer.append( "</s></font>" );
			}

			buffer.append( "</nobr><br>" );
		}
	}

	private static final void listCommonTasks( final StringBuffer buffer )
	{
		RelayRequest.redirectedCommandURL = "/ascend.php";

		int count = InventoryManager.getCount( ItemPool.INSTANT_KARMA );
		if ( count > 0 )
		{
			int banked = Preferences.getInteger( "bankedKarma" );
			buffer.append( "<nobr><a href=\"javascript:if(confirm('Are you sure you want to discard your Instant Karma?')) singleUse('inventory.php?which=1&action=discard&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "&whichitem=");
			buffer.append( ItemPool.INSTANT_KARMA );
			buffer.append( "&ajax=1');void(0);\">discard karma</a> (have " );
			buffer.append( count );
			buffer.append( ", banked " );
			buffer.append( banked );
			buffer.append( ")</nobr><br>" );
		}

		if ( KoLCharacter.hasChef() )
		{
			buffer.append( "<nobr><a href=\"craft.php?mode=cook\">blow up your chef</a></nobr><br>" );
		}

		if ( KoLCharacter.hasBartender() )
		{
			buffer.append( "<nobr><a href=\"craft.php?mode=cocktail\">blow up your bartender</a></nobr><br>" );
		}

		if ( KoLCharacter.getZapper() != null )
		{
			buffer.append( "<nobr><a href=\"wand.php?whichwand=" );
			buffer.append( KoLCharacter.getZapper() );
			buffer.append( "\">blow up your zap wand</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.DEAD_MIMIC ) )
		{
			buffer.append( "<nobr><a href=\"javascript:singleUse('inv_use.php?&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "&which=3&whichitem=" );
			buffer.append( ItemPool.DEAD_MIMIC );
			buffer.append( "&ajax=1')\">use your dead mimic</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.BORIS_KEY ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
			buffer.append( InventoryManager.getAccessibleCount( ItemPool.BORIS_KEY ) );
			buffer.append( "+Boris&#39;s+key+lime+pie&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make a Boris's key lime pie</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.JARLSBERG_KEY ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
			buffer.append( InventoryManager.getAccessibleCount( ItemPool.JARLSBERG_KEY ) );
			buffer.append( "+Jarlsberg&#39;s+key+lime+pie&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make a Jarlsberg's key lime pie</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.SNEAKY_PETE_KEY ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
			buffer.append( InventoryManager.getAccessibleCount( ItemPool.SNEAKY_PETE_KEY ) );
			buffer.append( "+Sneaky+Pete&#39;s+key+lime+pie" );
			buffer.append( "&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make a Sneaky Pete's key lime pie</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.DIGITAL_KEY ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
			buffer.append( InventoryManager.getAccessibleCount( ItemPool.DIGITAL_KEY ) );
			buffer.append( "+digital+key+lime+pie&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make a digital key lime pie</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.STAR_KEY ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
			buffer.append( InventoryManager.getAccessibleCount( ItemPool.STAR_KEY ) );
			buffer.append( "+star+key+lime+pie&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make a star key lime pie</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.BUBBLIN_STONE ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+1+aerated+diving+helmet&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make an aerated diving helmet</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.CITADEL_SATCHEL ) )
		{
			buffer.append( "<nobr><a href=\"guild.php?place=paco\">complete white citadel quest by turning in White Citadel Satisfaction Satchel</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.THICK_PADDED_ENVELOPE ) )
		{
			buffer.append( "<nobr><a href=\"guild.php?place=paco\">complete dwarvish delivery quest by turning in thick padded envelope</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.DWARVISH_PUNCHCARD )   )
		{
			buffer.append( "<nobr><a href=\"dwarfcontraption.php\">acquire dwarvish war outfit piece</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.GUNPOWDER ) )
		{
			buffer.append( "<nobr><a href=\"postwarisland.php?place=lighthouse&action=pyro\">trade in barrels of gunpowder for big boom</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.RAT_WHISKER ) )
		{
			buffer.append( "<nobr><a href=\"town_wrong.php?place=artist&action=whisker\">trade in rat whiskers for meat</a></nobr><br>" );
		}

		if ( Preferences.getInteger( "lastEasterEggBalloon" ) != KoLCharacter.getAscensions() )
		{			
			buffer.append( "<nobr><a href=\"lair2.php?preaction=key&whichkey=" );
			buffer.append( ItemPool.BALLOON_MONKEY );
			buffer.append( "\">get an easter egg balloon</a></nobr><br>" );
		}

		GenericRequest trophyCheck = new GenericRequest( "trophy.php" );
		trophyCheck.run();
		if ( trophyCheck.responseText.indexOf( "You're not currently entitled to any trophies" ) == -1 )
		{			
			buffer.append( "<nobr><a href=\"trophy.php\">buy trophies you're eligible for</a></nobr><br>" );
		}
		
		ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_EMO_ROE, "Veracity" );
		ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_WWTNSD_BRACELET, "Veracity" );
		ValhallaDecorator.developerGift( buffer, ItemPool.STUFFED_COCOABO, "holatuwol" );
	}

	private static final void developerGift( final StringBuffer buffer, final int itemId, final String developer )
	{
		int giftCount = InventoryManager.getAccessibleCount( itemId );
		if ( giftCount <= 0 )
		{
			return;
		}

		String itemName = StringUtilities.getURLEncode( ItemDatabase.getItemName( itemId ) );
		String plural = ItemDatabase.getPluralById( itemId );

		buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=acquire+" );
		buffer.append( giftCount );
		buffer.append( "+" );
		buffer.append( itemName );
		buffer.append( ";csend+" );
		buffer.append( giftCount );
		buffer.append( "+" );
		buffer.append( itemName );
		buffer.append( "+to+" );
		buffer.append( developer );
		buffer.append( "&pwd=" );
		buffer.append( GenericRequest.passwordHash );
		buffer.append( "\">send your " );
		buffer.append( plural );
		buffer.append( " to " );
		buffer.append( developer );
		buffer.append( "</a></nobr><br>" );
	}
}
