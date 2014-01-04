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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class SwitchScope
	extends BasicScope
{
	private ArrayList commands = new ArrayList();
	private int offset = -1;
	private int barrier = BasicScope.BARRIER_SEEN;
	private boolean breakable = false;

	public SwitchScope( final BasicScope parentScope )
	{
		super( parentScope );
	}

	@Override
	public void addCommand( final ParseTreeNode c, final Parser p )
	{
		this.commands.add( c );
		if ( this.barrier == BasicScope.BARRIER_NONE &&
			c.assertBarrier() )
		{
			this.barrier = BasicScope.BARRIER_SEEN;
		}
		else if ( this.barrier == BasicScope.BARRIER_SEEN )
		{
			this.barrier = BasicScope.BARRIER_PAST;
			p.warning( "Unreachable code" );
		}
		
		if ( !this.breakable )
		{
			this.breakable = c.assertBreakable();
		}
	}
	
	public void resetBarrier()
	{
		this.barrier = BasicScope.BARRIER_NONE;
	}

	@Override
	public Iterator getCommands()
	{
		return this.commands.listIterator( this.offset );
	}

	public int commandCount()
	{
		return this.commands.size();
	}

	public void setOffset( final int offset )
	{
		this.offset = offset;
	}

	@Override
	public boolean assertBarrier()
	{
		return this.barrier >= BasicScope.BARRIER_SEEN;
	}
	
	@Override
	public boolean assertBreakable()
	{
		return this.breakable;
	}

	public void print( final PrintStream stream, final int indent, Value [] tests, Integer [] offsets, int defaultIndex )
	{
		Iterator it;

		Interpreter.indentLine( stream, indent );
		stream.println( "<SCOPE>" );

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<VARIABLES>" );

		it = this.getVariables();
		while ( it.hasNext() )
		{
			Variable currentVar = (Variable) it.next();
			currentVar.print( stream, indent + 2 );
		}

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<COMMANDS>" );

		int commandCount = this.commands.size();
		int testIndex = 0;
		int testCount = tests.length;

		for ( int index = 0; index < commandCount; ++index )
		{
			while ( testIndex < testCount )
			{
				Value test = tests[testIndex];
				Integer offset = offsets[testIndex];
				if ( offset.intValue() != index )
				{
					break;
				}

				Interpreter.indentLine( stream, indent + 1 );
				stream.println( "<CASE>" );
				test.print( stream, indent + 2 );
				testIndex++;
			}

                        if ( defaultIndex == index )
                        {
                                Interpreter.indentLine( stream, indent + 1 );
                                stream.println( "<DEFAULT>" );
                        }

			ParseTreeNode command = (ParseTreeNode)commands.get( index );
			command.print( stream, indent + 2 );
		}
	}
}

