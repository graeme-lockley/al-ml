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

    fun leftVar(): Constraints =
        Constraints(state.map {
            when {
                it.t1 is TVar ->
                    it

                it.t2 is TVar ->
                    Constraint(it.t2, it.t1)

                else ->
                    it
            }
        })


    fun merge(): Constraints {
        val constraints =
            state.toMutableList()

        var lp = 0
        while (lp < constraints.size) {
            val current =
                constraints[lp]

            if (current.t1 is TVar) {
                var inner = lp + 1

                while (inner < constraints.size) {
                    val innerConstraint =
                        constraints[inner]

                    if (innerConstraint.t1 == current.t1) {
                        val mergeResult =
                            merge(current.t2, innerConstraint.t2)

                        if (mergeResult == null) {
                            println("Error - can't merge: ${current.t2} with ${innerConstraint.t2}")
                            inner += 1
                        } else {
                            constraints[lp] = Constraint(current.t1, mergeResult.second)
                            constraints.removeAt(inner)

                            if (mergeResult.first != nullSubstitution) {
                                var l = 0
                                while (l < constraints.size) {

                                    fun ff(type: Type): Type =
                                        type.apply(mergeResult.first)

                                    constraints[l] = Constraint(ff(constraints[l].t1), ff(constraints[l].t2))

                                    l += 1
                                }

                                constraints += mergeResult.first.state.map { Constraint(TVar(it.key), it.value) }
                            }
                        }
                    } else {
                        inner += 1
                    }
                }
            }

            lp += 1
        }

        return Constraints(constraints)
    }


    private fun merge(t1: Type?, t2: Type?): Pair<Substitution, Type>? =
        when {
            t1 == null || t2 == null ->
                null

            similar(t1, t2) ->
                Pair(nullSubstitution, t1)

            else -> {
                val substitution = createSubstitution(t1, t2)

                if (substitution == nullSubstitution)
                    null
                else
                    Pair(substitution, t1)
            }
        }


    private fun createSubstitution(t1: Type, t2: Type): Substitution =
        when {
            t1 == t2 ->
                nullSubstitution

            t1 is TArr && t2 is TArr ->
                createSubstitution(t1.domain, t2.domain) + createSubstitution(t1.range, t2.range)

            t1 is TCon && t2 is TCon && t1.name == t2.name ->
                nullSubstitution

            t1 is TVar && t2 is TVar && t1.variable == t2.variable ->
                nullSubstitution

            t1 is TVar ->
                Substitution(t1.variable, t2)

            t2 is TVar ->
                Substitution(t2.variable, t1)

            else ->
                nullSubstitution
        }
}

val noConstraints = Constraints()