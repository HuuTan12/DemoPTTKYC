# FastShip – Delivery Order Management System

Dự án môn **Phân tích và Thiết kế Yêu cầu (Requirements Analysis and Design)** – Khoa Công nghệ Thông tin, Trường Đại học Tôn Đức Thắng (TDTU).

### Thành viên thực hiện
| STT | Họ và tên | MSSV | Phân hệ phụ trách |
|---|---|---|---|
| 1 | **Đỗ Quốc Việt** | 524H0137 | Module 1: Order Management (FR-01 $\rightarrow$ FR-05, UC1, UC2) |
| 2 | **Trần Minh Thái** | 524H0029 | Module 2: Dispatch & Driver Management (FR-06 $\rightarrow$ FR-10, UC3, UC4) |
| 3 | **Nguyễn Hữu Tân** | 524H0028 | Module 3: Status Tracking & COD Reconciliation (FR-11 $\rightarrow$ FR-15, UC5, UC6) |

---

## 1. Công nghệ & Kiến trúc hệ thống (Chapter 7.2 – Table 40)

- **Ngôn ngữ & Giao diện:** Java 17+ (Java Swing Desktop Application – độ phân giải chuẩn `1440 × 900`).
- **Cơ sở dữ liệu:** MySQL 8.0 (13 bảng dữ liệu theo ERD Chương 5, kết nối qua JDBC `PreparedStatement` và quản lý Transaction `commit`/`rollback`).
- **Cấu trúc phân tầng (Layered Architecture):**
  ```text
  DemoPTTKYC/
  ├── pom.xml                       # Cấu hình Maven & MySQL Connector/J
  ├── docker-compose.yml            # Cấu hình container MySQL 8.0 tự động nạp SQL
  ├── start_mysql.bat               # Bật MySQL 8.0 trên cổng 3306 (Docker)
  ├── run_demo.bat                  # Biên dịch và khởi chạy ứng dụng FastShip
  ├── reset_db.bat                  # Reset sạch Database về dữ liệu gốc Table 41
  ├── sql/
  │   ├── schema.sql                # Tạo database fastship và 13 bảng (Chương 5)
  │   └── sample_data.sql           # Nạp dữ liệu mẫu phục vụ Demo (Table 41)
  └── src/main/java/fastship/
      ├── model/                    # 5 Enum và 13 lớp thực thể (Figure 5 Class Diagram)
      ├── dao/                      # 13 lớp DAO truy vấn SQL bằng PreparedStatement
      ├── service/                  # Tầng nghiệp vụ & Transaction (Figures 6-13 Sequence Diagrams)
      └── ui/                       # Các màn hình Java Swing (Figures 18-26 Wireframes)
  ```

---

## 2. Hướng dẫn chạy nhanh (Quick Start)

### Bước 1: Khởi động Database MySQL 8
- **Cách 1 (Dùng Docker tự động):** Nhấp đúp chạy file **`start_mysql.bat`**.
  - Script sẽ tự bật Docker, chạy container `fastship_mysql` ở cổng `127.0.0.1:3306` (`User: root`, `Password: để trống`) và tự nạp `sql/schema.sql` + `sql/sample_data.sql`.
- **Cách 2 (Dùng MySQL Server / XAMPP có sẵn):**
  - Chạy lần lượt `sql/schema.sql` và `sql/sample_data.sql`.
  - Nếu `root` có mật khẩu, cập nhật `DEFAULT_PASSWORD` tại `src/main/java/fastship/dao/DatabaseConnection.java`.

### Bước 2: Xem Database trên TablePlus (hoặc MySQL Workbench)
- Tạo kết nối **MySQL** trong TablePlus:
  - **Host:** `127.0.0.1` | **Port:** `3306` | **User:** `root` | **Password:** *(để trống)* | **Database:** `fastship`
- Sau mỗi thao tác trên ứng dụng Java, nhấn **`Ctrl + R`** trong TablePlus để làm mới và kiểm tra dữ liệu vừa lưu.

### Bước 3: Chạy ứng dụng Java Swing
- Nhấp đúp vào file **`run_demo.bat`** (hoặc chạy class `fastship.ui.FastShipApp` trong IDE).
- Kiểm tra dòng trạng thái trên màn hình Login hiển thị:
  `MySQL 8 Connected (localhost:3306/fastship)`

### Bước 4: Reset dữ liệu khi muốn test lại từ đầu
- Nhấp đúp vào file **`reset_db.bat`** để xóa dữ liệu test cũ và đưa toàn bộ 13 bảng về trạng thái chuẩn của **Table 41**.

---

## 3. Danh sách Tài khoản Demo (Table 41)

Tất cả tài khoản đều dùng mật khẩu chung là **`123456`**:

