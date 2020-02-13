package io.saagie.updatarium

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Pierre Leresteux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import io.saagie.updatarium.config.UpdatariumConfiguration
import io.saagie.updatarium.model.Changelog
import io.saagie.updatarium.model.UpdatariumError
import mu.KotlinLogging
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Main class of Updatarium.
 *
 * Updatarium is initialized with a PersistEngine (if not, it uses the DefaultPersistEngine),
 * Then you can call the function 'executeChangelog' using a Path, a Reader or directly the script in String,
 * It will execute the changelog.
 */
class Updatarium(val configuration: UpdatariumConfiguration = UpdatariumConfiguration()) {
    private val logger = KotlinLogging.logger {}
    private val ktsLoader = KtsObjectLoader()

    fun executeChangelog(reader: Reader, tags: List<String> = emptyList()) {
        executeScript(reader.readText(), tags)
    }

    fun executeChangelog(script: String, tags: List<String> = emptyList()) {
        executeScript(script, tags)
    }

    fun executeChangelog(reader: Reader, tag: String) {
        this.executeChangelog(reader, listOf(tag))
    }

    fun executeChangelog(path: Path, tags: List<String> = emptyList()) {
        executeChangelog(Files.newBufferedReader(path), tags, path.toAbsolutePath().toString())
    }

    fun executeChangelog(path: Path, tag: String) {
        executeChangelog(Files.newBufferedReader(path), listOf(tag), path.toAbsolutePath().toString())
    }

    fun executeChangelog(script: String, tag: String) {
        executeChangelog(script, listOf(tag))
    }

    fun executeChangelogs(path: Path, pattern: String, tag: String) {
        executeChangelogs(path, pattern, listOf(tag))
    }

    fun executeChangelogs(path: Path, pattern: String, tags: List<String> = emptyList()) {
        if (!Files.isDirectory(path)) {
            logger.error { "$path is not a directory." }
            throw UpdatariumError.ExitError
        } else {
            val exceptions: MutableList<UpdatariumError.ExitError> = mutableListOf()
            path
                .toFile()
                .walk()
                .filter { it.name.matches(Regex(pattern)) }
                .sorted()
                .forEach {
                    try {
                        this.executeChangelog(it.toPath(), tags)
                    } catch (e: UpdatariumError.ExitError) {
                        if (configuration.failfast) {
                            throw e
                        } else {
                            exceptions.add(e)
                        }
                    }
                }
            if (exceptions.isNotEmpty()) {
                throw UpdatariumError.ExitError
            }
        }
    }

    fun executeChangelog(changelog: Changelog, tags: List<String> = emptyList()) {
        val result = changelog.execute(configuration, tags)
        result.changeSetExceptions.forEach { logger.error { it } }
        if (result.changeSetExceptions.isNotEmpty()) {
            throw UpdatariumError.ExitError
        }
    }

    private fun executeChangelog(reader: Reader, tags: List<String>, id: String) {
        val changeLog = ktsLoader.load<Changelog>(reader).let { loaded ->
            if (loaded.id.isEmpty()) loaded.copy(id = id) else loaded
        }
        executeChangelog(changeLog, tags)
    }

    private fun executeScript(script: String, tags: List<String>) {
        val changeLog = ktsLoader.load<Changelog>(script)
        executeChangelog(changeLog, tags)
    }
}

