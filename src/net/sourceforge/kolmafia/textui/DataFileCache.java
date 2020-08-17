/**
 * Copyright (c) 2005-2020, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class DataFileCache
{
	private static final RollingLinkedList<String> recentlyUsedList = new RollingLinkedList<>( 500 ); 
	private static final Map<String, Long> dataFileTimestampCache = Collections.synchronizedMap(new HashMap<String, Long>());
	private static final Map<String, byte[]> dataFileDataCache = Collections.synchronizedMap(new HashMap<String, byte[]>());

	public static void clearCache()
	{
		DataFileCache.recentlyUsedList.clear();
		DataFileCache.dataFileTimestampCache.clear();
		DataFileCache.dataFileDataCache.clear();
	}
	
	public static File getFile( String filename, boolean readOnly )
	{
		if ( filename.startsWith( "http://" ) )
		{
			return null;
		}

		filename = filename.substring( filename.lastIndexOf( "\\" ) + 1 );

		File[] parents;

		if ( !readOnly && filename.endsWith( ".ash" ) )
		{
			parents = new File[]
			{
				KoLConstants.DATA_LOCATION
			};
		}
		else
		{
			parents = new File[]
			{
				KoLConstants.SCRIPT_LOCATION,
				KoLConstants.RELAY_LOCATION,
				KoLConstants.DATA_LOCATION,
				KoLConstants.SESSIONS_LOCATION,
			};
		}

		for ( int i = 0; i < parents.length; ++i )
		{
			File file = new File( parents[ i ], filename );
			if ( checkFile( parents, file, true ) )
			{
				try
				{
					if ( file.getCanonicalPath().startsWith( parents[ i ].getCanonicalPath() ) )
					{
						return file;
					}
					else
					{
						KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, filename + " is not within KoLmafia's directories." );
						return null;
					}
				}
				catch ( IOException e )
				{
					return null;
				}
				
			}
		}

		File file = new File( KoLConstants.ROOT_LOCATION, filename );
		if ( file.exists() && file.getParent().equals( KoLConstants.ROOT_LOCATION.getAbsolutePath() ) )
		{
			return file;
		}

		file = new File( KoLConstants.DATA_LOCATION, filename );
		try
		{
			if ( file.getCanonicalPath().startsWith( KoLConstants.DATA_LOCATION.getCanonicalPath() ) )
			{
				return file;
			}
		}
		catch ( IOException e )
		{
			return null;
		}

		KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, filename + " is not within KoLmafia's directories." );
		return null;
	}

	private static boolean checkFile( File[] parents, File file, boolean checkExists )
	{
		if ( checkExists && !file.exists() )
		{
			return false;
		}

		file = file.getAbsoluteFile();

		try
		{
			File settings = KoLConstants.SETTINGS_LOCATION.getCanonicalFile();
			
			if ( settings.equals( file.getCanonicalFile().getParentFile() ) )
			{
				return false;
			}
			
			while ( file != null )
			{
				for ( int i = 0; i < parents.length; ++i )
				{
					if ( file.equals( parents[ i ] ) )
					{
						return true;
					}
				}
				
				file = file.getParentFile();
			}
		}
		catch ( Exception e )
		{
		}

		return false;
	}

	public static BufferedReader getReader( final String filename )
	{
		if (filename.startsWith( "http://" ) || filename.startsWith( "https://" ))
		{
			return DataUtilities.getReader( "", filename );
		}
		byte[] data = DataFileCache.getBytes( filename );

		return DataUtilities.getReader( new ByteArrayInputStream( data ) );
	}

	public static byte[] getBytes( final String filename )
	{
		File input = DataFileCache.getFile( filename, true );

		if ( input == null )
		{
			return new byte[0];
		}

		long modifiedTime = input.lastModified();

		Long cacheModifiedTime = dataFileTimestampCache.get( filename );

		if ( cacheModifiedTime != null && cacheModifiedTime.longValue() == modifiedTime )
		{
			return dataFileDataCache.get( filename );
		}

		InputStream istream = null;

		if ( input.exists() )
		{
			try
			{
				istream = new FileInputStream( input );
			}
			catch ( IOException e )
			{
			}
		}
		
		if ( istream == null )
		{
			istream = DataUtilities.getInputStream( "data", filename );

			if ( istream instanceof ByteArrayInputStream )
			{
				istream = DataUtilities.getInputStream( "", filename );
			}
		}

		byte[] data = ByteBufferUtilities.read( istream );
		if (data == null)
		{
			//This check is here because a NPE was being thrown intermittently and data was the most likely candidate.
			//If the cause was a lack of synchronization then this message should never be displayed.  If it is displayed
			//then the hypothesis about synchronization (or the fix) was incorrect.  In any event we want to see this
			//message and the NPE if data is null.
			RequestLogger.printLine("getBytes returning null for file " + filename + ".");
		}
		DataFileCache.updateCache( filename, modifiedTime, data );
		return data;
	}

	public static Value printBytes( final String filename, final byte[] data )
	{
		File output = DataFileCache.getFile( filename, false );

		if ( output == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		if ( !output.exists() )
		{
			try
			{
				File parent = output.getParentFile();
				if ( parent != null )
				{
					parent.mkdirs();
				}

				output.createNewFile();
			}
			catch ( Exception e )
			{
				return DataTypes.FALSE_VALUE;
			}
		}

		try
		{
			FileOutputStream ostream = new FileOutputStream( output, false );
			ostream.write( data );
			ostream.close();
		}
		catch ( Exception e )
		{
			return DataTypes.FALSE_VALUE;
		}

		DataFileCache.updateCache( filename, output.lastModified(), data );
		return DataTypes.TRUE_VALUE;
	}

	private static void updateCache( String filename, long modifiedTime, byte[] data )
	{
		String recentlyUsedCheck = DataFileCache.recentlyUsedList.update( filename );
		
		if ( recentlyUsedCheck != null )
		{
			DataFileCache.dataFileTimestampCache.remove( filename );
			DataFileCache.dataFileDataCache.remove( filename );
		}
	
		DataFileCache.dataFileTimestampCache.put( filename, Long.valueOf( modifiedTime ) );
		DataFileCache.dataFileDataCache.put( filename, data );
	}
}
