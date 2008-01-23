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

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
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

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.webui.BasementDecorator;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;
import net.sourceforge.kolmafia.webui.UseLinkDecorator;
import net.sourceforge.kolmafia.webui.ValhallaDecorator;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MoonPhaseRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class RequestEditorKit
	extends HTMLEditorKit
{
	private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

	/**
	 * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts some of the form handling to
	 * ensure that <code>GenericRequest</code> objects are instantiated on form submission rather than the
	 * <code>HttpRequest</code> objects created by the default HTML editor kit.
	 */

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

	private static final Pattern FILEID_PATTERN = Pattern.compile( "(\\d+)\\." );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	private static final Pattern BOOKSHELF_PATTERN =
		Pattern.compile( "onClick=\"location.href='(.*?)';\"", Pattern.DOTALL );
	private static final Pattern ALTAR_PATTERN = Pattern.compile( "'An altar with a carving of a god of ([^']*)'" );

	public static final void downloadFile( final String remote, final File local )
	{
		if ( local.exists() )
		{
			return;
		}

		try
		{
			URLConnection connection = ( new URL( null, remote ) ).openConnection();
			if ( remote.startsWith( "http://pics.communityofloathing.com" ) )
			{
				Matcher idMatcher = RequestEditorKit.FILEID_PATTERN.matcher( local.getPath() );
				if ( idMatcher.find() )
				{
					connection.setRequestProperty(
						"Referer", "http://www.kingdomofloathing.com/showplayer.php?who=" + idMatcher.group( 1 ) );
				}
			}

			BufferedInputStream in = new BufferedInputStream( connection.getInputStream() );
			ByteArrayOutputStream outbytes = new ByteArrayOutputStream();

			byte[] buffer = new byte[ 4096 ];

			int offset;
			while ( ( offset = in.read( buffer ) ) > 0 )
			{
				outbytes.write( buffer, 0, offset );
			}

			in.close();

			// If it's textual data, then go ahead and modify it so
			// that all the variables point to KoLmafia.

			if ( remote.endsWith( ".js" ) )
			{
				String text = outbytes.toString();
				outbytes.reset();

				text = StaticEntity.globalStringReplace( text, "location.hostname", "location.host" );
				outbytes.write( text.getBytes() );
			}

			FileOutputStream outfile = new FileOutputStream( local );
			outbytes.writeTo( outfile );
			outfile.close();
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.
		}
	}

	/**
	 * Downloads the given file from the KoL images server and stores it locally.
	 */

	public static final URL downloadImage( final String filename )
	{
		if ( filename == null || filename.equals( "" ) )
		{
			return null;
		}

		String localname = filename.substring( filename.indexOf( "/", "http://".length() ) + 1 );
		if ( localname.startsWith( "albums/" ) )
		{
			localname = localname.substring( "albums/".length() );
		}

		File localfile = new File( UtilityConstants.IMAGE_LOCATION, localname );
		File parentfile =
			new File( UtilityConstants.IMAGE_LOCATION, localname.substring( 0, localname.lastIndexOf( "/" ) + 1 ) );

		if ( !parentfile.exists() )
		{
			parentfile.mkdirs();
		}

		if ( !localfile.exists() || localfile.length() == 0 )
		{
			if ( JComponentUtilities.getImage( localname ) != null )
			{
				StaticEntity.loadLibrary( UtilityConstants.IMAGE_LOCATION, UtilityConstants.IMAGE_DIRECTORY, localname );
			}
			else
			{
				RequestEditorKit.downloadFile( filename, localfile );
			}
		}

		try
		{
			return localfile.toURI().toURL();
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.

			return null;
		}
	}

	private static class KoLImageView
		extends ImageView
	{
		public KoLImageView( final Element elem )
		{
			super( elem );
		}

		public URL getImageURL()
		{
			String src = (String) this.getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
			{
				return null;
			}

			return RequestEditorKit.downloadImage( src );
		}
	}

	/**
	 * Utility method which converts the given text into a form which can be displayed properly in a
	 * <code>RequestPane</code>. This method is necessary primarily due to the bad HTML which is used but can still
	 * be properly rendered by post-3.2 browsers.
	 */

	public static final String getDisplayHTML( final String location, final String responseText )
	{
		if ( responseText == null || responseText.length() == 0 )
		{
			return "";
		}

		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		RequestLogger.updateDebugLog( "Rendering hypertext..." );
		String displayHTML = RequestEditorKit.getFeatureRichHTML( location, responseText, false );

		displayHTML =
			KoLConstants.LINE_BREAK_PATTERN.matcher(
				KoLConstants.COMMENT_PATTERN.matcher(
					KoLConstants.STYLE_PATTERN.matcher(
						KoLConstants.SCRIPT_PATTERN.matcher( displayHTML ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll(
					"" ) ).replaceAll( "" ).replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll(
				"<[Hh][Rr].*?>", "<br>" );

		// The default Java browser doesn't display blank lines correctly

		displayHTML = displayHTML.replaceAll( "<br><br>", "<br>&nbsp;<br>" );

		// Fix all the tables which decide to put a row end,
		// but no row beginning.

		displayHTML = displayHTML.replaceAll( "</tr><td", "</tr><tr><td" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML =
			displayHTML.replaceAll( "font-size: .8em;", "" ).replaceAll( "<font size=[12]>", "" ).replaceAll(
				" class=small", "" ).replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML =
			displayHTML.replaceAll(
				"<td valign=center><table[^>]*?><tr><td([^>]*?) bgcolor=black([^>]*?)>.*?</table></td>", "" );
		displayHTML = displayHTML.replaceAll( "<tr[^>]*?><td[^>]*bgcolor=\'?\"?black(.*?)</tr>", "" );
		displayHTML = displayHTML.replaceAll( "<table[^>]*title=.*?</table>", "" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)?(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\([\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML =
			displayHTML.replaceAll(
				"<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>", "</style>" );

		// Image links are mangled a little bit because they use
		// Javascript now -- fix them.

		displayHTML =
			displayHTML.replaceAll(
				"<img([^>]*?) onClick=\'descitem\\((\\d+)\\);\'>",
				"<a href=\"desc_item.php?whichitem=$2\"><img$1 border=0></a>" );

		// The last thing to worry about is the problems in
		// specific pages.

		// The first of these is the familiar page, where the
		// first "Take this one with you" link does not work.
		// On a related note, the sewer page suffers from an
		// odd problem as well, where you must remove cells in
		// the table before it works.

		displayHTML =
			displayHTML.replaceFirst( "<input class=button type=submit value=\"Take this one with you\">", "" );
		displayHTML =
			displayHTML.replaceFirst(
				"</td></tr><tr><td align=center>(<input class=button type=submit value='Take Items'>)", "$1" );

		// The second of these is the betting page.  Here, the
		// problem is an "onClick" in the input field, if the
		// Hagnk option is available.

		if ( displayHTML.indexOf( "whichbet" ) != -1 )
		{
			// Since the introduction of MMG bots, bets are usually
			// placed and taken instantaneously.  Therefore, the
			// search form is extraneous.

			displayHTML = displayHTML.replaceAll( "<center><b>Search.*?<center>", "<center>" );

			// Also, placing a bet is awkward through the KoLmafia
			// interface.  Remove this capability.

			displayHTML = displayHTML.replaceAll( "<center><b>Add.*?</form><br>", "<br>" );

			// Checkboxes were a safety which were added server-side,
			// but they do not really help anything and Java is not
			// very good at rendering them -- remove it.

			displayHTML = displayHTML.replaceFirst( "\\(confirm\\)", "" );
			displayHTML =
				displayHTML.replaceAll(
					"<input type=checkbox name=confirm>", "<input type=hidden name=confirm value=on>" );

			// In order to avoid the problem of having two submits,
			// which confuses the built-in Java parser, remove one
			// of the buttons and leave the one that makes sense.

			if ( KoLCharacter.canInteract() )
			{
				displayHTML =
					displayHTML.replaceAll(
						"whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
						"whichbet value='$1'><input type=hidden name=from value=0><input class=button type=submit value=\"On Hand\"><input type=hidden" );
			}
			else
			{
				displayHTML =
					displayHTML.replaceAll(
						"whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
						"whichbet value='$1'><input type=hidden name=from value=1><input class=button type=submit value=\"In Hagnk's\"><input type=hidden" );
			}
		}

		// The third of these is the outfit managing page,
		// which requires that the form for the table be
		// on the outside of the table.

		if ( displayHTML.indexOf( "action=account_manageoutfits.php" ) != -1 )
		{
			// turn:  <center><table><form>...</center></td></tr></form></table>
			// into:  <form><center><table>...</td></tr></table></center></form>

			displayHTML = displayHTML.replaceAll( "<center>(<table[^>]*>)(<form[^>]*>)", "$2<center>$1" );
			displayHTML =
				displayHTML.replaceAll( "</center></td></tr></form></table>", "</td></tr></table></center></form>" );
		}

		// The fourth of these is the fight page, which is
		// totally mixed up -- in addition to basic modifications,
		// also resort the combat item list.

		if ( displayHTML.indexOf( "action=fight.php" ) != -1 )
		{
			displayHTML = displayHTML.replaceAll( "<form(.*?)<tr><td([^>]*)>", "<tr><td$2><form$1" );
			displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );
		}

		// Doc Galaktik's page is going to get completely
		// killed, except for the main purchases.

		if ( displayHTML.indexOf( "action=galaktik.php" ) != -1 )
		{
			displayHTML =
				StaticEntity.globalStringReplace( displayHTML, "</tr><td valign=center>", "</tr><tr><td valign=center>" );
			displayHTML = StaticEntity.globalStringReplace( displayHTML, "<td>", "</td><td>" );
			displayHTML = StaticEntity.globalStringReplace( displayHTML, "</td></td>", "</td>" );

			displayHTML =
				displayHTML.replaceAll(
					"<table><table>(.*?)(<form action=galaktik\\.php method=post><input[^>]+><input[^>]+>)",
					"<table><tr><td>$2<table>$1<tr>" );
		}

		// The library bookshelf has some secretive Javascript
		// which needs to be removed.

		displayHTML = RequestEditorKit.BOOKSHELF_PATTERN.matcher( displayHTML ).replaceAll( "href=\"$1\"" );

		// All HTML is now properly rendered!  Return the
		// compiled string.  Print it to the debug log for
		// reference purposes.

		RequestLogger.updateDebugLog( displayHTML );
		return displayHTML;
	}

	public static final String getFeatureRichHTML( final String location, final String text,
		final boolean addComplexFeatures )
	{
		if ( text == null || text.length() == 0 )
		{
			return "";
		}

		StringBuffer buffer = new StringBuffer( text );
		RequestEditorKit.getFeatureRichHTML( location, buffer, addComplexFeatures );
		return buffer.toString();
	}

	private static final String NO_HERMIT_TEXT =
		"<img src=\"http://images.kingdomofloathing.com/otherimages/mountains/mount4.gif\" width=100 height=100>";
	private static final String AUTO_HERMIT_TEXT =
		"<a href=\"hermit.php?autopermit=on\"><img src=\"http://images.kingdomofloathing.com/otherimages/mountains/hermitage.gif\" width=100 height=100 border=0></a>";

	private static final String NO_PERMIT_TEXT =
		"<p>You don't have a Hermit Permit, so you're not allowed to visit the Hermit.<p><center>";
	private static final String BUY_PERMIT_TEXT =
		RequestEditorKit.NO_PERMIT_TEXT + "<a href=\"hermit.php?autopermit=on\">Buy a Hermit Permit</a></center></p><p><center>";

	private static final ArrayList maps = new ArrayList();
	static
	{
		RequestEditorKit.maps.add( "plains.php" );
		RequestEditorKit.maps.add( "plains2.php" );
		RequestEditorKit.maps.add( "bathole.php" );
		RequestEditorKit.maps.add( "fernruin.php" );
		RequestEditorKit.maps.add( "knob.php" );
		RequestEditorKit.maps.add( "knob2.php" );
		RequestEditorKit.maps.add( "cyrpt.php" );
		RequestEditorKit.maps.add( "beanstalk.php" );
		RequestEditorKit.maps.add( "woods.php" );
		RequestEditorKit.maps.add( "friars.php" );
		RequestEditorKit.maps.add( "wormwood.php" );
		RequestEditorKit.maps.add( "mountains.php" );
		RequestEditorKit.maps.add( "mclargehuge.php" );
		RequestEditorKit.maps.add( "island.php" );
		RequestEditorKit.maps.add( "bigisland.php" );
		RequestEditorKit.maps.add( "postwarisland.php" );
		RequestEditorKit.maps.add( "beach.php" );
		RequestEditorKit.maps.add( "pyramid.php" );
		RequestEditorKit.maps.add( "town_wrong.php" );
		RequestEditorKit.maps.add( "town_right.php" );
		RequestEditorKit.maps.add( "manor.php" );
		RequestEditorKit.maps.add( "manor2.php" );
		RequestEditorKit.maps.add( "manor3.php" );
		RequestEditorKit.maps.add( "dungeons.php" );
		RequestEditorKit.maps.add( "canadia.php" );
		RequestEditorKit.maps.add( "gnomes.php" );
		RequestEditorKit.maps.add( "heydeze.php" );
	}

	public static final void getFeatureRichHTML( final String location, final StringBuffer buffer,
		final boolean addComplexFeatures )
	{
		if ( buffer.length() == 0 )
		{
			return;
		}

		if ( addComplexFeatures )
		{
			StaticEntity.singleStringReplace(
				buffer, "</head>", "<script language=\"Javascript\" src=\"/basics.js\"></script></head>" );
			if ( location.indexOf( "?" ) == -1 && RequestEditorKit.maps.contains( location ) )
			{
				buffer.insert(
					buffer.indexOf( "</tr>" ),
					"<td width=15 valign=bottom align=left bgcolor=blue><a style=\"color: white; font-weight: normal; font-size: small; text-decoration: underline\" href=\"javascript: attachSafetyText(); void(0);\">?</a>" );
				buffer.insert( buffer.indexOf( "<td", buffer.indexOf( "</tr>" ) ) + 3, " colspan=2" );
			}
		}

		// Make all the character pane adjustments first, since
		// they only happen once and they occur frequently.

		if ( location.startsWith( "charpane.php" ) )
		{
			if ( addComplexFeatures )
			{
				CharPaneDecorator.decorate( buffer );
			}

			return;
		}

		if ( location.indexOf( "menu.php" ) != -1 )
		{
			MoonPhaseRequest.decorate( buffer );
			return;
		}

		// Now handle the changes which only impact a single
		// page one at a time.

		if ( location.startsWith( "adventure.php" ) )
		{
			RequestEditorKit.fixDucks( buffer );
			StationaryButtonDecorator.decorate( location, buffer );

			if ( AdventureRequest.useMarmotClover( location, buffer.toString() ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}
		}
		else if ( location.startsWith( "ascend.php" ) )
		{
			ValhallaDecorator.decorateGashJump( buffer );
		}
		else if ( location.startsWith( "ascensionhistory.php" ) )
		{
			if ( addComplexFeatures )
			{
				StaticEntity.singleStringReplace(
					buffer, "</head>", "<script language=\"Javascript\" src=\"/sorttable.js\"></script></head>" );
				StaticEntity.singleStringReplace(
					buffer, "<table><tr><td class=small>",
					"<table class=\"sortable\" id=\"history\"><tr><td class=small>" );
				StaticEntity.globalStringReplace(
					buffer, "<tr><td colspan=9", "<tr class=\"sortbottom\" style=\"display:none\"><td colspan=9" );
				StaticEntity.globalStringReplace(
					buffer,
					"<td></td>",
					"<td><img src=\"http://images.kingdomofloathing.com/itemimages/confused.gif\" title=\"No Data\" alt=\"No Data\" height=30 width=30></td>" );
			}
		}
		else if ( location.startsWith( "barrels.php" ) )
		{
			if ( AdventureRequest.useMarmotClover( location, buffer.toString() ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}
		}
		else if ( location.startsWith( "basement.php" ) )
		{
			BasementDecorator.decorate( buffer );
		}
		else if ( location.startsWith( "bigisland.php" ) )
		{
			BigIsland.decorateBigIsland( location, buffer );
		}
		else if ( location.startsWith( "postwarisland.php" ) )
		{
			BigIsland.decoratePostwarIsland( location, buffer );
		}
		else if ( location.startsWith( "bathole.php" ) )
		{
			StaticEntity.globalStringReplace( buffer, "action=bathole.php", "action=adventure.php" );
		}
		else if ( location.startsWith( "choice.php" ) )
		{
			StationaryButtonDecorator.decorate( location, buffer );
			RequestEditorKit.addChoiceSpoilers( buffer );
		}
		else if ( location.startsWith( "fight.php" ) )
		{
			StationaryButtonDecorator.decorate( location, buffer );
			RequestEditorKit.addFightModifiers( location, buffer );
		}
		else if ( location.startsWith( "hermit.php" ) )
		{
			if ( HermitRequest.useHermitClover( location ) )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}

			StaticEntity.singleStringReplace( buffer, RequestEditorKit.NO_PERMIT_TEXT, RequestEditorKit.BUY_PERMIT_TEXT );
		}
		else if ( location.startsWith( "hiddencity.php" ) )
		{
			RequestEditorKit.addHiddenCityModifiers( buffer );
		}
		else if ( location.startsWith( "inventory.php" ) )
		{
			if ( UseItemRequest.currentItemId() == UseItemRequest.HACK_SCROLL )
			{
				StaticEntity.globalStringReplace( buffer, "ten-leaf", "disassembled" );
				StaticEntity.globalStringReplace( buffer, "clover.gif", "disclover.gif" );
				StaticEntity.globalStringReplace( buffer, "370834526", "328909735" );
			}

			if ( KoLCharacter.inMuscleSign() )
			{
				StaticEntity.globalStringReplace( buffer, "combine.php", "knoll.php?place=paster" );
			}

			AdventureResult wand = KoLCharacter.getZapper();
			if ( wand != null )
			{
				StaticEntity.singleStringReplace(
					buffer,
					"]</a></font></td></tr></table></center>",
					"]</a>&nbsp;&nbsp;<a href=\"wand.php?whichwand=" + wand.getItemId() + "\">[zap items]</a></font></td></tr></table></center>" );
			}

			RequestEditorKit.changeSphereImages( buffer );

			// Automatically name the outfit "backup" for simple save
			// purposes while adventuring in browser.

			StaticEntity.singleStringReplace(
				buffer, "<input type=text name=outfitname", "<input type=text name=outfitname value=\"Backup\"" );

			// Split the custom outfits from the normal outfits for
			// easier browsing.

			int selectBeginIndex = buffer.indexOf( "<option value=-" );
			if ( selectBeginIndex != -1 && addComplexFeatures )
			{
				int selectEndIndex = buffer.indexOf( "</select>", selectBeginIndex );
				String outfitString = buffer.substring( selectBeginIndex, selectEndIndex );
				buffer.delete( selectBeginIndex, selectEndIndex );

				int formEndIndex = buffer.indexOf( "</form>", selectBeginIndex ) + 7;

				StringBuffer customString = new StringBuffer();
				customString.append( "<tr><td align=right><form name=outfit2 action=inv_equip.php><input type=hidden name=action value=\"outfit\"><input type=hidden name=which value=2><b>Custom:</b></td><td><select name=whichoutfit><option value=0>(select a custom outfit)</option>" );
				customString.append( outfitString );
				customString.append( "</select></td><td> <input class=button type=submit value=\"Dress Up!\"></form></td></tr></table>" );
				StaticEntity.globalStringDelete( customString, "Custom: " );

				buffer.insert( formEndIndex, customString.toString() );

				StaticEntity.singleStringReplace(
					buffer, "<form name=outfit", "<table><tr><td align=right><form name=outfit" );
				StaticEntity.singleStringReplace( buffer, "<select", "</td><td><select" );
				StaticEntity.singleStringReplace( buffer, "</select>", "</select></td><td>" );
				StaticEntity.singleStringReplace( buffer, "</form>", "</form></td></tr>" );

				StaticEntity.globalStringReplace( buffer, "<select", "<select style=\"width: 250px\"" );
			}
		}
		else if ( location.startsWith( "lair6.php" ) )
		{
			if ( buffer.indexOf( "ascend.php" ) != -1 )
			{
				KoLCharacter.liberateKing();
			}
		}
		else if ( location.indexOf( "lchat.php" ) != -1 )
		{
			StaticEntity.globalStringDelete( buffer, "spacing: 0px;" );
			StaticEntity.globalStringReplace( buffer, "cycles++", "cycles = 0" );
			StaticEntity.globalStringReplace( buffer, "location.hostname", "location.host" );

			StaticEntity.singleStringReplace(
				buffer, "if (postedgraf",
				"if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } if (postedgraf" );

			// This is a hack to fix KoL chat as handled in earlier
			// versions of Opera (doubled chat).

			StaticEntity.singleStringReplace(
				buffer, "http.onreadystatechange", "executed = false; http.onreadystatechange" );
			StaticEntity.singleStringReplace(
				buffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}
		else if ( location.startsWith( "manor3.php" ) )
		{
			RequestEditorKit.addWineCellarSpoilers( buffer );
		}
		else if ( location.startsWith( "mountains.php" ) )
		{
			StaticEntity.singleStringReplace(
				buffer, RequestEditorKit.NO_HERMIT_TEXT, RequestEditorKit.AUTO_HERMIT_TEXT );
		}
		else if ( location.startsWith( "multiuse.php" ) )
		{
			RequestEditorKit.addMultiuseModifiers( buffer );
		}
		else if ( location.startsWith( "palinshelves.php" ) )
		{
			StaticEntity.singleStringReplace(
				buffer, "</html>", "<script language=\"Javascript\" src=\"/palinshelves.js\" /></html>" );
		}
		else if ( location.startsWith( "pvp.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, "value=rank checked", "value=rank" );
			StaticEntity.singleStringReplace( buffer, "value=flowers", "value=flowers checked" );
		}
		else if ( location.startsWith( "rats.php" ) )
		{
			RequestEditorKit.addTavernSpoilers( buffer );
		}
		else if ( location.startsWith( "searchplayer.php" ) )
		{
			StaticEntity.singleStringReplace( buffer, "name=pvponly", "name=pvponly checked" );
			StaticEntity.singleStringReplace( buffer, "value=0 checked", "value=0" );

			if ( KoLCharacter.isHardcore() )
			{
				StaticEntity.singleStringReplace( buffer, "value=1", "value=1 checked" );
			}
			else
			{
				StaticEntity.singleStringReplace( buffer, "value=2", "value=2 checked" );
			}
		}
		else if ( location.startsWith( "valhalla.php" ) )
		{
			ValhallaDecorator.decorateAfterLife( location, buffer );
		}
		else if ( location.startsWith( "wand.php" ) )
		{
			ZapRequest.decorate( buffer );
		}

		// Always select the contents of text fields when you click on them
		// to make for easy editing.

		if ( addComplexFeatures )
		{
			// Now handle all the changes which happen on a lot of
			// different pages rather than just one or two.

			RequestEditorKit.changePotionImages( buffer );

			if ( Preferences.getBoolean( "relayAddsUseLinks" ) )
			{
				UseLinkDecorator.decorate( location, buffer );
			}

			if ( buffer.indexOf( "showplayer.php" ) != -1 && buffer.indexOf( "rcm.js" ) == -1 && buffer.indexOf( "rcm.2.js" ) == -1 )
			{
				RequestEditorKit.addChatFeatures( buffer );
			}

			StaticEntity.globalStringReplace( buffer, "type=text ", "type=text onFocus='this.select();' " );
		}

		String defaultColor = Preferences.getString( "defaultBorderColor" );
		if ( !defaultColor.equals( "blue" ) )
		{
			StaticEntity.globalStringReplace( buffer, "bgcolor=blue", "bgcolor=\"" + defaultColor + "\"" );
			StaticEntity.globalStringReplace( buffer, "border: 1px solid blue", "border: 1px solid " + defaultColor );
		}
	}

	public static final void addChatFeatures( final StringBuffer buffer )
	{
		StaticEntity.singleStringReplace(
			buffer,
			"</head>",
			"<script language=\"Javascript\"> var " + ChatRequest.getRightClickMenu() + " </script>" + "<script language=\"Javascript\" src=\"/images/scripts/rcm.2.js\"></script></head>" );

		StaticEntity.singleStringReplace( buffer, "</body>", "<div id='menu' class='rcm'></div></body>" );
	}

	private static final void addFightModifiers( final String location, final StringBuffer buffer )
	{
		// Change bang potion names in item dropdown
		RequestEditorKit.changePotionNames( buffer );

		// Change stone sphere names in item dropdown
		RequestEditorKit.changeSphereNames( buffer );

		int combatIndex = buffer.indexOf( "!</b>" );
		if ( combatIndex != -1 )
		{
			buffer.insert( combatIndex, ": Round " + FightRequest.getCurrentRound() );
		}

		// Add monster data HP/Atk/Def and item drop data
		RequestEditorKit.annotateMonster( buffer );

		switch ( KoLAdventure.lastAdventureId() )
		{
		case 126: // The Themthar Hills
			BigIsland.decorateThemtharFight( buffer );
			break;

		case 139: // Wartime Junkyard
			// Quest gremlins might have a tool.
			BigIsland.decorateGremlinFight( buffer );
			break;

		case 132: // Battlefield (Frat Uniform)
		case 140: // Battlefield (Hippy Uniform)
			BigIsland.decorateBattlefieldFight( buffer );
			break;
		}
	}

	private static final void annotateMonster( final StringBuffer buffer )
	{
		if ( FightRequest.getLastMonster() == null )
		{
			return;
		}

		// Don't show monster unless we know combat stats or items
		if ( FightRequest.getLastMonster().getHP() == 0 && FightRequest.getLastMonster().getItems().isEmpty() )
		{
			return;
		}

		int nameIndex = buffer.indexOf( "<span id='monname" );
		if ( nameIndex == -1 )
		{
			return;
		}

		int combatIndex = buffer.indexOf( "</span>", nameIndex );
		if ( combatIndex == -1 )
		{
			return;
		}

		StringBuffer monsterData = new StringBuffer();
		monsterData.append( "<font size=2 color=gray>" );
		if ( FightRequest.getLastMonster().getHP() != 0 )
		{
			monsterData.append( "<br />HP: " );
			monsterData.append( FightRequest.getMonsterHealth() );
			monsterData.append( ", Atk: " );
			monsterData.append( FightRequest.getMonsterAttack() );
			monsterData.append( ", Def: " );
			monsterData.append( FightRequest.getMonsterDefense() );
		}

		List items = FightRequest.getLastMonster().getItems();
		if ( !items.isEmpty() )
		{
			monsterData.append( "<br />Drops: " );
			for ( int i = 0; i < items.size(); ++i )
			{
				if ( i != 0 )
				{
					monsterData.append( ", " );
				}
				monsterData.append( items.get( i ) );
			}
		}

		BigIsland.appendMissingGremlinTool( monsterData );
		monsterData.append( "</font>" );
		buffer.insert( combatIndex + 7, monsterData.toString() );
	}

	private static final void addMultiuseModifiers( final StringBuffer buffer )
	{
		// Change bang potion names in item dropdown
		RequestEditorKit.changePotionNames( buffer );
	}

	private static final void addWineCellarSpoilers( final StringBuffer buffer )
	{
		// Change dusty bottle names in item dropdown
		RequestEditorKit.changeDustyBottleNames( buffer );
	}

	private static final void changePotionImages( final StringBuffer buffer )
	{
		KoLCharacter.ensureUpdatedPotionEffects();

		if ( buffer.indexOf( "exclam.gif" ) == -1 )
		{
			return;
		}

		ArrayList potionNames = new ArrayList();
		ArrayList potionEffects = new ArrayList();

		for ( int i = 819; i <= 827; ++i )
		{
			String name = ItemDatabase.getItemName( i );
			if ( buffer.indexOf( name ) != -1 )
			{
				String effect = Preferences.getString( "lastBangPotion" + i );
				if ( !effect.equals( "" ) )
				{
					potionNames.add( name );
					potionEffects.add( effect );
				}
			}
		}

		if ( potionNames.isEmpty() )
		{
			return;
		}

		for ( int i = 0; i < potionNames.size(); ++i )
		{
			String name = (String) potionNames.get( i );
			String effect = (String) potionEffects.get( i );

			StaticEntity.singleStringReplace( buffer, name + "</b>", name + " of " + effect + "</b>" );
			StaticEntity.singleStringReplace( buffer, name + "s</b>", name + "s of " + effect + "</b>" );
		}
	}

	private static final void changePotionNames( final StringBuffer buffer )
	{
		KoLCharacter.ensureUpdatedPotionEffects();

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

				StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
				// Pluralize correctly
				StaticEntity.globalStringReplace( buffer, name + " of " + effect + "s", name + "s of " + effect );
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
		glyphs[ 0 ] = StaticEntity.parseInt( matcher.group( 1 ) );

		if ( !matcher.find() )
		{
			return;
		}
		glyphs[ 1 ] = StaticEntity.parseInt( matcher.group( 1 ) );

		if ( !matcher.find() )
		{
			return;
		}
		glyphs[ 2 ] = StaticEntity.parseInt( matcher.group( 1 ) );

		int wines[] = new int[ 3 ];

		for ( int i = 2271; i <= 2276; ++i )
		{
			int glyph = Preferences.getInteger( "lastDustyBottle" + i );
			for ( int j = 0; j < 3; ++j )
			{
				if ( glyph == glyphs[ j ] )
				{
					wines[ j ] = i;
					break;
				}
			}
		}

		for ( int i = 0; i < 3; ++i )
		{
			if ( wines[i] == 0 )
				continue;
			String name = ItemDatabase.getItemName( wines[ i ] );
			StaticEntity.globalStringReplace( buffer, name, i + 1 + " " + name );
		}
	}

	private static final void changeSphereImages( final StringBuffer buffer )
	{
		KoLCharacter.ensureUpdatedSphereEffects();

		RequestEditorKit.changeSphereImage( buffer, "spheremoss.gif", 2174 );
		RequestEditorKit.changeSphereImage( buffer, "spheresmooth.gif", 2175 );
		RequestEditorKit.changeSphereImage( buffer, "spherecrack.gif", 2176 );
		RequestEditorKit.changeSphereImage( buffer, "sphererough.gif", 2177 );
	}

	private static final void changeSphereImage( final StringBuffer buffer, final String image, final int itemId )
	{
		if ( buffer.indexOf( image ) == -1 )
		{
			return;
		}

		String name = ItemDatabase.getItemName( itemId );
		if ( buffer.indexOf( name ) == -1 )
		{
			return;
		}

		String effect = Preferences.getString( "lastStoneSphere" + itemId );
		if ( effect.equals( "" ) )
		{
			return;
		}

		StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
	}

	private static final void addHiddenCityModifiers( final StringBuffer buffer )
	{
		// Change stone sphere names in item dropdown
		RequestEditorKit.changeSphereNames( buffer );

		// If we are at an altar, select the correct stone sphere
		Matcher matcher = RequestEditorKit.ALTAR_PATTERN.matcher( buffer.toString() );
		if ( matcher.find() )
		{
			String domain = matcher.group(1);
			String itemId = FightRequest.stoneSphereEffectToId( domain );
			if ( itemId != null )
			{
				String find = "<option value='" + itemId + "'";
				StaticEntity.singleStringReplace( buffer, find, find + " selected" );
			}
		}

	}

	private static final void changeSphereNames( final StringBuffer buffer )
	{
		KoLCharacter.ensureUpdatedSphereEffects();
		for ( int i = 2174; i <= 2177; ++i )
		{
			String name = ItemDatabase.getItemName( i );
			String effect = Preferences.getString( "lastStoneSphere" + i );

			if ( buffer.indexOf( name ) != -1 && !effect.equals( "" ) )
			{
				StaticEntity.globalStringReplace( buffer, name, name + " of " + effect );
			}
		}
	}

	private static final void addChoiceSpoilers( final StringBuffer buffer )
	{
		// For the plus sign teleportitis adventure, replace the book
		// message with a link to the plus sign.

		StaticEntity.singleStringReplace(
			buffer, "It's actually a book.  Read it.",
			"It's actually a book. <font size=1>[<a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=818\">read it</a>]</font>" );

		// For everything else, make sure that it's an actual choice adventure
		Matcher choiceMatcher = RequestEditorKit.CHOICE_PATTERN.matcher( buffer.toString() );
		if ( !choiceMatcher.find() )
		{
			return;
		}

		// Find the options for the choice we've encountered
		int choice = StaticEntity.parseInt( choiceMatcher.group( 1 ) );
		String[][] possibleDecisions = AdventureDatabase.choiceSpoilers( choice );

		if ( possibleDecisions == null )
		{
			return;
		}

		int index1 = 0, index2 = 0;

		String text = buffer.toString();
		buffer.setLength( 0 );

		for ( int i = 0; i < possibleDecisions[ 2 ].length; ++i )
		{
			index2 = text.indexOf( "</form>", index1 );

			// If KoL says we've run out of choices, quit now
			if ( index2 == -1 )
			{
				break;
			}

			// Start spoiler text
			buffer.append( text.substring( index1, index2 ) );
			buffer.append( "<br><font size=-1>(" );

			// Say what the choice will give you
			String item = AdventureDatabase.choiceSpoiler( choice, i, possibleDecisions[ 2 ] );
			buffer.append( item );

			// If this choice helps complete an outfit...
			if ( possibleDecisions.length > 3 )
			{
				String itemId = possibleDecisions[ 3 ][ i ];

				// If this decision leads to an item...
				if ( itemId != null )
				{
					// List # in inventory
					buffer.append( " - " );
					AdventureResult result = new AdventureResult( StaticEntity.parseInt( itemId ), 1 );

					int available = KoLCharacter.hasEquipped( result ) ? 1 : 0;
					available += result.getCount( KoLConstants.inventory );

					buffer.append( available );
					buffer.append( " in inventory" );
				}
			}

			// Finish spoiler text
			buffer.append( ")</font></form>" );
			index1 = index2 + 7;
		}

		buffer.append( text.substring( index1 ) );
	}

	private static final void addTavernSpoilers( final StringBuffer buffer )
	{
		String text = buffer.toString();
		buffer.setLength( 0 );

		String layout = Preferences.getString( "tavernLayout" );

		for ( int i = 1; i <= 25; ++i )
		{
			int squareType = Character.digit( layout.charAt( i - 1 ), 10 );

			switch ( squareType )
			{
			case 0:
				break;

			case 1:
				text =
					text.replaceFirst(
						"(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
						" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/rat.gif\" border=0></a>" );
				break;

			case 2:
				text =
					text.replaceFirst(
						"(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
						" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/otherimages/sigils/fratboy.gif\" border=0></a>" );
				break;

			case 3:
				text =
					text.replaceFirst(
						"(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
						" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/faucet.gif\" height=60 width=60 border=0></a>" );
				break;

			case 4:
				text =
					text.replaceFirst(
						"(><a href=\"rats\\.php\\?where=" + i + "\">).*?</a>",
						" align=center valign=center$1<img src=\"http://images.kingdomofloathing.com/adventureimages/ratsworth.gif\" height=60 width=60 border=0></a>" );
				break;
			}
		}

		buffer.append( text );
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

		String war = Preferences.getString( "warProgress" );
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

	private static class KoLSubmitView
		extends FormView
	{
		public KoLSubmitView( final Element elem )
		{
			super( elem );
		}

		public Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && ( c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox ) )
			{
				c.setBackground( Color.white );
			}

			return c;
		}

		public void submitData( final String data )
		{
			// Get the element

			Element inputElement = this.getElement();

			if ( inputElement == null )
			{
				return;
			}

			// Get the "value" associated with this input

			String value = (String) inputElement.getAttributes().getAttribute( HTML.Attribute.VALUE );

			// If there is no value, we won't be able to find the
			// frame that handles this form.

			if ( value == null )
			{
				return;
			}

			// Retrieve the frame which is being used by this form
			// viewer.

			RequestFrame frame = this.findFrame( value );

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

				StringBuffer actionString = new StringBuffer();
				actionString.append( action );

				for ( int i = 0; i < elements.length; ++i )
				{
					if ( elements[ i ] != null )
					{
						actionString.append( '&' );
						actionString.append( elements[ i ] );
					}
				}

				try
				{
					formSubmitter.constructURLString( URLDecoder.decode( actionString.toString(), "ISO-8859-1" ) );
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
					formSubmitter.constructURLString( actionString.toString() );
				}
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

		private RequestFrame findFrame( final String value )
		{
			KoLFrame[] frames = StaticEntity.getExistingFrames();
			String search = "value=\"" + value + "\"";

			for ( int i = 0; i < frames.length; ++i )
			{
				if ( frames[ i ] instanceof RequestFrame && ( (RequestFrame) frames[ i ] ).containsText( search ) )
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
			RequestEditorKit.downloadImage( location );
			location = location.substring( location.indexOf( "/" ) );

			GenericRequest extractedRequest = new GenericRequest( location );
			extractedRequest.responseCode = 200;
			extractedRequest.responseText = "<html><img src=\"" + location + "\"></html>";
			extractedRequest.responseText = extractedRequest.responseText;
			return extractedRequest;
		}

		return new GenericRequest( location );
	}
}
