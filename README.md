<h1 align="center">☕ Coffee Shop App</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth%20%7C%20RTDB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Target%20SDK-35-blue?style=for-the-badge"/>
</p>

<p align="center">
  Ứng dụng đặt cà phê trực tuyến được xây dựng bằng <strong>Kotlin</strong> theo kiến trúc <strong>MVVM</strong>, tích hợp <strong>Firebase</strong> làm backend, hỗ trợ thanh toán qua <strong>VNPay</strong> và chọn địa chỉ bằng <strong>Mapbox</strong>.
</p>

---

## 📱 Tính năng nổi bật

### 🛒 Mua sắm & Đặt hàng
- Duyệt sản phẩm theo danh mục, tìm kiếm, lọc theo giá / rating / danh mục
- Banner slider tự động cuộn với chấm chỉ báo (quản lý từ admin dashboard)
- Xem chi tiết sản phẩm, chọn size/số lượng, thêm vào giỏ hàng
- Giỏ hàng với hiệu ứng **"bong bóng"** Jetpack Compose khi thêm item
- Danh sách yêu thích (Favorites)

### 💳 Thanh toán
- **COD** (Thanh toán khi nhận hàng)
- **VNPay** — tích hợp WebView, xác nhận tự động, cập nhật trạng thái đơn hàng sau thanh toán
- Áp dụng **Voucher** giảm giá (fixed, phần trăm, miễn phí ship) tại màn hình giỏ hàng

### 📍 Địa chỉ giao hàng
- Tìm kiếm địa chỉ **inline** trong màn hình Checkout (Forward Geocoding Mapbox, debounce 500ms)
- Chọn địa chỉ trực tiếp trên **bản đồ Mapbox** (MapboxPickerActivity)
- Lưu địa chỉ vào Firestore, tự động load lại khi mở Checkout

### 🎖️ Hệ thống Loyalty (BEAN Points)
| Hạng | Lifetime BEAN | BEAN / 1.000đ chi tiêu |
|------|:---:|:---:|
| ☕ Normal | 0 – 99 | 2 |
| 🥈 Silver | 100 – 499 | 3 |
| 🥇 Gold | 500 – 1.499 | 4 |
| 💎 Diamond | ≥ 1.500 | 5 |

- Tích BEAN mỗi khi đặt hàng thành công
- Đổi BEAN lấy **BeanVoucher** (giảm giá cố định / phần trăm / miễn phí ship)
- Xem lịch sử điểm và tab đặc quyền theo hạng

### 👤 Tài khoản & Xác thực
- Đăng nhập / Đăng ký bằng **Email/Password**
- Đăng nhập bằng **Google Sign-In**
- Quản lý hồ sơ cá nhân
- Xem lịch sử đơn hàng

### 🔐 Phân quyền (RBAC)
- Phân biệt **user** / **admin** qua field `role` trong Firestore
- Admin có thể quản lý sản phẩm, banner, voucher qua **React Admin Dashboard** (repo riêng)

---

## 🏗️ Kiến trúc dự án

```
com.example.coffeeshop/
├── Activity/          # 16 Activities (Main, Cart, Checkout, Membership, Mapbox...)
├── Adapter/           # 13 RecyclerView Adapters
├── Api/               # MapboxApiService, VnPayApiService (Retrofit)
├── Domain/            # ItemsModel, CategoryModel, BannerModel, FilterOptions...
├── Fragment/          # FilterBottomSheet
├── Helper/            # ManagmentCart, ManagmentFavorite, BottomNavHelper, TinyDB...
├── Model/             # User, Order, BeanVoucher, RedeemedVoucher, Review...
├── Repository/        # Auth, Main, Order, User, Favorite, Review, ExchangeRate
├── ViewModel/         # MVVM ViewModels (Checkout, Membership, Main, Auth...)
└── ui/compose/        # Jetpack Compose components (CartBubble)
```

### Kiến trúc MVVM

