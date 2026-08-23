const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const { Client, Authenticator } = require('minecraft-launcher-core');

// FLAGS PARA PELAR CHROMIUM Y HACERLO SÚPER LIVIANO
app.commandLine.appendSwitch('disable-gpu');
app.commandLine.appendSwitch('disable-software-rasterizer');
app.commandLine.appendSwitch('disable-extensions');
app.commandLine.appendSwitch('disable-component-update');
app.commandLine.appendSwitch('disable-background-networking');
app.commandLine.appendSwitch('disable-sync');
app.commandLine.appendSwitch('disable-translate');
app.commandLine.appendSwitch('metrics-recording-only');

function createWindow() {
    const win = new BrowserWindow({
        width: 1000,
        height: 650,
        resizable: true,
        useContentSize: true,
        autoHideMenuBar: true,
        title: "EpicCraft Launcher",
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            backgroundThrottling: false,
            spellcheck: false
        }
    });

    win.loadFile('login.html');

    ipcMain.on('launch-mc', (event, data) => {
        const launcher = new Client();
        const homeDir = process.env.HOME || process.env.USERPROFILE;
        const mcPath = path.join(homeDir, 'Library', 'Application Support', 'minecraft');

        const opts = {
            authorization: Authenticator.getAuth(data.username),
            root: mcPath,
            version: {
                number: data.version,
                type: "release"
            },
            memory: {
                max: `${data.ram}G`,
                min: "1G"
            },
            customArgs: ['-XstartOnFirstThread']
        };

        launcher.launch(opts);

        launcher.on('progress', (e) => {
            if (e.total > 0) {
                const percent = ((e.task / e.total) * 100).toFixed(1);
                win.webContents.send('mc-progress', {
                    percent: percent,
                    type: e.type,
                    task: e.task,
                    total: e.total
                });
            }
        });

        launcher.on('data', (e) => win.webContents.send('mc-log', e));
        launcher.on('debug', (e) => win.webContents.send('mc-log', `[DEBUG] ${e}`));
        launcher.on('close', (code) => win.webContents.send('mc-closed', code));
    });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});