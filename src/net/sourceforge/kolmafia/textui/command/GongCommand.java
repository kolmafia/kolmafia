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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class GongCommand
	extends AbstractCommand
{
	public GongCommand()
	{
		this.usage = " [buy | set] manual | bird | mole | roach [<effect> [<stat> [<stat>]]]";
	}

	// These are all the possible paths that start with The Gong Has Been Bung.
	public static final String[] GONG_PATHS =
	{
		"show in browser",
		"bird",
		"mole",
		"roach (in browser)",
		"musc, musc, +30% musc",
		"musc, mox, +30% musc",
		"musc, MP, +30% musc",
		"myst, musc, +30% myst",
		"myst, myst, +30% myst",
		"myst, MP, +30% myst",
		"mox, myst, +30% mox",
		"mox, mox, +30% mox",
		"mox, MP, +30% mox",
		"musc, musc, +10% all",
		"myst, musc, +10% all",
		"musc, mox, +10% all",
		"myst, myst, +10% all",
		"mox, mox, +10% all",
		"mox, MP, +10% all",
		"musc, musc, +50% items",
		"myst, musc, +50% items",
		"musc, MP, +50% items",
		"mox, myst, +50% items",
		"myst, MP, +50% items",
		"mox, MP, +50% items",
		"musc, mox, +30 ML",
		"musc, MP, +30 ML",
		"myst, myst, +30 ML",
		"mox, myst, +30 ML",
		"myst, MP, +30 ML",
		"mox, mox, +30 ML",
	};

	// These are the choice adventure settings corresponding to GONG_PATHS.
	// Encoding: 2 bits per choice adv, starting with #276 in the low bits,
	// continuing thru #290 for a total of 30 bits used.
	private static final int[] GONG_CHOICES =
	{
		0x00000004,
		0x00000007,
		0x00000006,
		0x00000005,
		0x00004095,
		0x00001055,
		0x000100d5,
		0x00300225,
		0x00040125,
		0x00800325,
		0x08000835,
		0x02000435,
		0x10000c35,
		0x00008095,
		0x00200225,
		0x00002055,
		0x000c0125,
		0x03000435,
		0x20000c35,
		0x0000c095,
		0x00100225,
		0x000200d5,
		0x0c000835,
		0x00c00325,
		0x30000c35,
		0x00003055,
		0x000300d5,
		0x00080125,
		0x04000835,
		0x00400325,
		0x01000435
	};

	// These are the precalculated roach paths for achieving the closest possible
	// approximation to any desired effect & stat boosts.
	// Index: primary desired stat (musc=0, myst, mox, MP)
	//	+ 4 * secondary desired stat (musc=0, myst, mox, MP)
	//	+ 16 * mainstat (musc=0, myst, mox) as a tiebreaker
	// Values: six 5-bit fields, containing indexes into the arrays above for each
	//	possible effect (+mus%=low bits, +mys%, +mox%, +all%, +item, +ML)
	private static final int[] GONG_SEARCH =
	{
		0x3336a8e4,
		0x374728e4,
		0x3367ace5,
		0x35593126,
		0x334728e4,
		0x37482904,
		0x3967a8e5,
		0x3b793126,
		0x3337ace5,
		0x396728e5,
		0x3d68ace5,
		0x35893126,
		0x3556b0e6,
		0x3b772926,
		0x33893125,
		0x35593126,

		0x3336a8e4,
		0x374728e4,
		0x3367a8e5,
		0x35593126,
		0x334728e4,
		0x37482904,
		0x3968a905,
		0x3b793126,
		0x3347a8e5,
		0x39682905,
		0x3d68ad05,
		0x3b893126,
		0x355730e6,
		0x3b782926,
		0x39893125,
		0x3b793126,

		0x3336ace4,
		0x394728e5,
		0x3367ace5,
		0x35593126,
		0x334728e5,
		0x37682905,
		0x3968a905,
		0x3b793126,
		0x3337ace5,
		0x39682905,
		0x3d68ace5,
		0x35893126,
		0x3557b0e6,
		0x3b782926,
		0x3d893125,
		0x35893126
	};

	public void run( final String command, final String parameters )
	{
		String[] parameterList = parameters.split( "\\s+" );

		int pos = 0;
		int len = parameterList.length;
		boolean buy = false;
		boolean set = false;
		if ( pos < len && parameterList[ pos ].equalsIgnoreCase( "buy" ) )
		{
			buy = true;
			++pos;
		}
		else if ( pos < len && parameterList[ pos ].equalsIgnoreCase( "set" ) )
		{
			set = true;
			++pos;
		}

		if ( pos >= len || parameterList[ pos ].equals( "" ) )
		{
			RequestLogger.printLine( "Usage: gong" + this.usage );
			RequestLogger.printLine( "buy - use a gong even if you have to buy one." );
			RequestLogger.printLine( "set - don't use a gong, just set choices." );
			RequestLogger.printLine( "manual - show choices in browser." );
			RequestLogger.printLine( "bird | mole | roach - path to take." );
			RequestLogger.printLine( "'roach' can be followed by 1, 2, or 3 of:" );
			RequestLogger.printLine( "mus | mys | mox | all | item | ML - effect to get (20 turns)." );
			RequestLogger.printLine( "(You can also use the first word of the effect name.)" );
			RequestLogger.printLine( "mus | mys | mox | MP - stat to boost." );
			RequestLogger.printLine( "mus | mys | mox | MP - another stat to boost." );
			RequestLogger.printLine( "(If a stat is not specified, or is impossible due to other choices, your mainstat will be boosted if possible." );
			return;
		}

		int path = GongCommand.parse( parameterList, pos++ , "manual bird mole roach" );
		if ( path == 3 && pos < len )
		{
			int effect =
				GongCommand.parse( parameterList, pos++ , "mus ack mys alc mox rad all new item ext ml unp" ) / 2;
			int primary, secondary, main;
			primary = secondary = main = KoLCharacter.getPrimeIndex();
			if ( pos < len )
			{
				primary = GongCommand.parse( parameterList, pos++ , "mus mys mox mp" );
			}
			if ( pos < len )
			{
				secondary = GongCommand.parse( parameterList, pos++ , "mus mys mox mp" );
			}
			path = GongCommand.GONG_SEARCH[ primary + 4 * secondary + 16 * main ];
			path = path >> 5 * effect & 0x1F;
		}
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}
		if ( pos < len )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "Unexpected text after command, starting with: " + parameterList[ pos ] );
			return;
		}
		KoLmafia.updateDisplay( "Gong path: " + GongCommand.GONG_PATHS[ path ] );
		GongCommand.setPath( path );
		if ( set )
		{
			return;
		}
		AdventureResult gong = ItemPool.get( ItemPool.GONG, 1 );
		if ( buy && !InventoryManager.hasItem( gong ) )
		{
			BuyCommand.buy( "1 llama lama gong" );
		}
		RequestThread.postRequest( UseItemRequest.getInstance( gong ) );
	}

	private static int parse( final String[] parameters, final int pos, final String optionString )
	{
		if ( pos >= parameters.length )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Expected one of: " + optionString );
			return 0;
		}
		String[] options = optionString.split( " " );
		String param = parameters[ pos ].toLowerCase();
		for ( int i = 0; i < options.length; ++i )
		{
			if ( param.startsWith( options[ i ] ) )
			{
				return i;
			}
		}
		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Found '" + param + "', but expected one of: " + optionString );
		return 0;
	}

	public static void setPath( int path )
	{
		if ( path < 0 || path > GongCommand.GONG_PATHS.length )
		{
			return;
		}
		Preferences.setInteger( "gongPath", path );
		path = GongCommand.GONG_CHOICES[ path ];
		for ( int i = 276; i <= 290; ++i )
		{
			Preferences.setString( "choiceAdventure" + i, String.valueOf( path & 0x03 ) );
			path >>= 2;
		}
	}

}
