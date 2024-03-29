package lib.http

import java.lang.Thread.currentThread as cth
import java.net.*
import java.util.*
import java.io.*
import java.nio.charset.*

import lib.log.Logger

val log = Logger("HTTP")

/**
 * Simple HTTP Server
 */
class HttpServer(val host: String = "0.0.0.0", val port: Int = 80, val baseDir: String = ".") {

	data class HttpRequest(val method: String, val uri: String, val version: String, val headers: List<Header>)

	data class Header(val name: String, val value: String)

	val mime = mapOf(
		"html" to "text/html",
		"js" to "text/javascript",
		"css" to "text/css",
		"xml" to "application/xml",

		"zip" to "application/zip",
		"tar" to "application/x-tar",
		"rar" to "application/x-rar-compressed",
		"pdf" to "application/pdf",

		"webm" to "video/webm",
		"mp4" to "video/mp4",

		"mp3" to "audio/mp3",

		"jpg" to "image/jpeg",
		"jpeg" to "image/jpg",
		"png" to "image/png",
		"ico" to "image/ico",

		"default" to "application/octet-stream"
	)

	fun response(ous: OutputStream, request: HttpRequest) {
		
		var type = mime.get("default")!! // 默认资源类型

		val regexURI = """.*/[^/]+\.(.+)""".toRegex()

		val matchResult = regexURI.matchEntire(request.uri)
		matchResult ?.let {
			it.groups.get(1) ?.value.let { mime.get(it) ?.let { type = it } }
		}

		val file = File("$baseDir${ request.uri }")

		log.d("file: $file")

		if (file.isDirectory) {
			log.d("目录${ file }重定向至index.html")
			return response(ous, request.let { HttpRequest(it.method, "${it.uri}/index.html", it.version, it.headers) })
		}

		if (!file.isFile) {
			ous.write("""
			   |HTTP/2.0 404 NOT FOUND
			   |Content-Length: 0
			   |
			   |
			   """.trimMargin().toByteArray()
			)
			ous.flush()
			return
		}

		var length = file.length()

		when (request.method) {
			"HEAD", "GET" -> {
				ous.write("""
					|HTTP/2.0 200 OK
					|Content-Type: ${ type }
					|Content-Length: ${ length }
					|
					|
					""".trimMargin().toByteArray()
				)
				if ("GET".equals(request.method)) {
					// send file
					ous.write(file.readBytes())
				}
				ous.flush()
			}
		}
	}

	/**
	 * 开启监听服务
	 */
	fun service() {
		log.d("HttpServer监听于${ host } : ${ port }", "HttpServer")
		val sock = ServerSocket(port)

		while (true) {
			val conn = sock.accept()

			Thread {
				val name = cth().name

				log.d("${ (conn.remoteSocketAddress as InetSocketAddress).let{ "${ it.address.hostAddress }:${ it.port }" } }", "$name: 新的客户端连接")

				val ins = conn.getInputStream()
				val ous = conn.getOutputStream()
				val reader = BufferedReader(InputStreamReader(ins, Charset.forName("UTF-8")) )

				fun handleConn() : Boolean {

					// first line
					var line: String? = reader.readLine()

					if (line == null) {
						log.d("连接被关闭", name)
						return false
					}

					val regexRequest = """(GET|HEAD) ([^\s]*) HTTP/(\d\.\d)""".toRegex()
					var matchResult = regexRequest.matchEntire(line)

					if (matchResult == null) {
						log.d("Bad request: $line", name)
						return false
					}

					// 合法的请求
					var (method, uri, version) = matchResult.destructured
					uri = URLDecoder.decode(uri, "UTF-8")

					log.d("HTTP请求: $method $uri HTTP/$version", name)

					log.d("uri: $uri", name)

					val headers = LinkedList<Header>()
					while (true) {
						line = reader.readLine()
						if (line == null) {
							log.d("客户端断开连接", name)
							return false
						}

						if ("".equals(line.trim()) ) {
							log.d("Headers: $headers", name)
							break
						}

						val regexHeader = """(.*):\s(.*)""".toRegex()
						matchResult = regexHeader.matchEntire(line)
						if (matchResult == null) {
							log.d("Bad header: $line", name)
							continue
						}
						val (key, value) = matchResult.destructured
						headers.add(Header(key, value))
					}

					// response
					response(ous, HttpRequest(method, uri, version, headers))

					val headerConnection = try {
						headers.filter{ "Connection".equals(it.name) }.get(0).value
					} catch(e: Exception) {
						"unknown"
					}
					when (headerConnection) {
						"Close", "close" -> {
							log.d("处理完成, 正常断开", name)
							return false
						}
						"Keep-Alive" -> {
							log.d("处理完成, Keep-Alive", name)
							return true
						}
						else -> {
							log.d("处理完成, 继续等待", name)
							return true
						}
					}
				}

				while (handleConn()) {
					log.d("await handle...")
				}

				conn.close()

			}.start()
		}
	}
}
