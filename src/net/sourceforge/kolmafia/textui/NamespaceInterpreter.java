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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui;

import java.io.File;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;

public class NamespaceInterpreter
	extends Interpreter
{
	private String lastImportString;

	public NamespaceInterpreter()
	{
		super();
		refresh( "" );
	}

	public Value execute( final String functionName, final String[] parameters )
	{
		String importString = Preferences.getString( "commandLineNamespace" );

		boolean shouldRefresh = !this.lastImportString.equals( importString );

		if ( !shouldRefresh )
		{
			TreeMap imports = this.parser.getImports();
			Iterator it = imports.entrySet().iterator();

			while ( it.hasNext() && !shouldRefresh )
			{
				Entry entry = (Entry) it.next();
				File file = (File) entry.getKey();
				Long date = (Long) entry.getValue();
				shouldRefresh = date.longValue() != file.lastModified();
			}
		}

		if ( shouldRefresh && !refresh( importString ) )
		{
			return DataTypes.VOID_VALUE;
		}

		return super.execute( functionName, parameters, shouldRefresh );
	}
	
	private boolean refresh( String importString )
	{
		this.scope = new Scope( new VariableList(), Parser.getExistingFunctionScope() );

		TreeMap imports = this.parser.getImports();
		imports.clear();
	
		if ( importString.length() > 0 )
		{
			String[] importList = importString.split( "," );

			for ( int i = 0; i < importList.length; ++i )
			{
				try
				{
					this.parser.importFile( importList[ i ], this.scope );
				}
				catch ( ScriptException e )
				{
					// The user changed the script since it was validated
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
					return false;
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
					return false;
				}
			}
		}
		
		this.lastImportString = importString;
		return true;
	}
}
