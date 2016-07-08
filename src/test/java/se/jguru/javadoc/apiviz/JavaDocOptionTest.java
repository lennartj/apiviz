package se.jguru.javadoc.apiviz;

import com.sun.tools.doclets.standard.Standard;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import se.jguru.javadoc.apiviz.doclet.APIvizDoclet;

/**
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class JavaDocOptionTest {

    // Shared state
    private APIvizDoclet unitUnderTest;

    @Before
    public void setupSharedState() {
        unitUnderTest = new APIvizDoclet();
    }

    @Test
    public void validateKnownJavaDocOptionLengths() {

        // Act & Assert
        for(JavaDocOption current : JavaDocOption.values()) {
            final String errorMessage = "Option " + current.getOption() + " requires "
                    + current.getOptionLength() + " options, inclusive.";

            Assert.assertEquals(errorMessage,
                    current.getOptionLength(),
                    unitUnderTest.optionLength(current.getOption()));
        }
    }

    @Test
    public void validateUnknownJavaDocOptionLength() {

        // Assemble
        final String unknownOption = "FooBar";

        // Act
        final int result = unitUnderTest.optionLength(unknownOption);

        // Assert
        Assert.assertEquals(Standard.optionLength(unknownOption), result);
    }

    @Test
    public void showTestOutput() {
        unitUnderTest.optionLength(JavaDocOption.HELP.getOption());
    }
}
