// Fails if src/api/schema.gen.d.ts does not match what contracts/openapi.yaml generates today.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const COMMITTED = 'src/api/schema.gen.d.ts';
const CONTRACT = '../contracts/openapi.yaml';
const CLI = 'node_modules/openapi-typescript/bin/cli.js';

// Line endings depend on the checkout, so they never count as drift.
const read = (path) => readFileSync(path, 'utf8').replaceAll('\r\n', '\n');

const directory = mkdtempSync(join(tmpdir(), 'wolfandfox-types-'));
try {
  const generated = join(directory, 'schema.gen.d.ts');
  const result = spawnSync(process.execPath, [CLI, CONTRACT, '-o', generated], {
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    console.error(`types:check could not generate types.\n${result.stdout}\n${result.stderr}`);
    process.exit(1);
  }
  if (read(generated) !== read(COMMITTED)) {
    console.error(`${COMMITTED} is out of date with ${CONTRACT}. Run: npm run types:generate`);
    process.exit(1);
  }
  console.log(`${COMMITTED} matches ${CONTRACT}.`);
} finally {
  rmSync(directory, { recursive: true, force: true });
}
