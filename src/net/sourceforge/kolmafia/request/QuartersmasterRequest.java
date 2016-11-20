/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.IslandManager;

public class QuartersmasterRequest
	extends CoinMasterRequest
{
	public static final String master = "Quartersmaster"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( QuartersmasterRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( QuartersmasterRequest.master );
	private static final LockableListModel<AdventureResult> sellItems = CoinmastersDatabase.getSellItems( QuartersmasterRequest.master );
	private static final Map<Integer, Integer> sellPrices = CoinmastersDatabase.getSellPrices( QuartersmasterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You've.*?got ([\\d,]+) quarter" );
	public static final CoinmasterData FRATBOY =
		new CoinmasterData(
			"Quartersmaster",
			"quartersmaster",
			QuartersmasterRequest.class,
			"quarter",
			"You don't have any quarters",
			false,
			QuartersmasterRequest.TOKEN_PATTERN,
			null,
			"availableQuarters",
			null,
			"bigisland.php?place=camp&whichcamp=2",
			"getgear",
			QuartersmasterRequest.buyItems,
			QuartersmasterRequest.buyPrices,
			"bigisland.php?place=camp&whichcamp=2",
			"turnin",
			QuartersmasterRequest.sellItems,
			QuartersmasterRequest.sellPrices,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.TEQUILA_GRENADE:
				case ItemPool.MOLOTOV_COCKTAIL_COCKTAIL:
					return Preferences.getString( "sidequestLighthouseCompleted" ).equals( "fratboy" );
				}
				return super.canBuyItem( itemId );
			}
		};

	static
	{
		ConcoctionPool.set( new Concoction( "quarter", "availableQuarters" ) );
	};

	public QuartersmasterRequest()
	{
		super( QuartersmasterRequest.FRATBOY );
	}

	public QuartersmasterRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( QuartersmasterRequest.FRATBOY, buying, attachments );
	}

	public QuartersmasterRequest( final boolean buying, final AdventureResult attachment )
	{
		super( QuartersmasterRequest.FRATBOY, buying, attachment );
	}

	public QuartersmasterRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( QuartersmasterRequest.FRATBOY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		CoinMasterRequest.parseResponse( QuartersmasterRequest.FRATBOY, this.getURLString(), this.responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) || urlString.indexOf( "whichcamp=2" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = QuartersmasterRequest.FRATBOY;
		IslandRequest.lastCampVisited = data;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		if ( !IslandManager.warProgress().equals( "started" ) )
		{
			return "You're not at war.";
		}

		if ( !EquipmentManager.hasOutfit( OutfitPool.WAR_FRAT_OUTFIT ) )
		{
			return "You don't have the Frat Warrior Fatigues";
		}

		return null;
	}

	@Override
	public void equip()
	{
		if ( !EquipmentManager.isWearingOutfit( OutfitPool.WAR_FRAT_OUTFIT ) )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( OutfitPool.WAR_FRAT_OUTFIT );
			EquipmentRequest request = new EquipmentRequest( outfit );
			RequestThread.postRequest( request );
		}
	}
}
