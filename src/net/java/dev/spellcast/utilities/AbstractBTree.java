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

// Java utilities
import java.util.Map;
import java.util.Iterator;

import java.util.Set;
import java.util.AbstractSet;
import java.util.Collection;

/**
 * A generic B-tree data structure that makes use of a <code>AbstractBTreeNode</code>
 * as a root in order to satisfy the requirements of a B-tree.  Note that the only thing
 * that is required in order to properly extend this class is to create an extension of
 * the <code>AbstractBTreeNode</code> and have the child class call <code>setRoot()</code>.
 * Though it may make sense to allow for direct use of this class by making the method
 * <code>setRoot()</code> public, in order to ensure that this sequence takes place and
 * to avoid random resets of the root node, this was considered the best design method.
 */

public abstract class AbstractBTree implements Map
{
	private Class keyClass;
	private AbstractBTreeNode root;
	private int databaseSize;

	/**
	 * Extensions of this class should call this constructor in order
	 * to initialize the <code>Comparable</code> class to be used as a key.
	 * Because this is an abstract class, only classes extending it should
	 * ever be able to call it, hence its <tt>protected</tt> access.
	 */

	protected AbstractBTree( Class keyClass )
	{
		// initialize all the file values
		this.root = null;
		this.databaseSize = 0;
		setKeyClass( keyClass );
	}

	/**
	 * Sets the root of the <code>AbstractBTree</code> to the given node.
	 * This function allows all extensions of the <code>AbstractBTree</code>
	 * to simply call this function whenever the root is reset.
	 *
	 * @param	root	The new root for this B-tree
	 */

	protected void setRoot( AbstractBTreeNode root )
	{
		this.root = root;
		root.refresh();
	}

	/**
	 * Please refer to {@link java.util.Map#clear()} for more information
	 * regarding this function.
	 */

	public void clear()
	{
		if ( root == null )
			throw new NullPointerException( "The root has not yet been initialized" );

		Iterator rootIterator = root.iterator();
		while ( rootIterator.hasNext() )
		{
			rootIterator.next();
			rootIterator.remove();
			updateDatabaseSize( databaseSize - 1 );
		}
	}

	/**
	 * Please refer to {@link java.util.Map#containsKey(Object)} for more information
	 * regarding this function.
	 */

	public boolean containsKey( Object key )
	{
		if ( !keyClass.isInstance( key ) )
			return false;
		return get( key ) != null;
	}

	/**
	 * Please refer to {@link java.util.Map#containsValue(Object)} for more information
	 * regarding this function.
	 */

	public boolean containsValue( Object value )
	{	return values().contains( value );
	}

	/**
	 * Please refer to {@link java.util.Map#entrySet()} for more information
	 * regarding this function.
	 */

	public Set entrySet()
	{	return new AbstractBTreeSet( AbstractBTreeSet.ENTRY_SET );
	}

	/**
	 * Please refer to {@link java.util.Map#equals(Object)} for more information
	 * regarding this function.
	 */

	public boolean equals( Object o )
	{
		if ( o == null || !(o instanceof Map) )
			return false;
		return entrySet().equals( ((Map)o).entrySet() );
	}

	/**
	 * Please refer to {@link java.util.Map#get(Object)} for more
	 * information regarding this function.
	 */

	public Object get( Object key )
	{
		if ( root == null )
			throw new NullPointerException( "The root has not yet been initialized" );

		if ( key == null )
			return null;

		if ( !keyClass.isInstance( key ) )
			throw new IllegalArgumentException( "All keys must be of class " + keyClass.getName() );

		return root.get( (Comparable)key );
	}

	/**
	 * Retrieves the class associated with all of the keys within this
	 * <code>DiskAccessBTree</code>.  Casting any key from the tree
	 * into this class will be considered valid.
	 *
	 * @return	The class which all keys instantiate
	 */

	public Class getKeyClass()
	{	return keyClass;
	}

	/**
	 * Please refer to {@link java.util.Map#hashCode()} for more information
	 * regarding this function.
	 */

	public int hashCode()
	{	return entrySet().hashCode();
	}

	/**
	 * Please refer to {@link java.util.Map#isEmpty()} for more information
	 * regarding this function.
	 */

	public boolean isEmpty()
	{	return size() == 0;
	}

	/**
	 * Please refer to {@link java.util.Map#keySet()} for more information
	 * regarding this function.
	 */

	public Set keySet()
	{	return new AbstractBTreeSet( AbstractBTreeSet.KEY_SET );
	}

	/**
	 * Please refer to {@link java.util.Map#put(Object,Object)} for more
	 * information regarding this function.
	 */

