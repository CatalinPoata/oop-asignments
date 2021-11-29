/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.descriptors

import kotlin.reflect.jvm.internal.impl.builtins.StandardNames.CONTINUATION_INTERFACE_FQ_NAME_EXPERIMENTAL
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.incremental.components.NoLookupLocation
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorUtils
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.module
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.KotlinTypeFactory
import kotlin.reflect.jvm.internal.impl.types.typeUtil.asTypeProjection
import kotlin.reflect.jvm.internal.impl.utils.sure
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
            .memberScope.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
            ?.unsubstitutedInnerClassesScope
            ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

fun ModuleDescriptor.findContinuationClassDescriptorOrNull(lookupLocation: LookupLocation, releaseCoroutines: Boolean) =
    if (releaseCoroutines)
        resolveClassByFqName(CONTINUATION_INTERFACE_FQ_NAME_RELEASE, lookupLocation)
    else
        resolveClassByFqName(CONTINUATION_INTERFACE_FQ_NAME_EXPERIMENTAL, lookupLocation)

fun ModuleDescriptor.findContinuationClassDescriptor(lookupLocation: LookupLocation, releaseCoroutines: Boolean) =
    findContinuationClassDescriptorOrNull(lookupLocation, releaseCoroutines).sure { "Continuation interface is not found" }

fun ModuleDescriptor.getContinuationOfTypeOrAny(kotlinType: KotlinType, isReleaseCoroutines: Boolean) =
    module.findContinuationClassDescriptorOrNull(
        NoLookupLocation.FROM_DESERIALIZATION,
        isReleaseCoroutines
    )?.defaultType?.let {
        KotlinTypeFactory.simpleType(
            it,
            arguments = listOf(kotlinType.asTypeProjection())
        )
    } ?: module.builtIns.nullableAnyType

fun DeclarationDescriptor.isTopLevelInPackage(name: String, packageName: String): Boolean {
    if (name != this.name.asString()) return false

    val containingDeclaration = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}

fun CallableDescriptor.isSupportedForCallableReference() = this is PropertyDescriptor || this is FunctionDescriptor

@OptIn(ExperimentalContracts::class)
fun DeclarationDescriptor.isSealed(): Boolean {
    contract {
        returns(true) implies (this@isSealed is ClassDescriptor)
    }
    return DescriptorUtils.isSealedClass(this)
}

fun DeclarationDescriptor.containingPackage(): FqName? {
    var container = containingDeclaration
    while (true) {
        if (container == null || container is PackageFragmentDescriptor) break
        container = container.containingDeclaration
    }
    require(container is PackageFragmentDescriptor?)
    return container?.fqName
}
