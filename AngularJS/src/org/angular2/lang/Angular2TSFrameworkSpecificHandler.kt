// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.angular2.lang

import com.intellij.lang.javascript.frameworks.JSFrameworkSpecificHandler
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.types.JSTypeImpl

class Angular2TSFrameworkSpecificHandler: JSFrameworkSpecificHandler {

  override fun shouldPreserveAlias(type: JSType): Boolean =
    type is JSTypeImpl && type.typeText == "Signal" && type.sourceElement?.let { Angular2LangUtil.isAngular2Context(it)} == true

}