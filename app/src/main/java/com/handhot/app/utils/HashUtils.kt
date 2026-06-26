package com.handhot.app.utils

import java.security.MessageDigest

object HashUtils {
    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate dedup hash from title + link (not title + summary, since summary may change)
     */
    fun itemHash(title: String, link: String): String {
        return md5("$title|$link")
    }
}
