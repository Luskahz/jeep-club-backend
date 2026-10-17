#!/usr/bin/env python3
"""Run the real deployment state machine with a Docker double; no server access."""
import os
import pathlib
import subprocess
import tempfile
import unittest

SOURCE = pathlib.Path(__file__).with_name('vps-deploy.sh').read_text()
SHA = 'a' * 40

class DeploySafetyTest(unittest.TestCase):
    def run_deploy(self, failure=''):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = pathlib.Path(temp.name)
        state = root / 'state'; state.mkdir()
        env_file = root / '.env'; env_file.write_text('BACKEND_IMAGE=old-image\n')
        (state / 'current-commit').write_text('old-sha\n')
        (state / 'current-image').write_text('old-image\n')
        release = state / f'release-{SHA}-test'; (release / 'deploy').mkdir(parents=True)
        runtime = SOURCE.replace('/var/lib/jeep-club-deploy', str(state)).replace('/opt/jeep-club-compose/.env', str(env_file))
        (release / 'deploy/vps-deploy.sh').write_text(runtime)
        (release / 'deploy/vps-backup.sh').write_text('echo BACKUP >> "$TRACE"\n[[ $FAILURE != backup ]]\n')
        docker = root / 'docker'
        docker.write_text('''#!/bin/bash
printf '%s\\n' "$*" >> "$TRACE"
if [[ $1 == exec ]]; then [[ $FAILURE == history ]] && echo 0 || echo 1; exit 0; fi
if [[ $1 == compose ]]; then
 for arg in "$@"; do
  if [[ $arg == build && $FAILURE == build || $arg == up && $FAILURE == health ]]; then exit 1; fi
 done
fi
''')
        docker.chmod(0o700)
        trace = root / 'trace'
        env = dict(os.environ, PATH=str(root)+':'+os.environ['PATH'], TRACE=str(trace), FAILURE=failure)
        result = subprocess.run(['bash', '-c', 'exec 9>"$1/deploy.lock"; flock 9; bash "$2/deploy/vps-deploy.sh" "$3" "$2"', '_', str(state), str(release), SHA], env=env, text=True, capture_output=True)
        return result, state, trace.read_text(), env_file

    def test_success_persists_only_after_backup_and_health(self):
        result, state, trace, env = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertLess(trace.index('build backend'), trace.index('BACKUP'))
        self.assertLess(trace.index('BACKUP'), trace.index('up -d'))
        self.assertEqual((state/'current-commit').read_text().strip(), SHA)
        self.assertIn(SHA, env.read_text())

    def test_preflight_and_build_failures_leave_live_backend_alone(self):
        for failure in ('history', 'build'):
            with self.subTest(failure=failure):
                result, state, trace, env = self.run_deploy(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertNotIn('stop backend', trace)
                self.assertNotIn('BACKUP', trace)
                self.assertEqual((state/'current-commit').read_text(), 'old-sha\n')

    def test_cutover_failure_stops_backend_without_rollback_or_success_state(self):
        for failure in ('backup', 'health'):
            with self.subTest(failure=failure):
                result, state, trace, env = self.run_deploy(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn('stop backend', trace)
                self.assertLessEqual(trace.count('up -d'), 1)
                self.assertEqual((state/'current-commit').read_text(), 'old-sha\n')
                self.assertEqual(env.read_text(), 'BACKEND_IMAGE=old-image\n')
                self.assertTrue((state/'last-failure').exists())
                self.assertIn('no automatic image/schema rollback', result.stderr)

if __name__ == '__main__': unittest.main()
