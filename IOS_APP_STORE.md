# iPhone and iPad App Store CI/CD

The iPhone and iPad channel shares the stable-release boundary with direct downloads and macOS Store packages. A snapshot does not create an IPA, upload a build, tag a release or update a product page.

```mermaid
flowchart LR
  R[Stable release version] --> S[Apple Distribution certificate and profile]
  S --> A[Product adapter builds a signed IPA]
  A --> C[Shared identifier, version, architecture and signature checks]
  C --> P[One-day IPA artifact]
  P --> U[Shared App Store Connect upload]
```

The package workflow validates one signed arm64 IPA. It requires the registered bundle identifier, the stable marketing version, the GitHub run build number, an embedded Store profile and exactly the iPhone and iPad device families. The existing `wc_macos_app_store_upload.yml` uploads the IPA and does not submit App Review.

## Product contract

Each Store-ready product calls `wc_ios_app_store.yml` with its stable version and registered bundle ID. It supplies only this executable adapter:

```sh
scripts/package-ios-app-store.sh "$APP_STORE_MARKETING_VERSION" "$APP_STORE_BUILD_NUMBER"
```

The shared workflow provides `APP_STORE_BUNDLE_ID`, `APP_STORE_TEAM_ID`, `IOS_APP_STORE_DISTRIBUTION_IDENTITY`, `IOS_APP_STORE_PROFILE_NAME`, `IOS_APP_STORE_PROFILE_PATH` and `IOS_APP_STORE_PROFILE_UUID`. The adapter creates exactly:

```text
dist/app-store/<App>.ipa
```

Product code owns its toolchain, app resources, runtime tests and platform-specific entitlements. The reusable workflow owns certificate import, profile matching, final IPA validation, artifact retention and upload compatibility.

## Secrets

Each repository uses its own Base64 values. A provisioning profile is tied to one bundle ID and must never be shared across products.

| Secret | Content |
| --- | --- |
| `IOS_DISTRIBUTION_CERTIFICATE` | PKCS#12 export of Apple Distribution. |
| `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD` | Password for that export. |
| `IOS_APP_STORE_PROFILE` | App Store provisioning profile for the product identifier. |

The existing `MACOS_NOTARY_KEY`, `MACOS_NOTARY_KEY_ID` and `MACOS_NOTARY_ISSUER_ID` App Store Connect API key is reused by `wc_macos_app_store_upload.yml`. No second upload credential is needed. Uploading produces a processed build. TestFlight invitations, App Store metadata and App Review submission remain separate product actions.
