/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve

import kotlin.reflect.jvm.internal.impl.container.DefaultImplementation
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.types.KotlinType

@DefaultImplementation(impl = AdditionalClassPartsProvider.Default::class)
interface AdditionalClassPartsProvider {
    fun generateAdditionalMethods(
        thisDescriptor: ClassDescriptor,
        result: MutableCollection<SimpleFunctionDescriptor>,
        name: Name,
        location: LookupLocation,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    )

    fun getAdditionalSupertypes(
        thisDescriptor: ClassDescriptor,
        existingSupertypes: List<KotlinType>
    ): List<KotlinType>

    object Default : AdditionalClassPartsProvider {
        override fun generateAdditionalMethods(
            thisDescriptor: ClassDescriptor,
            result: MutableCollection<SimpleFunctionDescriptor>,
            name: Name,
            location: LookupLocation,
            fromSupertypes: Collection<SimpleFunctionDescriptor>
        ) {}

        override fun getAdditionalSupertypes(
            thisDescriptor: ClassDescriptor,
            existingSupertypes: List<KotlinType>
        ): List<KotlinType> = emptyList()
    }
}
