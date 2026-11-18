from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[4]


class IosAppStoreWorkflowTest(unittest.TestCase):
    def setUp(self):
        self.workflow = (ROOT / ".github/workflows/wc_ios_app_store.yml").read_text(encoding="utf-8")

    def test_packages_one_signed_iphone_and_ipad_app(self):
        for required in (
            "IOS_DISTRIBUTION_CERTIFICATE",
            "IOS_APP_STORE_PROFILE",
            "scripts/package-ios-app-store.sh",
            "dist/app-store/${APP_NAME}.ipa",
            "lipo -verify_arch arm64",
            "UIDeviceFamily:0",
            "UIDeviceFamily:1",
            "codesign --verify --deep --strict",
            "ios-app-store-package",
        ):
            with self.subTest(required=required):
                self.assertIn(required, self.workflow)

    def test_profile_and_signed_app_must_match_the_registered_identifier(self):
        self.assertIn('test "${profile_identifier}" = "${team_id}.${BUNDLE_ID}"', self.workflow)
        self.assertIn('test "${signed_identifier}" = "${TEAM_ID}.${BUNDLE_ID}"', self.workflow)
        self.assertIn('test "$(/usr/libexec/PlistBuddy -c \'Print :CFBundleIdentifier\' "${plist}")" = "${BUNDLE_ID}"', self.workflow)

    def test_requires_a_three_component_store_version(self):
        self.assertIn("Store version must contain three numeric components.", self.workflow)
        self.assertIn("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$", self.workflow)


if __name__ == "__main__":
    unittest.main()
