// [Face Gomoku] game.js - Final Fix

function toggleTheme() {
    document.body.classList.toggle('dark-mode');
    localStorage.setItem('theme', document.body.classList.contains('dark-mode') ? 'dark' : 'light');
}
if (localStorage.getItem('theme') === 'dark') document.body.classList.add('dark-mode');

function generateUUID() { return Math.random().toString(36).substr(2, 9); }

let stompClient = null;
let myNickname = "";
let myUniqueId = generateUUID();
let currentRoomId = "";
let mySkinUrl = "";
let myStoneType = 0;     // 1: 흑, 2: 백, 0: 관전
let currentTurn = 1;     // 1: 흑 차례, 2: 백 차례
let isGameEnded = false;

// 오목판 설정
const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const BOARD_SIZE = 15;
const CELL_SIZE = 40;
const PADDING = 20;

// --- 1. 입장 및 업로드 ---
function uploadAndEnter() {
    const nick = document.getElementById('nicknameInput').value.trim();
    if (!nick) return showAlert("닉네임을 입력하세요.");
    myNickname = nick;

    const fileInput = document.getElementById('skinInput');
    if (fileInput.files.length > 0) {
        const formData = new FormData();
        formData.append("file", fileInput.files[0]);

        // Nginx 경로 /Oh_Mock 포함
        fetch('/Oh_Mock/api/upload', { method: 'POST', body: formData })
            .then(res => res.text())
            .then(url => {
                mySkinUrl = url;
                enterLobby();
            })
            .catch(err => {
                console.error("Upload failed:", err);
                enterLobby();
            });
    } else {
        enterLobby();
    }
}

function enterLobby() {
    document.getElementById('welcome-msg').innerText = `환영합니다, ${myNickname}님!`;
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('lobby-screen').classList.remove('hidden');
    loadRooms();
}

function loadRooms() {
    fetch('/Oh_Mock/api/rooms').then(res => res.json()).then(rooms => {
        const list = document.getElementById('room-list');
        list.innerHTML = rooms.length ? '' : '<li style="padding:20px; text-align:center;">방이 없습니다.</li>';
        rooms.forEach(room => {
            const li = document.createElement('li');
            li.className = 'room-item';
            li.innerHTML = `<span>${room.roomName}</span> 
                            <button class="btn-default" onclick="joinRoom('${room.roomId}', '${room.roomName}')">입장</button>`;
            list.appendChild(li);
        });
    });
}

function createRoom() {
    const name = document.getElementById('roomNameInput').value;
    if(!name) return showAlert("방 제목을 입력하세요.");
    fetch(`/Oh_Mock/api/rooms?name=${encodeURIComponent(name)}`, { method: 'POST' })
        .then(res => res.json())
        .then(room => joinRoom(room.roomId, room.roomName));
}

function joinRoom(roomId, roomName) {
    currentRoomId = roomId;
    document.getElementById('room-title-text').innerText = roomName;
    document.getElementById('lobby-screen').classList.add('hidden');
    document.getElementById('game-screen').classList.remove('hidden');

    drawBoard();
    connectSocket();
}

// --- 2. 웹소켓 연결 (순서 중요!) ---
function connectSocket() {
    const socket = new SockJS('/Oh_Mock/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // 로그 끄기 (깔끔하게)

    stompClient.connect({}, function () {
        console.log("Connected via WebSocket");

        // [중요] 구독을 먼저 해야 메시지를 안 놓칩니다.

        // 1. 착수 정보 구독
        stompClient.subscribe(`/topic/${currentRoomId}/stone`, function (msg) {
            const body = JSON.parse(msg.body);
            console.log("착수 수신:", body); // 디버깅용 로그

            // 서버에서 받은 stoneType(1=흑, 2=백)으로 그리기
            renderStone(body.row, body.col, body.skinUrl, body.stoneType);

            // 턴 넘기기
            currentTurn = (body.stoneType === 1) ? 2 : 1;
            updateTurnIndicator();
        });

        // 2. 채팅 정보 구독
        stompClient.subscribe(`/topic/${currentRoomId}/chat`, function (msg) {
            const body = JSON.parse(msg.body);
            handleChatMessage(body);
        });

        // [중요] 구독 완료 후 입장 메시지 전송
        stompClient.send(`/app/${currentRoomId}/join`, {}, JSON.stringify({
            type: 'JOIN', sender: myNickname, senderId: myUniqueId, skinUrl: mySkinUrl
        }));
    });
}

// --- 3. 렌더링 로직 (핵심 수정) ---
function drawBoard() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#FAFCFA";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = "#000";
    ctx.lineWidth = 1;

    ctx.beginPath();
    for (let i = 0; i < BOARD_SIZE; i++) {
        ctx.moveTo(PADDING, PADDING + i * CELL_SIZE);
        ctx.lineTo(PADDING + (BOARD_SIZE - 1) * CELL_SIZE, PADDING + i * CELL_SIZE);
        ctx.moveTo(PADDING + i * CELL_SIZE, PADDING);
        ctx.lineTo(PADDING + i * CELL_SIZE, PADDING + (BOARD_SIZE - 1) * CELL_SIZE);
    }
    ctx.stroke();
}

