/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.builtins.jvm

import kotlin.reflect.jvm.internal.impl.builtins.functions.BuiltInFictitiousFunctionClassFactory
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinClassFinder
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.resolve.sam.SamConversionResolver
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.*
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInSerializerProtocol
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.types.checker.NewKotlinTypeChecker

class JvmBuiltInsPackageFragmentProvider(
    storageManager: StorageManager,
    finder: KotlinClassFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    additionalClassPartsProvider: AdditionalClassPartsProvider,
    platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
    deserializationConfiguration: DeserializationConfiguration,
    kotlinTypeChecker: NewKotlinTypeChecker,
    samConversionResolver: SamConversionResolver
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            deserializationConfiguration,
            DeserializedClassDataFinder(this),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
            this,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            listOf(
                BuiltInFictitiousFunctionClassFactory(storageManager, moduleDescriptor),
                JvmBuiltInClassDescriptorFactory(storageManager, moduleDescriptor)
            ),
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            additionalClassPartsProvider, platformDependentDeclarationFilter,
            BuiltInSerializerProtocol.extensionRegistry,
            kotlinTypeChecker,
            samConversionResolver
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
        finder.findBuiltInsData(fqName)?.let { inputStream ->
            BuiltInsPackageFragmentImpl.create(fqName, storageManager, moduleDescriptor, inputStream, isFallback = false)
        }

    companion object {
        const val DOT_BUILTINS_METADATA_FILE_EXTENSION = ".kotlin_builtins"
    }
}
