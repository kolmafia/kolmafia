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

package net.sourceforge.kolmafia.ASH;

import java.io.BufferedReader;
import java.io.File;
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.ItemManageFrame;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ASH.ParseTree;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptAggregateType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptAssignment;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptBasicScript;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptCall;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptCommand;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptCompositeReference;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptConditional;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptElse;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptElseIf;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptExistingFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptExpression;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptExpressionList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFlowControl;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFor;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptForeach;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFunctionList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptIf;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptNamedType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptOperator;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptRecord;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptRecordType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptRepeat;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptReturn;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptScope;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptSymbol;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptSymbolTable;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptTypeInitializer;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptTypeList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptUserDefinedFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptValue;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariable;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariableList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariableReference;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariableReferenceList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptWhile;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;
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
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

public class Interpreter
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

	public static final ScriptType ANY_TYPE = new ScriptType( "any", Interpreter.TYPE_ANY );
	public static final ScriptType VOID_TYPE = new ScriptType( "void", Interpreter.TYPE_VOID );
	public static final ScriptType BOOLEAN_TYPE = new ScriptType( "boolean", Interpreter.TYPE_BOOLEAN );
	public static final ScriptType INT_TYPE = new ScriptType( "int", Interpreter.TYPE_INT );
	public static final ScriptType FLOAT_TYPE = new ScriptType( "float", Interpreter.TYPE_FLOAT );
	public static final ScriptType STRING_TYPE = new ScriptType( "string", Interpreter.TYPE_STRING );
	public static final ScriptType BUFFER_TYPE = new ScriptType( "buffer", Interpreter.TYPE_BUFFER );
	public static final ScriptType MATCHER_TYPE = new ScriptType( "matcher", Interpreter.TYPE_MATCHER );

	public static final ScriptType ITEM_TYPE = new ScriptType( "item", Interpreter.TYPE_ITEM );
	public static final ScriptType LOCATION_TYPE = new ScriptType( "location", Interpreter.TYPE_LOCATION );
	public static final ScriptType CLASS_TYPE = new ScriptType( "class", Interpreter.TYPE_CLASS );
	public static final ScriptType STAT_TYPE = new ScriptType( "stat", Interpreter.TYPE_STAT );
	public static final ScriptType SKILL_TYPE = new ScriptType( "skill", Interpreter.TYPE_SKILL );
	public static final ScriptType EFFECT_TYPE = new ScriptType( "effect", Interpreter.TYPE_EFFECT );
	public static final ScriptType FAMILIAR_TYPE = new ScriptType( "familiar", Interpreter.TYPE_FAMILIAR );
	public static final ScriptType SLOT_TYPE = new ScriptType( "slot", Interpreter.TYPE_SLOT );
	public static final ScriptType MONSTER_TYPE = new ScriptType( "monster", Interpreter.TYPE_MONSTER );
	public static final ScriptType ELEMENT_TYPE = new ScriptType( "element", Interpreter.TYPE_ELEMENT );

	public static final ScriptType AGGREGATE_TYPE = new ScriptType( "aggregate", Interpreter.TYPE_AGGREGATE );
	public static final ScriptAggregateType RESULT_TYPE =
		new ScriptAggregateType( Interpreter.INT_TYPE, Interpreter.ITEM_TYPE );
	public static final ScriptAggregateType REGEX_GROUP_TYPE =
		new ScriptAggregateType(
			new ScriptAggregateType( Interpreter.STRING_TYPE, Interpreter.INT_TYPE ), Interpreter.INT_TYPE );

	// Common values

	public static final ScriptValue VOID_VALUE = new ScriptValue();
	public static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	public static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	public static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	public static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	public static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0f );

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	public static final ScriptValue BOOLEAN_INIT = Interpreter.FALSE_VALUE;
	public static final ScriptValue INT_INIT = Interpreter.ZERO_VALUE;
	public static final ScriptValue FLOAT_INIT = Interpreter.ZERO_FLOAT_VALUE;
	public static final ScriptValue STRING_INIT = new ScriptValue( "" );

	public static final ScriptValue ITEM_INIT = new ScriptValue( Interpreter.ITEM_TYPE, -1, "none" );
	public static final ScriptValue LOCATION_INIT = new ScriptValue( Interpreter.LOCATION_TYPE, "none", (Object) null );
	public static final ScriptValue CLASS_INIT = new ScriptValue( Interpreter.CLASS_TYPE, -1, "none" );
	public static final ScriptValue STAT_INIT = new ScriptValue( Interpreter.STAT_TYPE, -1, "none" );
	public static final ScriptValue SKILL_INIT = new ScriptValue( Interpreter.SKILL_TYPE, -1, "none" );
	public static final ScriptValue EFFECT_INIT = new ScriptValue( Interpreter.EFFECT_TYPE, -1, "none" );
	public static final ScriptValue FAMILIAR_INIT = new ScriptValue( Interpreter.FAMILIAR_TYPE, -1, "none" );
	public static final ScriptValue SLOT_INIT = new ScriptValue( Interpreter.SLOT_TYPE, -1, "none" );
	public static final ScriptValue MONSTER_INIT = new ScriptValue( Interpreter.MONSTER_TYPE, "none", (Object) null );
	public static final ScriptValue ELEMENT_INIT = new ScriptValue( Interpreter.ELEMENT_TYPE, "none", (Object) null );

	// Variables used during parsing

	public static final ScriptFunctionList existingFunctions = Interpreter.getExistingFunctions();
	private static final ScriptTypeList simpleTypes = Interpreter.getSimpleTypes();
	private static final ScriptSymbolTable reservedWords = Interpreter.getReservedWords();

	private static Interpreter currentAnalysis = null;

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
	public static int currentState = Interpreter.STATE_NORMAL;
	public static boolean isExecuting = false;
	private static String lastImportString = "";
	private String notifyRecipient = null;

	// Feature control;
	// disabled until and if we choose to document the feature

	private static final boolean arrays = false;

	public Interpreter()
	{
		this.global = new ScriptScope( new ScriptVariableList(), this.getExistingFunctionScope() );
	}

	private Interpreter( final Interpreter source, final File scriptFile )
	{
		this.fileName = scriptFile.getPath();
		this.global = source.global;
		this.imports = source.imports;

		try
		{
			this.commandStream = new LineNumberReader( new InputStreamReader( DataUtilities.getInputStream( scriptFile ) ) );

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

	public String getFileName()
	{
		return this.fileName;
	}

	public TreeMap getImports()
	{
		return this.imports;
	}

	// **************** Data Types *****************

	// For each simple data type X, we supply:
	// public static final ScriptValue parseXValue( String name );

	public static final ScriptValue parseBooleanValue( final String name )
	{
		if ( name.equalsIgnoreCase( "true" ) )
		{
			return Interpreter.TRUE_VALUE;
		}
		if ( name.equalsIgnoreCase( "false" ) )
		{
			return Interpreter.FALSE_VALUE;
		}

		if ( Interpreter.isExecuting )
		{
			return StaticEntity.parseInt( name ) == 0 ? Interpreter.FALSE_VALUE : Interpreter.TRUE_VALUE;
		}

		throw new AdvancedScriptException( "Can't interpret '" + name + "' as a boolean" );
	}

	public static final ScriptValue parseIntValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseInt( name ) );
	}

	public static final ScriptValue parseFloatValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseFloat( name ) );
	}

	public static final ScriptValue parseStringValue( final String name )
	{
		return new ScriptValue( name );
	}

	public static final ScriptValue parseItemValue( String name )
	{
		if ( name == null || name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.ITEM_INIT;
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
					if ( Interpreter.isExecuting )
					{
						return Interpreter.ITEM_INIT;
					}

					throw new AdvancedScriptException( "Item " + name + " not found in database" );
				}

				itemId = item.getItemId();
				name = ItemDatabase.getItemName( itemId );
				return new ScriptValue( Interpreter.ITEM_TYPE, itemId, name );
			}
		}

		// Since it is numeric, parse the integer value
		// and store it inside of the contentInt.

		itemId = StaticEntity.parseInt( name );
		name = ItemDatabase.getItemName( itemId );
		return new ScriptValue( Interpreter.ITEM_TYPE, itemId, name );
	}

	public static final ScriptValue parseLocationValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.LOCATION_INIT;
			}

			throw new AdvancedScriptException( "Location " + name + " not found in database" );
		}

		return new ScriptValue( Interpreter.LOCATION_TYPE, name, (Object) content );
	}

	public static final int classToInt( final String name )
	{
		for ( int i = 0; i < Interpreter.CLASSES.length; ++i )
		{
			if ( name.equalsIgnoreCase( Interpreter.CLASSES[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final ScriptValue parseClassValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return Interpreter.CLASS_INIT;
		}

		int num = Interpreter.classToInt( name );
		if ( num < 0 )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.CLASS_INIT;
			}

			throw new AdvancedScriptException( "Unknown class " + name );
		}

		return new ScriptValue( Interpreter.CLASS_TYPE, num, Interpreter.CLASSES[ num ] );
	}

	public static final int statToInt( final String name )
	{
		for ( int i = 0; i < Interpreter.STATS.length; ++i )
		{
			if ( name.equalsIgnoreCase( Interpreter.STATS[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final ScriptValue parseStatValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.STAT_INIT;
		}

		int num = Interpreter.statToInt( name );
		if ( num < 0 )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.STAT_INIT;
			}

			throw new AdvancedScriptException( "Unknown stat " + name );
		}

		return new ScriptValue( Interpreter.STAT_TYPE, num, Interpreter.STATS[ num ] );
	}

	public static final ScriptValue parseSkillValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.SKILL_INIT;
		}

		List skills = SkillDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.SKILL_INIT;
			}

			throw new AdvancedScriptException( "Skill " + name + " not found in database" );
		}

		int num = SkillDatabase.getSkillId( (String) skills.get( 0 ) );
		name = SkillDatabase.getSkillName( num );
		return new ScriptValue( Interpreter.SKILL_TYPE, num, name );
	}

	public static final ScriptValue parseEffectValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return Interpreter.EFFECT_INIT;
		}

		AdventureResult effect = KoLmafiaCLI.getFirstMatchingEffect( name );
		if ( effect == null )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.EFFECT_INIT;
			}

			throw new AdvancedScriptException( "Effect " + name + " not found in database" );
		}

		int num = EffectDatabase.getEffectId( effect.getName() );
		name = EffectDatabase.getEffectName( num );
		return new ScriptValue( Interpreter.EFFECT_TYPE, num, name );
	}

	public static final ScriptValue parseFamiliarValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.FAMILIAR_INIT;
		}

		int num = FamiliarDatabase.getFamiliarId( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.FAMILIAR_INIT;
			}

			throw new AdvancedScriptException( "Familiar " + name + " not found in database" );
		}

		name = FamiliarDatabase.getFamiliarName( num );
		return new ScriptValue( Interpreter.FAMILIAR_TYPE, num, name );
	}

	public static final ScriptValue parseSlotValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.SLOT_INIT;
		}

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.SLOT_INIT;
			}

			throw new AdvancedScriptException( "Bad slot name " + name );
		}

		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( Interpreter.SLOT_TYPE, num, name );
	}

	public static final ScriptValue parseMonsterValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.MONSTER_INIT;
		}

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.MONSTER_INIT;
			}

			throw new AdvancedScriptException( "Bad monster name " + name );
		}

		return new ScriptValue( Interpreter.MONSTER_TYPE, monster.getName(), (Object) monster );
	}

	public static final ScriptValue parseElementValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return Interpreter.ELEMENT_INIT;
		}

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return Interpreter.ELEMENT_INIT;
			}

			throw new AdvancedScriptException( "Bad element name " + name );
		}

		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( Interpreter.ELEMENT_TYPE, num, name );
	}

	public static final ScriptValue parseValue( final ScriptType type, final String name )
	{
		return type.parseValue( name );
	}

	// For data types which map to integers, also supply:
	//
	// public static final ScriptValue makeXValue( int num )
	//     throws nothing.

	public static final ScriptValue makeItemValue( final int num )
	{
		String name = ItemDatabase.getItemName( num );

		if ( name == null )
		{
			return Interpreter.ITEM_INIT;
		}

		return new ScriptValue( Interpreter.ITEM_TYPE, num, name );
	}

	public static final ScriptValue makeItemValue( final String name )
	{
		int num = ItemDatabase.getItemId( name );

		if ( num == -1 )
		{
			return Interpreter.ITEM_INIT;
		}

		return new ScriptValue( Interpreter.ITEM_TYPE, num, name );
	}

	public static final ScriptValue makeClassValue( final String name )
	{
		return new ScriptValue( Interpreter.CLASS_TYPE, Interpreter.classToInt( name ), name );
	}

	public static final ScriptValue makeSkillValue( final int num )
	{
		String name = SkillDatabase.getSkillName( num );
		if ( name == null )
		{
			return Interpreter.SKILL_INIT;
		}

		return new ScriptValue( Interpreter.SKILL_TYPE, num, name );
	}

	public static final ScriptValue makeEffectValue( final int num )
	{
		String name = EffectDatabase.getEffectName( num );
		if ( name == null )
		{
			return Interpreter.EFFECT_INIT;
		}
		return new ScriptValue( Interpreter.EFFECT_TYPE, num, name );
	}

	public static final ScriptValue makeFamiliarValue( final int num )
	{
		String name = FamiliarDatabase.getFamiliarName( num );
		if ( name == null )
		{
			return Interpreter.FAMILIAR_INIT;
		}
		return new ScriptValue( Interpreter.FAMILIAR_TYPE, num, name );
	}

	// **************** Tracing *****************

	private static int traceIndentation = 0;

	public static final void resetTracing()
	{
		Interpreter.traceIndentation = 0;
	}

	public static final void traceIndent()
	{
		Interpreter.traceIndentation++ ;
	}

	public static final void traceUnindent()
	{
		Interpreter.traceIndentation-- ;
	}

	public static final void trace( final String string )
	{
		Interpreter.indentLine( Interpreter.traceIndentation );
		RequestLogger.updateDebugLog( string );
	}

	public static final String executionStateString( final int state )
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
			return this.validate( DataUtilities.getInputStream( scriptFile ) );
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
		Interpreter previousAnalysis = Interpreter.currentAnalysis;

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

			Interpreter.currentAnalysis = this;
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
			Interpreter.currentAnalysis = previousAnalysis;
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

			boolean shouldRefresh = !Interpreter.lastImportString.equals( importString );

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
				Interpreter.lastImportString = "";

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
			boolean wasExecuting = Interpreter.isExecuting;

			Interpreter.isExecuting = true;
			this.executeScope( this.global, functionName, parameters );
			Interpreter.isExecuting = wasExecuting;
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

		Interpreter previousAnalysis = Interpreter.currentAnalysis;
		Interpreter.currentAnalysis = new Interpreter( this, scriptFile );

		try
		{
			result =
				Interpreter.currentAnalysis.parseScope(
					this.global, null, new ScriptVariableList(), this.global.parentScope, false );
		}
		catch ( Exception e )
		{
			error = new AdvancedScriptException( e );
		}

		try
		{
			Interpreter.currentAnalysis.commandStream.close();
		}
		catch ( Exception e )
		{
			// Do nothing, because this means the stream somehow
			// got closed anyway.
		}

		if ( error == null && Interpreter.currentAnalysis.currentLine != null )
		{
			error = new AdvancedScriptException( "Script parsing error" );
		}

		Interpreter.currentAnalysis = previousAnalysis;

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
			if ( t.getType() == Interpreter.TYPE_RECORD && this.currentToken() != null && this.currentToken().equals(
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

			if ( Interpreter.isReservedWord( recordName ) )
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

			if ( Interpreter.isReservedWord( fieldName ) )
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

		if ( Interpreter.isReservedWord( functionName ) )
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
		if ( !result.assertReturn() && !functionType.equals( Interpreter.TYPE_VOID )
		// The following clause can't be correct. I think it
		// depends on the various conditional & loop constructs
		// returning a boolean. Or something. But without it,
		// existing scripts break. Aargh!
		&& !functionType.equals( Interpreter.TYPE_BOOLEAN ) )
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
		if ( Interpreter.isReservedWord( variableName ) )
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

		if ( Interpreter.isReservedWord( typeName ) )
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

			result = new ScriptFlowControl( Interpreter.COMMAND_BREAK );
			this.readToken(); //break
		}

		else if ( this.currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
			{
				throw new AdvancedScriptException( "Encountered 'continue' outside of loop" );
			}

			result = new ScriptFlowControl( Interpreter.COMMAND_CONTINUE );
			this.readToken(); //continue
		}

		else if ( this.currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptFlowControl( Interpreter.COMMAND_EXIT );
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

		if ( Interpreter.arrays && this.readIntegerToken( this.currentToken() ) )
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
			if ( expectedType != null && expectedType.equals( Interpreter.TYPE_VOID ) )
			{
				return new ScriptReturn( null, Interpreter.VOID_TYPE );
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

		if ( expression.getType() != Interpreter.BOOLEAN_TYPE )
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
					expression = Interpreter.TRUE_VALUE;
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

		if ( expression.getType() != Interpreter.BOOLEAN_TYPE )
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

		if ( expression.getType() != Interpreter.BOOLEAN_TYPE )
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

		ScriptExpression increment = Interpreter.ONE_VALUE;
		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken(); // by
			increment = this.parseExpression( parentScope );
		}

		// Create integer index variable
		ScriptVariable indexvar = new ScriptVariable( name, Interpreter.INT_TYPE );

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
			if ( lhs.getType() != Interpreter.BOOLEAN_TYPE )
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

			if ( !Interpreter.validCoercion( lhs.getType(), rhs.getType(), oper.toString() ) )
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
			result = Interpreter.TRUE_VALUE;
		}

		else if ( this.currentToken().equalsIgnoreCase( "false" ) )
		{
			this.readToken();
			result = Interpreter.FALSE_VALUE;
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
				return Interpreter.parseValue( type, resultString.toString().trim() );
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

	public static final boolean validCoercion( ScriptType lhs, ScriptType rhs, final String oper )
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
			return lhs.getType() == Interpreter.TYPE_AGGREGATE && ( (ScriptAggregateType) lhs ).getIndexType().equals(
				rhs );
		}

		// If the types are equal, no coercion is necessary
		if ( lhs.equals( rhs ) )
		{
			return true;
		}

		if ( lhs.equals( Interpreter.TYPE_ANY ) && rhs.getType() != Interpreter.TYPE_AGGREGATE )
		{
			return true;
		}

		// Anything coerces to a string
		if ( lhs.equals( Interpreter.TYPE_STRING ) )
		{
			return true;
		}

		// Anything coerces to a string for concatenation
		if ( oper.equals( "+" ) && rhs.equals( Interpreter.TYPE_STRING ) )
		{
			return true;
		}

		// Int coerces to float
		if ( lhs.equals( Interpreter.TYPE_INT ) && rhs.equals( Interpreter.TYPE_FLOAT ) )
		{
			return true;
		}

		if ( lhs.equals( Interpreter.TYPE_FLOAT ) && rhs.equals( Interpreter.TYPE_INT ) )
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
			for ( int i = 0; i < Interpreter.tokenList.length; ++i )
			{
				if ( s.charAt( 0 ) == Interpreter.tokenList[ i ] )
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			for ( int i = 0; i < Interpreter.multiCharTokenList.length; ++i )
			{
				if ( s.equalsIgnoreCase( Interpreter.multiCharTokenList[ i ] ) )
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

		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<SCOPE>" );

		Interpreter.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<TYPES>" );

		it = scope.getTypes();
		ScriptType currentType;

		while ( it.hasNext() )
		{
			currentType = (ScriptType) it.next();
			this.printType( currentType, indent + 2 );
		}

		Interpreter.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<VARIABLES>" );

		it = scope.getVariables();
		ScriptVariable currentVar;

		while ( it.hasNext() )
		{
			currentVar = (ScriptVariable) it.next();
			this.printVariable( currentVar, indent + 2 );
		}

		Interpreter.indentLine( indent + 1 );
		RequestLogger.updateDebugLog( "<FUNCTIONS>" );

		it = scope.getFunctions();
		ScriptFunction currentFunc;

		while ( it.hasNext() )
		{
			currentFunc = (ScriptFunction) it.next();
			this.printFunction( currentFunc, indent + 2 );
		}

		Interpreter.indentLine( indent + 1 );
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
			Interpreter.indentLine( indent + 1 );
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
		this.showFunctions( Interpreter.existingFunctions.iterator(), filter.toLowerCase() );
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
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<TYPE " + type + ">" );
	}

	private void printVariable( final ScriptVariable var, final int indent )
	{
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<VAR " + var.getType() + " " + var.getName() + ">" );
	}

	private void printFunction( final ScriptFunction func, final int indent )
	{
		Interpreter.indentLine( indent );
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
			Interpreter.indentLine( indent );
			RequestLogger.updateDebugLog( "<COMMAND " + command + ">" );
		}
	}

	private void printReturn( final ScriptReturn ret, final int indent )
	{
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<RETURN " + ret.getType() + ">" );
		if ( !ret.getType().equals( Interpreter.TYPE_VOID ) )
		{
			this.printExpression( ret.getExpression(), indent + 1 );
		}
	}

	private void printConditional( final ScriptConditional command, final int indent )
	{
		Interpreter.indentLine( indent );
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
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<WHILE>" );
		this.printExpression( loop.getCondition(), indent + 1 );
		this.printScope( loop.getScope(), indent + 1 );
	}

	private void printRepeat( final ScriptRepeat loop, final int indent )
	{
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<REPEAT>" );
		this.printScope( loop.getScope(), indent + 1 );
		this.printExpression( loop.getCondition(), indent + 1 );
	}

	private void printForeach( final ScriptForeach loop, final int indent )
	{
		Interpreter.indentLine( indent );
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
		Interpreter.indentLine( indent );
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
		Interpreter.indentLine( indent );
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
		Interpreter.indentLine( indent );
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

				Interpreter.indentLine( indent );
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
			Interpreter.indentLine( indent );
			RequestLogger.updateDebugLog( "<VALUE " + value.getType() + " [" + value + "]>" );
		}
	}

	public void printOperator( final ScriptOperator oper, final int indent )
	{
		Interpreter.indentLine( indent );
		RequestLogger.updateDebugLog( "<OPER " + oper + ">" );
	}

	public void printCompositeReference( final ScriptCompositeReference varRef, final int indent )
	{
		Interpreter.indentLine( indent );
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

		Interpreter.indentLine( indent );
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

	public static void captureValue( final ScriptValue value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( KoLmafia.refusesContinue() || value == null )
		{
			// User aborted
			Interpreter.currentState = Interpreter.STATE_EXIT;
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		Interpreter.currentState = Interpreter.STATE_NORMAL;
		KoLmafia.forceContinue();
	}

	private ScriptValue executeScope( final ScriptScope topScope, final String functionName, final String[] parameters )
	{
		ScriptFunction main;
		ScriptValue result = null;

		Interpreter.currentState = Interpreter.STATE_NORMAL;
		Interpreter.resetTracing();

		main =
			functionName.equals( "main" ) ? this.mainMethod : topScope.findFunction( functionName, parameters != null );

		if ( main == null && !topScope.getCommands().hasNext() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to invoke " + functionName );
			return Interpreter.VOID_VALUE;
		}

		// First execute top-level commands;

		boolean executeTopLevel = this != KoLmafiaASH.NAMESPACE_INTERPRETER;

		if ( !executeTopLevel )
		{
			String importString = Preferences.getString( "commandLineNamespace" );
			executeTopLevel = !importString.equals( Interpreter.lastImportString );
			Interpreter.lastImportString = importString;
		}

		if ( executeTopLevel )
		{
			Interpreter.trace( "Executing top-level commands" );
			result = topScope.execute();
		}

		if ( Interpreter.currentState == Interpreter.STATE_EXIT )
		{
			return result;
		}

		// Now execute main function, if any
		if ( main != null )
		{
			Interpreter.trace( "Executing main function" );

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
				if ( type == Interpreter.VOID_TYPE )
				{
					value = Interpreter.VOID_VALUE;
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
					value = Interpreter.parseValue( type, input );
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

			ScriptValue value = Interpreter.parseValue( lastType, inputs.toString().trim() );
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
			return (String) KoLFrame.input( message, Interpreter.BOOLEANS );

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
			return (String) KoLFrame.input( message, Interpreter.CLASSES );

		case TYPE_STAT:
			return (String) KoLFrame.input( message, Interpreter.STATS );

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
		if ( Interpreter.currentAnalysis == null )
		{
			return "";
		}

		return Interpreter.currentAnalysis.getLineAndFile();
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
		return new ScriptScope( Interpreter.existingFunctions, null, Interpreter.simpleTypes );
	}

	public static final ScriptFunctionList getExistingFunctions()
	{
		ScriptFunctionList result = new ScriptFunctionList();
		ScriptType[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "enable", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "disable", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "user_confirm", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "logprint", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "print_html", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "abort", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "abort", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "cli_execute", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "load_html", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "write", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "writeln", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "form_field", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "visit_url", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "visit_url", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "wait", Interpreter.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_string", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_boolean", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_int", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		result.addElement( new ScriptExistingFunction( "to_float", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", Interpreter.ITEM_TYPE, params ) );
		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_item", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_class", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_stat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );
		params = new ScriptType[] { Interpreter.EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );
		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_location", Interpreter.LOCATION_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", Interpreter.FAMILIAR_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_familiar", Interpreter.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_monster", Interpreter.MONSTER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "to_slot", Interpreter.SLOT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "to_url", Interpreter.STRING_TYPE, params ) );

		// Functions related to daily information which get
		// updated usually once per day.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "today_to_string", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_phase", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "moon_light", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_today", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "adventure", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "add_item_condition", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "buy", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "create", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "use", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "eat", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "drink", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_closet", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_shop", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "put_display", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_closet", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_storage", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_display", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "take_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "autosell", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "hermit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "retrieve_item", Interpreter.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "is_npc_item", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "daily_special", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "available_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "item_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "closet_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "creatable_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "get_ingredients", Interpreter.RESULT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "storage_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "display_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "shop_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "stash_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "pulls_remaining", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "stills_available", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_mushroom_plot", Interpreter.BOOLEAN_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "refresh_status", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_hp", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "restore_mp", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_name", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_id", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_hash", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_muscle_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_mysticality_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_moxie_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_bad_moon", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_class", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_level", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxhp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_mp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_maxmp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_primestat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_basestat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STAT_TYPE };
		result.addElement( new ScriptExistingFunction( "my_buffedstat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_meat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_adventures", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_turncount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_fullness", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "fullness_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_inebriety", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "inebriety_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_spleen_use", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "spleen_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_eat", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_drink", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "turns_played", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "can_interact", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "in_hardcore", Interpreter.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "have_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "mp_cost", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "turns_per_cast", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE };
		result.addElement( new ScriptExistingFunction( "have_effect", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.SKILL_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "attack", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "steal", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "runaway", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		result.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_item", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "throw_items", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "run_combat", Interpreter.BUFFER_TYPE, params ) );

		// Equipment functions.

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "can_equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SLOT_TYPE, Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SLOT_TYPE };
		result.addElement( new ScriptExistingFunction( "equipped_item", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "have_equipped", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "outfit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "have_outfit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_familiar", Interpreter.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "have_familiar", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "use_familiar", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_equipment", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		result.addElement( new ScriptExistingFunction( "familiar_weight", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_hands", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "weapon_type", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "ranged_weapon", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		result.addElement( new ScriptExistingFunction( "get_power", Interpreter.INT_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "council", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_mcd", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "change_mcd", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_chef", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "have_bartender", Interpreter.BOOLEAN_TYPE, params ) );

		// String parsing functions.

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "contains_text", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_meat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "extract_items", Interpreter.RESULT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "length", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "last_index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "substring", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_lower_case", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "to_upper_case", Interpreter.STRING_TYPE, params ) );

		// String buffer functions

		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "append", Interpreter.BUFFER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "insert", Interpreter.BUFFER_TYPE, params ) );
		params =
			new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "delete", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.BUFFER_TYPE };
		result.addElement( new ScriptExistingFunction( "append_tail", Interpreter.BUFFER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "append_replacement", Interpreter.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "create_matcher", Interpreter.MATCHER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "find", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "start", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "end", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "group", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "group", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "group_count", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_first", Interpreter.STRING_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_all", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		result.addElement( new ScriptExistingFunction( "reset", Interpreter.MATCHER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "reset", Interpreter.MATCHER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_string", Interpreter.STRING_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "replace_string", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "group_string", Interpreter.REGEX_GROUP_TYPE, params ) );

		// Assorted functions

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "chat_reply", Interpreter.VOID_TYPE, params ) );

		// Quest handling functions.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "entryway", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "hedgemaze", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "guardians", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "chamber", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "tavern", Interpreter.INT_TYPE, params ) );

		// Arithmetic utility functions.

		params = new ScriptType[] { Interpreter.INT_TYPE };
		result.addElement( new ScriptExistingFunction( "random", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "round", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "truncate", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "floor", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "ceil", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		result.addElement( new ScriptExistingFunction( "square_root", Interpreter.FLOAT_TYPE, params ) );

		// Settings-type functions.

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_encode", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "url_decode", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "get_property", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "set_property", Interpreter.VOID_TYPE, params ) );

		// Functions for aggregates.

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "count", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "clear", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.AGGREGATE_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.AGGREGATE_TYPE, Interpreter.BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "file_to_map", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE, Interpreter.STRING_TYPE, Interpreter.BOOLEAN_TYPE };
		result.addElement( new ScriptExistingFunction( "map_to_file", Interpreter.BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "my_location", Interpreter.LOCATION_TYPE, params ) );

		params = new ScriptType[] { Interpreter.LOCATION_TYPE };
		result.addElement( new ScriptExistingFunction( "get_monsters", new ScriptAggregateType(
			Interpreter.MONSTER_TYPE, 0 ), params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "expected_damage", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "expected_damage", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_level_adjustment", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "weight_adjustment", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "mana_cost_modifier", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "raw_damage_absorption", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_absorption_percent", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "damage_reduction", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ELEMENT_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "combat_rate_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "initiative_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "experience_bonus", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "meat_drop_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drop_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "buffed_hit_stat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "current_hit_stat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_element", Interpreter.ELEMENT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_element", Interpreter.ELEMENT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_attack", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_attack", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_defense", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_defense", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "monster_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "monster_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "item_drops", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		result.addElement( new ScriptExistingFunction( "item_drops", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_miss", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addElement( new ScriptExistingFunction( "will_usually_dodge", Interpreter.BOOLEAN_TYPE, params ) );

		// Modifier introspection

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "boolean_modifier", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "boolean_modifier", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "effect_modifier", Interpreter.EFFECT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "class_modifier", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE, Interpreter.STRING_TYPE };
		result.addElement( new ScriptExistingFunction( "stat_modifier", Interpreter.STAT_TYPE, params ) );

		return result;
	}

	public static final ScriptTypeList getSimpleTypes()
	{
		ScriptTypeList result = new ScriptTypeList();
		result.addElement( Interpreter.VOID_TYPE );
		result.addElement( Interpreter.BOOLEAN_TYPE );
		result.addElement( Interpreter.INT_TYPE );
		result.addElement( Interpreter.FLOAT_TYPE );
		result.addElement( Interpreter.STRING_TYPE );
		result.addElement( Interpreter.BUFFER_TYPE );
		result.addElement( Interpreter.MATCHER_TYPE );

		result.addElement( Interpreter.ITEM_TYPE );
		result.addElement( Interpreter.LOCATION_TYPE );
		result.addElement( Interpreter.CLASS_TYPE );
		result.addElement( Interpreter.STAT_TYPE );
		result.addElement( Interpreter.SKILL_TYPE );
		result.addElement( Interpreter.EFFECT_TYPE );
		result.addElement( Interpreter.FAMILIAR_TYPE );
		result.addElement( Interpreter.SLOT_TYPE );
		result.addElement( Interpreter.MONSTER_TYPE );
		result.addElement( Interpreter.ELEMENT_TYPE );
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
		return Interpreter.reservedWords.findSymbol( name ) != null;
	}

	public static class AdvancedScriptException
		extends RuntimeException
	{
		AdvancedScriptException( final Throwable t )
		{
			this( t.getMessage() == null ? "" : t.getMessage() + " " + Interpreter.getCurrentLineAndFile() );
		}

		AdvancedScriptException( final String s )
		{
			super( s == null ? "" : s + " " + Interpreter.getCurrentLineAndFile() );
		}
	}
}
