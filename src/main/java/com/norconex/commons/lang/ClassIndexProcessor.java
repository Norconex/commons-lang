/* Copyright 2026 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

/**
 * Writes {@value ClassFinder#INDEX_RESOURCE} for every compilation that has a
 * package annotated with {@link ClassIndex}. It runs as part of the compiler,
 * so the index is built from the same sources as the classes it lists and
 * cannot drift from them.
 * <p>
 * Registered for the compiler through
 * {@code META-INF/services/javax.annotation.processing.Processor}. A module
 * that does not use {@link ClassIndex} is not affected.
 * </p>
 *
 * @since 3.2.0
 */
@SupportedAnnotationTypes("com.norconex.commons.lang.ClassIndex")
public class ClassIndexProcessor extends AbstractProcessor {

    private final Set<String> classes = new TreeSet<>();
    private final Set<CompilationUnitTree> units = new LinkedHashSet<>();
    private boolean requested;
    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        try {
            trees = Trees.instance(env);
        } catch (IllegalArgumentException e) {
            trees = null;
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        if (!roundEnv.getElementsAnnotatedWith(ClassIndex.class).isEmpty()) {
            requested = true;
        }
        for (Element root : roundEnv.getRootElements()) {
            if (root instanceof TypeElement type) {
                collect(type);
            }
        }
        if (roundEnv.processingOver() && requested) {
            if (trees == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "Compiler offers no source trees: anonymous and local "
                                + "classes are not in "
                                + ClassFinder.INDEX_RESOURCE);
            }
            write();
        }
        return false;
    }

    /**
     * Adds the type if it is concrete, then the types nested in it. With the
     * compiler's source trees, every class in the compilation unit is seen,
     * anonymous and local ones included; without them only named types are.
     */
    private void collect(TypeElement type) {
        if (trees == null) {
            collectNamed(type);
            return;
        }
        var path = trees.getPath(type);
        if (path == null || !units.add(path.getCompilationUnit())) {
            return;
        }
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree tree, Void unused) {
                var element = trees.getElement(getCurrentPath());
                if (element instanceof TypeElement type && isConcrete(type)) {
                    classes.add(binaryName(type));
                }
                return super.visitClass(tree, unused);
            }
        }.scan(path.getCompilationUnit(), null);
    }

    private void collectNamed(TypeElement type) {
        if (isConcrete(type)) {
            classes.add(binaryName(type));
        }
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed instanceof TypeElement nested) {
                collectNamed(nested);
            }
        }
    }

    private static boolean isConcrete(TypeElement type) {
        var kind = type.getKind();
        var isClass = kind == ElementKind.CLASS || kind == ElementKind.ENUM
                || kind == ElementKind.RECORD;
        return isClass && !type.getModifiers().contains(Modifier.ABSTRACT);
    }

    private String binaryName(TypeElement type) {
        return processingEnv.getElementUtils().getBinaryName(type).toString();
    }

    private void write() {
        try {
            var file = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "",
                    ClassFinder.INDEX_RESOURCE);
            try (var out = new PrintWriter(new OutputStreamWriter(
                    file.openOutputStream(), StandardCharsets.UTF_8))) {
                out.println("# Concrete classes of this compilation, read by "
                        + ClassFinder.class.getName()
                        + ". Generated by " + getClass().getName()
                        + "; do not edit.");
                classes.forEach(out::println);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not write " + ClassFinder.INDEX_RESOURCE + ": " + e);
        }
    }
}
