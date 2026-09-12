# Mail Buddy

2025.10 ~ 2025.11

Gmail 등 메일함에 쌓인 약속·일정 정보를 AI가 자동으로 읽고 핵심만 추려
캘린더에 등록해주는 개인 비서형 웹 서비스의 백엔드 저장소입니다.
"메일은 하나, 일정 입력은 자동으로"라는 컨셉으로, 사용자가 메일을 일일이
읽고 캘린더에 옮겨 적는 번거로움을 줄이는 것을 목표로 합니다.

- 프론트엔드 저장소: [MAILBUDDY_FE](https://github.com/khyuna0/MAILBUDDY_FE) (React)
- 본 저장소: Spring Boot 기반 REST API + WebSocket 서버
- 이 문서는 별도의 EC2 배포 서버가 아닌, **로컬 환경에서 백엔드를 직접 구동하는 방법**을 기준으로 작성되었습니다.

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 이름 | Mail Buddy |
| 설명 | Google 메일에서 일정 정보를 AI로 추출해 캘린더에 자동 등록하고, 개인 일정·주소록·투두를 관리하는 서비스의 API 서버 |
| 대상 | Google 계정으로 메일을 받는 개인 사용자 |
| 핵심 가치 | 반복적인 "메일 읽고 → 일정 수동 등록" 과정을 자동화하여 본질적인 일에 집중할 수 있게 함 |

### 주요 기능

- **Google OAuth2 로그인 & 연동**: 구글 계정으로 로그인하고, Gmail 읽기 권한을 연동
- **메일 → 일정 자동 추출**: Gmail API로 메일을 가져와 AI(Mistral)로 제목·시간·장소를 요약/추출 후 일정으로 저장
- **일정(Schedule) 관리**: 캘린더 기반 일정 CRUD
- **주소록(Address) 관리**: 연락처 CRUD 및 연락 빈도 확인
- **투두(Todo) 관리**: 개인 작업 관리 API
- **실시간 1:1 문의 채팅**: STOMP/WebSocket 기반 채팅(문의자 ↔ 관리자), 채팅방(ChatRoom) 단위 관리
- **날씨/위치 연동**: 일정 장소의 좌표 기반 날씨 정보 제공(`WeatherController`), 주소 → 좌표 지오코딩(`GeocodeController`)
- **인증/보안**: JWT 기반 API 인증 + 세션 기반 OAuth2 인증 병행, 민감 데이터(구글 토큰 등) 암호화 저장

## 사용 기술 및 채택 이유

| 기술 | 용도 | 채택 이유 |
| --- | --- | --- |
| **Java 17 + Spring Boot 3.5** | 애플리케이션 프레임워크 | LTS 버전의 안정성과 Spring Boot의 자동 설정(Auto Configuration)으로 REST API·보안·JPA·웹소켓 등 다양한 기능을 빠르게 통합 |
| **Gradle** | 빌드 도구 | Maven 대비 간결한 빌드 스크립트와 빠른 증분 빌드, 의존성 관리 편의성 |
| **Spring Data JPA + Hibernate** | ORM/영속성 | 엔티티(User, Schedules, Chat 등) 중심 개발로 반복적인 SQL 작성 없이 CRUD 구현, `ddl-auto`로 로컬 개발 시 스키마 자동 반영 |
| **MySQL** | 데이터베이스 | 운영 환경과 동일한 RDB를 로컬에서도 사용해 쿼리/제약조건 차이로 인한 이슈를 사전에 방지 |
| **Spring Security + OAuth2 Client** | 인증/인가 | Google 계정 기반 소셜 로그인과 Gmail API 접근 권한(OAuth2 scope)을 표준화된 방식으로 처리 |
| **JJWT (jjwt-api/impl/jackson)** | JWT 발급/검증 | 세션에 의존하지 않는 무상태(stateless) API 인증을 구현해 프론트(SPA)와의 토큰 기반 통신에 적합 |
| **Jasypt (jasypt-spring-boot-starter)** | 엔티티 암호화 | 구글 access/refresh token처럼 DB에 저장되는 민감 정보를 양방향 암호화하여 저장(단방향 해시로는 복호화가 불가능하므로 토큰 재사용을 위해 양방향 암호화 채택) |
| **Spring WebSocket + STOMP** | 실시간 채팅 | 1:1 문의 기능에서 발행/구독(Pub/Sub) 기반 메시징으로 관리자-사용자 간 실시간 채팅을 구현, SimpleBroker로 별도 메시지 브로커 없이 경량 구현 |
| **Spring WebFlux (WebClient)** | 외부 API 비동기 호출 | Gmail API, Mistral AI API, 날씨 API 등 외부 REST API를 논블로킹 방식으로 호출해 응답 대기 중 스레드 낭비를 줄임 |
| **Mistral AI API** | 메일 요약/일정 추출 | 메일 본문에서 제목·시간·장소 등 정형 정보를 추출하는 데 LLM을 활용(기존 Gemini에서 전환), 비용/응답속도 균형을 고려해 선택 |
| **Spring Boot Starter Mail** | 메일 발송 | 회원가입/알림 등에서 SMTP 기반 메일 발송 기능 제공 |
| **org.json** | JSON 파싱 | 외부 API(Gmail 등) 응답 중 구조가 유동적인 JSON을 가볍게 파싱하기 위해 사용 |
| **Lombok** | 보일러플레이트 제거 | 엔티티/DTO의 getter·setter·생성자 코드를 어노테이션으로 대체해 가독성 향상 |

## 패키지 구조

```
src/main/java/com/example/mailbuddy/
├── config/       # Security, WebSocket, Jasypt, WebClient 등 전역 설정
├── controller/   # REST API 엔드포인트 (Auth, Schedules, Address, Todo, Chat, Weather 등)
├── dto/          # 요청/응답 DTO
├── entity/       # JPA 엔티티 (User, Schedules, Chat, ChatRoom, Gmail, Summary, TodoItem 등)
├── handler/       # 예외/이벤트 핸들러
├── jwt/          # JWT 생성/검증 유틸
├── repository/   # Spring Data JPA 리포지토리
├── service/      # 비즈니스 로직 (GeminiService(→Mistral 연동), GoogleTokenService 등)
└── utils/        # 공통 유틸
```

## 로컬 실행 방법

### 요구 사항

- JDK 17
- 로컬에 설치되어 실행 중인 MySQL 8.x

### 1. 저장소 클론

```bash
git clone https://github.com/khyuna0/MAILBUDDY-BE.git
cd MAILBUDDY-BE
```

### 2. 로컬 데이터베이스 생성

```sql
CREATE DATABASE IF NOT EXISTS mailbuddy CHARACTER SET utf8mb4;
```

### 3. 환경변수 설정

`src/main/resources/application.yml`은 모든 민감 설정값을 **환경변수로
오버라이드 가능한 플레이스홀더**로 두고 있습니다. 환경변수를 지정하지 않으면
아래 기본값(더미 값 포함)으로 부팅되어, 별도 설정 없이도 서버가 기동됩니다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SERVER_PORT` | `8888` | 서버 포트 |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/mailbuddy?...` | DB 접속 URL |
| `DB_USERNAME` | `root` | DB 계정 |
| `DB_PASSWORD` | *(빈 문자열)* | DB 비밀번호. **로컬 MySQL의 root 비밀번호로 반드시 지정하세요** |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | 더미 값 | [Google Cloud Console](https://console.cloud.google.com/apis/credentials)에서 발급 |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP 더미 값 | 메일 발송용 SMTP 계정 (Gmail 앱 비밀번호 권장) |
| `JASYPT_ENCRYPTOR_PASSWORD` | 로컬 개발용 임의 값 | 엔티티 암호화 키. 아무 문자열이나 가능 |
| `MISTRAL_API_KEY` / `MISTRAL_API_URL` / `MISTRAL_API_MODEL` | 더미 값 | [Mistral AI 콘솔](https://console.mistral.ai/)에서 발급한 API 키 |

> **더미 값으로 기동한 경우**: 서버는 정상적으로 뜨지만 구글 로그인,
> 메일→일정 자동 추출, 메일 발송 기능은 동작하지 않습니다. 회원가입/로그인
> (JWT), 일정 직접 등록, 주소록, 투두 등은 정상 동작합니다.

예시 (Windows PowerShell):

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
.\gradlew.bat bootRun
```

예시 (macOS/Linux):

```bash
DB_PASSWORD=your-local-mysql-password ./gradlew bootRun
```

### 4. 정상 기동 확인

```
http://localhost:8888
```

콘솔에 `Tomcat started on port 8888`, `Started MailbuddyApplication` 로그가
찍히면 정상 기동된 것입니다.

### 5. 프론트엔드 연동

프론트엔드([MAILBUDDY_FE](https://github.com/khyuna0/MAILBUDDY_FE))는
`http://localhost:8888`을 기본 API 서버로 바라보도록 설정되어 있어, 별도
설정 없이 프론트를 `npm start`로 띄우면 이 백엔드와 바로 연동됩니다.

## 기타

```bash
./gradlew build   # 빌드 및 테스트 실행
./gradlew test    # 테스트만 실행
```

## 트러블슈팅

### 1. 보안 방식 변경 (Spring Security 세션 인증 → JWT)

| 구분 | 내용 |
| --- | --- |
| 문제 | 세션 기반 인증으로는 로그인 자체가 정상적으로 동작하지 않았고, 특히 Google OAuth로 이동되면서 세션이 증발되어 로그인된 구글 아이디에 사용자 연결이 불가능했음 |
| 원인 | HTTPS 프론트 ↔ HTTP 백엔드 교차로 Secure 쿠키가 차단되어 로그인·세션 유지가 불가능. OAuth 리디렉션 과정에서 HTTP/HTTPS 혼재 + 도메인 불일치로 세션 쿠키가 계속 유실됨 |
| 해결 | Spring Security의 세션 기반 인증을 폐기하고, 모든 인증을 JWT + Token 방식으로 변경. 기존에 세션에 저장하던 구글 인증 토큰을 DB에 저장하는 방식으로 변경 |
| 배운 점 | OAuth 리다이렉트가 포함되는 환경에서는 HTTP/HTTPS 프로토콜이 단 1회라도 어긋나면 세션 기반 인증은 작동하지 않는다. 도메인 또는 프로토콜이 하나라도 다르면 세션 쿠키는 브라우저에서 원천 차단된다 |

### 2. 최종 인증 구조 (JWT 기반 인증 + DB 저장 방식)

| 구분 | 내용 |
| --- | --- |
| 문제 | 세션 기반 인증은 도메인·프로토콜 차이, OAuth 리디렉트, SameSite 정책 때문에 구조적으로 안정적인 인증 유지가 불가능했다 |
| 원인 | 프론트·백이 분리된 배포 구조이고, HTTPS/HTTP 혼재 + OAuth 리디렉트 환경에서 세션 쿠키는 브라우저 정책상 지속적으로 차단됨 |
| 해결 | Access Token + Refresh Token 구조로 변경, 세션 저장에서 DB 저장 구조로 변경. 웹소켓(STOMP) 등의 인증도 JWT로 검증하도록 통일 |
| 배운 점 | 분리 배포 환경에서는 JWT 기반 인증이 사실상의 표준이자 유일한 안정적 선택이다. 인증/토큰을 DB에서 통합 관리하면 토큰 만료·재발급·로그아웃 처리 등 유지보수성이 크게 향상된다 |

### 3. Gmail API 테스트 모드의 사용자 등록 제약

| 구분 | 내용 |
| --- | --- |
| 문제 | 테스트 서버에서는 Gmail API가 사전 등록된 특정 사용자만 인증되어, 일반 사용자의 이메일은 연동이 불가능했음 |
| 원인 | Gmail API 테스트 모드에서는 허용된 이메일만 호출 가능하며, 아무나 로그인하는 구조를 만들 수 없음 |
| 해결 | '문의하기' 기능을 만들어 사용자가 Gmail 연동을 신청하면, 관리자가 Google Cloud Console에 해당 사용자 이메일을 수동 등록하도록 운영 |
| 배운 점 | Gmail API는 테스트 단계에서 제한이 강해 프로덕션 모드 전환이 필수다. 정식 검증·승인 절차를 거쳐 누구나 Gmail 인증을 사용할 수 있는 구조를 배우고 구현해보고 싶다 |

### 4. Refresh Token을 도입해야 했던 이유

| 구분 | 내용 |
| --- | --- |
| 문제 | Access Token만 사용할 경우 만료될 때마다 사용자가 매번 구글 재로그인을 해야 함 |
| 원인 | Google OAuth Access Token은 1시간이면 만료되고, 자체 폼 로그인과 구글 로그인이 공존하는 이중 인증 구조였음 |
| 해결 | Refresh Token 발급 후 Access Token을 자동 재발급하도록 구현, 프론트/백 모든 인증 흐름을 JWT로 통일해 UX를 대폭 개선 |
| 배운 점 | OAuth와 자체 로그인이 공존할 때 Refresh Token 구조는 필수다 |

### 5. 낮은 가독성·유지보수성 (백엔드)

| 구분 | 내용 |
| --- | --- |
| 문제 | 날짜·시간 형식이 API 요청마다 달라 파싱 오류·정렬 문제가 반복. 데이터 정제 기준이 없어 저장·호출 과정에서도 일관성이 깨져 혼선이 발생 |
| 원인 | API 초기 설계 단계에서 날짜·시간·정렬 등 데이터 규칙 표준화가 부족했음 |
| 해결 | 날짜/시간 파싱·포맷 유틸을 별도로 제작해 저장·조회 기준을 통일. 팀원과 지속적으로 논의하며 API 규칙·데이터 포맷을 통일 |
| 배운 점 | 백엔드는 데이터 기준점이 되므로 형식·정제 규칙 통일이 필수다. 데이터 형식 규칙을 초기에 명확히 정해두는 것이 중요하며, 주석과 설명을 충분히 작성하는 것이 협업과 유지보수에 큰 도움이 된다 |
