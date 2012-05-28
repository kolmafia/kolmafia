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

import java.io.File;

import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public class EditCommand
	extends AbstractCommand
{
	public EditCommand()
	{
		this.usage = " <filename> - launch external editor for a script or map file.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No filename specified." );
			return;
		}
		
		File scriptFile = KoLmafiaCLI.findScriptFile( parameters );

		if ( scriptFile == null )
		{
			scriptFile = new File( UtilityConstants.DATA_LOCATION, parameters );
			if ( !scriptFile.exists() )
			{
				if ( parameters.indexOf( "/" ) != -1 ||
					parameters.indexOf( "\\" ) != -1 )
				{	// Let user explicitly give the top-level directory,
					// as in "edit data/mymap.txt".
					scriptFile = new File( UtilityConstants.ROOT_LOCATION, parameters );
				}
				else
				{	// Assume scripts folder for bare filename
					scriptFile = new File( KoLConstants.SCRIPT_LOCATION, parameters );
				}
			}
		}

		if ( scriptFile.isDirectory() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Can't edit a directory!" );
			return;
		}

		String editor = Preferences.getString( "externalEditor" );
		String path = scriptFile.getAbsolutePath();

		if ( editor.equals( "" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No external editor specified in Preferences." );
			RequestLogger.printLine( "Full path to this file is " + path );
			return;
		}
		
		RequestLogger.printLine( "Launching editor for " + path );
		
		try
		{
			Runtime.getRuntime().exec( new String[] { editor, path } );
		}
		catch ( Exception e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Editor launch failed: " + e );
		}
	}
}
