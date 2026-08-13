;; Source for add.wasm, kept alongside it so the fixture is readable.
;;
;; The binary is 41 bytes and is checked in rather than built, because pulling a
;; WebAssembly toolchain into the build for one addition would cost more than it
;; is worth. Guest fixtures that need a real toolchain are built in CI instead.

(module
  (func (export "add") (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add))
