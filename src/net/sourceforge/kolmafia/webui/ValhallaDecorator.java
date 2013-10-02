/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
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

		ArrayList<String> skillList = new ArrayList<String>();
		ArrayList<UseSkillRequest> unpermedSkills = new ArrayList<UseSkillRequest>();
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
			buffer.append( "<nobr><a href=\"javascript:if(confirm('Are you sure you want to discard your Instant Karma?')) {singleUse('inventory.php?which=1&action=discard&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "&whichitem=");
			buffer.append( ItemPool.INSTANT_KARMA );
			buffer.append( "&ajax=1');discardKarma();}void(0);\">discard karma</a> (have <span id='haveKarma'>" );
			buffer.append( count );
			buffer.append( "</span>, banked <span id='bankedKarma'>" );
			buffer.append( banked );
			buffer.append( "</span>)</nobr><br>" );
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
			buffer.append( KoLCharacter.getZapper().getItemId() );
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

		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.BORIS_KEY, "Boris&#39;s" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.JARLSBERG_KEY, "Jarlsberg&#39;s" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.SNEAKY_PETE_KEY, "Sneaky Pete&#39;" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.DIGITAL_KEY, "digital" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.STAR_KEY, "star" );

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

		if ( InventoryManager.hasItem( ItemPool.RAT_WHISKER )
			&& Preferences.getString( Quest.ARTIST.getPref() ).equals( QuestDatabase.FINISHED ) )
		{
			buffer.append( "<nobr><a href=\"place.php?whichplace=town_wrong&action=townwrong_artist_quest&subaction=whiskers\">" );
			buffer.append( "trade in rat whiskers for meat</a></nobr><br>" );
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
		int ip = Preferences.getInteger("lastGoofballBuy");
		if (KoLCharacter.getAscensions() > ip) {
			buffer.append( "<nobr><a href=\"tavern.php?place=susguy\">get free goofballs?</a></nobr><br>" );
		}

		if ( KoLCharacter.getAttacksLeft() > 0 )
		{
			buffer.append( "<nobr><a href=\"peevpee.php?place=fight\">Use remaining PVP fights</a></nobr><br>" );
		}

		ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_EMO_ROE, "Veracity" );
		ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_WWTNSD_BRACELET, "Veracity" );
		ValhallaDecorator.developerGift( buffer, ItemPool.STUFFED_COCOABO, "holatuwol" );

		ValhallaDecorator.switchSeeds( buffer );

		ValhallaDecorator.switchCorrespondent( buffer );
	}

	private static void checkForKeyLime( StringBuffer buffer, int itemId, String keyType )
	{
		if ( !InventoryManager.hasItem( itemId ) )
		{
			return;
		}

		buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
		buffer.append( InventoryManager.getAccessibleCount( itemId ) );
		buffer.append( "+" );
		buffer.append( StringUtilities.getURLEncode( keyType ) );
		buffer.append( "+key+lime&pwd=" );
		buffer.append( GenericRequest.passwordHash );
		buffer.append( "\">make a ");
		buffer.append( keyType );
		buffer.append( " key lime</a></nobr><br />" );
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

	private static final void switchSeeds( final StringBuffer buffer )
	{
		boolean havePumpkin = InventoryManager.hasItem( ItemPool.PUMPKIN_SEEDS );
		boolean havePeppermint = InventoryManager.hasItem( ItemPool.PEPPERMINT_PACKET );
		boolean haveSkeleton = InventoryManager.hasItem( ItemPool.DRAGON_TEETH );
		boolean haveBeer = InventoryManager.hasItem( ItemPool.BEER_SEEDS );
		if ( !havePumpkin && !havePeppermint && !haveSkeleton && !haveBeer )
		{
			return;
		}
		boolean needSeparator = false;

		buffer.append( "Garden: plant " );
		if ( havePumpkin )
		{
			if ( needSeparator )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+pumpkin+seeds;" );
			buffer.append( "+use+packet+of+pumpkin+seeds&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">pumpkin</a>" );
			needSeparator = true;
		}
		if ( havePeppermint )
		{
			if ( needSeparator )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=acquire+Peppermint+Pip+Packet;" );
			buffer.append( "+use+Peppermint+Pip+Packet&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">peppermint</a>" );
			needSeparator = true;
		}
		if ( haveSkeleton )
		{
			if ( needSeparator )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+dragon's+teeth;" );
			buffer.append( "+use+packet+of+dragon's+teeth&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">skeleton</a>" );
			needSeparator = true;
		}
		if ( haveBeer )
		{
			if ( needSeparator )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+beer+seeds;" );
			buffer.append( "+use+packet+of+beer+seeds&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">beer</a>" );
			needSeparator = true;
		}
		AdventureResult crop = CampgroundRequest.getCrop();
		if ( crop != null )
		{
			String cropName = crop.getName();
			String cropString = 
				  ( cropName.contains( "peppermint" ) || cropName.contains( "candy cane" ) ) ? "Peppermint"
				: ( cropName.contains( "pumpkin" ) ) ? "Pumpkin"
				: ( cropName.contains( "skeleton" ) ) ? "Skeleton"
				: ( cropName.contains( "barley" ) || cropName.contains( "beer label" ) ) ? "Beer Garden"
				: "Unknown";
			buffer.append( " (currently " ).append( cropString ).append( ")" );
		}
		buffer.append( "<br>" );
	}

	private static final Pattern EUDORA_PATTERN = Pattern.compile( "<option (selected='selected' )?value=\"(\\d)\">([\\w\\s]*)" );
	private static final void switchCorrespondent( final StringBuffer buffer )
	{
		GenericRequest eudoraCheck = new GenericRequest( "account.php?tab=correspondence" );
		eudoraCheck.run();
		String response = eudoraCheck.responseText;
		if ( !response.contains( "Eudora" ) )
		{
			// We do not have at least two options, so the tab does not exist
			// and the default tab loaded instead
			return;
		}

		// have[Eudora] means that it can be switched to, which means
		// it is not currently active
		boolean havePenpal = false;
		boolean haveGamemag = false;
		String activeEudora = "";
		Matcher matcher = ValhallaDecorator.EUDORA_PATTERN.matcher( response );

		while ( matcher.find() )
		{
			if ( matcher.group(3).equals( "Pen Pal" ) )
			{
				if ( matcher.group(1) == null )
				{
					havePenpal = true;
				}
				else
				{
					activeEudora = "Pen Pal";
				}
			}
			else if ( matcher.group(3).equals( "GameInformPowerDailyPro Magazine" ) )
			{
				if ( matcher.group(1) == null )
				{
					haveGamemag = true;
				}
				else
				{
					activeEudora = "Game Magazine";
				}
			}
		}

		buffer.append( "Eudora: use " );
		boolean multiple = false;
		if ( havePenpal )
		{
			if ( multiple )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=eudora+penpal&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">Pen Pal</a>" );
			multiple = true;
		}
		if ( haveGamemag )
		{
			if ( multiple )
			{
				buffer.append( " or " );
			}
			buffer.append( "<a href=\"/KoLmafia/redirectedCommand?cmd=eudora+game&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">Game Magazine</a>" );
			multiple = true;
		}

		buffer.append( " (Currently " ).append( activeEudora ).append( ")" );
		buffer.append( "<br>" );
	}
}
