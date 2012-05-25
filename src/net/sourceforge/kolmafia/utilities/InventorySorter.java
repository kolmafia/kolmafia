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

package net.sourceforge.kolmafia.utilities;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import com.sun.java.forums.TableSorter;

/*
 InventorySorter is a TableSorter with an altered Comparator.
 */
public class InventorySorter
	extends TableSorter
{
	private static final Comparator<Object> COL2_COMPARATOR = new Comparator<Object>()
	{
		private final Pattern meatPattern = Pattern.compile( "\\((-?\\d+) meat\\)" );

		public int compare( Object o1, Object o2 )
		{
			Matcher matcher1 = meatPattern.matcher( o1.toString() );
			Matcher matcher2 = meatPattern.matcher( o2.toString() );
			if ( !matcher1.find() )
			{
				return -1;
			}
			else if ( !matcher2.find() )
			{
				return 1;
			}
			// if we're here, both strings are in the format (\d+ meat)
			Integer o1val = Integer.valueOf( matcher1.group( 1 ) );
			Integer o2val = Integer.valueOf( matcher2.group( 1 ) );

			return o1val.compareTo( o2val );
		}
	};

	public InventorySorter( TableModel tableModel, JTableHeader tableHeader )
	{
		super( tableModel, tableHeader );
	}

	@Override
	protected Comparator<Object> getComparator( final int column )
	{
		switch ( column )
		{
		case 0:
			return TableSorter.LEXICAL_COMPARATOR;
		case 1:
			return InventorySorter.COL2_COMPARATOR;
		case 2:
			return TableSorter.COMPARABLE_COMAPRATOR;
		}
		return COMPARABLE_COMAPRATOR;
	}
}