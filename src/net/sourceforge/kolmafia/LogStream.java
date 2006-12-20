/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LogStream extends PrintStream implements KoLConstants
{
	/**
	 * Utility method which indicates whether or not the given filename
	 * indicates that the log should be appended to or not.
	 */

	private static boolean shouldAppend( String fileName )
	{
		return (fileName.indexOf( "buffs" ) != -1 && fileName.indexOf( "_200" ) != -1) ||
			fileName.indexOf( "sessions" ) != -1 || fileName.indexOf( "DEBUG" ) != -1;
	}

	/**
	 * Constructs a new <code>LogStream</code> which will append all
	 * log data to the file of the specified name.  Note that the
	 * file must exist prior to calling this method.
	 *
	 * @param	fileName	The name of the file used as a log
	 * @throws	FileNotFoundException	The file does not exist
	 */

	public LogStream( String fileName ) throws IOException
	{	this( new File( fileName ), shouldAppend( fileName ) );
	}

	/**
	 * Constructs a new <code>LogStream</code> which will append all
	 * log data to the specified file.  Note that the file must exist
	 * prior to calling this method.
	 *
	 * @param	file	The file used as a log
	 * @throws	FileNotFoundException	The file does not exist
	 */

	public LogStream( File file ) throws IOException
	{	this( file, shouldAppend( file.getAbsolutePath() ) );
	}


	public LogStream( File file, boolean append ) throws IOException
	{
		this( new FileOutputStream( file, append ) );

		if ( file.getName().endsWith( "log" ) || file.getParent() != null && file.getParent().indexOf( "sessions" ) != -1 )
		{
			println();
			println();
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			StringBuffer versionData = new StringBuffer();
			versionData.append( VERSION_NAME );
			versionData.append( ", " );
			versionData.append( System.getProperty( "os.name" ) );
			versionData.append( ", Java " );
			versionData.append( System.getProperty( "java.version" ) );

			int leftIndent = (66 - versionData.length()) / 2;
			for ( int i = 0; i < leftIndent; ++i )
				versionData.insert( 0, ' ' );
			println( versionData.toString() );

			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println( " Please note: do not post these logs in the KoLmafia thread.  If " );
			println( " you would like us to look at the log, please instead email logs " );
			println( " to holatuwol@hotmail.com using the subject \"KoLmafia Debug Log\" " );
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println();
			println();
		}
	}

	public LogStream( OutputStream ostream ) throws IOException
	{	super( ostream, true, "ISO-8859-1" );
	}
}
