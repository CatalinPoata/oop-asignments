/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.builtins.StandardNames
import kotlin.reflect.jvm.internal.impl.builtins.jvm.createMappedTypeParametersSubstitution
import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.descriptors.impl.ClassDescriptorBase
import kotlin.reflect.jvm.internal.impl.incremental.components.NoLookupLocation
import kotlin.reflect.jvm.internal.impl.load.java.FakePureImplementationsProvider
import kotlin.reflect.jvm.internal.impl.load.java.JavaDescriptorVisibilities
import kotlin.reflect.jvm.internal.impl.load.java.JvmAnnotationNames
import kotlin.reflect.jvm.internal.impl.load.java.components.JavaResolverCache
import kotlin.reflect.jvm.internal.impl.load.java.components.TypeUsage
import kotlin.reflect.jvm.internal.impl.load.java.descriptors.JavaClassDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaResolverContext
import kotlin.reflect.jvm.internal.impl.load.java.lazy.childForClassOrPackage
import kotlin.reflect.jvm.internal.impl.load.java.lazy.replaceComponents
import kotlin.reflect.jvm.internal.impl.load.java.lazy.resolveAnnotations
import kotlin.reflect.jvm.internal.impl.load.java.lazy.types.toAttributes
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClass
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClassifierType
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaType
import kotlin.reflect.jvm.internal.impl.load.java.toDescriptorVisibility
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.isValidJavaFqName
import kotlin.reflect.jvm.internal.impl.resolve.constants.StringValue
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.fqNameSafe
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.fqNameUnsafe
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.resolveTopLevelClass
import kotlin.reflect.jvm.internal.impl.resolve.scopes.InnerClassesScopeWrapper
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.types.*
import kotlin.reflect.jvm.internal.impl.types.checker.KotlinTypeRefiner
import kotlin.reflect.jvm.internal.impl.utils.addIfNotNull
import kotlin.reflect.jvm.internal.impl.utils.addToStdlib.safeAs
import java.util.*

