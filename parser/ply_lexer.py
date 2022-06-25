from winreg import KEY_QUERY_VALUE
import ply.lex as lex

keywords = [
    # Variable type
    "int", "char", "arr",
    # Debug
    "printtop", "printstack",
    # Variable
    "var", "varset", "vardel",
    # Stack manipulation
    "push", "pop", "dup", "swap",
    # Control flow
    "lbl", "call", "ret",
    "jmp", "je", "jne", "jg", "jge", "jl", "jle",
    # Maths
    "add", "sub", "mul", "div",
    # Arrays
    "arrpush", "arrpop", "arrget", "arrset"
]

tokens = [
    "NAME",
    "INT_VALUE",
    "CHAR_VALUE",
    "CH_LBRACKET", "CH_RBRACKET",
] + list(map(lambda kw: "KW_" + kw.upper(), keywords))

t_ignore = " \t\n"
t_CH_LBRACKET = r"\["
t_CH_RBRACKET = r"\]"

t_INT_VALUE = r"\d+"
t_CHAR_VALUE = r"'.'"

def t_NAME (t):
    r"[_a-zA-Z][_a-zA-Z0-9]*"
    if t.value in keywords:
        t.type = "KW_" + t.value.upper()
    return t

lex.lex()