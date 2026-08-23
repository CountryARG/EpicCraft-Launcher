const { invoke } = window.__TAURI__.core;
// Librería minecraft-launcher-core (vía bundler o cargada globalmente)
const { Authenticator } = window.MCLC || require('minecraft-launcher-core');

const userInput = document.getElementById('username');
const offlineBtn = document.getElementById('offline-btn');
const statusLog = document.getElementById('status-log');

async function handleLogin() {
    const name = userInput.value.trim();
    if (name !== "") {
        statusLog.innerText = "Estado: Generando autenticación offline...";
        
        // Generamos el objeto de autenticación de minecraft-launcher-core
        const userAuth = Authenticator.getAuth(name);
        
        // Guardamos todo el objeto de autenticación para consumirlo en engine.js
        localStorage.setItem('currentUserAuth', JSON.stringify(userAuth));
        localStorage.setItem('currentUser', userAuth.name);
        
        try {
            await invoke('login_offline', { username: userAuth.name });
            window.location.href = 'home.html';
        } catch (err) {
            statusLog.innerText = "Error de sesión: " + err;
        }
    } else {
        statusLog.innerText = "Estado: Por favor, escribe un nombre.";
        userInput.style.borderColor = "red";
    }
}

offlineBtn.onclick = handleLogin;

userInput.onkeypress = (e) => {
    if (e.key === 'Enter') {
        handleLogin();
    }
};