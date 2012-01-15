/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Method;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.Expression;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterExpression;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatMessage;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.chat.WhoMessage;

import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.CraftRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoneyMakingGameManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;

import net.sourceforge.kolmafia.textui.command.ConditionalStatement;

import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class RuntimeLibrary
{
	private static final GenericRequest VISITOR = new GenericRequest( "" );
	private static final RelayRequest RELAYER = new RelayRequest( false );

	private static final RecordType itemDropRec = new RecordType(
		"{item drop; int rate; string type;}",
		new String[] { "drop", "rate", "type" },
		new Type[] { DataTypes.ITEM_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE } );

	public static final FunctionList functions = new FunctionList();

	public static Iterator getFunctions()
	{
		return functions.iterator();
	}

	static
	{
		Type[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_version", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_revision", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path_full", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path_variables", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "batch_open", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "batch_close", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "enable", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "disable", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "user_confirm", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "logprint", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print_html", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "cli_execute", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "load_html", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "write", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "writeln", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "form_field", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "form_fields", new AggregateType(
			DataTypes.STRING_TYPE, DataTypes.STRING_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "make_url", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "wait", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "waitq", DataTypes.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new Type[] { DataTypes.ANY_TYPE };
		functions.add( new LibraryFunction( "to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.ANY_TYPE };
		functions.add( new LibraryFunction( "to_boolean", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ANY_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ANY_TYPE };
		functions.add( new LibraryFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_class", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_stat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_location", DataTypes.LOCATION_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_location", DataTypes.LOCATION_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "to_slot", DataTypes.SLOT_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_slot", DataTypes.SLOT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_coinmaster", DataTypes.COINMASTER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "to_plural", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "to_url", DataTypes.STRING_TYPE, params ) );

		// Functions related to daily information which get
		// updated usually once per day.

		params = new Type[] {};
		functions.add( new LibraryFunction( "today_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "time_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "now_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "format_date_time", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gameday_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gameday_to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gametime_to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "moon_phase", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "moon_light", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stat_bonus_today", DataTypes.STAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stat_bonus_tomorrow", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "session_logs", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "session_logs", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "set_location", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "adventure", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.LOCATION_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "adventure", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "adv1", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "get_ccs_action", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "add_item_condition", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "remove_item_condition", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_goal", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "is_accessible", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "inaccessible_reason", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "visit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sell", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "craft", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "create", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "use", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "eatsilent", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "overdrink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_item_message", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "empty_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "put_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "take_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "take_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_storage", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "autosell", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "hermit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "retrieve_item", DataTypes.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_inventory", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_campground", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_dwelling", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "get_related", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_npc_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_coinmaster_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_tradeable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_giftable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_displayable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "autosell_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "mall_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "npc_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buys_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sells_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sell_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "historical_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "historical_age", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "daily_special", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "refresh_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "available_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "item_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "closet_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equipped_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "creatable_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "get_ingredients", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "storage_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "display_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "shop_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "stash_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "pulls_remaining", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stills_available", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_mushroom_plot", DataTypes.BOOLEAN_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new Type[] {};
		functions.add( new LibraryFunction( "refresh_status", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "restore_hp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "restore_mp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_name", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_id", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_hash", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_sign", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_path", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_muscle_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_mysticality_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_moxie_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_bad_moon", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_class", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_level", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_maxhp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_mp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_maxmp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_primestat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.STAT_TYPE };
		functions.add( new LibraryFunction( "my_basestat", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STAT_TYPE };
		functions.add( new LibraryFunction( "my_buffedstat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_closet_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_adventures", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_daycount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_turncount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_fullness", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "fullness_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_inebriety", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "inebriety_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_spleen_use", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "spleen_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "turns_played", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_ascensions", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_interact", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_hardcore", DataTypes.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "have_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "mp_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "turns_per_cast", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "have_effect", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_skill_message", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_auto_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "set_auto_attack", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "attack", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "steal", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "runaway", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "throw_item", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "throw_items", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "run_combat", DataTypes.BUFFER_TYPE, params ) );

		// Equipment functions.

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "can_equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SLOT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SLOT_TYPE };
		functions.add( new LibraryFunction( "equipped_item", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "have_equipped", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "have_outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "outfit_pieces", new AggregateType(
			DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_effective_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_enthroned_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "have_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "use_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "enthrone_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_equipment", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_equipped_equipment", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_weight", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "weapon_hands", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "item_type", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "weapon_type", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "get_power", DataTypes.INT_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new Type[] {};
		functions.add( new LibraryFunction( "council", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_mcd", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "change_mcd", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_chef", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_bartender", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "get_counters", DataTypes.STRING_TYPE, params ) );

		// String parsing functions.

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_integer", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "contains_text", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "extract_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "extract_items", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "length", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "char_at", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_lower_case", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_upper_case", DataTypes.STRING_TYPE, params ) );

		// String buffer functions

		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "append", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "insert", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace", DataTypes.BOOLEAN_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "delete", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "set_length", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE };
		functions.add( new LibraryFunction( "append_tail", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "append_replacement", DataTypes.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "create_matcher", DataTypes.MATCHER_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "find", DataTypes.BOOLEAN_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "start", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "start", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "end", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "end", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "group", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "group", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "group_count", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_first", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_all", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "reset", DataTypes.MATCHER_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "reset", DataTypes.MATCHER_TYPE, params ) );

		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "split_string", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "split_string", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "group_string", DataTypes.REGEX_GROUP_TYPE, params ) );

		// Assorted functions
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "expression_eval", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "modifier_eval", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "maximize", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "maximize", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "monster_eval", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_online", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_macro", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_clan", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_clan", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_private", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_notify", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "who_clan", new AggregateType(
			DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ), params ) );

		// Quest handling functions.

		params = new Type[] {};
		functions.add( new LibraryFunction( "entryway", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "hedgemaze", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "guardians", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "chamber", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "tavern", DataTypes.INT_TYPE, params ) );

		// Arithmetic utility functions.

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "random", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "round", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "truncate", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "floor", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "ceil", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "square_root", DataTypes.FLOAT_TYPE, params ) );

		// Versions of min and max that return int (if both arguments
		// are int) or float (if at least one arg is a float)
		//
		// The float versions must come first.

		params = new Type[] { DataTypes.FLOAT_TYPE, DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "min", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "min", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE, DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "max", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "max", DataTypes.INT_TYPE, params ) );

		// Settings-type functions.

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "url_encode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "url_decode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "entity_encode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "entity_decode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "get_property", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "set_property", DataTypes.VOID_TYPE, params ) );

		// Functions for aggregates.

		params = new Type[] { DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "count", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "clear", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		// Custom combat helper functions.

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_location", DataTypes.LOCATION_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "get_monsters", new AggregateType(
			DataTypes.MONSTER_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "appearance_rates", new AggregateType(
			DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_level_adjustment", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "weight_adjustment", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mana_cost_modifier", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "combat_mana_cost_modifier", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "raw_damage_absorption", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "damage_absorption_percent", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "damage_reduction", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ELEMENT_TYPE };
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "combat_rate_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "initiative_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "experience_bonus", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "meat_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "buffed_hit_stat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_hit_stat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drops", DataTypes.RESULT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "item_drops", DataTypes.RESULT_TYPE, params ) );

		Type itemDropRecArray = new AggregateType( itemDropRec, 0 );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drops_array", itemDropRecArray, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "item_drops_array", itemDropRecArray, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "meat_drop", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "meat_drop", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "will_usually_miss", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "will_usually_dodge", DataTypes.BOOLEAN_TYPE, params ) );

		// Modifier introspection

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "string_modifier", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "string_modifier", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "effect_modifier", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "effect_modifier", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "class_modifier", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "class_modifier", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "stat_modifier", DataTypes.STAT_TYPE, params ) );

		// Quest status inquiries

		params = new Type[] {};
		functions.add( new LibraryFunction( "galaktik_cures_discounted", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "white_citadel_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "friars_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "black_market_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "hippy_store_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "dispensary_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "guild_store_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "hidden_temple_unlocked", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "knoll_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "canadia_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gnomads_available", DataTypes.BOOLEAN_TYPE, params ) );

		// Path Support

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		// MMG support

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_visit", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_search", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "mmg_make_bet", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_retract_bet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "mmg_take_bet", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_my_bets", new AggregateType( DataTypes.INT_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_offered_bets", new AggregateType( DataTypes.INT_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_owner", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_owner_id", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_wait_event", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_taker", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_taker_id", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_winnings", DataTypes.INT_TYPE, params ) );
	}

	public static Method findMethod( final String name, final Class[] args )
		throws NoSuchMethodException
	{
		return RuntimeLibrary.class.getMethod( name, args );
	}

	private static Value continueValue()
	{
		boolean continueValue = Interpreter.getContinueValue();

		Interpreter.forgetPendingState();

		return DataTypes.makeBooleanValue( continueValue );
	}

	// Support for batching of server requests

	private static void batchCommand( String cmd, String params )
	{
		RuntimeLibrary.batchCommand( cmd, null, params );
	}

	private static void batchCommand( String cmd, String prefix, String params )
	{
		LinkedHashMap batched = LibraryFunction.interpreter.batched;
		if ( batched == null )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( cmd,
				prefix == null ? params : (prefix + " " + params) );
			return;
		}
		StringBuffer buf = (StringBuffer) batched.get( cmd );
		if ( buf == null )
		{	// First instance of this command
			buf = new StringBuffer();
			if ( prefix != null )
			{
				buf.append( prefix );
				buf.append( " " );
			}
			buf.append( params );
			batched.put( cmd, buf );
		}
		else if ( prefix != null &&
			!buf.substring( 0, prefix.length() ).equals( prefix ) )
		{	// Have seen this command, but with a different subcommand -
			// can't queue it.
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( cmd, prefix + " " + params );
			return;
		}
		else
		{	// Have seen this command, and prefix (if any) matches
			buf.append( ", " );
			buf.append( params );
		}
	}

	public static Value get_version()
	{
		return new Value( KoLConstants.VERSION_NAME );
	}

	public static Value get_revision()
	{
		return new Value( StaticEntity.getRevision() );
	}

	public static Value get_path()
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.STRING_INIT;
		}
		return new Value( KoLmafiaASH.relayRequest.getBasePath() );
	}

	public static Value get_path_full()
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.STRING_INIT;
		}
		return new Value( KoLmafiaASH.relayRequest.getPath() );
	}

	public static Value get_path_variables()
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.STRING_INIT;
		}
		String value = KoLmafiaASH.relayRequest.getPath();
		int quest = value.indexOf( "?" );
		return quest == -1 ? DataTypes.STRING_INIT : new Value( value.substring( 1, quest ) );
	}

	public static Value batch_open()
	{
		if ( LibraryFunction.interpreter.batched == null )
		{
			LibraryFunction.interpreter.batched = new LinkedHashMap();
		}
		return DataTypes.VOID_VALUE;
	}

	public static Value batch_close()
	{
		Interpreter terp = LibraryFunction.interpreter;
		LinkedHashMap batched = terp.batched;
		if ( batched != null )
		{
			Iterator i = batched.entrySet().iterator();
			while ( i.hasNext() )
			{
				Map.Entry e = (Map.Entry) i.next();
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( (String) e.getKey(),
					((StringBuffer) e.getValue()).toString() );
				if ( !KoLmafia.permitsContinue() ) break;
			}
			terp.batched = null;
		}

		return RuntimeLibrary.continueValue();
	}
	// Basic utility functions which print information
	// or allow for easy testing.

	public static Value enable( final Value name )
	{
		StaticEntity.enable( name.toString().toLowerCase() );
		return DataTypes.VOID_VALUE;
	}

	public static Value disable( final Value name )
	{
		StaticEntity.disable( name.toString().toLowerCase() );
		return DataTypes.VOID_VALUE;
	}

	public static Value user_confirm( final Value message )
	{
		return DataTypes.makeBooleanValue( InputFieldUtilities.confirm( message.toString() ) );
	}

	public static Value logprint( final Value string )
	{
		String parameters = string.toString();

		parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.getSessionStream().println( "> " + parameters );
		return DataTypes.VOID_VALUE;
	}

	public static Value print( final Value string )
	{
		String parameters = string.toString();

		parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.printLine( parameters );
		RequestLogger.getSessionStream().println( "> " + parameters );

		return DataTypes.VOID_VALUE;
	}

	public static Value print( final Value string, final Value color )
	{
		String parameters = string.toString();

		parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		String colorString = color.toString();
		colorString = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( colorString, "\"" ), "<" );

		RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );
		RequestLogger.getSessionStream().println( " > " + parameters );

		return DataTypes.VOID_VALUE;
	}

	public static Value print_html( final Value string )
	{
		RequestLogger.printLine( string.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value abort()
	{
		RequestThread.declareWorldPeace();
		return DataTypes.VOID_VALUE;
	}

	public static Value abort( final Value string )
	{
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, string.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value cli_execute( final Value string )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value load_html( final Value string )
	{
		StringBuffer buffer = new StringBuffer();
		Value returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );

		String location = string.toString();
		if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
		{
			return returnValue;
		}

		byte[] bytes = DataFileCache.getBytes( location );
		buffer.append( new String( bytes ) );
		return returnValue;
	}

	public static Value write( final Value string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.VOID_VALUE;
		}

		KoLmafiaASH.serverReplyBuffer.append( string.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value writeln( final Value string )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.VOID_VALUE;
		}

		RuntimeLibrary.write( string );
		KoLmafiaASH.serverReplyBuffer.append( KoLConstants.LINE_BREAK );
		return DataTypes.VOID_VALUE;
	}

	public static Value form_field( final Value key )
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return DataTypes.STRING_INIT;
		}

		String value = KoLmafiaASH.relayRequest.getFormField( key.toString() );
		return value == null ? DataTypes.STRING_INIT : new Value( value );
	}

	public static Value form_fields()
	{
		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, DataTypes.STRING_TYPE );
		MapValue value = new MapValue( type );
		if ( KoLmafiaASH.relayScript == null )
		{
			return value;
		}

		Iterator i = KoLmafiaASH.relayRequest.getFormFields().iterator();
		while ( i.hasNext() )
		{
			String field = (String) i.next();
			String[] pieces = field.split( "=", 2 );
			String name = pieces[ 0 ];
			Value keyval = DataTypes.STRING_INIT;
			if ( pieces.length > 1 )
			{
				try
				{
					String decoded = URLDecoder.decode( pieces[ 1 ], "UTF-8" );
					keyval = new Value( decoded );
				}
				catch ( IOException e )
				{
				}
			}
			Value keyname = new Value( name );
			while ( value.contains( keyname ) )
			{	// Make a unique name for duplicate fields
				name = name + "_";
				keyname = new Value( name );
			}
			value.aset( keyname, keyval );
		}
		return value;
	}

	public static Value visit_url()
	{
		if ( KoLmafiaASH.relayScript == null )
		{
			return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer() );
		}

		RequestThread.postRequest( KoLmafiaASH.relayRequest );

		StringBuffer buffer = new StringBuffer();
		if ( KoLmafiaASH.relayRequest.responseText != null )
		{
			buffer.append( KoLmafiaASH.relayRequest.responseText );
		}
		return new Value( DataTypes.BUFFER_TYPE, "", buffer );
	}

	public static Value visit_url( final Value string )
	{
		return RuntimeLibrary.visit_url( string.toString(), true, false );
	}

	public static Value visit_url( final Value string, final Value usePostMethod )
	{
		return RuntimeLibrary.visit_url( string.toString(), usePostMethod.intValue() == 1, false );
	}

	public static Value visit_url( final Value string, final Value usePostMethod, final Value encoded )
	{
		return RuntimeLibrary.visit_url( string.toString(), usePostMethod.intValue() == 1, encoded.intValue() == 1 );
	}

	private static Value visit_url( final String location )
	{
		return RuntimeLibrary.visit_url( location, true, false );
	}

	private static Value visit_url( final String location, final boolean usePostMethod, final boolean encoded )
	{
		StringBuffer buffer = new StringBuffer();
		Value returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );

		RuntimeLibrary.VISITOR.constructURLString( location, usePostMethod, encoded );
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
				ResponseTextParser.externalUpdate( location, RuntimeLibrary.VISITOR.responseText );
			}
		}
		else
		{
			RequestThread.postRequest( RuntimeLibrary.RELAYER.constructURLString( location, usePostMethod, encoded ) );
			if ( RuntimeLibrary.RELAYER.responseText != null )
			{
				buffer.append( RuntimeLibrary.RELAYER.responseText );
			}
		}

		return returnValue;
	}

	public static Value make_url( final Value arg1, final Value arg2, final Value arg3 )
	{
		String location = arg1.toString();
		boolean usePostMethod = arg2.intValue() == 1;
		boolean encoded = arg3.intValue() == 1;
		RuntimeLibrary.VISITOR.constructURLString( location, usePostMethod, encoded );
		return new Value( RuntimeLibrary.VISITOR.getURLString() );
	}

	public static Value wait( final Value delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "wait", delay.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value waitq( final Value delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "waitq", delay.toString() );
		return DataTypes.VOID_VALUE;
	}

	// Type conversion functions which allow conversion
	// of one data format to another.

	public static Value to_string( Value val )
	{
		// This function previously just returned val, except in the
		// case of buffers in which case it's necessary to capture the
		// current string value of the buffer.	That works fine in most
		// cases, but NOT if the value ever gets used as a key in a
		// map; having a key that's actually an int (for example) in a
		// string map causes the map ordering to become inconsistent,
		// because int Values compare differently than string Values.
		return val.toStringValue();
	}

	public static Value to_boolean( final Value value )
	{
		return DataTypes.makeBooleanValue( ( value.intValue() != 0 || value.toString().equalsIgnoreCase( "true" ) ) );
	}

	public static Value to_int( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			String string = value.toString();
			try
			{
				return new Value( StringUtilities.parseIntInternal1( string, true ) );
			}
			catch ( NumberFormatException e )
			{
			}

			// Try again with lax parsing

			try
			{
				int retval = StringUtilities.parseIntInternal2( string );
				Exception ex = LibraryFunction.interpreter.runtimeException( "The string \"" + string + "\" is not an integer; returning " + retval );
				RequestLogger.printLine( ex.getMessage() );
				return new Value( retval );
			}
			catch ( NumberFormatException e )
			{
				// Even with lax parsing, we failed.
				Exception ex = LibraryFunction.interpreter.runtimeException( "The string \"" + string + "\" does not look like an integer; returning 0" );
				RequestLogger.printLine( ex.getMessage() );
				return DataTypes.ZERO_VALUE;
			}
		}

		return new Value( value.intValue() );
	}

	public static Value to_float( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			String string = value.toString();
			try
			{
				return new Value( StringUtilities.parseFloat( string ) );
			}
			catch ( NumberFormatException e )
			{
				Exception ex = LibraryFunction.interpreter.runtimeException( "The string \"" + string + "\" is not a float; returning 0.0" );
				RequestLogger.printLine( ex.getMessage() );
				return DataTypes.ZERO_FLOAT_VALUE;
			}
		}

		if ( value.intValue() != 0 )
		{
			return new Value( (float) value.intValue() );
		}

		return new Value( value.floatValue() );
	}

	public static Value to_item( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeItemValue( value.intValue() );
		}

		return DataTypes.parseItemValue( value.toString(), true );
	}

	public static Value to_item( final Value name, final Value count )
	{
		return DataTypes.makeItemValue( ItemDatabase.getItemId(
			name.toString(), count.intValue() ) );
	}

	public static Value to_class( final Value value )
	{
		return DataTypes.parseClassValue( value.toString(), true );
	}

	public static Value to_stat( final Value value )
	{
		return DataTypes.parseStatValue( value.toString(), true );
	}

	public static Value to_skill( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeSkillValue( value.intValue() );
		}

		if ( value.getType().equals( DataTypes.TYPE_EFFECT ) )
		{
			return DataTypes.parseSkillValue( UneffectRequest.effectToSkill( value.toString() ), true );
		}

		return DataTypes.parseSkillValue( value.toString(), true );
	}

	public static Value to_effect( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeEffectValue( value.intValue() );
		}

		if ( value.getType().equals( DataTypes.TYPE_SKILL ) )
		{
			return DataTypes.parseEffectValue( UneffectRequest.skillToEffect( value.toString() ), true );
		}

		return DataTypes.parseEffectValue( value.toString(), true );
	}

	public static Value to_location( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.parseLocationValue( value.intValue(), true );
		}
		else
		{
			return DataTypes.parseLocationValue( value.toString(), true );
		}
	}

	public static Value to_familiar( final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeFamiliarValue( value.intValue() );
		}

		return DataTypes.parseFamiliarValue( value.toString(), true );
	}

	public static Value to_monster( final Value value )
	{
		return DataTypes.parseMonsterValue( value.toString(), true );
	}

	public static Value to_slot( final Value item )
	{
		if ( !item.getType().equals( DataTypes.TYPE_ITEM ) )
		{
			return DataTypes.parseSlotValue( item.toString(), true );
		}
		switch ( ItemDatabase.getConsumptionType( item.intValue() ) )
		{
		case KoLConstants.EQUIP_HAT:
			return DataTypes.parseSlotValue( "hat", true );
		case KoLConstants.EQUIP_WEAPON:
			return DataTypes.parseSlotValue( "weapon", true );
		case KoLConstants.EQUIP_OFFHAND:
			return DataTypes.parseSlotValue( "off-hand", true );
		case KoLConstants.EQUIP_SHIRT:
			return DataTypes.parseSlotValue( "shirt", true );
		case KoLConstants.EQUIP_PANTS:
			return DataTypes.parseSlotValue( "pants", true );
		case KoLConstants.EQUIP_FAMILIAR:
			return DataTypes.parseSlotValue( "familiar", true );
		case KoLConstants.EQUIP_ACCESSORY:
			return DataTypes.parseSlotValue( "acc1", true );
		default:
			return DataTypes.parseSlotValue( "none", true );
		}
	}

	public static Value to_element( final Value value )
	{
		return DataTypes.parseElementValue( value.toString(), true );
	}

	public static Value to_coinmaster( final Value value )
	{
		return DataTypes.parseCoinmasterValue( value.toString(), true );
	}

	public static Value to_phylum( final Value value )
	{
		return DataTypes.parsePhylumValue( value.toString(), true );
	}

	public static Value to_plural( final Value item ) {
		return new Value( ItemDatabase.getPluralName( item.intValue() ) );
	}

	public static Value to_url( final Value value )
	{
		KoLAdventure adventure = (KoLAdventure) value.rawValue();
		return new Value( adventure.getRequest().getURLString() );
	}

	// Functions related to daily information which get
	// updated usually once per day.

	public static Value today_to_string()
	{
		return new Value( KoLConstants.DAILY_FORMAT.format( new Date() ) );
	}

	public static Value time_to_string()
	{
		Calendar timestamp = new GregorianCalendar();
		return new Value( KoLConstants.TIME_FORMAT.format( timestamp.getTime() ) );
	}

	public static Value now_to_string( Value dateFormatValue )
	{
		Calendar timestamp = new GregorianCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat( dateFormatValue.toString() );
		return new Value( dateFormat.format( timestamp.getTime() ) );
	}

	public static Value format_date_time( Value inFormat, Value dateTimeString, Value outFormat )
	{
		Date inDate = null;
		SimpleDateFormat dateFormat = null;
		Value retVal = null;

		try
		{
			dateFormat = new SimpleDateFormat( inFormat.toString() );
			inDate = dateFormat.parse( dateTimeString.toString() );
			dateFormat= new SimpleDateFormat( outFormat.toString() );
			retVal = new Value( dateFormat.format( inDate ) );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			retVal = new Value( "Bad parameter(s) passed to format_date_time" );
		}
		return retVal;
	}

	public static Value gameday_to_string()
	{
		return new Value( HolidayDatabase.getCalendarDayAsString( HolidayDatabase.getCalendarDay( new Date() ) ) );
	}

	public static Value gameday_to_int()
	{
		return new Value( HolidayDatabase.getCalendarDay( new Date() ) );
	}

	public static Value gametime_to_int()
	{
		return new Value( HolidayDatabase.getTimeDifference( new Date() ) );
	}

	public static Value moon_phase()
	{
		return new Value( HolidayDatabase.getPhaseStep() );
	}

	public static Value moon_light()
	{
		return new Value( HolidayDatabase.getMoonlight() );
	}

	public static Value stat_bonus_today()
	{
		if ( ConditionalStatement.test( "today is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( ConditionalStatement.test( "today is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( ConditionalStatement.test( "today is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static Value stat_bonus_tomorrow()
	{
		if ( ConditionalStatement.test( "tomorrow is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( ConditionalStatement.test( "tomorrow is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( ConditionalStatement.test( "tomorrow is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static Value session_logs( final Value dayCount )
	{
		return RuntimeLibrary.getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue() );
	}

	public static Value session_logs( final Value player, final Value dayCount )
	{
		return RuntimeLibrary.getSessionLogs( player.toString(), dayCount.intValue() );
	}

	private static Value getSessionLogs( final String name, final int dayCount )
	{
		String[] files = new String[ dayCount ];

		Calendar timestamp = Calendar.getInstance( TimeZone.getTimeZone("GMT-0330") );

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, files.length );
		ArrayValue value = new ArrayValue( type );

		StringBuffer contents = new StringBuffer();

		for ( int i = 0; i < files.length; ++i )
		{
			String filename =
				StringUtilities.globalStringReplace( name, " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( timestamp.getTime() ) + ".txt";

			File path = new File( KoLConstants.SESSIONS_LOCATION, filename );
			BufferedReader reader = FileUtilities.getReader( path );
			timestamp.add( Calendar.DATE, -1 );

			if ( reader == null )
			{
				continue;
			}

			try
			{
				contents.setLength( 0 );
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

			value.aset( new Value( i ), new Value( contents.toString() ) );
		}

		return value;
	}

	// Major functions related to adventuring and
	// item management.

	public static Value adventure( final Value countValue, final Value locationValue )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "adventure", count + " " + locationValue );
		return RuntimeLibrary.continueValue();
	}

	public static Value adventure( final Value countValue, final Value locationValue, final Value filterFunction )
	{
		try
		{
			String filter = filterFunction.toString();
			Macrofier.setMacroOverride( filter );

			return RuntimeLibrary.adventure( countValue, locationValue );
		}
		finally
		{
			Macrofier.setMacroOverride( null );
		}
	}

	public static Value adv1( final Value locationValue, final Value adventuresUsedValue, final Value filterFunction )
	{
		KoLAdventure adventure = (KoLAdventure) locationValue.rawValue();

		if ( adventure == null )
		{
			return RuntimeLibrary.continueValue();
		}

		try
		{
			adventure.overrideAdventuresUsed( adventuresUsedValue.intValue() );

			String filter = filterFunction.toString();
			Macrofier.setMacroOverride( filter );

			KoLmafia.redoSkippedAdventures = false;

			StaticEntity.getClient().makeRequest( adventure, 1 );
		}
		finally
		{
			KoLmafia.redoSkippedAdventures = true;
			Macrofier.setMacroOverride( null );

			adventure.overrideAdventuresUsed( -1 );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value get_ccs_action( final Value index )
	{
		return new Value(
			CombatActionManager.getCombatAction(
				FightRequest.getCurrentKey(), index.intValue(), true ) );
	}

	public static Value add_item_condition( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.VOID_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "conditions", "add " + count + " \u00B6" + item.intValue() );
		return DataTypes.VOID_VALUE;
	}

	public static Value remove_item_condition( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.VOID_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "conditions", "remove " + count + " \u00B6" + item.intValue() );
		return DataTypes.VOID_VALUE;
	}

	public static Value is_goal( final Value item )
	{
		return DataTypes.makeBooleanValue( GoalManager.hasItemGoal( item.intValue() ) );
	}

	public static Value buy( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		AdventureResult itemToBuy = new AdventureResult( item.intValue(), 1 );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", count + " \u00B6" + item.intValue() );
		return DataTypes.makeBooleanValue( initialAmount + count == itemToBuy.getCount( KoLConstants.inventory ) );
	}

	public static Value buy( final Value arg1, final Value arg2, final Value arg3 )
	{
		if ( arg1.getType().equals( DataTypes.TYPE_COINMASTER ) )
		{
			return coinmaster_buy( arg1, arg2, arg3 );
		}

		int count = arg1.intValue();
		if ( count <= 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		int itemId = arg2.intValue();
		AdventureResult itemToBuy = new AdventureResult( itemId, 1 );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", count + " \u00B6"
			+ itemId + "@" + arg3.intValue() );
		return new Value( itemToBuy.getCount( KoLConstants.inventory ) - initialAmount );
	}

	// Coinmaster functions

	public static Value is_accessible( final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeBooleanValue( data != null && CoinMasterRequest.accessible( data ) == null );
	}

	public static Value inaccessible_reason( final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String reason = data != null ? CoinMasterRequest.accessible( data ) : null;
		return new Value( reason != null ? reason : "" );
	}

	public static Value visit( final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		CoinMasterRequest.visit( data );
		return RuntimeLibrary.continueValue();
	}

	private static Value coinmaster_buy( final Value master, final Value countValue, final Value itemValue )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}
		CoinmasterData data = (CoinmasterData) master.rawValue();
		AdventureResult item = new AdventureResult( itemValue.intValue(), count );
		int initialAmount = item.getCount( KoLConstants.inventory );
		CoinMasterRequest.buy( data, item );
		return DataTypes.makeBooleanValue( initialAmount + count == item.getCount( KoLConstants.inventory ) );
	}

	public static Value sell( final Value master, final Value countValue, final Value itemValue )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}
		AdventureResult item = new AdventureResult( itemValue.intValue(), count );
		CoinMasterRequest.sell( data, item );
		return RuntimeLibrary.continueValue();
	}

	public static Value craft( final Value modeValue, final Value countValue, final Value item1, final Value item2 )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		String mode = modeValue.toString();
		int id1 = item1.intValue();
		int id2 = item2.intValue();

		CraftRequest req = new CraftRequest( mode, count, id1, id2 );
		RequestThread.postRequest( req );
		return new Value( req.created() );
	}

	public static Value create( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "create", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value use( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", count + " \u00B6" + item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value eat( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "eat", count + " \u00B6" + item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value eatsilent( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "eatsilent", count + " \u00B6" + item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value drink( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "drink", count + " \u00B6" + item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value overdrink( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "overdrink", count + " \u00B6" + item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value last_item_message()
	{
		return new Value( UseItemRequest.lastUpdate );
	}

	public static Value empty_closet()
	{
		RuntimeLibrary.batchCommand( "closet", "empty" );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_closet( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "closet", "put", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_closet( final Value meatValue )
	{
		int meat = meatValue.intValue();
		if ( meat <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "closet", "put " + meat + " meat" );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_shop( final Value priceValue, final Value limitValue, final Value item )
	{
		int price = priceValue.intValue();
		int limit = limitValue.intValue();

		RuntimeLibrary.batchCommand( "shop", "put", "* \u00B6" + item.intValue() + " @ " + price + " limit " + limit );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_shop( final Value priceValue, final Value limitValue, final Value qtyValue, final Value item )
	{
		int price = priceValue.intValue();
		int limit = limitValue.intValue();
		int qty = qtyValue.intValue();
		if ( qty <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "shop", "put", qty + " \u00B6" + item.intValue() + " @ " + price + " limit " + limit );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_stash( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "stash", "put", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value put_display( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "display", "put", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_closet( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "closet", "take", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_closet( final Value meatValue )
	{
		int meat = meatValue.intValue();
		if ( meat <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "closet", "take " + meat + " meat" );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_shop( final Value item )
	{
		return RuntimeLibrary.take_shop( item, DataTypes.TRUE_VALUE );
	}

	public static Value take_shop( final Value item, final Value takeAll )
	{
		RuntimeLibrary.batchCommand( "shop", "take", ( takeAll.intValue() == 1 ? "all " : "" ) + "\u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_storage( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "hagnk", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_display( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "display", "take", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value take_stash( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "stash", "take", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value autosell( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		RuntimeLibrary.batchCommand( "sell", count + " \u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value hermit( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "hermit", count + " " + ItemDatabase.getItemName( item.intValue() ) );
		return RuntimeLibrary.continueValue();
	}

	public static Value retrieve_item( final Value countValue, final Value item )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		return DataTypes.makeBooleanValue( InventoryManager.retrieveItem( new AdventureResult( item.intValue(), count ) ) );
	}

	// Major functions which provide item-related
	// information.

	public static Value get_inventory()
	{
		MapValue value = new MapValue( DataTypes.RESULT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.inventory.size() ];
		KoLConstants.inventory.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.parseItemValue( items[i].getName(), true ),
				DataTypes.parseIntValue( String.valueOf( items[i].getCount() ), true ) );
		}

		return value;
	}

	public static Value get_campground()
	{
		MapValue value = new MapValue( DataTypes.RESULT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.campground.size() ];
		KoLConstants.campground.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.parseItemValue( items[i].getName(), true ),
				DataTypes.parseIntValue( String.valueOf( items[i].getCount() ), true ) );
		}

		return value;
	}

	public static Value get_dwelling()
	{
		return DataTypes.makeItemValue( CampgroundRequest.getCurrentDwelling().getItemId() );
	}

	private static int WAD2POWDER = -12;	// <elem> powder - <elem> wad
	private static int WAD2NUGGET = -6;
	private static int WAD2GEM = 1321;

	public static Value get_related( Value item, Value type )
	{
		MapValue value = new MapValue( DataTypes.RESULT_TYPE );
		String which = type.toString();

		if ( which.equals( "zap" ) )
		{
			String[] zapgroup = ZapRequest.getZapGroup( item.intValue() );
			for ( int i = zapgroup.length - 1; i >= 0; --i )
			{
				Value key = DataTypes.parseItemValue( zapgroup[ i ], true );
				if ( key.intValue() != item.intValue() )
				{
					value.aset( key, DataTypes.ZERO_VALUE );
				}
			}
		}
		else if ( which.equals( "fold" ) )
		{
			ArrayList list = ItemDatabase.getFoldGroup( item.toString() );
			if ( list == null ) return value;
			// Don't use element 0, that's the damage percentage
			for ( int i = list.size() - 1; i > 0; --i )
			{
				value.aset(
					DataTypes.parseItemValue( (String) list.get( i ), true ),
					new Value( i ) );
			}
		}
		else if ( which.equals( "pulverize" ) )
		{	// All values scaled up by one million
			int pulver = EquipmentDatabase.getPulverization( item.intValue() );
			if ( pulver == -1 || (pulver & EquipmentDatabase.MALUS_UPGRADE) != 0 )
			{
				return value;
			}
			if ( pulver > 0 )
			{
				value.aset( DataTypes.makeItemValue( pulver ),
					DataTypes.makeIntValue( 1000000 ) );
				return value;
			}

			ArrayList elems = new ArrayList();
			if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
			{
				elems.add( new Integer( ItemPool.HOT_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
			{
				elems.add( new Integer( ItemPool.COLD_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
			{
				elems.add( new Integer( ItemPool.STENCH_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
			{
				elems.add( new Integer( ItemPool.SPOOKY_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
			{
				elems.add( new Integer( ItemPool.SLEAZE_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
			{	// Important: twinkly must be last
				elems.add( new Integer( ItemPool.TWINKLY_WAD ) );
			}
			int nelems = elems.size();
			if ( nelems == 0 )
			{
				return value;	// shouldn't happen
			}

			int powders = 0, nuggets = 0, wads = 0;
			if ( (pulver & EquipmentDatabase.YIELD_3W) != 0 )
			{
				wads = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1W3N_2W) != 0 )
			{
				wads = 1500000;
				nuggets = 1500000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_4N_1W) != 0 )
			{
				wads = 500000;
				nuggets = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_3N) != 0 )
			{
				nuggets = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1N3P_2N) != 0 )
			{
				nuggets = 1500000;
				powders = 1500000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_4P_1N) != 0 )
			{
				nuggets = 500000;
				powders = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_3P) != 0 )
			{
				powders = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_2P) != 0 )
			{
				powders = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1P) != 0 )
			{
				powders = 1000000;
			}
			int gems = wads / 100;
			wads -= gems;

		 	Iterator i = elems.iterator();
		 	while ( i.hasNext() )
		 	{
		 		int wad = ((Integer) i.next()).intValue();
		 		if ( powders > 0 )
		 		{
					value.aset( DataTypes.makeItemValue( wad + WAD2POWDER ),
						DataTypes.makeIntValue( powders / nelems ) );
		 		}
		 		if ( nuggets > 0 )
		 		{
					value.aset( DataTypes.makeItemValue( wad + WAD2NUGGET ),
						DataTypes.makeIntValue( nuggets / nelems ) );
		 		}
		 		if ( wads > 0 )
		 		{
		 			if ( wad == ItemPool.TWINKLY_WAD )
		 			{	// no twinkly gem!
		 				wads += gems;
		 				gems = 0;
		 			}
					value.aset( DataTypes.makeItemValue( wad ),
						DataTypes.makeIntValue( wads / nelems ) );
		 		}
		 		if ( gems > 0 )
		 		{
					value.aset( DataTypes.makeItemValue( wad + WAD2GEM ),
						DataTypes.makeIntValue( gems / nelems ) );
		 		}
		 	}
		}
		return value;
	}

	public static Value is_tradeable( final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isTradeable( item.intValue() ) );
	}

	public static Value is_giftable( final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isGiftable( item.intValue() ) );
	}

	public static Value is_displayable( final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isDisplayable( item.intValue() ) );
	}

	public static Value is_npc_item( final Value item )
	{
		return DataTypes.makeBooleanValue( NPCStoreDatabase.contains( ItemDatabase.getItemName( item.intValue() ), false ) );
	}

	public static Value is_coinmaster_item( final Value item )
	{
		return DataTypes.makeBooleanValue( CoinmastersDatabase.contains( ItemDatabase.getItemName( item.intValue() ), false ) );
	}

	public static Value autosell_price( final Value item )
	{
		return new Value( ItemDatabase.getPriceById( item.intValue() ) );
	}

	public static Value mall_price( final Value item )
	{
		return new Value( StoreManager.getMallPrice(
			new AdventureResult( item.intValue(), 0 ) ) );
	}

	public static Value npc_price( final Value item )
	{
		String it = ItemDatabase.getItemName( item.intValue() );
		return new Value(
			NPCStoreDatabase.contains( it, true ) ?
			NPCStoreDatabase.price( it ) : 0 );
	}

	// Coinmaster functions

	public static Value buys_item( final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String itemName = ItemDatabase.getItemName( item.intValue() );
		return DataTypes.makeBooleanValue( data != null && data.canSellItem( itemName ) );
	}

	public static Value buy_price( final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String itemName = ItemDatabase.getItemName( item.intValue() );
		return DataTypes.makeIntValue( data != null ? data.getSellPrice( itemName ) : 0 );
	}

	public static Value sells_item( final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String itemName = ItemDatabase.getItemName( item.intValue() );
		return DataTypes.makeBooleanValue( data != null && data.canBuyItem( itemName ) );
	}

	public static Value sell_price( final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String itemName = ItemDatabase.getItemName( item.intValue() );
		return DataTypes.makeIntValue( data != null ? data.getBuyPrice( itemName ) : 0 );
	}

	public static Value historical_price( final Value item )
	{
		return new Value( MallPriceDatabase.getPrice( item.intValue() ) );
	}

	public static Value historical_age( final Value item )
	{
		return new Value( MallPriceDatabase.getAge( item.intValue() ) );
	}

	public static Value daily_special()
	{
		AdventureResult special =
			KoLCharacter.gnomadsAvailable() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.canadiaAvailable() ? ChezSnooteeRequest.getDailySpecial() : null;

		return special == null ? DataTypes.ITEM_INIT : DataTypes.parseItemValue( special.getName(), true );
	}

	public static Value refresh_stash()
	{
		RequestThread.postRequest( new ClanStashRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static Value available_amount( final Value arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return DataTypes.makeIntValue( InventoryManager.getAccessibleCount( item ) );
	}

	public static Value item_amount( final Value arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.inventory ) );
	}

	public static Value closet_amount( final Value arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.closet ) );
	}

	public static Value equipped_amount( final Value arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		int runningTotal = 0;

		for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
		{
			if ( EquipmentManager.getEquipment( i ).equals( item ) )
			{
				++runningTotal;
			}
		}

		return new Value( runningTotal );
	}

	public static Value creatable_amount( final Value arg )
	{
		CreateItemRequest item = CreateItemRequest.getInstance( arg.intValue() );
		return new Value( item == null ? 0 : item.getQuantityPossible() );
	}

	public static Value get_ingredients( final Value arg )
	{
		MapValue value = new MapValue( DataTypes.RESULT_TYPE );

		int itemId = arg.intValue();
		int method = ConcoctionDatabase.getMixingMethod( itemId );
		if ( !ConcoctionDatabase.isPermittedMethod( method ) )
		{
			return value;	// can't make it
		}

		AdventureResult[] data = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < data.length; ++i )
		{
			int count = data[ i ].getCount();
			Value key = DataTypes.parseItemValue( data[ i ].getName(), true );
			if ( value.contains( key ) )
			{
				count += value.aref( key ).intValue();
			}
			value.aset( key, new Value( count ) );
		}

		return value;
	}

	public static Value storage_amount( final Value arg )
	{
		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.storage ) );
	}

	public static Value display_amount( final Value arg )
	{
		if ( !KoLCharacter.hasDisplayCase() )
		{
			return DataTypes.ZERO_VALUE;
		}

		if ( !DisplayCaseManager.collectionRetrieved )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}

		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.collection ) );
	}

	public static Value shop_amount( final Value arg )
	{
		if ( !KoLCharacter.hasStore() )
		{
			return DataTypes.ZERO_VALUE;
		}

		if ( !StoreManager.soldItemsRetrieved )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		LockableListModel list = StoreManager.getSoldItemList();

		SoldItem item = new SoldItem( arg.intValue(), 0, 0, 0, 0 );
		int index = list.indexOf( item );

		if ( index < 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		item = (SoldItem) list.get( index );
		return new Value( item.getQuantity() );
	}

	public static Value stash_amount( final Value arg )
	{
		if ( !ClanManager.stashRetrieved )
		{
			RequestThread.postRequest( new ClanStashRequest() );
		}

		List stash = ClanManager.getStash();

		AdventureResult item = new AdventureResult( arg.intValue(), 0 );
		return new Value( item.getCount( stash ) );
	}

	public static Value pulls_remaining()
	{
		return new Value( ConcoctionDatabase.getPullsRemaining() );
	}

	public static Value stills_available()
	{
		return new Value( KoLCharacter.getStillsAvailable() );
	}

	public static Value have_mushroom_plot()
	{
		return DataTypes.makeBooleanValue( MushroomManager.ownsPlot() );
	}

	// The following functions pertain to providing updated
	// information relating to the player.

	public static Value refresh_status()
	{
		RequestThread.postRequest( new CharPaneRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static Value restore_hp( final Value amount )
	{
		return DataTypes.makeBooleanValue( RecoveryManager.recoverHP( amount.intValue() ) );
	}

	public static Value restore_mp( final Value amount )
	{
		return DataTypes.makeBooleanValue( RecoveryManager.recoverMP( amount.intValue() ) );
	}

	public static Value my_name()
	{
		return new Value( KoLCharacter.getUserName() );
	}

	public static Value my_id()
	{
		return new Value( KoLCharacter.getPlayerId() );
	}

	public static Value my_hash()
	{
		return new Value( GenericRequest.passwordHash );
	}

	public static Value my_sign()
	{
		return new Value( KoLCharacter.getSign() );
	}

	public static Value my_path()
	{
		return new Value( KoLCharacter.getPath() );
	}

	public static Value in_muscle_sign()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMuscleSign() );
	}

	public static Value in_mysticality_sign()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMysticalitySign() );
	}

	public static Value in_moxie_sign()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMoxieSign() );
	}

	public static Value in_bad_moon()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inBadMoon() );
	}

	public static Value my_class()
	{
		return DataTypes.makeClassValue( KoLCharacter.getClassType() );
	}

	public static Value my_level()
	{
		return new Value( KoLCharacter.getLevel() );
	}

	public static Value my_hp()
	{
		return new Value( KoLCharacter.getCurrentHP() );
	}

	public static Value my_maxhp()
	{
		return new Value( KoLCharacter.getMaximumHP() );
	}

	public static Value my_mp()
	{
		return new Value( KoLCharacter.getCurrentMP() );
	}

	public static Value my_maxmp()
	{
		return new Value( KoLCharacter.getMaximumMP() );
	}

	public static Value my_primestat()
	{
		int primeIndex = KoLCharacter.getPrimeIndex();
		return primeIndex == 0 ? DataTypes.MUSCLE_VALUE : primeIndex == 1 ? DataTypes.MYSTICALITY_VALUE : DataTypes.MOXIE_VALUE;
	}

	public static Value my_basestat( final Value arg )
	{
		int stat = arg.intValue();

		if ( stat == KoLConstants.MUSCLE )
		{
			return new Value( KoLCharacter.getBaseMuscle() );
		}
		if ( stat == KoLConstants.MYSTICALITY )
		{
			return new Value( KoLCharacter.getBaseMysticality() );
		}
		if ( stat == KoLConstants.MOXIE )
		{
			return new Value( KoLCharacter.getBaseMoxie() );
		}

		if ( stat == KoLConstants.MUSCLE + 3 )
		{
			return new Value( (int) KoLCharacter.getTotalMuscle() );
		}
		if ( stat == KoLConstants.MYSTICALITY + 3 )
		{
			return new Value( (int) KoLCharacter.getTotalMysticality() );
		}
		if ( stat == KoLConstants.MOXIE + 3 )
		{
			return new Value( (int) KoLCharacter.getTotalMoxie() );
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value my_buffedstat( final Value arg )
	{
		int stat = arg.intValue();

		if ( stat == KoLConstants.MUSCLE )
		{
			return new Value( KoLCharacter.getAdjustedMuscle() );
		}
		if ( stat == KoLConstants.MYSTICALITY )
		{
			return new Value( KoLCharacter.getAdjustedMysticality() );
		}
		if ( stat == KoLConstants.MOXIE )
		{
			return new Value( KoLCharacter.getAdjustedMoxie() );
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value my_meat()
	{
		return new Value( KoLCharacter.getAvailableMeat() );
	}

	public static Value my_closet_meat()
	{
		return new Value( KoLCharacter.getClosetMeat() );
	}

	public static Value my_adventures()
	{
		return new Value( KoLCharacter.getAdventuresLeft() );
	}

	public static Value my_daycount()
	{
		return new Value( KoLCharacter.getCurrentDays() );
	}

	public static Value my_turncount()
	{
		return new Value( KoLCharacter.getCurrentRun() );
	}

	public static Value my_fullness()
	{
		return new Value( KoLCharacter.getFullness() );
	}

	public static Value fullness_limit()
	{
		return new Value( KoLCharacter.getFullnessLimit() );
	}

	public static Value my_inebriety()
	{
		return new Value( KoLCharacter.getInebriety() );
	}

	public static Value inebriety_limit()
	{
		return new Value( KoLCharacter.getInebrietyLimit() );
	}

	public static Value my_spleen_use()
	{
		return new Value( KoLCharacter.getSpleenUse() );
	}

	public static Value spleen_limit()
	{
		return new Value( KoLCharacter.getSpleenLimit() );
	}

	public static Value can_eat()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canEat() );
	}

	public static Value can_drink()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canDrink() );
	}

	public static Value turns_played()
	{
		return new Value( KoLCharacter.getCurrentRun() );
	}

	public static Value my_ascensions()
	{
		return new Value( KoLCharacter.getAscensions() );
	}

	public static Value can_interact()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canInteract() );
	}

	public static Value in_hardcore()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.isHardcore() );
	}

	// Basic skill and effect functions, including those used
	// in custom combat consult scripts.

	public static Value have_skill( final Value arg )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasSkill( arg.intValue() ) );
	}

	public static Value mp_cost( final Value skill )
	{
		return new Value( SkillDatabase.getMPConsumptionById( skill.intValue() ) );
	}

	public static Value turns_per_cast( final Value skill )
	{
		return new Value( SkillDatabase.getEffectDuration( skill.intValue() ) );
	}

	public static Value have_effect( final Value arg )
	{
		List potentialEffects = EffectDatabase.getMatchingNames( arg.toString() );
		AdventureResult effect =
			potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
		return effect == null ? DataTypes.ZERO_VALUE : new Value( effect.getCount( KoLConstants.activeEffects ) );
	}

	public static Value use_skill( final Value countValue, final Value skill )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
			{
				RuntimeLibrary.use_skill( skill );
			}

			return DataTypes.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", count + " " + SkillDatabase.getSkillName( skill.intValue() ) );
		return UseSkillRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value use_skill( final Value skill )
	{
		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			return RuntimeLibrary.visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", "1 " + SkillDatabase.getSkillName( skill.intValue() ) );
		return new Value( UseSkillRequest.lastUpdate );
	}

	public static Value use_skill( final Value countValue, final Value skill, final Value target )
	{
		int count = countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( skill.intValue() ) )
		{
			for ( int i = 0; i < count; ++i )
			{
				RuntimeLibrary.use_skill( skill );
			}

			return DataTypes.TRUE_VALUE;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", count + " " + SkillDatabase.getSkillName( skill.intValue() ) + " on " + target );
		return UseSkillRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value last_skill_message()
	{
		return new Value( UseSkillRequest.lastUpdate );
	}

	public static Value get_auto_attack()
	{
		return new Value( KoLCharacter.getAutoAttackAction() );
	}

	public static Value set_auto_attack( Value attackValue )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "autoattack", String.valueOf( attackValue.intValue() ) );

		return DataTypes.VOID_VALUE;
	}

	public static Value attack()
	{
		return RuntimeLibrary.visit_url( "fight.php?action=attack" );
	}

	public static Value steal()
	{
		if ( !FightRequest.canStillSteal() )
		{
			return RuntimeLibrary.attack();
		}

		return RuntimeLibrary.visit_url( "fight.php?action=steal" );
	}

	public static Value runaway()
	{
		return RuntimeLibrary.visit_url( "fight.php?action=runaway" );
	}

	public static Value throw_item( final Value item )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item.intValue() );
	}

	public static Value throw_items( final Value item1, final Value item2 )
	{
		return RuntimeLibrary.visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue() + "&whichitem2=" + item2.intValue() );
	}

	public static Value run_combat()
	{
		RequestThread.postRequest( FightRequest.INSTANCE );
		String response =
			KoLmafiaASH.relayScript == null ? FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

		return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	// Equipment functions.

	public static Value can_equip( final Value item )
	{
		return DataTypes.makeBooleanValue( EquipmentManager.canEquip( ItemDatabase.getItemName( item.intValue() ) ) );
	}

	public static Value equip( final Value item )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "equip", "\u00B6" + item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value equip( final Value slotValue, final Value item )
	{
		String slot = slotValue.toString();
		if ( item.equals( DataTypes.ITEM_INIT ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "unequip", slot );
		}
		else
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "equip", slot + " \u00B6" + item.intValue() );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value equipped_item( final Value slot )
	{
		return DataTypes.makeItemValue( EquipmentManager.getEquipment( slot.intValue() ).getName() );
	}

	public static Value have_equipped( final Value item )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) );
	}

	public static Value outfit( final Value outfit )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "outfit", outfit.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value have_outfit( final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );

		if ( so == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		return DataTypes.makeBooleanValue( EquipmentManager.hasOutfit( so.getOutfitId() ) );
	}

	public static Value outfit_pieces( final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );
		if ( so == null )
		{
			return new ArrayValue( new AggregateType( DataTypes.ITEM_TYPE, 0 ) );
		}

		AdventureResult[] pieces = so.getPieces();
		AggregateType type = new AggregateType( DataTypes.ITEM_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			AdventureResult piece = pieces[ i ];
			value.aset( DataTypes.makeIntValue( i ),
				    DataTypes.makeItemValue( piece ) );
		}

		return value;
	}

	public static Value weapon_hands( final Value item )
	{
		return new Value( EquipmentDatabase.getHands( item.intValue() ) );
	}

	public static Value item_type( final Value item )
	{
		String type = EquipmentDatabase.getItemType( item.intValue() );
		return new Value( type );
	}

	public static Value weapon_type( final Value item )
	{
		int stat = EquipmentDatabase.getWeaponStat( item.intValue() );
		return stat == KoLConstants.MUSCLE ? DataTypes.MUSCLE_VALUE : stat == KoLConstants.MYSTICALITY ? DataTypes.MYSTICALITY_VALUE :
			stat == KoLConstants.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.STAT_INIT;
	}

	public static Value get_power( final Value item )
	{
		return new Value( EquipmentDatabase.getPower( item.intValue() ) );
	}

	public static Value my_familiar()
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
	}

	public static Value my_effective_familiar()
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getEffectiveFamiliar().getId() );
	}

	public static Value my_enthroned_familiar()
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getEnthroned().getId() );
	}

	public static Value have_familiar( final Value familiar )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.findFamiliar( familiar.toString() ) != null );
	}

	public static Value use_familiar( final Value familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "familiar", familiar.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value enthrone_familiar( final Value familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "enthrone", familiar.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value familiar_equipment( final Value familiar )
	{
		return DataTypes.parseItemValue( FamiliarDatabase.getFamiliarItem( familiar.intValue() ), true );
	}

	public static Value familiar_equipped_equipment( final Value familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( familiar.toString() );
		AdventureResult item = fam == null ? EquipmentRequest.UNEQUIP : fam.getItem();
		return item == EquipmentRequest.UNEQUIP ? DataTypes.ITEM_INIT : DataTypes.parseItemValue( item.getName(), true );
	}

	public static Value familiar_weight( final Value familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( familiar.toString() );
		return fam == null ? DataTypes.ZERO_VALUE : new Value( fam.getWeight() );
	}

	// Random other functions related to current in-game
	// state, not directly tied to the character.

	public static Value council()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "council", "" );
		return DataTypes.VOID_VALUE;
	}

	public static Value current_mcd()
	{
		return new Value( KoLCharacter.getMindControlLevel() );
	}

	public static Value change_mcd( final Value level )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "mcd", level.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value have_chef()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasChef() );
	}

	public static Value have_bartender()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasBartender() );
	}

	public static Value have_shop()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasStore() );
	}

	public static Value have_display()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasDisplayCase() );
	}

	public static Value get_counters( final Value label, final Value min, final Value max )
	{
		return new Value( TurnCounter.getCounters( label.toString(), min.intValue(), max.intValue() ) );
	}

	// String parsing functions.

	public static Value is_integer( final Value string )
	{
		return DataTypes.makeBooleanValue( StringUtilities.isNumeric( string.toString() ) );
	}

	public static Value contains_text( final Value source, final Value search )
	{
		return DataTypes.makeBooleanValue( source.toString().indexOf( search.toString() ) != -1 );
	}

	public static Value extract_meat( final Value string )
	{
		ArrayList data = new ArrayList();
		ResultProcessor.processResults( false,
			StringUtilities.globalStringReplace( string.toString(), "- ", "-" ),
			data );

		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			if ( result.getName().equals( AdventureResult.MEAT ) )
			{
				return new Value( result.getCount() );
			}
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value extract_items( final Value string )
	{
		ArrayList data = new ArrayList();
		ResultProcessor.processResults( false,
			StringUtilities.globalStringReplace( string.toString(), "- ", "-" ),
			data );
		MapValue value = new MapValue( DataTypes.RESULT_TYPE );

		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			if ( result.isItem() )
			{
				value.aset(
					DataTypes.parseItemValue( result.getName(), true ),
					DataTypes.parseIntValue( String.valueOf( result.getCount() ), true ) );
			}
		}

		return value;
	}

	public static Value length( final Value string )
	{
		return new Value( string.toString().length() );
	}

	public static Value char_at( final Value source, final Value index )
	{
		String string = source.toString();
		int offset = index.intValue();
		if ( offset < 0 || offset >= string.length() )
		{
			throw LibraryFunction.interpreter.runtimeException( "Offset " + offset + " out of bounds" );
		}
		return new Value( Character.toString( string.charAt( offset ) ) );
	}

	public static Value index_of( final Value source, final Value search )
	{
		String string = source.toString();
		String substring = search.toString();
		return new Value( string.indexOf( substring ) );
	}

	public static Value index_of( final Value source, final Value search,
		final Value start )
	{
		String string = source.toString();
		String substring = search.toString();
		int begin = start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw LibraryFunction.interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.indexOf( substring, begin ) );
	}

	public static Value last_index_of( final Value source, final Value search )
	{
		String string = source.toString();
		String substring = search.toString();
		return new Value( string.lastIndexOf( substring ) );
	}

	public static Value last_index_of( final Value source, final Value search,
		final Value start )
	{
		String string = source.toString();
		String substring = search.toString();
		int begin = start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw LibraryFunction.interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.lastIndexOf( substring, begin ) );
	}

	public static Value substring( final Value source, final Value start )
	{
		String string = source.toString();
		int begin = start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw LibraryFunction.interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.substring( begin ) );
	}

	public static Value substring( final Value source, final Value start,
		final Value finish )
	{
		String string = source.toString();
		int begin = start.intValue();
		if ( begin < 0 )
		{
			throw LibraryFunction.interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		int end = finish.intValue();
		if ( end > string.length() )
		{
			throw LibraryFunction.interpreter.runtimeException( "End index " + end + " out of bounds" );
		}
		if ( begin > end )
		{
			throw LibraryFunction.interpreter.runtimeException( "Begin index " + begin + " greater than end index " + end );
		}
		return new Value( string.substring( begin, end ) );
	}

	public static Value to_upper_case( final Value string )
	{
		return new Value( string.toString().toUpperCase() );
	}

	public static Value to_lower_case( final Value string )
	{
		return new Value( string.toString().toLowerCase() );
	}

	public static Value append( final Value buffer, final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.append( s.toString() );
		return buffer;
	}

	public static Value insert( final Value buffer, final Value index, final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.insert( index.intValue(), s.toString() );
		return buffer;
	}

	public static Value replace( final Value buffer, final Value start, final Value end,
		final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.replace( start.intValue(), end.intValue(), s.toString() );
		return buffer;
	}

	public static Value delete( final Value buffer, final Value start, final Value end )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.delete( start.intValue(), end.intValue() );
		return buffer;
	}

	public static Value set_length( final Value buffer, final Value i )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.setLength( i.intValue() );
		return DataTypes.VOID_VALUE;
	}

	public static Value append_tail( final Value matcher, final Value buffer )
	{
		Matcher m = (Matcher) matcher.rawValue();
		StringBuffer current = (StringBuffer) buffer.rawValue();
		m.appendTail( current );
		return buffer;
	}

	public static Value append_replacement( final Value matcher, final Value buffer, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		StringBuffer current = (StringBuffer) buffer.rawValue();
		m.appendReplacement( current, replacement.toString() );
		return buffer;
	}

	public static Value create_matcher( final Value patternValue, final Value stringValue )
	{
		String pattern = patternValue.toString();
		String string = stringValue.toString();

		if ( !( patternValue.content instanceof Pattern ) )
		{
			try
			{
				patternValue.content = Pattern.compile( pattern, Pattern.DOTALL );
			}
			catch ( PatternSyntaxException e )
			{
				throw LibraryFunction.interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}

		Pattern p = (Pattern) patternValue.content;
		return new Value( DataTypes.MATCHER_TYPE, pattern, p.matcher( string ) );
	}

	public static Value find( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return DataTypes.makeBooleanValue( m.find() );
	}

	public static Value start( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.start() );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value start( final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = group.intValue();
		try
		{
			return new Value( m.start( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value end( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.end() );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value end( final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = group.intValue();
		try
		{
			return new Value( m.end( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value group( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.group() );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value group( final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = group.intValue();
		try
		{
			return new Value( m.group( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw LibraryFunction.interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value group_count( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.groupCount() );
	}

	public static Value replace_first( final Value matcher, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.replaceFirst( replacement.toString() ) );
	}

	public static Value replace_all( final Value matcher, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.replaceAll( replacement.toString() ) );
	}

	public static Value reset( final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		m.reset();
		return matcher;
	}

	public static Value reset( final Value matcher, final Value input )
	{
		Matcher m = (Matcher) matcher.rawValue();
		m.reset( input.toString() );
		return matcher;
	}

	public static Value replace_string( final Value source,
					    final Value searchValue,
					    final Value replaceValue )
	{
		StringBuffer buffer;
		Value returnValue;

		if ( source.rawValue() instanceof StringBuffer )
		{
			buffer = (StringBuffer) source.rawValue();
			returnValue = source;
		}
		else
		{
			buffer = new StringBuffer( source.toString() );
			returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );
		}

		String search = searchValue.toString();
		String replace = replaceValue.toString();

		StringUtilities.globalStringReplace( buffer, search, replace );
		return returnValue;
	}

	public static Value split_string( final Value string )
	{
		String[] pieces = string.toString().split( KoLConstants.LINE_BREAK );

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new Value( i ), new Value( pieces[ i ] ) );
		}

		return value;
	}

	public static Value split_string( final Value string, final Value regex )
	{
		Pattern p;
		if ( regex.rawValue() instanceof Pattern )
		{
			p = (Pattern) regex.rawValue();
		}
		else
		{
			try
			{
				p = Pattern.compile( regex.toString() );
				if ( regex.content == null )
				{
					regex.content = p;
				}
			}
			catch ( PatternSyntaxException e )
			{
				throw LibraryFunction.interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}
		String[] pieces = p.split( string.toString() );

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new Value( i ), new Value( pieces[ i ] ) );
		}

		return value;
	}

	public static Value group_string( final Value string, final Value regex )
	{
		Pattern p;
		if ( regex.rawValue() instanceof Pattern )
		{
			p = (Pattern) regex.rawValue();
		}
		else
		{
			try
			{
				p = Pattern.compile( regex.toString() );
				if ( regex.content == null )
				{
					regex.content = p;
				}
			}
			catch ( PatternSyntaxException e )
			{
				throw LibraryFunction.interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}
		Matcher userPatternMatcher = p.matcher( string.toString() );
		MapValue value = new MapValue( DataTypes.REGEX_GROUP_TYPE );

		int matchCount = 0;
		int groupCount = userPatternMatcher.groupCount();

		Value[] groupIndexes = new Value[ groupCount + 1 ];
		for ( int i = 0; i <= groupCount; ++i )
		{
			groupIndexes[ i ] = new Value( i );
		}

		Value matchIndex;
		CompositeValue slice;

		try
		{
			while ( userPatternMatcher.find() )
			{
				matchIndex = new Value( matchCount );
				slice = (CompositeValue) value.initialValue( matchIndex );

				value.aset( matchIndex, slice );
				for ( int i = 0; i <= groupCount; ++i )
				{
					slice.aset( groupIndexes[ i ], new Value( userPatternMatcher.group( i ) ) );
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

	public static Value expression_eval( final Value expr )
	{
		Expression e;
		if ( expr.content instanceof Expression )
		{
			e = (Expression) expr.content;
		}
		else
		{
			e = new Expression( expr.toString(), "expression_eval()" );
			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return new Value( e.eval() );
	}

	public static Value modifier_eval( final Value expr )
	{
		ModifierExpression e;
		if ( expr.content instanceof ModifierExpression )
		{
			e = (ModifierExpression) expr.content;
		}
		else
		{
			e = new ModifierExpression( expr.toString(), "modifier_eval()" );
			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return new Value( e.eval() );
	}

	public static Value maximize( final Value maximizerStringValue, final Value isSpeculateOnlyValue )
	{
		return maximize( maximizerStringValue, DataTypes.ZERO_VALUE, DataTypes.ZERO_VALUE, isSpeculateOnlyValue );
	}

	public static Value maximize( final Value maximizerStringValue, final Value maxPriceValue, final Value priceLevelValue, final Value isSpeculateOnlyValue )
	{
		String maximizerString = maximizerStringValue.toString();
		int maxPrice = maxPriceValue.intValue();
		int priceLevel = priceLevelValue.intValue();
		boolean isSpeculateOnly = isSpeculateOnlyValue.intValue() != 0;

		return new Value( MaximizerFrame.maximize( maximizerString, maxPrice, priceLevel, isSpeculateOnly ) );
	}

	public static Value monster_eval( final Value expr )
	{
		MonsterExpression e;
		if ( expr.content instanceof MonsterExpression )
		{
			e = (MonsterExpression) expr.rawValue();
		}
		else
		{
			e = new MonsterExpression( expr.toString(), "monster_eval()" );
			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return new Value( e.eval() );
	}

	public static Value is_online( final Value arg )
	{
		String name = arg.toString();
		return DataTypes.makeBooleanValue( KoLmafia.isPlayerOnline( name ) );
	}

	public static Value chat_macro( final Value macroValue )
	{
		String macro = macroValue.toString().trim();

		ChatSender.executeMacro( macro );

		return DataTypes.VOID_VALUE;
	}

	public static Value chat_clan( final Value messageValue )
	{
		String channel = "/clan";
		String message = messageValue.toString().trim();

		ChatSender.sendMessage( channel, message, true );
		return DataTypes.VOID_VALUE;
	}

	public static Value chat_clan( final Value messageValue, final Value recipientValue )
	{
		String channel = "/" + recipientValue.toString().trim();
		String message = messageValue.toString().trim();

		ChatSender.sendMessage( channel, message, true );
		return DataTypes.VOID_VALUE;
	}

	public static Value chat_private( final Value recipientValue, final Value messageValue )
	{
		String recipient = recipientValue.toString();

		String message = messageValue.toString();

		if ( message.equals( "" ) || message.startsWith( "/" ) )
		{
			return DataTypes.VOID_VALUE;
		}

		ChatSender.sendMessage( recipient, message, false );

		return DataTypes.VOID_VALUE;
	}

	public static Value chat_notify( final Value messageValue, final Value colorValue )
	{
		String messageString = StringUtilities.globalStringReplace( messageValue.toString(), "<", "&lt;" );

		String colorString = StringUtilities.globalStringDelete( colorValue.toString(), "\"" );
		colorString = "\"" + colorString + "\"";

		InternalMessage message = new InternalMessage( messageString, colorString );

		ChatPoller.addEntry( message );

		return DataTypes.VOID_VALUE;
	}

	public static Value who_clan()
	{
		List chatMessages = new LinkedList();

		WhoMessage message = null;
		ChatSender.sendMessage( chatMessages, "/who clan", false, false );

		Iterator messageIterator = chatMessages.iterator();

		while ( messageIterator.hasNext() )
		{
			ChatMessage chatMessage = (ChatMessage) messageIterator.next();

			if ( chatMessage instanceof WhoMessage )
			{
				message = (WhoMessage) chatMessage;
				break;
			}
		}

		MapValue value = new MapValue( DataTypes.BOOLEAN_MAP_TYPE );

		if ( message != null )
		{
			Iterator entryIterator = message.getContacts().entrySet().iterator();

			while ( entryIterator.hasNext() )
			{
				Entry entry = (Entry) entryIterator.next();
				value.aset( new Value( (String) entry.getKey() ) , new Value( entry.getValue() == Boolean.TRUE ) );
			}
		}

		return value;
	}

	// Quest completion functions.

	public static Value entryway()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "entryway", "" );
		return RuntimeLibrary.continueValue();
	}

	public static Value hedgemaze()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "hedgemaze", "" );
		return RuntimeLibrary.continueValue();
	}

	public static Value guardians()
	{
		int itemId = SorceressLairManager.fightTowerGuardians( true );
		return DataTypes.makeItemValue( itemId );
	}

	public static Value chamber()
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "chamber", "" );
		return RuntimeLibrary.continueValue();
	}

	public static Value tavern()
	{
		int result = TavernManager.locateTavernFaucet();
		return new Value( KoLmafia.permitsContinue() ? result : -1 );
	}

	// Arithmetic utility functions.

	public static Value random( final Value arg )
	{
		int range = arg.intValue();
		if ( range < 2 )
		{
			throw LibraryFunction.interpreter.runtimeException( "Random range must be at least 2" );
		}
		return new Value( KoLConstants.RNG.nextInt( range ) );
	}

	public static Value round( final Value arg )
	{
		return new Value( (int) Math.round( arg.floatValue() ) );
	}

	public static Value truncate( final Value arg )
	{
		return new Value( (int) arg.floatValue() );
	}

	public static Value floor( final Value arg )
	{
		return new Value( (int) Math.floor( arg.floatValue() ) );
	}

	public static Value ceil( final Value arg )
	{
		return new Value( (int) Math.ceil( arg.floatValue() ) );
	}

	public static Value square_root( final Value val )
	{
		float value = val.floatValue();
		if ( value < 0.0f )
		{
			throw LibraryFunction.interpreter.runtimeException( "Can't take square root of a negative value" );
		}
		return new Value( (float) Math.sqrt( value ) );
	}

	public static Value min( final Value arg1, final Value arg2 )
	{
		if ( arg1.getType() == DataTypes.INT_TYPE && arg2.getType() == DataTypes.INT_TYPE )
		{
			return new Value(  Math.min( arg1.toIntValue().intValue(),
						     arg2.toIntValue().intValue() ) );
		}
		return new Value( Math.min( arg1.toFloatValue().floatValue(),
					    arg2.toFloatValue().floatValue() ) );

	}

	public static Value max( final Value arg1, final Value arg2 )
	{
		if ( arg1.getType() == DataTypes.INT_TYPE && arg2.getType() == DataTypes.INT_TYPE )
		{
			return new Value( Math.max( arg1.toIntValue().intValue(),
						    arg2.toIntValue().intValue() ) );
		}
		return new Value( Math.max( arg1.toFloatValue().floatValue(),
					    arg2.toFloatValue().floatValue() ) );

	}

	// Settings-type functions.

	public static Value url_encode( final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( URLEncoder.encode( arg.toString(), "UTF-8" ) );
	}

	public static Value url_decode( final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( URLDecoder.decode( arg.toString(), "UTF-8" ) );
	}

	public static Value entity_encode( final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( CharacterEntities.escape( arg.toString() ) );
	}

	public static Value entity_decode( final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( CharacterEntities.unescape( arg.toString() ) );
	}

	public static Value get_property( final Value name )
	{
		String property = name.toString();
		return !Preferences.isUserEditable( property ) ? DataTypes.STRING_INIT :
			new Value( Preferences.getString( property ) );
	}

	public static Value set_property( final Value name, final Value value )
	{
		// In order to avoid code duplication for combat
		// related settings, use the shell.

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
			"set", name.toString() + "=" + value.toString() );
		return DataTypes.VOID_VALUE;
	}

	// Functions for aggregates.

	public static Value count( final Value arg )
	{
		return new Value( arg.count() );
	}

	public static Value clear( final Value arg )
	{
		arg.clear();
		return DataTypes.VOID_VALUE;
	}

	public static Value file_to_map( final Value var1, final Value var2 )
	{
		return file_to_map( var1, var2, DataTypes.TRUE_VALUE );
	}

	public static Value file_to_map( final Value var1, final Value var2, final Value var3 )
	{
		String filename = var1.toString();
		CompositeValue result = (CompositeValue) var2;
		boolean compact = var3.intValue() == 1;

		BufferedReader reader = DataFileCache.getReader( filename );
		if ( reader == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		String[] data = null;
		result.clear();

		try
		{
			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				if ( data.length > 1 )
				{
					result.read( data, 0, compact );
				}
			}
		}
		catch ( Exception e )
		{
			StringBuffer buffer = new StringBuffer( "Invalid line in data file" );
			if ( data != null )
			{
				buffer.append( ": \"" );
				for ( int i = 0; i < data.length; ++i )
				{
					if ( i > 0 )
					{
						buffer.append( '\t' );
					}
					buffer.append( data[ i ] );
				}
				buffer.append( "\"" );
			}

			// Print the bad data that caused the error
			Exception ex = LibraryFunction.interpreter.runtimeException( buffer.toString() );

			// If it's a ScriptException, we generated it ourself
			if ( e instanceof ScriptException )
			{
				// Print the bad data and the resulting error
				RequestLogger.printLine( ex.getMessage() );
				RequestLogger.printLine( e.getMessage() );
			}
			else
			{
				// Otherwise, print a stack trace
				StaticEntity.printStackTrace( e, ex.getMessage() );
			}
			return DataTypes.FALSE_VALUE;
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}
		}

		return DataTypes.TRUE_VALUE;
	}

	public static Value map_to_file( final Value var1, final Value var2 )
	{
		return map_to_file( var1, var2, DataTypes.TRUE_VALUE );
	}

	public static Value map_to_file( final Value var1, final Value var2, final Value var3 )
	{
		CompositeValue map_variable = (CompositeValue) var1;
		String filename = var2.toString();
		boolean compact = var3.intValue() == 1;

		ByteArrayOutputStream cacheStream = new ByteArrayOutputStream();

		PrintStream writer = LogStream.openStream( cacheStream, "UTF-8" );
		map_variable.dump( writer, "", compact );
		writer.close();

		byte[] data = cacheStream.toByteArray();
		return DataFileCache.printBytes( filename, data );
	}

	// Custom combat helper functions.

	public static Value my_location()
	{
		String location = Preferences.getString( "lastAdventure" );
		return location.equals( "" ) ? DataTypes.parseLocationValue( "Rest", true ) : DataTypes.parseLocationValue( location, true );
	}

	public static Value set_location( final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		if ( adventure != null &&
			!Preferences.getString( "lastAdventure" ).equals( adventure.getAdventureName() ) )
		{
			Preferences.setString( "lastAdventure", adventure.getAdventureName() );
			AdventureFrame.updateSelectedAdventure( adventure );
		}
		return DataTypes.VOID_VALUE;
	}

	public static Value last_monster()
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		return DataTypes.parseMonsterValue(
			monster != null ? monster.getName() : "none", true );
	}

	public static Value get_monsters( final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

		int monsterCount = data == null ? 0 : data.getMonsterCount();

		AggregateType type = new AggregateType( DataTypes.MONSTER_TYPE, monsterCount );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < monsterCount; ++i )
		{
			value.aset( new Value( i ), DataTypes.parseMonsterValue( data.getMonster( i ).getName(), true ) );
		}

		return value;

	}

	public static Value appearance_rates( final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

		AggregateType type = new AggregateType( DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE );
		MapValue value = new MapValue( type );
		if ( data == null ) return value;

		float combatFactor = data.areaCombatPercent();
		value.aset( DataTypes.MONSTER_INIT,
			new Value( data.combats() < 0 ? -1.0F : 100.0f - combatFactor ) );

		int total = data.totalWeighting();
		for ( int i = data.getMonsterCount() - 1; i >= 0; --i )
		{
			int weight = data.getWeighting( i );
			if ( weight == -2 ) continue;	// impossible this ascension
			value.aset( DataTypes.parseMonsterValue( data.getMonster( i ).getName(), true ), new Value( combatFactor * weight / total ) );
		}

		return value;

	}

	public static Value expected_damage()
	{
		return expected_damage( MonsterStatusTracker.getLastMonster(), MonsterStatusTracker.getMonsterAttackModifier() );
	}

	public static Value expected_damage( final Value arg )
	{
		return expected_damage( (MonsterData) arg.rawValue(), 0 );
	}

	private static Value expected_damage( MonsterData monster, int attackModifier )
	{
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int attack = monster.getAttack() + attackModifier;
		int defenseStat = KoLCharacter.getAdjustedMoxie();

		if ( KoLCharacter.hasSkill(SkillDatabase.getSkillId("Hero of the Half-Shell" ) ) &&
		     EquipmentManager.usingShield() &&
		     KoLCharacter.getAdjustedMuscle() > defenseStat )
		{
			defenseStat = KoLCharacter.getAdjustedMuscle();
		}

		int baseValue =
			Math.max( 0, attack - defenseStat ) + attack / 4 - KoLCharacter.getDamageReduction();

		float damageAbsorb =
			1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
		float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() ) / 100.0f;
		return new Value( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static Value monster_level_adjustment()
	{
		return new Value( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static Value weight_adjustment()
	{
		return new Value( KoLCharacter.getFamiliarWeightAdjustment() );
	}

	public static Value mana_cost_modifier()
	{
		return new Value( KoLCharacter.getManaCostAdjustment() );
	}

	public static Value combat_mana_cost_modifier()
	{
		return new Value( KoLCharacter.getManaCostAdjustment( true ) );
	}

	public static Value raw_damage_absorption()
	{
		return new Value( KoLCharacter.getDamageAbsorption() );
	}

	public static Value damage_absorption_percent()
	{
		int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
		if ( raw == 0 )
		{
			return DataTypes.ZERO_FLOAT_VALUE;
		}

		// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
		// ( sqrt( raw / 10 ) - 1 ) / 10

		double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
		return new Value( (float) percent );
	}

	public static Value damage_reduction()
	{
		return new Value( KoLCharacter.getDamageReduction() );
	}

	public static Value elemental_resistance()
	{
		return new Value( KoLCharacter.getElementalResistance( MonsterStatusTracker.getMonsterAttackElement() ) );
	}

	public static Value elemental_resistance( final Value arg )
	{
		if ( arg.getType().equals( DataTypes.TYPE_ELEMENT ) )
		{
			return new Value( KoLCharacter.getElementalResistance( arg.intValue() ) );
		}

		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
	}

	public static Value combat_rate_modifier()
	{
		return new Value( KoLCharacter.getCombatRateAdjustment() );
	}

	public static Value initiative_modifier()
	{
		return new Value( KoLCharacter.getInitiativeAdjustment() );
	}

	public static Value experience_bonus()
	{
		return new Value( KoLCharacter.getExperienceAdjustment() );
	}

	public static Value meat_drop_modifier()
	{
		return new Value( KoLCharacter.getMeatDropPercentAdjustment() );
	}

	public static Value item_drop_modifier()
	{
		return new Value( KoLCharacter.getItemDropPercentAdjustment() );
	}

	public static Value buffed_hit_stat()
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();
		return new Value( hitStat );
	}

	public static Value current_hit_stat()
	{
		return EquipmentManager.getHitStatType() == KoLConstants.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.MUSCLE_VALUE;
	}

	public static Value monster_element()
	{
		int element = MonsterStatusTracker.getMonsterDefenseElement();
		return new Value( DataTypes.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static Value monster_element( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ELEMENT_INIT;
		}

		int element = monster.getDefenseElement();
		return new Value( DataTypes.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
	}

	public static Value monster_attack()
	{
		return new Value( MonsterStatusTracker.getMonsterAttack() );
	}

	public static Value monster_attack( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getAttack() );
	}

	public static Value monster_defense()
	{
		return new Value( MonsterStatusTracker.getMonsterDefense() );
	}

	public static Value monster_defense( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getDefense() );
	}

	public static Value monster_hp()
	{
		return new Value( MonsterStatusTracker.getMonsterHealth() );
	}

	public static Value monster_hp( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getHP() );
	}

	public static Value monster_phylum()
	{
		int phylum = MonsterStatusTracker.getMonsterPhylum();
		return new Value( DataTypes.PHYLUM_TYPE, phylum, MonsterDatabase.phylumNames[ phylum ] );
	}

	public static Value monster_phylum( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.PHYLUM_INIT;
		}

		int phylum = monster.getPhylum();
		return new Value( DataTypes.PHYLUM_TYPE, phylum, MonsterDatabase.phylumNames[ phylum ] );
	}

	public static Value item_drops()
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		List data = monster == null ? new ArrayList() : monster.getItems();

		MapValue value = new MapValue( DataTypes.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.parseItemValue( result.getName(), true ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() >> 16 ), true ) );
		}

		return value;
	}

	public static Value item_drops( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		List data = monster == null ? new ArrayList() : monster.getItems();

		MapValue value = new MapValue( DataTypes.RESULT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.parseItemValue( result.getName(), true ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() >> 16 ), true ) );
		}

		return value;
	}

	public static Value item_drops_array()
	{
		return item_drops_array( MonsterStatusTracker.getLastMonster() );
	}

	public static Value item_drops_array( final Value arg )
	{
		return item_drops_array( (MonsterData) arg.rawValue() );
	}

	public static Value item_drops_array( MonsterData monster )
	{
		List data = monster == null ? new ArrayList() : monster.getItems();
		int dropCount = data.size();
		AggregateType type = new AggregateType( RuntimeLibrary.itemDropRec, dropCount );
		ArrayValue value = new ArrayValue( type );
		for ( int i = 0; i < dropCount; ++i )
		{
			AdventureResult result = (AdventureResult) data.get( i );
			int count = result.getCount();
			char dropType = (char) (count & 0xFFFF);
			RecordValue rec = (RecordValue) value.aref( new Value( i ) );

			rec.aset( 0, DataTypes.parseItemValue( result.getName(), true ), null );
			rec.aset( 1, new Value( count >> 16 ), null );
			if ( dropType < '1' || dropType > '9' )
			{	// leave as an empty string if no special type was given
				rec.aset( 2, new Value( String.valueOf( dropType ) ), null );
			}
		}

		return value;
	}

	public static Value meat_drop()
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		if ( monster == null )
		{
			return new Value( -1 );
		}

		return new Value( (monster.getMinMeat() +monster.getMaxMeat()) / 2 );
	}

	public static Value meat_drop( final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return new Value( -1 );
		}

		return new Value( (monster.getMinMeat() +monster.getMaxMeat()) / 2 );
	}

	public static Value will_usually_dodge()
	{
		return DataTypes.makeBooleanValue( MonsterStatusTracker.willUsuallyDodge() );
	}

	public static Value will_usually_miss()
	{
		return DataTypes.makeBooleanValue( MonsterStatusTracker.willUsuallyMiss() );
	}

	public static Value numeric_modifier( final Value modifier )
	{
		String mod = modifier.toString();
		return new Value( KoLCharacter.currentNumericModifier( mod ) );
	}

	public static Value numeric_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return new Value( Modifiers.getNumericModifier( name, mod ) );
	}

	public static Value numeric_modifier( final Value familiar, final Value modifier, final Value weight, final Value item )
	{
		FamiliarData fam = new FamiliarData( familiar.intValue() );
		String mod = modifier.toString();
		int w = Math.max( 1, weight.intValue() );
		AdventureResult it = new AdventureResult( item.intValue(), 1 );

		return new Value( Modifiers.getNumericModifier( fam, mod, w, it ) );
	}

	public static Value boolean_modifier( final Value modifier )
	{
		String mod = modifier.toString();
		return DataTypes.makeBooleanValue( KoLCharacter.currentBooleanModifier( mod ) );
	}

	public static Value boolean_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return DataTypes.makeBooleanValue( Modifiers.getBooleanModifier( name, mod ) );
	}

	public static Value string_modifier( final Value modifier )
	{
		String mod = modifier.toString();
		return new Value( KoLCharacter.currentStringModifier( mod ) );
	}

	public static Value string_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return new Value( Modifiers.getStringModifier( name, mod ) );
	}

	public static Value effect_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return new Value( DataTypes.parseEffectValue( Modifiers.getStringModifier( name, mod ), true ) );
	}

	public static Value class_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return new Value( DataTypes.parseClassValue( Modifiers.getStringModifier( name, mod ), true ) );
	}

	public static Value stat_modifier( final Value arg, final Value modifier )
	{
		String name = arg.toString();
		String mod = modifier.toString();
		return new Value( DataTypes.parseStatValue( Modifiers.getStringModifier( name, mod ), true ) );
	}

	public static Value galaktik_cures_discounted()
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.galaktikCuresAvailable() );
	}

	public static Value white_citadel_available()
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isWhiteCitadelAvailable() );
	}

	public static Value friars_available()
	{
		if ( QuestLogRequest.areFriarsAvailable() )
			Preferences.setInteger( "lastFriarCeremonyAscension", Preferences.getInteger( "knownAscensions" ));
		return DataTypes.makeBooleanValue( QuestLogRequest.areFriarsAvailable() );
	}

	public static Value black_market_available()
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isBlackMarketAvailable() );
	}

	public static Value hippy_store_available()
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isHippyStoreAvailable() );
	}

	public static Value dispensary_available()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getDispensaryOpen() );
	}

	public static Value guild_store_available()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getGuildStoreOpen() );
	}

	public static Value hidden_temple_unlocked()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getTempleUnlocked() );
	}

	public static Value knoll_available()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.knollAvailable() );
	}

	public static Value canadia_available()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canadiaAvailable() );
	}

	public static Value gnomads_available()
	{
		return DataTypes.makeBooleanValue( KoLCharacter.gnomadsAvailable() );
	}

	public static Value is_trendy( final Value thing )
	{
		// Types: "Items", "Campground", Bookshelf", "Familiars", "Skills", "Clan Item".
		String key = thing.toString();
		Type type = thing.getType();
		boolean result;

		if ( type.equals( DataTypes.TYPE_STRING ) )
		{

			result = TrendyRequest.isTrendy( "Items", key ) &&
				TrendyRequest.isTrendy( "Campground", key ) &&
				TrendyRequest.isTrendy( "Bookshelf", key ) &&
				TrendyRequest.isTrendy( "Familiars", key ) &&
				TrendyRequest.isTrendy( "Skills", key ) &&
				TrendyRequest.isTrendy( "Clan Item", key );
		}
		else if ( type.equals( DataTypes.TYPE_ITEM ) )
		{
			result = TrendyRequest.isTrendy( "Items", key );
		}
		else if ( type.equals( DataTypes.TYPE_FAMILIAR ) )
		{
			result = TrendyRequest.isTrendy( "Familiars", key );
		}
		else if ( type.equals( DataTypes.TYPE_SKILL ) )
		{
			if ( SkillDatabase.isBookshelfSkill( key ) )
			{
				int itemId = SkillDatabase.skillToBook( key );
				key = ItemDatabase.getItemName( itemId );
				result = TrendyRequest.isTrendy( "Bookshelf", key );
			}
			else
			{
				result = TrendyRequest.isTrendy( "Skills", key );
			}
		}
		else
		{
			result = false;
		}

		return DataTypes.makeBooleanValue( result );
	}

	public static Value mmg_visit()
	{
		RequestThread.postRequest( new MoneyMakingGameRequest() );
		return DataTypes.VOID_VALUE;
	}

	public static Value mmg_search( final Value arg1, final Value arg2 )
	{
		int lower = arg1.intValue();
		int higher = arg2.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.SEARCH, lower, higher ) );
		return DataTypes.VOID_VALUE;
	}

	public static Value mmg_make_bet( final Value arg, final Value source )
	{
		int amount = arg.intValue();
		int storage = source.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.MAKE_BET, amount, storage ) );
		return new Value( MoneyMakingGameManager.getLastBetId() );
	}

	public static Value mmg_retract_bet( final Value arg )
	{
		int id = arg.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.RETRACT_BET, id ) );
		return RuntimeLibrary.continueValue();
	}

	public static Value mmg_take_bet( final Value arg, final Value source )
	{
		int betId = arg.intValue();
		int storage = source.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.TAKE_BET, betId, storage ) );
		return new Value( MoneyMakingGameManager.getLastWinnings() );
	}

	public static Value mmg_my_bets()
	{
		int[] bets = MoneyMakingGameManager.getActiveBets();

		AggregateType type = new AggregateType( DataTypes.INT_TYPE, bets.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < bets.length; ++i )
		{
			value.aset( new Value( i ), new Value( bets[ i ] ) );
		}

		return value;
	}

	public static Value mmg_offered_bets()
	{
		int[] bets = MoneyMakingGameManager.getOfferedBets();

		AggregateType type = new AggregateType( DataTypes.INT_TYPE, bets.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < bets.length; ++i )
		{
			value.aset( new Value( i ), new Value( bets[ i ] ) );
		}

		return value;
	}

	public static Value mmg_bet_owner( final Value arg )
	{
		int id = arg.intValue();
		return new Value( MoneyMakingGameManager.betOwner( id ) );
	}

	public static Value mmg_bet_owner_id( final Value arg )
	{
		int id = arg.intValue();
		return new Value( MoneyMakingGameManager.betOwnerId( id ) );
	}

	public static Value mmg_bet_amount( final Value arg )
	{
		int id = arg.intValue();
		return new Value( MoneyMakingGameManager.betAmount( id ) );
	}

	public static Value mmg_wait_event( final Value arg )
	{
		int seconds = arg.intValue();
		return new Value( MoneyMakingGameManager.getNextEvent( seconds ) );
	}

	public static Value mmg_bet_taker()
	{
		return new Value( MoneyMakingGameManager.getLastEventPlayer() );
	}

	public static Value mmg_bet_taker_id()
	{
		return new Value( MoneyMakingGameManager.getLastEventPlayerId() );
	}

	public static Value mmg_bet_winnings()
	{
		return new Value( MoneyMakingGameManager.getLastEventWinnings() );
	}
}
