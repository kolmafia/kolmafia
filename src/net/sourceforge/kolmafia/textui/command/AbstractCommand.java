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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.PrefixMap;

public abstract class AbstractCommand
{
	// Assign 'flags' in an instance initializer if the command needs one
	// of these:
	// KoLmafiaCLI.FULL_LINE_CMD - the command's parameters are the entire
	//	remainder of the line, semicolons do not end the command.
	// KoLmafiaCLI.FLOW_CONTROL_CMD - the remainder of the command line,
	//	plus additional lines as needed to ensure that at least one
	//	command is included, and that the final command is not itself
	//	flagged as FLOW_CONTROL_CMD, are made available to this command
	//	via its 'continuation' field, rather than being executed.  The
	//	command can examine and modify the continuation, and execute it
	//	zero or more times by calling CLI.executeLine(continuation).

	public int flags = 0;

	// Assign 'usage' in an instance initializer to set the help usage text.
	// If usage is null, this command won't be shown in the command list.

	public String usage = " - no help available.";

	// Usage strings should start with a space, or [?] if they support the
	// isExecutingCheckOnlyCommand flag, followed by any parameters (with
	// placeholder names enclosed in angle brackets - they'll be italicized
	// in HTML output).
	// There should then be a dash and a brief description of the command.
	// Or, override getUsage(cmd) to dynamically construct the usage text
	// (but it would probably be better to have separate commands in that
	// case).

	public String getUsage( final String cmd )
	{
		return this.usage;
	}

	// If the command is being called from an ASH Interpreter, here is
	// where it will be.

	public Interpreter interpreter = null;

	// Override one of run(cmd, parameters), run(cmd, parameters[]), or
	// run(cmd) to specify the command's action, with different levels of
	// parameter processing.

	public abstract void run( final String cmd, final String parameters );

	// 'CLI' is provided as a reference back to the invoking instance of
	// KoLmafiaCLI, for convenience if the command needs to call any of its
	// non-static methods.
	// Note that this reference can become invalid if another CLI instance
	// is recursively invoked, and happens to execute the same command; any
	// command that uses 'CLI' more than once should put it in a local
	// variable first.

	public KoLmafiaCLI CLI;

	// FLOW_CONTROL_CMDs will have the command line they're to operate on
	// stored here:

	public String continuation;

	// Each command class must be instantiated (probably in a static
	// initializer), and at least one of these methods called on it to add
	// it to the command table.  These methods return 'this', for easy
	// chaining.

	public AbstractCommand register( final String name )
	{
		// For commands that must be typed with an exact name
		AbstractCommand.lookup.putExact( name.toLowerCase(), this );
		this.registerFlags( name );
		return this;
	}

	public AbstractCommand registerPrefix( final String prefix )
	{
		// For commands that are parsed as startsWith(...)
		AbstractCommand.lookup.putPrefix( prefix.toLowerCase(), this );
		this.registerFlags( prefix );
		return this;
	}

	public AbstractCommand registerSubstring( String substring )
	{
		// For commands that are parsed as indexOf(...)!=-1.  Use sparingly!
		substring = substring.toLowerCase();
		AbstractCommand.substringLookup.add( substring );
		AbstractCommand.substringLookup.add( this );

		// Make it visible in the normal lookup map:
		AbstractCommand.lookup.putExact( "*" + substring + "*", this );
		this.registerFlags( substring );
		return this;
	}

	// Internal implementation thingies:

	public static final PrefixMap lookup = new PrefixMap();
	public static final ArrayList substringLookup = new ArrayList();
	public static String fullLineCmds = "";
	public static String flowControlCmds = "";

	public static AbstractCommand getSubstringMatch( final String cmd )
	{
		for ( int i = 0; i < AbstractCommand.substringLookup.size(); i += 2 )
		{
			if ( cmd.indexOf( (String) AbstractCommand.substringLookup.get( i ) ) != -1 )
			{
				return (AbstractCommand) AbstractCommand.substringLookup.get( i + 1 );
			}
		}
		return null;
	}

	private void registerFlags( final String name )
	{
		if ( this.flags == KoLmafiaCLI.FULL_LINE_CMD )
		{
			AbstractCommand.fullLineCmds += AbstractCommand.fullLineCmds.length() == 0 ? name : ", " + name;
		}
		if ( this.flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
		{
			AbstractCommand.flowControlCmds += AbstractCommand.flowControlCmds.length() == 0 ? name : ", " + name;
		}
	}

	protected static String[] splitCountAndName( final String parameters )
	{
		String nameString;
		String countString;

		if ( parameters.startsWith( "\"" ) )
		{
			nameString = parameters.substring( 1, parameters.length() - 1 );
			countString = null;
		}
		else if ( parameters.startsWith( "*" ) || parameters.indexOf( " " ) != -1 && Character.isDigit( parameters.charAt( 0 ) ) )
		{
			countString = parameters.split( " " )[ 0 ];
			String rest = parameters.substring( countString.length() ).trim();

			if ( rest.startsWith( "\"" ) )
			{
				nameString = rest.substring( 1, rest.length() - 1 );
			}
			else
			{
				nameString = rest;
			}
		}
		else
		{
			nameString = parameters;
			countString = null;
		}

		return new String[]
		{
			countString,
			nameString
		};
	}

	protected static final AdventureResult itemParameter( final String parameter )
	{
		List potentialItems = ItemDatabase.getMatchingNames( parameter );
		if ( potentialItems.isEmpty() )
		{
			return null;
		}

		return new AdventureResult( (String) potentialItems.get( 0 ), 0, false );
	}

	protected static final AdventureResult effectParameter( final String parameter )
	{
		List potentialEffects = EffectDatabase.getMatchingNames( parameter );
		if ( potentialEffects.isEmpty() )
		{
			return null;
		}

		return new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
	}
}
