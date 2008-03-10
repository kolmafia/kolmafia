package net.sourceforge.kolmafia.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A special kind of ByteArrayOutputStream which provides access to the buffer it uses. This allows you to
 * instantiate ByteArrayInputStream objects without having to allocate too much memory.
 */

public class ByteArrayStream
	extends ByteArrayOutputStream
{
	public ByteArrayStream()
	{
	}

	public ByteArrayStream( final int size )
	{
		super( size );
	}

	public byte[] getCurrentBuffer()
	{
		return this.buf;
	}

	public ByteArrayInputStream getByteArrayInputStream()
	{
		return new ByteArrayInputStream( this.buf, 0, this.count );
	}
}