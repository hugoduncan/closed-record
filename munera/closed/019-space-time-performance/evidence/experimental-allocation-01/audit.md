# Saved allocation batch audit

Counts below are bytes per 1,024-operation batch, without subtraction.

## Fork 1 — :array/store-4 / :value-array

Warmup: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 48 0 0 0 0 0]

Final: [0 0 0 0 0] 

## Fork 1 — :array/store-1024 / :reference-array

Warmup: [48 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]

Final: [0 0 0 0 0] 

## Fork 3 — :array/read-4 / :value-array

Warmup: [24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576]

Final: [24576 24624 24624 24624 24624] 

## Fork 4 — :array/read-4 / :value-array

Warmup: [24576 24576 24576 24576 24576 24576 24576 24576 24624 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576]

Final: [24576 24576 24576 24576 24576] 

## Fork 5 — :array/store-4 / :value-array

Warmup: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 48 0 0 0 0 0]

Final: [0 0 0 0 0] 

## Fork 1 — :field/read / :restricted-holder

Warmup: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]

Final: [0 0 0 0 0] 

## Fork 2 — :field/read / :restricted-holder

Warmup: [24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576]

Final: [24576 24576 24576 24576 24576] 

## Fork 3 — :field/read / :restricted-holder

Warmup: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]

Final: [0 0 0 0 0] 

## Fork 4 — :field/read / :restricted-holder

Warmup: [24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576 24576]

Final: [24576 24576 24576 24576 24576] 

## Fork 5 — :field/read / :restricted-holder

Warmup: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]

Final: [0 0 0 0 0] 

