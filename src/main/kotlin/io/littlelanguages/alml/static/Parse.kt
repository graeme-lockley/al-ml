package io.littlelanguages.alml.static

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.ParseError
import io.littlelanguages.alml.static.ast.*
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Tuple4
import io.littlelanguages.data.Union2
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable
import java.io.Reader

fun parse(scanner: Scanner, errors: Errors): Program = try {
    Parser(scanner, ParseVisitor()).program()
} catch (e: ParsingException) {
    errors.report(ParseError(e.found, e.expected))
    Program(listOf())
}

fun parse(reader: Reader, errors: Errors): Program =
    parse(Scanner(reader), errors)

class ParseVisitor :
    Visitor<
            Program,
            List<Expression>,
            Expression,
            TypedIdentifier,
            Expression,
            Expression,
            Expression,
            Expression,
            BinaryOperator,
            Expression,
            Expression,
            Expression,
            Expression,
            Type,
            Type,
            Type> {
    override fun visitProgram(a: List<Expression>): Program = Program(a)

    override fun visitExpressions(a1: Expression, a2: List<Tuple2<Token, Expression>>): List<Expression> =
        listOf(a1) + a2.map { it.b }

    override fun visitExpression1(a1: Token, a2: Token, a3: List<TypedIdentifier>, a4: Tuple2<Token, Type>?, a5: Token, a6: Expression): Expression =
        if (a3.isEmpty())
            LetValue(a1.location + a6.position(), Identifier(a2.location, a2.lexeme), a4?.b, a6)
        else
            LetFunction(a1.location + a6.position(), Identifier(a2.location, a2.lexeme), a3, a4?.b, a6)

    override fun visitExpression2(a: Expression): Expression =
        a

    override fun visitTypedIdentifier1(a1: Token, a2: Token, a3: Token, a4: Type, a5: Token): TypedIdentifier =
        TypedIdentifier(a1.location + a5.location, Identifier(a2.location, a2.lexeme), a4)

    override fun visitTypedIdentifier2(a: Token): TypedIdentifier =
        TypedIdentifier(a.location, Identifier(a.location, a.lexeme), null)

    override fun visitIfExpression1(
        a1: Token,
        a2: Expression,
        a3: Token,
        a4: Expression,
        a5: List<Tuple4<Token, Expression, Token, Expression>>,
        a6: Tuple2<Token, Expression>?
    ): Expression {
        val location =
            a1.location + (a6?.b?.position() ?: if (a5.isEmpty()) a4.position() else a5.last().d.position())

        return IfExpression(location, listOf(Tuple2(a2, a4)) + a5.map { Tuple2(it.b, it.d) }, a6?.b)
    }

    override fun visitIfExpression2(a: Expression): Expression =
        a

    override fun visitLambdaExpression1(
        a1: Token,
        a2: TypedIdentifier,
        a3: List<TypedIdentifier>,
        a4: Tuple2<Token, Type>?,
        a5: Token,
        a6: Expression
    ): Expression =
        LambdaExpression(a1.location + a6.position(), listOf(a2) + a3, a4?.b, a6)

    override fun visitLambdaExpression2(a: Expression): Expression =
        a

    override fun visitRelationalExpression(a1: Expression, a2: Tuple2<BinaryOperator, Expression>?): Expression =
        if (a2 == null) a1 else BinaryOpExpression(a1.position() + a2.b.position(), a1, a2.a, a2.b)

    override fun visitRelationalOp1(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.Equals)

    override fun visitRelationalOp2(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.NotEquals)

    override fun visitRelationalOp3(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.LessThan)

    override fun visitRelationalOp4(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.LessEquals)

    override fun visitRelationalOp5(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.GreaterThan)

    override fun visitRelationalOp6(a: Token): BinaryOperator =
        BinaryOperator(a.location, Operators.GreaterEquals)

    override fun visitTypedTerm(a1: Expression, a2: Tuple2<Token, Type>?): Expression =
        if (a2 == null) a1 else TypedExpression(a1.position() + a2.b.position(), a1, a2.b)

    override fun visitMultiplicativeExpression(a1: Expression, a2: List<Tuple2<Union2<Token, Token>, Expression>>): Expression =
        a2.fold(
            a1
        ) { acc, opExpr ->
            BinaryOpExpression(
                acc.position() + opExpr.b.position(),
                acc,
                if (opExpr.a.isA()) BinaryOperator(opExpr.a.a().location, Operators.Multiply) else BinaryOperator(
                    opExpr.a.b().location,
                    Operators.Divide
                ),
                opExpr.b
            )
        }

    override fun visitAdditiveExpression(a1: Expression, a2: List<Tuple2<Union2<Token, Token>, Expression>>): Expression =
        a2.fold(
            a1
        ) { acc, opExpr ->
            BinaryOpExpression(
                acc.position() + opExpr.b.position(),
                acc,
                if (opExpr.a.isA()) BinaryOperator(opExpr.a.a().location, Operators.Plus) else BinaryOperator(opExpr.a.b().location, Operators.Minus),
                opExpr.b
            )
        }

    override fun visitCallExpression(a1: Expression, a2: List<Expression>): Expression =
        if (a2.isEmpty()) a1 else ApplyExpression(locationOf(a1.position, a2), listOf(a1) + a2)

    override fun visitTerm1(a1: Token, a2: Expression?, a3: Token): Expression =
        a2 ?: LiteralUnit(a1.location + a3.location)

    override fun visitTerm2(a: Token): Expression =
        LiteralS32(a.location, a.lexeme)

    override fun visitTerm3(a: Token): Expression =
        LiteralString(a.location, a.lexeme)

    override fun visitTerm4(a: Token): Expression =
        Identifier(a.location, a.lexeme)

    override fun visitTerm5(a: Token): Expression =
        Identifier(a.location, a.lexeme)

    override fun visitTerm6(a1: Token, a2: Expression, a3: Expression): Expression =
        TryExpression(a1.location + a3.position(), a2, a3)

    override fun visitTerm7(a1: Token, a2: Expression): Expression =
        SignalExpression(a1.location + a2.position(), a2)

    override fun visitTerm8(a1: Token, a2: Expression, a3: List<Tuple2<Token, Expression>>, a4: Token): Expression =
        if (a3.isEmpty()) a2 else BlockExpression(a1.location + a4.location, listOf(a2) + a3.map { it.b })

    override fun visitType(a1: Type, a2: List<Tuple2<Token, Type>>): Type =
        if (a2.isEmpty()) a1 else FunctionType(a1.position + a2.last().b.position(), listOf(a1) + a2.map { it.b })

    override fun visitADTType1(a1: Token, a2: List<Type>): Type =
        AbstractDataType(if (a2.isEmpty()) a1.location else a1.location + a2.last().position(), Identifier(a1.location, a1.lexeme), a2)

    override fun visitADTType2(a: Type): Type =
        a

    override fun visitTermType1(a: Token): Type =
        VariableType(a.location, Identifier(a.location, a.lexeme))

    override fun visitTermType2(a1: Token, a2: Type?, a3: Token): Type =
        a2 ?: AbstractDataType(a1.location + a3.location, Identifier(a1.location + a3.location, "()"), emptyList())
}


private fun locationOf(loc: Location, ls: List<Locationable>): Location = ls.fold(loc) { l1, l2 -> l1 + l2.position() }
