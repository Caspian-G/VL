package com.Fch.vl.viewmodel

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.json.JSONArray
import java.io.File
import java.net.URLEncoder

class NetManager {
    suspend fun getHTTPFileList(ipPort: String, path: String): List<String>{
        HttpClient().use { client ->
            val spaceBarReplacedPath = path.replace(" ","%20")
            val response: HttpResponse = client.get("http://$ipPort/VL/Func/PathMenu?root=$spaceBarReplacedPath")
            when {
                response.status.value in 200..299 -> {
                    val body = response.bodyAsText()
                    val jsonArray = JSONArray(body)
                    return (0 until jsonArray.length()).map { jsonArray.getString(it) }
                }
                //4xx
                response.status.value in 400..499 -> {
                    throw Exception("Client Error: ${response.status}")
                }
                //5xx
                response.status.value in 500..599 -> {
                    throw Exception("Server Error: ${response.status}")
                }
                else -> {
                    throw Exception("Unknown Response: ${response.status}")
                }
            }
        }
    }

    suspend fun getHTTPCurPath(ipPort: String): String {
        val client = HttpClient()
        val response: HttpResponse = client.get("http://" + ipPort + "/VL/Func/Path")
        val body = response.bodyAsText()
        client.close()
        return body
    }
}
