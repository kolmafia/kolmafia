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
import java.util.*;
import java.text.MessageFormat;

/**
 *	<p>
 *	A {@link CompletionHandler} that deals with multiple distinct completions
 *	by outputting the complete list of possibilities to the console. This
 *	mimics the behavior of the
 *	<a href="http://www.gnu.org/directory/readline.html">readline</a>
 *	library.
 *	</p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CandidateListCompletionHandler
	implements CompletionHandler
{
	public boolean complete (final ConsoleReader reader,
		final List candidates, final int pos)
		throws IOException
	{
		CursorBuffer buf = reader.getCursorBuffer ();

		// if there is only one completion, then fill in the buffer
		if (candidates.size () == 1)
		{
			String value = candidates.get (0).toString ();

			// fail if the only candidate is the same as the current buffer
			if (value.equals (buf.toString ()))
				return false;
			setBuffer (reader, value, pos);
			return true;
		}
		else if (candidates.size () > 1)
		{
			String value = getUnambiguousCompletions (candidates);
			String bufString = buf.toString ();
			setBuffer (reader, value, pos);

			// if we have changed the buffer, then just return withough
			// printing out all the subsequent candidates
			if (bufString.length () - pos + 1 != value.length ())
				return true;
		}

		reader.printNewline ();
		printCandidates (reader, candidates);

		// redraw the current console buffer
		reader.drawLine ();

		return true;
	}


	private static void setBuffer (ConsoleReader reader,
		String value, int offset)
		throws IOException
	{
		while (reader.getCursorBuffer ().cursor >= offset
			&& reader.backspace ());
		reader.putString (value);
		reader.setCursorPosition (offset + value.length ());
	}


	/**
	 *  Print out the candidates. If the size of the candidates
	 *  is greated than the {@link getAutoprintThreshhold},
	 *  they prompt with aq warning.
	 *
	 *  @param  candidates  the list of candidates to print
	 */
	private final void printCandidates (ConsoleReader reader,
		Collection candidates)
		throws IOException
	{
		Set distinct = new HashSet (candidates);

		if (distinct.size () > reader.getAutoprintThreshhold ())
		{
			reader.printString (MessageFormat.format (
				"Display all possibilities? (y or n)",
				new Object [] { new Integer (candidates.size ()) } ) + " ");

			reader.flushConsole ();

			int c;

			String noOpt = "n";
			String yesOpt = "y";

			while ((c = reader.readCharacter (
				new char[] { yesOpt.charAt (0), noOpt.charAt (0) })) != -1)
			{
				if (noOpt.startsWith (new String (new char[] {(char)c})))
				{
					reader.printNewline ();
					return;
				}
				else if (yesOpt.startsWith (new String (new char[] {(char)c})))
				{
					break;
				}
				else
				{
					reader.beep ();
				}
			}
		}

		// copy the values and make them distinct, without otherwise
		// affecting the ordering. Only do it if the sizes differ.
		if (distinct.size () != candidates.size ())
		{
			Collection copy = new ArrayList ();
			for (Iterator i = candidates.iterator (); i.hasNext (); )
			{
				Object next = i.next ();
				if (!(copy.contains (next)))
					copy.add (next);
			}

			candidates = copy;
		}

		reader.printNewline ();
		reader.printColumns (candidates);
	}




	/**
	 *  Returns a root that matches all the {@link String} elements
	 *  of the specified {@link List}, or null if there are
	 *  no commalities. For example, if the list contains
	 *  <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the
	 *  method will return <i>foob</i>.
	 */
	private final String getUnambiguousCompletions (final List candidates)
	{
		if (candidates == null || candidates.size () == 0)
			return null;

		// convert to an array for speed
		String [] strings = (String [])candidates.toArray (
			new String [candidates.size ()]);

		String first = strings [0];
		StringBuffer candidate = new StringBuffer ();
		for (int i = 0; i < first.length (); i++)
		{
			if (startsWith (first.substring (0, i + 1), strings))
				candidate.append (first.charAt (i));
			else
				break;
		}

		return candidate.toString ();
	}


	/**
	 *  @return  true is all the elements of <i>candidates</i>
	 *  			start with <i>starts</i>
	 */
	private final boolean startsWith (final String starts,
		final String [] candidates)
	{
		for (int i = 0; i < candidates.length; i++)
		{
			if (!candidates [i].startsWith (starts))
				return false;
		}

		return true;
	}
}

