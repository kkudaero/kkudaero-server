# AGENTS.md

kkudaero-server에서 작업하는 코딩 에이전트가 따라야 하는 규칙입니다. 어떤 에이전트를 쓰더라도 같은 모양의 코드가 나오게 하는 것이 목적입니다.

꾸대로는 일본 구매대행을 처음 시작하는 1인 사업자를 돕는 서비스입니다. 이 저장소는 웹(`kkudaero-web`)과 iOS(`kkudaero-ios`)가 함께 쓰는 API 서버이고, 계산·콘텐츠·외부 API 연동·개인정보를 모두 여기서 처리합니다.

## 1. 프로젝트 정보

- 제품 개요와 1단계 기능은 `docs/PRODUCT.md`에 있습니다. 작업 전에 읽습니다.
- Java 21, Spring Boot 3, Gradle(Kotlin DSL)
- Spring Web, Spring Data JPA, Spring Validation, Spring Security
- DB: PostgreSQL, 마이그레이션은 Flyway
- API 문서: springdoc-openapi. 웹과 iOS는 이 명세로 타입을 만듭니다.
- 스케줄 작업: Spring `@Scheduled` (통관 상태 조회·알림)
- 이 저장소는 **private**입니다. 그래도 비밀값은 코드에 넣지 않습니다.
- `application-local.yml`과 `.env`는 git에서 제외된 파일입니다. 만들거나 커밋하지 않고, 없으면 사용자에게 알립니다. 필요한 키 이름은 `application-local.example.yml`에만 적습니다.

## 2. 빌드 확인

작업을 끝냈다고 말하기 전에 아래 명령이 성공하는지 확인합니다.

```bash
./gradlew clean build
```

- 테스트가 실패한 채로 끝내지 않습니다. 테스트를 지우거나 `@Disabled`로 넘기지 않습니다.
- API를 바꿨다면 서버를 띄워 `/v3/api-docs`에 변경이 반영됐는지 확인하고, 웹·iOS에서 타입을 다시 만들어야 한다고 사용자에게 알립니다.

## 3. 폴더 구조

```
src/main/java/com/kkudaero/server/
├─ KkudaeroServerApplication.java
├─ global/
│  ├─ config/               Security, JPA, OpenAPI, Scheduler 설정
│  ├─ error/                ErrorCode, BusinessException, GlobalExceptionHandler
│  ├─ response/             ApiResponse
│  ├─ security/             인증·인가
│  └─ util/                 암호화, 마스킹, 시간 변환
├─ domain/
│  └─ <기능>/
│     ├─ controller/        <기능>Controller.java
│     ├─ service/           <기능>Service.java
│     ├─ repository/        <기능>Repository.java
│     ├─ entity/            <기능>.java
│     └─ dto/               <이름>Request.java, <이름>Response.java
└─ infra/
   ├─ unipass/              관세청 통관 조회 클라이언트
   ├─ exchange/             환율 조회 클라이언트
   └─ notification/         푸시·메일 발송

src/main/resources/
├─ application.yml
├─ application-local.example.yml
└─ db/migration/            V<번호>__<설명>.sql
```

- 기능 이름은 서비스 용어를 그대로 씁니다: `checklist`, `calculator`, `customs`, `manual`, `order`, `seller`.
- 외부 시스템과 통신하는 코드는 모두 `infra`에 둡니다. `domain`은 외부 API의 응답 형식을 알지 않습니다.

## 4. 네이밍

| 종류 | 클래스 이름 | 예 |
|---|---|---|
| Controller | `<기능>Controller` | `CustomsController` |
| Service | `<기능>Service` | `CustomsService` |
| Repository | `<엔티티>Repository` | `ShipmentRepository` |
| Entity | `<엔티티>` | `Shipment` |
| 요청 DTO | `<이름>Request` | `CalculateDutyRequest` |
| 응답 DTO | `<이름>Response` | `CustomsStatusResponse` |
| 외부 클라이언트 | `<시스템>Client` | `UnipassClient` |
| 외부 응답 | `<시스템><이름>Payload` | `UnipassCargoPayload` |
| 예외 코드 | `<기능>ErrorCode` | `CustomsErrorCode` |

