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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Map;
import java.util.StringTokenizer;

/**
 *  <p>
 *  Terminal that is used for unix platforms. Terminal initialization
 *  is handled by issuing the <em>stty</em> command against the
 *  <em>/dev/tty</em> file to disable character echoing and enable
 *  character input. All known unix systems (including
 *  Linux and Macintosh OS X) support the <em>stty</em>), so this
 *  implementation should work for an reasonable POSIX system.
 *	</p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *  @author  Updates <a href="mailto:dwkemp@gmail.com">Dale Kemp</a> 2005-12-03
 */
public class UnixTerminal
	extends Terminal
{
	public static final short ARROW_START           = 27;
	public static final short ARROW_PREFIX          = 91;
	public static final short ARROW_LEFT            = 68;
	public static final short ARROW_RIGHT           = 67;
	public static final short ARROW_UP              = 65;
	public static final short ARROW_DOWN            = 66;
	public static final short HOME_CODE 			= 72;
	public static final short END_CODE 				= 70;

	private Map terminfo;


	/**
	 *  Remove line-buffered input by invoking "stty -icanon min 1"
	 *  against the current terminal.
	 */
	public void initializeTerminal ()
		throws IOException, InterruptedException
	{
		// save the initial tty configuration
		final String ttyConfig = stty ("-g");

		// sanity check
		if (ttyConfig.length () == 0
			|| (ttyConfig.indexOf ("=") == -1
			&& ttyConfig.indexOf (":") == -1))
		{
			throw new IOException ("Unrecognized stty code: " + ttyConfig);
		}


		// set the console to be character-buffered instead of line-buffered
		stty ("-icanon min 1");

		// disable character echoing
		stty ("-echo");
	}


	public int readVirtualKey (InputStream in)
		throws IOException
	{
		int c = readCharacter (in);

		// in Unix terminals, arrow keys are represented by
		// a sequence of 3 characters. E.g., the up arrow
		// key yields 27, 91, 68
		if (c == ARROW_START)
		{
			c = readCharacter (in);
			if (c == ARROW_PREFIX)
			{
				c = readCharacter (in);
				if (c == ARROW_UP)
					return CTRL_P;
				else if (c == ARROW_DOWN)
					return CTRL_N;
				else if (c == ARROW_LEFT)
					return CTRL_B;
				else if (c == ARROW_RIGHT)
					return CTRL_F;
				else if (c == HOME_CODE)
					return CTRL_A;
				else if (c == END_CODE)
					return CTRL_E;
			}
		}


		return c;
	}


	/**
	 *  No-op for exceptions we want to silently consume.
	 */
	private void consumeException (Throwable e)
	{
	}


	public boolean isSupported ()
	{
		return true;
	}


	public boolean getEcho ()
	{
		return false;
	}


	/**
 	 *	Returns the value of "stty size" width param.
	 *
	 *	<strong>Note</strong>: this method caches the value from the
	 *	first time it is called in order to increase speed, which means
	 *	that changing to size of the terminal will not be reflected
	 *	in the console.
 	 */
	public int getTerminalWidth ()
	{
		int val = -1;

		try
		{
			val = getTerminalProperty ("columns");
		}
		catch (Exception e)
		{
		}

		if (val == -1)
			val = 80;

		return val;
	}


	/**
 	 *	Returns the value of "stty size" height param.
	 *
	 *	<strong>Note</strong>: this method caches the value from the
	 *	first time it is called in order to increase speed, which means
	 *	that changing to size of the terminal will not be reflected
	 *	in the console.
 	 */
	public int getTerminalHeight ()
	{
		int val = -1;

		try
		{
			val = getTerminalProperty ("rows");
		}
		catch (Exception e)
		{
		}

		if (val == -1)
			val = 24;

		return val;
	}


	private static int getTerminalProperty (String prop)
		throws IOException, InterruptedException
	{
		// need to be able handle both output formats:
		// speed 9600 baud; 24 rows; 140 columns;
		// and:
		// speed 38400 baud; rows = 49; columns = 111; ypixels = 0; xpixels = 0;
		String props = stty ("-a");

		for (StringTokenizer tok = new StringTokenizer (props, ";\n");
			tok.hasMoreTokens (); )
		{
			String str = tok.nextToken ().trim ();
			if (str.startsWith (prop))
			{
				int index = str.lastIndexOf (" ");
				return Integer.parseInt (str.substring (index).trim ());
			}
			else if (str.endsWith (prop))
			{
				int index = str.indexOf (" ");
				return Integer.parseInt (str.substring (0, index).trim ());
			}
		}

		return -1;
	}


	/**
	 *  Execute the stty command with the specified arguments
	 *  against the current active terminal.
	 */
	private static String stty (final String args)
		throws IOException, InterruptedException
	{
		return exec ("stty " + args + " < /dev/tty").trim ();
	}


	/**
	 *  Execute the specified command and return the output
	 *  (both stdout and stderr).
	 */
	private static String exec (final String cmd)
		throws IOException, InterruptedException
	{
		return exec (new String [] { "sh", "-c", cmd });
	}


	/**
	 *  Execute the specified command and return the output
	 *  (both stdout and stderr).
	 */
	private static String exec (final String [] cmd)
		throws IOException, InterruptedException
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream ();

		Process p = Runtime.getRuntime ().exec (cmd);
		int c;
		InputStream in;

		in = p.getInputStream ();
		while ((c = in.read ()) != -1)
			bout.write (c);

		in = p.getErrorStream ();
		while ((c = in.read ()) != -1)
			bout.write (c);

		p.waitFor ();

		String result = new String (bout.toByteArray ());
		return result;
	}


	public static void main (String[] args)
	{
		System.out.println ("width: " + new UnixTerminal ().getTerminalWidth ());
		System.out.println ("height: " + new UnixTerminal ().getTerminalHeight ());
	}
}

