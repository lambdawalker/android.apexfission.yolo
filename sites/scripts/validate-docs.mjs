import { readFile, readdir, stat } from 'node:fs/promises';
import { resolve, relative, sep } from 'node:path';
import { pathToFileURL } from 'node:url';
import { parse } from 'parse5';
import GithubSlugger from 'github-slugger';
import { visit } from 'unist-util-visit';
import { base, parser } from './markdown.mjs';

async function filesAt(dir) {
  const files = [];
  for (const entry of await readdir(dir,{withFileTypes:true})) {
    const path = resolve(dir,entry.name);
    if (entry.isDirectory()) files.push(...await filesAt(path)); else files.push(path);
  }
  return files;
}
function inspect(text, html) {
  const links = [], ids = new Set();
  if (html) {
    function walk(node) {
      for (const {name,value} of node.attrs ?? []) {
        if (name === 'id') ids.add(value);
        if (['href','src'].includes(name) && !(node.tagName === 'link' && node.attrs.some(a => a.name === 'rel' && a.value === 'canonical'))) links.push(value);
      }
      for (const child of node.childNodes ?? []) walk(child);
    }
    walk(parse(text));
  } else {
    const tree = parser().parse(text), slugger = new GithubSlugger();
    const plain = node => node.value ?? (node.children ?? []).map(plain).join('');
    visit(tree, node => {
      if (node.type === 'heading') ids.add(slugger.slug(plain(node)));
      if (['link','image','definition'].includes(node.type)) links.push(node.url);
    });
  }
  return {links,ids};
}
export async function validateSite(directory) {
  const root = resolve(directory), errors = [], cache = new Map();
  const documents = (await filesAt(root)).filter(p => /\.(html|md|txt)$/.test(p));
  async function document(path) {
    if (!cache.has(path)) cache.set(path,inspect(await readFile(path,'utf8'),path.endsWith('.html')));
    return cache.get(path);
  }
  for (const source of documents) {
    const {links} = await document(source);
    const sourcePath = relative(root,source).split(sep).join('/');
    for (const href of links) {
      const origin = 'https://lambdawalker.github.io';
      const location = `${origin}${base}/${sourcePath.replace(/index\.html$/, '')}`;
      const url = new URL(href, location);
      if (url.origin !== origin) continue; // External source URLs are not a network availability gate.
      if (!url.pathname.startsWith(base + '/')) { errors.push(`${sourcePath}: outside project base: ${href}`); continue; }
      let target = resolve(root,decodeURIComponent(url.pathname.slice(base.length + 1)));
      if (target !== root && !target.startsWith(root + sep)) { errors.push(`${sourcePath}: path escape: ${href}`); continue; }
      try {
        if ((await stat(target)).isDirectory()) target = resolve(target,'index.html');
        await stat(target);
        if (url.hash && /\.(html|md)$/.test(target) && !(await document(target)).ids.has(decodeURIComponent(url.hash.slice(1)))) {
          errors.push(`${sourcePath}: missing anchor: ${href}`);
        }
      } catch (error) { if (error.code !== 'ENOENT') throw error; errors.push(`${sourcePath}: missing target: ${href}`); }
    }
  }
  return errors;
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const errors = await validateSite(resolve('dist'));
  for (const path of ['agents/index.md','agents/api.md','IMPORT.md','llms.txt','reference/index.html','getting-started/index.html']) {
    try { await stat(resolve('dist',path)); } catch { errors.push(`Missing required output: ${path}`); }
  }
  if (errors.length) { console.error(errors.join('\n')); process.exitCode = 1; }
  else console.log('Documentation links, anchors, images, project base and required raw Markdown outputs passed. External availability is not checked.');
}