- 클래스는 UpperCamelCase, 메서드·변수는 lowerCamelCase, 상수는 UPPER_SNAKE_CASE, DB 테이블·컬럼은 snake_case를 씁니다.
- 약어는 `Id`, `Url`, `Api`, `Usd`, `Krw`처럼 첫 글자만 대문자로 씁니다. (`orderId`, `imageUrl`)
- boolean은 `is`, `has`, `can`, `should`로 시작합니다.
- Service 메서드는 동작에 맞춰 씁니다: 조회 `get~`/`find~`, 생성 `create~`, 수정 `update~`, 삭제 `delete~`, 계산 `calculate~`.
- URL은 `/api/v1/<복수형 리소스>` 형식의 kebab-case를 씁니다. (`/api/v1/customs-statuses`)

## 5. 기준 구현

새 기능은 아래 모양을 따릅니다. 기능 이름 `calculator`와 필드는 예시입니다.

### Request DTO: `domain/calculator/dto/CalculateDutyRequest.java`

```java
public record CalculateDutyRequest(
        @NotNull Country country,
        @NotNull @PositiveOrZero BigDecimal itemPrice,
        @NotNull @PositiveOrZero BigDecimal localShippingFee,
        @NotNull LocalDate purchaseDate
) {
}
```

### Response DTO: `domain/calculator/dto/CalculateDutyResponse.java`

```java
public record CalculateDutyResponse(
        BigDecimal itemPriceUsd,
        boolean isListClearance,
        BigDecimal estimatedDutyKrw,
        BigDecimal exchangeRate,
        Instant exchangeRateAt
) {
}
```

### Service: `domain/calculator/service/CalculatorService.java`

```java
@Service
@RequiredArgsConstructor
public class CalculatorService {

    private static final BigDecimal LIST_CLEARANCE_LIMIT_USD = new BigDecimal("150");

    private final ExchangeRateClient exchangeRateClient;

    public CalculateDutyResponse calculateDuty(CalculateDutyRequest request) {
        ExchangeRate rate = exchangeRateClient.getRate(request.country().currency());
        BigDecimal itemPriceUsd = rate.toUsd(request.itemPrice().add(request.localShippingFee()));
        boolean isListClearance = itemPriceUsd.compareTo(LIST_CLEARANCE_LIMIT_USD) <= 0;

        return new CalculateDutyResponse(
                itemPriceUsd,
                isListClearance,
                estimateDuty(itemPriceUsd, isListClearance, rate),
                rate.value(),
                rate.fetchedAt()
        );
    }

    private BigDecimal estimateDuty(BigDecimal itemPriceUsd, boolean isListClearance, ExchangeRate rate) {
        if (isListClearance) {
            return BigDecimal.ZERO;
        }
        return DutyEstimator.estimate(itemPriceUsd, rate);
    }
}
```

### Controller: `domain/calculator/controller/CalculatorController.java`

```java
@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
@Tag(name = "Calculator", description = "관부가세·마진 예상 계산")
public class CalculatorController {

    private final CalculatorService calculatorService;

    @PostMapping("/duty")
    @Operation(summary = "관부가세 예상 계산", description = "150달러 목록통관 여부와 예상 관부가세를 계산합니다.")
    public ApiResponse<CalculateDutyResponse> calculateDuty(@Valid @RequestBody CalculateDutyRequest request) {
        return ApiResponse.success(calculatorService.calculateDuty(request));
    }
}
```

### 외부 클라이언트: `infra/unipass/UnipassClient.java`

```java
public interface UnipassClient {

    CargoStatus getCargoStatus(String trackingNumber);
}
```

- 구현체(`HttpUnipassClient`)는 외부 응답(`UnipassCargoPayload`)을 받아 도메인 타입(`CargoStatus`)으로 바꿔서 돌려줍니다.
- 테스트에서는 인터페이스를 Mock으로 바꿔 씁니다.

## 6. 계층별 규칙

### Controller

- 요청 검증(`@Valid`)과 Service 호출, 응답 감싸기만 합니다. 비즈니스 로직을 두지 않습니다.
- 응답은 항상 `ApiResponse<T>`로 감쌉니다.
- 모든 API에 `@Tag`, `@Operation`을 붙여 OpenAPI 명세에 설명이 나오게 합니다. 웹과 iOS가 이 명세로 타입을 만듭니다.
- Entity를 요청·응답에 그대로 쓰지 않습니다. 항상 DTO를 씁니다.

