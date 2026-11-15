import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { classify, timezoneErrors, wrapperErrors } from './ci-checks.mjs';

test('I0-03: solo Markdown no compila Android, incluso dentro de app', () => {
  assert.deepEqual(classify(['app/README.md', 'CONTRIBUTING.md']), { android: false, docs: true });
});
test('I0-03: código Android, shared y controles ejecutan Android', () => {
  for (const file of ['app/app/build.gradle.kts', 'shared/src/Model.kt', '.github/scripts/ci-checks.mjs']) {
    assert.equal(classify([file]).android, true);
  }
  assert.deepEqual(classify(['.github/workflows/ci.yml']), { android: true, docs: true });
  assert.deepEqual(classify(['.github/dependabot.yml']), { android: false, docs: false });
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
