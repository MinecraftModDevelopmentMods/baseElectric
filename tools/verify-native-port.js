const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = 'c5c8c9ea1d6f2010b8cff61e4bfae86f4c96c978';
const nativeBaseline = 'ce6b075f1d0e417e46e1742b9a2ca856b24dc67f';
const portBase = 'cf3d4b0e2d1591b2ae8e5d638002ca016e46eac4';
const oldJavaRoot = 'src/main/java/zone/moddev/mc/electricadvantage/';
const javaRoot = 'src/main/java/zone/moddev/mc/baseelectric/';
const migratePath = name => name.startsWith('src/test/resources/')
    ? name : name.replaceAll('/electricadvantage/', '/baseelectric/');
// Some historical blobs predate the checkout's canonical LF policy.
const migrateText = source => source.replaceAll('\r\n', '\n').replaceAll('electricadvantage', 'baseelectric')
    .replaceAll('zone.moddev.mc.poweradvantage', 'zone.moddev.mc.basepower')
    .replaceAll('poweradvantage:', 'basepower:');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const git = (...args) => execFileSync('git', args, { cwd: root });
const tracked = git('ls-tree', '-r', '--name-only', baseline).toString().trim().split('\n');
const portBaseFiles = git('ls-tree', '-r', '--name-only', portBase, 'src').toString().trim().split('\n');
const sha256 = value => createHash('sha256').update(value).digest('hex');
const results = [];
const properties = Object.fromEntries(read('gradle.properties').split(/\r?\n/)
    .filter(line => line && !line.startsWith('#')).map(line => {
        const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)];
    }));
