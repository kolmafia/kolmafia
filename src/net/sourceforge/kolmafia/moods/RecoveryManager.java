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

package net.sourceforge.kolmafia.moods;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.HPRestoreItemList.HPRestoreItem;
import net.sourceforge.kolmafia.moods.MPRestoreItemList.MPRestoreItem;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.parsetree.Value;

public class RecoveryManager
{
	private static boolean recoveryActive;

	public static boolean isRecoveryActive()
	{
		return RecoveryManager.recoveryActive;
	}

	public static void setRecoveryActive( final boolean recoveryActive )
	{
		RecoveryManager.recoveryActive = recoveryActive;
	}

	public static boolean isRecoveryPossible()
	{
		return	!RecoveryManager.isRecoveryActive() &&
			FightRequest.getCurrentRound() == 0 &&
			!FightRequest.inMultiFight() &&	
			GenericRequest.choiceHandled &&
			!CharPaneRequest.inValhalla();
	}

	public static boolean runThresholdChecks()
	{
		float autoStopValue = Preferences.getFloat( "autoAbortThreshold" );
		if ( autoStopValue >= 0.0f )
		{
			autoStopValue *= KoLCharacter.getMaximumHP();
			if ( KoLCharacter.getCurrentHP() <= autoStopValue )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ABORT_STATE, "Health fell below " + (int) autoStopValue + ". Auto-abort triggered." );
				return false;
			}
		}

		return true;
	}

	public static void runBetweenBattleChecks( final boolean isFullCheck )
	{
		RecoveryManager.runBetweenBattleChecks( isFullCheck, isFullCheck, true, isFullCheck );
	}

	public static void runBetweenBattleChecks( final boolean isScriptCheck, final boolean isMoodCheck,
		final boolean isHealthCheck, final boolean isManaCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( !RecoveryManager.isRecoveryPossible() || KoLmafia.refusesContinue() )
		{
			return;
		}

		// First, run the between battle script defined by the
		// user, which may obviate the built in behavior.

		RecoveryManager.recoveryActive = true;

		if ( isScriptCheck )
		{
			KoLmafia.executeScript( Preferences.getString( "betweenBattleScript" ) );
		}

		SpecialOutfit.createImplicitCheckpoint();

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isMoodCheck )
		{
			MoodManager.execute();
		}

		if ( isHealthCheck )
		{
			RecoveryManager.recoverHP();
		}

		if ( isMoodCheck )
		{
			ManaBurnManager.burnExtraMana( false );
		}

		if ( isManaCheck )
		{
			RecoveryManager.recoverMP();
		}

		SpecialOutfit.restoreImplicitCheckpoint();

		if ( KoLmafia.permitsContinue() && KoLCharacter.getCurrentHP() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );
		}

		if ( KoLmafia.permitsContinue() && KoLmafia.currentIterationString.length() > 0 )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( KoLmafia.currentIterationString );
			KoLmafia.currentIterationString = "";
		}

		FightRequest.haveFought(); // reset flag

		RecoveryManager.recoveryActive = false;
	}

	/**
	 * Utility. The method called in between battles. This method checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public static boolean recoverHP()
	{
		return RecoveryManager.recoverHP( 0 );
	}

	public static boolean recoverHP( final int recover )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		try
		{
			if ( Preferences.getBoolean( "removeMalignantEffects" ) )
			{
				MoodManager.removeMalignantEffects();
			}

			HPRestoreItemList.updateHealthRestored();
			if ( RecoveryManager.invokeRecoveryScript( "HP", recover ) )
			{
				return true;
			}
			return RecoveryManager.recover(
				recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method called in between commands. This method checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public static boolean recoverMP()
	{
		return RecoveryManager.recoverMP( 0 );
	}

	/**
	 * Utility. The method which restores the character's current mana points above the given value.
	 */

	public static boolean recoverMP( final int mpNeeded )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		try
		{
			MPRestoreItemList.updateManaRestored();
			if ( RecoveryManager.invokeRecoveryScript( "MP", mpNeeded ) )
			{
				return true;
			}
			return RecoveryManager.recover(
				mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method which ensures that the amount needed exists, and if not, calls the appropriate scripts to do
	 * so.
	 */

	private static boolean recover( float desired, final String settingName, final String currentName,
		final String maximumName, final Object[] techniques )
		throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option. This takes precedence over all other checks.

		String restoreSetting = Preferences.getString( settingName + "Items" ).trim().toLowerCase();

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object[] empty = new Object[ 0 ];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[ 0 ] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[ 0 ] );

		float setting = Preferences.getFloat( settingName );

		if ( setting < 0.0f && desired == 0 )
		{
			return true;
		}

		int current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
		{
			return true;
		}

		int maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
		int needed = (int) Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		// Next, check against the restore target to see how
		// far you need to go.

		setting = Preferences.getFloat( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, setting * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
		{
			desired = maximum;
		}

		// Special handling of the Hidden Temple. Here, as
		// long as your health is above zero, you're okay.

		boolean isNonCombatHealthRestore =
			settingName.startsWith( "hp" ) && KoLmafia.isAdventuring() && KoLmafia.currentAdventure.isNonCombatsOnly();

		if ( isNonCombatHealthRestore )
		{
			needed = 1;
			desired = 1;
		}

		if ( current >= needed )
		{
			return true;
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items. This involves a few extra
		// reflection methods.

		String currentTechniqueName;

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory intensive, but it allows for a lot
		// more flexibility.

		ArrayList possibleItems = new ArrayList();
		ArrayList possibleSkills = new ArrayList();

		for ( int i = 0; i < techniques.length; ++i )
		{
			currentTechniqueName = techniques[ i ].toString().toLowerCase();
			if ( restoreSetting.indexOf( currentTechniqueName ) == -1 )
			{
				continue;
			}

			if ( techniques[ i ] instanceof HPRestoreItem )
			{
				HPRestoreItem item = (HPRestoreItem) techniques[ i ];
				if ( item.isSkill() )
				{
					possibleSkills.add( item );
				}
				else if ( item.usableInCurrentPath() )
				{
					possibleItems.add( item );
				}
			}

			if ( techniques[ i ] instanceof MPRestoreItem )
			{
				MPRestoreItem item = (MPRestoreItem) techniques[ i ];
				if ( item.isSkill() )
				{
					possibleSkills.add( item );
				}
				else if ( item.usableInCurrentPath() )
				{
					possibleItems.add( item );
				}
			}
		}

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// First, use any available skills. 

		int last = -1;

		if ( !possibleSkills.isEmpty() )
		{
			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					last = current;
					currentTechniqueName = possibleSkills.get( indexToTry ).toString().toLowerCase();

					RecoveryManager.recoverOnce(
						possibleSkills.get( indexToTry ), currentTechniqueName, (int) desired, false );
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );

					if ( last >= current )
					{
						++indexToTry;
					}
				}
				while ( indexToTry < possibleSkills.size() && current < needed );
			}

			if ( KoLmafia.refusesContinue() )
			{
				return false;
			}
		}

		// Iterate through every restore item which is already available
		// in the player's inventory.

		Collections.sort( possibleItems );

		for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
		{
			do
			{
				last = current;
				currentTechniqueName = possibleItems.get( i ).toString().toLowerCase();

				RecoveryManager.recoverOnce( possibleItems.get( i ), currentTechniqueName, (int) desired, false );

				current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
				maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();

				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// If we get here, we still need healing. For areas that are
		// all noncombats, then you can heal using only unguent.

		if ( isNonCombatHealthRestore && KoLCharacter.getAvailableMeat() >= 30 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.PUNGENT_UNGUENT ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		if ( !possibleItems.isEmpty() )
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
			last = -1;

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleItems );

				do
				{
					last = current;
					currentTechniqueName = possibleItems.get( indexToTry ).toString().toLowerCase();

					RecoveryManager.recoverOnce(
						possibleItems.get( indexToTry ), currentTechniqueName, (int) desired, true );
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
					desired = Math.min( maximum, desired );

					if ( last >= current )
					{
						++indexToTry;
					}
				}
				while ( indexToTry < possibleItems.size() && current < needed );
			}

			HPRestoreItemList.setPurchaseBasedSort( false );
			MPRestoreItemList.setPurchaseBasedSort( false );
		}
		else if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You ran out of restores." );
			return false;
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Autorecovery failed." );
			return false;
		}

		return true;
	}

	/**
	 * Utility. The method which uses the given recovery technique (not specified in a script) in order to restore.
	 */

	private static void recoverOnce( final Object technique, final String techniqueName, final int needed,
		final boolean purchase )
	{
		// If the technique is an item, and the item is not readily
		// available, then don't bother with this item -- however, if
		// it is the only item present, then rethink it.

		if ( technique instanceof HPRestoreItem )
		{
			( (HPRestoreItem) technique ).recoverHP( needed, purchase );
		}

		if ( technique instanceof MPRestoreItem )
		{
			( (MPRestoreItem) technique ).recoverMP( needed, purchase );
		}
	}

	/**
	 * Returns the total number of mana restores currently available to the player.
	 */

	public static int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = Preferences.getString( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 )
			{
				AdventureResult item = MPRestoreItemList.CONFIGURES[ i ].getItem();
				if ( item != null )
				{
					restoreCount += item.getCount( KoLConstants.inventory );
				}
			}
		}

		return restoreCount;
	}

	private static boolean invokeRecoveryScript( final String type, final int needed )
	{
		String scriptName = Preferences.getString( "recoveryScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}
		Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			Value v = interpreter.execute( "main", new String[]
			{
				type,
				String.valueOf( needed )
			} );
			return v != null && v.intValue() != 0;
		}
		return false;
	}

}
