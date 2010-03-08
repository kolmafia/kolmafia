/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class RabbitHoleManager
{
	public final static Object [][] HAT_DATA =
	{
		{
			new Integer( 4 ),
			"Assaulted with Pepper",
			"Monster Level +20",
		},
		{
			new Integer( 6 ),
			"Three Days Slow",
			"Familiar Experience +3",
		},
		{
			new Integer( 7 ),
			"Cat-Alyzed",
			"Moxie +10",
		},
		{
			new Integer( 8 ),
			"Anytwo Five Elevenis?",
			"Muscle +10",
		},
		{
			new Integer( 9 ),
			"Coated Arms",
			"Weapon Damage +15",
		},
		{
			new Integer( 10 ),
			"Smoky Third Eye",
			"Mysticality +10",
		},
		{
			new Integer( 11 ),
			"Full Bottle in front of Me",
			"Spell Damage +30%",
		},
		{
			new Integer( 12 ),
			"Thick-Skinned",
			"Maximum HP +50",
		},
		{
			new Integer( 13 ),
			"20-20 Second Sight",
			"Maximum MP +25",
		},
		{
			new Integer( 14 ),
			"Slimy Hands",
			"+10 Sleaze Damage",
		},
		{
			new Integer( 15 ),
			"Bottle in front of Me",
			"Spell Damage +15",
		},
		{
			new Integer( 16 ),
			"Fan-Cooled",
			"+10 Cold Damage",
		},
		{
			new Integer( 17 ),
			"Ginger Snapped",
			"+10 Spooky Damage",
		},
		{
			new Integer( 18 ),
			"Egg on your Face",
			"+10 Stench Damage",
		},
		{
			new Integer( 19 ),
			"Pockets of Fire",
			"+10 Hot Damage",
		},
		{
			new Integer( 20 ),
			"Weapon of Mass Destruction",
			"Weapon Damage +30%",
		},
		{
			new Integer( 22 ),
			"Dances with Tweedles",
			"+40% Meat from Monsters",
		},
		{
			new Integer( 23 ),
			"Patched In",
			"Mysticality +20%",
		},
		{
			new Integer( 24 ),
			"You Can Really Taste the Dormouse",
			"+5 to Familiar Weight",
		},
		{
			new Integer( 25 ),
			"Turtle Titters",
			"+3 Stat Gains from Fights",
		},
		{
			new Integer( 26 ),
			"Cat Class, Cat Style",
			"Moxie +20%",
		},
		{
			new Integer( 27 ),
			"Surreally Buff",
			"Muscle +20%",
		},
		{
			new Integer( 28 ),
			"Quadrilled",
			"+20% Items from Monsters",
		},
	};

	private static final String[] IMAGES = new String[]
	{
		"blanktrans.gif",
		"chess_pww.gif",
		"chess_rww.gif",
		"chess_nww.gif",
		"chess_bww.gif",
		"chess_kww.gif",
		"chess_qww.gif",
		"chess_pbw.gif",
		"chess_rbw.gif",
		"chess_nbw.gif",
		"chess_bbw.gif",
		"chess_kbw.gif",
		"chess_qbw.gif",
		"chess_pwb.gif",
		"chess_rwb.gif",
		"chess_nwb.gif",
		"chess_bwb.gif",
		"chess_kwb.gif",
		"chess_qwb.gif",
		"chess_pbb.gif",
		"chess_rbb.gif",
		"chess_nbb.gif",
		"chess_bbb.gif",
		"chess_kbb.gif",
		"chess_qbb.gif",
	};

	static
	{
		for ( int i = 0; i < RabbitHoleManager.IMAGES.length; ++i )
		{
			FileUtilities.downloadImage( "http://images.kingdomofloathing.com/otherimages/chess/" + RabbitHoleManager.IMAGES[i] );
		}
	}

	// The chessboard is a table of cells, each labeled with the piece that
	// is on it. Your piece is labeled "White Knight" (for example) and a
	// target pieces is "Black Pawn". When you move to a piece, it changes
	// to "White Pawn" and your valid moves become that of the new piece.
	//
	// Valid targets cells are wrapped in HTML <a></a> tags with the URL to
	// choice.php to move there.
	//
	// Rows and columns are numbered from 0 to 7. A choice to capture a
	// piece adds a field: "xy=6,4" goes to the 7th column in the 5th row.
	//
	// Background colors:
	//
	// background-color: #fff;	white
	// background-color: #979797;	black
	//
	// Images:
	//
	// chess/blanktrans.gif		blank square
	//
	// chess/chess_<piece><side><square>.gif
	//
	// <piece> = p			pawn
	// <piece> = r			rook
	// <piece> = n			knight
	// <piece> = b			bishop
	// <piece> = k			king
	// <piece> = q			queen
	//
	// <side> = b			black
	// <side> = w			white
	//
	// <square> = b			black
	// <square> = w			white

	private static final Pattern SQUARE_PATTERN = Pattern.compile("<td.*?background-color: #(.*?);.*?title=\"(.*?)\".*?otherimages/chess/(blanktrans|(chess_(.)(.)(.)))\\.gif.*?</td>", Pattern.DOTALL );

	private static class Square
	{
		public final static int UNKNOWN = 0;

		public final static int BLACK = 1;
		public final static int WHITE = 2;

		public final static int EMPTY = 0;
		public final static int PAWN = 1;
		public final static int ROOK = 2;
		public final static int KNIGHT = 3;
		public final static int BISHOP = 4;
		public final static int KING = 5;
		public final static int QUEEN = 6;

		private final int color;
		private final String title;
		private final String image;
		private final int piece;
		private final int side;

		public Square( final Matcher matcher )
		{
			String colorString = matcher.group( 1 );
			if ( colorString.equals( "fff" ) )
			{
				this.color = WHITE;
			}
			else if ( colorString.equals( "979797" ) )
			{
				this.color = BLACK;
			}
			else
			{
				this.color = UNKNOWN;
			}

			this.title = matcher.group( 2 );

			String imageString = matcher.group( 3 );
			this.image = "otherimages/chess/" + imageString + ".gif";

			if ( matcher.group( 4 ) == null )
			{
				this.piece = EMPTY;
				this.side = UNKNOWN;
			}
			else
			{
				String pieceString = matcher.group( 5 );
				if ( pieceString.equals( "p" ) )
				{
					this.piece = PAWN;
				}
				else if ( pieceString.equals( "r" ) )
				{
					this.piece = ROOK;
				}
				else if ( pieceString.equals( "n" ) )
				{
					this.piece = KNIGHT;
				}
				else if ( pieceString.equals( "b" ) )
				{
					this.piece = BISHOP;
				}
				else if ( pieceString.equals( "k" ) )
				{
					this.piece = KING;
				}
				else if ( pieceString.equals( "q" ) )
				{
					this.piece = QUEEN;
				}
				else
				{
					this.piece = UNKNOWN;
				}

				String sideString = matcher.group( 6 );
				if ( sideString.equals( "w" ) )
				{
					this.side = WHITE;
				}
				else if ( sideString.equals( "b" ) )
				{
					this.side = BLACK;
				}
				else
				{
					this.side = UNKNOWN;
				}
			}
		}

		public String getImage()
		{
			return this.image;
		}

		public int getColor()
		{
			return this.color;
		}

		public int getSide()
		{
			return this.side;
		}

		public int getPiece()
		{
			return this.piece;
		}

		private static String whiteSquare = "style=\"width: 60px; height: 60px; text-align: center; background-color: #fff;\"";
		private static String blackSquare = "style=\"width: 60px; height: 60px; text-align: center; background-color: #979797;\"";

		public void appendHTML( final StringBuffer buffer )
		{
			buffer.append( "<td " );
			buffer.append( this.color == Square.WHITE ? whiteSquare : blackSquare );
			buffer.append( ">" );
			buffer.append( "<img src=\"http://images.kingdomofloathing.com/" );
			buffer.append( this.image );
			buffer.append( "\" height=50 width=50/>" );
			buffer.append( "</td>" );
		}

		public String toString()
		{
			StringBuffer buffer = new StringBuffer();
			if ( this.piece == EMPTY )
			{
				buffer.append( "Empty" );
			}
			else
			{
				switch ( this.side )
				{
				case WHITE:
					buffer.append( "White" );
					break;
				case BLACK:
					buffer.append( "Black" );
					break;
				}
				buffer.append( " " );
				switch ( this.piece )
				{
				case PAWN:
					buffer.append( "Pawn" );
					break;
				case ROOK:
					buffer.append( "Rook" );
					break;
				case KNIGHT:
					buffer.append( "Knight" );
					break;
				case BISHOP:
					buffer.append( "Bishop" );
					break;
				case KING:
					buffer.append( "King" );
					break;
				case QUEEN:
					buffer.append( "Queen" );
					break;
				}
				buffer.append( " on a" );
			}
			buffer.append( " " );
			switch ( this.color )
			{
			case WHITE:
				buffer.append( "White Square" );
				break;
			case BLACK:
				buffer.append( "Black Square" );
				break;
			}
			return buffer.toString();
		}
	}

	private static Square[] board;

	public static final void parseChessPuzzle( final String responseText )
	{
		if ( board == null )
		{
			board = new Square[ 64 ];
		}

		Matcher matcher = RabbitHoleManager.SQUARE_PATTERN.matcher( responseText );
		int index = 0;
		while ( matcher.find() )
		{
			if ( index < 64 )
			{
				board[ index ] = new Square( matcher );
			}
			index++;
		}

		if ( index != 64 )
		{
			KoLmafia.updateDisplay( "What kind of a chessboard is that? I found " + index + " squares!" );
			RabbitHoleManager.board = null;
			return;
		}
	}

	public static final void test()
	{
		if ( RabbitHoleManager.board == null )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( "<table cols=8>" );
		for ( int i = 0; i < 64; i++ )
		{
			Square square = RabbitHoleManager.board[ i ];
			if ( i % 8 == 0 )
			{
				buffer.append( "<tr>" );
			}
			square.appendHTML( buffer );
			if ( i % 8 == 7 )
			{
				buffer.append( "</tr>" );
			}
		}
		buffer.append( "</table>" );

		RequestLogger.printLine( buffer.toString() );
		RequestLogger.printLine();
	}

	private static final Pattern MOVE_PATTERN = Pattern.compile("move=((\\d+)(:?%2C|,)(\\d+))");


	public static final void ensureUpdatedRabbitHoleStatus()
	{
		if ( Preferences.getInteger( "lastRabbitHoleReset" ) == KoLCharacter.getAscensions() )
		{
			return;
		}
		Preferences.setInteger( "lastRabbitHoleReset", KoLCharacter.getAscensions() );
	}

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( !location.startsWith( "rabbithole.php" ) )
		{
			return;
		}

		// Add a "Solve!" button to the Chess Board
	}
}
