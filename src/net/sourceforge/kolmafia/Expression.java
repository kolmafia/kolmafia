/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Expression
{
	private static final Pattern NUM_PATTERN = Pattern.compile( "([+-]?[\\d.]+)(.*)" );
	private static final int STACK_SIZE = 128;

	protected String name;
	protected String text;

	private char[] bytecode;	// Compiled expression
	private ArrayList<Object> literals;	// Strings & floats needed by expression
	protected AdventureResult effect;

	// If non-null, contains concatenated error strings from compiling bytecode
	private StringBuilder error = null;

	private StringBuilder newError()
	{
		if ( this.error == null )
		{
			this.error = new StringBuilder();
		}
		else
		{
			this.error.append( KoLConstants.LINE_BREAK );
		}
		return this.error;
	}

	public String getExpressionErrors()
	{
		if ( this.error == null )
		{
			return null;
		}
		StringBuilder buf = new StringBuilder();
		buf.append( "Expression syntax errors for '" );
		buf.append( name );
		buf.append( "':" );
		buf.append( KoLConstants.LINE_BREAK );
		buf.append( this.error );
		return buf.toString();
	}

	private static double[] cachedStack;

	private synchronized static double[] stackFactory( double[] recycle )
	{
		if ( recycle != null )
		{	// Reuse this stack for the next evaluation.
			cachedStack = recycle;
			return null;
		}
		else if ( cachedStack != null )
		{	// We have a stack handy; it's yours now.
			double[] rv = cachedStack;
			cachedStack = null;
			return rv;
		}
		else
		{	// We're all out of stacks.
			return new double[ STACK_SIZE ];
		}
	}

	public Expression( String text, String name )
	{
		this.name = name;
		this.text = text;

		// Let subclass initialize variables needed for
		// compilation and evaluation 
		this.initialize();

		// Compile the expression into byte code
		this.bytecode = this.validBytecodes().toCharArray();
		Arrays.sort( this.bytecode );
		String compiled = this.expr() + "r";
		//if ( name.length() > 0 && name.equalsIgnoreCase(
		//	Preferences.getString( "debugEval" ) ) )
		//{
		//	compiled = compiled.replaceAll( ".", "?$0" );
		//}
		this.bytecode = compiled.toCharArray();
		if ( this.text.length() > 0 )
		{
			StringBuilder buf = this.newError();
			buf.append( "Expected end, found " );
			buf.append( this.text );
		}
		this.text = null;
	}

	public static Expression getInstance( String text, String name )
	{
		Expression expr = new Expression( text, name );
		String errors = expr.getExpressionErrors();
		if ( errors != null )
		{
			KoLmafia.updateDisplay( errors );
		}
		return expr;
	}

	protected void initialize()
	{
	}

	public double eval()
	{
		try
		{
			return this.evalInternal();
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			KoLmafia.updateDisplay( "Unreasonably complex expression for " + this.name + ": " + e );
		}
		catch ( RuntimeException e )
		{
			KoLmafia.updateDisplay( "Expression evaluation error for " + this.name + ": " + e );
		}
		catch ( Exception e )
		{
			KoLmafia.updateDisplay( "Unexpected exception for " + this.name + ": " + e );
		}
		return 0.0;
	}

	public double evalInternal()
	{
		double[] s = stackFactory( null );
		int sp = 0;
		int pc = 0;
		double v = 0.0;

		while ( true )
		{
			char inst = this.bytecode[ pc++ ];
			switch ( inst )
			{
// 			case '?':	// temporary instrumentation
// 				KoLmafia.updateDisplay( "\u2326 Eval " + this.name + " from " +
// 					Thread.currentThread().getName() );
// 				StringBuffer b = new StringBuffer(); 
// 				if ( pc == 1 )
// 				{
// 					b.append( "\u2326 Bytecode=" );
// 					b.append( this.bytecode );
// 					for ( int i = 1; i < this.bytecode.length; i += 2 )
// 					{
// 						b.append( ' ' );
// 						b.append( Integer.toHexString( this.bytecode[ i ] ) );
// 					}
// 					KoLmafia.updateDisplay( b.toString() );
// 					b.setLength( 0 );
// 				}
// 				b.append( "\u2326 PC=" );
// 				b.append( pc );
// 				b.append( " Stack=" );
// 				if ( sp < 0 )
// 				{
// 					b.append( sp );
// 				}
// 				else
// 				{
// 					for ( int i = 0; i < sp && i < INITIAL_STACK; ++i )
// 					{
// 						b.append( ' ' );
// 						b.append( s[ i ] );
// 					}
// 				}
// 				KoLmafia.updateDisplay( b.toString() );
// 				continue;

			case 'r':
				v = s[ --sp ];
				stackFactory( s );	// recycle this stack
				return v;

			case '^':
				double base = s[ --sp ];
				double expt = s[ --sp ];
				v = (double) Math.pow( base, expt );
				if ( Double.isNaN( v ) || Double.isInfinite( v ) )
				{
					throw new ArithmeticException( "Invalid exponentiation: cannot take " + base + " ** " + expt );
				}
				break;

			case '*':
				v = s[ --sp ] * s[ --sp ];
				break;
			case '/':
				double numerator = s[ --sp ];
				double denominator = s[ --sp ];
				if ( denominator == 0.0 )
				{
					throw new ArithmeticException( "Can't divide by zero" );
				}
				v = numerator / denominator;
				break;
				
			case '+':
				v = s[ --sp ] + s[ --sp ];
				break;
			case '-':
				v = s[ --sp ] - s[ --sp ];
				break;

			case 'c':
				v = (double) Math.ceil( s[ --sp ] );
				break;
			case 'f':
				v = (double) Math.floor( s[ --sp ] );
				break;
			case 's':
				v = (double) Math.sqrt( s[ --sp ] );
				if ( Double.isNaN(v) )
				{
					throw new ArithmeticException( "Can't take square root of a negative value" );
				}
				break;
			case 'p':
				v = StringUtilities.parseDouble( Preferences.getString( (String) this.literals.get( (int) s[ --sp ] ) ) );
				break;
			case 'm':
				v = Math.min( s[ --sp ], s[ --sp ] );
				break;
			case 'x':
				v = Math.max( s[ --sp ], s[ --sp ] );
				break;

			case '#':
				v = ((Double) this.literals.get( (int) s[ --sp ] )).doubleValue();
				break;
				
			// Valid with ModifierExpression:
			case 'l':
				v = Modifiers.currentLocation.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0 : 1.0;
				break;
			case 'z':
				v = Modifiers.currentZone.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0 : 1.0;
				break;
			case 'w':
				v = Modifiers.currentFamiliar.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0 : 1.0;
				break;
			case 'h':
				v = Modifiers.mainhandClass.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0 : 1.0;
				break;
			case 'e':
				AdventureResult eff = new AdventureResult( (String) this.literals.get( (int) s[ --sp ] ), 1, true );
				v = eff == null ? 0.0 :
					Math.max( 0, eff.getCount( KoLConstants.activeEffects ) );
				break;
			case 'b':
				String elem = (String) this.literals.get( (int) s[ --sp ] );
				int element = elem.equals( "cold" ) ? Modifiers.COLD_RESISTANCE :
							  elem.equals( "hot" ) ? Modifiers.HOT_RESISTANCE :
							  elem.equals( "sleaze" ) ? Modifiers.SLEAZE_RESISTANCE :
							  elem.equals( "spooky" ) ? Modifiers.SPOOKY_RESISTANCE :
							  elem.equals( "stench" ) ? Modifiers.STENCH_RESISTANCE :
							  elem.equals( "slime" ) ? Modifiers.SLIME_RESISTANCE :
							  -1;
				v = KoLCharacter.currentNumericModifier( element );
				break;
			case 'A':
				v = KoLCharacter.getAscensions();
				break;
			case 'B':
				v = HolidayDatabase.getBloodEffect();
				break;
			case 'C':
				v = KoLCharacter.getMinstrelLevel();
				break;
			case 'D':
				v = KoLCharacter.getInebriety();
				break;
			case 'E':
			{
				int size = KoLConstants.activeEffects.size();
				AdventureResult[] effectsArray = new AdventureResult[ size ];
				KoLConstants.activeEffects.toArray( effectsArray );

				v = 0;
				for ( int i = 0; i < size; i++ )
				{
					AdventureResult effect = effectsArray[ i ];
					int duration = effect.getCount();
					if ( duration != Integer.MAX_VALUE )
					{
						v++;
					}
				}
				break;
			}
			case 'F':
				v = KoLCharacter.getFullness();
				break;
			case 'G':
				v = HolidayDatabase.getGrimaciteEffect() / 10.0;
				break;
			case 'H':
				v = Modifiers.hoboPower;
				break;
			case 'J':
				v = HolidayDatabase.getHoliday().equals( "Festival of Jarlsberg" ) ? 1.0 : 0.0;
				break;
			case 'L':
				v = KoLCharacter.getLevel();
				break;
			case 'M':
				v = HolidayDatabase.getMoonlight();
				break;
			case 'R':
				v = KoLCharacter.getReagentPotionDuration();
				break;
			case 'S':
				v = KoLCharacter.getSpleenUse();
				break;
			case 'T':
				v = this.effect == null ? 0.0 :
					Math.max( 1, this.effect.getCount( KoLConstants.activeEffects ) );
				break;
			case 'U':
				v = KoLCharacter.getTelescopeUpgrades();
				break;
			case 'W':
				v = Modifiers.currentWeight;
				break;
			case 'X':
				v = KoLCharacter.getGender();
				break;
			
			// Valid with MonsterExpression:
			case '\u0080':
				v = KoLCharacter.getAdjustedMuscle();
				break;
			case '\u0081':
				v = KoLCharacter.getAdjustedMysticality();
				break;
			case '\u0082':
				v = KoLCharacter.getAdjustedMoxie();
				break;
			case '\u0083':
				v = KoLCharacter.getMonsterLevelAdjustment();
				break;
			case '\u0084':
				v = KoLCharacter.getMindControlLevel();
				break;
			case '\u0085':
				v = KoLCharacter.getMaximumHP();
				break;
			case '\u0086':
				v = BasementRequest.getBasementLevel();
				break;
			case '\u0087':
				v = FightRequest.dreadKisses( "Woods" );
				break;
			case '\u0088':
				v = FightRequest.dreadKisses( "Village" );
				break;
			case '\u0089':
				v = FightRequest.dreadKisses( "Castle" );
				break;
			case '\u0090':
				v = KoLCharacter.getAdjustedHighestStat();
				break;
					
			default:
				if ( inst > '\u00FF' )
				{
					v = inst - 0x8000;
					break;
				}
				throw new RuntimeException( "Evaluator bytecode invalid at " +
							    (pc - 1) + ": " + String.valueOf( this.bytecode ) );
			}
			s[ sp++ ] = v;
		}
	}

	protected String validBytecodes()
	{	// Allowed operations in the A-Z range.
		return "";
	}

	private void expect( String token )
	{
		if ( this.text.startsWith( token ) )
		{
			this.text = this.text.substring( token.length() );
			return;
		}
		StringBuilder buf = this.newError();
		buf.append( "Expected " );
		buf.append( token );
		buf.append( ", found " );
		buf.append( this.text );
	}

	protected String until( String token )
	{
		int pos = this.text.indexOf( token );
		if ( pos == -1 )
		{
			StringBuilder buf = this.newError();
			buf.append( "Expected " );
			buf.append( token );
			buf.append( ", found " );
			buf.append( this.text );
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

	private char optional( String token1, String token2 )
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

	protected String literal( Object value, char op )
	{
		if ( this.literals == null )
		{
			this.literals = new ArrayList<Object>();
		}
		this.literals.add( value == null ? "" : value );
		return String.valueOf( (char)( this.literals.size() - 1 + 0x8000 ) ) + op;
	}

	private String expr()
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

	private String term()
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

	private String factor()
	{
		String rv = this.value();
		while ( this.optional( "^", "**" ) != '\0' )
		{
			rv = this.value() + rv + "^";
		}
		return rv;
	}

	private String value()
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
			return this.literal( this.until( ")" ), 'p' );
		}

		rv = this.function();
		if ( rv != null )
		{
			return rv;
		}

		if ( this.text.length() == 0 )
		{
			StringBuilder buf = this.newError();
			buf.append( "Unexpected end of expr" );
			return "\u8000";	
		}
		rv = this.text.substring( 0, 1 );
		if ( rv.charAt( 0 ) >= 'A' && rv.charAt( 0 ) <= 'Z' )
		{
			this.text = this.text.substring( 1 );
			if ( Arrays.binarySearch( this.bytecode, rv.charAt( 0 ) ) < 0 )
			{
				StringBuilder buf = this.newError();
				buf.append( "'" );
				buf.append( rv );
				buf.append( "' is not valid in this context" );
				return "\u8000";
			}
			return rv;
		}
		Matcher m = NUM_PATTERN.matcher( this.text );
		if ( m.matches() )
		{
			double v = Double.parseDouble( m.group( 1 ) );
			this.text = m.group( 2 );
			if ( v % 1.0 == 0.0 && v >= -0x7F00 && v < 0x8000 )
			{
				return String.valueOf( (char)((int)v + 0x8000) );
			}
			else
			{
				return this.literal( new Double( v ), '#' );
			}
		}
		if ( this.optional( "-" ) )
		{
			return this.value() + "\u8000-";
		}

		StringBuilder buf = this.newError();
		buf.append( "Can't understand " );
		buf.append( this.text );
		this.text = "";
		return "\u8000";
	}

	protected String function()
	{
		return null;
	}
}
