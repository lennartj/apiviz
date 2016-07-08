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

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SourcePosition;

/**
 * {@link RootDoc} implementation which swallows/ignores APIviz-related messages.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class APIvizWrappedRootDoc extends AbstractRootDocWrapper {

    /**
     * Constructs an {@link APIvizWrappedRootDoc} instance around the supplied {@link RootDoc}.
     *
     * @param wrappedRootDoc A non-null {@link RootDoc} instance to be wrapped by this {@link APIvizWrappedRootDoc}.
     */
    public APIvizWrappedRootDoc(final RootDoc wrappedRootDoc) {
        super(wrappedRootDoc);
    }

    /**
     * Delegates to the wrapped {@link RootDoc} only if the supplied Message does not contain an
     * {@link JavaDocTag} identifier. Otherwise, the message is ignored.
     *
     * @param msg The warning message.
     */
    @Override
    public void printWarning(final String msg) {

        // Swallow APIViz warnings.
        if (!containsApiVizTag(msg)) {
            wrappedRootDoc.printWarning(msg);
        }
    }

    /**
     * Delegates to the wrapped {@link RootDoc} only if the supplied Message does not contain an
     * {@link JavaDocTag} identifier. Otherwise, the message is ignored.
     *
     * @param pos The position in the Source.
     * @param msg The warning message.
     */
    @Override
    public void printWarning(final SourcePosition pos, final String msg) {
        super.printWarning(pos, msg);
    }

    /**
     * Checks if ths supplied message contains an {@link JavaDocTag} reference, on the form
     * {@code '@apiviz.xxxx '}.
     *
     * @param message The message to check.
     * @return {@code true} if the supplied message contains an {@link JavaDocTag} reference.
     */
    public static boolean containsApiVizTag(final String message) {

        if (message != null) {
            for (JavaDocTag current : JavaDocTag.values()) {
                if (message.contains(current.toString() + " ")) {

                    // The message contains an entry on the form '@apiviz.xxxx '
                    // which implies that it has to do with an apiviz javadoc Tag.
                    return true;
                }
            }
        }

        // Nopes.
        return false;
    }
}
