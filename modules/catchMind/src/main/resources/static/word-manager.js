// Auto-Apply Theme
if (localStorage.getItem('theme') === 'dark') {
    document.body.classList.add('dark-mode');
}

window.onload = function() { loadWords(); };

function loadWords() {
    fetch('/catchmind/api/words').then(res => res.json()).then(words => {
        document.getElementById('word-list').innerHTML = words.reverse().map(w =>
            `<li class="word-item">
                <span style="font-family: monospace;">${w.word}</span>
                <span style="font-size: 12px; color: var(--text-secondary);">ID: ${w.id}</span>
            </li>`
        ).join('');
        document.getElementById('total-count').innerText = `${words.length} words`;
    });
}

function addWord() {
    const input = document.getElementById('wordInput');
    const val = input.value.trim();
    if(!val) return alert("Please enter a word.");
    fetch('/catchmind/api/words?text='+encodeURIComponent(val), {method:'POST'})
        .then(res=>res.text())
        .then(() => { input.value = ''; loadWords(); })
        .catch(err => alert("Error: " + err));
}