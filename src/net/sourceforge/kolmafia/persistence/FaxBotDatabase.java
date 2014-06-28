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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class FaxBotDatabase
	extends KoLDatabase
{
	private static boolean isInitialized = false;
	private static boolean faxBotConfigured = false;
	private static boolean faxBotError = false;

	// List of bots from faxbots.txt
	public static final ArrayList<BotData> botData = new ArrayList<BotData>();

	// List of faxbots named in config files.
	public static final ArrayList<FaxBot> faxbots = new ArrayList<FaxBot>();

	public static final void reconfigure()
	{
		FaxBotDatabase.isInitialized = false;
		FaxBotDatabase.configure();
	}

	public static final void configure()
	{
		if ( FaxBotDatabase.isInitialized )
		{
			return;
		}

		FaxBotDatabase.readFaxbotConfig();
		FaxBotDatabase.configureFaxBots();
	}

	private static final void readFaxbotConfig()
	{
		FaxBotDatabase.botData.clear();

		BufferedReader reader =
			FileUtilities.getVersionedReader( "faxbots.txt", KoLConstants.FAXBOTS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length > 1 )
			{
				FaxBotDatabase.botData.add( new BotData( data[ 0 ].trim().toLowerCase(), data[ 1 ].trim() ) );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static final void configureFaxBots()
	{
		KoLmafia.updateDisplay( "Configuring faxable monsters." );

		FaxBotDatabase.faxbots.clear();

		for ( BotData data : FaxBotDatabase.botData )
		{
			FaxBotDatabase.configureFaxBot( data );
		}

		KoLmafia.updateDisplay( "Faxable monster lists fetched." );
		FaxBotDatabase.isInitialized = true;
	}

	private static final void configureFaxBot( final BotData data )
	{
		FaxBotDatabase.faxBotConfigured = false;
		FaxBotDatabase.faxBotError = false;
		KoLmafia.forceContinue();

		( new DynamicBotFetcher( data ) ).start();

		PauseObject pauser = new PauseObject();

		while ( !FaxBotDatabase.faxBotError &&
			!FaxBotDatabase.faxBotConfigured )
		{
			pauser.pause( 200 );
		}

		if ( FaxBotDatabase.faxBotError )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Could not load " + data.name + " configuration from \"" + data.URL + "\"" );
			return;
		}
	}

	public static final FaxBot getFaxbot( final int i )
	{
		return ( i < 0 || i >= faxbots.size() ) ? null : FaxBotDatabase.faxbots.get(i);
	}

	public static final String botName( final int i )
	{
		FaxBot bot = FaxBotDatabase.getFaxbot( i );
		return bot == null ? null : bot.name;
	}

	public static class BotData
	{
		public final String name;
		public final String URL;

		public BotData( final String name, final String URL )
		{
			this.name = name;
			this.URL = URL;
		}
	}

	public static class FaxBot
		implements Comparable<FaxBot>
	{
		// Who is this bot?
		private final String name;
		private final int playerId;

		// What monsters does it serve?
		public final SortedListModel monsters = new SortedListModel();

		// Lists derived from the list of monsters
		private final LockableListModel categories = new LockableListModel();
		private LockableListModel [] monstersByCategory = new LockableListModel[0];

		private final Map<String, String> monsterByActualName = new HashMap<String, String>();
		private final Map<String, String> commandByActualName = new HashMap<String, String>();

		public FaxBot( final String name, final String playerId )
		{
			this( name, StringUtilities.parseInt( playerId ) );
		}

		public FaxBot( final String name, final int playerId )
		{
			this.name = name;
			this.playerId = playerId;
		}

		public String getName()
		{
			return this.name;
		}

		public int getPlayerId()
		{
			return this.playerId;
		}

		public LockableListModel getCategories()
		{
			return this.categories;
		}

		public LockableListModel [] getMonstersByCategory()
		{
			return this.monstersByCategory;
		}

		public String getMonsterByActualName( final String actualName )
		{
			return this.monsterByActualName.get( StringUtilities.getCanonicalName( actualName ) );
		}

		public String getCommandByActualName( final String actualName )
		{
			return this.commandByActualName.get( StringUtilities.getCanonicalName( actualName ) );
		}

		public void addMonsters( final List<Monster> monsters )
		{
			SortedListModel tempCategories = new SortedListModel();
			for ( Monster monster : monsters )
			{
				this.monsters.add( monster );
				String category = monster.category;
				if ( !category.equals( "" ) && !category.equalsIgnoreCase( "none" ) && !tempCategories.contains( category ) )
				{
					tempCategories.add( category );
				}
			}

			this.categories.add( "All Monsters" );
			this.categories.addAll( tempCategories );

			this.monsterByActualName.clear();

			// Make one list for each category
			this.monstersByCategory = new SortedListModel[ this.categories.size() ];
			for ( int i = 0; i < this.categories.size(); ++i )
			{
				String category = (String)categories.get( i );
				SortedListModel model = new SortedListModel();
				this.monstersByCategory[ i ] = model;
				for ( Monster monster : monsters )
				{
					if ( i == 0 || category.equals( monster.category ) )
					{
						model.add( monster );
					}
					// Build actual name / command lookup
					String canonical = StringUtilities.getCanonicalName( monster.actualName );
					this.monsterByActualName.put( canonical, monster.name );
					this.commandByActualName.put( canonical, monster.command );
				}
			}
		}

		public boolean hasCommand( final String command )
		{
			String canonical = StringUtilities.getCanonicalName( command );
			Collection<String> commands = this.commandByActualName.values();
			for ( String cmd : commands )
			{
				if ( cmd.equals( canonical ) )
				{
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof FaxBot ) )
			{
				return false;
			}

			FaxBot that = (FaxBot) o;
			return this.name.equals( that.name );
		}

		@Override
		public int hashCode()
		{
			return this.name != null ? this.name.hashCode() : 0;
		}

		public int compareTo( final FaxBot o )
		{
			if ( o == null || !( o instanceof FaxBot ) )
			{
				return -1;
			}

			FaxBot that = (FaxBot) o;
			return this.name.compareTo( that.name );
		}
	}

	public static class Monster
		implements Comparable<Monster>
	{
		private final String name;
		private final String actualName;
		private final String command;
		private final String category;

		private final String stringForm;
		private final String lowerCaseStringForm;

		public Monster( final String name, final String actualName, final String command, final String category )
		{
			this.name = name;
			this.actualName = actualName;
			this.command = command;
			this.category = category;
			this.stringForm = name + " [" + command + "]";
			this.lowerCaseStringForm = this.stringForm.toLowerCase();
		}

		public String getName()
		{
			return this.name;
		}

		public String getActualName()
		{
			return this.actualName;
		}

		public String getCommand()
		{
			return this.command;
		}

		public String getCategory()
		{
			return this.category;
		}

		@Override
		public String toString()
		{
			return this.stringForm;
		}

		public String toLowerCaseString()
		{
			return this.lowerCaseStringForm;
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof Monster ) )
			{
				return false;
			}

			Monster that = (Monster) o;
			return this.name.equals( that.name );
		}

		@Override
		public int hashCode()
		{
			return this.name != null ? this.name.hashCode() : 0;
		}

		public int compareTo( final Monster o )
		{
			if ( o == null || !( o instanceof Monster ) )
			{
				return -1;
			}

			Monster that = (Monster) o;
			return this.name.compareToIgnoreCase( that.name );
		}
	}

	private static class DynamicBotFetcher
		extends Thread
	{
		private final BotData data;

		public DynamicBotFetcher( final BotData data )
		{
			super( "DynamicBotFetcher" );
			this.data = data;
		}

		@Override
		public void run()
		{
			// Start with a clean slate
			FaxBotDatabase.faxBotConfigured = false;
			FaxBotDatabase.faxBotError = false;
			KoLmafia.forceContinue();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document dom = null;

			try
			{
				File local = new File( KoLConstants.DATA_LOCATION, this.data.name + ".xml" );
				FileUtilities.downloadFile( this.data.URL, local, true );

				// Get an instance of document builder
				DocumentBuilder db = dbf.newDocumentBuilder();

				// Parse using builder to get DOM
				// representation of the XML file
				dom = db.parse( local );
			}
			catch (ParserConfigurationException pce)
			{
			}
			catch (SAXException se)
			{
			}
			catch (IOException ioe)
			{
			}

			if ( dom == null )
			{
				FaxBotDatabase.faxBotError = true;
				return;
			}

			Element doc = dom.getDocumentElement();

			// Get a nodelist of bots
			ArrayList<FaxBot> bots = new ArrayList<FaxBot>();
			NodeList bl = doc.getElementsByTagName( "botdata" );
			if ( bl != null )
			{
				for ( int i = 0; i < bl.getLength(); i++ )
				{
					Element el = (Element)bl.item( i );
					FaxBot fb = getFaxBot( el );
					bots.add( fb );
				}
			}

			// Get a nodelist of monsters
			NodeList fl = doc.getElementsByTagName( "monsterdata" );
			ArrayList<Monster> monsters = new ArrayList<Monster>();
			if ( fl != null )
			{
				for ( int i = 0; i < fl.getLength(); i++ )
				{
					Element el = (Element)fl.item( i );
					Monster monster = getMonster( el );
					if ( monster != null )
					{
						monsters.add( monster );
					}
				}
			}

			// For each bot, add available monsters
			for ( FaxBot bot : bots )
			{
				bot.addMonsters( monsters );
			}

			// Add the bots to the list of available bots
			FaxBotDatabase.faxbots.addAll( bots );

			// Say that this config file has been processed
			FaxBotDatabase.faxBotConfigured = true;
		}

		private FaxBot getFaxBot( Element el )
		{
			String name = getTextValue( el, "name" );
			String playerId = getTextValue( el, "playerid" );
			ContactManager.registerPlayerId( name, playerId );
			KoLmafia.updateDisplay( "Configuring " + name + " (" + playerId + ")" );
			return new FaxBot( name, playerId );
		}

		private Monster getMonster( Element el )
		{
			String monster = getTextValue( el, "name" );
			if ( monster.equals( "" ) || monster.equals( "none" ) )
			{
				return null;
			}
			String actualMonster = getTextValue( el, "actual_name" );
			if ( actualMonster.equals( "" ) )
			{
				return null;
			}
			String command = getTextValue( el, "command" );
			if ( command.equals( "" ) )
			{
				return null;
			}
			String category = getTextValue( el, "category" );
			return new Monster( monster, actualMonster, command, category );
		}

		private String getTextValue( Element ele, String tagName )
		{
			NodeList nl = ele.getElementsByTagName( tagName );
			if ( nl != null && nl.getLength() > 0 )
			{
				Element el = (Element)nl.item(0);
				return el.getFirstChild().getNodeValue();
			}

			return "";
		}
	}
}
