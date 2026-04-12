package com.Fch.vl.viewmodel

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.io.File
import java.net.URLEncoder

class NetManager {
    suspend fun getHTTPFileList(ipPort: String, path: String): List<String>{
        val client = HttpClient()
        val response: HttpResponse = client.get("http://" + ipPort + "/VL/Func/PathMenu" + "?root=" + path)
        when {
            response.status.value in 200..299 -> {
                //2..
                val body = response.bodyAsText().removeSurrounding("[", "]").split(",").map{it.removeSurrounding("\"")}
                client.close()
                return body
            }
            response.status.value in 400..499 -> {
                //4..
                client.close()
                throw Exception("Client Error: ${response.status}")
            }
            response.status.value in 500..599 -> {
                //5..
                client.close()
                throw Exception("Server Error: ${response.status}")
            }
            else -> {
                client.close()
                throw Exception("Unknown Response: ${response.status}")
            }
        }
    }
}