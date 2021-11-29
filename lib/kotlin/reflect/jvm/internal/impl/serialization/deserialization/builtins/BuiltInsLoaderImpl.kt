/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins

import kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.PackageFragmentProvider
import kotlin.reflect.jvm.internal.impl.descriptors.PackageFragmentProviderImpl
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.ClassDescriptorFactory
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.resolve.sam.SamConversionResolverImpl
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.*
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import java.io.InputStream

class BuiltInsLoaderImpl : BuiltInsLoader {
    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
        additionalClassPartsProvider: AdditionalClassPartsProvider,
        isFallback: Boolean
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            StandardNames.BUILT_INS_PACKAGE_FQ_NAMES,
            classDescriptorFactories,
            platformDependentDeclarationFilter,
            additionalClassPartsProvider,
            isFallback,
            resourceLoader::loadResource
        )
    }

    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
        additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
        isFallback: Boolean,
        loadResource: (String) -> InputStream?
    ): PackageFragmentProvider {
        val packageFragments = packageFqNames.map { fqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
            val inputStream = loadResource(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
            BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream, isFallback)
        }
        val provider = PackageFragmentProviderImpl(packageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, module)

        val components = DeserializationComponents(
            storageManager,
            module,
            DeserializationConfiguration.Default,
            DeserializedClassDataFinder(provider),
            AnnotationAndConstantLoaderImpl(module, notFoundClasses, BuiltInSerializerProtocol),
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            classDescriptorFactories,
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            additionalClassPartsProvider,
            platformDependentDeclarationFilter,
            BuiltInSerializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList())
        )

        for (packageFragment in packageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }
}
