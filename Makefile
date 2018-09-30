.PHONY: compile

compile:
	clj -A:compile -m figwheel.main --build-once dev

dist: clean compile

clean:
	rm -rf ./target
