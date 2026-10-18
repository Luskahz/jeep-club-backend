#!/usr/bin/env python3
"""Security and version-gating checks for the portable VPS snapshot."""
import importlib.util
from pathlib import Path
import tempfile
import unittest


HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("snapshot_vps", HERE / "snapshot-vps.py")
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)


class SnapshotTests(unittest.TestCase):
    def test_metadata_names_cannot_embed_personal_data_or_markup(self):
        self.assertEqual(snapshot.safe_name("V4__safe_migration.sql"), "V4__safe_migration.sql")
        for unsafe in ("person@example.com", "<script>", "name\nsecret", "a" * 129):
            self.assertEqual(snapshot.safe_name(unsafe), "[redacted]")

    def test_v3_reference_is_never_reused_for_v4(self):
        with tempfile.TemporaryDirectory() as root:
            reference = Path(root, "reference")
            actual = Path(root, "actual")
            reference.mkdir()
            actual.mkdir()
            match, differences, version = snapshot.reference_comparison(
                str(reference), str(actual), str(HERE), latest_version=4, reference_version=3)
            self.assertIsNone(match)
            self.assertEqual(version, 3)
            self.assertTrue(differences)

    def test_matching_v4_reference_is_supported(self):
        with tempfile.TemporaryDirectory() as root:
            reference = Path(root, "reference")
            actual = Path(root, "actual")
            reference.mkdir()
            actual.mkdir()
            for name in ("tables.tsv", "columns.tsv", "indexes.tsv", "constraints.tsv", "keys.tsv",
                         "foreign-keys.tsv", "checks.tsv", "views.tsv", "triggers.tsv", "routines.tsv",
                         "events.tsv", "table-options.tsv", "index-options.tsv", "view-definitions.tsv"):
                (reference / name).write_text("")
                (actual / name).write_text("")
            match, differences, version = snapshot.reference_comparison(
                str(reference), str(actual), str(HERE), latest_version=4, reference_version=4)
            self.assertTrue(match)
            self.assertEqual(differences, [])
            self.assertEqual(version, 4)


if __name__ == "__main__":
    unittest.main()
