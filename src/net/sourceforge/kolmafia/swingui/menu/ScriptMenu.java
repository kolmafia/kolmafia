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

package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.GenericFrame;

/**
 * A special class which renders the list of available scripts.
 */

public class ScriptMenu
	extends MenuItemList
{
	public ScriptMenu()
	{
		super( "Scripts", KoLConstants.scripts );
	}

	@Override
	public JComponent constructMenuItem( final Object o )
	{
		return o instanceof JSeparator ? new JSeparator() : this.constructMenuItem( (File) o, "scripts" );
	}

	private JComponent constructMenuItem( final File file, final String prefix )
	{
		// Get path components of this file
		String[] pieces;

		try
		{
			pieces = file.getCanonicalPath().split( "[\\\\/]" );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}

		String name = pieces[ pieces.length - 1 ];
		String path = prefix + File.separator + name;

		if ( file.isDirectory() )
		{
			// Get a list of all the files
			File[] scriptList = DataUtilities.listFiles( file );

			//  Convert the list into a menu
			JMenu menu = new JMenu( name );

			// Iterate through the files.  Do this in two
			// passes to make sure that directories start
			// up top, followed by non-directories.

			for ( int i = 0; i < scriptList.length; ++i )
			{
				if ( scriptList[ i ].isDirectory() && ScriptMenu.shouldAddScript( scriptList[ i ] ) )
				{
					menu.add( this.constructMenuItem( scriptList[ i ], path ) );
				}
			}

			for ( int i = 0; i < scriptList.length; ++i )
			{
				if ( !scriptList[ i ].isDirectory() )
				{
					menu.add( this.constructMenuItem( scriptList[ i ], path ) );
				}
			}

			// Return the menu
			return menu;
		}

		return new LoadScriptMenuItem( name, path );
	}

	@Override
	public JComponent[] getHeaders()
	{
		JComponent[] headers = new JComponent[ 4 ];

		headers[ 0 ] = new DisplayFrameMenuItem( "Script Manager", "ScriptManageFrame" );
		headers[ 1 ] = new LoadScriptMenuItem();
		headers[ 2 ] = new InvocationMenuItem( "Refresh menu", GenericFrame.class, "compileScripts" );
		headers[ 3 ] = new JMenuItem( "(Shift key to edit)" );
		headers[ 3 ].setEnabled( false );

		return headers;
	}

	public static final boolean shouldAddScript( final File script )
	{
		if ( !script.isDirectory() )
		{
			return true;
		}
	
		File[] scriptList = DataUtilities.listFiles( script );
	
		if ( scriptList == null || scriptList.length == 0 )
		{
			return false;
		}
	
		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( ScriptMenu.shouldAddScript( scriptList[ i ] ) )
			{
				return true;
			}
		}
	
		return false;
	}
}
