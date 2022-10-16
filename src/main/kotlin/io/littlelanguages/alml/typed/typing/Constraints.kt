package io.littlelanguages.alml.typed.typing

import io.littlelanguages.alml.Error
import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.UnificationFail
import io.littlelanguages.alml.UnificationMismatch

data class Constraints(val state: MutableList<Constraint> = mutableListOf()) {
    fun add(t1: Type, t2: Type) {
        state.add(Constraint(t1, t2))
    }

    fun add(constraint: Constraint) {
        state.add(constraint)
    }

    fun solve(errors: Errors): Substitution =
        try {
            solver(state)
        } catch (e: UnificationException) {
            errors.report(e.error)
            nullSubst
        }

    override fun toString(): String =
        state.joinToString(", ") { it.toString() }
}

data class Constraint(val t1: Type, val t2: Type) {
    fun apply(substitution: Substitution): Constraint =
        Constraint(t1.apply(substitution), t2.apply(substitution))

    override fun toString(): String =
        "$t1 ~ $t2"
}

private data class Unifier(val subst: Substitution, val constraints: List<Constraint>)

private val emptyUnifier = Unifier(nullSubst, emptyList())

private fun bind(name: Var, type: Type): Unifier =
    Unifier(Substitution(mapOf(Pair(name, type))), emptyList())

private fun unifies(t1: Type, t2: Type): Unifier =
    when {
        t1 similar t2 -> emptyUnifier
        t1 is TVar -> bind(t1.variable, t2)
        t2 is TVar -> bind(t2.variable, t1)
        t1 is TArr && t2 is TArr -> unifyMany(listOf(t1.domain, t1.range), listOf(t2.domain, t2.range))
        else -> throw UnificationException(UnificationFail(t1, t2))
    }

private fun applyTypes(s: Substitution, ts: List<Type>): List<Type> =
    ts.map { it.apply(s) }

private fun unifyMany(ta: List<Type>, tb: List<Type>): Unifier =
    if (ta.isEmpty() && tb.isEmpty()) emptyUnifier
    else if (ta.isEmpty() || tb.isEmpty()) throw UnificationException(UnificationMismatch(ta, tb))
    else {
        val t1 = ta[0]
        val ts1 = ta.drop(1)

        val t2 = tb[0]
        val ts2 = tb.drop(1)

        val (su1, cs1) = unifies(t1, t2)
        val (su2, cs2) = unifyMany(applyTypes(su1, ts1), applyTypes(su1, ts2))

        Unifier(su2 compose su1, cs1 + cs2)
    }

private fun solver(constraints: List<Constraint>): Substitution {
    var su = nullSubst
    var cs = constraints.toList()

    while (cs.isNotEmpty()) {
        val (t1, t2) = cs[0]
        val cs0 = cs.drop(1)

        val (su1, cs1) = unifies(t1, t2)

        su = su1 compose su
        cs = cs1 + cs0.map { Constraint(it.t1.apply(su1), it.t2.apply(su1)) }
    }

    return su
}

private class UnificationException(val error: Error) : Exception(error.toString())