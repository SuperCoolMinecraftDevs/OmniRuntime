;; A module that breaks the contract on purpose, so the host's checks can be tested
;; against something rather than assumed. Assembled with:
;;   wasm-tools parse hostile.wat -o hostile.wasm

(module
  (memory (export "memory") 1)

  (func (export "cabi_realloc") (param i32 i32 i32 i32) (result i32)
    i32.const 1024)

  ;; Returns a pointer far outside its own memory.
  (func (export "far") (param i32 i32) (result i32)
    i32.const 2147483640)

  ;; Returns a valid pointer to a pair claiming a string of absurd length.
  (func (export "liar") (param i32 i32) (result i32)
    i32.const 8
    i32.const 0
    i32.store
    i32.const 12
    i32.const 2147483647
    i32.store
    i32.const 8)

  ;; Returns a pair pointing at bytes that are not valid UTF-8.
  (func (export "mangled") (param i32 i32) (result i32)
    i32.const 100
    i32.const 255
    i32.store8
    i32.const 8
    i32.const 100
    i32.store
    i32.const 12
    i32.const 1
    i32.store
    i32.const 8)

  ;; Returns a negative offset.
  (func (export "negative") (param i32 i32) (result i32)
    i32.const -16)
)
