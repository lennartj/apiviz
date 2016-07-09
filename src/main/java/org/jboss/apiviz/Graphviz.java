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

import com.sun.javadoc.RootDoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Utility class to wrap Graphviz operations, by launching and handling the "dot" executable.
 *
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
public final class Graphviz {

    /**
     * Regular expression to identify a significant text line.
     */
    public static final String GRAPHVIZ_EXECUTABLE_FIRST_LINE_CHECK = "^.*[Gg][Rr][Aa][Pp][Hh][Vv][Ii][Zz].*$";

    /**
     * The System property key defining the graphviz home.
     */
    public static final String HOMEDIR_SYSTEM_PROPERTY = "graphviz.home";

    /**
     * The environment key defining the graphviz home.
     */
    public static final String HOMEDIR_ENV_PROPERTY = "GRAPHVIZ_HOME";

    /**
     * Charset used by Graphviz to write the resulting files.
     */
    public static final String DOT_STANDARD_CHARSET = "UTF-8";

    private static boolean homeDetermined;
    private static File home;

    /*
     * Hide constructor for utility classes.
     */
    private Graphviz() {
        // Unused
    }

    /**
     * Checks if the Graphviz is available for processing.
     *
     * @param root the RootDoc received from JavaDoc.
     * @return true if the GraphViz installation was found.
     */
    public static boolean isAvailable(final RootDoc root) {

        // #1) Find dot(.exe) and the Graphviz installation directory
        String executable = Graphviz.getExecutable(root);
        File home = Graphviz.getHome(root);

        // #2) Fire dot and determine its version.
        ProcessBuilder pb = new ProcessBuilder(executable, "-V");
        pb.redirectErrorStream(true);
        if (home != null) {
            root.printNotice("Graphviz Home: " + home);
            pb.directory(home);
        }
        root.printNotice("Graphviz Executable: " + executable);

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            root.printWarning(e.getMessage());
            return false;
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        OutputStream out = p.getOutputStream();
        try {
            out.close();

            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.matches(GRAPHVIZ_EXECUTABLE_FIRST_LINE_CHECK)) {
                    root.printNotice("Graphviz Version: " + line);
                    return true;
                } else {
                    root.printWarning("Unknown Graphviz output: " + line);
                }
            }
            return false;
        } catch (IOException e) {
            root.printWarning("Problem detecting Graphviz: " + e.getMessage());
            return false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            try {
                in.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            for (; ; ) {
                try {
                    p.waitFor();
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * <p>Creates and writes PNG and HTML imagemap files by executing 'dot', with the following arguments:</p>
     * <pre>
     *     <code>
     *         dot -Tcmapx -o [outputDir/filename].map -Tpng -o [outputDir/filename].png
     *     </code>
     * </pre>
     * <p>The {@code diagram} string is fed to the dot process.</p>
     *
     * @param root            The active {@link RootDoc} instance.
     * @param diagram         The diagram (i.e. digraph) data to feed into the dot program. Source of the graphs.
     * @param outputDirectory The directory where the result should be sent.
     * @param filename        The filename of the PNG and MAP files generated.
     * @throws IOException If the files could not be properly generated.
     */
    public static void writeImageAndMap(final RootDoc root,
            final String diagram,
            final File outputDirectory,
            final String filename) throws IOException {

        // TODO: Check inbound arguments for sanity? (nulls, existence etc)

        // #1) Ensure that the PNG and MAP files can be written to.
        final File pngFile = new File(outputDirectory, filename + ".png");
        final File mapFile = new File(outputDirectory, filename + ".map");
        pngFile.delete();
        mapFile.delete();

        // #2) Compile the arguments used to launch Graphviz.
        final String dot = Graphviz.getExecutable(root);
        final ProcessBuilder pb = new ProcessBuilder(
                dot,
                "-Tcmapx", "-o", mapFile.getAbsolutePath(),
                "-Tpng", "-o", pngFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        File home = Graphviz.getHome(root);
        if (home != null) {
            pb.directory(home);
        }

        // #3) Launch Graphviz. Harvest output.
        final Process p = pb.start();
        final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final Writer out = new OutputStreamWriter(p.getOutputStream(), DOT_STANDARD_CHARSET);
        try {
            out.write(diagram);
            out.close();

            String line = null;
            while ((line = in.readLine()) != null) {
                System.err.println(line);
            }
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            try {
                in.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            for (; ; ) {
                try {
                    int result = p.waitFor();
                    if (result != 0) {
                        throw new IllegalStateException("Graphviz exited with a non-zero return value: " + result);
                    }
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    //
    // Private helpers
    //

    private static String getExecutable(final RootDoc root) {

        String command = "dot";

        try {
            String osName = System.getProperty("os.name");
            if (osName != null && osName.indexOf("Windows") >= 0) {

                // The Windows executable is called 'dot.exe' instead of 'dot'
                File path = Graphviz.getHome(root);
                command = path != null ? path.getAbsolutePath() + File.separator + "dot.exe" : "dot.exe";
            }
        } catch (Exception e) {
            // ignore me!
        }
        return command;
    }

    private static File getHome(final RootDoc root) {

        // Already done?
        if (homeDetermined) {
            return home;
        }

        File toReturn = null;

        try {

            // #1) Attempt to find the Graphviz directory from a System property
            String graphvizHome = System.getProperty(HOMEDIR_SYSTEM_PROPERTY);
            if (graphvizHome != null) {
                root.printNotice("Graphviz found using the '" + HOMEDIR_SYSTEM_PROPERTY
                        + "' system property: " + graphvizHome);
            } else {
                root.printNotice("The '" + HOMEDIR_SYSTEM_PROPERTY + "' system property was not specified.");

                // #2) Attempt to find the Graphviz directory from an environment property
                graphvizHome = System.getenv(HOMEDIR_ENV_PROPERTY);
                if (graphvizHome != null) {
                    root.printNotice("Graphviz found using the '" + HOMEDIR_ENV_PROPERTY + "' environment variable: "
                            + graphvizHome);
                } else {
                    root.printNotice("The '" + HOMEDIR_ENV_PROPERTY + "' environment variable was not specified.");
                }
            }

            // Check sanity
            if (graphvizHome != null) {
                toReturn = new File(graphvizHome);
                if (!toReturn.exists() || !toReturn.isDirectory()) {
                    root.printWarning("The specified graphviz home directory does not exist: "
                            + toReturn.getPath());
                    toReturn = null;
                }
            }

            if (toReturn == null) {
                root.printNotice("System path will be used as graphviz home directory was not specified.");
            }
        } catch (Exception e) {
            // ignore...
        }

        // Cache the Graphviz home directory.
        homeDetermined = true;
        home = toReturn;

        // All Done.
        return toReturn;
    }
}
