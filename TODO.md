# Reshape the code base

Move the code base into a style that is less cluttered and easier to work on.  This shape does not consider any bootstrapping - this will follow later.

## Housekeeping

- [X] Move ./bdwgc into ./build/bdwgc
- [X] Have ./ll-mini-ilisp-kotlin-llvm renamed to ./dist and move into ./build
- [X] Move ~/samples into ~/src/samples
- [ ] Move ./.bin to ./tasks
- [ ] Create the task "dev-clean" with an "all" option
- [ ] Add lint tasks for bash scripts and for markdown and embed into the build pipeline

## Reshape Source

- [ ] Rename .mil. package to .alml. - this will be in both main and test

