/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import java.io.ByteArrayOutputStream;
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

public class DataFileCache
{
	private static Map dataFileTimestampCache = new HashMap();
	private static Map dataFileDataCache = new HashMap();

	public static File getFile( String filename )
	{
		if ( filename.startsWith( "http" ) )
		{
			return null;
		}

		File f = new File( KoLConstants.SCRIPT_LOCATION, filename );
		if ( checkFile( f ) )
		{
			return f;
		}

		f = new File( UtilityConstants.DATA_LOCATION, filename );
		if ( checkFile( f ) )
		{
			return f;
		}

		f = new File( UtilityConstants.ROOT_LOCATION, filename );
		if ( checkFile( f ) )
		{
			return f;
		}

		if ( filename.endsWith( ".dat" ) )
		{
			return DataFileCache.getFile( filename.substring( 0, filename.length() - 4 ) + ".txt" );
		}

		return new File( KoLConstants.DATA_LOCATION, filename );
	}

	private static boolean checkFile( File f )
	{
		if ( !f.exists() )
		{
			return false;
		}

		boolean validFile = false;
		f = f.getParentFile();

		while ( f != null )
		{
			if ( f.getName().equals( ".." ) )
			{
				return false;
			}

			if ( f.equals( KoLConstants.SETTINGS_LOCATION ) )
			{
				return false;
			}

			if ( f.equals( KoLConstants.ROOT_LOCATION ) )
			{
				validFile = true;
			}

			f = f.getParentFile();
		}

		return validFile;
	}

	public static BufferedReader getReader( final String filename )
	{
		if ( filename.startsWith( "http" ) )
		{
			return DataUtilities.getReader( "", filename );
		}

		byte[] data = DataFileCache.getBytes( filename );
		return DataUtilities.getReader( new ByteArrayInputStream( data ) );
	}

	public static byte[] getBytes( final String filename )
	{
		File input = DataFileCache.getFile( filename );

		long modifiedTime = input.lastModified();

		Long cacheModifiedTime = (Long) dataFileTimestampCache.get( filename );

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
				return new byte[0];
			}
		}
		else
		{
			istream = DataUtilities.getInputStream( "data", filename );

			if ( istream instanceof ByteArrayInputStream )
			{
				istream = DataUtilities.getInputStream( "", filename );
			}
		}

		try
		{
			ByteArrayOutputStream ostream = new ByteArrayOutputStream();

			int length;
			byte[] buffer = new byte[ 8192 ];

			while ( ( length = istream.read( buffer ) ) > 0 )
			{
				ostream.write( buffer, 0, length );
			}

			istream.close();

			byte[] data = ostream.toByteArray();

			DataFileCache.dataFileTimestampCache.put( filename, new Long( modifiedTime ) );
			DataFileCache.dataFileDataCache.put( filename, data );

			return data;
		}
		catch ( Exception e )
		{
			return new byte[0];
		}
	}

	public static Value printBytes( final String filename, final byte[] data )
	{
		if ( filename.startsWith( "http" ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		File output = DataFileCache.getFile( filename );

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

		DataFileCache.dataFileTimestampCache.put( filename, new Long( output.lastModified() ) );
		DataFileCache.dataFileDataCache.put( filename, data );
		return DataTypes.TRUE_VALUE;
	}

}
