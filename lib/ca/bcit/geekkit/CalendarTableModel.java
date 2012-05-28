/**
 * @(#)CalendarTableModel.java	0.1 28/06/2002
 *
 * Copyright (c) 2002 Arron Ferguson
 *
 */

package ca.bcit.geekkit;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

/**
 * The custom <code>TableModel</code> for dealing with the calendar and dealing with <code>Date</code>s and the
 * <code>GregorianCalendar</code>
 * 
 * @version 0.1 28/06/2002
 * @author Arron Ferguson
 */

public class CalendarTableModel
	extends AbstractTableModel
{
	final String[] months =
		{ "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

	/**
	 * Column names for the calendar.
	 */
	final Vector columnNames = new Vector( 7 );

	{
		this.columnNames.add( "Sun" );
		this.columnNames.add( "Mon" );
		this.columnNames.add( "Tue" );
		this.columnNames.add( "Wed" );
		this.columnNames.add( "Thu" );
		this.columnNames.add( "Fri" );
		this.columnNames.add( "Sat" );
	};

	/**
	 * The actual data for the table. The data is in a dynamic list instead of the usual 2D array
	 */
	private Vector data;

	// initial 2D table data ... hard coded but since a dynamic collection we can
	// change this.
	{
		this.data = new Vector( 6 );

		for ( int i = 0; i < 6; i++ )
		{
			Vector v = new Vector( 7 );
			for ( int j = 0; j < 7; j++ )
			{
				v.add( "" );
			}
			this.data.add( v );
		}
	};

	/**
	 * Reference to the actual view and controller which is known as the delegate in MVC speak
	 */
	private final JCalendar delegate;

	/**
	 * The calendar that keeps track of dates, months and leap years. This object is called upon many times to render
	 * the month.
	 */
	private final GregorianCalendar calendar;

	/**
	 * The date at which the calendar is currently pointing at. This value will change as the calendar moves forward and
	 * backward through calendar months.
	 */
	private int currentDate;

	/**
	 * The month at which the calendar is currently pointing at. This value will change as the calendar moves forward
	 * and backward through calendar months.
	 */
	private int currentMonth;

	/**
	 * The year at which the calendar is currently pointing at. This value will change as the calendar moves forward and
	 * backward through calendar months.
	 */
	private int currentYear;

	/**
	 * The time stamp to indicate the actual date as determined by the system's clock
	 */
	private final int date;

	/**
	 * The first day in the week at which the date starts at. For example, the first of the month may be Wednesday.
	 */
	private int startday;

	/**
	 * The time stamp to indicate the actual month as determined by the system's clock
	 */
	private final int month;

	/**
	 * The time stamp to indicate the actual year as determined by the system's clock
	 */
	private final int year;

	/**
	 * Constructor for referencing the <code>JCalendar</code>
	 */
	public CalendarTableModel( final JCalendar caller )
	{
		this.delegate = caller;
		// set up the calendar
		this.calendar = new GregorianCalendar();
		this.date = this.calendar.get( Calendar.DAY_OF_MONTH );
		this.month = this.calendar.get( Calendar.MONTH );
		this.year = this.calendar.get( Calendar.YEAR );
		this.currentDate = this.date;
		this.currentMonth = this.month;
		this.currentYear = this.year;
	}

	/**
	 * Returns the column count of this <code>TableModel</code>
	 * 
	 * @return the number of columns for this <code>TableModel</code>
	 */
	public int getColumnCount()
	{
		return this.columnNames.size();
	}

	/**
	 * Return the current date which is where the calendar is pointing at. This current date may or may not be the
	 * actual real date as determined by the system clock.
	 * 
	 * @return the current date being pointed to
	 */
	public int getCurrentDate()
	{
		return this.calendar.get( Calendar.DAY_OF_MONTH );
	}

	/**
	 * Return the current month which is where the calendar is pointing at. This current date may or may not be the
	 * actual real date as determined by the system clock.
	 * 
	 * @return the current month being pointed to
	 */
	public int getCurrentMonth()
	{
		return this.calendar.get( Calendar.MONTH );
	}

	/**
	 * Return the current year which is where the calendar is pointing at. This current date may or may not be the
	 * actual real date as determined by the system clock.
	 * 
	 * @return the current year being pointed to
	 */
	public int getCurrentYear()
	{
		return this.calendar.get( Calendar.YEAR );
	}

	/**
	 * Return the real date based on the system clock
	 * 
	 * @return the actual date based on the system clock
	 */
	public int getDate()
	{
		return this.date;
	}

	/**
	 * Return the real month based on the system clock
	 * 
	 * @return the actual month based on the system clock
	 */
	public int getMonth()
	{
		return this.month;
	}

	/**
	 * Return the real year based on the system clock
	 * 
	 * @return the actual year based on the system clock
	 */
	public int getYear()
	{
		return this.year;
	}

	/**
	 * Return the number of rows in this table
	 * 
	 * @return the number of rows as an int
	 */
	public int getRowCount()
	{
		return this.data.size();
	}

	/**
	 * Returns the column name based on the index <code>col</code>
	 * 
	 * @param col the column at which a name is being requested for
	 * @return the <code>String</code> name of column <code>col</code>
	 */
	@Override
	public String getColumnName( final int col )
	{
		String s = (String) this.columnNames.get( col );
		return s;
	}

	/**
	 * Return the <code>Object</code> at a particular cell
	 * 
	 * @param row the row to index
	 * @param col the column to index
	 * @return the <code>Object</code> returned from a particular cell
	 */
	public Object getValueAt( final int row, final int col )
	{
		return ( (Vector) this.data.elementAt( row ) ).elementAt( col );
	}

	/*
	 * JTable uses this method to determine the default renderer/editor for each cell @param col the column to index
	 * @return the class of the object that is being used to render this column
	 */
	@Override
	public Class getColumnClass( final int col )
	{
		return this.getValueAt( 0, col ).getClass();
	}

	/*
	 * Allow for the ability to edit information in each cell of this table @param value the actual value being passed
	 * int at a particular cell in the table @param row the row at which to add the <code>Object</code> @param row the
	 * column at which to add the <code>Object</code>
	 */
	@Override
	public void setValueAt( final Object value, final int row, final int col )
	{
		( (Vector) this.data.elementAt( row ) ).set( col, value );
		this.fireTableCellUpdated( row, col );
	}

	/**
	 * Format a date and return it as a <code>String</code>
	 * 
	 * @return the date as a <code>String</code>
	 */
	public String simpleDate()
	{
		Locale locale = new Locale( "en", "CANADA" );
		DateFormat formatter = DateFormat.getDateInstance( DateFormat.FULL, locale );
		Date signoutDate = new Date();
		String now = formatter.format( signoutDate );
		return now;
	}

	/**
	 * Generates an entire month and populates the table/cell model with the values. This method will start off with the
	 * present date which is based on the system clock. In order to change the month that is being displayed, a roll
	 * value is given. If the roll value is 1, then the calendar moves ahead one month. If the roll value is -1, then
	 * the calendar moves back one month. If 0 is given as a roll value, then the current date that is in the calendar
	 * is used.
	 * 
	 * @param rollValue the value to move the calendar forwards or backwards
	 */
	public void generateCalendarMonth( final int rollValue )
	{
		if ( rollValue == 0 || rollValue < -1 || rollValue > 1 )
		{
			; // don't do anything since that value is non-valid
		}
		else
		{
			this.calendar.set( Calendar.MONTH, ( this.calendar.get( Calendar.MONTH ) + rollValue ) );
		}

		this.currentYear = this.calendar.get( Calendar.YEAR );
		this.currentMonth = this.calendar.get( Calendar.MONTH );

		// going to go to the first of the month to get where it falls upon within the week.
		// example: the 1st of the month is Monday?
		int tempDate = this.calendar.get( Calendar.DATE );

		this.calendar.set( Calendar.DATE, 1 );
		this.startday = this.calendar.get( Calendar.DAY_OF_WEEK );
		// arrays start at 0 so decrement by 1
		this.startday-- ;

		// now put it back
		this.calendar.set( Calendar.DATE, tempDate );

		// populate the label in the JCalendar
		this.delegate.label.setText( this.months[ this.currentMonth ] + " " + this.currentYear );

		// precalculate vector sizes. This assumes that all vectors
		// within the main vector will be of equal size.
		int columnMax = ( (Vector) this.data.elementAt( 0 ) ).size();
		// all months start at 1 so this can be hard-coded
		this.currentDate = 1;
		// precalculate the maximum date number for the current month. February may have
		// 28 or 29 days if it's a leap year, July has 31 days, etc.
		int maxMonthDate = this.calendar.getActualMaximum( Calendar.DATE );
		// increment because we are dealing with arrays
		maxMonthDate++ ;

		// populate the cells in the table model
		for ( int i = 0; i < this.data.size(); i++ )
		{
			for ( int k = 0; k < columnMax; k++ )
			{
				// we need to check if a it's the 1st row. If it is, the 1st of the month
				// may not start on the first column (column 0). We need to check for this.
				if ( i > 0 )
				{
					this.setValueAt( ( this.currentDate + "" ), i, k );
					this.currentDate++ ;
				}
				else if ( k >= this.startday )
				{
					this.setValueAt( ( this.currentDate + "" ), i, k );
					this.currentDate++ ;
				}
				else
				{
					this.setValueAt( "", i, k );
				}
				if ( this.currentDate > maxMonthDate )
				{
					this.setValueAt( "", i, k );
				}
				if ( this.currentDate == this.date )
				{
					;
				}
			}
		}
	}
}
