/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal.impl.load.java.lazy

import kotlin.reflect.jvm.internal.impl.builtins.StandardNames
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaAnnotationMapper
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaAnnotation
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaAnnotationOwner
import kotlin.reflect.jvm.internal.impl.name.FqName

class LazyJavaAnnotations(
    private val c: LazyJavaResolverContext,
    private val annotationOwner: JavaAnnotationOwner,
    private val areAnnotationsFreshlySupported: Boolean = false
) : Annotations {
    private val annotationDescriptors = c.components.storageManager.createMemoizedFunctionWithNullableValues { annotation: JavaAnnotation ->
        JavaAnnotationMapper.mapOrResolveJavaAnnotation(annotation, c, areAnnotationsFreshlySupported)
    }

    override fun findAnnotation(fqName: FqName) =
        annotationOwner.findAnnotation(fqName)?.let(annotationDescriptors)
            ?: JavaAnnotationMapper.findMappedJavaAnnotation(fqName, annotationOwner, c)

    override fun iterator() =
        (annotationOwner.annotations.asSequence().map(annotationDescriptors) +
                JavaAnnotationMapper.findMappedJavaAnnotation(
                    StandardNames.FqNames.deprecated,
                    annotationOwner,
                    c
                )).filterNotNull().iterator()

    override fun isEmpty() = annotationOwner.annotations.isEmpty() && !annotationOwner.isDeprecatedInJavaDoc
}

fun LazyJavaResolverContext.resolveAnnotations(annotationsOwner: JavaAnnotationOwner): Annotations =
    LazyJavaAnnotations(this, annotationsOwner)
