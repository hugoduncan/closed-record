# Experimental graph attempt 01

Five independent pinned Valhalla children exited zero. All 150 inventory outcomes
pass saved coverage, runtime, and declared artifact checks: 75 measured flat
roots and 75 unavailable reference roots. No measurements were repeated.

Flat arrays retain 32 bytes at length 4 and 4,112 bytes at length 1,024, for
construction, read, and changed store. Restricted holders retain 16 bytes.
Construction/replacement unions retain 32 bytes with a 16-byte increment;
read roots alias, with a 16-byte union and zero increment. All forks agree.
Reference graphs remain unavailable because value projection cannot establish
physical box identity; see [probe evidence](../value-identity) and
[operation controls](../experimental-graph-operations). Do not derive space ratios.

The child revision is in revision.txt; run.sh retains the exact JVM command.
The agent was built once before launch; graph-tool.edn declares independently
hashed JAR/class bytes, and sources.sha256 identifies Java inputs. Every row
retains observed active artifact identity. Resource hashes do not detect class
transformation. Child elapsed times, rounded to seconds, are 1, 0, 1, 0, 0;
these are process durations, not operation timings.

The first report command failed because its graph-tool require lacked a quote.
report-failure.edn preserves that failure. Correcting the report and reading the
same saved children produced report.md; collection was not repeated. Reproduce
reporting from the repository root with the base Clojure classpath plus dev/bench:

```sh
java -cp "dev/bench:$(clojure -Srepro -Spath)" clojure.main \
  munera/open/019-space-time-performance/evidence/experimental-graph-01/report.clj
```

This completes diagnostic graph capture, not acceptance. Experimental store timer
resolution and read/allocation order effects still require published limits.
