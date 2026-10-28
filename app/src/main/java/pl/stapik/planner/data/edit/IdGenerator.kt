package pl.stapik.planner.data.edit

import java.security.SecureRandom

fun interface IdGenerator {
    fun next(): String

    companion object {
        val Secure: IdGenerator = object : IdGenerator {
            private val random = SecureRandom()

            override fun next(): String = "%016x".format(random.nextLong())
        }
    }
}
