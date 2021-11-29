/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.types

import kotlin.reflect.jvm.internal.impl.descriptors.TypeAliasDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor

interface TypeAliasExpansionReportStrategy {
    fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int)
    fun conflictingProjection(typeAlias: TypeAliasDescriptor, typeParameter: TypeParameterDescriptor?, substitutedArgument: KotlinType)
    fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor)
    fun boundsViolationInSubstitution(
        substitutor: TypeSubstitutor,
        unsubstitutedArgument: KotlinType,
        argument: KotlinType,
        typeParameter: TypeParameterDescriptor
    )

    fun repeatedAnnotation(annotation: AnnotationDescriptor)

    object DO_NOTHING : TypeAliasExpansionReportStrategy {
        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {}
        override fun conflictingProjection(
            typeAlias: TypeAliasDescriptor,
            typeParameter: TypeParameterDescriptor?,
            substitutedArgument: KotlinType
        ) {
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {}
        override fun boundsViolationInSubstitution(
            substitutor: TypeSubstitutor,
            unsubstitutedArgument: KotlinType,
            argument: KotlinType,
            typeParameter: TypeParameterDescriptor
        ) {
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {}
    }
}
