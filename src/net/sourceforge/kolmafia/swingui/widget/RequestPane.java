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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;

import java.io.StringWriter;

import java.util.regex.Pattern;

import javax.swing.JEditorPane;

import javax.swing.text.html.HTMLDocument;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RequestPane
	extends JEditorPane
{
	private static final Pattern WHITESPACE = Pattern.compile( "\n\\s*" );
	private static final Pattern LINE_BREAK = Pattern.compile( "<br/?>", Pattern.CASE_INSENSITIVE );

	public RequestPane()
	{
		this.setContentType( "text/html" );
		this.setEditable( false );

		HTMLDocument currentHTML = (HTMLDocument) getDocument();
		currentHTML.putProperty( "multiByte", Boolean.FALSE );
		
		// Ensure that the background is off-white so that the
		// text is always legible.

		this.setBackground( new Color( 252, 252, 252 ) );

		// Set the font in the JEditorPane to be our default font

		this.setFont( KoLConstants.DEFAULT_FONT );
	}

	@Override
	public String getSelectedText()
	{
		// Retrieve the HTML version of the current selection
		// so that you can override the <BR> handling.

		StringWriter sw = new StringWriter();

		try
		{
			this.getEditorKit().write(
				sw, this.getDocument(), this.getSelectionStart(), this.getSelectionEnd() - this.getSelectionStart() );
		}
		catch ( Exception e )
		{
			// In the event that an exception happens, return
			// an empty string.

			return "";
		}

		// The HTML returned by Java is wrapped in body tags,
		// so remove those to find out the remaining HTML.

		String selectedText = sw.toString();
		int beginIndex = selectedText.indexOf( "<body>" ) + 6;
		int endIndex = selectedText.lastIndexOf( "</body>" );

		if ( beginIndex == -1 || endIndex == -1 )
		{
			return "";
		}

		selectedText = selectedText.substring( beginIndex, endIndex ).trim();
		if ( Preferences.getBoolean( "copyAsHTML" ) )
		{
			return selectedText;
		}

		// Now we begin trimming out some of the whitespace,
		// because that causes some strange rendering problems.

		selectedText = RequestPane.WHITESPACE.matcher( selectedText ).replaceAll( "\n" );

		selectedText = StringUtilities.globalStringDelete( selectedText, "\r" );
		selectedText = StringUtilities.globalStringDelete( selectedText, "\n" );
		selectedText = StringUtilities.globalStringDelete( selectedText, "\t" );

		// Finally, we start replacing the various HTML tags
		// with emptiness, except for the <br> tag which is
		// rendered as a new line.

		selectedText = RequestPane.LINE_BREAK.matcher( selectedText ).replaceAll( "\n" ).trim();
		selectedText = KoLConstants.ANYTAG_PATTERN.matcher( selectedText ).replaceAll( "" );

		return StringUtilities.getEntityDecode( selectedText, false );
	}
}
