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
import java.util.HashMap;
import java.util.Iterator;

import net.sourceforge.kolmafia.utilities.IntegerCache;

public class DebugModifiers
	extends Modifiers
{
	private static HashMap wanted, adjustments;
	private static String currentDesc;
	private static StringBuffer buffer;

	public static int setup( String parameters )
	{
		DebugModifiers.wanted = new HashMap();
		for ( int i = 0; i < Modifiers.FLOAT_MODIFIERS; ++i )
		{
			String name = Modifiers.getModifierName( i );
			if ( name.toLowerCase().indexOf( parameters ) != -1 )
			{
				DebugModifiers.wanted.put( IntegerCache.valueOf( i ),
					"<td colspan=2>" + name + "</td>" );
			}
		}
		DebugModifiers.adjustments = (HashMap) DebugModifiers.wanted.clone();
		DebugModifiers.currentDesc = "source";
		DebugModifiers.buffer = new StringBuffer( "<table border=2>" );
		return DebugModifiers.wanted.size();
	}
	
	private static void flushRow()
	{
		DebugModifiers.buffer.append( "<tr><td>" );
		DebugModifiers.buffer.append( DebugModifiers.currentDesc );
		DebugModifiers.buffer.append( "</td>" );
		Iterator i = DebugModifiers.wanted.keySet().iterator();
		while ( i.hasNext() )
		{
			Object key = i.next();
			String item = (String) DebugModifiers.adjustments.get( key );
			if ( item != null )
			{
				DebugModifiers.buffer.append( item );
			}
			else
			{
				DebugModifiers.buffer.append( "<td></td><td></td>" );
			}
		}
		DebugModifiers.buffer.append( "</tr>" );
		DebugModifiers.adjustments.clear();
	}
	
	public void add( final int index, final double mod, final String desc )
	{
		if ( index < 0 || index >= Modifiers.FLOAT_MODIFIERS || mod == 0.0 )
		{
			return;
		}
		
		super.add( index, mod, desc );
		
		Integer key = IntegerCache.valueOf( index );
		if ( ! DebugModifiers.wanted.containsKey( key ) )
		{
			return;
		}
		
		if ( desc != DebugModifiers.currentDesc ||
			DebugModifiers.adjustments.containsKey( key ) )
		{
			DebugModifiers.flushRow();
		}
		DebugModifiers.currentDesc = desc;
		DebugModifiers.adjustments.put( key, "<td>" +
			KoLConstants.ROUNDED_MODIFIER_FORMAT.format( mod ) + "</td><td>=&nbsp;" +
			this.get( index ) + "</td>" );
	}

	public static void finish()
	{
		DebugModifiers.flushRow();
		DebugModifiers.buffer.append( "</table><br>" );
		RequestLogger.printLine( DebugModifiers.buffer.toString() );
		RequestLogger.printLine();
		DebugModifiers.buffer = null;
	}

	public static void allModifiers()
	{
		DebugModifiers.buffer.append( "<tr>" );
		Iterator i = DebugModifiers.wanted.keySet().iterator();
		while ( i.hasNext() )
		{
			Object key = i.next();
			int ikey = ((Integer) key).intValue();
			String item = (String) DebugModifiers.wanted.get( key );
			DebugModifiers.buffer.append( item );
			ArrayList list = new ArrayList();
			Iterator allmods = Modifiers.getAllModifiers();
			while ( allmods.hasNext() )
			{
				String name = (String) allmods.next();
				Modifiers mods = Modifiers.getModifiers( name );
				float value = mods.get( ikey );
				if ( value != 0.0f )
				{
					list.add( new Change( name, value,
						mods.getBoolean( Modifiers.VARIABLE ) ) );
				}
				if ( list.size() > 0 )
				{
					Collections.sort( list );
					DebugModifiers.adjustments.put( key, list.iterator() );
				}
				else
				{
					DebugModifiers.adjustments.remove( key );
				}
			}
		}
		DebugModifiers.buffer.append( "</tr>" );
		while ( DebugModifiers.adjustments.size() > 0 )
		{
			DebugModifiers.buffer.append( "<tr>" );
			i = DebugModifiers.wanted.keySet().iterator();
			while ( i.hasNext() )
			{
				Object key = i.next();
				Iterator li = (Iterator) DebugModifiers.adjustments.get( key );
				if ( li == null )
				{
					DebugModifiers.buffer.append( "<td colspan=2></td>" );
				}
				else
				{
					Change c = (Change) li.next();
					DebugModifiers.buffer.append( c.toString() );
					
					if ( !li.hasNext() )
					{
						DebugModifiers.adjustments.remove( key );
					}
				}
			}
			DebugModifiers.buffer.append( "</tr>" );
		}
		
		DebugModifiers.buffer.append( "</table><br>" );
		RequestLogger.printLine( DebugModifiers.buffer.toString() );
		DebugModifiers.buffer = null;
	}
	
	private static class Change
	implements Comparable
	{
		String name;
		float value;
		boolean variable;
		
		public Change( String name, float value, boolean variable )
		{
			this.name = name;
			this.value = value;
			this.variable = variable;
		}
		
		public String toString()
		{
			return "<td>" + this.name + "</td><td>" +
				KoLConstants.ROUNDED_MODIFIER_FORMAT.format( this.value ) +
				( this.variable? "v" : "" ) + "</td>";
		}
	
		public int compareTo( Object o )
		{
			Change other = (Change) o;
			if ( this.value < other.value ) return 1;
			if ( this.value > other.value ) return -1;
			return this.name.compareTo( other.name );
		}
	}
}
