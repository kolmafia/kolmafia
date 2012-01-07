/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import org.json.JSONException;
import org.json.JSONObject;

public class ApiRequest
	extends GenericRequest
{
	private String what;
	private String id;
	public JSONObject JSON;

	public ApiRequest( final String what )
	{
		super( "api.php" );
		this.what = what;
		this.id = "";
		this.addFormField( "what", what );
		this.addFormField( "for", "KoLmafia" );
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

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		if ( this.what.equals( "status" ) )
		{
			KoLmafia.updateDisplay( "Loading character status..." );
		}
		else if ( this.what.equals( "inventory" ) )
		{
			KoLmafia.updateDisplay( "Updating inventory..." );
		}
		else if ( this.what.equals( "item" ) )
		{
			KoLmafia.updateDisplay( "Looking at item #" + this.id + "..." );
		}

		this.JSON = null;
		super.run();
	}

	public void processResults()
	{
		if ( this.redirectLocation != null )
		{
			return;
		}

		// Save the JSON object so caller can look further at it
		this.JSON = ApiRequest.getJSON( this.responseText, this.what );
		if ( this.what.equals( "status" ) )
		{
			ApiRequest.parseStatus( this.JSON );
		}
		else if ( this.what.equals( "inventory" ) )
		{
			ApiRequest.parseInventory( this.JSON );
		}
	}

	private static final Pattern WHAT_PATTERN =
		Pattern.compile( "what=([^&]*)");

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
	  Here's a sample collected on February 7, 2011

	{
	  "playerid":"121572",
	  "name":"Veracity",
	  "hardcore":"0",
	  "ascensions":"149",
	  "path":"None",
	  "sign":"Wallaby",
	  "roninleft":"0",
	  "casual":"0",
	  "drunk":"0",
	  "turnsplayed":"504202",
	  "familiar":"89",
	  "hp":"395",
	  "mp":"28",
	  "meat":"6086553",
	  "adventures":"127",
	  "level":"18",
	  "rawmuscle":"49201",
	  "rawmysticality":"49268",
	  "rawmoxie":"95116",
	  "basemuscle":"221",
	  "basemysticality":"221",
	  "basemoxie":"308",
	  "familiarexp":400,
	  "class":"5",
	  "lastadv":{
	    "id":"82",
	    "name":"The Castle in the Sky",
	    "link":"adventure.php?snarfblat=82",
	    "container":"beanstalk.php"
	  },
	  "title":"18",
	  "maxhp":395,
	  "maxmp":586,
	  "muscle":255,
	  "mysticality":332,
	  "moxie":324,
	  "famlevel":40,
	  "equipment":{
	    "hat":"1323",
	    "shirt":"2586",
	    "pants":"1324",
	    "weapon":"1325",
	    "offhand":"1325",
	    "acc1":"3337",
	    "acc2":"1232",
	    "acc3":"1226",
	    "familiarequip":"3343"
	  },
	  "flag_config":{
	    "questtracker":"1",
	    "compactchar":0,
	    "nodevdebug":0,
	    "noquestnudge":"1",
	    "nocalendar":"1",
	    "alwaystag":"1",
	    "clanlogins":0,
	    "quickskills":0,
	    "hprestorers":0,
	    "anchorshelf":0,
	    "showoutfit":0,
	    "wowbar":0,
	    "swapfam":0,
	    "invimages":0,
	    "showhandedness":0,
	    "acclinks":"1",
	    "powersort":0,
	    "autodiscard":0,
	    "unfamequip":0,
	    "invclose":0,
	    "sellstuffugly":0,
	    "oneclickcraft":0,
	    "dontscroll":0,
	    "multisume":"1",
	    "threecolinv":"1",
	    "profanity":"1",
	    "autoattack":0,
	    "topmenu":0
	    },
	  "pwd":"8474fcc985a4ace1bcac93ced6825196",
	  "rollover":"1297135800",
	  "turnsthisrun":5361,
	  "familiarpic":"hobomonkey",
	  "effects":{
	    "2d6d3ab04b40e1523aa9c716a04b3aab":["Leash of Linguini","13","string"],
	    "ac32e95f470a7e0999863fa0db58d808":["Empathy","13","empathy"],
	    "63e73adb3ecfb0cbf544db435eeeaf00":["Fat Leon's Phat Loot Lyric","13","fatleons"],
	    "626c8ef76cfc003c6ac2e65e9af5fd7a":["Ode to Booze","9","odetobooze"],
	    "bb44871dd165d4dc9b4d35daa46908ef":["Springy Fusilli","3","fusilli"],
	    "5c8d3b5b4a6d403f95f27f5d29528c59":["Rage of the Reindeer","8","reindeer"],
	    "06e123f9b46d3d180c97efc5ab0ad150":["Carlweather's Cantata of Confrontation","6","cantata"],
	    "5e788aac76c7451c42ce19d2acf6de18":["Musk of the Moose","3","stench"],
	    "a32acc4a5de83386ae3417140d09bf43":["Jingle Jangle Jingle","28","jinglebells"],
	    "37f9f99df81ca3d29abe85139a302f67":["Hip to Be Square Dancin'",10,"dance2"]
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
			GenericRequest.passwordHash = pwd;

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
