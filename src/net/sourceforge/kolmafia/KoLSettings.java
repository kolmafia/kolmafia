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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;
import java.util.InvalidPropertiesFormatException;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class KoLSettings extends Properties implements UtilityConstants
{
	private File settingsFile;
	private String characterName;

	public KoLSettings()
	{	this( "" );
	}

	public KoLSettings( String characterName )
	{
		this.characterName = characterName;
		this.settingsFile = new File( DATA_DIRECTORY + "~" + this.characterName + ".xml" );
		loadSettings( this.settingsFile );
	}

	public void saveSettings()
	{	storeSettings( settingsFile );
	}

	private void loadSettings( File source )
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !source.exists() )
			{
				source.getParentFile().mkdirs();
				source.createNewFile();

				// If this is the first-ever initialization of
				// a character's settings, load them from the
				// default file.

				if ( !source.getName().equals( "~.xml" ) )
					loadSettings( new File( DATA_DIRECTORY + "~.xml" ) );
				else
				{
					setProperty( "loginServer", "0" );
					setProperty( "proxySet", "false" );
					setProperty( "battleAction", "attack" );
				}

				// Then, store the results into the designated
				// file by calling the appropriate subroutine.

				storeSettings( source );
			}

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( source );
			loadFromXML( istream );
			istream.close();
			istream = null;
		}
		catch ( InvalidPropertiesFormatException e1 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			source.delete();
			loadSettings( source );
		}
		catch ( IOException e2 )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
	}

	private void storeSettings( File destination )
	{
		try
		{
			FileOutputStream ostream = new FileOutputStream( destination );
			storeToXML( ostream, "KoLmafia Settings" );
			ostream.close();
			ostream = null;
		}
		catch ( IOException e )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
	}

}