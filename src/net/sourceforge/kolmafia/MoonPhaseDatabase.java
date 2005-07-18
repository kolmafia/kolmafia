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

import java.util.Date;

/**
 * A special class used to determine the current moon phase.
 * Theoretically, the calculations are simple enough to be
 * an internal class elsewhere, but because it makes everything
 * cleaner to do things this way, so it goes.
 */

public class MoonPhaseDatabase implements KoLConstants
{
	protected static int PHASE_STEP = -1;
	protected static int RONALD_PHASE = -1;
	protected static int GRIMACE_PHASE = -1;

	private static final String [] STAT_EFFECT =
	{
		"Moxie day today and yesterday.", "3 days until Mysticism.", "2 days until Mysticism.", "Mysticism tomorrow (not today).",
		"Mysticism day today (not tomorrow).", "3 days until Muscle.", "2 days until Muscle.", "Muscle tomorrow (not today).",
		"Muscle day today and tomorrow.", "Muscle day today and yesterday.", "2 days until Mysticism.", "Mysticism tomorrow (not today).",
		"Mysticism day today (not tomorrow).", "2 days until Moxie.", "Moxie tomorrow (not today).", "Moxie day today and tomorrow."
	};

	public static final void setMoonPhases( int ronald, int grimace )
	{
		RONALD_PHASE = ronald;
		GRIMACE_PHASE = grimace;
		PHASE_STEP = RONALD_PHASE + ((GRIMACE_PHASE >= 4) ? 8 : 0);
	}

	/**
	 * Method to return which phase of the moon is currently
	 * appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return	The current phase of Ronald
	 */

	public static final String getRonaldMoonPhase()
	{	return getPhaseName( RONALD_PHASE );
	}

	/**
	 * Method to return which phase of the moon is currently
	 * appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return	The current phase of Ronald
	 */

	public static final String getGrimaceMoonPhase()
	{	return getPhaseName( GRIMACE_PHASE );
	}

	public static final String getPhaseName( int phase )
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
			default:  return "unknown";
		}
	}

	/**
	 * Returns the moon effect applicable today, or the amount
	 * of time until the next moon effect becomes applicable
	 * if today is not a moon effect day.
	 */

	public static final String getMoonEffect()
	{	return getMoonEffect( PHASE_STEP );
	}

	/**
	 * Returns the moon effect applicable at the given phase
	 * step, or the amount of time until the next moon effect,
	 * given the phase value.
	 */

	public static final String getMoonEffect( int phaseStep )
	{	return phaseStep == -1 || phaseStep >= STAT_EFFECT.length ? "Could not determine moon phase." : STAT_EFFECT[ phaseStep ];
	}


	/**
	 * Returns whether or not the grue will fight during the
	 * current moon phase.
	 */

	public static final boolean getGrueEffect()
	{	return getGrueEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns whether or not the grue will fight during the
	 * given moon phases.
	 */

	public static final boolean getGrueEffect( int ronaldPhase, int grimacePhase )
	{	return getMoonlight( ronaldPhase, grimacePhase ) < 5;
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of Blood of the Wereseal for today.
	 */

	public static final int getBloodEffect()
	{	return getBloodEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of Blood of the Wereseal for the given moon phase.
	 */

	public static final int getBloodEffect( int ronaldPhase, int grimacePhase )
	{	return (getMoonlight( ronaldPhase, grimacePhase ) - 4) * 25;
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of the Talisman of Baio for today.
	 */

	public static final int getBaioEffect()
	{	return getBaioEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of the Talisman of Baio for the given moon phases.
	 */

	public static final int getBaioEffect( int ronaldPhase, int grimacePhase )
	{	return getMoonlight( ronaldPhase, grimacePhase ) * 10;
	}

	/**
	 * Utility method which determines the moonlight available,
	 * given the moon phases as stated.
	 */

	private static final int getMoonlight( int ronaldPhase, int grimacePhase )
	{
		int ronaldLight = ronaldPhase > 4 ? 8 - ronaldPhase : ronaldPhase;
		int grimaceLight = grimacePhase > 4 ? 8 - grimacePhase : grimacePhase;

		return ronaldLight + grimaceLight;
	}
}
