/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import net.java.dev.spellcast.utilities.ChatBuffer;

public class LimitedSizeChatBuffer extends ChatBuffer
{
	private int maximumSize;
	private int previousFontSize;

	private static int fontSize = 3;
	static
	{
		setFontSize( fontSize );
		BUFFER_STOP = "</font></body>";
	}

	public LimitedSizeChatBuffer( String title, int maximumSize )
	{
		super( title );
		this.maximumSize = maximumSize;
		previousFontSize = fontSize;
	}

	/**
	 * Used to change the font size for all current chat buffers.  Note that
	 * this does not affect logging.
	 */

	public static void setFontSize( int fontSize )
	{
		LimitedSizeChatBuffer.fontSize = fontSize;
		ChatBuffer.BUFFER_INIT = "<body><font face=\"sans-serif\" size=" + fontSize + ">";
	}

	/**
	 * Appends the given message to the chat buffer.
	 * @param	message	The message to be appended to this <code>ChatBuffer</code>
	 */

	public void append( String message )
	{
		int totalLength = displayBuffer.length() + message.length();
		if ( totalLength > maximumSize )
		{
			int index = 1;
			while ( totalLength - index > maximumSize )
				index = displayBuffer.indexOf( "\n", index ) + 1 ;
			displayBuffer.delete( 0 , index );
		}

		super.append( message );
		previousFontSize = fontSize;
	}
}