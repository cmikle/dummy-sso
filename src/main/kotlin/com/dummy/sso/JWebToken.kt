package com.dummy.sso

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JWebToken(
    val header: JSONObject,
    val payload: JSONObject,
    val signature: String
) {
    override fun toString() = encode(header) + "." + encode(payload) + "." + signature

    companion object {
        private const val SECRET_KEY: String = "dummysecret"
        private const val ISSUER: String = "com.dummy.sso"
        private const val JWT_HEADER: String = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"

        fun generate(user: String, minutes: Long): JWebToken {
            val payload = JSONObject()
            payload.put("sub", "supplier-declaration-api")
            payload.put("status", 0)
            payload.put("user", user)

            payload.put("iat", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
            payload.put("iss", ISSUER)
            payload.put("jti", UUID.randomUUID().toString())

            val audArray = JSONArray()
            audArray.put("localhost:3000")
            payload.put("aud", audArray)

            val ldt = LocalDateTime.now().plusMinutes(minutes)
            payload.put("exp", ldt.toEpochSecond(ZoneOffset.UTC))

            val groups = JSONArray()
            groups.put("groupAccess")
            payload.put("groups", groups)

            val header = JSONObject(JWT_HEADER)
            return JWebToken(
                header,
                payload,
                hmacSha256(encode(header) + "." + encode(payload))!!
            )
        }

        private fun hmacSha256(data: String): String? {
            try {
                val hash = SECRET_KEY.toByteArray(StandardCharsets.UTF_8)
                val sha256Hmac = Mac.getInstance("HmacSHA256")
                val secretKey = SecretKeySpec(hash, "HmacSHA256")
                sha256Hmac.init(secretKey)

                val signedBytes = sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
                return encode(signedBytes)
            } catch (ex: NoSuchAlgorithmException) {
                Logger.getLogger(JWebToken::class.java.name).log(Level.SEVERE, ex.message, ex)
                return null
            } catch (ex: InvalidKeyException) {
                Logger.getLogger(JWebToken::class.java.name).log(Level.SEVERE, ex.message, ex)
                return null
            }
        }

        private fun encode(obj: JSONObject): String {
            return encode(obj.toString().toByteArray(StandardCharsets.UTF_8))
        }

        private fun encode(bytes: ByteArray): String {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
