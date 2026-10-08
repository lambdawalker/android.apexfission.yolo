/** Catalogs link to concrete scopes in their own language, never legacy development URLs. */
export function catalogLinks(scopes,language) {
 return scopes.filter(scope=>scope.language===language).map(scope=>({
  title:`${scope.module} · ${scope.version==='development'?(language==='es'?'Desarrollo':'Development'):scope.version}`,
  href:scope.root,
 }));
}

const legacyGuides={'agents':'', 'reference':'api', 'getting-started':'quickstart', 'task-recipes':'recipes', 'concepts':'concepts', 'limitations':'limitations', 'troubleshooting':'troubleshooting', 'migration':'migration', 'installation':'installation'};
/** Map preserved human URLs to their canonical guide before switching scopes. */
export function guideForPath(path,base,current) {
 if(current)return path.slice(current.root.length).replace(/\/$/,'');
 const legacy=path.slice(base.length).replace(/^\/+|\/+$/g,'');
 if(legacy==='en'||legacy==='es')return '';
 return Object.hasOwn(legacyGuides,legacy)?legacyGuides[legacy]:legacy||'overview';
}
export function scopeHref(scope,slug) {
 return scope.root+(scope.pages.some(p=>p.slug===slug)?(slug?slug+'/':''):'#guide-unavailable');
}
export function markdownHref(scope,slug) {
 const guide=scope.pages.some(p=>p.slug===slug)?slug:'';
 return `${scope.root}raw/${guide||'index'}.md`;
}
