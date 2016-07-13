package se.jguru.javadoc.apiviz.diagram;

/**
 * Trivial validator implementation.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public interface SimpleValidator {

    /**
     * Validates state - which either passes or throws an {@link IllegalStateException}.
     *
     * @throws IllegalStateException if this {@link SimpleValidator} did not properly validate its state.
     */
    void validate() throws IllegalStateException;
}
