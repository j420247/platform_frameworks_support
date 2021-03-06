/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import androidx.build.dokka.Dokka
import androidx.build.metalava.Metalava
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentModuleMetadataDetails
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportAndroidLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<AndroidXPlugin>()

        val supportLibraryExtension = project.extensions.create("supportLibrary",
                SupportLibraryExtension::class.java, project)
        project.configureMavenArtifactUpload(supportLibraryExtension)

        // Workaround for concurrentfuture
        project.dependencies.modules.module("com.google.guava:listenablefuture") {
            (it as ComponentModuleMetadataDetails).replacedBy(
                "com.google.guava:guava", "guava contains listenablefuture")
        }

        project.afterEvaluate {
            // workaround for b/120487939
            project.configurations.all {
                // Gradle seems to crash an androidtest configurations preferring project modules...
                if (!it.name.toLowerCase().contains("androidtest")) {
                    it.resolutionStrategy.preferProjectModules()
                }
            }
            if (supportLibraryExtension.publish) {
                project.extra.set("publish", true)
                project.addToProjectMap(supportLibraryExtension.mavenGroup?.group)
            }
            val library = project.extensions.findByType(LibraryExtension::class.java)
                    ?: return@afterEvaluate

            Dokka.registerAndroidProject(project, library, supportLibraryExtension)
            if (supportLibraryExtension.useMetalava) {
                Metalava.registerAndroidProject(project, library, supportLibraryExtension)
            } else {
                DiffAndDocs.get(project)
                    .registerAndroidProject(project, library, supportLibraryExtension)
            }

            if (supportLibraryExtension.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                        "Android libraries must use a compilation target of DEVICE")
            }

            library.libraryVariants.all { libraryVariant ->
                if (libraryVariant.getBuildType().getName().equals("debug")) {
                    libraryVariant.javaCompileProvider.configure { javaCompile ->
                        if (supportLibraryExtension.failOnUncheckedWarnings) {
                            javaCompile.options.compilerArgs.add("-Xlint:unchecked")
                        }
                        if (supportLibraryExtension.failOnDeprecationWarnings) {
                            javaCompile.options.compilerArgs.add("-Xlint:deprecation")
                        }
                    }
                }
            }
        }

        project.apply<LibraryPlugin>()

        val library = project.extensions.findByType(LibraryExtension::class.java)
                ?: throw Exception("Failed to find Android extension")

        project.configureLint(library.lintOptions, supportLibraryExtension)
    }
}
