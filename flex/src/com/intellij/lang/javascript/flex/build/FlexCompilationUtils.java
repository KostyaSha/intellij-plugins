package com.intellij.lang.javascript.flex.build;

import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexFacet;
import com.intellij.lang.javascript.flex.FlexUtils;
import com.intellij.lang.javascript.flex.projectStructure.FlexProjectLevelCompilerOptionsHolder;
import com.intellij.lang.javascript.flex.projectStructure.model.AirPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.AndroidPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.options.BCUtils;
import com.intellij.lang.javascript.flex.projectStructure.ui.CreateAirDescriptorTemplateDialog;
import com.intellij.lang.javascript.flex.sdk.AirMobileSdkType;
import com.intellij.lang.javascript.flex.sdk.AirSdkType;
import com.intellij.lang.javascript.flex.sdk.FlexSdkUtils;
import com.intellij.lang.javascript.flex.sdk.FlexmojosSdkType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.util.PathUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.text.StringTokenizer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.intellij.lang.javascript.flex.projectStructure.ui.CreateAirDescriptorTemplateDialog.AirDescriptorOptions;
import static com.intellij.lang.javascript.flex.projectStructure.ui.CreateHtmlWrapperTemplateDialog.*;

public class FlexCompilationUtils {

  public static final String SWF_MACRO = "${swf}";

  private static final String[] MACROS_TO_REPLACE =
    {SWF_MACRO, "${title}", "${application}", "${bgcolor}", "${width}", "${height}", VERSION_MAJOR_MACRO, VERSION_MINOR_MACRO,
      VERSION_REVISION_MACRO};

  private FlexCompilationUtils() {
  }

