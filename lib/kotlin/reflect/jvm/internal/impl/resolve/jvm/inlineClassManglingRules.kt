/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve.jvm

import kotlin.reflect.jvm.internal.impl.builtins.StandardNames
import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorUtils
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.fqNameSafe
import kotlin.reflect.jvm.internal.impl.resolve.isInlineClass
import kotlin.reflect.jvm.internal.impl.resolve.isInlineClassType
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.typeUtil.representativeUpperBound

fun shouldHideConstructorDueToInlineClassTypeValueParameters(descriptor: CallableMemberDescriptor): Boolean {
    val constructorDescriptor = descriptor as? ClassConstructorDescriptor ?: return false
    if (DescriptorVisibilities.isPrivate(constructorDescriptor.visibility)) return false
    if (constructorDescriptor.constructedClass.isInlineClass()) return false
    if (DescriptorUtils.isSealedClass(constructorDescriptor.constructedClass)) return false

    // TODO inner class in inline class

    return constructorDescriptor.valueParameters.any { it.type.requiresFunctionNameManglingInParameterTypes() }
}

fun requiresFunctionNameManglingForParameterTypes(descriptor: CallableMemberDescriptor): Boolean {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    return extensionReceiverType != null && extensionReceiverType.requiresFunctionNameManglingInParameterTypes() ||
            descriptor.valueParameters.any { it.type.requiresFunctionNameManglingInParameterTypes() }
}

// NB functions returning all inline classes (including our special 'kotlin.Result') should be mangled.
fun requiresFunctionNameManglingForReturnType(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.containingDeclaration !is ClassDescriptor) return false
    val returnType = descriptor.returnType ?: return false
    return returnType.isInlineClassType() || returnType.isTypeParameterWithUpperBoundThatRequiresMangling()
}

fun DeclarationDescriptor.isInlineClassThatRequiresMangling(): Boolean =
    isInlineClass() && !isDontMangleClass(this as ClassDescriptor)

fun KotlinType.isInlineClassThatRequiresMangling() =
    constructor.declarationDescriptor?.isInlineClassThatRequiresMangling() == true

private fun KotlinType.requiresFunctionNameManglingInParameterTypes() =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == StandardNames.RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return descriptor.representativeUpperBound.requiresFunctionNameManglingInParameterTypes()
}
