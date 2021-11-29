/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal.impl.serialization.deserialization

import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SourceElement
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf
import kotlin.reflect.jvm.internal.impl.metadata.deserialization.BinaryVersion
import kotlin.reflect.jvm.internal.impl.metadata.deserialization.NameResolverImpl
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

abstract class DeserializedPackageFragmentImpl(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    private val metadataVersion: BinaryVersion,
    private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    protected val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    override val classDataFinder =
        ProtoBasedClassDataFinder(proto, nameResolver, metadataVersion) { containerSource ?: SourceElement.NO_SOURCE }

    // Temporary storage: until `initialize` is called
    private var _proto: ProtoBuf.PackageFragment? = proto
    private lateinit var _memberScope: MemberScope

    override fun initialize(components: DeserializationComponents) {
        val proto = _proto ?: error("Repeated call to DeserializedPackageFragmentImpl::initialize")
        _proto = null
        _memberScope = DeserializedPackageMemberScope(
            this, proto.`package`, nameResolver, metadataVersion, containerSource, components,
            classNames = {
                classDataFinder.allClassIds.filter { classId ->
                    !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                }.map { it.shortClassName }
            }
        )
    }

    override fun getMemberScope(): MemberScope = _memberScope
}
