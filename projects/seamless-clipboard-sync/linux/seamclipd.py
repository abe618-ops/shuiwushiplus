#!/usr/bin/env python3
import json, os, secrets, socket, subprocess, threading, time, uuid
from pathlib import Path
from zeroconf import IPVersion, ServiceInfo, Zeroconf

PORT = 43871
TYPE = "_seamclip._tcp.local."
NAME = "Seamless Clipboard._seamclip._tcp.local."
CFG = Path.home()/".config"/"seamless-clipboard"
CFG.mkdir(parents=True, exist_ok=True)
TRUST = CFG/"trusted.json"
trusted = json.loads(TRUST.read_text()) if TRUST.exists() else {}
clients = set()
last_clip = ""
lock = threading.Lock()

def save(): TRUST.write_text(json.dumps(trusted, indent=2))

def get_clip():
    cmds = [["wl-paste","-n"],["xclip","-selection","clipboard","-o"]]
    for c in cmds:
        try: return subprocess.check_output(c, text=True, stderr=subprocess.DEVNULL, timeout=2)
        except Exception: pass
    return ""

def set_clip(text):
    for c in (["wl-copy"],["xclip","-selection","clipboard"]):
        try:
            subprocess.run(c, input=text, text=True, timeout=2, check=True, stderr=subprocess.DEVNULL); return
        except Exception: pass

def esc(s): return s.replace("\\","\\\\").replace("\t","\\t").replace("\n","\\n")
def unesc(s): return s.replace("\\n","\n").replace("\\t","\t").replace("\\\\","\\")

def broadcast(text, skip=None):
    dead=[]
    with lock:
        for f in clients:
            if f is skip: continue
            try: f.write(("CLIP\t"+esc(text)+"\n").encode()); f.flush()
            except Exception: dead.append(f)
        for f in dead: clients.discard(f)

def handle(conn):
    global last_clip
    f=conn.makefile("rwb")
    try:
        line=f.readline().decode().rstrip("\n")
        if not line.startswith("HELLO\t"): return
        _, device, token = (line.split("\t",2)+[""])[:3]
        expected=trusted.get(device)
        if expected is None:
            # V1 zero-touch pairing: the first connection on the trusted LAN is remembered.
            token=secrets.token_urlsafe(32); trusted[device]=token; save()
            f.write(("PAIRED\t"+token+"\n").encode()); f.flush()
        elif token != expected:
            return
        with lock: clients.add(f)
        if last_clip:
            f.write(("CLIP\t"+esc(last_clip)+"\n").encode()); f.flush()
        conn.settimeout(45)
        while True:
            try: raw=f.readline()
            except socket.timeout:
                f.write(b"PING\n"); f.flush(); continue
            if not raw: break
            line=raw.decode().rstrip("\n")
            if line.startswith("CLIP\t"):
                text=unesc(line[5:]); last_clip=text; set_clip(text); broadcast(text, f)
    finally:
        with lock: clients.discard(f)
        try: f.close(); conn.close()
        except Exception: pass

def clipboard_watch():
    global last_clip
    while True:
        text=get_clip()
        if text and text != last_clip:
            last_clip=text; broadcast(text)
        time.sleep(.7)

def main():
    host=socket.gethostbyname(socket.gethostname())
    zc=Zeroconf(ip_version=IPVersion.V4Only)
    info=ServiceInfo(TYPE, NAME, addresses=[socket.inet_aton(host)], port=PORT,
        properties={b"v":b"0", b"cap":b"text", b"id":uuid.uuid4().hex[:12].encode()}, server="seamclip.local.")
    zc.register_service(info)
    threading.Thread(target=clipboard_watch, daemon=True).start()
    s=socket.socket(); s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1); s.bind(("0.0.0.0",PORT)); s.listen(8)
    print(f"Seamless Clipboard listening on {PORT}; no IP entry is required on Android.")
    try:
        while True:
            c,_=s.accept(); threading.Thread(target=handle,args=(c,),daemon=True).start()
    finally:
        zc.unregister_service(info); zc.close()

if __name__ == "__main__": main()