	public Object put( Object key, Object element )
	{
		if ( root == null )
			throw new NullPointerException( "The root has not yet been initialized" );
		if ( key == null )
			throw new IllegalArgumentException( "You cannot use null keys to reference elements" );
		if ( element == null )
			throw new IllegalArgumentException( "You cannot place a null element into this AbstractBTree" );
		if ( !keyClass.isInstance( key ) )
			throw new IllegalArgumentException( "All keys must be of class " + keyClass.getName() );

		Object oldElement = root.put( (Comparable)key, element );
		root.refresh();

		if ( oldElement == null )
		{
			updateDatabaseSize( databaseSize + 1 );
			return null;
		}
		return oldElement;
	}

	/**
	 * Please refer to {@link java.util.Map#putAll(Map)} for more
	 * information regarding this function.
	 */

	public void putAll( Map t )
	{
		Object currentKey;
		Iterator mapKeys = t.keySet().iterator();

		while ( mapKeys.hasNext() )
		{
			currentKey = mapKeys.next();
			put( currentKey, t.get( currentKey ) );
		}
	}

	/**
	 * Please refer to {@link java.util.Map#remove(Object)} for more
	 * information regarding this function.
	 */

	public Object remove( Object key )
	{
		if ( root == null )
			throw new NullPointerException( "The root has not yet been initialized" );
		if ( key == null )
			throw new IllegalArgumentException( "You cannot use null keys to reference elements" );
		if ( !keyClass.isInstance( key ) )
			throw new IllegalArgumentException( "Invalid key" );

		Object oldElement = root.remove( (Comparable)key );
		root.refresh();

		if ( oldElement != null )
			updateDatabaseSize( databaseSize - 1 );
		return oldElement;
	}

	/**
	 * This function allows the setting/resetting of the class for the
	 * elements of the <code>AbstractBTree</code>.  Note that the reset
	 * can only be done to make the tree more general, not more specific;
	 * ensuring the reverse would be very expensive time-wise, or would
	 * result in the key being unreliable (if it were not expensive),
	 * or require tracking of the most general class currently used by
	 * all the keys, which would ultimately break down when elements were
	 * removed from the <code>AbstractBTree</code>.
	 *
	 * @param	keyClass	the new class which all keys must match
	 */

	protected void setKeyClass( Class keyClass )
	{
		if ( !Comparable.class.isAssignableFrom( keyClass ) )
			throw new IllegalArgumentException( "The class used for the keys must all implement Comparable" );
		if ( !(this.keyClass == null) && !keyClass.isAssignableFrom( this.keyClass ) )
			throw new IllegalArgumentException( "New class must be a parent of current class" );
		this.keyClass = keyClass;
	}

	/**
	 * Please refer to {@link java.util.Map#size()} for more information
	 * regarding this function.
	 */

	public int size()
	{	return databaseSize;
	}

	/**
	 * Whenever the <code>AbstractBTree</code> is modified (such as removal
	 * or addition of elements), the size needs to be updated, which, in
	 * turn, could potentially have side-effects related to the tree.  Rather
	 * than have child classes redefine every modify-related function, they
	 * may opt instead to redefine this function.
	 */

	protected void updateDatabaseSize( int databaseSize )
	{	this.databaseSize = databaseSize;
	}

	/**
	 * Please refer to {@link java.util.Map#values()} for more information
	 * regarding this function.
	 */

	public Collection values()
	{	return new AbstractBTreeSet( AbstractBTreeSet.VALUE_SET );
	}

	/**
	 * An internal class used to create all of the different sets that are needed
	 * in order to properly implement the set-related methods of the <code>Map</code>
	 * interface.  Note that because this is a <code>AbstractBTree</code> with
	 * an extremely large size, and because all of the functions are implemented
	 * via an <code>AbstractSet</code> which utilizes an iterator to accomplish
	 * all of its functionality, the operations on this set for larger maps is very
	 * slow; thus, unless sets are truly desired, one should use the functions
	 * which are available through the map itself.
	 */

	protected class AbstractBTreeSet extends AbstractSet
	{
		public static final int KEY_SET   = 1;
		public static final int VALUE_SET = 2;
		public static final int ENTRY_SET = 3;

		private int setType;

		/**
		 * Constructs a new <code>AbstractBTreeSet</code> of the given
		 * type.  The types are defined as public constants in the internal
		 * class and are used, as appropriate, by the enclosing class.
		 * Note that this class is not accessible to child classes, nor is
		 * it accessible to classes inside or outside of the package.
		 */

		public AbstractBTreeSet( int setType )
		{
			if ( setType != KEY_SET && setType != VALUE_SET && setType != ENTRY_SET )
				throw new IllegalArgumentException( "invalid set type" );
			this.setType = setType;
		}

