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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;

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
	final static String LOCATION = "http://www.hogsofdestiny.com/faxbot/faxbot.xml";

	private static boolean isInitialized = false;
	private static boolean faxBotConfigured = false;
	private static boolean faxBotError = false;

	public static final LockableListModel faxbots = new LockableListModel();
	public static final SortedListModel monsters = new SortedListModel();
        public static final LockableListModel categories = new LockableListModel();
        public static LockableListModel [] monstersByCategory;

	public static final void configure()
	{
		FaxBotDatabase.configureFaxBot();
	}

	public static final void reconfigure()
	{
		FaxBotDatabase.isInitialized = false;
		FaxBotDatabase.configure();
	}

	private static final void configureFaxBot()
	{
		if ( FaxBotDatabase.isInitialized )
		{
			return;
		}

		KoLmafia.updateDisplay( "Configuring available monsters." );

		if ( !FaxBotDatabase.configureFaxBot( LOCATION ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Could not load Faxbot configuration" );
		}

		// Iterate over all monsters and make a list of categories
		SortedListModel temp = new SortedListModel();
		for ( int i = 0; i < monsters.size(); ++i )
		{
			Monster monster = (Monster)monsters.get( i );
			if ( !temp.contains( monster.category ) )
			{
				temp.add( monster.category );
			}
		}

		categories.add( "All Monsters" );
		categories.addAll( temp );

		// Make one list for each category
		monstersByCategory = new SortedListModel[ categories.size() ];
		for ( int i = 0; i < categories.size(); ++i )
		{
			String category = (String)categories.get( i );
			SortedListModel model = new SortedListModel();
			monstersByCategory[ i ] = model;
			for ( int j = 0; j < monsters.size(); ++j )
			{
				Monster monster = (Monster)monsters.get( j );
				if ( i == 0 || category.equals( monster.category ) )
				{
					model.add( monster );
				}
			}
		}

		KoLmafia.updateDisplay( "Fax list fetched." );
		FaxBotDatabase.isInitialized = true;
	}

	private static final boolean configureFaxBot( final String URL )
	{
		FaxBotDatabase.faxBotConfigured = false;
		FaxBotDatabase.faxBotError = false;

		( new DynamicBotFetcher( URL ) ).start();

		PauseObject pauser = new PauseObject();

		while ( !FaxBotDatabase.faxBotError &&
			!FaxBotDatabase.faxBotConfigured )
		{
			pauser.pause( 200 );
		}

		return !FaxBotDatabase.faxBotError;
	}

	public static final String botName( final int i )
	{
		if ( i >= faxbots.size() )
		{
			return null;
		}
		return ((FaxBot)faxbots.get(i)).name;
	}

	public static class FaxBot
		implements Comparable<FaxBot>
	{
		private final String name;
		private final int playerId;

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
		private final String command;
		private final String category;

		private final String stringForm;
		private final String lowerCaseStringForm;

		public Monster( final String name, final String command, final String category )
		{
			this.name = name;
			this.command = command;
			this.category = category;
			this.stringForm = name + " [" + command + "]";
			this.lowerCaseStringForm = this.stringForm.toLowerCase();
		}

		public String getName()
		{
			return this.name;
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
			return this.name.compareTo( that.name );
		}
	}

	private static class DynamicBotFetcher
		extends Thread
	{
		private final String location;

		public DynamicBotFetcher( final String location )
		{
			super( "DynamicBotFetcher" );
			this.location = location;
		}

		@Override
		public void run()
		{
			// Start with a clean slate
			FaxBotDatabase.faxBotConfigured = false;
			FaxBotDatabase.faxBotError = false;
			FaxBotDatabase.faxbots.clear();
			FaxBotDatabase.monsters.clear();
			KoLmafia.forceContinue();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document dom = null;

			try
			{
				File local = new File( KoLConstants.DATA_LOCATION, "faxbot.xml" );
				FileUtilities.downloadFile( this.location, local, true );
		
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
				KoLmafia.updateDisplay( MafiaState.ABORT, "Could not load faxbot configuration from " + this.location );
				FaxBotDatabase.faxBotError = true;
				return;
			}

			Element doc = dom.getDocumentElement();

			// Get a nodelist of bots
			NodeList bl = doc.getElementsByTagName( "botdata" );
			if ( bl != null )
			{
				for ( int i = 0; i < bl.getLength(); i++ )
				{
					Element el = (Element)bl.item( i );
					FaxBot fb = getFaxBot( el );
					FaxBotDatabase.faxbots.add( fb );
				}
			}

			// Get a nodelist of monsters
			NodeList fl = doc.getElementsByTagName( "monsterdata" );
			if ( fl != null )
			{
				for ( int i = 0; i < fl.getLength(); i++ )
				{
					Element el = (Element)fl.item( i );
					Monster fax = getMonster( el );
					FaxBotDatabase.monsters.add( fax );
				}
			}

			FaxBotDatabase.faxBotConfigured = true;
		}

                private FaxBot getFaxBot( Element el )
		{
                        String name = getTextValue( el, "name" );
                        String playerId = getTextValue( el, "playerid" );
                        ContactManager.registerPlayerId( name, playerId );
                        return new FaxBot( name, playerId );
                }

                private Monster getMonster( Element el )
		{
                        String monster = getTextValue( el, "name" );
                        String command = getTextValue( el, "command" );
                        String category = getTextValue( el, "category" );
                        return new Monster( monster, command, category );
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

		private int getIntValue( Element ele, String tagName )
		{
                        String text = getTextValue( ele, tagName );
                        if ( text == null )
                        {
                                return 0;
                        }
                        return StringUtilities.parseInt( text );
                }
	}
}
