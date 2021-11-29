/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins

import kotlin.reflect.jvm.internal.impl.builtins.BuiltInsPackageFragment
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf
import kotlin.reflect.jvm.internal.impl.metadata.builtins.BuiltInsBinaryVersion
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializedPackageFragmentImpl
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import java.io.InputStream

class BuiltInsPackageFragmentImpl private constructor(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    metadataVersion: BuiltInsBinaryVersion,
    override val isFallback: Boolean
) : BuiltInsPackageFragment, DeserializedPackageFragmentImpl(
    fqName, storageManager, module, proto, metadataVersion, containerSource = null
) {
    companion object {
        fun create(
            fqName: FqName,
            storageManager: StorageManager,
            module: ModuleDescriptor,
            inputStream: InputStream,
            isFallback: Boolean
        ): BuiltInsPackageFragmentImpl {
            lateinit var version: BuiltInsBinaryVersion

            val proto = inputStream.use { stream ->
                version = BuiltInsBinaryVersion.readFrom(stream)

                if (!version.isCompatible()) {
                    // TODO: report a proper diagnostic
                    throw UnsupportedOperationException(
                        "Kotlin built-in definition format version is not supported: " +
                                "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                                "Please update Kotlin"
                    )
                }

                ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            }

            return BuiltInsPackageFragmentImpl(fqName, storageManager, module, proto, version, isFallback)
        }
    }
}
