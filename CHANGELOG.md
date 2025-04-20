# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] - 2025-04-20

### BREAKING

- Drop support for Java 8 and Clojure 1.9. [#10](https://github.com/totakke/cavia/pull/10)

### Added

- Support SHA512 hash algorithm (`:sha512`). [9d1d141](https://github.com/totakke/cavia/commit/9d1d141)
- Add specs. [#9](https://github.com/totakke/cavia/pull/9)

### Changed

- Migrate digest to clj-commons/digest. [3dc4fef](https://github.com/totakke/cavia/commit/3dc4fef)
- Migrate jcraft/jsch to mwiede/jsch. [ff3dbba](https://github.com/totakke/cavia/commit/ff3dbba)
- Use clj-http-lite instead of clj-http. [6cc4005](https://github.com/totakke/cavia/commit/6cc4005)
- Update aws-api to 0.8.735. [#10](https://github.com/totakke/cavia/pull/10)

### Fixed

- Fix a document of verbosity. [52873b1](https://github.com/totakke/cavia/commit/52873b1)

## [0.7.2] - 2025-02-21

### Added

- Add an arity for getting a single resource. [ca2f078](https://github.com/totakke/cavia/commit/ca2f078)

### Changed

- Update clojure to 1.12.0. [457963a](https://github.com/totakke/cavia/commit/457963a)
- Update dependencies except for aws-api. [0042956](https://github.com/totakke/cavia/commit/0042956)

### Fixed

- Fix deprecated calls. [4d0faae](https://github.com/totakke/cavia/commit/4d0faae)

## [0.7.1] - 2024-06-23

### Security

- Bump up commons-compress to 1.26.2. [#8](https://github.com/totakke/cavia/pull/8)
  - Because of CVE-2024-25710 and CVE-2024-26308.
- Update clojure to 1.11.3.
  - Because of CVE-2024-22871.

## [0.7.0] - 2024-02-12

### Changed

- Change license to the MIT License.
- Separate verbosity type.

## [0.6.2] - 2022-06-21

### Added

- Support S3 protocol.

## [0.6.1] - 2022-04-22

### Added

- Add resume option to FTP downloader.
- Support OAuth2 token.

### Changed

- Allow to pass true to resume option.
- Enable resume for cavia.core/get!.

## [0.6.0] - 2021-05-18

### Added

- Add resume option to http downloader.

### Changed

- Increase default buffer size for downloading (1024 bytes -> 4096 bytes).

### Removed

- Drop support for clojure 1.7 and 1.8.

## [0.5.1] - 2018-05-26

### Added

- Support SFTP protocol.

### Changed

- Move FTP client code to another ns.
- Add thread interruption to download func.

### Fixed

- Fix integer coercion.

## [0.5.0] - 2018-03-26

### Added

- bzip2 decompression support.

### Changed

- Change URL library: cemerick/url to lambdaisland/uri.
- Use commons-compress for decompression.
- Update clj-http to 3.8.0.
- Use mock FTP server for test.
- Make default FTP user "anonymous".

### Removed

- Drop clojure 1.6 support.

### Fixed

- Fix FTPS download.
- Fix FTP port parsing.
- Fix for FTP server not supporting MLST.

## [0.4.3] - 2017-11-15

### Changed

- Ignore failure of FTP MLST command. (by @alumi) [#6](https://github.com/totakke/cavia/pull/6)

## [0.4.2] - 2017-10-20

### Changed

- Support Java 9. (by @alumi) [#5](https://github.com/totakke/cavia/pull/5)
  - Use clj-http instead of clj-http-lite to avoid `ClassNotFoundException`.
- Update clj-digest to 1.4.6.

### Fixed

- Fix profile used in `clean!`.

## [0.4.1] - 2017-06-07

### Changed

- Remove dependency on raynes/fs.

### Fixed

- Fix a bug of verification.

## [0.4.0] - 2017-03-13

### Added

- Add hash algorithms: MD5 and SHA256.

### Changed

- Print alerts to stderr.
- Bump commons-net version up to 3.6.
- Bump progrock version up to 0.1.2.

## [0.3.1] - 2017-02-10

### Changed

- Bump clj-digest version up to 1.4.5.
- Improve hashing performance.

## [0.3.0] - 2016-08-10

### Added

- Add a function to automatically decompress GZIP resources. (by @federkasten) [#4](https://github.com/totakke/cavia/pull/4)

### Changed

- Use clj-digest instead of pandect.

## [0.2.3] - 2016-05-17

### Changed

- Bump commons-net version up to 3.5.

## [0.2.2] - 2016-02-13

### Changed

- Bump commons-net version up to 3.4.

## [0.2.1] - 2015-10-17

### Changed

- Bump pandect version up to 0.5.4.

### Fixed

- Add type hints.

## [0.2.0] - 2015-09-05

### Changed

- Use com.cemerick/url instead of clojurewerks/urly.
- Stop using clj-ftp.
- Use clj-http-lite instead of clj-http.
- Use progrock for printing progress.
- Bump up pandect version to v0.5.3.

## [0.1.5] - 2015-07-14

### Changed

- Bump dependencies version up.

## [0.1.4] - 2015-03-13

### Changed

- Bump dependencies version up.

## [0.1.3] - 2014-05-20

### Changed

- Bump pandect version up to 0.3.2.

### Fixed

- Avoid reflection.

## [0.1.2] - 2014-03-28

### Added

- Add some docstrings.

### Changed

- Bump up versions of pandect and clj-http.

### Fixed

- Fix error when `Content-Length` not found. [#2](https://github.com/totakke/cavia/issues/2)

## 0.1.1 - 2014-01-28

First release.

[Unreleased]: https://github.com/totakke/cavia/compare/v0.8.0...HEAD
[0.8.0]: https://github.com/totakke/cavia/compare/v0.7.2...v0.8.0
[0.7.2]: https://github.com/totakke/cavia/compare/v0.7.1...v0.7.2
[0.7.1]: https://github.com/totakke/cavia/compare/v0.7.0...v0.7.1
[0.7.0]: https://github.com/totakke/cavia/compare/0.6.2...v0.7.0
[0.6.2]: https://github.com/totakke/cavia/compare/0.6.1...0.6.2
[0.6.1]: https://github.com/totakke/cavia/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/totakke/cavia/compare/0.5.1...0.6.0
[0.5.1]: https://github.com/totakke/cavia/compare/0.5.0...0.5.1
[0.5.0]: https://github.com/totakke/cavia/compare/0.4.3...0.5.0
[0.4.3]: https://github.com/totakke/cavia/compare/0.4.2...0.4.3
[0.4.2]: https://github.com/totakke/cavia/compare/0.4.1...0.4.2
[0.4.1]: https://github.com/totakke/cavia/compare/0.4.0...0.4.1
[0.4.0]: https://github.com/totakke/cavia/compare/0.3.1...0.4.0
[0.3.1]: https://github.com/totakke/cavia/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/totakke/cavia/compare/0.2.3...0.3.0
[0.2.3]: https://github.com/totakke/cavia/compare/0.2.2...0.2.3
[0.2.2]: https://github.com/totakke/cavia/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/totakke/cavia/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/totakke/cavia/compare/0.1.5...0.2.0
[0.1.5]: https://github.com/totakke/cavia/compare/0.1.4...0.1.5
[0.1.4]: https://github.com/totakke/cavia/compare/0.1.3...0.1.4
[0.1.3]: https://github.com/totakke/cavia/compare/0.1.2...0.1.3
[0.1.2]: https://github.com/totakke/cavia/compare/0.1.1...0.1.2
