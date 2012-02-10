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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.nio.channels.FileChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

public class FileUtilities
{
	private static final Pattern FILEID_PATTERN = Pattern.compile( "(\\d+)\\." );

	public static final BufferedReader getReader( final String filename )
	{
		return FileUtilities.getReader( DataUtilities.getReader( UtilityConstants.DATA_DIRECTORY, filename ) );
	}

	public static final BufferedReader getReader( final File file )
	{
		return FileUtilities.getReader( DataUtilities.getReader( file ) );
	}

	public static final BufferedReader getReader( final InputStream istream )
	{
		return FileUtilities.getReader( DataUtilities.getReader( istream ) );
	}

	private static final BufferedReader getReader( final BufferedReader reader )
	{
		String lastMessage = DataUtilities.getLastMessage();
		if ( lastMessage != null )
		{
			RequestLogger.printLine( lastMessage );
		}
		return reader;
	}

	public static final BufferedReader getVersionedReader( final String filename, final int version )
	{
		BufferedReader reader = FileUtilities.getReader( DataUtilities.getReader( UtilityConstants.DATA_DIRECTORY, filename, true ) );

		// If no file, no reader
		if ( reader == null )
		{
			return null;
		}

		// Read the version number

		String line = FileUtilities.readLine( reader );

		// Parse the version number and validate

		try
		{
			int fileVersion = StringUtilities.parseInt( line );

			if ( version == fileVersion )
			{
				return reader;
			}
			RequestLogger.printLine( "Incorrect version of \"" + filename + "\". Found " + fileVersion + " require " + version );
		}
		catch ( Exception e )
		{
			// Incompatible data file, use KoLmafia's internal
			// files instead.
		}

		// We don't understand this file format
		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Override file is wrong version. Get built-in file

		reader = DataUtilities.getReader( UtilityConstants.DATA_DIRECTORY, filename, false );
		// Don't forget to skip past its version number:
		FileUtilities.readLine( reader );
		return reader;
	}

	public static final String readLine( final BufferedReader reader )
	{
		if ( reader == null )
		{
			return null;
		}

		try
		{
			String line;

			// Read in all of the comment lines, or until
			// the end of file, whichever comes first.

			while ( ( line = reader.readLine() ) != null && ( line.startsWith( "#" ) || line.length() == 0 ) )
			{
				;
			}

			// If you've reached the end of file, then
			// return null.  Otherwise, return the line
			// that's been split on tabs.

			return line;
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	public static final String[] readData( final BufferedReader reader )
	{
		if ( reader == null )
		{
			return null;
		}

		String line = readLine( reader );
		return line == null ? null : line.split( "\t", -1 );
	}

	public static final boolean loadLibrary( final File parent, final String directory, final String filename )
	{
		// Next, load the icon which will be used by KoLmafia
		// in the system tray.  For now, this will be the old
		// icon used by KoLmelion.

		File library = new File( parent, filename );

		if ( library.exists() )
		{
			if ( parent == KoLConstants.RELAY_LOCATION && !Preferences.getString( "lastRelayUpdate" ).equals(
				StaticEntity.getVersion() ) )
			{
				library.delete();
			}
			else
			{
				return true;
			}
		}

		InputStream istream = DataUtilities.getInputStream( directory, filename );
		
		byte[] data = ByteBufferUtilities.read( istream );
		OutputStream output = DataUtilities.getOutputStream( library );

		try
		{
			output.write( data );
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		try
		{
			output.close();
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		return true;
	}

	public static final void downloadFile( final String remote, final File local )
	{
		// Assume that a file with content is good
		if ( local.exists() && local.length() > 0 )
		{
			return;
		}

		URLConnection connection;
		try
		{
			connection = new URL( null, remote ).openConnection();
		}
		catch ( IOException e )
		{
			return;
		}

		if ( remote.startsWith( "http://pics.communityofloathing.com" ) )
		{
			Matcher idMatcher = FileUtilities.FILEID_PATTERN.matcher( local.getPath() );
			if ( idMatcher.find() )
			{
				connection.setRequestProperty(
					"Referer", "http://www.kingdomofloathing.com/showplayer.php?who=" + idMatcher.group( 1 ) );
			}
		}
		
		InputStream istream = null;
		try
		{
			istream = connection.getInputStream();
		}
		catch ( IOException e )
		{
			return;
		}

		OutputStream ostream = DataUtilities.getOutputStream( local );
		try
		{
			// If it's Javascript, then modify it so that
			// all the variables point to KoLmafia.
			if ( remote.endsWith( ".js" ) )
			{
				byte[] bytes = ByteBufferUtilities.read( istream ); 
				String text = new String( bytes );
				text = StringUtilities.globalStringReplace( text, "location.hostname", "location.host" );
				ostream.write( text.getBytes() );
			}
			else
			{
				ByteBufferUtilities.read( istream, ostream );
			}
		}
		catch ( IOException e )
		{
		}

		try
		{
			ostream.close();
		}
		catch ( IOException e )
		{
		}

		// Don't keep a 0-length file
		if ( local.exists() && local.length() == 0 )
		{
			local.delete();
		}
	}

	/**
	 * Downloads the given file from the KoL images server and stores it locally.
	 */

	public static final File downloadImage( final String filename )
	{
		if ( filename == null || filename.equals( "" ) )
		{
			return null;
		}

		String localname = filename.substring( filename.indexOf( "/", "http://".length() ) + 1 );
		if ( localname.startsWith( "albums/" ) )
		{
			localname = localname.substring( 7 );
		}

		File localfile = new File( UtilityConstants.IMAGE_LOCATION, localname );

		if ( !localfile.exists() || localfile.length() == 0 )
		{
			if ( JComponentUtilities.getImage( localname ) != null )
			{
				loadLibrary( UtilityConstants.IMAGE_LOCATION, UtilityConstants.IMAGE_DIRECTORY, localname );
			}
			else
			{
				downloadFile( filename, localfile );
			}
		}

		try
		{
			String localfilePath = localfile.getCanonicalPath();
			String imagePath = UtilityConstants.IMAGE_LOCATION.getCanonicalPath();

			if ( !localfilePath.startsWith( imagePath ) )
			{
				return null;
			}
		
			return localfile;
		}
		catch ( Exception e )
		{
			// This can happen whenever there is bad internet
			// or whenever the familiar is brand-new.

			return null;
		}
	}

	/**
	 * Copies a file.
	 */

	public static void copyFile( File source, File destination )
	{
		InputStream sourceStream = DataUtilities.getInputStream( source );
		OutputStream destinationStream = DataUtilities.getOutputStream( destination );

		if ( !( sourceStream instanceof FileInputStream ) || !( destinationStream instanceof FileOutputStream ) )
		{
			try
			{
				sourceStream.close();
			}
			catch ( IOException e )
			{
			}

			try
			{
				destinationStream.close();
			}
			catch ( IOException e )
			{
			}

			return;
		}

		FileChannel sourceChannel = ( (FileInputStream) sourceStream ).getChannel();
		FileChannel destinationChannel = ( (FileOutputStream) destinationStream ).getChannel();

		try
		{
			sourceChannel.transferTo( 0, sourceChannel.size(), destinationChannel );
		}
		catch ( IOException e )
		{
		}

		try
		{
			sourceStream.close();
		}
		catch ( IOException e )
		{
		}

		try
		{
			destinationStream.close();
		}
		catch ( IOException e )
		{
		}
	}
}