// [수정] 돌 그리기 함수 - 색깔 구분 강화
function renderStone(row, col, imageUrl, stoneType) {
    const x = PADDING + col * CELL_SIZE;
    const y = PADDING + row * CELL_SIZE;
    const radius = 17;

    // 기본 원 그리기 함수
    const drawCircle = (color, strokeColor) => {
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();
        ctx.strokeStyle = strokeColor || "#000";
        ctx.lineWidth = 1;
        ctx.stroke();

        // 입체감 (그림자)
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.shadowColor = "rgba(0,0,0,0.3)";
        ctx.shadowBlur = 5;
        ctx.shadowOffsetX = 2;
        ctx.shadowOffsetY = 2;
        ctx.stroke();
        ctx.shadowColor = "transparent"; // 초기화
    };

    // 흑/백 색상 결정 (stoneType이 1이면 흑, 2면 백)
    // 숫자가 문자로 올 수도 있으니 == 비교 사용
    const color = (stoneType == 1) ? "#000000" : "#ffffff";
    const stroke = "#000000";

    if (imageUrl) {
        const img = new Image();
        // CORS 문제 방지
        img.crossOrigin = "Anonymous";
        img.src = imageUrl;

        img.onload = () => {
            ctx.save();
            ctx.beginPath();
            ctx.arc(x, y, radius, 0, Math.PI * 2);
            ctx.clip(); // 원형으로 자르기
            ctx.drawImage(img, x - radius, y - radius, radius * 2, radius * 2);
            ctx.restore();

            // 테두리 그려서 깔끔하게
            ctx.beginPath();
            ctx.arc(x, y, radius, 0, Math.PI * 2);
            ctx.strokeStyle = "rgba(0,0,0,0.2)";
            ctx.stroke();
        };

        // 이미지 로드 실패 시 -> 기본 돌 그리기
        img.onerror = () => {
            console.warn("이미지 로드 실패, 기본 돌로 대체:", imageUrl);
            drawCircle(color, stroke);
        };
    } else {
        // 이미지 없을 시 -> 바로 기본 돌 그리기
        drawCircle(color, stroke);
    }
}

// --- 4. 이벤트 및 메시지 처리 ---
canvas.addEventListener('click', e => {
    if (isGameEnded) return;
    if (myStoneType === 0) return showChat("SYSTEM", "관전자는 돌을 둘 수 없습니다.");
    if (myStoneType != currentTurn) return showChat("SYSTEM", "상대방 차례입니다!");

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const col = Math.round((x - PADDING) / CELL_SIZE);
    const row = Math.round((y - PADDING) / CELL_SIZE);

    if (col < 0 || col >= BOARD_SIZE || row < 0 || row >= BOARD_SIZE) return;

    // 서버로 전송
    stompClient.send(`/app/${currentRoomId}/stone`, {}, JSON.stringify({
        sender: myNickname,
        senderId: myUniqueId,
        row: row,
        col: col,
        stoneType: myStoneType,
        skinUrl: mySkinUrl
    }));
});

