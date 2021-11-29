/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.SourceElement
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf
import kotlin.reflect.jvm.internal.impl.metadata.builtins.BuiltInsBinaryVersion
import kotlin.reflect.jvm.internal.impl.metadata.deserialization.NameResolverImpl
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.sam.SamConversionResolver
import kotlin.reflect.jvm.internal.impl.resolve.scopes.ChainedMemberScope
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInSerializerProtocol
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.types.checker.NewKotlinTypeChecker
import java.io.InputStream

class MetadataPackageFragmentProvider(
    storageManager: StorageManager,
    finder: KotlinMetadataFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    private val metadataPartProvider: MetadataPartProvider,
    contractDeserializer: ContractDeserializer,
    kotlinTypeChecker: NewKotlinTypeChecker,
    samConversionResolver: SamConversionResolver
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            DeserializationConfiguration.Default, // TODO
            DeserializedClassDataFinder(this),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
            this,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            emptyList(),
            notFoundClasses,
            contractDeserializer,
            AdditionalClassPartsProvider.None, PlatformDependentDeclarationFilter.All,
            BuiltInSerializerProtocol.extensionRegistry,
            kotlinTypeChecker,
            samConversionResolver
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
        if (finder.hasMetadataPackage(fqName))
            MetadataPackageFragment(fqName, storageManager, moduleDescriptor, metadataPartProvider, finder)
        else null
}

class MetadataPackageFragment(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    private val metadataPartProvider: MetadataPartProvider,
    private val finder: KotlinMetadataFinder
) : DeserializedPackageFragment(fqName, storageManager, module) {
    override val classDataFinder = ClassDataFinder { classId ->
        val topLevelClassId = generateSequence(classId, ClassId::getOuterClassId).last()
        val stream = finder.findMetadata(topLevelClassId) ?: return@ClassDataFinder null
        val (message, nameResolver, version) = readProto(stream)
        message.class_List.firstOrNull { classProto ->
            nameResolver.getClassId(classProto.fqName) == classId
        }?.let { classProto ->
            ClassData(nameResolver, classProto, version, SourceElement.NO_SOURCE)
        }
    }

    private lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    private val memberScope = storageManager.createLazyValue { computeMemberScope() }

    private fun computeMemberScope(): MemberScope {
        // For each .kotlin_metadata file which represents a package part, add a separate deserialized scope
        // with top level callables and type aliases (but no classes) only from that part
        val packageParts = metadataPartProvider.findMetadataPackageParts(fqName.asString())
        val scopes = arrayListOf<DeserializedPackageMemberScope>()
        for (partName in packageParts) {
            val stream = finder.findMetadata(ClassId(fqName, Name.identifier(partName))) ?: continue
            val (proto, nameResolver, version) = readProto(stream)

            scopes.add(DeserializedPackageMemberScope(
                this, proto.`package`, nameResolver, version, containerSource = null, components = components,
                classNames = { emptyList() }
            ))
        }

        // Also add the deserialized scope that can load all classes from this package
        scopes.add(object : DeserializedPackageMemberScope(
            this, ProtoBuf.Package.getDefaultInstance(),
            NameResolverImpl(ProtoBuf.StringTable.getDefaultInstance(), ProtoBuf.QualifiedNameTable.getDefaultInstance()),
            BuiltInsBinaryVersion.INSTANCE, // Exact version does not matter here
            containerSource = null, components = components, classNames = { emptyList() }
        ) {
            override fun hasClass(name: Name): Boolean = hasTopLevelClass(name)
            override fun definitelyDoesNotContainName(name: Name) = false
            override fun getClassifierNames(): Set<Name>? = null
            override fun getNonDeclaredClassifierNames(): Set<Name>? = null
        })

        return ChainedMemberScope.create("Metadata scope", scopes)
    }

    override fun getMemberScope() = memberScope()

    override fun hasTopLevelClass(name: Name): Boolean {
        // TODO: check if the corresponding file exists
        return true
    }

    private fun readProto(stream: InputStream): Triple<ProtoBuf.PackageFragment, NameResolverImpl, BuiltInsBinaryVersion> {
        val version = BuiltInsBinaryVersion.readFrom(stream)

        if (!version.isCompatible()) {
            // TODO: report a proper diagnostic
            throw UnsupportedOperationException(
                "Kotlin metadata definition format version is not supported: " +
                        "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                        "Please update Kotlin"
            )
        }

        val message = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
        val nameResolver = NameResolverImpl(message.strings, message.qualifiedNames)
        return Triple(message, nameResolver, version)
    }

    companion object {
        const val METADATA_FILE_EXTENSION = "kotlin_metadata"
        const val DOT_METADATA_FILE_EXTENSION = ".$METADATA_FILE_EXTENSION"
    }
}
