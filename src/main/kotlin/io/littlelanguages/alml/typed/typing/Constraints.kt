package io.littlelanguages.alml.typed.typing

data class Constraints(val state: List<Constraint> = emptyList()) {
    operator fun plus(constraint: Constraint): Constraints =
        Constraints(state + constraint)

    operator fun plus(constraints: Constraints): Constraints =
        Constraints(state + constraints.state)

    fun apply(substitution: Substitution): Constraints =
        Constraints(state.map { it.apply(substitution) })

    fun isNotEmpty(): Boolean =
        state.isNotEmpty()

    operator fun get(index: Int): Constraint =
        state[index]

    fun drop(count: Int): Constraints =
        Constraints(state.drop(count))

    override fun toString(): String =
        state.joinToString(", ") { it.toString() }
}

val noConstraints = Constraints()