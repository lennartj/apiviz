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
package se.jguru.javadoc.apiviz.model;

import com.sun.javadoc.DocErrorReporter;
import se.jguru.javadoc.apiviz.JavaDocOption;
import se.jguru.javadoc.apiviz.doclet.APIvizDoclet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple POJO model for the operation of the APIvizDoclet.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class DocletModel {

    /**
     * The argument for finding a classpath option to this {@link APIvizDoclet}.
     */
    public static final String CLASSPATH_ARGUMENT = "-classpath";

    // Internal state
    private List<ConfigOption> configOptions;

    // Model properties
    private File outputDirectory;
    private Boolean generatePackageDiagram;
    private List<File> classPath;
    private List<Category> categories;

    /**
     * Converts the supplied JavaDoc options array into a {@link DocletModel}.
     *
     * @param options       the given JavaDoc options.
     * @param errorReporter A non-null {@link DocErrorReporter}
     */
    public DocletModel(final String[][] options, final DocErrorReporter errorReporter) {

        // Assign internal state
        this.configOptions = ConfigOption.parse(options);

        // Assign and validate configuration options
        //
        // #1) Configuration: OutputDirectory
        final ConfigOption configOutputDir = ConfigOption.getFirst("-d", this.configOptions);
        if (configOutputDir != null && configOutputDir.getArguments().size() > 0) {
            outputDirectory = new File(configOutputDir.getArguments().get(0));
        } else {
            outputDirectory = new File(System.getProperty("user.dir", "."));
        }

        //
        // #2) Configuration: Generate Package Diagram
        this.generatePackageDiagram = true;
        final ConfigOption configPackageDiagram = ConfigOption.getFirst(
                JavaDocOption.NO_PACKAGE_DIAGRAM.getOption(), this.configOptions);
        if (configPackageDiagram != null && configPackageDiagram.getArguments().size() > 0) {

            final String configValue = configPackageDiagram.getArguments().get(0);
            try {
                this.generatePackageDiagram = Boolean.parseBoolean(configValue);
            } catch (Exception e) {
                errorReporter.printWarning("Configuration option " + JavaDocOption.NO_PACKAGE_DIAGRAM.getOption()
                        + " should be a Boolean value. (Got: " + configValue + "). Reverting to "
                        + this.generatePackageDiagram);
            }
        }

        //
        // #3) Configuration: ClassPath
        final List<File> tmpClassPath = new ArrayList<>();
        final ConfigOption configSourceClassPath = ConfigOption.getFirst(
                JavaDocOption.SOURCE_CLASSPATH.getOption(), this.configOptions);
        if (configSourceClassPath != null && configSourceClassPath.getArguments().size() > 0) {

            for (String currentSnippet : configSourceClassPath.getArguments()) {
                tmpClassPath.addAll(splitAndConvert(currentSnippet));
            }
        }

        final ConfigOption configStdClasspath = ConfigOption.getFirst(CLASSPATH_ARGUMENT, this.configOptions);
        if (configStdClasspath != null && configStdClasspath.getArguments().size() > 0) {

            for (String currentSnippet : configStdClasspath.getArguments()) {
                tmpClassPath.addAll(splitAndConvert(currentSnippet));
            }
        }
        // Check sanity
        if (tmpClassPath.isEmpty()) {
            errorReporter.printError(JavaDocOption.SOURCE_CLASSPATH.getOption()
                    + " requires at least one valid class path. ("
                    + JavaDocOption.SOURCE_CLASSPATH.getHelpText() + ")");
        }
        for (File classPathSnippet : tmpClassPath) {

            final boolean isReadableFile = classPathSnippet.isFile() && classPathSnippet.canRead();
            if (!isReadableFile) {
                errorReporter.printError("ClassPath snippet [" + classPathSnippet.getAbsolutePath()
                        + "] was not a readable file.");
            }
        }
        this.classPath = tmpClassPath;

        //
        // #4) Configuration: Categories
        this.categories = Category.parseCategories(this.configOptions);

        //
        // #5) Configuration: standard JavaDoc options
        for (String[] current : options) {
            JavaDocOption.parseJavaDocOptionArray(current);
        }
    }

    /**
     * Retrieves a List of Files which contitute the classPath given to this {@link DocletModel}
     * as part of the JavaDoc options.
     *
     * @return A List of Files representing the ClassPath entries.
     * @throws IllegalArgumentException if the options used to initialize this {@link DocletModel} were not correct
     *                                  with respect to the {@link #CLASSPATH_ARGUMENT} or
     *                                  {@link JavaDocOption#SOURCE_CLASSPATH}.
     */
    public List<File> getClassPath() throws IllegalArgumentException {
        return classPath;
    }

    /**
     * If {@code true}, the doclet is instructed to generate Package diagrams.
     *
     * @return {@code true} to make the Doclet generate Package diagrams.
     */

    public boolean generatePackageDiagram() {
        return generatePackageDiagram;
    }

    /**
     * Retrieves the directory where APIviz emits its files.
     *
     * @return the directory where APIviz emits its files.
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Retrieves all configured {@link Category} objects.
     *
     * @return all {@link Category} objects created from the JavaDoc configuration.
     */
    public List<Category> getCategories() {
        return categories;
    }

    /**
     * Splits the supplied classPathSnippet using the {@link File#pathSeparator} character, and returns
     * the results in the form of a File[].
     *
     * @param classPathSnippet A string containing concatenated classpath elements.
     * @return An array holding the corresponding Files.
     */
    public static List<File> splitAndConvert(final String classPathSnippet) {

        final List<File> toReturn = new ArrayList<>();

        // Check sanity
        if (classPathSnippet == null || classPathSnippet.isEmpty()) {
            return toReturn;
        }

        // Split and convert to Files
        final String[] split = classPathSnippet.split(File.pathSeparator);
        for (final String aSplit : split) {
            toReturn.add(new File(aSplit));
        }

        // All Done.
        return toReturn;
    }
}
