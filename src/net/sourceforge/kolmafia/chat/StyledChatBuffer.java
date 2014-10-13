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

package net.sourceforge.kolmafia.chat;

import java.awt.Color;

import java.util.ArrayList;
import java.util.List;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.preferences.Preferences;

public class StyledChatBuffer
	extends ChatBuffer
{
	private static int highlightCount = 0;

	public static final List searchStrings = new ArrayList();
	public static final List colorStrings = new ArrayList();

	private final String linkColor;
	
	public StyledChatBuffer( final String title, final String linkColor, final boolean affectsHighlightBuffer )
	{
		super( title );
		
		this.linkColor = linkColor;
	}

	public static final boolean initializeHighlights()
	{
		String highlights = Preferences.getString( "highlightList" ).trim();

		if ( highlights.length() == 0 )
		{
			return false;
		}

		StyledChatBuffer.highlightCount = 0;
		String[] highlightList = highlights.split( "\n+" );

		for ( int i = 0; i < highlightList.length; ++i )
		{
			StyledChatBuffer.addHighlight( highlightList[ i ], DataUtilities.toColor( highlightList[ ++i ] ) );
		}

		return true;
	}

	public static final String removeHighlight( final int index )
	{
		--StyledChatBuffer.highlightCount;

		String searchString = (String) StyledChatBuffer.searchStrings.remove( index );
		String colorString = (String) StyledChatBuffer.colorStrings.remove( index );

		return searchString + "\n" + colorString;
	}

	public static final String addHighlight( final String searchString, final Color color )
	{
		++StyledChatBuffer.highlightCount;

		String colorString = DataUtilities.toHexString( color );
		
		StyledChatBuffer.searchStrings.add( searchString.toLowerCase() );
		StyledChatBuffer.colorStrings.add( colorString );

		return searchString + "\n" + colorString;
	}

	/**
	 * Appends the given message to the chat buffer.
	 */

	@Override
	public void append( final String message )
	{
		if ( message == null )
		{
			super.append( null );
			return;
		}
		
		// Download all the images outside of the Swing thread
		// by downloading them here.

		String highlightMessage = message;

		for ( int i = 0; i < StyledChatBuffer.highlightCount; ++i )
		{
			String searchString = (String) StyledChatBuffer.searchStrings.get( i );
			String colorString = (String) StyledChatBuffer.colorStrings.get( i );

			highlightMessage = this.applyHighlight( highlightMessage, searchString, colorString );
		}

		super.append( highlightMessage );
	}

	@Override
	public String getStyle()
	{
		return "body { font-family: sans-serif; font-size: " + Preferences.getString( "chatFontSize" ) + "; } a { color: " + linkColor + "; text-decoration: none; } a.error { color: red; text-decoration: underline }";
	}

	public void applyHighlights()
	{
		String[] lines = this.getContent().split( "<br>" );

		this.clear();

		for ( int i = 0; i < lines.length; ++i )
		{
			this.append( lines[ i ] + "<br>" );
		}
	}

	private String applyHighlight( final String message, final String searchString, final String colorString )
	{
		if ( message.contains( "<html>" ) )
		{
			return message;
		}

		StringBuilder highlightMessage = new StringBuilder();
		String remaining = message;
		
		while ( true )
		{
			int searchIndex = remaining.toLowerCase().indexOf( searchString );
			if ( searchIndex == -1 )
			{
				break;
			}

			// Do not highlight HTML tags
			int openIndex = remaining.indexOf( "<" );
			if ( openIndex < searchIndex )
			{
				int closeIndex = remaining.indexOf( ">", openIndex ) + 1;
				if ( closeIndex > 0 )
				{
					highlightMessage.append( remaining.substring( 0, closeIndex ) );
					remaining = remaining.substring( closeIndex );
					continue;
				}
			}

			int stopIndex = searchIndex + searchString.length();

			highlightMessage.append( remaining.substring( 0, searchIndex ) );

			highlightMessage.append( "<font color=\"" );
			highlightMessage.append( colorString );
			highlightMessage.append( "\">" );
			highlightMessage.append( remaining.substring( searchIndex, stopIndex ) );
			highlightMessage.append( "</font>" );

			remaining = remaining.substring( stopIndex );
		}

		if ( highlightMessage.length() == 0 )
		{
			return message;
		}

		highlightMessage.append( remaining );
		return highlightMessage.toString();
	}
}
