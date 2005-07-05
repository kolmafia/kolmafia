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
import java.io.IOException;

/**
 * A single node in the <code>DiskAccessBTree</code>.  This class is responsible
 * for storing all of the information related to the node.  In order to stay true
 * to the ideal of minimizing the amount of memory that is used by the overall
 * data structure, all functions read and write directly to the file and only a
 * minimal amount of information is actually stored in memory.  This does mean
 * that this tree would be slower than an in-memory database, but it is believed
 * that the larger size of files managed by the <code>DiskAccessBTree</code> more
 * than outweighs this incurred cost.
 */

public class DiskAccessBTreeNode extends AbstractBTreeNode
{
	// constants used by the index of function
	private long offset, parentOffset;
	private long keysBaseOffset, elementsBaseOffset, childrenBaseOffset;
	private RandomObjectAccessFile dirfile, keyfile, datfile;

	/**
	 * Constructs a new <code>DiskAccessBTreeNode</code> from disk; the parameter
	 * provided indicates where in the file the <code>DiskAccessBTreeNode</code>
	 * starts.  Note that this function can only be used for nodes that have already
	 * been stored on disk.
	 *
	 * @param	dirfile	The file containing the <code>DiskAccessBTreeNode</code>
	 * @param	datfile	The file containing the data for the <code>DiskAccessBTreeNode</code>
	 * @param	offset	The offset into the file at which the node can be found
	 */

	public DiskAccessBTreeNode( RandomObjectAccessFile dirfile, RandomObjectAccessFile keyfile,
		RandomObjectAccessFile datfile, long offset )
	{
		this.offset = offset;
		this.dirfile = dirfile;
		this.keyfile = keyfile;
		this.datfile = datfile;
		this.refresh();
	}

	/**
	 * Constructs a new <code>DiskAccessBTreeNode</code> in memory and subsequently
	 * stores it immediately to the end of the indicated file; the parameters provided
	 * indicate all of the information relevant to the <code>DiskAccessBTreeNode</code>.
	 *
	 * @param	dirfile	The file containing the <code>DiskAccessBTreeNode</code>
	 * @param	datfile	The file containing the data for the <code>DiskAccessBTreeNode</code>
	 * @param	parentOffset	The offset of the parent node for this node
	 * @param	index	The child index for this node (relative to its parent node)
	 * @param	capacity	The maximum number of keys that this node will hold
	 */

