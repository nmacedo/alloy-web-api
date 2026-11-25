# Alloy API model test suite

These fixtures exercise the generic API with static and temporal Alloy models.
`AlloyModelExamplesTest` sends them through `/validate` and `/getInstances`.

| Category | Static fixture | Dynamic fixture | Expected result |
| --- | --- | --- | --- |
| Well-formed | `static/valid.als` | `dynamic/valid.als` | Validation succeeds and instance enumeration returns a satisfiable result; the response's `static` flag distinguishes the model kind. |
| Syntactically invalid | `static/invalid.als` | `dynamic/invalid.als` | Validation returns `success: false` with an error message and location. |
| Warning-producing | `static/warning.als` | `dynamic/warning.als` | Enumeration succeeds and returns Alloy's warning in `warning_error`/`msg`. |

Run from the `alloy-web-api` project directory:

```sh
mvn test
```

These are starter examples for review; they are not intended to exhaustively
cover Alloy syntax or warning classes.
