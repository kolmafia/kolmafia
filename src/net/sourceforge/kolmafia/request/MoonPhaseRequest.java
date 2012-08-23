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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoonPhaseRequest
	extends GenericRequest
{
	private static final Pattern MOONS_PATTERN = Pattern.compile( "moon(.)[ab]?\\.gif.*moon(.)[ab]?\\.gif" );
	private static final Pattern MENU1_PATTERN = Pattern.compile( "(<select name=\"loc\".*?)</select>", Pattern.DOTALL );
	private static final Pattern MENU2_PATTERN = Pattern.compile( "(<select name=location.*?)</select>", Pattern.DOTALL );

	/**
	 * The phases of the moons can be retrieved from the top menu, which
	 * varies based on whether or not the player is using compact mode.
	 */

	public MoonPhaseRequest()
	{
		super( "topmenu.php" );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	/**
	 * Runs the moon phase request, updating theas appropriate.
	 */

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Synchronizing moon data..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		String parseText = this.responseText;

		// Get current phase of Ronald and Grimace
		if ( parseText.indexOf( "minimoon" ) != -1 )
		{
			parseText = parseText.replaceAll( "minimoon", "" );
		}

		Matcher moonMatcher = MoonPhaseRequest.MOONS_PATTERN.matcher( parseText );
		if ( moonMatcher.find() )
		{
			HolidayDatabase.setMoonPhases(
				StringUtilities.parseInt( moonMatcher.group( 1 ) ) - 1,
				StringUtilities.parseInt( moonMatcher.group( 2 ) ) - 1 );
		}

		KoLCharacter.setClan( this.responseText.indexOf( "clan_hall.php" ) != -1 );
	}

	public static final void decorate( final StringBuffer buffer )
	{
		if ( GenericRequest.topMenuStyle == GenericRequest.MENU_COMPACT )
		{
			MoonPhaseRequest.adjustCompactMenu( buffer );
			StringUtilities.singleStringReplace( buffer, "parent.location.href=\"logout.php", "parent.location.href=\"/KoLmafia/logout?pwd=" + GenericRequest.passwordHash );
		}
		else
		{
			StringUtilities.singleStringReplace( buffer, "logout.php", "/KoLmafia/logout?pwd=" + GenericRequest.passwordHash );
		}
	}

	public static final void adjustCompactMenu( final StringBuffer buffer )
	{
		MoonPhaseRequest.mafiatizeFunctionMenu( buffer );
		MoonPhaseRequest.mafiatizeGotoMenu( buffer );

		// Now kill off the weird focusing problems inherent in
		// the Javascript.

		StringUtilities.globalStringReplace(
			buffer, "selectedIndex=0;", "selectedIndex=0; if ( parent && parent.mainpane ) parent.mainpane.focus();" );

		if ( Preferences.getBoolean( "relayAddsQuickScripts" ) )
		{
			StringBuffer selectBuffer = new StringBuffer();
			selectBuffer.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><form name=\"gcli\">" );
			selectBuffer.append( "<select id=\"scriptbar\">" );

			String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );
			for ( int i = 0; i < scriptList.length; ++i )
			{
				selectBuffer.append( "<option value=\"" );
				selectBuffer.append( scriptList[ i ] );
				selectBuffer.append( "\">" );
				selectBuffer.append( i + 1 );
				selectBuffer.append( ": " );
				selectBuffer.append( scriptList[ i ] );
				selectBuffer.append( "</option>" );
			}

			selectBuffer.append( "</select></td><td>&nbsp;</td><td>" );
			selectBuffer.append( "<input type=\"button\" class=\"button\" value=\"exec\" onClick=\"" );

			selectBuffer.append( "var script = document.getElementById( 'scriptbar' ).value; " );
			selectBuffer.append( "parent.charpane.location = '/KoLmafia/sideCommand?cmd=' + escape(script) + '&pwd=" );
			selectBuffer.append( GenericRequest.passwordHash );
			selectBuffer.append( "'; void(0);" );
			selectBuffer.append( "\">" );
			selectBuffer.append( "</form></td>" );

			int lastRowIndex = buffer.lastIndexOf( "</tr>" );
			if ( lastRowIndex != -1 )
			{
				buffer.insert( lastRowIndex, selectBuffer.toString() );
			}
		}
	}

	private static final void mafiatizeFunctionMenu( final StringBuffer buffer )
	{
		Matcher menuMatcher = MoonPhaseRequest.MENU1_PATTERN.matcher( buffer.toString() );
		if ( !menuMatcher.find() )
		{
			return;
		}

		StringBuffer functionMenu = new StringBuffer();
		functionMenu.append( menuMatcher.group() );

		StringUtilities.singleStringReplace(
			functionMenu,
			"<option value=\"inventory.php\">Inventory</option>",
			"<option value=\"inventory.php?which=1\">Consumables</option><option value=\"inventory.php?which=2\">Equipment</option><option value=\"inventory.php?which=3\">Misc Items</option><option value=\"sellstuff.php\">Sell Stuff</option>" );

		StringUtilities.singleStringReplace( buffer, menuMatcher.group(), functionMenu.toString() );
	}

	private static final void mafiatizeGotoMenu( final StringBuffer buffer )
	{
		Matcher menuMatcher = MoonPhaseRequest.MENU2_PATTERN.matcher( buffer.toString() );
		if ( !menuMatcher.find() )
		{
			return;
		}

		String originalMenu = menuMatcher.group( 1 );
		StringBuffer gotoMenu = new StringBuffer();
		gotoMenu.append( originalMenu );

		// Add special convenience areas not in normal menu
		for ( int i = 0; i < KoLConstants.GOTO_MENU.length; ++i )
		{
			String tag = KoLConstants.GOTO_MENU[ i ][ 0 ];
			if ( originalMenu.indexOf( tag ) != -1 )
			{
				continue;
			}
			String url = KoLConstants.GOTO_MENU[ i ][ 1 ];
			gotoMenu.append( "<option value=\"" );
			gotoMenu.append( url );
			gotoMenu.append( "\">" );
			gotoMenu.append( tag );
			gotoMenu.append( "</option>" );
		}

		String[] bookmarkData = Preferences.getString( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
		{
			gotoMenu.append( "<option value=\"nothing\"> </option>" );
			gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );

			for ( int i = 0; i < bookmarkData.length; i += 3 )
			{
				gotoMenu.append( "<option value=\"" );
				gotoMenu.append( bookmarkData[ i + 1 ] );
				gotoMenu.append( "\">" );
				gotoMenu.append( bookmarkData[ i ] );
				gotoMenu.append( "</option>" );
			}
		}

		gotoMenu.append( "</select>" );

		StringUtilities.singleStringReplace( buffer, menuMatcher.group(), gotoMenu.toString() );
	}
}
