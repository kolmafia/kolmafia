package net.sourceforge.kolmafia.textui.command;

public abstract class AbstractModeCommand extends AbstractCommand {
    /**
     * Basic side-effect-less command validation
     * @param command Command string
     * @param parameters Parameters as a string
     * @return Whether command is valid
     */
    public boolean validate(final String command, final String parameters) {
        return true;
    }

    /**
     * Normalise parameters to the value expected in modifiers.txt
     *
     * For example, one might specify "ml" to the "umbrella" command, but the value to compare in
     * state would be "broken".
     * @param parameters Input parameters
     * @return Appropriate state value
     */
    public String normalize(final String parameters) {
        return parameters;
    }
}
