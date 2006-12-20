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

import java.io.InputStream;
import java.io.IOException;
import java.io.SequenceInputStream;

import java.util.Enumeration;

/**
 *	An {@link InputStream} implementation that wraps a {@link ConsoleReader}.
 *	It is useful for setting up the {@link System#in} for a generic
 *	console.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleReaderInputStream
	extends SequenceInputStream
{
	private static InputStream systemIn = System.in;


	public static void setIn ()
		throws IOException
	{
		setIn (new ConsoleReader ());
	}


	public static void setIn (final ConsoleReader reader)
	{
		System.setIn (new ConsoleReaderInputStream (reader));
	}


	/**
	 *  Restore the original {@link System#in} input stream.
	 */
	public static void restoreIn ()
	{
		System.setIn (systemIn);
	}


	public ConsoleReaderInputStream (final ConsoleReader reader)
	{
		super (new ConsoleEnumeration (reader));
	}


	private static class ConsoleEnumeration
		implements Enumeration
	{
		private final ConsoleReader reader;
		private ConsoleLineInputStream next = null;
		private ConsoleLineInputStream prev = null;


		public ConsoleEnumeration (final ConsoleReader reader)
		{
			this.reader = reader;
		}


		public Object nextElement ()
		{
			if (next != null)
			{
				InputStream n = next;
				prev = next;
				next = null;
				return n;
			}

			return new ConsoleLineInputStream (reader);
		}


		public boolean hasMoreElements ()
		{
			// the last line was null
			if (prev != null && prev.wasNull == true)
				return false;

			if (next == null)
				next = (ConsoleLineInputStream)nextElement ();

			return next != null;
		}
	}


	private static class ConsoleLineInputStream
		extends InputStream
	{
		private final ConsoleReader reader;
		private String line = null;
		private int index = 0;
		private boolean eol = false;
		protected boolean wasNull = false;

		public ConsoleLineInputStream (final ConsoleReader reader)
		{
			this.reader = reader;
		}


		public int read ()
			throws IOException
		{
			if (eol)
				return -1;

			if (line == null)
				line = reader.readLine ();

			if (line == null)
			{
				wasNull = true;
				return -1;
			}

			if (index >= line.length ())
			{
				eol = true;
				return '\n'; // lines are ended with a newline
			}

			return line.charAt (index++);
		}
	}
}