  static void deleteCacheForFile(final String filePath) throws IOException {
    final VirtualFile cacheFile = LocalFileSystem.getInstance().findFileByPath(filePath + ".cache");
    if (cacheFile != null) {
      final Ref<IOException> exceptionRef = new Ref<IOException>();

      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
              if (cacheFile.exists()) {
                try {
                  cacheFile.delete(this);
                }
                catch (IOException e) {
                  exceptionRef.set(e);
                }
              }
            }
          });
        }
      }, ProgressManager.getInstance().getProgressIndicator().getModalityState());

      if (!exceptionRef.isNull()) {
        throw exceptionRef.get();
      }
    }
  }

  static List<String> buildCommand(final List<String> compilerCommand,
                                   final List<VirtualFile> configFiles,
                                   final Module module,
                                   final FlexIdeBuildConfiguration bc) {
    final List<String> command = new ArrayList<String>(compilerCommand);
    for (VirtualFile configFile : configFiles) {
      command.add("-load-config=" + configFile.getPath());
    }

    final Sdk sdk = bc.getSdk();
    assert sdk != null;

    addAdditionalOptions(command, module, sdk.getHomePath(),
                         FlexProjectLevelCompilerOptionsHolder.getInstance(module.getProject()).getProjectLevelCompilerOptions()
                           .getAdditionalOptions());
    addAdditionalOptions(command, module, sdk.getHomePath(),
                         FlexBuildConfigurationManager.getInstance(module).getModuleLevelCompilerOptions().getAdditionalOptions());
    addAdditionalOptions(command, module, sdk.getHomePath(), bc.getCompilerOptions().getAdditionalOptions());

    return command;
  }

  private static void addAdditionalOptions(final List<String> command,
                                           final Module module,
                                           final String sdkHome,
                                           final String additionalOptions) {
    if (!StringUtil.isEmpty(additionalOptions)) {
      // TODO handle -option="path with spaces"
      for (final String s : StringUtil.split(additionalOptions, " ")) {
        command.add(FlexUtils.replacePathMacros(s, module, sdkHome));
      }
    }
  }

  static List<String> getMxmlcCompcCommand(final Project project, final Sdk flexSdk, final boolean isApp) {
    final List<String> command = new ArrayList<String>();

    final String className =
      isApp ? (FlexSdkUtils.isFlex4Sdk(flexSdk) ? "flex2.tools.Mxmlc" : "flex2.tools.Compiler") : "flex2.tools.Compc";

    String additionalClasspath = FileUtil.toSystemDependentName(FlexUtils.getPathToBundledJar("idea-flex-compiler-fix.jar"));
    if (!(flexSdk.getSdkType() instanceof FlexmojosSdkType)) {
      additionalClasspath += File.pathSeparator + FileUtil.toSystemDependentName(flexSdk.getHomePath() + "/lib/compc.jar");
    }

    command.addAll(FlexSdkUtils.getCommandLineForSdkTool(project, flexSdk, additionalClasspath, className, null));

    return command;
  }

  /**
   * returns <code>false</code> if compilation error found in output
   */
  static boolean handleCompilerOutput(final FlexCompilationManager compilationManager,
                                      final FlexCompilationTask task,
                                      final String output) {
    boolean failureDetected = false;
    final StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");

    while (tokenizer.hasMoreElements()) {
      final String text = tokenizer.nextElement();
      if (!StringUtil.isEmptyOrSpaces(text)) {

        final Matcher matcher = FlexCompilerHandler.errorPattern.matcher(text);

        if (matcher.matches()) {
          final String file = matcher.group(1);
          final String additionalInfo = matcher.group(2);
          final String line = matcher.group(3);
          final String column = matcher.group(4);
          final String type = matcher.group(5);
          final String message = matcher.group(6);

          final CompilerMessageCategory messageCategory =
            "Warning".equals(type) ? CompilerMessageCategory.WARNING : CompilerMessageCategory.ERROR;
          final VirtualFile relativeFile = VfsUtil.findRelativeFile(file, null);

          final StringBuilder fullMessage = new StringBuilder();
          if (relativeFile == null) fullMessage.append(file).append(": ");
          if (additionalInfo != null) fullMessage.append(additionalInfo).append(' ');
          fullMessage.append(message);

          compilationManager.addMessage(task, messageCategory, fullMessage.toString(), relativeFile != null ? relativeFile.getUrl() : null,
                                        line != null ? Integer.parseInt(line) : 0, column != null ? Integer.parseInt(column) : 0);
          failureDetected |= messageCategory == CompilerMessageCategory.ERROR;
        }
        else if (text.startsWith("Error: ") || text.startsWith("Exception in thread \"main\" ")) {
          final String updatedText = text.startsWith("Error: ") ? text.substring("Error: ".length()) : text;
          compilationManager.addMessage(task, CompilerMessageCategory.ERROR, updatedText, null, -1, -1);
          failureDetected = true;
        }
        else {
          compilationManager.addMessage(task, CompilerMessageCategory.INFORMATION, text, null, -1, -1);
        }
      }
    }

    return !failureDetected;
  }

  private static String getOutputSwfFileNameForCssFile(final Project project, final String cssFilePath) {
    final VirtualFile cssFile = LocalFileSystem.getInstance().findFileByPath(cssFilePath);
    final VirtualFile sourceRoot = cssFile == null
                                   ? null
                                   : ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(cssFile);
    final String relativePath = sourceRoot == null ? null : VfsUtilCore.getRelativePath(cssFile, sourceRoot, '/');
    final String cssFileName = cssFilePath.substring(FileUtil.toSystemIndependentName(cssFilePath).lastIndexOf("/") + 1);
    final String relativeFolder = relativePath == null ? "" : relativePath.substring(0, relativePath.lastIndexOf('/') + 1);
    return relativeFolder + FileUtil.getNameWithoutExtension(cssFileName) + ".swf";
  }

  static FlexBuildConfiguration createCssConfig(final FlexBuildConfiguration config, final String cssFilePath) {
    final FlexBuildConfiguration cssConfig = config.clone();
    cssConfig.setType(FlexBuildConfiguration.Type.Default);
    cssConfig.OUTPUT_FILE_NAME = getOutputSwfFileNameForCssFile(config.getModule().getProject(), cssFilePath);
    cssConfig.OUTPUT_TYPE = FlexBuildConfiguration.APPLICATION;
    cssConfig.CSS_FILES_LIST.clear();
    cssConfig.PATH_TO_SERVICES_CONFIG_XML = "";
    cssConfig.CONTEXT_ROOT = "";
    return cssConfig;
  }

  public static void ensureOutputFileWritable(final Project project, final String filePath) {
    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
    if (file != null && !file.isWritable()) {
      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        public void run() {
          ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file);
        }
      }, ModalityState.defaultModalityState());
    }
  }

  public static void performPostCompileActions(final @NotNull FlexIdeBuildConfiguration bc) throws FlexCompilerException {
    switch (bc.getTargetPlatform()) {
      case Web:
        if (bc.isUseHtmlWrapper()) {
          handleHtmlWrapper(bc);
        }
        break;
      case Desktop:
        handleAirDescriptor(bc, bc.getAirDesktopPackagingOptions());
        break;
      case Mobile:
        if (bc.getAndroidPackagingOptions().isEnabled()) {
          handleAirDescriptor(bc, bc.getAndroidPackagingOptions());
        }
        if (bc.getIosPackagingOptions().isEnabled()) {
          handleAirDescriptor(bc, bc.getIosPackagingOptions());
        }
        break;
    }
  }

  private static void handleHtmlWrapper(final FlexIdeBuildConfiguration bc) throws FlexCompilerException {
    final VirtualFile templateDir = LocalFileSystem.getInstance().findFileByPath(bc.getWrapperTemplatePath());
    if (templateDir == null || !templateDir.isDirectory()) {
      throw new FlexCompilerException(FlexBundle.message("html.wrapper.dir.not.found", bc.getWrapperTemplatePath()));
    }
    final VirtualFile templateFile = templateDir.findChild(HTML_WRAPPER_TEMPLATE_FILE_NAME);
    if (templateFile == null) {
      throw new FlexCompilerException(FlexBundle.message("no.index.template.html.file", bc.getWrapperTemplatePath()));
    }

    final String outputFilePath = bc.getOutputFilePath(true);
    final String outputFolderPath = PathUtil.getParentPath(outputFilePath);

    final VirtualFile outputDir = LocalFileSystem.getInstance().findFileByPath(outputFolderPath);
    if (outputDir == null || !outputDir.isDirectory()) {
      throw new FlexCompilerException(FlexBundle.message("output.folder.does.not.exist", outputFolderPath));
    }

    final Ref<FlexCompilerException> exceptionRef = new Ref<FlexCompilerException>();
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      public void run() {
        exceptionRef.set(ApplicationManager.getApplication().runWriteAction(new NullableComputable<FlexCompilerException>() {
          public FlexCompilerException compute() {
            for (VirtualFile file : templateDir.getChildren()) {
              if (file == templateFile) {
                final String wrapperText;
                try {
                  wrapperText = VfsUtil.loadText(file);
                }
                catch (IOException e) {
                  return new FlexCompilerException(FlexBundle.message("failed.to.load.file", file.getPath(), e.getMessage()));
                }

                if (!wrapperText.contains(SWF_MACRO)) {
                  return new FlexCompilerException(FlexBundle.message("no.swf.macro", FileUtil.toSystemDependentName(file.getPath())));
                }

                final String outputFileName = PathUtil.getFileName(outputFilePath);
                final String fixedText = replaceMacros(wrapperText, FileUtil.getNameWithoutExtension(outputFileName),
                                                       bc.getDependencies().getTargetPlayer());
                final String wrapperFileName = BCUtils.getWrapperFileName(bc);
                try {
                  FlexUtils.addFileWithContent(wrapperFileName, fixedText, outputDir);
                }
                catch (IOException e) {
                  return new FlexCompilerException(
                    FlexBundle.message("failed.to.create.file", wrapperFileName, outputDir.getPath(), e.getMessage()));
                }
              }
              else {
                try {
                  file.copy(this, outputDir, file.getName());
                }
                catch (IOException e) {
                  return new FlexCompilerException(FlexBundle.message("failed.to.copy.file", file.getName(), templateDir.getPath(),
                                                                      outputDir.getPath(), e.getMessage()));
                }
              }
            }
            return null;
          }
        }));
      }
    }, ModalityState.any());

    if (!exceptionRef.isNull()) {
      throw exceptionRef.get();
    }
  }

  private static String replaceMacros(final String wrapperText, final String outputFileName, final String targetPlayer) {
    final String[] versionParts = targetPlayer.split("[.]");
    final String major = versionParts.length >= 1 ? versionParts[0] : "0";
    final String minor = versionParts.length >= 2 ? versionParts[1] : "0";
    final String revision = versionParts.length >= 3 ? versionParts[2] : "0";
    final String[] replacement = {outputFileName, outputFileName, outputFileName, "#ffffff", "100%", "100%", major, minor, revision};
    return StringUtil.replace(wrapperText, MACROS_TO_REPLACE, replacement);
  }

  private static void handleAirDescriptor(final FlexIdeBuildConfiguration bc,
                                          final AirPackagingOptions packagingOptions) throws FlexCompilerException {
    if (packagingOptions.isUseGeneratedDescriptor()) {
      final boolean android = packagingOptions instanceof AndroidPackagingOptions;
      generateAirDescriptor(bc, BCUtils.getGeneratedAirDescriptorName(bc, packagingOptions), android);
    }
    else {
      copyAndFixCustomAirDescriptor(bc, packagingOptions.getCustomDescriptorPath());
    }
  }

  private static void generateAirDescriptor(final FlexIdeBuildConfiguration bc,
                                            final String descriptorFileName, final boolean android) throws FlexCompilerException {
    final Ref<FlexCompilerException> exceptionRef = new Ref<FlexCompilerException>();

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      public void run() {
        final Sdk sdk = bc.getSdk();
        assert sdk != null;

        final String outputFilePath = bc.getOutputFilePath(true);
        final String outputFolderPath = PathUtil.getParentPath(outputFilePath);
        final VirtualFile outputFolder = LocalFileSystem.getInstance().findFileByPath(outputFolderPath);
        if (outputFolder == null) {
          exceptionRef.set(new FlexCompilerException("Failed to generate AIR descriptor. Folder does not exist: " + outputFolderPath));
          return;
        }

        final String airVersion = FlexSdkUtils.getAirVersion(sdk.getVersionString());
        final String appId = fixApplicationId(bc.getMainClass());
        final String appName = StringUtil.getShortName(bc.getMainClass());

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            try {
              final AirDescriptorOptions descriptorOptions =
                new AirDescriptorOptions(airVersion, appId, appName, PathUtil.getFileName(outputFilePath), android);
              final String descriptorText = CreateAirDescriptorTemplateDialog.getAirDescriptorText(descriptorOptions);

              FlexUtils.addFileWithContent(descriptorFileName, descriptorText, outputFolder);
            }
            catch (IOException e) {
              exceptionRef.set(new FlexCompilerException("Failed to generate AIR descriptor: " + e.getMessage()));
            }
          }
        });
      }
    }, ModalityState.any());

    if (!exceptionRef.isNull()) {
      throw exceptionRef.get();
    }
  }

  public static String fixApplicationId(final String appId) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < appId.length(); i++) {
      final char ch = appId.charAt(i);
      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '-' || ch == '.') {
        builder.append(ch);
      }
    }
    return builder.toString();
  }

  private static void copyAndFixCustomAirDescriptor(final FlexIdeBuildConfiguration bc,
                                                    final String customDescriptorPath) throws FlexCompilerException {
    final VirtualFile descriptorTemplateFile = LocalFileSystem.getInstance().findFileByPath(customDescriptorPath);
    if (descriptorTemplateFile == null) {
      throw new FlexCompilerException("Custom AIR descriptor file not found: " + customDescriptorPath);
    }

    final String outputFilePath = bc.getOutputFilePath(true);
    final String outputFolderPath = PathUtil.getParentPath(outputFilePath);
    final VirtualFile outputFolder = LocalFileSystem.getInstance().findFileByPath(outputFolderPath);
    if (outputFolder == null) {
      throw new FlexCompilerException("Failed to copy AIR descriptor. Folder does not exist: " + outputFolderPath);
    }

    final Ref<FlexCompilerException> exceptionRef = new Ref<FlexCompilerException>();

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            try {
              final String content = fixInitialContent(descriptorTemplateFile, PathUtil.getFileName(outputFilePath));
              FlexUtils.addFileWithContent(descriptorTemplateFile.getName(), content, outputFolder);
            }
            catch (FlexCompilerException e) {
              exceptionRef.set(e);
            }
            catch (IOException e) {
              exceptionRef.set(new FlexCompilerException("Failed to copy AIR descriptor to output folder", null, -1, -1));
            }
          }
        });
      }
    }, ModalityState.any());

    if (!exceptionRef.isNull()) {
      throw exceptionRef.get();
    }
  }

  private static String fixInitialContent(final VirtualFile descriptorFile, final String swfName) throws FlexCompilerException {
    try {
      final Document document;
      try {
        document = JDOMUtil.loadDocument(descriptorFile.getInputStream());
      }
      catch (IOException e) {
        throw new FlexCompilerException("Failed to read AIR descriptor content: " + e.getMessage(), descriptorFile.getUrl(), -1, -1);
      }

      final Element rootElement = document.getRootElement();
      if (rootElement == null || !"application".equals(rootElement.getName())) {
        throw new FlexCompilerException("AIR descriptor file has incorrect root tag", descriptorFile.getUrl(), -1, -1);
      }

      Element initialWindowElement = rootElement.getChild("initialWindow", rootElement.getNamespace());
      if (initialWindowElement == null) {
        initialWindowElement = new Element("initialWindow", rootElement.getNamespace());
        rootElement.addContent(initialWindowElement);
      }

      Element contentElement = initialWindowElement.getChild("content", rootElement.getNamespace());
      if (contentElement == null) {
        contentElement = new Element("content", rootElement.getNamespace());
        initialWindowElement.addContent(contentElement);
      }

      contentElement.setText(swfName);

      return JDOMUtil.writeDocument(document, SystemProperties.getLineSeparator());
    }
    catch (JDOMException e) {
      throw new FlexCompilerException("AIR descriptor file has incorrect format: " + e.getMessage(), descriptorFile.getUrl(), -1, -1);
    }
  }
}
