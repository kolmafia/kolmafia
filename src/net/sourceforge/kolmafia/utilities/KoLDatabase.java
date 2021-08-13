/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.kolmafia.KoLConstants;

public class KoLDatabase
{
	private static class ItemCounter
		implements Comparable<ItemCounter>
	{
		private final int count;
		private final String name;

		public ItemCounter( final String name, final int count )
		{
			this.name = name;
			this.count = count;
		}

		public int compareTo( final ItemCounter o )
		{

			if ( this.count != o.count )
			{
				return o.count - this.count;
			}

			return this.name.compareToIgnoreCase( o.name );
		}

		@Override
		public String toString()
		{
			return this.name + ": " + this.count;
		}
	}

	public static String getBreakdown(final List<String> items )
	{
		if ((items == null ) || ( items.isEmpty() ))
		{
			return KoLConstants.LINE_BREAK;
		}

		StringBuilder strbuf = new StringBuilder();
		strbuf.append( KoLConstants.LINE_BREAK );

		Object[] itemArray = new Object[ items.size() ];
		items.toArray( itemArray );

		int currentCount = 1;

		ArrayList<ItemCounter> itemList = new ArrayList<>();

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

		for (ItemCounter itemCounter : itemList) {
			strbuf.append("<li><nobr>").append(itemCounter).append("</nobr></li>");
			strbuf.append(KoLConstants.LINE_BREAK);
		}

		strbuf.append( "</ul>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	/**
	 * Calculates the sum of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects and the only usages of this pass a list of Integers
	 */

	public static long calculateTotal(final List<Integer> values )
	{
		long total = 0;
		for (Integer obj : values) {
			if (obj != null) {
				total += obj;
			}
		}

		return total;
	}

	/**
	 * Calculates the average of all the integers in the given list. Note that the list must consist entirely of Integer
	 * objects.
	 */

	public static float calculateAverage(final List<Integer> values )
	{
		return (float) KoLDatabase.calculateTotal( values ) / (float) values.size();
	}
}
