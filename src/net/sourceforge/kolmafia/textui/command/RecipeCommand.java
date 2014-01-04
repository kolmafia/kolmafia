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

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.session.InventoryManager;

public class RecipeCommand
	extends AbstractCommand
{
	public RecipeCommand()
	{
		this.usage = " <item> [, <item>]... - get ingredients or recipe for items.";
	}

	@Override
	public void run( final String cmd, final String params )
	{
		String[] concoctions = params.split( "\\s*,\\s*" );
		
		if ( concoctions.length == 0 )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();

		for ( int i = 0; i < concoctions.length; ++i )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( concoctions[ i ] );
			if ( item == null )
			{
				continue;
			}

			int itemId = item.getItemId();
			String name = item.getName();

			if ( ConcoctionDatabase.getMixingMethod( itemId ) == CraftingType.NOCREATE )
			{
				RequestLogger.printLine( "This item cannot be created: <b>" + name + "</b>" );
				continue;
			}

			buffer.setLength( 0 );
			if ( concoctions.length > 1 )
			{
				buffer.append( String.valueOf( i + 1 ) );
				buffer.append( ". " );
			}

			if ( cmd.equals( "ingredients" ) )
			{
				RecipeCommand.getIngredients( item, buffer );
			}
			else if ( cmd.equals( "recipe" ) )
			{
				RecipeCommand.getRecipe( item, buffer, 0 );
			}

			RequestLogger.printLine( buffer.toString() );
		}
	}
	
	private static void getIngredients( final AdventureResult ar, final StringBuffer sb )
	{
		sb.append( "<b>" );
		sb.append( ar.getInstance( ConcoctionDatabase.getYield( ar.getItemId() ) ).toString() );
		sb.append( "</b>: " );

		List ingredients = RecipeCommand.getFlattenedIngredients( ar, new ArrayList(), false );
		Collections.sort( ingredients );

		Iterator it = ingredients.iterator();
		boolean first = true;
		while ( it.hasNext() )
		{
			AdventureResult ingredient = (AdventureResult) it.next();
			int need = ingredient.getCount();
			int have = InventoryManager.getAccessibleCount( ingredient );
			int missing = need - have;

			if ( !first )
			{
				sb.append( ", " );
			}

			first = false;

			if ( missing < 1 )
			{
				sb.append( ingredient.toString() );
				continue;
			}
		
			sb.append( "<i>" );
			sb.append( ingredient.getName() );
			sb.append( " (" );
			sb.append( String.valueOf( have ) );
			sb.append( "/" );
			sb.append( String.valueOf( need ) );	
			sb.append( ")</i>" );
		}
	}

	private static List getFlattenedIngredients( AdventureResult ar, List list, boolean deep )
	{
		AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( ar.getItemId() );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			if ( ConcoctionDatabase.getMixingMethod( ingredient.getItemId() ) != CraftingType.NOCREATE )
			{
				int have = InventoryManager.getAccessibleCount( ingredient );
				if ( !RecipeCommand.isRecursing( ar, ingredient ) &&
				     ( deep || have == 0 ) )
				{
					RecipeCommand.getFlattenedIngredients( ingredient, list, deep );
					continue;
				}
			}
			AdventureResult.addResultToList( list, ingredient );
		}

		return list;
	}

	private static boolean isRecursing( final AdventureResult parent, final AdventureResult child )
	{
		if ( parent.equals( child ) )
		{
			// should never actually happen, but eh
			return true;
		}

		if ( ConcoctionDatabase.getMixingMethod( parent.getItemId() ) == CraftingType.ROLLING_PIN )
		{
			return true;
		}
		
		AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( child.getItemId() );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[ i ].equals( parent ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static void getRecipe( final AdventureResult ar, final StringBuffer sb, final int depth )
	{
		if ( depth > 0 )
		{
			sb.append( "<br>" );
			for ( int i = 0; i < depth; i++ )
			{
				sb.append( "\u00a0\u00a0\u00a0" );
			}
		}
		
		int itemId = ar.getItemId();
		
		sb.append( "<b>" );
		sb.append( ar.getInstance( ConcoctionDatabase.getYield( ar.getItemId() ) ).toString() );
		sb.append( "</b>" );
		
		CraftingType mixingMethod = ConcoctionDatabase.getMixingMethod( itemId );
		EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements( itemId );
		if ( mixingMethod != CraftingType.NOCREATE )
		{
			sb.append( "<b>:</b> <i>[" );
			sb.append( ConcoctionDatabase.mixingMethodDescription( mixingMethod, requirements ) );
			sb.append( "]</i> " );

			AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( itemId );
			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( i > 0 )
				{
					sb.append( " + " );
				}
				sb.append( ingredient.toString() );
			}

			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( RecipeCommand.isRecursing( ar, ingredient ) )
				{
					continue;
				}
				RecipeCommand.getRecipe( ingredient, sb, depth + 1 );
			}
		}
	}
}
