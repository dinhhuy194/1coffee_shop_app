# Commit Message Convention

Áp dụng Conventional Commits với scope:

```
<type>(<scope>): <description>

[optional body]
```

## Types:
- **feat**: Tính năng mới
- **fix**: Sửa bug
- **refactor**: Refactor code
- **docs**: Thay đổi documentation
- **style**: Format code (không ảnh hưởng logic)
- **test**: Thêm/sửa tests
- **chore**: Maintenance (cleanup, dependencies, config)

## Scope (bắt buộc):
Scope cho biết phần nào của project bị ảnh hưởng. Ví dụ:
- **auth**: Authentication/Authorization
- **product**: Product management
- **order**: Order processing
- **cart**: Shopping cart
- **category**: Category management
- **user**: User management
- **payment**: Payment processing
- **ui**: User interface
- **api**: API endpoints
- **db**: Database changes
- **config**: Configuration

## Rules:
1. **Type và Scope**:
   - Type: chữ thường
   - Scope: chữ thường, trong ngoặc đơn
   - Bắt buộc phải có scope để biết chỉnh sửa phần nào

2. **Description (dòng đầu)**:
   - Viết bằng tiếng việt 
   - Ngắn gọn, súc tích (tối đa 72 ký tự)
   - Không viết hoa chữ cái đầu
   - Không dấu chấm cuối câu
   - Dùng động từ nguyên thể (add, fix, update, remove)

3. **Body (optional)**:
   - Viết bằng tiếng việt
   - Giải thích WHAT và WHY, không phải HOW
   - Mỗi dòng tối đa 72 ký tự
   - Dùng dấu gạch đầu dòng (-) cho danh sách

## Ví dụ:

### ✅ GOOD:
```bash
git commit -m "feat(auth): thêm chức năng đặt lại mật khẩu"

git commit -m "fix(cart): sửa lỗi null pointer trong luồng thanh toán"

git commit -m "refactor(product): đơn giản hóa logic xác thực"

git commit -m "docs(api): cập nhật tài liệu cho các endpoint"

git commit -m "style(ui): đồng bộ chiều cao thẻ sản phẩm"

git commit -m "feat(category): thêm chức năng tab danh mục lồng nhau"

git commit -m "feat(auth): thêm chức năng quên mật khẩu

- thêm tính năng đặt lại mật khẩu qua email
- thêm xác thực token có thời hạn
- tạo giao diện đặt lại mật khẩu
- cập nhật service gửi email thông báo"
```

### ❌ BAD:
```bash
# Thiếu scope
git commit -m "feat: thêm chức năng đặt lại mật khẩu"

# Scope không rõ ràng
git commit -m "feat(stuff): thêm tính năng"

# Quá dài
git commit -m "feat(auth): thêm một tính năng mới cho phép người dùng đặt lại mật khẩu của họ khi họ quên mật khẩu"

# Viết hoa chữ cái đầu
git commit -m "feat(auth): Thêm chức năng đặt lại mật khẩu"

# Có dấu chấm cuối
git commit -m "feat(auth): thêm chức năng đặt lại mật khẩu."

# Không rõ ràng
git commit -m "fix(ui): cập nhật UI"
git commit -m "fix(cart): sửa lỗi"

# Dùng quá khứ (viết như đã làm xong thay vì động từ nguyên thể)
git commit -m "feat(auth): đã thêm chức năng đặt lại mật khẩu"
```

## General Principles

1. **Code phải tự giải thích** - Nếu cần comment dài để giải thích, hãy refactor code
2. **Comments phải có giá trị** - Giải thích WHY, không phải WHAT
3. **Ngắn gọn, súc tích** - Đọc vào là hiểu ngay
4. **Cập nhật comments** - Khi code thay đổi, comments cũng phải thay đổi
5. **Xóa commented code** - Dùng Git để lưu lịch sử, không comment code cũ
