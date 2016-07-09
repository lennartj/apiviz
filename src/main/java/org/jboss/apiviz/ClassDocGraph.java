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
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import se.jguru.javadoc.apiviz.JavaDocTag;
import se.jguru.javadoc.apiviz.model.Category;
import se.jguru.javadoc.apiviz.model.DocletModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.jboss.apiviz.FileUtil.ITALIC_FONT;
import static org.jboss.apiviz.FileUtil.NEWLINE;
import static org.jboss.apiviz.FileUtil.NORMAL_FONT;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
public class ClassDocGraph {

    final DocletModel model;
    final RootDoc root;
    private final Map<String, ClassDoc> nodes = new TreeMap<String, ClassDoc>();
    private final Map<ClassDoc, Set<Edge>> edges = new HashMap<ClassDoc, Set<Edge>>();
    private final Map<ClassDoc, Set<Edge>> reversedEdges = new HashMap<ClassDoc, Set<Edge>>();
    private int nonConfiguredCategoryCount = 0;
    private final Map<String, Category> name2CategoryMap = new HashMap<String, Category>();

    public ClassDocGraph(final RootDoc root, final DocletModel model) {

        // Assign internal state
        this.root = root;
        this.model = model;

        // #1) Map all known Categories
        for (Category current : model.getCategories()) {

            final String categoryName = current.getName();
            if (name2CategoryMap.containsKey(categoryName)) {
                root.printWarning("Category defined multiple times: " + categoryName);
            }

            name2CategoryMap.put(current.getName(), current);
        }

        // #2) Populate this ClassDocGraph.
        root.printNotice("Building graph for all classes...");
        for (ClassDoc node : root.classes()) {
            addNode(node, true);
        }
    }

    private void addNode(final ClassDoc node, final boolean addRelatedClasses) {

        String key = node.qualifiedName();
        if (!nodes.containsKey(key)) {
            nodes.put(key, node);
            edges.put(node, new TreeSet<Edge>());
        }

        if (addRelatedClasses) {
            addRelatedClasses(node);
        }
    }

