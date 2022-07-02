# Lamp

Minecraft Carpet standalone vanilla command-block CPU generator.

Programs are written in JSON, so it is possible to create some kind of more advanced compiler for it (just in theory)

# Instructions

- `number` — `[0-9]+`
- `char` — `'.'`
- `name`   — `[_a-zA-Z][_a-zA-Z0-9]*`
- `value`  — Either `number`, `char` or `name` (constant)
- `idx` — `value`, 0-based index from the bottom of the stack
- `A` - top value on the stack
- `B` - second-top value on the stack
- `type`
  - `int`  — 4-byte integer
  - `char` — 1-byte integer
  - `arr`  — array of either `int`, `char` or `arr[type]`

<!-- TODO: const -->

##### Stack manipulation

| Instruction | Description |
| :---------: | :---------: |
| push `value` | Push a `value` onto the top of the stack |
| pop `value`  | Discard the top `value` elements of the stack <br> 📝 No other instruction can pop a value off the stack |
| get  | `idx` Push the `idx`th element from stack bottom to top |
| set | `value` `idx` Set the `idx`th element from the bottom of the stack to the top element of the stack |
<!-- | del | `idx` Push the `idx`th element from stack bottom to top | -->

##### Control flow

| Instruction | Description |
| :---------: | :---------: |
| lbl `name`  | Define a new label of ID `name`
| call `name` | Call the subroutine at label `name`
| ret         | Return from the current subroutine
| jmp `name`  | Jump to label `name`
| je `name`   | Jump to label `name` if A == B
| jne `name`  | Jump to label `name` if A != B
| jg `name`   | Jump to label `name` if A >  B
| jge `name`  | Jump to label `name` if A >= B
| jl `name`   | Jump to label `name` if A <  B
| jle `name`  | Jump to label `name` if A <= B

##### Maths

| Instruction | Description |
| :---------: | :---------: |
| add         | Push A + B  |
| sub         | Push A - B  |
<!-- TODO: mul, div -->