### Service

- 비즈니스 로직은 Service에 둡니다.
- 조회만 하는 메서드는 `@Transactional(readOnly = true)`, 변경하는 메서드는 `@Transactional`을 붙입니다.
- 실패는 `BusinessException(<기능>ErrorCode.XXX)`로 던집니다. `RuntimeException`이나 `null`을 돌려주지 않습니다.
- 의존성은 생성자 주입(`@RequiredArgsConstructor` + `private final`)만 씁니다. `@Autowired` 필드 주입을 쓰지 않습니다.

### Entity

- `@Setter`를 쓰지 않습니다. 상태 변경은 의미 있는 이름의 메서드로 합니다. (`shipment.markHeld(reason)`)
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 막습니다.
- 생성은 정적 팩토리 메서드(`Shipment.create(...)`)나 빌더로 합니다.
- 모든 테이블에 `created_at`, `updated_at`을 둡니다(`BaseTimeEntity` 상속).

### DTO

- `record`로 만듭니다.
- Request DTO에 검증 어노테이션을 붙입니다.
- DTO ↔ Entity 변환은 Service나 DTO의 정적 메서드(`CustomsStatusResponse.from(shipment)`)에서 합니다. Entity가 DTO를 알지 않습니다.

### 계산 규칙

- 관부가세·마진·목록통관 판정은 **이 서버에서만** 계산합니다. 웹과 iOS는 결과만 받습니다.
- 금액은 `BigDecimal`로 다룹니다. `double`, `float`를 쓰지 않습니다.
- 150달러 기준, 환율, 수수료율처럼 바뀔 수 있는 값은 상수나 설정으로 한곳에 둡니다. 여러 곳에 숫자를 직접 쓰지 않습니다.
- 계산 결과는 언제나 **예상치**입니다. 응답과 API 설명에 계산에 쓴 환율과 기준 시각을 함께 줍니다.
- 통관 신고를 대신하는 기능은 만들지 않습니다(관세사 업무).

### 개인정보와 보안

- 개인통관고유부호, 이름, 주소, 전화번호는 DB에 **암호화해서** 저장합니다(`global/util`의 JPA `AttributeConverter`).
- 이 값들을 로그에 남기지 않습니다. 꼭 필요하면 `MaskingUtil`로 가립니다.
- 셀러가 탈퇴하면 그 셀러와 고객의 개인정보를 삭제합니다.
- API 키, 시크릿, DB 비밀번호는 환경변수나 `application-local.yml`로만 받습니다. 코드와 `application.yml`에 쓰지 않습니다.
- 모든 API는 인증된 셀러만 자기 데이터에 접근하도록 확인합니다. 다른 셀러의 ID로 조회되는지 항상 따져 봅니다.
- 외부 API를 부르는 경로에는 타임아웃을 정합니다. 호출 횟수 제한이 있는 API는 캐시합니다.

### 시간

- 저장과 응답은 UTC(`Instant`)로 합니다. 한국 시간 변환은 클라이언트에서 합니다.
- 합산과세처럼 "구매일"이 중요한 계산은 날짜(`LocalDate`)와 시간대를 명시해서 받습니다.

### 스케줄 작업

- 통관 상태 조회처럼 반복 실행되는 작업은 같은 데이터로 여러 번 돌아도 결과가 같아야 합니다(멱등).
- 알림은 상태가 **바뀌었을 때만** 한 번 보냅니다. 같은 알림을 중복으로 보내지 않습니다.
- 실패하면 로그를 남기고 다음 실행에서 다시 시도합니다. 작업 전체를 멈추지 않습니다.

### DB 마이그레이션

- 스키마는 Flyway SQL 파일로만 바꿉니다. `ddl-auto`는 `validate`로 둡니다.
- 이미 머지된 마이그레이션 파일은 수정하지 않습니다. 새 파일을 추가합니다.

## 7. 테스트

