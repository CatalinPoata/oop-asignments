/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.builtins.jvm

import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.types.TypeConstructorSubstitution
import kotlin.reflect.jvm.internal.impl.types.typeUtil.asTypeProjection

fun createMappedTypeParametersSubstitution(from: ClassDescriptor, to: ClassDescriptor): TypeConstructorSubstitution {
    assert(from.declaredTypeParameters.size == to.declaredTypeParameters.size) {
        "$from and $to should have same number of type parameters, " +
                "but ${from.declaredTypeParameters.size} / ${to.declaredTypeParameters.size} found"
    }

    return TypeConstructorSubstitution.createByConstructorsMap(
        from.declaredTypeParameters.map(TypeParameterDescriptor::getTypeConstructor).zip(
            to.declaredTypeParameters.map { it.defaultType.asTypeProjection() }
        ).toMap())
}
