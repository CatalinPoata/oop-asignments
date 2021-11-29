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

package kotlin.reflect.jvm.internal.impl.load.java.lazy

import kotlin.reflect.jvm.internal.impl.builtins.ReflectionTypes
import kotlin.reflect.jvm.internal.impl.descriptors.ClassOrPackageFragmentDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SupertypeLoopChecker
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.java.*
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaPropertyInitializerEvaluator
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaResolverCache
import kotlin.reflect.jvm.internal.impl.load.java.components.SignaturePropagator
import kotlin.reflect.jvm.internal.impl.load.java.lazy.types.JavaTypeResolver
import kotlin.reflect.jvm.internal.impl.load.java.sources.JavaSourceElementFactory
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaTypeParameterListOwner
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.SignatureEnhancement
import kotlin.reflect.jvm.internal.impl.load.kotlin.DeserializedDescriptorResolver
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinClassFinder
import kotlin.reflect.jvm.internal.impl.load.kotlin.PackagePartProvider
import kotlin.reflect.jvm.internal.impl.resolve.sam.SamConversionResolver
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.ErrorReporter
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.types.checker.NewKotlinTypeChecker
import kotlin.reflect.jvm.internal.impl.utils.JavaTypeEnhancementState

class JavaResolverComponents(
    val storageManager: StorageManager,
    val finder: JavaClassFinder,
    val kotlinClassFinder: KotlinClassFinder,
    val deserializedDescriptorResolver: DeserializedDescriptorResolver,
    val signaturePropagator: SignaturePropagator,
    val errorReporter: ErrorReporter,
    val javaResolverCache: JavaResolverCache,
    val javaPropertyInitializerEvaluator: JavaPropertyInitializerEvaluator,
    val samConversionResolver: SamConversionResolver,
    val sourceElementFactory: JavaSourceElementFactory,
    val moduleClassResolver: ModuleClassResolver,
    val packagePartProvider: PackagePartProvider,
    val supertypeLoopChecker: SupertypeLoopChecker,
    val lookupTracker: LookupTracker,
    val module: ModuleDescriptor,
    val reflectionTypes: ReflectionTypes,
    val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver,
    val signatureEnhancement: SignatureEnhancement,
    val javaClassesTracker: JavaClassesTracker,
    val settings: JavaResolverSettings,
    val kotlinTypeChecker: NewKotlinTypeChecker,
    val javaTypeEnhancementState: JavaTypeEnhancementState
) {
    fun replace(
        javaResolverCache: JavaResolverCache = this.javaResolverCache
    ) = JavaResolverComponents(
        storageManager, finder, kotlinClassFinder, deserializedDescriptorResolver,
        signaturePropagator, errorReporter, javaResolverCache,
        javaPropertyInitializerEvaluator, samConversionResolver, sourceElementFactory,
        moduleClassResolver, packagePartProvider, supertypeLoopChecker, lookupTracker, module, reflectionTypes,
        annotationTypeQualifierResolver, signatureEnhancement, javaClassesTracker, settings,
        kotlinTypeChecker,
        javaTypeEnhancementState
    )
}

interface JavaResolverSettings {
    val isReleaseCoroutines: Boolean
    val correctNullabilityForNotNullTypeParameter: Boolean
    val typeEnhancementImprovementsInStrictMode: Boolean

    object Default : JavaResolverSettings {
        override val isReleaseCoroutines: Boolean
            get() = false

        override val correctNullabilityForNotNullTypeParameter: Boolean
            get() = false

        override val typeEnhancementImprovementsInStrictMode: Boolean
            get() = false
    }

    companion object {
        fun create(
            isReleaseCoroutines: Boolean,
            correctNullabilityForNotNullTypeParameter: Boolean,
            typeEnhancementImprovementsInStrictMode: Boolean
        ): JavaResolverSettings =
            object : JavaResolverSettings {
                override val isReleaseCoroutines get() = isReleaseCoroutines
                override val correctNullabilityForNotNullTypeParameter get() = correctNullabilityForNotNullTypeParameter
                override val typeEnhancementImprovementsInStrictMode get() = typeEnhancementImprovementsInStrictMode
            }
    }
}

class LazyJavaResolverContext internal constructor(
    val components: JavaResolverComponents,
    val typeParameterResolver: TypeParameterResolver,
    internal val delegateForDefaultTypeQualifiers: Lazy<JavaTypeQualifiersByElementType?>
) {
    constructor(
        components: JavaResolverComponents,
        typeParameterResolver: TypeParameterResolver,
        typeQualifiersComputation: () -> JavaTypeQualifiersByElementType?
    ) : this(components, typeParameterResolver, lazy(LazyThreadSafetyMode.NONE, typeQualifiersComputation))

    val defaultTypeQualifiers: JavaTypeQualifiersByElementType? by delegateForDefaultTypeQualifiers

    val typeResolver = JavaTypeResolver(this, typeParameterResolver)

    val storageManager: StorageManager
        get() = components.storageManager

    val module: ModuleDescriptor get() = components.module
}

