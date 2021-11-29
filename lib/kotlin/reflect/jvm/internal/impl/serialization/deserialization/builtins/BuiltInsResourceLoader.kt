/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins

import java.io.InputStream

class BuiltInsResourceLoader {
    fun loadResource(path: String): InputStream? {
        return this::class.java.classLoader?.getResourceAsStream(path)
                ?: ClassLoader.getSystemResourceAsStream(path)
    }
}