global_statements = read_file('data', 'json');
global_labels = {};
global_constants = {};
global_stack = []; // []
global_call_stack = [];

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Auxiliary functions
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

assert_type(x, tp) -> (
    if (type(tp) != 'string', 
        x = tp;
        tp = 'string';
    );
    if (type(x) != tp, throw(str('NOT A %s : "%s"', tp, str(x))));
);

assert_num  (x) -> assert_type(x, 'number');
assert_str  (x) -> assert_type(x, 'string');
assert_list (x) -> assert_type(x, 'list');
assert_map  (x) -> assert_type(x, 'map');
assert_bool (x) -> assert_type(x, 'bool');

assert_vector3(x) -> (
    assert_list(x);
    if (length(x) != 3, throw('LIST MUST HAVE EXACTLY 3 ELEMENTS : "' + str(x) + '"'));
);

extend_list(l1_, l2) -> (
    assert_list(l1_);
    l1 = copy(l1_);
    put(l1, null, l2, 'extend');
    return (l1);
);

merge_lists(l) -> (
    assert_list(l);
    c_for(i = 0, i < length(l), i += 1,
        if (type(l:i) == 'list',
            if (length(l:i) > 1, put(l, i + 1, slice(l:i,1), 'extend'));
            if (length(l:i) > 0, l:i = l:i:0);
        );
    );
    return (l);
);

