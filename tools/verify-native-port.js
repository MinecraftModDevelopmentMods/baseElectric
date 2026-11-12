const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = '951fc3921f27e963e96f29c78a38ee7093a65fde';
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

check('Exact Forge 64 and local port identity', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    for (const [key, value] of Object.entries({
        minecraft_version: '26.1.2', forge_version: '64.1.5', mapping_channel: 'official',
        mapping_version: '26.1.2', mcp_version: '20260409.101008', java_version: '25',
        mod_id: 'baseelectric', mod_name: 'baseElectric', mod_version: '2.2.17.2601021',
        poweradvantage_version: '2.4.16.2601021', basemetals_version: '3.1.2.2601021',
        orespawn_source_commit: '101fd12b598934c353e417c57edc5c2bbefd5f53',
        orespawn_deobf_sha256: '810284ECB32D5249381F246498CEFA57E70FFF7CCC40110F799FC276CBC42BF7',
        orespawn_version: '4.1.1.2601021', curseforge_project_id: '1783006',
        cf_requirements: 'basepower,base-metals'
    })) assert.equal(props[key], value, key);
    assert.equal(props.reobf, 'false');
    assert.deepEqual(json('src/main/resources/pack.mcmeta').pack,
        {min_format:[101,1], max_format:[101,1], description:'baseElectric'});
    const mods = read('src/main/resources/META-INF/mods.toml').toString();
    assert.match(mods, /^showAsResourcePack=false$/m);
    assert.match(mods, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.16\.2601021,3\.0\.0\)"/);
    assert.match(mods, /modId="basemetals"\s+mandatory=true\s+versionRange="\[3\.1\.2\.2601021,4\.0\.0\)"/);
    const build = read('build.gradle').toString();
    assert.match(build, /inputs\.files\(javadocTool\.map/);
    assert.match(build, /legal\/java\.base/);
    assert.match(build, /filesMatching\('\*\*\/\*\.svg', normalizeArchiveLineEndings\)/);
    assert.match(build, /must contain the selected JDK's full canonical legal text/);
});

check('Native source and artifact pins are complete before qualification', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    assert(['LOCAL_SOURCE', 'MERGED_SOURCE'].includes(props.poweradvantage_pin_state));
    for (const prefix of ['poweradvantage', 'basemetals', 'orespawn']) {
        const source = props[prefix + (prefix === 'poweradvantage' ? '_source_ref' : '_source_commit')];
        assert.match(source, /^[a-f0-9]{40}$/, prefix + ' source pin is pending');
        assert.match(props[prefix + '_deobf_sha256'], /^[A-F0-9]{64}$/, prefix + ' artifact pin is pending');
        assert.equal(props[prefix + '_deobf_sha256'], props[prefix + '_runtime_sha256']);
    }
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

check('Released resources retain bytes except exact native metadata and atlas repairs', () => {
    const resources = tracked.filter(file => /^src\/(main|test)\/resources\//.test(file));
    const removedAbstract = assets + 'items/battery_base.json';
    const blocksAtlas = 'src/main/resources/assets/minecraft/atlases/blocks.json';
    const itemsAtlas = 'src/main/resources/assets/minecraft/atlases/items.json';
    const exceptions = new Set(['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml', blocksAtlas, removedAbstract]);
    assert(!fs.existsSync(path.join(root, removedAbstract)), 'Abstract battery parent must not be a standalone item definition');
    const sprites = new Set();
    for (const [atlas, count] of [[blocksAtlas, 49], [itemsAtlas, 17]]) {
        const entries = json(atlas).sources;
        assert.equal(entries.length, count, atlas);
        for (const source of entries) {
            assert.equal(source.type, 'minecraft:single', atlas + ' has a non-owned directory or filter');
            assert.match(source.resource, /^baseelectric:[a-z0-9_./-]+$/);
            assert(!('sprite' in source), 'Unexpected sprite alias');
            assert(!sprites.has(source.resource), 'Duplicate block/item atlas sprite: ' + source.resource);
            sprites.add(source.resource);
            assert(fs.existsSync(path.join(root, assets + 'textures/' + source.resource.split(':')[1] + '.png')));
        }
    }
    assert(sprites.has('baseelectric:entity/laser_turret'));
    assert(sprites.has('baseelectric:materials/machine_case'));
    assert(!sprites.has('baseelectric:materials/laser_beam'), 'Direct-render beam must not enter a mipmapped atlas');
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
        .map(file => file.replace('/models/item/', '/items/')).filter(file => file !== removedAbstract);
    assert.equal(definitions.length, 43);
    for (const file of definitions) {
        const id = path.basename(file, '.json');
        assert.deepEqual(json(file), {model:{type:'minecraft:model', model:'baseelectric:item/' + id}}, file);
    }
    const trades = ['electric_conduit', 'solder', 'integrated_circuit', 'blank_circuit_board', 'petrolplastic_ingot', 'psu']
        .map(id => 'src/main/resources/data/baseelectric/villager_trade/librarian/' + id + '.json');
    const tiers = [1,2].map(level => 'src/main/resources/data/minecraft/tags/villager_trade/librarian/level_' + level + '.json');
    assert.deepEqual(actual, [...tracked.filter(file => file !== javaRoot + 'init/Villages.java' && file !== removedAbstract), itemsAtlas, ...trades, ...tiers].sort(),
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

check('All 114 released tests retain their coverage without skips', () => {
    const methods = value => [...value.toString().matchAll(/@Test\s+public void (\w+)\(/g)].map(m => m[1]);
    const files = tracked.filter(file => file.startsWith('src/test/java/') && file.endsWith('.java'));
    const renamed = {
        hostedBuildsProvisionAllThreePinnedToolchainsBeforeGradle: 'hostedBuildsPinJava25ForEveryRoleBeforeGradle',
        resourcePackDeclaresTheNativeOneTwentyOneFormat: 'resourcePackDeclaresTheNativeTwentySixFormat'
    };
    let inherited = 0;
    let actual = 0;
    for (const file of files) {
        const current = read(file);
        const currentMethods = methods(current);
        const priorMethods = methods(old(file));
        for (const name of priorMethods) assert(currentMethods.includes(renamed[name] || name), file + ' lost ' + name);
        const assertions = value => [...value.toString().matchAll(/\bassert\w*\s*\(/g)].length;
        assert(assertions(current) >= assertions(old(file)), file + ' lost assertions');
        assert.doesNotMatch(current.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/, file);
        inherited += priorMethods.length;
        actual += currentMethods.length;
    }
    assert.equal(inherited, 114);
    assert.equal(actual, 117);
    const gameTests = javaRoot.replace('main', 'gametest') + 'gametest/ElectricGameTests.java';
    const names = value => [...value.toString().matchAll(/public static void (\w+)\(GameTestHelper/g)].map(m => m[1]);
    for (const name of names(old(gameTests))) assert(names(read(gameTests)).includes(name), 'Lost GameTest: ' + name);
});


check('Official-name production code contains no obsolete SRG field or method names', () => {
    const files = fs.readdirSync(path.join(root, javaRoot), {recursive:true, withFileTypes:true})
        .filter(entry => entry.isFile() && entry.name.endsWith('.java'));
    for (const entry of files) {
        const file = path.join(entry.parentPath, entry.name);
        assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /\b(?:f_\d+_|m_\d+_|field_\d+_[A-Za-z_]+|func_\d+_[A-Za-z_]+)\b/,
            path.relative(root, file) + ' still uses obsolete mapped field/method names');
    }
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
