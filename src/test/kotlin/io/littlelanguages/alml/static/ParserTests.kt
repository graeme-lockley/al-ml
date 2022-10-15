package io.littlelanguages.alml.static

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.alml.Errors
import org.yaml.snakeyaml.Yaml
import java.io.File

private val yaml = Yaml()


class ParserTests : FunSpec({
    context("Conformance Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/alml/static/parser.yaml").readText()

        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(this, scenarios)
        }
    }
})

suspend fun parserConformanceTest(ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(name) {
                val errors = Errors()

                val lhs = parse(input, errors)
                val rhs = output.toString()

                if (errors.reported())
                    errors.items().map { it.yaml() }.toString() shouldBe rhs
                else
                    lhs.yaml().toString() shouldBe rhs
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
