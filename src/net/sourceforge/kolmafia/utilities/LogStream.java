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

package net.sourceforge.kolmafia.utilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Date;

import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.StaticEntity;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class LogStream
	extends PrintStream
	implements Runnable
{
	private File proxy;

	public static final PrintStream openStream( final String filename, final boolean forceNewFile )
	{
		return LogStream.openStream( new File( UtilityConstants.ROOT_LOCATION, filename ), forceNewFile );
	}

	public static final PrintStream openStream( final File file, final boolean forceNewFile )
	{
		return LogStream.openStream( file, forceNewFile, "UTF-8" );
	}

	public static final PrintStream openStream( final File file, final boolean forceNewFile, final String encoding )
	{
		OutputStream ostream = DataUtilities.getOutputStream( file, !forceNewFile );
		PrintStream pstream = openStream( ostream, encoding );
		
		if ( !( pstream instanceof LogStream ) )
		{
			return pstream;
		}

		LogStream newStream = (LogStream) pstream;

		if ( file.getName().startsWith( "DEBUG" ) )
		{
			if ( KoLDesktop.instanceExists() )
			{
				newStream.proxy = file;
				SwingUtilities.invokeLater( (LogStream) newStream );
			}

			newStream.println();
			newStream.println();
			newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			StringBuffer versionData = new StringBuffer();
			versionData.append( StaticEntity.getVersion( true ) );
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
			newStream.println( " Please note: do not post this log in the KoLmafia thread. If you" );
			newStream.println( " would like the dev team to look at it, please write a bug report" );
			newStream.println( " at kolmafia.us. Include specific information about what you were " );
			newStream.println( " doing when you made this and include the log as an attachment." );
			newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			newStream.println( " Timestamp: " + ( new Date() ).toString() );
			newStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			newStream.println();
			newStream.println();
		}

		return newStream;
	}
	
	public static final PrintStream openStream( final OutputStream ostream, final String encoding )
	{
		try
		{
			return new LogStream( ostream, encoding );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return System.out;
		}
	}

	public void run()
	{
		KoLDesktop.getInstance().getRootPane().putClientProperty(
			"Window.documentFile", this.proxy );
	}

	private LogStream( final OutputStream ostream, final String encoding )
		throws IOException
	{
		super( ostream, true, encoding );
	}
}
