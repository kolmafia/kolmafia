/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

package net.sourceforge.kolmafia.textui;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

public class Python {
    public static void execute( final String file )
    {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName( "python" );

        if ( engine == null )
        {
    		KoLmafia.updateDisplay( MafiaState.ERROR, "Experimental feature! Add Jython to your classpath to run Python files" );
			return;
        }

        try
        {
            FileReader f = new FileReader( file );

            engine.eval( "import sys; sys.path.append(\"" + KoLConstants.SCRIPT_LOCATION + "\")" );
            engine.eval( f );

            Invocable invocable = (Invocable) engine;
            Object result = invocable.invokeFunction( "main" );
            
            if ( result != null )
            {
                KoLmafia.updateDisplay( MafiaState.CONTINUE, "Returned: " + result.toString() );
            }
        }
        catch ( NoSuchMethodException e )
        {
            KoLmafia.updateDisplay( MafiaState.ERROR, "Python scripts must specify a main() function" );
            return;          
        }
        catch ( FileNotFoundException e )
        {
            KoLmafia.updateDisplay( MafiaState.ERROR, "File not found" );
            return;
        }
        catch ( ScriptException e )
        {
            KoLmafia.updateDisplay( MafiaState.ERROR, e.getMessage() );
            return;
        }
    }
}