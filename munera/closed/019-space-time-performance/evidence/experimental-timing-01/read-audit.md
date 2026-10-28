# Saved experimental read timing audit

Raw is a bootstrap mean batch estimate in ns, not a single sample.
Pair position follows the fixed alternating execution order, not row order.

| Case | Representation | Fork | Pair position | Raw ns | Divisor | ns/op |
| --- | --- | --- | --- | --- | --- | --- |
| :array/read-4 | :value-array | 1 | first | 11367.839623 | 625 | 18.188543 |
| :array/read-4 | :reference-array | 1 | second | 4099.470588 | 10000 | 0.409947 |
| :array/read-1024 | :value-array | 1 | first | 11292.695652 | 625 | 18.068313 |
| :array/read-1024 | :reference-array | 1 | second | 3873.258065 | 10000 | 0.387326 |
| :field/read | :restricted-holder | 1 | first | 2416.389313 | 10000 | 0.241639 |
| :field/read | :reference-holder | 1 | second | 4373.603306 | 10000 | 0.437360 |
| :array/read-4 | :value-array | 2 | second | 2340.480769 | 10000 | 0.234048 |
| :array/read-4 | :reference-array | 2 | first | 4799.140625 | 10000 | 0.479914 |
| :array/read-1024 | :value-array | 2 | second | 10851.909091 | 588 | 18.455628 |
| :array/read-1024 | :reference-array | 2 | first | 3914.533835 | 10000 | 0.391453 |
| :field/read | :restricted-holder | 2 | second | 2429.271318 | 10000 | 0.242927 |
| :field/read | :reference-holder | 2 | first | 4385.577236 | 10000 | 0.438558 |
| :array/read-4 | :value-array | 3 | first | 10840.541667 | 588 | 18.436295 |
| :array/read-4 | :reference-array | 3 | second | 3858.598485 | 10000 | 0.385860 |
| :array/read-1024 | :value-array | 3 | first | 10813.549180 | 588 | 18.390390 |
| :array/read-1024 | :reference-array | 3 | second | 3874.231481 | 10000 | 0.387423 |
| :field/read | :restricted-holder | 3 | first | 2418.548872 | 10000 | 0.241855 |
| :field/read | :reference-holder | 3 | second | 4354.735043 | 10000 | 0.435474 |
| :array/read-4 | :value-array | 4 | second | 2392.571429 | 10000 | 0.239257 |
| :array/read-4 | :reference-array | 4 | first | 3874.375940 | 10000 | 0.387438 |
| :array/read-1024 | :value-array | 4 | second | 10707.669492 | 588 | 18.210322 |
| :array/read-1024 | :reference-array | 4 | first | 3879.570313 | 10000 | 0.387957 |
| :field/read | :restricted-holder | 4 | second | 2394.191304 | 10000 | 0.239419 |
| :field/read | :reference-holder | 4 | first | 4396.279661 | 10000 | 0.439628 |
| :array/read-4 | :value-array | 5 | first | 11355.973214 | 625 | 18.169557 |
| :array/read-4 | :reference-array | 5 | second | 3880.550388 | 10000 | 0.388055 |
| :array/read-1024 | :value-array | 5 | first | 11354.680000 | 625 | 18.167488 |
| :array/read-1024 | :reference-array | 5 | second | 3890.273438 | 10000 | 0.389027 |
| :field/read | :restricted-holder | 5 | first | 2395.830357 | 10000 | 0.239583 |
| :field/read | :reference-holder | 5 | second | 4367.877049 | 10000 | 0.436788 |
