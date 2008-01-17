/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
import net.sourceforge.kolmafia.StaticEntity;

public class HolidayDatabase
	extends StaticEntity
{
	private static long NEWYEAR = 0;
	private static long BOUNDARY = 0;
	private static long COLLISION = 0;

	static
	{
		try
		{
			// Change it so that it doesn't recognize daylight savings in order
			// to ensure different localizations work.

			Calendar myCalendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT-5" ) );

			myCalendar.set( 2005, 8, 17, 0, 0, 0 );
			HolidayDatabase.NEWYEAR = myCalendar.getTimeInMillis();

			myCalendar.set( 2005, 9, 27, 0, 0, 0 );
			HolidayDatabase.BOUNDARY = myCalendar.getTimeInMillis();

			myCalendar.set( 2006, 5, 3, 0, 0, 0 );
			HolidayDatabase.COLLISION = myCalendar.getTimeInMillis();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static int RONALD_PHASE = -1;
	private static int GRIMACE_PHASE = -1;
	private static int HAMBURGLAR_POSITION = -1;

	static
	{
		try
		{
			int calendarDay = HolidayDatabase.getCalendarDay( new Date() );
			int phaseStep = ( calendarDay % 16 + 16 ) % 16;

			HolidayDatabase.RONALD_PHASE = phaseStep % 8;
			HolidayDatabase.GRIMACE_PHASE = phaseStep / 2;
			HolidayDatabase.HAMBURGLAR_POSITION = HolidayDatabase.getHamburglarPosition( new Date() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
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

	public static final void setMoonPhases( final int ronaldPhase, final int grimacePhase )
	{
		// Reset the new year based on the internal
		// phase error.

		int phaseError = HolidayDatabase.getPhaseStep();

		HolidayDatabase.RONALD_PHASE = ronaldPhase;
		HolidayDatabase.GRIMACE_PHASE = grimacePhase;

		phaseError -= HolidayDatabase.getPhaseStep();

		// Adjust the new year by the appropriate
		// number of days.

		HolidayDatabase.NEWYEAR += phaseError * 86400000L;
		HolidayDatabase.BOUNDARY += phaseError * 86400000L;
		HolidayDatabase.COLLISION += phaseError * 86400000L;
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
		long timeDifference = time.getTime();
		if ( timeDifference < HolidayDatabase.COLLISION )
		{
			return -1;
		}

		timeDifference -= HolidayDatabase.COLLISION;
		int dayDifference = (int) Math.floor( timeDifference / 86400000L );
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
		return 50 + ( HolidayDatabase.getMoonlight( ronaldPhase, grimacePhase, hamburglarPosition ) - 4 ) * 5;
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
		int grimaceEffect = 4 - HolidayDatabase.getGrimaceMoonlight( grimacePhase );
		if ( HolidayDatabase.getHamburglarLight( ronaldPhase, grimacePhase, hamburglarPosition ) != 1 )
		{
			++grimaceEffect;
		}

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

	/**
	 * Computes the difference in days based on the given millisecond counts since January 1, 1970.
	 */

	public static final int getCalendarDay( final Date time )
	{
		try
		{
			long timeDifference =
				KoLConstants.DAILY_FORMAT.parse( KoLConstants.DAILY_FORMAT.format( time ) ).getTime() - HolidayDatabase.NEWYEAR;

			if ( timeDifference > HolidayDatabase.BOUNDARY )
			{
				timeDifference -= 86400000L;
			}

			int dayDifference = (int) Math.floor( timeDifference / 86400000L );
			return ( dayDifference % 96 + 96 ) % 96;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return 0;
		}
	}

	/**
	 * Utility method which calculates which day of the KoL calendar you're currently on, based on the number of
	 * milliseconds since January 1, 1970.
	 */

	public static final String getCalendarDayAsString( final Date time )
	{
		int[] calendarDayAsArray =
			HolidayDatabase.convertCalendarDayToArray( HolidayDatabase.getCalendarDay( time ) );
		return HolidayDatabase.MONTH_NAMES[ calendarDayAsArray[ 0 ] ] + " " + calendarDayAsArray[ 1 ];
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
				gameHoliday = gameHoliday + " today";
			}

			for ( int i = 1; gameHoliday == null; ++i )
			{
				calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray( calendarDay + i % 96 );
				gameHoliday = HolidayDatabase.HOLIDAYS[ calendarDayAsArray[ 0 ] ][ calendarDayAsArray[ 1 ] ];

				if ( gameHoliday != null )
				{
					if ( i == 1 )
					{
						gameHoliday = gameHoliday + " tomorrow";
					}
					else
					{
						gameHoliday = HolidayDatabase.getDayCountAsString( i ) + " until " + gameHoliday;
					}
				}
			}
		}

		return gameHoliday == null && realHoliday == null ? "No known holiday today." : gameHoliday == null ? realHoliday : realHoliday == null ? gameHoliday : realHoliday + " / " + gameHoliday;
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
			Calendar holidayFinder = Calendar.getInstance( TimeZone.getTimeZone( "GMT-5" ) );

			// Apparently, Easter isn't the second Sunday in April;
			// it actually depends on the occurrence of the first
			// ecclesiastical full moon after the Spring Equinox
			// (http://aa.usno.navy.mil/faq/docs/easter.html)

			int y = StaticEntity.parseInt( currentYear );
			int c = y / 100;
			int n = y - 19 * y / 19;
			int k = ( c - 17 ) / 25;
			int i = c - c / 4 - ( c - k ) / 3 + 19 * n + 15;
			i = i - 30 * i / 30;
			i = i - i / 28 * ( 1 - i / 28 * 29 / ( i + 1 ) * ( 21 - n ) / 11 );
			int j = y + y / 4 + i + 2 - c + c / 4;
			j = j - 7 * j / 7;
			int l = i - j;
			int m = 3 + ( l + 40 ) / 44;
			int d = l + 28 - 31 * m / 4;

			holidayFinder.set( Calendar.YEAR, y );
			holidayFinder.set( Calendar.MONTH, m - 1 );
			holidayFinder.set( Calendar.DAY_OF_MONTH, d );

			HolidayDatabase.easter = KoLConstants.DAILY_FORMAT.format( holidayFinder.getTime() );

			holidayFinder.set( Calendar.MONTH, Calendar.NOVEMBER );
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
