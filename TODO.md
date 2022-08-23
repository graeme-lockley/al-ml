# Transform the supported language from mini-ilisp to a small dialect Al-ML

Piece by piece transform mini-iLisp into Al-ML.  The intention here is to re-use the compiler and the generation of closures as far as possible.

- [X] Change comments to start with // and extend to end of line
- [X] Introduce semicolons between expressions at a top-level
- [X] Replace `(do ...)` form with `{ ... }`
- [X] Change the structure so that `{ ... }` is an expression block rather than passing `Array<Expression>` around.
- [X] Replace `const` with `let` and move it into expression
- [X] Reshape expressions into a precedence hierarchy
- [ ] Move `if` out of s-expression into Al-ML
- [ ] Move call expression
- [ ] Move `+` and `-`
- [ ] Move `*` and `/`
- [ ] Move `==`, `!=`, `<`, `<=`, `>`, `>=`
- [ ] Move `&&`, `||`

- [ ] Defect with comments - see euler-001 sample
