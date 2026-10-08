import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,rmSync,readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readRevision,readTranslationTree,translatedGuide,rewriteVersioned,stableAnchors} from '../versioned.mjs';
import {catalogLinks,guideForPath,scopeHref,markdownHref} from '../navigation.mjs';
import {base,repository} from '../markdown.mjs';
test('reads pinned Git documentation after the working file changes; refuses unknown revision',()=>{
 const root=mkdtempSync(join(tmpdir(),'docs-history-'));
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
 try{
  git('init','-q');git('config','user.name','Test');git('config','user.email','test@example.invalid');
  mkdirSync(join(root,'docs/agents'),{recursive:true});writeFileSync(join(root,'docs/agents/index.md'),'# Old API\n');
  git('add','.');git('commit','-qm','first');const sha=git('rev-parse','HEAD');
  writeFileSync(join(root,'docs/agents/index.md'),'# New API\n');
  assert.equal(readRevision(root,sha).get('docs/agents/index.md'),'# Old API\n');
  assert.throws(()=>readRevision(root,'f'.repeat(40)),/revision/i);
  assert.throws(()=>readRevision(root,'main'),/SHA/);
 }finally{rmSync(root,{recursive:true,force:true});}
});
test('missing or stale translations fall back to same-source English',()=>{
 const en='# Ownership\n\nKeep bitmap alive.\n',es='# Propiedad\n\nConserva el bitmap.\n';
 const files=new Map([['docs/agents/concepts.md',en],['docs/es/concepts.md',es]]);
 assert.equal(translatedGuide(files,'concepts.md','es').fallback,true);
 files.set('docs/es/translations.json',JSON.stringify({'concepts.md':createHash('sha256').update(en).digest('hex')}));
 assert.deepEqual(translatedGuide(files,'concepts.md','es'),{markdown:es,fallback:false});
 files.set('docs/agents/concepts.md',en+'Changed.');
 assert.equal(translatedGuide(files,'concepts.md','es').markdown,en+'Changed.');
});
test('links and raw guides remain version scoped, source/media links pinned, code untouched',()=>{
 const context={language:'es',module:'yolo',version:'0.1.2',ref:'a'.repeat(40),source:'b'.repeat(40),pages:new Set(['concepts.md','api.md'])};
 const input='[Ownership](concepts.md#ownership-table) [Install](../../IMPORT.md) [Source](../../yolo/src/Foo.kt)\n\n```text\n[unchanged](../concepts.md)\n```';
 const out=rewriteVersioned(input,'docs/agents/api.md',context);
 assert.ok(out.includes(`${base}/es/yolo/0.1.2/concepts/#ownership-table`));
 assert.ok(out.includes(`${base}/es/yolo/0.1.2/installation/`));
 assert.ok(out.includes(`${repository}/blob/${context.source}/yolo/src/Foo.kt`));
 assert.ok(out.includes('[unchanged](../concepts.md)'));
 const raw=rewriteVersioned(input,'docs/agents/api.md',context,'raw');
 assert.ok(raw.includes(`${base}/es/yolo/0.1.2/raw/concepts.md#ownership-table`));
});
test('fresh Spanish translations preserve original code',()=>{
 const root=new URL('../../../',import.meta.url);
 const hashes=JSON.parse(readFileSync(new URL('docs/es/translations.json',root),'utf8'));
 for(const [guide,hash] of Object.entries(hashes)) {
  const en=readFileSync(new URL('docs/agents/'+guide,root),'utf8');
  const es=readFileSync(new URL('docs/es/'+guide,root),'utf8');
  assert.match(hash,/^[0-9a-f]{64}$/,guide+' invalid source hash');
  if(createHash('sha256').update(en).digest('hex')!==hash)continue; // Stale translations intentionally fall back to English.
  assert.deepEqual(es.match(/```[\s\S]*?```/g),en.match(/```[\s\S]*?```/g),guide+' modified code');
 }
 assert.ok(Object.keys(hashes).length>0);
});

test('translation tree remains readable after squash-style commit replacement',()=>{
 const root=mkdtempSync(join(tmpdir(),'docs-translation-tree-'));
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
 try {
  git('init','-q');git('config','user.name','Test');git('config','user.email','test@example.invalid');
  mkdirSync(join(root,'docs/es'),{recursive:true});writeFileSync(join(root,'docs/es/index.md'),'# Español\n');
  git('add','.');git('commit','-qm','translation');const tree=git('rev-parse','HEAD:docs/es');
  // A squash keeps content trees but gives the containing commit a different SHA.
  git('commit','--amend','-qm','squashed');
  assert.equal(readTranslationTree(root,tree).get('docs/es/index.md'),'# Español\n');
  assert.throws(()=>readTranslationTree(root,'f'.repeat(40)),/translation tree/i);
 }finally{rmSync(root,{recursive:true,force:true});}
});

test('catalog navigation preserves language and offers module release scopes',()=>{
 const scopes=[{language:'en',module:'yolo',version:'0.1.2',root:`${base}/en/yolo/0.1.2/`},
  {language:'es',module:'yolo',version:'0.1.2',root:`${base}/es/yolo/0.1.2/`},
  {language:'es',module:'yolo',version:'development',root:`${base}/es/yolo/development/`}];
 const entries=catalogLinks(scopes,'es');
 assert.equal(entries.length,2);
 assert.ok(entries.every(e=>e.href.startsWith(`${base}/es/`)));
 assert.ok(entries.some(e=>e.title.includes('Desarrollo')));
});

