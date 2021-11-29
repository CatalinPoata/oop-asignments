/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.builtins.jvm

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.descriptors.impl.SimpleFunctionDescriptorImpl
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.builtIns
import kotlin.reflect.jvm.internal.impl.resolve.scopes.GivenFunctionsMemberScope
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

class CloneableClassScope(
    storageManager: StorageManager,
    containingClass: ClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = listOf(
        SimpleFunctionDescriptorImpl.create(containingClass, Annotations.EMPTY, CLONE_NAME, DECLARATION, SourceElement.NO_SOURCE).apply {
            initialize(
                null, containingClass.thisAsReceiverParameter, emptyList(), emptyList(), containingClass.builtIns.anyType,
                Modality.OPEN, DescriptorVisibilities.PROTECTED
            )
        }
    )

    companion object {
        val CLONE_NAME = Name.identifier("clone")
    }
}
