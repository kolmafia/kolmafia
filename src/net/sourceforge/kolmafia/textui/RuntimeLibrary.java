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

package net.sourceforge.kolmafia.textui;

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
import java.util.Iterator;
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
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptAggregateType;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptArray;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptCompositeValue;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptExistingFunction;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptFunctionList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptMap;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptType;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptValue;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariable;

public abstract class RuntimeLibrary
{
	private static StringBuffer concatenateBuffer = new StringBuffer();
	private static final GenericRequest VISITOR = new GenericRequest( "" );
	private static final RelayRequest RELAYER = new RelayRequest( false );

	public static final ScriptFunctionList functions = new ScriptFunctionList();

	public static Iterator getFunctions()
	{
		return functions.iterator();
	}

	static
	{
		ScriptType[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "enable", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "disable", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "user_confirm", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "logprint", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "print_html", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "cli_execute", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "load_html", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "write", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "writeln", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "form_field", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "wait", DataTypes.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new ScriptType[] { DataTypes.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_string", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_boolean", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_int", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ANY_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_item", DataTypes.ITEM_TYPE, params ) );
		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_item", DataTypes.ITEM_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_class", DataTypes.CLASS_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_stat", DataTypes.STAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new ScriptType[] { DataTypes.EFFECT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new ScriptType[] { DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_location", DataTypes.LOCATION_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_slot", DataTypes.SLOT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_url", DataTypes.STRING_TYPE, params ) );

		// Functions related to daily information which get
		// updated usually once per day.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "today_to_string", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "moon_phase", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "moon_light", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stat_bonus_today", DataTypes.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stat_bonus_tomorrow", DataTypes.STAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "session_logs", new ScriptAggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "adventure", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "add_item_condition", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "buy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "create", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "use", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "put_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_storage", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "take_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "autosell", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "hermit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "retrieve_item", DataTypes.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "is_npc_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "daily_special", DataTypes.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "refresh_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "available_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "item_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "closet_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "creatable_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_ingredients", DataTypes.RESULT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "storage_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "display_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "shop_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "stash_amount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "pulls_remaining", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "stills_available", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_mushroom_plot", DataTypes.BOOLEAN_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "refresh_status", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "restore_hp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "restore_mp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_name", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_id", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_hash", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_muscle_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_mysticality_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_moxie_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_bad_moon", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_class", DataTypes.CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_level", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_hp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_maxhp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_mp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_maxmp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_primestat", DataTypes.STAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "my_basestat", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "my_buffedstat", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_meat", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_adventures", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_turncount", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_fullness", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "fullness_limit", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_inebriety", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "inebriety_limit", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_spleen_use", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "spleen_limit", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "turns_played", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "can_interact", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "in_hardcore", DataTypes.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new ScriptType[] { DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "mp_cost", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "turns_per_cast", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.EFFECT_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_effect", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "attack", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "steal", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "runaway", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SKILL_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_skill", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "throw_item", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "throw_items", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "run_combat", DataTypes.BUFFER_TYPE, params ) );

		// Equipment functions.

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "can_equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SLOT_TYPE, DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SLOT_TYPE };
		functions.addElement( new ScriptExistingFunction( "equipped_item", DataTypes.ITEM_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_equipped", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "have_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "use_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "familiar_equipment", DataTypes.ITEM_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FAMILIAR_TYPE };
		functions.addElement( new ScriptExistingFunction( "familiar_weight", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "weapon_hands", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "weapon_type", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "ranged_weapon", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_power", DataTypes.INT_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "council", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "current_mcd", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "change_mcd", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_chef", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "have_bartender", DataTypes.BOOLEAN_TYPE, params ) );

		// String parsing functions.

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "contains_text", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "extract_meat", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "extract_items", DataTypes.RESULT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "length", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_lower_case", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "to_upper_case", DataTypes.STRING_TYPE, params ) );

		// String buffer functions

		params = new ScriptType[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "append", DataTypes.BUFFER_TYPE, params ) );
		params = new ScriptType[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "insert", DataTypes.BUFFER_TYPE, params ) );
		params =
			new ScriptType[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace", DataTypes.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "delete", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE };
		functions.addElement( new ScriptExistingFunction( "append_tail", DataTypes.BUFFER_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "append_replacement", DataTypes.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "create_matcher", DataTypes.MATCHER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "find", DataTypes.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "start", DataTypes.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "end", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "group", DataTypes.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "group", DataTypes.BOOLEAN_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "group_count", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_first", DataTypes.STRING_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_all", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MATCHER_TYPE };
		functions.addElement( new ScriptExistingFunction( "reset", DataTypes.MATCHER_TYPE, params ) );
		params = new ScriptType[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "reset", DataTypes.MATCHER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "split_string", new ScriptAggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "group_string", DataTypes.REGEX_GROUP_TYPE, params ) );

		// Assorted functions

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "chat_reply", DataTypes.VOID_TYPE, params ) );

		// Quest handling functions.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "entryway", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "hedgemaze", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "guardians", DataTypes.ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "chamber", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "tavern", DataTypes.INT_TYPE, params ) );

		// Arithmetic utility functions.

		params = new ScriptType[] { DataTypes.INT_TYPE };
		functions.addElement( new ScriptExistingFunction( "random", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "round", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "truncate", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "floor", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "ceil", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.FLOAT_TYPE };
		functions.addElement( new ScriptExistingFunction( "square_root", DataTypes.FLOAT_TYPE, params ) );

		// Settings-type functions.

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "url_encode", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "url_decode", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_property", DataTypes.STRING_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "set_property", DataTypes.VOID_TYPE, params ) );

		// Functions for aggregates.

		params = new ScriptType[] { DataTypes.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "count", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "clear", DataTypes.VOID_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE };
		functions.addElement( new ScriptExistingFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.addElement( new ScriptExistingFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.addElement( new ScriptExistingFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "my_location", DataTypes.LOCATION_TYPE, params ) );

		params = new ScriptType[] { DataTypes.LOCATION_TYPE };
		functions.addElement( new ScriptExistingFunction( "get_monsters", new ScriptAggregateType(
			DataTypes.MONSTER_TYPE, 0 ), params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_level_adjustment", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "weight_adjustment", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "mana_cost_modifier", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "raw_damage_absorption", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "damage_absorption_percent", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "damage_reduction", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ELEMENT_TYPE };
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "combat_rate_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "initiative_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "experience_bonus", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "meat_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "item_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "buffed_hit_stat", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "current_hit_stat", DataTypes.STAT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "item_drops", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.MONSTER_TYPE };
		functions.addElement( new ScriptExistingFunction( "item_drops", DataTypes.INT_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "will_usually_miss", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		functions.addElement( new ScriptExistingFunction( "will_usually_dodge", DataTypes.BOOLEAN_TYPE, params ) );

		// Modifier introspection

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "effect_modifier", DataTypes.EFFECT_TYPE, params ) );

		params = new ScriptType[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "class_modifier", DataTypes.CLASS_TYPE, params ) );

		params = new ScriptType[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.addElement( new ScriptExistingFunction( "stat_modifier", DataTypes.STAT_TYPE, params ) );
	}

        public static Method findMethod( final String name, final Class[] args )
		throws NoSuchMethodException
	{
                return RuntimeLibrary.class.getMethod( name, args );
        }

	private static ScriptValue continueValue()
	{
		return KoLmafia.permitsContinue() && !KoLmafia.hadPendingState() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

        // Basic utility functions which print information
        // or allow for easy testing.

        public static ScriptValue enable( final Interpreter interpreter, final ScriptVariable name )
 	{
                StaticEntity.enable( name.toStringValue( interpreter ).toString().toLowerCase() );
                return DataTypes.VOID_VALUE;
        }

	public static ScriptValue disable( final Interpreter interpreter, final ScriptVariable name )
	{
		StaticEntity.disable( name.toStringValue( interpreter ).toString().toLowerCase() );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue user_confirm( final Interpreter interpreter, final ScriptVariable message )
	{
		return KoLFrame.confirm( message.toStringValue( interpreter ).toString() ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue logprint( final Interpreter interpreter, final ScriptVariable string )
	{
		String parameters = string.toStringValue( interpreter ).toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.getSessionStream().println( "> " + parameters );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue print( final Interpreter interpreter, final ScriptVariable string )
	{
		String parameters = string.toStringValue( interpreter ).toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.printLine( parameters );
		RequestLogger.getSessionStream().println( "> " + parameters );

		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue print( final Interpreter interpreter, final ScriptVariable string, final ScriptVariable color )
	{
		String parameters = string.toStringValue( interpreter ).toString();

		parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

		String colorString = color.toStringValue( interpreter ).toString();
		colorString = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( colorString, "\"" ), "<" );

		RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );
		RequestLogger.getSessionStream().println( " > " + parameters );

		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue print_html( final Interpreter interpreter, final ScriptVariable string )
	{
		RequestLogger.printLine( string.toStringValue( interpreter ).toString() );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue abort( final Interpreter interpreter )
	{
		RequestThread.declareWorldPeace();
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue abort( final Interpreter interpreter, final ScriptVariable string )
	{
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, string.toStringValue( interpreter ).toString() );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue cli_execute( final Interpreter interpreter, final ScriptVariable string )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toStringValue( interpreter ).toString() );
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

	public static ScriptValue load_html( final Interpreter interpreter, final ScriptVariable string )
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( DataTypes.BUFFER_TYPE, "", buffer );

		String location = string.toStringValue( interpreter ).toString();
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

	public static ScriptValue write( final Interpreter interpreter, final ScriptVariable string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.VOID_VALUE;
		}

		KoLmafiaASH.serverReplyBuffer.append( string.toStringValue( interpreter ).toString() );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue writeln( final Interpreter interpreter, final ScriptVariable string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.VOID_VALUE;
		}

		RuntimeLibrary.write( interpreter, string );
		KoLmafiaASH.serverReplyBuffer.append( KoLConstants.LINE_BREAK );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue form_field( final Interpreter interpreter, final ScriptVariable key )
	{
		if ( KoLmafiaASH.relayRequest == null )
		{
			return DataTypes.STRING_INIT;
		}

		String value = KoLmafiaASH.relayRequest.getFormField( key.toStringValue( interpreter ).toString() );
		return value == null ? DataTypes.STRING_INIT : new ScriptValue( value );
	}

	public static ScriptValue visit_url( final Interpreter interpreter )
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( DataTypes.BUFFER_TYPE, "", buffer );

		RequestThread.postRequest( KoLmafiaASH.relayRequest );
		if ( KoLmafiaASH.relayRequest.responseText != null )
		{
			buffer.append( KoLmafiaASH.relayRequest.responseText );
		}

		return returnValue;
	}

	public static ScriptValue visit_url( final Interpreter interpreter, final ScriptVariable string )
	{
		return RuntimeLibrary.visit_url( string.toStringValue( interpreter ).toString() );
	}

	private static ScriptValue visit_url( final String location )
	{
		StringBuffer buffer = new StringBuffer();
		ScriptValue returnValue = new ScriptValue( DataTypes.BUFFER_TYPE, "", buffer );

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

	public static ScriptValue wait( final Interpreter interpreter, final ScriptVariable delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "wait " + delay.intValue( interpreter ) );
		return DataTypes.VOID_VALUE;
	}

	// Type conversion functions which allow conversion
	// of one data format to another.

	public static ScriptValue to_string( final Interpreter interpreter, final ScriptVariable val )
	{
		return val.toStringValue( interpreter );
	}

	public static ScriptValue to_boolean( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );
		return ( value.intValue() != 0 || value.toString().equals( "true" ) ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue to_int( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			return DataTypes.parseIntValue( value.toString() );
		}

		return new ScriptValue( value.intValue() );
	}

	public static ScriptValue to_float( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			return DataTypes.parseFloatValue( value.toString() );
		}

		if ( value.intValue() != 0 )
		{
			return new ScriptValue( (float) value.intValue() );
		}

		return new ScriptValue( value.floatValue() );
	}

	public static ScriptValue to_item( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeItemValue( value.intValue() );
		}

		return DataTypes.parseItemValue( value.toString() );
	}

	public static ScriptValue to_class( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );
		return DataTypes.parseClassValue( value.toString() );
	}

	public static ScriptValue to_stat( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );
		return DataTypes.parseStatValue( value.toString() );
	}

	public static ScriptValue to_skill( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeSkillValue( value.intValue() );
		}

		if ( value.getType().equals( DataTypes.TYPE_EFFECT ) )
		{
			return DataTypes.parseSkillValue( UneffectRequest.effectToSkill( value.toString() ) );
		}

		return DataTypes.parseSkillValue( value.toString() );
	}

	public static ScriptValue to_effect( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeEffectValue( val.intValue( interpreter ) );
		}

		if ( value.getType().equals( DataTypes.TYPE_SKILL ) )
		{
			return DataTypes.parseEffectValue( UneffectRequest.skillToEffect( value.toString() ) );
		}

		return DataTypes.parseEffectValue( value.toString() );
	}

	public static ScriptValue to_location( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );
		return DataTypes.parseLocationValue( value.toString() );
	}

	public static ScriptValue to_familiar( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );

		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeFamiliarValue( value.intValue() );
		}

		return DataTypes.parseFamiliarValue( value.toString() );
	}

	public static ScriptValue to_monster( final Interpreter interpreter, final ScriptVariable val )
	{
		ScriptValue value = val.getValue( interpreter );
		return DataTypes.parseMonsterValue( value.toString() );
	}

	public static ScriptValue to_slot( final Interpreter interpreter, final ScriptVariable item )
	{
		switch ( ItemDatabase.getConsumptionType( item.intValue( interpreter ) ) )
		{
		case KoLConstants.EQUIP_HAT:
			return DataTypes.parseSlotValue( "hat" );
		case KoLConstants.EQUIP_WEAPON:
			return DataTypes.parseSlotValue( "weapon" );
		case KoLConstants.EQUIP_OFFHAND:
			return DataTypes.parseSlotValue( "off-hand" );
		case KoLConstants.EQUIP_SHIRT:
			return DataTypes.parseSlotValue( "shirt" );
		case KoLConstants.EQUIP_PANTS:
			return DataTypes.parseSlotValue( "pants" );
		case KoLConstants.EQUIP_FAMILIAR:
			return DataTypes.parseSlotValue( "familiar" );
		case KoLConstants.EQUIP_ACCESSORY:
			return DataTypes.parseSlotValue( "acc1" );
		default:
			return DataTypes.parseSlotValue( "none" );
		}
	}

	public static ScriptValue to_url( final Interpreter interpreter, final ScriptVariable val )
	{
		KoLAdventure adventure = (KoLAdventure) val.rawValue( interpreter );
		return new ScriptValue( adventure.getRequest().getURLString() );
	}

	// Functions related to daily information which get
	// updated usually once per day.

	public static ScriptValue today_to_string( final Interpreter interpreter )
	{
		return DataTypes.parseStringValue( KoLConstants.DAILY_FORMAT.format( new Date() ) );
	}

	public static ScriptValue moon_phase( final Interpreter interpreter )
	{
		return new ScriptValue( HolidayDatabase.getPhaseStep() );
	}

	public static ScriptValue moon_light( final Interpreter interpreter )
	{
		return new ScriptValue( HolidayDatabase.getMoonlight() );
	}

	public static ScriptValue stat_bonus_today( final Interpreter interpreter )
	{
		if ( KoLmafiaCLI.testConditional( "today is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( KoLmafiaCLI.testConditional( "today is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( KoLmafiaCLI.testConditional( "today is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static ScriptValue stat_bonus_tomorrow( final Interpreter interpreter )
	{
		if ( KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( KoLmafiaCLI.testConditional( "tomorrow is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static ScriptValue session_logs( final Interpreter interpreter, final ScriptVariable dayCount )
	{
		return RuntimeLibrary.getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue( interpreter ) );
	}

	public static ScriptValue session_logs( final Interpreter interpreter, final ScriptVariable player, final ScriptVariable dayCount )
	{
		return RuntimeLibrary.getSessionLogs( player.toStringValue( interpreter ).toString(), dayCount.intValue( interpreter ) );
	}

	private static ScriptValue getSessionLogs( final String name, final int dayCount )
	{
		String[] files = new String[ dayCount ];

		Calendar timestamp = Calendar.getInstance();

		ScriptAggregateType type = new ScriptAggregateType( DataTypes.STRING_TYPE, files.length );
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

			value.aset( new ScriptValue( i ), DataTypes.parseStringValue( contents.toString() ) );
		}

		return value;
	}

	// Major functions related to adventuring and
	// item management.

	public static ScriptValue adventure( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable loc )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "adventure " + count + " " + loc.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue add_item_condition( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return DataTypes.VOID_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "conditions add " + count + " " + item.toStringValue( interpreter ) );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue buy( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		AdventureResult itemToBuy = new AdventureResult( item.intValue( interpreter ), 1 );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "buy " + count + " " + itemToBuy.getName() );
		return initialAmount + count == itemToBuy.getCount( KoLConstants.inventory ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue create( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "create " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue use( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use " + count + " " + item.toStringValue( interpreter ) );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue eat( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "eat " + count + " " + item.toStringValue( interpreter ) );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue drink( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "drink " + count + " " + item.toStringValue( interpreter ) );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue put_closet( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet put " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_shop( final Interpreter interpreter, final ScriptVariable priceVariable, final ScriptVariable limitVariable, final ScriptVariable itemVariable )
	{
		int price = priceVariable.intValue( interpreter );
		int limit = limitVariable.intValue( interpreter );
		String item = itemVariable.toStringValue( interpreter ).toString();
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mallsell " + item + " @ " + price + " limit " + limit );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_stash( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash put " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue put_display( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display put " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_closet( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet take " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_storage( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hagnk " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_display( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display take " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue take_stash( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash take " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue autosell( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "sell " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue hermit( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit " + count + " " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue retrieve_item( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable item )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		AdventureDatabase.retrieveItem( new AdventureResult( item.intValue( interpreter ), count ) );
		return RuntimeLibrary.continueValue();
	}

	// Major functions which provide item-related
	// information.

	public static ScriptValue is_npc_item( final Interpreter interpreter, final ScriptVariable item )
	{
		return NPCStoreDatabase.contains( ItemDatabase.getItemName( item.intValue( interpreter ) ), false ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue daily_special( final Interpreter interpreter )
	{
		AdventureResult special =
			KoLCharacter.inMoxieSign() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.inMysticalitySign() ? ChezSnooteeRequest.getDailySpecial() : null;

		return special == null ? DataTypes.ITEM_INIT : DataTypes.parseItemValue( special.getName() );
	}

	public static ScriptValue refresh_stash( final Interpreter interpreter )
	{
		RequestThread.postRequest( new ClanStashRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue available_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );

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

	public static ScriptValue item_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );
		return new ScriptValue( item.getCount( KoLConstants.inventory ) );
	}

	public static ScriptValue closet_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );
		return new ScriptValue( item.getCount( KoLConstants.closet ) );
	}

	public static ScriptValue creatable_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		CreateItemRequest item = CreateItemRequest.getInstance( arg.intValue( interpreter ) );
		return new ScriptValue( item == null ? 0 : item.getQuantityPossible() );
	}

	public static ScriptValue get_ingredients( final Interpreter interpreter, final ScriptVariable item )
	{
		AdventureResult[] data = ConcoctionDatabase.getIngredients( item.intValue( interpreter ) );
		ScriptMap value = new ScriptMap( DataTypes.RESULT_TYPE );

		for ( int i = 0; i < data.length; ++i )
		{
			value.aset(
				DataTypes.parseItemValue( data[ i ].getName() ),
				DataTypes.parseIntValue( String.valueOf( data[ i ].getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue storage_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );
		return new ScriptValue( item.getCount( KoLConstants.storage ) );
	}

	public static ScriptValue display_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		if ( KoLConstants.collection.isEmpty() )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}

		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );
		return new ScriptValue( item.getCount( KoLConstants.collection ) );
	}

	public static ScriptValue shop_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		LockableListModel list = StoreManager.getSoldItemList();
		if ( list.isEmpty() )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		SoldItem item = new SoldItem( arg.intValue( interpreter ), 0, 0, 0, 0 );
		int index = list.indexOf( item );

		if ( index < 0 )
		{
			return new ScriptValue( 0 );
		}

		item = (SoldItem) list.get( index );
		return new ScriptValue( item.getQuantity() );
	}

	public static ScriptValue stash_amount( final Interpreter interpreter, final ScriptVariable arg )
	{
		List stash = ClanManager.getStash();
		if ( stash.isEmpty() )
		{
			RequestThread.postRequest( new ClanStashRequest() );
		}

		AdventureResult item = new AdventureResult( arg.intValue( interpreter ), 0 );
		return new ScriptValue( item.getCount( stash ) );
	}

	public static ScriptValue pulls_remaining( final Interpreter interpreter )
	{
		return new ScriptValue( ItemManageFrame.getPullsRemaining() );
	}

	public static ScriptValue stills_available( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getStillsAvailable() );
	}

	public static ScriptValue have_mushroom_plot( final Interpreter interpreter )
	{
		return MushroomManager.ownsPlot() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	// The following functions pertain to providing updated
	// information relating to the player.

	public static ScriptValue refresh_status( final Interpreter interpreter )
	{
		RequestThread.postRequest( CharPaneRequest.getInstance() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue restore_hp( final Interpreter interpreter, final ScriptVariable amount )
	{
		return new ScriptValue( StaticEntity.getClient().recoverHP( amount.intValue( interpreter ) ) );
	}

	public static ScriptValue restore_mp( final Interpreter interpreter, final ScriptVariable amount )
	{
		int desiredMP = amount.intValue( interpreter );
		while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
		{
			StaticEntity.getClient().recoverMP( desiredMP );
		}
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue my_name( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getUserName() );
	}

	public static ScriptValue my_id( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getPlayerId() );
	}

	public static ScriptValue my_hash( final Interpreter interpreter )
	{
		return new ScriptValue( GenericRequest.passwordHash );
	}

	public static ScriptValue in_muscle_sign( final Interpreter interpreter )
	{
		return KoLCharacter.inMuscleSign() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue in_mysticality_sign( final Interpreter interpreter )
	{
		return KoLCharacter.inMysticalitySign() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue in_moxie_sign( final Interpreter interpreter )
	{
		return KoLCharacter.inMoxieSign() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue in_bad_moon( final Interpreter interpreter )
	{
		return KoLCharacter.inBadMoon() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue my_class( final Interpreter interpreter )
	{
		return DataTypes.makeClassValue( KoLCharacter.getClassType() );
	}

	public static ScriptValue my_level( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getLevel() );
	}

	public static ScriptValue my_hp( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getCurrentHP() );
	}

	public static ScriptValue my_maxhp( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getMaximumHP() );
	}

	public static ScriptValue my_mp( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getCurrentMP() );
	}

	public static ScriptValue my_maxmp( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getMaximumMP() );
	}

	public static ScriptValue my_primestat( final Interpreter interpreter )
	{
		int primeIndex = KoLCharacter.getPrimeIndex();
		return primeIndex == 0 ? DataTypes.MUSCLE_VALUE : primeIndex == 1 ? DataTypes.MYSTICALITY_VALUE : DataTypes.MOXIE_VALUE;
	}

	public static ScriptValue my_basestat( final Interpreter interpreter, final ScriptVariable arg )
	{
		int stat = arg.intValue( interpreter );

		if ( stat == KoLConstants.MUSCLE )
		{
			return new ScriptValue( KoLCharacter.getBaseMuscle() );
		}
		if ( stat == KoLConstants.MYSTICALITY )
		{
			return new ScriptValue( KoLCharacter.getBaseMysticality() );
		}
		if ( stat == KoLConstants.MOXIE )
		{
			return new ScriptValue( KoLCharacter.getBaseMoxie() );
		}

		throw new RuntimeException( "Internal error: unknown stat" );
	}

	public static ScriptValue my_buffedstat( final Interpreter interpreter, final ScriptVariable arg )
	{
		int stat = arg.intValue( interpreter );

		if ( stat == KoLConstants.MUSCLE )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
		}
		if ( stat == KoLConstants.MYSTICALITY )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
		}
		if ( stat == KoLConstants.MOXIE )
		{
			return new ScriptValue( KoLCharacter.getAdjustedMoxie() );
		}

		throw new RuntimeException( "Internal error: unknown stat" );
	}

	public static ScriptValue my_meat( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getAvailableMeat() );
	}

	public static ScriptValue my_adventures( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getAdventuresLeft() );
	}

	public static ScriptValue my_turncount( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getCurrentRun() );
	}

	public static ScriptValue my_fullness( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getFullness() );
	}

	public static ScriptValue fullness_limit( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getFullnessLimit() );
	}

	public static ScriptValue my_inebriety( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getInebriety() );
	}

	public static ScriptValue inebriety_limit( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getInebrietyLimit() );
	}

	public static ScriptValue my_spleen_use( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getSpleenUse() );
	}

	public static ScriptValue spleen_limit( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getSpleenLimit() );
	}

	public static ScriptValue can_eat( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.canEat() );
	}

	public static ScriptValue can_drink( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.canDrink() );
	}

	public static ScriptValue turns_played( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getCurrentRun() );
	}

	public static ScriptValue can_interact( final Interpreter interpreter )
	{
		return KoLCharacter.canInteract() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue in_hardcore( final Interpreter interpreter )
	{
		return KoLCharacter.isHardcore() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	// Basic skill and effect functions, including those used
	// in custom combat consult scripts.

	public static ScriptValue have_skill( final Interpreter interpreter, final ScriptVariable arg )
	{
		return KoLCharacter.hasSkill( arg.intValue( interpreter ) ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue mp_cost( final Interpreter interpreter, final ScriptVariable skill )
	{
		return new ScriptValue( SkillDatabase.getMPConsumptionById( skill.intValue( interpreter ) ) );
	}

	public static ScriptValue turns_per_cast( final Interpreter interpreter, final ScriptVariable skill )
	{
		return new ScriptValue( SkillDatabase.getEffectDuration( skill.intValue( interpreter ) ) );
	}

	public static ScriptValue have_effect( final Interpreter interpreter, final ScriptVariable arg )
	{
		List potentialEffects = EffectDatabase.getMatchingNames( arg.toStringValue( interpreter ).toString() );
		AdventureResult effect =
			potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
		return effect == null ? DataTypes.ZERO_VALUE : new ScriptValue( effect.getCount( KoLConstants.activeEffects ) );
	}

	public static ScriptValue use_skill( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable skillVariable )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		ScriptValue skill = skillVariable.getValue( interpreter );
		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
			{
				RuntimeLibrary.use_skill( interpreter, skillVariable );
			}

			return DataTypes.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count + " " + skill.toString() );
		return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
	}

	public static ScriptValue use_skill( final Interpreter interpreter, final ScriptVariable skillVariable )
	{
		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		ScriptValue skill = skillVariable.getValue( interpreter );
		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			return RuntimeLibrary.visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast 1 " + skill.toString() );
		return new ScriptValue( UseSkillRequest.lastUpdate );
	}

	public static ScriptValue use_skill( final Interpreter interpreter, final ScriptVariable countVariable, final ScriptVariable skillVariable, final ScriptVariable target )
	{
		int count = countVariable.intValue( interpreter );
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		ScriptValue skill = skillVariable.getValue( interpreter );
		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
			{
				RuntimeLibrary.use_skill( interpreter, skillVariable );
			}

			return DataTypes.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count + " " + skill.toString() + " on " + target.toStringValue( interpreter ) );
		return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
	}

	public static ScriptValue attack( final Interpreter interpreter )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=attack" );
	}

	public static ScriptValue steal( final Interpreter interpreter )
	{
		if ( !FightRequest.wonInitiative() )
		{
			return RuntimeLibrary.attack( interpreter );
		}

		return RuntimeLibrary.visit_url( "fight.php?action=steal" );
	}

	public static ScriptValue runaway( final Interpreter interpreter )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=runaway" );
	}

	public static ScriptValue throw_item( final Interpreter interpreter, final ScriptVariable item )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item.intValue( interpreter ) );
	}

	public static ScriptValue throw_items( final Interpreter interpreter, final ScriptVariable item1, final ScriptVariable item2 )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue( interpreter ) + "&whichitem2=" + item2.intValue( interpreter ) );
	}

	public static ScriptValue run_combat( final Interpreter interpreter )
	{
		RequestThread.postRequest( FightRequest.INSTANCE );
		String response =
			KoLmafiaASH.relayScript == null ? FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

		return new ScriptValue( DataTypes.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	// Equipment functions.

	public static ScriptValue can_equip( final Interpreter interpreter, final ScriptVariable item )
	{
		return EquipmentDatabase.canEquip( ItemDatabase.getItemName( item.intValue( interpreter ) ) ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue equip( final Interpreter interpreter, final ScriptVariable item )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + item.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue equip( final Interpreter interpreter, final ScriptVariable slotVariable, final ScriptVariable itemVariable )
	{
		String slot = slotVariable.toStringValue( interpreter ).toString();
		ScriptValue item = itemVariable.getValue( interpreter );
		if ( item.equals( DataTypes.ITEM_INIT ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "unequip " + slot );
		}
		else
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + slot + " " + item.toString() );
		}

		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue equipped_item( final Interpreter interpreter, final ScriptVariable slot )
	{
		return DataTypes.makeItemValue( KoLCharacter.getEquipment( slot.intValue( interpreter ) ).getName() );
	}

	public static ScriptValue have_equipped( final Interpreter interpreter, final ScriptVariable item )
	{
		return KoLCharacter.hasEquipped( new AdventureResult( item.intValue( interpreter ), 1 ) ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue outfit( final Interpreter interpreter, final ScriptVariable outfit )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue( interpreter ).toString() );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue have_outfit( final Interpreter interpreter, final ScriptVariable outfit )
	{
		SpecialOutfit so = KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue( interpreter ).toString() );

		if ( so == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		return EquipmentDatabase.hasOutfit( so.getOutfitId() ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue weapon_hands( final Interpreter interpreter, final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.getHands( item.intValue( interpreter ) ) );
	}

	public static ScriptValue weapon_type( final Interpreter interpreter, final ScriptVariable item )
	{
		String type = EquipmentDatabase.getType( item.intValue( interpreter ) );
		return new ScriptValue( type == null ? "unknown" : type );
	}

	public static ScriptValue ranged_weapon( final Interpreter interpreter, final ScriptVariable item )
	{
		return EquipmentDatabase.isRanged( item.intValue( interpreter ) ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue get_power( final Interpreter interpreter, final ScriptVariable item )
	{
		return new ScriptValue( EquipmentDatabase.getPower( item.intValue( interpreter ) ) );
	}

	public static ScriptValue my_familiar( final Interpreter interpreter )
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
	}

	public static ScriptValue have_familiar( final Interpreter interpreter, final ScriptVariable familiar )
	{
		return KoLCharacter.findFamiliar( familiar.toStringValue( interpreter ).toString() ) != null ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue use_familiar( final Interpreter interpreter, final ScriptVariable familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue familiar_equipment( final Interpreter interpreter, final ScriptVariable familiar )
	{
		return DataTypes.parseItemValue( FamiliarDatabase.getFamiliarItem( familiar.intValue( interpreter ) ) );
	}

	public static ScriptValue familiar_weight( final Interpreter interpreter, final ScriptVariable familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue( interpreter ).toString() );
		return fam == null ? DataTypes.ZERO_VALUE : new ScriptValue( fam.getWeight() );
	}

	// Random other functions related to current in-game
	// state, not directly tied to the character.

	public static ScriptValue council( final Interpreter interpreter )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "council" );
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue current_mcd( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getSignedMLAdjustment() );
	}

	public static ScriptValue change_mcd( final Interpreter interpreter, final ScriptVariable level )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mind-control " + level.intValue( interpreter ) );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue have_chef( final Interpreter interpreter )
	{
		return KoLCharacter.hasChef() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue have_bartender( final Interpreter interpreter )
	{
		return KoLCharacter.hasBartender() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	// String parsing functions.

	public static ScriptValue contains_text( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable search )
	{
		return source.toStringValue( interpreter ).toString().indexOf( search.toStringValue( interpreter ).toString() ) != -1 ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue extract_meat( final Interpreter interpreter, final ScriptVariable string )
	{
		ArrayList data = new ArrayList();
		StaticEntity.getClient().processResults( string.toStringValue( interpreter ).toString(), data );

		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			if ( result.getName().equals( AdventureResult.MEAT ) )
			{
				return new ScriptValue( result.getCount() );
			}
		}

		return DataTypes.ZERO_VALUE;
	}

	public static ScriptValue extract_items( final Interpreter interpreter, final ScriptVariable string )
	{
		ArrayList data = new ArrayList();
		StaticEntity.getClient().processResults( string.toStringValue( interpreter ).toString(), data );
		ScriptMap value = new ScriptMap( DataTypes.RESULT_TYPE );

		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			if ( result.isItem() )
			{
				value.aset(
					DataTypes.parseItemValue( result.getName() ),
					DataTypes.parseIntValue( String.valueOf( result.getCount() ) ) );
			}
		}

		return value;
	}

	public static ScriptValue length( final Interpreter interpreter, final ScriptVariable string )
	{
		return new ScriptValue( string.toStringValue( interpreter ).toString().length() );
	}

	public static ScriptValue index_of( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable search )
	{
		String string = source.toStringValue( interpreter ).toString();
		String substring = search.toStringValue( interpreter ).toString();
		return new ScriptValue( string.indexOf( substring ) );
	}

	public static ScriptValue index_of( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable search,
		final ScriptVariable start )
	{
		String string = source.toStringValue( interpreter ).toString();
		String substring = search.toStringValue( interpreter ).toString();
		int begin = start.intValue( interpreter );
		return new ScriptValue( string.indexOf( substring, begin ) );
	}

	public static ScriptValue last_index_of( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable search )
	{
		String string = source.toStringValue( interpreter ).toString();
		String substring = search.toStringValue( interpreter ).toString();
		return new ScriptValue( string.lastIndexOf( substring ) );
	}

	public static ScriptValue last_index_of( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable search,
		final ScriptVariable start )
	{
		String string = source.toStringValue( interpreter ).toString();
		String substring = search.toStringValue( interpreter ).toString();
		int begin = start.intValue( interpreter );
		return new ScriptValue( string.lastIndexOf( substring, begin ) );
	}

	public static ScriptValue substring( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable start )
	{
		String string = source.toStringValue( interpreter ).toString();
		int begin = start.intValue( interpreter );
		return new ScriptValue( string.substring( begin ) );
	}

	public static ScriptValue substring( final Interpreter interpreter, final ScriptVariable source, final ScriptVariable start,
		final ScriptVariable finish )
	{
		String string = source.toStringValue( interpreter ).toString();
		int begin = start.intValue( interpreter );
		int end = finish.intValue( interpreter );
		return new ScriptValue( string.substring( begin, end ) );
	}

	public static ScriptValue to_upper_case( final Interpreter interpreter, final ScriptVariable string )
	{
		return new ScriptValue( string.toStringValue( interpreter ).toString().toUpperCase() );
	}

	public static ScriptValue to_lower_case( final Interpreter interpreter, final ScriptVariable string )
	{
		return new ScriptValue( string.toStringValue( interpreter ).toString().toLowerCase() );
	}

	public static ScriptValue append( final Interpreter interpreter, final ScriptVariable buffer, final ScriptVariable s )
	{
		ScriptValue retval = buffer.getValue( interpreter );
		StringBuffer current = (StringBuffer) retval.rawValue();
		current.append( s.toStringValue( interpreter ).toString() );
		return retval;
	}

	public static ScriptValue insert( final Interpreter interpreter, final ScriptVariable buffer, final ScriptVariable index, final ScriptVariable s )
	{
		ScriptValue retval = buffer.getValue( interpreter );
		StringBuffer current = (StringBuffer) retval.rawValue();
		current.insert( index.intValue( interpreter ), s.toStringValue( interpreter ).toString() );
		return retval;
	}

	public static ScriptValue replace( final Interpreter interpreter, final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end,
		final ScriptVariable s )
	{
		ScriptValue retval = buffer.getValue( interpreter );
		StringBuffer current = (StringBuffer) retval.rawValue();
		current.replace( start.intValue( interpreter ), end.intValue( interpreter ), s.toStringValue( interpreter ).toString() );
		return retval;
	}

	public static ScriptValue delete( final Interpreter interpreter, final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end )
	{
		ScriptValue retval = buffer.getValue( interpreter );
		StringBuffer current = (StringBuffer) retval.rawValue();
		current.delete( start.intValue( interpreter ), end.intValue( interpreter ) );
		return retval;
	}

	public static ScriptValue append_tail( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable current )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		ScriptValue retval = current.getValue( interpreter );
		StringBuffer buffer = (StringBuffer) retval.rawValue();
		m.appendTail( buffer );
		return retval;
	}

	public static ScriptValue append_replacement( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable current,
		final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		ScriptValue retval = current.getValue( interpreter );
		StringBuffer buffer = (StringBuffer) retval.rawValue();
		m.appendReplacement( buffer, replacement.toStringValue( interpreter ).toString() );
		return retval;
	}

	public static ScriptValue create_matcher( final Interpreter interpreter, final ScriptVariable patternVariable, final ScriptVariable stringVariable )
	{
		String pattern = patternVariable.toStringValue( interpreter ).toString();
		String string = stringVariable.toStringValue( interpreter ).toString();
		return new ScriptValue( DataTypes.MATCHER_TYPE, pattern,
					Pattern.compile( pattern, Pattern.DOTALL ).matcher( string ) );
	}

	public static ScriptValue find( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return m.find() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue start( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.start() );
	}

	public static ScriptValue end( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.end() );
	}

	public static ScriptValue group( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.group() );
	}

	public static ScriptValue group( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable group )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.group( group.intValue( interpreter ) ) );
	}

	public static ScriptValue group_count( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.groupCount() );
	}

	public static ScriptValue replace_first( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.replaceFirst( replacement.toStringValue( interpreter ).toString() ) );
	}

	public static ScriptValue replace_all( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable replacement )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		return new ScriptValue( m.replaceAll( replacement.toStringValue( interpreter ).toString() ) );
	}

	public static ScriptValue reset( final Interpreter interpreter, final ScriptVariable matcher )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		m.reset();
		return matcher.getValue( interpreter );
	}

	public static ScriptValue reset( final Interpreter interpreter, final ScriptVariable matcher, final ScriptVariable input )
	{
		Matcher m = (Matcher) matcher.getValue( interpreter ).rawValue();
		m.reset( input.toStringValue( interpreter ).toString() );
		return matcher.getValue( interpreter );
	}

	public static ScriptValue replace_string( final Interpreter interpreter, final ScriptVariable source,
						  final ScriptVariable searchVariable, final ScriptVariable replaceVariable )
	{
		StringBuffer buffer;
		ScriptValue returnValue;

		if ( source.getValue( interpreter ).rawValue() instanceof StringBuffer )
		{
			buffer = (StringBuffer) source.getValue( interpreter ).rawValue();
			returnValue = source.getValue( interpreter );
		}
		else
		{
			buffer = new StringBuffer( source.toStringValue( interpreter ).toString() );
			returnValue = new ScriptValue( DataTypes.BUFFER_TYPE, "", buffer );
		}

		String search = searchVariable.toStringValue( interpreter ).toString();
		String replace = replaceVariable.toStringValue( interpreter ).toString();

		StaticEntity.globalStringReplace( buffer, search, replace );
		return returnValue;
	}

	public static ScriptValue split_string( final Interpreter interpreter, final ScriptVariable string )
	{
		String[] pieces = string.toStringValue( interpreter ).toString().split( KoLConstants.LINE_BREAK );

		ScriptAggregateType type = new ScriptAggregateType( DataTypes.STRING_TYPE, pieces.length );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new ScriptValue( i ), DataTypes.parseStringValue( pieces[ i ] ) );
		}

		return value;
	}

	public static ScriptValue split_string( final Interpreter interpreter, final ScriptVariable string, final ScriptVariable regex )
	{
		String[] pieces = string.toStringValue( interpreter ).toString().split( regex.toStringValue( interpreter ).toString() );

		ScriptAggregateType type = new ScriptAggregateType( DataTypes.STRING_TYPE, pieces.length );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new ScriptValue( i ), DataTypes.parseStringValue( pieces[ i ] ) );
		}

		return value;
	}

	public static ScriptValue group_string( final Interpreter interpreter, final ScriptVariable string, final ScriptVariable regex )
	{
		Matcher userPatternMatcher =
			Pattern.compile( regex.toStringValue( interpreter ).toString() ).matcher( string.toStringValue( interpreter ).toString() );
		ScriptMap value = new ScriptMap( DataTypes.REGEX_GROUP_TYPE );

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
					slice.aset( groupIndexes[ i ], DataTypes.parseStringValue( userPatternMatcher.group( i ) ) );
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

	public static ScriptValue chat_reply( final Interpreter interpreter, final ScriptVariable stringVariable )
	{
		String recipient = ChatManager.lastBlueMessage();
		if ( !recipient.equals( "" ) )
		{
			String string = stringVariable.toStringValue( interpreter ).toString();
			RequestThread.postRequest( new ChatRequest( recipient, string, false ) );
		}

		return DataTypes.VOID_VALUE;
	}

	// Quest completion functions.

	public static ScriptValue entryway( final Interpreter interpreter )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "entryway" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue hedgemaze( final Interpreter interpreter )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hedgemaze" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue guardians( final Interpreter interpreter )
	{
		int itemId = SorceressLairManager.fightTowerGuardians( true );
		return DataTypes.makeItemValue( itemId );
	}

	public static ScriptValue chamber( final Interpreter interpreter )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "chamber" );
		return RuntimeLibrary.continueValue();
	}

	public static ScriptValue tavern( final Interpreter interpreter )
	{
		int result = StaticEntity.getClient().locateTavernFaucet();
		return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
	}

	// Arithmetic utility functions.

	public static ScriptValue random( final Interpreter interpreter, final ScriptVariable arg )
	{
		int range = arg.intValue( interpreter );
		if ( range < 2 )
		{
			throw new RuntimeException( "Random range must be at least 2" );
		}
		return new ScriptValue( KoLConstants.RNG.nextInt( range ) );
	}

	public static ScriptValue round( final Interpreter interpreter, final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.round( arg.floatValue( interpreter ) ) );
	}

	public static ScriptValue truncate( final Interpreter interpreter, final ScriptVariable arg )
	{
		return new ScriptValue( (int) arg.floatValue( interpreter ) );
	}

	public static ScriptValue floor( final Interpreter interpreter, final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.floor( arg.floatValue( interpreter ) ) );
	}

	public static ScriptValue ceil( final Interpreter interpreter, final ScriptVariable arg )
	{
		return new ScriptValue( (int) Math.ceil( arg.floatValue( interpreter ) ) );
	}

	public static ScriptValue square_root( final Interpreter interpreter, final ScriptVariable val )
	{
		return new ScriptValue( (float) Math.sqrt( val.floatValue( interpreter ) ) );
	}

	// Settings-type functions.

	public static ScriptValue url_encode( final Interpreter interpreter, final ScriptVariable arg )
		throws UnsupportedEncodingException
	{
		return new ScriptValue( URLEncoder.encode( arg.toStringValue( interpreter ).toString(), "UTF-8" ) );
	}

	public static ScriptValue url_decode( final Interpreter interpreter, final ScriptVariable arg )
		throws UnsupportedEncodingException
	{
		return new ScriptValue( URLDecoder.decode( arg.toStringValue( interpreter ).toString(), "UTF-8" ) );
	}

	public static ScriptValue get_property( final Interpreter interpreter, final ScriptVariable name )
	{
		String property = name.toStringValue( interpreter ).toString();
		return !Preferences.isUserEditable( property ) ? DataTypes.STRING_INIT :
			new ScriptValue( Preferences.getString( property ) );
	}

	public static ScriptValue set_property( final Interpreter interpreter, final ScriptVariable name, final ScriptVariable value )
	{
		// In order to avoid code duplication for combat
		// related settings, use the shell.

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
			"set", name.toStringValue( interpreter ).toString() + "=" + value.toStringValue( interpreter ).toString() );
		return DataTypes.VOID_VALUE;
	}

	// Functions for aggregates.

	public static ScriptValue count( final Interpreter interpreter, final ScriptVariable arg )
	{
		return new ScriptValue( arg.getValue( interpreter ).count() );
	}

	public static ScriptValue clear( final Interpreter interpreter, final ScriptVariable arg )
	{
		arg.getValue( interpreter ).clear();
		return DataTypes.VOID_VALUE;
	}

	public static ScriptValue file_to_map( final Interpreter interpreter, final ScriptVariable var1, final ScriptVariable var2 )
	{
		String filename = var1.toStringValue( interpreter ).toString();
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue( interpreter );
		return RuntimeLibrary.readMap( filename, map_variable, true );
	}

	public static ScriptValue file_to_map( final Interpreter interpreter, final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
	{
		String filename = var1.toStringValue( interpreter ).toString();
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue( interpreter );
		boolean compact = var3.intValue( interpreter ) == 1;
		return RuntimeLibrary.readMap( filename, map_variable, compact );
	}

	private static ScriptValue readMap( final String filename, final ScriptCompositeValue result, final boolean compact )
	{
		BufferedReader reader = RuntimeLibrary.getReader( filename );
		if ( reader == null )
		{
			return DataTypes.FALSE_VALUE;
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
			return DataTypes.FALSE_VALUE;
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
		}

		return DataTypes.TRUE_VALUE;
	}

	public static ScriptValue map_to_file( final Interpreter interpreter, final ScriptVariable var1, final ScriptVariable var2 )
	{
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue( interpreter );
		String filename = var2.toStringValue( interpreter ).toString();
		return RuntimeLibrary.printMap( map_variable, filename, true );
	}

	public static ScriptValue map_to_file( final Interpreter interpreter, final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
	{
		ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue( interpreter );
		String filename = var2.toStringValue( interpreter ).toString();
		boolean compact = var3.intValue( interpreter ) == 1;
		return RuntimeLibrary.printMap( map_variable, filename, compact );
	}

	private static ScriptValue printMap( final ScriptCompositeValue map_variable, final String filename,
		final boolean compact )
	{
		if ( filename.startsWith( "http" ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		PrintStream writer = null;
		File output = RuntimeLibrary.getFile( filename );

		if ( output == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		writer = LogStream.openStream( output, true );
		map_variable.dump( writer, "", compact );
		writer.close();

		return DataTypes.TRUE_VALUE;
	}

	// Custom combat helper functions.

	public static ScriptValue my_location( final Interpreter interpreter )
	{
		String location = Preferences.getString( "lastAdventure" );
		return location.equals( "" ) ? DataTypes.parseLocationValue( "Rest" ) : DataTypes.parseLocationValue( location );
	}

	public static ScriptValue get_monsters( final Interpreter interpreter, final ScriptVariable location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue( interpreter );
		AreaCombatData data = adventure.getAreaSummary();

		int monsterCount = data == null ? 0 : data.getMonsterCount();

		ScriptAggregateType type = new ScriptAggregateType( DataTypes.MONSTER_TYPE, monsterCount );
		ScriptArray value = new ScriptArray( type );

		for ( int i = 0; i < monsterCount; ++i )
		{
			value.aset( new ScriptValue( i ), DataTypes.parseMonsterValue( data.getMonster( i ).getName() ) );
		}

		return value;

	}

	public static ScriptValue expected_damage( final Interpreter interpreter )
	{
		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int baseValue =
			Math.max( 0, FightRequest.getMonsterAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

		float damageAbsorb =
			1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
		float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() );
		return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static ScriptValue expected_damage( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int baseValue =
			Math.max( 0, monster.getAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

		float damageAbsorb =
			1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
		float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() );
		return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static ScriptValue monster_level_adjustment( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue weight_adjustment( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getFamiliarWeightAdjustment() );
	}

	public static ScriptValue mana_cost_modifier( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getManaCostAdjustment() );
	}

	public static ScriptValue raw_damage_absorption( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getDamageAbsorption() );
	}

	public static ScriptValue damage_absorption_percent( final Interpreter interpreter )
	{
		int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
		if ( raw == 0 )
		{
			return DataTypes.ZERO_FLOAT_VALUE;
		}

		// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
		// ( sqrt( raw / 10 ) - 1 ) / 10

		double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
		return new ScriptValue( (float) percent );
	}

	public static ScriptValue damage_reduction( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getDamageReduction() );
	}

	public static ScriptValue elemental_resistance( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() ) );
	}

	public static ScriptValue elemental_resistance( final Interpreter interpreter, final ScriptVariable arg )
	{
		if ( arg.getType().equals( DataTypes.TYPE_ELEMENT ) )
		{
			return new ScriptValue( KoLCharacter.getElementalResistance( arg.intValue( interpreter ) ) );
		}

		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new ScriptValue( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
	}

	public static ScriptValue combat_rate_modifier( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getCombatRateAdjustment() );
	}

	public static ScriptValue initiative_modifier( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
	}

	public static ScriptValue experience_bonus( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getExperienceAdjustment() );
	}

	public static ScriptValue meat_drop_modifier( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
	}

	public static ScriptValue item_drop_modifier( final Interpreter interpreter )
	{
		return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
	}

	public static ScriptValue buffed_hit_stat( final Interpreter interpreter )
	{
		int hitStat = KoLCharacter.getAdjustedHitStat();
		return new ScriptValue( hitStat );
	}

	public static ScriptValue current_hit_stat( final Interpreter interpreter )
	{
		return KoLCharacter.hitStat() == KoLConstants.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.MUSCLE_VALUE;
	}

	public static ScriptValue monster_element( final Interpreter interpreter )
	{
		int element = FightRequest.getMonsterDefenseElement();
		return new ScriptValue( DataTypes.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static ScriptValue monster_element( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ELEMENT_INIT;
		}

		int element = monster.getDefenseElement();
		return new ScriptValue( DataTypes.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static ScriptValue monster_attack( final Interpreter interpreter )
	{
		return new ScriptValue( FightRequest.getMonsterAttack() );
	}

	public static ScriptValue monster_attack( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new ScriptValue( monster.getAttack() + KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue monster_defense( final Interpreter interpreter )
	{
		return new ScriptValue( FightRequest.getMonsterDefense() );
	}

	public static ScriptValue monster_defense( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new ScriptValue( monster.getDefense() + KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static ScriptValue monster_hp( final Interpreter interpreter )
	{
		return new ScriptValue( FightRequest.getMonsterHealth() );
	}

	public static ScriptValue monster_hp( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new ScriptValue( monster.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) );
	}

	public static ScriptValue item_drops( final Interpreter interpreter )
	{
		Monster monster = FightRequest.getLastMonster();
		List data = monster == null ? new ArrayList() : monster.getItems();

		ScriptMap value = new ScriptMap( DataTypes.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.parseItemValue( result.getName() ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue item_drops( final Interpreter interpreter, final ScriptVariable arg )
	{
		Monster monster = (Monster) arg.rawValue( interpreter );
		List data = monster == null ? new ArrayList() : monster.getItems();

		ScriptMap value = new ScriptMap( DataTypes.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.parseItemValue( result.getName() ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() ) ) );
		}

		return value;
	}

	public static ScriptValue will_usually_dodge( final Interpreter interpreter )
	{
		return FightRequest.willUsuallyDodge() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue will_usually_miss( final Interpreter interpreter )
	{
		return FightRequest.willUsuallyMiss() ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue numeric_modifier( final Interpreter interpreter, final ScriptVariable modifier )
	{
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( KoLCharacter.currentNumericModifier( mod ) );
	}

	public static ScriptValue numeric_modifier( final Interpreter interpreter, final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue( interpreter ).toString();
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( Modifiers.getNumericModifier( name, mod ) );
	}

	public static ScriptValue boolean_modifier( final Interpreter interpreter, final ScriptVariable modifier )
	{
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( KoLCharacter.currentBooleanModifier( mod ) );
	}

	public static ScriptValue boolean_modifier( final Interpreter interpreter, final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue( interpreter ).toString();
		String mod = modifier.toStringValue( interpreter ).toString();
		return Modifiers.getBooleanModifier( name, mod ) ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static ScriptValue effect_modifier( final Interpreter interpreter, final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue( interpreter ).toString();
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( DataTypes.parseEffectValue( Modifiers.getStringModifier( name, mod ) ) );
	}

	public static ScriptValue class_modifier( final Interpreter interpreter, final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue( interpreter ).toString();
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( DataTypes.parseClassValue( Modifiers.getStringModifier( name, mod ) ) );
	}

	public static ScriptValue stat_modifier( final Interpreter interpreter, final ScriptVariable arg, final ScriptVariable modifier )
	{
		String name = arg.toStringValue( interpreter ).toString();
		String mod = modifier.toStringValue( interpreter ).toString();
		return new ScriptValue( DataTypes.parseStatValue( Modifiers.getStringModifier( name, mod ) ) );
	}
}
