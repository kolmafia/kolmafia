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
import java.text.SimpleDateFormat;

import java.awt.Color;
import java.awt.CardLayout;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JEditorPane;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ca.bcit.geekkit.JCalendar;
import ca.bcit.geekkit.CalendarTableModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A class which displays the calendar image to be used for today's
 * moon phases and today's calendar.
 */

public class CalendarFrame extends KoLFrame implements ListSelectionListener
{
	// Special date marked as the new year.  This is
	// done as a string, since sdf.parse() throws an
	// exception, most of the time.

	private static final String NEWYEAR = "20050614";

	// Special date formatter which formats according to
	// the standard Western format of month, day, year.

	private static final SimpleDateFormat TODAY_FORMATTER = new SimpleDateFormat( "MMMM d, yyyy" );

	// Static array of month names, as they exist within
	// the KoL calendar.

	private static final String [] MONTH_NAMES =
	{
		"", "Jarlsuary", "Frankruary", "Starch", "April", "Martinus", "Bill",
		"Bor", "Petember", "Carlvember", "Porktober", "Boozember", "Dougtember"
	};

	// Static array of file names (not including .gif extension)
	// for the various months in the KoL calendar.

	private static final String [] CALENDARS =
	{	"", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
	};

	// Static array of holidays.  This holiday is filled with the
	// name of the holiday which occurs on the given KoL month and
	// given KoL day.

	private static String [][] HOLIDAYS = new String[13][9];

	static
	{
		for ( int i = 0; i < 13; ++i )
			for ( int j = 0; j < 9; ++j )
				HOLIDAYS[i][j] = "No holiday today.";

		HOLIDAYS[1][8] = "St. Sneaky Pete's Day";
		HOLIDAYS[10][8] = "Halloween";
		HOLIDAYS[11][7] = "Feast of Boris";
	}

	// Static array of when the special events in KoL occur, including
	// stat days, holidays and all that jazz.  Values are false where
	// there is no special occasion, and true where there is.

	private static int [] SPECIAL = new int[96];

	private static int SP_NOTHING = 0;
	private static int SP_HOLIDAY = 1;
	private static int SP_STATDAY = 2;

