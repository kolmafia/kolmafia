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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.combat;

import java.util.HashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MPRestoreItemList;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;

public class Macrofier
{
	private static String macroOverride = null;
	private static Interpreter macroInterpreter = null;

	private static final Pattern ALLCALLS_PATTERN = Pattern.compile( "call (\\w+)" );
	private static final Pattern ALLSUBS_PATTERN = Pattern.compile( "sub (\\w+)([\\s;\\n]+endsub)?" );

	public static void resetMacroOverride()
	{
		Macrofier.macroOverride = null;
		Macrofier.macroInterpreter = null;
	}

	public static void setMacroOverride( String macroOverride, Interpreter interpreter )
	{
		if ( macroOverride == null || macroOverride.length() == 0 )
		{
			Macrofier.macroOverride = null;
			Macrofier.macroInterpreter = null;
		}
		else if ( macroOverride.indexOf( ';' ) != -1 )
		{
			Macrofier.macroOverride = macroOverride;
			Macrofier.macroInterpreter = null;
		}
		else
		{
			Macrofier.macroOverride = macroOverride;
			Macrofier.macroInterpreter = interpreter;
		}
	}

	private static boolean isSimpleAction( String action )
	{
		if ( action.startsWith( "consult" ) )
		{
			return false;
		}

		if ( action.startsWith( "delevel" ) )
		{
			return false;
		}

		if ( action.startsWith( "twiddle" ) )
		{
			return false;
		}

		return true;
	}

	public static String macrofy()
	{
		// If there's an override, always use it

		if ( Macrofier.macroInterpreter == null && Macrofier.macroOverride != null && Macrofier.macroOverride.length() > 0 )
		{
			return Macrofier.macroOverride;
		}

		// Don't try to macrofy the first round, since we don't know what monster it is, and "special action"
		// could include olfaction.
		
		if ( FightRequest.getCurrentRound() == 0 )
		{
			return null;
		}

		// Begin monster-specific macrofication.

		String monsterName = FightRequest.getLastMonsterName();

		if ( Macrofier.macroInterpreter != null )
		{
			String[] parameters = new String[ 3 ];
			parameters[ 0 ] = String.valueOf( FightRequest.getRoundIndex() );
			parameters[ 1 ] = monsterName;
			parameters[ 2 ] = FightRequest.lastResponseText;

			// Execute a single function in the scope of the
			// currently executing file.  Do not re-execute
			// top-level code in that file.
			Value returnValue = Macrofier.macroInterpreter.execute( macroOverride, parameters, false );

			if ( returnValue == null || returnValue.getType().equals( DataTypes.TYPE_VOID ) )
			{
				String message = "Macro override \"" + macroOverride + "\" returned void.";
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			else
			{
				String result = returnValue.toString();
				if ( result.length() > 0 )
				{
					return result;
				}
			}
		}

		StringBuffer macro = new StringBuffer();

		if ( monsterName.equals( "hulking construct" ) )
		{
			// use ATTACK & WALL punchcards
			macro.append( "if hascombatitem 3146 && hascombatitem 3155\n" );
			if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				macro.append( "  use 3146,3155\n" );
			}
			else
			{
				macro.append( "  use 3146; use 3155\n" );
			}
			macro.append( "endif\nrunaway; repeat\n" );
			return macro.toString();
		}

		float thresh = Preferences.getFloat( "autoAbortThreshold" );
		if ( thresh > 0.0f )
		{
			macro.append( "abort hppercentbelow " );
			macro.append( (int) ( thresh * 100.0f ) );
			macro.append( '\n' );
		}

		Macrofier.macroCommon( macro );

		macro.append( "#mafiaheader\n" );

		boolean debug = Preferences.getBoolean( "macroDebug" );

		boolean useGlobalPrefix = CombatActionManager.hasGlobalPrefix();

		for ( int i = 0; i < 10000; ++i )
		{
			if ( monsterName.equals( "rampaging adding machine" ) && !KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) && !FightRequest.waitingForSpecial )
			{
				if ( debug )
				{
					RequestLogger.printLine( "(unable to macrofy vs. RAM)" );
				}

				return null;
			}

			String action;

			if ( useGlobalPrefix )
			{
				action = CombatActionManager.getCombatAction( "global prefix", i, true );
			}
			else
			{
				action = CombatActionManager.getCombatAction( monsterName, i, true );
			}
			
			if ( !Macrofier.isSimpleAction( action ) )
			{
				if ( debug )
				{
					RequestLogger.printLine( "(unable to macrofy due to action: " + action + ")" );
				}

				return null;
			}

			int finalRound = 0;
			
			if ( !useGlobalPrefix && CombatActionManager.atEndOfStrategy() )
			{
				macro.append( "mark mafiafinal\n" );
				finalRound = macro.length();
			}

			Macrofier.macroAction( macro, action, finalRound );

			if ( finalRound != 0 )
			{
				if ( finalRound == macro.length() )
				{
					// last line of CCS generated no action!
					macro.append( "call mafiaround; attack\n" );
				}
				macro.append( "goto mafiafinal" );
				break;
			}

			if ( useGlobalPrefix && CombatActionManager.atEndOfStrategy() )
			{
				useGlobalPrefix = false;
				i = -1; // continue with actual CCS section
			}
		}

