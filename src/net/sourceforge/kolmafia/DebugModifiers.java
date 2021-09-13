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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;


public class DebugModifiers
	extends Modifiers
{
	private static HashMap<Integer, String> wanted, adjustments;
	private static String currentType;
	private static String currentDesc;
	private static StringBuffer buffer;

	public static int setup( String parameters )
	{
		DebugModifiers.wanted = new HashMap<>();
		DebugModifiers.adjustments = new HashMap<>();
		for ( int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i )
		{
			String name = Modifiers.getModifierName( i );
			if ( name.toLowerCase().indexOf( parameters ) != -1 )
			{
				DebugModifiers.wanted.put( IntegerPool.get( i ),
					"<td colspan=3>" + name + "</td>" );
				DebugModifiers.adjustments.put( IntegerPool.get( i ),
					"<td colspan=2>" + name + "</td>" );
			}
		}
		DebugModifiers.currentType = "type";
		DebugModifiers.currentDesc = "source";
		DebugModifiers.buffer = new StringBuffer( "<table border=2>" );
		return DebugModifiers.wanted.size();
	}
	
	private static void flushRow()
	{
		DebugModifiers.buffer.append( "<tr><td>" );
		DebugModifiers.buffer.append( DebugModifiers.currentType );
		DebugModifiers.buffer.append( "</td><td>" );
		if ( DebugModifiers.currentType.equals( "Item" ) )
		{
			DebugModifiers.buffer.append( ItemDatabase.getItemDisplayName( DebugModifiers.currentDesc ) );
		}
		else if ( DebugModifiers.currentType.equals( "Effect" ) )
		{
			DebugModifiers.buffer.append( EffectDatabase.getEffectDisplayName( DebugModifiers.currentDesc ) );
		}
		else
		{
			DebugModifiers.buffer.append( DebugModifiers.currentDesc );
		}
		DebugModifiers.buffer.append( "</td>" );
		Iterator<Integer> i = DebugModifiers.wanted.keySet().iterator();
		while ( i.hasNext() )
		{
			Integer key = i.next();
			String item = DebugModifiers.adjustments.get( key );
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
	
	@Override
	public void add( final int index, final double mod, final String desc )
	{
		if ( index < 0 || index >= Modifiers.DOUBLE_MODIFIERS || mod == 0.0 )
		{
			return;
		}
		
		super.add( index, mod, desc );
		
		Integer key = IntegerPool.get( index );
		if ( ! DebugModifiers.wanted.containsKey( key ) )
		{
			return;
		}
		
		String lookup = desc;
		String type = null;
		String name = null;
		int ind = lookup.indexOf( ":" );
		if ( ind > 0 )
		{
			type = lookup.substring( 0, ind );
			name = lookup.replace( type + ":", "" );
		}
		else
		{
			type = "";
			name = desc;
		}
		if ( !desc.equals( DebugModifiers.currentDesc ) ||
			DebugModifiers.adjustments.containsKey( key ) )
		{
			DebugModifiers.flushRow();
		}
		DebugModifiers.currentType = type;
		DebugModifiers.currentDesc = name;
		DebugModifiers.adjustments.put( key, "<td>" +
			KoLConstants.ROUNDED_MODIFIER_FORMAT.format( mod ) + "</td><td>=&nbsp;" +
			KoLConstants.ROUNDED_MODIFIER_FORMAT.format( this.get( index ) ) + "</td>" );
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
		Iterator<Integer> i = DebugModifiers.wanted.keySet().iterator();
		HashMap<Integer, Iterator<Change>> modifiersChangers = new HashMap<>();
		while ( i.hasNext() )
		{
			Integer key = i.next();
			String modifier = DebugModifiers.wanted.get( key );
			DebugModifiers.buffer.append( modifier );
			ArrayList<Change> modChangers = new ArrayList<>();
			Iterator<String> allmods = Modifiers.getAllModifiers();
			while ( allmods.hasNext() )
			{
				String lookup = allmods.next();
				String type = null;
				String name = null;
				int ind = lookup.indexOf( ":" );
				if ( ind > 0 )
				{
					type = lookup.substring( 0, ind );
					name = lookup.replace( type + ":", "" );
				}
				else
				{
					type = "";
					name = lookup;
				}
				Modifiers mods = Modifiers.getModifiers( type, name );
				if ( mods == null )
				{
					continue;
				}
				double value = mods.get( key );
				if ( value != 0.0 )
				{
					modChangers.add( new Change( type, name, value, mods.variable ) );
				}
			}
			if ( modChangers.size() > 0 )
			{
				Collections.sort( modChangers );
				modifiersChangers.put( key, modChangers.iterator() );
			}
		}
		DebugModifiers.buffer.append( "</tr>" );
		while ( modifiersChangers.size() > 0 )
		{
			DebugModifiers.buffer.append( "<tr>" );
			i = DebugModifiers.wanted.keySet().iterator();
			while ( i.hasNext() )
			{
				Integer key = i.next();
				Iterator<Change> li = modifiersChangers.get( key );
				if ( li == null )
				{
					DebugModifiers.buffer.append( "<td colspan=3></td>" );
				}
				else
				{
					Change c = li.next();
					DebugModifiers.buffer.append( c.toString() );
					
					if ( !li.hasNext() )
					{
						modifiersChangers.remove( key );
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
		implements Comparable<Change>
	{
		String type;
		String name;
		double value;
		boolean variable;
		
		public Change( String type, String name, double value, boolean variable )
		{
			this.type = type;
			this.name = name;
			this.value = value;
			this.variable = variable;
		}
		
		@Override
		public String toString()
		{
			if ( this.type.equals( "Item" ) )
			{
				return "<td>Item</td><td>" + ItemDatabase.getItemDisplayName( this.name ) + "</td><td>" +
					KoLConstants.ROUNDED_MODIFIER_FORMAT.format( this.value ) +
					( this.variable? "v" : "" ) + "</td>";
			}
			if ( this.type.equals( "Effect" ) )
			{
				return "<td>Effect</td><td>" + EffectDatabase.getEffectDisplayName( this.name ) + "</td><td>" +
					KoLConstants.ROUNDED_MODIFIER_FORMAT.format( this.value ) +
					( this.variable? "v" : "" ) + "</td>";
			}
			return "<td>" + this.type + "</td><td>" + this.name + "</td><td>" +
				KoLConstants.ROUNDED_MODIFIER_FORMAT.format( this.value ) +
				( this.variable? "v" : "" ) + "</td>";
		}
	
		public int compareTo( Change o )
		{
			Change other = o;
			if ( this.value < other.value ) return 1;
			if ( this.value > other.value ) return -1;
			return this.name.compareTo( other.name );
		}
	}
}
