/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Random;

/**
 * Constructed referencing the pseudocode available
 * at https://en.wikipedia.org/wiki/Mersenne_Twister#Pseudocode
 * This is mostly derived from PHP's source code by Gausie.
 */

public class PHPMTRandom extends Random {
  public static final long serialVersionUID = 0l;

  static int STATE_LENGTH = 624;
  static int PERIOD = 397;
  static long MAGIC_CONSTANT = 0x9908b0dfL;
  static long MAX_UNSIGNED = 0xFFFFFFFFL;

  // Mask all but highest bit of u
  static final long hiBit( long u ) { return u & 0x80000000L; }

  // Mask all bit lowest bit of u
  static final long loBit( long u ) { return u & 0x00000001L; }

  // Mask the highest bit of u
  static final long loBits( long u ) { return u & 0x7FFFFFFFL; }

  // Move hi bit of u to hi bit of v
  static final long mixBits( long u, long v ) { return hiBit( u ) | loBits( v ); }

	static final long twist( long m, long u, long v )
  {
		return ( m ^ ( mixBits( u, v ) >> 1 ) ^ ( ( MAX_UNSIGNED * ( loBit( u ) ) ) & MAGIC_CONSTANT ) ) ;
	}
 
	static final long temper( long value ){
		value ^= (value >> 11);
		value ^= (value <<  7) & 2_636_928_640L;
		value ^= (value << 15) & 4_022_730_752L;
		return ( value ^ (value >> 18) );
	}

	ArrayList<Long> state;
	int index;

  public int next( int bits )
  {
    if ( index >= state.size() )
    {
      reload();
    }

    long value = state.get( index++ );
    return (int) ( temper( value ) >> 1 );
  }

  public double nextDouble()
  {
    return nextInt() / ( Integer.MAX_VALUE + 1.0 );
  }

  public int nextInt( final int max )
  {
    return nextInt( 0, max );
  }

  public int nextInt( final int min, final int max )
  {
    double clamped = ( max - min + 1.0 ) * nextDouble();
    int val = min + (int) clamped;
    return val;
  }

  void initialize( final long seed )
  {
    state.clear();
    state.add( seed & Integer.MAX_VALUE );

    for ( int i = 1; i < STATE_LENGTH; i++ )
    {
      long prev = state.get( i - 1 );
      long value = ( ( 1_812_433_253L * ( prev ^ ( prev >> 30 ) ) ) + i ) & 4_294_967_295L;
      state.add( value );
    }

    index = state.size();
  }

  void reload()
  {
    for ( int i = 0; i < STATE_LENGTH; i++ )
    {
      long value = twist( state.get( i + PERIOD ), state.get( i ), state.get( i + 1 ) );
      state.add( value );
    }
  }
 
  synchronized public void setSeed( long seed )
  {
    if ( state == null )
    {
      state = new ArrayList<>();
    }

    initialize( seed );
    reload();
  }

  public PHPMTRandom( long s )
  {
    super( s );
  }
}
