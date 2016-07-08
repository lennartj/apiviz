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
package se.jguru.javadoc.apiviz.doclet;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

/**
 * Specification of a SimpleDoclet, implemented by classes which can be used as JavaDoc Doclet implementations.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public interface SimpleDoclet {

    /**
     * A constant containing the value returned by a {@link SimpleDoclet} implementation when an option is unknown.
     */
    int UNKNOWN_OPTION = 0;

    /**
     * Doclet start of execution.
     *
     * @param root A non-null {@link RootDoc} that represents the root of the program structure information
     *             for one run of javadoc. Enables extracting all other program structure information data.
     * @return true if the start completed OK.
     */
    boolean start(final RootDoc root);

    /**
     * <p>Validates the supplied javadoc options against this SimpleDoclet.
     * Each array contains the option as the first element in the array and its arguments as any following elements.
     * For example, given the command:</p>
     * <p>
     * <pre><code>javadoc -foo this that -bar other ...</code></pre>
     * <p>the RootDoc.options method will return</p>
     *
     * <pre>
     *     <code>
     *     options()[0][0] = "-foo"
     *     options()[0][1] = "this"
     *     options()[0][2] = "that"
     *     options()[1][0] = "-bar"
     *     options()[1][1] = "other"
     *     </code>
     * </pre>
     * <p>
     *
     * @param options the spliced JavaDoc options provided to the javadoc command.
     * @param errorReporter The JavaDoc error reporter
     * @return {@code true} if the options were valid, and {@code false} otherwise.
     */
    boolean validateJavaDocOptions(final String[][] options, final DocErrorReporter errorReporter);

    /**
     * <p>Any doclet that uses custom options must have a method called optionLength(String option) that returns an
     * int. For each custom option that you want your doclet to recognize, optionLength must return the number of
     * separate pieces or tokens in the option.</p>
     * <p>For example, the custom option {@code -foo bar} has 2 pieces, the -tag option itself and its value.
     * Hence, this optionLength method must return 2 for the {@code -foo} option.</p>
     *
     * @param option A javadoc option.
     * @return The length of the supplied option, including the option itself, or {@link #UNKNOWN_OPTION} for
     * unrecognized options.
     * @see DelegatingDoclet#optionLength(String)
     */
    int optionLength(String option);

    /**
     * Retrieves the LanguageVersion of this {@link SimpleDoclet} instance.
     * Should normally be {@link LanguageVersion#JAVA_1_5}.
     *
     * @return the LanguageVersion of this {@link SimpleDoclet} instance.
     */
    LanguageVersion languageVersion();
}
