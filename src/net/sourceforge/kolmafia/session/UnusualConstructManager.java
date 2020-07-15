/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.objectpool.ItemPool;

public class UnusualConstructManager
{
	private static final Pattern COLOR_PATTERN = Pattern.compile( "(?:LANO|ROUTING) ([a-zA-Z]*)" );

	private static int DISC = 0;

	public static int disc()
	{
		return DISC;
	}

	public static boolean solve( final String responseText )
	{
		DISC = 0;

		// Give us a response text, at least!
		if ( responseText == null )
		{
			return false;
		}

		// Extract the clues from the text
		Matcher matcher = COLOR_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return false;
		}

		String colorWord = matcher.group( 1 );

		if ( colorWord.equals( "CHO" )
		  || colorWord.equals( "FUNI" )
		  || colorWord.equals( "TAZAK" )
		  || colorWord.equals( "CANARY" )
		  || colorWord.equals( "CITRINE" )
		  || colorWord.equals( "GOLD" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_YELLOW;
			return true;
		}

		if ( colorWord.equals( "CHAKRO" )
		  || colorWord.equals( "ZEVE" )
		  || colorWord.equals( "ZEVESTANO" )
		  || colorWord.equals( "CRIMSON" )
		  || colorWord.equals( "RUBY" )
		  || colorWord.equals( "VERMILLION" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_RED;
			return true;
		}

		if ( colorWord.equals( "BUPABU" )
		  || colorWord.equals( "PATA" )
		  || colorWord.equals( "SOM" )
		  || colorWord.equals( "OBSIDIAN" )
		  || colorWord.equals( "EBONY" )
		  || colorWord.equals( "JET" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_BLACK;
			return true;
		}

		if ( colorWord.equals( "BE" )
		  || colorWord.equals( "ZAKSOM" )
		  || colorWord.equals( "ZEVEBENI" )
		  || colorWord.equals( "JADE" )
		  || colorWord.equals( "VERDIGRIS" )
		  || colorWord.equals( "EMERALD" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_GREEN;
			return true;
		}

		if ( colorWord.equals( "BELA" )
		  || colorWord.equals( "BULAZAK" )
		  || colorWord.equals( "BU" )
		  || colorWord.equals( "FUFUGAKRO" )
		  || colorWord.equals( "ULTRAMARINE" )
		  || colorWord.equals( "SAPPHIRE" )
		  || colorWord.equals( "COBALT" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_BLUE;
			return true;
		}

		if ( colorWord.equals( "NIPA" )
		  || colorWord.equals( "PACHA" )
		  || colorWord.equals( "SOMPAPA" )
		  || colorWord.equals( "IVORY" )
		  || colorWord.equals( "ALABASTER" )
		  || colorWord.equals( "PEARL" )
		   )
		{
			DISC = ItemPool.STRANGE_DISC_WHITE;
			return true;
		}

		return false;
	}
}
