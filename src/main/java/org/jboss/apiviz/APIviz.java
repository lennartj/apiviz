/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.apiviz;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import jdepend.framework.JDepend;
import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;
import se.jguru.javadoc.apiviz.APIvizWrappedRootDoc;
import se.jguru.javadoc.apiviz.JavaDocOption;
import se.jguru.javadoc.apiviz.doclet.APIvizDoclet;
import se.jguru.javadoc.apiviz.model.DocletModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static se.jguru.javadoc.apiviz.model.DocletModel.CLASSPATH_ARGUMENT;


/**
 * API visualization entry point.
 *
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
public class APIviz {

    /**
     * The newline character, as read from the System.property containing it.
     */
    public static final String NEWLINE = System.getProperty("line.separator", "\n");

    private static final Pattern INSERTION_POINT_PATTERN = Pattern.compile(
            "((<\\/PRE>)(?=\\s*(<P>|<div[^>]*block))|(?=<TABLE BORDER=\"1\")|(<div[^>]*contentContainer[^>]*>))",
            Pattern.CASE_INSENSITIVE);

    public static boolean start(RootDoc root) {
        root = new APIvizWrappedRootDoc(root);
        if (!Standard.start(root)) {
            return false;
        }

        if (!Graphviz.isAvailable(root)) {
            root.printWarning("Graphviz was not found on the system path. (Not installed or incorrect graphviz.home)");
            root.printWarning("Please install graphviz and specify -Dgraphviz.home ");
            root.printWarning("Skipping diagram generation.");
            return true;
        }

        // Create a DocletModel
        final DocletModel docletModel = new DocletModel(root.options(), root);

        try {
            final File outputDirectory = docletModel.getOutputDirectory();

            ClassDocGraph graph = new ClassDocGraph(root, docletModel);
            if (docletModel.generatePackageDiagram()) {
                generateOverviewSummary(root, graph, docletModel);
            }
            generatePackageSummaries(root, graph, outputDirectory);
            generateClassDiagrams(root, graph, outputDirectory);

        } catch (Throwable t) {
            root.printError("An error occurred during diagram generation: " + t.toString());
            t.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean validOptions(final String[][] options, final DocErrorReporter errorReporter) {

        // #1) Check sanity
        if (options == null) {
            errorReporter.printWarning("Received null options to validate. Skipping validation.");
            return false;
        }

        // #2) Validate JavaDoc options we recognize.
        for (String[] current : options) {

            final JavaDocOption javaDocOption = JavaDocOption.parseJavaDocOptionArray(current);

            if (javaDocOption != null && javaDocOption.getNumExpectedArguments() > 0) {
                switch (javaDocOption) {
                    case SOURCE_CLASSPATH:

                        // We need to validate the classpath, which consists of
                        // 1: The argument given to this JavaDocOption
                        // 2: The classpath argument given to the Doclet
                        final List<File> classPath = new ArrayList<>();

                        // 1: Find the classpath snippets from the argument to this JavaDocOption
                        for (String currentSourceClasspath : JavaDocOption.optionArguments(current)) {
                            classPath.addAll(DocletModel.splitAndConvert(currentSourceClasspath));
                        }

                        // 2: Find the classpath snippets from the '-classpath' argument
                        for (String[] inner : options) {
                            if (CLASSPATH_ARGUMENT.equals(inner[0]) && inner.length >= 2) {
                                classPath.addAll(DocletModel.splitAndConvert(inner[1]));
                            }
                        }

                        // Check sanity
                        if (classPath.isEmpty()) {
                            errorReporter.printError(JavaDocOption.SOURCE_CLASSPATH.getOption()
                                    + " requires at least one valid class path. ("
                                    + JavaDocOption.SOURCE_CLASSPATH.getHelpText() + ")");
                            return false;
                        }
                        for (File classPathSnippet : classPath) {

                            final boolean isReadableFile = classPathSnippet.isFile() && classPathSnippet.canRead();
                            if (!isReadableFile) {
                                errorReporter.printError("ClassPath snippet [" + classPathSnippet.getAbsolutePath()
                                        + "] was not a readable file.");
                                return false;
                            }
                        }

                        break;
                }
            }
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

    public static int optionLength(final String option) {

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

    public static LanguageVersion languageVersion() {
        return Standard.languageVersion();
    }

    public static void generateOverviewSummary(final RootDoc root,
            final ClassDocGraph graph,
            final DocletModel model) throws IOException {
        final Map<String, PackageDoc> packages = getPackages(root);

        PackageFilter packageFilter = PackageFilter.all();

        for (Map.Entry<String, PackageDoc> entry : packages.entrySet()) {
            String packageName = entry.getKey();
            PackageDoc p = entry.getValue();

            if (!ClassDocGraph.isHidden(p)) {
                packageFilter.including(packageName);
            }
        }

        JDepend jdepend = new JDepend(packageFilter.excludingRest());

        final List<File> classPath = model.getClassPath();
        for (File e : classPath) {
            if (e.isDirectory()) {
                root.printNotice("Included into dependency analysis: " + e);
                jdepend.addDirectory(e.toString());
            } else {
                root.printNotice("Excluded from dependency analysis: " + e);
            }
        }

        jdepend.analyze();

        if (checkClasspathOption(root, jdepend)) {
            instrumentDiagram(
                    root, model.getOutputDirectory(), "overview-summary",
                    graph.getOverviewSummaryDiagram(jdepend));
        } else {
            root.printWarning("Please make sure that the '"
                    + JavaDocOption.SOURCE_CLASSPATH.getOption() + "' option was specified correctly.");
            root.printWarning("Package dependency diagram will not be generated to avoid the inaccurate result.");
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean checkClasspathOption(final RootDoc root, final JDepend jdepend) {

        // Sanity check
        boolean correctClasspath = true;
        if (jdepend.countClasses() == 0) {
            root.printWarning("JDepend was not able to locate any compiled class files.");
            correctClasspath = false;
        } else {
            for (ClassDoc c : root.classes()) {
                if (c.containingPackage() == null
                        || c.containingPackage().name() == null
                        || ClassDocGraph.isHidden(c.containingPackage())) {
                    continue;
                }

                boolean found = false;
                String fqcn = c.containingPackage().name() + '.' + c.name().replace('.', '$');
                JavaPackage jpkg = jdepend.getPackage(c.containingPackage().name());
                if (jpkg != null) {
                    Collection<JavaClass> jclasses = jpkg.getClasses();
                    if (jclasses != null) {
                        for (JavaClass jcls : jclasses) {
                            if (fqcn.equals(jcls.getName())) {
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if (!found) {
                    root.printWarning("JDepend was not able to locate some compiled class files: " + fqcn);
                    correctClasspath = false;
                    break;
                }
            }
        }
        return correctClasspath;
    }

    public static void generatePackageSummaries(final RootDoc root,
            final ClassDocGraph graph,
            final File outputDirectory) throws IOException {

        for (PackageDoc p : getPackages(root).values()) {
            instrumentDiagram(root,
                    outputDirectory,
                    p.name().replace('.', File.separatorChar) + File.separatorChar + "package-summary",
                    graph.getPackageSummaryDiagram(p));
        }
    }

    public static void generateClassDiagrams(final RootDoc root,
            final ClassDocGraph graph,
            final File outputDirectory) throws IOException {

        for (ClassDoc c : root.classes()) {
            if (c.containingPackage() == null) {
                instrumentDiagram(
                        root,
                        outputDirectory,
                        c.name(),
                        graph.getClassDiagram(c));
            } else {
                instrumentDiagram(
                        root,
                        outputDirectory,
                        c.containingPackage().name().replace('.', File.separatorChar) + File.separatorChar + c.name(),
                        graph.getClassDiagram(c));
            }
        }
    }

    static Map<String, PackageDoc> getPackages(final RootDoc root) {

        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>();
        for (ClassDoc c : root.classes()) {
            PackageDoc p = c.containingPackage();
            if (!packages.containsKey(p.name())) {
                packages.put(p.name(), p);
            }
        }

        return packages;
    }

    private static void instrumentDiagram(final RootDoc root,
            final File outputDirectory,
            String filename,
            final String diagram) throws IOException {

        // TODO - it would be nice to have a debug flag that would spit out the graphviz source as well
        //System.out.println(diagram);

        boolean needsBottomMargin = filename.contains("overview-summary") || filename.contains("package-summary");

        File htmlFile = new File(outputDirectory, filename + ".html");
        File pngFile = new File(outputDirectory, filename + ".png");
        File mapFile = new File(outputDirectory, filename + ".map");

        if (!htmlFile.exists()) {
            // Shouldn't reach here anymore.
            // I'm retaining the code just in case.
            for (; ; ) {
                int idx = filename.lastIndexOf(File.separatorChar);
                if (idx > 0) {
                    filename = filename.substring(0, idx) + '.' + filename.substring(idx + 1);
                } else {
                    // Give up (maybe missing)
                    return;
                }
                htmlFile = new File(outputDirectory, filename + ".html");
                if (htmlFile.exists()) {
                    pngFile = new File(outputDirectory, filename + ".png");
                    mapFile = new File(outputDirectory, filename + ".map");
                    break;
                }
            }
        }

        root.printNotice("Generating " + pngFile + "...");
        Graphviz.writeImageAndMap(root, diagram, outputDirectory, filename);

        try {
            String oldContent = FileUtil.readFile(htmlFile);
            String mapContent = FileUtil.readFile(mapFile);

            Matcher matcher = INSERTION_POINT_PATTERN.matcher(oldContent);
            if (!matcher.find()) {
                throw new IllegalStateException(
                        "Failed to find an insertion point: " + htmlFile);
            }
            String style = "text-align: center;";
            if (needsBottomMargin) {
                style += "margin-bottom: 1em;";
            }
            String newContent = oldContent.substring(0, matcher.end()) + NEWLINE + mapContent
                    + "<div id=\"apivizContainer\" style=\"" + style + "\">"
                    + "<img src=\"" + pngFile.getName() + "\" usemap=\"#APIVIZ\" border=\"0\"></div>"
                    + oldContent.substring(matcher.end());
            FileUtil.writeFile(htmlFile, newContent);

        } finally {
            mapFile.delete();
        }
    }
}
