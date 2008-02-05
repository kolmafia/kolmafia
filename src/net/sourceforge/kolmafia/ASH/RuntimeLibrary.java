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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
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
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ASH.Interpreter;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptAggregateType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptArray;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptCompositeValue;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptExistingFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFunctionList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptMap;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptValue;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariable;
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
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

public abstract class RuntimeLibrary
{
	private static StringBuffer concatenateBuffer = new StringBuffer();
	private static final GenericRequest VISITOR = new GenericRequest( "" );
	private static final RelayRequest RELAYER = new RelayRequest( false );

	public static final ScriptFunctionList functions = new ScriptFunctionList();

	static
	{
		ScriptType[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "enable", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "disable", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "user_confirm", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "logprint", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print_html", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "abort", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "abort", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "cli_execute", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "load_html", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "write", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "writeln", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "form_field", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "visit_url", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "visit_url", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "wait", Interpreter.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_string", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_boolean", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_int", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_float", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_item", Interpreter.ITEM_TYPE, params ) );
		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_item", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_class", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_stat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );
		params = new ScriptType[] { Interpreter.EFFECT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", Interpreter.SKILL_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );
		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", Interpreter.EFFECT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_location", Interpreter.LOCATION_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_familiar", Interpreter.FAMILIAR_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_familiar", Interpreter.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_monster", Interpreter.MONSTER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_slot", Interpreter.SLOT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_url", Interpreter.STRING_TYPE, params ) );

		// Functions related to daily information which get
		// updated usually once per day.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "today_to_string", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "moon_phase", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "moon_light", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stat_bonus_today", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "adventure", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "add_item_condition", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "buy", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "create", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "use", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "eat", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "drink", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_closet", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_shop", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_display", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_closet", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_storage", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_display", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "autosell", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "hermit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "retrieve_item", Interpreter.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "is_npc_item", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "daily_special", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "refresh_stash", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "available_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "item_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "closet_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "creatable_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_ingredients", Interpreter.RESULT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "storage_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "display_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "shop_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "stash_amount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "pulls_remaining", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stills_available", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_mushroom_plot", Interpreter.BOOLEAN_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "refresh_status", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "restore_hp", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "restore_mp", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_name", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_id", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_hash", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_muscle_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_mysticality_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_moxie_sign", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_bad_moon", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_class", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_level", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_maxhp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_mp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_maxmp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_primestat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "my_basestat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "my_buffedstat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_meat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_adventures", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_turncount", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_fullness", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "fullness_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_inebriety", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "inebriety_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_spleen_use", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "spleen_limit", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_eat", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_drink", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "turns_played", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_interact", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_hardcore", Interpreter.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "mp_cost", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "turns_per_cast", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_effect", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE, Interpreter.SKILL_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "attack", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "steal", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "runaway", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "throw_item", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "throw_items", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "run_combat", Interpreter.BUFFER_TYPE, params ) );

		// Equipment functions.

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "can_equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SLOT_TYPE, Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "equip", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SLOT_TYPE };
		functions.addElement( new ScriptExistingFunction( "equipped_item", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_equipped", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "outfit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_outfit", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_familiar", Interpreter.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_familiar", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_familiar", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "familiar_equipment", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "familiar_weight", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "weapon_hands", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "weapon_type", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "ranged_weapon", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_power", Interpreter.INT_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "council", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "current_mcd", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "change_mcd", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_chef", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_bartender", Interpreter.BOOLEAN_TYPE, params ) );

		// String parsing functions.

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "contains_text", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "extract_meat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "extract_items", Interpreter.RESULT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "length", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "last_index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "last_index_of", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "substring", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "substring", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_lower_case", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_upper_case", Interpreter.STRING_TYPE, params ) );

		// String buffer functions

		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "append", Interpreter.BUFFER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "insert", Interpreter.BUFFER_TYPE, params ) );
		params =
			new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.INT_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "delete", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.BUFFER_TYPE };
		functions.addElement( new ScriptExistingFunction( "append_tail", Interpreter.BUFFER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "append_replacement", Interpreter.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "create_matcher", Interpreter.MATCHER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "find", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "start", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "end", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "group", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "group", Interpreter.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "group_count", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_first", Interpreter.STRING_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_all", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "reset", Interpreter.MATCHER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.MATCHER_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "reset", Interpreter.MATCHER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.BUFFER_TYPE, Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_string", Interpreter.BUFFER_TYPE, params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_string", Interpreter.BUFFER_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			Interpreter.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "group_string", Interpreter.REGEX_GROUP_TYPE, params ) );

		// Assorted functions

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "chat_reply", Interpreter.VOID_TYPE, params ) );

		// Quest handling functions.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "entryway", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "hedgemaze", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "guardians", Interpreter.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "chamber", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "tavern", Interpreter.INT_TYPE, params ) );

		// Arithmetic utility functions.

		params = new ScriptType[] { Interpreter.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "random", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "round", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "truncate", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "floor", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "ceil", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "square_root", Interpreter.FLOAT_TYPE, params ) );

		// Settings-type functions.

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "url_encode", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "url_decode", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_property", Interpreter.STRING_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "set_property", Interpreter.VOID_TYPE, params ) );

		// Functions for aggregates.

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "count", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "clear", Interpreter.VOID_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "file_to_map", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE, Interpreter.AGGREGATE_TYPE, Interpreter.BOOLEAN_TYPE };
		functions.addElement( new ScriptExistingFunction( "file_to_map", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "map_to_file", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.AGGREGATE_TYPE, Interpreter.STRING_TYPE, Interpreter.BOOLEAN_TYPE };
		functions.addElement( new ScriptExistingFunction( "map_to_file", Interpreter.BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_location", Interpreter.LOCATION_TYPE, params ) );

		params = new ScriptType[] { Interpreter.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_monsters", new ScriptAggregateType(
			Interpreter.MONSTER_TYPE, 0 ), params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "expected_damage", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "expected_damage", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_level_adjustment", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "weight_adjustment", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "mana_cost_modifier", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "raw_damage_absorption", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "damage_absorption_percent", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "damage_reduction", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ELEMENT_TYPE };
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "combat_rate_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "initiative_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "experience_bonus", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "meat_drop_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "item_drop_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "buffed_hit_stat", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "current_hit_stat", Interpreter.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_element", Interpreter.ELEMENT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_element", Interpreter.ELEMENT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_attack", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_attack", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_defense", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_defense", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_hp", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "item_drops", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "item_drops", Interpreter.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "will_usually_miss", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "will_usually_dodge", Interpreter.BOOLEAN_TYPE, params ) );

		// Modifier introspection

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.SKILL_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", Interpreter.FLOAT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "boolean_modifier", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "boolean_modifier", Interpreter.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "effect_modifier", Interpreter.EFFECT_TYPE, params ) );

		params = new ScriptType[] { Interpreter.ITEM_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "class_modifier", Interpreter.CLASS_TYPE, params ) );

		params = new ScriptType[] { Interpreter.EFFECT_TYPE, Interpreter.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "stat_modifier", Interpreter.STAT_TYPE, params ) );
	}

        public static Method findMethod( final String name, final Class[] args )
		throws NoSuchMethodException
	{
                return RuntimeLibrary.class.getMethod( name, args );
        }

	private static ScriptValue continueValue()
	{
		return KoLmafia.permitsContinue() && !KoLmafia.hadPendingState() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

        // Basic utility functions which print information
        // or allow for easy testing.

        public static ScriptValue enable( final ScriptVariable name )
 	{
                StaticEntity.enable( name.toStringValue().toString().toLowerCase() );
                return Interpreter.VOID_VALUE;
        }

	public static ScriptValue disable( final ScriptVariable name )
	{
		StaticEntity.disable( name.toStringValue().toString().toLowerCase() );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue user_confirm( final ScriptVariable message )
	{
		return KoLFrame.confirm( message.toStringValue().toString() ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue logprint( final ScriptVariable string )
	{
		String parameters = string.toStringValue().toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.getSessionStream().println( "> " + parameters );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue print( final ScriptVariable string )
	{
		String parameters = string.toStringValue().toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.printLine( parameters );
		RequestLogger.getSessionStream().println( "> " + parameters );

		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue print( final ScriptVariable string, final ScriptVariable color )
	{
		String parameters = string.toStringValue().toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		String colorString = color.toStringValue().toString();
		colorString = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( colorString, "\"" ), "<" );

		RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );
		RequestLogger.getSessionStream().println( " > " + parameters );

		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue print_html( final ScriptVariable string )
	{
		RequestLogger.printLine( string.toStringValue().toString() );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue abort()
	{
		RequestThread.declareWorldPeace();
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue abort( final ScriptVariable string )
	{
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, string.toStringValue().toString() );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue cli_execute( final ScriptVariable string )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
		return RuntimeLibrary.continueValue();
	}

	private static File getFile( String filename )
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
			return RuntimeLibrary.getFile( filename.substring( 0, filename.length() - 4 ) + ".txt" );
		}

		return new File( KoLConstants.SCRIPT_LOCATION, filename );
	}

	private static BufferedReader getReader( final String filename )
	{
		if ( filename.startsWith( "http" ) )
		{
			return DataUtilities.getReader( filename );
		}

		File input = RuntimeLibrary.getFile( filename );
		if ( input.exists() )
		{
			return DataUtilities.getReader( input );
		}

		BufferedReader reader = DataUtilities.getReader( "data", filename );
		return reader != null ? reader : DataUtilities.getReader( filename );
	}

	public static ScriptValue load_html( final ScriptVariable string )
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

		String location = string.toStringValue().toString();
		if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
		{
			return returnValue;
		}

		File input = RuntimeLibrary.getFile( location );
		if ( input == null || !input.exists() )
		{
			return returnValue;
		}

		RuntimeLibrary.VISITOR.loadResponseFromFile( input );
		if ( RuntimeLibrary.VISITOR.responseText != null )
		{
			buffer.append( RuntimeLibrary.VISITOR.responseText );
		}

		return returnValue;
	}

	public static ScriptValue write( final ScriptVariable string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return Interpreter.VOID_VALUE;
		}

		KoLmafiaASH.serverReplyBuffer.append( string.toStringValue().toString() );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue writeln( final ScriptVariable string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return Interpreter.VOID_VALUE;
		}

		RuntimeLibrary.write( string );
		KoLmafiaASH.serverReplyBuffer.append( KoLConstants.LINE_BREAK );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue form_field( final ScriptVariable key )
	{
		if ( KoLmafiaASH.relayRequest == null )
		{
			return Interpreter.STRING_INIT;
		}

		String value = KoLmafiaASH.relayRequest.getFormField( key.toStringValue().toString() );
		return value == null ? Interpreter.STRING_INIT : new ScriptValue( value );
	}

	public static ScriptValue visit_url()
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

		RequestThread.postRequest( KoLmafiaASH.relayRequest );
		if ( KoLmafiaASH.relayRequest.responseText != null )
		{
			buffer.append( KoLmafiaASH.relayRequest.responseText );
		}

		return returnValue;
	}

	public static ScriptValue visit_url( final ScriptVariable string )
	{
		return RuntimeLibrary.visit_url( string.toStringValue().toString() );
	}

	public static ScriptValue visit_url( final String location )
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

		RuntimeLibrary.VISITOR.constructURLString( location );
		if ( GenericRequest.shouldIgnore( RuntimeLibrary.VISITOR ) )
		{
			return returnValue;
		}

		if ( KoLmafiaASH.relayScript == null )
		{
			if ( RuntimeLibrary.VISITOR.getPath().equals( "fight.php" ) )
			{
				if ( FightRequest.getCurrentRound() == 0 )
				{
					return returnValue;
				}
			}

			RequestThread.postRequest( RuntimeLibrary.VISITOR );
			if ( RuntimeLibrary.VISITOR.responseText != null )
			{
				buffer.append( RuntimeLibrary.VISITOR.responseText );
				StaticEntity.externalUpdate( location, RuntimeLibrary.VISITOR.responseText );
			}
		}
		else
		{
			RequestThread.postRequest( RuntimeLibrary.RELAYER.constructURLString( location ) );
			if ( RuntimeLibrary.RELAYER.responseText != null )
			{
				buffer.append( RuntimeLibrary.RELAYER.responseText );
			}
		}

		return returnValue;
	}

	public static ScriptValue wait( final ScriptVariable delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
		return Interpreter.VOID_VALUE;
	}

	// Type conversion functions which allow conversion
	// of one data format to another.

	public static ScriptValue to_string( final ScriptVariable val )
	{
		return val.toStringValue();
	}

	public static ScriptValue to_boolean( final ScriptVariable val )
	{
		return val.toStringValue().toString().equals( "true" ) || val.intValue() != 0 ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue to_int( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_STRING ) ? Interpreter.parseIntValue( val.toStringValue().toString() ) : new ScriptValue(
			val.intValue() );
	}

	public static ScriptValue to_float( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_STRING ) ? Interpreter.parseFloatValue( val.toStringValue().toString() ) : val.intValue() != 0 ? new ScriptValue(
			(float) val.intValue() ) : new ScriptValue( val.floatValue() );
	}

	public static ScriptValue to_item( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeItemValue( val.intValue() ) : Interpreter.parseItemValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_class( final ScriptVariable val )
	{
		return Interpreter.parseClassValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_stat( final ScriptVariable val )
	{
		return Interpreter.parseStatValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_skill( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeSkillValue( val.intValue() ) : val.getValueType().equals(
			Interpreter.TYPE_EFFECT ) ? Interpreter.parseSkillValue( UneffectRequest.effectToSkill( val.toStringValue().toString() ) ) : Interpreter.parseSkillValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_effect( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeEffectValue( val.intValue() ) : val.getValueType().equals(
			Interpreter.TYPE_SKILL ) ? Interpreter.parseEffectValue( UneffectRequest.skillToEffect( val.toStringValue().toString() ) ) : Interpreter.parseEffectValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_location( final ScriptVariable val )
	{
		return Interpreter.parseLocationValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_familiar( final ScriptVariable val )
	{
		return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeFamiliarValue( val.intValue() ) : Interpreter.parseFamiliarValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_monster( final ScriptVariable val )
	{
		return Interpreter.parseMonsterValue( val.toStringValue().toString() );
	}

	public static ScriptValue to_slot( final ScriptVariable item )
	{
		switch ( ItemDatabase.getConsumptionType( item.intValue() ) )
		{
		case KoLConstants.EQUIP_HAT:
			return Interpreter.parseSlotValue( "hat" );
		case KoLConstants.EQUIP_WEAPON:
			return Interpreter.parseSlotValue( "weapon" );
		case KoLConstants.EQUIP_OFFHAND:
			return Interpreter.parseSlotValue( "off-hand" );
		case KoLConstants.EQUIP_SHIRT:
			return Interpreter.parseSlotValue( "shirt" );
		case KoLConstants.EQUIP_PANTS:
			return Interpreter.parseSlotValue( "pants" );
		case KoLConstants.EQUIP_FAMILIAR:
			return Interpreter.parseSlotValue( "familiar" );
		case KoLConstants.EQUIP_ACCESSORY:
			return Interpreter.parseSlotValue( "acc1" );
		default:
			return Interpreter.parseSlotValue( "none" );
		}
	}

	public static ScriptValue to_url( final ScriptVariable val )
	{
		KoLAdventure adventure = (KoLAdventure) val.rawValue();
		return new ScriptValue( adventure.getRequest().getURLString() );
	}

	// Functions related to daily information which get
	// updated usually once per day.

	public static ScriptValue today_to_string()
	{
		return Interpreter.parseStringValue( KoLConstants.DAILY_FORMAT.format( new Date() ) );
	}

	public static ScriptValue moon_phase()
	{
		return new ScriptValue( HolidayDatabase.getPhaseStep() );
	}

	public static ScriptValue moon_light()
	{
		return new ScriptValue( HolidayDatabase.getMoonlight() );
	}

	public static ScriptValue stat_bonus_today()
	{
		return KoLmafiaCLI.testConditional( "today is muscle day" ) ? Interpreter.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "today is myst day" ) ? Interpreter.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "today is moxie day" ) ? Interpreter.parseStatValue( "moxie" ) : Interpreter.STAT_INIT;
	}

	public static ScriptValue stat_bonus_tomorrow()
	{
		return KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) ? Interpreter.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "tomorrow is myst day" ) ? Interpreter.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) ? Interpreter.parseStatValue( "moxie" ) : Interpreter.STAT_INIT;
	}

	public static ScriptValue session_logs( final ScriptVariable dayCount )
	{
		return RuntimeLibrary.getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue() );
	}

	public static ScriptValue session_logs( final ScriptVariable player, final ScriptVariable dayCount )
	{
		return RuntimeLibrary.getSessionLogs( player.toStringValue().toString(), dayCount.intValue() );
	}

	public static ScriptValue getSessionLogs( final String name, final int dayCount )
	{
		String[] files = new String[ dayCount ];

		Calendar timestamp = Calendar.getInstance();

		ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, files.length );
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

			value.aset( new ScriptValue( i ), Interpreter.parseStringValue( contents.toString() ) );
		}

		return value;
	}

	// Major functions related to adventuring and
	// item management.

	public static ScriptValue adventure( final ScriptVariable count, final ScriptVariable loc )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue add_item_condition( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return Interpreter.VOID_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue buy( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		AdventureResult itemToBuy = new AdventureResult( item.intValue(), 1 );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
		return initialAmount + count.intValue() == itemToBuy.getCount( KoLConstants.inventory ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue create( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "create " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue use( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue eat( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "eat " + count.intValue() + " " + item.toStringValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue drink( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "drink " + count.intValue() + " " + item.toStringValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue put_closet( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_shop( final ScriptVariable price, final ScriptVariable limit, final ScriptVariable item )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " @ " + price.intValue() + " limit " + limit.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_stash( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash put " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_display( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display put " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_closet( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet take " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_storage( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_display( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_stash( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash take " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue autosell( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue hermit( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue retrieve_item( final ScriptVariable count, final ScriptVariable item )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
		return RuntimeLibrary.continueValue();
	}

	// Major functions which provide item-related
	// information.

	public static ScriptValue is_npc_item( final ScriptVariable item )
	{
		return NPCStoreDatabase.contains( ItemDatabase.getItemName( item.intValue() ), false ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue daily_special()
	{
		AdventureResult special =
			KoLCharacter.inMoxieSign() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.inMysticalitySign() ? ChezSnooteeRequest.getDailySpecial() : null;

		return special == null ? Interpreter.ITEM_INIT : Interpreter.parseItemValue( special.getName() );
	}

	public static ScriptValue refresh_stash()
	{
		RequestThread.postRequest( new ClanStashRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue available_amount( final ScriptVariable arg )
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

	public static ScriptValue item_amount( final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new ScriptValue( item.getCount( KoLConstants.inventory ) );
	}

	public static ScriptValue closet_amount( final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new ScriptValue( item.getCount( KoLConstants.closet ) );
	}

	public static ScriptValue creatable_amount( final ScriptVariable arg )
	{
		CreateItemRequest item = CreateItemRequest.getInstance( arg.intValue() );
		return new ScriptValue( item == null ? 0 : item.getQuantityPossible() );
	}

	public static ScriptValue get_ingredients( final ScriptVariable item )
	{
		AdventureResult[] data = ConcoctionDatabase.getIngredients( item.intValue() );
		ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );

		for ( int i = 0; i < data.length; ++i )
		{
			value.aset(
				Interpreter.parseItemValue( data[ i ].getName() ),
				Interpreter.parseIntValue( String.valueOf( data[ i ].getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue storage_amount( final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new ScriptValue( item.getCount( KoLConstants.storage ) );
	}

	public static ScriptValue display_amount( final ScriptVariable arg )
	{
		if ( KoLConstants.collection.isEmpty() )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}

		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new ScriptValue( item.getCount( KoLConstants.collection ) );
	}

	public static ScriptValue shop_amount( final ScriptVariable arg )
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

	public static ScriptValue stash_amount( final ScriptVariable arg )
	{
		List stash = ClanManager.getStash();
		if ( stash.isEmpty() )
		{
			RequestThread.postRequest( new ClanStashRequest() );
		}

		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new ScriptValue( item.getCount( stash ) );
	}

	public static ScriptValue pulls_remaining()
	{
		return new ScriptValue( ItemManageFrame.getPullsRemaining() );
	}

	public static ScriptValue stills_available()
	{
		return new ScriptValue( KoLCharacter.getStillsAvailable() );
	}

	public static ScriptValue have_mushroom_plot()
	{
		return new ScriptValue( MushroomManager.ownsPlot() );
	}

	// The following functions pertain to providing updated
	// information relating to the player.

	public static ScriptValue refresh_status()
	{
		RequestThread.postRequest( CharPaneRequest.getInstance() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue restore_hp( final ScriptVariable amount )
	{
		return new ScriptValue( StaticEntity.getClient().recoverHP( amount.intValue() ) );
	}

	public static ScriptValue restore_mp( final ScriptVariable amount )
	{
		int desiredMP = amount.intValue();
		while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
		{
			StaticEntity.getClient().recoverMP( desiredMP );
		}
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue my_name()
	{
		return new ScriptValue( KoLCharacter.getUserName() );
	}

	public static ScriptValue my_id()
	{
		return new ScriptValue( KoLCharacter.getPlayerId() );
	}

	public static ScriptValue my_hash()
	{
		return new ScriptValue( GenericRequest.passwordHash );
	}

	public static ScriptValue in_muscle_sign()
	{
		return new ScriptValue( KoLCharacter.inMuscleSign() );
	}

	public static ScriptValue in_mysticality_sign()
	{
		return new ScriptValue( KoLCharacter.inMysticalitySign() );
	}

	public static ScriptValue in_moxie_sign()
	{
		return new ScriptValue( KoLCharacter.inMoxieSign() );
	}

	public static ScriptValue in_bad_moon()
	{
		return new ScriptValue( KoLCharacter.inBadMoon() );
	}

	public static ScriptValue my_class()
	{
		return Interpreter.makeClassValue( KoLCharacter.getClassType() );
	}

	public static ScriptValue my_level()
	{
		return new ScriptValue( KoLCharacter.getLevel() );
	}

	public static ScriptValue my_hp()
	{
		return new ScriptValue( KoLCharacter.getCurrentHP() );
	}

	public static ScriptValue my_maxhp()
	{
		return new ScriptValue( KoLCharacter.getMaximumHP() );
	}

	public static ScriptValue my_mp()
	{
		return new ScriptValue( KoLCharacter.getCurrentMP() );
	}

	public static ScriptValue my_maxmp()
	{
		return new ScriptValue( KoLCharacter.getMaximumMP() );
	}

	public static ScriptValue my_primestat()
	{
		int primeIndex = KoLCharacter.getPrimeIndex();
		return primeIndex == 0 ? Interpreter.parseStatValue( "muscle" ) : primeIndex == 1 ? Interpreter.parseStatValue( "mysticality" ) : Interpreter.parseStatValue( "moxie" );
	}

	public static ScriptValue my_basestat( final ScriptVariable arg )
	{
		int stat = arg.intValue();

		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
		{
			return new ScriptValue( KoLCharacter.getBaseMuscle() );
		}
		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
		{
			return new ScriptValue( KoLCharacter.getBaseMysticality() );
		}
		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
		{
			return new ScriptValue( KoLCharacter.getBaseMoxie() );
		}

		throw new RuntimeException( "Internal error: unknown stat" );
	}

	public static ScriptValue my_buffedstat( final ScriptVariable arg )
	{
		int stat = arg.intValue();

		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
		}
		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
		}
		if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMoxie() );
		}

		throw new RuntimeException( "Internal error: unknown stat" );
	}

	public static ScriptValue my_meat()
	{
		return new ScriptValue( KoLCharacter.getAvailableMeat() );
	}

	public static ScriptValue my_adventures()
	{
		return new ScriptValue( KoLCharacter.getAdventuresLeft() );
	}

	public static ScriptValue my_turncount()
	{
		return new ScriptValue( KoLCharacter.getCurrentRun() );
	}

	public static ScriptValue my_fullness()
	{
		return new ScriptValue( KoLCharacter.getFullness() );
	}

	public static ScriptValue fullness_limit()
	{
		return new ScriptValue( KoLCharacter.getFullnessLimit() );
	}

	public static ScriptValue my_inebriety()
	{
		return new ScriptValue( KoLCharacter.getInebriety() );
	}

	public static ScriptValue inebriety_limit()
	{
		return new ScriptValue( KoLCharacter.getInebrietyLimit() );
	}

	public static ScriptValue my_spleen_use()
	{
		return new ScriptValue( KoLCharacter.getSpleenUse() );
	}

	public static ScriptValue spleen_limit()
	{
		return new ScriptValue( KoLCharacter.getSpleenLimit() );
	}

	public static ScriptValue can_eat()
	{
		return new ScriptValue( KoLCharacter.canEat() );
	}

	public static ScriptValue can_drink()
	{
		return new ScriptValue( KoLCharacter.canDrink() );
	}

	public static ScriptValue turns_played()
	{
		return new ScriptValue( KoLCharacter.getCurrentRun() );
	}

	public static ScriptValue can_interact()
	{
		return new ScriptValue( KoLCharacter.canInteract() );
	}

	public static ScriptValue in_hardcore()
	{
		return new ScriptValue( KoLCharacter.isHardcore() );
	}

	// Basic skill and effect functions, including those used
	// in custom combat consult scripts.

	public static ScriptValue have_skill( final ScriptVariable arg )
	{
		return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
	}

	public static ScriptValue mp_cost( final ScriptVariable skill )
	{
		return new ScriptValue( SkillDatabase.getMPConsumptionById( skill.intValue() ) );
	}

	public static ScriptValue turns_per_cast( final ScriptVariable skill )
	{
		return new ScriptValue( SkillDatabase.getEffectDuration( skill.intValue() ) );
	}

	public static ScriptValue have_effect( final ScriptVariable arg )
	{
		List potentialEffects = EffectDatabase.getMatchingNames( arg.toStringValue().toString() );
		AdventureResult effect =
			potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
		return new ScriptValue( effect == null ? 0 : effect.getCount( KoLConstants.activeEffects ) );
	}

	public static ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
			{
				RuntimeLibrary.use_skill( skill );
			}

			return Interpreter.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
		return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
	}

	public static ScriptValue use_skill( final ScriptVariable skill )
	{
		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			return RuntimeLibrary.visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast 1 " + skill.toStringValue() );
		return new ScriptValue( UseSkillRequest.lastUpdate );
	}

	public static ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill,
		final ScriptVariable target )
	{
		if ( count.intValue() <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
			{
				RuntimeLibrary.use_skill( skill );
			}

			return Interpreter.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() + " on " + target.toStringValue() );
		return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
	}

	public static ScriptValue attack()
	{
		return RuntimeLibrary.visit_url( "fight.php?action=attack" );
	}

	public static ScriptValue steal()
	{
		if ( !FightRequest.wonInitiative() )
		{
			return RuntimeLibrary.attack();
		}

		return RuntimeLibrary.visit_url( "fight.php?action=steal" );
	}

	public static ScriptValue runaway()
	{
		return RuntimeLibrary.visit_url( "fight.php?action=runaway" );
	}

	public static ScriptValue throw_item( final ScriptVariable item )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item.intValue() );
	}

	public static ScriptValue throw_items( final ScriptVariable item1, final ScriptVariable item2 )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue() + "&whichitem2=" + item2.intValue() );
	}

	public static ScriptValue run_combat()
	{
		RequestThread.postRequest( FightRequest.INSTANCE );
		String response =
			KoLmafiaASH.relayScript == null ? FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

		return new ScriptValue( Interpreter.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	// Equipment functions.

	public static ScriptValue can_equip( final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.canEquip( ItemDatabase.getItemName( item.intValue() ) ) );
	}

	public static ScriptValue equip( final ScriptVariable item )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue equip( final ScriptVariable slot, final ScriptVariable item )
	{
		if ( item.getValue().equals( Interpreter.ITEM_INIT ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "unequip " + slot.toStringValue() );
		}
		else
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );
		}

		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue equipped_item( final ScriptVariable slot )
	{
		return Interpreter.makeItemValue( KoLCharacter.getEquipment( slot.intValue() ).getName() );
	}

	public static ScriptValue have_equipped( final ScriptVariable item )
	{
		return KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue outfit( final ScriptVariable outfit )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue().toString() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue have_outfit( final ScriptVariable outfit )
	{
		SpecialOutfit so = KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue().toString() );

		if ( so == null )
		{
			return Interpreter.FALSE_VALUE;
		}

		return EquipmentDatabase.hasOutfit( so.getOutfitId() ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue weapon_hands( final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.getHands( item.intValue() ) );
	}

	public static ScriptValue weapon_type( final ScriptVariable item )
	{
		String type = EquipmentDatabase.getType( item.intValue() );
		return new ScriptValue( type == null ? "unknown" : type );
	}

	public static ScriptValue ranged_weapon( final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.isRanged( item.intValue() ) );
	}

	public static ScriptValue get_power( final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.getPower( item.intValue() ) );
	}

	public static ScriptValue my_familiar()
	{
		return Interpreter.makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
	}

	public static ScriptValue have_familiar( final ScriptVariable familiar )
	{
		return new ScriptValue( KoLCharacter.findFamiliar( familiar.toStringValue().toString() ) != null );
	}

	public static ScriptValue use_familiar( final ScriptVariable familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue familiar_equipment( final ScriptVariable familiar )
	{
		return Interpreter.parseItemValue( FamiliarDatabase.getFamiliarItem( familiar.intValue() ) );
	}

	public static ScriptValue familiar_weight( final ScriptVariable familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue().toString() );
		return new ScriptValue( fam == null ? 0 : fam.getWeight() );
	}

	// Random other functions related to current in-game
	// state, not directly tied to the character.

	public static ScriptValue council()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "council" );
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue current_mcd()
	{
		return new ScriptValue( KoLCharacter.getSignedMLAdjustment() );
	}

	public static ScriptValue change_mcd( final ScriptVariable level )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mind-control " + level.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue have_chef()
	{
		return new ScriptValue( KoLCharacter.hasChef() );
	}

	public static ScriptValue have_bartender()
	{
		return new ScriptValue( KoLCharacter.hasBartender() );
	}

	// String parsing functions.

	public static ScriptValue contains_text( final ScriptVariable source, final ScriptVariable search )
	{
		return new ScriptValue(
			source.toStringValue().toString().indexOf( search.toStringValue().toString() ) != -1 );
	}

	public static ScriptValue extract_meat( final ScriptVariable string )
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

	public static ScriptValue extract_items( final ScriptVariable string )
	{
		ArrayList data = new ArrayList();
		StaticEntity.getClient().processResults( string.toStringValue().toString(), data );
		ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );

		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			if ( result.isItem() )
			{
				value.aset(
					Interpreter.parseItemValue( result.getName() ),
					Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
			}
		}

		return value;
	}

	public static ScriptValue length( final ScriptVariable string )
	{
		return new ScriptValue( string.toStringValue().toString().length() );
	}

	public static ScriptValue index_of( final ScriptVariable source, final ScriptVariable search )
	{
		String string = source.toStringValue().toString();
		String substring = search.toStringValue().toString();
		return new ScriptValue( string.indexOf( substring ) );
	}

	public static ScriptValue index_of( final ScriptVariable source, final ScriptVariable search,
		final ScriptVariable start )
	{
		String string = source.toStringValue().toString();
		String substring = search.toStringValue().toString();
		int begin = start.intValue();
		return new ScriptValue( string.indexOf( substring, begin ) );
	}

	public static ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search )
	{
		String string = source.toStringValue().toString();
		String substring = search.toStringValue().toString();
		return new ScriptValue( string.lastIndexOf( substring ) );
	}

	public static ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search,
		final ScriptVariable start )
	{
		String string = source.toStringValue().toString();
		String substring = search.toStringValue().toString();
		int begin = start.intValue();
		return new ScriptValue( string.lastIndexOf( substring, begin ) );
	}

	public static ScriptValue substring( final ScriptVariable source, final ScriptVariable start )
	{
		String string = source.toStringValue().toString();
		int begin = start.intValue();
		return new ScriptValue( string.substring( begin ) );
	}

	public static ScriptValue substring( final ScriptVariable source, final ScriptVariable start,
		final ScriptVariable finish )
	{
		String string = source.toStringValue().toString();
		int begin = start.intValue();
		int end = finish.intValue();
		return new ScriptValue( string.substring( begin, end ) );
	}

	public static ScriptValue to_upper_case( final ScriptVariable string )
	{
		return Interpreter.parseStringValue( string.toStringValue().toString().toUpperCase() );
	}

	public static ScriptValue to_lower_case( final ScriptVariable string )
	{
		return Interpreter.parseStringValue( string.toStringValue().toString().toLowerCase() );
	}

	public static ScriptValue append( final ScriptVariable buffer, final ScriptVariable s )
	{
		StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
		current.append( s.toStringValue().toString() );
		return buffer.getValue();
	}

	public static ScriptValue insert( final ScriptVariable buffer, final ScriptVariable index, final ScriptVariable s )
	{
		StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
		current.insert( index.intValue(), s.toStringValue().toString() );
		return buffer.getValue();
	}

	public static ScriptValue replace( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end,
		final ScriptVariable s )
	{
		StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
		current.replace( start.intValue(), end.intValue(), s.toStringValue().toString() );
		return buffer.getValue();
	}

	public static ScriptValue delete( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end )
	{
		StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
		current.delete( start.intValue(), end.intValue() );
		return buffer.getValue();
	}

	public static ScriptValue append_tail( final ScriptVariable matcher, final ScriptVariable current )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
		m.appendTail( buffer );
		return current.getValue();
	}

	public static ScriptValue append_replacement( final ScriptVariable matcher, final ScriptVariable current,
		final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
		m.appendReplacement( buffer, replacement.toStringValue().toString() );
		return matcher.getValue();
	}

	public static ScriptValue create_matcher( final ScriptVariable pattern, final ScriptVariable string )
	{
		return new ScriptValue( Interpreter.MATCHER_TYPE, pattern.toStringValue().toString(), Pattern.compile(
			pattern.toStringValue().toString(), Pattern.DOTALL ).matcher( string.toStringValue().toString() ) );
	}

	public static ScriptValue find( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return m.find() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue start( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.start() );
	}

	public static ScriptValue end( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.end() );
	}

	public static ScriptValue group( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.group() );
	}

	public static ScriptValue group( final ScriptVariable matcher, final ScriptVariable group )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.group( group.intValue() ) );
	}

	public static ScriptValue group_count( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.groupCount() );
	}

	public static ScriptValue replace_first( final ScriptVariable matcher, final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.replaceFirst( replacement.toStringValue().toString() ) );
	}

	public static ScriptValue replace_all( final ScriptVariable matcher, final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		return new ScriptValue( m.replaceAll( replacement.toStringValue().toString() ) );
	}

	public static ScriptValue reset( final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		m.reset();
		return matcher.getValue();
	}

	public static ScriptValue reset( final ScriptVariable matcher, final ScriptVariable input )
	{
		Matcher m = (Matcher) matcher.getValue().rawValue();
		m.reset( input.toStringValue().toString() );
		return matcher.getValue();
	}

	public static ScriptValue replace_string( final ScriptVariable source, final ScriptVariable search,
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
			returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );
		}

		StaticEntity.globalStringReplace(
			buffer, search.toStringValue().toString(), replace.toStringValue().toString() );
		return returnValue;
	}

	public static ScriptValue split_string( final ScriptVariable string )
	{
		String[] pieces = string.toStringValue().toString().split( KoLConstants.LINE_BREAK );

		ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, pieces.length );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new ScriptValue( i ), Interpreter.parseStringValue( pieces[ i ] ) );
		}

		return value;
	}

	public static ScriptValue split_string( final ScriptVariable string, final ScriptVariable regex )
	{
		String[] pieces = string.toStringValue().toString().split( regex.toStringValue().toString() );

		ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, pieces.length );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new ScriptValue( i ), Interpreter.parseStringValue( pieces[ i ] ) );
		}

		return value;
	}

	public static ScriptValue group_string( final ScriptVariable string, final ScriptVariable regex )
	{
		Matcher userPatternMatcher =
			Pattern.compile( regex.toStringValue().toString() ).matcher( string.toStringValue().toString() );
		ScriptMap value = new ScriptMap( Interpreter.REGEX_GROUP_TYPE );

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
					slice.aset( groupIndexes[ i ], Interpreter.parseStringValue( userPatternMatcher.group( i ) ) );
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

	public static ScriptValue chat_reply( final ScriptVariable string )
	{
		String recipient = ChatManager.lastBlueMessage();
		if ( !recipient.equals( "" ) )
		{
			RequestThread.postRequest( new ChatRequest( recipient, string.toStringValue().toString(), false ) );
		}

		return Interpreter.VOID_VALUE;
	}

	// Quest completion functions.

	public static ScriptValue entryway()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "entryway" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue hedgemaze()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hedgemaze" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue guardians()
	{
		int itemId = SorceressLairManager.fightTowerGuardians( true );
		return Interpreter.makeItemValue( itemId );
	}

	public static ScriptValue chamber()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "chamber" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue tavern()
	{
		int result = StaticEntity.getClient().locateTavernFaucet();
		return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
	}

	// Arithmetic utility functions.

	public static ScriptValue random( final ScriptVariable arg )
	{
		int range = arg.intValue();
		if ( range < 2 )
		{
			throw new RuntimeException( "Random range must be at least 2" );
		}
		return new ScriptValue( KoLConstants.RNG.nextInt( range ) );
	}

	public static ScriptValue round( final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.round( arg.floatValue() ) );
	}

	public static ScriptValue truncate( final ScriptVariable arg )
	{
		return new ScriptValue( (int) arg.floatValue() );
	}

	public static ScriptValue floor( final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.floor( arg.floatValue() ) );
	}

	public static ScriptValue ceil( final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.ceil( arg.floatValue() ) );
	}

	public static ScriptValue square_root( final ScriptVariable val )
	{
		return new ScriptValue( (float) Math.sqrt( val.floatValue() ) );
	}

	// Settings-type functions.

	public static ScriptValue url_encode( final ScriptVariable arg )
		throws UnsupportedEncodingException
	{
		return new ScriptValue( URLEncoder.encode( arg.toStringValue().toString(), "UTF-8" ) );
	}

	public static ScriptValue url_decode( final ScriptVariable arg )
		throws UnsupportedEncodingException
	{
		return new ScriptValue( URLDecoder.decode( arg.toStringValue().toString(), "UTF-8" ) );
	}

	public static ScriptValue get_property( final ScriptVariable name )
	{
		String property = name.toStringValue().toString();
		return !Preferences.isUserEditable( property ) ? Interpreter.STRING_INIT : new ScriptValue(
			Preferences.getString( property ) );
	}

	public static ScriptValue set_property( final ScriptVariable name, final ScriptVariable value )
	{
		// In order to avoid code duplication for combat
		// related settings, use the shell.

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
			"set", name.toStringValue().toString() + "=" + value.toStringValue().toString() );
		return Interpreter.VOID_VALUE;
	}

	// Functions for aggregates.

	public static ScriptValue count( final ScriptVariable arg )
	{
		return new ScriptValue( arg.getValue().count() );
	}

	public static ScriptValue clear( final ScriptVariable arg )
	{
		arg.getValue().clear();
		return Interpreter.VOID_VALUE;
	}

	public static ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2 )
	{
		String filename = var1.toStringValue().toString();
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
		return RuntimeLibrary.readMap( filename, map_variable, true );
	}

	public static ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
	{
		String filename = var1.toStringValue().toString();
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
		boolean compact = var3.intValue() == 1;
		return RuntimeLibrary.readMap( filename, map_variable, compact );
	}

	public static ScriptValue readMap( final String filename, final ScriptCompositeValue result, final boolean compact )
	{
		BufferedReader reader = RuntimeLibrary.getReader( filename );
		if ( reader == null )
		{
			return Interpreter.FALSE_VALUE;
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
			return Interpreter.FALSE_VALUE;
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
		}

		return Interpreter.TRUE_VALUE;
	}

	public static ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2 )
	{
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
		String filename = var2.toStringValue().toString();
		return RuntimeLibrary.printMap( map_variable, filename, true );
	}

	public static ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
	{
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
		String filename = var2.toStringValue().toString();
		boolean compact = var3.intValue() == 1;
		return RuntimeLibrary.printMap( map_variable, filename, compact );
	}

	public static ScriptValue printMap( final ScriptCompositeValue map_variable, final String filename,
		final boolean compact )
	{
		if ( filename.startsWith( "http" ) )
		{
			return Interpreter.FALSE_VALUE;
		}

		PrintStream writer = null;
		File output = RuntimeLibrary.getFile( filename );

		if ( output == null )
		{
			return Interpreter.FALSE_VALUE;
		}

		writer = LogStream.openStream( output, true );
		map_variable.dump( writer, "", compact );
		writer.close();

		return Interpreter.TRUE_VALUE;
	}

	// Custom combat helper functions.

	public static ScriptValue my_location()
	{
		String location = Preferences.getString( "lastAdventure" );
		return location.equals( "" ) ? Interpreter.parseLocationValue( "Rest" ) : Interpreter.parseLocationValue( location );
	}

	public static ScriptValue get_monsters( final ScriptVariable location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure.getAreaSummary();

		int monsterCount = data == null ? 0 : data.getMonsterCount();

		ScriptAggregateType type = new ScriptAggregateType( Interpreter.MONSTER_TYPE, monsterCount );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < monsterCount; ++i )
		{
			value.aset( new ScriptValue( i ), Interpreter.parseMonsterValue( data.getMonster( i ).getName() ) );
		}

		return value;

	}

	public static ScriptValue expected_damage()
	{
		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int baseValue =
			Math.max( 0, FightRequest.getMonsterAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

		float damageAbsorb =
			1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
		float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() );
		return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static ScriptValue expected_damage( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ZERO_VALUE;
		}

		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int baseValue =
			Math.max( 0, monster.getAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

		float damageAbsorb =
			1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
		float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() );
		return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static ScriptValue monster_level_adjustment()
	{
		return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue weight_adjustment()
	{
		return new ScriptValue( KoLCharacter.getFamiliarWeightAdjustment() );
	}

	public static ScriptValue mana_cost_modifier()
	{
		return new ScriptValue( KoLCharacter.getManaCostAdjustment() );
	}

	public static ScriptValue raw_damage_absorption()
	{
		return new ScriptValue( KoLCharacter.getDamageAbsorption() );
	}

	public static ScriptValue damage_absorption_percent()
	{
		int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
		if ( raw == 0 )
		{
			return Interpreter.ZERO_FLOAT_VALUE;
		}

		// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
		// ( sqrt( raw / 10 ) - 1 ) / 10

		double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
		return new ScriptValue( (float) percent );
	}

	public static ScriptValue damage_reduction()
	{
		return new ScriptValue( KoLCharacter.getDamageReduction() );
	}

	public static ScriptValue elemental_resistance()
	{
		return new ScriptValue( KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() ) );
	}

	public static ScriptValue elemental_resistance( final ScriptVariable arg )
	{
		if ( arg.getType().equals( Interpreter.TYPE_ELEMENT ) )
		{
			return new ScriptValue( KoLCharacter.getElementalResistance( arg.intValue() ) );
		}

		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ZERO_VALUE;
		}

		return new ScriptValue( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
	}

	public static ScriptValue combat_rate_modifier()
	{
		return new ScriptValue( KoLCharacter.getCombatRateAdjustment() );
	}

	public static ScriptValue initiative_modifier()
	{
		return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
	}

	public static ScriptValue experience_bonus()
	{
		return new ScriptValue( KoLCharacter.getExperienceAdjustment() );
	}

	public static ScriptValue meat_drop_modifier()
	{
		return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
	}

	public static ScriptValue item_drop_modifier()
	{
		return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
	}

	public static ScriptValue buffed_hit_stat()
	{
		int hitStat = KoLCharacter.getAdjustedHitStat();
		return new ScriptValue( hitStat );
	}

	public static ScriptValue current_hit_stat()
	{
		return KoLCharacter.hitStat() == KoLConstants.MOXIE ? Interpreter.parseStatValue( "moxie" ) : Interpreter.parseStatValue( "muscle" );
	}

	public static ScriptValue monster_element()
	{
		int element = FightRequest.getMonsterDefenseElement();
		return new ScriptValue( Interpreter.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static ScriptValue monster_element( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ELEMENT_INIT;
		}

		int element = monster.getDefenseElement();
		return new ScriptValue( Interpreter.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static ScriptValue monster_attack()
	{
		return new ScriptValue( FightRequest.getMonsterAttack() );
	}

	public static ScriptValue monster_attack( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ZERO_VALUE;
		}

		return new ScriptValue( monster.getAttack() + KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue monster_defense()
	{
		return new ScriptValue( FightRequest.getMonsterDefense() );
	}

	public static ScriptValue monster_defense( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ZERO_VALUE;
		}

		return new ScriptValue( monster.getDefense() + KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue monster_hp()
	{
		return new ScriptValue( FightRequest.getMonsterHealth() );
	}

	public static ScriptValue monster_hp( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		if ( monster == null )
		{
			return Interpreter.ZERO_VALUE;
		}

		return new ScriptValue( monster.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) );
	}

	public static ScriptValue item_drops()
	{
		Monster monster = FightRequest.getLastMonster();
		List data = monster == null ? new ArrayList() : monster.getItems();

		ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				Interpreter.parseItemValue( result.getName() ),
				Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue item_drops( final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue();
		List data = monster == null ? new ArrayList() : monster.getItems();

		ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				Interpreter.parseItemValue( result.getName() ),
				Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue will_usually_dodge()
	{
		return FightRequest.willUsuallyDodge() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue will_usually_miss()
	{
		return FightRequest.willUsuallyMiss() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
	}

	public static ScriptValue numeric_modifier( final ScriptVariable modifier )
	{
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( KoLCharacter.currentNumericModifier( mod ) );
	}

	public static ScriptValue numeric_modifier( final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue().toString();
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( Modifiers.getNumericModifier( name, mod ) );
	}

	public static ScriptValue boolean_modifier( final ScriptVariable modifier )
	{
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( KoLCharacter.currentBooleanModifier( mod ) );
	}

	public static ScriptValue boolean_modifier( final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue().toString();
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( Modifiers.getBooleanModifier( name, mod ) );
	}

	public static ScriptValue effect_modifier( final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue().toString();
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( Interpreter.parseEffectValue( Modifiers.getStringModifier( name, mod ) ) );
	}

	public static ScriptValue class_modifier( final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue().toString();
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( Interpreter.parseClassValue( Modifiers.getStringModifier( name, mod ) ) );
	}

	public static ScriptValue stat_modifier( final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue().toString();
		String mod = modifier.toStringValue().toString();
		return new ScriptValue( Interpreter.parseStatValue( Modifiers.getStringModifier( name, mod ) ) );
	}
}
