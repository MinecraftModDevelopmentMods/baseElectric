const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = 'c5c8c9ea1d6f2010b8cff61e4bfae86f4c96c978';
const nativeBaseline = 'ce6b075f1d0e417e46e1742b9a2ca856b24dc67f';
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
const nativeFiles = git('ls-tree', '-r', '--name-only', nativeBaseline, 'src').toString().trim().split('\n');
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
    && !['"2.2.8.116051"', '"2.2.9.117011"', '"2.2.10.117011"'].includes(t)).sort();
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

check('Exact native 1.17.1 build target', () => {
    for (const [key, value] of Object.entries({minecraft_version:'1.17.1', forge_version:'37.1.1',
        mapping_channel:'official', mapping_version:'1.17.1', mcp_version:'20210706.113038',
        mod_id:'baseelectric', mod_name:'baseElectric', mod_version:'2.2.10.117011',
        mod_group:'zone.moddev.mc.baseelectric', curseforge_project_id:'1783006', cf_requirements:'basepower,base-metals',
        java_version:'16', java_toolchain_version:'16.0.2+7', gradle_java_version:'17',
        poweradvantage_version:'2.4.9.117011', basemetals_version:'3.1.1.117011', orespawn_version:'4.1.0.117011'})) {
        assert.equal(properties[key], value, key);
    }
    assert.deepEqual(JSON.parse(read('src/main/resources/pack.mcmeta')).pack, {pack_format:7, description:'baseElectric'});
    assert(read('build.gradle').includes('major != 60'));
    const metadata = read('src/main/resources/META-INF/mods.toml');
    assert.match(metadata, /modId="basepower"\s+mandatory=true\s+versionRange="\[2\.4\.9\.117011,3\.0\.0\)"/);
    assert.match(metadata, /modId="basemetals"\s+mandatory=false\s+versionRange="\[3\.1\.1\.117011,4\.0\.0\)"/);
    assert(metadata.includes('modId="baseelectric"') && metadata.includes('displayName="baseElectric"'));
});
check('Exact local basePower, Base Metals and OreSpawn pins are retained', () => {
    assert.equal(properties.poweradvantage_source_repository, 'MinecraftModDevelopmentMods/basePower');
    assert.equal(properties.poweradvantage_pin_state, 'LOCAL_CHECKPOINT');
    const build = read('build.gradle');
    assert.match(build, /poweradvantage_pin_state != 'LOCAL_CHECKPOINT'/);
    assert.match(build, /requireDevelopmentPins\(\)\s+def powerAdvantageDeobfJarPath/);
    assert(!build.includes('defaultPowerAdvantageDeobfJar'));
    for (const [key, value] of Object.entries({
        poweradvantage_source_ref:'bb7023835fd41aefe89c92275caa74da9e126cc7',
        poweradvantage_deobf_sha256:'AFC3FBDF84FF7E8FF8929F8192FDBCEC170C9AE1CF1B248E1C4B9C6ED17AA357',
        poweradvantage_runtime_sha256:'A788C08163CECE1C6D33FBDC295BE6EC272DE9AF7911BE74E707D0EE9121DF73',
        poweradvantage_unit_bootstrap_sha256:'28080BD563D6D2F378AD2735BBEFEBB079DBFC87A9457E066DDB99F43C3D4095',
        basemetals_source_commit:'65f9956c07d5d0c0b6ec0ac2108ce69c3ce52c6e',
        basemetals_deobf_sha256:'83A8C32C633B56BB7E4405E8B3B24A4DEA32B9119216E5097B884056D679CAD1',
        orespawn_source_commit:'b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf',
        orespawn_deobf_sha256:'CB7A736314842B110B2E4788FBD5E21D4AE8803C5ED34A49199B034BE572DA39',
        orespawn_runtime_sha256:'1BDEDCBB179CB5A3B169E5C8A6B301E3888AE15B94108B0AEC627D705CFAD833'})) {
        assert.equal(properties[key], value, key);
    }
});
check('Resources retained except namespace moves, canonical LF and exact native advancement conversion', () => {
    let changedFiles = 0, changedPredicates = 0;
    for (const name of tracked.filter(n => n.startsWith('src/main/resources/')
        && !['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml'].includes(n))) {
        const before = git('show', `${baseline}:${name}`);
        const target = migratePath(name);
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
        const expected = numbers(git('show', `${baseline}:${name}`).toString());
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
            'resourcePackDeclaresTheNativeOneSeventeenFormat'));
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
check('Native 1.17 source, tests and binary assets are preserved beyond the identity changes', () => {
    const actualFiles = fs.readdirSync(path.join(root, 'src'), {recursive:true, withFileTypes:true})
        .filter(file => file.isFile()).map(file => path.relative(root, path.join(file.parentPath, file.name))
            .split(path.sep).join('/')).sort();
    assert.deepEqual(actualFiles, nativeFiles.map(migratePath).sort(), 'Source path inventory');
    for (const name of nativeFiles) {
        const target = migratePath(name);
        if ([entry, migration, 'src/main/resources/META-INF/mods.toml', 'src/main/resources/pack.mcmeta'].includes(target)
                || /\/(ElectricLegacyMigrationsTest|BuildWorkflowContractTest)\.java$/.test(target)) continue;
        const before = git('show', `${nativeBaseline}:${name}`);
        const expected = !name.startsWith('src/test/resources/') && /\.(java|json|fsh|vsh|mcmeta)$/.test(name)
            ? Buffer.from(migrateText(before.toString())) : before;
        assert.deepEqual(fs.readFileSync(path.join(root, target)), expected, target);
    }
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
const testFiles = nativeFiles.map(migratePath).filter(n => n.startsWith('src/test/java/')
    && n.endsWith('.java') && fs.existsSync(path.join(root, n)));
const tests = testFiles.reduce((n, file) => n + (read(file).match(/@Test\b/g) || []).length, 0);
check('The complete native 1.17.1 test inventory is retained', () => {
    assert.equal(testFiles.length, 27);
    assert.equal(tests, 96);
});
const report = {baseline, nativeBaseline, scope:'Static source/resource contracts only; not compilation, runtime or manual acceptance',
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
