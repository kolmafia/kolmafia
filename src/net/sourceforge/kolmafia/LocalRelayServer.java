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
 *	  notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *	  notice, this list of conditions and the following disclaimer in
 *	  the documentation and/or other materials provided with the
 *	  distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *	  its contributors may be used to endorse or promote products
 *	  derived from this software without specific prior written
 *	  permission.
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.util.Vector;

public class LocalRelayServer implements Runnable
{
	private static Thread relayThread = null;
	private static final LocalRelayServer INSTANCE = new LocalRelayServer();

	private static final int MAX_AGENT_THREADS = 9;
	private static final int TIMEOUT = 5000;

	protected static Vector agentThreads = new Vector();
	private ServerSocket serverSocket = null;
	private static int port = 60080;
	private static boolean listening = false;

	private static long lastRequest = 0;
	private static StringBuffer statusMessages = new StringBuffer();

	private LocalRelayServer()
	{
	}

	public static void startThread()
	{
		if ( relayThread != null )
			return;

		relayThread = new Thread( INSTANCE );
		relayThread.start();
	}

	public static int getPort()
	{	return port;
	}

	public static boolean isRunning()
	{	return listening;
	}

	public synchronized void run()
	{
		port = 60080;
		while ( port <= 60090 && !openServerSocket() )
			++port;

		// Initialize all the agent threads first
		// to ensure every request is caught.

		for ( int i = 0; i < MAX_AGENT_THREADS; ++i )
		{
			RelayAgent agent = new RelayAgent();
			(new Thread( agent )).start();
			agentThreads.add( agent );
		}

		listening = true;
		while ( listening )
		{
			try
			{
				dispatchAgent( serverSocket.accept() );
			}
			catch ( Exception e )
			{
				// If an exception occurs here, that means
				// someone closed the thread; just reset
				// the listening state and fall through.

				listening = false;
			}
		}

		closeAgents();

		try
		{
			if ( serverSocket != null )
				serverSocket.close();
		}
		catch ( Exception e )
		{
			// The end result of a socket closing
			// should not throw an exception, but
			// if it does, the socket closes.
		}

		serverSocket = null;
		agentThreads.clear();
		relayThread = null;
	}

	private boolean openServerSocket()
	{
		try
		{
			serverSocket = new ServerSocket( port, 25, InetAddress.getByName( "127.0.0.1" ) );
			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private void closeAgents()
	{
		synchronized ( agentThreads )
		{
			while ( !agentThreads.isEmpty() )
			{
				RelayAgent agent = (RelayAgent) agentThreads.elementAt( 0 );
				agentThreads.removeElementAt( 0 );
				agent.setSocket( null );
			}
		}
	}

	private void dispatchAgent( Socket socket )
	{
		RelayAgent agent = null;
		synchronized ( agentThreads )
		{
			for ( int i = 0; i < agentThreads.size(); ++i )
			{
				agent = (RelayAgent) agentThreads.elementAt(i);
				if ( agent.isWaiting() )
				{
					agent.setSocket( socket );
					return;
				}
			}
		}
	}

	public static void addStatusMessage( String message )
	{
		synchronized ( statusMessages )
		{
			// If it's been more than 10 seconds, since the
			// last buffer was cleared, that means no monitoring
			// is happening, so you can clear the buffer and
			// ignore the addition (since no one is checking).

			if ( lastRequest == 0 || lastRequest - System.currentTimeMillis() > 10000 )
			{
				if ( statusMessages.length() > 0 )
					statusMessages.setLength( 0 );

				return;
			}

			statusMessages.append( message );
		}
	}

	public static String getNewStatusMessages()
	{
		synchronized ( statusMessages )
		{
			lastRequest = System.currentTimeMillis();
			String newMessages = statusMessages.toString();
			statusMessages.setLength(0);
			return newMessages;
		}
	}

	private class RelayAgent implements Runnable
	{
		protected Socket socket = null;

		boolean isWaiting()
		{	return socket == null;
		}

		synchronized void setSocket( Socket socket )
		{
			this.socket = socket;
			notify();
		}

		public synchronized void run()
		{
			while ( true )
			{
				if ( socket == null )
				{
					// wait for a client
					try
					{
						// Wait indefinitely for a client.  Exception
						// handling is probably not the best way to
						// handle this, but for now, it will do.

						wait();
					}
					catch ( InterruptedException e )
					{
						// We expect this to happen only when we are
						// interrupted.  Fall through.
					}
				}

				try
				{
					if ( socket != null )
						performRelay();
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
				}

				socket = null;
			}
		}

		protected void sendHeaders( PrintStream printStream, LocalRelayRequest request ) throws IOException
		{
			String header = null;
			boolean hasPragmaHeader = false;
			boolean hasCacheHeader = false;
			boolean hasConnectionHeader = false;

			for ( int i = 0; null != ( header = request.getHeader( i ) ); ++i )
			{
				if ( header.startsWith( "Cache-Control" ) )
				{
					header = "Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0";
					hasCacheHeader = true;
				}

				if ( header.startsWith( "Pragma:" ) )
				{
					header = "Pragma: no-cache";
					hasPragmaHeader = true;
				}

				if ( header.startsWith( "Connection:" ) )
				{
					header = "Connection: close";
					hasConnectionHeader = true;
				}

				printStream.println( header );
			}

			if ( !hasCacheHeader )
			{
				printStream.println( "Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0" );
			}

			if ( !hasPragmaHeader )
			{
				printStream.println( "Pragma: no-cache" );
			}

			if ( !hasConnectionHeader )
			{
				printStream.println( "Connection: close" );
			}
		}

		protected void performRelay() throws IOException
		{
			if ( socket == null )
				return;

			socket.setSoTimeout( TIMEOUT );
			socket.setTcpNoDelay( true );

			try
			{
				BufferedReader reader = new BufferedReader( new InputStreamReader( new BufferedInputStream( socket.getInputStream() ) ) );
				PrintStream printStream = new PrintStream( socket.getOutputStream() );

				String line;
				String path;
				String method;

				line = reader.readLine();
				if ( line == null )
					return;

				String [] tokens = line.trim().split( " " );
				method = tokens[0];
				LocalRelayRequest request = new LocalRelayRequest( StaticEntity.getClient(), tokens[1], false );

				int contentLength = 0;
				while ( (line = reader.readLine()) != null && line.trim().length() != 0 )
				{
					if ( line.indexOf( ": " ) == -1 )
						continue;

					tokens = line.split( "(: )" );
					if ( tokens[0].equals( "Content-Length" ) )
						contentLength = StaticEntity.parseInt( tokens[1].trim() );
				}

				if ( method.equals( "POST" ) )
				{
					StringBuffer postBuffer = new StringBuffer();
					for ( int i = 0; i < contentLength; ++i )
						postBuffer.append( (char) reader.read() );

					request.addEncodedFormFields( postBuffer.toString() );
				}

				request.run();

				sendHeaders( printStream, request );
				printStream.println();
				printStream.print( request.getFullResponse() );
				printStream.close();
			}
			finally
			{	socket.close();
			}
		}
	}
}
