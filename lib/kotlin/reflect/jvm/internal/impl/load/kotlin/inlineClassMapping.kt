/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.load.kotlin

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.resolve.substitutedUnderlyingType
import kotlin.reflect.jvm.internal.impl.resolve.unsubstitutedUnderlyingType
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.TypeUtils
import kotlin.reflect.jvm.internal.impl.types.typeUtil.representativeUpperBound

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        descriptor.representativeUpperBound
    else
        inlineClassType.substitutedUnderlyingType()
}

internal fun shouldUseUnderlyingType(inlineClassType: KotlinType): Boolean {
    val underlyingType = inlineClassType.unsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable ||
            !TypeUtils.isNullableType(underlyingType) && !KotlinBuiltIns.isPrimitiveType(underlyingType)
}
