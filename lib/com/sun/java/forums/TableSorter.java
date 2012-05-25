
package com.sun.java.forums;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * TableSorter is a decorator for TableModels; adding sorting functionality to a supplied TableModel. TableSorter does
 * not store or copy the data in its TableModel; instead it maintains a map from the row indexes of the view to the row
 * indexes of the model. As requests are made of the sorter (like getValueAt(row, col)) they are passed to the
 * underlying model after the row numbers have been translated via the internal mapping array. This way, the TableSorter
 * appears to hold another copy of the table with the rows in a different order. <p/> TableSorter registers itself as a
 * listener to the underlying model, just as the JTable itself would. Events recieved from the model are examined,
 * sometimes manipulated (typically widened), and then passed on to the TableSorter's listeners (typically the JTable).
 * If a change to the model has invalidated the order of TableSorter's rows, a note of this is made and the sorter will
 * resort the rows the next time a value is requested. <p/> When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the table header may be used as a complete UI for
 * TableSorter. The default renderer of the tableHeader is decorated with a renderer that indicates the sorting status
 * of each column. In addition, a mouse listener is installed with the following behavior:
 * <ul>
 * <li> Mouse-click: Clears the sorting status of all other columns and advances the sorting status of that column
 * through three values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to NOT_SORTED again).
 * <li> SHIFT-mouse-click: Clears the sorting status of all other columns and cycles the sorting status of the column
 * through the same three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li> CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except that the changes to the column do not cancel
 * the statuses of columns that are already sorting - giving a way to initiate a compound sort.
 * </ul>
 * <p/> This is a long overdue rewrite of a class of the same name that first appeared in the swing table demos in 1997.
 * 
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */

