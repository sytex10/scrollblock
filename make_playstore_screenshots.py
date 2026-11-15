import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

out_dir = r"C:\Users\Emin\Desktop\PlayStore-Gorseller"
os.makedirs(out_dir, exist_ok=True)

W, H = 1080, 1920

font_hero = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 52)
font_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 36)
font_subtitle = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 22)
font_card_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 26)
font_card_desc = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 19)
font_badge = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 18)
font_small = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 16)
font_bold_small = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 17)

def draw_top_bar(draw, app_y, app_w, app_x):
    # App top bar inside phone canvas
    draw.text((app_x + 36, app_y + 24), "SHORTBAN", font=font_card_title, fill=(255, 255, 255))
    draw.text((app_x + 195, app_y + 28), "v1.0", font=font_small, fill=(150, 150, 160))
    # Active pill
    pill_w = 96
    pill_x = app_x + app_w - 36 - pill_w
    draw.rounded_rectangle((pill_x, app_y + 22, pill_x + pill_w, app_y + 54), radius=16, fill=(16, 185, 129, 35), outline=(16, 185, 129, 120), width=1)
    draw.ellipse((pill_x + 14, app_y + 35, pill_x + 22, app_y + 43), fill=(16, 185, 129))
    draw.text((pill_x + 30, app_y + 28), "Aktif", font=font_small, fill=(16, 185, 129))

def draw_toggle(draw, x, y, active=True):
    # iOS/Android style toggle
    w, h = 64, 36
    if active:
        draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill=(242, 27, 45))
        draw.ellipse((x + w - h + 4, y + 4, x + w - 4, y + h - 4), fill=(255, 255, 255))
    else:
        draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill=(45, 47, 56))
        draw.ellipse((x + 4, y + 4, x + h - 4, y + h - 4), fill=(140, 142, 152))

