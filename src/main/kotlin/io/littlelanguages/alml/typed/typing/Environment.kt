package io.littlelanguages.alml.typed.typing

import io.littlelanguages.data.NestedMap

data class Environment(
    private val typeBindings: NestedMap<String, Type> = NestedMap()
) {
    fun type(name: String): Type? =
        typeBindings.get(name)

    fun add(name: String, type: Type) {
        typeBindings.add(name, type)
    }
}

fun initialEnvironment(): Environment {
    val environment = Environment()

    environment.add("True", typeBool)
    environment.add("False", typeBool)

    return environment
}