package com.example.logvarremote.data.runtime

import java.io.File
import java.security.MessageDigest

object FileIntegrity {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun requireSha256(file: File, expected: String) {
        val actual = sha256(file)
        require(actual.equals(expected, ignoreCase = true)) {
            "SHA-256 mismatch for ${file.name}: expected=$expected actual=$actual"
        }
    }
}
