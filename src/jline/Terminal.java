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

import java.io.*;

/**
 *  Representation of the input terminal for a platform. Handles
 *	any initialization that the platform may need to perform
 *	in order to allow the {@link ConsoleReader} to correctly handle
 *	input.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public abstract class Terminal
	implements ConsoleOperations
{
	private static Terminal term;


	/**
	 *  @see #setupTerminal
	 */
	public static Terminal getTerminal ()
	{
		return setupTerminal ();
	}


	/**
	 *  <p>Configure and return the {@link Terminal} instance for the
	 *  current platform. This will initialize any system settings
	 *  that are required for the console to be able to handle
	 *  input correctly, such as setting tabtop, buffered input, and
	 *  character echo.</p>
	 *
	 *  <p>This class will use the Terminal implementation specified in the
	 *  <em>jline.terminal</em> system property, or, if it is unset, by
	 *  detecting the operating system from the <em>os.name</em>
	 *  system property and instantiateing either the
	 *  {@link WindowsTerminal} or {@link UnixTerminal}.
	 *
	 *  @see #initializeTerminal
	 */
	public static synchronized Terminal setupTerminal ()
	{
		if (term != null)
			return term;

		final Terminal t;

		String os = System.getProperty ("os.name").toLowerCase ();
		String termProp = System.getProperty ("jline.terminal");
		if (termProp != null && termProp.length () > 0)
		{
			try
			{
				t = (Terminal)Class.forName (termProp).newInstance ();
			}
			catch (Exception e)
			{
				throw (IllegalArgumentException)new IllegalArgumentException (
					e.toString ()).fillInStackTrace ();
			}
		}
		else if (os.indexOf ("windows") != -1)
		{
			t = new WindowsTerminal ();
		}
		else
		{
			t = new UnixTerminal ();
		}

		try
		{
			t.initializeTerminal ();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			return term = new UnsupportedTerminal ();
		}

		return term = t;
	}


	/** 
	 *  Returns true if the current console supports ANSI
	 *  codes.
	 */
	public boolean isANSISupported ()
	{
		return true;
	}	


	/**
	 *  Read a single character from the input stream. This might
	 *  enable a terminal implementation to better handle nuances of
	 *  the console.
	 */
	public int readCharacter (final InputStream in)
		throws IOException
	{
		return in.read ();
	}


	/** 
	 *  Reads a virtual key from the console. Typically, this will
	 *  just be the raw character that was entered, but in some cases,
	 *  multiple input keys will need to be translated into a single
	 *  virtual key.
	 *  
	 *  @param  in  the InputStream to read from
	 *  @return  the virtual key (e.g., {@link ConsoleOperations#VK_UP})
	 */
	public int readVirtualKey (InputStream in)
		throws IOException
	{
		return readCharacter (in);
	}


	/**
	 *  Initialize any system settings
	 *  that are required for the console to be able to handle
	 *  input correctly, such as setting tabtop, buffered input, and
	 *  character echo.
	 */
	public abstract void initializeTerminal ()
		throws Exception;


	/**
	 *  Returns the current width of the terminal (in characters)
	 */
	public abstract int getTerminalWidth ();


	/**
	 *  Returns the current height of the terminal (in lines)
	 */
	public abstract int getTerminalHeight ();


	/**
	 *  Returns true if this terminal is capable of initializing the
	 *  terminal to use jline.
	 */
	public abstract boolean isSupported ();


	/**
	 *  Returns true if the terminal will echo all characters type.
	 */
	public abstract boolean getEcho ();
}
