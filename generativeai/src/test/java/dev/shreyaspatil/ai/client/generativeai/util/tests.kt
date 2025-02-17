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
@file:Suppress("DEPRECATION") // a replacement for our purposes has not been published yet

package dev.shreyaspatil.ai.client.generativeai.util

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.internal.api.APIController
import dev.shreyaspatil.ai.client.generativeai.internal.api.GenerateContentRequest
import dev.shreyaspatil.ai.client.generativeai.internal.api.GenerateContentResponse
import dev.shreyaspatil.ai.client.generativeai.internal.api.JSON
import dev.shreyaspatil.ai.client.generativeai.internal.api.server.Candidate
import dev.shreyaspatil.ai.client.generativeai.internal.api.shared.Content
import dev.shreyaspatil.ai.client.generativeai.internal.api.shared.TextPart
import dev.shreyaspatil.ai.client.generativeai.internal.util.SSE_SEPARATOR
import dev.shreyaspatil.ai.client.generativeai.internal.util.send
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.File

internal fun prepareStreamingResponse(response: List<GenerateContentResponse>): List<ByteArray> =
    response.map { "data: ${JSON.encodeToString(it)}$SSE_SEPARATOR".toByteArray() }

internal fun prepareResponse(response: GenerateContentResponse) =
    JSON.encodeToString(response).toByteArray()

internal fun createRequest(vararg text: String): GenerateContentRequest {
    val contents = text.map { Content(parts = listOf(TextPart(it))) }

    return GenerateContentRequest("gemini", contents)
}

internal fun createResponse(text: String) = createResponses(text).single()

internal fun createResponses(vararg text: String): List<GenerateContentResponse> {
    val candidates = text.map { Candidate(Content(parts = listOf(TextPart(it)))) }

    return candidates.map { GenerateContentResponse(candidates = listOf(it)) }
}

/**
 * Wrapper around common instances needed in tests.
 *
 * @param channel A [ByteChannel] for sending responses through the mock HTTP engine
 * @param model A [GenerativeModel] that consumes the [channel]
 * @see commonTest
 * @see send
 */
internal data class CommonTestScope(val channel: ByteChannel, val model: GenerativeModel)

/** A test that runs under a [CommonTestScope]. */
internal typealias CommonTest = suspend CommonTestScope.() -> Unit

/**
 * Common test block for providing a [CommonTestScope] during tests.
 *
 * Example usage:
 * ```
 * @Test
 * fun `(generateContent) generates a proper response`() = commonTest {
 *   val request = createRequest("say something nice")
 *   val response = createResponse("The world is a beautiful place!")
 *
 *   channel.send(prepareResponse(response))
 *
 *   withTimeout(testTimeout) {
 *     val data = controller.generateContent(request)
 *     data.candidates.shouldNotBeEmpty()
 *   }
 * }
 * ```
 *
 * @param status An optional [HttpStatusCode] to return as a response
 * @param block The test contents themselves, with the [CommonTestScope] implicitly provided
 * @see CommonTestScope
 */
internal fun commonTest(status: HttpStatusCode = HttpStatusCode.OK, block: CommonTest) =
    doBlocking {
        val channel = ByteChannel(autoFlush = true)
        val mockEngine = MockEngine {
            respond(channel, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val controller = APIController("super_cool_test_key", "gemini-pro", mockEngine)
        val model = GenerativeModel("gemini-pro", "super_cool_test_key", controller = controller)
        CommonTestScope(channel, model).block()
    }

/**
 * A variant of [commonTest] for performing *streaming-based* snapshot tests.
 *
 * Loads the *Golden File* and automatically parses the messages from it; providing it to the
 * channel.
 *
 * @param name The name of the *Golden File* to load
 * @param httpStatusCode An optional [HttpStatusCode] to return as a response
 * @param block The test contents themselves, with a [CommonTestScope] implicitly provided
 * @see goldenUnaryFile
 */
internal fun goldenStreamingFile(
    name: String,
    httpStatusCode: HttpStatusCode = HttpStatusCode.OK,
    block: CommonTest,
) = doBlocking {
    val goldenFile = loadGoldenFile("streaming/$name")
    val messages = goldenFile.readLines().filter { it.isNotBlank() }

    commonTest(httpStatusCode) {
        launch {
            for (message in messages) {
                channel.writeFully("$message$SSE_SEPARATOR".toByteArray())
            }
            channel.close()
        }

        block()
    }
}

/**
 * A variant of [commonTest] for performing snapshot tests.
 *
 * Loads the *Golden File* and automatically provides it to the channel.
 *
 * @param name The name of the *Golden File* to load
 * @param httpStatusCode An optional [HttpStatusCode] to return as a response
 * @param block The test contents themselves, with a [CommonTestScope] implicitly provided
 * @see goldenStreamingFile
 */
internal fun goldenUnaryFile(
    name: String,
    httpStatusCode: HttpStatusCode = HttpStatusCode.OK,
    block: CommonTest,
) =
    commonTest(httpStatusCode) {
        val goldenFile = loadGoldenFile("unary/$name")
        val message = goldenFile.readText()

        channel.send(message.toByteArray())

        block()
    }

/**
 * Loads a *Golden File* from the resource directory.
 *
 * Expects golden files to live under `golden-files` in the resource files.
 *
 * @see goldenUnaryFile
 */
internal fun loadGoldenFile(path: String): File = loadResourceFile("golden-files/$path")

/** Loads a file from the test resources directory. */
internal fun loadResourceFile(path: String) = File("src/test/resources/$path")
