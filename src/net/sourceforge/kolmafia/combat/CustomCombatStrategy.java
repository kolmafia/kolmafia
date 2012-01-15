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

import java.io.PrintStream;

import java.util.HashSet;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.kolmafia.KoLmafia;

public class CustomCombatStrategy
	extends DefaultMutableTreeNode
{
	private final String name;

	private int actionCount;
	private int[] actionOffsets;

	public CustomCombatStrategy( final String name )
	{
		super( name, true );

		this.name = name;

		this.resetActionCount();
	}

	public String getName()
	{
		return this.name;
	}

	public void removeAllChildren()
	{
		this.resetActionCount();

		super.removeAllChildren();
	}

	public void resetActionCount()
	{
		this.actionCount = 0;
		this.actionOffsets = null;
	}

	public int getActionCount( CustomCombatLookup lookup, HashSet seen )
	{
		// Ignore any call to a section that results in a loop

		if ( seen.contains( this.name ) )
		{
			KoLmafia.abortAfter( "CCS aborted due to recursive section reference: " + this.name );
			return 0;
		}

		seen.add( this.name );

		// If we've already computed the length, return the length

		if ( actionOffsets != null )
		{
			return this.actionCount;
		}

		int childCount = getChildCount();

		this.actionCount = 0;
		this.actionOffsets = new int[ childCount ];

		for ( int i = 0; i < childCount; ++i )
		{
			this.actionOffsets[ i ] = this.actionCount;

			CustomCombatAction actionNode = (CustomCombatAction) getChildAt( i );
			String sectionReference = actionNode.getSectionReference();

			CustomCombatStrategy strategy = null;

			if ( sectionReference != null )
			{
				strategy = lookup.getStrategy( sectionReference );
			}

			if ( strategy != null )
			{
				this.actionCount += strategy.getActionCount( lookup, seen );
			}
			else if ( sectionReference != null )
			{
				KoLmafia.abortAfter( "CCS aborted due to invalid section reference: " + sectionReference );
			}
			else
			{
				++this.actionCount;
			}
		}

		return this.actionCount;
	}

	public String getAction( final CustomCombatLookup lookup, final int roundIndex, boolean allowMacro )
	{
		int childCount = getChildCount();

		if ( childCount == 0 )
		{
			return "attack";
		}

		getActionCount( lookup, new HashSet() );

		for ( int i = 0; i < childCount; ++i )
		{
			if ( this.actionOffsets[ i ] > roundIndex )
			{
				CustomCombatAction actionNode = (CustomCombatAction) getChildAt( i - 1 );
				String sectionReference = actionNode.getSectionReference();

				if ( sectionReference != null )
				{
					int offset = ( i > 0 ) ? this.actionOffsets[ i - 1 ] : 0;
					CustomCombatStrategy strategy = lookup.getStrategy( sectionReference );

					if ( strategy != null )
					{
						return strategy.getAction( lookup, roundIndex - offset, allowMacro );
					}

					KoLmafia.abortAfter( "CCS aborted due to invalid section reference: " + sectionReference );
					return "abort";
				}

				if ( !allowMacro && actionNode.isMacro() )
				{
					return "skip";
				}

				return actionNode.getAction();
			}
		}

		CustomCombatAction actionNode = (CustomCombatAction) getLastChild();
		String sectionReference = actionNode.getSectionReference();

		if ( sectionReference != null )
		{
			CustomCombatStrategy strategy = lookup.getStrategy( sectionReference );

			if ( strategy != null )
			{
				return strategy.getAction( lookup, roundIndex - this.actionOffsets[ childCount - 1 ], allowMacro );
			}

			KoLmafia.abortAfter( "CCS aborted due to invalid section reference: " + sectionReference );
			return "abort";
		}

		if ( !allowMacro && actionNode.isMacro() )
		{
			return "skip";
		}

		return actionNode.getAction();
	}

	public void addCombatAction( final int roundIndex, final String indent, final String combatAction, boolean isMacro )
	{
		int currentIndex = getChildCount();

		if ( roundIndex <= currentIndex )
		{
			return;
		}

		addRepeatActions( roundIndex, indent );

		CustomCombatAction node = new CustomCombatAction( roundIndex, indent, combatAction, isMacro );

		this.resetActionCount();

		super.add( node );
	}

	private void addRepeatActions( final int roundIndex, final String indent )
	{
		int currentIndex = getChildCount();

		if ( roundIndex <= currentIndex )
		{
			return;
		}

		String repeatAction = "attack with weapon";
		boolean isMacro = false;

		if ( currentIndex > 0 )
		{
			CustomCombatAction node = (CustomCombatAction) getLastChild();

			repeatAction = node.getAction();
			isMacro = node.isMacro();
		}

		for ( int i = currentIndex + 1; i < roundIndex; ++i )
		{
			CustomCombatAction node = new CustomCombatAction( i, indent, repeatAction, isMacro );

			super.add( node );
		}

	}

	public void store( PrintStream writer )
	{
		writer.println( "[ " + this.name + " ]" );

		int childCount = getChildCount();

		for ( int i = 0; i < childCount; ++i )
		{
			CustomCombatAction action = (CustomCombatAction) getChildAt( i );

			action.store( writer );
		}

		writer.println();
	}
}
