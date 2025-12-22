/* [card-core.js] 카드 UI 생성 모듈 */
const CardModule = {
    getSuitChar: (suit) => {
        const map = { 'S': '♠', 'D': '♦', 'H': '♥', 'C': '♣' };
        return map[suit] || suit;
    },
    getColor: (suit) => {
        return (suit === 'H' || suit === 'D') ? 'red' : 'black';
    },
    getRankStr: (rank) => {
        const map = { 1: 'A', 11: 'J', 12: 'Q', 13: 'K' };
        return map[rank] || rank;
    },

    createCardElement: (suit, rank, isFlipped = false) => {
        const suitChar = CardModule.getSuitChar(suit);
        const rankStr = CardModule.getRankStr(rank);
        const color = CardModule.getColor(suit);

        const area = document.createElement('div');
        area.className = 'card-area'; // CSS 적용

        area.innerHTML = `
            <div class="card ${color} ${isFlipped ? 'flipped' : ''}">
                <div class="card-face">
                    <div class="card-top">${rankStr} ${suitChar}</div>
                    <div class="card-center">${suitChar}</div>
                    <div class="card-bottom">${rankStr} ${suitChar}</div>
                </div>
                <div class="card-back"></div>
            </div>
        `;
        return { area, cardInner: area.querySelector('.card') };
    },

    renderHand: (containerId, cards, isOverlap, onClick) => {
        const container = document.getElementById(containerId);
        if(!container) return;

        container.innerHTML = '';
        container.className = isOverlap ? 'overlap' : '';

        cards.forEach((c, idx) => {
            const isUnknown = (c.suit === '?' || c.rank === -1);
            const { area } = CardModule.createCardElement(
                isUnknown ? 'S' : c.suit,
                isUnknown ? 1 : c.rank,
                isUnknown
            );

            // 애니메이션 딜레이
            area.style.animationDelay = `${idx * 0.1}s`;

            if (onClick && !isUnknown) {
                area.onclick = () => {
                    area.classList.toggle('selected');
                    onClick(c, area.classList.contains('selected'));
                };
            }
            container.appendChild(area);
        });
    }
};