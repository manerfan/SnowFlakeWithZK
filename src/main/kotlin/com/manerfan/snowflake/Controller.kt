package com.manerfan.snowflake

import com.google.common.collect.Maps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.NoHandlerFoundException
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/next")
class IdWorkerController {
    @Autowired
    private lateinit var idWorker: IdWorker

    @GetMapping("/long")
    fun nextLong(): Map<String, Any> = mapOf(
            "code" to 200,
            "next" to idWorker.nextId()
    )

    @GetMapping("/hex")
    fun nextHex(): Map<String, Any> = mapOf(
            "code" to 200,
            "next" to idWorker.nextIdHex()
    )

    @GetMapping("/bin")
    fun nextBin(): Map<String, Any> = mapOf(
            "code" to 200,
            "next" to idWorker.nextIdBin()
    )
}

@RestControllerAdvice
class ControllerAdvicer : ErrorController {
    private val logger = LoggerFactory.getLogger(ControllerAdvice::class.java)

    @ExceptionHandler(NoHandlerFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(ex: NoHandlerFoundException) = mapOf("code" to HttpStatus.NOT_FOUND.value(), "message" to ex.message)

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun error(ex: Throwable) = mapOf("code" to HttpStatus.INTERNAL_SERVER_ERROR.value(), "message" to ex.message)

    @Value("\${server.error.path:\${error.path:/error}}")
    private lateinit var errorPath: String

    @RequestMapping(value = ["\${server.error.path:\${error.path:/error}}"])
    fun error(request: HttpServletRequest): ResponseEntity<*> {
        var statusCode = 400
        val sc = request.getAttribute("javax.servlet.error.status_code")
        if (null != sc) {
            statusCode = sc as Int
        }

        var message: String? = request.getAttribute("javax.servlet.error.message") as String
        val exception = request.getAttribute("javax.servlet.error.exception") as Throwable

        logger.error("[{}] {}", statusCode, message, exception)

        val responseBody = Maps.newHashMap<String, Any>()
        responseBody["code"] = statusCode
        responseBody["message"] = message

        return ResponseEntity.status(statusCode).body<Map<String, Any>>(responseBody)
    }

    override fun getErrorPath(): String? {
        return errorPath
    }
}