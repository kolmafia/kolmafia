/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import javax.swing.JCheckBox;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureDatabase.ChoiceAdventure;

public class KoLSettings extends Properties implements UtilityConstants
{
	private boolean initializingDefaults = false;

	private static final TreeMap checkboxMap = new TreeMap();
	private static final TreeMap CLIENT_SETTINGS = new TreeMap();
	private static final TreeMap PLAYER_SETTINGS = new TreeMap();

	public static final String [] COMMON_JUNK =
	{
		// Items which usually get autosold by people, regardless of the situation.
		// This includes the various meat combinables, sewer items, and stat boosters.

		"meat paste", "meat stack", "dense meat stack", "twinkly powder",
		"seal-clubbing club", "seal tooth", "helmet turtle", "pasta spoon", "ravioli hat", "disco mask", "mariachi pants",
		"moxie weed", "strongness elixir", "magicalness-in-a-can", "enchanted barbell", "concentrated magicalness pill", "giant moxie weed", "extra-strength strongness elixir", "jug-o-magicalness", "suntan lotion of moxiousness",

		// Next, some common drops in low level areas that are farmed for other
		// reasons other than those items.

		"Mad Train wine", "ice-cold fotie", "ice-cold Willer", "ice-cold Sir Schlitz", "bowl of cottage cheese", "Knob Goblin firecracker",
		"Knob Goblin pants", "Knob Goblin scimitar", "viking helmet", "bar skin", "spooky shrunken head", "dried face", "barskin hat", "spooky stick",
		"batgut", "bat guano", "ratgut", "briefcase", "taco shell", "uncooked chorizo", "Gnollish plunger", "gnoll teeth", "gnoll lips", "Gnollish toolbox",

		// Next, some common drops in medium level areas that are also farmed for
		// other reasons beyond these items.

		"hill of beans", "Knob Goblin love potion", "Knob Goblin steroids", "Imp Ale", "hot wing", "evil golden arch", "leather mask",
		"necklace chain", "hemp string", "piercing post", "phat turquoise bead", "carob chunks", "Feng Shui for Big Dumb Idiots", "homoerotic frat-paddle",
		"crowbarrr", "sunken chest", "barrrnacle", "safarrri hat", "arrrgyle socks", "charrrm", "leotarrrd", "pirate pelvis",
		"grave robbing shovel", "ghuol ears", "ghuol egg", "ghuol guolash", "lihc eye",
		"mind flayer corpse", "royal jelly", "goat beard", "sabre teeth", "t8r tots", "pail", "Trollhouse cookies", "Spam Witch sammich",
		"white satin pants", "white chocolate chips", "catgut", "white snake skin", "mullet wig",

		// High level area item drops which tend to be autosold or auto-used.

		"cocoa eggshell fragment", "glowing red eye", "amulet of extreme plot significance", "Penultimate Fantasy chest",
		"Warm Subject gift certificate", "disturbing fanfic", "probability potion", "procrastination potion", "Mick's IcyVapoHotness Rub"
	};

	public static final String [] SINGLETON_ITEMS =
	{
		"meat paste",

		// Things which are generally used during a softcore ascension should not
		// be placed on the junk list, unless they're easy to find.

		"turtle totem", "saucepan", "stolen accordion",
		"skeleton bone", "broken skull", "skeleton key", "digital key",
		"ruby W", "metallic A", "lowercase N", "heavy D", "Wand of Nagamar",
		"Richard's star key", "star crossbow", "star staff", "star sword",

		// Items which are used on tower guardians should also be considered junk,
		// but leave around one of the item just in case.

		"lime-and-chile-flavored chewing gum", "jaba&ntilde;ero-flavored chewing gum",
		"lime-and-chile-flavored chewing gum", "pickle-flavored chewing gum", "tamarind-flavored chewing gum",
		"Angry Farmer candy", "Tasty Fun Good rice candy", "marzipan skull",
		"Meleegra&trade; pills", "handsomeness potion", "wussiness potion",
		"pygmy pygment",  "thin black candle", "picture of a dead guy's girlfriend", "Black No. 2",
		"super-spikey hair gel", "Mick's IcyVapoHotness Rub", "adder bladder", "gremlin juice",
		"milky potion", "swirly potion", "bubbly potion", "smoky potion",
		"cloudy potion", "effervescent potion", "fizzy potion", "dark potion", "murky potion",
	};

