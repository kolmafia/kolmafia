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

package net.sourceforge.kolmafia;

import edu.stanford.ejalbert.BrowserLauncher;

import java.awt.Frame;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.ArtistRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.ChefStaffRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.KnollRequest;
import net.sourceforge.kolmafia.request.LeafletRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.MushroomRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.PyroRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.StarChartRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.SuspiciousGuyRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.MineDecorator;

public abstract class StaticEntity
{
	private static final Pattern NEWSKILL1_PATTERN = Pattern.compile( "<td>You (have learned|learn) a new skill: <b>(.*?)</b>" );
	private static final Pattern NEWSKILL2_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern NEWSKILL3_PATTERN = Pattern.compile( "You (?:gain|acquire) a skill: +<[bB]>(.*?)</[bB]>" );
	private static final Pattern RECIPE_PATTERN = Pattern.compile( "You learn to craft a new item: <b>(.*?)</b>" );
	private static final Pattern MR_A_PATTERN = Pattern.compile( "You have (\\w+) Mr. Accessor(y|ies) to trade." );
	private static final Pattern SLIMESKILL_PATTERN = Pattern.compile( "giving you \\+(\\d+)" );

	private static KoLmafia client;
	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;

	public static final ArrayList existingPanels = new ArrayList();
	private static ActionPanel[] panelArray = new GenericPanel[ 0 ];
	
	public static String backtraceTrigger = null;

