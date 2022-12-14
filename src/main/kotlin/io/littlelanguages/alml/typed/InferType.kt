package io.littlelanguages.alml.typed

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.static.ast.*
import io.littlelanguages.alml.typed.typing.*
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Union2a
import io.littlelanguages.data.Union2b

/*
 Type inference rules:
    Program i1...in d1..dm:
        L, i1: T1, ..., in: Tn |-
        ---
        L |- Program i1...in d1..dm: Unit

    Apply e a1 ... an:
        L, e: T1 -> ... -> Tn -> Tr |- a1: T1 ... an: Tn
        ---
        L |- Apply e a1 ... an: Tr

    BinaryOp el ('+', '-', '*', '/') er:
        L |- el: S32  L |- er: S32
        ---
        L |- el ('+', '-', '*', '/') er: S32

    BinaryOp el ('==', '!=') er:
        L |- el: T  L |- er: T
        ---
        L |- el ('==', '!=') er: Bool

    BinaryOp el ('<', '<=', '>', '>=) er:
        L |- el: T  L |- er: T  T in {S32, String, Bool}
        ---
        L |- el ('<', '<=', '>', '>=) er: Bool

    Block e1; ...; en (n = 0):
        ---
        L |- e1; ...; en: Unit

    Block e1; ...; en (n > 0):
        L |- e1: T1  ... L |- en: Tn
        ---
        L |- Block e1; ...; en (n > 0): Tn

    If e1 e2:
        L |- e1: Bool  L |- e2: S
        ---
        L |- If e1 e2: Unit

    If e1 e2 e3:
        L |- e1: Bool  L |- e2: S  L |- e3: S
        ---
        L |- If e1 e2 e3: S

    Lambda p1 ... pn -> e:
        L, p1: T1, ..., pn: Tn |- e: T
        ---
        L |- Lambda p1 ... pn -> e: T1 -> ... -> Tn -> T

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
        L |- Signal e: T

    Try i1 i2:
        L |- i1: T  L |- i2: String -> T
        ---
        Try i1 i2: T
 */

fun inferAndBindValueType(letValue: LetValue, errors: Errors, pump: VarPump, environment: Environment): Type {
    val type = inferValueType(nullTypeToType(letValue.type), letValue.expression, pump, environment, errors)

    environment.add(letValue.identifier.name, typeToScheme(type))

    return type
}

fun inferValueType(type: Type?, e: Expression, pump: VarPump, environment: Environment = Environment(), errors: Errors): Type {
    val inferType = InferType(pump, environment, errors)
    val resultType = inferType.expression(e)

    inferType.addConstraint(type, resultType)

    val subst = inferType.constraints.solve(errors)

    return resultType.apply(subst)
}

fun inferAndBindProcedureType(letFunction: LetFunction, errors: Errors, pump: VarPump, environment: Environment): Scheme {
    val valueType = inferProcedureType(
        letFunction.identifier.name,
        letFunction.parameters.map { Tuple2(it.id.name, nullTypeToType(it.type)) },
        nullTypeToType(letFunction.returnType),
        letFunction.expression,
        pump,
        environment,
        errors
    )

    val scheme = typeToScheme(valueType)

    environment.add(letFunction.identifier.name, scheme)

    return scheme
}

private fun inferProcedureType(
    name: String?,
    parameters: List<Tuple2<String, Type?>>,
    definedReturnType: Type?,
    e: Expression,
    pump: VarPump,
    environment: Environment,
    errors: Errors
): Type {
    val parameterTypes = parameters.map { Pair(it.a, it.b ?: pump.fresh()) }
    val returnType: Type = definedReturnType ?: pump.fresh()
    val procedureType = parameterTypes.foldRight(returnType) { t, acc -> TArr(t.second, acc) }

    environment.openScope()
    if (name != null) {
        environment.add(name, procedureType)
    }
    parameterTypes.forEach { environment.add(it.first, it.second) }

    val inferType = InferType(pump, environment, errors)
    val inferredReturnType = inferType.expression(e)

    inferType.addConstraint(returnType, inferredReturnType)
    val unificationResult = inferType.constraints.solve(errors)
    environment.closeScope()

    return procedureType.apply(unificationResult)
}

class InferType(
    private val pump: VarPump,
    private val environment: Environment,
    private val errors: Errors
) {
    var constraints = Constraints()

    fun expressions(es: List<Expression>): Type {
        val ts = es.map { expression(it) }

        return if (ts.isEmpty()) typeUnit else ts.last()
    }

    fun expression(e: Expression): Type = when (e) {
        is ApplyExpression -> {
            val esTypes = e.expressions.map { expression(it) }

            val fType = esTypes[0]
            val argTypes = esTypes.drop(1)

            val resultType: Type = pump.fresh()

            addConstraint(fType, argTypes.foldRight(resultType) { type, acc -> TArr(type, acc) })

            resultType
        }

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

        is BlockExpression -> expressions(e.expressions)

        is Identifier -> when (val result = environment.type(e.name)) {
            is Union2a -> result.a().withPosition(e.position)
            is Union2b -> result.b().instantiate(pump).withPosition(e.position)
            else -> {
//                errors.add(UnknownSymbolError(e.name, e.position))
                typeError
            }
        }

        is IfExpression -> {
            val ifThenTypes = e.ifThenExpressions.map { Tuple2(expression(it.a), expression(it.b)) }
            ifThenTypes.forEach {
                addConstraint(it.a, typeBool)
            }

            if (e.elseExpression == null) {
                typeUnit
            } else {
                val elseType = expression(e.elseExpression)

                ifThenTypes.forEach {
                    addConstraint(it.b, elseType)
                }

                elseType
            }.withPosition(e.position)
        }

        is LambdaExpression ->
            inferProcedureType(
                null,
                e.parameters.map { Tuple2(it.id.name, nullTypeToType(it.type)) },
                nullTypeToType(e.returnType),
                e.expression,
                pump,
                environment,
                errors
            ).withPosition(e.position)

        is LetFunction -> inferAndBindProcedureType(e, errors, pump, environment).instantiate(pump)

        is LetValue -> inferAndBindValueType(e, errors, pump, environment)

        is LiteralS32 -> typeS32.withPosition(e.position)

        is LiteralString -> typeString.withPosition(e.position)

        is LiteralUnit -> typeUnit.withPosition(e.position)

        is SignalExpression -> {
            val eType = expression(e.expression)
            addConstraint(eType, typeString)

            pump.fresh(e.position)
        }

        is TryExpression -> {
            val bodyT = expression(e.body)
            val catchT = expression(e.catch)

            addConstraint(TArr(typeString, bodyT), catchT)

            bodyT
        }

        is TypedExpression -> {
            val type = expression(e.expression)

            addConstraint(type, typeToType(e.type))

            type
        }
    }

    fun addConstraint(t1: Type?, t2: Type?) {
        if (t1 != null && !t1.isError() && t2 != null && !t2.isError() && t1 != t2)
            constraints.add(t1, t2)
    }
}

private fun Type.isError(): Boolean = when (this) {
    is TArr -> this.domain.isError() || this.range.isError()
    is TCon -> this.name == typeError.name
    is TVar -> false
}