	static
	{
		// Assume there are no special days at all, and then
		// fill them in once they're encountered.

		for ( int i = 0; i < 96; ++i )
			SPECIAL[i] = SP_NOTHING;

		// Muscle days occur every phase 8 and phase 9 on the
		// KoL calendar.

		for ( int i = 8; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 9; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Mysticism days occur every phase 4 and phase 12 on the
		// KoL calendar.

		for ( int i = 4; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 12; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Moxie days occur every phase 0 and phase 15 on the
		// KoL calendar.

		for ( int i = 0; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 15; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Next, fill in the holidays.  These are manually
		// computed based on the recurring day in the year
		// at which these occur.

		SPECIAL[7] = SP_HOLIDAY;
		SPECIAL[79] = SP_HOLIDAY;
		SPECIAL[86] = SP_HOLIDAY;
	}

	// The following are static variables used to track the calendar.
	// They are made static as a design decision to allow the oracle
	// table nested inside of this class the access it needs to data.

	private static int phaseError = Integer.MAX_VALUE;

	private static int currentMonth = 0;
	private static int currentDay = 0;

	private static int ronaldPhase = -1;
	private static int grimacePhase = -1;
	private static int phaseStep = -1;

	private static JCalendar calendar;
	private static OracleTable oracleTable;
	private static LimitedSizeChatBuffer buffer;

	private static Date selectedDate;
	private static int selectedRow, selectedColumn;

	public CalendarFrame( KoLmafia client )
	{
		super( "KoLmafia: Farmer's Almanac", client );

		selectedRow = -1;
		selectedColumn = -1;
		selectedDate = new Date();

		ronaldPhase = MoonPhaseDatabase.RONALD_PHASE;
		grimacePhase = MoonPhaseDatabase.GRIMACE_PHASE;
		phaseStep = MoonPhaseDatabase.PHASE_STEP;

		buffer = new LimitedSizeChatBuffer( "KoLmafia: Calendar" );
		getContentPane().setLayout( new BorderLayout() );

		JEditorPane htmlDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( htmlDisplay, 400, 300 );
		htmlDisplay.setEditable( false );
		htmlDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		buffer.setChatDisplay( htmlDisplay );

		calculateCalendar( System.currentTimeMillis() );
		updateSummaryPage();

		JPanel htmlPanel = new JPanel();
		htmlPanel.setLayout( new CardLayout( 5, 5 ) );
		htmlPanel.add( htmlDisplay, "" );

		getContentPane().add( htmlPanel, BorderLayout.CENTER );

		calendar = new JCalendar( OracleTable.class );
		oracleTable = (OracleTable) calendar.getTable();
		oracleTable.getSelectionModel().addListSelectionListener( this );
		oracleTable.getColumnModel().getSelectionModel().addListSelectionListener( this );

		getContentPane().add( calendar, BorderLayout.EAST );
		setResizable( false );
	}

	/**
	 * Listener method which updates the main HTML panel with
	 * information, pending on the user's calendar day selection.
	 */

	public void valueChanged( ListSelectionEvent e )
	{
		// If the person has not yet released the
		// mouse, then do nothing.

		if ( e.getValueIsAdjusting() )
			return;

		// Compute which date is being selected
		// in the calendar table and update the
		// HTML on the center pane as appropriate

		if ( oracleTable.getSelectedRow() != selectedRow || oracleTable.getSelectedColumn() != selectedColumn )
		{
			try
			{
				selectedRow = oracleTable.getSelectedRow();
				selectedColumn = oracleTable.getSelectedColumn();
				selectedDate = sdf.parse( constructDateString( calendar.getModel(), selectedRow, selectedColumn ) );

				calculatePhases( selectedDate );
				calculateCalendar( selectedDate.getTime() );
				updateSummaryPage();
			}
			catch ( Exception e1 )
			{
				// If an exception happens somewhere in this
				// process, that means it didn't get to the
				// HTML updating stage.  In that case, you
				// have nothing to do.
			}
		}
	}

	/**
	 * Utility method which constructs a date string based on
	 * the given calendar model and the given selected row
	 * and column.  This date string conforms to the standard
	 * YYYYMMdd format.
	 */

	private static String constructDateString( CalendarTableModel model, int selectedRow, int selectedColumn )
	{
		int year = model.getCurrentYear();
		int month = model.getCurrentMonth() + 1;
		int day = Integer.parseInt( (String) model.getValueAt( selectedRow, selectedColumn ) );

		StringBuffer dateString = new StringBuffer();
		dateString.append( year );

		if ( month < 10 )
			dateString.append( '0' );
		dateString.append( month );

		if ( day < 10 )
			dateString.append( '0' );
		dateString.append( day );

		return dateString.toString();
	}

	/**
	 * Recalculates the moon phases given the time noted
	 * in the constructor.  This calculation assumes that
	 * the straightforward algorithm has no errors.
	 */

	private static final void calculatePhases( Date time )
	{
		try
		{
			int timeDifference = calculateDifferenceInDays( sdf.parse( NEWYEAR ).getTime(), time.getTime() );

			phaseStep = ((timeDifference % 16) + 16) % 16;
			ronaldPhase = phaseStep % 8;
			grimacePhase = ( phaseStep / 2 ) % 8;
		}
		catch ( Exception e )
		{
		}
	}

	/**
	 * Returns the calendar date for today.  This is calculated
	 * based on estimating today's server date using the current
	 * moon phase and relevant calendars, based on the local time
	 * on the machine.
	 */

	private static final String getCalendarDay()
	{	return MONTH_NAMES[ currentMonth ] + " " + currentDay;
	}

	/**
	 * Updates the HTML which displays the date and the information
	 * relating to the given date.  This should be called after all
	 * recalculation attempts.
	 */

	private static void updateSummaryPage()
	{
		StringBuffer displayHTML = new StringBuffer();

		// First display today's date along with the
		// appropriate calendar picture.  Include the
		// link shown in the clan calendar.

		displayHTML.append( "<center><table><tr><td valign=top>" );

		displayHTML.append( "<center><table border=1><tr><td align=center>drawn by <b><a href=\"http://elfwood.lysator.liu.se/loth/l/e/leigh/leigh.html\">SpaceMonkey</a></b></td></tr>" );
		displayHTML.append( "<tr><td><img src=\"http://images.kingdomofloathing.com/otherimages/bikini/" );
		displayHTML.append( CALENDARS[ currentMonth ] );
		displayHTML.append( ".gif\"></td></tr><tr><td align=center>" );
		displayHTML.append( TODAY_FORMATTER.format( selectedDate ) );
		displayHTML.append( "</td></tr><tr><td align=center><font size=+1><b>" );
		displayHTML.append( getCalendarDay() );
		displayHTML.append( "</b></font></td></tr></table></center>" );

		displayHTML.append( "</td><td valign=top>" );
		displayHTML.append( "<center><table>" );

		// Holidays should probably be in the first
		// row, just in case.

		displayHTML.append( "<tr><td colspan=2 align=center><b>" );
		displayHTML.append( HOLIDAYS[ currentMonth ][ currentDay ] );
		displayHTML.append( "</b></td></tr><tr><td colspan=2></td></tr>" );

		// Next display today's moon phases, including
		// the uber-spiffy name for each phase.  Just
		// like in the browser, Ronald then Grimace.

		displayHTML.append( "<tr><td align=right><b>Ronald</b>:&nbsp;</td><td><img src=\"http://images.kingdomofloathing.com/itemimages/smoon" );
		displayHTML.append( ronaldPhase + 1 );
		displayHTML.append( ".gif\">&nbsp; (" );
		displayHTML.append( MoonPhaseDatabase.getPhaseName( ronaldPhase ) );
		displayHTML.append( ")</td></tr>" );
		displayHTML.append( "<tr><td align=right><b>Grimace</b>:&nbsp;</td><td><img src=\"http://images.kingdomofloathing.com/itemimages/smoon" );
		displayHTML.append( grimacePhase + 1 );
		displayHTML.append( ".gif\">&nbsp; (" );
		displayHTML.append( MoonPhaseDatabase.getPhaseName( grimacePhase ) );
		displayHTML.append( ")</td></tr>" );
		displayHTML.append( "<tr><td align=right><b>Stats</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getMoonEffect( phaseStep ) );
		displayHTML.append( "</td></tr><td align=right><b>Grue</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getGrueEffect( ronaldPhase, grimacePhase ) ? "bloodlusty" : "pacifistic" );
		displayHTML.append( "</td></tr><td align=right><b>Blood</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBloodEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr><td align=right><b>Baio</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBaioEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr></table></center>" );

		// That completes the table display!  More data
		// relevant to the current date may follow.
		// A forecast section, maybe, too - but for now,
		// this simple data should be enough.

		displayHTML.append( "</td></tr></table></center>" );

		// Now that the HTML has been completely
		// constructed, clear the display buffer
		// and append the appropriate text.

		buffer.clearBuffer();
		buffer.append( displayHTML.toString() );
	}

	/**
	 * Utility method which appends the given percentage to
	 * the given string buffer, complete with + and % signs,
	 * wherever applicable.  Also appends "no effect" if the
	 * percentage is zero.
	 */

	private static void appendModifierPercentage( StringBuffer buffer, int percentage )
	{
		if ( percentage > 0 )
		{
			buffer.append( '+' );
			buffer.append( percentage );
			buffer.append( '%' );
		}
		else if ( percentage < 0 )
		{
			buffer.append( percentage );
			buffer.append( '%' );
		}
		else if ( percentage == 0 )
			buffer.append( "no effect" );
	}

	/**
	 * Utility method which calculates which day of the
	 * KoL calendar you're currently on, based on the number
	 * of milliseconds since January 1, 1970.
	 */

	private static void calculateCalendar( long timeCalculate )
	{
		try
		{
			// First, compute the time difference between
			// today and the start time for the year.
			// This difference should be computed in terms
			// of days for ease of later computations.

			int estimatedDifference = calculateDifferenceInDays( sdf.parse( NEWYEAR ).getTime(), timeCalculate );

			// Next, compare this value with the actual
			// computed phase step to see how far off
			// you are in the computation.

			int estimatedMoonPhase = ((estimatedDifference % 16) + 16) % 16;

			if ( phaseError == Integer.MAX_VALUE )
			{
				phaseError = (estimatedMoonPhase == 15 && phaseStep == 0) ? -1 :
					(estimatedMoonPhase == 0 && phaseStep == 15) ? 1 : phaseStep - estimatedMoonPhase;
			}

			int actualDifference = estimatedDifference + phaseError;

			// Now that you have the actual difference
			// in days, do the computation of the KoL
			// calendar date.

			int daysSinceLastNewYear = ((actualDifference % 96) + 96) % 96;

			currentMonth = (daysSinceLastNewYear / 8) + 1;
			currentDay = (daysSinceLastNewYear % 8) + 1;
		}
		catch ( Exception e )
		{
		}
	}

	/**
	 * Computes the difference in days based on the given
	 * millisecond counts since January 1, 1970.
	 */

	private static int calculateDifferenceInDays( long timeStart, long timeEnd )
	{
		long difference = timeEnd - timeStart;
		return (int) (difference / 86400000L);
	}

	/**
	 * Internal class which functions as a table for the
	 * JCalendar object.  Unlike the standard implementation
	 * used by JCalendar, this also highlights stat days and
	 * holidays on the KoL calendar.
	 */

	public static class OracleTable extends JTable
	{
		private CalendarTableModel model;
		private DefaultTableCellRenderer normalRenderer, todayRenderer, holidayRenderer, statdayRenderer;

		public OracleTable( CalendarTableModel model )
		{
			super( model );
			this.model = model;

			normalRenderer = new DefaultTableCellRenderer();

			todayRenderer = new DefaultTableCellRenderer();
			todayRenderer.setForeground( new Color( 255, 255, 255 ) );
			todayRenderer.setBackground( new Color( 0, 0, 128 ) );

			holidayRenderer = new DefaultTableCellRenderer();
			holidayRenderer.setForeground( new Color( 255, 255, 255 ) );
			holidayRenderer.setBackground( new Color( 192, 0, 0 ) );

			statdayRenderer = new DefaultTableCellRenderer();
			statdayRenderer.setForeground( new Color( 0, 0, 0 ) );
			statdayRenderer.setBackground( new Color( 192, 192, 0 ) );
		}

		public TableCellRenderer getCellRenderer( int row, int column )
		{
			try
			{
				// First, if the date today is equal to the
				// date selected, highlight it.

				String todayDate = sdf.format( new Date() );
				String cellDate = constructDateString( model, row, column );

				if ( todayDate.equals( cellDate ) )
					return todayRenderer;

				// Otherwise, if the date selected is equal
				// to a special day, then highlight it.

				int difference = calculateDifferenceInDays( sdf.parse( NEWYEAR ).getTime(), sdf.parse( cellDate ).getTime() );
				int calendarDay = ((difference % 96) + 96) % 96;

				if ( SPECIAL[ calendarDay ] == SP_HOLIDAY )
					return holidayRenderer;

				if ( SPECIAL[ calendarDay ] == SP_STATDAY )
					return statdayRenderer;
			}
			catch ( Exception e )
			{
			}

			return normalRenderer;
		}
	}

	public static void main( String [] args )
	{
		calculatePhases( new Date() );
		MoonPhaseDatabase.setMoonPhases( ronaldPhase, grimacePhase );

		KoLFrame uitest = new CalendarFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
