/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.builtins.functions

import kotlin.reflect.jvm.internal.impl.builtins.FunctionInterfacePackageFragment
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.ClassDescriptorFactory
import kotlin.reflect.jvm.internal.impl.descriptors.impl.PackageFragmentDescriptorImpl
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.scopes.DescriptorKindFilter
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScopeImpl
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.utils.Printer

class FunctionInterfaceMemberScope(
    private val classDescriptorFactory: ClassDescriptorFactory,
    val packageName: FqName
) : MemberScopeImpl() {

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) =
        classDescriptorFactory.getAllContributedClassesIfPossible(packageName)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        emptyList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        emptyList()

    override fun getFunctionNames(): Set<Name> =
        emptySet()

    override fun getVariableNames(): Set<Name> =
        emptySet()

    override fun getClassifierNames(): Set<Name>? = null

    override fun printScopeStructure(p: Printer) {
        TODO()
    }

    private val classifiers = mutableMapOf<Name, ClassifierDescriptor>()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = when {
        classDescriptorFactory.shouldCreateClass(packageName, name) ->
            classifiers.getOrPut(name) {
                classDescriptorFactory.createClass(ClassId.topLevel(packageName.child(name)))!!
            }
        else -> null
    }
}

class FunctionInterfacePackageFragmentImpl(
    classDescriptorFactory: ClassDescriptorFactory,
    module: ModuleDescriptor,
    name: FqName
) : FunctionInterfacePackageFragment,
    PackageFragmentDescriptorImpl(module, name) {
    private val memberScope = FunctionInterfaceMemberScope(classDescriptorFactory, fqName)
    override fun getMemberScope() = memberScope
}

fun functionInterfacePackageFragmentProvider(
    storageManager: StorageManager,
    module: ModuleDescriptor
): PackageFragmentProvider {
    val classFactory = BuiltInFictitiousFunctionClassFactory(storageManager, module)
    val fragments = listOf(
        KOTLIN_REFLECT_FQ_NAME,
        BUILT_INS_PACKAGE_FQ_NAME,
        COROUTINES_PACKAGE_FQ_NAME_RELEASE
    ).map { fqName ->
        FunctionInterfacePackageFragmentImpl(classFactory, module, fqName)
    }
    return PackageFragmentProviderImpl(fragments)
}
