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
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import java.awt.Color;
import java.awt.BorderLayout;

import javax.swing.JTable;
import javax.swing.JTabbedPane;
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
	private static final SimpleDateFormat CALENDAR_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );
	private static final SimpleDateFormat TODAY_FORMATTER = new SimpleDateFormat( "MMMM d, yyyy", Locale.US );

	static
	{
		CALENDAR_FORMAT.setTimeZone( TimeZone.getDefault() );
		TODAY_FORMATTER.setTimeZone( TimeZone.getDefault() );
	}

	// Static array of file names (not including .gif extension)
	// for the various months in the KoL calendar.

	public static final String [] CALENDARS =
	{	"", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
	};

	// The following are static variables used to track the calendar.
	// They are made static as a design decision to allow the oracle
	// table nested inside of this class the access it needs to data.

	private static int calendarDay = 0;
	private static int ronaldPhase = -1;
	private static int grimacePhase = -1;
	private static int hamburglarPosition = -1;

	private static JCalendar calendar;
	private static OracleTable oracleTable;
	private static LimitedSizeChatBuffer dailyBuffer, predictBuffer;

	private static Calendar selectedDate;
	private static int selectedRow, selectedColumn;

	public CalendarFrame()
	{
		super( "Farmer's Almanac" );
		framePanel.setLayout( new BorderLayout() );

		selectedRow = -1;
		selectedColumn = -1;

		try
		{
			selectedDate = Calendar.getInstance( TimeZone.getDefault(), Locale.US );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		calculatePhases( selectedDate.getTime() );

		dailyBuffer = new LimitedSizeChatBuffer( false );
		predictBuffer = new LimitedSizeChatBuffer( false );

		JEditorPane dailyDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( dailyDisplay, 400, 300 );
		dailyDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		dailyBuffer.setChatDisplay( dailyDisplay );

		JEditorPane predictDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( predictDisplay, 400, 300 );
		predictBuffer.setChatDisplay( predictDisplay );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		tabs.addTab( "KoL One-a-Day", dailyDisplay );
		tabs.addTab( "Upcoming Events", predictDisplay );

		framePanel.add( tabs, BorderLayout.CENTER );

		calendar = new JCalendar( OracleTable.class );
		oracleTable = (OracleTable) calendar.getTable();
		oracleTable.getSelectionModel().addListSelectionListener( this );
		oracleTable.getColumnModel().getSelectionModel().addListSelectionListener( this );

		framePanel.add( calendar, BorderLayout.EAST );
		(new UpdateTabsThread()).start();
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

				selectedDate.set( calendar.getModel().getCurrentYear(), calendar.getModel().getCurrentMonth(),
					StaticEntity.parseInt( (String) calendar.getModel().getValueAt( selectedRow, selectedColumn ) ) );

				calculatePhases( selectedDate.getTime() );

				(new UpdateTabsThread()).start();
			}
			catch ( Exception e1 )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e1 );
			}
		}
	}

	/**
	 * Recalculates the moon phases given the time noted
	 * in the constructor.  This calculation assumes that
	 * the straightforward algorithm has no errors.
	 */

	private static final void calculatePhases( Date time )
	{
		// In order to ensure that everything is computed
		// based on new-year, wrap the date inside of the
		// formatter (which strips time information) and
		// reparse the date.

		calendarDay = MoonPhaseDatabase.getCalendarDay( time );
		int phaseStep = ((calendarDay % 16) + 16) % 16;

		ronaldPhase = phaseStep % 8;
		grimacePhase = phaseStep / 2;
		hamburglarPosition = MoonPhaseDatabase.getHamburglarPosition( time );
	}

	/**
	 * Updates the HTML which displays the date and the information
	 * relating to the given date.  This should be called after all
	 * recalculation attempts.
	 */

	private static void updateDailyPage()
	{
		if ( DATED_FILENAME_FORMAT.format( selectedDate.getTime() ).equals( "20051027" ) )
		{
			dailyBuffer.clearBuffer();
			dailyBuffer.append( "<center><h1>White Wednesday</h1></center>" );
			return;
		}

		StringBuffer displayHTML = new StringBuffer();

		// First display today's date along with the
		// appropriate calendar picture.  Include the
		// link shown in the clan calendar.

		displayHTML.append( "<center><table><tr><td valign=top>" );
		displayHTML.append( "<center><table border=1><tr><td align=center>drawn by <b>" );

		// Display either girls or boys of loathing, as desired

		String artistURL;
		String artistName;
		String artDirectory;

		if ( RNG.nextInt(2) == 1 )
		{
			artistURL = "http://elfwood.lysator.liu.se/loth/l/e/leigh/leigh.html";
			artistName = "SpaceMonkey";
			artDirectory = "bikini";
		}
		else
		{
			artistURL = "http://www.myimagehosting.com/album.php?u_id=2341UT9vj";
			artistName = "Cynn";
			artDirectory = "beefcake";
		}

		displayHTML.append( "<a href=\"" + artistURL + "\">" + artistName + "</a></b></td></tr>" );
		displayHTML.append( "<tr><td><img src=\"http://images.kingdomofloathing.com/otherimages/" + artDirectory + "/" );
		displayHTML.append( CALENDARS[ MoonPhaseDatabase.getCalendarMonth( selectedDate.getTime() ) ] );
		displayHTML.append( ".gif\"></td></tr><tr><td align=center>" );
		displayHTML.append( TODAY_FORMATTER.format( selectedDate.getTime() ) );
		displayHTML.append( "</td></tr><tr><td align=center><font size=+1><b>" );
		displayHTML.append( MoonPhaseDatabase.getCalendarDayAsString( selectedDate.getTime() ) );
		displayHTML.append( "</b></font></td></tr></table></center>" );

		displayHTML.append( "</td><td valign=top>" );
		displayHTML.append( "<center><table>" );

		// Holidays should probably be in the first
		// row, just in case.

		displayHTML.append( "<tr><td colspan=2 align=center><b>" );
		displayHTML.append( MoonPhaseDatabase.getHoliday( selectedDate.getTime() ) );
		displayHTML.append( "</b></td></tr><tr><td colspan=2></td></tr>" );

		// Next display today's moon phases, including
		// the uber-spiffy name for each phase.  Just
		// like in the browser, Ronald then Grimace.

		displayHTML.append( "<tr><td colspan=2 align=\"center\">" );
		int hamburglarLight = MoonPhaseDatabase.getHamburglarLight( ronaldPhase, grimacePhase, hamburglarPosition );

		if ( hamburglarPosition == 7 )
		{
			displayHTML.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/minimoon" );
			if ( hamburglarLight == 0 )
				displayHTML.append( "2" );
			displayHTML.append( ".gif\">" );
		}

		displayHTML.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/smoon" );
		displayHTML.append( ronaldPhase + 1 );

		if ( hamburglarPosition == 8 || hamburglarPosition == 9 )
			displayHTML.append( hamburglarPosition == 8 ? "a" : "b" );

		displayHTML.append( ".gif\">" );

		if ( hamburglarPosition == 4 || hamburglarPosition == 5 || hamburglarPosition == 10 )
		{
			displayHTML.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/minimoon" );
			if ( hamburglarLight == 0 )
				displayHTML.append( "2" );
			displayHTML.append( ".gif\">" );
		}

		displayHTML.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/smoon" );
		displayHTML.append( grimacePhase + 1 );

		if ( hamburglarPosition == 0 || hamburglarPosition == 1 )
			displayHTML.append( hamburglarPosition == 0 ? "a" : "b" );

		displayHTML.append( ".gif\">" );

		if ( hamburglarPosition == 2 )
		{
			displayHTML.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/minimoon" );
			if ( hamburglarLight == 0 )
				displayHTML.append( "2" );
			displayHTML.append( ".gif\">" );
		}

		displayHTML.append( "</td></tr><tr><td colspan=2></td></tr>" );

		displayHTML.append( "<tr><td align=right><b>Ronald</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getPhaseName( ronaldPhase ) );
		displayHTML.append( "</td></tr>" );
		displayHTML.append( "<tr><td align=right><b>Grimace</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getPhaseName( grimacePhase ) );
		displayHTML.append( "</td></tr>" );
		displayHTML.append( "<tr><td align=right><b>Stats</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getMoonEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr><td align=right><b>Grue</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getGrueEffect( ronaldPhase, grimacePhase, hamburglarPosition ) ? "bloodlusty" : "pacifistic" );
		displayHTML.append( "</td></tr><td align=right><b>Blood</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBloodEffect( ronaldPhase, grimacePhase, hamburglarPosition ) );
		displayHTML.append( "</td></tr><td align=right><b>Baio</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBaioEffect( ronaldPhase, grimacePhase, hamburglarPosition ) );
		displayHTML.append( "</td></tr><td align=right><b>Jekyllin</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getJekyllinEffect( ronaldPhase, grimacePhase, hamburglarPosition ) );
		displayHTML.append( "</td></tr></table></center>" );

		// That completes the table display!  More data
		// relevant to the current date may follow.
		// A forecast section, maybe, too - but for now,
		// this simple data should be enough.

		displayHTML.append( "</td></tr></table></center>" );

		// Now that the HTML has been completely
		// constructed, clear the display dailyBuffer
		// and append the appropriate text.

		dailyBuffer.clearBuffer();
		dailyBuffer.append( displayHTML.toString() );
	}

	/**
	 * Updates the HTML which displays the predictions for upcoming
	 * events on the KoL calendar.
	 */

	private static void updatePredictionsPage()
	{
		StringBuffer displayHTML = new StringBuffer();
		int phaseStep = MoonPhaseDatabase.getPhaseStep( ronaldPhase, grimacePhase );

		// First display today's date along with the
		// appropriate calendar picture.  Include the
		// link shown in the clan calendar.

		displayHTML.append( "<b><u>" );
		displayHTML.append( TODAY_FORMATTER.format( selectedDate.getTime() ) );
		displayHTML.append( "</u></b><br><i>" );
		displayHTML.append( MoonPhaseDatabase.getCalendarDayAsString( selectedDate.getTime() ) );
		displayHTML.append( "</i><br>&nbsp;<br>" );

		// Next display the upcoming stat days.

		displayHTML.append( "<b>Muscle Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString(
			Math.min( (24 - phaseStep) % 16, (25 - phaseStep) % 16 ) ) );
		displayHTML.append( "<br>" );

		displayHTML.append( "<b>Mysticality Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString(
			Math.min( (20 - phaseStep) % 16, (28 - phaseStep) % 16 ) ) );
		displayHTML.append( "<br>" );

		displayHTML.append( "<b>Moxie Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString(
			Math.min( (16 - phaseStep) % 16, (31 - phaseStep) % 16 ) ) );
		displayHTML.append( "<br>&nbsp;<br>" );

		// Next display the upcoming holidays.  This is done
		// through loop calculations in order to minimize the
		// amount of code done to handle individual holidays.

		String [] holidayPredictions = MoonPhaseDatabase.getHolidayPredictions( selectedDate.getTime() );
		for ( int i = 0; i < holidayPredictions.length; ++i )
		{
			displayHTML.append( "<b>" );
			displayHTML.append( holidayPredictions[i].replaceAll( ":", ":</b>&nbsp;" ) );
			displayHTML.append( "<br>" );
		}

		displayHTML.append( "</p>" );

		// Now that the HTML has been completely
		// constructed, clear the display dailyBuffer
		// and append the appropriate text.

		predictBuffer.clearBuffer();
		predictBuffer.append( displayHTML.toString() );
	}

	/**
	 * Utility method which appends the given percentage to
	 * the given string dailyBuffer, complete with + and % signs,
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
	 * Internal class which functions as a table for the
	 * JCalendar object.  Unlike the standard implementation
	 * used by JCalendar, this also highlights stat days and
	 * holidays on the KoL calendar.
	 */

	public static class OracleTable extends JTable
	{
		private Calendar dateCalculator;
		private CalendarTableModel model;
		private DefaultTableCellRenderer normalRenderer, todayRenderer, specialRenderer, holidayRenderer;
		private DefaultTableCellRenderer muscleRenderer, mysticalityRenderer, moxieRenderer;

		public OracleTable( CalendarTableModel model )
		{
			super( model );
			this.model = model;

			dateCalculator = Calendar.getInstance( TimeZone.getDefault(), Locale.US );
			normalRenderer = new DefaultTableCellRenderer();

			todayRenderer = new DefaultTableCellRenderer();
			todayRenderer.setForeground( new Color( 255, 255, 255 ) );
			todayRenderer.setBackground( new Color( 128, 128, 128 ) );

			specialRenderer = new DefaultTableCellRenderer();
			specialRenderer.setForeground( new Color( 255, 255, 255 ) );
			specialRenderer.setBackground( new Color( 0, 0, 0 ) );

			holidayRenderer = new DefaultTableCellRenderer();
			holidayRenderer.setForeground( new Color( 0, 0, 0 ) );
			holidayRenderer.setBackground( new Color( 255, 255, 204 ) );

			muscleRenderer = new DefaultTableCellRenderer();
			muscleRenderer.setForeground( new Color( 0, 0, 0 ) );
			muscleRenderer.setBackground( new Color( 255, 204, 204 ) );

			mysticalityRenderer = new DefaultTableCellRenderer();
			mysticalityRenderer.setForeground( new Color( 0, 0, 0 ) );
			mysticalityRenderer.setBackground( new Color( 204, 204, 255 ) );

			moxieRenderer = new DefaultTableCellRenderer();
			moxieRenderer.setForeground( new Color( 0, 0, 0 ) );
			moxieRenderer.setBackground( new Color( 204, 255, 204 ) );
		}

		public TableCellRenderer getCellRenderer( int row, int column )
		{
			try
			{
				// First, if the date today is equal to the
				// date selected, highlight it.

				String dayString = (String) model.getValueAt( row, column );
				if ( dayString.equals( "" ) )
					return normalRenderer;

				dateCalculator.set( model.getCurrentYear(), model.getCurrentMonth(), StaticEntity.parseInt( dayString ) );
				Date selectedTime = dateCalculator.getTime();

				if ( CALENDAR_FORMAT.format( new Date() ).equals( CALENDAR_FORMAT.format( dateCalculator.getTime() ) ) )
					return todayRenderer;

				// White wednesday special highlighting.
				// But, because white doesn't show up,
				// make it black instead.

				if ( DATED_FILENAME_FORMAT.format( dateCalculator.getTime() ).equals( "20051027" ) )
					return specialRenderer;

				// Otherwise, if the date selected is equal
				// to a special day, then highlight it.

				if ( MoonPhaseDatabase.isRealLifeHoliday( selectedTime ) )
					return holidayRenderer;

				if ( MoonPhaseDatabase.isHoliday( selectedTime ) )
					return holidayRenderer;

				if ( MoonPhaseDatabase.isMuscleDay( selectedTime ) )
					return muscleRenderer;

				if ( MoonPhaseDatabase.isMysticalityDay( selectedTime ) )
					return mysticalityRenderer;

				if ( MoonPhaseDatabase.isMoxieDay( selectedTime ) )
					return moxieRenderer;
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			return normalRenderer;
		}
	}

	/**
	 * Special thread which allows the daily page and predictions
	 * page to be updated outside of the Swing thread -- this means
	 * images can be downloaded without locking the UI.
	 */

	private class UpdateTabsThread extends Thread
	{
		public void run()
		{
			synchronized ( UpdateTabsThread.class )
			{
				updateDailyPage();
				updatePredictionsPage();
			}
		}
	}
}
