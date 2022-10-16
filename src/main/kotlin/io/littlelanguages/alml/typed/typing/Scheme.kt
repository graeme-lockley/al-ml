package io.littlelanguages.alml.typed.typing

data class Scheme(val parameters: Set<Var>, val type: Type) {
    fun apply(s: Substitution): Scheme =
        Scheme(parameters, type.apply(s - parameters))

    override fun toString(): String =
        "<${parameters.joinToString(", ") { it.toString() }}> $type"

    fun instantiate(varPump: VarPump): Type =
        type.apply(Substitution(parameters.toList().associateWith { varPump.fresh(type.position) }))
}

fun typeToScheme(type: Type): Scheme =
    Scheme(type.ftv(), type)
