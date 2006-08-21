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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

// input and output
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.IllegalArgumentException;
import java.lang.NumberFormatException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

// utility imports
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Set;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.DataUtilities;

//Parameter value requests
import javax.swing.JOptionPane;

public class KoLmafiaASH extends StaticEntity
{
	/* Variables for Advanced Scripting */
	public final static char [] tokenList = { ' ', '.', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '*', '^', '/', '%', '[', ']', '!', ';', '<', '>' };
	public final static String [] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&" };

	public static final int TYPE_VOID = 0;
	public static final int TYPE_BOOLEAN = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int TYPE_STRING = 4;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_ZODIAC = 101;
	public static final int TYPE_LOCATION = 102;
	public static final int TYPE_CLASS = 103;
	public static final int TYPE_STAT = 104;
	public static final int TYPE_SKILL = 105;
	public static final int TYPE_EFFECT = 106;
	public static final int TYPE_FAMILIAR = 107;
	public static final int TYPE_SLOT = 108;
	public static final int TYPE_MONSTER = 109;
	public static final int TYPE_ELEMENT = 110;

	public static final int TYPE_AGGREGATE = 1000;
	public static final int TYPE_RECORD = 1001;

	public static final String [] ZODIACS = { "none", "Wallaby", "Mongoose", "Vole", "Platypus", "Opossum", "Marmot", "Wombat", "Blender", "Packrat" };
	public static final String [] CLASSES = { "Seal Clubber", "Turtle Tamer", "Pastamancer", "Sauceror", "Disco Bandit", "Accordion Thief" };
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

	private static final String escapeString = "//";

	private static final ScriptType VOID_TYPE = new ScriptType( "void", TYPE_VOID );
	private static final ScriptType BOOLEAN_TYPE = new ScriptType( "boolean", TYPE_BOOLEAN );
	private static final ScriptType INT_TYPE = new ScriptType( "int", TYPE_INT );
	private static final ScriptType FLOAT_TYPE = new ScriptType( "float", TYPE_FLOAT );
	private static final ScriptType STRING_TYPE = new ScriptType( "string", TYPE_STRING );

	private static final ScriptType ITEM_TYPE = new ScriptType( "item", TYPE_ITEM );
	private static final ScriptType ZODIAC_TYPE = new ScriptType( "zodiac", TYPE_ZODIAC );
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

	// Common values

