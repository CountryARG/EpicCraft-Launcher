#!/usr/bin/env python3
"""Exercise the actual Android screens through UIAutomator, without a game account."""
import subprocess, time, re, xml.etree.ElementTree as ET

def adb(*args):
    return subprocess.check_output(['adb',*args],text=True)

def nodes():
    for _ in range(8):
        try:
            adb('shell','uiautomator','dump','/sdcard/epic-ui.xml')
            return list(ET.fromstring(adb('shell','cat','/sdcard/epic-ui.xml')).iter('node'))
        except Exception:
            time.sleep(2)
    raise RuntimeError('No se pudo leer la pantalla Android')

def wait_text(value, seconds=90):
    end=time.time()+seconds
    while time.time()<end:
        ns=nodes()
        if any(n.get('text','').casefold()==value.casefold() for n in ns): return ns
        time.sleep(2)
    print('SCREEN:', [(n.get('text'), n.get('resource-id')) for n in ns], flush=True)
    print(adb('logcat','-d','-t','200','AndroidRuntime:E','*:S'), flush=True)
    raise AssertionError('No apareció: '+value)

def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))

def tap_text(value):
    ns=wait_text(value)
    tap(next(n for n in ns if n.get('text','').casefold()==value.casefold()))

adb('shell','am','start','-n','com.nebulateam.epiccraft.debug/net.kdt.pojavlaunch.TestStorageActivity')
ns=wait_text('Iniciar sesión')
assert not any(n.get('text','').casefold()=='añadir cuenta' for n in ns), 'Cuenta visible antes de login'
assert not any(n.get('resource-id','').endswith(('/account_spinner','/progress_layout','/setting_button')) for n in ns), 'Interfaz heredada visible'
tap(next(n for n in ns if n.get('class')=='android.widget.EditText'))
adb('shell','input','text','EpicTester')
adb('shell','input','keyevent','4')
tap_text('Iniciar sesión')
ns=wait_text('JUGAR')
assert any(n.get('text','').casefold()=='añadir cuenta' for n in ns)
assert any('Conectado como' in n.get('text','') and 'EpicTester' in n.get('text','') for n in ns)
tap(next(n for n in ns if n.get('content-desc')=='Ajustes'))
wait_text('Importar mods Java (.jar)')
tap_text('Cambiar usuario')
ns=wait_text('Iniciar sesión')
assert not any(n.get('text','').casefold()=='añadir cuenta' for n in ns)
assert not any(n.get('content-desc')=='Ajustes' for n in ns)
print('UI_SMOKE_OK: login limpio, Home, engranaje, importar mods y cambio de usuario')
