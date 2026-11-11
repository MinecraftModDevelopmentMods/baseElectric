const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = 'b81645579c3330476b84149402c3ce78183df85f';
const legacyTrackBaseline = '013737d4cc41947e8b6fa42f4bf926a1fa5ab998';
const git = (...args) => execFileSync('git', args, { cwd: root });
const old = file => git('show', baseline + ':' + file);
const read = file => fs.readFileSync(path.join(root, file));
const json = file => JSON.parse(read(file));
const sha256 = data => createHash('sha256').update(data).digest('hex');
const tracked = git('ls-tree', '-r', '--name-only', baseline, 'src').toString().trim().split('\n');
const javaRoot = 'src/main/java/zone/moddev/mc/baseelectric/';
const assets = 'src/main/resources/assets/baseelectric/';
const relocated = file => file.replace(/(data\/[^/]+\/)recipes\//, '$1recipe/')
    .replace(/(data\/[^/]+\/)advancements\//, '$1advancement/')
    .replace(/(data\/[^/]+\/)loot_tables\//, '$1loot_table/')
    .replace(/(data\/[^/]+\/tags\/)items\//, '$1item/')
    .replace(/(data\/[^/]+\/tags\/)blocks\//, '$1block/');
function nativeIngredients(recipe) {
    const ingredient = value => Array.isArray(value) ? value.map(ingredient)
        : typeof value === 'string' ? value : value.item || '#' + value.tag;
    if (recipe.key) recipe.key = Object.fromEntries(Object.entries(recipe.key).map(([key, value]) => [key, ingredient(value)]));
    if (recipe.ingredient) recipe.ingredient = ingredient(recipe.ingredient);
    if (recipe.ingredients) recipe.ingredients = ingredient(recipe.ingredients);
    return recipe;
}
const track = file => file.startsWith(assets + 'models/block/electric_track_') && file.endsWith('.json');
const checks = [];
function check(name, run) {
    try { run(); checks.push({name, pass: true}); }
    catch (error) { checks.push({name, pass: false, error: error.message}); }
}

check('Exact Forge 61 and local port identity', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    for (const [key, value] of Object.entries({
        minecraft_version: '1.21.11', forge_version: '61.1.0', mapping_channel: 'official',
        mapping_version: '1.21.11', mcp_version: '20251223.124241', java_version: '21',
        mod_id: 'baseelectric', mod_name: 'baseElectric', mod_version: '2.2.16.121111',
        poweradvantage_version: '2.4.15.121111', basemetals_version: '3.1.2.121111',
        poweradvantage_pin_state: 'LOCAL_SOURCE',
        poweradvantage_source_ref: '41959572b11dce236df98f23e97c6a3868cdda03',
        poweradvantage_deobf_sha256: 'C98D2CF163BE2480D9AFB0023BBD2D92D401AE0D50BEF73191235BF3F83774A7',
        poweradvantage_runtime_sha256: 'C98D2CF163BE2480D9AFB0023BBD2D92D401AE0D50BEF73191235BF3F83774A7',
        basemetals_source_commit: '616d527e50e4bf0137eeb9c44fa586e34a0452a0',
        basemetals_deobf_sha256: '598D3FB0FF956EBCC67FE477A01CE393C75D0013AF0CE00A79E2072B63E9F2BA',
        basemetals_runtime_sha256: '598D3FB0FF956EBCC67FE477A01CE393C75D0013AF0CE00A79E2072B63E9F2BA',
        orespawn_source_commit: 'ecad75b24309a344f712d898d717c6a8823fc9f2',
        orespawn_deobf_sha256: 'DE39929057288CEA110196C9E9D708E129B80BC360A2E53976337FFA6A84A67B',
        orespawn_version: '4.1.0.121111', curseforge_project_id: '1783006',
        cf_requirements: 'basepower,base-metals'
    })) assert.equal(props[key], value, key);
    assert.deepEqual(json('src/main/resources/pack.mcmeta').pack,
        {min_format:[94,1], max_format:[94,1], description:'baseElectric'});
    const mods = read('src/main/resources/META-INF/mods.toml').toString();
    assert.match(mods, /^showAsResourcePack=false$/m);
    assert.match(mods, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.15\.121111,3\.0\.0\)"/);
    assert.match(mods, /modId="basemetals"\s+mandatory=true\s+versionRange="\[3\.1\.2\.121111,4\.0\.0\)"/);
});

function checkTrackModel(file, model) {
    assert.equal(model.render_type, 'minecraft:cutout', file + ' lacks its native cutout layer');
    const { render_type, ...unchanged } = model;
    assert.deepEqual(unchanged, JSON.parse(git('show', legacyTrackBaseline + ':' + file)), file + ' changed beyond the render layer');
}

check('All 64 track states declare cutout without changing any geometry', () => {
    const { variants } = json(assets + 'blockstates/electric_track.json');
    assert.equal(Object.keys(variants).length, 64);
    const models = new Set(Object.values(variants).map(variant => variant.model));
    for (const id of models) {
        assert.match(id, /^baseelectric:block\/electric_track_/);
        const file = assets + 'models/' + id.split(':')[1] + '.json';
        checkTrackModel(file, json(file));
    }
    for (const file of tracked.filter(track)) checkTrackModel(file, json(file));
});

check('Track audit rejects the old asset and a geometry mutation', () => {
    const file = assets + 'models/block/electric_track_ns.json';
    assert.throws(() => checkTrackModel(file, JSON.parse(git('show', legacyTrackBaseline + ':' + file))), /lacks its native cutout layer/);
    const mutated = json(file);
    mutated.elements[0].from[0]++;
    assert.throws(() => checkTrackModel(file, mutated), /changed beyond the render layer/);
});

check('Accepted 1.21.1 resources retain bytes except native recipe schema and pack metadata', () => {
    const resources = tracked.filter(file => /^src\/(main|test)\/resources\//.test(file));
    const exceptions = new Set(['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml']);
    for (const file of resources.filter(file => !exceptions.has(file))) {
        if (file.startsWith('src/main/resources/data/baseelectric/recipe/')) {
            assert.deepEqual(json(file), nativeIngredients(JSON.parse(old(file))), file);
            continue;
        }
        assert.equal(sha256(read(relocated(file))), sha256(old(file)), file);
        if (relocated(file) !== file) assert(!fs.existsSync(path.join(root, file)), 'Obsolete registry path: ' + file);
    }
    const actual = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(entry => entry.isFile()).map(entry => path.relative(root, path.join(entry.parentPath, entry.name))
            .split(path.sep).join('/')).sort();
    const definitions = tracked.filter(file => file.startsWith(assets + 'models/item/') && file.endsWith('.json'))
        .map(file => file.replace('/models/item/', '/items/'));
    assert.equal(definitions.length, 44);
    for (const file of definitions) {
        const id = path.basename(file, '.json');
        assert.deepEqual(json(file), {model:{type:'minecraft:model', model:'baseelectric:item/' + id}}, file);
    }
    assert.deepEqual(actual, [...tracked.map(relocated), ...definitions,
        javaRoot + 'util/RegistryIds.java', javaRoot + 'util/LegacyNbt.java',
        javaRoot.replace('main', 'test') + 'util/crafting/RecipeDeconstructorTest.java',
        javaRoot.replace('main', 'test') + 'resources/NativePackRangeContractTest.java',
        javaRoot.replace('main', 'test') + 'verification/NativePackResourceProof.java',
        ...['drillupgradesandtransferslegacybattery', 'recipeavailabilitydoesnotremovediscovery',
            'siliconandsolderresolverealmaterialtags', 'turretusesnativeupdatepacket']
            .map(name => 'src/gametest/resources/data/baseelectric/test_instance/' + name + '.json')].sort(),
        'Source and resource inventory');
});

check('Migration and registration code keep their accepted IDs and compatibility paths', () => {
    for (const name of [
        'migration/ElectricLegacyMigrations.java', 'init/Blocks.java', 'init/Items.java',
        'init/Entities.java', 'init/GUI.java', 'init/OreSpawnWorldGen.java'
    ]) {
        const file = javaRoot + name;
        const strings = source => new Set(source.toString().match(/"(?:\\.|[^"\\])*"/g));
        const current = strings(read(file));
        for (const id of strings(old(file))) assert(current.has(id), file + ' lost ' + id);
    }
    const migration = javaRoot + 'migration/ElectricLegacyMigrations.java';
    assert.equal(read(migration).toString().replace(/\s/g,''), old(migration).toString()
        .replaceAll('ResourceLocation', 'Identifier').replace(/\s/g,''));
});

check('All 106 accepted tests and their assertions are retained without skips', () => {
    const methods = value => [...value.toString().matchAll(/@Test\s+public void (\w+)\(/g)].map(m => m[1]);
    const files = tracked.filter(file => file.startsWith('src/test/java/') && file.endsWith('.java'));
    let inherited = 0;
    let actual = 0;
    for (const file of files) {
        const current = read(file);
        const currentMethods = methods(current);
        const priorMethods = methods(old(file));
        for (const name of priorMethods) assert(currentMethods.includes(name), file + ' lost ' + name);
        const assertions = value => [...value.toString().matchAll(/\bassert\w*\s*\(/g)].length;
        assert(assertions(current) >= assertions(old(file)), file + ' lost assertions');
        assert.doesNotMatch(current.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/, file);
        inherited += priorMethods.length;
        actual += currentMethods.length;
    }
    assert.equal(inherited, 106);
    assert.equal(actual, 106);
    const added = read(javaRoot.replace('main','test') + 'util/crafting/RecipeDeconstructorTest.java');
    assert.equal(methods(added).length, 2);
    assert.doesNotMatch(added.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/);
    const pack = read(javaRoot.replace('main', 'test') + 'resources/NativePackRangeContractTest.java');
    assert.equal(methods(pack).length, 6);
    assert.doesNotMatch(pack.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/);
    const gameTests = javaRoot.replace('main', 'gametest') + 'gametest/ElectricGameTests.java';
    const names = value => [...value.toString().matchAll(/public static void (\w+)\(GameTestHelper/g)].map(m => m[1]);
    for (const name of names(old(gameTests))) assert(names(read(gameTests)).includes(name), 'Lost GameTest: ' + name);
});


check('All 58 recipe IDs, results, guards and ingredients are unchanged', () => {
    const files = tracked.filter(file => /\/data\/baseelectric\/recipe\/.*\.json$/.test(file));
    assert.equal(files.length, 58);
    for (const file of files) {
        const expected = nativeIngredients(JSON.parse(old(file)));
        assert.deepEqual(json(relocated(file)), expected, file);
    }
});

const report = {baseline, scope: 'Static source and resource comparison only', checks};
const flag = process.argv.indexOf('--report');
if (flag >= 0) {
    const output = path.resolve(root, process.argv[flag + 1]);
    assert(output.startsWith(path.join(root, 'build', 'port-evidence') + path.sep));
    fs.mkdirSync(path.dirname(output), {recursive:true});
    fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n', {flag:'wx'});
}
for (const result of checks) console.log(result.pass ? 'PASS ' + result.name : 'FAIL ' + result.name + ': ' + result.error);
process.exitCode = checks.every(result => result.pass) ? 0 : 1;
