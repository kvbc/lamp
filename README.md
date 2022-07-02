# Lamp

Minecraft Carpet standalone vanilla command-block CPU generator

# Terminology

- `number` — `[0-9]+`
- `char` — `'.'`
- `name`   — `[_a-zA-Z][_a-zA-Z0-9]*`
- `value`  — Either `number`, `char` or `name` (constant)
- `idx` — `value`
- `type`
  - `int`  — 4-byte integer
  - `char` — 1-byte integer
  - `arr`  — array of either `int`, `char` or `arr[type]`

# Instructions

TODO: this readme is terrible

<!-- ##### Other -->
<!-- | Instruction | Description | -->
<!-- | :---------: | :---------: | -->
<!-- | const `name` `value` | Define a constant of the given `name` and `value` -->

##### Stack manipulation

| Instruction | Description |
| :---------: | :---------: |
| push `value` | Push a `value` onto the top of the stack |
| pop `value` | Discard the top `value` elements of the stack <br> 📝 No other instruction can pop a value off the stack |
| get | `idx` Push the `idx`th element from stack bottom to top |
| set | `value` `idx` Set the `idx`th element from the bottom of the stack to the top element of the stack |
<!-- | del | `idx` Push the `idx`th element from stack bottom to top | -->

##### Control flow

| Instruction | Description |
| :---------: | :---------: |
| lbl `name`  | Define a new label of ID `name` |
| call `name` | Call the subroutine at label `name` |
| ret         | Return from the subroutine |
| jmp `name`  | Jump to label `name` |
| je `name`   | Jump to label `name` if the top two value values on the stack are equal to each other |
| jne `name`  | Jump to label `name` if the top two values on the stack are NOT equal to each other |
| jg `name`   | Jump to label `name` if the top value on the stack is greater than the second-top value |
| jge `name`  | Jump to label `name` if the top value on the stack is greater OR EQUAL to the second-top value |
| jl `name`   | Jump to label `name` if the top value on the stack is lesser than the second-top value |
| jle `name`  | Jump to label `name` if the top value on the stack is lesser OR EQUAL than the second-top value |

##### Maths

| Instruction | Description |
| :---------: | :---------: |
| add | Add the top two values on the stack and push the result |
| sub | Subtract the top value on the stack from the second-top value and push the result |
<!-- | mul | Multiply the top two values on the stack and push the result | -->
<!-- | div | Divide the top value on the stack by the second-top value and push the result | -->