fun LazyJavaResolverContext.child(
    typeParameterResolver: TypeParameterResolver
) = LazyJavaResolverContext(components, typeParameterResolver, delegateForDefaultTypeQualifiers)

fun LazyJavaResolverContext.computeNewDefaultTypeQualifiers(
    additionalAnnotations: Annotations
): JavaTypeQualifiersByElementType? {
    if (components.javaTypeEnhancementState.disabledDefaultAnnotations) return defaultTypeQualifiers

    val defaultQualifiers =
        additionalAnnotations.mapNotNull(this::extractDefaultNullabilityQualifier)

    if (defaultQualifiers.isEmpty()) return defaultTypeQualifiers

    val defaultQualifiersByType =
        defaultTypeQualifiers?.defaultQualifiers?.let(::QualifierByApplicabilityType)
            ?: QualifierByApplicabilityType(AnnotationQualifierApplicabilityType::class.java)

    var wasUpdate = false
    for (qualifier in defaultQualifiers) {
        for (applicabilityType in qualifier.qualifierApplicabilityTypes) {
            defaultQualifiersByType[applicabilityType] = qualifier
            wasUpdate = true
        }
    }

    return if (!wasUpdate) defaultTypeQualifiers else JavaTypeQualifiersByElementType(defaultQualifiersByType)
}

private fun LazyJavaResolverContext.extractDefaultNullabilityQualifier(
    annotationDescriptor: AnnotationDescriptor
): JavaDefaultQualifiers? {
    val typeQualifierResolver = components.annotationTypeQualifierResolver
    typeQualifierResolver.resolveQualifierBuiltInDefaultAnnotation(annotationDescriptor)?.let { return it }

    val (typeQualifier, applicability) =
        typeQualifierResolver.resolveTypeQualifierDefaultAnnotation(annotationDescriptor)
            ?: return null

    val jsr305State = typeQualifierResolver.resolveJsr305CustomState(annotationDescriptor)
        ?: typeQualifierResolver.resolveJsr305AnnotationState(typeQualifier)

    if (jsr305State.isIgnore) {
        return null
    }

    val areImprovementsInStrictMode = components.settings.typeEnhancementImprovementsInStrictMode

    val nullabilityQualifier =
        components.signatureEnhancement.extractNullability(typeQualifier, areImprovementsInStrictMode, typeParameterBounds = false)
            ?.copy(isForWarningOnly = jsr305State.isWarning) ?: return null

    return JavaDefaultQualifiers(nullabilityQualifier, applicability)
}

fun LazyJavaResolverContext.replaceComponents(
    components: JavaResolverComponents
) = LazyJavaResolverContext(components, typeParameterResolver, delegateForDefaultTypeQualifiers)

private fun LazyJavaResolverContext.child(
    containingDeclaration: DeclarationDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner?,
    typeParametersIndexOffset: Int = 0,
    delegateForTypeQualifiers: Lazy<JavaTypeQualifiersByElementType?>
) = LazyJavaResolverContext(
    components,
    typeParameterOwner?.let {
        LazyJavaTypeParameterResolver(this, containingDeclaration, it, typeParametersIndexOffset) }
        ?: typeParameterResolver,
    delegateForTypeQualifiers
)

fun LazyJavaResolverContext.childForMethod(
    containingDeclaration: DeclarationDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner,
    typeParametersIndexOffset: Int = 0
) = child(containingDeclaration, typeParameterOwner, typeParametersIndexOffset, delegateForDefaultTypeQualifiers)

fun LazyJavaResolverContext.childForClassOrPackage(
    containingDeclaration: ClassOrPackageFragmentDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner? = null,
    typeParametersIndexOffset: Int = 0
) = child(
    containingDeclaration, typeParameterOwner, typeParametersIndexOffset,
    lazy(LazyThreadSafetyMode.NONE) { computeNewDefaultTypeQualifiers(containingDeclaration.annotations) }
)

fun LazyJavaResolverContext.copyWithNewDefaultTypeQualifiers(
    additionalAnnotations: Annotations
) = when {
    additionalAnnotations.isEmpty() -> this
    else -> LazyJavaResolverContext(
        components, typeParameterResolver,
        lazy(LazyThreadSafetyMode.NONE) { computeNewDefaultTypeQualifiers(additionalAnnotations) }
    )
}
