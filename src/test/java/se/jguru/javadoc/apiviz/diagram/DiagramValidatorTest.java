package se.jguru.javadoc.apiviz.diagram;

import org.junit.Test;

import java.io.InputStream;

/**
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class DiagramValidatorTest {

    @Test
    public void verifyValidDiagram() {
        validateDotDiagram("testdata/diagram/unixes.dot");
    }

    @Test
    public void verifyInvalidDiagram() {
        validateDotDiagram("testdata/diagram/invalid.dot");
    }

    //
    // Private helpers
    //

    private void validateDotDiagram(final String resourcePath) {

        final InputStream dotStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        final DiagramValidator unitUnderTest = new DiagramValidator(dotStream);

        // Validate
        unitUnderTest.validate();
    }
}
