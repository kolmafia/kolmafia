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
				output.append( defaults.containsKey( pref ) ? defaults.get( pref ) : "N/A" );
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
