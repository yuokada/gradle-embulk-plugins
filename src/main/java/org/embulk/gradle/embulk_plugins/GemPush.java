/*
 * Copyright 2019 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.gradle.embulk_plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;

class GemPush extends JavaExec {
    @Inject
    public GemPush() {
        super();

        final ObjectFactory objectFactory = this.getProject().getObjects();
        this.host = objectFactory.property(String.class);

        this.jrubyConfiguration = null;
    }

    @Override
    public void exec() {
        if ((!this.getHost().isPresent()) || this.getHost().get().isEmpty()) {
            throw new GradleException("`host` must be specified in `gemPush`.");
        }

        final Project project = this.getProject();
        final Logger logger = project.getLogger();

        final Gem gemTask = (Gem) project.getTasks().getByName("gem");
        final File archiveFile = gemTask.getArchiveFile().get().getAsFile();

        final FileCollection jrubyFiles = (FileCollection) this.jrubyConfiguration;
        this.setIgnoreExitValue(false);
        this.setWorkingDir(archiveFile.toPath().getParent().toFile());
        this.setClasspath(jrubyFiles);
        this.setMain("org.jruby.Main");

        final ArrayList<String> args = new ArrayList<>();
        args.add("-rjars/setup");
        args.add("-S");
        args.add("gem");
        args.add("push");
        args.add(archiveFile.toString());
        args.add("--verbose");
        this.setArgs(args);

        if (logger.isLifecycleEnabled()) {
            logger.lifecycle(args.stream().collect(Collectors.joining(" ", "Exec: `java org.jruby.Main ", "`")));
            logger.lifecycle(
                    jrubyFiles.getFiles().stream().map(File::toString).collect(Collectors.joining("],[", "Classpath: [", "]")));
        }

        final HashMap<String, Object> environments = new HashMap<>();
        environments.putAll(System.getenv());
        environments.putAll(this.getEnvironment());
        environments.put("RUBYGEMS_HOST", this.getHost().get());
        this.setEnvironment(environments);

        super.exec();

        logger.lifecycle("Exec `gem push` finished successfully.");
    }

    public Property<String> getHost() {
        return this.host;
    }

    void setJRubyConfiguration(final Configuration jrubyConfiguration) {
        this.jrubyConfiguration = jrubyConfiguration;
    }

    private final Property<String> host;

    private Configuration jrubyConfiguration;
}
