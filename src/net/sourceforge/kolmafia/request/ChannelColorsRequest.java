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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.chat.ChatFormatter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChannelColorsRequest
	extends GenericRequest
{
	private static final Pattern GENERAL_PATTERN =
		Pattern.compile( "<td>([^<]*?)&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<option value=(\\d+) selected>" );
	private static final Pattern SELF_PATTERN =
		Pattern.compile( "<select name=chatcolorself>.*?<option value=(\\d+) selected>" );
	private static final Pattern CONTACTS_PATTERN =
		Pattern.compile( "<select name=chatcolorcontacts>.*?<option value=(\\d+) selected>" );
	private static final Pattern OTHER_PATTERN =
		Pattern.compile( "<select name=chatcolorothers>.*?<option value=(\\d+) selected>" );

	private static final String[] AVAILABLE_COLORS =
	{
		"#000000", // default (0)
		"#CC9900", // brown (1)
		"#FFCC00", // gold (2)
		"#CC3300", // dark red (3)
		"#FF0033", // red (4)
		"#FF33CC", // hot pink (5)
		"#FF99FF", // soft pink (6)
		"#663399", // dark purple (7)
		"#9933CC", // purple (8)
		"#CC99FF", // light purple (9)
		"#000066", // dark blue (10)
		"#0000CC", // blue (11)
		"#9999FF", // light blue (12)
		"#336600", // dark green (13)
		"#339966", // green (14)
		"#66CC99", // light green (15)
		"#EAEA9A", // mustard (16)
		"#FF9900", // orange (17)
		"#000000", // black (18)
		"#666666", // dark grey (19)
		"#CCCCCC" // light grey (20)
	};

	public ChannelColorsRequest()
	{
		super( "account_chatcolors.php" );
	}

	@Override
	public void run()
	{
		super.run();

		if ( this.responseText == null )
		{
			return;
		}

		// First, add in all the colors for all of the
		// channel tags (for people using standard KoL
		// chatting mode).

		Matcher colorMatcher = ChannelColorsRequest.GENERAL_PATTERN.matcher( this.responseText );
		while ( colorMatcher.find() )
		{
			String channel = "/" + colorMatcher.group( 1 ).toLowerCase();
			this.setColor( channel, StringUtilities.parseInt( colorMatcher.group( 2 ) ) );
		}

		// Add in other custom colors which are available
		// in the chat options.

		colorMatcher = ChannelColorsRequest.SELF_PATTERN.matcher( this.responseText );
		if ( colorMatcher.find() )
		{
			this.setColor( "chatcolorself", StringUtilities.parseInt( colorMatcher.group( 1 ) ) );
		}

		colorMatcher = ChannelColorsRequest.CONTACTS_PATTERN.matcher( this.responseText );
		if ( colorMatcher.find() )
		{
			this.setColor( "chatcolorcontacts", StringUtilities.parseInt( colorMatcher.group( 1 ) ) );
		}

		colorMatcher = ChannelColorsRequest.OTHER_PATTERN.matcher( this.responseText );
		if ( colorMatcher.find() )
		{
			this.setColor( "chatcolorothers", StringUtilities.parseInt( colorMatcher.group( 1 ) ) );
		}
	}

	private void setColor( final String channel, final int colorIndex )
	{
		if ( colorIndex == 0 )
		{
			ChatFormatter.setChatColor( channel, channel.startsWith( "chat" ) ? "black" : "green" );
		}
		else
		{
			ChatFormatter.setChatColor( channel, ChannelColorsRequest.AVAILABLE_COLORS[ colorIndex ] );
		}
	}
}
