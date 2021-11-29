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

package kotlin.reflect.jvm.internal.impl.types

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.descriptors.ClassifierDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.resolve.scopes.TypeIntersectionScope
import kotlin.reflect.jvm.internal.impl.types.checker.KotlinTypeRefiner
import kotlin.reflect.jvm.internal.impl.types.model.IntersectionTypeConstructorMarker
import kotlin.reflect.jvm.internal.impl.types.refinement.TypeRefinement
import java.util.*

class IntersectionTypeConstructor(typesToIntersect: Collection<KotlinType>) : TypeConstructor, IntersectionTypeConstructorMarker {
    private var alternative: KotlinType? = null

    private constructor(
        typesToIntersect: Collection<KotlinType>,
        alternative: KotlinType?,
    ) : this(typesToIntersect) {
        this.alternative = alternative
    }

    init {
        assert(!typesToIntersect.isEmpty()) { "Attempt to create an empty intersection" }
    }

    private val intersectedTypes = LinkedHashSet(typesToIntersect)
    private val hashCode = intersectedTypes.hashCode()

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> = intersectedTypes

    // Type should not be rendered in scope's debug name. This may cause performance issues in case of complicated intersection types. 
    fun createScopeForKotlinType(): MemberScope =
        TypeIntersectionScope.create("member scope for intersection type", intersectedTypes)

    override fun isFinal(): Boolean = false

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): KotlinBuiltIns =
        intersectedTypes.iterator().next().constructor.builtIns

    override fun toString(): String =
        makeDebugNameForIntersectionType(intersectedTypes)

    private fun makeDebugNameForIntersectionType(resultingTypes: Iterable<KotlinType>): String =
        resultingTypes.sortedBy { it.toString() }.joinToString(separator = " & ", prefix = "{", postfix = "}")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntersectionTypeConstructor) return false

        return intersectedTypes == other.intersectedTypes
    }

    @OptIn(TypeRefinement::class)
    fun createType(): SimpleType =
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, this, listOf(), false, this.createScopeForKotlinType()
        ) { kotlinTypeRefiner ->
            this.refine(kotlinTypeRefiner).createType()
        }

    override fun hashCode(): Int = hashCode

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        transformComponents { it.refine(kotlinTypeRefiner) } ?: this

    fun setAlternative(alternative: KotlinType?): IntersectionTypeConstructor {
        return IntersectionTypeConstructor(intersectedTypes, alternative)
    }

    fun getAlternativeType(): KotlinType? = alternative
}

inline fun IntersectionTypeConstructor.transformComponents(
    predicate: (KotlinType) -> Boolean = { true },
    transform: (KotlinType) -> KotlinType
): IntersectionTypeConstructor? {
    var changed = false
    val newSupertypes = supertypes.map {
        if (predicate(it)) {
            changed = true
            transform(it)
        } else {
            it
        }
    }

    if (!changed) return null

    val updatedAlternative = getAlternativeType()?.let { alternative ->
        if (predicate(alternative)) transform(alternative) else alternative
    }

    return IntersectionTypeConstructor(newSupertypes).setAlternative(updatedAlternative)
}
