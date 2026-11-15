import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

out_dirs = [
    r"C:\Users\Emin\Desktop\PlayStore-Gorseller",
    r"C:\Users\Emin\.gemini\antigravity-ide\scratch\scrollblock\playstore"
]
for d in out_dirs:
    try:
        os.makedirs(d, exist_ok=True)
    except Exception:
        pass

logo_path = r"C:\Users\Emin\.gemini\antigravity-ide\scratch\scrollblock\web\site\logo.jpg"
logo_raw = Image.open(logo_path).convert("RGBA")

W, H = 1080, 1920

# Typography
f_badge = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 17)
f_hero = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 46)
f_sub = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 22)

# In-App Typography (Crisp Vector)
f_app_brand_bold = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 26)
f_app_brand_dim = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 16)
f_status = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 15)
f_status_dim = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 14)
f_section_header = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 14)

f_card_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 22)
f_card_sub = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 16)
f_card_label = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 15)
f_counter_big = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 64)
f_btn_text = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 19)
f_pill_active = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 17)
f_pill_inactive = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 17)
f_sub_item_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 18)
f_sub_item_desc = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 14)

# Colors
C_BG_CANVAS = (10, 10, 14)
C_PHONE_BG = (12, 13, 17)
C_CARD_BG = (22, 23, 29)
C_CARD_BORDER = (38, 40, 50)
C_RED = (242, 27, 45)
C_GREEN = (16, 185, 129)
C_TEXT_WHITE = (255, 255, 255)
C_TEXT_GREY = (150, 153, 168)
C_TEXT_MUTED = (100, 103, 116)
C_PILL_DARK = (32, 34, 42)

