/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.TypeSubstitutor
import kotlin.reflect.jvm.internal.impl.types.TypeUtils
import kotlin.reflect.jvm.internal.impl.types.Variance
import kotlin.reflect.jvm.internal.impl.utils.addToStdlib.safeAs

val JVM_INLINE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmInline")

fun ClassDescriptor.underlyingRepresentation(): ValueParameterDescriptor? {
    if (!isInlineClass()) return null
    return unsubstitutedPrimaryConstructor?.valueParameters?.singleOrNull()
}

// FIXME: DeserializedClassDescriptor in reflection do not have @JvmInline annotation, that we
// FIXME: would like to check as well.
fun DeclarationDescriptor.isInlineClass() = this is ClassDescriptor && (isInline || isValue)

fun KotlinType.unsubstitutedUnderlyingParameter(): ValueParameterDescriptor? {
    return constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.underlyingRepresentation()
}

fun KotlinType.unsubstitutedUnderlyingType(): KotlinType? = unsubstitutedUnderlyingParameter()?.type

fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false

fun KotlinType.substitutedUnderlyingType(): KotlinType? {
    val parameter = unsubstitutedUnderlyingParameter() ?: return null
    return TypeSubstitutor.create(this).substitute(parameter.type, Variance.INVARIANT)
}

fun KotlinType.isRecursiveInlineClassType() =
    isRecursiveInlineClassTypeInner(hashSetOf())

private fun KotlinType.isRecursiveInlineClassTypeInner(visited: HashSet<ClassifierDescriptor>): Boolean {
    val descriptor = constructor.declarationDescriptor?.original ?: return false

    if (!visited.add(descriptor)) return true

    return when (descriptor) {
        is ClassDescriptor ->
            descriptor.isInlineClass() &&
                    unsubstitutedUnderlyingType()?.isRecursiveInlineClassTypeInner(visited) == true

        is TypeParameterDescriptor ->
            descriptor.upperBounds.any { it.isRecursiveInlineClassTypeInner(visited) }

        else -> false
    }
}

fun KotlinType.isNullableUnderlyingType(): Boolean {
    if (!isInlineClassType()) return false
    val underlyingType = unsubstitutedUnderlyingType() ?: return false

    return TypeUtils.isNullableType(underlyingType)
}

fun CallableDescriptor.isGetterOfUnderlyingPropertyOfInlineClass() =
    this is PropertyGetterDescriptor && correspondingProperty.isUnderlyingPropertyOfInlineClass()

fun VariableDescriptor.isUnderlyingPropertyOfInlineClass(): Boolean {
    if (extensionReceiverParameter != null) return false
    val containingDeclaration = this.containingDeclaration
    if (!containingDeclaration.isInlineClass()) return false

    return (containingDeclaration as ClassDescriptor).underlyingRepresentation()?.name == this.name
}
