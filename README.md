# Alloy Web API

Standalone Quarkus web API for Alloy model validation, instance enumeration,
and projection. The endpoint implementations and response behavior are moved
from Alloy4Fun; this project has no dependency on Alloy4Fun or its database.

The service currently exposes:

- `POST /validate`
- `POST /getInstances`
- `POST /getProjection`

For an interactive API explorer, start the service and open
`http://localhost:4001/q/swagger-ui`. The OpenAPI document is at
`http://localhost:4001/q/openapi`. The explorer is included in packaged runs
as well as development mode.

The solver session is maintained by the service in memory, as in the original
Alloy4Fun API. Build and run with Java 17 or newer:

```sh
mvn package
java -Dquarkus.http.port=4001 -jar target/alloy-web-api-0.1.0-SNAPSHOT-runner.jar
```

In Swagger UI, try `/validate` first, then `/getInstances`. The latter creates
an in-memory solver session; use its `sessionId` and a type/atom label returned
by the instance response for `/getProjection`. Solver sessions are local to
the running API process and are not shared with the stats API.

The starter model test suite covers static and dynamic valid, syntactically
invalid, and warning-producing models. See
[`src/test/resources/models/README.md`](src/test/resources/models/README.md)
and run it with `mvn test`.

The project uses the published Alloy 6.2.0 distribution artifact from Maven
Central.

This project is maintained in the public
[`nmacedo/alloy-web-api`](https://github.com/nmacedo/alloy-web-api) repository.
Keep its build and tests independent of the Alloy4Fun checkout. Create a
versioned release after projection behavior and its regression tests meet the
A4F 2.0 criteria.

`/getStats` is intentionally not part of this project; it remains in the
Alloy4Fun-specific API.
