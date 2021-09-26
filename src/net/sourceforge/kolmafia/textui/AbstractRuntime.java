package net.sourceforge.kolmafia.textui;

import java.io.PrintStream;
import java.util.LinkedHashMap;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.NullStream;

public abstract class AbstractRuntime
        implements ScriptRuntime
{
        private State runtimeState = State.EXIT;

        // For relay scripts.
        private RelayRequest relayRequest = null;
        private StringBuffer serverReplyBuffer = null;

        // For use by RuntimeLibrary's CLI command batching feature
        private LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched;

        private int traceIndentation = 0;

        @Override
        public Value execute( final String functionName, final Object[] parameters )
        {
                return execute( functionName, parameters, true );
        }

        public abstract Value execute( final String functionName, final Object[] parameters, final boolean executeTopLevel );

        public abstract ScriptException runtimeException( final String message );

        public abstract ScriptException runtimeException2( final String message1, final String message2 );

        @Override
        public void initializeRelayScript( final RelayRequest request )
        {
                this.relayRequest = request;
                if ( this.serverReplyBuffer == null )
                {
                        this.serverReplyBuffer = new StringBuffer();
                }
                else
                {
                        this.serverReplyBuffer.setLength( 0 );
                }

                // Allow a relay script to execute regardless of error state
                KoLmafia.forceContinue();
        }

        @Override
        public RelayRequest getRelayRequest()
        {
                return relayRequest;
        }

        @Override
        public StringBuffer getServerReplyBuffer()
        {
                return serverReplyBuffer;
        }

        @Override
        public void finishRelayScript()
        {
                this.relayRequest = null;
                this.serverReplyBuffer = null;
        }

        @Override
        public void cloneRelayScript( final ScriptRuntime caller )
        {
                this.finishRelayScript();
                if ( caller != null )
                {
                        this.relayRequest = caller.getRelayRequest();
                        this.serverReplyBuffer = caller.getServerReplyBuffer();
                }
        }

        @Override
        public State getState()
        {
                return runtimeState;
        }

        @Override
        public void setState( final State newState )
        {
                runtimeState = newState;
        }

        @Override
        public LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> getBatched()
        {
                return batched;
        }

        @Override
        public void setBatched( LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched )
        {
                this.batched = batched;
        }

        // **************** Tracing *****************

        @Override
        public final void resetTracing()
        {
                this.traceIndentation = 0;
        }

        private static final String indentation = " " + " " + " ";

        public static void indentLine( final PrintStream stream, final int indent )
        {
                if ( stream != null && stream != NullStream.INSTANCE )
                {
                        for ( int i = 0; i < indent; ++i )
                        {
                                stream.print( indentation );
                        }
                }
        }

        protected void indentLine( final int indent )
        {
                if ( ScriptRuntime.isTracing() )
                {
                        AbstractRuntime.indentLine( ScriptRuntime.traceStream.getStream(), indent );
                }
        }

        @Override
        public final void traceIndent()
        {
                this.traceIndentation++;
        }

        @Override
        public final void traceUnindent()
        {
                this.traceIndentation--;
        }

        @Override
        public final void trace( final String string )
        {
                if ( ScriptRuntime.isTracing() )
                {
                        this.indentLine( this.traceIndentation );
                        traceStream.println( string );
                }
        }
}
