package net.sourceforge.kolmafia;

import java.io.File;
import java.io.FileNotFoundException;

public class LogStream extends java.io.PrintStream
{
	public LogStream( File file ) throws FileNotFoundException
	{	super( file );
	}

	public LogStream( String fileName ) throws FileNotFoundException
	{	super( fileName );
	}

	public void println( RuntimeException e )
	{
		println( "============================" );
		println( "  Caught Runtime Exception  " );
		println( "============================" );
		println();

		e.printStackTrace( this );
		println();
	}
}