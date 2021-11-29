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

package kotlin.reflect.jvm.internal.impl.util

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.builtins.ReflectionTypes
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.FunctionDescriptor
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.builtIns
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.module
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.typeUtil.isSubtypeOf
import kotlin.reflect.jvm.internal.impl.types.typeUtil.makeNotNullable
import kotlin.reflect.jvm.internal.impl.util.MemberKindCheck.Member
import kotlin.reflect.jvm.internal.impl.util.MemberKindCheck.MemberOrExtension
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.ASSIGNMENT_OPERATIONS
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.COMPARE_TO
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.COMPONENT_REGEX
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.CONTAINS
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.DEC
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.EQUALS
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.GET
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.GET_VALUE
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.HAS_NEXT
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.INC
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.INVOKE
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.ITERATOR
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.NEXT
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.PROVIDE_DELEGATE
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.RANGE_TO
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.SET
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.SET_VALUE
import kotlin.reflect.jvm.internal.impl.util.OperatorNameConventions.SIMPLE_UNARY_OPERATION_NAMES
import kotlin.reflect.jvm.internal.impl.util.ReturnsCheck.*
import kotlin.reflect.jvm.internal.impl.util.ValueParameterCountCheck.NoValueParameters
import kotlin.reflect.jvm.internal.impl.util.ValueParameterCountCheck.SingleValueParameter

sealed class CheckResult(val isSuccess: Boolean) {
    class IllegalSignature(val error: String) : CheckResult(false)
    object IllegalFunctionName : CheckResult(false)
    object SuccessCheck : CheckResult(true)
}

interface Check {
    val description: String
    fun check(functionDescriptor: FunctionDescriptor): Boolean
    operator fun invoke(functionDescriptor: FunctionDescriptor): String? = if (!check(functionDescriptor)) description else null
}

sealed class MemberKindCheck(override val description: String) : Check {
    object MemberOrExtension : MemberKindCheck("must be a member or an extension function") {
        override fun check(functionDescriptor: FunctionDescriptor) =
                functionDescriptor.dispatchReceiverParameter != null || functionDescriptor.extensionReceiverParameter != null
    }
    object Member : MemberKindCheck("must be a member function") {
        override fun check(functionDescriptor: FunctionDescriptor) =
                functionDescriptor.dispatchReceiverParameter != null
    }
}

sealed class ValueParameterCountCheck(override val description: String) : Check {
    object NoValueParameters : ValueParameterCountCheck("must have no value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.isEmpty()
    }
    object SingleValueParameter : ValueParameterCountCheck("must have a single value parameter") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == 1
    }
    class AtLeast(val n: Int) : ValueParameterCountCheck("must have at least $n value parameter" + (if (n > 1) "s" else "")) {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size >= n
    }
    class Equals(val n: Int) : ValueParameterCountCheck("must have exactly $n value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == n
    }
}

private object NoDefaultAndVarargsCheck : Check {
    override val description = "should not have varargs or parameters with default values"
    override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.valueParameters.all { !it.declaresOrInheritsDefaultValue() && it.varargElementType == null }
}

private object IsKPropertyCheck : Check {
    override val description = "second parameter must be of type KProperty<*> or its supertype"
    override fun check(functionDescriptor: FunctionDescriptor): Boolean {
        val secondParameter = functionDescriptor.valueParameters[1]
        return ReflectionTypes.createKPropertyStarType(secondParameter.module)?.isSubtypeOf(secondParameter.type.makeNotNullable()) ?: false
    }
}

sealed class ReturnsCheck(val name: String, val type: KotlinBuiltIns.() -> KotlinType) : Check {
    override val description = "must return $name"
    override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.returnType == functionDescriptor.builtIns.type()

    object ReturnsBoolean : ReturnsCheck("Boolean", { booleanType })
    object ReturnsInt : ReturnsCheck("Int", { intType })
    object ReturnsUnit : ReturnsCheck("Unit", { unitType })
}

