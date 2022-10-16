package io.littlelanguages.alml.typed

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.UnificationFail
import io.littlelanguages.alml.UnificationMismatch
import io.littlelanguages.alml.typed.typing.*

private typealias Unifier =
        Pair<Substitution, Constraints>


fun unifies(constraints: Constraints, errors: Errors): Substitution {
    val context =
        SolverContext(constraints, errors)

    return context.solve()
}


private class SolverContext(private var constraints: Constraints, private val errors: Errors) {
    private fun List<Type>.subst(substitution: Substitution): List<Type> =
        this.map { it.apply(substitution) }


    fun solve(): Substitution {
        var subst =
            Substitution()

        while (constraints.isNotEmpty()) {
            val constraint =
                constraints[0]

            val u =
                unifies(constraint.t1, constraint.t2)

            subst = u.first + subst
            constraints = u.second + constraints.drop(1).apply(u.first)
        }

        return subst
    }


    private fun unifies(t1: Type, t2: Type): Unifier =
        when {
            t1 == t2 ->
                Pair(nullSubstitution, noConstraints)

            t1 is TCon && t2 is TCon && t1.name == t2.name ->
                Pair(nullSubstitution, noConstraints)

            t1 is TVar && t2 is TVar && t1.variable == t2.variable ->
                Pair(nullSubstitution, noConstraints)

            t1 is TVar ->
                Pair(Substitution(t1.variable, t2), noConstraints)

            t2 is TVar ->
                Pair(Substitution(t2.variable, t1), noConstraints)

            t1 is TArr && t2 is TArr ->
                unifyMany(listOf(t1.domain, t1.range), listOf(t2.domain, t2.range))

            else -> {
                errors.report(UnificationFail(t1, t2))

                Pair(nullSubstitution, noConstraints)
            }
        }


    private fun unifyMany(t1s: Collection<Type>, t2s: Collection<Type>): Unifier =
        when {
            t1s.isEmpty() && t2s.isEmpty() ->
                Pair(nullSubstitution, noConstraints)

            t1s.isNotEmpty() && t2s.isNotEmpty() -> {
                val t1 =
                    t1s.first()

                val t2 =
                    t2s.first()

                val u1 =
                    unifies(t1, t2)

                val um =
                    unifyMany(t1s.drop(1).subst(u1.first), t2s.drop(1).subst(u1.first))


                Pair(um.first + u1.first, u1.second + um.second)
            }

            else -> {
                errors.report(UnificationMismatch(t1s, t2s))

                Pair(nullSubstitution, noConstraints)
            }
        }
}