# ==========================================================
# SCREENSHOT 1: AKIŞ ENGELLEME & DİJİTAL SAĞLIK
# ==========================================================
def create_shot_1():
    img = Image.new("RGB", (W, H), (10, 11, 15))
    draw = ImageDraw.Draw(img)

    # Ambient radial gradient glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 400, 940, 1200), fill=(242, 27, 45, 32))
    glow = glow.filter(ImageFilter.GaussianBlur(90))
    img.paste(glow, (0, 0), glow)

    # Marketing Header
    # Badge
    draw.rounded_rectangle((70, 75, 360, 115), radius=18, fill=(242, 27, 45, 30), outline=(242, 27, 45, 130), width=1)
    draw.text((95, 83), "SHORTBAN · RESMİ v1.0", font=font_badge, fill=(255, 90, 100))

    # Headline
    draw.text((70, 140), "Dopamin Tuzağına\nSon Verin.", font=font_hero, fill=(255, 255, 255), spacing=10)
    draw.text((70, 270), "Instagram Reels, TikTok ve YouTube Shorts akışlarını\nuygulamadan atmadan akıllıca engelleyin.", font=font_subtitle, fill=(165, 168, 185), spacing=8)

    # App Mockup Container
    app_x = 70
    app_y = 370
    app_w = W - 140
    app_h = 1480

    # Phone Card Background
    draw.rounded_rectangle((app_x, app_y, app_x + app_w, app_y + app_h), radius=32, fill=(18, 19, 25), outline=(42, 44, 55), width=2)
    draw_top_bar(draw, app_y, app_w, app_x)

    # 1. Counter Card
    cy = app_y + 80
    draw.rounded_rectangle((app_x + 24, cy, app_x + app_w - 24, cy + 155), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, cy + 22), "Toplam Engellenen Kısa Video", font=font_card_desc, fill=(155, 158, 175))
    draw.text((app_x + 50, cy + 55), "142", font=ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 54), fill=(255, 255, 255))
    draw.text((app_x + 165, cy + 76), "akış durduruldu", font=font_card_desc, fill=(16, 185, 129))
    draw.text((app_x + 50, cy + 120), "Son engelleme: Instagram Reels · 12 dk. önce", font=font_small, fill=(130, 134, 150))

    # 2. Mola Modu Card
    my = cy + 175
    draw.rounded_rectangle((app_x + 24, my, app_x + app_w - 24, my + 240), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, my + 22), "Geçici Mola Modu", font=font_card_title, fill=(255, 255, 255))
    draw.text((app_x + 50, my + 56), "Belirli bir süre için engellemeyi geçici olarak duraklatın.", font=font_small, fill=(155, 158, 175))

    # Mola pills (5 dk, 10 dk, 15 dk, 30 dk)
    durations = ["5 dk", "10 dk", "15 dk", "30 dk"]
    bw = 195
    for i, d in enumerate(durations):
        bx = app_x + 50 + (i * (bw + 12))
        is_sel = (i == 1)
        bg = (255, 255, 255) if is_sel else (36, 38, 50)
        fg = (15, 16, 22) if is_sel else (220, 222, 235)
        draw.rounded_rectangle((bx, my + 95, bx + bw, my + 145), radius=14, fill=bg)
        draw.text((bx + 62, my + 107), d, font=font_bold_small, fill=fg)

    # Start break button
    draw.rounded_rectangle((app_x + 50, my + 165, app_x + app_w - 50, my + 218), radius=16, fill=(242, 27, 45))
    draw.text((app_x + 355, my + 178), "10 Dakika Mola Başlat", font=font_card_title, fill=(255, 255, 255))

    # 3. Platform Toggles Card
    py = my + 260
    draw.rounded_rectangle((app_x + 24, py, app_x + app_w - 24, py + 480), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, py + 22), "KORUMA PLATFORMLARI", font=font_bold_small, fill=(242, 27, 45))

    platforms = [
        ("Instagram", "Reels sekmesini Ana Sayfa'ya yönlendirir (uygulamadan atmaz)", True),
        ("TikTok", "Sizin İçin akışını Gelen Kutusu'na yönlendirir (uygulamadan atmaz)", True),
        ("YouTube Shorts", "Dikey Shorts oynatıcısını kapatır, normal uzun videolar çalışır", True)
    ]

    item_y = py + 65
    for name, desc, active in platforms:
        draw.text((app_x + 50, item_y), name, font=font_card_title, fill=(255, 255, 255))
        draw.text((app_x + 50, item_y + 34), desc, font=font_card_desc, fill=(155, 158, 175))
        draw_toggle(draw, app_x + app_w - 110, item_y + 6, active=active)
        item_y += 135
        if name != "YouTube Shorts":
            draw.line((app_x + 50, item_y - 20, app_x + app_w - 50, item_y - 20), fill=(42, 44, 55), width=1)

    img.save(os.path.join(out_dir, "phone-screenshot-1.png"), "PNG")
    print("phone-screenshot-1.png generated successfully")

