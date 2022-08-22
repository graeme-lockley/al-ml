TARGETS=hello primes euler-001 divide-by-zero

all: $(TARGETS)

%: %.bc
	clang $< ../main/c/lib.o ../../build/bdwgc/libgc.a ../main/c/main.o -o $@

%.bc: %.mlsp
	../../build/dist/bin/al-ml $<

#	opt -O3 $@ > tmp
#	mv tmp $@
#	llvm-dis $@

clean:
	rm -f *.bc *.ll $(TARGETS)
