package io.littlelanguages.alml.typed.typing

data class Scheme(val parameters: Set<Var>, val type: Type) {
    fun apply(s: Substitution): Scheme =
        Scheme(parameters, type.apply(s - parameters))

    override fun toString(): String =
        "<${parameters.joinToString(", ") { it.toString() }}> $type"

    fun instantiate(varPump: VarPump): Type =
        type.apply(Substitution(parameters.toList().associateWith { varPump.fresh(type.position) }))
}

fun generalise(type: Type, substitution: Substitution = nullSubstitution): Scheme {
    val typeFtv: List<Int> =
        type.ftv().toList()

    val substitutionParameters =
        typeFtv.map { TVar(type.position, it).apply(substitution) }

    val typeSubstitution =
        typeFtv.zip(substitutionParameters).map { Substitution(it.first, it.second) }.fold(nullSubstitution) { s, m -> s + m }

    val type1 = type.apply(typeSubstitution)

    return Scheme(type1.ftv(), type1)
}

fun typeToScheme(type: Type): Scheme =
    Scheme(type.ftv(), type)
