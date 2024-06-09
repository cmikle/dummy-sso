package com.dummy.sso

import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SsoController {
    var user: String? = null

    @GetMapping("/auth/realms/protocol/openid-connect/auth")
    fun login() = String(ClassPathResource("templates/login.html").file.readBytes())

    @PostMapping("/auth/realms/protocol/openid-connect/callback")
    fun init(request: LoginRequest, httpServletResponse: HttpServletResponse) {
        user = request.user // @todo: use session to save user
        val projectUrl = "http://localhost:3000/auth/shop/oauth2/callback?session_state=84c46da6-f807-4f66-b12f-0dd04eeff835&code=640b92e1-517e-4c9b-993e-2d33a817015f.84c46da6-f807-4f66-b12f-0dd04eeff835.a88fbb2d-81c0-489c-85a4-6fa2f5d85739"
        httpServletResponse.setHeader("Location", projectUrl)
        httpServletResponse.status = 302
    }

    @PostMapping(
        path = ["/auth/realms/protocol/openid-connect/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun tokens(request: TokenRequest): ResponseEntity<TokenResponse>? {
        val validGranTypes = listOf(
            "authorization_code",
            "refresh_token"
        )

        if (!validGranTypes.contains(request.grant_type)) {
            throw RuntimeException("Unsupported grant-type ${request.grant_type}")
        }

        val response = TokenResponse(
            JWebToken.generate(user!!, 1L).toString(),
            JWebToken.generate(user!!, 3L).toString()
        )

        println("Requested: ${request.grant_type}")
        return ResponseEntity(response, HttpStatus.OK)
    }

    data class LoginRequest(val user: String)
    data class TokenRequest(val grant_type: String)
    data class TokenResponse(
        val id_token: String,
        val refresh_token: String
    )
}
