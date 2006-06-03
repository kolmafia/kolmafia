/**
 * http://www.informit.com/guides/content.asp?g=java&seqNum=58
 */

package com.informit.guides;

import javax.swing.JList;

import java.io.*;
import java.util.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import net.java.dev.spellcast.utilities.LockableListModel;

public class JDnDList extends JList
	implements DragSourceListener, DragGestureListener, DropTargetListener
{
	private int overIndex;
	private boolean dragging;
	private int [] selectedIndices;

	private DragSource dragSource;
	private DropTarget dropTarget;

	public JDnDList( LockableListModel model )
	{
		super( model );

		// Configure ourselves to be a drag source
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

		// Configure ourselves to be a drop target
		dropTarget = new DropTarget( this, this );
	}

	public void dragGestureRecognized(DragGestureEvent dge)
	{
		this.selectedIndices = this.getSelectedIndices();
		Object[] selectedObjects = this.getSelectedValues();
		if ( selectedObjects.length > 0 )
		{
			StringBuffer sb = new StringBuffer();
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
	{
		this.overIndex = -1;
	}

	public void dragEnter(DragSourceDragEvent dsde)
	{
		this.overIndex = this.locationToIndex( dsde.getLocation() );
		this.setSelectedIndex( this.overIndex );
	}

	public void dragEnter(DropTargetDragEvent dtde)
	{
		this.overIndex = this.locationToIndex( dtde.getLocation() );
		this.setSelectedIndex( this.overIndex );
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

				model.addAll( newIndex, items );

				// Update the selected indicies
				int[] newIndicies = new int[ items.size() ];
				for( int i=0; i < items.size(); i++ )
					 newIndicies[ i ] = newIndex + i;

				this.setSelectedIndices( newIndicies );

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