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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MemoriesDecorator
{
	private static final Pattern ELEMENT_PATTERN = Pattern.compile( "<select name=\"slot[12345]\">.*?</select>", Pattern.DOTALL );

	public static final void decorateElements( final int choice, final StringBuffer buffer )
	{
		// Handle only Elements choice adventure
		if ( choice != 392 )
		{
			return;
		}

                // Prefill the element dropdowns correctly
                Matcher matcher = ELEMENT_PATTERN.matcher( buffer );
                MemoriesDecorator.selectElement( matcher, buffer, "sleaze" );
                MemoriesDecorator.selectElement( matcher, buffer, "spooky" );
                MemoriesDecorator.selectElement( matcher, buffer, "stench" );
                MemoriesDecorator.selectElement( matcher, buffer, "cold" );
                MemoriesDecorator.selectElement( matcher, buffer, "hot" );
        }

	private static final void selectElement( final Matcher matcher, final StringBuffer buffer, final String element )
	{
		if ( !matcher.find() )
		{
			return;
		}

		String oldSelect = matcher.group(0);
		String newSelect = StringUtilities.globalStringReplace( oldSelect,
			">" + element, " selected>" + element );
		int index = buffer.indexOf( oldSelect );
		buffer.replace( index, index + oldSelect.length(), newSelect );
	}

        // "your ancestral memories are total, absolute jerks. </p></td>"
        private static final String JERKS = "absolute jerks. </p>";
        private static final String SECRET = "<center><table class=\"item\" style=\"float: none\" rel=\"id=4114&s=0&q=0&d=0&g=0&t=0&n=1\"><tr><td><img src=\"http://images.kingdomofloathing.com/itemimages/futurebox.gif\" alt=\"secret from the future\" title=\"secret from the future\" class=hand onClick='descitem(502821529)'></td><td valign=center class=effect>You acquire an item: <b>secret from the future</b></td></tr></table></center>";

	public static final void decorateElementsResponse( final StringBuffer buffer )
	{
		int index = buffer.indexOf( MemoriesDecorator.JERKS );
		if ( index != -1 )
		{
			buffer.insert( index + MemoriesDecorator.JERKS.length(), MemoriesDecorator.SECRET );
			return;
		}
	}

	private static final String[][] ELEMENTS =
	{
		{ "strikes a match", "red" },
		{ "lit match", "red" },
		{ "vile-smelling, milky-white replicant blood", "green" },
		{ "vile-smelling, milky-white blood", "green" },
		{ "spinning, whirring, vibrating, tubular \"appendage.\"", "blueviolet" },
		{ "spinning, whirring, vibrating, tubular appendage", "blueviolet" },
		{ "liquid nitrogen", "blue" },
		{ "freaky alien thing", "gray" },
	};

	public static final void decorateMegalopolisFight( final StringBuffer buffer )
	{
		if ( !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.RUBY_ROD, 1 ) ) )
		{
			return;
		}

		for ( int i = 0; i < MemoriesDecorator.ELEMENTS.length; ++i )
		{
			String message = MemoriesDecorator.ELEMENTS[ i ][0];
			String color = MemoriesDecorator.ELEMENTS[ i ][1];
			if ( buffer.indexOf( message ) != -1 )
			{
				StringUtilities.singleStringReplace( buffer, message, "<font color=" + color + ">" + message + "</font>" );
				return;
			}
		}
	}
}
