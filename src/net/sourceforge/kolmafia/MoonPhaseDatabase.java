/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * A special class used to determine the current moon phase.
 * Theoretically, the calculations are simple enough to be
 * an internal class elsewhere, but because it makes everything
 * cleaner to do things this way, so it goes.
 */

public class MoonPhaseDatabase
{
	private static int PHASE_STEP;

	static
	{
		// In order to ensure reliability, the value of the
		// date is fixed, rather than calculated.  This ms
		// value represents February 5, 2005 at 11:30pm on
		// the Eastern United States, which is when rollover
		// generally occurs.

		long newMoonDate = 1107664200000L;
		long dayLength = 24 * 60 * 60 * 1000L;

		long timeDifference = System.currentTimeMillis() - newMoonDate;
		PHASE_STEP = (int) Math.floor( (double)timeDifference / (double)dayLength );
		PHASE_STEP = (PHASE_STEP + 16) % 16;
	}

	private static final String [] STAT_EFFECT =
	{
		"Moxie day today.", "3 days until Mysticism.", "2 days until Mysticism.",
		"Mysticism tomorrow.", "Mysticism day today.", "3 days until Muscle.",
		"2 days until Muscle.", "Muscle tomorrow.", "Muscle day today.", "Muscle day today.",
		"2 days until Mysticism.", "Mysticism tomorrow.", "Mysticism day today.",
		"2 days until Moxie.", "Moxie tomorrow.", "Moxie day today."
	};

	/**
	 * Method to return which phase of the moon is currently
	 * appearing over the Kingdom of Loathing, as a string.
	 *
	 * @param	The current phases of Ronald and Grimace
	 */

	public static final String getMoonPhase()
	{
		int ronaldPhase = PHASE_STEP % 8;
		int grimacePhase = ((int)Math.floor( PHASE_STEP / 2 )) % 8;

		return "Ronald: " + getPhaseName( ronaldPhase ) + ", Grimace: " + getPhaseName( grimacePhase );
	}

	private static final String getPhaseName( int phase )
	{
		switch ( phase )
		{
			case 0:  return "new moon";
			case 1:  return "waxing crescent";
			case 2:  return "first quarter";
			case 3:  return "waxing gibbous";
			case 4:  return "full moon";
			case 5:  return "waning gibbous";
			case 6:  return "third quarter";
			case 7:  return "waning crescent";
			default:  return null;
		}
	}

	/**
	 * Returns the moon effect applicable today, or the amount
	 * of time until the next moon effect becomes applicable
	 * if today is not a moon effect day.
	 *
	 * @return	The time until the next moon
	 */

	public static final String getMoonEffect()
	{	return STAT_EFFECT[ PHASE_STEP ];
	}

	/**
	 * Creates a frame which displays detailed statistics about
	 * the current moon phase.  This currently is made private
	 * until the required functionality is added.
	 */

	public static final void displayMoonPhase()
	{
		JFrame display = new JFrame( getMoonPhase() );
		display.getContentPane().setLayout( new CardLayout( 10, 10 ) );
		display.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		display.pack();  display.setVisible( true );
	}
}