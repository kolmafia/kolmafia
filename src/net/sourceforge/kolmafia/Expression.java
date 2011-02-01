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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Expression
{
	private static final Pattern NUM_PATTERN = Pattern.compile( "([+-]?[\\d.]+)(.*)" );
	private static final int INITIAL_STACK = 8;
	private static final int MAXIMUM_STACK = 128;

	protected String name;
	protected String text;

	protected float[] stack;	// Also holds numeric literals
	protected int sp = 0;		// Stack pointer during evaluation
	protected char[] bytecode;	// Compiled expression
	protected ArrayList pref = new ArrayList();

	public Expression( String text, String name )
	{
		this.name = name;
		this.text = text;

		// Let subclass initialize variables needed for
		// compilation and evaluation 
		this.initialize();

		// Start with a small stack. This will be expanded, if
		// necessary, the first time the expression is evaluated.
		this.stack = new float[ INITIAL_STACK ];

		// Compile the expression into byte code
		String compiled = this.expr() + "r";
		this.bytecode = compiled.toCharArray();
		if ( this.text.length() > 0 )
		{
			KoLmafia.updateDisplay( "Expression syntax error for '" + name + "': expected end, found " + this.text );
		}
		this.text = null;
	}

	protected void initialize()
	{
	}

	public float eval()
	{
		while ( this.stack.length <= MAXIMUM_STACK )
		{	// Find required stack size to evaluate this expression
			try
			{
				return this.evalInternal();
			}
			catch ( ArrayIndexOutOfBoundsException e )
			{
				System.out.println( e.toString() );
				float[] larger = new float[ this.stack.length * 2 ];
				System.arraycopy( this.stack, 0, larger, 0, this.stack.length );
				this.stack = larger;
			}
		}

		KoLmafia.updateDisplay( "Unreasonably complex expression for " + this.name );
		return 0.0f;
	}

	private float evalInternal()
	{
		float[] s = this.stack;
		int pc = 0;
		float v = 0.0f;

		while ( true )
		{
			char inst = this.bytecode[ pc++ ];
			switch ( inst )
			{
			case 'r':
				return s[ --this.sp ];

			case '^':
				v = (float) Math.pow( s[ --this.sp ], s[ --this.sp ] );
				break;

			case '*':
				v = s[ --this.sp ] * s[ --this.sp ];
				break;
			case '/':
				v = s[ --this.sp ] / s[ --this.sp ];
				break;
				
			case '+':
				v = s[ --this.sp ] + s[ --this.sp ];
				break;
			case '-':
				v = s[ --this.sp ] - s[ --this.sp ];
				break;

			case 'c':
				v = (float) Math.ceil( s[ --this.sp ] );
				break;
			case 'f':
				v = (float) Math.floor( s[ --this.sp ] );
				break;
			case 's':
				v = (float) Math.sqrt( s[ --this.sp ] );
				break;
			case 'p':
				v = StringUtilities.parseFloat( Preferences.getString( (String) this.pref.get( (int) s[ --this.sp ] ) ) );
				break;
			case 'm':
				v = Math.min( s[ --this.sp ], s[ --this.sp ] );
				break;
			case 'x':
				v = Math.max( s[ --this.sp ], s[ --this.sp ] );
				break;

			case '0':
				v = s[ 0 ];
				break;
			case '1':
				v = s[ 1 ];
				break;
			case '2':
				v = s[ 2 ];
				break;
			case '3':
				v = s[ 3 ];
				break;
			case '4':
				v = s[ 4 ];
				break;
			case '5':
				v = s[ 5 ];
				break;
			case '6':
				v = s[ 6 ];
				break;
			case '7':
				v = s[ 7 ];
				break;
			case '8':
				v = s[ 8 ];
				break;
			case '9':
				v = s[ 9 ];
				break;
					
			default:
				if ( inst > '\u00FF' )
				{
					v = inst - 0x8000;
					break;
				}
				if ( !this.validBytecode( inst ) )
				{
					KoLmafia.updateDisplay( "Evaluator bytecode invalid at " +
								(pc - 1) + ": " + String.valueOf( this.bytecode ) );
					return 0.0f;
				}
				v = this.evalBytecode( inst );
				break;
			}
			s[ this.sp++ ] = v;
		}
	}

	protected boolean validBytecode( char inst )
	{
		return false;
	}

	protected float evalBytecode( char inst )
	{
		return 0.0f;
	}

	protected void expect( String token )
	{
		if ( this.text.startsWith( token ) )
		{
			this.text = this.text.substring( token.length() );
			return;
		}
		KoLmafia.updateDisplay( "Evaluator syntax error: expected " + token +
					", found " + this.text );
	}

	protected String until( String token )
	{
		int pos = this.text.indexOf( token );
		if ( pos == -1 )
		{
			KoLmafia.updateDisplay( "Evaluator syntax error: expected " + token +
						", found " + this.text );
			return "";
		}
		String rv = this.text.substring( 0, pos );
		this.text = this.text.substring( pos + token.length() );
		return rv;
	}

	protected boolean optional( String token )
	{
		if ( this.text.startsWith( token ) )
		{
			this.text = this.text.substring( token.length() );
			return true;
		}
		return false;
	}

	protected char optional( String token1, String token2 )
	{
		if ( this.text.startsWith( token1 ) )
		{
			this.text = this.text.substring( token1.length() );
			return token1.charAt( 0 );
		}
		if ( this.text.startsWith( token2 ) )
		{
			this.text = this.text.substring( token2.length() );
			return token2.charAt( 0 );
		}
		return '\0';
	}

	protected String expr()
	{
		String rv = this.term();
		while ( true )
		{
			switch ( this.optional( "+", "-" ) )
			{
			case '+':
				rv = this.term() + rv + "+";
				break;
			case '-':
				rv = this.term() + rv + "-";
				break;
			default:
				return rv;			
			}
		}
	}

	protected String term()
	{
		String rv = this.factor();
		while ( true )
		{
			switch ( this.optional( "*", "/" ) )
			{
			case '*':
				rv = this.factor() + rv + "*";
				break;
			case '/':
				rv = this.factor() + rv + "/";
				break;
			default:
				return rv;			
			}
		}
	}

	protected String factor()
	{
		String rv = this.value();
		while ( this.optional( "^" ) )
		{
			rv = this.value() + rv + "^";
		}
		return rv;
	}

	protected String value()
	{
		String rv;
		if ( this.optional( "(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "ceil(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "c";
		}
		if ( this.optional( "floor(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "f";
		}
		if ( this.optional( "sqrt(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "s";
		}
		if ( this.optional( "min(" ) )
		{
			rv = this.expr();
			this.expect( "," );
			rv = rv + this.expr() + "m";
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "max(" ) )
		{
			rv = this.expr();
			this.expect( "," );
			rv = rv + this.expr() + "x";
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "pref(" ) )
		{
			this.pref.add( this.until( ")" ) );
			return String.valueOf( (char)( ( this.pref.size()-1 ) + 0x8000) ) + "p";
		}

		rv = this.function();
		if ( rv != null )
		{
			return rv;
		}

		if ( this.text.length() == 0 )
		{
			KoLmafia.updateDisplay( "Evaluator syntax error: unexpected end of expr" );
			return "\u8000";	
		}
		rv = this.text.substring( 0, 1 );
		if ( rv.charAt( 0 ) >= 'A' && rv.charAt( 0 ) <= 'Z' )
		{
			this.text = this.text.substring( 1 );
			return rv;
		}
		Matcher m = NUM_PATTERN.matcher( this.text );
		if ( m.matches() )
		{
			float v = Float.parseFloat( m.group( 1 ) );
			this.text = m.group( 2 );
			if ( v % 1.0f == 0.0f && v >= -0x7F00 && v < 0x8000 )
			{
				return String.valueOf( (char)((int)v + 0x8000) );
			}
			else
			{
				if ( this.sp >= 10 )
				{
					KoLmafia.updateDisplay( "Evaluator syntax error: too many numeric literals" );
					return "\u8000";	
				}
				this.stack[ this.sp++ ] = v;
				return String.valueOf( (char)( '0' + this.sp - 1 ) );
			}
		}
		if ( this.optional( "-" ) )
		{
			return this.value() + "\u8000-";
		}
		KoLmafia.updateDisplay( "Evaluator syntax error: can't understand " + this.text );
		this.text = "";
		return "\u8000";	
	}

	protected String function()
	{
		return null;
	}
}
