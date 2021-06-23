/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.*;

import java.util.List;

public class JsRefCommand
	extends AbstractCommand
{
	public JsRefCommand()
	{
		this.usage = " [<filter>] - summarize JS built-in functions [matching filter].";
	}

	private String toObjectKeyType( final Type type )
	{
		String jsTypeName = toJavascriptTypeName( type );
		if ( jsTypeName.equals( "number" ) || jsTypeName.equals( "string" ) )
		{
			return jsTypeName;
		}

		return type.toString() + ": string";
	}

	private String toJavascriptTypeName( final Type type )
	{
		if ( type == null || type.toString() == null )
		{
			return "null";
		}

		if ( type instanceof AggregateType )
		{
			if ( ((AggregateType) type).getSize() < 0 )
			{
				return "{ [" + toObjectKeyType( ((AggregateType) type).getIndexType() ) + "]: " + toJavascriptTypeName( ((AggregateType) type).getDataType() ) + " }";
			}
			else
			{
				return 	toJavascriptTypeName( ((AggregateType) type).getDataType() ) + "[]";
			}
		}

		if ( DataTypes.enumeratedTypes.contains( type ) )
		{
			return JavascriptRuntime.capitalize( type.toString() );
		}

		if ( type instanceof RecordType )
		{
			StringBuilder object = new StringBuilder( "{ " );
			for (int i = 0 ; i < ((RecordType) type).fieldCount(); i++)
			{
				object.append( ((RecordType) type).getFieldNames()[i] ).append( ": " ).append( toJavascriptTypeName( ((RecordType) type).getFieldTypes()[i] ) ).append( "; " );
			}
			return object + "}";
		}

		switch( type.toString() )
		{
		case "int":
		case "float":
			return "number";
		case "buffer":
		case "strict_string":
			return "string";
		default:
			return type.toString();
		}
	}

	@Override
	public void run( final String cmd, String filter )
	{
		boolean addLinks = StaticEntity.isGUIRequired();

		List<Function> functions = JavascriptRuntime.getFunctions();

		if ( functions.isEmpty() )
		{
			RequestLogger.printLine( "No functions in your current namespace." );
			return;
		}

		filter = filter.toLowerCase();

		for( Function func : functions  )
		{
			boolean matches = filter.equals( "" );

			String funcName = func.getName();
			String jsFuncName = JavascriptRuntime.toCamelCase( funcName );

			if ( !matches )
			{
				matches = funcName.toLowerCase().contains( filter );
				matches |= jsFuncName.toLowerCase().contains( filter );
			}

			if ( !matches )
			{
				for ( VariableReference ref : func.getVariableReferences() )
				{
					String refType = ref.getType().toString();
					if ( refType != null )
					{
						matches = refType.toLowerCase().contains( filter );
						matches |= JavascriptRuntime.toCamelCase( refType ).toLowerCase().contains( filter );
					}
				}
			}

			if ( !matches )
			{
				continue;
			}

			StringBuilder description = new StringBuilder();

			description.append( toJavascriptTypeName( func.getType() ) );
			description.append( " " );
			if ( addLinks )
			{
				description.append( "<a href='https://wiki.kolmafia.us/index.php?title=" );
				description.append( funcName );
				description.append( "'>" );
			}
			description.append( jsFuncName );
			if ( addLinks )
			{
				description.append( "</a>" );
			}
			description.append( "(" );

			String sep = "";
			for ( VariableReference var : func.getVariableReferences() )
			{
				description.append( sep );
				sep = ", ";

				description.append( toJavascriptTypeName( var.getRawType() ) );

				if ( var.getName() != null )
				{
					description.append( " " );
					description.append( var.getName() );
				}
			}

			description.append( ")" );

			RequestLogger.printLine( description.toString() );
		}
	}
}
