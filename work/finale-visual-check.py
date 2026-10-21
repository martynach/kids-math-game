import subprocess,time
from pathlib import Path
adb=r'C:/Users/chomi/AppData/Local/Android/Sdk/platform-tools/adb.exe'
out=Path('work/finale-visual'); out.mkdir(exist_ok=True)
def run(*args): return subprocess.run([adb,*args],check=True,stdout=subprocess.PIPE).stdout
run('shell','am','force-stop','com.example.kidsmath')
print(run('install','-r','app/build/outputs/apk/debug/app-debug.apk').decode(),flush=True)
xml=out/'finale.xml'; xml.write_text('<?xml version="1.0" encoding="utf-8"?><map><long name="elapsed" value="0"/><boolean name="pending" value="true"/></map>')
run('push',str(xml),'/data/local/tmp/finale-review.xml')
run('shell','run-as','com.example.kidsmath','mkdir','-p','shared_prefs')
run('shell','run-as','com.example.kidsmath','cp','/data/local/tmp/finale-review.xml','shared_prefs/finale.xml')
run('logcat','-c')
print(run('shell','am','start','-W','-n','com.example.kidsmath/.MainActivity').decode(),flush=True)
start=time.monotonic()
for sec in [2,7,11,13.5,14.3,15,17,19.5,21.5,23.5,26,28,32,37,39,42]:
 time.sleep(max(0,sec-(time.monotonic()-start)))
 (out/f'{sec:04.1f}.png').write_bytes(run('exec-out','screencap','-p'))
 print('Captured',sec,flush=True)
(out/'startup.log').write_bytes(run('logcat','-d','-s','AndroidRuntime:E','ActivityTaskManager:I'))

