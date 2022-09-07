package io.littlelanguages.alml.typed

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.static.ast.*
import io.littlelanguages.alml.typed.typing.*
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right

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

fun inferValueType(type: Type?, e: Expression, environment: Environment = Environment()): Either<List<Errors>, Type> {
    val inferType = InferType(environment)
    val resultType = inferType.expression(e)

    inferType.addConstraint(type, resultType)

    return unifies(inferType.constraints) map { resultType.apply(it) }
}

fun inferProcedureType(
    name: String,
    parameterName: List<String>,
    e: Expression,
    pump: VarPump,
    environment: Environment
): Either<List<Errors>, Type> {
    val parameterTypes = parameterName.map { Pair(it, pump.fresh()) }
    val returnType: Type = pump.fresh()
    val procedureType = parameterTypes.foldRight(returnType) { t, acc -> TArr(t.second, acc) }

    environment.openScope()
    environment.add(name, procedureType)
    parameterTypes.forEach { environment.add(it.first, it.second) }

    val inferType = InferType(environment)
    val inferredReturnType = inferType.expression(e)

    inferType.addConstraint(returnType, inferredReturnType)
    val unificationResult = unifies(inferType.constraints)
    environment.closeScope()

    return when (unificationResult) {
        is Left -> Left(unificationResult.left)
        is Right -> {
            Right(procedureType.apply(unificationResult.right))
        }
    }
}

class InferType(
    private val environment: Environment,
    private val optimiseConstraints: Boolean = true
) {
    var constraints = Constraints()

    fun expressions(es: List<Expression>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression): Type =
        when (e) {
            is Identifier ->
                environment.type(e.name) ?: typeError

            is LiteralS32 ->
                typeS32.withPosition(e.position)

            is LiteralString ->
                typeString.withPosition(e.position)

            is LiteralUnit ->
                typeUnit.withPosition(e.position)

            is BinaryOpExpression -> {
                val leftType = expression(e.left)
                val rightType = expression(e.right)

                when (e.op.operator) {
                    Operators.Plus, Operators.Minus, Operators.Multiply, Operators.Divide -> {
                        addConstraint(leftType, typeS32)
                        addConstraint(rightType, typeS32)

                        typeS32.withPosition(e.position)
                    }

                    Operators.Equals, Operators.NotEquals, Operators.LessThan, Operators.LessEquals, Operators.GreaterThan, Operators.GreaterEquals -> {
                        addConstraint(leftType, rightType)
                        typeBool.withPosition(e.position)
                    }
                }
            }

            else ->
                typeError // TODO(e.toString()) // typeError
        }

    fun addConstraint(t1: Type?, t2: Type?) {
        if (t1 != null && t1 != typeError && t2 != null && t2 != typeError)
            if (optimiseConstraints && t1 != t2 || !optimiseConstraints)
                constraints += Constraint(t1, t2)
    }
}
