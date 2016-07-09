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
 * Enumeration containing definitions of APIViz custom JavaDoc Tags.
 * All tags have the prefix {@code apiviz}, to remain backwards compatible with the original
 * code from the JBoss project. Therefore, the tags typically have the form {@code apiviz.uses}.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public enum JavaDocTag {

    /**
     * apiviz.stereotype (name)
     */
    STEREOTYPE("<name>"),

    /**
     * apiviz.uses (FQCN) [(sourceLabel) (targetLabel) [(edgeLabel)]]
     */
    USES("<FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]]"),

    /**
     * apiviz.has (FQCN) [oneway] [(sourceLabel) (targetLabel) [(edgeLabel)]]
     * <p>
     * <p><strong>TODO</strong> change relationship spec to edgelabel sourcelabel targetlabel<br/>
     * Split apiviz.has into two tags</p>
     */
    HAS("<FQCN> [oneway] [<sourceLabel> <targetLabel> [<edgeLabel>]]"),

    /**
     * apiviz.owns (FQCN) [(sourceLabel) (targetLabel) [(edgeLabel)]]
     */
    OWNS("<FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]]"),

    /**
     * apiviz.composedOf (FQCN) [(sourceLabel) (targetLabel) [(edgeLabel)]]
     */
    COMPOSED_OF("composedOf", "<FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]]"),

    /**
     * apiviz.landmark
     */
    LANDMARK(""),

    /**
     * apiviz.hidden
     */
    HIDDEN(""),

    /**
     * apiviz.exclude (regex)
     */
    EXCLUDE("<regex>"),

    /**
     * apiviz.excludeSubtypes
     */
    EXCLUDE_SUBTYPES("excludeSubtypes", ""),

    /**
     * apiviz.inherit
     */
    INHERIT(""),

    /**
     * apiviz.category (categoryname)
     */
    CATEGORY("<categoryName>");

    // Internal state
    private String nonstandardTagSuffix;
    private String arguments;

    JavaDocTag(final String arguments) {
        this(null, arguments);
    }

    JavaDocTag(final String nonstandardTagSuffix, final String arguments) {
        this.nonstandardTagSuffix = nonstandardTagSuffix;
        this.arguments = arguments;
    }

    /**
     * The prefix of all APIVIZ javadoc tags.
     */
    public static final String TAG_PREFIX = "@apiviz.";

    /**
     * Retrieves the full javadoc tag name, including prefix.
     *
     * @return The full ApiViz JavaDoc tag name.
     */
    @Override
    public String toString() {
        return TAG_PREFIX + (nonstandardTagSuffix == null ? name().toLowerCase() : nonstandardTagSuffix);
    }

    /**
     * Retrieves a (brief) usage description of this {@link JavaDocTag}.
     *
     * @return a (brief) usage description of this {@link JavaDocTag}.
     */
    public String getTagUsage() {
        return toString() + " " + arguments;
    }
}
