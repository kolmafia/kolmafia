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
import java.util.Iterator;

/**
 * A single node in the <code>BTree</code>.  This class is responsible
 * for storing all of the information related to the node.  Note that
 * there are several storage functions which are declared abstract; it
 * is the responsibility of the implementing classes to determine how
 * such functions should be implemented.
 */

public abstract class AbstractBTreeNode
{
	// constants used by the index of function
	private static final int MATCH_INDEX = 0;
	private static final int INSERTION   = -1;
	protected int index, nodeSize, capacity, childCount;

	/**
	 * Used to set the the member variables within the <code>AbstractBTreeNode</code>.
	 * Note that this is only necessary during initialization, to ensure that the values
	 * which are retrieved are valid; note that the variables themselves are protected,
	 * so the values can be modified indirectly as well, though without certainty as to
	 * what the behavior of the <code>AbstractBTreeNode</code> will subsequently be.
	 *
	 * @param	index	The child index for this node (relative to its parent node)
	 * @param	nodeSize	The number of nodes currently contained by this node
	 * @param	capacity	The maximum number of keys that this node will hold
	 * @param	childCount	The number of children currently contained by this node
	 */

	protected void initialize( int index, int nodeSize, int capacity, int childCount )
	{
		if ( index < 0 || index > capacity )
			throw new IllegalArgumentException( "Invalid index " + index + " for the given node" );
		if ( capacity % 2 != 0 )
			throw new IllegalArgumentException( "Node size for the AbstractBTreeNode (" + capacity + ") is not even" );
		if ( capacity < 4 )
			throw new IllegalArgumentException( "Node size for the AbstractBTreeNode (" + capacity + ") is less than 4" );

		this.index = index;
		this.nodeSize = nodeSize;
		this.capacity = capacity;
		this.childCount = childCount;
	}

	/**
	 * Inserts the given key into the node, or puts it into one of its
	 * children, should this not be a leaf.  Note that if the node has
	 * reached capacity, the node splits and the structure of the tree
	 * will begin to shift around in order to accommodate the creation
	 * of the new node.
	 *
	 * @param	key	The key to be inserted into the node
	 * @param	element	The element to be associated with this key
	 * @return	The element previously associated with this key if one exists;
	 * 			<tt>null</tt> if this is a brand-new key to the B-Tree
	 */

	public final Object put( Comparable key, Object element )
	{
		// check to see if you have to even check this node at all
		// by checking the boundary keys

		if ( nodeSize == 0 )
		{
			add( 0, createKeyReference(key), createElementReference( element ) );
			return null;
		}

		int compareValue = key.compareTo( getKey(0) );
		if ( compareValue <= 0 )
			return put( 0, compareValue, key, element );

		if ( nodeSize > 1 )
			compareValue = key.compareTo( getKey( nodeSize - 1 ) );
		if ( compareValue > 0 )
			return put( nodeSize, compareValue, key, element );
		if ( compareValue == 0 )
			return put( nodeSize - 1, compareValue, key, element );

		// if it's neither of those two locations, then locate the
		// correct position using binary search

		int index = indexOf( 1, nodeSize - 2, key, INSERTION );
		return put( index, key.compareTo( getKey(index) ), key, element );
	}

	/**
	 * A helper function which actually attempts the putting.  This function
	 * is called whenever the correct index has been located and the key and
	 * corresponding element offset are to be insert into the B-Tree.
	 *
	 * @param	index	The index at which to put the key
	 * @param	compareValue	The result of the key-index compare
	 * @param	key	The key to insert into the B-Tree node
	 * @param	element	The element associated with the given key
	 * @return	The element previously associated with this key if one exists;
	 * 			<tt>null</tt> if this is a brand-new key to the B-Tree
	 */

