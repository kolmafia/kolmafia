/**
 * http://www.informit.com/guides/content.asp?g=java&seqNum=58
 */

package com.informit.guides;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JList;

import net.java.dev.spellcast.utilities.LockableListModel;

public class JDnDList extends JList
	implements DragSourceListener, DragGestureListener, DropTargetListener
{
	private boolean acceptsDrops;
	private int overIndex;
	private DragSource dragSource;

	public JDnDList( LockableListModel model )
	{	this( model, true );
	}

	public JDnDList( LockableListModel model, boolean acceptsDrops )
	{
		super( model );

		this.acceptsDrops = acceptsDrops;
		setPrototypeCellValue( "1234567890" );

		addKeyListener( new DeleteListener() );

		// Configure ourselves to be a drag source
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

		// Configure ourselves to be a drop target
		new DropTarget( this, this );
	}

	private class DeleteListener extends KeyAdapter
	{
		public void keyPressed( KeyEvent e )
		{
			if ( e.getKeyCode() != KeyEvent.VK_DELETE )
				return;

			LockableListModel model = (LockableListModel) JDnDList.this.getModel();
			Object[] selectedObjects = getSelectedValues();
			for ( int i = 0; i < selectedObjects.length; ++i )
				model.remove( selectedObjects[i] );
		}
	}

	public void dragGestureRecognized(DragGestureEvent dge)
	{
		Object[] selectedObjects = this.getSelectedValues();
		if ( selectedObjects.length == 0 )
			return;

		StringBuffer sb = new StringBuffer();

		for( int i=0; i<selectedObjects.length; i++ )
			sb.append( selectedObjects[ i ].toString() + "\n" );

		// Build a StringSelection object that the Drag Source
		// can use to transport a string to the Drop Target
		StringSelection text = new StringSelection( sb.toString() );

		// Start dragging the object
		dragSource.startDrag( dge, DragSource.DefaultMoveDrop, text, this );
	}

	public void dragDropEnd(DragSourceDropEvent dsde)
	{
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
		if ( overIndex != -1 )
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
			if( !transferable.isDataFlavorSupported( DataFlavor.stringFlavor ) )
			{
				dtde.rejectDrop();
				return;
			}

			dtde.acceptDrop( DnDConstants.ACTION_MOVE );

			// Find out where the item was dropped
			int newIndex = Math.max( 0, this.locationToIndex( dtde.getLocation() ) );

			// Get the items out of the transferable object and build an
			// array out of them...

			String s = (String) transferable.getTransferData( DataFlavor.stringFlavor );
			String [] items = s.split( "\n" );

			LockableListModel destination = (LockableListModel) this.getModel();

			// If we are dragging onto ourself, move the items,
			// otherwise just add them...

			for ( int i = 0; i < items.length; ++i )
				destination.remove( items[i] );

			for ( int i = 0; i < items.length; ++i )
				destination.add( newIndex + i, items[i] );

			// Reset the over index
			this.overIndex = -1;
			dtde.getDropTargetContext().dropComplete( true );
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
