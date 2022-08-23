# al-ml

Al-ML is an implementation of an ML flavoured language with the following characteristics:

- Statically typed
- Higher-ordered functions
- Closures
- Abstract data types
- Fixed records
- Modules
- C-FFI (Foreign Function Interface)

The implementation is built in Kotlin and compiles programs to binary code using LLVM.

This implementation is seeded from <https://github.com/littlelanguages/ll-mini-ilisp-kotlin-llvm>.

## What to expect

This implementation will evolve as I have more fun with it.  Essentially though it'll go through the following phases:

- [X] Reshape the code base into something that is a little tighter
- [X] Transform the supported language from mini-ilisp to a small dialect Al-ML
- [ ] Transform the runtime system so that it uses untagged values
- [ ] Extend the small dialect of Al-ML to include the full Al-ML language
- [ ] Add modules
- [ ] Add support for C-FFI

At that point I would then like to rewrite the entire compiler in Al-ML and have it bootstrap itself.

I maintain a [TODO](./TODO.md) to highlight what I am actively working on.  

## Building this code base

I have placed the entire build process into `./tasks/dev`.  This script captures the sequence and the individual tasks to build the entire code base including the samples from `./src/samples`.
