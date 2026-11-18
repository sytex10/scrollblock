<?php
/**
 * ShortBan Google Play Kapalı Beta Testi Başvuru Kaydedici
 * (vsolken.com)
 */

header('Content-Type: application/json; charset=utf-8');

// Sadece POST isteklerine izin ver
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method not allowed']);
    exit;
}

// Gelen veriyi oku (JSON veya Form POST)
$rawInput = file_get_contents('php://input');
$data = json_decode($rawInput, true);

if (!$data) {
    $data = $_POST;
}

$email = isset($data['email']) ? trim($data['email']) : '';
$name = isset($data['name']) ? trim($data['name']) : '';
$device = isset($data['device']) ? trim($data['device']) : '';

// E-posta doğrulaması
if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Geçerli bir Google Play / Gmail adresi giriniz.']);
    exit;
}

$timestamp = date('Y-m-d H:i:s');
$ip = $_SERVER['REMOTE_ADDR'] ?? 'UNKNOWN';

// Veriyi CSV formatında hazırla
$csvFile = __DIR__ . '/beta_testers.csv';
$isNewFile = !file_exists($csvFile);

$entry = [
    $timestamp,
    $email,
    $name,
    $device,
    $ip
];

$fp = fopen($csvFile, 'a');
if ($fp) {
    if ($isNewFile) {
        fputcsv($fp, ['Tarih', 'E-Posta (Gmail)', 'Ad Soyad', 'Cihaz', 'IP']);
    }
    fputcsv($fp, $entry);
    fclose($fp);
}

// Bildirim e-postası gönderme denemesi (sunucuda sendmail/postfix aktifse gider)
@mail(
    'ipekmuhammetemin@gmail.com',
    'Yeni ShortBan Beta Test Kullanıcısı: ' . $email,
    "Yeni bir test kullanıcısı başvurdu:\n\n" .
    "E-Posta: {$email}\n" .
    "Ad Soyad: {$name}\n" .
    "Cihaz: {$device}\n" .
    "Tarih: {$timestamp}\n" .
    "IP: {$ip}\n",
    "From: ShortBan Beta <noreply@vsolken.com>\r\nContent-Type: text/plain; charset=UTF-8"
);

echo json_encode([
    'success' => true,
    'message' => 'Kaydınız başarıyla alındı! Teşekkür ederiz.',
    'email' => $email
]);
