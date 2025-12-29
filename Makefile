SRC = $(shell find src/ -type f -name '*.java')
ROOT_DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))

SPEC_PROTO = $(wildcard spec/proto/*.proto)
SPEC_GEN   = $(patsubst spec/proto/%.proto,spec/gen/%.pb.cr,$(SPEC_PROTO))
SPEC_TXTPB = $(wildcard spec/text/*.txtpb)
SPEC_BINPB = $(patsubst spec/text/%.txtpb,spec/binary/%.binpb,$(SPEC_TXTPB))

.PHONY: all spec clean

all: bin/protocr

bin/protocr: $(SRC) build.gradle.kts settings.gradle.kts Makefile
	@mkdir -p $(@D)
	./gradlew --no-daemon installDist
	rm -f bin/protocr
	ln -s $(ROOT_DIR)/build/install/protocr/bin/protocr bin/protocr
	touch bin/protocr

spec/binary/%.binpb: spec/text/%.txtpb $(SPEC_PROTO) $(SPEC_TXTPB)
	@mkdir -p $(@D)
	protoc -I=spec/proto/ spec/proto/*.proto --encode=protocr.spec.$(basename $(basename $(@F))) < $< > $@~
	mv $@~ $@

spec/gen/%.pb.cr: spec/proto/%.proto bin/protocr
	@mkdir -p $(@D)
	protoc -I=spec/proto/ --crystal_out=spec/gen/ --plugin=protoc-gen-crystal=bin/protocr $<

spec: bin/protocr $(SPEC_BINPB) $(SPEC_TXTPB) $(SPEC_PROTO) $(SPEC_GEN)
	crystal spec -Dprotocr_included

clean:
	rm -f bin/protocr
	rm -f $(SPEC_BINPB)
	rm -f $(SPEC_GEN)
