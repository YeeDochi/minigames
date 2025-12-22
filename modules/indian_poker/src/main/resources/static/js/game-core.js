// [game-core.js] 전체 덮어쓰기
const Core = (function() {
    let stompClient = null;
    let myId = localStorage.getItem('myId');
    if (!myId) {
        myId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
        localStorage.setItem('myId', myId);
    }
    let myNickname = "";
    let currentRoomId = "";
    let GameImpl = null;
    let CONFIG = { apiPath: "", wsPath: "/ws" };

    function sendActionInternal(data) {
        if (!stompClient || !currentRoomId) return;
        stompClient.send(`/app/${currentRoomId}/action`, {}, JSON.stringify({
            type: 'ACTION',
            senderId: myId,
            sender: myNickname,
            data: data
        }));
    }

    function init(implementation, config) {
        GameImpl = implementation;
        if(config) {
            if(config.apiPath !== undefined) CONFIG.apiPath = config.apiPath;
            if(config.wsPath !== undefined) CONFIG.wsPath = config.wsPath;
            if(config.gameName) {
                const titleEl = document.getElementById('game-title-header');
                if(titleEl) titleEl.innerText = config.gameName;
            }
        }
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark') document.body.classList.add('dark-mode');
        else document.body.classList.remove('dark-mode');

        console.log("[GameCore] Initialized");
    }

    function login() {
        const input = document.getElementById('nicknameInput').value.trim();
        if (!input) return showAlert("닉네임을 입력하세요.");
        myNickname = input;
        document.getElementById('welcome-msg').innerText = ` ${myNickname}님`;
        document.getElementById('login-screen').classList.add('hidden');
        document.getElementById('lobby-screen').classList.remove('hidden');
        loadRooms();
    }

    function loadRooms() {
        fetch(`${CONFIG.apiPath}/api/rooms`)
            .then(res => res.json())
            .then(rooms => {
                const list = document.getElementById('room-list');
                if(list) {
                    list.innerHTML = '';
                    if (!rooms.length) list.innerHTML = '<li style="padding:15px; text-align:center; color:#888;">생성된 방이 없습니다.</li>';
                    rooms.forEach(r => {
                        const li = document.createElement('li');
                        li.className = 'room-item';
                        li.innerHTML = `<span style="font-weight:bold;">${r.roomName}</span> <button class="btn-default" onclick="Core.joinRoom('${r.roomId}', '${r.roomName}')">참가</button>`;
                        list.appendChild(li);
                    });
                }
            })
            .catch(err => showAlert("방 목록 로드 실패"));
    }

    function createRoom() {
        const name = document.getElementById('roomNameInput').value;
        if (!name) return showAlert("방 제목을 입력하세요.");
        fetch(`${CONFIG.apiPath}/api/rooms?name=${encodeURIComponent(name)}`, { method: 'POST' })
            .then(res => res.json())
            .then(room => joinRoom(room.roomId, room.roomName))
            .catch(err => showAlert("방 생성 실패: " + err));
    }

    function joinRoom(roomId, roomName) {
        fetch(`${CONFIG.apiPath}/api/rooms/${roomId}`)
            .then(res => res.json())
            .then(room => {
                currentRoomId = roomId;
                const titleText = document.getElementById('room-title-text');
                if(titleText) titleText.innerText = roomName;

                document.getElementById('lobby-screen').classList.add('hidden');
                document.getElementById('game-screen').classList.remove('hidden');

                // [수정] messages 요소가 있는지 확인 후 초기화 (에러 방지)
                const msgBox = document.getElementById('messages');
                if(msgBox) msgBox.innerHTML = '';

                // 게임별 초기화 로직 실행
                if (GameImpl.onEnterRoom) GameImpl.onEnterRoom();

                connectStomp(roomId);
            })
            .catch(err => showAlert("입장 실패: " + err));
    }

    function connectStomp(roomId) {
        const socket = new SockJS(CONFIG.wsPath);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function () {
            stompClient.send(`/app/${roomId}/join`, {}, JSON.stringify({ type: 'JOIN', sender: myNickname, senderId: myId }));
            stompClient.subscribe(`/topic/${roomId}`, function (msg) {
                handleCommonMessage(JSON.parse(msg.body));
            });
        }, function(error) {
            showAlert("서버 연결 끊김");
        });
    }

    function handleCommonMessage(msg) {
        if (msg.type === 'CHAT') showChat(msg.sender, msg.content);
        else if (msg.type === 'EXIT') showChat('SYSTEM', msg.content);
        else if (msg.type === 'GAME_OVER') {
            document.getElementById('ranking-modal').classList.remove('hidden');
            const wName = (msg.data && msg.data.winnerName) ? msg.data.winnerName : "Unknown";
            document.getElementById('winnerName').innerText = wName + " 승리!";
        }
        else {
            if (GameImpl.handleMessage) GameImpl.handleMessage(msg, myId);
        }
    }

    function sendChat() {
        const input = document.getElementById('chatInput');
        if (!input.value.trim()) return;
        stompClient.send(`/app/${currentRoomId}/chat`, {}, JSON.stringify({ type: 'CHAT', sender: myNickname, senderId: myId, content: input.value }));
        input.value = '';
    }

    function showChat(sender, msg) {
        const div = document.createElement('div');
        div.className = sender === 'SYSTEM' ? 'msg-system' : 'msg-item';
        div.innerHTML = sender === 'SYSTEM' ? msg : `<span style="font-weight:bold;">${sender}</span>: ${msg}`;
        const box = document.getElementById('messages');
        if(box) { box.appendChild(div); box.scrollTop = box.scrollHeight; }
    }

    function showAlert(msg) {
        document.getElementById('alert-msg-text').innerText = msg;
        document.getElementById('alert-modal').classList.remove('hidden');
    }
    function closeAlert() { document.getElementById('alert-modal').classList.add('hidden'); }
    function closeRanking() {
        document.getElementById('ranking-modal').classList.add('hidden');
        exitRoom();
    }
    function exitRoom() {
        if(stompClient) stompClient.disconnect();
        location.reload();
    }
    function toggleTheme() {
        document.body.classList.toggle('dark-mode');
        localStorage.setItem('theme', document.body.classList.contains('dark-mode') ? 'dark' : 'light');
    }
    function showConfirm(msg, callback) {
        document.getElementById('confirm-msg-text').innerText = msg;
        document.getElementById('confirm-modal').classList.remove('hidden');
        pendingConfirmCallback = callback;
    }
    function closeConfirm() {
        document.getElementById('confirm-modal').classList.add('hidden');
        pendingConfirmCallback = null;
    }
    function confirmOk() {
        if (pendingConfirmCallback) pendingConfirmCallback();
        closeConfirm();
    }

    return {
        init, login, createRoom, joinRoom, loadRooms, sendChat,
        showAlert, closeAlert,
        showConfirm, closeConfirm, confirmOk,
        closeRanking, exitRoom, toggleTheme,
        startGame: () => sendActionInternal({ actionType: 'START' }),
        sendAction: (data) => sendActionInternal(data)
    };
})();