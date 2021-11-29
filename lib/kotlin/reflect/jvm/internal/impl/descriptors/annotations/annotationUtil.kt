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

package kotlin.reflect.jvm.internal.impl.descriptors.annotations

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.constants.AnnotationValue
import kotlin.reflect.jvm.internal.impl.resolve.constants.ArrayValue
import kotlin.reflect.jvm.internal.impl.resolve.constants.EnumValue
import kotlin.reflect.jvm.internal.impl.resolve.constants.StringValue
import kotlin.reflect.jvm.internal.impl.types.Variance

fun KotlinBuiltIns.createDeprecatedAnnotation(
        message: String,
        replaceWith: String = "",
        level: String = "WARNING"
): AnnotationDescriptor {
    val replaceWithAnnotation = BuiltInAnnotationDescriptor(
        this,
        StandardNames.FqNames.replaceWith,
        mapOf(
                    REPLACE_WITH_EXPRESSION_NAME to StringValue(replaceWith),
                    REPLACE_WITH_IMPORTS_NAME to ArrayValue(emptyList()) { module ->
                        module.builtIns.getArrayType(Variance.INVARIANT, stringType)
                    }
            )
    )

    return BuiltInAnnotationDescriptor(
        this,
        StandardNames.FqNames.deprecated,
        mapOf(
                    DEPRECATED_MESSAGE_NAME to StringValue(message),
                    DEPRECATED_REPLACE_WITH_NAME to AnnotationValue(replaceWithAnnotation),
                    DEPRECATED_LEVEL_NAME to EnumValue(
                        ClassId.topLevel(StandardNames.FqNames.deprecationLevel),
                        Name.identifier(level)
                    )
            )
    )
}

private val DEPRECATED_MESSAGE_NAME = Name.identifier("message")
private val DEPRECATED_REPLACE_WITH_NAME = Name.identifier("replaceWith")
private val DEPRECATED_LEVEL_NAME = Name.identifier("level")
private val REPLACE_WITH_EXPRESSION_NAME = Name.identifier("expression")
private val REPLACE_WITH_IMPORTS_NAME = Name.identifier("imports")
