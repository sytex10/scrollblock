import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

out_dir = r"c:\Users\Emin\.gemini\antigravity-ide\scratch\scrollblock\cws-publish\screenshots"
os.makedirs(out_dir, exist_ok=True)

popup_path = r"C:\Users\Emin\.gemini\antigravity\brain\6f634e05-f20c-4166-a303-7bda3d9ffdc5\.user_uploaded\media_1794687267457_861e2086.png"
web_path = r"C:\Users\Emin\.gemini\antigravity\brain\6f634e05-f20c-4166-a303-7bda3d9ffdc5\.user_uploaded\media_1794687267475_6aede75f.jpg"

font_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 38)
font_subtitle = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 20)
font_badge = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 14)
font_feature_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 18)
font_feature_desc = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 14)

def add_rounded_corners(im, radius):
    mask = Image.new("L", im.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, im.size[0], im.size[1]), radius=radius, fill=255)
    im = im.convert("RGBA")
    im.putalpha(mask)
    return im

def add_drop_shadow(im, shadow_size=20, offset=(0, 10), shadow_color=(0, 0, 0, 160)):
    w, h = im.size
    shadow = Image.new("RGBA", (w + shadow_size * 2, h + shadow_size * 2), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    box = (shadow_size + offset[0], shadow_size + offset[1], 
           shadow_size + offset[0] + w, shadow_size + offset[1] + h)
    sdraw.rounded_rectangle(box, radius=18, fill=shadow_color)
    shadow = shadow.filter(ImageFilter.GaussianBlur(shadow_size // 2))
    shadow.paste(im, (shadow_size, shadow_size), im)
    return shadow

# ==========================================
# SCREENSHOT 1: EXTENSION POPUP SHOWCASE
# ==========================================
def create_screenshot_1():
    W, H = 1280, 800
    canvas = Image.new("RGB", (W, H), (10, 10, 13))
    draw = ImageDraw.Draw(canvas)

    # Ambient radial glow behind the popup
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((680, 100, 1280, 700), fill=(242, 27, 45, 28))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    canvas.paste(glow, (0, 0), glow)

    # Load and scale popup
    popup = Image.open(popup_path).convert("RGBA")
    # Original is (350, 594). Scale to 1.15x -> (402, 683)
    target_h = 680
    target_w = int(popup.width * (target_h / popup.height))
    popup = popup.resize((target_w, target_h), Image.Resampling.LANCZOS)
    popup = add_rounded_corners(popup, radius=18)
    popup_with_shadow = add_drop_shadow(popup, shadow_size=28, offset=(0, 12))

    # Place popup on right side
    px = 750
    py = (H - target_h) // 2 - 10
    canvas.paste(popup_with_shadow, (px - 28, py - 28), popup_with_shadow)

    # Left side marketing copy
    tx = 80
    # Badge
    badge_text = "SHORTBAN · RESMİ v1.0"
    draw.rounded_rectangle((tx, 140, tx + 200, 172), radius=16, fill=(242, 27, 45, 30), outline=(242, 27, 45, 120), width=1)
    draw.text((tx + 18, 147), badge_text, font=font_badge, fill=(255, 90, 100))

    # Title
    draw.text((tx, 195), "Dopamin Tuzağına\nSon Verin.", font=font_title, fill=(255, 255, 255), spacing=8)

    # Subtitle
    sub_text = "YouTube Shorts, Instagram Reels ve TikTok akışlarını\ntek tıkla engelleyin, dikkatinizi koruyun."
    draw.text((tx, 310), sub_text, font=font_subtitle, fill=(160, 160, 175), spacing=6)

    # Feature List Pills
    features = [
        ("🚫  Sıfır Gecikmeli Akış Kilidi", "Kısa videolara tıklandığında anında ana sayfaya döndürür."),
        ("💬  Akıllı Yorum Kalkanı", "Saatlerinizi harcatan toksik yorumları tek tıkla kapatır."),
        ("⚡  Dersler & Mesajlar Serbest", "Normal uzun videoları ve arkadaş sohbetlerini engellemez."),
        ("⏱️  Geçici Mola & PIN Kilidi", "İhtiyaç anında kontrollü mola hakkı tanımlayın.")
    ]

    fy = 405
    for title, desc in features:
        draw.rounded_rectangle((tx, fy, tx + 560, fy + 62), radius=12, fill=(18, 18, 23), outline=(38, 38, 46), width=1)
        draw.text((tx + 18, fy + 11), title, font=font_feature_title, fill=(255, 255, 255))
        draw.text((tx + 18, fy + 35), desc, font=font_feature_desc, fill=(140, 140, 155))
        fy += 74

    out_file = os.path.join(out_dir, "screenshot-1-extension-popup.png")
    canvas.save(out_file, "PNG", quality=95)
    print("Screenshot 1 saved:", out_file)

# ==========================================
# SCREENSHOT 2: WEB DASHBOARD SHOWCASE
# ==========================================
def create_screenshot_2():
    W, H = 1280, 800
    canvas = Image.new("RGB", (W, H), (10, 10, 13))
    draw = ImageDraw.Draw(canvas)

    # Ambient glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((400, 150, 900, 650), fill=(16, 185, 129, 20))
    glow = glow.filter(ImageFilter.GaussianBlur(70))
    canvas.paste(glow, (0, 0), glow)

    # Header Copy
    cx = W // 2
    badge_text = "WEB DASHBOARD & ANDROID ENTEGRASYONU"
    bbox = font_badge.getbbox(badge_text)
    bw = bbox[2] - bbox[0] + 30
    draw.rounded_rectangle((cx - bw // 2, 35, cx + bw // 2, 65), radius=15, fill=(16, 185, 129, 28), outline=(16, 185, 129, 100), width=1)
    draw.text((cx - (bbox[2] - bbox[0]) // 2, 42), badge_text, font=font_badge, fill=(52, 211, 153))

    title_text = "Tüm Cihazlarınızda Tek Kontrol Paneli"
    tbox = font_title.getbbox(title_text)
    draw.text((cx - (tbox[2] - tbox[0]) // 2, 78), title_text, font=font_title, fill=(255, 255, 255))

    # Browser Frame Mockup for web screenshot
    web_img = Image.open(web_path).convert("RGBA")
    
    # Target frame size in canvas
    frame_w = 980
    frame_h = 600
    fx = (W - frame_w) // 2
    fy = 155

    # Draw browser mockup container
    # Browser top bar
    bar_h = 36
    draw.rounded_rectangle((fx, fy, fx + frame_w, fy + frame_h), radius=16, fill=(18, 18, 22), outline=(42, 42, 52), width=1)
    
    # 3 dots
    draw.ellipse((fx + 16, fy + 12, fx + 26, fy + 22), fill=(255, 95, 86))
    draw.ellipse((fx + 34, fy + 12, fx + 44, fy + 22), fill=(255, 189, 46))
    draw.ellipse((fx + 52, fy + 12, fx + 62, fy + 22), fill=(39, 201, 63))

    # URL bar
    draw.rounded_rectangle((fx + 80, fy + 7, fx + frame_w - 30, fy + 29), radius=6, fill=(28, 28, 35))
    url_font = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 11)
    draw.text((fx + 95, fy + 10), "https://vsolken.com  ·  ShortBan Web Dashboard", font=url_font, fill=(140, 140, 155))

    # Place Web Image inside frame
    content_w = frame_w - 4
    content_h = frame_h - bar_h - 4
    web_resized = web_img.resize((content_w, int(web_img.height * (content_w / web_img.width))), Image.Resampling.LANCZOS)
    
    # Crop to fit inside browser frame
    crop_box = (0, 0, content_w, content_h)
    web_cropped = web_resized.crop(crop_box)
    canvas.paste(web_cropped, (fx + 2, fy + bar_h + 2))

    out_file = os.path.join(out_dir, "screenshot-2-web-dashboard.png")
    canvas.save(out_file, "PNG", quality=95)
    print("Screenshot 2 saved:", out_file)

if __name__ == "__main__":
    create_screenshot_1()
    create_screenshot_2()
