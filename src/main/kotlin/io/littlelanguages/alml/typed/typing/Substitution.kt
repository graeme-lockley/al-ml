package io.littlelanguages.alml.typed.typing

data class Substitution(private val items: Map<Var, Type>) {
    infix fun compose(s: Substitution): Substitution =
        Substitution(s.items.mapValues { it.value.apply(this) } + items)

    operator fun get(v: Var): Type? = items[v]

    operator fun minus(names: Set<Var>): Substitution =
        Substitution(items - names)
}

val nullSubst = Substitution(emptyMap())