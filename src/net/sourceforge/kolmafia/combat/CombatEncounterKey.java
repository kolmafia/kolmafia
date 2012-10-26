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

package net.sourceforge.kolmafia.combat;

import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

public class CombatEncounterKey
{
	private static final Pattern ELEMENT_PATTERN = Pattern.compile( "\\s*\\$element\\[([^\\]]+)\\]" );
	private static final Pattern PHYLUM_PATTERN = Pattern.compile( "\\s*\\$phylum\\[([^\\]]+)\\]" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "\\s*\\$item\\[([^\\]]+)\\]" );

	private String encounterKey;
	private String monsterName;

	private Element element;
	private Phylum phylum;
	private int itemId;

	public CombatEncounterKey( String encounterKey )
	{
		this.encounterKey = encounterKey.trim();
		this.monsterName = this.encounterKey;

		this.element = Element.NONE;
		this.phylum = Phylum.NONE;
		this.itemId = -1;

		Matcher elementMatcher = ELEMENT_PATTERN.matcher( this.monsterName );

		if ( elementMatcher.find() )
		{
			String elementName = elementMatcher.group( 1 );

			this.element = MonsterDatabase.stringToElement( elementName );
		}

		this.monsterName = elementMatcher.replaceAll( "" ).trim();

		Matcher phylumMatcher = PHYLUM_PATTERN.matcher( this.monsterName );

		if ( phylumMatcher.find() )
		{
			String phylumName = phylumMatcher.group( 1 );

			this.phylum = MonsterDatabase.phylumNumber( phylumName );
		}

		this.monsterName = phylumMatcher.replaceAll( "" ).trim();

		Matcher itemMatcher = ITEM_PATTERN.matcher( this.monsterName );

		if ( itemMatcher.find() )
		{
			String itemName = itemMatcher.group( 1 );

			this.itemId = ItemDatabase.getItemId( itemName );
		}

		this.monsterName = itemMatcher.replaceAll( "" ).trim();
	}

	public boolean matches( String monsterName, MonsterData monsterData )
	{
		if ( monsterData != null )
		{
			if ( this.element != Element.NONE && monsterData.getDefenseElement() != this.element )
			{
				return false;
			}

			if ( this.phylum != Phylum.NONE && monsterData.getPhylum() != this.phylum )
			{
				return false;
			}

			if ( this.itemId != -1 )
			{
				boolean foundItem = false;

				Iterator itemIterator = monsterData.getItems().iterator();

				while ( !foundItem && itemIterator.hasNext() )
				{
					AdventureResult item = (AdventureResult) itemIterator.next();

					if ( item.getItemId() == this.itemId )
					{
						foundItem = true;
					}
				}

				if ( !foundItem )
				{
					return false;
				}
			}
		}

		if ( this.monsterName.equals( "" ) )
		{
			return true;
		}

		return monsterName.indexOf( this.monsterName ) != -1;
	}

	@Override
	public String toString()
	{
		return encounterKey;
	}

}