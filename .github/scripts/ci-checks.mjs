import { execFileSync } from 'node:child_process';
import { appendFileSync, readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

export const allowlistPath = '.github/scripts/local-date-now-allowlist.txt';
const forbidden = /\b(?:(?:LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime)\s*\.\s*now|ZoneId\s*\.\s*systemDefault|TimeZone\s*\.\s*getDefault)\s*\(\s*\)/;

export function classify(paths) {
  const all = paths.includes('.github/workflows/ci.yml');
  const code = paths.filter(path => !path.endsWith('.md'));
  return {
    shared: all || code.some(path => path.startsWith('shared/')),
    backend: all || code.some(path => /^(backend\/|shared\/)/.test(path)),
    android: all || code.some(path => /^(app\/|shared\/|scripts\/|\.github\/scripts\/)/.test(path)),
    docs: all || paths.some(path => path.endsWith('.md')),
    docker: all || code.some(path => /^(backend\/|shared\/)/.test(path) ||
      ['docker-compose.yml', '.dockerignore', '.github/scripts/check-docker-image.mjs'].includes(path)),
  };
}

export function wrapperErrors(directory) {
  const file = directory + '/gradle/wrapper/gradle-wrapper.properties';
  const content = readFileSync(file, 'utf8').replaceAll('\r', '');
  return /^[ \t]*distributionSha256Sum[ \t]*=[ \t]*[a-fA-F0-9]{64}[ \t]*$/m.test(content)
    ? [] : [file + ': falta un distributionSha256Sum SHA-256 válido'];
}

export function timezoneErrors(sources, allowed, previous = allowed) {
  const errors = [];
  const violations = Object.keys(sources).filter(path =>
    !path.includes('/core/time/') && forbidden.test(sources[path]));
  for (const path of violations) {
    if (!allowed.includes(path)) errors.push(path + ': usa la zona horaria del sistema');
  }
  for (const path of allowed) {
    if (!violations.includes(path)) errors.push(path + ': quitar la excepción que ya no se usa');
    if (!previous.includes(path)) errors.push(path + ': la lista de excepciones no puede crecer');
  }
  return errors;
}

const git = (...args) => execFileSync('git', args, { encoding: 'utf8' });
const entries = text => text.split(/\r?\n/).map(line => line.trim())
  .filter(line => line && !line.startsWith('#'));

function main([command, ...args]) {
  let errors = [];
  if (command === 'changes') {
    let [base, head] = args;
    if (!base || /^0+$/.test(base)) base = git('hash-object', '-t', 'tree', '--stdin').trim();
    const paths = git('diff', '--no-renames', '--name-only', '-z', base, head).split('\0').filter(Boolean);
    const result = classify(paths);
    const output = Object.entries(result).map(([key, value]) => key + '=' + value).join('\n') + '\n';
    if (process.env.GITHUB_OUTPUT) appendFileSync(process.env.GITHUB_OUTPUT, output);
    process.stdout.write(output);
  } else if (command === 'wrapper') {
    errors = args.flatMap(wrapperErrors);
    if (!args.length) throw new Error('Indicar al menos un build');
  } else if (command === 'timezone') {
    const paths = git('ls-files', '-z', 'app/app/src/main').split('\0').filter(path => path.endsWith('.kt'));
    const sources = Object.fromEntries(paths.map(path => [path, readFileSync(path, 'utf8')]));
    const allowed = entries(readFileSync(allowlistPath, 'utf8'));
    let previous = allowed;
    if (process.env.CI_BASE_SHA && !/^0+$/.test(process.env.CI_BASE_SHA)) {
      try {
        previous = entries(git('show', process.env.CI_BASE_SHA + ':' + allowlistPath));
      } catch {
        // Primera incorporación: solo eximir usos que ya existían en la base del PR.
        previous = allowed.filter(path => {
          try { return forbidden.test(git('show', process.env.CI_BASE_SHA + ':' + path)); }
          catch { return false; }
        });
      }
    }
    errors = timezoneErrors(sources, allowed, previous);
  } else {
    throw new Error('Comando desconocido: ' + command);
  }
  if (errors.length) throw new Error(errors.join('\n'));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { main(process.argv.slice(2)); }
  catch (error) { console.error(error.message); process.exitCode = 1; }
}