	private Object put( int index, int compareValue, Comparable key, Object element )
	{
		// if the key is equal, then the current offset will
		// overwrite the existing offset, and the old offset
		// will be returned (as it is in a standard map)

		if ( compareValue == 0 )
		{
			Object oldElement = getElement( index );
			store( index, getKeyAsReference( index ), createElementReference( element ) );
			return oldElement;
		}

		// if the key is less than, then you've found the correct
		// insertion point for the key - insert it into this node
		// or the corresponding child

		if ( childCount != 0 )
			return getChild( index ).put( key, element );

		Object keyReference = createKeyReference( key );

		// in the event that a node split is necessary before adding
		// the node into the tree, split the node and recall the
		// add function on the appropriate child

		if ( nodeSize == capacity )
		{
			splitNode();
			return getChild( index <= (capacity >> 1) ? 0 : 1 ).put( key, element );
		}

		// otherwise, simply add the node into the correct
		// position in the tree

		add( index, keyReference, createElementReference( element ) );
		return null;
	}

	/**
	 * An internal method which splits the node in half.  Note that
	 * when a node split occurs, if the current node is not the root,
	 * the split will result in the center key being propogated up
	 * the B-Tree, which potentially causes many of the nodes to be
	 * restructured.  Thus, this is one of the more expensive operations
	 * and the B-Tree should be designed to use this operation as few
	 * times as possible.
	 */

	private void splitNode()
	{
		// first, check to see if the node-split condition occurred;
		// if no node-split is needed, return from the function

		if ( nodeSize != capacity )
			return;

		AbstractBTreeNode left = createChild( 0 );
		AbstractBTreeNode right = createChild( 1 );
		int splitPoint = capacity >> 1;

		// split the node - the node will be divided in half, with
		// capacity / 2 nodes in the left node, and capacity / 2
		// nodes in the right node; this node will change to hold
		// only the key at the split point

		AbstractBTreeNode child;
		for ( int i = 0; i < splitPoint; ++i )
		{
			left.add( i, getKeyAsReference(i), getElementAsReference(i) );
			if ( childCount > 0 )
				left.addChild( i, getChild(i) );
		}

		if ( childCount > 0 )
			left.addChild( splitPoint, getChild( splitPoint ) );

		// the right node is updated in much the same way that the
		// left node is updated; however, the difference between
		// the two is that the starting position for the child nodes
		// is different, and thus they actually have to have their
		// index references shifted

		for ( int i = 0, j = splitPoint + 1; j < capacity; ++i, ++j )
		{
			right.add( i, getKeyAsReference(j), getElementAsReference(j) );
			if ( childCount > 0 )
				right.addChild( i, getChild(j) );
		}

		if ( childCount > 0 )
			right.addChild( splitPoint, getChild( capacity ) );

		Object newKeyReference = getKeyAsReference( splitPoint );
		Object newElementReference = getElementAsReference( splitPoint );
		AbstractBTreeNode parent = getParent();

		// now, if the parent does not exist (this is the root), then
		// there is nothing left to do - you can return from the call

		if ( parent == null )
		{
			// shift the element at the split point to the first index
			// and make left and right the children of the new node

			clear();  add( 0, newKeyReference, newElementReference );
			addChild( 0, left );  addChild( 1, right );
			return;
		}

		// if this is not the root node, then this node is essentially
		// dead and cannot be reused - however, this means that you
		// need to move the references to the two newly created nodes
		// to the parent node, and move the key in this node up also

		parent.storeChild( index, left );
		parent.addChild( index + 1, right );
		parent.add( index, newKeyReference, newElementReference );
		parent.splitNode();
	}

	/**
	 * A public function which removes the given key from the tree.
	 * Note that if a node has children, the child nodes will be
	 * shifted around to accommodate the sudden disappearance of the
	 * parent node, which may result in multiple splits and mergings.
	 *
	 * @param	key	The key to be removed from the tree
	 * @return	The element previously associated with this key if one
	 * 			exists; <tt>null</tt> if the key could not be found
	 */

	public final Object remove( Comparable key )
	{
		// check to see if you have to even check this node at all
		// by checking the boundary keys

		if ( nodeSize == 0 )
			return null;

		int compareValue = key.compareTo( getKey(0) );
		if ( compareValue <= 0 )
			return remove( 0, compareValue, key );

		if ( nodeSize > 1 )
			compareValue = key.compareTo( getKey( nodeSize - 1 ) );
		if ( compareValue > 0 )
			return remove( nodeSize, compareValue, key );
		if ( compareValue == 0 )
			return remove( nodeSize - 1, compareValue, key );

		// if it's neither of those two locations, then locate the
		// correct position using binary search

		int index = indexOf( 1, nodeSize - 1, key, MATCH_INDEX );
		return remove( index, key.compareTo( getKey(index) ), key );
	}

