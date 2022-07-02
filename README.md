# Lamp

Minecraft CarpetMod standalone turing-complete vanilla command-block array-based CPU generator

The generator is written in [scarpet](https://github.com/gnembon/fabric-carpet/tree/master/docs/scarpet) using [CarpetMod](https://github.com/gnembon/fabric-carpet)  
Assembly is written in JSON, so it is possible to create a compiler for it (just in theory 😳)

The assembly language is array-based and it provides
- basic stack manipulation instructions (`push` and `pop` instructions)
- primitive usage of "variables" (`get` and `set` instructions)
- basic arithmetics (`add` and `sub` instructions)
- conditional jumps
- subroutines  

# Sections

- [Synopsis](#Synopsis)
- [Instructions](#Instructions)
  - [Memory manipulation](#memory-manipulation)
  - [Control flow](#control-flow)
  - [Arithmetic](#arithmetic)
- [TO-DO](#to-do)

# Synopsis

The first 10 fibonacci numbers - [Video Showcase](https://www.youtube.com/watch?v=NvBEsQZ-E2s)
```as
jmp main
lbl loop
  pop //get 0
  add
  get 0 //10
  push 1
  sub
  set 0
  pop
  pop
  jnz loop
lbl main
  push 10
  push 1
  push 1
  push 69 //dummy for 1st pop in :loop
  jmp loop
```

# Instructions

- `number` — `[0-9]+`
- `char` — `'.'`
- `name`   — `[_a-zA-Z][_a-zA-Z0-9]*`
- `value`  — Either `number`, `char` or `name` (constant)
- `idx` — `value`, 0-based index from the bottom of the stack
- `[...]` - optional argument, if not present, taken from the stack
- `A` - top value on the stack
- `B` - second-top value on the stack

<!-- TODO: const -->

### Memory manipulation

| Instruction          | Description |
| :------------------: | :---------- |
| push `value`         | Push `value` onto the top of the stack
| pop                  | Discard the top element of the stack <br> 📝 No other instruction can pop a value off the stack
| get `[idx]`          | Push element at `idx` onto the top of the stack
| set `[idx] [value]`  | Set element at `idx` to `value`
<!-- TODO: del -->

### Control flow

| Instruction | Description |
| :---------: | :---------- |
| lbl `name`  | Define a new label of ID `name`
| call `name` | Call the subroutine at label `name`
| ret         | Return from the current subroutine
| jmp `name`  | Jump to label `name`
| je `name`   | Jump to label `name` if `A == B`
| jne `name`  | Jump to label `name` if `A != B`
| jg `name`   | Jump to label `name` if `A >  B`
| jge `name`  | Jump to label `name` if `A >= B`
| jl `name`   | Jump to label `name` if `A <  B`
| jle `name`  | Jump to label `name` if `A <= B`
| jz `name`   | Jump to label `name` if `A == 0`
| jnz `name`  | Jump to label `name` if `A != 0`

### Arithmetic

| Instruction | Description |
| :---------: | :---------- |
| add         | Push `A + B`
| sub         | Push `A - B`
<!-- TODO: mul, div -->

# TO-DO

- Clean up the scarpet code
- Clean up this readme
- Actually handle `char` cuz I don't think it's working
- Make conditional jumps pop off their `A`s and `B`s
- Create a `const` instruction for setting constants
- Create an `idx` instruction that pushes the top index
- Create `mul` and `div` instructions for multiplication and division
- Perhaps create a `del` instruction that would remove an element at a specified index
- Perhaps create a `put` instruction that would place a specified block given `x y z` coordinates
