import ply.yacc as yacc 
import ply_lexer
tokens = ply_lexer.tokens
start = "main"

def p_main (p):
    '''main : stmt
            | stmt main
    '''
    if p[0] == None:
        p[0] = []
    p[0].append(p[1])
    if len(p) == 3:
        p[0] += p[2]

def p_statement (p):
    '''stmt : var
            | push
            | pop
            | kw
            | kw_name
    '''
    p[0] = p[1]

#
# Variables
#

def p_i_var (p):
    "var : KW_VAR var_type NAME"
    p[0] = [p[1], p[2], p[3]]

def p_var_type (p):
    '''var_type : KW_INT
                | KW_CHAR
                | KW_ARR CH_LBRACKET var_type CH_RBRACKET
    '''
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = p[1] + "," + p[3]

#
# Stack manipulation
#

def p_i_push (p):
    '''push : KW_PUSH NAME
            | KW_PUSH INT_VALUE
            | KW_PUSH CHAR_VALUE
    '''
    try:
        p[0] = [p[1], int(p[2])]
    except:
        p[0] = [p[1], p[2]]

def p_i_pop (p):
    '''pop : KW_POP
           | KW_POP INT_VALUE
    '''
    if len(p) == 3:
        p[0] = [p[1], p[2]]
    else:
        p[0] = [p[1], None]
#
# KW
#

def p_KW (p):
    '''kw : KW_RET
          | KW_ADD
          | KW_SUB
          | KW_MUL
          | KW_DIV
          | KW_DUP
          | KW_SWAP
          | KW_PRINTTOP
          | KW_PRINTSTACK
    '''
    p[0] = [p[1], None]

#
# KW NAME
#

def p_KW_NAME (p):
    '''kw_name : KW_LBL NAME
               | KW_CALL NAME
               | KW_JMP NAME
               | KW_JE NAME
               | KW_JNE NAME
               | KW_JG NAME
               | KW_JGE NAME
               | KW_JL NAME
               | KW_JLE NAME
               | KW_ARRPUSH NAME
               | KW_ARRPOP NAME
               | KW_ARRGET NAME
               | KW_ARRSET NAME
               | KW_VARSET NAME
               | KW_VARDEL NAME
    '''
    p[0] = [p[1], p[2]]

def build ():
    return yacc.yacc();