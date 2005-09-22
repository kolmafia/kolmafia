/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.io.File;

import net.java.dev.spellcast.utilities.LockableListModel;

public class AscensionSnapshotTable extends KoLDatabase
{
	private KoLmafia client;
	private String clanID;
	private String clanName;
	private Map ascensionMap;

	public AscensionSnapshotTable( KoLmafia client )
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		this.client = client;
		this.clanID = clanID;
		this.clanName = clanName;
		this.ascensionMap = new TreeMap();
	}

	public void registerMember( String playerName )
	{
		String lowerCaseName = playerName.toLowerCase();
		ascensionMap.put( lowerCaseName, "" );
	}

	public void unregisterMember( String playerID )
	{
		String lowerCaseName = client.getPlayerName( playerID ).toLowerCase();
		ascensionMap.remove( lowerCaseName );
	}

	public void setClanID( String clanID )
	{	this.clanID = clanID;
	}

	public void setClanName( String clanName )
	{	this.clanName = clanName;
	}

	public Map getAscensionMap()
	{	return ascensionMap;
	}

	public String getAscensionData()
	{
		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head><title>Ascension Data for " + clanName + "(" + (new Date()) + ")</title>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<center><h1>" + clanName + " (#" + clanID + ")</h1></center>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		return strbuf.toString();
	}

	public String getAscensionData( int pathFilter, int classFilter )
	{
		return "";
	}
}