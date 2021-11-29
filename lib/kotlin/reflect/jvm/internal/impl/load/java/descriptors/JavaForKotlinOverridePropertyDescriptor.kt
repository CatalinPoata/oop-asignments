/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.load.java.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.CallableMemberDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.PropertyDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations

class JavaForKotlinOverridePropertyDescriptor(
    ownerDescriptor: ClassDescriptor,
    getterMethod: SimpleFunctionDescriptor,
    setterMethod: SimpleFunctionDescriptor?,
    overriddenProperty: PropertyDescriptor
) : JavaPropertyDescriptor(
    ownerDescriptor,
    Annotations.EMPTY,
    getterMethod.modality,
    getterMethod.visibility,
    setterMethod != null,
    overriddenProperty.name,
    getterMethod.source,
    null,
    CallableMemberDescriptor.Kind.DECLARATION,
    false,
    null
)
