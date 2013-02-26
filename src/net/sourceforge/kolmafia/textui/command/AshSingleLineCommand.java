/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.io.ByteArrayInputStream;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class AshSingleLineCommand
	extends AbstractCommand
{
	public AshSingleLineCommand()
	{
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
		this.usage = " <statement> - test a line of ASH code without having to edit a script.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !parameters.endsWith( ";" ) && !parameters.endsWith( "}" ) )
		{
			parameters += ";";
		}
		ByteArrayInputStream istream = new ByteArrayInputStream( ( parameters + KoLConstants.LINE_BREAK ).getBytes() );

		Interpreter interpreter = new Interpreter();
		interpreter.validate( null, istream );
		Value rv;

		try
		{
			interpreter.cloneRelayScript( this.interpreter );
			rv = interpreter.execute( "main", null );
		}
		finally
		{
			interpreter.finishRelayScript();
		}

		if ( cmd.endsWith( "q" ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Returned: " + rv );

		rv = Value.asProxy( rv );
		if ( rv instanceof CompositeValue )
		{
			this.dump( (CompositeValue) rv, "" );
		}
	}

	private void dump( final CompositeValue obj, final String indent )
	{
		Value[] keys = obj.keys();
		for ( int i = 0; i < keys.length; ++i )
		{
			Value v = obj.aref( keys[ i ] );
			RequestLogger.printLine( indent + keys[ i ] + " => " + v );
			if ( v instanceof CompositeValue )
			{
				this.dump( (CompositeValue) v, indent + "&nbsp;&nbsp;" );
			}
		}
	}
}
