// Export all the code generators for our custom blocks,
// but don't register them with Blockly yet.
// This file has no side effects!
const forBlock = Object.create(null);

// 出力ブロックの Lua コード生成
forBlock['print_text'] = function (block, generator) {
  const text = generator.valueToCode(block, 'TEXT', generator.ORDER_NONE) || "''";
  const code = `print(${text})\n`;
  return code;
};

// 移動ブロックの Lua コード生成
forBlock['move'] = function (block, generator) {
  const direction = block.getFieldValue('DIRECTION');
  const code = `pico.move("${direction}")\n`;
  return code;
};

// 回転ブロックの Lua コード生成
forBlock['turn'] = function (block, generator) {
  const direction = block.getFieldValue('DIRECTION');
  const code = `pico.turn("${direction}")\n`;
  return code;
};