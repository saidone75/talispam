# talispam
a talispam (or spamulet) is a program held to act as a charm to avert spam and bring good messages

[![talispam.png](https://i.postimg.cc/KYZyT1M7/talispam.png)](https://postimg.cc/tsmfQCD7)

## Very quick roundtrip
### Build
get the sources:
```console
$ git clone https://github.com/saidone75/talispam.git -b v0.3.0
```
produce an uberjar with leiningen:
```console
$ cd talispam
$ lein uberjar
Compiling talispam.config
Compiling talispam.core
[...]
Compiling talispam.whitelist
Created /home/saidone/talispam/target/uberjar/talispam-0.3.0.jar
Created /home/saidone/talispam/target/uberjar/talispam-0.3.0-standalone.jar
```
create a native binary (need a GraalVM toolchain installed and configured):
```console
$ lein native
Build on Server(pid: 20771, port: 40355)
[./target/talispam:20771]    classlist:   4,185.77 ms,  2.15 GB
[...]
[./target/talispam:20771]      [total]: 244,502.81 ms,  1.98 GB
```
and copy the executable binary (target/talispam) somewhere in your path
### Configuration
clone the sample configuration from [talispam-config](https://github.com/saidone75/talispam-config) on your ~/.talispam folder:
```console
$ cd ~
$ git clone https://github.com/saidone75/talispam-config.git -b v0.3.0 .talispam
```
(WARNING: contains a quite big spam/ham training corpus, you may want to train the filter against your own collections)

train bayesian classifier:
```console
$ talispam learn
talispam 0.3.0
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
X-Spam-Checker-Version: talispam 0.3.0 on kugelmass
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
 0.3.0

COMMANDS:
   learn                train talispam classifier
   score                print ham/spam score for stdin
   whitelist            print a list of addresses in ham corpus
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
Copyright (c) 2020 Saidone

Distributed under the MIT License
