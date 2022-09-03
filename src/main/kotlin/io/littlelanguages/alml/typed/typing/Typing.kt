package io.littlelanguages.alml.typed.typing

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location

typealias Var =
        Int

sealed class Type(
    open val position: Location?
) : Yamlable {
    abstract fun apply(s: Substitution): Type

    abstract fun ftv(): Set<Var>

    override fun yaml(): Any =
        this.toString()
}

data class TArr(
    override val position: Location?,
    val domain: Type,
    val range: Type
) : Type(position) {
    constructor(domain: Type, range: Type) : this(combine(domain.position, range.position), domain, range)

    override fun apply(s: Substitution) =
        TArr(position, domain.apply(s), range.apply(s))

    override fun ftv() =
        domain.ftv().plus(range.ftv())

    override fun toString(): String =
        when (domain) {
            is TArr ->
                "($domain) -> $range"

            else ->
                "$domain -> $range"
        }
}

data class TCon(
    override val position: Location?,
    val name: String,
    val arguments: List<Type> = emptyList()
) : Type(position) {
    constructor(name: String, arguments: List<Type> = emptyList()) : this(null, name, arguments)

    override fun apply(s: Substitution) =
        if (arguments.isEmpty())
            this
        else
            TCon(position, name, arguments.map { it.apply(s) })

    override fun ftv() =
        arguments.fold(emptySet<Var>()) { ftvs, type -> ftvs + type.ftv() }

    override fun toString(): String =
        when {
            name == "Tuple${arguments.size}" ->
                "(${arguments.joinToString(", ") { it.toString() }})"

            arguments.isEmpty() ->
                name

            else ->
                "$name ${arguments.joinToString(" ") { it.toString() }}"
        }

    override fun equals(other: Any?): Boolean =
        other is TCon && name == other.name && arguments == other.arguments
}

data class TVar(
    override val position: Location?,
    val variable: Var
) : Type(position) {
    constructor(variable: Var) : this(null, variable)

    override fun apply(s: Substitution) =
        s[variable] ?: this

    override fun ftv() =
        setOf(variable)

    override fun toString(): String =
        "'$variable"

    override fun equals(other: Any?): Boolean =
        other is TVar && variable == other.variable
}

private fun combine(a: Location?, b: Location?): Location? =
    when {
        a == null -> b
        b == null -> a
        else -> a + b
    }

fun similar(t1: Type, t2: Type): Boolean =
    when {
        t1 is TCon && t2 is TCon ->
            t1.name == t2.name &&
                    t1.arguments.zip(t2.arguments).fold(true) { a, b -> a && similar(b.first, b.second) }

        t1 is TVar && t2 is TVar ->
            t1.variable == t2.variable

        t1 is TArr && t2 is TArr ->
            similar(t1.domain, t2.domain) && similar(t1.range, t2.range)

        else ->
            false
    }


val nullSubstitution =
    Substitution()

val typeError =
    TCon("*error*")

val typeUnit =
    TCon("()")

val typeS32 =
    TCon("S32")

val typeBool =
    TCon("Bool")

val typeString =
    TCon("String")

