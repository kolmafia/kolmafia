/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

// layout
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.SwingConstants;

// event listeners
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;

/**
 * One of the consequences of using a <code>JList</code> is that, in order
 * to allow selectability, many of the components within a rendered cell
 * have no functionality; buttons cannot be clicked, and sublists cannot
 * really exist.  However, two concepts of a JList are very useful: having
 * ordered components which respond to changes in a <code>ListModel</code>,
 * and having these components be displayed in a list-like fashion.  The
 * point of this class is to implement those two features of a <code>JList</code>
 * and ignore the selectable-index feature available to <code>JList</code>s.
 *
 * <br><br>
 *
 * This panel creates a list of which contains data synchronized with a
 * <code>LockableListModel</code>.  Note, however, that this does not
 * in any way keep track of what is added or removed in of itself, and
 * relies on the <code>Container</code> component to implement that part
 * of its functionality, which has the consequence of allowing all add()
 * and remove() functions to be public.  Thus, if any components are added
 * or removed from this list through other classes, the contents are no
 * longer reliable and unanticipated exceptions may be thrown.
 */

public abstract class PanelList extends javax.swing.JPanel implements javax.swing.Scrollable
{
	private int visibleRows;
	int cellHeight, cellWidth;

	/**
	 * Constructs a new <code>PanelList</code> which will respond to changes in
	 * the given <code>LockableListModel</code> by adding or removing
	 * components as needed.
	 *
	 * @param	visibleRows	if this component is inside a <code>JScrollPane</code>,
	 *						this reflects the number of rows that should be visible
	 *						in the viewport; otherwise, this value indicates the
	 *						minimum size of the panel
	 * @param	cellHeight	the height of each individual cell in the <code>PanelList</code>
	 * @param	cellWidth	the width of each individual cell in the <code>PanelList</code>
	 * @param	associatedListModel	the list model associated with this <code>PanelList</code>
	 */

	public PanelList( int visibleRows, int cellWidth, int cellHeight, LockableListModel associatedListModel )
	{
		super();  ((java.awt.FlowLayout)getLayout()).setVgap(0);
		this.visibleRows = visibleRows;  this.cellHeight = cellHeight;  this.cellWidth = cellWidth;

		// check to see if there are any components within the associated list
		// model, and add them to the existing panel list if they do exist;
		// note that if the associated list model is null, this panel becomes
		// fairly meaningless, but a null pointer exception will not be thrown

		if ( associatedListModel != null )
		{
			synchronized ( associatedListModel )
			{
				java.util.Iterator contents = associatedListModel.iterator();

				for ( int i = 0; contents.hasNext(); ++i )
					add( constructPanelListCell( contents.next(), i ), i );

				validatePanelList();
				associatedListModel.addListDataListener( new PanelListListener( this ) );
			}
		}
	}

	/**
	 * Returns a <code>PanelListCell</code> constructed from the given Object, to be
	 * positioned at the given index in the <code>PanelList</code>.
	 *
	 * @param	value	the object which contains the data needed to construct the display
	 * @param	index	this cell's intended index within the <code>PanelList</code>
	 * @return	the constructed panel list cell
	 */

	protected abstract PanelListCell constructPanelListCell( Object value, int index );

	/**
	 * A private function used to validate the panel list.  The function serves
	 * to resize the display, as appropriate.  This allows any function which
	 * is controlling the <code>Scrollable</code> elements of the list to
	 * properly adjust themselves to accomodate the updated panel.
	 */

	private synchronized void validatePanelList()
	{
		// reset the size of the container according to the number
		// of elements currently found in the container

		int displayedRows = getComponentCount() > visibleRows ? getComponentCount() : visibleRows;
		int appropriateHeight = displayedRows * getScrollableUnitIncrement( null, SwingConstants.VERTICAL, 1 );
		JComponentUtilities.setComponentSize( this, cellWidth, appropriateHeight );
		invalidate();  validate();
	}

	/**
	 * Returns the preferred size of the scrollable viewport.  Used by classes such as
	 * the <code>JScrollPane</code> to determine how much of the panel should be visible.
	 */

	public synchronized Dimension getPreferredScrollableViewportSize()
	{	return new Dimension( cellWidth, visibleRows * cellHeight );
	}

	/**
	 * Returns the amount that is needed to display a single cell, given the visible rectangle
	 * and the direction to be scrolled.
	 *
	 * @param	visibleRect	the visible rectangular area seen in the viewport
	 * @param	orientation	the orientation of the scrollbar that was clicked
	 * @param	direction	the direction (with respect to the scrollbar) that is being scrolled;
	 *						positive indicates a right/down, negative indicates up/left
	 * @return	the amount that needs to be scrolled to display the next cell
	 */

	public synchronized int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{	return orientation == SwingConstants.HORIZONTAL ? 0 : cellHeight;
	}