	/**
	 * A helper function which actually attempts the removal.  This function
	 * is called whenever the correct index has been located and the key and
	 * corresponding element offset are to be removed into the B-Tree.
	 *
	 * @param	index	The index at which the key is assumed to be located
	 * @param	compareValue	The result of the key-index compare
	 * @param	key	The key to remove from the B-Tree node
	 * @return	The element previously associated with this key if one
	 * 			exists; <tt>null</tt> if the key could not be found
	 */

	private Object remove( int index, int compareValue, Comparable key )
	{
		// if the compare value indicates that the key was not found,
		// then continue looking for the key in the children, or if
		// this happens to be a leaf node, return from the function

		if ( compareValue != 0 )
		{
			if ( childCount > 0 )
				return getChild( index ).remove( key );
			return null;
		}

		// at this point, it is known that the key has been located;
		// thus, from here on out, the point of the code is to remove
		// the key and fix any problems that result from its removal

		Object removedElement = getElement( index );
		AbstractBTreeNode childNode = getChild( index );

		if ( childCount == 0 || childNode.nodeSize == 0 )
		{
			// because it is known that there are no children, all
			// that is needed is to shift over all of the keys one
			// element, decrement the size, remedy any underflow
			// that might have resulted, and return the offset

			delete( index );
			remedyUnderflow();
			return removedElement;
		}

		// if it gets to this point, it is known that there are
		// children at this current node, and so the largest key
		// found in its left child (namely, the element immediately
		// proceeding it in an in-order traversal) will replace it

		AbstractBTreeNode donorNode = this;
		while ( donorNode.childCount > 0 && childNode.nodeSize > 0 )
		{
			donorNode = childNode;
			childNode = donorNode.getChild( donorNode.nodeSize );
		}

		// now, it is known which node contains the largest key,
		// and so all that needs to be done is to shift the
		// largest key to this position; this is done by removing
		// the key from the donor node and storing it into this node
		// being sure to remedy any underflows that might have resulted

		store( index, donorNode.getKeyAsReference( donorNode.nodeSize - 1 ),
			donorNode.getElementAsReference( donorNode.nodeSize - 1 ) );

		donorNode.delete( donorNode.nodeSize - 1 );
		donorNode.remedyUnderflow();
		return removedElement;
	}

	/**
	 * An internal method which remedies underflows that might result
	 * from the removal of an element.  An underflow results when the
	 * size of a non-root node has dropped below half the capacity of
	 * the node (which potentially results in the tree being one-sided
	 * and thus, inefficient), and may be remedied by rotating the
	 * positions of keys which follow each other in a sorted ordering.
	 */

