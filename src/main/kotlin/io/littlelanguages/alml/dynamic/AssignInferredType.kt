package io.littlelanguages.alml.dynamic

import io.littlelanguages.alml.dynamic.tst.*
import io.littlelanguages.alml.dynamic.typing.*

/*
 Type inference rules:
    Program i1...in d1..dm:
        L, i1: T1, ..., in: Tn |-
        ---
        Program i1...in d1..dm: Unit

    Block e1; ...; en (n = 0):
        ---
        L |- e1; ...; en: Unit

    Block e1; ...; en (n > 0):
        L |- e1: T1  ... L |- en: Tn
        ---
        Block e1; ...; en (n > 0): Tn

    CallProcedure e a1 ... an:
        L, e: T1 -> ... -> Tn -> Tr |- a1: T1 ... an: Tn
        ---
        CallProcedure e a1 ... an: Tr

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
class AssignInferredType<S, T> {
    private var environment = Environment<S, T>()
    var constraints = Constraints<S, T>()
    private val pump = VarPump()

    fun program(p: Program<S, T>) {
        p.values.forEach { environment += Pair(it, pump.fresh()) }
        p.declarations.forEach { expressions(it.es) }
    }

    fun expressions(es: Expressions<S, T>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression<S, T>): Type =
        when (e) {
            is IdentifierExpression ->
                environment.type(e.symbol.name) ?: typeError

            is IfExpression ->
                if (e.e3 == null) {
                    val e1Type = expressions(e.e1)
                    expressions(e.e2)

                    addConstraint(e1Type, typeBool)

                    typeUnit
                } else {
                    val e1Type = expressions(e.e1)
                    val e2Type = expressions(e.e2)
                    val e3Type = expressions(e.e3)

                    addConstraint(e1Type, typeBool)
                    addConstraint(e2Type, e3Type)

                    e2Type
                }

            is LiteralS32 ->
                typeS32

            is LiteralString ->
                typeString

            is LiteralUnit ->
                typeUnit

            is SignalExpression -> {
                val eType = expressions(e.es)

                addConstraint(eType, typeString)

                typeUnit
            }

            else ->
                typeError
        }

    private fun addConstraint(t1: Type, t2: Type) {
        constraints += Constraint(t1, t2)
    }
}
