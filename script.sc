global_statements = read_file('data', 'json');
global_labels = {};
global_constants = {};
global_stack = []; // []
global_call_stack = [];

warn(s) -> print('[!] ' + s);
error(s) -> print('[ERROR] ' + s);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

dir_from_list(dir) -> (
    if (dir:0 < 0, return('west'));
    if (dir:0 > 0, return('east'));
    if (dir:1 > 0, return('up'));
    if (dir:1 < 0, return('down'));
    if (dir:2 > 0, return('south'));
    if (dir:2 < 0, return('north'));
    return ('INVALID');
);

mcpos(pos) -> (
    if (type(pos) != 'list',
        error('Argument to mcpos() isn\'t a list! pos = ' + pos));
    return (join(' ', pos));
);

c = {
    'AIR' -> 'minecraft:air',
    'ON' -> 'minecraft:lime_wool',
    'OFF' -> 'minecraft:red_wool',

    'p_main' -> [-13, 0, 4],
    'p_load' -> [-14, 0, -11],
    'p_clear' -> [-12, 0, -11],

    'CPU' -> '@e[type=minecraft:armor_stand,name=CPU]',
    'p_CPU' -> [0, 0, 0],

    //
    // 1 --- 2
    // |
    // 3
    //  \
    //   4
    //
    'ALU' -> '@e[type=minecraft:armor_stand,name=ALU]',
    'p1_ALU' -> [-2, 1, 11],
    'p_ALU_end' -> [-12, 0, 9],

    'p_ALU_add' -> [-10, 0, 9],
    'p_ALU_add_loop' -> [-11, 0, 9],

    'p_ALU_sub' -> [-14, 0, 9],
    'p_ALU_sub_loop' -> [-15, 0, 9],

    'STACK' -> '@e[type=minecraft:armor_stand,name=STACK]',
    'p_STACK' -> [0, 0, -10],

    'CMP' -> '@e[type=minecraft:armor_stand,name=CMP]',
    'CMP_PTR' -> '@e[type=minecraft:armor_stand,name=CMP_PTR]',
    'p1_CMP' -> [-2, 1, 20],
    'p_CMP_eq' -> [-2, 0, 29],
    'p_CMP_gr' -> [-4, 0, 29],
    'p_CMP_gr_loop' -> [-5, 0, 29],
    'p_CMP_gr_end' -> [-6, 0, 29],

    'p_i_push' -> [0, 0, 13],
    'p_i_push_idx' -> [0, 2, 13],
    'p_i_push_ALU_cb' -> [0, 4, 13],
    'p_i_push_end' -> [0, 6, 13],

    'p_i_pop' -> [2, 0, 13],

    'p_i_add' -> [4, 0, 13],
    'p_i_add_ALU_cb' -> [4, 2, 13],
    'p_i_add_PUSH_cb' -> [4, 4, 13],

    'p_i_sub' -> [6, 0, 13],
    'p_i_sub_ALU_cb' -> [6, 2, 13],
    'p_i_sub_PUSH_cb' -> [6, 4, 13]
};

// CPU
c:'p1_CPU_num' = c:'p_CPU' + [-1,0,1];
c:'p2_CPU_num' = c:'p_CPU' + [-1,0,8];

// ALU
c:'p2_ALU' = c:'p1_ALU' + [0,0,7];
c:'p3_ALU' = c:'p1_ALU' + [-3,0,0];
c:'p4_ALU' = c:'p3_ALU' + [0,-1,0];

c:'p1_ALU_A' = c:'p1_ALU' + [-1,0,0];
c:'p2_ALU_A' = c:'p2_ALU' + [-1,0,0];
c:'p1_ALU_B' = c:'p1_ALU_A' + [-1,0,0];
c:'p2_ALU_B' = c:'p2_ALU_A' + [-1,0,0];
c:'p1_ALU_R' = c:'p1_ALU_B' + [-1,0,0];
c:'p2_ALU_R' = c:'p2_ALU_B' + [-1,0,0];

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

cmds_ALU_next(p_loop, p_end) -> [
    // advance ALU pointer
    'execute as $ALU$ at @s run tp @s ~ ~ ~-1',
    //
    'execute at $ALU$ if blocks $p3_ALU$ ~ ~ ~ $p4_ALU$ all run setblock ~-1 ~ ~ $AIR$',
    // LOOP
    str('execute at $ALU$ unless block ~-1 ~ ~ $AIR$ run setblock $%s$ minecraft:redstone_block', p_loop),
    // reset ALU pointer if reached end
    'execute as $ALU$ at @s if block ~-1 ~ ~ $AIR$ run tp @s $p2_ALU$',
        str('> setblock $%s$ minecraft:redstone_block', p_end)
];

