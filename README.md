# Terminology

- `number` — /`[0-9]+`/
- `name`   — /`[_a-zA-Z][_a-zA-Z0-9]*`/
- `intvalue`  — Either `number` or `name` (variable of type `int` or `char`)
- `type`
  - `int`  — 4-byte integer
  - `char` — 1-byte integer
  - `arr`  — array of either `int`, `char` or `arr[type]`

# Instructions

##### Debug

| Instruction | Description |
| :---------: | :---------: |
| printtop    | Print out the top value |
| printstack  | Print out the entire stack |

##### Variables

| Instruction | Description |
| :---------: | :---------: |
| var `type` `name` | Declare a variable of given `name` and `type` |
| varset `name` | Set the value of variable `name` to the value of the top element of the stack |
| vardel `name` | Delete variable `name` |

##### Stack manipulation

| Instruction | Description |
| :---------: | :---------: |
| push `intvalue` | Push a `intvalue` onto the top of the stack |
| pop `number` | Remove the top `number` elements of the stack <br> 📝 No other instruction can pop a value off the stack |
| dup | Duplicate the top element of the stack (push a copy of it) | — | dup |
| swap | Swap the top 2 elements of the stack |

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
| mul | Multiply the top two values on the stack and push the result |
| div | Divide the top value on the stack by the second-top value and push the result |

##### Arrays

`n` = number of dimensions
`val` = top `n+1`th element of the stack
`idx`<sub>1</sub> = top `n`th element of the stack
`idx`<sub>n</sub> = top element of the stack

<table>
    <tr>
        <th>Instruction</th>
        <th>Description</th>
    </tr>
    <tr>
        <td> arrpush <code>name</code> </td>
        <td> Push the top element of the stack into the array <code>name</code> </td>
    </tr>
    <tr>
        <td> arrpop <code>name</code> </td>
        <td> Discard the last element of array <code>name</code> </td>
    </tr>
    <tr>
        <td> arrget <code>name</code> </td>
        <td>
            <center>
                Get the value of the element under indices <code>idx</code><sub>1</sub> ... <code>idx</code><sub>n</sub> in array <code>name</code> <br><br>
                Example code <br>
            </center>
            <pre>
                var arr[arr[char]] myarr
                push 3
                push 7
                arrget myarr
            </pre>
            <center>
                C Equivalent
            </center>
            <pre>
                int myarr[10][10];
                int x = myarr[3][7];
            </pre>
        </td>
    </tr>
    <tr>
        <td> arrset <code>name</code> </td>
        <td>
            <center>
                Set the value of the element under indices <code>idx</code><sub>1</sub> ... <code>idx</code><sub>n</sub> in array <code>name</code> to <code>val</code> <br><br>
                Example Code <br>
            </center>
            <pre>
                var arr[arr[arr[int]]] myarr
                push 69
                push 4
                push 2
                push 0
                arrset myarr
            </pre>
            <center>
                C Equivalent
            </center>
            <pre>
                int myarr[10][10][10];
                myarr[4][2][0] = 69;
            </pre>
        </td>
    </tr>
</table>