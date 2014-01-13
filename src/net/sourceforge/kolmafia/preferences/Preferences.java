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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.preferences;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ChoiceManager.ChoiceAdventure;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class Preferences
{
	private static final byte [] LINE_BREAK_AS_BYTES = KoLConstants.LINE_BREAK.getBytes();

	private static final String [] characterMap = new String[ 65536 ];
	private static final HashMap<String, String> propertyNames = new HashMap<String, String>();

	private static final HashMap<String,String> userNames = new HashMap<String,String>();
	private static final TreeMap<String, Object> userValues = new TreeMap<String, Object>();
	private static File userPropertiesFile = null;

	private static final HashMap<String, String> globalNames = new HashMap<String, String>();
	private static final TreeMap<String, Object> globalValues = new TreeMap<String, Object>();
	private static File globalPropertiesFile = null;
	
	private static final Set<String> defaultsSet = new HashSet<String>();

	static
	{
		initializeMaps();

		Preferences.globalPropertiesFile = new File( KoLConstants.SETTINGS_LOCATION,
			Preferences.baseUserName( "" ) + "_prefs.txt" );

		Preferences.globalValues.clear();

		Preferences.loadPreferences( globalValues, globalPropertiesFile );
		Preferences.ensureGlobalDefaults();
	}

	/**
	 * Resets all settings so that the given user is represented whenever
	 * settings are modified.
	 */

	public static synchronized final void reset( final String username )
	{
		Preferences.saveToFile( Preferences.globalPropertiesFile, Preferences.globalValues );

		if ( username == null || username.equals( "" ) )
		{
			if ( Preferences.userPropertiesFile != null )
			{
				Preferences.saveToFile( Preferences.userPropertiesFile, Preferences.userValues );

				Preferences.userPropertiesFile = null;
				Preferences.userValues.clear();
			}

			return;
		}

		Preferences.userPropertiesFile = new File( KoLConstants.SETTINGS_LOCATION,
			Preferences.baseUserName( username ) + "_prefs.txt" );

		Preferences.userValues.clear();

		Preferences.loadPreferences( userValues, userPropertiesFile );
		Preferences.ensureUserDefaults();

		AdventureFrame.updateFromPreferences();
		CharPaneDecorator.updateFromPreferences();
		CombatActionManager.updateFromPreferences();
		MoodManager.updateFromPreferences();
		PreferenceListenerRegistry.fireAllPreferencesChanged();
	}

	public static final String baseUserName( final String name )
	{
		return name == null || name.equals( "" ) ? "GLOBAL" : StringUtilities.globalStringReplace( name.trim(), " ", "_" ).toLowerCase();
	}

	private static void loadPreferences( TreeMap<String, Object> values, File file )
	{
		Properties p = new Properties();
		InputStream istream = DataUtilities.getInputStream( file );

		try
		{
			p.load( istream );
		}
		catch ( IOException e )
		{
		}

		try
		{
			istream.close();
		}
		catch ( IOException e )
		{
		}

		Entry<Object, Object> currentEntry;
		Iterator<Entry<Object, Object>> it = p.entrySet().iterator();

		String currentName, currentValue;

		while ( it.hasNext() )
		{
			currentEntry = it.next();
			currentName = (String) currentEntry.getKey();
			currentValue = (String) currentEntry.getValue();

			Preferences.propertyNames.put( currentName.toLowerCase(), currentName );
			values.put( currentName, currentValue );
		}
	}

	private static final String encodeProperty( String name, String value )
	{
		StringBuffer buffer = new StringBuffer();

		Preferences.encodeString( buffer, name );

		if ( value != null && value.length() > 0 )
		{
			buffer.append( "=" );
			Preferences.encodeString( buffer, value );
		}

		return buffer.toString();
	}

	private static final void encodeString( StringBuffer buffer, String string )
	{
		int length = string.length();

		for ( int i = 0; i < length; ++i )
		{
			char ch = string.charAt( i );
			encodeCharacter( ch );
			buffer.append( characterMap[ ch ] );
		}
	}

	private static final void encodeCharacter( char ch )
	{
		if ( characterMap[ ch ] != null )
		{
			return;
		}

		switch ( ch )
		{
		case '\t':
			characterMap[ ch ] = "\\t";
			return;
		case '\n':
			characterMap[ ch ] = "\\n";
			return;
		case '\f':
			characterMap[ ch ] = "\\f";
			return;
		case '\r':
			characterMap[ ch ] = "\\r";
			return;
		case '\\':
		case '=':
		case ':':
		case '#':
		case '!':
			characterMap[ ch ] = "\\" + ch;
			return;
		}

		if ( ch > 0x0019 || ch < 0x007f )
		{
			characterMap[ ch ] = String.valueOf( ch );
			return;
		}

		if ( ch < 0x0010 )
		{
			characterMap[ ch ] = "\\u000" + Integer.toHexString( ch );
			return;
		}

		if ( ch < 0x0100 )
		{
			characterMap[ ch ] = "\\u00" + Integer.toHexString( ch );
			return;
		}

		if ( ch < 0x1000 )
		{
			characterMap[ ch ] = "\\u0" + Integer.toHexString( ch );
			return;
		}

		characterMap[ ch ] = "\\u" + Integer.toHexString( ch );
	}

	public static final String getCaseSensitiveName( final String name )
	{
		String lowercase = name.toLowerCase();
		String actualName = Preferences.propertyNames.get( lowercase );

		if ( actualName != null )
		{
			return actualName;
		}

		Preferences.propertyNames.put( lowercase, name );
		return name;
	}

	private static final boolean isGlobalProperty( final String name )
	{
		return Preferences.globalNames.containsKey( name );
	}

	public static final boolean isUserEditable( final String property )
	{
		return !property.startsWith( "saveState" ) && !property.equals( "externalEditor" ) && !property.equals( "preferredWebBrowser" );
	}

	public static final void setString( final String name, final String value )
	{
		setString( null, name, value );
	}

	public static final String getString( final String name )
	{
		return getString( null, name );
	}

	public static final void setBoolean( final String name, final boolean value )
	{
		setBoolean( null, name, value );
	}

	public static final boolean getBoolean( final String name )
	{
		return getBoolean( null, name );
	}

	public static final void setInteger( final String name, final int value )
	{
		setInteger( null, name, value );
	}

	public static final int getInteger( final String name )
	{
		return getInteger( null, name );
	}

	public static final void setFloat( final String name, final float value )
	{
		setFloat( null, name, value );
	}

	public static final void setLong( final String name, final long value )
	{
		setLong( null, name, value );
	}

	public static final long getLong( final String name )
	{
		return getLong( null, name );
	}

	public static final float getFloat( final String name )
	{
		return getFloat( null, name );
	}

	public static final int increment( final String name )
	{
		return Preferences.increment( name, 1 );
	}

	public static final int increment( final String name, final int delta )
	{
		return Preferences.increment( name, delta, 0, false );
	}

	public static final int increment( final String name, final int delta, final int max, final boolean mod )
	{
		int current = Preferences.getInteger( name );
		if ( delta != 0 )
		{
			current += delta;

			if ( max > 0 && current > max )
			{
				current = max;
			}

			if ( mod && current >= max )
			{
				current %= max;
			}

			Preferences.setInteger( name, current );
		}
		return current;
	}

	public static final int decrement( final String name )
	{
		return Preferences.decrement( name, 1 );
	}

	public static final int decrement( final String name, final int delta )
	{
		return Preferences.decrement( name, delta, 0 );
	}

	public static final int decrement( final String name, final int delta, final int min )
	{
		int current = Preferences.getInteger( name );
		if ( delta != 0 )
		{
			current -= delta;

			if ( current < min )
			{
				current = min;
			}

			Preferences.setInteger( name, current );
		}
		return current;
	}

	// Per-user global properties are stored in the global settings with
	// key "<name>.<user>"

	public static final String getString( final String user, final String name )
	{
		Object value = Preferences.getObject( user, name );

		if ( value == null )
		{
			return "";
		}

		return value.toString();
	}

	public static final boolean getBoolean( final String user, final String name )
	{
		TreeMap<String, Object> map = Preferences.getMap( name );
		Object value = Preferences.getObject( map, user, name );

		if ( value == null )
		{
			return false;
		}

		if ( !(value instanceof Boolean) )
		{
			value = Boolean.valueOf( value.toString() );
			map.put( name, value );
		}

		return ((Boolean) value).booleanValue();
	}

	public static final int getInteger( final String user, final String name )
	{
		TreeMap<String, Object> map = Preferences.getMap( name );
		Object value = Preferences.getObject( map, user, name );

		if ( value == null )
		{
			return 0;
		}

		if ( !(value instanceof Integer) )
		{
			value = IntegerPool.get( StringUtilities.parseInt( value.toString() ) );
			map.put( name, value );
		}

		return ((Integer) value).intValue();
	}

	public static final long getLong( final String user, final String name )
	{
		TreeMap<String, Object> map = Preferences.getMap( name );
		Object value = Preferences.getObject( map, user, name );

		if ( value == null )
		{
			return 0;
		}

		if ( !(value instanceof Long) )
		{
			value = new Long( StringUtilities.parseLong( value.toString() ) );
			map.put( name, value );
		}

		return ((Long) value).longValue();
	}

	public static final float getFloat( final String user, final String name )
	{
		TreeMap<String, Object> map = Preferences.getMap( name );
		Object value = Preferences.getObject( map, user, name );

		if ( value == null )
		{
			return 0.0f;
		}

		if ( !(value instanceof Float) )
		{
			value = new Float( StringUtilities.parseFloat( value.toString() ) );
			map.put( name, value );
		}

		return ((Float) value).floatValue();
	}

	private static final TreeMap<String, Object> getMap( final String name )
	{
		return Preferences.isGlobalProperty( name ) ? Preferences.globalValues : Preferences.userValues;
	}

	private static final Object getObject( final String user, final String name )
	{
		return Preferences.getObject( Preferences.getMap( name ), user, name );
	}

	private static final Object getObject( final TreeMap<String, Object> map, final String user, final String name )
	{
		String key = Preferences.propertyName( user, name );
		return map.get( key );
	}

	public static final void setString( final String user, final String name, final String value )
	{
		String old = Preferences.getString( user, name );
		if ( !old.equals( value ) )
		{
			Preferences.setObject( user, name, value, value );
		}
	}

	public static final void setBoolean( final String user, final String name, final boolean value )
	{
		boolean old = Preferences.getBoolean( user, name );
		if ( old != value )
		{
			Preferences.setObject( user, name, value ? "true" : "false", Boolean.valueOf( value ) );
		}
	}

	public static final void setInteger( final String user, final String name, final int value )
	{
		int old = Preferences.getInteger( user, name );
		if ( old != value )
		{
			Preferences.setObject( user, name, String.valueOf( value ), IntegerPool.get( value ) );
		}
	}

	public static final void setLong( final String user, final String name, final long value )
	{
		long old = Preferences.getLong( user, name );
		if ( old != value )
		{
			Preferences.setObject( user, name, String.valueOf( value ), new Long( value ) );
		}
	}

	public static final void setFloat( final String user, final String name, final float value )
	{
		float old = Preferences.getFloat( user, name );
		if ( old != value )
		{
			Preferences.setObject( user, name, String.valueOf( value ), new Float( value ) );
		}
	}

	private static synchronized final void setObject( final String user, final String name, final String value, final Object object )
	{
		if ( Preferences.isGlobalProperty( name ) )
		{
			String actualName = Preferences.propertyName( user, name );

			Preferences.globalValues.put( actualName, object );
			if ( Preferences.getBoolean( "saveSettingsOnSet" ) )
			{
				Preferences.saveToFile( Preferences.globalPropertiesFile, Preferences.globalValues );
			}
		}
		else if ( Preferences.userPropertiesFile != null )
		{
			Preferences.userValues.put( name, object );
			if ( Preferences.getBoolean( "saveSettingsOnSet" ) )
			{
				Preferences.saveToFile( Preferences.userPropertiesFile, Preferences.userValues );
			}
		}

		PreferenceListenerRegistry.firePreferenceChanged( name );

		if ( name.startsWith( "choiceAdventure" ) )
		{
			PreferenceListenerRegistry.firePreferenceChanged( "choiceAdventure*" );
		}
	}

	private static final String propertyName( final String user, final String name )
	{
		return user == null ? name : name + "." + Preferences.baseUserName( user );
	}

	private static final void saveToFile( File file, TreeMap<String, Object> data )
	{
		// Determine the contents of the file by
		// actually printing them.

		ByteArrayOutputStream ostream = new ByteArrayOutputStream();

		try
		{
			Entry<String, Object> current;
			Iterator<Entry<String, Object>> it = data.entrySet().iterator();

			while ( it.hasNext() )
			{
				current = it.next();
				ostream.write( Preferences.encodeProperty(
					(String) current.getKey(), current.getValue().toString() ).getBytes() );

				ostream.write( LINE_BREAK_AS_BYTES );
			}
		}
		catch ( IOException e )
		{
		}

		OutputStream fstream = DataUtilities.getOutputStream( file );

		try
		{
			ostream.writeTo( fstream );
		}
		catch ( IOException e )
		{
		}

		try
		{
			fstream.close();
		}
		catch ( IOException e )
		{
		}
	}

	private static final void initializeMaps()
	{
		String[] current;
		HashMap<String, String> desiredMap;

		BufferedReader istream = FileUtilities.getVersionedReader( "defaults.txt", KoLConstants.DEFAULTS_VERSION );

		while ( ( current = FileUtilities.readData( istream ) ) != null )
		{
			if ( current.length >= 2 )
			{
				String map = current[ 0 ];
				String name = current[ 1 ];
				String value = current.length == 2 ? "" : current[ 2 ];
				desiredMap = map.equals( "global" ) ? Preferences.globalNames : Preferences.userNames;
				desiredMap.put( name, value );

				// Maintain a set of prefs that exist in defaults.txt
				defaultsSet.add( name );
			}
		}

		// Update Mac-specific properties values to ensure
		// that the displays are usable (by default).

		boolean isUsingMac = System.getProperty( "os.name" ).startsWith( "Mac" );

		Preferences.globalNames.put( "useDecoratedTabs", String.valueOf( !isUsingMac ) );
		Preferences.globalNames.put( "chatFontSize", isUsingMac ? "medium" : "small" );

		try
		{
			istream.close();
		}
		catch ( Exception e )
		{
			// The stream is already closed, go ahead
			// and ignore this error.
		}
	}

	public static final void printDefaults()
	{
		PrintStream ostream = LogStream.openStream( "choices.txt", true );

		ostream.println( "[u]Configurable[/u]" );
		ostream.println();

		ChoiceManager.setChoiceOrdering( false );
		Arrays.sort( ChoiceManager.CHOICE_ADVS );
		Arrays.sort( ChoiceManager.CHOICE_ADV_SPOILERS );

		Preferences.printDefaults( ChoiceManager.CHOICE_ADVS, ostream );

		ostream.println();
		ostream.println();
		ostream.println( "[u]Not Configurable[/u]" );
		ostream.println();

		Preferences.printDefaults( ChoiceManager.CHOICE_ADV_SPOILERS, ostream );

		ChoiceManager.setChoiceOrdering( true );
		Arrays.sort( ChoiceManager.CHOICE_ADVS );
		Arrays.sort( ChoiceManager.CHOICE_ADV_SPOILERS );

		ostream.close();
	}

	private static final void printDefaults( final ChoiceAdventure[] choices, final PrintStream ostream )
	{
		for ( int i = 0; i < choices.length; ++i )
		{
			String setting = choices[ i ].getSetting();

			ostream.print( "[" + setting.substring( 15 ) + "] " );
			ostream.print( choices[ i ].getName() + ": " );

			Object[] options = choices[ i ].getOptions();
			int defaultOption = StringUtilities.parseInt( Preferences.userNames.get( setting ) );
			Object def = ChoiceManager.findOption( options, defaultOption );

			ostream.print( def.toString() + " [color=gray](" );

			int printedCount = 0;
			for ( int j = 0; j < options.length; ++j )
			{
				Object option = options[ j ];
				if ( option == def )
				{
					continue;
				}

				if ( printedCount != 0 )
				{
					ostream.print( ", " );
				}

				++printedCount;
				ostream.print( option.toString() );
			}

			ostream.println( ")[/color]" );
		}
	}

	/**
	 * Ensures that all the default keys are non-null. This is used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private static void ensureGlobalDefaults()
	{
		Entry< ? , ? >[] entries = new Entry[ Preferences.globalNames.size() ];
		Preferences.globalNames.entrySet().toArray( entries );

		for ( int i = 0; i < entries.length; ++i )
		{
			if ( !Preferences.globalValues.containsKey( entries[ i ].getKey() ) )
			{
				String key = (String) entries[ i ].getKey();
				String value = (String) entries[ i ].getValue();

				Preferences.globalValues.put( key, value );
			}
		}
	}

	private static void ensureUserDefaults()
	{
		Entry< ? , ? >[] entries = new Entry[ Preferences.userNames.size() ];
		Preferences.userNames.entrySet().toArray( entries );

		for ( int i = 0; i < entries.length; ++i )
		{
			if ( !Preferences.userValues.containsKey( entries[ i ].getKey() ) )
			{
				String key = (String) entries[ i ].getKey();
				String value = (String) entries[ i ].getValue();

				Preferences.userValues.put( key, value );
			}
		}
	}

	public static void resetToDefault( String name )
	{
		if ( Preferences.userNames.containsKey( name ) )
		{
			Preferences.setString( name, Preferences.userNames.get( name ) );
		}
		else if ( Preferences.globalNames.containsKey( name ) )
		{
			Preferences.setString( name, Preferences.globalNames.get( name ) );
		}
	}

	public static synchronized void resetDailies()
	{
		Iterator<String> i = Preferences.userValues.keySet().iterator();
		while ( i.hasNext() )
		{
			String name = i.next();
			if ( name.startsWith( "_" ) )
			{
				if ( !Preferences.containsDefault( name ) )
				{
					// fully delete preferences that start with _ and aren't in defaults.txt
					i.remove();
					if ( Preferences.getBoolean( "saveSettingsOnSet" ) )
					{
						Preferences.saveToFile( Preferences.userPropertiesFile, Preferences.userValues );
					}
					continue;
				}
				String val = Preferences.userNames.get( name );
				if ( val == null ) val = "";
				Preferences.setString( name, val );
			}
		}
	}

	public static boolean containsDefault( String key )
	{
		return defaultsSet.contains( key );
	}
}