	public static final String [] COMMON_MEMENTOS =
	{
		// Crimbo 2005/2006 accessories, if they're still around, probably shouldn't
		// be placed in the player's store.

		"tiny plastic Crimbo wreath", "tiny plastic Uncle Crimbo", "tiny plastic Crimbo elf",
		"tiny plastic sweet nutcracker", "tiny plastic Crimbo reindeer",
		"wreath-shaped Crimbo cookie", "bell-shaped Crimbo cookie", "tree-shaped Crimbo cookie",

		"candy stake", "spooky eggnog", "ancient unspeakable fruitcake", "gingerbread horror",
		"bat-shaped Crimboween cookie", "skull-shaped Crimboween cookie", "tombstone-shaped Crimboween cookie",

		"tiny plastic gift-wrapping vampire", "tiny plastic ancient yuletide troll",
		"tiny plastic skeletal reindeer", "tiny plastic Crimboween pentagram", "tiny plastic Scream Queen",
		"orange and black Crimboween candy",

		// Certain items tend to be used throughout an ascension, so they probably
		// shouldn't get sold, either.

		"sword behind inappropriate prepositions", "toy mercenary",

		// Collectible items should probably be sent to other players rather than
		// be autosold for no good reason.

		"stuffed cocoabo", "stuffed baby gravy fairy", "stuffed flaming gravy fairy",
		"stuffed frozen gravy fairy", "stuffed stinky gravy fairy", "stuffed spooky gravy fairy",
		"stuffed sleazy gravy fairy", "stuffed astral badger", "stuffed MagiMechTech MicroMechaMech",
		"stuffed hand turkey", "stuffed snowy owl", "stuffed scary death orb", "stuffed mind flayer",
		"stuffed undead elbow macaroni", "stuffed angry cow", "stuffed Cheshire bitten",
		"stuffed yo-yo", "rubber WWJD? bracelet", "rubber WWBD? bracelet", "rubber WWSPD? bracelet",
		"rubber WWtNSD? bracelet", "heart necklace", "spade necklace", "diamond necklace", "club necklace",
	};

	static
	{
		if ( !DATA_LOCATION.exists() )
			DATA_LOCATION.mkdirs();

		TreeMap filesToMove = new TreeMap();

		filesToMove.put( new File( SETTINGS_LOCATION, "junk_GLOBAL.txt" ), new File( DATA_LOCATION, "autosell.txt" ) );
		filesToMove.put( new File( SETTINGS_LOCATION, "profitable_GLOBAL.txt" ), new File( DATA_LOCATION, "mallsell.txt" ) );
		filesToMove.put( new File( SETTINGS_LOCATION, "memento_GLOBAL.txt" ), new File( DATA_LOCATION, "mementos.txt" ) );
		filesToMove.put( new File( SETTINGS_LOCATION, "skillsets_GLOBAL.txt" ), new File( DATA_LOCATION, "skillgroup.txt" ) );

		Object [] keys = filesToMove.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( ((File)keys[i]).exists() )
				((File)keys[i]).renameTo( (File) filesToMove.get( keys[i] ) );

		String currentName;
		File [] filelist = SETTINGS_LOCATION.listFiles();

		for ( int i = 0; i < filelist.length; ++i )
		{
			currentName = filelist[i].getName();

			if ( currentName.startsWith( "combat_" ) )
			{
				currentName = currentName.substring( 7, currentName.indexOf( ".txt" ) );
				filelist[i].renameTo( new File( SETTINGS_LOCATION, currentName + "_combat.txt" ) );
			}
			else if ( currentName.startsWith( "moods_" ) )
			{
				currentName = currentName.substring( 6, currentName.indexOf( ".txt" ) );
				filelist[i].renameTo( new File( SETTINGS_LOCATION, currentName + "_moods.txt" ) );
			}
			else if ( currentName.startsWith( "prefs_" ) )
			{
				currentName = currentName.substring( 6, currentName.indexOf( ".txt" ) );
				filelist[i].renameTo( new File( SETTINGS_LOCATION, currentName + "_prefs.txt" ) );
			}
		}

		initializeMaps();
	}

	public static final KoLSettings GLOBAL_SETTINGS = new KoLSettings( "" );

	private File settingsFile;

	private static final File junkItemsFile = new File( DATA_LOCATION, "autosell.txt" );
	private static final File singletonFile = new File( DATA_LOCATION, "singleton.txt" );
	private static final File mementoFile = new File( DATA_LOCATION, "mementos.txt" );
	private static final File profitableFile = new File( DATA_LOCATION, "mallsell.txt" );

