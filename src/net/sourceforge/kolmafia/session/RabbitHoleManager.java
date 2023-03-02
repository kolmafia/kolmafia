package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RabbitHoleRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class RabbitHoleManager {
  public static final Pattern HAT_CLEANER_PATTERN = Pattern.compile("\\s");

  public static class Hat {
    private final int length;
    private final String effect;
    private final String modifier;

    public Hat(final int length, final String effect, final String modifier) {
      this.length = length;
      this.effect = effect;
      this.modifier = modifier;
    }

    public int getLength() {
      return this.length;
    }

    public String getEffect() {
      return this.effect;
    }

    public String getModifier() {
      return this.modifier;
    }
  }

  public static final Hat[] HAT_DATA = {
    new Hat(4, "Assaulted with Pepper", "Monster Level +20"),
    new Hat(6, "Three Days Slow", "Familiar Experience +3"),
    new Hat(7, "Cat-Alyzed", "Moxie +10"),
    new Hat(8, "Anytwo Five Elevenis?", "Muscle +10"),
    new Hat(9, "Coated Arms", "Weapon Damage +15"),
    new Hat(10, "Smoky Third Eye", "Mysticality +10"),
    new Hat(11, "Full Bottle in front of Me", "Spell Damage +30%"),
    new Hat(12, "Thick-Skinned", "Maximum HP +50"),
    new Hat(13, "20-20 Second Sight", "Maximum MP +25"),
    new Hat(14, "Slimy Hands", "+10 Sleaze Damage"),
    new Hat(15, "Bottle in front of Me", "Spell Damage +15"),
    new Hat(16, "Fan-Cooled", "+10 Cold Damage"),
    new Hat(17, "Ginger Snapped", "+10 Spooky Damage"),
    new Hat(18, "Egg on your Face", "+10 Stench Damage"),
    new Hat(19, "Pockets of Fire", "+10 Hot Damage"),
    new Hat(20, "Weapon of Mass Destruction", "Weapon Damage +30%"),
    new Hat(21, "Orchid Blood", "Regenerate 5-10 MP per Adventure"),
    new Hat(22, "Dances with Tweedles", "+40% Meat from Monsters"),
    new Hat(23, "Patched In", "Mysticality +20%"),
    new Hat(24, "You Can Really Taste the Dormouse", "+5 to Familiar Weight"),
    new Hat(25, "Turtle Titters", "+3 Stat Gains from Fights"),
    new Hat(26, "Cat Class, Cat Style", "Moxie +20%"),
    new Hat(27, "Surreally Buff", "Muscle +20%"),
    new Hat(28, "Quadrilled", "+20% Items from Monsters"),
    new Hat(29, "Coming Up Roses", "Regenerate 10-20 MP per Adventure"),
    new Hat(30, "Oleaginous Soles", "+40% Combat Initiative"),
    new Hat(31, "Oleaginous Soles", "+40% Combat Initiative"),
  };

  private static final String[] IMAGES =
      new String[] {
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

  private static final String[] TITLES =
      new String[] {
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

  private static final String SYMBOLS =
      "\u2659\u2656\u2658\u2657\u2654\u2655" + "\u265F\u265C\u265E\u265D\u265A\u265B";

  static {
    String base = KoLmafia.imageServerPath() + "otherimages/chess/";
    for (String image : IMAGES) {
      FileUtilities.downloadImage(base + image);
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

  private static final Pattern SQUARE_PATTERN =
      Pattern.compile(
          "<td.*?background-color: #(.*?);.*?title=\"(.*?)\".*?otherimages/chess/(blanktrans|(chess_(.)(.)(.)))\\.gif.*?</td>",
          Pattern.DOTALL);

  private static class Square {
    public static final int UNKNOWN = 0;

    public static final int WHITE = 1;
    public static final int BLACK = 2;

    public static final int EMPTY = 0;
    public static final int PAWN = 1;
    public static final int ROOK = 2;
    public static final int KNIGHT = 3;
    public static final int BISHOP = 4;
    public static final int KING = 5;
    public static final int QUEEN = 6;

    private final int color;
    private final int piece;
    private final int side;

    private static final Square WHITE_SQUARE = new Square(WHITE);
    private static final Square BLACK_SQUARE = new Square(BLACK);

    public Square(final Matcher matcher) {
      String colorString = matcher.group(1);
      if (colorString.equals("fff")) {
        this.color = WHITE;
      } else {
        this.color = BLACK;
      }

      if (matcher.group(4) == null) {
        this.piece = EMPTY;
        this.side = UNKNOWN;
      } else {
        String pieceString = matcher.group(5);
        switch (pieceString) {
          case "p" -> this.piece = PAWN;
          case "r" -> this.piece = ROOK;
          case "n" -> this.piece = KNIGHT;
          case "b" -> this.piece = BISHOP;
          case "k" -> this.piece = KING;
          case "q" -> this.piece = QUEEN;
          default -> this.piece = UNKNOWN;
        }

        String sideString = matcher.group(6);
        switch (sideString) {
          case "w" -> this.side = WHITE;
          case "b" -> this.side = BLACK;
          default -> this.side = UNKNOWN;
        }
      }
    }

    public Square(final int color) {
      this(color, EMPTY, UNKNOWN);
    }

    public Square(final int color, final int piece, final int side) {
      this.color = color;
      this.piece = piece;
      this.side = side;
    }

    public Square(final int color, final String pc, final String sc) {
      this.color = color;
      this.piece = Square.codeToPiece(pc);
      this.side = Square.codeToSide(sc);
    }

    public static Square getSquare(final int color) {
      return color == WHITE ? WHITE_SQUARE : BLACK_SQUARE;
    }

    public String getTitle() {
      int index = this.piece;
      if (index > 0) {
        if (this.side == BLACK) {
          index += 6;
        }
      }
      return TITLES[index];
    }

    public String getSymbol() {
      int index = this.piece;
      if (index <= 0) {
        return "&nbsp;";
      }
      if (this.side == BLACK) {
        index += 6;
      }
      return SYMBOLS.substring(index - 1, index);
    }

    public String getColorString() {
      return this.color == WHITE ? "White Square" : "Black Square";
    }

    public String getImage() {
      int index = this.piece;
      if (index > 0) {
        if (this.color == BLACK) {
          index += 12;
        }
        if (this.side == BLACK) {
          index += 6;
        }
      }
      return IMAGES[index];
    }

    public int getColor() {
      return this.color;
    }

    public int getSide() {
      return this.side;
    }

    public int getPiece() {
      return this.piece;
    }

    public boolean isPiece() {
      return this.piece != Square.EMPTY;
    }

    public Square convert() {
      return new Square(this.color, this.piece, this.side == WHITE ? BLACK : WHITE);
    }

    private static final String whiteSquare =
        "style=\"width: 60px; height: 60px; text-align: center; background-color: #ffffff;\"";
    private static final String blackSquare =
        "style=\"width: 60px; height: 60px; text-align: center; background-color: #979797;\"";
    private static final String whiteCompact =
        "style=\"width: 30px; height: 30px; text-align: center; background-color: #ffffff; font-size: 30px\"";
    private static final String blackCompact =
        "style=\"width: 30px; height: 30px; text-align: center; background-color: #DDDDDD; font-size: 30px\"";

    public void appendHTML(final StringBuffer buffer) {
      if (Preferences.getBoolean("compactChessboard")) {
        buffer.append("<td ");
        buffer.append(this.color == Square.WHITE ? whiteCompact : blackCompact);
        buffer.append(">");
        buffer.append(this.getSymbol());
        buffer.append("</td>");
      } else {
        buffer.append("<td ");
        buffer.append(this.color == Square.WHITE ? whiteSquare : blackSquare);
        buffer.append(">");
        buffer.append("<img src=\"");
        buffer.append(KoLmafia.imageServerPath());
        buffer.append("otherimages/chess/");
        buffer.append(this.getImage());
        buffer.append("\" height=50 width=50/>");
        buffer.append("</td>");
      }
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      if (this.piece == EMPTY) {
        buffer.append("Empty");
      } else {
        buffer.append(this.getTitle());
        buffer.append(" on a");
      }
      buffer.append(" ");
      buffer.append(this.getColorString());
      return buffer.toString();
    }

    public String pieceCode() {
      return switch (this.piece) {
        case PAWN -> "P";
        case ROOK -> "R";
        case KNIGHT -> "N";
        case BISHOP -> "B";
        case KING -> "K";
        case QUEEN -> "Q";
        default -> "";
      };
    }

    public static int codeToPiece(final String code) {
      return switch (code.charAt(0)) {
        case 'P', 'p' -> PAWN;
        case 'R', 'r' -> ROOK;
        case 'N', 'n' -> KNIGHT;
        case 'B', 'b' -> BISHOP;
        case 'K', 'k' -> KING;
        case 'Q', 'q' -> QUEEN;
        default -> UNKNOWN;
      };
    }

    public String sideCode() {
      return switch (this.side) {
        case BLACK -> "B";
        case WHITE -> "W";
        default -> "";
      };
    }

    public static int codeToSide(final String code) {
      return switch (code.charAt(0)) {
        case 'B', 'b' -> BLACK;
        case 'W', 'w' -> WHITE;
        default -> UNKNOWN;
      };
    }

    public static String coords(final int square) {
      int row = square / 8;
      int col = square % 8;
      return Character.toString((char) ('a' + col)) + (row + 1);
    }

    public String notation(final int square) {
      return this.sideCode() + this.pieceCode() + Square.coords(square);
    }
  }

  private static class Board implements Cloneable {
    private final Square[] board;
    private int current;
    private int pieces;

    public Board() {
      this.board = new Square[64];
      this.current = -1;
      this.pieces = 0;

      // Load up with empty white and black squares
      for (int i = 0; i < 64; ++i) {
        board[i] = Square.getSquare(Board.color(i));
      }
    }

    public static int color(final int square) {
      int row = square / 8;
      int col = square % 8;
      return (row + col) % 2 == 0 ? Square.WHITE : Square.BLACK;
    }

    private static final Pattern CONFIG_PATTERN =
        Pattern.compile("([bwBW])([prnbkqPRNBKQ])([abcdefghABCDEFGH])([12345678])");

    public Board(final String config) {
      // Create an empty board
      this();

      // Split the config string into squares
      Matcher m = Board.CONFIG_PATTERN.matcher(config);
      while (m.find()) {
        // Find the square
        String cs = m.group(3);
        String rs = m.group(4);
        int square = Board.square(rs, cs);
        int color = Board.color(square);

        // Find the piece
        String sc = m.group(1);
        String pc = m.group(2);
        Square piece = new Square(color, pc, sc);

        // Place the piece on the square
        this.board[square] = piece;
        this.pieces++;
        if (piece.getSide() == Square.WHITE) {
          this.current = square;
        }
      }
    }

    private Board(Board board) {
      this.board = board.board.clone();
      this.current = board.current;
      this.pieces = board.pieces;
    }

    public String config() {
      StringBuilder buffer = new StringBuilder();
      boolean first = true;
      for (int i = 0; i < 64; ++i) {
        Square piece = this.board[i];
        if (piece.isPiece()) {
          if (first) {
            first = false;
          } else {
            buffer.append(",");
          }
          buffer.append(piece.notation(i));
        }
      }
      return buffer.toString();
    }

    @Override
    public Board clone() {
      return new Board(this);
    }

    public int getCurrent() {
      return this.current;
    }

    public int getPieces() {
      return this.pieces;
    }

    public static int square(final int row, final int col) {
      return (row * 8) + col;
    }

    public static int square(final String rs, final String cs) {
      int col = cs.charAt(0) - 'a';
      int row = rs.charAt(0) - '1';
      return Board.square(row, col);
    }

    public Square get(final int index) {
      Square piece = this.board[index];
      return piece != null ? piece : new Square(Square.BLACK);
    }

    public Square add(final int index, final Square square) {
      Square old = this.board[index];
      this.board[index] = square;
      if (square.isPiece()) {
        this.pieces++;
        if (square.getSide() == Square.WHITE) {
          this.current = index;
        }
      }
      return old;
    }

    public Square remove(final int index) {
      Square square = this.board[index];
      if (square.isPiece()) {
        Square empty = Square.getSquare(square.getColor());
        this.board[index] = empty;
        this.pieces--;
      }
      return square;
    }

    public void set(final int index, final Square square) {
      this.remove(index);
      this.add(index, square);
    }

    public Square move(final int index1, final int index2) {
      // Remove the piece from current location
      Square old = remove(index1);

      // Remove the piece from destination location
      Square captured = remove(index2);

      // If it was a capture, we take over piece type
      if (captured.isPiece()) {
        this.add(index2, captured.convert());
      }

      // Otherwise, we simply move into the square
      else {
        this.add(index2, old);
      }

      // Return the former contents of the square
      return captured;
    }

    public int getWinningMove() {
      if (this.current < 0) {
        return -1;
      }

      Square square = this.board[this.current];
      if (!square.isPiece()) {
        return -1;
      }

      int row = this.current / 8;
      int col = this.current % 8;

      switch (square.getPiece()) {
        case Square.PAWN:
        case Square.KING:
          if (row != 1) {
            return -1;
          }
          // Fall through
        case Square.ROOK:
        case Square.QUEEN:
          return col;

        case Square.KNIGHT:
          if (row == 1) {
            return col < 2 ? col + 2 : col - 2;
          }
          if (row == 2) {
            return col < 1 ? col + 1 : col - 1;
          }
          return -1;

        case Square.BISHOP:
          if (row + col <= 7) {
            return row + col;
          }
          if (col - row >= 0) {
            return col - row;
          }
          return -1;
      }

      return -1;
    }

    public List<Integer> getMoves() {
      List<Integer> list = new ArrayList<>();

      if (this.current < 0) {
        return list;
      }

      Square square = this.board[this.current];
      if (!square.isPiece()) {
        return list;
      }

      // Depending on type of piece, generate all moves
      // available on current board configuration

      int row = this.current / 8;
      int col = this.current % 8;

      switch (square.getPiece()) {
        case Square.PAWN -> {
          // Pawns capture diagonally forward one row
          this.addMove(list, row - 1, col - 1);
          this.addMove(list, row - 1, col + 1);
        }
        case Square.KING -> {
          // Kings move one in any direction
          this.addMove(list, row - 1, col - 1);
          this.addMove(list, row - 1, col);
          this.addMove(list, row - 1, col + 1);
          this.addMove(list, row, col - 1);
          this.addMove(list, row, col + 1);
          this.addMove(list, row + 1, col - 1);
          this.addMove(list, row + 1, col);
          this.addMove(list, row + 1, col + 1);
        }
        case Square.KNIGHT -> {
          // Knights wiggle
          this.addMove(list, row - 2, col - 1);
          this.addMove(list, row - 2, col + 1);
          this.addMove(list, row - 1, col + 2);
          this.addMove(list, row + 1, col + 2);
          this.addMove(list, row + 2, col + 1);
          this.addMove(list, row + 2, col - 1);
          this.addMove(list, row + 1, col - 2);
          this.addMove(list, row - 1, col - 2);
        }
        case Square.ROOK -> this.addRookMoves(list, row, col);
        case Square.BISHOP -> this.addBishopMoves(list, row, col);
        case Square.QUEEN -> {
          this.addRookMoves(list, row, col);
          this.addBishopMoves(list, row, col);
        }
      }

      return list;
    }

    private void addRookMoves(final List<Integer> list, final int row, final int col) {
      // Go West. Quit when you hit a piece
      for (int i = col - 1; i >= 0; --i) {
        if (this.addMove(list, row, i)) {
          break;
        }
      }
      // Go East. Quit when you hit a piece
      for (int i = col + 1; i <= 7; ++i) {
        if (this.addMove(list, row, i)) {
          break;
        }
      }
      // Go North. Quit when you hit a piece
      for (int i = row - 1; i >= 0; --i) {
        if (this.addMove(list, i, col)) {
          break;
        }
      }
      // Go South. Quit when you hit a piece
      for (int i = row + 1; i <= 7; ++i) {
        if (this.addMove(list, i, col)) {
          break;
        }
      }
    }

    private void addBishopMoves(final List<Integer> list, final int row, final int col) {
      // Go Northwest. Quit when you hit a piece
      for (int irow = row - 1, icol = col - 1; irow >= 0 && icol >= 0; --irow, --icol) {
        if (this.addMove(list, irow, icol)) {
          break;
        }
      }
      // Go Northeast. Quit when you hit a piece
      for (int irow = row - 1, icol = col + 1; irow >= 0 && icol <= 7; --irow, ++icol) {
        if (this.addMove(list, irow, icol)) {
          break;
        }
      }
      // Go Southwest. Quit when you hit a piece
      for (int irow = row + 1, icol = col - 1; irow <= 7 && icol >= 0; ++irow, --icol) {
        if (this.addMove(list, irow, icol)) {
          break;
        }
      }
      // Go Southeast. Quit when you hit a piece
      for (int irow = row + 1, icol = col + 1; irow <= 7 && icol <= 7; ++irow, ++icol) {
        if (this.addMove(list, irow, icol)) {
          break;
        }
      }
    }

    private boolean addMove(final List<Integer> list, final int row, final int col) {
      // If the proposed move is off the board, fail
      if (row < 0 || row > 7 || col < 0 || col > 7) {
        return false;
      }

      // If the proposed move is not a capture, fail
      int square = (row * 8) + col;
      if (!this.board[square].isPiece()) {
        return false;
      }

      // Otherwise, tally the move and succeed
      list.add(square);
      return true;
    }

    public void appendHTML(final StringBuffer buffer) {
      buffer.append("<table cols=9>");
      buffer.append("<tr>");
      buffer.append("<td></td><");
      for (int i = 0; i < 8; i++) {
        buffer.append("<td><b>");
        buffer.append((char) ('a' + i));
        buffer.append("</b></td>");
      }
      buffer.append("</tr>");
      for (int i = 0; i < 64; i++) {
        Square square = this.board[i];
        if (i % 8 == 0) {
          buffer.append("<tr>");
          buffer.append("<td><b>");
          buffer.append((i / 8 + 1));
          buffer.append("</b></td>");
        }
        square.appendHTML(buffer);
        if (i % 8 == 7) {
          buffer.append("</tr>");
        }
      }
      buffer.append("</table>");
    }
  }

  private static final String testData = null;
  private static Board board;
  private static int moves;

  public static final void parseChessPuzzle(final String responseText) {
    parseChessPuzzle(responseText, true);
    if (board != null) {
      String message = "Board: " + board.config();
      RequestLogger.updateSessionLog(message);
    }
  }

  private static void parseChessPuzzle(final String responseText, final boolean initialVisit) {
    if (responseText == null) {
      return;
    }

    board = new Board();

    if (initialVisit) {
      moves = 0;
    } else {
      ++moves;
    }

    Matcher matcher = SQUARE_PATTERN.matcher(responseText);
    int index = 0;
    while (matcher.find()) {
      if (index < 64) {
        board.add(index, new Square(matcher));
      }
      index++;
    }

    if (index != 0 && index != 64) {
      KoLmafia.updateDisplay("What kind of a chessboard is that? I found " + index + " squares!");
      board = null;
      return;
    }

    if (initialVisit) {
      Preferences.setString("lastChessboard", board.config());
    }
  }

  private static final Pattern MOVE_PATTERN = Pattern.compile("xy=((\\d+)(?:%2C|,)(\\d+))");

  public static final void parseChessMove(final String urlString, final String responseText) {
    // Parse the destination square out of the URL
    Matcher matcher = MOVE_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return;
    }

    int col = StringUtilities.parseInt(matcher.group(2));
    int row = StringUtilities.parseInt(matcher.group(3));
    int moveSquare = Board.square(row, col);

    // Save the original piece and the one it captures
    int square = board.getCurrent();
    Square piece = board.get(square);
    Square newPiece = board.get(moveSquare);

    // Parse the new board
    parseChessPuzzle(responseText, false);

    // Could not parse board - error already shown
    if (board == null) {
      return;
    }

    // Find the new piece
    int newSquare = board.getCurrent();

    // Assume we are capturing
    String action = "x";

    // If we completed the puzzle, there will no longer be a board
    if (board.getPieces() == 0) {
      // We simply moved
      newSquare = moveSquare;
      action = "-";
      // Verify that KoL recognizes that we cleared the board
      if (responseText.contains("queen cookie")) {
        Preferences.increment("chessboardsCleared", 1, 50, false);
      }
    }

    // Did we actually move where we expected?
    else if (newSquare != moveSquare) {
      // No. Bogus move
      return;
    }

    // Log the move in chess notation
    String message = moves + ": " + piece.notation(square) + action + newPiece.notation(newSquare);

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  // CLI command support
  public static final void reset() {
    board = null;
    moves = 0;
  }

  public static final void load() {
    if (board == null) {
      parseChessPuzzle(testData);
    }

    if (board == null) {
      String config = Preferences.getString("lastChessboard");
      load(config, false);
    }

    if (board == null) {
      RequestLogger.printLine("I haven't seen a chessboard recently.");
    }
  }

  public static final void load(final String config, final boolean save) {
    if (config.trim().equals("")) {
      return;
    }
    board = new Board(config);

    if (save && !KoLCharacter.getUserName().equals("")) {
      Preferences.setString("lastChessboard", board.config());
    }
  }

  public static final void board() {
    load();
    board(board);
  }

  private static void board(final Board board) {
    if (board == null) {
      return;
    }

    StringBuffer buffer = new StringBuffer();
    board.appendHTML(buffer);
    RequestLogger.printLine(buffer.toString());
    RequestLogger.printLine();
  }

  public static final void test() {
    load();
    test(board);
  }

  private static void test(final Board board) {
    if (board == null) {
      return;
    }

    Path solution = solve(board);

    if (solution == null) {
      RequestLogger.printLine("I couldn't solve the current board.");
      return;
    }

    List<Integer> path = solution.getPath();
    int square = board.getCurrent();
    Square piece = board.get(square);
    RequestLogger.printLine("The " + piece.getTitle() + " on square " + Square.coords(square));
    for (int i = 0; i < path.size() - 1; ++i) {
      int next = path.get(i);
      Square nextPiece = board.get(next);
      RequestLogger.printLine(
          "...takes the "
              + nextPiece.getTitle()
              + " on square "
              + Square.coords(next)
              + " ("
              + piece.notation(square)
              + "x"
              + nextPiece.notation(next)
              + ")");
      square = next;
      piece = nextPiece;
    }
    int next = path.get(path.size() - 1);
    RequestLogger.printLine(
        "...which moves to square "
            + Square.coords(next)
            + " to win. ("
            + piece.notation(square)
            + "-"
            + Square.coords(next)
            + ")");
  }

  public static final void step() {
    RelayRequest.redirectedCommandURL = "/choice.php?forceoption=0";

    if (board == null) {
      RequestLogger.printLine("I haven't seen a chessboard recently.");
      return;
    }

    Path solution = solve(board);

    if (solution == null) {
      RequestLogger.printLine("I couldn't solve the current board.");
      return;
    }

    List<Integer> path = solution.getPath();
    int length = path.size();

    if (length == 0) {
      RequestLogger.printLine("Invalid solution.");
      return;
    }

    int square = path.get(0);
    int row = square / 8;
    int col = square % 8;

    String URL =
        "/choice.php?whichchoice=443&option=1&xy="
            + col
            + ","
            + row
            + "&pwd="
            + GenericRequest.passwordHash;

    RelayRequest.redirectedCommandURL = URL;
  }

  public static final void solve() {
    RelayRequest.specialCommandResponse = ChoiceManager.lastResponseText;
    RelayRequest.specialCommandStatus = "Solving...";

    if (board == null) {
      RequestLogger.printLine("I haven't seen a chessboard recently.");
      return;
    }

    Path solution = solve(board);

    if (solution == null) {
      RequestLogger.printLine("I couldn't solve the current board.");
      return;
    }

    List<Integer> path = solution.getPath();
    int length = path.size();
    String response = "";

    for (int i = 0; i < length; ++i) {
      RelayRequest.specialCommandStatus = "Move " + i + " of " + length;
      int square = path.get(i);
      int row = square / 8;
      int col = square % 8;
      String url =
          "choice.php?whichchoice=443&option=1&xy="
              + col
              + ","
              + row
              + "&pwd="
              + GenericRequest.passwordHash;
      GenericRequest req = new GenericRequest(url, false);
      req.run();
      response = req.responseText;
    }

    RelayRequest.specialCommandResponse = decorateChessPuzzleResponse(response);
    RelayRequest.specialCommandIsAdventure = true;
  }

  private static Path solve(final Board board) {
    // Attempt to solve by moving the current piece
    return solve(board.clone(), new Path());
  }

  private static Path solve(final Board board, final Path path) {
    int current = board.getCurrent();

    // If there is no current square, no path
    if (current < 0) {
      return null;
    }

    // If there is only one piece left standing, we have a
    // potential solution
    if (board.getPieces() == 1) {
      // Find the move which takes the current piece to the
      // top row
      int end = board.getWinningMove();
      if (end < 0) {
        // Oops. You can't get there from here.
        return null;
      }
      // Add the final move to the path and return it
      path.add(end);
      return path;
    }

    // Save the current piece
    Square currentPiece = board.get(current);

    // Get possible moves for the current piece
    List<Integer> moves = board.getMoves();

    for (int square : moves) {
      // If there is no piece on the square, skip
      if (!board.get(square).isPiece()) {
        continue;
      }

      // Add the new square to the current path
      path.add(square);

      // Move the current piece to the new location
      Square captured = board.move(current, square);

      // Recurse
      Path newPath = solve(board, path);

      // If we found a solution, we are golden
      if (newPath != null) {
        return newPath;
      }

      // Otherwise, backtrack
      path.remove();
      board.set(square, captured);
      board.set(current, currentPiece);
    }

    // We were unable to find a path to the goal
    return null;
  }

  private static class Path implements Iterable<Integer> {
    private final List<Integer> list;

    public Path() {
      list = new ArrayList<>();
    }

    @Override
    public Iterator<Integer> iterator() {
      return this.list.iterator();
    }

    public Integer get(final int index) {
      return list.get(index);
    }

    public void add(final int square) {
      list.add(square);
    }

    public void remove() {
      list.remove(list.size() - 1);
    }

    public int size() {
      return list.size();
    }

    public List<Integer> getPath() {
      return list;
    }
  }

  public static final void decorateChessPuzzle(final StringBuffer buffer) {
    // Add a "Step" and a "Solve!" button to the Chess Board
    String search = "</form>";
    int index = buffer.lastIndexOf(search);
    if (index == -1) {
      return;
    }

    index += 7;

    StringBuffer span = new StringBuffer();
    span.append("<center><table cols=2><tr>");

    StringBuffer stepButton = new StringBuffer();
    String url = "/KoLmafia/redirectedCommand?cmd=chess+step&pwd=" + GenericRequest.passwordHash;
    stepButton.append("<td>");
    stepButton.append("<form name=stepform action='").append(url).append("' method=post>");
    stepButton
        .append("<input type=hidden name=pwd value='")
        .append(GenericRequest.passwordHash)
        .append("'>");
    stepButton.append("<input class=button type=submit value=\"Step\">").append("</form>");
    stepButton.append("</td>");
    span.append(stepButton);

    StringBuffer solveButton = new StringBuffer();
    url = "/KoLmafia/specialCommand?cmd=chess+solve&pwd=" + GenericRequest.passwordHash;
    solveButton.append("<td>");
    solveButton.append("<form name=solveform action='").append(url).append("' method=post>");
    solveButton
        .append("<input type=hidden name=pwd value='")
        .append(GenericRequest.passwordHash)
        .append("'>");
    solveButton.append("<input class=button type=submit value=\"Solve!\">").append("</form>");
    solveButton.append("</td>");
    span.append(solveButton);

    span.append("</tr></table></center>");

    // Insert it into the page
    buffer.insert(index, span);
  }

  public static final String decorateChessPuzzleResponse(final String response) {
    StringBuffer buffer = new StringBuffer(response);
    RequestEditorKit.getFeatureRichHTML("choice.php", buffer);
    decorateChessPuzzleResponse(buffer);
    return buffer.toString();
  }

  private static final AdventureResult REFLECTION_OF_MAP =
      ItemPool.get(ItemPool.REFLECTION_OF_MAP, 1);
  private static final String ADVENTURE_AGAIN =
      "<b>Adventure Again:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center>";

  public static final void decorateChessPuzzleResponse(final StringBuffer buffer) {
    // Give player a link to use another reflection of a map
    if (REFLECTION_OF_MAP.getCount(KoLConstants.inventory) <= 0) {
      return;
    }

    int index = buffer.indexOf(ADVENTURE_AGAIN);
    if (index == -1) {
      return;
    }

    index += ADVENTURE_AGAIN.length();
    String link =
        "<a href=\"inv_use.php?pwd="
            + GenericRequest.passwordHash
            + "&whichitem=4509\">Use another reflection of a map</a><p>";
    buffer.insert(index, link);
  }

  public static final Hat getHatData(int length) {
    for (Hat hat : HAT_DATA) {
      if (hat.getLength() == length) {
        return hat;
      }
    }
    return null;
  }

  public static final String getHatDescription(int length) {
    Hat hat = getHatData(length);
    if (hat != null) {
      return hat.getEffect() + " (" + hat.getModifier() + ")";
    }
    return "unknown (" + length + " characters)";
  }

  public static final void decorateRabbitHole(final StringBuffer buffer) {
    int index = buffer.lastIndexOf("</table>");
    if (index == -1) {
      return;
    }
    index += 8;

    List<AdventureResult> hats = EquipmentManager.getEquipmentLists().get(Slot.HAT);
    AdventureResult curHat = EquipmentManager.getEquipment(Slot.HAT);
    TreeMap<Integer, String> options = new TreeMap<>();
    for (AdventureResult hat : hats) {
      if (hat.equals(EquipmentRequest.UNEQUIP)) { // no buff without a hat!
        continue;
      }
      int len = HAT_CLEANER_PATTERN.matcher(hat.getName()).replaceAll("").length();
      StringBuilder buf = new StringBuilder("<option value=");
      buf.append(hat.getItemId());
      if (hat.equals(curHat)) {
        buf.append(" selected");
      }
      buf.append(">");
      buf.append(hat.getName());
      buf.append(": ");
      buf.append(getHatDescription(len));
      buf.append("</option>");
      options.put((len << 24) | hat.getItemId(), buf.toString());
    }

    String ending = buffer.substring(index);
    buffer.delete(index, Integer.MAX_VALUE);
    buffer.append(
        "Hat the player: <select onChange=\"singleUse('inv_equip.php', 'which=2&action=equip&pwd=");
    buffer.append(GenericRequest.passwordHash);
    buffer.append("&ajax=1&whichitem=' + this.options[this.selectedIndex].value);\">");
    for (String option : options.values()) {
      buffer.append(option);
    }
    buffer.append("</select>");
    buffer.append(ending);
  }

  private static TreeMap<Integer, StringBuffer> getHatMap() {
    // Make a map of all hats indexed by length
    List<AdventureResult> hats = EquipmentManager.getEquipmentLists().get(Slot.HAT);
    FamiliarData current = KoLCharacter.getFamiliar();

    if (current.getItem() != null && EquipmentDatabase.isHat(current.getItem())) {
      hats.add(current.getItem());
    }

    TreeMap<Integer, StringBuffer> lengths = new TreeMap<>();
    for (AdventureResult hat : hats) {
      if (hat.equals(EquipmentRequest.UNEQUIP)) { // no buff without a hat!
        continue;
      }

      if (!EquipmentManager.canEquip(hat)) {
        // skip it if we can't equip it
        continue;
      }

      String name = hat.getName();

      Integer len = hatLength(name);
      StringBuffer buffer = lengths.get(len);

      if (buffer == null) {
        buffer = new StringBuffer();
        lengths.put(len, buffer);
      } else {
        buffer.append("|");
      }
      buffer.append(name);
    }

    return lengths;
  }

  public static final void hatCommand() {
    TreeMap<Integer, StringBuffer> lengths = getHatMap();

    if (lengths.size() == 0) {
      RequestLogger.printLine("You don't have any hats");
      return;
    }

    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=3>");
    output.append("<tr>");
    output.append("<th>Hat</th>");
    output.append("<th>Benefit</th>");
    output.append("<th>Effect</th>");
    output.append("</tr>");

    // For each hat length, generate a table row
    for (Integer key : lengths.keySet()) {
      Hat hat = getHatData(key);
      if (hat == null) {
        continue;
      }

      StringBuffer buffer = lengths.get(key);
      String[] split = buffer.toString().split("\\|");
      output.append("<tr><td>");
      output.append(split[0]);
      output.append("</td><td rowspan=");
      output.append(split.length);
      output.append(">");
      output.append(hat.getModifier());
      output.append("</td><td rowspan=");
      output.append(split.length);
      output.append(">");
      output.append(hat.getEffect());
      output.append("</td></tr>");
      for (int i = 1; i < split.length; ++i) {
        output.append("<tr><td>");
        output.append(split[i]);
        output.append("</td></tr>");
      }
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }

  public static boolean teaPartyAvailable() {
    return !Preferences.getBoolean("_madTeaParty");
  }

  public static void getHatBuff(final AdventureResult hat) {
    AdventureResult oldHat = EquipmentManager.getEquipment(Slot.HAT);

    if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE))) {
      if (!InventoryManager.hasItem(ItemPool.DRINK_ME_POTION)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You need a DRINK ME! potion to get a hatter buff.");
        return;
      }
      InventoryManager.retrieveItem(ItemPool.get(ItemPool.DRINK_ME_POTION, 1));
      RequestThread.postRequest(
          UseItemRequest.getInstance(ItemPool.get(ItemPool.DRINK_ME_POTION, 1)));
    }

    RequestThread.postRequest(new EquipmentRequest(hat, Slot.HAT));
    if (EquipmentManager.getEquipment(Slot.HAT).getItemId() != hat.getItemId()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to equip " + hat.getName() + ".");
      return;
    }

    Hat data = getHatData(hatLength(hat.getName()));
    if (data == null) {
      return;
    }

    String effectName = data.getEffect();
    String effectModifiers = data.getModifier();

    RequestLogger.printLine(
        "Getting " + effectName + " (" + effectModifiers + ") from the Mad Tea Party...");

    RequestThread.postRequest(new RabbitHoleRequest("rabbithole_teaparty"));
    RequestThread.postRequest(new GenericRequest("choice.php?pwd&whichchoice=441&option=1", true));
    RequestThread.postRequest(new EquipmentRequest(oldHat, Slot.HAT));
  }

  public static void getHatBuff(final int desiredHatLength) {
    if (hatLengthAvailable(desiredHatLength)) {
      TreeMap<Integer, StringBuffer> lengths = getHatMap();

      String hat = lengths.get(desiredHatLength).toString().split("\\|")[0];
      getHatBuff(ItemFinder.getFirstMatchingItem(hat));
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No matching hat length found.");
    }
  }

  public static boolean hatLengthAvailable(int desiredHatLength) {
    TreeMap<Integer, StringBuffer> lengths = getHatMap();

    if (lengths.size() == 0) {
      return false;
    }

    if (lengths.containsKey(desiredHatLength)) {
      return true;
    }

    return false;
  }

  public static int hatLength(final String name) {
    return HAT_CLEANER_PATTERN.matcher(name).replaceAll("").length();
  }

  public static final boolean registerChessboardRequest(final String urlString) {
    // Don't log anything here. We will log it when we get the
    // response and see the board.
    return true;
  }
}
