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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;

import java.io.Serializable;

/**
 * Abstract RootDoc wrapper which delegates all calls to the original/wrapped RootDoc instance.
 * Intended to be overridden by subclasses.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 * @see RootDoc
 */
public abstract class AbstractRootDocWrapper implements RootDoc, Serializable {

    /**
     * The wrapped, non-null RootDoc instance.
     */
    protected RootDoc wrappedRootDoc;

    /**
     * Constructs an {@link AbstractRootDocWrapper} instance around the supplied {@link RootDoc}.
     *
     * @param wrappedRootDoc A non-null {@link RootDoc} instance to be wrapped by this {@link AbstractRootDocWrapper}.
     */
    public AbstractRootDocWrapper(final RootDoc wrappedRootDoc) {

        // Check sanity
        if (wrappedRootDoc == null) {
            throw new NullPointerException("Cannot handle null 'wrappedRootDoc' argument.");
        }

        // Assign internal state
        this.wrappedRootDoc = wrappedRootDoc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[][] options() {
        return wrappedRootDoc.options();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageDoc[] specifiedPackages() {
        return wrappedRootDoc.specifiedPackages();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassDoc[] specifiedClasses() {
        return wrappedRootDoc.specifiedClasses();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassDoc[] classes() {
        return wrappedRootDoc.classes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageDoc packageNamed(final String name) {
        return wrappedRootDoc.packageNamed(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassDoc classNamed(final String qualifiedName) {
        return wrappedRootDoc.classNamed(qualifiedName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String commentText() {
        return wrappedRootDoc.commentText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag[] tags() {
        return wrappedRootDoc.tags();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag[] tags(final String tagname) {
        return wrappedRootDoc.tags(tagname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeeTag[] seeTags() {
        return wrappedRootDoc.seeTags();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag[] inlineTags() {
        return wrappedRootDoc.inlineTags();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag[] firstSentenceTags() {
        return wrappedRootDoc.firstSentenceTags();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRawCommentText() {
        return wrappedRootDoc.getRawCommentText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRawCommentText(final String rawDocumentation) {
        wrappedRootDoc.setRawCommentText(rawDocumentation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return wrappedRootDoc.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Object obj) {
        return wrappedRootDoc.compareTo(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isField() {
        return wrappedRootDoc.isField();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnumConstant() {
        return wrappedRootDoc.isEnumConstant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConstructor() {
        return wrappedRootDoc.isConstructor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMethod() {
        return wrappedRootDoc.isMethod();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnotationTypeElement() {
        return wrappedRootDoc.isAnnotationTypeElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterface() {
        return wrappedRootDoc.isInterface();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isException() {
        return wrappedRootDoc.isException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError() {
        return wrappedRootDoc.isError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnum() {
        return wrappedRootDoc.isEnum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnotationType() {
        return wrappedRootDoc.isAnnotationType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOrdinaryClass() {
        return wrappedRootDoc.isOrdinaryClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClass() {
        return wrappedRootDoc.isClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIncluded() {
        return wrappedRootDoc.isIncluded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SourcePosition position() {
        return wrappedRootDoc.position();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printError(final String msg) {
        wrappedRootDoc.printError(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printError(final SourcePosition pos, final String msg) {
        wrappedRootDoc.printError(pos, msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printWarning(final String msg) {
        wrappedRootDoc.printWarning(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printWarning(final SourcePosition pos, final String msg) {
        wrappedRootDoc.printWarning(pos, msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printNotice(final String msg) {
        wrappedRootDoc.printNotice(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printNotice(final SourcePosition pos, final String msg) {
        wrappedRootDoc.printNotice(pos, msg);
    }
}
