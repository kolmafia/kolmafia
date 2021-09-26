package net.sourceforge.kolmafia.utilities;

import java.io.PrintStream;

/**
 * A <code>NullStream</code> is the rough equivalent of redirecting all output to <code>/dev/null</code> on some
 * variants of *NIX. The effect is that nothing gets put onto any output stream. This can be used by debugging
 * mechanisms to eliminate all output, should the overhead of function calls not be severe. Note that checking state
 * variables is probably still the best way to handle debug functionality; this is merely a sub-optimal alternative.
 */

public class NullStream
	extends PrintStream
{
	private boolean errorState;

	public static final NullStream INSTANCE = new NullStream();

	public NullStream()
	{
		super( System.out );
		this.errorState = false;
	}

	@Override
	public boolean checkError()
	{
		return this.errorState;
	}

	@Override
	public void close()
	{
	}

	@Override
	public void flush()
	{
	}

	@Override
	public void print( final boolean b )
	{
	}

	@Override
	public void print( final char c )
	{
	}

	@Override
	public void print( final char[] s )
	{
	}

	@Override
	public void print( final float d )
	{
	}

	@Override
	public void print( final double f )
	{
	}

	@Override
	public void print( final int i )
	{
	}

	@Override
	public void print( final long l )
	{
	}

	@Override
	public void print( final String s )
	{
	}

	@Override
	public void println()
	{
	}

	@Override
	public void println( final boolean x )
	{
	}

	@Override
	public void println( final char x )
	{
	}

	@Override
	public void println( final char[] x )
	{
	}

	@Override
	public void println( final float x )
	{
	}

	@Override
	public void println( final double x )
	{
	}

	@Override
	public void println( final int x )
	{
	}

	@Override
	public void println( final long x )
	{
	}

	@Override
	public void println( final Object x )
	{
	}

	@Override
	public void println( final String x )
	{
	}

	@Override
	public void setError()
	{
		this.errorState = true;
	}

	@Override
	public void write( final byte[] b )
	{
	}

	@Override
	public void write( final byte[] buf, final int off, final int len )
	{
	}

	@Override
	public void write( final int b )
	{
	}
}
