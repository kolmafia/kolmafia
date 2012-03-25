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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.io.File;

import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.HedgePuzzleRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;

import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class TestCommand
	extends AbstractCommand
{
	private static String contents = null;

	public TestCommand()
	{
		this.usage = " 1, 2, 3...";
	}

	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "intcache" ) )
		{
			int cacheHits = IntegerPool.getCacheHits();
			int cacheMissLows = IntegerPool.getCacheMissLows();
			int cacheMissHighs = IntegerPool.getCacheMissHighs();
			int totalAccesses = cacheHits + cacheMissLows + cacheMissHighs;

			float successRate = 0.0f;

			if ( totalAccesses != 0 )
			{
				successRate = (float) cacheHits / (float) totalAccesses * 100.0f;
			}

			RequestLogger.printLine( "cache hits: " + cacheHits );
			RequestLogger.printLine( "cache misses (too low): " + cacheMissLows );
			RequestLogger.printLine( "cache misses (too high): " + cacheMissHighs );
			RequestLogger.printLine( "success rate: " + successRate + " %" );

			return;
		}

		if ( command.equals( "load" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Load what?" );
				return;
			}

			String fileName = split[ 1 ];
			File file = new File( UtilityConstants.DATA_LOCATION, fileName );

			if ( !file.exists() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "File " + file + " does not exist" );
				return;
			}
			
			byte[] bytes = ByteBufferUtilities.read( file );
			TestCommand.contents = new String( bytes );

			KoLmafia.updateDisplay( "Read " + KoLConstants.COMMA_FORMAT.format( bytes.length ) + " bytes" );
		}

		if ( TestCommand.contents == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "no HTML loaded." );
		}

		if ( command.equals( "charpane" ) )
		{
			StringBuffer buffer = new StringBuffer( TestCommand.contents );
			boolean oldCompact  = CharPaneRequest.compactCharacterPane;
			boolean oldFamiliar  = CharPaneRequest.familiarBelowEffects;

			CharPaneRequest.compactCharacterPane = true;
			CharPaneRequest.familiarBelowEffects = false;
			CharPaneDecorator.decorate( buffer );
			CharPaneRequest.compactCharacterPane = oldCompact;
			CharPaneRequest.familiarBelowEffects = oldFamiliar;

			boolean shouldOpenStream = !RequestLogger.isDebugging();
			if ( shouldOpenStream )
			{
				RequestLogger.openDebugLog();
			}
			RequestLogger.updateDebugLog( buffer.toString() );
			if ( shouldOpenStream )
			{
				RequestLogger.closeDebugLog();
			}

			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "equipment" ) )
		{
			EquipmentRequest.parseEquipment( "inventory.php?which=2", TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "fight" ) )
		{
			FightRequest.parseFightHTML( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "generator" ) )
		{
			SpaaaceRequest.visitGeneratorChoice( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "hedgepuzzle" ) )
		{
			HedgePuzzleRequest.computeSolution( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}
	}
}
