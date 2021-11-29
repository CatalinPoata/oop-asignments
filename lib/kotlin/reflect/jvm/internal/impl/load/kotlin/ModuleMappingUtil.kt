/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.load.kotlin

import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmMetadataVersion
import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.ModuleMapping
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationConfiguration

fun ModuleMapping.Companion.loadModuleMapping(
    bytes: ByteArray?,
    debugName: String,
    configuration: DeserializationConfiguration,
    reportIncompatibleVersionError: (JvmMetadataVersion) -> Unit
): ModuleMapping =
    loadModuleMapping(
        bytes,
        debugName,
        configuration.skipMetadataVersionCheck,
        configuration.isJvmPackageNameSupported,
        reportIncompatibleVersionError
    )
