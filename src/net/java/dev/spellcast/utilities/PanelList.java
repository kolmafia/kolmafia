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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

// Containers
import javax.swing.JPanel;
import javax.swing.Scrollable;

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
 * This panel creates a list of which contains data mirroring a given
 * <code>LockableListModel</code>.  Note, however, that this does not
 * in any way keep track of what is added or removed in of itself, and
 * relies on the <code>Container</code> component to implement that part
 * of its functionality, which has the consequence of allowing all add()
 * and remove() functions to be public.  Thus, if any components are added
 * or removed from this list through other classes, the contents are no
 * longer reliable and unanticipated exceptions may be thrown.
 */

public abstract class PanelList extends JPanel
{
	private JPanel listPanel;
	private int visibleRows;
	private int cellHeight, cellWidth;

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
	{	this( visibleRows, cellWidth, cellHeight, associatedListModel, false );
	}

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

	public PanelList( int visibleRows, int cellWidth, int cellHeight, LockableListModel associatedListModel, boolean useBoxLayout )
	{
		super( new BorderLayout() );

		this.add( listPanel = new JPanel(), isResizeableList() ? BorderLayout.CENTER : BorderLayout.WEST );
		listPanel.setLayout( useBoxLayout ? (LayoutManager) new BoxLayout( listPanel, BoxLayout.Y_AXIS ) : (LayoutManager) new FlowLayout() );

		this.visibleRows = visibleRows;  this.cellHeight = cellHeight;  this.cellWidth = cellWidth;

		// check to see if there are any components within the associated list
		// model, and add them to the existing panel list if they do exist;
		// note that if the associated list model is null, this panel becomes
		// fairly meaningless, but a null pointer exception will not be thrown

		if ( associatedListModel != null )
		{
			Object [] contents = associatedListModel.toArray();

			for ( int i = 0; i < contents.length; ++i )
				listPanel.add( (Component) constructPanelListCell( contents[i], i ), i );

			validatePanelList();
			associatedListModel.addListDataListener( new PanelListListener() );
		}
	}

	protected boolean isResizeableList()
	{	return false;
	}

	/**
	 * Overridden so that individual components may be enabled and disabled.
	 * This is different from the behavior of a normal JPanel, because the
	 * children of a <code>PanelList</code> should be enabled and disabled
	 * with the parent list.
	 *
	 * @param	isEnabled	<code>true</code> if the list should be enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		int componentCount = listPanel.getComponentCount();
		for ( int i = 0; i < componentCount; ++i )
			listPanel.getComponent( i ).setEnabled( isEnabled );
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

	private void validatePanelList()
	{	listPanel.invalidate();  listPanel.validate();  listPanel.repaint();
	}

	public Component [] getComponents()
	{	return listPanel.getComponents();
	}

	/**
	 * Rather than having the PanelList implement <code>ListDataListener</code>, it
	 * instead defers all listening to this listener class, which then listens on
	 * the <code>LockableListModel</code> for changes in its underlying structure.
	 */

	private class PanelListListener implements javax.swing.event.ListDataListener
	{
		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalAdded( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= source.size() || source.size() == listPanel.getComponentCount() )
				return;

			for ( int i = index0; i <= index1; ++i )
				listPanel.add( (Component) constructPanelListCell( source.get(i), i ), i );

			validatePanelList();
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalRemoved( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= getComponentCount() || source.size() == listPanel.getComponentCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				listPanel.remove(i);

			validatePanelList();
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void contentsChanged( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= getComponentCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				((PanelListCell)listPanel.getComponent(i)).updateDisplay( PanelList.this, source.get(i), i );

			validatePanelList();
		}
	}
}