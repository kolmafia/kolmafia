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
import net.sourceforge.kolmafia.AdventureDatabase.ChoiceAdventure;

public class KoLSettings extends Properties implements KoLConstants
{
	private boolean valuesChanged = false;
	private static final TreeMap checkboxMap = new TreeMap();

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
		"necklace chain", "hemp string", "piercing post", "phat turquoise bead", "carob chunks", "Feng Shui for Big Dumb Idiots",
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

		// Common outfit pieces should also be kept

		"bugbear beanie", "bugbear bungguard",
		"filthy knitted dread sack", "filthy corduroys",
		"homoerotic frat-paddle", "Orcish baseball cap", "Orcish cargo shorts",
		"Knob Goblin harem veil", "Knob Goblin harem pants",
		"Knob Goblin elite helm", "Knob Goblin elite polearm", "Knob Goblin elite pants",
		"eyepatch", "swashbuckling pants", "stuffed shoulder parrot",
		"Cloaca-Cola fatigues", "Cloaca-Cola helmet", "Cloaca-Cola shield",
		"Dyspepsi-Cola fatigues", "Dyspepsi-Cola helmet", "Dyspepsi-Cola shield",
		"bullet-proof corduroys", "round purple sunglasses", "reinforced beaded headband",
		"beer helmet", "distressed denim pants", "bejeweled pledge pin",

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

	private static final TreeMap GLOBAL_MAP = new TreeMap();
	private static final TreeMap USER_MAP = new TreeMap();

	static
	{
		if ( !DATA_LOCATION.exists() )
			DATA_LOCATION.mkdirs();
		if ( !SETTINGS_LOCATION.exists() )
			SETTINGS_LOCATION.mkdirs();

		// Move all files to ~/Library/Application Support/KoLmafia
		// if the user is on a Macintosh, just for consistency.

		File source;

		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			source = new File( System.getProperty( "user.dir" ), DATA_DIRECTORY );
			if ( source.exists() )
				source.renameTo( DATA_LOCATION );
			source = new File( System.getProperty( "user.dir" ), IMAGE_DIRECTORY );
			if ( source.exists() )
				source.renameTo( IMAGE_LOCATION );
			source = new File( System.getProperty( "user.dir" ), SETTINGS_DIRECTORY );
			if ( source.exists() )
				source.renameTo( SETTINGS_LOCATION );
			source = new File( System.getProperty( "user.dir" ), PLOTS_DIRECTORY );
			if ( source.exists() )
				source.renameTo( PLOTS_LOCATION );
			source = new File( System.getProperty( "user.dir" ), SCRIPT_DIRECTORY );
			if ( source.exists() )
				source.renameTo( SCRIPT_LOCATION );
			source = new File( System.getProperty( "user.dir" ), SESSIONS_DIRECTORY );
			if ( source.exists() )
				source.renameTo( SESSIONS_LOCATION );
			source = new File( System.getProperty( "user.dir" ), RELAY_DIRECTORY );
			if ( source.exists() )
				source.renameTo( RELAY_LOCATION );
		}

