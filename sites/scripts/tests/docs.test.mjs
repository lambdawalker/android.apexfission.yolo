import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp,mkdir,writeFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {rewriteMarkdown,base} from '../markdown.mjs';
import {validateSite} from '../validate-docs.mjs';
test('nested raw links preserve Markdown targets, anchors, and source ref',()=>{
 process.env.DOCS_REF='a'.repeat(40);
 const out=rewriteMarkdown('[concepts](../concepts.md#ownership-table) [source](../../../yolo/test.kt) [install](../../../IMPORT.md)','docs/agents/api/image.md');
 assert.match(out,/\.\.\/concepts.md#ownership-table/);
 assert.match(out,new RegExp('/blob/'+'a'.repeat(40)+'/yolo/test.kt'));
 assert.match(out,/\.\.\/\.\.\/IMPORT.md/);
});
test('Markdown-aware rewriting leaves code, external URLs and fragments alone',()=>{
 const input='[outside](https://example.org/a.md) [local](#local)\n\n```kotlin\n"[x](../../IMPORT.md)"\n```\n';
 const out=rewriteMarkdown(input,'docs/agents/index.md');
 assert.ok(out.includes('https://example.org/a.md'));assert.ok(out.includes('[local](#local)'));
 assert.ok(out.includes('"[x](../../IMPORT.md)"'));
});
test('human nested API links resolve to HTML routes',()=>{
 const out=rewriteMarkdown('[API](api/image.md)','docs/agents/index.md','human');
 assert.ok(out.includes(`${base}/api/image/`));
});
test('validator rejects missing targets, anchors, and domain-root assets',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'yolo-docs-'));
 try{
  await mkdir(join(dir,'agents'));
  await writeFile(join(dir,'index.html'),`<a href="${base}/agents/index.md#missing">bad</a><img src="/wrong.png"><a href="${base}/absent/">missing</a>`);
  await writeFile(join(dir,'agents/index.md'),'# Valid\n');
  const errors=await validateSite(dir);
  assert.equal(errors.length,3);assert.ok(errors.some(x=>x.includes('missing anchor')));
 }finally{await rm(dir,{recursive:true,force:true});}
});


test('validator checks navigation options and explicit Markdown anchors',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'yolo-selectors-'));
 try {
  await writeFile(join(dir,'index.html'),`<select data-doc-navigation><option value="${base}/absent/">Missing</option></select><a href="${base}/guide.md#contract">Guide</a>`);
  await writeFile(join(dir,'guide.md'),'# Guía\n\n<a id="contract"></a>\n\n## Contrato\n');
  const errors=await validateSite(dir);
  assert.equal(errors.length,1);assert.match(errors[0],/missing target/);
 }finally {await rm(dir,{recursive:true,force:true});}
});