```
Activity / Fragment
      │
      ▼
  ViewModel  ◄──── LiveData / StateFlow
      │
      ▼
  Repository
      │
   ┌──┴──┐
   ▼     ▼
Firebase  Retrofit (Mapbox, VNPay)
```

---

## 🔧 Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin |
| UI | ViewBinding + Jetpack Compose (hybrid) |
| Architecture | MVVM + Repository Pattern |
| Backend | Firebase Firestore, Realtime Database, Auth |
| Bản đồ | Mapbox Maps SDK v11 |
| Geocoding | Mapbox Forward Geocoding API (Retrofit) |
| Thanh toán | VNPay (WebView + REST API) |
| Hình ảnh | Glide, Coil (Compose) |
| Coroutines | Kotlin Coroutines + Flow |
| DI | ViewModelFactory (manual) |
| Storage local | TinyDB (SharedPreferences wrapper) |

---

## ⚙️ Cài đặt & Cấu hình

### Yêu cầu
- Android Studio **Hedgehog** trở lên
- JDK 21
- Android SDK 35

### Bước 1: Clone repository

```bash
git clone https://github.com/dinhhuy194/1coffee_shop_app.git
cd 1coffee_shop_app
```

### Bước 2: Cấu hình Firebase

1. Tạo project trên [Firebase Console](https://console.firebase.google.com/)
2. Thêm Android app với package `com.example.coffeeshop`
3. Tải file `google-services.json` và đặt vào `app/`

### Bước 3: Cấu hình Mapbox

1. Đăng ký tài khoản tại [account.mapbox.com](https://account.mapbox.com/)
2. Tạo 2 tokens:
   - **Secret token** (`sk.xxx`) với scope `DOWNLOADS:READ`
   - **Public token** (`pk.xxx`) — scope mặc định
3. Thêm vào `local.properties` **(KHÔNG commit file này)**:

```properties
MAPBOX_DOWNLOADS_TOKEN=sk.eyJ1...TOKEN_CUA_BAN...
MAPBOX_ACCESS_TOKEN=pk.eyJ1...TOKEN_CUA_BAN...
```

> ⚠️ `local.properties` đã được thêm vào `.gitignore`. **Không bao giờ** commit token thật vào `gradle.properties` hay bất kỳ file nào được track bởi Git.

### Bước 4: Cấu hình VNPay (tuỳ chọn)

Backend VNPay là một ASP.NET Core API riêng. Cập nhật base URL trong `VnPayApiService.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:PORT/"
```

### Bước 5: Build & Run

```bash
./gradlew assembleDebug
```

Hoặc nhấn **Run ▶** trong Android Studio.

---

## 📂 Cấu trúc Firebase

### Firestore Collections

```
users/{userId}
  ├── name, email, photoUrl
  ├── rank, totalPoints, lifetimePoints, totalSpent
  ├── address
  └── role ("user" | "admin")

orders/{orderId}
  ├── userId, items[], subtotal, tax, delivery, total
  ├── voucherId, discountAmount, discountType
  ├── paymentMethod, paymentStatus, orderStatus
  └── createdAt, transactionNo, bankCode

redeemedVouchers/{userId}/vouchers/{voucherId}
  └── ...voucher info + redeemedAt

pointHistory/{userId}/history/{entryId}
  └── delta, reason, timestamp
```

### Realtime Database

```
items/         # Danh sách sản phẩm
categories/    # Danh mục
banners/       # Banner slider (isHidden, imageUrl, link...)
beanVouchers/  # Voucher đổi BEAN
privileges/    # Đặc quyền theo hạng thành viên
```

---

## 🤝 Đóng góp

1. Fork repo
2. Tạo branch mới: `git checkout -b feature/ten-tinh-nang`
3. Commit: `git commit -m "feat: mô tả ngắn"`
4. Push: `git push origin feature/ten-tinh-nang`
5. Tạo Pull Request

---

## 📄 License

Dự án này được phát triển cho mục đích học tập.

---

<p align="center">Made with ❤️ by <a href="https://github.com/dinhhuy194">dinhhuy194</a></p>