	/**
	 * Returns the amount that is needed to display a single block of cells, given the visible
	 * rectangle and the direction to be scrolled.
	 *
	 * @param	visibleRect	the visible rectangular area seen in the viewport
	 * @param	orientation	the orientation of the scrollbar that was clicked
	 * @param	direction	the direction (with respect to the scrollbar) that is being scrolled;
	 *						positive indicates a right/down, negative indicates up/left
	 * @return	the amount that needs to be scrolled to display the next block of cells
	 */

	public synchronized int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction )
	{	return orientation == SwingConstants.HORIZONTAL ? 0 :
				(visibleRows - 1) * getScrollableUnitIncrement( visibleRect, orientation, direction );
	}

	/**
	 * This function always returns false to indicate that this scrollable does not
	 * force the height of the viewport to match the height of the display.  The height
	 * is instead recomputed each time a component is added/removed.
	 */

	public synchronized boolean getScrollableTracksViewportHeight()
	{	return false;
	}

	/**
	 * This function always returns false to indicate that this scrollable does not
	 * force the width of the viewport to match the width of the display.  The width
	 * is instead recomputed each time a component is added/removed.
	 */

	public synchronized boolean getScrollableTracksViewportWidth()
	{	return false;
	}

	/**
	 * Rather than having the PanelList implement <code>ListDataListener</code>, it
	 * instead defers all listening to this listener class, which then listens on
	 * the <code>LockableListModel</code> for changes in its underlying structure.
	 */

	private class PanelListListener implements javax.swing.event.ListDataListener
	{
		private PanelList associatedPanelList;

		/**
		 * Constructs a new <code>PanelListListener</code> which will apply all
		 * changes in the <code>LockableListModel</code> to the given
		 * <code>PanelList</code>.
		 */

		public PanelListListener( PanelList associatedPanelList )
		{	this.associatedPanelList = associatedPanelList;
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalAdded( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.INTERVAL_ADDED && e.getSource() instanceof LockableListModel )
				(new ListDataEventProcessor( e )).run();
		}

		/**
		 * Indicates that the given list has added elements.  This function then
		 * proceeds to add the panels within the given index range using information
		 * provided in the <code>LockableListModel</code>.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private synchronized void intervalAdded( LockableListModel source, int index0, int index1 )
		{
			if ( index1 >= source.size() )
				return;
			for ( int i = index0; i <= index1; ++i )
				associatedPanelList.add( constructPanelListCell( source.get(i), i ), i );
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalRemoved( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.INTERVAL_REMOVED && e.getSource() instanceof LockableListModel )
				(new ListDataEventProcessor( e )).run();
		}

		/**
		 * Indicates that the given list has removed elements.  This function then
		 * proceeds to remove the panels within the given index range.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private synchronized void intervalRemoved( LockableListModel source, int index0, int index1 )
		{
			if ( index1 >= getComponentCount() )
				return;
			for ( int i = index1; i >= index0; --i )
				associatedPanelList.remove(i);
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void contentsChanged( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.CONTENTS_CHANGED && e.getSource() instanceof LockableListModel )
				(new ListDataEventProcessor( e )).run();
		}

		/**
		 * Indicates that the given list has changed its contents.  This function then
		 * proceeds to update the panels within the given index range with the information
		 * in the <code>LockableListModel</code>.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private synchronized void contentsChanged( LockableListModel source, int index0, int index1 )
		{
			if ( index1 >= getComponentCount() )
				return;
			for ( int i = index1; i >= index0; --i )
				((PanelListCell)associatedPanelList.getComponent(i)).updateDisplay( associatedPanelList, source.get(i), i );
		}

		/**
		 * An internal class used to process <code>ListDataEvent</code> objects that occur.
		 * Because all responses result in something occurring in changing of the GUI, this
		 * <code>Runnable</code> will always force the actual function calls to occur in the
		 * event dispatch thread.
		 */

		private class ListDataEventProcessor implements Runnable
		{
			private int associatedEventType;
			private LockableListModel source;
			private int index0, index1;

			public ListDataEventProcessor( ListDataEvent e )
			{
				associatedEventType = e.getType();
				source = (LockableListModel) e.getSource();
				index0 = e.getIndex0();  index1 = e.getIndex1();
			}

			public void run()
			{
				if ( source == null || index1 < 0 )
					return;

				if ( !SwingUtilities.isEventDispatchThread() )
				{
					// allow the thread to invoke later in the AWT
					// thread, to ensure nothing bad happens
					SwingUtilities.invokeLater( this );
					return;
				}

				switch ( associatedEventType )
				{
					case ListDataEvent.INTERVAL_ADDED:
						intervalAdded( source, index0, index1 );
						break;
					case ListDataEvent.INTERVAL_REMOVED:
						intervalRemoved( source, index0, index1 );
						break;
					case ListDataEvent.CONTENTS_CHANGED:
						contentsChanged( source, index0, index1 );
						break;
				}

				// after the event occurs, the panel should be
				// validated; this also occurs in the AWT thread

				validatePanelList();
			}
		}
	}
}