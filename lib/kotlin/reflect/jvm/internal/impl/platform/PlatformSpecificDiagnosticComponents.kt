/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.platform

import kotlin.reflect.jvm.internal.impl.container.DefaultImplementation
import kotlin.reflect.jvm.internal.impl.container.PlatformSpecificExtension
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor

@DefaultImplementation(impl = PlatformSpecificDiagnosticComponents.Default::class)
interface PlatformSpecificDiagnosticComponents : PlatformSpecificExtension<PlatformSpecificDiagnosticComponents> {
    fun isNullabilityAnnotation(
        annotationDescriptor: AnnotationDescriptor,
        containingDeclaration: DeclarationDescriptor
    ): Boolean

    object Default : PlatformSpecificDiagnosticComponents {
        override fun isNullabilityAnnotation(
            annotationDescriptor: AnnotationDescriptor,
            containingDeclaration: DeclarationDescriptor
        ): Boolean = false
    }
}