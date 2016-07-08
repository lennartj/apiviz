/*
 * #%L
 * APIviz Project
 * %%
 * Copyright (C) 2010 - 2015 jGuru Europe AB
 * %%
 * Licensed under the jGuru Europe AB license (the "License"), based
 * on Apache License, Version 2.0; you may not use this file except
 * in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *       http://www.jguru.se/licenses/jguruCorporateSourceLicense-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package se.jguru.javadoc.apiviz;

/**
 * Enumeration of available JavaDoc command-line option flags for the GraphViz doclet.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public enum JavaDocOption {

    /**
     * The option {@code -nopackagediagram} indicates that package diagrams should
     * not be generated in the Overview Summary.
     */
    NO_PACKAGE_DIAGRAM("-nopackagediagram", 0, "Do not generate the package diagram in the overview summary"),

    /**
     * The option {@code -sourceclasspath (commaSeparatedPathlist)} indicates where source
     * class files should be found.
     */
    SOURCE_CLASSPATH("-sourceclasspath", 1, "<commaSeparatedPathlist>   Specify where to find source class files"),

    /**
     * The option {@code -category (category)} specifies colorization for a given category.
     */
    CATEGORY("-category", 1, "<category>[:<fillcolor>[:<linecolor>]] Defines colors for items marked "
            + "with @apiviz.category"),

    /**
     * The option {@code -help}
     */
    HELP("-help", 0, null);

    // Internal state
    private String option;
    private int numExpectedArguments;
    private String helpText;

    JavaDocOption(final String option,
                  final int numExpectedArguments,
                  final String helpText) {

        this.option = option;
        this.numExpectedArguments = numExpectedArguments;
        this.helpText = helpText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return option;
    }

    /**
     * Retrieves the JavaDoc option, including the flag.
     *
     * @return the JavaDoc option (string) of this {@link JavaDocOption}, including the "-".
     */
    public String getOption() {
        return option;
    }

    /**
     * Retrieves a help text for this {@link JavaDocOption}
     *
     * @return a help text for this {@link JavaDocOption}
     */
    public String getHelpText() {
        return helpText;
    }

    /**
     * Retrieves the number of arguments expected to this {@link JavaDocOption}, excluding the option itself.
     *
     * @return the number of arguments expected for this Option.
     */
    public int getNumExpectedArguments() {
        return numExpectedArguments;
    }

    /**
     * Retrieves the Doclet optionLength for this {@link JavaDocOption}. In Doclet-speak, the optionLength is the
     * number of arguments expected to this JavaDocOption plus 1 (the option itself).
     *
     * @return The DocletLength for this {@link JavaDocOption}.
     */
    public int getOptionLength() {
        return 1 + numExpectedArguments;
    }

    /**
     * Converts the supplied option to the appropriate {@link JavaDocOption} instance.
     *
     * @param optionArray A JavaDoc option array.
     * @return The {@link JavaDocOption} having the supplied option (string).
     */
    public static JavaDocOption parseJavaDocOptionArray(final String... optionArray) {

        // Fail fast
        if (optionArray == null || optionArray.length == 0) {
            return null;
        }

        final String option = optionArray[0];
        final String[] argumentArray = optionArguments(optionArray);
        final int numArguments = argumentArray == null ? 0 : argumentArray.length;

        for (JavaDocOption current : values()) {
            if (current.getOption().equals(option) && current.getNumExpectedArguments() == numArguments) {
                return current;
            }
        }

        // Nopes.
        return null;
    }

    /**
     * Splices away the array of arguments from the supplied optionArray.
     *
     * @param optionArray An array of options as received by the JavaDoc engine.
     * @return null if the optionArray was null or empty, and otherwise the optionArray minus its first element.
     */
    public static String[] optionArguments(final String... optionArray) {

        // Fail fast
        if (optionArray == null || optionArray.length == 0) {
            return null;
        }

        final String[] arguments = optionArray.length > 1 ? new String[optionArray.length - 1] : new String[0];
        if (arguments.length > 0) {
            System.arraycopy(optionArray, 1, arguments, 0, arguments.length);
        }

        return arguments;
    }
}