- Service와 계산 로직은 JUnit 5 + Mockito로 단위 테스트를 씁니다.
- 관부가세 계산은 경계값(149.99, 150, 150.01달러)을 반드시 테스트합니다.
- 외부 API는 실제로 호출하지 않고 클라이언트 인터페이스를 Mock으로 바꿉니다.
- 테스트 메서드 이름은 한글로 동작을 적습니다. (`void 물품가가_150달러_이하면_목록통관이다()`)

## 8. 코드 스타일

- 들여쓰기 4칸, 한 줄 120자 이내
- import에 `*`를 쓰지 않습니다.
- 더 상속하지 않는 클래스는 `final`로 선언합니다(Entity, Spring 빈 제외).
- `Optional`은 반환 타입에만 씁니다. 필드와 파라미터에 쓰지 않습니다.
- `null`을 돌려주지 않습니다. 없을 수 있으면 `Optional`, 없으면 안 되면 예외를 던집니다.
- 설명이 꼭 필요한 경우를 빼면 주석을 커밋하지 않습니다. 쓰지 않는 코드를 남기지 않습니다.

## 9. Git 규칙

### 작업 유형

| 유형 | 사용 기준 |
|---|---|
| `feat` | 새로운 기능 구현 |
| `docs` | README, 문서 수정 |
| `fix` | 버그 수정 |
| `refactor` | 동작을 바꾸지 않는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `hotfix` | 즉시 고쳐야 하는 치명적인 버그 |
| `chore` | 설정, 의존성, 배포 등 기타 작업 |

새 API를 만드는 작업은 `feat`를 쓰고, 이슈와 PR에 `API` 라벨을 붙입니다.

### 브랜치

- 이슈를 만든 뒤 이슈 번호에 맞는 브랜치를 만듭니다.
- 형식: `<유형>/#<이슈번호>-<작업내용>` (예: `feat/#2-duty-calculator`)
- 작업 브랜치에서 `main`으로 PR을 보냅니다. `develop` 브랜치는 쓰지 않습니다.

### 커밋

- 형식: `<유형>: #<이슈번호> <한글 요약>` (예: `feat: #2 관부가세 예상 계산 API 구현`)
- 제목은 50자 이내로 씁니다. 필요하면 본문에 자세히 적습니다.
- 목적이 다른 변경은 커밋을 나눕니다.
- 빌드나 테스트가 실패하는 상태로 커밋하지 않습니다.
- 사용자가 요청할 때만 커밋하고 푸시합니다.
- 커밋 메시지에 에이전트 관련 문구(Co-Authored-By, "Generated with ..." 등)를 넣지 않습니다.

### 이슈와 PR

- 이슈 제목: `[유형] 작업 내용` (예: `[Feat] 관부가세 예상 계산 API`)
- PR 제목: `[유형/#이슈번호] 작업 내용` (예: `[Feat/#2] 관부가세 예상 계산 API`)
- PR 본문의 `## 🔗 관련 이슈`에 `- Resolved: #이슈번호`를 적습니다.
- API를 바꾼 PR에는 바뀐 엔드포인트와 요청·응답 예시를 적고, 웹·iOS에서 타입을 다시 만들어야 하는지 표시합니다.
- 1인 개발이므로 PR은 스스로 한 번 더 읽고 머지합니다.
- PR 설명에 에이전트 관련 문구를 넣지 않습니다.

## 10. 설계 결정 기록

- 구조나 정책을 정하는 결정(인증 방식, 암호화 방식, 외부 API 선택 등)은 `docs/decisions/<번호>-<제목>.md`에 짧게 남깁니다.
- 형식: 배경 → 결정 → 이유 → 버린 대안

## 11. 끝내기 전 확인

1. 2번의 명령이 성공했고 테스트가 모두 통과했는가
2. 새 이름이 4번의 표와 맞는가
3. Controller에 로직이 없고, Entity를 응답에 그대로 쓰지 않았는가
4. 금액을 `BigDecimal`로 다뤘고, 기준값을 한곳에 뒀는가
5. 개인정보를 암호화해 저장하고 로그에 남기지 않았는가
6. 다른 셀러의 데이터에 접근할 수 없는지 확인했는가
7. 비밀값이 코드나 설정 파일에 들어가지 않았는가
8. API를 바꿨다면 OpenAPI 명세에 설명이 붙었고, 사용자에게 클라이언트 타입 재생성을 알렸는가
