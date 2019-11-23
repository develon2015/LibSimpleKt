SHELL := /bin/bash
dir = out/production/LibSimpleKt
jar ?= simplekt.jar
opts ?= -include-runtime

.PHONY: ALL jar clean help test
ALL: jar

help:
	@echo '1. Generate a jar file $(jar), command: jar=simplekt.jar opts="-include-runtime" make jar'

# Generate a jar file $(jar), command: jar=simplekt.jar opts="-include-runtime" make jar
jar: $(jar)

$(jar): $(shell find src/lib -name '*.kt')
	kotlinc -d $@ $(opts) $^

$(dir)/test/MainKt.class: $(shell find src/test -name '*.kt') | $(jar)
	kotlinc -cp $(jar) $^

test: $(dir)/test/MainKt.class
	kotlin -cp $(jar):$(dir) test.MainKt

clean:
	rm -rf $(jar) $(dir)/*

