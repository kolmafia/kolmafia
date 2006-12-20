/**
 *	jline - Java console input library
 *	Copyright (c) 2002-2006, Marc Prud'hommeaux <mwp1@cornell.edu>
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *	Redistributions of source code must retain the above copyright
 *	notice, this list of conditions and the following disclaimer.
 *
 *	Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer
 *	in the documentation and/or other materials provided with
 *	the distribution.
 *
 *	Neither the name of JLine nor the names of its contributors
 *	may be used to endorse or promote products derived from this
 *	software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 *	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 *	AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 *	EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *	OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *	LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *	OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jline;

/**
 *	Synbolic constants for Console operations and virtual key bindings.
 *
 *	@see KeyEvent
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public interface ConsoleOperations
{
	final String CR = System.getProperty ("line.separator");

	final char BACKSPACE = '\b';
	final char RESET_LINE = '\r';
	final char KEYBOARD_BELL = '\07';

	final char CTRL_A = 1;
	final char CTRL_B = 2;
	final char CTRL_C = 3;
	final char CTRL_D = 4;
	final char CTRL_E = 5;
	final char CTRL_F = 6;
	final char CTRL_N = 14;
	final char CTRL_P = 16;


	/**
	 *	Logical constants for key operations.
	 */

	/**
	 *  Unknown operation.
	 */
	final short UNKNOWN				= -99;

	/**
	 *  Operation that moves to the beginning of the buffer.
	 */
	final short MOVE_TO_BEG			= -1;

	/**
	 *  Operation that moves to the end of the buffer.
	 */
	final short MOVE_TO_END			= -3;

	/**
	 *  Operation that moved to the previous character in the buffer.
	 */
	final short PREV_CHAR			= -4;

	/**
	 *  Operation that issues a newline.
	 */
	final short NEWLINE				= -6;

	/**
	 *  Operation that deletes the buffer from the current character to the end.
	 */
	final short KILL_LINE			= -7;

	/**
	 *  Operation that clears the screen.
	 */
	final short CLEAR_SCREEN		= -8;

	/**
	 *  Operation that sets the buffer to the next history item.
	 */
	final short NEXT_HISTORY		= -9;

	/**
	 *  Operation that sets the buffer to the previous history item.
	 */
	final short PREV_HISTORY		= -11;

	/**
	 *  Operation that redisplays the current buffer.
	 */
	final short REDISPLAY			= -13;

	/**
	 *  Operation that deletes the buffer from the cursor to the beginning.
	 */
	final short KILL_LINE_PREV		= -15;

	/**
	 *  Operation that deletes the previous word in the buffer.
	 */
	final short DELETE_PREV_WORD	= -16;

	/**
	 *  Operation that moves to the next character in the buffer.
	 */
	final short NEXT_CHAR			= -19;

	/**
	 *  Operation that moves to the previous character in the buffer.
	 */
	final short REPEAT_PREV_CHAR	= -20;

	/**
	 *  Operation that searches backwards in the command history.
	 */
	final short SEARCH_PREV			= -21;

	/**
	 *  Operation that repeats the character.
	 */
	final short REPEAT_NEXT_CHAR	= -24;

	/**
	 *  Operation that searches forward in the command history.
	 */
	final short SEARCH_NEXT			= -25;

	/**
	 *  Operation that moved to the previous whitespace.
	 */
	final short PREV_SPACE_WORD		= -27;

	/**
	 *  Operation that moved to the end of the current word.
	 */
	final short TO_END_WORD			= -29;

	/**
	 *  Operation that
	 */
	final short REPEAT_SEARCH_PREV	= -34;

	/**
	 *  Operation that
	 */
	final short PASTE_PREV			= -36;

	/**
	 *  Operation that
	 */
	final short REPLACE_MODE		= -37;

	/**
	 *  Operation that
	 */
	final short SUBSTITUTE_LINE		= -38;

	/**
	 *  Operation that
	 */
	final short TO_PREV_CHAR		= -39;

	/**
	 *  Operation that
	 */
	final short NEXT_SPACE_WORD		= -40;

	/**
	 *  Operation that
	 */
	final short DELETE_PREV_CHAR	= -41;

	/**
	 *  Operation that
	 */
	final short ADD					= -42;

	/**
	 *  Operation that
	 */
	final short PREV_WORD			= -43;

	/**
	 *  Operation that
	 */
	final short CHANGE_META			= -44;

	/**
	 *  Operation that
	 */
	final short DELETE_META			= -45;

	/**
	 *  Operation that
	 */
	final short END_WORD			= -46;

	/**
	 *  Operation that
	 */
	final short INSERT				= -48;

	/**
	 *  Operation that
	 */
	final short REPEAT_SEARCH_NEXT	= -49;

	/**
	 *  Operation that
	 */
	final short PASTE_NEXT			= -50;

	/**
	 *  Operation that
	 */
	final short REPLACE_CHAR		= -51;

	/**
	 *  Operation that
	 */
	final short SUBSTITUTE_CHAR		= -52;

	/**
	 *  Operation that
	 */
	final short TO_NEXT_CHAR		= -53;

	/**
	 *  Operation that undoes the previous operation.
	 */
	final short UNDO				= -54;

	/**
	 *  Operation that moved to the next word.
	 */
	final short NEXT_WORD			= -55;

	/**
	 *  Operation that deletes the previous character.
	 */
	final short DELETE_NEXT_CHAR	= -56;

	/**
	 *  Operation that toggles between uppercase and lowercase.
	 */
	final short CHANGE_CASE			= -57;

	/**
	 *  Operation that performs completion operation on the current word.
	 */
	final short COMPLETE			= -58;

	/**
	 *  Operation that exits the command prompt.
	 */
	final short EXIT				= -59;

	/**
	 *  Operation that pastes the contents of the cliboard into the line
	 */
	final short PASTE				= -60;
}






