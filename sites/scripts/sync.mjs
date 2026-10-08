import {readFile,writeFile,mkdir,rm,readdir} from 'node:fs/promises';
import {resolve,dirname,relative,sep} from 'node:path';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {rewriteMarkdown,humanPages,base,repository} from './markdown.mjs';
const root=resolve('..');
process.env.DOCS_REF ||= execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim();
if (!/^[a-f0-9]{40}$/.test(process.env.DOCS_REF)) throw new Error('DOCS_REF must be a full commit SHA');
execFileSync('python3',['scripts/release.py','verify'],{cwd:root,stdio:'inherit'});
const read=async p=>(await readFile(resolve(root,p),'utf8')).replace(/\r\n/g,'\n');
const portable=p=>p.split(sep).join('/');
async function put(p,s){await mkdir(dirname(p),{recursive:true});await writeFile(p,s);}
async function walk(dir){let result=[];for(const e of await readdir(dir,{withFileTypes:true})){const p=resolve(dir,e.name);result.push(...e.isDirectory()?await walk(p):[p]);}return result;}
const manifest=JSON.parse(await read('docs/api-excerpts.json'));
for(const item of manifest){
 const hash=createHash('sha256').update(await read(item.path)).digest('hex');
 if(hash!==item.sha256)throw new Error(`Review API and update manifest for ${item.path}`);
 await read(item.guide);
}
const internal=['image/LetterboxBuilder.kt','postprocess/YoloNms.kt','tflite/engine/InferenceCore.kt','tflite/validation/TfliteModelLoader.kt'];
for(const path of await walk(resolve(root,'yolo/src/main/java/com/apexfission/android/yolo'))){
 if(!path.endsWith('.kt'))continue;
 const rel=portable(relative(root,path));
 if(!manifest.some(x=>x.path===rel)&&!internal.some(x=>rel.endsWith('/'+x)))throw new Error(`Classify new API source: ${rel}`);
}
const snippets={quickstart:'app/src/main/java/com/apexfission/android/yolo/demo/DocumentationQuickstart.kt',postprocess:'app/src/main/java/com/apexfission/android/yolo/demo/DemoExamples.kt'};
async function expand(text){for(const [id,path] of Object.entries(snippets)){
 const start=`<!-- example: ${id} -->`,end=`<!-- end-example: ${id} -->`;
 if(!text.includes(start))continue;
 const replacement=start+'\n\n```kotlin\n'+(await read(path)).trim()+'\n```\n\n'+end;
 const a=text.indexOf(start),b=text.indexOf(end,a);
 text=text.slice(0,a)+replacement+text.slice(b<0?a+start.length:b+end.length);
}return text;}
for(const file of await walk(resolve(root,'docs/agents'))){if(!file.endsWith('.md'))continue;
 const old=await readFile(file,'utf8'),fresh=await expand(old);
 if(old!==fresh){if(process.argv.includes('--update-snippets'))await writeFile(file,fresh);else throw new Error(`Stale snippet in ${file}; run node scripts/sync.mjs --update-snippets and review`);}
}
await rm('dist',{recursive:true,force:true});
await rm('public',{recursive:true,force:true});await mkdir('public/agents',{recursive:true});
for(const entry of await readdir('src/content/docs'))if(entry!=='index.md')await rm(resolve('src/content/docs',entry),{recursive:true,force:true});
const record=JSON.parse(await read('docs/release.json'));
await put('src/build.json',JSON.stringify({ref:process.env.DOCS_REF,version:record.version}));
const stamp=`> Main development documentation. Build source: [${process.env.DOCS_REF.slice(0,12)}](${repository}/commit/${process.env.DOCS_REF}). Published dependency facts are independent; see [IMPORT.md](${base}/IMPORT.md).\n\n`;
async function human(source,slug){const md=await read(source);const title=(md.match(/^# (.+)$/m)||[])[1];if(!title)throw new Error(`Missing title: ${source}`);
 const body=rewriteMarkdown(md.replace(/^# .+\n/m,''),source,'human');
 await put(`src/content/docs/${slug}.md`,'---\ntitle: '+JSON.stringify(title)+'\n---\n\n'+body);
}
for(const file of await walk(resolve(root,'docs/agents'))){if(!file.endsWith('.md'))continue;
 const source=portable(relative(root,file)),guide=portable(relative(resolve(root,'docs/agents'),file));
 await put(resolve('public/agents',guide),stamp+rewriteMarkdown(await read(source),source));
 await human(source,humanPages[guide]||guide.replace(/\.md$/,''));
}
await put('public/favicon.svg', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="12" fill="#006973"/><path d="M14 26V14h12M38 14h12v12M50 38v12H38M26 50H14V38" fill="none" stroke="white" stroke-width="5"/></svg>');
await put('public/IMPORT.md',rewriteMarkdown(await read('IMPORT.md'),'IMPORT.md'));
for(const [source,slug] of [['IMPORT.md','installation'],['docs/maintenance.md','development'],['docs/coverage.md','coverage'],['docs/releases.md','releases']])await human(source,slug);
await put('public/llms.txt',`# Apexfission YOLO\n\nMain development docs at ${process.env.DOCS_REF}.\n\n- [Agent entry](${base}/agents/index.md)\n- [Installation](${base}/IMPORT.md)\n- [API](${base}/agents/api.md)\n- [Quickstart](${base}/agents/quickstart.md)\n`);
console.log('Verified installation, reviewed API inputs, demo-source snippets, and synchronized human/raw guides.');

const {buildVersioned}=await import('./build-versioned.mjs');
await buildVersioned(root);
