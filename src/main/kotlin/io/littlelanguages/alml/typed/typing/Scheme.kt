package io.littlelanguages.alml.typed.typing

data class Scheme(val parameters: List<Var>, val type: Type) {
    fun apply(s: Substitution): Scheme =
        Scheme(parameters, type.apply(s - parameters))

    override fun toString(): String =
        "<${parameters.joinToString(", ") { it.toString() }}> $type"

    fun instantiate(varPump: VarPump): Type {
        val asP =
            parameters.map { varPump.fresh(type.position) }

        val substitution =
            Substitution(parameters.zip(asP).toMap())

        return type.apply(substitution)
    }

    fun normalize(): Scheme {
        val subs =
            Substitution(parameters.foldIndexed(emptyMap()) { a, b, c -> b.plus(Pair(c, TVar(type.position, a))) })

        return Scheme(
            parameters.mapIndexed { index, _ -> index },
            type.apply(subs)
        )
    }

    fun isCompatibleWith(environment: Environment, other: Scheme): Boolean {
        val normalizedThis =
            this.normalize()

        val normalizedOther =
            other.normalize()

        return isCompatibleWith(environment, normalizedThis.type, normalizedOther.type)
    }
}

fun isCompatibleWith(environment: Environment, t1: Type, t2: Type): Boolean =
    when {
        t2 is TVar ->
            if (t1 is TVar)
                t1.variable == t2.variable
            else
                true

        t1 is TCon && t2 is TCon ->
            t1.name == t2.name && t1.arguments.size == t2.arguments.size && t1.arguments.zip(t2.arguments).fold(true) { a, b ->
                a && isCompatibleWith(environment, b.first, b.second)
            }

        t1 is TArr && t2 is TArr ->
            isCompatibleWith(environment, t1.domain, t2.domain) && isCompatibleWith(environment, t1.range, t2.range)

        else ->
            false
    }

fun generalise(type: Type, substitution: Substitution = nullSubstitution): Scheme {
    val typeFtv: List<Int> =
        type.ftv().toList()

    val substitutionParameters =
        typeFtv.map { TVar(type.position, it).apply(substitution) }

    val typeSubstitution =
        typeFtv.zip(substitutionParameters).map { Substitution(it.first, it.second) }.fold(nullSubstitution) { s, m -> s + m }

    val type1 = type.apply(typeSubstitution)

    return Scheme(type1.ftv().toList(), type1)
}

fun typeToScheme(type: Type): Scheme =
    Scheme(type.ftv().toList(), type)