# ==========================================================
# SCREENSHOT 2: AKILLI YORUM VE DM KALKANI
# ==========================================================
def create_shot_2():
    img = Image.new("RGB", (W, H), (10, 11, 15))
    draw = ImageDraw.Draw(img)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 400, 940, 1200), fill=(16, 185, 129, 28))
    glow = glow.filter(ImageFilter.GaussianBlur(90))
    img.paste(glow, (0, 0), glow)

    # Marketing Header
    draw.rounded_rectangle((70, 75, 410, 115), radius=18, fill=(16, 185, 129, 30), outline=(16, 185, 129, 130), width=1)
    draw.text((95, 83), "AKILLI KORUMA MODÜLLERİ", font=font_badge, fill=(52, 211, 153))

    draw.text((70, 140), "Yorum ve DM\nKalkanı.", font=font_hero, fill=(255, 255, 255), spacing=10)
    draw.text((70, 270), "Toksik yorumları tek dokunuşla kapatın;\narkadaşlarınızın gönderdiği ilk videoyu izleyin.", font=font_subtitle, fill=(165, 168, 185), spacing=8)

    app_x = 70
    app_y = 370
    app_w = W - 140
    app_h = 1480

    draw.rounded_rectangle((app_x, app_y, app_x + app_w, app_y + app_h), radius=32, fill=(18, 19, 25), outline=(42, 44, 55), width=2)
    draw_top_bar(draw, app_y, app_w, app_x)

    # 1. Feature Spotlight 1: DM Tek Video İzni
    fy1 = app_y + 80
    draw.rounded_rectangle((app_x + 24, fy1, app_x + app_w - 24, fy1 + 320), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.rounded_rectangle((app_x + 50, fy1 + 25, app_x + 260, fy1 + 65), radius=14, fill=(16, 185, 129, 30))
    draw.text((app_x + 65, fy1 + 33), "DM VİDEO İZNİ", font=font_badge, fill=(52, 211, 153))
    draw.text((app_x + 50, fy1 + 80), "DM'den Gelen İlk Reels'a İzin Ver", font=font_title, fill=(255, 255, 255))
    draw.text((app_x + 50, fy1 + 135), "Arkadaşınızın sohbetten attığı 1. videoyu izlemenize izin verir.\nKullanıcı parmağıyla 2. videoya kaydırdığı anda koruma\ndevreye girer ve oynatıcıyı kapatıp sohbete döndürür.", font=font_card_desc, fill=(175, 178, 195), spacing=8)
    draw_toggle(draw, app_x + app_w - 110, fy1 + 240, active=True)
    draw.text((app_x + 50, fy1 + 245), "Durum: Aktif (Dopamin döngüsünü kırar)", font=font_bold_small, fill=(16, 185, 129))

    # 2. Feature Spotlight 2: Yorum Kalkanı
    fy2 = fy1 + 345
    draw.rounded_rectangle((app_x + 24, fy2, app_x + app_w - 24, fy2 + 320), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.rounded_rectangle((app_x + 50, fy2 + 25, app_x + 285, fy2 + 65), radius=14, fill=(242, 27, 45, 30))
    draw.text((app_x + 65, fy2 + 33), "YORUM KALKANI", font=font_badge, fill=(255, 90, 100))
    draw.text((app_x + 50, fy2 + 80), "Toksik Yorumları Otomatik Engelle", font=font_title, fill=(255, 255, 255))
    draw.text((app_x + 50, fy2 + 135), "Saatlerinizi harcatan yorum sayfalarını tek dokunuşla kapatır.\nInstagram, TikTok ve YouTube'da yorum butonuna basıldığı veya\nyorum paneli açıldığı salisede pencereyi otomatik sonlandırır.", font=font_card_desc, fill=(175, 178, 195), spacing=8)
    draw_toggle(draw, app_x + app_w - 110, fy2 + 240, active=True)
    draw.text((app_x + 50, fy2 + 245), "Durum: Aktif (Zaman kaybını önler)", font=font_bold_small, fill=(255, 90, 100))

    # 3. Feature Spotlight 3: Hızlı Ayarlar Kutucuğu (Quick Settings)
    fy3 = fy2 + 345
    draw.rounded_rectangle((app_x + 24, fy3, app_x + app_w - 24, fy3 + 260), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, fy3 + 28), "Bildirim Paneli Hızlı Ayarlar Desteği", font=font_card_title, fill=(255, 255, 255))
    draw.text((app_x + 50, fy3 + 70), "Telefonunuzun üst bildirim çubuğuna ShortBan kutucuğunu ekleyin;\ntek dokunuşla korumayı duraklatın veya uzun basarak doğrudan\nuygulamayı açın.", font=font_card_desc, fill=(175, 178, 195), spacing=8)
    draw.rounded_rectangle((app_x + 50, fy3 + 175, app_x + 400, fy3 + 225), radius=14, fill=(36, 38, 50))
    draw.text((app_x + 75, fy3 + 188), "🛡️ ShortBan Tile: Aktif", font=font_bold_small, fill=(255, 255, 255))

    img.save(os.path.join(out_dir, "phone-screenshot-2.png"), "PNG")
    print("phone-screenshot-2.png generated successfully")

# ==========================================================
# SCREENSHOT 3: EBEVEYN KİLİDİ & %100 GİZLİLİK
# ==========================================================
def create_shot_3():
    img = Image.new("RGB", (W, H), (10, 11, 15))
    draw = ImageDraw.Draw(img)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 400, 940, 1200), fill=(59, 130, 246, 28))
    glow = glow.filter(ImageFilter.GaussianBlur(90))
    img.paste(glow, (0, 0), glow)

    # Marketing Header
    draw.rounded_rectangle((70, 75, 420, 115), radius=18, fill=(59, 130, 246, 30), outline=(59, 130, 246, 130), width=1)
    draw.text((95, 83), "%100 GİZLİLİK VE GÜVENLİK", font=font_badge, fill=(96, 165, 250))

    draw.text((70, 140), "Ebeveyn Kilidi &\nSıfır Veri Toplama.", font=font_hero, fill=(255, 255, 255), spacing=10)
    draw.text((70, 270), "Tüm işlemler telefon içinde yerel gerçekleşir.\nReklam, takip kodu veya veri aktarımı asla yoktur.", font=font_subtitle, fill=(165, 168, 185), spacing=8)

    app_x = 70
    app_y = 370
    app_w = W - 140
    app_h = 1480

    draw.rounded_rectangle((app_x, app_y, app_x + app_w, app_y + app_h), radius=32, fill=(18, 19, 25), outline=(42, 44, 55), width=2)
    draw_top_bar(draw, app_y, app_w, app_x)

    # 1. Security Pin Card
    sy1 = app_y + 80
    draw.rounded_rectangle((app_x + 24, sy1, app_x + app_w - 24, sy1 + 240), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, sy1 + 25), "Ebeveyn / Güvenlik PIN Kilidi", font=font_card_title, fill=(255, 255, 255))
    draw.text((app_x + 50, sy1 + 65), "Ayarları, platform kilitlerini ve mola sürelerini 4 haneli\ngüvenlik şifresiyle koruma altına alın. Şifre olmadan engeller\nkaldırılamaz.", font=font_card_desc, fill=(175, 178, 195), spacing=8)
    draw_toggle(draw, app_x + app_w - 110, sy1 + 165, active=True)
    draw.text((app_x + 50, sy1 + 172), "Durum: PIN Kilidi Aktif (••••)", font=font_bold_small, fill=(96, 165, 250))

    # 2. Zero Data Collection Card
    sy2 = sy1 + 265
    draw.rounded_rectangle((app_x + 24, sy2, app_x + app_w - 24, sy2 + 340), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.rounded_rectangle((app_x + 50, sy2 + 25, app_x + 300, sy2 + 65), radius=14, fill=(16, 185, 129, 30))
    draw.text((app_x + 65, sy2 + 33), "VERİ GÜVENLİĞİ", font=font_badge, fill=(52, 211, 153))
    draw.text((app_x + 50, sy2 + 80), "%100 Çevrimdışı ve Gizli Çalışma", font=font_title, fill=(255, 255, 255))
    draw.text((app_x + 50, sy2 + 135), "• Kişisel veriler, mesajlar veya şifreler ASLA okunmaz.\n• Erişilebilirlik izni sadece kısa video sekmelerini tespit eder.\n• Reklam, analitik SDK veya üçüncü taraf takipçisi içermez.\n• Sunucuya hiçbir kullanıcı verisi gönderilmez.", font=font_card_desc, fill=(175, 178, 195), spacing=12)

    # 3. Battery & System Card
    sy3 = sy2 + 365
    draw.rounded_rectangle((app_x + 24, sy3, app_x + app_w - 24, sy3 + 230), radius=22, fill=(26, 28, 38), outline=(50, 53, 68), width=1)
    draw.text((app_x + 50, sy3 + 25), "Sıfır Pil Tüketimi & Akıllı Uyku", font=font_card_title, fill=(255, 255, 255))
    draw.text((app_x + 50, sy3 + 68), "Servis yalnızca hedef sosyal medya uygulamaları açıldığında\nmilisaniyeler içinde uyanır. Diğer tüm zamanlarda uykudadır;\npili ve RAM'i tüketmez.", font=font_card_desc, fill=(175, 178, 195), spacing=8)
    draw.text((app_x + 50, sy3 + 165), "⚡ Android 15 & 16 Optimizasyonu: Tam Uyumlu", font=font_bold_small, fill=(16, 185, 129))

    img.save(os.path.join(out_dir, "phone-screenshot-3.png"), "PNG")
    print("phone-screenshot-3.png generated successfully")

create_shot_1()
create_shot_2()
create_shot_3()
