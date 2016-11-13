/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

package net.sourceforge.kolmafia.utilities;

import java.util.Map.Entry;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.maximizer.Boost;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiUtilities
{
	public static final int ANY_TYPE = 0;
	public static final int ITEM_TYPE = 1;
	public static final int EFFECT_TYPE = 2;
	public static final int SKILL_TYPE = 3;
	public static final int MONSTER_TYPE = 4;

	public static final String getWikiLocation( String name, int type )
	{
		boolean inItemTable = ItemDatabase.containsExactly( name );
		boolean inEffectTable = EffectDatabase.containsExactly( name );
		boolean inSkillTable = SkillDatabase.contains( name );

		if ( type != ANY_TYPE )
		{
			String modType =
				type == ITEM_TYPE ? "Item" :
				type == EFFECT_TYPE ? "Effect" :
				type == SKILL_TYPE ? "Skill" :
				"None";

			Modifiers mods = Modifiers.getModifiers( modType, name );
			if ( mods != null )
			{
				String wikiname = mods.getString( "Wiki Name" );
				if ( wikiname != null && wikiname.length() > 0 )
				{
					name = wikiname;
				}
			}
		}

		switch ( type )
		{
		case ITEM_TYPE:
			if ( name.equals( "sweet tooth" ) ||
			     name.equals( "water wings" ) ||
			     name.equals( "knuckle sandwich" ) ||
			     name.equals( "industrial strength starch" ) )
			{
				// If its not an effect or skill, no disambiguation needed
			}
			else if ( name.equals( "Bulky Buddy Box" ) )
			{
				name = name + " (hatchling)";
			}
			else if ( inEffectTable || inSkillTable )
			{
				name = name + " (item)";
			}
			break;
		case EFFECT_TYPE:
			if ( inItemTable || inSkillTable )
			{
				name = name + " (effect)";
			}
			break;
		case SKILL_TYPE:
			if ( inItemTable || inEffectTable )
			{
				name = name + " (skill)";
			}
			break;
		}

		name = StringUtilities.globalStringReplace( name, "#", "" );
		name = StringUtilities.globalStringReplace( name, "<i>", "" );
		name = StringUtilities.globalStringReplace( name, "</i>", "" );
		name = StringUtilities.globalStringReplace( name, "<s>", "" );
		name = StringUtilities.globalStringReplace( name, "</s>", "" );
		name = StringUtilities.globalStringReplace( name, " ", "_" );

		name = Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );

		// Turn character entities into characters
		name = CharacterEntities.unescape( name );

		// The Wiki does not consistently work with UTF-8 (or ISO-8859-1) encoded URLS
		// name = StringUtilities.getURLEncode( name );

		return "http://kol.coldfront.net/thekolwiki/index.php/" + name;
	}

	public static final String getWikiLocation( Object item )
	{
		if ( item == null )
		{
			return null;
		}

		String name = null;
		int type = WikiUtilities.ANY_TYPE;

		if ( item instanceof Boost )
		{
			item = ((Boost) item).getItem();
		}
		else if ( item instanceof Entry )
		{
			item = ((Entry) item).getValue();
		}

		if ( item instanceof MonsterData )
		{
			name = ((MonsterData)item).getWikiName();
			type = WikiUtilities.MONSTER_TYPE;
		}
		else if ( item instanceof AdventureResult )
		{
			AdventureResult result = (AdventureResult) item;
			name = result.getDataName();

			type =  result.isItem() ?
				WikiUtilities.ITEM_TYPE :
				result.isStatusEffect() ?
				WikiUtilities.EFFECT_TYPE :
				WikiUtilities.ANY_TYPE;
		}
		else if ( item instanceof UseSkillRequest )
		{
			name = ( (UseSkillRequest) item ).getSkillName();
			type = WikiUtilities.SKILL_TYPE;
		}
		else if ( item instanceof Concoction )
		{
			name = ( (Concoction) item ).getName();
			type = WikiUtilities.ITEM_TYPE;
		}
		else if ( item instanceof QueuedConcoction )
		{
			name = ( (QueuedConcoction) item ).getName();
			type = WikiUtilities.ITEM_TYPE;
		}
		else if ( item instanceof CreateItemRequest )
		{
			name = ( (CreateItemRequest) item ).getName();
			type = WikiUtilities.ITEM_TYPE;
		}
		else if ( item instanceof PurchaseRequest )
		{
			name = ( (PurchaseRequest) item ).getItem().getDataName();
			type = WikiUtilities.ITEM_TYPE;
		}
		else if ( item instanceof SoldItem )
		{
			name = ( (SoldItem) item ).getItemName();
			type = WikiUtilities.ITEM_TYPE;
		}
		else if ( item instanceof String )
		{
			name = (String) item;
		}

		if ( name == null )
		{
			return null;
		}

		return WikiUtilities.getWikiLocation( name, type );
	}

	public static final void showWikiDescription( final Object item )
	{
		String location = WikiUtilities.getWikiLocation( item );

		if ( location != null )
		{
			RelayLoader.openSystemBrowser( location );
		}
	}
}
