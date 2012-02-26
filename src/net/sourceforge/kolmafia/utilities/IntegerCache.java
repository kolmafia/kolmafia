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

public class IntegerCache
{
	private static final int MIN_VALUE = -2;
	private static final int MAX_VALUE = 12000;

	private static final int RANGE = ( IntegerCache.MAX_VALUE - IntegerCache.MIN_VALUE ) + 1;

	private static final Integer[] CACHE = new Integer[ IntegerCache.RANGE ];

	private static int cacheHits = 0;
	private static int cacheMissHighs = 0;
	private static int cacheMissLows = 0;

	static
	{
		for ( int i = 0; i < IntegerCache.RANGE; ++i )
		{
			IntegerCache.CACHE[ i ] = new Integer( IntegerCache.MIN_VALUE + i );
		}
	}

	public static final int getCacheHits()
	{
		return IntegerCache.cacheHits;
	}

	public static final int getCacheMissLows()
	{
		return IntegerCache.cacheMissLows;
	}

	public static final int getCacheMissHighs()
	{
		return IntegerCache.cacheMissHighs;
	}

	public static final Integer valueOf( int i )
	{
		if ( i < IntegerCache.MIN_VALUE )
		{
			++cacheMissLows;
			return new Integer( i );
		}

		if ( i > IntegerCache.MAX_VALUE )
		{
			++cacheMissHighs;
			return new Integer( i );
		}

		++cacheHits;
		return IntegerCache.CACHE[ i - IntegerCache.MIN_VALUE ];
	}
}
