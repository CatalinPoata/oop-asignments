/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve

import kotlin.reflect.jvm.internal.impl.descriptors.ModuleCapability
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor

interface ResolutionAnchorProvider {
    fun getResolutionAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor?
}

val RESOLUTION_ANCHOR_PROVIDER_CAPABILITY = ModuleCapability<ResolutionAnchorProvider>("ResolutionAnchorProvider")

fun ModuleDescriptor.getResolutionAnchorIfAny(): ModuleDescriptor? =
    getCapability(RESOLUTION_ANCHOR_PROVIDER_CAPABILITY)?.getResolutionAnchor(this)
