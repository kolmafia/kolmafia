/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import org.json.JSONException;
import org.json.JSONObject;

public class ApiRequest
	extends GenericRequest
{
	private static final ApiRequest INSTANCE = new ApiRequest( "status" );

	private String what;
	private String id;
	public JSONObject JSON;
	private boolean silent = false;

	public ApiRequest()
	{
		this( "status" );
	}

	public ApiRequest( final String what )
	{
		super( "api.php" );
		this.what = what;
		this.addFormField( "what", what );
		this.addFormField( "for", "KoLmafia" );
		this.id = "";
	}

	public ApiRequest( final String what, final String id )
	{
		this( what );
		this.addFormField( "id", id );
		this.id = id;
	}

	public ApiRequest( final String what, final int id )
	{
		this( what, String.valueOf( id ) );
	}

	public static String updateStatus()
	{
		return ApiRequest.updateStatus( false );
	}

	public synchronized static String updateStatus( final boolean silent)
	{
		ApiRequest.INSTANCE.silent = silent;
		ApiRequest.INSTANCE.run();
		return ApiRequest.INSTANCE.redirectLocation;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		String message = 
			this.silent ?
			null :
			this.what.equals( "status" ) ?
			"Loading character status..." :
			this.what.equals( "inventory" ) ?
			"Updating inventory..." :
			this.what.equals( "item" ) ?
			"Looking at item #" + this.id + "..." :
			null;

		if ( message != null )
		{
			KoLmafia.updateDisplay( message );
		}

		this.JSON = null;

		super.run();

		// Save the JSON object so caller can look further at it
		this.JSON = ApiRequest.getJSON( this.responseText, this.what );
	}

	@Override
	public void processResults()
	{
		if ( this.redirectLocation != null )
		{
			return;
		}

		ApiRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern WHAT_PATTERN = Pattern.compile( "what=([^&]*)");

	public static final void parseResponse( final String location, final String responseText )
	{
		Matcher whatMatcher = ApiRequest.WHAT_PATTERN.matcher( location );
		if ( !whatMatcher.find() )
		{
			return;
		}

		String what = whatMatcher.group(1);

		if ( what.equals( "status" ) )
		{
			ApiRequest.parseStatus( responseText );
		}
		else if ( what.equals( "inventory" ) )
		{
			ApiRequest.parseInventory( responseText );
		}
	}

	/*
	  Here's a sample collected on January 8, 2012

	  {
	    "playerid":"121572",
	    "name":"Veracity",
	    "hardcore":"0",
	    "ascensions":"242",
	    "path":"0",
	    "sign":"Mongoose",
	    "roninleft":"0",
	    "casual":"0",
	    "drunk":"24",
	    "full":"15",
	    "turnsplayed":"758794",
	    "familiar":"92",
	    "hp":"2198",
	    "mp":"586",
	    "meat":"5231459",
	    "adventures":"31",
	    "level":"28",
	    "rawmuscle":"589584",
	    "rawmysticality":"267061",
	    "rawmoxie":"267770",
	    "basemuscle":"767",
	    "basemysticality":"516",
	    "basemoxie":"517",
	    "familiarexp":"0",
	    "class":"1",
	    "lastadv":{
	      "id":"338",
	      "name":"Dreadsylvanian Woods",
	      "link":"adventure.php?snarfblat=338",
	      "container":"clan_dreadsylvania.php"
	    },
	    "title":"28",
	    "pvpfights":"100",
	    "maxhp":2326,
	    "maxmp":1409,
	    "spleen":"15",
	    "muscle":1205,
	    "mysticality":811,
	    "moxie":709,
	    "famlevel":16,
	    "locked":false,
	    "daysthisrun":"15",
	    "equipment":{
	      "hat":"5122",
	      "shirt":"2586",
	      "pants":"4306",
	      "weapon":"1325",
	      "offhand":"3543",
	      "acc1":"5460",
	      "acc2":"4309",
	      "acc3":"6530",
	      "container":"6335",
	      "familiarequip":"1325",
	      "fakehands":0,
	      "cardsleeve":"4968"
	    },
	    "stickers":[0,0,0],
	    "eleronkey":"xxx",
	    "flag_config":{
	      "lazyinventory":0,
	      "compactfights":0,
	      "poppvpsearch":0,
	      "questtracker":"1",
	      "charpanepvp":"1",
	      "fffights":"1",
	      "compactchar":0,
	      "noframesize":0,
	      "fullnesscounter":0,
	      "nodevdebug":0,
	      "noquestnudge":"1",
	      "nocalendar":"1",
	      "alwaystag":"1",
	      "clanlogins":0,
	      "quickskills":0,
	      "hprestorers":0,
	      "hidejacko":0,
	      "anchorshelf":0,
	      "showoutfit":"1",
	      "wowbar":0,
	      "swapfam":0,
	      "invimages":0,
	      "showhandedness":0,
	      "acclinks":"1",
	      "invadvancedsort":"1",
	      "powersort":0,
	      "autodiscard":0,
	      "unfamequip":0,
	      "invclose":0,
	      "sellstuffugly":"1",
	      "oneclickcraft":0,
	      "dontscroll":0,
	      "multisume":"1",
	      "threecolinv":"1",
	      "profanity":"1",
	      "tc_updatetitle":0,
	      "tc_alwayswho":0,
	      "tc_times":0,
	      "tc_combineallpublic":0,
	      "tc_eventsactive":0,
	      "tc_hidebadges":0,
	      "tc_colortabs":0,
	      "tc_modifierkey":0,
	      "tc_tabsonbottom":0,
	      "chatversion":"1",
	      "aabosses":0,
	      "compacteffects":0,
	      "slimhpmpdisplay":0,
	      "ignorezonewarnings":"1",
	      "whichpenpal":"1",
	      "compactmanuel":0,
	      "hideefarrows":0,
	      "autoattack":0,
	      "topmenu":0
	    },
	    "recalledskills":1,
	    "freedralph":1,
	    "mcd":0,
	    "pwd":"638cd7484036dcac3bb4039fef93cb4c",
	    "rollover":1378611001,
	    "turnsthisrun":6465,
	    "familiar_wellfed":0,
	    "intrinsics":[],
	    "familiarpic":"timesword",
	    "pathname":"",
	    "effects":{
	      "2d6d3ab04b40e1523aa9c716a04b3aab":["Leash of Linguini","14","string","skill:3010","16"],
	      "ac32e95f470a7e0999863fa0db58d808":["Empathy","29","empathy","skill:2009","50"],
	      "63e73adb3ecfb0cbf544db435eeeaf00":["Fat Leon's Phat Loot Lyric","22","fatleons","skill:6010","67"],
	      "626c8ef76cfc003c6ac2e65e9af5fd7a":["Ode to Booze","18","odetobooze","skill:6014","71"],
	      "bb44871dd165d4dc9b4d35daa46908ef":["Springy Fusilli","4","fusilli","skill:3015","130"],
	      "5c8d3b5b4a6d403f95f27f5d29528c59":["Rage of the Reindeer","4","reindeer","skill:1015","131"],
	      "c45d2469bf8cfd8c9bbc953e2a44f3c4":["Starry-Eyed","8","snowflakes","","350"],
	      "d75aaa2cc6a8dcfa7cdddef658968e26":["Brother Smothers's Blessing","18","monkhead","","461"],
	      "ff0e9ff0189cea375f60b5860463518d":["The Ballad of Richie Thingfinder","4","richie","item:4497","530"],
	      "a614dc2ae977f948665b230f5723b567":["Brass Loins","98","flask","item:6428","1273"],
	      "1839cbfe882d9243964d9097c1a3b0a6":["Silver Age Secrets","98","flask","item:6429","1274"]
	    }
	  }
	*/

	public static final void parseStatus( final String responseText )
	{
		ApiRequest.parseStatus( ApiRequest.getJSON( responseText, "status" ) );
	}

	public static final void parseStatus( final JSONObject JSON )
	{
		if ( JSON == null )
		{
			return;
		}

		// The data in the status is culled from many other KoL
		// pages. Let each page request handler parse the appropriate
		// data from it

		try
		{
			// Pull out the current ascension count. Do this first.
			// Some later processing depends on this.
			int ascensions = JSON.getInt( "ascensions" );
			KoLCharacter.setAscensions( ascensions );

			// Pull out the current password hash
			String pwd = JSON.getString( "pwd" );
			GenericRequest.setPasswordHash( pwd );

			// Many config options are available
			AccountRequest.parseStatus( JSON );

			// Many things from the Char Sheet are available
			CharSheetRequest.parseStatus( JSON );

			// Parse currently worn equipment
			EquipmentManager.parseStatus( JSON );

			// Many things from the Char Pane are available
			CharPaneRequest.parseStatus( JSON );
		}
		catch ( JSONException e )
		{
			ApiRequest.reportParseError( "status", JSON.toString(), e );
		}
		finally
		{
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();

			// Mana cost adjustment may have changed
			KoLConstants.summoningSkills.sort();
			KoLConstants.usableSkills.sort();
		}
	}

	public static final void parseInventory( final String responseText )
	{
		ApiRequest.parseInventory( ApiRequest.getJSON( responseText, "inventory" ) );
	}

	private static final void parseInventory( final JSONObject JSON )
	{
		if ( JSON == null )
		{
			return;
		}

		try
		{
			InventoryManager.parseInventory( JSON );
		}
		catch ( JSONException e )
		{
			ApiRequest.reportParseError( "inventory", JSON.toString(), e );
		}
	}

	public static final JSONObject getJSON( final String text, final String what )
	{
		// Parse the string into a JSON object
		try
		{
			String str = ApiRequest.getJSONString( text );
			return str == null ? null : new JSONObject( str );
		}
		catch ( JSONException e )
		{
			ApiRequest.reportParseError( what, text, e );
		}

		return null;
	}

	private static final String getJSONString( String responseText )
	{
		if ( responseText == null )
		{
			return null;
		}

		int pos = responseText.indexOf( "{" );
		return	pos == -1 ? null :
			pos == 0 ? responseText :
			responseText.substring( pos );
	}

	private static final void reportParseError( final String what, final String responseText, final JSONException e )
	{
		KoLmafia.updateDisplay( "api.php?what=" + what + " parse error: " + e.getMessage() );
		StaticEntity.printStackTrace( e, responseText );
	}
}