function check(name, run) {
    try { run(); results.push({ name, passed: true }); }
    catch (error) { results.push({ name, passed: false, error: error.message }); }
}
function tokens(source) {
    return source.match(/"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\/[^\n]*|\/\*[\s\S]*?\*\/|[A-Za-z_$][A-Za-z0-9_$]*|(?:0[xX][\da-fA-F_]+|(?:\d[\d_]*\.?[\d_]*|\.\d[\d_]*)(?:[eE][+-]?[\d_]+)?)[dDfFlL]?/g) || [];
}
const strings = source => tokens(source).filter(t => t.startsWith('"')
    && !['"2.2.8.116051"', '"2.2.9.117011"', '"2.2.10.117011"', '"2.2.11.118021"'].includes(t)).sort();
const numbers = source => tokens(source).filter(t => /^\d|^\.\d/.test(t));
const production = tracked.filter(n => n.startsWith(oldJavaRoot) && n.endsWith('.java'));
const entry = javaRoot + 'ElectricAdvantage.java';
const migration = javaRoot + 'migration/ElectricLegacyMigrations.java';

function nativeUnlockFromLegacy(source) {
    const document = JSON.parse(source);
    let converted = 0;
    for (const criterion of Object.values(document.criteria || {})) {
        if (criterion.trigger !== 'minecraft:inventory_changed') continue;
        for (const predicate of criterion.conditions?.items || []) {
            if (!Object.hasOwn(predicate, 'item')) continue;
            assert.deepEqual(Object.keys(predicate), ['item']);
            assert.equal(typeof predicate.item, 'string');
            const id = predicate.item;
            delete predicate.item;
            predicate.items = [id];
            converted++;
        }
    }
    return {document, converted};
}

function assertNativeUnlock(source, actual) {
    const expected = nativeUnlockFromLegacy(source);
    assert.deepEqual(JSON.parse(actual), expected.document);
    return expected.converted;
}

check('Exact native 1.18.2 build target', () => {
    for (const [key, value] of Object.entries({minecraft_version:'1.18.2', forge_version:'40.3.0',
        mapping_channel:'official', mapping_version:'1.18.2', mcp_version:'20220404.173914',
        mod_id:'baseelectric', mod_name:'baseElectric', mod_version:'2.2.11.118021',
        mod_group:'zone.moddev.mc.baseelectric', curseforge_project_id:'1783006', cf_requirements:'basepower,base-metals',
        java_version:'17', java_toolchain_version:'17.0.1+12', gradle_java_version:'17',
        poweradvantage_version:'2.4.10.118021', basemetals_version:'3.1.2.118021', orespawn_version:'4.1.0.118021'})) {
        assert.equal(properties[key], value, key);
    }
    assert.deepEqual(JSON.parse(read('src/main/resources/pack.mcmeta')).pack,
        {pack_format:9, 'forge:resource_pack_format':8, 'forge:data_pack_format':9, description:'baseElectric'});
    assert(read('build.gradle').includes('major != 61'));
    const metadata = read('src/main/resources/META-INF/mods.toml');
    assert.match(metadata, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.10\.118021,3\.0\.0\)"/);
    assert.match(metadata, /modId="basemetals"\s+mandatory=false\s+versionRange="\[3\.1\.1\.118021,4\.0\.0\)"/);
    assert(metadata.includes('modId="baseelectric"') && metadata.includes('displayName="baseElectric"'));
});
check('Exact local basePower, Base Metals and OreSpawn pins are retained', () => {
    assert.equal(properties.poweradvantage_source_repository, 'MinecraftModDevelopmentMods/basePower');
    assert.equal(properties.poweradvantage_pin_state, 'LOCAL_CHECKPOINT');
    const build = read('build.gradle');
    assert.match(build, /poweradvantage_pin_state != 'LOCAL_CHECKPOINT'/);
    assert.match(build, /requirePowerPin\(\)\s+pinnedFile\('powerAdvantageDeobfJar', project.poweradvantage_deobf_sha256\)/);
    assert(!build.includes('PA117') && !build.includes('117011-deobf.jar'));
    for (const [key, value] of Object.entries({
        poweradvantage_source_ref:'bab9971cc2d593eee1f4a3de6dafc7ec268d25c2',
        poweradvantage_deobf_sha256:'C00F835A7A80BAF332355EF1F181F252B603A017A979D2F41EA3BBEAE314E280',
        poweradvantage_runtime_sha256:'595991E8BB6B831C4F3A291251F283CD5FB1EC1F9BC48C1BB54BC5D88490ECB9',
        poweradvantage_unit_bootstrap_sha256:'025F16FCA9FD5CFC585931E7C8BCFAB22FC1DF933BE7DAAD490B8E4FC824B76E',
        basemetals_source_commit:'3488ed58403722ac3ff203cff7680dfc16e0fd3f',
        basemetals_deobf_sha256:'9371C976B890BF875FCD3A845B0E9FCF3DAF16F2FB76DADF5386A7BA28C12BAE',
        basemetals_runtime_sha256:'9EA9DE0E5D548FD77BBAB05565D3C2004E1C2C985353C626F6610CDA07EE3BBF',
        orespawn_source_commit:'d31f52f296739f34a0c13f554abf3e1ac071ae25',
        orespawn_deobf_sha256:'34B25B2116FD91B0050C80393F8356A5CA256E707BF443E4882BE5CFAD1335B6',
        orespawn_runtime_sha256:'BD8D84A96DAFB7668E95D61B9D72C62AEF3440675473E41244C182C64653356F',
        forge_installer_sha256:'9434C29790504DCD11CE97CB30DD8891B4B18848982357BC1B5ABFA79FB95103'})) {
        assert.equal(properties[key], value, key);
    }
});
check('Resources retained except namespace moves, exact advancement conversion and the native fog shader', () => {
    let changedFiles = 0, changedPredicates = 0;
    for (const name of tracked.filter(n => n.startsWith('src/main/resources/')
        && !['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml'].includes(n))) {
        const before = git('show', `${baseline}:${name}`);
        const target = migratePath(name);
        if (/\/shaders\/core\/unshaded\.(vsh|json)$/.test(target)) continue; // Exact 1.18 delta audited below.
        const expected = /\.(json|fsh|vsh|mcmeta)$/.test(name)
            ? Buffer.from(migrateText(before.toString())) : before;
        if (name.startsWith('src/main/resources/data/electricadvantage/advancements/recipes/')) {
            const converted = nativeUnlockFromLegacy(expected.toString()).converted;
            if (converted) {
                assert.equal(assertNativeUnlock(expected.toString(), read(target)), converted, target);
                changedFiles++;
                changedPredicates += converted;
                continue;
            }
        }
        assert.deepEqual(fs.readFileSync(path.join(root, target)), expected, target);
    }
    assert.equal(changedFiles, 19);
    assert.equal(changedPredicates, 21);
});
check('Advancement compatibility allowance rejects unrelated changes and old triggers', () => {
    const before = JSON.stringify({criteria:{has_input:{trigger:'minecraft:inventory_changed',
        conditions:{items:[{item:'minecraft:apple'}]}}}, rewards:{recipes:['electricadvantage:example']},
        requirements:[['has_input']]});
    const native = nativeUnlockFromLegacy(before).document;
    assert.equal(assertNativeUnlock(before, JSON.stringify(native)), 1);
    assert.throws(() => assertNativeUnlock(before, before));
    for (const mutate of [
        document => document.criteria.has_input.conditions.items[0].items.push('minecraft:dirt'),
        document => document.rewards.recipes.push('electricadvantage:another'),
        document => document.requirements.push(['has_input'])
    ]) {
        const altered = JSON.parse(JSON.stringify(native));
        mutate(altered);
        assert.throws(() => assertNativeUnlock(before, JSON.stringify(altered)));
    }
});
check('Registry, asset, provider and NBT string identities retained', () => {
    for (const name of production) {
        const target = migratePath(name);
        if (target === entry || target === migration) continue; // Checked against the native baseline below.
        const actual = strings(read(target));
        if (name.endsWith('/ElectricRenderTypes.java')) {
            assert.equal(actual.splice(actual.indexOf('"unshaded"'), 1)[0], '"unshaded"');
        }
        assert.deepEqual(actual, strings(migrateText(git('show', `${baseline}:${name}`).toString())), target);
    }
});
check('Machine balance and renderer test tolerances retained', () => {
    const renderer = 'src/test/java/zone/moddev/mc/electricadvantage/graphics/RendererBufferContractTest.java';
    for (const name of [...production, renderer]) {
        let before = git('show', `${baseline}:${name}`).toString();
        if (name.endsWith('/LaserTurretTileEntity.java')) {
            assert(before.includes('this.getBlockPos(), 0, nbtTag'));
            before = before.replace('this.getBlockPos(), 0, nbtTag', 'this.getBlockPos(), nbtTag');
        }
        const expected = numbers(before);
        if (name.endsWith('/ElectricRenderTypes.java')) {
            assert.equal(expected.splice(expected.indexOf('7'), 1)[0], '7'); // Native enum replaces GL_QUADS.
        }
        assert.deepEqual(numbers(read(migratePath(name))), expected, name);
    }
});
check('Inherited regression tests remain present with no skip or assume bypass', () => {
    let count = 0;
    for (const name of tracked.filter(n => n.startsWith('src/test/') && n.endsWith('.java'))) {
        const before = git('show', `${baseline}:${name}`).toString();
        const after = read(migratePath(name));
        const methods = s => [...s.matchAll(/@Test\s+public void (\w+)\(/g)].map(m => m[1]);
        const expected = methods(before).map(n => n.replace('resourcePackDeclaresTheNativeOneSixteenFormat',
            'resourcePackDeclaresTheNativeOneEighteenFormats')
            .replace('publishedOneSeventeenRecipesKeepRawAndOreOutputsDistinct', 'nativeCrushingRecipesKeepRawAndOreOutputsDistinct'));
        count += expected.length;
        if (name.endsWith('/RecipeUnlockContractTest.java')) {
            expected.splice(2, 0, 'nativeParserRestrictsItemArraysButSilentlyIgnoresTheLegacySingularKey');
        }
        assert.deepEqual(methods(after), expected, name);
        assert(!/@Ignore|Assume\.|assumeTrue\(|assumeFalse\(/.test(after), name);
    }
    assert.equal(count, 89);
});
check('Native constructors, factories and tag-only persistence', () => {
    const directory = path.join(root, javaRoot, 'machines');
    for (const name of fs.readdirSync(directory).filter(n => n.endsWith('TileEntity.java'))) {
        const source = fs.readFileSync(path.join(directory, name), 'utf8');
        assert(!/public \w+TileEntity\(\s*\)/.test(source), name);
        assert(!/void load\(BlockState|super\.load\(state,|setPosition\(/.test(source), name);
        assert.match(source, /super\([^;]*pos, state,/, name);
    }
    const entities = read(javaRoot + 'init/Entities.java');
    assert(entities.includes('BlockEntityType.BlockEntitySupplier<T>'));
    assert(entities.includes('.setTrackingRange(64).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true)'));
    const move = read(javaRoot + 'machines/ElectricDrillTileEntity.java');
    assert(move.includes('te.load(dataTransfer)'));
    assert(move.includes('te.subtractEnergy(ENERGY_COST_MOVE, getEnergyType())'));
    assert(read(javaRoot + 'machines/LaserTurretTileEntity.java').includes('handleUpdateTag(CompoundTag tag)'));
});
check('Native helper ticker, bucket pickup, renderer context and shader', () => {
    assert(read(javaRoot + 'machines/LEDBlock.java').includes('BlockEntityTicker<T> getTicker'));
    assert(read(javaRoot + 'machines/ElectricPumpTileEntity.java').includes('.pickupBlock(w, fluidSource, blockstate)'));
    for (const name of ['LaserDrillRenderer', 'LaserTurretRenderer']) {
        const source = read(javaRoot + `graphics/${name}.java`);
        assert(source.includes('implements BlockEntityRenderer<'));
        assert(source.includes('BlockEntityRendererProvider.Context'));
    }
    const render = read(javaRoot + 'graphics/ElectricRenderTypes.java');
    assert(render.includes('DefaultVertexFormat.NEW_ENTITY'));
    assert(render.includes('VertexFormat.Mode.QUADS'));
    assert(render.includes('new ShaderStateShard(() -> unshadedShader)'));
    const shader = read('src/main/resources/assets/baseelectric/shaders/core/unshaded.vsh');
    assert(shader.includes('vertexColor = Color;'));
    assert(shader.includes('lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);'));
    assert(shader.includes('overlayColor = texelFetch(Sampler1, UV1, 0);'));
    const registration = read(javaRoot + 'graphics/ClientRegistration.java');
    assert(registration.includes('EntityRenderersEvent.RegisterRenderers'));
});
check('Native harvest tags preserve lithium and sulfur tool tiers', () => {
    const tag = name => JSON.parse(read(`src/main/resources/data/minecraft/tags/blocks/${name}.json`));
    assert.deepEqual(tag('mineable/pickaxe'), {replace:false, values:['baseelectric:li_ore','baseelectric:sulfur_ore']});
    assert.deepEqual(tag('needs_stone_tool'), {replace:false, values:['baseelectric:li_ore']});
    assert.equal((read(javaRoot + 'blocks/Ore.java').match(/requiresCorrectToolForDrops\(\)/g) || []).length, 2);
});
check('Shared capabilities, legacy cyano API and first-tick interlocks retained', () => {
    assert(read(javaRoot + 'util/LegacyFluidHandler.java').includes('extends IAdvantageFluidHandler'));
    for (const name of ['ElectricMachineTileEntity', 'ElectricGeneratorTileEntity']) {
        const source = read(javaRoot + `machines/${name}.java`);
        assert(source.includes('!redstoneInitialized && getLevel() != null && !getLevel().isClientSide'));
    }
    assert(!production.some(n => /invalidateCaps\(|reviveCaps\(|LazyOptional/.test(read(migratePath(n)))));
    assert(read(javaRoot + 'ElectricAdvantage.java').includes('event.enqueueWork(RecipeConditions::register)'));
    assert(read(javaRoot + 'machines/PhotovoltaicGeneratorTileEntity.java').includes('BlockPos lightPos = pos.above()'));
});
check('Only the reviewed 1.18.2 source, shader, metadata and fixture deltas are present', () => {
    const actualFiles = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(file => file.isFile()).map(file => path.relative(root, path.join(file.parentPath, file.name))
            .split(path.sep).join('/')).sort();
    const expectedFiles = portBaseFiles.map(name => name.replace('/Native117ConstructionTest.java', '/Native118ConstructionTest.java'));
    expectedFiles.push('src/test/java/zone/moddev/mc/baseelectric/verification/NativeTestBootstrap.java');
    assert.deepEqual(actualFiles, expectedFiles.sort(), 'Source path inventory');
    const sourceChanges = [...new Set([
        ...git('diff', '--no-renames', '--name-only', portBase, '--', 'src').toString().trim().split('\n'),
        ...git('ls-files', '--others', '--exclude-standard', '--', 'src').toString().trim().split('\n')
    ])].filter(Boolean).sort();
    const fingerprints = sourceChanges.map(file => [file, fs.existsSync(path.join(root, file))
        ? sha256(fs.readFileSync(path.join(root, file))) : null]);
    assert.equal(sha256(JSON.stringify(fingerprints)), 'c684a77312129f3b90b367b6f0bf9b6a81402992e8bb2440b5795e99f9c60898',
        'Exact reviewed source delta from the PA117 clone point');
});
check('Original and baseElectric aliases retain the native registry paths and state decoding', () => {
    const old = git('show', `${nativeBaseline}:${oldJavaRoot}migration/ElectricLegacyMigrations.java`).toString();
    const source = read(migration);
    const ids = text => [...text.matchAll(/for \(String path : new String\[\] \{([^}]+)\}/g)]
        .map(match => [...match[1].matchAll(/"([^"]+)"/g)].map(id => id[1]));
    assert.equal(ids(old).length, 2);
    assert.deepEqual(ids(source), ids(old));
    const decoder = '    private static BlockState decodeState';
    assert.equal(source.slice(source.indexOf(decoder)), old.slice(old.indexOf(decoder)));
    assert(source.includes('LegacyMigrationApi.registerNamespaceAlias(ElectricAdvantage.LEGACY_MODID, ElectricAdvantage.MODID)'));
    assert(source.includes('LegacyMigrationApi.registerTileEntityAliases(id(path))'));
    assert(source.includes('new String[] {ElectricAdvantage.LEGACY_MODID, ElectricAdvantage.MODID}'));
    for (const alias of ['minecraft:electricadvantage.Hydroturbine', 'electricadvantage.electricadvantage.Hydroturbine']) {
        assert(source.includes(`registerEntityAlias("${alias}", hydroturbine)`), alias);
    }
    const main = read(entry);
    assert(main.includes('LEGACY_MODID = "electricadvantage"'));
    for (const bridge of ['LegacyConfigMigration', 'LegacyOreSpawnConfigBridge']) {
        assert(main.includes(`${bridge}.migrate(FMLPaths.CONFIGDIR.get(), MODID, LEGACY_MODID`));
    }
    for (const name of ['Blocks', 'Items']) {
        const registration = read(javaRoot + `init/${name}.java`);
        assert(registration.includes('import zone.moddev.mc.basepower.init.ItemGroups;'));
        assert(registration.includes('tab(ItemGroups.tab_powerAdvantage)'));
    }
});

const changedFiles = [...new Set([
    ...git('diff', '--name-only', baseline).toString().trim().split('\n'),
    ...git('ls-files', '--others', '--exclude-standard').toString().trim().split('\n')
])].filter(Boolean).sort();
const testFiles = fs.readdirSync(path.join(root, 'src/test/java'), {recursive:true, withFileTypes:true})
    .filter(file => file.isFile() && file.name.endsWith('.java'))
    .map(file => path.relative(root, path.join(file.parentPath, file.name)));
const tests = testFiles.reduce((n, file) => n + (read(file).match(/@Test\b/g) || []).length, 0);
check('All 27 inherited suites and 96 tests remain present', () => {
    assert.equal(testFiles.length, 28);
    assert.equal(testFiles.filter(file => /@Test\b/.test(read(file))).length, 27);
    assert.equal(tests, 96);
});
const report = {baseline, nativeBaseline, portBase, scope:'Static source/resource contracts only; not compilation, runtime or manual acceptance',
    inventory: {productionFiles:production.length, testFiles:testFiles.length, tests},
    changedFiles:changedFiles.map(file => ({file, sha256:fs.existsSync(path.join(root, file))
        ? createHash('sha256').update(fs.readFileSync(path.join(root, file))).digest('hex') : null})),
    results};
const reportFlag = process.argv.indexOf('--report');
if (reportFlag !== -1) {
    const output = path.resolve(root, process.argv[reportFlag + 1]);
    assert(output.startsWith(root + path.sep), 'Report must remain inside this checkout');
    assert(!fs.existsSync(output), 'Refusing to overwrite source evidence');
    fs.mkdirSync(path.dirname(output), {recursive:true});
    fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n');
}
for (const r of results) console.log(`${r.passed ? 'PASS' : 'FAIL'} ${r.name}${r.error ? ': ' + r.error : ''}`);
process.exitCode = results.every(r => r.passed) ? 0 : 1;