public class TableSorter
	extends AbstractTableModel
{
	protected TableModel tableModel;

	public static final int DESCENDING = -1;
	public static final int NOT_SORTED = 0;
	public static final int ASCENDING = 1;

	private static Directive EMPTY_DIRECTIVE = new Directive( -1, TableSorter.NOT_SORTED );

	public static final Comparator COMPARABLE_COMAPRATOR = new Comparator()
	{
		public int compare( Object o1, Object o2 )
		{
			return ( (Comparable) o1 ).compareTo( o2 );
		}
	};
	public static final Comparator LEXICAL_COMPARATOR = new Comparator()
	{
		public int compare( Object o1, Object o2 )
		{
			return o1.toString().toLowerCase().compareTo( o2.toString().toLowerCase() );
		}
	};

	private Row[] viewToModel;
	private int[] modelToView;

	private JTableHeader tableHeader;
	private final MouseListener mouseListener;
	private final TableModelListener tableModelListener;
	private final Map columnComparators = new HashMap();
	private final ArrayList sortingColumns = new ArrayList();

	public TableSorter()
	{
		this.mouseListener = new MouseHandler();
		this.tableModelListener = new TableModelHandler();
	}

	public TableSorter( final TableModel tableModel )
	{
		this();
		this.setTableModel( tableModel );
	}

	public TableSorter( final TableModel tableModel, final JTableHeader tableHeader )
	{
		this();
		this.setTableHeader( tableHeader );
		this.setTableModel( tableModel );
	}

	private void clearSortingState()
	{
		this.viewToModel = null;
		this.modelToView = null;
	}

	public TableModel getTableModel()
	{
		return this.tableModel;
	}

	public void setTableModel( final TableModel tableModel )
	{
		if ( this.tableModel != null )
		{
			this.tableModel.removeTableModelListener( this.tableModelListener );
		}

		this.tableModel = tableModel;
		if ( this.tableModel != null )
		{
			this.tableModel.addTableModelListener( this.tableModelListener );
		}

		this.clearSortingState();
		this.fireTableStructureChanged();
	}

	public JTableHeader getTableHeader()
	{
		return this.tableHeader;
	}

	public void setTableHeader( final JTableHeader tableHeader )
	{
		if ( this.tableHeader != null )
		{
			this.tableHeader.removeMouseListener( this.mouseListener );
			TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
			if ( defaultRenderer instanceof SortableHeaderRenderer )
			{
				this.tableHeader.setDefaultRenderer( ( (SortableHeaderRenderer) defaultRenderer ).tableCellRenderer );
			}
		}
		this.tableHeader = tableHeader;
		if ( this.tableHeader != null )
		{
			this.tableHeader.addMouseListener( this.mouseListener );
			this.tableHeader.setDefaultRenderer( new SortableHeaderRenderer( this.tableHeader.getDefaultRenderer() ) );
		}
	}

	public boolean isSorting()
	{
		return this.sortingColumns.size() != 0;
	}

	private Directive getDirective( final int column )
	{
		for ( int i = 0; i < this.sortingColumns.size(); i++ )
		{
			Directive directive = (Directive) this.sortingColumns.get( i );
			if ( directive.column == column )
			{
				return directive;
			}
		}
		return TableSorter.EMPTY_DIRECTIVE;
	}

	public int getSortingStatus( final int column )
	{
		return this.getDirective( column ).direction;
	}

	private void sortingStatusChanged()
	{
		this.clearSortingState();
		this.fireTableDataChanged();
		if ( this.tableHeader != null )
		{
			this.tableHeader.repaint();
		}
	}

	public void setSortingStatus( final int column, final int status )
	{
		Directive directive = this.getDirective( column );
		if ( directive != TableSorter.EMPTY_DIRECTIVE )
		{
			this.sortingColumns.remove( directive );
		}
		if ( status != TableSorter.NOT_SORTED )
		{
			this.sortingColumns.add( new Directive( column, status ) );
		}
		this.sortingStatusChanged();
	}

	protected Icon getHeaderRendererIcon( final int column, final int size )
	{
		Directive directive = this.getDirective( column );
		if ( directive == TableSorter.EMPTY_DIRECTIVE )
		{
			return null;
		}
		return new Arrow( directive.direction == TableSorter.DESCENDING, size, this.sortingColumns.indexOf( directive ) );
	}

	private void cancelSorting()
	{
		this.sortingColumns.clear();
		this.sortingStatusChanged();
	}

	public void setColumnComparator( final Class type, final Comparator comparator )
	{
		if ( comparator == null )
		{
			this.columnComparators.remove( type );
		}
		else
		{
			this.columnComparators.put( type, comparator );
		}
	}

	protected Comparator getComparator( final int column )
	{
		Class columnType = this.tableModel.getColumnClass( column );
		Comparator comparator = (Comparator) this.columnComparators.get( columnType );
		if ( comparator != null )
		{
			return comparator;
		}
		if ( Comparable.class.isAssignableFrom( columnType ) )
		{
			return TableSorter.COMPARABLE_COMAPRATOR;
		}
		return TableSorter.LEXICAL_COMPARATOR;
	}

	private Row[] getViewToModel()
	{
		if ( this.viewToModel == null )
		{
			int tableModelRowCount = this.tableModel.getRowCount();
			this.viewToModel = new Row[ tableModelRowCount ];
			for ( int row = 0; row < tableModelRowCount; row++ )
			{
				this.viewToModel[ row ] = new Row( row );
			}

			if ( this.isSorting() )
			{
				Arrays.sort( this.viewToModel );
			}
		}
		return this.viewToModel;
	}

	public int modelIndex( final int viewIndex )
	{
		return this.getViewToModel()[ viewIndex ].modelIndex;
	}

	private int[] getModelToView()
	{
		if ( this.modelToView == null )
		{
			int n = this.getViewToModel().length;
			this.modelToView = new int[ n ];
			for ( int i = 0; i < n; i++ )
			{
				this.modelToView[ this.modelIndex( i ) ] = i;
			}
		}
		return this.modelToView;
	}

	// TableModel interface methods

	public int getRowCount()
	{
		return this.tableModel == null ? 0 : this.tableModel.getRowCount();
	}

	public int getColumnCount()
	{
		return this.tableModel == null ? 0 : this.tableModel.getColumnCount();
	}

	public String getColumnName( final int column )
	{
		return this.tableModel.getColumnName( column );
	}

	public Class getColumnClass( final int column )
	{
		return this.tableModel.getColumnClass( column );
	}

	public boolean isCellEditable( final int row, final int column )
	{
		return this.tableModel.isCellEditable( this.modelIndex( row ), column );
	}

	public Object getValueAt( final int row, final int column )
	{
		return this.tableModel.getValueAt( this.modelIndex( row ), column );
	}

	public void setValueAt( final Object aValue, final int row, final int column )
	{
		this.tableModel.setValueAt( aValue, this.modelIndex( row ), column );
	}

	// Helper classes

	private class Row
		implements Comparable
	{
		private final int modelIndex;

		public Row( final int index )
		{
			this.modelIndex = index;
		}

		public int compareTo( final Object o )
		{
			int row1 = this.modelIndex;
			int row2 = ( (Row) o ).modelIndex;

			for ( Iterator it = TableSorter.this.sortingColumns.iterator(); it.hasNext(); )
			{
				Directive directive = (Directive) it.next();
				int column = directive.column;
				Object o1 = TableSorter.this.tableModel.getValueAt( row1, column );
				Object o2 = TableSorter.this.tableModel.getValueAt( row2, column );

				int comparison = 0;
				// Define null less than everything, except null.
				if ( o1 == null && o2 == null )
				{
					comparison = 0;
				}
				else if ( o1 == null )
				{
					comparison = -1;
				}
				else if ( o2 == null )
				{
					comparison = 1;
				}
				else
				{
					comparison = TableSorter.this.getComparator( column ).compare( o1, o2 );
				}
				if ( comparison != 0 )
				{
					return directive.direction == TableSorter.DESCENDING ? -comparison : comparison;
				}
			}
			return 0;
		}
	}

	private class TableModelHandler
		implements TableModelListener
	{
		public void tableChanged( final TableModelEvent e )
		{
			// If we're not sorting by anything, just pass the event along.
			if ( !TableSorter.this.isSorting() )
			{
				TableSorter.this.clearSortingState();
				TableSorter.this.fireTableChanged( e );
				return;
			}

			// If the table structure has changed, cancel the sorting; the
			// sorting columns may have been either moved or deleted from
			// the model.
			if ( e.getFirstRow() == TableModelEvent.HEADER_ROW )
			{
				TableSorter.this.cancelSorting();
				TableSorter.this.fireTableChanged( e );
				return;
			}

			// We can map a cell event through to the view without widening
			// when the following conditions apply:
			//
			// a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and,
			// b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
			// c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and,
			// d) a reverse lookup will not trigger a sort (modelToView != null)
			//
			// Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
			//
			// The last check, for (modelToView != null) is to see if modelToView
			// is already allocated. If we don't do this check; sorting can become
			// a performance bottleneck for applications where cells
			// change rapidly in different parts of the table. If cells
			// change alternately in the sorting column and then outside of
			// it this class can end up re-sorting on alternate cell updates -
			// which can be a performance problem for large tables. The last
			// clause avoids this problem.
			int column = e.getColumn();
			if ( e.getFirstRow() == e.getLastRow() && column != TableModelEvent.ALL_COLUMNS && TableSorter.this.getSortingStatus( column ) == TableSorter.NOT_SORTED && TableSorter.this.modelToView != null )
			{
				int viewIndex = TableSorter.this.getModelToView()[ e.getFirstRow() ];
				TableSorter.this.fireTableChanged( new TableModelEvent(
					TableSorter.this, viewIndex, viewIndex, column, e.getType() ) );
				return;
			}

			// Something has happened to the data that may have invalidated the row order.
			TableSorter.this.clearSortingState();
			TableSorter.this.fireTableDataChanged();
			return;
		}
	}

	private class MouseHandler
		extends MouseAdapter
	{
		public void mouseClicked( final MouseEvent e )
		{
			JTableHeader h = (JTableHeader) e.getSource();
			TableColumnModel columnModel = h.getColumnModel();
			int viewColumn = columnModel.getColumnIndexAtX( e.getX() );
			int column = columnModel.getColumn( viewColumn ).getModelIndex();
			if ( column != -1 )
			{
				int status = TableSorter.this.getSortingStatus( column );
				if ( !e.isControlDown() )
				{
					TableSorter.this.cancelSorting();
				}
				// Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
				// {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
				status = status + ( e.isShiftDown() ? -1 : 1 );
				status = ( status + 4 ) % 3 - 1; // signed mod, returning {-1, 0, 1}
				TableSorter.this.setSortingStatus( column, status );
			}
		}
	}

	private static class Arrow
		implements Icon
	{
		private final boolean descending;
		private final int size;
		private final int priority;

		public Arrow( final boolean descending, final int size, final int priority )
		{
			this.descending = descending;
			this.size = size;
			this.priority = priority;
		}

		public void paintIcon( final Component c, final Graphics g, int x, int y )
		{
			Color color = c == null ? Color.GRAY : c.getBackground();
			// In a compound sort, make each succesive triangle 20%
			// smaller than the previous one.
			int dx = (int) ( this.size / 2 * Math.pow( 0.8, this.priority ) );
			int dy = this.descending ? dx : -dx;
			// Align icon (roughly) with font baseline.
			y = y + 5 * this.size / 6 + ( this.descending ? -dy : 0 );
			int shift = this.descending ? 1 : -1;
			g.translate( x, y );

			// Right diagonal.
			g.setColor( color.darker() );
			g.drawLine( dx / 2, dy, 0, 0 );
			g.drawLine( dx / 2, dy + shift, 0, shift );

			// Left diagonal.
			g.setColor( color.brighter() );
			g.drawLine( dx / 2, dy, dx, 0 );
			g.drawLine( dx / 2, dy + shift, dx, shift );

			// Horizontal line.
			if ( this.descending )
			{
				g.setColor( color.darker().darker() );
			}
			else
			{
				g.setColor( color.brighter().brighter() );
			}
			g.drawLine( dx, 0, 0, 0 );

			g.setColor( color );
			g.translate( -x, -y );
		}

		public int getIconWidth()
		{
			return this.size;
		}

		public int getIconHeight()
		{
			return this.size;
		}
	}

	private class SortableHeaderRenderer
		implements TableCellRenderer
	{
		private final TableCellRenderer tableCellRenderer;

		public SortableHeaderRenderer( final TableCellRenderer tableCellRenderer )
		{
			this.tableCellRenderer = tableCellRenderer;
		}

		public Component getTableCellRendererComponent( final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			Component c =
				this.tableCellRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			if ( c instanceof JLabel )
			{
				JLabel l = (JLabel) c;
				l.setHorizontalTextPosition( SwingConstants.LEFT );
				int modelColumn = table.convertColumnIndexToModel( column );
				l.setIcon( TableSorter.this.getHeaderRendererIcon( modelColumn, l.getFont().getSize() ) );
			}
			return c;
		}
	}

	private static class Directive
	{
		private final int column;
		private final int direction;

		public Directive( final int column, final int direction )
		{
			this.column = column;
			this.direction = direction;
		}
	}
}
