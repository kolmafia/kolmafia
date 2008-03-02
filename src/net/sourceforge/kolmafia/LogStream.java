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

package net.sourceforge.kolmafia;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class LogStream
	extends PrintStream
{
	public static final LogStream openStream( final String filename, final boolean forceNewFile )
	{
		return LogStream.openStream( new File( UtilityConstants.ROOT_LOCATION, filename ), forceNewFile );
	}

	public static final LogStream openStream( final File file, final boolean forceNewFile )
	{
		LogStream newStream = null;

		try
		{
			OutputStream ostream = DataUtilities.getOutputStream( file, !forceNewFile );
			newStream = new LogStream( ostream );

			if ( file.getName().startsWith( "DEBUG" ) )
			{
				newStream.println();
				newStream.println();
				newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

				StringBuffer versionData = new StringBuffer();
				versionData.append( StaticEntity.getVersion() );
				versionData.append( ", " );
				versionData.append( System.getProperty( "os.name" ) );
				versionData.append( ", Java " );
				versionData.append( System.getProperty( "java.version" ) );

				int leftIndent = ( 66 - versionData.length() ) / 2;
				for ( int i = 0; i < leftIndent; ++i )
				{
					versionData.insert( 0, ' ' );
				}

				newStream.println( versionData.toString() );

				newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
				newStream.println( " Please note: do not post these logs in the KoLmafia thread.  If " );
				newStream.println( " you would like us to look at the log, please instead email logs " );
				newStream.println( " to holatuwol@hotmail.com using the subject \"KoLmafia Debug Log\" " );
				newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
				newStream.println( " Timestamp: " + ( new Date() ).toString() );
				newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
				newStream.println();
				newStream.println();
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		return newStream;
	}

	private LogStream( final OutputStream ostream )
		throws IOException
	{
		super( ostream, true, "ISO-8859-1" );
	}
}
