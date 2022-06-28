data = read_file('data', 'json');
global_instruction_set = data:'instruction_set';
global_statements = data:'program';

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
    'p_ALU_add' -> [-10, 0, 9],
    'p_ALU_add_end' -> [-11, 0, 9],

    'STACK' -> '@e[type=minecraft:armor_stand,name=STACK]',
    'p_STACK' -> [0, 0, -10],

    'p_i_push' -> [0, 0, 13],
    'p_i_push_ALU_cb' -> [0, 2, 13]
};

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

// CPU
c:'p1_CPU_num' = c:'p_CPU' + [-1,0,1];
c:'p2_CPU_num' = c:'p_CPU' + [-1,0,8];

CMD_CLEAR_ALU_CARRY = 'fill %p1_ALU% %p2_ALU% minecraft:red_wool';
CMD_MAIN_NEXT = 'setblock %p_main% minecraft:redstone_block';

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
            'summon minecraft:armor_stand %p_CPU%   {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"CPU"}\'}',
            'summon minecraft:armor_stand %p2_ALU%  {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"ALU"}\'}',
            'summon minecraft:armor_stand %p_STACK% {NoGravity:1b,CustomNameVisible:1b,CustomName:\'{"text":"STACK"}\'}',
            // stack 0 index
            'execute at %STACK% run fill ~ ~ ~-8 ~ ~ ~-1 minecraft:red_wool',
            //
            // ALU
            //
            CMD_CLEAR_ALU_CARRY,
            'execute at %ALU% run fill ~-3 ~-1 ~-7 ~ ~ ~ minecraft:red_wool'
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
            'fill -30 0 -30 30 2 30 minecraft:air'
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
            'execute at %CPU% run clone ~ ~ ~1 ~ ~ ~8 %p1_CPU_num%',
            // place redstone block
            'execute at %CPU% run setblock ~ ~ ~9 minecraft:redstone_block',
            // advance instruction pointer
            'execute as %CPU% at @s if block ~ ~ ~ minecraft:orange_wool run tp @s ~1 ~ ~'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ALU
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'ALU_add' -> {
        'position' -> c:'p_ALU_add',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // A=1 B=1 set result to carry
            'execute at %ALU% if block ~-1 ~ ~ minecraft:lime_wool if block ~-2 ~ ~ minecraft:lime_wool run clone ~ ~ ~ ~ ~ ~ ~-3 ~ ~',
                // set next carry
                '> execute at %ALU% run setblock ~ ~ ~-1 minecraft:lime_wool',
            // A=0 B=0 set result to carry
            'execute at %ALU% if block ~-1 ~ ~ minecraft:red_wool run execute if block ~-2 ~ ~ minecraft:red_wool run clone ~ ~ ~ ~ ~ ~ ~-3 ~ ~',
            // C=0 A=0 B=1
            'execute at %ALU% if block ~ ~ ~ minecraft:red_wool if block ~-1 ~ ~ minecraft:red_wool if block ~-2 ~ ~ minecraft:lime_wool run setblock ~-3 ~ ~ minecraft:lime_wool',
            // C=0 A=1 B=0
            'execute at %ALU% if block ~ ~ ~ minecraft:red_wool if block ~-1 ~ ~ minecraft:lime_wool if block ~-2 ~ ~ minecraft:red_wool run setblock ~-3 ~ ~ minecraft:lime_wool',
            // C=1 A=0 B=1
            'execute at %ALU% if block ~ ~ ~ minecraft:lime_wool if block ~-1 ~ ~ minecraft:red_wool if block ~-2 ~ ~ minecraft:lime_wool run setblock ~-3 ~ ~ minecraft:red_wool',
                // set next carry
                '> execute at %ALU% run setblock ~ ~ ~-1 minecraft:lime_wool',
            // C=1 A=1 B=0
            'execute at %ALU% if block ~ ~ ~ minecraft:lime_wool if block ~-1 ~ ~ minecraft:lime_wool if block ~-2 ~ ~ minecraft:red_wool run setblock ~-3 ~ ~ minecraft:red_wool',
                // set next carry
                '> execute at %ALU% run setblock ~ ~ ~-1 minecraft:lime_wool',
            // advance ALU pointer
            'execute as %ALU% at @s run tp @s ~ ~ ~-1',
            //
            'execute at %ALU% if blocks %p3_ALU% ~ ~ ~ %p4_ALU% all run setblock ~ ~ ~ minecraft:air',
            // LOOP
            'execute at %ALU% if block ~ ~ ~ minecraft:lime_wool run setblock %p_ALU_add% minecraft:redstone_block',
            'execute at %ALU% if block ~ ~ ~ minecraft:red_wool run setblock %p_ALU_add% minecraft:redstone_block',
            // reset ALU pointer if reached end
            'execute as %ALU% at @s if block ~ ~ ~ minecraft:air run tp @s %p2_ALU%',
                '> setblock %p_ALU_add_end% minecraft:redstone_block'
        ]
    },
    'ALU_add_end' -> {
        'position' -> c:'p_ALU_add_end',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            CMD_CLEAR_ALU_CARRY,
            // push callback
            'execute if block %p_i_push% minecraft:redstone_block run setblock %p_i_push_ALU_cb% minecraft:redstone_block'
        ]
    },
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instruction -- Push
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    'i_push' -> {
        'position' -> c:'p_i_push',
        'direction' -> [0,0,1],
        'commands' -> [
            // copy number from instruction to current stack pointer
            'execute at %STACK% run clone %p1_CPU_num% %p2_CPU_num% ~ ~ ~1',
            // copy stack index to ALU as A
            'execute at %STACK% run clone ~ ~ ~-8 ~ ~ ~-1 %p1_ALU_A%',
            // set ALU B to 1
            'fill %p1_ALU_B% %p2_ALU_B% minecraft:red_wool',
            'setblock %p2_ALU_B% minecraft:lime_wool',
            // add A + B
            'setblock %p_ALU_add% minecraft:redstone_block'
        ]
    },
    'i_push_ALU_cb' -> {
        'position' -> c:'p_i_push_ALU_cb',
        'direction' -> [0,0,1],
        'redstone_block' -> true,
        'commands' -> [
            // advance stack pointer
            'execute as %STACK% at @s run tp @s ~1 ~ ~',
            // copy new stack index from ALU
            'execute at %STACK% run clone %p1_ALU_R% %p2_ALU_R% ~ ~ ~-8',
            // clear the i_push redstone block
            'setblock %p_i_push% minecraft:air',
            CMD_MAIN_NEXT
        ]
    }
};

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
            cmd = replace(cmd, '%' + name + '%', val);
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

