package io.littlelanguages.alml.compiler

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.compiler.llvm.Context
import io.littlelanguages.alml.compiler.llvm.targetTriple
import io.littlelanguages.alml.dynamic.Binding
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.yaml.snakeyaml.Yaml
import java.io.*


private val yaml = Yaml()

class CompilerTests : FunSpec({
    context("Conformance Tests") {
        val context = Context(targetTriple())
        val content = File("./src/test/kotlin/io/littlelanguages/alml/compiler/compiler.yaml").readText()

        val scenarios: Any = /*emptyList<String>() */ yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(builtinBindings, context, this, scenarios)
        }

        context.dispose()
    }
})

suspend fun parserConformanceTest(
    builtinBindings: List<Binding<CompileState, LLVMValueRef>>,
    context: Context,
    ctx: FunSpecContainerContext,
    scenarios: List<*>
) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(name) {
                val errors = Errors()
                val module = compile(builtinBindings, context, input.reader(), "./test.mlsp", errors)

                if (errors.reported())
                    errors.items().map { it.yaml() }.toString() shouldBe output.toString()
                else {

//                        LLVM.LLVMDumpModule(module)
//                        System.err.println(LLVM.LLVMPrintModuleToString(module).string)
                    module.writeBitcodeToFile("test.bc")
                    runCommand(arrayOf("clang", "test.bc", "src/main/c/lib.o", "./src/main/c/main.o", "./build/bdwgc/libgc.a", "-o", "test.bin"))
                    val commandOutput = runCommand(arrayOf("./test.bin"))

                    module.dispose()

                    commandOutput shouldBe (output as Any).toString().trim()
                }
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(name) {
                parserConformanceTest(builtinBindings, context, this, tests)
            }
        }
    }
}

private fun runCommand(commands: Array<String>): String {
    val rt = Runtime.getRuntime()
    val proc = rt.exec(commands)

    val sb = StringBuffer()

    fun readInputStream(input: InputStream) {
        BufferedReader(InputStreamReader(input)).use { reader ->
            var s: String?
            while (reader.readLine().also { s = it } != null) {
                sb.append(s)
                sb.append("\n")
            }
        }
    }

    readInputStream(proc.inputStream)
    readInputStream(proc.errorStream)

    return sb.toString().trim()
}