extend_list(l1, l2) -> (
    put(l1, null, l2, 'extend');
    return (l1);
);

CMD_ALU_CLEAR_CARRY = 'fill $p1_ALU$ $p2_ALU$ minecraft:red_wool';
CMD_MAIN_NEXT = 'setblock $p_main$ minecraft:redstone_block';

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
            'summon minecraft:armor_stand $p_CPU$   {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"CPU"}\'}',
            'summon minecraft:armor_stand $p2_ALU$  {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"ALU"}\'}',
            'summon minecraft:armor_stand $p2_CMP$  {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"CMP_PTR"}\',ArmorItems:[{},{},{},{id:diamond_helmet,Count:1}]}',
            'summon minecraft:armor_stand $p_STACK$ {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"STACK"}\'}',
            // stack 0 index
            'execute at $STACK$ run fill ~ ~ ~-8 ~ ~ ~-1 $OFF$',
            //
            // ALU
            //
            CMD_ALU_CLEAR_CARRY,
            'execute at $ALU$ run fill ~-3 ~-1 ~-7 ~ ~ ~ $OFF$',
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
            'fill -30 0 -30 130 2 35 $AIR$'
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
            // copy instruction number
            'execute at $CPU$ run clone ~ ~ ~1 ~ ~ ~8 $p1_CPU_num$',
            // place redstone block
            'execute at $CPU$ run setblock ~ ~ ~9 minecraft:redstone_block',
            // advance instruction pointer
            'execute as $CPU$ at @s if block ~ ~ ~ minecraft:orange_wool run tp @s ~1 ~ ~'
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
            CMD_ALU_CLEAR_CARRY,
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
    //TODO - + | + - | - - 
    'ALU_add' -> {
        'position' -> c:'p_ALU_add',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // + +
            'execute if block $p1_ALU_A$ $OFF$ if block $p1_ALU_B$ $OFF$ run setblock $p_ALU_add_loop$ minecraft:redstone_block'
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
    //TODO - + | + - | - - | swapping A and B
    'ALU_sub' -> {
        'position' -> c:'p_ALU_sub',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // + +
            'execute if block $p1_ALU_A$ $OFF$ if block $p1_ALU_B$ $OFF$ run setblock $p_ALU_sub_loop$ minecraft:redstone_block'
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
    // Equals
    //
    'CMP_eq' -> {
        'position' -> c:'p_CMP_eq',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'setblock $p_CMP_R$ $OFF$',
            'execute if blocks $p1_CMP_A$ $p2_CMP_A$ $p1_CMP_B$ all run setblock $p_CMP_R$ $ON$'
        ]
    },
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
            // 'execute at $CMP$ unless blocks ~ ~ ~ ~ ~ ~ ~-1 ~ ~ all run setblock ~-3 ~ ~ $ON$',
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
            'execute at $CMP_PTR$ if block ~ ~ ~ $OFF$ if block ~-1 ~ ~ $ON$ if blocks $p1_CMP_XOR$ ~-3 ~ ~-1 $p1_CMP_XOR_CHK$ all run setblock $p_CMP_R$ $ON$',
                '> execute at $CMP_PTR$ run setblock ~ ~ ~-1 $AIR$',
            'execute as $CMP_PTR$ at @s run tp @s ~ ~ ~-1',
            'execute at $CMP_PTR$ if block ~ ~ ~ $AIR$ run setblock $p_CMP_gr_end$ minecraft:redstone_block',
            'execute at $CMP_PTR$ unless block ~ ~ ~ $AIR$ run setblock $p_CMP_gr_loop$ minecraft:redstone_block'
        ]
    },
    'CMP_gr_end' -> {
        'position' -> c:'p_CMP_gr_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'execute as $CMP_PTR$ at @s run tp @s $p2_CMP$'
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
            // copy number from instruction to current stack pointer
            'execute at $STACK$ run clone $p1_CPU_num$ $p2_CPU_num$ ~ ~ ~1',
            // if index doesn't exist, calc new one
            'execute at $STACK$ if block ~1 ~ ~-1 $AIR$',
                '> setblock $p_i_push_idx$ minecraft:redstone_block',
            // advance stack pointer if index exists
            'execute as $STACK$ at @s unless block ~1 ~ ~-1 $AIR$ run tp @s ~1 ~ ~',
                '> setblock $p_i_push_end$ minecraft:redstone_block'
        ]
    },
    'i_push_idx' -> {
        'position' -> c:'p_i_push_idx',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // copy stack index to ALU as A
            'execute at $STACK$ run clone ~ ~ ~-8 ~ ~ ~-1 $p1_ALU_A$',
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
            'execute as $STACK$ at @s run tp @s ~1 ~ ~',
            // copy new stack index from ALU
            'execute at $STACK$ run clone $p1_ALU_R$ $p2_ALU_R$ ~ ~ ~-8',
            // clear the i_push redstone block
            'setblock $p_i_push_end$ minecraft:redstone_block'
        ]
    },
    'i_push_end' -> {
        'position' -> c:'p_i_push_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // add callback
            'execute if block $p_i_add$ minecraft:redstone_block run setblock $p_i_add_PUSH_cb$ minecraft:redstone_block',
            // sub callback
            'execute if block $p_i_sub$ minecraft:redstone_block run setblock $p_i_sub_PUSH_cb$ minecraft:redstone_block',
            'setblock $p_i_push$ $AIR$',
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
            'execute as $STACK$ at @s run tp @s ~-1 ~ ~',
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
            'execute at $STACK$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_ALU_A$',
            'execute at $STACK$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_ALU_B$',
            'setblock $p_ALU_add$ minecraft:redstone_block'
        ]
    },
    'i_add_ALU_cb' -> {
        'position' -> c:'p_i_add_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'clone $p1_ALU_R$ $p2_ALU_R$ $p1_CPU_num$',
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
            'execute at $STACK$ run clone ~-1 ~ ~1 ~-1 ~ ~8 $p1_ALU_A$',
            'execute at $STACK$ run clone ~-2 ~ ~1 ~-2 ~ ~8 $p1_ALU_B$',
            'setblock $p_ALU_sub$ minecraft:redstone_block'
        ]
    },
    'i_sub_ALU_cb' -> {
        'position' -> c:'p_i_sub_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            'clone $p1_ALU_R$ $p2_ALU_R$ $p1_CPU_num$',
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
};