| Username | Password | Họ tên | Vai trò (Role) | Ghi chú |
|---|---|---|---|---|
| `customer01` | `123456` | Nguyen Van An | `CUSTOMER` | Có sẵn địa chỉ lưu tại Quận 7 (`D7`) |
| `staff01` | `123456` | Tran Thi Lan | `STAFF` | Duyệt/hủy đơn (UC2), xác nhận giao & đối soát COD (UC6) |
| `dispatch01` | `123456` | Pham Minh Hoang | `DISPATCHER` | Phân công & gợi ý tài xế (UC3) |
| `driver01` | `123456` | Le Van Binh | `DRIVER` | Khu vực `D7`, xe `59X1-123.45` (`AVAILABLE`) |
| `driver02` | `123456` | Pham Thi Cuc | `DRIVER` | Khu vực `D7`, xe `59X2-678.90` (`AVAILABLE`) |
| `manager01` | `123456` | Le Quang | `MANAGER` | Quản lý tài xế/xe (FR-06, FR-07) & Báo cáo (FR-15) |

---

## 4. Kịch bản Thuyết trình & Kiểm thử 11 Bước (Table 42)

| Bước | Tài khoản | Thao tác trên ứng dụng Java Swing | Kết quả & Bảng kiểm tra trong TablePlus (`Ctrl + R`) |
|---|---|---|---|
| **1** | `customer01` | Tại màn hình **Create order**, kiểm tra đơn mẫu (Người nhận `Tran Thi Dung`, SĐT `0912345678`, 2 kiện hàng tổng `2.5 kg`, gói `Standard`, COD `450,000 VND`, phí tự tính `32,500 VND`) $\rightarrow$ bấm **`Submit order`**. | Tạo đơn **`DH0002`** trạng thái `NEW`. Kiểm tra bảng `orders`, `OrderItem`, `TrackingEvent`. |
| **2** | `customer01` | Mở lại **Create order**, xóa trống ô `Phone` người nhận $\rightarrow$ bấm **`Submit order`**. | Hệ thống báo lỗi Validation (**UC1 – E1**) và không lưu xuống DB. |
| **3** | `staff01` | Tại màn hình **Orders**, lọc `Status: NEW`, chọn đơn **`DH0002`** $\rightarrow$ bấm **`Confirm`** (hoặc `View` $\rightarrow$ `Confirm order`). | Đơn `DH0002` chuyển sang `CONFIRMED`, có `confirmed_at` trong bảng `orders`. |
| **4** | `dispatch01` | Tại **Order assignment**, lọc `Area: D7`, chọn `DH0002` $\rightarrow$ bấm **`Suggest driver`** (gợi ý `Le Van Binh`) $\rightarrow$ bấm **`Confirm assignment`**. | Bảng `Assignment` thêm dòng `attempt_no = 1`, `status = 'PENDING'`. Bảng `orders` đổi thành `ASSIGNED`. |
| **5** | `driver01` | Tại **My assignments**, chọn `DH0002`, nhập lý do từ chối vào ô `Reject reason` $\rightarrow$ bấm **`Reject`**. | Bảng `Assignment` (`attempt_no = 1`) đổi thành `REJECTED`. Đơn `DH0002` tự quay về `CONFIRMED` chờ gán lại (**UC4 – A1**). |
| **6** | `dispatch01` | Tại **Order assignment**, chọn lại `DH0002` (hiển thị `Attempt 2`), chọn tài xế `Pham Thi Cuc` $\rightarrow$ bấm **`Confirm assignment`**. | Bảng `Assignment` giữ nguyên lịch sử lần 1 và thêm dòng mới `attempt_no = 2` cho `driver_id = 2` (**UC3 – A2**). |
| **7** | `driver02` | Bấm **`Accept order`** (Tài xế sang `BUSY`). Thử bấm nhảy cóc `In transit` sẽ bị chặn (**UC4 – E3**). Sau đó bấm tuần tự: **`Picked up`** $\rightarrow$ **`In transit`** $\rightarrow$ điền `Delivery proof` $\rightarrow$ bấm **`Submit proof`**. | Bảng `orders` thành `DELIVERED`. Bảng `DeliveryProof` lưu bằng chứng giao hàng. Bảng `Payment` sinh bản ghi COD `PENDING`. |
| **8** | `customer01` | Mở **Track order**, nhập mã **`DH0002`** $\rightarrow$ bấm **`Search`**. | Hiển thị trạng thái `DELIVERED`, tài xế `Pham Thi Cuc` và toàn bộ lịch sử `Timeline` (**UC5**). |
| **9** | `staff01` | Mở **Deliveries and COD**, chọn `DH0002` $\rightarrow$ bấm **`Confirm delivery`** $\rightarrow$ giữ `Cash received = 450000` $\rightarrow$ bấm **`Reconcile`**. | Bảng `DeliveryProof` cập nhật `confirmed_by`. Bảng `Payment` của `DH0002` chuyển thành `RECONCILED` (**UC6**). |
| **10** | `staff01` | Chọn đơn mẫu **`DH0001`** (`COD = 450000`), sửa `Cash received` thành **`400000`**, nhập ghi chú giải trình vào ô `Note` $\rightarrow$ bấm **`Reconcile`**. | Bảng `Payment` của `DH0001` chuyển thành `DIFFERENCE` kèm ghi chú lệch `-50,000 VND` (**UC6 – A2**). |
| **11** | `manager01` | Mở **Drivers and vehicles** (xem/thêm tài xế & xe) và mở **Reports**. | Xem tổng số đơn, tỷ lệ giao thành công, tổng COD thu được và cảnh báo lệch COD theo từng tài xế (**FR-15**). |
