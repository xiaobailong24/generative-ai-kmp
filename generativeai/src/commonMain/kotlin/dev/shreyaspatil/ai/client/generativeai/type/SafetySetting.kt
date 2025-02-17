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
 * A configuration for a [BlockThreshold] of some [HarmCategory] allowed and blocked in responses.
 *
 * @param harmCategory The relevant [HarmCategory].
 * @param threshold The threshold form harm allowable.
 */
class SafetySetting(val harmCategory: HarmCategory, val threshold: BlockThreshold)
