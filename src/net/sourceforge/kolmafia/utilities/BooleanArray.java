package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;

/**
 * Internal class which functions exactly an array of boolean, except it uses "sets" and "gets" like a list. This could
 * be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
 */

public class BooleanArray
{
	private final ArrayList<Boolean> internalList = new ArrayList<Boolean>();

	public boolean get( final int index )
	{
		return index >= 0 && index < this.internalList.size() && this.internalList.get(index);
	}

	public void set( final int index, final boolean value )
	{
		while ( index >= this.internalList.size() )
		{
			this.internalList.add( Boolean.FALSE );
		}

		this.internalList.set( index, value ? Boolean.TRUE : Boolean.FALSE );
	}

	public int size()
	{
		return this.internalList.size();
	}
}
