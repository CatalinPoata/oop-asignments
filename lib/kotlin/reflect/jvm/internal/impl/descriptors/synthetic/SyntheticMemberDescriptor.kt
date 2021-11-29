/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.descriptors.synthetic

import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.FunctionDescriptor

interface SyntheticMemberDescriptor<out T : DeclarationDescriptor> {
    val baseDescriptorForSynthetic: T
}

interface FunctionInterfaceConstructorDescriptor : SyntheticMemberDescriptor<ClassDescriptor>

interface FunctionInterfaceAdapterDescriptor<out T : FunctionDescriptor> : SyntheticMemberDescriptor<T>

interface FunctionInterfaceAdapterExtensionFunctionDescriptor : SyntheticMemberDescriptor<FunctionDescriptor>