/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve.sam

import kotlin.reflect.jvm.internal.impl.descriptors.CallableMemberDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.impl.SimpleFunctionDescriptorImpl
import kotlin.reflect.jvm.internal.impl.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import kotlin.reflect.jvm.internal.impl.resolve.scopes.DescriptorKindExclude

interface SamConstructorDescriptor : SimpleFunctionDescriptor, FunctionInterfaceConstructorDescriptor

class SamConstructorDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    private val samInterface: ClassDescriptor
) : SimpleFunctionDescriptorImpl(
    containingDeclaration,
    null,
    samInterface.annotations,
    samInterface.name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    samInterface.source
), SamConstructorDescriptor {
    override val baseDescriptorForSynthetic: ClassDescriptor
        get() = samInterface
}

object SamConstructorDescriptorKindExclude : DescriptorKindExclude() {
    override fun excludes(descriptor: DeclarationDescriptor) = descriptor is SamConstructorDescriptor

    override val fullyExcludedDescriptorKinds: Int get() = 0
}
