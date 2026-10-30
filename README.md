# Private Folder

A private local vault for securing photos and videos on Android.

## Automated Android releases

Every push to `main` and every manual workflow run executes unit tests, builds the release APK, uploads it as a workflow artifact, and publishes a GitHub Release named **Private Folder**.

For a persistent signing identity, configure repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD`. Without them, the workflow generates a temporary signing key for test releases.
