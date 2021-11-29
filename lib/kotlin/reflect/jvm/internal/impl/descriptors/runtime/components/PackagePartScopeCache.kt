/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.descriptors.runtime.components

import kotlin.reflect.jvm.internal.impl.descriptors.impl.EmptyPackageFragmentDescriptor
import kotlin.reflect.jvm.internal.impl.load.kotlin.DeserializedDescriptorResolver
import kotlin.reflect.jvm.internal.impl.load.kotlin.findKotlinClass
import kotlin.reflect.jvm.internal.impl.load.kotlin.header.KotlinClassHeader
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JvmClassName
import kotlin.reflect.jvm.internal.impl.resolve.scopes.ChainedMemberScope
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import java.util.concurrent.ConcurrentHashMap

class PackagePartScopeCache(private val resolver: DeserializedDescriptorResolver, private val kotlinClassFinder: ReflectKotlinClassFinder) {
    private val cache = ConcurrentHashMap<ClassId, MemberScope>()

    fun getPackagePartScope(fileClass: ReflectKotlinClass): MemberScope = cache.getOrPut(fileClass.classId) {
        val fqName = fileClass.classId.packageFqName

        val parts =
            if (fileClass.classHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS)
                fileClass.classHeader.multifilePartNames.mapNotNull { partName ->
                    val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
                    kotlinClassFinder.findKotlinClass(classId)
                }
            else listOf(fileClass)

        val packageFragment = EmptyPackageFragmentDescriptor(resolver.components.moduleDescriptor, fqName)

        val scopes = parts.mapNotNull { part ->
            resolver.createKotlinPackagePartScope(packageFragment, part)
        }.toList()

        ChainedMemberScope.create("package $fqName ($fileClass)", scopes)
    }
}