safeget(l,i) -> (
    assert_list(l);
    if (i < length(l), return(l:i));
    return (null);
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

dir_from_list(dir) -> (
    assert_vector3(dir);

    if (dir:0 < 0, return('west'));
    if (dir:0 > 0, return('east'));
    if (dir:1 > 0, return('up'));
    if (dir:1 < 0, return('down'));
    if (dir:2 > 0, return('south'));
    if (dir:2 < 0, return('north'));

    return ('INVALID');
);

mcpos(pos) -> (
    assert_vector3(pos);
    return (join(' ', pos));
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

c = {
    'AIR' -> 'minecraft:air',
    'ON' -> 'minecraft:lime_wool',
    'OFF' -> 'minecraft:red_wool',
    'p_IF' -> [0,10,0],

    'p_main' -> [-18, 0, 9],
    'p_load' -> [-14, 0, 3],
    'p_clear' -> [-12, 0, 3],

    'CPU' -> '@e[type=minecraft:armor_stand,name=CPU]',
    'CPU_PTR' -> '@e[type=minecraft:armor_stand,name=CPU_PTR]',
    'p_CPU' -> [0, 0, 0],

    //
    // 1 --- 2
    // |
    // 3
    //  \
    //   4
    //
    'ALU' -> '@e[type=minecraft:armor_stand,name=ALU]',
    'p1_ALU' -> [-2, 1, 19],
    'p_ALU_end' -> [-12, 0, 9],

    'p_ALU_add' -> [-10, 0, 9],
    'p_ALU_add_loop' -> [-11, 0, 9],

    'p_ALU_sub' -> [-14, 0, 9],
    'p_ALU_sub_loop' -> [-15, 0, 9],
    'p_ALU_sub_CMP_cb' -> [-16, 0, 9],

    'STACK' -> '@e[type=minecraft:armor_stand,name=STACK]',
    'STACK_PTR' -> '@e[type=minecraft:armor_stand,name=STACK_PTR]',
    'p_STACK' -> [0, 0, -18],

    'CMP' -> '@e[type=minecraft:armor_stand,name=CMP]',
    'CMP_PTR' -> '@e[type=minecraft:armor_stand,name=CMP_PTR]',
    'p1_CMP' -> [-2, 1, 28],
    'p_CMP_gr' -> [-10, 0, 30],
    'p_CMP_gr_loop' -> [-11, 0, 30],
    'p_CMP_gr_end' -> [-12, 0, 30],

    'CALL_PTR' -> '@e[type=minecraft:armor_stand,name=CALL_PTR]',
    'p_CALL' -> [0,0,-36],

    'p_i_push' -> [0, 0, 34],
    'p_i_push_idx' -> [0, 1, 34],
    'p_i_push_ALU_cb' -> [0, 2, 34],
    'p_i_push_end' -> [0, 3, 34],

    'p_i_pop' -> [1, 0, 34],

    'p_i_add' -> [3, 0, 34],
    'p_i_add_ALU_cb' -> [3, 1, 34],
    'p_i_add_PUSH_cb' -> [3, 2, 34],

    'p_i_sub' -> [4, 0, 34],
    'p_i_sub_ALU_cb' -> [4, 1, 34],
    'p_i_sub_PUSH_cb' -> [4, 2, 34],

    'p_i_jmp' -> [6, 0, 34],
    'p_i_je' -> [7, 0, 34],
    'p_i_jne' -> [8, 0, 34],
    'p_i_jg' -> [9, 0, 34],
    'p_i_jg_CMP_cb' -> [9, 1, 34],
    'p_i_jge' -> [10, 0, 34],
    'p_i_jl' -> [11, 0, 34],
    'p_i_jl_CMP_cb' -> [11, 1, 34],
    'p_i_jle' -> [12, 0, 34],
    'p_i_call' -> [13, 0, 34],
    'p_i_ret' -> [14, 0, 34],
    'p_i_jz' -> [15, 0, 34],
    'p_i_jnz' -> [16, 0, 34],

    'p_i_get' -> [18, 0, 34],
    'p_i_set' -> [19, 0, 34]
};

// CPU
c:'p1_CPU_A' = c:'p_CPU' + [-1,0,1];
c:'p2_CPU_A' = c:'p_CPU' + [-1,0,8];
c:'p1_CPU_B' = c:'p1_CPU_A' + [-1,0,0];
c:'p2_CPU_B' = c:'p2_CPU_A' + [-1,0,0];
c:'p1_CPU_C' = c:'p1_CPU_B' + [-1,0,0];
c:'p2_CPU_C' = c:'p2_CPU_B' + [-1,0,0];
c:'p_CPU_D' = c:'p2_CPU_C' + [-1,0,0];

// STACK
c:'p1_STACK_num' = c:'p_STACK' + [-1,0,1];
c:'p2_STACK_num' = c:'p1_STACK_num' + [0,0,7];

// ALU
c:'p2_ALU' = c:'p1_ALU' + [0,0,7];
c:'p3_ALU' = c:'p1_ALU' + [-3,0,0];
c:'p4_ALU' = c:'p3_ALU' + [0,-1,0];

c:'p1_ALU_A' = c:'p1_ALU' + [-1,0,0];
c:'p2_ALU_A' = c:'p2_ALU' + [-1,0,0];
c:'p1_ALU_B' = c:'p1_ALU_A' + [-1,0,0];
c:'p2_ALU_B' = c:'p2_ALU_A' + [-1,0,0];
c:'p1_ALU_R' = c:'p1_ALU_B' + [-1,0,0];
c:'p_ALU_R_MSB' = c:'p1_ALU_R' + [0,0,1];
c:'p2_ALU_R' = c:'p2_ALU_B' + [-1,0,0];

c:'p_ALU_pos' = c:'p2_ALU_R' + [-2,0,0];
c:'p_ALU_num' = c:'p_ALU_pos' + [-1,0,0];

// CMP
c:'p2_CMP' = c:'p1_CMP' + [0,0,7];

c:'p1_CMP_A' = c:'p1_CMP';
c:'p2_CMP_A' = c:'p2_CMP';
c:'p1_CMP_B' = c:'p1_CMP_A' + [-1,0,0];
c:'p2_CMP_B' = c:'p2_CMP_A' + [-1,0,0];
c:'p_CMP_R' = c:'p2_CMP_B' + [-1,0,0];
c:'p1_CMP_XOR' = c:'p1_CMP_B' + [-2,0,0];
c:'p2_CMP_XOR' = c:'p2_CMP_B' + [-2,0,0];
c:'p1_CMP_XOR_CHK' = c:'p1_CMP_XOR' + [0,-1,0];
c:'p2_CMP_XOR_CHK' = c:'p2_CMP_XOR' + [0,-1,0];

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_if(exec_args) -> (
    assert_str(exec_args);
    return ('execute ' + exec_args);
);
_do(cmds) -> (
    assert_list(cmds);
    dir = [0,0,1];
    ofs = [-dir:0, -dir:1, -dir:2];
    res = [];
    for (merge_lists(cmds),
        res += str('execute if data block ~%d ~%d ~%d {SuccessCount:1} run %s', ofs:0, ofs:1, ofs:2, _);
        ofs = ofs - dir;
    );
    return (res);
);
_ALUpos(block) -> 'execute if block $p_ALU_pos$ $AIR$ run setblock $p_ALU_pos$ ' + block;


cmds_ALU_next(p_loop, p_end) -> {
    assert_str(p_loop);
    assert_str(p_end);
    return ([
        // advance ALU pointer
        'execute as $ALU$ at @s run tp @s ~ ~ ~-1',
        //
        'execute at $ALU$ if blocks $p3_ALU$ ~ ~ ~ $p4_ALU$ all run setblock ~-1 ~ ~ $AIR$',
        // LOOP
        str('execute at $ALU$ unless block ~-1 ~ ~ $AIR$ run setblock $%s$ minecraft:redstone_block', p_loop),
        // reset ALU pointer if reached end
        'execute as $ALU$ at @s if block ~-1 ~ ~ $AIR$ run tp @s $p2_ALU$',
            str('> setblock $%s$ minecraft:redstone_block', p_end)
    ]);
};

CMD_CPU_TOP_OR_ARG = 'execute if block $p1_CPU_A$ $AIR$ at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_CPU_A$';

CMDS_CPU_TOP2_OR_ARG = [
    // B
    'execute if block $p1_CPU_B$ $AIR$ at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_CPU_B$',
        '> execute if block $p1_CPU_A$ $AIR$ at $STACK_PTR$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_CPU_B$',
    // A 
    'execute if block $p1_CPU_A$ $AIR$ at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_CPU_A$'
];

CMD_ALU_CLEAR_CARRY = 'fill $p1_ALU$ $p2_ALU$ $OFF$';

cmd_summon_armorstand     (pos, name) -> str('summon minecraft:armor_stand %s {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"%s"}\'}', pos, name);
cmd_summon_armorstand_ptr (pos, name) -> str('summon minecraft:armor_stand %s {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"%s"}\',ArmorItems:[{},{},{},{id:diamond_helmet,Count:1}]}', pos, name);

CMD_MAIN_NEXT = 'setblock $p_main$ minecraft:redstone_block';

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

cmdblocks = {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Load
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'load' -> {
        'position' -> c:'p_load',
        'direction' -> [0,0,-1],
        'redstone_block' -> true,
        'commands' -> [
            cmd_summon_armorstand_ptr('$p_CPU$', 'CPU_PTR'),
            cmd_summon_armorstand('$p2_ALU$', 'ALU'),
            cmd_summon_armorstand_ptr('$p2_CMP$', 'CMP_PTR'),
            cmd_summon_armorstand_ptr('$p_STACK$', 'STACK_PTR'),
            cmd_summon_armorstand_ptr('$p_CALL$', 'CALL_PTR'),
            // stack 0 index
            'execute at $STACK_PTR$ run fill ~ ~ ~-8 ~ ~ ~-1 $OFF$',
            //
            // ALU
            //
            'execute at $ALU$ run fill ~-3 ~-1 ~-7 ~ ~ ~ $OFF$',
            'setblock $p_ALU_pos$ $AIR$',
            //
            // CMP
            //
            'fill $p1_CMP_XOR_CHK$ $p2_CMP_XOR_CHK$ $ON$'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Clear
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'clear' -> {
        'position' -> c:'p_clear',
        'direction' -> [0,0,-1],
        'redstone_block' -> true,
        'commands' -> [
            'kill @e[type=minecraft:armor_stand]',
            'fill -50 5 -50 50 5 50 $AIR$',
            'fill -50 4 -50 50 4 50 $AIR$',
            'fill -50 3 -50 50 3 50 $AIR$',
            'fill -50 2 -50 50 2 50 $AIR$',
            'fill -50 1 -50 50 1 50 $AIR$',
            'fill -50 0 -50 50 0 50 $AIR$',
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Main
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'main' -> {
        'position' -> c:'p_main',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'fill $p1_CPU_A$ $p2_CPU_A$ $AIR$',
            'fill $p1_CPU_B$ $p2_CPU_B$ $AIR$',
            'fill $p1_CPU_C$ $p2_CPU_C$ $AIR$',
            'setblock $p_CPU_D$ $AIR$',
            // copy instruction number
            'execute at $CPU_PTR$ run clone ~ ~ ~1 ~ ~ ~8 $p1_CPU_A$',
            'execute at $CPU_PTR$ run clone ~ ~ ~10 ~ ~ ~17 $p1_CPU_B$',
            'execute at $CPU_PTR$ run clone ~ ~ ~19 ~ ~ ~26 $p1_CPU_C$',
            'execute at $CPU_PTR$ run clone ~ ~ ~28 ~ ~ ~28 $p_CPU_D$',
            // place redstone block
            'execute at $CPU_PTR$ run setblock ~ ~ ~30 minecraft:redstone_block',
            // advance instruction pointer
            'execute as $CPU_PTR$ at @s if block ~ ~ ~ minecraft:orange_wool run tp @s ~1 ~ ~'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ALU
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'ALU_end' -> {
        'position' -> c:'p_ALU_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // neg
            'clone $p_ALU_pos$ $p_ALU_pos$ $p1_ALU_R$',
            'setblock $p_ALU_pos$ $AIR$',
            // zero
            'execute if blocks $p_ALU_R_MSB$ $p2_ALU_R$ $p4_ALU$ all run setblock $p1_ALU_R$ $OFF$',
            // push callback
            'execute if block $p_i_push$ minecraft:redstone_block run setblock $p_i_push_ALU_cb$ minecraft:redstone_block',
            // add callback
            'execute if block $p_i_add$ minecraft:redstone_block run setblock $p_i_add_ALU_cb$ minecraft:redstone_block',
            // sub callback
            'execute if block $p_i_sub$ minecraft:redstone_block run setblock $p_i_sub_ALU_cb$ minecraft:redstone_block'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ALU --- addition
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'ALU_add' -> {
        'position' -> c:'p_ALU_add',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            CMD_ALU_CLEAR_CARRY,
            // -A + -B = -(A + B)
            _if('if block $p1_ALU_A$ $OFF$ if block $p1_ALU_B$ $OFF$'),
            _do([
                _ALUpos('$OFF$'),
                'setblock $p_ALU_add_loop$ minecraft:redstone_block'
            ]),
            // A + B
            _if('if block $p1_ALU_A$ $ON$ if block $p1_ALU_B$ $ON$'),
            _do([
                _ALUpos('$ON$'),
                'setblock $p1_ALU_A$ $OFF$',
                'setblock $p1_ALU_B$ $OFF$',
                'setblock $p_ALU_add_loop$ minecraft:redstone_block'
            ]),
            // - +
            // + -
            _if('unless blocks $p1_ALU_A$ $p1_ALU_A$ $p1_ALU_B$ all'),
            _do([
                'clone $p1_ALU_B$ $p1_ALU_B$ $p_ALU_num$',
                'execute if block $p_ALU_num$ $OFF$ run clone $p1_ALU_B$ $p2_ALU_B$ $p1_ALU_R$',
                'execute if block $p_ALU_num$ $OFF$ run clone $p1_ALU_A$ $p2_ALU_A$ $p1_ALU_B$',
                'execute if block $p_ALU_num$ $OFF$ run clone $p1_ALU_R$ $p2_ALU_R$ $p1_ALU_A$',
                'setblock $p1_ALU_A$ $ON$',
                'setblock $p1_ALU_B$ $ON$',
                'setblock $p_ALU_sub$ minecraft:redstone_block' 
            ])
        ]
    },
    'ALU_add_loop' -> {
        'position' -> c:'p_ALU_add_loop',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> extend_list([
            // https://www.researchgate.net/figure/Digital-circuit-of-a-binary-adder_fig4_228716075
            // and1 - carry out
            'execute at $ALU$ if block ~-1 ~ ~ $ON$ if block ~-2 ~ ~ $ON$ run setblock ~ ~ ~-1 $ON$',
            // xor1
            'execute at $ALU$ run setblock ~-4 ~ ~ $OFF$',
            'execute at $ALU$ unless blocks ~-1 ~ ~ ~-1 ~ ~ ~-2 ~ ~ all run setblock ~-4 ~ ~ $ON$',
                // and2 - carry out
                '> execute at $ALU$ if block ~ ~ ~ $ON$ run setblock ~ ~ ~-1 minecraft:lime_wool',
            // xor2 - result
            'execute at $ALU$ run setblock ~-3 ~ ~ $OFF$',
            'execute at $ALU$ unless blocks ~ ~ ~ ~ ~ ~ ~-4 ~ ~ all run setblock ~-3 ~ ~ $ON$'
        ], cmds_ALU_next('p_ALU_add_loop', 'p_ALU_end'))
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ALU --- subtraction
    //
    ////////////////////////////////////////////////////e////////////////////////////////////////////////////////////
    'ALU_sub' -> {
        'position' -> c:'p_ALU_sub',
        'direction' -> [0,0,1],
        'commands' -> [
            CMD_ALU_CLEAR_CARRY,
            // B > A
            'clone $p1_ALU_A$ $p2_ALU_A$ $p1_CMP_A$',
            'clone $p1_ALU_B$ $p2_ALU_B$ $p1_CMP_B$',
            'setblock $p1_CMP_A$ $ON$',
            'setblock $p1_CMP_B$ $ON$',
            'setblock $p_CMP_gr$ minecraft:redstone_block'
        ]
    },
    'ALU_sub_CMP_cb' -> {
        'position' -> c:'p_ALU_sub_CMP_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_ALU_sub$ $AIR$',
            //
            _if('if block $p_CMP_R$ $ON$'),
            _do([
                'clone $p1_ALU_B$ $p2_ALU_B$ $p1_ALU_R$',
                'clone $p1_ALU_A$ $p2_ALU_A$ $p1_ALU_B$',
                'clone $p1_ALU_R$ $p2_ALU_R$ $p1_ALU_A$',
                _ALUpos('$ON$')
            ]),
            _ALUpos('$OFF$'),
            // - -
            _if('if block $p1_ALU_A$ $OFF$ if block $p1_ALU_B$ $OFF$'),
            _do([
                // swap pos
                'execute if block $p_ALU_pos$ $ON$  run setblock $p_ALU_num$ $OFF$',
                'execute if block $p_ALU_pos$ $OFF$ run setblock $p_ALU_num$ $ON$',
                'clone $p_ALU_num$ $p_ALU_num$ $p_ALU_pos$',
                //
                'setblock $p_ALU_sub_loop$ minecraft:redstone_block'
            ]),
            // + +
            _if('if block $p1_ALU_A$ $ON$ if block $p1_ALU_B$ $ON$'),
            _do([
                'setblock $p1_ALU_A$ $OFF$',
                'setblock $p1_ALU_B$ $OFF$',
                'setblock $p_ALU_sub_loop$ minecraft:redstone_block'
            ]),
            // - +
            // + -
            _if('unless blocks $p1_ALU_A$ $p1_ALU_A$ $p1_ALU_B$ all'),
            _do([
                'setblock $p1_ALU_A$ $ON$',
                'setblock $p1_ALU_B$ $ON$',
                'setblock $p_ALU_add$ minecraft:redstone_block'
            ])
        ]
    },
    'ALU_sub_loop' -> {
        'position' -> c:'p_ALU_sub_loop',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> extend_list([
            // https://circuitdigest.com/tutorial/full-subtractor-circuit-and-its-construction
            // xor1
            'execute at $ALU$ run setblock ~-4 ~ ~ $OFF$',
            'execute at $ALU$ unless blocks ~-1 ~ ~ ~-1 ~ ~ ~-2 ~ ~ all run setblock ~-4 ~ ~ $ON$',
            // xor2 - result
            'execute at $ALU$ run setblock ~-3 ~ ~ $OFF$',
            'execute at $ALU$ unless blocks ~ ~ ~ ~ ~ ~ ~-4 ~ ~ all run setblock ~-3 ~ ~ $ON$',
            // borrow out = (!A & B)
            'execute at $ALU$ if block ~-1 ~ ~ $OFF$ if block ~-2 ~ ~ $ON$ run setblock ~ ~ ~-1 $ON$',
            // borrow out = (!xor1 && borrow in)
            'execute at $ALU$ if block ~-4 ~ ~ $OFF$ if block ~ ~ ~ $ON$ run setblock ~ ~ ~-1 $ON$'
        ], cmds_ALU_next('p_ALU_sub_loop', 'p_ALU_end'))
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CMP
    //
    ////////////////////////////////////////////////////e////////////////////////////////////////////////////////////
    //
    // Greater
    //
    'CMP_gr' -> {
        'position' -> c:'p_CMP_gr',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_CMP_R$ $OFF$',
            'execute at $CMP$ run setblock ~-3 ~ ~ $OFF$',
            'execute at $CMP$ if block ~-1 ~ ~ $OFF$ unless block ~ ~ ~ $ON$  run setblock ~-3 ~ ~ $ON$',
            'execute at $CMP$ if block ~-1 ~ ~ $ON$  unless block ~ ~ ~ $OFF$ run setblock ~-3 ~ ~ $ON$',
            'setblock $p_CMP_gr_loop$ minecraft:redstone_block'
        ]
    },
    'CMP_gr_loop' -> {
        'position' -> c:'p_CMP_gr_loop',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // https://slidetodoc.com/ee-121-john-wakerly-lecture-6-adders-multipliers/
            'execute at $CMP_PTR$ if block ~ ~ ~ $OFF$ if block ~-1 ~ ~ $ON$ if block ~-3 ~ ~-1 $AIR$ run setblock $p_CMP_R$ $ON$',
            'execute at $CMP_PTR$ if block ~ ~ ~ $OFF$ if block ~-1 ~ ~ $ON$ if blocks $p1_CMP_XOR$ ~-3 ~ ~-1 $p1_CMP_XOR_CHK$ all run setblock $p_CMP_R$ $ON$',
                '> execute at $CMP_PTR$ run setblock ~-3 ~ ~-1 $AIR$',
            'execute as $CMP_PTR$ at @s run tp @s ~ ~ ~-1',
            'execute at $CMP_PTR$ if block ~-3 ~ ~ $AIR$ run setblock $p_CMP_gr_end$ minecraft:redstone_block',
            'execute at $CMP_PTR$ unless block ~-3 ~ ~ $AIR$ run setblock $p_CMP_gr_loop$ minecraft:redstone_block'
        ]
    },
    'CMP_gr_end' -> {
        'position' -> c:'p_CMP_gr_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute as $CMP_PTR$ at @s run tp @s $p2_CMP$',
            'execute if block $p1_CMP_A$ $OFF$ if block $p1_CMP_B$ $OFF$ if block $p_CMP_R$ $ON$  run setblock $p_CMP_R$ $OFF$',
            'execute if block $p1_CMP_A$ $OFF$ if block $p1_CMP_B$ $OFF$ if block $p_CMP_R$ $OFF$ run setblock $p_CMP_R$ $ON$',
            // jg callback
            'execute if block $p_i_jg$ minecraft:redstone_block run setblock $p_i_jg_CMP_cb$ minecraft:redstone_block',
            // jl callback
            'execute if block $p_i_jl$ minecraft:redstone_block run setblock $p_i_jl_CMP_cb$ minecraft:redstone_block',
            // sub callback
            'execute if block $p_ALU_sub$ minecraft:redstone_block run setblock $p_ALU_sub_CMP_cb$ minecraft:redstone_block'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- push
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_push' -> {
        'position' -> c:'p_i_push',
        'direction' -> [0,0,1],
        'commands' -> [
            'execute at $STACK_PTR$ run ' + cmd_summon_armorstand('~ ~ ~', 'STACK'),
            // copy number from instruction to current stack pointer
            'execute at $STACK_PTR$ run clone $p1_CPU_A$ $p2_CPU_A$ ~ ~ ~1',
            // if index doesn't exist, calc new one
            'execute at $STACK_PTR$ if block ~1 ~ ~-1 $AIR$',
                '> setblock $p_i_push_idx$ minecraft:redstone_block',
            // advance stack pointer if index exists
            'execute as $STACK_PTR$ at @s unless block ~1 ~ ~-1 $AIR$ run tp @s ~1 ~ ~',
                '> setblock $p_i_push_end$ minecraft:redstone_block',
        ]
    },
    'i_push_idx' -> {
        'position' -> c:'p_i_push_idx',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // copy stack index to ALU as A
            'execute at $STACK_PTR$ run clone ~ ~ ~-8 ~ ~ ~-1 $p1_ALU_A$',
            // set ALU B to 1
            'fill $p1_ALU_B$ $p2_ALU_B$ $OFF$',
            'setblock $p2_ALU_B$ $ON$',
            // add A + B
            'setblock $p_ALU_add$ minecraft:redstone_block'
        ]
    },
    'i_push_ALU_cb' -> {
        'position' -> c:'p_i_push_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // advance stack pointer
            'execute as $STACK_PTR$ at @s run tp @s ~1 ~ ~',
            // copy new stack index from ALU
            'execute at $STACK_PTR$ run clone $p1_ALU_R$ $p2_ALU_R$ ~ ~ ~-8',
            // clear the i_push redstone block
            'setblock $p_i_push_end$ minecraft:redstone_block'
        ]
    },
    'i_push_end' -> {
        'position' -> c:'p_i_push_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_i_push$ $AIR$',
            // add callback
            'execute if block $p_i_add$ minecraft:redstone_block run setblock $p_i_add_PUSH_cb$ minecraft:redstone_block',
            // sub callback
            'execute if block $p_i_sub$ minecraft:redstone_block run setblock $p_i_sub_PUSH_cb$ minecraft:redstone_block',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- pop
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_pop' -> {
        'position' -> c:'p_i_pop',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // decrease stack pointer
            'execute as $STACK_PTR$ at @s run tp @s ~-1 ~ ~',
            //
            'execute at $STACK_PTR$ run kill @e[type=minecraft:armor_stand,name=STACK,dx=0,dy=0,dz=0]',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- add
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_add' -> {
        'position' -> c:'p_i_add',
        'direction' -> [0,0,1],
        'commands' -> [
            'execute at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_ALU_A$',
            'execute at $STACK_PTR$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_ALU_B$',
            'setblock $p_ALU_add$ minecraft:redstone_block'
        ]
    },
    'i_add_ALU_cb' -> {
        'position' -> c:'p_i_add_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'clone $p1_ALU_R$ $p2_ALU_R$ $p1_CPU_A$',
            'setblock $p_i_push$ minecraft:redstone_block'
        ]
    },
    'i_add_PUSH_cb' -> {
        'position' -> c:'p_i_add_PUSH_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_i_add$ $AIR$'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- sub
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_sub' -> {
        'position' -> c:'p_i_sub',
        'direction' -> [0,0,1],
        'commands' -> [
            'execute at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_ALU_A$',
            'execute at $STACK_PTR$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_ALU_B$',
            'setblock $p_ALU_sub$ minecraft:redstone_block'
        ]
    },
    'i_sub_ALU_cb' -> {
        'position' -> c:'p_i_sub_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'clone $p1_ALU_R$ $p2_ALU_R$ $p1_CPU_A$',
            'setblock $p_i_push$ minecraft:redstone_block'
        ]
    },
    'i_sub_PUSH_cb' -> {
        'position' -> c:'p_i_sub_PUSH_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_i_sub$ $AIR$'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jmp
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jmp' -> {
        'position' -> c:'p_i_jmp',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- je
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_je' -> {
        'position' -> c:'p_i_je',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $STACK_PTR$ if blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jne
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jne' -> {
        'position' -> c:'p_i_jne',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $STACK_PTR$ unless blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jg
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jg' -> {
        'position' -> c:'p_i_jg',
        'direction' -> [0,0,1],
        'commands' -> [
            'execute at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_CMP_A$',
            'execute at $STACK_PTR$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_CMP_B$',
            'setblock $p_CMP_gr$ minecraft:redstone_block'
        ]
    },
    'i_jg_CMP_cb' -> {
        'position' -> c:'p_i_jg_CMP_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute if block $p_CMP_R$ $ON$ run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            'setblock $p_i_jg$ $AIR$',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jge
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jge' -> {
        'position' -> c:'p_i_jge',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $STACK_PTR$ if blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
                '> ' + CMD_MAIN_NEXT,
            'execute at $STACK_PTR$ unless blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run setblock $p_i_jg$ minecraft:redstone_block'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jl
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jl' -> {
        'position' -> c:'p_i_jl',
        'direction' -> [0,0,1],
        'commands' -> [
            'execute at $STACK_PTR$ if blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all',
                '> setblock $p_i_jl$ $AIR$',
                '> ' + CMD_MAIN_NEXT,
            'execute at $STACK_PTR$ unless blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all',
                '> execute at $STACK_PTR$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_CMP_A$',
                '> execute at $STACK_PTR$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_CMP_B$',
                '> setblock $p_CMP_gr$ minecraft:redstone_block'
        ]
    },
    'i_jl_CMP_cb' -> {
        'position' -> c:'p_i_jl_CMP_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute if block $p_CMP_R$ $OFF$ run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            'setblock $p_i_jl$ $AIR$',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jle
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jle' -> {
        'position' -> c:'p_i_jle',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $STACK_PTR$ if blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
                '> ' + CMD_MAIN_NEXT,
            'execute at $STACK_PTR$ unless blocks ~-1 ~ ~1 ~-1 ~ ~8 ~-2 ~ ~1 all run setblock $p_i_jl$ minecraft:redstone_block'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jz
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jz' -> {
        'position' -> c:'p_i_jz',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            _if('at $STACK_PTR$ if blocks ~-1 ~ ~1 ~-1 ~ ~8 $p4_ALU$ all'),
            _do(['execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~']),
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- jnz
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_jnz' -> {
        'position' -> c:'p_i_jnz',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            _if('at $STACK_PTR$ unless blocks ~-1 ~ ~1 ~-1 ~ ~8 $p4_ALU$ all'),
            _do(['execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~']),
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- get
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_get' -> {
        'position' -> c:'p_i_get',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            CMD_CPU_TOP_OR_ARG,
            'execute at $STACK$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run clone ~ ~ ~1 ~ ~ ~8 $p1_CPU_A$',
                '> setblock $p_i_push$ minecraft:redstone_block'
            // CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- set
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_set' -> {
        'position' -> c:'p_i_set',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> extend_list(CMDS_CPU_TOP2_OR_ARG, [
            'execute at $STACK$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run clone $p1_CPU_B$ $p2_CPU_B$ ~ ~ ~1',
            CMD_MAIN_NEXT
        ])
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- call
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_call' -> {
        'position' -> c:'p_i_call',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute at $CPU_PTR$ run clone ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_B$',
            'execute at $CALL_PTR$ run clone $p1_CPU_B$ $p2_CPU_B$ ~ ~ ~1',
            'execute as $CALL_PTR$ at @s run tp @s ~1 ~ ~',
            // jmp
            'execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
            CMD_MAIN_NEXT
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- ret
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_ret' -> {
        'position' -> c:'p_i_ret',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute as $CALL_PTR$ at @s run tp @s ~-1 ~ ~',
            'execute at $CALL_PTR$ run clone ~ ~ ~1 ~ ~ ~8 $p1_CPU_A$',
            // jmp
            'execute at $CPU$ if blocks ~ ~ ~-8 ~ ~ ~-1 $p1_CPU_A$ all run tp $CPU_PTR$ ~ ~ ~',
                '> ' + CMD_MAIN_NEXT
        ]
    }
};

c_for(i = 0, i < 8, i += 1,
    cmdblocks:'load':'commands' += 'execute at $CMP_PTR$ run ' + cmd_summon_armorstand('~ ~ ~' + (-i), 'CMP');
);

place_cmdblock(c, data) -> (
    assert_map(c);
    assert_map(data);

    loc = data:'position';
    dir = data:'direction';
    cmds = data:'commands';

    assert_vector3(loc);
    assert_vector3(dir);
    assert_list(cmds);

    if (data:'redstone_block',
        put(cmds, 0, 'setblock ' + mcpos(loc) + ' minecraft:air', 'insert');
    ); 
    loc = loc + dir;

    for(merge_lists(cmds),
        cmd = _;
        assert_str(cmd);

        cond = 'false';
        cmdblock = 'chain_command_block';
        always_active = '1b';

        if (_i == 0,
            cmdblock = 'command_block';
            always_active = '0b';
        );

        if (replace(cmd, '^(?:\\s*)>(?:\\s*)') != cmd, cond = 'true');
        cmd = replace(cmd, '(^\\s*>?\\s*)|(\\s+$)');
        cmd = replace(cmd, '\\s+', ' ');
        cmd = replace(cmd, '"', '\\\\"');

        for (pairs(c), 
            name = _:0;
            val = _:1;
            assert_str(name);
            if (type(val) == 'list',
                assert_vector3(val);
                val = mcpos(val);
            );
            cmd = replace(cmd, '\\$' + name + '\\$', str(val));
        );

        set(loc, str('%s[facing=%s,conditional=%s]{"auto":%s,"Command":"%s"}', cmdblock, dir_from_list(dir), cond, always_active, cmd));
        loc = loc + dir;
    );
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

place_number(pos, num, bits, line) -> (
    assert_vector3(pos);
    assert_num(num);
    assert_num(bits);
    assert_bool(line);

    num_power(num, pwr) -> (
        assert_num(num);
        assert_num(pwr);
        res = 1;
        while(1, pwr, res = res * num);
        return (res);
    );

    bit_is_set(num, idx) -> (
        assert_num(num);
        assert_num(idx);
        return (floor(num / num_power(2, idx)) % 2 == 1);
    );

    set(pos, 'lime_wool');
    if (num <= 0,
        num = -num;
        set(pos, 'red_wool');
    );
    pos:2 += 1;

    if (line, (
        set(pos, 'orange_wool');
        pos:2 += 1;
    ));
    c_for (bit_idx = bits - 1 - 1, bit_idx >= 0, bit_idx = bit_idx - 1, 
        blockid = (bit_is_set(num, bit_idx) && 'lime_wool') || 'red_wool';
        set(pos, blockid);
        pos:2 += 1;
    );
);

place_sign(pos, ln1, ln2, ln3, ln4) -> (
    assert_vector3(pos);
    assert_str(ln1);
    assert_str(ln2);
    assert_str(ln3);
    assert_str(ln4);
    if (ln1 == 'null', ln1 = '');
    if (ln2 == 'null', ln2 = '');
    if (ln3 == 'null', ln3 = '');
    if (ln4 == 'null', ln4 = '');
    set(pos, str('oak_sign{Text1:"{\\"text\\":\\"%s\\"}",Text2:"{\\"text\\":\\"%s\\"}",Text3:"{\\"text\\":\\"%s\\"}",Text4:"{\\"text\\":\\"%s\\"}"}', ln1, ln2, ln3, ln4));
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

c_for (i = 0, i < length(global_statements), i += 1,
    stmt = global_statements:i;

    tp = stmt:0;
    if (tp == 'lbl',
        id = stmt:1;
        if (has(global_labels, id),
            throw('Label "' + id + '" has already been defined'));
        global_labels:id = i - length(keys(global_labels));
    );
);

stmt_num = 0;
for (global_statements, (
    stmt = _;
    type = '???';

    if (type(stmt) == 'list', type = stmt:0,
        type(stmt) == 'string', type = stmt,
        true, throw('Statement must be either a list or string : "' + str(stmt) + '"'));

    if (type == 'lbl', continue());

    if ((type == 'jmp') ||
        (type == 'je') ||
        (type == 'jne') ||
        (type == 'jg') ||
        (type == 'jge') ||
        (type == 'jl') ||
        (type == 'jle') ||
        (type == 'call') ||
        (type == 'jnz') ||
        (type == 'jz'), (
        id = stmt:1;
        if (!has(global_labels, id),
            error('Label "' + id + '" doesn\'t exist'));
        place_number([stmt_num, 0, 1], global_labels:id, 8, false);
    ));

    place_number([stmt_num,0,-8],stmt_num,8,false);
    set([stmt_num, 0, 0], 'orange_wool');
    set([stmt_num, 2, 0], 'air');
    
    if (type(stmt) == 'string', (
        place_sign([stmt_num,2,0], type, '', '', '');
    ), true, 
        arg1 = safeget(stmt, 1);
        arg2 = safeget(stmt, 2);
        arg3 = safeget(stmt, 3);
        arg4 = safeget(stmt, 4);

        place_sign([stmt_num,2,0], type + ' ' + str(arg1), str(arg2), str(arg3), str(arg4));

        if (type(arg1) == 'number', place_number([stmt_num,0,1],  arg1, 8, false));
        if (type(arg2) == 'number', place_number([stmt_num,0,10], arg2, 8, false));
        if (type(arg3) == 'number', place_number([stmt_num,0,19], arg3, 8, false));
    );
    cmdblocks:'load':'commands' += cmd_summon_armorstand(mcpos([stmt_num,0,0]), 'CPU');
    place_cmdblock(c, {
        'position' -> [stmt_num, 0, 30],
        'direction' -> [0, 0, 1],
        'redstone_block' -> true,
        'commands' -> ['setblock $' + 'p_i_' + type + '$ minecraft:redstone_block']
    });

    stmt_num += 1;
));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

for (values(cmdblocks), place_cmdblock(c,_));
