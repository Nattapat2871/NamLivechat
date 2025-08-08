<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Repo stars](https://img.shields.io/github/stars/Nattapat2871/NamLivechat?style=flat-square)](https://github.com/Nattapat2871/Namlivechat/stargazers)
![Visitor Badge](https://api.visitorbadge.io/api/VisitorHit?user=Nattapat2871&repo=NamLivechat&countColor=%237B1E7A&style=flat-square)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Nattapat2871)

</div>

<p align= "center">
      <a href="README.md">English</a> | <b>ภาษาไทย</b>
</p>

# NamLivechat

เชื่อมช่องว่างระหว่างไลฟ์สตรีมและเซิร์ฟเวอร์ Minecraft ของคุณ NamLivechat คือปลั๊กอินที่ทรงพลังและปรับแต่งได้อย่างยืดหยุ่น ที่จะดึงแชทสดจาก **YouTube**, **Twitch**, และ **TikTok** เข้ามาแสดงในเกมแบบ Real-time ช่วยให้คุณสื่อสารกับผู้ชมได้อย่างต่อเนื่องด้วยข้อความสดและระบบแจ้งเตือนที่สมบูรณ์แบบโดยไม่ต้องสลับหน้าจอไปมา ปลั๊กอินนี้ถูกสร้างมาเพื่อประสิทธิภาพและความเสถียร พร้อมรองรับเซิร์ฟเวอร์ยุคใหม่อย่าง Folia และ Paper

## หลักการทำงาน

ปลั๊กอินใช้วิธีการที่แตกต่างกันในการเชื่อมต่อกับแต่ละแพลตฟอร์ม เพื่อให้มั่นใจได้ถึงการเชื่อมต่อที่เสถียรและมีประสิทธิภาพที่สุด:
- **YouTube:** เชื่อมต่อผ่าน **Google YouTube Data API v3** ที่เป็นทางการ เพื่อดึงข้อความแชทและ Event ต่างๆ
- **Twitch:** เชื่อมต่อกับ **IRC Chat Server** ของ Twitch โดยตรงเพื่อการรับส่งข้อความที่รวดเร็ว และใช้ **Helix API** เพื่อตรวจสอบ Event เช่น ผู้ติดตามใหม่
- **TikTok:** เชื่อมต่อกับ **Webcast service** ที่ไม่เป็นทางการของ TikTok โดยใช้ไลบรารี `TikTokLiveJava` ซึ่งจะเลียนแบบการทำงานของเบราว์เซอร์เพื่อรับ Event สด

## ✨ ฟีเจอร์เด่น

- **รองรับหลายแพลตฟอร์ม:** เชื่อมต่อกับไลฟ์สตรีมของ **YouTube**, **Twitch**, และ **TikTok**
- **แชทสดในเกม:** แสดงข้อความแชทในเกม Minecraft พร้อม **สียศ** ตามบทบาทของผู้ใช้ในแต่ละแพลตฟอร์ม
- **ระบบแจ้งเตือนสมบูรณ์แบบ:** รับการแจ้งเตือนในเกมทันทีสำหรับทุก Event สำคัญ (Super Chat, Subscriptions, Gifts, Followers, ฯลฯ)
- **ระบบแจ้งเตือนขั้นสูง:** แสดงผลการแจ้งเตือน 3 รูปแบบพร้อมกัน: ข้อความในแชท, เสียงประกอบ, และ Boss Bar แบบไดนามิกที่แสดง **Event ล่าสุดขึ้นมาแทนที่ทันที**
- **ปรับแต่งได้ยืดหยุ่น:** สามารถปรับแต่งได้เกือบทุกส่วนของปลั๊กอินผ่านไฟล์ Config (.yml)
- **รองรับหลายภาษา:** ข้อความที่แสดงให้ผู้เล่นเห็นทั้งหมดสามารถแปลได้ มาพร้อมไฟล์ภาษา **อังกฤษ (`en.yml`)** และ **ไทย (`th.yml`)**
- **ระบบอัปเดตกึ่งอัตโนมัติ:** มีระบบตรวจสอบอัปเดตในเกม และคำสั่งสำหรับดาวน์โหลดเวอร์ชันล่าสุดไปยังโฟลเดอร์ `update` ของเซิร์ฟเวอร์เพื่อการอัปเดตที่ปลอดภัยและง่ายดาย

## ⚙️ การรองรับ

- **ซอฟต์แวร์เซิร์ฟเวอร์:** **PaperMC & Folia** (รองรับ 100%)
- **เวอร์ชัน Minecraft:** **1.21+**

## 📚 วิธีการติดตั้งและการตั้งค่าครั้งแรก

1.  ดาวน์โหลดไฟล์ `.jar` เวอร์ชันล่าสุดจากหน้า [Releases](https://github.com/Nattapat2871/NamLivechat/releases)
2.  นำไฟล์ `NamLivechat-X.X.jar` ไปใส่ในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์
3.  เปิดเซิร์ฟเวอร์ 1 ครั้งเพื่อให้ปลั๊กอินสร้างไฟล์ Config ที่จำเป็นทั้งหมด
4.  ปิดเซิร์ฟเวอร์ แล้วเข้าไปตั้งค่าไฟล์ต่างๆ ตามต้องการ (ดูหัวข้อถัดไป)
5.  เปิดเซิร์ฟเวอร์อีกครั้งและเริ่มใช้งาน!

## 🔧 การตั้งค่า (Configuration)

### `config.yml` (ไฟล์ตั้งค่าหลัก)

ไฟล์นี้ใช้ควบคุมการตั้งค่าโดยรวมของปลั๊กอิน

```yml
# ========================================= #
#            NamLivechat General Settings   #
# ========================================= #

# ตั้งค่าภาษาสำหรับข้อความในเกมทั้งหมด
language: "th"

# ตั้งค่าเป็น true เพื่อแสดง Log แบบละเอียดใน Console สำหรับการแก้บัค
debug-mode: false

# ========================================= #
#            Update Settings                #
# ========================================= #
# ตั้งค่าเป็น true เพื่อแจ้งเตือนแอดมินในเกมเมื่อมีเวอร์ชันใหม่
update-alert: true

# ตั้งค่าเป็น true เพื่อเปิดใช้งานคำสั่ง /namlivechat update
auto-update: true
```

### โฟลเดอร์ `messages/` (ไฟล์ภาษา)

โฟลเดอร์นี้จะเก็บไฟล์ภาษา (`en.yml`, `th.yml`) คุณสามารถแก้ไขไฟล์เหล่านี้เพื่อปรับเปลี่ยนข้อความทั้งหมดที่ปลั๊กอินใช้ส่งหาผู้เล่นได้

### `youtube-config.yml`

ไฟล์นี้ใช้ตั้งค่าทุกอย่างที่เกี่ยวกับ YouTube

```yml
# สวิตช์หลักสำหรับเปิด/ปิดการใช้งานส่วนของ YouTube
enabled: true

# API Key ของคุณจาก Google Cloud Console
youtube-api-key: "YOUR_API_KEY_HERE"

# รูปแบบข้อความแชทธรรมดา
# Placeholder ที่ใช้ได้: %player%, %message%
message-format: "&c[YouTube] &f%player%&7: &e%message%"

# สียศของผู้ใช้ในแชท
role-colors:
  owner: "&6"
  moderator: "&9"
  member: "&a"
  default: "&7"

# สวิตช์หลักสำหรับเปิด/ปิดการแจ้งเตือนทั้งหมดด้านล่าง
youtube-alerts:
  show-super-chat: true
  show-new-members: true

# --- การตั้งค่า Event ---

# การแจ้งเตือน Super Chat
super-chat:
  # Placeholder: %player%, %amount%, %message%
  message: "&6[Super Chat] &e%player% &fได้โดเนท &a%amount%&f: &d%message%"
  sound:
    name: "entity.firework_rocket.large_blast"
    volume: 1.0
    pitch: 1.2
  boss-bar:
    enabled: true
    # Placeholder: %player%, %amount%
    message: "&e&l%player% &f&lโดเนท &a&l%amount%&f&l!"
    # สีของ Boss Bar: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
    color: "YELLOW"
    duration: 12

# (การตั้งค่าอื่นๆ สำหรับ new-member, super-sticker, gifted-membership, member-milestone ก็จะมีรูปแบบคล้ายกัน)
```

### `twitch-config.yml`

ไฟล์นี้ใช้ตั้งค่าทุกอย่างที่เกี่ยวกับ Twitch

```yml
# สวิตช์หลักสำหรับเปิด/ปิดการใช้งานส่วนของ Twitch
enabled: true

# OAuth Token ของคุณ
oauth-token: "YOUR_OAUTH_TOKEN_HERE"

# รูปแบบข้อความแชทธรรมดา
# Placeholder ที่ใช้ได้: %badges%, %user%, %message%
format: "&5[Twitch] %badges%%user%&7: &f%message%"

# สียศของผู้ใช้ในแชท
role-colors:
  broadcaster: "&6"
  moderator: "&9"
  vip: "&d"
  subscriber: "&a"
  default: "&7"

# --- การตั้งค่า Event ---
events:
  enabled: true
  
  # การแจ้งเตือนผู้ติดตามใหม่
  new-follower:
    enabled: true
    # Placeholder: %user%
    message: "&d[Twitch] &f%user% &eเพิ่งติดตาม!"
    # ... (ตั้งค่า sound และ boss-bar)

  # (การตั้งค่าอื่นๆ สำหรับ new-subscription, resubscription, ฯลฯ ก็จะมีรูปแบบคล้ายกัน)
```

### `tiktok-config.yml`

ไฟล์นี้ใช้ตั้งค่าทุกอย่างที่เกี่ยวกับ TikTok

```yml
# สวิตช์หลักสำหรับเปิด/ปิดการใช้งานส่วนของ TikTok
enabled: true

# รูปแบบข้อความแชทธรรมดา
# Placeholder ที่ใช้ได้: %user%, %message%
message-format: "&b[TikTok] &f%user%&7: &f%message%"

# สียศของผู้ใช้ในแชท
role-colors:
  moderator: "&9"
  subscriber: "&a"
  default: "&7"

# --- การตั้งค่า Event ---
events:
  enabled: true

  # การแจ้งเตือนของขวัญ (Gift)
  gift:
    enabled: true
    # Placeholder: %user%, %gift_name%, %amount% (จำนวน), %total_value% (มูลค่าเพชร)
    message: "&e[TikTok Gift] &f%user% &aส่ง &d%gift_name% &aจำนวน %amount%x!"
    # ... (ตั้งค่า sound และ boss-bar)

  # การแจ้งเตือนผู้ติดตามใหม่
  follow:
    enabled: true
    # Placeholder: %user%
    message: "&b[TikTok] &f%user% &eเพิ่งติดตาม!"
    # ... (ตั้งค่า sound และ boss-bar)
```

## 🔑 วิธีการขอ API & Token

#### YouTube Data API v3 Key
1.  ไปที่ [Google Cloud Console](https://console.cloud.google.com/)
2.  สร้างโปรเจกต์ใหม่และเปิดใช้งาน **"YouTube Data API v3"**
3.  ไปที่ "Credentials" และสร้าง **"API key"** ใหม่
4.  คัดลอก Key ไปใส่ใน `youtube-config.yml`
5.  **สำคัญ:** เพื่อความปลอดภัย ควรกำหนดให้ API Key ของคุณใช้ได้เฉพาะจาก IP Address ของเซิร์ฟเวอร์เท่านั้น

#### Twitch OAuth Token
1.  ไปที่ [Twitch Token Generator](https://twitchtokengenerator.com/)
2.  เลือก **"Custom Scope"** และติ๊กถูกที่:
    - `chat:read`
    - `chat:edit`
    - `moderator:read:followers`
3.  สร้าง Token และคัดลอกทั้งหมด (รวม `oauth:`) ไปใส่ใน `twitch-config.yml`

## 💻 คำสั่งและ Permission

| คำสั่ง | คำอธิบาย | Permission | ค่าเริ่มต้น |
| :--- | :--- | :--- | :--- |
| `/livechat start <url>` | เริ่มการเชื่อมต่อโดยตรวจสอบแพลตฟอร์มอัตโนมัติ | `namlivechat.use` | `ทุกคน` |
| `/livechat start <platform> <url/id>` | เริ่มการเชื่อมต่อกับแพลตฟอร์มที่ระบุ | `namlivechat.use` | `ทุกคน` |
| `/livechat stop [platform]` | หยุดการเชื่อมต่อที่กำลังทำงานอยู่ | `namlivechat.use` | `ทุกคน` |
| `/namlivechat reload` | รีโหลดไฟล์ Config ทั้งหมด | `namlivechat.admin` | `OP` |
| `/namlivechat update` | ดาวน์โหลดปลั๊กอินเวอร์ชันล่าสุด | `namlivechat.admin` | `OP` |

## 📦 ไลบรารีที่ใช้งาน

- **Google API Client for Java**
- **Twitch4J**
- **TikTokLiveJava**

---
พัฒนาโดย Nattapat2871