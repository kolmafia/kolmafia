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
import java.awt.BorderLayout;

import javax.swing.JPanel;
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
	// Static array of file names (not including .gif extension)
	// for the various months in the KoL calendar.

	private static final String [] CALENDARS =
	{	"", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
	};

	// Special date formatter which formats according to
	// the standard Western format of month, day, year.

	public static final SimpleDateFormat TODAY_FORMATTER = new SimpleDateFormat( "MMMM d, yyyy" );

	// The following are static variables used to track the calendar.
	// They are made static as a design decision to allow the oracle
	// table nested inside of this class the access it needs to data.

	private static int ronaldPhase = -1;
	private static int grimacePhase = -1;

	private static JCalendar calendar;
	private static OracleTable oracleTable;
	private static LimitedSizeChatBuffer dailyBuffer, predictBuffer;

	private static Date selectedDate;
	private static int selectedRow, selectedColumn;

	public CalendarFrame()
	{
		super( "Farmer's Almanac" );
		framePanel.setLayout( new BorderLayout() );

		selectedRow = -1;
		selectedColumn = -1;

		try
		{
			selectedDate = sdf.parse( sdf.format( new Date() ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		calculatePhases( selectedDate );

		dailyBuffer = new LimitedSizeChatBuffer( "KoLmafia: Calendar", false );
		predictBuffer = new LimitedSizeChatBuffer( "KoLmafia: Next Event", false );

		JEditorPane dailyDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( dailyDisplay, 400, 300 );
		dailyDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		dailyBuffer.setChatDisplay( dailyDisplay );

		JEditorPane predictDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( predictDisplay, 400, 300 );
		predictBuffer.setChatDisplay( predictDisplay );

		tabs = new JTabbedPane();
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

				String selectedDateString = constructDateString( calendar.getModel(), selectedRow, selectedColumn );

				if ( selectedDateString.equals( "" ) )
					return;

				selectedDate = sdf.parse( selectedDateString );

				calculatePhases( selectedDate );
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
	 * Utility method which constructs a date string based on
	 * the given calendar model and the given selected row
	 * and column.  This date string conforms to the standard
	 * YYYYMMdd format.
	 */

	private static String constructDateString( CalendarTableModel model, int selectedRow, int selectedColumn )
	{
		String dayString = (String) model.getValueAt( selectedRow, selectedColumn );
		if ( dayString.equals( "" ) )
			return "";

		int year = model.getCurrentYear();
		int month = model.getCurrentMonth() + 1;
		int day = Integer.parseInt( dayString );

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
		// In order to ensure that everything is computed
		// based on new-year, wrap the date inside of the
		// formatter (which strips time information) and
		// reparse the date.

		int calendarDay = MoonPhaseDatabase.getCalendarDay( time );
		int phaseStep = ((calendarDay % 16) + 16) % 16;

		ronaldPhase = phaseStep % 8;
		grimacePhase = phaseStep / 2;
	}

	/**
	 * Updates the HTML which displays the date and the information
	 * relating to the given date.  This should be called after all
	 * recalculation attempts.
	 */

	private static void updateDailyPage()
	{
		if ( sdf.format( selectedDate ).equals( "20051027" ) )
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
		displayHTML.append( CALENDARS[ MoonPhaseDatabase.getCalendarMonth( selectedDate ) ] );
		displayHTML.append( ".gif\"></td></tr><tr><td align=center>" );
		displayHTML.append( TODAY_FORMATTER.format( selectedDate ) );
		displayHTML.append( "</td></tr><tr><td align=center><font size=+1><b>" );
		displayHTML.append( MoonPhaseDatabase.getCalendarDayAsString( selectedDate ) );
		displayHTML.append( "</b></font></td></tr></table></center>" );

		displayHTML.append( "</td><td valign=top>" );
		displayHTML.append( "<center><table>" );

		// Holidays should probably be in the first
		// row, just in case.

		displayHTML.append( "<tr><td colspan=2 align=center><b>" );
		displayHTML.append( MoonPhaseDatabase.getHoliday( selectedDate ) );
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
		displayHTML.append( MoonPhaseDatabase.getMoonEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr><td align=right><b>Grue</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getGrueEffect( ronaldPhase, grimacePhase ) ? "bloodlusty" : "pacifistic" );
		displayHTML.append( "</td></tr><td align=right><b>Blood</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBloodEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr><td align=right><b>Baio</b>:&nbsp;</td><td>" );
		appendModifierPercentage( displayHTML, MoonPhaseDatabase.getBaioEffect( ronaldPhase, grimacePhase ) );
		displayHTML.append( "</td></tr><td align=right><b>Jekyllin</b>:&nbsp;</td><td>" );
		displayHTML.append( MoonPhaseDatabase.getJekyllinEffect( ronaldPhase, grimacePhase ) );
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
		displayHTML.append( TODAY_FORMATTER.format( selectedDate ) );
		displayHTML.append( "</u></b><br><i>" );
		displayHTML.append( MoonPhaseDatabase.getCalendarDayAsString( selectedDate ) );
		displayHTML.append( "</i>" );

		// Next display the upcoming stat days.

		displayHTML.append( "<p><b>Muscle Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString( Math.min( (24 - phaseStep) % 16, (25 - phaseStep) % 16 ) ) );
		displayHTML.append( "<br>" );

		displayHTML.append( "<b>Mysticality Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString( Math.min( (20 - phaseStep) % 16, (28 - phaseStep) % 16 ) ) );
		displayHTML.append( "<br>" );

		displayHTML.append( "<b>Moxie Day</b>:&nbsp;" );
		displayHTML.append( MoonPhaseDatabase.getDayCountAsString( Math.min( (16 - phaseStep) % 16, (31 - phaseStep) % 16 ) ) );
		displayHTML.append( "</p><p>" );

		// Next display the upcoming holidays.  This is done
		// through loop calculations in order to minimize the
		// amount of code done to handle individual holidays.

		String [] holidayPredictions = MoonPhaseDatabase.getHolidayPredictions( selectedDate );
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
		private CalendarTableModel model;
		private DefaultTableCellRenderer normalRenderer, todayRenderer, specialRenderer, holidayRenderer;
		private DefaultTableCellRenderer muscleRenderer, mysticalityRenderer, moxieRenderer;

		public OracleTable( CalendarTableModel model )
		{
			super( model );
			this.model = model;

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

				String todayDateString = sdf.format( new Date() );
				String cellDateString = constructDateString( model, row, column );

				if ( cellDateString.equals( "" ) )
					return normalRenderer;

				if ( todayDateString.equals( cellDateString ) )
					return todayRenderer;

				// White wednesday special highlighting.
				// But, because white doesn't show up,
				// make it black instead.

				if ( cellDateString.equals( "20051027" ) )
					return specialRenderer;

				// Otherwise, if the date selected is equal
				// to a special day, then highlight it.

				Date cellDate = sdf.parse( cellDateString );

				if ( MoonPhaseDatabase.isHoliday( cellDate ) )
					return holidayRenderer;

				if ( MoonPhaseDatabase.isMuscleDay( cellDate ) )
					return muscleRenderer;

				if ( MoonPhaseDatabase.isMysticalityDay( cellDate ) )
					return mysticalityRenderer;

				if ( MoonPhaseDatabase.isMoxieDay( cellDate ) )
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
			updateDailyPage();
			updatePredictionsPage();
		}
	}
}
