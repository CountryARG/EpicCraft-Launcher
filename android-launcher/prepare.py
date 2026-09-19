#!/usr/bin/env python3
"""Create an isolated EpicCraft checkout from an audited, pinned Amethyst revision."""
import argparse
import pathlib
import shutil
import subprocess
import sys

REVISION = '3ad1100904fef8e3aaa7f50f1b6a05cef918b29c'
REPOSITORY = 'https://github.com/AngelAuraMC/Amethyst-Android.git'
BASE = pathlib.Path(__file__).resolve().parent


def run(*args, cwd=None):
    subprocess.run(args, cwd=cwd, check=True)


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('La base no coincide con la revisión esperada: ' + old[:90])
    return text.replace(old, new, 1)


def patch_download(text):
    text = replace_once(text, 'import net.kdt.pojavlaunch.JAssetInfo;',
                        'import net.kdt.pojavlaunch.epic.DownloadStatus;\nimport net.kdt.pojavlaunch.JAssetInfo;')
    text = replace_once(text, 'ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT);',
                        'DownloadStatus.finish();\n            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT);')
    text = replace_once(text, 'long dlFileCounter = mProcessedFileCounter.get();',
                        'long dlFileCounter = mProcessedFileCounter.get();\n        DownloadStatus.update(mProcessedSizeCounter.get(), -1);')
    text = replace_once(text, '(dlFileCounter * 100L) / mTotalFileCount',
                        '(dlFileCounter * 100L) / Math.max(1, mTotalFileCount)')
    text = replace_once(text, 'long dlFileSize = mProcessedSizeCounter.get();',
                        'long dlFileSize = mProcessedSizeCounter.get();\n        DownloadStatus.update(dlFileSize, mTotalSize);')
    text = replace_once(text, '(dlFileSize * 100L) / mTotalSize',
                        '(dlFileSize * 100L) / Math.max(1, mTotalSize)')
    text = replace_once(text, 'new ArrayBlockingQueue<>(mScheduledDownloadTasks.size(), false)',
                        'new ArrayBlockingQueue<>(Math.max(1, mScheduledDownloadTasks.size()), false)')
    text = replace_once(text, 'private void downloadFile() throws Exception {\n            try {',
                        'private void downloadFile() throws Exception {\n            DownloadStatus.file(mTargetPath.getAbsolutePath());\n            try {')
    text = replace_once(text, 'mLastCurr = curr;',
                        'mLastCurr = curr;\n            DownloadStatus.file(mTargetPath.getAbsolutePath());')
    text = replace_once(text, 'ensureJarFileCopy();\n                extractNatives(versionName);',
                        'if(mUseFileCounter) reportProgressFileCounter(0);\n                else reportProgressSizeCounter(0);\n                DownloadStatus.finish();\n                ensureJarFileCopy();\n                extractNatives(versionName);')
    return text


def apply(root):
    source = root / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/tasks/MinecraftDownloader.java'
    source.write_text(patch_download(source.read_text()))
    launcher = root / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/LauncherActivity.java'
    launcher.write_text(replace_once(launcher.read_text(), '        checkNotificationPermission();', '        // Notification permission is not requested on the local login screen.'))
    launcher.write_text(replace_once(launcher.read_text(),
        'Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();\n            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);',
        'Toast.makeText(this, "Vinculá tu cuenta de Minecraft desde Añadir cuenta para jugar", Toast.LENGTH_LONG).show();'))
    login = root / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/epic/EpicLogin.java'
    gradle = root / 'app_pojavlauncher/build.gradle'
    text = gradle.read_text().replace('org.angelauramc.amethyst', 'com.nebulateam.epiccraft')
    text = text.replace('"Amethyst (Debug)"', '"EpicCraft Launcher"').replace('"Amethyst"', '"EpicCraft Launcher"')
    text = text.replace('versionCode 10000000', 'versionCode 10000003')
    gradle.write_text(text)
    for item in (BASE / 'overlay').rglob('*'):
        if item.is_file():
            target = root / item.relative_to(BASE / 'overlay')
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(item, target)
    login.write_text(login.read_text().replace('login.setText("Iniciar sesión");', 'login.setAllCaps(false); login.setText("Iniciar sesión");'))
    shutil.copy2(BASE / 'README.md', root / 'EPICCRAFT.md')
    print('EpicCraft preparado en:', root)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--destination', default='EpicCraft-Android')
    args = parser.parse_args()
    destination = pathlib.Path(args.destination).resolve()
    if destination.exists():
        sys.exit('La carpeta de destino ya existe. Elegí una carpeta nueva para no sobrescribir trabajo.')
    run('git', 'clone', '--no-checkout', REPOSITORY, str(destination))
    run('git', 'checkout', '--detach', REVISION, cwd=destination)
    run('git', '-c', 'url.https://github.com/.insteadOf=git@github.com:',
        'submodule', 'update', '--init', '--recursive', cwd=destination)
    apply(destination)
    print('Abrí esta carpeta en Android Studio o usá el flujo de GitHub Actions incluido.')


if __name__ == '__main__':
    main()

