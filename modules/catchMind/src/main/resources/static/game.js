// [CatchMind] game.js

// --- Theme Logic ---
function toggleTheme() {
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    document.getElementById('themeBtn').innerText = isDark ? 'Light' : 'Dark';
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
}
if (localStorage.getItem('theme') === 'dark') {
    document.body.classList.add('dark-mode');
    document.getElementById('themeBtn').innerText = 'Light';
}

// --- Game Logic ---
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}
let stompClient = null;
let myNickname = "";
const myUniqueId = generateUUID();
let currentRoomId = "";
let isMyTurn = false;
let isGameEnded = false;

const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
let isDrawing = false;
let lastX = 0, lastY = 0;

function goToLobby() {
    const input = document.getElementById('nicknameInput').value;
    if (!input.trim()) return showAlert("닉네임을 입력하세요!");
    myNickname = input;
    document.getElementById('welcome-msg').innerText = `플레이어: ${myNickname}`;
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('lobby-screen').classList.remove('hidden');
    loadRooms();
}

function loadRooms() {
    fetch('/catchmind/api/rooms').then(res => res.json()).then(rooms => {
        const list = document.getElementById('room-list');

        // [수정] 방이 없을 때 메시지 통일
        if (!rooms.length) {
            list.innerHTML = '<li style="padding:20px; text-align:center; color:var(--text-secondary);">개설된 방이 없습니다.</li>';
        } else {
            list.innerHTML = '';
            rooms.forEach(room => {
                const li = document.createElement('li');
                li.className = 'room-item';
                li.innerHTML = `<span style="font-weight:600;">${room.roomName}</span> <button class="btn-default" onclick="joinRoom('${room.roomId}', '${room.roomName}')" style="font-size:12px;">참가</button>`;
                list.appendChild(li);
            });
        }
    });
}

function createRoom() {
    const name = document.getElementById('roomNameInput').value;
    const rounds = document.getElementById('roundsInput').value;
    if(!name) return showAlert("방 제목을 입력하세요!");
    fetch(`/catchmind/api/rooms?name=${encodeURIComponent(name)}&rounds=${rounds}`, { method: 'POST' })
        .then(res => res.json())
        .then(room => joinRoom(room.roomId, room.roomName));
}

function joinRoom(roomId, roomName) {
    fetch(`/catchmind/api/rooms/${roomId}`)
        .then(res => {
            if (!res.ok) throw new Error("방을 찾을 수 없습니다.");
            return res.json();
        })
        .then(room => {
            if (room.playing) return showAlert("이미 게임이 진행 중입니다!");
            enterRoomProcess(roomId, roomName);
        })
        .catch(err => { showAlert(err.message); loadRooms(); });
}

function enterRoomProcess(roomId, roomName) {
    currentRoomId = roomId;
    isGameEnded = false;
    document.getElementById('room-title-text').innerText = roomName;
    document.getElementById('lobby-screen').classList.add('hidden');
    document.getElementById('game-screen').classList.remove('hidden');

    const socket = new SockJS('/catchmind/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        stompClient.send(`/app/${roomId}/join`, {}, JSON.stringify({ type: 'JOIN', sender: myNickname, senderId: myUniqueId }));

        stompClient.subscribe(`/topic/${roomId}/draw`, function (msg) {
            if(isGameEnded) return;
            const body = JSON.parse(msg.body);
            if (body.type === 'DRAW' && body.senderId !== myUniqueId) {
                drawLine(body.prevX, body.prevY, body.x, body.y, body.color, false);
            } else if (body.type === 'CLEAR') {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }
        });

        stompClient.subscribe(`/topic/${roomId}/chat`, function (msg) {
            const body = JSON.parse(msg.body);
            if (body.type === 'KICK') { if (body.senderId === myUniqueId) { showAlert(body.content); exitRoom(); } return; }
            if (body.type === 'GAME_OVER') { isGameEnded = true; showRanking(body.rankings); return; }
            if (isGameEnded) return;

            if (body.type === 'START') { handleGameStart(body); }
            else if (body.type === 'SELECT_WORD') { handleSelectWord(body); }
            else { showChat(body.sender, body.content); }
        });
    });
}

function startGame() { stompClient.send(`/app/${currentRoomId}/start`, {}, JSON.stringify({ sender: myNickname, senderId: myUniqueId })); }

function resetGameState() {
    isMyTurn = false; isDrawing = false;
    document.getElementById('chatInput').disabled = false;
    document.getElementById('canvas-container').classList.remove('my-turn');
    document.getElementById('secret-area').style.display = 'none';
    document.getElementById('startBtn').style.display = 'none';
}

function handleSelectWord(msg) {
    resetGameState();
    if(msg.currentRound) document.getElementById('game-status').innerText = `Round ${msg.currentRound}`;
    if (msg.drawerId === myUniqueId) {
        document.getElementById('word-select-area').style.display = 'block';
        const btnArea = document.getElementById('candidate-buttons');
        btnArea.innerHTML = '';
        msg.candidates.forEach(word => {
            const btn = document.createElement('button');
            btn.className = 'btn-default'; btn.innerText = word;
            btn.onclick = () => selectWord(word);
            btnArea.appendChild(btn);
        });
        showChat("SYSTEM", "주제어를 선택하세요!");
    } else {
        showChat("SYSTEM", "출제자가 단어를 선택 중입니다...");
    }
}

