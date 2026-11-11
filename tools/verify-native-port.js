const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = '8160dba30c5146ff50b8ce0c314085f965fb2964';
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
const nativeLocations = source => source.toString()
    .replace(/new ResourceLocation\(\s*([\w.]+|"[^"]+"),/g, 'ResourceLocation.fromNamespaceAndPath($1,')
    .replaceAll('new ResourceLocation(', 'ResourceLocation.parse(');
const track = file => file.startsWith(assets + 'models/block/electric_track_') && file.endsWith('.json');
const checks = [];
function check(name, run) {
    try { run(); checks.push({name, pass: true}); }
    catch (error) { checks.push({name, pass: false, error: error.message}); }
}

check('Exact Forge 52 and local port identity', () => {
    const props = Object.fromEntries(read('gradle.properties').toString().split(/\r?\n/)
        .filter(line => /^[a-z][a-z_\d]*=/.test(line)).map(line => line.split(/=(.*)/s).slice(0, 2)));
    for (const [key, value] of Object.entries({
        minecraft_version: '1.21.1', forge_version: '52.1.0', mapping_channel: 'official',
        mapping_version: '1.21.1', mcp_version: '20240808.132146', java_version: '21',
        mod_id: 'baseelectric', mod_name: 'baseElectric', mod_version: '2.2.15.121011',
        poweradvantage_version: '2.4.14.121011', basemetals_version: '3.1.2.121011',
        basemetals_source_commit: 'b42b92b0e6f60ac5baf07a6b74042b2fa8427904',
        basemetals_deobf_sha256: '7C0B961A8D4991C9B69379518AAD700A71C1E4B2076CF5DEF292C58029408A07',
        orespawn_source_commit: '62c08169c71f4d4fd8f107289e63fd2a4c7c655c',
        orespawn_version: '4.1.0.121011', curseforge_project_id: '1783006',
        cf_requirements: 'basepower,base-metals'
    })) assert.equal(props[key], value, key);
    assert.deepEqual(json('src/main/resources/pack.mcmeta').pack, {pack_format:34, supported_formats:{min_inclusive:34,max_inclusive:48}, description:'baseElectric'});
    const mods = read('src/main/resources/META-INF/mods.toml').toString();
    assert.match(mods, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.14\.121011,3\.0\.0\)"/);
    assert.match(mods, /modId="basemetals"\s+mandatory=false\s+versionRange="\[3\.1\.2\.121011,4\.0\.0\)"/);
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

check('Shipped resources and fixtures retain released 1.20.6 bytes under singular registry folders', () => {
    const resources = tracked.filter(file => /^src\/(main|test)\/resources\//.test(file));
    const exceptions = new Set(['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml']);
    for (const file of resources.filter(file => !exceptions.has(file))) {
        assert.equal(sha256(read(relocated(file))), sha256(old(file)), file);
        if (relocated(file) !== file) assert(!fs.existsSync(path.join(root, file)), 'Obsolete registry path: ' + file);
    }
    const actual = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(entry => entry.isFile()).map(entry => path.relative(root, path.join(entry.parentPath, entry.name))
            .split(path.sep).join('/')).sort();
    assert.deepEqual(actual, [...tracked.map(relocated),
        javaRoot.replace('main', 'test') + 'resources/DatapackLayoutTest.java',
        javaRoot.replace('main', 'test') + 'verification/NativeMachineFixture.java',
        javaRoot.replace('main', 'test') + 'verification/NativeMachineFixtureTest.java'].sort(),
        'Source and resource inventory');
});

check('Migration and registration code keep their accepted IDs and compatibility paths', () => {
    for (const name of [
        'migration/ElectricLegacyMigrations.java', 'init/Blocks.java', 'init/Items.java',
        'init/Entities.java', 'init/GUI.java', 'init/OreSpawnWorldGen.java'
    ]) {
        const file = javaRoot + name;
        assert.equal(read(file).toString().replace(/\s/g, ''), nativeLocations(old(file)).replace(/\s/g, ''), file);
    }
});

check('All 102 released tests and their semantic assertions are retained', () => {
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
    assert.equal(inherited, 102);
    assert.equal(actual, 102);
    const gameTests = javaRoot.replace('main', 'gametest') + 'gametest/ElectricGameTests.java';
    const names = value => [...value.toString().matchAll(/public static void (\w+)\(GameTestHelper/g)].map(m => m[1]);
    for (const name of names(old(gameTests))) assert(names(read(gameTests)).includes(name), 'Lost GameTest: ' + name);
});


check('All 58 recipe IDs, results, guards and ingredients are unchanged', () => {
    const files = tracked.filter(file => /\/data\/baseelectric\/recipes\/.*\.json$/.test(file));
    assert.equal(files.length, 58);
    for (const file of files) {
        const expected = JSON.parse(old(file));
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
