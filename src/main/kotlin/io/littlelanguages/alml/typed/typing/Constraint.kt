package io.littlelanguages.alml.typed.typing

data class Constraint(val t1: Type, val t2: Type) {
    fun apply(substitution: Substitution): Constraint =
        Constraint(t1.apply(substitution), t2.apply(substitution))

    override fun toString(): String =
        "$t1 ~ $t2"
}