	private void remedyUnderflow()
	{
		// first, check to see if the underflow condition occurred;
		// if no underflow has happened, return from the function

		if ( nodeSize != (capacity >> 1) - 1 )
			return;

		AbstractBTreeNode parent = getParent();

		// an underflow cannot occur at the root; thus, make sure
		// that this condition is checked (many exceptions could
		// be thrown if this condition is not checked)

		if ( parent == null )
			return;

		boolean isClockwiseRotation = (index == parent.nodeSize);
		int donorIndex = isClockwiseRotation ? index - 1 : index + 1;
		AbstractBTreeNode donorNode = parent.getChild( donorIndex );

		// find out if the donor node should be merged with this
		// node in order to remedy the underflow, or if a straight
		// borrowing should occur

		if ( donorNode.nodeSize == (capacity >> 1) && (childCount != 0 || childCount == donorNode.childCount) )
		{
			// here, rotating the keys would result in an underflow
			// in the donating node; rather than resolving the issue
			// of a sibling node has an underflow (which potentially
			// results in an infinite loop), we instead risk having
			// an underflow occur in the parent, which has very finite
			// risks involved with it

			if ( isClockwiseRotation )
				parent.merge( donorIndex, index );
			else
				parent.merge( index, donorIndex );

			// once the merge occurs, nothing else needs to be done,
			// since the merge also checks for another underflow
			// occurring; at this point, simply return

			return;
		}

		// now it is known that the underflow should be remedied via
		// a key rotation, and that such a key rotation will *not*
		// result in a second underflow; rotate the keys and return

		int stolenKeyIndex = isClockwiseRotation ? parent.nodeSize - 1 : index;
		int donatedKeyIndex = isClockwiseRotation ? donorNode.nodeSize - 1 : 0;

		add( isClockwiseRotation ? 0 : nodeSize,
			parent.getKeyAsReference( stolenKeyIndex ), parent.getElementAsReference( stolenKeyIndex ) );

		// in addition to moving the keys, you have to move the child
		// associated with the given key into this node as well

		if ( childCount > 0 )
		{
			if ( isClockwiseRotation )
			{
				addChild( 0, donorNode.getChild( donorNode.nodeSize ) );
				donorNode.deleteChild( donorNode.nodeSize );
			}
			else
			{
				addChild( childCount, donorNode.getChild( 0 ) );
				donorNode.deleteChild( 0 );
			}
		}

		parent.store( stolenKeyIndex, donorNode.getKeyAsReference( donatedKeyIndex ),
			donorNode.getElementAsReference( donatedKeyIndex ) );
		donorNode.delete( donatedKeyIndex );
		donorNode.remedyUnderflow();
	}

	/**
	 * An internal method which merges two children together with the
	 * key between them in order to form a new child node.  If the
	 * current node is not the root, this could potentially result
	 * in an underflow occurring in the parent, which potentially
	 * causes many of the nodes to be restructured.  Thus, this is one
	 * of the more expensive operations and the B-Tree should be designed
	 * to use this operation as few times as possible.
	 *
	 * @param	leftIndex	The first index of the child to be merged
	 * @param	rightIndex	The second index of the child to be merged
	 */

	private void merge( int leftIndex, int rightIndex )
	{
		AbstractBTreeNode left = getChild( leftIndex );
		AbstractBTreeNode right = getChild( rightIndex );

		// make sure that this node is not a root whose size has
		// been reduced to 1; if it is, use a different function

		if ( nodeSize == 1 )
		{
			mergeRoot();
			return;
		}

		// make sure that the merge is feasible: ie, at least one node
		// must be of underflow-size, and the other cannot be larger
		// than half of the capacity

		int halfCapacity = (capacity >> 1);
		if ( left.nodeSize > halfCapacity || right.nodeSize > halfCapacity )
			return;
		if ( left.nodeSize == halfCapacity && right.nodeSize == halfCapacity )
			return;

		// first, add the key directly to the left child node - note
		// that this will not result in a split

		left.add( left.nodeSize, getKeyAsReference( leftIndex ), getElementAsReference( leftIndex ) );

		// then, shift all of the children of the right node over to
		// the left node in order to complete the new node; also, be
		// sure to update the information relatedto the left node

		AbstractBTreeNode child;
		for ( int i = 0; i < right.nodeSize; ++i )
		{
			left.add( left.nodeSize, right.getKeyAsReference(i), right.getElementAsReference(i) );
			if ( right.childCount > 0 )
				left.addChild( left.nodeSize, right.getChild(i) );
		}

		if ( right.childCount > 0 )
			left.addChild( left.nodeSize, right.getChild( right.nodeSize ) );

		// then, shift all of the children for this node over one slot
		// to accommodate the merge, being sure to check for another
		// underflow that resulted from the reduced key count in this
		// node where the merge occurred

		deleteChild( leftIndex + 1 );
		delete( leftIndex );
		remedyUnderflow();
	}

	/**
	 * An internal method which merges the two children of the root together
	 * along with the solitary key at the root in order to form a new root.
	 * Note that this function can only be called by the root node itself,
	 * and only if the root has been reduced to 1 key and both nodes are at
	 * less than half capacity.
	 */

