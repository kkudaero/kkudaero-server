# kkudaero-server

꾸대로 웹과 iOS가 함께 쓰는 API 서버입니다. 제품 개요는 [docs/PRODUCT.md](docs/PRODUCT.md), 작업 규칙은 [AGENTS.md](AGENTS.md)에 있습니다.

## 준비물

- JDK 21
- PostgreSQL

## 로컬 실행

1. `src/main/resources/application-local.example.yml`을 같은 폴더에 `application-local.yml`로 복사하고 DB 접속 정보를 채웁니다. 이 파일은 git에서 제외됩니다.
2. `kkudaero.jwt.secret`에 Base64로 인코딩한 32바이트 이상의 값을 넣습니다. `openssl rand -base64 32`로 만들 수 있습니다.
3. `kkudaero.encryption.key`에 Base64로 인코딩한 32바이트 값을 넣습니다. 같은 명령으로 만들되 JWT 시크릿과 다른 값을 씁니다.
4. 서버를 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- API 문서: http://localhost:8080/swagger-ui.html
- OpenAPI 명세: http://localhost:8080/v3/api-docs

`application-local.yml` 없이 실행하려면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `PERSONAL_DATA_ENCRYPTION_KEY` 환경변수를 줍니다.

## 인증

`POST /api/v1/sellers`로 가입하고 `POST /api/v1/sellers/login`으로 받은 `accessToken`을 `Authorization: Bearer <토큰>` 헤더에 넣습니다. 결정 배경은 [docs/decisions/0001-seller-authentication.md](docs/decisions/0001-seller-authentication.md)에 있습니다.

## 개인정보 암호화

개인통관고유부호, 이름, 주소, 전화번호 컬럼에는 `@Convert(converter = EncryptedStringConverter.class)`를 붙여 암호화해서 저장합니다. 로그에 꼭 남겨야 하면 `MaskingUtil`로 가립니다. 암호화 키를 잃으면 저장된 값을 복구할 수 없습니다. 배경은 [docs/decisions/0004-personal-data-encryption.md](docs/decisions/0004-personal-data-encryption.md)에 있습니다.

## 관부가세·마진 예상 계산

`POST /api/v1/duty-estimates`에 물품 금액, 배송비, 관세율, 판매가를 보내면 목록통관 여부와 합산과세 위험, 예상 관부가세와 마진을 돌려줍니다. 결과는 언제나 예상치입니다. 계산 방식은 [docs/decisions/0002-duty-estimate-calculation.md](docs/decisions/0002-duty-estimate-calculation.md)에 있습니다.

환율을 비우면 관세청 과세환율을 조회해 씁니다. 조회하려면 공공데이터포털에서 `관세청_관세환율정보(GW)` 활용 신청을 하고, 일반 인증키(Decoding)를 `CUSTOMS_EXCHANGE_SERVICE_KEY` 환경변수나 `application-local.yml`의 `kkudaero.exchange.service-key`에 넣습니다. 키가 없으면 환율을 직접 넣은 요청만 계산됩니다. 배경은 [docs/decisions/0003-customs-exchange-rate.md](docs/decisions/0003-customs-exchange-rate.md)에 있습니다.

## 주문 추적 보드

`/api/v1/orders`로 주문을 등록·조회·수정·삭제하고 `PATCH /api/v1/orders/{orderId}/status`로 진행 상태를 바꿉니다. 셀러는 자기 주문만 볼 수 있고, 수취인 이름과 개인통관고유부호는 암호화해서 저장합니다. 배경은 [docs/decisions/0005-order-board.md](docs/decisions/0005-order-board.md)에 있습니다.

## 통관 진행 조회

`GET /api/v1/orders/{orderId}/customs-status`는 주문의 국제 운송장번호로 관세청 화물통관 진행정보를 조회합니다. 조회하려면 UNIPASS에서 화물통관진행정보조회 OpenAPI 인증키를 받아 `UNIPASS_API_KEY` 환경변수나 `application-local.yml`의 `kkudaero.unipass.api-key`에 넣습니다. 키가 없으면 503을 돌려줍니다. 배경은 [docs/decisions/0006-customs-status.md](docs/decisions/0006-customs-status.md)에 있습니다.

## 빌드 확인

```bash
./gradlew clean build
```

테스트는 PostgreSQL 없이 H2(PostgreSQL 모드)로 돕니다.
