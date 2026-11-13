// Rebuild the checked-in recipe-book advancements; Node is not needed for normal builds.
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const properties = fs.readFileSync(path.join(root, 'gradle.properties'), 'utf8');
const mod = /^mod_id=(.+)$/m.exec(properties)[1].trim();
const data = path.join(root, 'src/main/resources/data', mod);
const recipes = path.join(data, 'recipe');
const output = path.join(data, 'advancement/recipes');
const tagOutput = path.join(data, 'tags/recipe/discovery');
const check = process.argv.includes('--check');
const ignored = new Set(['item:minecraft:stick', 'item:minecraft:string',
  'item:minecraft:feather', 'tag:forge:rods/wooden']);

function choices(value) {
  if (Array.isArray(value)) return value.flatMap(choices);
  if (typeof value === 'string') return value.startsWith('#') ? [{tag: value.slice(1)}] : [{item: value}];
  if (value && value.tag) return [{tag: value.tag}];
  if (value && value.item) return [{item: value.item}];
  throw new Error('Unsupported recipe ingredient: ' + JSON.stringify(value));
}

const expected = new Set();
for (const file of fs.readdirSync(recipes).filter(name => name.endsWith('.json')).sort()) {
  const recipe = JSON.parse(fs.readFileSync(path.join(recipes, file), 'utf8'));
  if (!['minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'minecraft:smelting'].includes(recipe.type)) continue;
  const inputs = choices(recipe.key ? Object.values(recipe.key) : recipe.ingredients || recipe.ingredient);
  const materials = inputs.filter(value => !ignored.has(Object.entries(value)[0].join(':')));
  const unique = [...new Map((materials.length ? materials : inputs).map(value => [JSON.stringify(value), value])).values()];
  const id = `${mod}:${file.slice(0, -5)}`;
  const criteria = {};
  unique.forEach((ingredient, index) => {
    criteria[`has_ingredient_${index + 1}`] = {
      trigger: 'minecraft:inventory_changed',
      conditions: {items: [{items: ingredient.item ? [ingredient.item] : '#' + ingredient.tag}]}
    };
  });
  criteria.has_recipe = {trigger: 'minecraft:recipe_unlocked', conditions: {recipes: `#${mod}:discovery/${file.slice(0, -5)}`}};
  const advancement = {
    parent: 'minecraft:recipes/root', criteria,
    requirements: [Object.keys(criteria)], rewards: {recipes: [id]}
  };
  expected.add(file);
  const tag = {values: [{id, required: false}]};
  for (const [directory, value] of [[output, advancement], [tagOutput, tag]]) {
    const text = JSON.stringify(value, null, 2) + '\n';
    const target = path.join(directory, file);
    if (check) {
      if (!fs.existsSync(target) || fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n') !== text) {
        throw new Error('Missing/stale recipe discovery: ' + target);
      }
    } else {
      fs.mkdirSync(directory, {recursive: true});
      fs.writeFileSync(target, text);
    }
  }
}
for (const directory of [output, tagOutput]) {
  for (const file of fs.existsSync(directory) ? fs.readdirSync(directory) : []) {
    if (file.endsWith('.json') && !expected.has(file)) throw new Error('Unexpected recipe discovery: ' + path.join(directory, file));
  }
}
console.log(`${mod}: ${expected.size} unconditional unlocks and optional recipe tags ${check ? 'verified' : 'generated'}`);
