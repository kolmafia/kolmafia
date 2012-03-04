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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.textui.Interpreter;

public abstract class ParseTreeNode
{
	public abstract Value execute( final Interpreter interpreter );
	public abstract void print( final PrintStream stream, final int indent );
	
	// A barrier is any code construct that is fundamentally incapable of
	// completing normally; any subsequent code in the same scope is
	// therefore dead code (exception: case labels can make code live again).
	// The basic barriers are the RETURN, EXIT, BREAK, and CONTINUE statements
	// (THROW would also be a barrier if we had something like that).
	// Any compound statement that unavoidably hits a barrier is itself a
	// barrier: IF/ELSEIF/ELSE with barriers in all branches; SWITCH with no
	// breaks and a barrier at the end; TRY with a barrier in either the code
	// block or the FINALLY clause; and infinite loops with no break.
	// Note that most looping constructs cannot be barriers, due to the
	// possibility of the loop executing zero times and just falling thru.
	// Note that function calls cannot be a barrier, not even abort(), due
	// to the existence of the "disable" CLI command.  The code currently
	// assumes that no expression can be a barrier.
	// Note that BREAK and CONTINUE are disallowed outside of loops, so the
	// presence of a barrier in a non-void function's body is exactly
	// equivalent to the requirement that it not return without a return value.
	public boolean assertBarrier()
	{
		return false;
	}
	
	// A breakable code construct is a BREAK statement, or any compound
	// statement that can possibly execute a BREAK that isn't caught internally.
	// Looping statements are therefore not considered breakable, since they
	// would handle the break themselves.
	public boolean assertBreakable()
	{
		return false;
	}
	
	// There is no need for a corresponding check for CONTINUE statements.
	// Since they can only branch back to already-executed code, they have
	// no effect on code reachability.
}
