package io.littlelanguages.alml.typed.typing

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location

typealias Var = Int

sealed class Type(
    open val position: Location?
) : Yamlable {
    abstract fun apply(s: Substitution): Type

    abstract fun ftv(): Set<Var>

    override fun yaml(): Any = this.toString()

    abstract fun fullYaml(): Any

    abstract fun withPosition(position: Location): Type
}

data class TArr(
    override val position: Location?, val domain: Type, val range: Type
) : Type(position) {
    constructor(domain: Type, range: Type) : this(combine(domain.position, range.position), domain, range)

    override fun apply(s: Substitution) = TArr(position, domain.apply(s), range.apply(s))

    override fun ftv() = domain.ftv().plus(range.ftv())

    override fun fullYaml(): Any = singletonMap(
        "TArr", mapOf(
            Pair("domain", domain.fullYaml()), Pair("range", range.fullYaml()), Pair("location", position?.yaml() ?: "none")
        )
    )

    override fun toString(): String = when (domain) {
        is TArr -> "($domain) -> $range"

        else -> "$domain -> $range"
    }

    override fun withPosition(position: Location): Type = TArr(position, domain, range)
}

data class TCon(
    override val position: Location?, val name: String
) : Type(position) {
    constructor(name: String) : this(null, name)

    override fun apply(s: Substitution) = this

    override fun ftv(): Set<Var> = emptySet()

    override fun fullYaml(): Any = singletonMap(
        "TCon", mapOf(
            Pair("name", name), Pair("location", position?.yaml() ?: "none")
        )
    )

    override fun toString(): String = name

    override fun withPosition(position: Location): Type = TCon(position, name)
}

data class TVar(
    override val position: Location?, val variable: Var
) : Type(position) {
    constructor(variable: Var) : this(null, variable)

    override fun apply(s: Substitution) = s[variable] ?: this

    override fun ftv() = setOf(variable)

    override fun fullYaml(): Any = singletonMap(
        "TVar", mapOf(
            Pair("variable", variable), Pair("location", position?.yaml() ?: "none")
        )
    )

    override fun toString(): String = "'$variable"

    override fun withPosition(position: Location): Type = TVar(position, variable)
}

private fun combine(a: Location?, b: Location?): Location? = when {
    a == null -> b
    b == null -> a
    else -> a + b
}

fun similar(t1: Type, t2: Type): Boolean = when {
    t1 is TCon && t2 is TCon -> t1.name == t2.name

    t1 is TVar && t2 is TVar -> t1.variable == t2.variable

    t1 is TArr && t2 is TArr -> similar(t1.domain, t2.domain) && similar(t1.range, t2.range)

    else -> false
}


val nullSubstitution = Substitution()

val typeError = TCon("*error*")

val typeUnit = TCon("Unit")

val typeS32 = TCon("S32")

val typeBool = TCon("Bool")

val typeString = TCon("String")

