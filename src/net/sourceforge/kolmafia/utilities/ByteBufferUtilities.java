package net.sourceforge.kolmafia.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;

public class ByteBufferUtilities
{
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[ 0 ];

	private static final ArrayList<byte[]> BYTEARRAYS = new ArrayList<byte[]>();
	private static final ArrayList<Boolean> BYTEARRAYS_STATUS = new ArrayList<Boolean>();

	private static final ArrayList<ByteArrayOutputStream> BYTESTREAMS = new ArrayList<ByteArrayOutputStream>();
	private static final ArrayList<Boolean> BYTESTREAMS_STATUS = new ArrayList<Boolean>();

	public static byte[] read( File file )
	{
		try
		{
			FileInputStream istream = new FileInputStream( file );

			return ByteBufferUtilities.read( istream );
		}
		catch ( IOException e )
		{
			return EMPTY_BYTE_ARRAY;
		}
	}

	public static byte[] read( InputStream istream )
	{
		if ( istream == null )
		{
			return EMPTY_BYTE_ARRAY;
		}

		ByteArrayOutputStream ostream = ByteBufferUtilities.getOutputStream();
		ByteBufferUtilities.read( istream, ostream );
		byte[] data = ostream.toByteArray();
		ByteBufferUtilities.returnOutputStream( ostream );

		return data;
	}

	public static void read( InputStream istream, OutputStream ostream )
	{
		if ( istream == null )
		{
			return;
		}

		byte[] buffer = ByteBufferUtilities.getBuffer();
		int availableBytes = 0;

		try
		{
			while ( ( availableBytes = istream.read( buffer ) ) != -1 )
			{
				ostream.write( buffer, 0, availableBytes );
			}
		}
		catch ( IOException e )
		{
		}

		ByteBufferUtilities.returnBuffer( buffer );

		try
		{
			ostream.flush();
		}
		catch ( IOException e )
		{
		}

		try
		{
			istream.close();
		}
		catch ( IOException e )
		{
		}
	}

	private synchronized static byte[] getBuffer()
	{
		for ( int i = 0; i < ByteBufferUtilities.BYTEARRAYS_STATUS.size(); ++i )
		{
			if ( ByteBufferUtilities.BYTEARRAYS_STATUS.get( i ) == Boolean.FALSE )
			{
				ByteBufferUtilities.BYTEARRAYS_STATUS.set( i, Boolean.TRUE );
				return ByteBufferUtilities.BYTEARRAYS.get( i );
			}
		}

		byte[] buffer = new byte[ 8192 ];

		ByteBufferUtilities.BYTEARRAYS.add( buffer );
		ByteBufferUtilities.BYTEARRAYS_STATUS.add( Boolean.TRUE );

		return buffer;
	}

	private static void returnBuffer( byte[] buffer )
	{
		for ( int i = 0; i < ByteBufferUtilities.BYTEARRAYS_STATUS.size(); ++i )
		{
			if ( ByteBufferUtilities.BYTEARRAYS.get( i ) == buffer )
			{
				ByteBufferUtilities.BYTEARRAYS_STATUS.set( i, Boolean.FALSE );
				return;
			}
		}
	}

	private synchronized static ByteArrayOutputStream getOutputStream()
	{
		for ( int i = 0; i < ByteBufferUtilities.BYTESTREAMS_STATUS.size(); ++i )
		{
			if ( ByteBufferUtilities.BYTESTREAMS_STATUS.get( i ) == Boolean.FALSE )
			{
				ByteBufferUtilities.BYTESTREAMS_STATUS.set( i, Boolean.TRUE );
				return ByteBufferUtilities.BYTESTREAMS.get( i );
			}
		}

		ByteArrayOutputStream ostream = new ByteArrayOutputStream();

		ByteBufferUtilities.BYTESTREAMS.add( ostream );
		ByteBufferUtilities.BYTESTREAMS_STATUS.add( Boolean.TRUE );

		return ostream;
	}

	private static void returnOutputStream( ByteArrayOutputStream outputStream )
	{
		outputStream.reset();

		for ( int i = 0; i < ByteBufferUtilities.BYTEARRAYS_STATUS.size(); ++i )
		{
			if ( ByteBufferUtilities.BYTESTREAMS.get( i ) == outputStream )
			{
				ByteBufferUtilities.BYTESTREAMS_STATUS.set( i, Boolean.FALSE );
				return;
			}
		}
	}

}
