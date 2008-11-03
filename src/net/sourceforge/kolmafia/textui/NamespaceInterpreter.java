/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;

public class NamespaceInterpreter
	extends Interpreter
{
	private String setting;
	private String lastImportString;

	public NamespaceInterpreter( final String setting )
	{
		super();
		this.setting = setting;
		this.lastImportString = "";
	}

	public Value execute( final String functionName, final String[] parameters )
	{
		String importString = Preferences.getString( setting );
		if ( importString.equals( "" ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "No available namespace with function: " + functionName );
			return DataTypes.VOID_VALUE;
		}

		TreeMap imports = this.parser.getImports();
		boolean shouldRefresh = !this.lastImportString.equals( importString );

		if ( !shouldRefresh )
		{
			Iterator it = imports.entrySet().iterator();

			while ( it.hasNext() && !shouldRefresh )
			{
				Entry entry = (Entry) it.next();

				File file = (File) entry.getValue();
				shouldRefresh = ( (Long) entry.getValue() ).longValue() != file.lastModified();
			}
		}

		if ( shouldRefresh )
		{
			this.scope = new Scope( new VariableList(), Parser.getExistingFunctionScope() );

			imports.clear();

			String[] importList = importString.split( "," );
			for ( int i = 0; i < importList.length; ++i )
			{
				try
				{
					this.parser.importFile( importList[ i ], this.scope );
				}
				catch (ScriptException e )
				{
					// The user changed the script since it was validated
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
					return DataTypes.VOID_VALUE;
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
					return DataTypes.VOID_VALUE;
				}
			}

			this.lastImportString = importString;
		}

		return super.execute( functionName, parameters, shouldRefresh );
	}
}
