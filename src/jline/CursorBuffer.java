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
 *  A CursorBuffer is a holder for a {@link StringBuffer} that
 *  also contains the current cursor position.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CursorBuffer
{
	public int cursor = 0;
	public final StringBuffer buffer = new StringBuffer ();


	public int length ()
	{
		return buffer.length ();
	}


	public char current ()
	{
		if (cursor <= 0)
			return 0;

		return buffer.charAt (cursor - 1);
	}


	/**
	 *  Insert the specific character into the buffer, setting the
	 *  cursor position ahead one.
	 *
	 *  @param  c  the character to insert
	 */
	public void insert (final char c)
	{
		buffer.insert (cursor++, c);
	}


	/**
	 *  Insert the specified {@link String} into the buffer, setting
	 *  the cursor to the end of the insertion point.
	 *
	 *  @param  str  the String to insert. Must not be null.
	 */
	public void insert (final String str)
	{
		if (buffer.length () == 0)
			buffer.append (str);
		else
			buffer.insert (cursor, str);

		cursor += str.length ();
	}


	public String toString ()
	{
		return buffer.toString ();
	}
}





