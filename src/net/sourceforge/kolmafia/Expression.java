package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

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

	public boolean hasErrors()
	{
		return this.error != null;
	}

	public String getExpressionErrors()
	{
		if ( this.error == null )
		{
			return null;
		}
        String buf = "Expression syntax errors for '" +
                name +
                "':" +
                KoLConstants.LINE_BREAK +
                this.error;
        return buf;
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
				
			case '+':
				v = s[ --sp ] + s[ --sp ];
				break;

			case '-':
				v = s[ --sp ] - s[ --sp ];
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

			case '%':
				v = s[ --sp ] % s[ --sp ];
				break;

			case '^':
				double base = s[ --sp ];
				double expt = s[ --sp ];
				v = Math.pow( base, expt );
				if ( Double.isNaN( v ) || Double.isInfinite( v ) )
				{
					throw new ArithmeticException( "Invalid exponentiation: cannot take " + base + " ** " + expt );
				}
				break;

			case 'a':
				v = Math.abs( s[ --sp ] );
				break;
			case 'c':
				v = Math.ceil( s[ --sp ] );
				break;
			case 'f':
				v = Math.floor( s[ --sp ] );
				break;
			case 'm':
				v = Math.min( s[ --sp ], s[ --sp ] );
				break;
			case 'p':
				String first = (String) this.literals.get( (int) s[ --sp ] );
				String second = null;
				int commaIndex = first.indexOf( "," );
				if ( commaIndex > -1 )
				{
					second = first.substring( commaIndex + 1 );
					first = first.substring( 0, commaIndex );
				}
				String prefString = Preferences.getString( first );
				if ( second != null )
				{
					v = prefString.contains( second ) ? 1 : 0;
				}
				else
				{
					v =
						prefString.contains( "true" ) ? 1 :
						prefString.contains( "false" ) ? 0 :
						StringUtilities.parseDouble( prefString );
				}
				break;
			case 's':
				v = Math.sqrt( s[ --sp ] );
				if ( Double.isNaN(v) )
				{
					throw new ArithmeticException( "Can't take square root of a negative value" );
				}
				break;
			case 'x':
				v = Math.max( s[ --sp ], s[ --sp ] );
				break;

			case '#':
				v = ((Double) this.literals.get( (int) s[ --sp ] )).doubleValue();
				break;
				
			// Valid with ModifierExpression:
			case 'b':
				String elem = (String) this.literals.get( (int) s[ --sp ] );
				int element =
					elem.equalsIgnoreCase( "cold" ) ? Modifiers.COLD_RESISTANCE :
					elem.equalsIgnoreCase( "hot" ) ? Modifiers.HOT_RESISTANCE :
					elem.equalsIgnoreCase( "sleaze" ) ? Modifiers.SLEAZE_RESISTANCE :
					elem.equalsIgnoreCase( "spooky" ) ? Modifiers.SPOOKY_RESISTANCE :
					elem.equalsIgnoreCase( "stench" ) ? Modifiers.STENCH_RESISTANCE :
					elem.equalsIgnoreCase( "slime" ) ? Modifiers.SLIME_RESISTANCE :
					elem.equalsIgnoreCase( "supercold" ) ? Modifiers.SUPERCOLD_RESISTANCE :
					-1;
				v = KoLCharacter.currentNumericModifier( element );
				break;
			case 'd':
				String skillName = (String) this.literals.get( (int) s[ --sp ] );
				if ( StringUtilities.isNumeric( skillName ) )
				{
					int skillId = StringUtilities.parseInt( skillName );
					skillName = SkillDatabase.getSkillName( skillId );
				}
				v = KoLCharacter.hasSkill( skillName ) ? 1 : 0;
				break;
			case 'e':
				String effectName = (String) this.literals.get( (int) s[ --sp ] );
				// If effect name is a number, convert to name
				AdventureResult eff = null;
				if ( StringUtilities.isNumeric( effectName ) )
				{
					int effectId = StringUtilities.parseInt( effectName );
					eff = EffectPool.get( effectId );
				}
				else
				{
					int effectId = EffectDatabase.getEffectId( effectName );
					eff = EffectPool.get( effectId );
				}
				v = eff == null ? 0.0 :
					Math.max( 0, eff.getCount( KoLConstants.activeEffects ) );
				break;
			case 'g':
				String itemName = (String) this.literals.get( (int) s[ --sp ] );
				int itemId = ItemDatabase.getItemId( itemName );
				AdventureResult item = ItemPool.get( itemId );
				v = KoLCharacter.hasEquipped( item ) ? 1 : 0;
				break;
			case 'h':
				v = Modifiers.mainhandClass.equalsIgnoreCase( (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'i':
				v = FamiliarDatabase.hasAttribute( Modifiers.currentFamiliar, (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'j':
				v = Modifiers.currentEnvironment.equalsIgnoreCase( (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'l':
				v = Modifiers.currentLocation.equalsIgnoreCase( (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'n':
				v = KoLCharacter.getClassName().equalsIgnoreCase( (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'w':
				v = Modifiers.currentFamiliar.equalsIgnoreCase( (String) this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			case 'z':
				String expressionZone = (String) this.literals.get( (int) s[ --sp ] );
				String currentZone = Modifiers.currentZone;
				v = 0;
				while ( true )
				{
					if ( currentZone.equalsIgnoreCase( expressionZone ) )
					{
						v = 1;
						break;
					}
					String parentZone = AdventureDatabase.PARENT_ZONES.get( currentZone );
					if ( parentZone == null || currentZone.equals( parentZone ) )
					{
						break;
					}
					currentZone = parentZone;
				}
				break;
			case 'v':
				String event = (String) this.literals.get( (int) s[ --sp ] );

				if ( HolidayDatabase.getHoliday().contains( event ) )
				{
					v = 1;
					break;
				}

				Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
				if ( event.equals( "Crimbo2015" ) )
				{
					// Event ends just after rollover on 3rd January 2016
					GregorianCalendar eventEnd = new GregorianCalendar( 2016, Calendar.JANUARY, 3, 20, 30 );
					eventEnd.setTimeZone( TimeZone.getTimeZone( "GMT-0700" ) );
					v = date.before( eventEnd ) ? 1 : 0;
				}
				else if ( event.equals( "December" ) )
				{
					int month = date.get( Calendar.MONTH );
					v = ( month == Calendar.DECEMBER ) ? 1 : 0;
				}

				break;
			
			// Valid with MonsterExpression:
			case '\u0080':
				v = KoLCharacter.getAdjustedMuscle();
				break;
			// Valid with MonsterExpression:
			case '\u0081':
				v = KoLCharacter.getAdjustedMysticality();
				break;
			// Valid with MonsterExpression:
			case '\u0082':
				v = KoLCharacter.getAdjustedMoxie();
				break;
			// Valid with MonsterExpression:
			case '\u0083':
				v = KoLCharacter.getMonsterLevelAdjustment();
				break;
			// Valid with MonsterExpression:
			case '\u0084':
				v = KoLCharacter.getMindControlLevel();
				break;
			// Valid with MonsterExpression and RestoreExpression:
			case '\u0085':
				v = KoLCharacter.getMaximumHP();
				break;
			// Valid with MonsterExpression:
			case '\u0086':
				v = BasementRequest.getBasementLevel();
				break;
			// Valid with MonsterExpression:
			case '\u0087':
				v = FightRequest.dreadKisses( "Woods" );
				break;
			// Valid with MonsterExpression:
			case '\u0088':
				v = FightRequest.dreadKisses( "Village" );
				break;
			// Valid with MonsterExpression:
			case '\u0089':
				v = FightRequest.dreadKisses( "Castle" );
				break;
			// Valid with MonsterExpression:
			case '\u0090':
				v = KoLCharacter.getAdjustedHighestStat();
				break;
			// Valid with RestoreExpression:
			case '\u0091':
				v = KoLCharacter.getMaximumMP();
				break;
			// Valid with ModifierExpression and MonsterExpression:
			case '\u0092':
				v = KoLCharacter.getPath().equals( this.literals.get( (int) s[ --sp ] ) ) ? 1 : 0;
				break;
			// Valid with ModifierExpression:
			case '\u0093':
				Modifiers mods = KoLCharacter.getCurrentModifiers();
				String modName = (String) this.literals.get( (int) s[ --sp ] );
				v = mods.getExtra( modName );
				break;
			// Valid with ModifierExpression:
			case '\u0094':
				v = KoLCharacter.canInteract() ? 1 : 0;
				break;
			// Valid with RestoreExpression:
			case '\u0095':
				v = KoLCharacter.getCurrentHP();
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
			case 'I':
				v = KoLCharacter.getDiscoMomentum();
				break;
			case 'J':
				v = HolidayDatabase.getHoliday().contains( "Festival of Jarlsberg" ) ? 1.0 : 0.0;
				break;
			case 'K':
				v = Modifiers.smithsness;
				break;
			case 'L':
				v = KoLCharacter.getLevel();
				break;
			case 'M':
				v = HolidayDatabase.getMoonlight();
				break;
			case 'N':
				v = KoLCharacter.getAudience();
				break;
			case 'P':
				v = KoLCharacter.currentPastaThrall.getLevel();
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
			case 'Y':
				v = KoLCharacter.getFury();
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
		while( true )
		{
			switch ( this.optional( "^", "%" ) )
			{
			case '^':
				rv = this.value() + rv + "^";
				break;
			case '%':
				rv = this.value() + rv + "%";
				break;
			default:
				return rv;
			}
		}
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
		if ( this.optional( "abs(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "a";
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
				return this.literal( v, '#' );
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
