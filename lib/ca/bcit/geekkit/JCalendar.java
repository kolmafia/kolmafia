/**
 * @(#)JCalendar.java	0.1 28/06/2002
 *
 * Copyright (c) 2002 Arron Ferguson
 *
 */

package ca.bcit.geekkit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * <p>
 * <code>JCalendar</code> is a complex widget that keeps track of a calendar. It uses its own customized
 * <code>TableModel</code> for keeping track of specific dates that may require custom rendrering other than the usual
 * number being in each cell.
 * </p>
 * 
 * @version 0.1 28/06/2002
 * @author Arron Ferguson
 */
public class JCalendar
	extends JPanel
	implements ActionListener
{
	/**
	 * For clicking on to take the calendar to the next month
	 */
	private JButton nextButton;

	/**
	 * For clicking on to take the calendar to the previous month
	 */
	private JButton previousButton;

	/**
	 * Displays the month and the year inside of the calendar layout
	 */
	protected JLabel label;

	/**
	 * The set of rows and columns used to display dates.
	 */
	protected JTable table;

	/**
	 * Layout that allows for a grid like layout pattern. Components do not have to take up exactly one cell, instead
	 * they can take up more than one row or column.
	 */
	private GraphPaperLayout gp;

	/**
	 * A custom <code>TableModel</code> for dealing with specifically calendar like cells
	 */
	private CalendarTableModel model;

	private final Class tableClass;

	/**
	 * Default constructor
	 */

	public JCalendar()
	{
		this( JCalendarTable.class );
	}

	public JCalendar( final Class tableClass )
	{
		super();

		this.tableClass = tableClass;

		this.configUI();

		this.nextButton.addActionListener( this );
		this.previousButton.addActionListener( this );
	}

	/**
	 * Configures the UI and sets up the renderers
	 */

	private void configUI()
	{
		this.gp = new GraphPaperLayout( new Dimension( 8, 10 ) );
		this.setLayout( this.gp );
		this.nextButton = new JButton( "Next" );
		this.previousButton = new JButton( "Back" );
		this.label = new JLabel( "", SwingConstants.CENTER );

		this.model = new CalendarTableModel( this );
		this.initializeTable();

		this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		// don't allow rows or columns to be selected
		this.table.setCellSelectionEnabled( true );
		this.table.setColumnSelectionAllowed( false );
		this.table.setRowSelectionAllowed( false );

		// don't allow cells to be selected
		this.table.setFocusable( true );

		// little bit of customization of cell renderers
		JLabel cell = (JLabel) this.table.getDefaultRenderer( JLabel.class );
		cell.setHorizontalAlignment( SwingConstants.LEFT );
		cell.setVerticalAlignment( SwingConstants.TOP );

		this.table.getTableHeader().setReorderingAllowed( false );
		// add buttons
		this.add( this.previousButton, new Rectangle( 0, 0, 2, 1 ) );
		this.add( this.nextButton, new Rectangle( 6, 0, 2, 1 ) );
		// add label
		this.add( this.label, new Rectangle( 2, 0, 4, 1 ) );
		this.add( this.table.getTableHeader(), new Rectangle( 0, 2, 8, 1 ) );
		this.add( this.table, new Rectangle( 0, 3, 8, 7 ) );

		// now call it for a populate
		this.model.generateCalendarMonth( 0 );
	}

	/**
	 * Handles the two <code>JButton</code>s events for going forward and backward in the years
	 * 
	 * @param e the <code>ActionEvent</code> given.
	 */
	public void actionPerformed( final ActionEvent e )
	{
		if ( e.getSource() == this.nextButton )
		{
			this.model.generateCalendarMonth( 1 );
		}
		else if ( e.getSource() == this.previousButton )
		{
			this.model.generateCalendarMonth( -1 );
		}
	}

	/**
	 * Custom paint to allow cells to change height based on the size of the <code>Container</code> that the table is
	 * in
	 * 
	 * @param g the <code>Graphics</code> context used to paint the table.
	 */
	@Override
	public void paint( final Graphics g )
	{
		super.paint( g );

		// make row height resize as the component is resized so that rows fill up the space
		float componentHeight = this.table.getHeight();
		float numberofRows = this.table.getRowCount();
		float tableCellHeight = componentHeight / numberofRows;
		int height = (int) tableCellHeight;
		this.table.setRowHeight( height );
	}

	/**
	 * Returns the preferred size of this composite component
	 * 
	 * @return the width and height of this component as a <code>Dimension</code> object and as its preferred size to
	 *         be rendered.
	 */
	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension( 310, 220 );
	}

	/**
	 * Returns the minimum size that this composite component should be drawn at
	 * 
	 * @return the minimum width and height that this component should be rendered at
	 */
	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension( 260, 170 );
	}

	public JTable getTable()
	{
		return this.table;
	}

	public CalendarTableModel getModel()
	{
		return this.model;
	}

	private void initializeTable()
	{
		try
		{
			if ( JTable.class.isAssignableFrom( this.tableClass ) )
			{
				Object[] parameters = new Object[ 1 ];
				parameters[ 0 ] = this.model;

				Class[] parameterTypes = new Class[ 1 ];
				parameterTypes[ 0 ] = CalendarTableModel.class;

				this.table = (JTable) this.tableClass.getConstructor( parameterTypes ).newInstance( parameters );
			}
		}
		catch ( Exception e )
		{
		}

		if ( this.table == null )
		{
			this.table = new JCalendarTable( this.model );
		}
	}

	public class JCalendarTable
		extends JTable
	{
		private final DefaultTableCellRenderer highlighter;

		public JCalendarTable( final CalendarTableModel model )
		{
			super( model );

			this.highlighter = new DefaultTableCellRenderer();
			this.highlighter.setForeground( new Color( 255, 255, 255 ) );
			this.highlighter.setBackground( new Color( 0, 0, 128 ) );
		}

		@Override
		public TableCellRenderer getCellRenderer( final int row, final int column )
		{
			if ( String.valueOf( JCalendar.this.model.getCurrentDate() ).equals(
				JCalendar.this.model.getValueAt( row, column ) ) && JCalendar.this.model.getCurrentMonth() == JCalendar.this.model.getMonth() && JCalendar.this.model.getCurrentYear() == JCalendar.this.model.getYear() )
			{
				return this.highlighter;
			}

			return super.getCellRenderer( row, column );
		}
	}

	/**
	 * For running this program
	 */
	public static void main( final String[] args )
	{
		JCalendar jc = new JCalendar();
		JFrame frame = new JFrame( "calendar" );
		frame.getContentPane().add( jc );
		Dimension frameD = new Dimension( 310, 220 );
		Dimension screenD = new Dimension();
		screenD = Toolkit.getDefaultToolkit().getScreenSize();
		if ( frameD.width >= screenD.width )
		{
			frame.setLocation( 1, 1 );
		}
		frame.setLocation( ( ( screenD.width - frameD.width ) / 2 ), ( ( screenD.height - frameD.height ) / 2 ) );
		frame.setSize( frameD.width, frameD.height );
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				System.exit( 0 );
			}
		} );
		frame.setVisible( true );
	}
}
