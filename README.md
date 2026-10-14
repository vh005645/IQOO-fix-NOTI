# FCM Keeper iQOO

App giúp giữ kết nối thông báo Google (FCM) trên iQOO / vivo chạy OriginOS (ROM nội địa Trung Quốc, GMS cài thêm).

## Build
1. Mở thư mục này bằng Android Studio (Koala trở lên), chờ Gradle sync (đồng ý dùng Gradle 8.7 nếu được hỏi).
2. Build > Build APK(s) rồi cài file `app/build/outputs/apk/debug/app-debug.apk` lên máy.

## Sau khi cài
1. Mở app, bấm lần lượt nút 1 → 5.
2. Với Google Play Services, Google Services Framework và app đang mất thông báo:
   - Pin: cho phép chạy nền tiêu thụ cao / không giới hạn
   - Quyền: bật Tự khởi động
   - Khóa app trong màn hình đa nhiệm
3. Không vuốt tắt (force-stop) các app đó.
4. Nếu dùng VPN/proxy, app VPN cũng cần các quyền ở bước 2.

## Bản 1.1.0
- Chu kỳ heartbeat tự điều chỉnh: sạc ~10 phút, bình thường ~15 phút, pin yếu / tiết kiệm pin / mất mạng ~30 phút.
- Thêm intent MCS_HEARTBEAT; chỉ gửi heartbeat khi có mạng.
- Mở màn hình tự khởi động của vivo/iQOO, Xiaomi, OPPO/realme/OnePlus, Honor.
- Hiển thị lần heartbeat tiếp theo và trạng thái mạng.

## Bản 1.3.0
- Đọc trạng thái FCM trực tiếp từ Google Play Services (yêu cầu "local/status", không có tài liệu chính thức; hiện chỉ biết mã 3 = mất kết nối).
- Mỗi 3 phút:
  - Đang kết nối: chỉ gửi heartbeat, không reconnect.
  - Mất kết nối: gửi heartbeat, chờ 2 giây, đọc lại; vẫn mất kết nối thì mới gửi reconnect.
  - GMS không trả lời: dùng kiểm tra TCP tới mtalk.google.com (cổng 5228/443) làm phương án dự phòng.
  - Không có mạng: không gửi gì, chờ mạng trở lại rồi kiểm tra lại.
- Màn hình hiển thị mã trạng thái thô mà GMS trả về để bạn đối chiếu.

## Bản 1.4.0
- Thêm cơ chế thử nghiệm giữ liên kết (bind) từ app tới một service của Google Play Services trong lúc foreground service đang chạy, để GMS ít bị hệ thống coi là app nền có thể đóng băng.
- Bật/tắt bằng nút 8; màn hình hiển thị trạng thái liên kết (đã liên kết / đang kết nối / không liên kết được / tắt).
- Tên service dùng để liên kết là nội bộ của GMS, có thể khác giữa các phiên bản; nếu không liên kết được thì app vẫn chạy bình thường.
- Chưa có bằng chứng cơ chế này vượt được bộ đóng băng của OriginOS; hãy kiểm chứng trên máy.

## Bản 1.4.1
- Nút 5 và nút 8 hiển thị rõ trạng thái ĐANG BẬT / ĐANG TẮT.
- Thêm biểu tượng ứng dụng (chuông thông báo kèm dấu "đã kết nối"), có biểu tượng đơn sắc cho Android 13+; thay biểu tượng thông báo bằng hình chuông.

## Bản 1.5.0
- Đầu màn hình hiện "Connect time" (chữ lớn, tự đếm từng giây) và dòng "Mất kết nối gần nhất" (ngày giờ + bao lâu trước) khi GMS báo mã 3.
- "Connect time" tính từ lúc app thấy GMS ở trạng thái kết nối; bị đặt lại khi mất kết nối, khởi động lại máy hoặc tắt dịch vụ. Thời điểm mất kết nối chỉ chính xác theo chu kỳ kiểm tra (khoảng 3 phút).
- Biểu tượng app: chuông thu nhỏ, thêm chữ IQOO màu vàng bên dưới.

## Bản 1.5.1
- "Dịch vụ giữ kết nối" và "Giữ liên kết với GMS" là công tắc bật/tắt thật.
- Mỗi điều kiện (Google Play Services, quyền thông báo, bỏ tối ưu pin, dịch vụ, liên kết GMS, mạng, kết nối FCM) hiện biểu tượng ✅ đạt / ❌ chưa đạt / ❔ đang chờ hoặc chưa rõ, kèm màu xanh / đỏ / cam.
- Trạng thái tự làm mới mỗi vài giây; bỏ các đoạn hướng dẫn dài trên màn hình.

## Bản 1.5.2
- Dòng "Connect time" nhỏ lại (22sp) và đổi sang màu xanh dương để tách khỏi các dòng trạng thái (xanh lá / đỏ / cam).

## Bản 1.5.3
- "Mất kết nối gần nhất" gọn lại: nhãn nhỏ phía trên, bên dưới chỉ hiện giờ và ngày (HH:mm:ss • dd/MM/yyyy).

## Giới hạn
- Yêu cầu đọc trạng thái FCM là cơ chế không có tài liệu chính thức; bản GMS mới có thể đổi hoặc chặn. Khi đó app tự dùng phương án dự phòng (kiểm tra TCP), kém chính xác hơn.
- Chu kỳ 3 phút tốn pin hơn; khi máy ngủ sâu (Doze) hệ thống có thể giãn alarm ra, nhất là nếu app chưa được bỏ tối ưu pin.
- App không thể tự bỏ giới hạn của OriginOS (cần root hoặc quyền hệ thống); phần quyết định là các cài đặt ở bước 2.
- Heartbeat gửi tới GMS là nỗ lực tốt nhất, bản GMS mới có thể bỏ qua.
- Tên màn hình cài đặt của vivo thay đổi theo phiên bản; nếu không mở thẳng được, app sẽ mở trang thông tin ứng dụng.
