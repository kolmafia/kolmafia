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
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JEditorPane;
import java.text.SimpleDateFormat;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ca.bcit.geekkit.JCalendar;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A class which displays the calendar image to be used for today's
 * moon phases and today's calendar.
 */

public class CalendarFrame extends KoLFrame implements ListSelectionListener
{
	private static final SimpleDateFormat TODAY_FORMATTER = new SimpleDateFormat( "MMMM d, yyyy" );

	private int phaseError = Integer.MAX_VALUE;

	private int currentMonth = -1;
	private int currentDay = -1;

	private int ronaldPhase = -1;
	private int grimacePhase = -1;
	private int phaseStep = -1;

	private static final String [] MONTH_NAMES =
	{
		"Jarlsuary", "Frankruary", "Starch", "April", "Martinus", "Bill",
		"Bor", "Petember", "Carlvember", "Porktober", "Boozember", "Dougtember"
	};

	private static final String [] CALENDARS =
	{	"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
	};

	private JCalendar calendar;
	private LimitedSizeChatBuffer buffer;

	private Date selectedDate;
	private int selectedRow, selectedColumn;

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

		calendar = new JCalendar();
		calendar.getTable().getSelectionModel().addListSelectionListener( this );
		calendar.getTable().getColumnModel().getSelectionModel().addListSelectionListener( this );

		getContentPane().add( calendar, BorderLayout.EAST );
		setResizable( false );
	}

	public void valueChanged( ListSelectionEvent e )
	{
		// If the person has not yet released the
		// mouse, then do nothing.

		if ( e.getValueIsAdjusting() )
			return;

		// Compute which date is being selected
		// in the calendar table and update the
		// HTML on the center pane as appropriate

		if ( calendar.getTable().getSelectedRow() != selectedRow || calendar.getTable().getSelectedColumn() != selectedColumn )
		{
			try
			{
				selectedRow = calendar.getTable().getSelectedRow();
				selectedColumn = calendar.getTable().getSelectedColumn();

				StringBuffer dateString = new StringBuffer();
				dateString.append( calendar.getModel().getCurrentYear() );

				int selectedMonth = calendar.getModel().getCurrentMonth() + 1;
				if ( selectedMonth < 10 )
					dateString.append( '0' );
				dateString.append( selectedMonth );

				int selectedDay = Integer.parseInt( (String) calendar.getModel().getValueAt( selectedRow, selectedColumn ) );
				if ( selectedDay < 10 )
					dateString.append( '0' );
				dateString.append( selectedDay );

				selectedDate = sdf.parse( dateString.toString() );

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

	private final void calculatePhases( Date time )
	{
		// In order to ensure reliability, the value of the
		// date is fixed, rather than calculated.  This ms
		// value represents February 5, 2005 at 11:30pm on
		// the Eastern United States, which is when rollover
		// generally occurs.

		long newMoonDate = 1107664200000L;
		long dayLength = 24 * 60 * 60 * 1000L;

		long timeDifference = time.getTime() - newMoonDate;

		phaseStep = (((int) Math.floor( (double)timeDifference / (double)dayLength )) + 16) % 16;
		ronaldPhase = ((phaseStep % 8) + 8) % 8;
		grimacePhase = ((((int)Math.floor( phaseStep / 2 )) % 8) + 8) % 8;
	}

	/**
	 * Returns the calendar date for today.  This is calculated
	 * based on estimating today's server date using the current
	 * moon phase and relevant calendars, based on the local time
	 * on the machine.
	 */

	private final String getCalendarDay()
	{	return currentMonth == -1 ? "Penguinary 0" : MONTH_NAMES[ currentMonth ] + " " + currentDay;
	}

	/**
	 * Updates the HTML which displays the date and the information
	 * relating to the given date.  This should be called after all
	 * recalculation attempts.
	 */

	private void updateSummaryPage()
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

		// Next display today's moon phases, including
		// the uber-spiffy name for each phase.  Just
		// like in the browser, Ronald then Grimace.

		displayHTML.append( "<center><table><tr><td align=right><b>Ronald</b>:&nbsp;</td><td><img src=\"http://images.kingdomofloathing.com/itemimages/smoon" );
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
		displayHTML.append( String.valueOf( MoonPhaseDatabase.getGrueEffect( ronaldPhase, grimacePhase ) ) );
		displayHTML.append( "</td></tr><td align=right><b>Blood</b>:&nbsp;</td><td>" );
		displayHTML.append( String.valueOf( MoonPhaseDatabase.getBloodEffect( ronaldPhase, grimacePhase ) ) );
		displayHTML.append( "%</td></tr><td align=right><b>Baio</b>:&nbsp;</td><td>" );
		displayHTML.append( String.valueOf( MoonPhaseDatabase.getBaioEffect( ronaldPhase, grimacePhase ) ) );
		displayHTML.append( "%</td></tr></table></center>" );

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

	private void calculateCalendar( long timeCalculate )
	{
		try
		{
			// First, compute the time difference between
			// today and the start time for the year.
			// This difference should be computed in terms
			// of days for ease of later computations.

			long timeStart = sdf.parse( "20050614" ).getTime();
			long estimatedDifference = timeCalculate - timeStart;

			int estimatedDifferenceInDays = (int) (estimatedDifference / 86400000L);

			// Next, compare this value with the actual
			// computed phase step to see how far off
			// you are in the computation.

			int estimatedMoonPhase = ((estimatedDifferenceInDays % 16) + 16) % 16;

			if ( this.phaseError == Integer.MAX_VALUE )
			{
				this.phaseError = (estimatedMoonPhase == 15 && phaseStep == 0) ? -1 :
					(estimatedMoonPhase == 0 && phaseStep == 15) ? 1 : phaseStep - estimatedMoonPhase;
			}

			int actualDifferenceInDays = estimatedDifferenceInDays + this.phaseError;

			// Now that you have the actual difference
			// in days, do the computation of the KoL
			// calendar date.

			int daysSinceLastNewYear = ((actualDifferenceInDays % 96) + 96) % 96;

			currentMonth = daysSinceLastNewYear / 8;
			currentDay = (((daysSinceLastNewYear % 8) + 8) % 8) + 1;
		}
		catch ( Exception e )
		{
		}
	}

	public static void main( String [] args )
	{
		MoonPhaseDatabase.setMoonPhases( 0, 0 );
		KoLFrame uitest = new CalendarFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