	private static final void initializeList( LockableListModel model, File input, String [] defaults )
	{
		AdventureResult item;

		if ( !input.exists() )
		{
			for ( int i = 0; i < defaults.length; ++i )
			{
				item =  new AdventureResult( defaults[i], 1, false );
				model.add( item );
			}

			return;
		}

		try
		{
			FileInputStream istream = new FileInputStream( input );
			BufferedReader reader = new BufferedReader( new InputStreamReader( istream ) );

			String line;

			while ( (line = reader.readLine()) != null )
			{
				if ( line.equals( "" ) || line.startsWith( "[" ) )
					continue;

				if ( !TradeableItemDatabase.contains( line ) )
					continue;

				item = new AdventureResult( line, 1, false );
				if ( !model.contains( item ) )
					model.add( item );
			}

			reader.close();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void initializeLists()
	{
		KoLConstants.junkList.clear();
		initializeList( KoLConstants.junkList, junkItemsFile, COMMON_JUNK );

		KoLConstants.singletonList.clear();
		initializeList( KoLConstants.junkList, singletonFile, SINGLETON_ITEMS );
		initializeList( KoLConstants.singletonList, singletonFile, SINGLETON_ITEMS );

		KoLConstants.mementoList.clear();
		initializeList( KoLConstants.mementoList, mementoFile, COMMON_MEMENTOS );

		KoLConstants.profitableList.clear();
		initializeList( KoLConstants.profitableList, profitableFile, new String[0] );
	}

	private String noExtensionName;

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
		noExtensionName = baseUserName( characterName );
		settingsFile = new File( SETTINGS_LOCATION, noExtensionName + "_prefs.txt" );

		loadSettings();
		ensureDefaults();
	}

	public static final String baseUserName( String name )
	{
		return name == null || name.equals( "" ) ? "GLOBAL" :
			StaticEntity.globalStringReplace( name, " ", "_" ).trim().toLowerCase();
	}

	public static final boolean isUserEditable( String property )
	{	return !property.startsWith( "saveState" );
	}

	public static final boolean isGlobalProperty( String name )
	{
		return CLIENT_SETTINGS.containsKey( name ) || name.startsWith( "saveState" ) || name.startsWith( "displayName" ) ||
			name.startsWith( "getBreakfast" ) || name.startsWith( "autoPlant" ) || name.startsWith( "visitRumpus" ) ||
			name.startsWith( "initialFrames" ) || name.startsWith( "initialDesktop" );
	}

	public String getProperty( String name )
	{
		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (GLOBAL_SETTINGS == null || this != GLOBAL_SETTINGS) )
		{
			String value = GLOBAL_SETTINGS.getProperty( name );
			return value == null ? "" : value;
		}
		else if ( !isGlobalProperty && this == GLOBAL_SETTINGS )
			return "";

		String value = super.getProperty( name );
		return value == null ? "" : RequestEditorKit.getUnicode( value );
	}

	public Object setProperty( String name, String value )
	{
		if ( value == null )
			return "";

		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (GLOBAL_SETTINGS == null || this != GLOBAL_SETTINGS) )
			return GLOBAL_SETTINGS.setProperty( name, value );
		else if ( !isGlobalProperty && this == GLOBAL_SETTINGS )
			return "";

		// All tests passed.  Now, go ahead and execute the
		// set property and return the old value.

		String oldValue = this.getProperty( name );
		value = RequestEditorKit.getEntities( value );

		if ( oldValue != null && oldValue.equals( value ) )
			return oldValue;

		super.setProperty( name, value );

		if ( checkboxMap.containsKey( name ) )
		{
			ArrayList list = (ArrayList) checkboxMap.get( name );
			for ( int i = 0; i < list.size(); ++i )
			{
				WeakReference reference = (WeakReference) list.get(i);
				JCheckBox item = (JCheckBox) reference.get();
				if ( item != null )
					item.setSelected( value.equals( "true" ) );
			}
		}

