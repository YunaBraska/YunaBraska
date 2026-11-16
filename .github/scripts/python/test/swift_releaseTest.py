import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[4]
REVISION = "a" * 40
VERSION = "2026.11.16"

# GitHub is the external boundary. Execute the real publisher shell, including
# its credential selection and cleanup, against a small stateful gh substitute.
GH = r'''
import json, os, pathlib, sys
state_file = pathlib.Path("state.json")
state = json.loads(state_file.read_text())
args = sys.argv[1:]
token = os.environ.get("GH_TOKEN", "")
state["calls"].append({"args": args, "token": token})
def finish(code=0, error=""):
    state_file.write_text(json.dumps(state))
    if error:
        print(error, file=sys.stderr)
    sys.exit(code)
if args[:2] == ["release", "view"]:
    finish(1, "HTTP 404: release not found")
if args[0] == "api":
    finish(0 if state["tag"] else 1)
if token != state["accepted_token"]:
    finish(1, "HTTP 403: Resource not accessible by integration")
if args[:2] == ["release", "create"]:
    assert args[2] == "2026.11.16"
    assert args[args.index("--target") + 1] == "a" * 40
    assert "--draft" in args
    state["draft"] = True
    state["tag"] = True
elif args[:2] == ["release", "upload"]:
    if state["fail_upload"]:
        finish(1, "Upload failed")
    assert all(pathlib.Path("dist", name).is_file() for name in ["app.zip", "app.dmg", "appcast.xml"])
    assert all("dist/" + name in args for name in ["app.zip", "app.dmg", "appcast.xml"])
    state["uploaded"] = True
elif args[:2] == ["release", "edit"]:
    assert state["uploaded"] and "--draft=false" in args
    state["published"] = True
    state["draft"] = False
elif args[:2] == ["release", "delete"]:
    state["draft"] = False
    if "--cleanup-tag" in args:
        state["tag"] = False
else:
    finish(2, "Unexpected gh command")
finish()
'''


class SwiftReleaseTest(unittest.TestCase):
    def publish(self, custom_token="", historical=False, fail_upload=False, existing_tag=False):
        caller = (ROOT / ".github/workflows/wc_swift_release.yml").read_text()
        publisher = (ROOT / ".github/workflows/wc_swift_create_github_release.yml").read_text()
        release_job = caller.split("\n  release:\n", 1)[1]
        forwarded = re.search(r"^      RELEASE_TOKEN: \$\{\{ secrets.RELEASE_TOKEN \}\}$", release_job, re.MULTILINE)
        values = {
            "github.token": "github-token",
            "secrets.RELEASE_TOKEN": custom_token if forwarded else "",
            "github.repository": "example/app",
            "inputs.version": VERSION,
            "inputs.commit_sha": REVISION,
        }

        def render(value):
            def expression(match):
                return next((values[key.strip()] for key in match[1].split("||") if values[key.strip()]), "")
            return re.sub(r"\$\{\{ (.*?) \}\}", expression, value)

        step = publisher.split('      - name: "🏷️ GitHub Release', 1)[1]
        script = textwrap.dedent(step.split("        run: |\n", 1)[1])
        environment = {"PATH": os.environ["PATH"]}
        if "        env:\n" in step:
            block = step.split("        env:\n", 1)[1].split("        run:", 1)[0]
            for line in block.splitlines():
                key, value = line.strip().split(": ", 1)
                environment[key] = render(value)
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            gh = directory / "gh"
            gh.write_text(f"#!{sys.executable}\n" + textwrap.dedent(GH))
            gh.chmod(0o755)
            environment["PATH"] = str(directory) + os.pathsep + environment["PATH"]
            (directory / "dist").mkdir()
            for name in ("app.zip", "app.dmg", "appcast.xml"):
                (directory / "dist" / name).write_text("tested artifact")
            state_file = directory / "state.json"
            state_file.write_text(json.dumps({
                "accepted_token": "publisher-token" if historical else "github-token",
                "tag": existing_tag, "draft": False, "published": False,
                "uploaded": False, "calls": [], "fail_upload": fail_upload,
            }))
            result = subprocess.run(["/bin/sh", "-c", render(script)], cwd=directory,
                                    env=environment, text=True, capture_output=True, timeout=10)
            state = json.loads(state_file.read_text())
        self.assertNotIn("publisher-token", result.stdout + result.stderr)
        self.assertNotIn("expired-token", result.stdout + result.stderr)
        return result, state

    def test_historical_revision_uses_existing_publisher_token(self):
        result, state = self.publish("publisher-token", historical=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(state["published"])
        self.assertEqual({call["token"] for call in state["calls"]}, {"publisher-token"})

    def test_default_token_still_publishes_ordinary_revision(self):
        result, state = self.publish()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(state["published"])

    def test_rejected_publisher_does_not_fall_back(self):
        result, state = self.publish("expired-token")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(state["published"])
        self.assertEqual({call["token"] for call in state["calls"]}, {"expired-token"})

    def test_upload_failure_cleans_own_draft_and_tag_with_publisher_token(self):
        result, state = self.publish("publisher-token", historical=True, fail_upload=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(state["published"])
        self.assertFalse(state["draft"])
        self.assertFalse(state["tag"])
        self.assertEqual(state["calls"][-1]["args"][:2], ["release", "delete"])
        self.assertEqual(state["calls"][-1]["token"], "publisher-token")

    def test_upload_failure_keeps_preexisting_tag(self):
        result, state = self.publish("publisher-token", historical=True, fail_upload=True, existing_tag=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(state["tag"])
        self.assertFalse(state["draft"])
        self.assertEqual(state["calls"][-1]["args"][:2], ["release", "delete"])
        self.assertEqual(state["calls"][-1]["token"], "publisher-token")
        self.assertNotIn("--cleanup-tag", state["calls"][-1]["args"])


if __name__ == "__main__":
    unittest.main()
