package io.littlelanguages.alml.dynamic

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location

typealias Var =
        Int

sealed class Type(
    open val position: Location?
): Yamlable {
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
}

data class TVar(
    override val position: Location?,
    val variable: Var
) : Type(position) {
    override fun apply(s: Substitution) =
        s[variable] ?: this

    override fun ftv() =
        setOf(variable)

    override fun toString(): String =
        "'$variable"
}

data class Substitution(val state: Map<Var, Type> = emptyMap()) {
    constructor(key: Var, value: Type) : this(mapOf(Pair(key, value)))

    operator fun plus(other: Substitution): Substitution =
        Substitution(other.state.mapValues { it.value.apply(this) } + state)

    operator fun get(key: Var): Type? =
        state[key]

    operator fun minus(keys: List<Var>): Substitution =
        Substitution(state - keys.toSet())

    override fun toString(): String =
        state.entries.map { "'${it.key} ${it.value}" }.sorted().joinToString(", ")
}

private fun combine(a: Location?, b: Location?): Location? =
    when {
        a == null -> b
        b == null -> a
        else -> a + b
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
