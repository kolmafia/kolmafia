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
		if ( ensureDefaults() )
			storeSettings( settingsFile );
	}

	public synchronized String getProperty( String name )
	{	return super.getProperty( name );
	}

	public synchronized Object setProperty( String name, String value )
	{
		if ( value == null )
			return value;

		String oldValue = super.getProperty( name );
		if ( oldValue != null && oldValue.equals( value ) )
			return value;

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

	private synchronized boolean ensureDefaults()
	{
		boolean hadChanges = false;

		// The remaining settings are not related to choice
		// adventures and require no special handling.

		hadChanges |= ensureProperty( "alwaysGetBreakfast", "true" );
		hadChanges |= ensureProperty( "autoLogin", "" );
		hadChanges |= ensureProperty( "autoRepairBoxes", "false" );
		hadChanges |= ensureProperty( "autoSatisfyChecks", "false" );
		hadChanges |= ensureProperty( "autoLogChat", "false" );
		hadChanges |= ensureProperty( "battleAction", "attack" );
		hadChanges |= ensureProperty( "battleStop", "0.0" );
		hadChanges |= ensureProperty( "betweenBattleScript", "" );
		hadChanges |= ensureProperty( "breakfast.softcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		hadChanges |= ensureProperty( "breakfast.hardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		hadChanges |= ensureProperty( "browserBookmarks", "" );
		hadChanges |= ensureProperty( "buffBotCasting", "" );
		hadChanges |= ensureProperty( "buffBotMessageDisposal", "0" );
		hadChanges |= ensureProperty( "chatStyle", "0" );
		hadChanges |= ensureProperty( "clanRosterHeader", ClanSnapshotTable.getDefaultHeader() );
		hadChanges |= ensureProperty( "cloverProtectActive", "false" );
		hadChanges |= ensureProperty( "createWithoutBoxServants", "false" );
		hadChanges |= ensureProperty( "defaultDropdown1", "0" );
		hadChanges |= ensureProperty( "defaultDropdown2", "1" );
		hadChanges |= ensureProperty( "defaultLimit", "13" );
		hadChanges |= ensureProperty( "defaultToRelayBrowser", "true" );
		hadChanges |= ensureProperty( "eSoluScriptType", "0" );
		hadChanges |= ensureProperty( "fontSize", "3" );
		hadChanges |= ensureProperty( "forceReconnect", "false" );
		hadChanges |= ensureProperty( "hpAutoRecover", "-0.1" );
		hadChanges |= ensureProperty( "hpAutoRecoverTarget", "-0.1" );
		hadChanges |= ensureProperty( "hpRecoveryScript", "" );
		hadChanges |= ensureProperty( "hpRestores", "" );
		hadChanges |= ensureProperty( "highlightList", "" );
		hadChanges |= ensureProperty( "http.proxyHost", "" );
		hadChanges |= ensureProperty( "http.proxyPort", "" );
		hadChanges |= ensureProperty( "http.proxyUser", "" );
		hadChanges |= ensureProperty( "http.proxyPassword", "" );
		hadChanges |= ensureProperty( "initialDesktop", "AdventureFrame,MallSearchFrame,SkillBuffFrame,RestoreOptionsFrame" );
		hadChanges |= ensureProperty( "initialFrames", "EventsFrame,MailboxFrame" );
		hadChanges |= ensureProperty( "invalidBuffMessage", "You sent an amount which was not a valid buff amount." );
		hadChanges |= ensureProperty( "keepSessionLogs", "false" );
		hadChanges |= ensureProperty( "lastFaucetLocation", "-1" );
		hadChanges |= ensureProperty( "lastFaucetUse", "0: " );
		hadChanges |= ensureProperty( "lastAdventure", "" );
		hadChanges |= ensureProperty( "lastMessageID", "" );
		hadChanges |= ensureProperty( "lastUsername", "" );
		hadChanges |= ensureProperty( "loginServer", "0" );
		hadChanges |= ensureProperty( "luckySewerAdventure", "stolen accordion" );
		hadChanges |= ensureProperty( "mpAutoRecover", "0.0" );
		hadChanges |= ensureProperty( "mpAutoRecoverTarget", "0.0" );
		hadChanges |= ensureProperty( "mpRecoveryScript", "" );
		hadChanges |= ensureProperty( "mpRestores", "" );
		hadChanges |= ensureProperty( "proxySet", "false" );
		hadChanges |= ensureProperty( "relayAddsCommandLineLinks", "true" );
		hadChanges |= ensureProperty( "relayAddsSimulatorLinks", "true" );
		hadChanges |= ensureProperty( "relayAddsUseLinks", "true" );
		hadChanges |= ensureProperty( "relayMovesManeuver", "true" );
		hadChanges |= ensureProperty( "retrieveContacts", "true" );
		hadChanges |= ensureProperty( "saveState", "" );
		hadChanges |= ensureProperty( "scriptList", "win game" );
		hadChanges |= ensureProperty( "serverFriendly", "false" );
		hadChanges |= ensureProperty( "showAdventureZone", "true" );
		hadChanges |= ensureProperty( "showAllRequests", "false" );
		hadChanges |= ensureProperty( "showClosetDrivenCreations", "true" );
		hadChanges |= ensureProperty( "sortAdventures", "false" );
		hadChanges |= ensureProperty( "thanksMessage", "Thank you for the donation.  It is greatly appreciated." );
		hadChanges |= ensureProperty( "toolbarPosition", "1" );
		hadChanges |= ensureProperty( "useSystemTrayIcon", "false" );
		hadChanges |= ensureProperty( "usePopupContacts", "1" );
		hadChanges |= ensureProperty( "useTabbedChat", "1" );
		hadChanges |= ensureProperty( "useTextHeavySidepane", "true" );
		hadChanges |= ensureProperty( "useToolbars", "true" );
		hadChanges |= ensureProperty( "violetFogGoal", "0" );
		hadChanges |= ensureProperty( "whiteList", "" );
		hadChanges |= ensureProperty( "zoneExcludeList", "Removed" );
		hadChanges |= ensureProperty( "guiUsesOneWindow", "false" );

		// These are settings related to choice adventures.
		// Ensure that they exist, and if they do not, load
		// them to their default settings.

		// KoL no longer allows you to "ignore" a choice adventure,
		// although some of them have a setting that is the equivalent
		// of "ignore".

		// Choices that have an "ignore" setting: use ensureProperty
		// Choices that have no "ignore" setting: use ensureNonZeroProperty

		hadChanges |= ensureProperty( "choiceAdventure2", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure3", "1" );
		hadChanges |= ensureProperty( "choiceAdventure4", "3" );
		hadChanges |= ensureProperty( "choiceAdventure5", "2" );
		hadChanges |= ensureProperty( "choiceAdventure7", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure8", "3" );
		hadChanges |= ensureProperty( "choiceAdventure9", "1" );
		hadChanges |= ensureProperty( "choiceAdventure10", "1" );
		hadChanges |= ensureProperty( "choiceAdventure11", "3" );
		hadChanges |= ensureProperty( "choiceAdventure12", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure14", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure15", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure16", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure17", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure18", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure19", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure20", "4" );
		hadChanges |= ensureProperty( "choiceAdventure21", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure22", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure23", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure24", "4" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure25", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure26", "3" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure27", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure28", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure29", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure40", "3" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure41", "3" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure42", "3" );
		hadChanges |= ensureProperty( "choiceAdventure45", "0" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure46", "3" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure47", "2" );
		hadChanges |= ensureNonZeroProperty( "choiceAdventure71", "1" );

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

		String wheelChoice = null;
		String wheelDecision = null;

		for ( int i = 0; i < 4; ++i )
		{
			wheelChoice = "choiceAdventure" + (9+i);
			wheelDecision = String.valueOf( wheelChoices[i] );
			if ( !getProperty( wheelChoice ).equals( wheelDecision ) )
			{
				super.setProperty( wheelChoice, wheelDecision );
				hadChanges = true;
			}
		}

		// Return whether or not any changes were detected
		// in the settings files.

		return hadChanges;
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private synchronized boolean ensureProperty( String key, String defaultValue )
	{
		if ( containsKey( key ) )
			return false;

		super.setProperty( key, defaultValue );
		return true;
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value. Additionally, if the property exists
	 * and is 0, force it to the default value. This is for choice adventures.
	 */

	private synchronized boolean ensureNonZeroProperty( String key, String defaultValue )
	{
		if ( containsKey( key ) && !get( key ).equals( "0" ) )
			return false;

		super.setProperty( key, defaultValue );
		return true;
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
