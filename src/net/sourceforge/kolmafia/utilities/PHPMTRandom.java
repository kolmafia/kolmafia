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
      state = new ArrayList<Long>();
    }

    initialize( seed );
    reload();
  }

  public PHPMTRandom( long s )
  {
    super( s );
  }
}