	private void mergeRoot()
	{
		// ensure that this is actually a root which has been reduced
		// to exactly one key

		if ( nodeSize != 1 )
			return;

		// make sure that the merge is feasible: ie, at least one node
		// must be of underflow-size, and the other cannot be larger
		// than half of the capacity

		AbstractBTreeNode left = getChild( 0 );
		AbstractBTreeNode right = getChild( 1 );

		int halfCapacity = (capacity >> 1);
		if ( left.nodeSize > halfCapacity || right.nodeSize > halfCapacity )
			return;
		if ( left.nodeSize == halfCapacity && right.nodeSize == halfCapacity )
			return;

		Object rootKeyReference = getKeyAsReference(0);
		Object rootElementReference = getElementAsReference(0);

		clear();

		// first, add all the keys from the left child into this node;
		// also take the time to move the corresponding children over
		// into this node as well - note that this will not result in
		// a node split

		for ( int i = 0; i < left.nodeSize; ++i )
		{
			add( i, left.getKeyAsReference(i), left.getElementAsReference(i) );
			if ( left.childCount > 0 )
				addChild( i, left.getChild(i) );
		}
		if ( left.childCount > 0 )
			addChild( left.nodeSize, left.getChild( left.nodeSize ) );

		// now, add the key originally at the root back to this node
		// to prepare for adding of the right node

		add( nodeSize, rootKeyReference, rootElementReference );

		// now, add all the keys from the right child into this node;
		// also take the time to move the corresponding children over
		// into this node as well - note that this will not result in
		// a node split

		for ( int i = 0; i < right.nodeSize; ++i )
		{
			add( nodeSize, right.getKeyAsReference(i), right.getElementAsReference(i) );
			if ( right.childCount > 0 )
				addChild( nodeSize - 1, right.getChild(i) );
		}

		if ( right.childCount > 0 )
			addChild( nodeSize, right.getChild( right.nodeSize ) );
	}

	/**
	 * A public function which retrieves the element offset associated
	 * with the given key; this is the main function used for most
	 * <code>BTree</code>.
	 *
	 * @param	key	The key whose elemnt offset is to be retrieved from the tree
	 * @return	The element offset associated with this key if one exists;
	 * 			<tt>null</tt> if the key could not be found
	 */

	public final Object get( Comparable key )
	{
		// check to see if you have to even check this node at all
		// by checking the boundary keys

		if ( nodeSize == 0 )
			return null;

		int compareValue = key.compareTo( getKey(0) );
		if ( compareValue <= 0 )
			return get( 0, compareValue, key );

		if ( nodeSize > 1 )
			compareValue = key.compareTo( getKey( nodeSize - 1 ) );
		if ( compareValue > 0 )
			return get( nodeSize, compareValue, key );
		if ( compareValue == 0 )
			return get( nodeSize - 1, compareValue, key );

		// if it's neither of those two locations, then locate the
		// correct position using binary search

		int index = indexOf( 1, nodeSize - 1, key, MATCH_INDEX );
		return get( index, key.compareTo( getKey(index) ), key );
	}

	/**
	 * A helper function which actually attempts to retrieve the offset.  This
	 * function is called whenever the correct index has been located and the
	 * corresponding element offset is to be retrieved.
	 *
	 * @param	index	The index at which the key is assumed to be located
	 * @param	compareValue	The result of the key-index compare
	 * @param	key	The key to remove from the B-Tree node
	 * @return	The element offset associated with this key if one exists;
	 * 			<tt>null</tt> if the key could not be found
	 */

	private Object get( int index, int compareValue, Comparable key )
	{
		if ( compareValue == 0 )
			return getElement( index );

		if ( childCount > 0 )
			return getChild( index ).get( key );
		return null;
	}

	/**
	 * An internal method which clears the node of all of its references.
	 * Because all of the elements of the node are determined by its header
	 * information, this merely involves resetting that header information.
	 */

	protected abstract void clear();

	/**
	 * A public method which refreshes the node with all the correct
	 * information.  This is useful for when there is a central point
	 * where node information is considered "correct" and the node can
	 * refresh its information based on that data.
	 */

	public abstract void refresh();

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

	public abstract AbstractBTreeNode getParent();

