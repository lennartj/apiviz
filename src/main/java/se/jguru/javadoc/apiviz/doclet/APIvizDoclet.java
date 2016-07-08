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
import com.sun.tools.doclets.standard.Standard;
import org.jboss.apiviz.APIviz;
import org.jboss.apiviz.ClassDocGraph;
import org.jboss.apiviz.Graphviz;
import se.jguru.javadoc.apiviz.APIvizWrappedRootDoc;
import se.jguru.javadoc.apiviz.JavaDocOption;
import se.jguru.javadoc.apiviz.model.DocletModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Doclet-compliant class which provides methods required by the Doclet specification.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class APIvizDoclet implements SimpleDoclet {

    private static DocletModel model;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean start(final RootDoc root) {

        // #1) Wrap the inbound RootDoc, and toss it to the standard Doclet.
        final APIvizWrappedRootDoc rootDoc = new APIvizWrappedRootDoc(root);
        if (!Standard.start(rootDoc)) {
            return false;
        }

        // #2) Check sanity
        if (!Graphviz.isAvailable(rootDoc)) {
            rootDoc.printWarning("Graphviz was not found on the system path. "
                    + "(Not installed or incorrect graphviz.home)");
            rootDoc.printWarning("Please install graphviz and specify -Dgraphviz.home ");
            rootDoc.printWarning("Skipping diagram generation.");

            // Continue with normal JavaDoc generation.
            return true;
        }

        // #3) Create a DocletModel
        final DocletModel docletModel = new DocletModel(rootDoc.options());

        // #4) Use Graphviz to generate JavaDoc diagrams.
        try {
            final File outputDirectory = docletModel.getOutputDirectory();
            final ClassDocGraph graph = new ClassDocGraph(rootDoc);

            if (docletModel.generatePackageDiagram()) {
                APIviz.generateOverviewSummary(root, graph, outputDirectory);
            }

            APIviz.generatePackageSummaries(root, graph, outputDirectory);
            APIviz.generateClassDiagrams(root, graph, outputDirectory);
        } catch (Throwable t) {
            root.printError("An error occurred during diagram generation: " + t.toString());
            t.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateJavaDocOptions(final String[][] options, final DocErrorReporter errorReporter) {

        // #1) Check sanity
        if (options == null) {
            errorReporter.printWarning("Received null options to validate. Skipping validation.");
            return false;
        }

        // #2) Create a DocletModel.
        if(model == null) {
            model = new DocletModel(options, errorReporter);
        }

        // #2) Validate JavaDoc options we recognize.
        for (String[] current : options) {
            JavaDocOption.parseJavaDocOptionArray(current);
        }

        // #3) Create a String[][] holding the options we do not recognize.
        final List<String[]> unknownOptions = new ArrayList<>();
        outer:
        for (String[] current : options) {

            final String currentArgument = current[0];

            // Ignore the known options... but let -help pour through.
            for (JavaDocOption currentOption : JavaDocOption.values()) {
                if (currentOption == JavaDocOption.HELP || currentOption.getOption().equals(currentArgument)) {
                    continue outer;
                }
            }

            // This is not a known JavaDoc option.
            unknownOptions.add(current);
        }

        // Delegate to the standard Doclet.
        return Standard.validOptions(
                unknownOptions.toArray(new String[unknownOptions.size()][]),
                errorReporter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int optionLength(final String option) {

        // #1) Handle the -help option.
        if (JavaDocOption.HELP.getOption().equals(option)) {

            // First, provide the help text from the Standard Doclet.
            final int toReturn = Standard.optionLength(option);

            // Print the options provided by APIviz.
            System.out.println();
            System.out.println("Provided by " + APIvizDoclet.class.getName() + ":");
            for (JavaDocOption current : JavaDocOption.values()) {
                if (current != JavaDocOption.HELP) {
                    System.out.println(current.getOption() + " " + current.getHelpText());
                }
            }

            // Delegate to the standard Doclet implementation.
            return toReturn;
        }

        // #2) Handle all JavaDoc options known to this Doclet
        for (JavaDocOption current : JavaDocOption.values()) {
            if (current.getOption().equals(option)) {
                return current.getOptionLength();
            }
        }

        // #3) Delegate to the standard Doclet.
        return Standard.optionLength(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }
}