	public static final String getVersion()
	{
		if ( KoLConstants.REVISION == null )
		{
			return KoLConstants.VERSION_NAME;
		}

		int colonIndex = KoLConstants.REVISION.indexOf( ":" );
		if ( colonIndex != -1 )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, colonIndex );
		}

		if ( KoLConstants.REVISION.endsWith( "M" ) )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, KoLConstants.REVISION.length() - 1 );
		}

		return "KoLmafia r" + KoLConstants.REVISION;
	}

	public static final void setClient( final KoLmafia client )
	{
		StaticEntity.client = client;
	}

	public static final KoLmafia getClient()
	{
		return StaticEntity.client;
	}

	public static final void registerPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.add( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.remove( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final boolean isHeadless()
	{
		return StaticEntity.client instanceof KoLmafiaCLI;
	}

	public static final ActionPanel[] getExistingPanels()
	{
		synchronized ( StaticEntity.existingPanels )
		{
			boolean needsRefresh = StaticEntity.panelArray.length != StaticEntity.existingPanels.size();

			if ( !needsRefresh )
			{
				for ( int i = 0; i < StaticEntity.panelArray.length && !needsRefresh; ++i )
				{
					needsRefresh = StaticEntity.panelArray[ i ] != StaticEntity.existingPanels.get( i );
				}
			}

			if ( needsRefresh )
			{
				StaticEntity.panelArray = new ActionPanel[ StaticEntity.existingPanels.size() ];
				StaticEntity.existingPanels.toArray( StaticEntity.panelArray );
			}

			return StaticEntity.panelArray;
		}
	}

	public static final boolean usesSystemTray()
	{
		if ( StaticEntity.usesSystemTray == 0 )
		{
			StaticEntity.usesSystemTray =
				System.getProperty( "os.name" ).startsWith( "Windows" ) && Preferences.getBoolean( "useSystemTrayIcon" ) ? 1 : 2;
		}

		return StaticEntity.usesSystemTray == 1;
	}

	public static final boolean usesRelayWindows()
	{
		if ( StaticEntity.usesRelayWindows == 0 )
		{
			StaticEntity.usesRelayWindows = Preferences.getBoolean( "useRelayWindows" ) ? 1 : 2;
		}

		return StaticEntity.usesRelayWindows == 1;
	}

	public static final void openSystemBrowser( final String location )
	{
		( new SystemBrowserThread( location ) ).start();
	}

	private static String currentBrowser = null;

	private static class SystemBrowserThread
		extends Thread
	{
		private final String location;

		public SystemBrowserThread( final String location )
		{
			super( "SystemBrowserThread@" + location );
			this.location = location;
		}

		public void run()
		{
			String preferredBrowser = Preferences.getString( "preferredWebBrowser" );

			if ( currentBrowser == null || !currentBrowser.equals( preferredBrowser ) )
			{
				System.setProperty( "os.browser", preferredBrowser );
				currentBrowser = preferredBrowser;
			}

			BrowserLauncher.openURL( this.location );
		}
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays the given location, relative to the KoL
	 * home directory for the current session. This should be called whenever <code>RequestFrame</code>s need to be
	 * created in order to keep code modular.
	 */

	public static final void openRequestFrame( final String location )
	{
		GenericRequest request = RequestEditorKit.extractRequest( location );

		if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
		{
			DescriptionFrame.showRequest( request );
			return;
		}

		Frame[] frames = Frame.getFrames();
		RequestFrame requestHolder = null;

		for ( int i = frames.length - 1; i >= 0; --i )
		{
			if ( frames[ i ].getClass() == RequestFrame.class && ( (RequestFrame) frames[ i ] ).hasSideBar() )
			{
				requestHolder = (RequestFrame) frames[ i ];
			}
		}

		if ( requestHolder == null )
		{
			RequestSynchFrame.showRequest( request );
			return;
		}

		if ( !location.equals( "main.php" ) )
		{
			requestHolder.refresh( request );
		}
	}

	public static final void externalUpdate( final String location, final String responseText )
	{
		if ( location.startsWith( "account.php" ) )
		{
			if ( location.indexOf( "&ajax" ) != -1 )
			{
				AccountRequest.parseAjax( location );
			}
			else
			{
				boolean wasHardcore = KoLCharacter.isHardcore();
				boolean hadRestrictions = !KoLCharacter.canEat() || !KoLCharacter.canDrink();
	
				AccountRequest.parseAccountData( responseText );
	
				if ( wasHardcore && !KoLCharacter.isHardcore() )
				{
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "dropped hardcore" );
					RequestLogger.updateSessionLog();
				}
	
				if ( hadRestrictions && KoLCharacter.canEat() && KoLCharacter.canDrink() )
				{
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "dropped consumption restrictions" );
					RequestLogger.updateSessionLog();
				}
			}
		}

		else if ( location.startsWith( "account_contactlist.php" ) )
		{
			ContactListRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "arena.php" ) )
		{
			CakeArenaRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( responseText );
		}

		else if ( location.startsWith( "bet.php" ) )
		{
			MoneyMakingGameRequest.parseResponse( location, responseText, false );
		}

		else if ( location.startsWith( "campground.php" ) )
		{
			CampgroundRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "charsheet.php" ) &&
			location.indexOf( "ajax=1" ) == -1 )
		{
			CharSheetRequest.parseStatus( responseText );
		}

		else if ( location.startsWith( "clan_stash.php" ) )
		{
			ClanStashRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "clan_viplounge.php" ) )
		{
			ClanLoungeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "closet.php" ) )
		{
			ClosetRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "craft.php" ) )
		{
			CreateItemRequest.parseCrafting( location, responseText );
		}

		else if ( location.startsWith( "familiar.php" ) && location.indexOf( "ajax=1" ) == -1)
		{
			FamiliarData.registerFamiliarData( responseText );
		}

		else if ( location.startsWith( "familiarbinger.php" ))
		{
			UseItemRequest.parseBinge( location, responseText );
		}

		else if ( location.startsWith( "galaktik.php" ) )
		{
			GalaktikRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "guild.php" ) )
		{
			GuildRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "hermit.php" ) )
		{
			HermitRequest.parseHermitTrade( location, responseText );
		}

		else if ( location.startsWith( "hiddencity.php" ) )
		{
			HiddenCityRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "desc_skill.php" ) &&
			location.indexOf( "self=true" ) != -1 )
		{
			Matcher m = NEWSKILL2_PATTERN.matcher( location );
			if ( m.find() )
			{
				int skill = StringUtilities.parseInt( m.group( 1 ) );
				if ( skill >= 46 && skill <= 48 )
				{
					m = SLIMESKILL_PATTERN.matcher( responseText );
					if ( m.find() )
					{
						Preferences.setInteger( "skillLevel" + skill,
							StringUtilities.parseInt( m.group( 1 ) ) );
					}
				}
			}
		}

		else if ( location.startsWith( "dwarfcontraption.php" ) )
		{
			DwarfContraptionRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "dwarffactory.php" ) )
		{
			DwarfFactoryRequest.parseResponse( location, responseText );
		}

		// Keep your current equipment and familiars updated, if you
		// visit the appropriate pages.

		else if ( location.startsWith( "inventory.php" ) )
		{
			// If KoL is showing us our current equipment, parse it.
			if  ( location.indexOf( "which=2" ) != -1 || location.indexOf( "curequip=1" ) != -1 )
			{
				EquipmentRequest.parseEquipment( location, responseText );
			}

			// If there is a consumption message, parse it
			if ( location.indexOf( "action=message" ) != -1 )
			{
				UseItemRequest.parseConsumption( responseText, false );
			}

			// If there is a binge message, parse it
			if ( location.indexOf( "action=ghost" ) != -1 ||
			     location.indexOf( "action=hobo" ) != -1)
			{
				UseItemRequest.parseBinge( location, responseText );
			}
		}

		else if ( location.startsWith( "inv_equip.php" ) &&
			  location.indexOf( "ajax=1" ) != -1 )
		{
			// If we are changing equipment via a chat command,
			// try to deduce what changed.
			EquipmentRequest.parseEquipmentChange( location, responseText );
		}

		else if ( ( location.startsWith( "inv_eat.php" ) ||
			    location.startsWith( "inv_booze.php" ) ||
			    location.startsWith( "inv_use.php" ) ||
			    location.startsWith( "inv_familiar.php" ) ) &&
			  location.indexOf( "whichitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "knoll.php" ) )
		{
			KnollRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "knoll_mushrooms.php" ) )
		{
			MushroomRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "leaflet.php" ) )
		{
			LeafletRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "managecollection.php" ) )
		{
			DisplayCaseRequest.parseDisplayTransfer( location, responseText );
		}
		else if ( location.startsWith( "managecollectionshelves.php" ) )
		{
			DisplayCaseRequest.parseDisplayArrangement( location, responseText );
		}

		else if ( location.startsWith( "managestore.php" ) )
		{
			SellStuffRequest.parseMallSell( location, responseText );
		}

		else if ( location.startsWith( "manor3" ) )
		{
			WineCellarRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mining.php" ) )
		{
			MineDecorator.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mrstore.php" ) )
		{
			Matcher m = MR_A_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				String num = m.group( 1 );
				int delta = (num.equals( "one" ) ? 1 : StringUtilities.parseInt( num )) -
					InventoryManager.getCount( ItemPool.MR_ACCESSORY );
				if ( delta != 0 )
				{
					ResultProcessor.processItem( ItemPool.MR_ACCESSORY, delta );
				}
			}
		}

		else if ( ( location.startsWith( "multiuse.php" ) ||
			    location.startsWith( "skills.php" ) ) &&
			  location.indexOf( "useitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "pvp.php" ) &&
			  location.indexOf( "action" ) != -1 )
		{
			PvpManager.processOffenseContests( responseText );
		}

		else if ( location.startsWith( "pyramid.php" ) )
		{
			PyramidRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "questlog.php" ) )
		{
			QuestLogRequest.registerQuests( true, location, responseText );
		}

		else if ( location.startsWith( "sellstuff.php" ) )
		{
			SellStuffRequest.parseCompactAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sellstuff_ugly.php" ) )
		{
			SellStuffRequest.parseDetailedAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sendmessage.php" ) )
		{
			SendMailRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "skills.php" ) )
		{
			if ( location.indexOf( "action=useditem" ) != -1 )
			{
				UseItemRequest.parseConsumption( responseText, false );
			}
			else
			{
				UseSkillRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "starchart.php" ) )
		{
			StarChartRequest.parseCreation( location, responseText );
		}

		else if ( location.startsWith( "storage.php" ) )
		{
			StorageRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "store.php" ) )
		{
			MallPurchaseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sushi.php" ) )
		{
			SushiRequest.parseConsumption( location, responseText );
		}

		else if ( location.startsWith( "town_right.php" ) )
		{
			GourdRequest.parseResponse( location, responseText );
			UntinkerRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_sendgift.php" ) )
		{
			SendGiftRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "town_wrong.php" ) )
		{
			ArtistRequest.parseResponse( location, responseText );
			SuspiciousGuyRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "wand.php" ) )
		{
			ZapRequest.parseResponse( location, responseText );
		}

		else if ( location.indexOf( "action=pyro" ) != -1 )
		{
			PyroRequest.parseResponse( location, responseText );
		}

		// You can learn a skill on many pages.
		StaticEntity.learnSkill( location, responseText );
		
		// Currently, required recipes can only be learned via using an item, but
		// that's probably not guaranteed to be true forever.
		StaticEntity.learnRecipe( responseText );
	}
	
	public static void learnRecipe( String responseText )
	{
		Matcher matcher = StaticEntity.RECIPE_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}
		int id = ItemDatabase.getItemId( matcher.group( 1 ), 1, false );
		if ( id > 0 )
		{
			Preferences.setBoolean( "unknownRecipe" + id, false );
			RequestLogger.printLine( "Learned recipe: " + matcher.group( 1 ) );
			RequestLogger.updateSessionLog( "learned recipe: " + matcher.group( 1 ) );
			ConcoctionDatabase.refreshConcoctions();
		}
	}

	public static void learnSkill( final String location, final String responseText )
	{
		// Don't parse skill acquisition via item use here, since
		// UseItemRequest will detect it.

		if ( location.startsWith( "inv_use.php" ) )
		{
			return;
		}

		Matcher matcher = StaticEntity.NEWSKILL1_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			StaticEntity.learnSkill( matcher.group( 2 ) );
			return;
		}

		matcher = StaticEntity.NEWSKILL3_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			StaticEntity.learnSkill( matcher.group( 1 ) );
			return;
		}

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.indexOf( "You leargn a new skill." ) != -1 )
		{
			matcher = StaticEntity.NEWSKILL2_PATTERN.matcher( location );
			if ( matcher.find() )
			{
				int skillId = StringUtilities.parseInt( matcher.group( 1 ) );
				String skillName = SkillDatabase.getSkillName( skillId );
				StaticEntity.learnSkill( skillName );
				return;
			}
		}
	}

	public static final void learnSkill( final String skillName )
	{
		// The following skills are found in battle and result in
		// losing an item from inventory.

		if ( skillName.equals( "Snarl of the Timberwolf" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_WOLF_STANDARD ) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_WOLF_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Spectral Snapper" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_SNAKE_STANDARD) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_SNAKE_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Scarysauce" ) || skillName.equals( "Fearful Fettucini" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY) )
			{
				ResultProcessor.processItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY, -1 );
			}
		}
		else if ( skillName.equals( "Tango of Terror" ) || skillName.equals( "Dirge of Dreadfulness" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC ) )
			{
				ResultProcessor.processItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC, -1 );
			}
		}			
 
		String message = "You learned a new skill: " + skillName;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
		KoLCharacter.addAvailableSkill( skillName );
		KoLCharacter.updateStatus();
		KoLCharacter.addDerivedSkills();
		KoLConstants.usableSkills.sort();
		ConcoctionDatabase.refreshConcoctions();
		if ( SkillDatabase.isBookshelfSkill( skillName ) )
		{
			KoLCharacter.setBookshelf( true );
		}
		Preferences.firePreferenceChanged( "(skill)" );
	}

	public static final boolean executeCountdown( final String message, final int seconds )
	{
		PauseObject pauser = new PauseObject();

		StringBuffer actualMessage = new StringBuffer( message );

		for ( int i = seconds; i > 0 && KoLmafia.permitsContinue(); --i )
		{
			boolean shouldDisplay = false;

			// If it's the first count, then it should definitely be shown
			// for the countdown.

			if ( i == seconds )
			{
				shouldDisplay = true;
			}
			else if ( i >= 1800 )
			{
				shouldDisplay = i % 600 == 0;
			}
			else if ( i >= 600 )
			{
				shouldDisplay = i % 300 == 0;
			}
			else if ( i >= 300 )
			{
				shouldDisplay = i % 120 == 0;
			}
			else if ( i >= 60 )
			{
				shouldDisplay = i % 60 == 0;
			}
			else if ( i >= 15 )
			{
				shouldDisplay = i % 15 == 0;
			}
			else if ( i >= 5 )
			{
				shouldDisplay = i % 5 == 0;
			}
			else
			{
				shouldDisplay = true;
			}

			// Only display the message if it should be displayed based on
			// the above checks.

			if ( shouldDisplay )
			{
				actualMessage.setLength( message.length() );

				if ( i >= 60 )
				{
					int minutes = i / 60;
					actualMessage.append( minutes );
					actualMessage.append( minutes == 1 ? " minute" : " minutes" );

					if ( i % 60 != 0 )
					{
						actualMessage.append( ", " );
					}
				}

				if ( i % 60 != 0 )
				{
					actualMessage.append( i % 60 );
					actualMessage.append( i % 60 == 1 ? " second" : " seconds" );
				}

				actualMessage.append( "..." );
				KoLmafia.updateDisplay( actualMessage.toString() );
			}

			pauser.pause( 1000 );
		}

		return KoLmafia.permitsContinue();
	}

	public static final void printStackTrace()
	{
		StaticEntity.printStackTrace( "Forced stack trace" );
	}

	public static final void printStackTrace( final String message )
	{
		try
		{
			throw new Exception( message );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, message );
		}
	}

	public static final void printStackTrace( final Throwable t )
	{
		StaticEntity.printStackTrace( t, "" );
	}

	public static final void printStackTrace( final Throwable t, final String message )
	{
		printStackTrace( t, message, false );
	}

	public static final void printStackTrace( final Throwable t, final String message, boolean printOnlyCause )
	{
		// Next, print all the information to the debug log so that
		// it can be sent.

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		if ( message.startsWith( "Backtrace" ) )
		{
			StaticEntity.backtraceTrigger = null;
			KoLmafia.updateDisplay( "Backtrace triggered, debug log printed." );
		}
		else
		{
			KoLmafia.updateDisplay( "Unexpected error, debug log printed." );
		}

		Throwable cause = t.getCause();

		if ( cause == null || !printOnlyCause )
		{
			StaticEntity.printStackTrace( t, message, System.err );
			StaticEntity.printStackTrace( t, message, RequestLogger.getDebugStream() );
		}

		if ( cause != null )
		{
			StaticEntity.printStackTrace( cause, message, System.err );
			StaticEntity.printStackTrace( cause, message, RequestLogger.getDebugStream() );
		}

		try
		{
			if ( shouldOpenStream )
			{
				RequestLogger.closeDebugLog();
			}
		}
		catch ( Exception e )
		{
			// Okay, since you're in the middle of handling an exception
			// and got a new one, just return from here.
		}
	}

	private static final void printStackTrace( final Throwable t, final String message, final PrintStream ostream )
	{
		ostream.println( t.getClass() + ": " + t.getMessage() );
		t.printStackTrace( ostream );
	}

	public static final void printRequestData( final GenericRequest request )
	{
		if ( request == null )
		{
			return;
		}

		boolean shouldOpenStream = RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "" + request.getClass() + ": " + request.getURLString() );
		RequestLogger.updateDebugLog( KoLConstants.LINE_BREAK_PATTERN.matcher( request.responseText ).replaceAll( "" ) );
		RequestLogger.updateDebugLog();

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final String[] getPastUserList()
	{
		ArrayList pastUserList = new ArrayList();

		String user;
		File[] files = DataUtilities.listFiles( UtilityConstants.SETTINGS_LOCATION );

		for ( int i = 0; i < files.length; ++i )
		{
			user = files[ i ].getName();
			if ( user.startsWith( "GLOBAL" ) || !user.endsWith( "_prefs.txt" ) )
			{
				continue;
			}

			user = user.substring( 0, user.length() - 10 );
			if ( !user.equals( "GLOBAL" ) && !pastUserList.contains( user ) )
			{
				pastUserList.add( user );
			}
		}

		String[] pastUsers = new String[ pastUserList.size() ];
		pastUserList.toArray( pastUsers );
		return pastUsers;
	}

	public static final void disable( final String name )
	{
		String functionName;
		StringTokenizer tokens = new StringTokenizer( name, ", " );

		while ( tokens.hasMoreTokens() )
		{
			functionName = tokens.nextToken();
			if ( !KoLConstants.disabledScripts.contains( functionName ) )
			{
				KoLConstants.disabledScripts.add( functionName );
			}
		}
	}

	public static final void enable( final String name )
	{
		if ( name.equals( "all" ) )
		{
			KoLConstants.disabledScripts.clear();
			return;
		}

		StringTokenizer tokens = new StringTokenizer( name, ", " );
		while ( tokens.hasMoreTokens() )
		{
			KoLConstants.disabledScripts.remove( tokens.nextToken() );
		}
	}

	public static final boolean isDisabled( final String name )
	{
		if ( name.equals( "enable" ) || name.equals( "disable" ) )
		{
			return false;
		}

		return KoLConstants.disabledScripts.contains( "all" ) || KoLConstants.disabledScripts.contains( name );
	}
}
