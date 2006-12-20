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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *  A command history buffer.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class History
{
	private List			history			= new ArrayList ();
	private PrintWriter		output			= null;
	private int				maxSize			= 500;
	private int				currentIndex	= 0;


	/**
	 *  Construstor: initialize a blank history.
	 */
	public History ()
	{
	}


	/**
	 *  Construstor: initialize History object the the specified
	 *  {@link File} for storage.
	 */
	public History (final File historyFile)
		throws IOException
	{
		setHistoryFile (historyFile);
	}


	public void setHistoryFile (final File historyFile)
		throws IOException
	{
		if (historyFile.isFile ())
			load (new FileInputStream (historyFile));
		setOutput (new PrintWriter (new FileWriter (historyFile), true));
		flushBuffer ();
	}


	/**
	 *  Load the history buffer from the specified InputStream.
	 */
	public void load (final InputStream in)
		throws IOException
	{
		load (new InputStreamReader (in));
	}


	/**
	 *  Load the history buffer from the specified Reader.
	 */
	public void load (final Reader reader)
		throws IOException
	{
		BufferedReader breader = new BufferedReader (reader);
		List lines = new ArrayList ();
		String line;
		while ((line = breader.readLine ()) != null)
		{
			lines.add (line);
		}

		for (Iterator i = lines.iterator (); i.hasNext (); )
			addToHistory ((String)i.next ());
	}


	public int size ()
	{
		return history.size ();
	}


	/**
	 *  Clear the history buffer
	 */
	public void clear ()
	{
		history.clear ();
		currentIndex = 0;
	}


	/**
	 *  Add the specified buffer to the end of the history. The pointer is
	 *  set to the end of the history buffer.
	 */
	public void addToHistory (final String buffer)
	{
		// don't append duplicates to the end of the buffer
		if (history.size () != 0 && buffer.equals (
			history.get (history.size () - 1)))
			return;

		history.add (buffer);
		while (history.size () > getMaxSize ())
			history.remove (0);

		currentIndex = history.size ();

		if (getOutput () != null)
		{
			getOutput ().println (buffer);
			getOutput ().flush ();
		}
	}


	/**
	 *  Flush the entire history buffer to the output PrintWriter.
	 */
	public void flushBuffer ()
		throws IOException
	{
		if (getOutput () != null)
		{
			for (Iterator i = history.iterator (); i.hasNext ();
				getOutput ().println ((String)i.next ()));

			getOutput ().flush ();
		}
	}


	/**
	 *  Move to the end of the history buffer.
	 */
	public void moveToEnd ()
	{
		currentIndex = history.size ();
	}


	/**
	 *  Set the maximum size that the history buffer will store.
	 */
	public void setMaxSize (final int maxSize)
	{
		this.maxSize = maxSize;
	}


	/**
	 *  Get the maximum size that the history buffer will store.
	 */
	public int getMaxSize ()
	{
		return this.maxSize;
	}


	/**
	 *  The output to which all history elements will be written (or null
	 *  of history is not saved to a buffer).
	 */
	public void setOutput (final PrintWriter output)
	{
		this.output = output;
	}


	/**
	 *  Returns the PrintWriter that is used to store history elements.
	 */
	public PrintWriter getOutput ()
	{
		return this.output;
	}


	/**
	 *  Returns the current history index.
	 */
	public int getCurrentIndex ()
	{
		return this.currentIndex;
	}


	/**
	 *  Return the content of the current buffer.
	 */
	public String current ()
	{
		if (currentIndex >= history.size ())
			return "";

		return (String)history.get (currentIndex);
	}


	/**
	 *  Move the pointer to the previous element in the buffer.
	 *
	 *  @return  true if we successfully went to the previous element
	 */
	public boolean previous ()
	{
		if (currentIndex <= 0)
			return false;

		currentIndex--;
		return true;
	}


	/**
	 *  Move the pointer to the next element in the buffer.
	 *
	 *  @return  true if we successfully went to the next element
	 */
	public boolean next ()
	{
		if (currentIndex >= history.size ())
			return false;

		currentIndex++;
		return true;
	}


	/**
	 *  Returns an immutable list of the history buffer.
	 */
	public List getHistoryList ()
	{
		return Collections.unmodifiableList (history);
	}


	/**
	 *  Returns the standard {@link AbstractCollection#toString} representation
	 *  of the history list.
	 */
	public String toString ()
	{
		return history.toString ();
	}
}

