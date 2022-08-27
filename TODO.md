# TODO

## Transform the supported language from mini-ilisp to a small dialect Al-ML

Piece by piece transform mini-iLisp into Al-ML.  The intention here is to re-use the compiler and the generation of closures as far as possible.

- [X] Change comments to start with // and extend to end of line
- [X] Introduce semicolons between expressions at a top-level
- [X] Replace `(do ...)` form with `{ ... }`
- [X] Change the structure so that `{ ... }` is an expression block rather than passing `Array<Expression>` around.
- [X] Replace `const` with `let` and move it into expression
- [X] Reshape expressions into a precedence hierarchy
- [X] Move `if` out of s-expression into Al-ML
- [X] Refactor if AST so that it is understandable
- [X] Move `+` and `-` to binary op
- [X] Move `*` and `/` to binary op
- [X] Move `==`, `!=`, `<`, `<=`, `>`, `>=` to binary op
- [X] Move lambda expression
- [X] Move call expression

## Transform the runtime system so that it uses untagged values

Superficially this is straight forward however it is necessary to introduce type expressions and free variables.  Further to that it'll be necessary to allow type signatures to be specified.

- [X] Enhance the grammar to include type info
- [X] Extend the parser to parse the type detail and augment the AST
- [ ] Type every TST expression

## Extend the small dialect of Al-ML to include the full Al-ML language

- [ ] Add support for custom infix operators
- [ ] Add support for `|>` and `<|`
- [ ] Add support for `(..)` for all infix operators
- [ ] Add support for unary `-` and `+`

- [ ] Defect with comments - see euler-001 sample
