# Experimental timing diagnostic

Five independent forks; one-second site budgets. Not acceptance or S-03 parity.
Stores traverse 1,024 distinct arrays; timer/input and traversal costs are included; slot resets are outside timing.

| Case | Topic | Comparison type | Configuration | Representation | Operation | Dimensions | Metric | Root | Unit | Status | Mean | Range | Sample SD | Reasons |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=1024 | :time | — | :ns/op | :measured | 571.255 | 558.988–583.803 | 11.204 |  |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :value-array | :construct | length=1024 | :time | — | :ns/op | :measured | 7430.170 | 7416.655–7450.617 | 16.178 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=4 | :time | — | :ns/op | :measured | 27.509 | 25.927–29.801 | 1.603 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :value-array | :construct | length=4 | :time | — | :ns/op | :measured | 54.041 | 53.142–54.950 | 0.667 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :reference-array | :read | length=1024 | :time | — | :ns/op | :measured | 0.388 | 0.386–0.389 | 0.001 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :value-array | :read | length=1024 | :time | — | :ns/op | :measured | 18.287 | 17.940–19.032 | 0.430 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :reference-array | :read | length=4 | :time | — | :ns/op | :measured | 0.388 | 0.386–0.389 | 0.001 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :value-array | :read | length=4 | :time | — | :ns/op | :measured | 11.043 | 0.228–18.263 | 9.865 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :reference-array | :store | length=1024 | :time | — | :ns/op | :measured | 1.113 | 1.008–1.347 | 0.135 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :value-array | :store | length=1024 | :time | — | :ns/op | :measured | 11.042 | 10.853–11.187 | 0.153 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :reference-array | :store | length=4 | :time | — | :ns/op | :measured | 0.689 | 0.670–0.703 | 0.017 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :value-array | :store | length=4 | :time | — | :ns/op | :measured | 6.715 | 0.263–11.203 | 5.891 |  |
| :field/construct | :field | :external-java-point | :valhalla | :reference-holder | :construct | — | :time | — | :ns/op | :measured | 1.946 | 1.928–1.966 | 0.017 |  |
| :field/construct | :field | :external-java-point | :valhalla | :restricted-holder | :construct | — | :time | — | :ns/op | :measured | 2.101 | 2.083–2.153 | 0.030 |  |
| :field/read | :field | :external-java-point | :valhalla | :reference-holder | :read | — | :time | — | :ns/op | :measured | 0.439 | 0.433–0.448 | 0.006 |  |
| :field/read | :field | :external-java-point | :valhalla | :restricted-holder | :read | — | :time | — | :ns/op | :measured | 0.240 | 0.238–0.241 | 0.001 |  |
| :field/replace | :field | :external-java-point | :valhalla | :reference-holder | :replace | — | :time | — | :ns/op | :measured | 1.953 | 1.933–1.961 | 0.012 |  |
| :field/replace | :field | :external-java-point | :valhalla | :restricted-holder | :replace | — | :time | — | :ns/op | :measured | 2.078 | 2.056–2.092 | 0.014 |  |