internal class Checks private constructor(
        val name: Name?,
        val regex: Regex?,
        val nameList: Collection<Name>?,
        val additionalCheck: (FunctionDescriptor) -> String?,
        vararg val checks: Check
) {
    fun isApplicable(functionDescriptor: FunctionDescriptor): Boolean {
        if (name != null && functionDescriptor.name != name) return false
        if (regex != null && !functionDescriptor.name.asString().matches(regex)) return false
        if (nameList != null && functionDescriptor.name !in nameList) return false
        return true
    }

    fun checkAll(functionDescriptor: FunctionDescriptor): CheckResult {
        for (check in checks) {
            val checkResult = check(functionDescriptor)
            if (checkResult != null) {
                return CheckResult.IllegalSignature(checkResult)
            }
        }

        val additionalCheckResult = additionalCheck(functionDescriptor)
        if (additionalCheckResult != null) {
            return CheckResult.IllegalSignature(additionalCheckResult)
        }

        return CheckResult.SuccessCheck
    }

    constructor(vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, null, null, additionalChecks, *checks)
    constructor(name: Name, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(name, null, null, additionalChecks, *checks)
    constructor(regex: Regex, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, regex, null, additionalChecks, *checks)
    constructor(nameList: Collection<Name>, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, null, nameList, additionalChecks, *checks)
}

abstract class AbstractModifierChecks {
    abstract internal val checks: List<Checks>

    inline fun ensure(cond: Boolean, msg: () -> String) = if (!cond) msg() else null

    fun check(functionDescriptor: FunctionDescriptor): CheckResult {
        for (check in checks) {
            if (!check.isApplicable(functionDescriptor)) continue
            return check.checkAll(functionDescriptor)
        }

        return CheckResult.IllegalFunctionName
    }
}

object OperatorChecks : AbstractModifierChecks() {
    override val checks = listOf(
            Checks(GET, MemberOrExtension, ValueParameterCountCheck.AtLeast(1)),
            Checks(SET, MemberOrExtension, ValueParameterCountCheck.AtLeast(2)) {
                val lastIsOk =
                    valueParameters.lastOrNull()?.let { !it.declaresOrInheritsDefaultValue() && it.varargElementType == null } == true
                ensure(lastIsOk) { "last parameter should not have a default value or be a vararg" }
            },
            Checks(GET_VALUE, MemberOrExtension, NoDefaultAndVarargsCheck, ValueParameterCountCheck.AtLeast(2), IsKPropertyCheck),
            Checks(SET_VALUE, MemberOrExtension, NoDefaultAndVarargsCheck, ValueParameterCountCheck.AtLeast(3), IsKPropertyCheck),
            Checks(PROVIDE_DELEGATE, MemberOrExtension, NoDefaultAndVarargsCheck, ValueParameterCountCheck.Equals(2), IsKPropertyCheck),
            Checks(INVOKE, MemberOrExtension),
            Checks(CONTAINS, MemberOrExtension, SingleValueParameter, NoDefaultAndVarargsCheck, ReturnsBoolean),
            Checks(ITERATOR, MemberOrExtension, NoValueParameters),
            Checks(NEXT, MemberOrExtension, NoValueParameters),
            Checks(HAS_NEXT, MemberOrExtension, NoValueParameters, ReturnsBoolean),
            Checks(RANGE_TO, MemberOrExtension, SingleValueParameter, NoDefaultAndVarargsCheck),
            Checks(EQUALS, Member) {
                fun DeclarationDescriptor.isAny() = this is ClassDescriptor && KotlinBuiltIns.isAny(this)
                ensure(containingDeclaration.isAny() || overriddenDescriptors.any { it.containingDeclaration.isAny() }) { "must override ''equals()'' in Any" }
            },
            Checks(COMPARE_TO, MemberOrExtension, ReturnsInt, SingleValueParameter, NoDefaultAndVarargsCheck),
            Checks(BINARY_OPERATION_NAMES, MemberOrExtension, SingleValueParameter, NoDefaultAndVarargsCheck),
            Checks(SIMPLE_UNARY_OPERATION_NAMES, MemberOrExtension, NoValueParameters),
            Checks(listOf(INC, DEC), MemberOrExtension) {
                val receiver = dispatchReceiverParameter ?: extensionReceiverParameter
                ensure(receiver != null && (returnType?.isSubtypeOf(receiver.type) ?: false)) {
                    "receiver must be a supertype of the return type"
                }
            },
            Checks(ASSIGNMENT_OPERATIONS, MemberOrExtension, ReturnsUnit, SingleValueParameter, NoDefaultAndVarargsCheck),
            Checks(COMPONENT_REGEX, MemberOrExtension, NoValueParameters)
    ) }

object InfixChecks : AbstractModifierChecks() {
    override val checks = listOf(
            Checks(MemberKindCheck.MemberOrExtension, SingleValueParameter, NoDefaultAndVarargsCheck))
}

fun FunctionDescriptor.isValidOperator() = isOperator && OperatorChecks.check(this).isSuccess