package net.sourceforge.kolmafia.textui;

public class ScriptException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ScriptException(final String message) {
    super(message);
  }

  public ScriptException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ScriptException(final Throwable cause) {
    super(cause);
  }
}
