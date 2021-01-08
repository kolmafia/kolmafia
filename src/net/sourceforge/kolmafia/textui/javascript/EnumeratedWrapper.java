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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class EnumeratedWrapper
	extends ScriptableObject
{
	private static final long serialVersionUID = 1L;

	// Make sure each wrapper is a singleton, so that equality comparison works in JS.
	private static final Map<Scriptable, Map<Value, EnumeratedWrapper>> registry = new HashMap<>();

	private final Class<?> recordValueClass;
	// NB: This wrapped value is NOT the proxy record type version.
	// Instead, it's the plain Value that can be turned into a proxy record via asProxy.
	private final Value wrapped;

	private EnumeratedWrapper( Class<?> recordValueClass, Value wrapped )
	{
		this.recordValueClass = recordValueClass;
		this.wrapped = wrapped;
	}

	public static EnumeratedWrapper wrap( Scriptable scope, Class<?> recordValueClass, Value wrapped )
	{
		scope = getTopLevelScope( scope );
		Scriptable proto = scope.getPrototype();
		while ( proto != null && proto != getObjectPrototype( scope ) ) {
			scope = proto;
			proto = scope.getPrototype();
		}

		Map<Value, EnumeratedWrapper> subRegistry = registry.getOrDefault( scope, null );
		if ( subRegistry == null )
		{
			subRegistry = new HashMap<>();
			registry.put( scope, subRegistry );
		}

		EnumeratedWrapper existing = subRegistry.getOrDefault( wrapped, null );
		if ( existing == null )
		{
			existing = new EnumeratedWrapper( recordValueClass, wrapped );
			existing.setPrototype( EnumeratedWrapperPrototype.getPrototypeInstance( scope, wrapped.getType() ) );
			existing.sealObject();
			subRegistry.put( wrapped, existing );
		}

		return existing;
	}

	public static void cleanup( Scriptable scope )
	{
		registry.remove( scope );
	}

	public Value getWrapped()
	{
		return wrapped;
	}

	@Override
	public String getClassName()
	{
		return recordValueClass.getName();
	}

	@Override
	public String toString()
	{
		return wrapped.toString();
	}

	public int valueOf()
	{
		// Rhino doesn't translate longs correctly, so this has to be an int.
		return (int) wrapped.contentLong;
	}

	public static Object constructDefaultValue()
	{
		return new EnumeratedWrapper( ProxyRecordValue.ItemProxy.class, new ProxyRecordValue.ItemProxy( DataTypes.makeIntValue( 1 ) ) );
	}

	private static EnumeratedWrapper getOne( Scriptable scope, Type type, Object key )
	{
		Value rawValue = type.initialValue();
		if ( key instanceof String )
		{
			rawValue = type.parseValue( (String) key, false );
		}
		else if ( key instanceof Float || key instanceof Double )
		{
			rawValue = type.makeValue( (int) Math.round( (Double) key ), false );
		}
		else if ( key instanceof Number )
		{
			rawValue = type.makeValue( ((Number) key).intValue(), false );
		}

		if ( rawValue == null )
		{
			throw new ScriptException( "Bad " + type.getName() + " value: " + key.toString() );
		}

		Class<?> proxyRecordValueClass = null;
		for ( Class<?> testRecordValueClass : ProxyRecordValue.class.getDeclaredClasses() )
		{
			if ( testRecordValueClass.getSimpleName().toLowerCase().startsWith( type.getName() ) )
			{
				proxyRecordValueClass = testRecordValueClass;
			}
		}

		return EnumeratedWrapper.wrap( scope, proxyRecordValueClass, rawValue );
	}

	public static Object genericGet( Context cx, Scriptable thisObject, Object[] args, Function functionObject )
	{
		if ( args.length != 1 )
		{
			throw new ScriptException( "<Class>.get takes only one argument: a number/string or an array." );
		}

		String typeName = (String) ScriptableObject.getProperty( functionObject, "typeName" );
		Type type = DataTypes.simpleTypes.find( typeName );

		Scriptable scope = ScriptableObject.getTopLevelScope( thisObject );

		Object arg = args[0];
		if ( arg instanceof Iterable )
		{
			List<Object> result = new ArrayList<>();
			for ( Object key : (Iterable<?>) arg )
			{
				result.add( getOne( scope, type, key ) );
			}
			return cx.newArray( scope, result.toArray() );
		}
		else
		{
			return getOne( scope, type, arg );
		}
	}

	public static Object all( Context cx, Scriptable thisObject, Object[] args, Function functionObject )
	{
		if ( args.length != 0 )
		{
			throw new ScriptException( "<Class>.all does not take arguments." );
		}

		String typeName = (String) ScriptableObject.getProperty( functionObject, "typeName" );
		Type type = DataTypes.simpleTypes.find( typeName );

		Scriptable scope = ScriptableObject.getTopLevelScope( thisObject );
		ValueConverter coercer = new ValueConverter( cx, scope );

		return cx.newArray( scope,
				Arrays.asList( ( Value[] ) type.allValues().content )
						.stream()
						.map( coercer::asJava ).toArray() );
	}
}
