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

package kotlin.reflect.jvm.internal.impl.load.java

import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DescriptorVisibility
import kotlin.reflect.jvm.internal.impl.descriptors.Visibility
import kotlin.reflect.jvm.internal.impl.resolve.deprecation.Deprecation
import kotlin.reflect.jvm.internal.impl.resolve.deprecation.DeprecationLevelValue

class DeprecationCausedByFunctionN(override val target: DeclarationDescriptor) : Deprecation {
    override val deprecationLevel: DeprecationLevelValue
        get() = DeprecationLevelValue.ERROR
    override val message: String?
        get() = "Java members containing references to ${JavaToKotlinClassMap.FUNCTION_N_FQ_NAME} are not supported"
}

internal fun Visibility.toDescriptorVisibility(): DescriptorVisibility = JavaDescriptorVisibilities.toDescriptorVisibility(this)
