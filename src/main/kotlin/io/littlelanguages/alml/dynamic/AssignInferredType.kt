package io.littlelanguages.alml.dynamic

import io.littlelanguages.alml.dynamic.tst.*
import io.littlelanguages.alml.typed.typing.*

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
class AssignInferredType<S, T>(
    private val optimiseConstraints: Boolean = true
) {
    var environment = Environment<S, T>()
    var constraints = Constraints<S, T>()
    private val pump = VarPump()

    fun program(p: Program<S, T>) {
        addDeclarationProceduresIntoEnvironment(p)
        addDeclarationValueTypesIntoEnvironment(p)
        p.declarations.forEach { expressions(it.es) }
    }

    private fun addDeclarationProceduresIntoEnvironment(p: Program<S, T>) {
        p.declarations.filter {
            it.name != "_main"
        }.forEach {
            val start: Type = pump.fresh()
            val type = it.parameters.foldRight(start) { _, acc -> TArr(pump.fresh(), acc) }
            environment += Pair(it.name, type)
        }
    }

    private fun addDeclarationValueTypesIntoEnvironment(p: Program<S, T>) {
        val names = mutableSetOf<String>()

        p.declarations.find { it.name == "_main" }?.es?.forEach {
            if (it is AssignExpression && it.typeOf() != null) {
                val name = it.symbol.name

                environment += Pair(name, it.typeOf()!!)
                names.add(name)
            }
        }

        p.values.forEach { if (!names.contains(it)) environment += Pair(it, pump.fresh()) }
    }

    fun expressions(es: Expressions<S, T>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression<S, T>): Type =
        when (e) {
            is AssignExpression -> {
                val lhsType = environment.type(e.symbol.name)
                val rhsType = expressions(e.es)

                addConstraint(lhsType, rhsType)

                rhsType
            }

            is CallProcedureExpression -> {
                val procType = environment.type(e.procedure.name)

                val resultType: Type = pump.fresh()
                val argTypes = e.es.map { expressions(it) }
                val rhsType = argTypes.foldRight(resultType) { t, acc -> TArr(t, acc) }

                addConstraint(procType, rhsType)

                resultType
            }

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

            else -> TODO(e.toString())
//                typeError
        }

    private fun addConstraint(t1: Type?, t2: Type?) {
        if (t1 != null && t2 != null)
            if (optimiseConstraints && t1 != t2 || !optimiseConstraints)
                constraints += Constraint(t1, t2)
    }
}