num_power(num, pwr) -> (
    res = 1;
    while(1, pwr, res = res * num);
    return (res);
);

bit_is_set(num, idx) -> (
    return (floor(num / num_power(2, idx)) % 2 == 1);
);

mem_set_number(location, num, bits, line) -> (
    if (line, (
        set(location, 'orange_wool');
        location:2 += 1;
    ));
    c_for (bit_idx = bits - 1, bit_idx >= 0, bit_idx = bit_idx - 1, 
        blockid = (bit_is_set(num, bit_idx) && 'lime_wool') || 'red_wool';
        set(location, blockid);
        location:2 += 1;
    );
);

mem_del_number(location, bits) -> (
    while (1, bits, 
        set(location, 'air');
        location:2 += 1;
    );
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

c_for (i = 0, i <= 255, i += 1, 
    mem_set_number(c:'p_STACK' + [i,0,-8], i, 8, false);
);

//
// First pass
//
// stmt_num = 0;
for (global_statements, (
    stmt = _;
    stmt_idx = _i;
    code = stmt:0;
    type = stmt:1;

    if (type == 'lbl', (
        id = stmt:2;
        if (has(global_labels, id),
            warn('Label "' + id + '" has already been defined'));
        global_labels:id = stmt_idx + 1;
    ), type == 'push', (
        val = stmt:2;
        mem_set_number([stmt_idx, 0, 1], val, 8, false);        
    ));

    set([stmt_idx, 0, 0], 'orange_wool');
    place_cmdblock(c, {
        'position' -> [stmt_idx, 0, 9],
        'direction' -> [0, 0, 1],
        'redstone_block' -> true,
        'commands' -> ['setblock %' + 'p_i_' + type + '% minecraft:redstone_block']
    });
));

//
// Second pass
//
c_for (stmt_idx = 0, stmt_idx < length(global_statements), stmt_idx += 1,
    stmt = global_statements:stmt_idx;
    type = stmt:1;

    jump(curidx, lbl) -> (
        if (has(global_labels, lbl), (
            return(global_labels:lbl - 1);
        ));
        print('Label "' + lbl + '" doesn\'t exist');
        return(curidx);
    );

    stack_top() -> global_stack:(-1);
    stack_push(v) -> (
        put(global_stack, null, v);
    );
    stack_set(idx, val) -> (
        global_stack:idx = val;
    );
    stack_del(idx) -> (
        delete(global_stack, idx);
    );
    stack_gettop(i) -> global_stack:(-(i+1));

    //
    // Other
    //
    if (type == 'const', (
        id = stmt:2;
        val = stmt:3;
        if (has(global_constants, id), warn('Constant "' + id + '" has already been defined'));
        global_constants:id = val;
    ),
        type == 'printtop', (
            print('top: ' + stack_top());
        ),
        type == 'printstack', (
            print('stack: ' + str(global_stack));
        ),
        //
        // Stack manipulation
        //
        type == 'push', (
            stack_push(stmt:2);
        ),
        type == 'pop', (
            times = stmt:2 || 1;
            while(1, times, stack_del(-1));
        ),
        type == 'get', (
            stack_push(global_stack:(stack_top()));
        ),
        type == 'set', (
            stack_set(stack_top(), stack_gettop(1));
        ),
        type == 'del', (
            stack_delete(stack_top());
        ),
        //
        // Maths
        //
        type == 'add', (
            stack_push(stack_gettop(1) + stack_top());
        ),
        type == 'sub', (
            stack_push(stack_gettop(1) - stack_top());
        ),
        type == 'mul', (
            stack_push(stack_gettop(1) * stack_top());
        ),
        type == 'div', (
            stack_push(stack_gettop(1) / stack_top());
        ),
        //
        // Control flow
        //
        type == 'jmp', (
            stmt_idx = jump(stmt_idx, stmt:2);
        ),
        type == 'je', (
            if (stack_top() == stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'jne', (
            if (stack_top() != stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'jl', (
            if (stack_top() < stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'jle', (
            if (stack_top() <= stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'jg', (
            if (stack_top() > stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'jge', (
            if (stack_top() >= stack_gettop(1), stmt_idx = jump(stmt_idx, stmt:2));
        ),
        type == 'call', (
            old_stmt_idx = stmt_idx;
            stmt_idx = jump(stmt_idx, stmt:2);
            if (stmt_idx != old_stmt_idx, put(global_call_stack, null, old_stmt_idx + 1));
        ),
        type == 'ret', (
            if (length(global_call_stack) > 0, (
                stmt_idx = global_call_stack:(-1) - 1;
                delete(global_call_stack, -1);
            ));
        )
    );
)
