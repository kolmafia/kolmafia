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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DwarfFactoryRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	public static final Pattern RUNE_PATTERN = Pattern.compile( "title=\"Dwarf (Digit|Word) Rune (.)\"" );
	public static final Pattern ITEMDESC_PATTERN = Pattern.compile( "descitem\\((\\d*)\\)" );
	public static final Pattern MEAT_PATTERN = Pattern.compile( "You (gain|lose) (\\d*) Meat" );

	private static final int [] ITEMS = new int[]
	{
		ItemPool.SPRING,
		ItemPool.SPROCKET,
		ItemPool.COG,
		ItemPool.MINERS_HELMET,
		ItemPool.MINERS_PANTS,
		ItemPool.MATTOCK,
		ItemPool.LINOLEUM_ORE,
		ItemPool.ASBESTOS_ORE,
		ItemPool.CHROME_ORE,
		ItemPool.DWARF_BREAD,
		ItemPool.LUMP_OF_COAL,
	};

	public DwarfFactoryRequest()
	{
		super( "dwarffactory.php" );
	}

	public DwarfFactoryRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	public void processResults()
	{
		DwarfFactoryRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "dwarffactory.php" ) )
		{
			return;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "ware" ) )
		{
			Matcher runeMatcher = DwarfFactoryRequest.getRuneMatcher( responseText );
			String rune1 = DwarfFactoryRequest.getRune( runeMatcher );
			String rune2 = DwarfFactoryRequest.getRune( runeMatcher );
			String rune3 = DwarfFactoryRequest.getRune( runeMatcher );
			int itemId = DwarfFactoryRequest.getItemId( responseText );
			DwarfFactoryRequest.setItemRunes( itemId, rune1, rune2, rune3 );
			return;
		}

		if ( action.equals( "dodice" ) )
		{
			Matcher meatMatcher = MEAT_PATTERN.matcher( responseText );
			if ( !meatMatcher.find() )
			{
				return;
			}

			boolean won = meatMatcher.group(1).equals( "gain" );
			int meat = StringUtilities.parseInt( meatMatcher.group( 2 ) ) / 7;

			Matcher runeMatcher = DwarfFactoryRequest.getRuneMatcher( responseText );
			String first = DwarfFactoryRequest.getRune( runeMatcher ) + DwarfFactoryRequest.getRune( runeMatcher );
			String second = DwarfFactoryRequest.getRune( runeMatcher ) + DwarfFactoryRequest.getRune( runeMatcher );
			String message = ( won ? second : first ) + "-" + ( won ? first : second ) + "=" + ( meat / 7 ) + ( meat % 7 );

			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );

			return;
		}
	}

	private static void setItemRunes( final int itemId, final String rune1, final String rune2, final String rune3 )
	{
		KoLCharacter.ensureUpdatedDwarfFactory();
		String setting = "lastDwarfFactoryItem" + itemId;
		String oldRunes = Preferences.getString( setting );
		String newRunes = "";

		if ( oldRunes.equals( "" ) )
		{
			newRunes = rune1 + rune2 + rune3;
		}
		else
		{
			if ( oldRunes.indexOf( rune1) != -1 )
			{
				newRunes += rune1;
			}
			if ( oldRunes.indexOf( rune2) != -1 )
			{
				newRunes += rune2;
			}
			if ( oldRunes.indexOf( rune3) != -1 )
			{
				newRunes += rune3;
			}
		}
		Preferences.setString( setting, newRunes );

		if ( newRunes.length() > 1 )
		{
			return;
		}

		// If the length is 1, that rune has been matched with an
		// item. Therefore, if it appears in the list of runes for
		// another item, it can't be the match for that item, too, and
		// can be removed from that list.

		DwarfFactoryRequest.pruneItemRunes( itemId, newRunes );
	}

	private static void pruneItemRunes( final int id, final String rune )
	{
		for ( int i = 0; i < ITEMS.length; ++i )
		{
			int itemId = ITEMS[i];
			if ( id == itemId )
			{
				continue;
			}

			String setting = "lastDwarfFactoryItem" + itemId;
			String value = Preferences.getString( setting );

			if ( value.indexOf( rune ) == -1 )
			{
				continue;
			}

			value = value.replace( rune, "" );
			Preferences.setString( setting, value );

			if ( value.length() == 1 )
			{
				// We've identified another item. Recurse!
				DwarfFactoryRequest.pruneItemRunes( itemId, value );
			}
		}
	}

	private static Matcher getRuneMatcher( final String responseText )
	{
		return RUNE_PATTERN.matcher( responseText );
	}

	public static String getRune( final String responseText )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		return DwarfFactoryRequest.getRune( matcher );
	}

	private static String getRune( final Matcher matcher )
	{
		if ( !matcher.find() )
		{
			return "";
		}

		return matcher.group( 2 );
	}

	public static void useUnlaminatedItem( final int itemId, final String responseText )
	{
		DwarfFactoryRequest.useItem( itemId, responseText, 2 );
	}

	public static void useLaminatedItem( final int itemId, final String responseText )
	{
		DwarfFactoryRequest.useItem( itemId, responseText, 3 );
	}

	private static void useItem( final int itemId, final String responseText, final int offset )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		String runes = "";
		boolean digit = false;
		int count = 0;
		while ( matcher.find() )
		{
			if ( ++count == offset )
			{
				runes += ',';
			}
			String type = matcher.group(1);
			if ( type.equals( "Digit" ) )
			{
				digit = true;
			}
			else
			{
				if ( digit )
				{
					runes += ',';
				}
				digit = false;
			}
			runes += matcher.group( 2 );
		}
		Preferences.setString( "lastDwarfOfficeItem" + itemId, runes );
	}

	private static int getItemId( final String responseText )
	{
		Matcher matcher = ITEMDESC_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return -1;
		}

		return ItemDatabase.getItemIdFromDescription( matcher.group(1) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "dwarffactory.php" ) )
		{
			return false;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		if ( action.equals( "ware" ) )
		{
			String message = "[" + KoLAdventure.getAdventureCount() + "] Dwarven Factory Warehouse";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		if ( action.equals( "dorm" ) )
		{
			String message = "Visiting the Dwarven Factory Dormitory";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( action.equals( "dodice" ) || action.equals( "nodice" ) || action.equals( "nonodice" ) )
		{
			return true;
		}

		return false;
	}
}
