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

import java.util.Comparator;

/**
 * Comparator implementation for ClassDoc instances, which takes layout orientation into account.
 *
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
class ClassDocComparator implements Comparator<ClassDoc> {

    private final boolean isPortraitOrientation;

    /**
     * Creates a new ClassDocComparator using the supplied layout orientation.
     *
     * @param isPortraitOrientation if true, portrait orientation is used.
     */
    ClassDocComparator(final boolean isPortraitOrientation) {
        this.isPortraitOrientation = isPortraitOrientation;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final ClassDoc left, final ClassDoc right) {

        // Calculate the natural sort order between the supplied ClassDocs
        final int typeDifference = getNaturalSortOrder(left) - getNaturalSortOrder(right);

        // Different types?
        if (typeDifference != 0) {
            return isPortraitOrientation ? -typeDifference : typeDifference;
        }

        // Fallback to comparing the names of the ClassDocs given.
        return isPortraitOrientation ? left.name().compareTo(right.name()) : right.name().compareTo(left.name());
    }

    //
    // Private helpers
    //

    /**
     * Retrieves the normal/natural sort order between ClassDocs, which are assumed to be sorted as follows:
     *
     * <ol>
     *     <li>Annotations</li>
     *     <li>Enums</li>
     *     <li>Static Classes</li>
     *     <li>Interfaces</li>
     *     <li>Abstract types</li>
     *     <li>All other types</li>
     *     <li>Errors / Exceptions</li>
     * </ol>
     *
     * @param classDoc A non-null ClassDoc instance.
     * @return
     */
    @SuppressWarnings("all")
    private static int getNaturalSortOrder(final ClassDoc classDoc) {

        if (classDoc.isAnnotationType()) {
            return 0;
        }

        if (classDoc.isEnum()) {
            return 1;
        }

        if (ClassDocGraph.isStaticType(classDoc)) {
            return 2;
        }

        if (classDoc.isInterface()) {
            return 3;
        }

        if (classDoc.isAbstract()) {
            return 4;
        }

        if (classDoc.isError() || classDoc.isException()) {
            return 100;
        }

        return 50;
    }
}