		initializeMaps();
	}

	private static KoLSettings globalSettings = new KoLSettings( "" );
	private static KoLSettings userSettings = globalSettings;

	private File userSettingsFile;

	private static final File junkItemsFile = new File( DATA_LOCATION, "autosell.txt" );
	private static final File singletonFile = new File( DATA_LOCATION, "singleton.txt" );
	private static final File mementoFile = new File( DATA_LOCATION, "mementos.txt" );
	private static final File profitableFile = new File( DATA_LOCATION, "mallsell.txt" );

	private static final void initializeList( LockableListModel model, File input, String [] defaults )
	{
		AdventureResult item;

		if ( defaults == SINGLETON_ITEMS || !input.exists() )
		{
			for ( int i = 0; i < defaults.length; ++i )
			{
				item =  new AdventureResult( defaults[i], 1, false );
				model.add( item );
			}
		}

		if ( !input.exists() )
			return;

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
		junkList.clear();
		initializeList( junkList, junkItemsFile, COMMON_JUNK );

		singletonList.clear();
		initializeList( junkList, singletonFile, SINGLETON_ITEMS );
		initializeList( singletonList, singletonFile, SINGLETON_ITEMS );

		mementoList.clear();
		initializeList( mementoList, mementoFile, COMMON_MEMENTOS );

		profitableList.clear();
		initializeList( profitableList, profitableFile, new String[0] );
	}

	public static final void reset( String username )
	{
		if ( username.equals( "" ) )
		{
			userSettings = globalSettings;
			return;
		}

		userSettings = new KoLSettings( username );

		CombatSettings.restoreDefaults();
		MoodSettings.restoreDefaults();
	}

	private String noExtensionName;

	/**
	 * Constructs a userSettings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	characterName	The name of the character this userSettings file represents
	 */

	private KoLSettings( String characterName )
	{
		noExtensionName = baseUserName( characterName );
		userSettingsFile = new File( SETTINGS_LOCATION, noExtensionName + "_prefs.txt" );

		loadFromFile();
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

	private static final String getPropertyName( String player, String name )
	{	return player == null || player.equals( "" ) ? name : name + "." + KoLSettings.baseUserName( player );
	}

	public static final void setUserProperty( String name, String value )
	{	userSettings.setProperty( name, value );
	}

	public static final String getUserProperty( String name )
	{	return userSettings.getProperty( name );
	}

	public static final void setGlobalProperty( String name, String value )
	{	setGlobalProperty( KoLCharacter.getUserName(), name, value );
	}

	public static final String getGlobalProperty( String name )
	{	return getGlobalProperty( KoLCharacter.getUserName(), name );
	}

	public static final void setGlobalProperty( String player, String name, String value )
	{	userSettings.setProperty( getPropertyName( player, name ), value );
	}

	public static final String getGlobalProperty( String player, String name )
	{	return userSettings.getProperty( getPropertyName( player, name ) );
	}

	public static final boolean getBooleanProperty( String name )
	{	return getUserProperty( name ).equals( "true" );
	}

	public static final int getIntegerProperty( String name )
	{	return StaticEntity.parseInt( getUserProperty( name ) );
	}

	public static final float getFloatProperty( String name )
	{	return StaticEntity.parseFloat( getUserProperty( name ) );
	}

	public static final boolean isGlobalProperty( String name )
	{
		return GLOBAL_MAP.containsKey( name ) || name.startsWith( "saveState" ) || name.startsWith( "displayName" ) ||
			name.startsWith( "getBreakfast" ) || name.startsWith( "autoPlant" ) || name.startsWith( "visitRumpus" ) ||
			name.startsWith( "initialFrames" ) || name.startsWith( "initialDesktop" );
	}

	public String getProperty( String name )
	{
		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (globalSettings == null || this != globalSettings) )
		{
			String value = globalSettings.getProperty( name );
			return value == null ? "" : value;
		}
		else if ( !isGlobalProperty && this == globalSettings )
			return "";

		String value = super.getProperty( name );
		return value == null ? "" : RequestEditorKit.getUnicode( value );
	}

	public Object setProperty( String name, String value )
	{
		if ( value == null )
			return "";

		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (globalSettings == null || this != globalSettings) )
			return globalSettings.setProperty( name, value );
		else if ( !isGlobalProperty && this == globalSettings )
			return "";

		// All tests passed.  Now, go ahead and execute the
		// set property and return the old value.

		String oldValue = this.getProperty( name );
		value = RequestEditorKit.getEntities( value );

		if ( oldValue != null && oldValue.equals( value ) )
			return oldValue;

		this.valuesChanged = true;
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

		this.saveToFile();
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

		for ( int i = 0; i < junkList.size(); ++i )
		{
			item = ((AdventureResult) junkList.get(i));
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( singletonFile, true );
		for ( int i = 0; i < singletonList.size(); ++i )
		{
			item = (AdventureResult)singletonList.get(i);
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( mementoFile, true );
		for ( int i = 0; i < mementoList.size(); ++i )
		{
			item = (AdventureResult)mementoList.get(i);
			ostream.println( item.getName() );
		}

		ostream.close();

		ostream = LogStream.openStream( profitableFile, true );
		for ( int i = 0; i < profitableList.size(); ++i )
		{
			item = (AdventureResult) profitableList.get(i);
			ostream.println( item.getCount() + " " + item.getName() );
		}

		ostream.close();
	}

	public void saveToFile()
	{
		if ( !this.valuesChanged )
			return;

		SETTINGS_LOCATION.mkdirs();

		try
		{
			// Determine the contents of the file by
			// actually printing them.

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			this.store( ostream, VERSION_NAME );

			String [] lines = ostream.toString().split( LINE_BREAK );
			Arrays.sort( lines );

			ostream.reset();

			for ( int i = 0; i < lines.length; ++i )
			{
				if ( lines[i].startsWith( "#" ) )
					continue;

				ostream.write( lines[i].getBytes() );
				ostream.write( LINE_BREAK.getBytes() );
			}

			if ( this.userSettingsFile.exists() )
				this.userSettingsFile.delete();

			this.userSettingsFile.createNewFile();
			ostream.writeTo( new FileOutputStream( this.userSettingsFile ) );
		}
		catch ( IOException e )
		{
			// This should not happen.
		}
	}

	/**
	 * Loads the userSettings located in the given file into this object.
	 * Note that all userSettings are overridden; if the given file does
	 * not exist, the current global userSettings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	private void loadFromFile()
	{
		try
		{
			// First guarantee that a userSettings file exists with
			// the appropriate Properties data.

			if ( !this.userSettingsFile.exists() )
				return;

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( this.userSettingsFile );
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
			// Somehow, the userSettings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			StaticEntity.printStackTrace( e2 );
			this.userSettingsFile.delete();
		}
	}

	private static final void initializeMaps()
	{
		// Do not initialize the maps more than once, as this
		// would not serve any purpose.

		GLOBAL_MAP.put( "addChatCommandLine", "false" );
		GLOBAL_MAP.put( "addCreationQueue", "true" );
		GLOBAL_MAP.put( "addExitMenuItems", String.valueOf( !System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		GLOBAL_MAP.put( "addStatusBarToFrames", "false" );
		GLOBAL_MAP.put( "allowBreakfastBurning", "true" );
		GLOBAL_MAP.put( "allowCloseableDesktopTabs", "false" );
		GLOBAL_MAP.put( "allowEncounterRateBurning", "true" );
		GLOBAL_MAP.put( "allowGenericUse", "false" );
		GLOBAL_MAP.put( "allowNegativeTally", "true" );
		GLOBAL_MAP.put( "allowNonMoodBurning", "true" );
		GLOBAL_MAP.put( "alwaysGetBreakfast", "false" );
		GLOBAL_MAP.put( "autoBuyRestores", "true" );
		GLOBAL_MAP.put( "autoLogin", "" );
		GLOBAL_MAP.put( "autoPlantHardcore", "false" );
		GLOBAL_MAP.put( "autoPlantSoftcore", "false" );
		GLOBAL_MAP.put( "autoSatisfyWithNPCs", "true" );
		GLOBAL_MAP.put( "autoSatisfyWithMall", "true" );
		GLOBAL_MAP.put( "autoSatisfyWithStash", "false" );
		GLOBAL_MAP.put( "avoidInvertingTabs", "false" );
		GLOBAL_MAP.put( "breakfastSoftcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		GLOBAL_MAP.put( "breakfastHardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		GLOBAL_MAP.put( "browserBookmarks", "" );
		GLOBAL_MAP.put( "cacheMallSearches", "false" );
		GLOBAL_MAP.put( "charsheetDropdown", "1" );
		GLOBAL_MAP.put( "chatFontSize", System.getProperty( "os.name" ).startsWith( "Mac" ) ? "medium" : "small" );
		GLOBAL_MAP.put( "chatLinksUseRelay", "false" );
		GLOBAL_MAP.put( "chatStyle", "0" );
		GLOBAL_MAP.put( "cloverProtectActive", "true" );
		GLOBAL_MAP.put( "commandLineNamespace", "" );
		GLOBAL_MAP.put( "completeHealthRestore", "false" );
		GLOBAL_MAP.put( "defaultBorderColor", "blue" );
		GLOBAL_MAP.put( "defaultDropdown1", "0" );
		GLOBAL_MAP.put( "defaultDropdown2", "1" );
		GLOBAL_MAP.put( "defaultDropdownSplit", "0" );
		GLOBAL_MAP.put( "defaultLimit", "5" );
		GLOBAL_MAP.put( "defaultLoginServer", "1" );
		GLOBAL_MAP.put( "eSoluScriptType", "0" );
		GLOBAL_MAP.put( "grabCloversHardcore", "false" );
		GLOBAL_MAP.put( "grabCloversSoftcore", "true" );
		GLOBAL_MAP.put( "greenScreenProtection", "false" );
		GLOBAL_MAP.put( "guiUsesOneWindow", "false" );
		GLOBAL_MAP.put( "hideServerDebugText", "false" );
		GLOBAL_MAP.put( "highlightList", "" );
		GLOBAL_MAP.put( "http.proxyHost", "" );
		GLOBAL_MAP.put( "http.proxyPort", "" );
		GLOBAL_MAP.put( "http.proxyUser", "" );
		GLOBAL_MAP.put( "http.proxyPassword", "" );
		GLOBAL_MAP.put( "proxySet", "false" );
		GLOBAL_MAP.put( "ignoreAutoAttack", "false" );
		GLOBAL_MAP.put( "initialDesktop", "AdventureFrame,CommandDisplayFrame,MallSearchFrame,SkillBuffFrame" );
		GLOBAL_MAP.put( "initialFrames", "EventsFrame" );
		GLOBAL_MAP.put( "itemManagerIndex", "0" );
		GLOBAL_MAP.put( "lastBuffRequestType", "0" );
		GLOBAL_MAP.put( "lastRelayUpdate", "" );
		GLOBAL_MAP.put( "lastUsername", "" );
		GLOBAL_MAP.put( "logChatMessages", "true" );
		GLOBAL_MAP.put( "loginServerName", "" );
		GLOBAL_MAP.put( "loginWindowLogo", "lantern.jpg" );
		GLOBAL_MAP.put( "loginRecoveryHardcore", "false" );
		GLOBAL_MAP.put( "loginRecoverySoftcore", "true" );
		GLOBAL_MAP.put( "loginScript", "" );
		GLOBAL_MAP.put( "logoutScript", "" );

		GLOBAL_MAP.put( "logAcquiredItems", "true" );
		GLOBAL_MAP.put( "logBattleAction", "true" );
		GLOBAL_MAP.put( "logFamiliarActions", "false" );
		GLOBAL_MAP.put( "logGainMessages", "true" );
		GLOBAL_MAP.put( "logMonsterHealth", "false" );
		GLOBAL_MAP.put( "logReverseOrder", "false" );
		GLOBAL_MAP.put( "logStatGains", "true" );
		GLOBAL_MAP.put( "logStatusEffects", "false" );

		GLOBAL_MAP.put( "mapLoadsMiniBrowser", "false" );
		GLOBAL_MAP.put( "mementoListActive", "false" );
		GLOBAL_MAP.put( "pathedSummonsHardcore", "true" );
		GLOBAL_MAP.put( "pathedSummonsSoftcore", "false" );
		GLOBAL_MAP.put( "previousNotifyList", "<>" );
		GLOBAL_MAP.put( "previousUpdateVersion", VERSION_NAME );
		GLOBAL_MAP.put( "protectAgainstOverdrink", "true" );

		GLOBAL_MAP.put( "readManualHardcore", "true" );
		GLOBAL_MAP.put( "readManualSoftcore", "true" );
		GLOBAL_MAP.put( "relayAddsGraphicalCLI", "false" );
		GLOBAL_MAP.put( "relayAddsKoLSimulator", "false" );
		GLOBAL_MAP.put( "relayAddsMonsterHealth", "false" );
		GLOBAL_MAP.put( "relayAddsQuickScripts", "false" );
		GLOBAL_MAP.put( "relayAddsRestoreLinks", "true" );
		GLOBAL_MAP.put( "relayAddsRoundNumber", "false" );
		GLOBAL_MAP.put( "relayAddsUpArrowLinks", "true" );
		GLOBAL_MAP.put( "relayAddsUseLinks", "true" );
		GLOBAL_MAP.put( "relayAllowsOverrides", "false" );
		GLOBAL_MAP.put( "relayAlwaysBuysGum", "true" );
		GLOBAL_MAP.put( "relayBrowserOnly", "false" );
		GLOBAL_MAP.put( "relayFormatsChatText", "true" );
		GLOBAL_MAP.put( "relayHidesJunkMallItems", "true" );
		GLOBAL_MAP.put( "relayMaintainsEffects", "false" );
		GLOBAL_MAP.put( "relayMaintainsHealth", "true" );
		GLOBAL_MAP.put( "relayMaintainsMana", "false" );
		GLOBAL_MAP.put( "relayBrowserOnly", "false" );
		GLOBAL_MAP.put( "basementBuysItems", "false" );
		GLOBAL_MAP.put( "relayTextualizesEffects", "false" );
		GLOBAL_MAP.put( "relayUsesCachedImages", "false" );
		GLOBAL_MAP.put( "relayUsesInlineLinks", "false" );
		GLOBAL_MAP.put( "relayUsesIntegratedChat", "false" );
		GLOBAL_MAP.put( "relayViewsCustomItems", "false" );

		GLOBAL_MAP.put( "saveState", "" );
		GLOBAL_MAP.put( "saveStateActive", "" );
		GLOBAL_MAP.put( "scriptButtonPosition", "0" );
		GLOBAL_MAP.put( "scriptList", "restore hp | restore mp" );
		GLOBAL_MAP.put( "showAllRequests", "false" );
		GLOBAL_MAP.put( "swingLookAndFeel", "" );
		GLOBAL_MAP.put( "switchEquipmentForBuffs", "true" );
		GLOBAL_MAP.put( "testSocketTimeout", "true" );
		GLOBAL_MAP.put( "toolbarPosition", "1" );

		GLOBAL_MAP.put( "useDecoratedTabs", String.valueOf( !System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		GLOBAL_MAP.put( "innerTabColor", "#8ca9ff" );
		GLOBAL_MAP.put( "outerTabColor", "#0f46b4" );
		GLOBAL_MAP.put( "innerChatColor", "#ffa98c" );
		GLOBAL_MAP.put( "outerChatColor", "#b4460f" );

		GLOBAL_MAP.put( "useChatMonitor", "false" );
		GLOBAL_MAP.put( "useChatToolbar", "true" );
		GLOBAL_MAP.put( "useContactsFrame", "true" );
		GLOBAL_MAP.put( "useFastOutfitSwitch", "true" );
		GLOBAL_MAP.put( "useLowBandwidthRadio", "false" );
		GLOBAL_MAP.put( "useSeparateChannels", "true" );
		GLOBAL_MAP.put( "useShinyTabbedChat", "true" );
		GLOBAL_MAP.put( "useSystemTrayIcon", "false" );
		GLOBAL_MAP.put( "useTabbedChatFrame", "true" );
		GLOBAL_MAP.put( "useToolbars", "true" );
		GLOBAL_MAP.put( "visitRumpusHardcore", "true" );
		GLOBAL_MAP.put( "visitRumpusSoftcore", "true" );

		// Individual player userSettings which are not set on
		// a global level.

		USER_MAP.put( "autoAbortThreshold", "-0.1" );
		USER_MAP.put( "autoRepairBoxServants", "true" );
		USER_MAP.put( "battleAction", "attack with weapon" );
		USER_MAP.put( "betweenBattleScript", "" );
		USER_MAP.put( "breakfastCompleted", "false" );
		USER_MAP.put( "buffBotCasting", "" );
		USER_MAP.put( "buffBotMessageDisposal", "0" );
		USER_MAP.put( "buffBotPhilanthropyType", "1" );
		USER_MAP.put( "candyHeartSummons", "0" );
		USER_MAP.put( "chatbotScript", "" );
		USER_MAP.put( "chatbotScriptExecuted", "false" );
		USER_MAP.put( "chosenTrip", "" );
		USER_MAP.put( "cocktailSummons", "0" );
		USER_MAP.put( "currentBountyItem", "0" );
		USER_MAP.put( "currentFullness", "0" );
		USER_MAP.put( "currentHippyStore", "none" );
		USER_MAP.put( "currentMojoFilters", "0" );
		USER_MAP.put( "currentMood", "default" );
		USER_MAP.put( "currentPvpVictories", "" );
		USER_MAP.put( "currentSpleenUse", "0" );
		USER_MAP.put( "currentWheelPosition", "muscle" );
		USER_MAP.put( "defaultAutoAttack", "0" );
		USER_MAP.put( "defaultFlowerLossMessage", "" );
		USER_MAP.put( "defaultFlowerWinMessage", "" );
		USER_MAP.put( "demonName1", "" );
		USER_MAP.put( "demonName2", "" );
		USER_MAP.put( "demonName3", "" );
		USER_MAP.put( "demonName4", "" );
		USER_MAP.put( "demonName5", "" );
		USER_MAP.put( "expressCardUsed", "false" );
		USER_MAP.put( "grimoireSummons", "0" );
		USER_MAP.put( "hpAutoRecovery", "-0.1" );
		USER_MAP.put( "hpAutoRecoveryTarget", "1.0" );
		USER_MAP.put( "hpAutoRecoveryItems", "cannelloni cocoon;scroll of drastic healing;tongue of the walrus;lasagna bandages;doc galaktik's ailment ointment" );
		USER_MAP.put( "invalidBuffMessage", "You sent an amount which does not correspond to a valid buff amount." );
		USER_MAP.put( "lastAdventure", "" );
		USER_MAP.put( "lastCouncilVisit", "0" );
		USER_MAP.put( "lastEmptiedStorage", "-1" );
		USER_MAP.put( "lastFilthClearance", "-1" );

		USER_MAP.put( "lastDustyBottleReset", "-1" );
		USER_MAP.put( "lastDustyBottle2271", "" );
		USER_MAP.put( "lastDustyBottle2272", "" );
		USER_MAP.put( "lastDustyBottle2273", "" );
		USER_MAP.put( "lastDustyBottle2274", "" );
		USER_MAP.put( "lastDustyBottle2275", "" );
		USER_MAP.put( "lastDustyBottle2276", "" );

		USER_MAP.put( "lastBangPotionReset", "-1" );
		USER_MAP.put( "lastBangPotion819", "" );
		USER_MAP.put( "lastBangPotion820", "" );
		USER_MAP.put( "lastBangPotion821", "" );
		USER_MAP.put( "lastBangPotion822", "" );
		USER_MAP.put( "lastBangPotion823", "" );
		USER_MAP.put( "lastBangPotion824", "" );
		USER_MAP.put( "lastBangPotion825", "" );
		USER_MAP.put( "lastBangPotion826", "" );
		USER_MAP.put( "lastBangPotion827", "" );

		USER_MAP.put( "lastStoneSphereReset", "-1" );
		USER_MAP.put( "lastStoneSphere2174", "" );
		USER_MAP.put( "lastStoneSphere2175", "" );
		USER_MAP.put( "lastStoneSphere2176", "" );
		USER_MAP.put( "lastStoneSphere2177", "" );

		USER_MAP.put( "lastBreakfast", "-1" );
		USER_MAP.put( "lastCounterDay", "-1" );
		USER_MAP.put( "lastGalleryUnlock", "-1" );
		USER_MAP.put( "lastMessageId", "" );
		USER_MAP.put( "lastQuartetAscension", "-1" );
		USER_MAP.put( "lastQuartetRequest", "0" );
		USER_MAP.put( "lastSecondFloorUnlock", "-1" );
		USER_MAP.put( "lastTowerClimb", "-1" );
		USER_MAP.put( "luckySewerAdventure", "stolen accordion" );
		USER_MAP.put( "manaBurningThreshold", "-0.1" );

		USER_MAP.put( "munchiesPillsUsed", "0" );
		USER_MAP.put( "mpAutoRecovery", "0.0" );
		USER_MAP.put( "mpAutoRecoveryTarget", "0.3" );
		USER_MAP.put( "mpAutoRecoveryItems", "phonics down;knob goblin superseltzer;mountain stream soda;magical mystery juice;knob goblin seltzer;cherry cloaca cola;soda water" );
		USER_MAP.put( "noodleSummons", "0" );
		USER_MAP.put( "plantingDay", "-1" );
		USER_MAP.put( "plantingDate", "" );
		USER_MAP.put( "plantingLength", "" );
		USER_MAP.put( "plantingScript", "" );
		USER_MAP.put( "preBlackbirdFamiliar", "" );
		USER_MAP.put( "reagentSummons", "0" );
		USER_MAP.put( "relayAddsCustomCombat", "true" );
		USER_MAP.put( "relayCounters", "" );
		USER_MAP.put( "requireBoxServants", "true" );
		USER_MAP.put( "retrieveContacts", "true" );
		USER_MAP.put( "showGainsPerUnit", "false" );
		USER_MAP.put( "snowconeSummons", "0" );
		USER_MAP.put( "thanksMessage", "Thank you for the donation!" );
		USER_MAP.put( "trapperOre", "chrome ore" );
		USER_MAP.put( "violetFogGoal", "0" );
		USER_MAP.put( "visibleBrowserInventory", "" );

		// These are userSettings related to the tavern faucet
		// used to make the interface friendlier.

		USER_MAP.put( "lastTavernSquare", "0" );
		USER_MAP.put( "lastTavernAscension", "0" );
		USER_MAP.put( "tavernLayout", "0000000000000000000000000" );

		// Yay for the Louvre.

		USER_MAP.put( "lastLouvreMap", "0" );
		USER_MAP.put( "louvreLayout", "" );
		USER_MAP.put( "louvreDesiredGoal", "7" );
		USER_MAP.put( "louvreGoal", "0" );

		// These are userSettings related to choice adventures.
		// Ensure that they exist, and if they do not, load
		// them to their default userSettings.

		USER_MAP.put( "lastChoiceUpdate", "" );
		USER_MAP.put( "choiceAdventure2", "2" );
		USER_MAP.put( "choiceAdventure3", "3" );
		USER_MAP.put( "choiceAdventure4", "3" );
		USER_MAP.put( "choiceAdventure5", "2" );
		USER_MAP.put( "choiceAdventure6", "1" );
		USER_MAP.put( "choiceAdventure7", "1" );
		USER_MAP.put( "choiceAdventure8", "3" );
		USER_MAP.put( "choiceAdventure9", "2" );
		USER_MAP.put( "choiceAdventure10", "1" );
		USER_MAP.put( "choiceAdventure11", "3" );
		USER_MAP.put( "choiceAdventure12", "2" );
		USER_MAP.put( "choiceAdventure14", "4" );
		USER_MAP.put( "choiceAdventure15", "4" );
		USER_MAP.put( "choiceAdventure16", "4" );
		USER_MAP.put( "choiceAdventure17", "4" );
		USER_MAP.put( "choiceAdventure18", "4" );
		USER_MAP.put( "choiceAdventure19", "4" );
		USER_MAP.put( "choiceAdventure20", "4" );
		USER_MAP.put( "choiceAdventure21", "2" );
		USER_MAP.put( "choiceAdventure22", "4" );
		USER_MAP.put( "choiceAdventure23", "4" );
		USER_MAP.put( "choiceAdventure24", "4" );
		USER_MAP.put( "choiceAdventure25", "2" );
		USER_MAP.put( "choiceAdventure26", "2" );
		USER_MAP.put( "choiceAdventure27", "2" );
		USER_MAP.put( "choiceAdventure28", "2" );
		USER_MAP.put( "choiceAdventure29", "2" );
		USER_MAP.put( "choiceAdventure40", "3" );
		USER_MAP.put( "choiceAdventure41", "3" );
		USER_MAP.put( "choiceAdventure42", "3" );
		USER_MAP.put( "choiceAdventure45", "1" );
		USER_MAP.put( "choiceAdventure46", "3" );
		USER_MAP.put( "choiceAdventure47", "2" );
		USER_MAP.put( "choiceAdventure71", "1" );
		USER_MAP.put( "choiceAdventure72", "2" );
		USER_MAP.put( "choiceAdventure73", "2" );
		USER_MAP.put( "choiceAdventure74", "2" );
		USER_MAP.put( "choiceAdventure75", "3" );
		USER_MAP.put( "choiceAdventure76", "3" );
		USER_MAP.put( "choiceAdventure77", "2" );
		USER_MAP.put( "choiceAdventure78", "1" );
		USER_MAP.put( "choiceAdventure79", "1" );
		USER_MAP.put( "choiceAdventure80", "99" );
		USER_MAP.put( "choiceAdventure81", "1" );
		USER_MAP.put( "choiceAdventure82", "1" );
		USER_MAP.put( "choiceAdventure83", "1" );
		USER_MAP.put( "choiceAdventure84", "2" );
		USER_MAP.put( "choiceAdventure85", "1" );
		USER_MAP.put( "choiceAdventure86", "2" );
		USER_MAP.put( "choiceAdventure87", "2" );
		USER_MAP.put( "choiceAdventure88", "1" );
		USER_MAP.put( "choiceAdventure89", "3" );
		USER_MAP.put( "choiceAdventure90", "2" );
		USER_MAP.put( "choiceAdventure91", "1" );
		USER_MAP.put( "choiceAdventure105", "3" );
		USER_MAP.put( "choiceAdventure106", "4" );
		USER_MAP.put( "choiceAdventure107", "4" );
		USER_MAP.put( "choiceAdventure108", "4" );
		USER_MAP.put( "choiceAdventure109", "1" );
		USER_MAP.put( "choiceAdventure110", "4" );
		USER_MAP.put( "choiceAdventure111", "3" );
		USER_MAP.put( "choiceAdventure112", "2" );
		USER_MAP.put( "choiceAdventure113", "2" );
		USER_MAP.put( "choiceAdventure114", "2" );
		USER_MAP.put( "choiceAdventure115", "1" );
		USER_MAP.put( "choiceAdventure116", "4" );
		USER_MAP.put( "choiceAdventure117", "1" );
		USER_MAP.put( "choiceAdventure118", "2" );
		USER_MAP.put( "choiceAdventure120", "4" );
		USER_MAP.put( "choiceAdventure123", "2" );
		USER_MAP.put( "choiceAdventure125", "3" );
		USER_MAP.put( "choiceAdventure126", "1" );
		USER_MAP.put( "choiceAdventure127", "3" );
		USER_MAP.put( "choiceAdventure129", "1" );
		USER_MAP.put( "choiceAdventure130", "1" );
		USER_MAP.put( "choiceAdventure131", "1" );
		USER_MAP.put( "choiceAdventure132", "2" );
		USER_MAP.put( "choiceAdventure134", "2" );
		USER_MAP.put( "choiceAdventure135", "2" );
		USER_MAP.put( "choiceAdventure136", "4" );
		USER_MAP.put( "choiceAdventure137", "4" );
		USER_MAP.put( "choiceAdventure138", "4" );
		USER_MAP.put( "choiceAdventure139", "1" );
		USER_MAP.put( "choiceAdventure140", "2" );
		USER_MAP.put( "choiceAdventure141", "1" );
		USER_MAP.put( "choiceAdventure142", "3" );
		USER_MAP.put( "choiceAdventure143", "1" );
		USER_MAP.put( "choiceAdventure144", "1" );
		USER_MAP.put( "choiceAdventure145", "1" );
		USER_MAP.put( "choiceAdventure146", "3" );
		USER_MAP.put( "choiceAdventure147", "3" );
		USER_MAP.put( "choiceAdventure148", "1" );
		USER_MAP.put( "choiceAdventure149", "2" );
		USER_MAP.put( "choiceAdventure151", "2" );
		USER_MAP.put( "choiceAdventure152", "1" );
		USER_MAP.put( "choiceAdventure153", "4" );
		USER_MAP.put( "choiceAdventure154", "1" );
		USER_MAP.put( "choiceAdventure155", "4" );
		USER_MAP.put( "choiceAdventure156", "1" );
		USER_MAP.put( "choiceAdventure157", "4" );
		USER_MAP.put( "choiceAdventure158", "1" );
		USER_MAP.put( "choiceAdventure159", "4" );
		USER_MAP.put( "choiceAdventure160", "1" );
		USER_MAP.put( "choiceAdventure161", "4" );
		USER_MAP.put( "choiceAdventure162", "1" );
		USER_MAP.put( "choiceAdventure163", "1" );
		USER_MAP.put( "choiceAdventure164", "2" );
		USER_MAP.put( "choiceAdventure165", "2" );
		USER_MAP.put( "choiceAdventure166", "3" );
		USER_MAP.put( "choiceAdventure167", "3" );
		USER_MAP.put( "choiceAdventure168", "3" );
		USER_MAP.put( "choiceAdventure169", "3" );
		USER_MAP.put( "choiceAdventure170", "1" );
		USER_MAP.put( "choiceAdventure171", "3" );
		USER_MAP.put( "choiceAdventure172", "1" );
		USER_MAP.put( "choiceAdventure177", "4" );
		USER_MAP.put( "choiceAdventure178", "1" );
		USER_MAP.put( "choiceAdventure180", "1" );
		USER_MAP.put( "choiceAdventure181", "1" );
	}

	private void ensureChoiceDefaults()
	{
		if ( globalSettings == null || this == globalSettings )
			return;

		if ( super.getProperty( "lastChoiceUpdate" ).equals( VERSION_NAME ) )
			return;

		String setting;
		super.setProperty( "lastChoiceUpdate", VERSION_NAME );

		for ( int i = 0; i < AdventureDatabase.CHOICE_ADV_SPOILERS.length; ++i )
		{
			setting = AdventureDatabase.CHOICE_ADV_SPOILERS[i].getSetting();
			if ( !forceChoiceDefault( StaticEntity.parseInt( setting.substring(15) ) ) )
				continue;

			this.valuesChanged = true;
			super.setProperty( setting, (String) USER_MAP.get( setting ) );
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

			int defaultOption = StaticEntity.parseInt( (String) USER_MAP.get( setting ) ) - 1;

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
		boolean isGlobal = this.noExtensionName.equals( "GLOBAL" );
		TreeMap currentMap = isGlobal ? GLOBAL_MAP : USER_MAP;

		Object [] keys = currentMap.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			if ( !this.containsKey( keys[i] ) )
			{
				this.valuesChanged = true;
				super.setProperty( (String) keys[i], (String) currentMap.get( keys[i] ) );
			}
		}

		if ( isGlobal )
			this.ensureChoiceDefaults();

		this.saveToFile();
	}
}
