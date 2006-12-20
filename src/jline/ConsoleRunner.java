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

import java.io.File;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 *	<p>
 *	A pass-through application that sets the system input stream to a
 *	{@link ConsoleReader} and invokes the specified main method.
 *	</p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleRunner
{
	public static final String property = "jline.history";

	public static void main (final String[] args)
		throws Exception
	{
		String historyFileName = null;

		List argList = new ArrayList (Arrays.asList (args));
		if (argList.size () == 0)
		{
			usage ();
			return;
		}

		historyFileName = System.getProperty(ConsoleRunner.property, null);

		// invoke the main() method
		String mainClass = (String)argList.remove (0);

		// setup the inpout stream
		ConsoleReader reader = new ConsoleReader ();
		if (historyFileName != null)
		{
			reader.setHistory (new History (new File (
					System.getProperty ("user.home"), ".jline-" + mainClass
					+ "." + historyFileName + ".history")));
		}
		else
		{
			reader.setHistory (new History (new File (
				System.getProperty ("user.home"), ".jline-" + mainClass
					+ ".history")));
		}

		String completors = System.getProperty (ConsoleRunner.class.getName ()
			+ ".completors", "");
		List completorList = new ArrayList ();
		for (StringTokenizer tok = new StringTokenizer (completors, ",");
			tok.hasMoreTokens (); )
		{
			completorList.add ((Completor)Class.forName (tok.nextToken ())
				.newInstance ());
		}

		if (completorList.size () > 0)
			reader.addCompletor (new ArgumentCompletor (completorList));

		ConsoleReaderInputStream.setIn (reader);
		try
		{
			Class.forName (mainClass)
				.getMethod ("main", new Class[] { String[].class})
				.invoke (null, new Object[] { argList.toArray (new String[0])});
		}
		finally
		{
			// just in case this main method is called from another program
			ConsoleReaderInputStream.restoreIn ();
		}
	}


	private static void usage ()
	{
		System.out.println ("Usage: \n   java "
			+ "[-Djline.history='name'] "
			+ ConsoleRunner.class.getName ()
			+ " <target class name> [args]"
			+ "\n\nThe -Djline.history option will avoid history"
			+ "\nmangling when running ConsoleRunner on the same application."
			+ "\n\nargs will be passed directly to the target class name.");
	}
}

