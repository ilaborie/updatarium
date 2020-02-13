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

import io.saagie.updatarium.model.action.Action
import mu.KotlinLogging

typealias Tag = String

class ChangeLogDsl(val id: String) {
    private var changeSetsDsl: MutableList<ChangeSetDsl> = mutableListOf()

    fun changeSet(id: String, author: String, block: ChangeSetDsl.() -> Unit) =
        this.changeSetsDsl.add(ChangeSetDsl(id, author).apply(block))

    internal fun build(): Changelog =
        Changelog(id, this.changeSetsDsl.map { it.build(id) })
}

class ChangeSetDsl(val id: String, val author: String) {
    private var actions: MutableList<ActionDsl> = mutableListOf()

    var tags: List<Tag> = emptyList()

    fun action(name: String = "basicAction", block: ActionDsl.() -> Unit) =
        this.actions.add(ActionDsl(name, block))

    internal fun build(changelogId: String): ChangeSet =
        ChangeSet(
            id = if (changelogId.isEmpty()) id else "${changelogId}_$id",
            author = author,
            tags = tags,
            actions = actions.map(ActionDsl::build)
        )
}

class ActionDsl(val name: String, val block: ActionDsl.() -> Unit) {
    val logger = KotlinLogging.logger(name)

    internal fun build(): Action = object : Action() {
        override fun execute() {
            block()
        }
    }
}

fun changeLog(id: String = "", block: ChangeLogDsl.() -> Unit): Changelog =
    ChangeLogDsl(id)
        .apply(block)
        .build()