class LazyJavaClassDescriptor(
    val outerContext: LazyJavaResolverContext,
    containingDeclaration: DeclarationDescriptor,
    val jClass: JavaClass,
    private val additionalSupertypeClassDescriptor: ClassDescriptor? = null
) : ClassDescriptorBase(
    outerContext.storageManager, containingDeclaration, jClass.name,
    outerContext.components.sourceElementFactory.source(jClass),
    /* isExternal = */ false
), JavaClassDescriptor {

    companion object {
        @JvmStatic
        private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")
    }

    private val c: LazyJavaResolverContext = outerContext.childForClassOrPackage(this, jClass)

    init {
        c.components.javaResolverCache.recordClass(jClass, this)

        assert(jClass.lightClassOriginKind == null) {
            "Creating LazyJavaClassDescriptor for light class $jClass"
        }
    }

    private val kind = when {
        jClass.isAnnotationType -> ClassKind.ANNOTATION_CLASS
        jClass.isInterface -> ClassKind.INTERFACE
        jClass.isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

    private val modality =
        if (jClass.isAnnotationType || jClass.isEnum) Modality.FINAL
        // TODO: replace false with jClass.isSealed when it will be properly supported in platform
        else Modality.convertFromFlags(sealed = false, jClass.isSealed || jClass.isAbstract || jClass.isInterface, !jClass.isFinal)

    private val visibility = jClass.visibility
    private val isInner = jClass.outerClass != null && !jClass.isStatic

    override fun getKind() = kind
    override fun getModality() = modality

    override fun isRecord(): Boolean = jClass.isRecord

    // To workaround a problem with Scala compatibility (KT-9700),
    // we consider private visibility of a Java top level class as package private
    // Shortly: Scala plugin introduces special kind of "private in package" classes
    // which can be inherited from the same package.
    // Kotlin considers this "private in package" just as "private" and thinks they are invisible for inheritors,
    // so their functions are invisible fake which is not true.
    override fun getVisibility(): DescriptorVisibility {
        return if (visibility == DescriptorVisibilities.PRIVATE && jClass.outerClass == null) {
            JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        } else {
            visibility.toDescriptorVisibility()
        }
    }

    override fun isInner() = isInner
    override fun isData() = false
    override fun isInline() = false
    override fun isCompanionObject() = false
    override fun isExpect() = false
    override fun isActual() = false
    override fun isFun() = false
    override fun isValue() = false

    private val typeConstructor = LazyJavaClassTypeConstructor()
    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    private val unsubstitutedMemberScope =
        LazyJavaClassMemberScope(c, this, jClass, skipRefinement = additionalSupertypeClassDescriptor != null)

    private val scopeHolder =
        ScopesHolderForClass.create(this, c.storageManager, c.components.kotlinTypeChecker.kotlinTypeRefiner) {
            LazyJavaClassMemberScope(
                c, this, jClass,
                skipRefinement = additionalSupertypeClassDescriptor != null,
                mainScope = unsubstitutedMemberScope
            )
        }

    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner) = scopeHolder.getScope(kotlinTypeRefiner)

    private val innerClassesScope = InnerClassesScopeWrapper(unsubstitutedMemberScope)
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = innerClassesScope

    private val staticScope = LazyJavaStaticClassScope(c, jClass, this)
    override fun getStaticScope(): MemberScope = staticScope

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null

    override fun getUnsubstitutedMemberScope() = super.getUnsubstitutedMemberScope() as LazyJavaClassMemberScope
    override fun getConstructors() = unsubstitutedMemberScope.constructors()

    override val annotations = c.resolveAnnotations(jClass)

    private val declaredParameters = c.storageManager.createLazyValue {
        jClass.typeParameters.map { p ->
            c.typeParameterResolver.resolveTypeParameter(p)
                ?: throw AssertionError("Parameter $p surely belongs to class $jClass, so it must be resolved")
        }
    }

    override fun getDeclaredTypeParameters() = declaredParameters()

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? =
        c.components.samConversionResolver.resolveFunctionTypeIfSamInterface(this)

    override fun isDefinitelyNotSamInterface(): Boolean {
        if (kind != ClassKind.INTERFACE) return true

        val candidates = jClass.methods.filter { it.isAbstract && it.typeParameters.isEmpty() }
        // From the definition of function interfaces in the Java specification (pt. 9.8):
        // "methods that are members of I that do not have the same signature as any public instance method of the class Object"
        // It means that if an interface declares `int hashCode()` then the method won't be taken into account when
        // checking if the interface is SAM.
        // We make here a conservative check just filtering out methods by name.
        // If we ignore a method with wrong signature (different from one in Object) it's not very bad,
        // we'll just say that the interface MAY BE a SAM when it's not and then more detailed check will be applied.
        if (candidates.count { it.name.identifier !in PUBLIC_METHOD_NAMES_IN_OBJECT } > 1) return true

        // If we have default methods the interface could be a SAM even while a super interface has more than one abstract method
        if (jClass.methods.any { !it.isAbstract && it.typeParameters.isEmpty() }) return false

        // Check if any of the super-interfaces contain too many methods to be a SAM
        return typeConstructor.supertypes.any {
            it.constructor.declarationDescriptor.safeAs<LazyJavaClassDescriptor>()?.isDefinitelyNotSamInterface == true
        }
    }

    // Checks if any part of compiler has requested scope content
    // It's necessary for IC to figure out if there is a need to track symbols in the class
    fun wasScopeContentRequested() =
        getUnsubstitutedMemberScope().wasContentRequested() || staticScope.wasContentRequested()

    override fun getSealedSubclasses(): Collection<ClassDescriptor> = if (modality == Modality.SEALED) {
        val attributes = TypeUsage.COMMON.toAttributes()
        jClass.permittedTypes.mapNotNull {
            c.typeResolver.transformJavaType(it, attributes).constructor.declarationDescriptor as? ClassDescriptor
        }
    } else {
        emptyList()
    }

    override fun toString() = "Lazy Java class ${this.fqNameUnsafe}"

    private inner class LazyJavaClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters = c.storageManager.createLazyValue {
            this@LazyJavaClassDescriptor.computeConstructorTypeParameters()
        }

        override fun getParameters(): List<TypeParameterDescriptor> = parameters()

        override fun computeSupertypes(): Collection<KotlinType> {
            val javaTypes = jClass.supertypes
            val result = ArrayList<KotlinType>(javaTypes.size)
            val incomplete = ArrayList<JavaType>(0)

            val purelyImplementedSupertype: KotlinType? = getPurelyImplementedSupertype()

            for (javaType in javaTypes) {
                val kotlinType = c.typeResolver.transformJavaType(javaType, TypeUsage.SUPERTYPE.toAttributes())
                val areImprovementsInStrictMode = c.components.settings.typeEnhancementImprovementsInStrictMode
                val enhancedKotlinType = if (areImprovementsInStrictMode) {
                    c.components.signatureEnhancement.enhanceSuperType(kotlinType, c)
                } else kotlinType

                if (enhancedKotlinType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor) {
                    incomplete.add(javaType)
                }

                if (enhancedKotlinType.constructor == purelyImplementedSupertype?.constructor) {
                    continue
                }

                if (!KotlinBuiltIns.isAnyOrNullableAny(enhancedKotlinType)) {
                    result.add(enhancedKotlinType)
                }
            }

            // Add fake supertype kotlin.collection.Collection<E> to java.util.Collection<E> class if needed
            // Only needed when calculating built-ins member scope
            result.addIfNotNull(
                additionalSupertypeClassDescriptor?.let {
                    createMappedTypeParametersSubstitution(it, this@LazyJavaClassDescriptor)
                        .buildSubstitutor().substitute(it.defaultType, Variance.INVARIANT)
                }
            )

            result.addIfNotNull(purelyImplementedSupertype)

            if (incomplete.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(declarationDescriptor, incomplete.map { javaType ->
                    (javaType as JavaClassifierType).presentableText
                })
            }

            return if (result.isNotEmpty()) result.toList() else listOf(c.module.builtIns.anyType)
        }

        private fun getPurelyImplementedSupertype(): KotlinType? {
            val annotatedPurelyImplementedFqName = getPurelyImplementsFqNameFromAnnotation()?.takeIf { fqName ->
                !fqName.isRoot && fqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
            }

            val purelyImplementedFqName =
                annotatedPurelyImplementedFqName
                    ?: FakePureImplementationsProvider.getPurelyImplementedInterface(fqNameSafe)
                    ?: return null

            val classDescriptor = c.module.resolveTopLevelClass(purelyImplementedFqName, NoLookupLocation.FROM_JAVA_LOADER) ?: return null

            val supertypeParameterCount = classDescriptor.typeConstructor.parameters.size
            val typeParameters = getTypeConstructor().parameters
            val typeParameterCount = typeParameters.size

            val parametersAsTypeProjections = when {
                typeParameterCount == supertypeParameterCount ->
                    typeParameters.map { parameter ->
                        TypeProjectionImpl(Variance.INVARIANT, parameter.defaultType)
                    }
                typeParameterCount == 1 && supertypeParameterCount > 1 && annotatedPurelyImplementedFqName == null -> {
                    val parameter = TypeProjectionImpl(Variance.INVARIANT, typeParameters.single().defaultType)
                    (1..supertypeParameterCount).map { parameter } // TODO: List(supertypeParameterCount) { parameter }
                }
                else -> return null
            }

            return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, classDescriptor, parametersAsTypeProjections)
        }

        private fun getPurelyImplementsFqNameFromAnnotation(): FqName? {
            val annotation =
                this@LazyJavaClassDescriptor.annotations.findAnnotation(JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION)
                    ?: return null

            val fqNameString = (annotation.allValueArguments.values.singleOrNull() as? StringValue)?.value ?: return null
            if (!isValidJavaFqName(fqNameString)) return null

            return FqName(fqNameString)
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.components.supertypeLoopChecker

        override fun isDenotable(): Boolean = true

        override fun getDeclarationDescriptor(): ClassDescriptor = this@LazyJavaClassDescriptor

        override fun toString(): String = name.asString()
    }

    // Only needed when calculating built-ins member scope
    internal fun copy(
        javaResolverCache: JavaResolverCache, additionalSupertypeClassDescriptor: ClassDescriptor?
    ): LazyJavaClassDescriptor = LazyJavaClassDescriptor(
        c.replaceComponents(c.components.replace(javaResolverCache = javaResolverCache)),
        containingDeclaration, jClass, additionalSupertypeClassDescriptor
    )
}
