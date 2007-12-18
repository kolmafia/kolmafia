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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import ca.bcit.geekkit.CalendarTableModel;
import ca.bcit.geekkit.JCalendar;

public class CalendarFrame extends KoLFrame implements ListSelectionListener
{
	public static final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );
	public static final SimpleDateFormat LONG_FORMAT = new SimpleDateFormat( "MMMM d, yyyy", Locale.US );

	static
	{
		SHORT_FORMAT.setTimeZone( TimeZone.getDefault() );
		LONG_FORMAT.setTimeZone( TimeZone.getDefault() );
	}

	// static final array of file names (not including .gif extension)
	// for the various months in the KoL calendar.

	public static final String [] CALENDARS =
	{	"", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
	};

	// The following are static final variables used to track the calendar.
	// They are made static final as a design decision to allow the oracle
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
		this.setDefaultCloseOperation( HIDE_ON_CLOSE );
		this.framePanel.setLayout( new BorderLayout() );

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

		RequestPane dailyDisplay = new RequestPane();
		JComponentUtilities.setComponentSize( dailyDisplay, 400, 335 );
		dailyDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		dailyBuffer.setChatDisplay( dailyDisplay );

		RequestPane predictDisplay = new RequestPane();
		JComponentUtilities.setComponentSize( predictDisplay, 400, 335 );
		predictBuffer.setChatDisplay( predictDisplay );

		this.tabs.addTab( "KoL One-a-Day", dailyDisplay );
		this.tabs.addTab( "Upcoming Events", predictDisplay );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );

		calendar = new JCalendar( OracleTable.class );
		oracleTable = (OracleTable) calendar.getTable();
		oracleTable.getSelectionModel().addListSelectionListener( this );
		oracleTable.getColumnModel().getSelectionModel().addListSelectionListener( this );

		this.framePanel.add( calendar, BorderLayout.EAST );
		this.updateTabs();
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
				this.updateTabs();
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

	private static final void updateDailyPage()
	{
		if ( DAILY_FORMAT.format( selectedDate.getTime() ).equals( "20051027" ) )
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
		displayHTML.append( LONG_FORMAT.format( selectedDate.getTime() ) );
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

	private static final void updatePredictionsPage()
	{
		StringBuffer displayHTML = new StringBuffer();
		int phaseStep = MoonPhaseDatabase.getPhaseStep( ronaldPhase, grimacePhase );

		// First display today's date along with the
		// appropriate calendar picture.  Include the
		// link shown in the clan calendar.

		displayHTML.append( "<b><u>" );
		displayHTML.append( LONG_FORMAT.format( selectedDate.getTime() ) );
		displayHTML.append( "</u></b><br><i>" );
		displayHTML.append( MoonPhaseDatabase.getCalendarDayAsString( selectedDate.getTime() ) );
		displayHTML.append( "</i><br>&nbsp;<br>" );

		MoonPhaseDatabase.addPredictionHTML( displayHTML, selectedDate.getTime(), phaseStep );

		predictBuffer.clearBuffer();
		predictBuffer.append( displayHTML.toString() );
	}


	/**
	 * Utility method which appends the given percentage to
	 * the given string dailyBuffer, complete with + and % signs,
	 * wherever applicable.  Also appends "no effect" if the
	 * percentage is zero.
	 */

	private static final void appendModifierPercentage( StringBuffer buffer, int percentage )
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

			this.dateCalculator = Calendar.getInstance( TimeZone.getDefault(), Locale.US );
			this.normalRenderer = new DefaultTableCellRenderer();

			this.todayRenderer = new DefaultTableCellRenderer();
			this.todayRenderer.setForeground( new Color( 255, 255, 255 ) );
			this.todayRenderer.setBackground( new Color( 128, 128, 128 ) );

			this.specialRenderer = new DefaultTableCellRenderer();
			this.specialRenderer.setForeground( new Color( 255, 255, 255 ) );
			this.specialRenderer.setBackground( new Color( 0, 0, 0 ) );

			this.holidayRenderer = new DefaultTableCellRenderer();
			this.holidayRenderer.setForeground( new Color( 0, 0, 0 ) );
			this.holidayRenderer.setBackground( new Color( 255, 255, 204 ) );

			this.muscleRenderer = new DefaultTableCellRenderer();
			this.muscleRenderer.setForeground( new Color( 0, 0, 0 ) );
			this.muscleRenderer.setBackground( new Color( 255, 204, 204 ) );

			this.mysticalityRenderer = new DefaultTableCellRenderer();
			this.mysticalityRenderer.setForeground( new Color( 0, 0, 0 ) );
			this.mysticalityRenderer.setBackground( new Color( 204, 204, 255 ) );

			this.moxieRenderer = new DefaultTableCellRenderer();
			this.moxieRenderer.setForeground( new Color( 0, 0, 0 ) );
			this.moxieRenderer.setBackground( new Color( 204, 255, 204 ) );
		}

		public TableCellRenderer getCellRenderer( int row, int column )
		{
			try
			{
				// First, if the date today is equal to the
				// date selected, highlight it.

				String dayString = (String) this.model.getValueAt( row, column );
				if ( dayString.equals( "" ) )
					return this.normalRenderer;

				this.dateCalculator.set( this.model.getCurrentYear(), this.model.getCurrentMonth(), StaticEntity.parseInt( dayString ) );
				Date selectedTime = this.dateCalculator.getTime();

				if ( SHORT_FORMAT.format( new Date() ).equals( SHORT_FORMAT.format( this.dateCalculator.getTime() ) ) )
					return this.todayRenderer;

				// White wednesday special highlighting.
				// But, because white doesn't show up,
				// make it black instead.

				if ( DAILY_FORMAT.format( this.dateCalculator.getTime() ).equals( "20051027" ) )
					return this.specialRenderer;

				// Otherwise, if the date selected is equal
				// to a special day, then highlight it.

				if ( MoonPhaseDatabase.isRealLifeHoliday( selectedTime ) )
					return this.holidayRenderer;

				if ( MoonPhaseDatabase.isHoliday( selectedTime ) )
					return this.holidayRenderer;

				if ( MoonPhaseDatabase.isMuscleDay( selectedTime ) )
					return this.muscleRenderer;

				if ( MoonPhaseDatabase.isMysticalityDay( selectedTime ) )
					return this.mysticalityRenderer;

				if ( MoonPhaseDatabase.isMoxieDay( selectedTime ) )
					return this.moxieRenderer;
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			return this.normalRenderer;
		}
	}

	public synchronized void updateTabs()
	{
		updateDailyPage();
		updatePredictionsPage();
	}
}
