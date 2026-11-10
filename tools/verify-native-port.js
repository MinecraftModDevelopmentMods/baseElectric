const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = '28dd438dde6898fcb6d6e1d17a62a4c0f527169e';
const legacyTrackBaseline = '013737d4cc41947e8b6fa42f4bf926a1fa5ab998';
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

check('Exact Forge 50 and release identity', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    for (const [key, value] of Object.entries({
        minecraft_version: '1.20.6', forge_version: '50.2.0', mapping_channel: 'official',
        mapping_version: '1.20.6', mcp_version: '20240429.135109', java_version: '21',
        mod_id: 'baseelectric', mod_name: 'baseElectric', mod_version: '2.2.14.120061',
        orespawn_version: '4.1.0.120061', curseforge_project_id: '1783006',
        cf_requirements: 'basepower,base-metals'
    })) assert.equal(props[key], value, key);
    assert.deepEqual(json('src/main/resources/pack.mcmeta').pack, {pack_format:32, supported_formats:{min_inclusive:32,max_inclusive:41}, description:'baseElectric'});
    const mods = read('src/main/resources/META-INF/mods.toml').toString();
    assert.match(mods, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.13\.120061,3\.0\.0\)"/);
    assert.match(mods, /modId="basemetals"\s+mandatory=false\s+versionRange="\[3\.1\.2\.120061,4\.0\.0\)"/);
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

check('Unrelated shipped resources and fixtures retain accepted 1.20.1 bytes', () => {
    const resources = tracked.filter(file => /^src\/(main|test)\/resources\//.test(file));
    const exceptions = new Set(['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml',
        assets + 'shaders/core/unshaded.vsh', assets + 'shaders/core/unshaded.json']);
    for (const file of resources.filter(file => !exceptions.has(file) && !/\/data\/baseelectric\/(recipes|advancements\/recipes)\//.test(file))) {
        assert.equal(sha256(read(file)), sha256(old(file)), file);
    }
    const actual = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(entry => entry.isFile()).map(entry => path.relative(root, path.join(entry.parentPath, entry.name))
            .split(path.sep).join('/')).sort();
    assert.deepEqual(actual, [...tracked, javaRoot.replace('main', 'test') + 'items/BatteryComponentTest.java',
        javaRoot.replace('main', 'test') + 'items/LegacyBatteryDataTest.java'].sort(),
        'Source and resource inventory');
});

check('Migration and registration code keep their accepted IDs and compatibility paths', () => {
    for (const name of [
        'migration/ElectricLegacyMigrations.java', 'init/Blocks.java', 'init/Items.java',
        'init/Entities.java', 'init/GUI.java', 'init/OreSpawnWorldGen.java'
    ]) {
        const file = javaRoot + name;
        assert.equal(sha256(read(file)), sha256(old(file)), file);
    }
});

check('All 99 accepted tests are retained, plus three battery component tests', () => {
    const methods = value => [...value.toString().matchAll(/@Test\s+public void (\w+)\(/g)].map(m => m[1]);
    const files = tracked.filter(file => file.startsWith('src/test/java/') && file.endsWith('.java'));
    let inherited = 0;
    let actual = 0;
    for (const file of files) {
        const current = read(file);
        const currentMethods = methods(current);
        const priorMethods = methods(old(file));
        for (const name of priorMethods) assert(currentMethods.includes(name), file + ' lost ' + name);
        assert.doesNotMatch(current.toString(), /@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/, file);
        inherited += priorMethods.length;
        actual += currentMethods.length;
    }
    assert.equal(inherited, 99);
    assert.equal(actual, 99);
    assert.equal(methods(read(javaRoot.replace('main', 'test') + 'items/BatteryComponentTest.java')).length, 2);
    assert.equal(methods(read(javaRoot.replace('main', 'test') + 'items/LegacyBatteryDataTest.java')).length, 1);
});


check('Recipe results and guards retain their meaning under Forge 50 codecs', () => {
    const files = tracked.filter(file => /\/data\/baseelectric\/recipes\/.*\.json$/.test(file));
    assert.equal(files.length, 58);
    for (const file of files) {
        const expected = JSON.parse(old(file));
        if (typeof expected.result === 'string') expected.result = {id: expected.result};
        else {
            const {item, ...other} = expected.result;
            expected.result = {id: item, ...other};
        }
        if (expected.conditions) {
            const conditions = expected.conditions.map(condition => condition.type === 'basepower:tag_not_empty'
                ? {type:'forge:not', value:{type:'forge:tag_empty', tag:condition.tag}} : condition);
            delete expected.conditions;
            expected['forge:condition'] = conditions.length === 1 ? conditions[0]
                : {type:'forge:and', values:conditions};
        }
        assert.deepEqual(json(file), expected, file);
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
