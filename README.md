# talispam
(or spamulet) a software held to act as a charm to avert spam and bring good messages

## Very quick roundtrip
### Build
get the sources:
```console
$ git clone https://github.com/saidone75/talispam.git
```
produce an uberjar with leiningen:
```console
$ cd talispam
$ lein uberjar
Compiling talispam.config
Compiling talispam.core
[...]
Compiling talispam.filter
Created /home/saidone/talispam/target/uberjar/talispam-0.1.0-SNAPSHOT.jar
Created /home/saidone/talispam/target/uberjar/talispam-0.1.0-SNAPSHOT-standalone.jar
```
create a native binary (need a GraalVM toolchain installed and configured):
```console
$ lein native
Build on Server(pid: 31266, port: 40987)
[./target/talispam:31266]    classlist:   2,674.83 ms
[...]
[./target/talispam:31266]      [total]:  40,015.73 ms
```
and copy the executable binary (target/talispam) somewhere in your path
### Configuration
clone the sample configuration from [talispam-config](https://github.com/saidone75/talispam-config) on your ~/.talispam folder:
```console
$ cd ~
$ git clone https://github.com/saidone75/talispam-config.git .talispam
```
(WARNING: contains a quite big spam/ham training corpus, you may want to train the filter against your own collections)

train bayesian classifier:
```console
$ talispam learn
building classifier db...
done
```
### Test
print spam/ham score (lower score is ham, higher is spam)
```console
$ cat .talispam/easy_ham/02051.58e196144807bd76d7b77d4b7efb6d32 | talispam score
19
$ cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam score
96
```
### Compatibility
I imagined it as a drop-in replacement for SpamAssassin on my personal mail server: invoked by procmail without arguments will add the same spam identification header and return the message to stdout:
```console
$ cat .talispam/spam/00460.8996dc28ab56dd7b6f35b956deceaf22 | talispam | head -n 5
From ilug-admin@linux.ie  Wed Sep 25 10:29:22 2002
X-Spam-Checker-Version: TaliSpam 0.1.0-SNAPSHOT on kugelmass
X-Spam-Flag: YES
X-Spam-Level: 96
Return-Path: <ilug-admin@linux.ie>
```
### Performance
on my little mail server (Intel(R) Atom(TM) CPU D2550   @ 1.86GHz) is extraordinarily fast, expecially in comparison with SpamAssassin (to be honest, not directly comparable because SpamAssassin perform a lot more checks):
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
X-Spam-Checker-Version: TaliSpam 0.1.0-SNAPSHOT on kugelmass
X-Spam-Flag: YES
X-Spam-Level: 96
Return-Path: <ilug-admin@linux.ie>

real    0m0.340s
user    0m0.299s
sys     0m0.071s
```

