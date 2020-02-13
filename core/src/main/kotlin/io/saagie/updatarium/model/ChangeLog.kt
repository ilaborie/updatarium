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
package io.saagie.updatarium.model

import io.saagie.updatarium.config.UpdatariumConfiguration
import io.saagie.updatarium.log.InMemoryAppenderManager
import io.saagie.updatarium.model.UpdatariumError.ChangeSetError
import mu.KLoggable

data class Changelog(val id: String = "", val changeSets: List<ChangeSet> = emptyList()) : KLoggable {
    override val logger = logger()

    /**
     * It will execute each changeSets present in this changelog sequentially and return the list of ChangeSet
     * exceptions (if not failFast)
     */
    fun execute(
        configuration: UpdatariumConfiguration,
        tags: List<String> = emptyList()
    ): ChangeLogReport {
        val exceptions: MutableList<ChangeSetError> = mutableListOf()
        configuration.persistEngine.checkConnection()
        InMemoryAppenderManager.setup(persistConfig = configuration.persistEngine.configuration)
        matchedChangeSets(tags).forEach {
            exceptions.addAll(it.execute(configuration))
            if (configuration.failfast && exceptions.isNotEmpty()) {
                return ChangeLogReport(exceptions)
            }
        }
        InMemoryAppenderManager.tearDown()
        return ChangeLogReport(exceptions)
    }

    /**
     * filter the changeSets that matched with the targetTags
     * return all changeSets if the targetTags list is empty
     * return only the matched changeSets (at least one tag matched)
     */
    fun matchedChangeSets(targetTags: List<String> = emptyList()) =
        this.changeSets
            .filter { targetTags.isEmpty() || targetTags.intersect(it.tags).isNotEmpty() }
}

data class ChangeLogReport(
    val changeSetExceptions: List<ChangeSetError>
)
