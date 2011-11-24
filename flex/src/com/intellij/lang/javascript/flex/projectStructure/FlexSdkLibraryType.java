package com.intellij.lang.javascript.flex.projectStructure;

import com.intellij.lang.javascript.flex.library.FlexLibraryType;
import com.intellij.lang.javascript.flex.projectStructure.ui.FilteringLibraryRootsComponentDescriptor;
import com.intellij.lang.javascript.flex.sdk.FlexSdkType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.LibraryTypeService;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * User: ksafonov
 */
public class FlexSdkLibraryType extends LibraryType<FlexSdkProperties> {
  public static final LibraryKind<FlexSdkProperties> FLEX_SDK = LibraryKind.create("Flex SDK");

  protected FlexSdkLibraryType() {
    super(FLEX_SDK);
  }

  @Override
  public String getCreateActionName() {
    return "Flex SDK";
  }

  @Override
  public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent,
                                                  @Nullable VirtualFile contextDirectory,
                                                  @NotNull Project project) {
    return LibraryTypeService.getInstance().createLibraryFromFiles(createLibraryRootsComponentDescriptor(), parentComponent, contextDirectory, this, project);
  }

  @NotNull
  @Override
  public FlexSdkProperties createDefaultProperties() {
    return new FlexSdkProperties();
  }

  @Override
  public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<FlexSdkProperties> flexSdkPropertiesLibraryEditorComponent) {
    return null;
  }

  @Override
  public Icon getIcon() {
    return FlexSdkType.getInstance().getIcon();
  }

  @Override
  public boolean isSuitableModule(@NotNull Module module, @NotNull FacetsProvider facetsProvider) {
    // should not be added manually
    return false;
  }

  public static FlexSdkLibraryType getInstance() {
    return LibraryType.EP_NAME.findExtension(FlexSdkLibraryType.class);
  }

  @Override
  @NotNull
  public LibraryRootsComponentDescriptor createLibraryRootsComponentDescriptor() {
    return new FilteringLibraryRootsComponentDescriptor(
      FlexLibraryType.getInstance().createLibraryRootsComponentDescriptor(), FlexSdk.EDITABLE_ROOT_TYPES);
  }
}
