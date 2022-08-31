package io.littlelanguages.alml.dynamic

import io.littlelanguages.alml.dynamic.tst.*
import io.littlelanguages.alml.dynamic.typing.*

/*
 Type inference rules:
    Block e1; ...; en (n = 0):
        ---
        L |- e1; ...; en: Unit

    Block e1; ...; en (n > 0):
        L |- e1: T1  ... L |- en: Tn
        ---
        Block e1; ...; en (n > 0): Tn

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
 */
class AssignInferredType<S, T> {
    var constraints = Constraints<S, T>()

    fun program(p: Program<S, T>) {
        p.declarations.forEach { expressions(it.es) }
    }

    fun expressions(es: Expressions<S, T>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression<S, T>): Type =
        when (e) {
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

            else -> TODO(e.toString())
        }

    private fun addConstraint(t1: Type, t2: Type) {
        constraints += Constraint(t1, t2)
    }
}
