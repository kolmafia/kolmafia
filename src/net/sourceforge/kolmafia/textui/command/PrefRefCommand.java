/**
+ * Copyright (c) 2005-2015, KoLmafia development team
+ * http://kolmafia.sourceforge.net/
+ * All rights reserved.
+ *
+ * Redistribution and use in source and binary forms, with or without
+ * modification, are permitted provided that the following conditions
+ * are met:
+ *
+ *  [1] Redistributions of source code must retain the above copyright
+ *      notice, this list of conditions and the following disclaimer.
+ *  [2] Redistributions in binary form must reproduce the above copyright
+ *      notice, this list of conditions and the following disclaimer in
+ *      the documentation and/or other materials provided with the
+ *      distribution.
+ *  [3] Neither the name "KoLmafia" nor the names of its contributors may
+ *      be used to endorse or promote products derived from this software
+ *      without specific prior written permission.
+ *
+ * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
+ * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
+ * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
+ * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
+ * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
+ * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
+ * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
+ * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
+ * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
+ * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
+ * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
+ * POSSIBILITY OF SUCH DAMAGE.
+ */

package net.sourceforge.kolmafia.textui.command;

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

		TreeMap<String, String> userPrefs = new TreeMap<String, String>();
		TreeMap<String, String> userDefs = new TreeMap<String, String>();
		TreeMap<String, String> globalPrefs = new TreeMap<String, String>();
		TreeMap<String, String> globalDefs = new TreeMap<String, String>();
		if ( findUserPrefs )
		{
			userPrefs = Preferences.getMap( false, true );
			userDefs = Preferences.getMap( true, true );
		}
		if ( findGlobalPrefs )
		{
			globalPrefs = Preferences.getMap( false, false );
			globalDefs = Preferences.getMap( true, false );
		}

		if ( !isRegEx )
		{
			searchText = searchText.toLowerCase();
		}
		Pattern p = Pattern.compile( searchText );

		output.append( "<table border=2 cols=4>" );
		output.append( "<tr>" );
		output.append( "<th>Name</th>" );
		output.append( "<th>Value</th>" );
		output.append( "<th>Default</th>" );
		output.append( "<th>Scope</th>" );
		output.append( "</tr>" );

		if ( findGlobalPrefs )
		{
			for ( String pref : globalPrefs.keySet() )
			{
				if ( !Preferences.isUserEditable( pref ) )
				{
					continue;
				}
				if ( isRegEx ? p.matcher( pref ).find() : pref.toLowerCase().contains( searchText ) )
				{
					output.append( "<tr><td>" );
					output.append( pref );
					output.append( "</td><td><p>" );
					output.append( globalPrefs.get( pref ) );
					output.append( "</p></td><td><p>" );
					output.append( globalDefs.containsKey( pref ) ? globalDefs.get( pref ) : "N/A" );
					output.append( "</p></td><td>" );
					output.append( "global" );
					output.append( "</td></tr>" );
				}
			}
		}

		if ( findUserPrefs )
		{
			for ( String pref : userPrefs.keySet() )
			{
				// This might be needed eventually, but for now all non user-editable settings are global
				/*if ( !Preferences.isUserEditable( pref ) )
				{
					continue;
				}
				*/
				if ( isRegEx ? p.matcher( pref ).find() : pref.toLowerCase().contains( searchText ) )
				{
					output.append( "<tr><td>" );
					output.append( pref );
					output.append( "</td><td><p>" );
					output.append( userPrefs.get( pref ) );
					output.append( "</p></td><td><p>" );
					output.append( userDefs.containsKey( pref ) ? userDefs.get( pref ) : "N/A" );
					output.append( "</p></td><td>" );
					output.append( "user" );
					output.append( "</td></tr>" );
				}
			}
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
