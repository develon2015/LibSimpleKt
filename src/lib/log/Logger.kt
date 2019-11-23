package lib.log

/**
 * call stack:
 * 		Thread#getStackTrace() <= Logger#d(msg) <= Obj#foo()
 * OR
 * 		Thread#getStackTrace() <= Logger#d(title, msg) <= Obj#foo()
 *
 * So can't use the default parameter,
 * Obj#foo()'s index is 2
 */
class Logger(private val name: String = "Logger") {
	fun d(msg: String) {
		val trace: Array<StackTraceElement>?  = Thread.currentThread().stackTrace

		if (trace == null || trace.size < 3) return println("Logger@$name $msg")

		val caller = trace[2]

		with (caller) {
			val output = "Logger@$name ~ $fileName($lineNumber) ~ $className.$methodName(): $msg"
			println(output)
		}
	}

	fun d(title: String, msg: String) {
		val trace: Array<StackTraceElement>?  = Thread.currentThread().stackTrace

		if (trace == null || trace.size < 3) return println("Logger@$name $title -> $msg")

		val caller = trace[2]

		with (caller) {
			var output = "Logger@$name ~ $fileName($lineNumber) ~ $className.$methodName(): "
			output += if (title.trim() != "") "$title -> $msg" else msg
			println(output)
		}
	}
}
