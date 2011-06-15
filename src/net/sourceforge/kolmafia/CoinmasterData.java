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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmasterData
	implements Comparable
{
	private final String master;
	private final String token;
	private final String tokenString;
	private final String URL;
	private final String test;
	private final AdventureResult item;
	private final String property;
	private final Pattern itemPattern;
	private final Pattern countPattern;
	private final LockableListModel buyItems;
	private final Map buyPrices;
	private final Map sellPrices;

	public CoinmasterData( 
		final String master,
		final String token,
		final String tokenString,
		final String URL,
		final String test,
		final AdventureResult item,
		final String property,
		final Pattern itemPattern,
		final Pattern countPattern,
		final LockableListModel buyItems,
		final Map buyPrices,
		final Map sellPrices )
	{
		this.master = master;
		this.token = token;
		this.tokenString = tokenString;
		this.URL = URL;
		this.test = test;
		this.item = item;
		this.property = property;
		this.itemPattern = itemPattern;
		this.countPattern = countPattern;
		this.buyItems = buyItems;
		this.buyPrices = buyPrices;
		this.sellPrices = sellPrices;
	}

	public final String getMaster()
	{
		return this.master;
	}

	public final String getToken()
	{
		return this.token;
	}

	public final String getTokenString()
	{
		return this.tokenString;
	}

	public final String getURL()
	{
		return this.URL;
	}

	public final String getTest()
	{
		return this.test;
	}

	public final AdventureResult getItem()
	{
		return this.item;
	}

	public final String getProperty()
	{
		return this.property;
	}

	public final Pattern getItemPattern()
	{
		return this.itemPattern;
	}

	public final Matcher getItemMatcher( final String string )
	{
		return this.itemPattern == null ? null : this.itemPattern.matcher( string );
	}

	public final Pattern getCountPattern()
	{
		return this.countPattern;
	}

	public final Matcher getCountMatcher( final String string )
	{
		return this.countPattern == null ? null : this.countPattern.matcher( string );
	}

	public final LockableListModel getBuyItems()
	{
		return this.buyItems;
	}

	public final Map getBuyPrices()
	{
		return this.buyPrices;
	}

	public final Map getSellPrices()
	{
		return this.sellPrices;
	}

	public String toString()
	{
		return this.master;
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof CoinmasterData && this.master == ( (CoinmasterData) o ).master;
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof CoinmasterData ) ? 1 : this.compareTo( (CoinmasterData) o );
	}

	public int compareTo( final CoinmasterData cd )
	{
		return this.master.compareToIgnoreCase( cd.master );
	}
}
