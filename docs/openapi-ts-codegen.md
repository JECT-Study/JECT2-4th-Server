# OpenAPI Spec → TypeScript 클라이언트 코드 생성

Spring Boot가 자동 생성하는 OpenAPI spec(`swagger.json`)으로부터 TypeScript axios 클라이언트를 생성하는 방법을 설명합니다.

## 디렉토리 구조

```
project-root/
├── swagger.json                  # Spring Boot(Springdoc)가 생성하는 OpenAPI 3.0 spec
├── openapi-generator/            # 코드 생성 설정 디렉토리
│   ├── openapitools.json         # openapi-generator-cli 버전 고정
│   ├── package.json              # 생성될 npm 패키지 메타데이터 템플릿
│   ├── tsconfig.json             # TypeScript 컴파일 옵션
│   └── modelGeneric.mustache     # 커스텀 모델 Mustache 템플릿
└── generated-api-client/         # 생성 결과물 (자동 생성, 직접 수정 금지)
    ├── api/                      # Controller별 API 함수
    │   ├── auth-controller-api.ts
    │   └── hello-controller-api.ts
    ├── models/                   # DTO 인터페이스
    │   └── hello-response.ts
    ├── configuration.ts          # axios 기본 설정 클래스
    ├── common.ts                 # 내부 유틸 함수
    ├── base.ts                   # BaseAPI 클래스
    └── index.ts                  # 통합 re-export
```

## swagger.json 생성

Spring Boot 앱을 실행하면 Springdoc이 자동으로 OpenAPI spec을 노출합니다.

```bash
# 앱 실행 후 spec 다운로드
curl http://localhost:8080/v3/api-docs > swagger.json
```

앱 내 `@Operation`, `@Schema` 어노테이션으로 spec 내용을 보강할 수 있습니다.

## TypeScript 클라이언트 생성

### 사전 준비

```bash
cd openapi-generator
npm install
```

### 생성 명령

```bash
# project root에서 실행
npx --prefix openapi-generator @openapitools/openapi-generator-cli generate \
  -i swagger.json \
  -g typescript-axios \
  -o generated-api-client \
  -t openapi-generator \
  --additional-properties=withSeparateModelsAndApi=true,modelPackage=models,apiPackage=api,npmName=@ject-4-vs-team/api-client,npmVersion=0.0.1
```

| 옵션 | 설명 |
|------|------|
| `-i swagger.json` | 입력 OpenAPI spec 파일 |
| `-g typescript-axios` | typescript-axios 제너레이터 사용 |
| `-o generated-api-client` | 출력 디렉토리 |
| `-t openapi-generator` | 커스텀 Mustache 템플릿 디렉토리 |
| `withSeparateModelsAndApi=true` | `api/`와 `models/` 디렉토리 분리 |
| `npmName` | 생성될 npm 패키지 이름 |

### npm 패키지 빌드

```bash
cd generated-api-client
npm install
npm run build   # dist/ 디렉토리에 컴파일 결과 생성
```

## 커스텀 템플릿 (modelGeneric.mustache)

기본 제너레이터는 숫자 enum을 일반 객체로 생성하지만, `modelGeneric.mustache`를 통해 string enum은 TypeScript `enum`, 숫자 enum은 `as const` 패턴으로 생성합니다.

```typescript
// string enum → TypeScript enum
export enum Status {
    Active = 'ACTIVE',
    Inactive = 'INACTIVE',
}

// 숫자 enum → as const 패턴 (타입 안전성 유지)
export const Priority = {
    Low: 1,
    High: 2,
} as const;

export type Priority = typeof Priority[keyof typeof Priority];
```

## 프론트엔드에서 사용

```typescript
import { HelloControllerApi, AuthControllerApi, Configuration } from '@ject-4-vs-team/api-client';

const config = new Configuration({
    basePath: 'https://api.example.com',
    accessToken: () => localStorage.getItem('token') ?? '',
});

const helloApi = new HelloControllerApi(config);
const authApi = new AuthControllerApi(config);

// API 호출
const response = await helloApi.hello();
console.log(response.data.message);
```

## API 변경 시 워크플로우

```
Spring Boot API 변경
       ↓
앱 실행 후 swagger.json 재다운로드
       ↓
openapi-generator-cli generate 재실행
       ↓
generated-api-client/ 변경분 커밋
       ↓
프론트엔드에서 타입 변경 반영
```

`generated-api-client/` 하위 파일은 자동 생성 결과물이므로 직접 수정하지 않습니다. 변경이 필요하면 Mustache 템플릿(`openapi-generator/*.mustache`) 또는 `--additional-properties` 옵션을 수정한 뒤 재생성합니다.

## CLI 버전 고정

`openapi-generator/openapitools.json`에서 CLI 버전을 고정해 팀원 간 동일한 결과물을 보장합니다.

```json
{
  "generator-cli": {
    "version": "7.20.0"
  }
}
```

버전을 올릴 때는 생성 결과물의 변경 범위를 확인하고 프론트엔드 영향도를 함께 검토합니다.
