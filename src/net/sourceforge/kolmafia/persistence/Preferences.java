/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JCheckBox;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.ChoiceManager.ChoiceAdventure;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class Preferences
{
	private static final byte [] LINE_BREAK_AS_BYTES = KoLConstants.LINE_BREAK.getBytes();

	private static final String [] characterMap = new String[ 65536 ];
	private static final TreeMap checkboxMap = new TreeMap();
	private static final TreeMap propertyNames = new TreeMap();

	private static final TreeMap userNames = new TreeMap();
	private static final TreeMap userValues = new TreeMap();
	private static File userPropertiesFile = null;

	private static final TreeMap globalNames = new TreeMap();
	private static final TreeMap globalValues = new TreeMap();
	private static File globalPropertiesFile = null;

	private static Boolean TRUE = new Boolean( true );
	private static Boolean FALSE = new Boolean( false );

	static
	{
		initializeMaps();

		Preferences.globalPropertiesFile = new File( UtilityConstants.SETTINGS_LOCATION,
			Preferences.baseUserName( "" ) + "_prefs.txt" );

		Preferences.globalValues.clear();

		Preferences.loadPreferences( globalValues, globalPropertiesFile );
		Preferences.ensureGlobalDefaults();
	}

	/**
	 * Resets all settings so that the given user is represented whenever
	 * settings are modified.
	 */

	public static final void reset( final String username )
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

		Preferences.userPropertiesFile = new File( UtilityConstants.SETTINGS_LOCATION,
			Preferences.baseUserName( username ) + "_prefs.txt" );

		Preferences.userValues.clear();

		Preferences.loadPreferences( userValues, userPropertiesFile );
		Preferences.ensureUserDefaults();

		CustomCombatManager.loadSettings();
		MoodManager.restoreDefaults();
	}

	public static final String baseUserName( final String name )
	{
		return name == null || name.equals( "" ) ? "GLOBAL" : StringUtilities.globalStringReplace( name.trim(), " ", "_" ).toLowerCase();
	}

	private static void loadPreferences( TreeMap values, File file )
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

		Entry currentEntry;
		Iterator it = p.entrySet().iterator();

		String currentName, currentValue;

		while ( it.hasNext() )
		{
			currentEntry = (Entry) it.next();
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
		String actualName = (String) Preferences.propertyNames.get( lowercase );

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
		return !property.startsWith( "saveState" );
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

	public static final float getFloat( final String name )
	{
		return getFloat( null, name );
	}

	public static final int increment( final String name, final int increment )
	{
		return Preferences.increment( name, increment, 0, false );
	}

	public static final int increment( final String name, final int increment, final int max,
		final boolean mod )
	{
		int current = Preferences.getInteger( name );
		current += increment;

		if ( max > 0 && current > max )
		{
			current = max;
		}

		if ( mod && current >= max )
		{
			current %= max;
		}

		Preferences.setInteger( name, current );
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
		TreeMap map = Preferences.getMap( name );
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
		TreeMap map = Preferences.getMap( name );
		Object value = Preferences.getObject( map, user, name );

		if ( value == null )
		{
			return 0;
		}

		if ( !(value instanceof Integer) )
		{
			value = new Integer( StringUtilities.parseInt( value.toString() ) );
			map.put( name, value );
		}

		return ((Integer) value).intValue();
	}

	public static final float getFloat( final String user, final String name )
	{
		TreeMap map = Preferences.getMap( name );
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

	private static final TreeMap getMap( final String name )
	{
		return Preferences.isGlobalProperty( name ) ? Preferences.globalValues : Preferences.userValues;
	}

	private static final Object getObject( final String user, final String name )
	{
		return Preferences.getObject( Preferences.getMap( name ), user, name );
	}

	private static final Object getObject( final TreeMap map, final String user, final String name )
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
			Preferences.setObject( user, name, value ? "true" : "false", value ? Preferences.TRUE : Preferences.FALSE );
		}
	}

	public static final void setInteger( final String user, final String name, final int value )
	{
		int old = Preferences.getInteger( user, name );
		if ( old != value )
		{
			Preferences.setObject( user, name, String.valueOf( value ), new Integer( value ) );
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

		if ( object instanceof Boolean && Preferences.checkboxMap.containsKey( name ) )
		{
			boolean isTrue = ((Boolean) object).booleanValue();

			ArrayList list = (ArrayList) Preferences.checkboxMap.get( name );
			for ( int i = 0; i < list.size(); ++i )
			{
				WeakReference reference = (WeakReference) list.get( i );
				JCheckBox item = (JCheckBox) reference.get();
				if ( item != null )
				{
					item.setSelected( isTrue );
				}
			}
		}
	}

	private static final String propertyName( final String user, final String name )
	{
		return user == null ? name : name + "." + Preferences.baseUserName( user );
	}

	public static final void registerCheckbox( final String name, final JCheckBox checkbox )
	{
		ArrayList list = null;

		if ( Preferences.checkboxMap.containsKey( name ) )
		{
			list = (ArrayList) Preferences.checkboxMap.get( name );
		}
		else
		{
			list = new ArrayList();
			Preferences.checkboxMap.put( name, list );
		}

		list.add( new WeakReference( checkbox ) );
	}

	private static final void saveToFile( File file, TreeMap data )
	{
		// Determine the contents of the file by
		// actually printing them.

		ByteArrayOutputStream ostream = new ByteArrayOutputStream();

		try
		{
			Entry current;
			Iterator it = data.entrySet().iterator();

			while ( it.hasNext() )
			{
				current = (Entry) it.next();
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
		TreeMap desiredMap;

		BufferedReader istream = FileUtilities.getVersionedReader( "defaults.txt", KoLConstants.DEFAULTS_VERSION );

		while ( ( current = FileUtilities.readData( istream ) ) != null )
		{
			desiredMap = current[ 0 ].equals( "global" ) ? Preferences.globalNames : Preferences.userNames;
			desiredMap.put( current[ 1 ], current.length == 2 ? "" : current[ 2 ] );
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
			int defaultOption = StringUtilities.parseInt( (String) Preferences.userNames.get( setting ) ) - 1;

			ostream.print( "[" + setting.substring( 15 ) + "] " );
			ostream.print( choices[ i ].getName() + ": " );

			int printedCount = 0;
			String[] options = choices[ i ].getOptions();

			ostream.print( options[ defaultOption ] + " [color=gray](" );

			for ( int j = 0; j < options.length; ++j )
			{
				if ( j == defaultOption )
				{
					continue;
				}

				if ( printedCount != 0 )
				{
					ostream.print( ", " );
				}

				++printedCount;
				ostream.print( options[ j ] );
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
		Entry[] entries = new Entry[ Preferences.globalNames.size() ];
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
		Entry[] entries = new Entry[ Preferences.userNames.size() ];
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
}
