import contextlib
import inspect
import io
from pathlib import Path
import re
import unittest
from unittest.mock import patch

import verify


class ReleaseTargetTests(unittest.TestCase):
    def test_released_minecraft_lines_keep_their_runtime(self):
        for java, versions in {
            8: ("1.10.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2", "1.16.5"),
            16: ("1.17.1",),
            17: ("1.18.2", "1.19.4", "1.20.1"),
            21: ("1.20.6", "1.21.1", "1.21.11"),
            25: ("26.1.2", "26.2"),
        }.items():
            for version in versions:
                with self.subTest(version=version):
                    self.assertEqual((version, f"Java {java}"), verify.release_target(f"master-{version}"))

    def test_unqualified_or_malformed_targets_are_rejected(self):
        for branch in ("master-26.3", "master-26.2.0", "master-26.1.1", "master-26.1.3",
                       "master-1.21.12", "master-26.1.2-extra", "feature/26.1.2",
                       "26.1.2", "master-main", "master-26.1.2\n"):
            with self.subTest(branch=branch), self.assertRaises(ValueError):
                verify.release_target(branch)

    def test_missing_java25_entry_is_rejected(self):
        source = inspect.getsource(verify.release_target)
        marker = '"26.1.2": 25'
        self.assertIn(marker, source)
        mutated = source.replace(marker + ",", "")
        namespace = {"require": verify.require}
        exec(compile(mutated, "<missing-java25-control>", "exec"), namespace)
        with self.assertRaisesRegex(ValueError, "Unsupported Minecraft version"):
            namespace["release_target"]("master-26.1.2")

    def test_target_cli_has_no_publication_side_effects(self):
        output = io.StringIO()
        with patch.object(verify.sys, "argv", ["verify.py", "target", "2.2.16.121111", "unused"]), \
                patch.object(verify, "github") as github, patch.object(verify, "read_url") as read_url, \
                patch.object(verify, "stage") as stage, contextlib.redirect_stdout(output):
            verify.main()
        self.assertEqual("minecraft=1.21.11\njava=Java 21\n", output.getvalue())
        github.assert_not_called()
        read_url.assert_not_called()
        stage.assert_not_called()

    def test_target_cli_requires_an_approved_version(self):
        with patch.object(verify.sys, "argv", ["verify.py", "target", "0.0.0.9999999", "unused"]), \
                self.assertRaisesRegex(ValueError, "Version is not approved"):
            verify.main()

    def test_dispatcher_retains_live_publication_guards(self):
        workflow = (Path(__file__).resolve().parent.parent / "workflows/deploy-release.yml").read_text()
        for marker in ("default: check-only", "github.event.repository.default_branch",
                       "inputs.mode == 'publish' && inputs.existing_curseforge_file_id == ''",
                       "inputs.mode == 'publish' && steps.preflight.outputs.state == 'absent'",
                       "Do not retry an upload.", "https://maven.moddev.zone/releases",
                       'verify-curseforge "$RELEASE_VERSION" "$RELEASE_WORK"',
                       'require-absent "$RELEASE_VERSION" "$RELEASE_WORK"'):
            self.assertIn(marker, workflow)
        self.assertIn('verify.py target "$RELEASE_VERSION" "$release_work"', workflow)
        self.assertNotIn('capture("^master-', workflow)
        self.assertIn("python3 -m unittest discover", workflow)
        target = re.search(r'--verify-tag --target "\$(target|source_commit|RELEASE_SOURCE_COMMIT)"',
                           workflow)
        self.assertIsNotNone(target)
        guards = ('[[ "$' + target.group(1) + '" =~ ^[' + chars + ']{40}$ ]]'
                  for chars in ("a-f0-9", "0-9a-f"))
        self.assertTrue(any(guard in workflow for guard in guards))

    def test_ci_and_tag_provenance_stay_sealed(self):
        source = Path(verify.__file__).read_text()
        for marker in ('ci["head_sha"] == release["commit"]',
                       'ci["event"] == "push"', 'ci["head_branch"] == release["branch"]',
                       'ref["sha"] == release["commit"]',
                       'details["digest"] == "sha256:" + release["archiveSha256"]',
                       'sums[jars[0]] == release["mainSha256"]',
                       'bundle.namelist().count(member) == 1'):
            self.assertIn(marker, source)


if __name__ == "__main__":
    unittest.main()

