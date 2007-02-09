/**
 * http://www.informit.com/guides/content.asp?g=java&seqNum=58
 */

package com.informit.guides;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DnDConstants;

import java.io.IOException;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.ThreadedMenuItem;

public class JDnDList extends JList
	implements DragSourceListener, DragGestureListener, DropTargetListener
{
	private boolean acceptsDrops;
	private int overIndex;
	private boolean dragging;
	private int [] selectedIndices;

	private JPopupMenu contextMenu;
	private DragSource dragSource;
	private DropTarget dropTarget;

	public JDnDList( LockableListModel model )
	{	this( model, true );
	}

	public JDnDList( LockableListModel model, boolean acceptsDrops )
	{
		super( model );

		this.acceptsDrops = acceptsDrops;
		setPrototypeCellValue( "1234567890" );

		contextMenu = new JPopupMenu();
		contextMenu.add( new DeleteSelectedMenuItem() );

		addMouseListener( new PopupListener() );

		// Configure ourselves to be a drag source
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

		// Configure ourselves to be a drop target
		dropTarget = new DropTarget( this, this );
	}

	/**
	 * Shows and hides the applicable context menu item.  Actually
	 * all it does is show it -- the VM will handle hiding it.
	 */

	public class PopupListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{	maybeShowPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{	maybeShowPopup( e );
		}

		private void maybeShowPopup( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				int index = locationToIndex( e.getPoint() );

				if ( !isSelectedIndex( index ) )
				{
					clearSelection();
					addSelectionInterval( index, index );
				}

				contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
		}
    }

	private class DeleteSelectedMenuItem extends ThreadedMenuItem
	{
		public DeleteSelectedMenuItem()
		{	super( "remove from list" );
		}

		public void run()
		{
			LockableListModel model = (LockableListModel) JDnDList.this.getModel();
			Object[] selectedObjects = getSelectedValues();
			for ( int i = 0; i < selectedObjects.length; ++i )
				model.remove( selectedObjects[i] );
		}
	}

	public void dragGestureRecognized(DragGestureEvent dge)
	{
		this.selectedIndices = this.getSelectedIndices();
		Object[] selectedObjects = this.getSelectedValues();
		if ( selectedObjects.length > 0 )
		{
			StringBuffer sb = new StringBuffer();
			LockableListModel model = (LockableListModel) getModel();

			for( int i=0; i<selectedObjects.length; i++ )
				sb.append( selectedObjects[ i ].toString() + "\n" );

			// Build a StringSelection object that the Drag Source
			// can use to transport a string to the Drop Target
			StringSelection text = new StringSelection( sb.toString() );

			// Start dragging the object
			this.dragging = true;
			dragSource.startDrag( dge, DragSource.DefaultMoveDrop, text, this );
		}
	}

	public void dragDropEnd(DragSourceDropEvent dsde)
	{	this.dragging = false;
	}

	public void dragExit(DragSourceEvent dte)
	{	this.overIndex = -1;
	}

	public void dragExit(DropTargetEvent dte)
	{	this.overIndex = -1;
	}

	public void dragEnter(DragSourceDragEvent dsde)
	{	this.overIndex = this.locationToIndex( dsde.getLocation() );
	}

	public void dragEnter(DropTargetDragEvent dtde)
	{	this.overIndex = this.locationToIndex( dtde.getLocation() );
	}

	public void dragOver(DragSourceDragEvent dsde)
	{
		// See who we are over...
		int overIndex = this.locationToIndex( dsde.getLocation() );
		if ( overIndex != -1 && overIndex != this.overIndex )
		{
			// If the value has changed from what we were previously over
			// then change the selected object to the one we are over; this
			// is a visual representation that this is where the drop will occur
			this.overIndex = overIndex;
		}
	}

	public void dragOver(DropTargetDragEvent dtde)
	{
		// See who we are over...
		int overIndex = this.locationToIndex( dtde.getLocation() );
		if ( overIndex != -1 && overIndex != this.overIndex )
		{
			// If the value has changed from what we were previously over
			// then change the selected object to the one we are over; this
			// is a visual representation that this is where the drop will occur
			this.overIndex = overIndex;
		}
	}

	public void drop(DropTargetDropEvent dtde)
	{
		if ( !acceptsDrops )
			return;

		try
		{
			Transferable transferable = dtde.getTransferable();
			if( transferable.isDataFlavorSupported( DataFlavor.stringFlavor ) )
			{
				dtde.acceptDrop( DnDConstants.ACTION_MOVE );

				// Find out where the item was dropped
				int newIndex = this.locationToIndex( dtde.getLocation() );

				// Get the items out of the transferable object and build an
				// array out of them...
				String s = (String) transferable.getTransferData( DataFlavor.stringFlavor );
				StringTokenizer st = new StringTokenizer( s, "\n" );
				ArrayList items = new ArrayList();

				while ( st.hasMoreTokens() )
					items.add( st.nextToken() );

				LockableListModel model = (LockableListModel) this.getModel();

				// If we are dragging from our this to our list them move the items,
				// otherwise just add them...
				if ( this.dragging )
				{
					for ( int i = 0; i < this.selectedIndices.length; ++i )
						model.remove( this.selectedIndices[i] );
				}

				model.removeAll( items );
				model.addAll( newIndex, items );

				// Reset the over index
				this.overIndex = -1;
				dtde.getDropTargetContext().dropComplete( true );
			}
			else
			{
				dtde.rejectDrop();
			}
		}
		catch( IOException exception )
		{
			exception.printStackTrace();
			System.err.println( "Exception" + exception.getMessage());
			dtde.rejectDrop();
		}
		catch( UnsupportedFlavorException ufException )
		{
			ufException.printStackTrace();
			System.err.println( "Exception" + ufException.getMessage());
			dtde.rejectDrop();
		}
	}

	public void dropActionChanged( DragSourceDragEvent e )
	{
	}

	public void dropActionChanged( DropTargetDragEvent e )
	{
	}
}
