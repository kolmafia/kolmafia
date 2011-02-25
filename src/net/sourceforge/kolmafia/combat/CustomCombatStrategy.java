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

package net.sourceforge.kolmafia.combat;

import java.io.PrintStream;

import javax.swing.tree.DefaultMutableTreeNode;

public class CustomCombatStrategy
	extends DefaultMutableTreeNode
{
	private final String name;

	public CustomCombatStrategy( final String name )
	{
		super( name, true );

		this.name = name;
	}

	public String getAction( final int roundIndex )
	{
		int childCount = getChildCount();

		if ( childCount == 0 )
		{
			return "attack";
		}

		CustomCombatAction actionNode;

		if ( roundIndex >= childCount )
		{
			actionNode = (CustomCombatAction) getLastChild();
		}
		else
		{
			actionNode = (CustomCombatAction) getChildAt( roundIndex );
		}

		if ( actionNode == null )
		{
			return "attack";
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
