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

package net.sourceforge.kolmafia.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class FileUtilities
{
	private static final Pattern FILEID_PATTERN = Pattern.compile( "(\\d+)\\." );

	public static final BufferedReader getReader( final String filename, final boolean allowOverride )
	{
		return FileUtilities.getReader( DataUtilities.getReader( KoLConstants.DATA_DIRECTORY, filename, allowOverride ) );
	}

	public static final BufferedReader getReader( final String filename )
	{
		return FileUtilities.getReader( DataUtilities.getReader( KoLConstants.DATA_DIRECTORY, filename ) );
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
		BufferedReader reader = FileUtilities.getReader( DataUtilities.getReader( KoLConstants.DATA_DIRECTORY, filename, true ) );

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

		reader = DataUtilities.getReader( KoLConstants.DATA_DIRECTORY, filename, false );
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
		// Assume that any file with content is good
		FileUtilities.downloadFile( remote, local, false );
	}

	public static final void downloadFile( final String remote, final File local, boolean probeLastModified )
	{
		if ( !local.exists() || local.length() == 0 )
		{
			// If we don't have it cached, don't probe
			probeLastModified = false;
		}
		else if ( !probeLastModified )
		{
			// If we are not probing, assume that a file with content is good
			return;
		}

		HttpURLConnection connection;
		try
		{
			connection = (HttpURLConnection) new URL( null, remote ).openConnection();
		}
		catch ( IOException e )
		{
			return;
		}

		if ( probeLastModified )
		{
			//This isn't perfect, because the user could've modified the file themselves, but it's better than nothing.
			connection.setIfModifiedSince( local.lastModified() );
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

		if ( RequestLogger.isDebugging() )
		{
			GenericRequest.printRequestProperties( remote, (HttpURLConnection)connection );
		}

		if ( RequestLogger.isTracing() )
		{
			RequestLogger.trace( "Requesting: " + remote );
		}
		
		InputStream istream = null;
		try
		{
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();
			switch ( responseCode ) {
			case 200:
				istream = connection.getInputStream();
				break;
			case 304:
				//Requested variant not modified, fall through.
				if ( RequestLogger.isDebugging() )
				{
					RequestLogger.updateDebugLog( "Not modified: " + remote );
				}

				if ( RequestLogger.isTracing() )
				{
					RequestLogger.trace( "Not modified: " + remote );
				}
			default:
				if ( RequestLogger.isDebugging() )
				{
					RequestLogger.updateDebugLog( "Server returned response code " + responseCode + " (" + responseMessage + ")" );
				}
				return;
			}
		}
		catch ( IOException e )
		{
			return;
		}

		if ( RequestLogger.isDebugging() )
		{
			GenericRequest.printHeaderFields( remote, (HttpURLConnection)connection );
		}

		if ( RequestLogger.isTracing() )
		{
			RequestLogger.trace( "Retrieved: " + remote );
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
			else if ( remote.endsWith( ".gif" ) )
			{
				byte[] bytes = ByteBufferUtilities.read( istream );
				String signature = new String( bytes, 0, 3 );
				// Certain firewalls return garbage if they
				// prevent you from getting to the image
				// server. Don't cache that.
				if ( signature.equals( "GIF" ) )
				{
					ostream.write( bytes, 0, bytes.length );
				}
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
		else
		{
			String lastModifiedString = ((HttpURLConnection)connection).getHeaderField( "Last-Modified" );
			long lastModified = StringUtilities.parseDate( lastModifiedString );
			if ( lastModified > 0 )
			{
				local.setLastModified( lastModified );
			}
		}
	}

	/**
	 * Downloads the given file from the KoL images server and stores it locally.
	 */

	private static final String localImageName( final String filename )
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
		return localname;
	}

	public static final File imageFile( final String filename )
	{
		return new File( KoLConstants.IMAGE_LOCATION, FileUtilities.localImageName( filename ) );
	}

	public static final File downloadImage( final String filename )
	{
		String localname = FileUtilities.localImageName( filename );
		File localfile = new File( KoLConstants.IMAGE_LOCATION, localname );

		try
		{
			if ( !localfile.exists() || localfile.length() == 0 )
			{
				if ( JComponentUtilities.getImage( localname ) != null )
				{
					loadLibrary( KoLConstants.IMAGE_LOCATION, KoLConstants.IMAGE_DIRECTORY, localname );
				}
				else
				{
					downloadFile( filename, localfile );
				}
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

	private static List<Object> getPathList( File f )
	{
		List<Object> l = new ArrayList<Object>();
		File r;
		try
		{
			r = f.getCanonicalFile();
			while ( r != null )
			{
				l.add( r.getName() );
				r = r.getParentFile();
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			l = null;
		}
		return l;
	}

	/**
	 * figure out a string representing the relative path of 'f' with respect to 'r'
	 * 
	 * @param r home path
	 * @param f path of file
	 */
	private static String matchPathLists( List<Object> r, List<Object> f )
	{
		int i;
		int j;
		String s;
		// start at the beginning of the lists
		// iterate while both lists are equal
		s = "";
		i = r.size() - 1;
		j = f.size() - 1;

		// first eliminate common root
		while ( ( i >= 0 ) && ( j >= 0 ) && ( r.get( i ).equals( f.get( j ) ) ) )
		{
			i-- ;
			j-- ;
		}

		// for each remaining level in the home path, add a ..
		for ( ; i >= 0; i-- )
		{
			s += ".." + File.separator;
		}

		// for each level in the file path, add the path
		for ( ; j >= 1; j-- )
		{
			s += f.get( j ) + File.separator;
		}

		// file name
		s += f.get( j );
		return s;
	}

	/**
	 * get relative path of File 'f' with respect to 'home' directory example : home = /a/b/c f = /a/d/e/x.txt s =
	 * getRelativePath(home,f) = ../../d/e/x.txt
	 * 
	 * @param home base path, should be a directory, not a file, or it doesn't make sense
	 * @param f file to generate path for
	 * @return path from home to f as a string
	 */
	public static String getRelativePath( File home, File f )
	{
		List<Object> homelist;
		List<Object> filelist;
		String s;
		
		homelist = getPathList( home );
		filelist = getPathList( f );
		s = matchPathLists( homelist, filelist );

		return s;
	}
}
