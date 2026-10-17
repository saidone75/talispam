# talispam
a talispam is a program held to act as a charm to avert spam and bring good messages

[![talispam.png](https://i.postimg.cc/KYZyT1M7/talispam.png)](https://postimg.cc/tsmfQCD7)

## Very quick roundtrip
### Build
#### Clojure CLI commands

| Command | Purpose |
| --- | --- |
| `clojure` | Start a REPL. |
| `clojure -M:run --help` | Run the application from source. |
| `clojure -M:test` | Run all tests; exit with a nonzero status on failure. |
| `clojure -T:build uber` | Build `target/uberjar/talispam-0.4.2-standalone.jar` with AOT compilation. |
| `clojure -T:build native-image` | Build the uberjar and then the native executable `target/talispam` (requires GraalVM). |
| `clojure -T:build clean` | Remove `target/` and its build artifacts. |

#### Local build

Install Java (JDK 25 is used in CI) and the official [Clojure CLI](https://clojure.org/guides/install_clojure).
The Debian/Ubuntu `clojure` package may provide only the language launcher,
which does not support `deps.edn`, `-M`, or `-T`. Verify your installation:
```console
$ clojure -Sdescribe
```
This must print CLI configuration information. If it reports
`-Sdescribe (No such file or directory)` or `-T:build (No such file or directory)`,
install the official CLI. On Linux, with Java, Bash, curl, and rlwrap installed:
```sh
curl -fL https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh -o /tmp/clojure-linux-install.sh
chmod +x /tmp/clojure-linux-install.sh
sudo /tmp/clojure-linux-install.sh
hash -r
clojure -Sdescribe
```
The installer places `clojure` in `/usr/local/bin`; ensure that directory precedes
`/usr/bin` in your `PATH` if the old launcher is still selected.

Then get the sources:
```console
$ git clone https://github.com/saidone75/talispam.git
$ cd talispam
```
Run the tests and produce an executable uberjar with tools.build:
```console
$ clojure -M:test
$ clojure -T:build uber
$ java -jar target/uberjar/talispam-0.4.2-standalone.jar --help
```
Run directly from source with `clojure -M:run --help`, or start a REPL with `clojure`.
Project metadata (including the version) lives in `resources/talispam.edn`.
Use `clojure -T:build clean` to remove build artifacts.

Create a native binary (*requires GraalVM with `native-image` on PATH*):
```console
$ clojure -T:build native-image
```
Copy `target/talispam` somewhere in your path. Optional build parameters:
```console
$ clojure -T:build native-image :optimization "b" :march "native"
```
The uberjar is built with AOT compilation and direct linking, following the
[tools.build application guide](https://clojure.org/guides/tools_build).

### Build with Docker (GraalVM)
Only Docker with Compose and BuildKit is required on the host. The build uses
the GraalVM version specified in [docker/Dockerfile](docker/Dockerfile)
and Clojure CLI with tools.build to compile an uberjar and then a native executable.

On Windows, from any directory:
```bat
build-docker.bat
```
On Linux/macOS:
```console
$ ./build-docker.sh
```
Both scripts run the build directly without arguments and produce `dist/talispam`, a **Linux** executable.
Copy it to the target Linux machine and configure `~/.talispam` as described below.

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
$ talispam whitelist edit
$ talispam whitelist print
```
`ham` scans the configured ham directories and mbox files, merges their
senders with the existing whitelist, and saves a sorted list without duplicates.
`edit` opens the configured whitelist file using `$VISUAL`, then `$EDITOR`,
falling back to `vi`; editor arguments are supported. `print` prints the saved
entries without scanning the corpus. These commands do not need a trained database
or compiled dictionary. Whitelist matching is enabled by `:whitelist :use`;
the file path is set by `:whitelist :location`.

train bayesian classifier:
```console
$ talispam learn
building classifier db ✓
done!
```
### Test
print spam/ham score (lower score is ham, higher is spam)
```console
$ cat .talispam/easy_ham/02051.58e196144807bd76d7b77d4b7efb6d32 | talispam score
14
$ cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam score
98
```
### Compatibility
I imagined it as a drop-in replacement for SpamAssassin on my personal mail server: invoked without arguments will add the same spam identification header and return the message to stdout:
```console
$ cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam | head -n 5
From ilug-admin@linux.ie  Wed Sep 25 10:29:22 2002
X-Spam-Checker-Version: talispam 0.4.2 on kugelmass
X-Spam-Flag: YES
X-Spam-Score: 98
Return-Path: <ilug-admin@linux.ie>
```
integration with procmail is pretty much the same as well, just add these lines on your .procmailrc:
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
 0.4.2

COMMANDS:
   learn                train talispam classifier
   score                print ham/spam score for stdin
   whitelist            manage trusted senders (ham, edit, print)
   init                 create default configuration and download dictionaries
   print-db             print all words from classifier db by spam score
   stats                print stats summary for a mbox

GLOBAL OPTIONS:
   -?, --help
```
### Performance
on my little mail server (Intel Atom D2550 @ 1.86GHz) is extraordinarily fast, expecially in comparison with SpamAssassin (to be honest, not directly comparable because SpamAssassin perform a lot more checks):
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
