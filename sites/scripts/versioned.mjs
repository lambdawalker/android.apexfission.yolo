/** Read historical documentation as data, never run historical build scripts. */
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readFileSync,readdirSync,existsSync} from 'node:fs';
import {resolve} from 'node:path';
import {unified} from 'unified';
import remarkStringify from 'remark-stringify';
import remarkGfm from 'remark-gfm';
import {visit} from 'unist-util-visit';
import GithubSlugger from 'github-slugger';
import {parser,base,repository} from './markdown.mjs';

export function readRevision(root,ref) {
 if(!/^[0-9a-f]{40}$/.test(ref))throw new Error('Documentation requires a full immutable SHA');
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024,stdio:['ignore','pipe','pipe']});
 let paths;
 try { paths=git('ls-tree','-r','--name-only',ref,'--','docs/agents','docs/es').trim().split('\n').filter(Boolean); }
 catch {throw new Error(`Missing documentation revision ${ref}; fetch full Git history before building`);}
 return new Map(paths.filter(p=>/\.(md|json)$/.test(p)).map(p=>[p,git('show',`${ref}:${p}`)]));
}
export function readTranslationTree(root,tree) {
 if(!/^[0-9a-f]{40}$/.test(tree))throw new Error('Translation tree requires a full SHA');
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024,stdio:['ignore','pipe','pipe']});
 try {
  if(git('cat-file','-t',tree).trim()!=='tree')throw new Error('Not a tree');
  const paths=git('ls-tree','-r','--name-only',tree).trim().split('\n').filter(p=>/\.(md|json)$/.test(p));
  return new Map(paths.map(p=>['docs/es/'+p,git('show',`${tree}:${p}`)]));
 }catch {throw new Error(`Missing translation tree ${tree}; fetch full Git history`);}
}
export function readWorkingDocs(root) {
 const files=new Map();
 function walk(path) {
  for(const e of readdirSync(resolve(root,path),{withFileTypes:true})) {
   const p=path+'/'+e.name;
   if(e.isDirectory())walk(p);else if(/\.(md|json)$/.test(p))files.set(p,readFileSync(resolve(root,p),'utf8'));
  }
 }
 walk('docs/agents');if(existsSync(resolve(root,'docs/es')))walk('docs/es');return files;
}
export function translatedGuide(files,guide,language) {
 const english=files.get('docs/agents/'+guide);
 if(english===undefined)throw new Error(`Missing English guide ${guide} at recorded revision`);
 if(language==='en')return {markdown:english,fallback:false};
 const hashes=JSON.parse(files.get('docs/es/translations.json')||'{}');
 const spanish=files.get('docs/es/'+guide);
 const valid=spanish!==undefined && hashes[guide]===createHash('sha256').update(english).digest('hex');
 return {markdown:valid?spanish:english,fallback:!valid};
}
export const guideSlug=guide=>guide==='index.md'?'':guide.replace(/\.md$/,'');
export const scopePath=c=>`${base}/${c.language}/${c.module}/${c.version}`;
export function rewriteVersioned(markdown,sourcePath,context,mode='human') {
 const tree=parser().parse(markdown),prefix=scopePath(context);
 visit(tree,node=>{
  if(!['link','image','definition'].includes(node.type))return;
  let href=node.url;
  const own=href.startsWith(repository+'/blob/main/');
  if(own)href='/'+href.slice((repository+'/blob/main/').length);
  if(!own && /^(?:[a-z][a-z\d+.-]*:|\/|#)/i.test(href))return;
  const url=new URL(href,`https://local/${sourcePath}`),target=decodeURIComponent(url.pathname.slice(1));
  let output;
  if(target.startsWith('docs/agents/') && context.pages.has(target.slice(12))) {
   const guide=target.slice(12);
   output=mode==='raw'?`${prefix}/raw/${guide}`:`${prefix}/${guideSlug(guide)}${guideSlug(guide)?'/':''}`;
  } else if(target==='IMPORT.md')output=mode==='raw'?`${prefix}/raw/installation.md`:`${prefix}/installation/`;
  else if(node.type==='image')output=`https://raw.githubusercontent.com/lambdawalker/android.apexfission.yolo/${context.ref}/${target}`;
  else output=`${repository}/blob/${(target.startsWith('docs/')||target.startsWith('app/'))?context.ref:(context.source||context.ref)}/${target}`;
  node.url=output+url.search+url.hash;
 });
 return unified().use(remarkStringify,{fences:true,bullet:'-'}).use(remarkGfm).stringify(tree);
}
/** Preserve English heading anchors in translated guides, including raw Markdown. */
export function stableAnchors(english,translated) {
 const headings=md=>{const result=[],slugger=new GithubSlugger();visit(parser().parse(md),'heading',n=>{
  const plain=n=>n.value??(n.children??[]).map(plain).join('');
  result.push({id:slugger.slug(plain(n)),offset:n.position.start.offset,depth:n.depth});
 });return result;};
 const source=headings(english),target=headings(translated);
 if(source.length!==target.length || source.some((h,i)=>h.depth!==target[i].depth))throw new Error('Translation heading structure differs from English');
 let out=translated;
 for(let i=target.length-1;i>=0;i--)if(source[i].id!==target[i].id && target[i].depth>1 && !translated.includes(`id="${source[i].id}"`)){
  const offset=target[i].offset;out=out.slice(0,offset)+`<a id="${source[i].id}"></a>\n\n`+out.slice(offset);
 }
 return out;
}
