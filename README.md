# 🎮 Web Mini-Game Hub (Multiplayer)

> **실시간 웹소켓 기반의 멀티플레이어 게임 플랫폼** > 캐치마인드(그림 퀴즈)와 끄투(끝말잇기)를 하나의 허브에서 즐길 수 있습니다.

![Project Status](https://img.shields.io/badge/Status-Active-success)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-Gateway-009639?logo=nginx&logoColor=white)

## 📋 프로젝트 소개

이 프로젝트는 **MSA(Microservices Architecture) 지향적 구조**로 설계된 웹 게임 플랫폼입니다.  
Nginx를 리버스 프록시(Gateway)로 사용하여 트래픽을 라우팅하며, 각 게임(캐치마인드, 끄투)은 독립적인 Spring Boot 컨테이너에서 동작합니다. 모든 데이터는 공유된 MySQL 데이터베이스에서 관리됩니다.

### 🕹️ 포함된 게임
1.  **🎨 캐치마인드 (CatchMind)**: 출제자가 제시어를 그리면 다른 플레이어들이 실시간으로 맞추는 게임.
2.  **🗣️ 끄투 (KKutu)**: 끝말잇기 규칙을 기반으로, 봇 또는 다른 플레이어와 대결하는 게임.

---

## 🛠️ 기술 스택 (Tech Stack)

### **Frontend**
* **Core**: HTML5, CSS3, Vanilla JavaScript (ES6+)
* **Communication**: WebSocket (SockJS, STOMP)
* **Graphics**: HTML5 Canvas API (드로잉 구현), `canvas-confetti` (이펙트)
* **Design**: Responsive UI, Dark Mode Support

### **Backend**
* **Framework**: Java 17, Spring Boot 3.x
* **Database**: MySQL 8.0, JPA (Hibernate)
* **Real-time**: Spring WebSocket (STOMP Broker)

### **Infrastructure**
* **Container**: Docker, Docker Compose
* **Web Server**: Nginx (Reverse Proxy, Static File Serving)

---

## 🏗️ 시스템 아키텍처 (Architecture)

```mermaid
graph LR
    User[User / Browser] -- Port 80 (HTTP/WS) --> Nginx[Nginx Gateway]
    
    subgraph Docker Network
        Nginx -- /catchmind --> CM[CatchMind App]
        Nginx -- /KKUTU --> KK[KKutu App]
        
        CM -- JDBC --> DB[(Shared MySQL)]
        KK -- JDBC --> DB
    end
```
* **Nginx**: 메인 허브 UI 제공 및 `/catchmind`, `/KKUTU` 경로에 따른 리버스 프록시 역할. 웹소켓(Upgrade 헤더) 처리가 구성되어 있습니다.
* **Spring Boot Apps**: 각 게임의 비즈니스 로직, 소켓 통신, 게임 룸 상태 관리를 담당합니다.
* **MySQL**: `kkutu_db`, `catchmind_db` 스키마를 통해 데이터를 분리하여 저장합니다.

---

## ✨ 주요 기능 (Key Features)

### 1. 공통 기능
* **게임 허브**: 직관적인 메인 화면에서 게임 선택 및 이동.
* **다크 모드**: 눈이 편안한 테마 전환 기능.
* **반응형 웹**: 모바일과 데스크톱 환경 지원.

### 2. 캐치마인드
* **실시간 드로잉 동기화**: Canvas API 좌표 데이터를 웹소켓으로 전송하여 끊김 없는 그림 공유.
* **게임 로직**: 방 생성/입장, 라운드 관리, 정답 판독 및 점수 계산.
* **채팅**: 실시간 채팅 및 정답 입력.
* **랭킹 시스템**: 게임 종료 후 점수 집계 및 폭죽(Confetti) 효과.

### 3. 끄투 (끝말잇기)
* **단어 검증 시스템**: DB에 저장된 방대한 단어 데이터를 기반으로 유효성 검사.
* **봇(Bot) 대전**: 혼자서도 즐길 수 있는 AI 봇 기능 (난이도 조절 가능).
* **턴 관리**: 제한 시간 타이머 및 턴 넘기기 시스템.
* **두음법칙**: 유연한 게임 진행 지원.

---

## 🚀 실행 방법 (Getting Started)

이 프로젝트는 **Docker Compose**를 통해 원터치로 실행할 수 있습니다.

### 사전 요구사항
* Docker & Docker Compose가 설치되어 있어야 합니다.

### 설치 및 실행

**1. 프로젝트 클론**
```bash
git clone [https://github.com/username/game-hub.git](https://github.com/username/game-hub.git)
cd game-hub
```
**2. 컨테이너 빌드 및 실행**
```bash
docker compose up -d --build
```
최초 실행 시 MySQL 초기화(init.sql, korean_kr.sql)로 인해 시간이 소요될 수 있습니다.

**3. 접속**

`브라우저 주소창에 http://localhost 입력.`

**🛑 실행 종료**
```Bash
docker compose down
```
`(데이터까지 초기화하려면 docker compose down -v)`



### 📂 프로젝트 구조 (Directory Structure)
```
.
├── docker-compose.yml       # 컨테이너 오케스트레이션 설정
├── db/                      # DB 초기화 스크립트
│   └── init.sql             # DB 스키마 생성
├── nginx/                   # Nginx 설정 및 정적 파일
│   ├── nginx.conf           # 라우팅 및 웹소켓 설정
│   └── html/                # 메인 허브 화면 (index.html)
├── catchmind/               # 캐치마인드 백엔드 프로젝트
│   └── src/main/resources/static/ # 프론트엔드 소스 (HTML/JS)
└── kkutu/                   # 끄투 백엔드 프로젝트
    ├── db-init/             # 끄투 단어 데이터 (korean_kr.sql)
    └── src/main/resources/static/ # 프론트엔드 소스 (HTML/JS)
```

### 🐛 트러블슈팅 (Troubleshooting)
* Q. **끄투 봇이 시작하자마자 죽어요.**

* A. **DB에 단어 데이터가 없어서 그렇습니다. docker compose down -v로 볼륨을 초기화한 후 다시 실행하면 korean_kr.sql이 로드되어 해결됩니다.**

* Q. **웹소켓 연결이 안 돼요 (404 Error).**

* A. **Nginx 설정에서 Upgrade 및 Connection 헤더 설정이 올바른지 확인해야 합니다. 현재 프로젝트에는 이미 적용되어 있습니다.**

---
### DEMO
https://github.com/YeeDochi/minigames/issues/1#issue-3729316556

---

👤 Author
Yeedochi - Main Developer

License: MIT
