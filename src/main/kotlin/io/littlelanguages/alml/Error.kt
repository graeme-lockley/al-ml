package io.littlelanguages.alml

import io.littlelanguages.alml.static.TToken
import io.littlelanguages.alml.static.Token
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location

sealed interface Errors : Yamlable

data class ParseError(
    val found: Token, val expected: Set<TToken>
) : Errors {
    override fun yaml(): Any = singletonMap(
        "ParseError", mapOf(
            Pair("found", found), Pair("expected", expected)
        )
    )
}

data class CompilationError(override val message: String) : Exception(message), Errors {
    override fun yaml(): Any = singletonMap("CompilationError", message)
}

data class ArgumentMismatchError(val name: String, val expected: Int, val actual: Int, val location: Location) : Errors {
    override fun yaml(): Any = singletonMap(
        "ArgumentMismatchError", mapOf(
            Pair("name", name), Pair("expected", expected), Pair("actual", actual), Pair("location", location)
        )
    )
}

data class DuplicateParameterNameError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any = singletonMap(
        "DuplicateParameterNameError", mapOf(
            Pair("name", name), Pair("location", location)
        )
    )
}

data class DuplicateNameError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any = singletonMap(
        "DuplicateNameError", mapOf(
            Pair("name", name), Pair("location", location)
        )
    )
}

data class ExpressionNotProcedureError(val location: Location) : Errors {
    override fun yaml(): Any = singletonMap("ExpressionNotProcedureError", location.yaml())
}

data class UnificationFail(
    val t1: Type, val t2: Type
) : Errors {
    override fun yaml(): Any = singletonMap("UnificationFailError", mapOf(Pair("t1", t1.yaml()), Pair("t2", t2.yaml())))
}

data class UnificationMismatch(
    val t1s: Collection<Type>, val t2s: Collection<Type>
) : Errors {
    override fun yaml(): Any = singletonMap("UnificationMismatchError", t1s.map { it.yaml() })
}


data class UnknownSymbolError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any = singletonMap(
        "UnknownSymbolError", mapOf(
            Pair("name", name), Pair("location", location)
        )
    )
}