def draw_phone_top(draw, px, py, pw, logo_img):
    # Status bar
    draw.text((px + 36, py + 16), "02:16", font=f_status, fill=(230, 230, 235))
    # Right status icons (VoLTE, Wifi, 5G, Battery 73%)
    draw.text((px + pw - 135, py + 16), "📶 5G 🔋 73", font=f_status_dim, fill=(200, 202, 215))

    # App Bar
    ay = py + 52
    # Logo
    lw, lh = 36, 36
    scaled_logo = logo_raw.resize((lw, lh), Image.Resampling.LANCZOS)
    # Mask rounded
    mask = Image.new("L", (lw, lh), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.ellipse((0, 0, lw, lh), fill=255)
    # Note: caller will paste logo onto canvas if needed or draw circle
    
    # Text logo
    draw.text((px + 82, ay + 3), "SHORT", font=f_app_brand_bold, fill=(255, 255, 255))
    draw.text((px + 172, ay + 3), "BAN", font=f_app_brand_bold, fill=C_RED)
    draw.text((px + 232, ay + 9), "v1.0", font=f_app_brand_dim, fill=C_TEXT_GREY)

    # Active Pill
    pill_w = 98
    pill_x = px + pw - 36 - pill_w
    draw.rounded_rectangle((pill_x, ay + 2, pill_x + pill_w, ay + 36), radius=17, fill=(16, 185, 129, 32), outline=(16, 185, 129, 100), width=1)
    draw.ellipse((pill_x + 14, ay + 15, pill_x + 22, ay + 23), fill=C_GREEN)
    draw.text((pill_x + 30, ay + 8), "Aktif", font=f_status, fill=C_GREEN)

def draw_switch(draw, x, y, active=True):
    sw, sh = 56, 32
    if active:
        draw.rounded_rectangle((x, y, x + sw, y + sh), radius=sh // 2, fill=(255, 255, 255))
        draw.ellipse((x + sw - sh + 3, y + 3, x + sw - 3, y + sh - 3), fill=(15, 16, 20))
    else:
        draw.rounded_rectangle((x, y, x + sw, y + sh), radius=sh // 2, fill=(45, 47, 56))
        draw.ellipse((x + 3, y + 3, x + sh - 3, y + sh - 3), fill=(130, 133, 145))

# =========================================================================
# SHOT 1: EXACT MATCH OF SCREENSHOT TOP (Sayaç + Mola Modu)
# =========================================================================
def build_shot_1():
    img = Image.new("RGB", (W, H), C_BG_CANVAS)
    draw = ImageDraw.Draw(img)

    # Ambient glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 350, 940, 1150), fill=(242, 27, 45, 28))
    glow = glow.filter(ImageFilter.GaussianBlur(80))
    img.paste(glow, (0, 0), glow)

    # Top Marketing
    draw.rounded_rectangle((65, 60, 360, 100), radius=18, fill=(242, 27, 45, 26), outline=(242, 27, 45, 110), width=1)
    draw.text((90, 69), "SHORTBAN · RESMİ v1.0", font=f_badge, fill=(255, 85, 95))

    draw.text((65, 120), "Dopamin Tuzağına\nSon Verin.", font=f_hero, fill=(255, 255, 255), spacing=8)
    draw.text((65, 235), "Instagram Reels, TikTok ve YouTube Shorts akışlarını\nuygulamadan atmadan akıllıca engelleyin.", font=f_sub, fill=(160, 163, 180), spacing=6)

    # Phone Frame
    px, py = 65, 335
    pw = W - 130
    ph = 1530

    # Phone Shadow
    s_box = (px - 15, py - 10, px + pw + 15, py + ph + 15)
    draw.rounded_rectangle(s_box, radius=36, fill=(0, 0, 0))

    # Phone Screen
    draw.rounded_rectangle((px, py, px + pw, py + ph), radius=32, fill=C_PHONE_BG, outline=(42, 45, 56), width=2)
    draw_phone_top(draw, px, py, pw, logo_raw)
    
    # Paste real logo
    lw, lh = 36, 36
    s_logo = logo_raw.resize((lw, lh), Image.Resampling.LANCZOS)
    img.paste(s_logo, (px + 36, py + 52), s_logo)

    # Card 1: Toplam Engellenen Akış
    c1_y = py + 115
    c1_h = 165
    draw.rounded_rectangle((px + 24, c1_y, px + pw - 24, c1_y + c1_h), radius=20, fill=C_CARD_BG)
    draw.text((px + 48, c1_y + 24), "Toplam Engellenen Akış", font=f_card_label, fill=C_TEXT_GREY)
    # Sıfırla button
    draw.rounded_rectangle((px + pw - 120, c1_y + 20, px + pw - 48, c1_y + 54), radius=14, fill=C_PILL_DARK)
    draw.text((px + pw - 105, c1_y + 26), "Sıfırla", font=f_status_dim, fill=C_TEXT_GREY)
    # Big Number "7"
    draw.text((px + 48, c1_y + 58), "7", font=f_counter_big, fill=C_TEXT_WHITE)
    draw.text((px + 48, c1_y + 126), "Son engelleme: TikTok · 45 dk. önce", font=f_sub_item_desc, fill=C_TEXT_MUTED)

    # Label: MOLA VE DİNLENME
    ly = c1_y + c1_h + 30
    draw.text((px + 28, ly), "MOLA VE DİNLENME", font=f_section_header, fill=C_TEXT_MUTED)

    # Card 2: Geçici Mola Modu
    c2_y = ly + 28
    c2_h = 440
    draw.rounded_rectangle((px + 24, c2_y, px + pw - 24, c2_y + c2_h), radius=20, fill=C_CARD_BG)
    draw.text((px + 48, c2_y + 24), "Geçici Mola Modu", font=f_card_title, fill=C_TEXT_WHITE)
    # Sınırsız pill right
    draw.rounded_rectangle((px + pw - 130, c2_y + 22, px + pw - 48, c2_y + 56), radius=14, fill=C_PILL_DARK)
    draw.text((px + pw - 118, c2_y + 28), "Sınırsız", font=f_status_dim, fill=C_TEXT_GREY)
    draw.text((px + 48, c2_y + 60), "Belirli bir süre için engellemeyi geçici\nolarak duraklatın.", font=f_card_sub, fill=C_TEXT_GREY, spacing=4)

    # Mola Süresi Seçin
    draw.text((px + 48, c2_y + 125), "Mola Süresi Seçin", font=f_card_label, fill=C_TEXT_GREY)
    durations = [("5 dk", False), ("10 dk", True), ("15 dk", False), ("30 dk", False)]
    bw = 195
    for i, (txt, sel) in enumerate(durations):
        bx = px + 48 + i * (bw + 12)
        by = c2_y + 155
        if sel:
            draw.rounded_rectangle((bx, by, bx + bw, by + 56), radius=14, fill=(255, 255, 255))
            draw.text((bx + 66, by + 16), txt, font=f_pill_active, fill=(15, 16, 20))
        else:
            draw.rounded_rectangle((bx, by, bx + bw, by + 56), radius=14, fill=C_PILL_DARK)
            draw.text((bx + 72, by + 16), txt, font=f_pill_inactive, fill=C_TEXT_WHITE)

    # Günlük Mola Hakkı Sınırı
    draw.text((px + 48, c2_y + 235), "Günlük Mola Hakkı Sınırı", font=f_card_label, fill=C_TEXT_GREY)
    limits = [("Sınırsız", True), ("1 kez", False), ("2 kez", False), ("3 kez", False)]
    for i, (txt, sel) in enumerate(limits):
        bx = px + 48 + i * (bw + 12)
        by = c2_y + 265
        if sel:
            draw.rounded_rectangle((bx, by, bx + bw, by + 56), radius=14, fill=(255, 255, 255))
            draw.text((bx + 54, by + 16), txt, font=f_pill_active, fill=(15, 16, 20))
        else:
            draw.rounded_rectangle((bx, by, bx + bw, by + 56), radius=14, fill=C_PILL_DARK)
            draw.text((bx + 68, by + 16), txt, font=f_pill_inactive, fill=C_TEXT_WHITE)

    # 10 Dakika Mola Başlat Button
    btn_y = c2_y + 350
    draw.rounded_rectangle((px + 48, btn_y, px + pw - 48, btn_y + 64), radius=18, fill=(255, 255, 255))
    draw.text((px + 330, btn_y + 18), "10 Dakika Mola Başlat", font=f_btn_text, fill=(15, 16, 20))

    # Peek of KORUMA PLATFORMLARI
    k_y = c2_y + c2_h + 30
    draw.text((px + 28, k_y), "KORUMA PLATFORMLARI", font=f_section_header, fill=C_TEXT_MUTED)
    draw.rounded_rectangle((px + 24, k_y + 26, px + pw - 24, py + ph), radius=20, fill=C_CARD_BG)
    draw.text((px + 48, k_y + 50), "Instagram", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, k_y + 80), "Reels sekmesini Ana Sayfa'ya yönlendirir (uygulamadan atmaz)", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw_switch(draw, px + pw - 90, k_y + 55, active=True)

    for d in out_dirs:
        try:
            img.save(os.path.join(d, "phone-screenshot-1.png"), "PNG")
        except Exception:
            pass
    print("Shot 1 generated")

# =========================================================================
# SHOT 2: EXACT MATCH OF SCREENSHOT MIDDLE (KORUMA PLATFORMLARI)
# =========================================================================
def build_shot_2():
    img = Image.new("RGB", (W, H), C_BG_CANVAS)
    draw = ImageDraw.Draw(img)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 350, 940, 1150), fill=(16, 185, 129, 26))
    glow = glow.filter(ImageFilter.GaussianBlur(80))
    img.paste(glow, (0, 0), glow)

    # Top Marketing
    draw.rounded_rectangle((65, 60, 420, 100), radius=18, fill=(16, 185, 129, 26), outline=(16, 185, 129, 110), width=1)
    draw.text((90, 69), "AKILLI KORUMA KALKANI", font=f_badge, fill=(52, 211, 153))

    draw.text((65, 120), "Yorum ve DM\nKalkanı.", font=f_hero, fill=(255, 255, 255), spacing=8)
    draw.text((65, 235), "Toksik yorumları tek dokunuşla kapatın;\narkadaşlarınızın gönderdiği ilk videoyu izleyin.", font=f_sub, fill=(160, 163, 180), spacing=6)

    # Phone Frame
    px, py = 65, 335
    pw = W - 130
    ph = 1530

    s_box = (px - 15, py - 10, px + pw + 15, py + ph + 15)
    draw.rounded_rectangle(s_box, radius=36, fill=(0, 0, 0))
    draw.rounded_rectangle((px, py, px + pw, py + ph), radius=32, fill=C_PHONE_BG, outline=(42, 45, 56), width=2)
    draw_phone_top(draw, px, py, pw, logo_raw)
    
    lw, lh = 36, 36
    s_logo = logo_raw.resize((lw, lh), Image.Resampling.LANCZOS)
    img.paste(s_logo, (px + 36, py + 52), s_logo)

    # Section Header
    ly = py + 115
    draw.text((px + 28, ly), "KORUMA PLATFORMLARI", font=f_section_header, fill=C_TEXT_MUTED)

    # Single Big Card with all exact items from user screenshot
    cy = ly + 28
    ch = 1250
    draw.rounded_rectangle((px + 24, cy, px + pw - 24, cy + ch), radius=22, fill=C_CARD_BG)

    curr_y = cy + 24

    # 1. Instagram
    draw.text((px + 48, curr_y), "Instagram", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 30), "Reels sekmesini Ana Sayfa'ya yönlendirir (uygulamadan atmaz)", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw_switch(draw, px + pw - 90, curr_y + 6, active=True)
    curr_y += 75

    # Sub-item: DM'den gelen ilk Reels'a izin ver
    # Left vertical indicator bar
    draw.line((px + 60, curr_y + 5, px + 60, curr_y + 52), fill=C_TEXT_MUTED, width=2)
    draw.text((px + 75, curr_y), "DM'den gelen ilk Reels'a izin ver", font=f_sub_item_title, fill=C_TEXT_WHITE)
    draw.text((px + 75, curr_y + 26), "Yalnızca sohbetten açılan ilk videoya izin verir; sonraki\nvideoya kaydırdığınız an kapatır.", font=f_sub_item_desc, fill=C_TEXT_MUTED, spacing=4)
    draw_switch(draw, px + pw - 90, curr_y + 12, active=True)
    curr_y += 85

    # Sub-item: Yorumları engelle (Yorum Kalkanı)
    draw.line((px + 60, curr_y + 5, px + 60, curr_y + 52), fill=C_TEXT_MUTED, width=2)
    draw.text((px + 75, curr_y), "Yorumları engelle (Yorum Kalkanı)", font=f_sub_item_title, fill=(180, 183, 195))
    draw.text((px + 75, curr_y + 26), "Gönderi ve Reels yorumlar penceresi açıldığında\notomatik olarak kapatır.", font=f_sub_item_desc, fill=C_TEXT_MUTED, spacing=4)
    draw_switch(draw, px + pw - 90, curr_y + 12, active=False)
    curr_y += 115

    # 2. TikTok
    draw.text((px + 48, curr_y), "TikTok", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 30), "Sizin İçin akışını Gelen Kutusu'na yönlendirir (uygulamadan atmaz)", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw_switch(draw, px + pw - 90, curr_y + 6, active=True)
    curr_y += 75

    # Sub-item: DM'den gelen ilk videoya izin ver
    draw.line((px + 60, curr_y + 5, px + 60, curr_y + 52), fill=C_TEXT_MUTED, width=2)
    draw.text((px + 75, curr_y), "DM'den gelen ilk videoya izin ver", font=f_sub_item_title, fill=C_TEXT_WHITE)
    draw.text((px + 75, curr_y + 26), "Sohbetten açılan ilk video izlenir; sonraki videoya\nkaydırınca sohbete döner.", font=f_sub_item_desc, fill=C_TEXT_MUTED, spacing=4)
    draw_switch(draw, px + pw - 90, curr_y + 12, active=True)
    curr_y += 85

    # Sub-item: Yorumları engelle (Yorum Kalkanı)
    draw.line((px + 60, curr_y + 5, px + 60, curr_y + 52), fill=C_TEXT_MUTED, width=2)
    draw.text((px + 75, curr_y), "Yorumları engelle (Yorum Kalkanı)", font=f_sub_item_title, fill=(180, 183, 195))
    draw.text((px + 75, curr_y + 26), "Video yorumları penceresi açıldığında otomatik\nolarak kapatır.", font=f_sub_item_desc, fill=C_TEXT_MUTED, spacing=4)
    draw_switch(draw, px + pw - 90, curr_y + 12, active=False)
    curr_y += 115

    # 3. YouTube Shorts
    draw.text((px + 48, curr_y), "YouTube Shorts", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 30), "Shorts oynatıcısını kapatır, normal uzun videolar çalışır", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw_switch(draw, px + pw - 90, curr_y + 6, active=True)
    curr_y += 75

    # Sub-item: Yorumları engelle (Yorum Kalkanı)
    draw.line((px + 60, curr_y + 5, px + 60, curr_y + 52), fill=C_TEXT_MUTED, width=2)
    draw.text((px + 75, curr_y), "Yorumları engelle (Yorum Kalkanı)", font=f_sub_item_title, fill=(180, 183, 195))
    draw.text((px + 75, curr_y + 26), "Uzun videolarda ve Shorts'ta yorumlar paneli açıldığında\notomatik kapatır.", font=f_sub_item_desc, fill=C_TEXT_MUTED, spacing=4)
    draw_switch(draw, px + pw - 90, curr_y + 12, active=False)

    for d in out_dirs:
        try:
            img.save(os.path.join(d, "phone-screenshot-2.png"), "PNG")
        except Exception:
            pass
    print("Shot 2 generated")

# =========================================================================
# SHOT 3: EXACT MATCH OF SCREENSHOT BOTTOM (GÜVENLİK VE SİSTEM)
# =========================================================================
def build_shot_3():
    img = Image.new("RGB", (W, H), C_BG_CANVAS)
    draw = ImageDraw.Draw(img)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((140, 350, 940, 1150), fill=(59, 130, 246, 26))
    glow = glow.filter(ImageFilter.GaussianBlur(80))
    img.paste(glow, (0, 0), glow)

    # Top Marketing
    draw.rounded_rectangle((65, 60, 420, 100), radius=18, fill=(59, 130, 246, 26), outline=(59, 130, 246, 110), width=1)
    draw.text((90, 69), "%100 GİZLİLİK VE GÜVENLİK", font=f_badge, fill=(96, 165, 250))

    draw.text((65, 120), "Ebeveyn Kilidi &\nSıfır Veri Toplama.", font=f_hero, fill=(255, 255, 255), spacing=8)
    draw.text((65, 235), "Tüm işlemler telefon içinde yerel gerçekleşir.\nReklam, takip kodu veya veri aktarımı asla yoktur.", font=f_sub, fill=(160, 163, 180), spacing=6)

    # Phone Frame
    px, py = 65, 335
    pw = W - 130
    ph = 1530

    s_box = (px - 15, py - 10, px + pw + 15, py + ph + 15)
    draw.rounded_rectangle(s_box, radius=36, fill=(0, 0, 0))
    draw.rounded_rectangle((px, py, px + pw, py + ph), radius=32, fill=C_PHONE_BG, outline=(42, 45, 56), width=2)
    draw_phone_top(draw, px, py, pw, logo_raw)
    
    lw, lh = 36, 36
    s_logo = logo_raw.resize((lw, lh), Image.Resampling.LANCZOS)
    img.paste(s_logo, (px + 36, py + 52), s_logo)

    # Section Header
    ly = py + 115
    draw.text((px + 28, ly), "GÜVENLİK VE SİSTEM", font=f_section_header, fill=C_TEXT_MUTED)

    # Security Card matching exact screenshot
    cy = ly + 28
    ch = 720
    draw.rounded_rectangle((px + 24, cy, px + pw - 24, cy + ch), radius=22, fill=C_CARD_BG)

    curr_y = cy + 28

    # 1. Ebeveyn / Güvenlik PIN Kilidi
    draw.text((px + 48, curr_y), "Ebeveyn / Güvenlik PIN Kilidi", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 32), "Ayarları ve molayı 4 haneli şifreyle koruma altına alın.", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw_switch(draw, px + pw - 90, curr_y + 6, active=False)
    curr_y += 105
    draw.line((px + 48, curr_y - 20, px + pw - 48, curr_y - 20), fill=C_CARD_BORDER, width=1)

    # 2. Erişilebilirlik Servis Ayarları
    draw.text((px + 48, curr_y), "Erişilebilirlik Servis Ayarları", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 32), "Servis arka planda çalışıyor · Ayarları aç", font=f_sub_item_desc, fill=C_TEXT_GREY)
    # Right button: Aç
    draw.rounded_rectangle((px + pw - 105, curr_y + 6, px + pw - 48, curr_y + 44), radius=12, fill=C_PILL_DARK)
    draw.text((px + pw - 86, curr_y + 13), "Aç", font=f_status, fill=C_TEXT_WHITE)
    curr_y += 105
    draw.line((px + 48, curr_y - 20, px + pw - 48, curr_y - 20), fill=C_CARD_BORDER, width=1)

    # 3. Kullanım ve Kurulum Rehberi
    draw.text((px + 48, curr_y), "Kullanım ve Kurulum Rehberi", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 32), "Uygulamanın nasıl çalıştığını ve izin adımlarını görüntüleyin", font=f_sub_item_desc, fill=C_TEXT_GREY)
    # Right button: Rehber
    draw.rounded_rectangle((px + pw - 130, curr_y + 6, px + pw - 48, curr_y + 44), radius=12, fill=C_PILL_DARK)
    draw.text((px + pw - 116, curr_y + 13), "Rehber", font=f_status, fill=C_TEXT_WHITE)
    curr_y += 105
    draw.line((px + 48, curr_y - 20, px + pw - 48, curr_y - 20), fill=C_CARD_BORDER, width=1)

    # 4. Gizlilik Politikası ve Erişilebilirlik Beyanı
    draw.text((px + 48, curr_y), "Gizlilik Politikası ve Erişilebilirlik Beyanı", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 32), "Sıfır veri toplama taahhüdü ve izin kullanım detayları", font=f_sub_item_desc, fill=C_TEXT_GREY)
    # Right button: Oku
    draw.rounded_rectangle((px + pw - 110, curr_y + 6, px + pw - 48, curr_y + 44), radius=12, fill=C_PILL_DARK)
    draw.text((px + pw - 94, curr_y + 13), "Oku", font=f_status, fill=C_TEXT_WHITE)
    curr_y += 105
    draw.line((px + 48, curr_y - 20, px + pw - 48, curr_y - 20), fill=C_CARD_BORDER, width=1)

    # 5. Sürüm Row
    draw.text((px + 48, curr_y), "Sürüm", font=f_card_title, fill=C_TEXT_WHITE)
    draw.text((px + 48, curr_y + 32), "ShortBan Mobil", font=f_sub_item_desc, fill=C_TEXT_GREY)
    draw.text((px + pw - 195, curr_y + 12), "v1.0 (Build 36)", font=f_status, fill=C_TEXT_GREY)

    for d in out_dirs:
        try:
            img.save(os.path.join(d, "phone-screenshot-3.png"), "PNG")
        except Exception:
            pass
    print("Shot 3 generated")

build_shot_1()
build_shot_2()
build_shot_3()
