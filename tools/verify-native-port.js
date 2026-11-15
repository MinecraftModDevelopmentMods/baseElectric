const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const baseline = 'b81645579c3330476b84149402c3ce78183df85f';
const git = (...args) => execFileSync('git', args, { cwd: root, maxBuffer: 16 * 1024 * 1024 });
const read = file => fs.readFileSync(path.join(root, file));
const files = git('ls-tree', '-r', '--name-only', baseline, 'src').toString().trim().split('\n');
const old = file => git('show', `${baseline}:${file}`);
const checks = [];
function check(name, run) {
    try { run(); checks.push({ name, pass: true }); }
    catch (error) { checks.push({ name, pass: false, error: error.message }); }
}
function neo(value) {
    if (Array.isArray(value)) return value.map(neo);
    if (value && typeof value === 'object') {
        const result = {};
        for (const [key, entry] of Object.entries(value)) {
            if (key === 'forge:condition') result['neoforge:conditions'] = [neo(entry)];
            else result[key] = neo(entry);
        }
        return result;
    }
    if (typeof value !== 'string') return value;
    if (value === 'basemetals:crushing') return 'mmdlib:crushing';
    if (/^forge:(and|or|not|mod_loaded|tag_empty|true|false)$/.test(value)) return value.replace('forge:', 'neoforge:');
    return value.replace(/^forge:/, 'c:').replace(/^#forge:/, '#c:');
}

check('Native NeoForge build and candidate identity', () => {
    const build = read('build.gradle').toString();
    assert.match(build, /id 'net.neoforged.moddev' version '2.0.148'/);
    assert.doesNotMatch(build, /net\.minecraftforge\.gradle|net\.neoforged\.gradle/);
    const props = read('gradle.properties').toString();
    for (const pin of ['neo_version=21.1.265', 'mod_version=2.2.20.121012',
        'poweradvantage_version=2.4.19.121012', 'java_version=21']) assert(props.includes(pin), pin);
    assert(!fs.existsSync(path.join(root, 'src/main/resources/META-INF/mods.toml')));
    assert(fs.existsSync(path.join(root, 'src/main/resources/META-INF/neoforge.mods.toml')));
});
check('Release campaign uses published Base Metals and exact dependency pins', () => {
    const props = read('gradle.properties').toString();
    for (const pin of ['neo_version_range=[21.1.265,21.2)',
        'poweradvantage_pin_state=MERGED_SOURCE',
        'poweradvantage_source_ref=c5550ce133ce0f5d3fe3e5227f021b390cdc7953',
        'poweradvantage_source_tag=2.4.19.121012',
        'release_player_sha256=BC866F83F55FAD289EC36808D33A71B2602B8062EB71FC2E80C9CE3B0495F6D8',
        'cf_requirements=basepower,base-metals,mmdlib',
        'poweradvantage_sha256=E9ED662E15C3510E2A48CE85448D3D39C6EF2667208F29EB281791200507F77E',
        'poweradvantage_runtime_sha256=E9ED662E15C3510E2A48CE85448D3D39C6EF2667208F29EB281791200507F77E',
        'mmdlib_version=2.0.2.121012+Neo',
        'mmdlib_sha256=EE417C82D588B0050A49709B6430CF836315E71E4DB2DF4036D3735454BC5311',
        'basemetals_version=4.0.1.121012+Neo',
        'basemetals_sha256=B4BB1CBCD54DFCF2EC8465ACE82715EDCCC5BF1E5C15E14C627AE7CC58E8FED1',
        'basemetals_source_commit=39b240bb0d9f80997847ab4df70a2dd23e6ea826']) {
        assert(props.split(/\r?\n/).includes(pin), pin);
    }
    const build = read('build.gradle').toString();
    assert(build.includes('runtimeOnly "zone.moddev.mc.basemetals:BaseMetals:${project.basemetals_version}"'));
    assert(build.includes("project.neo_version != '21.1.265'"));
    assert.doesNotMatch(props + build, /basemetals_local_jar|hook-candidate-a/);
    const inputs = read('gradle/verify-native-inputs.gradle').toString();
    assert(inputs.includes('("BaseMetals-${project.basemetals_version}.jar"): project.basemetals_sha256'));
    const metadata = read('src/main/resources/META-INF/neoforge.mods.toml').toString();
    assert(metadata.includes('versionRange="[2.0.2.121012,3.0.0)"'));
    assert(metadata.includes('versionRange="[4.0.1.121012,5.0.0)"'));
    const gameTests = read('src/gametest/java/zone/moddev/mc/baseelectric/gametest/ElectricGameTests.java').toString();
    assert(gameTests.includes('"basemetals", "4.0.1.121012+Neo"'));
    assert(gameTests.includes('"mmdlib", "2.0.2.121012+Neo"'));
    assert(gameTests.includes('"neoforge", "21.1.265"'));
    const ecj = read('gradle/verify-eclipse-ecj.gradle').toString();
    assert(ecj.includes("'neoforge-' + project.neo_version + '.jar'"));
    assert(ecj.includes('containers.size() != jres.size() + buildship.size() || buildship.size() > 1'));
});
check('All released visual assets are byte-identical', () => {
    for (const file of files.filter(file => file.startsWith('src/main/resources/assets/'))) {
        assert(read(file).equals(old(file)), file);
    }
});
check('All 58 recipe identities, quantities and guards survive the loader conversion', () => {
    const recipes = files.filter(file => /data\/baseelectric\/recipe\/.*\.json$/.test(file));
    assert.equal(recipes.length, 58);
    let crushing = 0;
    for (const file of recipes) {
        const expected = neo(JSON.parse(old(file)));
        if (expected.type === 'mmdlib:crushing') {
            expected.result.item = expected.result.id;
            delete expected.result.id;
            crushing++;
        }
        assert.deepEqual(JSON.parse(read(file)), expected, file);
    }
    assert.equal(crushing, 8);
});
check('Common tag contents retain released members under the c namespace', () => {
    for (const file of files.filter(file => file.startsWith('src/main/resources/data/forge/'))) {
        const target = file.replace('/data/forge/', '/data/c/');
        assert.deepEqual(JSON.parse(read(target)), neo(JSON.parse(old(file))), target);
        assert(!fs.existsSync(path.join(root, file)), file);
    }
});
check('Legacy registry, NBT and normal drill mining contracts are retained', () => {
    const java = 'src/main/java/zone/moddev/mc/baseelectric/';
    for (const file of ['migration/ElectricLegacyMigrations.java', 'init/GUI.java']) {
        assert.equal(read(java + file).toString(), old(java + file).toString(), file);
    }
    const drill = java + 'machines/ElectricDrillTileEntity.java';
    assert.equal(read(drill).toString().replaceAll('net.neoforged.api.distmarker', 'net.minecraftforge.api.distmarker'),
        old(drill).toString());
    assert(read(drill).toString().includes('Block.getDrops('));
    assert.doesNotMatch(read(drill).toString(), /CrushingRecipes|BaseMetalsCompat/);
});
check('Crusher delegates exclusively to shared reloadable recipes', () => {
    const bridge = read('src/main/java/zone/moddev/mc/baseelectric/compat/BaseMetalsCompat.java').toString();
    assert.equal((bridge.match(/return CrushingRecipes\.find\(world, input\);/g) || []).length, 2);
    assert.doesNotMatch(bridge, /getVanillaCrusherOutput|basemetals\.recipe|minecraft:cobblestone/);
});

const report = { baseline, scope: 'Static source and resource contracts, not gameplay acceptance', checks };
const flag = process.argv.indexOf('--report');
if (flag >= 0) {
    const output = path.resolve(root, process.argv[flag + 1]);
    assert(output.startsWith(path.join(root, 'build', 'port-evidence') + path.sep));
    fs.mkdirSync(path.dirname(output), { recursive: true });
    fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n', { flag: 'wx' });
}
for (const result of checks) console.log(`${result.pass ? 'PASS' : 'FAIL'} ${result.name}${result.error ? ': ' + result.error : ''}`);
process.exitCode = checks.every(result => result.pass) ? 0 : 1;
