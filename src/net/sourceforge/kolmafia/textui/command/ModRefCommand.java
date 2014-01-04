/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

public class ModRefCommand
	extends AbstractCommand
{
	public ModRefCommand()
	{
		this.usage = " [<object>] - list all modifiers, show values for player [and object].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		Modifiers mods = Modifiers.getModifiers( parameters );
		StringBuffer buf = new StringBuffer( "<table border=2>" + "<tr><td colspan=2>NUMERIC MODIFIERS</td></tr>" );
		String mod;
		int i = 0;
		while ( ( mod = Modifiers.getModifierName( i++ ) ) != null )
		{
			buf.append( "<tr><td>" );
			buf.append( mod );
			buf.append( "</td><td>" );
			buf.append( KoLCharacter.currentNumericModifier( mod ) );
			if ( mods != null )
			{
				buf.append( "</td><td>" );
				buf.append( mods.get( mod ) );
			}
			buf.append( "</td></tr>" );
		}
		buf.append( "<tr><td colspan=2>BITMAP MODIFIERS</td></tr>" );
		i = 1;
		while ( ( mod = Modifiers.getBitmapModifierName( i++ ) ) != null )
		{
			buf.append( "<tr><td>" );
			buf.append( mod );
			buf.append( "</td><td>0x" );
			buf.append( Integer.toHexString( KoLCharacter.currentRawBitmapModifier( mod ) ) );
			buf.append( " (" );
			buf.append( KoLCharacter.currentBitmapModifier( mod ) );
			buf.append( ")" );
			if ( mods != null )
			{
				buf.append( "</td><td>0x" );
				buf.append( Integer.toHexString( mods.getRawBitmap( mod ) ) );
				buf.append( " (" );
				buf.append( mods.getBitmap( mod ) );
				buf.append( ")" );
			}
			buf.append( "</td></tr>" );
		}
		buf.append( "<tr><td colspan=2>BOOLEAN MODIFIERS</td></tr>" );
		i = 0;
		while ( ( mod = Modifiers.getBooleanModifierName( i++ ) ) != null )
		{
			buf.append( "<tr><td>" );
			buf.append( mod );
			buf.append( "</td><td>" );
			buf.append( KoLCharacter.currentBooleanModifier( mod ) );
			if ( mods != null )
			{
				buf.append( "</td><td>" );
				buf.append( mods.getBoolean( mod ) );
			}
			buf.append( "</td></tr>" );
		}
		buf.append( "<tr><td colspan=2>STRING MODIFIERS</td></tr>" );
		i = 0;
		while ( ( mod = Modifiers.getStringModifierName( i++ ) ) != null )
		{
			buf.append( "<tr><td>" );
			buf.append( mod );
			buf.append( "</td><td>" );
			buf.append( KoLCharacter.currentStringModifier( mod ).replaceAll( "\t", "<br>" ) );
			if ( mods != null )
			{
				buf.append( "</td><td>" );
				buf.append( mods.getString( mod ) );
			}
			buf.append( "</td></tr>" );
		}
		buf.append( "</table><br>" );
		RequestLogger.printLine( buf.toString() );
	}
}
