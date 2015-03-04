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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.textui.DataTypes;

import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiLookupCommand
	extends AbstractCommand
{
	public WikiLookupCommand()
	{
		this.usage = " [effect|familiar|item|skill|outfit|monster|location] <target> - go to appropriate KoL Wiki page. If not specified, defaults to effect then item if no matches.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		int index = parameters.indexOf( " " );
		String type = null;
		String target = parameters;
		if ( index != -1 )
		{
			type = parameters.substring( 0, index ).toLowerCase().trim();
			target = parameters.substring( index + 1 ).trim();
		}
		if ( type == null || ( !type.startsWith( "effect" ) && !type.startsWith( "familiar" ) &&
			!type.startsWith( "item" ) && !type.startsWith( "skill" ) && !type.startsWith( "outfit" ) &&
			!type.startsWith( "monster" ) && !type.startsWith( "location" ) ) )
		{
			// No type specified
			type = "item or effect";
		}
		if ( command.equals( "lookup" ) && target.length() > 0 )
		{
			if ( type.startsWith( "effect" ) || type.startsWith( "item or effect" ) )
 			{
				List names = EffectDatabase.getMatchingNames( target );
				if ( names.size() == 1 )
				{
					int effectId = EffectDatabase.getEffectId( (String) names.get( 0 ) );
					AdventureResult result = EffectPool.get( effectId );
					WikiUtilities.showWikiDescription( result );
					return;
				}
			}

			if ( type.startsWith( "familiar" ) )
			{
				int num = FamiliarDatabase.getFamiliarId( target );
				if ( num != -1 )
				{
					target = FamiliarDatabase.getFamiliarName( num );
				}
				WikiUtilities.showWikiDescription( target );
 				return;
 			}

			if ( type.startsWith( "item" ) )
			{
				AdventureResult result = ItemFinder.getFirstMatchingItem( target, ItemFinder.ANY_MATCH );
				if ( result != null )
				{
					WikiUtilities.showWikiDescription( result );
					return;
				}
			}

			if ( type.startsWith( "skill" ) )
			{
				List names = SkillDatabase.getMatchingNames( target );
				if ( names.size() == 1 )
				{
					int num = SkillDatabase.getSkillId( (String) names.get( 0 ) );
					target = SkillDatabase.getSkillDataName( num );
					WikiUtilities.showWikiDescription( target );
					return;
				}
			}

			if ( type.startsWith( "outfit" ) )
			{
				SpecialOutfit so = EquipmentManager.getMatchingOutfit( target.toString() );
				if ( so != null )
				{
					WikiUtilities.showWikiDescription( so.toString() );
					return;
				}
			}

			if ( type.startsWith( "monster" ) )
			{
				MonsterData monster = MonsterDatabase.findMonster( target, true );
				if ( monster != null )
				{
					WikiUtilities.showWikiDescription( monster.getName() );
					return;
				}
			}

			if ( type.startsWith( "location" ) )
			{
				KoLAdventure content = AdventureDatabase.getAdventure( target );
				if ( content != null )
				{
					WikiUtilities.showWikiDescription( content.getAdventureName() );
					return;
				}
			}
 		}

		RelayLoader.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + StringUtilities.getURLEncode( target ) + "&go=Go" );
	}
}
