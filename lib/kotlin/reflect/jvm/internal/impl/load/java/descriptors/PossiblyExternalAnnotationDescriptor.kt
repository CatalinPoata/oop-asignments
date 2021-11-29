/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.load.java.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor

interface PossiblyExternalAnnotationDescriptor : AnnotationDescriptor {
    val isIdeExternalAnnotation: Boolean
}