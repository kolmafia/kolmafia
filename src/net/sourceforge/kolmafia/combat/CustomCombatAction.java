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

import net.sourceforge.kolmafia.KoLConstants;

public class CustomCombatAction
	extends DefaultMutableTreeNode
{
	private final int index;
	private final String action;
	private final String actionString;
	private final boolean isMacro;

	public CustomCombatAction( final int index, final String action, boolean isMacro )
	{
		super( action, false );

		this.index = index;
		this.isMacro = isMacro;

		if ( isMacro )
		{
			this.action = CombatActionManager.getLongCombatOptionName( action );
		}
		else
		{
			this.action = action;
		}

		String actionString = this.index + ": " + this.action.replaceAll( "\\s+", " " );

		if ( actionString.length() > 20 )
		{
			this.actionString = actionString.substring( 0, 20 ) + "...";
		}
		else
		{
			this.actionString = actionString;
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

	public String toString()
	{
		return this.actionString;
	}

	public void store( PrintStream writer )
	{
		if ( this.isMacro )
		{
			// Add a single line containing a quote if KoLmafia can't figure out that this is a macro from the first line.

			String testLine = this.action;
			int pos = this.action.indexOf( KoLConstants.LINE_BREAK );

			if ( pos != -1 )
			{
				testLine = this.action.substring( 0, pos );
			}

			if ( !CombatActionManager.isMacroAction( testLine ) )
			{
				writer.println( "\"" );
			}

			writer.println( this.action );
		}
		else
		{
			writer.println( this.index + ": " + this.action );
		}
	}
}