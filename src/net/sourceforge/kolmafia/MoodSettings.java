/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.util.Set;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class MoodSettings extends Properties implements KoLConstants
{
	public static String [] SKILL_NAMES = null;
	
	private String filename;
	private File settingsFile;

	static
	{
		ArrayList skills = new ArrayList();
		skills.addAll( ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.BUFF ) );
		skills.addAll( ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.SKILL ) );
		
		UseSkillRequest [] requests = new UseSkillRequest[ skills.size() ];
		skills.toArray( requests );
		
		SKILL_NAMES = new String[ requests.length ];
		for ( int i = 0; i < requests.length; ++i )
			SKILL_NAMES[i] = requests[i].getSkillName().toLowerCase();
	}
	
	public MoodSettings( String filename )
	{
		this.filename = filename;
		this.settingsFile = new File( DATA_DIRECTORY + filename );

		loadSettings( this.settingsFile );
		ensureDefaults();
	}
	
	public void ensureDefaults()
	{
		if ( !keySet().isEmpty() )
			return;
		
		setProperty( "hat", "(no change)" );
		setProperty( "weapon", "(no change)" );
		setProperty( "off-hand", "(no change)" );
		setProperty( "pants", "(no change)" );
		setProperty( "accessory 1", "(no change)" );
		setProperty( "accessory 2", "(no change)" );
		setProperty( "accessory 3", "(no change)" );
		setProperty( "familiar", "(no change)" );
		
		for ( int i = 0; i < SKILL_NAMES.length; ++i )
			setProperty( SKILL_NAMES[i], "ignore" );
	}
	
	public void execute()
	{
		// Change out all your gear first.  The new
		// equipment will change how casting works.
		
		DEFAULT_SHELL.executeLine( "equip hat " + getProperty( "hat" ) );
		DEFAULT_SHELL.executeLine( "equip weapon " + getProperty( "weapon" ) );
		DEFAULT_SHELL.executeLine( "equip off-hand " + getProperty( "off-hand" ) );
		DEFAULT_SHELL.executeLine( "equip pants " + getProperty( "pants" ) );
		DEFAULT_SHELL.executeLine( "equip acc1 " + getProperty( "accessory 1" ) );
		DEFAULT_SHELL.executeLine( "equip acc2 " + getProperty( "accessory 2" ) );
		DEFAULT_SHELL.executeLine( "equip acc3 " + getProperty( "accessory 3" ) );
		DEFAULT_SHELL.executeLine( "familiar " + getProperty( "familiar" ) );
	}
	
	public synchronized Object setProperty( String name, String value )
	{
		String oldValue = super.getProperty( name );
		if ( oldValue != null && oldValue.equals( value ) )
			return value;

		super.setProperty( name, value );
		saveSettings();

		return oldValue;
	}

	public synchronized void saveSettings()
	{	storeSettings( settingsFile );
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	destination	The file to which the settings will be stored.
	 */

	private synchronized void storeSettings( File destination )
	{
		try
		{
			// Determine the contents of the file by
			// actually printing them.

			FileOutputStream ostream = new FileOutputStream( destination );
			store( ostream, "KoLmafia Settings" );
			ostream.close();

			// Make sure that all of the settings are
			// in a sorted order.

			ArrayList contents = new ArrayList();
			BufferedReader reader = new BufferedReader( new InputStreamReader(
				new FileInputStream( destination ) ) );

			String line;
			while ( (line = reader.readLine()) != null )
				contents.add( line );

			reader.close();
			Collections.sort( contents );

			File temporary = new File( DATA_DIRECTORY + "~" + filename + ".tmp" );
			temporary.createNewFile();
			temporary.deleteOnExit();

			PrintStream writer = new PrintStream( new FileOutputStream( temporary ) );
			for ( int i = 0; i < contents.size(); ++i )
				if ( !((String) contents.get(i)).startsWith( "saveState" ) || filename.equals( "" ) )
					writer.println( (String) contents.get(i) );

			writer.close();
			destination.delete();
			temporary.renameTo( destination );

			ostream = null;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private synchronized void loadSettings( File source )
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !source.exists() )
			{
				source.getParentFile().mkdirs();
				source.createNewFile();

				// Then, store the results into the designated
				// file by calling the appropriate subroutine.

				if ( source != settingsFile )
					storeSettings( source );
			}

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( source );
			load( istream );
			istream.close();
			istream = null;
		}
		catch ( IOException e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			StaticEntity.printStackTrace( e2 );
			source.delete();
			loadSettings( source );
		}
	}
}