/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.descriptors.impl

import kotlin.reflect.jvm.internal.impl.descriptors.FieldDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.PropertyDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotatedImpl
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations

class FieldDescriptorImpl(
    annotations: Annotations,
    override val correspondingProperty: PropertyDescriptor
) : FieldDescriptor, AnnotatedImpl(annotations)