		return oldValue == null ? "" : oldValue;
	}

	public static final void registerCheckbox( String name, JCheckBox checkbox )
	{
		ArrayList list = null;

		if ( checkboxMap.containsKey( name ) )
		{
			list = (ArrayList) checkboxMap.get( name );
		}
		else
		{
			list = new ArrayList();
			checkboxMap.put( name, list );
		}

		list.add( new WeakReference( checkbox ) );
	}

	public static final void saveFlaggedItemList()
	{
		AdventureResult item;

		LogStream ostream = LogStream.openStream( junkItemsFile, true );

		for ( int i = 0; i < KoLConstants.junkList.size(); ++i )
		{
			item = ((AdventureResult) KoLConstants.junkList.get(i));
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( singletonFile, true );
		for ( int i = 0; i < KoLConstants.singletonList.size(); ++i )
		{
			item = (AdventureResult)KoLConstants.singletonList.get(i);
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( mementoFile, true );
		for ( int i = 0; i < KoLConstants.mementoList.size(); ++i )
		{
			item = (AdventureResult)KoLConstants.mementoList.get(i);
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( profitableFile, true );
		for ( int i = 0; i < KoLConstants.profitableList.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.profitableList.get(i);
			ostream.println( item.getCount() + " " + item.getName() );
		}

		ostream.close();
	}

	public synchronized void saveSettings()
	{
		if ( this.initializingDefaults )
			return;

		SETTINGS_LOCATION.mkdirs();

		try
		{
			// Determine the contents of the file by
			// actually printing them.

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			this.store( ostream, KoLConstants.VERSION_NAME );

			String [] lines = ostream.toString().split( KoLConstants.LINE_BREAK );
			Arrays.sort( lines );

			ostream.reset();

			for ( int i = 0; i < lines.length; ++i )
			{
				if ( lines[i].startsWith( "#" ) )
					continue;

				ostream.write( lines[i].getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}

			if ( this.settingsFile.exists() )
				this.settingsFile.delete();

			this.settingsFile.createNewFile();
			ostream.writeTo( new FileOutputStream( this.settingsFile ) );
		}
		catch ( IOException e )
		{
			// This should not happen.
		}
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	private void loadSettings()
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !this.settingsFile.exists() )
				return;

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( this.settingsFile );
			this.load( istream );

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
			this.settingsFile.delete();
		}
	}

	private static final void initializeMaps()
	{
		// Do not initialize the maps more than once, as this
		// would not serve any purpose.

		CLIENT_SETTINGS.put( "addChatCommandLine", "false" );
		CLIENT_SETTINGS.put( "addCreationQueue", "true" );
		CLIENT_SETTINGS.put( "addExitMenuItems", String.valueOf( !System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		CLIENT_SETTINGS.put( "addStatusBarToFrames", "false" );
		CLIENT_SETTINGS.put( "allowBreakfastBurning", "true" );
		CLIENT_SETTINGS.put( "allowCloseableDesktopTabs", "false" );
		CLIENT_SETTINGS.put( "allowEncounterRateBurning", "true" );
		CLIENT_SETTINGS.put( "allowGenericUse", "false" );
		CLIENT_SETTINGS.put( "allowNonMoodBurning", "true" );
		CLIENT_SETTINGS.put( "alwaysGetBreakfast", "false" );
		CLIENT_SETTINGS.put( "autoBuyRestores", "true" );
		CLIENT_SETTINGS.put( "autoLogin", "" );
		CLIENT_SETTINGS.put( "autoPlantHardcore", "false" );
		CLIENT_SETTINGS.put( "autoPlantSoftcore", "false" );
		CLIENT_SETTINGS.put( "autoSatisfyWithNPCs", "true" );
		CLIENT_SETTINGS.put( "autoSatisfyWithMall", "true" );
		CLIENT_SETTINGS.put( "autoSatisfyWithStash", "false" );
		CLIENT_SETTINGS.put( "avoidInvertingTabs", "false" );
		CLIENT_SETTINGS.put( "breakfastSoftcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "breakfastHardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "browserBookmarks", "" );
		CLIENT_SETTINGS.put( "cacheMallSearches", "false" );
		CLIENT_SETTINGS.put( "charsheetDropdown", "1" );
		CLIENT_SETTINGS.put( "chatFontSize", System.getProperty( "os.name" ).startsWith( "Mac" ) ? "medium" : "small" );
		CLIENT_SETTINGS.put( "chatStyle", "0" );
		CLIENT_SETTINGS.put( "cloverProtectActive", "true" );
		CLIENT_SETTINGS.put( "commandLineNamespace", "" );
		CLIENT_SETTINGS.put( "completeHealthRestore", "false" );
		CLIENT_SETTINGS.put( "defaultBorderColor", "blue" );
		CLIENT_SETTINGS.put( "defaultDropdown1", "0" );
		CLIENT_SETTINGS.put( "defaultDropdown2", "1" );
		CLIENT_SETTINGS.put( "defaultDropdownSplit", "0" );
		CLIENT_SETTINGS.put( "defaultLimit", "5" );
		CLIENT_SETTINGS.put( "defaultLoginServer", "1" );
		CLIENT_SETTINGS.put( "eSoluScriptType", "0" );
		CLIENT_SETTINGS.put( "grabCloversHardcore", "false" );
		CLIENT_SETTINGS.put( "grabCloversSoftcore", "true" );
		CLIENT_SETTINGS.put( "greenScreenProtection", "false" );
		CLIENT_SETTINGS.put( "guiUsesOneWindow", "false" );
		CLIENT_SETTINGS.put( "highlightList", "" );
		CLIENT_SETTINGS.put( "http.proxyHost", "" );
		CLIENT_SETTINGS.put( "http.proxyPort", "" );
		CLIENT_SETTINGS.put( "http.proxyUser", "" );
		CLIENT_SETTINGS.put( "http.proxyPassword", "" );
		CLIENT_SETTINGS.put( "proxySet", "false" );
		CLIENT_SETTINGS.put( "ignoreAutoAttack", "false" );
		CLIENT_SETTINGS.put( "initialDesktop", "AdventureFrame,CommandDisplayFrame,MallSearchFrame,SkillBuffFrame" );
		CLIENT_SETTINGS.put( "initialFrames", "EventsFrame" );
		CLIENT_SETTINGS.put( "itemManagerIndex", "0" );
		CLIENT_SETTINGS.put( "lastUsername", "" );
		CLIENT_SETTINGS.put( "logChatMessages", "true" );
		CLIENT_SETTINGS.put( "loginServerName", "" );
		CLIENT_SETTINGS.put( "loginWindowLogo", "lantern.jpg" );
		CLIENT_SETTINGS.put( "loginRecoveryHardcore", "false" );
		CLIENT_SETTINGS.put( "loginRecoverySoftcore", "true" );
		CLIENT_SETTINGS.put( "loginScript", "" );
		CLIENT_SETTINGS.put( "logoutScript", "" );

		CLIENT_SETTINGS.put( "logAcquiredItems", "true" );
		CLIENT_SETTINGS.put( "logBattleAction", "true" );
		CLIENT_SETTINGS.put( "logFamiliarActions", "false" );
		CLIENT_SETTINGS.put( "logGainMessages", "true" );
		CLIENT_SETTINGS.put( "logMonsterHealth", "false" );
		CLIENT_SETTINGS.put( "logReverseOrder", "false" );
		CLIENT_SETTINGS.put( "logStatGains", "true" );
		CLIENT_SETTINGS.put( "logStatusEffects", "false" );

		CLIENT_SETTINGS.put( "mapLoadsMiniBrowser", "false" );
		CLIENT_SETTINGS.put( "mementoListActive", "false" );
		CLIENT_SETTINGS.put( "pathedSummonsHardcore", "true" );
		CLIENT_SETTINGS.put( "pathedSummonsSoftcore", "false" );
		CLIENT_SETTINGS.put( "previousNotifyList", "<>" );
		CLIENT_SETTINGS.put( "previousUpdateVersion", KoLConstants.VERSION_NAME );
		CLIENT_SETTINGS.put( "protectAgainstOverdrink", "true" );

		CLIENT_SETTINGS.put( "readManualHardcore", "true" );
		CLIENT_SETTINGS.put( "readManualSoftcore", "true" );
		CLIENT_SETTINGS.put( "relayAddsGraphicalCLI", "false" );
		CLIENT_SETTINGS.put( "relayAddsKoLSimulator", "false" );
		CLIENT_SETTINGS.put( "relayAddsMonsterHealth", "false" );
		CLIENT_SETTINGS.put( "relayAddsQuickScripts", "false" );
		CLIENT_SETTINGS.put( "relayAddsRestoreLinks", "true" );
		CLIENT_SETTINGS.put( "relayAddsRoundNumber", "false" );
		CLIENT_SETTINGS.put( "relayAddsUpArrowLinks", "true" );
		CLIENT_SETTINGS.put( "relayAddsUseLinks", "true" );
		CLIENT_SETTINGS.put( "relayAllowsOverrides", "false" );
		CLIENT_SETTINGS.put( "relayAlwaysBuysGum", "true" );
		CLIENT_SETTINGS.put( "relayBrowserOnly", "false" );
		CLIENT_SETTINGS.put( "relayFormatsChatText", "true" );
		CLIENT_SETTINGS.put( "relayHidesJunkMallItems", "true" );
		CLIENT_SETTINGS.put( "relayMaintainsEffects", "false" );
		CLIENT_SETTINGS.put( "relayMaintainsHealth", "true" );
		CLIENT_SETTINGS.put( "relayMaintainsMana", "false" );
		CLIENT_SETTINGS.put( "relayBrowserOnly", "false" );
		CLIENT_SETTINGS.put( "relayTextualizesEffects", "false" );
		CLIENT_SETTINGS.put( "relayUsesCachedImages", "false" );
		CLIENT_SETTINGS.put( "relayUsesInlineLinks", "false" );
		CLIENT_SETTINGS.put( "relayUsesIntegratedChat", "false" );
		CLIENT_SETTINGS.put( "relayViewsCustomItems", "false" );

		CLIENT_SETTINGS.put( "saveState", "" );
		CLIENT_SETTINGS.put( "saveStateActive", "" );
		CLIENT_SETTINGS.put( "scriptButtonPosition", "0" );
		CLIENT_SETTINGS.put( "scriptList", "restore hp | restore mp" );
		CLIENT_SETTINGS.put( "showAllRequests", "false" );
		CLIENT_SETTINGS.put( "swingLookAndFeel", "" );
		CLIENT_SETTINGS.put( "switchEquipmentForBuffs", "true" );
		CLIENT_SETTINGS.put( "testSocketTimeout", "true" );
		CLIENT_SETTINGS.put( "toolbarPosition", "1" );

		CLIENT_SETTINGS.put( "useDecoratedTabs", String.valueOf( !System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		CLIENT_SETTINGS.put( "innerTabColor", "#8ca9ff" );
		CLIENT_SETTINGS.put( "outerTabColor", "#0f46b4" );
		CLIENT_SETTINGS.put( "innerChatColor", "#ffa98c" );
		CLIENT_SETTINGS.put( "outerChatColor", "#b4460f" );

		CLIENT_SETTINGS.put( "useChatMonitor", "false" );
		CLIENT_SETTINGS.put( "useChatToolbar", "true" );
		CLIENT_SETTINGS.put( "useContactsFrame", "true" );
		CLIENT_SETTINGS.put( "useFastOutfitSwitch", "true" );
		CLIENT_SETTINGS.put( "useLowBandwidthRadio", "false" );
		CLIENT_SETTINGS.put( "useSeparateChannels", "true" );
		CLIENT_SETTINGS.put( "useShinyTabbedChat", "true" );
		CLIENT_SETTINGS.put( "useSystemTrayIcon", "false" );
		CLIENT_SETTINGS.put( "useTabbedChatFrame", "true" );
		CLIENT_SETTINGS.put( "useToolbars", "true" );
		CLIENT_SETTINGS.put( "visitRumpusHardcore", "true" );
		CLIENT_SETTINGS.put( "visitRumpusSoftcore", "true" );

		// Individual player settings which are not set on
		// a global level.

		PLAYER_SETTINGS.put( "autoAbortThreshold", "-0.1" );
		PLAYER_SETTINGS.put( "autoRepairBoxServants", "true" );
		PLAYER_SETTINGS.put( "battleAction", "attack with weapon" );
		PLAYER_SETTINGS.put( "betweenBattleScript", "" );
		PLAYER_SETTINGS.put( "breakfastCompleted", "false" );
		PLAYER_SETTINGS.put( "buffBotCasting", "" );
		PLAYER_SETTINGS.put( "buffBotMessageDisposal", "0" );
		PLAYER_SETTINGS.put( "buffBotPhilanthropyType", "1" );
		PLAYER_SETTINGS.put( "candyHeartSummons", "0" );
		PLAYER_SETTINGS.put( "chatbotScript", "" );
		PLAYER_SETTINGS.put( "chatbotScriptExecuted", "false" );
		PLAYER_SETTINGS.put( "chosenTrip", "" );
		PLAYER_SETTINGS.put( "cocktailSummons", "0" );
		PLAYER_SETTINGS.put( "currentBountyItem", "0" );
		PLAYER_SETTINGS.put( "currentFullness", "0" );
		PLAYER_SETTINGS.put( "currentHippyStore", "none" );
		PLAYER_SETTINGS.put( "currentMojoFilters", "0" );
		PLAYER_SETTINGS.put( "currentMood", "default" );
		PLAYER_SETTINGS.put( "currentPvpVictories", "" );
		PLAYER_SETTINGS.put( "currentSpleenUse", "0" );
		PLAYER_SETTINGS.put( "currentWheelPosition", "muscle" );
		PLAYER_SETTINGS.put( "defaultAutoAttack", "0" );
		PLAYER_SETTINGS.put( "defaultFlowerLossMessage", "" );
		PLAYER_SETTINGS.put( "defaultFlowerWinMessage", "" );
		PLAYER_SETTINGS.put( "demonName1", "" );
		PLAYER_SETTINGS.put( "demonName2", "" );
		PLAYER_SETTINGS.put( "demonName3", "" );
		PLAYER_SETTINGS.put( "demonName4", "" );
		PLAYER_SETTINGS.put( "demonName5", "" );
		PLAYER_SETTINGS.put( "grimoireSummons", "0" );
		PLAYER_SETTINGS.put( "hpAutoRecovery", "-0.1" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryTarget", "1.0" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryItems", "cannelloni cocoon;scroll of drastic healing;tongue of the walrus;lasagna bandages;doc galaktik's ailment ointment" );
		PLAYER_SETTINGS.put( "invalidBuffMessage", "You sent an amount which does not correspond to a valid buff amount." );
		PLAYER_SETTINGS.put( "lastAdventure", "" );
		PLAYER_SETTINGS.put( "lastCouncilVisit", "0" );
		PLAYER_SETTINGS.put( "lastEmptiedStorage", "-1" );
		PLAYER_SETTINGS.put( "lastFilthClearance", "-1" );

		PLAYER_SETTINGS.put( "lastDustyBottleReset", "-1" );
		PLAYER_SETTINGS.put( "lastDustyBottle2271", "" );
		PLAYER_SETTINGS.put( "lastDustyBottle2272", "" );
		PLAYER_SETTINGS.put( "lastDustyBottle2273", "" );
		PLAYER_SETTINGS.put( "lastDustyBottle2274", "" );
		PLAYER_SETTINGS.put( "lastDustyBottle2275", "" );
		PLAYER_SETTINGS.put( "lastDustyBottle2276", "" );

		PLAYER_SETTINGS.put( "lastBangPotionReset", "-1" );
		PLAYER_SETTINGS.put( "lastBangPotion819", "" );
		PLAYER_SETTINGS.put( "lastBangPotion820", "" );
		PLAYER_SETTINGS.put( "lastBangPotion821", "" );
		PLAYER_SETTINGS.put( "lastBangPotion822", "" );
		PLAYER_SETTINGS.put( "lastBangPotion823", "" );
		PLAYER_SETTINGS.put( "lastBangPotion824", "" );
		PLAYER_SETTINGS.put( "lastBangPotion825", "" );
		PLAYER_SETTINGS.put( "lastBangPotion826", "" );
		PLAYER_SETTINGS.put( "lastBangPotion827", "" );

		PLAYER_SETTINGS.put( "lastStoneSphereReset", "-1" );
		PLAYER_SETTINGS.put( "lastStoneSphere2174", "" );
		PLAYER_SETTINGS.put( "lastStoneSphere2175", "" );
		PLAYER_SETTINGS.put( "lastStoneSphere2176", "" );
		PLAYER_SETTINGS.put( "lastStoneSphere2177", "" );

		PLAYER_SETTINGS.put( "lastBreakfast", "-1" );
		PLAYER_SETTINGS.put( "lastCounterDay", "-1" );
		PLAYER_SETTINGS.put( "lastGalleryUnlock", "-1" );
		PLAYER_SETTINGS.put( "lastMessageId", "" );
		PLAYER_SETTINGS.put( "lastQuartetAscension", "-1" );
		PLAYER_SETTINGS.put( "lastQuartetRequest", "0" );
		PLAYER_SETTINGS.put( "lastSecondFloorUnlock", "-1" );
		PLAYER_SETTINGS.put( "lastTowerClimb", "-1" );
		PLAYER_SETTINGS.put( "luckySewerAdventure", "stolen accordion" );
		PLAYER_SETTINGS.put( "manaBurningThreshold", "-0.1" );

		PLAYER_SETTINGS.put( "munchiesPillsUsed", "0" );
		PLAYER_SETTINGS.put( "mpAutoRecovery", "0.0" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryTarget", "0.3" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryItems", "phonics down;knob goblin superseltzer;mountain stream soda;magical mystery juice;knob goblin seltzer;cherry cloaca cola;soda water" );
		PLAYER_SETTINGS.put( "noodleSummons", "0" );
		PLAYER_SETTINGS.put( "plantingDay", "-1" );
		PLAYER_SETTINGS.put( "plantingDate", "" );
		PLAYER_SETTINGS.put( "plantingLength", "" );
		PLAYER_SETTINGS.put( "plantingScript", "" );
		PLAYER_SETTINGS.put( "preBlackbirdFamiliar", "" );
		PLAYER_SETTINGS.put( "reagentSummons", "0" );
		PLAYER_SETTINGS.put( "relayAddsCustomCombat", "true" );
		PLAYER_SETTINGS.put( "relayCounters", "" );
		PLAYER_SETTINGS.put( "requireBoxServants", "true" );
		PLAYER_SETTINGS.put( "retrieveContacts", "true" );
		PLAYER_SETTINGS.put( "showGainsPerUnit", "false" );
		PLAYER_SETTINGS.put( "snowconeSummons", "0" );
		PLAYER_SETTINGS.put( "thanksMessage", "Thank you for the donation!" );
		PLAYER_SETTINGS.put( "trapperOre", "chrome ore" );
		PLAYER_SETTINGS.put( "violetFogGoal", "0" );
		PLAYER_SETTINGS.put( "visibleBrowserInventory", "" );

		// These are settings related to the tavern faucet
		// used to make the interface friendlier.

		PLAYER_SETTINGS.put( "lastTavernSquare", "0" );
		PLAYER_SETTINGS.put( "lastTavernAscension", "0" );
		PLAYER_SETTINGS.put( "tavernLayout", "0000000000000000000000000" );

		// Yay for the Louvre.

		PLAYER_SETTINGS.put( "lastLouvreMap", "0" );
		PLAYER_SETTINGS.put( "louvreLayout", "" );
		PLAYER_SETTINGS.put( "louvreDesiredGoal", "7" );
		PLAYER_SETTINGS.put( "louvreGoal", "0" );

		// These are settings related to choice adventures.
		// Ensure that they exist, and if they do not, load
		// them to their default settings.

		PLAYER_SETTINGS.put( "lastChoiceUpdate", "" );
		PLAYER_SETTINGS.put( "choiceAdventure2", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure3", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure4", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure5", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure6", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure7", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure8", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure9", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure10", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure11", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure12", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure14", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure15", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure16", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure17", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure18", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure19", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure20", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure21", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure22", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure23", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure24", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure25", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure26", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure27", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure28", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure29", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure40", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure41", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure42", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure45", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure46", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure47", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure71", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure72", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure73", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure74", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure75", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure76", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure77", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure78", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure79", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure80", "99" );
		PLAYER_SETTINGS.put( "choiceAdventure81", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure82", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure83", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure84", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure85", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure86", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure87", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure88", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure89", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure90", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure91", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure105", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure106", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure107", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure108", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure109", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure110", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure111", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure112", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure113", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure114", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure115", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure116", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure117", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure118", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure120", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure123", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure125", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure126", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure127", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure129", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure130", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure131", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure132", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure134", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure135", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure136", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure137", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure138", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure139", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure140", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure141", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure142", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure143", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure144", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure145", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure146", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure147", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure148", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure149", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure151", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure152", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure153", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure154", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure155", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure156", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure157", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure158", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure159", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure160", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure161", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure162", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure163", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure164", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure165", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure166", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure167", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure168", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure169", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure170", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure171", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure172", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure177", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure178", "1" );
	}

	private void ensureChoiceDefaults()
	{
		if ( GLOBAL_SETTINGS == null || this == GLOBAL_SETTINGS )
			return;

		if ( super.getProperty( "lastChoiceUpdate" ).equals( KoLConstants.VERSION_NAME ) )
			return;

		String setting;
		super.setProperty( "lastChoiceUpdate", KoLConstants.VERSION_NAME );

		for ( int i = 0; i < AdventureDatabase.CHOICE_ADV_SPOILERS.length; ++i )
		{
			setting = AdventureDatabase.CHOICE_ADV_SPOILERS[i].getSetting();
			if ( !forceChoiceDefault( StaticEntity.parseInt( setting.substring(15) ) ) )
				continue;

			super.setProperty( setting, (String) PLAYER_SETTINGS.get( setting ) );
		}
	}

	private static final boolean forceChoiceDefault( int choiceId )
	{
		switch ( choiceId )
		{
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
		case 12:
		case 26:
		case 27:
		case 28:
		case 29:
		case 77:
		case 78:
		case 79:
		case 80:
		case 81:
		case 86:
		case 87:
		case 88:
		case 89:
		case 91:
		case 152:
			return false;

		default:
			return true;
		}
	}

	public static final void printDefaults()
	{
		LogStream ostream = LogStream.openStream( "choices.txt", true );

		ostream.println( "[u]Configurable[/u]" );
		ostream.println();

		AdventureDatabase.setChoiceOrdering( false );
		Arrays.sort( AdventureDatabase.CHOICE_ADVS );
		Arrays.sort( AdventureDatabase.CHOICE_ADV_SPOILERS );

		printDefaults( AdventureDatabase.CHOICE_ADVS, ostream );

		ostream.println();
		ostream.println();
		ostream.println( "[u]Not Configurable[/u]" );
		ostream.println();

		printDefaults( AdventureDatabase.CHOICE_ADV_SPOILERS, ostream );

		AdventureDatabase.setChoiceOrdering( true );
		Arrays.sort( AdventureDatabase.CHOICE_ADVS );
		Arrays.sort( AdventureDatabase.CHOICE_ADV_SPOILERS );

		ostream.close();
	}

	private static final void printDefaults( ChoiceAdventure [] choices, LogStream ostream )
	{
		for ( int i = 0; i < choices.length; ++i )
		{
			String setting = choices[i].getSetting();
			if ( !forceChoiceDefault( StaticEntity.parseInt( setting.substring(15) ) ) )
				continue;

			int defaultOption = StaticEntity.parseInt( (String) PLAYER_SETTINGS.get( setting ) ) - 1;

			ostream.print( "[" + setting.substring(15) + "] " );
			ostream.print( choices[i].getName() + ": " );

			int printedCount = 0;
			String [] options = choices[i].getOptions();

			ostream.print( options[defaultOption] + " [color=gray](" );

			for ( int j = 0; j < options.length; ++j )
			{
				if ( j == defaultOption )
					continue;

				if ( printedCount != 0 )
					ostream.print( ", " );

				++printedCount;
				ostream.print( options[j] );
			}

			ostream.println( ")[/color]" );
		}
	}

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private void ensureDefaults()
	{
		this.initializingDefaults = true;

		// If this is the set of global settings, be sure
		// to initialize the global settings.

		if ( this.noExtensionName.equals( "GLOBAL" ) )
		{
			Object [] keys = CLIENT_SETTINGS.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
				if ( !this.containsKey( keys[i] ) )
					super.setProperty( (String) keys[i], (String) CLIENT_SETTINGS.get( keys[i] ) );

			this.initializingDefaults = false;
			return;
		}

		// Otherwise, initialize the client-specific settings.
		// No global settings will be loaded.

		Object [] keys = PLAYER_SETTINGS.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !this.containsKey( keys[i] ) )
				super.setProperty( (String) keys[i], (String) PLAYER_SETTINGS.get( keys[i] ) );

		this.ensureChoiceDefaults();
		this.initializingDefaults = false;
	}
}