function handleChatMessage(msg) {
    if (msg.senderId === myUniqueId && msg.stoneType) {
        myStoneType = msg.stoneType;
        const typeText = myStoneType === 1 ? "흑돌 (⚫)" : (myStoneType === 2 ? "백돌 (⚪)" : "관전 모드");
        document.getElementById('my-stone-status').innerText = typeText;
        if(myStoneType === 0) document.getElementById('startBtn').style.display = 'none';
        if (myStoneType !== 0) {
            document.getElementById('startBtn').style.display = 'inline-block';
        } else {
            document.getElementById('startBtn').style.display = 'none';
        }
    }
    if (msg.type === 'JOIN') {
        // msg.stoneType이 1이면 흑, 2면 백
        if (msg.stoneType) {
            updatePlayerProfile(msg.stoneType, msg.sender, msg.skinUrl);
        }
    }

    // [추가] 착수(STONE) 메시지 처리: 게임 중 싱크가 안 맞을 경우를 대비해 갱신
    if (msg.type === 'STONE' && msg.stoneType) {
        // 돌을 둔 사람의 정보를 확실히 업데이트
        updatePlayerProfile(msg.stoneType, msg.sender, msg.skinUrl);
    }
    if (msg.type === 'START') {
        isGameEnded = false;
        currentTurn = 1;
        drawBoard();
        updateTurnIndicator();
        showChat("SYSTEM", msg.content);
        const startBtn = document.getElementById('startBtn');
        if(startBtn) startBtn.style.display = 'none';
    } else if (msg.type === 'GAME_OVER') {
        isGameEnded = true;
        document.getElementById('turn-indicator').style.display = 'none';
        fireConfetti();
        const modal = document.getElementById('ranking-modal');
        const img = document.getElementById('winnerImage');
        const name = document.getElementById('winnerName');

        const winnerName = msg.winnerName || msg.sender;
        const winnerSkin = msg.winnerSkin || msg.skinUrl;

        // 승리자 정보 주입
        img.src = winnerSkin || "https://placehold.co/150x150/000000/FFFFFF?text=WINNER";
        img.onerror = () => { img.src = "https://placehold.co/150x150/000000/FFFFFF?text=WINNER"; };
        name.innerText = winnerName;
        modal.classList.remove('hidden');
        showChat(msg.sender, msg.content);

        if (myStoneType !== 0) {
            const startBtn = document.getElementById('startBtn');
            if(startBtn) startBtn.style.display = 'inline-block';
        }
    } else if (msg.type === 'EXIT') {
        if(msg.senderId === myUniqueId) location.reload();
        else showChat("SYSTEM", msg.content);
    } else {
        showChat(msg.sender, msg.content);
    }
}

function updateTurnIndicator() {
    const indicator = document.getElementById('turn-indicator');
    if (!isGameEnded && myStoneType == currentTurn) {
        indicator.style.display = 'inline-block';
        indicator.innerText = "🚩 내 차례입니다!";
    } else {
        indicator.style.display = 'none';
    }
}

function startGame() {
    stompClient.send(`/app/${currentRoomId}/start`, {}, JSON.stringify({ sender: myNickname }));
}

function sendChat() {
    const val = document.getElementById('chatInput').value.trim();
    if (!val) return;
    stompClient.send(`/app/${currentRoomId}/chat`, {}, JSON.stringify({ sender: myNickname, senderId: myUniqueId, content: val }));
    document.getElementById('chatInput').value = '';
}

function showChat(sender, msg) {
    const div = document.createElement('div');
    div.className = sender === 'SYSTEM' ? 'msg-system' : 'msg-item';
    div.innerHTML = sender === 'SYSTEM' ? msg : `<b>${sender}</b>: ${msg}`;
    const container = document.getElementById('messages');
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function exitRoom() {
    if (stompClient) stompClient.send(`/app/${currentRoomId}/exit`, {}, JSON.stringify({ sender: myNickname, senderId: myUniqueId }));
    location.reload();
}

function fireConfetti() {
    const duration = 2000;
    const end = Date.now() + duration;
    (function frame() {
        confetti({ particleCount: 3, angle: 60, spread: 55, origin: { x: 0 } });
        confetti({ particleCount: 3, angle: 120, spread: 55, origin: { x: 1 } });
        if (Date.now() < end) requestAnimationFrame(frame);
    }());
}
function showAlert(msg) {
    const modal = document.getElementById('alert-modal');
    const text = document.getElementById('alert-msg-text');
    if (modal && text) {
        text.innerText = msg;
        modal.classList.remove('hidden'); // hidden 클래스 제거하여 표시
    } else {
        alert(msg); // 방어 코드
    }
}

function closeAlert() {
    const modal = document.getElementById('alert-modal');
    if (modal) modal.classList.add('hidden'); // hidden 클래스 추가하여 숨김
}

function updatePlayerProfile(stoneType, nickname, skinUrl) {
    // skinUrl이 없으면 기본 이미지 사용
    const defaultImg = stoneType === 1
        ? "https://placehold.co/40x40/000000/FFFFFF?text=B"
        : "https://placehold.co/40x40/FFFFFF/000000?text=W";

    const finalUrl = skinUrl || defaultImg;

    if (stoneType === 1) { // 흑돌
        document.getElementById('p1-name').innerText = nickname;
        document.getElementById('p1-img').src = finalUrl;
    } else if (stoneType === 2) { // 백돌
        document.getElementById('p2-name').innerText = nickname;
        document.getElementById('p2-img').src = finalUrl;
    }
}