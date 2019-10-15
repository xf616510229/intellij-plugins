// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.lang.dart.ide.runner.server.webdev;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Consumer;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.DartRelativePathsConsoleFilter;
import com.jetbrains.lang.dart.ide.runner.server.OpenDartObservatoryUrlAction;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DartWebdevRunningState extends CommandLineState {

  @NotNull protected DartWebdevParameters myDartWebdevParameters;

  private final Collection<Consumer<String>> myObservatoryUrlConsumers = new ArrayList<>();

  public DartWebdevRunningState(final @NotNull ExecutionEnvironment env) throws ExecutionException {
    super(env);
    myDartWebdevParameters = ((DartWebdevConfiguration)env.getRunProfile()).getParameters().clone();

    final Project project = env.getProject();
    try {
      myDartWebdevParameters.check(project);
    }
    catch (RuntimeConfigurationError e) {
      throw new ExecutionException(e);
    }

    final TextConsoleBuilder builder = getConsoleBuilder();
    if (builder instanceof TextConsoleBuilderImpl) {
      ((TextConsoleBuilderImpl)builder).setUsePredefinedMessageFilter(false);
    }

    builder.addFilter(new DartRelativePathsConsoleFilter(project, myDartWebdevParameters.computeProcessWorkingDirectory(project)));
    DartWebdevConsoleView.install(env.getProject(), this);
  }

  public void addObservatoryUrlConsumer(@NotNull final Consumer<String> consumer) {
    myObservatoryUrlConsumers.add(consumer);
  }

  @NotNull
  @Override
  protected AnAction[] createActions(final ConsoleView console, final ProcessHandler processHandler, final Executor executor) {
    // These actions are effectively added only to the Run tool window. For Debug see DartCommandLineDebugProcess.registerAdditionalActions()
    final List<AnAction> actions = new ArrayList<>(Arrays.asList(super.createActions(console, processHandler, executor)));
    addObservatoryActions(actions, processHandler);
    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  private void addObservatoryActions(List<AnAction> actions, final ProcessHandler processHandler) {
    actions.add(new Separator());

    final OpenDartObservatoryUrlAction openObservatoryAction =
      new OpenDartObservatoryUrlAction(null, () -> !processHandler.isProcessTerminated());
    addObservatoryUrlConsumer(openObservatoryAction::setUrl);

    actions.add(openObservatoryAction);
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    final GeneralCommandLine commandLine = createCommandLine();

    final OSProcessHandler processHandler = new ColoredProcessHandler(commandLine);

    ProcessTerminatedListener.attach(processHandler, getEnvironment().getProject());
    return processHandler;
  }

  private GeneralCommandLine createCommandLine() throws ExecutionException {
    final DartSdk sdk = DartSdk.getDartSdk(getEnvironment().getProject());
    if (sdk == null) {
      throw new ExecutionException(DartBundle.message("dart.sdk.is.not.configured"));
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine()
      .withWorkDirectory(myDartWebdevParameters.computeProcessWorkingDirectory(getEnvironment().getProject()));
    commandLine.setCharset(StandardCharsets.UTF_8);

    commandLine.setExePath(FileUtil.toSystemDependentName(DartSdkUtil.getPubPath(sdk)));
    commandLine.addParameters("global", "run", "webdev", "daemon");
    if (myDartWebdevParameters.getWebdevPort() != -1) {
      // TODO served folder name based on HTML file path
      String folderToServe = "web";
      commandLine.addParameter(folderToServe + ":" + myDartWebdevParameters.getWebdevPort());
    }

    return commandLine;
  }
}
