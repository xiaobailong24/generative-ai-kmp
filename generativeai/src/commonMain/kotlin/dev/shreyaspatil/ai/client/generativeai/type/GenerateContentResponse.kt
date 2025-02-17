/*
 * Copyright 2024 Shreyas Patil
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
package dev.shreyaspatil.ai.client.generativeai.type

/**
 * Represents a response from the model.
 *
 * @property candidates a list of possible responses generated from the model
 * @property promptFeedback optional feedback for the given prompt. When streaming, it's only
 *   populated in the first response.
 */
class GenerateContentResponse(
    val candidates: List<Candidate>,
    val promptFeedback: PromptFeedback?,
) {
    /** Convenience field representing the first text part in the response, if it exists. */
    val text: String? by lazy { firstPartAs<TextPart>()?.text }

    private inline fun <reified T : Part> firstPartAs(): T? {
        if (candidates.isEmpty()) {
            warn("No candidates were found, but was asked to get a candidate.")
            return null
        }

        val (parts, otherParts) = candidates.first().content.parts.partition { it is T }
        val type = T::class.simpleName ?: "of the part type you asked for"

        if (parts.isEmpty()) {
            if (otherParts.isNotEmpty()) {
                warn(
                    "We didn't find any $type, but we did find other part types. Did you ask for the right type?",
                )
            }

            return null
        }

        if (parts.size > 1) {
            warn("Multiple $type were found, returning the first one.")
        } else if (otherParts.isNotEmpty()) {
            warn("Returning the only $type found, but other part types were present as well.")
        }

        return parts.first() as T
    }

    private fun warn(message: String) {
        dev.shreyaspatil.ai.client.generativeai.platform.Log.w("GenerateContentResponse", message)
    }
}
