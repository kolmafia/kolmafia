/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.util.HashMap;
import java.util.Iterator;

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
				DebugModifiers.wanted.put( new Integer( i ),
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
		
		Integer key = new Integer( index );
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
		DebugModifiers.buffer.append( "</table>" );
		RequestLogger.printLine( DebugModifiers.buffer.toString() );
		RequestLogger.printLine();
		DebugModifiers.buffer = null;
	}
}
