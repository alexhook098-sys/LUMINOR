#!/usr/bin/env python3
import asyncio
import json
import os
import re
import subprocess
import tempfile
from pathlib import Path

import requests
from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes, MessageHandler, filters

ROOT = Path(__file__).resolve().parent
ADAPTER = ROOT.parent / "browser" / "loremotion_adapter.js"
BOT_TOKEN = os.getenv("ASTRA_TELEGRAM_BOT_TOKEN", "").strip()
NODE_BIN = os.getenv("ASTRA_NODE_BIN", "node")
LORE_TIMEOUT = int(os.getenv("ASTRA_LORE_TIMEOUT", "420"))


def run_loremotion(prompt: str) -> dict:
    env = os.environ.copy()
    try:
        p = subprocess.run(
            [NODE_BIN, str(ADAPTER), prompt],
            cwd=str(ROOT),
            env=env,
            text=True,
            capture_output=True,
            timeout=LORE_TIMEOUT,
        )
    except subprocess.TimeoutExpired:
        return {"status": "GENERATION_TIMEOUT", "error": "LoreMotion process timeout"}
    except Exception as exc:
        return {"status": "PROCESS_ERROR", "error": str(exc)}

    # The Browser Agent writes diagnostics to stdout before the final JSON.
    # Extract the final JSON object without assuming diagnostics are JSON-free.
    result = None
    decoder = json.JSONDecoder()
    for match in re.finditer(r"\{", p.stdout):
        try:
            obj, _ = decoder.raw_decode(p.stdout[match.start():])
        except json.JSONDecodeError:
            continue
        if isinstance(obj, dict) and "status" in obj:
            result = obj

    if result is None:
        return {
            "status": "ADAPTER_ERROR",
            "error": (p.stderr or p.stdout or "No adapter result").strip()[-4000:],
        }

    if p.returncode not in (0, 2):
        result.setdefault("error", p.stderr.strip()[-2000:])
    return result


def download_video(url: str) -> Path:
    response = requests.get(url, stream=True, timeout=120)
    response.raise_for_status()
    suffix = ".mp4"
    content_type = response.headers.get("content-type", "")
    if "webm" in content_type:
        suffix = ".webm"
    fd, name = tempfile.mkstemp(prefix="astra_lore_", suffix=suffix)
    os.close(fd)
    path = Path(name)
    with path.open("wb") as f:
        for chunk in response.iter_content(chunk_size=1024 * 1024):
            if chunk:
                f.write(chunk)
    return path


async def start_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "ASTRA-SIRIUS online.\n"
        "Отправь текстовый промпт — я передам его в LoreMotion и верну видео.\n"
        "Если появится Turnstile, его нужно пройти вручную."
    )


async def generate(update: Update, context: ContextTypes.DEFAULT_TYPE):
    prompt = " ".join(context.args).strip()
    if not prompt:
        await update.message.reply_text("После /video нужен промпт.")
        return
    await process_prompt(update, prompt)


async def text_prompt(update: Update, context: ContextTypes.DEFAULT_TYPE):
    prompt = (update.message.text or "").strip()
    if prompt:
        await process_prompt(update, prompt)


async def process_prompt(update: Update, prompt: str):
    await update.message.reply_text("Запускаю Browser Agent → LoreMotion. ⏳")
    result = await asyncio.to_thread(run_loremotion, prompt)

    if result.get("status") != "GENERATION_COMPLETE":
        status = result.get("status", "UNKNOWN")
        if status == "GENERATION_BLOCKED_BY_VERIFICATION":
            msg = "Нужна ручная проверка Turnstile в открытом браузере. После прохождения повтори запрос."
        else:
            msg = f"Генерация не завершилась: {status}"
            if result.get("error"):
                msg += f"\n{result['error'][-1000:]}"
        await update.message.reply_text(msg)
        return

    url = result.get("videoUrl")
    if not url:
        await update.message.reply_text("Видео создано, но ссылка на MP4 не найдена.")
        return

    path = None
    try:
        await update.message.reply_text("Видео готово. Загружаю MP4 в Telegram…")
        path = await asyncio.to_thread(download_video, url)
        with path.open("rb") as video:
            await update.message.reply_video(video=video, caption="LoreMotion • ASTRA-SIRIUS")
    except Exception as exc:
        await update.message.reply_text(f"Видео создано, но отправить его не удалось: {exc}")
    finally:
        if path:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                pass


def main():
    if not BOT_TOKEN:
        raise SystemExit("ASTRA_TELEGRAM_BOT_TOKEN is not set")
    app = Application.builder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start_cmd))
    app.add_handler(CommandHandler("video", generate))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, text_prompt))
    print("[ASTRA TELEGRAM] polling")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
