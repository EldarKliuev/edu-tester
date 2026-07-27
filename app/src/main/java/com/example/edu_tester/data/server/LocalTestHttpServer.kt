package com.example.edu_tester.data.server

import android.content.res.AssetManager
import com.example.edu_tester.data.session.ActiveSessionManager
import com.example.edu_tester.domain.model.Answer
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException

class LocalTestHttpServer(
    port: Int,
    private val assets: AssetManager,
    private val sessionManager: ActiveSessionManager,
    private val gson: Gson,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.substringBefore('?')
        val method = session.method

        if (method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "").also { addCors(it) }
        }

        return try {
            when {
                uri.startsWith("/api/") -> serveApi(uri, method, session)
                method == Method.GET -> serveStatic(uri)
                else -> jsonError(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed")
            }
        } catch (e: IllegalArgumentException) {
            jsonError(Response.Status.BAD_REQUEST, e.message ?: "Bad request")
        } catch (e: IllegalStateException) {
            jsonError(Response.Status.CONFLICT, e.message ?: "Conflict")
        } catch (e: Exception) {
            jsonError(Response.Status.INTERNAL_ERROR, e.message ?: "Internal error")
        }
    }

    private fun serveApi(uri: String, method: Method, session: IHTTPSession): Response {
        return when {
            uri == "/api/session" && method == Method.GET -> getSession()
            uri == "/api/questions" && method == Method.GET -> getQuestions()
            uri == "/api/register" && method == Method.POST -> postRegister(session)
            uri == "/api/submit" && method == Method.POST -> postSubmit(session)
            else -> jsonError(Response.Status.NOT_FOUND, "Unknown API endpoint")
        }
    }

    private fun getSession(): Response {
        val active = sessionManager.current()
            ?: return jsonError(Response.Status.NOT_FOUND, "Сессия не запущена")
        val body = mapOf(
            "sessionId" to active.session.id,
            "title" to active.test.title,
            "questionCount" to active.test.questions.size,
        )
        return jsonOk(body)
    }

    private fun getQuestions(): Response {
        if (!sessionManager.isActive()) {
            return jsonError(Response.Status.NOT_FOUND, "Сессия не запущена")
        }
        return jsonOk(mapOf("questions" to sessionManager.publicQuestions()))
    }

    private fun postRegister(session: IHTTPSession): Response {
        if (!sessionManager.isActive()) {
            return jsonError(Response.Status.NOT_FOUND, "Сессия не запущена")
        }
        val json = readJsonBody(session)
        val firstName = json.get("firstName")?.asString?.trim().orEmpty()
        val lastName = json.get("lastName")?.asString?.trim().orEmpty()
        if (firstName.isBlank() || lastName.isBlank()) {
            return jsonError(Response.Status.BAD_REQUEST, "Укажите имя и фамилию")
        }
        val student = sessionManager.register(firstName, lastName)
        return jsonOk(
            mapOf(
                "token" to student.token,
                "firstName" to student.firstName,
                "lastName" to student.lastName,
            )
        )
    }

    private fun postSubmit(session: IHTTPSession): Response {
        if (!sessionManager.isActive()) {
            return jsonError(Response.Status.NOT_FOUND, "Сессия не запущена")
        }
        val json = readJsonBody(session)
        val token = json.get("token")?.asString.orEmpty()
        if (token.isBlank()) {
            return jsonError(Response.Status.BAD_REQUEST, "Нет токена студента")
        }
        if (sessionManager.findStudent(token) == null) {
            return jsonError(Response.Status.UNAUTHORIZED, "Неизвестный студент")
        }
        val answersArray = json.getAsJsonArray("answers")
            ?: return jsonError(Response.Status.BAD_REQUEST, "Нет ответов")
        val type = object : TypeToken<List<AnswerDto>>() {}.type
        val dtos: List<AnswerDto> = gson.fromJson(answersArray, type)
        val answers = dtos.map { Answer(questionId = it.questionId, optionIndex = it.optionIndex) }

        val result = runBlocking {
            sessionManager.submit(token, answers)
        }
        return jsonOk(
            mapOf(
                "score" to result.attempt.score,
                "maxScore" to result.attempt.maxScore,
                "alreadySubmitted" to result.alreadySubmitted,
                "firstName" to result.attempt.firstName,
                "lastName" to result.attempt.lastName,
            )
        )
    }

    private fun serveStatic(uri: String): Response {
        val path = when {
            uri == "/" || uri.isEmpty() -> "web/index.html"
            uri == "/test" || uri == "/test.html" -> "web/test.html"
            uri.startsWith("/") -> "web${uri}"
            else -> "web/$uri"
        }
        return try {
            val bytes = assets.open(path).use { it.readBytes() }
            val mime = mimeTypeFor(path)
            newFixedLengthResponse(
                Response.Status.OK,
                mime,
                ByteArrayInputStream(bytes),
                bytes.size.toLong(),
            )
        } catch (_: IOException) {
            if (!sessionManager.isActive() && (uri == "/" || uri.isEmpty())) {
                return inactivePage()
            }
            jsonError(Response.Status.NOT_FOUND, "Not found")
        }
    }

    private fun inactivePage(): Response {
        val html = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1"/>
              <title>Сессия не запущена</title>
              <link rel="stylesheet" href="/styles.css"/>
            </head>
            <body>
              <main class="card">
                <h1>Сессия не запущена</h1>
                <p>Преподаватель ещё не начал тестирование. Попробуйте позже.</p>
              </main>
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    private fun readJsonBody(session: IHTTPSession): JsonObject {
        val files = HashMap<String, String>()
        session.parseBody(files)
        val body = files["postData"].orEmpty()
        if (body.isBlank()) throw IllegalArgumentException("Пустое тело запроса")
        return JsonParser.parseString(body).asJsonObject
    }

    private fun jsonOk(body: Any): Response =
        newFixedLengthResponse(
            Response.Status.OK,
            "application/json; charset=utf-8",
            gson.toJson(body),
        ).also { addCors(it) }

    private fun jsonError(status: Response.Status, message: String): Response =
        newFixedLengthResponse(
            status,
            "application/json; charset=utf-8",
            gson.toJson(mapOf("error" to message)),
        ).also { addCors(it) }

    private fun addCors(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type")
    }

    private fun mimeTypeFor(path: String): String = when {
        path.endsWith(".html") -> "text/html; charset=utf-8"
        path.endsWith(".css") -> "text/css; charset=utf-8"
        path.endsWith(".js") -> "application/javascript; charset=utf-8"
        path.endsWith(".json") -> "application/json; charset=utf-8"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".svg") -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    private data class AnswerDto(
        val questionId: String,
        val optionIndex: Int,
    )
}