    private void addRelatedClasses(final ClassDoc type) {

        // Generalization
        ClassDoc superType = type.superclass();
        if (superType != null
                && !superType.qualifiedName().equals("java.lang.Object")
                && !superType.qualifiedName().equals("java.lang.Annotation")
                && !superType.qualifiedName().equals("java.lang.Enum")) {

            addNode(superType, false);
            addEdge(new Edge(EdgeType.GENERALIZATION, type, superType));
        }

        // Realization
        for (ClassDoc i : type.interfaces()) {
            if (i.qualifiedName().equals("java.lang.annotation.Annotation")) {
                continue;
            }

            addNode(i, false);
            addEdge(new Edge(EdgeType.REALIZATION, type, i));
        }

        // Apply custom Doclet tags.
        for (Tag t : type.tags()) {
            if (t.name().equals(JavaDocTag.USES.toString())) {
                addEdge(new Edge(root, EdgeType.DEPENDENCY, type, t.text()));
            } else if (t.name().equals(JavaDocTag.HAS.toString())) {
                addEdge(new Edge(root, EdgeType.NAVIGABILITY, type, t.text()));
            } else if (t.name().equals(JavaDocTag.OWNS.toString())) {
                addEdge(new Edge(root, EdgeType.AGGREGATION, type, t.text()));
            } else if (t.name().equals(JavaDocTag.COMPOSED_OF.toString())) {
                addEdge(new Edge(root, EdgeType.COMPOSITION, type, t.text()));
            }
        }

        // Add an edge with '<<see also>>' label for the classes with @see tags, but avoid duplication.
        for (SeeTag t : type.seeTags()) {
            try {
                if (t.referencedClass() == null) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String a = type.qualifiedName();
            String b = t.referencedClass().qualifiedName();
            addNode(t.referencedClass(), false);
            if (a.compareTo(b) != 0) {
                if (a.compareTo(b) < 0) {
                    addEdge(new Edge(root, EdgeType.SEE_ALSO, type, b + " - - &#171;see also&#187;"));
                } else {
                    addEdge(new Edge(root, EdgeType.SEE_ALSO, t.referencedClass(), a + " - - &#171;see also&#187;"));
                }
            }
        }
    }

    private void addEdge(final Edge edge) {
        edges.get(edge.getSource()).add(edge);

        Set<Edge> reversedEdgeSubset = reversedEdges.get(edge.getTarget());
        if (reversedEdgeSubset == null) {
            reversedEdgeSubset = new TreeSet<Edge>();
            reversedEdges.put((ClassDoc) edge.getTarget(), reversedEdgeSubset);
        }
        reversedEdgeSubset.add(edge);
    }

    public String getOverviewSummaryDiagram(final JDepend jdepend) {

        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>(new Comparator<String>() {
            public int compare(final String left, final String right) {
                return right.compareTo(left);
            }
        });

        Set<Edge> edgesToRender = new TreeSet<Edge>();

        addPackageDependencies(jdepend, packages, edgesToRender);

        // Replace direct dependencies with transitive dependencies
        // if possible to simplify the diagram.

        //// Build the matrix first.
        Map<Doc, Set<Doc>> dependencies = new HashMap<Doc, Set<Doc>>();
        for (Edge edge : edgesToRender) {
            Set<Doc> nextDependencies = dependencies.get(edge.getSource());
            if (nextDependencies == null) {
                nextDependencies = new HashSet<Doc>();
                dependencies.put(edge.getSource(), nextDependencies);
            }
            nextDependencies.add(edge.getTarget());
        }

        //// Remove the edges which doesn't change the effective relationship
        //// which can be calculated by indirect (transitive) dependency resolution.
        for (int i = edgesToRender.size(); i > 0; i--) {
            for (Edge edge : edgesToRender) {
                if (isIndirectlyReachable(dependencies, edge.getSource(), edge.getTarget())) {
                    edgesToRender.remove(edge);
                    Set<Doc> targets = dependencies.get(edge.getSource());
                    if (targets != null) {
                        targets.remove(edge.getTarget());
                    }
                    break;
                }
            }
        }

        // Get the least common prefix to compact the diagram even further.
        int minPackageNameLen = Integer.MAX_VALUE;
        int maxPackageNameLen = Integer.MIN_VALUE;
        for (String pname : packages.keySet()) {
            if (pname.length() > maxPackageNameLen) {
                maxPackageNameLen = pname.length();
            }
            if (pname.length() < minPackageNameLen) {
                minPackageNameLen = pname.length();
            }
        }

        if (minPackageNameLen == 0) {
            throw new IllegalStateException("Unexpected empty package name");
        }

        int prefixLen = 0;
        if (!packages.keySet().isEmpty()) {
            String firstPackageName = packages.keySet().iterator().next();
            for (prefixLen = minPackageNameLen; prefixLen > 0; prefixLen--) {
                if (firstPackageName.charAt(prefixLen - 1) != '.') {
                    continue;
                }

                String candidatePrefix = firstPackageName.substring(0, prefixLen);
                boolean found = true;
                for (String pname : packages.keySet()) {
                    if (!pname.startsWith(candidatePrefix)) {
                        found = false;
                        break;
                    }
                }

                if (found) {
                    break;
                }
            }
        }

        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                        "rankdir=LR;" + NEWLINE +
                        "ranksep=0.3;" + NEWLINE +
                        "nodesep=0.2;" + NEWLINE +
                        "mclimit=128;" + NEWLINE +
                        "outputorder=edgesfirst;" + NEWLINE +
                        "center=1;" + NEWLINE +
                        "remincross=true;" + NEWLINE +
                        "searchsize=65536;" + NEWLINE +
                        "splines=polyline;" + NEWLINE +
                        "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                        "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                        "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                        "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        for (PackageDoc pkg : packages.values()) {
            renderPackage(buf, pkg, prefixLen);
        }

        for (Edge edge : edgesToRender) {
            renderEdge(null, buf, edge);
        }

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    private void addPackageDependencies(
            JDepend jdepend, Map<String, PackageDoc> packages, Set<Edge> edgesToRender) {

        Map<String, PackageDoc> allPackages = APIviz.getPackages(root);
        for (String pname : allPackages.keySet()) {
            if (isHidden(allPackages.get(pname))) {
                continue;
            }

            JavaPackage pkg = jdepend.getPackage(pname);
            if (pkg == null) {
                continue;
            }

            packages.put(pname, allPackages.get(pname));

            Collection<JavaPackage> epkgs = pkg.getEfferents();
            if (epkgs == null) {
                continue;
            }

            for (JavaPackage epkg : epkgs) {
                if (isHidden(allPackages.get(epkg.getName()))) {
                    continue;
                }
                addPackageDependency(edgesToRender, allPackages.get(pname), allPackages.get(epkg.getName()));
            }
        }
    }

    static boolean isHidden(Doc node) {
        if (node.tags(JavaDocTag.HIDDEN.toString()).length > 0) {
            return true;
        }

        Tag[] tags = node.tags(JavaDocTag.EXCLUDE.toString());
        if (tags == null) {
            return false;
        }

        for (Tag t : tags) {
            if (t.text() == null || t.text().trim().length() == 0) {
                return true;
            }
        }

        return false;
    }

    private static void addPackageDependency(
            Set<Edge> edgesToRender, PackageDoc source, PackageDoc target) {
        if (source != target && source.isIncluded() && target.isIncluded()) {
            edgesToRender.add(
                    new Edge(EdgeType.DEPENDENCY, source, target));
        }
    }

    private static boolean isIndirectlyReachable(Map<Doc, Set<Doc>> dependencyGraph, Doc source, Doc target) {
        Set<Doc> intermediaryTargets = dependencyGraph.get(source);
        if (intermediaryTargets == null || intermediaryTargets.isEmpty()) {
            return false;
        }

        Set<Doc> visited = new HashSet<Doc>();
        visited.add(source);

        for (Doc t : intermediaryTargets) {
            if (t == target) {
                continue;
            }
            if (isIndirectlyReachable(dependencyGraph, t, target, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIndirectlyReachable(Map<Doc, Set<Doc>> dependencyGraph, Doc source, Doc target, Set<Doc> visited) {
        if (visited.contains(source)) {
            // Evade cyclic dependency.
            return false;
        }
        visited.add(source);

        Set<Doc> intermediaryTargets = dependencyGraph.get(source);
        if (intermediaryTargets == null || intermediaryTargets.isEmpty()) {
            return false;
        }

        for (Doc t : intermediaryTargets) {
            if (t == target) {
                return true;
            }

            if (isIndirectlyReachable(dependencyGraph, t, target, visited)) {
                return true;
            }
        }
        return false;
    }

    public String getPackageSummaryDiagram(final PackageDoc pkg) {
        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                        "rankdir=LR;" + NEWLINE +
                        "ranksep=0.3;" + NEWLINE +
                        "nodesep=0.25;" + NEWLINE +
                        "mclimit=1024;" + NEWLINE +
                        "outputorder=edgesfirst;" + NEWLINE +
                        "center=1;" + NEWLINE +
                        "remincross=true;" + NEWLINE +
                        "searchsize=65536;" + NEWLINE +
                        "splines=polyline;" + NEWLINE +
                        "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                        "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                        "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                        "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        Map<String, ClassDoc> nodesToRender = new TreeMap<String, ClassDoc>();

        Set<Edge> edgesToRender = new TreeSet<Edge>();

        for (ClassDoc node : nodes.values()) {
            fetchSubgraph(pkg, node, nodesToRender, edgesToRender, true, false, true);
        }

        renderSubgraph(pkg, null, buf, nodesToRender, edgesToRender, true);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void checkCategoryExistence(final Doc node) {

        // Don't crash on null nodes
        if (node != null) {

            // Should we add the category with the supplied text?
            final Tag[] nodeCategoryTags = node.tags(JavaDocTag.CATEGORY.toString());
            final String categoryName = nodeCategoryTags == null || nodeCategoryTags.length == 0
                    ? null
                    : nodeCategoryTags[0].text();

            final boolean shouldAddCategory = nodeCategoryTags != null
                    && nodeCategoryTags.length > 0
                    && categoryName != null
                    && !name2CategoryMap.containsKey(nodeCategoryTags[0].text());

            if (shouldAddCategory) {

                Category toAdd = new Category(categoryName, null, null);
                if (ColorCombination.values().length > nonConfiguredCategoryCount) {

                    // Use a predefined combination of colors which works "well"...
                    final ColorCombination colorCombination = ColorCombination.values()[nonConfiguredCategoryCount++];

                    // Overwrite the 'toAdd' Category.
                    toAdd = new Category(
                            categoryName,
                            colorCombination.getFillColor(),
                            colorCombination.getLineColor());
                }

                name2CategoryMap.put(toAdd.getName(), toAdd);
            }
        }
    }

    private void fetchSubgraph(
            PackageDoc pkg, ClassDoc cls,
            Map<String, ClassDoc> nodesToRender, Set<Edge> edgesToRender,
            boolean useHidden, boolean useSee, boolean forceInherit) {

        if (useHidden && isHidden(cls)) {
            return;
        }

        if (forceInherit) {
            for (Tag t : pkg.tags(JavaDocTag.EXCLUDE.toString())) {
                if (t.text() == null || t.text().trim().length() == 0) {
                    continue;
                }

                if (Pattern.compile(t.text().trim()).matcher(cls.qualifiedName()).find()) {
                    return;
                }
            }
        }

        if (cls.containingPackage() == pkg) {
            Set<Edge> directEdges = edges.get(cls);
            nodesToRender.put(cls.qualifiedName(), cls);
            for (Edge edge : directEdges) {
                if (!useSee && edge.getType() == EdgeType.SEE_ALSO) {
                    continue;
                }

                ClassDoc source = (ClassDoc) edge.getSource();
                ClassDoc target = (ClassDoc) edge.getTarget();

                boolean excluded = false;
                if (forceInherit || cls.tags(JavaDocTag.INHERIT.toString()).length > 0) {
                    for (Tag t : pkg.tags(JavaDocTag.EXCLUDE.toString())) {
                        if (t.text() == null || t.text().trim().length() == 0) {
                            continue;
                        }

                        Pattern p = Pattern.compile(t.text().trim());

                        if (p.matcher(source.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                        if (p.matcher(target.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) {
                        continue;
                    }
                }

                for (Tag t : cls.tags(JavaDocTag.EXCLUDE.toString())) {
                    if (t.text() == null || t.text().trim().length() == 0) {
                        continue;
                    }

                    Pattern p = Pattern.compile(t.text().trim());

                    if (p.matcher(source.qualifiedName()).find()) {
                        excluded = true;
                        break;
                    }
                    if (p.matcher(target.qualifiedName()).find()) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) {
                    continue;
                }

                if (!useHidden || !isHidden(source) && !isHidden(target)) {
                    edgesToRender.add(edge);
                }
                if (!useHidden || !isHidden(source)) {
                    nodesToRender.put(source.qualifiedName(), source);
                }
                if (!useHidden || !isHidden(target)) {
                    nodesToRender.put(target.qualifiedName(), target);
                }
            }

            Set<Edge> reversedDirectEdges = reversedEdges.get(cls);
            if (reversedDirectEdges != null) {
                for (Edge edge : reversedDirectEdges) {
                    if (!useSee && edge.getType() == EdgeType.SEE_ALSO) {
                        continue;
                    }

                    if (cls.tags(JavaDocTag.EXCLUDE_SUBTYPES.toString()).length > 0
                            && (edge.getType() == EdgeType.GENERALIZATION
                            || edge.getType() == EdgeType.REALIZATION)) {
                        continue;
                    }

                    ClassDoc source = (ClassDoc) edge.getSource();
                    ClassDoc target = (ClassDoc) edge.getTarget();

                    boolean excluded = false;
                    if (forceInherit || cls.tags(JavaDocTag.INHERIT.toString()).length > 0) {
                        for (Tag t : pkg.tags(JavaDocTag.EXCLUDE.toString())) {
                            if (t.text() == null || t.text().trim().length() == 0) {
                                continue;
                            }

                            Pattern p = Pattern.compile(t.text().trim());

                            if (p.matcher(source.qualifiedName()).find()) {
                                excluded = true;
                                break;
                            }
                            if (p.matcher(target.qualifiedName()).find()) {
                                excluded = true;
                                break;
                            }
                        }
                        if (excluded) {
                            continue;
                        }
                    }

                    for (Tag t : cls.tags(JavaDocTag.EXCLUDE.toString())) {
                        if (t.text() == null || t.text().trim().length() == 0) {
                            continue;
                        }

                        Pattern p = Pattern.compile(t.text().trim());

                        if (p.matcher(source.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                        if (p.matcher(target.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) {
                        continue;
                    }

                    if (!useHidden || !isHidden(source) && !isHidden(target)) {
                        edgesToRender.add(edge);
                    }
                    if (!useHidden || !isHidden(source)) {
                        nodesToRender.put(source.qualifiedName(), source);
                    }
                    if (!useHidden || !isHidden(target)) {
                        nodesToRender.put(target.qualifiedName(), target);
                    }
                }
            }
        }
    }

    public String getClassDiagram(final ClassDoc cls) {
        PackageDoc pkg = cls.containingPackage();

        StringBuilder buf = new StringBuilder(16384);
        Map<String, ClassDoc> nodesToRender = new TreeMap<String, ClassDoc>();
        Set<Edge> edgesToRender = new TreeSet<Edge>();

        fetchSubgraph(pkg, cls, nodesToRender, edgesToRender, false, true, false);

        buf.append("digraph APIVIZ {" + NEWLINE);

        // Determine the graph orientation automatically.
        int nodesAbove = 0;
        int nodesBelow = 0;
        for (Edge e : edgesToRender) {
            if (e.getType().isReversed()) {
                if (e.getSource() == cls) {
                    nodesAbove++;
                } else {
                    nodesBelow++;
                }
            } else {
                if (e.getSource() == cls) {
                    nodesBelow++;
                } else {
                    nodesAbove++;
                }
            }
        }

        boolean portrait;
        if (Math.max(nodesAbove, nodesBelow) <= 5) {
            // Landscape looks better usually up to 5.
            // There are just a few subtypes and supertypes.
            buf.append("rankdir=TB;" + NEWLINE
                    + "ranksep=0.4;" + NEWLINE
                    + "nodesep=0.3;" + NEWLINE);
            portrait = false;
        } else {
            // Portrait looks better.
            // There are too many subtypes or supertypes.
            buf.append("rankdir=LR;" + NEWLINE
                    + "ranksep=1.0;" + NEWLINE
                    + "nodesep=0.2;" + NEWLINE);
            portrait = true;
        }

        buf.append("mclimit=128;" + NEWLINE
                + "outputorder=edgesfirst;" + NEWLINE
                + "center=1;" + NEWLINE
                + "remincross=true;" + NEWLINE
                + "searchsize=65536;" + NEWLINE
                + "splines=polyline;" + NEWLINE
                + "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", "
                + "style=\"setlinewidth(0.6)\"]; " + NEWLINE
                + "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", "
                + "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        renderSubgraph(pkg, cls, buf, nodesToRender, edgesToRender, portrait);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void renderSubgraph(final PackageDoc pkg,
            final ClassDoc cls,
            final StringBuilder buf,
            final Map<String, ClassDoc> nodesToRender,
            final Set<Edge> edgesToRender,
            final boolean portrait) {

        List<ClassDoc> nodesToRenderCopy = new ArrayList<ClassDoc>(nodesToRender.values());
        Collections.sort(nodesToRenderCopy, new ClassDocComparator(portrait));

        for (ClassDoc node : nodesToRenderCopy) {
            renderClass(pkg, cls, buf, node);
        }

        for (Edge edge : edgesToRender) {
            renderEdge(pkg, buf, edge);
        }
    }

    private void renderPackage(final StringBuilder buf,
            final PackageDoc pkg,
            final int prefixLen) {

        checkCategoryExistence(pkg);

        String href = pkg.name().replace('.', '/') + "/package-summary.html";
        buf.append(getNodeId(pkg));
        buf.append(" [label=\"");
        buf.append(pkg.name().substring(prefixLen));
        buf.append("\", style=\"filled");
        if (pkg.tags("@deprecated").length > 0) {
            buf.append(",dotted");
        }
        buf.append("\", fillcolor=\"");
        buf.append(getFillColor(pkg));
        buf.append("\", href=\"");
        buf.append(href);
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderClass(final PackageDoc pkg,
            final ClassDoc cls,
            final StringBuilder buf,
            final ClassDoc node) {

        checkCategoryExistence(node);

        String fillColor = getFillColor(pkg, cls, node);
        String lineColor = getLineColor(pkg, cls, node);
        String fontColor = getFontColor(pkg, node);
        String href = getPath(pkg, node);

        buf.append(getNodeId(node));
        buf.append(" [label=\"");
        buf.append(getNodeLabel(pkg, node));
        buf.append("\", tooltip=\"");
        buf.append(escape(getNodeLabel(pkg, node)));
        buf.append("\"");
        if (node.isAbstract() && !node.isInterface()) {
            buf.append(", fontname=\"");
            buf.append(ITALIC_FONT);
            buf.append("\"");
        }
        buf.append(", style=\"filled");
        if (node.tags("@deprecated").length > 0) {
            buf.append(",dotted");
        }
        buf.append("\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", fillcolor=\"");
        buf.append(fillColor);

        if (href != null) {
            buf.append("\", href=\"");
            buf.append(href);
        }

        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderEdge(final PackageDoc pkg,
            final StringBuilder buf,
            final Edge edge) {

        EdgeType type = edge.getType();
        String lineColor = getLineColor(pkg, edge);
        String fontColor = getFontColor(pkg, edge);

        // Graphviz lays out nodes upside down - adjust for
        // important relationships.
        boolean reverse = edge.getType().isReversed();

        if (reverse) {
            buf.append(getNodeId(edge.getTarget()));
            buf.append(" -> ");
            buf.append(getNodeId(edge.getSource()));
            buf.append(" [arrowhead=\"");
            buf.append(type.getArrowTail());
            buf.append("\", arrowtail=\"");
            buf.append(type.getArrowHead() == null ? (edge.isOneway() ? "open" : "none") : type.getArrowHead());
        } else {
            buf.append(getNodeId(edge.getSource()));
            buf.append(" -> ");
            buf.append(getNodeId(edge.getTarget()));
            buf.append(" [arrowhead=\"");
            buf.append(type.getArrowHead() == null ? (edge.isOneway() ? "open" : "none") : type.getArrowHead());
            buf.append("\", arrowtail=\"");
            buf.append(type.getArrowTail());
        }

        buf.append("\", style=\"" + type.getStyle());
        buf.append("\", dir=\"both");
        buf.append("\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", label=\"");
        buf.append(escape(edge.getEdgeLabel()));
        buf.append("\", headlabel=\"");
        buf.append(escape(edge.getTargetLabel()));
        buf.append("\", taillabel=\"");
        buf.append(escape(edge.getSourceLabel()));
        buf.append("\" ];");
        buf.append(NEWLINE);
    }

    private static String getStereotype(final ClassDoc node) {
        String stereotype = node.isInterface() ? "interface" : null;
        if (node.isException() || node.isError()) {
            stereotype = "exception";
        } else if (node.isAnnotationType()) {
            stereotype = "annotation";
        } else if (node.isEnum()) {
            stereotype = "enum";
        } else if (isStaticType(node)) {
            stereotype = "static";
        }

        if (node.tags(JavaDocTag.STEREOTYPE.toString()).length > 0) {
            stereotype = node.tags(JavaDocTag.STEREOTYPE.toString())[0].text();
        }

        return escape(stereotype);
    }

    static boolean isStaticType(final ClassDoc node) {
        boolean staticType = true;
        int methods = 0;
        for (MethodDoc m : node.methods()) {
            if (m.isConstructor()) {
                continue;
            }
            methods++;
            if (!m.isStatic()) {
                staticType = false;
                break;
            }
        }

        return staticType && methods > 0;
    }

    private Color getFillColor(final PackageDoc pkg) {

        // Does the current PackageDoc have a Category defined?
        final List<String> categoryNames = getTextsForTag(JavaDocTag.CATEGORY, pkg);
        final String categoryName = categoryNames.isEmpty()
                ? null
                : categoryNames.get(0);

        final Category existingCategory = categoryName == null
                ? null
                : name2CategoryMap.get(categoryName);

        // Default fill color
        Color toReturn = Color.white;

        if (existingCategory != null) {

            // We have an existing Category. Use its fill color
            toReturn = existingCategory.getFillColor();
        }
        if (pkg.tags(JavaDocTag.LANDMARK.toString()).length > 0) {

            // Landmark tags imply Khaki1...
            toReturn = Color.khaki1;
        }

        // All Done.
        return toReturn;
    }

    private Color getFillColor(final PackageDoc pkg, final ClassDoc cls, final ClassDoc node) {

        Color toReturn = Color.white;

        // Investigate if we have an existing Category
        final List<String> textsForTag = getTextsForTag(JavaDocTag.CATEGORY, node);
        final String categoryName = textsForTag.isEmpty()
                ? null
                : textsForTag.get(0);

        final Category existingCategory = categoryName == null
                ? null
                : name2CategoryMap.get(categoryName);

        if (cls == null) {

            // We are rendering for a package summary since there is no Class
            // See if the node has a fillColor
            if (existingCategory != null) {
                toReturn = existingCategory.getFillColor();
            }

            // Override previous values if a Landmark is set
            if (node.containingPackage() == pkg && node.tags(JavaDocTag.LANDMARK.toString()).length > 0) {
                toReturn = Color.khaki1;
            }

        } else if (cls == node) {
            // this is class we are rending the class diagram for
            toReturn = Color.khaki1;

        } else if (existingCategory != null) {

            // not the class for the class diagram so use its fill color
            toReturn = existingCategory.getFillColor();

            if (node.containingPackage() != pkg && toReturn.matches("^[!@#$%^&*+=][0-9A-Fa-f]{6}$")) {

                //grey out the fill color
                final StringBuffer sb = new StringBuffer("#");
                sb.append(shiftColor(toReturn.substring(1, 3)));
                sb.append(shiftColor(toReturn.substring(3, 5)));
                sb.append(shiftColor(toReturn.substring(5, 7)));
                toReturn = sb.toString();
            }
        }
        return toReturn;
    }

    /**
     * Shifts the supplied Color to achieve a "gray-out" effect.
     *
     * @param number
     * @return
     */
    private static String shiftColor(final String number) {

        Integer colorValue = Integer.parseInt(number, 16);
        colorValue = colorValue + 0x4D; //aproach white
        if (colorValue > 0xFF) {
            colorValue = 0xFF;
        }
        return Integer.toHexString(colorValue);
    }

    private String getLineColor(final PackageDoc pkg,
            final ClassDoc cls,
            final ClassDoc node) {

        String color = "#000000";
        if (cls != node && node.tags(JavaDocTag.LANDMARK.toString()).length <= 0
                && node.tags(JavaDocTag.CATEGORY.toString()).length > 0
                && name2CategoryMap.containsKey(node.tags(JavaDocTag.CATEGORY.toString())[0].text())) {
            color = name2CategoryMap.get(node.tags(JavaDocTag.CATEGORY.toString())[0].text()).getLineColor();
        }

        if (node.containingPackage() != pkg) {
            //grey out the fill color
            final StringBuffer sb = new StringBuffer("#");
            sb.append(shiftColor(color.substring(1, 3)));
            sb.append(shiftColor(color.substring(3, 5)));
            sb.append(shiftColor(color.substring(5, 7)));
            color = sb.toString();
        }
        return color;
    }

    private String getLineColor(final PackageDoc pkg, final Edge edge) {

        if (edge.getTarget() instanceof ClassDoc) {
            //we have a class
            return getLineColor(pkg, (ClassDoc) edge.getSource(), (ClassDoc) edge.getTarget());

        } else {
            //not a class (a package or something)
            String color = "#000000";

            if (pkg != null
                    && pkg.tags(JavaDocTag.CATEGORY.toString()).length > 0
                    && name2CategoryMap.containsKey(pkg.tags(JavaDocTag.CATEGORY.toString())[0].text())) {
                color = name2CategoryMap.get(pkg.tags(JavaDocTag.CATEGORY.toString())[0].text()).getLineColor();
            }
            return color;
        }
    }

    private static String getFontColor(final PackageDoc pkg, final ClassDoc doc) {
        String color = "black";
        if (!(doc.containingPackage() == pkg)) {
            color = "gray30";
        }
        return color;
    }

    private static String getFontColor(final PackageDoc pkg, final Edge edge) {
        if (edge.getTarget() instanceof ClassDoc) {
            return getFontColor(pkg, (ClassDoc) edge.getTarget());
        } else {
            return "black";
        }
    }

    private static String getNodeId(final Doc node) {
        String name;
        if (node instanceof ClassDoc) {
            name = ((ClassDoc) node).qualifiedName();
        } else {
            name = node.name();
        }
        return name.replace('.', '_');
    }

    private static String getNodeLabel(final PackageDoc pkg, final ClassDoc node) {
        StringBuilder buf = new StringBuilder(256);
        String stereotype = getStereotype(node);
        if (stereotype != null) {
            //TODO - we should have an option to use "<<" and ">>" for systems
            // where the encoding is messed up
            buf.append("&#171;");
            buf.append(stereotype);
            buf.append("&#187;\\n");
        }

        if (node.containingPackage() == pkg) {
            //we are in the same package
            buf.append(node.name());
        } else {
            //in a different package
            if (node.containingPackage() == null) {
                //if the class does not have a package
                buf.append(node.name());
            } else {
                //it does, so append the package name
                buf.append(node.name());
                buf.append("\\n(");
                buf.append(node.containingPackage().name());
                buf.append(')');
            }
        }
        return buf.toString();
    }

    private static String escape(final String text) {

        // Escape some characters to prevent syntax errors.
        if (text != null) {
            return text.replaceAll("(\"|'|\\\\.?|\\s)+", " ");
        }

        return text;
    }

    private static String getPath(final PackageDoc pkg, final ClassDoc node) {
        if (!node.isIncluded()) {
            return null;
        }

        String sourcePath = pkg.name().replace('.', '/');
        String targetPath =
                node.containingPackage().name().replace('.', '/') + '/' + node.name() + ".html";
        String[] sourcePathElements = sourcePath.split("[\\/\\\\]+");
        String[] targetPathElements = targetPath.split("[\\/\\\\]+");

        int maxCommonLength = Math.min(sourcePathElements.length, targetPathElements.length);
        int commonLength;
        for (commonLength = 0; commonLength < maxCommonLength; commonLength++) {
            if (!sourcePathElements[commonLength].equals(targetPathElements[commonLength])) {
                break;
            }
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < sourcePathElements.length - commonLength; i++) {
            buf.append("/..");
        }

        for (int i = commonLength; i < targetPathElements.length; i++) {
            buf.append('/');
            buf.append(targetPathElements[i]);
        }
        return buf.substring(1);
    }

    protected class CategoryOptions {
        private String fillColor = "#FFFFFF";
        private String lineColor = "#000000";

        protected CategoryOptions(String catName, final String fillColor, final String lineColor) {
            this.fillColor = Color.resolveColor(fillColor);
            if (lineColor != null) {
                this.lineColor = Color.resolveColor(lineColor);
            }
            root.printNotice("Category Options: " + catName + ", " + fillColor + ", " + lineColor);
        }

        protected CategoryOptions(String catName, final ColorCombination combination) {
            fillColor = combination.getFillColor().getRgbValue();
            lineColor = combination.getLineColor().getRgbValue();
            root.printNotice("Category Options: " + catName + ", " + fillColor + ", " + lineColor);
        }

        public String getFillColor() {
            return fillColor;
        }

        public String getLineColor() {
            return lineColor;
        }

    }

    private static List<String> getTextsForTag(final JavaDocTag expectedTag, final Doc aDoc) {

        final List<String> toReturn = new ArrayList<>();

        if(expectedTag != null && aDoc != null) {

            final Tag[] tags = aDoc.tags(expectedTag.toString());
            if(tags != null && tags.length > 0) {
                for(Tag current : tags) {
                    toReturn.add(current.text());
                }
            }
        }

        // All Done.
        return toReturn;
    }
}
