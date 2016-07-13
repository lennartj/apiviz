package se.jguru.javadoc.apiviz.diagram;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr4.grammar.DOTLexer;
import org.antlr4.grammar.DOTParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple validator of a generated .dot file.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class DiagramValidator implements SimpleValidator {

    // Internal state
    private InputStream digraphStream;
    private DiGraphSyntaxListener validatorListener;

    /**
     * Default constructor creating internal state of this {@link DiagramValidator}.
     * @param digraphStream An input stream connected to a digraph document.
     */
    public DiagramValidator(final InputStream digraphStream) {

        // Check sanity
        if(digraphStream == null) {
            throw new NullPointerException("Cannot handle null 'digraphStream' argument.");
        }

        // Assign internal state.
        this.validatorListener = new DiGraphSyntaxListener();
        this.digraphStream = digraphStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws IllegalStateException {

        try {
            final DOTLexer dotLexer = new DOTLexer(new ANTLRInputStream(digraphStream));
            final DOTParser dotParser = new DOTParser(new CommonTokenStream(dotLexer));
            dotParser.addErrorListener(validatorListener);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse InputStream as a Graphviz/DOT digraph.", e);
        }

        // Delegate the validation
        validatorListener.validate();
    }
}
