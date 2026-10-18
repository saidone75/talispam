# Changelog

## Unreleased

**[compare](https://github.com/saidone75/talispam/compare/v0.4.3...HEAD)**

### Added

- `talispam whitelist compact` accepts `A` to approve the current and all remaining domains without further prompts.

- `talispam whitelist clean` removes addresses containing `bounce` (including `bounces`) or a UUID, ignoring case, and saves a backup before changes.

## 0.4.3 (2026-10-18)

**[compare](https://github.com/saidone75/talispam/compare/v0.4.2...v0.4.3)**

### Added

- `talispam db info` reports training ham/spam message counts, the number of distinct words and the database file size in bytes.

- `talispam whitelist compact` shows example senders and asks before merging each exact domain, normalizes PRVS senders, and saves a backup before changes.

### Changed

- Replace `talispam print-db` with `talispam db print`, preserving the existing output.

- Include the project version in GraalVM executable filenames for local and Docker builds (for example, `talispam-0.4.2`).

### Fixed

- Compute `db print` word probabilities directly from stored training counts, avoiding neutral scores caused by an unloaded dictionary.

- Match explicit `@domain` whitelist entries literally against the exact host, and recognize changing PRVS tokens when checking senders.

## 0.4.2 (2026-10-17)

**[compare](https://github.com/saidone75/talispam/compare/v0.4.1...v0.4.2)**

### Added

- `talispam init` creates the default configuration, an empty whitelist and ham/spam directories, and downloads the English and Italian dictionaries from `talispam-config`; existing files are preserved.
- `talispam whitelist ham` merges senders from the configured ham directories and mbox files into the saved whitelist, without duplicates.
- `talispam whitelist edit` opens the whitelist with `$VISUAL`, `$EDITOR` or `vi`.
- `talispam whitelist print` prints the saved whitelist.
- Docker build scripts support `--no-cache`.

### Changed

- Replaced the previous `talispam whitelist` command with the `ham`, `edit` and `print` subcommands.
- Migrated from Leiningen to Clojure CLI and `tools.build` for tests, uberjars and native executables, including Docker builds and CI.
- Updated build dependencies and the `clj-holmes` action, and clarified Clojure CLI installation instructions.

### Fixed

- Load the whitelist on the first sender check.
- Ignore blank whitelist entries and handle messages without a recognized sender.
- Whitelist management commands no longer load the dictionary or require a trained classifier database.

## 0.4.1 (2026-10-10)

**[compare](https://github.com/saidone75/talispam/compare/v0.4.0...v0.4.1)**

- Allow CLI help without loading configuration or dictionaries.
- Always stop progress spinners, including when an operation fails.
- Read project metadata with the safe EDN reader.
- Optimize GraalVM builds for size by default and update Docker Compose configuration.
- Remove the obsolete `--no-fallback` native-image option.

## 0.4.0 (2026-10-04)

**[compare](https://github.com/saidone75/talispam/compare/v0.3.0...v0.4.0)**

- Improve MIME parsing, including multipart messages, encoded subjects, base64 and quoted-printable content; exclude attachments.
- Make parallel training repeatable and scoring stable for large messages, counting each word once per message.
- Add Docker builds with GraalVM and Leiningen, exporting the Linux native executable to `dist/talispam`.
- Replace `lein-shell` with `lein-native-image` for native builds.
- Remove the obsolete test that depended on local configuration, dictionaries, databases and corpus files.

## 0.3.0 (2020-06-17)

**[compare](https://github.com/saidone75/talispam/compare/v0.2.1...v0.3.0)**

- Replace `tools.cli` with `cli-matic`, providing command-specific help and option validation.
- Speed up classifier training by processing the corpus in parallel.
- Show progress spinners during training and mailbox statistics calculation.
- Rework message parsing and limit sender address extraction to the header section.
- Prepare database and dictionary serialization for MessagePack through Transit; the release continues to use Transit JSON by default.
- Read the application version from project metadata instead of maintaining a separate hard-coded version string.
- Improve GraalVM compatibility with reflection configuration, direct linking and an updated Clojure dependency.
- Validate mbox arguments and avoid exceptions when checking empty files.

## 0.2.1 (2020-05-27)

**[compare](https://github.com/saidone75/talispam/compare/v0.2.0...v0.2.1)**

- Add initial support for extracting words from base64-encoded message bodies.
- Include the subject in classification and strip HTML tags from message content.
- Add `print-db` to inspect words in the classifier database, ordered by spam score.
- Add `stats` to calculate a summary of message scores for an mbox file, with validation of the mailbox argument.
- Add the original `whitelist` command to extract and print sender addresses from the ham corpus.
- Support an optional whitelist of trusted addresses, hosts and domains, marking matching messages in the spam flag header.
- Make the spam threshold configurable, with a default of 70, and output the numeric score in `X-Spam-Score`.
- Handle missing subjects and parsing failures more defensively.

## 0.2.0 (2020-05-20)

**[compare](https://github.com/saidone75/talispam/compare/v0.1.0...v0.2.0)**

- Allow classifier training from mbox files as well as directories of individual messages, for both ham and spam corpora.
- Expand `~` to the user's home directory in configuration paths, making configurations portable between users and machines.
- Persist the dictionary using Transit JSON and load it on subsequent runs instead of rebuilding it for each invocation.
- Build and save the dictionary automatically from the configured word lists when no persisted dictionary exists.

## 0.1.0 (2020-05-18)

- Introduce the Bayesian mail filter, with `learn` to train and save a classifier from ham and spam corpora.
- Add `score` to read a message from standard input and print its ham/spam score.
- Support invocation without a command to return the message with spam identification headers, preserving its first line for mbox compatibility.
- Load runtime settings from `~/.talispam/talispam.cfg.edn`, including corpus, dictionary and classifier database locations.
- Support optional dictionary-based word filtering.
- Support GraalVM native executable builds and document integration with procmail.
