# Product search

Small Spring Boot service that searches products from dummyjson and returns them two per page with the discount applied.

## Running

Needs Java 21.

```sh
./gradlew bootRun   # starts on localhost:8080
./gradlew test
```

Example request:

```sh
curl -X POST localhost:8080/products/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"phone","page":2}'
```

XML works too, just set `Content-Type` / `Accept` to `application/xml`.
