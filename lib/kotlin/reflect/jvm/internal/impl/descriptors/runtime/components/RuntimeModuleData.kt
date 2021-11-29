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

package kotlin.reflect.jvm.internal.impl.descriptors.runtime.components

import kotlin.reflect.jvm.internal.impl.builtins.ReflectionTypes
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JvmBuiltIns
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.SupertypeLoopChecker
import kotlin.reflect.jvm.internal.impl.descriptors.impl.CompositePackageFragmentProvider
import kotlin.reflect.jvm.internal.impl.descriptors.impl.ModuleDescriptorImpl
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.java.AnnotationTypeQualifierResolver
import kotlin.reflect.jvm.internal.impl.load.java.JavaClassesTracker
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaPropertyInitializerEvaluator
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaResolverCache
import kotlin.reflect.jvm.internal.impl.load.java.components.SignaturePropagator
import kotlin.reflect.jvm.internal.impl.load.java.lazy.*
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.JavaTypeEnhancement
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.SignatureEnhancement
import kotlin.reflect.jvm.internal.impl.load.kotlin.*
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JavaDescriptorResolver
import kotlin.reflect.jvm.internal.impl.resolve.sam.SamConversionResolverImpl
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.ContractDeserializer
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationComponents
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationConfiguration
import kotlin.reflect.jvm.internal.impl.storage.LockBasedStorageManager
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.types.checker.NewKotlinTypeChecker
import kotlin.reflect.jvm.internal.impl.utils.JavaTypeEnhancementState

class RuntimeModuleData private constructor(
    val deserialization: DeserializationComponents,
    val packagePartScopeCache: PackagePartScopeCache
) {
    val module: ModuleDescriptor get() = deserialization.moduleDescriptor

    companion object {
        fun create(classLoader: ClassLoader): RuntimeModuleData {
            val storageManager = LockBasedStorageManager("RuntimeModuleData")
            val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
            val module = ModuleDescriptorImpl(Name.special("<runtime module for $classLoader>"), storageManager, builtIns)
            builtIns.builtInsModule = module

            builtIns.initialize(module, isAdditionalBuiltInsFeatureSupported = true)

            val reflectKotlinClassFinder = ReflectKotlinClassFinder(classLoader)
            val deserializedDescriptorResolver = DeserializedDescriptorResolver()
            val singleModuleClassResolver = SingleModuleClassResolver()
            val notFoundClasses = NotFoundClasses(storageManager, module)

            val lazyJavaPackageFragmentProvider =
                makeLazyJavaPackageFragmentFromClassLoaderProvider(
                    classLoader, module, storageManager, notFoundClasses,
                    reflectKotlinClassFinder, deserializedDescriptorResolver, singleModuleClassResolver
                )

            val deserializationComponentsForJava =
                makeDeserializationComponentsForJava(
                    module, storageManager, notFoundClasses, lazyJavaPackageFragmentProvider,
                    reflectKotlinClassFinder, deserializedDescriptorResolver
                )

            deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

            val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, JavaResolverCache.EMPTY)
            singleModuleClassResolver.resolver = javaDescriptorResolver

            // .kotlin_builtins files should be found by the same class loader that loaded stdlib classes
            val stdlibClassLoader = Unit::class.java.classLoader
            val builtinsProvider = JvmBuiltInsPackageFragmentProvider(
                storageManager, ReflectKotlinClassFinder(stdlibClassLoader), module, notFoundClasses, builtIns.customizer, builtIns.customizer,
                DeserializationConfiguration.Default, NewKotlinTypeChecker.Default, SamConversionResolverImpl(storageManager, emptyList())
            )

            module.setDependencies(module)
            module.initialize(CompositePackageFragmentProvider(listOf(javaDescriptorResolver.packageFragmentProvider, builtinsProvider)))

            return RuntimeModuleData(
                deserializationComponentsForJava.components,
                PackagePartScopeCache(deserializedDescriptorResolver, reflectKotlinClassFinder)
            )
        }
    }
}

fun makeLazyJavaPackageFragmentFromClassLoaderProvider(
    classLoader: ClassLoader,
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver,
    singleModuleClassResolver: ModuleClassResolver,
    packagePartProvider: PackagePartProvider = PackagePartProvider.Empty
): LazyJavaPackageFragmentProvider {
    val annotationTypeQualifierResolver = AnnotationTypeQualifierResolver(storageManager, JavaTypeEnhancementState.DISABLED_JSR_305)
    val javaTypeEnhancementState = JavaTypeEnhancementState.DISABLED_JSR_305
    val javaResolverComponents = JavaResolverComponents(
        storageManager, ReflectJavaClassFinder(classLoader), reflectKotlinClassFinder, deserializedDescriptorResolver,
        SignaturePropagator.DO_NOTHING, RuntimeErrorReporter, JavaResolverCache.EMPTY,
        JavaPropertyInitializerEvaluator.DoNothing, SamConversionResolverImpl(storageManager, emptyList()), RuntimeSourceElementFactory,
        singleModuleClassResolver, packagePartProvider, SupertypeLoopChecker.EMPTY, LookupTracker.DO_NOTHING, module,
        ReflectionTypes(module, notFoundClasses), annotationTypeQualifierResolver,
        SignatureEnhancement(annotationTypeQualifierResolver, JavaTypeEnhancementState.DISABLED_JSR_305, JavaTypeEnhancement(JavaResolverSettings.Default)),
        JavaClassesTracker.Default, JavaResolverSettings.Default, NewKotlinTypeChecker.Default, javaTypeEnhancementState
    )

    return LazyJavaPackageFragmentProvider(javaResolverComponents)
}

fun makeDeserializationComponentsForJava(
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    lazyJavaPackageFragmentProvider: LazyJavaPackageFragmentProvider,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver
): DeserializationComponentsForJava {
    val javaClassDataFinder = JavaClassDataFinder(reflectKotlinClassFinder, deserializedDescriptorResolver)
    val binaryClassAnnotationAndConstantLoader = BinaryClassAnnotationAndConstantLoaderImpl(
        module, notFoundClasses, storageManager, reflectKotlinClassFinder
    )
    return DeserializationComponentsForJava(
        storageManager, module, DeserializationConfiguration.Default, javaClassDataFinder,
        binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider, notFoundClasses,
        RuntimeErrorReporter, LookupTracker.DO_NOTHING, ContractDeserializer.DEFAULT, NewKotlinTypeChecker.Default
    )
}
