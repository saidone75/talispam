# Talispam
A program held to act as a charm to avert spam and bring good messages.

[![talispam.png](https://i.postimg.cc/KYZyT1M7/talispam.png)](https://postimg.cc/tsmfQCD7)

## Very quick roundtrip
### Build
#### Clojure CLI commands

| Command | Purpose |
| --- | --- |
| `clojure` | Start a REPL. |
| `clojure -M:run --help` | Run the application from source. |
| `clojure -M:test` | Run all tests; exit with a nonzero status on failure. |
| `clojure -T:build uber` | Build `target/uberjar/talispam-0.5.0-standalone.jar` with AOT compilation. |
| `clojure -T:build native-image` | Build the uberjar and then the native executable `target/talispam-0.5.0` (requires GraalVM). |
| `clojure -T:build clean` | Remove `target/` and its build artifacts. |

#### Local build

Requires Java (JDK 25 is used in CI) and the official [Clojure CLI](https://clojure.org/guides/install_clojure).
Verify the CLI with `clojure -Sdescribe`; a plain Clojure language launcher does not support `-M` or `-T`.

Clone, test and build the executable uberjar:
```console
$ git clone https://github.com/saidone75/talispam.git
$ cd talispam
$ clojure -M:test
$ clojure -T:build uber
$ java -jar target/uberjar/talispam-0.5.0-standalone.jar --help
```

For a native executable, install GraalVM with `native-image` on `PATH`, then run:
```console
$ clojure -T:build native-image
```
Copy `target/talispam-0.5.0` to a directory on your `PATH` as `talispam`.
Optional build parameters: `:optimization "b" :march "native"`.
Artifact filenames use the version in `resources/talispam.edn`.

### Build with Docker (GraalVM)
Only Docker with Compose and BuildKit is required on the host. The build uses
the GraalVM version specified in [docker/Dockerfile](docker/Dockerfile)
and Clojure CLI with tools.build to compile an uberjar and then a native executable.

From the repository root, on Windows:
```bat
build-docker.bat
```
On Linux/macOS:
```console
$ ./build-docker.sh
```
Both scripts run the build directly without arguments and produce `dist/talispam-0.5.0`, a **Linux** executable.
The filename uses the version in `resources/talispam.edn`.
Copy it to the target Linux machine as `talispam` and configure `~/.talispam` as described below.

Pass `--no-cache` to rebuild all Docker image layers without reusing the layer cache:
```console
$ ./build-docker.sh --no-cache
```
On Windows, use `build-docker.bat --no-cache`.
Dependency downloads stored in BuildKit cache mounts are still reused.
Use `--help` to display the available options.

The default native build uses `-Os` to optimize executable size and
`-march=compatibility` so the executable can run on older CPUs.

Docker builds also accept these environment variables:

```console
# Optimize for the CPU of the build machine (use only on compatible target CPUs).
$ NATIVE_MARCH=native ./build-docker.sh
# Faster development builds, with fewer compiler optimizations.
$ NATIVE_OPTIMIZATION=b ./build-docker.sh
# Favor performance over executable size.
$ NATIVE_OPTIMIZATION=2 ./build-docker.sh
```

On Windows, set the variables with `SET` before running `build-docker.bat`.
GraalVM Community treats `-O3` the same as `-O2`; profile-guided optimization
requires Oracle GraalVM. See the [GraalVM optimization guide](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/).

### Configuration
Create the default configuration in `~/.talispam`:
```console
$ talispam init
```
This downloads `talispam.cfg.edn`, `en_20k.txt` and `it_30k.txt` from
[talispam-config](https://github.com/saidone75/talispam-config), creates an empty
`whitelist.txt` and the configured ham/spam directories. Existing files are
preserved; rerunning the command installs missing files. Internet access is
required for downloads. The training corpus is not downloaded: populate the
ham/spam directories or set `:training-corpus` in the configuration to your
own `:ham-dirs`, `:spam-dirs`, `:ham-mboxes` and `:spam-mboxes` before training.

Manage trusted senders with:
```console
$ talispam whitelist ham
$ talispam whitelist clean
$ talispam whitelist compact
$ talispam whitelist edit
$ talispam whitelist print
```
`ham` scans the configured ham directories and mbox files, merges their
senders with the existing whitelist, and saves a sorted list without duplicates.
`compact` interactively proposes merging senders into `@exact-host` entries.
Only hosts with at least two distinct addresses after PRVS normalization are
proposed. For each host it shows the address count, up to three examples and the proposed
entry. Answer `y` to trust all senders on that exact host (excluding subdomains),
or `n` / Enter to keep individual addresses. Answer `A` (or `a` / `all`) to approve
the current host and all remaining hosts without further questions; earlier
rejections are preserved. This also applies to shared providers such as Gmail or Amazon SES; there is no automatic
provider classification. Already whitelisted exact hosts are skipped.
`q` or end of input cancels the operation without saving any changes.
PRVS prefixes (`prvs=token=` and `msprvs1=token=`) are normalized automatically,
retaining the underlying address; ordinary plus-address tags are preserved.
Duplicates and addresses covered by explicit `@exact-host` entries are removed.
All changes are saved after the questions, with a uniquely named `.bak` backup
beside the whitelist. Run it after `whitelist ham` to compact new addresses.
Sender checks also normalize PRVS tokens so future token variants still match.
`clean` removes email addresses containing `bounce` (including `bounces`) or a
UUID in hexadecimal `8-4-4-4-12` format, ignoring case. Explicit `@host` entries
and other whitelist patterns are preserved. It saves a backup beside the
whitelist before changes and reports the number of removed addresses.

`edit` opens the configured whitelist file using `$VISUAL`, then `$EDITOR`,
falling back to `vi`; editor arguments are supported. `print` prints the saved
entries without scanning the corpus. These commands do not need a trained database
or compiled dictionary. Whitelist matching is enabled by `:whitelist :use`;
the file path is set by `:whitelist :location`.
Sender matching reads the address from the leading mbox `From ` envelope line;
a message containing only an RFC `From:` header does not match the whitelist.
Whitelist matches leave the numeric score unchanged and set `X-Spam-Flag` to
`*** sender whitelisted ***`.

Train the Bayesian classifier:
```console
$ talispam learn
building classifier db ✓
done!
```
### Classify messages
Print the spam score (0–100; lower scores indicate ham):
```console
$ cat ~/.talispam/easy_ham/02051.58e196144807bd76d7b77d4b7efb6d32 | talispam score
14
$ cat ~/.talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam score
98
```
Inspect why a message receives its classification:
```console
$ talispam explain < message.eml
```
`explain` reports the score on the same 0–100 scale as `score`, the configured
threshold (default 70), whitelist status (`disabled`, `matched`, or `not matched`),
and the resulting ham/spam classification. A message is spam when its rounded
score exceeds the threshold and its sender is not whitelisted.
It lists each distinct recognized word alphabetically, with ham/spam training
message counts and its smoothed Bayesian spam probability on a 0–1 scale.
The same MIME parsing and optional dictionary filtering used by `score` apply;
unknown words are omitted. With no recognized words, the score is 50.
Word probabilities are clues, not exact additive contributions to the Fisher score.

### Compatibility
Invoked without arguments, Talispam adds `X-Spam-Checker-Version`,
`X-Spam-Flag` and `X-Spam-Score` headers and writes the message to standard output.
The first line is preserved for mbox compatibility:
```console
$ cat ~/.talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam | head -n 5
From ilug-admin@linux.ie  Wed Sep 25 10:29:22 2002
X-Spam-Checker-Version: talispam 0.5.0 on kugelmass
X-Spam-Flag: YES
X-Spam-Score: 98
Return-Path: <ilug-admin@linux.ie>
```
To integrate with procmail, add these lines to `~/.procmailrc`:
```
:0fw
| talispam

:0e
EXITCODE==$?

:0:
* ^X-Spam-Flag: YES
$HOME/Mail/spam
```
### Available commands
```console
$ talispam -?
NAME:
 talispam - a Bayesian mail filter

USAGE:
 talispam [global-options] command [command options] [arguments...]

VERSION:
 0.5.0

COMMANDS:
   learn                train talispam classifier
   score                print ham/spam score for stdin
   explain              explain the classification of stdin
   whitelist            manage trusted senders
   init                 create default configuration and download dictionaries
   db                   inspect the classifier database
   stats                print stats summary for a mbox

GLOBAL OPTIONS:
   -?, --help
```
Use `talispam whitelist --help` or `talispam db --help` to list subcommands.

Inspect the trained classifier database with:
```console
$ talispam db print
$ talispam db info
Ham messages: 2500
Spam messages: 1000
Words: 42000
Database size: 1234567 bytes
```
`db print` replaces `print-db` and preserves its output: words sorted by spam
score, with scores formatted to three decimal places. Word probabilities are
calculated directly from the stored training counts, independently of the dictionary. `db info` reports the
ham/spam message counts saved during training, the number of distinct words
in the database, and its on-disk size in bytes. The numbers above are examples.
Both commands use the database configured under `:filter-db :location` and do
not require a dictionary. The `print-db` command has been removed.

Calculate score statistics for an mbox file:
```console
$ talispam stats --mbox /path/to/mailbox.mbox
```
`stats` requires a trained database and an mbox file beginning with a `From `
envelope line. Its statistics use raw scores on a 0–1 scale.

### Performance
The following measurements compare Talispam with SpamAssassin on an Intel Atom
D2550 @ 1.86 GHz. The results are not directly comparable because SpamAssassin
performs more checks:
```console
$ time cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | spamassassin | head -n 5
From ilug-admin@linux.ie  Wed Sep 25 10:29:22 2002
Return-Path: <ilug-admin@linux.ie>
X-Spam-Checker-Version: SpamAssassin 3.4.4 (2020-01-24) on kugelmass.local
X-Spam-Flag: YES
X-Spam-Level: ***********

real    0m7.332s
user    0m7.170s
sys     0m0.156s
$ time cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam | head -n 5
From ilug-admin@linux.ie  Wed Sep 25 10:29:22 2002
X-Spam-Checker-Version: talispam 0.3.0 on kugelmass
X-Spam-Flag: YES
X-Spam-Score: 98
Return-Path: <ilug-admin@linux.ie>

real    0m0.269s
user    0m0.238s
sys     0m0.055s
```

## License
Copyright (c) 2020-2026 Saidone

Distributed under the MIT License
