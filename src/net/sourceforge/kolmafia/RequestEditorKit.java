/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import net.sourceforge.kolmafia.chat.ChatPoller;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HedgePuzzleRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.MoonPhaseRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.NemesisManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.VolcanoMazeManager;

import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.BasementDecorator;
import net.sourceforge.kolmafia.webui.BeerPongDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.DvorakDecorator;
import net.sourceforge.kolmafia.webui.FightDecorator;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;
import net.sourceforge.kolmafia.webui.MineDecorator;
import net.sourceforge.kolmafia.webui.MoneyMakingGameDecorator;
import net.sourceforge.kolmafia.webui.NemesisDecorator;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;
import net.sourceforge.kolmafia.webui.UseItemDecorator;
import net.sourceforge.kolmafia.webui.UseLinkDecorator;
import net.sourceforge.kolmafia.webui.ValhallaDecorator;

public class RequestEditorKit
	extends HTMLEditorKit
{
	private static final Pattern FORM_PATTERN = Pattern.compile( "<form name=choiceform(\\d+)" );
	//private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice\"? value=\"?(\\d+)\"?" );
	private static final Pattern CHOICE2_PATTERN = Pattern.compile( "whichchoice=(\\d+)" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "name=option value=(\\d+)" );
	private static final Pattern OUTFIT_FORM_PATTERN = Pattern.compile( "<form name=outfit.*?</form>", Pattern.DOTALL );
	private static final Pattern OPTGROUP_PATTERN = Pattern.compile( "<optgroup label=['\"]([^']*)['\"]>(.*?)</optgroup>", Pattern.DOTALL );
	private static final Pattern NOLABEL_CUSTOM_OUTFITS_PATTERN = Pattern.compile( "\\(select an outfit\\)</option>(<option.*?)<optgroup", Pattern.DOTALL );
	private static final Pattern BOUNTY_ITEM_PATTERN = Pattern.compile( "acquire a bounty item: <b>(.*?)</b></td></tr></table>\\((\\d+) of (\\d+)" );

	private static final Pattern ALTAR_PATTERN = Pattern.compile( "'An altar with a carving of a god of ([^']*)'" );
	private static final Pattern ROUND_SEP_PATTERN = Pattern.compile( "<(?:b>Combat!</b>|hr.*?>)" );

	private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

	/**
	 * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts some of the form handling to
	 * ensure that <code>GenericRequest</code> objects are instantiated on form submission rather than the
	 * <code>HttpRequest</code> objects created by the default HTML editor kit.
	 */

	@Override
	public ViewFactory getViewFactory()
	{
		return RequestEditorKit.DEFAULT_FACTORY;
	}

	/**
	 * Registers thethat is supposed to be used for handling data submission to the Kingdom of Loathing server.
	 */

	private static class RequestViewFactory
		extends HTMLFactory
	{
		@Override
		public View create( final Element elem )
		{
			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.INPUT )
			{
				return new KoLSubmitView( elem );
			}

			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.IMG )
			{
				return new KoLImageView( elem );
			}

			return super.create( elem );
		}
	}

	private static class KoLImageView
		extends ImageView
	{
		public KoLImageView( final Element elem )
		{
			super( elem );
		}

		@Override
		public URL getImageURL()
		{
			String src = (String) this.getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
			{
				return null;
			}

			File imageFile = FileUtilities.downloadImage( src );
			
			try
			{
				return imageFile.toURI().toURL();
			}
			catch ( IOException e )
			{
				return null;
			}
		}
	}

	public static final String getFeatureRichHTML( final String location, final String text )
	{
		return RequestEditorKit.getFeatureRichHTML( location, text, true );
	}

	public static final void getFeatureRichHTML( final String location, final StringBuffer buffer )
	{
		RequestEditorKit.getFeatureRichHTML( location, buffer, true );
	}

	// Stupid bureacrats, always ruining everybody's fun with their permits
	// and forms.
	private static final String NO_PERMIT_TEXT =
		"always ruining everybody's fun with their permits and forms.";
	private static final String BUY_PERMIT_TEXT =
		RequestEditorKit.NO_PERMIT_TEXT + " [<a href=\"hermit.php?autopermit=on\">buy a hermit permit</a>]";

	// He looks at you with a disappointed sigh -- looks like you don't have
	// anything worthless enough for him to want to trade for it.
	private static final String NO_WORTHLESS_ITEM_TEXT =
		"worthless enough for him to want to trade for it.";
	private static final String BUY_WORTHLESS_ITEM_TEXT =
		RequestEditorKit.NO_WORTHLESS_ITEM_TEXT + " [<a href=\"hermit.php?autoworthless=on\">fish for a worthless item</a>]";

	private static final ArrayList<String> maps = new ArrayList<String>();
	static
	{
		RequestEditorKit.maps.add( "place.php?whichplace=plains" );
		RequestEditorKit.maps.add( "place.php?whichplace=bathole" );
		RequestEditorKit.maps.add( "fernruin.php" );
		RequestEditorKit.maps.add( "cobbsknob.php" );
		RequestEditorKit.maps.add( "cobbsknob.php?action=tolabs" );
		RequestEditorKit.maps.add( "cobbsknob.php?action=tomenagerie" );
		RequestEditorKit.maps.add( "cyrpt.php" );
		RequestEditorKit.maps.add( "place.php?whichplace=beanstalk" );
		RequestEditorKit.maps.add( "woods.php" );
		RequestEditorKit.maps.add( "friars.php" );
		RequestEditorKit.maps.add( "pandamonium.php" );
		RequestEditorKit.maps.add( "mountains.php" );
		RequestEditorKit.maps.add( "tutorial.php" );
		RequestEditorKit.maps.add( "place.php?whichplace=mclargehuge" );
		RequestEditorKit.maps.add( "island.php" );
		RequestEditorKit.maps.add( "place.php?whichplace=cove" );
		RequestEditorKit.maps.add( "bigisland.php" );
		RequestEditorKit.maps.add( "postwarisland.php" );
		RequestEditorKit.maps.add( "place.php?whichplace=desertbeach" );
		RequestEditorKit.maps.add( "pyramid.php" );
		RequestEditorKit.maps.add( "place.php?whichplace=town_wrong" );
		RequestEditorKit.maps.add( "place.php?whichplace=town_right" );
		RequestEditorKit.maps.add( "place.php?whichplace=spookyraven1" );
		RequestEditorKit.maps.add( "place.php?whichplace=spookyraven2" );
		RequestEditorKit.maps.add( "place.php?whichplace=wormwood" );
		RequestEditorKit.maps.add( "manor3.php" );
		RequestEditorKit.maps.add( "da.php" );
		RequestEditorKit.maps.add( "canadia.php" );
		RequestEditorKit.maps.add( "gnomes.php" );
		RequestEditorKit.maps.add( "heydeze.php" );
		RequestEditorKit.maps.add( "dwarffactory.php" );
	}

	public static final String getFeatureRichHTML( final String location, final String text, final boolean addComplexFeatures )
	{
		if ( text == null || text.length() == 0 )
		{
			return "";
		}

		StringBuffer buffer = new StringBuffer( text );
		RequestEditorKit.getFeatureRichHTML( location, buffer, addComplexFeatures );
		return buffer.toString();
	}

	public static final void getFeatureRichHTML( final String location, final StringBuffer buffer, final boolean addComplexFeatures )
	{
		if ( buffer.length() == 0 )
		{
			return;
		}

		// Skip all decorations on the raw KoL api.

		if ( location.startsWith( "api.php" ) )
		{
			return;
		}

		// Remove bogus <body> tag preceding <head> tag.  topmenu has
		// this, but don't assume other pages are flawless

		StringUtilities.singleStringReplace( buffer, "<body><head>", "<head>" );

		// Apply individual page adjustments

		RequestEditorKit.applyPageAdjustments( location, buffer, addComplexFeatures );

		// Apply adjustments that should be on all pages

		RequestEditorKit.applyGlobalAdjustments( location, buffer, addComplexFeatures );
	}

	protected static final void applyPageAdjustments( final String location, final StringBuffer buffer, final boolean addComplexFeatures )
	{
		// Check for charpane first, since it occurs frequently.

		if ( location.startsWith( "charpane.php" ) )
		{
			if ( addComplexFeatures )
			{
				CharPaneDecorator.decorate( buffer );
			}

			return;
		}

		// Handle topmenu

		if ( location.contains( "menu.php" ) )
		{
			MoonPhaseRequest.decorate( buffer );
			StringUtilities.singleStringReplace( buffer, "</body>",
				"<div style='position: absolute; right: 0px; bottom: 0px;'><font size=-1>" + KoLmafiaCLI.buildRelayScriptMenu() + "[<a href=\"" + location + "\">re</a>]</font></div></body>" );
			return;
		}

		// If clovers were auto-disassembled, show disassembled clovers

		if ( ResultProcessor.disassembledClovers( location ) )
		{
			// Replace not only the bolded item name, but
			// also alt and title tags of the image
			StringUtilities.globalStringReplace( buffer, "ten-leaf clover", "disassembled clover" );
			StringUtilities.singleStringReplace( buffer, "clover.gif", "disclover.gif" );
			StringUtilities.singleStringReplace( buffer, "370834526", "328909735" );
		}

		// Override images, if requested
		RelayRequest.overrideImages( buffer );

		// Make changes which only apply to a single page.

		if ( location.startsWith( "account_combatmacros.php" ) )
		{
			StringUtilities.insertAfter( buffer, "</textarea>", "<script language=JavaScript src=\"/" + KoLConstants.MACROHELPER_JS + "\"></script>" );
		}
		else if ( location.startsWith( "adminmail.php" ) )
		{
			// Per KoL dev team request, add extra warning to the
			// bug report form.
			RequestEditorKit.addBugReportWarning( buffer );
		}
		else if ( location.startsWith( "adventure.php" ) )
		{
			RequestEditorKit.fixTavernCellar( buffer );
			StationaryButtonDecorator.decorate( location, buffer );
			RequestEditorKit.fixDucks( buffer );
			RequestEditorKit.fixRottingMatilda( buffer );
		}
		else if ( location.startsWith( "afterlife.php" ) )
		{
			ValhallaDecorator.decorateAfterLife( location, buffer );
		}
		else if ( location.startsWith( "arcade.php" ) )
		{
			StringBuilder note = new StringBuilder( "Arcade (" );
			int count = InventoryManager.getCount( ItemPool.GG_TOKEN );
			note.append( count );
			note.append( " token" );
			if ( count != 1 )
			{
				note.append( 's' );
			}
			note.append( ", " );
			count = InventoryManager.getCount( ItemPool.GG_TICKET );
			note.append( count );
			note.append( " ticket" );
			if ( count != 1 )
			{
				note.append( 's' );
			}
			note.append( ")</b>" );

			StringUtilities.singleStringReplace( buffer,
				"Arcade</b>", note.toString() );
		}
		else if ( location.startsWith( "ascend.php" ) )
		{
			ValhallaDecorator.decorateGashJump( buffer );
		}
		else if ( location.startsWith( "ascensionhistory.php" ) )
		{
			// No Javascript in Java's HTML renderer
			if ( addComplexFeatures )
			{
				StringUtilities.insertBefore(
					buffer, "</head>", "<script language=\"Javascript\" src=\"/" + KoLConstants.SORTTABLE_JS + "\"></script>" );
				StringUtilities.singleStringReplace(
					buffer, "<table><tr><td class=small>",
					"<table class=\"sortable\" id=\"history\"><tr><td class=small>" );
				StringUtilities.globalStringReplace(
					buffer, "<tr><td colspan=9", "<tr class=\"sortbottom\" style=\"display:none\"><td colspan=9" );
			}
		}
		else if ( location.startsWith( "barrel.php" ) )
		{
			BarrelDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "basement.php" ) )
		{
			BasementDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "bathole.php" ) )
		{
			StringUtilities.globalStringReplace( buffer, "action=bathole.php", "action=adventure.php" );
		}
		else if ( location.startsWith( "beerpong.php" ) )
		{
			BeerPongDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "bet.php" ) )
		{
			MoneyMakingGameDecorator.decorate( location, buffer );
		}
		else if ( location.startsWith( "bigisland.php" ) )
		{
			IslandDecorator.decorateBigIsland( location, buffer );
		}
		else if ( location.startsWith( "casino.php" ) )
		{
			if ( !InventoryManager.hasItem( ItemPool.TEN_LEAF_CLOVER ) )
			{
				StringUtilities.insertAfter( buffer, "<a href=\"casino.php?action=slot&whichslot=11\"", " onclick=\"return confirm('Are you sure you want to adventure here WITHOUT a ten-leaf clover?');\"" );
			}
		}
		else if ( location.startsWith( "cave.php" ) )
		{
			NemesisManager.decorate( location, buffer );
		}
		else if ( location.startsWith( "choice.php" ) )
		{
			RequestEditorKit.fixTavernCellar( buffer );
			StationaryButtonDecorator.decorate( location, buffer );
			RequestEditorKit.addChoiceSpoilers( location, buffer );
		}
		else if ( location.startsWith( "clan_hobopolis.php" ) )
		{
			HobopolisDecorator.decorate( location, buffer );
		}
		else if ( location.startsWith( "council.php" ) )
		{
			RequestEditorKit.decorateCouncil( buffer );
		}
		else if ( location.startsWith( "crypt.php" ) )
		{
			RequestEditorKit.decorateCrypt( buffer );
		}
		else if ( location.startsWith( "dwarffactory.php" ) )
		{
			DwarfFactoryRequest.decorate( location, buffer );
		}
		else if ( location.startsWith( "fight.php" ) )
		{
			RequestEditorKit.fixTavernCellar( buffer );
			StationaryButtonDecorator.decorate( location, buffer );
			DiscoCombatHelper.decorate( buffer );
			RequestEditorKit.addFightModifiers( location, buffer );
			RequestEditorKit.addTaleOfDread( buffer );

			// Do any monster-specific decoration
			FightDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "place.php?whichplace=forestvillage" ) )
		{
			UntinkerRequest.decorate( location, buffer );
		}
		else if ( location.startsWith( "hedgepuzzle.php" ) )
		{
			HedgePuzzleRequest.decorate( buffer );
		}
		else if ( location.startsWith( "hermit.php" ) )
		{
			StringUtilities.singleStringReplace( buffer, RequestEditorKit.NO_PERMIT_TEXT, RequestEditorKit.BUY_PERMIT_TEXT );
			StringUtilities.singleStringReplace( buffer, RequestEditorKit.NO_WORTHLESS_ITEM_TEXT, RequestEditorKit.BUY_WORTHLESS_ITEM_TEXT );
		}
		else if ( location.startsWith( "inventory.php" ) )
		{
			RequestEditorKit.decorateInventory( buffer, addComplexFeatures );
			UseItemDecorator.decorate( location, buffer );
		}
		else if ( location.startsWith( "inv_use.php" ) )
		{
			UseItemDecorator.decorate( location, buffer );
		}
		else if ( location.startsWith( "lair1.php?action=gates" ) )
		{
			SorceressLairManager.decorateGates( buffer );
		}
		else if ( location.startsWith( "lair2.php?preaction=key" ) )
		{
			SorceressLairManager.decorateKey( location, buffer );
		}
		else if ( location.startsWith( "lair6.php?preaction=heavydoor" ) )
		{
			SorceressLairManager.decorateHeavyDoor( buffer );
		}
		else if ( location.startsWith( "lair6.php" ) )
		{
			if ( !KoLCharacter.inAxecore() &&
			     !KoLCharacter.inZombiecore() &&
			     !KoLCharacter.isSneakyPete() )
			{
				SorceressLairManager.decorateFamiliars( buffer );
			}
		}
		else if ( location.contains( "lchat.php" ) )
		{
			StringUtilities.globalStringDelete( buffer, "spacing: 0px;" );
			StringUtilities.globalStringReplace( buffer, "cycles++", "cycles = 0" );
			StringUtilities.globalStringReplace( buffer, "location.hostname", "location.host" );

			StringUtilities.insertBefore(
				buffer, "if (postedgraf",
				"if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } " );

			// This is a hack to fix KoL chat as handled in earlier
			// versions of Opera (doubled chat).

			StringUtilities.insertBefore(
				buffer, "http.onreadystatechange", "executed = false; " );
			StringUtilities.singleStringReplace(
				buffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}
		else if ( location.startsWith( "mall.php" ) )
		{
			MallSearchRequest.decorateMallSearch( buffer );
		}
		else if ( location.startsWith( "manor3.php" ) )
		{
			RequestEditorKit.addWineCellarSpoilers( buffer );
		}
		else if ( location.startsWith( "mining.php" ) )
		{
			MineDecorator.decorate( location, buffer );
		}
		else if ( location.startsWith( "multiuse.php" ) )
		{
			RequestEditorKit.addMultiuseModifiers( buffer );
		}
		else if ( location.startsWith( "pandamonium.php" ) )
		{
			PandamoniumRequest.decoratePandamonium( location, buffer );
		}
		else if ( location.startsWith( "postwarisland.php" ) )
		{
			IslandDecorator.decoratePostwarIsland( location, buffer );
		}
		else if ( location.startsWith( "place.php?whichplace=rabbithole" ) )
		{
			RabbitHoleManager.decorateRabbitHole( buffer );
		}
		else if ( location.startsWith( "place.php?whichplace=spookyraven2" ) )
		{
			RequestEditorKit.add2ndFloorSpoilers( buffer );
		}
		else if ( location.startsWith( "searchplayer.php" ) )
		{
			StringUtilities.insertAfter( buffer, "name=pvponly", " checked" );
			StringUtilities.singleStringReplace( buffer, "value=0 checked", "value=0" );

			if ( KoLCharacter.isHardcore() )
			{
				StringUtilities.insertAfter( buffer, "value=1", " checked" );
			}
			else
			{
				StringUtilities.insertAfter( buffer, "value=2", " checked" );
			}
		}
		else if ( location.startsWith( "tiles.php" ) )
		{
			DvorakDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "volcanomaze.php" ) )
		{
			VolcanoMazeManager.decorate( location, buffer );
		}
		else if ( location.startsWith( "wand.php" ) && !location.contains( "notrim=1" ) )
		{
			ZapRequest.decorate( buffer );
		}
	}

	protected static final void applyGlobalAdjustments( final String location, final StringBuffer buffer, final boolean addComplexFeatures )
	{
		// Handle changes which happen on a lot of different pages
		// rather than just one or two.

		RequestEditorKit.changePunchcardNames( buffer );
		RequestEditorKit.identifyDustyBottles( buffer );
		RequestEditorKit.changePotionImages( buffer );
		RequestEditorKit.decorateLevelGain( buffer );
		RequestEditorKit.addAbsintheLink( buffer );
		RequestEditorKit.addTransponderLink( buffer );
		RequestEditorKit.addBatteryLink( buffer );
		RequestEditorKit.addFolioLink( buffer );
		RequestEditorKit.addNewLocationLinks( buffer );

		// Now do anything which doesn't work in Java's internal HTML renderer

		if ( addComplexFeatures )
		{
			StringUtilities.insertBefore(
				buffer, "</head>", "<script language=\"Javascript\" src=\"/" + KoLConstants.BASICS_JS + "\"></script>" );

			StringUtilities.insertBefore(
				buffer, "</head>", "<link rel=\"stylesheet\" href=\"/" + KoLConstants.BASICS_CSS + "\" />" );
		}

		// Skip additional decorations for the character pane and the top menu

		if ( location.startsWith( "charpane.php" ) || location.contains( "menu.php" ) )
		{
			return;
		}

		if ( addComplexFeatures )
		{
			if ( RequestEditorKit.maps.contains( location ) )
			{
				buffer.insert(
					buffer.indexOf( "</tr>" ),
					"<td width=15 valign=bottom align=left bgcolor=blue><a style=\"color: white; font-weight: normal; font-size: small; text-decoration: underline\" href=\"javascript: attachSafetyText(); void(0);\">?</a>" );
				buffer.insert( buffer.indexOf( "<td", buffer.indexOf( "</tr>" ) ) + 3, " colspan=2" );
			}

			if ( Preferences.getBoolean( "relayAddsUseLinks" ) )
			{
				UseLinkDecorator.decorate( location, buffer );
			}

			if ( buffer.indexOf( "showplayer.php" ) != -1 &&
			     buffer.indexOf( "rcm.js" ) == -1 &&
			     buffer.indexOf( "rcm.2.js" ) == -1 &&
			     buffer.indexOf( "rcm.3.js" ) == -1 &&
			     buffer.indexOf( "rcm.20090915.js" ) == -1 &&
			     buffer.indexOf( "rcm.20101215.js" ) == -1 )
			{
				RequestEditorKit.addChatFeatures( buffer );
			}

			// Always select the contents of text fields when you
			// click on them to make for easy editing.

			if ( Preferences.getBoolean( "autoHighlightOnFocus" ) && buffer.indexOf( "</html>" ) != -1 )
			{
				StringUtilities.insertBefore( buffer, "</html>", "<script src=\"/" + KoLConstants.ONFOCUS_JS + "\"></script>" );
			}
			
			if ( location.contains( "fight.php" ) )
			{
				StringUtilities.insertBefore( buffer, "</html>", "<script src=\"/" + KoLConstants.COMBATFILTER_JS + "\"></script>" );
			}
		}

		Matcher eventMatcher = EventManager.EVENT_PATTERN.matcher( buffer.toString() );
		boolean showingEvents = eventMatcher.find();

		if ( EventManager.hasEvents() && ( showingEvents || location.contains( "main.php" ) ) )
		{
			int eventTableInsertIndex = 0;

			if ( showingEvents )
			{
				eventTableInsertIndex = eventMatcher.start();

				buffer.setLength( 0 );
				buffer.append( eventMatcher.replaceFirst( "" ) );
			}
			else
			{
				eventTableInsertIndex = buffer.indexOf( "</div>" ) + 6;
			}

			StringBuilder eventsTable = new StringBuilder();

			eventsTable.append( "<center><table width=95% cellspacing=0 cellpadding=0>" );
			eventsTable.append( "<tr><td style=\"color: white;\" align=center bgcolor=orange>" );
			eventsTable.append( "<b>New Events:</b>" );
			eventsTable.append( "</td></tr>" );
			eventsTable.append( "<tr><td style=\"padding: 5px; border: 1px solid orange;\" align=center>" );

			Iterator eventHyperTextIterator = EventManager.getEventHyperTexts().iterator();

			while ( eventHyperTextIterator.hasNext() )
			{
				eventsTable.append( eventHyperTextIterator.next() );

				if ( eventHyperTextIterator.hasNext() )
				{
					eventsTable.append( "<br />" );
				}
			}

			eventsTable.append( "</td></tr>" );
			eventsTable.append( "<tr><td height=4></td></tr>" );
			eventsTable.append( "</table></center>" );

			buffer.insert( eventTableInsertIndex, eventsTable.toString() );

			EventManager.clearEventHistory();
		}

		// Having done all the decoration on the page, do things that
		// might modify or depend on those decorations

		// Change border colors if the user wants something other than blue

		String defaultColor = Preferences.getString( "defaultBorderColor" );
		if ( !defaultColor.equals( "blue" ) )
		{
			StringUtilities.globalStringReplace( buffer, "bgcolor=blue", "bgcolor=\"" + defaultColor + "\"" );
			StringUtilities.globalStringReplace( buffer, "border: 1px solid blue", "border: 1px solid " + defaultColor );
		}
	}

	private static final void decorateLevelGain( final StringBuffer buffer )
	{
		String test = "<b>You gain a Level!</b>";
		int index = buffer.indexOf( test );

		if ( index == -1 )
		{
			return;
		}

		StringBuilder links = new StringBuilder();
		boolean haveLinks = false;
		int newLevel = KoLCharacter.getLevel();

		links.append( "<font size=1>" );

		// If we are Level 13 or less, the Council might have quests for us
		if ( newLevel <= 13 )
		{
			links.append( " [<a href=\"council.php\">council</a>]" );
			haveLinks = true;
		}

		// If we are an Avatar of Boris, we can learn a new skill
		if ( KoLCharacter.inAxecore() && newLevel <= 15 )
		{
			links.append( " [<a href=\"da.php?place=gate1\">boris</a>]" );
			haveLinks = true;
		}

		else if ( KoLCharacter.isJarlsberg() && newLevel <= 15 )
		{
			links.append( " [<a href=\"da.php?place=gate2\">jarlsberg</a>]" );
			haveLinks = true;
		}

		else if ( KoLCharacter.isSneakyPete() && newLevel <= 15 )
		{
			links.append( " [<a href=\"da.php?place=gate3\">sneaky pete</a>]" );
			haveLinks = true;
		}

		// Otherwise, if we are level 15 or less, the guild might have a skill for us
		// Zombie Masters don't get access to new skills by gaining a level
		else if ( newLevel <= 15 && !KoLCharacter.inZombiecore() )
		{
			links.append( " [<a href=\"guild.php\">guild</a>]" );
			haveLinks = true;
		}

		links.append( "</font>" );

		if ( haveLinks )
		{
			buffer.insert( index + test.length(), links.toString() );
		}
	}

	private static final void addTransponderLink( final StringBuffer buffer )
	{
		// You can't get there anymore, because you don't know the
		// transporter frequency. You consider beating up Kenneth to
		// see if <i>he</i> remembers it, but you think better of it.

		String test = "You consider beating up Kenneth to see if <i>he</i> remembers it, but you think better of it.";
		int index = buffer.indexOf( test );

		if ( index == -1 )
		{
			test = "You can't get here without the proper transporter frequency.";
			index = buffer.indexOf( test );
		}

		if ( index == -1 )
		{
			return;
		}

		if ( SpaaaceRequest.TRANSPONDER.getCount( KoLConstants.inventory ) == 0 )
		{
			return;
		}

		UseLinkDecorator.UseLink link = new UseLinkDecorator.UseLink( ItemPool.TRANSPORTER_TRANSPONDER, 1, "use transponder", "inv_use.php?which=3&whichitem=" );
		buffer.insert( index + test.length(), link.getItemHTML() );
	}

	private static final AdventureResult WARBEAR_BATTERY = ItemPool.get( ItemPool.WARBEAR_BATTERY, 1 );
	private static final void addBatteryLink( final StringBuffer buffer )
	{
		// Your hoverbelt would totally do the trick to get you up
		// there, only it's out of juice.

		String test = "Your hoverbelt would totally do the trick to get you up there, only it's out of juice.";
		int index = buffer.indexOf( test );
		if ( index == -1 )
		{
			return;
		}

		if ( RequestEditorKit.WARBEAR_BATTERY.getCount( KoLConstants.inventory ) == 0 )
		{
			return;
		}

		UseLinkDecorator.UseLink link = new UseLinkDecorator.UseLink( ItemPool.WARBEAR_BATTERY, 1, "install warbear battery", "inv_use.php?which=3&whichitem=" );
		buffer.insert( index + test.length(), link.getItemHTML() );
	}

	private static final void addFolioLink( final StringBuffer buffer )
	{
		// Remember that devilish folio you read?
		// No, you don't! You don't have it all still in your head!
		// Better find a new one you can read! I swear this:
		// 'Til you do, you can't visit the Suburbs of Dis!

		String test = "'Til you do, you can't visit the Suburbs of Dis!";
		int index = buffer.indexOf( test );

		if ( index == -1 )
		{
			return;
		}

		if ( SuburbanDisRequest.FOLIO.getCount( KoLConstants.inventory ) == 0 )
		{
			return;
		}

		UseLinkDecorator.UseLink link = new UseLinkDecorator.UseLink( ItemPool.DEVILISH_FOLIO, 1, "use devilish folio", "inv_use.php?which=3&whichitem=" );
		buffer.insert( index + test.length(), link.getItemHTML() );
	}

	private static final void addAbsintheLink( final StringBuffer buffer )
	{
		// For some reason, you can't find your way back there.

		String test = "For some reason, you can't find your way back there.";
		int index = buffer.indexOf( test );

		if ( index == -1 )
		{
			return;
		}

		if ( ItemPool.get( ItemPool.ABSINTHE, 1 ).getCount( KoLConstants.inventory ) == 0 )
		{
			return;
		}

		UseLinkDecorator.UseLink link = new UseLinkDecorator.UseLink( ItemPool.ABSINTHE, 1, "use absinthe", "inv_use.php?which=3&whichitem=" );
		buffer.insert( index + test.length(), link.getItemHTML() );
	}

	// <table  width=400  cellspacing=0 cellpadding=0><tr><td style="color: white;" align=center bgcolor=blue>f<b>New Area Unlocked</b></td></tr><tr><td style="padding: 5px; border: 1px solid blue;"><center><table><tr><td><center><table><tr><td valign=center><img src="http://images.kingdomofloathing.com/adventureimages/../otherimages/ocean/corrala.gif"></td><td valign=center class=small><b>The Coral Corral</b>, on <a class=nounder href=seafloor.php><b>The Sea Floor</b></a>.</td></tr></table></center></td></tr></table></center></td></tr><tr><td height=4></td></tr></table>

	private static final Pattern NEW_LOCATION_PATTERN = Pattern.compile( "<table.*?<b>New Area Unlocked</b>.*?(<img[^>]*>).*?(<b>(.*?)</b>)", Pattern.DOTALL );

	public static final void addNewLocationLinks( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "New Area Unlocked" ) == -1 )
		{
			return;
		}

		Matcher matcher = NEW_LOCATION_PATTERN.matcher( buffer );

		// The Trapper can unlock multiple new locations for you at once
		while ( matcher.find() )
		{
			String image = matcher.group(1);
			String boldloc = matcher.group(2);
			String locname = matcher.group(3);
			String url;

			if ( locname.contains( "Degrassi Knoll" ) )
			{
				url = KoLCharacter.knollAvailable() ?
					"place.php?whichplace=knoll_friendly" :
					"place.php?whichplace=knoll_hostile";
			}
			else if ( locname.contains( "A Small Pyramid" ) )
			{
				url = "place.php?whichplace=desertbeach&action=db_pyramid1";
			}
			else
			{
				KoLAdventure adventure = AdventureDatabase.getAdventure( locname );
				if ( adventure == null )
				{
					if ( locname.startsWith( "The " ) )
					{
						adventure = AdventureDatabase.getAdventure( locname.substring( 4) );
					}
					else
					{
						adventure = AdventureDatabase.getAdventure( "The " + locname );
					}
				}

				if ( adventure == null )
				{
					continue;
				}

				url = adventure.getRequest().getURLString();
			}

			StringBuilder replace = new StringBuilder();
			replace.append( "<a href=\"" );
			replace.append( url );
			replace.append( "\">" );
			replace.append( image );
			replace.append( "</a>" );
			StringUtilities.singleStringReplace( buffer, image, replace.toString() );

			replace.setLength( 0 );
			replace.append( "<a class=nounder href=\"" );
			replace.append( url );
			replace.append( "\">" );
			replace.append( boldloc );
			replace.append( "</a>" );
			StringUtilities.singleStringReplace( buffer, boldloc, replace.toString() );
		}
	}

	private static final void decorateInventory( final StringBuffer buffer, final boolean addComplexFeatures )
	{
		// <table width=100%><tr><td colspan=2 width="210"></td><td width=20 rowspan=2></td><td class=small align=center valign=top rowspan=2><font size=2>[<a href="craft.php">craft&nbsp;stuff</a>]&nbsp;  [<a href="sellstuff.php">sell&nbsp;stuff</a>]<br /></font></td><td width=20 rowspan=2></td><td colspan=2 width="210"></td></tr></table>

		StringBuilder links = new StringBuilder();
		boolean sushi = KoLCharacter.hasSushiMat();
		if ( sushi )
		{
			links.append( "[<a href=\"sushi.php\">roll sushi</a>]" );
		}

		AdventureResult wand = KoLCharacter.getZapper();
		if ( wand != null )
		{
			if ( sushi )
			{
				links.append( "&nbsp;&nbsp;" );
			}
			
			links.append( "[<a href=\"wand.php?whichwand=" ).append( wand.getItemId() ).append( "\">zap items</a>]" );
		}


		if ( links.length() > 0 )
		{
			StringUtilities.globalStringDelete(
				buffer,
				"<td width=20 rowspan=2></td>" );
			StringUtilities.singleStringReplace(
				buffer,
				"<br /></font></td>",
				"<br />" + links.toString() + "<br /></font>" );
		}

		// Automatically name the outfit "backup" for simple save
		// purposes while adventuring in browser.

		StringUtilities.insertAfter(
			buffer, "<input type=text name=outfitname", " value=\"Backup\"" );

                if ( !addComplexFeatures )
                {
                        return;
                }

		// Split out normal outfits, custom outfits, automatic outfits
		Matcher fmatcher = OUTFIT_FORM_PATTERN.matcher( buffer );
		if ( !fmatcher.find() )
		{
			return;
		}

		StringBuffer obuffer = new StringBuffer();
		obuffer.append( "<table>" );

		// If there aren't any normal outfits, the Custom Outfits label is absent
		Matcher cmatcher = NOLABEL_CUSTOM_OUTFITS_PATTERN.matcher( fmatcher.group() );
		if ( cmatcher.find() )
		{
			String options = cmatcher.group( 1 );
			addOutfitGroup( obuffer, "outfit2", "Custom", "a custom", options );
		}

		// Find option groups in the whichoutfit drop down
		Matcher omatcher = OPTGROUP_PATTERN.matcher( fmatcher.group() );
		while ( omatcher.find() )
		{
			String group = omatcher.group( 1 );
			String options = omatcher.group( 2 );
			if ( group.equals( "Normal Outfits" ) )
			{
				addOutfitGroup( obuffer, "outfit", "Outfits", "an", options );
			}
			else if ( group.equals( "Custom Outfits" ) )
			{
				addOutfitGroup( obuffer, "outfit2", "Custom", "a custom", options );
			}
			else if ( group.equals( "Automatic Outfits" ) )
			{
				addOutfitGroup( obuffer, "outfit3", "Automatic", "an automatic", options );
			}
		}

		obuffer.append( "</table>" );

		// Replace the original form with a table of forms
		buffer.replace( fmatcher.start(), fmatcher.end(), obuffer.toString() );
	}

	private static final void addOutfitGroup( final StringBuffer buffer, final String formName, final String label, final String type, final String options )
	{
		if ( options.length() == 0 )
		{
			return;
		}

		buffer.append( "<tr><td align=right><form name=" );
		buffer.append( formName );
		buffer.append( " action=inv_equip.php><input type=hidden name=action value=\"outfit\"><input type=hidden name=which value=2><b>" );
		buffer.append( label );
		buffer.append( ":</b> </td><td><select style=\"width: 250px\" name=whichoutfit><option value=0>(select " );
		buffer.append( type );
		buffer.append( " outfit)</option>" );
		buffer.append( options );
		buffer.append( "</select></td><td> <input class=button type=submit value=\"Dress Up!\"><br></form></td></tr>" );
	}

	public static final void addChatFeatures( final StringBuffer buffer )
	{
		StringUtilities.insertBefore(
			buffer, "</html>",
			"<script language=\"Javascript\"> var notchat = true; var " + ChatPoller.getRightClickMenu() + " </script>" + "<script language=\"Javascript\" src=\"/images/scripts/rcm.20101215.js\"></script>" );

		StringUtilities.insertBefore( buffer, "</body>", "<div id='menu' class='rcm'></div>" );
	}

	private static final void addFightModifiers( final String location, final StringBuffer buffer )
	{
		// Change bang potion names in item dropdown
		RequestEditorKit.changePotionNames( buffer );

		// Hilight He-Boulder eye color messages
		if ( KoLCharacter.getFamiliar().getId() == FamiliarPool.HE_BOULDER )
		{
			StringUtilities.globalStringReplace( buffer, "s red eye",
				"s <font color=red>red eye</font>" );
			StringUtilities.globalStringReplace( buffer, " blue eye",
				" <font color=blue>blue eye</font>" );
			StringUtilities.globalStringReplace( buffer, " yellow eye",
				" <font color=olive>yellow eye</font>" );
		}

		RequestEditorKit.insertRoundNumbers( buffer );

		if ( Preferences.getBoolean( "macroLens" ) )
		{
			String test = "<input type=\"hidden\" name=\"macrotext\" value=\"\">";
			if ( buffer.indexOf( test ) == -1 )
			{
				String test2 = "<form name=runaway action=fight.php method=post>";
				int index = buffer.indexOf( test2 );
				if ( index != -1 )
				{
					buffer.insert( index,
						       "<form name=macro action=fight.php method=post><input type=hidden name=action value=\"macro\"><input type=\"hidden\" name=\"macrotext\" value=\"\"><tr><td align=center><select name=whichmacro><option value='0'>(select a macro)</option></select> <input class=button type=submit onclick=\"return killforms(this);\" value=\"Execute Macro\"></td></tr></form>" );
				}
			}
			StringUtilities.singleStringReplace( buffer,
				test,
				"<tr><td><textarea name=\"macrotext\" cols=25 rows=10 placeholder=\"type macro here\"></textarea><script language=JavaScript src=\"/" + KoLConstants.MACROHELPER_JS + "\"></script></td></tr>" );
		}

		if ( buffer.indexOf( "but not before you grab one of its teeth" ) != -1 )
		{
			StringUtilities.singleStringReplace( buffer, "necklace",
				"<a href=\"javascript:void(item('222160625'))\">necklace</a>" );
		}

		int runaway = FightRequest.freeRunawayChance();
		if ( runaway > 0 )
		{
			int pos = buffer.indexOf( "type=submit value=\"Run Away\"" );
			if ( pos != -1 )
			{
				buffer.insert( pos + 27, " (" + runaway + "% chance of being free)" );
			}
		}

		// Add monster data HP/Atk/Def and item drop data
		RequestEditorKit.annotateMonster( buffer );

		// You slap a flyer up on your opponent.  It enrages
		// it.</td></tr>

		int flyerIndex = buffer.indexOf( "You slap a flyer up on your opponent" );
		if ( flyerIndex != -1 )
		{
			String message = "<tr><td colspan=2>" + RequestEditorKit.advertisingMessage() + "</td></tr>";
			flyerIndex = buffer.indexOf( "</tr>", flyerIndex );
			buffer.insert( flyerIndex + 5, message );
		}

		// You are slowed too much by the water, and a stupid dolphin
		// swims up and snags <b>a seaweed</b> before you can grab
		// it.<p>

		int dolphinIndex = buffer.indexOf( "a stupid dolphin swims up and snags" );
		if ( dolphinIndex != -1 )
		{
			// If we have a dolphin whistle in inventory, offer a link to use it.
			if ( InventoryManager.hasItem( ItemPool.DOLPHIN_WHISTLE ) )
			{
				String message = "<br><font size=1>[<a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=3997\">use dolphin whistle</a>]</font><br>";
				dolphinIndex = buffer.indexOf( "<p>", dolphinIndex );
				buffer.replace( dolphinIndex, dolphinIndex + 3, message );
			}
		}

		// Add link for visiting bounty hunter if the last bounty drops
		
		Matcher matcher = RequestEditorKit.BOUNTY_ITEM_PATTERN.matcher( buffer );
		if ( matcher.find() )
		{
			int bountyCount = StringUtilities.parseInt( matcher.group( 2 ) );
			int bountyTotal = StringUtilities.parseInt( matcher.group( 3 ) );
			if ( bountyCount == bountyTotal )
			{
				String message = " <font size=1>[<a href=\"bounty.php\">return to hunter</a>]</font>";
				int bountyIndex = buffer.indexOf( "acquire a bounty item" );
				bountyIndex = buffer.indexOf( "</td>", bountyIndex );
				buffer.insert( bountyIndex, message );
			}
		}
		
		matcher = DwarfFactoryRequest.attackMessage( buffer );
		if ( matcher != null )
		{
			int attack = DwarfFactoryRequest.deduceAttack( matcher );
			buffer.insert( matcher.end(), "<p>(Attack rating = " + attack + ")</p>" );
		}

		matcher = DwarfFactoryRequest.defenseMessage( buffer );
		if ( matcher != null )
		{
			int defense = DwarfFactoryRequest.deduceDefense( matcher );
			buffer.insert( matcher.end(), "<p>(Defense rating = " + defense + ")</p>" );
		}

		matcher = DwarfFactoryRequest.hpMessage( buffer );
		if ( matcher != null )
		{
			// Must iterate over a copy of the buffer, since we'll be modifying it
			matcher = DwarfFactoryRequest.hpMessage( buffer.toString() );
			buffer.setLength( 0 );
			do
			{
				int hp = DwarfFactoryRequest.deduceHP( matcher );
				matcher.appendReplacement( buffer, "$0<p>(Hit Points = " + hp + ")</p>" );
			}
			while ( matcher.find() );
			matcher.appendTail( buffer );
		}

		String monster = FightRequest.getLastMonsterName();

		// We want to decorate battlefield monsters, whether or not you
		// actually find them on the battlefield.
		if ( IslandManager.isBattlefieldMonster( monster ) )
		{
			IslandDecorator.decorateBattlefieldFight( buffer );
		}

		switch ( KoLAdventure.lastAdventureId() )
		{
		case AdventurePool.THEMTHAR_HILLS:
			IslandDecorator.decorateThemtharFight( buffer );
			break;

		case AdventurePool.JUNKYARD_BARREL:
		case AdventurePool.JUNKYARD_REFRIGERATOR:
		case AdventurePool.JUNKYARD_TIRES:
		case AdventurePool.JUNKYARD_CAR:
			// Quest gremlins might have a tool.
			IslandDecorator.decorateGremlinFight( buffer );
			break;

		case AdventurePool.SEASIDE_MEGALOPOLIS:
			MemoriesDecorator.decorateMegalopolisFight( buffer );
			break;

		case AdventurePool.OUTSIDE_THE_CLUB:
			NemesisDecorator.decorateRaverFight( buffer );
			break;
		}
	}

	public static final String advertisingMessage()
	{
		int ML = Preferences.getInteger( "flyeredML" );
		float percent = Math.min( 100.0f * (float)ML/10000.0f, 100.0f );
		return "You have completed " +
			KoLConstants.FLOAT_FORMAT.format( percent ) +
			"% of the necessary advertising.";
	}

	private static final void annotateMonster( final StringBuffer buffer )
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();

		if ( monster == null )
		{
			return;
		}

		// Don't show monster unless we know combat stats or items
		// or monster element
		if ( monster.getHP() == 0 &&
		     monster.getItems().isEmpty() &&
		     monster.getDefenseElement() == MonsterDatabase.Element.NONE )
		{
			return;
		}

		StringBuffer monsterData = new StringBuffer( "<font size=2 color=gray>" );
		int nameIndex = buffer.indexOf( "<span id='monname" );
		int insertionPointForData;

		if ( nameIndex != -1 )
		{
			int combatIndex = buffer.indexOf( "</span>", nameIndex );
			if ( combatIndex == -1 )
			{
				return;
			}
			insertionPointForData = combatIndex + 7;
		}
		else
		{
			// find bold "Combat"
			insertionPointForData = buffer.indexOf( "<b>" );
			if ( insertionPointForData == -1 )
			{
				return;
			}
			// find bold monster name
			nameIndex = buffer.indexOf( "<b>", insertionPointForData + 4 );
			if ( nameIndex == -1 )
			{
				return;
			}
			buffer.insert( nameIndex, "<span id='monname'>" );
			// find end of monster
			int combatIndex = buffer.indexOf( "</td>", nameIndex );
			if ( combatIndex == -1 )
			{
				return;
			}
			buffer.insert( combatIndex, "</span>" );
			insertionPointForData = combatIndex + 7;
		}

		monsterData.append( "<br />HP: " );
		monsterData.append( MonsterStatusTracker.getMonsterHealth() );
		monsterData.append( ", Atk: " );
		monsterData.append( MonsterStatusTracker.getMonsterAttack() );
		monsterData.append( ", Def: " );
		monsterData.append( MonsterStatusTracker.getMonsterDefense() );
		monsterData.append( ", Type: " );
		monsterData.append( MonsterStatusTracker.getMonsterPhylum().toString() );

		String monsterName = FightRequest.getLastMonsterName();
		if ( monsterName.indexOf( "pirate" ) != -1 && !( monsterName.equalsIgnoreCase( "Stone Temple Pirate" ) ) )
		{
			int count = BeerPongRequest.countPirateInsults();
			monsterData.append( ", Insults: ");
			monsterData.append( count );
			monsterData.append( " (");
			float odds = BeerPongRequest.pirateInsultOdds( count ) * 100.0f;
			monsterData.append( KoLConstants.FLOAT_FORMAT.format( odds ) );
			monsterData.append( "%)");
		}
		else if ( monsterName.equalsIgnoreCase( "Black Pudding" ) )
		{
			int count = Preferences.getInteger( "blackPuddingsDefeated" );
			monsterData.append( ", Defeated: ");
			monsterData.append( count );
		}

		String[] guardianData = SorceressLairManager.findGuardianByName( monsterName );
		if ( guardianData != null && Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			String itemName = SorceressLairManager.guardianItem( guardianData );
			monsterData.append( "<br />Defeated by a <font color=#DD00FF>" ).append( itemName ).append( "</font>" );

			// Auto-select the correct item in the dropdown, which
			// must be later in the buffer than insertionPoint
			// where we annotate the monster.

			// We must remove any other selected item, since the
			// last one in the select list wins.

			int itemId = ItemDatabase.getItemId( itemName );
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( itemId ) );
		}

		String danceMoveStatus = NemesisDecorator.danceMoveStatus( monsterName );
		if ( danceMoveStatus != null )
		{
			monsterData.append( "<br />" );
			monsterData.append( danceMoveStatus );
		}

		List items = MonsterStatusTracker.getLastMonster().getItems();
		if ( !items.isEmpty() )
		{
			monsterData.append( "<br />Drops: " );
			for ( int i = 0; i < items.size(); ++i )
			{
				if ( i != 0 )
				{
					monsterData.append( ", " );
				}
				AdventureResult item = (AdventureResult) items.get( i );
				int rate = item.getCount() >> 16;
				monsterData.append( item.getName() );
				switch ( (char) item.getCount() & 0xFFFF )
				{
				case 'p':
					monsterData.append( " (" );
					monsterData.append( rate );
					monsterData.append( " pp only)" );
					break;
				case 'n':
					monsterData.append( " (" );
					monsterData.append( rate );
					monsterData.append( " no pp)" );
					break;
				case 'c':
					monsterData.append( " (" );
					monsterData.append( rate );
					monsterData.append( " cond)" );
					break;
				case 'f':
					monsterData.append( " (" );
					monsterData.append( rate );
					monsterData.append( " no mod)" );
					break;
				case 'a':
					monsterData.append( " (stealable accordion)" );
					break;
				default:
					monsterData.append( " (" );
					monsterData.append( rate );
					monsterData.append( ")" );
				}
			}
		}
		String bounty = BountyDatabase.getNameByMonster( MonsterDatabase.findMonster( monsterName, false ).getName() );
		if ( bounty != null )
		{
			if ( items.isEmpty() )
			{
			monsterData.append( "<br />Drops: " );			
			}
			else
			{
				monsterData.append( ", " );
			}
			monsterData.append( bounty + " (bounty)" );
		}

		IslandDecorator.appendMissingGremlinTool( monsterData );
		monsterData.append( "</font>" );
		buffer.insert( insertionPointForData, monsterData.toString() );

		// Insert color for monster element
		MonsterDatabase.Element monsterElement = monster.getDefenseElement();
		if ( monsterElement != MonsterDatabase.Element.NONE )
		{
			int insertionPointForElement = nameIndex + 6;
			buffer.insert( insertionPointForElement, "class=\"element" + monsterElement + "\" " );
		}
	}

	private static final void addMultiuseModifiers( final StringBuffer buffer )
	{
		// Change bang potion names in item dropdown
		RequestEditorKit.changePotionNames( buffer );
	}

	private static final void add2ndFloorSpoilers( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		// Insert GMoB/Ballroom song spoilers
		StringBuilder spoiler = new StringBuilder();
		if ( Preferences.getBoolean( "guyMadeOfBeesDefeated" ) )
		{
			spoiler.append( "GMoB: dead<br>" );
		}
		else
		{
			int count = Preferences.getInteger( "guyMadeOfBeesCount" );
			if ( count > 0 )
			{
				spoiler.append( "GMoB: " );
				spoiler.append( count );
				spoiler.append( "<br>" );
			}
		}

		if ( KoLCharacter.getAscensions() == Preferences.getInteger( "lastQuartetAscension" ) )
		{
			switch ( Preferences.getInteger( "lastQuartetRequest" ) )
			{
			case 1:
				spoiler.append( "Song: +ML<br>" );
				break;
			case 2:
				spoiler.append( "Song: -combat<br>" );
				break;
			case 3:
				spoiler.append( "Song: +items<br>" );
				break;
			}
		}

		if ( spoiler.length() > 0 )
		{
			spoiler.insert( 0, "<small><center>" );
			spoiler.append( "</center></small>" );
			StringUtilities.singleStringReplace( buffer,
				"<img src=\"http://images.kingdomofloathing.com/otherimages/manor/sm2_3.gif\" width=100 height=100 border=0 alt=\"\" title=\"\">",
				spoiler.toString() );
		}
	}

	private static final void addWineCellarSpoilers( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		// Change dusty bottle names in item dropdown
		RequestEditorKit.changeDustyBottleNames( buffer );
		CellarDecorator.decorate( buffer );
	}

	private static final void identifyDustyBottles( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "wine2.gif" ) == -1 )
		{
			return;
		}
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		for ( int i = 2271; i <= 2276; ++i )
		{
			String oldName = ItemDatabase.getItemName( i );
			if ( buffer.indexOf( oldName ) == -1 )
			{
				continue;
			}

			String type = ItemDatabase.shortDustyBottleType( i );
			String newName = StringUtilities.globalStringReplace( oldName, " of", " of " + type );
			String oldPlural = ItemDatabase.getPluralById( i );
			String newPlural = StringUtilities.globalStringReplace( oldPlural, " of", " of " + type );
			StringUtilities.globalStringReplace( buffer, oldName + "</b>", newName + "</b>" );
			StringUtilities.globalStringReplace( buffer, oldPlural + "</b>", newPlural + "</b>" );
		}
	}

	private static final void changePotionImages( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "exclam.gif" ) == -1 &&
		     buffer.indexOf( "vial.gif" ) == -1)
		{
			return;
		}
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		ArrayList<String> potionNames = new ArrayList<String>();
		ArrayList<String> pluralNames = new ArrayList<String>();
		ArrayList<String> potionEffects = new ArrayList<String>();

		for ( int i = 819; i <= 827; ++i )
		{
			String name = ItemDatabase.getItemName( i );
			if ( buffer.indexOf( name ) != -1 )
			{
				String effect = Preferences.getString( "lastBangPotion" + i );
				if ( !effect.equals( "" ) )
				{
					potionNames.add( name );
					pluralNames.add( ItemDatabase.getPluralById( i ) );
					potionEffects.add( " of " + effect );
				}
			}
		}
		for ( int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i )
		{
			String name = ItemDatabase.getItemName( i );
			String plural = ItemDatabase.getPluralById( i );
			if ( buffer.indexOf( name ) != -1 ||
				(plural != null && buffer.indexOf( plural ) != -1) )
			{
				String effect = Preferences.getString( "lastSlimeVial" + i );
				if ( !effect.equals( "" ) )
				{
					potionNames.add( name );
					pluralNames.add( plural );
					potionEffects.add( ": " + effect );
				}
			}
		}

		if ( potionNames.isEmpty() )
		{
			return;
		}

		for ( int i = 0; i < potionNames.size(); ++i )
		{
			String name = potionNames.get( i );
			String plural = pluralNames.get( i );
			String effect = potionEffects.get( i );

			StringUtilities.globalStringReplace( buffer, name + "</b>", name + effect + "</b>" );
			if ( plural != null )
			{
				StringUtilities.globalStringReplace( buffer, plural + "</b>", plural + effect + "</b>" );
			}
		}
	}

	private static final void changePotionNames( final StringBuffer buffer )
	{
		for ( int i = 819; i <= 827; ++i )
		{
			String name = ItemDatabase.getItemName( i );
			if ( buffer.indexOf( name ) != -1 )
			{
				String effect = Preferences.getString( "lastBangPotion" + i );
				if ( effect.equals( "" ) )
				{
					continue;
				}

				StringUtilities.globalStringReplace( buffer, name, name + " of " + effect );
				// Pluralize correctly
				StringUtilities.globalStringReplace( buffer, name + " of " + effect + "s", name + "s of " + effect );
			}
		}
		for ( int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i )
		{
			String name = ItemDatabase.getItemName( i );

			// I have no idea how this can happen, but it does,
			// and it screws people up badly when it does
			if ( name == null )
			{
				continue;
			}

			String plural = ItemDatabase.getPluralById( i );
			if ( buffer.indexOf( name ) != -1 ||
				(plural != null && buffer.indexOf( plural ) != -1) )
			{
				String effect = Preferences.getString( "lastSlimeVial" + i );
				if ( effect.equals( "" ) )
				{
					continue;
				}

				StringUtilities.globalStringReplace( buffer, name, name + ": " + effect );
				// Pluralize correctly
				if ( plural != null )
				{
					StringUtilities.globalStringReplace( buffer, plural, plural + ": " + effect );
				}
			}
		}
	}

	public static Object[][] PUNCHCARDS =
	{
		// Verbs
		{ IntegerPool.get( 3146 ),
		  "El Vibrato punchcard (115 holes)",
		  "El Vibrato punchcard (ATTACK)"
		},
		{ IntegerPool.get( 3147 ),
		  "El Vibrato punchcard (97 holes)",
		  "El Vibrato punchcard (REPAIR)"
		},
		{ IntegerPool.get( 3148 ),
		  "El Vibrato punchcard (129 holes)",
		  "El Vibrato punchcard (BUFF)"
		},
		{ IntegerPool.get( 3149 ),
		  "El Vibrato punchcard (213 holes)",
		  "El Vibrato punchcard (MODIFY)"
		},
		{ IntegerPool.get( 3150 ),
		  "El Vibrato punchcard (165 holes)",
		  "El Vibrato punchcard (BUILD)"
		},

		// Objects
		{ IntegerPool.get( 3151 ),
		  "El Vibrato punchcard (142 holes)",
		  "El Vibrato punchcard (TARGET)"
		},
		{ IntegerPool.get( 3152 ),
		  "El Vibrato punchcard (216 holes)",
		  "El Vibrato punchcard (SELF)"
		},
		{ IntegerPool.get( 3153 ),
		  "El Vibrato punchcard (88 holes)",
		  "El Vibrato punchcard (FLOOR)"
		},
		{ IntegerPool.get( 3154 ),
		  "El Vibrato punchcard (182 holes)",
		  "El Vibrato punchcard (DRONE)"
		},
		{ IntegerPool.get( 3155 ),
		  "El Vibrato punchcard (176 holes)",
		  "El Vibrato punchcard (WALL)"
		},
		{ IntegerPool.get( 3156 ),
		  "El Vibrato punchcard (104 holes)",
		  "El Vibrato punchcard (SPHERE)"
		},
	};

	private static final void changePunchcardNames( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "El Vibrato punchcard" ) == -1 )
		{
			return;
		}

		for ( int i = 0; i < PUNCHCARDS.length; ++i )
		{
			Object [] punchcard = PUNCHCARDS[i];
			if ( buffer.indexOf( (String) punchcard[1] ) != -1 )
			{
				StringUtilities.globalStringReplace( buffer, (String) punchcard[1], (String) punchcard[2] );
			}
		}
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "title=\"Arcane Glyph #(\\d)\"" );

	private static final void changeDustyBottleNames( final StringBuffer buffer )
	{
		ItemDatabase.getDustyBottles();

		int glyphs[] = new int[ 3 ];

		Matcher matcher = RequestEditorKit.GLYPH_PATTERN.matcher( buffer );

		if ( !matcher.find() )
		{
			return;
		}
		glyphs[ 0 ] = StringUtilities.parseInt( matcher.group( 1 ) );

		if ( !matcher.find() )
		{
			return;
		}
		glyphs[ 1 ] = StringUtilities.parseInt( matcher.group( 1 ) );

		if ( !matcher.find() )
		{
			return;
		}
		glyphs[ 2 ] = StringUtilities.parseInt( matcher.group( 1 ) );

		// We are looking at the glyphs in the Wine Cellar or are
		// pouring wines into the pedestal. See what we've done so far.

		int done = Preferences.getInteger( "wineCellarProgress" );

		GoalManager.clearGoals();

		// If we have poured all three wines, nothing more to do.
		if ( done == 3 )
		{
			return;
		}

		boolean haveGoals = false;

		// Determine which wines correspond to which glyph
		String name = null;
		for ( int i = 2271; i <= 2276; ++i )
		{
			int glyph = Preferences.getInteger( "lastDustyBottle" + i );
			for ( int j = done; j < 3; ++j )
			{
				if ( glyph != glyphs[ j ] )
				{
					continue;
				}

				AdventureResult wine = ItemPool.get( i, 1 );
				if ( !KoLConstants.inventory.contains( wine ) )
				{
					if ( !haveGoals )
					{
						// Updating the location does not work if we have goals already
						AdventureFrame.updateSelectedAdventure( AdventureDatabase.getAdventure( "Haunted Wine Cellar (automatic)" ) );
					}
					// Add the wine to the goals
					GoalManager.addGoal( wine );
					haveGoals = true;
				}

				if ( j == done )
				{
					// Point to the wine in the dropdown
					name = ItemDatabase.getItemName( i );
				}

				break;
			}
		}

		// Set the name
		if ( name != null )
		{
			if ( buffer.indexOf( name ) != -1 )
			{
				// He has some of the correct wine in inventory. Highlight it.
				String find = ">" + name;
				String replace = " selected>--&gt;" + name + "&lt;--";
				StringUtilities.globalStringReplace( buffer, find, replace );
			}
			else
			{
				// He has none of the correct wine. Tell him what to get.
				String find = "- select a bottle -";
				String replace = "(you need a " + name + ")";
				StringUtilities.globalStringReplace( buffer, find, replace );
			}
		}
	}

	private static final void fixTavernCellar( final StringBuffer buffer )
	{
		// When you adventure in the Typical Tavern Cellar, the
		// Adventure Again link takes you to the map. Fix that link
		// as follows:
		//
		// (new) Explore Next Unexplored Square
		// Go back to the Typical Tavern Cellar

		int index = buffer.indexOf( "<a href=\"cellar.php\">" );
		if ( index == -1 )
		{
			return;
		}

		int unexplored = TavernManager.nextUnexploredSquare();
		if ( unexplored <= 0 )
		{
			return;
		}

		StringBuilder link = new StringBuilder();

		link.append( "<a href=\"cellar.php?action=explore&whichspot=" );
		link.append( String.valueOf( unexplored ) );
		link.append( "\">Explore Next Unexplored Square</a><p>" );
		buffer.insert( index, link.toString() );
	}

	private static final void addTaleOfDread( final StringBuffer buffer )
	{
		// You hear the scratching sounds of a quill pen from inside
		// your sack. A new Tale of Dread has written itself in your
		// scary storybook!

		int index = buffer.indexOf( "A new Tale of Dread" );
		if ( index == -1 )
		{
			return;
		}

		String monster = StringUtilities.singleStringReplace( MonsterStatusTracker.getLastMonsterName(), " (dreadsylvanian)", "" );
		String find = "your scary storybook!";
		StringBuilder replace = new StringBuilder( find );
		replace.append( " <font size=1>[<a href=\"" );
		replace.append( "/KoLmafia/redirectedCommand?cmd=taleofdread " );
		replace.append( monster );
		replace.append( " redirect" );
		replace.append( "&pwd=" );
		replace.append( GenericRequest.passwordHash );
		replace.append( "\">read it</a>]</font>" );

		StringUtilities.singleStringReplace( buffer, find, replace.toString() );
	}

	private static final void insertRoundNumbers( final StringBuffer buffer )
	{
		Matcher m = FightRequest.ONTURN_PATTERN.matcher( buffer );
		if ( !m.find() )
		{
			return;
		}
		int round = StringUtilities.parseInt( m.group( 1 ) );
		m = RequestEditorKit.ROUND_SEP_PATTERN.matcher( buffer.toString() );
		buffer.setLength( 0 );
		while ( m.find() )
		{
			if ( m.group().startsWith( "<b" ) )
			{	// Initial round - add # after "Combat"
				m.appendReplacement( buffer, "<b>Combat: Round " );
				buffer.append( round++ );
				buffer.append( "!</b>" );
			}
			else
			{	// Subsequent rounds - replace <hr> with bar like title
				m.appendReplacement( buffer, "<table width=100%><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Round " );
				buffer.append( round++ );
				buffer.append( "!</b></td></tr></table>" );
			}
		}
		m.appendTail( buffer );
	}

	private static final void addChoiceSpoilers( final String location, final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers"  ) )
		{
			return;
		}

		// Make sure that it's an actual choice adventure
		int choice = ChoiceManager.extractChoice( buffer.toString() );

		if ( choice == 0 )
		{
			// It's a response to taking a choice.
			RequestEditorKit.decorateChoiceResponse( location, buffer );
			return;
		}

		// Do any choice-specific decorations
		ChoiceManager.decorateChoice( choice, buffer );

		String text = buffer.toString();
		Matcher matcher = FORM_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		// Find the options for the choice we've encountered
		Object[][] spoilers = ChoiceManager.choiceSpoilers( choice );

		if ( spoilers == null )
		{	// Don't give up - there may be a specified choice even if there
			// are no spoilers.
			spoilers = new Object[][] { null, null, {} };
		}

		int index1 = matcher.start();
		int decision = ChoiceManager.getDecision( choice, text );

		buffer.setLength( 0 );
		buffer.append( text.substring( 0, index1 ) );

		while ( true )
		{
			int index2 = text.indexOf( "</form>", index1 );

			// If KoL says we've run out of choices, quit now
			if ( index2 == -1 )
			{
				break;
			}

			String currentSection = text.substring( index1, index2 );
			Matcher optionMatcher = RequestEditorKit.OPTION_PATTERN.matcher( currentSection );
			if ( !optionMatcher.find() )
			{	// this wasn't actually a choice option - strange!
				buffer.append( currentSection );
				buffer.append( "</form>" );
				index1 = index2 + 7;
				continue;
			}

			int i = StringUtilities.parseInt( optionMatcher.group( 1 ) );
			if ( i != decision )
			{
				buffer.append( currentSection );
			}
			else
			{
				int pos = currentSection.lastIndexOf( "value=\"" );
				buffer.append( currentSection.substring( 0, pos + 7 ) );
				buffer.append( "&rarr; " );
				buffer.append( currentSection.substring( pos + 7 ) );
			}

			// Start spoiler text
			while ( i > 0 )
			{
				// Say what the choice will give you
				Object spoiler = ChoiceManager.choiceSpoiler( choice, i, spoilers[ 2 ] );

				// If we have nothing to say about this option, don't say anything
				if ( spoiler == null )
				{
					break;
				}

				String name = spoiler.toString();
				if ( name.equals( "" ) )
				{
					break;
				}

				buffer.append( "<br><font size=-1>(" );
				buffer.append( name );
	
				// If this decision has an item associated with it, annotate it
				if ( spoiler instanceof ChoiceManager.Option )
				{
					AdventureResult item = ((ChoiceManager.Option)spoiler).getItem();
	
					// If this decision leads to an item...
					if ( item != null )
					{
						// List # in inventory
						buffer.append( "<img src=\"/images/itemimages/magnify.gif\" valign=middle onclick=\"descitem('" );
						buffer.append( ItemDatabase.getDescriptionId( item.getItemId() ) );
						buffer.append( "');\">" );
	
						int available = KoLCharacter.hasEquipped( item ) ? 1 : 0;
						available += item.getCount( KoLConstants.inventory );
	
						buffer.append( available );
						buffer.append( " in inventory" );
					}
				}
	
				// Finish spoiler text
				buffer.append( ")</font>" );
				break;
			}
			buffer.append( "</form>" );
			index1 = index2 + 7;
		}

		buffer.append( text.substring( index1 ) );
	}

	private static final void decorateChoiceResponse( final String location, final StringBuffer buffer )
	{
		Matcher matcher = RequestEditorKit.CHOICE2_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return;
		}

		int choice = StringUtilities.parseInt( matcher.group( 1 ) );

		switch ( choice )
		{
		// The Oracle Will See You Now
		case 3:
			StringUtilities.singleStringReplace(
				buffer, "It's actually a book.  Read it.",
				"It's actually a book. <font size=1>[<a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=818\">read it</a>]</font>" );
			break;

		// Wheel In the Pyramid, Keep on Turning
		case 134:
		case 135:
			PyramidRequest.decorateChoiceResponse( buffer );
			break;

		case 392:
			MemoriesDecorator.decorateElementsResponse( buffer );
			break;
		case 443:
			// Chess Puzzle
			RabbitHoleManager.decorateChessPuzzleResponse( buffer );
			break;
		// Of Course!
		case 509:
			// You should probably go tell Bart you've fixed his
			// rat problem.
			StringUtilities.singleStringReplace(
				buffer, "you've fixed his rat problem.",
				"you've fixed his rat problem. <font size=1>[<a href=\"tavern.php?place=barkeep\">Visit Bart</a>]</font>" );
			break;
		case 537:
			// Play Porko!
		case 540:
			// Big-Time Generator
			SpaaaceRequest.decoratePorko( buffer );
			break;

		case 571:
			// Your Minstrel Vamps
			RequestEditorKit.addMinstrelNavigationLink( buffer, "Go to the Typical Tavern", "tavern.php" );
			break;

		case 572:
			// Your Minstrel Clamps
			RequestEditorKit.addMinstrelNavigationLink( buffer, "Go to the Knob Shaft", "adventure.php?snarfblat=101" );
			break;

		case 573:
			// Your Minstrel Stamps
			// Add a link to the Luter's Grave
			RequestEditorKit.addMinstrelNavigationLink( buffer, "Go to the Luter's Grave", 
				"place.php?whichplace=plains&action=lutersgrave" );
			break;

		case 576:
			// Your Minstrel Camps
			RequestEditorKit.addMinstrelNavigationLink( buffer, "Go to the Icy Peak", "adventure.php?snarfblat=110" );
			break;

		case 577:
			// Your Minstrel Scamp
			RequestEditorKit.addMinstrelNavigationLink( buffer, "Go to the Ancient Buried Pyramid", "pyramid.php" );
			break;
		}
	}

	private static final void addMinstrelNavigationLink( final StringBuffer buffer, final String tag, final String url )
	{
		int index = buffer.lastIndexOf( "<table>" );
		if ( index == -1 )
		{
			return;
		}

		index = buffer.indexOf( "<p>", index );
		if ( index == -1 )
		{
			return;
		}

		StringBuilder link = new StringBuilder();

		link.append( "<p><a href=\"" );
		link.append( url );
		link.append( "\">" );
		link.append( tag );
		link.append( "</a>" );

		buffer.insert( index, link.toString()  );
	}

	private static final void decorateCouncil( final StringBuffer buffer )
	{
		if ( !KoLCharacter.inAxecore() )
		{
			return;
		}

		int index = buffer.lastIndexOf( "<p>" );
		if ( index != -1 )
		{
			buffer.insert( index + 3, "<center><a href=\"da.php?place=gate1\">Bask in the Glory of Boris</a></center><br>" );
		}
	}

	private static final void decorateCrypt( final StringBuffer buffer )
	{
		if ( Preferences.getInteger( "cyrptTotalEvilness") == 0 )
		{
			return;
		}

		int nookEvil = Preferences.getInteger( "cyrptNookEvilness" );
		int nicheEvil = Preferences.getInteger( "cyrptNicheEvilness" );
		int crannyEvil = Preferences.getInteger( "cyrptCrannyEvilness" );
		int alcoveEvil = Preferences.getInteger( "cyrptAlcoveEvilness" );

		String nookColor = nookEvil > 25 ? "000000" : "FF0000";
		String nookHint = nookEvil > 25 ? "Item Drop" : "<b>BOSS</b>";
		String nicheColor = nicheEvil > 25 ? "000000" : "FF0000";
		String nicheHint = nicheEvil > 25 ? "Sniff Dirty Lihc" : "<b>BOSS</b>";
		String crannyColor = crannyEvil > 25 ? "000000" : "FF0000";
		String crannyHint = crannyEvil > 25 ? "ML & Noncombat" : "<b>BOSS</b>";
		String alcoveColor = alcoveEvil > 25 ? "000000" : "FF0000";
		String alcoveHint = alcoveEvil > 25 ? "Initiative" : "<b>BOSS</b>";

		StringBuilder evilometer = new StringBuilder();

		evilometer.append( "<table cellpadding=0 cellspacing=0><tr><td colspan=3>" );
		evilometer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/cyrpt/eo_top.gif\">" );
		evilometer.append( "<tr><td><img src=\"http://images.kingdomofloathing.com/otherimages/cyrpt/eo_left.gif\">" );
		evilometer.append( "<td width=150><center>" );

		if ( nookEvil > 0 )
		{
			evilometer.append( "<font size=2 color=\"#" );
			evilometer.append( nookColor );
			evilometer.append( "\"><b>Nook</b> - " );
			evilometer.append( nookEvil );
			evilometer.append( "<br><font size=1>" );
			evilometer.append( nookHint );
			evilometer.append( "<br></font></font>" );
		}

		if ( nicheEvil > 0 )
		{
			evilometer.append( "<font size=2 color=\"#" );
			evilometer.append( nicheColor );
			evilometer.append( "\"><b>Niche</b> - " );
			evilometer.append( nicheEvil );
			evilometer.append( "<br><font size=1>" );
			evilometer.append( nicheHint );
			evilometer.append( "<br></font></font>" );
		}

		if ( crannyEvil > 0 )
		{
			evilometer.append( "<font size=2 color=\"#" );
			evilometer.append( crannyColor );
			evilometer.append( "\"><b>Cranny</b> - " );
			evilometer.append( crannyEvil );
			evilometer.append( "<br><font size=1>" );
			evilometer.append( crannyHint );
			evilometer.append( "<br></font></font>" );
		}
		
		if ( alcoveEvil > 0 )
		{
			evilometer.append( "<font size=2 color=\"#" );
			evilometer.append( alcoveColor );
			evilometer.append( "\"><b>Alcove</b> - " );
			evilometer.append( alcoveEvil );
			evilometer.append( "<br><font size=1>" );
			evilometer.append( alcoveHint );
			evilometer.append( "<br></font></font>" );
		}

		evilometer.append( "<td><img src=\"http://images.kingdomofloathing.com/otherimages/cyrpt/eo_right.gif\"><tr><td colspan=3>" );
		evilometer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/cyrpt/eo_bottom.gif\"></table>" );

		String selector = "</map><table";
		int index = buffer.indexOf( selector );
		buffer.insert( index + selector.length(), "><tr><td><table" );

		index = buffer.indexOf( "</tr></table><p><center><A href=plains.php>" );
		evilometer.insert( 0, "</table><td>" );
		buffer.insert( index + 5, evilometer.toString() );
	}

	private static final void addBugReportWarning( final StringBuffer buffer )
	{
		// <div id="type_1">
		// <table><tr><td><b>IMPORTANT:</b> If you can see this notice,
		// the information you're about to submit may be seen by dev
		// team volunteers in addition to the staff of Asymmetric
		// Publications.<p>For the protection of your privacy, please
		// do not submit any passwords, personal data, or donation
		// information as a bug report! If you're having a donation or
		// store issue, please select the appropriate category
		// above.</td></tr></table>
		// <p><b>Please describe the bug:</b></p>
		// <textarea class="req" name=message cols=60 rows=10></textarea><br>
		// </div>

		int index = buffer.indexOf( "<p><b>Please describe the bug:</b></p>" );
		if ( index == -1 )
		{
			return;
		}

		StringBuilder disclaimer = new StringBuilder();
		disclaimer.append( "<p><span style=\"width: 95%; border: 3px solid red; color: red; padding: 5px; text-align: left; margin-bottom: 10px; background-color: rgb(254, 226, 226); display:block;\">" );
		disclaimer.append( "You are currently running in the KoLmafia Relay Browser. It is possible that the bug you are experiencing is not in KoL itself, but is a result of KoLmafia, a Greasemonkey script, or another client-side modification. The KoL team requests that you verify that you can reproduce the bug in a vanilla browser with all add-ons and extensions disabled before submitting a bug report." );
		disclaimer.append( "</span>" );

		buffer.insert( index, disclaimer.toString() );
	}

	private static final void fixDucks( final StringBuffer buffer )
	{
		// KoL does not currently provide a link back to the farm after
		// you defeat the last duck.

		if ( buffer.indexOf( "ducks" ) == -1 )
		{
			return;
		}

		// But if they fix it and it now adds one, cool.

		if ( buffer.indexOf( "island.php" ) != -1 )
		{
			return;
		}

		String war = IslandManager.warProgress();
		String test;
		String url;

		if ( war.equals( "finished" ) )
		{
			// You wander around the farm for a while, but can't
			// find any additional ducks to fight. Maybe some more
			// will come out of hiding by tomorrow.
			test = "any additional ducks";
			url = "postwarisland.php";
		}
		else
		{
			// There are no more ducks here.
			test = "There are no more ducks here.";
			url = "bigisland.php?place=farm";
		}

		if ( buffer.indexOf( test ) == -1 )
		{
			return;
		}

		int index = buffer.indexOf( "</body></html>" );
		if ( index != -1 )
		{
			buffer.insert( index, "<center><table width=95% cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Adventure Again:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center><p><a href=\"" + url + "\">Go back to The Mysterious Island of Mystery</a></center></td></tr></table></center></td></tr><tr><td height=4></td></tr></table></center>" );
		}
	}

	private static final AdventureResult DANCE_CARD = ItemPool.get( ItemPool.DANCE_CARD, 1);

	private static final void fixRottingMatilda( final StringBuffer buffer )
	{
		// Give player a link to use another dance card

		if ( buffer.indexOf( "Rotting Matilda" ) == -1 ||
		     DANCE_CARD.getCount( KoLConstants.inventory ) <= 0 )
		{
			return;
		}

		int index = buffer.indexOf( "<p><a href=\"adventure.php?snarfblat=109\">" );
		if ( index != -1 )
		{
			String link = "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem=1963&pwd=" + GenericRequest.passwordHash + "&ajax=1');void(0);\">Use another dance card</a>";
			buffer.insert( index, link );
		}
	}

	private static class KoLSubmitView
		extends FormView
	{
		public KoLSubmitView( final Element elem )
		{
			super( elem );
		}

		@Override
		public Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && ( c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox ) )
			{
				c.setBackground( Color.white );
			}

			return c;
		}

		@Override
		public void submitData( final String data )
		{
			// Get the element

			Element inputElement = this.getElement();

			if ( inputElement == null )
			{
				return;
			}

			// Retrieve the frame which is being used by this form
			// viewer.

			RequestFrame frame = this.findFrame();

			// If there is no frame, then there's nothing to
			// refresh, so return.

			if ( frame == null )
			{
				return;
			}

			// Retrieve the form element so that you know where you
			// need to submit the data.

			Element formElement = inputElement;

			while ( formElement != null && formElement.getAttributes().getAttribute( StyleConstants.NameAttribute ) != HTML.Tag.FORM )
			{
				formElement = formElement.getParentElement();
			}

			// If the form element is null, then there was no
			// enclosing form for the <INPUT> tag, so you can
			// return, doing nothing.

			if ( formElement == null )
			{
				return;
			}

			// Now that you know you have a form element,
			// get the action field, attach the data, and
			// refresh the appropriate request frame.

			String action = (String) formElement.getAttributes().getAttribute( HTML.Attribute.ACTION );

			// If there is no action, how do we know which page to
			// connect to?  We assume it's the originating page.

			if ( action == null )
			{
				action = frame.getCurrentLocation();
			}

			// Now get the data fields we will submit to this form

			String[] elements = data.split( "&" );
			String[] fields = new String[ elements.length ];

			if ( elements[ 0 ].length() > 0 )
			{
				for ( int i = 0; i < elements.length; ++i )
				{
					fields[ i ] = elements[ i ].substring( 0, elements[ i ].indexOf( "=" ) );
				}
			}
			else
			{
				fields[ 0 ] = "";
			}

			// Prepare the element string -- make sure that
			// you don't have duplicate fields.

			for ( int i = 0; i < elements.length; ++i )
			{
				for ( int j = i + 1; j < elements.length; ++j )
				{
					if ( elements[ i ] != null && elements[ j ] != null && fields[ i ].equals( fields[ j ] ) )
					{
						elements[ j ] = null;
					}
				}
			}

			GenericRequest formSubmitter = new GenericRequest( "" );

			if ( action.indexOf( "?" ) != -1 )
			{
				// For quirky URLs where there's a question mark
				// in the middle of the URL, just string the data
				// onto the URL.  This is the way browsers work,
				// so it's the way KoL expects the data.

				StringBuilder actionString = new StringBuilder();
				actionString.append( action );

				for ( int i = 0; i < elements.length; ++i )
				{
					if ( elements[ i ] != null )
					{
						actionString.append( '&' );
						actionString.append( elements[ i ] );
					}
				}

				formSubmitter.constructURLString( GenericRequest.decodeField( actionString.toString(), "ISO-8859-1" ) );
			}
			else
			{
				// For normal URLs, the form data can be submitted
				// just like in every other request.

				formSubmitter.constructURLString( action );
				if ( elements[ 0 ].length() > 0 )
				{
					for ( int i = 0; i < elements.length; ++i )
					{
						if ( elements[ i ] != null )
						{
							formSubmitter.addEncodedFormField( elements[ i ] );
						}
					}
				}
			}

			frame.refresh( formSubmitter );
		}

		private RequestFrame findFrame()
		{
			// Goal: find the RequestFrame that contains the RequestPane that
			// contains the HTML field containing this form submit button.
			// Original solution: enumerate all Frames, choose the one containing
			// text that matches the button name.  This broke in the presence
			// of HTML entities, and wasn't guaranteed to be unique anyway.
			// Try 1: enumerate enclosing containers until an instance of
			// RequestFrame is found.  This works for standalone windows, but
			// not for frames that open in a tab because they aren't actually
			// part of the containment hierarchy in that case - the contentPane
			// of the frame gets reparented into the main tabs.
			// Try 2: enumerate containers to find the RequestPane, enumerate
			// Frames to find the RequestFrame that owns it.

			Container c = this.getContainer();
			while ( c != null && !(c instanceof RequestPane) )
			{
				c = c.getParent();
			}

			Frame[] frames = Frame.getFrames();
			for ( int i = 0; i < frames.length; ++i )
			{
				if ( frames[ i ] instanceof RequestFrame &&
					((RequestFrame) frames[ i ]).mainDisplay == c )
				{
					return (RequestFrame) frames[ i ];
				}
			}
			return null;
		}
	}

	/**
	 * Utility method used to determine the GenericRequest that should be sent, given the appropriate location.
	 */

	public static final GenericRequest extractRequest( String location )
	{
		if ( location.indexOf( "pics.communityofloathing.com" ) != -1 )
		{
			FileUtilities.downloadImage( location );
			location = location.substring( location.indexOf( "/" ) );

			GenericRequest extractedRequest = new GenericRequest( location );
			extractedRequest.responseCode = 200;
			extractedRequest.responseText = "<html><img src=\"" + location + "\"></html>";
			return extractedRequest;
		}

		return new GenericRequest( location );
	}

	/**
	 * Utility method used to deselect current selection and select a new
	 * one
	 */

	public static final void selectOption( final StringBuffer buffer, final String select, final String option )
	{
		// Find the correct select within the html
		int start = buffer.indexOf( "<select name=" + select + ">" );
		if ( start < 0)
		{
			return;
		}
		int end = buffer.indexOf( "</select>", start );
		if ( end < 0)
		{
			return;
		}

		// Delete currently selected items
		int index = start;
		while (true)
		{
			index = buffer.indexOf( " selected", index );
			if ( index == -1 || index >= end )
			{
				break;
			}
			buffer.delete( index, index + 9 );
		}

		// Select desired item
		String selector = "value=" + option;
		end = buffer.indexOf( "</select>", start );
		index = buffer.indexOf( selector + ">", start );
		if ( index == -1  || index >= end )
		{
			return;
		}

		buffer.insert( index + selector.length(), " selected" );
	}
}
