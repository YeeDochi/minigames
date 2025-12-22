const IndianPoker = {
    onEnterRoom: () => {
        const board = document.getElementById('indian-poker-board');
        if (board) board.classList.remove('hidden');

        const globalStartBtn = document.getElementById('startBtn');
        if(globalStartBtn) {
            globalStartBtn.innerText = "게임 시작";
            globalStartBtn.style.display = 'inline-block';
            globalStartBtn.onclick = () => Core.startGame();
        }
    },

    handleMessage: (msg, myId) => {
        const globalStartBtn = document.getElementById('startBtn');

        // 게임 종료 처리
        if (msg.type === 'GAME_OVER') {
            Core.showAlert(msg.content);
            if(globalStartBtn) {
                globalStartBtn.innerText = "새 게임 시작";
                globalStartBtn.style.display = 'inline-block';
            }
            return;
        }

        // 라운드 종료 처리
        if (msg.type === 'ROUND_END') {
            Core.showAlert(msg.content);
            if(globalStartBtn) {
                globalStartBtn.innerText = "다음 라운드 ▶";
                globalStartBtn.style.display = 'inline-block';
            }
        }

        const data = msg.data;
        if (!data) return;

        // 버튼 제어
        const betBtns = document.getElementById('bet-btns');
        if (betBtns) {
            if (data.playing) {
                if(globalStartBtn) globalStartBtn.style.display = 'none';
                betBtns.style.visibility = 'visible';
            } else {
                betBtns.style.visibility = 'hidden';
            }
        }

        const hands = data.hands || {};
        const diedList = data.diedPlayers || [];
        const chips = data.chips || {};
        const nicknames = data.nicknames || {};
        const turnId = data.turnId;

        // --- [A] 상대방 그리기 ---
        const oppGrid = document.getElementById('opponents-grid');
        if (oppGrid) {
            oppGrid.innerHTML = '';

            Object.keys(hands).forEach(pid => {
                if (pid === myId) return; // 나는 건너뜀

                const box = document.createElement('div');
                box.className = 'opponent-box';

                if (pid === turnId) box.classList.add('active-turn');
                if (diedList.includes(pid)) box.classList.add('player-die');

                const cardData = hands[pid];
                const chipCount = chips[pid] !== undefined ? chips[pid] : 0;

                // [수정] 닉네임이 없으면 ID 뒷자리라도 보여줌 (이름 겹침 방지)
                let realName = nicknames[pid];
                if (!realName) realName = "Player " + pid.substring(0, 4);
                let html = `<div class="opponent-name" title="${realName}">${realName}</div><div class="chip-badge">🪙 ${chipCount}</div>`;

                if (cardData) {
                    const { area } = CardModule.createCardElement(cardData.suit, cardData.rank, false);
                    box.innerHTML = html;
                    box.appendChild(area);
                } else {
                    box.innerHTML = html + `<div style="height:100px; display:flex; align-items:center; color:#ccc; font-size:12px;">대기</div>`;
                }
                oppGrid.appendChild(box);
            });
        }

        // --- [B] 내 카드 그리기 ---
        const myArea = document.getElementById('my-card-area');
        if (myArea) {
            const myCard = hands[myId];
            const myRenderList = myCard ? [{ suit: '?', rank: -1 }] : [];
            const myChipCount = chips[myId] !== undefined ? chips[myId] : 0;

            let myName = nicknames[myId];
            if (!myName) myName = "나 (Me)";

            const myTitle = myArea.parentElement.querySelector('div:first-child');
            if(myTitle) {
                myTitle.innerHTML = `${myName} <span style="font-weight:normal; color:var(--link-color); margin-left:5px;">(🪙 ${myChipCount})</span>`;
            }

            myArea.style.opacity = diedList.includes(myId) ? '0.4' : '1';

            if (!data.playing && myCard) {
                CardModule.renderHand('my-card-area', [myCard], false, null);
            } else {
                CardModule.renderHand('my-card-area', myRenderList, false, null);
            }
        }

        // --- [C] 턴 제어 ---
        const isMyTurn = (turnId === myId);
        if (betBtns) {
            const btns = betBtns.querySelectorAll('button');
            btns.forEach(b => {
                b.disabled = !isMyTurn;
                b.style.opacity = isMyTurn ? '1' : '0.5';
                if(isMyTurn) b.style.transform = "scale(1.05)";
                else b.style.transform = "scale(1)";
            });
        }

        const turnText = document.getElementById('turn-alert');
        if (turnText) {
            if (data.playing) {
                let turnName = nicknames[turnId];
                if (!turnName) turnName = (isMyTurn ? "당신" : "상대방");

                turnText.innerHTML = isMyTurn
                    ? `<span style="color:#fff;">🚩 당신 차례입니다!</span>`
                    : `⏳ ${turnName}님이 고민 중...`;
            } else {
                turnText.innerText = "";
            }
        }
    },

    bet: (action) => {
        Core.sendAction({ actionType: 'BET', betChoice: action });
    }
};

Core.init(IndianPoker, {
    apiPath: '/indian_poker',
    wsPath: '/indian_poker/ws',
    gameName: '👳 인디언 포커'
});