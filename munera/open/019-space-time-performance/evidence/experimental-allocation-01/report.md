# Experimental allocation diagnostic

Five independent forks; 1,024 operations per batch, 20 warmup and five final batches.
Raw counts include boundary costs. No subtraction. Not acceptance or graph ownership.

| Case | Topic | Comparison type | Configuration | Representation | Operation | Dimensions | Metric | Root | Unit | Status | Mean | Range | Sample SD | Reasons |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=1024 | :allocation | — | :bytes/op | :measured | 4112.000 | 4112.000–4112.000 | 0.000 |  |
| :array/construct-1024 | :array | :external-java-point | :valhalla | :value-array | :construct | length=1024 | :allocation | — | :bytes/op | :measured | 4112.000 | 4112.000–4112.000 | 0.000 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :reference-array | :construct | length=4 | :allocation | — | :bytes/op | :measured | 32.000 | 32.000–32.000 | 0.000 |  |
| :array/construct-4 | :array | :external-java-point | :valhalla | :value-array | :construct | length=4 | :allocation | — | :bytes/op | :measured | 32.000 | 32.000–32.000 | 0.000 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :reference-array | :read | length=1024 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :array/read-1024 | :array | :external-java-point | :valhalla | :value-array | :read | length=1024 | :allocation | — | :bytes/op | :measured | 24.000 | 24.000–24.000 | 0.000 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :reference-array | :read | length=4 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :array/read-4 | :array | :external-java-point | :valhalla | :value-array | :read | length=4 | :allocation | — | :bytes/op | :measured | 24.008 | 24.000–24.038 | 0.017 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :reference-array | :store | length=1024 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :array/store-1024 | :array | :external-java-point | :valhalla | :value-array | :store | length=1024 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :reference-array | :store | length=4 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :array/store-4 | :array | :external-java-point | :valhalla | :value-array | :store | length=4 | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :field/construct | :field | :external-java-point | :valhalla | :reference-holder | :construct | — | :allocation | — | :bytes/op | :measured | 16.000 | 16.000–16.000 | 0.000 |  |
| :field/construct | :field | :external-java-point | :valhalla | :restricted-holder | :construct | — | :allocation | — | :bytes/op | :measured | 16.000 | 16.000–16.000 | 0.000 |  |
| :field/read | :field | :external-java-point | :valhalla | :reference-holder | :read | — | :allocation | — | :bytes/op | :measured | 0.000 | 0.000–0.000 | 0.000 |  |
| :field/read | :field | :external-java-point | :valhalla | :restricted-holder | :read | — | :allocation | — | :bytes/op | :measured | 9.600 | 0.000–24.000 | 13.145 |  |
| :field/replace | :field | :external-java-point | :valhalla | :reference-holder | :replace | — | :allocation | — | :bytes/op | :measured | 16.000 | 16.000–16.000 | 0.000 |  |
| :field/replace | :field | :external-java-point | :valhalla | :restricted-holder | :replace | — | :allocation | — | :bytes/op | :measured | 16.000 | 16.000–16.000 | 0.000 |  |
