package org.apache.arrow.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.glavo.mic.ModuleInfoCompiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "module-info-compile", defaultPhase = LifecyclePhase.COMPILE)
public class ModuleInfoCompilerPlugin extends AbstractMojo {
  /**
   * Source directories
   */
  @Parameter(defaultValue = "${project.compileSourceRoots}", property = "compileSourceRoots",
      required = true)
  private final List<String> compileSourceRoots = new ArrayList<>();

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    Optional<File> moduleInfoFile = findFirstModuleInfo(compileSourceRoots);
    if (moduleInfoFile.isPresent()) {
      // The compiled module-info.class file goes into target/classes/module-info/main
      Path outputDir = Path.of(project.getBuild().getOutputDirectory());

      outputDir.toFile().mkdirs();
      Path targetPath = outputDir.resolve("module-info.class");

      // Invoke the compiler,
      ModuleInfoCompiler compiler = new ModuleInfoCompiler();
      try (Reader reader = new InputStreamReader(Files.newInputStream(moduleInfoFile.get().toPath()),
          StandardCharsets.UTF_8);
           OutputStream output = Files.newOutputStream(targetPath)) {
        compiler.compile(reader, output);
      } catch (IOException ex) {
        throw new MojoExecutionException("Error compiling module-info.java", ex);
      }
    }
  }

  /**
   * Finds the first module-info.java file in the set of source directories.
   */
  private Optional<File> findFirstModuleInfo(List<String> sourceDirectories) {
    return sourceDirectories.stream().map(Path::of)
        .map(sourcePath ->
            sourcePath.toFile().listFiles(file ->
                file.getName().equals("module-info.java")))
        .filter(matchingFiles -> matchingFiles.length != 0)
        .map(matchingFiles -> matchingFiles[0])
        .findAny();
  }
}
