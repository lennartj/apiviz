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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Trivial holder for a single JavaDoc configure option, as defined by the JavaDoc framework.
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
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class ConfigOption implements Comparable<ConfigOption> {

    // Internal state
    private String option;
    private List<String> arguments;

    /**
     * Parses a non-null/non-empty javadoc option array into a ConfigOption instance.
     *
     * @param javadocOption a non-null/non-empty javadoc option array.
     */
    public ConfigOption(final String[] javadocOption) {

        // Check sanity
        if (javadocOption == null) {
            throw new IllegalArgumentException("Cannot handle null 'javadocOption' argument.");
        }
        if (javadocOption.length == 0) {
            throw new IllegalArgumentException("Cannot handle empty 'javadocOption' argument.");
        }

        // Assign internal state
        final List<String> converted = Arrays.asList(javadocOption);
        this.option = converted.get(0);
        this.arguments = new ArrayList<>();
        if (javadocOption.length > 1) {
            arguments.addAll(converted.subList(1, converted.size()));
        }
    }

    /**
     * Retrieves the option provided, such as {@code -foo} or {@code -bar} in the javadoc execution line
     * <pre><code>javadoc -foo this that -bar other ...</code></pre>
     *
     * @return the option wrapped by this {@link ConfigOption} instance.
     */
    public String getOption() {
        return option;
    }

    /**
     * Retrieves the arguments supplied to the option represented by this {@link ConfigOption} instance, such as
     * {@code "this", "that"} for the option {@code -foo} in the javadoc execution line
     * <pre><code>javadoc -foo this that -bar other ...</code></pre>
     *
     * @return A List containing all arguments supplied for this {@link ConfigOption}. The List may be empty but
     * never {@code null}.
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final ConfigOption that) {

        // Fail fast
        if (that == null) {
            return -1;
        } else if (that == this) {
            return 0;
        }

        // Delegate to internal state
        int toReturn = this.option.compareTo(that.option);
        if (toReturn == 0) {
            toReturn = this.arguments.size() - that.arguments.size();
        }
        if (toReturn == 0) {
            for (int i = 0; i < arguments.size(); i++) {
                toReturn = this.arguments.get(i).compareTo(that.arguments.get(i));
            }
        }
        return toReturn;
    }

    /**
     * Factory method converting the supplied JavaDoc option array into a List of {@link ConfigOption} instances.
     *
     * @param javaDocOptions The JavaDoc-supplied options.
     * @return A non-null List containing {@link ConfigOption} instances.
     */
    public static List<ConfigOption> parse(final String[][] javaDocOptions) {

        final List<ConfigOption> toReturn = new ArrayList<>();

        if (javaDocOptions != null) {
            for (String[] currentOption : javaDocOptions) {
                toReturn.add(new ConfigOption(currentOption));
            }
        }

        // All Done.
        return toReturn;
    }

    /**
     * Retrieves the first {@link ConfigOption} in the List matching the given name.
     *
     * @param name          The {@link ConfigOption} name. Cannot be null.
     * @param configOptions A non-null List of {@link ConfigOption}s.
     * @return The first matching {@link ConfigOption}.
     */
    public static ConfigOption getFirst(final String name, final List<ConfigOption> configOptions) {

        // Check sanity
        if(name == null) {
            throw new NullPointerException("Cannot handle null 'name' argument.");
        }
        if(configOptions == null) {
            throw new NullPointerException("Cannot handle null 'configOptions' argument.");
        }

        for (ConfigOption current : configOptions) {
            if (name.equals(current.getOption())) {
                return current;
            }
        }

        // All Done.
        return null;
    }
}
