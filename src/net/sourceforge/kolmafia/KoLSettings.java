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
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * An extension of {@link java.util.Properties} which handles all the
 * user settings of <code>KoLmafia</code>.  In order to maintain issues
 * involving compatibility (J2SE 1.4 does not support XML output directly),
 * all data is written using {@link java.util.Properties#store(OutputStream,String)}.
 * Files are named according  to the following convention: a tilde (<code>~</code>)
 * preceeds the name of the character whose settings this object represents,
 * with the 'kcs' extension (KoLmafia Character Settings).  All global settings
 * are stored in <code>~.kcs</code>.
 */

public class KoLSettings extends Properties implements UtilityConstants
{
	private File settingsFile;
	private String characterName;

	/**
	 * Constructs a global settings file.
	 */

	public KoLSettings()
	{	this( "" );
	}

	/**
	 * Constructs a settings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	characterName	The name of the character this settings file represents
	 */

	public KoLSettings( String characterName )
	{
		this.characterName = characterName;
		String noExtensionName = characterName.replaceAll( "\\/q", "" ).replaceAll( " ", "_" ).toLowerCase();
		this.settingsFile = new File( DATA_DIRECTORY + "~" + noExtensionName + ".kcs" );

		// Make sure that any settings that were applied in the default
		// that did not exist before are applied universally.

		if ( !characterName.equals( "" ) )
			loadSettings( new File( DATA_DIRECTORY + "~.kcs" ) );

		loadSettings( this.settingsFile );
		ensureDefaults();
		storeSettings( settingsFile );
	}


	public synchronized String getProperty( String name )
	{	return super.getProperty( name );
	}

	public synchronized Object setProperty( String name, String value )
	{
		Object returnValue = super.setProperty( name, value );
		storeSettings( settingsFile );
		return returnValue;
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

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

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private synchronized void ensureDefaults()
	{
		// The remaining settings are not related to choice
		// adventures and require no special handling.

		ensureProperty( "alwaysGetBreakfast", "true" );
		ensureProperty( "autoLogin", "" );
		ensureProperty( "autoRepairBoxes", "false" );
		ensureProperty( "autoSatisfyChecks", "false" );
		ensureProperty( "autoLogChat", "false" );
		ensureProperty( "battleAction", "attack" );
		ensureProperty( "battleStop", "0.0" );
		ensureProperty( "betweenBattleScript", "" );
		ensureProperty( "breakfast.softcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		ensureProperty( "breakfast.hardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		ensureProperty( "browserBookmarks", "" );
		ensureProperty( "buffBotCasting", "" );
		ensureProperty( "buffBotMessageDisposal", "0" );
		ensureProperty( "chatStyle", "0" );
		ensureProperty( "clanRosterHeader", ClanSnapshotTable.getDefaultHeader() );
		ensureProperty( "cloverProtectActive", "false" );
		ensureProperty( "createWithoutBoxServants", "false" );
		ensureProperty( "defaultDropdown1", "0" );
		ensureProperty( "defaultDropdown2", "1" );
		ensureProperty( "defaultLimit", "13" );
		ensureProperty( "defaultToRelayBrowser", "true" );
		ensureProperty( "eSoluScriptType", "0" );
		ensureProperty( "fontSize", "3" );
		ensureProperty( "forceReconnect", "false" );
		ensureProperty( "hpAutoRecover", "-0.1" );
		ensureProperty( "hpAutoRecoverTarget", "1.0" );
		ensureProperty( "hpRecoveryScript", "" );
		ensureProperty( "hpRestores", "" );
		ensureProperty( "highlightList", "" );
		ensureProperty( "http.proxyHost", "" );
		ensureProperty( "http.proxyPort", "" );
		ensureProperty( "http.proxyUser", "" );
		ensureProperty( "http.proxyPassword", "" );
		ensureProperty( "initialDesktop", "AdventureFrame,MallSearchFrame,SkillBuffFrame,RestoreOptionsFrame" );
		ensureProperty( "initialFrames", "EventsFrame,MailboxFrame" );
		ensureProperty( "invalidBuffMessage", "You sent an amount which was not a valid buff amount." );
		ensureProperty( "keepSessionLogs", "false" );
		ensureProperty( "lastAdventure", "" );
		ensureProperty( "lastMessageID", "" );
		ensureProperty( "lastUsername", "" );
		ensureProperty( "loginServer", "0" );
		ensureProperty( "luckySewerAdventure", "stolen accordion" );
		ensureProperty( "mpAutoRecover", "-0.1" );
		ensureProperty( "mpAutoRecoverTarget", "1.0" );
		ensureProperty( "mpRecoveryScript", "" );
		ensureProperty( "mpRestores", "" );
		ensureProperty( "proxySet", "false" );
		ensureProperty( "relayAddsCommandLineLinks", "true" );
		ensureProperty( "relayAddsSimulatorLinks", "true" );
		ensureProperty( "relayAddsUseLinks", "true" );
		ensureProperty( "relayMovesManeuver", "true" );
		ensureProperty( "saveState", "" );
		ensureProperty( "serverFriendly", "false" );
		ensureProperty( "showAdventureZone", "true" );
		ensureProperty( "showAllRequests", "false" );
		ensureProperty( "showClosetDrivenCreations", "true" );
		ensureProperty( "sortAdventures", "false" );
		ensureProperty( "thanksMessage", "Thank you for the donation.  It is greatly appreciated." );
		ensureProperty( "toolbarPosition", "1" );
		ensureProperty( "useSystemTrayIcon", "false" );
		ensureProperty( "usePopupContacts", "1" );
		ensureProperty( "useTabbedChat", "1" );
		ensureProperty( "useTextHeavySidepane", "true" );
		ensureProperty( "useToolbars", "true" );
		ensureProperty( "whiteList", "" );
		ensureProperty( "zoneExcludeList", "Removed" );

		// These are settings related to choice adventures.
		// Ensure that they exist, and if they do not, load
		// them to their default settings.

		// KoL no longer allows you to "ignore" a choice adventure,
		// although some of them have a setting that is the equivalent
		// of "ignore".

		// Choices that have an "ignore" setting: use ensureProperty
		// Choices that have no "ignore" setting: use ensureNonZeroProperty

		ensureProperty( "choiceAdventure2", "2" );
		ensureNonZeroProperty( "choiceAdventure3", "1" );
		ensureProperty( "choiceAdventure4", "3" );
		ensureProperty( "choiceAdventure5", "2" );
		ensureProperty( "choiceAdventure7", "2" );
		ensureNonZeroProperty( "choiceAdventure8", "3" );
		ensureProperty( "choiceAdventure9", "1" );
		ensureProperty( "choiceAdventure10", "1" );
		ensureProperty( "choiceAdventure11", "3" );
		ensureProperty( "choiceAdventure12", "2" );
		ensureNonZeroProperty( "choiceAdventure14", "4" );
		ensureNonZeroProperty( "choiceAdventure15", "4" );
		ensureNonZeroProperty( "choiceAdventure16", "4" );
		ensureNonZeroProperty( "choiceAdventure17", "4" );
		ensureNonZeroProperty( "choiceAdventure18", "4" );
		ensureNonZeroProperty( "choiceAdventure19", "4" );
		ensureNonZeroProperty( "choiceAdventure20", "4" );
		ensureProperty( "choiceAdventure21", "2" );
		ensureNonZeroProperty( "choiceAdventure22", "4" );
		ensureNonZeroProperty( "choiceAdventure23", "4" );
		ensureNonZeroProperty( "choiceAdventure24", "4" );
		ensureNonZeroProperty( "choiceAdventure25", "2" );
		ensureNonZeroProperty( "choiceAdventure26", "3" );
		ensureNonZeroProperty( "choiceAdventure27", "2" );
		ensureNonZeroProperty( "choiceAdventure28", "2" );
		ensureNonZeroProperty( "choiceAdventure29", "2" );
		ensureNonZeroProperty( "choiceAdventure40", "3" );
		ensureNonZeroProperty( "choiceAdventure41", "3" );
		ensureNonZeroProperty( "choiceAdventure42", "3" );
		ensureProperty( "choiceAdventure45", "0" );
		ensureNonZeroProperty( "choiceAdventure46", "3" );
		ensureNonZeroProperty( "choiceAdventure47", "2" );

		// Wheel choice adventures need special handling.
		// This is where everything is validated for that.

		// KoL no longer allows you to "ignore" a choice adventure.
		// Fortunately, the wheel choices all provide a "leave the wheel alone" option
		// which does exactly that - and doesn't use an adventure to take it.

		int [] wheelChoices = new int[4];
		for ( int i = 0; i < 4; ++i )
			wheelChoices[i] = Integer.parseInt( getProperty( "choiceAdventure" + (9+i) ) );

		int clockwiseCount = 0, counterClockwiseCount = 0;
		for ( int i = 0; i < 4; ++i )
		{
			switch ( wheelChoices[i] )
			{
				case 0:
					wheelChoices[i] = 3;
					break;

				case 1:
					++clockwiseCount;
					break;

				case 2:
					++counterClockwiseCount;
					break;
			}
		}

		// Check for valid settings:

		// 1) Two clockwise, one counterclockwise, one leave alone
		// 2) Two counterclockwise, one clockwise, one leave alone
		// 3) Four clockwise
		// 4) Four counterclockwise
		// 5) All leave alone

		if ( !( (clockwiseCount == 1 && counterClockwiseCount == 2) ||
			(clockwiseCount == 2 && counterClockwiseCount == 1) ||
			(clockwiseCount == 4) ||
			(counterClockwiseCount == 4) ||
			(clockwiseCount == 0 && counterClockwiseCount == 0) ) )
		{
			wheelChoices[0] = 1;
			wheelChoices[1] = 1;
			wheelChoices[2] = 3;
			wheelChoices[3] = 2;
		}

		for ( int i = 0; i < 4; ++i )
			setProperty( "choiceAdventure" + (9+i), String.valueOf( wheelChoices[i] ) );
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private synchronized void ensureProperty( String key, String defaultValue )
	{
		if ( !containsKey( key ) )
			super.setProperty( key, defaultValue );
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value. Additionally, if the property exists
	 * and is 0, force it to the default value. This is for choice adventures.
	 */

	private synchronized void ensureNonZeroProperty( String key, String defaultValue )
	{
		if ( !containsKey( key ) || ( get( key).equals( "0" ) ) )
			super.setProperty( key, defaultValue );
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

			File temporary = new File( DATA_DIRECTORY + "~" + characterName + ".tmp" );
			temporary.createNewFile();
			temporary.deleteOnExit();

			PrintStream writer = new PrintStream( new FileOutputStream( temporary ) );
			for ( int i = 0; i < contents.size(); ++i )
				if ( !((String) contents.get(i)).startsWith( "saveState" ) || characterName.equals( "" ) )
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
}
