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

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.DataUtilities;

public class LimitedSizeChatBuffer extends ChatBuffer implements KoLConstants
{
	private static final int RESIZE_SIZE = 12000;
	private static final int MAXIMUM_SIZE = 16000;
	private static final int DELETE_AMOUNT = MAXIMUM_SIZE - RESIZE_SIZE;

	public static final List colors = new ArrayList();
	public static final List highlights = new ArrayList();
	public static final List dehighlights = new ArrayList();
	public static LimitedSizeChatBuffer highlightBuffer;

	private boolean requiresTruncation;
	private boolean affectsHighlightBuffer;

	public LimitedSizeChatBuffer()
	{	this( "", true, false );
	}

	public LimitedSizeChatBuffer( boolean requiresTruncation )
	{	this( "", requiresTruncation, false );
	}

	public LimitedSizeChatBuffer( String title, boolean requiresTruncation, boolean affectsHighlightBuffer )
	{
		super( title, requiresTruncation );
		this.requiresTruncation = requiresTruncation;
		this.affectsHighlightBuffer = affectsHighlightBuffer;
	}

	public static final void clearHighlights()
	{
		colors.clear();
		highlights.clear();
		dehighlights.clear();
	}

	public static final String removeHighlight( int index )
	{
		String removedColor = (String) colors.remove( index );
		String removedPattern = ((Pattern) highlights.remove( index )).pattern();
		dehighlights.remove( index );

		return removedPattern + "\n" + removedColor;
	}

	/**
	 * Used to change the font size for all current chat buffers.  Note that
	 * this does not affect logging.
	 */

	public static final void updateFontSize()
	{
		ChatBuffer.BUFFER_STYLE = "body { font-family: sans-serif; font-size: " +
			KoLSettings.getUserProperty( "chatFontSize" ) + "; } a { color: black; text-decoration: none; }";
	}

	/**
	 * Appends the given message to the chat buffer.
	 * @param	message	The message to be appended to this <code>ChatBuffer</code>
	 */

	public void append( String message )
	{
		// Download all the images outside of the Swing thread
		// by downloading them here.  Also convert everything
		// into unicode, rather than character entities.

		message = RequestEditorKit.getStripped( RequestEditorKit.getEntities( message ) );
		String highlightMessage = message;

		if ( this != highlightBuffer )
		{
			if ( !highlights.isEmpty() )
			{
				String [] colorArray = new String[ colors.size() ];
				colors.toArray( colorArray );

				Pattern [] highlightArray = new Pattern[ highlights.size() ];
				highlights.toArray( highlightArray );

				Pattern [] dehighlightArray = new Pattern[ dehighlights.size() ];
				dehighlights.toArray( dehighlightArray );

				for ( int i = 0; i < colorArray.length; ++i )
					highlightMessage = this.applyHighlight( highlightMessage, colorArray[i], highlightArray[i], dehighlightArray[i] );
			}
		}

		super.append( highlightMessage );

		if ( this.requiresTruncation && this.displayBuffer.length() > MAXIMUM_SIZE )
		{
			int lineIndex = this.displayBuffer.indexOf( "<br>", DELETE_AMOUNT );
			if ( lineIndex == -1 )
				lineIndex = this.displayBuffer.lastIndexOf( "<br>", DELETE_AMOUNT );

			if ( lineIndex != -1 )
				this.displayBuffer.delete( 0, lineIndex + 4 );
			else
				this.displayBuffer.setLength( 0 );

			this.fireBufferChanged();
		}

		if ( this.affectsHighlightBuffer && message.compareToIgnoreCase( highlightMessage ) != 0 )
			highlightBuffer.append( highlightMessage.replaceAll( "(<br>)+", "<br>" + LINE_BREAK ) );
	}

	public static final String addHighlight( String highlight, Color color )
	{
		String colorString = DataUtilities.toHexString( color );

		colors.add( colorString );
		highlights.add( Pattern.compile( highlight, Pattern.CASE_INSENSITIVE ) );
		dehighlights.add( Pattern.compile( "(<[^>]*?)<font color=\"" + colorString + "\">" + highlight + "</font>", Pattern.CASE_INSENSITIVE ) );

		return highlight + "\n" + colorString;
	}

	public void applyHighlights()
	{
		if ( this == highlightBuffer )
			return;

		String colorString;
		Pattern highlight, dehighlight;

		String highlightMessage;

		String displayString = this.displayBuffer.toString();
		String [] lines = displayString.split( "<br>" );

		for ( int j = 0; j < highlights.size(); ++j )
		{
			colorString = (String) colors.get(j);
			highlight = (Pattern) highlights.get(j);
			dehighlight = (Pattern) dehighlights.get(j);

			for ( int i = 0; i < lines.length; ++i )
			{
				highlightMessage = this.applyHighlight( lines[i], colorString, highlight, dehighlight );
				if ( lines[i].compareToIgnoreCase( highlightMessage ) != 0 )
					highlightBuffer.append( highlightMessage + "<br>" );
			}

			displayString = this.applyHighlight( displayString, colorString, highlight, dehighlight );
		}

		this.displayBuffer.setLength( 0 );
		this.displayBuffer.append( displayString );
		this.fireBufferChanged();
	}

	private String applyHighlight( String message, String colorString, Pattern highlight, Pattern dehighlight )
	{
		if ( message.indexOf( "<html>" ) != -1 )
			return message;

		Matcher matching = highlight.matcher( message );
		String highlightMessage = matching.replaceAll( "<font color=\"" + colorString + "\">" + highlight.pattern() + "</font>" );

		// Now make sure that the changes occuring inside of
		// HTML tags don't get saved.

		if ( !message.equals( highlightMessage ) )
			highlightMessage = dehighlight.matcher( highlightMessage ).replaceAll( "$1" + highlight.pattern() );

		// Now that everything is properly replaced, go ahead
		// and return the finalized string.

		return highlightMessage;
	}
}
