package io.littlelanguages.alml.typed

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.alml.Error
import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.static.Scanner
import io.littlelanguages.alml.static.parse
import io.littlelanguages.alml.typed.st.Program
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

private val yaml = Yaml()

class TypedTests : FunSpec({
    context("Typed Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/alml/typed/typing.yaml").readText()

        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(this, scenarios)
        }
    }
})

fun translate(input: String): Either<List<Error>, Program> =
    parse(Scanner(StringReader(input))) mapLeft { listOf(it) } andThen {
        val errors = Errors()
        val r = translate(it, errors)

        if (errors.reported()) Left(errors.items()) else Right(r)
    }

suspend fun parserConformanceTest(ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]
//            val constraints = s["constraints"]
//            val environment = s["environment"]

            ctx.test(name) {
                val lhs =
                    translate(input)

                if (output != null) {
                    val rhs =
                        output.toString()

                    when (lhs) {
                        is Left ->
                            lhs.left.map { it.yaml() }.toString() shouldBe rhs

                        is Right ->
                            lhs.right.yaml().toString() shouldBe rhs
                    }
                }

//                if (constraints != null) {
//                    val ait = AssignInferredType<S, T>(false)
//
//                    when (lhs) {
//                        is Left -> lhs.left.map { it.yaml() }.toString() shouldBe ""
//
//                        is Right -> {
//                            ait.program(lhs.right)
//
//                            ait.constraints.state.map { it.toString() } shouldBe constraints
//                        }
//                    }
//                }

//                if (environment != null) {
//                    val ait = AssignInferredType<S, T>(false)
//
//                    when (lhs) {
//                        is Left -> lhs.left.map { it.yaml() }.toString() shouldBe ""
//
//                        is Right -> {
//                            ait.program(lhs.right)
//
//                            ait.environment.types().map { it.toString() } shouldBe environment
//                        }
//                    }
//                }
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(name) {
                parserConformanceTest(this, tests)
            }
        }
    }
}
