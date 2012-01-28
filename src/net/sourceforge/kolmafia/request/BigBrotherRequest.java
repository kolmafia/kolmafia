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

package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

public class BigBrotherRequest
	extends CoinMasterRequest
{
	public static final String master = "Big Brother"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BigBrotherRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BigBrotherRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? sand dollar" );
	public static final AdventureResult SAND_DOLLAR = ItemPool.get( ItemPool.SAND_DOLLAR, 1 );

	public static final CoinmasterData BIG_BROTHER =
		new CoinmasterData(
			BigBrotherRequest.master,
			BigBrotherRequest.class,
			"monkeycastle.php?who=2",
			"sand dollar",
			"You haven't got any sand dollars",
			false,
			BigBrotherRequest.TOKEN_PATTERN,
			BigBrotherRequest.SAND_DOLLAR,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buyitem",
			BigBrotherRequest.buyItems,
			BigBrotherRequest.buyPrices,
			null,
			null
			);

	public static final AdventureResult AERATED_DIVING_HELMET = ItemPool.get( ItemPool.AERATED_DIVING_HELMET, 1 );
	public static final AdventureResult SCUBA_GEAR = ItemPool.get( ItemPool.SCUBA_GEAR, 1 );
	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult AMPHIBIOUS_TOPHAT = ItemPool.get( ItemPool.AMPHIBIOUS_TOPHAT, 1 );
	public static final AdventureResult BUBBLIN_STONE = ItemPool.get( ItemPool.BUBBLIN_STONE, 1 );

	private static AdventureResult self = null;
	private static AdventureResult familiar = null;
	private static boolean rescuedBigBrother = false;
	private static boolean waterBreathingFamiliar = false;

	public BigBrotherRequest()
	{
		super( BigBrotherRequest.BIG_BROTHER );
	}

	public BigBrotherRequest( final String action )
	{
		super( BigBrotherRequest.BIG_BROTHER, action );
	}

	public BigBrotherRequest( final String action, final int itemId, final int quantity )
	{
		super( BigBrotherRequest.BIG_BROTHER, action, itemId, quantity );
	}

	public BigBrotherRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public BigBrotherRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public void processResults()
	{
		BigBrotherRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "who=2" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	private static void update()
	{
		if ( InventoryManager.getCount( BigBrotherRequest.AERATED_DIVING_HELMET ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.AERATED_DIVING_HELMET;
			BigBrotherRequest.rescuedBigBrother = true;
		}
		else if ( InventoryManager.getCount( BigBrotherRequest.SCUBA_GEAR ) > 0 )
		{
			BigBrotherRequest.self = BigBrotherRequest.SCUBA_GEAR;
			BigBrotherRequest.rescuedBigBrother = InventoryManager.getCount( BigBrotherRequest.BUBBLIN_STONE ) > 0;
		}
		else
		{
			BigBrotherRequest.rescuedBigBrother = false;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();

		// Check if the familiar is inherently water breathing
		BigBrotherRequest.waterBreathingFamiliar = familiar.waterBreathing();

		// For the dancing frog, the amphibious tophat is the best familiar equipment
		if ( familiar.getId() == FamiliarPool.DANCING_FROG &&
		     InventoryManager.getCount( BigBrotherRequest.AMPHIBIOUS_TOPHAT ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.AMPHIBIOUS_TOPHAT;
		}
		else if ( InventoryManager.getCount( BigBrotherRequest.DAS_BOOT ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.DAS_BOOT;
		}
		else if ( InventoryManager.getCount( BigBrotherRequest.BATHYSPHERE ) > 0 )
		{
			BigBrotherRequest.familiar = BigBrotherRequest.BATHYSPHERE;
		}
	}

	public static String accessible()
	{
		BigBrotherRequest.update();

		if ( !BigBrotherRequest.rescuedBigBrother )
		{
			return "You haven't rescued Big Brother yet.";
		}

		if ( BigBrotherRequest.self == null )
		{
			return "You don't have the right equipment to adventure underwater.";
		}

		if ( !BigBrotherRequest.waterBreathingFamiliar && BigBrotherRequest.familiar == null )
		{
			return "Your familiar doesn't have the right equipment to adventure underwater.";
		}

		return null;
	}

	public void equip()
	{
		BigBrotherRequest.update();

		if ( !KoLCharacter.hasEquipped( BigBrotherRequest.self ) )
		{
			EquipmentRequest request = new EquipmentRequest( BigBrotherRequest.self );
			RequestThread.postRequest( request );
		}

		if ( !BigBrotherRequest.waterBreathingFamiliar && !KoLCharacter.hasEquipped( BigBrotherRequest.familiar ) )
		{
			EquipmentRequest request = new EquipmentRequest( familiar );
			RequestThread.postRequest( request );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim monkeycastle.php?action=buyitem or
		// monkeycastle.php?who=2
		if ( !urlString.startsWith( "monkeycastle.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action" ) == -1 && urlString.indexOf( "who=2" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