		/**
		 * The method missing returns an iterator of the appropriate
		 * type, matching the <code>AbstractBTreeSet</code>.
		 *
		 * @return	The iterator for this <code>AbstractBTreeSet</code>
		 */

		public Iterator iterator()
		{	return new AbstractBTreeIterator();
		}

		/**
		 * Returns the size of this set.  Because this set uses the map
		 * as a backing, it merely calls the corresponding function that
		 * is found in <code>Map</code>.
		 *
		 * @return	The size of this database
		 */

		public int size()
		{	return AbstractBTreeSet.this.size();
		}

		/**
		 * An internal class which implements all of the iterator functionality
		 * needed for any of the <code>AbstractBTree</code> sets.  This iterator
		 * uses the node iterator for the root node in order to accomplish all
		 * of the non-set-dependant operations.
		 */

		protected class AbstractBTreeIterator implements Iterator
		{
			private Iterator rootIterator;

			/**
			 * Constructs a new <code>AbstractBTreeIterator</code> by
			 * instantiating a node iterator for the root.
			 */

			public AbstractBTreeIterator()
			{
				if ( root == null )
					throw new NullPointerException( "The root has not yet been initialized" );
				rootIterator = root.iterator();
			}

			/**
			 * Returns the next element in the iteration.  Note that the
			 * element type returned is dependent on the type of set that
			 * is being iterated upon.
			 *
			 * @return	The next element in the iteration; <tt>null</tt> if
			 *			the iteration has been exhausted
			 */

			public Object next()
			{
				Object nextKey = rootIterator.next();

				if ( nextKey == null )
					return null;

				switch ( setType )
				{
					case KEY_SET:
						return nextKey;
					case VALUE_SET:
						return get( nextKey );
					case ENTRY_SET:
						return new AbstractBTreeEntry( nextKey, get( nextKey ) );
				}

				// if the function gets this far, then something happened in
				// the constructor which resulted in the IllegalArgumentException
				// not being thrown, but the set type was invalid

				throw new InternalError();
			}

			/**
			 * Determines whether or not there are any elements left in the
			 * enumeration.  This function should be used when iterating to
			 * ensure that the values returned by <code>next()</code> are
			 * non-<tt>null</tt>.
			 *
			 * @return	<tt>true</tt> if there are more elements in the iteration
			 */

			public boolean hasNext()
			{	return rootIterator.hasNext();
			}

			/**
			 * Removes the last element seen in the iteration.  Note that this
			 * method allows repeated calls to remove all of the elements seen
			 * in the iteration thus far.
			 */

			public void remove()
			{
				rootIterator.remove();
				updateDatabaseSize( databaseSize - 1 );
			}

			/**
			 * An internal class for the <code>AbstractBTreeIterator</code> which
			 * allows it to construct the appropriate <code>Entry</code> values to
			 * return, should the set be an entry set.
			 */

			private class AbstractBTreeEntry implements Entry
			{
				private Object key, value;

				/**
				 * Constructs a new <code>Entry</code> with the given key and
				 * value.  Note that the value is the *actual* object that is
				 * being stored by the tree, not the element offset associated
				 * with the object.
				 */

				public AbstractBTreeEntry( Object key, Object value )
				{
					this.key = key;
					this.value = value;
				}

				/**
				 * Please refer to {@link java.util.Map.Entry#equals(Object)} for more
				 * information regarding this function.
				 */

				public boolean equals( Object o )
				{
					if ( o == null || !(o instanceof Entry) )
						return false;

					Entry e1 = this, e2 = (Entry) o;

					return (e1.getKey() == null ? e2.getKey() == null :
						e1.getKey().equals( e2.getKey() )) &&
							(e1.getValue() == null ? e2.getValue() == null :
								e1.getValue().equals( e2.getValue() ));
				}

				/**
				 * Please refer to {@link java.util.Map.Entry#getKey()} for more
				 * information regarding this function.
				 */

				public Object getKey()
				{	return key;
				}

				/**
				 * Please refer to {@link java.util.Map.Entry#getValue()} for more
				 * information regarding this function.
				 */

				public Object getValue()
				{	return value;
				}

				/**
				 * Please refer to {@link java.util.Map.Entry#hashCode()} for more
				 * information regarding this function.
				 */

				public int hashCode()
				{
					return (getKey() == null ? 0 : getKey().hashCode()) ^
						(getValue() == null ? 0 : getValue().hashCode());
				}

				/**
				 * Please refer to {@link java.util.Map.Entry#setValue(Object)} for
				 * more information regarding this function.
				 */

				public Object setValue( Object value )
				{	return put( getKey(), value );
				}
			}
		}
	}
}