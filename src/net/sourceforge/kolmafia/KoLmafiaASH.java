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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
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

import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public class KoLmafiaASH extends StaticEntity
{
	/* Variables for Advanced Scripting */

	public final static char [] tokenList = { ' ', '.', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '\'', '*', '^', '/', '%', '[', ']', '!', ';', '<', '>' };
	public final static String [] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&", "/*", "*/" };

	public static final int TYPE_ANY = 0;
	public static final int TYPE_VOID = 1;
	public static final int TYPE_BOOLEAN = 2;
	public static final int TYPE_INT = 3;
	public static final int TYPE_FLOAT = 4;
	public static final int TYPE_STRING = 5;

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

	public static final String [] CLASSES = { KoLCharacter.SEAL_CLUBBER, KoLCharacter.TURTLE_TAMER, KoLCharacter.PASTAMANCER, KoLCharacter.SAUCEROR, KoLCharacter.DISCO_BANDIT, KoLCharacter.ACCORDION_THIEF };
	public static final String [] STATS = { "Muscle", "Mysticality", "Moxie" };
	public static final String [] BOOLEANS = { "true", "false" };

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;
	public static final int COMMAND_EXIT = 3;

	public static final int STATE_NORMAL = 1;
	public static final int STATE_RETURN = 2;
	public static final int STATE_BREAK = 3;
	public static final int STATE_CONTINUE = 4;
	public static final int STATE_EXIT = 5;

	private static final ScriptType ANY_TYPE = new ScriptType( "any", TYPE_ANY );
	private static final ScriptType VOID_TYPE = new ScriptType( "void", TYPE_VOID );
	private static final ScriptType BOOLEAN_TYPE = new ScriptType( "boolean", TYPE_BOOLEAN );
	private static final ScriptType INT_TYPE = new ScriptType( "int", TYPE_INT );
	private static final ScriptType FLOAT_TYPE = new ScriptType( "float", TYPE_FLOAT );
	private static final ScriptType STRING_TYPE = new ScriptType( "string", TYPE_STRING );

	private static final ScriptType ITEM_TYPE = new ScriptType( "item", TYPE_ITEM );
	private static final ScriptType LOCATION_TYPE = new ScriptType( "location", TYPE_LOCATION );
	private static final ScriptType CLASS_TYPE = new ScriptType( "class", TYPE_CLASS );
	private static final ScriptType STAT_TYPE = new ScriptType( "stat", TYPE_STAT );
	private static final ScriptType SKILL_TYPE = new ScriptType( "skill", TYPE_SKILL );
	private static final ScriptType EFFECT_TYPE = new ScriptType( "effect", TYPE_EFFECT );
	private static final ScriptType FAMILIAR_TYPE = new ScriptType( "familiar", TYPE_FAMILIAR );
	private static final ScriptType SLOT_TYPE = new ScriptType( "slot", TYPE_SLOT );
	private static final ScriptType MONSTER_TYPE = new ScriptType( "monster", TYPE_MONSTER );
	private static final ScriptType ELEMENT_TYPE = new ScriptType( "element", TYPE_ELEMENT );

	private static final ScriptType AGGREGATE_TYPE = new ScriptType( "aggregate", TYPE_AGGREGATE );
	private static final ScriptAggregateType RESULT_TYPE = new ScriptAggregateType( INT_TYPE, ITEM_TYPE );
	private static final ScriptAggregateType REGEX_GROUP_TYPE = new ScriptAggregateType( new ScriptAggregateType( STRING_TYPE, INT_TYPE ), INT_TYPE );

	// Common values

	private static final ScriptValue VOID_VALUE = new ScriptValue();
	private static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	private static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	private static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	private static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	private static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0f );

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	private static final ScriptValue BOOLEAN_INIT = FALSE_VALUE;
	private static final ScriptValue INT_INIT = ZERO_VALUE;
	private static final ScriptValue FLOAT_INIT = ZERO_FLOAT_VALUE;
	private static final ScriptValue STRING_INIT = new ScriptValue( "" );
	private static final ScriptValue ITEM_INIT = new ScriptValue( ITEM_TYPE, -1, "none" );
	private static final ScriptValue LOCATION_INIT = new ScriptValue( LOCATION_TYPE, "none", (Object)null );
	private static final ScriptValue CLASS_INIT = new ScriptValue( CLASS_TYPE, -1, "none" );
	private static final ScriptValue STAT_INIT = new ScriptValue( STAT_TYPE, -1, "none" );
	private static final ScriptValue SKILL_INIT = new ScriptValue( SKILL_TYPE, -1, "none" );
	private static final ScriptValue EFFECT_INIT = new ScriptValue( EFFECT_TYPE, -1, "none" );
	private static final ScriptValue FAMILIAR_INIT = new ScriptValue( FAMILIAR_TYPE, -1, "none" );
	private static final ScriptValue SLOT_INIT = new ScriptValue( SLOT_TYPE, -1, "none" );
	private static final ScriptValue MONSTER_INIT = new ScriptValue( MONSTER_TYPE, "none", (Object)null );
	private static final ScriptValue ELEMENT_INIT = new ScriptValue( ELEMENT_TYPE, "none", (Object)null );

	// Variables used during parsing
	private static final ScriptFunctionList existingFunctions = getExistingFunctions();
	private static final ScriptTypeList simpleTypes = getSimpleTypes();
	private static final ScriptSymbolTable reservedWords = getReservedWords();

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
	private int currentState = STATE_NORMAL;

	// Feature control;

	// disabled until and if we choose to document the feature
	private static String lastImportString = "";
	private static String notifyRecipient = null;
	private static boolean isExecuting = false;
	private static boolean arrays = false;

	private static final TreeMap TIMESTAMPS = new TreeMap();
	private static final TreeMap INTERPRETERS = new TreeMap();

	public KoLmafiaASH()
	{
		this.global = new ScriptScope( new ScriptVariableList(), getExistingFunctionScope() );
	}

	private KoLmafiaASH( KoLmafiaASH source, File scriptFile )
	{
		this.global = source.global;
		this.imports = source.imports;
		this.fileName = scriptFile.getPath();

		try
		{
			this.commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );

			this.currentLine = getNextLine();
			this.lineNumber = commandStream.getLineNumber();
			this.nextLine = getNextLine();
		}
		catch ( Exception e )
		{
			// If any part of the initialization fails,
			// then throw an exception.

			throw new AdvancedScriptException( fileName + " could not be accessed" );
		}
	}

	public static final KoLmafiaASH getInterpreter( File toExecute )
	{
		if ( toExecute == null )
			return null;

		KoLmafiaASH interpreter;
		boolean createInterpreter = !TIMESTAMPS.containsKey( toExecute );

		if ( !createInterpreter )
			createInterpreter = ((Long)TIMESTAMPS.get( toExecute )).longValue() != toExecute.lastModified();

		if ( !createInterpreter )
		{
			interpreter = (KoLmafiaASH) INTERPRETERS.get( toExecute );
			Iterator it = interpreter.imports.keySet().iterator();

			while ( it.hasNext() && !createInterpreter )
			{
				File file = (File) it.next();
				createInterpreter = ((Long) interpreter.imports.get( file )).longValue() != file.lastModified();
			}
		}

		if ( createInterpreter )
		{
			TIMESTAMPS.clear();

			interpreter = new KoLmafiaASH();
			if ( !interpreter.validate( toExecute ) )
				return null;

			TIMESTAMPS.put( toExecute, new Long( toExecute.lastModified() ) );
			INTERPRETERS.put( toExecute, interpreter );
		}

		return (KoLmafiaASH) INTERPRETERS.get( toExecute );
	}


	// **************** Data Types *****************

	// For each simple data type X, we supply:
	// private static ScriptValue parseXValue( String name );

	private static ScriptValue parseBooleanValue( String name )
	{
		if ( name.equalsIgnoreCase( "true" ) )
			return TRUE_VALUE;
		if ( name.equalsIgnoreCase( "false" ) )
			return FALSE_VALUE;

		if ( isExecuting )
			return parseInt( name ) == 0 ? FALSE_VALUE : TRUE_VALUE;

		throw new AdvancedScriptException( "Can't interpret '" + name + "' as a boolean" );
	}

	private static ScriptValue parseIntValue( String name ) throws NumberFormatException
	{	return new ScriptValue( parseInt( name ) );
	}

	private static ScriptValue parseFloatValue( String name ) throws NumberFormatException
	{	return new ScriptValue( parseFloat( name ) );
	}

	private static ScriptValue parseStringValue( String name )
	{	return new ScriptValue( name );
	}

	private static ScriptValue parseItemValue( String name )
	{
		if ( name == null || name.equalsIgnoreCase( "none" ) )
			return ITEM_INIT;

		// Allow for an item number to be specified
		// inside of the "item" construct.

		int itemId;
		for ( int i = 0; i < name.length(); ++i )
		{
			// If you get an actual item number, then store it
			// inside of contentInt and return from the method.
			// But, in this case, we're testing if it's not an item
			// number -- use substring matching to make it
			// user-friendlier.

			if ( !Character.isDigit( name.charAt(i) ) )
			{
				AdventureResult item = KoLmafiaCLI.getFirstMatchingItem( name, false );

				if ( item == null )
				{
					if ( isExecuting )
						return ITEM_INIT;

					throw new AdvancedScriptException( "Item " + name + " not found in database" );
				}

				itemId = item.getItemId();
				name = TradeableItemDatabase.getItemName( itemId );
				return new ScriptValue( ITEM_TYPE, itemId, name );
			}
		}

		// Since it is numeric, parse the integer value
		// and store it inside of the contentInt.

		itemId = parseInt( name );
		name = TradeableItemDatabase.getItemName( itemId );
		return new ScriptValue( ITEM_TYPE, itemId, name );
	}

	private static ScriptValue parseLocationValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return LOCATION_INIT;

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			if ( isExecuting )
				return LOCATION_INIT;

			throw new AdvancedScriptException( "Location " + name + " not found in database" );
		}

		return new ScriptValue( LOCATION_TYPE, name, (Object) content );
	}

	private static int classToInt( String name )
	{
		for ( int i = 0; i < CLASSES.length; ++i )
			if ( name.equalsIgnoreCase( CLASSES[i] ) )
				return i;
		return -1;
	}

	private static ScriptValue parseClassValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return CLASS_INIT;

		int num = classToInt( name );
		if ( num < 0 )
		{
			if ( isExecuting )
				return CLASS_INIT;

			throw new AdvancedScriptException( "Unknown class " + name );
		}

		return new ScriptValue( CLASS_TYPE, num, CLASSES[num] );
	}

	private static int statToInt( String name )
	{
		for ( int i = 0; i < STATS.length; ++i )
			if ( name.equalsIgnoreCase( STATS[i] ) )
				return i;
		return -1;
	}

	private static ScriptValue parseStatValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return STAT_INIT;

		int num = statToInt( name );
		if ( num < 0 )
		{
			if ( isExecuting )
				return STAT_INIT;

			throw new AdvancedScriptException( "Unknown stat " + name );
		}

		return new ScriptValue( STAT_TYPE, num, STATS[num] );
	}

	private static ScriptValue parseSkillValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return SKILL_INIT;

		List skills = ClassSkillsDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
		{
			if ( isExecuting )
				return SKILL_INIT;

			throw new AdvancedScriptException( "Skill " + name + " not found in database" );
		}

		int num = ClassSkillsDatabase.getSkillId( (String)skills.get(0) );
		name = ClassSkillsDatabase.getSkillName( num );
		return new ScriptValue( SKILL_TYPE, num, name );
	}

	private static ScriptValue parseEffectValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return EFFECT_INIT;

		AdventureResult effect = KoLmafiaCLI.getFirstMatchingEffect( name );
		if ( effect == null )
		{
			if ( isExecuting )
				return EFFECT_INIT;

			throw new AdvancedScriptException( "Effect " + name + " not found in database" );
		}

		int num = StatusEffectDatabase.getEffectId( effect.getName() );
		name = StatusEffectDatabase.getEffectName( num );
		return new ScriptValue( EFFECT_TYPE, num, name );
	}

	private static ScriptValue parseFamiliarValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return FAMILIAR_INIT;

		int num = FamiliarsDatabase.getFamiliarId( name );
		if ( num == -1 )
		{
			if ( isExecuting )
				return FAMILIAR_INIT;

			throw new AdvancedScriptException( "Familiar " + name + " not found in database" );
		}

		name = FamiliarsDatabase.getFamiliarName( num );
		return new ScriptValue( FAMILIAR_TYPE, num, name );
	}

	private static ScriptValue parseSlotValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return SLOT_INIT;

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
		{
			if ( isExecuting )
				return SLOT_INIT;

			throw new AdvancedScriptException( "Bad slot name " + name );
		}

		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( SLOT_TYPE, num, name );
	}

	private static ScriptValue parseMonsterValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return MONSTER_INIT;

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
		{
			if ( isExecuting )
				return MONSTER_INIT;

			throw new AdvancedScriptException( "Bad monster name " + name );
		}

		return new ScriptValue( MONSTER_TYPE, monster.getName(), (Object)monster );
	}

	private static ScriptValue parseElementValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return ELEMENT_INIT;

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
		{
			if ( isExecuting )
				return ELEMENT_INIT;

			throw new AdvancedScriptException( "Bad element name " + name );
		}

		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( ELEMENT_TYPE, num, name );
	}

	private static ScriptValue parseValue( ScriptType type, String name )
	{	return type.parseValue( name );
	}

	// For data types which map to integers, also supply:
	//
	// private static ScriptValue makeXValue( int num )
	//     throws nothing.

	private static ScriptValue makeItemValue( int num )
	{
		String name = TradeableItemDatabase.getItemName( num );

		if ( name == null )
			return ITEM_INIT;

		return new ScriptValue( ITEM_TYPE, num, name );
	}

	private static ScriptValue makeItemValue( String name )
	{
		int num = TradeableItemDatabase.getItemId( name );

		if ( num == -1 )
			return ITEM_INIT;

		return new ScriptValue( ITEM_TYPE, num, name );
	}

	private static ScriptValue makeClassValue( String name )
	{
		return new ScriptValue( CLASS_TYPE, classToInt( name ), name );
	}

	private static ScriptValue makeSkillValue( int num )
	{
		String name = ClassSkillsDatabase.getSkillName( num );
		if ( name == null )
			return SKILL_INIT;

		return new ScriptValue( SKILL_TYPE, num, name );
	}

	private static ScriptValue makeEffectValue( int num )
	{
		String name = StatusEffectDatabase.getEffectName( num );
		if ( name == null )
			return EFFECT_INIT;
		return new ScriptValue( EFFECT_TYPE, num, name );
	}

	private static ScriptValue makeFamiliarValue( int num )
	{
		String name = FamiliarsDatabase.getFamiliarName( num );
		if ( name == null )
			return FAMILIAR_INIT;
		return new ScriptValue( FAMILIAR_TYPE, num, name );
	}

	private static ScriptValue makeSlotValue( int num )
	{
		String name;

		if ( num < 0 || num >= EquipmentRequest.slotNames.length )
			name = "bogus";
		else
			name  = EquipmentRequest.slotNames[num];

		return new ScriptValue( SLOT_TYPE, num, name );
	}

	private static ScriptValue makeElementValue( int num )
	{
		String name;

		if ( num < 0 || num >= MonsterDatabase.elementNames.length )
			name = "bogus";
		else
			name  = MonsterDatabase.elementNames[num];

		return new ScriptValue( ELEMENT_TYPE, num, name );
	}

	// **************** Tracing *****************

	private static boolean tracing = true;
	private static int traceIndentation = 0;

	private static void resetTracing()
	{
		traceIndentation = 0;
	}

	private static void traceIndent()
	{	traceIndentation++;
	}

	private static void traceUnindent()
	{	traceIndentation--;
	}

	private static void traceUnindent( int levels )
	{	traceIndentation -= levels;
	}

	private static void trace( String string )
	{
		if ( tracing )
		{
			indentLine( traceIndentation );
			RequestLogger.updateDebugLog( string );
		}
	}

	private static String executionStateString( int state )
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

		return String.valueOf(state);
	}

	// **************** Parsing *****************

	public boolean validate( File scriptFile )
	{
		this.fileName = scriptFile.getPath();

		try
		{
			return validate( new FileInputStream( scriptFile ) );
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( ERROR_STATE, e.getMessage() );
			return false;
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return false;
		}
	}

	public boolean validate( InputStream istream )
	{
		try
		{
			this.commandStream = new LineNumberReader( new InputStreamReader( istream ) );
		}
		catch ( Exception e )
		{
			this.commandStream = null;
			return false;
		}

		this.currentLine = getNextLine();
		this.lineNumber = commandStream.getLineNumber();
		this.nextLine = getNextLine();

		this.global = parseScope( null, null, new ScriptVariableList(), getExistingFunctionScope(), false );
		printScope( global, 0 );

		try
		{
			this.commandStream.close();
		}
		catch ( Exception e )
		{
			this.commandStream = null;
			return false;
		}

		this.commandStream = null;

		if ( this.currentLine != null )
			throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

		return true;
	}

	public void execute( String functionName, String [] parameters )
	{
		// Before you do anything, validate the script, if one
		// is provided.  One will not be provided in the event
		// that we are using a global namespace.

		if ( this == NAMESPACE_INTERPRETER )
		{
			String importString = getProperty( "commandLineNamespace" );
			if ( importString.equals( "" ) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "No available namespace with function: " + functionName );
				return;
			}

			boolean shouldRefresh = !lastImportString.equals( importString );

			if ( !shouldRefresh )
			{
				Iterator it = imports.keySet().iterator();

				while ( it.hasNext() && !shouldRefresh )
				{
					File file = (File) it.next();
					shouldRefresh = ((Long) imports.get( file )).longValue() != file.lastModified();
				}
			}

			if ( shouldRefresh )
			{
				imports.clear();
				lastImportString = "";

				this.global = new ScriptScope( new ScriptVariableList(), getExistingFunctionScope() );
				ScriptScope result = this.global;

				String [] importList = importString.split( "," );

				for ( int i = 0; i < importList.length; ++i )
					result = parseFile( importList[i] );
			}
		}

		String currentScript = (fileName == null) ? "<>" : "<" + fileName + ">";
		String notifyList = StaticEntity.getProperty( "previousNotifyList" );

		if ( notifyRecipient != null && notifyList.indexOf( currentScript ) == -1 )
		{
			StaticEntity.setProperty( "previousNotifyList", notifyList + currentScript );

			GreenMessageRequest notifier = new GreenMessageRequest( notifyRecipient, currentScript );
			RequestThread.postRequest( notifier );
		}

		try
		{
			boolean wasExecuting = isExecuting;

			isExecuting = true;
			executeScope( global, functionName, parameters );
			isExecuting = wasExecuting;
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( ERROR_STATE, e.getMessage() );
			return;
		}
		catch ( RuntimeException e )
		{
			// If it's an exception resulting from
			// a premature abort, which causes void
			// values to be returned, ignore.

			printStackTrace( e );
		}
	}

	private String getNextLine()
	{
		try
		{
			do
			{
				// Read a currentLine from input, and break out of the
				// do-while loop when you've read a valid currentLine
				// (which is a non-comment and a non-blank currentLine
				// ) or when you've reached EOF.

				this.fullLine = commandStream.readLine();

				// Return null at end of file
				if ( this.fullLine == null )
					return null;

				// Remove in-currentLine comment
				int comment = this.fullLine.indexOf( "//" );
				if ( comment != -1 )
					this.fullLine = this.fullLine.substring( 0, comment );

				// Remove whitespace at front and end
				this.fullLine = this.fullLine.trim();
			}
			while ( this.fullLine.length() == 0 || this.fullLine.startsWith( "#" ) || this.fullLine.startsWith( "\'" ) || this.fullLine.startsWith( "//" ) );

			// Found valid currentLine - return it

			return this.fullLine;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
			return null;
		}
	}

	private ScriptScope parseFile( String fileName )
	{
		File scriptFile = KoLmafiaCLI.findScriptFile( fileName );
		if ( scriptFile == null || !scriptFile.exists() )
			throw new AdvancedScriptException( fileName + " could not be found" );

		if ( imports.containsKey( scriptFile ) )
			return this.global;

		AdvancedScriptException error = null;

		ScriptScope result = this.global;
		KoLmafiaASH script = new KoLmafiaASH( this, scriptFile );

		try
		{
			result = script.parseScope( this.global, null, new ScriptVariableList(), this.global.parentScope, false );
		}
		catch ( Exception e )
		{
			error = new AdvancedScriptException( e );
		}

		try
		{
			script.commandStream.close();
		}
		catch ( Exception e )
		{
			// Do nothing, because this means the stream somehow
			// got closed anyway.
		}

		if ( error != null )
			throw error;

		if ( script.currentLine != null )
			throw new AdvancedScriptException( "Script parsing error " + script.getLineAndFile() );

		imports.put( scriptFile, new Long( scriptFile.lastModified() ) );
		mainMethod = null;

		return result;
	}

	private ScriptScope parseScope( ScriptScope startScope, ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop )
	{
		ScriptScope result;
		String importString;

		result = startScope == null ? new ScriptScope( variables, parentScope ) : startScope;
		parseNotify();

		while ( (importString = parseImport()) != null )
			result = parseFile( importString );

		while ( true )
		{
			if ( parseTypedef( result ) )
			{
				if ( !currentToken().equals( ";" ) )
					parseError( ";", currentToken() );

				readToken(); //read ;
				continue;
			}

			ScriptType t = parseType( result, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ScriptCommand c = parseCommand( expectedType, result, false, whileLoop );
				if ( c != null )
				{
					result.addCommand( c );

					continue;
				}

				// No type and no command -> done.
				break;
			}

			// If this is a new record definition, enter it
			if ( t.getType() == TYPE_RECORD && currentToken() != null && currentToken().equals( ";" ) )
			{
				readToken();	// read ;
				continue;
			}

			ScriptFunction f = parseFunction( t, result );
			if ( f != null )
			{
				if ( f.getName().equalsIgnoreCase( "main" ) )
					mainMethod = f;

				continue;
			}

			if ( parseVariables( t, result ) )
			{
				if ( !currentToken().equals( ";" ) )
					parseError( ";", currentToken() );

				readToken(); //read ;
				continue;
			}

			//Found a type but no function or variable to tie it to
			throw new AdvancedScriptException( "Script parse error " + getLineAndFile() );
		}

		return result;
	}

	private ScriptType parseRecord( ScriptScope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "record" ) )
			return null;

		readToken(); // read record

		if ( currentToken() == null )
			throw new AdvancedScriptException( "Record name expected " + getLineAndFile() );

		// Allow anonymous records
		String recordName = null;

		if ( !currentToken().equals( "{" ) )
		{
			// Named record
			recordName = currentToken();

			if ( !parseIdentifier( recordName ) )
				throw new AdvancedScriptException( "Invalid record name '" + recordName + "' " + getLineAndFile() );

			if ( isReservedWord( recordName ) )
				throw new AdvancedScriptException( "Reserved word '" + recordName + "' cannot be a record name " + getLineAndFile() );

			if ( parentScope.findType( recordName ) != null )
				throw new AdvancedScriptException( "Record name '" + recordName + "' is already defined " + getLineAndFile() );

			readToken(); // read name
		}

		if ( currentToken() == null || !currentToken().equals( "{" ) )
			parseError( "{", currentToken() );

		readToken(); // read {

		// Loop collecting fields
		ArrayList fieldTypes = new ArrayList();
		ArrayList fieldNames = new ArrayList();

		while ( true )
		{
			// Get the field type
			ScriptType fieldType = parseType( parentScope, true, true );
			if ( fieldType == null )
				throw new AdvancedScriptException( "Type name expected " + getLineAndFile() );

			// Get the field name
			String fieldName = currentToken();
			if ( fieldName == null )
				throw new AdvancedScriptException( "Field name expected " + getLineAndFile() );

			if ( !parseIdentifier( fieldName ) )
				throw new AdvancedScriptException( "Invalid field name '" + fieldName + "' " + getLineAndFile() );

			if ( isReservedWord( fieldName ) )
				throw new AdvancedScriptException( "Reserved word '" + fieldName + "' cannot be used as a field name " + getLineAndFile() );

			if ( fieldNames.contains( fieldName ) )
				throw new AdvancedScriptException( "Field name '" + fieldName + "' is already defined " + getLineAndFile() );

			readToken(); // read name

			if ( currentToken() == null || !currentToken().equals( ";" ) )
				parseError( ";", currentToken() );

			readToken(); // read ;

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.toLowerCase() );

			if ( currentToken() == null )
				parseError( "}", "EOF" );

			if ( currentToken().equals( "}" ) )
			     break;
		}

		readToken(); // read }

		String [] fieldNameArray = new String[ fieldNames.size() ];
		ScriptType [] fieldTypeArray = new ScriptType[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		ScriptRecordType rec = new ScriptRecordType( recordName == null ? "(anonymous record)" : recordName , fieldNameArray, fieldTypeArray );

		if ( recordName != null )
			// Enter into type table
			parentScope.addType( rec );

		return rec;
	}

	private ScriptFunction parseFunction( ScriptType functionType, ScriptScope parentScope )
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		String functionName = currentToken();

		if ( isReservedWord( functionName ) )
			throw new AdvancedScriptException( "Reserved word '" + functionName + "' cannot be used as a function name " + getLineAndFile() );

		readToken(); //read Function name
		readToken(); //read (

		ScriptVariableList paramList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();

		while ( !currentToken().equals( ")" ) )
		{
			ScriptType paramType = parseType( parentScope, true, false );
			if ( paramType == null )
				parseError( ")", currentToken() );

			ScriptVariable param = parseVariable( paramType, null );
			if ( param == null )
				parseError( "identifier", currentToken() );

			if ( !paramList.addElement( param ) )
				throw new AdvancedScriptException( "Variable " + param.getName() + " is already defined " + getLineAndFile() );

			if ( !currentToken().equals( ")" ) )
			{
				if ( !currentToken().equals( "," ) )
					parseError( ")", currentToken() );

				readToken(); //read comma
			}

			variableReferences.addElement( new ScriptVariableReference( param ) );
		}

		readToken(); //read )

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion. Replace an existing
		// forward reference.

		ScriptUserDefinedFunction result = parentScope.replaceFunction( new ScriptUserDefinedFunction( functionName, functionType, variableReferences ) );
		if ( currentToken() != null && currentToken().equals( ";" ) )
		{
			// Yes. Return forward reference
			readToken(); // ;
			return result;
		}

		ScriptScope scope;
		if ( currentToken() != null && currentToken().equals( "{" ) )
		{
			// Scope is a block

			readToken(); // {

			scope = parseScope( null, functionType, paramList, parentScope, false );
			if ( currentToken() == null || !currentToken().equals( "}" ) )
				parseError( "}", currentToken() );

			readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new ScriptScope( paramList, parentScope );
			scope.addCommand( parseCommand( functionType, parentScope, false, false ) );
		}

		result.setScope( scope );
		if ( !result.assertReturn() && !functionType.equals( TYPE_VOID )
		     // The following clause can't be correct. I think it
		     // depends on the various conditional & loop constructs
		     // returning a boolean. Or something. But without it,
		     // existing scripts break. Aargh!
		     && !functionType.equals( TYPE_BOOLEAN ) )
			throw new AdvancedScriptException( "Missing return value " + getLineAndFile() );

		return result;
	}

	private boolean parseVariables( ScriptType t, ScriptScope parentScope )
	{
		while ( true )
		{
			ScriptVariable v = parseVariable( t, parentScope );
			if ( v == null )
				return false;

			if ( currentToken().equals( "," ) )
			{
				readToken(); //read ,
				continue;
			}

			return true;
		}
	}

	private ScriptVariable parseVariable( ScriptType t, ScriptScope scope )
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		String variableName = currentToken();
		if ( isReservedWord( variableName ) )
			throw new AdvancedScriptException( "Reserved word '" + variableName + "' cannot be a variable name " + getLineAndFile() );

		ScriptVariable result = new ScriptVariable( variableName, t );
		if ( scope != null && !scope.addVariable( result ) )
			throw new AdvancedScriptException( "Variable " + result.getName() + " is already defined " + getLineAndFile() );

		readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( currentToken().equals( "=" ) )
				throw new AdvancedScriptException( "Cannot initialize parameter " + result.getName() + " " + getLineAndFile() );
			return result;
		}

		// Otherwise, we must initialize the variable.

		ScriptVariableReference lhs = new ScriptVariableReference( result.getName(), scope );
		ScriptExpression rhs;

		if ( currentToken().equals( "=" ) )
		{
			readToken(); // Eat the equals sign
			rhs = parseExpression( scope );
		}
		else
			rhs = t.initialValueExpression();

		scope.addCommand( new ScriptAssignment( lhs, rhs ) );
		return result;
	}

	private boolean parseTypedef( ScriptScope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "typedef" ) )
			return false;
		readToken();	// read typedef

		ScriptType t = parseType( parentScope, true, true );
		if ( t == null )
			throw new AdvancedScriptException( "Missing data type for typedef " + getLineAndFile() );

		String typeName = currentToken();

		if ( !parseIdentifier( typeName ) )
			throw new AdvancedScriptException( "Invalid type name '" + typeName + "' " + getLineAndFile() );

		if ( isReservedWord( typeName ) )
			throw new AdvancedScriptException( "Reserved word '" + typeName + "' cannot be a type name " + getLineAndFile() );

		if ( parentScope.findType( typeName ) != null )
			throw new AdvancedScriptException( "Type name '" + typeName + "' is already defined " + getLineAndFile() );

		readToken(); // read name

		// Add the type to the type table
		ScriptNamedType type = new ScriptNamedType( typeName, t );
		parentScope.addType( type );

		return true;
	}

	private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop )
	{
		ScriptCommand result;

		if ( currentToken() == null )
			return null;

		if ( currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "Encountered 'break' outside of loop " + getLineAndFile() );

			result = new ScriptFlowControl( COMMAND_BREAK );
			readToken(); //break
		}

		else if ( currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "Encountered 'continue' outside of loop " + getLineAndFile() );

			result = new ScriptFlowControl( COMMAND_CONTINUE );
			readToken(); //continue
		}

		else if ( currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptFlowControl( COMMAND_EXIT );
			readToken(); //exit
		}


		else if ( (result = parseReturn( functionType, scope )) != null )
			;

		else if ( (result = parseBasicScript()) != null )
			// basic_script doesn't have a ; token
			return result;

		else if ( (result = parseWhile( functionType, scope )) != null )
			// while doesn't have a ; token
			return result;

		else if ( (result = parseForeach( functionType, scope )) != null )
			// foreach doesn't have a ; token
			return result;

		else if ( (result = parseFor( functionType, scope )) != null )
			// for doesn't have a ; token
			return result;

		else if ( (result = parseRepeat( functionType, scope )) != null )
			;

		else if ( (result = parseConditional( functionType, scope, noElse, whileLoop )) != null )
			// loop doesn't have a ; token
			return result;

		else if ( (result = parseCall( scope )) != null )
			;

		else if ( (result = parseAssignment( scope )) != null )
			;

		else if ( (result = parseRemove( scope )) != null )
			;

		else if ( (result = parseValue( scope )) != null )
			;

		else
			return null;

		if ( currentToken() == null || !currentToken().equals( ";" ) )
			parseError( ";", currentToken() );

		readToken(); // ;
		return result;
	}

	private ScriptType parseType( ScriptScope scope, boolean aggregates, boolean records )
	{
		if ( currentToken() == null )
			return null;

		ScriptType valType = scope.findType( currentToken() );
		if ( valType == null )
		{
			if ( records && currentToken().equalsIgnoreCase( "record" ) )
			{
				valType = parseRecord( scope );

				if ( valType == null )
					return null;

				if ( aggregates && currentToken().equals( "[" ) )
					return parseAggregateType( valType, scope );

				return valType;
			}

			return null;
		}

		readToken();

		if ( aggregates && currentToken().equals( "[" ) )
			return parseAggregateType( valType, scope );

		return valType;
	}

	private ScriptType parseAggregateType( ScriptType dataType, ScriptScope scope )
	{
		readToken();	// [ or ,
		if ( currentToken() == null )
			throw new AdvancedScriptException( "Missing index token " + getLineAndFile() );

		if ( arrays && readIntegerToken( currentToken() ) )
		{
			int size = parseInt( currentToken() );
			readToken(); // integer

			if ( currentToken() == null )
				parseError( "]", currentToken() );

			if ( currentToken().equals( "]" ) )
			{
				readToken();	// ]

				if ( currentToken().equals( "[" ) )
					return new ScriptAggregateType( parseAggregateType( dataType, scope ) , size );

				return new ScriptAggregateType( dataType, size );
			}

			if ( currentToken().equals( "," ) )
				return new ScriptAggregateType( parseAggregateType( dataType, scope ) , size );

			parseError( "]", currentToken() );
		}

		ScriptType indexType = scope.findType( currentToken() );
		if ( indexType == null )
			throw new AdvancedScriptException( "Invalid type name '" + currentToken() + "' " + getLineAndFile() );

		if ( !indexType.isPrimitive() )
			throw new AdvancedScriptException( "Index type '" + currentToken() + "' is not a primitive type " + getLineAndFile() );

		readToken();	// type name
		if ( currentToken() == null )
			parseError( "]", currentToken() );

		if ( currentToken().equals( "]" ) )
		{
			readToken();	// ]

			if ( currentToken().equals( "[" ) )
				return new ScriptAggregateType( parseAggregateType( dataType, scope ) , indexType );

			return new ScriptAggregateType( dataType, indexType );
		}

		if ( currentToken().equals( "," ) )
			return new ScriptAggregateType( parseAggregateType( dataType, scope ) , indexType );

		parseError( ", or ]", currentToken() );
		return null;
	}

	private boolean parseIdentifier( String identifier )
	{
		if ( !Character.isLetter( identifier.charAt( 0 ) ) && (identifier.charAt( 0 ) != '_' ) )
			return false;

		for ( int i = 1; i < identifier.length(); ++i )
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && (identifier.charAt( i ) != '_' ) )
				return false;

		return true;
	}

	private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope )
	{
		ScriptExpression expression = null;

		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "return" ) )
			return null;

		readToken(); //return

		if ( currentToken() != null && currentToken().equals( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( TYPE_VOID ) )
				return new ScriptReturn( null, VOID_TYPE );

			throw new AdvancedScriptException( "Return needs value " + getLineAndFile() );
		}
		else
		{
			if ( (expression = parseExpression( parentScope )) == null )
				throw new AdvancedScriptException( "Expression expected " + getLineAndFile() );

			return new ScriptReturn( expression, expectedType );
		}
	}

	private ScriptConditional parseConditional( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "if" ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			parseError( "(", nextToken() );

		readToken(); // if
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			parseError( ")", currentToken() );

		readToken(); // )

		ScriptIf result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			ScriptScope scope;

			if ( currentToken() == null || !currentToken().equals( "{" ) ) //Scope is a single call
			{
				ScriptCommand command = parseCommand( functionType, parentScope, !elseFound, loop );
				scope = new ScriptScope( command, parentScope );
			}
			else
			{
				readToken(); //read {
				scope = parseScope( null, functionType, null, parentScope, loop );

				if ( currentToken() == null || !currentToken().equals( "}" ) )
					parseError( "}", currentToken() );

				readToken(); //read }
			}

			if ( result == null )
				result = new ScriptIf( scope, expression );
			else if ( finalElse )
				result.addElseLoop( new ScriptElse( scope, expression ) );
			else
				result.addElseLoop( new ScriptElseIf( scope, expression ) );

			if ( !noElse && currentToken() != null && currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
					throw new AdvancedScriptException( "Else without if " + getLineAndFile() );

				if ( nextToken() != null && nextToken().equalsIgnoreCase( "if" ) )
				{
					readToken(); //else
					readToken(); //if

					if ( currentToken() == null || !currentToken().equals( "(" ) )
						parseError( "(", currentToken() );

					readToken(); //(
					expression = parseExpression( parentScope );

					if ( currentToken() == null || !currentToken().equals( ")" ) )
						parseError( ")", currentToken() );

					readToken(); // )
				}
				else //else without condition
				{
					readToken(); //else
					expression = TRUE_VALUE;
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
		if ( currentToken() == null )
			return null;

		if ( !currentToken().equalsIgnoreCase( "cli_execute" ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "{" ) )
			return null;

		readToken(); // while
		readToken(); // {

		ByteArrayStream ostream = new ByteArrayStream();

		while ( currentToken() != null && !currentToken().equals( "}" ) )
		{
			try
			{
				ostream.write( currentLine.getBytes() );
				ostream.write( LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				printStackTrace( e );
			}

			currentLine = "";
			fixLines();
		}

		if ( currentToken() == null )
			parseError( "}", currentToken() );

		readToken(); // }

		return new ScriptBasicScript( ostream );
	}

	private ScriptWhile parseWhile( ScriptType functionType, ScriptScope parentScope )
	{
		if ( currentToken() == null )
			return null;

		if ( !currentToken().equalsIgnoreCase( "while" ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			parseError( "(", nextToken() );

		readToken(); // while
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			parseError( ")", currentToken() );

		readToken(); // )

		ScriptScope scope = parseLoopScope( functionType, null, parentScope );

		return new ScriptWhile( scope, expression );
	}

	private ScriptRepeat parseRepeat( ScriptType functionType, ScriptScope parentScope )
	{
		if ( currentToken() == null )
			return null;

		if ( !currentToken().equalsIgnoreCase( "repeat" ) )
			return null;

		readToken(); // repeat

		ScriptScope scope = parseLoopScope( functionType, null, parentScope );
		if ( currentToken() == null || !currentToken().equals( "until" ) )
			parseError( "until", currentToken() );

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			parseError( "(", nextToken() );

		readToken(); // until
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			parseError( ")", currentToken() );

		readToken(); // )

		return new ScriptRepeat( scope, expression );
	}

	private ScriptForeach parseForeach( ScriptType functionType, ScriptScope parentScope )
	{
		// foreach key [, key ... ] in aggregate { scope }

		if ( currentToken() == null )
			return null;

		if ( !(currentToken().equalsIgnoreCase( "foreach" ) ) )
			return null;

		readToken();	// foreach

		ArrayList names = new ArrayList();

		while ( true )
		{
			String name = currentToken();

			if ( !parseIdentifier( name ) )
				throw new AdvancedScriptException( "Key variable name expected " + getLineAndFile() );

			if ( parentScope.findVariable( name ) != null )
				throw new AdvancedScriptException( "Key variable '" + name + "' is already defined " + getLineAndFile() );

			names.add( name );
			readToken();	// name

			if ( currentToken() != null )
			{
				if ( currentToken().equals( "," ) )
				{
					readToken();	// ,
					continue;
				}

				if ( currentToken().equalsIgnoreCase( "in" ) )
				{
					readToken();	// in
					break;
				}
			}

			parseError( "in", currentToken() );
		}

		// Get an aggregate reference
		ScriptExpression aggregate = parseVariableReference( parentScope );

		if ( aggregate == null || !(aggregate instanceof ScriptVariableReference) || !(aggregate.getType().getBaseType() instanceof ScriptAggregateType) )
			throw new AdvancedScriptException( "Aggregate reference expected " + getLineAndFile() );

		// Define key variables of appropriate type
		ScriptVariableList varList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();
		ScriptType type = aggregate.getType().getBaseType();

		for ( int i = 0; i < names.size(); ++i )
		{
			if ( !( type instanceof ScriptAggregateType ) )
				throw new AdvancedScriptException( "Too many key variables specified " + getLineAndFile() );

			ScriptType itype = ((ScriptAggregateType)type).getIndexType();
			ScriptVariable keyvar = new ScriptVariable( (String)names.get( i ), itype );
			varList.addElement( keyvar );
			variableReferences.addElement( new ScriptVariableReference( keyvar ) );
			type = ((ScriptAggregateType)type).getDataType();
		}

		// Parse the scope with the list of keyVars
		ScriptScope scope = parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ScriptForeach( scope, variableReferences, (ScriptVariableReference) aggregate );
	}

	private ScriptFor parseFor( ScriptType functionType, ScriptScope parentScope )
	{
		// foreach key in aggregate {scope }

		if ( currentToken() == null )
			return null;

		if ( !(currentToken().equalsIgnoreCase( "for" ) ) )
			return null;

		String name = nextToken();

		if ( !parseIdentifier( name ) )
			return null;

		if ( parentScope.findVariable( name ) != null )
			throw new AdvancedScriptException( "Index variable '" + name + "' is already defined " + getLineAndFile() );

		readToken();	// for
		readToken();	// name

		if ( !(currentToken().equalsIgnoreCase( "from" ) ) )
			parseError( "from", currentToken() );

		readToken();	// from

		ScriptExpression initial = parseExpression( parentScope );

		int direction = 0;

		if ( currentToken().equalsIgnoreCase( "upto" ) )
			direction = 1;
		else if ( currentToken().equalsIgnoreCase( "downto" ) )
			direction = -1;
		else if ( currentToken().equalsIgnoreCase( "to" ) )
			direction = 0;
		else
			parseError( "to, upto, or downto", currentToken() );

		readToken();	// upto/downto

		ScriptExpression last = parseExpression( parentScope );

		ScriptExpression increment = ONE_VALUE;
		if ( currentToken().equalsIgnoreCase( "by" ) )
		{
			readToken();	// by
			increment = parseExpression( parentScope );
		}

		// Create integer index variable
		ScriptVariable indexvar = new ScriptVariable( name, INT_TYPE );

		// Put index variable onto a list
		ScriptVariableList varList = new ScriptVariableList();
		varList.addElement( indexvar );

		ScriptScope scope = parseLoopScope( functionType, varList, parentScope );

		return new ScriptFor( scope, new ScriptVariableReference( indexvar ), initial, last, increment, direction );
	}

	private ScriptScope parseLoopScope( ScriptType functionType, ScriptVariableList varList, ScriptScope parentScope )
	{
		ScriptScope scope;

		if ( currentToken() != null && currentToken().equals( "{" ) )
		{
			// Scope is a block

			readToken(); // {

			scope = parseScope( null, functionType, varList, parentScope, true );
			if ( currentToken() == null || !currentToken().equals( "}" ) )
				parseError( "}", currentToken() );

			readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new ScriptScope( varList, parentScope );
			scope.addCommand( parseCommand( functionType, scope, false, true ) );
		}

		return scope;
	}

	private ScriptExpression parseCall( ScriptScope scope )
	{	return parseCall( scope, null );
	}

	private ScriptExpression parseCall( ScriptScope scope, ScriptExpression firstParam )
	{
		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();

		readToken(); //name
		readToken(); //(

		ScriptExpressionList params = new ScriptExpressionList();
		if ( firstParam != null )
			params.addElement( firstParam );

		while ( currentToken() != null && !currentToken().equals( ")" ) )
		{
			ScriptExpression val = parseExpression( scope );
			if ( val != null )
				params.addElement( val );

			if ( !currentToken().equals( "," ) )
			{
				if ( !currentToken().equals( ")" ) )
					parseError( ")", currentToken() );
			}
			else
			{
				readToken();
				if ( currentToken().equals( ")" ) )
					parseError( "parameter", currentToken() );
			}
		}

		if ( !currentToken().equals( ")" ) )
			parseError( ")", currentToken() );

		readToken(); // )

		ScriptExpression result = new ScriptCall( name, scope, params );

		ScriptVariable current;
		while ( result != null && currentToken() != null && currentToken().equals( "." ) )
		{
			current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptCommand parseAssignment( ScriptScope scope )
	{
		if ( nextToken() == null )
			return null;

		if ( !nextToken().equals( "=" ) && !nextToken().equals( "[" ) && !nextToken().equals( "." ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		ScriptExpression lhs = parseVariableReference( scope );
		if ( lhs instanceof ScriptCall )
			return lhs;

		if ( lhs == null || !(lhs instanceof ScriptVariableReference) )
			throw new AdvancedScriptException( "Variable reference expected " + getLineAndFile() );

		if ( !currentToken().equals( "=" ) )
			return null;

		readToken(); //=

		ScriptExpression rhs = parseExpression( scope );

		if ( rhs == null )
			throw new AdvancedScriptException( "Internal error " + getLineAndFile() );

		return new ScriptAssignment( (ScriptVariableReference) lhs, rhs );
	}

	private ScriptExpression parseRemove( ScriptScope scope )
	{
		if ( currentToken() == null || !currentToken().equals( "remove" ) )
			return null;

		ScriptExpression lhs = parseExpression( scope );

		if ( lhs == null )
			throw new AdvancedScriptException( "Bad 'remove' statement " + getLineAndFile() );

		return lhs;
	}

	private ScriptExpression parseExpression( ScriptScope scope )
	{
		return parseExpression( scope, null );
	}

	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper )
	{
		if ( currentToken() == null )
			return null;

		ScriptExpression lhs = null;
		ScriptExpression rhs = null;
		ScriptOperator oper = null;

		if ( currentToken().equals( "!" ) )
		{
			String operator = currentToken();
			readToken(); // !
			if ( (lhs = parseValue( scope )) == null )
				throw new AdvancedScriptException( "Value expected " + getLineAndFile() );

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else if ( currentToken().equals( "-" ) )
		{
			// See if it's a negative numeric constant
			if ( (lhs = parseValue( scope )) != null )
				return lhs;

			// Nope. Must be unary minus.
			String operator = currentToken();
			readToken(); // !
			if ( (lhs = parseValue( scope )) == null )
				throw new AdvancedScriptException( "Value expected " + getLineAndFile() );

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else if ( currentToken().equals( "remove" ) )
		{
			String operator = currentToken();
			readToken(); // remove

			lhs = parseVariableReference( scope );
			if ( lhs == null || !(lhs instanceof ScriptCompositeReference) )
				throw new AdvancedScriptException( "Aggregate reference expected " + getLineAndFile() );

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else
		{
			if ( (lhs = parseValue( scope )) == null )
				return null;
		}

		do
		{
			oper = parseOperator( currentToken() );

			if ( oper == null )
				return lhs;

			if ( previousOper != null && !oper.precedes( previousOper ) )
				return lhs;

			readToken(); //operator

			if ( (rhs = parseExpression( scope, oper )) == null )
				throw new AdvancedScriptException( "Value expected " + getLineAndFile() );

			if ( !validCoercion( lhs.getType(), rhs.getType(), oper.toString() ) )
			{
				throw new AdvancedScriptException( "Cannot apply operator " + oper + " to " +
					lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ") " + getLineAndFile() );
			}

			lhs = new ScriptExpression( lhs, rhs, oper );
		}
		while ( true );
	}

	private ScriptExpression parseValue( ScriptScope scope )
	{
		if ( currentToken() == null )
			return null;

		ScriptExpression result = null;

		// Parse parenthesized expressions
		if ( currentToken().equals( "(" ) )
		{
			readToken();	// (

			result = parseExpression( scope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
				parseError( ")", currentToken() );

			readToken();    // )
		}

		// Parse constant values
		// true and false are reserved words

		else if ( currentToken().equalsIgnoreCase( "true" ) )
		{
			readToken();
			result = TRUE_VALUE;
		}

		else if ( currentToken().equalsIgnoreCase( "false" ) )
		{
			readToken();
			result = FALSE_VALUE;
		}

		// numbers
		else if ( (result = parseNumber()) != null )
			;

		// strings
		else if ( currentToken().equals( "\"" ) || currentToken().equals( "\'" ) )
			result = parseString();

		// typed constants
		else if ( currentToken().equals( "$" ) )
			result = parseTypedConstant( scope );

		// Function calls
		else if ( (result = parseCall( scope, result )) != null )
			;

		// Variable and aggregate references
		else if ( (result = parseVariableReference( scope )) != null )
			;

		ScriptVariable current;
		while ( result != null && currentToken() != null && currentToken().equals( "." ) )
		{
			current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptValue parseNumber()
	{
		if ( currentToken() == null )
			return null;

		int sign = 1;

		if ( currentToken().equals( "-" ) )
		{
			String next = nextToken();

			if ( next == null )
				return null;

			if ( !next.equals( "." ) && !readIntegerToken( next ) )
				// Unary minus
				return null;

			sign = -1;
			readToken();	// Read -
		}

		if ( currentToken().equals( "." ) )
		{
			readToken();
			String fraction = currentToken();

			if ( !readIntegerToken( fraction ) )
				parseError( "numeric value", fraction );

			readToken();	// integer
			return new ScriptValue( sign * parseFloat( "0." + fraction ) );
		}

		String integer = currentToken();
		if ( !readIntegerToken( integer ) )
			return null;

		readToken();	// integer

		if ( currentToken().equals( "." ) )
		{
			String fraction = nextToken();
			if ( !readIntegerToken( fraction ) )
				return new ScriptValue( sign * parseInt( integer ) );

			readToken();	// .
			readToken();	// fraction

			return new ScriptValue( sign * parseFloat( integer + "." + fraction ) );
		}

		return new ScriptValue( sign * parseInt( integer ) );
	}

	private boolean readIntegerToken( String token )
	{
		if ( token == null )
			return false;

		for ( int i = 0; i < token.length(); ++i )
			if ( !Character.isDigit( token.charAt( i ) ) )
				return false;

		return true;
	}

	private ScriptValue parseString()
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		StringBuffer resultString = new StringBuffer();
		char startCharacter = currentLine.charAt(0);

		for ( int i = 1; ; ++i )
		{
			if ( i == currentLine.length() )
				throw new AdvancedScriptException( "No closing \" found " + getLineAndFile() );

			if ( currentLine.charAt( i ) == '\\' )
			{
				char ch = currentLine.charAt( ++i );

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
					BigInteger hex08 = new BigInteger( currentLine.substring( i + 1, i + 3 ), 16 );
					resultString.append( (char) hex08.intValue() );
					i += 2;
					break;

				case 'u':
					BigInteger hex16 = new BigInteger( currentLine.substring( i + 1, i + 5 ), 16 );
					resultString.append( (char) hex16.intValue() );
					i += 4;
					break;

				default:
					if ( Character.isDigit( ch ) )
					{
						BigInteger octal = new BigInteger( currentLine.substring( i, i + 3 ), 8 );
						resultString.append( (char) octal.intValue() );
						i += 2;
					}
				}
			}
			else if ( currentLine.charAt( i ) == startCharacter )
			{
				currentLine = currentLine.substring( i + 1 ); //+ 1 to get rid of '"' token
				return new ScriptValue( resultString.toString() );
			}
			else
			{
				resultString.append( currentLine.charAt( i ) );
			}
		}
	}

	private ScriptValue parseTypedConstant( ScriptScope scope )
	{
		readToken();    // read $

		String name = currentToken();
		ScriptType type = parseType( scope, false, false );
		if ( type == null || !type.isPrimitive() )
			throw new AdvancedScriptException( "Unknown type " + name + " " + getLineAndFile() );

		if ( !currentToken().equals( "[" ) )
			parseError( "[", currentToken() );

		StringBuffer resultString = new StringBuffer();

		for ( int i = 1; ; ++i )
		{
			if ( i == currentLine.length() )
			{
				throw new AdvancedScriptException( "No closing ] found " + getLineAndFile() );
			}
			else if ( currentLine.charAt( i ) == '\\' )
			{
				resultString.append( currentLine.charAt( ++i ) );
			}
			else if ( currentLine.charAt( i ) == ']' )
			{
				currentLine = currentLine.substring( i + 1 ); //+1 to get rid of ']' token
				return parseValue( type, resultString.toString().trim());
			}
			else
			{
				resultString.append( currentLine.charAt( i ) );
			}
		}
	}

	private ScriptOperator parseOperator( String oper )
	{
		if ( oper == null || !isOperator( oper ) )
			return null;

		return new ScriptOperator( oper );
	}

	private boolean isOperator( String oper )
	{
		return oper.equals( "!" ) ||
			oper.equals( "*" ) || oper.equals( "^" ) || oper.equals( "/" ) || oper.equals( "%" ) ||
			oper.equals( "+" ) || oper.equals( "-" ) ||
			oper.equals( "<" ) || oper.equals( ">" ) || oper.equals( "<=" ) || oper.equals( ">=" ) ||
			oper.equals( "=" ) || oper.equals( "==" ) || oper.equals( "!=" ) ||
			oper.equals( "||" ) || oper.equals( "&&" ) ||
			oper.equals( "contains" ) || oper.equals( "remove" );
	}

	private ScriptExpression parseVariableReference( ScriptScope scope )
	{
		if ( currentToken() == null || !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();
		ScriptVariable var = scope.findVariable( name, true );

		if ( var == null )
			throw new AdvancedScriptException( "Unknown variable '" + name + "' " + getLineAndFile() );

		readToken(); // read name

		if ( currentToken() == null || (!currentToken().equals( "[" ) && !currentToken().equals( "." ) ) )
			return new ScriptVariableReference( var );

		return parseVariableReference( scope, var );
	}

	private ScriptExpression parseVariableReference( ScriptScope scope, ScriptVariable var )
	{
		ScriptType type = var.getType();
		ScriptExpressionList indices = new ScriptExpressionList();

		boolean parseAggregate = currentToken().equals( "[" );

		while ( currentToken() != null && (currentToken().equals( "[" ) || currentToken().equals( "." ) || (parseAggregate && currentToken().equals( "," ))) )
		{
			ScriptExpression index;

			type = type.getBaseType();

			if ( currentToken().equals( "[" ) || currentToken().equals( "," ) )
			{
				readToken(); // read [ or . or ,
				parseAggregate = true;

				if ( !(type instanceof ScriptAggregateType) )
				{
					if ( indices.isEmpty() )
						throw new AdvancedScriptException( "Variable '" + var.getName() + "' cannot be indexed " + getLineAndFile() );
					else
						throw new AdvancedScriptException( "Too many keys " + getLineAndFile() );
				}

				ScriptAggregateType atype = (ScriptAggregateType) type;
				index = parseExpression( scope );
				if ( index == null )
					throw new AdvancedScriptException( "Index expression expected " + getLineAndFile() );

				if ( !index.getType().equals( atype.getIndexType() ) )
					throw new AdvancedScriptException( "Index has wrong data type " + getLineAndFile() );
				type = atype.getDataType();
			}
			else
			{
				readToken(); // read [ or . or ,

				// Maybe it's a function call with an implied "this" parameter.

				if ( nextToken().equals( "(" ) )
					return parseCall( scope, indices.isEmpty() ? new ScriptVariableReference( var ) : new ScriptCompositeReference( var, indices ) );

				if ( !(type instanceof ScriptRecordType) )
					throw new AdvancedScriptException( "Record expected " + getLineAndFile() );

				ScriptRecordType rtype = (ScriptRecordType)type;

				String field = currentToken();
				if ( field == null || !parseIdentifier( field ) )
					throw new AdvancedScriptException( "Field name expected " + getLineAndFile() );

				index = rtype.getFieldIndex( field );
				if ( index == null )
					throw new AdvancedScriptException( "Invalid field name " + getLineAndFile() );
				readToken(); // read name
				type = rtype.getDataType( index );
			}

			indices.addElement( index );

			if ( parseAggregate && currentToken() != null )
			{
				if ( currentToken().equals( "]" ) )
				{
					readToken(); // read ]
					parseAggregate = false;
				}
			}
		}

		if ( parseAggregate )
			parseError( currentToken(), "]" );

		return new ScriptCompositeReference( var, indices );
	}

	private String parseDirective( String directive )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( directive ) )
			return null;

		readToken(); //directive

		if ( currentToken() == null )
			parseError( "<", currentToken() );

		int startIndex = currentLine.indexOf( "<" );
		int endIndex = currentLine.indexOf( ">" );

		if ( startIndex != -1 && endIndex == -1 )
			throw new AdvancedScriptException( "No closing > found " + getLineAndFile() );

		if ( startIndex == -1 )
		{
			startIndex = currentLine.indexOf( "\"" );
			endIndex = currentLine.indexOf( "\"", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
				throw new AdvancedScriptException( "No closing \" found " + getLineAndFile() );
		}

		if ( startIndex == -1 )
		{
			startIndex = currentLine.indexOf( "\'" );
			endIndex = currentLine.indexOf( "\'", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
				throw new AdvancedScriptException( "No closing \' found " + getLineAndFile() );
		}

		if ( endIndex == -1 )
		{
			endIndex = currentLine.indexOf( ";" );
			if ( endIndex == -1 )
				endIndex = currentLine.length();
		}

		String resultString = currentLine.substring( startIndex + 1, endIndex );
		currentLine = currentLine.substring( endIndex );

		if ( currentToken().equals( ">" ) || currentToken().equals( "\"" ) || currentToken().equals( "\'" ) )
			readToken(); //get rid of '>' or '"' token

		if ( currentToken().equals( ";" ) )
			readToken(); //read ;

		return resultString;
	}

	private void parseNotify()
	{
		String resultString = parseDirective( "notify" );
		if ( notifyRecipient == null )
			notifyRecipient = resultString;
	}

	private String parseImport()
	{	return parseDirective( "import" );
	}

	private static boolean validCoercion( ScriptType lhs, ScriptType rhs, String oper )
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
			return lhs.getType() == TYPE_AGGREGATE &&
				((ScriptAggregateType)lhs).getIndexType().equals( rhs );
		}

		// If the types are equal, no coercion is necessary
		if ( lhs.equals( rhs ) )
			return true;

		if ( lhs.equals( TYPE_ANY ) && rhs.getType() != TYPE_AGGREGATE )
			return true;

		// Anything coerces to a string as a parameter
		if  ( oper.equals( "parameter" ) && lhs.equals( TYPE_STRING ) )
			return true;

		// Anything coerces to a string for concatenation
		if ( oper.equals( "+" ) && ( lhs.equals( TYPE_STRING ) || rhs.equals( TYPE_STRING ) ) )
			return true;

		// Int coerces to float
		if ( lhs.equals( TYPE_INT ) && rhs.equals( TYPE_FLOAT ) )
		     return true;

		if ( lhs.equals( TYPE_FLOAT ) && rhs.equals( TYPE_INT ) )
		     return true;

		return false;
	}

	private String currentToken()
	{
		fixLines();
		if ( currentLine == null )
			return null;

		if ( !currentLine.trim().equals( "/*" ) )
			return currentLine.substring( 0, tokenLength( currentLine ) );

		while ( currentLine != null && !currentLine.trim().equals( "*/" ) )
		{
			currentLine = "";
			fixLines();
		}

		if ( currentLine == null )
			return null;

		currentLine = "";
		return currentToken();
	}

	private String nextToken()
	{
		fixLines();

		if ( currentLine == null )
			return null;

		if ( tokenLength( currentLine ) >= currentLine.length() )
		{
			if ( nextLine == null )
				return null;

			return nextLine.substring( 0, tokenLength( nextLine ) ).trim();
		}

		String result = currentLine.substring( tokenLength( currentLine ) ).trim();

		if ( result.equals( "" ) )
		{
			if ( nextLine == null )
				return null;

			return nextLine.substring( 0, tokenLength( nextLine ) );
		}

		return result.substring( 0, tokenLength( result ) );
	}

	private void readToken()
	{
		fixLines();

		if ( currentLine == null )
			return;

		currentLine = currentLine.substring( tokenLength( currentLine ) );
	}

	private int tokenLength( String s )
	{
		int result;
		if ( s == null )
			return 0;

		for ( result = 0; result < s.length(); result++ )
		{
			if ( result + 1 < s.length() && tokenString( s.substring( result, result + 2 ) ) )
				return result == 0 ? 2 : result;

			if ( result < s.length() && tokenString( s.substring( result, result + 1 ) ) )
				return result == 0 ? 1 : result;
		}

		return result; //== s.length()
	}

	private void fixLines()
	{
		if ( currentLine == null )
			return;

		while ( currentLine.equals( "" ) )
		{
			currentLine = nextLine;
			lineNumber = commandStream.getLineNumber();
			nextLine = getNextLine();

			if ( currentLine == null )
				return;
		}

		currentLine = currentLine.trim();

		if ( nextLine == null )
			return;

		while ( nextLine.equals( "" ) )
		{
			nextLine = getNextLine();
			if ( nextLine == null )
				return;
		}

		nextLine = nextLine.trim();
	}

	private boolean tokenString( String s )
	{
		if ( s.length() == 1 )
		{
			for ( int i = 0; i < tokenList.length; ++i )
				if ( s.charAt( 0 ) == tokenList[i] )
					return true;
			return false;
		}
		else
		{
			for ( int i = 0; i < multiCharTokenList.length; ++i )
				if ( s.equalsIgnoreCase( multiCharTokenList[i] ) )
					return true;

			return false;
		}
	}

	// **************** Debug printing *****************

	private void printScope( ScriptScope scope, int indent )
	{
		if ( scope == null )
			return;

		Iterator it;

		indentLine( indent );
		RequestLogger.updateDebugLog( "<SCOPE>" );

		indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<TYPES>" );

		it = scope.getTypes();
		ScriptType currentType;

		while ( it.hasNext() )
		{
			currentType = (ScriptType) it.next();
			printType( currentType, indent + 2 );
		}

		indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<VARIABLES>" );

		it = scope.getVariables();
		ScriptVariable currentVar;

		while ( it.hasNext() )
		{
			currentVar = (ScriptVariable) it.next();
			printVariable( currentVar, indent + 2 );
		}

		indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<FUNCTIONS>" );

		it = scope.getFunctions();
		ScriptFunction currentFunc;

		while ( it.hasNext() )
		{
			currentFunc = (ScriptFunction) it.next();
			printFunction( currentFunc, indent + 2 );
		}

		indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<COMMANDS>" );

		it = scope.getCommands();
		ScriptCommand currentCommand;
		while ( it.hasNext() )
		{
			currentCommand = (ScriptCommand) it.next();
			printCommand( currentCommand, indent + 2 );
		}
	}

	public void showUserFunctions( String filter )
	{	showFunctions( global.getFunctions(), filter.toLowerCase() );
	}

	public void showExistingFunctions( String filter )
	{	showFunctions( existingFunctions.iterator(), filter.toLowerCase() );
	}

	private void showFunctions( Iterator it, String filter )
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
			hasDescription = func instanceof ScriptExistingFunction && ((ScriptExistingFunction)func).getDescription() != null;

			if ( !filter.equals( "" ) && func.getName().toLowerCase().indexOf( filter ) == -1 )
				continue;

			StringBuffer description = new StringBuffer();

			if ( hasDescription )
				description.append( "<b>" );

			description.append( func.getType() );
			description.append( " " );
			description.append( func.getName() );
			description.append( "( " );

			Iterator it2 = func.getReferences();
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
					description.append( ", " );
			}

			description.append( " )" );

			if ( hasDescription )
			{
				description.append( "</b><br>" );
				description.append( ((ScriptExistingFunction)func).getDescription() );
				description.append( "<br>" );
			}


			RequestLogger.printLine( description.toString() );

		}
	}

	private void printType( ScriptType type, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<TYPE " + type + ">" );
	}

	private void printVariable( ScriptVariable var, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<VAR " + var.getType() + " " + var.getName() + ">" );
	}

	private void printFunction( ScriptFunction func, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<FUNC " + func.getType() + " " + func.getName() + ">" );

		Iterator it = func.getReferences();
		ScriptVariableReference current;

		while ( it.hasNext() )
		{
			current = (ScriptVariableReference) it.next();
			printVariableReference( current, indent + 1 );
		}

		if ( func instanceof ScriptUserDefinedFunction )
			printScope( ((ScriptUserDefinedFunction)func).getScope(), indent + 1 );
	}

	private void printCommand( ScriptCommand command, int indent )
	{
		if ( command instanceof ScriptReturn )
			printReturn( ( ScriptReturn ) command, indent );
		else if ( command instanceof ScriptConditional )
			printConditional( ( ScriptConditional ) command, indent );
		else if ( command instanceof ScriptWhile )
			printWhile( ( ScriptWhile ) command, indent );
		else if ( command instanceof ScriptRepeat )
			printRepeat( ( ScriptRepeat ) command, indent );
		else if ( command instanceof ScriptForeach )
			printForeach( ( ScriptForeach ) command, indent );
		else if ( command instanceof ScriptFor )
			printFor( ( ScriptFor ) command, indent );
		else if ( command instanceof ScriptCall )
			printCall( ( ScriptCall ) command, indent );
		else if ( command instanceof ScriptAssignment )
			printAssignment( ( ScriptAssignment ) command, indent );
		else
		{
			indentLine( indent );
			RequestLogger.updateDebugLog( "<COMMAND " + command + ">" );
		}
	}

	private void printReturn( ScriptReturn ret, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<RETURN " + ret.getType() + ">" );
		if ( !ret.getType().equals( TYPE_VOID ) )
			printExpression( ret.getExpression(), indent + 1 );
	}

	private void printConditional( ScriptConditional command, int indent )
	{
		indentLine( indent );
		if ( command instanceof ScriptIf )
		{
			ScriptIf loop = (ScriptIf)command;
			RequestLogger.updateDebugLog( "<IF>" );

			printExpression( loop.getCondition(), indent + 1 );
			printScope( loop.getScope(), indent + 1 );

			Iterator it = loop.getElseLoops();
			ScriptConditional currentElse;

			while ( it.hasNext() )
			{
				currentElse = (ScriptConditional) it.next();
				printConditional( currentElse, indent );
			}
		}
		else if ( command instanceof ScriptElseIf )
		{
			ScriptElseIf loop = (ScriptElseIf)command;
			RequestLogger.updateDebugLog( "<ELSE IF>" );
			printExpression( loop.getCondition(), indent + 1 );
			printScope( loop.getScope(), indent + 1 );
		}
		else if (command instanceof ScriptElse )
		{
			ScriptElse loop = (ScriptElse)command;
			RequestLogger.updateDebugLog( "<ELSE>" );
			printScope( loop.getScope(), indent + 1 );
		}
	}

	private void printWhile( ScriptWhile loop, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<WHILE>" );
		printExpression( loop.getCondition(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printRepeat( ScriptRepeat loop, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<REPEAT>" );
		printScope( loop.getScope(), indent + 1 );
		printExpression( loop.getCondition(), indent + 1 );
	}

	private void printForeach( ScriptForeach loop, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<FOREACH>" );

		Iterator it = loop.getReferences();
		ScriptVariableReference current;

		while ( it.hasNext() )
		{
			current = (ScriptVariableReference) it.next();
			printVariableReference( current, indent + 1 );
		}

		printVariableReference( loop.getAggregate(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printFor( ScriptFor loop, int indent )
	{
		indentLine( indent );
		int direction = loop.getDirection();
		RequestLogger.updateDebugLog( "<FOR " + ( direction < 0 ? "downto" : direction > 0 ? "upto" : "to" ) + " >" );
		printVariableReference( loop.getVariable(), indent + 1 );
		printExpression( loop.getInitial(), indent + 1 );
		printExpression( loop.getLast(), indent + 1 );
		printExpression( loop.getIncrement(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printCall( ScriptCall call, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<CALL " + call.getTarget().getName() + ">" );

		Iterator it = call.getExpressions();
		ScriptExpression current;

		while ( it.hasNext() )
		{
			current = (ScriptExpression) it.next();
			printExpression( current, indent + 1 );
		}
	}

	private void printAssignment( ScriptAssignment assignment, int indent )
	{
		indentLine( indent );
		ScriptVariableReference lhs = assignment.getLeftHandSide();
		RequestLogger.updateDebugLog( "<ASSIGN " + lhs.getName() + ">" );
		printIndices( lhs.getIndices(), indent + 1 );
		printExpression( assignment.getRightHandSide(), indent + 1 );
	}

	private void printIndices( ScriptExpressionList indices, int indent )
	{
		if ( indices != null )
		{
			Iterator it = indices.iterator();
			ScriptExpression current;

			while ( it.hasNext() )
			{
				current = (ScriptExpression) it.next();

				indentLine( indent );
				RequestLogger.updateDebugLog( "<KEY>" );
				printExpression( current, indent + 1 );
			}
		}
	}

	private void printExpression( ScriptExpression expression, int indent )
	{
		if ( expression instanceof ScriptValue )
			printValue( (ScriptValue) expression, indent );
		else
		{
			printOperator( expression.getOperator(), indent );
			printExpression( expression.getLeftHandSide(), indent + 1 );
			if ( expression.getRightHandSide() != null ) // ! operator
				printExpression( expression.getRightHandSide(), indent + 1 );
		}
	}

	public void printValue( ScriptValue value, int indent )
	{
		if ( value instanceof ScriptVariableReference )
			printVariableReference( (ScriptVariableReference) value, indent );
		else if ( value instanceof ScriptCall )
			printCall( (ScriptCall) value, indent );
		else
		{
			indentLine( indent );
			RequestLogger.updateDebugLog( "<VALUE " + value.getType() + " [" + value + "]>" );
		}
	}

	public void printOperator( ScriptOperator oper, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<OPER " + oper + ">" );
	}

	public void printCompositeReference( ScriptCompositeReference varRef, int indent )
	{
		indentLine( indent );
		RequestLogger.updateDebugLog( "<AGGREF " + varRef.getName() + ">" );

		printIndices( varRef.getIndices(), indent + 1 );
	}

	public void printVariableReference( ScriptVariableReference varRef, int indent )
	{
		if ( varRef instanceof ScriptCompositeReference )
		{
			printCompositeReference( (ScriptCompositeReference)varRef, indent );
			return;
		}

		indentLine( indent );
		RequestLogger.updateDebugLog( "<VARREF> " + varRef.getName() );
	}

	private static void indentLine( int indent )
	{
		for ( int i = 0; i < indent; ++i )
			RequestLogger.getDebugStream().print( "   " );
	}

	// **************** Execution *****************

	private void captureValue( ScriptValue value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( KoLmafia.refusesContinue() || value == null )
		{
			// User aborted
			currentState = STATE_EXIT;
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		currentState = STATE_NORMAL;
		KoLmafia.forceContinue();
	}

	private ScriptValue executeScope( ScriptScope topScope, String functionName, String [] parameters )
	{
		ScriptFunction main;
		ScriptValue result = null;

		currentState = STATE_NORMAL;
		resetTracing();

		main = functionName.equals( "main" ) ? mainMethod : topScope.findFunction( functionName, null );

		if ( main == null && !topScope.getCommands().hasNext() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Unable to invoke " + functionName );
			return VOID_VALUE;
		}

		// First execute top-level commands;

		boolean executeTopLevel = this != NAMESPACE_INTERPRETER;

		if ( !executeTopLevel )
		{
			String importString = getProperty( "commandLineNamespace" );
			executeTopLevel = !importString.equals( lastImportString );
			lastImportString = importString;
		}

		if ( executeTopLevel )
		{
			trace( "Executing top-level commands" );
			result = topScope.execute();
		}

		if ( currentState == STATE_EXIT )
			return result;

		// Now execute main function, if any
		if ( main != null )
		{
			trace( "Executing main function" );

			if ( !requestUserParams( main, parameters ) )
				return null;

			result = main.execute();
		}

		return result;
	}

	private boolean requestUserParams( ScriptFunction targetFunction, String [] parameters )
	{
		int args = ( parameters == null ) ? 0 : parameters.length;

		ScriptType lastType = null;
		ScriptVariableReference lastParam = null;

		int index = 0;

		Iterator it = targetFunction.getReferences();
		ScriptVariableReference	param;

		while ( it.hasNext() )
		{
			param = (ScriptVariableReference) it.next();

			ScriptType type = param.getType();
			String name = param.getName();
			ScriptValue value = null;

			while ( value == null )
			{
				if ( type == VOID_TYPE )
				{
					value = VOID_VALUE;
					break;
				}

				String input = null;

				if ( index >= args )
					input = promptForValue( type, name );
				else
					input = parameters[ index ];

				// User declined to supply a parameter
				if ( input == null )
					return false;

				try
				{
					value = parseValue( type, input );
				}
				catch ( AdvancedScriptException e )
				{
					RequestLogger.printLine( e.getMessage() );

					// Punt if parameter came from the CLI
					if ( index < args )
						return false;
				}
			}

			param.setValue( value );

			lastType = type;
			lastParam = param;

			index++;
		}

		if ( index < args )
		{
			StringBuffer inputs = new StringBuffer();
			for ( int i = index - 1; i < args; ++i )
				inputs.append( parameters[i] + " " );

			ScriptValue value = parseValue( lastType, inputs.toString().trim() );
			lastParam.setValue( value );
		}

		return true;
	}

	private String promptForValue( ScriptType type, String name )
	{	return promptForValue( type, "Please input a value for " + type + " " + name, name );
	}

	private String promptForValue( ScriptType type, String message, String name )
	{
		switch ( type.getType() )
		{
		case TYPE_BOOLEAN:
			return (String) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, BOOLEANS, BOOLEANS[0] );

		case TYPE_LOCATION:
			return ((KoLAdventure) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, AdventureDatabase.getAsLockableListModel().toArray(),
				AdventureDatabase.getAdventure( getProperty( "lastAdventure" ) ) )).getAdventureName();

		case TYPE_SKILL:

			List castableSkills = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.CASTABLE );
			return ((UseSkillRequest) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, castableSkills.toArray(), castableSkills.get(0) )).getSkillName();

		case TYPE_FAMILIAR:
			return ((FamiliarData) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, KoLCharacter.getFamiliarList().toArray(), KoLCharacter.getFamiliar() )).getRace();

		case TYPE_SLOT:
			return (String) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, EquipmentRequest.slotNames, EquipmentRequest.slotNames[0] );

		case TYPE_ELEMENT:
			return (String) JOptionPane.showInputDialog( null, message, "Input Variable",
				JOptionPane.INFORMATION_MESSAGE, null, MonsterDatabase.elementNames, MonsterDatabase.elementNames[0] );

		case TYPE_CLASS:
			return (String) JOptionPane.showInputDialog( null, message, "Input Variable",
					JOptionPane.INFORMATION_MESSAGE, null, CLASSES, CLASSES[0] );

		case TYPE_STAT:
			return (String) JOptionPane.showInputDialog( null, message, "Input Variable",
					JOptionPane.INFORMATION_MESSAGE, null, STATS, STATS[0] );

		case TYPE_INT:
		case TYPE_FLOAT:
		case TYPE_STRING:
		case TYPE_ITEM:
		case TYPE_EFFECT:
		case TYPE_MONSTER:
			return JOptionPane.showInputDialog( message );

		default:
			throw new RuntimeException( "Internal error: Illegal type for main() parameter" );
		}
	}

	public void parseError( String expected, String actual )
	{	throw new AdvancedScriptException( "Expected " + expected + ", found " + actual + " " + getLineAndFile() );
	}

	public String getLineAndFile()
	{
		if ( fileName == null )
			return "(" + StaticEntity.getProperty( "commandLineNamespace" ) + ")";

		String partialName = fileName.substring( fileName.lastIndexOf( File.separator ) + 1 );
		return "(" + partialName + ", line " + lineNumber + ")";
	}

	public ScriptScope getExistingFunctionScope()
	{	return new ScriptScope( existingFunctions, null, simpleTypes );
	}

	public static ScriptFunctionList getExistingFunctions()
	{
		ScriptFunctionList result = new ScriptFunctionList();
		ScriptType [] params;


		// Basic utility functions which print information
		// or allow for easy testing.


		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "enable", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "disable", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "user_confirm", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print_html", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "abort", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "abort", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "cli_execute", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "load_html", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "visit_url", STRING_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "wait", VOID_TYPE, params ) );


		// Type conversion functions which allow conversion
		// of one data format to another.


		params = new ScriptType[] { ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_boolean", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_int", INT_TYPE, params ) );

		params = new ScriptType[] { ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_float", FLOAT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", ITEM_TYPE, params ) );
		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", ITEM_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", SKILL_TYPE, params ) );
		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", SKILL_TYPE, params ) );
		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", SKILL_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", EFFECT_TYPE, params ) );
		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", EFFECT_TYPE, params ) );
		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", EFFECT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_location", LOCATION_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", FAMILIAR_TYPE, params ) );
		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_monster", MONSTER_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "to_slot", SLOT_TYPE, params ) );

		params = new ScriptType[] { LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "to_url", STRING_TYPE, params ) );


		// Functions related to daily information which get
		// updated usually once per day.


		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "today_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_phase", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_light", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_today", STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", STAT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType( STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { STRING_TYPE, INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType( STRING_TYPE, 0 ), params ) );


		// Major functions related to adventuring and
		// item management.


		params = new ScriptType[] { INT_TYPE, LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "adventure", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "add_item_condition", VOID_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "buy", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "create", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "use", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "eat", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "drink", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_shop", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_display", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_storage", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_display", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "autosell", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "hermit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "retrieve_item", BOOLEAN_TYPE, params ) );


		// Major functions which provide item-related
		// information.


		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "daily_special", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "available_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "closet_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "creatable_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "storage_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "display_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "shop_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "stash_amount", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "pulls_remaining", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stills_available", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_mushroom_plot", BOOLEAN_TYPE, params ) );


		// The following functions pertain to providing updated
		// information relating to the player.


		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_status", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_hp", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_mp", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_name", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_id", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_muscle_sign", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_mysticality_sign", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_moxie_sign", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_class", CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_level", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_hp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxhp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_mp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxmp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_primestat", STAT_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_basestat", INT_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_buffedstat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_meat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_adventures", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_turncount", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_fullness", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "fullness_limit", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_inebriety", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "inebriety_limit", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_spleen_use", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "spleen_limit", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_eat", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_drink", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_interact", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_hardcore", BOOLEAN_TYPE, params ) );


		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.


		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "have_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "mp_cost", INT_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "turns_per_cast", INT_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "have_effect", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, SKILL_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "attack", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "steal", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "runaway", STRING_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", STRING_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_item", STRING_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_items", STRING_TYPE, params ) );


		// Equipment functions.


		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "can_equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "equipped_item", ITEM_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "have_equipped", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "outfit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "have_outfit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "have_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "use_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_equipment", ITEM_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_weight", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_hands", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_type", STRING_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "ranged_weapon", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "get_power", INT_TYPE, params ) );


		// Random other functions related to current in-game
		// state, not directly tied to the character.


		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "council", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_mcd", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "change_mcd", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_chef", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_bartender", BOOLEAN_TYPE, params ) );


		// String parsing functions.


		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "contains_text", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_meat", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_items", RESULT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "length", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE, INT_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE, INT_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, INT_TYPE, INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType( STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType( STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "group_string", REGEX_GROUP_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_lower_case", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_upper_case", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "chat_reply", VOID_TYPE, params ) );


		// Quest handling functions.


		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "entryway", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "hedgemaze", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "guardians", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "chamber", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "tavern", INT_TYPE, params ) );


		// Arithmetic utility functions.


		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "random", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "round", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "truncate", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "floor", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "ceil", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "square_root", FLOAT_TYPE, params ) );


		// Settings-type functions.


		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_encode", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_decode", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "get_property", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "set_property", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "remove_property", VOID_TYPE, params ) );


		// Functions for aggregates.


		params = new ScriptType[] { AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "count", INT_TYPE, params ) );

		params = new ScriptType[] { AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "clear", VOID_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, AGGREGATE_TYPE, BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { AGGREGATE_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { AGGREGATE_TYPE, STRING_TYPE, BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_location", LOCATION_TYPE, params ) );

		params = new ScriptType[] { LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "get_monsters", new ScriptAggregateType( MONSTER_TYPE, 0 ), params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "expected_damage", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "expected_damage", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_level_adjustment", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "weight_adjustment", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "mana_cost_modifier", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "raw_damage_absorption", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_absorption_percent", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_reduction", INT_TYPE, params ) );

		params = new ScriptType[] { ELEMENT_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "elemental_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "combat_rate_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "initiative_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "experience_bonus", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "meat_drop_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drop_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "buffed_hit_stat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_hit_stat", STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_element", ELEMENT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_element", ELEMENT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_attack", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_attack", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_defense", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_defense", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_hp", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_hp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_miss", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_dodge", BOOLEAN_TYPE, params ) );

		return result;
	}

	public static ScriptTypeList getSimpleTypes()
	{
		ScriptTypeList result = new ScriptTypeList();
		result.addElement( VOID_TYPE );
		result.addElement( BOOLEAN_TYPE );
		result.addElement( INT_TYPE );
		result.addElement( FLOAT_TYPE );
		result.addElement( STRING_TYPE );
		result.addElement( ITEM_TYPE );
		result.addElement( LOCATION_TYPE );
		result.addElement( CLASS_TYPE );
		result.addElement( STAT_TYPE );
		result.addElement( SKILL_TYPE );
		result.addElement( EFFECT_TYPE );
		result.addElement( FAMILIAR_TYPE );
		result.addElement( SLOT_TYPE );
		result.addElement( MONSTER_TYPE );
		result.addElement( ELEMENT_TYPE );
		return result;
	}

	public static ScriptSymbolTable getReservedWords()
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

	public static boolean isReservedWord( String name )
	{	return reservedWords.findSymbol( name ) != null;
	}

	private class ScriptScope
	{
		ScriptFunctionList	functions;
		ScriptVariableList	variables;
		ScriptTypeList		types;
		ScriptCommandList	commands;
		ScriptScope		parentScope;

		public ScriptScope( ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptCommand command, ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.commands.addElement( command );
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptVariableList variables, ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			if ( variables == null )
				variables = new ScriptVariableList();
			this.variables = variables;
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptFunctionList functions, ScriptVariableList variables, ScriptTypeList types  )
		{
			if ( functions == null )
				functions = new ScriptFunctionList();
			this.functions = functions;
			if ( variables == null )
				variables = new ScriptVariableList();
			this.variables = variables;
			if ( types == null )
				types = new ScriptTypeList();
			this.types = types;
			this.commands = new ScriptCommandList();
			this.parentScope = null;
		}

		public ScriptScope getParentScope()
		{	return parentScope;
		}

		public boolean addFunction( ScriptFunction f )
		{	return functions.addElement( f );
		}

		public boolean removeFunction( ScriptFunction f )
		{	return functions.removeElement( f );
		}

		public Iterator getFunctions()
		{	return functions.iterator();
		}

		public boolean addVariable( ScriptVariable v )
		{	return variables.addElement( v );
		}

		public Iterator getVariables()
		{	return variables.iterator();
		}

		public ScriptVariable findVariable( String name )
		{	return findVariable( name, false );
		}

		public ScriptVariable findVariable( String name, boolean recurse )
		{
			ScriptVariable current = variables.findVariable( name );
			if ( current != null )
				return current;
			if ( recurse && parentScope != null )
				return parentScope.findVariable( name, true );
			return null;
		}

		public boolean addType( ScriptType t )
		{	return types.addElement( t );
		}

		public Iterator getTypes()
		{	return types.iterator();
		}

		public ScriptType findType( String name )
		{
			ScriptType current = types.findType( name );
			if ( current != null )
				return current;
			if ( parentScope != null )
				return parentScope.findType( name );
			return null;
		}

		public void addCommand( ScriptCommand c )
		{	commands.addElement( c );
		}

		public boolean assertReturn()
		{
			int size = commands.size();
			if ( size == 0 )
				return false;
			if ( commands.get( size - 1 ) instanceof ScriptReturn )
				return true;
			return false;
		}

		private boolean isMatchingFunction( ScriptUserDefinedFunction current, ScriptUserDefinedFunction f )
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
					return false;

				++paramCount;
			}

			// There must be the same number of parameters

			if ( it1.hasNext() )
				return false;

			if ( it2.hasNext() )
				return false;

			// Unfortunately, if it's an exact match and you're avoiding
			// exact matches, you need to throw an exception.

			if ( avoidExactMatch )
				throw new AdvancedScriptException( "Function '" + f.getName() + "' already defined " + getLineAndFile() );

			return true;
		}

		public ScriptUserDefinedFunction replaceFunction( ScriptUserDefinedFunction f )
		{
			if ( f.getName().equals( "main" ) )
				return f;

			ScriptFunction [] options = functions.findFunctions( f.getName() );

			for ( int i = 0; i < options.length; ++i )
				if ( options[i] instanceof ScriptUserDefinedFunction && isMatchingFunction( (ScriptUserDefinedFunction) options[i], f ) )
					return (ScriptUserDefinedFunction) options[i];

			addFunction( f );
			return f;
		}

		private ScriptFunction findFunction( ScriptFunctionList source, String name, ScriptExpressionList params, boolean isExactMatch )
		{
			String errorMessage = null;

			ScriptFunction [] functions = source.findFunctions( name );

			// First, try to find an exact match on parameter types.
			// This allows strict matches to take precedence.

			for ( int i = 0; i < functions.length; ++i )
			{
				errorMessage = null;

				if ( params == null )
					return functions[i];

				Iterator refIterator = functions[i].getReferences();
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
							errorMessage = "Illegal parameter #" + paramIndex + " for function " + name +
								", got " + currentValue.getType() + ", need " + currentParam.getType() + " " + getLineAndFile();
						}
					}
					else if ( !validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
					{
						errorMessage = "Illegal parameter #" + paramIndex + " for function " + name +
							", got " + currentValue.getType() + ", need " + currentParam.getType() + " " + getLineAndFile();
					}

					++paramIndex;
				}

				if ( errorMessage == null && (refIterator.hasNext() || valIterator.hasNext()) )
				{
					errorMessage = "Illegal amount of parameters for function " + name +
						", got " + functions[i].getVariableReferences().size() + ", expected " + params.size() + " " + getLineAndFile();
				}

				if ( errorMessage == null )
					return functions[i];
			}

			if ( !isExactMatch && parentScope != null )
				return parentScope.findFunction( name, params );

			if ( !isExactMatch && source == existingFunctions && errorMessage != null )
				throw new AdvancedScriptException( errorMessage );

			return null;
		}

		public ScriptFunction findFunction( String name, ScriptExpressionList params )
		{
			ScriptFunction result = findFunction( functions, name, params, true );

			if ( result == null )
				result = findFunction( existingFunctions, name, params, true );
			if ( result == null )
				result = findFunction( functions, name, params, false );
			if ( result == null )
				result = findFunction( existingFunctions, name, params, false );

			// Just in case there's some people who don't want to edit
			// their scripts to use the new function format, check for
			// the old versions as well.

			if ( result == null )
			{
				if ( name.endsWith( "to_string" ) )
					return findFunction( "to_string", params );
				if ( name.endsWith( "to_boolean" ) )
					return findFunction( "to_boolean", params );
				if ( name.endsWith( "to_int" ) )
					return findFunction( "to_int", params );
				if ( name.endsWith( "to_float" ) )
					return findFunction( "to_float", params );
				if ( name.endsWith( "to_item" ) )
					return findFunction( "to_item", params );
				if ( name.endsWith( "to_skill" ) )
					return findFunction( "to_skill", params );
				if ( name.endsWith( "to_effect" ) )
					return findFunction( "to_effect", params );
				if ( name.endsWith( "to_location" ) )
					return findFunction( "to_location", params );
				if ( name.endsWith( "to_familiar" ) )
					return findFunction( "to_familiar", params );
				if ( name.endsWith( "to_monster" ) )
					return findFunction( "to_monster", params );
				if ( name.endsWith( "to_slot" ) )
					return findFunction( "to_slot", params );
				if ( name.endsWith( "to_url" ) )
					return findFunction( "to_url", params );
			}

			return result;
		}

		public Iterator getCommands()
		{	return commands.iterator();
		}

		public ScriptValue execute()
		{
			ScriptValue result = null;
			traceIndent();

			ScriptCommand current;
			Iterator it = commands.iterator();

			while ( it.hasNext() )
			{
				current = (ScriptCommand) it.next();
				result = current.execute();

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
					currentState = STATE_EXIT;

				if ( result == null )
					result = VOID_VALUE;

				trace( "[" + executionStateString( currentState ) + "] <- " + result.toQuotedString() );
				switch ( currentState )
				{
				case STATE_RETURN:
				case STATE_BREAK:
				case STATE_CONTINUE:
				case STATE_EXIT:

					traceUnindent();
					return result;
				}
			}

			traceUnindent();
			return result;
		}
	}

	private static class ScriptScopeList extends ScriptList
	{
		public boolean addElement( ScriptScope n )
		{	return super.addElement( n );
		}
	}

	private static class ScriptSymbol implements Comparable
	{
		public String name;

		public ScriptSymbol()
		{
		}

		public ScriptSymbol( String name )
		{	this.name = name;
		}

		public String getName()
		{	return name;
		}

		public int compareTo( Object o )
		{
			if ( !( o instanceof ScriptSymbol ) )
				throw new ClassCastException();
			if ( name == null)
				return 1;
			return name.compareToIgnoreCase( ((ScriptSymbol)o).name );
		}
	}

	private static class ScriptSymbolTable extends Vector
	{
		private int searchIndex = -1;

		public boolean addElement( ScriptSymbol n )
		{
			if ( findSymbol( n.getName() ) != null )
			     return false;

			super.addElement( n );
			return true;
		}

		public ScriptSymbol findSymbol( String name )
		{
			ScriptSymbol currentSymbol = null;
			for ( int i = 0; i < size(); ++i )
			{
				currentSymbol = (ScriptSymbol) get(i);
				if ( currentSymbol.getName().equalsIgnoreCase( name ) )
					return currentSymbol;
			}

			return null;
		}
	}

	private static abstract class ScriptFunction extends ScriptSymbol
	{
		public ScriptType type;
		public ScriptVariableReferenceList variableReferences;

		public ScriptFunction( String name, ScriptType type, ScriptVariableReferenceList variableReferences )
		{
			super( name );
			this.type = type;
			this.variableReferences = variableReferences;
		}

		public ScriptFunction( String name, ScriptType type )
		{	this( name, type, new ScriptVariableReferenceList() );
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{	return variableReferences;
		}

		public void setVariableReferences( ScriptVariableReferenceList variableReferences )
		{	this.variableReferences = variableReferences;
		}

		public Iterator getReferences()
		{	return variableReferences.iterator();
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
				message.append( getName() );

				message.append( "(" );


				Iterator it = variableReferences.iterator();
				ScriptVariableReference current;

				for ( int i = 0; it.hasNext(); ++i )
				{
					current = (ScriptVariableReference) it.next();

					if ( i != 0 )
						message.append( ',' );

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

	private class ScriptUserDefinedFunction extends ScriptFunction
	{
		private ScriptScope scope;
		private Stack callStack;

		public ScriptUserDefinedFunction( String name, ScriptType type, ScriptVariableReferenceList variableReferences )
		{
			super( name, type, variableReferences );

			this.scope = null;
			this.callStack = new Stack();
		}

		public void setScope( ScriptScope s )
		{	scope = s;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public void saveBindings()
		{
			if ( scope == null )
				return;

			ArrayList values = new ArrayList();
			for ( int i = 0; i < scope.variables.size(); ++i )
				values.add( ((ScriptVariable)scope.variables.get(i)).getValue() );

			callStack.push( values );
		}

		public void restoreBindings()
		{
			if ( scope == null )
				return;

			ArrayList values = (ArrayList) callStack.pop();
			for ( int i = 0; i < scope.variables.size(); ++i )
				if ( !(((ScriptVariable)scope.variables.get(i)).getType() instanceof ScriptAggregateType) )
					((ScriptVariable)scope.variables.get(i)).forceValue( (ScriptValue) values.get(i) );
		}

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( getName() ) )
			{
				printDisabledMessage();
				return getType().initialValue();
			}

			if ( scope == null )
				throw new RuntimeException( "Calling undefined user function: " + getName() );

			ScriptValue result = scope.execute();

			if ( result.getType().equals( type ) )
				return result;

			return getType().initialValue();
		}

		public boolean assertReturn()
		{	return scope.assertReturn();
		}
	}

	private static class ScriptExistingFunction extends ScriptFunction
	{
		private Method method;
		private String description;
		private ScriptVariable [] variables;

		public ScriptExistingFunction( String name, ScriptType type, ScriptType [] params )
		{	this( name, type, params, null );
		}

		public ScriptExistingFunction( String name, ScriptType type, ScriptType [] params, String description )
		{
			super( name.toLowerCase(), type );
			this.description = description;

			variables = new ScriptVariable[ params.length ];
			Class [] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				variables[i] = new ScriptVariable( params[i] );
				variableReferences.addElement( new ScriptVariableReference( variables[i] ) );
				args[i] = ScriptVariable.class;
			}

			try
			{
				this.method = ScriptExistingFunction.class.getMethod( name, args );
			}
			catch ( Exception e )
			{
				// This should not happen; it denotes a coding
				// error that must be fixed before release.

				printStackTrace( e, "No method found for built-in function: " + name );
			}
		}

		public String getDescription()
		{	return description;
		}

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( getName() ) )
			{
				printDisabledMessage();
				return getType().initialValue();
			}

			if ( method == null )
				throw new RuntimeException( "Internal error: no method for " + getName() );

			try
			{
				// Invoke the method

				return (ScriptValue) method.invoke(this, variables);
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				throw new AdvancedScriptException( e.getCause() == null ? e : e.getCause() );
			}
		}

		private ScriptValue continueValue()
		{	return ( KoLmafia.permitsContinue() && !KoLmafia.hadPendingState() ) ? TRUE_VALUE : FALSE_VALUE;
		}

		// Basic utility functions which print information
		// or allow for easy testing.

		public ScriptValue enable( ScriptVariable name )
		{
			StaticEntity.enable( name.toStringValue().toString().toLowerCase() );
			return VOID_VALUE;
		}

		public ScriptValue disable( ScriptVariable name )
		{
			StaticEntity.disable( name.toStringValue().toString().toLowerCase() );
			return VOID_VALUE;
		}

		public ScriptValue user_confirm( ScriptVariable message )
		{
			return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null, message.toStringValue().toString(),
				"Scripted User Confirmation Request", JOptionPane.YES_NO_OPTION ) ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue print( ScriptVariable string )
		{
			RequestLogger.printLine( string.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue print( ScriptVariable string, ScriptVariable color )
		{
			String parameters = string.toStringValue().toString();
			String colorString = color.toStringValue().toString();

			RequestLogger.printLine( "<font color=\"" + colorString + "\">" + StaticEntity.globalStringReplace( parameters, "<", "&lt;" ) + "</font>" );

			return VOID_VALUE;
		}

		public ScriptValue print_html( ScriptVariable string )
		{
			RequestLogger.printLine( string.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue abort()
		{
			RequestThread.declareWorldPeace();
			return VOID_VALUE;
		}

		public ScriptValue abort( ScriptVariable string )
		{
			KoLmafia.updateDisplay( ABORT_STATE, string.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue cli_execute( ScriptVariable string )
		{
			DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
			return continueValue();
		}

		private File getFile( String filename )
		{
			if ( filename.startsWith( "http" ) )
				return null;

			File f = new File( SCRIPT_LOCATION, filename );
			if ( !f.exists() )
				f = new File( DATA_LOCATION, filename );
			if ( !f.exists() )
				f = new File( ROOT_LOCATION, filename );

			if ( !f.exists() && filename.endsWith( ".dat" ) )
				return getFile( filename.substring( 0, filename.length() - 4 ) + ".txt" );

			return new File( DATA_LOCATION, filename );
		}

		private BufferedReader getReader( String filename )
		{
			if ( filename.startsWith( "http" ) )
				return DataUtilities.getReader( filename );

			File input = getFile( filename );
			if ( input.exists() )
				return DataUtilities.getReader( input );

			return DataUtilities.getReader( "data", filename );
		}

		public ScriptValue load_html( ScriptVariable string )
		{
			String location = string.toStringValue().toString();
			if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
				return STRING_INIT;

			File input = getFile( location );
			if ( input == null || !input.exists() )
				return STRING_INIT;

			KoLRequest request = new KoLRequest( "", true );
			request.loadResponseFromFile( input );

			return request.responseText == null ? STRING_INIT : new ScriptValue( request.responseText );
		}

		public ScriptValue visit_url( ScriptVariable string )
		{	return visit_url( string.toStringValue().toString() );
		}

		private ScriptValue visit_url( String location )
		{
			if ( KoLRequest.shouldIgnore( location ) )
				return STRING_INIT;

			if ( location.startsWith( "fight.php" ) )
			{
				if ( FightRequest.getActualRound() == 0 )
					return STRING_INIT;

				KoLRequest.delay();
			}

			KoLRequest request = new KoLRequest( location, true );
			request.setDelayExempt( false );
			RequestThread.postRequest( request );

			StaticEntity.externalUpdate( location, request.responseText );
			return request.responseText == null ? STRING_INIT : new ScriptValue( request.responseText );
		}

		public ScriptValue wait( ScriptVariable delay )
		{
			DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
			return VOID_VALUE;
		}

		// Type conversion functions which allow conversion
		// of one data format to another.

		public ScriptValue to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue to_boolean( ScriptVariable val )
		{	return val.toStringValue().toString().equals( "true" ) || val.intValue() != 0 ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue to_int( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_STRING ) ? parseIntValue( val.toStringValue().toString() ) :
				new ScriptValue( val.intValue() );
		}

		public ScriptValue to_float( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_STRING ) ? parseFloatValue( val.toStringValue().toString() ) :
				val.intValue() != 0 ? new ScriptValue( (float) val.intValue() ) : new ScriptValue( val.floatValue() );
		}

		public ScriptValue to_item( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_INT ) ? makeItemValue( val.intValue() ) :
				parseItemValue( val.toStringValue().toString() );
		}

		public ScriptValue to_skill( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_INT ) ? makeSkillValue( val.intValue() ) : val.getValueType().equals( TYPE_EFFECT ) ?
				parseSkillValue( UneffectRequest.effectToSkill( val.toStringValue().toString() ) ) :
				parseSkillValue( val.toStringValue().toString() );
		}

		public ScriptValue to_effect( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_INT ) ? makeEffectValue( val.intValue() ) : val.getValueType().equals( TYPE_SKILL ) ?
				parseEffectValue( UneffectRequest.skillToEffect( val.toStringValue().toString() ) ) :
				parseEffectValue( val.toStringValue().toString() );
		}

		public ScriptValue to_location( ScriptVariable val )
		{	return parseLocationValue( val.toStringValue().toString() );
		}

		public ScriptValue to_familiar( ScriptVariable val )
		{
			return val.getValueType().equals( TYPE_INT ) ? makeFamiliarValue( val.intValue() ) :
				parseFamiliarValue( val.toStringValue().toString() );
		}

		public ScriptValue to_monster( ScriptVariable val )
		{	return parseMonsterValue( val.toStringValue().toString() );
		}

		public ScriptValue to_slot( ScriptVariable item )
		{
			switch ( TradeableItemDatabase.getConsumptionType( item.intValue() ) )
			{
			case EQUIP_HAT:
				return parseSlotValue( "hat" );
			case EQUIP_WEAPON:
				return parseSlotValue( "weapon" );
			case EQUIP_OFFHAND:
				return parseSlotValue( "off-hand" );
			case EQUIP_SHIRT:
				return parseSlotValue( "shirt" );
			case EQUIP_PANTS:
				return parseSlotValue( "pants" );
			case EQUIP_FAMILIAR:
				return parseSlotValue( "familiar" );
			case EQUIP_ACCESSORY:
				return parseSlotValue( "acc1" );
			default:
				return parseSlotValue( "none" );
			}
		}

		public ScriptValue to_url( ScriptVariable val )
		{
			KoLAdventure adventure = (KoLAdventure) val.rawValue();
			return new ScriptValue( adventure.getRequest().getURLString() );
		}

		// Functions related to daily information which get
		// updated usually once per day.

		public ScriptValue today_to_string()
		{	return parseStringValue( DATED_FILENAME_FORMAT.format( new Date() ) );
		}

		public ScriptValue moon_phase()
		{	return new ScriptValue( MoonPhaseDatabase.getPhaseStep() );
		}

		public ScriptValue moon_light()
		{	return new ScriptValue( MoonPhaseDatabase.getMoonlight() );
		}

		public ScriptValue stat_bonus_today()
		{
			return KoLmafiaCLI.testConditional( "today is muscle day" ) ? parseStatValue( "muscle" ) :
				KoLmafiaCLI.testConditional( "today is myst day" ) ? parseStatValue( "mysticality" ) :
				KoLmafiaCLI.testConditional( "today is moxie day" ) ? parseStatValue( "moxie" ) : STAT_INIT;
		}

		public ScriptValue stat_bonus_tomorrow()
		{
			return KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) ? parseStatValue( "muscle" ) :
				KoLmafiaCLI.testConditional( "tomorrow is myst day" ) ? parseStatValue( "mysticality" ) :
				KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) ? parseStatValue( "moxie" ) : STAT_INIT;
		}

		public ScriptValue session_logs( ScriptVariable dayCount )
		{	return getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue() );
		}

		public ScriptValue session_logs( ScriptVariable player, ScriptVariable dayCount )
		{	return getSessionLogs( player.toStringValue().toString(), dayCount.intValue() );
		}

		private ScriptValue getSessionLogs( String name, int dayCount )
		{
			String [] files = new String[ dayCount ];

			Calendar timestamp = Calendar.getInstance();

			ScriptAggregateType type = new ScriptAggregateType( STRING_TYPE, files.length );
			ScriptArray value = new ScriptArray( type );

			String filename;
			BufferedReader reader;
			StringBuffer contents = new StringBuffer();

			for ( int i = 0; i < files.length; ++i )
			{
				contents.setLength(0);
				filename = StaticEntity.globalStringReplace( name, " ", "_" ) + "_" +
					DATED_FILENAME_FORMAT.format( timestamp.getTime() ) + ".txt";

				reader = KoLDatabase.getReader( new File( SESSIONS_DIRECTORY, filename ) );
				timestamp.add( Calendar.DATE, -1 );

				if ( reader == null )
					continue;

				try
				{
					String line;

					while ( (line = reader.readLine()) != null )
					{
						contents.append( line );
						contents.append( LINE_BREAK );
					}
				}
				catch ( Exception e )
				{
					printStackTrace( e );
				}

				value.aset( new ScriptValue( i ), parseStringValue( contents.toString() ) );
			}

			return value;
		}

		// Major functions related to adventuring and
		// item management.

		public ScriptValue adventure( ScriptVariable count, ScriptVariable loc )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
			return continueValue();
		}

		public ScriptValue add_item_condition( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return VOID_VALUE;

			DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
			return VOID_VALUE;
		}

		public ScriptValue buy( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			AdventureResult itemToBuy = new AdventureResult( item.intValue(), 1 );
			int initialAmount = itemToBuy.getCount( inventory );
			DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
			return initialAmount + count.intValue() == itemToBuy.getCount( inventory ) ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue create( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "create " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue use( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return ConsumeItemRequest.lastUpdate.equals( "" ) ? continueValue() : FALSE_VALUE;
		}

		public ScriptValue eat( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "eat " + count.intValue() + " " + item.toStringValue() );
			return ConsumeItemRequest.lastUpdate.equals( "" ) ? continueValue() : FALSE_VALUE;
		}

		public ScriptValue drink( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "drink " + count.intValue() + " " + item.toStringValue() );
			return ConsumeItemRequest.lastUpdate.equals( "" ) ? continueValue() : FALSE_VALUE;
		}

		public ScriptValue put_closet( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue put_shop( ScriptVariable price, ScriptVariable limit, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " @ " + price.intValue() + " limit " + limit.intValue() );
			return continueValue();
		}

		public ScriptValue put_stash( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "stash put " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue put_display( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "display put " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_closet( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "closet take " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_storage( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_display( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_stash( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "stash take " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue autosell( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue hermit( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue retrieve_item( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
			return continueValue();
		}

		// Major functions which provide item-related
		// information.

		public ScriptValue daily_special()
		{
			AdventureResult special = KoLCharacter.inMoxieSign() ? MicrobreweryRequest.getDailySpecial() :
				KoLCharacter.inMysticalitySign() ? RestaurantRequest.getDailySpecial() : null;

			return special == null ? ITEM_INIT : parseItemValue( special.getName() );
		}

		public ScriptValue refresh_stash()
		{
			RequestThread.postRequest( new ClanStashRequest() );
			return continueValue();
		}

		public ScriptValue available_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );

			int runningTotal = item.getCount( inventory ) + item.getCount( closet );

			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
				if ( KoLCharacter.getEquipment(i).equals( item ) )
					++runningTotal;

			if ( KoLCharacter.canInteract() )
				runningTotal += item.getCount( storage );

			return new ScriptValue( runningTotal );
		}

		public ScriptValue item_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( inventory ) );
		}

		public ScriptValue closet_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( closet ) );
		}

		public ScriptValue creatable_amount( ScriptVariable arg )
		{
			ItemCreationRequest item = ItemCreationRequest.getInstance( arg.intValue() );
			return new ScriptValue( item == null ? 0 : item.getQuantityPossible() );
		}

		public ScriptValue storage_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( storage ) );
		}

		public ScriptValue display_amount( ScriptVariable arg )
		{
			if ( collection.isEmpty() )
				RequestThread.postRequest( new MuseumRequest() );

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( collection ) );
		}

		public ScriptValue shop_amount( ScriptVariable arg )
		{
			LockableListModel list = StoreManager.getSoldItemList();
			if ( list.isEmpty() )
				RequestThread.postRequest( new StoreManageRequest() );

			SoldItem item = new SoldItem( arg.intValue(), 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
				return new ScriptValue( 0 );

			item = (SoldItem) list.get( index );
			return new ScriptValue( item.getQuantity() );
		}

		public ScriptValue stash_amount( ScriptVariable arg )
		{
			List stash = ClanManager.getStash();
			if ( stash.isEmpty() )
				RequestThread.postRequest( new ClanStashRequest() );

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( stash ) );
		}

		public ScriptValue pulls_remaining()
		{	return new ScriptValue( ItemManageFrame.getPullsRemaining() );
		}

		public ScriptValue stills_available()
		{	return new ScriptValue( KoLCharacter.getStillsAvailable() );
		}

		public ScriptValue have_mushroom_plot()
		{	return new ScriptValue( MushroomPlot.ownsPlot() );
		}

		// The following functions pertain to providing updated
		// information relating to the player.

		public ScriptValue refresh_status()
		{
			RequestThread.postRequest( CharpaneRequest.getInstance() );
			return continueValue();
		}

		public ScriptValue restore_hp( ScriptVariable amount )
		{	return new ScriptValue( getClient().recoverHP( amount.intValue() ) );
		}

		public ScriptValue restore_mp( ScriptVariable amount )
		{
			int desiredMP = amount.intValue();
			while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
				getClient().recoverMP( desiredMP );
			return continueValue();
		}

		public ScriptValue my_name()
		{	return new ScriptValue( KoLCharacter.getUserName() );
		}

		public ScriptValue my_id()
		{	return new ScriptValue( KoLCharacter.getPlayerId() );
		}

		public ScriptValue in_muscle_sign()
		{	return new ScriptValue( KoLCharacter.inMuscleSign() );
		}

		public ScriptValue in_mysticality_sign()
		{	return new ScriptValue( KoLCharacter.inMysticalitySign() );
		}

		public ScriptValue in_moxie_sign()
		{	return new ScriptValue( KoLCharacter.inMoxieSign() );
		}

		public ScriptValue my_class()
		{	return makeClassValue( KoLCharacter.getClassType() );
		}

		public ScriptValue my_level()
		{	return new ScriptValue( KoLCharacter.getLevel() );
		}

		public ScriptValue my_hp()
		{	return new ScriptValue( KoLCharacter.getCurrentHP() );
		}

		public ScriptValue my_maxhp()
		{	return new ScriptValue( KoLCharacter.getMaximumHP() );
		}

		public ScriptValue my_mp()
		{	return new ScriptValue( KoLCharacter.getCurrentMP() );
		}

		public ScriptValue my_maxmp()
		{	return new ScriptValue( KoLCharacter.getMaximumMP() );
		}

		public ScriptValue my_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex();
			return primeIndex == 0 ? parseStatValue( "muscle" ) : primeIndex == 1 ? parseStatValue( "mysticality" ) :
				parseStatValue( "moxie" );
		}

		public ScriptValue my_basestat( ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( STATS[ stat ].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( KoLCharacter.getBaseMuscle() );
			if ( STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( KoLCharacter.getBaseMysticality() );
			if ( STATS[ stat ].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( KoLCharacter.getBaseMoxie() );

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_buffedstat( ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( STATS[ stat ].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
			if ( STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
			if ( STATS[ stat ].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( KoLCharacter.getAdjustedMoxie() );

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_meat()
		{	return new ScriptValue( KoLCharacter.getAvailableMeat() );
		}

		public ScriptValue my_adventures()
		{	return new ScriptValue( KoLCharacter.getAdventuresLeft() );
		}

		public ScriptValue my_turncount()
		{	return new ScriptValue( KoLCharacter.getCurrentRun() );
		}

		public ScriptValue my_fullness()
		{	return new ScriptValue( KoLCharacter.getFullness() );
		}

		public ScriptValue fullness_limit()
		{	return new ScriptValue( KoLCharacter.getFullnessLimit() );
		}

		public ScriptValue my_inebriety()
		{	return new ScriptValue( KoLCharacter.getInebriety() );
		}

		public ScriptValue inebriety_limit()
		{	return new ScriptValue( KoLCharacter.getInebrietyLimit() );
		}

		public ScriptValue my_spleen_use()
		{	return new ScriptValue( KoLCharacter.getSpleenUse() );
		}

		public ScriptValue spleen_limit()
		{	return new ScriptValue( KoLCharacter.getSpleenLimit() );
		}

		public ScriptValue can_eat()
		{	return new ScriptValue( KoLCharacter.canEat() );
		}

		public ScriptValue can_drink()
		{	return new ScriptValue( KoLCharacter.canDrink() );
		}

		public ScriptValue can_interact()
		{	return new ScriptValue( KoLCharacter.canInteract() );
		}

		public ScriptValue in_hardcore()
		{	return new ScriptValue( KoLCharacter.isHardcore() );
		}

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		public ScriptValue have_skill( ScriptVariable arg )
		{	return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
		}

		public ScriptValue mp_cost( ScriptVariable skill )
		{	return new ScriptValue( ClassSkillsDatabase.getMPConsumptionById( skill.intValue() ) );
		}

		public ScriptValue turns_per_cast( ScriptVariable skill )
		{	return new ScriptValue( ClassSkillsDatabase.getEffectDuration( skill.intValue() ) );
		}

		public ScriptValue have_effect( ScriptVariable arg )
		{
			List potentialEffects = StatusEffectDatabase.getMatchingNames( arg.toStringValue().toString() );
			AdventureResult effect = potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get(0), 0, true );
			return new ScriptValue( effect == null ? 0 : effect.getCount( activeEffects ) );
		}

		public ScriptValue use_skill( ScriptVariable count, ScriptVariable skill )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( ClassSkillsDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
					use_skill( skill );

				return TRUE_VALUE;
			}

			DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue use_skill( ScriptVariable skill )
		{
			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( ClassSkillsDatabase.isCombat( skill.intValue() ) )
				return visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );

			DEFAULT_SHELL.executeLine( "cast 1 " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate );
		}

		public ScriptValue use_skill( ScriptVariable count, ScriptVariable skill, ScriptVariable target )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( ClassSkillsDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
					use_skill( skill );

				return TRUE_VALUE;
			}

			DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() + " on " + target.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue attack()
		{	return visit_url( "fight.php?action=attack" );
		}

		public ScriptValue steal()
		{
			if ( !FightRequest.wonInitiative() )
				return attack();

			return visit_url( "fight.php?action=steal" );
		}

		public ScriptValue runaway()
		{	return visit_url( "fight.php?action=runaway" );
		}

		public ScriptValue throw_item( ScriptVariable item )
		{
			return item.intValue() == CombatSettings.MEAT_VORTEX ? attack() :
				visit_url( "fight.php?action=useitem&whichitem=" + item.intValue() );
		}

		public ScriptValue throw_items( ScriptVariable item1, ScriptVariable item2 )
		{
			return item1.intValue() == CombatSettings.MEAT_VORTEX ? throw_item( item2 ) :
				item2.intValue() == CombatSettings.MEAT_VORTEX ? throw_item( item1 ) :
				visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue() + "&whichitem2=" + item2.intValue() );
		}

		// Equipment functions.

		public ScriptValue can_equip( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.canEquip( TradeableItemDatabase.getItemName( item.intValue() ) ) );
		}

		public ScriptValue equip( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue equip( ScriptVariable slot, ScriptVariable item )
		{
			if ( item.getValue().equals( ITEM_INIT ) )
				DEFAULT_SHELL.executeLine( "unequip " + slot.toStringValue() );
			else
				DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );

			return continueValue();
		}

		public ScriptValue equipped_item( ScriptVariable slot )
		{	return makeItemValue( KoLCharacter.getEquipment( slot.intValue() ).getName() );
		}

		public ScriptValue have_equipped( ScriptVariable item )
		{	return KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue outfit( ScriptVariable outfit )
		{
			DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue().toString() );
			return continueValue();
		}

		public ScriptValue have_outfit( ScriptVariable outfit )
		{
			SpecialOutfit so = KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue().toString() );

			if ( so == null )
				return FALSE_VALUE;

			return EquipmentDatabase.hasOutfit( so.getOutfitId() ) ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue weapon_hands( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.getHands( item.intValue() ) );
		}

		public ScriptValue weapon_type( ScriptVariable item )
		{
			String type = EquipmentDatabase.getType( item.intValue() );
			return new ScriptValue( type == null ? "unknown" : type );
		}

		public ScriptValue ranged_weapon( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.isRanged( item.intValue() ) );
		}

		public ScriptValue get_power( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.getPower( item.intValue() ) );
		}

		public ScriptValue my_familiar()
		{	return makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
		}

		public ScriptValue have_familiar( ScriptVariable familiar )
		{	return new ScriptValue( KoLCharacter.findFamiliar( familiar.toStringValue().toString() ) != null );
		}

		public ScriptValue use_familiar( ScriptVariable familiar )
		{
			DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
			return continueValue();
		}

		public ScriptValue familiar_equipment( ScriptVariable familiar )
		{	return parseItemValue( FamiliarsDatabase.getFamiliarItem( familiar.intValue() ) );
		}

		public ScriptValue familiar_weight( ScriptVariable familiar )
		{
			FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue().toString() );
			return new ScriptValue( (fam == null ) ? 0 : fam.getWeight() );
		}


		// Random other functions related to current in-game
		// state, not directly tied to the character.

		public ScriptValue council()
		{
			DEFAULT_SHELL.executeLine( "council" );
			return VOID_VALUE;
		}

		public ScriptValue current_mcd()
		{	return new ScriptValue( KoLCharacter.getSignedMLAdjustment() );
		}

		public ScriptValue change_mcd( ScriptVariable level )
		{
			DEFAULT_SHELL.executeLine( "mind-control " + level.intValue() );
			return continueValue();
		}

		public ScriptValue have_chef()
		{	return new ScriptValue( KoLCharacter.hasChef() );
		}

		public ScriptValue have_bartender()
		{	return new ScriptValue( KoLCharacter.hasBartender() );
		}

		// String parsing functions.

		public ScriptValue contains_text( ScriptVariable source, ScriptVariable search )
		{	return new ScriptValue( source.toStringValue().toString().indexOf( search.toStringValue().toString() ) != -1 );
		}

		public ScriptValue extract_meat( ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get(i);
				if ( result.getName().equals( AdventureResult.MEAT ) )
					return new ScriptValue( result.getCount() );
			}

			return new ScriptValue( 0 );
		}

		public ScriptValue extract_items( ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );
			ScriptMap value = new ScriptMap( RESULT_TYPE );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get(i);
				if ( result.isItem() )
					value.aset( parseItemValue( result.getName() ), parseIntValue( String.valueOf( result.getCount() ) ) );
			}

			return value;
		}

		public ScriptValue length( ScriptVariable string )
		{	return new ScriptValue( string.toStringValue().toString().length() );
		}

		public ScriptValue index_of( ScriptVariable source, ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.indexOf( substring ) );
		}

		public ScriptValue index_of( ScriptVariable source, ScriptVariable search, ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.indexOf( substring, begin ) );
		}

		public ScriptValue last_index_of( ScriptVariable source, ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.lastIndexOf( substring ) );
		}

		public ScriptValue last_index_of( ScriptVariable source, ScriptVariable search, ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.lastIndexOf( substring, begin ) );
		}

		public ScriptValue substring( ScriptVariable source, ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.substring( begin ) );
		}

		public ScriptValue substring( ScriptVariable source, ScriptVariable start, ScriptVariable finish )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			int end = finish.intValue();
			return new ScriptValue( string.substring( begin, end ) );
		}

		public ScriptValue replace_string( ScriptVariable string, ScriptVariable search, ScriptVariable replace )
		{
			return parseStringValue( StaticEntity.globalStringReplace( string.toStringValue().toString(),
				search.toStringValue().toString(), replace.toStringValue().toString() ) );
		}

		public ScriptValue split_string( ScriptVariable string )
		{
			String [] pieces = string.toStringValue().toString().split( LINE_BREAK );

			ScriptAggregateType type = new ScriptAggregateType( STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
				value.aset( new ScriptValue( i ), parseStringValue( pieces[i] ) );

			return value;
		}

		public ScriptValue split_string( ScriptVariable string, ScriptVariable regex )
		{
			String [] pieces = string.toStringValue().toString().split( regex.toStringValue().toString() );

			ScriptAggregateType type = new ScriptAggregateType( STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
				value.aset( new ScriptValue( i ), parseStringValue( pieces[i] ) );

			return value;
		}

		public ScriptValue group_string( ScriptVariable string, ScriptVariable regex )
		{
			Matcher userPatternMatcher = Pattern.compile( regex.toStringValue().toString() ).matcher( string.toStringValue().toString() );
			ScriptMap value = new ScriptMap( REGEX_GROUP_TYPE );

			int matchCount = 0;
			int groupCount = userPatternMatcher.groupCount();

			ScriptValue [] groupIndexes = new ScriptValue[ groupCount + 1 ];
			for ( int i = 0; i <= groupCount; ++i )
				groupIndexes[i] = new ScriptValue( i );

			String [] keyParts = new String[3];

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
						slice.aset( groupIndexes[i], parseStringValue( userPatternMatcher.group(i) ) );

					++matchCount;
				}
			}
			catch ( Exception e )
			{
				// Because we're doing everything ourselves, this
				// error shouldn't get generated.  Print a stack
				// trace, just in case.

				printStackTrace( e );
			}

			return value;
		}

		public ScriptValue to_upper_case( ScriptVariable string )
		{	return parseStringValue( string.toStringValue().toString().toUpperCase() );
		}

		public ScriptValue to_lower_case( ScriptVariable string )
		{	return parseStringValue( string.toStringValue().toString().toLowerCase() );
		}

		public ScriptValue chat_reply( ScriptVariable string )
		{
			String recipient = KoLMessenger.lastBlueMessage();
			if ( !recipient.equals( "" ) )
				RequestThread.postRequest( new ChatRequest( recipient, string.toStringValue().toString(), false ) );

			return VOID_VALUE;
		}

		// Quest completion functions.

		public ScriptValue entryway()
		{
			DEFAULT_SHELL.executeLine( "entryway" );
			return continueValue();
		}

		public ScriptValue hedgemaze()
		{
			DEFAULT_SHELL.executeLine( "hedgemaze" );
			return continueValue();
		}

		public ScriptValue guardians()
		{
			int itemId = SorceressLair.fightTowerGuardians( true );
			return makeItemValue( itemId );
		}

		public ScriptValue chamber()
		{
			DEFAULT_SHELL.executeLine( "chamber" );
			return continueValue();
		}

		public ScriptValue tavern()
		{
			int result = getClient().locateTavernFaucet();
			return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
		}

		// Arithmetic utility functions.

		public ScriptValue random( ScriptVariable arg )
		{
			int range = arg.intValue();
			if ( range < 2 )
				throw new RuntimeException( "Random range must be at least 2" );
			return new ScriptValue( RNG.nextInt( range ) );
		}

		public ScriptValue round( ScriptVariable arg )
		{	return new ScriptValue( (int)Math.round( arg.floatValue() ) );
		}

		public ScriptValue truncate( ScriptVariable arg )
		{	return new ScriptValue( (int)arg.floatValue() );
		}

		public ScriptValue floor( ScriptVariable arg )
		{	return new ScriptValue( (int)Math.floor( arg.floatValue() ) );
		}

		public ScriptValue ceil( ScriptVariable arg )
		{	return new ScriptValue( (int)Math.ceil( arg.floatValue() ) );
		}

		public ScriptValue square_root( ScriptVariable val )
		{	return new ScriptValue( (float) Math.sqrt( val.floatValue() ) );
		}

		// Settings-type functions.

		public ScriptValue url_encode( ScriptVariable arg ) throws UnsupportedEncodingException
		{	return new ScriptValue( URLEncoder.encode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue url_decode( ScriptVariable arg ) throws UnsupportedEncodingException
		{	return new ScriptValue( URLDecoder.decode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue get_property( ScriptVariable name )
		{
			String property = name.toStringValue().toString();
			return !KoLSettings.isUserEditable( property ) ? STRING_INIT : new ScriptValue( getProperty( property ) );
		}

		public ScriptValue set_property( ScriptVariable name, ScriptVariable value )
		{
			// In order to avoid code duplication for combat
			// related settings, use the shell.

			DEFAULT_SHELL.executeCommand( "set", name.toStringValue().toString() + "=" + value.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue remove_property( ScriptVariable name )
		{
			// In order to avoid code duplication for combat
			// related settings, use the shell.

			StaticEntity.removeProperty( name.toStringValue().toString() );
			return VOID_VALUE;
		}

		// Functions for aggregates.

		public ScriptValue count( ScriptVariable arg )
		{	return new ScriptValue( arg.getValue().count() );
		}

		public ScriptValue clear( ScriptVariable arg )
		{
			arg.getValue().clear();
			return VOID_VALUE;
		}

		public ScriptValue file_to_map( ScriptVariable var1, ScriptVariable var2 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			return readMap( filename, map_variable, true );
		}

		public ScriptValue file_to_map( ScriptVariable var1, ScriptVariable var2, ScriptVariable var3 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			boolean compact = var3.intValue() == 1;
			return readMap( filename, map_variable, compact );
		}

		private ScriptValue readMap( String filename, ScriptCompositeValue result, boolean compact )
		{
			BufferedReader reader = getReader( filename );
			if ( reader == null )
				return FALSE_VALUE;

			String [] data = null;
			result.clear();

			try
			{
				while ( (data = KoLDatabase.readData( reader )) != null )
					result.read( data, 0, compact );
			}
			catch ( Exception e )
			{
				// Okay, runtime error. Indicate that there was
				// a bad currentLine in the data file and print the
				// stack trace.

				StringBuffer buffer = new StringBuffer( "Invalid line in data file:" );
				if ( data != null )
				{
					buffer.append( LINE_BREAK );

					for ( int i = 0; i < data.length; ++i )
					{
						buffer.append( '\t' );
						buffer.append( data[i] );
					}
				}

				printStackTrace( e, buffer.toString() );
				return FALSE_VALUE;
			}

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}

			return TRUE_VALUE;
		}

		public ScriptValue map_to_file( ScriptVariable var1, ScriptVariable var2 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			return printMap( map_variable, filename, true );
		}

		public ScriptValue map_to_file( ScriptVariable var1, ScriptVariable var2, ScriptVariable var3 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			boolean compact = var3.intValue() == 1;
			return printMap( map_variable, filename, compact );
		}

		private ScriptValue printMap( ScriptCompositeValue map_variable, String filename, boolean compact )
		{
			if ( filename.startsWith( "http" ) )
				return FALSE_VALUE;

			PrintStream writer = null;
			File output = getFile( filename );

			if ( output == null )
				return FALSE_VALUE;

			writer = LogStream.openStream( output, true );
			map_variable.dump( writer, "", compact );
			writer.close();

			return TRUE_VALUE;
		}

		// Custom combat helper functions.

		public ScriptValue my_location()
		{
			String location = getProperty( "lastAdventure" );
			return location.equals( "" ) ? parseLocationValue( "Rest" ) : parseLocationValue( location );
		}

		public ScriptValue get_monsters( ScriptVariable location )
		{
			KoLAdventure adventure = (KoLAdventure) location.rawValue();
			AreaCombatData data = adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();

			ScriptAggregateType type = new ScriptAggregateType( MONSTER_TYPE, monsterCount );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < monsterCount; ++i )
				value.aset( new ScriptValue( i ), parseMonsterValue( data.getMonster(i).getName() ) );

			return value;

		}

		public ScriptValue expected_damage()
		{
			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue = Math.max( 0, FightRequest.getMonsterAttack() - KoLCharacter.getAdjustedMoxie() ) +
				(FightRequest.getMonsterAttack() / 4) - KoLCharacter.getDamageReduction();

			float damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue expected_damage( ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ZERO_VALUE;

			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue = Math.max( 0, monster.getAttack() - KoLCharacter.getAdjustedMoxie() ) +
				(FightRequest.getMonsterAttack() / 4) - KoLCharacter.getDamageReduction();

			float damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue monster_level_adjustment()
		{	return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue weight_adjustment()
		{	return new ScriptValue( KoLCharacter.getFamiliarWeightAdjustment() );
		}

		public ScriptValue mana_cost_modifier()
		{	return new ScriptValue( KoLCharacter.getManaCostAdjustment() );
		}

		public ScriptValue raw_damage_absorption()
		{	return new ScriptValue( KoLCharacter.getDamageAbsorption() );
		}

		public ScriptValue damage_absorption_percent()
		{
			int raw = KoLCharacter.getDamageAbsorption();
			if ( raw == 0 )
				return ZERO_FLOAT_VALUE;

			// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
			// ( sqrt( raw / 10 ) - 1 ) / 10

			double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
			return new ScriptValue( (float)percent );
		}

		public ScriptValue damage_reduction()
		{	return new ScriptValue( KoLCharacter.getDamageReduction() );
		}

		public ScriptValue elemental_resistance()
		{	return new ScriptValue( KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() ) );
		}

		public ScriptValue elemental_resistance( ScriptVariable arg )
		{
			if ( arg.getType().equals( TYPE_ELEMENT ) )
				return new ScriptValue( KoLCharacter.getElementalResistance( arg.intValue() ) );

			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ZERO_VALUE;

			return new ScriptValue( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
		}

		public ScriptValue combat_rate_modifier()
		{	return new ScriptValue( KoLCharacter.getCombatRateAdjustment() );
		}

		public ScriptValue initiative_modifier()
		{	return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
		}

		public ScriptValue experience_bonus()
		{	return new ScriptValue( KoLCharacter.getExperienceAdjustment() );
		}

		public ScriptValue meat_drop_modifier()
		{	return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
		}

		public ScriptValue item_drop_modifier()
		{	return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
		}

		public ScriptValue buffed_hit_stat()
		{
			int hitStat = KoLCharacter.getAdjustedHitStat();
			return new ScriptValue( hitStat );
		}

		public ScriptValue current_hit_stat()
		{
			String hitStat = KoLCharacter.getHitStatName();
			return parseStatValue( hitStat );
		}

		public ScriptValue monster_element()
		{
			int element = FightRequest.getMonsterDefenseElement();
			return new ScriptValue( ELEMENT_TYPE, element, MonsterDatabase.elementNames[element] );
		}

		public ScriptValue monster_element( ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ELEMENT_INIT;

			int element = monster.getDefenseElement();
			return new ScriptValue( ELEMENT_TYPE, element, MonsterDatabase.elementNames[element] );
		}

		public ScriptValue monster_attack()
		{	return new ScriptValue( FightRequest.getMonsterAttack() );
		}

		public ScriptValue monster_attack( ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ZERO_VALUE;

			return new ScriptValue( monster.getAttack() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_defense()
		{	return new ScriptValue( FightRequest.getMonsterDefense() );
		}

		public ScriptValue monster_defense( ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ZERO_VALUE;

			return new ScriptValue( monster.getDefense() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_hp()
		{	return new ScriptValue( FightRequest.getMonsterHealth() );
		}

		public ScriptValue monster_hp( ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
				return ZERO_VALUE;

			return new ScriptValue( monster.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) );
		}


		public ScriptValue will_usually_dodge()
		{	return FightRequest.willUsuallyDodge() ? TRUE_VALUE : FALSE_VALUE;
		}

		public ScriptValue will_usually_miss()
		{	return FightRequest.willUsuallyMiss() ? TRUE_VALUE : FALSE_VALUE;
		}
	}

	private static class ScriptFunctionList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptFunction n )
		{
			super.add( n );
			return true;
		}

		public ScriptFunction [] findFunctions( String name )
		{
			ArrayList matches = new ArrayList();

			for ( int i = 0; i < size(); ++i )
				if ( ((ScriptFunction) get(i)).getName().equalsIgnoreCase( name ) )
					matches.add( get(i) );

			ScriptFunction [] matchArray = new ScriptFunction[ matches.size() ];
			matches.toArray( matchArray );
			return matchArray;
		}
	}

	private static class ScriptVariable extends ScriptSymbol
	{
		ScriptType type;
		ScriptValue	content;
		ScriptExpression expression = null;

		public ScriptVariable( ScriptType type )
		{
			super( null );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptVariable( String name, ScriptType type )
		{
			super( name );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptValue getValue()
		{
			if ( expression != null )
				content = expression.execute();

			return content;
		}

		public ScriptType getValueType()
		{	return getValue().getType();
		}

		public Object rawValue()
		{	return getValue().rawValue();
		}

		public int intValue()
		{	return getValue().intValue();
		}

		public ScriptValue toStringValue()
		{	return getValue().toStringValue();
		}

		public float floatValue()
		{	return getValue().floatValue();
		}

		public void setExpression( ScriptExpression targetExpression )
		{	expression = targetExpression;
		}

		public void forceValue( ScriptValue targetValue )
		{
			content = targetValue;
			expression = null;
		}

		public void setValue( ScriptValue targetValue )
		{
			if ( getType().equals( targetValue.getType() ) )
			{
				content = targetValue;
				expression = null;
			}
			else if ( getType().equals( TYPE_STRING ) )
			{
				content = targetValue.toStringValue();
				expression = null;
			}
			else if ( getType().equals( TYPE_INT ) && targetValue.getType().equals( TYPE_FLOAT ) )
			{
				content = targetValue.toIntValue();
				expression = null;
			}
			else if ( getType().equals( TYPE_FLOAT ) && targetValue.getType().equals( TYPE_INT ) )
			{
				content = targetValue.toFloatValue();
				expression = null;
			}
			else if ( getType().equals( TYPE_ANY ) )
			{
				content = targetValue;
				expression = null;
			}
			else if ( getType().getBaseType().equals( TYPE_AGGREGATE ) && targetValue.getType().getBaseType().equals( TYPE_AGGREGATE ) )
			{
				content = targetValue;
				expression = null;
			}
			else
			{
				throw new RuntimeException( "Internal error: Cannot assign " + targetValue.getType() + " to " + getType() );
			}
		}
	}

	private static class ScriptVariableList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptVariable n )
		{	return super.addElement( n );
		}

		public ScriptVariable findVariable( String name )
		{	return (ScriptVariable) super.findSymbol( name );
		}

		public Iterator getVariables()
		{	return iterator();
		}
	}

	private static class ScriptVariableReference extends ScriptValue
	{
		public ScriptVariable target;

		public ScriptVariableReference( ScriptVariable target )
		{	this.target = target;
		}

		public ScriptVariableReference( String varName, ScriptScope scope )
		{	target = scope.findVariable( varName, true );
		}

		public boolean valid()
		{	return target != null;
		}

		public ScriptType getType()
		{	return target.getType();
		}

		public String getName()
		{	return target.getName();
		}

		public ScriptExpressionList getIndices()
		{	return null;
		}

		public int compareTo( Object o )
		{	return target.getName().compareTo( ((ScriptVariableReference)o).target.getName() );
		}

		public ScriptValue execute()
		{	return target.getValue();
		}

		public ScriptValue getValue()
		{	return target.getValue();
		}

		public void forceValue( ScriptValue targetValue )
		{	target.forceValue( targetValue );
		}

		public void setValue( ScriptValue targetValue )
		{	target.setValue( targetValue );
		}

		public String toString()
		{	return target.getName();
		}
	}

	private class ScriptCompositeReference extends ScriptVariableReference
	{
		private ScriptExpressionList indices;

		// Derived from indices: Final slice and index into it
		private ScriptCompositeValue slice;
		private ScriptValue index;

		public ScriptCompositeReference( ScriptVariable target, ScriptExpressionList indices )
		{
			super( target );
			this.indices = indices;
		}

		public ScriptType getType()
		{
			ScriptType type = target.getType().getBaseType();
			for ( int i = 0; i < indices.size(); ++i )
				type = ((ScriptCompositeType)type).getDataType( indices.get(i) ).getBaseType();
			return type;
		}

		public String getName()
		{	return target.getName() + "[]";
		}

		public ScriptExpressionList getIndices()
		{	return indices;
		}

		public ScriptValue execute()
		{	return getValue();
		}

		// Evaluate all the indices and step through the slices.
		//
		// When done, this.slice has the final slice and this.index has
		// the final evaluated index.

		private boolean getSlice()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return false;
			}

			slice = (ScriptCompositeValue)target.getValue();
			index = null;

			traceIndent();
			trace( "AREF: " + slice.toString() );

			int count = indices.size();
			for ( int i = 0; i < count; ++i )
			{
				ScriptExpression exp = (ScriptExpression)indices.get(i);

				traceIndent();
				trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );

				index = exp.execute();
				captureValue( index );

				trace( "[" + executionStateString( currentState ) + "] <- " + index.toQuotedString() );
				traceUnindent();

				if ( currentState == STATE_EXIT )
				{
					traceUnindent();
					return false;
				}

				// If this is the last index, stop now
				if ( i == count - 1 )
					break;

				ScriptCompositeValue result = (ScriptCompositeValue)slice.aref( index );

				// Create missing intermediate slices
				if ( result == null )
				{
					result = (ScriptCompositeValue)slice.initialValue( index );
					slice.aset( index, result );
				}

				slice = result;

				trace( "AREF <- " + slice.toString() );
			}

			traceUnindent();

			return true;
		}

		public ScriptValue getValue()
		{
			// Iterate through indices to final slice
			if ( getSlice() )
			{
				ScriptValue result = slice.aref( index );

				if ( result == null )
				{
					result = slice.initialValue( index );
					slice.aset( index, result );
				}

				traceIndent();
				trace( "AREF <- " + result.toQuotedString() );
				traceUnindent();

				return result;
			}

			return null;
		}

		public void setValue( ScriptValue targetValue )
		{
			// Iterate through indices to final slice
			if ( getSlice() )
			{
				slice.aset( index, targetValue );
				traceIndent();
				trace( "ASET: " + targetValue.toQuotedString() );
				traceUnindent();
			}
		}

		public ScriptValue removeKey()
		{
			// Iterate through indices to final slice
			if ( getSlice() )
			{
				ScriptValue result = slice.remove( index );
				if ( result == null )
					result = slice.initialValue( index );
				traceIndent();
				trace( "remove <- " + result.toQuotedString() );
				traceUnindent();
				return result;
			}
			return null;
		}

		public boolean contains( ScriptValue index )
		{
			boolean result = false;
			// Iterate through indices to final slice
			if ( getSlice() )
				result = slice.aref( index ) != null;
			traceIndent();
			trace( "contains <- " + result );
			traceUnindent();
			return result;
		}

		public String toString()
		{	return target.getName() + "[]";
		}
        }

	private static class ScriptVariableReferenceList extends ScriptList
	{
		public boolean addElement( ScriptVariableReference n )
		{	return super.addElement( n );
		}
	}

	private static class ScriptCommand
	{
		public ScriptValue execute()
		{
			return null;
		}
	}

	private class ScriptFlowControl extends ScriptCommand
	{
		int command;

		public ScriptFlowControl( String command )
		{
			if ( command.equalsIgnoreCase( "break" ) )
				this.command = COMMAND_BREAK;
			else if ( command.equalsIgnoreCase( "continue" ) )
				this.command = COMMAND_CONTINUE;
			else if ( command.equalsIgnoreCase( "exit" ) )
				this.command = COMMAND_EXIT;
			else
				throw new AdvancedScriptException( "Invalid command '" + command + "' " + getLineAndFile() );
		}

		public ScriptFlowControl( int command )
		{	this.command = command;
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
			traceIndent();
			trace( toString() );
			traceUnindent();

			switch ( this.command )
			{
			case COMMAND_BREAK:
				currentState = STATE_BREAK;
				return VOID_VALUE;
			case COMMAND_CONTINUE:
				currentState = STATE_CONTINUE;
				return VOID_VALUE;
			case COMMAND_EXIT:
				currentState = STATE_EXIT;
				return null;
			}

			throw new RuntimeException( "Internal error: unknown ScriptCommand type" );
		}
	}

	private static class ScriptCommandList extends ScriptList
	{
		public boolean addElement( ScriptCommand n )
		{	return super.addElement( n );
		}
	}

	private class ScriptReturn extends ScriptCommand
	{
		private ScriptExpression returnValue;
		private ScriptType expectedType;

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType )
		{
			this.returnValue = returnValue;

			if ( expectedType != null && returnValue != null )
			{
				if ( !validCoercion( returnValue.getType(), expectedType, "return" ) )
					throw new AdvancedScriptException( "Cannot apply " + returnValue.getType() + " to " + expectedType + " " + getLineAndFile() );
			}

			this.expectedType = expectedType;
		}

		public ScriptType getType()
		{
			if ( expectedType != null )
				return expectedType;

			if ( returnValue == null )
				return VOID_TYPE;

			return returnValue.getType();
		}

		public ScriptExpression getExpression()
		{	return returnValue;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
				currentState = STATE_EXIT;

			if ( currentState == STATE_EXIT )
				return null;

			currentState = STATE_RETURN;

			if ( returnValue == null )
				return null;

			traceIndent();
			trace( "Eval: " + returnValue );

			ScriptValue result = returnValue.execute();
			captureValue( result );

			trace( "Returning: " + result );
			traceUnindent();

			if ( currentState != STATE_EXIT )
				currentState = STATE_RETURN;

			if ( result == null )
				return null;

			if ( expectedType.equals( TYPE_STRING ) )
				return result.toStringValue();

			if ( expectedType.equals( TYPE_FLOAT ) )
				return result.toFloatValue();

			if ( expectedType.equals( TYPE_INT ) )
				return result.toIntValue();

			return result;
		}

		public String toString()
		{	return "return " + returnValue;
		}
	}

	private class ScriptConditional extends ScriptCommand
	{
		public ScriptScope scope;
		private ScriptExpression condition;

		public ScriptConditional( ScriptScope scope, ScriptExpression condition )
		{
			this.scope = scope;
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			trace( "Test: " + condition );

			ScriptValue conditionResult = condition.execute();
			captureValue( conditionResult );

			trace( "[" + executionStateString( currentState ) + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() == 1 )
			{
				ScriptValue result = scope.execute();

				traceUnindent();

				if ( currentState != STATE_NORMAL )
					return result;

				return TRUE_VALUE;
			}

			traceUnindent();
			return FALSE_VALUE;
		}
	}

	private class ScriptIf extends ScriptConditional
	{
		private ArrayList elseLoops;

		public ScriptIf( ScriptScope scope, ScriptExpression condition )
		{
			super( scope, condition );
			elseLoops = new ArrayList();
		}

		public Iterator getElseLoops()
		{	return elseLoops.iterator();
		}

		public void addElseLoop( ScriptConditional elseLoop )
		{	elseLoops.add( elseLoop );
		}

		public ScriptValue execute()
		{
			ScriptValue result = super.execute();
			if ( currentState != STATE_NORMAL || result == TRUE_VALUE )
				return result;

			// Conditional failed. Move to else clauses

			Iterator it = elseLoops.iterator();
			ScriptConditional elseLoop;

			while ( it.hasNext() )
			{
				elseLoop = (ScriptConditional) it.next();
				result = elseLoop.execute();

				if ( currentState != STATE_NORMAL || result == TRUE_VALUE )
					return result;
			}

			return FALSE_VALUE;
		}

		public String toString()
		{	return "if";
		}
	}

	private class ScriptElseIf extends ScriptConditional
	{
		public ScriptElseIf( ScriptScope scope, ScriptExpression condition )
		{	super( scope, condition );
		}

		public String toString()
		{	return "else if";
		}
	}

	private class ScriptElse extends ScriptConditional
	{
		public ScriptElse( ScriptScope scope, ScriptExpression condition )
		{	super( scope, condition );
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( "else" );
			ScriptValue result = scope.execute();
			traceUnindent();

			if ( currentState != STATE_NORMAL )
				return result;

			return TRUE_VALUE;
		}

		public String toString()
		{	return "else";
		}
	}

	private class ScriptLoop extends ScriptCommand
	{
		public ScriptScope scope;

		public ScriptLoop( ScriptScope scope )
		{
			this.scope = scope;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptValue execute()
		{
			ScriptValue result = scope.execute();

			if ( !KoLmafia.permitsContinue() )
				currentState = STATE_EXIT;

			switch ( currentState )
			{
			case STATE_EXIT:
				return null;

			case STATE_BREAK:
				// Stay in state; subclass exits loop
				return VOID_VALUE;

			case STATE_RETURN:
				// Stay in state; subclass exits loop
				return result;

			case STATE_CONTINUE:
				// Done with this iteration
				currentState = STATE_NORMAL;
				return result;

			case STATE_NORMAL:
				return result;
			}

			return result;
		}
	}

	private class ScriptForeach extends ScriptLoop
	{
		private ScriptVariableReferenceList variableReferences;
		private ScriptVariableReference aggregate;

		public ScriptForeach( ScriptScope scope, ScriptVariableReferenceList variableReferences, ScriptVariableReference aggregate )
		{
			super( scope );
			this.variableReferences = variableReferences;
			this.aggregate = aggregate;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{	return variableReferences;
		}

		public Iterator getReferences()
		{	return variableReferences.iterator();
		}

		public ScriptVariableReference getAggregate()
		{	return aggregate;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue) aggregate.execute();
			captureValue( slice );
			if ( currentState == STATE_EXIT )
				return null;

			// Iterate over the slice with bound keyvar

			Iterator it = getReferences();
			return executeSlice( slice, it, (ScriptVariableReference) it.next() );

		}

		private ScriptValue executeSlice( ScriptAggregateValue slice, Iterator it, ScriptVariableReference variable )
		{
			// Get an array of keys for the slice
			ScriptValue [] keys = slice.keys();

			// Get the next key variable
			ScriptVariableReference nextVariable = it.hasNext() ? (ScriptVariableReference) it.next() : null;

			// While there are further keys
			for ( int i = 0; i < keys.length; ++i )
			{
				// Get current key
				ScriptValue key = keys[i];

				// Bind variable to key
				variable.setValue( key );

				trace( "Key #" + i + ": " + key );

				// If there are more indices to bind, recurse
				ScriptValue result;
				if ( nextVariable != null )
				{
					ScriptAggregateValue nextSlice = (ScriptAggregateValue)slice.aref( key );
					traceIndent();
					result = executeSlice( nextSlice, it, nextVariable );
				}
				else
				{
					// Otherwise, execute scope
					result = super.execute();
				}
				switch ( currentState )
				{
				case STATE_NORMAL:
					break;
				case STATE_BREAK:
					currentState = STATE_NORMAL;
					// Fall through
				default:
					traceUnindent();
					return result;
				}
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return "foreach";
		}
	}

	private class ScriptWhile extends ScriptLoop
	{
		private ScriptExpression condition;

		public ScriptWhile( ScriptScope scope, ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );

		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			while ( true )
			{
				trace( "Test: " + condition );

				ScriptValue conditionResult = condition.execute();
				captureValue( conditionResult );

				trace( "[" + executionStateString( currentState ) + "] <- " + conditionResult );

				if (  conditionResult == null )
				{
					traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
					break;

				ScriptValue result = super.execute();

				if ( currentState == STATE_BREAK )
				{
					currentState = STATE_NORMAL;
					traceUnindent();
					return VOID_VALUE;
				}

				if ( currentState != STATE_NORMAL )
				{
					traceUnindent();
					return result;
				}
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return "while";
		}
	}

	private class ScriptRepeat extends ScriptLoop
	{
		private ScriptExpression condition;

		public ScriptRepeat( ScriptScope scope, ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );

		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			while ( true )
			{
				ScriptValue result = super.execute();

				if ( currentState == STATE_BREAK )
				{
					currentState = STATE_NORMAL;
					traceUnindent();
					return VOID_VALUE;
				}

				if ( currentState != STATE_NORMAL )
				{
					traceUnindent();
					return result;
				}

				trace( "Test: " + condition );

				ScriptValue conditionResult = condition.execute();
				captureValue( conditionResult );

				trace( "[" + executionStateString( currentState ) + "] <- " + conditionResult );

				if (  conditionResult == null )
				{
					traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() == 1 )
					break;
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return "repeat";
		}
	}

	private class ScriptFor extends ScriptLoop
	{
		private ScriptVariableReference variable;
		private ScriptExpression initial;
		private ScriptExpression last;
		private ScriptExpression increment;
		private int direction;

		public ScriptFor( ScriptScope scope, ScriptVariableReference variable, ScriptExpression initial, ScriptExpression last, ScriptExpression increment, int direction )
		{
			super( scope );
			this.variable = variable;
			this.initial = initial;
			this.last = last;
			this.increment = increment;
			this.direction = direction;
		}

		public ScriptVariableReference getVariable()
		{	return variable;
		}

		public ScriptExpression getInitial()
		{	return initial;
		}

		public ScriptExpression getLast()
		{	return last;
		}

		public ScriptExpression getIncrement()
		{	return increment;
		}

		public int getDirection()
		{	return direction;
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			// Get the initial value
			trace( "Initial: " + initial );

			ScriptValue initialValue = initial.execute();
			captureValue( initialValue );

			trace( "[" + executionStateString( currentState ) + "] <- " + initialValue );

			if (  initialValue == null )
			{
				traceUnindent();
				return null;
			}

			// Get the final value
			trace( "Last: " + last );

			ScriptValue lastValue = last.execute();
			captureValue( lastValue );

			trace( "[" + executionStateString( currentState ) + "] <- " + lastValue );

			if (  lastValue == null )
			{
				traceUnindent();
				return null;
			}

			// Get the increment
			trace( "Increment: " + increment );

			ScriptValue incrementValue = increment.execute();
			captureValue( incrementValue );

			trace( "[" + executionStateString( currentState ) + "] <- " + incrementValue );

			if (  incrementValue == null )
			{
				traceUnindent();
				return null;
			}

			int current = initialValue.intValue();
			int increment = incrementValue.intValue();
			int end = lastValue.intValue();

			boolean up = false;

			if ( direction > 0 )
				up = true;
			else if ( direction < 0 )
                        {
				up = false;
				increment = -increment;
                        }
			else
				up = ( current <= end );

			// Make sure the loop will eventually terminate

			if ( current != end && increment == 0 )
				throw new AdvancedScriptException( "Start not equal to end and increment equals 0" );

			while ( ( up && current <= end ) ||
				( !up && current >= end ) )
			{
				// Bind variable to current value
				variable.setValue( new ScriptValue( current ) );

				// Execute the scope
				ScriptValue result = super.execute();

				if ( currentState == STATE_BREAK )
				{
					currentState = STATE_NORMAL;
					traceUnindent();
					return VOID_VALUE;
				}

				if ( currentState != STATE_NORMAL )
				{
					traceUnindent();
					return result;
				}

				// Calculate next value
				current += increment;
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return "foreach";
		}
	}

	private class ScriptBasicScript extends ScriptValue
	{
		private ByteArrayStream data;

		public ScriptBasicScript( ByteArrayStream data )
		{	this.data = data;
		}

		public ScriptType getType()
		{	return VOID_TYPE;
		}

		public ScriptValue execute()
		{
			KoLmafiaCLI script = new KoLmafiaCLI( data.getByteArrayInputStream() );
			script.listenForCommands();
			return VOID_VALUE;
		}
	}

	private class ScriptCall extends ScriptValue
	{
		private ScriptFunction target;
		private ScriptExpressionList params;

		public ScriptCall( String functionName, ScriptScope scope, ScriptExpressionList params )
		{
			target = findFunction( functionName, scope, params );
			if ( target == null )
				throw new AdvancedScriptException( "Undefined reference '" + functionName + "' " + getLineAndFile() );
			this.params = params;
		}

		private ScriptFunction findFunction( String name, ScriptScope scope, ScriptExpressionList params )
		{
			if ( scope == null )
				return null;

			return scope.findFunction( name, params );
		}

		public ScriptFunction getTarget()
		{	return target;
		}

		public Iterator getExpressions()
		{	return params.iterator();
		}

		public ScriptType getType()
		{	return target.getType();
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			// Save current variable bindings
			target.saveBindings();
			traceIndent();

			Iterator refIterator = target.getReferences();
			Iterator valIterator = params.getExpressions();

			ScriptVariableReference paramVarRef;
			ScriptExpression paramValue;

			int paramCount = 0;

			while ( refIterator.hasNext() )
			{
				paramVarRef = (ScriptVariableReference) refIterator.next();

				++paramCount;

				if ( !valIterator.hasNext() )
				{
					target.restoreBindings();
					throw new RuntimeException( "Internal error: illegal arguments" );
				}

				paramValue = (ScriptExpression) valIterator.next();

				trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

				ScriptValue value = paramValue.execute();
				captureValue( value );

				trace( "[" + executionStateString( currentState ) + "] <- " + value.toQuotedString() );

				if ( currentState == STATE_EXIT )
				{
					target.restoreBindings();
					traceUnindent();
					return null;
				}

				// Bind parameter to new value
				if ( paramVarRef.getType().equals( TYPE_STRING ) )
					paramVarRef.setValue( value.toStringValue() );
				else if ( paramVarRef.getType().equals( TYPE_INT ) && paramValue.getType().equals( TYPE_FLOAT ) )
					paramVarRef.setValue( value.toIntValue() );
				else if ( paramVarRef.getType().equals( TYPE_FLOAT ) && paramValue.getType().equals( TYPE_INT ) )
					paramVarRef.setValue( value.toFloatValue() );
				else
					paramVarRef.setValue( value );
			}

			if ( valIterator.hasNext() )
			{
				target.restoreBindings();
				throw new RuntimeException( "Internal error: illegal arguments" );
			}

			trace( "Entering function " + target.getName() );
			ScriptValue result = target.execute();
			trace( "Function " + target.getName() + " returned: " + result );

			if ( currentState != STATE_EXIT )
				currentState = STATE_NORMAL;

			// Restore initial variable bindings
			target.restoreBindings();
			traceUnindent();

			return result;
		}

		public String toString()
		{	return target.getName() + "()";
		}
	}

	private class ScriptAssignment extends ScriptCommand
	{
		private ScriptVariableReference lhs;
		private ScriptExpression rhs;

		public ScriptAssignment( ScriptVariableReference lhs, ScriptExpression rhs )
		{
			this.lhs = lhs;
			this.rhs = rhs;

			if ( !validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
				throw new AdvancedScriptException( "Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() + " " + getLineAndFile() );
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return rhs;
		}

		public ScriptType getType()
		{
			return lhs.getType();
		}

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( "Eval: " + rhs );

			ScriptValue value = rhs.execute();
			captureValue( value );

			trace( "Set: " + value );
			traceUnindent();

			if ( currentState == STATE_EXIT )
				return null;

			if ( lhs.getType().equals( TYPE_STRING ) )
				lhs.setValue( value.toStringValue() );
			else if ( lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT ) )
				lhs.setValue( value.toIntValue() );
			else if ( lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT ) )
				lhs.setValue( value.toFloatValue() );
			else
				lhs.setValue( value );

			return VOID_VALUE;
		}

		public String toString()
		{	return lhs.getName() + " = " + rhs;
		}
	}

	private static class ScriptType extends ScriptSymbol
	{
		public boolean primitive;
		private int type;

		public ScriptType( String name, int type )
		{
			super( name );
			this.primitive = true;
			this.type = type;
		}

		public int getType()
		{	return type;
		}

		public ScriptType getBaseType()
		{	return this;
		}

		public boolean isPrimitive()
		{	return primitive;
		}

		public boolean equals( ScriptType type )
		{	return this.type == type.type;
		}

		public boolean equals( int type )
		{	return this.type == type;
		}

		public String toString()
		{	return name;
		}

		public ScriptType simpleType()
		{	return this;
		}

		public ScriptValue initialValue()
		{
			switch ( type )
			{
			case TYPE_VOID:
				return VOID_VALUE;
			case TYPE_BOOLEAN:
				return BOOLEAN_INIT;
			case TYPE_INT:
				return INT_INIT;
			case TYPE_FLOAT:
				return FLOAT_INIT;
			case TYPE_STRING:
				return STRING_INIT;
			case TYPE_ITEM:
				return ITEM_INIT;
			case TYPE_LOCATION:
				return LOCATION_INIT;
			case TYPE_CLASS:
				return CLASS_INIT;
			case TYPE_STAT:
				return STAT_INIT;
			case TYPE_SKILL:
				return SKILL_INIT;
			case TYPE_EFFECT:
				return EFFECT_INIT;
			case TYPE_FAMILIAR:
				return FAMILIAR_INIT;
			case TYPE_SLOT:
				return SLOT_INIT;
			case TYPE_MONSTER:
				return MONSTER_INIT;
			case TYPE_ELEMENT:
				return ELEMENT_INIT;
			}
			return null;
		}

		public ScriptValue parseValue( String name )
		{
			switch ( type )
			{
			case TYPE_BOOLEAN:
				return parseBooleanValue( name );
			case TYPE_INT:
				return parseIntValue( name );
			case TYPE_FLOAT:
				return parseFloatValue( name );
			case TYPE_STRING:
				return parseStringValue( name );
			case TYPE_ITEM:
				return parseItemValue( name );
			case TYPE_LOCATION:
				return parseLocationValue( name );
			case TYPE_CLASS:
				return parseClassValue( name );
			case TYPE_STAT:
				return parseStatValue( name );
			case TYPE_SKILL:
				return parseSkillValue( name );
			case TYPE_EFFECT:
				return parseEffectValue( name );
			case TYPE_FAMILIAR:
				return parseFamiliarValue( name );
			case TYPE_SLOT:
				return parseSlotValue( name );
			case TYPE_MONSTER:
				return parseMonsterValue( name );
			case TYPE_ELEMENT:
				return parseElementValue( name );
			}
			return null;
		}

		public ScriptExpression initialValueExpression()
		{	return initialValue();
		}

		public boolean containsAggregate()
		{	return false;
		}
	}

	private static class ScriptNamedType extends ScriptType
	{
                ScriptType base;

		public ScriptNamedType( String name, ScriptType base )
		{
			super( name, TYPE_TYPEDEF );
			this.base = base;
		}

		public ScriptType getBaseType()
		{	return base.getBaseType();
		}

		public ScriptExpression initialValueExpression()
		{	return new ScriptTypeInitializer( base.getBaseType() );
		}
	}

	private static class ScriptCompositeType extends ScriptType
	{
		public ScriptCompositeType( String name, int type )
		{
			super( name, type );
			this.primitive = false;
		}

		public ScriptType getIndexType()
		{	return null;
		}

		public ScriptType getDataType()
		{	return null;
		}

		public ScriptType getDataType( Object key )
		{	return null;
		}

		public ScriptValue getKey( ScriptValue key )
		{	return key;
		}

		public ScriptExpression initialValueExpression()
		{	return new ScriptTypeInitializer( this );
		}
        }

	private static class ScriptAggregateType extends ScriptCompositeType
	{
		private ScriptType dataType;
		private ScriptType indexType;
		private int size;

		// Map
		public ScriptAggregateType( ScriptType dataType, ScriptType indexType )
		{
			super( "aggregate", TYPE_AGGREGATE );
			this.dataType = dataType;
			this.indexType = indexType;
			this.size = 0;
		}

		// Array
		public ScriptAggregateType( ScriptType dataType, int size )
		{
			super( "aggregate", TYPE_AGGREGATE );
			this.primitive = false;
			this.dataType = dataType;
			this.indexType = INT_TYPE;
			this.size = size;
		}

		public ScriptType getDataType()
		{	return dataType;
		}

		public ScriptType getDataType( Object key )
		{	return dataType;
		}

		public ScriptType getIndexType()
		{	return indexType;
		}

		public int getSize()
		{	return size;
		}

		public boolean equals( ScriptType o )
		{
			return ( o instanceof ScriptAggregateType &&
				 dataType.equals( ((ScriptAggregateType)o).dataType ) &&
				 indexType.equals( ((ScriptAggregateType)o).indexType ) );
		}

		public ScriptType simpleType()
		{
			if ( dataType instanceof ScriptAggregateType )
				return dataType.simpleType();
			return dataType;
		}

		public String toString()
		{
			return simpleType().toString() + " [" + indexString() + "]";
		}

		public String indexString()
		{
			if ( dataType instanceof ScriptAggregateType )
			{
				String suffix = ", " + ((ScriptAggregateType)dataType).indexString();
				if ( size != 0 )
					return size + suffix;
				return indexType.toString() + suffix;
			}

			if ( size != 0 )
				return String.valueOf( size );
			return indexType.toString();
		}

		public ScriptValue initialValue()
		{
			if ( size != 0 )
				return new ScriptArray( this );
			return new ScriptMap( this );
		}

		public boolean containsAggregate()
		{	return true;
		}
	}

	private static class ScriptRecordType extends ScriptCompositeType
	{
		private String [] fieldNames;
		private ScriptType [] fieldTypes;
		private ScriptValue[] fieldIndices;

		public ScriptRecordType( String name, String [] fieldNames,ScriptType [] fieldTypes )
		{
			super( name, TYPE_RECORD );
			if ( fieldNames.length != fieldTypes.length )
				throw new AdvancedScriptException( "Internal error: wrong number of field types" );

			this.fieldNames = fieldNames;
			this.fieldTypes = fieldTypes;

			// Build field index values.
			// These can be either integers or strings.
			//   Integers don't require a lookup
			//   Strings make debugging easier.

			this.fieldIndices = new ScriptValue[ fieldNames.length ];
			for ( int i = 0; i < fieldNames.length; ++i )
				fieldIndices[i] = new ScriptValue( fieldNames[i] );
		}

		public String [] getFieldNames()
		{	return fieldNames;
                }

		public ScriptType [] getFieldTypes()
		{	return fieldTypes;
                }

		public ScriptValue [] getFieldIndices()
		{	return fieldIndices;
                }

		public int fieldCount()
		{	return fieldTypes.length;
                }

		public ScriptType getIndexType()
		{	return STRING_TYPE;
		}

		public ScriptType getDataType( Object key )
		{
                        if ( !( key instanceof ScriptValue ) )
				throw new RuntimeException( "Internal error: key is not a ScriptValue" );
			int index = indexOf( (ScriptValue)key );
			if ( index < 0 || index >= fieldTypes.length )
				return null;
			return fieldTypes[index];
		}

		public ScriptValue getFieldIndex( String field )
		{
			String val = field.toLowerCase();
			for ( int index = 0; index < fieldNames.length; ++ index )
				if ( val.equals( fieldNames[index] ) )
					return fieldIndices[index];
			return null;
		}

		public ScriptValue getKey( ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= fieldNames.length )
					return null;
				return fieldIndices[ index ];
			}

			if ( type.equals( TYPE_STRING ) )
			{
				String str = key.toString();
				for ( int index = 0; index < fieldNames.length; ++ index )
					if ( fieldNames[index].equals( str ) )
						return fieldIndices[ index ];
				return null;
			}

			return null;
		}

		public int indexOf( ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= fieldNames.length )
					return -1;
				return index;
			}

			if ( type.equals( TYPE_STRING ) )
			{
				for ( int index = 0; index < fieldNames.length; ++ index )
					if ( key == fieldIndices[index] )
						return index;
				return -1;
			}

			return -1;
		}

		public boolean equals( ScriptType o )
		{
			return ( o instanceof ScriptRecordType &&
				 name == ((ScriptRecordType)o).name );
		}

		public ScriptType simpleType()
		{	return this;
		}

		public String toString()
		{	return name;
		}

		public ScriptValue initialValue()
		{	return new ScriptRecord( this );
		}

		public boolean containsAggregate()
		{
			for ( int i = 0; i < fieldTypes.length; ++i )
				if ( fieldTypes[i].containsAggregate() )
					return true;
			return false;
		}
	}

	private static class ScriptTypeInitializer extends ScriptValue
	{
		public ScriptType type;

		public ScriptTypeInitializer( ScriptType type )
		{	this.type = type;
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptValue execute()
		{	return type.initialValue();
		}

		public String toString()
		{	return "<initial value>";
		}
	}

	private static class ScriptTypeList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptType n )
		{	return super.addElement( n );
		}

		public ScriptType findType( String name )
		{	return (ScriptType)super.findSymbol( name );
		}
	}

	private static class ScriptValue extends ScriptExpression implements Comparable
	{
		public ScriptType type;

		public int contentInt = 0;
		public float contentFloat = 0.0f;
		public String contentString = null;
		public Object content = null;

		public ScriptValue()
		{	this.type = VOID_TYPE;
		}

		public ScriptValue( int value )
		{
			this.type = INT_TYPE;
			this.contentInt = value;
		}

		public ScriptValue( boolean value )
		{
			this.type = BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( String value )
		{
			this.type = STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( float value )
		{
			this.type = FLOAT_TYPE;
			this.contentInt = (int) value;
			this.contentFloat = value;
		}

		public ScriptValue( ScriptType type )
		{
			this.type = type;
		}

		public ScriptValue( ScriptType type, int contentInt, String contentString )
		{
			this.type = type;
			this.contentInt = contentInt;
			this.contentString = contentString;
		}

		public ScriptValue( ScriptType type, String contentString, Object content )
		{
			this.type = type;
			this.contentString = contentString;
			this.content = content;
		}

		public ScriptValue( ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public ScriptValue toFloatValue()
		{
			if ( type.equals( TYPE_FLOAT ) )
				return this;
			else
				return new ScriptValue( (float) contentInt );
		}

		public ScriptValue toIntValue()
		{
			if ( type.equals( TYPE_INT ) )
				return this;
			else
				return new ScriptValue( (int) contentFloat );
		}

		public ScriptType getType()
		{
			return type.getBaseType();
		}

		public String toString()
		{
			if ( type.equals( TYPE_VOID ) )
				return "void";

			if ( contentString != null )
				return contentString;

			if ( type.equals( TYPE_BOOLEAN ) )
				return String.valueOf( contentInt != 0 );

			if ( type.equals( TYPE_FLOAT ) )
				return String.valueOf( contentFloat );

			return String.valueOf( contentInt );
		}

		public String toQuotedString()
		{
			if ( contentString != null )
				return "\"" + contentString + "\"";
			return toString();
		}

		public ScriptValue toStringValue()
		{
			return new ScriptValue( toString() );
		}

		public Object rawValue()
		{
			return content;
		}

		public int intValue()
		{
			return contentInt;
		}

		public float floatValue()
		{
			return contentFloat;
		}

		public ScriptValue execute()
		{
			return this;
		}

		public int compareTo( Object o )
		{
			if ( !( o instanceof ScriptValue ) )
				throw new ClassCastException();

			ScriptValue it = (ScriptValue) o;

			if ( type == BOOLEAN_TYPE || type == INT_TYPE )
				return contentInt < it.contentInt ? -1 : contentInt == it.contentInt ? 0 : 1;

			if ( type == FLOAT_TYPE )
				return contentFloat < it.contentFloat ? -1 : contentFloat == it.contentFloat ? 0 : 1;

			if ( contentString != null )
				return contentString.compareTo( it.contentString );

			return -1;
		}

		public int count()
		{	return 1;
		}

		public void clear()
		{
		}

		public boolean contains( ScriptValue index )
		{	return false;
		}

		public boolean equals( Object o )
		{	return o == null || !(o instanceof ScriptValue) ? false : this.compareTo( (Comparable) o ) == 0;
		}

		public void dumpValue( PrintStream writer )
		{	writer.print( toStringValue().toString() );
		}

		public void dump( PrintStream writer, String prefix, boolean compact )
		{	writer.println( prefix + toStringValue().toString() );
		}
	}

	private static class ScriptCompositeValue extends ScriptValue
	{
		public ScriptCompositeValue( ScriptCompositeType type )
		{	super( type );
		}

		public ScriptCompositeType getCompositeType()
		{	return (ScriptCompositeType)type;
		}

		public ScriptValue aref( ScriptValue key )
		{	return null;
		}

		public void aset( ScriptValue key, ScriptValue val )
		{
		}

		public ScriptValue remove( ScriptValue key )
		{	return null;
		}

		public void clear()
		{
		}

		public ScriptValue [] keys()
		{	return new ScriptValue[0];
		}

		public ScriptValue initialValue( Object key )
		{	return ((ScriptCompositeType)type).getDataType( key ).initialValue();
		}

		public void dump( PrintStream writer, String prefix, boolean compact )
		{
			ScriptValue [] keys = keys();
			if ( keys.length == 0 )
				return;

			for ( int i = 0; i < keys.length; ++i )
			{
				ScriptValue key = keys[i];
				ScriptValue value = aref( key );
				String first = prefix + key + "\t";
				value.dump( writer, first, compact );
			}
		}

		public void dumpValue( PrintStream writer )
		{
		}

		// Returns number of fields consumed
		public int read( String [] data, int index, boolean compact )
		{
			ScriptCompositeType type = (ScriptCompositeType) this.type;
			ScriptValue key = null;

			if ( index < data.length )
				key = type.getKey( parseValue( type.getIndexType(), data[ index ] ) );
			else
				key = type.getKey( parseValue( type.getIndexType(), "none" ) );

			// If there's only a key and a value, parse the value
			// and store it in the composite

			if ( !(type.getDataType( key ) instanceof ScriptCompositeType) )
			{
				aset( key, parseValue( type.getDataType( key ), data[ index + 1 ] ) );
				return 2;
			}

			// Otherwise, recurse until we get the final slice
			ScriptCompositeValue slice = (ScriptCompositeValue) aref( key );

			// Create missing intermediate slice
			if ( slice == null )
			{
				slice = (ScriptCompositeValue) initialValue( key );
				aset( key, slice );
			}

			return slice.read( data, index + 1, compact ) + 1;
		}

		public String toString()
		{	return "composite " + type.toString();
		}
	}

	private static class ScriptAggregateValue extends ScriptCompositeValue
	{
		public ScriptAggregateValue( ScriptAggregateType type )
		{	super( type );
		}

		public ScriptType getDataType()
		{	return ((ScriptAggregateType)type).getDataType();
		}

		public int count()
		{	return 0;
		}

		public boolean contains( ScriptValue index )
		{	return false;
		}

		public String toString()
		{	return "aggregate " + type.toString();
		}
	}

	private static class ScriptArray extends ScriptAggregateValue
	{
		public ScriptArray( ScriptAggregateType type )
		{
			super( type );

			int size = type.getSize();
			ScriptType dataType = type.getDataType();
			ScriptValue [] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
				content[i] = dataType.initialValue();
			this.content = content;
		}

		public ScriptValue aref( ScriptValue index )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int i = index.intValue();
			if ( i < 0 || i > array.length )
				throw new AdvancedScriptException( "Array index out of bounds" );
			return array[ i ];
		}

		public void aset( ScriptValue key,  ScriptValue val )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
				throw new AdvancedScriptException( "Array index out of bounds" );

			if ( array[ index ].getType().equals( val.getType() ) )
				array[ index ] = val;
			else if ( array[ index ].getType().equals( TYPE_STRING ) )
				array[ index ] = val.toStringValue();
			else if ( array[ index ].getType().equals( TYPE_INT ) && val.getType().equals( TYPE_FLOAT ) )
				array[ index ] = val.toIntValue();
			else if ( array[ index ].getType().equals( TYPE_FLOAT ) && val.getType().equals( TYPE_INT ) )
				array[ index ] = val.toFloatValue();
			else
				throw new RuntimeException( "Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
		}

		public ScriptValue remove( ScriptValue key )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
				throw new AdvancedScriptException( "Array index out of bounds" );
			ScriptValue result = array[ index ];
			array[ index ] = getDataType().initialValue();
			return result;
		}

		public void clear()
		{
			ScriptValue [] array = (ScriptValue [])content;
			for ( int index = 0; index < array.length; ++index )
				array[ index ] = getDataType().initialValue();
		}

		public int count()
		{
			ScriptValue [] array = (ScriptValue [])content;
			return array.length;
		}

		public boolean contains( ScriptValue key )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int index = key.intValue();
			return ( index >= 0 && index < array.length );
		}

		public ScriptValue [] keys()
		{
			int size = ((ScriptValue [])content).length;
			ScriptValue [] result = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
				result[i] = new ScriptValue(i);
			return result;
		}
	}

	private static class ScriptMap extends ScriptAggregateValue
	{
		public ScriptMap( ScriptAggregateType type )
		{
			super( type );
			this.content = new TreeMap();
		}

		public ScriptValue aref( ScriptValue key )
		{
			TreeMap map = (TreeMap)content;
			return (ScriptValue)map.get( key );
		}

		public void aset( ScriptValue key, ScriptValue val )
		{
			TreeMap map = (TreeMap)content;

			if ( !getDataType().equals( val.getType() ) )
			{
				if ( getDataType().equals( TYPE_STRING ) )
					val = val.toStringValue();
				else if ( getDataType().equals( TYPE_INT ) && val.getType().equals( TYPE_FLOAT ) )
					val = val.toIntValue();
				else if ( getDataType().equals( TYPE_FLOAT ) && val.getType().equals( TYPE_INT ) )
					val = val.toFloatValue();
			}

			map.put( key, val );
		}

		public ScriptValue remove( ScriptValue key )
		{
			TreeMap map = (TreeMap)content;
			return (ScriptValue)map.remove( key );
		}

		public void clear()
		{
			TreeMap map = (TreeMap)content;
			map.clear();
		}

		public int count()
		{
			TreeMap map = (TreeMap)content;
			return map.size();
		}

		public boolean contains( ScriptValue key )
		{
			TreeMap map = (TreeMap)content;
			return map.containsKey( key );
		}

		public ScriptValue [] keys()
		{
			Set set = ((TreeMap)content).keySet();
			ScriptValue [] keys = new ScriptValue[ set.size() ];
			set.toArray( keys );
			return keys;
		}
	}

	private static class ScriptRecord extends ScriptCompositeValue
	{
		public ScriptRecord( ScriptRecordType type )
		{
			super( type );

			ScriptType [] dataTypes = type.getFieldTypes();
                        int size = dataTypes.length;
			ScriptValue [] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
				content[i] = dataTypes[i].initialValue();
			this.content = content;
		}

		public ScriptRecordType getRecordType()
		{	return (ScriptRecordType)type;
		}

		public ScriptType getDataType( ScriptValue key )
		{	return ((ScriptRecordType)type).getDataType( key );
		}

		public ScriptValue aref( ScriptValue key )
		{
			int index = ((ScriptRecordType)type).indexOf( key );
			if ( index < 0 )
				throw new RuntimeException( "Internal error: field index out of bounds" );
			ScriptValue [] array = (ScriptValue [])content;
			return array[ index ];
		}

		public ScriptValue aref( int index )
		{
			ScriptRecordType type = (ScriptRecordType)this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
				throw new RuntimeException( "Internal error: field index out of bounds" );
			ScriptValue [] array = (ScriptValue [])content;
			return array[ index ];
		}

		public void aset( ScriptValue key, ScriptValue val )
		{
			int index = ((ScriptRecordType)type).indexOf( key );
			if ( index < 0 )
				throw new RuntimeException( "Internal error: field index out of bounds" );

			aset( index, val );
		}

		public void aset( int index, ScriptValue val )
		{
			ScriptRecordType type = (ScriptRecordType)this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
				throw new RuntimeException( "Internal error: field index out of bounds" );

			ScriptValue [] array = (ScriptValue []) content;

			if ( array[ index ].getType().equals( val.getType() ) )
				array[ index ] = val;
			else if ( array[ index ].getType().equals( TYPE_STRING ) )
				array[ index ] = val.toStringValue();
			else if ( array[ index ].getType().equals( TYPE_INT ) && val.getType().equals( TYPE_FLOAT ) )
				array[ index ] = val.toIntValue();
			else if ( array[ index ].getType().equals( TYPE_FLOAT ) && val.getType().equals( TYPE_INT ) )
				array[ index ] = val.toFloatValue();
			else
				throw new RuntimeException( "Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
		}

		public ScriptValue remove( ScriptValue key )
		{
			int index = ((ScriptRecordType)type).indexOf( key );
			if ( index < 0 )
				throw new RuntimeException( "Internal error: field index out of bounds" );
			ScriptValue [] array = (ScriptValue [])content;
			ScriptValue result = array[ index ];
			array[ index ] = getDataType( key ).initialValue();
			return result;
		}

		public void clear()
		{
			ScriptType [] dataTypes = ((ScriptRecordType)type).getFieldTypes();
			ScriptValue [] array = (ScriptValue [])content;
			for ( int index = 0; index < array.length; ++index )
				array[ index ] = dataTypes[index].initialValue();
		}

		public ScriptValue [] keys()
		{	return ((ScriptRecordType)type).getFieldIndices();
		}

		public void dump( PrintStream writer, String prefix, boolean compact )
		{
			if ( !compact || type.containsAggregate() )
			{
				super.dump( writer, prefix, compact );
				return;
			}

			writer.print( prefix );
			dumpValue( writer );
			writer.println();
		}

		public void dumpValue( PrintStream writer )
		{
			int size = ((ScriptRecordType)type).getFieldTypes().length;
			for ( int i = 0; i < size; ++i )
			{
				ScriptValue value = aref( i );
				if ( i > 0  )
					writer.print( "\t" );
				value.dumpValue( writer );
			}
		}

		public int read( String [] data, int index, boolean compact )
		{
			if ( !compact || type.containsAggregate() )
				return super.read( data, index, compact );

			ScriptType [] dataTypes = ((ScriptRecordType)this.type).getFieldTypes();
			ScriptValue [] array = (ScriptValue []) content;

			int size = Math.min( dataTypes.length, data.length - index );
			int first = index;

			// Consume remaining data values and store them
			for ( int offset = 0; offset < size; ++offset )
			{
				ScriptType valType = dataTypes[offset];
				if ( valType instanceof ScriptRecordType )
				{
					ScriptRecord rec = (ScriptRecord)array[offset];
					index += rec.read( data, index, true );
				}
				else
				{
					array[offset] = parseValue( valType, data[ index ] );
					index += 1;
				}
			}

			for ( int offset = size; offset < dataTypes.length; ++offset )
				array[offset] = parseValue( dataTypes[offset], "none" );

			// assert index == data.length
			return index - first;
		}

		public String toString()
		{	return "record " + type.toString();
		}
	}

	private static class ScriptExpression extends ScriptCommand
	{
		ScriptExpression lhs;
		ScriptExpression rhs;
		ScriptOperator oper;

		public ScriptExpression()
		{
		}

		public ScriptExpression( ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper )
		{
			this.lhs = lhs;
			this.rhs = rhs;
			this.oper = oper;
		}

		public ScriptType getType()
		{
			ScriptType leftType = lhs.getType();

			// Unary operators have no right hand side
			if ( rhs == null )
				return leftType;

			ScriptType rightType = rhs.getType();

			// String concatenation always yields a string
			if ( oper.equals( "+" ) && ( leftType.equals( TYPE_STRING ) || rightType.equals( TYPE_STRING ) ) )
				return STRING_TYPE;

			// If it's not arithmetic, it's boolean
			if ( !oper.isArithmetic() )
				return BOOLEAN_TYPE;

			// Coerce int to float
			if ( leftType.equals( TYPE_FLOAT ) ) // int ( oper ) float evaluates to float.
				return FLOAT_TYPE;

			// Otherwise result is whatever is on right
			return rightType;
		}

		public ScriptExpression getLeftHandSide()
		{
			return lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return rhs;
		}

		public ScriptOperator getOperator()
		{
			return oper;
		}

		public ScriptValue execute()
		{	return oper.applyTo( lhs, rhs );
		}

		public String toString()
		{
			if ( rhs == null )
				return oper.toString() + " " + lhs.toQuotedString();
			return "( " + lhs.toQuotedString() + " " + oper.toString() + " " + rhs.toQuotedString() + " )";
		}

		public String toQuotedString()
		{	return toString();
		}
	}

	private static class ScriptExpressionList extends ScriptList
	{
		public Iterator getExpressions()
		{	return iterator();
		}
	}

	private class ScriptOperator
	{
		String operator;

		public ScriptOperator( String operator )
		{
			if ( operator == null )
				throw new RuntimeException( "Internal error in ScriptOperator()" );

			this.operator = operator;
		}

		public boolean equals( String op )
		{	return operator.equals( op );
		}

		public boolean precedes( ScriptOperator oper )
		{
			return operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if ( operator.equals( "!" ) || operator.equals( "contains" ) || operator.equals( "remove" ) )
				return 7;

			if ( operator.equals( "^" ) )
				return 6;

			if ( operator.equals( "*" ) || operator.equals( "/" ) || operator.equals( "%" ) )
				return 5;

			if ( operator.equals( "+" ) || operator.equals( "-" ) )
				return 4;

			if ( operator.equals( "<" ) || operator.equals( ">" ) || operator.equals( "<=" ) || operator.equals( ">=" ) )
				return 3;

			if ( operator.equals( "=" ) || operator.equals( "==" ) || operator.equals( "!=" ) )
				return 2;

			if ( operator.equals( "||" ) || operator.equals( "&&" ) )
				return 1;

			return -1;
		}

		public boolean isArithmetic()
		{	return	operator.equals( "+" ) ||
				operator.equals( "-" ) ||
				operator.equals( "*" ) ||
				operator.equals( "^" ) ||
				operator.equals( "/" ) ||
				operator.equals( "%" );
		}

		public String toString()
		{	return operator;
		}

		public ScriptValue applyTo( ScriptExpression lhs, ScriptExpression rhs )
		{
			// Unary operator with special evaluation of argument
			if ( operator.equals( "remove" ) )
				return ((ScriptCompositeReference)lhs).removeKey();

			ScriptValue leftValue = lhs.execute();
			captureValue( leftValue );
			if ( currentState == STATE_EXIT )
				return null;

			// Unary Operators
			if ( operator.equals( "!" ) )
				return new ScriptValue( leftValue.intValue() == 0 );

			if ( operator.equals( "-" ) && rhs == null )
			{
				if ( lhs.getType().equals( TYPE_INT ) )
					return new ScriptValue( 0 - leftValue.intValue() );
				if ( lhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( 0.0f - leftValue.floatValue() );
				throw new RuntimeException( "Unary minus can only be applied to numbers" );
			}

			// Unknown operator
			if ( rhs == null )
				throw new RuntimeException( "Internal error: missing right operand." );

			// Binary operators with optional right values
			if ( operator.equals( "||" ) )
			{
				if ( leftValue.intValue() == 1 )
					return TRUE_VALUE;
				ScriptValue rightValue = rhs.execute();
				captureValue( rightValue );
				if ( currentState == STATE_EXIT )
					return null;
				return rightValue;
			}

			if ( operator.equals( "&&" ) )
			{
				if ( leftValue.intValue() == 0 )
					return FALSE_VALUE;
				ScriptValue rightValue = rhs.execute();
				captureValue( rightValue);
				if ( currentState == STATE_EXIT )
					return null;
				return rightValue;
			}

			// Ensure type compatibility of operands
			if ( !validCoercion( lhs.getType(), rhs.getType(), operator ) )
				throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );

			// Special binary operator: <aggref> contains <any>
			if ( operator.equals( "contains" ) )
			{
				ScriptValue rightValue = rhs.execute();
				captureValue( rightValue);
				if ( currentState == STATE_EXIT )
					return null;
				return new ScriptValue( leftValue.contains( rightValue) );
			}

			// Binary operators
			ScriptValue rightValue = rhs.execute();
			captureValue( rightValue );
			if ( currentState == STATE_EXIT )
				return null;

			// String operators
			if ( operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) || rhs.getType().equals( TYPE_STRING ) )
					return new ScriptValue( leftValue.toStringValue().toString() + rightValue.toStringValue().toString() );
			}

			if ( operator.equals( "=" ) || operator.equals( "==" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) ||
				     lhs.getType().equals( TYPE_LOCATION ) ||
				     lhs.getType().equals( TYPE_MONSTER ) )
					return new ScriptValue( leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
			}

			if ( operator.equals( "!=" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) ||
				     lhs.getType().equals( TYPE_LOCATION ) ||
				     lhs.getType().equals( TYPE_MONSTER ) )
					return new ScriptValue( !leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
			}

			// Arithmetic operators
			boolean isInt;
			float lfloat = 0.0f, rfloat = 0.0f;
			int lint = 0, rint = 0;

			if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
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

			if ( operator.equals( "+" ) )
			{
				if ( isInt )
					return new ScriptValue( lint + rint );
				return new ScriptValue( lfloat + rfloat );
			}

			if ( operator.equals( "-" ) )
			{
				if ( isInt )
					return new ScriptValue( lint - rint );
				return new ScriptValue( lfloat - rfloat );
			}

			if ( operator.equals( "*" ) )
			{
				if ( isInt )
					return new ScriptValue( lint * rint );
				return new ScriptValue( lfloat * rfloat );
			}

			if ( operator.equals( "^" ) )
			{
				if ( isInt )
					return new ScriptValue( (int) Math.pow( lint, rint ) );
				return new ScriptValue( (float) Math.pow( lfloat, rfloat ) );
			}

			if ( operator.equals( "/" ) )
			{
				if ( isInt )
					return new ScriptValue( ((float)lint) / ((float)rint) );
				return new ScriptValue( lfloat / rfloat );
			}

			if ( operator.equals( "%" ) )
			{
				if ( isInt )
					return new ScriptValue( lint % rint );
				return new ScriptValue( lfloat % rfloat );
			}

			if ( operator.equals( "<" ) )
			{
				if ( isInt )
					return new ScriptValue( lint < rint );
				return new ScriptValue( lfloat < rfloat );
			}

			if ( operator.equals( ">" ) )
			{
				if ( isInt )
					return new ScriptValue( lint > rint );
				return new ScriptValue( lfloat > rfloat );
			}

			if ( operator.equals( "<=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint <= rint );
				return new ScriptValue( lfloat <= rfloat );
			}

			if ( operator.equals( ">=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint >= rint );
				return new ScriptValue( lfloat >= rfloat );
			}

			if ( operator.equals( "=" ) || operator.equals( "==" ) )
			{
				if ( isInt )
					return new ScriptValue( lint == rint );
				return new ScriptValue( lfloat == rfloat );
			}

			if ( operator.equals( "!=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint != rint );
				return new ScriptValue( lfloat != rfloat );
			}

			// Unknown operator
			throw new RuntimeException( "Internal error: illegal operator \"" + operator + "\"" );
		}
	}

	private static class ScriptList extends ArrayList
	{
		public boolean addElement( Object n )
		{
			add( n );
			return true;
		}
	}

	public static class AdvancedScriptException extends RuntimeException
	{
		AdvancedScriptException( Throwable t )
		{	this( t.getMessage() == null ? "" : t.getMessage() );
		}

		AdvancedScriptException( String s )
		{	super( s == null ? "" : s );
		}
	}
}
