# Integrated Swagger API Specification

## Introduction
We have used `swagger-codegen-maven-plugin` to generate api interfaces from the swagger specification file. This is an automated process, and the generated code is placed in `target/generated-sources/swagger` directory. You can directly use these interfaces in your code.

## Swagger Version
Swagger version of swagger specification file should be Swagger 2.0

If you are using Swagger 3.0, you can convert it to Swagger 2.0 using the following command:

```bash
pnpm install -g api-spec-converter
api-spec-converter          \
    --from openapi_3        \
    --to swagger_2          \
    --syntax yaml           \
    --order default         \
    --check                 \
    --dummy                 \
    your-swagger-3.0.yaml
```
