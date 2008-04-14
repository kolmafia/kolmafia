/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
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

		int startPoint = 0;

		if ( KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			startPoint = 1000;
		}
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.TURTLE_TAMER ) )
		{
			startPoint = 2000;
		}
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER ) )
		{
			startPoint = 3000;
		}
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
		{
			startPoint = 4000;
		}
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT ) )
		{
			startPoint = 5000;
		}
		else if ( KoLCharacter.getClassType().equals( KoLCharacter.ACCORDION_THIEF ) )
		{
			startPoint = 6000;
		}

		StringBuffer reminders = new StringBuffer();
		reminders.append( "<br><table>" );

		reminders.append( "<tr><td><img id = 'current' src=\"http://images.kingdomofloathing.com/" );
		reminders.append( FamiliarDatabase.getFamiliarImageLocation( KoLCharacter.getFamiliar().getId() ) );
		reminders.append( "\"></td><td><select id=\"familiar\" style=\"width: 250px\" onchange=\"var select = document.getElementById('familiar'); " );
		reminders.append( "var option = select.options[select.selectedIndex]; " );
		reminders.append( "top.charpane.document.location.href = '/KoLmafia/sideCommand?cmd=familiar+' + option.value + '&pwd=" );
		reminders.append( GenericRequest.passwordHash );
		reminders.append( "'; document.getElementById('current').src = 'http://images.kingdomofloathing.com/' + option.id; " );
		reminders.append( "return true;\"><option value=\"none\">- No Familiar -</option>" );

		Object[] familiars = KoLCharacter.getFamiliarList().toArray();

		for ( int i = 1; i < familiars.length; ++i )
		{
			reminders.append( "<option id=\"" );
			reminders.append( FamiliarDatabase.getFamiliarImageLocation( ( (FamiliarData) familiars[ i ] ).getId() ) );
			reminders.append( "\" value=\"" );
			reminders.append( StringUtilities.globalStringReplace( ( (FamiliarData) familiars[ i ] ).getRace(), " ", "+" ) );
			reminders.append( "\"" );

			if ( familiars[ i ].equals( KoLCharacter.getFamiliar() ) )
			{
				reminders.append( " selected" );
			}

			reminders.append( ">" );
			reminders.append( ( (FamiliarData) familiars[ i ] ).getRace() );
			reminders.append( " (" );
			reminders.append( ( (FamiliarData) familiars[ i ] ).getWeight() );
			reminders.append( " lbs.)" );
			reminders.append( "</option>" );
		}

		reminders.append( "</select></td><td><input type=submit class=button value=\"Ascend\"><input type=hidden name=confirm value=on><input type=hidden name=confirm2 value=on></td></tr>" );
		reminders.append( "</table>" );

		reminders.append( "<br><table cellspacing=10 cellpadding=10><tr>" );
		reminders.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Didn't Buy</th></tr><tr><td align=center><font size=\"-1\">" );

		ArrayList skillList = new ArrayList();
		for ( int i = 0; i < KoLConstants.availableSkills.size(); ++i )
		{
			skillList.add( String.valueOf( ( (UseSkillRequest) KoLConstants.availableSkills.get( i ) ).getSkillId() ) );
		}

		ValhallaDecorator.listPermanentSkills( reminders, skillList, startPoint );
		reminders.append( "</font></td></tr></table></td>" );
		reminders.append( "<td bgcolor=\"#eeeeff\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Common Stuff You Didn't Do</th></tr><tr><td align=center><font size=\"-1\">" );

		if ( KoLCharacter.hasChef() )
		{
			reminders.append( "<nobr>blow up your chef</nobr><br>" );
		}

		if ( KoLCharacter.hasBartender() )
		{
			reminders.append( "<nobr>blow up your bartender</nobr><br>" );
		}

		if ( KoLCharacter.getZapper() != null )
		{
			reminders.append( "<nobr>blow up your zap wand</nobr><br>" );
		}
		
		if ( InventoryManager.hasItem( ItemPool.RUBBER_EMO_ROE ) )
		{
			reminders.append( "<nobr>send your rubber emo roes to Veracity</nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.STUFFED_COCOABO ) )
		{
			reminders.append( "<nobr>send your stuffed cocoabos to holatuwol</nobr><br>" );
		}

		reminders.append( "</font></td></tr></table></td></tr></table>" );

		reminders.append( "<br><br>" );
		StringUtilities.singleStringReplace(
			buffer,
			"<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)",
			reminders.toString() );

		return;
	}

	public static final void decorateAfterLife( final String location, final StringBuffer buffer )
	{
		if ( buffer.indexOf( "<form" ) == -1 )
		{
			return;
		}

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
		buffer.append( "<option value=1>The Mongoose</option><option value=2>The Wallaby</option><option value=3>The Vole</option>" );
		buffer.append( "<option value=4>The Platypus</option><option value=5>The Opossum</option><option value=6>The Marmot</option>" );
		buffer.append( "<option value=7>The Wombat</option><option value=8>The Blender</option><option value=9>The Packrat</option>" );
		buffer.append( "<option value=10>Bad Moon</option>" );
		buffer.append( "</select></td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Restrictions:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=\"whichpath\"><option value=0 selected>No dietary restrictions</option><option value=1>Boozetafarian</option><option value=2>Teetotaler</option><option value=3>Oxygenarian</option></select>" );
		buffer.append( "</td></tr>" );
		buffer.append( KoLConstants.LINE_BREAK );

		buffer.append( "<tr><td align=right><b>Skill to Keep:</b>&nbsp;</td><td>" );
		buffer.append( "<select style=\"width: 250px\" name=keepskill><option value=9999 selected></option><option value=0 selected>(no skill)</option>" );

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

	private static final void listPermanentSkills( final StringBuffer buffer, final ArrayList skillList,
		final int startingPoint )
	{
		String skillName;
		for ( int i = 0; i < 100; ++i )
		{
			skillName = SkillDatabase.getSkillName( startingPoint + i );
			if ( skillName == null )
			{
				continue;
			}

			buffer.append( "<nobr>" );
			boolean alreadyPermed = skillList.contains( String.valueOf( startingPoint + i ) );
			if ( alreadyPermed )
			{
				buffer.append( "<font color=darkgray><s>" );
			}

			buffer.append( "<a onClick=\"skill('" );
			buffer.append( startingPoint + i );
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
}