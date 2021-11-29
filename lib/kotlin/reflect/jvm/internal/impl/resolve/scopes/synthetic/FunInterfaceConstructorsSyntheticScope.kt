/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.resolve.scopes.synthetic

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.incremental.record
import kotlin.reflect.jvm.internal.impl.resolve.sam.*
import kotlin.reflect.jvm.internal.impl.resolve.scopes.SyntheticScope
import kotlin.reflect.jvm.internal.impl.resolve.scopes.SyntheticScopes
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.util.collectionUtils.filterIsInstanceMapNotNull

class FunInterfaceConstructorsScopeProvider(
    storageManager: StorageManager,
    lookupTracker: LookupTracker,
    samResolver: SamConversionResolver,
    samConversionOracle: SamConversionOracle
) : SyntheticScopes {
    override val scopes: Collection<SyntheticScope> = listOf(
        FunInterfaceConstructorsSyntheticScope(storageManager, lookupTracker, samResolver, samConversionOracle)
    )
}

class FunInterfaceConstructorsSyntheticScope(
    storageManager: StorageManager,
    private val lookupTracker: LookupTracker,
    private val samResolver: SamConversionResolver,
    private val samConversionOracle: SamConversionOracle
) : SyntheticScope.Default() {

    private val samConstructorForClassifier =
        storageManager.createMemoizedFunction<ClassDescriptor, SamConstructorDescriptor> { classifier ->
            createSamConstructorFunction(classifier.containingDeclaration, classifier, samResolver, samConversionOracle)
        }

    override fun getSyntheticConstructors(
        contributedClassifier: ClassifierDescriptor,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        recordSamLookupsToClassifier(contributedClassifier, location)

        return listOfNotNull(getSamConstructor(contributedClassifier))
    }

    override fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> =
        classifierDescriptors.filterIsInstanceMapNotNull<ClassifierDescriptor, FunctionDescriptor> { getSamConstructor(it) }

    private fun getSamConstructor(classifier: ClassifierDescriptor): SamConstructorDescriptor? {
        if (classifier is TypeAliasDescriptor) {
            return getTypeAliasSamConstructor(classifier)
        }

        val classDescriptor = checkIfClassifierApplicable(classifier) ?: return null
        return samConstructorForClassifier(classDescriptor)
    }

    private fun getTypeAliasSamConstructor(classifier: TypeAliasDescriptor): SamConstructorDescriptor? {
        val classDescriptor = checkIfClassifierApplicable(classifier.classDescriptor ?: return null) ?: return null

        return createTypeAliasSamConstructorFunction(
            classifier, samConstructorForClassifier(classDescriptor), samResolver, samConversionOracle
        )
    }

    private fun checkIfClassifierApplicable(classifier: ClassifierDescriptor): ClassDescriptor? {
        if (classifier !is ClassDescriptor) return null
        if (!classifier.isFun) return null
        if (getSingleAbstractMethodOrNull(classifier) == null) return null

        return classifier
    }

    private fun recordSamLookupsToClassifier(classifier: ClassifierDescriptor, location: LookupLocation) {
        if (classifier !is ClassDescriptor || !classifier.isFun) return
        lookupTracker.record(location, classifier, SAM_LOOKUP_NAME)
    }
}