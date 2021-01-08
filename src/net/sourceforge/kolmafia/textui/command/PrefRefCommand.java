/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public class PrefRefCommand
	extends AbstractCommand
{
	public PrefRefCommand()
	{
		this.usage = " <searchText> [regex] [user|global] - Search and list preferences";
	}

	private void dumpPrefs( StringBuilder output, Pattern p, String searchText,
				String type, Map<String,String> prefs, Map<String,String> defaults )
	{
		for ( String pref : prefs.keySet() )
		{
			if ( !Preferences.isUserEditable( pref ) )
			{
				continue;
			}

			if ( p != null ? p.matcher( pref ).find() : pref.toLowerCase().contains( searchText ) )
			{
				output.append( "<tr><td><p>" );
				output.append( pref );
				output.append( "</p></td><td><p>" );
				output.append( prefs.get( pref ) );
				output.append( "</p></td><td><p>" );
				output.append( defaults.getOrDefault( pref, "N/A" ) );
				output.append( "</p></td><td>" );
				output.append( type );
				output.append( "</td></tr>" );
			}
		}
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		StringBuilder output = new StringBuilder();

		int pos = 0;
		String searchText = "";
		boolean isRegEx = false;
		boolean findUserPrefs = false;
		boolean findGlobalPrefs = false;

		for ( String param : parameters.split( " " ) )
		{
			if ( pos == 0 )
			{
				searchText = param;
			}
			else if ( param.equals( "regex" ) )
			{
				isRegEx = true;
			}
			else if ( param.equals( "user" ) )
			{
				findUserPrefs = true;
			}
			else if ( param.equals( "global" ) )
			{
				findGlobalPrefs = true;
			}
			pos++;
		}

		if ( !findUserPrefs && !findGlobalPrefs )
		{
			findUserPrefs = true;
			findGlobalPrefs = true;
		}

		Pattern p = null;

		if ( isRegEx )
		{
			p = Pattern.compile( searchText );
		}
		else
		{
			searchText = searchText.toLowerCase();
		}

		output.append( "<table border=2 cols=4>" );
		output.append( "<tr>" );
		output.append( "<th>Name</th>" );
		output.append( "<th>Value</th>" );
		output.append( "<th>Default</th>" );
		output.append( "<th>Scope</th>" );
		output.append( "</tr>" );

		if ( findGlobalPrefs )
		{
			TreeMap<String, String> globalPrefs = Preferences.getMap( false, false );
			TreeMap<String, String> globalDefs = Preferences.getMap( true, false );
			dumpPrefs( output, p, searchText, "global", globalPrefs, globalDefs );
		}

		if ( findUserPrefs )
		{
			TreeMap<String, String> userPrefs = Preferences.getMap( false, true );
			TreeMap<String, String> userDefs = Preferences.getMap( true, true );
			dumpPrefs( output, p, searchText, "user", userPrefs, userDefs );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
