/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil

import kotlin.reflect.jvm.internal.impl.descriptors.CallableDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.types.KotlinType

fun KotlinType.hasNoInferAnnotation(): Boolean = annotations.hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME)

fun KotlinType.hasExactAnnotation(): Boolean = annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun AnnotationDescriptor.isExactAnnotation(): Boolean = this.fqName == EXACT_ANNOTATION_FQ_NAME

fun Annotations.hasInternalAnnotationForResolve(): Boolean =
        hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME) || hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun FqName.isInternalAnnotationForResolve() = this == NO_INFER_ANNOTATION_FQ_NAME || this == EXACT_ANNOTATION_FQ_NAME

fun CallableDescriptor.hasLowPriorityInOverloadResolution(): Boolean = annotations.hasAnnotation(LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME)

fun CallableDescriptor.hasHidesMembersAnnotation(): Boolean = annotations.hasAnnotation(HIDES_MEMBERS_ANNOTATION_FQ_NAME)
fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = annotations.hasAnnotation(DYNAMIC_EXTENSION_FQ_NAME)

fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)

fun CallableDescriptor.hasBuilderInferenceAnnotation(): Boolean =
    annotations.hasAnnotation(BUILDER_INFERENCE_ANNOTATION_FQ_NAME)

fun getExactInAnnotations(): Annotations = AnnotationsWithOnly(EXACT_ANNOTATION_FQ_NAME)

private class AnnotationsWithOnly(val presentAnnotation: FqName): Annotations {
    override fun iterator(): Iterator<AnnotationDescriptor> = emptyList<AnnotationDescriptor>().iterator()

    override fun isEmpty(): Boolean = false

    override fun hasAnnotation(fqName: FqName): Boolean = fqName == this.presentAnnotation
}
