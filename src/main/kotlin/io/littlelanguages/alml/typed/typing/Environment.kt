package io.littlelanguages.alml.typed.typing

import io.littlelanguages.data.NestedMap
import io.littlelanguages.data.Union2
import io.littlelanguages.data.Union2a
import io.littlelanguages.data.Union2b

data class Environment(
    private val typeBindings: NestedMap<String, Union2<Type, Scheme>> = NestedMap()
) {
    fun type(name: String): Union2<Type, Scheme>? =
        typeBindings.get(name)

    fun add(name: String, type: Type) {
        typeBindings.add(name, Union2a(type))
    }

    fun add(name: String, scheme: Scheme) {
        typeBindings.add(name, Union2b(scheme))
    }

    fun openScope() =
        typeBindings.open()

    fun closeScope() =
        typeBindings.close()

    fun generalise(type: Type): Scheme =
        Scheme(type.ftv(), type)
}

fun initialEnvironment(): Environment {
    val environment = Environment()

    environment.add("True", typeBool)
    environment.add("False", typeBool)

    return environment
}