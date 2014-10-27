package org.ObjectLayout;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;

import javax.tools.Diagnostic;

/**
 * A javac annotation processor for the @Intrinsic annotation.
 */
@SupportedAnnotationTypes("org.ObjectLayout.Intrinsic")
@SupportedSourceVersion(RELEASE_6)
public class IntrinsicProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Intrinsic.class);
            for (Element element : elements) {

                if (element.getKind() != ElementKind.FIELD) {
                    reportError("@Intrinsic must prefix a field -- " + element +
                            " is not a field", element);
                    continue;
                }

                Set<Modifier> modifiers = element.getModifiers();

                if (!(modifiers.contains(Modifier.FINAL) && modifiers.contains(Modifier.PRIVATE))) {
                    reportError("@Intrinsic object annotations can only be declared " +
                            "for private final fields", element);
                }

                // We'd like to do a lot more sanity checking here:
                // E.g. : element type must be derived from Object, and must not be a Java array.
                // E.g. : length should only be specified for array (structured and primitive) types.
                // E.g. : elementClass should only be specified for StructuredArrays
                // E.g. : element must only be assigned directly from IntrinsicObjectModel factories.

                // reportInfo("Good @Intrinsic field " + element.getSimpleName(), element);
            }
        }

        return true;
    }

    void reportError(String errorMessage, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMessage, e);
    }

    void reportInfo(String infoMessage, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, infoMessage, e);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