function selectWord(word) {
    stompClient.send(`/app/${currentRoomId}/choose`, {}, JSON.stringify({ senderId: myUniqueId, content: word }));
    document.getElementById('word-select-area').style.display = 'none';
}

function sendManualWord() {
    const word = document.getElementById('manualWordInput').value.trim();
    if(!word) return;
    stompClient.send(`/app/${currentRoomId}/input`, {}, JSON.stringify({ senderId: myUniqueId, content: word }));
    document.getElementById('word-select-area').style.display = 'none';
}

function handleGameStart(msg) {
    document.getElementById('word-select-area').style.display = 'none';
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    showChat('SYSTEM', msg.content);
    const statusSpan = document.getElementById('game-status');
    if (msg.drawerId === myUniqueId) {
        isMyTurn = true;
        statusSpan.innerText = "그리는 중";
        statusSpan.style.background = "#2da44e";
        document.getElementById('secret-area').style.display = 'inline-block';
        document.getElementById('secret-text').innerText = msg.answer;
        document.getElementById('canvas-container').classList.add('my-turn');
        document.getElementById('chatInput').disabled = true;
    } else {
        isMyTurn = false;
        statusSpan.innerText = `출제자: ${msg.drawer}`;
        statusSpan.style.background = "#6e7681";
    }
}

function showRanking(rankings) {
    const modal = document.getElementById('ranking-modal');
    modal.classList.remove('hidden');

    // [추가된 부분] 🎉 빵파레(폭죽) 효과 발사!
    // 모달(z-index:999)보다 위에 보이도록 zIndex를 높게 설정합니다.
    var duration = 3 * 1000; // 3초 동안
    var animationEnd = Date.now() + duration;
    var defaults = { startVelocity: 30, spread: 360, ticks: 60, zIndex: 9999 };

    function randomInRange(min, max) {
        return Math.random() * (max - min) + min;
    }

    var interval = setInterval(function() {
        var timeLeft = animationEnd - Date.now();

        if (timeLeft <= 0) {
            return clearInterval(interval);
        }

        var particleCount = 50 * (timeLeft / duration);
        // 양쪽에서 팡팡 터지게 설정
        confetti(Object.assign({}, defaults, { particleCount, origin: { x: randomInRange(0.1, 0.3), y: Math.random() - 0.2 } }));
        confetti(Object.assign({}, defaults, { particleCount, origin: { x: randomInRange(0.7, 0.9), y: Math.random() - 0.2 } }));
    }, 250);

    // [기존 코드] 랭킹 리스트 그리기
    const list = document.getElementById('ranking-list');
    list.innerHTML = '';
    rankings.forEach((p, i) => {
        const li = document.createElement('li');
        li.style.cssText = "padding:10px; border-bottom:1px solid var(--border-color); display:flex; justify-content:space-between;";
        const rankIcon = i === 0 ? '👑 ' : `<b>#${i+1}</b> `;
        li.innerHTML = `<span>${rankIcon}${p.nickname}</span> <span style="color:var(--btn-primary-bg); font-weight:bold;">${p.point} pts</span>`;
        list.appendChild(li);
    });
}

function closeRanking() { document.getElementById('ranking-modal').classList.add('hidden'); exitRoom(); }

function drawLine(x1, y1, x2, y2, color, emit) {
    ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2);
    ctx.strokeStyle = color; ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.stroke(); ctx.closePath();
    if (emit) stompClient.send(`/app/${currentRoomId}/draw`, {}, JSON.stringify({ type: 'DRAW', sender: myNickname, senderId: myUniqueId, prevX: x1, prevY: y1, x: x2, y: y2, color: color }));
}

canvas.addEventListener('mousedown', e => { if(isMyTurn){ isDrawing=true; lastX=e.offsetX; lastY=e.offsetY; } });
canvas.addEventListener('mousemove', e => { if(isDrawing && isMyTurn){ drawLine(lastX, lastY, e.offsetX, e.offsetY, document.getElementById('colorPicker').value, true); lastX=e.offsetX; lastY=e.offsetY; } });
canvas.addEventListener('mouseup', () => isDrawing=false);
canvas.addEventListener('mouseout', () => isDrawing=false);

function clearCanvas() {
    if(!isMyTurn) return;
    ctx.clearRect(0,0,canvas.width,canvas.height);
    stompClient.send(`/app/${currentRoomId}/draw`, {}, JSON.stringify({ type: 'CLEAR', sender: myNickname, senderId: myUniqueId }));
}

function sendChat() {
    const val = document.getElementById('chatInput').value.trim();
    if(!val) return;
    stompClient.send(`/app/${currentRoomId}/chat`, {}, JSON.stringify({ type: 'CHAT', sender: myNickname, senderId: myUniqueId, content: val }));
    document.getElementById('chatInput').value = '';
}

function showChat(sender, msg) {
    const div = document.createElement('div');
    div.className = sender === 'SYSTEM' ? 'msg-system' : 'msg-item';
    div.innerHTML = sender === 'SYSTEM' ? msg : `<span class="msg-sender" style="font-weight:700;">${sender}</span>: ${msg}`;
    document.getElementById('messages').appendChild(div);
    document.getElementById('messages').scrollTop = document.getElementById('messages').scrollHeight;
}

function exitRoom() { if(stompClient) stompClient.disconnect(); location.reload(); }

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