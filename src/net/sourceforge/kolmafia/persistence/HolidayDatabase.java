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

package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HolidayDatabase
{
	private static long NEWYEAR = 0;
	private static long BOUNDARY = 0;
	private static long COLLISION = 0;

	private static long MS_PER_DAY = 86400000L;

	private static int RONALD_PHASE = -1;
	private static int GRIMACE_PHASE = -1;
	private static int HAMBURGLAR_POSITION = -1;

	static
	{
		HolidayDatabase.guessPhaseStep();
	}

	// static final array of status effect day predictions
	// within the KoL lunar calendar.

	private static final String[] STAT_EFFECT =
	{
		"Moxie bonus today and yesterday.",
		"3 days until Mysticism.",
		"2 days until Mysticism.",
		"Mysticism bonus tomorrow (not today).",
		"Mysticism bonus today (not tomorrow).",
		"3 days until Muscle.",
		"2 days until Muscle.",
		"Muscle bonus tomorrow (not today).",
		"Muscle bonus today and tomorrow.",
		"Muscle bonus today and yesterday.",
		"2 days until Mysticism.",
		"Mysticism bonus tomorrow (not today).",
		"Mysticism bonus today (not tomorrow).",
		"2 days until Moxie.",
		"Moxie bonus tomorrow (not today).",
		"Moxie bonus today and tomorrow."
	};

	// static final array of month names, as they exist within
	// the KoL calendar.

	private static final String[] MONTH_NAMES =
	{
		"",
		"Jarlsuary",
		"Frankruary",
		"Starch",
		"April",
		"Martinus",
		"Bill",
		"Bor",
		"Petember",
		"Carlvember",
		"Porktober",
		"Boozember",
		"Dougtember"
	};

	// static final array of HOLIDAYS.  This holiday is filled with the
	// name of the holiday which occurs on the given KoL month and
	// given KoL day.

	private static final String[][] HOLIDAYS = new String[ 13 ][ 9 ];

	static
	{
		for ( int i = 0; i < 13; ++i )
		{
			for ( int j = 0; j < 9; ++j )
			{
				HolidayDatabase.HOLIDAYS[ i ][ j ] = null;
			}
		}

		// Initialize all the known HOLIDAYS here so that
		// they can be used in later initializers.

		HolidayDatabase.HOLIDAYS[ 1 ][ 1 ] = "Festival of Jarlsberg"; // Jarlsuary 1
		HolidayDatabase.HOLIDAYS[ 2 ][ 4 ] = "Valentine's Day"; // Frankuary 4
		HolidayDatabase.HOLIDAYS[ 3 ][ 3 ] = "St. Sneaky Pete's Day"; // Starch 3
		HolidayDatabase.HOLIDAYS[ 4 ][ 2 ] = "Oyster Egg Day"; // April 2
		HolidayDatabase.HOLIDAYS[ 5 ][ 2 ] = "El Dia De Los Muertos Borrachos"; // Martinus 2
		HolidayDatabase.HOLIDAYS[ 6 ][ 3 ] = "Generic Summer Holiday"; // Bill 3
		HolidayDatabase.HOLIDAYS[ 7 ][ 4 ] = "Dependence Day"; // Bor 4
		HolidayDatabase.HOLIDAYS[ 8 ][ 4 ] = "Arrrbor Day"; // Petember 4
		HolidayDatabase.HOLIDAYS[ 9 ][ 6 ] = "Lab&oacute;r Day"; // Carlvember 6
		HolidayDatabase.HOLIDAYS[ 10 ][ 8 ] = "Halloween"; // Porktober 8
		HolidayDatabase.HOLIDAYS[ 11 ][ 7 ] = "Feast of Boris"; // Boozember 7
		HolidayDatabase.HOLIDAYS[ 12 ][ 4 ] = "Yuletide"; // Dougtember
	}

	// static final array of when the special events in KoL occur, including
	// stat days, HOLIDAYS and all that jazz.  Values are false where
	// there is no special occasion, and true where there is.

	private static final int[] SPECIAL = new int[ 96 ];

	public static final int SP_NOTHING = 0;
	public static final int SP_HOLIDAY = 1;

	public static final int SP_MUSDAY = 2;
	public static final int SP_MYSDAY = 3;
	public static final int SP_MOXDAY = 4;

	static
	{
		// Assume there are no special days at all, and then
		// fill them in once they're encountered.

		for ( int i = 0; i < 96; ++i )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_NOTHING;
		}

		// Muscle days occur every phase 8 and phase 9 on the
		// KoL calendar.

		for ( int i = 8; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MUSDAY;
		}
		for ( int i = 9; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MUSDAY;
		}

		// Mysticism days occur every phase 4 and phase 12 on the
		// KoL calendar.

		for ( int i = 4; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MYSDAY;
		}
		for ( int i = 12; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MYSDAY;
		}

		// Moxie days occur every phase 0 and phase 15 on the
		// KoL calendar.

		for ( int i = 0; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MOXDAY;
		}
		for ( int i = 15; i < 96; i += 16 )
		{
			HolidayDatabase.SPECIAL[ i ] = HolidayDatabase.SP_MOXDAY;
		}

		// Next, fill in the HOLIDAYS.  These are manually
		// computed based on the recurring day in the year
		// at which these occur.

		for ( int i = 0; i < 13; ++i )
		{
			for ( int j = 0; j < 9; ++j )
			{
				if ( HolidayDatabase.HOLIDAYS[ i ][ j ] != null )
				{
					HolidayDatabase.SPECIAL[ 8 * i + j - 9 ] = HolidayDatabase.SP_HOLIDAY;
				}
			}
		}
	}

	public static final void guessPhaseStep()
	{
		try
		{
			// Use a timezone such that the "day" begins at rollover.

			Calendar myCalendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0330" ) );

			myCalendar.set( 2005, 8, 17, 0, 0, 0 );
			HolidayDatabase.NEWYEAR = myCalendar.getTimeInMillis();

			// the date of White Wednesday, which throws everything off by a day
			myCalendar.set( 2005, 9, 27, 0, 0, 0 );
			HolidayDatabase.BOUNDARY = myCalendar.getTimeInMillis();

			// the day the thing crashed into Grimace
			myCalendar.set( 2006, 5, 3, 0, 0, 0 );
			HolidayDatabase.COLLISION = myCalendar.getTimeInMillis();

			Date now = new Date();
			int calendarDay = HolidayDatabase.getCalendarDay( now );
			int phaseStep = ( calendarDay + 16 ) % 16;

			HolidayDatabase.RONALD_PHASE = phaseStep % 8;
			HolidayDatabase.GRIMACE_PHASE = phaseStep / 2;
			HolidayDatabase.HAMBURGLAR_POSITION = HolidayDatabase.getHamburglarPosition( now );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void logMoonStatus( final String label )
	{
		Date now = new Date();

		int calendarDay = HolidayDatabase.getCalendarDay( now );
		int phaseStep = ( calendarDay + 16 ) % 16;
		String kolString = HolidayDatabase.getCalendarDayAsString( calendarDay );

		int ronald = HolidayDatabase.RONALD_PHASE;
		String ronaldString = HolidayDatabase.getPhaseName( ronald );
		int grimace = HolidayDatabase.GRIMACE_PHASE;
		String grimaceString = HolidayDatabase.getPhaseName( grimace );

		String message1 = label + ": " +
			now + " = " +
			kolString + " (" + calendarDay + ":" + phaseStep + ").";
		String message2 = "Ronald: " + ronaldString + " (" + ronald + ")" +
			", Grimace: " + grimaceString + " (" + grimace + ")";

		RequestLogger.printLine( message1 );
		RequestLogger.printLine( message2 );
	}

	public static final void setMoonPhases( final int ronaldPhase, final int grimacePhase )
	{
		HolidayDatabase.guessPhaseStep();
		int oldStep = HolidayDatabase.getPhaseStep();

		HolidayDatabase.RONALD_PHASE = ronaldPhase;
		HolidayDatabase.GRIMACE_PHASE = grimacePhase;

		int newStep = HolidayDatabase.getPhaseStep();

		if ( oldStep != newStep )
		{
			// There are several reasons why our calculation of the KoL
			// moon phases might disagree with KoL's

			// - We assume KoL dates change at midnight of a calendar
			//   day. They do not. They change at KoL Rollover, which is
			//   before midnight in Arizona.
			//
			// - If you are in a different timezone than the KoL servers,
			//   Rollover happens at different times. Even if you are in
			//   the same timezone as Arizona, you may observe DST, and KoL
			//   does not.
			//
			// - The local computer clock may be off

			// If we ignore, for the moment, the last case, KoLmafia's idea
			// of the KoL date could be one off from KoL's idea:
			//
			// - If Rollover is finished but it is not yet midnight in this
			//   timezone, KoL will be one day ahead
			//
			// - If it is after midnight in this timezone, but Rollover has
			//   not occurred for KoL, KoL will be one day behind.

			// Therefore, assuming the local computer clock is correct, We
			// could be one day out of phase in either direction.

			// Unfortunately, a computer's clock can be arbitrarily
			// incorrect. We can accomodate that if we can, somehow, synch
			// up with the date used by KoL itself. We have two options:

			// We can easily see KoL's moon phases on the top menu. These
			// go through a 16-day cycle. If the local clock is 7 or fewer
			// days off, we can assume that our relative error to KoL is
			// from -8 to +7.

			// We can see KoL's actual month and day from the chatlaunch
			// frame. That goes through a 96-day cycle, which would allow
			// us to correct a phase error from -48 to +47.

			// For now, since we already read the moon phases, we'll do the
			// former, and assume that the phase error is between -8 days
			// and +7 days.

			int phaseError = oldStep - newStep;

			if ( phaseError > 7 )
			{
				phaseError -= 16;
			}

			String message = "Old phase = " + oldStep + " new phase = " + newStep + " phase error = " + phaseError;
			RequestLogger.printLine( message );

			// Adjust the new year by the appropriate number of days.

			HolidayDatabase.NEWYEAR += phaseError * MS_PER_DAY;
			HolidayDatabase.BOUNDARY += phaseError * MS_PER_DAY;
			HolidayDatabase.COLLISION += phaseError * MS_PER_DAY;
		}
		HolidayDatabase.HAMBURGLAR_POSITION = HolidayDatabase.getHamburglarPosition( new Date() );
	}

	public static final int getRonaldPhase()
	{
		return HolidayDatabase.RONALD_PHASE + 1;
	}

	public static final int getGrimacePhase()
	{
		return HolidayDatabase.GRIMACE_PHASE + 1;
	}

	public static final int getHamburglarPosition( final Date time )
	{
		long currentTime = time.getTime();
		long timeDifference = currentTime - HolidayDatabase.COLLISION;

		if ( timeDifference < 0 )
		{
			return -1;
		}

		int dayDifference = (int)(timeDifference / MS_PER_DAY);
		return ( dayDifference * 2 % 11 + 11 ) % 11;
	}

	/**
	 * Method to return which phase of the moon is currently appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return The current phase of Ronald
	 */

	public static final String getRonaldPhaseAsString()
	{
		return HolidayDatabase.getPhaseName( HolidayDatabase.RONALD_PHASE );
	}

	/**
	 * Method to return which phase of the moon is currently appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return The current phase of Ronald
	 */

	public static final String getGrimacePhaseAsString()
	{
		return HolidayDatabase.getPhaseName( HolidayDatabase.GRIMACE_PHASE );
	}

	public static final String getPhaseName( final int phase )
	{
		switch ( phase )
		{
		case 0:
			return "new moon";
		case 1:
			return "waxing crescent";
		case 2:
			return "first quarter";
		case 3:
			return "waxing gibbous";
		case 4:
			return "full moon";
		case 5:
			return "waning gibbous";
		case 6:
			return "third quarter";
		case 7:
			return "waning crescent";
		default:
			return "unknown";
		}
	}

	public static final String getHamburglarPositionAsString()
	{
		return HolidayDatabase.getHamburglarPositionName( HolidayDatabase.HAMBURGLAR_POSITION );
	}

	public static final String getHamburglarPositionName( final int phase )
	{
		switch ( phase )
		{
		case 0:
			return "in front of Grimace, L side";
		case 1:
			return "in front of Grimace, R side";
		case 2:
			return "far right";
		case 3:
			return "behind Grimace";
		case 4:
			return "in back, near Grimace";
		case 5:
			return "in back, near Ronald";
		case 6:
			return "behind Ronald";
		case 7:
			return "far left";
		case 8:
			return "in front of Ronald, L side";
		case 9:
			return "in front of Ronald, R side";
		case 10:
			return "front center";
		default:
			return "unknown";
		}
	}

	/**
	 * Returns the moon effect applicable today, or the amount of time until the next moon effect becomes applicable if
	 * today is not a moon effect day.
	 */

	public static final String getMoonEffect()
	{
		return HolidayDatabase.getMoonEffect( HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE );
	}

	/**
	 * Returns the moon effect applicable at the given phase step, or the amount of time until the next moon effect,
	 * given the phase value.
	 */

	public static final String getMoonEffect( final int ronaldPhase, final int grimacePhase )
	{
		int phaseStep = HolidayDatabase.getPhaseStep( ronaldPhase, grimacePhase );
		return phaseStep == -1 ? "Could not determine moon phase." : HolidayDatabase.STAT_EFFECT[ phaseStep ];
	}

	public static final int getRonaldMoonlight( final int ronaldPhase )
	{
		return ronaldPhase > 4 ? 8 - ronaldPhase : ronaldPhase;
	}

	public static final int getGrimaceMoonlight( final int grimacePhase )
	{
		return grimacePhase > 4 ? 8 - grimacePhase : grimacePhase;
	}

	/**
	 * Returns the "phase step" currently recognized by the KoL calendar. This corresponds to the day within the KoL
	 * lunar calendar, which has a cycle of 16 days.
	 */

	public static final int getPhaseStep()
	{
		return HolidayDatabase.getPhaseStep( HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE );
	}

	/**
	 * Returns the "phase step" currently recognized by the KoL calendar, corresponding to the given phases. This
	 * corresponds to the day within the KoL lunar calendar, which has a cycle of 16 days.
	 */

	public static final int getPhaseStep( final int ronaldPhase, final int grimacePhase )
	{
		return grimacePhase >= 4 ? 8 + ronaldPhase : ronaldPhase;
	}

	/**
	 * Returns whether or not the grue will fight during the current moon phase.
	 */

	public static final boolean getGrueEffect()
	{
		return HolidayDatabase.getGrueEffect(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	/**
	 * Returns whether or not the grue will fight during the given moon phases.
	 */

	public static final boolean getGrueEffect( final int ronaldPhase, final int grimacePhase,
		final int hamburglarPosition )
	{
		return HolidayDatabase.getMoonlight( ronaldPhase, grimacePhase, hamburglarPosition ) < 5;
	}

	/**
	 * Returns the effect percentage (as a whole number integer) of Blood of the Wereseal for today.
	 */

	public static final int getBloodEffect()
	{
		return HolidayDatabase.getBloodEffect(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	/**
	 * Returns the effect percentage (as a whole number integer) of Blood of the Wereseal for the given moon phase.
	 */

	public static final int getBloodEffect( final int ronaldPhase, final int grimacePhase, final int hamburglarPosition )
	{
		return (int) Math.round( 10.0f + 20.0f *
			Math.sqrt( (float) HolidayDatabase.getMoonlight(
				ronaldPhase, grimacePhase, hamburglarPosition ) ) );
	}

	/**
	 * Returns the effect percentage (as a whole number integer) of the Talisman of Baio for today.
	 */

	public static final int getBaioEffect()
	{
		return HolidayDatabase.getBaioEffect(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	/**
	 * Returns the effect percentage (as a whole number integer) of the Talisman of Baio for the given moon phases.
	 */

	public static final int getBaioEffect( final int ronaldPhase, final int grimacePhase, final int hamburglarPosition )
	{
		return HolidayDatabase.getMoonlight( ronaldPhase, grimacePhase, hamburglarPosition ) * 10;
	}

	public static final int getGrimaciteEffect()
	{
		return HolidayDatabase.getGrimaciteEffect(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	public static final int getGrimaciteEffect( final int ronaldPhase, final int grimacePhase,
		final int hamburglarPosition )
	{
		int grimaceEffect = 4 - HolidayDatabase.getGrimaceMoonlight( grimacePhase )
			+ HolidayDatabase.getHamburglarDarkness( ronaldPhase, grimacePhase,
				hamburglarPosition );
		return grimaceEffect * 10;
	}

	/**
	 * Returns the effect of the Jekyllin, based on the current moon phase information.
	 */

	public static final String getJekyllinEffect()
	{
		return HolidayDatabase.getJekyllinEffect(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	/**
	 * Returns the effect of the Jekyllin for the given moon phases
	 */

	public static final String getJekyllinEffect( final int ronaldPhase, final int grimacePhase,
		final int hamburglarPosition )
	{
		int moonlight = HolidayDatabase.getMoonlight( ronaldPhase, grimacePhase, hamburglarPosition );
		return "+" + ( 9 - moonlight ) + " stats, " + ( 15 + moonlight * 5 ) + "% items";
	}

	/**
	 * Utility method which determines the moonlight available, given the current moon phases.
	 */

	public static final int getMoonlight()
	{
		return HolidayDatabase.getMoonlight(
			HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE, HolidayDatabase.HAMBURGLAR_POSITION );
	}

	/**
	 * Utility method which determines the moonlight available, given the moon phases as stated.
	 */

	private static final int getMoonlight( final int ronaldPhase, final int grimacePhase, final int hamburglarPosition )
	{
		int ronaldLight = HolidayDatabase.getRonaldMoonlight( ronaldPhase );
		int grimaceLight = HolidayDatabase.getGrimaceMoonlight( grimacePhase );
		int hamburglarLight = HolidayDatabase.getHamburglarLight( ronaldPhase, grimacePhase, hamburglarPosition );
		return ronaldLight + grimaceLight + hamburglarLight;
	}

	public static final int getHamburglarLight( final int ronaldPhase, final int grimacePhase,
		final int hamburglarPosition )
	{
		//         6    5    4    3
		//
		//       /---\          /---\
		//   7   | R |          | G |   2
		//       \___/          \___/
		//
		//       8   9    10    0   1

		switch ( hamburglarPosition )
		{

		case 0:
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				return -1;
			}
			return 1;

		case 1:
			if ( grimacePhase < 4 )
			{
				return 1;
			}
			return -1;

		case 2:
			if ( grimacePhase > 3 )
			{
				return 1;
			}
			return 0;

		case 4:
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				return 1;
			}
			return 0;

		case 5:
			if ( ronaldPhase > 3 )
			{
				return 1;
			}
			return 0;

		case 7:
			if ( ronaldPhase > 0 && ronaldPhase < 5 )
			{
				return 1;
			}
			return 0;

		case 8:
			if ( ronaldPhase > 0 && ronaldPhase < 5 )
			{
				return -1;
			}
			return 1;

		case 9:
			if ( ronaldPhase < 4 )
			{
				return 1;
			}
			return -1;

		case 10:
			int totalEffect = 0;
			if ( ronaldPhase > 3 )
			{
				++totalEffect;
			}
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				++totalEffect;
			}
			return totalEffect;

		default:
			return 0;

		}
	}

	public static final int getHamburglarDarkness( final int ronaldPhase,
		final int grimacePhase, final int hamburglarPosition )
	{
		//         6    5    4    3
		//
		//       /---\          /---\
		//   7   | R |          | G |   2
		//       \___/          \___/
		//
		//       8   9    10    0   1

		switch ( hamburglarPosition )
		{

		case 0:
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				return 1;
			}
			return -1;

		case 1:
			if ( grimacePhase < 4 )
			{
				return -1;
			}
			return 1;

		case 2:
			if ( grimacePhase > 3 )
			{
				return 0;
			}
			return 1;

		case 4:
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				return 0;
			}
			return 1;

		case 5:
			if ( ronaldPhase > 3 )
			{
				return 0;
			}
			return 1;

		case 7:
			if ( ronaldPhase > 0 && ronaldPhase < 5 )
			{
				return 0;
			}
			return 1;

		case 8:
			if ( ronaldPhase > 0 && ronaldPhase < 5 )
			{
				return 1;
			}
			return 0;

		case 9:
			if ( ronaldPhase < 4 )
			{
				return 0;
			}
			return 1;

		case 10:
			if ( ronaldPhase > 3 )
			{
				return 0;
			}
			if ( grimacePhase > 0 && grimacePhase < 5 )
			{
				return 0;
			}
			return 1;

		default:
			return 0;

		}
	}

	/**
	 * Computes the difference in days based on the given millisecond counts since January 1, 1970.
	 */

	public static final long getDayDifference( final Date time )
	{
		long currentTime = time.getTime();
		long timeDifference = currentTime - HolidayDatabase.NEWYEAR;

		if ( currentTime > HolidayDatabase.BOUNDARY )
		{
			timeDifference -= MS_PER_DAY;
		}

		return timeDifference;
	}

	public static final int getCalendarDay( final Date time )
	{
		int dayDifference = (int) Math.floor( getDayDifference( time ) / MS_PER_DAY );
		return ( dayDifference + 96 ) % 96;
	}

	public static final int getTimeDifference( final Date time )
	{
		return (int) Math.floor( getDayDifference( time ) % MS_PER_DAY );
	}

	/**
	 * Utility method which calculates which day of the KoL calendar you're currently on, based on the number of
	 * milliseconds since January 1, 1970.
	 */

	public static final String getCalendarDayAsString( final int day )
	{
		int[] calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( day );
		return HolidayDatabase.MONTH_NAMES[ calendarDayAsArray[ 0 ] ] + " " + calendarDayAsArray[ 1 ];
	}

	public static final String getCalendarDayAsString( final Date time )
	{
		return HolidayDatabase.getCalendarDayAsString( HolidayDatabase.getCalendarDay( time ) );
	}

	/**
	 * Utility method which decomposes a given calendar day into its actual calendar components.
	 */

	private static final int[] convertCalendarDayToArray( final int calendarDay )
	{
		return new int[] { calendarDay / 8 % 12 + 1, calendarDay % 8 + 1 };
	}

	/**
	 * Utility method which returns the given day count as an easily-understood string (today, tomorrow) instead of just
	 * "x days".
	 */

	public static final String getDayCountAsString( final int dayCount )
	{
		return dayCount == 0 ? "today" : dayCount == 1 ? "tomorrow" : dayCount + " days";
	}

	/**
	 * Returns the KoL calendar month associated with the given date in the real world.
	 */

	public static final int getCalendarMonth( final Date time )
	{
		return HolidayDatabase.convertCalendarDayToArray( HolidayDatabase.getCalendarDay( time ) )[ 0 ];
	}

	/**
	 * Returns whether or not the given day's most important attribute is being a holiday.
	 */

	public static final boolean isHoliday( final Date time )
	{
		return HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] == HolidayDatabase.SP_HOLIDAY;
	}

	public static final boolean isRealLifeHoliday( final Date time )
	{
		return HolidayDatabase.getRealLifeHoliday( KoLConstants.DAILY_FORMAT.format( time ) ) != null;
	}

	/**
	 * Returns whether or not the given day's most important attribute is being a muscle day. Note that this ranks
	 * behind being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster Egg Day, for example), will
	 * not be recognized as "stat days" in this method.
	 */

	public static final boolean isMuscleDay( final Date time )
	{
		return HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] == HolidayDatabase.SP_MUSDAY;
	}

	/**
	 * Returns whether or not the given day's most important attribute is being a mysticality day. Note that this ranks
	 * behind being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster Egg Day, for example), will
	 * not be recognized as "stat days" in this method.
	 */

	public static final boolean isMysticalityDay( final Date time )
	{
		return HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] == HolidayDatabase.SP_MYSDAY;
	}

	/**
	 * Returns whether or not the given day's most important attribute is being a moxie day. Note that this ranks behind
	 * being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster Egg Day, for example), will not be
	 * recognized as "stat days" in this method.
	 */

	public static final boolean isMoxieDay( final Date time )
	{
		return HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] == HolidayDatabase.SP_MOXDAY;
	}

	/**
	 * Returns whether or not the given day's most important attribute is being a moxie day. Note that this ranks behind
	 * being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster Egg Day, for example), will not be
	 * recognized as "stat days" in this method.
	 */

	public static final int statDay( final Date time )
	{
		switch ( HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] )
		{
		case HolidayDatabase.SP_MUSDAY:
			return KoLConstants.MUSCLE;
		case HolidayDatabase.SP_MYSDAY:
			return KoLConstants.MYSTICALITY;
		case HolidayDatabase.SP_MOXDAY:
			return KoLConstants.MOXIE;
		}
		return KoLConstants.NONE;
	}

	public static final String currentStatDay()
	{
		int stat = HolidayDatabase.statDay( new Date() );
		return	stat == KoLConstants.MUSCLE ? "Muscle Day" :
			stat == KoLConstants.MYSTICALITY ? "Mysticality Day" :
			stat == KoLConstants.MOXIE ? "Moxie Day" :
			"None";
	}

	/**
	 * Returns a complete list of all holiday predictions for the given day, as an array.
	 */

	public static final String[] getHolidayPredictions( final Date time )
	{
		List holidayList = new ArrayList();
		int currentCalendarDay = HolidayDatabase.getCalendarDay( time );

		int[] calendarDayAsArray;

		for ( int i = 0; i < 96; ++i )
		{
			if ( HolidayDatabase.SPECIAL[ i ] == HolidayDatabase.SP_HOLIDAY )
			{
				calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( i );
				int currentEstimate = ( i - currentCalendarDay + 96 ) % 96;

				String holiday = HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ];

				String testDate = null;
				String testResult = null;

				Calendar holidayTester = Calendar.getInstance();
				holidayTester.setTime( time );

				for ( int j = 0; j < currentEstimate; ++j )
				{
					testDate = KoLConstants.DAILY_FORMAT.format( holidayTester.getTime() );
					testResult = HolidayDatabase.getRealLifeHoliday( testDate );

					if ( holiday != null && testResult != null && testResult.equals( holiday ) )
					{
						currentEstimate = j;
					}

					holidayTester.add( Calendar.DATE, 1 );
				}

				holidayList.add( new HolidayEntry( currentEstimate, HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ] ) );
			}
		}

		// If today is a real life holiday that doesn't map to a KoL
		// holiday, list it here.

		if ( HolidayDatabase.SPECIAL[ HolidayDatabase.getCalendarDay( time ) ] != HolidayDatabase.SP_HOLIDAY )
		{
			String holiday = HolidayDatabase.getRealLifeOnlyHoliday( KoLConstants.DAILY_FORMAT.format( time ) );
			if ( holiday != null )
			{
				holidayList.add( new HolidayEntry( 0, holiday ) );
			}
		}

		Collections.sort( holidayList );
		HolidayEntry[] holidayArray = new HolidayEntry[ holidayList.size() ];
		holidayList.toArray( holidayArray );
		String[] predictionsArray = new String[ holidayList.size() ];
		for ( int i = 0; i < holidayArray.length; ++i )
		{
			predictionsArray[i] = holidayArray[i].toString();
		}

		return predictionsArray;
	}

	private static class HolidayEntry
		implements Comparable
	{
		private final int offset;
		private final String name;

		public HolidayEntry( final int offset, final String name )
		{
			this.offset = offset;
			this.name = name;
		}

		public int compareTo( final Object o )
		{
			if ( o == null || !( o instanceof HolidayEntry ) )
			{
				return -1;
			}

			HolidayEntry other = (HolidayEntry) o;

			if ( this.offset != other.offset )
			{
				return this.offset - other.offset;
			}

			return this.name.compareTo( other.name );
		}

		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof HolidayEntry ) )
			{
				return false;
			}

			HolidayEntry other = (HolidayEntry) o;

			return ( this.offset == other.offset ) && this.name.equals( other.name );
		}

		public String toString()
		{
			return this.name + ": " + getDayCountAsString( offset );
		}
	}

	public static final String getHoliday()
	{
		return HolidayDatabase.getHoliday( new Date(), false );
	}

	public static final String getHoliday( final boolean showPredictions)
	{
		return HolidayDatabase.getHoliday( new Date(), showPredictions );
	}

	public static final String getHoliday( final Date time )
	{
		return HolidayDatabase.getHoliday( time, false );
	}

	/**
	 * Returns the KoL holiday associated with the given date in the real world.
	 */

	public static final String getHoliday( final Date time, final boolean showPrediction )
	{
		int calendarDay = HolidayDatabase.getCalendarDay( time );
		int[] calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( calendarDay );

		String gameHoliday = HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ];
		String realHoliday = HolidayDatabase.getRealLifeHoliday( KoLConstants.DAILY_FORMAT.format( time ) );

		if ( showPrediction && realHoliday == null )
		{
			if ( gameHoliday != null )
			{
				return gameHoliday + " today";
			}

			calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( ( calendarDay + 1 ) % 96 );
			gameHoliday = HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ];

			if ( gameHoliday != null )
			{
				return gameHoliday + " tomorrow";
			}

			for ( int i = 2; i < 96 && gameHoliday == null; ++i )
			{
				calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( ( calendarDay + i ) % 96 );
				gameHoliday = HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ];

				if ( gameHoliday != null )
				{
					return HolidayDatabase.getDayCountAsString( i ) + " until " + gameHoliday;
				}
			}

			return "";
		}

		if ( gameHoliday == null && realHoliday == null )
		{
			return "";
		}

		if ( gameHoliday == null )
		{
			return realHoliday;
		}

		if ( realHoliday == null )
		{
			return gameHoliday;
		}

		String holiday = realHoliday + " / " + gameHoliday;
		if ( holiday.equals( "St. Sneaky Pete's Day / Feast of Boris" ) ||
		     holiday.equals( "Feast of Boris / St. Sneaky Pete's Day" ) )
		{
			holiday = "Drunksgiving";
		}

		return holiday;
	}

	private static String cachedYear = "";
	private static String easter = "";
	private static String thanksgiving = "";

	public static final String getRealLifeHoliday( final String stringDate )
	{
		String currentYear = stringDate.substring( 0, 4 );
		if ( !currentYear.equals( HolidayDatabase.cachedYear ) )
		{
			HolidayDatabase.cachedYear = currentYear;
			// Calculate holidays for the in-game timezone (days which start at rollover)
			Calendar holidayFinder = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0330" ) );

			// Apparently, Easter isn't the second Sunday in April;
			// it actually depends on the occurrence of the first
			// ecclesiastical full moon after the Spring Equinox
			// (http://aa.usno.navy.mil/faq/docs/easter.html)

			int y = StringUtilities.parseInt( currentYear );
			int c = y / 100;
			int n = y - 19 * ( y / 19 );
			int k = ( c - 17 ) / 25;
			int i = c - c / 4 - ( c - k ) / 3 + 19 * n + 15;
			i = i - 30 * ( i / 30 );
			i = i - ( i / 28 ) * ( 1 - ( i / 28 ) * ( 29 / ( i + 1 ) ) * ( ( 21 - n ) / 11 ) );
			int j = y + y / 4 + i + 2 - c + c / 4;
			j = j - 7 * ( j / 7 );
			int l = i - j;
			int m = 3 + ( l + 40 ) / 44;
			int d = l + 28 - 31 * ( m / 4 );

			holidayFinder.set( Calendar.YEAR, y );
			holidayFinder.set( Calendar.MONTH, m - 1 );
			holidayFinder.set( Calendar.DAY_OF_MONTH, d );

			HolidayDatabase.easter = KoLConstants.DAILY_FORMAT.format( holidayFinder.getTime() );

			holidayFinder.set( Calendar.MONTH, Calendar.NOVEMBER );
			holidayFinder.set( Calendar.DAY_OF_MONTH, 1 );
			switch ( holidayFinder.get( Calendar.DAY_OF_WEEK ) )
			{
			case Calendar.FRIDAY:
				HolidayDatabase.thanksgiving = "1128";
				break;
			case Calendar.SATURDAY:
				HolidayDatabase.thanksgiving = "1127";
				break;
			case Calendar.SUNDAY:
				HolidayDatabase.thanksgiving = "1126";
				break;
			case Calendar.MONDAY:
				HolidayDatabase.thanksgiving = "1125";
				break;
			case Calendar.TUESDAY:
				HolidayDatabase.thanksgiving = "1124";
				break;
			case Calendar.WEDNESDAY:
				HolidayDatabase.thanksgiving = "1123";
				break;
			case Calendar.THURSDAY:
				HolidayDatabase.thanksgiving = "1122";
				break;
			}
		}

		// Real-life holiday list borrowed from JRSiebz's
		// variables for HOLIDAYS on the KoL JS Almanac
		// (http://home.cinci.rr.com/jrsiebz/KoL/almanac.html)

		if ( stringDate.endsWith( "0102" ) )
		{
			return "Festival of Jarlsberg";
		}

		if ( stringDate.endsWith( "0214" ) )
		{
			return "Valentine's Day";
		}

		if ( stringDate.endsWith( "0317" ) )
		{
			return "St. Sneaky Pete's Day";
		}

		if ( stringDate.equals( HolidayDatabase.easter ) )
		{
			return "Oyster Egg Day";
		}

		if ( stringDate.endsWith( HolidayDatabase.thanksgiving ) )
		{
			return "Feast of Boris";
		}

		if ( stringDate.endsWith( "1031" ) )
		{
			return "Halloween";
		}

		return HolidayDatabase.getRealLifeOnlyHoliday( stringDate );
	}

	public static final String getRealLifeOnlyHoliday( final String stringDate )
	{
		if ( stringDate.endsWith( "0202" ) )
		{
			return "Groundhog Day";
		}

		if ( stringDate.endsWith( "0401" ) )
		{
			return "April Fool's Day";
		}

		if ( stringDate.endsWith( "0919" ) )
		{
			return "Talk Like a Pirate Day";
		}

		if ( stringDate.endsWith( "1225" ) )
		{
			return "Crimbo";
		}

		if ( stringDate.endsWith( "1022" ) )
		{
			return "Holatuwol's Birthday";
		}

		if ( stringDate.endsWith( "0923" ) )
		{
			return "Veracity's Birthday";
		}

		return null;
	}

	public static final void addPredictionHTML( final StringBuffer displayHTML, final Date today, final int phaseStep )
	{
		HolidayDatabase.addPredictionHTML( displayHTML, today, phaseStep, true );
	}

	public static final void addPredictionHTML( final StringBuffer displayHTML, final Date today, final int phaseStep,
		final boolean addStatDays )
	{
		// Next display the upcoming stat days.

		if ( addStatDays )
		{
			displayHTML.append( "<nobr><b>Muscle Day</b>:&nbsp;" );
			displayHTML.append( HolidayDatabase.getDayCountAsString( Math.min(
				( 24 - phaseStep ) % 16, ( 25 - phaseStep ) % 16 ) ) );
			displayHTML.append( "</nobr><br>" );

			displayHTML.append( "<nobr><b>Mysticality Day</b>:&nbsp;" );
			displayHTML.append( HolidayDatabase.getDayCountAsString( Math.min(
				( 20 - phaseStep ) % 16, ( 28 - phaseStep ) % 16 ) ) );
			displayHTML.append( "</nobr><br>" );

			displayHTML.append( "<nobr><b>Moxie Day</b>:&nbsp;" );
			displayHTML.append( HolidayDatabase.getDayCountAsString( Math.min(
				( 16 - phaseStep ) % 16, ( 31 - phaseStep ) % 16 ) ) );
			displayHTML.append( "</nobr><br>&nbsp;<br>" );
		}

		// Next display the upcoming holidays.  This is done
		// through loop calculations in order to minimize the
		// amount of code done to handle individual holidays.

		String[] holidayPredictions = HolidayDatabase.getHolidayPredictions( today );
		for ( int i = 0; i < holidayPredictions.length; ++i )
		{
			displayHTML.append( "<nobr><b>" );
			displayHTML.append( holidayPredictions[ i ].replaceAll( ":", ":</b>&nbsp;" ) );
			displayHTML.append( "</nobr><br>" );
		}
	}
}
