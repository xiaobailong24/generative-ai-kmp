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

/** Represents the probability that some [HarmCategory] is applicable in a [SafetyRating]. */
enum class HarmProbability {
    /** A new and not yet supported value. */
    UNKNOWN,

    /** Probability for harm is unspecified. */
    UNSPECIFIED,

    /** Probability for harm is negligible. */
    NEGLIGIBLE,

    /** Probability for harm is low. */
    LOW,

    /** Probability for harm is medium. */
    MEDIUM,

    /** Probability for harm is high. */
    HIGH,
}
