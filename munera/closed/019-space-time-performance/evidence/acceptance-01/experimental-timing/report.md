# Experimental timing acceptance capture

Five independent forks; one-second site budgets. No S-03 parity requirement applies to these external types.
Stores traverse 1,024 distinct arrays; timer/input and traversal costs are included; slot resets are outside timing.

| Case | Topic | Comparison type | Configuration | Representation | Operation | Dimensions | Metric | Root | Unit | Status | Mean | Range | Sample SD | Reasons |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=1024 | :time | — | :ns/op | :measured | 610.585 | 592.124–616.700 | 10.373 |  |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :value-array | :construct | length=1024 | :time | — | :ns/op | :measured | 7511.500 | 7423.593–7714.463 | 116.348 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=4 | :time | — | :ns/op | :measured | 27.244 | 26.905–27.887 | 0.384 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :value-array | :construct | length=4 | :time | — | :ns/op | :measured | 54.066 | 53.582–54.659 | 0.467 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :reference-array | :read | length=1024 | :time | — | :ns/op | :measured | 0.390 | 0.387–0.399 | 0.005 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :value-array | :read | length=1024 | :time | — | :ns/op | :measured | 18.416 | 18.078–18.960 | 0.356 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :reference-array | :read | length=4 | :time | — | :ns/op | :measured | 0.392 | 0.386–0.400 | 0.006 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :value-array | :read | length=4 | :time | — | :ns/op | :measured | 11.494 | 0.227–19.263 | 10.278 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :reference-array | :store | length=1024 | :time | — | :ns/op | :measured | 1.123 | 1.024–1.187 | 0.062 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :value-array | :store | length=1024 | :time | — | :ns/op | :measured | 11.243 | 11.138–11.401 | 0.128 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :reference-array | :store | length=4 | :time | — | :ns/op | :measured | 0.694 | 0.661–0.744 | 0.034 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :value-array | :store | length=4 | :time | — | :ns/op | :measured | 6.858 | 0.263–11.438 | 6.015 |  |
| :field/construct | :field | :external-java-point | :valhalla | :reference-holder | :construct | — | :time | — | :ns/op | :measured | 1.959 | 1.945–1.997 | 0.022 |  |
| :field/construct | :field | :external-java-point | :valhalla | :restricted-holder | :construct | — | :time | — | :ns/op | :measured | 2.099 | 2.082–2.120 | 0.016 |  |
| :field/read | :field | :external-java-point | :valhalla | :reference-holder | :read | — | :time | — | :ns/op | :measured | 0.437 | 0.433–0.445 | 0.005 |  |
| :field/read | :field | :external-java-point | :valhalla | :restricted-holder | :read | — | :time | — | :ns/op | :measured | 0.241 | 0.239–0.244 | 0.002 |  |
| :field/replace | :field | :external-java-point | :valhalla | :reference-holder | :replace | — | :time | — | :ns/op | :measured | 1.965 | 1.948–1.989 | 0.016 |  |
| :field/replace | :field | :external-java-point | :valhalla | :restricted-holder | :replace | — | :time | — | :ns/op | :measured | 2.083 | 2.068–2.099 | 0.012 |  |
