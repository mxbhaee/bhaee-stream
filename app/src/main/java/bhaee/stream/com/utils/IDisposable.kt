package bhaee.stream.com.utils

interface IDisposable {
    fun dispose()
}

object IDisposableHelper {
    fun <T : IDisposable> using(disposeObject: T, work: (T) -> Unit) {
        work.invoke(disposeObject)
        disposeObject.dispose()
    }
}