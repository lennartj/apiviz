package se.jguru.javadoc.apiviz.diagram;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Trivial validator implementation which throws an Exception containing all error messages.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class DiGraphSyntaxListener extends BaseErrorListener implements SimpleValidator {

    // Internal state
    private List<String> errorMessages = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws IllegalStateException {

        if(!errorMessages.isEmpty()) {

            final StringBuilder builder = new StringBuilder("Encountered " + errorMessages.size()
                    + " validation errors in digraph:\n");
            for(int i = 0; i < errorMessages.size(); i++) {
                builder.append("" + i + ": " + errorMessages.get(i));
            }

            // All Done.
            throw new IllegalStateException(builder.toString());
        }
    }

    /**
     * {@code}
     */
    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer,
                            final Object offendingSymbol,
                            final int line,
                            final int charPositionInLine,
                            final String msg,
                            final RecognitionException e) {
        errorMessages.add("[Line: " + line + ", pos: " + charPositionInLine + "]: " + msg);
    }
}
