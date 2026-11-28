import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { classify, timezoneErrors, wrapperErrors } from './ci-checks.mjs';

const none = { shared: false, backend: false, android: false, docs: false, docker: false };

test('I0-03: solo Markdown no compila Android, incluso dentro de app', () => {
  assert.deepEqual(classify(['app/README.md', 'CONTRIBUTING.md']), { ...none, docs: true });
});
test('I0-03: código Android, shared y controles ejecutan Android', () => {
  for (const file of ['app/app/build.gradle.kts', 'shared/src/Model.kt', 'scripts/check.sh', '.github/scripts/ci-checks.mjs']) {
    assert.equal(classify([file]).android, true);
  }
  assert.deepEqual(classify(['.github/workflows/ci.yml']), { shared: true, backend: true, android: true, docs: true, docker: true });
  assert.deepEqual(classify(['.github/dependabot.yml']), none);
});
test('I0-02: shared ejecuta shared, backend y android; backend solo backend', () => {
  assert.deepEqual(classify(['shared/build.gradle.kts']), { ...none, shared: true, backend: true, android: true, docker: true });
  assert.deepEqual(classify(['backend/src/main/kotlin/App.kt']), { ...none, backend: true, docker: true });
  assert.deepEqual(classify(['app/app/src/main/java/Main.kt']), { ...none, android: true });
  assert.deepEqual(classify(['backend/README.md', 'shared/README.md']), { ...none, docs: true });
});

test('I0-09: imagen y Compose ejecutan Docker, Markdown no', () => {
  for (const file of ['backend/Dockerfile', 'docker-compose.yml', '.dockerignore', '.github/scripts/check-docker-image.mjs']) {
    assert.equal(classify([file]).docker, true);
  }
  assert.equal(classify(['README.md']).docker, false);
});
test('ADR-26: rechaza usos nuevos y conserva solo excepciones vigentes', () => {
  assert.equal(timezoneErrors({ 'New.kt': 'LocalDate.now()' }, []).length, 1);
  assert.equal(timezoneErrors({ 'Old.kt': 'LocalDate.now()' }, ['Old.kt'], []).length, 1);
  assert.equal(timezoneErrors({ 'Old.kt': 'today(clock)' }, ['Old.kt']).length, 1);
  assert.deepEqual(timezoneErrors({ 'Old.kt': 'LocalDate.now()' }, ['Old.kt']), []);
  assert.deepEqual(timezoneErrors({ 'Old.kt': 'LocalDate.now(clock)' }, [], ['Old.kt']), []);
});
test('ADR-26: admite reloj explícito y centralización, rechaza otros relojes implícitos', () => {
  assert.deepEqual(timezoneErrors({
    'Screen.kt': 'LocalDate.now(clock)',
    'app/core/time/ArgentinaClock.kt': 'LocalDate.now()',
  }, []), []);
  for (const code of ['LocalTime.now()', 'LocalDateTime.now( )', 'ZonedDateTime.now()', 'OffsetDateTime.now()', 'ZoneId.systemDefault()', 'TimeZone.getDefault()']) {
    assert.equal(timezoneErrors({ 'Screen.kt': code }, []).length, 1);
  }
});
test('checksum: falla si falta o es inválido y acepta SHA-256 con CRLF', () => {
  const root = mkdtempSync(join(tmpdir(), 'pasitos-wrapper-'));
  try {
    mkdirSync(join(root, 'gradle/wrapper'), { recursive: true });
    const file = join(root, 'gradle/wrapper/gradle-wrapper.properties');
    for (const content of ['distributionUrl=https://example.invalid/gradle.zip', 'distributionSha256Sum=abc']) {
      writeFileSync(file, content);
      assert.equal(wrapperErrors(root).length, 1);
    }
    writeFileSync(file, 'distributionSha256Sum=' + 'a'.repeat(64) + '\r\n');
    assert.deepEqual(wrapperErrors(root), []);
  } finally { rmSync(root, { recursive: true, force: true }); }
});
