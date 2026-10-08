import {execFileSync} from 'node:child_process';
import {writeFile,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';
import {readRevision,readTranslationTree,readWorkingDocs,translatedGuide,rewriteVersioned,stableAnchors,guideSlug,scopePath} from './versioned.mjs';
import {base,repository} from './markdown.mjs';

export async function buildVersioned(root) {
 const catalog=JSON.parse(execFileSync('python3',['scripts/documentation_history.py','export'],{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024}));
 const put=async(path,text)=>{await mkdir(dirname(path),{recursive:true});await writeFile(path,text);};
 const snapshots=new Map(),scopes=[];
 const working=readWorkingDocs(root);
 const entries=[...catalog,...['yolo'].map(module=>({module,version:'development',source:process.env.DOCS_REF,documentation_ref:process.env.DOCS_REF}))];
  for(const entry of entries) {
  const dev=entry.version==='development',ref=entry.documentation_ref;
  if(!dev&&!snapshots.has(ref))snapshots.set(ref,readRevision(root,ref));
  const files=new Map(dev?working:snapshots.get(ref));
  if(entry.translation_tree)for(const [path,text] of readTranslationTree(root,entry.translation_tree))files.set(path,text);
  const available=[...files.keys()].filter(p=>p.startsWith('docs/agents/')&&p.endsWith('.md')).map(p=>p.slice(12));
  if(!available.includes('index.md'))throw new Error(`No library guides at ${ref}; explicitly record a reviewed documentation correction`);
  for(const language of ['en','es']) {
   const es=language==='es';
   const pageNames=available;
   const context={language,module:entry.module,version:entry.version,ref,source:entry.source,pages:new Set(pageNames)};
   const prefix=scopePath(context),scope={...context,source:entry.source,root:prefix+'/',pages:[]};
   const sourceLink=`[${entry.source.slice(0,12)}](${repository}/commit/${entry.source})`;
   const docsLink=`[${ref.slice(0,12)}](${repository}/commit/${ref})`;
   const translationNote=entry.translation_tree?` · ${es?'Traducción':'Translation'}: \`${entry.translation_tree.slice(0,12)}\``:'';
   const exampleNote=!dev&&ref!==entry.source?(es?`> **Corrección posterior a la publicación:** las guías y los ejemplos ejecutables se añadieron en la revisión de documentación. Para ejecutar los comandos de ejemplos, usa esa revisión (${ref}); el código de la biblioteca coincide con el de la publicación.\n\n`:`> **Postrelease documentation correction:** these guides and the runnable examples were added at the documentation revision. Run the example commands from that revision (${ref}); library implementation matches the release.\n\n`):'';
   const notice=es
    ? `> **${dev?'Documentación de desarrollo; puede incluir API aún no publicada':`Documentación de ${entry.module} ${entry.version}`}** · Código: ${sourceLink} · Documentación: ${docsLink}${translationNote}. ${!dev?'Las menciones a main en las guías archivadas se refieren a la revisión indicada.':''}\n\n`
    : `> **${dev?'Development documentation; may include unreleased API':`${entry.module} ${entry.version} documentation`}** · Code: ${sourceLink} · Documentation: ${docsLink}${translationNote}. ${!dev?'References to main in archived guides mean the recorded revision.':''}\n\n`;
   async function page(guide,markdown,fallback=false) {
    const slug=guideSlug(guide),title=markdown.match(/^# (.+)$/m)?.[1];
    if(!title)throw new Error(`No title: ${guide}`);
    scope.pages.push({slug,title});
    const warning=fallback?(es?'> **Traducción pendiente:** esta página muestra el original en inglés de esta misma revisión.\n\n':'> Translation pending.\n\n'):'';
    const sourcePath='docs/agents/'+guide;
    const raw=notice+exampleNote+warning+rewriteVersioned(markdown,sourcePath,context,'raw');
    await put(`public/${language}/${entry.module}/${entry.version}/raw/${guide}`,raw);
    const body=rewriteVersioned(markdown.replace(/^# .+\n/m,''),sourcePath,context);
    await put(`src/content/docs/${language}/${entry.module}/${entry.version}/${slug||'index'}.md`,
     `---\nslug: ${JSON.stringify(`${language}/${entry.module}/${entry.version}${slug?"/"+slug:""}`)}\ntitle: ${JSON.stringify(title+' · '+entry.version+' · '+language)}\neditUrl: false\nprev: false\nnext: false\n---\n\n${notice}${exampleNote}${warning}${body}\n\n[${es?'Leer Markdown':'Read Markdown'}](${prefix}/raw/${guide})\n`);
   }
   for(const guide of pageNames) {
    let {markdown,fallback}=translatedGuide(files,guide,language);
    if(es&&!fallback)markdown=stableAnchors(files.get('docs/agents/'+guide),markdown);
    await page(guide,markdown,fallback);
   }
   let install=entry.installation?.[language];
   if(dev) {
    const latest=catalog.filter(e=>e.module===entry.module).at(-1);
    install=es?`# Instalación\n\nEl código de desarrollo puede contener API aún no publicada.\n\n${latest?`[Instalar la última versión confirmada: ${latest.version}](${base}/es/${entry.module}/${latest.version}/installation/)`:`No hay versiones archivadas con identidad de código verificable. Consulta la [instalación heredada](${base}/installation/); estas guías de desarrollo no describen necesariamente ese paquete.`}\n`
      :`# Installation\n\nDevelopment source may contain unreleased API.\n\n${latest?`[Install latest confirmed version: ${latest.version}](${base}/en/${entry.module}/${latest.version}/installation/)`:`No releases have archived, verifiable code identity yet. See [legacy installation](${base}/installation/); these development guides do not necessarily describe that package.`}\n`;
   }
   await page('installation.md',install);
   scopes.push(scope);
  }
 }
 for(const language of ['en','es']) {
  const es=language==='es';let body=`# ${es?'Documentación de la biblioteca':'Library documentation'}\n\n${es?'Selecciona una versión.':'Choose a documentation version.'}\n\n`;
  for(const module of ['yolo']) {

   for(const e of catalog.filter(e=>e.module===module).reverse())body+=`- [${e.version}](${base}/${language}/${module}/${e.version}/) — ${Object.keys(e.destinations).join(', ')}\n`;
   body+=`- [${es?'Desarrollo (sin publicar)':'Development (unreleased)'}](${base}/${language}/${module}/development/)\n\n`;
  }
  body+=es?'El archivo comienza con las publicaciones confirmadas disponibles al incorporar este sistema; no representa un historial completo de versiones anteriores.\n':'This archive starts with confirmed publications available when this system was introduced; it is not a complete record of older releases.\n';
  const title=body.match(/^# (.+)/)[1];
  await put(`src/content/docs/${language}/index.md`,`---\ntitle: ${JSON.stringify(title)}\nprev: false\nnext: false\n---\n\n`+body.replace(/^# .+\n/,''));
 }
 await put('public/versions.json',JSON.stringify(scopes,null,2)+'\n');
 await put('src/versions.json',JSON.stringify(scopes,null,2)+'\n');
 console.log(`Rendered ${catalog.length} archived releases and bilingual development documentation.`);
}
