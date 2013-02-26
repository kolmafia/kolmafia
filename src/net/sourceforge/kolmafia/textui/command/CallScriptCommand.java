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

import java.io.File;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Profiler;

import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CallScriptCommand
	extends AbstractCommand
{
	private static final Pattern ASHNAME_PATTERN = Pattern.compile( "\\.ash", Pattern.CASE_INSENSITIVE );

	public CallScriptCommand()
	{
		this.usage = " [<number>x] <filename> | <function> [<parameters>] - check/run script.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		CallScriptCommand.call( command, parameters, this.interpreter );
	}

	public static void call( final String command, String parameters, Interpreter caller )
	{
		try
		{
			int runCount = 1;
			String[] arguments = null;

			parameters = parameters.trim();
			File scriptFile = KoLmafiaCLI.findScriptFile( parameters );

			// If still no script was found, perhaps it's the
			// secret invocation of the "#x script" that allows a
			// script to be run multiple times.

			if ( scriptFile == null )
			{
				String runCountString = parameters.split( " " )[ 0 ];
				boolean hasMultipleRuns = runCountString.endsWith( "x" );

				for ( int i = 0; i < runCountString.length() - 1 && hasMultipleRuns; ++i )
				{
					hasMultipleRuns = Character.isDigit( runCountString.charAt( i ) );
				}

				if ( hasMultipleRuns )
				{
					runCount = StringUtilities.parseInt( runCountString );
					//Fixes StringIndexOutOfBoundsException error when "x" is entered
					//as a command.  This may be addressing the symptom and not the
					//cause and but should not break x as a "repeat indicator".
					if (runCount <= 0) {
						return;
					}
					parameters = parameters.substring( parameters.indexOf( " " ) ).trim();
					scriptFile = KoLmafiaCLI.findScriptFile( parameters );
				}
			}

			// Maybe the more ambiguous invocation of an ASH script
			// which does not use parentheses?

			if ( scriptFile == null )
			{
				int spaceIndex = parameters.indexOf( " " );
				if ( spaceIndex != -1 )
				{
					arguments = new String[]
					{ parameters.substring( spaceIndex + 1 ).trim()
					};
					parameters = parameters.substring( 0, spaceIndex );
					scriptFile = KoLmafiaCLI.findScriptFile( parameters );
				}
			}

			// If not even that, perhaps it's the invocation of a
			// function which is defined in the ASH namespace?

			if ( scriptFile == null )
			{
				Value rv = KoLmafiaASH.NAMESPACE_INTERPRETER.execute( parameters, arguments );
				// A script only has a meaningful return value
				// if it succeeded.
				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( "Returned: " + rv );
				}
				return;
			}

			// In theory, you could execute EVERY script in a
			// directory, but instead, let's make it an error.

			if ( scriptFile.isDirectory() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, scriptFile.getCanonicalPath() + " is a directory." );
				return;
			}

			// Got here so have valid script file name and not a directory.

			if ( Preferences.getInteger( "scriptMRULength" ) > 0 )
			{
				// Add name, without path, to MRU list
				KoLConstants.scriptMList.addItem( scriptFile.getName() );
				GenericFrame.compileScripts( true );
			}

			// Allow the ".ash" to appear anywhere in the filename
			// in a case-insensitive manner.

			if ( CallScriptCommand.ASHNAME_PATTERN.matcher( scriptFile.getPath() ).find() )
			{
				// If there's an alternate namespace being
				// used, then be sure to switch.

				if ( command.equals( "validate" ) || command.equals( "verify" ) || command.equals( "check" ) )
				{
					Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFile );
					if ( interpreter != null )
					{
						RequestLogger.printLine();
						KoLmafiaASH.showUserFunctions( interpreter, "" );

						RequestLogger.printLine();
						RequestLogger.printLine( "Script verification complete." );
					}

					return;
				}

				if ( command.equals( "profile" ) )
				{
					Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFile );
					if ( interpreter != null )
					{
						Profiler prof = Profiler.create( "toplevel" );
						long t0 = System.nanoTime();
						prof.net0 = t0;
						interpreter.profiler = prof;

						for ( int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i )
						{
							interpreter.execute( "main", arguments );
						}

						long t1 = System.nanoTime();
						prof.total = t1 - t0;
						prof.net += t1 - prof.net0;
						prof.finish();
						interpreter.profiler = null;
						RequestLogger.printLine( Profiler.summary() );
					}
					return;
				}
				
				// If there's an alternate namespace being
				// used, then be sure to switch.

				Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFile );
				if ( interpreter != null )
				{
					try
					{
						interpreter.cloneRelayScript( caller );
						for ( int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i )
						{
							interpreter.execute( "main", arguments );
						}
					}
					finally
					{
						interpreter.finishRelayScript();
					}
				}
			}
			else
			{
				if ( arguments != null )
				{
					KoLmafia.updateDisplay(
						MafiaState.ERROR, "You can only specify arguments for an ASH script" );
					return;
				}

				for ( int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i )
				{
					new KoLmafiaCLI( DataUtilities.getInputStream( scriptFile ) ).listenForCommands();
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return;
		}
	}
}
