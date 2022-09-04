package io.littlelanguages.alml.typed.typing

data class Environment(
    private val typeBindings: Map<String, Type> = emptyMap()
) {
    fun type(name: String): Type? =
        typeBindings[name]

    fun types() = typeBindings.entries.toList()

    operator fun plus(binding: Pair<String, Type>): Environment =
        Environment(typeBindings + binding)
}
