/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KoLDatabase
{
	private static class ItemCounter
		implements Comparable
	{
		private final int count;
		private final String name;

		public ItemCounter( final String name, final int count )
		{
			this.name = name;
			this.count = count;
		}

		public int compareTo( final Object o )
		{
			ItemCounter ic = (ItemCounter) o;

			if ( this.count != ic.count )
			{
				return ic.count - this.count;
			}

			return this.name.compareToIgnoreCase( ic.name );
		}

		@Override
		public String toString()
		{
			return this.name + ": " + this.count;
		}
	}

	public static final String getBreakdown( final List items )
	{
		if ( items.isEmpty() )
		{
			return KoLConstants.LINE_BREAK;
		}

		StringBuffer strbuf = new StringBuffer();
		strbuf.append( KoLConstants.LINE_BREAK );

		Object[] itemArray = new Object[ items.size() ];
		items.toArray( itemArray );

		int currentCount = 1;

		ArrayList<ItemCounter> itemList = new ArrayList<ItemCounter>();

		for ( int i = 1; i < itemArray.length; ++i )
		{
			if ( itemArray[ i - 1 ] == null )
			{
				continue;
			}

			if ( itemArray[ i ] != null && !itemArray[ i - 1 ].equals( itemArray[ i ] ) )
			{
				itemList.add( new ItemCounter( itemArray[ i - 1 ].toString(), currentCount ) );
				currentCount = 0;
			}

			++currentCount;
		}

		if ( itemArray[ itemArray.length - 1 ] != null )
		{
			itemList.add( new ItemCounter( itemArray[ itemArray.length - 1 ].toString(), currentCount ) );
		}

		strbuf.append( "<ul>" );
		Collections.sort( itemList );

		for ( int i = 0; i < itemList.size(); ++i )
		{
			strbuf.append( "<li><nobr>" + itemList.get( i ) + "</nobr></li>" );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</ul>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	/**
	 * Calculates the sum of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects.
	 */

	public static final long calculateTotal( final List values )
	{
		long total = 0;
		for ( int i = 0; i < values.size(); ++i )
		{
			if ( values.get( i ) != null )
			{
				total += ( (Integer) values.get( i ) ).intValue();
			}
		}

		return total;
	}

	/**
	 * Calculates the average of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects.
	 */

	public static final float calculateAverage( final List values )
	{
		return (float) KoLDatabase.calculateTotal( values ) / (float) values.size();
	}
}
