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

package net.sourceforge.kolmafia.chat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.Preferences;

public class StyledChatBuffer
	extends ChatBuffer
{
	public static final List colors = new ArrayList();
	public static final List highlights = new ArrayList();
	public static final List dehighlights = new ArrayList();

	public static StyledChatBuffer highlightBuffer;
	private final boolean affectsHighlightBuffer;

	public StyledChatBuffer( final String title, final boolean affectsHighlightBuffer )
	{
		super( title );
		this.affectsHighlightBuffer = affectsHighlightBuffer;
	}

	public static final void clearHighlights()
	{
		StyledChatBuffer.colors.clear();
		StyledChatBuffer.highlights.clear();
		StyledChatBuffer.dehighlights.clear();
	}

	public static final String removeHighlight( final int index )
	{
		String removedColor = (String) StyledChatBuffer.colors.remove( index );
		String removedPattern = ( (Pattern) StyledChatBuffer.highlights.remove( index ) ).pattern();
		StyledChatBuffer.dehighlights.remove( index );

		return removedPattern + "\n" + removedColor;
	}

	public static final String addHighlight( final String highlight, final Color color )
	{
		String colorString = DataUtilities.toHexString( color );

		StyledChatBuffer.colors.add( colorString );
		StyledChatBuffer.highlights.add( Pattern.compile( highlight, Pattern.CASE_INSENSITIVE ) );
		StyledChatBuffer.dehighlights.add( Pattern.compile(
			"(<[^>]*?)<font color=\"" + colorString + "\">" + highlight + "</font>", Pattern.CASE_INSENSITIVE ) );

		return highlight + "\n" + colorString;
	}

	/**
	 * Appends the given message to the chat buffer.
	 */

	public void append( final String message )
	{
		// Download all the images outside of the Swing thread
		// by downloading them here.

		String highlightMessage = message;

		if ( this != StyledChatBuffer.highlightBuffer )
		{
			if ( !StyledChatBuffer.highlights.isEmpty() )
			{
				String[] colorArray = new String[ StyledChatBuffer.colors.size() ];
				StyledChatBuffer.colors.toArray( colorArray );

				Pattern[] highlightArray = new Pattern[ StyledChatBuffer.highlights.size() ];
				StyledChatBuffer.highlights.toArray( highlightArray );

				Pattern[] dehighlightArray = new Pattern[ StyledChatBuffer.dehighlights.size() ];
				StyledChatBuffer.dehighlights.toArray( dehighlightArray );

				for ( int i = 0; i < colorArray.length; ++i )
				{
					highlightMessage =
						this.applyHighlight(
							highlightMessage, colorArray[ i ], highlightArray[ i ], dehighlightArray[ i ] );
				}
			}
		}

		super.append( highlightMessage );

		if ( this.affectsHighlightBuffer && message.compareToIgnoreCase( highlightMessage ) != 0 )
		{
			StyledChatBuffer.highlightBuffer.append( highlightMessage.replaceAll(
				"(<br>)+", "<br>" + KoLConstants.LINE_BREAK ) );
		}
	}

	public String getStyle()
	{
		return "body { font-family: sans-serif; font-size: " + Preferences.getString( "chatFontSize" ) + "; } a { color: black; text-decoration: none; }";
	}

	public void applyHighlights()
	{
		if ( this == StyledChatBuffer.highlightBuffer )
		{
			return;
		}

		String colorString;
		Pattern highlight, dehighlight;

		String highlightMessage;

		String displayString = this.getHTMLContent();
		String[] lines = displayString.split( "<br>" );

		for ( int j = 0; j < StyledChatBuffer.highlights.size(); ++j )
		{
			colorString = (String) StyledChatBuffer.colors.get( j );
			highlight = (Pattern) StyledChatBuffer.highlights.get( j );
			dehighlight = (Pattern) StyledChatBuffer.dehighlights.get( j );

			for ( int i = 0; i < lines.length; ++i )
			{
				highlightMessage = this.applyHighlight( lines[ i ], colorString, highlight, dehighlight );
				if ( lines[ i ].compareToIgnoreCase( highlightMessage ) != 0 )
				{
					StyledChatBuffer.highlightBuffer.append( highlightMessage + "<br>" );
				}
			}

			displayString = this.applyHighlight( displayString, colorString, highlight, dehighlight );
		}

		this.clear();
		this.append( displayString );
	}

	private String applyHighlight( final String message, final String colorString, final Pattern highlight,
		final Pattern dehighlight )
	{
		if ( message.indexOf( "<html>" ) != -1 )
		{
			return message;
		}

		Matcher matching = highlight.matcher( message );
		String highlightMessage =
			matching.replaceAll( "<font color=\"" + colorString + "\">" + highlight.pattern() + "</font>" );

		// Now make sure that the changes occuring inside of
		// HTML tags don't get saved.

		if ( !message.equals( highlightMessage ) )
		{
			highlightMessage = dehighlight.matcher( highlightMessage ).replaceAll( "$1" + highlight.pattern() );
		}

		// Now that everything is properly replaced, go ahead
		// and return the finalized string.

		return highlightMessage;
	}
}
