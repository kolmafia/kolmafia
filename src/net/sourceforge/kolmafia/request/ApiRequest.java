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
	private String what;
	private String id;
	public JSONObject JSON;

	public ApiRequest()
	{
		this( "status" );
	}

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

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
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

	@Override
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
	  Here's a sample collected on January 8, 2012

	  {
	    "playerid":"121572",
	    "name":"Veracity",
	    "hardcore":"0",
	    "ascensions":"170",
	    "path":"0",
	    "sign":"Mongoose",
	    "roninleft":"0",
	    "casual":"0",
	    "drunk":"0",
	    "full":"0",
	    "turnsplayed":"584236",
	    "familiar":"120",
	    "hp":"2706",
	    "mp":"2158",
	    "meat":"12717722",
	    "adventures":"55",
	    "level":"28",
	    "rawmuscle":"615063",
	    "rawmysticality":"295937",
	    "rawmoxie":"290147",
	    "basemuscle":"784",
	    "basemysticality":"544",
	    "basemoxie":"538",
	    "familiarexp":10000,
	    "class":"2",
	    "lastadv":{
	      "id":"82",
	      "name":"The Castle in the Sky",
	      "link":"adventure.php?snarfblat=82",
	      "container":"beanstalk.php"
	    },
	    "title":"28",
	    "maxhp":2666,
	    "maxmp":2118,
	    "muscle":952,
	    "mysticality":816,
	    "moxie":592,
	    "famlevel":120,
	    "daysthisrun":"136",
	    "equipment":{
	      "hat":"4614",
	      "shirt":"3777",
	      "pants":"4268",
	      "weapon":"3390",
	      "offhand":"4389",
	      "acc1":"4644",
	      "acc2":"2844",
	      "acc3":"5460",
	      "familiarequip":"4329",
	      "fakehands":0
	    },
	    "stickers":[0,0,0],
	    "flag_config":{
	      "lazyinventory":0,
	      "questtracker":"1",
	      "compactchar":0,
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
	      "showoutfit":0,
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
	      "autoattack":0,
	      "topmenu":0
	    },
	    "recalledskills":1,
	    "freedralph":1,
	    "mcd":"10",
	    "pwd":"2d57ccd7c8359633c21aaa2bba32a5f9",
	    "rollover":null,
	    "turnsthisrun":23952,
	    "familiarpic":"smimic",
	    "pathname":"",
	    "effects":{
	      "2d6d3ab04b40e1523aa9c716a04b3aab":["Leash of Linguini","1","string"],
	      "ac32e95f470a7e0999863fa0db58d808":["Empathy","10","empathy"],
	      "c26a911b8ec2c57f7eef57f9ff5fdc24":["Polka of Plenty","39","plenty"],
	      "63e73adb3ecfb0cbf544db435eeeaf00":["Fat Leon's Phat Loot Lyric","6","fatleons"],
	      "bb44871dd165d4dc9b4d35daa46908ef":["Springy Fusilli","4","fusilli"],
	      "5c8d3b5b4a6d403f95f27f5d29528c59":["Rage of the Reindeer","4","reindeer"],
	      "59a1ac48440e1ea1f869f2b37753f9cc":["Ur-Kel's Aria of Annoyance","15","urkels"],
	      "5e788aac76c7451c42ce19d2acf6de18":["Musk of the Moose","4","stench"],
	      "a32acc4a5de83386ae3417140d09bf43":["Jingle Jangle Jingle","10166","jinglebells"]
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
