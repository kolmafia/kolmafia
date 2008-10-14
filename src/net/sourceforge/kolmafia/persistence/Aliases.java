package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Aliases
{
	private static final File ALIAS_FILE = new File( UtilityConstants.SETTINGS_LOCATION, "GLOBAL_aliases.txt" );
	private static final TreeMap aliasMap = new TreeMap();

	private static boolean loadedAliases = false;
	private static Set aliasSet = null;

	private static void load()
	{
		if ( loadedAliases )
		{
			return;
		}

		File oldAliasFile = new File( UtilityConstants.SETTINGS_LOCATION, "aliases_GLOBAL.txt" );
		if ( oldAliasFile.exists() )
		{
			oldAliasFile.renameTo( Aliases.ALIAS_FILE );
		}

		Aliases.aliasMap.clear();

		BufferedReader reader = FileUtilities.getReader( Aliases.ALIAS_FILE );
		if ( reader != null )
		{
			String[] data;

			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				if ( data.length >= 2 )
				{
					Aliases.aliasMap.put( " " + data[ 0 ] + " ", " " + data[ 1 ] + " " );
				}
			}

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}

		Aliases.aliasSet = Aliases.aliasMap.entrySet();
		loadedAliases = true;
	}

	public static void save()
	{
		PrintStream aliasStream = LogStream.openStream( Aliases.ALIAS_FILE, true );

		Iterator it = Aliases.aliasSet.iterator();
		while ( it.hasNext() )
		{
			Entry current = (Entry) it.next();
			String aliasString = (String) current.getKey();
			String aliasCommand = (String) current.getValue();
			aliasStream.println( aliasString.trim() + "\t" + aliasCommand.trim() );
		}

		aliasStream.close();
	}

	public static String apply( String line )
	{
		Aliases.load();

		if ( Aliases.aliasSet.size() == 0 )
		{
			return line;
		}

		if ( line.startsWith( "alias " ) || line.startsWith( "unalias " ) )
		{
			return line;
		}

		line = " " + line + " ";

		Iterator it = Aliases.aliasSet.iterator();
		while ( it.hasNext() )
		{
			Entry current = (Entry) it.next();
			String aliasString = (String) current.getKey();
			String aliasCommand = (String) current.getValue();

			// If the alias has a "%%" that means the person is using the old
			// aliasing scheme where only the first word can be considered a
			// part of the alias.

			if ( aliasCommand.indexOf( "%%" ) != -1 )
			{
				if ( line.startsWith( aliasString ) )
				{
					String parameters = line.substring( aliasString.length() );
					line = StringUtilities.globalStringReplace( aliasCommand, "%%", parameters );
				}
			}

			// Otherwise, it's likely that this is a newer style alias where
			// we just expand a word or phrase.

			else
			{
				line = StringUtilities.singleStringReplace( line, aliasString, aliasCommand );
			}
		}

		return line.trim();
	}

	public static void add( final String aliasString, final String aliasCommand )
	{
		Aliases.aliasMap.put( " " + aliasString + " ", " " + aliasCommand + " " );
		Aliases.aliasSet = Aliases.aliasMap.entrySet();
		save();
	}

	public static void remove( final String aliasString )
	{
		Aliases.aliasMap.remove(  " " + aliasString + " " );
		Aliases.aliasSet = Aliases.aliasMap.entrySet();
		save();
	}

	public static void print()
	{
		ArrayList aliasList = new ArrayList();
		Iterator it = Aliases.aliasSet.iterator();
		while ( it.hasNext() )
		{
			Entry current = (Entry) it.next();
			String aliasString = (String) current.getKey();
			String aliasCommand = (String) current.getValue();

			aliasList.add( aliasString + " => " + aliasCommand );
		}

		RequestLogger.printList( aliasList );
	}
}
