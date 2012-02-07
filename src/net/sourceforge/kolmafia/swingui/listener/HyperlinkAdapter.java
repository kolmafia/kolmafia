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

package net.sourceforge.kolmafia.swingui.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class HyperlinkAdapter
	implements HyperlinkListener
{
	private static final Pattern[] ACTION_PATTERNS = new Pattern[ 3 ];
	private static final Pattern[] NAME_PATTERNS = new Pattern[ 3 ];
	private static final Pattern[] VALUE_PATTERNS = new Pattern[ 3 ];

	static
	{
		HyperlinkAdapter.ACTION_PATTERNS[ 0 ] = Pattern.compile( "action=\"(.*?)\"" );
		HyperlinkAdapter.NAME_PATTERNS[ 0 ] = Pattern.compile( "name=\"(.*?)\"" );
		HyperlinkAdapter.VALUE_PATTERNS[ 0 ] = Pattern.compile( "value=\"(.*?)\"" );

		HyperlinkAdapter.ACTION_PATTERNS[ 1 ] = Pattern.compile( "action=\'(.*?)\'" );
		HyperlinkAdapter.NAME_PATTERNS[ 1 ] = Pattern.compile( "name=\'(.*?)\'" );
		HyperlinkAdapter.VALUE_PATTERNS[ 1 ] = Pattern.compile( "value=\'(.*?)\'" );

		HyperlinkAdapter.ACTION_PATTERNS[ 2 ] = Pattern.compile( "action=([^\\s]*?)" );
		HyperlinkAdapter.NAME_PATTERNS[ 2 ] = Pattern.compile( "name=([^\\s]*?)" );
		HyperlinkAdapter.VALUE_PATTERNS[ 2 ] = Pattern.compile( "value=([^\\s]*?)" );
	}

	public void hyperlinkUpdate( final HyperlinkEvent e )
	{
		if ( e.getEventType() != HyperlinkEvent.EventType.ACTIVATED )
		{
			return;
		}

		RequestPane requestPane = (RequestPane) e.getSource();
		String location = e.getDescription();

		RequestThread.runInParallel( new HyperlinkUpdateRunnable( requestPane, location ) );
	}

	private class HyperlinkUpdateRunnable
		implements Runnable
	{
		private RequestPane requestPane;
		private String location;

		private HyperlinkUpdateRunnable( RequestPane requestPane, String location )
		{
			this.requestPane = requestPane;
			this.location = location;
		}

		public void run()
		{
			if ( location.indexOf( "pics.communityofloathing.com" ) != -1 )
			{
				HyperlinkAdapter.this.handleInternalLink( location );
				return;
			}
			else if ( location.startsWith( "http://" ) || location.startsWith( "https://" ) )
			{
				RelayLoader.openSystemBrowser( location );
				return;
			}
			else if ( location.startsWith( "javascript:" ) && ( location.indexOf( "submit()" ) == -1 || location.indexOf( "messageform" ) != -1 ) )
			{
				InputFieldUtilities.alert( "Ironically, Java does not support Javascript." );
				return;
			}
			else if ( location.indexOf( "submit()" ) == -1 )
			{
				HyperlinkAdapter.this.handleInternalLink( location );
				return;
			}

			// If it's an attempt to submit an adventure form,
			// examine the location string to see which form is
			// being submitted and submit it manually.

			String[] locationSplit = location.split( "\\." );
			String formId = "\"" + locationSplit[ locationSplit.length - 2 ] + "\"";

			String editorText = requestPane.getText();
			int formIndex = editorText.indexOf( formId );

			String locationText =
				editorText.substring(
					editorText.lastIndexOf( "<form", formIndex ), editorText.toLowerCase().indexOf(
						"</form>", formIndex ) );

			Matcher inputMatcher = Pattern.compile( "<input.*?>" ).matcher( locationText );

			String lastInput;
			int patternIndex;
			Matcher actionMatcher, nameMatcher, valueMatcher;
			StringBuffer inputString = new StringBuffer();

			// Determine the action associated with the
			// form -- this is used for the URL.

			patternIndex = 0;
			do
			{
				actionMatcher = HyperlinkAdapter.ACTION_PATTERNS[ patternIndex ].matcher( locationText );
			}
			while ( !actionMatcher.find() && ++patternIndex < 3 );

			// Figure out which inputs need to be submitted.
			// This is determined through the existing HTML,
			// looking at preset values only.

			while ( inputMatcher.find() )
			{
				lastInput = inputMatcher.group();

				// Each input has a name associated with it.
				// This should be determined first.

				patternIndex = 0;
				do
				{
					nameMatcher = HyperlinkAdapter.NAME_PATTERNS[ patternIndex ].matcher( lastInput );
				}
				while ( !nameMatcher.find() && ++patternIndex < 3 );

				// Each input has a name associated with it.
				// This should be determined next.

				patternIndex = 0;
				do
				{
					valueMatcher = HyperlinkAdapter.VALUE_PATTERNS[ patternIndex ].matcher( lastInput );
				}
				while ( !valueMatcher.find() && ++patternIndex < 3 );

				// Append the latest input's name and value to
				// the complete input string.

				inputString.append( inputString.length() == 0 ? '?' : '&' );

				inputString.append( StringUtilities.getURLEncode( nameMatcher.group( 1 ) ) );
				inputString.append( '=' );
				inputString.append( StringUtilities.getURLEncode( valueMatcher.group( 1 ) ) );
			}

			// Now that the entire form string is known, handle
			// the appropriate internal link.

			String targetLocation = actionMatcher.group( 1 ) + inputString.toString();
			HyperlinkAdapter.this.handleInternalLink( targetLocation );
		}
	}

	public void handleInternalLink( String location )
	{
		RelayLoader.openSystemBrowser( location );
	}
}
