import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

// Ejecutar después de Compose --wait. Inspecciona el filesystem y los jars reales.
const container = process.argv[2];
assert.ok(container, 'Indicar el contenedor del backend');
const run = (command, ...args) => execFileSync(command, args, { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const docker = (...args) => run('docker', ...args);
const temporary = mkdtempSync(join(tmpdir(), 'pasitos-image-'));
const envFile = /(?:^|\/)\.env(?:\.[^/]*)?$/i;
const privateKey = /\.(?:pem|jks|keystore)$/i;
try {
  assert.equal(docker('exec', container, 'id', '-u').trim(), '10001');
  docker('exec', container, 'sh', '-c', 'test ! -w /opt/pasitos && test ! -w /opt/pasitos/bin/pasitos-backend && test ! -w /opt/pasitos/lib');
  const archive = join(temporary, 'image.tar');
  docker('export', '-o', archive, container);
  const files = run('tar', '-tf', archive).split(/\r?\n/);
  assert.deepEqual(files.filter(path => envFile.test(path)), [], 'La imagen contiene archivos .env');
  assert.deepEqual(files.filter(path => /^(?:\.\/)?opt\/pasitos\//.test(path) && privateKey.test(path)), [], 'La aplicación contiene claves');
  docker('cp', container + ':/opt/pasitos/lib', join(temporary, 'lib'));
  const jars = readdirSync(join(temporary, 'lib')).filter(name => /^(?:pasitos-backend|shared)-.*\.jar$/.test(name));
  assert.equal(jars.length, 2, 'Deben existir los jars de backend y shared para inspeccionarlos');
  const jar = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'jar.exe' : 'jar') : 'jar';
  for (const name of jars) {
    const entries = run(jar, 'tf', join(temporary, 'lib', name)).split(/\r?\n/);
    assert.deepEqual(entries.filter(path => envFile.test(path) || privateKey.test(path)), [], 'Secretos dentro de ' + name);
  }
  console.log('I0-09: uid 10001, aplicación de solo lectura y sin secretos en filesystem ni jars.');
} finally {
  rmSync(temporary, { recursive: true, force: true });
}
