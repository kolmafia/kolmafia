/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
