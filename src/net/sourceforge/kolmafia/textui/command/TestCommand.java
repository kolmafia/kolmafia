/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.awt.Frame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatMessage;
import net.sourceforge.kolmafia.chat.ChatParser;
import net.sourceforge.kolmafia.chat.ChatPoller;

import net.sourceforge.kolmafia.combat.CombatUtilities;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.RumpleManager;

import net.sourceforge.kolmafia.swingui.SkillBuffFrame;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;

public class TestCommand
	extends AbstractCommand
{
	private static String contents = null;

	public TestCommand()
	{
		this.usage = " 1, 2, 3...";
	}

	private static void dump( final String data )
	{
		File file = new File( KoLConstants.DATA_LOCATION, "testCommand.html" );
		try
		{
			OutputStream o = DataUtilities.getOutputStream( file );
			BufferedWriter w = new BufferedWriter( new OutputStreamWriter( o ) );
			w.write( data );
			w.flush();
			o.close();
		}
		catch ( Exception e )
		{
		}
	}

	private static Frame findFrame( final Class type )
	{
		for ( Frame frame : Frame.getFrames() )
		{
			if ( frame.getClass() == type )
			{
				return frame;
			}
		}
		return null;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		// Load an HTML file to be used by a subsequent "test" command
		if ( command.equals( "load" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Load what?" );
				return;
			}

			String fileName = split[ 1 ];
			File file = new File( KoLConstants.DATA_LOCATION, fileName );

			if ( !file.exists() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "File " + file + " does not exist" );
				return;
			}
			
			byte[] bytes = ByteBufferUtilities.read( file );
			String string = StringUtilities.getEncodedString( bytes, "UTF-8" );
			TestCommand.contents = string;

			KoLmafia.updateDisplay( "Read " + KoLConstants.COMMA_FORMAT.format( bytes.length ) +
						" bytes into a " + KoLConstants.COMMA_FORMAT.format( string.length() ) +
						" character string" );
		}

		// *** Commands that do not depend on a loaded HTML file, in alphabetical order

		if ( command.equals( "adventure" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test adventure URL" );
				return;
			}
			String adventureURL = split[ 1 ].trim();
			KoLAdventure adventure = AdventureDatabase.getAdventureByURL( adventureURL );
			RequestLogger.printLine( "returned " + adventure );
			return;
		}

		if ( command.equals( "canonical" ) )
		{
			String string;
			if ( TestCommand.contents == null )
			{
				int index = parameters.indexOf( " " );
				string = parameters.substring( index + 1 );
			}
			else
			{
				string = TestCommand.contents.trim();
				TestCommand.contents = null;
			}
			String canonical = StringUtilities.getEntityEncode( string, false );
			String escaped = CharacterEntities.escape( canonical );
			RequestLogger.printLine( "canonical(" + canonical.length() + ") = \"" + escaped + "\"" );
			return;
		}

		if ( command.equals( "cturns" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test cturns URL" );
				return;
			}
			String URL = parameters.substring( parameters.indexOf( " " ) + 1 ).trim();
			GenericRequest request = new GenericRequest( URL );
			int turns = CreateItemRequest.getAdventuresUsed( request );
			RequestLogger.printLine( "That will require " + turns + " turns" );
			return;
		}

		if ( command.equals( "dump_disabled_skills" ) )
		{
			Frame frame = TestCommand.findFrame( SkillBuffFrame.class );
			if ( frame != null )
			{
				SkillBuffFrame sbf = (SkillBuffFrame) frame;
				sbf.dumpDisabledSkills();
			}
			return;
		}

		if ( command.equals( "fairy" ) )
		{
			FamiliarData familiar = KoLCharacter.getFamiliar();
			if ( split.length >= 2 )
			{
				int index = parameters.indexOf( " " );
				String race = parameters.substring( index + 1 ).trim();
				familiar = KoLCharacter.findFamiliar( race );
			}
			if ( familiar == null || familiar == FamiliarData.NO_FAMILIAR )
			{
				return;
			}
			double itemDrop = Modifiers.getNumericModifier( "Familiar", familiar.getRace(), "Item Drop" );
			RequestLogger.printLine( "Item Drop: " + itemDrop );
			return;
		}

		if ( command.equals( "hitchance" ) )
		{
			if ( split.length < 5 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test hitchance attack defense critical fumble" );
				return;
			}

			int attack = StringUtilities.parseInt( split[ 1 ] );
			int defense = StringUtilities.parseInt( split[ 2 ] );
			int critical = StringUtilities.parseInt( split[ 3 ] );
			int fumble = StringUtilities.parseInt( split[ 4 ] );

			float hitchance = CombatUtilities.hitChance( attack, defense, critical / 100.0f, fumble / 100.0f );

			RequestLogger.printLine( "Hit Chance: " + (int)Math.round( 100.0 * hitchance ) );

			return;
		}

		if ( command.equals( "intcache" ) )
		{
			int cacheHits = IntegerPool.getCacheHits();
			int cacheMissLows = IntegerPool.getCacheMissLows();
			int cacheMissHighs = IntegerPool.getCacheMissHighs();
			int totalAccesses = cacheHits + cacheMissLows + cacheMissHighs;

			float successRate = 0.0f;

			if ( totalAccesses != 0 )
			{
				successRate = (float) cacheHits / (float) totalAccesses * 100.0f;
			}

			RequestLogger.printLine( "cache hits: " + cacheHits );
			RequestLogger.printLine( "cache misses (too low): " + cacheMissLows );
			RequestLogger.printLine( "cache misses (too high): " + cacheMissHighs );
			RequestLogger.printLine( "success rate: " + successRate + " %" );

			return;
		}

		if ( command.equals( "leet" ) )
		{
			int index = parameters.indexOf( " " );
			if ( index == -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test leet NAME" );
				return;
			}
			String name = parameters.substring( index + 1 ).trim();
			String leet = StringUtilities.leetify( name );
			String monsterName = MonsterDatabase.translateLeetMonsterName( leet );
			RequestLogger.printLine( name + " -> " + leet + " -> " +  monsterName );
			return;
		}

		if ( command.equals( "neweffect" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test neweffect descId" );
				return;
			}

			String descId = split[ 1 ].trim();
			EffectDatabase.learnEffectId( null, descId );
			return;
		}

		if ( command.equals( "newitem" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test newitem descId" );
				return;
			}

			String descId = split[ 1 ].trim();
			ItemDatabase.registerItem( descId );
			return;
		}

		if ( command.equals( "places" ) )
		{
			int count = PlaceRequest.places.size();
			if ( count == 0 )
			{
				RequestLogger.printLine( "No unclaimed places" );
				return;
			}
			
			for ( String place : PlaceRequest.places )
			{
				RequestLogger.printLine( "place.php?whichplace=" + place );
			}

			return;
		}

		if ( command.equals( "register" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test register URL..." );
				return;
			}

			GenericRequest request = new GenericRequest( "" );
			for ( int i = 1; i < split.length; ++i )
			{
				String urlString = split[ i ];
				request.constructURLString( urlString );
				KoLAdventure.lastVisitedLocation = null;
				KoLAdventure.locationLogged = false;
				KoLAdventure.lastLocationName = null;
				KoLAdventure.lastLocationURL = null;
				RequestLogger.registerRequest( request, urlString );
				if ( KoLAdventure.lastLocationName != null )
				{
					KoLAdventure.recordToSession( urlString, "Response text" );
				}
			}
			return;
		}

		if ( command.equals( "relstring" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test relstring RELSTRING" );
				return;
			}
			AdventureResult result = ItemDatabase.itemFromRelString( split[ 1 ] );
			RequestLogger.printLine( "returned " + result );
			return;
		}

		if ( command.equals( "result" ) )
		{
			String text;
			if ( TestCommand.contents == null )
			{
				int index = parameters.indexOf( " " );
				text = parameters.substring( index + 1 );
			}
			else
			{
				text = TestCommand.contents.trim();
				TestCommand.contents = null;
			}

			boolean result = ResultProcessor.processResults( false, text, null );
			RequestLogger.printLine( "returned " + result );
			ConcoctionDatabase.refreshConcoctionsNow();
			return;
		}

		if ( command.equals( "row" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test row #" );
				return;
			}

			int row = StringUtilities.parseInt( split[ 1 ] );
			int itemId = ConcoctionPool.rowToId( row );
			if ( itemId < 0 )
			{
				RequestLogger.printLine( "That row doesn't map to a known item." );
				return;
			}
			String itemName = ItemDatabase.getItemName( itemId );
			Concoction concoction = ConcoctionPool.get( itemId );
			RequestLogger.printLine( "Row " + row + " -> \"" + itemName + "\" (" + itemId + ") " + ( concoction == null ? "IS NOT" : "is" ) + " a known concoction" );
			return;
		}

		if ( command.equals( "state" ) )
		{
			if ( split.length >= 2 )
			{
				int index = parameters.indexOf( " " );
				String state = parameters.substring( index + 1 ).trim();
				RequestLogger.printLine( KoLmafia.getSaveState( state ) );
			}
			return;
		}
		
		if ( command.equals( "xpath" ) )
		{
			File htmlFile;
			String xpath;

			if ( !split[ 1 ].endsWith( ".html" ) )
			{
				split = parameters.split( " ", 2 );
				htmlFile = new File( KoLConstants.DATA_LOCATION, "test.html" );
				xpath = split[ 1 ];
			}
			else
			{
				split = parameters.split( " ", 3 );
				htmlFile = new File( KoLConstants.DATA_LOCATION, split[ 1 ] );
				xpath = split[ 2 ];
			}

			if ( !htmlFile.exists() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "File " + htmlFile + " does not exist" );
				return;
			}

			HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();

			TagNode doc;
			try
			{
				doc = cleaner.clean( htmlFile );
			}
			catch ( IOException e )
			{
				StaticEntity.printStackTrace( e );
				return;
			}

			Object[] result;
			try
			{
				result = doc.evaluateXPath( xpath );
			}
			catch ( XPatherException e )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "invalid xpath expression" );
				return;
			}

			if ( result.length == 0 )
				RequestLogger.printLine( "no matches." );

			for ( int i = 0; i < result.length; i++ )
			{
				RequestLogger.printLine( "<b>" + ( i + 1 ) + ":</b> " + result[ i ] );
			}
			//RequestLogger.printList( Arrays.asList( result ) );

			return;
		}

		// *** Commands that DO depend on a loaded HTML file, in alphabetical order

		if ( TestCommand.contents == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "no HTML loaded." );
			return;
		}

		if ( command.equals( "aagain" ) )
		{
			StringBuffer buffer = new StringBuffer( TestCommand.contents );
			TestCommand.contents = null;
			String location = StationaryButtonDecorator.getAdventureAgainLocation( buffer );
			RequestLogger.printLine( location );
			return;
		}

		if ( command.equals( "barrel" ) )
		{
			StringBuffer buffer = new StringBuffer( TestCommand.contents );
			TestCommand.contents = null;
			BarrelDecorator.decorate( buffer );
			TestCommand.dump( buffer.toString() );
			return;
		}

		if ( command.equals( "charpane" ) )
		{
			CharPaneRequest.processResults( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "chat" ) )
		{
			List<ChatMessage> chatMessages = ChatParser.parseLines(TestCommand.contents );
			ChatManager.processMessages( chatMessages );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "choice" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test choice CHOICE" );
				return;
			}
			int choice = StringUtilities.parseInt( split[ 1 ] );
			String lastResponseText = ChoiceManager.lastResponseText;
			boolean handlingChoice = ChoiceManager.handlingChoice;
			int lastChoice = ChoiceManager.lastChoice;
			try
			{
				ChoiceManager.lastResponseText = TestCommand.contents;
				ChoiceManager.handlingChoice = true;
				ChoiceManager.lastChoice = choice;
				TestCommand.contents = null;
				RequestLogger.printLine( "default decision = " + ChoiceManager.getDecision( choice, ChoiceManager.lastResponseText ) );
				ChoiceCommand.printChoices();
			}
			finally
			{
				ChoiceManager.lastResponseText = lastResponseText;
				ChoiceManager.handlingChoice = handlingChoice;
				ChoiceManager.lastChoice = lastChoice;
			}
			return;
		}

		if ( command.equals( "dad" ) )
		{
			if ( !DadManager.solve( TestCommand.contents ) )
			{
				RequestLogger.printLine( "Unable to solve for elemental weaknesses" );
			}
			CLI.executeLine( "dad" );
			return;
		}

		if ( command.equals( "decorate" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test decorate URL" );
				return;
			}
			String urlString = split[ 1 ].trim();
			StringBuffer buffer = new StringBuffer( TestCommand.contents );
			TestCommand.contents = null;
			RequestEditorKit.getFeatureRichHTML( urlString, buffer, true );
			TestCommand.dump( buffer.toString() );
			return;
		}

		if ( command.equals( "div" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test div LABEL" );
				return;
			}
			String label = split[ 1 ].trim();
			String string = ResponseTextParser.parseDivLabel( label, TestCommand.contents );
			TestCommand.contents = null;
			RequestLogger.printLine( "string = \"" + string + "\"" );
			return;
		}

		if ( command.equals( "dvorak" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test dvorak URL" );
				return;
			}
			String url = split[ 1 ].trim();
			DvorakManager.registerRequest( url );
			DvorakManager.parseResponse( url, TestCommand.contents );
			DvorakManager.printTiles();
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "encounter" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "test encounter URL" );
				return;
			}

			String urlString = split[ 1 ];
			GenericRequest request = new GenericRequest( urlString );
			request.responseText = TestCommand.contents;
			TestCommand.contents = null;

			KoLAdventure.lastVisitedLocation = null;
			KoLAdventure.locationLogged = false;
			KoLAdventure.lastLocationName = null;
			KoLAdventure.lastLocationURL = null;

			RequestLogger.registerRequest( request, urlString );
			AdventureRequest.registerEncounter( request );
			return;
		}

		if ( command.equals( "equipment" ) )
		{
			EquipmentRequest.parseEquipment( "inventory.php?which=2", TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "events" ) )
		{
			EventManager.checkForNewEvents( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "fight" ) )
		{
			int round = split.length > 1 ? StringUtilities.parseInt( split[ 1 ].trim() ) : -1;
			String adventureName = split.length > 2 ? split[ 2 ].trim() : Preferences.getString( "nextAdventure" );
			if ( round >= 0 )
			{
				KoLAdventure.setLastAdventure( AdventureDatabase.getAdventure( adventureName ) );
				String encounter = AdventureRequest.parseMonsterEncounter( TestCommand.contents );
				MonsterStatusTracker.setNextMonsterName( encounter );
				FightRequest.currentRound = round;
				FightRequest.updateCombatData( "fight.php", encounter, TestCommand.contents );
			}
			else
			{
				FightRequest.parseFightHTML( TestCommand.contents );
			}
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "generator" ) )
		{
			SpaaaceRequest.visitGeneratorChoice( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "location" ) )
		{
			StringBuffer buffer = new StringBuffer( TestCommand.contents );
			TestCommand.contents = null;
			RequestEditorKit.addNewLocationLinks( buffer );
			TestCommand.dump( buffer.toString() );
			return;
		}

		if ( command.equals( "mchat" ) )
		{
			List<ChatMessage> chatMessages = ChatPoller.parseNewChat( TestCommand.contents );
			ChatManager.processMessages( chatMessages );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "monster" ) )
		{
			String encounter = AdventureRequest.parseMonsterEncounter( TestCommand.contents );
			MonsterStatusTracker.setNextMonsterName( encounter );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "rumple" ) )
		{
			RumpleManager.spyOnParents( TestCommand.contents );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "speakeasy" ) )
		{
			ClanLoungeRequest.parseSpeakeasy( TestCommand.contents, true );
			TestCommand.contents = null;
			return;
		}

		if ( command.equals( "taleofdread" ) )
		{
			String tale = TaleOfDreadCommand.extractTale( TestCommand.contents );
			TestCommand.contents = null;
			RequestLogger.printLine( tale );
			return;
		}

		if ( command.equals( "visit-choice" ) )
		{
			GenericRequest request = new GenericRequest( "choice.php" );
			request.addFormField( "forceoption", "0" );
			request.responseText = TestCommand.contents;
			ChoiceManager.lastChoice = 0;
			ChoiceManager.visitChoice( request );
			RequestLogger.printLine( "choice = " + ChoiceManager.lastChoice );
			TreeMap<Integer,String> choices = ChoiceCommand.parseChoices( TestCommand.contents );
			TestCommand.contents = null;
			for ( Map.Entry<Integer,String> entry : choices.entrySet() )
			{
				RequestLogger.printLine( "<b>choice " + entry.getKey() + "</b>: " + entry.getValue() );
			}
			return;
		}
	}
}
