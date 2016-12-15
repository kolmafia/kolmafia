/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.List;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.VYKEACompanionData;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONException;

public class Value
	extends ParseTreeNode
	implements Comparable<Value>
{
	public Type type;

	public long contentLong = 0;
	public String contentString = null;
	public Object content = null;

	public Value()
	{
		this.type = DataTypes.VOID_TYPE;
	}

	public Value( final long value )
	{
		this.type = DataTypes.INT_TYPE;
		this.contentLong = value;
	}

	public Value( final boolean value )
	{
		this.type = DataTypes.BOOLEAN_TYPE;
		this.contentLong = value ? 1 : 0;
	}

	public Value( final String value )
	{
		this.type = DataTypes.STRING_TYPE;
		this.contentString = value == null ? "" : value;
	}

	public Value( final double value )
	{
		this.type = DataTypes.FLOAT_TYPE;
		this.contentLong = Double.doubleToRawLongBits( value );
	}

	public Value( final Type type )
	{
		this.type = type;
	}

	public Value( final Type type, final String contentString )
	{
		this.type = type;
		this.contentString = contentString;
	}

	public Value( final Type type, final long contentLong, final String contentString )
	{
		this.type = type;
		this.contentLong = contentLong;
		this.contentString = contentString;
	}

	public Value( final Type type, final String contentString, final Object content )
	{
		this.type = type;
		this.contentString = contentString;
		this.content = content;
	}

	public Value( final Type type, final long contentLong, final String contentString, final Object content )
	{
		this.type = type;
		this.contentLong = contentLong;
		this.contentString = contentString;
		this.content = content;
	}

	public Value( final Value original )
	{
		this.type = original.type;
		this.contentLong = original.contentLong;
		this.contentString = original.contentString;
		this.content = original.content;
	}

	public Value toFloatValue()
	{
		if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return this;
		}
		return DataTypes.makeFloatValue( (double) this.contentLong );
	}

	public Value toIntValue()
	{
		if ( this.type.equals( DataTypes.TYPE_INT ) )
		{
			return this;
		}
		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return DataTypes.makeIntValue( this.contentLong != 0 );
		}
		return DataTypes.makeIntValue( (long) this.floatValue() );
	}

	public Value toBooleanValue()
	{
		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return this;
		}
		return DataTypes.makeBooleanValue( this.contentLong != 0 );
	}

	public Type getType()
	{
		return this.type.getBaseType();
	}

	@Override
	public String toString()
	{
		if ( this.content instanceof StringBuffer )
		{
			return ( (StringBuffer) this.content ).toString();
		}

		if ( this.type.equals( DataTypes.TYPE_VOID ) )
		{
			return "void";
		}

		if ( this.contentString != null )
		{
			return this.contentString;
		}

		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return String.valueOf( this.contentLong != 0 );
		}

		if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return KoLConstants.NONSCIENTIFIC_FORMAT.format( this.floatValue() );
		}

		return String.valueOf( this.contentLong );
	}

	public String toQuotedString()
	{
		if ( this.contentString != null )
		{
			return "\"" + this.contentString + "\"";
		}
		return this.toString();
	}

	public Value toStringValue()
	{
		return new Value( this.toString() );
	}

	public Object rawValue()
	{
		return this.content;
	}

	public long intValue()
	{
		if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return (long) Double.longBitsToDouble( this.contentLong );
		}
		return this.contentLong;
	}

	public double floatValue()
	{
		if ( !this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return (double) this.contentLong;
		}
		return Double.longBitsToDouble( this.contentLong );
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		return this;
	}
	
	public Value asProxy()
	{
		if ( this.type == DataTypes.CLASS_TYPE )
		{
			return new ProxyRecordValue.ClassProxy( this );
		}
		if ( this.type == DataTypes.ITEM_TYPE )
		{
			return new ProxyRecordValue.ItemProxy( this );
		}
		if ( this.type == DataTypes.FAMILIAR_TYPE )
		{
			return new ProxyRecordValue.FamiliarProxy( this );
		}
		if ( this.type == DataTypes.SKILL_TYPE )
		{
			return new ProxyRecordValue.SkillProxy( this );
		}
		if ( this.type == DataTypes.EFFECT_TYPE )
		{
			return new ProxyRecordValue.EffectProxy( this );
		}
		if ( this.type == DataTypes.LOCATION_TYPE )
		{
			if ( this.content == null )
			{	// All attribute lookups on $location[none] would generate NPEs,
				// so instead of adding a null check to each attribute, just
				// return a normal record with default values.
				return new RecordValue( ProxyRecordValue.LocationProxy._type );
			}
			return new ProxyRecordValue.LocationProxy( this );
		}
		if ( this.type == DataTypes.MONSTER_TYPE )
		{
			if ( this.content == null )
			{
				// Ditto
				return new RecordValue( ProxyRecordValue.MonsterProxy._type );
			}
			return new ProxyRecordValue.MonsterProxy( this );
		}
		if ( this.type == DataTypes.COINMASTER_TYPE )
		{
			if ( this.content == null )
			{
				// Ditto
				return new RecordValue( ProxyRecordValue.CoinmasterProxy._type );
			}
			return new ProxyRecordValue.CoinmasterProxy( this );
		}
		if ( this.type == DataTypes.BOUNTY_TYPE )
		{
			return new ProxyRecordValue.BountyProxy( this );
		}
		if ( this.type == DataTypes.THRALL_TYPE )
		{
			return new ProxyRecordValue.ThrallProxy( this );
		}
		if ( this.type == DataTypes.SERVANT_TYPE )
		{
			return new ProxyRecordValue.ServantProxy( this );
		}
		if ( this.type == DataTypes.VYKEA_TYPE )
		{
			return new ProxyRecordValue.VykeaProxy( this );
		}
		if ( this.type == DataTypes.ELEMENT_TYPE )
		{
			return new ProxyRecordValue.ElementProxy( this );
		}
		if ( this.type == DataTypes.PHYLUM_TYPE )
		{
			return new ProxyRecordValue.PhylumProxy( this );
		}
		return this;
	}
	
	/* null-safe version of the above */
	public static Value asProxy( Value value )
	{
		if ( value == null )
		{
			return null;
		}
		return value.asProxy();
	}

	public int compareTo( final Value o )
	{
		if ( !( o instanceof Value ) )
		{
			throw new ClassCastException();
		}

		Value it = (Value) o;

		if ( this.type == DataTypes.BOOLEAN_TYPE ||
		     this.type == DataTypes.INT_TYPE ||
		     this.type == DataTypes.ITEM_TYPE ||
		     this.type == DataTypes.EFFECT_TYPE ||
		     this.type == DataTypes.CLASS_TYPE ||
		     this.type == DataTypes.SKILL_TYPE ||
		     this.type == DataTypes.FAMILIAR_TYPE ||
		     this.type == DataTypes.SLOT_TYPE ||
		     this.type == DataTypes.THRALL_TYPE ||
		     this.type == DataTypes.SERVANT_TYPE )
		{
			return this.contentLong < it.contentLong ? -1 : this.contentLong == it.contentLong ? 0 : 1;
		}

		if ( this.type == DataTypes.VYKEA_TYPE )
		{
			// Let the underlying data type itself decide
			VYKEACompanionData v1 = (VYKEACompanionData)( this.content );
			VYKEACompanionData v2 = (VYKEACompanionData)( it.content );
			return v1.compareTo( v2 );
		}

		if ( this.type == DataTypes.FLOAT_TYPE )
		{
			return Double.compare(
				Double.longBitsToDouble( this.contentLong ),
				Double.longBitsToDouble( it.contentLong ) );
		}

		if ( this.type == DataTypes.MONSTER_TYPE )
		{
			// If we know a monster ID, compare it
			if ( this.contentLong != 0 || it.contentLong != 0 )
			{
				return this.contentLong < it.contentLong ? -1 : this.contentLong == it.contentLong ? 0 : 1;
			}
			// Otherwise, must compare names
		}

		if ( this.contentString != null && it.contentString != null )
		{
			return this.contentString.compareTo( it.contentString );
		}

		return -1;
	}

	public int count()
	{
		return 1;
	}

	public void clear()
	{
	}

	public boolean contains( final Value index )
	{
		return false;
	}

	@Override
	public boolean equals( final Object o )
	{
		return o == null || !( o instanceof Value ) ? false : this.compareTo( (Value) o ) == 0;
	}

	@Override
	public int hashCode()
	{
		int hash;
		hash = this.type != null ? this.type.hashCode() : 0;
		hash = hash + 31 * (int) this.contentLong;
		hash = hash + 31 * ( this.contentString != null ? this.contentString.hashCode() : 0 );
		return hash;
	}

	public static String escapeString( String string )
	{
		// Since map_to_file has one record per line with fields
		// delimited by tabs, string values cannot have newline or tab
		// characters in them. Escape those characters. And, since we
		// escape using backslashes, backslash must also be escaped.
		// 
		// Replace backslashes with \\, newlines with \n, and tabs with \t

		int length = string.length();
		StringBuilder buffer = new StringBuilder( length );
		for ( int i = 0; i < length; i++ )
		{
			char c = string.charAt( i );
			switch ( c )
			{
			case '\n':
				buffer.append( "\\n" );
				break;
			case '\t':
				buffer.append( "\\t" );
				break;
			case '\\':
				buffer.append( "\\\\" );
				break;
			default:
				buffer.append( c );
				break;
			}
		}
		return buffer.toString();
	}

	public static String unEscapeString( String string )
	{
		int length = string.length();
		StringBuilder buffer = new StringBuilder( length );
		boolean saw_backslash = false;

		for ( int i = 0; i < length; i++ )
		{
			char c = string.charAt( i );
			if ( !saw_backslash )
			{
				if ( c == '\\' )
				{
					saw_backslash = true;
				}
				else
				{
					buffer.append( c );
				}
				continue;
			}

			switch ( c )
			{
			case 'n':
				buffer.append( '\n' );
				break;
			case 't':
				buffer.append( '\t' );
				break;
			default:
				buffer.append( c );
				break;
			}

			saw_backslash = false;
		}

		if ( saw_backslash )
		{
			buffer.append('\\');
		}

		return buffer.toString();
	}

	public static Value readValue( final Type type, final String string, final String filename, final int line )
	{
		int tnum = type.getType();
		if ( tnum == DataTypes.TYPE_STRING )
		{
			return new Value( Value.unEscapeString( string ) );
		}

		Value value = type.parseValue( string, true );

		// Validate data and report errors
		List<String> names = type.getAmbiguousNames( string, value, false );
		if ( names != null && names.size() > 1 )
		{
			StringBuilder message = new StringBuilder();
			message.append( "Multiple matches for " );
			message.append( string );
			message.append( "; using " );
			message.append( value.toString() );
			message.append( " in " );
			message.append( Parser.getLineAndFile( filename, line ) );
			message.append( ". Clarify by using one of:" );
			RequestLogger.printLine( message.toString() );
			for ( String str : names )
			{
				RequestLogger.printLine( str );
			}
		}

		return value;
	}

	public String dumpValue()
	{
		int type = this.type.getType();
		return  type == DataTypes.TYPE_STRING ?
			Value.escapeString( this.contentString ) :
			type == DataTypes.TYPE_ITEM || type == DataTypes.TYPE_EFFECT ?
			( this.contentString.startsWith( "[" ) ?
			  this.contentString :
			  "[" + String.valueOf( this.contentLong ) + "]" + this.contentString ) :
			this.toString();
	}

	public void dumpValue( final PrintStream writer )
	{
		writer.print( this.dumpValue() );
	}

	public void dump( final PrintStream writer, final String prefix, final boolean compact )
	{
		writer.println( prefix + this.dumpValue() );
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<VALUE " + this.getType() + " [" + this.toString() + "]>" );
	}

	public Object toJSON()
		throws JSONException
	{
		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return new Boolean( this.contentLong > 0 );
		}
		else if ( this.type.equals( DataTypes.TYPE_INT ) )
		{
			return new Long( this.contentLong );
		}
		else if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return new Double( Double.longBitsToDouble( this.contentLong ) );
		}
		else
		{
			return this.toString();
		}
	}
}
