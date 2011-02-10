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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ProxyRecordValue
	extends RecordValue
{
	public ProxyRecordValue( final RecordType type, final Value obj )
	{
		super( type );
		
		this.contentInt = obj.contentInt;
		this.contentFloat = obj.contentFloat;
		this.contentString = obj.contentString;
		this.content = obj.content;
	}

	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		return this.aref( index, interpreter );
	}

	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		
		Object rv;
		try
		{
			rv = this.getClass().getMethod(
				"get_" + type.getFieldNames()[ index ], null ).invoke( this, null );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e );
		}
		
		if ( rv == null )
		{
			return type.getFieldTypes()[ index ].initialValue();
		}
		if ( rv instanceof Value )
		{
			return (Value) rv;
		}
		if ( rv instanceof Integer )
		{
			return new Value( ((Integer) rv).intValue() );
		}
		if ( rv instanceof Float )
		{
			return new Value( ((Float) rv).floatValue() );
		}
		if ( rv instanceof String )
		{
			return new Value( rv.toString() );
		}
		if ( rv instanceof Boolean )
		{
			return DataTypes.makeBooleanValue( ((Boolean) rv).booleanValue() );
		}
		throw interpreter.runtimeException( "Unable to convert attribute value of type: " + rv.getClass() );
	}

	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public Value remove( final Value key, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public void clear()
	{
	}
	
	/* Helper for building parallel arrays of field names & types */
	private static class RecordBuilder
	{
		private ArrayList names;
		private ArrayList types;

		public RecordBuilder()
		{
			names = new ArrayList();
			types = new ArrayList();
		}
		
		public RecordBuilder add( String name, Type type )
		{
			this.names.add( name.toLowerCase() );
			this.types.add( type );
			return this;
		}
		
		public RecordType finish( String name )
		{
			int len = this.names.size();
			return new RecordType( name,
				(String[]) this.names.toArray( new String[len] ),
				(Type[]) this.types.toArray( new Type[len] ) );
		}
	}

	public static class ItemProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "fullness", DataTypes.INT_TYPE )
			.add( "inebriety", DataTypes.INT_TYPE )
			.add( "spleen", DataTypes.INT_TYPE )
			.add( "notes", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "levelreq", DataTypes.INT_TYPE )
			.finish( "item proxy" );
			
		public ItemProxy( Value obj )
		{
			super( _type, obj );
		}
		
		public int get_fullness()
		{
			return ItemDatabase.getFullness( this.contentString );
		}
		
		public int get_inebriety()
		{
			return ItemDatabase.getInebriety( this.contentString );
		}
		
		public int get_spleen()
		{
			return ItemDatabase.getSpleenHit( this.contentString );
		}
		
		public String get_notes()
		{
			return ItemDatabase.getNotes( this.contentString );
		}
		
		public String get_descid()
		{
			return ItemDatabase.getDescriptionId( this.contentString );
		}
		
		public Integer get_levelreq()
		{
			return ItemDatabase.getLevelReqByName( this.contentString );
		}
	}

	public static class FamiliarProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "hatchling", DataTypes.ITEM_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.finish( "item proxy" );
			
		public FamiliarProxy( Value obj )
		{
			super( _type, obj );
		}
		
		public boolean get_combat()
		{
			return FamiliarDatabase.isCombatType( this.contentInt );
		}

		public Value get_hatchling()
		{
			return DataTypes.makeItemValue(
				FamiliarDatabase.getFamiliarLarva( this.contentInt ) );
		}
		
		public String get_image()
		{
			return FamiliarDatabase.getFamiliarImageLocation( this.contentInt );
		}
	}
}
