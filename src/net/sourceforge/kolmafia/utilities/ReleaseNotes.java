/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import net.java.dev.spellcast.utilities.UtilityConstants;

public class ReleaseNotes
{
	private static final String LINE_BREAK = System.getProperty( "line.separator" );
	private static final File INPUT_LOCATION = new File( UtilityConstants.ROOT_LOCATION, "history.txt" );
	private static final File OUTPUT_LOCATION = new File( UtilityConstants.ROOT_LOCATION, "release.txt" );
	private static final String VIEW_REVISION_ROOT = "http://kolmafia.svn.sourceforge.net/viewvc/kolmafia?view=rev&revision=";

	public static void main( String [] args )
		throws Exception
	{
		if ( OUTPUT_LOCATION.exists() )
		{
			OUTPUT_LOCATION.delete();
		}

		OUTPUT_LOCATION.createNewFile();

		ArrayList revisionHistory = getRevisionHistory( INPUT_LOCATION, VIEW_REVISION_ROOT );
		Collections.sort( revisionHistory );

		PrintStream ostream = new PrintStream( OUTPUT_LOCATION );
		Iterator revisionIterator = revisionHistory.iterator();

		while ( revisionIterator.hasNext() )
		{
			ostream.println( revisionIterator.next() );
		}

		ostream.close();
	}

	private static ArrayList getRevisionHistory( File input, String viewRoot )
		throws Exception
	{
		ArrayList revisionHistory = new ArrayList();

		if ( !input.exists() )
		{
			return revisionHistory;
		}

		FileInputStream istream = new FileInputStream( input );

		byte [] available = new byte[ istream.available() ];
		istream.read( available );
		istream.close();

		String [] lines = new String( available ).split( "[\r\n]+" );

		int lineCount = lines.length;
		int lineNumber = 0;

		Revision currentRevision;

		while ( lineNumber < lineCount )
		{
			// Find the next revision in the input stream.

			while ( !lines[ lineNumber ].startsWith( "Revision" ) )
			{
				if ( ++lineNumber >= lineCount )
				{
					return revisionHistory;
				}
			}

			int spaceIndex = lines[ lineNumber ].indexOf( " " ) + 1;
			currentRevision = new Revision( lines[ lineNumber ].substring( spaceIndex ), viewRoot );
			revisionHistory.add( currentRevision );

			// Find the log messages.

			while ( !lines[ ++lineNumber ].startsWith( "Message" ) );

			while ( !lines[ ++lineNumber ].startsWith( "----" ) )
			{
				currentRevision.addMessage( lines[ lineNumber ] );
			}
		}

		return revisionHistory;
	}

	private static class Revision
		implements Comparable
	{
		private int revisionId;
		private StringBuffer contents;

		public Revision( String revision, String viewRoot )
		{
			this.revisionId = StringUtilities.parseInt( revision );
			this.contents = new StringBuffer();

			contents.append( "Revision: [url=" );
			contents.append( viewRoot );
			contents.append( revision );
			contents.append( "]" );
			contents.append( revision );
			contents.append( "[/url]" );
			contents.append( LINE_BREAK );
		}

		public void addMessage( String message )
		{
			contents.append( message );
			contents.append( LINE_BREAK );
		}

		public String toString()
		{
			return contents.toString();
		}

		public int compareTo( Object o )
		{
			return this.revisionId - ((Revision)o).revisionId;
		}
	}
}