	private static final ScriptValue VOID_VALUE = new ScriptValue();
	private static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	private static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	private static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	private static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	private static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0 );

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	private static final ScriptValue BOOLEAN_INIT = FALSE_VALUE;
	private static final ScriptValue INT_INIT = ZERO_VALUE;
	private static final ScriptValue FLOAT_INIT = ZERO_FLOAT_VALUE;
	private static final ScriptValue STRING_INIT = new ScriptValue( "" );
	private static final ScriptValue ITEM_INIT = new ScriptValue( ITEM_TYPE, -1, "none" );
	private static final ScriptValue ZODIAC_INIT = new ScriptValue( ZODIAC_TYPE, 0, "none" );
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

	private static ArrayList imports = new ArrayList();
	public LineNumberReader commandStream;
	public String fileName;
	private String line;
	private String nextLine;
	private int lineNumber;

	// Variables used during execution
	private ScriptScope global;
	public int currentState = STATE_NORMAL;

	// Feature control;

	// Disabled until and if we choose to document the feature
	private static boolean arrays = false;

	// **************** Data Types *****************

	// For each simple data type X, we supply:
	//
	// private static ScriptValue parseXValue( String name );
	//    throws IllegalArgumentException if can't parse

	private static ScriptValue parseBooleanValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "true" ) )
			return TRUE_VALUE;
		if ( name.equalsIgnoreCase( "false" ) )
			return FALSE_VALUE;
		throw new IllegalArgumentException( "Can't interpret '" + name + "' as a boolean" );
	}

	private static ScriptValue parseIntValue( String name ) throws NumberFormatException
	{	return new ScriptValue( StaticEntity.parseInt( name ) );
	}

	private static ScriptValue parseFloatValue( String name ) throws NumberFormatException
	{	return new ScriptValue( StaticEntity.parseDouble( name ) );
	}

	private static ScriptValue parseStringValue( String name )
	{	return new ScriptValue( name );
	}

	private static ScriptValue parseItemValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return ITEM_INIT;

		// Allow for an item number to be specified
		// inside of the "item" construct.

		int itemID;
		for ( int i = 0; i < name.length(); ++i )
		{
			// If you get an actual item number, then store it
			// inside of contentInt and return from the method.
			// But, in this case, we're testing if it's not an item
			// number -- use substring matching to make it
			// user-friendlier.

			if ( !Character.isDigit( name.charAt(i) ) )
			{
				AdventureResult item = DEFAULT_SHELL.getFirstMatchingItem( name );

				// Otherwise, throw an AdvancedScriptException
				// so that an unsuccessful parse happens before
				// the script gets executed (consistent with
				// paradigm).

				if ( item == null )
					throw new IllegalArgumentException( "Item " + name + " not found in database" );

				itemID = item.getItemID();
				name = TradeableItemDatabase.getItemName( itemID );
				return new ScriptValue( ITEM_TYPE, itemID, name );
			}
		}

		// Since it is numeric, parse the integer value
		// and store it inside of the contentInt.

		itemID = StaticEntity.parseInt( name );
		name = TradeableItemDatabase.getItemName( itemID );
		return new ScriptValue( ITEM_TYPE, itemID, name );
	}

	private static int zodiacToInt( String name )
	{
		for ( int i = 0; i < ZODIACS.length; ++i )
			if ( name.equalsIgnoreCase( ZODIACS[i] ) )
				return i;
		return -1;
	}

	private static ScriptValue parseZodiacValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return ZODIAC_INIT;

		int num = zodiacToInt( name );
		if ( num < 0 )
			throw new IllegalArgumentException( "Unknown zodiac " + name );
		return new ScriptValue( ZODIAC_TYPE, num, ZODIACS[num] );
	}

	private static ScriptValue parseLocationValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return LOCATION_INIT;

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
			throw new IllegalArgumentException( "Location " + name + " not found in database" );
		return new ScriptValue( LOCATION_TYPE, name, (Object) content );
	}

	private static int classToInt( String name )
	{
		for ( int i = 0; i < CLASSES.length; ++i )
			if ( name.equalsIgnoreCase( CLASSES[i] ) )
				return i;
		return -1;
	}

	private static ScriptValue parseClassValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return CLASS_INIT;

		int num = classToInt( name );
		if ( num < 0 )
			throw new IllegalArgumentException( "Unknown class " + name );
		return new ScriptValue( CLASS_TYPE, num, CLASSES[num] );
	}

	private static int statToInt( String name )
	{
		for ( int i = 0; i < STATS.length; ++i )
			if ( name.equalsIgnoreCase( STATS[i] ) )
				return i;
		return -1;
	}

	private static ScriptValue parseStatValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return STAT_INIT;

		int num = statToInt( name );
		if ( num < 0 )
			throw new IllegalArgumentException( "Unknown stat " + name );
		return new ScriptValue( STAT_TYPE, num, STATS[num] );
	}

	private static ScriptValue parseSkillValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return SKILL_INIT;

		List skills = ClassSkillsDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
			throw new IllegalArgumentException( "Skill " + name + " not found in database" );

		int num = ClassSkillsDatabase.getSkillID( (String)skills.get(0) );
		name = ClassSkillsDatabase.getSkillName( num );
		return new ScriptValue( SKILL_TYPE, num, name );
	}

	private static ScriptValue parseEffectValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return EFFECT_INIT;

		AdventureResult effect = DEFAULT_SHELL.getFirstMatchingEffect( name );
		if ( effect == null )
			throw new IllegalArgumentException( "Effect " + name + " not found in database" );

		int num = StatusEffectDatabase.getEffectID( effect.getName() );
		name = StatusEffectDatabase.getEffectName( num );
		return new ScriptValue( EFFECT_TYPE, num, name );
	}

	private static ScriptValue parseFamiliarValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return FAMILIAR_INIT;

		int num = FamiliarsDatabase.getFamiliarID( name );
		if ( num == -1 )
			throw new IllegalArgumentException( "Familiar " + name + " not found in database" );

		name = FamiliarsDatabase.getFamiliarName( num );
		return new ScriptValue( FAMILIAR_TYPE, num, name );
	}

	private static ScriptValue parseSlotValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return SLOT_INIT;

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
			throw new IllegalArgumentException( "Bad slot name " + name );
		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( SLOT_TYPE, num, name );
	}

	private static ScriptValue parseMonsterValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return MONSTER_INIT;

		MonsterDatabase.Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
			throw new IllegalArgumentException( "Bad monster name " + name );
		return new ScriptValue( MONSTER_TYPE, name, (Object)monster );
	}

	private static ScriptValue parseElementValue( String name ) throws IllegalArgumentException
	{
		if ( name.equalsIgnoreCase( "none" ) )
			return ELEMENT_INIT;

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
			throw new IllegalArgumentException( "Bad element name " + name );
		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( ELEMENT_TYPE, num, name );
	}

	private static ScriptValue parseValue( ScriptType type, String name ) throws AdvancedScriptException
	{
		try
		{
			return type.parseValue( name );
		}
		catch ( IllegalArgumentException e )
		{
			throw new AdvancedScriptException( e.getMessage() );
		}
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
		int num = TradeableItemDatabase.getItemID( name );

		if ( num == -1 )
			return ITEM_INIT;

		return new ScriptValue( ITEM_TYPE, num, name );
	}

	private static ScriptValue makeZodiacValue( String name )
	{
		return new ScriptValue( ZODIAC_TYPE, zodiacToInt( name ), name );
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

	private static void trace( String string )
	{
		if ( tracing )
		{
			indentLine( traceIndentation );
			KoLmafia.getDebugStream().println( string );
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

	public void validate( File scriptFile ) throws IOException
	{
		this.commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );
		this.fileName = scriptFile.getPath();
		this.imports.clear();

		this.line = getNextLine();
		this.lineNumber = commandStream.getLineNumber();
		this.nextLine = getNextLine();

		try
		{
			this.global = parseScope( null, null, new ScriptVariableList(), getExistingFunctionScope(), false );

			if ( this.line != null )
				throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

			this.commandStream.close();
			printScope( global, 0 );
		}
		catch ( AdvancedScriptException e )
		{
			this.commandStream.close();
			this.commandStream = null;

			// Only error message, not stack trace, for a parse error
			KoLmafia.updateDisplay( e.getMessage() );
		}
	}

	public void execute( File scriptFile ) throws IOException
	{
		// Before you do anything, validate the script.
		validate( scriptFile );

		if ( this.commandStream == null )
			return;

		try
		{
			ScriptValue result = executeGlobalScope( global );

			if ( !KoLmafia.permitsContinue() || result == null || result.getType() == null )
			{
				KoLmafiaCLI.printLine( "Script aborted!" );
				return;
			}

			if ( result.getType().equals( TYPE_VOID ) )
				KoLmafiaCLI.printLine( !KoLmafia.permitsContinue() ? "Script failed!" : "Script succeeded!" );
			else if ( result.getType().equals( TYPE_BOOLEAN ) )
				KoLmafiaCLI.printLine( result.intValue() == 0 ? "Script failed!" : "Script succeeded!" );
			else if ( result.getType().equals( TYPE_STRING ) )
				KoLmafiaCLI.printLine( result.toString() );
			else
				KoLmafiaCLI.printLine(  "Script returned value " + result );

		}
		catch ( AdvancedScriptException e )
		{
			printStackTrace( e, e.getMessage() );
		}
		catch ( RuntimeException e )
		{
			// If it's an exception resulting from
			// a premature abort, which causes void
			// values to be return, ignore.

			if ( !e.getMessage().startsWith( "Cannot" ) )
				printStackTrace( e, e.getMessage() );
		}
	}

	private String getNextLine()
	{
		try
		{
			String line;

			do
			{
				// Read a line from input, and break out of the do-while
				// loop when you've read a valid line (which is a non-comment
				// and a non-blank line ) or when you've reached EOF.

				line = commandStream.readLine();
			}
			while ( line != null && (line.trim().length() == 0 || line.trim().startsWith( "#" ) || line.trim().startsWith( "//" ) || line.trim().startsWith( "\'" )) );

			// You will either have reached the end of file, or you will
			// have a valid line -- return it.

			return line == null ? null : line.trim();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
			return null;
		}
	}

	private ScriptScope parseFile( String fileName, ScriptScope startScope, ScriptScope parentScope ) throws AdvancedScriptException, java.io.FileNotFoundException
	{
		ScriptScope result;
		this.fileName = fileName;

		File scriptFile = new File( "scripts/" + fileName );
		if ( !scriptFile.exists() )
			scriptFile = new File( "scripts/" + fileName + ".ash" );

		if ( scriptFile.exists() )
		{
			String name = scriptFile.toString();
			if ( imports.contains( name ) )
				return startScope;
			imports.add( name );
		}

		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );

		line = getNextLine();
		lineNumber = commandStream.getLineNumber();
		nextLine = getNextLine();

		result = parseScope( startScope, null, new ScriptVariableList(), parentScope, false );

		try
		{
			commandStream.close();
		}
		catch ( IOException e )
		{
		}

		if ( line != null )
			throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

		return result;
	}

	private ScriptScope parseScope( ScriptScope startScope, ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop ) throws AdvancedScriptException
	{
		ScriptScope result;
		String importString;

		result = startScope == null ? new ScriptScope( variables, parentScope ) : startScope;

		while ( (importString = parseImport()) != null )
		{
			try
			{
				result = new KoLmafiaASH().parseFile( importString, result, parentScope );
			}
			catch( java.io.FileNotFoundException e )
			{
				throw new AdvancedScriptException( "File " + importString + " not found " + getLineAndFile() );
			}
		}

		while ( true )
		{
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
				// People want to code scripts that work either
				// standalone or imported into another script
				//
				// Therefore remove "main" functions that are
				// defined in non-toplevel scopes
				//
				// We could just leave them; we only look for a
				// "main" function in the outermost scope
				if ( startScope != null && f.getName().equalsIgnoreCase( "main" ) )
					// throw new AdvancedScriptException( "Only outer script can define 'main' function " + getLineAndFile() );
					result.removeFunction( f );
				continue;
			}

			ScriptVariable v = parseVariable( t, result );
			if ( v != null )
			{
				if ( !currentToken().equals( ";" ) )
					throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );
				readToken(); //read ;
				continue;
			}

			//Found a type but no function or variable to tie it to
			throw new AdvancedScriptException( "Script parse error " + getLineAndFile() );
		}

		return result;
	}

	private ScriptType parseRecord( ScriptScope parentScope ) throws AdvancedScriptException
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
				throw new AdvancedScriptException( "Invalid record name: '" + recordName + "' " + getLineAndFile() );

			if ( isReservedWord( recordName ) )
				throw new AdvancedScriptException( "'" + recordName + "' is a reserved word " + getLineAndFile() );

			if ( parentScope.findType( recordName ) != null )
				throw new AdvancedScriptException( "'" + recordName + "' is already defined " + getLineAndFile() );

			readToken(); // read name
		}

		if ( currentToken() == null || !currentToken().equals( "{" ) )
			throw new AdvancedScriptException( "'{' Expected " + getLineAndFile() );

		readToken(); // read {

		// Loop collecting fields
		ArrayList fieldTypes = new ArrayList();
		ArrayList fieldNames = new ArrayList();

		while ( true )
		{
			// Get the field type
			ScriptType fieldType = parseType( parentScope, true, true );
			if ( fieldType == null )
				throw new AdvancedScriptException( "Type name Expected " + getLineAndFile() );

			// Get the field name
			String fieldName = currentToken();
			if ( fieldName == null )
				throw new AdvancedScriptException( "Field name Expected " + getLineAndFile() );

			if ( !parseIdentifier( fieldName ) )
				throw new AdvancedScriptException( "Invalid field name: '" + fieldName + "' " + getLineAndFile() );

			if ( isReservedWord( fieldName ) )
				throw new AdvancedScriptException( "'" + fieldName + "' is a reserved word " + getLineAndFile() );

			if ( fieldNames.contains( fieldName ) )
				throw new AdvancedScriptException( "'" + fieldName + "' is already defined " + getLineAndFile() );

			readToken(); // read name

			if ( currentToken() == null || !currentToken().equals( ";" ) )
				throw new AdvancedScriptException( " ';' Expected " + getLineAndFile() );

			readToken(); // read ;

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.toLowerCase() );

			if ( currentToken() == null )
				throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );
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

	private ScriptFunction parseFunction( ScriptType functionType, ScriptScope parentScope ) throws AdvancedScriptException
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		String functionName = currentToken();

		if ( isReservedWord( functionName ) )
			throw new AdvancedScriptException( "'" + functionName + "' is a reserved word " + getLineAndFile() );

		readToken(); //read Function name
		readToken(); //read (

		ScriptVariableList paramList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();

		while ( !currentToken().equals( ")" ) )
		{
			ScriptType paramType = parseType( parentScope, true, false );
			if (paramType == null )
				throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

			ScriptVariable param = parseVariable( paramType, null );
			if ( param == null )
				throw new AdvancedScriptException( " Identifier expected " + getLineAndFile() );

			if ( !paramList.addElement( param ) )
				throw new AdvancedScriptException( "Variable " + param.getName() + " already defined " + getLineAndFile() );

			if ( !currentToken().equals( ")" ) )
			{
				if ( !currentToken().equals( "," ) )
					throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

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
				throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );
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

	private ScriptVariable parseVariable( ScriptType t, ScriptScope scope ) throws AdvancedScriptException
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		String variableName = currentToken();
		if ( isReservedWord( variableName ) )
			throw new AdvancedScriptException( "'" + variableName + "' is a reserved word " + getLineAndFile() );

		ScriptVariable result = new ScriptVariable( variableName, t );
		if ( scope != null && !scope.addVariable( result ) )
			throw new AdvancedScriptException( "Variable " + result.getName() + " already defined " + getLineAndFile() );

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

	private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop ) throws AdvancedScriptException
	{
		ScriptCommand result;

		if ( currentToken() == null )
			return null;

		if ( currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "break outside of loop " + getLineAndFile() );

			result = new ScriptFlowControl( COMMAND_BREAK );
			readToken(); //break
		}

		else if ( currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "continue outside of loop " + getLineAndFile() );

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
		else
			return null;

		if ( currentToken() == null || !currentToken().equals( ";" ) )
			throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );

		readToken(); // ;
		return result;
	}

	private ScriptType parseType( ScriptScope scope, boolean aggregates, boolean records ) throws AdvancedScriptException
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

	private ScriptType parseAggregateType( ScriptType dataType, ScriptScope scope ) throws AdvancedScriptException
	{
		readToken();	// [ or ,
		if ( currentToken() == null )
			throw new AdvancedScriptException( "Missing index token " + getLineAndFile() );

		if ( arrays && integerToken() )
		{
			int size = StaticEntity.parseInt( currentToken() );
			readToken(); // integer
			if ( currentToken() == null )
				throw new AdvancedScriptException( "] expected " + getLineAndFile() );

			if ( currentToken().equals( "]" ) )
			{
				readToken();	// ]
				return new ScriptAggregateType( dataType, size );
			}

			if ( currentToken().equals( "," ) )
				return new ScriptAggregateType( parseAggregateType( dataType, scope ) , size );

			throw new AdvancedScriptException( ", or ] expected " + getLineAndFile() );
		}

		ScriptType indexType = scope.findType( currentToken() );
		if ( indexType == null )
			throw new AdvancedScriptException( "Invalid type name: " + currentToken() + " " + getLineAndFile() );

		if ( !indexType.isPrimitive() )
			throw new AdvancedScriptException( "Index type: " + currentToken() + " is not a primitive type " + getLineAndFile() );

		readToken();	// type name
		if ( currentToken() == null )
			throw new AdvancedScriptException( "] expected " + getLineAndFile() );

		if ( currentToken().equals( "]" ) )
		{
			readToken();	// ]
			return new ScriptAggregateType( dataType, indexType );
		}

		if ( currentToken().equals( "," ) )
			return new ScriptAggregateType( parseAggregateType( dataType, scope ) , indexType );

		throw new AdvancedScriptException( ", or ] expected " + getLineAndFile() );
	}

        private boolean integerToken()
	{
                for ( int i = 0; i < currentToken().length(); ++i )
                {
                        if ( !Character.isDigit( currentToken().charAt( i ) ) )
                                return false;
                }

                return true;
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

	private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope ) throws AdvancedScriptException
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

	private ScriptConditional parseConditional( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop ) throws AdvancedScriptException
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "if" ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

		readToken(); // if
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

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
					throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );

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
						throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

					readToken(); //(
					expression = parseExpression( parentScope );

					if ( currentToken() == null || !currentToken().equals( ")" ) )
						throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

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

	private ScriptWhile parseWhile( ScriptType functionType, ScriptScope parentScope ) throws AdvancedScriptException
	{
		if ( currentToken() == null )
			return null;

		if ( !currentToken().equalsIgnoreCase( "while" ) )
			return null;

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

		readToken(); // while
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

		readToken(); // )

		ScriptScope scope = parseLoopScope( functionType, null, parentScope );

		return new ScriptWhile( scope, expression );
	}

	private ScriptRepeat parseRepeat( ScriptType functionType, ScriptScope parentScope ) throws AdvancedScriptException
	{
		if ( currentToken() == null )
			return null;

		if ( !currentToken().equalsIgnoreCase( "repeat" ) )
			return null;

		readToken(); // repeat

		ScriptScope scope = parseLoopScope( functionType, null, parentScope );
		if ( currentToken() == null || !currentToken().equals( "until" ) )
			throw new AdvancedScriptException( "until Expected " + getLineAndFile() );

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

		readToken(); // until
		readToken(); // (

		ScriptExpression expression = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

		readToken(); // )

		return new ScriptRepeat( scope, expression );
	}

	private ScriptForeach parseForeach( ScriptType functionType, ScriptScope parentScope ) throws AdvancedScriptException
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
				throw new AdvancedScriptException( "key variable name expected " + getLineAndFile() );

			if ( parentScope.findVariable( name ) != null )
				throw new AdvancedScriptException( "key variable " + name + " already defined " + getLineAndFile() );

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

			throw new AdvancedScriptException( "'in' expected " + getLineAndFile() );
		}

		// Get an aggregate reference
		ScriptVariableReference aggregate = parseVariableReference( parentScope );

		if ( aggregate == null || !( aggregate.getType() instanceof ScriptAggregateType ) )
			throw new AdvancedScriptException( "Aggregate reference expected " + getLineAndFile() );

		// Define key variables of appropriate type
		ScriptVariableList varList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();
		ScriptType type = aggregate.getType();

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
		return new ScriptForeach( scope, variableReferences, aggregate );
	}

	private ScriptFor parseFor( ScriptType functionType, ScriptScope parentScope ) throws AdvancedScriptException
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
			throw new AdvancedScriptException( "index variable " + name + " already defined " + getLineAndFile() );

		readToken();	// for
		readToken();	// name

		if ( !(currentToken().equalsIgnoreCase( "from" ) ) )
			throw new AdvancedScriptException( "'from' expected " + getLineAndFile() );
		readToken();	// from

		ScriptExpression initial = parseExpression( parentScope );

		boolean up;
		if ( currentToken().equalsIgnoreCase( "upto" ) )
			up = true;
		else if ( currentToken().equalsIgnoreCase( "downto" ) )
			up = false;
		else
			throw new AdvancedScriptException( "'from' expected " + getLineAndFile() );
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

		return new ScriptFor( scope, new ScriptVariableReference( indexvar ), initial, last, increment, up );
	}

	private ScriptScope parseLoopScope( ScriptType functionType, ScriptVariableList varList, ScriptScope parentScope ) throws AdvancedScriptException
	{
		ScriptScope scope;

		if ( currentToken() != null && currentToken().equals( "{" ) )
		{
			// Scope is a block

			readToken(); // {

			scope = parseScope( null, functionType, varList, parentScope, true );
			if ( currentToken() == null || !currentToken().equals( "}" ) )
				throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );
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

	private ScriptCall parseCall( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();

		readToken(); //name
		readToken(); //(

		ScriptExpressionList params = new ScriptExpressionList();
		while ( currentToken() != null && !currentToken().equals( ")" ) )
		{
			ScriptExpression val = parseExpression( scope );
			if ( val != null )
				params.addElement( val );

			if ( !currentToken().equals( "," ) )
			{
				if ( !currentToken().equals( ")" ) )
					throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );
			}
			else
			{
				readToken();
				if ( currentToken().equals( ")" ) )
					throw new AdvancedScriptException( "Parameter expected " + getLineAndFile() );
			}
		}

		if ( !currentToken().equals( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

		readToken(); // )

		return new ScriptCall( name, scope, params );
	}

	private ScriptAssignment parseAssignment( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( nextToken() == null )
			return null;

		if ( !nextToken().equals( "=" ) && !nextToken().equals( "["  ) && !nextToken().equals( "." ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		ScriptVariableReference lhs = parseVariableReference( scope );

                if ( lhs == null )
			throw new AdvancedScriptException( "Variable reference expected " + getLineAndFile() );

		if ( !currentToken().equals( "=" ) )
			return null;

		readToken(); //=

		ScriptExpression rhs = parseExpression( scope );
		return new ScriptAssignment( lhs, rhs );
	}

	private ScriptExpression parseRemove( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( currentToken() == null || !currentToken().equals( "remove" ) )
			return null;

		ScriptExpression lhs = parseExpression( scope );

		if ( lhs == null )
			throw new AdvancedScriptException( "Bad 'remove' statement " + getLineAndFile() );

		return lhs;
	}

	private ScriptExpression parseExpression( ScriptScope scope ) throws AdvancedScriptException
	{
		return parseExpression( scope, null );
	}

	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper ) throws AdvancedScriptException
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
                        if ( lhs == null || !( lhs instanceof ScriptCompositeReference ) )
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
                                throw new AdvancedScriptException( "Cannot apply " + rhs.getType() + " to " + lhs + " " + getLineAndFile() );
			lhs = new ScriptExpression( lhs, rhs, oper );
		}
		while ( true );
	}

	private ScriptExpression parseValue( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( currentToken() == null )
			return null;

		ScriptExpression result;

                // Parse parenthesized expressions
		if ( currentToken().equals( "(" ) )
		{
			readToken();	// (

			result = parseExpression( scope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
				throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

			readToken();// )
			return result;
		}

                // Parse constant values
		// true and false are reserved words
		if ( currentToken().equalsIgnoreCase( "true" ) )
		{
			readToken();
			return TRUE_VALUE;
		}

		if ( currentToken().equalsIgnoreCase( "false" ) )
		{
			readToken();
			return FALSE_VALUE;
		}

                // numbers
		if ( (result = parseNumber()) != null )
			return result;

                // strings
		if ( currentToken().equals( "\"" ) )
                        return parseString();

                // typed constants
		if ( currentToken().equals( "$" ) )
                        return parseTypedConstant( scope );

                // Function calls
		if ( (result = parseCall( scope )) != null )
			return result;

                // Variable and aggregate references
		if ( (result = parseVariableReference( scope )) != null )
			return result;

		return null;
	}

	private ScriptValue parseNumber() throws AdvancedScriptException
	{
		if ( currentToken() == null )
			return null;

		int sign = 1;

		if ( currentToken().equals( "-" ) )
		{
			String next = nextToken();

			if ( next == null )
				return null;

			if ( !next.equals( ".") && !readIntegerToken( next ) )
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
				throw new AdvancedScriptException( "Bad numeric value " + getLineAndFile() );
			readToken();	// integer
			return new ScriptValue( sign * StaticEntity.parseDouble( "0." + fraction ) );
		}

		String integer = currentToken();
		if ( !readIntegerToken( integer ) )
			return null;
		readToken();	// integer

		if ( currentToken().equals( "." ) )
		{
			readToken();	// .
			String fraction = currentToken();
			if ( !readIntegerToken( fraction ) )
				return new ScriptValue( sign * StaticEntity.parseDouble( integer ) );
			readToken();	// fraction
			return new ScriptValue( sign * StaticEntity.parseDouble( integer + "." + fraction ) );
		}

		return new ScriptValue( sign * StaticEntity.parseInt( integer ) );
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

	private ScriptValue parseString() throws AdvancedScriptException
	{
		// Directly work with line - ignore any "tokens" you meet until
		// the string is closed

		StringBuffer resultString = new StringBuffer();

		for ( int i = 1; ; ++i )
		{
			if ( i == line.length() )
				throw new AdvancedScriptException( "No closing '\"' found " + getLineAndFile() );

			if ( line.charAt( i ) == '\\' )
			{
				char ch = line.charAt( ++i );

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
					BigInteger hex08 = new BigInteger( line.substring( i + 1, i + 3 ), 16 );
					resultString.append( (char) hex08.intValue() );
					i += 2;
					break;

				case 'u':
					BigInteger hex16 = new BigInteger( line.substring( i + 1, i + 5 ), 16 );
					resultString.append( (char) hex16.intValue() );
					i += 4;
					break;

				default:
					if ( Character.isDigit( ch ) )
					{
						BigInteger octal = new BigInteger( line.substring( i, i + 3 ), 8 );
						resultString.append( (char) octal.intValue() );
						i += 2;
					}
				}
			}
			else if ( line.charAt( i ) == '"' )
			{
				line = line.substring( i + 1 ); //+ 1 to get rid of '"' token
				return new ScriptValue( resultString.toString() );
			}
			else
			{
				resultString.append( line.charAt( i ) );
			}
		}
	}

	private ScriptValue parseTypedConstant( ScriptScope scope ) throws AdvancedScriptException
	{
		readToken();    // read $

		String name = currentToken();
		ScriptType type = parseType( scope, false, false );
		if ( type == null || !type.isPrimitive() )
			throw new AdvancedScriptException( "Unknown type " + name + " " + getLineAndFile() );

		if ( !currentToken().equals( "[" ) )
			throw new AdvancedScriptException( "'[' Expected " + getLineAndFile() );

		StringBuffer resultString = new StringBuffer();

		for ( int i = 1; ; ++i )
		{
			if ( i == line.length() )
			{
				throw new AdvancedScriptException( "No closing ']' found " + getLineAndFile() );
			}
			else if ( line.charAt( i ) == '\\' )
			{
				resultString.append( line.charAt( ++i ) );
			}
			else if ( line.charAt( i ) == ']' )
			{
				line = line.substring( i + 1 ); //+1 to get rid of ']' token
				return parseValue( type, resultString.toString().trim());
			}
			else
			{
				resultString.append( line.charAt( i ) );
			}
		}
	}

	private ScriptOperator parseOperator( String oper )
	{
		if ( oper == null )
			return null;
		if
		(
			oper.equals( "!" ) ||
			oper.equals( "*" ) || oper.equals( "^" ) || oper.equals( "/" ) || oper.equals( "%" ) ||
			oper.equals( "+" ) || oper.equals( "-" ) ||
			oper.equals( "<" ) || oper.equals( ">" ) || oper.equals( "<=" ) || oper.equals( ">=" ) ||
			oper.equals( "==" ) || oper.equals( "!=" ) ||
			oper.equals( "||" ) || oper.equals( "&&" ) ||
			oper.equals( "contains" ) || oper.equals( "remove" )
		 )
		{
			return new ScriptOperator( oper );
		}
		return null;
	}

	private ScriptVariableReference parseVariableReference( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( currentToken() == null || !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();
		ScriptVariable var = scope.findVariable( name, true );

		if ( var == null )
			throw new AdvancedScriptException( "Unknown variable " + name + " " + getLineAndFile() );

		readToken(); // read name

		if ( currentToken() == null || (!currentToken().equals( "[" ) && !currentToken().equals( "." ) ) )
			return new ScriptVariableReference( var );

		ScriptType type = var.getType();
		ScriptExpressionList indices = new ScriptExpressionList();
		boolean aggregate = currentToken().equals( "[" );

		while ( true )
		{
			readToken(); // read [ or . or ,

			ScriptExpression index;

			if ( aggregate )
			{
				if ( !( type instanceof ScriptAggregateType ) )
				{
					if ( indices.isEmpty() )
						throw new AdvancedScriptException( name + " is not an aggregate " + getLineAndFile() );
					else
						throw new AdvancedScriptException( "Too many keys " + getLineAndFile() );
				}

				ScriptAggregateType atype = (ScriptAggregateType)type;
				index = parseExpression( scope );
				if ( index == null )
					throw new AdvancedScriptException( "Index expression expected " + getLineAndFile() );

				if ( !index.getType().equals( atype.getIndexType() ) )
					throw new AdvancedScriptException( "Index has wrong data type " + getLineAndFile() );
				type = atype.getDataType();
			}
			else
			{
				if ( !( type instanceof ScriptRecordType ) )
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

			if ( aggregate )
			{
				if ( currentToken() == null )
					throw new AdvancedScriptException( "] expected " + getLineAndFile() );
				if ( currentToken().equals( "," ) )
					continue;

				if ( !currentToken().equals( "]" ) )
					throw new AdvancedScriptException( "] expected " + getLineAndFile() );

				readToken(); // read ]
				aggregate = false;
			}

			if ( currentToken() == null || ( !currentToken().equals( "[" ) && !currentToken().equals( "." ) ) )
				break;

			aggregate = currentToken().equals( "[" );
		}

		return new ScriptCompositeReference( var, indices );
	}

	private String parseImport() throws AdvancedScriptException
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "import" ) )
			return null;
		readToken(); //import

		if ( currentToken() == null || !currentToken().equals( "<" ) )
			throw new AdvancedScriptException( "'<' Expected " + getLineAndFile() );

		int index = line.indexOf( ">" );
		if ( index == -1 )
			throw new AdvancedScriptException( "No closing '>' found " + getLineAndFile() );

		String resultString = line.substring( 1, index );
		line = line.substring( index + 1 ); //+1 to get rid of '>' token

		if ( !currentToken().equals( ";" ) )
			throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );
		readToken(); //read ;

		return resultString;
	}

	private static boolean validCoercion( ScriptType lhs, ScriptType rhs, String oper )
	{
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
		if ( line == null )
			return null;
		return line.substring( 0, tokenLength( line ) );
	}

	private String nextToken()
	{
		fixLines();

		if ( line == null )
			return null;

		if ( tokenLength( line ) >= line.length() )
		{
			if ( nextLine == null )
				return null;

			return nextLine.substring( 0, tokenLength( nextLine ) ).trim();
		}

		String result = line.substring( tokenLength( line ) ).trim();

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
		if ( line == null )
			return;

		fixLines();
		line = line.substring( tokenLength( line ) );
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
		if ( line == null )
			return;

		while ( line.equals( "" ) )
		{
			line = nextLine;
			lineNumber = commandStream.getLineNumber();
			nextLine = getNextLine();

			if ( line == null )
				return;
		}

		line = line.trim();

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
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<SCOPE>" );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<VARIABLES>" );
		for ( ScriptVariable currentVar = scope.getFirstVariable(); currentVar != null; currentVar = scope.getNextVariable() )
			printVariable( currentVar, indent + 2 );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<FUNCTIONS>" );
		for ( ScriptFunction currentFunc = scope.getFirstFunction(); currentFunc != null; currentFunc = scope.getNextFunction() )
			printFunction( currentFunc, indent + 2 );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<COMMANDS>" );
		for ( ScriptCommand currentCommand = scope.getFirstCommand(); currentCommand != null; currentCommand = scope.getNextCommand() )
			printCommand( currentCommand, indent + 2 );
	}

	private void printVariable( ScriptVariable var, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<VAR " + var.getType() + " " + var.getName() + ">" );
	}

	private void printFunction( ScriptFunction func, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<FUNC " + func.getType() + " " + func.getName() + ">" );
		for ( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam() )
			printVariableReference( current, indent + 1 );
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
			KoLmafia.getDebugStream().println( "<COMMAND " + command + ">" );
		}
	}

	private void printReturn( ScriptReturn ret, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<RETURN " + ret.getType() + ">" );
		if ( !ret.getType().equals( TYPE_VOID ) )
			printExpression( ret.getExpression(), indent + 1 );
	}

	private void printConditional( ScriptConditional command, int indent )
	{
		indentLine( indent );
		if ( command instanceof ScriptIf )
		{
			ScriptIf loop = (ScriptIf)command;
			KoLmafia.getDebugStream().println( "<IF>" );
			printExpression( loop.getCondition(), indent + 1 );
			printScope( loop.getScope(), indent + 1 );
			for ( ScriptConditional currentElse = loop.getFirstElseLoop(); currentElse != null; currentElse = loop.getNextElseLoop() )
				printConditional( currentElse, indent );
		}
		else if ( command instanceof ScriptElseIf )
		{
			ScriptElseIf loop = (ScriptElseIf)command;
			KoLmafia.getDebugStream().println( "<ELSE IF>" );
			printExpression( loop.getCondition(), indent + 1 );
			printScope( loop.getScope(), indent + 1 );
		}
		else if (command instanceof ScriptElse )
		{
			ScriptElse loop = (ScriptElse)command;
			KoLmafia.getDebugStream().println( "<ELSE>" );
			printScope( loop.getScope(), indent + 1 );
		}
	}

	private void printWhile( ScriptWhile loop, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<WHILE>" );
		printExpression( loop.getCondition(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printRepeat( ScriptRepeat loop, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<REPEAT>" );
		printScope( loop.getScope(), indent + 1 );
		printExpression( loop.getCondition(), indent + 1 );
	}

	private void printForeach( ScriptForeach loop, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<FOREACH>" );
		for ( ScriptVariableReference current = loop.getFirstVariableReference(); current != null; current = loop.getNextVariableReference() )
			printVariableReference( current, indent + 1 );
		printVariableReference( loop.getAggregate(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printFor( ScriptFor loop, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<FOR " + ( loop.getUp() ? "upto" : "downto" ) + " >" );
		printVariableReference( loop.getVariable(), indent + 1 );
		printExpression( loop.getInitial(), indent + 1 );
		printExpression( loop.getLast(), indent + 1 );
		printExpression( loop.getIncrement(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
	}

	private void printCall( ScriptCall call, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<CALL " + call.getTarget().getName() + ">" );
		for ( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam() )
			printExpression( current, indent + 1 );
	}

	private void printAssignment( ScriptAssignment assignment, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName() + ">" );
		printExpression( assignment.getRightHandSide(), indent + 1 );

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
			KoLmafia.getDebugStream().println( "<VALUE " + value.getType() + " [" + value + "]>" );
		}
	}

	public void printOperator( ScriptOperator oper, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<OPER " + oper + ">" );
	}

	public void printCompositeReference( ScriptCompositeReference varRef, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<AGGREF> " + varRef.getName() );
	}

	public void printVariableReference( ScriptVariableReference varRef, int indent )
	{
		if ( varRef instanceof ScriptCompositeReference )
		{
			printCompositeReference( (ScriptCompositeReference)varRef, indent );
			return;
		}
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<VARREF> " + varRef.getName() );
	}

	private static void indentLine( int indent )
	{
		for ( int i = 0; i < indent; ++i )
			KoLmafia.getDebugStream().print( "   " );
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

		if ( !KoLmafia.refusesContinue() )
		{
			currentState = STATE_NORMAL;
			KoLmafia.forceContinue();
		}
	}

	private ScriptValue executeGlobalScope( ScriptScope globalScope ) throws AdvancedScriptException
	{
		ScriptFunction main;
		ScriptValue result = null;
		String resultString;

		currentState = STATE_NORMAL;
		resetTracing();

		main = globalScope.findFunction( "main", null );

		if ( main == null && globalScope.getFirstCommand() == null )
			throw new AdvancedScriptException( "No commands or main function found." );

		// First execute top-level commands;
		trace( "Executing top-level commands" );

		result = globalScope.execute();
		if ( currentState == STATE_EXIT )
			return result;

		// Now execute main function, if any
		if ( main != null )
		{
			trace( "Executing main function" );
			requestUserParams( main );
			result = main.execute();
		}

		return result;
	}

	private void requestUserParams( ScriptFunction targetFunction ) throws AdvancedScriptException
	{
		ScriptVariableReference	param;
		String resultString;

		for ( param = targetFunction.getFirstParam(); param != null; param = targetFunction.getNextParam() )
		{
			if ( param.getType().equals( TYPE_ZODIAC ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					ZODIACS,
					ZODIACS[0]
				 );
				param.setValue( parseZodiacValue( resultString ) );
			}
			else if ( param.getType().equals( TYPE_CLASS ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					CLASSES,
					CLASSES[0]
				 );
				param.setValue( parseClassValue( resultString ) );
			}
			else if ( param.getType().equals( TYPE_STAT ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					STATS,
					STATS[0]
				 );
				param.setValue( parseStatValue( resultString ) );
			}
			else if
			(
				param.getType().equals( TYPE_ITEM ) ||
				param.getType().equals( TYPE_LOCATION ) ||
				param.getType().equals( TYPE_STRING ) ||
				param.getType().equals( TYPE_SKILL ) ||
				param.getType().equals( TYPE_EFFECT ) ||
				param.getType().equals( TYPE_FAMILIAR ) ||
				param.getType().equals( TYPE_SLOT ) ||
				param.getType().equals( TYPE_MONSTER ) ||
				param.getType().equals( TYPE_ELEMENT )
			)
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				param.setValue( parseValue( param.getType(), resultString ) );
			}
			else if ( param.getType().equals( TYPE_INT ) )
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				try
				{
					param.setValue( new ScriptValue( StaticEntity.parseInt( resultString ) ) );
				}
				catch( NumberFormatException e )
				{
					throw new AdvancedScriptException( "Incorrect value for integer." );
				}
			}
			else if ( param.getType().equals( TYPE_FLOAT ) )
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				try
				{
					param.setValue( new ScriptValue( StaticEntity.parseDouble( resultString ) ) );
				}
				catch( NumberFormatException e )
				{
					throw new AdvancedScriptException( "Incorrect value for float." );
				}
			}
			else if ( param.getType().equals( TYPE_BOOLEAN ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					BOOLEANS,
					BOOLEANS[0]
				 );
				if ( resultString.equalsIgnoreCase( "true" ) )
					param.setValue( TRUE_VALUE );
				else if ( resultString.equalsIgnoreCase( "false" ) )
					param.setValue( FALSE_VALUE );
				else
					throw new RuntimeException( "Internal error: Illegal value for boolean" );
			}
			else if ( param.getType().equals( TYPE_VOID ) )
			{
				param.setValue( VOID_VALUE );
			}
			else
				throw new RuntimeException( "Internal error: Illegal type for main() parameter" );
		}
	}

	public String getLineAndFile()
	{
		return "at line " + lineNumber + " in file " + fileName;
	}

	private static void readMap( ScriptCompositeValue result, String [] data ) throws AdvancedScriptException
	{
		ScriptCompositeValue slice = result;
		ScriptCompositeType dataType = slice.getCompositeType();
		ScriptValue index = null;

		for ( int i = 0; i < data.length - 2; ++i )
		{
			// Create missing intermediate slices while storing
			// the slice where the value is ultimately stored.

			index = parseValue( dataType.getIndexType(), data[i] );
			result = (ScriptCompositeValue) slice.aref( index );

			if ( result == null )
			{
				result = (ScriptCompositeValue) slice.initialValue( index );
				slice.aset( index, result );
			}

			slice = result;
			dataType = slice.getCompositeType();
		}

		if ( data.length > 2 )
		{
			index = parseValue( dataType.getIndexType(), data[ data.length - 2 ] );
			slice.aset( index, parseValue( dataType.getDataType( index ), data[ data.length - 1 ] ) );
		}
	}

	private static void printMap( PrintStream writer, String prefix, ScriptCompositeValue map_value )
	{
		ScriptValue [] keys = map_value.keys();
		if ( keys.length == 0 )
			return;

		for ( int i = 0; i < keys.length; ++i )
		{
			ScriptValue key = keys[i];
			ScriptValue value = map_value.aref( key );
			String first = prefix + key + "\t";
			if ( map_value.getCompositeType().getDataType( key ) instanceof ScriptCompositeType )
				printMap( writer, first, (ScriptCompositeValue)value );
			else
				writer.println( first + value.toStringValue().toString() );
		}
	}

	public ScriptScope getExistingFunctionScope()
	{	return new ScriptScope( existingFunctions, null, simpleTypes );
	}

	public static ScriptFunctionList getExistingFunctions()
	{
		ScriptFunctionList result = new ScriptFunctionList();
		ScriptType [] params;

		// All datatypes must supply xxx_to_string and string_to_xxx
		// methods.
		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_status", VOID_TYPE, params ) );

		params = new ScriptType[] { BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "boolean_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_boolean", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "float_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_float", FLOAT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_item", ITEM_TYPE, params ) );

		params = new ScriptType[] { ZODIAC_TYPE };
		result.addElement( new ScriptExistingFunction( "zodiac_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_zodiac", ZODIAC_TYPE, params ) );

		params = new ScriptType[] { LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "location_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "location_to_url", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_location", LOCATION_TYPE, params ) );

		params = new ScriptType[] { CLASS_TYPE };
		result.addElement( new ScriptExistingFunction( "class_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_class", CLASS_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "stat_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_stat", STAT_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "skill_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_skill", SKILL_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "effect_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_effect", EFFECT_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "slot_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_slot", SLOT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_monster", MONSTER_TYPE, params ) );

		params = new ScriptType[] { ELEMENT_TYPE };
		result.addElement( new ScriptExistingFunction( "element_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "string_to_element", ELEMENT_TYPE, params ) );

		// Now include xxx_to_int and int_xxx methods for datatypes
		// for which that makes sense

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_item", ITEM_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_skill", SKILL_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "skill_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_effect", EFFECT_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "effect_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_slot", SLOT_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "slot_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "int_to_element", ELEMENT_TYPE, params ) );

		params = new ScriptType[] { ELEMENT_TYPE };
		result.addElement( new ScriptExistingFunction( "element_to_int", INT_TYPE, params ) );

		// Begin the functions which are documented in the KoLmafia
		// Advanced Script Handling manual.

		params = new ScriptType[] { INT_TYPE, LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "adventure", BOOLEAN_TYPE, params ) );

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

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addElement( new ScriptExistingFunction( "closet_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addElement( new ScriptExistingFunction( "museum_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addElement( new ScriptExistingFunction( "shop_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "storage_amount", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "stash_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "creatable_amount", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "put_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_shop", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_display", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "take_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "pulls_remaining", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_storage", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_display", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "sell_item", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_name", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_zodiac", ZODIAC_TYPE, params ) );

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
		result.addElement( new ScriptExistingFunction( "my_closetmeat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stills_available", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_adventures", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_turncount", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_inebriety", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "inebriety_limit", INT_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "have_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "have_effect", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "add_item_condition", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_eat", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_drink", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_interact", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_hardcore", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "trade_hermit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "trade_bounty_hunter", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "trade_trapper", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "trade_trapper", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip_slot", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "unequip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "unequip_slot", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "current_equipment", ITEM_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_to_slot", SLOT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "outfit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "have_outfit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "have_equipped", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "equip_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "have_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_weight", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_base_attack", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_base_defense", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_base_hp", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_hands", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "ranged_weapon", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "council", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_mind_control_level", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "mind_control", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_chef", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_bartender", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "cli_execute", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "visit_url", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "contains_text", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "bounty_hunter_wants", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "wait", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "entryway", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "hedgemaze", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "guardians", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "chamber", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "nemesis", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "guild", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "gourd", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "tavern", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "train_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "retrieve_item", BOOLEAN_TYPE, params ) );

		// Arithmetic utility functions
		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "random", INT_TYPE, params ) );

		// Float-to-int conversion functions
		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "round", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "truncate", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "floor", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "ceil", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_encode", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_decode", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "get_property", STRING_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "set_property", VOID_TYPE, params ) );

		params = new ScriptType[] { AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "count", INT_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE, AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { AGGREGATE_TYPE, STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_location", LOCATION_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_mushroom_plot", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_hp", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_mp", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "skill_to_effect", EFFECT_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "effect_to_skill", SKILL_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "mp_cost", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "can_equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_equipment", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_today", STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_level_adjustment", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "familiar_weight_adjustment", INT_TYPE, params ) );

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
		result.addElement( new ScriptExistingFunction( "cold_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "hot_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "sleaze_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "spooky_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stench_resistance", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "combat_percent_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "initiative_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "fixed_experience_bonus", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "meat_drop_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drop_modifier", FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "buffed_hit_stat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_hit_stat", STAT_TYPE, params ) );

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
		result.addElement( ZODIAC_TYPE );
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
		result.addElement( new ScriptSymbol( "zodiac" ) );
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

		public ScriptFunction getFirstFunction()
		{	return (ScriptFunction)functions.getFirstElement();
		}

		public ScriptFunction getNextFunction()
		{	return (ScriptFunction)functions.getNextElement();
		}

		public boolean addVariable( ScriptVariable v )
		{	return variables.addElement( v );
		}

		public ScriptVariable getFirstVariable()
		{	return (ScriptVariable)variables.getFirstElement();
		}

		public ScriptVariable getNextVariable()
		{	return (ScriptVariable)variables.getNextElement();
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

		public ScriptType getFirstType()
		{	return (ScriptType)types.getFirstElement();
		}

		public ScriptType getNextType()
		{	return (ScriptType)types.getNextElement();
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

		public ScriptCommand getFirstCommand()
		{	return (ScriptCommand)commands.getFirstElement();
		}

		public ScriptCommand getNextCommand()
		{	return (ScriptCommand)commands.getNextElement();
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

		public ScriptUserDefinedFunction replaceFunction( ScriptUserDefinedFunction f ) throws AdvancedScriptException
		{
			String functionName = f.getName();
			ScriptUserDefinedFunction current = (ScriptUserDefinedFunction) functions.findFunction( functionName );
			if ( current != null )
			{
				// The existing function must be a forward
				// reference.

				if ( current.getScope() != null )
					throw new AdvancedScriptException( "Function " + functionName + " already defined " + getLineAndFile() );

				// The types of the new function's parameters
				// must exactly match the types of the existing
				// function's parameters

				ScriptVariableReference p1 = current.getFirstParam();
				ScriptVariableReference p2 = f.getFirstParam();
				int paramCount = 1;
				while ( p1 != null && p2 != null )
				{
					if ( !p1.getType().equals( p2.getType() ) )
						throw new AdvancedScriptException( "Function " + functionName + " parameter #" + paramCount + " previously declared to have type " + p1.getType().toString() + " " + getLineAndFile() );
					p1 = current.getNextParam();
					p2 = f.getNextParam();
					++paramCount;
				}

				// There must be the same number of parameters

				if ( p1 != null )
					throw new AdvancedScriptException( "Function " + functionName + " previously declared to have more parameters " + getLineAndFile() );

				if ( p2 != null )
					throw new AdvancedScriptException( "Function " + functionName + " previously declared to have fewer parameters " + getLineAndFile() );

				current.setVariableReferences( f.getVariableReferences() );
				return current;
			}
			addFunction( f );
			return f;
		}

		public ScriptFunction findFunction( String name, ScriptExpressionList params ) throws AdvancedScriptException
		{
			String errorMessage = null;
			int currentIndex = functions.indexOf( name, 0 );
			while ( currentIndex != -1 )
			{
				errorMessage = null;
				ScriptFunction current = functions.findFunction( name, currentIndex );
				currentIndex = functions.indexOf( name, currentIndex + 1 );

				if ( params == null )
					return current;

				ScriptVariableReference currentParam = current.getFirstParam();
				ScriptExpression currentValue = params.getFirstExpression();
				int paramIndex = 1;

				while ( errorMessage == null && currentParam != null && currentValue != null )
				{
					if ( !validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
						errorMessage = "Illegal parameter #" + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " " + getLineAndFile();

					++paramIndex;
					currentParam = current.getNextParam();
					currentValue = params.getNextExpression();
				}

				if ( errorMessage == null && (currentParam != null || currentValue != null) )
					errorMessage = "Illegal amount of parameters for function " + name + " " + getLineAndFile();

				if ( errorMessage == null )
					return current;
			}

			if ( errorMessage != null )
				throw new AdvancedScriptException( errorMessage );

			if ( parentScope != null )
				return parentScope.findFunction( name, params );

			return null;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptCommand current;
			ScriptValue result = null;

			traceIndent();
			for ( current = getFirstCommand(); current != null; current = getNextCommand() )
			{
				trace( "Command: " + current );

				result = current.execute();

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
					currentState = STATE_EXIT;

				trace( "[" + executionStateString( currentState ) + "] <- " + result );

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
		protected String name;

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

		public ScriptSymbol getFirstElement()
		{
			searchIndex = -1;
			return getNextElement();
		}

		public ScriptSymbol getNextElement()
		{
			if ( ++searchIndex >= size() )
				return null;
			return (ScriptSymbol)get( searchIndex );
		}

		public ScriptSymbol getNextElement( ScriptSymbol n )
		{
			searchIndex = indexOf( n );
			if ( searchIndex == -1 )
				return null;
			return getNextElement();
		}
	}

	private static class ScriptFunction extends ScriptSymbol
	{
		protected ScriptType type;
		protected ScriptVariableReferenceList variableReferences;
		protected ScriptValue [] values;

		public ScriptFunction( String name, ScriptType type, ScriptVariableReferenceList variableReferences )
		{
			super( name );
			this.type = type;
			this.variableReferences = variableReferences;
			this.values = new ScriptValue[ variableReferences.size() ];
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

		public ScriptVariableReference getFirstParam()
		{	return (ScriptVariableReference)variableReferences.getFirstElement();
		}

		public ScriptVariableReference getNextParam()
		{	return (ScriptVariableReference)variableReferences.getNextElement();
		}

		public void saveBindings() throws AdvancedScriptException
		{
			// Save current parameter value bindings
			for ( int i = 0; i < values.length; ++i )
				values[i] = ((ScriptVariableReference)variableReferences.get(i)).getValue();
		}

		public void restoreBindings()
		{
			// Restore  parameter value bindings
			for ( int i = 0; i < values.length; ++i )
				((ScriptVariableReference)variableReferences.get(i)).forceValue( values[i] );
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			return null;
		}
        }

	private class ScriptUserDefinedFunction extends ScriptFunction
	{
		ScriptScope scope;

		public ScriptUserDefinedFunction( String name, ScriptType type, ScriptVariableReferenceList variableReferences )
		{
			super( name, type, variableReferences );
			this.scope = null;
		}

		public void setScope( ScriptScope s )
		{	scope = s;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( scope == null )
				throw new RuntimeException( "Calling undefined user function: " + getName() );

			ScriptValue result = scope.execute();

			if ( currentState != STATE_EXIT )
				currentState = STATE_NORMAL;

			return result;
		}

		public boolean assertReturn()
		{	return scope.assertReturn();
		}
	}

	private static class ScriptExistingFunction extends ScriptFunction
	{
		private Method method;
		private ScriptVariable [] variables;

		public ScriptExistingFunction( String name, ScriptType type, ScriptType [] params )
		{
			super( name.toLowerCase(), type );

			variables = new ScriptVariable[ params.length ];
			values = new ScriptValue[ params.length ];
			Class [] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				variables[i] = new ScriptVariable( params[i] );
				variableReferences.addElement( new ScriptVariableReference( variables[i] ) );
				args[i] = ScriptVariable.class;
			}

			try
			{
				this.method = getClass().getMethod( name, args );
			}
			catch ( Exception e )
			{
				// This should not happen; it denotes a coding
				// error that must be fixed before release. So,
				// simply print the bogus function to stdout

				System.out.println( "No method found for built-in function: " + name );
			}
		}

		public ScriptValue execute()
		{
			if ( method == null )
				throw new RuntimeException( "Internal error: no method for " + getName() );

			try
			{
				// Invoke the method
				return (ScriptValue)method.invoke(this, variables);
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				printStackTrace( e, "Exception during call to " + getName() );
				return null;
			}
		}

		private ScriptValue continueValue()
		{	return KoLmafia.permitsContinue() ? TRUE_VALUE : FALSE_VALUE;
		}

		// Here are all the methods for built-in ASH functions

		public ScriptValue refresh_status()
		{
			DEFAULT_SHELL.executeLine( "refresh status" );
			return VOID_VALUE;
		}

		public ScriptValue boolean_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_boolean( ScriptVariable val ) throws IllegalArgumentException
		{	return parseBooleanValue( val.toStringValue().toString() );
		}

		public ScriptValue int_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_int( ScriptVariable val ) throws IllegalArgumentException
		{	return parseIntValue( val.toStringValue().toString() );
		}

		public ScriptValue float_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_float( ScriptVariable val ) throws IllegalArgumentException
		{	return parseFloatValue( val.toStringValue().toString() );
		}

		public ScriptValue item_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_item( ScriptVariable val ) throws IllegalArgumentException
		{	return parseItemValue( val.toStringValue().toString() );
		}

		public ScriptValue zodiac_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_zodiac( ScriptVariable val ) throws IllegalArgumentException
		{	return parseZodiacValue( val.toStringValue().toString() );
		}

		public ScriptValue location_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue location_to_url( ScriptVariable val )
		{
			KoLAdventure adventure = (KoLAdventure) val.rawValue();
			return new ScriptValue( adventure.getRequest().getURLString() );
		}

		public ScriptValue string_to_location( ScriptVariable val ) throws IllegalArgumentException
		{	return parseLocationValue( val.toStringValue().toString() );
		}

		public ScriptValue class_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_class( ScriptVariable val ) throws IllegalArgumentException
		{	return parseClassValue( val.toStringValue().toString() );
		}

		public ScriptValue stat_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_stat( ScriptVariable val ) throws IllegalArgumentException
		{	return parseStatValue( val.toStringValue().toString() );
		}

		public ScriptValue skill_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_skill( ScriptVariable val ) throws IllegalArgumentException
		{	return parseSkillValue( val.toStringValue().toString() );
		}

		public ScriptValue effect_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_effect( ScriptVariable val ) throws IllegalArgumentException
		{	return parseEffectValue( val.toStringValue().toString() );
		}

		public ScriptValue familiar_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_familiar( ScriptVariable val ) throws IllegalArgumentException
		{	return parseFamiliarValue( val.toStringValue().toString() );
		}

		public ScriptValue slot_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_slot( ScriptVariable val ) throws IllegalArgumentException
		{	return parseSlotValue( val.toStringValue().toString() );
		}

		public ScriptValue monster_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_monster( ScriptVariable val ) throws IllegalArgumentException
		{	return parseMonsterValue( val.toStringValue().toString() );
		}

		public ScriptValue element_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue string_to_element( ScriptVariable val ) throws IllegalArgumentException
		{	return parseElementValue( val.toStringValue().toString() );
		}

		public ScriptValue item_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_item( ScriptVariable val )
		{	return makeItemValue( val.intValue() );
		}

		public ScriptValue skill_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_skill( ScriptVariable val )
		{	return makeSkillValue( val.intValue() );
		}

		public ScriptValue effect_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_effect( ScriptVariable val )
		{	return makeEffectValue( val.intValue() );
		}

		public ScriptValue familiar_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_familiar( ScriptVariable val )
		{	return makeFamiliarValue( val.intValue() );
		}

		public ScriptValue slot_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_slot( ScriptVariable val )
		{	return makeSlotValue( val.intValue() );
		}

		public ScriptValue element_to_int( ScriptVariable val )
		{	return new ScriptValue( val.intValue() );
		}

		public ScriptValue int_to_element( ScriptVariable val )
		{	return makeElementValue( val.intValue() );
		}

		// Begin the functions which are documented in the KoLmafia
		// Advanced Script Handling manual.

		public ScriptValue adventure( ScriptVariable count, ScriptVariable loc )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
			return continueValue();
		}

		public ScriptValue buy( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
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

		public ScriptValue item_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLCharacter.getInventory() ) );
		}

		public ScriptValue closet_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLCharacter.getCloset() ) );
		}

		public ScriptValue museum_amount( ScriptVariable arg )
		{
			if ( KoLCharacter.getCollection().isEmpty() )
				(new MuseumRequest( client )).run();
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLCharacter.getCollection() ) );
		}

		public ScriptValue shop_amount( ScriptVariable arg )
		{
			(new StoreManageRequest( client )).run();

			LockableListModel list = StoreManager.getSoldItemList();
			StoreManager.SoldItem item = new StoreManager.SoldItem( arg.intValue(), 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
				return new ScriptValue( 0 );

			item = (StoreManager.SoldItem) list.get( index );
			return new ScriptValue( item.getQuantity() );
		}

		public ScriptValue storage_amount( ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLCharacter.getStorage() ) );
		}

		public ScriptValue refresh_stash()
		{
			(new ClanStashRequest( client )).run();
			return continueValue();
		}

		public ScriptValue stash_amount( ScriptVariable arg )
		{
			List stash = ClanManager.getStash();
			if ( stash.size() == 0 )
				(new ClanStashRequest( client )).run();
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( stash ) );
		}

		public ScriptValue creatable_amount( ScriptVariable arg )
		{
			ConcoctionsDatabase.refreshConcoctions();
			ItemCreationRequest item = ItemCreationRequest.getInstance( client, arg.intValue(), 0 );
			return new ScriptValue( item == null ? 0 : item.getCount( ConcoctionsDatabase.getConcoctions() ) );
		}

		public ScriptValue put_closet( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue put_closet( ScriptVariable meat )
		{
			if ( meat.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "closet put " + meat.intValue() + " meat" );
			return continueValue();
		}

		public ScriptValue put_shop( ScriptVariable price, ScriptVariable limit, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " " + price.intValue() + " " + limit.intValue() );
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

		public ScriptValue take_closet( ScriptVariable meat )
		{
			if ( meat.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "closet take " + meat.intValue() + " meat" );
			return continueValue();
		}

		public ScriptValue pulls_remaining()
		{	return new ScriptValue( HagnkStorageFrame.getPullsRemaining() );
		}

		public ScriptValue take_storage( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_stash( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "stash take " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue take_display( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue sell_item( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue print( ScriptVariable string )
		{
			KoLmafia.updateDisplay( string.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue my_name()
		{	return new ScriptValue( KoLCharacter.getUsername() );
		}

		public ScriptValue my_zodiac()
		{	return makeZodiacValue( KoLCharacter.getSign() );
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

		public ScriptValue my_closetmeat()
		{	return new ScriptValue( KoLCharacter.getClosetMeat() );
		}

		public ScriptValue stills_available()
		{	return new ScriptValue( KoLCharacter.getStillsAvailable() );
		}

		public ScriptValue my_adventures()
		{	return new ScriptValue( KoLCharacter.getAdventuresLeft() );
		}

		public ScriptValue my_turncount()
		{	return new ScriptValue( KoLCharacter.getTotalTurnsUsed() );
		}

		public ScriptValue my_inebriety()
		{	return new ScriptValue( KoLCharacter.getInebriety() );
		}

		public ScriptValue inebriety_limit()
		{	return new ScriptValue( !KoLCharacter.canDrink() ? 0 : KoLCharacter.hasSkill( "Liver of Steel" ) ? 20 : 15 );
		}

		public ScriptValue my_familiar()
		{	return makeFamiliarValue( KoLCharacter.getFamiliar().getID() );
		}

		public ScriptValue have_familiar( ScriptVariable familiar )
		{	return new ScriptValue( KoLCharacter.findFamiliar( familiar.toStringValue().toString() ) != null );
		}

		public ScriptValue familiar_weight( ScriptVariable familiar )
		{
			FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue().toString() );
			return new ScriptValue( (fam == null ) ? 0 : fam.getWeight() );
		}

		public ScriptValue have_effect( ScriptVariable arg )
		{
			List potentialEffects = StatusEffectDatabase.getMatchingNames( arg.toStringValue().toString() );
			AdventureResult effect = potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get(0), 0, true );
			return new ScriptValue( effect == null ? 0 : effect.getCount( KoLCharacter.getEffects() ) );
		}

		public ScriptValue have_skill( ScriptVariable arg )
		{	return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
		}

		public ScriptValue use_skill( ScriptVariable count, ScriptVariable skill )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue add_item_condition( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return VOID_VALUE;

			DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
			return VOID_VALUE;
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

		public ScriptValue trade_hermit( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue bounty_hunter_wants( ScriptVariable item )
		{
			String itemName = item.toStringValue().toString();

			if ( client.hunterItems.isEmpty() )
				(new BountyHunterRequest( client )).run();

			for ( int i = 0; i < client.hunterItems.size(); ++i )
				if ( ((String)client.hunterItems.get(i)).equalsIgnoreCase( itemName ) )
					return TRUE_VALUE;

			return FALSE_VALUE;
		}

		public ScriptValue trade_bounty_hunter( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "hunter " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue trade_trapper( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "trapper " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue trade_trapper( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			DEFAULT_SHELL.executeLine( "trapper " + count.intValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue equip( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue equip_slot( ScriptVariable slot, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue unequip( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "unequip " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue unequip_slot( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "unequip " + item.toStringValue() );
			return continueValue();
		}

		public ScriptValue current_equipment( ScriptVariable slot )
		{	return makeItemValue( KoLCharacter.getCurrentEquipmentName( slot.intValue() ) );
		}

		public ScriptValue item_to_slot( ScriptVariable slot )
		{
			switch ( TradeableItemDatabase.getConsumptionType( slot.intValue() ) )
			{
				case ConsumeItemRequest.EQUIP_HAT:
					return parseSlotValue( "hat" );
				case ConsumeItemRequest.EQUIP_WEAPON:
					return parseSlotValue( "weapon" );
				case ConsumeItemRequest.EQUIP_OFFHAND:
					return parseSlotValue( "off-hand" );
				case ConsumeItemRequest.EQUIP_SHIRT:
					return parseSlotValue( "shirt" );
				case ConsumeItemRequest.EQUIP_PANTS:
					return parseSlotValue( "pants" );
				case ConsumeItemRequest.EQUIP_FAMILIAR:
					return parseSlotValue( "familiar" );
				case ConsumeItemRequest.EQUIP_ACCESSORY:
					return parseSlotValue( "acc1" );
				default:
					return parseSlotValue( "none" );
			}
		}

		public ScriptValue outfit( ScriptVariable outfit )
		{
			DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue().toString() );
			return continueValue();
		}

		public ScriptValue have_outfit( ScriptVariable outfit )
		{	return new ScriptValue( KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue().toString() ) != null );
		}

		public ScriptValue have_equipped( ScriptVariable item )
		{	return new ScriptValue( KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) );
		}

		public ScriptValue monster_base_attack( ScriptVariable arg )
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());
			return new ScriptValue( monster.getAttack() );
		}

		public ScriptValue monster_base_defense( ScriptVariable arg )
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());
			return new ScriptValue( monster.getDefense() );
		}

		public ScriptValue monster_base_hp( ScriptVariable arg )
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());
			return new ScriptValue( monster.getHP() );
		}

		public ScriptValue weapon_hands( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.getHands( item.intValue() ) );
		}

		public ScriptValue ranged_weapon( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.isRanged( item.intValue() ) );
		}

		public ScriptValue equip_familiar( ScriptVariable familiar )
		{
			DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
			return continueValue();
		}

		public ScriptValue council()
		{
			DEFAULT_SHELL.executeLine( "council" );
			return VOID_VALUE;
		}

		public ScriptValue current_mind_control_level()
		{	return new ScriptValue( KoLCharacter.getMindControlLevel() );
		}

		public ScriptValue mind_control( ScriptVariable level )
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

		public ScriptValue contains_text( ScriptVariable source, ScriptVariable search )
		{	return new ScriptValue( source.toStringValue().toString().indexOf( search.toStringValue().toString() ) != -1 );
		}

		public ScriptValue cli_execute( ScriptVariable string )
		{
			DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
			return continueValue();
		}

		public ScriptValue visit_url( ScriptVariable string )
		{
			String location = string.toStringValue().toString();
			String url = location.indexOf( "?" ) != -1 ? location.substring( 0, location.indexOf( "?" ) ) : location;
			if ( url.indexOf( "send" ) != -1 || url.indexOf( "chat" ) != -1 || url.indexOf( "search" ) != -1 )
				return STRING_INIT;

			KoLRequest request = new KoLRequest( client, url, true );
			request.run();

			if ( request.getURLString().indexOf( "choice.php" ) != -1 && StaticEntity.getProperty( "makeBrowserDecisions" ).equals( "false" ) )
				request.handleChoiceResponse( request );

			return new ScriptValue( request.fullResponse == null ? "" : request.fullResponse );
		}

		public ScriptValue wait( ScriptVariable delay )
		{
			DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
			return VOID_VALUE;
		}

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
			int itemID = SorceressLair.fightTowerGuardians();
			return makeItemValue( itemID );
		}

		public ScriptValue chamber()
		{
			DEFAULT_SHELL.executeLine( "chamber" );
			return continueValue();
		}

		public ScriptValue nemesis()
		{
			DEFAULT_SHELL.executeLine( "nemesis" );
			return continueValue();
		}

		public ScriptValue guild()
		{
			DEFAULT_SHELL.executeLine( "guild" );
			return continueValue();
		}

		public ScriptValue gourd()
		{
			DEFAULT_SHELL.executeLine( "gourd" );
			return continueValue();
		}

		public ScriptValue tavern()
		{
			int result = client.locateTavernFaucet();
			return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
		}

		public ScriptValue train_familiar( ScriptVariable weight, ScriptVariable familiar )
		{
			DEFAULT_SHELL.executeLine( "train " + familiar.toStringValue() + " " + weight.intValue() );
			return continueValue();
		}

		public ScriptValue retrieve_item( ScriptVariable count, ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
				return continueValue();

			AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
			return continueValue();
		}

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

		public ScriptValue url_encode( ScriptVariable arg ) throws UnsupportedEncodingException
		{	return new ScriptValue( URLEncoder.encode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue url_decode( ScriptVariable arg ) throws UnsupportedEncodingException
		{	return new ScriptValue( URLDecoder.decode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue get_property( ScriptVariable name )
		{	return new ScriptValue( StaticEntity.getProperty( name.toStringValue().toString() ) );
		}

		public ScriptValue set_property( ScriptVariable name, ScriptVariable value )
		{
			// In order to avoid code duplication for combat
			// related settings, use the shell.

			DEFAULT_SHELL.executeLine( "set " + name.toStringValue().toString() + "=" + value.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue count( ScriptVariable arg )
		{	return new ScriptValue( arg.getValue().count() );
		}

		public ScriptValue file_to_map( ScriptVariable filename, ScriptVariable map_variable )
		{
			BufferedReader reader = DataUtilities.getReader( "", filename.toStringValue().toString() );
			ScriptAggregateValue result = (ScriptAggregateValue) map_variable.getValue();
			String [] data = null;

			while ( (data = KoLDatabase.readData( reader )) != null )
			{
				try
				{
					readMap( result, data );
				}
				catch ( Exception e )
				{
					// Okay, runtime error. Indicate that
					// there was a bad line in the data
					// file and print the stack trace.

					StringBuffer buffer = new StringBuffer( "Invalid line in data file:" );
					buffer.append( LINE_BREAK );

					for ( int i = 0; i < data.length; ++i )
					{
						buffer.append( '\t' );
						buffer.append( data[i] );
					}

					StaticEntity.printStackTrace( e, buffer.toString() );
					return FALSE_VALUE;
				}
			}

			return TRUE_VALUE;
		}

		public ScriptValue map_to_file( ScriptVariable map_variable, ScriptVariable filename )
		{
			try
			{
				File data = new File( filename.toStringValue().toString() );

				if ( data.getParentFile() != null )
					data.getParentFile().mkdirs();

				if ( data.exists() )
					data.delete();

				PrintStream writer = new PrintStream( new FileOutputStream( data, true ) );
				printMap( writer, "", (ScriptAggregateValue) map_variable.getValue() );
				writer.close();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
				return FALSE_VALUE;
			}

			return TRUE_VALUE;
		}

		public ScriptValue my_location()
		{
			return KoLCharacter.getNextAdventure() == null ?
				parseLocationValue( "Rest" ) : parseLocationValue( KoLCharacter.getNextAdventure().getAdventureName() );
		}

		public ScriptValue have_mushroom_plot()
		{	return new ScriptValue( MushroomPlot.ownsPlot() );
		}

		public ScriptValue restore_hp( ScriptVariable amount )
		{	return new ScriptValue( StaticEntity.getClient().recoverHP( amount.intValue() ) );
		}

		public ScriptValue restore_mp( ScriptVariable amount )
		{
			int desiredMP = amount.intValue();
			while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
				StaticEntity.getClient().recoverMP( desiredMP );
			return continueValue();
		}

		public ScriptValue skill_to_effect( ScriptVariable skill )
		{
			String effectName = UneffectRequest.skillToEffect( skill.toStringValue().toString() );
			return !StatusEffectDatabase.contains( effectName ) ? EFFECT_INIT :
				new ScriptValue( EFFECT_TYPE, StatusEffectDatabase.getEffectID( effectName ), effectName );
		}

		public ScriptValue effect_to_skill( ScriptVariable effect )
		{
			String skillName = UneffectRequest.effectToSkill( effect.toStringValue().toString() );
			return !ClassSkillsDatabase.contains( skillName ) ? SKILL_INIT :
				new ScriptValue( SKILL_TYPE, ClassSkillsDatabase.getSkillID( skillName ), skillName );
		}

		public ScriptValue mp_cost( ScriptVariable skill )
		{	return new ScriptValue( ClassSkillsDatabase.getMPConsumptionByID( skill.intValue() ) );
		}

		public ScriptValue can_equip( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.canEquip( TradeableItemDatabase.getItemName( item.intValue() ) ) );
		}

		public ScriptValue familiar_equipment( ScriptVariable familiar )
		{	return parseItemValue( FamiliarsDatabase.getFamiliarItem( familiar.intValue() ) );
		}

		public ScriptValue stat_bonus_today()
		{
			return KoLmafiaCLI.testConditional( "today is muscle day" ) ? parseStatValue( "muscle" ) :
				KoLmafiaCLI.testConditional( "today is mysticism day" ) ? parseStatValue( "mysticality" ) :
				KoLmafiaCLI.testConditional( "today is moxie day" ) ? parseStatValue( "moxie" ) : STAT_INIT;
		}

		public ScriptValue stat_bonus_tomorrow()
		{
			return KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) ? parseStatValue( "muscle" ) :
				KoLmafiaCLI.testConditional( "tomorrow is mysticism day" ) ? parseStatValue( "mysticality" ) :
				KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) ? parseStatValue( "moxie" ) : STAT_INIT;
		}

		public ScriptValue monster_level_adjustment()
		{	return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue familiar_weight_adjustment()
		{
			// Weight from skills and effects
			int val = (KoLCharacter.getFamiliar().getID() == 38 ) ? KoLCharacter.getDodecapedeWeightAdjustment() : KoLCharacter.getFamiliarWeightAdjustment();
			// plus weight from equipment
			val += KoLCharacter.getFamiliarItemWeightAdjustment();
			return new ScriptValue( val );
		}

		public ScriptValue mana_cost_modifier()
		{	return new ScriptValue( KoLCharacter.getManaCostModifier() );
		}

		public ScriptValue raw_damage_absorption()
		{	return new ScriptValue( KoLCharacter.getDamageAbsorption() );
		}

		public ScriptValue damage_absorption_percent()
		{
			int raw = KoLCharacter.getDamageAbsorption();
			if ( raw == 0 )
				return ZERO_FLOAT_VALUE;
			// 1 - 2^(-DA/350)
			double percent = 100.0 * ( 1.0 - Math.pow( 2.0, -raw / 350.0 ) );
			return new ScriptValue( percent );
		}

		public ScriptValue damage_reduction()
		{	return new ScriptValue( KoLCharacter.getDamageReduction() );
		}

		public ScriptValue elemental_resistance( ScriptVariable element )
		{	return new ScriptValue( KoLCharacter.getElementalResistance( element.intValue() ) );
		}

		public ScriptValue cold_resistance()
		{	return new ScriptValue( KoLCharacter.getColdResistance() );
		}

		public ScriptValue hot_resistance()
		{	return new ScriptValue( KoLCharacter.getHotResistance() );
		}

		public ScriptValue sleaze_resistance()
		{	return new ScriptValue( KoLCharacter.getSleazeResistance() );
		}

		public ScriptValue spooky_resistance()
		{	return new ScriptValue( KoLCharacter.getSpookyResistance() );
		}

		public ScriptValue stench_resistance()
		{	return new ScriptValue( KoLCharacter.getStenchResistance() );
		}

		public ScriptValue combat_percent_modifier()
		{	return new ScriptValue( KoLCharacter.getCombatPercentAdjustment() );
		}

		public ScriptValue initiative_modifier()
		{	return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
		}

		public ScriptValue fixed_experience_bonus()
                        {	return new ScriptValue( KoLCharacter.getFixedXPAdjustment() + (double)KoLCharacter.getMonsterLevelAdjustment() / 5.0 );
		}

		public ScriptValue meat_drop_modifier()
		{	return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
		}

		public ScriptValue item_drop_modifier()
		{	return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
		}

		public ScriptValue buffed_hit_stat()
		{
			if ( KoLCharacter.rigatoniActive() )
				return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
			if ( KoLCharacter.rangedWeapon() )
				return new ScriptValue( KoLCharacter.getAdjustedMoxie() );
			return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
		}

		public ScriptValue current_hit_stat()
		{
			if ( KoLCharacter.rigatoniActive() )
				return parseStatValue( "mysticality" );
			if ( KoLCharacter.rangedWeapon() )
				return parseStatValue( "moxie" );
			return parseStatValue( "muscle" );
		}
	}

	private static class ScriptFunctionList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptFunction n )
		{
			super.add( n );
			return true;
		}

		public int indexOf( String name )
		{	return indexOf( name, 0 );
		}

		public int indexOf( String name, int searchIndex )
		{
			for ( int i = searchIndex; i < size(); ++i )
				if ( ((ScriptFunction) get(i)).getName().equalsIgnoreCase( name ) )
					return i;

			return -1;
		}

		public ScriptFunction findFunction( String name )
		{	return findFunction( name, 0 );
		}

		public ScriptFunction findFunction( String name, int searchIndex )
		{
			int index = indexOf( name, searchIndex );
			return index < 0 ? null : (ScriptFunction) get( index );
		}
	}

	private static class ScriptVariable extends ScriptSymbol
	{
		ScriptValue	content;

		public ScriptVariable( ScriptType type )
		{
			super( null );
			content = new ScriptValue( type );
		}

		public ScriptVariable( String name, ScriptType type )
		{
			super( name );
			content = new ScriptValue( type );
		}

		public ScriptType getType()
		{	return content.getType();
		}

		public ScriptValue getValue()
		{	return content;
		}

		public Object rawValue()
		{	return content.rawValue();
		}

		public int intValue()
		{	return content.intValue();
		}

		public ScriptValue toStringValue()
		{	return content.toStringValue();
		}

		public double floatValue()
		{	return content.floatValue();
		}

		public void forceValue( ScriptValue targetValue )
		{	content = targetValue;
		}

		public void setValue( ScriptValue targetValue )
		{
			if ( getType().equals( targetValue.getType() ) )
			{
				content = targetValue;
			}
			else if ( getType().equals( TYPE_STRING ) )
			{
				content = targetValue.toStringValue();
			}
			else if ( getType().equals( TYPE_INT ) && targetValue.getType().equals( TYPE_FLOAT ) )
			{
				content = targetValue.toIntValue();
			}
			else if ( getType().equals( TYPE_FLOAT ) && targetValue.getType().equals( TYPE_INT ) )
			{
				content = targetValue.toFloatValue();
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
		{	return (ScriptVariable)super.findSymbol( name );
		}

		public ScriptVariable getFirstVariable()
		{	return ( ScriptVariable)getFirstElement();
		}

		public ScriptVariable getNextVariable()
		{	return ( ScriptVariable)getNextElement();
		}
	}

	private static class ScriptVariableReference extends ScriptValue
	{
		protected ScriptVariable target;

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

		public int compareTo( Object o )
		{	return target.getName().compareTo( ((ScriptVariableReference)o).target.getName() );
		}

		public ScriptValue execute() throws AdvancedScriptException
		{	return target.getValue();
		}

		public ScriptValue getValue() throws AdvancedScriptException
		{	return target.getValue();
		}

		public void forceValue( ScriptValue targetValue )
		{	target.forceValue( targetValue );
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
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
			ScriptType type = target.getType();
			for ( int i = 0; i < indices.size(); ++i )
				type = ((ScriptCompositeType)type).getDataType( indices.get(i) );
			return type;
		}

		public String getName()
		{	return target.getName() + "[]";
		}

		public ScriptValue execute() throws AdvancedScriptException
		{	return getValue();
		}

		// Evaluate all the indices and step through the slices.
		//
		// When done, this.slice has the final slice and this.index has
		// the final evaluated index.

		private boolean getSlice() throws AdvancedScriptException
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

				trace( "Index #" + i + ": " + index );

				index = exp.execute();
				captureValue( index );

				trace( "[" + executionStateString( currentState ) + "] <- " + index );

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

				trace( "AREF: " + slice.toString() );
			}

			traceUnindent();

			return true;
		}

		public ScriptValue getValue() throws AdvancedScriptException
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
				return result;
			}

			return null;
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
		{
			// Iterate through indices to final slice
			if ( getSlice() )
				slice.aset( index, targetValue );
		}

		public ScriptValue removeKey() throws AdvancedScriptException
		{
			// Iterate through indices to final slice
			if ( getSlice() )
			{
				ScriptValue result = slice.remove( index );
				if ( result != null )
					return result;
				return slice.initialValue( index );
			}
			return null;
		}

		public boolean contains( ScriptValue index ) throws AdvancedScriptException
		{
			// Iterate through indices to final slice
			if ( getSlice() )
				return slice.aref( index ) != null;
			return false;
		}

		public ScriptExpression getFirstIndex()
		{	return ( ScriptExpression)indices.getFirstElement();
		}

		public ScriptExpression getNextIndex()
		{	return ( ScriptExpression)indices.getNextElement();
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

		public ScriptVariableReference getFirstVariableReference()
		{	return (ScriptVariableReference)getFirstElement();
		}

		public ScriptVariableReference getNextVariableReference()
		{	return (ScriptVariableReference)getNextElement();
		}

		public ScriptVariableReference getNextVariableReference( ScriptVariableReference current )
		{	return (ScriptVariableReference)getNextElement( current );
		}
	}

	private static class ScriptCommand
	{
		public ScriptValue execute() throws AdvancedScriptException
		{
			return null;
		}
        }

	private class ScriptFlowControl extends ScriptCommand
	{
		int command;

		public ScriptFlowControl( String command ) throws AdvancedScriptException
		{
			if ( command.equalsIgnoreCase( "break" ) )
				this.command = COMMAND_BREAK;
			else if ( command.equalsIgnoreCase( "continue" ) )
				this.command = COMMAND_CONTINUE;
			else if ( command.equalsIgnoreCase( "exit" ) )
				this.command = COMMAND_EXIT;
			else
				throw new AdvancedScriptException( command + " is not a command " + getLineAndFile() );
		}

		public ScriptFlowControl( int command )
		{	this.command = command;
		}

		public String toString()
		{
			switch ( this.command)
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

		public ScriptValue execute() throws AdvancedScriptException
		{
			traceIndent();
			trace( toString() );
			traceUnindent();

			switch ( this.command)
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

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType ) throws AdvancedScriptException
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

		public ScriptValue execute() throws AdvancedScriptException
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
		protected ScriptScope scope;
		private ScriptExpression condition;

		public ScriptConditional( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
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

		public ScriptValue execute() throws AdvancedScriptException
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
		private ScriptConditionalList elseLoops;

		public ScriptIf( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
		{
			super( scope, condition );
			elseLoops = new ScriptConditionalList();
		}

		public ScriptConditional getFirstElseLoop()
		{	return (ScriptConditional)elseLoops.getFirstElement();
		}

		public ScriptConditional getNextElseLoop()
		{	return (ScriptConditional)elseLoops.getNextElement();
		}

		public void addElseLoop( ScriptConditional elseLoop ) throws AdvancedScriptException
		{
			elseLoops.addElement( elseLoop );
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptValue result = super.execute();
			if ( currentState != STATE_NORMAL || result == TRUE_VALUE )
				return result;

			// Conditional failed. Move to else clauses
			for ( ScriptConditional elseLoop = elseLoops.getFirstScriptConditional(); elseLoop != null; elseLoop = elseLoops.getNextScriptConditional() )
			{
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
		public ScriptElseIf( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
		{	super( scope, condition );
		}

		public String toString()
		{	return "else if";
		}
	}

	private class ScriptElse extends ScriptConditional
	{
		public ScriptElse( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
		{	super( scope, condition );
		}

		public ScriptValue execute() throws AdvancedScriptException
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

	private static class ScriptConditionalList extends ScriptList
	{
		public boolean addElement( ScriptConditional n )
		{	return super.addElement( n );
		}

		public ScriptConditional getFirstScriptConditional()
		{	return (ScriptConditional)getFirstElement();
		}

		public ScriptConditional getNextScriptConditional()
		{	return (ScriptConditional)getNextElement();
		}
	}

	private class ScriptLoop extends ScriptCommand
	{
		protected ScriptScope scope;

		public ScriptLoop( ScriptScope scope ) throws AdvancedScriptException
		{
			this.scope = scope;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptValue execute() throws AdvancedScriptException
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

		public ScriptForeach( ScriptScope scope, ScriptVariableReferenceList variableReferences, ScriptVariableReference aggregate ) throws AdvancedScriptException
		{
			super( scope );
			this.variableReferences = variableReferences;
			this.aggregate = aggregate;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{	return variableReferences;
		}

		public ScriptVariableReference getFirstVariableReference()
		{	return variableReferences.getFirstVariableReference();
		}

		public ScriptVariableReference getNextVariableReference()
		{	return variableReferences.getNextVariableReference();
		}

		public ScriptVariableReference getAggregate()
		{	return aggregate;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue)aggregate.execute();
			captureValue( slice );
			if ( currentState == STATE_EXIT )
				return null;

			// Iterate over the slice with bound keyvar
			return executeSlice( slice, variableReferences.getFirstVariableReference() );

		}

		private ScriptValue executeSlice( ScriptAggregateValue slice, ScriptVariableReference variable ) throws AdvancedScriptException
		{
			// Get an array of keys for the slice
			ScriptValue [] keys = slice.keys();

			// Get the next key variable
			ScriptVariableReference nextVariable = variableReferences.getNextVariableReference( variable );

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
					result = executeSlice( nextSlice, nextVariable );
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

		public ScriptWhile( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );

		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptValue execute() throws AdvancedScriptException
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

		public ScriptRepeat( ScriptScope scope, ScriptExpression condition ) throws AdvancedScriptException
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );

		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptValue execute() throws AdvancedScriptException
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
		private boolean up;

		public ScriptFor( ScriptScope scope, ScriptVariableReference variable, ScriptExpression initial, ScriptExpression last, ScriptExpression increment, boolean up ) throws AdvancedScriptException
		{
			super( scope );
			this.variable = variable;
			this.initial = initial;
			this.last = last;
			this.increment = increment;
			this.up = up;
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

		public boolean getUp()
		{	return up;
		}

		public ScriptValue execute() throws AdvancedScriptException
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
			int adjustment = up ? incrementValue.intValue() : -incrementValue.intValue();
			int end = lastValue.intValue();

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
				current += adjustment;
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return "foreach";
		}
	}

	private class ScriptCall extends ScriptValue
	{
		private ScriptFunction target;
		private ScriptExpressionList params;

		public ScriptCall( String functionName, ScriptScope scope, ScriptExpressionList params ) throws AdvancedScriptException
		{
			target = findFunction( functionName, scope, params );
			if ( target == null )
				throw new AdvancedScriptException( "Undefined reference " + functionName + " " + getLineAndFile() );
			this.params = params;
		}

		private ScriptFunction findFunction( String name, ScriptScope scope, ScriptExpressionList params ) throws AdvancedScriptException
		{
			if ( scope == null )
				return null;
			return scope.findFunction( name, params );
		}

		public ScriptFunction getTarget()
		{	return target;
		}

		public ScriptExpression getFirstParam()
		{	return ( ScriptExpression )params.getFirstElement();
		}

		public ScriptExpression getNextParam()
		{	return ( ScriptExpression )params.getNextElement();
		}

		public ScriptType getType()
		{	return target.getType();
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !KoLmafia.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();

			// Save current variable bindings
			target.saveBindings();

			ScriptVariableReference paramVarRef = target.getFirstParam();
			ScriptExpression paramValue = params.getFirstExpression();

			int paramCount = 0;
			while ( paramVarRef != null )
			{
				++paramCount;
				if ( paramValue == null )
					throw new RuntimeException( "Internal error: illegal arguments" );

				trace( "Param #" + paramCount + ": " + paramValue );

				ScriptValue value = paramValue.execute();
				captureValue( value );

				trace( "[" + executionStateString( currentState ) + "] <- " + value );

				if ( currentState == STATE_EXIT )
				{
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

				paramVarRef = target.getNextParam();
				paramValue = params.getNextExpression();
			}

			if ( paramValue != null )
				throw new RuntimeException( "Internal error: illegal arguments" );

			trace( "Entering function " + target.getName() );
			ScriptValue result = target.execute();

			trace( "Function " + target.getName() + " returned: " + result );

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

		public ScriptAssignment( ScriptVariableReference lhs, ScriptExpression rhs ) throws AdvancedScriptException
		{
			this.lhs = lhs;
			this.rhs = rhs;

			if ( !validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
				throw new AdvancedScriptException( "Cannot store " + rhs.getType() + " in " + lhs + " " + getLineAndFile() );
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

		public ScriptValue execute() throws AdvancedScriptException
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
		protected boolean primitive;
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

		public boolean isPrimitive()
		{	return primitive;
		}

		public boolean equals( ScriptType type )
		{
			if ( this.type == type.type )
				return true;
			return false;
		}

		public boolean equals( int type )
		{
			if ( this.type == type )
				return true;
			return false;
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
			case TYPE_ZODIAC:
				return ZODIAC_INIT;
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

		public ScriptValue parseValue( String name ) throws AdvancedScriptException
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
			case TYPE_ZODIAC:
				return parseZodiacValue( name );
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
	}

	private static class ScriptCompositeType extends ScriptType
	{
		public ScriptCompositeType( String name, int type)
		{
			super( name, type );
			this.primitive = false;
		}

		public ScriptType getIndexType()
		{	return null;
		}

		public ScriptType getDataType( Object key )
		{	return null;
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
				 size == ((ScriptAggregateType)o).size &&
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

		public ScriptExpression initialValueExpression()
		{	return new ScriptTypeInitializer( this );
		}
	}

	private static class ScriptRecordType extends ScriptCompositeType
	{
		private String [] fieldNames;
		private ScriptType [] fieldTypes;

		public ScriptRecordType( String name, String [] fieldNames,ScriptType [] fieldTypes )
		{
			super( name, TYPE_RECORD );
			this.fieldNames = fieldNames;
			this.fieldTypes = fieldTypes;
		}

		public String [] getFieldNames()
		{	return fieldNames;
                }

		public ScriptType [] getFieldTypes()
		{	return fieldTypes;
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
					return new ScriptValue( index );
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
				String val = key.toString().toLowerCase();
				for ( int index = 0; index < fieldNames.length; ++ index )
					if ( val.equals( fieldNames[index] ) )
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

		public ScriptExpression initialValueExpression()
		{	return new ScriptTypeInitializer( this );
		}
	}

	private static class ScriptTypeInitializer extends ScriptValue
	{
		protected ScriptType type;

		public ScriptTypeInitializer( ScriptType type )
		{	this.type = type;
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{	return type.initialValue();
		}

		public String toString()
		{	return "init";
		}
	}

	private static class ScriptRecordInitializer extends ScriptValue
	{
		protected ScriptRecordType type;

		public ScriptRecordInitializer( ScriptRecordType type )
		{	this.type = type;
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{	return type.initialValue();
		}

		public String toString()
		{	return "init";
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

		public ScriptType getFirstType()
		{	return (ScriptType)getFirstElement();
		}

		public ScriptType getNextType()
		{	return (ScriptType)getNextElement();
		}
	}

	private static class ScriptValue extends ScriptExpression implements Comparable
	{
		protected ScriptType type;

		protected int contentInt = 0;
		protected double contentFloat = 0.0;
		protected String contentString = null;
		protected Object content = null;

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

		public ScriptValue( double value )
		{
			this.type = FLOAT_TYPE;
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
				return new ScriptValue( (double) contentInt );
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
			return type;
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

		public double floatValue()
		{
			return contentFloat;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			return this;
		}

		public int compareTo( Object o )
		{
			if ( !( o instanceof ScriptValue ) )
				throw new ClassCastException();

			ScriptValue it = (ScriptValue) o;

			if ( contentString != null )
				return contentString.compareTo( it.contentString );

			if ( type == BOOLEAN_TYPE || type == INT_TYPE )
				return contentInt < it.contentInt ? -1 : contentInt == it.contentInt ? 0 : 1;

			if ( type == FLOAT_TYPE )
				return contentFloat < it.contentFloat ? -1 : contentFloat == it.contentFloat ? 0 : 1;

			return -1;
		}

		public int count()
		{	return 1;
		}

		public boolean contains( ScriptValue index ) throws AdvancedScriptException
		{	return false;
		}

		public boolean equals( Object o )
		{	return o == null || !(o instanceof ScriptValue) ? false : this.compareTo( (Comparable) o ) == 0;
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

		public ScriptValue [] keys()
		{	return new ScriptValue[0];
		}

		public ScriptValue initialValue( Object key )
		{	return ((ScriptCompositeType)type).getDataType( key).initialValue();
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
				throw new RuntimeException( "Array index out of bounds" );
			return array[ i ];
		}

		public void aset( ScriptValue key,  ScriptValue val )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
				throw new RuntimeException( "Array index out of bounds" );
			array[ index ] = val;
		}

		public ScriptValue remove( ScriptValue key )
		{
			ScriptValue [] array = (ScriptValue [])content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
				throw new RuntimeException( "Array index out of bounds" );
			ScriptValue result = array[ index ];
			array[ index ] = getDataType().initialValue();
			return result;
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

		public void aset( ScriptValue key,  ScriptValue val )
		{
			TreeMap map = (TreeMap)content;
			map.put( key, val );
		}

		public ScriptValue remove( ScriptValue key )
		{
			TreeMap map = (TreeMap)content;
			return (ScriptValue)map.remove( key );
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

		public void aset( ScriptValue key,  ScriptValue val )
		{
			int index = ((ScriptRecordType)type).indexOf( key );
			if ( index < 0 )
				throw new RuntimeException( "Internal error: field index out of bounds" );
			ScriptValue [] array = (ScriptValue [])content;
			array[ index ] = val;
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

		public ScriptValue [] keys()
		{
			ScriptRecordType type = (ScriptRecordType)this.type;
			String [] fields = type.getFieldNames();
			int size = fields.length;
			ScriptValue [] result = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
				result[i] = new ScriptValue( fields[i] );
			return result;
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

		public ScriptExpression( ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper ) throws AdvancedScriptException
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

		public ScriptValue execute() throws AdvancedScriptException
		{
			try
			{
				return oper.applyTo( lhs, rhs );
			}
			catch( AdvancedScriptException e )
			{
				throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing." );
			}
		}

		public String toString()
		{
			if ( rhs == null )
				return oper.toString() + " " + lhs;
			return lhs + " " + oper.toString() + " " + rhs;
		}
	}

	private static class ScriptExpressionList extends ScriptList
	{
		public ScriptExpression getFirstExpression()
		{	return (ScriptExpression) getFirstElement();
		}

		public ScriptExpression getNextExpression()
		{	return (ScriptExpression) getNextElement();
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

			if ( operator.equals( "==" ) || operator.equals( "!=" ) )
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

		public ScriptValue applyTo( ScriptExpression lhs, ScriptExpression rhs ) throws AdvancedScriptException
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
					return new ScriptValue( 0.0 - leftValue.floatValue() );
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

			if ( operator.equals( "==" ) )
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
			double lfloat = 0.0, rfloat = 0.0;
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
				return new ScriptValue( Math.pow( lfloat, rfloat ) );
			}

			if ( operator.equals( "/" ) )
			{
				if ( isInt )
					return new ScriptValue( lint / rint );
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

			if ( operator.equals( "==" ) )
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
			throw new RuntimeException( "Internal error: illegal operator." );
		}
	}

	private static class ScriptList extends ArrayList
	{
		private int searchIndex = -1;

		public boolean addElement( Object n )
		{
			add( n );
			return true;
		}

		public Object getFirstElement()
		{
			searchIndex = -1;
			return getNextElement();
		}

		public Object getNextElement()
		{
			if ( ++searchIndex >= size() )
				return null;
			return get( searchIndex );
		}

		public Object getNextElement( Object n )
		{
			searchIndex = indexOf( n );
			if ( searchIndex == -1 )
				return null;
			return getNextElement();
		}
	}

	private static class AdvancedScriptException extends Exception
	{
		AdvancedScriptException( String s )
		{	super( s );
		}
	}
}
