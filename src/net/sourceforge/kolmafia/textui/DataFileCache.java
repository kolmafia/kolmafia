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

import java.util.HashMap;
import java.util.Map;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class DataFileCache
{
	private static RollingLinkedList recentlyUsedList = new RollingLinkedList( 500 ); 
	private static Map<String, Long> dataFileTimestampCache = new HashMap<String, Long>();
	private static Map<String, byte[]> dataFileDataCache = new HashMap<String, byte[]>();

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

		File[] parents;

		if ( !readOnly && filename.endsWith( ".ash" ) )
		{
			parents = new File[]
			{
				UtilityConstants.DATA_LOCATION
			};
		}
		else
		{
			parents = new File[]
			{
				KoLConstants.SCRIPT_LOCATION,
				KoLConstants.RELAY_LOCATION,
				UtilityConstants.DATA_LOCATION,
				UtilityConstants.ROOT_LOCATION
			};
		}

		for ( int i = 0; i < parents.length; ++i )
		{
			File file = new File( parents[ i ], filename );
			if ( checkFile( parents, file, true ) )
			{
				return file;
			}
		}
		
		File file = new File( KoLConstants.DATA_LOCATION, filename );
		if ( checkFile( parents, file, false ) )
		{
			return file;
		}
		
		filename = filename.substring( filename.lastIndexOf( "\\" ) + 1 );
		filename = filename.substring( filename.lastIndexOf( "/" ) + 1 );

		file = new File( KoLConstants.DATA_LOCATION, filename );

		return file;
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
		if ( filename.startsWith( "http://" ) )
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
			return (byte[]) dataFileDataCache.get( filename );
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
		Object recentlyUsedCheck = DataFileCache.recentlyUsedList.update( filename );
		
		if ( recentlyUsedCheck != null )
		{
			DataFileCache.dataFileTimestampCache.remove( filename );
			DataFileCache.dataFileDataCache.remove( filename );
		}
	
		DataFileCache.dataFileTimestampCache.put( filename, new Long( modifiedTime ) );
		DataFileCache.dataFileDataCache.put( filename, data );
	}
}