test('complete archived guide rendering retains dotted versions and exact installation after edits',async()=>{
 const {buildVersioned}=await import('../build-versioned.mjs');
 const root=mkdtempSync(join(tmpdir(),'yolo-archive-'));
 const previous=process.cwd(),previousRef=process.env.DOCS_REF;
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
 try {
  for(const p of ['docs/agents','docs/es','scripts','sites'])mkdirSync(join(root,p),{recursive:true});
  git('init','-q');git('config','user.name','Test');git('config','user.email','test@example.invalid');
  writeFileSync(join(root,'docs/agents/index.md'),'# Old yolo guide\n\n[API](api.md#contract) [Install](../../IMPORT.md)\n');
  writeFileSync(join(root,'docs/agents/api.md'),'# Old API\n\n## Contract\n\nOld contract.\n');
  git('add','.');git('commit','-qm','confirmed test source');const ref=git('rev-parse','HEAD');
  const record={module:'yolo',version:'1.2.3',source:ref,documentation_ref:ref,destinations:{central:{}},installation:{en:'# Installation\n\n`example:yolo:1.2.3`\n',es:'# Instalación\n\n`example:yolo:1.2.3`\n'}};
  writeFileSync(join(root,'scripts/documentation_history.py'),'print('+JSON.stringify(JSON.stringify([record]))+')\n');
  writeFileSync(join(root,'docs/agents/api.md'),'# New API\n\n## Contract\n\nNew development contract.\n');
  process.chdir(join(root,'sites'));process.env.DOCS_REF=ref;
  await buildVersioned(root);
  const old=readFileSync('public/es/yolo/1.2.3/raw/api.md','utf8');
  assert.match(old,/Old contract/);assert.doesNotMatch(old,/New development/);assert.match(old,/Traducción pendiente/);
  assert.match(readFileSync('src/content/docs/en/yolo/1.2.3/api.md','utf8'),/slug: "en\/yolo\/1\.2\.3\/api"/);
  assert.match(readFileSync('public/en/yolo/1.2.3/raw/index.md','utf8'),/en\/yolo\/1\.2\.3\/raw\/api\.md#contract/);
  assert.match(readFileSync('public/en/yolo/1.2.3/raw/installation.md','utf8'),/example:yolo:1\.2\.3/);
  assert.match(readFileSync('public/en/yolo/development/raw/api.md','utf8'),/New development contract/);
  const scopes=JSON.parse(readFileSync('public/versions.json','utf8'));
  assert.equal(scopes.length,4);assert.ok(scopes.every(s=>s.pages.some(p=>p.slug==='installation')));
 }finally{process.chdir(previous);if(previousRef===undefined)delete process.env.DOCS_REF;else process.env.DOCS_REF=previousRef;rmSync(root,{recursive:true,force:true});}
});


test('legacy routes retain their canonical guide for version, language and raw links',()=>{
 const scope={root:`${base}/es/yolo/1.2.3/`,pages:['','compose','code-only','api','quickstart','platform-recipes','recipes'].map(slug=>({slug}))};
 for(const [legacy,guide] of Object.entries({reference:'api','getting-started':'quickstart','task-recipes':'recipes',agents:''})){
  const slug=guideForPath(`${base}/${legacy}/`,base);
  assert.equal(slug,guide);
  assert.equal(scopeHref(scope,slug),scope.root+(guide?guide+'/':''));
  assert.equal(markdownHref(scope,slug),scope.root+`raw/${guide||'index'}.md`);
 }
 assert.equal(scopeHref(scope,guideForPath(`${base}/gallery/`,base)),scope.root+'#guide-unavailable');
 assert.equal(scopeHref(scope,guideForPath(`${base}/`,base)),scope.root+'#guide-unavailable');
 assert.equal(guideForPath(`${scope.root}api/`,base,scope),'api');
});


test('translations retain canonical duplicate heading anchors',()=>{
 const result=stableAnchors('# Guide\n\n## Contract\n\n## Contract\n','# Guía\n\n## Contrato\n\n## Contrato\n');
 assert.match(result,/<a id="contract"><\/a>/);
 assert.match(result,/<a id="contract-1"><\/a>/);
 assert.throws(()=>stableAnchors('# Guide\n## Contract\n','# Guía\n'),/structure/);
});

test('postrelease executable demo uses documentation provenance',()=>{
 const context={language:'en',module:'yolo',version:'0.1.0',ref:'a'.repeat(40),source:'b'.repeat(40),pages:new Set()};
 const output=rewriteVersioned('[Demo](../../app/src/main/java/com/apexfission/android/yolo/demo/DocumentationQuickstart.kt) [API](../../src/main/Foo.kt)','docs/agents/quickstart.md',context);
 assert.ok(output.includes(`/blob/${context.ref}/app/src/`));
 assert.ok(output.includes(`/blob/${context.source}/src/main/`));
});
