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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
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

	private static final String[] TITLES = new String[]
	{
		"blank square",
		"White Pawn",
		"White Rook",
		"White Knight",
		"White Bishop",
		"White King",
		"White Queen",
		"Black Pawn",
		"Black Rook",
		"Black Knight",
		"Black Bishop",
		"Black King",
		"Black Queen",
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

		public final static int WHITE = 1;
		public final static int BLACK = 2;

		public final static int EMPTY = 0;
		public final static int PAWN = 1;
		public final static int ROOK = 2;
		public final static int KNIGHT = 3;
		public final static int BISHOP = 4;
		public final static int KING = 5;
		public final static int QUEEN = 6;

		private final int color;
		private final int piece;
		private final int side;

		private static final Square WHITE_SQUARE = new Square( WHITE );
		private static final Square BLACK_SQUARE = new Square( BLACK );

		public Square( final Matcher matcher )
		{
			String colorString = matcher.group( 1 );
			if ( colorString.equals( "fff" ) )
			{
				this.color = WHITE;
			}
			else
			{
				this.color = BLACK;
			}

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

		public Square( final int color )
		{
			this( color, EMPTY, UNKNOWN );
		}

		public Square( final int color, final int piece, final int side )
		{
			this.color = color;
			this.piece = piece;
			this.side = side;
		}

		public static Square getColoredSquare( final int color )
		{
			return color == WHITE ? WHITE_SQUARE : BLACK_SQUARE;
		}

		public String getTitle()
		{
			int index = this.piece;
			if ( index > 0 )
			{
				if (this.side == BLACK )
				{
					index += 6;
				}
			}
			return TITLES[ index ];
		}

		public String getColorString()
		{
			return this.color == WHITE ? "White Square" : "Black Square";
		}

		public String getImage()
		{
			int index = this.piece;
			if ( index > 0 )
			{
				if (this.color == BLACK )
				{
					index += 12;
				}
				if (this.side == BLACK )
				{
					index += 6;
				}
			}
			return IMAGES[ index ];
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

		public boolean isPiece()
		{
			return this.piece != Square.EMPTY;
		}

		public Square convert()
		{
			return new Square( this.color, this.piece, this.side == WHITE ? BLACK : WHITE );
		}

		private static String whiteSquare = "style=\"width: 60px; height: 60px; text-align: center; background-color: #fff;\"";
		private static String blackSquare = "style=\"width: 60px; height: 60px; text-align: center; background-color: #979797;\"";

		public void appendHTML( final StringBuffer buffer )
		{
			buffer.append( "<td " );
			buffer.append( this.color == Square.WHITE ? whiteSquare : blackSquare );
			buffer.append( ">" );
			buffer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/chess/" );
			buffer.append( this.getImage() );
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
				buffer.append( this.getTitle() );
				buffer.append( " on a" );
			}
			buffer.append( " " );
			buffer.append( this.getColorString() );
			return buffer.toString();
		}

		public String code()
		{
			switch ( this.piece )
			{
			case PAWN:
				return "P";
			case ROOK:
				return "R";
			case KNIGHT:
				return "N";
			case BISHOP:
				return "B";
			case KING:
				return "K";
			case QUEEN:
				return "Q";
			}
			return "";
		} 

		public static String coords( final int square )
		{
			int row = square / 8;
			int col = square % 8;
			return Character.toString( (char)('a' + col) ) + String.valueOf( row + 1 );
		} 

		public String notation( final int square )
		{
			return this.code() + Square.coords( square );
		} 
	}

	private static class Board
		implements Cloneable
	{
		private Square[] board;
		private int current;
		private int pieces;

		public Board()
		{
			this.board = new Square[ 64 ];
			this.current = -1;
			this.pieces = 0;
		}

		private Board( Square [] board, int current, int pieces )
		{
			this.board = (Square [])board.clone();
			this.current = current;
			this.pieces = pieces;
		}

		public Object clone()
		{
			return new Board( this.board, this.current, this.pieces );
		}

		public int getCurrent()
		{
			return this.current;
		}

		public int getPieces()
		{
			return this.pieces;
		}

		public static int square( final int row, final int col )
		{
			return ( row * 8 ) + col;
		}

		public Square get( final int index )
		{
			Square piece = this.board[ index ];
			return piece != null ? piece : new Square( Square.BLACK );
		}

		public Square add( final int index, final Square square )
		{
			Square old = this.board[ index ];
			this.board[ index ] = square;
			if ( square.isPiece() )
			{
				this.pieces++;
				if ( square.getSide() == Square.WHITE )
				{
					this.current = index;
				}
			}
			return old;
		}

		public Square remove( final int index )
		{
			Square square = this.board[ index ];
			if ( square.isPiece() )
			{
				Square empty = Square.getColoredSquare( square.getColor() );
				this.board[ index ] = empty;
				this.pieces--;
			}
			return square;
		}

		public void set( final int index, final Square square )
		{
			this.remove( index );
			this.add( index, square );
		}

		public Square move( final int index1, final int index2 )
		{
			// Remove the piece from current location
			Square old = remove( index1 );

			// Remove the piece from destination location
			Square captured = remove( index2 );

			// If it was a capture, we take over piece type
			if ( captured.isPiece() )
			{
				this.add( index2, captured.convert() );
			}

			// Otherwise, we simply move into the square
			else
			{
				this.add( index2, old );
			}

			// Return the former contents of the square
			return captured;
		}

		public int getWinningMove()
		{
			if ( this.current < 0 )
			{
				return -1;
			}

			Square square = this.board[ this.current ];
			if ( !square.isPiece() )
			{
				return -1;
			}

			int row = this.current / 8;
			int col = this.current % 8;

			switch ( square.getPiece() )
			{
			case Square.PAWN:
			case Square.KING:
				if ( row != 1 )
				{
					return -1;
				}
				// Fall through
			case Square.ROOK:
			case Square.QUEEN:
				return col;

			case Square.KNIGHT:
				if ( row == 1 )
				{
					return col < 2 ? col + 2 : col - 2;
				}
				if ( row == 2 )
				{
					return col < 1 ? col + 1 : col - 1;
				}
				return -1;
				
			case Square.BISHOP:
				if ( row + col <= 7 )
				{
					return row + col;
				}
				if ( col - row >= 0 )
				{
					return col - row;
				}
				return -1;
			}

			return -1;
		}

		public Integer [] getMoves()
		{
			if ( this.current < 0 )
			{
				return new Integer[0];
			}

			Square square = this.board[ this.current ];
			if ( !square.isPiece() )
			{
				return new Integer[0];
			}

			ArrayList list = new ArrayList();

			// Depending on type of piece, generate all moves
			// available on current board configuration

			int row = this.current / 8;
			int col = this.current % 8;

			switch ( square.getPiece() )
			{
			case Square.PAWN:
				// Pawns capture diagonally forward one row
				this.addMove( list, row - 1, col - 1 );
				this.addMove( list, row - 1, col + 1 );
				break;
			case Square.KING:
				// Kings move one in any direction
				this.addMove( list, row - 1, col - 1 );
				this.addMove( list, row - 1, col );
				this.addMove( list, row - 1, col + 1 );
				this.addMove( list, row, col - 1 );
				this.addMove( list, row, col + 1 );
				this.addMove( list, row + 1, col - 1 );
				this.addMove( list, row + 1, col );
				this.addMove( list, row + 1, col + 1 );
				break;
			case Square.KNIGHT:
				// Knights wiggle
				this.addMove( list, row - 2, col - 1 );
				this.addMove( list, row - 2, col + 1 );
				this.addMove( list, row - 1, col + 2 );
				this.addMove( list, row + 1, col + 2 );
				this.addMove( list, row + 2, col + 1 );
				this.addMove( list, row + 2, col - 1 );
				this.addMove( list, row + 1, col - 2 );
				this.addMove( list, row - 1, col - 2 );
				break;
			case Square.ROOK:
				this.addRookMoves( list, row, col );
				break;
			case Square.BISHOP:
				this.addBishopMoves( list, row, col );
				break;
			case Square.QUEEN:
				this.addRookMoves( list, row, col );
				this.addBishopMoves( list, row, col );
				break;
			}

			// Convert the list into an array
			Integer [] array = new Integer[ list.size() ];
			return (Integer []) list.toArray( array );
		}

		private void addRookMoves( final ArrayList list, final int row, final int col )
		{
			// Go West. Quit when you hit a piece
			for ( int i = col - 1; i >= 0; --i )
			{
				if ( this.addMove( list, row, i ) )
				{
					break;
				}
			}
			// Go East. Quit when you hit a piece
			for ( int i = col + 1; i <= 7; ++i )
			{
				if ( this.addMove( list, row, i ) )
				{
					break;
				}
			}
			// Go North. Quit when you hit a piece
			for ( int i = row - 1; i >= 0; --i )
			{
				if ( this.addMove( list, i, col ) )
				{
					break;
				}
			}
			// Go South. Quit when you hit a piece
			for ( int i = row + 1; i <= 7; ++i )
			{
				if ( this.addMove( list, i, col ) )
				{
					break;
				}
			}
		}

		private void addBishopMoves( final ArrayList list, final int row, final int col )
		{
			// Go Northwest. Quit when you hit a piece
			for ( int irow = row - 1, icol = col - 1; irow >= 0 && icol >= 0; --irow, --icol )
			{
				if ( this.addMove( list, irow, icol ) )
				{
					break;
				}
			}
			// Go Northeast. Quit when you hit a piece
			for ( int irow = row - 1, icol = col + 1; irow >= 0 && icol <=7; --irow, ++icol )
			{
				if ( this.addMove( list, irow, icol ) )
				{
					break;
				}
			}
			// Go Southwest. Quit when you hit a piece
			for ( int irow = row + 1, icol = col - 1; irow <= 7 && icol >= 0; ++irow, --icol )
			{
				if ( this.addMove( list, irow, icol ) )
				{
					break;
				}
			}
			// Go Southeast. Quit when you hit a piece
			for ( int irow = row + 1, icol = col + 1; irow <= 7 && icol <= 7; ++irow, ++icol )
			{
				if ( this.addMove( list, irow, icol ) )
				{
					break;
				}
			}
		}

		private boolean addMove( final ArrayList list, final int row, final int col )
		{
			// If the proposed move is off the board, fail
			if ( row < 0 || row > 7 || col < 0 || col > 7 )
			{
				return false;
			}

			// If the proposed move is not a capture, fail
			int square = ( row * 8 ) + col;
			if ( !this.board[ square ].isPiece() )
			{
				return false;
			}

			// Otherwise, tally the move and succeed
			list.add( new Integer( square ) );
			return true;
		}

		public void appendHTML( final StringBuffer buffer )
		{
			buffer.append( "<table cols=9>" );
			buffer.append( "<tr>" );
			buffer.append( "<td></td><" );
			for ( int i = 0; i < 8; i++ )
			{
				buffer.append( "<td><b>" );
				buffer.append( (char)( 'a' + i ) );
				buffer.append( "</b></td>" );
			}
			buffer.append( "</tr>" );
			for ( int i = 0; i < 64; i++ )
			{
				Square square = this.board[ i ];
				if ( i % 8 == 0 )
				{
					buffer.append( "<tr>" );
					buffer.append( "<td><b>" );
					buffer.append( String.valueOf( i / 8 + 1) );
					buffer.append( "</b></td>" );
				}
				square.appendHTML( buffer );
				if ( i % 8 == 7 )
				{
					buffer.append( "</tr>" );
				}
			}
			buffer.append( "</table>" );
		}
	}

	private static String testData = null;
	private static Board original;
	private static Board board;
	private static int moves;

	public static final void parseChessPuzzle( final String responseText )
	{
		RabbitHoleManager.parseChessPuzzle( responseText, true );
	}

	private static final void parseChessPuzzle( final String responseText, final boolean initialVisit )
	{
		if ( responseText == null )
		{
			return;
		}

		RabbitHoleManager.board = new Board();

		if ( initialVisit )
		{
			RabbitHoleManager.original = RabbitHoleManager.board;
			RabbitHoleManager.moves = 0;
		}
		else
		{
			++RabbitHoleManager.moves;
		}

		Matcher matcher = RabbitHoleManager.SQUARE_PATTERN.matcher( responseText );
		int index = 0;
		while ( matcher.find() )
		{
			if ( index < 64 )
			{
				board.add( index, new Square( matcher ) );
			}
			index++;
		}

		if ( index != 0 && index != 64 )
		{
			KoLmafia.updateDisplay( "What kind of a chessboard is that? I found " + index + " squares!" );
			RabbitHoleManager.board = null;
			return;
		}
	}

	private static final Pattern MOVE_PATTERN = Pattern.compile("xy=((\\d+)(?:%2C|,)(\\d+))");

	public static final void parseChessMove( final String urlString, final String responseText )
	{
		// Parse the destination square out of the URL
		Matcher matcher = MOVE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return;
		}

		int col = StringUtilities.parseInt( matcher.group( 2 ) );
		int row = StringUtilities.parseInt( matcher.group( 3 ) );
		int moveSquare = Board.square( row, col );

		// Save the original piece
		int square = RabbitHoleManager.board.getCurrent();
		Square piece = RabbitHoleManager.board.get( square );

		// Parse the new board
		RabbitHoleManager.parseChessPuzzle( responseText, false );

		// Find the new piece
		int newSquare = RabbitHoleManager.board.getCurrent();

		// Assume we are capturing
		String action = "x";

		// If we completed the puzzle, there will no longer be a board
		if ( RabbitHoleManager.board.getPieces() == 0 )
		{
			// We simply moved
			newSquare = moveSquare;
			action = "-";
		}

		// Did we actually move where we expected?
		else if ( newSquare != moveSquare )
		{
			// No. Bogus move
			return;
		}

		Square newPiece = RabbitHoleManager.board.get( newSquare );

		// Log the move in chess notation
		String message = RabbitHoleManager.moves + ": " + piece.notation( square ) + action + newPiece.notation( newSquare );

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	// CLI command support
	public static final void board( final boolean current )
	{
		if ( RabbitHoleManager.board == null )
		{
			RabbitHoleManager.parseChessPuzzle( RabbitHoleManager.testData );
		}

		if ( RabbitHoleManager.board == null )
		{
			RequestLogger.printLine( "I haven't seen a chessboard recently." );
			return;
		}

		RabbitHoleManager.board( current ? RabbitHoleManager.board : RabbitHoleManager.original );
	}

	private static final void board( final Board board )
	{
		StringBuffer buffer = new StringBuffer();
		board.appendHTML( buffer );
		RequestLogger.printLine( buffer.toString() );
		RequestLogger.printLine();
	}

	public static final void test( final boolean current )
	{
		if ( RabbitHoleManager.board == null )
		{
			RabbitHoleManager.parseChessPuzzle( RabbitHoleManager.testData );
		}

		if ( RabbitHoleManager.board == null )
		{
			RequestLogger.printLine( "I haven't seen a chessboard recently." );
			return;
		}

		RabbitHoleManager.test( current ? RabbitHoleManager.board : RabbitHoleManager.original );
	}

	private static final void test( final Board board )
	{
		Path solution = RabbitHoleManager.solve( RabbitHoleManager.board );

		if ( solution == null )
		{
			RequestLogger.printLine( "I couldn't solve the current board." );
			return;
		}

		Integer [] path = solution.toArray();
		int square = board.getCurrent();
		Square piece = board.get( square );
		RequestLogger.printLine( "The " +
				       piece.getTitle() +
				       " on square " +
				       Square.coords( square ) );
		for ( int i = 0; i < path.length - 1; ++i )
		{
			int next  = path[ i ].intValue();
			Square nextPiece  = board.get( next );
			RequestLogger.printLine( "...takes the " +
						 nextPiece.getTitle() +
						 " on square " +
						 Square.coords( next ) +
						 " (" +
						 piece.notation( square ) +
						 "x" +
						 nextPiece.notation( next ) +
						 ")" );
			square = next;
			piece = nextPiece;
		}
		int next = path[ path.length - 1 ].intValue();
		RequestLogger.printLine( "...which moves to square " +
					 Square.coords( next ) +
					 " to win. (" +
					 piece.notation( square ) +
					 "-" +
					 Square.coords( next ) +
					 ")" );
	}

	public static final void solve()
	{
		RelayRequest.specialCommandResponse = ChoiceManager.lastResponseText;

		if ( RabbitHoleManager.board == null )
		{
			RequestLogger.printLine( "I haven't seen a chessboard recently." );
			return;
		}

		Path solution = RabbitHoleManager.solve( RabbitHoleManager.board );

		if ( solution == null )
		{
			RequestLogger.printLine( "I couldn't solve the current board." );
			return;
		}

		Integer [] path = solution.toArray();
		for ( int i = 0; i < path.length; ++i )
		{
			int square = path[ i ].intValue();
			int row = square / 8;
			int col = square % 8;
			String url = "choice.php?pwd&whichchoice=443&option=1&xy=" + String.valueOf( col ) + "," + String.valueOf( row );
			GenericRequest req = new GenericRequest( url );
			req.run();
			RelayRequest.specialCommandResponse = req.responseText;
		}
	}

	private static final Path solve( final Board board )
	{
		// Attempt to solve by moving the current piece
		return RabbitHoleManager.solve( (Board)board.clone(), new Path() );
	}

	private static final Path solve( final Board board, final Path path )
	{
		int current = board.getCurrent();

		// If there is no current square, no path
		if ( current < 0 )
		{
			return null;
		}

		// If there is only one piece left standing, we have a
		// potential solution
		if ( board.getPieces() == 1 )
		{
			// Find the move which takes the current piece to the
			// top row
			int end = board.getWinningMove();
			if ( end < 0 )
			{
				// Oops. You can't get there from here.
				return null;
			}
			// Add the final move to the path and return it
			path.add( end );
			return path;
		}

		// Save the current piece
		Square currentPiece = board.get( current );

		// Get possible moves for the current piece
		Integer [] moves = board.getMoves();

		for ( int i = 0; i < moves.length; ++i )
		{
			int square = moves[ i ].intValue();

			// If there is no piece on the square, skip
			if ( !board.get( square ).isPiece() )
			{
				continue;
			}

			// Add the new square to the current path
			path.add( square );

			// Move the current piece to the new location
			Square captured = board.move( current, square );

			// Recurse
			Path newPath = solve( board, path );

			// If we found a solution, we are golden
			if ( newPath != null )
			{
				return newPath;
			}

			// Otherwise, backtrack
			path.remove();
			board.set( square, captured );
			board.set( current, currentPiece );
		}

		// We were unable to find a path to the goal
		return null;
	}

	private static class Path
	{
		private final ArrayList list;

		public Path()
		{
			list = new ArrayList();
		}

		public void add( final int square )
		{
			list.add( new Integer( square ) );
		}

		public void remove()
		{
			list.remove( list.size() - 1 );
		}

		public int size()
		{
			return list.size();
		}

		public Integer [] toArray()
		{
			Integer [] array = (Integer []) list.toArray( new Integer[ list.size() ] );
			return array;
		}
	}

	public static final void decorateChessPuzzle( final StringBuffer buffer )
	{
		// Add a "Solve!" button to the Chess Board
		String search = "</form>";
		int index = buffer.lastIndexOf( search );
		if ( index == -1 )
		{
			return;
		}
		index += 7;

		StringBuffer button = new StringBuffer();

		String url = "/KoLmafia/specialCommand?cmd=chess+solve&pwd=" + GenericRequest.passwordHash;
		button.append( "<form name=solveform action='" + url + "' method=post>" );
		button.append( "<input class=button type=submit value=\"Solve!\">" );
		button.append( "</form>" );

		// Insert it into the page
		buffer.insert( index, button );
	}
	
	public static final void decorateRabbitHole( final StringBuffer buffer )
	{
		int index = buffer.lastIndexOf( "<Center>" );
		if ( true )	//index == -1 )
		{
			return;
		}
		index += 8;
		String ending = buffer.substring( index );
		buffer.delete( index, Integer.MAX_VALUE );
		buffer.append( "Hat the player: <select onChange='alert(this.options[this.selectedIndex].value)'>" );
		buffer.append( "<option value=123>this hat</option>" );
		buffer.append( "<option value=456 selected>that hat</option>" );
		buffer.append( "</select><br/>" );
		buffer.append( ending );
	}
}
