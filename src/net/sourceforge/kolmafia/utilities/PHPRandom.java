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
 * PHP < 7.1.0 uses glibc's rand function which is explained very clearly
 * at https://www.mscs.dal.ca/~selinger/random/. This is mostly derived
 * directly from PHP's source code by Gausie.
 */

public class PHPRandom extends Random {
  public static final long serialVersionUID = 0l;
  public ArrayList<Integer> state;

  public int next( int bits )
  {
    int i = state.size();
    int value = state.get( i - 31 ) + state.get( i - 3 );
    state.add( value );
    return value >>> 1;
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

  synchronized public void setSeed( long seed )
  {    
    if ( state == null )
    {
      state = new ArrayList<Integer>();
    }

    state.clear();
    state.add( (int) seed );

    for(int i = 1; i < 31; i++) {
      int value = (int) ( ( 16_807l * state.get( i - 1 ) ) % Integer.MAX_VALUE );
      if( value < 0 ) {
        value += Integer.MAX_VALUE;
      }
      state.add(value);
    }

    for(int i = 31; i < 34; i++) {
      state.add( state.get( i - 31 ) );
    }

    for(int i = 34; i < 344; i++) {
      next( 32 );
    }
  }

  public int[] array( int count, int required )
  {
    required = Math.min( required, count );

    int[] result = new int[required];
    int j = 0;

    for ( int i = 0; i < count; i++ )
    {
      double chance = ( ( required - j ) / (double) ( count - i ) );
      if ( nextDouble() < chance ) {
        result[j++] = i;
      }
    }

    return result;
  }

  public PHPRandom( int s )
  {
    super( s );
  }
}
