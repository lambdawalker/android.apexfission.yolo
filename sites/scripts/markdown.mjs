import { unified } from 'unified';
import remarkParse from 'remark-parse';
import remarkGfm from 'remark-gfm';
import remarkStringify from 'remark-stringify';
import { visit } from 'unist-util-visit';
import { posix } from 'node:path';

export const base = '/android.apexfission.yolo';
export const repository = 'https://github.com/lambdawalker/android.apexfission.yolo';
export const humanPages = {
  'index.md': 'agents', 'quickstart.md': 'getting-started', 'api.md': 'reference',
  'model-contract.md': 'model-contract', 'demos.md': 'demos', 'concepts.md': 'concepts', 'limitations.md': 'limitations', 'troubleshooting.md': 'troubleshooting',
  'migration.md': 'migration', 'recipes.md': 'task-recipes',
};
export const parser = () => unified().use(remarkParse).use(remarkGfm);
export function rewriteMarkdown(markdown, sourcePath, mode = 'raw') {
  const tree = parser().parse(markdown);
  visit(tree, node => {
    if (!['link','image','definition'].includes(node.type)) return;
    let href = node.url;
    if (href.startsWith(repository + '/blob/main/')) href = '/' + href.slice((repository + '/blob/main/').length);
    const ownSource = node.url.startsWith(repository + '/blob/main/');
    if (!ownSource && /^(?:[a-z][a-z\d+.-]*:|\/|#)/i.test(href)) return;
    const url = new URL(href, `https://local/${sourcePath}`);
    const target = decodeURIComponent(url.pathname.slice(1));
    const suffix = url.search + url.hash;
    let output;
    if (target.startsWith('docs/agents/')) {
      const guide = target.slice('docs/agents/'.length);
      output = mode === 'human'
        ? `${base}/${humanPages[guide] || guide.replace(/\.md$/, '')}/`
        : `${base}/agents/${guide}`;
    } else if (mode === 'human' && ['docs/maintenance.md','docs/releases.md','docs/coverage.md'].includes(target)) output = `${base}/${{'docs/maintenance.md':'development','docs/releases.md':'releases','docs/coverage.md':'coverage'}[target]}/`;
    else if (target === 'IMPORT.md') output = mode === 'human' ? `${base}/installation/` : `${base}/IMPORT.md`;
    else if (target.startsWith('docs/screenshots/') && target.endsWith('.png')) output = `${base}/screenshots/${target.slice('docs/screenshots/'.length)}`;
    else output = `${repository}/blob/${process.env.DOCS_REF || 'main'}/${target}`;
    if (mode === 'raw' && output.startsWith(base + '/')) {
      const from = sourcePath.startsWith('docs/agents/') ? `agents/${sourcePath.slice('docs/agents/'.length)}` : sourcePath;
      output = posix.relative(posix.dirname(from), output.slice(base.length + 1));
    }
    node.url = output + suffix;
  });
  return unified().use(remarkStringify, {fences:true, bullet:'-'}).use(remarkGfm).stringify(tree);
}
