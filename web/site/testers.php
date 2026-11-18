<?php
/**
 * ShortBan Beta Test Kullanıcıları Paneli
 * https://vsolken.com/testers.php
 */

$csvFile = __DIR__ . '/beta_testers.csv';
$testers = [];

if (file_exists($csvFile)) {
    $rows = array_map('str_getcsv', file($csvFile));
    $header = array_shift($rows);
    foreach ($rows as $row) {
        if (!empty($row) && isset($row[1]) && !empty(trim($row[1]))) {
            $testers[] = [
                'date' => $row[0] ?? '',
                'email' => trim($row[1]),
                'name' => $row[2] ?? '',
                'device' => $row[3] ?? '',
                'ip' => $row[4] ?? ''
            ];
        }
    }
    // En yeni kayıtlar en üstte görünsün
    $testers = array_reverse($testers);
}

// Tüm e-postaları virgülle ayrılmış liste yap
$allEmails = array_unique(array_column($testers, 'email'));
$emailsJoined = implode(",\n", $allEmails);
?>
<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ShortBan · Beta Test Kullanıcıları</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: #09090b;
      color: #f4f4f6;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 30px 20px;
      display: flex;
      justify-content: center;
    }
    .container {
      width: 100%;
      max-width: 860px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .header-card {
      background: #121215;
      border: 1px solid #26262f;
      border-radius: 16px;
      padding: 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }
    .title-group h1 {
      font-size: 20px;
      font-weight: 700;
      color: #ffffff;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .badge {
      font-size: 11px;
      font-weight: 700;
      background: rgba(245, 158, 11, 0.15);
      color: #f59e0b;
      padding: 3px 10px;
      border-radius: 20px;
      border: 1px solid rgba(245, 158, 11, 0.3);
    }
    .counter {
      font-size: 32px;
      font-weight: 800;
      color: #10b981;
      font-family: monospace;
    }
    .copy-box {
      background: #18181c;
      border: 1px solid #2a2a34;
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    textarea {
      width: 100%;
      height: 80px;
      background: #0d0d10;
      border: 1px solid #26262f;
      border-radius: 8px;
      padding: 10px;
      color: #6ee7b7;
      font-family: monospace;
      font-size: 13px;
      resize: vertical;
    }
    .btn {
      background: #F21B2D;
      color: #fff;
      border: none;
      border-radius: 8px;
      padding: 10px 18px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: opacity 0.2s;
    }
    .btn:hover { opacity: 0.9; }
    .table-card {
      background: #121215;
      border: 1px solid #26262f;
      border-radius: 16px;
      overflow: hidden;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 13px;
    }
    th {
      background: #17171d;
      color: #a1a1aa;
      font-weight: 600;
      padding: 12px 16px;
      border-bottom: 1px solid #26262f;
      text-transform: uppercase;
      font-size: 11px;
      letter-spacing: 0.5px;
    }
    td {
      padding: 12px 16px;
      border-bottom: 1px solid #1a1a20;
      color: #d4d4d8;
    }
    tr:last-child td { border-bottom: none; }
    .email-cell { color: #6ee7b7; font-weight: 600; font-family: monospace; }
    .date-cell { color: #71717a; font-size: 12px; }
    .empty-state {
      padding: 40px;
      text-align: center;
      color: #71717a;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header-card">
      <div class="title-group">
        <div>
          <span class="badge">GOOGLE PLAY CLOSED TEST</span>
          <h1 style="margin-top: 6px;">ShortBan Kayıtlı Test Kullanıcıları</h1>
          <p style="color: #71717a; font-size: 13px; margin-top: 4px;">Web sitesinden ve LinkedIn'den başvuran adayların tam listesi</p>
        </div>
      </div>
      <div>
        <div style="font-size: 11px; color: #a1a1aa; text-transform: uppercase;">Toplam Başvuru</div>
        <div class="counter"><?= count($allEmails) ?></div>
      </div>
    </div>

    <?php if (!empty($allEmails)): ?>
    <div class="copy-box">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <label style="font-size: 12px; font-weight: 600; color: #e4e4e7;">Google Play Console İçin Toplu E-Posta Listesi (Virgülle Ayrılmış):</label>
        <button class="btn" id="copyBtn" onclick="copyEmails()">📋 Listeyi Kopyala</button>
      </div>
      <textarea id="emailsText" readonly><?= htmlspecialchars($emailsJoined) ?></textarea>
    </div>
    <?php endif; ?>

    <div class="table-card">
      <?php if (empty($testers)): ?>
        <div class="empty-state">
          Henüz kayıtlı başvuru bulunmuyor veya dosya yeni oluşturuldu.
        </div>
      <?php else: ?>
        <table>
          <thead>
            <tr>
              <th>Tarih</th>
              <th>Gmail Adresi</th>
              <th>Ad Soyad</th>
              <th>Cihaz</th>
            </tr>
          </thead>
          <tbody>
            <?php foreach ($testers as $t): ?>
            <tr>
              <td class="date-cell"><?= htmlspecialchars($t['date']) ?></td>
              <td class="email-cell"><?= htmlspecialchars($t['email']) ?></td>
              <td><?= htmlspecialchars($t['name'] ?: '—') ?></td>
              <td><?= htmlspecialchars($t['device'] ?: '—') ?></td>
            </tr>
            <?php endforeach; ?>
          </tbody>
        </table>
      <?php endif; ?>
    </div>
  </div>

  <script>
    function copyEmails() {
      var textarea = document.getElementById("emailsText");
      if (textarea) {
        textarea.select();
        document.execCommand("copy");
        var btn = document.getElementById("copyBtn");
        if (btn) {
          btn.textContent = "✓ Kopyalandı!";
          setTimeout(function() { btn.textContent = "📋 Listeyi Kopyala"; }, 2000);
        }
      }
    }
  </script>
</body>
</html>
