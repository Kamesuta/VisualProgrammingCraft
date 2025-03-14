const printText = {
  type: 'print_text',
  message0: '「%1」を表示する',
  args0: [
    {
      type: 'input_value',
      name: 'TEXT',
      check: 'String',
    },
  ],
  previousStatement: null,
  nextStatement: null,
  colour: 160,
  tooltip: 'テキストを表示します',
  helpUrl: '',
};

const moveBlock = {
  type: 'move',
  message0: '%1 移動',
  args0: [
    {
      type: 'field_dropdown',
      name: 'DIRECTION',
      options: [
        ['前へ', 'forward'],
        ['後ろへ', 'back'],
        ['左へ', 'left'],
        ['右へ', 'right'],
        ['上へ', 'up'],
        ['下へ', 'down'],
      ],
    },
  ],
  previousStatement: null,
  nextStatement: null,
  colour: 230,
  tooltip: '指定した方向に移動します',
  helpUrl: '',
};

const turnBlock = {
  type: 'turn',
  message0: '%1 旋回',
  args0: [
    {
      type: 'field_dropdown',
      name: 'DIRECTION',
      options: [
        ['左へ', 'left'],
        ['右へ', 'right'],
      ],
    },
  ],
  previousStatement: null,
  nextStatement: null,
  colour: 230,
  tooltip: '指定した方向に回転します',
  helpUrl: '',
};

// Blockly にブロックを登録
const blocks = Blockly.common.createBlockDefinitionsFromJsonArray([
  printText,
  moveBlock,
  turnBlock,
]);