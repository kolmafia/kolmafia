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

import java.util.Iterator;

import net.sourceforge.kolmafia.textui.Parser;

public class Scope
	extends BasicScope
{
	private ParseTreeNodeList commands;
	private int barrier = BasicScope.BARRIER_NONE;
	private boolean breakable = false;

	public Scope( VariableList variables, final BasicScope parentScope )
	{
		super( variables, parentScope );
		this.commands = new ParseTreeNodeList();
	}

	public Scope( final BasicScope parentScope )
	{
		super( parentScope );
		this.commands = new ParseTreeNodeList();
	}

	public Scope( final ParseTreeNode command, final BasicScope parentScope )
	{
		super( parentScope );
		this.commands = new ParseTreeNodeList();
		this.commands.add( command );
		this.barrier = command.assertBarrier() ? BasicScope.BARRIER_SEEN : 0;
		this.breakable = command.assertBreakable();
	}

	public Scope( FunctionList functions, VariableList variables, TypeList types )
	{
		super( functions, variables, types, null );
		this.commands = new ParseTreeNodeList();
	}

	@Override
	public void addCommand( final ParseTreeNode c, final Parser p )
	{
		if ( c == null )
		{
			// We shouldn't be called with no command, but don't do
			// the wrong thing with one.
			return;
		}

		this.commands.add( c );

		if ( this.barrier == BasicScope.BARRIER_NONE &&
			c.assertBarrier() )
		{
			this.barrier = BasicScope.BARRIER_SEEN;
		}
		else if ( this.barrier == BasicScope.BARRIER_SEEN &&
			!(c instanceof FunctionReturn) )
		{	// A return statement after a barrier is temporarily allowed,
			// since they were previously required in some cases that didn't
			// really need them.
			this.barrier = BasicScope.BARRIER_PAST;
			p.warning( "Unreachable code" );
		}
		
		if ( !this.breakable )
		{
			this.breakable = c.assertBreakable();
		}
	}

	@Override
	public Iterator getCommands()
	{
		return this.commands.iterator();
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
}

