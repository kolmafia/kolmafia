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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.Speculation;

public class SpeculateCommand
	extends AbstractCommand
{
	public SpeculateCommand()
	{
		this.usage =
			" MCD <num> | equip [<slot>] <item> | unequip <slot> | familiar <type> | enthrone <type> | up <eff> | uneffect <eff> | quiet ; [<another>;...] - predict modifiers.";

		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		Speculation spec = new Speculation();
		boolean quiet = spec.parse( parameters );
		Modifiers mods = spec.calculate();
		Modifiers.overrideModifier( "_spec", mods );
		if ( quiet )
		{
			return;
		}
		String table = SpeculateCommand.getHTML( mods, "" );
		if ( table != null )
		{
			RequestLogger.printLine( table + "<br>" );
		}
		else
		{
			RequestLogger.printLine( "No modifiers changed." );
		}
	}
		
	public static String getHTML( Modifiers mods, String attribs )
	{	
		StringBuffer buf = new StringBuffer( "<table border=2 " );
		buf.append( attribs );
		buf.append( ">" );
		int len = buf.length();
		String mod;
		int i = 0;
		while ( ( mod = Modifiers.getModifierName( i++ ) ) != null )
		{
			doNumeric( mod, mods, buf );
		}
		i = 0;
		while ( ( mod = Modifiers.getDerivedModifierName( i++ ) ) != null )
		{
			doNumeric( mod, mods, buf );
		}
		i = 1;
		while ( ( mod = Modifiers.getBitmapModifierName( i++ ) ) != null )
		{
			doNumeric( mod, mods, buf );
		}
		i = 0;
		while ( ( mod = Modifiers.getBooleanModifierName( i++ ) ) != null )
		{
			boolean was = KoLCharacter.currentBooleanModifier( mod );
			boolean now = mods.getBoolean( mod );
			if ( now == was )
			{
				continue;
			}
			buf.append( "<tr><td>" );
			buf.append( mod );
			buf.append( "</td><td>" );
			buf.append( now );
			buf.append( "</td></tr>" );
		}
		i = 0;
		while ( ( mod = Modifiers.getStringModifierName( i++ ) ) != null )
		{
			String was = KoLCharacter.currentStringModifier( mod );
			String now = mods.getString( mod );
			if ( now.equals( was ) )
			{
				continue;
			}
			if ( was.equals( "" ) )
			{
				buf.append( "<tr><td>" );
				buf.append( mod );
				buf.append( "</td><td>" );
				buf.append( now.replaceAll( "\t", "<br>" ) );
				buf.append( "</td></tr>" );
			}
			else
			{
				buf.append( "<tr><td rowspan=2>" );
				buf.append( mod );
				buf.append( "</td><td>" );
				buf.append( was.replaceAll( "\t", "<br>" ) );
				buf.append( "</td></tr><tr><td>" );
				buf.append( now.replaceAll( "\t", "<br>" ) );
				buf.append( "</td></tr>" );
			}
		}
		if ( buf.length() > len )
		{
			buf.append( "</table>" );
			return buf.toString();
		}
		return null;
	}

	private static void doNumeric( final String mod, final Modifiers mods, final StringBuffer buf )
	{
		double was = KoLCharacter.currentNumericModifier( mod );
		double now = mods.get( mod );
		if ( now == was )
		{
			return;
		}
		buf.append( "<tr><td>" );
		buf.append( mod );
		buf.append( "</td><td>" );
		buf.append( KoLConstants.FLOAT_FORMAT.format( now ) );
		buf.append( " (" );
		if ( now > was )
		{
			buf.append( "+" );
		}
		buf.append( KoLConstants.FLOAT_FORMAT.format( now - was ) );
		buf.append( ")</td></tr>" );
	}
}
