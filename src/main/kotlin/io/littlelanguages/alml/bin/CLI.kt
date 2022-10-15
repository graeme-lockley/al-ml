package io.littlelanguages.alml.bin

import io.littlelanguages.alml.*
import io.littlelanguages.alml.compiler.CompileState
import io.littlelanguages.alml.compiler.builtinBindings
import io.littlelanguages.alml.compiler.llvm.Context
import io.littlelanguages.alml.compiler.llvm.Module
import io.littlelanguages.alml.compiler.llvm.targetTriple
import io.littlelanguages.alml.dynamic.Binding
import io.littlelanguages.alml.static.Scanner
import io.littlelanguages.alml.static.TToken
import io.littlelanguages.alml.static.Token
import io.littlelanguages.alml.static.parse
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange
import org.bytedeco.llvm.LLVM.LLVMValueRef
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.io.FileReader
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun compile(builtinBindings: List<Binding<CompileState, LLVMValueRef>>, context: Context, input: File): Either<List<Error>, Module> {
    val reader = FileReader(input)

    val result = parse(Scanner(reader)) mapLeft { listOf(it) } andThen { io.littlelanguages.alml.typed.translate(it) } andThen {
        io.littlelanguages.alml.dynamic.translate(
            builtinBindings,
            it
        )
    } andThen {
        io.littlelanguages.alml.compiler.compile(
            context,
            input.name,
            it
        )
    }
    reader.close()

    return result
}

fun changeExtension(f: File, newExtension: String): File {
    val i = f.name.lastIndexOf('.')
    val name = f.name.substring(0, i)
    return File(f.parent, name + newExtension)
}

fun reportErrors(errors: List<Error>) {
    errors.forEach { println(formatError(it)) }
}

fun formatTToken(token: TToken): String =
    when (token) {
        TToken.TBangEqual -> "'!='"
        TToken.TBackslash -> "'\\'"
        TToken.TBar -> "'|'"
        TToken.TColon -> "':'"
        TToken.TDash -> "'-'"
        TToken.TDashGreaterThan -> "'->'"
        TToken.TElse -> "'else'"
        TToken.TEqual -> "'='"
        TToken.TEqualEqual -> "'=='"
        TToken.TGreaterThan -> "'>'"
        TToken.TGreaterThanEqual -> "'>='"
        TToken.TIf -> "'if'"
        TToken.TLCurly -> "'{'"
        TToken.TLessThan -> "'<'"
        TToken.TLessThanEqual -> "'<='"
        TToken.TLet -> "'let'"
        TToken.TLiteralInt -> "Literal Int"
        TToken.TLiteralString -> "Literal String"
        TToken.TLowerID -> "LowerID"
        TToken.TLParen -> "'('"
        TToken.TPlus -> "'+'"
        TToken.TRCurly -> "'}'"
        TToken.TRParen -> "')'"
        TToken.TSemicolon -> "';'"
        TToken.TSignal -> "'signal'"
        TToken.TSlash -> "'/'"
        TToken.TStar -> "'*'"
        TToken.TTry -> "'try'"
        TToken.TUpperID -> "UpperID"
        TToken.TEOS -> "End Of Stream"
        TToken.TERROR -> "<Error Token>"
    }

fun formatToken(token: Token): String =
    when (token.tToken) {
        TToken.TLiteralInt,
        TToken.TLiteralString,
        TToken.TLowerID,
        TToken.TUpperID -> "${formatTToken(token.tToken)} (${token.lexeme})"

        else -> formatTToken(token.tToken)
    }

fun formatLocation(location: Location): String =
    when (location) {
        is LocationCoordinate ->
            "(${location.line}, ${location.column})"

        is LocationRange ->
            if (location.start.line == location.end.line)
                "(${location.start.line}, ${location.start.column}-${location.end.column})"
            else
                "(${location.start.line}, ${location.start.column})-(${location.end.line}, ${location.end.column})"
    }

fun formatError(error: Error): String =
    when (error) {
        is ArgumentMismatchError ->
            "Argument Mismatch: ${formatLocation(error.location)}: Procedure \"${error.name}\" expects ${error.expected} ${if (error.expected == 1) "argument" else "arguments"} but was passed ${error.actual}"

        is CompilationError ->
            "Compilation Error: ${error.message}"

        is DuplicateNameError ->
            "Duplicate Name: ${formatLocation(error.location)}: Attempt to redefine \"${error.name}\""

        is DuplicateParameterNameError ->
            "Duplicate Parameter Name: ${formatLocation(error.location)}: Attempt to redefine \"${error.name}\""

        is ExpressionNotProcedureError ->
            "Expected Procedure Expression: ${formatLocation(error.location)}: try catch expression needs to be a procedure"

        is ParseError -> {
            val oneOfPhrase = if (error.expected.size == 1) "" else "one of "
            val expectedTokens = error.expected.joinToString { formatTToken(it) }
            val actualToken = formatToken(error.found)

            "Parse Error: ${formatLocation(error.found.location)}: Expected $oneOfPhrase$expectedTokens but found $actualToken"
        }

        is UnknownSymbolError ->
            "Unknown Symbol: ${formatLocation(error.location)}: Reference to unknown symbol \"${error.name}\""

        is UnificationFail ->
            "Unification Error: $error"

        is UnificationMismatch ->
            "Unification Mismatch: $error"
    }

fun compile(input: File, triple: String, output: File) {
    val context = Context(triple)

    when (val compiledResult = compile(builtinBindings, context, input)) {
        is Left -> {
            reportErrors(compiledResult.left)
            exitProcess(1)
        }

        is Right ->
            compiledResult.right.writeBitcodeToFile(output.absolutePath)
    }
    context.dispose()
}

@Command(name = "almi", version = ["0.1"], mixinStandardHelpOptions = true, description = ["An Al-ML compiler."])
class CLI : Callable<Int> {
    @Parameters(paramLabel = "FILE", description = ["File to compile.  File must exist and have a .mlsp extension."], arity = "1")
    private lateinit var file: File

    @CommandLine.Option(names = ["-t", "--triple"], paramLabel = "TRIPLE", description = ["Module target triple embedded into the compiled code."])
    private var triple = targetTriple()

    private fun failOnError(error: String) {
        println("Error: $error")
        exitProcess(1)
    }

    override fun call(): Int {
        // Take file and compile with defaults to a .bc file.  File must have a .mlsp extension and the extension is changed to a .o
        if (!file.canRead())
            failOnError("Invalid input file: $file is not readable")
        if (file.extension != "mlsp")
            failOnError("Invalid input file: $file requires a .mlsp extension")

        compile(file, triple, changeExtension(file, ".bc"))

        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(CLI()).execute(*args))
}