# kkudaero-server

꾸대로 웹과 iOS가 함께 쓰는 API 서버입니다. 제품 개요는 [docs/PRODUCT.md](docs/PRODUCT.md), 작업 규칙은 [AGENTS.md](AGENTS.md)에 있습니다.

## 준비물

- JDK 21
- PostgreSQL

## 로컬 실행

1. `src/main/resources/application-local.example.yml`을 같은 폴더에 `application-local.yml`로 복사하고 DB 접속 정보를 채웁니다. 이 파일은 git에서 제외됩니다.
2. 서버를 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- API 문서: http://localhost:8080/swagger-ui.html
- OpenAPI 명세: http://localhost:8080/v3/api-docs

`application-local.yml` 없이 실행하려면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수를 줍니다.

## 빌드 확인

```bash
./gradlew clean build
```

테스트는 PostgreSQL 없이 H2(PostgreSQL 모드)로 돕니다.
