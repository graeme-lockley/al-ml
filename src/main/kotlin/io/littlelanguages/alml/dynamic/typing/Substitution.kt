package io.littlelanguages.alml.dynamic.typing

data class Substitution(val state: Map<Var, Type> = emptyMap()) {
    constructor(key: Var, value: Type) : this(mapOf(Pair(key, value)))

    operator fun plus(other: Substitution): Substitution =
        Substitution(other.state.mapValues { it.value.apply(this) } + state)

    operator fun get(key: Var): Type? =
        state[key]

    operator fun minus(keys: List<Var>): Substitution =
        Substitution(state - keys.toSet())

    override fun toString(): String =
        state.entries.map { "'${it.key} ${it.value}" }.sorted().joinToString(", ")
}
