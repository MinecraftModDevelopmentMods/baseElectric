const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { createHash } = require('node:crypto');

const root = path.resolve(__dirname, '..');
const baseline = 'c5c8c9ea1d6f2010b8cff61e4bfae86f4c96c978';
const javaRoot = 'src/main/java/zone/moddev/mc/electricadvantage/';
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const git = (...args) => execFileSync('git', args, { cwd: root });
const tracked = git('ls-tree', '-r', '--name-only', baseline).toString().trim().split('\n');
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
    && !['"2.2.8.116051"', '"2.2.9.117011"'].includes(t)).sort();
const numbers = source => tokens(source).filter(t => /^\d|^\.\d/.test(t));
const production = tracked.filter(n => n.startsWith(javaRoot) && n.endsWith('.java'));

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
        mod_version:'2.2.9.117011', mod_group:'zone.moddev.mc.electricadvantage',
        java_version:'16', java_toolchain_version:'16.0.2+7', gradle_java_version:'17',
        basemetals_version:'3.1.0.117011', orespawn_version:'4.1.0.117011'})) {
        assert.equal(properties[key], value, key);
    }
    assert.equal(JSON.parse(read('src/main/resources/pack.mcmeta')).pack.pack_format, 7);
    assert(read('build.gradle').includes('major != 60'));
});
check('Approved 1.17 API source and artifact pins', () => {
    assert.equal(properties.poweradvantage_deobf_sha256, '799B59B06EB2AC3FC4682AA64BF42C3A2A2C1D89D7FFFE9C72745DEC7DFBE0D8');
    assert.equal(properties.poweradvantage_source_ref, '5b9abe578b053d2ef59b1962cf9e04f6880c91b9');
    assert.notEqual(properties.poweradvantage_pin_state, 'API_PIN_PROVISIONAL', 'Power API remains provisional; source freeze pending');
    assert.match(properties.poweradvantage_source_ref, /^[a-f0-9]{40}$/, 'Power source pin pending parent');
    assert.match(properties.poweradvantage_deobf_sha256, /^[A-F0-9]{64}$/, 'Power development jar pin pending parent');
    assert.match(properties.poweradvantage_version, /^\d+\.\d+\.\d+\.117011$/);
    for (const [key, value] of Object.entries({
        basemetals_source_commit:'dacfaa3af2e3310d5cb6b081da79f09260ae103d',
        basemetals_deobf_sha256:'4EB49B12ED17211296F5FEC67FC4307174702BE096D3F1C02DD67B45AED587FA',
        basemetals_runtime_sha256:'9276FA7E8A1E5BD6B9E336CF83CFEE9B178167DA09CE2B231C8DFF733F351B0C',
        orespawn_source_commit:'b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf',
        orespawn_deobf_sha256:'CB7A736314842B110B2E4788FBD5E21D4AE8803C5ED34A49199B034BE572DA39',
        orespawn_runtime_sha256:'1BDEDCBB179CB5A3B169E5C8A6B301E3888AE15B94108B0AEC627D705CFAD833'})) {
        assert.equal(properties[key], value, key);
    }
});
check('Resource bytes retained except exact native advancement predicate conversion', () => {
    let changedFiles = 0, changedPredicates = 0;
    for (const name of tracked.filter(n => n.startsWith('src/main/resources/')
        && !['src/main/resources/pack.mcmeta', 'src/main/resources/META-INF/mods.toml'].includes(n))) {
        const before = git('show', `${baseline}:${name}`);
        if (name.startsWith('src/main/resources/data/electricadvantage/advancements/recipes/')) {
            const converted = nativeUnlockFromLegacy(before.toString()).converted;
            if (converted) {
                assert.equal(assertNativeUnlock(before.toString(), read(name)), converted, name);
                changedFiles++;
                changedPredicates += converted;
                continue;
            }
        }
        assert.deepEqual(fs.readFileSync(path.join(root, name)), before, name);
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
        const actual = strings(read(name));
        if (name.endsWith('/ElectricRenderTypes.java')) {
            assert.equal(actual.splice(actual.indexOf('"unshaded"'), 1)[0], '"unshaded"');
        }
        assert.deepEqual(actual, strings(git('show', `${baseline}:${name}`).toString()), name);
    }
});
check('Machine balance and renderer test tolerances retained', () => {
    const renderer = 'src/test/java/zone/moddev/mc/electricadvantage/graphics/RendererBufferContractTest.java';
    for (const name of [...production, renderer]) {
        const expected = numbers(git('show', `${baseline}:${name}`).toString());
        if (name.endsWith('/ElectricRenderTypes.java')) {
            assert.equal(expected.splice(expected.indexOf('7'), 1)[0], '7'); // Native enum replaces GL_QUADS.
        }
        assert.deepEqual(numbers(read(name)), expected, name);
    }
});
check('Inherited regression tests remain present with no skip or assume bypass', () => {
    let count = 0;
    for (const name of tracked.filter(n => n.startsWith('src/test/') && n.endsWith('.java'))) {
        const before = git('show', `${baseline}:${name}`).toString();
        const after = read(name);
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
    const shader = read('src/main/resources/assets/electricadvantage/shaders/core/unshaded.vsh');
    assert(shader.includes('vertexColor = Color;'));
    assert(shader.includes('lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);'));
    assert(shader.includes('overlayColor = texelFetch(Sampler1, UV1, 0);'));
    const registration = read(javaRoot + 'graphics/ClientRegistration.java');
    assert(registration.includes('EntityRenderersEvent.RegisterRenderers'));
});
check('Native harvest tags preserve lithium and sulfur tool tiers', () => {
    const tag = name => JSON.parse(read(`src/main/resources/data/minecraft/tags/blocks/${name}.json`));
    assert.deepEqual(tag('mineable/pickaxe'), {replace:false, values:['electricadvantage:li_ore','electricadvantage:sulfur_ore']});
    assert.deepEqual(tag('needs_stone_tool'), {replace:false, values:['electricadvantage:li_ore']});
    assert.equal((read(javaRoot + 'blocks/Ore.java').match(/requiresCorrectToolForDrops\(\)/g) || []).length, 2);
});
check('Shared capabilities, legacy cyano API and first-tick interlocks retained', () => {
    assert(read(javaRoot + 'util/LegacyFluidHandler.java').includes('extends IAdvantageFluidHandler'));
    for (const name of ['ElectricMachineTileEntity', 'ElectricGeneratorTileEntity']) {
        const source = read(javaRoot + `machines/${name}.java`);
        assert(source.includes('!redstoneInitialized && getLevel() != null && !getLevel().isClientSide'));
    }
    assert(!production.some(n => /invalidateCaps\(|reviveCaps\(|LazyOptional/.test(read(n))));
    assert(read(javaRoot + 'ElectricAdvantage.java').includes('event.enqueueWork(RecipeConditions::register)'));
    assert(read(javaRoot + 'machines/PhotovoltaicGeneratorTileEntity.java').includes('BlockPos lightPos = pos.above()'));
});

const changedFiles = [...new Set([
    ...git('diff', '--name-only', baseline).toString().trim().split('\n'),
    ...git('ls-files', '--others', '--exclude-standard').toString().trim().split('\n')
])].filter(Boolean).sort();
const testFiles = [...tracked, ...changedFiles].filter((n, i, all) => all.indexOf(n) === i
    && n.startsWith('src/test/java/') && n.endsWith('.java'));
const report = {baseline, scope:'Static source/resource contracts only; not compilation, runtime or manual acceptance',
    inventory: {productionFiles:production.length, testFiles:testFiles.length,
        tests:testFiles.reduce((n, file) => n + (read(file).match(/@Test\b/g) || []).length, 0)},
    changedFiles:changedFiles.map(file => ({file, sha256:createHash('sha256').update(fs.readFileSync(path.join(root, file))).digest('hex')})),
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
