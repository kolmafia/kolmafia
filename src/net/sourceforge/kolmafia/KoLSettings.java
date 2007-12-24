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
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.JCheckBox;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureDatabase.ChoiceAdventure;

public class KoLSettings
	extends Properties
	implements KoLConstants
{
	private boolean valuesChanged = false;
	private static final TreeMap checkboxMap = new TreeMap();
	private static final TreeMap propertyNames = new TreeMap();

	public static final String[] COMMON_JUNK =
	{
		// Items which usually get autosold by people, regardless of
		// the situation.  This includes the various meat combinables,
		// sewer items, and stat boosters.

		"meat stack",
		"dense meat stack",
		"twinkly powder",
		"seal-clubbing club",
		"seal tooth",
		"helmet turtle",
		"pasta spoon",
		"ravioli hat",
		"disco mask",
		"mariachi pants",
		"moxie weed",
		"strongness elixir",
		"magicalness-in-a-can",
		"enchanted barbell",
		"concentrated magicalness pill",
		"giant moxie weed",
		"extra-strength strongness elixir",
		"jug-o-magicalness",
		"suntan lotion of moxiousness",

		// Next, some common drops in low level areas that are farmed
		// for other reasons other than those items.

		"Mad Train wine",
		"ice-cold fotie",
		"ice-cold Willer",
		"ice-cold Sir Schlitz",
		"bowl of cottage cheese",
		"Knob Goblin firecracker",
		"Knob Goblin pants",
		"Knob Goblin scimitar",
		"viking helmet",
		"bar skin",
		"spooky shrunken head",
		"dried face",
		"barskin hat",
		"spooky stick",
		"batgut",
		"bat guano",
		"ratgut",
		"briefcase",
		"taco shell",
		"uncooked chorizo",
		"Gnollish plunger",
		"gnoll teeth",
		"gnoll lips",
		"Gnollish toolbox",

		// Next, some common drops in medium level areas that are also
		// farmed for other reasons beyond these items.

		"hill of beans",
		"Knob Goblin love potion",
		"Knob Goblin steroids",
		"Imp Ale",
		"hot wing",
		"evil golden arch",
		"leather mask",
		"necklace chain",
		"hemp string",
		"piercing post",
		"phat turquoise bead",
		"carob chunks",
		"Feng Shui for Big Dumb Idiots",
		"crowbarrr",
		"sunken chest",
		"barrrnacle",
		"safarrri hat",
		"arrrgyle socks",
		"charrrm",
		"leotarrrd",
		"pirate pelvis",
		"grave robbing shovel",
		"ghuol ears",
		"ghuol egg",
		"ghuol guolash",
		"lihc eye",
		"mind flayer corpse",
		"royal jelly",
		"goat beard",
		"sabre teeth",
		"t8r tots",
		"pail",
		"Trollhouse cookies",
		"Spam Witch sammich",
		"white satin pants",
		"white chocolate chips",
		"catgut",
		"white snake skin",
		"mullet wig",

		// High level area item drops which tend to be autosold or
		// auto-used.

		"cocoa eggshell fragment",
		"glowing red eye",
		"amulet of extreme plot significance",
		"Penultimate Fantasy chest",
		"Warm Subject gift certificate",
		"disturbing fanfic",
		"probability potion",
		"procrastination potion",
		"Mick's IcyVapoHotness Rub"
	};

	// For now, only outfit pieces should be considered as
	// part of the default singleton set.

	public static final String[] SINGLETON_ITEMS =
	{
		"bugbear beanie",
		"bugbear bungguard",
		"filthy knitted dread sack",
		"filthy corduroys",
		"homoerotic frat-paddle",
		"Orcish baseball cap",
		"Orcish cargo shorts",
		"Knob Goblin harem veil",
		"Knob Goblin harem pants",
		"Knob Goblin elite helm",
		"Knob Goblin elite polearm",
		"Knob Goblin elite pants",
		"eyepatch",
		"swashbuckling pants",
		"stuffed shoulder parrot",
		"Cloaca-Cola fatigues",
		"Cloaca-Cola helmet",
		"Cloaca-Cola shield",
		"Dyspepsi-Cola fatigues",
		"Dyspepsi-Cola helmet",
		"Dyspepsi-Cola shield",
		"bullet-proof corduroys",
		"round purple sunglasses",
		"reinforced beaded headband",
		"beer helmet",
		"distressed denim pants",
		"bejeweled pledge pin"
	};

	public static final String[] COMMON_MEMENTOS =
	{
		// Crimbo 2005/2006 accessories, if they're still around,
		// probably shouldn't be placed in the player's store.

		"tiny plastic Crimbo wreath",
		"tiny plastic Uncle Crimbo",
		"tiny plastic Crimbo elf",
		"tiny plastic sweet nutcracker",
		"tiny plastic Crimbo reindeer",
		"wreath-shaped Crimbo cookie",
		"bell-shaped Crimbo cookie",
		"tree-shaped Crimbo cookie",

		"candy stake",
		"spooky eggnog",
		"ancient unspeakable fruitcake",
		"gingerbread horror",
		"bat-shaped Crimboween cookie",
		"skull-shaped Crimboween cookie",
		"tombstone-shaped Crimboween cookie",

		"tiny plastic gift-wrapping vampire",
		"tiny plastic ancient yuletide troll",
		"tiny plastic skeletal reindeer",
		"tiny plastic Crimboween pentagram",
		"tiny plastic Scream Queen",
		"orange and black Crimboween candy",

		// Certain items tend to be used throughout an ascension, so
		// they probably shouldn't get sold, either.

		"sword behind inappropriate prepositions",
		"toy mercenary",

		// Collectible items should probably be sent to other players
		// rather than be autosold for no good reason.

		"stuffed cocoabo",
		"stuffed baby gravy fairy",
		"stuffed flaming gravy fairy",
		"stuffed frozen gravy fairy",
		"stuffed stinky gravy fairy",
		"stuffed spooky gravy fairy",
		"stuffed sleazy gravy fairy",
		"stuffed astral badger",
		"stuffed MagiMechTech MicroMechaMech",
		"stuffed hand turkey",
		"stuffed snowy owl",
		"stuffed scary death orb",
		"stuffed mind flayer",
		"stuffed undead elbow macaroni",
		"stuffed angry cow",
		"stuffed Cheshire bitten",
		"stuffed yo-yo",
		"rubber WWJD? bracelet",
		"rubber WWBD? bracelet",
		"rubber WWSPD? bracelet",
		"rubber WWtNSD? bracelet",
		"heart necklace",
		"spade necklace",
		"diamond necklace",
		"club necklace",
	};

	private static final TreeMap GLOBAL_MAP = new TreeMap();
	private static final TreeMap USER_MAP = new TreeMap();

	static
	{
		// Move all files to ~/Library/Application Support/KoLmafia
		// if the user is on a Macintosh, just for consistency.

		File source;

		if ( UtilityConstants.USE_OSX_STYLE_DIRECTORIES || UtilityConstants.USE_LINUX_STYLE_DIRECTORIES )
		{
			UtilityConstants.ROOT_LOCATION.mkdirs();

			source = new File( UtilityConstants.BASE_LOCATION, UtilityConstants.DATA_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( UtilityConstants.DATA_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, UtilityConstants.IMAGE_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( UtilityConstants.IMAGE_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, UtilityConstants.SETTINGS_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( UtilityConstants.SETTINGS_LOCATION );
			}

			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.ATTACKS_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.ATTACKS_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.BUFFBOT_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.BUFFBOT_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.CCS_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.CCS_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.CHATLOG_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.CHATLOG_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.PLOTS_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.PLOTS_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.SCRIPT_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.SCRIPT_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.SESSIONS_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.SESSIONS_LOCATION );
			}
			source = new File( UtilityConstants.BASE_LOCATION, KoLConstants.RELAY_DIRECTORY );
			if ( source.exists() )
			{
				source.renameTo( KoLConstants.RELAY_LOCATION );
			}
		}

		if ( !UtilityConstants.DATA_LOCATION.exists() )
		{
			UtilityConstants.DATA_LOCATION.mkdirs();
		}

		if ( !KoLConstants.CCS_LOCATION.exists() )
		{
			KoLConstants.CCS_LOCATION.mkdirs();
		}

		// Move CCS files from data directory to ccs directory
		File[] listing = UtilityConstants.DATA_LOCATION.listFiles();
		for ( int i = 0; i < listing.length; ++i )
		{
			source = listing[ i ];
			String name = source.getName();
			if ( name.endsWith( ".ccs" ) )
			{
				source.renameTo( new File( KoLConstants.CCS_LOCATION, name ) );
			}
		}

		if ( !UtilityConstants.SETTINGS_LOCATION.exists() )
		{
			UtilityConstants.SETTINGS_LOCATION.mkdirs();
		}

		listing = UtilityConstants.SETTINGS_LOCATION.listFiles();
		for ( int i = 0; i < listing.length; ++i )
		{
			String path = listing[ i ].getPath();
			if ( path.startsWith( "combat_" ) || path.endsWith( "_combat.txt" ) )
			{
				listing[ i ].delete();
			}
			else if ( path.startsWith( "moods_" ) )
			{
				path = path.substring( 6, path.indexOf( ".txt" ) );
				listing[ i ].renameTo( new File( UtilityConstants.SETTINGS_LOCATION, path + "_moods.txt" ) );
			}
			else if ( path.startsWith( "prefs_" ) )
			{
				path = path.substring( 6, path.indexOf( ".txt" ) );
				listing[ i ].renameTo( new File( UtilityConstants.SETTINGS_LOCATION, path + "_prefs.txt" ) );
			}
		}

		File deprecated = new File( UtilityConstants.DATA_LOCATION, "autosell.txt" );
		if ( deprecated.exists() )
		{
			deprecated.delete();
		}
		deprecated = new File( UtilityConstants.DATA_LOCATION, "singleton.txt" );
		if ( deprecated.exists() )
		{
			deprecated.delete();
		}
		deprecated = new File( UtilityConstants.DATA_LOCATION, "mementos.txt" );
		if ( deprecated.exists() )
		{
			deprecated.delete();
		}
		deprecated = new File( UtilityConstants.DATA_LOCATION, "mallsell.txt" );
		if ( deprecated.exists() )
		{
			deprecated.delete();
		}

		KoLSettings.initializeMaps();
	}

	private static KoLSettings globalSettings = new KoLSettings( "" );
	private static KoLSettings userSettings = KoLSettings.globalSettings;

	private final File userSettingsFile;
	private static final File itemFlagsFile = new File( UtilityConstants.DATA_LOCATION, "itemflags.txt" );

	private static final void initializeList( final LockableListModel model, final String[] defaults )
	{
		model.clear();
		AdventureResult item;

		for ( int i = 0; i < defaults.length; ++i )
		{
			item = new AdventureResult( defaults[ i ], 1, false );
			if ( !model.contains( item ) )
			{
				model.add( item );
			}

			if ( model == KoLConstants.singletonList && !KoLConstants.junkList.contains( item ) )
			{
				KoLConstants.junkList.add( item );
			}
		}
	}

	public static final void initializeLists()
	{
		if ( !KoLSettings.itemFlagsFile.exists() )
		{
			KoLSettings.initializeList( KoLConstants.junkList, KoLSettings.COMMON_JUNK );
			KoLSettings.initializeList( KoLConstants.singletonList, KoLSettings.SINGLETON_ITEMS );
			KoLSettings.initializeList( KoLConstants.mementoList, KoLSettings.COMMON_MEMENTOS );

			KoLConstants.profitableList.clear();
			return;
		}

		try
		{
			AdventureResult item;
			FileInputStream istream = new FileInputStream( KoLSettings.itemFlagsFile );
			BufferedReader reader = new BufferedReader( new InputStreamReader( istream ) );

			String line;
			LockableListModel model = null;

			while ( ( line = reader.readLine() ) != null )
			{
				if ( line.equals( "" ) )
				{
					continue;
				}

				if ( line.startsWith( " > " ) )
				{
					if ( line.endsWith( "junk" ) )
					{
						model = KoLConstants.junkList;
					}
					else if ( line.endsWith( "singleton" ) )
					{
						model = KoLConstants.singletonList;
					}
					else if ( line.endsWith( "mementos" ) )
					{
						model = KoLConstants.mementoList;
					}
					else if ( line.endsWith( "profitable" ) )
					{
						model = KoLConstants.profitableList;
					}

					if ( model != null )
					{
						model.clear();
					}
				}
				else if ( model != null && TradeableItemDatabase.contains( line ) )
				{
					item = new AdventureResult( line, 1, false );

					if ( !model.contains( item ) )
					{
						model.add( item );
					}

					if ( model == KoLConstants.singletonList && !KoLConstants.junkList.contains( item ) )
					{
						KoLConstants.junkList.add( item );
					}
				}
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

	public static final void reset( final String username )
	{
		if ( username.equals( "" ) )
		{
			KoLSettings.userSettings = KoLSettings.globalSettings;
			return;
		}

		KoLSettings.userSettings = new KoLSettings( username );

		CombatSettings.loadSettings();
		MoodSettings.restoreDefaults();
	}

	private boolean isGlobal;

	/**
	 * Constructs a userSettings file for a character with the specified name. Note that in the data file created, all
	 * spaces in the character name will be replaced with an underscore, and all other punctuation will be removed.
	 *
	 * @param characterName The name of the character this userSettings file represents
	 */

	private KoLSettings( final String characterName )
	{
		this.isGlobal = characterName.equals( "" );
		String noExtensionName = KoLSettings.baseUserName( characterName );
		this.userSettingsFile = new File( UtilityConstants.SETTINGS_LOCATION, noExtensionName + "_prefs.txt" );

		this.loadFromFile();
		this.ensureDefaults();
	}

	public static final String getCaseSensitiveName( final String name )
	{
		String lowercase = name.toLowerCase();
		String actualName = (String) KoLSettings.propertyNames.get( lowercase );
		if ( actualName != null )
		{
			return actualName;
		}

		KoLSettings.propertyNames.put( lowercase, name );
		return name;
	}

	public static final String baseUserName( final String name )
	{
		return name == null || name.equals( "" ) ? "GLOBAL" : StaticEntity.globalStringReplace( name, " ", "_" ).trim().toLowerCase();
	}

	public static final boolean isUserEditable( final String property )
	{
		return !property.startsWith( "saveState" );
	}

	private static final String getPropertyName( final String player, final String name )
	{
		return player == null || player.equals( "" ) ? name : name + "." + KoLSettings.baseUserName( player );
	}

	public static final void setUserProperty( final String name, final String value )
	{
		KoLSettings.userSettings.setProperty( name, value );
	}

	public static final void resetUserProperty( final String name, final String value )
	{
		String old = KoLSettings.userSettings.getProperty( name );
		if ( !old.equals( value ) )
		{
			KoLSettings.userSettings.setProperty( name, value );
		}
	}

	public static final String getUserProperty( final String name )
	{
		return KoLSettings.userSettings.getProperty( name );
	}

	public static final void setGlobalProperty( final String name, final String value )
	{
		KoLSettings.setGlobalProperty( KoLCharacter.getUserName(), name, value );
	}

	public static final String getGlobalProperty( final String name )
	{
		return KoLSettings.getGlobalProperty( KoLCharacter.getUserName(), name );
	}

	public static final void setGlobalProperty( final String player, final String name, final String value )
	{
		KoLSettings.userSettings.setProperty( KoLSettings.getPropertyName( player, name ), value );
	}

	public static final String getGlobalProperty( final String player, final String name )
	{
		return KoLSettings.userSettings.getProperty( KoLSettings.getPropertyName( player, name ) );
	}

	public static final boolean getBooleanProperty( final String name )
	{
		return KoLSettings.getUserProperty( name ).equals( "true" );
	}

	public static final int getIntegerProperty( final String name )
	{
		return StaticEntity.parseInt( KoLSettings.getUserProperty( name ) );
	}

	public static final float getFloatProperty( final String name )
	{
		return StaticEntity.parseFloat( KoLSettings.getUserProperty( name ) );
	}

	public static final int incrementIntegerProperty( final String name, final int increment, final int max,
		final boolean mod )
	{
		int current = StaticEntity.parseInt( KoLSettings.getUserProperty( name ) );
		current += increment;
		if ( max > 0 && current > max )
		{
			current = max;
		}
		if ( mod && current >= max )
		{
			current %= max;
		}
		KoLSettings.setUserProperty( name, String.valueOf( current ) );
		return current;
	}

	public static final boolean isGlobalProperty( final String name )
	{
		return KoLSettings.GLOBAL_MAP.containsKey( name ) || name.startsWith( "saveState" ) || name.startsWith( "displayName" ) || name.startsWith( "getBreakfast" );
	}

	public String getProperty( String name )
	{
		name = KoLSettings.getCaseSensitiveName( name );
		boolean isGlobalProperty = KoLSettings.isGlobalProperty( name );

		if ( isGlobalProperty && !this.isGlobal )
		{
			String value = KoLSettings.globalSettings.getProperty( name );
			return value == null ? "" : value;
		}
		else if ( !isGlobalProperty && this.isGlobal )
		{
			return "";
		}

		String value = super.getProperty( name );
		return value == null ? "" : RequestEditorKit.getUnicode( value );
	}

	public Object setProperty( String name, String value )
	{
		if ( value == null )
		{
			return "";
		}

		name = KoLSettings.getCaseSensitiveName( name );
		boolean isGlobalProperty = KoLSettings.isGlobalProperty( name );

		if ( isGlobalProperty && !this.isGlobal )
		{
			return KoLSettings.globalSettings.setProperty( name, value );
		}
		else if ( !isGlobalProperty && this.isGlobal )
		{
			return "";
		}

		// All tests passed.  Now, go ahead and execute the
		// set property and return the old value.

		String oldValue = this.getProperty( name );
		value = RequestEditorKit.getEntities( value );

		if ( oldValue != null && oldValue.equals( value ) )
		{
			return oldValue;
		}

		this.valuesChanged = true;
		super.setProperty( name, value );

		if ( KoLSettings.checkboxMap.containsKey( name ) )
		{
			ArrayList list = (ArrayList) KoLSettings.checkboxMap.get( name );
			for ( int i = 0; i < list.size(); ++i )
			{
				WeakReference reference = (WeakReference) list.get( i );
				JCheckBox item = (JCheckBox) reference.get();
				if ( item != null )
				{
					item.setSelected( value.equals( "true" ) );
				}
			}
		}

		this.saveToFile();
		return oldValue == null ? "" : oldValue;
	}

	public static final void registerCheckbox( final String name, final JCheckBox checkbox )
	{
		ArrayList list = null;

		if ( KoLSettings.checkboxMap.containsKey( name ) )
		{
			list = (ArrayList) KoLSettings.checkboxMap.get( name );
		}
		else
		{
			list = new ArrayList();
			KoLSettings.checkboxMap.put( name, list );
		}

		list.add( new WeakReference( checkbox ) );
	}

	public static final void saveFlaggedItemList()
	{
		AdventureResult item;

		LogStream ostream = LogStream.openStream( KoLSettings.itemFlagsFile, true );

		ostream.println( " > junk" );
		ostream.println();

		for ( int i = 0; i < KoLConstants.junkList.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.junkList.get( i );
			if ( !KoLConstants.singletonList.contains( item ) )
			{
				ostream.println( item.getName() );
			}
		}

		ostream.println();
		ostream.println( " > singleton" );
		ostream.println();

		for ( int i = 0; i < KoLConstants.singletonList.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.singletonList.get( i );
			ostream.println( item.getName() );
		}

		ostream.println();
		ostream.println( " > mementos" );
		ostream.println();

		for ( int i = 0; i < KoLConstants.mementoList.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.mementoList.get( i );
			ostream.println( item.getName() );
		}

		ostream.println();
		ostream.println( " > profitable" );
		ostream.println();

		for ( int i = 0; i < KoLConstants.profitableList.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.profitableList.get( i );
			ostream.println( item.getCount() + " " + item.getName() );
		}

		ostream.close();
	}

	public void saveToFile()
	{
		if ( !this.valuesChanged )
		{
			return;
		}

		UtilityConstants.SETTINGS_LOCATION.mkdirs();

		try
		{
			// Determine the contents of the file by
			// actually printing them.

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			this.store( ostream, KoLConstants.VERSION_NAME );

			String[] lines = ostream.toString().split( KoLConstants.LINE_BREAK );
			Arrays.sort( lines );

			ostream.reset();

			for ( int i = 0; i < lines.length; ++i )
			{
				if ( lines[ i ].startsWith( "#" ) )
				{
					continue;
				}

				ostream.write( lines[ i ].getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}

			if ( this.userSettingsFile.exists() )
			{
				this.userSettingsFile.delete();
			}

			this.userSettingsFile.createNewFile();
			ostream.writeTo( new FileOutputStream( this.userSettingsFile ) );
		}
		catch ( IOException e )
		{
			// This should not happen.
		}
	}

	/**
	 * Loads the userSettings located in the given file into this object. Note that all userSettings are overridden; if
	 * the given file does not exist, the current global userSettings will also be rewritten into the appropriate file.
	 *
	 * @param source The file that contains (or will contain) the character data
	 */

	private void loadFromFile()
	{
		try
		{
			// First guarantee that a userSettings file exists with
			// the appropriate Properties data.

			if ( !this.userSettingsFile.exists() )
			{
				return;
			}

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

		String currentKey;
		Iterator keyIterator = this.keySet().iterator();
		while ( keyIterator.hasNext() )
		{
			currentKey = (String) keyIterator.next();
			KoLSettings.propertyNames.put( currentKey.toLowerCase(), currentKey );
		}
	}

	private static final void initializeMaps()
	{
		String[] current;
		TreeMap desiredMap;
		BufferedReader istream = KoLDatabase.getVersionedReader( "defaults.txt", KoLConstants.DEFAULTS_VERSION );

		while ( ( current = KoLDatabase.readData( istream ) ) != null )
		{
			desiredMap = current[ 0 ].equals( "global" ) ? KoLSettings.GLOBAL_MAP : KoLSettings.USER_MAP;
			desiredMap.put( current[ 1 ], current.length == 2 ? "" : current[ 2 ] );
		}

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
		LogStream ostream = LogStream.openStream( "choices.txt", true );

		ostream.println( "[u]Configurable[/u]" );
		ostream.println();

		AdventureDatabase.setChoiceOrdering( false );
		Arrays.sort( AdventureDatabase.CHOICE_ADVS );
		Arrays.sort( AdventureDatabase.CHOICE_ADV_SPOILERS );

		KoLSettings.printDefaults( AdventureDatabase.CHOICE_ADVS, ostream );

		ostream.println();
		ostream.println();
		ostream.println( "[u]Not Configurable[/u]" );
		ostream.println();

		KoLSettings.printDefaults( AdventureDatabase.CHOICE_ADV_SPOILERS, ostream );

		AdventureDatabase.setChoiceOrdering( true );
		Arrays.sort( AdventureDatabase.CHOICE_ADVS );
		Arrays.sort( AdventureDatabase.CHOICE_ADV_SPOILERS );

		ostream.close();
	}

	private static final void printDefaults( final ChoiceAdventure[] choices, final LogStream ostream )
	{
		for ( int i = 0; i < choices.length; ++i )
		{
			String setting = choices[ i ].getSetting();
			int defaultOption = StaticEntity.parseInt( (String) KoLSettings.USER_MAP.get( setting ) ) - 1;

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

	private void ensureDefaults()
	{
		TreeMap currentMap = this.isGlobal ? KoLSettings.GLOBAL_MAP : KoLSettings.USER_MAP;

		Object[] keys = currentMap.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			if ( !this.containsKey( keys[ i ] ) )
			{
				this.valuesChanged = true;
				super.setProperty( (String) keys[ i ], (String) currentMap.get( keys[ i ] ) );
			}
		}

		this.saveToFile();
	}
}
