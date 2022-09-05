package io.littlelanguages.alml.typed.typing

data class Environment(
    private val typeBindings: MutableMap<String, Type> = mutableMapOf()
) {
    fun type(name: String): Type? =
        typeBindings[name]

    fun types() = typeBindings.entries.toList()

    fun add(name: String, type: Type) {
        typeBindings[name] = type
    }
}

fun initialEnvironment(): Environment {
    val environment = Environment()

    environment.add("True", typeBool)
    environment.add("False", typeBool)

    return environment
}