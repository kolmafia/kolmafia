/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;

import net.sourceforge.kolmafia.preferences.Preferences;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountRequest
	extends PasswordHashRequest
{
	private static final Pattern SELECTED_PATTERN =
		Pattern.compile( "selected=\"selected\" value=\"?(\\d+)\"?>" );

	public enum Tab
	{
		ALL( null ),
		INTERFACE( "interface" ),
		INVENTORY( "inventory" ),
		CHAT( "chat" ),
		COMBAT( "combat" ),
		ACCOUNT( "account" ),
		PROFILE( "profile" ),
		PRIVACY( "privacy" ),
		NONE( null );

		private final String name;

		private Tab( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	private Tab tab;

	public AccountRequest()
	{
		this( Tab.ALL );
	}

	public AccountRequest( final Tab tab )
	{
		super( "account.php" );
		this.tab = tab;

		String field = tab.toString();
		if ( field != null )
		{
			this.addFormField( "tab", field );
		}
	}

	private static final Pattern TAB_PATTERN =
		Pattern.compile( "tab=([^&]*)" );
	private static final Pattern LOADTAB_PATTERN =
		Pattern.compile( "action=loadtab&value=([^&]*)" );

	private static final Tab getTab( final String urlString )
	{
		if ( urlString.equals( "account.php" ) )
		{
			return Tab.INTERFACE;
		}

		Matcher m = TAB_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			m = LOADTAB_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return Tab.NONE;
			}
		}

		String tabName = m.group(1);
		for ( Tab tab : Tab.values() )
		{
			if ( tabName.equals( tab.toString() ) )
			{
				return tab;
			}
		}
		
		return Tab.NONE;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( this.tab == Tab.ALL )
		{
			RequestThread.postRequest( new AccountRequest( Tab.INTERFACE ) );
			RequestThread.postRequest( new AccountRequest( Tab.INVENTORY ) );
			// RequestThread.postRequest( new AccountRequest ( Tab.CHAT ) );
			RequestThread.postRequest( new AccountRequest( Tab.COMBAT ) );
			RequestThread.postRequest( new AccountRequest( Tab.ACCOUNT ) );
			// RequestThread.postRequest( new AccountRequest ( Tab.PROFILE ) );
			// RequestThread.postRequest( new AccountRequest ( Tab.PRIVACY ) );
		}
		else
		{
			super.run();
		}
	}

	@Override
	public void processResults()
	{
		AccountRequest.parseAccountData( this.getURLString(), this.responseText );
	}

	public static final void parseAccountData( final String location, final String responseText )
	{
		if ( location.indexOf( "action=" ) != -1 )
		{
			AccountRequest.parseAction( location, responseText );
			return;
		}

		// Visiting a tab on the Options page
		PasswordHashRequest.updatePasswordHash( responseText );
		AccountRequest.parseOptionTab( location, responseText );
	}

	private static final void parseOptionTab( final String location, final String responseText )
	{
		switch ( AccountRequest.getTab( location ) )
		{
		case INTERFACE:
			AccountRequest.parseInterfaceOptions( responseText );
			return;
		case INVENTORY:
			AccountRequest.parseInventoryOptions( responseText );
			return;
		case CHAT:
			AccountRequest.parseChatOptions( responseText );
			return;
		case COMBAT:
			AccountRequest.parseCombatOptions( responseText );
			return;
		case ACCOUNT:
			AccountRequest.parseAccountOptions( responseText );
			return;
		case PROFILE:
			AccountRequest.parseProfileOptions( responseText );
			return;
		case PRIVACY:
			AccountRequest.parsePrivacyOptions( responseText );
			return;
		}
	}

	private static boolean getCheckbox( final String flag, final String responseText )
	{
		String test = "checked=\"checked\"  name=\"" + flag + "\"";
		return responseText.indexOf( test ) != -1;
	}

	private static String fancyMenuStyle = "<input type=\"radio\" value=\"fancy\" checked=\"checked\"  name=\"menu\"/>Icons";
	private static String compactMenuStyle = "<input type=\"radio\" value=\"compact\" checked=\"checked\"  name=\"menu\"/>Drop-Downs";
	private static String normalMenuStyle = "<input type=\"radio\" value=\"normal\" checked=\"checked\"  name=\"menu\"/>Links";

	private static final void parseInterfaceOptions( final String responseText )
	{
		// Top Menu Style
		GenericRequest.topMenuStyle =
			responseText.indexOf( fancyMenuStyle ) != -1 ?
			GenericRequest.MENU_FANCY :
			responseText.indexOf( compactMenuStyle ) != -1 ?
			GenericRequest.MENU_COMPACT :
			GenericRequest.MENU_NORMAL;

		boolean checked;
		checked = AccountRequest.getCheckbox( "flag_compactchar", responseText );
		CharPaneRequest.compactCharacterPane = checked;
		checked = AccountRequest.getCheckbox( "flag_swapfam", responseText );
		CharPaneRequest.familiarBelowEffects = checked;
	}

	private static final void parseInventoryOptions( final String responseText )
	{
		boolean checked;
		checked = AccountRequest.getCheckbox( "flag_sellstuffugly", responseText );
		KoLCharacter.setAutosellMode( checked ? "compact" : "detailed" );
		checked = AccountRequest.getCheckbox( "flag_lazyinventory", responseText );
		KoLCharacter.setLazyInventory( checked );
		checked = AccountRequest.getCheckbox( "flag_unfamequip", responseText );
		KoLCharacter.setUnequipFamiliar( checked );
	}

	private static final void parseChatOptions( final String responseText )
	{
	}

	private static final Pattern AUTOATTACK_PATTERN =
		Pattern.compile( "<select name=\"autoattack\">.*?</select>", Pattern.DOTALL );

	private static final void parseCombatOptions( final String responseText )
	{
		// Disable stationary buttons to avoid conflicts when
		// the action bar is enabled.

		boolean checked = AccountRequest.getCheckbox( "flag_wowbar", responseText );
		Preferences.setBoolean( "serverAddsCustomCombat", checked );

		int autoAttackAction = 0;

		Matcher selectMatcher = AccountRequest.AUTOATTACK_PATTERN.matcher( responseText );
		if ( selectMatcher.find() )
		{
			Matcher optionMatcher = AccountRequest.SELECTED_PATTERN.matcher( selectMatcher.group() );
			if ( optionMatcher.find() )
			{
				String autoAttackActionString = optionMatcher.group( 1 );
				autoAttackAction = Integer.parseInt( autoAttackActionString );
			}
		}

		KoLCharacter.setAutoAttackAction( autoAttackAction );
	}

	private static final void parseAccountOptions( final String responseText )
	{
		// Whether or not a player is currently in Bad Moon or hardcore
		// is also found here through the presence of buttons.

		// <input class=button name="action" type=submit value="Drop Hardcore">
		boolean isHardcore = responseText.indexOf( "<input class=button name=\"action\" type=submit value=\"Drop Hardcore\">" ) != -1;
		KoLCharacter.setHardcore( isHardcore );

		// <input class=button name="action" type=submit value="Drop Bad Moon">

		if ( responseText.indexOf( "<input class=button name=\"action\" type=submit value=\"Drop Bad Moon\">" ) != -1 )
		{
			KoLCharacter.setHardcore( true );
			KoLCharacter.setSign( "Bad Moon" );
		}
		else if ( KoLCharacter.getSignStat() == ZodiacType.BAD_MOON )
		{
			KoLCharacter.setSign( "None" );
		}

		// Your skills have been recalled if you have freed the king
		// and don't have a "Recall Skills" button in your account menu

		// <input class=button name="action" type="submit" value="Recall Skills">

		boolean recalled =
			KoLCharacter.kingLiberated() &&
			responseText.indexOf( "<input class=button name=\"action\" type=\"submit\" value=\"Recall Skills\">") == -1;
		KoLCharacter.setSkillsRecalled( recalled );
	}

	private static final void parseProfileOptions( final String responseText )
	{
	}

	private static final void parsePrivacyOptions( final String responseText )
	{
	}

	private static final Pattern ACTION_PATTERN =
		Pattern.compile( "action=([^&]*)");
	private static final Pattern VALUE_PATTERN =
		Pattern.compile( "value=([^&]*)");

	private static final void parseAction( final String location, final String responseText )
	{
		Matcher actionMatcher = AccountRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
			return;
		}

		String action = actionMatcher.group(1);

		if ( action.equals( "loadtab" ) )
		{
			AccountRequest.parseOptionTab( location, responseText );
			return;
		}

		Matcher valueMatcher = AccountRequest.VALUE_PATTERN.matcher( location );
		String valueString = valueMatcher.find() ? valueMatcher.group(1) : "";

		// Interface options

		if ( action.equals( "menu" ) )
		{
			// account.php?pwd&action=menu&value=fancy&ajax=1
			// account.php?pwd&action=menu&value=compact&ajax=1
			// account.php?pwd&action=menu&value=normal&ajax=1
			GenericRequest.topMenuStyle =
				valueString.equals( "fancy" ) ?
				GenericRequest.MENU_FANCY :
				valueString.equals( "compact" ) ?
				GenericRequest.MENU_COMPACT :
				GenericRequest.MENU_NORMAL;
			return;
		}

		if ( action.equals( "flag_compactchar" ) )
		{
			boolean checked = valueString.equals( "1" );
			CharPaneRequest.compactCharacterPane = checked;
			return;
		}

		if ( action.equals( "flag_swapfam" ) )
		{
			boolean checked = valueString.equals( "1" );
			CharPaneRequest.familiarBelowEffects = checked;
			return;
		}

		// Inventory options

		if ( action.equals( "flag_sellstuffugly" ) )
		{
			boolean checked = valueString.equals( "1" );
			KoLCharacter.setAutosellMode( checked ? "compact" : "detailed" );
			return;
		}

		if ( action.equals( "flag_lazyinventory" ) )
		{
			boolean checked = valueString.equals( "1" );
			KoLCharacter.setLazyInventory( checked );
			return;
		}

		if ( action.equals( "flag_unfamequip" ) )
		{
			boolean checked = valueString.equals( "1" );
			KoLCharacter.setUnequipFamiliar( checked );
			return;
		}

		// Combat options

		if ( action.equals( "flag_wowbar" ) )
		{
			boolean checked = valueString.equals( "1" );
			Preferences.setBoolean( "serverAddsCustomCombat", checked );
			return;
		}

		if ( action.equals( "autoattack" ) )
		{
			int value = Integer.parseInt( valueString );
			KoLCharacter.setAutoAttackAction( value );
			return;
		}

		// Account options

		// <form method="post" action="account.php">

		// account.php?actions[]=recallskills&action=Recall+Skills&tab=account&pwd
		// -->
		// You must confirm the confirmation box.
		//
		// account.php?actions[]=recallskills&action=Recall+Skills&recallconfirm=1&tab=account&pwd
		//-->
		// Your ancient memories return in a flood!  You feel more
		// skilled!  You remember some old familiar familiars!

		if ( action.equals( "Recall+Skills" ) )
		{
			if ( location.indexOf( "recallconfirm=1" ) != -1 )
			{
				// Recalling skills
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Recalled ancient memories. Yowza!" );
				RequestLogger.updateSessionLog();
				KoLCharacter.setSkillsRecalled( true );
			}
			return;
		}
		
		// Check for failure to drop path before checking to see if a path was dropped
		// For Boris, "You must abandon the Avatar of Boris before forsaking ronin."
		if ( responseText.indexOf( "You must abandon" ) != -1 )
		{
			return;
		}

		// <input type=hidden name="actions[]" value="unronin">
		// <input class=button name="action" type=submit value="Forsake Ronin">
		// <input type="checkbox" class="confirm" name="unroninconfirm" value="1">

		// account.php?actions[]=unronin&action=Forsake+Ronin&unroninconfirm=1&tab=account&pwd

		if ( action.equals( "Forsake+Ronin" ) )
		{
			if ( location.indexOf( "unroninconfirm=1" ) != -1 )
			{
				// Dropping from Softcore to Casual.
				KoLCharacter.setRonin( false );
				CharPaneRequest.setInteraction( true );
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Dropped into Casual. Slacker." );
				RequestLogger.updateSessionLog();
			}
			return;
		}

		// <input type=hidden name="actions[]" value="unpath">
		// <input class=button name="action" type="submit" value="Drop Oxygenarian">
		// <input type="checkbox" class="confirm" name="unpathconfirm" value="1">

		// account.php?actions[]=unpath&action=Drop+Teetotaler&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Boozetafarian&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Oxygenarian&unpathconfirm=1&tab=account&pwd
		if ( action.equals( "Drop+Teetotaler" ) ||
		     action.equals( "Drop+Boozetafarian" ) ||
		     action.equals( "Drop+Oxygenarian" ) )
		{
			if ( location.indexOf( "unpathconfirm=1" ) != -1 )
			{
				// Dropping consumption restrictions
				KoLCharacter.setConsumptionRestriction( AscensionSnapshot.NOPATH );
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Dropped consumption restrictions." );
				RequestLogger.updateSessionLog();
			}
			return;
		}

		// account.php?actions[]=unpath&action=Drop+Bees+Hate+You&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Way+of+the+Surprising+Fist&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Trendy&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Avatar+of+Boris&unpathconfirm=1&tab=account&pwd
		// account.php?actions[]=unpath&action=Drop+Bugbear+Invasion&unpathconfirm=1&tab=account&pwd
		if ( action.equals( "Drop+Bees+Hate+You" ) ||
		     action.equals( "Drop+Way+of+the+Surprising+Fist" ) ||
		     action.equals( "Drop+Trendy" ) ||
		     action.equals( "Drop+Avatar+of+Boris" ) ||
		     action.equals( "Drop+Bugbear+Invasion" ) ||
			 action.equals( "Drop+Zombie+Slayer" ) ||
			 action.equals( "Drop+Class+Act" ) )
		{
			if ( location.indexOf( "unpathconfirm=1" ) != -1 )
			{
				// Dropping challenge path
				String oldPath = KoLCharacter.getPath();
				KoLCharacter.setPath( "None" );
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Dropped " + oldPath );
				RequestLogger.updateSessionLog();

				// If we were in Beecore, we need to check the Telescope again
				// Ditto for Bugbear Invasion
				if ( oldPath.equals( "Bees Hate You" ) || oldPath.equals( "Bugbear Invasion" ) )
				{
					Preferences.setInteger( "lastTelescopeReset", -1 );
					KoLCharacter.checkTelescope();
				}

				// If we drop Avatar of Boris, do we get here,
				// or are we redirected to the "End of the
				// Boris Road" choice adventure?
			}
			return;
		}

		// <input type=hidden name="actions[]" value="unhardcore">
		// <input class=button name="action" type=submit value="Drop Hardcore">
		// <input type="checkbox" class="confirm" name="unhardcoreconfirm" value="1">

		// account.php?actions[]=unhardcore&action=Drop+Hardcore&unhardcoreconfirm=1&tab=account&pwd
		if ( action.equals( "Drop+Hardcore" ) )
		{
			if ( location.indexOf( "unhardcoreconfirm=1" ) != -1 )
			{
				// Dropping Hardcore
				KoLCharacter.setHardcore( false );
				KoLCharacter.setRonin( KoLCharacter.getCurrentRun() < 1000 );
				CharPaneRequest.setInteraction( KoLCharacter.getCurrentRun() >= 1000 );
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Dropped Hardcore. Wimp." );
				RequestLogger.updateSessionLog();
			}
			return;
		}

		// <input type=hidden name="actions[]" value="unbadmoon">
		// <input class=button name="action" type=submit value="Drop Bad Moon">
		// <input type="checkbox" class="confirm" name="unbadmoonconfirm" value="1">

		// account.php?actions[]=unbadmoon&action=Drop+Bad+Moon&unbadmoonconfirm=1&tab=account&pwd
		if ( action.equals( "Drop+Bad+Moon" ) )
		{
			if ( location.indexOf( "unbadmoonconfirm=1" ) != -1 )
			{
				// Dropping Bad Moon
				KoLCharacter.setSign( "None" );
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Dropped Bad Moon. You fool!" );
				RequestLogger.updateSessionLog();
			}
			return;
		}
	}

	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		JSONObject flags = JSON.getJSONObject( "flag_config" );

		boolean checked;

		// Interface options

		int topmenu = flags.getInt( "topmenu" );
		GenericRequest.topMenuStyle =
			(topmenu == 2 ) ?
			GenericRequest.MENU_FANCY :
			(topmenu == 1 ) ?
			GenericRequest.MENU_COMPACT :
			GenericRequest.MENU_NORMAL;

		checked = flags.getInt( "compactchar" ) == 1;
		CharPaneRequest.compactCharacterPane = checked;

		checked = flags.getInt( "swapfam" ) == 1;
		CharPaneRequest.familiarBelowEffects = checked;

		checked = flags.getInt( "ignorezonewarnings" ) == 1;
		KoLCharacter.setIgnoreZoneWarnings( checked );

		// Inventory options

		checked = flags.getInt( "sellstuffugly" ) == 1;
		KoLCharacter.setAutosellMode( checked ? "compact" : "detailed" );

		checked = flags.getInt( "lazyinventory" ) == 1;
		KoLCharacter.setLazyInventory( checked );

		checked = flags.getInt( "unfamequip" ) == 1;
		KoLCharacter.setUnequipFamiliar( checked );

		// Combat options

		checked = flags.getInt( "wowbar" ) == 1;
		Preferences.setBoolean( "serverAddsCustomCombat", checked );

		int autoAttackAction = flags.getInt( "autoattack" );
		KoLCharacter.setAutoAttackAction( autoAttackAction );

		// Account options

		String sign = JSON.getString( "sign" );
		KoLCharacter.setSign( sign );

		String path = JSON.getString( "path" );
		if ( path.equals( "0" ) )
		{
			path = "None";
		}
		else if ( path.equals( "1" ) )
		{
			path = "Boozetafarian";
		}
		else if ( path.equals( "2" ) )
		{
			path = "Teetotaler";
		}
		else if ( path.equals( "3" ) )
		{
			path = "Oxygenarian";
		}
		else if ( path.equals( "4" ) )
		{
			path = "Bees Hate You";
		}
		else if ( path.equals( "6" ) )
		{
			path = "Way of the Surprising Fist";
		}
		else if ( path.equals( "7" ) )
		{
			path = "Trendy";
		}
		else if ( path.equals( "8" ) )
		{
			path = "Avatar of Boris";
		}
		else if ( path.equals( "9" ) )
		{
			path = "Bugbear Invasion";
		}
		else if ( path.equals( "10" ) )
		{
			path = "Zombie Slayer";
		}
		else if ( path.equals( "11" ) )
		{
			path = "Class Act";
		}
		else if ( path.equals( "12" ) )
		{
			path = "Avatar of Jarlsberg";
		}
		else if ( path.equals( "14" ) )
		{
			path = "BIG!";
		}
		else if ( path.equals( "15" ) )
		{
			path = "KOLHS";
		}
		else if ( path.equals( "16" ) )
		{
			path = "Class Act II: A Class For Pigs";
		}
		else if ( path.equals( "17" ) )
		{
			path = "Avatar of Sneaky Pete";
		}
		KoLCharacter.setPath( path );

		boolean hardcore = JSON.getInt( "hardcore" ) == 1 || sign.equals( "Bad Moon" );
		KoLCharacter.setHardcore( hardcore );

		boolean liberated = JSON.getInt( "freedralph" ) == 1;
		KoLCharacter.setKingLiberated( liberated );

		boolean recalled = JSON.getInt( "recalledskills" ) == 1;
		KoLCharacter.setSkillsRecalled( recalled );
	}
}