		if ( debug )
		{
			RequestLogger.printLine( "Generated macro:" );
			Macrofier.indentify( macro.toString(), false );
			RequestLogger.printLine( "" );
		}

		HashSet allCalls = new HashSet();
		Matcher m = Macrofier.ALLCALLS_PATTERN.matcher( macro );

		while ( m.find() )
		{
			allCalls.add( m.group( 1 ) );
		}

		m = Macrofier.ALLSUBS_PATTERN.matcher( macro.toString() );

		while ( m.find() )
		{
			String label = m.group( 1 );
			if ( m.group( 2 ) != null || !allCalls.contains( label ) )
			{
				// this sub is useless!
				Matcher del =
					Pattern.compile( "call " + label + "\\b|sub " + label + "\\b.*?endsub", Pattern.DOTALL ).matcher(
						macro.toString() );

				macro.setLength( 0 );

				while ( del.find() )
				{
					del.appendReplacement( macro, "" );
				}

				del.appendTail( macro );
			}
		}

		if ( debug )
		{
			RequestLogger.updateDebugLog( "Optimized macro:" );
			Macrofier.indentify( macro.toString(), true );
		}

		return macro.toString();
	}

	protected static void macroAction( StringBuffer macro, String action, final int finalRound )
	{
		if ( action.length() == 0 || action.equals( "skip" ) )
		{
			return;
		}
	
		if ( CombatActionManager.isMacroAction( action ) )
		{
			if ( action.startsWith( "\"" ) )
			{
				action = action.substring( 1 );
			}
			
			if ( action.charAt( action.length() - 1 ) == '\"' )
			{
				action = action.substring( 0, action.length() - 1 );
			}
		
			macro.append( action );
			macro.append( '\n' );

			return;
		}

		action = CombatActionManager.getShortCombatOptionName( action );

		if ( action.equals( "special" ) )
		{
			if ( FightRequest.waitingForSpecial )
			{
				// only allow once per combat
				FightRequest.waitingForSpecial = false;
				String specialAction = FightRequest.getSpecialAction();

				if ( specialAction != null )
				{
					if ( specialAction.startsWith( "skill" ) )
					{
						Macrofier.macroSkill( macro, StringUtilities.parseInt( specialAction.substring( 5 ) ) );
					}
					else
					{
						macro.append( "call mafiaround; use " + specialAction + "\n" );
						// TODO
					}
				}
			}
		}
		else if ( action.equals( "abort" ) )
		{
			if ( finalRound != 0 )
			{
				macro.append( "abort \"KoLmafia CCS abort\"\n" );
			}
			else
			{
				macro.append( "abort \"Click Script button again to continue\"\n" );
				macro.append( "#mafiarestart\n" );
			}
		}
		else if ( action.equals( "abort after" ) )
		{
			KoLmafia.abortAfter( "Aborted by CCS request" );
		}
		else if ( action.equals( "runaway" ) )
		{
			macro.append( "runaway\n" );
		}
		else if ( action.startsWith( "runaway" ) )
		{
			int runaway = StringUtilities.parseInt( action.substring( 7 ) );
			if ( FightRequest.freeRunawayChance() >= runaway )
			{
				macro.append( "runaway\n" );
			}
		}
		else if ( action.startsWith( "attack" ) )
		{
			macro.append( "call mafiaround; attack\n" );
		}
		else if ( action.equals( "steal" ) )
		{
			if ( MonsterStatusTracker.shouldSteal() )
			{
				macro.append( "pickpocket\n" );
			}
		}
		else if ( action.equals( "summon ghost" ) )
		{
			macro.append( "summonspirit\n" );
		}
		else if ( action.equals( "jiggle" ) )
		{
			if ( EquipmentManager.usingChefstaff() )
			{
				macro.append( "call mafiaround; jiggle\n" );
			}
		}
		else if ( action.startsWith( "combo " ) )
		{
			int[] combo = DiscoCombatHelper.getCombo( action.substring( 6 ) );
			if ( combo != null )
			{
				Macrofier.macroCombo( macro, combo );
			}
		}
		else if ( action.startsWith( "skill" ) )
		{
			int skillId = StringUtilities.parseInt( action.substring( 5 ) );
			String skillName = SkillDatabase.getSkillName( skillId );

			if ( skillName.equals( "Transcendent Olfaction" ) )
			{
				// You can't sniff if you are already on the trail.

				// You can't sniff in Bad Moon, even though the skill
				// shows up on the char sheet, unless you've recalled
				// your skills.

				if ( ( KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled() ) ||
				     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.ON_THE_TRAIL ) ) )
				{ // ignore
				}
				else
				{ // must insert On The Trail check in generated macro
					// too, in case more than one olfact is attempted.
					macro.append( "if !haseffect 331\n" );
					Macrofier.macroSkill( macro, skillId );
					macro.append( "endif\n" );
				}
			}
			else if ( skillName.equals( "CLEESH" ) )
			{ // Macrofied combat will continue with the same CCS after
				// a CLEESH, unlike round-by-round combat which switches
				// sections.  Make sure there's something to finish off
				// the amphibian.
				Macrofier.macroSkill( macro, skillId );
				if ( finalRound != 0 )
				{
					macro.append( "attack; repeat\n" );
				}
			}
			else
			{
				Macrofier.macroSkill( macro, skillId );
			}
		}
		else if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{ // can't use items in Birdform
		}
		else
		// must be an item use
		{
			macro.append( "call mafiaround; use " + action + "\n" );
			// TODO
		}
	}

	public static void indentify( String macro, boolean debug )
	{
		String indent = "";
		String element = debug ? "\t" : "\u00A0\u00A0\u00A0\u00A0";
		String[] pieces = macro.split( "\n" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			String line = pieces[ i ].trim();
			if ( line.startsWith( "end" ) && indent.length() > 0 )
			{
				indent = indent.substring( element.length() );
			}
			if ( debug )
			{
				RequestLogger.updateDebugLog( indent + line );
			}
			else
			{
				RequestLogger.printLine( indent + line );
			}
			if ( line.startsWith( "if " ) || line.startsWith( "while " ) || line.startsWith( "sub " ) )
			{
				indent = indent + element;
			}
		}
	}

	public static void macroCommon( StringBuffer macro )
	{
		macro.append( "sub mafiaround\n" );
		Macrofier.macroUseAntidote( macro );
		macro.append( "endsub#mafiaround\n" );

		macro.append( "sub mafiamp\n" );
		Macrofier.macroManaRestore( macro );
		macro.append( "endsub#mafiamp\n" );
	}

	public static void macroSkill( StringBuffer macro, int skillId )
	{
		int cost = SkillDatabase.getMPConsumptionById( skillId );
		if ( cost > KoLCharacter.getMaximumMP() )
		{
			return; // no point in even trying
		}

		if ( cost > 0 && Preferences.getBoolean( "autoManaRestore" ) )
		{
			macro.append( "while mpbelow " );
			macro.append( cost );
			macro.append( "\ncall mafiamp\nendwhile\n" );
		}
		macro.append( "if hasskill " );
		macro.append( skillId );
		macro.append( "\ncall mafiaround; skill " );
		macro.append( skillId );
		macro.append( "\nendif\n" );
	}

	public static void macroCombo( StringBuffer macro, int[] combo )
	{
		int cost = 0;
		for ( int i = 0; i < combo.length; ++i )
		{
			cost += SkillDatabase.getMPConsumptionById( combo[ i ] );
		}

		if ( cost > KoLCharacter.getMaximumMP() )
		{
			return; // no point in even trying
		}

		boolean restore = Preferences.getBoolean( "autoManaRestore" );

		if ( restore )
		{
			macro.append( "while mpbelow " );
			macro.append( cost );
			macro.append( "\ncall mafiamp\nendwhile\n" );
		}
		else
		{
			macro.append( "if !mpbelow " );
			macro.append( cost );
			macro.append( "\n" );
		}
		macro.append( "call mafiaround; " );
		for ( int i = 0; i < combo.length; ++i )
		{
			macro.append( "skill " );
			macro.append( combo[ i ] );
			macro.append( "; " );
		}
		macro.append( "\n" );
		if ( !restore )
		{
			macro.append( "endif\n" );
		}
	}

	public static final void macroUseAntidote( StringBuffer macro )
	{
		if ( !KoLConstants.inventory.contains( FightRequest.ANTIDOTE ) )
		{
			return;
		}
		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return;	// can't use items!
		}
		int minLevel = Preferences.getInteger( "autoAntidote" );
		int poison = MonsterStatusTracker.getPoisonLevel();
		if ( poison > minLevel || minLevel == 0 )
		{
			return;	// no poison expected that the user wants to remove
		}

		macro.append( "if hascombatitem " );
		macro.append( ItemPool.ANTIDOTE );
		macro.append( " && (" );
		boolean first = true;
		for ( int i = minLevel; i > 0; --i )
		{
			if ( poison != 0 && i != poison )
			{	// only check for the monster's known poison attack
				continue;
			}
			if ( !first )
			{
				macro.append( " || " );
			}
			first = false;
			macro.append( "haseffect " );
			macro.append( EffectDatabase.POISON_ID[ i ] );
		}
		macro.append( ")\n  use " );
		macro.append( ItemPool.ANTIDOTE );
		macro.append( "\nendif\n" );
	}

	public static void macroManaRestore( StringBuffer macro )
	{
		int cumulative = 0;
		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( MPRestoreItemList.CONFIGURES[ i ].isCombatUsable() && !KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
			{
				int count = MPRestoreItemList.CONFIGURES[ i ].getItem().getCount( KoLConstants.inventory );
				if ( count <= 0 )
					continue;
				String item = String.valueOf( MPRestoreItemList.CONFIGURES[ i ].getItem().getItemId() );
				cumulative += count;
				if ( cumulative >= 30 )
				{ // Assume this item will be sufficient for all requests
					macro.append( "call mafiaround; use " );
					macro.append( item );
					macro.append( "\nmark mafiampexit\n" );
					return;
				}

				macro.append( "if hascombatitem " );
				macro.append( item );
				macro.append( "\ncall mafiaround; use " );
				macro.append( item );
				macro.append( "\ngoto mafiampexit\n endif\n" );
			}
		}

		macro.append( "abort \"No MP restoratives!\"\n" );
		macro.append( "mark mafiampexit\n" );
	}

}
