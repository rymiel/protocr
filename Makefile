SRC = $(shell find src/ -type f -name '*.java')
ROOT_DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))

bin/protocr: $(SRC) build.gradle.kts settings.gradle.kts Makefile
	mkdir -p $(@D)
	./gradlew installDist
	rm -f bin/protocr
	ln -s $(ROOT_DIR)/build/install/protocr/bin/protocr bin/protocr