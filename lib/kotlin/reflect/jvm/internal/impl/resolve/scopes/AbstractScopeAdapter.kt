/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.impl.resolve.scopes

import kotlin.reflect.jvm.internal.impl.descriptors.ClassifierDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.PropertyDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.utils.Printer

/**
 * Introduces a simple wrapper for internal scope.
 */
abstract class AbstractScopeAdapter : MemberScope {
    protected abstract val workerScope: MemberScope

    fun getActualScope(): MemberScope =
            if (workerScope is AbstractScopeAdapter)
                (workerScope as AbstractScopeAdapter).getActualScope()
            else
                workerScope

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return workerScope.getContributedFunctions(name, location)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifier(name, location)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return workerScope.getContributedVariables(name, location)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return workerScope.getContributedDescriptors(kindFilter, nameFilter)
    }

    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.print("worker =")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}