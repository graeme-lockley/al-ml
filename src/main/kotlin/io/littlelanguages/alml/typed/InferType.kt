package io.littlelanguages.alml.typed

import io.littlelanguages.alml.static.ast.*
import io.littlelanguages.alml.typed.typing.*
import io.littlelanguages.alml.typed.typing.Type

/*
 Type inference rules:
    Program i1...in d1..dm:
        L, i1: T1, ..., in: Tn |-
        ---
        L |- Program i1...in d1..dm: Unit

    Block e1; ...; en (n = 0):
        ---
        L |- e1; ...; en: Unit

    Block e1; ...; en (n > 0):
        L |- e1: T1  ... L |- en: Tn
        ---
        L |- Block e1; ...; en (n > 0): Tn

    CallProcedure e a1 ... an:
        L, e: T1 -> ... -> Tn -> Tr |- a1: T1 ... an: Tn
        ---
        L |- CallProcedure e a1 ... an: Tr

    If e1 e2:
        L |- e1: Bool  L |- e2: S
        ---
        L |- If e1 e2: Unit

    If e1 e2 e3:
        L |- e1: Bool  L |- e2: S  L |- e3: S
        ---
        L |- If e1 e2 e3: S

    LiteralS32:
        ---
        L |- n: S32

    LiteralString:
        ---
        L |- s: String

    LiteralUnit:
        ---
        L |- (): Unit

    Signal e:
        L |- e: String
        ---
        L |- Signal e: Unit

    Try i1 i2:
        L, i1: T, i2: String -> T |-
        ---
        Try i1 i2: T
 */

fun inferValueType(type: Type?, e: Expression): Type {
    val inferType = InferType()
    val resultType = inferType.expression(e)
    inferType.addConstraint(type, resultType)

    return resultType
}

class InferType(
    private val optimiseConstraints: Boolean = true
) {
    var environment = Environment()
    var constraints = Constraints()
    private val pump = VarPump()

    init {
        environment += Pair("True", typeBool)
        environment += Pair("False", typeBool)
    }

    fun expressions(es: List<Expression>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression): Type =
        when (e) {
            is Identifier ->
                environment.type(e.name) ?: typeError

            is LiteralS32 ->
                typeS32

            is LiteralString ->
                typeString

            is LiteralUnit ->
                typeUnit

            else ->
                typeError // TODO(e.toString()) // typeError
        }

    fun addConstraint(t1: Type?, t2: Type?) {
        if (t1 != null && t2 != null)
            if (optimiseConstraints && t1 != t2 || !optimiseConstraints)
                constraints += Constraint(t1, t2)
    }
}