c_for(i = 0, i < 8, i += 1,
    cmdblocks:'load':'commands' += str('execute at $CMP_PTR$ run summon minecraft:armor_stand ~ ~ ~%d {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"CMP"}\'}', -i);
);

place_cmdblock(c, data) -> (
    loc = data:'position';
    dir = data:'direction';
    cmds = data:'commands';

    if (data:'redstone_block',
        put(cmds, 0, 'setblock ' + mcpos(loc) + ' minecraft:air', 'insert');
    ); 
    loc = loc + dir;

    for (cmds,
        cmd = str(_);
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
            if (type(val) == 'list', val = mcpos(val));
            cmd = replace(cmd, '\\$' + name + '\\$', val);
        );

        set(loc, str('%s[facing=%s,conditional=%s]{"auto":%s,"Command":"%s"}', cmdblock, dir_from_list(dir), cond, always_active, cmd));
        loc = loc + dir;
    );
);

for (values(cmdblocks), place_cmdblock(c,_));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

place_number(location, num, bits, line) -> (
    num_power(num, pwr) -> (
        res = 1;
        while(1, pwr, res = res * num);
        return (res);
    );

    bit_is_set(num, idx) -> (
        return (floor(num / num_power(2, idx)) % 2 == 1);
    );

    set(location, 'red_wool');
    if (num < 0,
        num = -num;
        set(location, 'lime_wool');
    );
    location:2 += 1;

    if (line, (
        set(location, 'orange_wool');
        location:2 += 1;
    ));
    c_for (bit_idx = bits - 1 - 1, bit_idx >= 0, bit_idx = bit_idx - 1, 
        blockid = (bit_is_set(num, bit_idx) && 'lime_wool') || 'red_wool';
        set(location, blockid);
        location:2 += 1;
    );
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

for (global_statements, (
    stmt = _;
    stmt_idx = _i;
    type = '???';

    if (type(stmt) == 'list', type = stmt:0,
        type(stmt) == 'string', type = stmt,
        true, error('Unknown statement "' + str(stmt) + '"'));

    if (type == 'lbl', (
        id = stmt:1;
        if (has(global_labels, id),
            warn('Label "' + id + '" has already been defined'));
        global_labels:id = stmt_idx + 1;
    ), type == 'push', (
        val = stmt:1;
        place_number([stmt_idx, 0, 1], val, 8, false);        
    ));

    set([stmt_idx, 0, 0], 'orange_wool');
    place_cmdblock(c, {
        'position' -> [stmt_idx, 0, 9],
        'direction' -> [0, 0, 1],
        'redstone_block' -> true,
        'commands' -> ['setblock $' + 'p_i_' + type + '$ minecraft:redstone_block']
    });
));
