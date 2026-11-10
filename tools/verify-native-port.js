const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = '013737d4cc41947e8b6fa42f4bf926a1fa5ab998';
const git = (...args) => execFileSync('git', args, { cwd: root });
const old = file => git('show', baseline + ':' + file);
const read = file => fs.readFileSync(path.join(root, file));
const json = file => JSON.parse(read(file));
const sha256 = data => createHash('sha256').update(data).digest('hex');
const tracked = git('ls-tree', '-r', '--name-only', baseline, 'src').toString().trim().split('\n');
const javaRoot = 'src/main/java/zone/moddev/mc/baseelectric/';
const assets = 'src/main/resources/assets/baseelectric/';
const track = file => file.startsWith(assets + 'models/block/electric_track_') && file.endsWith('.json');
const checks = [];
function check(name, run) {
    try { run(); checks.push({name, pass: true}); }
    catch (error) { checks.push({name, pass: false, error: error.message}); }
}

check('Exact Forge 47 and release identity', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    for (const [key, value] of Object.entries({
        minecraft_version: '1.20.1', forge_version: '47.4.10', mapping_channel: 'official',
        mapping_version: '1.20.1', mcp_version: '20230612.114412', java_version: '17',
        mod_id: 'baseelectric', mod_name: 'baseElectric', mod_version: '2.2.13.120011',
        poweradvantage_version: '2.4.12.120011', basemetals_version: '3.1.2.120011',
        orespawn_version: '4.1.0.120011', curseforge_project_id: '1783006',
        cf_requirements: 'basepower,base-metals'
    })) assert.equal(props[key], value, key);
    assert.deepEqual(json('src/main/resources/pack.mcmeta').pack, {pack_format:15, description:'baseElectric'});
    const mods = read('src/main/resources/META-INF/mods.toml').toString();
    assert.match(mods, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.12\.120011,3\.0\.0\)"/);
    assert.match(mods, /modId="basemetals"\s+mandatory=false\s+versionRange="\[3\.1\.2\.120011,4\.0\.0\)"/);
});

function checkTrackModel(file, model) {
    assert.equal(model.render_type, 'minecraft:cutout', file + ' lacks its native cutout layer');
    const { render_type, ...unchanged } = model;
    assert.deepEqual(unchanged, JSON.parse(old(file)), file + ' changed beyond the render layer');
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
    assert.throws(() => checkTrackModel(file, JSON.parse(old(file))), /lacks its native cutout layer/);
    const mutated = json(file);
    mutated.elements[0].from[0]++;
    assert.throws(() => checkTrackModel(file, mutated), /changed beyond the render layer/);
});

check('Every other shipped resource and test fixture retains its accepted bytes', () => {
    const resources = tracked.filter(file => /^src\/(main|test)\/resources\//.test(file));
    const exceptions = new Set(['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml']);
    for (const file of resources.filter(file => !exceptions.has(file) && !track(file))) {
        assert.equal(sha256(read(file)), sha256(old(file)), file);
    }
    const actual = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(entry => entry.isFile()).map(entry => path.relative(root, path.join(entry.parentPath, entry.name))
            .split(path.sep).join('/')).sort();
    assert.deepEqual(actual, [...tracked].sort(), 'Source and resource inventory');
});

check('Migration and registration code keep their accepted IDs and compatibility paths', () => {
    for (const name of [
        'migration/ElectricLegacyMigrations.java', 'init/Blocks.java', 'init/Items.java',
        'init/Entities.java', 'init/GUI.java', 'init/OreSpawnWorldGen.java', 'recipe/RecipeConditions.java'
    ]) {
        const file = javaRoot + name;
        assert.equal(sha256(read(file)), sha256(old(file)), file);
    }
});

check('All 96 inherited tests are still present, plus three new assertions', () => {
    const methods = value => [...value.toString().matchAll(/@Test\s+public void (\w+)\(/g)].map(m => m[1]);
    const files = tracked.filter(file => file.startsWith('src/test/java/') && file.endsWith('.java'));
    let inherited = 0;
    let actual = 0;
    for (const file of files) {
        const current = read(file);
        const currentMethods = methods(current);
        const priorMethods = methods(old(file)).map(name => name === 'resourcePackDeclaresTheNativeOneNineteenFourFormats'
            ? 'resourcePackDeclaresTheNativeOneTwentyOneFormat' : name);
        for (const name of priorMethods) assert(currentMethods.includes(name), file + ' lost ' + name);
        assert.doesNotMatch(current.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/, file);
        inherited += priorMethods.length;
        actual += currentMethods.length;
    }
    assert.equal(inherited, 96);
    assert.equal(actual, 99);
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
