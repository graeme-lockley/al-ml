package io.littlelanguages.alml

class Errors(private val errors: MutableList<Error> = mutableListOf()) {
    fun report(error: Error) {
        errors.add(error)
    }
}
