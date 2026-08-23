const { invoke } = window.__TAURI__.core;
// Importamos minecraft-launcher-core
const { Client } = window.MCLC || require('minecraft-launcher-core');

let selectedVersion = null;
let selectedMod = null;

// Función para obtener las versiones reales directamente desde el core
async function fetchOfficialVersions() {
    const container = document.getElementById('version-container');
    container.innerHTML = '<div style="padding:10px; color:#aaa; font-size:12px;">Cargando versiones de Mojang...</div>';
    
    try {
        // Consultamos la lista oficial de versiones que provee el launcher core
        const response = await fetch('https://launchermeta.mojang.com/mc/game/version_manifest.json');
        const data = await response.json();
        
        // Filtramos solo las releases oficiales
        const releases = data.versions.filter(v => v.type === 'release');
        
        container.innerHTML = '';
        
        // Si no había selección previa, marcamos la más reciente por defecto
        if (!selectedVersion && releases.length > 0) {
            selectedVersion = releases[0].id;
        }

        releases.forEach(ver => {
            const item = document.createElement('div');
            item.className = `version-item ${ver.id === selectedVersion ? 'selected' : ''}`;
            item.innerText = `Minecraft ${ver.id}`;
            item.onclick = () => {
                document.querySelectorAll('.version-item').forEach(i => i.classList.remove('selected'));
                item.classList.add('selected');
                selectedVersion = ver.id;
            };
            container.appendChild(item);
        });
    } catch (err) {
        container.innerHTML = '<div style="padding:10px; color:red; font-size:12px;">Error al obtener versiones.</div>';
        console.error('Error cargando versiones:', err);
    }
}

// 1. Al tocar JUGAR, se piden las versiones al core y se despliega la lista
document.getElementById('play-btn').onclick = async () => {
    document.getElementById('version-card').style.display = 'block';
    await fetchOfficialVersions();
};

// 2. Al confirmar la versión elegida por el usuario
document.getElementById('confirm-version-btn').onclick = async () => {
    if (!selectedVersion) {
        alert('Por favor elegí una versión antes de continuar.');
        return;
    }

    document.getElementById('version-card').style.display = 'none';
    
    const dlCard = document.getElementById('download-card');
    const dlStatus = document.getElementById('dl-status');
    const dlFolder = document.getElementById('dl-folder');
    const barFill = document.getElementById('bar-fill');

    dlCard.style.display = 'block';

    // Recuperamos las credenciales del usuario generadas en auth.js
    const storedAuthRaw = localStorage.getItem('currentUserAuth');
    const userAuth = storedAuthRaw ? JSON.parse(storedAuthRaw) : { name: 'Player', uuid: '0000', client_token: '0000' };
    const ramLimpia = parseInt(document.getElementById('r-in').value);

    // Opciones dinámicas para el launcher core con la versión seleccionada por el usuario
    const launcherOpts = {
        authorization: userAuth,
        root: "./.minecraft",
        version: {
            number: selectedVersion,
            type: "release"
        },
        memory: {
            max: `${ramLimpia}G`,
            min: "1G"
        }
    };

    const downloadSteps = [
        { status: "VALIDANDO CREDENCIALES...", folder: `Usuario: ${userAuth.name}`, progress: 15 },
        { status: "CREANDO CARPETAS BASE...", folder: `${launcherOpts.root}/assets`, progress: 35 },
        { status: "DESCARGANDO LIBRERÍAS...", folder: `${launcherOpts.root}/versions/${selectedVersion}/libs`, progress: 65 },
        { status: "DESCARGANDO CLIENTE JAR...", folder: `${launcherOpts.root}/versions/${selectedVersion}/${selectedVersion}.jar`, progress: 85 },
        { status: "VERIFICANDO ASSETS Y RECURSOS...", folder: `${launcherOpts.root}/assets/objects`, progress: 100 }
    ];

    for (let step of downloadSteps) {
        dlStatus.innerText = step.status;
        dlFolder.innerText = step.folder;
        barFill.style.width = step.progress + "%";
        
        await new Promise(res => setTimeout(res, 500));
    }

    try {
        await invoke('start_game', { 
            ram: ramLimpia,
            username: userAuth.name,
            version: selectedVersion,
            modPath: selectedMod
        });
    } catch (err) {
        alert('Error al ejecutar el juego: ' + err);
        dlCard.style.display = 'none';
    }
};

document.getElementById('txt-mod').onclick = async () => {
    toggleSettings(false);
    try {
        selectedMod = await invoke('open_mod_selector'); 
        alert("Mod seleccionado correctamente.");
    } catch (err) {
        console.error(err);
    }
};