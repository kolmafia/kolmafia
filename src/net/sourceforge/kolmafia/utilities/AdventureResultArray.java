package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;

/**
 * Internal class which functions exactly an array of strings, except it uses "sets" and "gets" like a list. This
 * could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
 */

public class AdventureResultArray
	implements Iterable<AdventureResult>
{
	private final ArrayList<AdventureResult> internalList;

	public AdventureResultArray()
	{
		this.internalList = new ArrayList<>();
	}

	public AdventureResultArray( final List<AdventureResult> data )
	{
		this.internalList = new ArrayList<>( data );
	}

	public Iterator<AdventureResult> iterator()
	{
		return this.internalList.iterator();
	}

	public AdventureResult get( final int index )
	{
		return this.internalList.get( index );
	}

	public void set( final int index, final AdventureResult value )
	{
		this.internalList.set( index, value );
	}

	public void add( final AdventureResult s )
	{
		this.internalList.add( s );
	}

	public void clear()
	{
		this.internalList.clear();
	}

	public AdventureResult[] toArray()
	{
		AdventureResult[] array = new AdventureResult[ this.internalList.size() ];
		this.internalList.toArray( array );
		return array;
	}

	public int size()
	{
		return this.internalList.size();
	}

	public boolean isEmpty()
	{
		return this.internalList.isEmpty();
	}

	public boolean contains( AdventureResult ar )
	{
		return this.internalList.contains( ar );
	}
}
