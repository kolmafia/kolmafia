package net.sourceforge.kolmafia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

public class LogStream extends java.io.PrintStream
{
	public LogStream( String fileName ) throws FileNotFoundException
	{	this( new File( fileName ) );
	}

	public LogStream( File file ) throws FileNotFoundException
	{
		super( new FileOutputStream( file, true ) );
		println();
		println();
		println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		println( "                  Beginning New Logging Session" );
		println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		println();
		println();
	}

	public void println( RuntimeException e )
	{
		println();
		println( "****************************" );
		println( "Runtime Exception:" );
		println();

		e.printStackTrace( this );

		println( "****************************" );
		println();
		println();
	}
}