	/**
	 * A method for retrieving the key at the given index.  This is used
	 * primarily by internal methods, but could alternatively be used if
	 * one wished to create an enumeration or iterator class for the node.
	 * By default, this function retrieves the reference to the key and
	 * translates the reference into the key.
	 *
	 * @param	index	The index of the key to be retrieved
	 * @return	The key in this node associated with the given index
	 */

	public Comparable getKey( int index )
	{	return referenceToKey( getKeyAsReference( index ) );
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

	protected abstract Object getKeyAsReference( int index );

	/**
	 * An internal method for translating a reference to a key.  By default,
	 * this method returns  a type-cast of the original object.  In cases
	 * where a real conversion is necessary, this would allow translation
	 * between the reference and the original key.
	 *
	 * @param	reference	The reference to be translated to a key
	 * @return	The key associated with the given reference
	 */

	protected Comparable referenceToKey( Object reference )
	{	return (Comparable) reference;
	}

	/**
	 * An internal method for translating a key to a reference, in the
	 * event that there is no reference available.  By default, the two
	 * are assumed to be identical, and so the key itself is returned.
	 *
	 * @param	key	The key to create a reference for
	 * @return	The reference created to be associated with the given key
	 */

	protected Object createKeyReference( Comparable key )
	{	return key;
	}

	/**
	 * An internal method for retrieving the reference into the database
	 * file for a given element.  This is used primarily when nodes
	 * are being added or removed in order to determine insertion or
	 * removal points.
	 *
	 * @param	index	The index of the element offset to be retrieved
	 * @return	The element offset associated with the given index
	 */

	protected Object getElement( int index )
	{	return referenceToElement( getElementAsReference( index ) );
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

	protected abstract Object getElementAsReference( int index );

	/**
	 * An internal method for translating a reference to an element.  By
	 * default, this method returns  a type-cast of the original object.
	 * In cases where a real conversion is necessary, this would allow
	 * translation between the reference and the original key.
	 */

	protected Object referenceToElement( Object reference )
	{	return reference;
	}

	/**
	 * An internal method for translating an element to a reference, in the
	 * event that there is no reference available.  By default, the two
	 * are assumed to be identical, and so the element itself is returned.
	 *
	 * @param	element	The element to create a reference for
	 * @return	The reference created to be associated with the given key
	 */

	protected Object createElementReference( Object element )
	{	return element;
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

	protected abstract void store( int index, Object keyReference, Object elementReference );

	/**
	 * An internal method which adds a key at the specified index.  By
	 * default, this is accomplished by having all of the elements which are
	 * located after the index be shifted up and the element be stored at the
	 * appropriate position.  Note that the key provided in this function
	 * is the reference to the key, not the key itself.
	 *
	 * @param	index	The index at which the element and key are to be added
	 * @param	keyReference	The key to be added at the given index
	 * @param	elementReference	The offset to be associated with the given key
	 */

	protected void add( int index, Object keyReference, Object elementReference )
	{
		Object insertKeyReference = keyReference;
		Object removeKeyReference;
		Object insertElementReference = elementReference;
		Object removeElementReference;

		for ( int i = index; i < nodeSize; ++i )
		{
			removeKeyReference = getKeyAsReference(i);
			removeElementReference = getElementAsReference(i);
			store( i, insertKeyReference, insertElementReference );
			insertKeyReference = removeKeyReference;
			insertElementReference = removeElementReference;
		}

		// there is one last element to insert into the tree,
		// which is the last key and offset in the batch

		store( nodeSize++, insertKeyReference, insertElementReference );
	}

	/**
	 * An internal method which deletes the key-element pair at the given
	 * index from the node.  By default, this is accomplished by shifting
	 * all of the succeeding keys down in order to overwrite the index.
	 *
	 * @param	index	The index of the key to be deleted
	 */

	protected void delete( int index )
	{
		for ( int i = index + 1; i < nodeSize; ++i )
			store( i - 1, getKeyAsReference(i), getElementAsReference(i) );
		--nodeSize;
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

	protected abstract AbstractBTreeNode createChild( int index );

	/**
	 * A method for retrieving the child reference stored at the given index.
	 * The index of a child indicates that all of its keys are less than
	 * the key with the same index in this node, or greater than all keys
	 * in this node if the index is larger than the number of keys.
	 *
	 * @param	index	The index of the element offset to be retrieved
	 * @return	The child node associated with the given index
	 */

	public abstract AbstractBTreeNode getChild( int index );

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

	protected abstract void storeChild( int index, AbstractBTreeNode child );

	/**
	 * An internal method for adding a reference to the child node.  By
	 * default, in order to accomplish this, all of the elements which are
	 * located after the index are shifted up and the reference is stored
	 * at the appropriate position.
	 *
	 * @param	index	The index at which the child reference is to be added
	 */

	protected void addChild( int index, AbstractBTreeNode child )
	{
		AbstractBTreeNode insertChild = child, removeChild = null;
		for ( int i = index; i < childCount; ++i )
		{
			removeChild = getChild(i);
			storeChild( i, insertChild );
			insertChild = removeChild;
		}

		storeChild( childCount++, insertChild );
	}

	/**
	 * An internal method which deletes the child reference at the
	 * given index from the node.  By default, this is accomplished by
	 * shifting all the succeeding child references down in order to
	 * overwrite the index.
	 *
	 * @param	index	The index of the key to be deleted
	 */

	protected void deleteChild( int index )
	{
		for ( int i = index + 1; i < childCount; ++i )
			storeChild( i - 1, getChild(i) );
		--childCount;
	}

	/**
	 * A helper function which calculates the index of a key using
	 * binary search.  Because this is intended for large on-disk
	 * databases with extremely large nodes, this should save quite
	 * a bit of time as the nodes get larger; in truth, this is a
	 * slight modification of the one in <code>SortedListModel</code>.
	 *
	 * @param	beginIndex	The starting index of the binary search
	 * @param	endIndex	The ending index of the binary search
	 * @param	key	The key to be located
	 */

	private int indexOf( int beginIndex, int endIndex, Comparable key, int whichIndexOf )
	{
		// if the binary search has been terminated, return -1
		if ( beginIndex < 0 || beginIndex > endIndex || endIndex >= nodeSize )
			return beginIndex;

		// calculate the halfway point and compare the key with the
		// key located at the halfway point - note that in locating
		// the last index of, the value is rounded up to avoid an infinite
		// recursive loop

		int halfwayIndex = beginIndex + endIndex;
		if ( whichIndexOf == INSERTION )
			++halfwayIndex;
		halfwayIndex >>= 1;

		Comparable halfwayKey = getKey( halfwayIndex );
		int compareResult = halfwayKey.compareTo( key );

		// if the key in the middle is larger than the key being checked,
		// then it is known that the key is smaller than the middle key,
		// so it must preceed the middle key

		if ( compareResult > 0 )
			return indexOf( beginIndex, halfwayIndex - 1, key, whichIndexOf );

		// if the key in the middle is smaller than the key being checked,
		// then it is known that the key is larger than the middle key, so
		// it must succeed the middle key

		if ( compareResult < 0 )
			return indexOf( halfwayIndex + 1, endIndex, key, whichIndexOf );

		// if the key in the middle is equal to the key being checked,
		// then it is known that you have located at least one occurrence of the
		// object; if the range has not yet been narrowed completely, continue
		// to narrow the range - locate the last index of the key by looking
		// in the second half, and locate all others by looking in the first half

		if ( beginIndex != endIndex )
		{
			if ( whichIndexOf == INSERTION )
				return indexOf( halfwayIndex, endIndex, key, whichIndexOf );
			return indexOf( beginIndex, halfwayIndex, key, whichIndexOf );
		}

		// if the range has been narrowed completely, then you have found the
		// appropriate index;  thus, you should return it

		return halfwayIndex;
	}

	/**
	 * A public function used to display a subtree.  Each node in the
	 * <code>BTree</code> prints its contents in reverse so
	 * that turning the image 90 degrees clockwise shows its layout.
	 * Note that this method is only particularly useful for very small
	 * trees in debugging whether or not a certain function is being
	 * correctly implemented.
	 *
	 * @param	indentAmount	The number of indents to give this node
	 * @param	indentString	The string used to display each indent
	 */

	public void printNodeContents( int indentAmount, String indentString )
	{
		if ( childCount > 0 )
			getChild( nodeSize ).printNodeContents( indentAmount + 1, indentString );

		for ( int i = nodeSize - 1; i >= 0; --i )
		{
			for ( int j = 0; j < indentAmount; ++j )
				System.out.print( indentString );
			System.out.println( getKey(i) );

			if ( childCount > 0 )
				getChild(i).printNodeContents( indentAmount + 1, indentString );
		}
	}

	/**
	 * Returns an iterator which iterates over the subtree rooted at this node.
	 * Note that if this node is the root of the tree, this creates an iterator
	 * which iterates over the entire tree.
	 *
	 * @return	An iterator which iterates over the subtree rooted at this node
	 */

	public Iterator iterator()
	{	return new BTreeNodeIterator();
	}

	/**
	 * An internal class which allows iteration over a node.  The idea behind
	 * this class is to allow the creation of a recursive iterator in order to
	 * traverse the entire tree.  This would be more intuitive than creating
	 * any other form of iterator, and would also be far less expensive.
	 */

	private class BTreeNodeIterator implements Iterator
	{
		private int enumerated;
		private int currentIndex;
		private Object lastElement;
		private Iterator childIterator;

		/**
		 * Constructs a new <code>BTreeNodeIterator</code> which iterates over
		 * the subtree rooted at this node.  Note that the iterator is dependent
		 * on the structure of the <code>AbstractBTreeNode</code> and modifications
		 * to the node itself result in unreliable behavior.
		 */

		public BTreeNodeIterator()
		{
			enumerated = 0;
			currentIndex = 0;
			lastElement = null;
			childIterator = childCount == 0 ? null : getChild(0).iterator();
		}

		/**
		 * Returns the next element in the iteration.  Note that the
		 * method does <i>not</i> throw an exception in the event that
		 * there are no elements left; because no <tt>null</tt> keys
		 * are permitted, a return of <tt>null</tt> from this method
		 * indicates that there are no elements left.
		 *
		 * @return	The next element in the iteration; <tt>null</tt> if
		 *			the iteration has been exhausted
		 */

		public Object next()
		{
			// if no elements remain, then simply return null from
			// this function, since there is no definition as to
			// what should be done if no element was found

			if ( !hasNext() )
				return null;

			// increment the number of elements that have been
			// enumerated at this node thus far

			++enumerated;

			// if there is a child iterator existing (either because
			// it existed before or was just created after the check
			// for null), then the next element is inside there

			if ( childIterator != null && childIterator.hasNext() )
				return lastElement = childIterator.next();

			// otherwise, the child iterator has no more elements, so
			// the next element in the iteration is the key at the
			// current index, and the next child is in the subsequent
			// index

			childIterator = childCount == 0 ? null : getChild( currentIndex + 1 ).iterator();
			return lastElement = getKey( currentIndex++ );
		}

		/**
		 * Determines whether or not there are any elements left in the
		 * iteration.  This function should be used when iterating to
		 * ensure that the values returned by <code>next()</code> are
		 * non-<tt>null</tt>.
		 *
		 * @return	<tt>true</tt> if there are more elements in the iteration
		 */

		public boolean hasNext()
		{
			return currentIndex < nodeSize ||
				( childCount > 0 && childIterator.hasNext() );
		}

		/**
		 * Removes the last element seen in the iteration.  Note that this
		 * method allows repeated calls to remove all of the elements seen
		 * in the iteration thus far.
		 */

		public void remove()
		{
			if ( lastElement == null )
				return;

			AbstractBTreeNode.this.remove( (Comparable)lastElement );

			// because remove modifies the underlying structure with
			// which this iterator locates its next elements, in order
			// to restore the position of the pointer to the correct
			// position, all the values have to be reset, and you have
			// to redo the iteration to this point, skipping the element
			// that was just deleted

			currentIndex = 0;
			lastElement = null;

			// you need to refresh the root node after a removal as
			// well, since the variables stored in memory no longer
			// match what is on disk

			refresh();

			childIterator = childCount == 0 ? null : getChild(0).iterator();
			--enumerated;  for ( int i = 0; i < enumerated; ++i, next() );
		}
	}
}