/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve.sam

import kotlin.reflect.jvm.internal.impl.container.DefaultImplementation
import kotlin.reflect.jvm.internal.impl.container.PlatformSpecificExtension
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.types.SimpleType

val SAM_LOOKUP_NAME = Name.special("<SAM-CONSTRUCTOR>")

@DefaultImplementation(impl = SamConversionResolverImpl.SamConversionResolverWithoutReceiverConversion::class)
interface SamConversionResolver : PlatformSpecificExtension<SamConversionResolver> {
    object Empty : SamConversionResolver {
        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? = null
    }

    fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType?
}
