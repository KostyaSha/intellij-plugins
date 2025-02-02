// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.jhipster;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.jhipster.psi.JdlFile;
import org.jetbrains.annotations.NotNull;

final class JdlLiveTemplateContextType extends TemplateContextType {
  public JdlLiveTemplateContextType() {
    super("JDL");
  }

  @Override
  public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
    return templateActionContext.getFile() instanceof JdlFile;
  }
}
