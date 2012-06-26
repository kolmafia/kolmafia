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

import javax.swing.tree.DefaultMutableTreeNode;

public class CustomCombatAction
	extends DefaultMutableTreeNode
{
	private final int index;
	private final String action;
	private final String indent;
	private final String actionString;

	private final boolean isMacro;
	private final String sectionReference;

	public CustomCombatAction( final int index, final String indent, final String action, boolean isMacro )
	{
		super( action, false );

		this.index = index;
		this.indent = indent;
		this.isMacro = isMacro;

		if ( isMacro )
		{
			if ( CombatActionManager.isMacroAction( action ) )
			{
				this.action = action;
			}
			else
			{
				this.action = "\"" + action + "\"";
			}
		}
		else
		{
			this.action = CombatActionManager.getLongCombatOptionName( action );
		}

		String actionString = this.index + ": " + this.action.replaceAll( "\\s+", " " );

		this.actionString = actionString;

		if ( this.action.equals( "default" ) )
		{
			this.sectionReference = "default";
		}
		else if ( this.action.startsWith( "section" ) )
		{
			this.sectionReference = CombatActionManager.encounterKey( this.action.substring( 8 ).trim().toLowerCase() );
		}
		else
		{
			this.sectionReference = null;
		}
	}

	public String getAction()
	{
		return this.action;
	}

	public boolean isMacro()
	{
		return this.isMacro;
	}
	
	public String getSectionReference()
	{
		return this.sectionReference;
	}

	@Override
	public String toString()
	{
		return this.actionString;
	}

	public void store( PrintStream writer )
	{
		writer.print( this.indent );
		writer.print( this.action );

		writer.println();
	}
}