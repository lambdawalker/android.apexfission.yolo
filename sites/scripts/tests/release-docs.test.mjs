import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import test from 'node:test';
import {buildVersioned} from '../build-versioned.mjs';

const repository = fileURLToPath(new URL('../../../', import.meta.url));
const moduleName = 'yolo';

test('the release exporter renders both providers at the same version', async () => {
    const root = mkdtempSync(join(tmpdir(), 'release-docs-'));
    const previous = process.cwd();
    const previousRef = process.env.DOCS_REF;
    const git = (...args) => execFileSync('git', args, {cwd: root, encoding: 'utf8'}).trim();
    try {
        cpSync(join(repository, 'docs'), join(root, 'docs'), {
            recursive: true,
            filter: path => !/\.(png|jpe?g|webp|mp4|zip)$/i.test(path),
        });
        rmSync(join(root, 'docs/releases'), {recursive: true, force: true});
        mkdirSync(join(root, 'docs/releases/jitpack'), {recursive: true});
        mkdirSync(join(root, 'sites'));
        mkdirSync(join(root, 'scripts'));
        cpSync(join(repository, '.project/docs'), join(root, '.project/docs'), {recursive: true});
        git('init', '-q');
        git('config', 'user.name', 'Test');
        git('config', 'user.email', 'test@example.invalid');
        const guide = join(root, 'docs/agents/index.md');
        const publishSource = marker => {
            writeFileSync(guide, `# Test guide\n\n${marker} source.\n`);
            git('add', 'docs/agents');
            git('commit', '-qm', marker);
            return git('rev-parse', 'HEAD');
        };
        const version = '9.8.7';
        const record = {
            module: moduleName, version, phase: 'confirmed-public',
            group: 'example.library', artifact: moduleName,
        };
        const central = {...record, repository: 'maven-central', source: publishSource('Central fixture'),
            source_tag: `${moduleName}/v${version}-maven-central`};
        const jitpack = {...record, repository: 'jitpack', source: publishSource('JitPack fixture'),
            repository_url: 'https://jitpack.io', source_tag: `${moduleName}/v${version}-jitpack`,
            consumer_version: `${moduleName}~v${version}-jitpack`};
        for (const [file, release] of [[`${moduleName}.json`, central], [`jitpack/${moduleName}.json`, jitpack]]) {
            writeFileSync(join(root, 'docs/releases', file), JSON.stringify(release));
        }
        execFileSync('android-repo-docs', ['history', 'seed'], {cwd: root});
        process.chdir(join(root, 'sites'));
        process.env.DOCS_REF = jitpack.source;
        await buildVersioned(root);
        for (const [provider, marker, release] of [['maven-central', 'Central fixture', central], ['jitpack', 'JitPack fixture', jitpack]]) {
            const route = resolve('public/en', moduleName, `${version}-${provider}`, 'raw');
            assert.ok(readFileSync(join(route, 'index.md'), 'utf8').includes(marker));
            const install = readFileSync(join(route, 'installation.md'), 'utf8');
            assert.ok(install.includes(`example.library:${moduleName}:${release.consumer_version ?? version}`));
            if (provider === 'maven-central') assert.ok(!install.includes(jitpack.consumer_version));
        }
    } finally {
        process.chdir(previous);
        if (previousRef === undefined) delete process.env.DOCS_REF;
        else process.env.DOCS_REF = previousRef;
        rmSync(root, {recursive: true, force: true});
    }
});
