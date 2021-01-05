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

package net.sourceforge.kolmafia.textui.javascript;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.parsetree.Type;

public class EnumeratedWrapperPrototype
	extends ScriptableObject
{
	private static final long serialVersionUID = 1L;

	private static Map<Scriptable, TreeMap<Type, EnumeratedWrapperPrototype>> registry = new HashMap<>();

	private Class<?> recordValueClass;
	private Type type;

	public EnumeratedWrapperPrototype( Class<?> recordValueClass, Type type )
	{
		this.recordValueClass = recordValueClass;
		this.type = type;
	}

	public void initToScope( Context cx, Scriptable scope )
	{
		setPrototype( ScriptableObject.getObjectPrototype( scope ) );

		if ( recordValueClass != null )
		{
			for ( Method method : recordValueClass.getDeclaredMethods() )
			{
				if ( method.getName().startsWith("get_") )
				{
					ProxyRecordMethodWrapper methodWrapper = new ProxyRecordMethodWrapper( scope, ScriptableObject.getFunctionPrototype( scope ), method );
					String methodShortName = JavascriptRuntime.toCamelCase( method.getName().replace( "get_", "" ) );
					setGetterOrSetter( methodShortName, 0, methodWrapper, false );
				}
			}
		}

		try
		{
			Method constructorMethod = EnumeratedWrapper.class.getDeclaredMethod( "constructDefaultValue" );
			FunctionObject constructor = new FunctionObject( getClassName(), constructorMethod, scope );
			constructor.addAsConstructor( scope, this );

			Method getMethod = EnumeratedWrapper.class.getDeclaredMethod( "genericGet",
                    Context.class, Scriptable.class, Object[].class, Function.class );
			Function getFunction = new FunctionObject( "get", getMethod, scope );
			ScriptableObject.defineProperty( getFunction, "typeName", getClassName(), DONTENUM | READONLY | PERMANENT );
			constructor.defineProperty( "get", getFunction, DONTENUM | READONLY | PERMANENT );

			Method allMethod = EnumeratedWrapper.class.getDeclaredMethod( "all",
                    Context.class, Scriptable.class, Object[].class, Function.class );
			Function allFunction = new FunctionObject( "all", allMethod, scope );
			ScriptableObject.defineProperty( allFunction, "typeName", getClassName(), DONTENUM | READONLY | PERMANENT );
			constructor.defineProperty( "all", allFunction, DONTENUM | READONLY | PERMANENT );

			constructor.sealObject();

			for ( String methodName : new String[] { "toString", "valueOf" } )
			{
				Method method = EnumeratedWrapper.class.getDeclaredMethod( methodName );
				FunctionObject functionObject = new FunctionObject( methodName, method, scope );
				defineProperty( methodName, functionObject, DONTENUM | READONLY | PERMANENT );
				functionObject.sealObject();
			}
		}
		catch ( NoSuchMethodException e )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "NoSuchMethodException: " + e.getMessage() );
		}

		sealObject();

		if ( !registry.containsKey( scope ) )
		{
			registry.put( scope, new TreeMap<>() );
		}
		registry.get( scope ).put( type, this );
	}

	public static EnumeratedWrapperPrototype getPrototypeInstance( Scriptable scope, Type type )
	{
		Scriptable topScope = ScriptableObject.getTopLevelScope( scope );
		Object constructor = ScriptableObject.getProperty( topScope, getClassName( type ) );
		if ( !( constructor instanceof Scriptable ) )
		{
			return null;
		}
		Object result = ScriptableObject.getProperty( (Scriptable) constructor, "prototype" );
		return result instanceof EnumeratedWrapperPrototype ? (EnumeratedWrapperPrototype) result : null;
	}

	public static String getClassName( Type type )
	{
		return JavascriptRuntime.capitalize( type.getName() );
	}

	public String getClassName()
	{
		return EnumeratedWrapperPrototype.getClassName( type );
	}
}