	public DiskAccessBTreeNode( RandomObjectAccessFile dirfile, RandomObjectAccessFile keyfile,
		RandomObjectAccessFile datfile, long parentOffset, int index, int capacity )
	{
		try
		{
			this.offset = dirfile.length();
			this.parentOffset = parentOffset;
			initialize( index, 0, capacity, 0 );

			// now write the header information for the node to disk
			// so that all the operations can be performed from there

			this.dirfile = dirfile;
			this.keyfile = keyfile;
			this.datfile = datfile;
			writeNodeHeader();

			// now, fill in fake content so that all of the accesses
			// can be successfully achieved, and in addition to that,
			// future creations of nodes won't result in faulty data

			keysBaseOffset = dirfile.getFilePointer();

			for ( int i = 0; i < this.capacity; ++i )
				dirfile.writeLong(0);

			elementsBaseOffset = dirfile.getFilePointer();
			for ( int i = 0; i < this.capacity; ++i )
				dirfile.writeLong(0);

			childrenBaseOffset = dirfile.getFilePointer();
			for ( int i = 0; i <= this.capacity; ++i )
				dirfile.writeLong(0);
		}
		catch ( IOException e )
		{	throw new RuntimeException( "Failed to initialize the DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method which clears the node of all of its references.
	 * Because all of the elements of the node are determined by its header
	 * information, this merely involves resetting that header information.
	 */

	protected void clear()
	{
		nodeSize = 0;
		childCount = 0;
		writeNodeHeader();
	}

	/**
	 * A public method which refreshes the node with all the correct
	 * information.  This is useful for when there is a central point
	 * where node information is considered "correct" and the node can
	 * refresh its information based on that data.
	 */

	public void refresh()
	{
		try
		{
			// read in all the header information that is not
			// actually a part of the content of the node

			dirfile.seek( offset );
			parentOffset = dirfile.readLong();

			initialize( dirfile.readInt(), dirfile.readInt(), dirfile.readInt(), dirfile.readInt() );

			keysBaseOffset     = dirfile.getFilePointer();
			elementsBaseOffset = keysBaseOffset + capacity * 8;
			childrenBaseOffset = elementsBaseOffset + capacity * 8;
		}
		catch ( IOException e )
		{	throw new RuntimeException( "Failed to initialize the DiskAccessBTreeNode", e );
		}
	}

	/**
	 * A method for retrieving the parent for this node.  Note that if
	 * if this node has no parent (ie: the offset specified is -1), this
	 * method returns null.  Thus, <code>NullPointerException</code>s
	 * may arise from its use, and one should check to make sure that the
	 * return value is non-null before using it
	 *
	 * @return	The parent of this node, if one exists; <tt>null</tt> if
	 *			this node has no parent
	 */

	public AbstractBTreeNode getParent()
	{
		if ( parentOffset == -1 )
			return null;
		return new DiskAccessBTreeNode( dirfile, keyfile, datfile, parentOffset );
	}

	/**
	 * A method for retrieving a reference to the key at the given index.
	 * This is to allow for the possibility that the reference to the key
	 * can be retrieved much faster than the key itself (which is true in
	 * some cases, untrue in others), and is used in such things as the
	 * store and add.
	 *
	 * @param	index	The index of the key to be retrieved
	 * @return	The key in this node associated with the given index
	 */

	protected Object getKeyAsReference( int index )
	{
		try
		{
			dirfile.seek( keysBaseOffset + index * 8 );
			return new Long( dirfile.readLong() );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to read the reference to key at index " + index +
				" from this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method for translating a reference to a key.  By default,
	 * this method returns  a type-cast of the original object.  In cases
	 * where a real conversion is necessary, this would allow translation
	 * between the reference and the original key.
	 */

	protected Comparable referenceToKey( Object reference )
	{
		long keyOffset = ((Long)reference).longValue();

		try
		{	return (Comparable) keyfile.readObject( keyOffset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to translate the key at offset " + keyOffset +
				" from this DiskAccessBTreeNode", e );
		}
		catch ( ClassNotFoundException e )
		{
			throw new InternalError( "The key associated with the reference " + reference +
				" of this DiskAccessBTreeNode is of a class not defined in this runtime environment" );
		}
	}

	/**
	 * An internal method for translating a key to a reference, in the
	 * event that there is no reference available.
	 *
	 * @param	key	The key to create a reference for
	 * @return	The reference created to be associated with the given key
	 */

	protected Object createKeyReference( Comparable key )
	{
		try
		{
			long keyOffset = keyfile.length();
			keyfile.appendObject( key );
			return new Long( keyOffset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to translate the key " + key +
				" into a reference usable by this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method for retrieving the reference for a given element.
	 * This is used primarily when nodes are being added or removed, since
	 * this retrieval is sometimes faster than retrieving the actual element
	 * itself.  Also, the <code>store()</code> method requires an element
	 * reference, not the element itself.
	 *
	 * @param	index	The index of the element offset to be retrieved
	 * @return	The element offset associated with the given index
	 */

	protected Object getElementAsReference( int index )
	{
		try
		{
			dirfile.seek( elementsBaseOffset + index * 8 );
			return new Long ( dirfile.readLong() );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to read the element offset at index " + index +
				" of this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method for translating a reference to an element.  By
	 * default, this method returns  a type-cast of the original object.
	 * In cases where a real conversion is necessary, this would allow
	 * translation between the reference and the original key.
	 */

	protected Object referenceToElement( Object reference )
	{
		long elementOffset = ((Long)reference).longValue();

		try
		{	return datfile.readObject( elementOffset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to translate the object at offset " + elementOffset +
				" from this DiskAccessBTreeNode", e );
		}
		catch ( ClassNotFoundException e )
		{
			throw new InternalError( "The element associated with the reference " + reference +
				" of this DiskAccessBTreeNode is of a class not defined in this runtime environment" );
		}
	}

	/**
	 * An internal method for translating an element to a reference, in the
	 * event that there is no reference available.
	 *
	 * @param	element	The element to create a reference for
	 * @return	The reference created to be associated with the given key
	 */

	protected Object createElementReference( Object element )
	{
		try
		{
			long elementOffset = datfile.length();
			datfile.appendObject( element );
			return new Long( elementOffset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to translate the element " + element +
				" into a reference usable by this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method which overwrites the key and element offsets
	 * currently at the given index with the provided values.  This is
	 * used primarily when adding and removing items and keys from the
	 * <code>DiskAccessBTreeNode</code>.  Note that the parameter that
	 * is provided is a <i>reference</i> to the key to be stored, not
	 * the actual key itself.
	 *
	 * @param	index	The index at which the values are to be stored
	 * @param	keyReference	The reference to the key to be stored into the tree
	 * @param	elementReference	The element offset to be associated with the given key
	 */

	protected void store( int index, Object keyReference, Object elementReference )
	{
		try
		{
			dirfile.seek( keysBaseOffset + index * 8 );
			dirfile.writeLong( ((Long)keyReference).longValue() );

			dirfile.seek( elementsBaseOffset + index * 8 );
			dirfile.writeLong( ((Long)elementReference).longValue() );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to store the key-element pair into index " + index +
				" of this DiskAccessBTreeNode", e );
		}
	}

	protected void add( int index, Object keyReference, Object elementReference )
	{
		super.add( index, keyReference, elementReference );
		writeNodeHeader();
	}

	/**
	 * An internal method which deletes the key-element pair at the given
	 * index from the node.  This method calls the superclass version of
	 * the method, and then rewrites the header.
	 *
	 * @param	index	The index of the key to be deleted
	 */

	protected void delete( int index )
	{
		super.delete( index );
		writeNodeHeader();
	}

	/**
	 * An internal method for constructing a new child node with the
	 * given index.  This method is used whenever one of the internal
	 * methods needs to construct a new node; thus, the creation of
	 * new nodes can become implementation-specific, without having
	 * to modify the original source code.
	 *
	 * @param	index	The index to be associated with the child
	 */

	protected AbstractBTreeNode createChild( int index )
	{	return new DiskAccessBTreeNode( dirfile, keyfile, datfile, offset, index, capacity );
	}

	/**
	 * A method for retrieving the child reference stored at the given index.
	 * The index of a child indicates that all of its keys are less than
	 * the key with the same index in this node, or greater than all keys
	 * in this node if the index is larger than the number of keys.
	 *
	 * @param	index	The index of the element offset to be retrieved
	 * @return	The child node associated with the given index
	 */

	public AbstractBTreeNode getChild( int index )
	{
		try
		{
			dirfile.seek( childrenBaseOffset + index * 8 );
			long childOffset = dirfile.readLong();
			return childOffset == 0 ? null : new DiskAccessBTreeNode( dirfile, keyfile, datfile, childOffset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to read the child offset at index " + index +
				" of this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method for storing a reference to the child node.  The
	 * index of a child indicates that all of its keys are less than the
	 * key with the same index in this node, or greater than all keys in
	 * this node if the index is larger than the number of keys.  Thus,
	 * one should be sure to make sure that this is the case when storing
	 * child offsets, as this method will not make that assurance.
	 *
	 * @param	index	The index of the child offset to be stored
	 * @param	child	The node reference to be stored
	 */

	protected void storeChild( int index, AbstractBTreeNode child )
	{
		try
		{
			child.index = index;
			((DiskAccessBTreeNode)child).parentOffset = offset;
			((DiskAccessBTreeNode)child).writeNodeHeader();

			dirfile.seek( childrenBaseOffset + index * 8 );
			dirfile.writeLong( ((DiskAccessBTreeNode)child).offset );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( "Failed to store the child offset into index " + index +
				" of this DiskAccessBTreeNode", e );
		}
	}

	/**
	 * An internal method for adding a reference to the child node.
	 * This method calls the superclass version of the method, and
	 * then rewrites the header.
	 *
	 * @param	index	The index at which the child reference is to be added
	 */

	protected void addChild( int index, AbstractBTreeNode child )
	{
		super.addChild( index, child );
		writeNodeHeader();
	}

	/**
	 * An internal method which deletes the child reference at the
	 * given index from the node.  This method calls the superclass
	 * version of the method, and then rewrites the header.
	 *
	 * @param	index	The index of the key to be deleted
	 */

	protected void deleteChild( int index )
	{
		super.deleteChild( index );
		writeNodeHeader();
	}

	/**
	 * An internal method which rewrites the node header.  The header
	 * is rewritten every time something changes inside of the node,
	 * since the header contains both the offsets and the number of
	 * elements currently within the node.
	 */

	private void writeNodeHeader()
	{
		try
		{
			// the first part of the header consists of the identifier
			// information for this node, which includes its offset into
			// the file, the location of its parent, and the child index
			// of itself relative to its parent node

			dirfile.seek( offset );
			dirfile.writeLong( parentOffset );
			dirfile.writeInt( index );

			// the second part of the header contains size information
			// which is used for restoring the individual pieces of the
			// node whenever the node is queried

			dirfile.writeInt( nodeSize );
			dirfile.writeInt( capacity );
			dirfile.writeInt( childCount );
		}
		catch ( IOException e )
		{	throw new RuntimeException( "Failed to write the node header for this DiskAccessBTreeNode", e );
		}
	}
}