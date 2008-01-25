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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;

public class KoLmafiaASH
	extends StaticEntity
{
	/* Variables for Advanced Scripting */

	public static final char[] tokenList =
		{ ' ', '.', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '\'', '*', '^', '/', '%', '[', ']', '!', ';', '<', '>' };
	public static final String[] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&", "/*", "*/" };

	public static final int TYPE_ANY = 0;
	public static final int TYPE_VOID = 1;
	public static final int TYPE_BOOLEAN = 2;
	public static final int TYPE_INT = 3;
	public static final int TYPE_FLOAT = 4;
	public static final int TYPE_STRING = 5;
	public static final int TYPE_BUFFER = 6;
	public static final int TYPE_MATCHER = 7;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_LOCATION = 101;
	public static final int TYPE_CLASS = 102;
	public static final int TYPE_STAT = 103;
	public static final int TYPE_SKILL = 104;
	public static final int TYPE_EFFECT = 105;
	public static final int TYPE_FAMILIAR = 106;
	public static final int TYPE_SLOT = 107;
	public static final int TYPE_MONSTER = 108;
	public static final int TYPE_ELEMENT = 109;

	public static final int TYPE_AGGREGATE = 1000;
	public static final int TYPE_RECORD = 1001;
	public static final int TYPE_TYPEDEF = 1002;

	public static final String[] CLASSES =
	{
		KoLCharacter.SEAL_CLUBBER,
		KoLCharacter.TURTLE_TAMER,
		KoLCharacter.PASTAMANCER,
		KoLCharacter.SAUCEROR,
		KoLCharacter.DISCO_BANDIT,
		KoLCharacter.ACCORDION_THIEF
	};
	public static final String[] STATS = { "Muscle", "Mysticality", "Moxie" };
	public static final String[] BOOLEANS = { "true", "false" };

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;
	public static final int COMMAND_EXIT = 3;

	public static final int STATE_NORMAL = 1;
	public static final int STATE_RETURN = 2;
	public static final int STATE_BREAK = 3;
	public static final int STATE_CONTINUE = 4;
	public static final int STATE_EXIT = 5;

	private static final ScriptType ANY_TYPE = new ScriptType( "any", KoLmafiaASH.TYPE_ANY );
	private static final ScriptType VOID_TYPE = new ScriptType( "void", KoLmafiaASH.TYPE_VOID );
	private static final ScriptType BOOLEAN_TYPE = new ScriptType( "boolean", KoLmafiaASH.TYPE_BOOLEAN );
	private static final ScriptType INT_TYPE = new ScriptType( "int", KoLmafiaASH.TYPE_INT );
	private static final ScriptType FLOAT_TYPE = new ScriptType( "float", KoLmafiaASH.TYPE_FLOAT );
	private static final ScriptType STRING_TYPE = new ScriptType( "string", KoLmafiaASH.TYPE_STRING );
	private static final ScriptType BUFFER_TYPE = new ScriptType( "buffer", KoLmafiaASH.TYPE_BUFFER );
	private static final ScriptType MATCHER_TYPE = new ScriptType( "matcher", KoLmafiaASH.TYPE_MATCHER );

	private static final ScriptType ITEM_TYPE = new ScriptType( "item", KoLmafiaASH.TYPE_ITEM );
	private static final ScriptType LOCATION_TYPE = new ScriptType( "location", KoLmafiaASH.TYPE_LOCATION );
	private static final ScriptType CLASS_TYPE = new ScriptType( "class", KoLmafiaASH.TYPE_CLASS );
	private static final ScriptType STAT_TYPE = new ScriptType( "stat", KoLmafiaASH.TYPE_STAT );
	private static final ScriptType SKILL_TYPE = new ScriptType( "skill", KoLmafiaASH.TYPE_SKILL );
	private static final ScriptType EFFECT_TYPE = new ScriptType( "effect", KoLmafiaASH.TYPE_EFFECT );
	private static final ScriptType FAMILIAR_TYPE = new ScriptType( "familiar", KoLmafiaASH.TYPE_FAMILIAR );
	private static final ScriptType SLOT_TYPE = new ScriptType( "slot", KoLmafiaASH.TYPE_SLOT );
	private static final ScriptType MONSTER_TYPE = new ScriptType( "monster", KoLmafiaASH.TYPE_MONSTER );
	private static final ScriptType ELEMENT_TYPE = new ScriptType( "element", KoLmafiaASH.TYPE_ELEMENT );

	private static final ScriptType AGGREGATE_TYPE = new ScriptType( "aggregate", KoLmafiaASH.TYPE_AGGREGATE );
	private static final ScriptAggregateType RESULT_TYPE =
		new ScriptAggregateType( KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE );
	private static final ScriptAggregateType REGEX_GROUP_TYPE =
		new ScriptAggregateType(
			new ScriptAggregateType( KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE ), KoLmafiaASH.INT_TYPE );

	// Common values

	private static final ScriptValue VOID_VALUE = new ScriptValue();
	private static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	private static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	private static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	private static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	private static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0f );

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	private static final ScriptValue BOOLEAN_INIT = KoLmafiaASH.FALSE_VALUE;
	private static final ScriptValue INT_INIT = KoLmafiaASH.ZERO_VALUE;
	private static final ScriptValue FLOAT_INIT = KoLmafiaASH.ZERO_FLOAT_VALUE;
	private static final ScriptValue STRING_INIT = new ScriptValue( "" );

	private static final ScriptValue ITEM_INIT = new ScriptValue( KoLmafiaASH.ITEM_TYPE, -1, "none" );
	private static final ScriptValue LOCATION_INIT = new ScriptValue( KoLmafiaASH.LOCATION_TYPE, "none", (Object) null );
	private static final ScriptValue CLASS_INIT = new ScriptValue( KoLmafiaASH.CLASS_TYPE, -1, "none" );
	private static final ScriptValue STAT_INIT = new ScriptValue( KoLmafiaASH.STAT_TYPE, -1, "none" );
	private static final ScriptValue SKILL_INIT = new ScriptValue( KoLmafiaASH.SKILL_TYPE, -1, "none" );
	private static final ScriptValue EFFECT_INIT = new ScriptValue( KoLmafiaASH.EFFECT_TYPE, -1, "none" );
	private static final ScriptValue FAMILIAR_INIT = new ScriptValue( KoLmafiaASH.FAMILIAR_TYPE, -1, "none" );
	private static final ScriptValue SLOT_INIT = new ScriptValue( KoLmafiaASH.SLOT_TYPE, -1, "none" );
	private static final ScriptValue MONSTER_INIT = new ScriptValue( KoLmafiaASH.MONSTER_TYPE, "none", (Object) null );
	private static final ScriptValue ELEMENT_INIT = new ScriptValue( KoLmafiaASH.ELEMENT_TYPE, "none", (Object) null );

	// Variables used during parsing

	private static final ScriptFunctionList existingFunctions = KoLmafiaASH.getExistingFunctions();
	private static final ScriptTypeList simpleTypes = KoLmafiaASH.getSimpleTypes();
	private static final ScriptSymbolTable reservedWords = KoLmafiaASH.getReservedWords();

	private static final TreeMap relayScriptMap = new TreeMap();
	private static final StringBuffer serverReplyBuffer = new StringBuffer();
	private static final RelayRequest relayRequest = new RelayRequest( false );

	private static KoLmafiaASH relayScript = null;
	private static KoLmafiaASH currentAnalysis = null;
	private static StringBuffer concatenateBuffer = new StringBuffer();

	private String fileName;
	private String fullLine;
	private String currentLine;
	private String nextLine;

	private int lineNumber;
	private LineNumberReader commandStream;
	private TreeMap imports = new TreeMap();
	private ScriptFunction mainMethod = null;

	// Variables used during execution

	private ScriptScope global;
	private static int currentState = KoLmafiaASH.STATE_NORMAL;
	private static boolean isExecuting = false;
	private static String lastImportString = "";
	private String notifyRecipient = null;

	// Feature control;
	// disabled until and if we choose to document the feature

	private static final boolean arrays = false;

	private static final TreeMap TIMESTAMPS = new TreeMap();
	private static final TreeMap INTERPRETERS = new TreeMap();

	private static final GenericRequest VISITOR = new GenericRequest( "" );
	private static final RelayRequest RELAYER = new RelayRequest( false );
	public static final KoLmafiaASH NAMESPACE_INTERPRETER = new KoLmafiaASH();

	public KoLmafiaASH()
	{
		this.global = new ScriptScope( new ScriptVariableList(), this.getExistingFunctionScope() );
	}

	public String getFileName()
	{
		return this.fileName;
	}

	private KoLmafiaASH( final KoLmafiaASH source, final File scriptFile )
	{
		this.global = source.global;
		this.imports = source.imports;
		this.fileName = scriptFile.getPath();

		try
		{
			this.commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );

			this.currentLine = this.getNextLine();
			this.lineNumber = this.commandStream.getLineNumber();
			this.nextLine = this.getNextLine();
		}
		catch ( Exception e )
		{
			// If any part of the initialization fails,
			// then throw an exception.

			throw new AdvancedScriptException( this.fileName + " could not be accessed" );
		}
	}

	public static final boolean getClientHTML( final RelayRequest request )
	{
		String script = request.getPath();

		if ( KoLmafiaASH.relayScriptMap.containsKey( script ) )
		{
			File toExecute = (File) KoLmafiaASH.relayScriptMap.get( script );
			return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
		}

		if ( !script.endsWith( ".ash" ) )
		{
			if ( !script.endsWith( ".php" ) )
			{
				return false;
			}

			script = script.substring( 0, script.length() - 4 ) + ".ash";
		}

		File toExecute = new File( KoLConstants.RELAY_LOCATION, script );
		KoLmafiaASH.relayScriptMap.put( request.getPath(), toExecute );
		return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
	}

	private static final boolean getClientHTML( final RelayRequest request, final File toExecute )
	{
		synchronized ( KoLmafiaASH.serverReplyBuffer )
		{
			KoLmafiaASH.relayScript = KoLmafiaASH.getInterpreter( (File) toExecute );
			if ( KoLmafiaASH.relayScript == null )
			{
				return false;
			}

			KoLmafiaASH.serverReplyBuffer.setLength( 0 );
			KoLmafiaASH.relayRequest.constructURLString( request.getURLString() );

			KoLmafiaASH.relayScript.execute( "main", null );

			if ( KoLmafiaASH.serverReplyBuffer.length() == 0 )
			{
				if ( KoLmafiaASH.relayRequest.responseText != null && KoLmafiaASH.relayRequest.responseText.length() != 0 )
				{
					KoLmafiaASH.serverReplyBuffer.append( KoLmafiaASH.relayRequest.responseText );
				}
			}

			if ( KoLmafiaASH.serverReplyBuffer.length() != 0 )
			{
				request.pseudoResponse( "HTTP/1.1 200 OK", KoLmafiaASH.serverReplyBuffer.toString() );
			}

			KoLmafiaASH.relayScript = null;
			return KoLmafiaASH.serverReplyBuffer.length() != 0;
		}
	}

	public static final KoLmafiaASH getInterpreter( final File toExecute )
	{
		if ( toExecute == null )
		{
			return null;
		}

		KoLmafiaASH interpreter;
		boolean createInterpreter = !KoLmafiaASH.TIMESTAMPS.containsKey( toExecute );

		if ( !createInterpreter )
		{
			createInterpreter =
				( (Long) KoLmafiaASH.TIMESTAMPS.get( toExecute ) ).longValue() != toExecute.lastModified();
		}

		if ( !createInterpreter )
		{
			interpreter = (KoLmafiaASH) KoLmafiaASH.INTERPRETERS.get( toExecute );
			Iterator it = interpreter.imports.keySet().iterator();

			while ( it.hasNext() && !createInterpreter )
			{
				File file = (File) it.next();
				createInterpreter = ( (Long) interpreter.imports.get( file ) ).longValue() != file.lastModified();
			}
		}

		if ( createInterpreter )
		{
			KoLmafiaASH.TIMESTAMPS.clear();
			interpreter = new KoLmafiaASH();

			if ( !interpreter.validate( toExecute ) )
			{
				return null;
			}

			KoLmafiaASH.TIMESTAMPS.put( toExecute, new Long( toExecute.lastModified() ) );
			KoLmafiaASH.INTERPRETERS.put( toExecute, interpreter );
		}

		return (KoLmafiaASH) KoLmafiaASH.INTERPRETERS.get( toExecute );
	}

	// **************** Data Types *****************

	// For each simple data type X, we supply:
	// private static final ScriptValue parseXValue( String name );

	private static final ScriptValue parseBooleanValue( final String name )
	{
		if ( name.equalsIgnoreCase( "true" ) )
		{
			return KoLmafiaASH.TRUE_VALUE;
		}
		if ( name.equalsIgnoreCase( "false" ) )
		{
			return KoLmafiaASH.FALSE_VALUE;
		}

		if ( KoLmafiaASH.isExecuting )
		{
			return StaticEntity.parseInt( name ) == 0 ? KoLmafiaASH.FALSE_VALUE : KoLmafiaASH.TRUE_VALUE;
		}

		throw new AdvancedScriptException( "Can't interpret '" + name + "' as a boolean" );
	}

	private static final ScriptValue parseIntValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseInt( name ) );
	}

	private static final ScriptValue parseFloatValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseFloat( name ) );
	}

	private static final ScriptValue parseStringValue( final String name )
	{
		return new ScriptValue( name );
	}

	private static final ScriptValue parseItemValue( String name )
	{
		if ( name == null || name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.ITEM_INIT;
		}

		// Allow for an item number to be specified
		// inside of the "item" construct.

		int itemId;
		for ( int i = 0; i < name.length(); ++i )
		{
			if ( !Character.isDigit( name.charAt( i ) ) )
			{
				AdventureResult item = KoLmafiaCLI.getFirstMatchingItem( name, false );

				if ( item == null )
				{
					if ( KoLmafiaASH.isExecuting )
					{
						return KoLmafiaASH.ITEM_INIT;
					}

					throw new AdvancedScriptException( "Item " + name + " not found in database" );
				}

				itemId = item.getItemId();
				name = ItemDatabase.getItemName( itemId );
				return new ScriptValue( KoLmafiaASH.ITEM_TYPE, itemId, name );
			}
		}

		// Since it is numeric, parse the integer value
		// and store it inside of the contentInt.

		itemId = StaticEntity.parseInt( name );
		name = ItemDatabase.getItemName( itemId );
		return new ScriptValue( KoLmafiaASH.ITEM_TYPE, itemId, name );
	}

	private static final ScriptValue parseLocationValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.LOCATION_INIT;
			}

			throw new AdvancedScriptException( "Location " + name + " not found in database" );
		}

		return new ScriptValue( KoLmafiaASH.LOCATION_TYPE, name, (Object) content );
	}

	private static final int classToInt( final String name )
	{
		for ( int i = 0; i < KoLmafiaASH.CLASSES.length; ++i )
		{
			if ( name.equalsIgnoreCase( KoLmafiaASH.CLASSES[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	private static final ScriptValue parseClassValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return KoLmafiaASH.CLASS_INIT;
		}

		int num = KoLmafiaASH.classToInt( name );
		if ( num < 0 )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.CLASS_INIT;
			}

			throw new AdvancedScriptException( "Unknown class " + name );
		}

		return new ScriptValue( KoLmafiaASH.CLASS_TYPE, num, KoLmafiaASH.CLASSES[ num ] );
	}

	private static final int statToInt( final String name )
	{
		for ( int i = 0; i < KoLmafiaASH.STATS.length; ++i )
		{
			if ( name.equalsIgnoreCase( KoLmafiaASH.STATS[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	private static final ScriptValue parseStatValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.STAT_INIT;
		}

		int num = KoLmafiaASH.statToInt( name );
		if ( num < 0 )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.STAT_INIT;
			}

			throw new AdvancedScriptException( "Unknown stat " + name );
		}

		return new ScriptValue( KoLmafiaASH.STAT_TYPE, num, KoLmafiaASH.STATS[ num ] );
	}

	private static final ScriptValue parseSkillValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.SKILL_INIT;
		}

		List skills = SkillDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.SKILL_INIT;
			}

			throw new AdvancedScriptException( "Skill " + name + " not found in database" );
		}

		int num = SkillDatabase.getSkillId( (String) skills.get( 0 ) );
		name = SkillDatabase.getSkillName( num );
		return new ScriptValue( KoLmafiaASH.SKILL_TYPE, num, name );
	}

	private static final ScriptValue parseEffectValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return KoLmafiaASH.EFFECT_INIT;
		}

		AdventureResult effect = KoLmafiaCLI.getFirstMatchingEffect( name );
		if ( effect == null )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.EFFECT_INIT;
			}

			throw new AdvancedScriptException( "Effect " + name + " not found in database" );
		}

		int num = EffectDatabase.getEffectId( effect.getName() );
		name = EffectDatabase.getEffectName( num );
		return new ScriptValue( KoLmafiaASH.EFFECT_TYPE, num, name );
	}

	private static final ScriptValue parseFamiliarValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.FAMILIAR_INIT;
		}

		int num = FamiliarDatabase.getFamiliarId( name );
		if ( num == -1 )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.FAMILIAR_INIT;
			}

			throw new AdvancedScriptException( "Familiar " + name + " not found in database" );
		}

		name = FamiliarDatabase.getFamiliarName( num );
		return new ScriptValue( KoLmafiaASH.FAMILIAR_TYPE, num, name );
	}

	private static final ScriptValue parseSlotValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.SLOT_INIT;
		}

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.SLOT_INIT;
			}

			throw new AdvancedScriptException( "Bad slot name " + name );
		}

		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( KoLmafiaASH.SLOT_TYPE, num, name );
	}

	private static final ScriptValue parseMonsterValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.MONSTER_INIT;
		}

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.MONSTER_INIT;
			}

			throw new AdvancedScriptException( "Bad monster name " + name );
		}

		return new ScriptValue( KoLmafiaASH.MONSTER_TYPE, monster.getName(), (Object) monster );
	}

	private static final ScriptValue parseElementValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return KoLmafiaASH.ELEMENT_INIT;
		}

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
		{
			if ( KoLmafiaASH.isExecuting )
			{
				return KoLmafiaASH.ELEMENT_INIT;
			}

			throw new AdvancedScriptException( "Bad element name " + name );
		}

		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( KoLmafiaASH.ELEMENT_TYPE, num, name );
	}

	private static final ScriptValue parseValue( final ScriptType type, final String name )
	{
		return type.parseValue( name );
	}

	// For data types which map to integers, also supply:
	//
	// private static final ScriptValue makeXValue( int num )
	//     throws nothing.

	private static final ScriptValue makeItemValue( final int num )
	{
		String name = ItemDatabase.getItemName( num );

		if ( name == null )
		{
			return KoLmafiaASH.ITEM_INIT;
		}

		return new ScriptValue( KoLmafiaASH.ITEM_TYPE, num, name );
	}

	private static final ScriptValue makeItemValue( final String name )
	{
		int num = ItemDatabase.getItemId( name );

		if ( num == -1 )
		{
			return KoLmafiaASH.ITEM_INIT;
		}

		return new ScriptValue( KoLmafiaASH.ITEM_TYPE, num, name );
	}

	private static final ScriptValue makeClassValue( final String name )
	{
		return new ScriptValue( KoLmafiaASH.CLASS_TYPE, KoLmafiaASH.classToInt( name ), name );
	}

	private static final ScriptValue makeSkillValue( final int num )
	{
		String name = SkillDatabase.getSkillName( num );
		if ( name == null )
		{
			return KoLmafiaASH.SKILL_INIT;
		}

		return new ScriptValue( KoLmafiaASH.SKILL_TYPE, num, name );
	}

	private static final ScriptValue makeEffectValue( final int num )
	{
		String name = EffectDatabase.getEffectName( num );
		if ( name == null )
		{
			return KoLmafiaASH.EFFECT_INIT;
		}
		return new ScriptValue( KoLmafiaASH.EFFECT_TYPE, num, name );
	}

	private static final ScriptValue makeFamiliarValue( final int num )
	{
		String name = FamiliarDatabase.getFamiliarName( num );
		if ( name == null )
		{
			return KoLmafiaASH.FAMILIAR_INIT;
		}
		return new ScriptValue( KoLmafiaASH.FAMILIAR_TYPE, num, name );
	}

	// **************** Tracing *****************

	private static int traceIndentation = 0;

	private static final void resetTracing()
	{
		KoLmafiaASH.traceIndentation = 0;
	}

	private static final void traceIndent()
	{
		KoLmafiaASH.traceIndentation++ ;
	}

	private static final void traceUnindent()
	{
		KoLmafiaASH.traceIndentation-- ;
	}

	private static final void trace( final String string )
	{
		KoLmafiaASH.indentLine( KoLmafiaASH.traceIndentation );
		RequestLogger.updateDebugLog( string );
	}

	private static final String executionStateString( final int state )
	{
		switch ( state )
		{
		case STATE_NORMAL:
			return "NORMAL";
		case STATE_RETURN:
			return "RETURN";
		case STATE_BREAK:
			return "BREAK";
		case STATE_CONTINUE:
			return "CONTINUE";
		case STATE_EXIT:
			return "EXIT";
		}

		return String.valueOf( state );
	}

	// **************** Parsing *****************

	public boolean validate( final File scriptFile )
	{
		this.fileName = scriptFile.getPath();

		try
		{
			return this.validate( new FileInputStream( scriptFile ) );
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
			return false;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	public boolean validate( final InputStream istream )
	{
		KoLmafiaASH previousAnalysis = KoLmafiaASH.currentAnalysis;

		try
		{
			try
			{
				this.commandStream = new LineNumberReader( new InputStreamReader( istream ) );
			}
			catch ( Exception e )
			{
				return false;
			}

			KoLmafiaASH.currentAnalysis = this;
			this.currentLine = this.getNextLine();
			this.lineNumber = this.commandStream.getLineNumber();
			this.nextLine = this.getNextLine();
			this.global = this.parseScope( null, null, new ScriptVariableList(), this.getExistingFunctionScope(), false );
			this.printScope( this.global, 0 );

			if ( this.currentLine != null )
			{
				throw new AdvancedScriptException( "Script parsing error" );
			}
		}
		finally
		{
			KoLmafiaASH.currentAnalysis = previousAnalysis;
			this.commandStream = null;

			try
			{
				istream.close();
			}
			catch ( IOException e )
			{
				return false;
			}
		}

		return true;
	}

	public void execute( final String functionName, final String[] parameters )
	{
		// Before you do anything, validate the script, if one
		// is provided.  One will not be provided in the event
		// that we are using a global namespace.

		if ( this == KoLmafiaASH.NAMESPACE_INTERPRETER )
		{
			String importString = Preferences.getString( "commandLineNamespace" );
			if ( importString.equals( "" ) )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "No available namespace with function: " + functionName );
				return;
			}

			boolean shouldRefresh = !KoLmafiaASH.lastImportString.equals( importString );

			if ( !shouldRefresh )
			{
				Iterator it = this.imports.keySet().iterator();

				while ( it.hasNext() && !shouldRefresh )
				{
					File file = (File) it.next();
					shouldRefresh = ( (Long) this.imports.get( file ) ).longValue() != file.lastModified();
				}
			}

			if ( shouldRefresh )
			{
				this.imports.clear();
				KoLmafiaASH.lastImportString = "";

				this.global = new ScriptScope( new ScriptVariableList(), this.getExistingFunctionScope() );
				String[] importList = importString.split( "," );

				for ( int i = 0; i < importList.length; ++i )
				{
					this.parseFile( importList[ i ] );
				}
			}
		}

		String currentScript = this.fileName == null ? "<>" : "<" + this.fileName + ">";
		String notifyList = Preferences.getString( "previousNotifyList" );

		if ( this.notifyRecipient != null && notifyList.indexOf( currentScript ) == -1 )
		{
			Preferences.setString( "previousNotifyList", notifyList + currentScript );

			SendMailRequest notifier = new SendMailRequest( this.notifyRecipient, this );
			RequestThread.postRequest( notifier );
		}

		try
		{
			boolean wasExecuting = KoLmafiaASH.isExecuting;

			KoLmafiaASH.isExecuting = true;
			this.executeScope( this.global, functionName, parameters );
			KoLmafiaASH.isExecuting = wasExecuting;
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
			return;
		}
		catch ( RuntimeException e )
		{
			// If it's an exception resulting from
			// a premature abort, which causes void
			// values to be returned, ignore.

			StaticEntity.printStackTrace( e );
		}
	}

	private String getNextLine()
	{
		try
		{
			do
			{
				// Read a line from input, and break out of the
				// do-while loop when you've read a valid line

				this.fullLine = this.commandStream.readLine();

				// Return null at end of file
				if ( this.fullLine == null )
				{
					return null;
				}

				// Remove whitespace at front and end
				this.fullLine = this.fullLine.trim();
			}
			while ( this.fullLine.length() == 0 );

			// Found valid currentLine - return it

			return this.fullLine;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private ScriptScope parseFile( final String fileName )
	{
		File scriptFile = KoLmafiaCLI.findScriptFile( fileName );
		if ( scriptFile == null )
		{
			throw new AdvancedScriptException( fileName + " could not be found" );
		}

		if ( this.imports.containsKey( scriptFile ) )
		{
			return this.global;
		}

		AdvancedScriptException error = null;

		ScriptScope result = this.global;

		KoLmafiaASH previousAnalysis = KoLmafiaASH.currentAnalysis;
		KoLmafiaASH.currentAnalysis = new KoLmafiaASH( this, scriptFile );

		try
		{
			result =
				KoLmafiaASH.currentAnalysis.parseScope(
					this.global, null, new ScriptVariableList(), this.global.parentScope, false );
		}
		catch ( Exception e )
		{
			error = new AdvancedScriptException( e );
		}

		try
		{
			KoLmafiaASH.currentAnalysis.commandStream.close();
		}
		catch ( Exception e )
		{
			// Do nothing, because this means the stream somehow
			// got closed anyway.
		}

		if ( error == null && KoLmafiaASH.currentAnalysis.currentLine != null )
		{
			error = new AdvancedScriptException( "Script parsing error" );
		}

		KoLmafiaASH.currentAnalysis = previousAnalysis;

		if ( error != null )
		{
			throw error;
		}

		this.imports.put( scriptFile, new Long( scriptFile.lastModified() ) );
		this.mainMethod = null;

		return result;
	}

	private ScriptScope parseScope( final ScriptScope startScope, final ScriptType expectedType,
		final ScriptVariableList variables, final ScriptScope parentScope, final boolean whileLoop )
	{
		ScriptScope result;
		String importString;

		result = startScope == null ? new ScriptScope( variables, parentScope ) : startScope;
		this.parseNotify();

		while ( ( importString = this.parseImport() ) != null )
		{
			result = this.parseFile( importString );
		}

		while ( true )
		{
			if ( this.parseTypedef( result ) )
			{
				if ( !this.currentToken().equals( ";" ) )
				{
					this.parseError( ";", this.currentToken() );
				}

				this.readToken(); //read ;
				continue;
			}

			ScriptType t = this.parseType( result, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ScriptCommand c = this.parseCommand( expectedType, result, false, whileLoop );
				if ( c != null )
				{
					result.addCommand( c );

					continue;
				}

				// No type and no command -> done.
				break;
			}

			// If this is a new record definition, enter it
			if ( t.getType() == KoLmafiaASH.TYPE_RECORD && this.currentToken() != null && this.currentToken().equals(
				";" ) )
			{
				this.readToken(); // read ;
				continue;
			}

			ScriptFunction f = this.parseFunction( t, result );
			if ( f != null )
			{
				if ( f.getName().equalsIgnoreCase( "main" ) )
				{
					this.mainMethod = f;
				}

				continue;
			}

			if ( this.parseVariables( t, result ) )
			{
				if ( !this.currentToken().equals( ";" ) )
				{
					this.parseError( ";", this.currentToken() );
				}

				this.readToken(); //read ;
				continue;
			}

			//Found a type but no function or variable to tie it to
			throw new AdvancedScriptException( "Script parse error" );
		}

		return result;
	}

	private ScriptType parseRecord( final ScriptScope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "record" ) )
		{
			return null;
		}

		this.readToken(); // read record

		if ( this.currentToken() == null )
		{
			throw new AdvancedScriptException( "Record name expected" );
		}

		// Allow anonymous records
		String recordName = null;

		if ( !this.currentToken().equals( "{" ) )
		{
			// Named record
			recordName = this.currentToken();

			if ( !this.parseIdentifier( recordName ) )
			{
				throw new AdvancedScriptException( "Invalid record name '" + recordName + "'" );
			}

			if ( KoLmafiaASH.isReservedWord( recordName ) )
			{
				throw new AdvancedScriptException( "Reserved word '" + recordName + "' cannot be a record name" );
			}

			if ( parentScope.findType( recordName ) != null )
			{
				throw new AdvancedScriptException( "Record name '" + recordName + "' is already defined" );
			}

			this.readToken(); // read name
		}

		if ( this.currentToken() == null || !this.currentToken().equals( "{" ) )
		{
			this.parseError( "{", this.currentToken() );
		}

		this.readToken(); // read {

		// Loop collecting fields
		ArrayList fieldTypes = new ArrayList();
		ArrayList fieldNames = new ArrayList();

		while ( true )
		{
			// Get the field type
			ScriptType fieldType = this.parseType( parentScope, true, true );
			if ( fieldType == null )
			{
				throw new AdvancedScriptException( "Type name expected" );
			}

			// Get the field name
			String fieldName = this.currentToken();
			if ( fieldName == null )
			{
				throw new AdvancedScriptException( "Field name expected" );
			}

			if ( !this.parseIdentifier( fieldName ) )
			{
				throw new AdvancedScriptException( "Invalid field name '" + fieldName + "'" );
			}

			if ( KoLmafiaASH.isReservedWord( fieldName ) )
			{
				throw new AdvancedScriptException( "Reserved word '" + fieldName + "' cannot be used as a field name" );
			}

			if ( fieldNames.contains( fieldName ) )
			{
				throw new AdvancedScriptException( "Field name '" + fieldName + "' is already defined" );
			}

			this.readToken(); // read name

			if ( this.currentToken() == null || !this.currentToken().equals( ";" ) )
			{
				this.parseError( ";", this.currentToken() );
			}

			this.readToken(); // read ;

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.toLowerCase() );

			if ( this.currentToken() == null )
			{
				this.parseError( "}", "EOF" );
			}

			if ( this.currentToken().equals( "}" ) )
			{
				break;
			}
		}

		this.readToken(); // read }

		String[] fieldNameArray = new String[ fieldNames.size() ];
		ScriptType[] fieldTypeArray = new ScriptType[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		ScriptRecordType rec =
			new ScriptRecordType(
				recordName == null ? "(anonymous record)" : recordName, fieldNameArray, fieldTypeArray );

		if ( recordName != null )
		{
			// Enter into type table
			parentScope.addType( rec );
		}

		return rec;
	}

	private ScriptFunction parseFunction( final ScriptType functionType, final ScriptScope parentScope )
	{
		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			return null;
		}

		String functionName = this.currentToken();

		if ( KoLmafiaASH.isReservedWord( functionName ) )
		{
			throw new AdvancedScriptException( "Reserved word '" + functionName + "' cannot be used as a function name" );
		}

		this.readToken(); //read Function name
		this.readToken(); //read (

		ScriptVariableList paramList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();

		while ( !this.currentToken().equals( ")" ) )
		{
			ScriptType paramType = this.parseType( parentScope, true, false );
			if ( paramType == null )
			{
				this.parseError( ")", this.currentToken() );
			}

			ScriptVariable param = this.parseVariable( paramType, null );
			if ( param == null )
			{
				this.parseError( "identifier", this.currentToken() );
			}

			if ( !paramList.addElement( param ) )
			{
				throw new AdvancedScriptException( "Variable " + param.getName() + " is already defined" );
			}

			if ( !this.currentToken().equals( ")" ) )
			{
				if ( !this.currentToken().equals( "," ) )
				{
					this.parseError( ")", this.currentToken() );
				}

				this.readToken(); //read comma
			}

			variableReferences.addElement( new ScriptVariableReference( param ) );
		}

		this.readToken(); //read )

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion. Replace an existing
		// forward reference.

		ScriptUserDefinedFunction result =
			parentScope.replaceFunction( new ScriptUserDefinedFunction( functionName, functionType, variableReferences ) );
		if ( this.currentToken() != null && this.currentToken().equals( ";" ) )
		{
			// Yes. Return forward reference
			this.readToken(); // ;
			return result;
		}

		ScriptScope scope;
		if ( this.currentToken() != null && this.currentToken().equals( "{" ) )
		{
			// Scope is a block

			this.readToken(); // {

			scope = this.parseScope( null, functionType, paramList, parentScope, false );
			if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
			{
				this.parseError( "}", this.currentToken() );
			}

			this.readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new ScriptScope( paramList, parentScope );
			scope.addCommand( this.parseCommand( functionType, parentScope, false, false ) );
		}

		result.setScope( scope );
		if ( !result.assertReturn() && !functionType.equals( KoLmafiaASH.TYPE_VOID )
		// The following clause can't be correct. I think it
		// depends on the various conditional & loop constructs
		// returning a boolean. Or something. But without it,
		// existing scripts break. Aargh!
		&& !functionType.equals( KoLmafiaASH.TYPE_BOOLEAN ) )
		{
			throw new AdvancedScriptException( "Missing return value" );
		}

		return result;
	}

	private boolean parseVariables( final ScriptType t, final ScriptScope parentScope )
	{
		while ( true )
		{
			ScriptVariable v = this.parseVariable( t, parentScope );
			if ( v == null )
			{
				return false;
			}

			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); //read ,
				continue;
			}

			return true;
		}
	}

	private ScriptVariable parseVariable( final ScriptType t, final ScriptScope scope )
	{
		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String variableName = this.currentToken();
		if ( KoLmafiaASH.isReservedWord( variableName ) )
		{
			throw new AdvancedScriptException( "Reserved word '" + variableName + "' cannot be a variable name" );
		}

		ScriptVariable result = new ScriptVariable( variableName, t );
		if ( scope != null && !scope.addVariable( result ) )
		{
			throw new AdvancedScriptException( "Variable " + result.getName() + " is already defined" );
		}

		this.readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( this.currentToken().equals( "=" ) )
			{
				throw new AdvancedScriptException( "Cannot initialize parameter " + result.getName() );
			}
			return result;
		}

		// Otherwise, we must initialize the variable.

		ScriptVariableReference lhs = new ScriptVariableReference( result.getName(), scope );
		ScriptExpression rhs;

		if ( this.currentToken().equals( "=" ) )
		{
			this.readToken(); // Eat the equals sign
			rhs = this.parseExpression( scope );
		}
		else
		{
			rhs = null;
		}

		scope.addCommand( new ScriptAssignment( lhs, rhs ) );
		return result;
	}

	private boolean parseTypedef( final ScriptScope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "typedef" ) )
		{
			return false;
		}
		this.readToken(); // read typedef

		ScriptType t = this.parseType( parentScope, true, true );
		if ( t == null )
		{
			throw new AdvancedScriptException( "Missing data type for typedef" );
		}

		String typeName = this.currentToken();

		if ( !this.parseIdentifier( typeName ) )
		{
			throw new AdvancedScriptException( "Invalid type name '" + typeName + "'" );
		}

		if ( KoLmafiaASH.isReservedWord( typeName ) )
		{
			throw new AdvancedScriptException( "Reserved word '" + typeName + "' cannot be a type name" );
		}

		if ( parentScope.findType( typeName ) != null )
		{
			throw new AdvancedScriptException( "Type name '" + typeName + "' is already defined" );
		}

		this.readToken(); // read name

		// Add the type to the type table
		ScriptNamedType type = new ScriptNamedType( typeName, t );
		parentScope.addType( type );

		return true;
	}

	private ScriptCommand parseCommand( final ScriptType functionType, final ScriptScope scope, final boolean noElse,
		boolean whileLoop )
	{
		ScriptCommand result;

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( this.currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
			{
				throw new AdvancedScriptException( "Encountered 'break' outside of loop" );
			}

			result = new ScriptFlowControl( KoLmafiaASH.COMMAND_BREAK );
			this.readToken(); //break
		}

		else if ( this.currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
			{
				throw new AdvancedScriptException( "Encountered 'continue' outside of loop" );
			}

			result = new ScriptFlowControl( KoLmafiaASH.COMMAND_CONTINUE );
			this.readToken(); //continue
		}

		else if ( this.currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptFlowControl( KoLmafiaASH.COMMAND_EXIT );
			this.readToken(); //exit
		}

		else if ( ( result = this.parseReturn( functionType, scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseBasicScript() ) != null )
		{
			// basic_script doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseWhile( functionType, scope ) ) != null )
		{
			// while doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseForeach( functionType, scope ) ) != null )
		{
			// foreach doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseFor( functionType, scope ) ) != null )
		{
			// for doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseRepeat( functionType, scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseConditional( functionType, scope, noElse, whileLoop ) ) != null )
		{
			// loop doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseCall( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseAssignment( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseRemove( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseValue( scope ) ) != null )
		{
			;
		}
		else
		{
			return null;
		}

		if ( this.currentToken() == null || !this.currentToken().equals( ";" ) )
		{
			this.parseError( ";", this.currentToken() );
		}

		this.readToken(); // ;
		return result;
	}

	private ScriptType parseType( final ScriptScope scope, final boolean aggregates, final boolean records )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptType valType = scope.findType( this.currentToken() );
		if ( valType == null )
		{
			if ( records && this.currentToken().equalsIgnoreCase( "record" ) )
			{
				valType = this.parseRecord( scope );

				if ( valType == null )
				{
					return null;
				}

				if ( aggregates && this.currentToken().equals( "[" ) )
				{
					return this.parseAggregateType( valType, scope );
				}

				return valType;
			}

			return null;
		}

		this.readToken();

		if ( aggregates && this.currentToken().equals( "[" ) )
		{
			return this.parseAggregateType( valType, scope );
		}

		return valType;
	}

	private ScriptType parseAggregateType( final ScriptType dataType, final ScriptScope scope )
	{
		this.readToken(); // [ or ,
		if ( this.currentToken() == null )
		{
			throw new AdvancedScriptException( "Missing index token" );
		}

		if ( KoLmafiaASH.arrays && this.readIntegerToken( this.currentToken() ) )
		{
			int size = StaticEntity.parseInt( this.currentToken() );
			this.readToken(); // integer

			if ( this.currentToken() == null )
			{
				this.parseError( "]", this.currentToken() );
			}

			if ( this.currentToken().equals( "]" ) )
			{
				this.readToken(); // ]

				if ( this.currentToken().equals( "[" ) )
				{
					return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), size );
				}

				return new ScriptAggregateType( dataType, size );
			}

			if ( this.currentToken().equals( "," ) )
			{
				return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), size );
			}

			this.parseError( "]", this.currentToken() );
		}

		ScriptType indexType = scope.findType( this.currentToken() );
		if ( indexType == null )
		{
			throw new AdvancedScriptException( "Invalid type name '" + this.currentToken() + "'" );
		}

		if ( !indexType.isPrimitive() )
		{
			throw new AdvancedScriptException( "Index type '" + this.currentToken() + "' is not a primitive type" );
		}

		this.readToken(); // type name
		if ( this.currentToken() == null )
		{
			this.parseError( "]", this.currentToken() );
		}

		if ( this.currentToken().equals( "]" ) )
		{
			this.readToken(); // ]

			if ( this.currentToken().equals( "[" ) )
			{
				return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), indexType );
			}

			return new ScriptAggregateType( dataType, indexType );
		}

		if ( this.currentToken().equals( "," ) )
		{
			return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), indexType );
		}

		this.parseError( ", or ]", this.currentToken() );
		return null;
	}

	private boolean parseIdentifier( final String identifier )
	{
		if ( !Character.isLetter( identifier.charAt( 0 ) ) && identifier.charAt( 0 ) != '_' )
		{
			return false;
		}

		for ( int i = 1; i < identifier.length(); ++i )
		{
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && identifier.charAt( i ) != '_' )
			{
				return false;
			}
		}

		return true;
	}

	private ScriptReturn parseReturn( final ScriptType expectedType, final ScriptScope parentScope )
	{
		ScriptExpression expression = null;

		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "return" ) )
		{
			return null;
		}

		this.readToken(); //return

		if ( this.currentToken() != null && this.currentToken().equals( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( KoLmafiaASH.TYPE_VOID ) )
			{
				return new ScriptReturn( null, KoLmafiaASH.VOID_TYPE );
			}

			throw new AdvancedScriptException( "Return needs value" );
		}
		else
		{
			if ( ( expression = this.parseExpression( parentScope ) ) == null )
			{
				throw new AdvancedScriptException( "Expression expected" );
			}

			return new ScriptReturn( expression, expectedType );
		}
	}

	private ScriptConditional parseConditional( final ScriptType functionType, final ScriptScope parentScope,
		boolean noElse, final boolean loop )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "if" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // if
		this.readToken(); // (

		ScriptExpression expression = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( expression.getType() != KoLmafiaASH.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"if\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		ScriptIf result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			ScriptScope scope;

			if ( this.currentToken() == null || !this.currentToken().equals( "{" ) ) //Scope is a single call
			{
				ScriptCommand command = this.parseCommand( functionType, parentScope, !elseFound, loop );
				scope = new ScriptScope( command, parentScope );
			}
			else
			{
				this.readToken(); //read {
				scope = this.parseScope( null, functionType, null, parentScope, loop );

				if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
				{
					this.parseError( "}", this.currentToken() );
				}

				this.readToken(); //read }
			}

			if ( result == null )
			{
				result = new ScriptIf( scope, expression );
			}
			else if ( finalElse )
			{
				result.addElseLoop( new ScriptElse( scope, expression ) );
			}
			else
			{
				result.addElseLoop( new ScriptElseIf( scope, expression ) );
			}

			if ( !noElse && this.currentToken() != null && this.currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
				{
					throw new AdvancedScriptException( "Else without if" );
				}

				if ( this.nextToken() != null && this.nextToken().equalsIgnoreCase( "if" ) )
				{
					this.readToken(); //else
					this.readToken(); //if

					if ( this.currentToken() == null || !this.currentToken().equals( "(" ) )
					{
						this.parseError( "(", this.currentToken() );
					}

					this.readToken(); //(
					expression = this.parseExpression( parentScope );

					if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
					{
						this.parseError( ")", this.currentToken() );
					}

					this.readToken(); // )
				}
				else
				//else without condition
				{
					this.readToken(); //else
					expression = KoLmafiaASH.TRUE_VALUE;
					finalElse = true;
				}

				elseFound = true;
				continue;
			}

			elseFound = false;
		}
		while ( elseFound );

		return result;
	}

	private ScriptBasicScript parseBasicScript()
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "cli_execute" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "{" ) )
		{
			return null;
		}

		this.readToken(); // while
		this.readToken(); // {

		ByteArrayStream ostream = new ByteArrayStream();

		while ( this.currentToken() != null && !this.currentToken().equals( "}" ) )
		{
			try
			{
				ostream.write( this.currentLine.getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				StaticEntity.printStackTrace( e );
			}

			this.currentLine = "";
			this.fixLines();
		}

		if ( this.currentToken() == null )
		{
			this.parseError( "}", this.currentToken() );
		}

		this.readToken(); // }

		return new ScriptBasicScript( ostream );
	}

	private ScriptWhile parseWhile( final ScriptType functionType, final ScriptScope parentScope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "while" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // while
		this.readToken(); // (

		ScriptExpression expression = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( expression.getType() != KoLmafiaASH.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"while\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		ScriptScope scope = this.parseLoopScope( functionType, null, parentScope );

		return new ScriptWhile( scope, expression );
	}

	private ScriptRepeat parseRepeat( final ScriptType functionType, final ScriptScope parentScope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "repeat" ) )
		{
			return null;
		}

		this.readToken(); // repeat

		ScriptScope scope = this.parseLoopScope( functionType, null, parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( "until" ) )
		{
			this.parseError( "until", this.currentToken() );
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // until
		this.readToken(); // (

		ScriptExpression expression = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( expression.getType() != KoLmafiaASH.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"repeat\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		return new ScriptRepeat( scope, expression );
	}

	private ScriptForeach parseForeach( final ScriptType functionType, final ScriptScope parentScope )
	{
		// foreach key [, key ... ] in aggregate { scope }

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "foreach" ) )
		{
			return null;
		}

		this.readToken(); // foreach

		ArrayList names = new ArrayList();

		while ( true )
		{
			String name = this.currentToken();

			if ( !this.parseIdentifier( name ) )
			{
				throw new AdvancedScriptException( "Key variable name expected" );
			}

			if ( parentScope.findVariable( name ) != null )
			{
				throw new AdvancedScriptException( "Key variable '" + name + "' is already defined" );
			}

			names.add( name );
			this.readToken(); // name

			if ( this.currentToken() != null )
			{
				if ( this.currentToken().equals( "," ) )
				{
					this.readToken(); // ,
					continue;
				}

				if ( this.currentToken().equalsIgnoreCase( "in" ) )
				{
					this.readToken(); // in
					break;
				}
			}

			this.parseError( "in", this.currentToken() );
		}

		// Get an aggregate reference
		ScriptExpression aggregate = this.parseVariableReference( parentScope );

		if ( aggregate == null || !( aggregate instanceof ScriptVariableReference ) || !( aggregate.getType().getBaseType() instanceof ScriptAggregateType ) )
		{
			throw new AdvancedScriptException( "Aggregate reference expected" );
		}

		// Define key variables of appropriate type
		ScriptVariableList varList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();
		ScriptType type = aggregate.getType().getBaseType();

		for ( int i = 0; i < names.size(); ++i )
		{
			if ( !( type instanceof ScriptAggregateType ) )
			{
				throw new AdvancedScriptException( "Too many key variables specified" );
			}

			ScriptType itype = ( (ScriptAggregateType) type ).getIndexType();
			ScriptVariable keyvar = new ScriptVariable( (String) names.get( i ), itype );
			varList.addElement( keyvar );
			variableReferences.addElement( new ScriptVariableReference( keyvar ) );
			type = ( (ScriptAggregateType) type ).getDataType();
		}

		// Parse the scope with the list of keyVars
		ScriptScope scope = this.parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ScriptForeach( scope, variableReferences, (ScriptVariableReference) aggregate );
	}

	private ScriptFor parseFor( final ScriptType functionType, final ScriptScope parentScope )
	{
		// foreach key in aggregate {scope }

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "for" ) )
		{
			return null;
		}

		String name = this.nextToken();

		if ( !this.parseIdentifier( name ) )
		{
			return null;
		}

		if ( parentScope.findVariable( name ) != null )
		{
			throw new AdvancedScriptException( "Index variable '" + name + "' is already defined" );
		}

		this.readToken(); // for
		this.readToken(); // name

		if ( !this.currentToken().equalsIgnoreCase( "from" ) )
		{
			this.parseError( "from", this.currentToken() );
		}

		this.readToken(); // from

		ScriptExpression initial = this.parseExpression( parentScope );

		int direction = 0;

		if ( this.currentToken().equalsIgnoreCase( "upto" ) )
		{
			direction = 1;
		}
		else if ( this.currentToken().equalsIgnoreCase( "downto" ) )
		{
			direction = -1;
		}
		else if ( this.currentToken().equalsIgnoreCase( "to" ) )
		{
			direction = 0;
		}
		else
		{
			this.parseError( "to, upto, or downto", this.currentToken() );
		}

		this.readToken(); // upto/downto

		ScriptExpression last = this.parseExpression( parentScope );

		ScriptExpression increment = KoLmafiaASH.ONE_VALUE;
		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken(); // by
			increment = this.parseExpression( parentScope );
		}

		// Create integer index variable
		ScriptVariable indexvar = new ScriptVariable( name, KoLmafiaASH.INT_TYPE );

		// Put index variable onto a list
		ScriptVariableList varList = new ScriptVariableList();
		varList.addElement( indexvar );

		ScriptScope scope = this.parseLoopScope( functionType, varList, parentScope );

		return new ScriptFor( scope, new ScriptVariableReference( indexvar ), initial, last, increment, direction );
	}

	private ScriptScope parseLoopScope( final ScriptType functionType, final ScriptVariableList varList,
		final ScriptScope parentScope )
	{
		ScriptScope scope;

		if ( this.currentToken() != null && this.currentToken().equals( "{" ) )
		{
			// Scope is a block

			this.readToken(); // {

			scope = this.parseScope( null, functionType, varList, parentScope, true );
			if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
			{
				this.parseError( "}", this.currentToken() );
			}

			this.readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new ScriptScope( varList, parentScope );
			scope.addCommand( this.parseCommand( functionType, scope, false, true ) );
		}

		return scope;
	}

	private ScriptExpression parseCall( final ScriptScope scope )
	{
		return this.parseCall( scope, null );
	}

	private ScriptExpression parseCall( final ScriptScope scope, final ScriptExpression firstParam )
	{
		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			return null;
		}

		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String name = this.currentToken();

		this.readToken(); //name
		this.readToken(); //(

		ScriptExpressionList params = new ScriptExpressionList();
		if ( firstParam != null )
		{
			params.addElement( firstParam );
		}

		while ( this.currentToken() != null && !this.currentToken().equals( ")" ) )
		{
			ScriptExpression val = this.parseExpression( scope );
			if ( val != null )
			{
				params.addElement( val );
			}

			if ( !this.currentToken().equals( "," ) )
			{
				if ( !this.currentToken().equals( ")" ) )
				{
					this.parseError( ")", this.currentToken() );
				}
			}
			else
			{
				this.readToken();
				if ( this.currentToken().equals( ")" ) )
				{
					this.parseError( "parameter", this.currentToken() );
				}
			}
		}

		if ( !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		this.readToken(); // )

		ScriptExpression result = new ScriptCall( name, scope, params );

		ScriptVariable current;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptCommand parseAssignment( final ScriptScope scope )
	{
		if ( this.nextToken() == null )
		{
			return null;
		}

		if ( !this.nextToken().equals( "=" ) && !this.nextToken().equals( "[" ) && !this.nextToken().equals( "." ) )
		{
			return null;
		}

		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		ScriptExpression lhs = this.parseVariableReference( scope );
		if ( lhs instanceof ScriptCall )
		{
			return lhs;
		}

		if ( lhs == null || !( lhs instanceof ScriptVariableReference ) )
		{
			throw new AdvancedScriptException( "Variable reference expected" );
		}

		if ( !this.currentToken().equals( "=" ) )
		{
			return null;
		}

		this.readToken(); //=

		ScriptExpression rhs = this.parseExpression( scope );

		if ( rhs == null )
		{
			throw new AdvancedScriptException( "Internal error" );
		}

		return new ScriptAssignment( (ScriptVariableReference) lhs, rhs );
	}

	private ScriptExpression parseRemove( final ScriptScope scope )
	{
		if ( this.currentToken() == null || !this.currentToken().equals( "remove" ) )
		{
			return null;
		}

		ScriptExpression lhs = this.parseExpression( scope );

		if ( lhs == null )
		{
			throw new AdvancedScriptException( "Bad 'remove' statement" );
		}

		return lhs;
	}

	private ScriptExpression parseExpression( final ScriptScope scope )
	{
		return this.parseExpression( scope, null );
	}

	private ScriptExpression parseExpression( final ScriptScope scope, final ScriptOperator previousOper )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptExpression lhs = null;
		ScriptExpression rhs = null;
		ScriptOperator oper = null;

		if ( this.currentToken().equals( "!" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				throw new AdvancedScriptException( "Value expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
			if ( lhs.getType() != KoLmafiaASH.BOOLEAN_TYPE )
			{
				throw new AdvancedScriptException( "\"!\" operator requires a boolean value" );
			}
		}
		else if ( this.currentToken().equals( "-" ) )
		{
			// See if it's a negative numeric constant
			if ( ( lhs = this.parseValue( scope ) ) != null )
			{
				return lhs;
			}

			// Nope. Must be unary minus.
			String operator = this.currentToken();
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				throw new AdvancedScriptException( "Value expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else if ( this.currentToken().equals( "remove" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // remove

			lhs = this.parseVariableReference( scope );
			if ( lhs == null || !( lhs instanceof ScriptCompositeReference ) )
			{
				throw new AdvancedScriptException( "Aggregate reference expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else if ( ( lhs = this.parseValue( scope ) ) == null )
		{
			return null;
		}

		do
		{
			oper = this.parseOperator( this.currentToken() );

			if ( oper == null )
			{
				return lhs;
			}

			if ( previousOper != null && !oper.precedes( previousOper ) )
			{
				return lhs;
			}

			this.readToken(); //operator

			if ( ( rhs = this.parseExpression( scope, oper ) ) == null )
			{
				throw new AdvancedScriptException( "Value expected" );
			}

			if ( !KoLmafiaASH.validCoercion( lhs.getType(), rhs.getType(), oper.toString() ) )
			{
				throw new AdvancedScriptException(
					"Cannot apply operator " + oper + " to " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
			}

			lhs = new ScriptExpression( lhs, rhs, oper );
		}
		while ( true );
	}

	private ScriptExpression parseValue( final ScriptScope scope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptExpression result = null;

		// Parse parenthesized expressions
		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (

			result = this.parseExpression( scope );
			if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
			{
				this.parseError( ")", this.currentToken() );
			}

			this.readToken(); // )
		}

		// Parse constant values
		// true and false are reserved words

		else if ( this.currentToken().equalsIgnoreCase( "true" ) )
		{
			this.readToken();
			result = KoLmafiaASH.TRUE_VALUE;
		}

		else if ( this.currentToken().equalsIgnoreCase( "false" ) )
		{
			this.readToken();
			result = KoLmafiaASH.FALSE_VALUE;
		}

		// numbers
		else if ( ( result = this.parseNumber() ) != null )
		{
			;
		}
		else if ( this.currentToken().equals( "\"" ) || this.currentToken().equals( "\'" ) )
		{
			result = this.parseString();
		}
		else if ( this.currentToken().equals( "$" ) )
		{
			result = this.parseTypedConstant( scope );
		}
		else if ( ( result = this.parseCall( scope, result ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseVariableReference( scope ) ) != null )
		{
			;
		}

		ScriptVariable current;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptValue parseNumber()
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		int sign = 1;

		if ( this.currentToken().equals( "-" ) )
		{
			String next = this.nextToken();

			if ( next == null )
			{
				return null;
			}

			if ( !next.equals( "." ) && !this.readIntegerToken( next ) )
			{
				// Unary minus
				return null;
			}

			sign = -1;
			this.readToken(); // Read -
		}

		if ( this.currentToken().equals( "." ) )
		{
			this.readToken();
			String fraction = this.currentToken();

			if ( !this.readIntegerToken( fraction ) )
			{
				this.parseError( "numeric value", fraction );
			}

			this.readToken(); // integer
			return new ScriptValue( sign * StaticEntity.parseFloat( "0." + fraction ) );
		}

		String integer = this.currentToken();
		if ( !this.readIntegerToken( integer ) )
		{
			return null;
		}

		this.readToken(); // integer

		if ( this.currentToken().equals( "." ) )
		{
			String fraction = this.nextToken();
			if ( !this.readIntegerToken( fraction ) )
			{
				return new ScriptValue( sign * StaticEntity.parseInt( integer ) );
			}

			this.readToken(); // .
			this.readToken(); // fraction

			return new ScriptValue( sign * StaticEntity.parseFloat( integer + "." + fraction ) );
		}

		return new ScriptValue( sign * StaticEntity.parseInt( integer ) );
	}

	private boolean readIntegerToken( final String token )
	{
		if ( token == null )
		{
			return false;
		}

		for ( int i = 0; i < token.length(); ++i )
		{
			if ( !Character.isDigit( token.charAt( i ) ) )
			{
				return false;
			}
		}

		return true;
	}

	private ScriptValue parseString()
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		StringBuffer resultString = new StringBuffer();
		char startCharacter = this.currentLine.charAt( 0 );

		for ( int i = 1;; ++i )
		{
			if ( i == this.currentLine.length() )
			{
				throw new AdvancedScriptException( "No closing \" found" );
			}

			if ( this.currentLine.charAt( i ) == '\\' )
			{
				char ch = this.currentLine.charAt( ++i );

				switch ( ch )
				{
				case 'n':
					resultString.append( '\n' );
					break;

				case 'r':
					resultString.append( '\r' );
					break;

				case 't':
					resultString.append( '\t' );
					break;

				case '\\':
				case '\'':
				case '\"':
					resultString.append( ch );
					break;

				case 'x':
					BigInteger hex08 = new BigInteger( this.currentLine.substring( i + 1, i + 3 ), 16 );
					resultString.append( (char) hex08.intValue() );
					i += 2;
					break;

				case 'u':
					BigInteger hex16 = new BigInteger( this.currentLine.substring( i + 1, i + 5 ), 16 );
					resultString.append( (char) hex16.intValue() );
					i += 4;
					break;

				default:
					if ( Character.isDigit( ch ) )
					{
						BigInteger octal = new BigInteger( this.currentLine.substring( i, i + 3 ), 8 );
						resultString.append( (char) octal.intValue() );
						i += 2;
					}
				}
			}
			else if ( this.currentLine.charAt( i ) == startCharacter )
			{
				this.currentLine = this.currentLine.substring( i + 1 ); //+ 1 to get rid of '"' token
				return new ScriptValue( resultString.toString() );
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private ScriptValue parseTypedConstant( final ScriptScope scope )
	{
		this.readToken(); // read $

		String name = this.currentToken();
		ScriptType type = this.parseType( scope, false, false );
		if ( type == null || !type.isPrimitive() )
		{
			throw new AdvancedScriptException( "Unknown type " + name );
		}

		if ( !this.currentToken().equals( "[" ) )
		{
			this.parseError( "[", this.currentToken() );
		}

		StringBuffer resultString = new StringBuffer();

		for ( int i = 1;; ++i )
		{
			if ( i == this.currentLine.length() )
			{
				throw new AdvancedScriptException( "No closing ] found" );
			}
			else if ( this.currentLine.charAt( i ) == '\\' )
			{
				resultString.append( this.currentLine.charAt( ++i ) );
			}
			else if ( this.currentLine.charAt( i ) == ']' )
			{
				this.currentLine = this.currentLine.substring( i + 1 ); //+1 to get rid of ']' token
				return KoLmafiaASH.parseValue( type, resultString.toString().trim() );
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private ScriptOperator parseOperator( final String oper )
	{
		if ( oper == null || !this.isOperator( oper ) )
		{
			return null;
		}

		return new ScriptOperator( oper );
	}

	private boolean isOperator( final String oper )
	{
		return oper.equals( "!" ) || oper.equals( "*" ) || oper.equals( "^" ) || oper.equals( "/" ) || oper.equals( "%" ) || oper.equals( "+" ) || oper.equals( "-" ) || oper.equals( "<" ) || oper.equals( ">" ) || oper.equals( "<=" ) || oper.equals( ">=" ) || oper.equals( "=" ) || oper.equals( "==" ) || oper.equals( "!=" ) || oper.equals( "||" ) || oper.equals( "&&" ) || oper.equals( "contains" ) || oper.equals( "remove" );
	}

	private ScriptExpression parseVariableReference( final ScriptScope scope )
	{
		if ( this.currentToken() == null || !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String name = this.currentToken();
		ScriptVariable var = scope.findVariable( name, true );

		if ( var == null )
		{
			throw new AdvancedScriptException( "Unknown variable '" + name + "'" );
		}

		this.readToken(); // read name

		if ( this.currentToken() == null || !this.currentToken().equals( "[" ) && !this.currentToken().equals( "." ) )
		{
			return new ScriptVariableReference( var );
		}

		return this.parseVariableReference( scope, var );
	}

	private ScriptExpression parseVariableReference( final ScriptScope scope, final ScriptVariable var )
	{
		ScriptType type = var.getType();
		ScriptExpressionList indices = new ScriptExpressionList();

		boolean parseAggregate = this.currentToken().equals( "[" );

		while ( this.currentToken() != null && ( this.currentToken().equals( "[" ) || this.currentToken().equals( "." ) || parseAggregate && this.currentToken().equals(
			"," ) ) )
		{
			ScriptExpression index;

			type = type.getBaseType();

			if ( this.currentToken().equals( "[" ) || this.currentToken().equals( "," ) )
			{
				this.readToken(); // read [ or . or ,
				parseAggregate = true;

				if ( !( type instanceof ScriptAggregateType ) )
				{
					if ( indices.isEmpty() )
					{
						throw new AdvancedScriptException( "Variable '" + var.getName() + "' cannot be indexed" );
					}
					else
					{
						throw new AdvancedScriptException( "Too many keys for '" + var.getName() + "'" );
					}
				}

				ScriptAggregateType atype = (ScriptAggregateType) type;
				index = this.parseExpression( scope );
				if ( index == null )
				{
					throw new AdvancedScriptException( "Index for '" + var.getName() + "' expected" );
				}

				if ( !index.getType().equals( atype.getIndexType() ) )
				{
					throw new AdvancedScriptException(
						"Index for '" + var.getName() + "' has wrong data type " + "(expected " + atype.getIndexType() + ", got " + index.getType() + ")" );
				}

				type = atype.getDataType();
			}
			else
			{
				this.readToken(); // read [ or . or ,

				// Maybe it's a function call with an implied "this" parameter.

				if ( this.nextToken().equals( "(" ) )
				{
					return this.parseCall(
						scope, indices.isEmpty() ? new ScriptVariableReference( var ) : new ScriptCompositeReference(
							var, indices ) );
				}

				if ( !( type instanceof ScriptRecordType ) )
				{
					throw new AdvancedScriptException( "Record expected" );
				}

				ScriptRecordType rtype = (ScriptRecordType) type;

				String field = this.currentToken();
				if ( field == null || !this.parseIdentifier( field ) )
				{
					throw new AdvancedScriptException( "Field name expected" );
				}

				index = rtype.getFieldIndex( field );
				if ( index == null )
				{
					throw new AdvancedScriptException( "Invalid field name '" + field + "'" );
				}
				this.readToken(); // read name
				type = rtype.getDataType( index );
			}

			indices.addElement( index );

			if ( parseAggregate && this.currentToken() != null )
			{
				if ( this.currentToken().equals( "]" ) )
				{
					this.readToken(); // read ]
					parseAggregate = false;
				}
			}
		}

		if ( parseAggregate )
		{
			this.parseError( this.currentToken(), "]" );
		}

		return new ScriptCompositeReference( var, indices );
	}

	private String parseDirective( final String directive )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( directive ) )
		{
			return null;
		}

		this.readToken(); //directive

		if ( this.currentToken() == null )
		{
			this.parseError( "<", this.currentToken() );
		}

		int startIndex = this.currentLine.indexOf( "<" );
		int endIndex = this.currentLine.indexOf( ">" );

		if ( startIndex != -1 && endIndex == -1 )
		{
			throw new AdvancedScriptException( "No closing > found" );
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\"" );
			endIndex = this.currentLine.indexOf( "\"", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw new AdvancedScriptException( "No closing \" found" );
			}
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\'" );
			endIndex = this.currentLine.indexOf( "\'", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw new AdvancedScriptException( "No closing \' found" );
			}
		}

		if ( endIndex == -1 )
		{
			endIndex = this.currentLine.indexOf( ";" );
			if ( endIndex == -1 )
			{
				endIndex = this.currentLine.length();
			}
		}

		String resultString = this.currentLine.substring( startIndex + 1, endIndex );
		this.currentLine = this.currentLine.substring( endIndex );

		if ( this.currentToken().equals( ">" ) || this.currentToken().equals( "\"" ) || this.currentToken().equals(
			"\'" ) )
		{
			this.readToken(); //get rid of '>' or '"' token
		}

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); //read ;
		}

		return resultString;
	}

	private void parseNotify()
	{
		String resultString = this.parseDirective( "notify" );
		if ( this.notifyRecipient == null )
		{
			this.notifyRecipient = resultString;
		}
	}

	private String parseImport()
	{
		return this.parseDirective( "import" );
	}

	private static final boolean validCoercion( ScriptType lhs, ScriptType rhs, final String oper )
	{
		// Resolve aliases

		lhs = lhs.getBaseType();
		rhs = rhs.getBaseType();

		// "oper" is either a standard operator or is a special name:
		//
		// "parameter" - value used as a function parameter
		//	lhs = parameter type, rhs = expression type
		//
		// "return" - value returned as function value
		//	lhs = function return type, rhs = expression type
		//
		// "assign" - value
		//	lhs = variable type, rhs = expression type

		// The "contains" operator requires an aggregate on the left
		// and the correct index type on the right.

		if ( oper.equals( "contains" ) )
		{
			return lhs.getType() == KoLmafiaASH.TYPE_AGGREGATE && ( (ScriptAggregateType) lhs ).getIndexType().equals(
				rhs );
		}

		// If the types are equal, no coercion is necessary
		if ( lhs.equals( rhs ) )
		{
			return true;
		}

		if ( lhs.equals( KoLmafiaASH.TYPE_ANY ) && rhs.getType() != KoLmafiaASH.TYPE_AGGREGATE )
		{
			return true;
		}

		// Anything coerces to a string
		if ( lhs.equals( KoLmafiaASH.TYPE_STRING ) )
		{
			return true;
		}

		// Anything coerces to a string for concatenation
		if ( oper.equals( "+" ) && rhs.equals( KoLmafiaASH.TYPE_STRING ) )
		{
			return true;
		}

		// Int coerces to float
		if ( lhs.equals( KoLmafiaASH.TYPE_INT ) && rhs.equals( KoLmafiaASH.TYPE_FLOAT ) )
		{
			return true;
		}

		if ( lhs.equals( KoLmafiaASH.TYPE_FLOAT ) && rhs.equals( KoLmafiaASH.TYPE_INT ) )
		{
			return true;
		}

		return false;
	}

	private String currentToken()
	{
		this.fixLines();
		if ( this.currentLine == null )
		{
			return null;
		}

		while ( this.currentLine.startsWith( "#" ) || this.currentLine.startsWith( "//" ) )
		{
			this.currentLine = "";

			this.fixLines();
			if ( this.currentLine == null )
			{
				return null;
			}
		}

		if ( !this.currentLine.trim().equals( "/*" ) )
		{
			return this.currentLine.substring( 0, this.tokenLength( this.currentLine ) );
		}

		while ( this.currentLine != null && !this.currentLine.trim().equals( "*/" ) )
		{
			this.currentLine = "";
			this.fixLines();
		}

		if ( this.currentLine == null )
		{
			return null;
		}

		this.currentLine = "";
		return this.currentToken();
	}

	private String nextToken()
	{
		this.fixLines();

		if ( this.currentLine == null )
		{
			return null;
		}

		if ( this.tokenLength( this.currentLine ) >= this.currentLine.length() )
		{
			if ( this.nextLine == null )
			{
				return null;
			}

			return this.nextLine.substring( 0, this.tokenLength( this.nextLine ) ).trim();
		}

		String result = this.currentLine.substring( this.tokenLength( this.currentLine ) ).trim();

		if ( result.equals( "" ) )
		{
			if ( this.nextLine == null )
			{
				return null;
			}

			return this.nextLine.substring( 0, this.tokenLength( this.nextLine ) );
		}

		return result.substring( 0, this.tokenLength( result ) );
	}

	private void readToken()
	{
		this.fixLines();

		if ( this.currentLine == null )
		{
			return;
		}

		this.currentLine = this.currentLine.substring( this.tokenLength( this.currentLine ) );
	}

	private int tokenLength( final String s )
	{
		int result;
		if ( s == null )
		{
			return 0;
		}

		for ( result = 0; result < s.length(); result++ )
		{
			if ( result + 1 < s.length() && this.tokenString( s.substring( result, result + 2 ) ) )
			{
				return result == 0 ? 2 : result;
			}

			if ( result < s.length() && this.tokenString( s.substring( result, result + 1 ) ) )
			{
				return result == 0 ? 1 : result;
			}
		}

		return result; //== s.length()
	}

	private void fixLines()
	{
		if ( this.currentLine == null )
		{
			return;
		}

		while ( this.currentLine.equals( "" ) )
		{
			this.currentLine = this.nextLine;
			this.lineNumber = this.commandStream.getLineNumber();
			this.nextLine = this.getNextLine();

			if ( this.currentLine == null )
			{
				return;
			}
		}

		this.currentLine = this.currentLine.trim();

		if ( this.nextLine == null )
		{
			return;
		}

		while ( this.nextLine.equals( "" ) )
		{
			this.nextLine = this.getNextLine();
			if ( this.nextLine == null )
			{
				return;
			}
		}

		this.nextLine = this.nextLine.trim();
	}

	private boolean tokenString( final String s )
	{
		if ( s.length() == 1 )
		{
			for ( int i = 0; i < KoLmafiaASH.tokenList.length; ++i )
			{
				if ( s.charAt( 0 ) == KoLmafiaASH.tokenList[ i ] )
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			for ( int i = 0; i < KoLmafiaASH.multiCharTokenList.length; ++i )
			{
				if ( s.equalsIgnoreCase( KoLmafiaASH.multiCharTokenList[ i ] ) )
				{
					return true;
				}
			}

			return false;
		}
	}

	// **************** Debug printing *****************

	private void printScope( final ScriptScope scope, final int indent )
	{
		if ( scope == null )
		{
			return;
		}

		Iterator it;

		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<SCOPE>" );

		KoLmafiaASH.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<TYPES>" );

		it = scope.getTypes();
		ScriptType currentType;

		while ( it.hasNext() )
		{
			currentType = (ScriptType) it.next();
			this.printType( currentType, indent + 2 );
		}

		KoLmafiaASH.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<VARIABLES>" );

		it = scope.getVariables();
		ScriptVariable currentVar;

		while ( it.hasNext() )
		{
			currentVar = (ScriptVariable) it.next();
			this.printVariable( currentVar, indent + 2 );
		}

		KoLmafiaASH.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<FUNCTIONS>" );

		it = scope.getFunctions();
		ScriptFunction currentFunc;

		while ( it.hasNext() )
		{
			currentFunc = (ScriptFunction) it.next();
			this.printFunction( currentFunc, indent + 2 );
		}

		KoLmafiaASH.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<COMMANDS>" );

		it = scope.getCommands();
		ScriptCommand currentCommand;
		while ( it.hasNext() )
		{
			currentCommand = (ScriptCommand) it.next();
			this.printCommand( currentCommand, indent + 2 );
		}

		if ( indent == 0 && this.mainMethod != null )
		{
			KoLmafiaASH.indentLine( indent + 1 );
			RequestLogger.updateDebugLog( "<MAIN>" );
			this.printFunction( this.mainMethod, indent + 2 );
		}
	}

	public void showUserFunctions( final String filter )
	{
		this.showFunctions( this.global.getFunctions(), filter.toLowerCase() );
	}

	public void showExistingFunctions( final String filter )
	{
		this.showFunctions( KoLmafiaASH.existingFunctions.iterator(), filter.toLowerCase() );
	}

	private void showFunctions( final Iterator it, final String filter )
	{
		ScriptFunction func;

		if ( !it.hasNext() )
		{
			RequestLogger.printLine( "No functions in your current namespace." );
			return;
		}

		boolean hasDescription = false;

		while ( it.hasNext() )
		{
			func = (ScriptFunction) it.next();
			hasDescription =
				func instanceof ScriptExistingFunction && ( (ScriptExistingFunction) func ).getDescription() != null;

			boolean matches = filter.equals( "" );
			matches |= func.getName().toLowerCase().indexOf( filter ) != -1;

			Iterator it2 = func.getReferences();
			matches |=
				it2.hasNext() && ( (ScriptVariableReference) it2.next() ).getType().toString().indexOf( filter ) != -1;

			if ( !matches )
			{
				continue;
			}

			StringBuffer description = new StringBuffer();

			if ( hasDescription )
			{
				description.append( "<b>" );
			}

			description.append( func.getType() );
			description.append( " " );
			description.append( func.getName() );
			description.append( "( " );

			it2 = func.getReferences();
			ScriptVariableReference var;

			while ( it2.hasNext() )
			{
				var = (ScriptVariableReference) it2.next();
				description.append( var.getType() );

				if ( var.getName() != null )
				{
					description.append( " " );
					description.append( var.getName() );
				}

				if ( it2.hasNext() )
				{
					description.append( ", " );
				}
			}

			description.append( " )" );

			if ( hasDescription )
			{
				description.append( "</b><br>" );
				description.append( ( (ScriptExistingFunction) func ).getDescription() );
				description.append( "<br>" );
			}

			RequestLogger.printLine( description.toString() );

		}
	}

	private void printType( final ScriptType type, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<TYPE " + type + ">" );
	}

	private void printVariable( final ScriptVariable var, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<VAR " + var.getType() + " " + var.getName() + ">" );
	}

	private void printFunction( final ScriptFunction func, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<FUNC " + func.getType() + " " + func.getName() + ">" );

		Iterator it = func.getReferences();
		ScriptVariableReference current;

		while ( it.hasNext() )
		{
			current = (ScriptVariableReference) it.next();
			this.printVariableReference( current, indent + 1 );
		}

		if ( func instanceof ScriptUserDefinedFunction )
		{
			this.printScope( ( (ScriptUserDefinedFunction) func ).getScope(), indent + 1 );
		}
	}

	private void printCommand( final ScriptCommand command, final int indent )
	{
		if ( command instanceof ScriptReturn )
		{
			this.printReturn( (ScriptReturn) command, indent );
		}
		else if ( command instanceof ScriptConditional )
		{
			this.printConditional( (ScriptConditional) command, indent );
		}
		else if ( command instanceof ScriptWhile )
		{
			this.printWhile( (ScriptWhile) command, indent );
		}
		else if ( command instanceof ScriptRepeat )
		{
			this.printRepeat( (ScriptRepeat) command, indent );
		}
		else if ( command instanceof ScriptForeach )
		{
			this.printForeach( (ScriptForeach) command, indent );
		}
		else if ( command instanceof ScriptFor )
		{
			this.printFor( (ScriptFor) command, indent );
		}
		else if ( command instanceof ScriptCall )
		{
			this.printCall( (ScriptCall) command, indent );
		}
		else if ( command instanceof ScriptAssignment )
		{
			this.printAssignment( (ScriptAssignment) command, indent );
		}
		else
		{
			KoLmafiaASH.indentLine( indent );
			RequestLogger.updateDebugLog( "<COMMAND " + command + ">" );
		}
	}

	private void printReturn( final ScriptReturn ret, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<RETURN " + ret.getType() + ">" );
		if ( !ret.getType().equals( KoLmafiaASH.TYPE_VOID ) )
		{
			this.printExpression( ret.getExpression(), indent + 1 );
		}
	}

	private void printConditional( final ScriptConditional command, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		if ( command instanceof ScriptIf )
		{
			ScriptIf loop = (ScriptIf) command;
			RequestLogger.updateDebugLog( "<IF>" );

			this.printExpression( loop.getCondition(), indent + 1 );
			this.printScope( loop.getScope(), indent + 1 );

			Iterator it = loop.getElseLoops();
			ScriptConditional currentElse;

			while ( it.hasNext() )
			{
				currentElse = (ScriptConditional) it.next();
				this.printConditional( currentElse, indent );
			}
		}
		else if ( command instanceof ScriptElseIf )
		{
			ScriptElseIf loop = (ScriptElseIf) command;
			RequestLogger.updateDebugLog( "<ELSE IF>" );
			this.printExpression( loop.getCondition(), indent + 1 );
			this.printScope( loop.getScope(), indent + 1 );
		}
		else if ( command instanceof ScriptElse )
		{
			ScriptElse loop = (ScriptElse) command;
			RequestLogger.updateDebugLog( "<ELSE>" );
			this.printScope( loop.getScope(), indent + 1 );
		}
	}

	private void printWhile( final ScriptWhile loop, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<WHILE>" );
		this.printExpression( loop.getCondition(), indent + 1 );
		this.printScope( loop.getScope(), indent + 1 );
	}

	private void printRepeat( final ScriptRepeat loop, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<REPEAT>" );
		this.printScope( loop.getScope(), indent + 1 );
		this.printExpression( loop.getCondition(), indent + 1 );
	}

	private void printForeach( final ScriptForeach loop, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<FOREACH>" );

		Iterator it = loop.getReferences();
		ScriptVariableReference current;

		while ( it.hasNext() )
		{
			current = (ScriptVariableReference) it.next();
			this.printVariableReference( current, indent + 1 );
		}

		this.printVariableReference( loop.getAggregate(), indent + 1 );
		this.printScope( loop.getScope(), indent + 1 );
	}

	private void printFor( final ScriptFor loop, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		int direction = loop.getDirection();
		RequestLogger.updateDebugLog( "<FOR " + ( direction < 0 ? "downto" : direction > 0 ? "upto" : "to" ) + " >" );
		this.printVariableReference( loop.getVariable(), indent + 1 );
		this.printExpression( loop.getInitial(), indent + 1 );
		this.printExpression( loop.getLast(), indent + 1 );
		this.printExpression( loop.getIncrement(), indent + 1 );
		this.printScope( loop.getScope(), indent + 1 );
	}

	private void printCall( final ScriptCall call, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<CALL " + call.getTarget().getName() + ">" );

		Iterator it = call.getExpressions();
		ScriptExpression current;

		while ( it.hasNext() )
		{
			current = (ScriptExpression) it.next();
			this.printExpression( current, indent + 1 );
		}
	}

	private void printAssignment( final ScriptAssignment assignment, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		ScriptVariableReference lhs = assignment.getLeftHandSide();
		RequestLogger.updateDebugLog( "<ASSIGN " + lhs.getName() + ">" );
		this.printIndices( lhs.getIndices(), indent + 1 );
		this.printExpression( assignment.getRightHandSide(), indent + 1 );
	}

	private void printIndices( final ScriptExpressionList indices, final int indent )
	{
		if ( indices != null )
		{
			Iterator it = indices.iterator();
			ScriptExpression current;

			while ( it.hasNext() )
			{
				current = (ScriptExpression) it.next();

				KoLmafiaASH.indentLine( indent );
				RequestLogger.updateDebugLog( "<KEY>" );
				this.printExpression( current, indent + 1 );
			}
		}
	}

	private void printExpression( final ScriptExpression expression, final int indent )
	{
		if ( expression instanceof ScriptValue )
		{
			this.printValue( (ScriptValue) expression, indent );
		}
		else
		{
			this.printOperator( expression.getOperator(), indent );
			this.printExpression( expression.getLeftHandSide(), indent + 1 );
			if ( expression.getRightHandSide() != null )
			{
				this.printExpression( expression.getRightHandSide(), indent + 1 );
			}
		}
	}

	public void printValue( final ScriptValue value, final int indent )
	{
		if ( value instanceof ScriptVariableReference )
		{
			this.printVariableReference( (ScriptVariableReference) value, indent );
		}
		else if ( value instanceof ScriptCall )
		{
			this.printCall( (ScriptCall) value, indent );
		}
		else
		{
			KoLmafiaASH.indentLine( indent );
			RequestLogger.updateDebugLog( "<VALUE " + value.getType() + " [" + value + "]>" );
		}
	}

	public void printOperator( final ScriptOperator oper, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<OPER " + oper + ">" );
	}

	public void printCompositeReference( final ScriptCompositeReference varRef, final int indent )
	{
		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<AGGREF " + varRef.getName() + ">" );

		this.printIndices( varRef.getIndices(), indent + 1 );
	}

	public void printVariableReference( final ScriptVariableReference varRef, final int indent )
	{
		if ( varRef instanceof ScriptCompositeReference )
		{
			this.printCompositeReference( (ScriptCompositeReference) varRef, indent );
			return;
		}

		KoLmafiaASH.indentLine( indent );
		RequestLogger.updateDebugLog( "<VARREF> " + varRef.getName() );
	}

	private static final void indentLine( final int indent )
	{
		for ( int i = 0; i < indent; ++i )
		{
			RequestLogger.getDebugStream().print( "   " );
		}
	}

	// **************** Execution *****************

	private static void captureValue( final ScriptValue value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( KoLmafia.refusesContinue() || value == null )
		{
			// User aborted
			KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
		KoLmafia.forceContinue();
	}

	private ScriptValue executeScope( final ScriptScope topScope, final String functionName, final String[] parameters )
	{
		ScriptFunction main;
		ScriptValue result = null;

		KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
		KoLmafiaASH.resetTracing();

		main =
			functionName.equals( "main" ) ? this.mainMethod : topScope.findFunction( functionName, parameters != null );

		if ( main == null && !topScope.getCommands().hasNext() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to invoke " + functionName );
			return KoLmafiaASH.VOID_VALUE;
		}

		// First execute top-level commands;

		boolean executeTopLevel = this != KoLmafiaASH.NAMESPACE_INTERPRETER;

		if ( !executeTopLevel )
		{
			String importString = Preferences.getString( "commandLineNamespace" );
			executeTopLevel = !importString.equals( KoLmafiaASH.lastImportString );
			KoLmafiaASH.lastImportString = importString;
		}

		if ( executeTopLevel )
		{
			KoLmafiaASH.trace( "Executing top-level commands" );
			result = topScope.execute();
		}

		if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
		{
			return result;
		}

		// Now execute main function, if any
		if ( main != null )
		{
			KoLmafiaASH.trace( "Executing main function" );

			if ( !this.requestUserParams( main, parameters ) )
			{
				return null;
			}

			result = main.execute();
		}

		return result;
	}

	private boolean requestUserParams( final ScriptFunction targetFunction, final String[] parameters )
	{
		int args = parameters == null ? 0 : parameters.length;

		ScriptType lastType = null;
		ScriptVariableReference lastParam = null;

		int index = 0;

		Iterator it = targetFunction.getReferences();
		ScriptVariableReference param;

		while ( it.hasNext() )
		{
			param = (ScriptVariableReference) it.next();

			ScriptType type = param.getType();
			String name = param.getName();
			ScriptValue value = null;

			while ( value == null )
			{
				if ( type == KoLmafiaASH.VOID_TYPE )
				{
					value = KoLmafiaASH.VOID_VALUE;
					break;
				}

				String input = null;

				if ( index >= args )
				{
					input = this.promptForValue( type, name );
				}
				else
				{
					input = parameters[ index ];
				}

				// User declined to supply a parameter
				if ( input == null )
				{
					return false;
				}

				try
				{
					value = KoLmafiaASH.parseValue( type, input );
				}
				catch ( AdvancedScriptException e )
				{
					RequestLogger.printLine( e.getMessage() );

					// Punt if parameter came from the CLI
					if ( index < args )
					{
						return false;
					}
				}
			}

			param.setValue( value );

			lastType = type;
			lastParam = param;

			index++ ;
		}

		if ( index < args )
		{
			StringBuffer inputs = new StringBuffer();
			for ( int i = index - 1; i < args; ++i )
			{
				inputs.append( parameters[ i ] + " " );
			}

			ScriptValue value = KoLmafiaASH.parseValue( lastType, inputs.toString().trim() );
			lastParam.setValue( value );
		}

		return true;
	}

	private String promptForValue( final ScriptType type, final String name )
	{
		return this.promptForValue( type, "Please input a value for " + type + " " + name, name );
	}

	private String promptForValue( final ScriptType type, final String message, final String name )
	{
		switch ( type.getType() )
		{
		case TYPE_BOOLEAN:
			return (String) KoLFrame.input( message, KoLmafiaASH.BOOLEANS );

		case TYPE_LOCATION:
			return (String) ( (KoLAdventure) KoLFrame.input(
				message, AdventureDatabase.getAsLockableListModel().toArray(),
				AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) ) ) ).getAdventureName();

		case TYPE_SKILL:
			return (String) ( (UseSkillRequest) KoLFrame.input( message, SkillDatabase.getSkillsByType(
				SkillDatabase.CASTABLE ).toArray() ) ).getSkillName();

		case TYPE_FAMILIAR:
			return ( (FamiliarData) KoLFrame.input(
				message, KoLCharacter.getFamiliarList().toArray(), KoLCharacter.getFamiliar() ) ).getRace();

		case TYPE_SLOT:
			return (String) KoLFrame.input( message, EquipmentRequest.slotNames );

		case TYPE_ELEMENT:
			return (String) KoLFrame.input( message, MonsterDatabase.elementNames );

		case TYPE_CLASS:
			return (String) KoLFrame.input( message, KoLmafiaASH.CLASSES );

		case TYPE_STAT:
			return (String) KoLFrame.input( message, KoLmafiaASH.STATS );

		case TYPE_INT:
		case TYPE_FLOAT:
		case TYPE_STRING:
		case TYPE_ITEM:
		case TYPE_EFFECT:
		case TYPE_MONSTER:
			return KoLFrame.input( message );

		default:
			throw new RuntimeException( "Internal error: Illegal type for main() parameter" );
		}
	}

	public void parseError( final String expected, final String actual )
	{
		throw new AdvancedScriptException( "Expected " + expected + ", found " + actual );
	}

	public static String getCurrentLineAndFile()
	{
		if ( KoLmafiaASH.currentAnalysis == null )
		{
			return "";
		}

		return KoLmafiaASH.currentAnalysis.getLineAndFile();
	}

	private String getLineAndFile()
	{
		if ( this.fileName == null )
		{
			return "(" + Preferences.getString( "commandLineNamespace" ) + ")";
		}

		String partialName = this.fileName.substring( this.fileName.lastIndexOf( File.separator ) + 1 );
		return "(" + partialName + ", line " + this.lineNumber + ")";
	}

	public ScriptScope getExistingFunctionScope()
	{
		return new ScriptScope( KoLmafiaASH.existingFunctions, null, KoLmafiaASH.simpleTypes );
	}

	public static final ScriptFunctionList getExistingFunctions()
	{
		ScriptFunctionList result = new ScriptFunctionList();
		ScriptType[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "enable", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "disable", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "user_confirm", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "logprint", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print_html", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "abort", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "abort", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "cli_execute", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "load_html", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "write", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "writeln", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "form_field", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "visit_url", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "visit_url", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "wait", KoLmafiaASH.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new ScriptType[] { KoLmafiaASH.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_string", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_boolean", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_int", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_float", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", KoLmafiaASH.ITEM_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", KoLmafiaASH.ITEM_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_class", KoLmafiaASH.CLASS_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_stat", KoLmafiaASH.STAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", KoLmafiaASH.SKILL_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", KoLmafiaASH.SKILL_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", KoLmafiaASH.SKILL_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", KoLmafiaASH.EFFECT_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", KoLmafiaASH.EFFECT_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", KoLmafiaASH.EFFECT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_location", KoLmafiaASH.LOCATION_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", KoLmafiaASH.FAMILIAR_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", KoLmafiaASH.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_monster", KoLmafiaASH.MONSTER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "to_slot", KoLmafiaASH.SLOT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "to_url", KoLmafiaASH.STRING_TYPE, params ) );

		// Functions related to daily information which get
		// updated usually once per day.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "today_to_string", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_phase", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_light", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_today", KoLmafiaASH.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", KoLmafiaASH.STAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			KoLmafiaASH.STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			KoLmafiaASH.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "adventure", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "add_item_condition", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "buy", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "create", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "use", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "eat", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "drink", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_closet", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_shop", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_stash", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_display", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_closet", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_storage", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_display", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_stash", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "autosell", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "hermit", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "retrieve_item", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "is_npc_item", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "daily_special", KoLmafiaASH.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_stash", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "available_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "closet_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "creatable_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "get_ingredients", KoLmafiaASH.RESULT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "storage_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "display_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "shop_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "stash_amount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "pulls_remaining", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stills_available", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_mushroom_plot", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_status", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_hp", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_mp", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_name", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_id", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_hash", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_muscle_sign", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_mysticality_sign", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_moxie_sign", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_bad_moon", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_class", KoLmafiaASH.CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_level", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_hp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxhp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_mp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxmp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_primestat", KoLmafiaASH.STAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_basestat", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_buffedstat", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_meat", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_adventures", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_turncount", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_fullness", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "fullness_limit", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_inebriety", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "inebriety_limit", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_spleen_use", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "spleen_limit", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_eat", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_drink", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "turns_played", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_interact", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_hardcore", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "have_skill", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "mp_cost", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "turns_per_cast", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "have_effect", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE, KoLmafiaASH.SKILL_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "attack", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "steal", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "runaway", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_item", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_items", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "run_combat", KoLmafiaASH.BUFFER_TYPE, params ) );

		// Equipment functions.

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "can_equip", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SLOT_TYPE, KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "equipped_item", KoLmafiaASH.ITEM_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "have_equipped", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "outfit", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "have_outfit", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_familiar", KoLmafiaASH.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "have_familiar", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "use_familiar", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_equipment", KoLmafiaASH.ITEM_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_weight", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_hands", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_type", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "ranged_weapon", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "get_power", KoLmafiaASH.INT_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "council", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_mcd", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "change_mcd", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_chef", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_bartender", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// String parsing functions.

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "contains_text", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_meat", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_items", KoLmafiaASH.RESULT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "length", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_lower_case", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_upper_case", KoLmafiaASH.STRING_TYPE, params ) );

		// String buffer functions

		params = new ScriptType[] { KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "append", KoLmafiaASH.BUFFER_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "insert", KoLmafiaASH.BUFFER_TYPE, params ) );
		params =
			new ScriptType[] { KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace", KoLmafiaASH.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.INT_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "delete", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.BUFFER_TYPE };
		result.addElement( new ScriptExistingFunction( "append_tail", KoLmafiaASH.BUFFER_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "append_replacement", KoLmafiaASH.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "create_matcher", KoLmafiaASH.MATCHER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "find", KoLmafiaASH.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "start", KoLmafiaASH.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "end", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "group", KoLmafiaASH.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "group", KoLmafiaASH.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "group_count", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_first", KoLmafiaASH.STRING_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_all", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "reset", KoLmafiaASH.MATCHER_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.MATCHER_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "reset", KoLmafiaASH.MATCHER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.BUFFER_TYPE, KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_string", KoLmafiaASH.STRING_TYPE, params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_string", KoLmafiaASH.BUFFER_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			KoLmafiaASH.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			KoLmafiaASH.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "group_string", KoLmafiaASH.REGEX_GROUP_TYPE, params ) );

		// Assorted functions

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "chat_reply", KoLmafiaASH.VOID_TYPE, params ) );

		// Quest handling functions.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "entryway", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "hedgemaze", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "guardians", KoLmafiaASH.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "chamber", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "tavern", KoLmafiaASH.INT_TYPE, params ) );

		// Arithmetic utility functions.

		params = new ScriptType[] { KoLmafiaASH.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "random", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "round", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "truncate", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "floor", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "ceil", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "square_root", KoLmafiaASH.FLOAT_TYPE, params ) );

		// Settings-type functions.

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_encode", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_decode", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "get_property", KoLmafiaASH.STRING_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "set_property", KoLmafiaASH.VOID_TYPE, params ) );

		// Functions for aggregates.

		params = new ScriptType[] { KoLmafiaASH.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "count", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "clear", KoLmafiaASH.VOID_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE, KoLmafiaASH.AGGREGATE_TYPE, KoLmafiaASH.BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.AGGREGATE_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.AGGREGATE_TYPE, KoLmafiaASH.STRING_TYPE, KoLmafiaASH.BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_location", KoLmafiaASH.LOCATION_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "get_monsters", new ScriptAggregateType(
			KoLmafiaASH.MONSTER_TYPE, 0 ), params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "expected_damage", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "expected_damage", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_level_adjustment", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "weight_adjustment", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "mana_cost_modifier", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "raw_damage_absorption", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_absorption_percent", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_reduction", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ELEMENT_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "elemental_resistance", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "combat_rate_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "initiative_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "experience_bonus", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "meat_drop_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drop_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "buffed_hit_stat", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_hit_stat", KoLmafiaASH.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_element", KoLmafiaASH.ELEMENT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_element", KoLmafiaASH.ELEMENT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_attack", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_attack", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_defense", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_defense", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_hp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_hp", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drops", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "item_drops", KoLmafiaASH.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_miss", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_dodge", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		// Modifier introspection

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.EFFECT_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.SKILL_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", KoLmafiaASH.FLOAT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "boolean_modifier", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "boolean_modifier", KoLmafiaASH.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "effect_modifier", KoLmafiaASH.EFFECT_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.ITEM_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "class_modifier", KoLmafiaASH.CLASS_TYPE, params ) );

		params = new ScriptType[] { KoLmafiaASH.EFFECT_TYPE, KoLmafiaASH.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "stat_modifier", KoLmafiaASH.STAT_TYPE, params ) );

		return result;
	}

	public static final ScriptTypeList getSimpleTypes()
	{
		ScriptTypeList result = new ScriptTypeList();
		result.addElement( KoLmafiaASH.VOID_TYPE );
		result.addElement( KoLmafiaASH.BOOLEAN_TYPE );
		result.addElement( KoLmafiaASH.INT_TYPE );
		result.addElement( KoLmafiaASH.FLOAT_TYPE );
		result.addElement( KoLmafiaASH.STRING_TYPE );
		result.addElement( KoLmafiaASH.BUFFER_TYPE );
		result.addElement( KoLmafiaASH.MATCHER_TYPE );

		result.addElement( KoLmafiaASH.ITEM_TYPE );
		result.addElement( KoLmafiaASH.LOCATION_TYPE );
		result.addElement( KoLmafiaASH.CLASS_TYPE );
		result.addElement( KoLmafiaASH.STAT_TYPE );
		result.addElement( KoLmafiaASH.SKILL_TYPE );
		result.addElement( KoLmafiaASH.EFFECT_TYPE );
		result.addElement( KoLmafiaASH.FAMILIAR_TYPE );
		result.addElement( KoLmafiaASH.SLOT_TYPE );
		result.addElement( KoLmafiaASH.MONSTER_TYPE );
		result.addElement( KoLmafiaASH.ELEMENT_TYPE );
		return result;
	}

	public static final ScriptSymbolTable getReservedWords()
	{
		ScriptSymbolTable result = new ScriptSymbolTable();

		// Constants
		result.addElement( new ScriptSymbol( "true" ) );
		result.addElement( new ScriptSymbol( "false" ) );

		// Operators
		result.addElement( new ScriptSymbol( "contains" ) );
		result.addElement( new ScriptSymbol( "remove" ) );

		// Control flow
		result.addElement( new ScriptSymbol( "if" ) );
		result.addElement( new ScriptSymbol( "else" ) );
		result.addElement( new ScriptSymbol( "foreach" ) );
		result.addElement( new ScriptSymbol( "in" ) );
		result.addElement( new ScriptSymbol( "for" ) );
		result.addElement( new ScriptSymbol( "from" ) );
		result.addElement( new ScriptSymbol( "upto" ) );
		result.addElement( new ScriptSymbol( "downto" ) );
		result.addElement( new ScriptSymbol( "by" ) );
		result.addElement( new ScriptSymbol( "while" ) );
		result.addElement( new ScriptSymbol( "repeat" ) );
		result.addElement( new ScriptSymbol( "until" ) );
		result.addElement( new ScriptSymbol( "break" ) );
		result.addElement( new ScriptSymbol( "continue" ) );
		result.addElement( new ScriptSymbol( "return" ) );
		result.addElement( new ScriptSymbol( "exit" ) );

		// Data types
		result.addElement( new ScriptSymbol( "void" ) );
		result.addElement( new ScriptSymbol( "boolean" ) );
		result.addElement( new ScriptSymbol( "int" ) );
		result.addElement( new ScriptSymbol( "float" ) );
		result.addElement( new ScriptSymbol( "string" ) );
		result.addElement( new ScriptSymbol( "buffer" ) );
		result.addElement( new ScriptSymbol( "matcher" ) );

		result.addElement( new ScriptSymbol( "item" ) );
		result.addElement( new ScriptSymbol( "location" ) );
		result.addElement( new ScriptSymbol( "class" ) );
		result.addElement( new ScriptSymbol( "stat" ) );
		result.addElement( new ScriptSymbol( "skill" ) );
		result.addElement( new ScriptSymbol( "effect" ) );
		result.addElement( new ScriptSymbol( "familiar" ) );
		result.addElement( new ScriptSymbol( "slot" ) );
		result.addElement( new ScriptSymbol( "monster" ) );
		result.addElement( new ScriptSymbol( "element" ) );

		result.addElement( new ScriptSymbol( "record" ) );
		result.addElement( new ScriptSymbol( "typedef" ) );

		return result;
	}

	public static final boolean isReservedWord( final String name )
	{
		return KoLmafiaASH.reservedWords.findSymbol( name ) != null;
	}

	private static class ScriptScope
	{
		ScriptFunctionList functions;
		ScriptVariableList variables;
		ScriptTypeList types;
		ScriptCommandList commands;
		ScriptScope parentScope;

		public ScriptScope( final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( final ScriptCommand command, final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.commands.addElement( command );
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptVariableList variables, final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			if ( variables == null )
			{
				variables = new ScriptVariableList();
			}
			this.variables = variables;
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptFunctionList functions, ScriptVariableList variables, ScriptTypeList types )
		{
			if ( functions == null )
			{
				functions = new ScriptFunctionList();
			}
			this.functions = functions;
			if ( variables == null )
			{
				variables = new ScriptVariableList();
			}
			this.variables = variables;
			if ( types == null )
			{
				types = new ScriptTypeList();
			}
			this.types = types;
			this.commands = new ScriptCommandList();
			this.parentScope = null;
		}

		public ScriptScope getParentScope()
		{
			return this.parentScope;
		}

		public boolean addFunction( final ScriptFunction f )
		{
			return this.functions.addElement( f );
		}

		public boolean removeFunction( final ScriptFunction f )
		{
			return this.functions.removeElement( f );
		}

		public Iterator getFunctions()
		{
			return this.functions.iterator();
		}

		public boolean addVariable( final ScriptVariable v )
		{
			return this.variables.addElement( v );
		}

		public Iterator getVariables()
		{
			return this.variables.iterator();
		}

		public ScriptVariable findVariable( final String name )
		{
			return this.findVariable( name, false );
		}

		public ScriptVariable findVariable( final String name, final boolean recurse )
		{
			ScriptVariable current = this.variables.findVariable( name );
			if ( current != null )
			{
				return current;
			}
			if ( recurse && this.parentScope != null )
			{
				return this.parentScope.findVariable( name, true );
			}
			return null;
		}

		public boolean addType( final ScriptType t )
		{
			return this.types.addElement( t );
		}

		public Iterator getTypes()
		{
			return this.types.iterator();
		}

		public ScriptType findType( final String name )
		{
			ScriptType current = this.types.findType( name );
			if ( current != null )
			{
				return current;
			}
			if ( this.parentScope != null )
			{
				return this.parentScope.findType( name );
			}
			return null;
		}

		public void addCommand( final ScriptCommand c )
		{
			this.commands.addElement( c );
		}

		public boolean assertReturn()
		{
			int size = this.commands.size();
			if ( size == 0 )
			{
				return false;
			}
			if ( this.commands.get( size - 1 ) instanceof ScriptReturn )
			{
				return true;
			}
			return false;
		}

		private boolean isMatchingFunction( final ScriptUserDefinedFunction current, final ScriptUserDefinedFunction f )
		{
			// The existing function must be a forward
			// reference.  Thus, already-defined functions
			// need to be skipped.

			boolean avoidExactMatch = current.getScope() != null;

			// The types of the new function's parameters
			// must exactly match the types of the existing
			// function's parameters

			Iterator it1 = current.getReferences();
			Iterator it2 = f.getReferences();
			ScriptVariableReference p1, p2;

			int paramCount = 1;

			while ( it1.hasNext() && it2.hasNext() )
			{
				p1 = (ScriptVariableReference) it1.next();
				p2 = (ScriptVariableReference) it2.next();

				if ( !p1.getType().equals( p2.getType() ) )
				{
					return false;
				}

				++paramCount;
			}

			// There must be the same number of parameters

			if ( it1.hasNext() )
			{
				return false;
			}

			if ( it2.hasNext() )
			{
				return false;
			}

			// Unfortunately, if it's an exact match and you're
			// avoiding exact matches, you need to throw an
			// exception.

			if ( avoidExactMatch )
			{
				throw new AdvancedScriptException( "Function '" + f.getName() + "' defined multiple times" );
			}

			return true;
		}

		public ScriptUserDefinedFunction replaceFunction( final ScriptUserDefinedFunction f )
		{
			if ( f.getName().equals( "main" ) )
			{
				return f;
			}

			ScriptFunction[] options = this.functions.findFunctions( f.getName() );

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[ i ] instanceof ScriptUserDefinedFunction )
				{
					ScriptUserDefinedFunction existing = (ScriptUserDefinedFunction) options[ i ];
					if ( !this.isMatchingFunction( existing, f ) )
					{
						continue;
					}

					// Must use new definition's variables
					existing.setVariableReferences( f.getVariableReferences() );
					return existing;
				}
			}

			this.addFunction( f );
			return f;
		}

		private ScriptFunction findFunction( final String name, boolean hasParameters )
		{
			ScriptFunction[] functions = this.functions.findFunctions( name );

			int paramCount, stringCount;
			ScriptFunction bestMatch = null;

			for ( int i = 0; i < functions.length; ++i )
			{
				paramCount = 0;
				stringCount = 0;

				Iterator refIterator = functions[ i ].getReferences();

				while ( refIterator.hasNext() )
				{
					++paramCount;
					if ( ( (ScriptVariableReference) refIterator.next() ).getType().equals( KoLmafiaASH.STRING_TYPE ) )
					{
						++stringCount;
					}
				}

				if ( !hasParameters && paramCount == 0 )
				{
					return functions[ i ];
				}
				if ( hasParameters && paramCount == 1 )
				{
					if ( stringCount == 1 )
					{
						return functions[ i ];
					}
					else if ( bestMatch != null )
					{
						return null;
					}
					else
					{
						bestMatch = functions[ i ];
					}
				}
			}

			return bestMatch;
		}

		private ScriptFunction findFunction( final ScriptFunctionList source, final String name,
			final ScriptExpressionList params, boolean isExactMatch )
		{
			String errorMessage = null;

			ScriptFunction[] functions = source.findFunctions( name );

			// First, try to find an exact match on parameter types.
			// This allows strict matches to take precedence.

			for ( int i = 0; i < functions.length; ++i )
			{
				errorMessage = null;

				if ( params == null )
				{
					return functions[ i ];
				}

				Iterator refIterator = functions[ i ].getReferences();
				Iterator valIterator = params.getExpressions();

				ScriptVariableReference currentParam;
				ScriptExpression currentValue;
				int paramIndex = 1;

				while ( errorMessage == null && refIterator.hasNext() && valIterator.hasNext() )
				{
					currentParam = (ScriptVariableReference) refIterator.next();
					currentValue = (ScriptExpression) valIterator.next();

					if ( isExactMatch )
					{
						if ( currentParam.getType() != currentValue.getType() )
						{
							errorMessage =
								"Illegal parameter #" + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType();
						}
					}
					else if ( !KoLmafiaASH.validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
					{
						errorMessage =
							"Illegal parameter #" + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType();
					}

					++paramIndex;
				}

				if ( errorMessage == null && ( refIterator.hasNext() || valIterator.hasNext() ) )
				{
					errorMessage =
						"Illegal amount of parameters for function " + name + ", got " + params.size() + ", expected " + functions[ i ].getVariableReferences().size();
				}

				if ( errorMessage == null )
				{
					return functions[ i ];
				}
			}

			if ( !isExactMatch && this.parentScope != null )
			{
				return this.parentScope.findFunction( name, params );
			}

			if ( !isExactMatch && source == KoLmafiaASH.existingFunctions && errorMessage != null )
			{
				throw new AdvancedScriptException( errorMessage );
			}

			return null;
		}

		public ScriptFunction findFunction( final String name, final ScriptExpressionList params )
		{
			ScriptFunction result = this.findFunction( this.functions, name, params, true );

			if ( result == null )
			{
				result = this.findFunction( KoLmafiaASH.existingFunctions, name, params, true );
			}
			if ( result == null )
			{
				result = this.findFunction( this.functions, name, params, false );
			}
			if ( result == null )
			{
				result = this.findFunction( KoLmafiaASH.existingFunctions, name, params, false );
			}

			// Just in case there's some people who don't want to edit
			// their scripts to use the new function format, check for
			// the old versions as well.

			if ( result == null )
			{
				if ( name.endsWith( "to_string" ) )
				{
					return this.findFunction( "to_string", params );
				}
				if ( name.endsWith( "to_boolean" ) )
				{
					return this.findFunction( "to_boolean", params );
				}
				if ( name.endsWith( "to_int" ) )
				{
					return this.findFunction( "to_int", params );
				}
				if ( name.endsWith( "to_float" ) )
				{
					return this.findFunction( "to_float", params );
				}
				if ( name.endsWith( "to_item" ) )
				{
					return this.findFunction( "to_item", params );
				}
				if ( name.endsWith( "to_class" ) )
				{
					return this.findFunction( "to_class", params );
				}
				if ( name.endsWith( "to_stat" ) )
				{
					return this.findFunction( "to_stat", params );
				}
				if ( name.endsWith( "to_skill" ) )
				{
					return this.findFunction( "to_skill", params );
				}
				if ( name.endsWith( "to_effect" ) )
				{
					return this.findFunction( "to_effect", params );
				}
				if ( name.endsWith( "to_location" ) )
				{
					return this.findFunction( "to_location", params );
				}
				if ( name.endsWith( "to_familiar" ) )
				{
					return this.findFunction( "to_familiar", params );
				}
				if ( name.endsWith( "to_monster" ) )
				{
					return this.findFunction( "to_monster", params );
				}
				if ( name.endsWith( "to_slot" ) )
				{
					return this.findFunction( "to_slot", params );
				}
				if ( name.endsWith( "to_url" ) )
				{
					return this.findFunction( "to_url", params );
				}
			}

			return result;
		}

		public Iterator getCommands()
		{
			return this.commands.iterator();
		}

		public ScriptValue execute()
		{
			// Delay one millisecond at the top of the scope to
			// allow other tasks to run and keyboard input -
			// especially the Escape key - to be accepted.
			GenericRequest.delay(1);

			ScriptValue result = KoLmafiaASH.VOID_VALUE;
			KoLmafiaASH.traceIndent();

			ScriptCommand current;
			Iterator it = this.commands.iterator();

			while ( it.hasNext() )
			{
				current = (ScriptCommand) it.next();
				result = current.execute();

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
				{
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				}

				if ( result == null )
				{
					result = KoLmafiaASH.VOID_VALUE;
				}

				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + result.toQuotedString() );
				switch ( KoLmafiaASH.currentState )
				{
				case STATE_RETURN:
				case STATE_BREAK:
				case STATE_CONTINUE:
				case STATE_EXIT:

					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			KoLmafiaASH.traceUnindent();
			return result;
		}
	}

	private static class ScriptSymbol
		implements Comparable
	{
		public String name;

		public ScriptSymbol()
		{
		}

		public ScriptSymbol( final String name )
		{
			this.name = name;
		}

		public String getName()
		{
			return this.name;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptSymbol ) )
			{
				throw new ClassCastException();
			}
			if ( this.name == null )
			{
				return 1;
			}
			return this.name.compareToIgnoreCase( ( (ScriptSymbol) o ).name );
		}
	}

	private static class ScriptSymbolTable
		extends Vector
	{
		public boolean addElement( final ScriptSymbol n )
		{
			if ( this.findSymbol( n.getName() ) != null )
			{
				return false;
			}

			super.addElement( n );
			return true;
		}

		public ScriptSymbol findSymbol( final String name )
		{
			ScriptSymbol currentSymbol = null;
			for ( int i = 0; i < this.size(); ++i )
			{
				currentSymbol = (ScriptSymbol) this.get( i );
				if ( currentSymbol.getName().equalsIgnoreCase( name ) )
				{
					return currentSymbol;
				}
			}

			return null;
		}
	}

	private static abstract class ScriptFunction
		extends ScriptSymbol
	{
		public ScriptType type;
		public ScriptVariableReferenceList variableReferences;

		public ScriptFunction( final String name, final ScriptType type,
			final ScriptVariableReferenceList variableReferences )
		{
			super( name );
			this.type = type;
			this.variableReferences = variableReferences;
		}

		public ScriptFunction( final String name, final ScriptType type )
		{
			this( name, type, new ScriptVariableReferenceList() );
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{
			return this.variableReferences;
		}

		public void setVariableReferences( final ScriptVariableReferenceList variableReferences )
		{
			this.variableReferences = variableReferences;
		}

		public Iterator getReferences()
		{
			return this.variableReferences.iterator();
		}

		public void saveBindings()
		{
		}

		public void restoreBindings()
		{
		}

		public void printDisabledMessage()
		{
			try
			{
				StringBuffer message = new StringBuffer( "Called disabled function: " );
				message.append( this.getName() );

				message.append( "(" );

				Iterator it = this.variableReferences.iterator();
				ScriptVariableReference current;

				for ( int i = 0; it.hasNext(); ++i )
				{
					current = (ScriptVariableReference) it.next();

					if ( i != 0 )
					{
						message.append( ',' );
					}

					message.append( ' ' );
					message.append( current.getValue().toStringValue().toString() );
				}

				message.append( " )" );
				RequestLogger.printLine( message.toString() );
			}
			catch ( Exception e )
			{
				// If it fails, don't print the disabled message.
				// Which means, exiting here is okay.
			}
		}

		public abstract ScriptValue execute();
	}

	private static class ScriptUserDefinedFunction
		extends ScriptFunction
	{
		private ScriptScope scope;
		private final Stack callStack;

		public ScriptUserDefinedFunction( final String name, final ScriptType type,
			final ScriptVariableReferenceList variableReferences )
		{
			super( name, type, variableReferences );

			this.scope = null;
			this.callStack = new Stack();
		}

		public void setScope( final ScriptScope s )
		{
			this.scope = s;
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public void saveBindings()
		{
			if ( this.scope == null )
			{
				return;
			}

			ArrayList values = new ArrayList();
			for ( int i = 0; i < this.scope.variables.size(); ++i )
			{
				values.add( ( (ScriptVariable) this.scope.variables.get( i ) ).getValue() );
			}

			this.callStack.push( values );
		}

		public void restoreBindings()
		{
			if ( this.scope == null )
			{
				return;
			}

			ArrayList values = (ArrayList) this.callStack.pop();
			for ( int i = 0; i < this.scope.variables.size(); ++i )
			{
				if ( !( ( (ScriptVariable) this.scope.variables.get( i ) ).getType() instanceof ScriptAggregateType ) )
				{
					( (ScriptVariable) this.scope.variables.get( i ) ).forceValue( (ScriptValue) values.get( i ) );
				}
			}
		}

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage();
				return this.getType().initialValue();
			}

			if ( this.scope == null )
			{
				throw new RuntimeException( "Calling undefined user function: " + this.getName() );
			}

			ScriptValue result = this.scope.execute();

			if ( result.getType().equals( this.type ) )
			{
				return result;
			}

			return this.getType().initialValue();
		}

		public boolean assertReturn()
		{
			return this.scope.assertReturn();
		}
	}

	private static class ScriptExistingFunction
		extends ScriptFunction
	{
		private Method method;
		private final String description;
		private final ScriptVariable[] variables;

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params )
		{
			this( name, type, params, null );
		}

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params,
			final String description )
		{
			super( name.toLowerCase(), type );
			this.description = description;

			this.variables = new ScriptVariable[ params.length ];
			Class[] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				this.variables[ i ] = new ScriptVariable( params[ i ] );
				this.variableReferences.addElement( new ScriptVariableReference( this.variables[ i ] ) );
				args[ i ] = ScriptVariable.class;
			}

			try
			{
				this.method = ScriptExistingFunction.class.getMethod( name, args );
			}
			catch ( Exception e )
			{
				// This should not happen; it denotes a coding
				// error that must be fixed before release.

				StaticEntity.printStackTrace( e, "No method found for built-in function: " + name );
			}
		}

		public String getDescription()
		{
			return this.description;
		}

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage();
				return this.getType().initialValue();
			}

			if ( this.method == null )
			{
				throw new RuntimeException( "Internal error: no method for " + this.getName() );
			}

			try
			{
				// Invoke the method

				return (ScriptValue) this.method.invoke( this, this.variables );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				throw new AdvancedScriptException( e.getCause() == null ? e : e.getCause() );
			}
		}

		private ScriptValue continueValue()
		{
			return KoLmafia.permitsContinue() && !KoLmafia.hadPendingState() ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		// Basic utility functions which print information
		// or allow for easy testing.

		public ScriptValue enable( final ScriptVariable name )
		{
			StaticEntity.enable( name.toStringValue().toString().toLowerCase() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue disable( final ScriptVariable name )
		{
			StaticEntity.disable( name.toStringValue().toString().toLowerCase() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue user_confirm( final ScriptVariable message )
		{
			return KoLFrame.confirm( message.toStringValue().toString() ) ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue logprint( final ScriptVariable string )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.getSessionStream().println( "> " + parameters );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue print( final ScriptVariable string )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.printLine( parameters );
			RequestLogger.getSessionStream().println( "> " + parameters );

			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue print( final ScriptVariable string, final ScriptVariable color )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			String colorString = color.toStringValue().toString();
			colorString = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( colorString, "\"" ), "<" );

			RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );
			RequestLogger.getSessionStream().println( " > " + parameters );

			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue print_html( final ScriptVariable string )
		{
			RequestLogger.printLine( string.toStringValue().toString() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue abort()
		{
			RequestThread.declareWorldPeace();
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue abort( final ScriptVariable string )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, string.toStringValue().toString() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue cli_execute( final ScriptVariable string )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
			return this.continueValue();
		}

		private File getFile( String filename )
		{
			if ( filename.startsWith( "http" ) )
			{
				return null;
			}

			filename = filename.substring( filename.lastIndexOf( "/" ) + 1 );

			File f = new File( KoLConstants.SCRIPT_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			f = new File( UtilityConstants.DATA_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			f = new File( UtilityConstants.ROOT_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			if ( filename.endsWith( ".dat" ) )
			{
				return this.getFile( filename.substring( 0, filename.length() - 4 ) + ".txt" );
			}

			return new File( KoLConstants.SCRIPT_LOCATION, filename );
		}

		private BufferedReader getReader( final String filename )
		{
			if ( filename.startsWith( "http" ) )
			{
				return DataUtilities.getReader( filename );
			}

			File input = this.getFile( filename );
			if ( input.exists() )
			{
				return DataUtilities.getReader( input );
			}

			BufferedReader reader = DataUtilities.getReader( "data", filename );
			return reader != null ? reader : DataUtilities.getReader( filename );
		}

		public ScriptValue load_html( final ScriptVariable string )
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", buffer );

			String location = string.toStringValue().toString();
			if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
			{
				return returnValue;
			}

			File input = this.getFile( location );
			if ( input == null || !input.exists() )
			{
				return returnValue;
			}

			KoLmafiaASH.VISITOR.loadResponseFromFile( input );
			if ( KoLmafiaASH.VISITOR.responseText != null )
			{
				buffer.append( KoLmafiaASH.VISITOR.responseText );
			}

			return returnValue;
		}

		public ScriptValue write( final ScriptVariable string )
		{
			if ( KoLmafiaASH.relayScript == null )
			{
				return KoLmafiaASH.VOID_VALUE;
			}

			KoLmafiaASH.serverReplyBuffer.append( string.toStringValue().toString() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue writeln( final ScriptVariable string )
		{
			if ( KoLmafiaASH.relayScript == null )
			{
				return KoLmafiaASH.VOID_VALUE;
			}

			this.write( string );
			KoLmafiaASH.serverReplyBuffer.append( KoLConstants.LINE_BREAK );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue form_field( final ScriptVariable key )
		{
			if ( KoLmafiaASH.relayRequest == null )
			{
				return KoLmafiaASH.STRING_INIT;
			}

			String value = KoLmafiaASH.relayRequest.getFormField( key.toStringValue().toString() );
			return value == null ? KoLmafiaASH.STRING_INIT : new ScriptValue( value );
		}

		public ScriptValue visit_url()
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", buffer );

			RequestThread.postRequest( KoLmafiaASH.relayRequest );
			if ( KoLmafiaASH.relayRequest.responseText != null )
			{
				buffer.append( KoLmafiaASH.relayRequest.responseText );
			}

			return returnValue;
		}

		public ScriptValue visit_url( final ScriptVariable string )
		{
			return this.visit_url( string.toStringValue().toString() );
		}

		private ScriptValue visit_url( final String location )
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", buffer );

			KoLmafiaASH.VISITOR.constructURLString( location );
			if ( GenericRequest.shouldIgnore( KoLmafiaASH.VISITOR ) )
			{
				return returnValue;
			}

			if ( KoLmafiaASH.relayScript == null )
			{
				if ( KoLmafiaASH.VISITOR.getPath().equals( "fight.php" ) )
				{
					if ( FightRequest.getCurrentRound() == 0 )
					{
						return returnValue;
					}
				}

				RequestThread.postRequest( KoLmafiaASH.VISITOR );
				if ( KoLmafiaASH.VISITOR.responseText != null )
				{
					buffer.append( KoLmafiaASH.VISITOR.responseText );
					StaticEntity.externalUpdate( location, KoLmafiaASH.VISITOR.responseText );
				}
			}
			else
			{
				RequestThread.postRequest( KoLmafiaASH.RELAYER.constructURLString( location ) );
				if ( KoLmafiaASH.RELAYER.responseText != null )
				{
					buffer.append( KoLmafiaASH.RELAYER.responseText );
				}
			}

			return returnValue;
		}

		public ScriptValue wait( final ScriptVariable delay )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
			return KoLmafiaASH.VOID_VALUE;
		}

		// Type conversion functions which allow conversion
		// of one data format to another.

		public ScriptValue to_string( final ScriptVariable val )
		{
			return val.toStringValue();
		}

		public ScriptValue to_boolean( final ScriptVariable val )
		{
			return val.toStringValue().toString().equals( "true" ) || val.intValue() != 0 ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue to_int( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_STRING ) ? KoLmafiaASH.parseIntValue( val.toStringValue().toString() ) : new ScriptValue(
				val.intValue() );
		}

		public ScriptValue to_float( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_STRING ) ? KoLmafiaASH.parseFloatValue( val.toStringValue().toString() ) : val.intValue() != 0 ? new ScriptValue(
				(float) val.intValue() ) : new ScriptValue( val.floatValue() );
		}

		public ScriptValue to_item( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_INT ) ? KoLmafiaASH.makeItemValue( val.intValue() ) : KoLmafiaASH.parseItemValue( val.toStringValue().toString() );
		}

		public ScriptValue to_class( final ScriptVariable val )
		{
			return KoLmafiaASH.parseClassValue( val.toStringValue().toString() );
		}

		public ScriptValue to_stat( final ScriptVariable val )
		{
			return KoLmafiaASH.parseStatValue( val.toStringValue().toString() );
		}

		public ScriptValue to_skill( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_INT ) ? KoLmafiaASH.makeSkillValue( val.intValue() ) : val.getValueType().equals(
				KoLmafiaASH.TYPE_EFFECT ) ? KoLmafiaASH.parseSkillValue( UneffectRequest.effectToSkill( val.toStringValue().toString() ) ) : KoLmafiaASH.parseSkillValue( val.toStringValue().toString() );
		}

		public ScriptValue to_effect( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_INT ) ? KoLmafiaASH.makeEffectValue( val.intValue() ) : val.getValueType().equals(
				KoLmafiaASH.TYPE_SKILL ) ? KoLmafiaASH.parseEffectValue( UneffectRequest.skillToEffect( val.toStringValue().toString() ) ) : KoLmafiaASH.parseEffectValue( val.toStringValue().toString() );
		}

		public ScriptValue to_location( final ScriptVariable val )
		{
			return KoLmafiaASH.parseLocationValue( val.toStringValue().toString() );
		}

		public ScriptValue to_familiar( final ScriptVariable val )
		{
			return val.getValueType().equals( KoLmafiaASH.TYPE_INT ) ? KoLmafiaASH.makeFamiliarValue( val.intValue() ) : KoLmafiaASH.parseFamiliarValue( val.toStringValue().toString() );
		}

		public ScriptValue to_monster( final ScriptVariable val )
		{
			return KoLmafiaASH.parseMonsterValue( val.toStringValue().toString() );
		}

		public ScriptValue to_slot( final ScriptVariable item )
		{
			switch ( ItemDatabase.getConsumptionType( item.intValue() ) )
			{
			case KoLConstants.EQUIP_HAT:
				return KoLmafiaASH.parseSlotValue( "hat" );
			case KoLConstants.EQUIP_WEAPON:
				return KoLmafiaASH.parseSlotValue( "weapon" );
			case KoLConstants.EQUIP_OFFHAND:
				return KoLmafiaASH.parseSlotValue( "off-hand" );
			case KoLConstants.EQUIP_SHIRT:
				return KoLmafiaASH.parseSlotValue( "shirt" );
			case KoLConstants.EQUIP_PANTS:
				return KoLmafiaASH.parseSlotValue( "pants" );
			case KoLConstants.EQUIP_FAMILIAR:
				return KoLmafiaASH.parseSlotValue( "familiar" );
			case KoLConstants.EQUIP_ACCESSORY:
				return KoLmafiaASH.parseSlotValue( "acc1" );
			default:
				return KoLmafiaASH.parseSlotValue( "none" );
			}
		}

		public ScriptValue to_url( final ScriptVariable val )
		{
			KoLAdventure adventure = (KoLAdventure) val.rawValue();
			return new ScriptValue( adventure.getRequest().getURLString() );
		}

		// Functions related to daily information which get
		// updated usually once per day.

		public ScriptValue today_to_string()
		{
			return KoLmafiaASH.parseStringValue( KoLConstants.DAILY_FORMAT.format( new Date() ) );
		}

		public ScriptValue moon_phase()
		{
			return new ScriptValue( HolidayDatabase.getPhaseStep() );
		}

		public ScriptValue moon_light()
		{
			return new ScriptValue( HolidayDatabase.getMoonlight() );
		}

		public ScriptValue stat_bonus_today()
		{
			return KoLmafiaCLI.testConditional( "today is muscle day" ) ? KoLmafiaASH.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "today is myst day" ) ? KoLmafiaASH.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "today is moxie day" ) ? KoLmafiaASH.parseStatValue( "moxie" ) : KoLmafiaASH.STAT_INIT;
		}

		public ScriptValue stat_bonus_tomorrow()
		{
			return KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) ? KoLmafiaASH.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "tomorrow is myst day" ) ? KoLmafiaASH.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) ? KoLmafiaASH.parseStatValue( "moxie" ) : KoLmafiaASH.STAT_INIT;
		}

		public ScriptValue session_logs( final ScriptVariable dayCount )
		{
			return this.getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue() );
		}

		public ScriptValue session_logs( final ScriptVariable player, final ScriptVariable dayCount )
		{
			return this.getSessionLogs( player.toStringValue().toString(), dayCount.intValue() );
		}

		private ScriptValue getSessionLogs( final String name, final int dayCount )
		{
			String[] files = new String[ dayCount ];

			Calendar timestamp = Calendar.getInstance();

			ScriptAggregateType type = new ScriptAggregateType( KoLmafiaASH.STRING_TYPE, files.length );
			ScriptArray value = new ScriptArray( type );

			String filename;
			BufferedReader reader;
			StringBuffer contents = new StringBuffer();

			for ( int i = 0; i < files.length; ++i )
			{
				contents.setLength( 0 );
				filename =
					StaticEntity.globalStringReplace( name, " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( timestamp.getTime() ) + ".txt";

				reader = KoLDatabase.getReader( new File( KoLConstants.SESSIONS_DIRECTORY, filename ) );
				timestamp.add( Calendar.DATE, -1 );

				if ( reader == null )
				{
					continue;
				}

				try
				{
					String line;

					while ( ( line = reader.readLine() ) != null )
					{
						contents.append( line );
						contents.append( KoLConstants.LINE_BREAK );
					}
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}

				value.aset( new ScriptValue( i ), KoLmafiaASH.parseStringValue( contents.toString() ) );
			}

			return value;
		}

		// Major functions related to adventuring and
		// item management.

		public ScriptValue adventure( final ScriptVariable count, final ScriptVariable loc )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue add_item_condition( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return KoLmafiaASH.VOID_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue buy( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			AdventureResult itemToBuy = new AdventureResult( item.intValue(), 1 );
			int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
			return initialAmount + count.intValue() == itemToBuy.getCount( KoLConstants.inventory ) ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue create( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "create " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue use( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue eat( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "eat " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue drink( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "drink " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue put_closet( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue put_shop( final ScriptVariable price, final ScriptVariable limit, final ScriptVariable item )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " @ " + price.intValue() + " limit " + limit.intValue() );
			return this.continueValue();
		}

		public ScriptValue put_stash( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue put_display( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_closet( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_storage( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_display( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_stash( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue autosell( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue hermit( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue retrieve_item( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
			return this.continueValue();
		}

		// Major functions which provide item-related
		// information.

		public ScriptValue is_npc_item( final ScriptVariable item )
		{
			return NPCStoreDatabase.contains( ItemDatabase.getItemName( item.intValue() ), false ) ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue daily_special()
		{
			AdventureResult special =
				KoLCharacter.inMoxieSign() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.inMysticalitySign() ? ChezSnooteeRequest.getDailySpecial() : null;

			return special == null ? KoLmafiaASH.ITEM_INIT : KoLmafiaASH.parseItemValue( special.getName() );
		}

		public ScriptValue refresh_stash()
		{
			RequestThread.postRequest( new ClanStashRequest() );
			return this.continueValue();
		}

		public ScriptValue available_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );

			int runningTotal = item.getCount( KoLConstants.inventory ) + item.getCount( KoLConstants.closet );

			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
			{
				if ( KoLCharacter.getEquipment( i ).equals( item ) )
				{
					++runningTotal;
				}
			}

			if ( KoLCharacter.canInteract() )
			{
				runningTotal += item.getCount( KoLConstants.storage );
			}

			return new ScriptValue( runningTotal );
		}

		public ScriptValue item_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.inventory ) );
		}

		public ScriptValue closet_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.closet ) );
		}

		public ScriptValue creatable_amount( final ScriptVariable arg )
		{
			CreateItemRequest item = CreateItemRequest.getInstance( arg.intValue() );
			return new ScriptValue( item == null ? 0 : item.getQuantityPossible() );
		}

		public ScriptValue get_ingredients( final ScriptVariable item )
		{
			AdventureResult[] data = ConcoctionDatabase.getIngredients( item.intValue() );
			ScriptMap value = new ScriptMap( KoLmafiaASH.RESULT_TYPE );

			for ( int i = 0; i < data.length; ++i )
			{
				value.aset(
					KoLmafiaASH.parseItemValue( data[ i ].getName() ),
					KoLmafiaASH.parseIntValue( String.valueOf( data[ i ].getCount() ) ) );
			}

			return value;
		}

		public ScriptValue storage_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.storage ) );
		}

		public ScriptValue display_amount( final ScriptVariable arg )
		{
			if ( KoLConstants.collection.isEmpty() )
			{
				RequestThread.postRequest( new DisplayCaseRequest() );
			}

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.collection ) );
		}

		public ScriptValue shop_amount( final ScriptVariable arg )
		{
			LockableListModel list = StoreManager.getSoldItemList();
			if ( list.isEmpty() )
			{
				RequestThread.postRequest( new ManageStoreRequest() );
			}

			SoldItem item = new SoldItem( arg.intValue(), 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
			{
				return new ScriptValue( 0 );
			}

			item = (SoldItem) list.get( index );
			return new ScriptValue( item.getQuantity() );
		}

		public ScriptValue stash_amount( final ScriptVariable arg )
		{
			List stash = ClanManager.getStash();
			if ( stash.isEmpty() )
			{
				RequestThread.postRequest( new ClanStashRequest() );
			}

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( stash ) );
		}

		public ScriptValue pulls_remaining()
		{
			return new ScriptValue( ItemManageFrame.getPullsRemaining() );
		}

		public ScriptValue stills_available()
		{
			return new ScriptValue( KoLCharacter.getStillsAvailable() );
		}

		public ScriptValue have_mushroom_plot()
		{
			return new ScriptValue( MushroomManager.ownsPlot() );
		}

		// The following functions pertain to providing updated
		// information relating to the player.

		public ScriptValue refresh_status()
		{
			RequestThread.postRequest( CharPaneRequest.getInstance() );
			return this.continueValue();
		}

		public ScriptValue restore_hp( final ScriptVariable amount )
		{
			return new ScriptValue( StaticEntity.getClient().recoverHP( amount.intValue() ) );
		}

		public ScriptValue restore_mp( final ScriptVariable amount )
		{
			int desiredMP = amount.intValue();
			while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
			{
				StaticEntity.getClient().recoverMP( desiredMP );
			}
			return this.continueValue();
		}

		public ScriptValue my_name()
		{
			return new ScriptValue( KoLCharacter.getUserName() );
		}

		public ScriptValue my_id()
		{
			return new ScriptValue( KoLCharacter.getPlayerId() );
		}

		public ScriptValue my_hash()
		{
			return new ScriptValue( GenericRequest.passwordHash );
		}

		public ScriptValue in_muscle_sign()
		{
			return new ScriptValue( KoLCharacter.inMuscleSign() );
		}

		public ScriptValue in_mysticality_sign()
		{
			return new ScriptValue( KoLCharacter.inMysticalitySign() );
		}

		public ScriptValue in_moxie_sign()
		{
			return new ScriptValue( KoLCharacter.inMoxieSign() );
		}

		public ScriptValue in_bad_moon()
		{
			return new ScriptValue( KoLCharacter.inBadMoon() );
		}

		public ScriptValue my_class()
		{
			return KoLmafiaASH.makeClassValue( KoLCharacter.getClassType() );
		}

		public ScriptValue my_level()
		{
			return new ScriptValue( KoLCharacter.getLevel() );
		}

		public ScriptValue my_hp()
		{
			return new ScriptValue( KoLCharacter.getCurrentHP() );
		}

		public ScriptValue my_maxhp()
		{
			return new ScriptValue( KoLCharacter.getMaximumHP() );
		}

		public ScriptValue my_mp()
		{
			return new ScriptValue( KoLCharacter.getCurrentMP() );
		}

		public ScriptValue my_maxmp()
		{
			return new ScriptValue( KoLCharacter.getMaximumMP() );
		}

		public ScriptValue my_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex();
			return primeIndex == 0 ? KoLmafiaASH.parseStatValue( "muscle" ) : primeIndex == 1 ? KoLmafiaASH.parseStatValue( "mysticality" ) : KoLmafiaASH.parseStatValue( "moxie" );
		}

		public ScriptValue my_basestat( final ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMuscle() );
			}
			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMysticality() );
			}
			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMoxie() );
			}

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_buffedstat( final ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
			}
			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
			}
			if ( KoLmafiaASH.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMoxie() );
			}

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_meat()
		{
			return new ScriptValue( KoLCharacter.getAvailableMeat() );
		}

		public ScriptValue my_adventures()
		{
			return new ScriptValue( KoLCharacter.getAdventuresLeft() );
		}

		public ScriptValue my_turncount()
		{
			return new ScriptValue( KoLCharacter.getCurrentRun() );
		}

		public ScriptValue my_fullness()
		{
			return new ScriptValue( KoLCharacter.getFullness() );
		}

		public ScriptValue fullness_limit()
		{
			return new ScriptValue( KoLCharacter.getFullnessLimit() );
		}

		public ScriptValue my_inebriety()
		{
			return new ScriptValue( KoLCharacter.getInebriety() );
		}

		public ScriptValue inebriety_limit()
		{
			return new ScriptValue( KoLCharacter.getInebrietyLimit() );
		}

		public ScriptValue my_spleen_use()
		{
			return new ScriptValue( KoLCharacter.getSpleenUse() );
		}

		public ScriptValue spleen_limit()
		{
			return new ScriptValue( KoLCharacter.getSpleenLimit() );
		}

		public ScriptValue can_eat()
		{
			return new ScriptValue( KoLCharacter.canEat() );
		}

		public ScriptValue can_drink()
		{
			return new ScriptValue( KoLCharacter.canDrink() );
		}

		public ScriptValue turns_played()
		{
			return new ScriptValue( KoLCharacter.getCurrentRun() );
		}

		public ScriptValue can_interact()
		{
			return new ScriptValue( KoLCharacter.canInteract() );
		}

		public ScriptValue in_hardcore()
		{
			return new ScriptValue( KoLCharacter.isHardcore() );
		}

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		public ScriptValue have_skill( final ScriptVariable arg )
		{
			return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
		}

		public ScriptValue mp_cost( final ScriptVariable skill )
		{
			return new ScriptValue( SkillDatabase.getMPConsumptionById( skill.intValue() ) );
		}

		public ScriptValue turns_per_cast( final ScriptVariable skill )
		{
			return new ScriptValue( SkillDatabase.getEffectDuration( skill.intValue() ) );
		}

		public ScriptValue have_effect( final ScriptVariable arg )
		{
			List potentialEffects = EffectDatabase.getMatchingNames( arg.toStringValue().toString() );
			AdventureResult effect =
				potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
			return new ScriptValue( effect == null ? 0 : effect.getCount( KoLConstants.activeEffects ) );
		}

		public ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
				{
					this.use_skill( skill );
				}

				return KoLmafiaASH.TRUE_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue use_skill( final ScriptVariable skill )
		{
			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				return this.visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast 1 " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate );
		}

		public ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill,
			final ScriptVariable target )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
				{
					this.use_skill( skill );
				}

				return KoLmafiaASH.TRUE_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() + " on " + target.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue attack()
		{
			return this.visit_url( "fight.php?action=attack" );
		}

		public ScriptValue steal()
		{
			if ( !FightRequest.wonInitiative() )
			{
				return this.attack();
			}

			return this.visit_url( "fight.php?action=steal" );
		}

		public ScriptValue runaway()
		{
			return this.visit_url( "fight.php?action=runaway" );
		}

		public ScriptValue throw_item( final ScriptVariable item )
		{
			return this.visit_url( "fight.php?action=useitem&whichitem=" + item.intValue() );
		}

		public ScriptValue throw_items( final ScriptVariable item1, final ScriptVariable item2 )
		{
			return this.visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue() + "&whichitem2=" + item2.intValue() );
		}

		public ScriptValue run_combat()
		{
			RequestThread.postRequest( FightRequest.INSTANCE );
			String response =
				KoLmafiaASH.relayScript == null ? FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

			return new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
		}

		// Equipment functions.

		public ScriptValue can_equip( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.canEquip( ItemDatabase.getItemName( item.intValue() ) ) );
		}

		public ScriptValue equip( final ScriptVariable item )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue equip( final ScriptVariable slot, final ScriptVariable item )
		{
			if ( item.getValue().equals( KoLmafiaASH.ITEM_INIT ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "unequip " + slot.toStringValue() );
			}
			else
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );
			}

			return this.continueValue();
		}

		public ScriptValue equipped_item( final ScriptVariable slot )
		{
			return KoLmafiaASH.makeItemValue( KoLCharacter.getEquipment( slot.intValue() ).getName() );
		}

		public ScriptValue have_equipped( final ScriptVariable item )
		{
			return KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue outfit( final ScriptVariable outfit )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue().toString() );
			return this.continueValue();
		}

		public ScriptValue have_outfit( final ScriptVariable outfit )
		{
			SpecialOutfit so = KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue().toString() );

			if ( so == null )
			{
				return KoLmafiaASH.FALSE_VALUE;
			}

			return EquipmentDatabase.hasOutfit( so.getOutfitId() ) ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue weapon_hands( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.getHands( item.intValue() ) );
		}

		public ScriptValue weapon_type( final ScriptVariable item )
		{
			String type = EquipmentDatabase.getType( item.intValue() );
			return new ScriptValue( type == null ? "unknown" : type );
		}

		public ScriptValue ranged_weapon( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.isRanged( item.intValue() ) );
		}

		public ScriptValue get_power( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.getPower( item.intValue() ) );
		}

		public ScriptValue my_familiar()
		{
			return KoLmafiaASH.makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
		}

		public ScriptValue have_familiar( final ScriptVariable familiar )
		{
			return new ScriptValue( KoLCharacter.findFamiliar( familiar.toStringValue().toString() ) != null );
		}

		public ScriptValue use_familiar( final ScriptVariable familiar )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue familiar_equipment( final ScriptVariable familiar )
		{
			return KoLmafiaASH.parseItemValue( FamiliarDatabase.getFamiliarItem( familiar.intValue() ) );
		}

		public ScriptValue familiar_weight( final ScriptVariable familiar )
		{
			FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue().toString() );
			return new ScriptValue( fam == null ? 0 : fam.getWeight() );
		}

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		public ScriptValue council()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "council" );
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue current_mcd()
		{
			return new ScriptValue( KoLCharacter.getSignedMLAdjustment() );
		}

		public ScriptValue change_mcd( final ScriptVariable level )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mind-control " + level.intValue() );
			return this.continueValue();
		}

		public ScriptValue have_chef()
		{
			return new ScriptValue( KoLCharacter.hasChef() );
		}

		public ScriptValue have_bartender()
		{
			return new ScriptValue( KoLCharacter.hasBartender() );
		}

		// String parsing functions.

		public ScriptValue contains_text( final ScriptVariable source, final ScriptVariable search )
		{
			return new ScriptValue(
				source.toStringValue().toString().indexOf( search.toStringValue().toString() ) != -1 );
		}

		public ScriptValue extract_meat( final ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				if ( result.getName().equals( AdventureResult.MEAT ) )
				{
					return new ScriptValue( result.getCount() );
				}
			}

			return new ScriptValue( 0 );
		}

		public ScriptValue extract_items( final ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );
			ScriptMap value = new ScriptMap( KoLmafiaASH.RESULT_TYPE );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				if ( result.isItem() )
				{
					value.aset(
						KoLmafiaASH.parseItemValue( result.getName() ),
						KoLmafiaASH.parseIntValue( String.valueOf( result.getCount() ) ) );
				}
			}

			return value;
		}

		public ScriptValue length( final ScriptVariable string )
		{
			return new ScriptValue( string.toStringValue().toString().length() );
		}

		public ScriptValue index_of( final ScriptVariable source, final ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.indexOf( substring ) );
		}

		public ScriptValue index_of( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.indexOf( substring, begin ) );
		}

		public ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.lastIndexOf( substring ) );
		}

		public ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.lastIndexOf( substring, begin ) );
		}

		public ScriptValue substring( final ScriptVariable source, final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.substring( begin ) );
		}

		public ScriptValue substring( final ScriptVariable source, final ScriptVariable start,
			final ScriptVariable finish )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			int end = finish.intValue();
			return new ScriptValue( string.substring( begin, end ) );
		}

		public ScriptValue to_upper_case( final ScriptVariable string )
		{
			return KoLmafiaASH.parseStringValue( string.toStringValue().toString().toUpperCase() );
		}

		public ScriptValue to_lower_case( final ScriptVariable string )
		{
			return KoLmafiaASH.parseStringValue( string.toStringValue().toString().toLowerCase() );
		}

		public ScriptValue append( final ScriptVariable buffer, final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.append( s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue insert( final ScriptVariable buffer, final ScriptVariable index, final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.insert( index.intValue(), s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue replace( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end,
			final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.replace( start.intValue(), end.intValue(), s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue delete( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.delete( start.intValue(), end.intValue() );
			return buffer.getValue();
		}

		public ScriptValue append_tail( final ScriptVariable matcher, final ScriptVariable current )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
			m.appendTail( buffer );
			return current.getValue();
		}

		public ScriptValue append_replacement( final ScriptVariable matcher, final ScriptVariable current,
			final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
			m.appendReplacement( buffer, replacement.toStringValue().toString() );
			return matcher.getValue();
		}

		public ScriptValue create_matcher( final ScriptVariable pattern, final ScriptVariable string )
		{
			return new ScriptValue( KoLmafiaASH.MATCHER_TYPE, pattern.toStringValue().toString(), Pattern.compile(
				pattern.toStringValue().toString(), Pattern.DOTALL ).matcher( string.toStringValue().toString() ) );
		}

		public ScriptValue find( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return m.find() ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue start( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.start() );
		}

		public ScriptValue end( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.end() );
		}

		public ScriptValue group( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.group() );
		}

		public ScriptValue group( final ScriptVariable matcher, final ScriptVariable group )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.group( group.intValue() ) );
		}

		public ScriptValue group_count( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.groupCount() );
		}

		public ScriptValue replace_first( final ScriptVariable matcher, final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.replaceFirst( replacement.toStringValue().toString() ) );
		}

		public ScriptValue replace_all( final ScriptVariable matcher, final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.replaceAll( replacement.toStringValue().toString() ) );
		}

		public ScriptValue reset( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			m.reset();
			return matcher.getValue();
		}

		public ScriptValue reset( final ScriptVariable matcher, final ScriptVariable input )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			m.reset( input.toStringValue().toString() );
			return matcher.getValue();
		}

		public ScriptValue replace_string( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable replace )
		{
			StringBuffer buffer;
			ScriptValue returnValue;

			if ( source.getValue().rawValue() instanceof StringBuffer )
			{
				buffer = (StringBuffer) source.getValue().rawValue();
				returnValue = source.getValue();
			}
			else
			{
				buffer = new StringBuffer( source.toStringValue().toString() );
				returnValue = new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", buffer );
			}

			StaticEntity.globalStringReplace(
				buffer, search.toStringValue().toString(), replace.toStringValue().toString() );
			return returnValue;
		}

		public ScriptValue split_string( final ScriptVariable string )
		{
			String[] pieces = string.toStringValue().toString().split( KoLConstants.LINE_BREAK );

			ScriptAggregateType type = new ScriptAggregateType( KoLmafiaASH.STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
			{
				value.aset( new ScriptValue( i ), KoLmafiaASH.parseStringValue( pieces[ i ] ) );
			}

			return value;
		}

		public ScriptValue split_string( final ScriptVariable string, final ScriptVariable regex )
		{
			String[] pieces = string.toStringValue().toString().split( regex.toStringValue().toString() );

			ScriptAggregateType type = new ScriptAggregateType( KoLmafiaASH.STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
			{
				value.aset( new ScriptValue( i ), KoLmafiaASH.parseStringValue( pieces[ i ] ) );
			}

			return value;
		}

		public ScriptValue group_string( final ScriptVariable string, final ScriptVariable regex )
		{
			Matcher userPatternMatcher =
				Pattern.compile( regex.toStringValue().toString() ).matcher( string.toStringValue().toString() );
			ScriptMap value = new ScriptMap( KoLmafiaASH.REGEX_GROUP_TYPE );

			int matchCount = 0;
			int groupCount = userPatternMatcher.groupCount();

			ScriptValue[] groupIndexes = new ScriptValue[ groupCount + 1 ];
			for ( int i = 0; i <= groupCount; ++i )
			{
				groupIndexes[ i ] = new ScriptValue( i );
			}

			ScriptValue matchIndex;
			ScriptCompositeValue slice;

			try
			{
				while ( userPatternMatcher.find() )
				{
					matchIndex = new ScriptValue( matchCount );
					slice = (ScriptCompositeValue) value.initialValue( matchIndex );

					value.aset( matchIndex, slice );
					for ( int i = 0; i <= groupCount; ++i )
					{
						slice.aset( groupIndexes[ i ], KoLmafiaASH.parseStringValue( userPatternMatcher.group( i ) ) );
					}

					++matchCount;
				}
			}
			catch ( Exception e )
			{
				// Because we're doing everything ourselves, this
				// error shouldn't get generated.  Print a stack
				// trace, just in case.

				StaticEntity.printStackTrace( e );
			}

			return value;
		}

		public ScriptValue chat_reply( final ScriptVariable string )
		{
			String recipient = ChatManager.lastBlueMessage();
			if ( !recipient.equals( "" ) )
			{
				RequestThread.postRequest( new ChatRequest( recipient, string.toStringValue().toString(), false ) );
			}

			return KoLmafiaASH.VOID_VALUE;
		}

		// Quest completion functions.

		public ScriptValue entryway()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "entryway" );
			return this.continueValue();
		}

		public ScriptValue hedgemaze()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hedgemaze" );
			return this.continueValue();
		}

		public ScriptValue guardians()
		{
			int itemId = SorceressLairManager.fightTowerGuardians( true );
			return KoLmafiaASH.makeItemValue( itemId );
		}

		public ScriptValue chamber()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "chamber" );
			return this.continueValue();
		}

		public ScriptValue tavern()
		{
			int result = StaticEntity.getClient().locateTavernFaucet();
			return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
		}

		// Arithmetic utility functions.

		public ScriptValue random( final ScriptVariable arg )
		{
			int range = arg.intValue();
			if ( range < 2 )
			{
				throw new RuntimeException( "Random range must be at least 2" );
			}
			return new ScriptValue( KoLConstants.RNG.nextInt( range ) );
		}

		public ScriptValue round( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.round( arg.floatValue() ) );
		}

		public ScriptValue truncate( final ScriptVariable arg )
		{
			return new ScriptValue( (int) arg.floatValue() );
		}

		public ScriptValue floor( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.floor( arg.floatValue() ) );
		}

		public ScriptValue ceil( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.ceil( arg.floatValue() ) );
		}

		public ScriptValue square_root( final ScriptVariable val )
		{
			return new ScriptValue( (float) Math.sqrt( val.floatValue() ) );
		}

		// Settings-type functions.

		public ScriptValue url_encode( final ScriptVariable arg )
			throws UnsupportedEncodingException
		{
			return new ScriptValue( URLEncoder.encode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue url_decode( final ScriptVariable arg )
			throws UnsupportedEncodingException
		{
			return new ScriptValue( URLDecoder.decode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue get_property( final ScriptVariable name )
		{
			String property = name.toStringValue().toString();
			return !Preferences.isUserEditable( property ) ? KoLmafiaASH.STRING_INIT : new ScriptValue(
				Preferences.getString( property ) );
		}

		public ScriptValue set_property( final ScriptVariable name, final ScriptVariable value )
		{
			// In order to avoid code duplication for combat
			// related settings, use the shell.

			KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
				"set", name.toStringValue().toString() + "=" + value.toStringValue().toString() );
			return KoLmafiaASH.VOID_VALUE;
		}

		// Functions for aggregates.

		public ScriptValue count( final ScriptVariable arg )
		{
			return new ScriptValue( arg.getValue().count() );
		}

		public ScriptValue clear( final ScriptVariable arg )
		{
			arg.getValue().clear();
			return KoLmafiaASH.VOID_VALUE;
		}

		public ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			return this.readMap( filename, map_variable, true );
		}

		public ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			boolean compact = var3.intValue() == 1;
			return this.readMap( filename, map_variable, compact );
		}

		private ScriptValue readMap( final String filename, final ScriptCompositeValue result, final boolean compact )
		{
			BufferedReader reader = this.getReader( filename );
			if ( reader == null )
			{
				return KoLmafiaASH.FALSE_VALUE;
			}

			String[] data = null;
			result.clear();

			try
			{
				while ( ( data = KoLDatabase.readData( reader ) ) != null )
				{
					if ( data.length > 1 )
					{
						result.read( data, 0, compact );
					}
				}
			}
			catch ( Exception e )
			{
				// Okay, runtime error. Indicate that there was
				// a bad currentLine in the data file and print the
				// stack trace.

				StringBuffer buffer = new StringBuffer( "Invalid line in data file:" );
				if ( data != null )
				{
					buffer.append( KoLConstants.LINE_BREAK );

					for ( int i = 0; i < data.length; ++i )
					{
						buffer.append( '\t' );
						buffer.append( data[ i ] );
					}
				}

				StaticEntity.printStackTrace( e, buffer.toString() );
				return KoLmafiaASH.FALSE_VALUE;
			}

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}

			return KoLmafiaASH.TRUE_VALUE;
		}

		public ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			return this.printMap( map_variable, filename, true );
		}

		public ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			boolean compact = var3.intValue() == 1;
			return this.printMap( map_variable, filename, compact );
		}

		private ScriptValue printMap( final ScriptCompositeValue map_variable, final String filename,
			final boolean compact )
		{
			if ( filename.startsWith( "http" ) )
			{
				return KoLmafiaASH.FALSE_VALUE;
			}

			PrintStream writer = null;
			File output = this.getFile( filename );

			if ( output == null )
			{
				return KoLmafiaASH.FALSE_VALUE;
			}

			writer = LogStream.openStream( output, true );
			map_variable.dump( writer, "", compact );
			writer.close();

			return KoLmafiaASH.TRUE_VALUE;
		}

		// Custom combat helper functions.

		public ScriptValue my_location()
		{
			String location = Preferences.getString( "lastAdventure" );
			return location.equals( "" ) ? KoLmafiaASH.parseLocationValue( "Rest" ) : KoLmafiaASH.parseLocationValue( location );
		}

		public ScriptValue get_monsters( final ScriptVariable location )
		{
			KoLAdventure adventure = (KoLAdventure) location.rawValue();
			AreaCombatData data = adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();

			ScriptAggregateType type = new ScriptAggregateType( KoLmafiaASH.MONSTER_TYPE, monsterCount );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < monsterCount; ++i )
			{
				value.aset( new ScriptValue( i ), KoLmafiaASH.parseMonsterValue( data.getMonster( i ).getName() ) );
			}

			return value;

		}

		public ScriptValue expected_damage()
		{
			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue =
				Math.max( 0, FightRequest.getMonsterAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

			float damageAbsorb =
				1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue expected_damage( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ZERO_VALUE;
			}

			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue =
				Math.max( 0, monster.getAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

			float damageAbsorb =
				1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue monster_level_adjustment()
		{
			return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue weight_adjustment()
		{
			return new ScriptValue( KoLCharacter.getFamiliarWeightAdjustment() );
		}

		public ScriptValue mana_cost_modifier()
		{
			return new ScriptValue( KoLCharacter.getManaCostAdjustment() );
		}

		public ScriptValue raw_damage_absorption()
		{
			return new ScriptValue( KoLCharacter.getDamageAbsorption() );
		}

		public ScriptValue damage_absorption_percent()
		{
			int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
			if ( raw == 0 )
			{
				return KoLmafiaASH.ZERO_FLOAT_VALUE;
			}

			// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
			// ( sqrt( raw / 10 ) - 1 ) / 10

			double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
			return new ScriptValue( (float) percent );
		}

		public ScriptValue damage_reduction()
		{
			return new ScriptValue( KoLCharacter.getDamageReduction() );
		}

		public ScriptValue elemental_resistance()
		{
			return new ScriptValue( KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() ) );
		}

		public ScriptValue elemental_resistance( final ScriptVariable arg )
		{
			if ( arg.getType().equals( KoLmafiaASH.TYPE_ELEMENT ) )
			{
				return new ScriptValue( KoLCharacter.getElementalResistance( arg.intValue() ) );
			}

			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ZERO_VALUE;
			}

			return new ScriptValue( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
		}

		public ScriptValue combat_rate_modifier()
		{
			return new ScriptValue( KoLCharacter.getCombatRateAdjustment() );
		}

		public ScriptValue initiative_modifier()
		{
			return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
		}

		public ScriptValue experience_bonus()
		{
			return new ScriptValue( KoLCharacter.getExperienceAdjustment() );
		}

		public ScriptValue meat_drop_modifier()
		{
			return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
		}

		public ScriptValue item_drop_modifier()
		{
			return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
		}

		public ScriptValue buffed_hit_stat()
		{
			int hitStat = KoLCharacter.getAdjustedHitStat();
			return new ScriptValue( hitStat );
		}

		public ScriptValue current_hit_stat()
		{
			return KoLCharacter.hitStat() == KoLConstants.MOXIE ? KoLmafiaASH.parseStatValue( "moxie" ) : KoLmafiaASH.parseStatValue( "muscle" );
		}

		public ScriptValue monster_element()
		{
			int element = FightRequest.getMonsterDefenseElement();
			return new ScriptValue( KoLmafiaASH.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
		}

		public ScriptValue monster_element( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ELEMENT_INIT;
			}

			int element = monster.getDefenseElement();
			return new ScriptValue( KoLmafiaASH.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
		}

		public ScriptValue monster_attack()
		{
			return new ScriptValue( FightRequest.getMonsterAttack() );
		}

		public ScriptValue monster_attack( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ZERO_VALUE;
			}

			return new ScriptValue( monster.getAttack() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_defense()
		{
			return new ScriptValue( FightRequest.getMonsterDefense() );
		}

		public ScriptValue monster_defense( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ZERO_VALUE;
			}

			return new ScriptValue( monster.getDefense() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_hp()
		{
			return new ScriptValue( FightRequest.getMonsterHealth() );
		}

		public ScriptValue monster_hp( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return KoLmafiaASH.ZERO_VALUE;
			}

			return new ScriptValue( monster.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) );
		}

		public ScriptValue item_drops()
		{
			Monster monster = FightRequest.getLastMonster();
			List data = monster == null ? new ArrayList() : monster.getItems();

			ScriptMap value = new ScriptMap( KoLmafiaASH.RESULT_TYPE );
			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				value.aset(
					KoLmafiaASH.parseItemValue( result.getName() ),
					KoLmafiaASH.parseIntValue( String.valueOf( result.getCount() ) ) );
			}

			return value;
		}

		public ScriptValue item_drops( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			List data = monster == null ? new ArrayList() : monster.getItems();

			ScriptMap value = new ScriptMap( KoLmafiaASH.RESULT_TYPE );
			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				value.aset(
					KoLmafiaASH.parseItemValue( result.getName() ),
					KoLmafiaASH.parseIntValue( String.valueOf( result.getCount() ) ) );
			}

			return value;
		}

		public ScriptValue will_usually_dodge()
		{
			return FightRequest.willUsuallyDodge() ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue will_usually_miss()
		{
			return FightRequest.willUsuallyMiss() ? KoLmafiaASH.TRUE_VALUE : KoLmafiaASH.FALSE_VALUE;
		}

		public ScriptValue numeric_modifier( final ScriptVariable modifier )
		{
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLCharacter.currentNumericModifier( mod ) );
		}

		public ScriptValue numeric_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Modifiers.getNumericModifier( name, mod ) );
		}

		public ScriptValue boolean_modifier( final ScriptVariable modifier )
		{
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLCharacter.currentBooleanModifier( mod ) );
		}

		public ScriptValue boolean_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Modifiers.getBooleanModifier( name, mod ) );
		}

		public ScriptValue effect_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLmafiaASH.parseEffectValue( Modifiers.getStringModifier( name, mod ) ) );
		}

		public ScriptValue class_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLmafiaASH.parseClassValue( Modifiers.getStringModifier( name, mod ) ) );
		}

		public ScriptValue stat_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLmafiaASH.parseStatValue( Modifiers.getStringModifier( name, mod ) ) );
		}
	}

	private static class ScriptFunctionList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptFunction n )
		{
			super.add( n );
			return true;
		}

		public ScriptFunction[] findFunctions( final String name )
		{
			ArrayList matches = new ArrayList();

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ( (ScriptFunction) this.get( i ) ).getName().equalsIgnoreCase( name ) )
				{
					matches.add( this.get( i ) );
				}
			}

			ScriptFunction[] matchArray = new ScriptFunction[ matches.size() ];
			matches.toArray( matchArray );
			return matchArray;
		}
	}

	private static class ScriptVariable
		extends ScriptSymbol
	{
		ScriptType type;
		ScriptValue content;
		ScriptExpression expression = null;

		public ScriptVariable( final ScriptType type )
		{
			super( null );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptVariable( final String name, final ScriptType type )
		{
			super( name );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptValue getValue()
		{
			if ( this.expression != null )
			{
				this.content = this.expression.execute();
			}

			return this.content;
		}

		public ScriptType getValueType()
		{
			return this.getValue().getType();
		}

		public Object rawValue()
		{
			return this.getValue().rawValue();
		}

		public int intValue()
		{
			return this.getValue().intValue();
		}

		public ScriptValue toStringValue()
		{
			return this.getValue().toStringValue();
		}

		public float floatValue()
		{
			return this.getValue().floatValue();
		}

		public void setExpression( final ScriptExpression targetExpression )
		{
			this.expression = targetExpression;
		}

		public void forceValue( final ScriptValue targetValue )
		{
			this.content = targetValue;
			this.expression = null;
		}

		public void setValue( final ScriptValue targetValue )
		{
			if ( this.getType().equals( targetValue.getType() ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().equals( KoLmafiaASH.TYPE_STRING ) )
			{
				this.content = targetValue.toStringValue();
				this.expression = null;
			}
			else if ( this.getType().equals( KoLmafiaASH.TYPE_INT ) && targetValue.getType().equals(
				KoLmafiaASH.TYPE_FLOAT ) )
			{
				this.content = targetValue.toIntValue();
				this.expression = null;
			}
			else if ( this.getType().equals( KoLmafiaASH.TYPE_FLOAT ) && targetValue.getType().equals(
				KoLmafiaASH.TYPE_INT ) )
			{
				this.content = targetValue.toFloatValue();
				this.expression = null;
			}
			else if ( this.getType().equals( KoLmafiaASH.TYPE_ANY ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().getBaseType().equals( KoLmafiaASH.TYPE_AGGREGATE ) && targetValue.getType().getBaseType().equals(
				KoLmafiaASH.TYPE_AGGREGATE ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + targetValue.getType() + " to " + this.getType() );
			}
		}
	}

	private static class ScriptVariableList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptVariable n )
		{
			return super.addElement( n );
		}

		public ScriptVariable findVariable( final String name )
		{
			return (ScriptVariable) super.findSymbol( name );
		}

		public Iterator getVariables()
		{
			return this.iterator();
		}
	}

	private static class ScriptVariableReference
		extends ScriptValue
	{
		public ScriptVariable target;

		public ScriptVariableReference( final ScriptVariable target )
		{
			this.target = target;
		}

		public ScriptVariableReference( final String varName, final ScriptScope scope )
		{
			this.target = scope.findVariable( varName, true );
		}

		public boolean valid()
		{
			return this.target != null;
		}

		public ScriptType getType()
		{
			return this.target.getType();
		}

		public String getName()
		{
			return this.target.getName();
		}

		public ScriptExpressionList getIndices()
		{
			return null;
		}

		public int compareTo( final Object o )
		{
			return this.target.getName().compareTo( ( (ScriptVariableReference) o ).target.getName() );
		}

		public ScriptValue execute()
		{
			return this.target.getValue();
		}

		public ScriptValue getValue()
		{
			return this.target.getValue();
		}

		public void forceValue( final ScriptValue targetValue )
		{
			this.target.forceValue( targetValue );
		}

		public void setValue( final ScriptValue targetValue )
		{
			this.target.setValue( targetValue );
		}

		public String toString()
		{
			return this.target.getName();
		}
	}

	private static class ScriptCompositeReference
		extends ScriptVariableReference
	{
		private final ScriptExpressionList indices;

		// Derived from indices: Final slice and index into it
		private ScriptCompositeValue slice;
		private ScriptValue index;

		public ScriptCompositeReference( final ScriptVariable target, final ScriptExpressionList indices )
		{
			super( target );
			this.indices = indices;
		}

		public ScriptType getType()
		{
			ScriptType type = this.target.getType().getBaseType();
			for ( int i = 0; i < this.indices.size(); ++i )
			{
				type = ( (ScriptCompositeType) type ).getDataType( this.indices.get( i ) ).getBaseType();
			}
			return type;
		}

		public String getName()
		{
			return this.target.getName() + "[]";
		}

		public ScriptExpressionList getIndices()
		{
			return this.indices;
		}

		public ScriptValue execute()
		{
			return this.getValue();
		}

		// Evaluate all the indices and step through the slices.
		//
		// When done, this.slice has the final slice and this.index has
		// the final evaluated index.

		private boolean getSlice()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return false;
			}

			this.slice = (ScriptCompositeValue) this.target.getValue();
			this.index = null;

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "AREF: " + this.slice.toString() );

			int count = this.indices.size();
			for ( int i = 0; i < count; ++i )
			{
				ScriptExpression exp = (ScriptExpression) this.indices.get( i );

				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );

				this.index = exp.execute();
				KoLmafiaASH.captureValue( this.index );

				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + this.index.toQuotedString() );
				KoLmafiaASH.traceUnindent();

				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
				{
					KoLmafiaASH.traceUnindent();
					return false;
				}

				// If this is the last index, stop now
				if ( i == count - 1 )
				{
					break;
				}

				ScriptCompositeValue result = (ScriptCompositeValue) this.slice.aref( this.index );

				// Create missing intermediate slices
				if ( result == null )
				{
					result = (ScriptCompositeValue) this.slice.initialValue( this.index );
					this.slice.aset( this.index, result );
				}

				this.slice = result;

				KoLmafiaASH.trace( "AREF <- " + this.slice.toString() );
			}

			KoLmafiaASH.traceUnindent();

			return true;
		}

		public ScriptValue getValue()
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
			{
				ScriptValue result = this.slice.aref( this.index );

				if ( result == null )
				{
					result = this.slice.initialValue( this.index );
					this.slice.aset( this.index, result );
				}

				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "AREF <- " + result.toQuotedString() );
				KoLmafiaASH.traceUnindent();

				return result;
			}

			return null;
		}

		public void setValue( final ScriptValue targetValue )
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
			{
				this.slice.aset( this.index, targetValue );
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "ASET: " + targetValue.toQuotedString() );
				KoLmafiaASH.traceUnindent();
			}
		}

		public ScriptValue removeKey()
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
			{
				ScriptValue result = this.slice.remove( this.index );
				if ( result == null )
				{
					result = this.slice.initialValue( this.index );
				}
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "remove <- " + result.toQuotedString() );
				KoLmafiaASH.traceUnindent();
				return result;
			}
			return null;
		}

		public boolean contains( final ScriptValue index )
		{
			boolean result = false;
			// Iterate through indices to final slice
			if ( this.getSlice() )
			{
				result = this.slice.aref( index ) != null;
			}
			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "contains <- " + result );
			KoLmafiaASH.traceUnindent();
			return result;
		}

		public String toString()
		{
			return this.target.getName() + "[]";
		}
	}

	private static class ScriptVariableReferenceList
		extends ScriptList
	{
		public boolean addElement( final ScriptVariableReference n )
		{
			return super.addElement( n );
		}
	}

	private static class ScriptCommand
	{
		public ScriptValue execute()
		{
			return null;
		}
	}

	private class ScriptFlowControl
		extends ScriptCommand
	{
		int command;

		public ScriptFlowControl( final String command )
		{
			if ( command.equalsIgnoreCase( "break" ) )
			{
				this.command = KoLmafiaASH.COMMAND_BREAK;
			}
			else if ( command.equalsIgnoreCase( "continue" ) )
			{
				this.command = KoLmafiaASH.COMMAND_CONTINUE;
			}
			else if ( command.equalsIgnoreCase( "exit" ) )
			{
				this.command = KoLmafiaASH.COMMAND_EXIT;
			}
			else
			{
				throw new AdvancedScriptException( "Invalid command '" + command + "'" );
			}
		}

		public ScriptFlowControl( final int command )
		{
			this.command = command;
		}

		public String toString()
		{
			switch ( this.command )
			{
			case COMMAND_BREAK:
				return "break";
			case COMMAND_CONTINUE:
				return "continue";
			case COMMAND_EXIT:
				return "exit";
			}
			return "unknown command";
		}

		public ScriptValue execute()
		{
			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );
			KoLmafiaASH.traceUnindent();

			switch ( this.command )
			{
			case COMMAND_BREAK:
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_BREAK;
				return KoLmafiaASH.VOID_VALUE;
			case COMMAND_CONTINUE:
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_CONTINUE;
				return KoLmafiaASH.VOID_VALUE;
			case COMMAND_EXIT:
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			throw new RuntimeException( "Internal error: unknown ScriptCommand type" );
		}
	}

	private static class ScriptCommandList
		extends ScriptList
	{
		public boolean addElement( final ScriptCommand n )
		{
			return super.addElement( n );
		}
	}

	private static class ScriptReturn
		extends ScriptCommand
	{
		private final ScriptExpression returnValue;
		private final ScriptType expectedType;

		public ScriptReturn( final ScriptExpression returnValue, final ScriptType expectedType )
		{
			this.returnValue = returnValue;

			if ( expectedType != null && returnValue != null )
			{
				if ( !KoLmafiaASH.validCoercion( expectedType, returnValue.getType(), "return" ) )
				{
					throw new AdvancedScriptException( "Cannot apply " + returnValue.getType() + " to " + expectedType );
				}
			}

			this.expectedType = expectedType;
		}

		public ScriptType getType()
		{
			if ( this.expectedType != null )
			{
				return this.expectedType;
			}

			if ( this.returnValue == null )
			{
				return KoLmafiaASH.VOID_TYPE;
			}

			return this.returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return this.returnValue;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
			}

			if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
			{
				return null;
			}

			KoLmafiaASH.currentState = KoLmafiaASH.STATE_RETURN;

			if ( this.returnValue == null )
			{
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "Eval: " + this.returnValue );

			ScriptValue result = this.returnValue.execute();
			KoLmafiaASH.captureValue( result );

			KoLmafiaASH.trace( "Returning: " + result );
			KoLmafiaASH.traceUnindent();

			if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_EXIT )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_RETURN;
			}

			if ( result == null )
			{
				return null;
			}

			if ( this.expectedType.equals( KoLmafiaASH.TYPE_STRING ) )
			{
				return result.toStringValue();
			}

			if ( this.expectedType.equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				return result.toFloatValue();
			}

			if ( this.expectedType.equals( KoLmafiaASH.TYPE_INT ) )
			{
				return result.toIntValue();
			}

			return result;
		}

		public String toString()
		{
			return "return " + this.returnValue;
		}
	}

	private class ScriptConditional
		extends ScriptCommand
	{
		public ScriptScope scope;
		private final ScriptExpression condition;

		public ScriptConditional( final ScriptScope scope, final ScriptExpression condition )
		{
			this.scope = scope;
			this.condition = condition;
			if ( !condition.getType().equals( KoLmafiaASH.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );

			KoLmafiaASH.trace( "Test: " + this.condition );

			ScriptValue conditionResult = this.condition.execute();
			KoLmafiaASH.captureValue( conditionResult );

			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() == 1 )
			{
				ScriptValue result = this.scope.execute();

				KoLmafiaASH.traceUnindent();

				if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL )
				{
					return result;
				}

				return KoLmafiaASH.TRUE_VALUE;
			}

			KoLmafiaASH.traceUnindent();
			return KoLmafiaASH.FALSE_VALUE;
		}
	}

	private class ScriptIf
		extends ScriptConditional
	{
		private final ArrayList elseLoops;

		public ScriptIf( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
			this.elseLoops = new ArrayList();
		}

		public Iterator getElseLoops()
		{
			return this.elseLoops.iterator();
		}

		public void addElseLoop( final ScriptConditional elseLoop )
		{
			this.elseLoops.add( elseLoop );
		}

		public ScriptValue execute()
		{
			ScriptValue result = super.execute();
			if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL || result == KoLmafiaASH.TRUE_VALUE )
			{
				return result;
			}

			// Conditional failed. Move to else clauses

			Iterator it = this.elseLoops.iterator();
			ScriptConditional elseLoop;

			while ( it.hasNext() )
			{
				elseLoop = (ScriptConditional) it.next();
				result = elseLoop.execute();

				if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL || result == KoLmafiaASH.TRUE_VALUE )
				{
					return result;
				}
			}

			return KoLmafiaASH.FALSE_VALUE;
		}

		public String toString()
		{
			return "if";
		}
	}

	private class ScriptElseIf
		extends ScriptConditional
	{
		public ScriptElseIf( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
		}

		public String toString()
		{
			return "else if";
		}
	}

	private class ScriptElse
		extends ScriptConditional
	{
		public ScriptElse( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "else" );
			ScriptValue result = this.scope.execute();
			KoLmafiaASH.traceUnindent();

			if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL )
			{
				return result;
			}

			return KoLmafiaASH.TRUE_VALUE;
		}

		public String toString()
		{
			return "else";
		}
	}

	private class ScriptLoop
		extends ScriptCommand
	{
		public ScriptScope scope;

		public ScriptLoop( final ScriptScope scope )
		{
			this.scope = scope;
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public ScriptValue execute()
		{
			ScriptValue result = this.scope.execute();

			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
			}

			switch ( KoLmafiaASH.currentState )
			{
			case STATE_EXIT:
				return null;

			case STATE_BREAK:
				// Stay in state; subclass exits loop
				return KoLmafiaASH.VOID_VALUE;

			case STATE_RETURN:
				// Stay in state; subclass exits loop
				return result;

			case STATE_CONTINUE:
				// Done with this iteration
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
				return result;

			case STATE_NORMAL:
				return result;
			}

			return result;
		}
	}

	private class ScriptForeach
		extends ScriptLoop
	{
		private final ScriptVariableReferenceList variableReferences;
		private final ScriptVariableReference aggregate;

		public ScriptForeach( final ScriptScope scope, final ScriptVariableReferenceList variableReferences,
			final ScriptVariableReference aggregate )
		{
			super( scope );
			this.variableReferences = variableReferences;
			this.aggregate = aggregate;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{
			return this.variableReferences;
		}

		public Iterator getReferences()
		{
			return this.variableReferences.iterator();
		}

		public ScriptVariableReference getAggregate()
		{
			return this.aggregate;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue) this.aggregate.execute();
			KoLmafiaASH.captureValue( slice );
			if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			// Iterate over the slice with bound keyvar

			Iterator it = this.getReferences();
			return this.executeSlice( slice, it, (ScriptVariableReference) it.next() );

		}

		private ScriptValue executeSlice( final ScriptAggregateValue slice, final Iterator it,
			final ScriptVariableReference variable )
		{
			// Get an array of keys for the slice
			ScriptValue[] keys = slice.keys();

			// Get the next key variable
			ScriptVariableReference nextVariable = it.hasNext() ? (ScriptVariableReference) it.next() : null;

			// While there are further keys
			for ( int i = 0; i < keys.length; ++i )
			{
				// Get current key
				ScriptValue key = keys[ i ];

				// Bind variable to key
				variable.setValue( key );

				KoLmafiaASH.trace( "Key #" + i + ": " + key );

				// If there are more indices to bind, recurse
				ScriptValue result;
				if ( nextVariable != null )
				{
					ScriptAggregateValue nextSlice = (ScriptAggregateValue) slice.aref( key );
					KoLmafiaASH.traceIndent();
					result = this.executeSlice( nextSlice, it, nextVariable );
				}
				else
				{
					// Otherwise, execute scope
					result = super.execute();
				}
				switch ( KoLmafiaASH.currentState )
				{
				case STATE_NORMAL:
					break;
				case STATE_BREAK:
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
					// Fall through
				default:
					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			KoLmafiaASH.traceUnindent();
			return KoLmafiaASH.VOID_VALUE;
		}

		public String toString()
		{
			return "foreach";
		}
	}

	private class ScriptWhile
		extends ScriptLoop
	{
		private final ScriptExpression condition;

		public ScriptWhile( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( KoLmafiaASH.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );

			while ( true )
			{
				KoLmafiaASH.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute();
				KoLmafiaASH.captureValue( conditionResult );

				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					KoLmafiaASH.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
				{
					break;
				}

				ScriptValue result = super.execute();

				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_BREAK )
				{
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
					KoLmafiaASH.traceUnindent();
					return KoLmafiaASH.VOID_VALUE;
				}

				if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL )
				{
					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			KoLmafiaASH.traceUnindent();
			return KoLmafiaASH.VOID_VALUE;
		}

		public String toString()
		{
			return "while";
		}
	}

	private class ScriptRepeat
		extends ScriptLoop
	{
		private final ScriptExpression condition;

		public ScriptRepeat( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( KoLmafiaASH.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );

			while ( true )
			{
				ScriptValue result = super.execute();

				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_BREAK )
				{
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
					KoLmafiaASH.traceUnindent();
					return KoLmafiaASH.VOID_VALUE;
				}

				if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL )
				{
					KoLmafiaASH.traceUnindent();
					return result;
				}

				KoLmafiaASH.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute();
				KoLmafiaASH.captureValue( conditionResult );

				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					KoLmafiaASH.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() == 1 )
				{
					break;
				}
			}

			KoLmafiaASH.traceUnindent();
			return KoLmafiaASH.VOID_VALUE;
		}

		public String toString()
		{
			return "repeat";
		}
	}

	private class ScriptFor
		extends ScriptLoop
	{
		private final ScriptVariableReference variable;
		private final ScriptExpression initial;
		private final ScriptExpression last;
		private final ScriptExpression increment;
		private final int direction;

		public ScriptFor( final ScriptScope scope, final ScriptVariableReference variable,
			final ScriptExpression initial, final ScriptExpression last, final ScriptExpression increment,
			final int direction )
		{
			super( scope );
			this.variable = variable;
			this.initial = initial;
			this.last = last;
			this.increment = increment;
			this.direction = direction;
		}

		public ScriptVariableReference getVariable()
		{
			return this.variable;
		}

		public ScriptExpression getInitial()
		{
			return this.initial;
		}

		public ScriptExpression getLast()
		{
			return this.last;
		}

		public ScriptExpression getIncrement()
		{
			return this.increment;
		}

		public int getDirection()
		{
			return this.direction;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( this.toString() );

			// Get the initial value
			KoLmafiaASH.trace( "Initial: " + this.initial );

			ScriptValue initialValue = this.initial.execute();
			KoLmafiaASH.captureValue( initialValue );

			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + initialValue );

			if ( initialValue == null )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			// Get the final value
			KoLmafiaASH.trace( "Last: " + this.last );

			ScriptValue lastValue = this.last.execute();
			KoLmafiaASH.captureValue( lastValue );

			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + lastValue );

			if ( lastValue == null )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			// Get the increment
			KoLmafiaASH.trace( "Increment: " + this.increment );

			ScriptValue incrementValue = this.increment.execute();
			KoLmafiaASH.captureValue( incrementValue );

			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + incrementValue );

			if ( incrementValue == null )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			int current = initialValue.intValue();
			int increment = incrementValue.intValue();
			int end = lastValue.intValue();

			boolean up = false;

			if ( this.direction > 0 )
			{
				up = true;
			}
			else if ( this.direction < 0 )
			{
				up = false;
			}
			else
			{
				up = current <= end;
			}

			if ( up && increment < 0 || !up && increment > 0 )
			{
				increment = -increment;
			}

			// Make sure the loop will eventually terminate

			if ( current != end && increment == 0 )
			{
				throw new AdvancedScriptException( "Start not equal to end and increment equals 0" );
			}

			while ( up && current <= end || !up && current >= end )
			{
				// Bind variable to current value
				this.variable.setValue( new ScriptValue( current ) );

				// Execute the scope
				ScriptValue result = super.execute();

				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_BREAK )
				{
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
					KoLmafiaASH.traceUnindent();
					return KoLmafiaASH.VOID_VALUE;
				}

				if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL )
				{
					KoLmafiaASH.traceUnindent();
					return result;
				}

				// Calculate next value
				current += increment;
			}

			KoLmafiaASH.traceUnindent();
			return KoLmafiaASH.VOID_VALUE;
		}

		public String toString()
		{
			return "for";
		}
	}

	private class ScriptBasicScript
		extends ScriptValue
	{
		private final ByteArrayStream data;

		public ScriptBasicScript( final ByteArrayStream data )
		{
			this.data = data;
		}

		public ScriptType getType()
		{
			return KoLmafiaASH.VOID_TYPE;
		}

		public ScriptValue execute()
		{
			KoLmafiaCLI script = new KoLmafiaCLI( this.data.getByteArrayInputStream() );
			script.listenForCommands();
			return KoLmafiaASH.VOID_VALUE;
		}
	}

	private class ScriptCall
		extends ScriptValue
	{
		private final ScriptFunction target;
		private final ScriptExpressionList params;

		public ScriptCall( final String functionName, final ScriptScope scope, final ScriptExpressionList params )
		{
			this.target = this.findFunction( functionName, scope, params );
			if ( this.target == null )
			{
				throw new AdvancedScriptException( "Undefined reference '" + functionName + "'" );
			}
			this.params = params;
		}

		private ScriptFunction findFunction( final String name, final ScriptScope scope,
			final ScriptExpressionList params )
		{
			if ( scope == null )
			{
				return null;
			}

			return scope.findFunction( name, params );
		}

		public ScriptFunction getTarget()
		{
			return this.target;
		}

		public Iterator getExpressions()
		{
			return this.params.iterator();
		}

		public ScriptType getType()
		{
			return this.target.getType();
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			// Save current variable bindings
			this.target.saveBindings();
			KoLmafiaASH.traceIndent();

			Iterator refIterator = this.target.getReferences();
			Iterator valIterator = this.params.getExpressions();

			ScriptVariableReference paramVarRef;
			ScriptExpression paramValue;

			int paramCount = 0;

			while ( refIterator.hasNext() )
			{
				paramVarRef = (ScriptVariableReference) refIterator.next();

				++paramCount;

				if ( !valIterator.hasNext() )
				{
					this.target.restoreBindings();
					throw new RuntimeException( "Internal error: illegal arguments" );
				}

				paramValue = (ScriptExpression) valIterator.next();

				KoLmafiaASH.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

				ScriptValue value = paramValue.execute();
				KoLmafiaASH.captureValue( value );

				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + value.toQuotedString() );

				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
				{
					this.target.restoreBindings();
					KoLmafiaASH.traceUnindent();
					return null;
				}

				// Bind parameter to new value
				if ( paramVarRef.getType().equals( KoLmafiaASH.TYPE_STRING ) )
				{
					paramVarRef.setValue( value.toStringValue() );
				}
				else if ( paramVarRef.getType().equals( KoLmafiaASH.TYPE_INT ) && paramValue.getType().equals(
					KoLmafiaASH.TYPE_FLOAT ) )
				{
					paramVarRef.setValue( value.toIntValue() );
				}
				else if ( paramVarRef.getType().equals( KoLmafiaASH.TYPE_FLOAT ) && paramValue.getType().equals(
					KoLmafiaASH.TYPE_INT ) )
				{
					paramVarRef.setValue( value.toFloatValue() );
				}
				else
				{
					paramVarRef.setValue( value );
				}
			}

			if ( valIterator.hasNext() )
			{
				this.target.restoreBindings();
				throw new RuntimeException( "Internal error: illegal arguments" );
			}

			KoLmafiaASH.trace( "Entering function " + this.target.getName() );
			ScriptValue result = this.target.execute();
			KoLmafiaASH.trace( "Function " + this.target.getName() + " returned: " + result );

			if ( KoLmafiaASH.currentState != KoLmafiaASH.STATE_EXIT )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
			}

			// Restore initial variable bindings
			this.target.restoreBindings();
			KoLmafiaASH.traceUnindent();

			return result;
		}

		public String toString()
		{
			return this.target.getName() + "()";
		}
	}

	private class ScriptAssignment
		extends ScriptCommand
	{
		private final ScriptVariableReference lhs;
		private final ScriptExpression rhs;

		public ScriptAssignment( final ScriptVariableReference lhs, final ScriptExpression rhs )
		{
			this.lhs = lhs;
			this.rhs = rhs;

			if ( rhs != null && !KoLmafiaASH.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
			{
				throw new AdvancedScriptException(
					"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			}
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return this.rhs == null ? this.lhs.getType().initialValueExpression() : this.rhs;
		}

		public ScriptType getType()
		{
			return this.lhs.getType();
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
				return null;
			}

			ScriptValue value;

			if ( this.rhs == null )
			{
				value = this.lhs.getType().initialValue();
			}
			else
			{
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Eval: " + this.rhs );

				value = this.rhs.execute();
				KoLmafiaASH.captureValue( value );

				KoLmafiaASH.trace( "Set: " + value );
				KoLmafiaASH.traceUnindent();
			}

			if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
			{
				return null;
			}

			if ( this.lhs.getType().equals( KoLmafiaASH.TYPE_STRING ) )
			{
				this.lhs.setValue( value.toStringValue() );
			}
			else if ( this.lhs.getType().equals( KoLmafiaASH.TYPE_INT ) )
			{
				this.lhs.setValue( value.toIntValue() );
			}
			else if ( this.lhs.getType().equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				this.lhs.setValue( value.toFloatValue() );
			}
			else
			{
				this.lhs.setValue( value );
			}

			return KoLmafiaASH.VOID_VALUE;
		}

		public String toString()
		{
			return this.rhs == null ? this.lhs.getName() : this.lhs.getName() + " = " + this.rhs;
		}
	}

	private static class ScriptType
		extends ScriptSymbol
	{
		public boolean primitive;
		private final int type;

		public ScriptType( final String name, final int type )
		{
			super( name );
			this.primitive = true;
			this.type = type;
		}

		public int getType()
		{
			return this.type;
		}

		public ScriptType getBaseType()
		{
			return this;
		}

		public boolean isPrimitive()
		{
			return this.primitive;
		}

		public boolean equals( final ScriptType type )
		{
			return this.type == type.type;
		}

		public boolean equals( final int type )
		{
			return this.type == type;
		}

		public String toString()
		{
			return this.name;
		}

		public ScriptType simpleType()
		{
			return this;
		}

		public ScriptValue initialValue()
		{
			switch ( this.type )
			{
			case TYPE_VOID:
				return KoLmafiaASH.VOID_VALUE;
			case TYPE_BOOLEAN:
				return KoLmafiaASH.BOOLEAN_INIT;
			case TYPE_INT:
				return KoLmafiaASH.INT_INIT;
			case TYPE_FLOAT:
				return KoLmafiaASH.FLOAT_INIT;
			case TYPE_STRING:
				return KoLmafiaASH.STRING_INIT;
			case TYPE_BUFFER:
				return new ScriptValue( KoLmafiaASH.BUFFER_TYPE, "", new StringBuffer() );
			case TYPE_MATCHER:
				return new ScriptValue( KoLmafiaASH.MATCHER_TYPE, "", Pattern.compile( "" ).matcher( "" ) );

			case TYPE_ITEM:
				return KoLmafiaASH.ITEM_INIT;
			case TYPE_LOCATION:
				return KoLmafiaASH.LOCATION_INIT;
			case TYPE_CLASS:
				return KoLmafiaASH.CLASS_INIT;
			case TYPE_STAT:
				return KoLmafiaASH.STAT_INIT;
			case TYPE_SKILL:
				return KoLmafiaASH.SKILL_INIT;
			case TYPE_EFFECT:
				return KoLmafiaASH.EFFECT_INIT;
			case TYPE_FAMILIAR:
				return KoLmafiaASH.FAMILIAR_INIT;
			case TYPE_SLOT:
				return KoLmafiaASH.SLOT_INIT;
			case TYPE_MONSTER:
				return KoLmafiaASH.MONSTER_INIT;
			case TYPE_ELEMENT:
				return KoLmafiaASH.ELEMENT_INIT;
			}
			return null;
		}

		public ScriptValue parseValue( final String name )
		{
			switch ( this.type )
			{
			case TYPE_BOOLEAN:
				return KoLmafiaASH.parseBooleanValue( name );
			case TYPE_INT:
				return KoLmafiaASH.parseIntValue( name );
			case TYPE_FLOAT:
				return KoLmafiaASH.parseFloatValue( name );
			case TYPE_STRING:
				return KoLmafiaASH.parseStringValue( name );
			case TYPE_ITEM:
				return KoLmafiaASH.parseItemValue( name );
			case TYPE_LOCATION:
				return KoLmafiaASH.parseLocationValue( name );
			case TYPE_CLASS:
				return KoLmafiaASH.parseClassValue( name );
			case TYPE_STAT:
				return KoLmafiaASH.parseStatValue( name );
			case TYPE_SKILL:
				return KoLmafiaASH.parseSkillValue( name );
			case TYPE_EFFECT:
				return KoLmafiaASH.parseEffectValue( name );
			case TYPE_FAMILIAR:
				return KoLmafiaASH.parseFamiliarValue( name );
			case TYPE_SLOT:
				return KoLmafiaASH.parseSlotValue( name );
			case TYPE_MONSTER:
				return KoLmafiaASH.parseMonsterValue( name );
			case TYPE_ELEMENT:
				return KoLmafiaASH.parseElementValue( name );
			}
			return null;
		}

		public ScriptExpression initialValueExpression()
		{
			return this.initialValue();
		}

		public boolean containsAggregate()
		{
			return false;
		}
	}

	private static class ScriptNamedType
		extends ScriptType
	{
		ScriptType base;

		public ScriptNamedType( final String name, final ScriptType base )
		{
			super( name, KoLmafiaASH.TYPE_TYPEDEF );
			this.base = base;
		}

		public ScriptType getBaseType()
		{
			return this.base.getBaseType();
		}

		public ScriptExpression initialValueExpression()
		{
			return new ScriptTypeInitializer( this.base.getBaseType() );
		}
	}

	private static class ScriptCompositeType
		extends ScriptType
	{
		public ScriptCompositeType( final String name, final int type )
		{
			super( name, type );
			this.primitive = false;
		}

		public ScriptType getIndexType()
		{
			return null;
		}

		public ScriptType getDataType()
		{
			return null;
		}

		public ScriptType getDataType( final Object key )
		{
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			return key;
		}

		public ScriptExpression initialValueExpression()
		{
			return new ScriptTypeInitializer( this );
		}
	}

	private static class ScriptAggregateType
		extends ScriptCompositeType
	{
		private final ScriptType dataType;
		private final ScriptType indexType;
		private final int size;

		// Map
		public ScriptAggregateType( final ScriptType dataType, final ScriptType indexType )
		{
			super( "aggregate", KoLmafiaASH.TYPE_AGGREGATE );
			this.dataType = dataType;
			this.indexType = indexType;
			this.size = 0;
		}

		// Array
		public ScriptAggregateType( final ScriptType dataType, final int size )
		{
			super( "aggregate", KoLmafiaASH.TYPE_AGGREGATE );
			this.primitive = false;
			this.dataType = dataType;
			this.indexType = KoLmafiaASH.INT_TYPE;
			this.size = size;
		}

		public ScriptType getDataType()
		{
			return this.dataType;
		}

		public ScriptType getDataType( final Object key )
		{
			return this.dataType;
		}

		public ScriptType getIndexType()
		{
			return this.indexType;
		}

		public int getSize()
		{
			return this.size;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptAggregateType && this.dataType.equals( ( (ScriptAggregateType) o ).dataType ) && this.indexType.equals( ( (ScriptAggregateType) o ).indexType );
		}

		public ScriptType simpleType()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				return this.dataType.simpleType();
			}
			return this.dataType;
		}

		public String toString()
		{
			return this.simpleType().toString() + " [" + this.indexString() + "]";
		}

		public String indexString()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				String suffix = ", " + ( (ScriptAggregateType) this.dataType ).indexString();
				if ( this.size != 0 )
				{
					return this.size + suffix;
				}
				return this.indexType.toString() + suffix;
			}

			if ( this.size != 0 )
			{
				return String.valueOf( this.size );
			}
			return this.indexType.toString();
		}

		public ScriptValue initialValue()
		{
			if ( this.size != 0 )
			{
				return new ScriptArray( this );
			}
			return new ScriptMap( this );
		}

		public boolean containsAggregate()
		{
			return true;
		}
	}

	private static class ScriptRecordType
		extends ScriptCompositeType
	{
		private final String[] fieldNames;
		private final ScriptType[] fieldTypes;
		private final ScriptValue[] fieldIndices;

		public ScriptRecordType( final String name, final String[] fieldNames, final ScriptType[] fieldTypes )
		{
			super( name, KoLmafiaASH.TYPE_RECORD );
			if ( fieldNames.length != fieldTypes.length )
			{
				throw new AdvancedScriptException( "Internal error: wrong number of field types" );
			}

			this.fieldNames = fieldNames;
			this.fieldTypes = fieldTypes;

			// Build field index values.
			// These can be either integers or strings.
			//   Integers don't require a lookup
			//   Strings make debugging easier.

			this.fieldIndices = new ScriptValue[ fieldNames.length ];
			for ( int i = 0; i < fieldNames.length; ++i )
			{
				this.fieldIndices[ i ] = new ScriptValue( fieldNames[ i ] );
			}
		}

		public String[] getFieldNames()
		{
			return this.fieldNames;
		}

		public ScriptType[] getFieldTypes()
		{
			return this.fieldTypes;
		}

		public ScriptValue[] getFieldIndices()
		{
			return this.fieldIndices;
		}

		public int fieldCount()
		{
			return this.fieldTypes.length;
		}

		public ScriptType getIndexType()
		{
			return KoLmafiaASH.STRING_TYPE;
		}

		public ScriptType getDataType( final Object key )
		{
			if ( !( key instanceof ScriptValue ) )
			{
				throw new RuntimeException( "Internal error: key is not a ScriptValue" );
			}
			int index = this.indexOf( (ScriptValue) key );
			if ( index < 0 || index >= this.fieldTypes.length )
			{
				return null;
			}
			return this.fieldTypes[ index ];
		}

		public ScriptValue getFieldIndex( final String field )
		{
			String val = field.toLowerCase();
			for ( int index = 0; index < this.fieldNames.length; ++index )
			{
				if ( val.equals( this.fieldNames[ index ] ) )
				{
					return this.fieldIndices[ index ];
				}
			}
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( KoLmafiaASH.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return null;
				}
				return this.fieldIndices[ index ];
			}

			if ( type.equals( KoLmafiaASH.TYPE_STRING ) )
			{
				String str = key.toString();
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( this.fieldNames[ index ].equals( str ) )
					{
						return this.fieldIndices[ index ];
					}
				}
				return null;
			}

			return null;
		}

		public int indexOf( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( KoLmafiaASH.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return -1;
				}
				return index;
			}

			if ( type.equals( KoLmafiaASH.TYPE_STRING ) )
			{
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( key == this.fieldIndices[ index ] )
					{
						return index;
					}
				}
				return -1;
			}

			return -1;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptRecordType && this.name == ( (ScriptRecordType) o ).name;
		}

		public ScriptType simpleType()
		{
			return this;
		}

		public String toString()
		{
			return this.name;
		}

		public ScriptValue initialValue()
		{
			return new ScriptRecord( this );
		}

		public boolean containsAggregate()
		{
			for ( int i = 0; i < this.fieldTypes.length; ++i )
			{
				if ( this.fieldTypes[ i ].containsAggregate() )
				{
					return true;
				}
			}
			return false;
		}
	}

	private static class ScriptTypeInitializer
		extends ScriptValue
	{
		public ScriptType type;

		public ScriptTypeInitializer( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptValue execute()
		{
			return this.type.initialValue();
		}

		public String toString()
		{
			return "<initial value>";
		}
	}

	private static class ScriptTypeList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptType n )
		{
			return super.addElement( n );
		}

		public ScriptType findType( final String name )
		{
			return (ScriptType) super.findSymbol( name );
		}
	}

	private static class ScriptValue
		extends ScriptExpression
		implements Comparable
	{
		public ScriptType type;

		public int contentInt = 0;
		public float contentFloat = 0.0f;
		public String contentString = null;
		public Object content = null;

		public ScriptValue()
		{
			this.type = KoLmafiaASH.VOID_TYPE;
		}

		public ScriptValue( final int value )
		{
			this.type = KoLmafiaASH.INT_TYPE;
			this.contentInt = value;
		}

		public ScriptValue( final boolean value )
		{
			this.type = KoLmafiaASH.BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( final String value )
		{
			this.type = KoLmafiaASH.STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( final float value )
		{
			this.type = KoLmafiaASH.FLOAT_TYPE;
			this.contentInt = (int) value;
			this.contentFloat = value;
		}

		public ScriptValue( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptValue( final ScriptType type, final int contentInt, final String contentString )
		{
			this.type = type;
			this.contentInt = contentInt;
			this.contentString = contentString;
		}

		public ScriptValue( final ScriptType type, final String contentString, final Object content )
		{
			this.type = type;
			this.contentString = contentString;
			this.content = content;
		}

		public ScriptValue( final ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public ScriptValue toFloatValue()
		{
			if ( this.type.equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (float) this.contentInt );
			}
		}

		public ScriptValue toIntValue()
		{
			if ( this.type.equals( KoLmafiaASH.TYPE_INT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (int) this.contentFloat );
			}
		}

		public ScriptType getType()
		{
			return this.type.getBaseType();
		}

		public String toString()
		{
			if ( this.content instanceof StringBuffer )
			{
				return ( (StringBuffer) this.content ).toString();
			}

			if ( this.type.equals( KoLmafiaASH.TYPE_VOID ) )
			{
				return "void";
			}

			if ( this.contentString != null )
			{
				return this.contentString;
			}

			if ( this.type.equals( KoLmafiaASH.TYPE_BOOLEAN ) )
			{
				return String.valueOf( this.contentInt != 0 );
			}

			if ( this.type.equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				return String.valueOf( this.contentFloat );
			}

			return String.valueOf( this.contentInt );
		}

		public String toQuotedString()
		{
			if ( this.contentString != null )
			{
				return "\"" + this.contentString + "\"";
			}
			return this.toString();
		}

		public ScriptValue toStringValue()
		{
			return new ScriptValue( this.toString() );
		}

		public Object rawValue()
		{
			return this.content;
		}

		public int intValue()
		{
			return this.contentInt;
		}

		public float floatValue()
		{
			return this.contentFloat;
		}

		public ScriptValue execute()
		{
			return this;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptValue ) )
			{
				throw new ClassCastException();
			}

			ScriptValue it = (ScriptValue) o;

			if ( this.type == KoLmafiaASH.BOOLEAN_TYPE || this.type == KoLmafiaASH.INT_TYPE )
			{
				return this.contentInt < it.contentInt ? -1 : this.contentInt == it.contentInt ? 0 : 1;
			}

			if ( this.type == KoLmafiaASH.FLOAT_TYPE )
			{
				return this.contentFloat < it.contentFloat ? -1 : this.contentFloat == it.contentFloat ? 0 : 1;
			}

			if ( this.contentString != null )
			{
				return this.contentString.compareTo( it.contentString );
			}

			return -1;
		}

		public int count()
		{
			return 1;
		}

		public void clear()
		{
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public boolean equals( final Object o )
		{
			return o == null || !( o instanceof ScriptValue ) ? false : this.compareTo( (Comparable) o ) == 0;
		}

		public void dumpValue( final PrintStream writer )
		{
			writer.print( this.toStringValue().toString() );
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			writer.println( prefix + this.toStringValue().toString() );
		}
	}

	private static class ScriptCompositeValue
		extends ScriptValue
	{
		public ScriptCompositeValue( final ScriptCompositeType type )
		{
			super( type );
		}

		public ScriptCompositeType getCompositeType()
		{
			return (ScriptCompositeType) this.type;
		}

		public ScriptValue aref( final ScriptValue key )
		{
			return null;
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
		}

		public ScriptValue remove( final ScriptValue key )
		{
			return null;
		}

		public void clear()
		{
		}

		public ScriptValue[] keys()
		{
			return new ScriptValue[ 0 ];
		}

		public ScriptValue initialValue( final Object key )
		{
			return ( (ScriptCompositeType) this.type ).getDataType( key ).initialValue();
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			ScriptValue[] keys = this.keys();
			if ( keys.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < keys.length; ++i )
			{
				ScriptValue key = keys[ i ];
				ScriptValue value = this.aref( key );
				String first = prefix + key + "\t";
				value.dump( writer, first, compact );
			}
		}

		public void dumpValue( final PrintStream writer )
		{
		}

		// Returns number of fields consumed
		public int read( final String[] data, final int index, final boolean compact )
		{
			ScriptCompositeType type = (ScriptCompositeType) this.type;
			ScriptValue key = null;

			if ( index < data.length )
			{
				key = type.getKey( KoLmafiaASH.parseValue( type.getIndexType(), data[ index ] ) );
			}
			else
			{
				key = type.getKey( KoLmafiaASH.parseValue( type.getIndexType(), "none" ) );
			}

			// If there's only a key and a value, parse the value
			// and store it in the composite

			if ( !( type.getDataType( key ) instanceof ScriptCompositeType ) )
			{
				this.aset( key, KoLmafiaASH.parseValue( type.getDataType( key ), data[ index + 1 ] ) );
				return 2;
			}

			// Otherwise, recurse until we get the final slice
			ScriptCompositeValue slice = (ScriptCompositeValue) this.aref( key );

			// Create missing intermediate slice
			if ( slice == null )
			{
				slice = (ScriptCompositeValue) this.initialValue( key );
				this.aset( key, slice );
			}

			return slice.read( data, index + 1, compact ) + 1;
		}

		public String toString()
		{
			return "composite " + this.type.toString();
		}
	}

	private static class ScriptAggregateValue
		extends ScriptCompositeValue
	{
		public ScriptAggregateValue( final ScriptAggregateType type )
		{
			super( type );
		}

		public ScriptType getDataType()
		{
			return ( (ScriptAggregateType) this.type ).getDataType();
		}

		public int count()
		{
			return 0;
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public String toString()
		{
			return "aggregate " + this.type.toString();
		}
	}

	private static class ScriptArray
		extends ScriptAggregateValue
	{
		public ScriptArray( final ScriptAggregateType type )
		{
			super( type );

			int size = type.getSize();
			ScriptType dataType = type.getDataType();
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataType.initialValue();
			}
			this.content = content;
		}

		public ScriptValue aref( final ScriptValue index )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int i = index.intValue();
			if ( i < 0 || i > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			return array[ i ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_INT ) && val.getType().equals(
				KoLmafiaASH.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_FLOAT ) && val.getType().equals(
				KoLmafiaASH.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType().initialValue();
			return result;
		}

		public void clear()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = this.getDataType().initialValue();
			}
		}

		public int count()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array.length;
		}

		public boolean contains( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			return index >= 0 && index < array.length;
		}

		public ScriptValue[] keys()
		{
			int size = ( (ScriptValue[]) this.content ).length;
			ScriptValue[] result = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				result[ i ] = new ScriptValue( i );
			}
			return result;
		}
	}

	private static class ScriptMap
		extends ScriptAggregateValue
	{
		public ScriptMap( final ScriptAggregateType type )
		{
			super( type );
			this.content = new TreeMap();
		}

		public ScriptValue aref( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.get( key );
		}

		public void aset( final ScriptValue key, ScriptValue val )
		{
			TreeMap map = (TreeMap) this.content;

			if ( !this.getDataType().equals( val.getType() ) )
			{
				if ( this.getDataType().equals( KoLmafiaASH.TYPE_STRING ) )
				{
					val = val.toStringValue();
				}
				else if ( this.getDataType().equals( KoLmafiaASH.TYPE_INT ) && val.getType().equals(
					KoLmafiaASH.TYPE_FLOAT ) )
				{
					val = val.toIntValue();
				}
				else if ( this.getDataType().equals( KoLmafiaASH.TYPE_FLOAT ) && val.getType().equals(
					KoLmafiaASH.TYPE_INT ) )
				{
					val = val.toFloatValue();
				}
			}

			map.put( key, val );
		}

		public ScriptValue remove( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.remove( key );
		}

		public void clear()
		{
			TreeMap map = (TreeMap) this.content;
			map.clear();
		}

		public int count()
		{
			TreeMap map = (TreeMap) this.content;
			return map.size();
		}

		public boolean contains( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return map.containsKey( key );
		}

		public ScriptValue[] keys()
		{
			Set set = ( (TreeMap) this.content ).keySet();
			ScriptValue[] keys = new ScriptValue[ set.size() ];
			set.toArray( keys );
			return keys;
		}
	}

	private static class ScriptRecord
		extends ScriptCompositeValue
	{
		public ScriptRecord( final ScriptRecordType type )
		{
			super( type );

			ScriptType[] dataTypes = type.getFieldTypes();
			int size = dataTypes.length;
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataTypes[ i ].initialValue();
			}
			this.content = content;
		}

		public ScriptRecordType getRecordType()
		{
			return (ScriptRecordType) this.type;
		}

		public ScriptType getDataType( final ScriptValue key )
		{
			return ( (ScriptRecordType) this.type ).getDataType( key );
		}

		public ScriptValue aref( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public ScriptValue aref( final int index )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}

			this.aset( index, val );
		}

		public void aset( final int index, final ScriptValue val )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}

			ScriptValue[] array = (ScriptValue[]) this.content;

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_INT ) && val.getType().equals(
				KoLmafiaASH.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( KoLmafiaASH.TYPE_FLOAT ) && val.getType().equals(
				KoLmafiaASH.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType( key ).initialValue();
			return result;
		}

		public void clear()
		{
			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = dataTypes[ index ].initialValue();
			}
		}

		public ScriptValue[] keys()
		{
			return ( (ScriptRecordType) this.type ).getFieldIndices();
		}

		public void dump( final PrintStream writer, final String prefix, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				super.dump( writer, prefix, compact );
				return;
			}

			writer.print( prefix );
			this.dumpValue( writer );
			writer.println();
		}

		public void dumpValue( final PrintStream writer )
		{
			int size = ( (ScriptRecordType) this.type ).getFieldTypes().length;
			for ( int i = 0; i < size; ++i )
			{
				ScriptValue value = this.aref( i );
				if ( i > 0 )
				{
					writer.print( "\t" );
				}
				value.dumpValue( writer );
			}
		}

		public int read( final String[] data, int index, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				return super.read( data, index, compact );
			}

			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;

			int size = Math.min( dataTypes.length, data.length - index );
			int first = index;

			// Consume remaining data values and store them
			for ( int offset = 0; offset < size; ++offset )
			{
				ScriptType valType = dataTypes[ offset ];
				if ( valType instanceof ScriptRecordType )
				{
					ScriptRecord rec = (ScriptRecord) array[ offset ];
					index += rec.read( data, index, true );
				}
				else
				{
					array[ offset ] = KoLmafiaASH.parseValue( valType, data[ index ] );
					index += 1;
				}
			}

			for ( int offset = size; offset < dataTypes.length; ++offset )
			{
				array[ offset ] = KoLmafiaASH.parseValue( dataTypes[ offset ], "none" );
			}

			// assert index == data.length
			return index - first;
		}

		public String toString()
		{
			return "record " + this.type.toString();
		}
	}

	private static class ScriptExpression
		extends ScriptCommand
	{
		ScriptExpression lhs;
		ScriptExpression rhs;
		ScriptOperator oper;

		public ScriptExpression()
		{
		}

		public ScriptExpression( final ScriptExpression lhs, final ScriptExpression rhs, final ScriptOperator oper )
		{
			this.lhs = lhs;
			this.rhs = rhs;
			this.oper = oper;
		}

		public ScriptType getType()
		{
			ScriptType leftType = this.lhs.getType();

			// Unary operators have no right hand side
			if ( this.rhs == null )
			{
				return leftType;
			}

			ScriptType rightType = this.rhs.getType();

			// String concatenation always yields a string
			if ( this.oper.equals( "+" ) && ( leftType.equals( KoLmafiaASH.TYPE_STRING ) || rightType.equals( KoLmafiaASH.TYPE_STRING ) ) )
			{
				return KoLmafiaASH.STRING_TYPE;
			}

			// If it's not arithmetic, it's boolean
			if ( !this.oper.isArithmetic() )
			{
				return KoLmafiaASH.BOOLEAN_TYPE;
			}

			// Coerce int to float
			if ( leftType.equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				return KoLmafiaASH.FLOAT_TYPE;
			}

			// Otherwise result is whatever is on right
			return rightType;
		}

		public ScriptExpression getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return this.rhs;
		}

		public ScriptOperator getOperator()
		{
			return this.oper;
		}

		public ScriptValue execute()
		{
			return this.oper.applyTo( this.lhs, this.rhs );
		}

		public String toString()
		{
			if ( this.rhs == null )
			{
				return this.oper.toString() + " " + this.lhs.toQuotedString();
			}
			return "( " + this.lhs.toQuotedString() + " " + this.oper.toString() + " " + this.rhs.toQuotedString() + " )";
		}

		public String toQuotedString()
		{
			return this.toString();
		}
	}

	private static class ScriptExpressionList
		extends ScriptList
	{
		public Iterator getExpressions()
		{
			return this.iterator();
		}
	}

	private static class ScriptOperator
	{
		String operator;

		public ScriptOperator( final String operator )
		{
			if ( operator == null )
			{
				throw new RuntimeException( "Internal error in ScriptOperator()" );
			}

			this.operator = operator;
		}

		public boolean equals( final String op )
		{
			return this.operator.equals( op );
		}

		public boolean precedes( final ScriptOperator oper )
		{
			return this.operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if ( this.operator.equals( "!" ) || this.operator.equals( "contains" ) || this.operator.equals( "remove" ) )
			{
				return 7;
			}

			if ( this.operator.equals( "^" ) )
			{
				return 6;
			}

			if ( this.operator.equals( "*" ) || this.operator.equals( "/" ) || this.operator.equals( "%" ) )
			{
				return 5;
			}

			if ( this.operator.equals( "+" ) || this.operator.equals( "-" ) )
			{
				return 4;
			}

			if ( this.operator.equals( "<" ) || this.operator.equals( ">" ) || this.operator.equals( "<=" ) || this.operator.equals( ">=" ) )
			{
				return 3;
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) || this.operator.equals( "!=" ) )
			{
				return 2;
			}

			if ( this.operator.equals( "||" ) || this.operator.equals( "&&" ) )
			{
				return 1;
			}

			return -1;
		}

		public boolean isArithmetic()
		{
			return this.operator.equals( "+" ) || this.operator.equals( "-" ) || this.operator.equals( "*" ) || this.operator.equals( "^" ) || this.operator.equals( "/" ) || this.operator.equals( "%" );
		}

		public String toString()
		{
			return this.operator;
		}

		public ScriptValue applyTo( final ScriptExpression lhs, final ScriptExpression rhs )
		{
			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "Operator: " + this.operator );

			// Unary operator with special evaluation of argument
			if ( this.operator.equals( "remove" ) )
			{
				ScriptCompositeReference operand = (ScriptCompositeReference) lhs;
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Operand: " + operand );
				KoLmafiaASH.traceUnindent();
				ScriptValue result = operand.removeKey();
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "Operand 1: " + lhs );
			ScriptValue leftValue = lhs.execute();
			KoLmafiaASH.captureValue( leftValue );
			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + leftValue.toQuotedString() );
			KoLmafiaASH.traceUnindent();
			if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			// Unary Operators
			if ( this.operator.equals( "!" ) )
			{
				ScriptValue result = new ScriptValue( leftValue.intValue() == 0 );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "-" ) && rhs == null )
			{
				ScriptValue result = null;
				if ( lhs.getType().equals( KoLmafiaASH.TYPE_INT ) )
				{
					result = new ScriptValue( 0 - leftValue.intValue() );
				}
				else if ( lhs.getType().equals( KoLmafiaASH.TYPE_FLOAT ) )
				{
					result = new ScriptValue( 0.0f - leftValue.floatValue() );
				}
				else
				{
					throw new RuntimeException( "Unary minus can only be applied to numbers" );
				}
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			// Unknown operator
			if ( rhs == null )
			{
				throw new RuntimeException( "Internal error: missing right operand." );
			}

			// Binary operators with optional right values
			if ( this.operator.equals( "||" ) )
			{
				if ( leftValue.intValue() == 1 )
				{
					KoLmafiaASH.trace( "<- " + KoLmafiaASH.TRUE_VALUE );
					KoLmafiaASH.traceUnindent();
					return KoLmafiaASH.TRUE_VALUE;
				}
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				KoLmafiaASH.captureValue( rightValue );
				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + rightValue.toQuotedString() );
				KoLmafiaASH.traceUnindent();
				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
				{
					KoLmafiaASH.traceUnindent();
					return null;
				}
				KoLmafiaASH.trace( "<- " + rightValue );
				KoLmafiaASH.traceUnindent();
				return rightValue;
			}

			if ( this.operator.equals( "&&" ) )
			{
				if ( leftValue.intValue() == 0 )
				{
					KoLmafiaASH.traceUnindent();
					KoLmafiaASH.trace( "<- " + KoLmafiaASH.FALSE_VALUE );
					return KoLmafiaASH.FALSE_VALUE;
				}
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				KoLmafiaASH.captureValue( rightValue );
				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + rightValue.toQuotedString() );
				KoLmafiaASH.traceUnindent();
				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
				{
					KoLmafiaASH.traceUnindent();
					return null;
				}
				KoLmafiaASH.trace( "<- " + rightValue );
				KoLmafiaASH.traceUnindent();
				return rightValue;
			}

			// Ensure type compatibility of operands
			if ( !KoLmafiaASH.validCoercion( lhs.getType(), rhs.getType(), this.operator ) )
			{
				throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );
			}

			// Special binary operator: <aggref> contains <any>
			if ( this.operator.equals( "contains" ) )
			{
				KoLmafiaASH.traceIndent();
				KoLmafiaASH.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				KoLmafiaASH.captureValue( rightValue );
				KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + rightValue.toQuotedString() );
				KoLmafiaASH.traceUnindent();
				if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
				{
					KoLmafiaASH.traceUnindent();
					return null;
				}
				ScriptValue result = new ScriptValue( leftValue.contains( rightValue ) );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			// Binary operators
			KoLmafiaASH.traceIndent();
			KoLmafiaASH.trace( "Operand 2: " + rhs );
			ScriptValue rightValue = rhs.execute();
			KoLmafiaASH.captureValue( rightValue );
			KoLmafiaASH.trace( "[" + KoLmafiaASH.executionStateString( KoLmafiaASH.currentState ) + "] <- " + rightValue.toQuotedString() );
			KoLmafiaASH.traceUnindent();
			if ( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT )
			{
				KoLmafiaASH.traceUnindent();
				return null;
			}

			// String operators
			if ( this.operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( KoLmafiaASH.TYPE_STRING ) || rhs.getType().equals( KoLmafiaASH.TYPE_STRING ) )
				{
					KoLmafiaASH.concatenateBuffer.setLength( 0 );
					KoLmafiaASH.concatenateBuffer.append( leftValue.toStringValue().toString() );
					KoLmafiaASH.concatenateBuffer.append( rightValue.toStringValue().toString() );
					ScriptValue result = new ScriptValue( KoLmafiaASH.concatenateBuffer.toString() );
					KoLmafiaASH.trace( "<- " + result );
					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				if ( lhs.getType().equals( KoLmafiaASH.TYPE_STRING ) || lhs.getType().equals( KoLmafiaASH.TYPE_LOCATION ) || lhs.getType().equals(
					KoLmafiaASH.TYPE_MONSTER ) )
				{
					ScriptValue result =
						new ScriptValue( leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
					KoLmafiaASH.trace( "<- " + result );
					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			if ( this.operator.equals( "!=" ) )
			{
				if ( lhs.getType().equals( KoLmafiaASH.TYPE_STRING ) || lhs.getType().equals( KoLmafiaASH.TYPE_LOCATION ) || lhs.getType().equals(
					KoLmafiaASH.TYPE_MONSTER ) )
				{
					ScriptValue result =
						new ScriptValue( !leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
					KoLmafiaASH.trace( "<- " + result );
					KoLmafiaASH.traceUnindent();
					return result;
				}
			}

			// Arithmetic operators
			boolean isInt;
			float lfloat = 0.0f, rfloat = 0.0f;
			int lint = 0, rint = 0;

			if ( lhs.getType().equals( KoLmafiaASH.TYPE_FLOAT ) || rhs.getType().equals( KoLmafiaASH.TYPE_FLOAT ) )
			{
				isInt = false;
				lfloat = leftValue.toFloatValue().floatValue();
				rfloat = rightValue.toFloatValue().floatValue();
			}
			else
			{
				isInt = true;
				lint = leftValue.intValue();
				rint = rightValue.intValue();
			}

			if ( this.operator.equals( "+" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint + rint ) : new ScriptValue( lfloat + rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "-" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint - rint ) : new ScriptValue( lfloat - rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "*" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint * rint ) : new ScriptValue( lfloat * rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "^" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (int) Math.pow( lint, rint ) ) : new ScriptValue( (float) Math.pow(
						lfloat, rfloat ) );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "/" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (float) lint / (float) rint ) : new ScriptValue( lfloat / rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "%" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint % rint ) : new ScriptValue( lfloat % rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint < rint ) : new ScriptValue( lfloat < rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint > rint ) : new ScriptValue( lfloat > rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint <= rint ) : new ScriptValue( lfloat <= rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint >= rint ) : new ScriptValue( lfloat >= rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint == rint ) : new ScriptValue( lfloat == rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "!=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint != rint ) : new ScriptValue( lfloat != rfloat );
				KoLmafiaASH.trace( "<- " + result );
				KoLmafiaASH.traceUnindent();
				return result;
			}

			// Unknown operator
			throw new RuntimeException( "Internal error: illegal operator \"" + this.operator + "\"" );
		}
	}

	private static class ScriptList
		extends ArrayList
	{
		public boolean addElement( final Object n )
		{
			this.add( n );
			return true;
		}
	}

	public static class AdvancedScriptException
		extends RuntimeException
	{
		AdvancedScriptException( final Throwable t )
		{
			this( t.getMessage() == null ? "" : t.getMessage() + " " + KoLmafiaASH.getCurrentLineAndFile() );
		}

		AdvancedScriptException( final String s )
		{
			super( s == null ? "" : s + " " + KoLmafiaASH.getCurrentLineAndFile() );
		}
	}
}
