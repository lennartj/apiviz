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

import org.jboss.apiviz.Color;
import se.jguru.javadoc.apiviz.JavaDocOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Definition of a Category model, used to store definitions for fill and line Colors.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 * @see Color
 */
public class Category implements Serializable, Comparable<Category> {

    /**
     * Default/fallback fill color.
     */
    public static final String DEFAULT_FILL_COLOR = "#FFFFFF";

    /**
     * Default/fallback line color.
     */
    public static final String DEFAULT_LINE_COLOR = "#000000";

    // Internal state
    private String name;
    private Color fillColor;
    private Color lineColor;

    /**
     * Compound constructor creating a Category wrapping the supplied data.
     *
     * @param name      The category name
     * @param fillColor The fill {@link Color}
     * @param lineColor The line {@link Color}
     */
    public Category(final String name, final Color fillColor, final Color lineColor) {

        // Check sanity
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Cannot handle null or empty 'name' argument.");
        }

        // Assign internal state
        this.name = name;
        this.fillColor = fillColor == null ? Color.valueOf(DEFAULT_FILL_COLOR.toLowerCase()) : fillColor;
        this.lineColor = lineColor == null ? Color.valueOf(DEFAULT_LINE_COLOR.toLowerCase()) : lineColor;
    }

    /**
     * Retrieves the name of this Category.
     *
     * @return the name of this Category.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the fill Color of this Category.
     *
     * @return the fill Color of this Category.
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Retrieves the line Color of this Category.
     *
     * @return the line Color of this Category.
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Category that) {

        // Fail fast
        if (that == null) {
            return -1;
        } else if (that == this) {
            return 0;
        }

        // Delegate
        int toReturn = getName().compareTo(that.getName());
        if (toReturn == 0) {
            toReturn = getFillColor().compareTo(that.getFillColor());
        }
        if (toReturn == 0) {
            toReturn = getLineColor().compareTo(that.getLineColor());
        }
        return toReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {

        // Fail fast
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category)) {
            return false;
        }

        // Delegate to internal state
        final Category category = (Category) o;
        return name.equals(category.name)
                && fillColor == category.fillColor
                && lineColor == category.lineColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (fillColor != null ? fillColor.hashCode() : 0);
        result = 31 * result + (lineColor != null ? lineColor.hashCode() : 0);
        return result;
    }

    /**
     * Retrieves a List of Categories from the supplied List of {@link ConfigOption}s.
     *
     * @param configOptions The List of {@link ConfigOption} created from the JavaDoc options.
     * @return A non-null List of Category instances.
     */
    public static List<Category> parseCategories(final List<ConfigOption> configOptions) {

        final List<Category> toReturn = new ArrayList<>();

        if (configOptions != null) {
            for (ConfigOption current : configOptions) {
                if (current.getOption().equals(JavaDocOption.CATEGORY.getOption())) {

                    // We should have between 1 and 3 arguments here, which
                    // should provide information on the following form:
                    //
                    // 1 arg: categoryName[:fillColor[:lineColor]]
                    // 2 arg: categoryName fillColor[:lineColor]
                    // 3 arg: categoryName fillColor lineColor
                    final String[] categoryAndColorArray = getCategoryAndColorArray(current.getArguments());

                    Color fillColor = categoryAndColorArray[1] == null ? null : Color.valueOf(categoryAndColorArray[1]);
                    Color lineColor = categoryAndColorArray[2] == null ? null : Color.valueOf(categoryAndColorArray[2]);

                    toReturn.add(new Category(categoryAndColorArray[0], fillColor, lineColor));
                }
            }
        }

        // All Done.
        return toReturn;
    }

    /**
     * <p>We should have between 1 and 3 arguments here, which should provide information on the following form:</p>
     * <ul>
     * <li>1 arg: categoryName[:fillColor[:lineColor]]</li>
     * <li>2 arg: categoryName fillColor[:lineColor]</li>
     * <li>3 arg: categoryName fillColor lineColor</li>
     * </ul>
     *
     * @param args The List of arguments as received from the JavaDoc configuration framework.
     * @return a 3-element String[] where element 1 and 2 may be null, in case no color definitions have been provided.
     */
    static String[] getCategoryAndColorArray(final List<String> args) {

        final String[] toReturn = new String[3];

        if(args != null && !args.isEmpty()) {

            List<String> spliced = new ArrayList<>();
            switch (args.size()) {

                case 1:
                    spliced.addAll(Arrays.asList(args.get(0).split(":")));
                    break;

                case 2:
                    spliced.add(args.get(0));
                    spliced.addAll(Arrays.asList(args.get(1).split(":")));
                    break;

                default:
                    spliced.addAll(args);
                    break;
            }

            toReturn[0] = spliced.get(0);
            if(spliced.size() > 1) {
                toReturn[1] = spliced.get(1);
            }
            if(spliced.size() > 2) {
                toReturn[2] = spliced.get(2);
            }
        }

        // All Done.
        return toReturn;
    }
}
