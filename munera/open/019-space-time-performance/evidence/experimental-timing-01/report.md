# Experimental timing diagnostic

Five independent forks; one-second site budgets. Not acceptance or S-03 parity.
Stores include per-operation timer and opaque-input costs; reset is outside timing.

| Case | Topic | Comparison type | Configuration | Representation | Operation | Dimensions | Metric | Root | Unit | Status | Mean | Range | Sample SD | Reasons |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=1024 | :time | — | :ns/op | :measured | 585.944 | 564.723–611.336 | 17.337 |  |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :value-array | :construct | length=1024 | :time | — | :ns/op | :measured | 7455.142 | 7416.639–7521.093 | 43.546 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=4 | :time | — | :ns/op | :measured | 27.339 | 26.421–28.394 | 0.946 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :value-array | :construct | length=4 | :time | — | :ns/op | :measured | 53.649 | 53.061–55.039 | 0.826 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :reference-array | :read | length=1024 | :time | — | :ns/op | :measured | 0.389 | 0.387–0.391 | 0.002 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :value-array | :read | length=1024 | :time | — | :ns/op | :measured | 18.258 | 18.068–18.456 | 0.160 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :reference-array | :read | length=4 | :time | — | :ns/op | :measured | 0.410 | 0.386–0.480 | 0.040 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :value-array | :read | length=4 | :time | — | :ns/op | :measured | 11.054 | 0.234–18.436 | 9.875 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :reference-array | :store | length=1024 | :time | — | :ns/op | :measured | 1.171 | 0.552–1.674 | 0.447 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :value-array | :store | length=1024 | :time | — | :ns/op | :measured | 21.076 | 21.038–21.119 | 0.038 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :reference-array | :store | length=4 | :time | — | :ns/op | :measured | 2.344 | 0.654–4.044 | 1.536 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :value-array | :store | length=4 | :time | — | :ns/op | :measured | 13.292 | 1.429–21.039 | 10.577 |  |
| :field/construct | :field | :external-java-point | :valhalla | :reference-holder | :construct | — | :time | — | :ns/op | :measured | 1.948 | 1.930–1.965 | 0.014 |  |
| :field/construct | :field | :external-java-point | :valhalla | :restricted-holder | :construct | — | :time | — | :ns/op | :measured | 2.091 | 2.071–2.099 | 0.011 |  |
| :field/read | :field | :external-java-point | :valhalla | :reference-holder | :read | — | :time | — | :ns/op | :measured | 0.438 | 0.435–0.440 | 0.002 |  |
| :field/read | :field | :external-java-point | :valhalla | :restricted-holder | :read | — | :time | — | :ns/op | :measured | 0.241 | 0.239–0.243 | 0.002 |  |
| :field/replace | :field | :external-java-point | :valhalla | :reference-holder | :replace | — | :time | — | :ns/op | :measured | 1.951 | 1.934–1.960 | 0.010 |  |
| :field/replace | :field | :external-java-point | :valhalla | :restricted-holder | :replace | — | :time | — | :ns/op | :measured | 2.088 | 2.083–2.094 | 0.004 |  |
